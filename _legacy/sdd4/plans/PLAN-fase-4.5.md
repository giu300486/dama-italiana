# PLAN — Fase 4.5: UI Responsiveness desktop (mini-fase fix)

- **Riferimento roadmap**: NON pianificata in SPEC §16 originale. Mini-fase intermedia tra Fase 4 (`v0.4.0`, chiusa 2026-05-06) e Fase 5 (server centrale skeleton, prossima fase pianificata). Pull-forward parziale di feature di adattività UI emerse post-demo cliente: con MSI installato su PC del cliente, il client mostra la parte bassa della scacchiera tagliata e la finestra non si adatta al resize. **Stesso modello F3.5** (mini-fase intermedia, pull-forward, tag `v0.<fase>.5`).
- **SPEC version**: 2.2 — questo PLAN propone una **SPEC change request** esplicita (vedi §11) che andrà approvata allo stop point §10 prima di scrivere codice.
- **Data piano**: 2026-05-06.
- **Autore**: Claude Code.
- **Stato**: DRAFT — in attesa di approvazione utente.
- **Branch**: `feature/4.5-ui-responsiveness` (staccato da `develop` HEAD `c3983a0` post-tag `v0.4.0`).
- **Tag finale previsto**: `v0.4.5` (mini-fase intermedia, F5 resta F5 con tag `v0.5.0`).
- **Senior amendments** (post user approval, applicati in PLAN v1.1):
  - Initial Stage size: **80% computed dell'area primary screen, centrato** (sostituisce raccomandazione fissa §10.5 `1366×768`).
  - Aspect ratio handling esplicito per ultrawide (21:9 / 32:9): cluster board+panel centrato, margini wood decorativi laterali.
  - Stage state persistence: nuovo task 4.5.7b — save `windowWidth/Height/X/Y/Maximized` in `~/.dama-italiana/config.json` v3.
  - Asset audit upfront texture wood (prerequisite Task 4.5.4): verifica risoluzione ≥ 2048×2048; se inferiore, source CC0 replacement prima del refactor BoardView.
  - Test matrix DPI focused: 7 combinazioni strategiche invece di 20 (vedi §5.1).

---

## 1. Scopo della fase

Rendere il client **adattivo** su tutto il gamut di risoluzioni desktop standard (1280×720 → 4K) e su window resize/maximize/restore senza clipping né letterboxing né scrollbar inattese. **L'estetica F3.5 wood premium resta invariata**: il fix tocca solo layout/binding/scaling, non token CSS, non texture, non animazioni, non audio.

L'obiettivo concreto è arrivare a uno stato in cui:

> Il cliente lancia il client su qualsiasi monitor desktop con risoluzione ≥ 1280×720 (laptop, FHD, QHD, 4K), ridimensiona la finestra, la massimizza, la rimette in finestra normale, e tutte le 8 schermate F3.5 (splash, main menu, sp setup, board, save dialog, load screen, settings, rules) si adattano fluide: la scacchiera resta sempre interamente visibile e quadrata, il pannello laterale si comprime/espande proporzionalmente, niente parti tagliate sotto, niente scrollbar laterali inaspettate, niente font illeggibili a 4K.

### 1.1 Out of scope

- **Mobile / tablet** (touch, portrait orientation): non sono target — il progetto è desktop-only (SPEC §1.1).
- **Multi-monitor span**: la finestra si comporta come singola Stage; drag tra monitor con DPI diversi rispetta le scelte JavaFX runtime (nessun custom handling).
- **Dark mode runtime toggle** (resta F11).
- **Localizzazione testi `accessibleText`** (resta F11).
- **Pattern daltonismo** (resta F11).
- **Re-design visivo di componenti F3.5**: i token v2, le texture, i font, le animazioni, gli SFX restano **immutati**. Niente ridisegno; solo binding/layout/scaling.
- **Server / multiplayer / tornei** (F5+).

---

## 2. Acceptance criteria

### 2.1 Acceptance globale F4.5

> Il cliente apre il client su monitor con risoluzione ≥ 1280×720 (laptop legacy 1366×768, FHD 1920×1080, QHD 2560×1440, 4K 3840×2160), gioca una partita single-player vs IA, ridimensiona/massimizza/ripristina la finestra in qualsiasi momento, e in tutte le 8 schermate F3.5 percepisce un layout fluido e proporzionale: la scacchiera resta sempre quadrata e completamente visibile, il pannello laterale si comprime o espande senza tagliare nulla, le card e i bottoni mantengono proporzioni leggibili, niente clipping della parte bassa della scacchiera (defect originale), niente scrollbar inaspettate.

### 2.2 Criteri operativi (A4.5.x)

| ID | Criterio | Verifica |
|---|---|---|
| **A4.5.1** | `mvn -pl client verify -DexcludedGroups=slow,performance` BUILD SUCCESS | output Maven |
| **A4.5.2** | Coverage JaCoCo client ≥ 60% line+branch (gate `haltOnFailure=true` invariato) | `target/site/jacoco/jacoco.csv` |
| **A4.5.3** | SpotBugs 0 High, Spotless OK | output Maven |
| **A4.5.4** | **Defect originale risolto**: alla risoluzione del cliente (vedere stop point §10.4 per l'esatta coppia `width × height` riportata dall'utente), la scacchiera è interamente visibile (8 righe + 8 colonne), nessun clipping della parte bassa | screenshot manuale + test parametrizzato |
| **A4.5.5** | **Scacchiera quadrata sempre**: `BoardView.width == BoardView.height` su tutte le risoluzioni testate (1280×720, 1366×768, 1920×1080, 2560×1440, 3840×2160) e su window resize libero. Lato della scacchiera = `min(stageWidth - sidePanelMin, stageHeight - headerFooterReserved) - paddingFrame` | TestFX parametrizzato + visual review |
| **A4.5.6** | **Min window size enforced**: `Stage.minWidth = 1024`, `Stage.minHeight = 720` (sotto questi, il sistema non lascia ridurre la finestra). **Initial size**: **80% dell'area primary screen, centrato** (computed via `Screen.getPrimary().getVisualBounds()` × 0.80, clamp a min 1024×720). Stage state persistence in v3 schema (Task 4.5.7b) restora last-known state al launch successivo con fallback 80% se invalid | inspection codice + manual |
| **A4.5.7** | **Nessun clipping vertical / horizontal** in nessuna delle 8 schermate F3.5 al ridimensionamento libero entro min-size; nessuna ScrollBar appare quando non necessaria | TestFX parametric + manual |
| **A4.5.8** | **Side panel adattivo**: il pannello laterale (storia mosse + giocatori + tempi) si comprime sotto soglia `sidePanelPreferredWidth = 320px` fino a `sidePanelMinWidth = 240px`; sotto 240px (finestra molto stretta) si nasconde con toggle (stop point §10.6) | manual review + test |
| **A4.5.9** | **Tipografia adattiva**: i font display rispettano clamp `min(displaySize, max(displayFontSize, viewportWidth × scaleFactor))` per evitare titoli giganteschi a 4K e illeggibili a 1280×720. Token CSS `font-size-display-md/lg` invariati come baseline; nuove classi `display-fluid` derivate via JavaFX-bind o calc CSS | inspection token + visual review |
| **A4.5.10** | **`UiScalingService` 100/125/150 preservato e ortogonale**: il toggle UI scaling (a11y, F3) continua a funzionare sopra il layout responsive — i due meccanismi non si combinano in modo sbagliato. Test esplicito: scaling 150% × viewport 1366×768 → board ancora visibile e quadrata | test integration |
| **A4.5.11** | **DPI/HiDPI safe**: su monitor 4K con scaling Windows 200% (effective 1920×1080 logical), il client si comporta come a 1920×1080 fisico (no rendering blurry, no font tiny). JavaFX 21 gestisce DPI awareness implicitamente; verificare che nessun calcolo nel codice usi pixel fisici hardcoded | inspection + manual |
| **A4.5.12** | **Resize fluido (no jank)**: durante drag bordo finestra, FPS percepito ≥ 30 (no stutter visibile, no re-layout cascading) — le animazioni in corso (mossa pezzo, particle puff) NON sono interrotte dal resize | manual verifica visual review |
| **A4.5.13** | **Anti-pattern #15 preservato**: tutti i `ThemeService.applyTheme` invariati, token CSS v2 invariati, BoardRenderer/PieceNode/MoveAnimator/ParticleEffects/AudioService invariati — diff Git mostra solo modifiche a layout/binding/CSS responsive, NON a colori/texture/font/animation params | git diff review + grep |
| **A4.5.14** | **Asset texture wood ri-tilate**: la `BackgroundImage` della tavola/header gestisce REPEAT correttamente al resize (no stretch, no pixelation). Verificare scegliendo background mode `REPEAT` invece di `STRETCH` se non già | inspection + visual |
| **A4.5.14b** | **Asset audit**: ogni texture wood + cornice frame in `client/src/main/resources/assets/textures/` ha risoluzione ≥ 2048×2048. Se inferiore, sostituita con asset CC0 superiore PRIMA del BoardView refactor; `CREDITS.md` aggiornato | inspection + audit pre-Task 4.5.4 |
| **A4.5.14c** | **Aspect ratio ultrawide**: su 21:9 (3440×1440) e 32:9 (5120×1440) il cluster (board + side panel) resta centrato con maxWidth ~70% del viewport, margini decorativi wood texture ai lati. Niente board che cresce indefinita su super-ultrawide | manual visual review (test parametric non include questi aspect — l'overhead non vale per market share residuo) |
| **A4.5.14d** | **Stage state persistence v3**: `~/.dama-italiana/config.json` schema v3 aggiunge campi `windowWidth/Height/X/Y/Maximized`; restore automatico al launch successivo; fallback al computed 80% se invalid (monitor scollegato, coordinate fuori bounds, schema v2 senza i campi) | unit test serialization + manual restart |
| **A4.5.15** | **TRACEABILITY** aggiornato: nuove righe per A4.5.x | inspection |
| **A4.5.16** | **Manual demo run su PC cliente** (o VM con stessa risoluzione): cliente conferma che il defect originale è risolto e che il layout funziona su finestre ridimensionate liberamente | log riempito in TEST-PLAN-fase-4.5 §7 |
| **A4.5.17** | **Nessun TODO/FIXME pending** introdotto in `client/src/main/java/` da F4.5 | grep |
| **A4.5.18** | **`mvn clean verify`** (root, `slow`+`performance` inclusi) BUILD SUCCESS finale — nessuna regression cross-modulo | output Maven |
| **A4.5.19** | `package-info.java` per eventuali nuovi sotto-package (es. `client.layout` se introdotto) | inspection |

---

## 3. Requisiti SPEC coperti / aggiornati

### 3.1 FR coperti

Nessuna modifica funzionale: F4.5 è puramente layout/visual. Tutti gli FR-SP, FR-RUL, FR-LAN, FR-COM resta invariati.

### 3.2 NFR coperti / pull-forward

| NFR | Descrizione SPEC | Stato corrente | F4.5 obiettivo |
|---|---|---|---|
| NFR-U-01 | Localizzazione IT/EN | ✅ F3 (UI), Deferred F11 (accessibleText) | Invariato |
| NFR-U-02 | Dark/Light mode | Light implementato F3.5; Dark deferred F11 | Invariato |
| NFR-U-03 | Accessibilità tastiera | ✅ F3 | Invariato (verifica che layout responsive non rompa keyboard nav) |
| NFR-U-04 | Contrasto WCAG AA | ✅ F3.5 (light) | Invariato (verifica che font fluidi mantengano contrast) |
| **NFR-U-05** | **NUOVO — UI responsive desktop** (vedi CR §11) | **Mancante** | **Coperto pienamente** in F4.5 |
| NFR-P-01 | 60 FPS durante animazioni | ⚠️ Manual F3.5 | Verifica che resize fluido non rompa il target |
| NFR-M-02 | Coverage UI ≥ 60% | ✅ F3.5 | Preservato |

### 3.3 Acceptance criteria globali (§17)

Nessun AC §17.x esistente impatta direttamente F4.5. CR §11 propone aggiunta di AC §17.2.8 "UI responsive su gamut desktop standard".

---

## 4. Decomposizione in task

Ordine sequenziale; dipendenze lineari salvo dove esplicitato. **Stima**: ~10 task.

### Task 4.5.1 — SPEC change request + ADR layout responsive

- Aprire CR (vedi §11) e attendere approvazione utente PRIMA di procedere.
- Una volta approvata: applicare diff a `SPEC.md` (NFR-U-05 + §13.7 + §17.2.8 + §16 entry F4.5).
- **Nuovo ADR-043** in `ARCHITECTURE.md`: "Strategia layout responsive — JavaFX `Region.bind` + `min/pref/max` constraints + `BorderPane` strutturale per 8 schermate; nessun framework esterno (no FXyz, no ControlsFX layout helpers)".
- **Moduli toccati**: docs only.
- **Dipendenze**: nessuna. **Output stop point §10.1 (CR)**.

### Task 4.5.2 — Audit baseline su tutte le 8 schermate

- Eseguire screenshot delle 8 schermate F3.5 a 5 risoluzioni canoniche (1280×720, 1366×768, 1920×1080, 2560×1440, 3840×2160) + 1 screenshot dimensione finestra "narrow" (1024×720) e "tall" (1280×1024). 7 × 8 = 56 screenshot in `tests/visual-review/responsiveness-baseline-pre-fix/`.
- Documentare in `AI_CONTEXT.md` "SPEC clarifications" il defect specifico per ogni schermata problematica (board view è sicuro; le altre 7 da verificare).
- **Moduli toccati**: docs/test only (nessun cambio prod).
- **Dipendenze**: 4.5.1 approvato.

### Task 4.5.3 — `Stage` min-size + initial-size computed (80%) + maximize policy

- In `JavaFxApp.start(Stage)` (e in `SceneRouter` se serve):
  - `stage.setMinWidth(1024); stage.setMinHeight(720);`
  - **Initial size computed**: `Rectangle2D b = Screen.getPrimary().getVisualBounds(); double w = max(1024, b.getWidth() * 0.80); double h = max(720, b.getHeight() * 0.80);` + center: `stage.setX(b.getMinX() + (b.getWidth()-w)/2); stage.setY(b.getMinY() + (b.getHeight()-h)/2);`.
  - Stage state persistence (vedi Task 4.5.7b): se `config.json` v3 ha state valido → restore; altrimenti fallback al computed 80%.
- Verificare che Save Dialog modal stage **non** erediti la min-size (modal piccolo è OK). Aggiungere se necessario `dialogStage.setResizable(false)` o ridimensionamento min adatto al contenuto.
- Splash screen: probabilmente **non** vuole min-size (è transitorio, ~1.5s). Verificare.
- **Test**: `JavaFxAppTest` (nuovo o esteso) verifica `stage.getMinWidth() == 1024`, initial size sopra min, position centrata su primary screen.
- **Moduli toccati**: `client`. **Dipendenze**: 4.5.2.

### Task 4.5.3b — Asset audit texture wood (prerequisite di Task 4.5.4)

- Audit `client/src/main/resources/assets/textures/`: ogni texture wood (board light/dark, frame, header background) deve avere risoluzione **≥ 2048×2048** per restare nitida a 4K. La cornice frame (BorderImage) deve essere ≥ 1024×1024.
- Se < soglia: source replacement CC0 da Poly Haven / Pixabay / textures.com, audit licenza, aggiornare `client/src/main/resources/assets/CREDITS.md`.
- **Output**: report breve in `AI_CONTEXT.md` "decisioni recenti" con esito audit (asset OK o sostituiti).
- **Moduli toccati**: `client/src/main/resources/assets/` + `CREDITS.md`. **Dipendenze**: 4.5.2.

### Task 4.5.4 — `BoardView` adattivo (defect cliente)

- **Cuore del fix originale**. Convertire l'attuale layout della BoardView (probabilmente `GridPane` con cell size hardcoded o weak binding) in:
  - Un `StackPane` root che contiene la cornice frame texture wood + un `Pane` figlio centrato.
  - Il `Pane` figlio ha lato `boardSide = min(stackPane.width - 2*frameThickness, stackPane.height - 2*frameThickness)`.
  - Le 64 celle e i pezzi sono posizionati su questo `Pane` con coordinate scalate `cellSize = boardSide / 8`.
  - Bind su `widthProperty` + `heightProperty` di `stackPane` per re-layout automatico.
- Conservare cornice texture wood (anti-pattern #15): la cornice deve scalare con la scacchiera, non avere thickness fisso.
- Conservare `BoardCellNode`/`PieceNode` esistenti — solo il layout cambia, non i nodi.
- Conservare `MoveAnimator` — verificare che `TranslateTransition` calcoli correttamente le destinazioni post-resize (probabilmente già OK se usa coordinate Cell-based).
- **Test**: `BoardViewLayoutTest` (TestFX, parametrizzato su 5 risoluzioni canoniche): verifica `boardSide = boardView.getWidth() == boardView.getHeight()`, posizione delle celle 1..64, no clipping inferiore.
- **Moduli toccati**: `client`. **Dipendenze**: 4.5.3.

### Task 4.5.5 — Side panel responsive

- Il pannello laterale (storia mosse + giocatori + tempi) attualmente probabilmente ha width fisso. Convertire a:
  - `prefWidth = 320`, `minWidth = 240`, `maxWidth = 400` (token spacing). Se viewport < 1024 → toggle hide (stop point §10.6).
  - `VBox` interno con `Region` filler che si espande/contrae verticalmente.
  - `ListView` storia mosse: `VBox.vgrow = ALWAYS` per occupare lo spazio rimanente.
- **Test**: `SidePanelLayoutTest` parametrizzato su `(stageWidth, expectedSidePanelWidth)`.
- **Moduli toccati**: `client`. **Dipendenze**: 4.5.4 (la BoardView occupa il main slot del `BorderPane` center; il side panel sta a right).

### Task 4.5.6 — Main menu + 5 form screens responsive

- Main menu (6 card grid): da grid fisso a `FlowPane` o `GridPane` con column constraints `percentWidth` o `prefWidth` adattivi.
- Single-player setup, save dialog, load screen, settings, rules: ognuna ha card centrale + (opzionalmente) sidebar. Convertire a `VBox`/`HBox` con `maxWidth` token (es. 600px / 800px) + center alignment, così la card resta leggibile a 4K e usa tutta la viewport a 1280×720.
- **Test**: estendere i `*FxmlSmokeTest` esistenti con assertion su pref/min/max width dei root container.
- **Moduli toccati**: `client` (FXML + CSS + controllers). **Dipendenze**: 4.5.4.

### Task 4.5.6b — Aspect ratio handling ultrawide (21:9 / 32:9)

- In `SceneRouter` o nel root `BorderPane` di ogni scena: introdurre `maxWidth` constraint sul cluster centrale (board + side panel) ~`1920px` o `viewport.width × 0.70`, max il maggiore tra i due. I margini laterali si riempiono con la stessa `BackgroundImage` wood texture in REPEAT mode (decorativi).
- Test manuale (no parametric): screenshot a 3440×1440 (21:9) e 5120×1440 (32:9) salvati in `tests/visual-review/responsiveness-baseline-post-fix/ultrawide/`. Mercato ultrawide è < 5% (Steam Hardware Survey 2024-2025), quindi non vale l'overhead di parametric automatizzato.
- **Moduli toccati**: `client` (CSS + Java). **Dipendenze**: 4.5.4 + 4.5.5.

### Task 4.5.7 — Tipografia fluida + token responsive

- Aggiungere classi CSS `display-fluid` + `display-fluid-lg` che usano `-fx-font-size: ...` calcolato come `min(48px, max(28px, viewport_width × 0.025))`. JavaFX CSS NON supporta `clamp()` nativo, quindi probabilmente serve binding programmatico via `JavaFxScalingHelper.bindFluidFontSize(label, baseSize, scaleFactor)`.
- I token `-font-size-display-md/lg` esistenti restano invariati come baseline. Le nuove classi sono additive.
- Applicare alle label "display" delle 8 schermate: titoli main menu, header schermate, etichette card.
- **Test**: `FluidFontTest` verifica binding correttezza su 3 viewport.
- **Moduli toccati**: `client`. **Dipendenze**: 4.5.4 + 4.5.6.

### Task 4.5.7b — Stage state persistence (config.json schema v3)

- Estendere `~/.dama-italiana/config.json` da schema v2 (F3.5) a v3 con migrazione trasparente. Nuovi campi:
  - `windowWidth: int` (logical pixels)
  - `windowHeight: int`
  - `windowX: int` (top-left X, logical)
  - `windowY: int` (top-left Y, logical)
  - `windowMaximized: boolean`
- Migration v2 → v3: se i campi mancano, init con `null`/`false` e fallback a computed 80% al primo launch v3.
- Save policy: chiamato in `Stage.setOnCloseRequest`; salva sempre `width/height/x/y/maximized` last-known.
- Restore policy in `JavaFxApp.start`: se schema ≥ v3 e i campi sono valid (width/height ≥ minSize, X/Y dentro bounds di un Screen visibile), apply; altrimenti fallback computed 80%.
- Validation: nuovo helper `StagePersistenceValidator.isStateValid(state, screens)` con unit test (monitor scollegato simulato → invalid; window oversize → invalid; window normale → valid).
- **Test**: `PreferencesServiceTest` esteso con migration v2→v3 + roundtrip; `JavaFxAppTest` esteso con restore-from-config simulation.
- **Moduli toccati**: `client` (PreferencesService + JavaFxApp). **Dipendenze**: 4.5.3.

### Task 4.5.8 — `BackgroundImage` REPEAT mode + frame scaling

- Verificare che le texture wood in tutte le schermate usino `BackgroundRepeat.REPEAT` (sia X sia Y) e `BackgroundSize` con `cover = false` per evitare stretching.
- La cornice frame attorno alla scacchiera: deve scalare proporzionalmente. Se attualmente è un `BorderImage` con thickness fisso, convertire a `BorderImage` con `Insets` proporzionali al `boardSide`.
- **Test**: visual review screenshot (no programmatic test possibile per texture).
- **Moduli toccati**: `client` (CSS + Java). **Dipendenze**: 4.5.4.

### Task 4.5.9 — Test parametrizzato + screenshot baseline post-fix

- Nuovo test class `ResponsivenessParametricTest` (TestFX, `@ParameterizedTest @MethodSource("canonicalResolutions")`) con **7 combinazioni strategiche** (revised dalla 5 originale per coprire DPI scaling Win 10/11 reale):
  1. `1024×720 @ 100% DPI` — floor assoluto, side panel auto-hide attivo
  2. `1280×720 @ 100% DPI` — small laptop
  3. `1366×768 @ 125% DPI` — Windows laptop più diffuso 2018-2024
  4. `1920×1080 @ 100% DPI` — FHD desktop standard
  5. `1920×1080 @ 150% DPI` — FHD su laptop 13" premium
  6. `2560×1440 @ 100% DPI` — QHD desktop
  7. `3840×2160 @ 200% DPI` — 4K logical 1920×1080 (rendering native sharpness)
- Per ogni schermata F3.5 (8) × ogni combinazione (7) = 56 assertion bundle. Per ogni:
  - Set `stage.setWidth/Height(w, h)` + `stage.getRenderScaleX/Y` simulato via JVM args (per il DPI test, eseguire come test category).
  - Assert no clipping (root container `bounds.maxY <= scene.height`).
  - Assert no scrollbar (cerca `ScrollPane` figlio attivo con `hbarPolicy != NEVER` o `vbarPolicy != NEVER`).
  - Assert board (se presente) è quadrata: `|board.width - board.height| < 1.0`.
- Aggiungere `@Tag("slow")` perché 56 combinazioni × ~200ms each = ~12s.
- Salvare 56 screenshot post-fix in `tests/visual-review/responsiveness-baseline-post-fix/` per regression visuale futura.
- **Out of parametric matrix** (manual visual review separato): ultrawide 21:9 + 32:9 (vedi Task 4.5.6b).
- **Moduli toccati**: `client`. **Dipendenze**: 4.5.4 → 4.5.8.

### Task 4.5.10 — Documentation + ADR + CHANGELOG + TRACEABILITY + closure

- ADR-043 finalizzato (vedi 4.5.1).
- `tests/TRACEABILITY.md`: nuova sezione "Acceptance criteria di Fase 4.5 (PLAN-fase-4.5.md §2.2)" + righe NFR-U-05 nuove + AC §17.2.8 nuovo.
- `CHANGELOG.md`: voce sintetica per F4.5 closure.
- `AI_CONTEXT.md`: stato avanzato post-task, Sotto-fase IMPLEMENTA chiusa.
- **Moduli toccati**: docs only. **Dipendenze**: 4.5.1..4.5.9.

---

## 5. Strategia di test

### 5.1 Composizione (CLAUDE.md §2.4.1)

| Tipo | Target | Tooling | Cosa testa in F4.5 |
|---|---|---|---|
| **Unit** | ~50% | JUnit 5 + AssertJ | Layout calculation pure (es. `BoardSizingHelper.computeBoardSide(viewport, sidePanelWidth, frameThickness)`), `JavaFxScalingHelper.bindFluidFontSize` |
| **Integration / UI** | ~40% | TestFX + JavaFX | `BoardViewLayoutTest`, `SidePanelLayoutTest`, `*FxmlSmokeTest` esteso con assertion responsive, `ResponsivenessParametricTest @Tag("slow")` con 5 risoluzioni × 8 schermate |
| **Visual regression** | ~5% | Screenshot baseline | 56 screenshot pre-fix + 56 post-fix in `tests/visual-review/` per regression manuale futura |
| **E2E manuale** | ~5% | Manual run su PC cliente o VM | Defect originale risolto + libero resize verificato dal cliente |

### 5.2 Coverage target (NFR-M-02)

| Modulo | Coverage minima | Note |
|---|---|---|
| `client` | ≥ 60% line+branch (gate invariato) | Esclusioni F3.5 invariate; F4.5 aggiunge nuove classi `client.layout` (se introdotte) — verificare se necessario aggiungere esclusioni per helper JavaFX-bound. |
| Altri moduli | invariati (F4.5 non li tocca) | — |

### 5.3 Regression

- Tutti i 321 test `client` F3.5 + 166 `core-server` F4 + 391 `shared` F1+F2 + 1 `server` F0 = **879 test verdi** invariati.
- `RuleEngineCorpusTest` F1 (53 posizioni) + `AiTournamentSimulationTest` F2 (gating ≥95/100) DEVONO continuare a passare.

### 5.4 Naming convention

Stile per modulo `client`: `should<Espressione>_when<Condizione>` (uniforme con F3+F3.5). Test parametric usa `@DisplayName("[{0}x{1}] <Feature><Scenario>")`.

---

## 6. Rischi e mitigazioni

| ID | Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|---|
| R-1 | Cambio layout rompe animazioni F3.5 (`MoveAnimator`, `ParticleEffects`, `AnimationOrchestrator`). | Media | Alto | Test `MoveAnimatorTest` esistenti continuano a girare; aggiungere test `ResponsiveBoardAnimationTest` che verifica animazione mossa funziona su viewport 1280×720 e 4K. Non rifattorizzare gli animatori — solo i layout container. |
| R-2 | TestFX non disponibile / instabile in CI (no display). | Media | Medio | Il pattern F3.5 con `Assumptions.assumeTrue(fxToolkitReady)` funziona già; F4.5 lo riusa. Test parametrico headless via Monocle. |
| R-3 | `UiScalingService` (a11y 100/125/150) interagisce in modo confuso con il layout responsive. | Media | Alto | Test esplicito A4.5.10. Documentare il comportamento ortogonale: scaling moltiplica le size effettive del nodo, layout responsive si adatta alla viewport effettiva (post-scaling). |
| R-4 | DPI awareness Windows: scaling 200% su 4K → JavaFX vede 1920×1080 logici, pixel fisici 3840×2160. Comportamenti misti tra `Screen.getPrimary().getBounds()` (logici) e calcoli pixel hardcoded. | Media | Medio | Audit del codice esistente per pixel hardcoded; usare sempre logical size. ADR-043 documenta la convenzione. |
| R-5 | Texture wood pixelizzano a 4K se la risoluzione asset è bassa. | Bassa | Medio | Audit asset attuali: se < 2048×2048 pixel, sostituire con asset CC0 più grandi. `CREDITS.md` aggiornato. |
| R-6 | Resize fluido < 30 FPS su hardware target Intel UHD 620. | Bassa | Medio | Profilare con JavaFX Pulse logger su VM target; ottimizzare se necessario (es. disabilitare effetti durante drag bordo finestra). |
| R-7 | Manual demo run su PC cliente non disponibile (cliente lontano). | Media | Basso | Fallback: VM Windows con risoluzione cliente o screenshot dimostrativi. Stop point §10.4 cattura risoluzione esatta. |
| R-8 | Scope creep verso ridisegno F3.5 (tentazione di "già che ci siamo, ritocchiamo X"). | Alta | Alto | Anti-pattern #15 ribadito. Diff finale **deve** mostrare solo modifiche layout/binding/CSS — non token, non texture, non animation params. Review esplicita in REVIEW Fase 4.5 verifica questo. |

---

## 7. Stop points / decisioni utente da confermare

### 10.1 SPEC change request (vedi §11)

CR-F4.5-001: aggiungere NFR-U-05 + §13.7 + AC §17.2.8 + entry F4.5 in §16.

**Raccomandazione**: approvare. Senza di essa la fase non ha base SPEC.

### 10.2 Ambito scope: solo defect cliente (board clipping) o adattività completa 8 schermate?

| Opzione | Pro | Contro |
|---|---|---|
| **A — Solo defect originale** (BoardView) | Rapido (~3 task), risolve l'issue cliente immediato | Le altre 7 schermate restano problematiche su risoluzioni edge; debt destinato a F11 |
| **B — Adattività completa 8 schermate (questo PLAN)** | Stato consistente; F11 si concentra solo su replay/profilo/dark/i18n | ~10 task, ~3-5 giorni di lavoro |

**Raccomandazione**: **B**. Mezza dose lascia debt visibile al cliente sulle altre schermate.

### 10.3 Min-window-size baseline

| Opzione | Pro | Contro |
|---|---|---|
| `1024×720` | Compatibile con netbook, schermi vecchi | Stretto per il side panel + board; potrebbe richiedere toggle hide del panel |
| `1280×720` | Ragionevole per laptop ultimi 10 anni | Esclude netbook (poco significativi nel target cliente desktop) |
| `1366×768` | Standard "minimo accettabile" Windows laptop moderno | Esclude alcuni laptop pre-2014 |

**Raccomandazione**: `1024×720` come **min** assoluto (con side panel toggle), `1280×720` come **initial size raccomandata**.

### 10.4 Risoluzione esatta del PC cliente

L'utente deve confermare la risoluzione fisica + scaling Windows del monitor del cliente dove il defect appare. Se non disponibile, assumere `1366×768` (standard laptop budget) o `1920×1080` con scaling 100% e definire un test parametrizzato che lo includa.

**Raccomandazione**: utente fornisce la coppia esatta; se non disponibile, fallback su `1366×768`.

### 10.5 Initial window size al primo lancio

| Opzione | Pro | Contro |
|---|---|---|
| `1280×720` | Comodo per laptop stretti | Stretto per maximize a 4K |
| `1366×768` | Standard 16:9 laptop | Idem |
| Centrato + 80% schermo | Adattivo a hardware | Variabilità tra utenti, harder to reproduce |

**Raccomandazione**: `1366×768` fixed initial; utente massimizza/restora a piacere. Stage state ricordato in `~/.dama-italiana/config.json` come bonus opzionale (NON in scope F4.5 — eventuale follow-up).

### 10.6 Side panel toggle hide quando viewport molto stretta

Sotto i 1024px di viewport (eccezione: hipster con finestra mezza schermo), il side panel si auto-nasconde con bottone toggle "» » »" per mostrarlo via overlay.

| Opzione | Pro | Contro |
|---|---|---|
| Auto-hide con toggle | Adattivo, niente clipping | Aggiunge un nuovo controllo UI (toggle) |
| Side panel sempre visibile, board scala anche meno | Semplice | Board minuscolo a 1024×720, gameplay difficoltoso |

**Raccomandazione**: auto-hide con toggle (visibile solo quando attivo).

### 10.7 Strategia tipografica fluida

| Opzione | Pro | Contro |
|---|---|---|
| Static token CSS (no fluid) | Zero codice nuovo | Titoli giganteschi a 4K |
| Programmatic binding via `JavaFxScalingHelper.bindFluidFontSize` | Precisi, testabili | ~50 binding sites = manutenzione |
| Dimensione discreta a 3 viewport-bracket (small/medium/large via CSS pseudo-class) | CSS-native, less code | Gradini visibili al resize |

**Raccomandazione**: opzione 2 (programmatic binding) per la display tipography (titoli `display-md/lg`), opzione 1 (static) per UI body. ~10 binding sites in totale.

### 10.8 Test parametrizzato vs visual-only

Il PLAN propone test parametrizzato + screenshot baseline (56 pre + 56 post). 

**Raccomandazione**: approvare entrambi. Screenshot baseline serve per future regressioni quando si farà dark mode F11.

### 10.9 Manual demo run con cliente

| Opzione | Pro | Contro |
|---|---|---|
| Cliente esegue test manuale post-tag `v0.4.5` | Validazione reale | Richiede cliente disponibile |
| VM con risoluzione cliente esegue test manuale | Sotto controllo | Approssimazione |

**Raccomandazione**: idealmente entrambi. Manuale VM + manual cliente come confirma finale.

### 10.10 Tempistica

Stima: 10 task, ~3-5 giorni di lavoro full-time. Tag `v0.4.5` realizticamente entro 1 settimana dal kickoff.

---

## 11. SPEC change request

### CR-F4.5-001 — Aggiungere NFR-U-05 + §13.7 + §17.2.8 + entry F4.5 in §16

#### Contesto

SPEC v2.2 NON contiene requisiti di adattività UI desktop (responsiveness, min-resolution, behavior on resize). L'unico riferimento al "scaling" è §13.5 a11y (UI 100/125/150) che è feature ortogonale alla responsiveness alla viewport.

Il defect cliente post-MSI-install (parte bassa scacchiera tagliata, finestra non si adatta al resize) ha esposto il gap.

#### Diff proposto a `SPEC.md`

##### A — Aggiungere riga in §5 (Requisiti non funzionali) sotto NFR-U-04

```
| NFR-U-05 | UI client adattiva al gamut desktop standard: ridimensionamento finestra fluido, no clipping, no scrollbar inattese, scacchiera sempre quadrata e completamente visibile, su risoluzioni da 1280×720 a 4K (3840×2160) e su DPI scaling Windows 100/125/150/200%. Min window size 1024×720. |
```

##### B — Aggiungere nuova sezione §13.7 (Layout responsive)

```markdown
### 13.7 Layout responsive (da Fase 4.5)

Il client adotta un layout **fluido** che si adatta al ridimensionamento della finestra senza clipping né scrollbar inattese. Strategie:

- **`Stage` constraints**: `minWidth = 1024`, `minHeight = 720`, `initialWidth = 1366`, `initialHeight = 768`. Stage state non persistito (out of scope F4.5).
- **`BoardView`**: contenitore `StackPane` + `Pane` figlio centrato. Lato della scacchiera = `min(viewport.width - sidePanelMin, viewport.height - headerFooterReserved) - frameThickness`. Cornice texture wood scala proporzionalmente (BorderImage con `Insets` proporzionali al `boardSide`).
- **Side panel** (storia mosse): `prefWidth = 320`, `minWidth = 240`, `maxWidth = 400`. Sotto viewport 1024px → auto-hide con toggle button overlay.
- **Main menu / form screens**: `VBox`/`HBox` con `maxWidth` token (600/800px) + center alignment.
- **Tipografia display fluida**: titoli display usano binding programmatico `clamp(min, viewportWidth × scaleFactor, max)`. Token base `font-size-display-md/lg` invariati.
- **Texture background**: `BackgroundRepeat.REPEAT` (no stretch), risoluzione asset ≥ 2048×2048 per evitare pixelation a 4K.
- **DPI awareness**: JavaFX 21 implicit; nessun pixel hardcoded nel codice; logical size sempre.
- **`UiScalingService`** (a11y 100/125/150 da §13.5): ortogonale al layout responsive. Lo scaling moltiplica le size effettive dei nodi; il layout responsive si adatta alla viewport effettiva post-scaling.

Tutte le 8 schermate F3.5 (splash, main menu, sp setup, board, save dialog, load screen, settings, rules) seguono questa strategia. Visual regression baseline a 5 risoluzioni canoniche (1280×720, 1366×768, 1920×1080, 2560×1440, 3840×2160) in `tests/visual-review/responsiveness-baseline-post-fix/`.
```

##### C — Aggiungere AC §17.2.8

```
8. **UI responsive desktop**: il client si adatta fluido a tutte le risoluzioni desktop standard (1280×720 → 4K) e a window resize libero, senza clipping né scrollbar inattese. Verificato via `ResponsivenessParametricTest @Tag("slow")` (40 combinazioni schermata × risoluzione) + manual demo run.
```

##### D — Aggiungere entry §16 Fase 4.5

```markdown
### Fase 4.5 — UI Responsiveness desktop

> Mini-fase intermedia tra Fase 4 e Fase 5, fix mirato post-demo cliente. **Pull-forward** parziale di feature di adattività UI da F11. Stessa modalità della F3.5 (mini-fase intermedia, tag `v0.4.5`).

- Adattività delle 8 schermate F3.5 al gamut risoluzioni desktop standard (1280×720 → 4K).
- `Stage` min/initial size enforced. `BoardView` adattivo (defect originale risolto).
- Side panel responsive con auto-hide toggle.
- Tipografia display fluida.
- Texture wood REPEAT mode + cornice scalabile.
- ADR-043 documenta la strategia.
- **Anti-pattern #15 preservato**: design system F3.5 wood premium INVARIATO. Solo layout/binding/CSS responsive.
- Test parametrizzato 5 risoluzioni × 8 schermate. Screenshot baseline pre/post-fix.

**Acceptance**: cliente conferma defect originale risolto + libero resize verificato su PC reale o VM con risoluzione cliente.
```

#### Decisione utente

PENDING — approvazione esplicita richiesta a stop point §10.1 prima di applicare diff a SPEC.

---

## 12. Riepilogo per approvazione

In sintesi, F4.5 è un fix mirato di adattività UI desktop con i seguenti vincoli:

1. **Pull-forward** stile F3.5 → mini-fase intermedia, branch dedicato `feature/4.5-ui-responsiveness`, tag `v0.4.5`.
2. **Anti-pattern #15** preservato → solo layout/binding, NIENTE re-skin.
3. **SPEC change request** CR-F4.5-001 da approvare prima di procedere.
4. **10 task** decomposti in §4.
5. **Stop points** §10.1..§10.10 da confermare.
6. **Stima**: ~3-5 giorni full-time, tag entro 1 settimana.

Confermi di approvare il PLAN così com'è (con le raccomandazioni nei stop points)? Oppure vuoi modificare qualcuno dei stop point §10.x prima di procedere?
