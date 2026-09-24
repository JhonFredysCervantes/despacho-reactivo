package com.despachoreactivo.project.config;

import com.despachoreactivo.project.event.DespachoEvent;
import com.despachoreactivo.project.event.EventBus;
import com.despachoreactivo.project.model.Despacho;
import com.despachoreactivo.project.model.Paquete;
import com.despachoreactivo.project.repository.DespachoRepository;
import com.despachoreactivo.project.repository.PaqueteRepository;
import com.despachoreactivo.project.service.AsignacionSaga;
import com.despachoreactivo.project.service.AsignacionSaga.ReservaCupo;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ExpiryJobConfig {

    private final DespachoRepository despachoRepository;
    private final PaqueteRepository paqueteRepository;
    private final AsignacionSaga asignacionSaga;
    private final EventBus eventBus;

    private Disposable expirySubscription;

    @Value("${app.expiry-interval:30s}")
    private Duration expiryInterval;

    public ExpiryJobConfig(
            DespachoRepository despachoRepository,
            PaqueteRepository paqueteRepository,
            AsignacionSaga asignacionSaga,
            EventBus eventBus) {
        this.despachoRepository = despachoRepository;
        this.paqueteRepository = paqueteRepository;
        this.asignacionSaga = asignacionSaga;
        this.eventBus = eventBus;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startExpiryJob() {
        expirySubscription = Flux.interval(expiryInterval)
                .subscribeOn(Schedulers.boundedElastic())
                .onBackpressureDrop()
                .flatMap(tick -> despachoRepository.findAll()
                        .filter(d -> "ASIGNADO".equals(d.getEstado()))
                        .filter(d -> d.getExpiraEn() != null && d.getExpiraEn().isBefore(Instant.now()))
                        .concatMap(this::expirarDespacho))
                .subscribe();
    }

    private Mono<Despacho> expirarDespacho(Despacho despacho) {
        return paqueteRepository.findByDespachoId(despacho.getId())
                .collectList()
                .flatMap(paquetes -> {
                    List<ReservaCupo> reservas = paquetes.stream()
                            .filter(p -> p.getVehiculoId() != null && p.getPesoKg() != null)
                            .map(p -> new ReservaCupo(p.getVehiculoId(), p.getPesoKg()))
                            .toList();

                    return asignacionSaga.compensar(reservas)
                            .then(actualizarDespachoCancelado(despacho, paquetes));
                });
    }

    private Mono<Despacho> actualizarDespachoCancelado(Despacho despacho, List<Paquete> paquetes) {
        despacho.setEstado("CANCELADO");
        despacho.setExpiraEn(null);
        despacho.setUpdatedAt(Instant.now());

        int totalPaquetes = paquetes.size();

        return despachoRepository.save(despacho)
                .doOnSuccess(guardado -> eventBus.emit(new DespachoEvent(
                        guardado.getId(),
                        "CANCELADO",
                        "CANCELADO",
                        "Despacho expirado; cupo liberado",
                        guardado.getCiudad(),
                        totalPaquetes)));
    }

    @PreDestroy
    public void detenerJob() {
        if (expirySubscription != null && !expirySubscription.isDisposed()) {
            expirySubscription.dispose();
        }
    }
}
