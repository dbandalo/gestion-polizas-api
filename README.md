# API de Gestión de Pólizas

API para administrar pólizas de arrendamiento individuales y colectivas, construida con Spring Boot 3 y Java 17. El proyecto está organizado en capas: los controladores exponen los endpoints, los servicios concentran las reglas de negocio y los repositorios se encargan del acceso a datos.

## Requisitos

- JDK 17 o superior
- Maven 3.9 o superior

## Cómo ejecutarlo

```bash
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`. Usa una base de datos H2 en memoria, así que no hace falta instalar nada adicional. Si quieres ver los datos directamente, la consola de H2 está en `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:polizas`, usuario `sa`, sin contraseña).

Al iniciar se cargan dos pólizas de ejemplo para poder probar de inmediato:

| ID | Tipo       | Estado | Riesgos |
|----|------------|--------|---------|
| 1  | INDIVIDUAL | ACTIVA | 1       |
| 2  | COLECTIVA  | ACTIVA | 2       |

Para correr las pruebas unitarias y de integración:

```bash
mvn test
```

## Seguridad

Todas las peticiones deben incluir este header; si falta o el valor no coincide, la API responde `401`:

```http
x-api-key: 123456
```

## Endpoints

| Método | Ruta | Qué hace |
|--------|------|----------|
| GET | `/polizas?tipo=&estado=` | Lista las pólizas; los filtros por tipo y estado son opcionales |
| GET | `/polizas/{id}/riesgos` | Lista los riesgos de una póliza |
| POST | `/polizas/{id}/renovar` | Renueva la póliza aplicando el IPC |
| POST | `/polizas/{id}/cancelar` | Cancela la póliza junto con todos sus riesgos |
| POST | `/polizas/{id}/riesgos` | Agrega un riesgo (solo pólizas colectivas) |
| POST | `/riesgos/{id}/cancelar` | Cancela un riesgo |
| POST | `/core-mock/evento` | Simula el envío de un evento al CORE |

### Ejemplos

Listar las pólizas colectivas activas:

```bash
curl -H "x-api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=ACTIVA"
```

Consultar los riesgos de la póliza colectiva:

```bash
curl -H "x-api-key: 123456" "http://localhost:8080/polizas/2/riesgos"
```

Renovar la póliza individual:

```bash
curl -X POST -H "x-api-key: 123456" "http://localhost:8080/polizas/1/renovar"
```

Agregar un riesgo a la póliza colectiva:

```bash
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d "{\"descripcion\":\"Apt 303 - Torre Norte\"}" \
  "http://localhost:8080/polizas/2/riesgos"
```

Cancelar un riesgo de la póliza colectiva:

```bash
curl -X POST -H "x-api-key: 123456" "http://localhost:8080/riesgos/2/cancelar"
```

Cancelar la póliza colectiva (también cancela sus riesgos):

```bash
curl -X POST -H "x-api-key: 123456" "http://localhost:8080/polizas/2/cancelar"
```

Enviar un evento al mock del CORE:

```bash
curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d "{\"evento\":\"ACTUALIZACION\",\"polizaId\":555}" \
  "http://localhost:8080/core-mock/evento"
```

## Reglas de negocio

- Una póliza individual tiene un único riesgo: no se le pueden agregar más, y ese riesgo no se puede cancelar por separado; para retirarlo hay que cancelar la póliza completa.
- Solo las pólizas colectivas permiten agregar riesgos.
- Una póliza cancelada no se puede renovar ni recibir riesgos nuevos.
- Al cancelar una póliza se cancelan todos sus riesgos.
- Al renovar, el canon se incrementa según el IPC, la prima se recalcula como canon por meses de vigencia, la vigencia avanza un periodo igual al inicial y el estado pasa a `RENOVADA`.
- Cada operación que cambia el estado de una póliza o de un riesgo se reporta al mock del CORE, que la deja registrada en el log.

## Configuración

El porcentaje de IPC y la API key se definen en `src/main/resources/application.yml`:

```yaml
app:
  api-key: "123456"
  ipc-increment: 0.05   # 5 %
```

## Manejo de errores

Los errores se devuelven siempre con la misma estructura, para que el front pueda mostrarlos sin casos especiales:

```json
{
  "timestamp": "2026-11-13T17:56:50Z",
  "status": 400,
  "error": "Bad Request",
  "message": "No se puede renovar una póliza cancelada",
  "path": "/polizas/2/renovar"
}
```

Las violaciones a reglas de negocio y los datos inválidos responden `400`, los recursos inexistentes `404` y la falta de API key `401`.

## Estructura del proyecto

| Paquete | Contenido |
|---------|-----------|
| `controller` | Endpoints HTTP |
| `service` | Reglas de negocio e integración con el CORE |
| `repository` | Acceso a datos con Spring Data JPA |
| `domain` | Entidades `Poliza` y `Riesgo` |
| `dto` | Objetos de entrada y salida de la API |
| `security` | Filtro que valida el header `x-api-key` |
| `exception` | Manejo centralizado de errores |
| `config` | Registro del filtro y carga de datos de ejemplo |
