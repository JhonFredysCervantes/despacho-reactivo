package com.despachoreactivo.project.service;

import com.despachoreactivo.project.exception.CupoInsuficienteException;
import com.despachoreactivo.project.exception.ValidacionException;
import com.despachoreactivo.project.exception.VehiculoNoExisteException;
import com.despachoreactivo.project.model.Paquete;
import com.despachoreactivo.project.repository.PaqueteRepository;
import com.despachoreactivo.project.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AsignacionSaga {

    private final VehiculoRepository vehiculoRepository;
    private final PaqueteRepository paqueteRepository;

    public AsignacionSaga(
            VehiculoRepository vehiculoRepository,
            PaqueteRepository paqueteRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.paqueteRepository = paqueteRepository;
    }

    public record ReservaCupo(Long vehiculoId, Integer pesoKg) {
    }

    public record ResultadoReserva(List<Paquete> paquetes, List<ReservaCupo> reservas) {
    }

    public Mono<ResultadoReserva> reservarPaquetes(String ciudad, List<Paquete> paquetes) {
        List<ReservaCupo> reservasAcumuladas = new ArrayList<>();

        return Flux.fromIterable(paquetes)
                .concatMap(paquete -> descontarYPersistirPaquete(ciudad, paquete)
                        .doOnNext(par -> reservasAcumuladas.add(par.reserva()))
                        .map(PaqueteReservado::paquete))
                .collectList()
                .map(guardados -> new ResultadoReserva(guardados, List.copyOf(reservasAcumuladas)))
                .onErrorResume(error -> compensar(new ArrayList<>(reservasAcumuladas))
                        .then(Mono.error(error)));
    }

    public Mono<Void> compensar(List<ReservaCupo> reservas) {
        if (reservas == null || reservas.isEmpty()) {
            return Mono.empty();
        }

        List<ReservaCupo> enOrdenInverso = new ArrayList<>(reservas);
        Collections.reverse(enOrdenInverso);

        return Flux.fromIterable(enOrdenInverso)
                .concatMap(reserva -> vehiculoRepository
                        .liberarCupo(reserva.vehiculoId(), reserva.pesoKg())
                        .then())
                .then();
    }

    private Mono<PaqueteReservado> descontarYPersistirPaquete(String ciudad, Paquete paquete) {
        Integer peso = paquete.getPesoKg();
        if (peso == null || peso <= 0) {
            return Mono.error(new ValidacionException("pesoKg debe ser positivo"));
        }

        return resolverVehiculoYDescontar(ciudad, paquete, peso)
                .flatMap(vehiculoId -> {
                    paquete.setVehiculoId(vehiculoId);
                    if (paquete.getEstado() == null || paquete.getEstado().isBlank()) {
                        paquete.setEstado("RESERVADO");
                    }
                    return paqueteRepository.save(paquete)
                            .map(guardado -> new PaqueteReservado(
                                    guardado,
                                    new ReservaCupo(vehiculoId, peso)));
                });
    }

    private Mono<Long> resolverVehiculoYDescontar(String ciudad, Paquete paquete, int peso) {
        if (paquete.getVehiculoId() != null) {
            return descontarEnVehiculoExplicito(ciudad, paquete.getVehiculoId(), peso);
        }
        return descontarEnPrimerVehiculoDisponible(ciudad, peso);
    }

    private Mono<Long> descontarEnVehiculoExplicito(String ciudad, Long vehiculoId, int peso) {
        return vehiculoRepository.findById(vehiculoId)
                .switchIfEmpty(Mono.error(new VehiculoNoExisteException()))
                .flatMap(vehiculo -> {
                    if (!ciudad.equals(vehiculo.getCiudad())) {
                        return Mono.error(new ValidacionException(
                                "El vehículo " + vehiculoId + " no opera en la ciudad " + ciudad));
                    }
                    return vehiculoRepository.descontarCupo(vehiculoId, peso)
                            .switchIfEmpty(Mono.error(new CupoInsuficienteException()))
                            .map(v -> vehiculoId);
                });
    }

    private Mono<Long> descontarEnPrimerVehiculoDisponible(String ciudad, int peso) {
        return vehiculoRepository.findAll()
                .filter(v -> ciudad.equals(v.getCiudad()))
                .concatMap(v -> vehiculoRepository.descontarCupo(v.getId(), peso)
                        .map(vehiculo -> v.getId()))
                .next()
                .switchIfEmpty(Mono.error(new CupoInsuficienteException()));
    }

    private record PaqueteReservado(Paquete paquete, ReservaCupo reserva) {
    }
}
