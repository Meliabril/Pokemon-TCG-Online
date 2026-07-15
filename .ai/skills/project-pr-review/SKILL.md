---
name: project-pr-review
description: Usar al revisar PRs, diffs, ramas o cambios propuestos del proyecto tpi-pokemon, especialmente cuando haya que validar arquitectura backend, estructura frontend, contratos, tests, builds o reglas de AGENTS.md.
---

# Project PR Review

## Flujo

1. Resolver el alcance de la revision: PR de GitHub, diff local, rama actual o comparacion contra la rama base.
2. Usar GitHub app para metadata y patches cuando este disponible; usar `gh` o `git` local para resolver rama actual, base branch o diffs que el conector no exponga.
3. Leer siempre `AGENTS.md`.
4. Clasificar archivos tocados:
   - `BE/**`: leer `BE/AGENTS.md` y sus reglas obligatorias.
   - `FE/**`: leer `FE/AGENTS.md` y sus reglas obligatorias.
   - documentacion, reglas o skills: revisar que no mezclen prompts/checklists en `docs/`.
5. Revisar el diff, no solo los nombres de archivos. Buscar regresiones de comportamiento, incumplimientos de estructura, dependencias incorrectas y cobertura faltante.
6. Reportar hallazgos primero, ordenados por severidad, con archivo/linea, riesgo concreto y cambio esperado.

## Backend

Revisar cambios bajo `BE/**` contra `BE/AGENTS.md` y:

- Respetar arquitectura por capas: `Controller -> Service`, `Service -> Service` solo si mejora responsabilidades, `Service -> Repository` solo hacia el repository propio.
- Marcar como hallazgo critico cualquier service que inyecte o use repositories de otro dominio o contrato. Si necesita cruzar dominios, debe depender de un contrato de service.
- Verificar controllers finos, sin repositories ni logica de negocio.
- Verificar que la logica viva en services, no en controllers ni repositories.
- Verificar inyeccion con `@RequiredArgsConstructor`, dependencias `private final` y sin field injection con `@Autowired`.
- Rechazar implementaciones concretas inyectadas cuando exista contrato o interface de service.
- Rechazar consultas directas en services: SQL, JPQL, `EntityManager` o `JdbcTemplate`.
- Revisar DTOs, mappers y contratos API: no exponer entidades JPA desde controllers.
- Revisar clean code Java: no `var`, no ternarios anidados, nombres expresivos, constantes para reglas y metodos chicos.
- Revisar CORS: debe estar centralizado salvo excepcion justificada en controller.
- Exigir tests proporcionales al cambio: services/reglas, controllers HTTP, mappers/validadores o integracion segun riesgo.

## Frontend

Revisar cambios bajo `FE/**` contra `FE/AGENTS.md` y:

- Mantener responsabilidades entre `core`, `infrastructure/api`, `features`, `presentation`, `shared` y `routes`.
- Rechazar HTML inline en `.ts`; usar `templateUrl` con `.html`.
- Usar CSS propio solo si Tailwind no resuelve el caso de forma mantenible.
- Evitar componentes grandes, templates con expresiones largas y logica compleja en templates.
- Delegar HTTP a services; no hacer llamadas HTTP directas desde components.
- Mantener tipado estricto: evitar `any`, usar `unknown` si el tipo no esta claro y definir `interface` o `type` para contratos.
- Reutilizar modelos compartidos en `core/models` y UI reutilizable en `shared`.
- Usar `inject()`, `input()`, `output()`, signals y `computed()` cuando correspondan al patron existente.
- Usar `ChangeDetectionStrategy.OnPush` en componentes nuevos o refactorizados.
- Revisar estados visuales, foco visible, contraste y responsive en cambios de UI.

## Validacion

- Si toca backend funcional, correr desde `BE`: `.\mvnw.cmd -q test`.
- Si toca frontend funcional, correr desde `FE`: `npm run build`.
- Si toca logica o tests frontend, correr desde `FE`: `npm test -- --watch=false --browsers=ChromeHeadless`.
- Si solo toca documentacion, reglas o skills, no correr suites; revisar `git status --short`.
- En tareas no funcionales, confirmar que no se modificaron archivos bajo `BE/src/` ni `FE/src/`.
- Si una validacion no puede ejecutarse por entorno, permisos, red o dependencia faltante, reportarlo como limitacion. No decir que paso.

## Salida Esperada

- Empezar con hallazgos. Si no hay hallazgos, decirlo explicitamente.
- Para cada hallazgo incluir severidad, archivo/linea, riesgo y accion esperada.
- Separar preguntas o supuestos despues de los hallazgos.
- Cerrar con validaciones ejecutadas y riesgos residuales.

Ejemplo de hallazgo:

`P1 - BE/src/.../SomeServiceImpl.java:24`: este service inyecta `OtherDomainRepository`, lo que rompe la regla de repository propio del dominio. Debe depender de un contrato de service del otro dominio o mover la operacion al service responsable.

## Errores Comunes

- No aprobar una PR solo porque compila si rompe capas, contratos o responsabilidad.
- No aceptar repositories extra en services como atajo de implementacion.
- No pedir tests de repositories por defecto; preferir tests de service o integracion cuando validen comportamiento real.
- No duplicar reglas operativas en `docs/`; usar `AGENTS.md` o `.ai/`.
- No ocultar fallos de validacion. Si un comando falla, incluir el comando y el motivo observable.
