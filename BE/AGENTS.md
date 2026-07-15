# Instrucciones para agentes de backend

Alcance: `BE/**`.

## Lectura

- Para preguntas simples, explicaciones, diagnosticos o navegacion sin editar codigo: leer solo este archivo y abrir rules/skills solo si hace falta.
- Para cambios funcionales de backend: leer las rules backend relevantes antes de editar.
- No duplicar en este archivo el detalle que vive en:
  - `BE/.ai/rules/backend-architecture.md`
  - `BE/.ai/rules/service-layer.md`
  - `BE/.ai/rules/clean-code.md`
  - `BE/.ai/rules/testing.md`

## Reglas criticas

- Respetar arquitectura por capas estricta.
- Mantener controllers finos; la logica de negocio vive en services.
- Usar repositories solo desde el service del dominio correspondiente.
- Inyectar contratos/interfaces; no depender de implementaciones concretas si existe contrato.
- Anotar con `@Service` (no `@Component`) toda clase de la capa de services que tenga logica de negocio, incluidas las implementaciones de un contrato/interface usadas como estrategias (por ejemplo, las implementaciones de `AttackEffect`). Reservar `@Component` para helpers tecnicos o de infraestructura sin reglas de negocio.
- Usar `@RequiredArgsConstructor` y dependencias `private final`; no usar field injection.
- No usar `var` ni ternarios anidados.
- CORS debe estar centralizado por defecto; usar `@CrossOrigin` en controllers solo como excepcion justificada.
- Agregar tests proporcionales al riesgo cuando el cambio sea funcional.

## Skills del stack

- `BE/.ai/skills/dto-design/SKILL.md`: usar para DTOs, requests, responses, mappers y contratos de API.
- `BE/.ai/skills/spring-backend/SKILL.md`: usar para features Spring Boot.
- `BE/.ai/skills/service-refactor/SKILL.md`: usar para refactor de services.

Si existe una skill local/global mas especifica, se puede consultar como apoyo, pero no se copia al repo sin permiso.

## Validacion

- Para cambios funcionales de backend: `.\mvnw.cmd -q test`.
- Para cambios solo de reglas o documentacion: no ejecutar tests; verificar `git status --short`.
- Antes de cerrar, confirmar que no hubo cambios accidentales bajo `BE/src/` si la tarea no era funcional.
