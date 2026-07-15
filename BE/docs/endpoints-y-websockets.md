# Endpoints y WebSockets

## Health

```http
GET /ping
GET /api/health
```

Estado: Implementado.

## Auth

```http
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/verify-account
POST /api/auth/resend-verification-code
POST /api/auth/logout
GET  /api/auth/me
POST /api/auth/password/forgot
POST /api/auth/forgot-password
POST /api/auth/password/reset
POST /api/auth/reset-password
```

Estado: Implementado.

## Usuarios

```http
POST  /api/users
POST  /api/users/create
GET   /api/users/me/profile
PATCH /api/users/me/profile
PATCH /api/users/{id}
PATCH /api/users/update/{id}
PATCH /api/users/delete/{id}
```

Admin:

```http
POST  /api/admin/users
POST  /api/admin/users/create
PATCH /api/admin/users/{id}/status
GET   /api/admin/users/{id}
GET   /api/admin/users
GET   /api/admin/users/getAll
```

Estado: Implementado.

## Cartas

```http
POST /api/admin/cards/import/xy1
GET  /api/cards
GET  /api/cards/{id}
GET  /api/cards/search?name=...&setCode=xy1
GET  /api/cards/import-status/xy1
```

Estado: Implementado.

## Mazos

```http
GET    /api/me/decks
POST   /api/me/decks
POST   /api/me/decks/randomize
GET    /api/me/decks/{deckId}
PUT    /api/me/decks/{deckId}
POST   /api/me/decks/{deckId}/cards
DELETE /api/me/decks/{deckId}/cards/{cardId}
GET    /api/me/decks/{deckId}/validation
PUT    /api/me/decks/{deckId}/activate
PUT    /api/me/decks/{deckId}/randomize
DELETE /api/me/decks/{deckId}
```

Estado: Implementado.

## Matchmaking

```http
POST   /api/matchmaking/queue
DELETE /api/matchmaking/queue
GET    /api/matchmaking/queue/me
```

Estado: Implementado.

## Partidas

```http
GET  /api/games/{gameId}
POST /api/games/{gameId}/pause
POST /api/games/{gameId}/resume
POST /api/games/{gameId}/actions
GET  /api/games/{gameId}/history
GET  /api/games/{gameId}/snapshot/latest
```

Estado: Parcialmente implementado para MVP jugable. El backend soporta acciones principales, snapshots e historial. La validacion final depende de prueba manual completa desde UI.

## WebSocket STOMP

Endpoint:

```text
/ws
```

Header requerido en STOMP `CONNECT`:

```text
Authorization: Bearer <accessToken>
```

Destinos:

```text
/user/queue/matchmaking
/user/queue/games/{gameId}
/topic/games/{gameId}
/topic/games/{gameId}/visual-events
/app/games/{gameId}/state-sync
```

Estado:

- Backend: Implementado.
- Frontend: Parcialmente implementado; existen servicios STOMP y consumo de eventos, pero falta validacion estable de dos clientes.

## Acciones principales por payload

Ver [Payloads de acciones frontend](../../FE/docs/game-action-payloads-validation.md).
