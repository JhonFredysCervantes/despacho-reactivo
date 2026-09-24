package com.despachoreactivo.project.controller;

import com.despachoreactivo.project.event.EventBus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

//@RestController
//@RequestMapping("/api/ops")
public class OpsController {

    private final EventBus eventBus;

    public OpsController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @GetMapping(value = "/tablero", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> obtenerTablero(
            @RequestHeader(value = "X-Traza-Id", required = false) String trazaId) {
        return eventBus.getEvents()
                .map(event -> "data: {\"id\":" + event.getDespachoId() + 
                        ",\"tipo\":\"" + event.getTipo() + 
                        "\",\"estado\":\"" + event.getEstado() + 
                        "\",\"mensaje\":\"" + event.getMensaje() + 
                        "\",\"timestamp\":\"" + event.getTimestamp() + "\"}\n\n");
    }
}
