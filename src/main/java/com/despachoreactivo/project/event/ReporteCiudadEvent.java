package com.despachoreactivo.project.event;

public record ReporteCiudadEvent(
        String ciudad,
        int totalPaquetes
) {

    public ReporteCiudadEvent sumar(ReporteCiudadEvent siguiente) {
        return new ReporteCiudadEvent(ciudad, totalPaquetes + siguiente.totalPaquetes);
    }
}
