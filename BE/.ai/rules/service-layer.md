# Capa de services

## Responsabilidad

Los services contienen casos de uso y reglas de negocio. Deben coordinar validaciones, repositorios propios, mappers y otros services cuando haga falta.

## Reglas obligatorias

- Mantener metodos chicos y con nombres de negocio.
- Usar `@RequiredArgsConstructor` para inyeccion de dependencias.
- Declarar dependencias como `private final`.
- Inyectar services por contrato o interface cuando exista.
- Delegar persistencia al repository correspondiente.
- No escribir SQL, JPQL, `EntityManager` ni `JdbcTemplate` dentro de services.
- No consultar repositories de otros dominios.
- Usar otros services para cruzar dominios cuando sea necesario.
- Evitar services con demasiadas responsabilidades.
- Separar validadores, factories o helpers cuando una regla crece.
- No usar `@Autowired` sobre campos.
- Anotar con `@Service` las clases de la capa de services con logica de negocio, incluidas las implementaciones de un contrato usadas como estrategias (por ejemplo las de `AttackEffect`). Usar `@Component` solo para helpers tecnicos sin reglas de negocio (lectores, factories tecnicas, adaptadores).

## Dependencias permitidas

- Repository propio del dominio.
- Mapper propio o compartido.
- Service de otro dominio solo a traves de su contrato.
- Componentes tecnicos de Spring solo si pertenecen al caso de uso.

## Dependencias prohibidas

- Controller.
- Implementacion concreta de otro service.
- Repository de otro dominio.
- Acceso directo a base de datos fuera de repositories.
- Field injection con `@Autowired`.
