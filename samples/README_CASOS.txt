Supuestos para probar estos CSV:
- Clientes válidos y activos: CLI-123, CLI-999
- ZONA1 existe y soporta refrigeración
- ZONA5 existe y NO soporta refrigeración

Casos:
01_valido.csv -> todo OK
02_numero_pedido_invalido.csv -> NUMERO_PEDIDO_INVALIDO
03_cliente_no_encontrado.csv -> CLIENTE_NO_ENCONTRADO
04_fecha_pasada.csv -> FECHA_INVALIDA
05_fecha_formato_invalido.csv -> FECHA_INVALIDA
06_estado_invalido.csv -> ESTADO_INVALIDO
07_zona_invalida.csv -> ZONA_INVALIDA
08_cadena_frio_no_soportada.csv -> CADENA_FRIO_NO_SOPORTADA
09_refrigeracion_invalida.csv -> REFRIGERACION_INVALIDA
10_duplicado_en_archivo.csv -> DUPLICADO
11_multiples_errores.csv -> múltiples errores en una fila
12_mixto_validos_y_errores.csv -> mezcla de guardados y errores
13_idempotencia_base.csv + 14_idempotencia_conflicto.csv -> usar la misma Idempotency-Key para probar conflicto si tu lógica lo maneja por misma key con hash distinto
