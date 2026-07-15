# Reglas del juego Pokemon TCG digital

Este documento conserva la referencia de reglas usada por el proyecto para modelar el motor de partida. Debe leerse como documentacion de dominio, no como prompt operativo para agentes.

El backend es la fuente de verdad: valida acciones, aplica reglas, persiste estado y publica eventos. El frontend solo envia intenciones y renderiza el estado confirmado.

---

---

## 1. PREPARACIÃ“N DE LA PARTIDA

### 1.1 Reparto inicial
- Cada jugador tiene un mazo de exactamente 60 cartas.
- Cada jugador baraja su mazo.
- Cada jugador roba 7 cartas iniciales.
- Las cartas de un jugador NO son visibles para el oponente en ningÃºn momento,
  salvo que una carta lo indique explÃ­citamente.

### 1.2 Mulligan (Segunda Oportunidad)
- Cada jugador verifica si su mano tiene al menos 1 PokÃ©mon BÃ¡sico.
- Si un jugador NO tiene PokÃ©mon BÃ¡sico en mano:
  1. Muestra su mano al oponente.
  2. Devuelve las 7 cartas al mazo.
  3. Baraja el mazo.
  4. Roba 7 cartas nuevas.
  5. Incrementa su contador de mulligans en 1.
  6. Repite el proceso hasta tener al menos 1 PokÃ©mon BÃ¡sico.
- Las cartas extra para el oponente se otorgan AL FINAL, no durante el proceso:
  el oponente roba 1 carta adicional por cada mulligan realizado.
- Si ambos jugadores hacen mulligan, cada uno acumula mulligans de forma
  independiente y recibe cartas extra segÃºn los mulligans del rival.

### 1.3 ColocaciÃ³n inicial de PokÃ©mon
- Cada jugador elige 1 PokÃ©mon BÃ¡sico de su mano â†’ lo coloca como PokÃ©mon Activo.
  Esta acciÃ³n es OBLIGATORIA.
- Cada jugador puede elegir hasta 5 PokÃ©mon BÃ¡sicos adicionales de su mano
  â†’ los coloca en la Banca. Esta acciÃ³n es OPCIONAL (puede colocar entre 0 y 5).
- Solo se permiten PokÃ©mon BÃ¡sicos en esta fase (no evoluciones).
- Todas las cartas se colocan BOCA ABAJO al mismo tiempo.
- NingÃºn jugador puede ver el campo del rival hasta que ambos terminen.

### 1.4 Cartas de Premio
- Cada jugador toma las primeras 6 cartas de su mazo y las coloca boca abajo
  como sus cartas de Premio.
- Estas cartas NO forman parte de la mano.
- El mazo queda con 47 cartas (60 âˆ’ 7 robadas âˆ’ 6 de premio) antes de colocar PokÃ©mon.
- Las cartas de Premio permanecen ocultas para ambos jugadores durante la partida,
  hasta que se toman como recompensa.

### 1.5 Inicio de la partida
- Se lanza una moneda para determinar quÃ© jugador empieza.
- Ambos jugadores revelan sus PokÃ©mon Activos y de Banca simultÃ¡neamente.
- El jugador inicial toma el primer turno.
- RESTRICCIÃ“N: el jugador que va primero NO puede atacar en su primer turno.

---

## 2. ESTRUCTURA DEL TURNO

El turno sigue este orden de fases. El orden es FIJO y no se puede alterar.

```
DRAW â†’ MAIN â†’ ATTACK â†’ BETWEEN_TURNS
```

### Fase DRAW
- El jugador activo roba exactamente 1 carta de su mazo.
- Si el mazo estÃ¡ vacÃ­o al intentar robar â†’ el jugador PIERDE la partida inmediatamente.
- Esta regla se evalÃºa al inicio del turno, antes de cualquier otra acciÃ³n.

### Fase MAIN
El jugador puede realizar las siguientes acciones en CUALQUIER orden,
todas las veces que sean vÃ¡lidas, salvo restricciÃ³n explÃ­cita:

**Colocar PokÃ©mon BÃ¡sicos en Banca**
- Se pueden colocar desde la mano a la Banca.
- Sin lÃ­mite de cantidad por turno, pero la Banca tiene un mÃ¡ximo de 5 PokÃ©mon.
- Si la Banca ya tiene 5 PokÃ©mon, no se pueden colocar mÃ¡s.

**Evolucionar PokÃ©mon**
- Se puede evolucionar tantas veces como sea posible en el turno.
- RESTRICCIÃ“N 1: no se puede evolucionar un PokÃ©mon en el mismo turno en que
  fue colocado en juego (incluyendo la fase de preparaciÃ³n).
- RESTRICCIÃ“N 2: ningÃºn PokÃ©mon puede evolucionar en el primer turno del jugador.
- La evoluciÃ³n elimina todos los estados especiales del PokÃ©mon.
- Los contadores de daÃ±o previos se mantienen (salvo que la carta diga lo contrario).

**Unir EnergÃ­a**
- Se puede unir exactamente 1 carta de EnergÃ­a por turno.
- La energÃ­a puede unirse a cualquier PokÃ©mon propio (activo o en banca).
- Una vez unida 1 energÃ­a en el turno, no se puede unir otra (flag `energyAttachedThisTurn`).

**Jugar cartas de Entrenador**
- Objeto: se pueden jugar todas las que se desee en el turno. Efecto inmediato â†’ se descarta.
- Partidario: solo 1 por turno (flag `supporterPlayedThisTurn`). Efecto inmediato â†’ se descarta.
- Estadio: solo 1 por turno. Permanece en juego en zona compartida.
  Si se juega otro Estadio, reemplaza al anterior (el anterior se descarta).
  Solo puede haber 1 Estadio activo en todo el tablero.
- Herramienta PokÃ©mon: se une a un PokÃ©mon propio. MÃ¡ximo 1 herramienta por PokÃ©mon.
  Permanece hasta que el PokÃ©mon salga del juego.

**Retirar el PokÃ©mon Activo**
- Solo 1 vez por turno (flag `retreatedThisTurn`).
- El jugador debe descartar del PokÃ©mon Activo tantas cartas de EnergÃ­a como
  indique su costo de retirada.
- El PokÃ©mon pasa a la Banca y el jugador elige un PokÃ©mon de la Banca
  como nuevo Activo.
- Al ir a la Banca, el PokÃ©mon pierde todos sus estados especiales.

**Usar habilidades**
- Se pueden usar todas las habilidades disponibles que cumplan sus condiciones.
- Cada habilidad especifica cuÃ¡ndo y cÃ³mo puede activarse.

### Fase ATTACK
- El jugador puede atacar UNA SOLA VEZ con su PokÃ©mon Activo.
- Atacar es OPCIONAL, pero termina el turno automÃ¡ticamente al hacerlo.
- El jugador tambiÃ©n puede elegir finalizar el turno sin atacar.
- RESTRICCIÃ“N: el jugador que va primero NO puede atacar en su primer turno.

### Fase BETWEEN_TURNS
Se procesa al terminar el ataque (o al finalizar el turno sin atacar).
El orden de procesamiento es CRÃTICO y debe respetarse exactamente:

```
1. Envenenado   â†’ aplica 1 contador de daÃ±o (automÃ¡tico, sin moneda)
2. Quemado      â†’ lanza moneda; si sale CRUZ aplica 2 contadores de daÃ±o
3. Dormido      â†’ lanza moneda; si sale CARA el PokÃ©mon se despierta
4. Paralizado   â†’ se cura automÃ¡ticamente si ya pasÃ³ el turno completo
```

Luego:
5. Aplicar efectos de habilidades que se activan entre turnos.
6. Verificar Knockout para cada PokÃ©mon con daÃ±o acumulado.

Una vez finalizado este procesamiento, el turno pasa al oponente.

---

## 3. SISTEMA DE ATAQUE

### 3.1 ValidaciÃ³n inicial
Antes de ejecutar cualquier ataque verificar:
- El jugador eligiÃ³ un ataque vÃ¡lido de los disponibles en su PokÃ©mon Activo.
- El PokÃ©mon Activo tiene la energÃ­a suficiente para el ataque elegido.
- El PokÃ©mon Activo NO estÃ¡ en estado Dormido ni Paralizado.
- No es el primer turno del jugador que va primero.

### 3.2 ResoluciÃ³n de ConfusiÃ³n (si aplica)
Si el PokÃ©mon Activo estÃ¡ Confundido:
- Lanzar moneda ANTES de ejecutar el ataque.
- Si sale CARA â†’ el ataque se ejecuta normalmente.
- Si sale CRUZ:
  - El ataque FALLA completamente (no se aplica daÃ±o ni efectos al rival).
  - El PokÃ©mon Confundido recibe 3 contadores de daÃ±o (30 de daÃ±o a sÃ­ mismo).
  - El turno termina inmediatamente.

### 3.3 SelecciÃ³n de objetivo
- Por defecto: el objetivo es el PokÃ©mon Activo del rival.
- Si el ataque lo permite explÃ­citamente: el jugador puede elegir un PokÃ©mon
  de la Banca rival como objetivo.

### 3.4 Efectos previos al daÃ±o
Ejecutar antes de calcular el daÃ±o:
- Lanzamientos de moneda que modifican el daÃ±o (ej: "si sale cara, +40 de daÃ±o").
- Chequeos de condiciÃ³n que pueden cancelar el ataque.
- Estos resultados modifican o cancelan el daÃ±o que se calcularÃ¡ a continuaciÃ³n.

### 3.5 Modificadores y cancelaciones
Verificar efectos activos que puedan modificar el ataque:
- Buffs activos del atacante (entrenadores, habilidades, efectos del turno anterior).
- Debuffs del defensor (efectos que bloquean daÃ±o, reducen daÃ±o, etc.).
- Ejemplo: si el rival tiene un efecto "no puede recibir daÃ±o este turno",
  el daÃ±o no se aplica.

### 3.6 CÃ¡lculo del daÃ±o â€” ORDEN OBLIGATORIO

El daÃ±o se calcula en este orden exacto. No se puede alterar:

```
Paso 1: DaÃ±o base
        â†’ El valor que indica la carta del ataque.

Paso 2: Modificadores del atacante
        â†’ Sumar/restar segÃºn entrenadores, habilidades y buffs activos.

Paso 3: Debilidad
        â†’ Si el PokÃ©mon defensor es dÃ©bil al tipo del atacante:
          daÃ±o = daÃ±o Ã— 2

Paso 4: Resistencia
        â†’ Si el PokÃ©mon defensor tiene resistencia al tipo del atacante:
          daÃ±o = daÃ±o âˆ’ 20
          NUNCA puede bajar de 0 por este paso.

Paso 5: Modificadores del defensor
        â†’ Aplicar reducciones, escudos y efectos activos del defensor.

Paso 6: AplicaciÃ³n final
        â†’ Convertir daÃ±o a contadores:
          damageCounters = finalDamage / 10
          (10 de daÃ±o = 1 contador)
```

### 3.7 Efectos posteriores al daÃ±o
DespuÃ©s de aplicar el daÃ±o:
- Aplicar estados alterados al defensor si el ataque lo indica
  (Envenenado, Dormido, Paralizado, Quemado, Confundido).
- Descartar energÃ­as si el ataque lo requiere.
- Aplicar curaciÃ³n si el ataque lo indica.
- Aplicar daÃ±o en banca si el ataque lo indica.
- Aplicar recoil (daÃ±o al propio atacante) si el ataque lo indica.

---

## 4. KNOCKOUT (FUERA DE COMBATE)

### 4.1 CondiciÃ³n de Knockout
Un PokÃ©mon queda Fuera de Combate cuando:
```
damageCounters Ã— 10 â‰¥ HP mÃ¡ximo del PokÃ©mon
```

### 4.2 Proceso de Knockout
1. El PokÃ©mon derrotado y TODAS las cartas unidas a Ã©l
   (EnergÃ­as, Herramienta) se mueven a la pila de descarte.
2. El oponente toma cartas de Premio:
   - PokÃ©mon normal â†’ 1 carta de Premio.
   - PokÃ©mon-EX â†’ 2 cartas de Premio.
   - MegaevoluciÃ³n â†’ 2 cartas de Premio.
3. El jugador afectado debe elegir un PokÃ©mon de su Banca como nuevo Activo.
4. Si el jugador NO tiene PokÃ©mon en Banca para reemplazar al Activo
   â†’ pierde la partida inmediatamente.

### 4.3 EvaluaciÃ³n
- El Knockout se evalÃºa inmediatamente despuÃ©s de aplicar el daÃ±o.
- TambiÃ©n se evalÃºa despuÃ©s de los efectos entre turnos
  (el veneno o quemadura puede causar un KO).

---

## 5. CONDICIONES ESPECIALES

### 5.1 Reglas generales
- Solo el PokÃ©mon ACTIVO puede tener condiciones especiales.
- Los PokÃ©mon en Banca no pueden tener condiciones especiales.
- Las condiciones se eliminan COMPLETAMENTE cuando el PokÃ©mon:
  - Va a la Banca (por retirada o reemplazo tras KO del activo).
  - Evoluciona.

### 5.2 Tipos de condiciones

**DORMIDO** ðŸ’¤
- El PokÃ©mon NO puede atacar.
- El PokÃ©mon NO puede retirarse.
- Entre turnos: lanzar moneda.
  - CARA â†’ el PokÃ©mon se despierta (condiciÃ³n eliminada).
  - CRUZ â†’ el PokÃ©mon sigue dormido.
- Se resuelve entre turnos, no durante el ataque.

**QUEMADO** ðŸ”¥
- Entre turnos: lanzar moneda.
  - CRUZ â†’ el PokÃ©mon recibe 2 contadores de daÃ±o (20 de daÃ±o).
  - CARA â†’ no recibe daÃ±o.
- El daÃ±o NO es automÃ¡tico: depende del resultado de la moneda.
- Usa marcador independiente (no un contador directo).

**CONFUNDIDO** ðŸ˜µ
- Se evalÃºa Ãºnicamente al intentar atacar.
- Antes del ataque: lanzar moneda.
  - CRUZ â†’ el ataque falla, el PokÃ©mon recibe 3 contadores de daÃ±o (30), termina el turno.
  - CARA â†’ el ataque se ejecuta normalmente.

**PARALIZADO** âš¡
- El PokÃ©mon NO puede atacar.
- El PokÃ©mon NO puede retirarse.
- Se cura automÃ¡ticamente al final del siguiente turno completo del jugador.
- No requiere lanzar moneda.
- Dura exactamente 1 turno completo del jugador.

**ENVENENADO** â˜ ï¸
- Entre turnos: el PokÃ©mon recibe 1 contador de daÃ±o (10 de daÃ±o) automÃ¡ticamente.
- No requiere moneda. El daÃ±o es fijo y constante.

### 5.3 Compatibilidad entre condiciones

**Condiciones MUTUAMENTE EXCLUYENTES** (solo puede haber 1 activa):
- Dormido, Confundido y Paralizado NO pueden coexistir.
- Si se aplica una nueva condiciÃ³n de este grupo, reemplaza a la anterior.
- Ejemplo: si el PokÃ©mon estaba Dormido y queda Paralizado â†’ queda SOLO Paralizado.

**Condiciones ACUMULABLES**:
- Quemado y Envenenado pueden coexistir entre sÃ­.
- Quemado y/o Envenenado pueden coexistir con Dormido, Confundido o Paralizado.
- Ejemplo vÃ¡lido: PokÃ©mon Quemado + Envenenado + Paralizado.
- NO se puede duplicar el mismo estado (no puede estar "doble envenenado").

### 5.4 Orden de procesamiento entre turnos (CRÃTICO)
```
1. Envenenado
2. Quemado
3. Dormido
4. Paralizado
```
Este orden es obligatorio. El resultado de cada paso puede causar KO
antes de procesar el siguiente.

---

## 6. TIPOS DE CARTAS

### 6.1 PokÃ©mon
- Tienen: HP, ataques, debilidad, resistencia y costo de retirada.
- Tipos de evoluciÃ³n: BÃ¡sico, Fase 1, Fase 2.
- Restricciones de evoluciÃ³n: ver secciÃ³n 2, Fase MAIN.

### 6.2 PokÃ©mon-EX
- Son PokÃ©mon BÃ¡sicos con mÃ¡s HP y ataques mÃ¡s poderosos.
- Regla especial: cuando son derrotados, el rival toma 2 cartas de Premio.
- El sufijo "-EX" forma parte del nombre de la carta.

### 6.3 MegaevoluciÃ³n (opcional)
- Es una evoluciÃ³n de PokÃ©mon-EX.
- Al megaevolucionar: el turno termina AUTOMÃTICAMENTE.
- Al ser derrotada: el rival toma 2 cartas de Premio.

### 6.4 EnergÃ­a BÃ¡sica
- Sin lÃ­mite de copias en el mazo.
- Se puede unir 1 por turno.

### 6.5 EnergÃ­a Especial
- MÃ¡ximo 4 copias en el mazo.
- Tiene efectos adicionales que se aplican en combate.

### 6.6 Objeto
- Se pueden jugar cantidades ilimitadas por turno.
- Efecto inmediato. Se descarta despuÃ©s de usarlo.

### 6.7 Partidario
- Solo 1 por turno.
- Efecto inmediato. Se descarta despuÃ©s de usarlo.

### 6.8 Estadio
- Solo 1 por turno.
- Permanece en juego en zona compartida.
- Si se juega otro Estadio, reemplaza al activo (el anterior se descarta).
- Solo puede haber 1 Estadio activo globalmente.

### 6.9 Herramienta PokÃ©mon
- Se une a un PokÃ©mon propio.
- MÃ¡ximo 1 herramienta por PokÃ©mon.
- Permanece unida hasta que el PokÃ©mon salga del juego.
- Al salir del juego: la herramienta se descarta junto al PokÃ©mon.

---

## 7. CONDICIONES DE VICTORIA Y DERROTA

### 7.1 Victoria por Premios
- Un jugador gana cuando toma su ÃšLTIMA carta de Premio.
- Se evalÃºa inmediatamente despuÃ©s de tomar cualquier carta de Premio.

### 7.2 Victoria por Knockout total
- Un jugador gana si derrota al PokÃ©mon Activo del rival
  y el rival NO tiene ningÃºn PokÃ©mon en Banca para reemplazarlo.
- Ocurre inmediatamente despuÃ©s de un Knockout.

### 7.3 Derrota por mazo vacÃ­o
- Un jugador pierde si intenta robar una carta al inicio de su turno
  y su mazo estÃ¡ vacÃ­o.
- Se evalÃºa al inicio del turno, antes de cualquier acciÃ³n.

### 7.4 Muerte SÃºbita (Sudden Death)
- Ocurre cuando ambos jugadores cumplen una condiciÃ³n de victoria
  al mismo tiempo (ejemplo: ambos toman su Ãºltima carta de Premio en el mismo momento).
- Se inicia una nueva partida con las mismas reglas, pero con 1 carta de Premio por jugador.
- Se repite hasta que haya exactamente 1 ganador.

### 7.5 Momentos de evaluaciÃ³n de victoria/derrota
Las condiciones se evalÃºan INMEDIATAMENTE en estos momentos:
- DespuÃ©s de un Knockout.
- DespuÃ©s de tomar cartas de Premio.
- Al inicio del turno (derrota por mazo vacÃ­o).

Si hay conflicto (ambos cumplen condiciÃ³n simultÃ¡neamente) â†’ se activa Muerte SÃºbita.
Una vez detectada una condiciÃ³n de victoria, la partida termina. No se continÃºa.

---

## 8. CONSTRUCCIÃ“N DE MAZOS

### 8.1 Fuente de cartas
- Solo se pueden usar cartas del set XY (set.id = "xy1") obtenidas
  de la API pokemontcg.io.
- No se pueden mezclar cartas de otros sets.

### 8.2 Validaciones obligatorias del mazo
- El mazo debe tener EXACTAMENTE 60 cartas.
- MÃ¡ximo 4 copias de cualquier carta con el mismo nombre.
  - EXCEPCIÃ“N: EnergÃ­a BÃ¡sica no tiene lÃ­mite de copias.
- El mazo debe tener AL MENOS 1 PokÃ©mon BÃ¡sico.
- Un mazo que no cumpla estas condiciones es INVÃLIDO y no puede usarse.

---

## 9. ESTADOS DEL JUEGO

El ciclo de vida de una partida sigue estos estados. El orden es fijo:

```
WAITING       â†’ esperando que se una un segundo jugador
     â†“
SETUP         â†’ preparaciÃ³n: mulligan, colocar PokÃ©mon, cartas de premio
     â†“
ACTIVE        â†’ partida en curso (ciclo de turnos)
     â†“
FINISHED      â†’ partida terminada con un ganador
```

Dentro de ACTIVE, cada turno sigue el ciclo:
```
DRAW â†’ MAIN â†’ ATTACK â†’ BETWEEN_TURNS â†’ (turno del oponente)
```

---

## 10. RESTRICCIONES GLOBALES DEL MOTOR DE JUEGO

- El backend NUNCA confÃ­a en datos enviados por el cliente. Toda acciÃ³n
  se valida contra el estado persistido en el servidor.
- El motor de juego NUNCA llama a la API externa durante una partida.
  Usa exclusivamente la cache local de cartas.
- El estado completo de la partida se persiste despuÃ©s de CADA acciÃ³n relevante.
- El log de acciones es INMUTABLE: solo se agregan entradas, nunca se modifican.
- Los PokÃ©mon de la Banca no pueden tener condiciones especiales.
- Un jugador no puede realizar una acciÃ³n fuera de su turno.
- Un jugador no puede realizar una acciÃ³n que ya realizÃ³ en el turno
  (energÃ­a, partidario, retirada, ataque).
- La Banca tiene un mÃ¡ximo estricto de 5 PokÃ©mon.
- Solo PokÃ©mon BÃ¡sicos pueden colocarse directamente desde la mano al tablero.
- Las cartas de Premio permanecen ocultas hasta que son tomadas como recompensa.

