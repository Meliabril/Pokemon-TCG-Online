# Flujo actual de partida

Este flujo describe como avanza una partida en el estado actual del proyecto. El backend es la fuente de verdad: el frontend envia intenciones y renderiza snapshots/eventos confirmados.

## 1. Armado y seleccion de mazo

1. El usuario crea o randomiza un mazo.
2. El backend valida que el mazo tenga exactamente 60 cartas.
3. Todas las cartas deben pertenecer al set `xy1`.
4. Se permite un maximo de 4 copias por nombre, excepto energia basica.
5. El mazo debe incluir al menos 1 Pokemon basico.
6. El usuario activa un mazo valido para poder buscar partida.

## 2. Matchmaking

1. Jugador 1 llama `POST /api/matchmaking/queue`.
2. Si no hay rival, queda esperando en cola.
3. Jugador 2 llama `POST /api/matchmaking/queue`.
4. El backend valida usuarios y mazos activos.
5. El backend crea la partida con dos participantes.
6. Se emite notificacion privada de match con `gameId`.
7. Ambos clientes navegan a `/games/{gameId}`.

## 3. Inicio y mulligan automatico

1. La partida arranca en `WAITING`.
2. `START_GAME` prepara el tablero inicial.
3. El backend expande mazos, baraja y roba 7 cartas por jugador.
4. Si una mano no tiene Pokemon basico, el backend ejecuta mulligan automatico:
   - revela la mano al rival por evento;
   - devuelve la mano al mazo;
   - baraja;
   - roba 7 cartas;
   - incrementa el contador de mulligan.
5. El rival recibe automaticamente cartas extra segun los mulligans del oponente.
6. El backend coloca 6 premios boca abajo por jugador.
7. La partida pasa a `SETUP`.

## 4. Eleccion de Pokemon inicial y banca

1. Cada jugador recibe `availableActions` con `CHOOSE_INITIAL_POKEMON`.
2. Cada jugador elige 1 Pokemon basico de la mano como activo.
3. Opcionalmente elige hasta 5 Pokemon basicos adicionales para la banca.
4. La accion envia:

```json
{
  "actionType": "CHOOSE_INITIAL_POKEMON",
  "payload": {
    "activeCardInstanceId": "<uuid>",
    "benchCardInstanceIds": ["<uuid>"]
  }
}
```

5. Cuando ambos jugadores completan setup, el backend revela ambos campos.
6. El backend crea `PokemonInPlay`, stacks base de evolucion y actualiza zonas `ACTIVE`/`BENCH`.

## 5. Turno random

1. Al completar setup, el backend elige aleatoriamente que jugador empieza.
2. La partida pasa a `ACTIVE`.
3. El turno inicial queda en fase `DRAW`.
4. El jugador que va primero no puede atacar en su primer turno.

## 6. Robo

1. En fase `DRAW`, `availableActions` expone `DRAW_CARD`.
2. El jugador activo debe robar una carta.
3. Si el mazo esta vacio al robar, el jugador pierde la partida.
4. Despues de robar, el backend pasa a fase `MAIN`.

## 7. Fase principal

Durante `MAIN`, el backend puede exponer acciones como:

- `PLAY_BASIC_POKEMON`.
- `ATTACH_ENERGY`.
- `EVOLVE_POKEMON`.
- `PLAY_TRAINER`.
- `RETREAT`.
- `DECLARE_ATTACK`.
- `END_TURN`.

La disponibilidad real depende de `availableActions`, fase, turno, cartas en mano, energia disponible, condiciones especiales y reglas de una vez por turno.

## 8. Bajar Pokemon a banca

1. El jugador selecciona un Pokemon basico de la mano.
2. Ejecuta `PLAY_BASIC_POKEMON`.
3. El backend valida propiedad, zona y capacidad de banca.
4. La carta pasa de `HAND` a `BENCH`.
5. Se crea el Pokemon en juego y su stack base.

Bug conocido: en algunas corridas la banca se actualiza tarde en UI. Ver [bugs conocidos](./bugs-conocidos-y-pendientes.md).

## 9. Unir energia

1. El jugador selecciona una energia de la mano y un Pokemon propio.
2. Ejecuta `ATTACH_ENERGY`.
3. El backend valida que no se haya unido energia en el turno.
4. La carta pasa a `ATTACHED`.
5. Se marca `energyAttachedThisTurn`.

## 10. Retirada

1. El jugador elige un Pokemon de banca como nuevo activo.
2. Ejecuta `RETREAT`.
3. El backend valida que el activo pueda retirarse y que no haya retirada previa en el turno.
4. El backend paga el costo descartando energias adjuntas.
5. El activo pasa a banca y el Pokemon elegido pasa a activo.
6. Se limpian condiciones especiales del Pokemon retirado.

Bug conocido: la UI puede mostrar el cambio recien al terminar turno.

## 11. Ataque

1. El jugador elige un ataque disponible del Pokemon activo.
2. Ejecuta `DECLARE_ATTACK`.
3. El backend valida energia, fase, turno y condiciones especiales.
4. Si corresponde, resuelve confusion antes del ataque.
5. Calcula dano, debilidad, resistencia y efectos soportados.
6. Aplica dano y eventos.
7. Evalua KO, premios, promocion pendiente o fin de partida.

## 12. KO, premios y promocion

1. Un Pokemon queda KO si sus contadores de dano cubren o superan su HP.
2. El backend descarta el Pokemon, cartas adjuntas y stack de evolucion.
3. El rival toma premios automaticamente.
4. Si el jugador afectado no tiene banca, pierde la partida.
5. Si tiene banca, el backend crea resolucion `PROMOTION_REQUIRED`.
6. Mientras hay promocion pendiente, solo se expone `PROMOTE_BENCH_POKEMON`.
7. El jugador afectado elige Pokemon de banca y la partida continua.

## 13. Fin de turno y fin de partida

1. `END_TURN` cambia de fase o avanza al siguiente jugador segun estado.
2. Al cambiar de turno se reinician flags de energia, supporter y retirada.
3. La partida termina si:
   - un jugador toma su ultimo premio;
   - un jugador queda sin Pokemon activo ni banca;
   - un jugador intenta robar con mazo vacio.
4. El estado final esperado es `FINISHED`.

## 14. Sincronizacion

Despues de acciones exitosas, el backend persiste snapshot y envia `STATE_SYNC` privado a cada jugador. El frontend debe actualizar el tablero desde ese estado confirmado. Ante conflicto `409`, el frontend debe refrescar snapshot o solicitar sync y no conservar estado visual obsoleto.
