# Testing frontend

## Stack

El frontend usa:

- Jasmine.
- Karma.
- Angular testing utilities.
- HttpClient testing para servicios HTTP.

Configuracion:

- `FE/angular.json`.
- `FE/tsconfig.spec.json`.
- Dependencias en `FE/package.json`.

## Comando

Desde `FE/`:

```bash
npm test
```

## Tests detectados

Se detectaron tests para:

- Root app.
- Guards.
- Interceptor auth.
- Storage service.
- Navbar.
- Home page.
- Play room.
- Prematch facade.
- Deck API service.

## Cobertura

No confirmado:

- Cobertura real actual.
- Nivel de cobertura por feature.
- Estado de tests en ambiente local.

Motivo:

- Esta documentacion no ejecuto `npm test`.

## Huecos recomendados

- Tests de pantallas de auth completas.
- Tests de deck editor y validaciones visuales.
- Tests de Pokedex con respuestas HTTP mockeadas.
- Tests de play config y flujo de cola.
- Tests futuros de cliente STOMP.
- Tests de `GamePageComponent` cuando deje de ser placeholder.

## Criterio sugerido

Priorizar tests donde hay integracion con backend:

- Interceptor y refresh token.
- Manejo de errores HTTP.
- Guards de sesion.
- Servicios API.
- Estado de prematch y matchmaking.
