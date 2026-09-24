package com.despachoreactivo.project.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;

@Table("despacho")
public class Despacho {
    @Id
    private Long id;

    @NotBlank(message = "Ciudad es requerida")
    private String ciudad;

    @NotEmpty(message = "Debe haber al menos un paquete")
    @Transient
    private List<Paquete> paquetes;

    @Column("estado")
    private String estado; // RECIBIDO, ASIGNADO, EN_RUTA, ENTREGADO, CANCELADO

    @Column("cliente_id")
    private Long clienteId;

    @Column("tarifa_total")
    private Double tarifaTotal;

    @Column("risk_score")
    private Integer riskScore;

    @Column("expira_en")
    private Instant expiraEn;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;

    // Constructores
    public Despacho() {}

    public Despacho(Long id, String ciudad, List<Paquete> paquetes, String estado,
                    Long clienteId, Double tarifaTotal, Integer riskScore,
                    Instant expiraEn, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ciudad = ciudad;
        this.paquetes = paquetes;
        this.estado = estado;
        this.clienteId = clienteId;
        this.tarifaTotal = tarifaTotal;
        this.riskScore = riskScore;
        this.expiraEn = expiraEn;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }

    public List<Paquete> getPaquetes() { return paquetes; }
    public int totalPaquetes() { return paquetes != null ? paquetes.size() : 0; }
    public void setPaquetes(List<Paquete> paquetes) { this.paquetes = paquetes; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }

    public Double getTarifaTotal() { return tarifaTotal; }
    public void setTarifaTotal(Double tarifaTotal) { this.tarifaTotal = tarifaTotal; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
