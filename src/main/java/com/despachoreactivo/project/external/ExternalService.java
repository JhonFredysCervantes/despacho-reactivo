package com.despachoreactivo.project.external;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExternalService {

    private final WebClient webClient;
    private final Map<String, Mono<ClimaResponse>> climaCache =
            new ConcurrentHashMap<>();
    public ExternalService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("http://localhost:8081")
                .build();
    }

    public Mono<TarifaResponse> consultarTarifa(Long clienteId, Integer peso) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/external/tarifa")
                        .queryParam("clienteId", clienteId)
                        .queryParam("peso", peso)
                        .build())
                .retrieve()
                .bodyToMono(TarifaResponse.class)
                .retryWhen(
                        reactor.util.retry.Retry
                                .backoff(3, java.time.Duration.ofMillis(200))
                                .jitter(0.5)
                )
                .onErrorReturn(new TarifaResponse(0.0));
    }

    public Mono<ClimaResponse> consultarClima(String ciudad) {

        return climaCache.computeIfAbsent(
                ciudad,
                key -> webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/external/clima")
                                .queryParam("ciudad", key)
                                .build())
                        .retrieve()
                        .bodyToMono(ClimaResponse.class)
                        .cache(java.time.Duration.ofMinutes(10))
        );
    }

    public Mono<RiesgoResponse> consultarRiesgo(
            Long clienteId,
            String ciudad) {

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/external/riesgo")
                        .queryParam("clienteId", clienteId)
                        .queryParam("ciudad", ciudad)
                        .build())
                .retrieve()
                .bodyToMono(RiesgoResponse.class)
                .timeout(java.time.Duration.ofMillis(800))
                .onErrorReturn(new RiesgoResponse(50));
    }
}