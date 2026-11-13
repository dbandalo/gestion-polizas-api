# API de Gestión de Pólizas

Prueba técnica práctica (Módulo 2) — Spring Boot 3 + capas controller / service / repository.

## Requisitos

- JDK 17+
- Maven 3.9+

## Ejecutar

```bash
mvn spring-boot:run
```

La API queda en `http://localhost:8080`.

Al iniciar se cargan datos de ejemplo:

| ID | Tipo        | Estado | Riesgos |
|----|-------------|--------|---------|
| 1  | INDIVIDUAL  | ACTIVA | 1       |
| 2  | COLECTIVA   | ACTIVA | 2       |

## Seguridad

Header obligatorio en todas las peticiones:

```http
x-api-key: 123456
```

## IPC

Incremento configurable en `application.yml`:

```yaml
app:
  ipc-increment: 0.05   # 5%
```

Al renovar: `canon` y `prima` se incrementan por IPC; estado → `RENOVADA`; se registra evento al CORE mock.

## Endpoints

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/polizas?tipo=&estado=` | Listar pólizas |
| GET | `/polizas/{id}/riesgos` | Riesgos de una póliza |
| POST | `/polizas/{id}/renovar` | Renovar (+IPC) |
| POST | `/polizas/{id}/cancelar` | Cancelar póliza y sus riesgos |
| POST | `/polizas/{id}/riesgos` | Agregar riesgo (solo COLECTIVA) |
| POST | `/riesgos/{id}/cancelar` | Cancelar riesgo |
| POST | `/core-mock/evento` | Mock de integración CORE |

### Ejemplos

```bash
curl -H "x-api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=ACTIVA"

curl -X POST -H "x-api-key: 123456" "http://localhost:8080/polizas/1/renovar"

curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d "{\"descripcion\":\"Apt 303\"}" \
  "http://localhost:8080/polizas/2/riesgos"

curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d "{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}" \
  "http://localhost:8080/core-mock/evento"
```

## Reglas de negocio implementadas

- Póliza individual: no admite agregar riesgos adicionales (máximo 1).
- No se puede renovar una póliza cancelada.
- Cancelar póliza cancela todos sus riesgos.
- Agregar riesgo solo si tipo = `COLECTIVA`.
- Toda mutación de estado notifica al CORE mock vía log.

## Estructura

```
controller/   → endpoints HTTP
service/      → reglas de negocio + integración CORE
repository/   → acceso a datos JPA
domain/       → entidades Poliza / Riesgo
dto/          → contratos de entrada/salida
security/     → filtro x-api-key
exception/    → manejo centralizado de errores
```
