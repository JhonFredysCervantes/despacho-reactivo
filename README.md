# 📦 Despacho Reactivo

Servicio backend reactivo para gestión de despachos y entregas, construido con **Spring Boot WebFlux** y **R2DBC**. 

## 🎯 Descripción

Sistema de distribución de paquetes que utiliza programación reactiva para manejar:
- ✅ Creación y seguimiento de despachos
- ✅ Gestión de vehículos y cupo disponible
- ✅ Stream de eventos en tiempo real (SSE)
- ✅ Reportes por ciudades
- ✅ Validación de zonas de riesgo y tarificación
- ✅ Resiliencia con compensación de transacciones

## 🛠 Stack Tecnológico

- **Java 17**
- **Spring Boot 4.1.1** (WebFlux, Data R2DBC)
- **PostgreSQL 15**
- **Reactor 3.x** (Programación reactiva)
- **Lombok**
- **JUnit 5 + Reactor Test**

## 📋 Requisitos Previos

- Java 17+
- Docker & Docker Compose
- Gradle (incluido con `gradlew`)
- Git

## 🚀 Levantar el Servicio

### 1. Clonar el repositorio
```bash
git clone https://github.com/JhonFredysCervantes/despacho-reactivo.git
cd despacho-reactivo
```

### 2. Iniciar PostgreSQL (Docker)
```bash
docker-compose up -d
```

Verifica que PostgreSQL está corriendo:
```bash
docker ps
# Deberías ver el contenedor 'postgres_r2dbc'
```

### 3. Compilar el proyecto
```bash
./gradlew build
```

### 4. Ejecutar la aplicación
```bash
./gradlew bootRun
```

La aplicación estará disponible en: **http://localhost:8081**

## 🧪 Probar los Endpoints

### Opción A: Usando Swagger UI
Abre en el navegador: http://localhost:8081/swagger-ui.html

### Opción B: Usando cURL

#### 1️⃣ Listar vehículos disponibles
```bash
curl -X GET "http://localhost:8081/api/vehiculos" \
  -H "Content-Type: application/json"
```

#### 1️⃣ bis Carga masiva de vehículos (NDJSON) — Windows / PowerShell

En PowerShell usa **`curl.exe`** (no el alias `curl`). La app debe estar en marcha (`./gradlew bootRun`) y PostgreSQL arriba (`docker compose up -d`).

**1. Crear el archivo** `vehiculos.ndjson` (UTF-8 sin BOM; funciona en PowerShell 5.1 y 7+):

```powershell
cd C:\RepositoriosGitHub\despacho-reactivo

$body = @'
{"id":1,"placa":"ABC123","ciudad":"BOG","cupoKg":500,"reservadoKg":0}
{"id":2,"placa":"XYZ987","ciudad":"MDE","cupoKg":200,"reservadoKg":0}
'@

[System.IO.File]::WriteAllText(
    (Join-Path $PWD "vehiculos.ndjson"),
    $body,
    [System.Text.UTF8Encoding]::new($false)
)

Get-Content .\vehiculos.ndjson
```

**2. Enviar el bulk:**

```powershell
curl.exe -i -X POST "http://localhost:8081/api/vehiculos/bulk" `
  -H "Content-Type: application/x-ndjson" `
  -H "Accept: application/json" `
  --data-binary "@vehiculos.ndjson"
```

En una sola línea:

```powershell
curl.exe -i -X POST "http://localhost:8081/api/vehiculos/bulk" -H "Content-Type: application/x-ndjson" -H "Accept: application/json" --data-binary "@vehiculos.ndjson"
```

**Respuesta esperada:** HTTP 200 con JSON de resumen (p. ej. cantidad de registros procesados/afectados).

**Errores frecuentes:**

| Síntoma | Causa |
|---------|--------|
| `Connection refused` | La app no está en el puerto 8081 |
| Error 500 | Postgres no está corriendo |
| ParserError al pegar JSON | Falta el bloque `@' ... '@` al crear el archivo |

En Linux/macOS/Git Bash puedes usar el mismo `curl` con `--data-binary "@vehiculos.ndjson"` y crear el `.ndjson` con un editor (una línea JSON por vehículo).

#### 2️⃣ Crear un despacho
```bash
curl -X POST "http://localhost:8081/api/despachos" \
  -H "Content-Type: application/json" \
  -d '{
    "peso": 100,
    "origen": "Bogota",
    "destino": "Medellin",
    "clienteId": 1
  }'
```

#### 3️⃣ Confirmar un despacho
```bash
curl -X POST "http://localhost:8081/api/despachos/1/confirm" \
  -H "X-Traza-Id: trace-123"
```

#### 4️⃣ Obtener eventos de un despacho (SSE)
```bash
curl -N "http://localhost:8081/api/despachos/1/events" \
  -H "X-Traza-Id: trace-123"
```

#### 5️⃣ Reporte de ciudades (snapshot)
```bash
curl -X GET "http://localhost:8081/api/reports/ciudades" \
  -H "Content-Type: application/json"
```

#### 6️⃣ Reporte de ciudades (stream NDJSON)
```bash
curl -N "http://localhost:8081/api/reports/ciudades/stream"
```

#### 7️⃣ Tablero en tiempo real (SSE)
```bash
curl -N "http://localhost:8081/api/ops/tablero"
```

## 🧪 Ejecutar Tests

### Tests unitarios (sin PostgreSQL)
```bash
./gradlew test
```
Incluye servicios, EventBus y manejo de errores con mocks. **No** levanta Spring ni base de datos.

### Tests de integración (con PostgreSQL)
```bash
docker compose up -d
./gradlew integrationTest
```
Carga el contexto Spring (`ProjectApplicationTests`). Mismas credenciales que `application.yml` (`testdb`, `postgres`/`postgres`).

### Suite completa (evaluación)
```bash
docker compose up -d
./gradlew test integrationTest
```

### Tests específicos de EventBus
```bash
./gradlew test --tests "*EventBusTest*"
```

### Tests de un servicio específico
```bash
./gradlew test --tests "*VehiculoServiceTest*"
```

### Ver reporte de tests
```bash
# Después de ejecutar los tests
open build/reports/tests/test/index.html  # macOS
start build/reports/tests/test/index.html # Windows
```

## 📂 Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/despachoreactivo/project/
│   │   ├── controller/           # REST Controllers
│   │   │   ├── DespachoController.java
│   │   │   ├── VehiculoController.java
│   │   │   ├── ReporteController.java
│   │   │   └── TableroController.java
│   │   ├── service/              # Lógica de negocio
│   │   │   ├── DespachoService.java
│   │   │   ├── VehiculoService.java
│   │   │   └── DespachoExternalService.java
│   │   ├── repository/           # Acceso a datos (R2DBC)
│   │   ├── model/                # Entidades
│   │   ├── event/                # EventBus reactivo
│   │   │   ├── EventBus.java
│   │   │   ├── DespachoEvent.java
│   │   │   └── ReporteCiudadEvent.java
│   │   └── exception/            # Manejo de excepciones
│   └── resources/
│       ├── application.yml       # Configuración
│       └── schema.sql            # DDL de base de datos
└── test/
    └── java/com/despachoreactivo/project/
        ├── event/
        │   └── EventBusTest.java  # Tests del EventBus
        └── service/
            ├── VehiculoServiceTest.java
            └── DespachoExternalServiceTest.java
```

## 🔧 Configuración

Archivo: `src/main/resources/application.yml`

| Propiedad | Valor | Descripción |
|-----------|-------|-------------|
| `server.port` | 8081 | Puerto de la aplicación |
| `spring.r2dbc.url` | localhost:5432 | Base de datos PostgreSQL |
| `app.risk-threshold` | 80 | Umbral de riesgo para entregas |
| `app.reservation-ttl` | 15m | TTL de reservas |
| `springdoc.api-docs.path` | /v3/api-docs | OpenAPI JSON |

## 📡 API Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/vehiculos` | Lista todos los vehículos |
| POST | `/api/despachos` | Crear un nuevo despacho |
| GET | `/api/despachos/{id}` | Obtener despacho por ID |
| POST | `/api/despachos/{id}/confirm` | Confirmar un despacho |
| GET | `/api/despachos/{id}/events` | SSE: Eventos del despacho |
| GET | `/api/reports/ciudades` | Snapshot de paquetes por ciudad |
| GET | `/api/reports/ciudades/stream` | Stream NDJSON de ciudades |
| GET | `/api/ops/tablero` | SSE: Tablero en tiempo real |


### Conectar a PostgreSQL directamente
```bash
docker exec -it postgres_r2dbc psql -U postgres -d postgres
```

### Consultas útiles
```sql
SELECT * FROM despachos;
SELECT * FROM vehiculos;
SELECT COUNT(*) as total_eventos FROM despachos WHERE estado = 'ENTREGADO';
```
