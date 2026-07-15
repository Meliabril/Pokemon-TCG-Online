# Estado del desarrollo

Esta matriz resume el estado real detectado en el repositorio despues de la auditoria documental.

## Categorias usadas

- Implementado: existe codigo funcional identificable.
- Parcialmente implementado: existe base tecnica, pero falta validacion, cobertura completa o estabilidad.
- Pendiente: no se encontro implementacion suficiente.
- Requiere validacion: el codigo o la documentacion sugieren soporte, pero falta prueba manual o evidencia reciente.

## Matriz funcional

| Area | Estado | Evidencia |
| --- | --- | --- |
| Registro, login, refresh, logout y me | Implementado | `AuthController`, services auth, pantallas Angular. |
| Verificacion de cuenta | Implementado | Endpoints y pantalla de verificacion. |
| Recuperacion de contrasena | Implementado | Controladores, DTOs y flujo frontend. |
| Perfil de usuario | Implementado | Endpoints `/api/users/me/profile`, feature `profile`. |
| Importacion cartas XY1 | Implementado | `AdminCardController`, `CardImportServiceImpl`. |
| Consulta de cartas | Implementado | `CardController`, Pokedex frontend. |
| Mazos y validacion | Implementado | `DeckController`, `DeckValidationServiceImpl`, feature `deck`. |
| Hasta 3 mazos por usuario + 1 activo para jugar | Implementado | Validacion/activacion de mazo para matchmaking. |
| Matchmaking REST | Implementado | `MatchmakingController`, `MatchmakingServiceImpl`. |
| Bootstrap de partida por match | Implementado | `MatchGameBootstrapServiceImpl`. |
| WebSocket backend | Implementado | `WebSocketConfig`, interceptores STOMP, publisher. |
| WebSocket frontend | Parcialmente implementado | `StompRealtimeService`, `GameRealtimeService`, `MatchmakingRealtimeService`; requiere prueba estable. |
| STATE_SYNC | Implementado / requiere validacion UI | Backend emite sync; frontend lo consume, pero hay reportes de intermitencia. |
| Motor de acciones | Parcialmente implementado | Handlers principales implementados; habilidades y algunos efectos quedan pendientes. |
| UI de tablero | Parcialmente implementado | Componentes `features/game`, mapper y `GameFacadeService`; hay bugs visuales. |
| Historial visual | Pendiente | Backend registra historial; UI completa no cerrada. |
| PostgreSQL runtime | Requiere decision | El stack academico lo indica, pero configuraciones locales usan H2. |
| Cobertura real | Requiere validacion | No se ejecutaron tests en esta fase documental. |

## MVP jugable

Estado: parcial.

El backend soporta el flujo principal de partida. El frontend ya tiene las piezas para jugar, pero el MVP no queda cerrado hasta validar una partida completa entre dos personas y corregir los bugs de actualizacion visual tardia.

Ver:

- [Estado actual del juego](../game/estado-actual-del-juego.md)
- [Bugs conocidos y pendientes MVP](../game/bugs-conocidos-y-pendientes.md)
- [Guia de prueba manual](../game/guia-prueba-manual-dos-jugadores.md)

## Riesgos principales

- Sincronizacion visual tardia en banca, retirada y acciones que aparecen despues de `END_TURN`.
- Dependencia de `STATE_SYNC` privado para que ambos clientes queden consistentes.
- Contradiccion pendiente entre PostgreSQL academico y runtime local H2.
- Habilidades y algunos efectos de cartas no forman parte del MVP validado.
