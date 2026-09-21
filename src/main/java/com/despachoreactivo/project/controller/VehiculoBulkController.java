package com.despachoreactivo.project.controller;

import com.despachoreactivo.project.model.Vehiculo;
import com.despachoreactivo.project.service.VehiculoBulkService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoBulkController {

    private final VehiculoBulkService bulkService;
    //private final ObjectMapper objectMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VehiculoBulkController(VehiculoBulkService bulkService) {
        this.bulkService = bulkService;
    }

    @PostMapping(
            value = "/bulk",
            consumes = MediaType.APPLICATION_NDJSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<VehiculoBulkService.BulkResult> cargar(
            @RequestBody String body) {

        Flux<Vehiculo> vehiculos = Flux.fromArray(body.split("\\R"))
                .filter(linea -> !linea.isBlank())
                .map(linea -> {
                    try {
                        return objectMapper.readValue(linea, Vehiculo.class);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                                "JSON de vehículo inválido: " + linea, e);
                    }
                });

        return bulkService.procesar(vehiculos);
    }
}