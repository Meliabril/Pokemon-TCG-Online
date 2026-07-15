# Auditoria de documentacion

Fecha de auditoria: 2026-06-26.

## Alcance revisado

- `docs/`
- `BE/docs/`
- `FE/docs/`
- Archivos `.md` y `.txt` relevantes fuera de esas carpetas.

No se auditaron dependencias externas como `node_modules` ni reportes generados de `target`.

## Diagnostico

La documentacion util estaba distribuida en tres ubicaciones principales, pero tambien habia archivos sueltos en `BE/`, `FE/`, `BE/src` y `FE/src`. Se detectaron documentos vigentes, handoffs historicos, prompts de IA, placeholders vacios y documentacion con estado obsoleto.

## Documentacion vigente conservada

- Documentacion general en `docs/architecture`, `docs/decisions`, `docs/api` y `docs/sdd`.
- Documentacion backend principal en `BE/docs`.
- Documentacion frontend principal en `FE/docs`.
- Swagger/docs API generados en `BE/docs/api_doc`.
- Assets academicos en `BE/docs/assets`.

## Documentacion util reubicada

| Ruta original | Nueva ruta | Motivo |
| --- | --- | --- |
| `BE/ENGINE_ARCHITECTURE.md` | `BE/docs/motor-arquitectura-detallada.md` | Documento tecnico vigente del motor; correspondia a backend docs. |
| `BE/rules_pokemon_tcg.md` | `docs/game/reglas-pokemon-tcg.md` | Reglas de juego utiles para explicar el dominio; correspondia a docs generales del juego. |
| `FE/ESTRUCTURA_FRONTEND.md` | `FE/docs/estructura-frontend.md` | Guia vigente de estructura frontend; correspondia a FE docs. |

## Problemas corregidos

- Se centralizo material historico en `documentation-archive`.
- Se separaron bugs conocidos de pendientes MVP.
- Se creo documentacion especifica para estado del juego, flujo de partida y prueba manual.
- Se actualizaron indices para que programadores y evaluadores encuentren la documentacion vigente.
- Se corrigieron contradicciones sobre STOMP y tablero frontend: ya existen, pero el flujo sigue parcial por validacion y bugs.

## Estructura final

```text
docs/
  README.md
  DOCUMENTATION_AUDIT.md
  architecture/
  api/
  decisions/
  diagrams/
  game/
  sdd/
BE/docs/
FE/docs/
documentation-archive/
  ARCHIVED_DOCS_INDEX.md
```

## Observaciones

- Algunos documentos historicos tienen mojibake/encoding deteriorado. Se conservaron en archivo para no perder informacion.
- El estado PostgreSQL/H2 queda como contradiccion a resolver antes de entrega final.
- Las pruebas funcionales no se ejecutaron en esta fase porque el trabajo fue documental.
