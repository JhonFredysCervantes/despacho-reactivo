package com.despachoreactivo.project.event;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

@Component
public class EventBus {
    
    private final Many<DespachoEvent> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

    public void emit(DespachoEvent event) {
        sink.tryEmitNext(event);
    }

    public Flux<DespachoEvent> getEvents() {
        return sink.asFlux();
    }

    public Flux<DespachoEvent> getEventsByDespachoId(Long despachoId) {
        return sink.asFlux()
                .filter(event -> event.getDespachoId().equals(despachoId));
    }
}
