# Rutas y navegacion

## Archivo principal

Las rutas se definen en:

```text
FE/src/app/routes/app.routes.ts
```

Las constantes de rutas estan en:

```text
FE/src/app/core/constants/routing/routes.constants.ts
```

## Rutas publicas

```text
/auth/login
/auth/register
/auth/verify-account
```

`login` y `register` usan `publicAuthGuard`, para evitar que un usuario ya autenticado vuelva a pantallas publicas de autenticacion.

## Rutas protegidas

```text
/home
/pokedex
/deck
/play/config
/play/room
/perfil
/game/:gameId
```

Estas rutas usan `authGuard`.

## Redirecciones

```text
/ -> /home
/matchmaking -> /play/config
/** -> /home
```

## Guards

### authGuard

Estado: Implementado.

Responsabilidad:

- Permitir acceso solo si existe sesion autenticada.
- Redirigir cuando el usuario no esta autenticado.

### publicAuthGuard

Estado: Implementado.

Responsabilidad:

- Evitar que un usuario autenticado use rutas publicas como login o registro.

## Navegacion funcional actual

Implementado:

- Usuario autenticado puede navegar por home, pokedex, mazo, play config, play room y perfil.
- Ruta de partida existe y recibe `gameId`.

Parcialmente implementado:

- Flujo desde match encontrado hacia partida.

Pendiente:

- Apertura automatica de `/game/:gameId` desde evento realtime.
