# AI Context

> File di stato corrente del progetto.
> Da aggiornare ad ogni task completato (CLAUDE.md §2.2).

## Stato corrente

- **Branch corrente**: `feature/1-domain-and-rules` (staccato da `develop`, GitFlow leggero).
- **Fase roadmap**: Fase 1 — Dominio e regole (`shared`).
- **Sotto-fase**: IMPLEMENTA. PIANIFICA chiusa il 2026-04-28 con `plans/PLAN-fase-1.md` approvato.
- **Ultimo task completato**: Task 1.1 — FID notation utility (`Square` minimal + `FidNotation` con bijezione 1↔32, parsing/format mosse, ParsedMove record). 59 test verdi.
- **Prossimo task**: Task 1.2 — Modello di dominio completo (`Color`, `PieceKind`, `Piece`, `Board`, `Move` sealed + `SimpleMove` + `CaptureSequence`, `GameStatus` 6 voci, `GameState`).
- **Ultimo commit (su `develop`)**: `e4f788d docs(claude): add PLAN for Fase 1 (domain + RuleEngine)`.
- **mvn clean verify**: BUILD SUCCESS in ~50s (parent + 4 moduli).
- **Smoke test eseguiti**: 1 per modulo, tutti verdi.
- **JaCoCo report**: presenti in `target/site/jacoco/` per tutti i moduli.
- **SpotBugs**: 0 warning High.
- **Acceptance A0.1 ÷ A0.5, A0.8 ÷ A0.11**: verificati ✓.
- **Acceptance A0.6 (CI verde)**: marcata **deferred** in ADR-019 (no repository remoto disponibile; workflow disattivato).
- **Acceptance A0.7 (docker compose up)**: marcata **superata** in ADR-018 (Docker rimosso; il developer usa MySQL locale).
- **Piano di riferimento**: [`plans/PLAN-fase-0.md`](plans/PLAN-fase-0.md).

## Decisioni recenti

- Approvate dall'utente in data 2026-04-27 (Task PIANIFICA Fase 0):
  - Spotless: indentazione **2 spazi** (Google standard, conforme a NFR-M-04).
  - Maven `groupId`: `com.damaitaliana`.
  - Tag chiusura Fase 0: `v0.0.0`.
- Revisione post-implementazione (2026-04-27, dopo feedback utente):
  - **ADR-018**: Docker Compose rimosso. L'ambiente di sviluppo usa il MySQL locale dell'utente (porta 3306, gestito via Workbench/DBeaver).
  - **ADR-019**: workflow GitHub Actions rinominato in `ci.yml.disabled` (non eseguibile). Verrà riattivato quando un repository remoto sarà disponibile.
  - `application.yml` server: default `jdbc:mysql://localhost:3306/dama_italiana`.
- Adozione **GitFlow leggero** (2026-04-28): `main` = production / tag, `develop` = integrazione e default branch GitHub, branch effimeri `feature/<fase>-<topic>` e `fix/review-N-F-<id>` staccati da `develop`, mergiati `--no-ff`. Tag delle fasi (`v0.<fase>.0`) sul commit di merge in `main`. Aggiornato `CLAUDE.md` §4.3-§4.4.

## SPEC clarifications needed

Nessuna al momento.

## Note operative

- Rappresentazione del board (FEN-like vs explicit) per il corpus regole: **decisione rinviata alla Fase 1** (CLAUDE.md §2.4.4).
- I test smoke `<Modulo>SmokeTest` introdotti in Fase 0 vanno **rimossi** appena ogni modulo ha test reali (Fase 1 per `shared`, Fase 4 per `core-server`, Fase 3 per `client`, Fase 5 per `server`).
- JaCoCo `haltOnFailure=false` in Fase 0; passa a `true` con soglie reali a partire dalla Fase 1.
