# Explicacion simple para defensa

## 1. Login y Register: hay algun patron?

Si, hay una forma de trabajo bastante ordenada y repetida entre ambos.

Mas que un "patron formal" super academico, lo que se ve es una combinacion de ideas:

- `Login` y `Register` usan la misma base visual: ambos se montan sobre `AuthShellComponent`.
- Cada pantalla tiene su propia responsabilidad:
  - `login-page` maneja iniciar sesion y recuperar contrasena.
  - `register-page` maneja crear cuenta.
- La logica de comunicacion con backend no esta metida en el HTML, sino en `AuthApiService`.
- Los formularios usan validaciones antes de pegarle al backend.

### Como lo explicaria en una defensa

"En autenticacion usamos una estructura reutilizable. No hicimos dos pantallas totalmente separadas desde cero, sino que compartimos una misma carcasa visual y cada pagina se encarga solo de su flujo. Eso hace que el codigo sea mas prolijo, mas mantenible y mas facil de escalar."

### Que patron se puede nombrar

Se puede defender como una mezcla de:

- **Reutilizacion por componente base**: `AuthShellComponent` funciona como una plantilla visual compartida.
- **Separacion de responsabilidades**: la vista muestra, el componente decide, y el servicio habla con el backend.
- **Patron de flujo por estados** en login: el login no es solo "entrar", tambien pasa por estados como:
  - login
  - recuperar contrasena
  - verificar codigo
  - cambiar contrasena

O sea: el sistema va cambiando de "pantalla interna" segun en que paso esta el usuario.

### En resumen

Si te lo preguntan corto:

"Si, hay un patron. Login y Register comparten una misma estructura visual y siguen separacion de responsabilidades: formulario, validacion, servicio y navegacion. En login, ademas, hay una logica por estados para manejar recuperacion de contrasena paso a paso."

---

## 2. Como estan hechas las habilidades? Usan algun patron?

Si, tambien siguen una idea bastante clara.

Las habilidades no estan hechas como un bloque unico gigante, sino como una logica que:

1. reconoce que habilidad se quiere usar,
2. decide que datos necesita,
3. arma la interfaz para pedirle eso al jugador,
4. construye el payload final para enviarlo al backend.

### Que hace el sistema concretamente

Hay un helper que revisa el `id` de la habilidad y, segun cual sea, devuelve que necesita esa habilidad:

- si necesita elegir un Pokemon rival,
- si necesita elegir una energia,
- si necesita una carta de la mano,
- si necesita una carta del mazo.

Despues, con esa informacion, la UI muestra las opciones correctas.

### Que patron se puede decir

La forma mas defendible es decir que usa una mezcla de:

- **Strategy simple / por caso**: cada habilidad tiene su propia forma de resolverse.
- **Tabla de decisiones**: segun el codigo de habilidad, el sistema sabe que pedir.
- **Builder de payload**: una vez que el jugador eligio todo, se arma el objeto final para ejecutar la accion.

No parece un motor 100% generico de habilidades. Mas bien es un sistema guiado por reglas concretas por cada habilidad conocida.

### Caso especial importante

Hay una habilidad, `Fairy Transfer`, que tiene incluso un modal propio. Eso muestra que:

- hay una base general para habilidades,
- pero si una habilidad necesita una interaccion mas especial, se le da un componente especifico.

Eso esta bueno para defender porque muestra flexibilidad.

### Como lo explicaria oralmente

"Las habilidades estan modeladas con una logica por casos. El sistema identifica la habilidad y, segun su tipo, define que elecciones tiene que hacer el jugador antes de ejecutarla. No es una logica hardcodeada toda mezclada en la vista, sino una capa que prepara la seleccion y despues arma la accion final."

### En resumen

Si te lo preguntan corto:

"Si, usan un patron. Se resuelven con una estrategia por tipo de habilidad: cada habilidad define que seleccion necesita, la interfaz muestra esas opciones y luego se arma el payload para ejecutar la accion."

---

## 3. Como funciona el hover para que parezca que el rival mueve las cartas?

Esto es de las partes mas interesantes del front.

No es solamente un `:hover` de CSS local.

Lo que pasa es esto:

1. un jugador pone el mouse sobre una carta,
2. el frontend detecta ese hover,
3. manda un evento visual,
4. el otro cliente recibe ese evento,
5. en el tablero rival se marca esa misma carta como "hovered",
6. CSS le aplica una transformacion visual para que parezca que se levanta o se mueve.

### O sea, que se sincroniza?

Se sincroniza **el evento visual**, no el mouse literal.

El sistema no esta transmitiendo "la posicion exacta del cursor".
Lo que transmite es algo como:

- zona de tablero,
- de quien es la carta,
- indice o id visual de la carta,
- si entro o salio el hover.

Con eso, el otro cliente sabe que carta tiene que resaltar.

### Por que se ve como que el rival mueve las cartas?

Porque cuando el otro lado recibe ese evento:

- guarda cual es la carta remarcada,
- le agrega una clase visual,
- esa clase aplica `transform`, `scale`, sombra y brillo.

Entonces visualmente parece que la carta "sube" igual que cuando vos mismo le haces hover en tu pantalla.

### Que patron hay aca?

Aca se puede defender muy bien como:

- **Observer / evento**: una accion visual genera un evento que otro modulo escucha.
- **Estado centralizado**: la fachada del juego guarda cual carta remota esta hovered.
- **Presentacion por CSS**: el movimiento final lo hace el estilo visual, no una logica pesada.

### Lo importante para decir

"El efecto del rival no es una animacion inventada aparte. Es la misma idea visual del hover local, pero sincronizada como evento entre clientes. Uno emite el cambio de hover y el otro lo reproduce visualmente marcando la carta correspondiente."

### En resumen

Si te lo preguntan corto:

"El hover del rival funciona porque el front sincroniza un evento visual de carta seleccionada o enfocada. Cuando el otro cliente lo recibe, aplica una clase CSS a esa carta y la levanta visualmente, dando la sensacion de que el rival esta moviendo sus cartas."

---

## 4. Respuesta corta para defenderte rapido

### Login/Register

"Siguen una estructura reutilizable: comparten una misma base visual, validan formularios en frontend y delegan la comunicacion con backend a servicios. Login, ademas, esta manejado como un flujo por estados para cubrir recuperacion de contrasena."

### Habilidades

"Las habilidades estan hechas con una estrategia por casos. Segun la habilidad, el sistema decide que necesita pedirle al jugador y luego arma la accion final para ejecutarla."

### Hover rival

"No es solo CSS local: el hover se manda como evento visual al otro cliente, que resalta la misma carta con transformaciones y sombras para simular que el rival la esta moviendo."

---

## 5. Si queres nombrar patrones sin sonar demasiado tecnico

- **Componente reutilizable** en login/register.
- **Separacion de responsabilidades** entre vista, logica y servicio.
- **Flujo por estados** en recuperacion de contrasena.
- **Strategy por casos** en habilidades.
- **Observer o eventos** para el hover sincronizado.

---

## 6. Conclusion

El frontend esta bastante bien organizado para defenderlo:

- autenticacion con estructura reutilizable,
- habilidades con logica por tipo de accion,
- hover remoto resuelto como evento visual sincronizado.

Eso demuestra que no solo se penso en "que funcione", sino tambien en orden, mantenimiento y experiencia de usuario.
