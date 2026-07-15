# Clean Code frontend

## TypeScript

- Usar tipado estricto.
- Evitar `any`.
- Usar `unknown` cuando el tipo sea incierto.
- Crear `interface` o `type` para contratos claros.
- Evitar funciones grandes.
- Evitar nombres genericos como `data`, `item` o `value` cuando el dominio permita un nombre mejor.

## Componentes

- Mantener componentes chicos.
- Separar responsabilidades entre page, component, service y modelo.
- Mover logica reutilizable a services o utilidades.
- No hacer llamadas HTTP en componentes.
- Evitar templates con expresiones largas.

## Estado

- Mantener transformaciones puras y predecibles.
- Usar signals para estado local si encaja con el patron existente.
- Evitar efectos secundarios escondidos en getters o templates.
