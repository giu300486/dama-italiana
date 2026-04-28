# TEST PLAN — Fase 2: IA (`shared.ai`)

- **Riferimento roadmap**: `SPEC.md` §16 — Fase 2.
- **SPEC version**: 2.0 (2026-04-26, aggiornata il 2026-04-28 con CR-001).
- **Data**: 2026-04-28.
- **Autore**: Claude Code.
- **Stato**: chiuso.

---

## 1. Scopo della sotto-fase TEST

Documentare la strategia, la composizione e la copertura della suite di test della Fase 2, validare il **gating SPEC §16 Fase 2** ("Campione vince contro Principiante in ≥ 95% delle 100 partite simulate"), e finalizzare la matrice di tracciabilità requisiti → test (`TRACEABILITY.md`) per i requisiti coperti dalla Fase 2.

---

## 2. Strategia di test (CLAUDE.md §2.4)

Piramide classica con **traceability matrix esplicita** (Approccio C). Fase 2 estende la suite del modulo `shared` aggiungendo i test del package `ai`. Gli altri moduli (`core-server`, `client`, `server`) restano con i loro smoke test per il momento (saranno popolati a partire dalle fasi 4-5).

### 2.1 Composizione effettiva (modulo `shared`)

| Tipo                                         | Conteggio | Tooling                         | Cosa testa                                                                                                         |
|----------------------------------------------|----------:|---------------------------------|--------------------------------------------------------------------------------------------------------------------|
| **Unit — modello (F1, regressione)**         |        58 | JUnit 5 + AssertJ               | `Square`, `Color`, `PieceKind`, `Piece`, `Board`, `Move`/`SimpleMove`/`CaptureSequence`, `GameStatus`, `GameState`. |
| **Unit — notazione (F1, regressione)**       |        59 | JUnit 5                         | `FidNotation`: bijezione 1↔32, parsing/format mosse.                                                              |
| **Unit — RuleEngine (F1, regressione)**      |        70 | JUnit 5 + AssertJ               | Movimenti, catture, sequenze, 4 leggi, applyMove+promozione, status. +1 test interno post-hardening fix.            |
| **Corpus parametrizzato (F1, regressione)**  |        48 | JUnit 5 + Jackson + `@MethodSource` | `RuleEngineCorpusTest` su `test-positions.json`.                                                              |
| **Schema/copertura corpus (F1)**             |         7 | JUnit 5                         | `RuleEngineCorpusSchemaTest` (3) + `RuleEngineCorpusCoverageTest` (4).                                            |
| **End-to-end via API pura (F1)**             |         3 | JUnit 5                         | `EndToEndGameApiTest`: white-wins, black-wins (stallo), draw (40 mosse).                                          |
| **Unit — evaluation (F2)**                   |        34 | JUnit 5                         | `MaterialTermTest` (6), `MobilityTermTest` (4), `AdvancementTermTest` (5), `EdgeSafetyTermTest` (4), `CenterControlTermTest` (6), `WeightedSumEvaluatorTest` (9). |
| **Unit — search core (F2)**                  |        33 | JUnit 5                         | `StandardMoveOrdererTest` (7), `MinimaxSearchTest` (18), `IterativeDeepeningSearchTest` (9), `PvFirstOrdererTest` (4), `MinimaxSearchTtTest` (5). Hash + TT a parte. |
| **Unit — TT + Zobrist (F2)**                 |        16 | JUnit 5                         | `ZobristHasherTest` (8) + `TranspositionTableTest` (8). Determinismo, slot collision, store/probe.                |
| **Unit — cancellation (F2)**                 |        14 | JUnit 5                         | `CancellationTokenTest`: `never`, `mutable`, `deadline` (con `Clock` iniettabile), `composite`, null-guards.       |
| **Unit — AiEngine (F2)**                     |        20 | JUnit 5                         | `AiEngineFactoryTest` (6) + `PrincipianteAiTest` (5) + `EspertoAiTest` (3) + `CampioneAiTest` (5).                |
| **Concurrent — VirtualThreadAiExecutor (F2)** |        9 | JUnit 5 + concurrent             | Submit, timeout, graceful + hard cancel, multiple submissions, closed-executor rejection, null/non-positive timeout. |
| **Tactical positions (F2)**                  |         5 | JUnit 5                         | `AiTacticalPositionsTest`: 5 posizioni golden hand-built.                                                          |
| **Tournament simulation — quick (F2)**       |         1 | JUnit 5                         | `AiTournamentSimulationTest#campionDominatesPrincipianteInQuickSanityCheck` (5 partite, untagged).                 |
| **Tournament simulation — gating (F2)**      |         1 | JUnit 5 `@Tag("slow")`          | `AiTournamentSimulationTest#campionWinsAtLeast95OutOf100AgainstPrincipiante` (100 partite, ~16 min).               |
| **Performance (F2)**                         |         3 | JUnit 5 `@Tag("performance")`    | `AiPerformanceTest`: budget per livello con tolleranza 1.5x (PLAN-fase-2 §7.7).                                    |
| **Totale (con `slow` + `performance` inclusi)** | **391** |                                  |                                                                                                                    |
| **Default `mvn -pl shared verify -DexcludedGroups=slow,performance`** | **387** |  | (skip 4 test taggati)                                                                                              |

### 2.2 Coverage effettiva (`mvn -pl shared jacoco:report`)

Misura post-Fase 2 dal report `shared/target/site/jacoco/jacoco.csv`:

| Scope                                              | Linee coperte / totali | Coverage line | Branch coperte / totali | Coverage branch | Gate            |
|----------------------------------------------------|------------------------|---------------|-------------------------|-----------------|-----------------|
| `shared` — modulo                                  | 767 / 788              | **97.3%**     | 404 / 423               | **95.5%**       | ≥ 90% ✅        |
| `shared.rules` — package                           | 202 / 210              | **96.2%**     | 132 / 138               | **95.7%**       | ≥ 90% ✅        |
| `shared.ai` (incl. `ai.evaluation`, `ai.search`)   | 379 / 388              | **97.7%**     | 151 / 157               | **96.2%**       | ≥ 85% ✅        |

Tutte le soglie di JaCoCo `check` sono rispettate; il gate `haltOnFailure=true` fa saltare la build se queste scendono.

### 2.3 SAST e style

- **SpotBugs**: 0 warning High su tutti i moduli (gate `failOnError=true`, threshold `High`).
- **Spotless googleJavaFormat (2 spazi, NFR-M-04)**: passa pulito.
- **Maven Enforcer**: dependencyConvergence + Java 21 + Maven ≥ 3.9 — passa.

---

## 3. Validazione del gating SPEC §16 Fase 2

> **Acceptance**: Campione vince contro Principiante in ≥ 95% delle 100 partite simulate.

### 3.1 Setup del test

`AiTournamentSimulationTest#campionWinsAtLeast95OutOf100AgainstPrincipiante`:

- 100 partite scriptate via API pura (`AiEngine.chooseMove` con `CancellationToken.never()`); colori alternati (50 con Campione bianco, 50 nero).
- Principiante con `SplittableRandom(42L + gameIndex)` — bit-deterministico.
- Campione fully deterministico (TT determinista per il seed Zobrist `0xDA4A172L`).
- Cap di 200 ply per partita (oltre, la partita è considerata patta tecnica → NON vittoria di Campione).
- Asserzione: `campioneWins >= 95`.

### 3.2 Esito

```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 959.0 s
BUILD SUCCESS — Total time: 16:02 min
```

✅ **Gating PASSED** in 16 minuti su hardware locale dello sviluppatore. La soglia ≥ 95 vittorie è soddisfatta.

### 3.3 Determinismo

Il test è bit-deterministico: stesso jar version + stesso hardware ⇒ stesso conteggio vittorie. Le sorgenti di determinismo:

- Principiante: `SplittableRandom(42L + i)` — rumore riproducibile.
- Campione: nessuna randomicità — search puramente deterministico.
- Zobrist: seed fisso `0xDA4A172L` (ADR-026).
- TT: politica always-replace, ordine inserimenti dipende solo dall'ordine search-tree (deterministico).

---

## 4. Test corpus regole italiane

Nessuna nuova posizione aggiunta in Fase 2. Le 48 posizioni di Fase 1 sono **tutte ancora verdi** (regression).

Distribuzione invariata rispetto al `TEST-PLAN-fase-1` §3.2:

| Area regole                                 | Effettivo |
|---------------------------------------------|----------:|
| Movimento pedina                            |       3 ✅ |
| Movimento dama                              |       4 ✅ |
| Cattura semplice pedina                     |       4 ✅ |
| Cattura semplice dama                       |       4 ✅ |
| Pedina non cattura dama                     |       3 ✅ |
| Presa multipla                              |       5 ✅ |
| Legge della quantità                        |       5 ✅ |
| Legge della qualità                         |       5 ✅ |
| Legge della precedenza dama                 |       3 ✅ |
| Legge della prima dama                      |       3 ✅ |
| Promozione con stop sequenza                |       3 ✅ |
| Triplice ripetizione                        |       2 ✅ |
| Regola 40 mosse                             |       2 ✅ |
| Stallo = sconfitta                          |       2 ✅ |
| **Totale**                                  |    **48** |

In Fase 2 sono state aggiunte **5 posizioni tattiche golden** (`AiTacticalPositionsTest`), hardcoded in Java perché legate al search-depth più che alle regole (nessuna duplicazione del corpus regole F1).

---

## 5. Naming convention

Conforme a CLAUDE.md §2.4.5:

- Unit test: `<ClasseProduzione>Test`. Stile metodo: `should<Espressione>_when<Condizione>` o `<feature><Scenario>` per i test del corpus / tactical / simulation.
- I test parametrici (`RuleEngineCorpusTest`) ricevono il nome dall'`id` della posizione.

Esempi specifici di Fase 2:
- `MinimaxSearchTest#detectsForcedMateInOnePlyAsHugeScore`
- `IterativeDeepeningSearchTest#cancellationMidIterationKeepsPreviousDepthsBestMove`
- `AiTournamentSimulationTest#campionDominatesPrincipianteInQuickSanityCheck`
- `AiTournamentSimulationTest#campionWinsAtLeast95OutOf100AgainstPrincipiante`

---

## 6. Tag JUnit

Tre gruppi di test sono tagged in Fase 2:

| Tag             | Conteggio | Default in `mvn verify`    | Cosa contiene                                                                 |
|-----------------|----------:|-----------------------------|-------------------------------------------------------------------------------|
| `slow`          |         1 | ON                          | Gating 100-game Campione vs Principiante. ~16 min. Skippabile via `-DexcludedGroups=slow`. |
| `performance`   |         3 | ON                          | Wall-clock budget per livello via executor. Tolleranza 1.5x (PLAN-fase-2 §7.7). |
| _(untagged)_    |       387 | sempre                      | Tutto il resto.                                                                |

`mvn -pl shared verify` (default) include tutto. Loop di sviluppo veloce: `mvn -pl shared verify -DexcludedGroups=slow,performance` (~1.5 min).

---

## 7. Aggiornamento `TRACEABILITY.md`

In Fase 2 sono state aggiunte le righe per:

- **FR-SP-02** — Tre livelli di difficoltà → `AiEngineFactoryTest`, `PrincipianteAiTest`, `EspertoAiTest`, `CampioneAiTest`.
- **NFR-P-02** (upgraded) — IA Campione ≤ 5s → `AiPerformanceTest#campioneRespondsWithinBudget` (precedentemente "indiretto F1").
- **AC §17.1.1** (parziale) — Partita SP entro 30 min → `AiTournamentSimulationTest` quick + slow gating.
- **AC F2 (A2.1 ÷ A2.13)** — sezione dedicata con 13 righe operative.

Non sono state rimosse righe esistenti (le righe FR-SP-04, FR-SP-05, FR-SP-09, FR-COM-01, FR-RUL-01, NFR-M-01, NFR-M-04, AC §17.1.6/7/8/§17.2.4/5 di Fase 1 restano intatte).

---

## 8. Regressione cross-modulo

`mvn clean verify -DexcludedGroups=slow,performance` (root):

```
Reactor Summary for Dama Italiana Multiplayer — Parent 0.1.0-SNAPSHOT:
[INFO] Dama Italiana Multiplayer — Parent ........... SUCCESS [  4.171 s]
[INFO] Dama Italiana — Shared (domain & rules) ...... SUCCESS [01:29 min]
[INFO] Dama Italiana — Core Server (transport-agnostic) SUCCESS [ 10.192 s]
[INFO] Dama Italiana — Client (JavaFX desktop) ...... SUCCESS [ 13.158 s]
[INFO] Dama Italiana — Central Server (Spring Boot) . SUCCESS [ 12.430 s]
[INFO] BUILD SUCCESS  — Total time: 02:10 min
```

Nessuna regressione sui moduli `core-server`, `client`, `server` (smoke test ciascuno verde).

---

## 9. Limiti documentati e debiti tecnici tracciati

I seguenti debiti sono noti, deliberati, e tracciati per le fasi successive:

- **F-001 + F-002** (REVIEW-fase-2): `isThreefoldRepetition` replay-based con catch `IllegalMoveException` — workaround per supportare il search di stati hand-built. Il fix vero (Zobrist-incremental) è deferred-F4 (PLAN-fase-2 §7.9).
- **F-003** (REVIEW-fase-2): AC §17.1.1 (partita SP entro 30 min) coperto solo via API simulazione. Validazione UI E2E in F3 by design.
- **F-004**: `ZobristHasher.hashAfterMove` (incremental update) non implementato. Recompute-from-scratch è sufficiente per Campione a depth 8 (~16 XOR/nodo, non bottleneck). Documentato in Javadoc.
- **F-005**: `MoveOrderer.order(state)` parametro non utilizzato dall'implementazione standard. Future-proof, documentato in Javadoc.

Nessun limite mette a rischio l'acceptance di Fase 2.

---

## 10. Closure della sotto-fase TEST (CLAUDE.md §2.4.6)

- [x] Coverage target raggiunti — modulo 97.3%, `rules` 96.2%, `ai` 97.7%
- [x] Traceability matrix aggiornata — ogni FR/NFR/AC della fase ha almeno un test
- [x] Test corpus regole italiane invariato (48 posizioni, regression verde)
- [x] `mvn verify` passa pulito su tutti i moduli (default `-DexcludedGroups=slow,performance`)
- [x] **Gating A2.2 ✅ PASSED** (Campione ≥ 95/100 vs Principiante in 16:02 min)
- [x] Test plan documenta scelte e copertura — questo file
- [x] Nessun test in stato `@Disabled`/`@Ignore` senza issue tracciata

---

**FINE TEST-PLAN-fase-2**
