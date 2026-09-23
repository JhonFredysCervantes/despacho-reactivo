package com.despachoreactivo.project.event;

import java.time.Instant;

public class DespachoEvent {
    private Long despachoId;
    private String tipo; // RECIBIDO, ASIGNADO, EN_RUTA, ENTREGADO, CANCELADO
    private String estado;
    private String mensaje;
    private Instant timestamp;

    public DespachoEvent(Long despachoId, String tipo, String estado, String mensaje) {
        this.despachoId = despachoId;
        this.tipo = tipo;
        this.estado = estado;
        this.mensaje = mensaje;
        this.timestamp = Instant.now();
    }

    public Long getDespachoId() { return despachoId; }
    public String getTipo() { return tipo; }
    public String getEstado() { return estado; }
    public String getMensaje() { return mensaje; }
    public Instant getTimestamp() { return timestamp; }
}
