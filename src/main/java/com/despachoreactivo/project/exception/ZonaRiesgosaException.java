package com.despachoreactivo.project.exception;

public class ZonaRiesgosaException extends RuntimeException {

    public ZonaRiesgosaException() {
        super("La zona tiene un nivel de riesgo demasiado alto");
    }
}