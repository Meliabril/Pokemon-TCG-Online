# Guia de prueba manual para dos jugadores

Esta guia permite probar el flujo actual de una partida real entre dos personas. El objetivo es verificar estado de partida, snapshots, acciones y sincronizacion visual.

## Precondiciones

- Backend corriendo en `http://localhost:8080`.
- Frontend corriendo en `http://localhost:4200`.
- Base de datos inicializada.
- Cartas XY1 cargadas.
- Jugador 1 registrado, verificado y con sesion activa.
- Jugador 2 registrado, verificado y con sesion activa.
- Ambos jugadores tienen un mazo activo y valido.
- Usar dos navegadores distintos o una ventana normal y otra privada.

## Endpoints utiles

- Login: `POST /api/auth/login`.
- Entrar a matchmaking: `POST /api/matchmaking/queue`.
- Estado de cola: `GET /api/matchmaking/queue/me`.
- Salir de cola: `DELETE /api/matchmaking/queue`.
- Detalle de partida: `GET /api/games/{gameId}`.
- Snapshot actual: `GET /api/games/{gameId}/snapshot/latest`.
- Accion de juego: `POST /api/games/{gameId}/actions`.
- WebSocket: `ws://localhost:8080/ws`.

Canales esperados:

- Matchmaking privado: `/user/queue/matchmaking`.
- Eventos publicos de partida: `/topic/games/{gameId}`.
- Eventos privados de partida: `/user/queue/games/{gameId}`.
- Pedido de sync: `/app/games/{gameId}/state-sync`.

## Prueba desde UI

### 1. Jugador 1 busca partida

1. Iniciar sesion como Jugador 1.
2. Confirmar que tiene mazo activo valido.
3. Entrar a buscar partida.

Resultado esperado:

- El frontend llama a `POST /api/matchmaking/queue`.
- Jugador 1 queda en espera si no hay rival.
- No debe navegar a una partida sin `gameId`.

### 2. Jugador 2 busca partida

1. Iniciar sesion como Jugador 2 en otro navegador.
2. Confirmar que tiene mazo activo valido.
3. Entrar a buscar partida.

Resultado esperado:

- El backend empareja a ambos jugadores.
- Ambos reciben el mismo `gameId`.
- Ambos navegan a `/games/{gameId}`.

### 3. Revisar snapshot inicial

1. Abrir DevTools de ambos navegadores.
2. Revisar la llamada a `GET /api/games/{gameId}/snapshot/latest`.
3. Confirmar `gameId`, `status`, `stateVersion`, `players`, `board`, `actions` y `resolution`.

Resultado esperado:

- No hay estado hardcodeado.
- Mano rival, mazo y premios rivales no exponen informacion privada.
- `availableActions` refleja el estado actual.

### 4. Ambos eligen Pokemon inicial

1. Jugador 1 selecciona Pokemon activo.
2. Jugador 1 selecciona Pokemon de banca opcionales.
3. Jugador 1 confirma setup.
4. Jugador 2 repite el flujo.

Resultado esperado:

- Se envia `CHOOSE_INITIAL_POKEMON`.
- Cada request incluye `clientActionId` UUID puro y `expectedStateVersion`.
- Al completar ambos, el backend revela el tablero y pasa a `ACTIVE`.

### 5. Probar robar

1. Identificar el jugador activo.
2. Si el turno esta en `DRAW`, ejecutar la accion de robo desde UI.

Resultado esperado:

- Se envia `DRAW_CARD`.
- `stateVersion` aumenta.
- El jugador activo suma una carta en mano.
- La fase cambia a `MAIN`.
- Ambos clientes reciben snapshot actualizado.

### 6. Probar bajar Pokemon a banca

1. En `MAIN`, seleccionar un Pokemon basico de la mano.
2. Elegir accion de banca.

Resultado esperado:

- Se envia `PLAY_BASIC_POKEMON`.
- La carta desaparece de mano propia.
- Aparece un Pokemon en banca.
- El rival ve contador/estado publico actualizado.

Observacion: si la banca aparece recien tras `END_TURN`, registrar bug de sincronizacion.

### 7. Probar unir energia

1. En `MAIN`, seleccionar una energia de la mano.
2. Seleccionar un Pokemon propio activo o en banca.
3. Ejecutar accion.

Resultado esperado:

- Se envia `ATTACH_ENERGY`.
- La energia queda adjunta al Pokemon.
- `energyAttachedThisTurn` bloquea otra energia en el mismo turno.

### 8. Probar retirada

1. Tener al menos un Pokemon en banca.
2. Seleccionar retirada del activo.
3. Elegir el Pokemon de banca que pasa a activo.

Resultado esperado:

- Se envia `RETREAT`.
- El backend descarta energias necesarias.
- Activo y banca intercambian posiciones.
- No debe requerir terminar turno para verse actualizado.

Observacion: si el cambio se ve recien al terminar turno, registrar bug.

### 9. Probar ataque

1. Asegurar que el Pokemon activo tiene energia suficiente.
2. Seleccionar ataque disponible.
3. Ejecutar `DECLARE_ATTACK`.

Resultado esperado:

- El backend calcula dano.
- Se emiten eventos de ataque/dano.
- Si hay KO, se toman premios.
- Si el activo KO tiene banca, aparece promocion pendiente.
- Si no hay banca o se toma ultimo premio, la partida termina.

### 10. Revisar snapshots

Despues de cada accion:

1. Revisar `stateVersion`.
2. Revisar `availableActions`.
3. Revisar `turn.currentPhase`.
4. Revisar `board.view.players`.
5. Comparar snapshot de Jugador 1 y Jugador 2.

Resultado esperado:

- Ambos snapshots tienen la misma version logica.
- Cada jugador ve su informacion privada y oculta la del rival.
- No quedan acciones obsoletas habilitadas.

## Prueba por API si la UI no permite una accion

Usar solo acciones presentes en `availableActions`.

```powershell
$token = '<accessToken>'
$gameId = '<gameId>'
$snapshot = Invoke-RestMethod -Headers @{ Authorization = "Bearer $token" } -Uri "http://localhost:8080/api/games/$gameId/snapshot/latest"

$request = @{
  gameId = $gameId
  clientActionId = [guid]::NewGuid().ToString()
  actionType = 'DRAW_CARD'
  expectedStateVersion = $snapshot.stateVersion
  payload = @{}
}

Invoke-RestMethod `
  -Method Post `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType 'application/json' `
  -Uri "http://localhost:8080/api/games/$gameId/actions" `
  -Body ($request | ConvertTo-Json -Depth 10)
```

## Criterio de aceptacion manual

La prueba se considera aprobada cuando:

- Ambos jugadores llegan a la misma partida.
- Ambos completan setup inicial.
- Se observa `ACTIVE`.
- Se ejecuta al menos una accion de robo.
- Se baja al menos un Pokemon a banca.
- Se une al menos una energia.
- Se prueba retirada.
- Se prueba ataque.
- Se revisan snapshots en ambos clientes.
- Los bugs conocidos quedan registrados con accion, version, jugador y evidencia.
