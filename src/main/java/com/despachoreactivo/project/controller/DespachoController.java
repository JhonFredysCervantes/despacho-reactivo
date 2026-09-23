package com.despachoreactivo.project.controller;

import com.despachoreactivo.project.model.Despacho;
import com.despachoreactivo.project.service.DespachoService;
import com.despachoreactivo.project.event.EventBus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/despachos")
public class DespachoController {

    private final DespachoService despachoService;
    private final EventBus eventBus;

    public DespachoController(DespachoService despachoService, EventBus eventBus) {
        this.despachoService = despachoService;
        this.eventBus = eventBus;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Despacho> crearDespacho(
            @Valid @RequestBody Despacho despacho,
            @RequestHeader(value = "X-Traza-Id", required = false) String trazaId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return despachoService.crearDespacho(despacho);
    }

    @GetMapping("/{id}")
    public Mono<Despacho> obtenerDespacho(
            @PathVariable Long id,
            @RequestHeader(value = "X-Traza-Id", required = false) String trazaId) {
        return despachoService.obtenerDespacho(id);
    }

    @PostMapping("/{id}/confirm")
    public Mono<Despacho> confirmarDespacho(
            @PathVariable Long id,
            @RequestHeader(value = "X-Traza-Id", required = true) String trazaId) {
        return despachoService.confirmarDespacho(id);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> obtenerEventosDespacho(
            @PathVariable Long id,
            @RequestHeader(value = "X-Traza-Id", required = true) String trazaId) {
        return eventBus.getEventsByDespachoId(id)
                .takeUntil(event -> "ENTREGADO".equals(event.getEstado()) || "CANCELADO".equals(event.getEstado()))
                .map(event -> "data: {\"id\":" + event.getDespachoId() +
                        ",\"tipo\":\"" + event.getTipo() + 
                        "\",\"estado\":\"" + event.getEstado() + 
                        "\",\"mensaje\":\"" + event.getMensaje() + 
                        "\",\"timestamp\":\"" + event.getTimestamp() + "\"}\n\n");
    }

    @GetMapping(value = "/ciudad/{ciudad}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<Despacho> obtenerDespachosPorCiudad(
            @PathVariable String ciudad,
            @RequestHeader(value = "X-Traza-Id", required = false) String trazaId) {
        return despachoService.obtenerDespachosPorCiudad(ciudad);
    }
}
