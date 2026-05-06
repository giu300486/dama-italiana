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
| FR-COM-01 | Anti-cheat 5-illegal-moves consecutive forfeit (SPEC §9.8.3, ADR-040)    | `AntiCheatTest` + `MatchManagerValidationTest` + `MatchManagerTurnTest` | `fiveConsecutiveIllegalMovesForfeitsThePlayerAndAwardsTheOpponent`, `aLegalMoveResetsTheCounterAndAvoidsForfeit`, `eachPlayerHasItsOwnCounterAndOnlyTheOffenderForfeits`, `illegalMoveProducesMoveRejectedAndIncrementsCounter`, `engineThrowingIllegalMoveExceptionBecomesMoveRejected`, `legalMoveResetsAntiCheatCounterForSender`, wrong-turn rejection bumps counter | Unit + Integration |
| FR-COM-04 | Sequence number strict monotonic per match (SPEC §7.5)                   | `MatchRepositoryContractTest` (via `InMemoryMatchRepositoryTest`) + `SoloMatchEndToEndTest` | `appendEventRejectsNonMonotonicSequenceNo`, `firstEventMustBeSequenceZero`, `currentSequenceNoIsMinusOneForUnknownMatch`, `playsRandomGameUntilTerminal` (asserts `events.get(i).sequenceNo() == i`) | Unit + E2E |
| FR-RUL-01 | Sezione Regole accessibile (regole eseguibili)                           | `RuleEngineCorpusTest`                           | tutte le 48 posizioni del corpus = regolamento eseguibile           | Unit  |
| FR-SP-01  | Avvio partita SP vs CPU offline                                          | `MainMenuControllerTest` + `SinglePlayerSetupController` (manuale via `mvn javafx:run`) | Click su "Single Player" apre setup → conferma crea `SinglePlayerGame` e naviga BOARD | Unit + manual |
| FR-SP-02  | Tre livelli di difficoltà (Principiante / Esperto / Campione) — UI       | `SinglePlayerSetupController` (ToggleGroup `levelGroup` con 3 RadioButton mappati 1-a-1 sull'enum `AiLevel`) | Verificato anche dal corpus AI di F2 + lancio manuale | Manual + F2 unit |
| FR-SP-03  | Scelta colore Bianco / Nero / Casuale                                    | `SinglePlayerSetupController.selectedColorChoice` (visible via FXML) | Default Bianco; `ColorChoice.RANDOM` risolto via `RandomGenerator` iniettato | Manual |
| FR-SP-04  | Highlight mosse legali al click sulla pedina (UI)                        | `SinglePlayerControllerTest` + `SinglePlayerE2ETest` | `selectedSquare`+`highlightLegalTargets` capture, A3.7 in-UI verifica           | Unit + E2E |
| FR-SP-05  | Highlight cattura obbligatoria (style `pulse-mandatory`)                 | `SinglePlayerControllerTest` + `BoardRendererTest` | `mandatoryHighlightsRecomputeAfterEachMove` + cell rendering        | Unit |
| FR-SP-06  | Undo/redo illimitato (coppia umana+IA come unità)                        | `SinglePlayerControllerTest` + `SinglePlayerE2ETest` + `BoardViewControllerTest` | `undoPair*`/`redoPair*` (9 test su Task 3.24), `undoRedoCycleRestoresAndReappliesHumanMove` (E2E), `onUndo/onRedoDelegatesToControllerUndoPair/RedoPair` (UI binding) | Unit + E2E |
| FR-SP-07  | Multi-slot save (lista, carica, riprendi)                                | `SaveServiceTest` + `LoadScreenControllerTest` + `SaveLoadE2ETest` | save/load/list/delete + UI filter+sort, round-trip byte-equal      | Unit + E2E |
| FR-SP-08  | Autosave write-through dopo ogni mossa + recovery prompt al riavvio      | `AutosaveServiceTest` + `SinglePlayerAutosaveTriggerTest` + `AutosaveE2ETest` | Round-trip recovery, schema mismatch toast, IO error tolerated     | Unit + E2E |
| FR-SP-09  | Cronologia mosse in notazione FID                                        | `MoveHistoryViewModelTest` + `FidNotationTest`   | `appendingWhiteMoveCreatesNewRow`, `appendingBlackMoveCompletesRow`, `formatsCaptureSequenceWithCrossNotation`, `replaceWithHistory*` | Unit |
| FR-RUL-02 | Navigazione 7 sezioni regole                                             | `RulesControllerTest` + `RulesScreenE2ETest`     | `allSevenSectionsAreExposedInDisplayOrder`, `openRulesAndNavigateSections` | Unit + E2E |
| FR-RUL-03 | ≥ 5 diagrammi statici su ≥ 4 sezioni                                     | `RuleDiagramLoaderTest`                          | `coverageHasAtLeastFiveDiagramsAcrossAtLeastFourSections`           | Unit |
| FR-RUL-04 | Mini-animazioni regole (opzionale §7.6)                                  | `RulesAnimationsTest`                            | 8 test su 3 Kind (SIMPLE_CAPTURE/MULTI_CAPTURE/PROMOTION) + animation builder | Unit |
| FR-RUL-05 | Tutti i testi regole localizzati                                         | `RulesControllerTest#allSevenSectionsHaveLocalizedTitleAndBody` + `LocalizationE2ETest` | Bundle parity + 87 chiavi referenziate dai controller risolvono IT/EN | Unit + E2E |

---

## Requisiti non funzionali (NFR)

| NFR ID    | Descrizione SPEC                                                         | Test class / gate                                | Note                                                                  |
|-----------|--------------------------------------------------------------------------|--------------------------------------------------|-----------------------------------------------------------------------|
| NFR-M-01  | Coverage ≥ 80% sul motore di gioco (`shared`)                            | JaCoCo `check` su `shared/pom.xml`               | Soglia Fase 1 alzata a ≥ 90% modulo + ≥ 90% `rules`. Fase 2 aggiunge ≥ 85% sul package `ai` (PLAN-fase-2 §7.8). Gate `haltOnFailure=true` |
| NFR-M-02  | Coverage ≥ 60% modulo `client` (line + branch)                           | JaCoCo `check` su `client/pom.xml`               | `haltOnFailure=true`, esclusi 9 file: bootstrap JavaFX/Spring (`JavaFxApp`, `JavaFxAppContextHolder`, `ClientApplication`), Alert wrapper (`JavaFxUserPromptService`), anonymous cell-factory (`MoveHistoryView$MoveHistoryCell`, `LoadScreenController$MiniatureCell`, `RulesController$1`, `SettingsController$1`, `SplashController$1`). Bundle dopo esclusioni: line 73.94%, branch 60.55%. |
| NFR-M-04  | Stile codice Google Java Style                                           | Spotless `googleJavaFormat` (parent POM)         | Verifica obbligatoria in `mvn verify`                                |
| NFR-P-02  | IA Campione ≤ 5s in posizioni di metà partita                            | `AiPerformanceTest#campioneRespondsWithinBudget` (`@Tag("performance")`) | Tolleranza 1.5x (PLAN-fase-2 §7.7) per CI; SPEC effettiva validata in dev locale |
| NFR-U-01  | Localizzazione IT/EN runtime                                             | `MessageSourceConfigTest` + `LocalizationE2ETest` | Bundle parity, 87 chiavi controller risolvono IT/EN, `MessageFormat` placeholder substitution | Unit + E2E |
| NFR-U-04  | Contrasto WCAG AA (light mode in F3)                                     | Verifica visiva del design system `theme-light.css` | Token contrast definiti SPEC §13.2; misura tool deferred F11 con dark mode toggle | Manual |
| NFR-U-05  | UI client adattiva 1280×720 → 4K, min Stage 1024×720, DPI 100/125/150/200%, Stage state persistito v3 (SPEC v2.3 CR-F4.5-001 §13.7) | `ResponsivenessParametricTest` (`@Tag("slow")`, 7 res × 8 schermate = 56 scenari) + `BoardRendererLayoutTest` + `SidePanelLayoutTest` + `ScreenLayoutTest` + `PrimaryStageInitializerTest` + `JavaFxScalingHelperTest` + `BoardFrameThicknessHelperTest` + `StagePersistenceValidatorTest` + `PreferencesServiceTest` (v2→v3) | Layout math DPI-independent → single-JVM parametric copre correttezza al 100% logical DPI; varianti DPI 125/150/200% verificate manualmente in TEST-PLAN-fase-4.5 §5.2 (Task 4.5.16) |

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
| 17.1.9   | Salvataggio multi-slot single-player (lista, carica, riprendi)                                      | `SaveLoadE2ETest`                                 | `saveThenLoadResumesAtSameState` + `deleteRemovesSlotFromListing`     | E2E |
| 17.1.12  | Sezione regole in-app accessibile e completa                                                        | `RulesScreenE2ETest` + `RulesFxmlSmokeTest`      | `openRulesAndNavigateSections` + sezione default + diagrammi rendered | E2E |
| 17.1.2   | Partita Internet (preview F4 — `MatchManager` API end-to-end senza UI/rete)                          | `SoloMatchEndToEndTest`                          | `playsRandomGameUntilTerminal`, `replayFromEventsReconstructsCurrentState`, `eventsBroadcastOnBusAndStomp` (Spring DI flow, `BufferingStompPublisher` + `@EventListener`) — F6 incolla il trasporto reale sul `MatchManager` di F4 | E2E (API) |
| 17.1.10  | Autosave host LAN (preview F4 — `MatchRepository.appendEvent` strict monotonic + `eventsSince` replay-from-cursor) | `MatchRepositoryContractTest` + `SoloMatchEndToEndTest#replayFromEventsReconstructsCurrentState` | F7 host LAN persisterà via lo stesso port; reconnection-replay è già provato dal fold `MoveApplied` events su `GameState.initial()` | E2E (API) |
| 17.2.8   | UI responsive desktop su gamut 1280×720 → 4K + DPI 100/125/150/200% (SPEC v2.3 CR-F4.5-001) | `ResponsivenessParametricTest` (`@Tag("slow")`) | 56 scenari (7 res × 8 schermate) con assertions: no clipping (`root.bounds ≤ scene + 1px`), no `ScrollBar` visibile, board square (`8 × currentCellSize ≤ renderer.w/h`); manual demo DPI varianti in TEST-PLAN-fase-4.5 §5.2 (Task 4.5.16) | Integration + Manual |

---

## Acceptance criteria di Fase 4 (PLAN-fase-4.md §2.2)

| ID    | Criterio                                                                                            | Verifica                                                                            |
|-------|-----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| A4.1  | `mvn -pl core-server verify` BUILD SUCCESS, JaCoCo gate ≥ 80% line + branch (NFR-M-01)              | Output Maven (150 test verdi, 38 classi analizzate, gate "All coverage checks have been met") |
| A4.2  | Match end-to-end via API Java (autoritativo): partita random fino a stato terminale via `MatchManager` API; status FINISHED, MatchEnded ultimo evento | `SoloMatchEndToEndTest#playsRandomGameUntilTerminal` (seed 42, MAX_PLIES 500)        |
| A4.3  | Sequence number monotonico (FR-COM-04): per ogni match `sequenceNo` strict crescente da 0; `eventsSince(matchId, n)` ritorna esattamente seq > n in ordine | `MatchRepositoryContractTest#appendEventRejectsNonMonotonicSequenceNo`, `eventsSinceReturnsSuffix*`, `SoloMatchEndToEndTest` (assert `i.sequenceNo() == i`) |
| A4.4  | Anti-cheat 5-illegal forfeit (FR-COM-01, SPEC §9.8.3, ADR-040): 5 illegali consecutive → MoveRejected x5 + MatchEnded(FORFEIT_ANTI_CHEAT, opposite winner); legal move resets counter | `AntiCheatTest` (3 test, copre forfait + reset + per-player independence)            |
| A4.5  | `MatchManager.applyMove` valida turno: mossa fuori turno → MoveRejected NOT_YOUR_TURN, sequence + counter avanzano | `MatchManagerTurnTest#wrongTurnProducesMoveRejectedAndBumpsCounter`                  |
| A4.6  | Validazione via `RuleEngine`: tutte le mosse passano da `RuleEngine.applyMove`; mossa illegale → MoveRejected ILLEGAL_MOVE, stato non modificato | `MatchManagerValidationTest#illegalMoveProducesMoveRejectedAndIncrementsCounter`, `engineThrowingIllegalMoveExceptionBecomesMoveRejected` |
| A4.7  | Resign: `resign(matchId, who)` → Resigned + MatchEnded(opposite winner, RESIGN); resign su match FINISHED → IllegalStateException | `ResignFlowTest` (6 test)                                                            |
| A4.8  | Draw offer/response: offer → DrawOffered; accept → DrawAccepted + MatchEnded DRAW_AGREEMENT; decline → DrawDeclined + ONGOING; doppia offer pending → IllegalStateException | `DrawFlowTest` (6 test)                                                              |
| A4.9  | Event bus interno: ogni evento broadcast via Spring `@EventListener(MatchEventEnvelope)`; ordine FIFO per match | `SpringMatchEventBusTest` (4 test)                                                   |
| A4.10 | STOMP-compatible publisher: ogni evento pubblicato su `/topic/match/{id}` via `StompCompatiblePublisher.publishToTopic` | `MatchManagerTurnTest` (verify mock), `SoloMatchEndToEndTest#eventsBroadcastOnBusAndStomp` (BufferingStompPublisher real + topic assertion) |
| A4.11 | TournamentEngine compila + ritorna stub: `createTournament(spec)` + `registerParticipant` funzionano (status CREATED); `startTournament` / `BracketGenerator.generate` / `RoundRobinScheduler.schedule` lanciano `UnsupportedOperationException("deferred to F8/F9")` | `TournamentEngineSkeletonTest` (24 test in 8 nested class)                            |
| A4.12 | In-memory adapter thread-safe: stress 1000 match concorrenti + 10000 mosse parallele → no `ConcurrentModificationException`, sequence ancora monotonico | `InMemoryRepositoryConcurrencyTest` (`@Tag("slow")`) 3 test: 16 thread × 1000 match save (no CME), 16 thread × 10000 appendEvent su 64 match disjoint (sequence strict monotonic per match), 16 thread × 1000 appendEvent stesso match con caller-side lock (synchronized(log) interno regge contesa) — ✅ COVERED in sotto-fase TEST Fase 4 (mvn -pl core-server test -Dgroups=slow -Dtest=InMemoryRepositoryConcurrencyTest 3/3 in 0.435s) |
| A4.13 | Anti-pattern check (CLAUDE.md §8.7-8.8): `core-server` NON dipende da `javafx..`, `org.springframework.boot.web..`, `org.eclipse.jetty..`, `org.apache.tomcat..`, `jakarta.persistence..`, `org.hibernate..`. Bonus: `match` non dipende da `tournament` (post Option F refactor, ADR-042) | `CoreServerArchitectureTest` (5 rule ArchUnit, tutte verdi)                          |
| A4.14 | Spotless OK, SpotBugs 0 High su `core-server`                                                       | Output Maven (`mvn -pl core-server verify`)                                          |
| A4.15 | `package-info.java` per ogni nuovo sotto-package                                                    | `match/`, `match/event/`, `tournament/`, `repository/`, `repository/inmemory/`, `eventbus/`, `stomp/` |
| A4.16 | TRACEABILITY aggiornato: FR-COM-01 (anti-cheat), FR-COM-04 (sequence monotonic), AC §17.1.2 + §17.1.10 (preview F6/F7) | Questo file (sezione corrente)                                                       |
| A4.17 | Nessun TODO/FIXME pending in `core-server/src/main/java/`                                           | grep                                                                                |
| A4.18 | Regression: `RuleEngineCorpusTest` F1 (53 posizioni: 48 corpus + 5 tactical F3.21) + `AiTournamentSimulationTest` F2 (gating ≥95/100) continuano a passare | ✅ COVERED in sotto-fase TEST Fase 4 (E4: `mvn clean verify` root, shared 16:49 min con AI tournament gating verde, RuleEngineCorpusTest parametrizzato 53 posizioni verde) |
| A4.19 | `mvn clean verify` (root, **inclusi** `slow,performance`): BUILD SUCCESS su tutti i moduli           | ✅ COVERED in sotto-fase TEST Fase 4 (E4 2026-05-06: BUILD SUCCESS in 18:55 min, 879 test verdi: shared 391 + core-server 166 + client 321 + server 1) |

---

## Acceptance criteria di Fase 4.5 (PLAN-fase-4.5.md §2.2)

| ID         | Criterio                                                                                                                                                | Verifica                                                                                                                                                                  |
|------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A4.5.1     | `mvn -pl client verify -DexcludedGroups=slow,performance` BUILD SUCCESS                                                                                 | ✅ COVERED — 379/379 test verdi a chiusura Task 4.5.9 (Output Maven 2026-05-06)                                                                                            |
| A4.5.2     | Coverage JaCoCo client ≥ 60% line+branch (gate `haltOnFailure=true` invariato)                                                                          | ✅ COVERED — `mvn -pl client verify` riporta "All coverage checks have been met" ad ogni Task 4.5.x; bundle 74 classi post-merge (era 70 a F4 baseline)                    |
| A4.5.3     | SpotBugs 0 High, Spotless OK                                                                                                                            | ✅ COVERED — output Maven verde ad ogni task closure                                                                                                                       |
| A4.5.4     | Defect originale risolto a 1366×768: scacchiera interamente visibile, no clipping della parte bassa                                                     | ✅ COVERED — `tests/visual-review/responsiveness-baseline-post-fix/board-view_1366x768-laptop.png` (committed sample) + `BoardRendererLayoutTest` parametric 7 viewports + manual demo Task 4.5.16 |
| A4.5.5     | Scacchiera quadrata sempre: `8 × currentCellSize ≤ renderer.w/h`                                                                                       | ✅ COVERED — `BoardRendererLayoutTest.cellsAreCenteredAndFitWithinTheRenderer` parametric 7 viewports + `ResponsivenessParametricTest` board-square assertion 7 res        |
| A4.5.6     | Stage min 1024×720 enforced; initial size 80% primary screen centrato; Stage state v3 restore con fallback 80% se invalid                              | ✅ COVERED — `PrimaryStageInitializerTest` (4 test: minSize, 80%, centering, NPE) + `StagePersistenceCoordinator` orchestration + `StagePersistenceValidatorTest` (9 test) |
| A4.5.7     | Nessun clipping vertical / horizontal in 8 schermate F3.5; nessuna ScrollBar inattesa                                                                  | ✅ COVERED — `ResponsivenessParametricTest` no-clipping assertion (`root.bounds ≤ scene + 1px`) + no-ScrollBar visibile assertion, 7 res × 7 schermate assertable          |
| A4.5.8     | Side panel adattivo: pref=320, min=240, max=400; auto-hide < 1024 NON implementato (Stage min enforce 1024 → unreachable, hook documentato in FXML)    | ✅ COVERED — `SidePanelLayoutTest` (1 property + 6 parametric su 1024/1366/1920/2560/3440/3840 stage widths)                                                               |
| A4.5.9     | Tipografia adattiva: `display-fluid`/`display-fluid-lg` clamp (24-32, 28-48 px); CSS class baseline statici come fallback                              | ✅ COVERED — `JavaFxScalingHelperTest` (12 math + 3 FX-binding) + 6 FXML modificati (splash, sp-setup, settings, load-screen, rules) + `SceneRouter.show` wiring          |
| A4.5.10    | `UiScalingService` 100/125/150 ortogonale al layout responsive                                                                                          | ⚠️ PARTIAL — `ResponsivenessParametricTest` chiama `uiScalingService.applyTo(scene)` nella pipeline (parità con `SceneRouter.show`); ortogonalità esplicita scaling 150% × 1366×768 verificata manualmente in Task 4.5.16 (TEST-PLAN-fase-4.5 §5.2) |
| A4.5.11    | DPI/HiDPI safe (Win 100/125/150/200%); nessun pixel hardcoded                                                                                           | ⚠️ DEFERRED to manual — layout math DPI-independent (single-JVM parametric copre correttezza), varianti DPI verificate da manual launch con `-Dprism.allowhidpi=true -Dglass.win.uiScale=N` (TEST-PLAN-fase-4.5 §5.2) |
| A4.5.12    | Resize fluido (no jank), animazioni in corso non interrotte                                                                                             | ⚠️ DEFERRED to manual — verificato visualmente durante manual demo Task 4.5.16; nessun re-layout cascading nei test parametric                                          |
| A4.5.13    | Anti-pattern #15 preservato: solo modifiche layout/binding/CSS responsive; token CSS v2 + texture + font + animation params invariati                  | ✅ COVERED — `git diff develop..feature/4.5-ui-responsiveness` su `theme-light.css` mostra solo classi additive (`.display-fluid`, `.display-fluid-lg`) e cambio `cover→repeat` (Task 4.5.8); zero modifiche a `BoardRenderer`/`PieceNode`/`MoveAnimator`/`ParticleEffects`/`AudioService` |
| A4.5.14    | `BackgroundImage` REPEAT mode su texture wood (no stretch al resize)                                                                                    | ✅ COVERED — `theme-light.css` `.splash-root`/`.main-menu-root`/`.screen-root` + `components.css` `.board-frame` cambiate da `cover` a `repeat` (Task 4.5.8); cell selectors `100% 100% no-repeat` invariati (corretto per cell-sized backgrounds) |
| A4.5.14b   | Asset audit ≥ 2048×2048; CC0 documentato in `CREDITS.md`                                                                                                | ✅ COVERED — Task 4.5.3b audit PASS: 3/3 texture wood a 2048×2048 (`board_dark.jpg`, `board_light.jpg`, `frame.jpg`), CC0 Poly Haven già documentato in `CREDITS.md` linee 36-38 |
| A4.5.14c   | Aspect ratio ultrawide 21:9 / 32:9: cluster centrato con maxWidth ≤ 70% viewport; Task 4.5.6 + 4.5.4                                                   | ⚠️ PARTIAL — `main-menu`/`sp-setup`/`settings` capati a 960px, `load-screen` a 1100, `rules` SplitPane a 1200, board centrato (Task 4.5.4); Task 4.5.6b skipped per Opzione B utente (manual review confirms ultrawide screenshot post-fix è centrato + tile naturale, no defect critico) |
| A4.5.14d   | Stage state persistence v3: campi `windowWidth/Height/X/Y/Maximized`; restore al launch successivo; fallback 80% se invalid                            | ✅ COVERED — `UserPreferences` schema v3 + `withWindowState` helper + `StagePersistenceValidator.isStateValid` (9 test failure cases) + `StagePersistenceCoordinator` orchestration + `PreferencesServiceTest.migratesSchemaV2FileLeavingWindowFieldsNull` |
| A4.5.15    | TRACEABILITY aggiornato con righe A4.5.x                                                                                                                | ✅ COVERED — questo file (Task 4.5.10)                                                                                                                                     |
| A4.5.16    | Manual demo run su PC cliente / VM: defect risolto + resize libero verificato                                                                          | ⚠️ DEFERRED to TEST sotto-fase — TEST-PLAN-fase-4.5 §7 documenterà l'esito; A/B confronto pre/post-fix screenshot già anchora il fix programmaticamente                  |
| A4.5.17    | Nessun TODO/FIXME pending introdotto in `client/src/main/java/` da F4.5                                                                                  | ✅ COVERED — verifica grep su tutti i nuovi/modificati file Task 4.5.1..4.5.10                                                                                            |
| A4.5.18    | `mvn clean verify` (root, slow + performance inclusi) BUILD SUCCESS                                                                                     | ⚠️ DEFERRED to TEST sotto-fase — regression cross-modulo finale post-implementa, Task 4.5.16/E4.5                                                                         |
| A4.5.19    | `package-info.java` per nuovi sotto-package (`client.layout`)                                                                                            | ✅ COVERED — `client/src/main/java/com/damaitaliana/client/layout/package-info.java` (Task 4.5.10) documenta `JavaFxScalingHelper` + `BoardFrameThicknessHelper`           |

---

## Acceptance criteria di Fase 3.5 (PLAN-fase-3.5.md §2.2)

| ID       | Criterio                                                                                            | Verifica                                                                            |
|----------|-----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| A3.5.1   | `mvn -pl client verify -DexcludedGroups=slow,performance` BUILD SUCCESS                             | Output Maven (321 test, JaCoCo + SpotBugs + Spotless verdi)                          |
| A3.5.2   | Coverage JaCoCo client ≥ 60% line+branch (gate `haltOnFailure=true`)                                | `client/target/site/jacoco/jacoco.csv` + `jacoco-check` execution                    |
| A3.5.3   | SpotBugs 0 High, Spotless OK                                                                        | Output Maven verify                                                                 |
| A3.5.4   | 8 schermate ridisegnate con design tokens v2 (palette wood, font display, bottoni gradient/bevel/glow) | `tests/visual-review/{splash,main-menu,sp-setup,board-game,save-dialog,load-screen,settings,rules}.png` + `ThemeServiceTest#themeLightDefinesWoodPremiumColorTokens`, `themeLightDefinesPrimaryAndSecondaryButtonAndDisplayLabel`, `themeLightDefinesFontFamilyChainsForUiAndDisplay` |
| A3.5.5   | Tavola texture legno + pezzi 3D-look + dame con marker oro/rosso                                    | `tests/visual-review/board-game.png` + `PieceNodeTest` (5 test rendering)            |
| A3.5.6   | Animazione mossa easing OUT_BACK (overshoot)                                                        | `MoveAnimatorTest` (interpolator assertion) + manual demo Task 3.5.14                |
| A3.5.7   | Cattura: particle puff 8-12 marrone/grigie, fade+scale 350ms                                        | `ParticleEffectsTest` (count + lifecycle) + manual demo                              |
| A3.5.8   | Promozione: raggi dorati 8-12 radiali, fade 600ms                                                   | `ParticleEffectsTest` (promotion glow) + manual demo                                 |
| A3.5.9   | Cattura obbligatoria: glow halo oro animato 1200ms                                                  | `ParticleEffectsTest` (mandatoryGlow) + manual demo                                  |
| A3.5.10  | Music shuffle 3-5 tracce orchestrali, default 30%, no overlap, loop continuo                        | `MusicPlaylistTest` (shuffle deterministic, no back-to-back, reset) + `JavaFxAudioServiceTest` (defaults SPEC §13.4) + manual demo |
| A3.5.11  | SFX MOVE/CAPTURE/PROMOTION/VICTORY\|DEFEAT su 4 eventi gameplay, mutabili separatamente             | `SinglePlayerControllerTest` (5 test sfx), `SinglePlayerE2ETest` (capture/promotion/victory/defeat sfx, 4 test), `SfxTest` (6 enum + classpath), `SfxPlaybackSmokeTest` |
| A3.5.12  | Settings: 2 slider Volume musica/effetti + 2 toggle Muto, persisti in `config.json`                 | `SettingsControllerTest` (audio handlers) + `PreferencesServiceTest#migratesSchemaV1FileFillingAudioDefaults` + `JavaFxAudioServiceTest` (persist) + manual `tests/visual-review/settings.png` |
| A3.5.13  | `mvn -pl client -Pinstaller -DskipTests package` produce `Dama Italiana-0.3.5.msi` su Win 10/11    | Manual build Task 3.5.12+3.5.14 + ADR-036                                            |
| A3.5.14  | Asset CC0 o CC-BY (visual+audio); `client/src/main/resources/assets/CREDITS.md` con autore/fonte/licenza | Manual audit (`CREDITS.md` + Task 3.5.1/3.5.4 follow-up) + ADR-037                   |
| A3.5.15  | WCAG AA preservato light theme (contrast text/bg ≥ 4.5:1 sui token critici)                         | Manual check (Coolors/WebAIM) + `tests/visual-review/*.png` regression baseline      |
| A3.5.16  | Nessun TODO/FIXME pending in `client/src/main/java/`                                                | grep                                                                                |
| A3.5.17  | TRACEABILITY aggiornato con A3.5.x                                                                  | Questo file                                                                         |
| A3.5.18  | TEST F3 debt estinto (TEST-PLAN-fase-3.md §2-6,8-10 + manuale §7.4 + regression slow+perf)          | Task 3.5.0 (commit `fc7b68c`), `tests/TEST-PLAN-fase-3.md` finalizzato              |
| A3.5.19  | Manual demo run su Win 10/11 fresca, partita completa, tutti A3.5.x visibili                        | `tests/TEST-PLAN-fase-3.5.md §7` + screenshot baseline                              |
| A3.5.20  | `mvn clean verify` (root) BUILD SUCCESS finale                                                      | Output Maven (regression Task 3.5.17)                                               |
| A3.5.21  | `package-info.java` per nuovi sotto-package (`client.audio`)                                        | `client/src/main/java/com/damaitaliana/client/audio/package-info.java`               |

---

## Acceptance criteria di Fase 3 (PLAN-fase-3.md §2.2)

| ID    | Criterio                                                                                            | Verifica                                                                            |
|-------|-----------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| A3.1  | `mvn -pl client verify` BUILD SUCCESS                                                               | Output Maven (264 test, JaCoCo + SpotBugs + Spotless verdi)                          |
| A3.2  | `mvn -pl client javafx:run` lancia il client; splash → main menu senza eccezioni                    | Manuale (script test plan + screencast in REVIEW)                                   |
| A3.3  | Partita end-to-end vs IA chiusa fino a stato terminale senza crash UI                                | `SinglePlayerE2ETest#humanFirstMoveAdvancesGameStateAndHistory` (light), full game manuale |
| A3.4  | Salva con nome → ricarica → riprendi                                                                 | `SaveLoadE2ETest#saveThenLoadResumesAtSameState`                                    |
| A3.5  | Autosave recovery (chiudi finestra → riapri → prompt riprendi)                                      | `AutosaveE2ETest#promptOnRestartWhenAutosavePresent`                                 |
| A3.6  | Highlight cattura obbligatoria (`pulse-mandatory`)                                                  | `SinglePlayerControllerTest#mandatoryHighlightsRecomputeAfterEachMove` + `BoardRendererTest` |
| A3.7  | Highlight mosse legali al click pedina propria                                                      | `SinglePlayerE2ETest#manCannotCaptureKingInUi` (verifica `highlightLegalTargets`) + `SinglePlayerControllerTest#clickOnOwnPieceHighlightsLegalTargets` |
| A3.8  | Cronologia mosse in notazione FID (alternanza Bianco/Nero)                                           | `MoveHistoryViewModelTest#appendingWhiteMoveCreatesNewRow`, `appendingBlackMoveCompletesRow`, `multipleTurnsProduceMultipleRows` |
| A3.9  | Localizzazione IT/EN funzionante, no stringhe hardcoded                                              | `LocalizationE2ETest` (87 chiavi IT+EN) + `MessageSourceConfigTest#bothBundlesHaveSameKeySet` |
| A3.10 | Sezione regole accessibile + navigazione 7 sezioni                                                   | `RulesScreenE2ETest#openRulesAndNavigateSections`                                    |
| A3.11 | Pedina non cattura dama (end-to-end UI)                                                              | `SinglePlayerE2ETest#manCannotCaptureKingInUi`                                       |
| A3.12 | Promozione termina turno (end-to-end UI)                                                             | `SinglePlayerE2ETest#promotionStopsSequenceInUi`                                     |
| A3.13 | Coverage ≥ 60% line+branch sui package non-view del client                                          | JaCoCo `haltOnFailure=true` (`client/pom.xml`); 9 esclusioni esplicite              |
| A3.14 | Spotless OK, SpotBugs 0 High su `client`                                                            | Output Maven verify                                                                 |
| A3.15 | `package-info.java` per ogni nuovo sotto-package                                                    | `client/.../package-info.java` per app/ui[+menu/setup/board+animation/save/settings/rules/splash]/controller/persistence/i18n |
| A3.16 | TRACEABILITY aggiornato (FR-SP-01..09, FR-RUL-01..05, NFR-U-01, NFR-U-04, NFR-M-02 client, AC §17.1.{1,7,8,9,12}) | Questo file                                                                         |
| A3.17 | Nessun TODO/FIXME pending in `client/src/main/java/`                                                | grep                                                                                |
| A3.18 | Test corpus regole F1 + gating IA F2 continuano a passare (regression)                              | `RuleEngineCorpusTest` + `AiTournamentSimulationTest` verdi                          |
| A3.19 | `mvn clean verify` (root) BUILD SUCCESS                                                             | Output Maven                                                                        |
| A3.20 | Schema versionato saves: `"schemaVersion": 1` + rifiuto versioni ignote                             | `SaveServiceTest#loadFailsOnUnknownSchemaVersion`                                    |
| A3.21 | Atomicità autosave (write-temp + ATOMIC_MOVE)                                                       | `SaveServiceTest#saveOverwritesExistingSlotAtomically` + `saveDoesNotLeaveTempFileBehind` + `AutosaveE2ETest#writeFailureToleratedWhenSavesDirIsAFile` |

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
