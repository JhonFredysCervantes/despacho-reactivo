package com.despachoreactivo.project.external;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/external/simulator")
public class ExternalSimulatorController {

    private final SimulatorConfig config = new SimulatorConfig(0, 0, 50);

    @GetMapping
    public Mono<SimulatorConfig> obtenerConfiguracion() {
        return Mono.just(config);
    }

    @PutMapping
    public Mono<SimulatorConfig> actualizarConfiguracion(
            @RequestBody SimulatorConfig nuevaConfig) {

        config.setFallasTarifa(nuevaConfig.getFallasTarifa());
        config.setLatenciaRiesgoMs(nuevaConfig.getLatenciaRiesgoMs());
        config.setScoreRiesgo(nuevaConfig.getScoreRiesgo());

        return Mono.just(config);
    }

    @DeleteMapping
    public Mono<SimulatorConfig> restaurarConfiguracion() {
        config.setFallasTarifa(0);
        config.setLatenciaRiesgoMs(0);
        config.setScoreRiesgo(50);

        return Mono.just(config);
    }
}