package com.despachoreactivo.project.controller;

import com.despachoreactivo.project.model.Vehiculo;
import com.despachoreactivo.project.repository.VehiculoRepository;
import com.despachoreactivo.project.service.VehiculoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoRepository vehiculoRepository;
    private final VehiculoService vehiculoService;

    public VehiculoController(
            VehiculoRepository vehiculoRepository,
            VehiculoService vehiculoService) {

        this.vehiculoRepository = vehiculoRepository;
        this.vehiculoService = vehiculoService;
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

    @PutMapping("/{id}/cupo")
    public Mono<Vehiculo> descontarCupo(
            @PathVariable Long id,
            @RequestParam Integer peso) {

        return vehiculoService.descontarCupo(id, peso);
    }

    @PutMapping("/{id}/liberar-cupo")
    public Mono<Vehiculo> liberarCupo(
            @PathVariable Long id,
            @RequestParam Integer peso) {

        return vehiculoService.liberarCupo(id, peso);
    }

    @PutMapping("/{id}/cupo/saga")
    public Mono<Vehiculo> probarSaga(
            @PathVariable Long id,
            @RequestParam Integer peso,
            @RequestParam(defaultValue = "false") boolean simularFallo) {

        return vehiculoService.descontarCupoConCompensacion(
                id,
                peso,
                simularFallo
        );
    }
}