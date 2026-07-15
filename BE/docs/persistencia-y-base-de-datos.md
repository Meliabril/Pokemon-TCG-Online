# Persistencia y base de datos

## Configuracion actual

El backend tiene Spring Data JPA y Flyway como dependencias, pero la configuracion activa usa H2 en memoria y no ejecuta Flyway.

Archivo:

```text
BE/src/main/resources/application.properties
```

Configuracion relevante:

```properties
spring.datasource.url=jdbc:h2:mem:pokemon_tcg;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
spring.jpa.hibernate.ddl-auto=create
spring.flyway.enabled=false
```

Estado: Implementado para ejecucion local con H2.

## Migraciones

Existen migraciones SQL en:

```text
BE/src/main/resources/db/migration
```

Incluyen tablas para:

- Usuarios y tokens.
- Cartas, ataques, costos, debilidades y resistencias.
- Mazos.
- Partidas y participantes.
- Instancias de cartas.
- Logs, snapshots y eventos.
- Matchmaking.
- Condiciones especiales y estado de turno.
- Verificacion de cuenta y recuperacion de contrasena.

Estado: Documentado y versionado, pero no activo en runtime porque Flyway esta desactivado.

## Diferencia con el SDD

`BE/SDD_Pokemon_TCG_Backend.md` define PostgreSQL como stack obligatorio. El codigo actual, sin embargo, ejecuta H2 en memoria.

Esto debe documentarse como inconsistencia:

- PostgreSQL: Documentado pero no encontrado como runtime activo.
- H2: Implementado como runtime actual.
- Flyway: disponible, con migraciones, pero desactivado.

## Entidades y repositorios

La persistencia se implementa con entidades JPA y repositorios Spring Data.

Capas:

- `entities`: modelo persistente.
- `repositories`: contratos de acceso a datos.
- `services`: transacciones y reglas.

Ejemplos:

- `CardRepository` usa consultas con relaciones para detalle de cartas.
- `DeckRepository` busca mazos por owner y activo.
- `GameRepository` trabaja con estado de partida.
- `GameStateSnapshotRepository` recupera ultimo snapshot.
- `MatchmakingQueueEntryRepository` administra la cola.

## Consideraciones para continuar

Antes de presentar el backend como PostgreSQL-ready en una entrega final, conviene:

1. Activar Flyway en un perfil real.
2. Cambiar datasource a PostgreSQL.
3. Revisar compatibilidad de tipos SQL usados en migraciones.
4. Evitar `ddl-auto=create` en ambientes persistentes.
5. Documentar perfiles `local`, `test` y `prod`.
