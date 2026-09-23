package com.despachoreactivo.project.repository;

import com.despachoreactivo.project.model.Despacho;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface DespachoRepository extends R2dbcRepository<Despacho, Long> {
    
    @Query("""
        SELECT * FROM despacho 
        WHERE id = :id
        """)
    Mono<Despacho> findByIdWithTimeout(Long id);

    @Query("""
        UPDATE despacho 
        SET estado = :estado, updated_at = NOW()
        WHERE id = :id
        RETURNING *
        """)
    Mono<Despacho> updateEstado(Long id, String estado);

    @Query("""
        UPDATE despacho 
        SET tarifa_total = :tarifaTotal, 
            risk_score = :riskScore,
            estado = :estado,
            expira_en = :expiraEn,
            updated_at = NOW()
        WHERE id = :id
        RETURNING *
        """)
    Mono<Despacho> updateWithExternalData(Long id, Double tarifaTotal, 
                                          Integer riskScore, String estado, 
                                          java.time.Instant expiraEn);
}
