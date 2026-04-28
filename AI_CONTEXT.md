# AI Context

> File di stato corrente del progetto.
> Da aggiornare ad ogni task completato (CLAUDE.md §2.2).

## Stato corrente

- **Branch corrente**: `develop` (default branch). Fase 2 chiusa, branch `feature/2-ai` eliminato.
- **Fase roadmap**: Fase 2 — IA (`shared.ai`), **chiusa**.
- **Sotto-fase**: nessuna in corso. Le 4 sotto-fasi della Fase 2 (PIANIFICA / IMPLEMENTA / REVIEW / TEST) sono tutte chiuse; chiusura formalizzata col tag `v0.2.0` (commit `4839149` su `main`, merge `91fa9d3` su `develop`).
- **Ultimo task completato**: chiusura Fase 2 con merge `feature/2-ai` → `develop` (`91fa9d3`) → `main` (`4839149`), tag `v0.2.0` applicato, branch feature eliminato (locale; remoto non disponibile per ADR-019).
- **Prossimo passo**: PIANIFICA Fase 3 — Client UI single-player (JavaFX + design system CSS, schermate splash/menu/board/settings, rendering damiera + animazioni, salvataggio multi-slot, autosave, localizzazione IT/EN, schermata regole). Output atteso: `plans/PLAN-fase-3.md`.
- **Stato test**: `mvn clean verify` (root) BUILD SUCCESS in ~2 min — `shared` 391 test (387 default + 1 slow + 3 performance), JaCoCo 97.3% modulo + 96.2% package `rules` + 97.7% package `ai`, SpotBugs 0 High, Spotless OK. `core-server`/`client`/`server` ancora con il singolo smoke test ciascuno.
- **Test gating A2.2 (slow)**: ✅ PASSED il 2026-04-28 in 16:02 min (Campione ≥ 95/100 vs Principiante).
- **Piano di riferimento Fase 2**: [`plans/PLAN-fase-2.md`](plans/PLAN-fase-2.md). Closure REVIEW: [`reviews/REVIEW-fase-2.md`](reviews/REVIEW-fase-2.md). Closure TEST: [`tests/TEST-PLAN-fase-2.md`](tests/TEST-PLAN-fase-2.md).

## Decisioni recenti

- Approvate dall'utente in data 2026-04-27 (Task PIANIFICA Fase 0):
  - Spotless: indentazione **2 spazi** (Google standard, conforme a NFR-M-04).
  - Maven `groupId`: `com.damaitaliana`.
  - Tag chiusura Fase 0: `v0.0.0`.
- Revisione post-implementazione (2026-04-27, dopo feedback utente):
  - **ADR-018**: Docker Compose rimosso. L'ambiente di sviluppo usa il MySQL locale dell'utente (porta 3306, gestito via Workbench/DBeaver).
  - **ADR-019**: workflow GitHub Actions rinominato in `ci.yml.disabled` (non eseguibile). Verrà riattivato quando un repository remoto sarà disponibile.
- Adozione **GitFlow leggero** (2026-04-28): `main` = production / tag, `develop` = integrazione e default branch GitHub, branch effimeri `feature/<fase>-<topic>` e `fix/review-N-F-<id>` staccati da `develop`, mergiati `--no-ff`. Tag delle fasi (`v0.<fase>.0`) sul commit di merge in `main`.
- Approvati dall'utente in data 2026-04-28 (Task PIANIFICA Fase 2): tutti gli stop point §7.1-§7.10 in opzione A in blocco. Task 2.14 (Zobrist-based threefold repetition) deferred-F4. Si è applicata in F2 una hardening fix non-Zobrist (catch `IllegalMoveException` → `false`) per supportare il search su stati hand-built (REVIEW finding F-001/F-002, ACKNOWLEDGED).
- ADR-024 ÷ ADR-028 registrati durante Fase 2 (architettura `AiEngine` sealed + 3 livelli, Evaluator modulare, Zobrist + TT, cancellation cooperativa, rumore Principiante deterministico).

## SPEC clarifications needed

Nessuna al momento.

## Note operative

- Rappresentazione del board per il corpus regole **decisa in Fase 1** (ADR-022): JSON con quattro liste disgiunte `whiteMen`/`whiteKings`/`blackMen`/`blackKings` indicizzate in FID 1-32 (ADR-020).
- Smoke test `SharedSmokeTest` **rimosso** in Task 1.11. Restano `CoreServerSmokeTest`, `ClientSmokeTest`, `ServerSmokeTest`: da rimuovere quando i rispettivi moduli ricevono test reali (Fase 4 / 3 / 5 rispettivamente).
- JaCoCo `haltOnFailure=true` su `shared` con soglie 90% bundle + 90% package `rules` (Fase 1) + 85% package `ai` (Fase 2). Per `client`/`core-server`/`server` il gate resta lasco fino alle rispettive fasi.
- `isThreefoldRepetition` (ADR-021) replay-based con hardening fix Fase 2 (catch `IllegalMoveException` → `false`). Limite documentato per stati hand-built. Ottimizzazione Zobrist-incremental deferred a Fase 4 (REVIEW-fase-2 finding F-001/F-002).
- Tag JUnit attivi nel modulo `shared`: `slow` (1 test, gating A2.2 ~16 min) + `performance` (3 test, NFR-P-02). Default `mvn verify` include tutto; loop di sviluppo veloce: `-DexcludedGroups=slow,performance`.
- Determinismo Principiante (ADR-028): `RandomGenerator` iniettato; in test `SplittableRandom(42L + gameIndex)` per simulazioni riproducibili. Default produzione: `SplittableRandom(System.nanoTime())`.
- Determinismo Zobrist (ADR-026): seed costante `0xDA4A172L`. Stesso jar = stessi hash = stesso comportamento TT.
