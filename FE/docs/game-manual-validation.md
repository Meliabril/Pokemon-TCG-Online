# Validacion manual - Flujo de partida real

## Objetivo

Verificar que dos usuarios puedan entrar a matchmaking, llegar a la misma partida, recibir snapshot, conectarse por WebSocket y ver acciones reflejadas en ambos tableros.

## Precondiciones

- Backend corriendo.
- Front corriendo.
- Base de datos inicializada.
- Usuario A creado.
- Usuario B creado.
- Usuario A tiene mazo activo y valido.
- Usuario B tiene mazo activo y valido.
- WebSocket disponible.
- Endpoint `POST /api/matchmaking/queue` disponible.
- Ambos navegadores/sesiones usan usuarios distintos.

## Referencia tecnica para validacion

### Comandos y endpoints disponibles

- Login: `POST /api/auth/login`.
- Entrar a matchmaking: `POST /api/matchmaking/queue`.
- Ver estado propio de queue: `GET /api/matchmaking/queue/me`.
- Salir de queue: `DELETE /api/matchmaking/queue`.
- Detalle de partida: `GET /api/games/{gameId}`.
- Snapshot actual: `GET /api/games/{gameId}/snapshot/latest`.
- Ejecutar accion: `POST /api/games/{gameId}/actions`.
- WebSocket: `ws://localhost:8080/ws`.
- Canales de partida que consume Front:
  - privado: `/user/queue/games/{gameId}`;
  - publico: `/topic/games/{gameId}`;
  - pedido de sync: `/app/games/{gameId}/state-sync`.

### Como disparar acciones desde Front

Todas las acciones jugables deben pasar por `GameFacadeService`; los componentes visuales no deben llamar directo a `GameApiService` ni a endpoints HTTP.

- Generico: `gameFacade.executeAction(actionType, payload)`.
- Setup: `gameFacade.chooseInitialPokemon(activeCardInstanceId, benchCardInstanceIds)`.
- Robo: `gameFacade.drawCard()`.
- Banca: `gameFacade.playBasicPokemon(cardId)`.
- Energia: `gameFacade.attachEnergy(cardId, pokemonInPlayId)`.
- Ataque: `gameFacade.declareAttack(attackId, targetPokemonInPlayId)`.
- Fin de turno: `gameFacade.endTurn()`.
- Promocion: `gameFacade.promoteBenchPokemon(pokemonInPlayId)`.
- Evolucion: `gameFacade.evolvePokemon(cardId, pokemonInPlayId)`.
- Trainer: `gameFacade.playTrainer(cardId, targetPokemonInPlayId)`.
- Retirada: `gameFacade.retreat(targetPokemonInPlayId)`.
- Ability: `gameFacade.useAbility()` queda bloqueado hasta soporte Backend.

La UI debe emitir intenciones de usuario hacia la page o shell, y esa capa debe delegar en `GameFacadeService`. Cada request enviado por el facade incluye `gameId`, `clientActionId` UUID puro, `actionType`, `payload` y `expectedStateVersion` tomado del snapshot vigente.

### Prueba rapida por consola/API

Usar el `stateVersion` y una accion presente en `availableActions` del snapshot actual:

```powershell
$tokenA = '<accessToken usuario A>'
$gameId = '<gameId>'
$snapshotA = Invoke-RestMethod -Headers @{ Authorization = "Bearer $tokenA" } -Uri "http://localhost:8080/api/games/$gameId/snapshot/latest"

$request = @{
  gameId = $gameId
  clientActionId = [guid]::NewGuid().ToString()
  actionType = '<accion disponible, por ejemplo DRAW_CARD>'
  expectedStateVersion = $snapshotA.stateVersion
  payload = @{}
}

Invoke-RestMethod `
  -Method Post `
  -Headers @{ Authorization = "Bearer $tokenA" } `
  -ContentType 'application/json' `
  -Uri "http://localhost:8080/api/games/$gameId/actions" `
  -Body ($request | ConvertTo-Json -Depth 10)
```

Para `409`, repetir una accion valida con un `expectedStateVersion` anterior al snapshot actual. En Front, el resultado esperado es que `GameFacadeService` refresque snapshot y muestre: `La partida se actualizo. El tablero fue sincronizado.`

## Pasos

### 1. Usuario A entra a queue

Resultado esperado:

- Front valida que Usuario A tiene mazo valido.
- Se llama a `POST /api/matchmaking/queue`.
- Si no hay rival disponible, Usuario A queda en espera.
- No se navega a partida hasta recibir `gameId`.

### 2. Usuario B entra a queue

Resultado esperado:

- Front valida que Usuario B tiene mazo valido.
- Se llama a `POST /api/matchmaking/queue`.
- Backend asigna partida.
- Ambos usuarios reciben el mismo `gameId`.

### 3. Navegacion a partida

Resultado esperado:

- Usuario A navega a `/games/{gameId}`.
- Usuario B navega a `/games/{gameId}`.
- La pantalla de partida toma `gameId` desde la ruta.
- No se usa un estado hardcodeado para renderizar el tablero.

### 4. Snapshot inicial HTTP

Resultado esperado:

- Ambos clientes piden snapshot inicial por HTTP.
- Se muestra estado de carga hasta recibir snapshot valido.
- El snapshot incluye `gameId`, `status`, `stateVersion`, `players`, `board`, `actions` y `resolution`.
- Las cartas privadas se muestran segun reglas de visibilidad del snapshot.

### 5. Suscripcion WebSocket

Resultado esperado:

- Ambos clientes se suscriben al canal de la partida.
- No hay error de conexion WebSocket.
- Si se pierde la conexion, Front muestra estado de reconexion.
- Si el cliente solicita sync, Backend puede responder con `STATE_SYNC` privado.

### 6. Estado `SETUP`

Resultado esperado:

- La partida pasa a `SETUP`.
- Cada jugador ve su mano inicial.
- `availableActions` incluye `CHOOSE_INITIAL_POKEMON` cuando corresponde.
- La UI habilita elegir Pokemon inicial solo si la accion esta disponible.

### 7. Accion `CHOOSE_INITIAL_POKEMON`

Resultado esperado:

- Se envia accion con:
  - `gameId`;
  - `clientActionId` como UUID puro;
  - `actionType: CHOOSE_INITIAL_POKEMON`;
  - `expectedStateVersion`;
  - `payload.activeCardInstanceId`;
  - `payload.benchCardInstanceIds`.
- Backend acepta la accion.
- `newStateVersion` aumenta.
- Cuando ambos jugadores completan setup, Backend emite reveal inicial o deja snapshot actualizado.

### 8. Estado `ACTIVE`

Resultado esperado:

- La partida pasa a `ACTIVE`.
- El turno actual se representa correctamente.
- `availableActions` refleja la fase actual.
- `resolution` no bloquea acciones si no hay decision pendiente.

### 9. Accion jugable

Probar al menos una accion real:

- `DRAW_CARD`;
- `PLAY_BASIC_POKEMON`;
- `ATTACH_ENERGY`;
- `DECLARE_ATTACK`;
- `END_TURN`;
- `PROMOTE_BENCH_POKEMON` si hay promocion pendiente.

Resultado esperado:

- La accion se envia por `GameFacadeService.executeAction()`.
- Backend responde success y nuevo `stateVersion`.
- Backend persiste snapshot post accion.
- La accion se refleja en el tablero del actor.
- Ambos clientes reciben `STATE_SYNC` privado post accion.

### 10. Conflicto `409`

Resultado esperado:

- Enviar una accion con `expectedStateVersion` viejo produce conflicto.
- Front muestra mensaje de conflicto de version.
- Front refresca snapshot o solicita sync.
- El tablero queda consistente con la ultima version del Backend.

## Resultado de la validacion

| Paso | Estado | Observacion |
|---|---|---|
| Usuario A entra a queue | OK | `admin@gmail.com` quedo en queue con mazo activo/valido. |
| Usuario B entra a queue | OK | `codex.match.b@example.com` matcheo contra Usuario A. |
| Ambos reciben mismo `gameId` | OK | Match creado: `8311d2c4-0013-43e1-8d19-ec532618010f`. |
| Navegacion a `/games/{gameId}` | OK | Front cargo `/games/8311d2c4-0013-43e1-8d19-ec532618010f` con usuario B logueado. |
| Snapshot inicial HTTP | OK | Ambos usuarios recibieron snapshot `WAITING`, `stateVersion: 0`, `availableActions: START_GAME`. |
| Suscripcion WebSocket | Parcial | Ambos clientes STOMP conectan, pero en las corridas aparece `STOMP_ERROR: Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]` en uno de los clientes. |
| Estado `SETUP` | OK tecnico | `START_GAME` por API paso de `WAITING` a `SETUP`, `stateVersion: 1`, `availableActions: CHOOSE_INITIAL_POKEMON`. |
| `CHOOSE_INITIAL_POKEMON` Usuario A | OK | A eligio Pansage con `activeCardInstanceId: daf4f966-29d5-4621-980b-d81d61a6a6f8`; respuesta 200, `newStateVersion: 2`. |
| `CHOOSE_INITIAL_POKEMON` Usuario B | OK | B eligio Weedle con `activeCardInstanceId: 0845c2f4-be24-4b18-9f54-4488ccb6d398`; respuesta 200, `newStateVersion: 3`. |
| Evento reveal o snapshot actualizado | OK | Al completar setup se observaron `INITIAL_BOARD_REVEALED`, `GAME_STARTED` y snapshot final `ACTIVE`. |
| Estado `ACTIVE` | OK tecnico | Snapshot quedo `ACTIVE`, turno 1, fase `DRAW`, jugador activo B. |
| Accion jugable reflejada en actor | OK | B ejecuto `DRAW_CARD`; mano propia paso de 8 a 9 y fase cambio a `MAIN`. |
| Accion jugable reflejada en rival | OK | Snapshot de A quedo en `stateVersion: 4`, fase `MAIN`, con mano rival oculta y contadores actualizados. |
| Snapshot post accion persistido | OK | `DRAW_CARD` respondio 200, `newStateVersion: 4`; snapshot posterior por HTTP devuelve version 4. |
| `STATE_SYNC` post accion automatico | Parcial | Se capturo `STATE_SYNC` privado version 4, pero solo en uno de los clientes de la corrida; revisar el `STOMP_ERROR` intermitente. |
| Conflicto `409` y refresh/sync | Parcial | Backend devolvio 409 con `CONCURRENT_GAME_STATE` al mandar `CHOOSE_INITIAL_POKEMON` con `expectedStateVersion: 1` cuando el estado persistido era 2. No se pudo confirmar mensaje visible via UI porque el tablero aun no expone controles de accion y Angular debug no expone `window.ng` para invocar el facade desde consola. |

### Registro 2026-06-06

| Punto | Estado | Observacion |
|---|---|---|
| Entorno local Backend | OK | `GET /api/health` respondio `UP` en `localhost:8080`. |
| Entorno local Front | OK | Front respondio en `localhost:4200`. |
| Flujo real Usuario A / Usuario B | OK tecnico | Se creo/uso Usuario B de prueba verificado por admin, ambos usuarios tuvieron mazo activo/valido y matchearon en el mismo `gameId`. |
| Accion real por API | OK | `START_GAME`, `CHOOSE_INITIAL_POKEMON` para ambos y `DRAW_CARD` fueron aceptadas con incremento de version. |
| Conflicto `409` real | OK Backend / parcial Front | Backend devolvio 409 con version vieja; no se pudo disparar el handler del facade desde UI por falta de controles visuales de accion. |
| Referencia tecnica de validacion | Documentado | Quedan arriba endpoints, metodos del facade y regla de no llamar directo al backend desde componentes visuales. |

## Criterio de cierre

La validacion manual queda aprobada cuando:

- Ambos usuarios llegan a la misma partida.
- Ambos reciben snapshot valido.
- WebSocket conecta sin errores.
- Setup inicial se completa sin payload mismatch.
- Al menos una accion jugable cambia `stateVersion`.
- Ambos tableros terminan sincronizados tras la accion.
- El comportamiento de `STATE_SYNC` post accion automatico queda registrado.

