package com.despachoreactivo.project.model;

import java.util.List;

public record ResultadoReserva(List<Paquete> paquetes, List<ReservaCupo> reservas) {
}