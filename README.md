# ms-order-loading

Microservicio en Java 17 y Spring Boot 3 para cargar pedidos desde archivos CSV, validarlos y persistirlos usando arquitectura hexagonal, procesamiento batch e idempotencia.

## Stack tecnológico

- Java 17
- Spring Boot 3.4.4
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Security OAuth2 Resource Server con JWT
- Springdoc OpenAPI
- JaCoCo
- MapStruct
- Lombok

## Arquitectura

La solución está organizada en capas:

- `domain`: modelos y reglas de negocio puras
- `application`: casos de uso y orquestación
- `adapter.in`: entrada REST
- `adapter.out`: persistencia JPA y adaptadores externos
- `config`: configuración general, seguridad, OpenAPI y batch

## Estructura principal

```text
src/main/java/com/reto/ms_order_loading
├── adapter
│   ├── in
│   └── out
├── application
├── config
├── domain
└── MsOrderLoadingApplication.java
```

## Objetivo del microservicio

Exponer un endpoint para recibir un archivo CSV con pedidos, validarlo por filas, guardar únicamente los registros válidos y devolver un resumen del procesamiento.

## Reglas de negocio implementadas

Cada fila del CSV valida lo siguiente:

- `numeroPedido`: obligatorio, alfanumérico y no duplicado
- `clienteId`: debe existir y estar activo
- `fechaEntrega`: no puede ser una fecha pasada, considerando `America/Lima`
- `estado`: debe ser `PENDIENTE`, `CONFIRMADO` o `ENTREGADO`
- `zonaEntrega`: debe existir
- `requiereRefrigeracion`: debe ser `true` o `false`
- si `requiereRefrigeracion = true`, la zona debe soportar refrigeración

## Decisiones de diseño

- Se eligió Spring MVC porque el reto trabaja con carga de archivos y persistencia JPA batch
- El procesamiento se realiza por lotes configurables con `app.batch.size`
- La idempotencia se maneja con `Idempotency-Key + hash SHA-256 del archivo`
- Se almacena `status` y `response_payload` en `cargas_idempotencia` para devolver la misma respuesta ante reintentos del mismo archivo
- Los catálogos y duplicados se consultan por batch para evitar validaciones fila por fila contra base de datos
- La fecha se valida usando `Clock` en zona `America/Lima`

## Supuestos

- `numeroPedido` usa la regex `^[A-Za-z0-9]+$`
- Un cliente válido es uno existente con `activo = true`
- Si llega el mismo archivo con la misma `Idempotency-Key`, se devuelve la misma respuesta sin volver a persistir pedidos
- Si llega la misma llave mientras la carga sigue en proceso, se responde `409 Conflict`
- El volumen esperado es hasta 1000 filas por archivo

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

Rango permitido: de `500` a `1000`.

## Autenticación JWT local

El proyecto protege los endpoints con JWT firmado con llave HMAC local configurada en `application.yml`.

Header requerido:

```text
Authorization: Bearer <token>
```

Token local de ejemplo:

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

- `file`: archivo CSV en UTF-8

## curl de ejemplo

```bash
curl --location 'http://localhost:8080/pedidos/cargar' \
  --header 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJyZXRvLXRlY25pY28iLCJzY29wZSI6InBlZGlkb3Mud3JpdGUifQ.PynS75yxIEFvX3z_DOkM70ZzHlMsknqSLY-pSv7OEAw' \
  --header 'Idempotency-Key: carga-001' \
  --form 'file=@samples/pedidos-validos.csv'
```

## Estrategia de procesamiento batch

1. Se lee el archivo CSV
2. Se divide en lotes configurables
3. Por cada lote se consultan en bloque:
   - clientes activos existentes
   - zonas existentes
   - pedidos ya registrados
4. Se valida cada fila usando estructuras en memoria
5. Solo los registros válidos se envían a `saveAll`
6. Hibernate agrupa inserts usando batch JDBC

## Migraciones Flyway

- `V1__init_schema.sql`: creación de tablas, índices y restricciones
- `V2__seed_catalogs.sql`: carga inicial de clientes y zonas para pruebas

## Swagger y OpenAPI

- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Logs estructurados

Los logs se generan en formato JSON e incluyen `correlationId`, obtenido desde `X-Correlation-Id` o generado automáticamente si no viene en la request.

## Carpetas incluidas en el proyecto

- `Colección Postman`
- `samples`

## Archivos de apoyo

En el proyecto se incluyen archivos de ejemplo para pruebas manuales:

- carpeta `samples` con CSV de prueba
- carpeta `Colección Postman` con la colección para consumir el endpoint

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

## Límites conocidos

- El parser soporta únicamente la estructura definida en el reto
- El dedupe entre cargas distintas depende de `Idempotency-Key + hash`
- Ante concurrencia exacta de la misma carga, la segunda petición puede responder `409 Conflict`
- El comportamiento batch está optimizado para el rango planteado en el reto
