# Testing backend

## Criterio

Todo cambio funcional de backend debe tener una validacion proporcional al riesgo.

## Reglas

- Tests unitarios para reglas de negocio y services puros.
- Tests de controller para contratos HTTP.
- Tests de mappers, validadores y casos de uso cuando aporten cobertura real.
- Tests de integracion cuando el flujo cruce varias capas.
- Mantener nombres de tests descriptivos.
- No probar detalles internos si el comportamiento publico queda cubierto.

## Repositories

- No crear tests de repositories por defecto.
- No testear metodos heredados de Spring Data JPA.
- No usar `@DataJpaTest` salvo pedido explicito del equipo.
- Si una query o constraint necesita validacion, cubrirla desde un test de service o integracion cuando sea posible.

## Comandos

- Suite backend: `.\mvnw.cmd -q test`.
- Compilacion sin tests: `.\mvnw.cmd -q -DskipTests package`.

## Tareas no funcionales

Si solo se modifican reglas IA, documentacion o skills, no hace falta ejecutar tests.
