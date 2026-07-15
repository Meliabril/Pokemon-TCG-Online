# Modelo de dominio

## Usuarios

Entidad principal: `User`.

Responsabilidad:

- Representar al jugador o administrador.
- Guardar email, username, password hash, rol, estado y avatar.
- Relacionarse con tokens y codigos de verificacion o recuperacion.

Entidades relacionadas:

- `RefreshToken`.
- `EmailVerificationCode`.
- `PasswordResetCode`.

## Cartas

Entidad principal: `Card`.

Responsabilidad:

- Representar una carta importada del set `xy1`.
- Guardar datos normalizados y JSON original.
- Relacionarse con ataques, costos, debilidades y resistencias.

Entidades relacionadas:

- `Attack`.
- `AttackCost`.
- `CardWeakness`.
- `CardResistance`.

Categorias relevantes:

- Pokemon basico.
- Pokemon evolucion.
- Energia basica.
- Energia especial.
- Trainer.

## Mazos

Entidad principal: `Deck`.

Responsabilidad:

- Representar uno de los mazos del usuario autenticado.
- Guardar nombre, formato, validez, estado activo y errores de validacion.
- Permitir que el usuario elija 1 mazo activo valido para matchmaking y partida.

Entidad relacionada:

- `DeckCard`: relacion entre mazo, carta y cantidad.

Reglas implementadas:

- Exactamente 60 cartas.
- Cartas del set `xy1`.
- Maximo 4 copias por nombre, excepto energia basica.
- Al menos 1 Pokemon basico.

## Partidas

Entidad principal: `Game`.

Responsabilidad:

- Representar una partida 1 vs 1.
- Guardar estado, fase, turno, version de estado y metadata.

Entidades relacionadas:

- `GameParticipant`.
- `GameCardInstance`.
- `PokemonInPlay`.
- `PokemonEvolutionStack`.
- `PokemonAttachedCard`.
- `SpecialCondition`.
- `GameActionLog`.
- `GameStateSnapshot`.
- `GameEvent`.

## Participantes

Entidad principal: `GameParticipant`.

Responsabilidad:

- Asociar usuario, partida y mazo.
- Definir orden de jugador.
- Mantener datos de estado por jugador cuando corresponde.

## Cartas en partida

Entidad principal: `GameCardInstance`.

Responsabilidad:

- Representar una copia fisica de una carta durante una partida.
- Guardar zona actual, propietario y orden cuando corresponde.

Zonas esperadas:

- Mazo.
- Mano.
- Premio.
- Banca.
- Activo.
- Descarte.

## Pokemon en juego

Entidades:

- `PokemonInPlay`.
- `PokemonEvolutionStack`.
- `PokemonAttachedCard`.
- `SpecialCondition`.

Responsabilidad:

- Modelar activo y banca.
- Mantener dano.
- Mantener pila evolutiva.
- Guardar energias o herramientas unidas.
- Registrar condiciones especiales.

## Auditoria de juego

Entidades:

- `GameActionLog`.
- `GameStateSnapshot`.
- `GameEvent`.

Responsabilidad:

- Registrar acciones ejecutadas.
- Guardar snapshots versionados.
- Publicar y persistir eventos visibles para clientes.

Estas entidades permiten historial, pausa/reanudacion, reconexion y depuracion del flujo de juego.
