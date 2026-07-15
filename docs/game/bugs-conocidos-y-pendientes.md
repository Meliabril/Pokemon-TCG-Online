# Bugs conocidos y pendientes MVP

Este documento separa defectos observados de funcionalidades pendientes. Un bug es un comportamiento que deberia funcionar segun el flujo actual; un pendiente es una capacidad que todavia no esta completa o no forma parte del MVP validado.

## Bugs conocidos

| Bug | Impacto | Estado | Como reproducir | Resultado esperado |
| --- | --- | --- | --- | --- |
| Banca se actualiza tarde | El jugador o rival no ve inmediatamente un Pokemon bajado a banca. | Requiere reproduccion y correccion | Ejecutar `PLAY_BASIC_POKEMON` desde UI y comparar snapshot con tablero. | La banca debe actualizarse al recibir snapshot/evento de la accion. |
| Retirada cambia recien al terminar turno | El intercambio activo/banca no se ve inmediatamente. | Requiere reproduccion y correccion | Ejecutar `RETREAT`, observar tablero antes y despues de `END_TURN`. | El cambio de activo debe verse luego del `STATE_SYNC` de `RETREAT`. |
| Acciones visibles recien despues de `END_TURN` | Algunas acciones no refrescan estado visual hasta el fin de turno. | Requiere reproduccion y correccion | Ejecutar acciones en `MAIN` y revisar `availableActions`/tablero. | Cada accion aceptada debe reflejarse sin esperar una accion posterior. |
| `STATE_SYNC` post accion intermitente en ambos clientes | Puede dejar un cliente atrasado visualmente. | Parcial / requiere validacion | Probar dos navegadores y revisar eventos privados por jugador. | Ambos clientes deben recibir su sync privado despues de cada accion exitosa. |
| Error STOMP intermitente en corrida previa | Puede cortar suscripcion o impedir sync. | Requiere validacion actual | Repetir prueba manual completa observando consola backend/frontend. | No deben aparecer errores de canal STOMP durante acciones normales. |

## Pendientes funcionales para MVP jugable

| Pendiente | Motivo | Siguiente paso |
| --- | --- | --- |
| Validacion manual completa de partida desde UI | Hay pruebas tecnicas parciales, pero falta cierre end-to-end reciente. | Ejecutar la guia de dos jugadores y registrar resultado actualizado. |
| Habilidades (`USE_ABILITY`) | Backend reconoce la accion pero la rechaza de forma controlada. | Implementar motor de habilidades o mantener UI deshabilitada. |
| Cobertura completa de Trainers | Solo hay efectos registrados soportados. | Documentar lista de Trainers soportados y bloquear los demas. |
| Cobertura amplia de ataques XY1 | El motor depende de definiciones curadas. | Completar/validar `game-engine/xy1-attack-effects.json`. |
| Fin de partida desde UI | El backend tiene condiciones de victoria, pero falta prueba manual completa. | Probar KO sin banca, premios finales y mazo vacio. |
| Historial visual completo | Backend registra logs/eventos; UI de historial no esta cerrada. | Completar pantalla o dejar fuera de MVP jugable. |
| Decision runtime PostgreSQL/H2 | La consigna menciona PostgreSQL, pero el entorno local documentado usa H2. | Definir base de entrega y actualizar instalacion. |

## Pendientes tecnicos recomendados

- Agregar pruebas E2E o manual scriptadas para el flujo `matchmaking -> setup -> draw -> main actions -> attack`.
- Registrar fecha, usuarios y `gameId` de cada prueba manual.
- Revisar que `GameFacadeService` aplique snapshots de acciones sin quedar esperando una version futura.
- Verificar que el mapper de tablero no descarte cambios validos por readiness incompleta.
- Revisar que los eventos visuales no sustituyan al snapshot como fuente final de verdad.

## No incluir como bug

- `TAKE_PRIZE_CARD` no estar disponible manualmente no es bug actual: el backend toma premios automaticamente.
- `USE_ABILITY` bloqueado no es bug actual: es pendiente controlado.
- Datos visuales faltantes de cartas publicas no son bug si el snapshot no los expone; son pendiente de contrato.
