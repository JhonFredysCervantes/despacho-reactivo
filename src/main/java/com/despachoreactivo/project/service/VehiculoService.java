package com.despachoreactivo.project.service;

import com.despachoreactivo.project.exception.CupoInsuficienteException;
import com.despachoreactivo.project.exception.VehiculoNoExisteException;
import com.despachoreactivo.project.exception.ZonaRiesgosaException;
import com.despachoreactivo.project.model.Vehiculo;
import com.despachoreactivo.project.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final DespachoExternalService despachoExternalService;

    public VehiculoService(
            VehiculoRepository vehiculoRepository,
            DespachoExternalService despachoExternalService) {

        this.vehiculoRepository = vehiculoRepository;
        this.despachoExternalService = despachoExternalService;
    }

    public Mono<Vehiculo> descontarCupo(Long id, Integer peso) {

        return vehiculoRepository.findById(id)
                .switchIfEmpty(Mono.error(new VehiculoNoExisteException()))
                .flatMap(vehiculo ->
                        vehiculoRepository.descontarCupo(id, peso)
                                .switchIfEmpty(Mono.error(
                                        new CupoInsuficienteException()
                                ))
                );
    }

    public Mono<Vehiculo> liberarCupo(Long id, Integer peso) {

        return vehiculoRepository.findById(id)
                .switchIfEmpty(Mono.error(new VehiculoNoExisteException()))
                .flatMap(vehiculo ->
                        vehiculoRepository.liberarCupo(id, peso)
                );
    }
    public Mono<Vehiculo> descontarCupoConCompensacion(
            Long id,
            Integer peso,
            boolean simularFallo) {

        return descontarCupo(id, peso)
                .flatMap(vehiculo -> {

                    if (simularFallo) {
                        return liberarCupo(id, peso)
                                .then(Mono.error(
                                        new RuntimeException("Fallo simulado después de descontar cupo")
                                ));
                    }

                    return Mono.just(vehiculo);
                });
    }

    public Mono<DespachoExternalService.ResultadoExternos>
    validarServiciosConCupo(
            Long vehiculoId,
            Integer peso,
            Long clienteId,
            String ciudad) {

        return descontarCupo(vehiculoId, peso)
                .flatMap(vehiculo ->
                        despachoExternalService
                                .consultarServicios(clienteId, peso, ciudad)
                                .flatMap(resultado -> {

                                    if (resultado.riesgo().score() > 80) {

                                        return liberarCupo(vehiculoId, peso)
                                                .then(Mono.error(
                                                        new ZonaRiesgosaException()
                                                ));
                                    }

                                    return Mono.just(resultado);
                                })
                                .onErrorResume(
                                        error -> !(error instanceof ZonaRiesgosaException),
                                        error -> liberarCupo(vehiculoId, peso)
                                                .then(Mono.error(error))
                                )
                );
    }
}