package com.despachoreactivo.project.exception;

public class VehiculoNoExisteException extends RuntimeException {

    public VehiculoNoExisteException() {
        super("Vehículo no existe");
    }
}