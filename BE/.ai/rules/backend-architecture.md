# Arquitectura backend

## Regla base

El backend usa arquitectura por capas estricta. Cada clase debe tener una responsabilidad clara y respetar el flujo permitido.

## Flujo permitido

- `Controller -> Service`
- `Service -> Service` solo cuando sea necesario para separar un caso de uso o reutilizar una regla de negocio.
- `Service -> Repository` solo hacia el repository propio del contrato o dominio trabajado.

## Prohibiciones

- Un controller no debe llamar repositories.
- Un service no debe llamar repositories de otro dominio o contrato.
- Un repository no debe contener logica de negocio.
- No crear atajos entre capas para ahorrar archivos.
- No mezclar reglas de juego, persistencia y presentacion HTTP en una misma clase.

## Contratos

- Inyectar interfaces de services cuando existan.
- No inyectar implementaciones concretas si existe contrato.
- La logica debe vivir en la implementacion del service.
- Los contratos deben describir operaciones de negocio, no detalles tecnicos de persistencia.

## Inyeccion de dependencias

- Usar `@RequiredArgsConstructor` de Lombok para inyeccion por constructor.
- Declarar dependencias inyectadas como `private final`.
- No escribir constructores manuales para inyeccion si Lombok resuelve el caso.
- No usar field injection con `@Autowired`.
- Si una clase necesita demasiadas dependencias, no ocultar el problema con Lombok: revisar responsabilidades.
- Permitir constructor manual solo si hay una razon tecnica clara, como `@Value`, inicializacion especial o configuracion de infraestructura.

## CORS

- La politica general de CORS debe vivir centralizada en configuracion Spring o Security.
- Usar `@CrossOrigin` en controllers solo como excepcion explicita y justificada.
- No duplicar origins, headers o methods si ya estan definidos en la configuracion global.
- No abrir CORS con `*` cuando haya credenciales, JWT, cookies o datos sensibles.
- Antes de agregar CORS a un controller, revisar la configuracion global existente.

## Ubicacion esperada

- Controllers: `controllers/<dominio>`.
- Services: contrato en `services/<dominio>` e implementacion en `services/<dominio>/impl`.
- DTOs: `dtos/<dominio>`.
- Repositories: `repositories`.
- Mappers: `mappers`.
- Configuracion: `configs`.
