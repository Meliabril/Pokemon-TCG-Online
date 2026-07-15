# Frontend Pokemon TCG

Aplicacion Angular 21 del Trabajo Practico Integrador Pokemon TCG.

La documentacion vigente del frontend esta en [FE/docs](./docs/README.md).

## Stack

- Angular 21.
- TypeScript 5.9.
- Servicios HTTP para backend REST.
- STOMP para realtime de matchmaking y partida.

## Comandos utiles

```bash
npm install
npm start
npm run build
npm test
```

## Estructura

La estructura detallada esta documentada en [estructura frontend](./docs/estructura-frontend.md).

Resumen:

- `core`: constantes, guards, interceptores, modelos compartidos, storage y servicios transversales.
- `features`: pantallas y funcionalidades por dominio.
- `infrastructure`: adaptadores HTTP/WebSocket hacia backend.
- `presentation`: layouts y navegacion global.
- `shared`: componentes reutilizables.
- `routes`: configuracion principal de rutas.

## Estado de partida

El tablero y los servicios realtime existen, pero el MVP jugable requiere validacion manual completa y correccion de bugs de sincronizacion visual. Ver [estado UI y pendientes](./docs/estado-ui-y-pendientes.md).
