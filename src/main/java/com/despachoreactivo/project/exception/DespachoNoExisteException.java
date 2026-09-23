package com.despachoreactivo.project.exception;

public class DespachoNoExisteException extends RuntimeException {
    public DespachoNoExisteException() {
        super("Despacho no existe");
    }
}
