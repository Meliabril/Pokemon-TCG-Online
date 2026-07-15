# Documentacion tecnica y academica

Este directorio organiza la documentacion humana general del Trabajo Practico Integrador Pokemon TCG.

Las reglas operativas para IA no viven aca. Para instrucciones de agentes usar `../AGENTS.md`, `../BE/AGENTS.md` y `../FE/AGENTS.md`.

## Lectura recomendada

1. [Estado actual del juego](./game/estado-actual-del-juego.md)
2. [Flujo actual de partida](./game/flujo-de-partida.md)
3. [Guia de prueba manual para dos jugadores](./game/guia-prueba-manual-dos-jugadores.md)
4. [Bugs conocidos y pendientes MVP](./game/bugs-conocidos-y-pendientes.md)
5. [Arquitectura general](./architecture/arquitectura-general.md)
6. [Instalacion y ejecucion](./architecture/instalacion-y-ejecucion.md)
7. [Decisiones tecnicas](./decisions/decisiones-tecnicas.md)
8. [Diagramas](./architecture/diagramas.md)
9. [Auditoria de documentacion](./DOCUMENTATION_AUDIT.md)

## Documentacion por subsistema

- [Backend](../BE/docs/README.md)
- [Frontend](../FE/docs/README.md)

## Organizacion

- `game/`: estado actual del juego, flujo de partida, prueba manual, bugs y reglas.
- `architecture/`: arquitectura general, instalacion, estado, flujo funcional y referencias de diagramas.
- `diagrams/`: diagramas Excalidraw para evaluacion tecnica y academica.
- `decisions/`: decisiones tecnicas humanas.
- `api/`: contratos HTTP/WebSocket y referencias de integracion.
- `sdd/`: especificaciones y trazabilidad vigentes o resumidas.

## Archivo historico

Los documentos obsoletos, vacios, duplicados o de handoff fueron movidos a `../documentation-archive`.

Ver [indice de documentos archivados](../documentation-archive/ARCHIVED_DOCS_INDEX.md).

## Advertencia de vigencia

En caso de conflicto, priorizar el codigo actual y los documentos de `docs/game/`. La documentacion archivada se conserva como evidencia historica, no como fuente normativa.
