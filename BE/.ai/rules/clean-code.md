# Clean Code backend

## Estilo Java

- Prohibido usar `var`.
- Usar nombres en ingles para clases, metodos, variables, logs y excepciones.
- Evitar ternarios complejos.
- Prohibidos ternarios anidados.
- Lambdas y streams estan permitidos solo si son faciles de leer para un dev Junior/Trainee.
- Si un stream requiere varias condiciones o transformaciones, preferir un loop claro.

## Diseno

- Evitar constructores gigantes.
- Evitar clases dios.
- Evitar metodos largos.
- Extraer constantes para numeros o strings usados en reglas.
- No dejar numeros magicos dentro de `if`, `for`, `while` o validaciones.
- Usar nombres expresivos antes que comentarios explicativos.
- Aplicar SOLID y Clean Code.

## Errores

- No silenciar excepciones.
- No capturar excepciones sin accion clara.
- Usar excepciones de dominio cuando exista una alternativa especifica.
