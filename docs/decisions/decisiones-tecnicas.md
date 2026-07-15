# Decisiones tecnicas

## Backend como fuente de verdad

Decision: el backend valida y ejecuta las reglas del juego.

Motivo:

- Evita que el cliente manipule reglas.
- Permite persistir historial, snapshots y eventos confiables.
- Facilita sincronizacion entre dos jugadores.

Impacto:

- El frontend envia intenciones.
- El backend responde estado y eventos.
- Las reglas deben concentrarse en servicios y validadores backend.

## Set de cartas XY1

Decision: trabajar con el set `xy1` de Pokemon TCG API.

Motivo:

- Acota el dominio.
- Permite validar cantidad esperada de 146 cartas.
- Simplifica reglas de mazo y pruebas.

Impacto:

- Los endpoints de cartas restringen el set.
- La importacion falla si el lote no cumple.
- Los mazos rechazan cartas fuera de `xy1`.

## JWT y refresh token

Decision: autenticacion propia con JWT y refresh token rotativo.

Motivo:

- Mantener backend stateless para access token.
- Permitir renovacion de sesion.
- Proteger REST y STOMP con el mismo usuario autenticado.

Impacto:

- Frontend guarda sesion en storage.
- Interceptor adjunta access token.
- WebSocket requiere header `Authorization: Bearer <accessToken>` en STOMP `CONNECT`.

## WebSocket STOMP

Decision: usar Spring WebSocket con STOMP.

Motivo:

- Separar canales publicos y privados.
- Permitir notificaciones de matchmaking y sincronizacion de juego.
- Integrar con usuarios autenticados de Spring.

Impacto:

- Backend ya define endpoint `/ws`.
- Frontend tiene destinos declarados, pero falta cliente real.

## H2 actual vs PostgreSQL documentado

Decision observada en codigo: H2 en memoria como base activa.

Documentacion previa: SDD menciona PostgreSQL como stack obligatorio.

Impacto:

- La ejecucion local es simple.
- Las migraciones Flyway existen, pero no gobiernan el schema activo porque Flyway esta desactivado.
- Antes de presentar PostgreSQL como implementado, se debe ajustar configuracion o documentarlo como pendiente.

## Tres mazos y mazo activo

Decision funcional: el usuario puede tener hasta 3 mazos disponibles.

De esos mazos, el usuario elige o activa 1 mazo valido para usar en matchmaking y partida.

Impacto:

- La validacion aplica a cada mazo.
- El mazo activo debe ser valido para entrar a matchmaking.
- La partida usa el mazo activo seleccionado por el usuario.

## Estructura feature-driven en frontend

Decision: organizar Angular por features y responsabilidades.

Motivo:

- Facilita ubicar pantallas y servicios.
- Evita mezclar UI reusable con dominio especifico.
- Permite al equipo continuar desarrollo por modulo.

Impacto:

- Pantallas en `features`.
- HTTP en `infrastructure/api`.
- Modelos y constantes globales en `core`.
- UI reutilizable en `shared`.
