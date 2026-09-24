package com.despachoreactivo.project.service;

import com.despachoreactivo.project.event.DespachoEvent;
import com.despachoreactivo.project.event.EventBus;
import com.despachoreactivo.project.exception.DespachoNoExisteException;
import com.despachoreactivo.project.exception.EstadoInvalidoException;
import com.despachoreactivo.project.exception.ValidacionException;
import com.despachoreactivo.project.exception.ZonaRiesgosaException;
import com.despachoreactivo.project.model.Despacho;
import com.despachoreactivo.project.model.Paquete;
import com.despachoreactivo.project.repository.DespachoRepository;
import com.despachoreactivo.project.repository.PaqueteRepository;
import com.despachoreactivo.project.service.AsignacionSaga.ReservaCupo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DespachoService {

    private final DespachoRepository despachoRepository;
    private final PaqueteRepository paqueteRepository;
    private final DespachoExternalService externalService;
    private final EventBus eventBus;
    private final TransactionalOperator transactionalOperator;
    private final AsignacionSaga asignacionSaga;

    @Value("${app.risk-threshold:80}")
    private int riskThreshold;

    @Value("${app.reservation-ttl:15m}")
    private Duration reservationTtl;

    public DespachoService(
            DespachoRepository despachoRepository,
            PaqueteRepository paqueteRepository,
            DespachoExternalService externalService,
            EventBus eventBus,
            TransactionalOperator transactionalOperator,
            AsignacionSaga asignacionSaga) {
        this.despachoRepository = despachoRepository;
        this.paqueteRepository = paqueteRepository;
        this.externalService = externalService;
        this.eventBus = eventBus;
        this.transactionalOperator = transactionalOperator;
        this.asignacionSaga = asignacionSaga;
    }

    public Mono<Despacho> crearDespacho(Despacho despacho) {
        if (despacho.getCiudad() == null || despacho.getCiudad().isBlank()) {
            return Mono.error(new ValidacionException("Ciudad es requerida"));
        }
        if (despacho.getPaquetes() == null || despacho.getPaquetes().isEmpty()) {
            return Mono.error(new ValidacionException("Debe haber al menos un paquete"));
        }
        if (despacho.getClienteId() == null) {
            return Mono.error(new ValidacionException("clienteId es requerido"));
        }

        List<Paquete> paquetes = despacho.getPaquetes();

        return persistirDespachoRecibido(despacho, paquetes)
                .flatMap(despachoGuardado ->
                        asignacionSaga.reservarPaquetes(despachoGuardado.getCiudad(), despachoGuardado.getPaquetes())
                                .flatMap(resultado -> {
                                    despachoGuardado.setPaquetes(resultado.paquetes());
                                    return consultarServiciosExternos(despachoGuardado, resultado.reservas());
                                }));
    }

    private Mono<Despacho> persistirDespachoRecibido(Despacho despacho, List<Paquete> paquetes) {
        despacho.setEstado("RECIBIDO");
        despacho.setCreatedAt(Instant.now());
        despacho.setUpdatedAt(Instant.now());

        return despachoRepository.save(despacho)
                .flatMap(despachoGuardado -> {
                    eventBus.emit(new DespachoEvent(
                            despachoGuardado.getId(),
                            "RECIBIDO",
                            "RECIBIDO",
                            "Despacho recibido con " + paquetes.size() + " paquetes",
                            despacho.getCiudad(),
                            despacho.totalPaquetes()
                    ));

                    List<Paquete> paquetesConDespacho = paquetes.stream()
                            .peek(p -> p.setDespachoId(despachoGuardado.getId()))
                            .peek(p -> p.setEstado("RESERVADO"))
                            .peek(p -> p.setCreatedAt(Instant.now()))
                            .collect(Collectors.toList());

                    return Flux.fromIterable(paquetesConDespacho)
                            .concatMap(paqueteRepository::save)
                            .collectList()
                            .map(paquetesGuardados -> {
                                despachoGuardado.setPaquetes(paquetesGuardados);
                                return despachoGuardado;
                            });
                })
                .as(transactionalOperator::transactional);
    }
    private Mono<Despacho> consultarServiciosExternos(Despacho despacho, List<ReservaCupo> reservas) {
        int pesoTotal = despacho.getPaquetes().stream()
                .mapToInt(Paquete::getPesoKg)
                .sum();

        return externalService.consultarServicios(
                        despacho.getClienteId(),
                        pesoTotal,
                        despacho.getCiudad())
                .flatMap(resultado -> {
                    if (resultado.riesgo().score() > riskThreshold) {
                        return asignacionSaga.compensar(reservas)
                                .then(Mono.error(new ZonaRiesgosaException(
                                        "Score de riesgo: " + resultado.riesgo().score())));
                    }

                    despacho.setTarifaTotal(resultado.tarifa().tarifa());
                    despacho.setRiskScore(resultado.riesgo().score());
                    despacho.setEstado("ASIGNADO");
                    despacho.setExpiraEn(Instant.now().plus(reservationTtl));
                    despacho.setUpdatedAt(Instant.now());

                    return persistirDespachoAsignado(despacho)
                            .doOnSuccess(despachoAsignado -> eventBus.emit(new DespachoEvent(
                                    despachoAsignado.getId(),
                                    "ASIGNADO",
                                    "ASIGNADO",
                                    "Despacho asignado, tarifa: " + resultado.tarifa().tarifa()
                                            + ", riesgo: " + resultado.riesgo().score(),
                                    despacho.getCiudad(),
                                    despacho.totalPaquetes())));
                })
                .onErrorResume(error -> {
                    if (error instanceof ZonaRiesgosaException) {
                        return Mono.error(error);
                    }
                    return asignacionSaga.compensar(reservas).then(Mono.error(error));
                });
    }

    private Mono<Despacho> persistirDespachoAsignado(Despacho despacho) {
        return despachoRepository.save(despacho).as(transactionalOperator::transactional);
    }

    public Mono<Despacho> obtenerDespacho(Long id) {
        return despachoRepository.findById(id)
                .switchIfEmpty(Mono.error(new DespachoNoExisteException()))
                .flatMap(despacho ->
                        paqueteRepository.findByDespachoId(id)
                                .collectList()
                                .map(paquetes -> {
                                    despacho.setPaquetes(paquetes);
                                    return despacho;
                                })
                );
    }

    public Mono<Despacho> confirmarDespacho(Long id) {
        return obtenerDespacho(id)
                .flatMap(despacho -> {
                    if (!"ASIGNADO".equals(despacho.getEstado())) {
                        return Mono.error(
                                new EstadoInvalidoException("Despacho debe estar en estado ASIGNADO"));
                    }

                    despacho.setEstado("EN_RUTA");
                    despacho.setExpiraEn(null);
                    despacho.setUpdatedAt(Instant.now());

                    return despachoRepository.save(despacho)
                            .flatMap(despachoConfirmado -> {
                                eventBus.emit(new DespachoEvent(
                                        despachoConfirmado.getId(),
                                        "EN_RUTA",
                                        "EN_RUTA",
                                        "Despacho confirmado y en ruta",
                                        despacho.getCiudad(),
                                        despacho.totalPaquetes()));
                                return Mono.just(despachoConfirmado);
                            });
                })
                .as(transactionalOperator::transactional);
    }

    public Flux<Despacho> obtenerDespachosPorCiudad(String ciudad) {
        return despachoRepository.findAll()
                .filter(d -> d.getCiudad().equals(ciudad))
                .flatMap(despacho ->
                        paqueteRepository.findByDespachoId(despacho.getId())
                                .collectList()
                                .map(paquetes -> {
                                    despacho.setPaquetes(paquetes);
                                    return despacho;
                                })
                );
    }

    public Flux<Despacho> obtenerDespachosPorEstado(String estado) {
        return despachoRepository.findAll()
                .filter(d -> d.getEstado().equals(estado))
                .flatMap(despacho ->
                        paqueteRepository.findByDespachoId(despacho.getId())
                                .collectList()
                                .map(paquetes -> {
                                    despacho.setPaquetes(paquetes);
                                    return despacho;
                                })
                );
    }
}
