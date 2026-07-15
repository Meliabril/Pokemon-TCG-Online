# Motor de juego

## Objetivo

El motor de juego ejecuta acciones de una partida Pokemon TCG desde backend. El cliente no aplica reglas finales: envia intenciones, recibe snapshots/eventos y renderiza estado confirmado.

## Flujo de accion

1. El cliente llama `POST /api/games/{gameId}/actions`.
2. `GameController` valida entrada y usuario autenticado.
3. `GameServiceImpl` carga partida y estado.
4. Se construye `GameActionContext`.
5. `RuleValidator` ejecuta validadores.
6. `GameActionExecutorImpl` busca un handler por `GameActionType`.
7. El handler aplica cambios mediante servicios especializados.
8. Se restauran entidades si corresponde.
9. Se guardan snapshots, logs y eventos.
10. Se publica `STATE_SYNC` privado para los jugadores.
11. Se devuelve `GameActionResponseDto` con `newStateVersion`.

## Validadores

Estado: Implementado como base modular.

Validadores detectados:

- Existencia de partida.
- Estado de partida.
- Participante.
- Version de estado.
- Idempotencia por `clientActionId`.
- Turno del jugador.
- Fase.
- Ownership de carta.
- Energia por turno.
- Partidario por turno.
- Retirada por turno.
- Disponibilidad de ataque.
- Costo de energia.
- Condiciones especiales.
- Evolucion.
- Capacidad de banca.
- Promocion pendiente.

## Handlers y acciones

| Accion | Estado | Observacion |
| --- | --- | --- |
| `START_GAME` | Implementado | Prepara mazos, manos, premios, mulligan y pasa a `SETUP`. |
| `CHOOSE_INITIAL_POKEMON` | Implementado | Selecciona activo y banca inicial. |
| `DRAW_CARD` | Implementado | Robo obligatorio en fase `DRAW`. |
| `PLAY_BASIC_POKEMON` | Implementado | Baja Pokemon basico a banca. |
| `ATTACH_ENERGY` | Implementado | Une energia a Pokemon propio. |
| `EVOLVE_POKEMON` | Implementado | Evoluciona sobre stack, preserva dano y limpia condiciones. |
| `PLAY_TRAINER` | Parcialmente implementado | Solo efectos registrados. |
| `USE_ABILITY` | Pendiente controlado | Existe handler minimo que rechaza la accion. |
| `RETREAT` | Implementado | Paga coste, intercambia activo/banca y limpia condiciones. |
| `DECLARE_ATTACK` | Implementado | Resuelve ataque, dano, efectos soportados, KO y salida. |
| `END_TURN` | Implementado | Cambia fase o turno. |
| `PROMOTE_BENCH_POKEMON` | Implementado | Resuelve promocion manual tras KO del activo. |
| `TAKE_PRIZE_CARD` | Automatico/no manual | Premios tomados por `PrizeService`. |
| `CREATE_GAME`, `JOIN_GAME`, `PAUSE_GAME`, `RESUME_GAME`, `CONCEDE`, `SELECT_TARGET` | Parcial o externo al flujo principal | Algunas se resuelven por endpoints separados o requieren validacion. |

## Servicios especializados

El motor se apoya en servicios de dominio:

- `SetupService`.
- `TurnService`.
- `MainPhaseActionService`.
- `PlayBasicPokemonService`.
- `AttachEnergyService`.
- `TrainerEffectService`.
- `EvolutionService`.
- `RetreatService`.
- `AttackService`.
- `DamageCalculatorService`.
- `DamageApplicationService`.
- `CombatResolutionService`.
- `KnockoutService`.
- `PrizeService`.
- `PromotionService`.
- `VictoryConditionService`.
- `GameSnapshotService`.
- `GameRealtimeEventService`.

## Estado actual

Implementado:

- Pipeline de accion.
- Validacion modular.
- Handlers principales del MVP.
- Persistencia de snapshots/logs/eventos.
- Sincronizacion por `STATE_SYNC`.
- Setup, mulligan automatico, turno random, robo, banca, energia, retirada, ataque, KO, premios, promocion y fin de partida por condiciones principales.

Parcialmente implementado:

- Cobertura de todos los ataques/efectos XY1.
- Trainers completos.
- Habilidades.
- Validacion manual de partida completa desde UI.

Bugs/observaciones de integracion:

- La UI puede actualizar banca o retirada tarde.
- Algunas acciones se observan recien despues de `END_TURN`.
- `STATE_SYNC` debe validarse en ambos clientes despues de cada accion.

Ver tambien:

- [Arquitectura detallada del motor](./motor-arquitectura-detallada.md)
- [Estado actual del juego](../../docs/game/estado-actual-del-juego.md)
- [Bugs conocidos y pendientes MVP](../../docs/game/bugs-conocidos-y-pendientes.md)
