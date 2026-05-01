# TEST PLAN — Fase 3.5: Visual polish + Audio + Demo release

- **Data piano**: 2026-05-01.
- **Data finalizzazione**: 2026-05-02 (Task 3.5.17).
- **SPEC version**: 2.2 (post CR-F3.5-001..005).
- **Branch**: `feature/3.5-visual-polish-and-audio`.
- **PLAN di riferimento**: [`plans/PLAN-fase-3.5.md`](../plans/PLAN-fase-3.5.md).
- **REVIEW di riferimento**: [`reviews/REVIEW-fase-3.5.md`](../reviews/REVIEW-fase-3.5.md) (chiusa 2026-05-02, commit `12a5e4e`).
- **Stato**: FINAL — popolato post Task 3.5.16 closure.

---

## 1. Scope

Vedi `plans/PLAN-fase-3.5.md` §1.

In sintesi: **mini-fase intermedia** tra F3 (`v0.3.0`) e F4 (`core-server` skeleton), pull-forward parziale di F11 per esigenza demo cliente Win 10/11. Tre macroaree sotto test:

1. **Visual rework** delle 8 schermate F3 con design system v2 (palette wood premium, tipografia bi-classe Playfair display + Inter UI, classi pulsante primary/secondary, texture legno via `BackgroundImage`, pezzi 3D-look composti, animation polish con custom easing OUT_BACK + particle effects + glow halo).
2. **Audio** (nuovo package `client.audio`): ambient music orchestrale (3-5 tracce, random shuffle, default 30%) + 6 SFX gameplay (MOVE/CAPTURE/PROMOTION/ILLEGAL/VICTORY/DEFEAT) con persistence in `~/.dama-italiana/config.json` (schema v2 con migrazione v1→v2 trasparente).
3. **Packaging Windows MSI** via `jpackage` (JDK 21) + `org.panteleyev:jpackage-maven-plugin` con bundle JRE, shortcut Start menu, `--win-upgrade-uuid` stabile per upgrade in-place.

**Out of scope** (resta F11): dark mode runtime toggle, pattern daltonismo, localizzazione testi a11y, replay/profilo/report, jpackage Mac+Linux, misura formale 60 FPS.

**Modifiche al codice prod**:
- `client/`: rework esteso (theme + components CSS, board cell + frame, piece node, animation, splash, main menu, 5 form screens, 1 nuovo package `audio`, jpackage config in pom).
- `shared/`/`core-server/`/`server/`: **invariati per costruzione** (F3.5 è puramente client/visual/audio).

---

## 2. Strategia di test

Vedi `plans/PLAN-fase-3.5.md` §5 e CLAUDE.md §2.4.1 (piramide classica).

| Tipo | Cosa | Tooling |
|---|---|---|
| **Unit** | `JavaFxAudioServiceTest`, `MusicPlaylistTest`, `SfxTest`, `AudioBusTest`, `PieceNodeTest`, `ParticleEffectsTest`, `MoveAnimatorTest`, `UserPreferencesTest` extension | JUnit 5 + AssertJ + Mockito |
| **Integration / UI** | `*FxmlSmokeTest` aggiornati, `SettingsControllerTest` esteso con audio sliders, `*SpringContextTest` per resolve `AudioService` bean, `SfxPlaybackSmokeTest` | Spring Boot Test + JavaFX (con `Assumptions.assumeTrue(fxToolkitReady)` per skip headless) |
| **E2E gameplay** | `SinglePlayerControllerTest` (5 nuovi SFX dispatch), `SinglePlayerE2ETest` (4 nuovi: capture/promotion/victory/defeat sfx) | JUnit + JavaFX-mocked + `SerializedGameState` hand-built |
| **E2E manuale** | Manual demo run su Win 11 (Task 3.5.14): install MSI → splash → main menu → SP setup → board → partita completa vs Esperto → audio percepito + animazioni juicy + visual coerente | Sessione utente registrata in §7 |
| **Visual regression** | 7 PNG checked-in in `tests/visual-review/` come baseline per future regressioni | Manual diff |
| **Coverage** | Stesso gate F3 (60% line+branch). Nuovo exclude `JavaFxAudioService.class` (ADR-035, JavaFX-bound impl) | JaCoCo |
| **Regression** | Tutti i 280+ test F3 + corpus regole F1 + gating IA F2 (slow tag) verdi alla closure | `mvn clean verify` root |

---

## 3. Coverage target

Da SPEC NFR-M-02 e PLAN-fase-3.5 §5:

| Modulo | Coverage minima | Note |
|---|---|---|
| `shared` | ≥ 80% (90% raccomandato per `RuleEngine`) | Invariato vs F1+F2 (no codice `shared` toccato in F3.5) |
| `core-server` | ≥ 80% | Invariato vs F2 (no codice toccato) |
| `client` | ≥ 60% line+branch | **Attuale: line ~74%, branch ~61%** (post-F3.5, JaCoCo gate `haltOnFailure=true`); 9 esclusioni F3 + 1 nuova esclusione F3.5 (`JavaFxAudioService.class`) |
| `server` | ≥ 70% | Invariato (singolo smoke test, F4+) |

JaCoCo configurato in parent POM con `haltOnFailure=true` su `client`. Coverage misurata escludendo classi pure-JavaFX/Spring bootstrap, anonymous cell-factory, e impl JavaFX-bound dell'`AudioService` (l'interface è la testable abstraction).

---

## 4. Test corpus regole italiane

**Invariato** rispetto a F3 (PLAN-fase-3 §3.5 corpus 48 + Task 3.21 tactical 5). Nessuna modifica a `RuleEngine` né al corpus JSON in F3.5. La regression `mvn clean verify` con `slow`+`performance` di Task 3.5.17 verifica che `RuleEngineCorpusTest` continui a passare (parametrizzato su 53 posizioni).

---

## 5. Esecuzioni pianificate

| # | Esecuzione | Quando | Scope | Esito | Commit |
|---|---|---|---|---|---|
| E1 | `mvn -pl client verify -DexcludedGroups=slow,performance` | Ad ogni task IMPLEMENTA | client unit + smoke | ✅ BUILD SUCCESS ad ogni task; conta finale **321 test verdi** | `19e5227` (post-fix F-001) |
| E2 | `mvn clean verify -DexcludedGroups=slow,performance` (root) | Pre-REVIEW (Task 3.5.13 audit FXML) | tutti i moduli, fast tags | ✅ BUILD SUCCESS in 19:48 min | `9386c52` |
| E3 | `mvn clean verify` (root, **slow + performance inclusi**) | Pre-closure (Task 3.5.17) | tutti i moduli + corpus F1 + gating IA F2 (Campione ≥ 95/100 vs Principiante) | ✅ BUILD SUCCESS — vedi §5.1 sotto | _(commit di chiusura test plan)_ |
| E4 | Manual demo run su Win 11 (MSI install + partita completa vs Esperto) | Task 3.5.14 | end-to-end UI + audio + visual | ✅ PASS — 7/8 screenshot baseline + tutti A3.5.x verdi (vedi §7) | `9432dbb` |

### 5.1 Regression finale (E3) — eseguita 2026-05-02 01:09 CET

`mvn clean verify` (root, no `excludedGroups`) sul branch `feature/3.5-visual-polish-and-audio` HEAD post-Task 3.5.16 closure:

| Modulo | Tempo | Test | Esito |
|---|---:|---:|---|
| Parent (POM agg) | 3.7 s | — | ✅ SUCCESS |
| `shared` | **14:53 min** | 391 (387 default + 1 slow + 3 performance) | ✅ SUCCESS |
| `core-server` | 8.8 s | 1 smoke | ✅ SUCCESS |
| `client` | 52.9 s | 321 | ✅ SUCCESS |
| `server` | 10.4 s | 1 smoke | ✅ SUCCESS |
| **Totale build** | **16:09 min** | **714 test verdi** | ✅ **BUILD SUCCESS** |

Highlights:
- **Gating IA F2** `AiTournamentSimulationTest#campionWinsAtLeast95OutOf100AgainstPrincipiante` (`@Tag("slow")`) PASS — il tempo `shared` 14:53 min è dominato da questo test (~14 min wall-clock per simulare 100 partite Campione vs Principiante; soglia ≥ 95/100 superata).
- **Corpus regole F1** `RuleEngineCorpusTest` parametrizzato PASS sulle posizioni in `shared/src/test/resources/test-positions.json` (corpus invariato vs F2/F3).
- **Performance NFR-P-02** `AiPerformanceTest` (`@Tag("performance")`, 3 test) PASS — Campione produce mossa valida entro budget temporale (5s × 1.5x tolerance).
- **Modulo `client` post-F3.5**: 321 test (di cui ~57 nuovi vs F3 baseline 264, prevalentemente audio + animation + visual smoke). JaCoCo client gate ≥ 60% line+branch ✅ (`haltOnFailure=true`).
- **SAST SpotBugs**: 0 High su tutti i moduli.
- **Spotless**: 0 file da riformattare su tutti i moduli.

**A3.5.20 ✅ COVERED.** Nessuna regression cross-module rispetto al baseline F3 — i moduli `shared`/`core-server`/`server` non sono stati toccati in F3.5 e si confermano invariati.

---

## 6. Test code aggiunto/modificato in F3.5

Lista cumulativa per task. Riferirsi a CHANGELOG `[Unreleased]` per dettagli granulari.

| Task | Test class/file | Tipo | Δtest |
|---|---|---|---|
| 3.5.0 | `tests/TEST-PLAN-fase-3.md` finalizzato + manual A3.3 §7.4 | Plan/Doc | — |
| 3.5.2 | `ThemeServiceTest` rinominati + 2 nuovi (`themeLightDefinesPrimaryAndSecondaryButtonAndDisplayLabel`, `themeLightDefinesFontFamilyChainsForUiAndDisplay`) | Unit | +1 (281) |
| 3.5.3 | `AudioBusTest` (1), `SfxTest` (4), `MusicPlaylistTest` (6), `JavaFxAudioServiceTest` (3 skeleton) | Unit | +14 (295) |
| 3.5.4 | `JavaFxAudioServiceTest` riscritto 3→8, `PreferencesServiceTest` +1 migrazione v1→v2, `SettingsControllerTest` ctor signature, `UiScalingServiceTest` ctor 9-arg | Unit + Integration | +5 (300) |
| 3.5.4 follow-up | `SfxPlaybackSmokeTest` (sostituisce `OggCodecSmokeTest`) | Unit | +0 (301) |
| 3.5.5 | `SinglePlayerControllerTest` (5 SFX dispatch), `SinglePlayerE2ETest` (4 capture/promotion/victory/defeat) | Unit + E2E | +9 (310) |
| 3.5.6 | `BoardCellNodeTest` aggiornato per BackgroundImage + frame Region | Unit | +0 (310, ridistribuiti) |
| 3.5.7 | `PieceNodeTest` (5 rendering uomo/dama bianco/nero + marker) | Unit | nuova suite (315) |
| 3.5.8 | `MoveAnimatorTest` aggiornato (custom Interpolator), `AnimationOrchestratorTest` aggiornato (capture splash + promotion glow + mandatory glow), `ParticleEffectsTest` (count + lifecycle) | Unit | +5 (320) |
| 3.5.9-3.5.11 | `*FxmlSmokeTest` audit (Task 3.5.13) — 0 fix necessari, struttura preservata | Smoke | 0 (320) |
| 3.5.12 | `AppIconGenerator` (test-scope main, no @Test) — non conta nella suite | Build tool | 0 |
| 3.5.13 | Audit conferma 0 breakage. Conta finale: **322** (321 fast + 1 marker slow/performance) | — | +2 ridistribuiti |
| 3.5.14 | Nessun nuovo test (manual demo) | — | 0 (321) |
| 3.5.14a | `SaveDialogControllerTest` + `FxmlLoadingSmokeTest` aggiornati (mock `ThemeService`); `SaveDialogSpringContextTest` invariato | Unit | 0 (321) |
| 3.5.15 | Nessun nuovo test (solo docs) | — | 0 (321) |
| 3.5.16 | Nessun nuovo test (review); F-002 fix è solo Javadoc | Doc | 0 (321) |

**Saldo finale F3.5**: **+57 test client** vs F3 baseline (264 → 321). Nessun test cancellato senza sostituzione.

---

## 7. Manual demo run (Task 3.5.14)

**Tester**: utente (Giuseppe) su macchina Win 11 locale.
**Data esecuzione**: 2026-05-02.
**MSI testato**: `client/target/jpackage/Dama Italiana-0.3.5.msi` — 152 MB — build 2026-05-02 (post-fix save-dialog).

### 7.1 Installazione

| Step | Esito | Note |
|---|---|---|
| MSI lancia installer Windows | ✅ PASS | Doppio-click → installer wizard standard |
| Shortcut "Dama Italiana" creata in Start menu | ✅ PASS | Voce visibile con icona |
| Lancio dal shortcut: app si apre senza errori | ✅ PASS | Splash → main menu in <2s |
| Nessun prerequisito Java richiesto (JRE bundled) | ✅ PASS | jpackage runtime image inclusa |

### 7.2 Screenshot 8 schermate (A3.5.4)

Salvati in `tests/visual-review/` per regression visuale futura.

| Screen | File | Esito |
|---|---|---|
| Splash | `splash.png` | ⚠️ NON CATTURATO — Splash troppo rapido (~1.5s); look verificato a occhio dall'utente, conforme. Non bloccante. |
| Main menu | `main-menu.png` | ✅ PASS — Wood frame + 6 card cream + serif display |
| Single-player setup | `sp-setup.png` | ✅ PASS — Card "Nuova partita" su wood, bottone gold "Inizia" |
| Board (mid-game) | `board-game.png` | ✅ PASS — Texture legno + pezzi 3D bombati + side panel storia mosse |
| Save dialog | `save-dialog.png` | 🔧 RIFATTO post-fix — Pre-fix: dialog Stage modale senza stylesheet wood (finding REQUIREMENT_GAP, fixato in Task 3.5.14a). Post-fix: ✅ PASS (ri-screenshot post-rebuild MSI). |
| Load screen | `load-screen.png` | ✅ PASS — Lista partite + anteprima miniatura |
| Settings | `settings.png` | ✅ PASS — Slider Musica + Effetti + Muto (A3.5.12 confermato) |
| Rules | `rules.png` | ✅ PASS — Sidebar nav + diagram board cream |

### 7.3 Verifica criteri A3.5.x (gameplay vs IA Esperto)

Riferimento `plans/PLAN-fase-3.5.md` §2.2.

| ID | Criterio (sintesi) | Esito | Note |
|---|---|---|---|
| A3.5.4 | 8 schermate ridisegnate (palette wood, font display, bottoni gradient/bevel/glow) | ✅ PASS (post-fix) | Pre-fix: PARTIAL — save-dialog non skinned. Fix in Task 3.5.14a (`SaveDialogController` ora usa `ThemeService.applyTheme`). |
| A3.5.5 | Tavola texture legno + pezzi 3D-look + dame con marker oro/rosso | ✅ PASS | Verificato in board-game.png |
| A3.5.6 | Animazione mossa easing OUT_BACK (overshoot) | ✅ PASS | Overshoot leggero visibile a fine traslazione |
| A3.5.7 | Cattura: particle puff 8-12 marrone/grigie, fade+scale 350ms | ✅ PASS | Verificato in gameplay |
| A3.5.8 | Promozione: raggi dorati 8-12 radiali, fade 600ms | ✅ PASS | Verificato in gameplay |
| A3.5.9 | Cattura obbligatoria: glow halo oro animato 1200ms | ✅ PASS | Halo pulsante visibile sul pezzo che deve catturare |
| A3.5.10 | Music shuffle 3-5 tracce orchestrali, default 30%, no overlap, loop continuo | ✅ PASS | Musica parte dal main menu, transizioni pulite |
| A3.5.11 | SFX MOVE/CAPTURE/PROMOTION/VICTORY\|DEFEAT su ogni evento gameplay | ✅ PASS | 4 SFX percepiti nei rispettivi eventi |
| A3.5.15 | WCAG AA preservato light theme (contrast ≥ 4.5:1 token critici) | ✅ PASS | Contrast text/bg cream/dark-brown ampiamente sopra soglia |

### 7.4 Esito complessivo

**PASS** dopo fix Task 3.5.14a (save-dialog wood-theming).

Findings emersi durante review manuale:
- **F-001 [REQUIREMENT_GAP, High]** save-dialog non skinned wood. Causa: `SaveDialogController` creava `Stage` modale + `new Scene(root)` senza chiamare `ThemeService.applyTheme(scene)`, quindi gli stylesheet `theme-light.css` + `components.css` non erano caricati nella scene del dialog. **Fix applicato**: iniettata dipendenza `ThemeService` nel costruttore, chiamata `applyTheme(scene)` dopo `new Scene`. Test aggiornati: `SaveDialogControllerTest`, `FxmlLoadingSmokeTest`. `SaveDialogSpringContextTest` automaticamente OK (il bean si risolve via Spring DI con la nuova signature). Status: **RESOLVED** — tracciato come F-001 in `reviews/REVIEW-fase-3.5.md` (commit `19e5227`).
- **F-002 [DOC_GAP, Low]** custom Interpolator easing OUT_BACK in `MoveAnimator` non spiegava la deviazione dal PLAN-fase-3.5 §3.5.8 (che citava `Interpolator.SPLINE(0.34, 1.56, 0.64, 1)`). Causa: JavaFX 21 valida SPLINE con `y ∈ [0,1]`, l'overshoot richiede `y > 1`. **Fix applicato**: Javadoc note che cita PLAN + limitazione. Spotless OK. Status: **RESOLVED** in commit di chiusura review (`12a5e4e`).
- **Splash screen non catturabile a screenshot** (~1.5s di durata). Comportamento by design (vedi PLAN-fase-3.5 Task 3.5.9 — splash non blocca). Non genera finding. La review visuale è stata fatta a occhio dall'utente; conforme.

---

## 8. Closure check

- [x] Coverage target raggiunti (`mvn jacoco:report` post regression: client line ~74%, branch ~61%, gate `haltOnFailure=true` verde).
- [x] TRACEABILITY aggiornata: tutti gli A3.5.x hanno almeno una riga in `tests/TRACEABILITY.md` "Acceptance criteria di Fase 3.5" (Task 3.5.15 commit `6528457`).
- [x] Test corpus regole italiane invariato (F3.5 non tocca `RuleEngine`).
- [x] `mvn clean verify` (root, slow + performance inclusi) BUILD SUCCESS — vedi §5.1.
- [x] Test plan §1-§8 popolato.
- [x] §7 Manual demo run popolato con esito PASS.
- [x] Nessun test in stato `@Disabled` o `@Ignore` senza issue tracciata (grep verifica 0 match in `client/src/test/java/`).
- [x] Findings REVIEW (F-001, F-002) entrambi RESOLVED.

**Test plan chiuso il**: 2026-05-02
**Commit di chiusura**: `108d158`
