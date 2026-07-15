# Documentacion Frontend

Esta carpeta documenta el frontend Angular del TPI Pokemon TCG.

El frontend implementa la experiencia de usuario, consume el backend por HTTP/STOMP y no debe resolver reglas finales del juego. Las reglas pertenecen al backend.

## Indice

1. [Frontend overview](./frontend-overview.md)
2. [Arquitectura frontend](./arquitectura-frontend.md)
3. [Estructura frontend](./estructura-frontend.md)
4. [Componentes principales](./componentes-principales.md)
5. [Rutas y navegacion](./rutas-y-navegacion.md)
6. [Integracion con backend](./integracion-con-backend.md)
7. [Estado UI y pendientes](./estado-ui-y-pendientes.md)
8. [Validacion manual de partida](./game-manual-validation.md)
9. [Payloads de acciones](./game-action-payloads-validation.md)
10. [Bloqueantes/observaciones de integracion](./game-integration-blockers.md)
11. [Testing frontend](./testing-frontend.md)

## Estado resumido

Implementado:

- Angular 21.
- Rutas publicas y protegidas.
- Guards de autenticacion.
- Interceptor JWT con refresh.
- Servicios HTTP por dominio.
- Cliente STOMP base.
- Servicios realtime para matchmaking y partida.
- Pantallas de auth, home, pokedex, mazo, play config, play room, perfil y partida.
- Tablero, mapper de snapshot y `GameFacadeService`.

Parcialmente implementado:

- Flujo visual completo de partida.
- Sincronizacion estable de acciones en ambos clientes.
- Visualizacion inmediata de algunas acciones de tablero.

Pendiente:

- Validacion manual completa de partida desde UI.
- Correccion de bugs de banca, retirada y acciones que aparecen tarde.
- Historial visual completo.
