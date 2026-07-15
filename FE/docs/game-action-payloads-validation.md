# Validacion de payloads Front vs Backend

## Alcance

Revision de cierre para acciones de partida enviadas por `GameFacadeService.executeAction()`.

Esta revision deja explicito que las acciones disponibles se derivan desde `availableActions`, y que Front no envia campos sin consumo Backend actual.

## Contrato comun de acciones

Todas las acciones viajan dentro de `GameActionRequest` con:

| Campo | Estado | Observacion |
|---|---|---|
| `gameId` | OK | Front lo envia como string UUID; Backend lo recibe como `UUID`. |
| `clientActionId` | OK | Corregido en Fase 1 para enviar UUID puro. |
| `actionType` | OK | Front lo envia separado del payload. |
| `expectedStateVersion` | OK | Front usa `snapshot.stateVersion`; Backend valida concurrencia optimista. |
| `payload` | OK | Backend lo recibe como `Map<String, Object>`. |

## Disponibilidad de acciones

| Punto | Estado | Decision |
|---|---|---|
| `canEvolve` | Cerrado | No se agrega booleano al DTO. Front deriva evolucion desde `actions.availableActions` con `EVOLVE_POKEMON`. |
| `canRetreat` | Cerrado | No se agrega booleano al DTO. Front deriva retreat desde `actions.availableActions` con `RETREAT`. |

## Acciones principales

| Accion | Estado | Front payload | Backend esperado | Observacion |
|---|---|---|---|---|
| `CHOOSE_INITIAL_POKEMON` | OK | `{ activeCardInstanceId: string; benchCardInstanceIds: string[] }` | `activeCardInstanceId` UUID requerido; `benchCardInstanceIds` lista opcional de UUID. | Coincide con `SetupServiceImpl`. Backend espera ids de instancia de carta, no `cardId`. |
| `PLAY_BASIC_POKEMON` | OK | `{ cardId: string }` | `cardId` UUID requerido. | Coincide con `PlayBasicPokemonServiceImpl`; Backend busca una instancia en mano por `cardId`. |
| `ATTACH_ENERGY` | OK | `{ cardId: string; pokemonInPlayId: string }` | `cardId` UUID requerido; `pokemonInPlayId` UUID requerido. | Coincide con `AttachEnergyServiceImpl`. |
| `DECLARE_ATTACK` | OK | `{ attackId: string; targetPokemonInPlayId?: string }` | `attackId` UUID requerido; `targetPokemonInPlayId` opcional. | Coincide con `AttackResolutionContextFactoryImpl` y `AttackTargetResolverServiceImpl`; si no se envia target, Backend usa el Pokemon activo defensor. |
| `END_TURN` | OK | `{}` | Payload no usado. | Coincide con `EndTurnActionHandler` y `TurnServiceImpl`. |
| `PROMOTE_BENCH_POKEMON` | OK | `{ pokemonInPlayId: string }` | `pokemonInPlayId` UUID requerido. | Coincide con `PromotionServiceImpl`; solo valido cuando hay promotion pendiente. |

## Acciones avanzadas revisadas

| Accion | Estado | Front payload | Backend esperado | Observacion |
|---|---|---|---|---|
| `EVOLVE_POKEMON` | OK | `{ cardId: string; pokemonInPlayId: string }` | `cardId` UUID requerido; `pokemonInPlayId` UUID requerido. | Coincide con `EvolutionServiceImpl`. |
| `PLAY_TRAINER` | OK | `{ cardId: string; targetPokemonInPlayId?: string }` | `cardId` UUID requerido; `targetPokemonInPlayId` requerido solo por efectos que apunten a Pokemon en juego. | `TrainerEffectServiceImpl` siempre exige `cardId`. `HealDamageTrainerEffect` consume `targetPokemonInPlayId`. Front no envia `targetCardId` porque Backend no lo consume para Trainer. |
| `RETREAT` | OK | `{ targetPokemonInPlayId: string }` | `targetPokemonInPlayId` UUID requerido. | Coincide con `RetreatServiceImpl`; representa el Pokemon de banca que pasa a activo. |
| `USE_ABILITY` | Pendiente controlado | `{}` | Backend reconoce `USE_ABILITY`, pero su handler minimo lo rechaza con `InvalidGameActionException`. | Se mantiene como pendiente/deshabilitada hasta que exista motor real de habilidades. |

## Mismatches o pendientes

| Punto | Estado | Detalle | Siguiente paso |
|---|---|---|---|
| `PLAY_TRAINER.targetCardId` | Cerrado | Front ya no lo permite ni lo envia en `PlayTrainerPayload`. | Reintroducirlo solo si Backend agrega un efecto/contrato que lo requiera. |
| `USE_ABILITY` | Cerrado como pendiente controlado | Front contempla la accion y Backend ahora la reconoce, pero la rechaza explicitamente porque no hay motor real de habilidades. | Mantenerla deshabilitada hasta que Backend agregue reglas, payload real y exposicion en `availableActions`. |

## USE_ABILITY

Estado: pendiente / deshabilitado.

Front ya contempla `USE_ABILITY` en `GameActionType`. Backend ahora reconoce la accion, pero la rechaza explicitamente con error de dominio porque todavia no existe soporte real para habilidades.

Decision actual:

- No enviar `USE_ABILITY` al Backend.
- Mantenerla preparada en Front.
- Habilitarla unicamente cuando Backend la incluya en `availableActions` y exista handler real de habilidades.

Evidencia:

- `GameFacadeService.executeAction()` rechaza `GameActionType.UseAbility` antes de construir el request HTTP.
- `hasAction(...)` devuelve `false` para `GameActionType.UseAbility`.
- `game-board.mapper.ts` muestra `USE_ABILITY` como accion visual deshabilitada con motivo de soporte Backend pendiente.
- Backend incluye `USE_ABILITY` en `GameActionType`, pero el handler minimo devuelve error controlado.
- `PlayTrainerPayload` ya no incluye `targetCardId`.

## Evidencia revisada

- Front: `FE/src/app/core/models/interfaces/game/game-action-payloads.interface.ts`
- Front: `FE/src/app/features/game/services/game-facade.service.ts`
- Front: `FE/src/app/features/game/domain/game-state.helpers.ts`
- Front: `FE/src/app/features/game/domain/game-board.mapper.ts`
- Backend: `GameActionPayloadReaderImpl`
- Backend: `SetupServiceImpl`
- Backend: `PlayBasicPokemonServiceImpl`
- Backend: `AttachEnergyServiceImpl`
- Backend: `AttackResolutionContextFactoryImpl`
- Backend: `AttackTargetResolverServiceImpl`
- Backend: `TurnServiceImpl`
- Backend: `PromotionServiceImpl`
- Backend: `EvolutionServiceImpl`
- Backend: `TrainerEffectServiceImpl`
- Backend: `HealDamageTrainerEffect`
- Backend: `DrawCardsTrainerEffect`
- Backend: `RetreatServiceImpl`
- Backend: `GameActionType`
