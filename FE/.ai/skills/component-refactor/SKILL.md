---
name: component-refactor
description: Usar al simplificar componentes Angular grandes, extraer componentes hijos, reducir logica de templates, separar responsabilidades o mejorar legibilidad sin cambiar comportamiento.
---

# Component Refactor

## Flujo

1. Leer `FE/.ai/rules/angular-components.md`.
2. Identificar responsabilidades mezcladas.
3. Extraer componentes hijos cuando el componente padre sea dificil de leer.
4. Mantener inputs y outputs tipados.
5. Conservar comportamiento visual y funcional.

## Reglas

- No mover HTML al `.ts`.
- No introducir estado global si el estado local alcanza.
- No refactorizar de mas.
- Mantener nombres de componentes claros y especificos.
