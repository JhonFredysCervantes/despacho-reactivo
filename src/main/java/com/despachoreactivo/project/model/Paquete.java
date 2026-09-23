package com.despachoreactivo.project.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

@Table("paquete")
public class Paquete {
    @Id
    private Long id;

    @Column("despacho_id")
    private Long despachoId;

    @NotBlank(message = "Descripción es requerida")
    private String descripcion;

    @Positive(message = "Peso debe ser positivo")
    @Column("peso_kg")
    private Integer pesoKg;

    @Column("vehiculo_id")
    private Long vehiculoId;

    private String estado; // RESERVADO, ASIGNADO, EN_TRANSITO, ENTREGADO

    @Column("created_at")
    private Instant createdAt;

    // Constructores
    public Paquete() {}

    public Paquete(Long id, Long despachoId, String descripcion, Integer pesoKg,
                   Long vehiculoId, String estado, Instant createdAt) {
        this.id = id;
        this.despachoId = despachoId;
        this.descripcion = descripcion;
        this.pesoKg = pesoKg;
        this.vehiculoId = vehiculoId;
        this.estado = estado;
        this.createdAt = createdAt;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDespachoId() { return despachoId; }
    public void setDespachoId(Long despachoId) { this.despachoId = despachoId; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getPesoKg() { return pesoKg; }
    public void setPesoKg(Integer pesoKg) { this.pesoKg = pesoKg; }

    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
