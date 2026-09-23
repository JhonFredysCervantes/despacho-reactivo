-- Punto de partida sugerido para el taller. Adaptar: el modelo es del equipo, no una respuesta cerrada.
-- Spring lo ejecuta con spring.sql.init.mode=always. Guardar en UTF-8 sin BOM.
DROP TABLE IF EXISTS idempotency_key;
DROP TABLE IF EXISTS paquete;
DROP TABLE IF EXISTS despacho;
DROP TABLE IF EXISTS vehiculo;

CREATE TABLE vehiculo (
    id           BIGINT PRIMARY KEY,
    placa        VARCHAR(10)  NOT NULL UNIQUE,
    ciudad       VARCHAR(8)   NOT NULL,
    cupo_kg      INT          NOT NULL CHECK (cupo_kg >= 0),      -- el CHECK delata la carrera en la demo
    reservado_kg INT          NOT NULL DEFAULT 0 CHECK (reservado_kg >= 0),
    created_at   TIMESTAMPTZ  DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  DEFAULT NOW()
);

CREATE TABLE despacho (
    id           BIGSERIAL PRIMARY KEY,                             -- Postgres asigna el id: insertar antes de reservar
    cliente_id   BIGINT       NOT NULL,
    ciudad       VARCHAR(8)   NOT NULL,
    estado       VARCHAR(16)  NOT NULL DEFAULT 'RECIBIDO',          -- RECIBIDO|ASIGNADO|EN_RUTA|ENTREGADO|RECHAZADO|EXPIRADO
    tarifa_total NUMERIC(12,2),
    risk_score   INT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expira_en    TIMESTAMPTZ
);

CREATE TABLE paquete (
    id          BIGSERIAL PRIMARY KEY,
    despacho_id BIGINT NOT NULL REFERENCES despacho(id) ON DELETE CASCADE,
    descripcion VARCHAR(500),
    peso_kg     INT    NOT NULL CHECK (peso_kg > 0),
    vehiculo_id BIGINT REFERENCES vehiculo(id),
    estado      VARCHAR(50) NOT NULL DEFAULT 'RESERVADO',
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE idempotency_key (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key  VARCHAR(255) UNIQUE NOT NULL,
    despacho_id      BIGINT NOT NULL REFERENCES despacho(id),
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_despacho_expira ON despacho (estado, expira_en);  -- lo usa el job cada 30 s
CREATE INDEX idx_despacho_ciudad ON despacho (ciudad);
CREATE INDEX idx_despacho_estado ON despacho (estado);
CREATE INDEX idx_paquete_despacho ON paquete (despacho_id);
CREATE INDEX idx_paquete_vehiculo ON paquete (vehiculo_id);
CREATE INDEX idx_paquete_estado ON paquete (estado);
CREATE INDEX idx_vehiculo_ciudad ON vehiculo (ciudad);

INSERT INTO vehiculo (id, placa, ciudad, cupo_kg) VALUES
    (1, 'ABC123', 'BOG', 500),
    (2, 'XYZ987', 'MDE', 200),
    (3, 'JKL456', 'CLO', 800)
ON CONFLICT (id) DO NOTHING;
