# Estado UI y pendientes

## Implementado

Pantallas o piezas funcionales:

- Login.
- Registro.
- Verificacion de cuenta.
- Recuperacion de contrasena desde login.
- Home.
- Pokedex.
- Mazo.
- Configuracion previa a jugar.
- Sala de espera/play room.
- Perfil.
- Partida/tablero.
- Navbar y layout general.

Integraciones implementadas:

- Auth HTTP.
- Cards HTTP.
- Deck HTTP.
- Matchmaking REST.
- Game API service.
- Cliente STOMP base.
- Realtime de matchmaking y partida.
- Interceptor JWT.
- Storage de sesion.
- `GameFacadeService` para hidratar partida, ejecutar acciones y consumir `STATE_SYNC`.

## Parcialmente implementado

### Play config y play room

La UI permite preparar busqueda, consultar mazo activo y entrar a cola. El consumo realtime existe, pero requiere validacion manual estable en dos navegadores.

### Perfil

El perfil existe, pero el historial visual completo no esta cerrado.

### Game page

La ruta existe, recibe `gameId`, hidrata detalle/snapshot, se suscribe a eventos y renderiza tablero. Sin embargo, el flujo completo todavia no esta validado como MVP estable.

## Bugs conocidos

- Banca que se actualiza tarde.
- Retirada que cambia recien al terminar turno.
- Acciones que se ven recien despues de `END_TURN`.
- Posible intermitencia de `STATE_SYNC` en ambos clientes.

Ver [bugs conocidos y pendientes MVP](../../docs/game/bugs-conocidos-y-pendientes.md).

## Pendiente

- Validacion manual completa desde UI con dos jugadores.
- Correccion de sincronizacion visual tardia.
- Historial visual completo.
- Confirmar reconexion y `STATE_SYNC` sin errores STOMP.
- Mantener `USE_ABILITY` deshabilitada hasta soporte backend real.

## Recomendacion para continuar

1. Ejecutar la guia manual de dos jugadores.
2. Registrar `gameId`, acciones, `stateVersion` y evidencia de snapshots.
3. Corregir primero el flujo de actualizacion de snapshots post accion.
4. Recien despues ampliar habilidades, trainers o efectos avanzados.
