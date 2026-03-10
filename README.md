# ms-order-loading

Microservicio en Java 17 y Spring Boot 3 para cargar pedidos desde CSV, validarlos y persistirlos con arquitectura hexagonal, batch e idempotencia.

## Stack

- Java 17
- Spring Boot 3.4.3
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Security OAuth2 Resource Server (JWT)
- Springdoc OpenAPI

## Arquitectura

La solución está separada en cuatro capas:

- `domain`: reglas de negocio puras y modelos.
- `application`: caso de uso `cargar pedidos` y orquestación batch.
- `domain.port.out`: contratos que necesita el dominio/aplicación.
- `adapter`: REST de entrada, JPA de salida, seguridad, logs y configuración.

## Decisiones de diseño

- Se eligió Spring MVC en lugar de WebFlux porque el problema es principalmente I/O de archivo + JPA batch y no requiere streaming reactivo.
- El CSV se procesa por lotes configurables con `app.batch.size`.
- La idempotencia se resuelve con `Idempotency-Key + SHA-256 del archivo`.
- Para poder responder exactamente el mismo resultado ante reintentos idempotentes, se agregó `response_payload` y `status` en `cargas_idempotencia`.
- Los catálogos de clientes, zonas y duplicados existentes se consultan por batch para evitar lecturas fila por fila.
- La fecha válida se evalúa con `Clock` en zona `America/Lima`.

## Supuestos

- `numeroPedido` se valida con regex `^[A-Za-z0-9]+$`.
- Un `cliente` válido es uno existente y `activo = true`.
- Si llega el mismo archivo con la misma `Idempotency-Key`, se devuelve la misma respuesta sin volver a insertar pedidos.
- Si llega el mismo archivo y la misma llave mientras el primero aún está en proceso, se responde `409`.
- El volumen esperado es hasta 1000 filas por archivo, por eso el archivo se lee una vez a memoria para calcular hash y luego se procesa por lotes.

## Estructura principal

```text
src/main/java/com/reto/ms_order_loading.adapter
├── adapter
├── application
├── config
├── domain
└── MsOrderLoadingApplication.java
```

## Requisitos

- Java 17+
- Maven 3.9+
- Docker opcional para PostgreSQL

## Levantar PostgreSQL

```bash
docker compose up -d
```

## Ejecutar la aplicación

```bash
mvn clean spring-boot:run
```

## Ejecutar pruebas y cobertura

```bash
mvn clean verify
```

## Configuración relevante

```yaml
app:
  batch:
    size: 500
```

Rango permitido: 500 a 1000.

## Autenticación JWT local

El proyecto valida JWT firmado con una llave HMAC local configurada en `application.yml`.

Header:

```text
Authorization: Bearer <token>
```

Token de ejemplo listo para usar en local:

```text
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJyZXRvLXRlY25pY28iLCJzY29wZSI6InBlZGlkb3Mud3JpdGUifQ.PynS75yxIEFvX3z_DOkM70ZzHlMsknqSLY-pSv7OEAw
```

## Endpoint principal

```http
POST /pedidos/cargar
Content-Type: multipart/form-data
Idempotency-Key: carga-001
Authorization: Bearer <token>
```

Campo multipart:

- `file`: archivo CSV UTF-8

## curl de ejemplo

```bash
curl --location 'http://localhost:8080/pedidos/cargar'   --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJyZXRvLXRlY25pY28iLCJzY29wZSI6InBlZGlkb3Mud3JpdGUifQ.PynS75yxIEFvX3z_DOkM70ZzHlMsknqSLY-pSv7OEAw'   --header 'Idempotency-Key: carga-001'   --form 'file=@samples/pedidos-validos.csv'
```

## Estrategia de batch

1. Se lee el CSV y se corta en lotes configurables.
2. Por cada lote se obtienen en bloque:
   - clientes activos existentes
   - zonas existentes
   - pedidos ya existentes en BD
3. Se valida fila por fila solo contra estructuras en memoria del batch.
4. Solo los válidos se envían a `saveAll`.
5. Hibernate agrupa inserts con `hibernate.jdbc.batch_size` y `order_inserts=true`.

## Migraciones Flyway

- `V1__init_schema.sql`: crea tablas, índices y restricciones.
- `V2__seed_catalogs.sql`: inserta clientes y zonas base para pruebas.

## Swagger y OpenAPI

- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Logs estructurados

Los logs salen en JSON e incluyen `correlationId` desde `X-Correlation-Id` o uno generado automáticamente.

## Límites conocidos

- El parser soporta el esquema definido por el reto y no contempla columnas adicionales complejas ni campos con comas escapadas fuera del soporte estándar de Commons CSV.
- El dedupe entre cargas distintas depende de `Idempotency-Key + hash`, no solo del contenido.
- Ante concurrencia exacta de la misma carga, la segunda petición puede recibir `409` si la primera sigue en proceso.

## Archivos incluidos

- `samples/pedidos-validos.csv`
- `samples/pedidos-mixto.csv`
- `postman/PedidosCarga.postman_collection.json`

## Respuesta esperada

```json
{
  "totalProcesados": 4,
  "guardados": 2,
  "conError": 2,
  "errores": [
    {
      "linea": 3,
      "tipo": "CLIENTE_NO_ENCONTRADO",
      "mensaje": "El cliente CLI-404 no existe o está inactivo"
    }
  ],
  "erroresAgrupados": [
    {
      "tipo": "CLIENTE_NO_ENCONTRADO",
      "cantidad": 1
    }
  ]
}
```
