# Arquitectura backend

## Organizacion por capas

El backend usa una arquitectura por capas tecnicas dentro del paquete base:

```text
ar.edu.utn.frc.tup.piii
  configs
  controllers
  dtos
  entities
  exceptions
  mappers
  repositories
  security
  services
  util
```

## Controllers

Responsabilidad:

- Exponer endpoints REST.
- Recibir y validar request bodies.
- Resolver usuario autenticado cuando corresponde.
- Delegar a servicios.

Controllers principales:

- `AuthController`.
- `PasswordRecoveryController`.
- `UserController`.
- `AdminUserController`.
- `CardController`.
- `AdminCardController`.
- `DeckController`.
- `MatchmakingController`.
- `GameController`.
- `GameHistoryController`.
- `PingController`.
- `GameWebSocketController`.

## Services

Responsabilidad:

- Implementar casos de uso.
- Orquestar repositorios, mappers y validadores.
- Aplicar reglas de negocio.
- Definir limites transaccionales.

Patron usado:

- Interfaz en `services/<modulo>`.
- Implementacion en `services/<modulo>/impl`.

Ejemplos:

- `AuthService` / `AuthServiceImpl`.
- `DeckService` / `DeckServiceImpl`.
- `CardService` / `CardServiceImpl`.
- `MatchmakingService` / `MatchmakingServiceImpl`.

## Repositories

Responsabilidad:

- Acceder a datos con Spring Data JPA.
- Consultar entidades por identificadores, ownership y relaciones.
- Separar la persistencia de la logica de negocio.

Ejemplos:

- `UserRepository`.
- `CardRepository`.
- `DeckRepository`.
- `GameRepository`.
- `GameParticipantRepository`.
- `GameEventRepository`.

## Entities

Responsabilidad:

- Representar el modelo persistente.
- Mapear tablas y relaciones JPA.

Grupos principales:

- Usuarios y tokens.
- Cartas y detalles.
- Mazos.
- Partidas.
- Estado de cartas en partida.
- Snapshots, logs y eventos.

## DTOs

Responsabilidad:

- Definir contratos de entrada y salida.
- Evitar exponer entidades JPA directamente.
- Separar payloads por dominio.

Directorios:

- `dtos/auth`.
- `dtos/card`.
- `dtos/deck`.
- `dtos/game`.
- `dtos/user`.
- `dtos/websocket`.
- `dtos/common`.
- `dtos/enums`.

## Mappers

Responsabilidad:

- Convertir entidades a DTOs.
- Ordenar estructuras de salida cuando hace falta.
- Mantener controllers y services libres de armado manual de respuestas.

Ejemplos:

- `CardMapper`.
- `DeckMapper`.
- `GameDetailMapper`.
- `GameEventMapper`.
- `UserMapper`.

## Config y security

Configuraciones principales:

- `SecurityConfig`: reglas HTTP, filtros y permisos.
- `SecurityBeansConfig`: beans de seguridad como password encoder.
- `WebSocketConfig`: endpoint STOMP y broker.
- `SpringDocConfig`: OpenAPI.
- `MappersConfig`: ModelMapper.
- `AdminSeeder` y `Xy1CardSeeder`: seeders.

Seguridad:

- `JwtAuthenticationFilter` protege REST.
- `StompJwtChannelInterceptor` protege STOMP.
- `StompPrincipal` representa el usuario en WebSocket.

## Exceptions

Responsabilidad:

- Modelar errores de negocio.
- Traducir errores a una respuesta comun.

El `GlobalExceptionHandler` centraliza errores de validacion, negocio, recursos inexistentes y errores no controlados.
