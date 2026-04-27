# AI Context

> File di stato corrente del progetto.
> Da aggiornare ad ogni task completato (CLAUDE.md §2.2).

## Stato corrente

- **Fase roadmap**: Fase 0 — Setup infrastruttura.
- **Sotto-fase**: TEST chiusa il 2026-04-28. Tutte e 4 le sotto-fasi della Fase 0 completate.
- **Ultimo task completato**: sotto-fase TEST della Fase 0. Creato `tests/TEST-PLAN-fase-0.md` (sintetico: nessun FR/NFR coperto in fase infrastrutturale; 4 smoke test verdi; TRACEABILITY vuota e coerente).
- **Prossimo passo**: chiusura Fase 0 → tag git `v0.0.0` (in attesa conferma utente) → PIANIFICA Fase 1 (dominio + RuleEngine).
- **Ultimo commit**: in corso (revisione post-feedback).
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

## SPEC clarifications needed

Nessuna al momento.

## Note operative

- Rappresentazione del board (FEN-like vs explicit) per il corpus regole: **decisione rinviata alla Fase 1** (CLAUDE.md §2.4.4).
- I test smoke `<Modulo>SmokeTest` introdotti in Fase 0 vanno **rimossi** appena ogni modulo ha test reali (Fase 1 per `shared`, Fase 4 per `core-server`, Fase 3 per `client`, Fase 5 per `server`).
- JaCoCo `haltOnFailure=false` in Fase 0; passa a `true` con soglie reali a partire dalla Fase 1.
