# Diagramas

Los diagramas vigentes se guardan en `docs/diagrams` en formato Excalidraw. Se pueden abrir en https://excalidraw.com o con una extension compatible de IDE.

## Diagramas generados

1. [Arquitectura general](../diagrams/arquitectura-general.excalidraw)
   - Relacion entre usuarios, Angular, REST, STOMP, backend, base de datos y Pokemon TCG API.
2. [Flujo de partida](../diagrams/flujo-partida.excalidraw)
   - Secuencia desde mazo activo hasta fin de partida.
3. [Prueba manual dos jugadores](../diagrams/prueba-manual-dos-jugadores.excalidraw)
   - Swimlane simplificada para validar Jugador 1, Jugador 2 y backend.

## Diagramas evaluados y no generados

- Flujo de snapshots detallado: no se genero como diagrama separado porque queda cubierto por arquitectura general y prueba manual. Si la sincronizacion sigue fallando, conviene crear un diagrama especifico de `STATE_SYNC`.
- Modelo de dominio completo: no se genero ahora para evitar un diagrama demasiado grande. La explicacion textual esta en `BE/docs/modelo-de-dominio.md` y `BE/docs/motor-arquitectura-detallada.md`.

## Criterio usado

Se generaron solo diagramas que ayudan a evaluadores y desarrolladores a entender el estado del MVP y la prueba manual. Los diagramas historicos o placeholders se archivaron en `documentation-archive`.
