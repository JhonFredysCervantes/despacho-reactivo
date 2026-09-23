package com.despachoreactivo.project.service;

import com.despachoreactivo.project.model.Vehiculo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;

import java.util.Map;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculoBulkServiceTest {

    @Mock
    private DatabaseClient databaseClient;

    @Test
    void procesar_fluxVacio_devuelveResultadoEnCero() {
        VehiculoBulkService service = new VehiculoBulkService(databaseClient);

        StepVerifier.create(service.procesar(Flux.empty()))
                .assertNext(resultado -> {
                    assertThat(resultado.getProcesados()).isZero();
                    assertThat(resultado.getAfectados()).isZero();
                })
                .verifyComplete();
    }

    @Test
    void procesar_unVehiculo_ejecutaUpsertYLote() {
        DatabaseClient.GenericExecuteSpec executeSpec =
                mock(DatabaseClient.GenericExecuteSpec.class);
        @SuppressWarnings("unchecked")
        org.springframework.r2dbc.core.FetchSpec<Map<String, Object>> fetchSpec =
                mock(org.springframework.r2dbc.core.FetchSpec.class);

        when(databaseClient.sql(anyString())).thenReturn(executeSpec);
        when(executeSpec.bind(anyString(), any())).thenReturn(executeSpec);
        when(executeSpec.fetch()).thenReturn(fetchSpec);
        when(fetchSpec.rowsUpdated()).thenReturn(Mono.just(1L));

        VehiculoBulkService service = new VehiculoBulkService(databaseClient);
        Vehiculo vehiculo = new Vehiculo(1L, "XYZ999", "Cali", 800, 50);

        StepVerifier.create(service.procesar(Flux.just(vehiculo)))
                .assertNext(resultado -> {
                    assertThat(resultado.getProcesados()).isEqualTo(1);
                    assertThat(resultado.getAfectados()).isEqualTo(1);
                })
                .verifyComplete();
    }

    @Test
    void bulkResult_sumar_acumulaContadores() {
        VehiculoBulkService.BulkResult acumulado =
                new VehiculoBulkService.BulkResult(100, 80);

        acumulado.sumar(new VehiculoBulkService.BulkResult(50, 45));

        assertThat(acumulado.getProcesados()).isEqualTo(150);
        assertThat(acumulado.getAfectados()).isEqualTo(125);
    }
}
