# Componentes Angular

## Reglas obligatorias

- Prohibido escribir HTML dentro de archivos `.ts`.
- Usar `templateUrl` con archivo `.html`.
- Usar `styleUrl` con archivo `.css` solo si el componente necesita estilos propios.
- Priorizar Tailwind para estilos simples.
- Evitar logica compleja en templates.
- Evitar componentes grandes.
- Si un componente crece demasiado, dividirlo en componentes hijos.

## Angular moderno

- Usar componentes standalone.
- No declarar `standalone: true` si Angular ya lo asume por defecto.
- Usar `inject()` para dependencias.
- Usar `input()` y `output()` para entradas y salidas.
- Usar signals y `computed()` cuando simplifiquen el estado.
- Usar `ChangeDetectionStrategy.OnPush` en componentes nuevos o refactorizados.
- Usar control flow moderno: `@if`, `@for`, `@switch`.

## Forms

- Preferir Reactive Forms para formularios con validacion.
- Mantener validaciones complejas fuera del template.
- Tipar los datos enviados al backend.
