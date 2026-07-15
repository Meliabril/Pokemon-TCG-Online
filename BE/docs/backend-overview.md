# Backend overview

## Proposito

El backend implementa la API y la logica de negocio del juego Pokemon TCG. Su responsabilidad principal es ser la fuente de verdad para usuarios, cartas, mazos, matchmaking y partidas.

Stack actual:

- Java 21.
- Spring Boot 3.3.0.
- Spring MVC.
- Spring Data JPA.
- Spring Security.
- Spring WebSocket/STOMP.
- Springdoc OpenAPI.
- JUnit 5, Mockito y JaCoCo.
- H2 en memoria como base activa.

## Modulos principales

### Auth y usuarios

Estado: Implementado.

Responsabilidades:

- Registro.
- Login.
- Refresh token.
- Logout.
- Usuario actual.
- Verificacion de cuenta.
- Recuperacion de contrasena.
- Perfil de usuario.
- Administracion de usuarios.

Archivos representativos:

- `controllers/auth/AuthController.java`.
- `controllers/auth/PasswordRecoveryController.java`.
- `controllers/user/UserController.java`.
- `controllers/user/AdminUserController.java`.
- `services/auth`.
- `services/user`.

### Cartas

Estado: Implementado.

Responsabilidades:

- Importar cartas del set `xy1`.
- Validar cantidad esperada de 146 cartas.
- Persistir ataques, costos, debilidades y resistencias.
- Consultar cartas por listado, detalle y busqueda.

Archivos representativos:

- `controllers/card/AdminCardController.java`.
- `controllers/card/CardController.java`.
- `services/card`.
- `entities/Card.java`, `Attack.java`, `AttackCost.java`.

### Mazos

Estado: Implementado.

El usuario puede tener hasta 3 mazos disponibles. Para jugar, elige o activa 1 mazo valido, que sera el mazo usado por matchmaking y por la partida.

Responsabilidades:

- Crear, reemplazar y administrar mazos.
- Agregar/quitar cartas.
- Validar mazo.
- Activar 1 mazo valido para jugar.
- Randomizar mazo.

### Matchmaking

Estado: Implementado en backend.

Responsabilidades:

- Cola de espera por usuario autenticado.
- Validacion de usuario activo y mazo valido.
- Emparejamiento FIFO basico.
- Creacion bootstrap de partida.
- Notificacion privada de match.

### Partidas y motor

Estado: Parcialmente implementado.

Responsabilidades:

- Consultar partida.
- Pausar y reanudar.
- Ejecutar acciones.
- Validar reglas.
- Generar estado visible.
- Guardar snapshots y logs.
- Publicar eventos.

La base existe, pero no todas las acciones declaradas tienen handler completo.

### WebSocket

Estado: Implementado en backend.

Responsabilidades:

- Exponer `/ws`.
- Autenticar STOMP `CONNECT` con JWT.
- Publicar eventos publicos y privados.
- Resolver `STATE_SYNC`.

## Relacion con frontend

El frontend consume el backend por servicios HTTP. Los contratos estan reflejados en DTOs Java y en interfaces TypeScript del frontend. Para realtime, el backend esta preparado, pero el cliente STOMP Angular todavia esta pendiente.
