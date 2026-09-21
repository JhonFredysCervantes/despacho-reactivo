package com.despachoreactivo.project.external;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/external")
public class ExternalSimulatorController {

    private final SimulatorConfig config = new SimulatorConfig(0, 0, 50);

    @GetMapping("/simulator")
    public Mono<SimulatorConfig> obtenerConfiguracion() {
        return Mono.just(config);
    }

    @PutMapping("/simulator")
    public Mono<SimulatorConfig> actualizarConfiguracion(
            @RequestBody SimulatorConfig nuevaConfig) {

        config.setFallasTarifa(nuevaConfig.getFallasTarifa());
        config.setLatenciaRiesgoMs(nuevaConfig.getLatenciaRiesgoMs());
        config.setScoreRiesgo(nuevaConfig.getScoreRiesgo());

        return Mono.just(config);
    }

    @DeleteMapping("/simulator")
    public Mono<SimulatorConfig> restaurarConfiguracion() {
        config.setFallasTarifa(0);
        config.setLatenciaRiesgoMs(0);
        config.setScoreRiesgo(50);

        return Mono.just(config);
    }

    @GetMapping("/tarifa")
    public Mono<TarifaResponse> tarifa(
            @RequestParam Long clienteId,
            @RequestParam Integer peso) {

        if (config.getFallasTarifa() > 0) {
            config.setFallasTarifa(config.getFallasTarifa() - 1);

            return Mono.error(
                    new RuntimeException("Falla simulada en servicio de tarifa")
            );
        }

        double tarifa = peso * 100.0;

        return Mono.just(new TarifaResponse(tarifa));
    }

    @GetMapping("/clima")
    public Mono<ClimaResponse> clima(
            @RequestParam String ciudad) {

        return Mono.just(
                new ClimaResponse(
                        ciudad,
                        "NORMAL"
                )
        );
    }

    @GetMapping("/riesgo")
    public Mono<RiesgoResponse> riesgo(
            @RequestParam Long clienteId,
            @RequestParam String ciudad) {

        return Mono.delay(
                        java.time.Duration.ofMillis(
                                config.getLatenciaRiesgoMs()
                        )
                )
                .thenReturn(
                        new RiesgoResponse(
                                config.getScoreRiesgo()
                        )
                );
    }
}