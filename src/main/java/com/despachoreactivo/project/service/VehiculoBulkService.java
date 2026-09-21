package com.despachoreactivo.project.service;

import com.despachoreactivo.project.model.Vehiculo;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class VehiculoBulkService {

    private static final int TAMANO_LOTE = 500;

    private final DatabaseClient databaseClient;

    public VehiculoBulkService(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    public Mono<BulkResult> procesar(Flux<Vehiculo> vehiculos) {

        return vehiculos
                .buffer(TAMANO_LOTE)
                .concatMap(this::upsertLote)
                .reduce(new BulkResult(), BulkResult::sumar);
    }

    private Mono<BulkResult> upsertLote(List<Vehiculo> lote) {

        if (lote.isEmpty()) {
            return Mono.just(new BulkResult());
        }

        StringBuilder sql = new StringBuilder("""
                INSERT INTO vehiculo
                    (id, placa, ciudad, cupo_kg, reservado_kg)
                VALUES
                """);

        for (int i = 0; i < lote.size(); i++) {

            if (i > 0) {
                sql.append(", ");
            }

            sql.append("""
                    (:id%d, :placa%d, :ciudad%d, :cupoKg%d, :reservadoKg%d)
                    """.formatted(i, i, i, i, i));
        }

        sql.append("""
                ON CONFLICT (id) DO UPDATE SET
                    placa = EXCLUDED.placa,
                    ciudad = EXCLUDED.ciudad,
                    cupo_kg = EXCLUDED.cupo_kg,
                    reservado_kg = EXCLUDED.reservado_kg
                """);

        DatabaseClient.GenericExecuteSpec spec =
                databaseClient.sql(sql.toString());

        for (int i = 0; i < lote.size(); i++) {

            Vehiculo vehiculo = lote.get(i);

            spec = spec
                    .bind("id" + i, vehiculo.getId())
                    .bind("placa" + i, vehiculo.getPlaca())
                    .bind("ciudad" + i, vehiculo.getCiudad())
                    .bind("cupoKg" + i, vehiculo.getCupoKg())
                    .bind("reservadoKg" + i, vehiculo.getReservadoKg());
        }

        return spec
                .fetch()
                .rowsUpdated()
                .map(rows -> new BulkResult(lote.size(), rows.intValue()));
    }

    public static class BulkResult {

        private int procesados;
        private int afectados;

        public BulkResult() {
        }

        public BulkResult(int procesados, int afectados) {
            this.procesados = procesados;
            this.afectados = afectados;
        }

        public BulkResult sumar(BulkResult otro) {
            this.procesados += otro.procesados;
            this.afectados += otro.afectados;
            return this;
        }

        public int getProcesados() {
            return procesados;
        }

        public int getAfectados() {
            return afectados;
        }
    }
}