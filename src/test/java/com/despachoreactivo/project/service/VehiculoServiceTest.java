package com.despachoreactivo.project.service;

import com.despachoreactivo.project.exception.CupoInsuficienteException;
import com.despachoreactivo.project.exception.VehiculoNoExisteException;
import com.despachoreactivo.project.exception.ZonaRiesgosaException;
import com.despachoreactivo.project.external.ClimaResponse;
import com.despachoreactivo.project.external.RiesgoResponse;
import com.despachoreactivo.project.external.TarifaResponse;
import com.despachoreactivo.project.model.Vehiculo;
import com.despachoreactivo.project.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

    private static final Long VEHICULO_ID = 1L;
    private static final Integer PESO = 100;
    private static final Long CLIENTE_ID = 2L;
    private static final String CIUDAD = "Bogota";

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private DespachoExternalService despachoExternalService;

    @InjectMocks
    private VehiculoService vehiculoService;

    private Vehiculo vehiculo;

    @BeforeEach
    void setUp() {
        vehiculo = new Vehiculo(VEHICULO_ID, "ABC123", CIUDAD, 1000, 0);
    }

    @Test
    void descontarCupo_devuelveVehiculoCuandoExisteYCupoDisponible() {
        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, PESO)).thenReturn(Mono.just(vehiculo));

        StepVerifier.create(vehiculoService.descontarCupo(VEHICULO_ID, PESO))
                .expectNext(vehiculo)
                .verifyComplete();
    }

    @Test
    void descontarCupo_fallaCuandoVehiculoNoExiste() {
        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.empty());

        StepVerifier.create(vehiculoService.descontarCupo(VEHICULO_ID, PESO))
                .expectError(VehiculoNoExisteException.class)
                .verify();

        verify(vehiculoRepository, never()).descontarCupo(VEHICULO_ID, PESO);
    }

    @Test
    void descontarCupo_fallaCuandoCupoInsuficiente() {
        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, PESO)).thenReturn(Mono.empty());

        StepVerifier.create(vehiculoService.descontarCupo(VEHICULO_ID, PESO))
                .expectError(CupoInsuficienteException.class)
                .verify();
    }

    @Test
    void liberarCupo_devuelveVehiculoCuandoExiste() {
        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.liberarCupo(VEHICULO_ID, PESO)).thenReturn(Mono.just(vehiculo));

        StepVerifier.create(vehiculoService.liberarCupo(VEHICULO_ID, PESO))
                .expectNext(vehiculo)
                .verifyComplete();
    }

    @Test
    void liberarCupo_fallaCuandoVehiculoNoExiste() {
        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.empty());

        StepVerifier.create(vehiculoService.liberarCupo(VEHICULO_ID, PESO))
                .expectError(VehiculoNoExisteException.class)
                .verify();

        verify(vehiculoRepository, never()).liberarCupo(VEHICULO_ID, PESO);
    }

    @Test
    void descontarCupoConCompensacion_devuelveVehiculoSinSimularFallo() {
        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, PESO)).thenReturn(Mono.just(vehiculo));

        StepVerifier.create(
                        vehiculoService.descontarCupoConCompensacion(VEHICULO_ID, PESO, false))
                .expectNext(vehiculo)
                .verifyComplete();

        verify(vehiculoRepository, never()).liberarCupo(VEHICULO_ID, PESO);
    }

    @Test
    void descontarCupoConCompensacion_liberaCupoYFallaCuandoSimularFallo() {
        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, PESO)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.liberarCupo(VEHICULO_ID, PESO)).thenReturn(Mono.just(vehiculo));

        StepVerifier.create(
                        vehiculoService.descontarCupoConCompensacion(VEHICULO_ID, PESO, true))
                .expectErrorMatches(error ->
                        error instanceof RuntimeException
                                && error.getMessage()
                                        .contains("Fallo simulado después de descontar cupo"))
                .verify();

        verify(vehiculoRepository).liberarCupo(VEHICULO_ID, PESO);
    }

    @Test
    void validarServiciosConCupo_devuelveResultadoCuandoRiesgoEsAceptable() {
        var resultadoExternos = resultadoExternos(50);

        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, PESO)).thenReturn(Mono.just(vehiculo));
        when(despachoExternalService.consultarServicios(CLIENTE_ID, PESO, CIUDAD))
                .thenReturn(Mono.just(resultadoExternos));

        StepVerifier.create(
                        vehiculoService.validarServiciosConCupo(
                                VEHICULO_ID, PESO, CLIENTE_ID, CIUDAD))
                .expectNext(resultadoExternos)
                .verifyComplete();

        verify(vehiculoRepository, never()).liberarCupo(VEHICULO_ID, PESO);
    }

    @Test
    void validarServiciosConCupo_liberaCupoCuandoZonaEsRiesgosa() {
        var resultadoExternos = resultadoExternos(85);

        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, PESO)).thenReturn(Mono.just(vehiculo));
        when(despachoExternalService.consultarServicios(CLIENTE_ID, PESO, CIUDAD))
                .thenReturn(Mono.just(resultadoExternos));
        when(vehiculoRepository.liberarCupo(VEHICULO_ID, PESO)).thenReturn(Mono.just(vehiculo));

        StepVerifier.create(
                        vehiculoService.validarServiciosConCupo(
                                VEHICULO_ID, PESO, CLIENTE_ID, CIUDAD))
                .expectError(ZonaRiesgosaException.class)
                .verify();

        verify(vehiculoRepository).liberarCupo(VEHICULO_ID, PESO);
    }

    @Test
    void validarServiciosConCupo_liberaCupoCuandoFallaServicioExterno() {
        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, PESO)).thenReturn(Mono.just(vehiculo));
        when(despachoExternalService.consultarServicios(CLIENTE_ID, PESO, CIUDAD))
                .thenReturn(Mono.error(new IllegalStateException("Servicio externo caído")));
        when(vehiculoRepository.liberarCupo(VEHICULO_ID, PESO)).thenReturn(Mono.just(vehiculo));

        StepVerifier.create(
                        vehiculoService.validarServiciosConCupo(
                                VEHICULO_ID, PESO, CLIENTE_ID, CIUDAD))
                .expectErrorMatches(error ->
                        error instanceof IllegalStateException
                                && error.getMessage().equals("Servicio externo caído"))
                .verify();

        verify(vehiculoRepository).liberarCupo(VEHICULO_ID, PESO);
    }

    private static DespachoExternalService.ResultadoExternos resultadoExternos(int scoreRiesgo) {
        return new DespachoExternalService.ResultadoExternos(
                new TarifaResponse(12.5),
                new ClimaResponse(CIUDAD, "soleado"),
                new RiesgoResponse(scoreRiesgo));
    }
}
