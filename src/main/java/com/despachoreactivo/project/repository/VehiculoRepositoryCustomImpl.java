package com.despachoreactivo.project.repository;

import com.despachoreactivo.project.model.Vehiculo;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class VehiculoRepositoryCustomImpl implements VehiculoRepositoryCustom {

    private final DatabaseClient databaseClient;

    public VehiculoRepositoryCustomImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Vehiculo> insertar(Vehiculo vehiculo) {
        return databaseClient.sql("""
                INSERT INTO vehiculo (id, placa, ciudad, cupo_kg, reservado_kg)
                VALUES (:id, :placa, :ciudad, :cupoKg, :reservadoKg)
                RETURNING id, placa, ciudad, cupo_kg, reservado_kg
                """)
                .bind("id", vehiculo.getId())
                .bind("placa", vehiculo.getPlaca())
                .bind("ciudad", vehiculo.getCiudad())
                .bind("cupoKg", vehiculo.getCupoKg())
                .bind("reservadoKg", vehiculo.getReservadoKg())
                .map((row, metadata) -> new Vehiculo(
                        row.get("id", Long.class),
                        row.get("placa", String.class),
                        row.get("ciudad", String.class),
                        row.get("cupo_kg", Integer.class),
                        row.get("reservado_kg", Integer.class)
                ))
                .one();
    }
    @Override
    public Mono<Vehiculo> descontarCupo(Long id, Integer peso) {

        return databaseClient.sql("""
            UPDATE vehiculo
            SET cupo_kg = cupo_kg - :peso
            WHERE id = :id
              AND cupo_kg >= :peso
            RETURNING id, placa, ciudad, cupo_kg, reservado_kg
            """)
                .bind("id", id)
                .bind("peso", peso)
                .map((row, metadata) -> new Vehiculo(
                        row.get("id", Long.class),
                        row.get("placa", String.class),
                        row.get("ciudad", String.class),
                        row.get("cupo_kg", Integer.class),
                        row.get("reservado_kg", Integer.class)
                ))
                .one();
    }

    @Override
    public Mono<Vehiculo> liberarCupo(Long id, Integer peso) {

        return databaseClient.sql("""
            UPDATE vehiculo
            SET cupo_kg = cupo_kg + :peso
            WHERE id = :id
            RETURNING id, placa, ciudad, cupo_kg, reservado_kg
            """)
                .bind("id", id)
                .bind("peso", peso)
                .map((row, metadata) -> new Vehiculo(
                        row.get("id", Long.class),
                        row.get("placa", String.class),
                        row.get("ciudad", String.class),
                        row.get("cupo_kg", Integer.class),
                        row.get("reservado_kg", Integer.class)
                ))
                .one();
    }
}