package com.despachoreactivo.project.service;

import com.despachoreactivo.project.exception.CupoInsuficienteException;
import com.despachoreactivo.project.exception.ValidacionException;
import com.despachoreactivo.project.exception.VehiculoNoExisteException;
import com.despachoreactivo.project.model.Paquete;
import com.despachoreactivo.project.model.ReservaCupo;
import com.despachoreactivo.project.model.Vehiculo;
import com.despachoreactivo.project.repository.PaqueteRepository;
import com.despachoreactivo.project.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionSagaTest {

    private static final String CIUDAD = "BOG";
    private static final Long VEHICULO_ID = 1L;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private PaqueteRepository paqueteRepository;

    @InjectMocks
    private AsignacionSaga asignacionSaga;

    private Vehiculo vehiculo;

    @BeforeEach
    void setUp() {
        vehiculo = new Vehiculo(VEHICULO_ID, "ABC123", CIUDAD, 500, 0);
    }

    private Paquete paquete(Long vehiculoId, Integer pesoKg) {
        Paquete paquete = new Paquete();
        paquete.setVehiculoId(vehiculoId);
        paquete.setPesoKg(pesoKg);
        return paquete;
    }

    @Test
    void reservarPaquetes_asignaVehiculoExplicitoCuandoTieneCupo() {
        Paquete paquete = paquete(VEHICULO_ID, 120);

        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, 120)).thenReturn(Mono.just(vehiculo));
        when(paqueteRepository.save(paquete)).thenReturn(Mono.just(paquete));

        StepVerifier.create(asignacionSaga.reservarPaquetes(CIUDAD, List.of(paquete)))
                .assertNext(resultado -> {
                    assertThat(resultado.paquetes()).hasSize(1);
                    assertThat(resultado.paquetes().get(0).getVehiculoId()).isEqualTo(VEHICULO_ID);
                    assertThat(resultado.paquetes().get(0).getEstado()).isEqualTo("RESERVADO");
                    assertThat(resultado.reservas()).containsExactly(
                            new ReservaCupo(VEHICULO_ID, 120));
                })
                .verifyComplete();

        verify(vehiculoRepository, never()).liberarCupo(any(), any());
    }

    @Test
    void reservarPaquetes_fallaCuandoVehiculoExplicitoNoExiste() {
        Paquete paquete = paquete(99L, 50);

        when(vehiculoRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(asignacionSaga.reservarPaquetes(CIUDAD, List.of(paquete)))
                .expectError(VehiculoNoExisteException.class)
                .verify();

        verify(paqueteRepository, never()).save(any());
    }

    @Test
    void reservarPaquetes_fallaCuandoVehiculoExplicitoNoOperaEnLaCiudad() {
        Vehiculo vehiculoOtraCiudad = new Vehiculo(VEHICULO_ID, "XYZ999", "MDE", 500, 0);
        Paquete paquete = paquete(VEHICULO_ID, 50);

        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculoOtraCiudad));

        StepVerifier.create(asignacionSaga.reservarPaquetes(CIUDAD, List.of(paquete)))
                .expectErrorMatches(error ->
                        error instanceof ValidacionException
                                && error.getMessage().contains(CIUDAD))
                .verify();

        verify(vehiculoRepository, never()).descontarCupo(any(), any());
    }

    @Test
    void reservarPaquetes_fallaCuandoVehiculoExplicitoSinCupo() {
        Paquete paquete = paquete(VEHICULO_ID, 600);

        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, 600)).thenReturn(Mono.empty());

        StepVerifier.create(asignacionSaga.reservarPaquetes(CIUDAD, List.of(paquete)))
                .expectError(CupoInsuficienteException.class)
                .verify();
    }

    @Test
    void reservarPaquetes_seleccionaPrimerVehiculoDisponibleCuandoNoHayVehiculoExplicito() {
        Paquete paquete = paquete(null, 120);
        Vehiculo otro = new Vehiculo(2L, "OTR001", "MDE", 300, 0);

        when(vehiculoRepository.findAll()).thenReturn(Flux.just(otro, vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, 120)).thenReturn(Mono.just(vehiculo));
        when(paqueteRepository.save(paquete)).thenReturn(Mono.just(paquete));

        StepVerifier.create(asignacionSaga.reservarPaquetes(CIUDAD, List.of(paquete)))
                .assertNext(resultado ->
                        assertThat(resultado.paquetes().get(0).getVehiculoId()).isEqualTo(VEHICULO_ID))
                .verifyComplete();
    }

    @Test
    void reservarPaquetes_fallaCuandoNoHayVehiculoConCupoDisponible() {
        Paquete paquete = paquete(null, 999);

        when(vehiculoRepository.findAll()).thenReturn(Flux.just(vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, 999)).thenReturn(Mono.empty());

        StepVerifier.create(asignacionSaga.reservarPaquetes(CIUDAD, List.of(paquete)))
                .expectError(CupoInsuficienteException.class)
                .verify();
    }

    @Test
    void reservarPaquetes_fallaCuandoPesoEsNuloONoPositivo() {
        Paquete paquete = paquete(VEHICULO_ID, 0);

        StepVerifier.create(asignacionSaga.reservarPaquetes(CIUDAD, List.of(paquete)))
                .expectError(ValidacionException.class)
                .verify();

        verify(vehiculoRepository, never()).findById(any(Long.class));
    }

    @Test
    void reservarPaquetes_compensaReservasPreviasCuandoUnPaqueteFalla() {
        Paquete paqueteOk = paquete(VEHICULO_ID, 100);
        Paquete paqueteFalla = paquete(2L, 50);

        when(vehiculoRepository.findById(VEHICULO_ID)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.descontarCupo(VEHICULO_ID, 100)).thenReturn(Mono.just(vehiculo));
        when(paqueteRepository.save(paqueteOk)).thenReturn(Mono.just(paqueteOk));
        when(vehiculoRepository.findById(2L)).thenReturn(Mono.empty());
        when(vehiculoRepository.liberarCupo(VEHICULO_ID, 100)).thenReturn(Mono.just(vehiculo));

        StepVerifier.create(asignacionSaga.reservarPaquetes(CIUDAD, List.of(paqueteOk, paqueteFalla)))
                .expectError(VehiculoNoExisteException.class)
                .verify();

        verify(vehiculoRepository, times(1)).liberarCupo(VEHICULO_ID, 100);
    }

    @Test
    void compensar_noHaceNadaCuandoListaEsVaciaONula() {
        StepVerifier.create(asignacionSaga.compensar(null)).verifyComplete();
        StepVerifier.create(asignacionSaga.compensar(List.of())).verifyComplete();

        verify(vehiculoRepository, never()).liberarCupo(any(), any());
    }

    @Test
    void compensar_liberaCupoEnOrdenInversoALaReserva() {
        ReservaCupo primera = new ReservaCupo(1L, 50);
        ReservaCupo segunda = new ReservaCupo(2L, 30);

        when(vehiculoRepository.liberarCupo(2L, 30)).thenReturn(Mono.just(vehiculo));
        when(vehiculoRepository.liberarCupo(1L, 50)).thenReturn(Mono.just(vehiculo));

        StepVerifier.create(asignacionSaga.compensar(List.of(primera, segunda)))
                .verifyComplete();

        var inOrder = org.mockito.Mockito.inOrder(vehiculoRepository);
        inOrder.verify(vehiculoRepository).liberarCupo(2L, 30);
        inOrder.verify(vehiculoRepository).liberarCupo(1L, 50);
    }
}
