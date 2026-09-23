package com.despachoreactivo.project.controller;

import com.despachoreactivo.project.event.EventBus;
import com.despachoreactivo.project.service.ReportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final EventBus eventBus;

    public ReportController(ReportService reportService, EventBus eventBus) {
        this.reportService = reportService;
        this.eventBus = eventBus;
    }

    @GetMapping(value = "/ciudades", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, ReportService.ReporteCiudad>> obtenerReporteCiudades(
            @RequestHeader(value = "X-Traza-Id", required = false) String trazaId) {
        return reportService.obtenerReportePorCiudad();
    }

    @GetMapping(value = "/ciudades/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<String> obtenerReporteCiudadesStream(
            @RequestHeader(value = "X-Traza-Id", required = false) String trazaId) {
        return reportService.obtenerReportePorCiudadStream()
                .map(reporte -> "{\"ciudad\":\"" + reporte.ciudad + 
                        "\",\"pesoKgTotal\":" + reporte.pesoKgTotal + 
                        ",\"valorTotal\":" + reporte.valorTotal + "}\n");
    }
}
