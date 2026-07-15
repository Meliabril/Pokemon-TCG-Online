# Instrucciones para agentes de frontend

Alcance: `FE/**`.

## Lectura

- Para preguntas simples, explicaciones, diagnosticos o navegacion sin editar codigo: leer solo este archivo y abrir rules/skills solo si hace falta.
- Para cambios funcionales frontend: leer las rules frontend relevantes antes de editar.
- No duplicar en este archivo el detalle que vive en:
  - `FE/.ai/rules/frontend-architecture.md`
  - `FE/.ai/rules/angular-components.md`
  - `FE/.ai/rules/tailwind.md`
  - `FE/.ai/rules/clean-code.md`

## Reglas criticas

- Usar Angular y TypeScript con tipado estricto.
- Prohibido escribir HTML dentro de archivos `.ts`.
- Usar `templateUrl`.
- Priorizar Tailwind; usar CSS propio solo si hace falta.
- Mantener llamadas HTTP en services.
- Evitar `any`; usar `unknown` si el tipo todavia no esta claro.
- Mantener contratos claros con `interface` o `type`.
- Mantener componentes chicos y responsabilidades claras.
- Usar `inject()`, `input()`, `output()`, signals y `computed()` cuando encaje con el patron existente.
- Usar `ChangeDetectionStrategy.OnPush` en componentes nuevos o refactorizados.

## Skills del stack

- `FE/.ai/skills/angular-frontend/SKILL.md`: usar para features Angular.
- `FE/.ai/skills/component-refactor/SKILL.md`: usar para refactor de componentes.
- `FE/.ai/skills/tailwind-ui/SKILL.md`: usar para UI con Tailwind y estilos visuales.

Si existe una skill local/global mas especifica, se puede consultar como apoyo, pero no se copia al repo sin permiso.

## Validacion

- Para cambios funcionales frontend: `npm run build`.
- Para cambios solo de reglas o documentacion: no ejecutar build; verificar `git status --short`.
- Antes de cerrar, confirmar que no hubo cambios accidentales bajo `FE/src/` si la tarea no era funcional.
