# Documentacion Backend

Esta carpeta concentra la documentacion tecnica vigente del backend del TPI Pokemon TCG.

El backend es la fuente de verdad del sistema: valida usuarios, cartas, mazos, matchmaking, acciones de juego, persistencia de estado, logs, snapshots y eventos realtime.

## Indice

1. [Backend overview](./backend-overview.md)
2. [Arquitectura backend](./arquitectura-backend.md)
3. [Modelo de dominio](./modelo-de-dominio.md)
4. [Persistencia y base de datos](./persistencia-y-base-de-datos.md)
5. [Endpoints y WebSockets](./endpoints-y-websockets.md)
6. [Motor de juego](./motor-de-juego.md)
7. [Arquitectura detallada del motor](./motor-arquitectura-detallada.md)
8. [Testing backend](./testing-backend.md)
9. [Pendientes backend](./pendientes-backend.md)

## Estado resumido

Implementado:

- Auth, usuarios, perfil, verificacion y recuperacion de contrasena.
- Cartas XY1, importacion, consulta y detalle.
- Hasta 3 mazos por usuario, validacion, activacion de 1 mazo para jugar y randomizacion.
- Matchmaking REST con bootstrap de partida.
- WebSocket STOMP backend y `STATE_SYNC`.
- Persistencia de partidas, participantes, cartas, snapshots, logs y eventos.
- Motor de acciones con setup, robo, banca, energia, evolucion, trainers parciales, retirada, ataque, KO, premios y promocion.

Parcialmente implementado:

- Cobertura completa de todos los efectos de cartas XY1.
- Validacion end-to-end de partida completa desde UI.
- Habilidades (`USE_ABILITY`) como accion real.

Pendiente o no confirmado:

- Decision final de runtime PostgreSQL/H2 para entrega.
- Cobertura real actual.
- Validacion manual completa de WebSocket en ambos clientes.

## Archivo historico

Prompts, handoffs por persona, plantillas y documentos internos antiguos fueron movidos a `../../documentation-archive`.
