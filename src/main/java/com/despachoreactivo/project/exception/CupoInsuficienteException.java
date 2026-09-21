package com.despachoreactivo.project.exception;

public class CupoInsuficienteException extends RuntimeException {

    public CupoInsuficienteException() {
        super("Cupo insuficiente para el vehículo");
    }
}