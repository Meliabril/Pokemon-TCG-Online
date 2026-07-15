# Estado actual del juego

Este documento resume el estado real del juego Pokemon TCG en el repositorio. La fuente de verdad es el codigo actual; cuando una funcionalidad no pudo confirmarse por codigo o por validacion manual previa, se marca como requiere validacion.

## Resumen ejecutivo

El proyecto ya tiene una base jugable parcial: dos usuarios pueden autenticarse, preparar mazos validos, entrar a matchmaking, crear una partida, recibir snapshots, completar setup inicial y ejecutar acciones de motor desde backend. El frontend ya contiene tablero, fachada de juego, servicios HTTP y servicios STOMP, pero el MVP todavia no debe considerarse completamente jugable por bugs de sincronizacion visual y por falta de validacion manual completa de una partida hasta finalizacion.

Estado del MVP: parcial. El backend implementa la mayor parte del ciclo de juego. El frontend puede representar tablero y enviar acciones, pero algunas actualizaciones visuales llegan tarde o dependen de `STATE_SYNC`/`END_TURN`.

## Acciones y flujos que funcionan

| Area | Estado | Evidencia |
| --- | --- | --- |
| Registro, login, refresh, logout y usuario actual | Implementado | Backend auth, frontend auth, interceptor JWT. |
| Verificacion de cuenta y recuperacion de password | Implementado | Controladores y pantallas dedicadas. |
| Consulta e importacion de cartas XY1 | Implementado | `CardController`, `AdminCardController`, servicios de cartas. |
| Armado, validacion, activacion y randomizacion de mazos | Implementado | `DeckController`, validaciones de 60 cartas, XY1, maximo 4 copias y Pokemon basico. |
| Matchmaking 1 vs 1 | Implementado | `MatchmakingController`, `MatchmakingServiceImpl`, notificaciones privadas. |
| Bootstrap de partida | Implementado | `MatchGameBootstrapServiceImpl`. |
| Snapshots, logs y eventos | Implementado | `GameStateSnapshot`, `GameActionLog`, `GameEvent`, servicios de query/sync. |
| WebSocket backend | Implementado | `/ws`, JWT STOMP, `/topic/games/{gameId}`, `/user/queue/games/{gameId}`. |
| WebSocket frontend | Parcialmente implementado | `StompRealtimeService`, `GameRealtimeService`, `MatchmakingRealtimeService`; requiere validacion manual estable. |
| Tablero frontend | Parcialmente implementado | Componentes de `features/game`, mapper de snapshot y `GameFacadeService`. |

## Acciones de partida confirmadas

| Accion | Backend | Frontend | Observacion |
| --- | --- | --- | --- |
| `START_GAME` | Implementado | Uso indirecto/automatico por presencia o API | Pasa de `WAITING` a `SETUP`. |
| `CHOOSE_INITIAL_POKEMON` | Implementado | Implementado en fachada/UI de setup | Requiere `activeCardInstanceId` y banca opcional. |
| `DRAW_CARD` | Implementado | Implementado | Robo obligatorio al inicio del turno. |
| `PLAY_BASIC_POKEMON` | Implementado | Implementado | Baja Pokemon basico desde mano a banca. |
| `ATTACH_ENERGY` | Implementado | Implementado | Una energia por turno. |
| `EVOLVE_POKEMON` | Implementado | Implementado | Sujeto a reglas de evolucion; requiere validacion manual amplia. |
| `PLAY_TRAINER` | Parcialmente implementado | Implementado como payload | Solo efectos registrados; no cubrir todos los Trainers. |
| `RETREAT` | Implementado | Implementado | Hay bug visual conocido de sincronizacion tardia. |
| `DECLARE_ATTACK` | Implementado | Implementado | Cubre ataques soportados por definiciones curadas. |
| `END_TURN` | Implementado | Implementado | Cambia fase/turno segun estado. |
| `PROMOTE_BENCH_POKEMON` | Implementado | Implementado | Se usa si hay promocion pendiente tras KO del activo. |
| `TAKE_PRIZE_CARD` | Automatico en backend | No manual | Los premios se toman automaticamente por `PrizeService`. |
| `USE_ABILITY` | Rechazo controlado | Deshabilitado | Pendiente de motor real de habilidades. |
| `CREATE_GAME`, `JOIN_GAME`, `PAUSE_GAME`, `RESUME_GAME`, `CONCEDE`, `SELECT_TARGET` | Parcial o por endpoints separados | Requiere validacion | No forman parte del flujo manual principal actual. |

## Estado de MVP jugable

Implementado para MVP tecnico:

- Dos usuarios con mazos activos validos pueden entrar a matchmaking.
- El backend crea partida y participantes.
- El estado se expone por snapshot visible.
- El setup inicial permite elegir Pokemon activo y banca.
- El backend resuelve mulligan automatico.
- El backend elige aleatoriamente quien empieza.
- El backend controla fases `DRAW`, `MAIN`, `ATTACK` y `BETWEEN_TURNS`.
- El backend aplica acciones principales, ataque, KO, premios y promocion.
- El frontend tiene tablero, fachada, comandos y mapeo de snapshots.

Pendiente para MVP jugable estable:

- Validar una partida manual completa entre dos personas desde UI, sin consola.
- Corregir sincronizacion visual tardia de banca, retirada y acciones visibles solo tras `END_TURN`.
- Confirmar que ambos clientes reciben `STATE_SYNC` post accion en forma estable.
- Confirmar que el frontend no depende de refresh manual para ver cada accion.
- Completar o bloquear claramente habilidades y efectos no soportados.
- Documentar resultado de pruebas reales recientes con fecha, usuarios y acciones recorridas.

## Observaciones y contradicciones detectadas

- Algunas docs generales decian que el cliente STOMP real estaba pendiente. El codigo actual ya contiene `StompRealtimeService`, `GameRealtimeService` y suscripciones de partida; el estado correcto es parcialmente implementado y requiere validacion estable.
- Algunas docs decian que el tablero estaba pendiente. El codigo actual ya tiene componentes de tablero y fachada; el estado correcto es parcialmente implementado con bugs de sincronizacion.
- El SDD historico menciona PostgreSQL como stack obligatorio, pero la configuracion local/documentada usa H2 en varios puntos. Esto queda como contradiccion historica y requiere decision de entrega.
- `TAKE_PRIZE_CARD` existe en el enum, pero la toma de premios actual esta automatizada por backend, no como accion manual de usuario.

## Requiere validacion

- Partida completa hasta `GAME_FINISHED` desde dos navegadores.
- Consistencia de `STATE_SYNC` en ambos clientes despues de cada accion.
- Visualizacion inmediata de banca y retirada sin esperar `END_TURN`.
- Cobertura real de todos los ataques soportados en `game-engine/xy1-attack-effects.json`.
- Estado final de PostgreSQL/Flyway como stack runtime de entrega.
