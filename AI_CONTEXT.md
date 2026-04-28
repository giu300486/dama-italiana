# AI Context

> File di stato corrente del progetto.
> Da aggiornare ad ogni task completato (CLAUDE.md §2.2).

## Stato corrente

- **Branch corrente**: `feature/2-ai` (staccato da `develop` il 2026-04-28).
- **Fase roadmap**: Fase 2 — IA (`shared.ai`).
- **Sotto-fase**: **REVIEW chiusa** il 2026-04-28. 7 finding totali (nessuno bloccante): F-004 + F-005 RESOLVED via mini-fix Javadoc; F-001 + F-002 + F-003 + F-006 + F-007 ACKNOWLEDGED (F-001/F-002 deferred-F4, F-003 deferred-F3 by design, F-006/F-007 design intentional).
- **Ultimo task completato**: closure `reviews/REVIEW-fase-2.md` con disposizione finale di tutti i finding e gating A2.2 ✅ PASSED.
- **Prossimo passo**: sotto-fase **TEST** (CLAUDE.md §2.4) → produrre `tests/TEST-PLAN-fase-2.md`, validare coverage finale, garantire la traceability matrix completa per FR/NFR/AC della Fase 2, eseguire `mvn clean verify` (root) per la regressione.
- **Stato test**: 387/387 verdi con `-DexcludedGroups=slow,performance` sul modulo `shared` in `mvn -pl shared verify`. JaCoCo gate raggiunto (modulo ≥ 90%, `rules` ≥ 90%, `ai` ≥ 85%). SpotBugs 0 High. Spotless clean.
- **Test gating A2.2 (slow)**: ✅ PASSED il 2026-04-28 in 16:02 min (`mvn -pl shared test -Dtest=AiTournamentSimulationTest#campionWinsAtLeast95OutOf100AgainstPrincipiante -Dgroups=slow` → BUILD SUCCESS, 1 test green). Campione ≥ 95/100 vs Principiante confermato.
- **Task 2.14 (deferred)**: ottimizzazione `isThreefoldRepetition` con Zobrist — rinviata a F4 come da §7.9 del piano. È stata applicata in F2 una hardening fix non-Zobrist (catch `IllegalMoveException` → `false`) per supportare il search su stati hand-built.
- **Stato test**: `mvn clean verify` (root) BUILD SUCCESS — `shared` 245 test, JaCoCo 96.7% modulo + 95.7% package `rules`, SpotBugs 0 High, Spotless OK; `core-server`/`client`/`server` ancora con il singolo smoke test ciascuno.
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

Nessuna ambiguità SPEC bloccante. Il piano F2 contiene 10 stop point su scelte di design interne (architettura `AiEngine`, schema di valutazione, Zobrist/TT, modello cancellazione, rumore Principiante, esecuzione test simulazione, tolleranza performance, soglia coverage, inclusione Task 2.14, naming branch). Tutti hanno una proposta motivata (opzione A) — l'utente può confermare in blocco oppure indicare i punti su cui preferisce un'opzione diversa.

## Note operative — deviazioni dal piano

- **Task 2.4 vs 2.5**: la primitive minimale di `CancellationToken` (interface + `never()` + `MutableCancellationToken`) e `SearchCancelledException` sono state introdotte in Task 2.4 anziché in Task 2.5 perché necessarie per testare la cancellazione di `MinimaxSearch`. Task 2.5 aggiungerà `CancellationToken.deadline(Instant)` + `composite(...)` come previsto, oltre a `IterativeDeepeningSearch`. Deviazione minore, motivata dalla coesione test-codice (CLAUDE.md §2.2).

## Note operative

- Rappresentazione del board per il corpus regole **decisa in Fase 1** (ADR-022): JSON con quattro liste disgiunte `whiteMen`/`whiteKings`/`blackMen`/`blackKings` indicizzate in FID 1-32 (ADR-020).
- Smoke test `SharedSmokeTest` **rimosso** in Task 1.11. Restano `CoreServerSmokeTest`, `ClientSmokeTest`, `ServerSmokeTest`: da rimuovere quando i rispettivi moduli ricevono test reali (Fase 4 / 3 / 5 rispettivamente).
- JaCoCo `haltOnFailure=true` da Fase 1 sul modulo `shared` con soglie 90% bundle + 90% package `rules`. Per `client`/`core-server`/`server` il gate resta lasco fino alle rispettive fasi.
- `isThreefoldRepetition` (ADR-021) replay-based: assume `history` consistente con `GameState.initial()`. Stati costruiti a mano con history arbitrarie possono dare falsi negativi (documentato nel Javadoc). Possibile ottimizzazione futura: hash Zobrist (Fase 2 con transposition table).
