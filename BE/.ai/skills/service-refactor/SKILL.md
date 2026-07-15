---
name: service-refactor
description: Usar al refactorizar services de backend, dividir clases grandes, corregir dependencias entre capas, extraer validadores o separar responsabilidades de negocio.
---

# Service Refactor

## Flujo

1. Leer `BE/.ai/rules/service-layer.md`.
2. Detectar responsabilidades mezcladas.
3. Separar por caso de uso, regla de negocio o dependencia.
4. Mantener contratos estables salvo pedido explicito.
5. Preservar comportamiento existente con tests.

## Reglas

- Extraer clases cuando reduzcan complejidad real.
- Mantener nombres de negocio.
- Evitar refactors esteticos no pedidos.
- No mover logica a controllers ni repositories.
