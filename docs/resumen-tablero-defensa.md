# Resumen simple del tablero

## Idea general

El tablero esta armado como una composicion de partes:

- la mano,
- el Pokemon activo,
- la banca,
- las zonas de cartas,
- una capa de animaciones,
- y una logica central que coordina todo.

Esa logica central vive en `game-board`, que recibe el estado del juego y decide que se puede hacer en cada momento.

En otras palabras:

"El tablero no es un bloque unico. Es un conjunto de componentes especializados, coordinados por un componente principal que interpreta el estado de la partida."

---

## 1. Como funciona el drag and drop

El drag and drop no esta hecho "a ojo". Esta bastante controlado.

### Como pensarlo simple

Cuando arrastras una carta, el sistema primero identifica **que tipo de carta es** y **que accion puede hacer**.

Por ejemplo:

- si es un Pokemon basico, puede ir al activo o a la banca en setup;
- si es una energia, puede ir sobre un Pokemon valido;
- si es una evolucion, solo puede caer sobre un Pokemon que pueda evolucionar;
- si es un trainer, puede tener su propia zona o un objetivo concreto.

### Que hace el tablero

El `game-board` guarda temporalmente que se esta arrastrando:

- si es carta para activo,
- si es energia,
- si es evolucion,
- si es trainer,
- o si se esta moviendo un Pokemon desde la banca.

Con eso habilita solo los destinos validos.

### Por que esto esta bueno

Porque el usuario no puede soltar la carta en cualquier lado. El tablero muestra y acepta solo los lugares coherentes con la jugada.

### Como defenderlo

"El drag and drop esta guiado por reglas del juego. No se arrastra libremente cualquier carta a cualquier lugar, sino que el sistema identifica el tipo de accion y habilita solo los objetivos validos."

---

## 2. Como funcionan las animaciones cuando se despliegan las cartas

Las animaciones no estan mezcladas dentro de cada carta. Hay una **capa de animacion separada**.

### Que significa eso

Cuando pasa algo importante:

- robar carta,
- mover carta,
- revelar mano,
- ataque,
- mulligan,
- moneda,

no se anima directamente "la carta real" del tablero, sino que se usa una capa visual encima del tablero para mostrar el movimiento.

### Beneficio

Esto hace que:

- el tablero siga ordenado,
- las animaciones sean mas controlables,
- y no se rompa la estructura real de las zonas mientras algo se mueve.

### Ejemplo simple

Si una carta va del mazo a la mano:

1. se detecta el punto de salida,
2. se detecta el punto de llegada,
3. se crea una carta flotante temporal,
4. esa carta hace el recorrido visual,
5. al terminar, desaparece y queda el estado real actualizado.

### Como defenderlo

"Las animaciones estan desacopladas del tablero. Hay una capa visual que reproduce movimientos entre zonas, asi la interfaz puede animar sin ensuciar la logica real del juego."

---

## 3. Como funciona la banca

La banca esta modelada como una fila de slots.

Cada slot puede:

- estar vacio u ocupado,
- aceptar drops validos,
- mostrar el Pokemon,
- mostrar energia,
- mostrar HP,
- mostrar estados especiales,
- permitir seleccion o inspeccion.

### Que puede pasar en la banca

- En setup, puede recibir Pokemon basicos desde la mano.
- Durante la partida, puede recibir energias si corresponde.
- Puede recibir evoluciones si ese Pokemon es objetivo valido.
- Puede recibir ciertos trainers, como herramientas.
- Desde la banca se puede seleccionar un Pokemon.
- En algunos casos se puede arrastrar desde la banca al activo, por ejemplo para retreat o promocion.

### Idea importante

La banca no es solo "cinco espacios dibujados". Cada espacio sabe si puede recibir algo y que tipo de interaccion permite.

### Como defenderlo

"La banca esta hecha como una fila de slots inteligentes. Cada slot sabe si esta ocupado, si acepta una carta, si se puede seleccionar y que informacion del Pokemon tiene que mostrar."

---

## 4. Como funciona el setup

El setup es un modo especial del tablero.

### Que cambia en setup

Cuando el juego esta en estado `Setup`, la UI cambia:

- aparecen ayudas especificas,
- aparecen botones para confirmar setup,
- se puede limpiar la banca elegida,
- se limita el tipo de cartas que se pueden arrastrar,
- y se exige elegir un Pokemon activo antes de confirmar.

### Logica del setup

En setup el jugador:

1. elige un Pokemon basico como activo,
2. puede mandar otros basicos a la banca,
3. cuando la seleccion esta bien, confirma el setup.

Si todavia no eligio activo, el tablero no deja confirmar.

### Relacion con mulligan

El setup tambien esta muy conectado con el mulligan:

- si no hay mano inicial valida, entra el flujo de mulligan;
- el tablero muestra avisos y contadores;
- y cuando termina ese flujo, vuelve a habilitar la seleccion inicial normal.

### Como defenderlo

"El setup funciona como un modo especial del tablero. En esa etapa no se juega normalmente, sino que el sistema restringe acciones para que el jugador primero arme su posicion inicial: un activo y, si quiere, banca valida."

---

## 5. Como funciona el Pokemon activo

El Pokemon activo tiene su propio componente porque es una zona muy importante.

### Que muestra

El slot del activo muestra:

- la carta principal,
- energias unidas,
- resumen de energias,
- HP restante,
- cambios de vida,
- condiciones especiales,
- escudos o efectos visuales,
- y algunas herramientas equipadas.

### Que permite

El activo puede:

- recibir drops validos,
- ser inspeccionado,
- ser seleccionado,
- recibir energia,
- recibir evolucion,
- recibir ciertos trainers,
- y ser destino de promocion o retiro segun el caso.

### Diferencia con la banca

La banca es una fila de opciones.
El activo es el centro operativo del turno.

Por eso tiene mas protagonismo visual y mas logica asociada.

### Como defenderlo

"El Pokemon activo esta tratado como una zona principal del tablero. Tiene mas informacion visible y mas interacciones porque concentra gran parte de las acciones del turno."

---

## 6. Respuesta oral corta

Si te lo preguntan rapido, podes decir algo asi:

"El tablero esta dividido en componentes especializados: mano, activo, banca y zonas de recursos. El `game-board` central coordina todo segun el estado de la partida. El drag and drop esta guiado por reglas del juego, las animaciones corren en una capa visual separada, la banca funciona como una fila de slots inteligentes, el setup es un modo especial para armar la posicion inicial y el Pokemon activo es la zona principal donde se concentran las acciones mas importantes."

---

## 7. Version muy breve para defenderte

- **Drag and drop**: solo habilita destinos validos segun la carta y la accion posible.
- **Animaciones**: usan una capa visual aparte para mover cartas sin romper el tablero real.
- **Banca**: es una fila de slots que saben si estan ocupados, si aceptan drops y que mostrar.
- **Setup**: es un modo especial para elegir activo y banca antes de empezar.
- **Pokemon activo**: es la zona central de juego, con mas info y mas interacciones.

---

## 8. Conclusion

El tablero esta bien defendible porque mezcla dos cosas:

- orden visual por componentes,
- y logica de juego controlada por estado.

O sea: no solo se ve como un tablero, sino que realmente se comporta segun las reglas de la partida.
