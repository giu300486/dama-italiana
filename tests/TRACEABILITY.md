# Traceability Matrix

> Mappatura cumulativa **requisito SPEC → test**.
> Aggiornata insieme ad ogni nuovo test. Vedi `CLAUDE.md` §2.4.3.

---

## Requisiti funzionali (FR)

| FR ID     | Descrizione SPEC (breve)                                                | Test class                                       | Test method(s) / criterio                                            | Tipo  |
|-----------|--------------------------------------------------------------------------|--------------------------------------------------|----------------------------------------------------------------------|-------|
| FR-SP-02  | Tre livelli di difficoltà (Principiante / Esperto / Campione)            | `AiEngineFactoryTest` + `PrincipianteAiTest` + `EspertoAiTest` + `CampioneAiTest` | `forLevelReturnsCorrectImplementation`, `levelMethodMatchesEnum`, `noiseProbabilityIsApproximatelyTwentyFivePercent`, `deterministicOnRepeatedCalls`, `findsForcedCaptureMate` | Unit |
| FR-SP-04  | Highlight delle mosse legali al click sulla pedina                       | `ItalianRuleEngineMovementsTest` + `RuleEngineCorpusTest` | tutto il set parametrico, in particolare `mov-pedina-*`, `mov-dama-*` | Unit |
| FR-SP-05  | Highlight rosso pulsante per cattura obbligatoria                        | `ItalianRuleEngineCapturesTest`                  | `simpleMovesExcludedWhenAnyCaptureExists` + corpus `cap-*`           | Unit  |
| FR-SP-09  | Cronologia mosse in notazione FID                                        | `FidNotationTest`                                | `formatsSimpleMove`, `formatsMultiCapture`, `roundTrip*`              | Unit  |
| FR-COM-01 | Validazione mosse server-side (engine canonico)                          | `ItalianRuleEngineMovementsTest` (applyMove path) | `applyMoveRejectsIllegalMove`                                       | Unit  |
| FR-RUL-01 | Sezione Regole accessibile (regole eseguibili)                           | `RuleEngineCorpusTest`                           | tutte le 48 posizioni del corpus = regolamento eseguibile           | Unit  |

---

## Requisiti non funzionali (NFR)

| NFR ID    | Descrizione SPEC                                                         | Test class / gate                                | Note                                                                  |
|-----------|--------------------------------------------------------------------------|--------------------------------------------------|-----------------------------------------------------------------------|
| NFR-M-01  | Coverage ≥ 80% sul motore di gioco (`shared`)                            | JaCoCo `check` su `shared/pom.xml`               | Soglia Fase 1 alzata a ≥ 90% modulo + ≥ 90% `rules`. Fase 2 aggiunge ≥ 85% sul package `ai` (PLAN-fase-2 §7.8). Gate `haltOnFailure=true` |
| NFR-M-04  | Stile codice Google Java Style                                           | Spotless `googleJavaFormat` (parent POM)         | Verifica obbligatoria in `mvn verify`                                |
| NFR-P-02  | IA Campione ≤ 5s in posizioni di metà partita                            | `AiPerformanceTest#campioneRespondsWithinBudget` (`@Tag("performance")`) | Tolleranza 1.5x (PLAN-fase-2 §7.7) per CI; SPEC effettiva validata in dev locale |

---

## Acceptance criteria (SPEC sezione 17)

| AC ID    | Descrizione                                                                                        | Test class                                       | Test method(s)                                                       | Tipo |
|----------|----------------------------------------------------------------------------------------------------|--------------------------------------------------|----------------------------------------------------------------------|------|
| 17.1.1   | Partita SP vs Campione si conclude entro 30 min (parziale: F2 valida correttezza+perf, end-to-end UI in F3) | `AiTournamentSimulationTest#campionDominatesPrincipianteInQuickSanityCheck` + `@Tag("slow")` 100-game gating | Quick: 5 partite ≥4 vittorie. Slow: 100 partite Campione vs Principiante, ≥95 vittorie. | Integration |
| 17.1.6   | Tutte le 4 leggi della Dama Italiana applicate                                                     | `ItalianRuleEngineLawsTest` + `RuleEngineCorpusTest` | tutto + corpus `legge-quantita-*`, `legge-qualita-*`, `legge-precedenza-dama-*`, `legge-prima-dama-*` | Unit |
| 17.1.7   | Pedina che raggiunge la promozione durante una sequenza di catture **non continua**                | `ItalianRuleEngineSequencesTest` + corpus         | `manReachingPromotionRowStopsTheSequence`, `blackManReachingRank0StopsTheSequence`, corpus `promozione-stop-*` | Unit |
| 17.1.8   | Pedina **non può** catturare la dama                                                                | `ItalianRuleEngineCapturesTest` + corpus          | `manCannotCaptureKing`, `manCannotCaptureKingButOtherManCanCaptureManIsLegal`, corpus `pedina-non-cattura-dama-*` | Unit |
| 17.2.4   | Coverage ≥ 80% modulo `shared`                                                                      | JaCoCo gate                                       | `shared/pom.xml` — modulo ≥ 0.90, package `rules` ≥ 0.90, package `ai` ≥ 0.85 | Gate |
| 17.2.5   | SAST SpotBugs senza warning High                                                                    | SpotBugs gate (parent POM)                        | threshold `High`, `failOnError=true`                                  | Gate |

---

## Acceptance criteria di Fase 2 (PLAN-fase-2.md §2)

| ID    | Criterio                                                                                            | Verifica                                                                            |
|-------|-----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| A2.1  | `mvn -pl shared verify` BUILD SUCCESS                                                                | CI/locale                                                                           |
| A2.2  | **Gating**: `AiTournamentSimulationTest` Campione ≥ 95 vittorie su 100 vs Principiante              | `campionWinsAtLeast95OutOf100AgainstPrincipiante` (`@Tag("slow")`)                  |
| A2.3  | Campione produce mossa valida entro 5s in posizioni di metà partita                                  | `AiPerformanceTest#campioneRespondsWithinBudget` (`@Tag("performance")`, tol. 1.5x) |
| A2.4  | Esperto ≤ 2s, Principiante ≤ 500 ms                                                                  | `AiPerformanceTest#espertoRespondsWithinBudget`, `AiPerformanceTest#principianteRespondsWithinBudget` |
| A2.5  | Cancellazione `Future.cancel(true)` entro ~200 ms; ritorno graceful della migliore mossa trovata    | `VirtualThreadAiExecutorTest#hardCancelMakesGetThrow`, `gracefulCancelReturnsBestMoveSoFar` |
| A2.6  | Coverage `shared` ≥ 90%, `rules` ≥ 90%, `ai` ≥ 85%                                                   | JaCoCo gate (`shared/pom.xml`)                                                      |
| A2.7  | Spotless OK, SpotBugs 0 High                                                                          | `mvn verify`                                                                        |
| A2.8  | `package-info.java` per `shared.ai`, `shared.ai.evaluation`, `shared.ai.search`                      | Lettura visiva                                                                      |
| A2.9  | `tests/TRACEABILITY.md` aggiornato con FR-SP-02, NFR-P-02, AC §17.1.1 parziale, AC F2               | Questo file                                                                         |
| A2.10 | Nessun TODO/FIXME pending in `shared/src/main/java/`                                                 | grep                                                                                |
| A2.11 | Test corpus regole italiane (Fase 1, 48 posizioni) **continua a passare** (regression)               | `RuleEngineCorpusTest` verde                                                        |
| A2.12 | Determinismo simulazione: stesso seed → stesso risultato bit-per-bit                                 | Principiante seed = `42 + gameIndex` in `AiTournamentSimulationTest`                |
| A2.13 | Posizioni tattiche golden trovate dai livelli giusti                                                 | `AiTacticalPositionsTest` (5 posizioni)                                             |

---

## Acceptance criteria di Fase 1 (PLAN-fase-1.md §2)

| ID    | Criterio                                                                                            | Verifica                                                                            |
|-------|-----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| A1.1  | `mvn -pl shared verify` BUILD SUCCESS                                                                | CI/locale                                                                           |
| A1.2  | Test corpus contiene ≥ 48 posizioni distribuite per categoria                                        | `RuleEngineCorpusCoverageTest`                                                      |
| A1.3  | `RuleEngineCorpusTest` passa per ogni posizione                                                      | 48 parametri verdi                                                                  |
| A1.4  | `EndToEndGameApiTest` esegue 3 partite complete (white-wins, black-wins, draw)                       | `EndToEndGameApiTest` (3 metodi)                                                    |
| A1.5  | Coverage `shared` ≥ 90% globale e ≥ 90% sul package `rules`                                          | JaCoCo check con `haltOnFailure=true`                                               |
| A1.6  | Spotless OK, SpotBugs 0 High                                                                          | `mvn verify`                                                                        |
| A1.7  | `package-info.java` presente in tutti i sotto-package di `shared`                                    | Lettura visiva (`shared`, `domain`, `rules`, `notation`)                            |
| A1.8  | `tests/TRACEABILITY.md` aggiornato con righe per ogni FR/NFR/AC della fase                          | Questo file                                                                         |
| A1.9  | `SharedSmokeTest` rimosso                                                                            | Assenza file                                                                        |
| A1.10 | Nessun TODO/FIXME pending in `shared/src/main/java/`                                                 | grep                                                                                |
| A1.11 | AC §17.1.6/7/8 mappati a test specifici                                                              | Tabella sopra                                                                       |

---

## Note di manutenzione

- Una nuova riga è obbligatoria ad ogni test aggiunto che copre un FR/NFR/AC.
- Un FR senza nessuna riga è un finding `REQUIREMENT_GAP` alla review successiva (CLAUDE.md §2.4.3).
- Quando un test viene rimosso o spostato, aggiornare lo status; non lasciare righe orfane.
- Un FR può avere più righe: è normale e desiderabile per requisiti complessi.
