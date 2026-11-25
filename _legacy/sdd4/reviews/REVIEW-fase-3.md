# REVIEW — Fase 3: Client UI single-player

- **Data apertura**: 2026-04-30
- **Data chiusura**: 2026-04-30
- **Commit codebase apertura**: `9e83337`
- **Commit di chiusura**: `59bedc7` (head di `feature/3-ui-singleplayer` post-fix)
- **SPEC version**: 2.0 (2026-04-26)
- **Reviewer**: Claude Code

## Sommario

| Categoria       | Critical | High | Medium | Low | Totale | Resolved |
|-----------------|---------:|-----:|-------:|----:|-------:|---------:|
| BLOCKER         |        0 |    0 |      0 |   0 |      0 |        — |
| REQUIREMENT_GAP |        0 |    1 |      1 |   0 |      2 |      2/2 |
| BUG             |        0 |    0 |      0 |   0 |      0 |        — |
| SECURITY        |        0 |    0 |      0 |   0 |      0 |        — |
| PERFORMANCE     |        0 |    0 |      0 |   0 |      0 |        — |
| CODE_QUALITY    |        0 |    0 |      0 |   0 |      0 |        — |
| DOC_GAP         |        0 |    0 |      0 |   3 |      3 |      3/3 |
| **Totale**      |        0 |    1 |      1 |   3 |      5 |  **5/5** |

**Stato complessivo (post-closure)**: tutti e 5 i findings sono **RESOLVED**. F-001 (REQUIREMENT_GAP High, FR-SP-06 Undo/redo) chiuso con implementazione opzione A in Task 3.24 (commit `74de2af`). F-002 (REQUIREMENT_GAP Medium, A3.3 light) chiuso con opzione B — procedura di validazione manuale documentata in `tests/TEST-PLAN-fase-3.md` §7 (commit `603306f`). F-003+F-004+F-005 (3× DOC_GAP Low) chiusi in singolo commit `docs(claude)` (commit `59bedc7`). CR-001 WITHDRAWN.

`mvn -pl client verify -DexcludedGroups=slow,performance` BUILD SUCCESS al commit `74de2af`: 280 test verdi, 0 skipped, JaCoCo client gate ✅ (line 74.18%, branch 60.95%, gate 60% line+branch con `haltOnFailure=true`), SpotBugs 0 High, Spotless OK. Regression `mvn clean verify -DexcludedGroups=slow,performance` (root) BUILD SUCCESS al commit di chiusura `59bedc7`. La regression con tag `slow`+`performance` inclusi è schedulata in apertura della sotto-fase TEST di Fase 3 (corpus regole F1 + gating IA F2 ~16 min).

---

## Acceptance criteria coverage

### SPEC §17 (acceptance globali rilevanti per F3)

| AC ID    | Descrizione                                                                  | Status      | Note |
|----------|------------------------------------------------------------------------------|-------------|------|
| 17.1.1   | Partita SP vs IA Campione si conclude entro 30 min senza crash               | ⚠️ PARTIAL  | F2 valida correttezza+perf (`AiTournamentSimulationTest`); F3 implementa la UI; il full-game UI vs Esperto resta light + manual (F-002 RESOLVED via `tests/TEST-PLAN-fase-3.md` §7) |
| 17.1.7   | Pedina che raggiunge promozione mid-sequence non continua                    | ✅ COVERED   | `SinglePlayerE2ETest#promotionStopsSequenceInUi` + corpus F1 |
| 17.1.8   | Pedina non può catturare la dama                                              | ✅ COVERED   | `SinglePlayerE2ETest#manCannotCaptureKingInUi` + corpus F1 |
| 17.1.9   | Save multi-slot single-player (lista, carica, riprendi)                      | ✅ COVERED   | `SaveLoadE2ETest#saveThenLoadResumesAtSameState` + `LoadScreenControllerTest` |
| 17.1.12  | Sezione regole in-app accessibile e completa                                  | ✅ COVERED   | `RulesScreenE2ETest#openRulesAndNavigateSections` + diagrammi 6/4 sezioni |
| 17.2.3   | Client a 60 FPS durante animazioni                                           | ⚠️ DEFERRED | NFR-P-01 — animazioni JavaFX HW-accelerated, misura formale a Fase 11 |
| 17.2.4   | Coverage ≥ 80% modulo `shared`                                                | ✅ COVERED   | Mantenuto da F1+F2 (gate JaCoCo `haltOnFailure=true`) |
| 17.2.5   | SAST SpotBugs senza warning High                                             | ✅ COVERED   | 0 warning High |
| 17.2.7   | Dark mode + light mode WCAG AA                                                | ⚠️ PARTIAL   | Light mode WCAG AA in F3; dark + verifica tool deferred F11 (NFR-U-04) |

### Acceptance criteria operativi della Fase 3 (PLAN-fase-3 §2.2)

| ID    | Criterio                                                                                          | Status      | Note |
|-------|---------------------------------------------------------------------------------------------------|-------------|------|
| A3.1  | `mvn -pl client verify` BUILD SUCCESS                                                              | ✅           | 280 test verdi (excludedGroups=slow,performance) al commit `74de2af` (post Task 3.24) |
| A3.2  | `mvn -pl client javafx:run` lancia il client; splash → main menu senza eccezioni                  | ⚠️ PENDING   | Verifica manuale richiesta nella REVIEW (screencast / screenshot) |
| A3.3  | Partita end-to-end vs IA Esperto fino a stato terminale senza crash UI                            | ⚠️ PARTIAL   | F-002 RESOLVED opzione B: light test smoke (`humanFirstMoveAdvancesGameStateAndHistory`) + procedura manuale documentata in `tests/TEST-PLAN-fase-3.md` §7 (run schedulato in chiusura sotto-fase TEST) |
| A3.4  | Salva con nome → ricarica → riprendi                                                              | ✅           | `SaveLoadE2ETest#saveThenLoadResumesAtSameState` |
| A3.5  | Autosave recovery (chiudi finestra → riapri → prompt riprendi)                                    | ✅           | `AutosaveE2ETest#promptOnRestartWhenAutosavePresent` + `MainMenuController.handleAutosavePrompt` |
| A3.6  | Highlight cattura obbligatoria (`pulse-mandatory`)                                                | ✅           | `SinglePlayerControllerTest#mandatoryHighlightsRecomputeAfterEachMove` + `BoardRenderer.highlightMandatorySources` line 170 |
| A3.7  | Highlight mosse legali al click pedina propria                                                    | ✅           | `SinglePlayerControllerTest#clickOnOwnPieceHighlightsLegalTargets` + E2E |
| A3.8  | Cronologia mosse in notazione FID                                                                  | ✅           | `MoveHistoryViewModelTest` 9 test su FID format + alternanza Bianco/Nero |
| A3.9  | Localizzazione IT/EN, no stringhe UI hardcoded                                                    | ✅           | `LocalizationE2ETest` 87 chiavi + `MessageSourceConfigTest#bothBundlesHaveSameKeySet` + grep `setText("[A-Z]"` 0 match |
| A3.10 | Sezione regole accessibile + 7 sezioni                                                             | ✅           | `RulesScreenE2ETest#openRulesAndNavigateSections` |
| A3.11 | Pedina non cattura dama (UI)                                                                       | ✅           | `SinglePlayerE2ETest#manCannotCaptureKingInUi` |
| A3.12 | Promozione termina turno (UI)                                                                      | ✅           | `SinglePlayerE2ETest#promotionStopsSequenceInUi` |
| A3.13 | Coverage ≥ 60% line+branch sui package non-view del client                                        | ✅           | Gate `haltOnFailure=true`, line 73.94%, branch 60.55%, 9 esclusioni esplicite (Task 3.22) |
| A3.14 | Spotless OK, SpotBugs 0 High su `client`                                                          | ✅           | confermato |
| A3.15 | `package-info.java` per ogni nuovo sotto-package                                                  | ✅           | 13 file presenti (`app`, `controller`, `i18n`, `persistence`, `ui`, `ui.menu`, `ui.setup`, `ui.board`, `ui.board.animation`, `ui.save`, `ui.settings`, `ui.rules`, `ui.splash`) |
| A3.16 | TRACEABILITY aggiornato per F3                                                                     | ✅           | Drift nomi metodo risolto al commit `59bedc7` (F-003 RESOLVED); FR-SP-06 row aggiornata con coverage Task 3.24 |
| A3.17 | Nessun TODO/FIXME pending in `client/src/main/java/`                                              | ✅           | grep su `TODO\|FIXME\|XXX\|HACK` → 0 match |
| A3.18 | Test corpus regole F1 + gating IA F2 (regression)                                                 | ⚠️ DEFERRED  | Verifica con tag `slow`+`performance` schedulata in apertura sotto-fase TEST F3 (~16 min); senza i tag confermata da `mvn clean verify -DexcludedGroups=slow,performance` BUILD SUCCESS root al commit `59bedc7` (closure REVIEW) |
| A3.19 | `mvn clean verify` (root) BUILD SUCCESS                                                            | ✅           | Confermato `mvn clean verify -DexcludedGroups=slow,performance` BUILD SUCCESS root al commit `59bedc7` (`shared`+`core-server`+`client`+`server` tutti verdi) |
| A3.20 | Schema versionato saves: `"schemaVersion": 1` + rifiuto versioni ignote                            | ✅           | `SaveServiceTest#loadFailsOnUnknownSchemaVersion` + `SavedGame.CURRENT_SCHEMA_VERSION = 1` |
| A3.21 | Atomicità autosave (write-temp + ATOMIC_MOVE)                                                     | ✅           | `SaveServiceTest#saveOverwritesExistingSlotAtomically` + `saveDoesNotLeaveTempFileBehind` + fallback `AtomicMoveNotSupportedException` documentato in `SaveService.save` |

### Requisiti SPEC funzionali coperti (FR)

| FR ID    | Status     | Note |
|----------|------------|------|
| FR-SP-01 | ✅ COVERED  | Bootstrap offline; main menu → setup → board, no auth |
| FR-SP-02 | ✅ COVERED  | `SinglePlayerSetupController` 3 RadioButton mappati su `AiLevel` enum |
| FR-SP-03 | ✅ COVERED  | Scelta colore Bianco/Nero/Casuale via `ColorChoice` |
| FR-SP-04 | ✅ COVERED  | Highlight `legal-target` su `selectPiece` (SinglePlayerController:192) |
| FR-SP-05 | ✅ COVERED  | Highlight `pulse-mandatory` (CSS) + `refreshMandatoryHighlights` (SinglePlayerController:281) |
| FR-SP-06 | ✅ COVERED  | Implementato in Task 3.24 (commit `74de2af`) — vedi F-001 RESOLVED. Coperto da `SinglePlayerControllerTest` (9 test), `BoardViewControllerTest` (3 test), `MoveHistoryViewModelTest#replaceWithHistory*` (3 test), `SinglePlayerE2ETest#undoRedoCycleRestoresAndReappliesHumanMove`. |
| FR-SP-07 | ✅ COVERED  | SaveService multi-slot + LoadScreenController + miniature |
| FR-SP-08 | ✅ COVERED  | AutosaveService + SinglePlayerAutosaveTrigger + MainMenuController.handleAutosavePrompt |
| FR-SP-09 | ✅ COVERED  | MoveHistoryViewModel + FidNotation, alternanza Bianco/Nero |
| FR-RUL-01 | ✅ COVERED | Card "Regole" main menu + MenuItem "board.menu.rules" in board view |
| FR-RUL-02 | ✅ COVERED | 7 sezioni in `RuleSection.ALL` (ordine SPEC §3) |
| FR-RUL-03 | ✅ COVERED | 6 diagrammi su 4 sezioni (≥ 5 su ≥ 4) — `RuleDiagramLoaderTest#coverageHasAtLeastFiveDiagramsAcrossAtLeastFourSections` |
| FR-RUL-04 | ✅ COVERED | `RulesAnimations` 3 Kind (SIMPLE_CAPTURE, MULTI_CAPTURE, PROMOTION) |
| FR-RUL-05 | ✅ COVERED | Tutti i body+title+caption+animation localizzati IT/EN, parità enforced da `MessageSourceConfigTest` |

### Requisiti SPEC non funzionali coperti (NFR)

| NFR ID   | Status     | Note |
|----------|------------|------|
| NFR-P-01 | ⚠️ DEFERRED | 60 FPS misura formale a F11; animazioni JavaFX HW-accelerated |
| NFR-U-01 | ✅ COVERED  | MessageSource + LocaleService + I18n; `LocalizationE2ETest` |
| NFR-U-02 | ⚠️ PARTIAL  | Light mode wired; dark mode stub `theme-dark.css` non selezionabile (toggle F11) |
| NFR-U-03 | ⚠️ PARTIAL  | `accessibleText` su pezzi/case + keyboard nav board (Task 3.20); ma testi a11y in inglese hardcoded (vedi F-005) |
| NFR-U-04 | ⚠️ PARTIAL  | Light mode WCAG AA per design (manuale); tool-based check + dark a F11 |
| NFR-M-02 | ✅ COVERED  | JaCoCo gate 60% line+branch, `haltOnFailure=true`, esclusioni esplicite |
| NFR-M-04 | ✅ COVERED  | Spotless Google Java Style |

---

## Findings

### F-001 — [REQUIREMENT_GAP, High] FR-SP-06 Undo/redo non implementato

- **Posizione**: assenza di feature in:
  - `client/src/main/java/com/damaitaliana/client/controller/SinglePlayerController.java` — non c'è `undo()`/`redo()` sul controller né cronologia di stati precedenti.
  - `client/src/main/resources/fxml/board-view.fxml` — non ci sono pulsanti / shortcut.
  - `client/src/main/resources/i18n/messages_*.properties` — non esistono chiavi `board.menu.undo`/`redo`.
  - grep su `undo|redo` (case-insensitive) in `client/src/main/java/`: 1 match, ed è un commento in `BoardViewController:88` (la parola "undone" in un commento sul `Platform.runLater` deferral, niente a che vedere con la feature).
- **SPEC reference**: SPEC §4.1 line 161 — `FR-SP-06 | Undo/redo illimitato.`
- **PLAN reference**: `plans/PLAN-fase-3.md` §3.1 dichiara FR-SP-06 "Coperto" e §7.13 (stop point approvato in blocco opzione A) fissa la semantica "undo annulla la coppia (mossa umana + risposta IA) come unità". Nessun task della decomposizione (3.1..3.23) implementa effettivamente la feature.
- **Descrizione**: SPEC marca FR-SP-06 come obbligatorio nel set Single-Player (§4.1). Il PLAN ha confermato la semantica nel §7.13 ma la decomposizione in task (3.1..3.23) non include un task dedicato a undo/redo. La sotto-fase IMPLEMENTA è arrivata in fondo senza che la feature venga scritta. Impatto utente: il giocatore non può rivedere la propria mossa né annullare la risposta IA.
- **Proposta di fix** — due opzioni da discutere con l'utente:
  - **Opzione A (implementazione in F3)**: aggiungere un nuovo task **3.24** (3-5 sottotask) che (1) tiene una `Deque<GameState>` di history nel `SinglePlayerController`, (2) espone `undoPair()` / `redoPair()` con la semantica "annulla coppia (umana+IA)" del §7.13, (3) wira due `MenuItem` Indietro/Avanti in `board-view.fxml` con shortcut `Ctrl+Z`/`Ctrl+Y` + chiavi i18n `board.menu.undo` / `board.menu.redo`, (4) test unit + 1 E2E. Stima: ~6-8 ore di lavoro.
  - **Opzione B (deferral con SPEC change request)**: aprire CR-001 per spostare FR-SP-06 alla Fase 11 (Polish & UX). Motivazione: la feature non è bloccante per il flusso "gioca → salva → riprendi" che SPEC §16 Fase 3 elegge come acceptance principale; concentra in F3 il valore che è già pronto (264 test verdi).
- **Decisione utente** (2026-04-30): **opzione A** scelta — implementazione immediata in F3 come Task 3.24.
- **Status**: ✅ **RESOLVED** al commit `74de2af` (`feat(client): implement undo/redo of human+ai move pairs`). Implementati `Deque<GameState>` undo/redo stacks in `SinglePlayerController`, API pubblica `undoPair()`/`redoPair()`/`canUndo()`/`canRedo()`/`undoState()`, observable `UndoState` (BiConsumer pattern come `AiThinkingState`), `MenuItem` Indietro/Avanti in `board-view.fxml` con accelerator `Ctrl+Z`/`Ctrl+Y` (`KeyCombination.SHORTCUT_DOWN` cross-platform), chiavi i18n `board.menu.undo`/`board.menu.redo` in IT+EN, `MoveHistoryViewModel.replaceWithHistory(List<Move>)` per ricostruire il pannello. Coverage: 9 unit test in `SinglePlayerControllerTest`, 3 in `BoardViewControllerTest`, 3 in `MoveHistoryViewModelTest`, 1 E2E in `SinglePlayerE2ETest#undoRedoCycleRestoresAndReappliesHumanMove`. Build post-fix: 280 test verdi (+16), JaCoCo line 74.18%/branch 60.95%, SpotBugs 0 High, Spotless OK.

---

### F-002 — [REQUIREMENT_GAP, Medium] A3.3 partita end-to-end vs IA Esperto coperta solo "light"

- **Posizione**: `client/src/test/java/com/damaitaliana/client/controller/SinglePlayerE2ETest.java:103-119` (`humanFirstMoveAdvancesGameStateAndHistory`).
- **PLAN reference**: `plans/PLAN-fase-3.md` §2.2 A3.3 dice "Partita end-to-end vs IA Esperto chiusa fino a stato terminale (vittoria, sconfitta o patta) **senza crash UI**" e §4 Task 3.21 menziona esplicitamente `SinglePlayerE2ETest#completesGameVsEsperto` con `@Tag("slow")`.
- **Descrizione**: il test implementato gioca **una sola mezza-mossa** del bianco e verifica `sideToMove==BLACK` + `history.size==1` + `status.isOngoing()`. L'AI loop non viene mai esercitato perché `AutosaveTrigger` e `BoardRenderer` sono Mockito-mocked e nessun `AiTurnService` è wirato (è `Optional.empty()` nel costruttore). Manca: la validazione che il loop "umano gioca → IA risponde → umano gioca → ..." non crashi su una partita reale fino a stato terminale.
- **Proposta di fix** — tre opzioni:
  - **Opzione A (test slow completo)**: aggiungere `completesGameVsEsperto` in `SinglePlayerE2ETest` con `@Tag("slow")`, `AiTurnService` reale, mosse del lato umano via `RuleEngine.legalMoves(state).first`, `whenComplete` su `pendingAiRequest` con `CountDownLatch`, durata stimata 1-3 minuti per partita. Esegue su demand (`-Dgroups=slow`). Stima: ~3-4 ore.
  - **Opzione B (test light + manuale)**: lasciare il test light come "smoke", aggiungere un punto al `tests/TEST-PLAN-fase-3.md` per la validazione manuale "partita completa vs Esperto" come parte della closure REVIEW. Marcare A3.3 come ✅ via verifica manuale in REVIEW.
  - **Opzione C (deferral)**: SPEC change request per accettare che A3.3 sia validato "tramite le primitive coperte (E2E ply singolo + corpus IA F2)" senza un test di partita completa. Più rischioso (regressione di interaction loop non rilevata).
- **Decisione utente** (2026-04-30): **opzione B** scelta — light test rimane come smoke + validazione manuale documentata in `tests/TEST-PLAN-fase-3.md`.
- **Status**: ✅ **RESOLVED** al commit `603306f` (`docs(tests): seed TEST-PLAN-fase-3 with manual a3.3 validation procedure`). La procedura operativa di validazione manuale è documentata in `tests/TEST-PLAN-fase-3.md` §7 (procedura step-by-step, success criteria, log-slot §7.4). L'esecuzione del run manuale è schedulata come ultimo step della closure della sotto-fase TEST di Fase 3 (vedi §7.4 quando popolato).

---

### F-003 — [DOC_GAP, Low] TRACEABILITY referenzia metodi che non esistono

- **Posizione**: `tests/TRACEABILITY.md` sezione "Acceptance criteria di Fase 3 (PLAN-fase-3.md §2.2)".
- **Descrizione**: alcune righe della matrice puntano a nomi metodo che vengono dal piano ma non sono mai stati implementati:
  - A3.6 / FR-SP-05 → `BoardInteractionTest#mandatoryCaptureIsHighlighted` non esiste; copertura reale `SinglePlayerControllerTest#mandatoryHighlightsRecomputeAfterEachMove`.
  - A3.7 / FR-SP-04 → `BoardInteractionTest#legalTargetsHighlightedOnSelect` non esiste; copertura reale `SinglePlayerControllerTest#clickOnOwnPieceHighlightsLegalTargets`.
  - A3.8 / FR-SP-09 → `MoveHistoryControllerTest#displaysMovesInFidNotation` non esiste; copertura reale `MoveHistoryViewModelTest` (9 test).
  - A3.20 → `SaveServiceTest#rejectsUnknownSchemaVersion` (plan name) → vero `SaveServiceTest#loadFailsOnUnknownSchemaVersion`.
  - A3.21 → `SaveServiceTest#autosaveIsAtomic` (plan name) → vero `SaveServiceTest#saveOverwritesExistingSlotAtomically` + `saveDoesNotLeaveTempFileBehind`.
  - FR-SP-01 / FR-SP-02 / FR-SP-03 → riga aggiunta da Task 3.23 con copertura "manuale", legittima ma da spiegare meglio nelle note.
- **Proposta di fix**: micro-edit di `tests/TRACEABILITY.md` sostituendo i nomi non esistenti con quelli reali. Lavoro ~10 minuti, no test code change.
- **Decisione utente** (2026-04-30): fix puntuale in singolo commit `docs(claude)` durante closure REVIEW (insieme a F-004 e F-005).
- **Status**: ✅ **RESOLVED** al commit `59bedc7` (`docs(claude): apply review fase 3 doc fixes (f-003 f-004 f-005)`). Aggiornati nomi metodo nelle righe FR-SP-05/06/09 e A3.6/7/8/20/21; FR-SP-06 ora referenzia il corpus di test del Task 3.24 (commit `74de2af`).

---

### F-004 — [DOC_GAP, Low] ADR-031 descrive `kind` come `"manual"|"autosave"` ma implementazione usa `"SINGLE_PLAYER_GAME"` per entrambi

- **Posizione**:
  - `ARCHITECTURE.md` ADR-031 (testo aggiunto al commit `9e83337` durante Task 3.23).
  - `client/src/main/java/com/damaitaliana/client/persistence/SavedGame.java:39` — `KIND_SINGLE_PLAYER = "SINGLE_PLAYER_GAME"`, l'unico valore usato sia da `SavedGame.of(...)` (manual) sia da `AutosaveService.writeAutosave` (che chiama lo stesso `SavedGame.of`).
  - `client/src/main/java/com/damaitaliana/client/persistence/AutosaveService.java:67-69` — non distingue `kind`.
- **Descrizione**: nello scrivere ADR-031 ho descritto i valori del campo `kind` come `"manual"|"autosave"` che sembravano l'intenzione naturale, ma l'implementazione effettiva è univoca: tutti i salvataggi scrivono `kind="SINGLE_PLAYER_GAME"` e la distinzione manual/autosave passa attraverso il **nome del file** (`_autosave.json` reserved slot, escluso da `listSlots()` in `SaveService:144`). L'ADR è quindi documentazione errata.
- **Proposta di fix**: aggiornare ADR-031 in `ARCHITECTURE.md` chiarendo che `kind="SINGLE_PLAYER_GAME"` (futuro: discriminante per match LAN/Internet quando arriveranno in F6/F7), e che la differenza manuale↔autosave è data dal nome del file. Aggiungere una `KIND_*` constant dedicata se in futuro si vorranno discriminare. Lavoro ~10 minuti.
- **Decisione utente** (2026-04-30): fix puntuale in singolo commit `docs(claude)` durante closure REVIEW (insieme a F-003 e F-005).
- **Status**: ✅ **RESOLVED** al commit `59bedc7`. ADR-031 ora descrive `kind` come univoco `"SINGLE_PLAYER_GAME"` in F3 con riserva di valori distinti per match LAN/Internet in F6/F7; la distinzione manuale ↔ autosave è codificata nel nome del file (`_autosave.json` slot riservato escluso da `SaveService.listSlots`).

---

### F-005 — [DOC_GAP, Low] Testi `accessibleText` hardcoded in inglese, non localizzati

- **Posizione**:
  - `client/src/main/java/com/damaitaliana/client/ui/board/BoardRenderer.java:46` — `setAccessibleText("Italian draughts board, 8 by 8 grid");`
  - `client/src/main/java/com/damaitaliana/client/ui/board/CellAccessibleText.java` — formato `"Light square (not playable)"`, `"Dark square <FID>, empty"`, `"Dark square <FID>, ..."`.
  - `client/src/main/java/com/damaitaliana/client/ui/board/PieceAccessibleText.java` — formato `"White man on FID 22"` etc.
- **Descrizione**: SPEC NFR-U-01 richiede IT/EN runtime. PLAN-fase-3 §3.2 NFR-U-03 marca a11y come "Parziale" e CHANGELOG Task 3.20 documenta esplicitamente la decisione: "format inglese-only stile `PieceAccessibleText` (Fase 3 — localizzazione a11y in F11)". È un trade-off conscio per non aggiungere ~30 chiavi i18n in F3 (ogni screen-reader cell read riproduce la stessa frase). Ma la documentazione utente non lo menziona, e un AC reviewer attento (NFR-U-03) potrebbe segnalarlo come non-conformità.
- **Proposta di fix**: documentare nel `tests/TEST-PLAN-fase-3.md` come "deferral conscio" + aggiungere un commento in `CellAccessibleText.java`/`PieceAccessibleText.java` / BoardRenderer che cita "F11 i18n a11y". Niente cambio runtime.
- **Decisione utente** (2026-04-30): fix puntuale in singolo commit `docs(claude)` durante closure REVIEW (insieme a F-003 e F-004).
- **Status**: ✅ **RESOLVED** al commit `59bedc7`. Aggiunto commento esplicito accanto a `BoardRenderer.setAccessibleText` (riga 46-48) che richiama il deferral F11; `CellAccessibleText` e `PieceAccessibleText` già documentavano lo stesso trade-off in Javadoc. Nessun cambio runtime.

---

## SPEC change requests

> Vuota se nessun finding suggerisce di modificare lo SPEC.

### CR-001 — Eventuale deferral di FR-SP-06 (Undo/redo) a Fase 11

- **Contesto**: vedi finding F-001. Se l'utente sceglie l'opzione B (deferral), formalizzare lo spostamento dello scope in SPEC §16 Fase 11 e aggiornare §4.1 con annotazione "(deferred F11)" su FR-SP-06.
- **Proposta**: aggiungere a SPEC §16 Fase 11 la voce "FR-SP-06 Undo/redo illimitato (deferred da F3)". Mantenere FR-SP-06 nella tabella §4.1 con una nota.
- **Decisione utente** (2026-04-30): **WITHDRAWN** — l'utente ha scelto l'opzione A per F-001, quindi FR-SP-06 è implementato in F3 e nessuna modifica allo SPEC è necessaria.

---

## Closure

- [x] Tutti i `BLOCKER` risolti — N/A, nessuno presente
- [x] Tutti i `REQUIREMENT_GAP` risolti — F-001 RESOLVED (`74de2af`, opzione A), F-002 RESOLVED (`603306f`, opzione B)
- [x] Tutti i `Critical/High` `BUG` risolti — N/A
- [x] Tutti i `Critical/High` `SECURITY` risolti — N/A
- [x] `PERFORMANCE` che violano NFR risolti — N/A
- [x] SPEC change requests con stato non-PENDING — CR-001 WITHDRAWN (vedi sopra)
- [x] `mvn clean verify -DexcludedGroups=slow,performance` (root) BUILD SUCCESS al commit `59bedc7`
- [ ] `mvn clean verify` (root) con tag `slow`+`performance` inclusi — DEFERRED in apertura sotto-fase TEST F3 (~16 min, valida corpus regole F1 + gating IA F2)

**Review chiusa il**: 2026-04-30
**Commit di chiusura**: `59bedc7` (`docs(claude): apply review fase 3 doc fixes (f-003 f-004 f-005)`)
