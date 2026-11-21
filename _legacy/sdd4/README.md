# _legacy/sdd4 · SDD-4 (archivio)

Metodo di lavoro usato dal 2026-04-27 al 2026-11-21: Spec-Driven Development in quattro sotto-fasi per ogni
fase della roadmap di `SPEC.md` §16 — PIANIFICA (`PLAN-fase-N.md`) → IMPLEMENTA → REVIEW (`REVIEW-fase-N.md`)
→ TEST (`TEST-PLAN-fase-N.md` + traceability matrix), con stop point utente e `AI_CONTEXT.md` come documento di
stato. Sostituito dal workflow aw, senza framework di sviluppo (`none`).

**Archivio, non regole attive:** consultalo come storia del progetto; non applicarlo e non proporlo.
Eccezione di contenuto: `plans/PLAN-fase-5.md` resta il riferimento per *cosa* manca a chiudere la Fase 5
(task 5.14–5.15, criteri A5.x), non per *come* lavorare.

| Contenuto | Origine |
|---|---|
| `CLAUDE-removed.md` | sezioni rimosse da `CLAUDE.md`, verbatim, con gli intervalli di righe originali |
| `README-removed.md` | sezione "Workflow Spec-Driven Development" rimossa da `README.md` (righe 286–295) |
| `AI_CONTEXT.md` | `AI_CONTEXT.md` alla radice: stato corrente, decisioni e note operative fino a F5 Task 5.13 |
| `plans/` | `plans/`: `PLAN-fase-0…5.md` (output di PIANIFICA) |
| `reviews/` | `reviews/`: `REVIEW-fase-0…4.5.md` (output di REVIEW) |
| `tests/TEST-PLAN-fase-*.md` | `tests/`: test plan per fase (output di TEST) |
| `tests/TRACEABILITY.md` | `tests/TRACEABILITY.md`: matrice cumulativa requisito → test |
| `memory/` | memoria di progetto di Claude Code: copia di `MEMORY.md` prima del ritiro e la nota `project_phase_transition_protocol.md` (rimandava a CLAUDE.md §7) |

Restano al loro posto `tests/visual-review/` (screenshot di riferimento) e tutti i documenti di prodotto
(`SPEC.md`, `ARCHITECTURE.md`, `CHANGELOG.md`, `CREDITS.md`, `README.md`, `scripts/`, `docs/`).

I riferimenti a questi file nella specifica, nell'architettura, nel CHANGELOG o nei commenti del codice sono
storia: puntano qui.

## Come tornare indietro

Lo spostamento è il commit "docs(claude): retire the SDD-4 method into _legacy/sdd4/"
(`git log -- _legacy/sdd4/README.md` lo trova), fatto con `git mv`: `git revert <hash>` riporta file, cartelle,
`CLAUDE.md` e `README.md` allo stato precedente. La nota di memoria va ricopiata a mano da `memory/` nella
cartella di memoria di Claude Code, ripristinandone la riga in `MEMORY.md`.
