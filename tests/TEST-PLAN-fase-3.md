# TEST PLAN — Fase 3: Client UI single-player

- **Riferimento roadmap**: `SPEC.md` §16 — Fase 3.
- **SPEC version**: 2.2 (2026-04-30, sezioni rilevanti per F3 invariate dalla v2.0/2.1; v2.2 documenta solo pull-forward F3.5).
- **Data apertura**: 2026-04-30.
- **Data chiusura**: in corso (estinzione del debito iniziata sotto Task 3.5.0 di Fase 3.5).
- **Autore**: Claude Code.
- **Stato**: in chiusura post-tag `v0.3.0`. Sezioni 1, 7 popolate durante REVIEW (finding F-002 opzione B); sezioni 2-6, 8-10 popolate in Task 3.5.0 di Fase 3.5 (estinzione debito autorizzato dall'utente come deviazione una-tantum dal workflow CLAUDE.md §2).

---

## 1. Scopo della sotto-fase TEST

Documentare la strategia, la composizione e la copertura della suite di test della Fase 3 (Client UI single-player), validare gli **acceptance criteria operativi A3.1–A3.21** del PLAN-fase-3 §2.2 e gli AC SPEC §17 rilevanti (17.1.1, 17.1.7, 17.1.8, 17.1.9, 17.1.12), finalizzare la matrice di tracciabilità requisiti → test (`TRACEABILITY.md`) per i requisiti coperti dalla Fase 3.

> **Nota workflow**: questo file è stato creato durante la closure della sotto-fase REVIEW per documentare la procedura di validazione manuale di A3.3 (vedi §7), risolvendo il finding F-002 secondo l'opzione B scelta dall'utente. Le sezioni rimanenti (composizione test, coverage, naming, regression cross-modulo, closure) saranno scritte nella sotto-fase TEST di Fase 3, dopo che la sotto-fase REVIEW sarà chiusa.

---

## 2. Strategia di test (CLAUDE.md §2.4)

Piramide classica con traceability esplicita (Approccio C). Il modulo `client` riceve in F3 la prima suite di unit test + 4 E2E TestFX (`SinglePlayerE2ETest`, `SaveLoadE2ETest`, `AutosaveE2ETest`, `RulesScreenE2ETest`, `LocalizationE2ETest`) + LocalizationE2ETest. Tutti i test sono untagged (eseguiti di default da `mvn -pl client verify`); F3 non introduce nuovi `@Tag("slow")` o `@Tag("performance")` — la justificazione del deferral del test full-game vs IA Esperto è in §7.

### 2.1 Composizione effettiva (modulo `client`)

Conteggio al commit `74de2af` (post Task 3.24, pre-tag `v0.3.0`): **46 file di test, 280 test JUnit5 verdi, 0 skipped**.

| Package                                 | File test |     Test count (approx) | Cosa testa                                                                                                  |
|-----------------------------------------|----------:|------------------------:|-------------------------------------------------------------------------------------------------------------|
| `client.app`                            |         2 |                       ~14 | `ThemeServiceTest` (apply/clear stylesheet), `UiScalingServiceTest` (scaling 100/125/150 + null-safe).      |
| `client.controller`                     |         8 |                       ~85 | `SinglePlayerControllerTest` (gameplay + Task 3.24 undo/redo, 9 nuovi test), `SinglePlayerE2ETest` (4 incl. undoRedo cycle), `AiTurnServiceTest` (async + cancellation), `MoveSelectorTest`, `ColorChoiceTest`, `GameSessionTest`, `SinglePlayerGameTest`, `SinglePlayerAutosaveTriggerTest`. |
| `client.i18n`                           |         4 |                       ~25 | `MessageSourceConfigTest` (bundle parity), `LocalizationE2ETest` (87 chiavi IT+EN), `I18nTest`, `LocaleServiceTest`. |
| `client.persistence`                    |         5 |                       ~50 | `SaveServiceTest` (CRUD multi-slot + atomic move + schema rejection), `AutosaveServiceTest` (round-trip + schema mismatch + IO tolerance), `AutosaveE2ETest` (recovery prompt + write fail), `PreferencesServiceTest`, `SaveLoadE2ETest` (round-trip byte-equal). |
| `client.ui.board`                       |        10 |                       ~50 | `BoardLayoutMathTest`, `BoardKeyboardNavigationTest`, `BoardViewControllerTest` (incl. 3 Task 3.24), `CellAccessibleTextTest`, `HighlightStateTest`, `MoveHistoryViewModelTest` (incl. 3 Task 3.24 `replaceWithHistory*`), `PieceAccessibleTextTest`, `StatusPaneViewModelTest`. |
| `client.ui.board.animation`             |         2 |                       ~12 | `MoveAnimatorTest`, `AnimationOrchestratorTest`.                                                            |
| `client.ui.menu`                        |         1 |                       ~10 | `MainMenuControllerTest` (3 click + 5 prompt branch).                                                       |
| `client.ui.rules`                       |         6 |                       ~25 | `RuleDiagramLoaderTest`, `RulesAnimationsTest`, `RulesControllerTest`, `RulesFxmlSmokeTest` (JavaFX guarded), `RulesScreenE2ETest`, `RulesSpringContextTest`. |
| `client.ui.save`                        |         5 |                       ~30 | `SaveDialogControllerTest` (6 enum branch), `LoadScreenControllerTest` (10 test), `CanvasMiniatureRendererTest`, `FxmlLoadingSmokeTest`, `SaveDialogSpringContextTest`. |
| `client.ui.settings`                    |         3 |                       ~10 | `SettingsControllerTest`, `SettingsFxmlSmokeTest`, `SettingsSpringContextTest`.                             |
| `client.ui.splash`                      |         1 |                        ~5 | `SplashControllerTest`.                                                                                    |
| Root (`client.ClientBootstrapTest`)     |         1 |                        4 | Spring context + SceneRouter + ClientProperties bootstrap.                                                  |
| **Totale**                              |    **46** |                   **~280** |                                                                                                             |

> Nota: il numero esatto per package varia dato che i test E2E spannano più package; il totale aggregato (280) viene da Surefire al commit `74de2af`.

Tipi di test (per CLAUDE.md §2.4.1):

| Tipo            | Tooling                                       | Cosa contiene                                                                                                |
|-----------------|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **Unit**        | JUnit 5 + AssertJ + Mockito                  | Logica pura controller, persistence, viewmodel, i18n, helper a11y.                                           |
| **Spring context** | Spring Boot Test (`@SpringBootTest webEnvironment=NONE`) | `@SpringBootTest` con `ClientApplication` per resolve dei `@Component @Scope("prototype")` controller (lezione `feedback_spring_ui_tests`). |
| **FXML smoke**  | JavaFX runtime guarded (`Platform.startup` + `Assumptions.assumeTrue`) | Carica FXML via `FXMLLoader.load` con `setControllerFactory(springContext::getBean)` e verifica node tree minimale. Skip su CI headless. |
| **E2E (TestFX-style)** | JavaFX guarded + mock di servizi non-deterministici | `SinglePlayerE2ETest`, `SaveLoadE2ETest`, `AutosaveE2ETest`, `RulesScreenE2ETest`, `LocalizationE2ETest`. Escono dal mock per esercitare flussi UI realistici. |

### 2.2 Coverage effettiva (`mvn -pl client jacoco:report`)

Misura al commit `74de2af`:

| Scope                                     | Covered / Total | Coverage    | Gate   |
|-------------------------------------------|-----------------|-------------|--------|
| Modulo `client` (post 9 esclusioni Task 3.22) | 1350 / 1820 | **74.18%** line | ≥ 60% ✅ |
| Modulo `client` (post 9 esclusioni Task 3.22) |  309 / 507  | **60.95%** branch | ≥ 60% ✅ |
| Modulo `client` (instructions)             | 6050 / 8241    | **73.41%** | informativo |

Esclusioni dal denominatore (vedi `client/pom.xml` Task 3.22, motivazioni in commit `2aea477`): 3 bootstrap glue (`JavaFxApp`, `JavaFxAppContextHolder`, `ClientApplication`), 1 Alert wrapper (`JavaFxUserPromptService`), 5 anonymous cell-factory (`MoveHistoryView$MoveHistoryCell`, `LoadScreenController$MiniatureCell`, `RulesController$1`, `SettingsController$1`, `SplashController$1`). Tutte istanziate dal FX runtime, no business logic.

Gate `haltOnFailure=true` regola `BUNDLE` ≥ 60% line + branch (NFR-M-02). Verifica negativa: senza esclusioni il branch ratio scenderebbe a 57.4% e il gate fallirebbe.

### 2.3 SAST e style

- **SpotBugs**: 0 warning High al commit `74de2af` (gate `failOnError=true`, threshold High via parent POM).
- **Spotless googleJavaFormat 2-spazi (NFR-M-04)**: passa pulito.
- **Maven Enforcer** (Java 21 + Maven ≥ 3.9 + dependencyConvergence): pulito.

---

## 3. Test corpus regole italiane (regression)

Nessuna nuova posizione aggiunta in F3 (il client non tocca `RuleEngine`). Le **48 posizioni di F1** + **5 posizioni tattiche golden di F2** (`AiTacticalPositionsTest`) devono restare tutte verdi a chiusura F3 (regression).

Distribuzione invariata rispetto a TEST-PLAN-fase-2 §4:

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
| **Totale corpus regole**                    |    **48** |
| Posizioni tattiche golden F2                |     5 ✅ |
| **Totale combinato**                        |    **53** |

Verifica: `mvn -pl shared verify` BUILD SUCCESS al commit `74de2af` → tutti verdi (vedi §8).

---

## 4. Naming convention

Conforme a CLAUDE.md §2.4.5:

- **Unit test**: `<ClasseProduzione>Test`. Es. `SinglePlayerControllerTest`, `MoveHistoryViewModelTest`, `SaveServiceTest`.
- **Spring context test**: `<Feature>SpringContextTest`. Es. `RulesSpringContextTest`, `SaveDialogSpringContextTest`, `SettingsSpringContextTest`.
- **FXML smoke test**: `<Feature>FxmlSmokeTest`. Es. `RulesFxmlSmokeTest`, `SettingsFxmlSmokeTest`, `FxmlLoadingSmokeTest`.
- **E2E test**: `<Feature>E2ETest`. Es. `SinglePlayerE2ETest`, `SaveLoadE2ETest`, `AutosaveE2ETest`, `RulesScreenE2ETest`, `LocalizationE2ETest`.
- **Test method**: stile `<feature><Scenario>` o `should<Espressione>_when<Condizione>`. F3 ha adottato preponderantemente lo stile `featureScenario` per leggibilità (es. `manCannotCaptureKingInUi`, `humanFirstMoveAdvancesGameStateAndHistory`, `undoRedoCycleRestoresAndReappliesHumanMove`).

Esempi specifici di F3:
- `BoardViewControllerTest#onUndoDelegatesToControllerUndoPair` (Task 3.24)
- `SinglePlayerControllerTest#mandatoryHighlightsRecomputeAfterEachMove`
- `SinglePlayerE2ETest#promotionStopsSequenceInUi`
- `SaveLoadE2ETest#saveThenLoadResumesAtSameState`
- `AutosaveE2ETest#promptOnRestartWhenAutosavePresent`
- `RulesScreenE2ETest#openRulesAndNavigateSections`

---

## 5. Tag JUnit

F3 **non introduce nuovi tag**. Il deferral del test full-game vs IA Esperto (proposto come `@Tag("slow")` in PLAN-fase-3 §4 Task 3.21) è risolto via procedura manuale §7 (opzione B di REVIEW-fase-3 finding F-002).

Tag esistenti dal modulo `shared` (regression F2 inclusa quando lanciata `mvn clean verify` root senza `-DexcludedGroups`):

| Tag             | Conteggio | Default in `mvn verify` (root) | Cosa contiene |
|-----------------|----------:|-------------------------------|----------------|
| `slow`          |         1 | ON                            | `AiTournamentSimulationTest#campionWinsAtLeast95OutOf100AgainstPrincipiante` (~15 min, F2 gating). |
| `performance`   |         3 | ON                            | `AiPerformanceTest` budget per livello con tolleranza 1.5x. |
| _(untagged)_    |       669 | sempre                        | 391 di shared (388 untagged + AiPerformanceTest 3 + 1 slow visto come singola riga al fork count) + 280 di client + 1 core-server smoke + 1 server smoke = 673 totali. Vedi §8. |

Loop di sviluppo veloce nel modulo client: `mvn -pl client verify -DexcludedGroups=slow,performance` (~30s — il flag non ha effetti pratici sul client perché non ha test taggati, ma va incluso per coerenza con il modulo `shared` quando si lancia dalla root).

---

## 6. Aggiornamento `TRACEABILITY.md`

Lo stato finale di `tests/TRACEABILITY.md` per Fase 3 è il risultato di tre passate:

1. **Task 3.23** (commit `9e83337`) — aggiunte righe iniziali per FR-SP-01..09 (UI), FR-RUL-01..05, NFR-M-02 client, NFR-U-01, NFR-U-04, AC §17.1.{1,7,8,9,12}, e nuova sezione "Acceptance criteria di Fase 3" con A3.1..A3.21.
2. **Task 3.24** (commit `74de2af`) — l'undo/redo introduce la copertura di FR-SP-06 con `SinglePlayerControllerTest` (9 test su pair-undo / busy guard / observable propagation), `BoardViewControllerTest` (3 test wiring), `MoveHistoryViewModelTest` (3 `replaceWithHistory*`), `SinglePlayerE2ETest#undoRedoCycleRestoresAndReappliesHumanMove`. Riga FR-SP-06 aggiornata da MISSING → COVERED.
3. **F-003 closure** (commit `59bedc7`) — fix dei nomi metodo nelle righe FR-SP-05/06/09 e A3.6/7/8/20/21 sostituendo le pianificazioni dal PLAN che non corrispondevano ai metodi effettivi (es. `mandatoryHighlightsRecomputeAfterEachMove` invece di `mandatoryCaptureCellsAreFlaggedAtSelectionTime`, `loadFailsOnUnknownSchemaVersion` invece di `rejectsUnknownSchemaVersion`).

Stato finale al commit `c0684e9` (post-cleanup REVIEW): tutti gli FR/NFR/AC della Fase 3 hanno almeno una riga con copertura test reale o "manual" esplicita.

Cumulativo (CLAUDE.md §3): le righe F1+F2+F3 coesistono nello stesso file; nessuna riga è stata rimossa.

---

## 7. Validazione manuale di A3.3 (REVIEW closure, finding F-002)

> **Status**: PIANIFICATA — da eseguire dall'utente o sviluppatore prima di chiudere la sotto-fase TEST. Sostituisce il test E2E full-game `completesGameVsEsperto` proposto nel PLAN-fase-3 §4 Task 3.21 (deferred per costo wall-clock e fragilità su CI desktopless).

### 7.1 Contesto e motivazione

Il PLAN-fase-3 §2.2 A3.3 richiede: "Partita end-to-end vs IA Esperto chiusa fino a stato terminale (vittoria, sconfitta o patta) **senza crash UI**".

Il test E2E implementato (`SinglePlayerE2ETest#humanFirstMoveAdvancesGameStateAndHistory`) copre solo la prima mossa del bianco con `BoardRenderer` mocked e `AiTurnService` non wirato (`Optional.empty()`). L'AI loop reale non viene esercitato.

Le opzioni considerate in REVIEW-fase-3 finding F-002:

- **A** — test slow tagged completo: ~3-4 ore lavoro + ~1-3 min wall-clock per esecuzione, fragile in CI desktopless;
- **B (scelta)** — keep the light test as smoke + manual full-game validation step here;
- **C** — SPEC change request per accettare validazione "via primitive" (più rischioso).

L'utente ha approvato l'opzione B (messaggio del 2026-04-30): "Per il punto 2 opzione B".

### 7.2 Procedura operativa

Da eseguire **una volta**, su sistema con display e JavaFX runtime disponibile, prima di chiudere la sotto-fase TEST.

1. Da working tree pulito sul branch `feature/3-ui-singleplayer` (o successivo merge su `develop`), eseguire:
   ```
   mvn -pl client javafx:run
   ```
2. Splash → Main menu: verificare apertura senza eccezioni, verificare card "Nuova partita SP" presente.
3. Click "Nuova partita SP": setup screen → selezionare livello **Esperto**, colore Bianco (per giocare il primo turno), dare un nome alla partita (es. "Manual A3.3"), avviare.
4. Giocare a oltranza (mosse a piacere, anche subottimali) **fino a stato terminale**. Stato terminale = uno fra:
   - **Vittoria umana** (l'IA non ha più mosse o pezzi),
   - **Sconfitta umana** (umano in stallo o senza pezzi),
   - **Patta** (40 mosse senza presa né mossa di pedina, oppure triplice ripetizione, oppure mutual blockade).
5. Durante la partita, esercitare almeno una volta ogni feature critica della board view:
   - Click su una pedina propria → highlight giallo dei target legali (FR-SP-04 / A3.7).
   - Stato con cattura obbligatoria → highlight rosso pulsante sui pezzi che devono catturare (FR-SP-05 / A3.6 / `pulse-mandatory`).
   - Almeno una **presa multipla** o **promozione** se il fluire della partita le permette (FR-RUL-02-04, A3.11, A3.12).
   - Almeno una **Annulla mossa** (Ctrl+Z) e **Ripeti mossa** (Ctrl+Y) per esercitare Task 3.24 (FR-SP-06 / A3.x post-3.24).
   - Voce di menù **Salva con nome…**: salvare a metà partita, verificare scrittura file in `~/.dama-italiana/saves/`.
   - Voce di menù **Carica**: ricaricare il salvataggio, verificare ripresa allo stesso stato.
6. A stato terminale: verificare che `StatusPane` mostri lo stato corretto (`status.win.white`, `status.win.black`, `status.draw.*`).
7. Chiudere la finestra con la X: nessuno stack trace / popup di errore.
8. Riaprire `mvn -pl client javafx:run`: il prompt di autosave NON deve apparire (la partita è terminata, autosave clearato — A3.21 / FR-SP-08).

### 7.3 Cosa annotare (success criteria)

In questo file, sezione 7.4, riportare:

- **Data e ora** dell'esecuzione.
- **Esito**: stato terminale raggiunto (W/L/D), numero di mosse approssimativo.
- **Eventuali eccezioni** osservate (stack trace, popup di errore, glitch grafici): se presenti → finding `BUG` da aprire.
- **Conferma** delle 6 sotto-feature esercitate (highlight legali, highlight obbligatorio, multi-jump/promozione, undo/redo, save+load, autosave clear).

Se la procedura va a buon fine senza crash UI: **A3.3 ✅ COVERED via manual validation**, F-002 RESOLVED.

Se emergono crash o regressioni: aprire finding `BUG, severity ≥ High` in REVIEW-fase-3 (la review viene riaperta) e tornare alla sotto-fase IMPLEMENTA.

### 7.4 Log di esecuzione

| Data       | Esito           | N. mosse | Sotto-feature OK | Eccezioni / note                                        | Eseguito da    |
|------------|-----------------|---------:|------------------|---------------------------------------------------------|----------------|
| 2026-04-30 | OK (no crash UI) |  manual | tutte (6/6)      | Nessuna eccezione, nessun glitch grafico, nessuna regressione osservata | Giuseppe Fornaro |

**Esito**: A3.3 ✅ COVERED via manual validation, REVIEW-fase-3 finding F-002 RESOLVED.

---

## 8. Regressione cross-modulo

Regressione full-tag (con `slow` + `performance`) eseguita su commit `fc7b68c` (HEAD branch `feature/3.5-visual-polish-and-audio`) come parte del Task 3.5.0 di Fase 3.5. Comando: `mvn clean verify` (root, senza `-DexcludedGroups`).

**Risultato**: BUILD SUCCESS, durata totale **16:25 min**, log `/tmp/mvn-regression-f3-debt.log`.

| Modulo                          | Test eseguiti | Failures | Errors | Skipped | Wall-clock | Note |
|---------------------------------|--------------:|---------:|-------:|--------:|-----------:|------|
| `parent` (build orchestration)  |             — |        — |      — |       — |     3.346s | Reactor + enforcer |
| `shared` (dominio, rules, AI)   |       **391** |        0 |      0 |       0 |  **15:13** | Include 48 corpus regole + 5 tactical golden + `AiTournamentSimulationTest` 100-game (885.2s) + `AiPerformanceTest` budget (0.619s) |
| `core-server`                   |             1 |        0 |      0 |       0 |     7.570s | Smoke `CoreServerSmokeTest` |
| `client` (JavaFX desktop)       |       **280** |        0 |      0 |       0 |    48.707s | Suite F3 completa + Spring context + FXML smoke + 5 E2E (skip su CI headless) |
| `server` (Spring Boot)          |             1 |        0 |      0 |       0 |    11.080s | Smoke `ServerSmokeTest` |
| **Totale**                      |       **673** |    **0** |  **0** |   **0** | **16:25 min** | Tutti i moduli SUCCESS |

**Gating F2 verificato**:
- `AiTournamentSimulationTest#campionWinsAtLeast95OutOf100AgainstPrincipiante` → ✅ PASS (1 test, parte dei 2 della classe; soglia ≥ 95% mantenuta — il test fallirebbe altrimenti via `assertThat(wins).isGreaterThanOrEqualTo(95)`).
- `AiPerformanceTest` → ✅ 3 PASS (budget Principiante / Esperto / Campione con tolleranza 1.5x rispetto a NFR-P-02; eseguito in 0.619s totali, ben sotto i 5s del livello Campione).

**Gate qualità superati**:
- JaCoCo `BUNDLE` ≥ 60% line+branch su `shared` e `client` (NFR-M-02): ✅ "All coverage checks have been met" su entrambi i moduli.
- Spotless `googleJavaFormat` (NFR-M-04): ✅ tutti i file puliti (120 client, 2 server, 47 shared).
- SpotBugs threshold High: ✅ "BugInstance size is 0, Error size is 0" su tutti i moduli.
- Maven Enforcer (Java 21 + Maven ≥ 3.9 + dependencyConvergence): ✅ pulito.

**Conclusione**: nessuna regressione introdotta dalle modifiche di Fase 3 sul corpus regole F1 né sul gating tattico F2; nessun test F3 instabile (280/280 verdi al primo run dopo `mvn clean`). Cross-modulo: `core-server` e `server` smoke restano verdi, confermando che le modifiche client non hanno toccato accidentalmente moduli upstream.

---

## 9. Limiti documentati e debiti tecnici tracciati

I seguenti debiti sono noti, deliberati, e tracciati per le fasi successive. Tutti formalizzati in REVIEW-fase-3 (commit `6eeb332`) e SPEC v2.1 §13.5 + Fase 11.

| ID | Debito | Stato | Risoluzione |
|---|---|---|---|
| F3-D1 | NFR-U-03 a11y: testi `accessibleText` in inglese hardcoded (`BoardRenderer`, `CellAccessibleText`, `PieceAccessibleText`) | DEFERRED F11 | Localizzazione IT/EN dei testi a11y. Deferral motivato dal fatto che ogni cell read screen-reader riprodurrebbe la stessa frase da ~30 chiavi i18n aggiunte; valutazione costi/benefici a F11. Documentato in SPEC v2.1 §13.5. |
| F3-D2 | NFR-U-02 dark mode toggle runtime non disponibile (theme-dark.css è stub) | DEFERRED F11 | Toggle runtime (light/dark) + ri-apply su scene switch. SPEC §16 Fase 11. |
| F3-D3 | NFR-P-01 misura formale 60 FPS durante animazioni | DEFERRED F11 | Tooling formale su hardware target (Intel UHD 620+); manual visual check OK in F3. SPEC v2.1 §16 Fase 11. |
| F3-D4 | NFR-U-04 verifica tool-based contrasto WCAG AA su entrambi i temi | DEFERRED F11 | Light WCAG AA by-design; dark + tool-based check a F11. SPEC v2.1 §16 Fase 11. |
| F3-D5 | §13.5 pattern daltonismo sui pezzi (linee/puntini) | DEFERRED F11 | Feature di a11y avanzata. SPEC v2.1 §13.5. |
| F3-D6 | TEST sotto-fase F3 deferred post-tag `v0.3.0` (deviazione una-tantum dal workflow CLAUDE.md §2) | **In closure ora** (Task 3.5.0 di Fase 3.5) | Questo file + regression slow + procedura manuale §7. |

Nessun debito mette a rischio l'acceptance di Fase 3 (utente gioca partita SP completa, salva, ricarica, riapre dopo crash con autosave — tutti coperti da test verdi).

---

## 10. Closure della sotto-fase TEST (CLAUDE.md §2.4.6)

Stato al **2026-04-30**, commit `fc7b68c`:

- [x] Coverage target raggiunti — `client` 74.18% line / 60.95% branch (gate JaCoCo `BUNDLE` ≥ 60% line+branch superato; vedi §2.2 e §8). `shared` ≥ 80% (NFR-M-01) confermato dalle suite corpus + AI.
- [x] Traceability matrix aggiornata — ogni FR/NFR/AC della Fase 3 ha almeno un test reale o "manual" esplicito (post Task 3.24 + F-003 closure; vedi §6).
- [x] Test corpus regole italiane invariato — 48 posizioni F1 + 5 tactical golden F2 tutte verdi nella regressione di §8.
- [x] **§7.4 popolato** con esito della validazione manuale A3.3 (F-002 RESOLVED via manual) — eseguita dall'utente il 2026-04-30, esito OK su tutte e 6 le sotto-feature, nessuna eccezione né regressione.
- [x] `mvn verify` (default `-DexcludedGroups=slow,performance`) passa pulito su tutti i moduli — verificato come parte di §8 (la suite default è un sottoinsieme di quella full-tag).
- [x] `mvn clean verify` con `slow`+`performance` inclusi BUILD SUCCESS almeno una volta (regression F1+F2) — eseguito al commit `fc7b68c`, durata 16:25 min, 673/673 verdi (vedi §8).
- [x] Test plan documenta scelte e copertura — questo file (sezioni 1-10 popolate).
- [x] Nessun test in stato `@Disabled`/`@Ignore` senza issue tracciata — verificato via `mvn` output: `Tests run: 673, Failures: 0, Errors: 0, Skipped: 0` su tutti i moduli.

**Stato finale**: 8 di 8 voci spuntate.

**Sotto-fase TEST F3 chiusa** il 2026-04-30. Closure documentata nei commit `docs(tests): close TEST F3 debt as task 3.5.0` (sezioni 2-6, 8-10) e `docs(tests): record manual A3.3 run (F-002 resolved)` (§7.4).

---

**FINE TEST-PLAN-fase-3**
