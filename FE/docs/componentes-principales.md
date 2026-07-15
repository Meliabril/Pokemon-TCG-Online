# Componentes principales

## Auth

Estado: Implementado.

Componentes/paginas:

- `LoginPageComponent`.
- `RegisterPageComponent`.
- `VerifyAccountPageComponent`.
- `AuthShellComponent`.

Responsabilidad:

- Autenticacion.
- Registro.
- Verificacion.
- Recuperacion de contrasena.
- Estados de carga y error.

## Navegacion y layout

Estado: Implementado.

Componentes:

- `NavbarComponent`.
- `MainLayoutComponent`.
- `AppShellComponent`.
- `PageHeaderComponent`.

Responsabilidad:

- Estructura global.
- Navegacion principal.
- Encabezados reutilizables.

## Home

Estado: Implementado.

Pagina:

- `HomePageComponent`.

Responsabilidad:

- Entrada principal para usuario autenticado.

## Pokedex

Estado: Implementado.

Pagina:

- `PokedexPage`.

Responsabilidad:

- Listar y buscar cartas.
- Consumir datos de cartas desde backend.

## Deck

Estado: Implementado.

Pagina y componentes:

- `DeckPageComponent`.
- `DeckEditorComponent`.
- `DeckOverviewComponent`.

Responsabilidad:

- Visualizar mazo.
- Editar cartas.
- Mostrar validacion.
- Activar o randomizar mazo.

## Play

Estado: Parcialmente implementado.

Paginas y servicios:

- `PlayConfigPageComponent`.
- `PlayRoomPageComponent`.
- `PrematchFacadeService`.
- `MatchmakingRealtimeService`.

Responsabilidad:

- Preparar partida.
- Validar condiciones previas.
- Entrar y salir de cola.
- Mostrar estado de busqueda.

Pendiente:

- Realtime STOMP real.
- Navegacion automatica a partida.

## Game

Estado: Pendiente visual.

Pagina:

- `GamePageComponent`.

Responsabilidad actual:

- Recibir `gameId`.
- Mostrar placeholder de motor visual.

Pendiente:

- Tablero.
- Acciones de juego.
- Estado de partida.
- Eventos realtime.

## Profile

Estado: Parcialmente implementado.

Componentes:

- `ProfilePageComponent`.
- `ProfileSummaryCardComponent`.
- `ProfileHistoryPlaceholderComponent`.

Responsabilidad:

- Mostrar perfil.
- Indicar estado de cuenta.
- Preparar espacio para historial.
