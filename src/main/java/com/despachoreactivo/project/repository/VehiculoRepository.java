package com.despachoreactivo.project.repository;

import com.despachoreactivo.project.model.Vehiculo;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehiculoRepository
        extends ReactiveCrudRepository<Vehiculo, Long>, VehiculoRepositoryCustom {
}