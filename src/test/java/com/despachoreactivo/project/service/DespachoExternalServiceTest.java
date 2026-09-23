package com.despachoreactivo.project.service;

import com.despachoreactivo.project.external.ClimaResponse;
import com.despachoreactivo.project.external.ExternalService;
import com.despachoreactivo.project.external.RiesgoResponse;
import com.despachoreactivo.project.external.TarifaResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DespachoExternalServiceTest {

    @Mock
    private ExternalService externalService;

    @InjectMocks
    private DespachoExternalService despachoExternalService;

    @Test
    void consultarServicios_agrupaTarifaClimaYRiesgoEnParalelo() {
        Long clienteId = 10L;
        Integer peso = 250;
        String ciudad = "Medellin";

        when(externalService.consultarTarifa(clienteId, peso))
                .thenReturn(Mono.just(new TarifaResponse(18.0)));
        when(externalService.consultarClima(ciudad))
                .thenReturn(Mono.just(new ClimaResponse(ciudad, "lluvia")));
        when(externalService.consultarRiesgo(clienteId, ciudad))
                .thenReturn(Mono.just(new RiesgoResponse(42)));

        StepVerifier.create(despachoExternalService.consultarServicios(clienteId, peso, ciudad))
                .assertNext(resultado -> {
                    assertThat(resultado.tarifa().tarifa()).isEqualTo(18.0);
                    assertThat(resultado.clima().ciudad()).isEqualTo(ciudad);
                    assertThat(resultado.clima().estado()).isEqualTo("lluvia");
                    assertThat(resultado.riesgo().score()).isEqualTo(42);
                })
                .verifyComplete();
    }

    @Test
    void consultarServicios_propagaErrorCuandoFallaUnExterno() {
        when(externalService.consultarTarifa(1L, 50))
                .thenReturn(Mono.error(new RuntimeException("tarifa no disponible")));
        when(externalService.consultarClima("Cali"))
                .thenReturn(Mono.just(new ClimaResponse("Cali", "nublado")));
        when(externalService.consultarRiesgo(1L, "Cali"))
                .thenReturn(Mono.just(new RiesgoResponse(10)));

        StepVerifier.create(despachoExternalService.consultarServicios(1L, 50, "Cali"))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException
                                && error.getMessage().equals("tarifa no disponible"))
                .verify();
    }
}
