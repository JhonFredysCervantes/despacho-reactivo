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

### Todos los tests
```bash
./gradlew test
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
