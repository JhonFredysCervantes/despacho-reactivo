package com.despachoreactivo.project.controller;

import com.despachoreactivo.project.event.EventBus;
import com.despachoreactivo.project.event.ReporteCiudadEvent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReporteController {

    private final EventBus eventBus;

    public ReporteController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @GetMapping("/ciudades")
    public Mono<List<ReporteCiudadEvent>> reporteCiudades() {
        return eventBus.reporteCiudadesSnapshot();
    }

    @GetMapping(value = "/ciudades/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<ReporteCiudadEvent> reporteCiudadesStream() {
        return eventBus.reporteCiudadesStream();
    }
}
