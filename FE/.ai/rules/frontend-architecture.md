# Arquitectura frontend

## Regla base

El frontend debe mantener responsabilidades separadas y una estructura facil de entender para un equipo Junior/Trainee.

## Carpetas principales

- `core`: constantes, guards, interceptors, modelos compartidos, storage y servicios globales.
- `infrastructure/api`: services que llaman al backend.
- `features`: pantallas y funcionalidades del usuario.
- `presentation`: layouts y navegacion de nivel aplicacion.
- `shared`: UI y utilidades reutilizables sin dominio propio.
- `routes`: configuracion principal de rutas.

## Reglas

- Una page coordina estado visual y composicion, no debe concentrar toda la logica.
- Un component debe tener una responsabilidad clara.
- Las llamadas HTTP van en services.
- Los modelos compartidos van en `core/models`.
- La UI reutilizable va en `shared`.
- La UI que pertenece a una feature se queda dentro de esa feature.
