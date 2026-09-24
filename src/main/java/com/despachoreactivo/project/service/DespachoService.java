package com.despachoreactivo.project.service;

import com.despachoreactivo.project.event.DespachoEvent;
import com.despachoreactivo.project.event.EventBus;
import com.despachoreactivo.project.exception.CupoInsuficienteException;
import com.despachoreactivo.project.exception.DespachoNoExisteException;
import com.despachoreactivo.project.exception.EstadoInvalidoException;
import com.despachoreactivo.project.exception.ValidacionException;
import com.despachoreactivo.project.exception.ZonaRiesgosaException;
import com.despachoreactivo.project.model.Despacho;
import com.despachoreactivo.project.model.Paquete;
import com.despachoreactivo.project.repository.DespachoRepository;
import com.despachoreactivo.project.repository.PaqueteRepository;
import com.despachoreactivo.project.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DespachoService {

    private final DespachoRepository despachoRepository;
    private final PaqueteRepository paqueteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final DespachoExternalService externalService;
    private final EventBus eventBus;
    private final TransactionalOperator transactionalOperator;
    
    @Value("${app.risk-threshold:80}")
    private int riskThreshold;
    
    @Value("${app.reservation-ttl:2m}")
    private Duration reservationTtl;

    public DespachoService(
            DespachoRepository despachoRepository,
            PaqueteRepository paqueteRepository,
            VehiculoRepository vehiculoRepository,
            DespachoExternalService externalService,
            EventBus eventBus,
            TransactionalOperator transactionalOperator) {
        this.despachoRepository = despachoRepository;
        this.paqueteRepository = paqueteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.externalService = externalService;
        this.eventBus = eventBus;
        this.transactionalOperator = transactionalOperator;
    }

    public Mono<Despacho> crearDespacho(Despacho despacho) {
        if (despacho.getCiudad() == null || despacho.getCiudad().isBlank()) {
            return Mono.error(new ValidacionException("Ciudad es requerida"));
        }
        if (despacho.getPaquetes() == null || despacho.getPaquetes().isEmpty()) {
            return Mono.error(new ValidacionException("Debe haber al menos un paquete"));
        }

        List<Paquete> paquetes = despacho.getPaquetes();
        int totalPeso = paquetes.stream().mapToInt(Paquete::getPesoKg).sum();
        
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
                            .flatMap(paqueteRepository::save)
                            .collectList()
                            .flatMap(paquetesGuardados -> {
                                despachoGuardado.setPaquetes(paquetesGuardados);
                                return reservarCupoSaga(despachoGuardado, paquetesGuardados, totalPeso);
                            });
                })
                .as(transactionalOperator::transactional);
    }

    private Mono<Despacho> reservarCupoSaga(Despacho despacho, List<Paquete> paquetes, int totalPeso) {
        List<Long> reservasRealizadas = Collections.synchronizedList(new ArrayList<>());

        return Flux.fromIterable(paquetes)
                .flatMap(paquete -> vehiculoRepository.findAll()
                        .filter(v -> v.getCiudad().equals(despacho.getCiudad()))
                        .filterWhen(v -> vehiculoRepository.findById(v.getId())
                                .map(vehiculo -> vehiculo.getCupoKg() >= paquete.getPesoKg()))
                        .next()
                        .switchIfEmpty(Mono.error(new CupoInsuficienteException()))
                        .flatMap(vehiculoAsignado -> {
                            paquete.setVehiculoId(vehiculoAsignado.getId());
                            return paqueteRepository.save(paquete)
                                    .doOnSuccess(p -> reservasRealizadas.add(vehiculoAsignado.getId()));
                        }))
                .onErrorResume(error -> {
                    if (error instanceof CupoInsuficienteException) {
                        return liberarReservas(reservasRealizadas)
                                .then(Mono.error(new CupoInsuficienteException()));
                    }
                    return Mono.error(error);
                })
                .collectList()
                .flatMap(paquetesActualizados -> {
                    despacho.setPaquetes(paquetesActualizados);
                    return consultarServiciosExternos(despacho);
                });
    }

    private Mono<Despacho> consultarServiciosExternos(Despacho despacho) {
        return externalService.consultarServicios(
                despacho.getClienteId(),
                despacho.getPaquetes().stream().mapToInt(Paquete::getPesoKg).sum(),
                despacho.getCiudad()
        ).flatMap(resultado -> {
            if (resultado.riesgo().score() > riskThreshold) {
                List<Long> vehiculosReservados = despacho.getPaquetes().stream()
                        .map(Paquete::getVehiculoId)
                        .distinct()
                        .collect(Collectors.toList());

                return liberarReservas(vehiculosReservados)
                        .then(Mono.error(
                                new ZonaRiesgosaException("Score de riesgo: " + resultado.riesgo().score())
                        ));
            }

            despacho.setTarifaTotal(resultado.tarifa().tarifa());
            despacho.setRiskScore(resultado.riesgo().score());
            despacho.setEstado("ASIGNADO");
            despacho.setExpiraEn(Instant.now().plus(reservationTtl));
            despacho.setUpdatedAt(Instant.now());

            return despachoRepository.save(despacho)
                    .flatMap(despachoAsignado -> {
                        eventBus.emit(new DespachoEvent(
                                despachoAsignado.getId(),
                                "ASIGNADO",
                                "ASIGNADO",
                                "Despacho asignado, tarifa: " + resultado.tarifa().tarifa() + 
                                ", riesgo: " + resultado.riesgo().score(),
                                despacho.getCiudad(),
                                despacho.totalPaquetes()
                        ));
                        return Mono.just(despachoAsignado);
                    });
        });
    }

    private Mono<Void> liberarReservas(List<Long> vehiculosReservados) {
        return Flux.fromIterable(vehiculosReservados)
                .distinct()
                .flatMap(vehiculoId -> {
                    return Mono.empty();
                })
                .then();
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
                                new EstadoInvalidoException("Despacho debe estar en estado ASIGNADO")
                        );
                    }

                    despacho.setEstado("EN_RUTA");
                    despacho.setUpdatedAt(Instant.now());

                    return despachoRepository.save(despacho)
                            .flatMap(despachoConfirmado -> {
                                eventBus.emit(new DespachoEvent(
                                        despachoConfirmado.getId(),
                                        "EN_RUTA",
                                        "EN_RUTA",
                                        "Despacho confirmado y en ruta",
                                        despacho.getCiudad(),
                                        despacho.totalPaquetes()
                                ));
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
