package com.despachoreactivo.project.service;

import com.despachoreactivo.project.event.DespachoEvent;
import com.despachoreactivo.project.event.EventBus;
import com.despachoreactivo.project.exception.DespachoNoExisteException;
import com.despachoreactivo.project.exception.EstadoInvalidoException;
import com.despachoreactivo.project.exception.ValidacionException;
import com.despachoreactivo.project.exception.ZonaRiesgosaException;
import com.despachoreactivo.project.external.ClimaResponse;
import com.despachoreactivo.project.external.RiesgoResponse;
import com.despachoreactivo.project.external.TarifaResponse;
import com.despachoreactivo.project.model.Despacho;
import com.despachoreactivo.project.model.Paquete;
import com.despachoreactivo.project.model.ReservaCupo;
import com.despachoreactivo.project.model.ResultadoReserva;
import com.despachoreactivo.project.repository.DespachoRepository;
import com.despachoreactivo.project.repository.PaqueteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DespachoServiceTest {

    private static final Long DESPACHO_ID = 1L;
    private static final Long CLIENTE_ID = 2L;
    private static final String CIUDAD = "BOG";

    @Mock
    private DespachoRepository despachoRepository;

    @Mock
    private PaqueteRepository paqueteRepository;

    @Mock
    private DespachoExternalService externalService;

    @Mock
    private EventBus eventBus;

    @Mock
    private AsignacionSaga asignacionSaga;

    private final TransactionalOperator transactionalOperator = mock(TransactionalOperator.class);

    private DespachoService despachoService;

    @BeforeEach
    void setUp() {
        despachoService = new DespachoService(
                despachoRepository,
                paqueteRepository,
                externalService,
                eventBus,
                transactionalOperator,
                asignacionSaga);

        ReflectionTestUtils.setField(despachoService, "riskThreshold", 80);
        ReflectionTestUtils.setField(despachoService, "reservationTtl", Duration.ofMinutes(15));

        lenientPassthroughTransactional();
    }

    @SuppressWarnings("unchecked")
    private void lenientPassthroughTransactional() {
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Despacho despachoRequest() {
        Despacho despacho = new Despacho();
        despacho.setClienteId(CLIENTE_ID);
        despacho.setCiudad(CIUDAD);
        Paquete paquete = new Paquete();
        paquete.setPesoKg(120);
        despacho.setPaquetes(new java.util.ArrayList<>(List.of(paquete)));
        return despacho;
    }

    @Test
    void crearDespacho_fallaCuandoCiudadEsNula() {
        Despacho despacho = despachoRequest();
        despacho.setCiudad(null);

        StepVerifier.create(despachoService.crearDespacho(despacho))
                .expectError(ValidacionException.class)
                .verify();

        verify(despachoRepository, never()).save(any());
    }

    @Test
    void crearDespacho_fallaCuandoNoHayPaquetes() {
        Despacho despacho = despachoRequest();
        despacho.setPaquetes(List.of());

        StepVerifier.create(despachoService.crearDespacho(despacho))
                .expectError(ValidacionException.class)
                .verify();

        verify(despachoRepository, never()).save(any());
    }

    @Test
    void crearDespacho_fallaCuandoClienteIdEsNulo() {
        Despacho despacho = despachoRequest();
        despacho.setClienteId(null);

        StepVerifier.create(despachoService.crearDespacho(despacho))
                .expectError(ValidacionException.class)
                .verify();

        verify(despachoRepository, never()).save(any());
    }

    @Test
    void crearDespacho_asignaDespachoCuandoRiesgoEsAceptable() {
        Despacho despacho = despachoRequest();
        Paquete paqueteGuardado = despacho.getPaquetes().get(0);
        paqueteGuardado.setId(10L);

        Despacho despachoRecibido = new Despacho();
        despachoRecibido.setId(DESPACHO_ID);
        despachoRecibido.setCiudad(CIUDAD);
        despachoRecibido.setClienteId(CLIENTE_ID);
        despachoRecibido.setEstado("RECIBIDO");

        when(despachoRepository.save(any(Despacho.class)))
                .thenReturn(Mono.just(despachoRecibido))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(paqueteRepository.save(any(Paquete.class))).thenReturn(Mono.just(paqueteGuardado));

        ReservaCupo reserva = new ReservaCupo(1L, 120);
        ResultadoReserva resultadoReserva =
                new ResultadoReserva(List.of(paqueteGuardado), List.of(reserva));
        when(asignacionSaga.reservarPaquetes(CIUDAD, List.of(paqueteGuardado)))
                .thenReturn(Mono.just(resultadoReserva));

        DespachoExternalService.ResultadoExternos resultadoExternos =
                new DespachoExternalService.ResultadoExternos(
                        new TarifaResponse(15000.0),
                        new ClimaResponse(CIUDAD, "soleado"),
                        new RiesgoResponse(50));
        when(externalService.consultarServicios(CLIENTE_ID, 120, CIUDAD))
                .thenReturn(Mono.just(resultadoExternos));

        StepVerifier.create(despachoService.crearDespacho(despacho))
                .assertNext(resultado -> {
                    assertThat(resultado.getEstado()).isEqualTo("ASIGNADO");
                    assertThat(resultado.getTarifaTotal()).isEqualTo(15000.0);
                    assertThat(resultado.getRiskScore()).isEqualTo(50);
                    assertThat(resultado.getExpiraEn()).isNotNull();
                })
                .verifyComplete();

        verify(asignacionSaga, never()).compensar(any());
        verify(eventBus, org.mockito.Mockito.times(2)).emit(any(DespachoEvent.class));
    }

    @Test
    void crearDespacho_compensaYFallaCuandoZonaEsRiesgosa() {
        Despacho despacho = despachoRequest();
        Paquete paqueteGuardado = despacho.getPaquetes().get(0);
        paqueteGuardado.setId(10L);

        Despacho despachoRecibido = new Despacho();
        despachoRecibido.setId(DESPACHO_ID);
        despachoRecibido.setCiudad(CIUDAD);
        despachoRecibido.setClienteId(CLIENTE_ID);

        when(despachoRepository.save(any(Despacho.class))).thenReturn(Mono.just(despachoRecibido));
        when(paqueteRepository.save(any(Paquete.class))).thenReturn(Mono.just(paqueteGuardado));

        ReservaCupo reserva = new ReservaCupo(1L, 120);
        List<ReservaCupo> reservas = List.of(reserva);
        ResultadoReserva resultadoReserva =
                new ResultadoReserva(List.of(paqueteGuardado), reservas);
        when(asignacionSaga.reservarPaquetes(CIUDAD, List.of(paqueteGuardado)))
                .thenReturn(Mono.just(resultadoReserva));
        when(asignacionSaga.compensar(reservas)).thenReturn(Mono.empty());

        DespachoExternalService.ResultadoExternos resultadoExternos =
                new DespachoExternalService.ResultadoExternos(
                        new TarifaResponse(15000.0),
                        new ClimaResponse(CIUDAD, "soleado"),
                        new RiesgoResponse(95));
        when(externalService.consultarServicios(CLIENTE_ID, 120, CIUDAD))
                .thenReturn(Mono.just(resultadoExternos));

        StepVerifier.create(despachoService.crearDespacho(despacho))
                .expectError(ZonaRiesgosaException.class)
                .verify();

        verify(asignacionSaga).compensar(reservas);
    }

    @Test
    void crearDespacho_compensaCuandoFallaConsultaDeServiciosExternos() {
        Despacho despacho = despachoRequest();
        Paquete paqueteGuardado = despacho.getPaquetes().get(0);
        paqueteGuardado.setId(10L);

        Despacho despachoRecibido = new Despacho();
        despachoRecibido.setId(DESPACHO_ID);
        despachoRecibido.setCiudad(CIUDAD);
        despachoRecibido.setClienteId(CLIENTE_ID);

        when(despachoRepository.save(any(Despacho.class))).thenReturn(Mono.just(despachoRecibido));
        when(paqueteRepository.save(any(Paquete.class))).thenReturn(Mono.just(paqueteGuardado));

        ReservaCupo reserva = new ReservaCupo(1L, 120);
        List<ReservaCupo> reservas = List.of(reserva);
        ResultadoReserva resultadoReserva =
                new ResultadoReserva(List.of(paqueteGuardado), reservas);
        when(asignacionSaga.reservarPaquetes(CIUDAD, List.of(paqueteGuardado)))
                .thenReturn(Mono.just(resultadoReserva));
        when(asignacionSaga.compensar(reservas)).thenReturn(Mono.empty());

        when(externalService.consultarServicios(CLIENTE_ID, 120, CIUDAD))
                .thenReturn(Mono.error(new IllegalStateException("servicio externo caído")));

        StepVerifier.create(despachoService.crearDespacho(despacho))
                .expectErrorMatches(error -> error instanceof IllegalStateException)
                .verify();

        verify(asignacionSaga).compensar(reservas);
    }

    @Test
    void obtenerDespacho_retornaDespachoConPaquetes() {
        Despacho despacho = new Despacho();
        despacho.setId(DESPACHO_ID);
        Paquete paquete = new Paquete();
        paquete.setId(5L);

        when(despachoRepository.findById(DESPACHO_ID)).thenReturn(Mono.just(despacho));
        when(paqueteRepository.findByDespachoId(DESPACHO_ID)).thenReturn(Flux.just(paquete));

        StepVerifier.create(despachoService.obtenerDespacho(DESPACHO_ID))
                .assertNext(resultado -> assertThat(resultado.getPaquetes()).containsExactly(paquete))
                .verifyComplete();
    }

    @Test
    void obtenerDespacho_fallaCuandoNoExiste() {
        when(despachoRepository.findById(DESPACHO_ID)).thenReturn(Mono.empty());

        StepVerifier.create(despachoService.obtenerDespacho(DESPACHO_ID))
                .expectError(DespachoNoExisteException.class)
                .verify();
    }

    @Test
    void confirmarDespacho_actualizaEstadoAEnRuta() {
        Despacho despacho = new Despacho();
        despacho.setId(DESPACHO_ID);
        despacho.setCiudad(CIUDAD);
        despacho.setEstado("ASIGNADO");

        when(despachoRepository.findById(DESPACHO_ID)).thenReturn(Mono.just(despacho));
        when(paqueteRepository.findByDespachoId(DESPACHO_ID)).thenReturn(Flux.empty());
        when(despachoRepository.save(any(Despacho.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(despachoService.confirmarDespacho(DESPACHO_ID))
                .assertNext(resultado -> assertThat(resultado.getEstado()).isEqualTo("EN_RUTA"))
                .verifyComplete();

        verify(eventBus).emit(any(DespachoEvent.class));
    }

    @Test
    void confirmarDespacho_fallaCuandoEstadoNoEsAsignado() {
        Despacho despacho = new Despacho();
        despacho.setId(DESPACHO_ID);
        despacho.setEstado("RECIBIDO");

        when(despachoRepository.findById(DESPACHO_ID)).thenReturn(Mono.just(despacho));
        when(paqueteRepository.findByDespachoId(DESPACHO_ID)).thenReturn(Flux.empty());

        StepVerifier.create(despachoService.confirmarDespacho(DESPACHO_ID))
                .expectError(EstadoInvalidoException.class)
                .verify();

        verify(despachoRepository, never()).save(any());
    }

    @Test
    void obtenerDespachosPorCiudad_filtraSoloLaCiudadSolicitada() {
        Despacho bogota = new Despacho();
        bogota.setId(1L);
        bogota.setCiudad("BOG");
        Despacho medellin = new Despacho();
        medellin.setId(2L);
        medellin.setCiudad("MDE");

        when(despachoRepository.findAll()).thenReturn(Flux.just(bogota, medellin));
        when(paqueteRepository.findByDespachoId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(despachoService.obtenerDespachosPorCiudad("BOG"))
                .assertNext(resultado -> assertThat(resultado.getId()).isEqualTo(1L))
                .verifyComplete();
    }

    @Test
    void obtenerDespachosPorEstado_filtraSoloElEstadoSolicitado() {
        Despacho asignado = new Despacho();
        asignado.setId(1L);
        asignado.setEstado("ASIGNADO");
        Despacho enRuta = new Despacho();
        enRuta.setId(2L);
        enRuta.setEstado("EN_RUTA");

        when(despachoRepository.findAll()).thenReturn(Flux.just(asignado, enRuta));
        when(paqueteRepository.findByDespachoId(1L)).thenReturn(Flux.empty());

        StepVerifier.create(despachoService.obtenerDespachosPorEstado("ASIGNADO"))
                .assertNext(resultado -> assertThat(resultado.getId()).isEqualTo(1L))
                .verifyComplete();
    }
}
