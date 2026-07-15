# Frontend overview

## Proposito

El frontend es una SPA Angular que permite al usuario interactuar con el sistema Pokemon TCG. Su objetivo es presentar pantallas, gestionar sesion, consumir APIs y preparar la experiencia de juego.

Stack actual:

- Angular 21.2.x.
- TypeScript 5.9.
- RxJS.
- Angular Router.
- Angular HttpClient.
- Jasmine/Karma para tests.
- Tailwind CSS configurado como dependencia.

## Features principales

### Auth

Estado: Implementado.

Pantallas:

- Login.
- Registro.
- Verificacion de cuenta.

Responsabilidades:

- Formularios.
- Validaciones visuales.
- Login y registro contra backend.
- Manejo de sesion.
- Recuperacion de contrasena desde login.

### Home

Estado: Implementado.

Pantalla inicial protegida para usuarios autenticados.

### Pokedex

Estado: Implementado.

Permite consultar cartas desde el backend.

### Deck

Estado: Implementado.

Permite ver y editar mazo con cartas obtenidas del backend, validacion y acciones de mazo.

### Play

Estado: Parcialmente implementado.

Incluye:

- Configuracion previa a partida.
- Revision de mazo activo.
- Entrada a cola.
- Sala de espera.

Pendiente:

- Apertura automatica de partida por evento realtime.

### Game

Estado: Pendiente visual.

Existe ruta `/game/:gameId`, pero la pantalla muestra que el motor visual esta pendiente.

### Profile

Estado: Parcialmente implementado.

Incluye resumen y edicion de perfil. El historial aparece como placeholder.

## Relacion con backend

Los servicios HTTP estan en:

```text
FE/src/app/infrastructure/api
```

Los modelos compartidos estan en:

```text
FE/src/app/core/models
```

La sesion se guarda mediante:

```text
FE/src/app/core/storage/storage.service.ts
```

El interceptor esta en:

```text
FE/src/app/core/interceptors/auth/auth.interceptor.ts
```
