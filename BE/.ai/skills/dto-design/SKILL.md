---
name: dto-design
description: Usar al crear o ajustar DTOs, requests, responses, mappers y contratos de API del backend, cuidando claridad, compatibilidad y separacion entre entidad y contrato externo.
---

# DTO Design

## Flujo

1. Identificar si el dato es request, response o estructura interna.
2. Ubicar el DTO en `dtos/<dominio>`.
3. Mantener entidades fuera del contrato publico.
4. Actualizar mapper si corresponde.
5. Cubrir cambios de contrato con tests de controller o mapper.

## Reglas

- No exponer entidades JPA desde controllers.
- Evitar DTOs gigantes sin necesidad.
- Usar nombres claros con sufijos `RequestDto`, `ResponseDto` o `Dto`.
- No romper contratos existentes sin pedido explicito.
