# Mecanica "lanzar moneda" en ataques xy1 - estado y pendientes

Este documento resume la arquitectura del sistema de efectos de ataque basados en
lanzamiento de monedas para el set `xy1`, lo que ya esta implementado/verificado, y
lo que falta para considerar la mecanica "completa". Pensado para que cualquier
agente/dev pueda retomar el trabajo sin tener que releer todo el historial de chat.

## Arquitectura general

- **Catalogo data-driven**: `src/main/resources/game-engine/xy1-attack-effects.json`
  (49 `cardExternalId` + `attackName` definidos a la fecha). Se lee con
  `AttackEffectDefinitionReader` (Jackson `ObjectMapper`, match case-insensitive por
  `(cardExternalId, attackName)`). Si un ataque tiene `effectText` no vacio y NO tiene
  entrada en este catalogo, se lanza `InvalidGameActionException` al usarlo.
- **`AttackEffectOperation`** (record, 11 campos):
  `type, phase, target, amount, conditionType, coinRequirement, cancelOnTails,
  coinCount, coinGroupKey, dynamicCoinCountSource, energyType`.
  - `phase`: `BEFORE_DAMAGE` o `AFTER_DAMAGE`; se resuelven en dos pasadas separadas
    desde `AttackEffectServiceImpl`.
  - `coinRequirement`: `NONE | HEADS | TAILS`; si no es `NONE`, se tira 1 moneda
    (salvo `coinGroupKey`) y la operacion solo se aplica si el resultado coincide.
  - `cancelOnTails`: si la moneda sale tails y este flag esta activo, se cancela el
    ataque completo.
  - `coinGroupKey`: opcional. Operaciones con la misma key dentro de la misma fase
    comparten un unico resultado de moneda. No comparte entre `BEFORE_DAMAGE` y
    `AFTER_DAMAGE` porque son llamadas separadas.
  - `dynamicCoinCountSource`: opcional. Usado por `DYNAMIC_COIN_DAMAGE` para calcular
    cuantas monedas se lanzan desde el estado actual.
  - `energyType`: opcional. Usado cuando `dynamicCoinCountSource =
    ATTACKER_ATTACHED_ENERGY_TYPE`.
- **Strategy pattern**: cada `type` de operacion tiene un `AttackEffect`
  (`supports()`/`apply()`) en `services/game/attack/impl/`. Implementaciones actuales:
  - `CoinMultiDamageAttackEffect`: `COIN_MULTI_DAMAGE`.
  - `CoinDamageModifierAttackEffect`: `COIN_DAMAGE_MODIFIER`.
  - `ApplySpecialConditionAttackEffect`: `APPLY_SPECIAL_CONDITION`.
  - `DynamicCoinDamageAttackEffect`: `DYNAMIC_COIN_DAMAGE`.
  - `DiscardEnergyAttackEffect`: `DISCARD_ENERGY`.
  - `RecoilDamageAttackEffect`: `RECOIL_DAMAGE`.
  - `BenchDamageAttackEffect`, `HealDamageAttackEffect`: usados por otras cartas.
  - `PreventDamageNextTurnAttackEffect`: `PREVENT_DAMAGE_NEXT_TURN`.
  - `PreventSelfAttackNextTurnAttackEffect`: `PREVENT_SELF_ATTACK_NEXT_TURN`.
  - `PreventOpponentSupporterNextTurnAttackEffect`:
    `PREVENT_OPPONENT_SUPPORTER_NEXT_TURN`.
  - `CoinMultiDamageAllHeadsProtectionAttackEffect`:
    `COIN_MULTI_DAMAGE_ALL_HEADS_PROTECTION`.
  - `CoinAllHeadsOpponentDeckDiscardAttackEffect`:
    `COIN_ALL_HEADS_OPPONENT_DECK_DISCARD`.
  - `OpponentCoinTailsHandDiscardAttackEffect`:
    `OPPONENT_COIN_TAILS_HAND_DISCARD`.
  - `SearchSupporterFromDeckAttackEffect`: `SEARCH_SUPPORTER_FROM_DECK`.
  - `SelfSwitchWithBenchAttackEffect`: `SELF_SWITCH_WITH_BENCH`.
  - `AttachWaterEnergyFromDiscardToBenchAttackEffect`:
    `ATTACH_WATER_ENERGY_FROM_DISCARD_TO_BENCH`.
  - `MentalPanicAttackEffect` + `MentalPanicService`: `MENTAL_PANIC`.
- **Coin gate central**: `AttackEffectServiceImpl.applyEffects()` orquesta
  flip/agrupamiento/`cancelOnTails` y despacha a la `AttackEffect` correspondiente.

## Incrementos completados

1. **Coin gate generico**: `CoinRequirement`, `cancelOnTails` y
   `COIN_MULTI_DAMAGE`.
2. **Bucket 0**: 20 operaciones JSON-only para 19 cartas, reutilizando tipos ya
   existentes.
3. **Incremento A - prevenir dano el proximo turno rival**:
   `PREVENT_DAMAGE_NEXT_TURN` para xy1-13 (Scrunch), xy1-111 y xy1-112 (Dig).
   Agrego `damageProtectionTurn`, `DamageProtectionService`, consumo en
   `AttackServiceImpl` y expiracion en `BetweenTurnsResolutionServiceImpl`.
   Simplificacion conocida: para Bunnelby/Diggersby solo se previene dano, no todos
   los efectos del ataque.
4. **Bucket 1**: 6 operaciones JSON-only: xy1-32 "Spike Cannon", xy1-37/65/66,
   xy1-114 y xy1-142.
5. **Incremento B - grupo de moneda compartido**: `coinGroupKey` para xy1-32
   "Clamp Crush", xy1-67 "Dynamic Punch" y xy1-76 "Distortion Beam".
6. **Bucket 2 - lanzar moneda hasta que salga cruz**:
   `COIN_FLIP_UNTIL_TAILS_DAMAGE` + `CoinFlipUntilTailsDamageAttackEffect` para
   xy1-36 "Spiny Rush" y xy1-52 "Continuous Tumble".
7. **No puede atacar el proximo turno propio**:
   `PREVENT_SELF_ATTACK_NEXT_TURN` para xy1-78 "Darkness Blade". Agrego
   `attackLockedTurn`, `AttackLockService`, consumo antes de validar energia y
   expiracion entre turnos.
8. **Conteo dinamico de monedas**:
   `DYNAMIC_COIN_DAMAGE` + `DynamicCoinDamageAttackEffect`.
   - `ATTACKER_ATTACHED_ENERGY_TYPE`: lanza 1 moneda por cada energia adjunta al
     atacante que matchee `energyType`.
   - `ATTACKER_DAMAGE_COUNTERS`: lanza 1 moneda por cada contador de dano del
     atacante.
   - Cubre xy1-62 Rhyperior "Rock Black" y xy1-100 Tauros "Seething Anger".
9. **Lock de cartas de Partidario rival**:
   `PREVENT_OPPONENT_SUPPORTER_NEXT_TURN` + `SupporterLockService`.
   - Cubre xy1-71 Krookodile "Bother".
   - El ataque setea `supporterLockedTurn` en el participante rival para su proximo
     turno.
   - `TrainerEffectServiceImpl` rechaza Partidarios mientras el lock esta activo.
   - `BetweenTurnsResolutionServiceImpl` expira el lock al cierre del turno bloqueado.
10. **Condicionales multi-flip con resultado agregado**:
    - `COIN_MULTI_DAMAGE_ALL_HEADS_PROTECTION` cubre xy1-5 Beedrill "Flash Needle":
      lanza 3 monedas, hace 40 dano por cara, y si todas son cara aplica proteccion
      de dano para el proximo turno rival. Simplificacion conocida: igual que Dig,
      se previene dano pero no todos los efectos secundarios del ataque.
    - `COIN_ALL_HEADS_OPPONENT_DECK_DISCARD` cubre xy1-61 Rhydon "Mad Mountain":
      lanza 2 monedas y, si ambas son cara, descarta del tope del mazo rival 1 carta
      por cada contador de dano del atacante.
11. **Monedas del rival con descarte de mano**:
    `OPPONENT_COIN_TAILS_HAND_DISCARD` cubre xy1-76 Malamar "Mental Trash":
    el rival lanza 4 monedas y se descarta 1 carta de su mano por cada cruz.
    Politica actual: como el motor no pausa para que el rival elija cartas de su
    mano, el descarte se resuelve automaticamente con cartas aleatorias de la mano
    rival, limitado por la cantidad de cartas disponibles.
12. **Busqueda automatica de Partidario en mazo**:
    `SEARCH_SUPPORTER_FROM_DECK` cubre xy1-18 Skiddo "Lead". Si la moneda sale cara,
    busca el primer Partidario en el mazo del atacante, lo revela via evento, lo mueve
    a la mano y baraja el resto del mazo. Politica actual: al no existir eleccion
    interactiva de carta, se toma el primer Partidario por orden de mazo.
13. **Cambio automatico con banca**:
    `SELF_SWITCH_WITH_BENCH` cubre xy1-39 Froakie "Bounce". Si la moneda sale cara,
    cambia el Pokemon atacante con el primer Pokemon de la banca por `slotPosition`.
    Politica actual: al no existir seleccion adicional de banca dentro del ataque,
    se elige el menor `slotPosition`.
14. **Adjuntar Energia Agua desde descarte a banca**:
    `ATTACH_WATER_ENERGY_FROM_DISCARD_TO_BENCH` cubre xy1-35 Lapras "Seafaring".
    Lanza 3 monedas y adjunta 1 Energia Agua basica desde el descarte por cada cara,
    limitada por energias disponibles y Pokemon en banca. Politica actual: al no
    existir seleccion interactiva de distribucion, reparte las energias entre la
    banca en orden de `slotPosition`, repitiendo desde el inicio si hace falta.
15. **Efecto diferido al intentar atacar**:
    `MENTAL_PANIC` cubre xy1-77 Malamar "Mental Panic". El ataque marca al Pokemon
    defensor para su proximo turno. Cuando ese Pokemon intenta atacar, se lanza 1
    moneda; si sale cruz, el ataque se cancela y el turno se cierra sin aplicar dano
    ni efectos. El lock se consume al intentarlo y expira al cerrar el turno marcado.

Catalogo actual: 49 entradas `cardExternalId`/`attackName` (xy1-32 aparece dos veces:
"Spike Cannon" y "Clamp Crush", ataques distintos de la misma carta).

## Como correr los tests relevantes

```bash
./mvnw -o -q test -Dtest="AttackEffectServiceImplTest,Xy1AttackEffectCatalogTest,CoinMultiDamageAttackEffectTest,CoinMultiDamageAllHeadsProtectionAttackEffectTest,CoinAllHeadsOpponentDeckDiscardAttackEffectTest,OpponentCoinTailsHandDiscardAttackEffectTest,SearchSupporterFromDeckAttackEffectTest,SelfSwitchWithBenchAttackEffectTest,AttachWaterEnergyFromDiscardToBenchAttackEffectTest,MentalPanicAttackEffectTest,MentalPanicServiceImplTest,CoinFlipUntilTailsDamageAttackEffectTest,DynamicCoinDamageAttackEffectTest,PreventDamageNextTurnAttackEffectTest,PreventSelfAttackNextTurnAttackEffectTest,PreventOpponentSupporterNextTurnAttackEffectTest,AttackLockServiceImplTest,SupporterLockServiceImplTest,DamageProtectionServiceImplTest,*Attack*,GameActionEngineFlowIntegrationTest"
```

No se agregan tests de FE (`.spec.ts`) en este proyecto.

## Pendientes - ataques de moneda sin cubrir

Relevados desde `i18n/cards-es.json` y contrastados contra el catalogo JSON.
Cualquiera de estas cartas con `effectText` no vacio dispara
`InvalidGameActionException` si se intenta usar hoy porque no tiene entrada en el
catalogo.

| Carta | Ataque | Texto (ES) | Mecanica nueva requerida |
|---|---|---|---|
No quedan ataques de moneda xy1 pendientes en el relevamiento actual.

## Pendiente no moneda detectado durante el relevamiento

| Carta | Ataque | Texto (ES) | Mecanica nueva requerida |
|---|---|---|---|
| xy1-67 Conkeldurr | Espabila | Si el Pokemon Defensor activo tiene una Condicion Especial, +60 dano y luego se le quitan todas las condiciones especiales. | Sin moneda: condicional sobre estado del rival + bonus de dano + limpiar condiciones especiales del rival. |

## Agrupamiento sugerido para proximos incrementos

- **Monedas xy1**: sin pendientes conocidos.
- **No moneda / estado de condiciones**:
  xy1-67. Conviene tratarlo fuera del roadmap estricto de monedas.

## Notas para quien retome

- Verificar siempre primero si la carta/ataque ya tiene entrada en
  `xy1-attack-effects.json` antes de asumir que falta.
- Los campos nuevos de `AttackEffectOperation` tienen un constructor backward
  compatible para los tests existentes con 9 argumentos.
- Si se agrega un campo nullable nuevo al record, las entradas existentes del catalogo
  que no lo tengan deserializan como `null` via Jackson.
- `groupCoinResults` es local a cada llamada de `applyEffects()`; si un combo necesita
  compartir resultado entre `BEFORE_DAMAGE` y `AFTER_DAMAGE`, hay que extender el
  scope de ese mapa.
