# Estructura del Frontend

Este documento explica como esta ordenado el frontend y que criterio usar cuando haya que crear nuevos archivos. La idea es que el proyecto sea facil de leer, mantener y escalar sin que las carpetas se vuelvan una bolsa de cosas mezcladas.

## Idea general

El frontend esta dividido por responsabilidades:

- `core`: piezas globales de la aplicacion.
- `features`: pantallas y funcionalidades concretas.
- `infrastructure`: comunicacion con servicios externos, principalmente el backend.
- `presentation`: componentes y layouts de presentacion de nivel aplicacion.
- `shared`: elementos reutilizables y genericos.
- `routes`: configuracion principal de rutas.

La regla principal es simple: si algo pertenece a un dominio concreto, se guarda dentro de una subcarpeta de ese dominio. Por ejemplo, todo lo de `game` va junto, todo lo de `auth` va junto, todo lo de `deck` va junto.

## `core`

`core` contiene archivos que son transversales a toda la app. No deberia tener componentes de una pantalla puntual.

### `core/constants`

Constantes globales agrupadas por categoria:

```txt
core/constants
+-- api
+-- app
+-- routing
+-- storage
```

- `api`: endpoints y paths del backend.
- `app`: configuracion general de la aplicacion.
- `routing`: nombres o paths de rutas usadas en varios lugares.
- `storage`: claves usadas en local storage, session storage o mecanismos similares.

Si aparece una constante nueva, primero hay que preguntarse a que categoria pertenece. Si no encaja en ninguna, se crea una subcarpeta nueva con un nombre claro.

### `core/models`

Modelos compartidos por el frontend, alineados con el backend cuando corresponde.

```txt
core/models
+-- enums
|   +-- card
|   +-- game
|   +-- user
+-- interfaces
    +-- auth
    +-- card
    +-- common
    +-- deck
    +-- game
    +-- matchmaking
    +-- navigation
    +-- system
    +-- user
```

#### `core/models/enums`

Enums separados por dominio:

- `card`: categorias, zonas, supertipos y tipos relacionados con cartas.
- `game`: estados, fases, acciones, eventos y condiciones de partida.
- `user`: roles y estados de usuario.

Ejemplo: si hay que agregar un enum `DeckVisibility`, deberia ir en `core/models/enums/deck/deck-visibility.enum.ts`.

#### `core/models/interfaces`

Interfaces separadas por dominio:

- `auth`: login, registro, sesion y tokens.
- `card`: respuestas o estructuras de cartas.
- `common`: tipos reutilizables como errores o paginacion.
- `deck`: requests y responses de mazos.
- `game`: estado de partida, acciones, eventos y requests de juego.
- `matchmaking`: cola y estado de busqueda de partida.
- `navigation`: items de navbar o menu.
- `system`: health check, environment y datos tecnicos.
- `user`: datos del usuario actual o perfiles.

Si una interface representa datos del backend, conviene respetar los nombres y campos del contrato backend para evitar mapeos innecesarios.

### `core/interceptors`

Interceptors globales de Angular, separados por responsabilidad.

```txt
core/interceptors
+-- auth
```

Por ejemplo, el interceptor de autenticacion vive en `core/interceptors/auth` porque se encarga de adjuntar el token o manejar requests autenticados.

### `core/storage`

Servicios para leer y escribir datos persistidos del usuario, como tokens o sesion. No deberia guardar logica de negocio de pantallas.

## `infrastructure`

`infrastructure` contiene adaptadores hacia afuera de la app. En este proyecto, principalmente servicios HTTP contra el backend.

```txt
infrastructure/api
+-- auth
+-- card
+-- deck
+-- game
+-- matchmaking
+-- system
```

Cada carpeta representa un recurso o modulo del backend:

- `auth`: login, registro, logout, usuario actual.
- `card`: cartas y carga/importacion de cartas.
- `deck`: mazos y cartas dentro de mazos.
- `game`: partidas, acciones, eventos y estado de juego.
- `matchmaking`: entrada, salida y estado de cola.
- `system`: endpoints tecnicos como health check.

Regla practica: si el archivo hace requests HTTP, normalmente va en `infrastructure/api/<dominio>`.

## `features`

`features` contiene las funcionalidades visibles para el usuario. Cada feature agrupa sus propias paginas y, si crece, puede tener sus propios componentes, servicios o modelos internos.

```txt
features
+-- auth
+-- deck
+-- game
+-- home
+-- matchmaking
+-- pokedex
+-- profile
```

Dentro de cada feature:

- `pages`: componentes que representan pantallas completas.
- `components`: componentes internos de esa feature.
- `data-access`: servicios o adaptadores especificos de la feature.
- `domain`: modelos o reglas propias de esa feature.

No todas las features necesitan todas esas carpetas. Se crean cuando hacen falta.

Ejemplo: si `deck` empieza a tener componentes como `deck-card-list` o `deck-editor`, deberian vivir en `features/deck/components`.

### Estructura de paginas y componentes

Las paginas y componentes deben vivir dentro de una carpeta propia. El nombre de la carpeta y de los archivos debe coincidir con el nombre del componente.

Para paginas:

```txt
features/<feature>/pages
+-- login-page
    +-- login-page.component.ts
    +-- login-page.component.html
    +-- login-page.component.css
```

Para componentes internos de una feature:

```txt
features/<feature>/components
+-- auth-shell
    +-- auth-shell.component.ts
    +-- auth-shell.component.html
    +-- auth-shell.component.css
```

Reglas:

- Evitar templates inline en el `.ts` cuando el componente representa una pantalla o una pieza visual con estructura propia.
- Usar `templateUrl` y `styleUrl` apuntando a los archivos de la misma carpeta.
- Mantener la logica en el `.ts`, la estructura visual en el `.html` y los estilos propios en el `.css`.
- Crear `.spec.ts` solo si la consigna o el flujo de trabajo del proyecto lo pide.

## `presentation`

`presentation` guarda componentes de estructura visual de la app que no pertenecen a una feature puntual.

```txt
presentation
+-- components
|   +-- navigation
+-- layouts
```

- `components/navigation`: componentes de navegacion global, como el navbar.
- `layouts`: layouts generales, como `main-layout`, que envuelven paginas o definen la estructura comun.

La diferencia con `shared` es que `presentation` puede estar mas conectado con la composicion visual general de la aplicacion.

## `shared`

`shared` contiene piezas reutilizables, genericas y sin dependencia fuerte de una feature.

```txt
shared
+-- components
+-- directives
+-- models
+-- pipes
+-- ui
    +-- actions
    +-- feedback
    +-- layout
```

### `shared/ui`

Componentes visuales genericos agrupados por tipo:

- `actions`: botones y controles de accion.
- `feedback`: estados vacios, mensajes, loaders, errores visuales.
- `layout`: headers, shells y estructuras reutilizables.

Un componente en `shared` deberia poder usarse en varias partes sin saber demasiado del dominio. Si solo sirve para una feature, va dentro de esa feature.

## `routes`

`routes` contiene la configuracion principal de rutas de Angular. Las constantes de paths reutilizables estan en `core/constants/routing`.

## Como decidir donde crear algo nuevo

1. Si es una pantalla, va en `features/<feature>/pages`.
2. Si es un componente usado solo por una feature, va en `features/<feature>/components`.
3. Si es un componente reutilizable y generico, va en `shared/ui/<categoria>`.
4. Si es un servicio HTTP contra backend, va en `infrastructure/api/<dominio>`.
5. Si es un tipo compartido, va en `core/models/interfaces/<dominio>`.
6. Si es un enum compartido, va en `core/models/enums/<dominio>`.
7. Si es una constante global, va en `core/constants/<categoria>`.
8. Si es configuracion de rutas, va en `routes` o `core/constants/routing`, segun corresponda.

## Convenciones de nombres

- Interfaces: `nombre-del-dominio.interface.ts`.
- Enums: `nombre-del-enum.enum.ts`.
- Servicios HTTP: `dominio-api.service.ts`.
- Componentes: carpeta `nombre`, con `nombre.component.ts`, `nombre.component.html` y `nombre.component.css`.
- Tests: junto al archivo probado, con `.spec.ts`, solo cuando se pidan.

Ejemplos:

```txt
core/models/interfaces/game/game-state.interface.ts
core/models/enums/card/card-zone.enum.ts
infrastructure/api/deck/deck-api.service.ts
shared/ui/feedback/empty-state/empty-state.component.ts
features/matchmaking/pages/matchmaking-page/matchmaking-page.component.ts
```

## Buenas practicas

- Mantener los modelos del frontend alineados con los contratos del backend.
- Evitar carpetas gigantes con archivos mezclados.
- Crear subcarpetas por dominio cuando haya mas de un concepto.
- No duplicar interfaces si ya existe una en `core/models`.
- No poner llamadas HTTP dentro de componentes.
- No mover logica de una feature a `shared` hasta que realmente sea reutilizable.
- Mantener imports claros y consistentes con la estructura.

## Resumen corto

La estructura busca que cualquier persona pueda encontrar rapido lo que necesita:

- Contratos y enums compartidos: `core/models`.
- Constantes globales: `core/constants`.
- Servicios contra backend: `infrastructure/api`.
- Pantallas reales: `features`.
- Layouts y navegacion global: `presentation`.
- UI reutilizable: `shared`.

Si cada archivo queda en la categoria correcta, el proyecto se mantiene limpio aunque siga creciendo.
