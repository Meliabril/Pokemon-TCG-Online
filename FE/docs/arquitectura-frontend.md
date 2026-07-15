# Arquitectura frontend

## Estructura principal

```text
src/app
  core
  features
  infrastructure
  presentation
  routes
  shared
```

Esta estructura coincide con la intencion documentada en `FE/ESTRUCTURA_FRONTEND.md`.

## Core

Responsabilidad:

- Elementos transversales a toda la app.
- Constantes globales.
- Guards.
- Interceptor.
- Modelos compartidos.
- Storage de sesion.

Subcarpetas relevantes:

- `core/constants/api`.
- `core/constants/routing`.
- `core/constants/storage`.
- `core/guards`.
- `core/interceptors/auth`.
- `core/models/enums`.
- `core/models/interfaces`.
- `core/storage`.

## Features

Responsabilidad:

- Funcionalidades visibles para el usuario.
- Pantallas y componentes por dominio.

Features actuales:

- `auth`.
- `deck`.
- `game`.
- `home`.
- `matchmaking`.
- `play`.
- `pokedex`.
- `profile`.

Regla:

- Si una pantalla pertenece a un dominio, vive en `features/<dominio>/pages`.
- Si un componente solo sirve a una feature, vive en `features/<dominio>/components`.

## Infrastructure

Responsabilidad:

- Adaptadores hacia servicios externos.
- En este proyecto, principalmente HTTP contra backend.

Servicios actuales:

- `auth-api.service.ts`.
- `cards-api.service.ts`.
- `deck-api.service.ts`.
- `game-api.service.ts`.
- `matchmaking-api.service.ts`.
- `health-api.service.ts`.
- `user-api.service.ts`.

## Presentation

Responsabilidad:

- Componentes de estructura visual global.
- Layouts y navegacion.

Ejemplos:

- Navbar.
- Main layout.

## Shared

Responsabilidad:

- UI reutilizable y generica.
- Componentes que no dependen de una feature puntual.

Ejemplos:

- Boton principal.
- Empty state.
- Page header.
- App shell.
- Avatar picker.

## Routes

Responsabilidad:

- Definir rutas de la aplicacion.
- Aplicar guards.
- Cargar componentes de forma lazy.

Archivo principal:

```text
FE/src/app/routes/app.routes.ts
```
