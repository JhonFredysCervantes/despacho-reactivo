package com.despachoreactivo.project.controller;

import com.despachoreactivo.project.event.DespachoEvent;
import com.despachoreactivo.project.event.EventBus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/ops")
public class TableroController {
    private final EventBus eventBus;

    public TableroController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @GetMapping(value = "/tablero", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<DespachoEvent>> tablero() {
        return eventBus.getEvents()
                .map(evento -> ServerSentEvent.<DespachoEvent>builder()
                        .event("despacho")
                        .data(evento)
                        .build());
    }
}
