package com.despachoreactivo.project.config;

import com.despachoreactivo.project.event.DespachoEvent;
import com.despachoreactivo.project.event.EventBus;
import com.despachoreactivo.project.repository.DespachoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.Duration;

@Component
public class ExpiryJobConfig {

    private final DespachoRepository despachoRepository;
    private final EventBus eventBus;

    @Value("${app.expiry-interval:30s}")
    private Duration expiryInterval;

    public ExpiryJobConfig(DespachoRepository despachoRepository, EventBus eventBus) {
        this.despachoRepository = despachoRepository;
        this.eventBus = eventBus;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startExpiryJob() {
        Flux.interval(expiryInterval)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(tick -> despachoRepository.findAll()
                        .filter(despacho -> "ASIGNADO".equals(despacho.getEstado()))
                        .filter(despacho -> despacho.getExpiraEn() != null && 
                                despacho.getExpiraEn().isBefore(Instant.now()))
                        .doOnNext(despacho -> {
                            despacho.setEstado("CANCELADO");
                            despacho.setUpdatedAt(Instant.now());
                        })
                        .flatMap(despachoRepository::save)
                        .doOnNext(despacho -> {
                            eventBus.emit(new DespachoEvent(
                                    despacho.getId(),
                                    "CANCELADO",
                                    "CANCELADO",
                                    "Despacho expirado por tiempo de espera",
                                    despacho.getCiudad(),
                                    despacho.totalPaquetes()
                            ));
                        })
                )
                .subscribe();
    }
}
