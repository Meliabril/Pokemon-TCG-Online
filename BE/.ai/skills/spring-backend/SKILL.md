---
name: spring-backend
description: Usar al crear o modificar features de backend Spring Boot en BE, especialmente controllers, services, repositories, DTOs, mappers, validaciones, seguridad o endpoints REST/WebSocket.
---

# Spring Backend

## Flujo

1. Leer `BE/AGENTS.md`.
2. Leer `BE/.ai/rules/backend-architecture.md`.
3. Identificar dominio, contrato de service y repository propio.
4. Mantener controllers finos y delegar a services.
5. Usar DTOs para entrada y salida.
6. Verificar inyeccion con `@RequiredArgsConstructor` y dependencias `private final`.
7. Revisar si CORS queda cubierto por configuracion global antes de agregar `@CrossOrigin`.
8. Agregar tests proporcionales al cambio funcional.

## Reglas

- No cruzar repositories entre dominios.
- No inyectar implementaciones concretas si existe interface.
- No usar field injection con `@Autowired`.
- No agregar consultas en services.
- No agregar `@CrossOrigin` en controllers salvo excepcion justificada.
- No modificar dependencias sin pedido explicito.
