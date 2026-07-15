# Integracion con backend

## Configuracion HTTP

El frontend registra `HttpClient` en:

```text
FE/src/app/app.config.ts
```

Tambien registra el interceptor:

```text
authInterceptor
```

## Endpoints centralizados

Los paths de API se centralizan en:

```text
FE/src/app/core/constants/api/api-endpoints.constants.ts
```

Dominios definidos:

- `health`.
- `auth`.
- `users`.
- `cards`.
- `deck`.
- `games`.
- `matchmaking`.

## Servicios HTTP

Servicios actuales:

- `AuthApiService`: login, refresh, logout, me, verificacion y recuperacion.
- `UserApiService`: perfil y usuario.
- `CardsApiService`: listado y busqueda de cartas.
- `DeckApiService`: mazo, validacion, activacion y randomizacion.
- `GameApiService`: partida, pausa, reanudacion, acciones, historial, chat, eventos y snapshot.
- `MatchmakingApiService`: cola.
- `HealthApiService`: health.

Estado: Implementado.

## Sesion y tokens

El frontend usa:

```text
FE/src/app/core/storage/storage.service.ts
```

Responsabilidades:

- Guardar sesion.
- Leer access token y refresh token.
- Saber si el usuario esta autenticado.

El interceptor:

- Adjunta JWT a requests protegidos.
- Intenta refresh ante errores de autenticacion cuando corresponde.

Estado: Implementado.

## WebSocket/realtime

Servicios relevantes:

```text
FE/src/app/core/services/stomp-realtime.service.ts
FE/src/app/features/play/services/matchmaking-realtime.service.ts
FE/src/app/features/game/services/game-realtime.service.ts
FE/src/app/features/game/services/game-facade.service.ts
```

Destinos consumidos o publicados:

```text
/user/queue/matchmaking
/user/queue/games/{gameId}
/topic/games/{gameId}
/topic/games/{gameId}/visual-events
/app/games/{gameId}/state-sync
```

Estado: Parcialmente implementado.

El transporte STOMP existe. El pendiente no es crear el cliente desde cero, sino validar estabilidad en dos clientes y corregir los casos donde el tablero queda atrasado.

## Snapshot de juego y tablero

El Front define contratos crudos para el snapshot en:

```text
FE/src/app/core/models/interfaces/game/game-snapshot.interface.ts
```

El tablero transforma ese snapshot a un ViewModel visual con:

```text
FE/src/app/features/game/domain/board/game-board.mapper.ts
```

Los componentes visuales consumen `BoardGameViewModel`. Las acciones pasan por `GameFacadeService`; los componentes no deben llamar directo a endpoints de juego.

### Datos confirmados en snapshot/estado

`GameStateDto`/snapshot expone, entre otros:

- `gameId`.
- `status`.
- `stateVersion`.
- `playerIds`.
- `players`.
- `turn`.
- `board`.
- `actions`.
- `resolution`.
- `updatedAt`.

`ActionStateDto` expone:

- `availableActions`.
- `processedClientActionIds`.

`ResolutionStateDto` expone:

- `resolutionType`.
- `playerToPromoteId`.
- `nextActivePlayerId`.
- `nextTurnNumber`.

### Reglas actuales del mapper

- Mano propia: visible para el jugador local.
- Mano rival: oculta.
- Premios: ocultos con contador.
- Mazo: oculto con contador.
- Setup rival antes del reveal: oculto.
- Acciones disponibles: derivadas desde `actions.availableActions`.
- `USE_ABILITY`: preparada pero deshabilitada hasta soporte backend.

### Pendientes de contrato o UI

- Validar si todos los datos visuales de cartas publicas llegan completos en snapshots reales.
- Confirmar que energias, dano, evolution stack, ataques y abilities se renderizan con suficiente metadata en todos los casos.
- Confirmar que la sanitizacion backend de zonas privadas cumple lo esperado para ambos jugadores.
- Corregir actualizacion tardia de banca, retirada y acciones visibles despues de `END_TURN`.

## Acciones de juego desde frontend

Las acciones principales se envian mediante `GameFacadeService`:

- `chooseInitialPokemon(activeCardInstanceId, benchCardInstanceIds)`.
- `drawCard()`.
- `playBasicPokemon(cardId)`.
- `attachEnergy(cardId, pokemonInPlayId)`.
- `retreat(targetPokemonInPlayId)`.
- `declareAttack(attackId, targetPokemonInPlayId)`.
- `endTurn()`.
- `promoteBenchPokemon(pokemonInPlayId)`.
- `evolvePokemon(cardId, pokemonInPlayId)`.
- `playTrainer(cardId, targetPokemonInPlayId)`.

Cada request debe incluir `clientActionId` UUID puro y `expectedStateVersion` del snapshot vigente.

Ver tambien:

- [Guia de prueba manual](../../docs/game/guia-prueba-manual-dos-jugadores.md)
- [Payloads de acciones](./game-action-payloads-validation.md)
- [Bugs conocidos](../../docs/game/bugs-conocidos-y-pendientes.md)
