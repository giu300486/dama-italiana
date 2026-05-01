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
