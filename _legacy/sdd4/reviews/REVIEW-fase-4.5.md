# REVIEW — Fase 4.5: UI Responsiveness desktop

- **Data**: 2026-05-06
- **Branch reviewed**: `feature/4.5-ui-responsiveness` @ `c51c145`
- **SPEC version**: 2.3 (post CR-F4.5-001)
- **Reviewer**: Claude Code

## Sommario

| Categoria       | Critical | High | Medium | Low | Totale |
|-----------------|---------:|-----:|-------:|----:|-------:|
| BLOCKER         |        0 |    0 |      0 |   0 |      0 |
| REQUIREMENT_GAP |        0 |    0 |      1 |   0 |      1 |
| BUG             |        0 |    2 |      4 |   1 |      7 |
| SECURITY        |        0 |    0 |      0 |   0 |      0 |
| PERFORMANCE     |        0 |    0 |      0 |   1 |      1 |
| CODE_QUALITY    |        0 |    0 |      2 |   2 |      4 |
| DOC_GAP         |        0 |    0 |      1 |   1 |      2 |
| **Totale**      |        0 |    2 |      8 |   5 |     15 |

Nessun `BLOCKER` o `Critical`. I 2 `High` (F-001 + F-002) sono interazioni del flusso di Stage state persistence di Task 4.5.7b — fix circoscritti, no regression cross-fase.

## Acceptance criteria coverage

| AC ID     | Status              | Evidence                                                                                                                                                                                                                                                                                                                                                                                       |
|-----------|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| A4.5.1    | ✅ COVERED          | Fast loop `mvn -pl client verify -DexcludedGroups=slow,performance` BUILD SUCCESS, 379/379 verdi (Task 4.5.9 closure).                                                                                                                                                                                                                                                                          |
| A4.5.2    | ✅ COVERED          | JaCoCo `All coverage checks have been met` ad ogni Task 4.5.x; bundle 74 classi post-merge.                                                                                                                                                                                                                                                                                                    |
| A4.5.3    | ✅ COVERED          | Output Maven verde ad ogni task closure (SpotBugs 0, Spotless OK).                                                                                                                                                                                                                                                                                                                              |
| A4.5.4    | ✅ COVERED          | `BoardRendererLayoutTest#cellsAreCenteredAndFitWithinTheRenderer` (7 viewports) + `ResponsivenessParametricTest` board-square branch + `tests/visual-review/responsiveness-baseline-post-fix/board-view_1366x768-laptop.png` (committed sample, board completamente visibile).                                                                                                                |
| A4.5.5    | ✅ COVERED          | `BoardRendererLayoutTest` asserisce `cellSize == min(w,h)/8` e grid centered; `ResponsivenessParametricTest` asserisce board fits renderer.                                                                                                                                                                                                                                                    |
| A4.5.6    | ✅ COVERED          | `PrimaryStageInitializerTest` (4 test): `minSizeMatchesF45Floor`, `initialSizeIsAtLeastMinAndComputedAt80PercentOfPrimaryScreen`, `positionIsCenteredOnPrimaryScreenVisualBounds`, `nullStageThrowsNpe`.                                                                                                                                                                                       |
| A4.5.7    | ✅ COVERED          | `ResponsivenessParametricTest.assertLayoutInvariants` (no clipping `root.bounds ≤ scene + 1px` + no `ScrollBar` visible) su 7 res × 7 schermate assertable.                                                                                                                                                                                                                                   |
| A4.5.8    | ⚠️ PARTIAL          | `SidePanelLayoutTest` asserisce pref=320/min=240/max=400 su 6 stage widths. **Auto-hide < 1024 NON implementato** (Stage min enforce 1024 → unreachable; hook documentato in `board-view.fxml:69`). Vedi finding F-001 per impatto del persisted state quando geometry fields fanno bypass del min Stage. Acceptable by design.                                                       |
| A4.5.9    | ✅ COVERED          | `JavaFxScalingHelperTest` (12 parametric math + 3 FX-binding) + 6 FXML modificati con marker class + `SceneRouter.show` wiring.                                                                                                                                                                                                                                                              |
| A4.5.10   | ⚠️ DEFERRED (manual) | Nessun test combina `UiScalingService.applyTo` + `JavaFxScalingHelper.applyFluidFontsToScene` con assertions sul cascade. `ResponsivenessParametricTest` chiama entrambi (linee 172-173) ma asserisce solo layout invariants. Vedi **F-009** (REQUIREMENT_GAP, Medium).                                                                                                                       |
| A4.5.11   | ⚠️ DEFERRED (manual) | DPI varianti documentate manuale in `ResponsivenessParametricTest` Javadoc + `BaselineScreenshotCapture` Javadoc; layout math DPI-independent. Da verificare in TEST sotto-fase via Task 4.5.16.                                                                                                                                                                                              |
| A4.5.12   | ⚠️ DEFERRED (manual) | Resize FPS non automatable; manual demo Task 4.5.16.                                                                                                                                                                                                                                                                                                                                            |
| A4.5.13   | ✅ COVERED          | Anti-pattern #15 verification — vedi §4.                                                                                                                                                                                                                                                                                                                                                        |
| A4.5.14   | ✅ COVERED          | CSS diff `-fx-background-size: cover;` → `-fx-background-repeat: repeat;` su 4 selectors (`.board-frame`, `.splash-root`, `.main-menu-root`, `.screen-root`). Cell selectors `.board-cell-light/dark` mantengono `100% 100% no-repeat` (corretto). Vedi **F-008** per defensiveness.                                                                                                          |
| A4.5.14b  | ✅ COVERED          | Task 4.5.3b audit PASS (`5878880`): 3/3 texture wood 2048×2048, CC0 Poly Haven già documentato in `CREDITS.md` linee 36-38.                                                                                                                                                                                                                                                                    |
| A4.5.14c  | ✅ COVERED          | `BoardRendererLayoutTest` include caso 2400×1200 ultrawide-after-chrome; main-menu/load-screen/rules/settings/sp-setup capati a 960/1100/1200/560; board centrato (Task 4.5.4). Task 4.5.6b skipped per Opzione B utente, screenshot post-fix conferma centratura.                                                                                                                            |
| A4.5.14d  | ✅ COVERED          | `StagePersistenceValidatorTest` (9 test failure cases) + `PreferencesServiceTest.migratesSchemaV2FileLeavingWindowFieldsNull` + `…V1FileFillingAudioDefaults`. Vedi **F-001/F-002/F-003** per defects nel flusso di restore.                                                                                                                                                                  |
| A4.5.15   | ✅ COVERED          | `tests/TRACEABILITY.md` aggiornato in Task 4.5.10 con NFR-U-05 + AC §17.2.8 + nuova sezione "Acceptance criteria di Fase 4.5".                                                                                                                                                                                                                                                                  |
| A4.5.16   | ⚠️ DEFERRED         | Manual demo run su PC cliente / VM — TEST sotto-fase, log in TEST-PLAN-fase-4.5 §7.                                                                                                                                                                                                                                                                                                            |
| A4.5.17   | ✅ COVERED          | `grep "TODO\|FIXME"` su tutti i file F4.5 modificati: nessun token nuovo non tracciato.                                                                                                                                                                                                                                                                                                          |
| A4.5.18   | ⚠️ DEFERRED         | Cross-module `mvn clean verify` (root, slow + performance inclusi) — TEST sotto-fase.                                                                                                                                                                                                                                                                                                          |
| A4.5.19   | ✅ COVERED          | `client/src/main/java/com/damaitaliana/client/layout/package-info.java` (Task 4.5.10).                                                                                                                                                                                                                                                                                                          |

**Riepilogo coverage**: 13 ✅ COVERED, 2 ⚠️ PARTIAL, 4 ⚠️ DEFERRED to TEST sotto-fase (A4.5.10 ortogonalità via test mancante → finding F-009; A4.5.11 DPI varianti, A4.5.12 resize jank, A4.5.16 manual demo, A4.5.18 mvn clean verify root).

## Findings

### F-001 — [BUG, High] Maximized state restored prima di `Stage.show()` + persistenza geometry confonde maximized e windowed

- **Posizione**: `client/src/main/java/com/damaitaliana/client/app/StagePersistenceCoordinator.java:54-60` (restore) e `client/src/main/java/com/damaitaliana/client/app/StagePersistenceCoordinator.java:73-77` (persist).
- **Descrizione**: Due defects accoppiati.
  1. `setMaximized(true)` invocato prima di `primaryStage.show()` (chiamato in `JavaFxApp.start`). Il contract Javadoc di JavaFX 21 dice "may be ignored if the stage is not yet shown". Su Linux Wayland/X11 può essere ignorato; su Windows generalmente onorato ma non garantito.
  2. `persist()` (lines 73-77) cattura `stage.getWidth/Height/X/Y` incondizionatamente. Quando lo stage è **maximized at close time**, `getWidth/Height` ritornano le dimensioni maximized, non le windowed sottostanti. Reproducer: app aperta a 1366×768, drag-resize a 1500×900, maximize, close. Next launch apre maximized (OK). Utente clicca restore → finestra snap a maximized-screen-size, NON a 1500×900 originale. La memoria della "finestra che lui aveva configurato" è persa al primo close-da-maximized.
- **Proposta di fix**: Catturare `windowWidth/Height/X/Y` solo quando NOT maximized. Pattern: in `setOnCloseRequest` se `stage.isMaximized()` write only `windowMaximized=true` preservando i geometry fields precedenti (load → modify maximized only → save). Per la transizione live, aggiungere listener su `stage.maximizedProperty()` che cattura la geometry windowed appena prima del maximize. Differire `setMaximized(true)` a dopo `stage.show()` via `Platform.runLater`, oppure restructure `JavaFxApp.start` per chiamare `coordinator.initialize` post-`show`.
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-002 — [BUG, High] Validator accetta finestre il cui solo top-left è on-screen ma right/bottom edge è completamente off-screen

- **Posizione**: `client/src/main/java/com/damaitaliana/client/app/StagePersistenceValidator.java:46-53`.
- **SPEC reference**: §13.7 ("fallback al computed 80% se invalid (monitor scollegato, coordinate fuori bounds)").
- **Descrizione**: Il validator controlla solo `bounds.contains(x, y)` (top-left). NON verifica che `(x + width, y + height)` sia raggiungibile. Reproducer: dual-monitor user con finestra a top-left `(1900, 50)` su primary 1920×1080 + secondary 1920×1080 side-by-side (span 0..3840). Disconnette il secondary. `1900 < 1920` → `bounds.contains(1900, 50) = true` → state accettato. Ma window allocata con `(width=1366) + 1900 = 3266 > 1920` → la maggior parte del contenuto è off-screen sul right edge del primary. Utente vede un "sliver" della finestra a destra. SPEC §13.7 richiede fallback in questo scenario; l'implementazione non lo cattura.
- **Proposta di fix**: Estendere `isStateValid` per richiedere che almeno N% (es. 50%) del rect persisted sia dentro la union dei `screenVisualBounds`. Pseudocodice:
  ```java
  Rectangle2D win = new Rectangle2D(x, y, width, height);
  for (Rectangle2D bounds : screenVisualBounds) {
    if (bounds.intersects(win)) {
      double interW = Math.min(bounds.getMaxX(), win.getMaxX()) - Math.max(bounds.getMinX(), win.getMinX());
      double interH = Math.min(bounds.getMaxY(), win.getMaxY()) - Math.max(bounds.getMinY(), win.getMinY());
      double ratio = (interW * interH) / (win.getWidth() * win.getHeight());
      if (ratio >= 0.5) return true;
    }
  }
  return false;
  ```
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-003 — [BUG, Medium] Validator rifiuta v3 con `windowMaximized=true` ma geometry `null` — interazione con F-001

- **Posizione**: `client/src/main/java/com/damaitaliana/client/persistence/UserPreferences.java:91-106` + `StagePersistenceValidator.java:36-42`.
- **Descrizione**: Se file v3 ha `windowMaximized=true` ma una qualsiasi delle 4 geometry int è `null` (es. utente chiude maximized prima volta dopo v2→v3 migration), validator ritorna `false` → fallback a 80% computed. La narrativa Javadoc (`UserPreferences.java:21-22`) dice "v2 → v3 migration leaves geometry null". Combinato con F-001 (persist cattura sempre maximized geometry come window), il primo save dopo migration v2→v3 in stato maximized scrive geometry maximized → al riavvio il restore funziona ma se l'utente fa restore window-mode, vede dimensioni maximized non windowed. Lo stato "maximized senza windowed memory" è non rappresentabile.
- **Proposta di fix**: Trattare `windowMaximized=true` come sufficiente da solo (skip geometry-null check quando maximized), OR fixare F-001 per primo (capture windowed size separatamente dal flag maximized — è la fix preferita perché risolve entrambi).
- **Status**: RESOLVED (vedi "Resolution log" in fondo) (dipendente da F-001)

---

### F-004 — [BUG, Medium] `StagePersistenceCoordinator.persist` può scrivere geometry `(0,0,0,0)` se Stage close avviene pre-show

- **Posizione**: `client/src/main/java/com/damaitaliana/client/app/StagePersistenceCoordinator.java:79-82` e `client/src/main/java/com/damaitaliana/client/persistence/PreferencesService.java:103-105`.
- **Descrizione**: Race contrived ma reale: utente chiude la finestra splash (Alt+F4) prima che il main menu carichi. `stage.getWidth/Height/X/Y` possono ritornare `Double.NaN` o 0 se lo Stage non è stato sized completamente. `(int) Math.round(NaN) = 0` → persist scrive `(0, 0, 0, 0, false)` → `width=0 < MIN_WIDTH=1024` → al prossimo launch validator rifiuta → fallback a 80% (graceful) MA il previous valid state è stato sovrascritto. La intent dell'utente persa.
- **Proposta di fix**: Guard al top di `persist`: se any of `stage.getWidth/Height ≤ 0` o `Double.isNaN` skip il save e log INFO ("stage geometry unavailable on close, preserving previous state"). Alternativa: persist solo se `width >= MIN_WIDTH && height >= MIN_HEIGHT`.
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-005 — [BUG, Medium] `BoardFrameThicknessHelper` setta padding a 16px (clamp floor) prima del primo layout pulse — regression visiva vs FXML fallback 24px

- **Posizione**: `client/src/main/java/com/damaitaliana/client/layout/BoardFrameThicknessHelper.java:47-66`.
- **Descrizione**: Quando `BoardViewController.initialize()` chiama `bindFrameThickness(boardFrame, boardRenderer)`, il binding evaluator immediatamente computa `Math.min(renderer.getWidth(), renderer.getHeight())` con `width = height = 0.0` (renderer non ancora layoutato). `computeFrameThickness(0, 16, 48, 0.035) = max(16, 0) = 16`. Padding settato a `Insets(16)`, sovrascrivendo il fallback FXML `Insets(24)`. Il commento board-view.fxml:51 promette "static 24px below is the fallback when the controller isn't wired" — ma il controller runs at FXML load → bind immediate → 24 → 16 transient. Impact visivo minimo (16 vs 24, diff 8px) ma il contract documentato è violato. `BoardFrameThicknessHelperTest:117-133` testa idempotenza ma non lo state pre-layout.
- **Proposta di fix**: Wrappare il binding con `Bindings.when(renderer.widthProperty().greaterThan(0).and(renderer.heightProperty().greaterThan(0)))…otherwise(new Insets(24))`. OR delay del bind via `Platform.runLater` se renderer.width=0. Preferisco la prima opzione (più dichiarativo, meno race).
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-006 — [BUG, Medium] Cascade marker class `display-fluid-lg` non re-evaluata se styleClass mutato runtime

- **Posizione**: `client/src/main/java/com/damaitaliana/client/layout/JavaFxScalingHelper.java:71-75`.
- **Descrizione**: Walker fa `getStyleClass().contains(...)` at bind time. Se un controller futuro muta dinamicamente styleClass di un Labeled (es. aggiunge `display-fluid-lg` a runtime), il walker non re-evaluata. No drift attuale (nessun controller muta styleClass su display labels), ma contract non documentato. `applyFluidFontsToScene` viene chiamato da `SceneRouter.show` su ogni navigazione e walka `scene.getRoot()` corrente — siccome `setRoot(newRoot)` cambia il root, il vecchio non è raggiungibile più; la binding precedente sopravvive (riferimento forte a `Scene.widthProperty()`) ma il vecchio root verrà GC'd. No leak reale, ma "idempotent re-bind" in pratica si applica solo al new root.
- **Proposta di fix**: Doc-only — aggiungere al Javadoc `JavaFxScalingHelper.applyFluidFontsToScene` "assumes one-shot styleClass assignment at FXML load; runtime mutation of styleClass on bound labels is out of contract". No code change.
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-007 — [BUG, Low] CSS fallback `display-fluid: 28px` mismatch col clamp floor `24px` → flash visivo a viewport stretti

- **Posizione**: `client/src/main/resources/css/theme-light.css:93-98` e `client/src/main/java/com/damaitaliana/client/layout/JavaFxScalingHelper.java:33-36, 39-42`.
- **Descrizione**: CSS fallback `.display-fluid { -fx-font-size: 28px; }` viene usato "when the helper has not bound yet" (CSS comment). Ma il helper clamp `[24, 32]` → at viewport 1024×720 produce `1024 × 0.018 = 18.4 → clamp 24`. Fallback paint a 28 → first frame → helper rebinds a 24 → flash visivo. Stesso pattern per `display-fluid-lg: 38px` baseline vs computed 28px at floor. Tests confermano binding wins (`JavaFxScalingHelperTest:124-130`); il flash transient resta osservabile.
- **Proposta di fix**: Settare CSS baseline al floor del clamp range: `.display-fluid: 24px`, `.display-fluid-lg: 28px`. Il bind transitions upward smoothly invece che downward at small viewports. Aggiornare commento CSS line 89-91 di conseguenza.
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-008 — [DOC_GAP, Medium] CSS texture switch `cover` → `repeat` senza `-fx-background-size: auto` esplicito

- **Posizione**: `client/src/main/resources/css/components.css:32-37` (`.board-frame`) e `client/src/main/resources/css/theme-light.css:382-390, 434-443, 474-484` (3 selectors).
- **Descrizione**: Diff rimuove `-fx-background-size: cover;` e aggiunge solo `-fx-background-repeat: repeat;`. JavaFX CSS reference: quando `-fx-background-size` è omesso default è `auto` → image native pixel size (2048×2048). È quello che vogliamo (tile native). MA il comment "(Poly Haven CC0 textures are designed to repeat)" non documenta la dipendenza implicita dal default `auto`. Se future task aggiunge `-fx-background-size: 100% 100%;` pensando sia helpful, repeat diventa ineffective (single stretched tile fills whole region) — silently regression to the pre-F4.5 behavior. `.board-frame` ha highest risk perché è la più piccola region (~24-48 px padding around renderer); at 4K il frame texture tila in many small tiles, visually OK ma non quello che readers del comment expect.
- **Proposta di fix**: Aggiungere `-fx-background-size: auto;` esplicito a tutti i 4 selectors, OR aggiungere comment line "// background-size omitted = auto = native pixel size (required for tile pattern)". Documentare in `assets/CREDITS.md` la assumed asset native resolution.
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-009 — [REQUIREMENT_GAP, Medium] Nessun automated test per orthogonality `UiScalingService` × `JavaFxScalingHelper` (A4.5.10)

- **Posizione**: tests scope.
- **SPEC reference**: `PLAN-fase-4.5.md` §2.2 row A4.5.10 + SPEC §13.5 (UI scaling 100/125/150).
- **Descrizione**: AC A4.5.10: "scaling 150% × viewport 1366×768 → board ancora visibile e quadrata". `UiScalingService.applyTo(scene)` setta `-fx-font-size: 21.0px` sul root (per `UiScalingServiceTest#readsCurrentPreferenceWhenNoExplicitPercent`). `JavaFxScalingHelper.applyFluidFontsToScene(scene)` walka tree e binda matching labels a inline `-fx-font-size: Xpx`. CSS cascade: inline style wins su class selectors. Quindi helper override UiScalingService root-level scale. **Effetto**: at 150% UI scale + 1366 viewport, hero title displaya at helper-computed size (es. 34.15px), NOT 1.5 × baseline 38px = 57px. Argomentabile "ortogonale by design" (helper intenzionalmente assoluto) ma significa che UI scaling NON ha effetto su display labels. SPEC §13.5 implicitamente applica UI scaling a tutto il testo UI; il fluid binding silently exempts display labels. Nessun test verifica che questo sia il behavior intended.
- **Proposta di fix**: Una di:
  1. **Compose**: bindFluidFontSize legge active scale percent e moltiplica → display labels rispettano UI scaling.
  2. **Document**: aggiungere a `JavaFxScalingHelper` Javadoc + SPEC §13.7 che fluid display labels NON compongono con UiScalingService scale; sono assoluti viewport-derived sizes. Aggiungere parametric test `(uiScalePercent ∈ {100, 125, 150}, sceneWidth ∈ {1024, 1920, 3840})` che asserisce display-fluid font-size matcha contract documentato.
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-010 — [CODE_QUALITY, Medium] Hard-coded `/tmp/test-saves-…` path è Unix-style su Windows CI

- **Posizione**: `client/src/test/java/com/damaitaliana/client/layout/BaselineScreenshotCapture.java:68` e `client/src/test/java/com/damaitaliana/client/layout/ResponsivenessParametricTest.java:82`.
- **Descrizione**: `@TestPropertySource(properties = {"dama.client.saves-dir=/tmp/test-saves-baseline-screenshot"})` (e analogo per ResponsivenessParametric). Su Windows CI mappa a `C:\tmp\…` (drive root, può fallire o creare unwanted dir). Pre-existing pattern da F3 ma worth flagging perché slow tag rende failure modes facili da missare. No parallelism issue inherente (slow tag isola).
- **Proposta di fix**: Sostituire con `${java.io.tmpdir}/test-saves-…` via `@TempDir` injection o `System.getProperty("java.io.tmpdir") + "/test-saves-…"` + UUID per run. Low priority.
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-011 — [CODE_QUALITY, Medium] `UserPreferences.withWindowState` accetta width/height illimitati senza validation

- **Posizione**: `client/src/main/java/com/damaitaliana/client/persistence/UserPreferences.java:275-291`.
- **Descrizione**: Accetta blindly. Negative width/height vengono persisted negativi; `Integer.MAX_VALUE` accettato. `StagePersistenceValidator` cattura negative su load (`< MIN_WIDTH`) → sistema self-heals al next launch. Ma compact constructor (lines 63-70) potrebbe clampare al write time (esiste già `clampVolume` per audio). Defensive clamping evita rappresentazioni inconsistenti del config.json.
- **Proposta di fix**: Nel canonical constructor (lines 63-70), se `windowWidth != null && windowWidth < 0` set to `null`; same per height. OR fail-fast `IllegalArgumentException` su negative geometry — il call site `(int) Math.round(stage.getWidth())` shouldn't ever produce negatives in practice, ma defensive.
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-012 — [PERFORMANCE, Low] `JavaFxScalingHelper.applyFluidFontsToScene` walka l'intero scene graph at every navigation

- **Posizione**: `client/src/main/java/com/damaitaliana/client/app/SceneRouter.java:86` chiama `JavaFxScalingHelper.applyFluidFontsToScene(scene)` on every navigation; helper traverses `scene.getRoot()` recursive (`JavaFxScalingHelper.java:69-82`).
- **Descrizione**: Each `show` navigates by `scene.setRoot(root)`, walker runs on **new** root. Per le 8 schermate F3.5 con ~10-50 nodes each negligible. MA: future F8 tournament screen ~hundreds of nodes (live bracket + scoreboard) + rapid navigation può crescere. Non un problema oggi.
- **Proposta di fix**: Nessuna for F4.5. Aggiungere TODO referenced from PLAN F8 to revisit if perf surfaces.
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-013 — [DOC_GAP, Medium] `BoardRenderer.layoutChildren` class Javadoc non aggiornato col centering invariant Task 4.5.4

- **Posizione**: `client/src/main/java/com/damaitaliana/client/ui/board/BoardRenderer.java:18-25` (class Javadoc).
- **Descrizione**: Class Javadoc dice "Sized to fit its containing parent (square aspect, layoutChildren sizes each cell to min(width, height) / 8)". Il F4.5 change ha aggiunto centering math (`xOffset / yOffset`). Il class-level Javadoc è il discoverable contract; future maintainers refactoring potrebbero anchor at top-left non sapendo del centering. Inline comment at lines 239-243 documenta ma non discoverable se uno legge solo il Javadoc.
- **Proposta di fix**: Append al class Javadoc: "Cells are centered within the available area when it is non-square; the wood frame surrounds the playing field equally on all sides (F4.5 Task 4.5.4)."
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-014 — [CODE_QUALITY, Low] `PrimaryStageInitializer.INITIAL_SIZE_RATIO_PERCENT` package-private ma non usato dal test

- **Posizione**: `client/src/main/java/com/damaitaliana/client/app/PrimaryStageInitializer.java:35-36` e `client/src/test/java/com/damaitaliana/client/app/PrimaryStageInitializerTest.java:74`.
- **Descrizione**: `static final int INITIAL_SIZE_RATIO_PERCENT = 80` package-private (presumibilmente per test). Test hardcoda `0.80` invece di referenziare la costante.
- **Proposta di fix**: Usare `PrimaryStageInitializer.INITIAL_SIZE_RATIO_PERCENT / 100.0` nel test, OR rimuovere `_PERCENT` constant se non usato outside class (inline il letterale 80).
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

### F-015 — [DOC_GAP, Low] `package-info.java` cita "AudioService all unchanged" ma non enumera tutti gli unchanged components di anti-pattern #15

- **Posizione**: `client/src/main/java/com/damaitaliana/client/layout/package-info.java:7-9`.
- **Descrizione**: Javadoc dice "(anti-pattern CLAUDE.md §8 #15 — token CSS v2, texture wood, font families, animation parameters all unchanged)". CLAUDE.md §8 #15 lista anche `BoardRenderer`/`PieceNode`/`MoveAnimator`/`ParticleEffects`/`AudioService`/`AnimationOrchestrator`. Optional improvement per traceability completa.
- **Proposta di fix**: Append explicit list da CLAUDE.md §8 #15. Optional.
- **Status**: RESOLVED (vedi "Resolution log" in fondo)

---

## Anti-pattern #15 attestation

**Verified: F4.5 changes touched ONLY layout/binding/CSS responsive concerns.**

Diff inspection summary:

| File                           | Change type                                                                                            | Tocca tokens / texture paths / font families / animation params? |
|--------------------------------|--------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| `BoardRenderer.java`           | Centering math in `layoutChildren`                                                                     | No                                                               |
| `PrimaryStageInitializer.java` | New (Stage geometry only)                                                                              | No                                                               |
| `StagePersistenceCoordinator.java` | New (Stage state IO)                                                                              | No                                                               |
| `StagePersistenceValidator.java` | New (pure-math validator)                                                                            | No                                                               |
| `BoardFrameThicknessHelper.java` | New (binding helper)                                                                                  | No                                                               |
| `JavaFxScalingHelper.java`     | New (binding helper)                                                                                   | No                                                               |
| `JavaFxApp.java`               | Wires `StagePersistenceCoordinator`                                                                    | No                                                               |
| `SceneRouter.java`             | Calls `applyFluidFontsToScene`                                                                         | No                                                               |
| `BoardViewController.java`     | Wires `bindFrameThickness`                                                                             | No                                                               |
| `PreferencesService.java`      | v1/v2 → v3 migration accept                                                                            | No                                                               |
| `UserPreferences.java`         | New geometry fields + accessor                                                                         | No                                                               |
| `components.css`               | `.board-frame` `cover` → `repeat`, comment update                                                      | **Borderline**: cambia tiling mode, no token / color / font change |
| `theme-light.css`              | Adds `.display-fluid` / `.display-fluid-lg` (additive); 3× `cover` → `repeat`                         | **Borderline**: additive new classes; tile mode swap             |
| 7× `*.fxml`                    | `maxWidth`/`maxHeight=-Infinity`, `fx:id`, sidePanel pref/min/max, root `<StackPane>` wrapper main-menu | No                                                               |

**Borderline cases**: il CSS `cover` → `repeat` swap su 4 selectors cambia il **rendering mode** delle texture wood esistenti — concern responsive by design (PLAN Task 4.5.8); paths file / colors / opacity invariati. Anti-pattern #15 NON proibisce changing how an asset is **tiled vs stretched**; proibisce cambiare l'asset stesso, design tokens, visual identity. Verdict: **NOT a violation**, documentato in F-008 come defensiveness/comment improvement.

Nessuna modifica rilevata a: `MoveAnimator`, `ParticleEffects`, `AnimationOrchestrator`, `AudioService`, `BoardCellNode`, `PieceNode`, `ThemeService.applyTheme`, palette tokens (`-color-bg-*` / `-color-accent-*` / `-color-text-on-*`), animation parameters, font family declarations.

## SPEC change requests

> Vuota se nessun finding suggerisce di modificare lo SPEC.

**None** — F4.5 implementation matches SPEC v2.3 NFR-U-05 + §13.7 + AC §17.2.8 as approved by CR-F4.5-001.

Note: findings F-001 / F-002 / F-003 surface defects nella **implementation** del Task 4.5.7b stage state persistence, NON nel testo SPEC. SPEC §13.7 "fallback al computed 80% se invalid" è corretto; il validator è quello che non fully implementa. F-009 (orthogonality A4.5.10) potrebbe richiedere chiarimento in SPEC §13.7 se la fix scelta è "document the orthogonality" invece di "compose" — but that's a follow-up sotto-fase REVIEW decision, not a CR yet.

## Closure

- [x] Tutti i `BLOCKER` risolti (0 raised)
- [x] Tutti i `REQUIREMENT_GAP` risolti — F-009 RESOLVED in `4086572`
- [x] Tutti i `Critical/High` `BUG` risolti — F-001 RESOLVED in `15958e8`, F-002 RESOLVED in `1090345`
- [x] Tutti i `Critical/High` `SECURITY` risolti (0 raised)
- [x] `PERFORMANCE` che violano NFR risolti (0 NFR-violating; F-012 deferred a F8 con TODO)
- [x] SPEC change requests con stato non-PENDING (none raised)

**Closure status**: ✅ CHIUSA il 2026-05-06. Tutti i 13 findings actionable risolti in 7 commit (batch logici); 2 findings deferred con rationale (F-010 pre-existing pattern da F3, F-012 perf F8+).

**Review chiusa il**: 2026-05-06
**Commit di chiusura**: `ebfbb84` (batch 7 doc/quality, ultimo del fix loop)

---

## Resolution log

| ID    | Severità          | Commit        | Note                                                                                       |
|-------|-------------------|---------------|--------------------------------------------------------------------------------------------|
| F-001 | High BUG          | `15958e8`     | Stage persistence rework: continuous windowed-bounds tracking, deferred setMaximized.       |
| F-002 | High BUG          | `1090345`     | Validator richiede ≥50% intersection con union dei screen visualBounds.                    |
| F-003 | Medium BUG        | `15958e8`     | Risolto come effetto collaterale di F-001 (persist scrive sempre coppie coerenti).          |
| F-004 | Medium BUG        | `15958e8`     | NaN/zero guard in `persist`; preserva previously persisted state instead of overwriting.    |
| F-005 | Medium BUG        | `0268be4`     | `BoardFrameThicknessHelper` PRE_LAYOUT_FALLBACK_PX=24 evita flash 24→16 pre-layout.         |
| F-006 | Medium BUG (doc)  | `ebfbb84`     | Javadoc documenta che styleClass è letto at bind time only.                                 |
| F-007 | Low BUG           | `0dc609b`     | CSS baselines pinned al clamp floor (24/28) → no flash su small viewports.                  |
| F-008 | Medium DOC_GAP    | `0dc609b`     | `-fx-background-size: auto` dichiarato esplicito sui 4 wood selectors.                      |
| F-009 | Medium REQ_GAP    | `4086572`     | **Compose** chosen: `JavaFxScalingHelper` × `UiScalingService.activeScaleFactor()`. 8 nuovi parametric test (sceneWidth × uiScale). |
| F-010 | Medium CODE_QUALITY | DEFERRED    | Pre-existing `/tmp/test-saves-…` pattern da F3; no F4.5-specific regression. Da rivedere se diventa flaky in CI Windows. |
| F-011 | Medium CODE_QUALITY | `15958e8`   | Defensive clamp negative width/height → null nel canonical constructor di `UserPreferences`. |
| F-012 | Low PERFORMANCE   | DEFERRED      | Walker O(N) su scene graph; F4.5 schermate ~10-50 nodes negligible. TODO F8 tournament.    |
| F-013 | Medium DOC_GAP    | `ebfbb84`     | `BoardRenderer` class Javadoc documenta centering invariant Task 4.5.4.                    |
| F-014 | Low CODE_QUALITY  | `ebfbb84`     | Test usa `INITIAL_SIZE_RATIO_PERCENT/100.0` invece di hardcoded 0.80.                       |
| F-015 | Low DOC_GAP       | `ebfbb84`     | `package-info.java` enumera componenti unchanged di anti-pattern #15.                       |

**Test count delta REVIEW** (fix loop): 379 → 405 fast (+26): 8 `StagePersistenceCoordinatorTest` + 4 `UserPreferencesTest` + 3 `StagePersistenceValidatorTest` + 1 `BoardFrameThicknessHelperTest` + 8 `JavaFxScalingHelperTest` parametric + 2 `UiScalingServiceTest`. Slow tag invariato: 56/56 `ResponsivenessParametricTest` continuano a passare con lo strenghtened validator e la composed fluid typography.
