# Bloqueantes Backend - Integracion de partida

## Alcance

Cierre de estado Backend para las tareas TEJE de integracion de partida.

Backend ya sincroniza snapshot visible despues de acciones exitosas y reconoce `USE_ABILITY` con rechazo controlado. La implementacion real de habilidades queda como tarea Backend separada. La disponibilidad de evolucion y retreat se deriva desde `availableActions`, sin booleanos adicionales en el DTO.

## Confirmados

| Bloqueante | Impacto | Evidencia | Responsable sugerido |
|---|---|---|---|
| `clientActionId` debe ser UUID puro | Acciones reales pueden fallar si Front envia prefijos como `END_TURN-uuid`. | `GameActionRequestDto.clientActionId` es `UUID`; `GameServiceImpl` usa ese id para idempotencia/log. | Front / Backend |
| `USE_ABILITY` sin motor real | Cerrado como pendiente controlado: no se puede habilitar como accion jugable real. | Backend incluye `USE_ABILITY` y registra un handler minimo, pero lo rechaza con `InvalidGameActionException`; Front lo bloquea antes de enviar HTTP. | Backend |
| `PLAY_TRAINER.targetCardId` sin consumo actual | Cerrado: Front no envia ese campo para efectos Trainer actuales. | `TrainerEffectServiceImpl` exige `cardId`; `HealDamageTrainerEffect` consume `targetPokemonInPlayId`; no se encontro efecto Trainer que consuma `targetCardId`. | Backend / Front |

## Confirmados OK

| Punto | Estado | Evidencia | Observacion |
|---|---|---|---|
| Payloads principales | Confirmado | Handlers Backend consumen los campos documentados en `game-action-payloads-validation.md`. | `CHOOSE_INITIAL_POKEMON`, `PLAY_BASIC_POKEMON`, `ATTACH_ENERGY`, `DECLARE_ATTACK`, `END_TURN` y `PROMOTE_BENCH_POKEMON` quedaron alineados. |
| `availableActions` | Confirmado | `GameStateQueryServiceImpl` arma acciones disponibles segun status, fase y resolucion. | Si hay promocion pendiente, expone solo `PROMOTE_BENCH_POKEMON`. |
| `canEvolve` / `canRetreat` | Cerrado | No existen como booleanos en `GameStateDto`; Front deriva disponibilidad desde `availableActions`. | `EVOLVE_POKEMON` y `RETREAT` aparecen en MAIN phase cuando corresponden. |
| `stateVersion` | Confirmado | `GameActionRequestDto.expectedStateVersion` se valida en `GameServiceImpl`; `GameStateDto` expone `stateVersion`. | El contrato soporta concurrencia optimista y errores `409`. |
| `resolution` | Confirmado | `GameStateQueryServiceImpl` construye `ResolutionStateDto` desde `game.getResolutionState()`. | Soporta promocion pendiente y bloqueo de acciones durante resoluciones. |
| Snapshot post accion | Confirmado | `GameServiceImpl.executeAction()` llama a `gameSnapshotService.saveSnapshot(...)` despues de ejecutar la accion. | La respuesta HTTP devuelve `newStateVersion`, no el snapshot completo. |
| Evento de mulligan | Confirmado | `SetupServiceImpl` emite `MULLIGAN_HAND_REVEALED`; hay cobertura en tests de setup/contrato. | Se mantiene como evento de setup. |
| Evento de reveal inicial | Confirmado | `SetupServiceImpl` emite `INITIAL_BOARD_REVEALED` cuando se completa la seleccion inicial. | Se usa para revelar estado inicial del tablero. |
| `STATE_SYNC` | Confirmado | `GameServiceImpl` envia `STATE_SYNC` privado a cada jugador despues de acciones exitosas. | Tambien existe `GameStateSyncUseCaseImpl` para sync explicito por WebSocket. |

## Eventos y sincronizacion

| Punto | Estado | Observacion |
|---|---|---|
| Mulligan | Confirmado | Existe `MULLIGAN_HAND_REVEALED` y se emite en setup. |
| Reveal | Confirmado | Existe `INITIAL_BOARD_REVEALED` y se emite al completar seleccion inicial. |
| Snapshot post accion | Confirmado | `GameServiceImpl` persiste snapshot con la nueva version despues de ejecutar acciones. |
| Evento WS post accion | Confirmado | `GameServiceImpl` despacha eventos emitidos por handlers y luego envia `STATE_SYNC` privado a cada jugador. |
| `availableActions` | Confirmado | Incluido en `GameStateDto.actions.availableActions`. |
| `stateVersion` | Confirmado | Incluido en snapshot y respuesta de accion. |
| `resolution` | Confirmado | Incluido en `GameStateDto.resolution`. |

## Pendientes de validacion manual

| Punto | Por que importa | Como validarlo |
|---|---|---|
| Flujo Usuario A / Usuario B por WebSocket | Confirma que ambos clientes reciben eventos reales y no solo snapshots HTTP. | Ejecutar partida real con dos usuarios y observar eventos recibidos en ambos clientes. |
| Necesidad de solicitar `/state-sync` despues de cada accion | Ya no deberia ser necesario para acciones exitosas normales. | Ejecutar una accion real y verificar que ambos clientes reciban `STATE_SYNC` privado post accion. |
| `PLAY_TRAINER.targetCardId` futuro | Solo aplica si se agrega un nuevo efecto Trainer que lo necesite. | Reabrir contrato y payload cuando Backend defina ese consumo. |
| `USE_ABILITY` | Define contrato final para habilidades. | Backend debe reemplazar el handler minimo por reglas reales, payload, tests y exposicion en `availableActions`. |

## Decision actual

- Mantener `USE_ABILITY` deshabilitada en Front.
- No enviar `targetCardId` para Trainer hasta que Backend lo soporte.
- Derivar evolucion y retreat desde `availableActions`; no agregar `canEvolve` ni `canRetreat`.
- Usar `STATE_SYNC` post accion como mecanismo esperado de resincronizacion.
- Continuar con las fases siguientes del SDD despues de validar esta documentacion.
