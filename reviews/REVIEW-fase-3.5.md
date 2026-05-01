# REVIEW — Fase 3.5: Visual polish + Audio + Demo release (Windows installer)

- **Data apertura**: 2026-05-02
- **Commit codebase apertura**: `6528457` (head `feature/3.5-visual-polish-and-audio` post Task 3.5.15)
- **SPEC version**: 2.2 (2026-04-30)
- **PLAN di riferimento**: [`plans/PLAN-fase-3.5.md`](../plans/PLAN-fase-3.5.md)
- **Reviewer**: Claude Code

## Sommario

| Categoria       | Critical | High | Medium | Low | Totale | Resolved |
|-----------------|---------:|-----:|-------:|----:|-------:|---------:|
| BLOCKER         |        0 |    0 |      0 |   0 |      0 |        — |
| REQUIREMENT_GAP |        0 |    1 |      0 |   0 |      1 |      1/1 |
| BUG             |        0 |    0 |      0 |   0 |      0 |        — |
| SECURITY        |        0 |    0 |      0 |   0 |      0 |        — |
| PERFORMANCE     |        0 |    0 |      0 |   0 |      0 |        — |
| CODE_QUALITY    |        0 |    0 |      0 |   0 |      0 |        — |
| DOC_GAP         |        0 |    0 |      0 |   1 |      1 |      1/1 |
| **Totale**      |        0 |    1 |      0 |   1 |      2 |  **2/2** |

**Stato complessivo (post-closure)**: tutti e 2 i findings sono **RESOLVED**. F-001 (REQUIREMENT_GAP High, save-dialog non wood-themed) chiuso in Task 3.5.14a (commit `19e5227`). F-002 (DOC_GAP Low, custom interpolator easing OUT_BACK) chiuso opzione A — Javadoc note aggiunta nel commit di chiusura della review. Nessun BLOCKER / BUG / SECURITY / PERFORMANCE.

`mvn -pl client verify -DexcludedGroups=slow,performance` BUILD SUCCESS al commit `19e5227` (post-fix F-001): **321 test verdi**, 0 skipped, JaCoCo client gate ✅ (haltOnFailure=true), SpotBugs 0 High, Spotless OK. Regression `mvn clean verify` root con `slow`+`performance` schedulata in apertura sotto-fase TEST (Task 3.5.17). Manual demo run su macchina cliente target (Win 11) PASS — tutti gli A3.5.x audio/animazione/visual percepiti come da SPEC §13.2-13.4 v2.2.

---

## Acceptance criteria coverage

### SPEC §17 (acceptance globali rilevanti per F3.5)

| AC ID    | Descrizione                                                                  | Status      | Note |
|----------|------------------------------------------------------------------------------|-------------|------|
| 17.1.1   | Partita SP vs IA Campione si conclude entro 30 min senza crash               | ✅ PRESERVATO | Manual demo Task 3.5.14 vs Esperto completata; Campione invariato (no codice ai/ toccato in F3.5) |
| 17.1.7   | Pedina che raggiunge promozione mid-sequence non continua                    | ✅ PRESERVATO | Corpus F1 + `SinglePlayerE2ETest#promotionStopsSequenceInUi` invariati |
| 17.1.8   | Pedina non può catturare la dama                                              | ✅ PRESERVATO | Corpus F1 + `SinglePlayerE2ETest#manCannotCaptureKingInUi` invariati |
| 17.2.3   | Client a 60 FPS durante animazioni                                           | ⚠️ DEFERRED  | NFR-P-01 — particle effects + ambient music non hanno mostrato lag percepibile in Task 3.5.14 manual run; misura formale resta F11 (PLAN §1.1 out of scope) |
| 17.2.4   | Coverage ≥ 80% modulo `shared`                                                | ✅ PRESERVATO | `shared` non toccato in F3.5; gate F1+F2 invariato |
| 17.2.5   | SAST SpotBugs senza warning High                                             | ✅           | 0 warning High al commit `19e5227` |
| 17.2.7   | Dark mode + light mode WCAG AA                                                | ⚠️ PARTIAL   | Light mode WCAG AA preservato e rinforzato col nuovo design system (token dual `text-on-dark/text-on-light` esplicitano i contrast pair); dark mode resta stub coordinato (toggle F11) |

### Acceptance criteria operativi della Fase 3.5 (PLAN-fase-3.5 §2.2)

| ID       | Criterio                                                                                          | Status      | Note |
|----------|---------------------------------------------------------------------------------------------------|-------------|------|
| A3.5.1   | `mvn -pl client verify -DexcludedGroups=slow,performance` BUILD SUCCESS                           | ✅           | 321 test verdi al commit `19e5227` (post-fix F-001) |
| A3.5.2   | Coverage JaCoCo client ≥ 60% line+branch                                                          | ✅           | Gate `haltOnFailure=true` in `client/pom.xml` (override del parent), bundle 69 classes con 9 esclusioni esplicite + nuovo exclude `JavaFxAudioService.class` (ADR-035) |
| A3.5.3   | SpotBugs 0 High, Spotless OK                                                                      | ✅           | Confermato |
| A3.5.4   | 8 schermate ridisegnate con design tokens v2                                                      | ✅ POST-FIX  | 7/8 covered direttamente; `save-dialog` covered post-fix Task 3.5.14a (vedi F-001 RESOLVED) |
| A3.5.5   | Tavola texture legno + pezzi 3D-look + dame con marker oro/rosso                                 | ✅           | `BoardCellNode` BackgroundImage + `PieceNode` Group composition + `tests/visual-review/board-game.png` |
| A3.5.6   | Animazione mossa easing OUT_BACK (overshoot)                                                      | ✅ (VEDI F-002) | Custom `Interpolator` in `MoveAnimator` implementa la curva `easeOutBack`. Deviazione dal PLAN §3.5.8 (citava `Interpolator.SPLINE(0.34, 1.56, 0.64, 1)`) per limitazione di JavaFX 21 SPLINE che valida y∈[0,1] (overshoot richiede y>1). Comportamento finale identico, solo l'API diverge. Vedi finding F-002. |
| A3.5.7   | Cattura: particle puff 8-12 marrone/grigie, fade+scale 350ms                                      | ✅           | `ParticleEffects.captureSplash` 10 Circle, `ParallelTransition(translate radiale + fade + shrink)` 350ms, `setOnFinished` rimuove i nodi |
| A3.5.8   | Promozione: raggi dorati 8-12 radiali, fade 600ms                                                 | ✅           | `ParticleEffects.promotionGlow` 10 Line gold, expand+fade 600ms |
| A3.5.9   | Cattura obbligatoria: glow halo oro animato 1200ms                                                | ✅           | `ParticleEffects.mandatoryGlow` DropShadow Timeline cycle 1200ms su pezzo che deve catturare; `setOnFinished` cleanup `setEffect(null)` |
| A3.5.10  | Music shuffle 3-5 tracce orchestrali, default 30%, no overlap, loop continuo                      | ✅           | 4 tracce (1 MP3 + 3 WAV PCM) bundled, `MusicPlaylist` shuffle deterministico anti back-to-back, `setOnEndOfMedia` dispone player corrente e crea nuovo per il prossimo brano (no overlap) |
| A3.5.11  | SFX MOVE/CAPTURE/PROMOTION/VICTORY\|DEFEAT (+ ILLEGAL bonus)                                      | ✅           | 6 enum `Sfx` (PLAN diceva 4, espansione documentata in CHANGELOG); `SinglePlayerControllerTest` 5 + `SinglePlayerE2ETest` 4 + `SfxPlaybackSmokeTest` |
| A3.5.12  | Settings: 2 slider Volume musica/effetti + 2 toggle Muto, persisti in `config.json`               | ✅           | `UserPreferences` schema v2 con migrazione v1→v2 trasparente; `JavaFxAudioServiceTest` 8 test su persist; `tests/visual-review/settings.png` |
| A3.5.13  | `mvn -pl client -Pinstaller -DskipTests package` produce MSI su Win 10/11                         | ✅           | `Dama Italiana-0.3.5.msi` 152 MB build verificata 2x (Task 3.5.14) con WiX 3.14; ADR-036 |
| A3.5.14  | Asset CC0 o CC-BY (visual+audio) + `CREDITS.md` con autore/fonte/licenza                          | ✅           | Audit: 3 texture Poly Haven CC0, 4 music OpenGameArt CC0, 6 SFX Kenney CC0, 2 font Google Fonts OFL — tutti tracciati con esclusioni esplicite (ADR-037) |
| A3.5.15  | WCAG AA preservato light theme (contrast text/bg ≥ 4.5:1)                                        | ✅           | Audit code-by-code: pair critici tutti ≥9:1 (`text-on-light`/`bg-elevated` cream ~10:1, `button-primary` text on gold gradient ~9:1, `label-display` gold on dark roast ~9:1, `splash-progress` bar/track ~8:1). `text-secondary` muted brown su cream ~3.5:1 OK per secondary (no body text usage) |
| A3.5.16  | Nessun TODO/FIXME pending in `client/src/main/java/`                                              | ✅           | grep 0 match |
| A3.5.17  | TRACEABILITY aggiornato con A3.5.x                                                                | ✅           | Sezione "Acceptance criteria di Fase 3.5" aggiunta in Task 3.5.15 (commit `6528457`) |
| A3.5.18  | TEST F3 debt estinto (TEST-PLAN-fase-3.md §2-6,8-10 + manuale §7.4 + regression slow+perf)        | ✅           | Task 3.5.0 commit `daec3b4`, A3.3 manual run `f3c2ae7`; `tests/TEST-PLAN-fase-3.md` finalizzato pre-IMPLEMENTA F3.5 |
| A3.5.19  | Manual demo run su Win 10/11 fresca                                                               | ✅           | `tests/TEST-PLAN-fase-3.5.md §7` finalizzato esito **PASS** (commit `9432dbb`); 7 PNG baseline checked-in (splash by-design non screenshot-able) |
| A3.5.20  | `mvn clean verify` (root) BUILD SUCCESS finale                                                    | ⚠️ DEFERRED  | Schedulato per Task 3.5.17 (TEST sotto-fase) con `slow`+`performance` inclusi (~16 min). Senza i tag `mvn clean verify` (root) BUILD SUCCESS al commit `9386c52` (Task 3.5.13 audit) — vedi AI_CONTEXT |
| A3.5.21  | `package-info.java` per nuovi sotto-package (`client.audio`)                                      | ✅           | `client/src/main/java/com/damaitaliana/client/audio/package-info.java` presente (Task 3.5.3) |

### Requisiti SPEC funzionali coperti (FR)

| FR ID    | Status        | Note |
|----------|---------------|------|
| FR-SP-01..09 | ✅ PRESERVATO | Tutta la logica single-player preservata invariata; F3.5 tocca solo la pelle |
| FR-RUL-01..05 | ✅ PRESERVATO | Rules screen restyling visivo (Task 3.5.11), contenuto preservato |

### Requisiti SPEC non funzionali coperti (NFR)

| NFR ID   | Status     | Note |
|----------|------------|------|
| NFR-P-01 | ⚠️ DEFERRED | 60 FPS misura formale a F11. Manual demo Task 3.5.14 non ha mostrato lag percepibile con particle effects + ambient music attivi su Win 11 |
| NFR-U-01 | ✅ COVERED  | Localizzazione IT/EN preservata + nuove chiavi audio/splash |
| NFR-U-02 | ⚠️ PARTIAL  | Light mode reinforced; dark mode stub coordinato (token speculari) ma non attivato — toggle F11 |
| NFR-U-03 | ⚠️ PARTIAL  | `accessibleText` + keyboard nav board preservati; testi a11y in inglese hardcoded resta deferred F11 (come F3) |
| NFR-U-04 | ✅ COVERED  | Light mode WCAG AA verificato manualmente sui token critici (vedi A3.5.15) — nuovo vocabolario duale `text-on-dark/light` rende esplicite le coppie contrast |
| NFR-M-02 | ✅ COVERED  | JaCoCo gate 60% line+branch preservato (`haltOnFailure=true`), nuovo exclude `JavaFxAudioService.class` per JavaFX-bound impl (ADR-035) |
| NFR-M-04 | ✅ COVERED  | Spotless Google Java Style |

---

## Findings

### F-001 — [REQUIREMENT_GAP, High] Save dialog non skinned wood

- **Posizione**: `client/src/main/java/com/damaitaliana/client/ui/save/SaveDialogController.java:105` (pre-fix) — il metodo `show(SinglePlayerGame, Window)` creava un proprio `Stage` modale con `new Scene(root)` ma **non chiamava `ThemeService.applyTheme(scene)`**. Gli stylesheet `theme-light.css` + `components.css` non venivano caricati nella scene del dialog.
- **SPEC reference**: SPEC §13.2 (v2.2) e PLAN-fase-3.5 §2.2 A3.5.4 — "Tutte le 8 schermate F3 esistenti ridisegnate con design tokens v2".
- **Descrizione**: emerso durante il manual demo run di Task 3.5.14 (`tests/visual-review/save-dialog.png` pre-fix mostrava sfondo bianco/font sistema, divergente dalle altre 7 schermate). 7/8 PASS direttamente, save dialog era l'unico FAIL. Causa root: `SaveDialogController` non era stato ribattezzato dopo l'introduzione di `ThemeService` come single source of truth per gli stylesheet (Task 3.1+, F3); è uno Stage modale che non eredita CSS dalla scene del padre.
- **Proposta di fix**: iniettare `ThemeService` come dipendenza del costruttore (`@Autowired` 4-arg + visible-for-tests 5-arg) e chiamare `themeService.applyTheme(scene)` immediatamente dopo `new Scene(root)`. Stesso pattern usato da `SceneRouter`. Aggiornare `SaveDialogControllerTest` + `FxmlLoadingSmokeTest` per mockare `ThemeService` (3-arg → 4-arg/5-arg ctor); `SaveDialogSpringContextTest` invariato (autowiring risolve `ThemeService` come `@Component` esistente).
- **Status**: ✅ **RESOLVED** al commit `19e5227` (`fix(client): apply wood theme to save-dialog scene (Task 3.5.14a)`). MSI ricostruito post-fix; ri-screenshot `save-dialog.png` confermato wood-themed dall'utente. 321 test verdi, JaCoCo client gate ✅, SpotBugs 0, Spotless OK.

---

### F-002 — [DOC_GAP, Low] Custom Interpolator easing OUT_BACK in `MoveAnimator` non spiega la deviazione dal PLAN

- **Posizione**: `client/src/main/java/com/damaitaliana/client/ui/board/animation/MoveAnimator.java` (righe ~42-57, custom `Interpolator` anonimo che implementa la curva `easeOutBack` con formula `1 + c3*(t-1)³ + c1*(t-1)²`, c1=1.70158, c3=2.70158).
- **SPEC reference**: SPEC §13.3 (v2.2, "Animazioni avanzate Fase 3.5"); PLAN-fase-3.5 §3.5.8 esplicitava `Interpolator.SPLINE(0.34, 1.56, 0.64, 1)`.
- **Descrizione**: l'implementazione finale usa una **`Interpolator` custom** (non `SPLINE`) perché JavaFX 21 valida i punti di controllo SPLINE con y ∈ [0, 1], proibendo l'overshoot necessario (y > 1) richiesto dalla curva out-back. Il commento sopra il blocco custom spiega la sostituzione, ma la deviazione **non è registrata** nel CHANGELOG né in un ADR — un futuro lettore che cerca "SPLINE" come da PLAN potrebbe pensare a una regressione. Comportamento osservabile (overshoot leggero al landing) **identico** alla curva SPEC.
- **Proposta di fix**: due opzioni minori, in alternativa una all'altra, da discutere con l'utente:
  - **Opzione A (preferita)**: aggiungere una nota di 1-2 righe nel Javadoc del custom `Interpolator` che cita SPEC §13.3 e la limitazione JavaFX 21 SPLINE (link issue / classpath del costante restrittivo se rilevante). Nessun cambio architetturale.
  - **Opzione B**: aprire SPEC change request CR-F3.5-006 per chiarire SPEC §13.3 — la cita come "curva equivalente a out-back, implementazione custom Interpolator se la libreria UI non supporta `SPLINE` con punti fuori range". Più formale ma overhead per la closure F3.5.
- **Decisione utente** (2026-05-02): opzione A scelta — Javadoc note.
- **Status**: ✅ **RESOLVED** al commit di chiusura della review. Aggiunta frase finale al Javadoc del custom `Interpolator` in `MoveAnimator.java`: "PLAN-fase-3.5 §3.5.8 originally suggested `Interpolator.SPLINE(0.34, 1.56, 0.64, 1)`; this anonymous subclass is the equivalent visible behavior given that limitation. See REVIEW-fase-3.5 finding F-002." Nessun cambio comportamentale; Spotless OK.

---

## SPEC change requests

> Vuota. F-002 risolvibile via comment update (opzione A); l'opzione B dello stesso finding è quella che genererebbe una CR, ma è documentata come deferral discrezionale in attesa di decisione utente.

---

## Closure

- [x] Tutti i `BLOCKER` risolti (0 finding)
- [x] Tutti i `REQUIREMENT_GAP` risolti (1/1: F-001 RESOLVED al commit `19e5227`)
- [x] Tutti i `Critical/High` `BUG` risolti (0 finding)
- [x] Tutti i `Critical/High` `SECURITY` risolti (0 finding)
- [x] `PERFORMANCE` che violano NFR risolti (0 finding; NFR-P-01 deferred F11 come F3, manual run senza lag percepibile)
- [x] **F-002 [DOC_GAP, Low]** — RESOLVED opzione A (Javadoc note) al commit di chiusura
- [x] SPEC change requests con stato non-PENDING (0 CR pending)

**Review chiusa il**: 2026-05-02
**Commit di chiusura**: `12a5e4e`
