# Instrucciones generales para agentes

Este repositorio separa documentacion humana de reglas operativas para IA.

## Alcance

- `docs/` es solo documentacion humana del proyecto.
- `.ai/` contiene reglas y skills versionables para agentes que aplican al proyecto completo.
- `.ai/skills/` contiene skills transversales del proyecto, como revision de PRs que cruzan backend y frontend.
- `BE/docs/` es documentacion humana del backend.
- `FE/docs/` es documentacion humana del frontend.
- `BE/.ai/` contiene reglas y skills versionables para agentes de backend.
- `FE/.ai/` contiene reglas y skills versionables para agentes de frontend.
- No mezcles prompts, reglas operativas o checklists de IA dentro de `docs/`.

## Flujo obligatorio

1. Para tareas simples, explicaciones, diagnosticos o navegacion sin editar codigo, usar el contexto minimo necesario.
2. Leer `BE/AGENTS.md` solo si la tarea toca backend.
3. Leer `FE/AGENTS.md` solo si la tarea toca frontend.
4. Si la tarea toca ambos stacks, leer ambos archivos.
5. Usar `.ai/skills/project-pr-review/SKILL.md` solo para PRs, diffs, reviews o comparaciones de ramas.
6. Mantener el cambio dentro del alcance pedido.
7. No modificar codigo fuente, dependencias ni build cuando la tarea sea solo de documentacion, reglas o skills.

## Skills locales o globales

- Las skills instaladas fuera del repo pueden usarse como apoyo personal.
- No copiar, sobrescribir ni versionar skills locales/globales sin permiso explicito.
- Las skills transversales del equipo deben vivir dentro de `.ai/skills/`.
- Las skills especificas de stack deben vivir dentro de `BE/.ai/skills/` o `FE/.ai/skills/`.
- Si una skill externa resulta util, referenciarla en el `AGENTS.md` correspondiente y proponer su incorporacion despues.

## Validacion esperada

- Para cambios de reglas o documentacion, revisar `git status --short`.
- Confirmar que no se modificaron archivos bajo `BE/src/` o `FE/src/` salvo pedido explicito.
- Para cambios funcionales futuros, ejecutar la validacion indicada por el stack afectado.
