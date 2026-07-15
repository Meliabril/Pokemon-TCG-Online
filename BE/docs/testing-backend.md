# Testing backend

## Stack

El backend usa:

- JUnit 5.
- Mockito.
- Spring Boot Test.
- MockMvc.
- JaCoCo.
- H2 para tests.

## Ubicacion

Los tests estan en:

```text
BE/src/test/java/ar/edu/utn/frc/tup/piii
```

La configuracion de tests esta en:

```text
BE/src/test/resources/application.properties
```

## Areas cubiertas

Se detectaron tests para:

- Controllers: auth, usuarios, cartas, mazos, partidas, matchmaking, health.
- Services: auth, usuarios, cartas, mazos, matchmaking, juego, snapshots, logs, realtime.
- Repositories: partidas, participantes, eventos, cartas en partida y entidades relacionadas.
- Security: interceptor STOMP JWT.
- Config: security, websocket, springdoc, seeders.
- Validadores de juego.

## Comandos

Desde `BE/`:

```bash
./mvnw test
```

En Windows:

```powershell
.\mvnw.cmd test
```

Para ejecutar un subconjunto:

```bash
./mvnw -Dtest=NombreDelTest test
```

## Cobertura

JaCoCo esta configurado en `BE/pom.xml`.

No confirmado:

- Cobertura real actual.
- Si se alcanza el objetivo historico de 80% global.
- Si `RuleValidator`, `DamageCalculatorService` y `StatusEffectService` alcanzan los porcentajes definidos en el SDD.

Motivo: esta documentacion no ejecuto la suite de tests ni genero reporte de cobertura.

## Huecos recomendados para revisar

- Tests end-to-end con filtros reales de seguridad.
- Ownership en endpoints de usuario y mazo.
- Flujo completo de matchmaking a partida.
- Cobertura de acciones de juego sin handler confirmado.
- Persistencia y visibilidad de datos ocultos para rival.
- Tests de integracion con frontend no presentes.
