# AI Context

> File di stato corrente del progetto.
> Da aggiornare ad ogni task completato (CLAUDE.md §2.2).

## Stato corrente

- **Fase roadmap**: Fase 0 — Setup infrastruttura.
- **Sotto-fase**: IMPLEMENTA.
- **Ultimo task completato**: Task 0.9 — Smoke verify.
- **Prossimo passo**: chiusura sotto-fase IMPLEMENTA → passaggio a sotto-fase REVIEW (CLAUDE.md §2.3).
- **Ultimo commit**: in corso (Task 0.9 fix + package-info).
- **mvn clean verify**: BUILD SUCCESS in 50s (parent + 4 moduli).
- **Smoke test eseguiti**: 1 per modulo, tutti verdi.
- **JaCoCo report**: presenti in `target/site/jacoco/` per tutti i moduli.
- **SpotBugs**: 0 warning High.
- **Acceptance A0.1 ÷ A0.5, A0.8 ÷ A0.11**: verificati ✓.
- **Acceptance A0.6 (CI verde)**: da verificare al primo push remoto (workflow file presente e syntactically valido).
- **Acceptance A0.7 (docker compose up)**: da verificare manualmente dall'utente — Docker non disponibile/non in PATH in questa sessione di sviluppo.
- **Piano di riferimento**: [`plans/PLAN-fase-0.md`](plans/PLAN-fase-0.md).

## Decisioni recenti

- Approvate dall'utente in data 2026-04-27 (Task PIANIFICA Fase 0):
  - Spotless: indentazione **2 spazi** (Google standard, conforme a NFR-M-04).
  - Maven `groupId`: `com.damaitaliana`.
  - Tag chiusura Fase 0: `v0.0.0`.
  - `docker-compose.yml`: include Adminer come servizio comodità di dev.
  - CI: include upload artifact JaCoCo HTML su PR/push.
  - MySQL container mappato su porta host **3307** (non 3306) per non confliggere col MySQL locale dell'utente, gestito via Workbench/DBeaver.

## SPEC clarifications needed

Nessuna al momento.

## Note operative

- Rappresentazione del board (FEN-like vs explicit) per il corpus regole: **decisione rinviata alla Fase 1** (CLAUDE.md §2.4.4).
- I test smoke `<Modulo>SmokeTest` introdotti in Fase 0 vanno **rimossi** appena ogni modulo ha test reali (Fase 1 per `shared`, Fase 4 per `core-server`, Fase 3 per `client`, Fase 5 per `server`).
- JaCoCo `haltOnFailure=false` in Fase 0; passa a `true` con soglie reali a partire dalla Fase 1.
