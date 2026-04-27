# AI Context

> File di stato corrente del progetto.
> Da aggiornare ad ogni task completato (CLAUDE.md §2.2).

## Stato corrente

- **Fase roadmap**: Fase 0 — Setup infrastruttura.
- **Sotto-fase**: IMPLEMENTA.
- **Ultimo task completato**: nessuno (inizio progetto).
- **Prossimo task**: Task 0.1 — File workflow SDD.
- **Ultimo commit**: `028d16d first commit` (CLAUDE.md + SPEC.md).
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
