package com.despachoreactivo.project.repository;

import com.despachoreactivo.project.model.Paquete;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface PaqueteRepository extends R2dbcRepository<Paquete, Long> {

    @Query("SELECT * FROM paquete WHERE despacho_id = :despachoId")
    Flux<Paquete> findByDespachoId(Long despachoId);

    @Query("""
        UPDATE paquete
        SET vehiculo_id = :vehiculoId,
            estado = :estado
        WHERE despacho_id = :despachoId
        """)
    Mono<Void> updateVehiculoIdAndEstado(Long despachoId, Long vehiculoId, String estado);

    @Query("""
        UPDATE paquete
        SET estado = :estado
        WHERE despacho_id = :despachoId
        """)
    Mono<Void> updateEstadoByDespachoId(Long despachoId, String estado);
}
