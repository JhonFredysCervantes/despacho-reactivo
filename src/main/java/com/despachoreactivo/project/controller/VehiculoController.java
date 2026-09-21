package com.despachoreactivo.project.controller;

import com.despachoreactivo.project.model.Vehiculo;
import com.despachoreactivo.project.repository.VehiculoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoRepository vehiculoRepository;

    public VehiculoController(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    @GetMapping
    public Flux<Vehiculo> listar() {
        return vehiculoRepository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<Vehiculo> buscarPorId(@PathVariable Long id) {
        return vehiculoRepository.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Vehiculo> crear(@RequestBody Vehiculo vehiculo) {
        return vehiculoRepository.insertar(vehiculo);
    }
}