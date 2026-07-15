# Flujo funcional del juego

Este documento resume el flujo funcional de uso del sistema y enlaza el detalle actual de partida.

Para el flujo paso a paso de la partida, ver [Flujo actual de partida](../game/flujo-de-partida.md).

## 1. Registro y acceso

Estado: Implementado.

1. El usuario se registra.
2. El backend guarda usuario y credenciales hasheadas.
3. El sistema puede requerir verificacion de cuenta por codigo.
4. El usuario inicia sesion.
5. El frontend guarda la sesion y adjunta el access token mediante interceptor.
6. El refresh token permite renovar sesion.

## 2. Importacion y consulta de cartas

Estado: Implementado.

1. Un admin ejecuta `POST /api/admin/cards/import/xy1`.
2. El backend consulta Pokemon TCG API.
3. Se valida que el set sea `xy1`.
4. Se persisten cartas, ataques, costos, debilidades y resistencias.
5. El frontend lista y busca cartas desde Pokedex y editor de mazo.

## 3. Construccion de mazo

Estado: Implementado.

Reglas implementadas:

- Exactamente 60 cartas.
- Todas las cartas deben pertenecer a `xy1`.
- Maximo 4 copias por nombre, excepto energia basica.
- Al menos 1 Pokemon basico.
- El mazo activo debe ser valido para entrar a matchmaking.

## 4. Matchmaking y preparacion

Estado: Implementado con validacion UI pendiente.

- Frontend consulta mazo activo y validacion.
- Backend permite entrar y salir de cola.
- Backend valida usuario activo y mazo valido.
- Al encontrar rival, crea una partida bootstrap con participantes.
- Backend publica notificacion de match.
- Frontend tiene servicios realtime para consumir notificaciones.

## 5. Inicio de partida y motor

Estado: Parcialmente implementado.

Implementado:

- Entidades de partida, participantes, cartas en partida, snapshots, logs y eventos.
- Validadores de reglas.
- `GameActionExecutorImpl` con handlers para acciones principales.
- Setup, mulligan automatico, turno random, robo, banca, energia, retirada, ataque, KO, premios y promocion.

Pendiente o requiere validacion:

- Partida completa hasta fin desde UI.
- Estabilidad visual de todas las acciones.
- Habilidades y cobertura completa de efectos.

## 6. Sincronizacion y reconexion

Estado: Parcialmente implementado.

Implementado:

- WebSocket STOMP en `/ws`.
- Autenticacion JWT en `CONNECT`.
- `STATE_SYNC` via `/app/games/{gameId}/state-sync`.
- Publicacion publica y privada de eventos.
- Cliente STOMP y servicios realtime frontend.

Requiere validacion:

- Recepcion estable de `STATE_SYNC` en ambos jugadores tras cada accion.
- Reconexion limpia sin dejar tablero obsoleto.

## 7. Historial

Estado: Parcialmente implementado.

- Backend registra action logs, eventos y snapshots.
- Existen endpoints de historial y ultimo snapshot.
- Falta cerrar experiencia visual de historial en frontend.
