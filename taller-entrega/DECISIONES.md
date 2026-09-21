# Decisiones de diseño

> Máximo una página. Una decisión por bloque, en el formato de abajo.
> Las cuatro primeras son **obligatorias**; agregar las propias si las hay.
> Se evalúa el argumento, no la extensión. "Porque así sale en la demo" no es un argumento.

**Formato:** *Elegimos X · Descartamos Y · Porque Z · Se rompe si W*

---

## 1. `flatMap` vs `concatMap` (obligatoria)

- **Elegimos:** <p. ej. `concatMap` al reservar cupo paquete por paquete>
- **Descartamos:** <`flatMap`>
- **Porque:** <la compensación necesita saber exactamente qué se reservó y en qué orden; con `flatMap` la
  concurrencia mezcla los updates y la saga no sabe qué revertir>
- **Se rompe si:** <hay 200 paquetes por despacho: la latencia crece lineal; ahí tocaría `flatMap`
  con concurrencia acotada y un registro explícito de lo reservado>

## 2. Estrategia de backpressure del tablero (obligatoria)

- **Elegimos:** <`onBackpressureLatest`>
- **Descartamos:** <`onBackpressureBuffer`, `limitRate`>
- **Porque:** <el tablero muestra estado actual; a un operador con red lenta le sirve el último evento,
  no una cola de eventos viejos. Bufferear termina en OOM con un cliente colgado>
- **Se rompe si:** <el tablero se usara para auditoría: ahí perder eventos es inaceptable y habría que
  persistir el stream>

## 3. Hot y no cold en el tablero (obligatoria)

- **Elegimos:** <`Sinks.many().multicast()` + `publish().refCount()`>
- **Descartamos:** <un `Flux` frío por suscriptor>
- **Porque:** <cada suscriptor frío abriría su propia consulta y su propio job; con 10 operadores conectados
  serían 10 fuentes haciendo el mismo trabajo y viendo datos distintos>
- **Se rompe si:** <no hay suscriptores: `refCount` corta la fuente y se pierden los eventos de ese intervalo;
  lo asumimos porque nadie los está mirando>

## 4. Dónde empieza y termina la transacción (obligatoria)

- **Elegimos:** <la transacción cubre solo `guardar despacho + paquetes + cambio de estado`>
- **Descartamos:** <envolver también las llamadas a los externos y la reserva de cupo>
- **Porque:** <una transacción no puede abarcar llamadas HTTP: la conexión quedaría tomada durante cientos de
  ms y un timeout externo dejaría la transacción abierta. El cupo es atómico por fila (`RETURNING`) y se
  revierte con saga, no con rollback>
- **Se rompe si:** <el proceso muere entre la reserva de cupo y el commit: el cupo queda tomado hasta que el
  job de expiración lo libera a los 15 min. Es el trade-off que aceptamos>

---

## Decisiones propias

## 5. <título>

- **Elegimos:** <>
- **Descartamos:** <>
- **Porque:** <>
- **Se rompe si:** <>
