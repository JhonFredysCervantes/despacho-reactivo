package com.despachoreactivo.project.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class EventBusTest {

    private static final Long DESPACHO_ID_1 = 1L;
    private static final Long DESPACHO_ID_2 = 2L;
    private static final String CIUDAD_BOGOTA = "Bogota";
    private static final String CIUDAD_MEDELLIN = "Medellin";

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
    }

    @Test
    void emit_debeAñadirEventoAlSink() {
        var event = new DespachoEvent(DESPACHO_ID_1, "RECIBIDO", "RECIBIDO", "Paquete recibido", CIUDAD_BOGOTA, 5);

        eventBus.emit(event);

        StepVerifier.create(eventBus.getEvents().take(1))
                .expectNext(event)
                .verifyComplete();
    }

    @Test
    void emit_debeActualizarEstadoCiudadesAlEmitirEventoRecibido() {
        var event = new DespachoEvent(DESPACHO_ID_1, "RECIBIDO", "RECIBIDO", "Paquete recibido", CIUDAD_BOGOTA, 5);

        eventBus.emit(event);

        StepVerifier.create(eventBus.reporteCiudadesSnapshot())
                .assertNext(snapshot -> {
                    assertThat(snapshot).hasSize(1);
                    assertThat(snapshot.get(0).ciudad()).isEqualTo(CIUDAD_BOGOTA);
                    assertThat(snapshot.get(0).totalPaquetes()).isEqualTo(5);
                })
                .verifyComplete();
    }

    @Test
    void emit_noDebeActualizarEstadoParaEventosNoRecibidos() {
        var event = new DespachoEvent(DESPACHO_ID_1, "ENTREGADO", "ENTREGADO", "Paquete entregado", CIUDAD_BOGOTA, 5);

        eventBus.emit(event);

        StepVerifier.create(eventBus.reporteCiudadesSnapshot())
                .assertNext(snapshot -> assertThat(snapshot).isEmpty())
                .verifyComplete();
    }

    @Test
    void getEvents_debeRetornarFluxInfinito() {
        var event1 = new DespachoEvent(DESPACHO_ID_1, "RECIBIDO", "RECIBIDO", "Primer evento", CIUDAD_BOGOTA, 2);
        var event2 = new DespachoEvent(DESPACHO_ID_2, "RECIBIDO", "RECIBIDO", "Segundo evento", CIUDAD_MEDELLIN, 3);

        StepVerifier.create(eventBus.getEvents().take(2))
                .then(() -> eventBus.emit(event1))  // Emitir DESPUÉS de suscribirse
                .expectNext(event1)
                .then(() -> eventBus.emit(event2))
                .expectNext(event2)
                .verifyComplete();
    }

    @Test
    void getEventsByDespachoId_debeFilterarEventosPorId() {
        var event1 = new DespachoEvent(DESPACHO_ID_1, "RECIBIDO", "RECIBIDO", "Evento 1", CIUDAD_BOGOTA, 5);
        var event2 = new DespachoEvent(DESPACHO_ID_2, "RECIBIDO", "RECIBIDO", "Evento 2", CIUDAD_MEDELLIN, 3);

        eventBus.emit(event1);
        eventBus.emit(event2);

        StepVerifier.create(eventBus.getEventsByDespachoId(DESPACHO_ID_1).take(1))
                .expectNext(event1)
                .verifyComplete();
    }

    @Test
    void getEventsByDespachoId_debeFiltraBienMultiplosEventos() {
        var event1 = new DespachoEvent(DESPACHO_ID_1, "RECIBIDO", "RECIBIDO", "Evento 1", CIUDAD_BOGOTA, 5);
        var event2 = new DespachoEvent(DESPACHO_ID_1, "ASIGNADO", "ASIGNADO", "Evento 1B", CIUDAD_BOGOTA, 5);
        var event3 = new DespachoEvent(DESPACHO_ID_2, "RECIBIDO", "RECIBIDO", "Evento 2", CIUDAD_MEDELLIN, 3);

        eventBus.emit(event1);
        eventBus.emit(event2);
        eventBus.emit(event3);

        StepVerifier.create(eventBus.getEventsByDespachoId(DESPACHO_ID_1).take(2))
                .expectNext(event1, event2)
                .verifyComplete();
    }

    @Test
    void reporteCiudadesSnapshot_debeRetornarListaOrdenada() {
        var event1 = new DespachoEvent(DESPACHO_ID_1, "RECIBIDO", "RECIBIDO", "Evento 1", CIUDAD_MEDELLIN, 5);
        var event2 = new DespachoEvent(DESPACHO_ID_2, "RECIBIDO", "RECIBIDO", "Evento 2", CIUDAD_BOGOTA, 3);

        eventBus.emit(event1);
        eventBus.emit(event2);

        StepVerifier.create(eventBus.reporteCiudadesSnapshot())
                .assertNext(snapshot -> {
                    assertThat(snapshot).hasSize(2);
                    assertThat(snapshot.get(0).ciudad()).isEqualTo(CIUDAD_BOGOTA);
                    assertThat(snapshot.get(1).ciudad()).isEqualTo(CIUDAD_MEDELLIN);
                })
                .verifyComplete();
    }

    @Test
    void reporteCiudadesSnapshot_debeAcumularPaquetesPorCiudad() {
        var event1 = new DespachoEvent(DESPACHO_ID_1, "RECIBIDO", "RECIBIDO", "Evento 1", CIUDAD_BOGOTA, 5);
        var event2 = new DespachoEvent(DESPACHO_ID_2, "RECIBIDO", "RECIBIDO", "Evento 2", CIUDAD_BOGOTA, 3);
        var event3 = new DespachoEvent(DESPACHO_ID_1, "RECIBIDO", "RECIBIDO", "Evento 3", CIUDAD_MEDELLIN, 2);

        eventBus.emit(event1);
        eventBus.emit(event2);
        eventBus.emit(event3);

        StepVerifier.create(eventBus.reporteCiudadesSnapshot())
                .assertNext(snapshot -> {
                    assertThat(snapshot).hasSize(2);
                    var bogota = snapshot.stream()
                            .filter(r -> r.ciudad().equals(CIUDAD_BOGOTA))
                            .findFirst()
                            .orElseThrow();
                    assertThat(bogota.totalPaquetes()).isEqualTo(8);
                })
                .verifyComplete();
    }

    @Test
    void reporteCiudadesSnapshot_debeRetornarListaVaciaAlNoHaberEventos() {
        StepVerifier.create(eventBus.reporteCiudadesSnapshot())
                .assertNext(snapshot -> assertThat(snapshot).isEmpty())
                .verifyComplete();
    }

    @Test
    void reporteCiudadesStream_debeEmitirEventosDeCiudad() {
        var event1 = new DespachoEvent(DESPACHO_ID_1, "RECIBIDO", "RECIBIDO", "Evento 1", CIUDAD_BOGOTA, 5);
        var event2 = new DespachoEvent(DESPACHO_ID_2, "RECIBIDO", "RECIBIDO", "Evento 2", CIUDAD_BOGOTA, 3);

        eventBus.emit(event1);
        eventBus.emit(event2);

        StepVerifier.create(eventBus.reporteCiudadesStream().take(2))
                .assertNext(report -> assertThat(report.ciudad()).isEqualTo(CIUDAD_BOGOTA))
                .assertNext(report -> {
                    assertThat(report.ciudad()).isEqualTo(CIUDAD_BOGOTA);
                    assertThat(report.totalPaquetes()).isEqualTo(8);
                })
                .verifyComplete();
    }

    @Test
    void multipleEmits_debeMantenerseLaConexion() {
        var event1 = new DespachoEvent(DESPACHO_ID_1, "RECIBIDO", "RECIBIDO", "Evento 1", CIUDAD_BOGOTA, 5);
        var event2 = new DespachoEvent(DESPACHO_ID_2, "RECIBIDO", "RECIBIDO", "Evento 2", CIUDAD_BOGOTA, 3);
        var event3 = new DespachoEvent(DESPACHO_ID_1, "ENTREGADO", "ENTREGADO", "Evento 3", CIUDAD_BOGOTA, 5);

        eventBus.emit(event1);

        StepVerifier.create(eventBus.getEvents().take(3))
                .expectNext(event1)
                .then(() -> eventBus.emit(event2))
                .expectNext(event2)
                .then(() -> eventBus.emit(event3))
                .expectNext(event3)
                .verifyComplete();
    }
}