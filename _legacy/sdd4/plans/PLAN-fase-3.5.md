# PLAN — Fase 3.5: Visual polish + Audio + Demo release (Windows installer)

- **Riferimento roadmap**: pull-forward parziale di SPEC §16 Fase 11 (Polish & rilascio) per esigenza di **demo cliente**. Mini-fase intermedia tra F3 (`v0.3.0`) e F4 (`core-server` skeleton, prossima fase pianificata).
- **SPEC version**: 2.1 (post-clarifiche F3) — questo PLAN propone una **SPEC change request** esplicita (vedi §11) che andrà approvata allo stop point §10.
- **Data piano**: 2026-04-30.
- **Autore**: Claude Code.
- **Stato**: DRAFT — in attesa di approvazione utente.
- **Branch**: `feature/3.5-visual-polish-and-audio` (staccato da `develop` post-tag `v0.3.0`).
- **Tag finale previsto**: `v0.3.5` (mini-fase intermedia, F4 resta F4).

---

## 1. Scopo della fase

Trasformare l'estetica del client da "moderna ma gestionale" a **"videogame premium / wood classico"** ispirata al riferimento Play Store del cliente, integrare **musica ambient orchestrale** + SFX di gameplay, e produrre un **installer Windows `.msi`** consegnabile al cliente come demo del single-player con IA.

L'obiettivo concreto è arrivare a uno stato in cui:

> Il cliente lancia l'installer su Win 10 o Win 11, gioca una partita completa contro l'IA al livello che preferisce, percepisce un look "videogame" coerente in tutte le 8 schermate (splash, main menu, sp setup, board, save dialog, load screen, settings, rules), sente una musica orchestrale soft di sottofondo + SFX di mossa/cattura/promozione/vittoria, e dichiara "questo non è un gestionale".

### 1.1 Out of scope (resta in F11)

- **Dark mode toggle runtime** (resta deferred a F11 come da SPEC §16).
- **Pattern daltonismo sui pezzi** (resta F11).
- **Localizzazione IT/EN dei testi `accessibleText`** (resta F11).
- **Replay viewer / profilo / sistema report / monitoring server / docker image / OpenAPI doc utente completa** (multiplayer scope, F11).
- **Misura formale 60 FPS con tooling** (manual visual check OK in F3.5; tooling formale resta F11).
- **Tornei + multiplayer** (F4-F10).
- **Installer `.dmg` Mac e `.deb` Linux** (cliente è Win 10/11; gli altri OS in F11).

---

## 2. Acceptance criteria

### 2.1 Acceptance globale F3.5

> Il cliente apre l'installer Windows `.msi` su Win 10 o Win 11, completa l'installazione senza prerequisiti manuali (Java bundled), lancia il gioco, naviga splash → main menu → setup → board, gioca una partita completa vs IA al livello scelto fino a stato terminale, salva, ricarica, sente musica ambient orchestrale di sottofondo (default 30% mutabile da settings) + SFX su ogni evento di gameplay (mossa/cattura/promozione/vittoria), e percepisce uno stile "videogame premium wood" coerente in tutte e 8 le schermate.

### 2.2 Criteri operativi (A3.5.x)

| ID | Criterio | Verifica |
|---|---|---|
| **A3.5.1** | `mvn -pl client verify -DexcludedGroups=slow,performance` BUILD SUCCESS | output Maven |
| **A3.5.2** | Coverage JaCoCo client ≥ 60% line+branch (gate `haltOnFailure=true` invariato) | `target/site/jacoco/jacoco.csv` |
| **A3.5.3** | SpotBugs 0 High, Spotless OK | output Maven |
| **A3.5.4** | Tutte le 8 schermate F3 esistenti ridisegnate con design tokens v2 (palette wood, tipografia serif+sans, bottoni rounded con gradient/bevel, glow su hover) | screenshot review manuale + `tests/visual-review/<scene>.png` salvati nel repo per future regressioni |
| **A3.5.5** | Tavola e pezzi ridisegnati: cell con texture legno (chiaro/scuro alternati, venature visibili), pedine con effetto disco scanalato + gradient 3D-look + ombra, dame con marker decorativo (oro su bianco / rosso su nero) | screenshot review |
| **A3.5.6** | Animazione mossa con easing **OUT_BACK** (overshoot leggero) sostituisce `OUT_QUAD` | inspection codice + visual |
| **A3.5.7** | Cattura: aggiunge **particle puff** (8-12 particle marrone/grigie con fade+scale-out 350ms) sopra il pezzo catturato | visual + test orchestrator |
| **A3.5.8** | Promozione: aggiunge **raggi dorati** (8-12 raggi `#FFD700`/`#FFA500` che esplodono radialmente dal pezzo, fade-out 600ms) sovrapposti al flash dorato esistente | visual + test |
| **A3.5.9** | Cattura obbligatoria: il `pulse-mandatory` `FillTransition` 800ms è sostituito/affiancato da **glow halo esterno** (Drop shadow oro espandente cycle 1200ms) | visual + test |
| **A3.5.10** | Audio ambient: 3-5 tracce orchestrali soft caricate dalle risorse, playlist random shuffle, default 30%, mutabile via Settings; nessun overlap tra tracce; loop continuo a fine playlist | manual playback + `AudioServiceTest` |
| **A3.5.11** | SFX su 4 eventi: mossa (click discreto), cattura (tonfo legno), promozione (chime), vittoria (fanfara breve). Mutabili separatamente da Settings | manual + unit test |
| **A3.5.12** | Settings screen mostra 2 nuovi controlli: **Volume musica** (slider 0-100%) e **Volume effetti** (slider 0-100%); persisti in `~/.dama-italiana/config.json` (campi nuovi `musicVolumePercent`, `sfxVolumePercent`, default 30/70) | UI + persistence test |
| **A3.5.13** | `mvn -pl client jpackage:jpackage` produce `target/jpackage/dama-italiana-0.3.5.msi` su Win 10/11. L'installer crea una shortcut "Dama Italiana" in Start menu, installa JRE bundled (no prerequisito Java sul target), il gioco si lancia da shortcut senza errori | install + run su VM/box reale |
| **A3.5.14** | Asset (texture legno, music tracks, SFX) **tutti CC0 o CC-BY**; lista licenze in nuovo file `client/src/main/resources/assets/CREDITS.md` con autore, fonte, licenza per ognuno | grep + audit manuale |
| **A3.5.15** | WCAG AA preservato per il light theme: contrast text/background ≥ 4.5:1 sui token critici (status, button labels, menu items) | manual check con tool tipo WebAIM/Coolors |
| **A3.5.16** | Nessun TODO/FIXME pending in `client/src/main/java/` | grep |
| **A3.5.17** | TRACEABILITY aggiornato: nuove righe per A3.5.x + righe esistenti F3 ri-puntate ai nuovi nomi metodo se cambiano | inspection |
| **A3.5.18** | **Pre-existing TEST F3 debt** estinto: `tests/TEST-PLAN-fase-3.md` sezioni 2-6,8-10 popolate, regression `mvn clean verify` con `slow`+`performance` BUILD SUCCESS, procedura manuale A3.3 (TEST-PLAN §7) eseguita e §7.4 popolato | inspection |
| **A3.5.19** | Manual demo run: installa il `.msi` su una macchina Win 10/11 fresca (VM o real), gioca partita completa, verifica tutti gli A3.5.x visibili al cliente, popola log in `tests/TEST-PLAN-fase-3.5.md` | log riempito |
| **A3.5.20** | `mvn clean verify` (root) BUILD SUCCESS finale — tutti i moduli verdi | output Maven |
| **A3.5.21** | `package-info.java` per nuovi sotto-package (`client.audio`) | inspection |

---

## 3. Requisiti SPEC coperti / aggiornati

### 3.1 FR coperti

| FR | Descrizione | Stato in F3 | Stato F3.5 |
|---|---|---|---|
| FR-SP-01..09 | Tutto il single-player | ✅ Coperto | ✅ Preservato (non si tocca la logica, solo la pelle) |
| FR-RUL-01..05 | Rules screen | ✅ Coperto | ✅ Reskin senza cambiare contenuto |

### 3.2 NFR coperti / pull-forward

| NFR | Descrizione SPEC | F3 status | F3.5 obiettivo |
|---|---|---|---|
| NFR-P-01 | Client a 60 FPS durante animazioni | ⚠️ Deferred F11 | Verifica visual manuale 60 FPS con particle + ambient (no formal tooling) |
| NFR-U-01 | Localizzazione IT/EN | ✅ Coperto | ✅ Nuove chiavi audio settings IT+EN |
| NFR-U-02 | Dark + light mode WCAG AA | ⚠️ Light only | ⚠️ Light only (dark resta F11) — light si **rinforza** col nuovo design system |
| NFR-U-03 | Accessibilità tastiera | ✅ Coperto | ✅ Preservato |
| NFR-U-04 | Contrasto WCAG AA | ⚠️ Manual | ⚠️ Manual con check più rigoroso sui nuovi token |
| NFR-M-02 | Coverage ≥ 60% line+branch (client) | ✅ 74.18% / 60.95% | ✅ Da preservare; nuova logica audio + particle deve avere test |
| NFR-M-04 | Spotless | ✅ | ✅ |

### 3.3 SPEC sections impactate (vedi §11 SPEC change request)

- **§13.2 Design system** — riscrittura tokens (palette warm wood, tipografia, spacing, motion).
- **§13.3 Animazioni** — aggiunta easing juicy + particle effects + glow halo.
- **§13.4 Audio** — aggiunta ambient music orchestrale + playlist + volume controls (oggi descrive solo SFX).
- **§16 Fase 3.5** — nuova fase (mini) tra F3 e F4 con acceptance.
- **§16 Fase 11** — sottrazione delle voci anticipate ("Audio + animazioni avanzate", "jpackage Windows" — Mac+Linux jpackage restano F11).

---

## 4. Decomposizione in task

Ordine vincolato dalle dipendenze. Stima totale: **18 task ordinati**, ~3 settimane di lavoro a ritmo F3.

### Pre-requisito (estinzione debito F3)

#### Task 3.5.0 — Close TEST sotto-fase F3 debt
- Popolare `tests/TEST-PLAN-fase-3.md` sezioni 2-6, 8-10.
- Eseguire `mvn clean verify` root con `slow`+`performance` inclusi (~16 min). Documentare esito.
- Eseguire procedura manuale A3.3 (TEST-PLAN §7) sul `mvn -pl client javafx:run` corrente (look gestionale, OK perché valida solo crash-free + sotto-feature). Popolare §7.4.
- Aggiornare TRACEABILITY se necessario.
- Output: TEST-PLAN-fase-3 chiuso, regressione su F1 corpus + F2 gating IA confermata.
- **Moduli toccati**: documentazione + nessun codice prod.

### Foundation (design system + asset)

#### Task 3.5.1 — Asset acquisition + CREDITS
- Selezionare e scaricare:
  - 1-2 texture **legno**: warm brown + light beige (Kenney Boardgame Pack o equivalente CC0).
  - 1 texture **tavolo da gioco** (sfondo): wood polished o feltro verde scuro (default wood).
  - 3-5 tracce **musica orchestrale soft** ambient (es. Pixabay/Freesound/OpenGameArt — chamber orchestral, no battle/heroic). Durata 1-3 min ciascuna.
  - 4 SFX: click mossa (`move.wav` ~50ms), tonfo cattura (`capture.wav` ~200ms), chime promozione (`promotion.wav` ~600ms), fanfara vittoria (`victory.wav` ~2s). Tutti CC0 idealmente.
- Convertire in formati JavaFX-friendly: PNG per texture, MP3/OGG per audio.
- Compilare `client/src/main/resources/assets/CREDITS.md` con: filename, autore, fonte URL, licenza CC0/CC-BY, eventuale attribution string.
- **Moduli toccati**: `client/src/main/resources/assets/` (nuovo), `assets/CREDITS.md`.

#### Task 3.5.2 — Design tokens v2
- Riscrivere `client/src/main/resources/css/theme-light.css` con:
  - **Palette wood premium**: `--bg-primary: #2a1f15`, `--bg-secondary: #3d2e20`, `--surface: #f5e6c8` (cream), `--accent-gold: #c9a45c`, `--accent-deep-red: #8b3a3a`, `--text-on-dark: #f0e0c4`, `--text-on-light: #2a1f15`, `--border-frame: #6b4423`.
  - **Tipografia**: aggiungere `Playfair Display` o `Cormorant Garamond` come `--font-display` (titoli/header); `Inter` resta `--font-ui`. Caricare via `@font-face` da `client/src/main/resources/fonts/` (asset CC-BY/OFL).
  - **Spacing/radius**: invariato dove sensato; nuovi token per `--radius-button: 8px`, `--shadow-bevel: inset 0 1px 0 rgba(255,255,255,0.2), 0 2px 4px rgba(0,0,0,0.3)`.
  - **Motion**: nuovo `--easing-out-back: cubic-bezier(0.34, 1.56, 0.64, 1)` per overshoot.
- Aggiornare `components.css` con classi `.button-primary` (gradient gold + bevel + glow on hover), `.button-secondary` (frame wood scura), `.label-display` (font display, color accent-gold).
- **Moduli toccati**: `theme-light.css`, `components.css`, `fonts/` (nuovo file font), `theme-dark.css` lasciato stub invariato.

#### Task 3.5.3 — `package-info` audio + nuovo skeleton
- Nuovo package `com.damaitaliana.client.audio` con `package-info.java`.
- Skeleton: `AudioService` interface + implementazione, `MusicPlaylist` (record), `AudioBus` (enum: MUSIC, SFX), `Sfx` (enum: MOVE, CAPTURE, PROMOTION, VICTORY).
- **Moduli toccati**: `client/src/main/java/com/damaitaliana/client/audio/` (nuovo).

### Audio infrastructure

#### Task 3.5.4 — `AudioService` implementazione + Settings integration
- `AudioService` `@Component`: gestisce `MediaPlayer` per la traccia corrente + pool `MediaPlayer` per SFX.
- API: `playMusicShuffle()` (avvia shuffle, loop continuo), `stopMusic()`, `playSfx(Sfx)`, `setMusicVolume(int 0-100)`, `setSfxVolume(int 0-100)`, `mute(AudioBus)`, `unmute(AudioBus)`. Volume scaling lineare → JavaFX `[0.0, 1.0]`.
- Persistence: estendere `UserPreferences` record con `int musicVolumePercent` + `int sfxVolumePercent` + `boolean musicMuted` + `boolean sfxMuted`. Default: 30/70/false/false. Migrazione `config.json`: campi assenti → default.
- Settings screen: aggiungere 2 slider + 2 toggle. Wiring: change listener invoca `audioService.set*Volume`.
- Auto-start music su avvio app (controller `MainMenuController` o all'avvio di `JavaFxApp`); stop quando `terminate()` board view (opzionale: o continua sempre).
- Test: `AudioServiceTest` 8-10 test (volume scaling, mute toggles, shuffle deterministica con seed, SFX dispatch, persistence round-trip).
- **Moduli toccati**: `client/audio/AudioService.java`, `MusicPlaylist.java`, `AudioBus.java`, `Sfx.java`, `client/persistence/UserPreferences.java`, `client/ui/settings/SettingsController.java`, `fxml/settings.fxml`, `messages_*.properties` (chiavi `settings.audio.*`).

#### Task 3.5.5 — SFX wiring nei punti di gameplay
- `SinglePlayerController.finalizeMove`: dopo apply, `audioService.playSfx(Sfx.MOVE)` (o `CAPTURE` se la mossa era cattura → check `move.captureCount > 0`).
- Promozione (rilevata in `finalizeMove` confrontando il pezzo pre/post): `Sfx.PROMOTION`.
- Stato terminale (vittoria umana): `Sfx.VICTORY`.
- Test: `SinglePlayerControllerTest` aggiunge mock `AudioService` e verifica chiamate.
- **Moduli toccati**: `SinglePlayerController.java`, `SinglePlayerControllerTest.java`.

### Visual core (board + pieces + animazioni)

#### Task 3.5.6 — `BoardCellNode` redesign con texture legno
- Sostituire i background colorati piatti delle celle con `BackgroundImage` (texture legno chiaro/scuro alternato, da Task 3.5.1).
- Aggiungere `Frame` (cornice) attorno al `BoardRenderer`: `Region` decorativa con texture legno scura, padding 24px sui 4 lati.
- Highlight legali: cambiare da overlay verde piatto a glow giallo-oro (DropShadow color `--accent-gold` con `BlurType.GAUSSIAN`).
- Highlight obbligatorio (Task 3.5.9 lo riprende): per ora preservare comportamento corrente.
- **Moduli toccati**: `BoardCellNode.java`, `BoardRenderer.java`, `board-view.fxml` (aggiungere `Region` cornice), `components.css`.

#### Task 3.5.7 — `PieceNode` redesign 3D-look
- Sostituire `Circle` semplice con composizione `Group(Circle background + Circle ring scanalato + Circle gloss highlight + Text king-marker)`:
  - Background: gradient radiale (cream → ivory chiaro per bianco; brown scuro → black per nero) per simulare disco bombato.
  - Ring scanalato esterno: `Circle stroke` con DashArray simulando la scanalatura del disco fisico.
  - Gloss highlight: `Circle` piccolo bianco semi-trasparente in alto-sinistra per riflesso luce.
  - Dame: aggiungere `Text` o `Polygon` (corona stilizzata) al centro: oro su bianco, rosso scuro su nero.
- Ombra: DropShadow `(2px, 2px, 4px, rgba(0,0,0,0.5))`.
- Test: `PieceNodeTest` (nuovo, 5 test): rendering uomo bianco, uomo nero, dama bianca, dama nera, marker presente solo per dame.
- **Moduli toccati**: `PieceNode.java`, `PieceNodeTest.java` (nuovo), `BoardCellNode.java` (chiamante).

#### Task 3.5.8 — Animation polish (juicy + particle + glow)
- `MoveAnimator.translate`: cambiare interpolator da `Interpolator.EASE_OUT` a `Interpolator.SPLINE(0.34, 1.56, 0.64, 1)` (out-back).
- Cattura particle: nuovo helper `ParticleEffects.captureSplash(Pane host, double x, double y)`. Crea 8-12 `Circle` piccole color marrone/grigio polvere, ognuna con `ParallelTransition(TranslateTransition radiale, FadeTransition out, ScaleTransition shrink)` durata 350ms. `setOnFinished` rimuove i nodi dal parent.
- Promozione raggi: `ParticleEffects.promotionGlow(Pane host, double x, double y)`. 8-12 `Line` color `#FFD700/#FFA500`, irradianti dal centro con `ParallelTransition(ScaleTransition expand, FadeTransition out)` 600ms. Sovrappone al flash dorato esistente.
- Glow halo cattura obbligatoria: `ParticleEffects.mandatoryGlow(Node piece)`. Applica `DropShadow` al pezzo, animato in `Timeline` cycle infinito 1200ms (radius 12→24→12, color `#FFD700`).
- `AnimationOrchestrator` invoca i nuovi effetti.
- Test: aggiornare `MoveAnimatorTest` + `AnimationOrchestratorTest` per asserire la presenza degli effetti (count children sul Pane prima/dopo).
- **Moduli toccati**: `MoveAnimator.java`, `AnimationOrchestrator.java`, nuovo `ParticleEffects.java`, test.

### Screen restyling

#### Task 3.5.9 — Splash screen
- Sostituire splash piatto con: sfondo wood polished, logo "Dama Italiana" in font display oro centrato, sottotitolo italico "Gioco classico contro IA" più piccolo, progress bar wood-themed in basso.
- Loading time invariato.
- **Moduli toccati**: `splash.fxml`, `SplashController.java` (minor), `theme-light.css` (classi `.splash-*`).

#### Task 3.5.10 — Main menu
- Layout invariato (6-card grid), restyling: ogni card con cornice in legno scura, hover glow oro, icon Ikonli a colori warm brown, font display per titolo card.
- Background: texture wood subtle (parquet o table top).
- **Moduli toccati**: `main-menu.fxml`, `MainMenuController.java` (minor), css.

#### Task 3.5.11 — SP setup, Save dialog, Load screen, Settings, Rules screen
- Restyling coordinato delle 5 schermate restanti con stessi token v2.
- Settings include i nuovi audio sliders (Task 3.5.4).
- Rules: heading display gold, body serif italics per quote regole, diagrammi su sfondo wood (riusa `MiniatureRenderer` corrente).
- **Moduli toccati**: 5 fxml + relative css + (minor) controller per eventuali nuovi nodi.

### Packaging Windows

#### Task 3.5.12 — `jpackage` Windows MSI
- Aggiungere `org.panteleyev:jpackage-maven-plugin` al `client/pom.xml` (CC0 / Apache plugin).
- Configurazione:
  - `--name "Dama Italiana"`, `--app-version 0.3.5`, `--vendor "Dama Italiana"`, `--type msi`.
  - `--input target/jpackage-input/` (jar + dependencies copiate via maven-dependency-plugin).
  - `--main-jar dama-italiana-client-0.3.5-SNAPSHOT.jar`, `--main-class com.damaitaliana.client.JavaFxApp` (o equivalente entry point).
  - `--win-shortcut`, `--win-menu`, `--win-menu-group "Dama Italiana"`, `--icon assets/app-icon.ico`.
  - `--runtime-image` con `jlink` modules custom (riduce JRE bundled da ~300MB a ~80MB).
- Prerequisito: WiX Toolset 3.x installato sulla macchina build (documentare in README e nel commit).
- Output: `target/jpackage/dama-italiana-0.3.5.msi`.
- **Moduli toccati**: `client/pom.xml`, `assets/app-icon.ico` (nuovo), `README.md` (sezione "Build installer Windows").

### Validation + cleanup

#### Task 3.5.13 — Test fixes per breakage FXML/layout
- Le modifiche ai 5+ FXML (Task 3.5.6, 3.5.9-3.5.11) probabilmente rompono `*FxmlSmokeTest.java` (assertion su node tree).
- Identificare i breakage: `mvn -pl client verify` post-Task 3.5.11. Per ognuno: aggiornare assertion al nuovo tree (non sopprimere il test).
- Stima 5-15 test da aggiustare.
- **Moduli toccati**: vari `*Test.java`.

#### Task 3.5.14 — Manual demo run + visual review
- Build `.msi`, installare su VM Win 10 fresca o macchina locale Win 11 dell'utente.
- Eseguire ogni A3.5.x: salvare screenshot in `tests/visual-review/<screen>.png` (8 immagini).
- Giocare partita completa vs IA Esperto, vincere o perdere o pattare. Verificare ogni A3.5.x dei punti audio/animazione.
- Popolare `tests/TEST-PLAN-fase-3.5.md` §7 con esito.
- Se uno o più A3.5.x falliscono → aprire come finding `BUG` o `REQUIREMENT_GAP` e tornare a IMPLEMENTA.
- **Moduli toccati**: `tests/TEST-PLAN-fase-3.5.md`, `tests/visual-review/*.png`.

#### Task 3.5.15 — Documentation + ADR
- Aggiungere ADR per: visual rework rationale (ADR-034), AudioService architecture (ADR-035), jpackage Windows packaging (ADR-036), asset licensing strategy (ADR-037).
- Aggiornare README sezione "Eseguire il client" con istruzioni installer + nota su cliente demo.
- Aggiornare CHANGELOG `[Unreleased]` con voci F3.5.
- Aggiornare TRACEABILITY (sezione "Fase 3.5") con mappatura A3.5.x → test.
- **Moduli toccati**: `ARCHITECTURE.md`, `README.md`, `CHANGELOG.md`, `tests/TRACEABILITY.md`, `AI_CONTEXT.md`.

### Closure

#### Task 3.5.16 — REVIEW Fase 3.5
- Output: `reviews/REVIEW-fase-3.5.md`.
- Mappare A3.5.x → codice; findings categorizzati per CLAUDE.md §2.3.1.
- Stop point con utente per fix concordati.

#### Task 3.5.17 — TEST sotto-fase Fase 3.5
- Output: completare `tests/TEST-PLAN-fase-3.5.md`.
- Regression `mvn clean verify` root con `slow`+`performance`.
- Closure check.

---

## 5. Strategia di test

| Tipo | Cosa | Tooling |
|---|---|---|
| **Unit** | `AudioServiceTest` (volume, mute, playlist), `PieceNodeTest` (rendering), `ParticleEffectsTest` (count + lifecycle), `UserPreferencesTest` extension (audio fields persistence) | JUnit 5 + Mockito |
| **Integration / UI** | `*FxmlSmokeTest` aggiornati al nuovo tree; `SettingsControllerTest` esteso con audio slider; `*SpringContextTest` per resolve `AudioService` bean | Spring Boot Test + JavaFX (con `Assumptions.assumeTrue` per skip headless) |
| **E2E manuali** | Manual demo run (Task 3.5.14) — non testabile programmaticamente in modo affidabile | Sessione manuale registrata in TEST-PLAN |
| **Visual regression** | Screenshot review (Task 3.5.14) — checked-in in `tests/visual-review/` per riferimento futuro | Manual diff post-modifica |
| **Coverage** | Stesso gate F3 (60% line+branch). `AudioService` + `ParticleEffects` + `PieceNode` aggiungono ~200 LOC; serve ~70% coverage su questi per non scendere | JaCoCo |
| **Regression** | Tutti i 280 test F3 + corpus regole F1 + gating IA F2 verdi alla closure | `mvn clean verify` root con `slow`+`performance` |

---

## 6. Rischi identificati e mitigazioni

| Rischio | Probabilità | Impatto | Mitigazione |
|---|---|---|---|
| Particle effects + ambient music degrada FPS sotto 60 | Media | Medio | Test manuale sul box dello sviluppatore (Win 11). Se FPS scende: ridurre conteggio particle 12→6, considerare Canvas batched draw invece di Node-per-particle |
| TestFX/smoke test rotti dalle modifiche FXML/layout | Alta | Alto | Aggiustamenti incrementali Task 3.5.13; non sopprimere asserzioni |
| Asset licensing diligence — CC-BY richiede attribution rigorosa | Bassa | Medio | Task 3.5.1 produce `CREDITS.md` esplicito; preferire CC0 dove possibile |
| Audio playback latency su Windows (JavaFX Media non eccelle) | Media | Basso | SFX < 600ms, pre-load all'avvio, `MediaPlayer.setOnReady` warmup; se latency >50ms: switch a libreria `javax.sound.sampled` |
| `jpackage` build environment (WiX Toolset) | Media | Medio | Documentare prerequisiti; testare build su una macchina pulita Win 10/11 |
| Coverage scende sotto 60% per nuovo codice non testato | Bassa | Alto | TDD su `AudioService`, `ParticleEffects`, `PieceNode` |
| WCAG AA violato su nuovi token (oro su bianco potrebbe avere contrast basso) | Media | Medio | Verificare ogni coppia testo/sfondo con [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/); accent-gold solo su sfondi scuri |
| Cliente Win 8.1 invece di Win 10 (già escluso) | Risolto | — | — |
| Demo crash su macchina cliente per dipendenza mancante | Bassa | Critico | Task 3.5.12 jpackage bundle JRE; Task 3.5.14 test su box Win pulito |
| Tag `v0.3.5` vs `v0.4.0` confusione SemVer | Bassa | Basso | Convenzione documentata: `v0.3.5` = polish/visual, `v0.4.0` = `core-server` skeleton |

---

## 7. Stop point del piano

Tre stop point durante la fase (oltre allo stop point §10 di approvazione iniziale):

### 7.1 Approvazione asset (post Task 3.5.1)
Mostro l'utente:
- 2-3 candidate texture legno (chiaro/scuro)
- 3-5 candidate tracce ambient orchestral (link Pixabay/Freesound + 30s preview)
- 4 candidate SFX

Utente sceglie. Procedo con quelli scelti.

### 7.2 Approvazione design tokens v2 (post Task 3.5.2)
Mostro: file CSS aggiornato + screenshot di un mock di main menu o board.
Utente conferma palette + tipografia OK. Solo se OK procedo con il restyling delle altre schermate.

### 7.3 Stop point pre-tag (post Task 3.5.14)
Mostro: installer `.msi` + screenshot di tutte le 8 schermate + audio playback log.
Utente conferma "demo-ready". Solo dopo conferma → tag v0.3.5 + merge develop+main.

---

## 8. Stima

- **18 task** ordinati.
- A ritmo F3 (~1.5 task/giorno mediamente, considerando design discussion + testing + linting): ~12 giorni di lavoro effettivo + ~2 sessioni di review/test.
- Calendario indicativo (no deadline cliente, ma ragionevole): **3 settimane** dalla approvazione del PLAN al tag `v0.3.5`.

---

## 9. Branch e versioning

- **Branch**: `feature/3.5-visual-polish-and-audio` (già creato post `v0.3.0`).
- **Tag**: `v0.3.5` sul commit di merge in `main`.
- **Workflow merge** (CLAUDE.md §4.4): `feature/3.5-* → develop --no-ff → main --no-ff → tag v0.3.5`.
- F4 (`core-server` skeleton) staccherà `feature/4-core-server-skeleton` da `develop` post-tag `v0.3.5`.

---

## 10. Stop point principale: approvazione del piano

**Approva**:
- Acceptance criteria §2.
- Decomposizione task §4 (l'ordine può cambiare con motivazione documentata in `AI_CONTEXT.md`).
- SPEC change request §11.
- Branch + tag plan §9.

**Eventualmente correggi**:
- Numero/ordine task.
- Acceptance specifici.
- Asset budget (es. preferire un altro pack texture).
- SPEC change request §11.

---

## 11. SPEC change request

Per CLAUDE.md §1, le modifiche allo SPEC seguono il flusso CR. Questo PLAN propone le seguenti modifiche, che andrebbero applicate con commit dedicato `docs(spec): pull-forward parts of fase 11 into fase 3.5 for client demo` **prima** dell'IMPLEMENTA.

### CR-F3.5-001 — Riscrittura §13.2 Design system (palette wood premium)

**Contesto**: §13.2 attuale descrive un design system "moderno controllato e coerente" con token CSS generici (palette neutra, font Inter). Il nuovo direzione "videogame premium wood" richiede token esplicitamente legati al tema legno + tipografia display.

**Proposta**: Sostituire i token color con palette wood (cream/dark-brown/gold/deep-red), aggiungere `--font-display` (Playfair Display o equivalente CC-BY/OFL), aggiungere token motion (`--easing-out-back`).

**Decisione utente**: PENDING.

### CR-F3.5-002 — Estensione §13.3 Animazioni (juicy + particle + glow)

**Contesto**: §13.3 oggi specifica animazioni funzionali (250/200/500/800ms con easing OUT_QUAD). Il nuovo direzione richiede:
- Easing **out-back** per la mossa (overshoot leggero su atterraggio).
- **Particle effects**: puff polvere su cattura, raggi dorati su promozione.
- **Glow halo esterno** (DropShadow animato) per cattura obbligatoria, in sostituzione/aggiunta del `pulse-mandatory FillTransition`.

**Proposta**: aggiungere paragrafo "Animazioni avanzate (da F3.5)" con specifiche puntuali (easing curve, particle count, durata).

**Decisione utente**: PENDING.

### CR-F3.5-003 — Estensione §13.4 Audio (ambient music + volume controls)

**Contesto**: §13.4 oggi descrive solo SFX ("Suoni discreti: click mossa, tonfo cattura, chime promozione, fanfara vittoria"). Il nuovo direzione richiede ambient music orchestrale soft con playlist e volume control.

**Proposta**: aggiungere paragrafo "Music ambient (da F3.5)" con specifiche: 3-5 tracce, random shuffle, default 30%, mutabile via Settings con persistence.

**Decisione utente**: PENDING.

### CR-F3.5-004 — Aggiungere §16 Fase 3.5 e modificare §16 Fase 11

**Contesto**: roadmap §16 ha 12 fasi (0-11). F3.5 è una mini-fase intermedia non prevista.

**Proposta**:
1. Inserire **§16 Fase 3.5 — Visual polish + Audio + Demo release** tra Fase 3 e Fase 4 con bullet:
   - Visual rework completo delle 8 schermate F3 con design system v2 (palette wood premium).
   - Audio: ambient music orchestrale + SFX gameplay.
   - Animazioni juicy + particle effects.
   - Packaging Windows `.msi` via `jpackage`.
   - **Acceptance**: cliente apre installer Win 10/11, gioca demo SP completa, percepisce stile "videogame premium" + audio coerente.
2. Modificare **§16 Fase 11**: rimuovere "Audio + animazioni avanzate" (anticipato F3.5); rimuovere implicit jpackage Windows (anticipato F3.5); resta jpackage Mac+Linux. Aggiungere annotazione "*(visuali e audio Windows installer anticipati a F3.5 per esigenza demo cliente)*".

**Decisione utente**: PENDING.

### CR-F3.5-005 — Bump SPEC version 2.1 → 2.2

**Contesto**: applicazione delle CR sopra è una modifica significativa.

**Proposta**: bump frontmatter a **2.2 (data applicazione)** + storico inline aggiornato.

**Decisione utente**: PENDING.

---

## 12. Memoria operativa

Aggiornare `AI_CONTEXT.md` post-approvazione del PLAN:

- Sotto-fase corrente: **PIANIFICA Fase 3.5 → in attesa approvazione**.
- Branch: `feature/3.5-visual-polish-and-audio`.
- Estinzione TEST F3 debt: pianificata come Task 3.5.0 (primo task IMPLEMENTA).

---

**FINE PIANO**
