# Pendientes backend

## Pendientes funcionales

### Completar motor de acciones

Estado: Pendiente/Parcial.

Acciones declaradas sin handler directo confirmado:

- `PLAY_TRAINER`.
- `SELECT_TARGET`.
- `PROMOTE_BENCH_POKEMON`.
- `TAKE_PRIZE_CARD`.
- `CONCEDE`.
- `CREATE_GAME`.
- `JOIN_GAME`.
- `PAUSE_GAME`.
- `RESUME_GAME`.

Recomendacion:

- Definir si cada accion pertenece al motor o a endpoints REST separados.
- Si pertenece al motor, agregar handler y tests.
- Si no pertenece, documentar explicitamente por que esta en el enum.

### Realtime de acciones

Estado: Parcialmente implementado.

Backend tiene STOMP y `STATE_SYNC`. Falta confirmar si todas las acciones de juego deben poder entrar por STOMP o solo por REST.

## Pendientes tecnicos

### PostgreSQL y Flyway

Estado: Documentado pero no activo.

El SDD pide PostgreSQL, pero runtime actual usa H2 en memoria y Flyway desactivado.

Recomendacion:

- Crear perfil local H2 y perfil productivo PostgreSQL.
- Activar Flyway donde corresponda.
- Revisar migraciones contra PostgreSQL real.

### Seguridad de entorno

Estado: Parcial.

Riesgos:

- JWT secret con fallback.
- H2 console habilitada.
- Swagger expuesto.
- `ddl-auto=create`.

Recomendacion:

- Separar configuracion local y productiva.
- Exigir secreto externo.
- Restringir herramientas de desarrollo fuera de local.

### Tests de autorizacion

Estado: No confirmado.

Hay tests de controllers, pero conviene verificar casos con filtros reales de seguridad y ownership.

## Pendientes de documentacion

- Generar diagramas Excalidraw.
- Confirmar cobertura JaCoCo.
- Documentar contratos OpenAPI exportados si se requiere entrega formal.
- Mantener una matriz de diferencias entre SDD historico y codigo real.
