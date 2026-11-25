# TEST PLAN — Fase 4.5: UI Responsiveness desktop

- **Data piano**: 2026-05-06.
- **Data finalizzazione**: 2026-05-06 (chiusura sotto-fase TEST).
- **SPEC version**: 2.3 (post CR-F4.5-001).
- **Branch**: `feature/4.5-ui-responsiveness`.
- **PLAN di riferimento**: [`plans/PLAN-fase-4.5.md`](../plans/PLAN-fase-4.5.md).
- **REVIEW di riferimento**: [`reviews/REVIEW-fase-4.5.md`](../reviews/REVIEW-fase-4.5.md) (chiusa 2026-05-06, commit `30131e3`).
- **Stato**: FINAL — popolato post fix-loop REVIEW + manual demo cliente.

---

## 1. Scope

Vedi `plans/PLAN-fase-4.5.md` §1-§2.

In sintesi: **mini-fase intermedia** tra F4 (`v0.4.0` core-server skeleton) e F5 (server transport reale), motivata dal defect cliente "parte in basso della scacchiera tagliata + finestra non si adatta al resize" rilevato sulla demo F3.5. Tre macroaree sotto test:

1. **Stage lifecycle**: `PrimaryStageInitializer` (min 1024×720 + initial 80% del primary screen visualBounds + center policy); `StagePersistenceCoordinator` (restore state al launch + persist on close, schema v3 con campi `windowWidth/Height/X/Y/Maximized`); `StagePersistenceValidator` (rifiuta state offscreen, sotto floor, < 50% intersection con union dei screen visualBounds).
2. **Layout responsive**: `BoardRenderer.layoutChildren` 8×8 grid centrato (xOffset/yOffset = (w/h - 8×cellSize) / 2, particle layer overlays the centered area); side panel adattivo (`prefWidth=320, minWidth=240, maxWidth=400`); main menu / 5 form screens cap a `maxWidth ∈ {560, 960, 1100, 1200}` per evitare stretch ultrawide; texture wood `cover→repeat` mode + `BoardFrameThicknessHelper` proporzionale `clamp(16, boardSide×0.035, 48)`.
3. **Tipografia fluida**: `JavaFxScalingHelper` walk del scene root, binding `styleProperty` di ogni `Labeled` con marker class `display-fluid` / `display-fluid-lg` a `clamp(min, sceneWidth × scale, max) × uiScaleFactor` — composta con `UiScalingService.activeScaleFactor()` (SPEC §13.5 a11y, F4.5 REVIEW F-009).

**Out of scope** (deferred):
- Dark mode runtime toggle (F11).
- Aspect ratio handling esplicito ultrawide 21:9 / 32:9 (Task 4.5.6b skipped per Opzione B utente; verificato manualmente in §6.3 sotto, no defect critico).
- Accessibilità a11y completa (NFR-U-03 keyboard nav già coperta F3, runtime toggle F11).
- Mobile / tablet / touch (out of SPEC).

**Modifiche al codice prod F4.5**:
- `client/`: 6 file prod nuovi (`PrimaryStageInitializer`, `StagePersistenceCoordinator`, `StagePersistenceValidator`, `JavaFxScalingHelper`, `BoardFrameThicknessHelper`, `package-info` per nuovo package `client.layout`) + 6 modificati (`JavaFxApp`, `SceneRouter`, `PreferencesService` v1/v2→v3 migration, `UserPreferences` schema v3 + clamp + `withWindowMaximized`, `BoardRenderer` centering, `BoardViewController` wiring) + 2 CSS (`theme-light.css`, `components.css`) + 7 FXML (`board-view`, `main-menu`, `sp-setup`, `settings`, `load-screen`, `rules`, `splash`).
- `shared`/`core-server`/`server`: **invariati per costruzione** (F4.5 è puramente client/layout/CSS).

---

## 2. Strategia di test

Vedi `plans/PLAN-fase-4.5.md` §5 e CLAUDE.md §2.4.1 (piramide classica).

| Tipo | Cosa | Tooling |
|---|---|---|
| **Unit (pure math)** | `JavaFxScalingHelper.computeFluidFontSize` clamp 12 parametric × 2 (`display-fluid` + `display-fluid-lg`) + 8 compose-with-UI-scale parametric; `BoardFrameThicknessHelper.computeFrameThickness` 7 parametric clamp; `StagePersistenceValidator.isStateValid` 12 cases (null/below-floor/secondary-unplugged/intersection-ratio); `StagePersistenceCoordinator.computePersistedState` 8 cases (maximized × null/valid/below-floor snapshot); `UserPreferences` 4 cases (withWindowMaximized + canonical-constructor clamp) | JUnit 5 + AssertJ |
| **Integration / FX-toolkit** | `BoardRendererLayoutTest` 7 viewport parametric; `SidePanelLayoutTest` 1 property + 6 parametric stage widths; `ScreenLayoutTest` 5 cases (5 form screens cap); `BoardFrameThicknessHelperTest` 4 FX-binding (proportional + idempotent + ultrawide-min + pre-layout fallback); `JavaFxScalingHelperTest` 3 FX-binding (binding + reattività + idempotent); `PrimaryStageInitializerTest` 4 (min size + 80% + centering + NPE); `UiScalingServiceTest` 4 (activeScaleFactor + sanitisation) | JUnit 5 + JavaFX (con `Assumptions.assumeTrue(fxToolkitReady)` per skip headless ADR-018) |
| **Visual regression / parametric assertions** | `ResponsivenessParametricTest` `@Tag("slow")` — 7 risoluzioni × 8 schermate = 56 scenari + 56 PNG dump in `tests/visual-review/responsiveness-baseline-post-fix/`. Per ogni scenario assertable (7 res × 7 schermate, save-dialog escluso): no clipping (`root.bounds ≤ scene + 1 px`), no `ScrollBar` visibile, board square (`8 × currentCellSize ≤ renderer.w/h`). | JUnit 5 + Spring Boot Test + JavaFX |
| **Schema migration** | `PreferencesServiceTest` v1→v3 + v2→v3 + roundtrip con window state + invalid schema fallback (esistente F3.5 esteso F4.5) | JUnit 5 + `@TempDir` |
| **Architectural (ad-hoc grep)** | Anti-pattern #15 attestation: nessuna modifica a token CSS palette / texture file paths / font families / animation params / `MoveAnimator` / `ParticleEffects` / `AnimationOrchestrator` / `AudioService` / `BoardCellNode` / `PieceNode` (verificato in REVIEW §4). | `git diff develop..feature/4.5-ui-responsiveness` |
| **Coverage** | Gate `client` ≥ 60% line+branch (NFR-M-02), invariato vs F3.5; nuove classi `client.layout` aggiunte al bundle (4 helper + 2 stage geometry) — copertura via i nuovi unit test | JaCoCo 0.8.12 |
| **Regression** | Tutti F1+F2+F3+F3.5+F4 verdi: corpus regole 53 posizioni, gating IA Campione ≥95/100, performance NFR-P-02, anti-cheat F4, tournament skeleton F4, persistenza schema migration F3.5 → v3 F4.5 | `mvn clean verify` root |
| **Manual** | Demo run su PC cliente / VM (A4.5.16); DPI matrix 100/125/150/200% (A4.5.11); resize fluido FPS check (A4.5.12); ultrawide aspect ratio screenshot review (A4.5.14c) | Sessione utente registrata in §6 |

---

## 3. Coverage target

Da SPEC NFR-M-02 e PLAN-fase-4.5 §5.2:

| Modulo | Coverage minima | Stato post-F4.5 |
|---|---|---|
| `client` | ≥ 60% line + branch (gate `haltOnFailure=true` invariato vs F3) | ✅ MET — `mvn -pl client jacoco:report` post regression: bundle delta 70 → 76 classi (+6 = `PrimaryStageInitializer`, `StagePersistenceCoordinator`, `StagePersistenceValidator`, `JavaFxScalingHelper`, `BoardFrameThicknessHelper`, fix update di `UserPreferences`/`PreferencesService`). Ogni nuova classe coperta dai nuovi unit test (sezione 5.1). |
| `shared` | ≥ 80% (≥ 90% `rules`, ≥ 85% `ai`) | Invariato vs F4 (no codice `shared` toccato in F4.5). |
| `core-server` | ≥ 80% line + branch | Invariato vs F4 (no codice `core-server` toccato in F4.5). |
| `server` | ≥ 70% | Invariato (singolo smoke test, F6+ aggiunge transport reale). |

Esclusioni JaCoCo (invariate vs F3.5 + nessuna nuova in F4.5): bootstrap JavaFX/Spring (`JavaFxApp`, `JavaFxAppContextHolder`, `ClientApplication`), Alert wrapper (`JavaFxUserPromptService`), anonymous cell-factory (`MoveHistoryView$MoveHistoryCell`, `LoadScreenController$MiniatureCell`, `RulesController$1`, `SettingsController$1`, `SplashController$1`).

---

## 4. Test corpus regole italiane

**Invariato** vs F4 (PLAN-fase-1 §3.5 corpus 48 + Task 3.21 tactical 5 → 53 posizioni totali). F4.5 NON tocca `RuleEngine` né il corpus JSON. La regression `mvn clean verify` root con `slow`+`performance` (E5, §5.2) verifica che `RuleEngineCorpusTest` parametrizzato continui a passare su 53 posizioni invariate.

---

## 5. Esecuzioni pianificate

| # | Esecuzione | Quando | Scope | Esito | Commit |
|---|---|---|---|---|---|
| E1 | `mvn -pl client verify -DexcludedGroups=slow,performance` | Ad ogni task IMPLEMENTA (4.5.1 → 4.5.10) | client unit + integration, fast tags | ✅ BUILD SUCCESS ad ogni task; conta finale post-IMPLEMENTA: **379 verdi** | `c51c145` (Task 4.5.10 closure) |
| E2 | `mvn -pl client test -Dgroups=slow -Dtest=ResponsivenessParametricTest` | Task 4.5.9 closure | A4.5.7 + A4.5.5 + A4.5.4 — 56 scenari parametric assertion + 56 PNG dump post-fix | ✅ BUILD SUCCESS in 32.39 s, **56/56 verdi** | `59497cb` |
| E3 | `mvn -pl client verify -DexcludedGroups=slow,performance` | Sotto-fase REVIEW closure | client fast (post fix loop F-001..F-015) | ✅ BUILD SUCCESS, **405 verdi** (379 + 26 nuovi: 8 `StagePersistenceCoordinatorTest` + 4 `UserPreferencesTest` + 3 `StagePersistenceValidatorTest` 50%-intersection + 1 `BoardFrameThicknessHelperTest` pre-layout fallback + 8 `JavaFxScalingHelperTest` compose parametric + 2 `UiScalingServiceTest` activeScaleFactor) | `30131e3` (REVIEW closure) |
| E4 | `mvn -pl client test -Dgroups=slow -Dtest=ResponsivenessParametricTest` | Sotto-fase REVIEW closure | regression slow post fix loop (validator stricter, fluid typography composed) | ✅ BUILD SUCCESS in 45.89 s, **56/56 verdi invariati** | `30131e3` |
| E5 | `mvn clean verify` (root, no `excludedGroups`) | Sotto-fase TEST closure | tutti i moduli + corpus F1 (53 pos) + gating IA F2 (≥95/100) + performance NFR + slow stress F4 + slow F4.5 | ✅ BUILD SUCCESS — vedi §5.2 sotto | (questo TEST-PLAN) |

### 5.1 Test code aggiunto/modificato in F4.5

Lista cumulativa per task (IMPLEMENTA + REVIEW fix loop). Saldo finale: **+84 test client** vs F4 baseline (321 → 405 fast). Slow tag: +56 (`ResponsivenessParametricTest`) + 56 esistenti (`BaselineScreenshotCapture` Task 4.5.2 pre-fix, conservato per A/B comparison).

| Task / Batch | Test class/file | Tipo | Δtest fast | Δtest slow |
|---|---|---|---:|---:|
| Task 4.5.2 | `BaselineScreenshotCapture` (audit pre-fix, 56 scenari `@Tag("slow")`) | Visual baseline | 0 | +56 slow |
| Task 4.5.3 | `PrimaryStageInitializerTest` 4 | Unit + FX-toolkit | +4 | 0 |
| Task 4.5.4 | `BoardRendererLayoutTest` 7 parametric viewport | FX-toolkit | +7 | 0 |
| Task 4.5.5 | `SidePanelLayoutTest` 1 + 6 parametric | FX-toolkit | +7 | 0 |
| Task 4.5.6 | `ScreenLayoutTest` 5 (5 form screens) | FX-toolkit | +5 | 0 |
| Task 4.5.7 | `JavaFxScalingHelperTest` 12 math + 3 FX-binding | Unit + FX-toolkit | +15 | 0 |
| Task 4.5.7b | `StagePersistenceValidatorTest` 9 + `PreferencesServiceTest` v2→v3 +1 | Unit | +10 | 0 |
| Task 4.5.8 | `BoardFrameThicknessHelperTest` 7 math + 3 FX-binding | Unit + FX-toolkit | +10 | 0 |
| Task 4.5.9 | `ResponsivenessParametricTest` 56 scenari `@Tag("slow")` | Visual + assertion | 0 | +56 slow |
| **REVIEW F-001/F-003/F-004/F-011** | `StagePersistenceCoordinatorTest` 8 + `UserPreferencesTest` 4 | Unit | +12 | 0 |
| **REVIEW F-002** | `StagePersistenceValidatorTest` +3 (50% intersection scenarios) | Unit | +3 | 0 |
| **REVIEW F-005** | `BoardFrameThicknessHelperTest` +1 (pre-layout fallback) | FX-toolkit | +1 | 0 |
| **REVIEW F-009** | `JavaFxScalingHelperTest` +8 compose parametric + `UiScalingServiceTest` +2 | Unit + FX-toolkit | +10 | 0 |
| **Totale F4.5** | — | — | **+84** | **+112** (56 pre + 56 post) |

Nessun test in stato `@Disabled` o `@Ignore` introdotto. Nessun test cancellato.

### 5.2 Regression finale (E5) — eseguita 2026-05-06 18:34 CET

`mvn clean verify` (root, **inclusi** `slow,performance`) sul branch `feature/4.5-ui-responsiveness` HEAD `30131e3`:

| Modulo | Tempo | Test | Esito |
|---|---:|---:|---|
| Parent (POM agg) | 6.460 s | — | ✅ SUCCESS |
| `shared` | **16:11 min** | 391 (387 default + 1 slow + 3 performance) | ✅ SUCCESS |
| `core-server` | 25.847 s | 166 (163 fast + 3 slow stress F4) | ✅ SUCCESS |
| `client` | 1:20 min | **461** (405 fast + 56 slow `ResponsivenessParametricTest`) | ✅ SUCCESS |
| `server` | 10.976 s | 1 smoke | ✅ SUCCESS |
| **Totale build** | **18:16 min** | **1019 test verdi** | ✅ **BUILD SUCCESS** |

Highlights:
- **Gating IA F2** `AiTournamentSimulationTest#campionWinsAtLeast95OutOf100AgainstPrincipiante` (`@Tag("slow")`) PASS — il tempo `shared` 16:11 min è dominato da questo test (~16 min wall-clock per 100 partite Campione vs Principiante; soglia ≥95/100 superata; **+/- vs F4 baseline 16:49 min nel range di varianza JIT warmup + load macchina**).
- **Corpus regole F1** `RuleEngineCorpusTest` parametrizzato PASS sulle 53 posizioni in `shared/src/test/resources/test-positions.json` (corpus invariato vs F2/F3/F3.5/F4).
- **Performance NFR-P-02** `AiPerformanceTest` (`@Tag("performance")`, 3 test) PASS — Campione produce mossa valida entro 5s × 1.5x tolerance.
- **Modulo `core-server`**: 166 invariato vs F4 (F4.5 non tocca `core-server`). JaCoCo gate ≥ 80% line+branch ✅.
- **Modulo `client` post-F4.5**: **461** test (321 F3.5 baseline → 405 fast + 56 slow ResponsivenessParametric, +140 vs F3.5 = +84 fast F4.5 + 56 slow F4.5 — vedi §5.1). JaCoCo gate ≥ 60% line+branch ✅; bundle delta 70 → 76 classi (+6 nuove client.layout/app classes).
- **`BaselineScreenshotCapture` (Task 4.5.2 audit pre-fix)** non incluso nel test count del modulo `client` — il classname non matcha il pattern surefire default (`**/*Test.java` / `**/Test*.java`); è una utility di audit on-demand, eseguibile via `mvn -pl client test -Dgroups=slow -Dtest=BaselineScreenshotCapture` quando serve rigenerare le 56 PNG pre-fix per A/B comparison. Decisione corretta — non dovrebbe consumare ~33 s ad ogni regression.
- **SAST SpotBugs**: 0 High su tutti i moduli.
- **Spotless**: 0 file da riformattare su tutti i moduli.

**A4.5.18 ✅ COVERED** (era DEFERRED in REVIEW): `mvn clean verify` (root, slow + performance inclusi) BUILD SUCCESS su tutti i moduli.

Nessuna regressione cross-module rispetto al baseline F4 — i moduli `shared`/`core-server`/`server` non sono stati toccati in F4.5 e si confermano invariati nei conteggi e tempi.

---

## 6. Manual procedures

Tre acceptance criteria F4.5 sono verificabili solo con manual procedure perché coinvolgono input hardware reale o percezione visiva:

### 6.1 Manual demo run cliente / VM (A4.5.16) — RICHIEDE INPUT UTENTE

**Procedura**:

1. Build dell'installer Windows MSI (richiede JDK 21 con `jpackage`):
   ```cmd
   mvn -pl client -Pinstaller -DskipTests package
   ```
   Output atteso: `client/target/dist/Dama Italiana-0.4.5.msi` (~140 MB con bundled JRE).
2. Trasferire l'MSI sul PC del cliente (o VM Windows 10/11 con stesse caratteristiche hardware: ~2014-2015 era, Intel HD 3000-4000, 4-8 GB RAM, HDD spinning, display 1366×768).
3. Doppio-click sull'MSI → install in `C:\Program Files\Dama Italiana`. Avviare via Start Menu shortcut.
4. **Test del defect originale**: il cliente conferma che la parte bassa della scacchiera è **completamente visibile** alla risoluzione 1366×768 (era il defect riportato pre-F4.5).
5. **Test resize**: il cliente trascina i bordi della finestra (drag right/bottom edge) e verifica che il layout si adatta fluido senza clipping; la scacchiera resta sempre quadrata e centrata.
6. **Test maximize**: clic sul pulsante massimizza, verifica che la scacchiera scala correttamente; ripristina, verifica che torna alla dimensione windowed precedente (REVIEW F-001 fix).
7. **Test persistenza**: chiudi l'app, riapri — finestra ripristinata at last known size/position/maximized state.
8. **Test multi-launch**: chiudi maximized, riapri (rimane maximized), de-maximizza (la finestra restora at the geometry pre-maximize, NON at the maximized geometry), chiudi de-maximizzata, riapri (windowed).

**Sign-off cliente**:

| Step | Esito | Note utente |
|---|---|---|
| 1-3 install + first run | ⏳ pending | — |
| 4 defect originale 1366×768 | ⏳ pending | — |
| 5 resize fluido | ⏳ pending | — |
| 6 maximize/restore | ⏳ pending | — |
| 7 persistenza launch successivo | ⏳ pending | — |
| 8 multi-maximize cycle | ⏳ pending | — |

**A4.5.16**: ⏳ DEFERRED (pending sign-off cliente).

### 6.2 DPI matrix verification (A4.5.11) — RICHIEDE INPUT UTENTE

JavaFX 21 prende il render scale da JVM startup args. Single-JVM run automatic non può esercitare DPI multipli. Il layout math F4.5 è DPI-independent quindi `ResponsivenessParametricTest` al 100% logical DPI valida la correttezza dell'algoritmo; le varianti DPI verificano l'integrazione con il rendering JavaFX 21 sotto Windows DPI scaling.

**Procedura**:

Per ogni combinazione DPI, lanciare manualmente:
```cmd
"C:\Program Files\Dama Italiana\Dama Italiana.exe" -J-Dprism.allowhidpi=true -J-Dglass.win.uiScale=N
```
sostituendo N con `1.0`, `1.25`, `1.5`, `2.0`. Oppure (più realistico): impostare DPI scaling dal pannello Windows Display Settings → Scale → 100% / 125% / 150% / 200% e ri-lanciare lo shortcut Start Menu.

| DPI scaling Windows | Risoluzione fisica | Logical viewport | Verifica visiva | Esito |
|---|---|---|---|---|
| 100% | 1366×768 | 1366×768 | Board completamente visibile, font UI a baseline 14 px | ⏳ pending |
| 125% | 1920×1080 | 1536×864 | Board scalato, font UI a 17.5 px (preview tools), no clipping | ⏳ pending |
| 150% | 1920×1080 | 1280×720 | Board scalato, font UI a 21 px, no clipping al floor logico | ⏳ pending |
| 200% | 3840×2160 | 1920×1080 | Board native sharp (pixel doubled), font UI a 28 px, no blur | ⏳ pending |

**A4.5.11**: ⏳ DEFERRED (pending verifica utente).

### 6.3 Resize fluido / FPS check (A4.5.12) — RICHIEDE INPUT UTENTE

Procedura osservazionale: durante drag del bordo della finestra in modo continuo per 5-10 secondi:

- Visivamente: nessuno stutter percepibile (FPS percepito ≥ 30).
- Animazioni in corso (es. mossa pezzo, particle puff, cattura): NON interrotte dal resize. Il resize cascading layout deve coesistere con `MoveAnimator`/`ParticleEffects` senza glitch.

**Test scenario**:
1. Avviare partita SP vs Principiante, attendere prima mossa.
2. Iniziare un drag del bordo destro della finestra mentre l'AI sta pensando.
3. Durante il drag, il particle puff di una cattura precedente deve continuare a fadeoutare; il layout deve riflettere il resize live.
4. Rilasciare il drag → il layout deve essere stabile, board centrata, side panel adattivo entro [240, 400].

**Manual review**: ⏳ pending.

**A4.5.12**: ⏳ DEFERRED (pending verifica utente).

### 6.4 Ultrawide aspect ratio screenshot review (A4.5.14c)

Task 4.5.6b skipped per Opzione B utente (REVIEW closure). Il post-fix screenshot a `3440×1440-ultrawide` è committato in `tests/visual-review/responsiveness-baseline-post-fix/board-view_3440x1440-ultrawide.png`:

- Board centrata orizzontalmente nella metà sinistra (post Task 4.5.4 fix).
- Side panel cramped a destra (1366 - 3440 = ampio espanso wood).
- Texture wood tiled naturalmente (post Task 4.5.8 fix REPEAT mode), no stretch.

**Verdict**: A4.5.14c ✅ ACCEPTABLE for F4.5 scope (no defect critico; cluster centering esplicito + wood margin handling esplicito sarà rivisitato in F11 polish se richiesto da feedback utente).

---

## 7. Closure check

- [x] Coverage target raggiunti: `mvn -pl client jacoco:report` post regression → bundle 76 classi, gate ≥ 60% line+branch verde (F4.5 aggiunge 6 classi al bundle, tutte coperte dai nuovi unit test).
- [x] TRACEABILITY aggiornata: NFR-U-05 + AC §17.2.8 nuovi righe + nuova sezione "Acceptance criteria di Fase 4.5" con 19 righe A4.5.1..A4.5.19 — vedi `tests/TRACEABILITY.md` (Task 4.5.10 + REVIEW closure).
- [x] Test corpus regole italiane invariato (F4.5 non tocca `RuleEngine` né `test-positions.json`).
- [x] `mvn clean verify` (root, **slow + performance inclusi**) BUILD SUCCESS — vedi §5.2 (1019 test verdi in 18:16 min).
- [x] Test plan §1-§7 popolato.
- [x] Nessun test in stato `@Disabled` o `@Ignore` senza issue tracciata (`grep "@Disabled\|@Ignore" client/src/test/java/` = 0 match in F4.5).
- [x] Findings REVIEW Fase 4.5 (15 totali) addressed: 13 RESOLVED nel fix loop, 2 DEFERRED con rationale (F-010 pre-existing F3 pattern, F-012 perf F8+ TODO referenced).
- [ ] Manual procedures §6.1 (demo cliente A4.5.16) + §6.2 (DPI matrix A4.5.11) + §6.3 (resize jank A4.5.12) eseguite e signed-off — pending input utente.

**Test plan chiuso il**: 2026-05-06 (regression cross-module), pending manual sign-off utente per A4.5.11/12/16.
**Commit di chiusura sotto-fase TEST**: `<TBD post-regression>`.

---

## 8. Conseguenze per Fase 5+

F4.5 è una **mini-fase di consolidamento** focalizzata sul lato client/UI. Le decisioni architetturali (ADR-043 strategia layout responsive con JavaFX puro) e i due helper introdotti in `client.layout` (`JavaFxScalingHelper`, `BoardFrameThicknessHelper`) rappresentano il **pattern di riferimento** per future schermate UI:

- Quando F6 introduce le schermate Internet match (lobby online, friend list, chat), F7 le LAN host/discovery/chat, F8 le tournament bracket views, F9 le classifiche/scheduler, F11 i replay/profilo: ogni nuova schermata deve riusare il design system di F3.5 (anti-pattern #15) **e** il layout responsive di F4.5 (token spacing pref/min/max, marker class `display-fluid`/`display-fluid-lg`, `BackgroundImage` REPEAT mode + frame proporzionali se applicabile).
- Lo schema v3 di `~/.dama-italiana/config.json` aggiunge i 5 campi window* nullable + `windowMaximized` boolean. Future fasi che aggiungono persistence client-side (es. F6 server credentials cache, F11 last replay viewed) seguiranno lo stesso pattern di schema versionato (v3 → v4 con migration trasparente).
- `StagePersistenceCoordinator` continuous-windowed-bounds-tracking pattern è riusabile per altre subordinate finestre (es. dialog modal di setup torneo F8) se diventeranno persistent.
- `ResponsivenessParametricTest` `@Tag("slow")` 56-scenario harness rimane in `client/src/test/java/.../layout/` come **regression baseline** per ogni fase futura: qualsiasi PR che faccia fallire E2 (parametric assertion) o regressione visiva sui 2 PNG committati è un finding `BUG` o `REQUIREMENT_GAP` alla review della fase corrispondente.
- `JavaFxScalingHelper.applyFluidFontsToScene(scene, uiScaleFactor)` continua ad essere chiamato da `SceneRouter.show` su ogni navigazione → ogni nuova schermata che aggiunge titoli display deve usare il marker class invece di `-fx-font-size` hardcoded.
- F-012 (walker performance) è marcato per revisita in F8 quando arrivano schermate con ~hundreds di nodes (live tournament bracket).
