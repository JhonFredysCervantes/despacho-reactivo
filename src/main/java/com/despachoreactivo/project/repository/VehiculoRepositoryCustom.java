package com.despachoreactivo.project.repository;

import com.despachoreactivo.project.model.Vehiculo;
import reactor.core.publisher.Mono;

public interface VehiculoRepositoryCustom {

    Mono<Vehiculo> insertar(Vehiculo vehiculo);
}