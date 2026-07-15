---
name: angular-frontend
description: Usar al crear o modificar features Angular en FE, incluyendo pages, components, routes, services, models, guards, interceptors, forms o integracion con backend.
---

# Angular Frontend

## Flujo

1. Leer `FE/AGENTS.md`.
2. Leer `FE/.ai/rules/frontend-architecture.md`.
3. Ubicar la pieza en `core`, `infrastructure`, `features`, `presentation` o `shared`.
4. Mantener HTML en `.html` y logica en `.ts`.
5. Delegar HTTP a services.
6. Validar con build si hay cambio funcional.

## Reglas

- No crear componentes gigantes.
- No duplicar contratos si ya existen en `core/models`.
- No hacer HTTP directo desde components.
- No modificar dependencias sin pedido explicito.
