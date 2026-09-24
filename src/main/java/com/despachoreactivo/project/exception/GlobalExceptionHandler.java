package com.despachoreactivo.project.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public Map<String, Object> manejarVehiculoNoExiste(VehiculoNoExisteException ex) {
        return crearErrorResponseBasica("Vehículo no existe", HttpStatus.NOT_FOUND.value());
    }

    public Map<String, Object> manejarCupoInsuficiente(CupoInsuficienteException ex) {
        return crearErrorResponseBasica("Cupo insuficiente para el vehículo", HttpStatus.CONFLICT.value());
    }

    public Map<String, Object> manejarZonaRiesgosa(ZonaRiesgosaException ex) {
        return crearErrorResponseBasica("La zona tiene un nivel de riesgo demasiado alto", HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    private String getTrazaId(ServerWebExchange exchange) {
        String trazaId = exchange.getRequest().getHeaders().getFirst("X-Traza-Id");
        return trazaId != null ? trazaId : UUID.randomUUID().toString();
    }

    @ExceptionHandler(VehiculoNoExisteException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> manejarVehiculoNoExiste(
            VehiculoNoExisteException ex,
            ServerWebExchange exchange) {

        return crearErrorResponse("VEHICULO_NO_EXISTE", ex.getMessage(), 
                getTrazaId(exchange), HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(CupoInsuficienteException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> manejarCupoInsuficiente(
            CupoInsuficienteException ex,
            ServerWebExchange exchange) {

        return crearErrorResponse("CUPO_INSUFICIENTE", "Cupo insuficiente para el vehículo", 
                getTrazaId(exchange), HttpStatus.CONFLICT.value());
    }

    @ExceptionHandler(ZonaRiesgosaException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, Object> manejarZonaRiesgosa(
            ZonaRiesgosaException ex,
            ServerWebExchange exchange) {

        return crearErrorResponse("ZONA_RIESGOSA", ex.getMessage(), 
                getTrazaId(exchange), HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @ExceptionHandler(DespachoNoExisteException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> manejarDespachoNoExiste(
            DespachoNoExisteException ex,
            ServerWebExchange exchange) {

        return crearErrorResponse("DESPACHO_NO_EXISTE", ex.getMessage(), 
                getTrazaId(exchange), HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> manejarEstadoInvalido(
            EstadoInvalidoException ex,
            ServerWebExchange exchange) {

        return crearErrorResponse("ESTADO_INVALIDO", ex.getMessage(), 
                getTrazaId(exchange), HttpStatus.CONFLICT.value());
    }

    @ExceptionHandler(ValidacionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> manejarValidacion(
            ValidacionException ex,
            ServerWebExchange exchange) {

        return crearErrorResponse("VALIDACION_ERROR", ex.getMessage(), 
                getTrazaId(exchange), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> manejarError(
            Exception ex,
            ServerWebExchange exchange) {

        return crearErrorResponse("ERROR_INTERNO", "Error interno del servidor", 
                getTrazaId(exchange), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private Map<String, Object> crearErrorResponse(String codigo, String mensaje, 
                                                     String trazaId, int status) {
        return Map.of(
                "codigo", codigo,
                "mensaje", mensaje,
                "trazaId", trazaId,
                "instante", Instant.now().toString(),
                "status", status
        );
    }

    private Map<String, Object> crearErrorResponseBasica(String error, int status) {
        return Map.of(
                "status", status,
                "error", error,
                "timestamp", Instant.now().toString()
        );
    }
}