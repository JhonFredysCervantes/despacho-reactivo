package com.despachoreactivo.project.exception;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void manejarVehiculoNoExiste_devuelve404() {
        Map<String, Object> body =
                handler.manejarVehiculoNoExiste(new VehiculoNoExisteException());

        assertThat(body.get("status")).isEqualTo(404);
        assertThat(body.get("error")).isEqualTo("Vehículo no existe");
        assertThat(body.get("timestamp")).isNotNull();
    }

    @Test
    void manejarCupoInsuficiente_devuelve409() {
        Map<String, Object> body =
                handler.manejarCupoInsuficiente(new CupoInsuficienteException());

        assertThat(body.get("status")).isEqualTo(409);
        assertThat(body.get("error")).isEqualTo("Cupo insuficiente para el vehículo");
    }

    @Test
    void manejarZonaRiesgosa_devuelve422() {
        Map<String, Object> body =
                handler.manejarZonaRiesgosa(new ZonaRiesgosaException());

        assertThat(body.get("status")).isEqualTo(422);
        assertThat(body.get("error"))
                .isEqualTo("La zona tiene un nivel de riesgo demasiado alto");
    }
}
