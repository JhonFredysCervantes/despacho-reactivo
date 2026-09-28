package com.despachoreactivo.project.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String TRAZA_HEADER = "X-Traza-Id";

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        when(exchange.getRequest()).thenReturn(request);
    }

    @Test
    void manejarVehiculoNoExiste_usaTrazaDelHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(TRAZA_HEADER, "traza-vehiculo");
        when(request.getHeaders()).thenReturn(headers);

        Map<String, Object> body = handler.manejarVehiculoNoExiste(
                new VehiculoNoExisteException(), exchange);

        assertThat(body.get("codigo")).isEqualTo("VEHICULO_NO_EXISTE");
        assertThat(body.get("status")).isEqualTo(404);
        assertThat(body.get("trazaId")).isEqualTo("traza-vehiculo");
        assertThat(body.get("mensaje")).isNotNull();
        assertThat(body.get("instante")).isNotNull();
    }

    @Test
    void manejarCupoInsuficiente_generaTrazaSiFaltaHeader() {
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        Map<String, Object> body = handler.manejarCupoInsuficiente(
                new CupoInsuficienteException(), exchange);

        assertThat(body.get("codigo")).isEqualTo("CUPO_INSUFICIENTE");
        assertThat(body.get("status")).isEqualTo(409);
        assertThat(body.get("mensaje")).isEqualTo("Cupo insuficiente para el vehículo");
        assertThat(body.get("trazaId")).isNotNull();
    }

    @Test
    void manejarZonaRiesgosa_devuelve422() {
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        Map<String, Object> body = handler.manejarZonaRiesgosa(
                new ZonaRiesgosaException("Score de riesgo: 95"), exchange);

        assertThat(body.get("codigo")).isEqualTo("ZONA_RIESGOSA");
        assertThat(body.get("status")).isEqualTo(422);
        assertThat(body.get("mensaje")).isEqualTo("Score de riesgo: 95");
    }

    @Test
    void manejarDespachoNoExiste_devuelve404() {
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        Map<String, Object> body = handler.manejarDespachoNoExiste(
                new DespachoNoExisteException(), exchange);

        assertThat(body.get("codigo")).isEqualTo("DESPACHO_NO_EXISTE");
        assertThat(body.get("status")).isEqualTo(404);
    }
}
