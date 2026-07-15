# Arquitectura general

## Vision del sistema

El proyecto es un monorepo compuesto por dos aplicaciones principales:

- `BE/`: backend Java 21 con Spring Boot 3.3.0.
- `FE/`: frontend Angular 21 con TypeScript 5.9.

El backend actua como fuente de verdad. El frontend no resuelve reglas finales del juego: envia intenciones, consulta estado y muestra resultados calculados por el servidor.

```text
Usuario
  |
  v
Angular SPA
  | REST HTTP
  | WebSocket STOMP
  v
Spring Boot Backend
  | JPA repositories
  v
Base de datos local/configurada

Spring Boot tambien consume Pokemon TCG API para importar cartas XY1.
```

## Separacion Frontend y Backend

El frontend se ocupa de:

- Pantallas de autenticacion, home, pokedex, mazos, configuracion de partida, sala de espera, perfil y partida.
- Guards de rutas publicas/protegidas.
- Interceptor para adjuntar JWT y refrescar sesion.
- Servicios HTTP agrupados por dominio.
- Cliente STOMP, servicios realtime y consumo de snapshots/eventos.
- Render del tablero desde `BoardGameViewModel`.

El backend se ocupa de:

- Registro, login, refresh token, logout y usuario actual.
- Verificacion de cuenta y recuperacion de contrasena.
- Gestion de usuarios y perfil.
- Importacion y consulta de cartas XY1.
- Administracion y validacion de mazos.
- Matchmaking, creacion bootstrap de partida y notificacion.
- Estado de juego, validadores, handlers de accion, snapshots, logs y eventos.
- WebSocket STOMP con autenticacion JWT.

## Comunicacion

La comunicacion principal es REST bajo rutas `/api/**`. Las features de frontend consumen estas rutas mediante servicios en `FE/src/app/infrastructure/api`.

La comunicacion realtime usa STOMP:

- Endpoint: `/ws`.
- Prefijo de entrada: `/app`.
- Eventos publicos: `/topic/games/{gameId}`.
- Eventos privados: `/user/queue/games/{gameId}`.
- Matchmaking privado: `/user/queue/matchmaking`.
- Sync explicito: `/app/games/{gameId}/state-sync`.

En frontend existen `StompRealtimeService`, `GameRealtimeService` y `MatchmakingRealtimeService`. El estado correcto es parcial: el transporte esta implementado, pero requiere validacion manual estable en dos clientes y correccion de bugs de sincronizacion visual.

## Capas principales del backend

El backend sigue una organizacion por capas tecnicas:

- `controllers`: entrada REST y WebSocket.
- `services`: casos de uso, reglas, orquestacion y logica de negocio.
- `repositories`: acceso a datos con Spring Data JPA.
- `entities`: modelo persistente.
- `dtos`: contratos de entrada/salida.
- `mappers`: conversion entre entidades y DTOs.
- `configs`: seguridad, OpenAPI, WebSocket, beans y seeders.
- `exceptions`: errores de negocio y handler global.

## Capas principales del frontend

El frontend sigue una estructura por responsabilidades:

- `core`: constantes, guards, interceptores, modelos compartidos y storage.
- `features`: pantallas y funcionalidades por dominio.
- `infrastructure`: adaptadores HTTP/WebSocket hacia backend.
- `presentation`: layouts y navegacion global.
- `shared`: componentes reutilizables.
- `routes`: tabla principal de rutas.

## Estado arquitectonico

Implementado:

- Monorepo con separacion BE/FE.
- Backend con REST, JPA, Security, WebSocket, tests y documentacion parcial.
- Frontend con rutas, guards, services HTTP, cliente STOMP, fachada de juego, tablero y pantallas funcionales.
- Hasta 3 mazos por usuario, con 1 mazo activo/valido elegido para matchmaking y partida.

Parcialmente implementado:

- MVP jugable completo desde UI.
- Sincronizacion frontend estable de todas las acciones.
- Cobertura completa de efectos de cartas y habilidades.

Documentado pero requiere decision:

- PostgreSQL como runtime final frente a configuraciones locales H2.
