package com.despachoreactivo.project.event;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

import java.util.Comparator;
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
        return tableroHot
                .onBackpressureLatest();
    }

    public Flux<DespachoEvent> getEventsByDespachoId(Long despachoId) {
        return tableroHot
                .filter(event -> event.getDespachoId().equals(despachoId));
    }

    public Flux<ReporteCiudadEvent> reporteCiudadesStream() {
        return tableroHot
                .map(this::toReport)
                .scan(new LinkedHashMap<String, ReporteCiudadEvent>(), (acumulado, actual) -> {
                    LinkedHashMap<String, ReporteCiudadEvent> copia = new LinkedHashMap<>(acumulado);
                    copia.merge(actual.ciudad(), actual, ReporteCiudadEvent::sumar);
                    return copia;
                })
                .skip(1)
                .flatMapIterable(Map::values)
                .onBackpressureLatest();
    }

    public Mono<List<ReporteCiudadEvent>> reporteCiudadesSnapshot() {
        return tableroHot
                .map(this::toReport)
                .reduce(new LinkedHashMap<String, Integer>(), (acumulado, actual) -> {
                    acumulado.merge(actual.ciudad(), actual.totalPaquetes(), Integer::sum);
                    return acumulado;
                })
                .map(this::toReporteCiudadSnapshot);
    }

    private ReporteCiudadEvent toReport(DespachoEvent evento) {
        return new ReporteCiudadEvent(evento.getCiudad(), evento.getTotalPaquetes());
    }

    private List<ReporteCiudadEvent> toReporteCiudadSnapshot(Map<String, Integer> porCiudad) {
        return porCiudad.entrySet().stream()
                .map(entry -> new ReporteCiudadEvent(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ReporteCiudadEvent::ciudad))
                .toList();
    }
}
