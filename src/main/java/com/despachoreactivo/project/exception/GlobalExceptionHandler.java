package com.despachoreactivo.project.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(VehiculoNoExisteException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> manejarVehiculoNoExiste(
            VehiculoNoExisteException ex) {

        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", 404,
                "error", "Vehículo no existe"
        );
    }

    @ExceptionHandler(CupoInsuficienteException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> manejarCupoInsuficiente(
            CupoInsuficienteException ex) {

        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", 409,
                "error", "Cupo insuficiente para el vehículo"
        );
    }
    @ExceptionHandler(ZonaRiesgosaException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, Object> manejarZonaRiesgosa(
            ZonaRiesgosaException ex) {

        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", 422,
                "error", ex.getMessage()
        );
    }
}