package com.despachoreactivo.project.event;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EventBus {
    
    private final Many<DespachoEvent> sink = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

    private final Flux<DespachoEvent> tableroHot = sink.asFlux()
            .onBackpressureLatest()
            .publish()
            .refCount();

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

    public Flux<ReporteCiudadEvent> reporteCiudadesStream() {
        return tableroHot
                .map(this::toReport)
                .scan(new LinkedHashMap<String, ReporteCiudadEvent>(), (acumulado, actual) -> {
                    LinkedHashMap<String, ReporteCiudadEvent> copia = new LinkedHashMap<>(acumulado);
                    ReporteCiudadEvent existente = copia.get(actual.ciudad());
                    copia.put(actual.ciudad(), existente == null ? actual : existente.sumar(actual));
                    return copia;
                })
                .skip(1)
                .map(mapa -> mapa.get(mapa.keySet().stream().reduce((a, b) -> b).orElse("")))
                .filter(reporte -> reporte != null);
    }

    public Mono<List<ReporteCiudadEvent>> reporteCiudadesSnapshot() {
        return tableroHot
                .filter(evento -> evento.getEstado().equals("ENTREGADO"))
                .take(10)
                .map(this::toReport)
                .limitRate(10)
                .collectMultimap(ReporteCiudadEvent::ciudad)
                .map(this::sumarPorCiudad);
    }

    private ReporteCiudadEvent toReport(DespachoEvent evento) {
        return new ReporteCiudadEvent(evento.getCiudad(), evento.getTotalPaquetes());
    }

    private List<ReporteCiudadEvent> sumarPorCiudad(Map<String, Collection<ReporteCiudadEvent>> porCiudad) {
        return porCiudad.entrySet().stream()
                .map(entry -> entry.getValue().stream()
                        .reduce(new ReporteCiudadEvent(entry.getKey(), 0), ReporteCiudadEvent::sumar))
                .toList();
    }
}
