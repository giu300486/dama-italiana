# PLAN — Fase 3: Client UI single-player

- **Riferimento roadmap**: `SPEC.md` §16 — Fase 3.
- **SPEC version**: 2.0 (2026-04-26).
- **Data piano**: 2026-04-28.
- **Autore**: Claude Code.
- **Stato**: DRAFT — in attesa di approvazione utente.

---

## 1. Scopo della fase

Costruire il **client desktop JavaFX** in modalità single-player end-to-end: bootstrap JavaFX + Spring Boot DI (non-web), design system CSS custom, schermate splash / main menu / setup partita / board view / settings / regole, rendering damiera con animazioni, interazione (highlight mosse legali e cattura obbligatoria), cronologia mosse FID, salva/carica multi-slot, autosave con prompt di ripresa, localizzazione IT/EN. La modalità di gioco è esclusivamente **giocatore umano vs IA** (3 livelli), riusando `AiEngine` di Fase 2 e `RuleEngine` di Fase 1.

Obiettivi specifici (SPEC §16 Fase 3):

1. **Setup JavaFX + design system CSS custom** (SPEC §13.2): `theme.css` parametrico via CSS variables, font Inter caricato, Ikonli MD2/FA5 wired.
2. **Schermate**: splash, main menu, single-player setup, board view, settings.
3. **Rendering damiera + pezzi + animazioni** (SPEC §13.3): `TranslateTransition` 250 ms easing OUT_QUAD per mossa; `ScaleTransition` 200 ms + `FadeTransition` per cattura; flash dorato + `RotateTransition` 500 ms per promozione; `FillTransition` ciclica 800 ms per highlight cattura obbligatoria.
4. **Highlight mosse legali + cattura obbligatoria** (FR-SP-04, FR-SP-05): giallo per legali, rosso pulsante per obbligatorie.
5. **Cronologia mosse in notazione FID** (FR-SP-09): pannello laterale.
6. **Salva/carica multi-slot** (FR-SP-07): file dedicato per partita, lista ordinabile, miniatura board.
7. **Autosave** (FR-SP-08): write post-mossa su `_autosave.json`; prompt "Riprendi partita interrotta?" all'avvio.
8. **Localizzazione IT/EN** (FR-RUL-05, NFR-U-01): `MessageSource` Spring + `messages_it.properties` / `messages_en.properties`.
9. **Schermata regole in-app** (FR-RUL-01÷04): sezioni setup/movimento/cattura/leggi/promozione/fine partita/notazione, diagrammi statici renderizzati con il board renderer in modalità snapshot.
10. **Acceptance SPEC §16 Fase 3**: utente gioca partita completa contro IA, salva, ricarica, riapre dopo crash con autosave.

**Out of scope** (rinviati a fasi successive):

- LAN host/discovery e schermate LAN (Fase 7).
- Lobby online, account/JWT, schermate online (Fase 5–6).
- Tornei: hub, room, bracket, classifica (Fase 8–10).
- Profilo & statistiche, storia partite, replay viewer, sistema report (Fase 11).
- **Audio** (suoni mossa/cattura/promozione/vittoria) — SPEC §16 Fase 11 ("Audio + animazioni avanzate"). Le animazioni di §13.3 sono in F3, gli effetti "juicy" extra restano F11.
- **Dark mode toggle runtime** — SPEC §16 Fase 11. Vedi stop point §7.3 per la decisione su quanto del CSS dark esporre già in F3.
- **Cifratura JWT del config.json** (SPEC §14.1 "AES-GCM con chiave derivata dalla machine-id") — non c'è ancora un token JWT da cifrare prima di Fase 5/6. `PreferencesService` scrive in chiaro lingua/scaling/preferenze; il campo `jwtTokenEncrypted` è omesso fino a Fase 5.
- **Pattern daltonismo sui pezzi** (SPEC §13.5 "linee, puntini") — feature di a11y avanzata, deferred a Fase 11 polish. In F3 si garantisce contrasto WCAG AA in light mode (NFR-U-04) e `accessibleText` su pezzi e pulsanti chiave (NFR-U-03).
- **Mini-animazioni dimostrative regole** (FR-RUL-04 "opzionali") — vedi stop point §7.6.
- Tuning empirico delle animazioni oltre i parametri SPEC §13.3.

---

## 2. Acceptance criteria

### 2.1 SPEC §16 Fase 3 (autoritativo)

> **Acceptance**: utente gioca partita completa contro IA, salva, ricarica, riapre dopo crash con autosave.

### 2.2 Criteri operativi estesi

| ID    | Criterio                                                                                                                                                                                                                                     | Verificabile come                                              |
|-------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| A3.1  | `mvn -pl client verify` BUILD SUCCESS.                                                                                                                                                                                                       | Output Maven                                                   |
| A3.2  | `mvn -pl client javafx:run` lancia il client; splash → main menu visibili senza eccezioni in console.                                                                                                                                        | Manuale, screencast nella REVIEW                               |
| A3.3  | **Partita end-to-end vs IA Esperto** chiusa fino a stato terminale (vittoria, sconfitta o patta) **senza crash UI** (TestFX o test custom con `Stage` headless).                                                                              | `SinglePlayerE2ETest#completesGameVsEsperto`                  |
| A3.4  | **Salva con nome → ricarica → riprendi**: nuova partita, alcune mosse, salva con nome `partita-test`, chiudi, riapri client, schermata Carica mostra `partita-test` con metadati corretti, click → board view nello stato salvato.            | `SaveLoadE2ETest#saveThenLoadResumesAtSameState`               |
| A3.5  | **Autosave recovery**: nuova partita, alcune mosse, chiudi finestra senza terminare → riapri client → prompt "Riprendi partita interrotta?" → conferma → board view nello stato pre-chiusura.                                                  | `AutosaveE2ETest#promptOnRestartWhenAutosavePresent`           |
| A3.6  | **Highlight cattura obbligatoria** (FR-SP-05): in posizione con cattura obbligatoria, le case di partenza dei pezzi che possono catturare hanno `pulse-mandatory` style class attiva (verifica via TestFX `lookup` o `BoardController` API).   | `BoardInteractionTest#mandatoryCaptureIsHighlighted`           |
| A3.7  | **Highlight mosse legali** (FR-SP-04): click su pedina propria → le case di destinazione delle mosse legali da quella pedina hanno `legal-target` style class. Click su casa avversaria o vuota → nessun highlight.                            | `BoardInteractionTest#legalTargetsHighlightedOnSelect`          |
| A3.8  | **Cronologia mosse in notazione FID** (FR-SP-09): dopo N mosse, il pannello laterale mostra le mosse formattate da `FidNotation.format(move)` in righe alternate Bianco/Nero, in ordine.                                                       | `MoveHistoryControllerTest#displaysMovesInFidNotation`         |
| A3.9  | **Localizzazione IT/EN funzionante** (NFR-U-01): cambio lingua nelle Settings → riavvio → tutti i testi UI nella nuova lingua. Nessuna stringa hardcoded sfugge a `MessageSource` (verifica grep su `setText("...")` con stringhe non-key).    | `LocalizationTest` + grep                                       |
| A3.10 | **Sezione regole accessibile** (FR-RUL-01, AC §17.1.12): card "Regole" da home + icona ⓘ in board view aprono la stessa schermata; navigazione laterale per le 7 sezioni; almeno 1 diagramma board statico per sezione "Cattura".               | `RulesScreenTest#openRulesAndNavigateSections`                  |
| A3.11 | **Pedina non cattura dama** end-to-end UI (AC §17.1.8): in posizione preparata via FEN/JSON, la pedina umana non ha la cattura della dama avversaria fra i target highlightati.                                                                | `BoardInteractionRulesTest#manCannotCaptureKing`                |
| A3.12 | **Promozione termina turno** end-to-end UI (AC §17.1.7): pedina che raggiunge promozione mid-sequenza si ferma; l'animazione di promozione viene riprodotta; il turno passa all'avversario.                                                    | `BoardInteractionRulesTest#promotionStopsSequence`              |
| A3.13 | **Coverage modulo `client`** ≥ **60%** line + branch sui package non-view (NFR-M-02). I package `com.damaitaliana.client.ui.view` (FXML controller di rendering puro) sono **esclusi** dal gate. Vedi stop point §7.8.                          | JaCoCo `haltOnFailure=true` con regola dedicata                |
| A3.14 | Spotless OK, SpotBugs 0 High su tutto `client`.                                                                                                                                                                                              | Output Maven                                                   |
| A3.15 | `package-info.java` per ogni nuovo sotto-package del client (`ui`, `controller`, `persistence`, `i18n`, `app`).                                                                                                                              | Lettura visiva                                                 |
| A3.16 | `tests/TRACEABILITY.md` aggiornato: FR-SP-01÷09, FR-RUL-01÷05, NFR-U-01, NFR-U-04 (light), NFR-M-02 client, AC §17.1.1 (chiusura), §17.1.7, §17.1.8, §17.1.9, §17.1.12.                                                                       | Lettura visiva                                                 |
| A3.17 | Nessun TODO/FIXME pending in `client/src/main/java/`.                                                                                                                                                                                        | grep                                                           |
| A3.18 | **Test corpus regole italiane (Fase 1, 48 posizioni)** continua a passare (regression). **Gating IA F2** (`AiTournamentSimulationTest`) **continua a passare** (regression).                                                                  | `RuleEngineCorpusTest` + `AiTournamentSimulationTest` verdi    |
| A3.19 | `mvn clean verify` (root): BUILD SUCCESS su tutti i moduli.                                                                                                                                                                                  | Output Maven root                                              |
| A3.20 | **Schema versionato** dei salvataggi: file `~/.dama-italiana/saves/*.json` ha `"schemaVersion": 1`; il loader rifiuta versioni ignote con messaggio chiaro all'utente (toast).                                                                  | `SaveServiceTest#rejectsUnknownSchemaVersion`                   |
| A3.21 | **Atomicità autosave**: scrittura `_autosave.json` con pattern `write-temp + atomic-rename` (no file half-written se il processo viene killato a metà write). Test scrive su tmp dir e simula interruzione.                                    | `SaveServiceTest#autosaveIsAtomic`                              |

---

## 3. Requisiti SPEC coperti

### 3.1 Funzionali (FR)

| FR ID    | SPEC ref | Coperto in F3 come                                                                                                                                                          |
|----------|----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FR-SP-01 | §4.1     | **Coperto pienamente**: nuova partita avviabile da main menu senza account né rete; bootstrap offline.                                                                       |
| FR-SP-02 | §4.1     | **Coperto pienamente**: setup modal con `AiLevel` enum (Principiante/Esperto/Campione) wired su `AiEngine.forLevel(...)`.                                                    |
| FR-SP-03 | §4.1     | **Coperto pienamente**: setup modal con scelta colore Bianco/Nero/Casuale.                                                                                                  |
| FR-SP-04 | §4.1     | **Coperto pienamente**: highlight giallo (style class `legal-target`) sulle case di destinazione al click su pezzo proprio.                                                |
| FR-SP-05 | §4.1     | **Coperto pienamente**: highlight rosso pulsante (style class `pulse-mandatory` + `FillTransition`) sui pezzi con cattura obbligatoria.                                    |
| FR-SP-06 | §4.1     | **Coperto**: undo/redo illimitato basato su lista lineare di `GameState` (no branching). Vedi stop point §7.13 per la semantica con IA in mezzo.                          |
| FR-SP-07 | §4.1     | **Coperto pienamente**: SaveService multi-slot, schermata Load con lista ordinabile (per nome/data/livello), miniatura board snapshot.                                     |
| FR-SP-08 | §4.1     | **Coperto pienamente**: autosave write-through dopo ogni `applyMove`; prompt al riavvio se `_autosave.json` esiste.                                                         |
| FR-SP-09 | §4.1     | **Coperto pienamente**: pannello laterale con cronologia in notazione FID, formattata da `FidNotation.format(move)`.                                                        |
| FR-RUL-01 | §4.6    | **Coperto pienamente**: card "Regole" da main menu + icona ⓘ in board view.                                                                                                  |
| FR-RUL-02 | §4.6    | **Coperto pienamente**: navigazione laterale con le 7 sezioni (setup, movimento, cattura, leggi, promozione, fine partita, notazione).                                       |
| FR-RUL-03 | §4.6    | **Coperto pienamente**: diagrammi statici della damiera generati con il board renderer in modalità snapshot (no interazione).                                               |
| FR-RUL-04 | §4.6    | **Stop point §7.6**: SPEC qualifica le mini-animazioni come "opzionali". Proposta: 3 mini-animazioni (cattura semplice, presa multipla, promozione) incluse in F3 come riuso dell'animation engine già scritto per il board view; nessuna animazione completamente nuova. |
| FR-RUL-05 | §4.6    | **Coperto pienamente**: tutti i testi della schermata regole sono in `messages_it.properties` / `messages_en.properties`.                                                    |

### 3.2 Non funzionali (NFR)

| NFR ID   | SPEC ref | Coperto in F3 come                                                                                                                                                                          |
|----------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-P-01 | §5       | **Coperto operativamente**: animazioni 200-500 ms con interpolatori JavaFX (TranslateTransition ecc.); rendering canvas/Pane senza ridipinti spuri. Verifica visiva nella REVIEW; eventuale FPSMeter di debug. Misurazione formale 60 FPS rinviata a Fase 11 (NFR-P-01 monitorato continuativamente). |
| NFR-U-01 | §5       | **Coperto pienamente**: `MessageSource` Spring + `ResourceBundle` IT/EN. `LocaleService` esposto come bean.                                                                                  |
| NFR-U-02 | §5       | **Parziale**: design token `theme-light.css` definitivo in F3; `theme-dark.css` come **stub presente ma non selezionabile** in Settings (toggle runtime in Fase 11). Vedi stop point §7.3.    |
| NFR-U-03 | §5       | **Parziale**: `accessibleText` su pezzi/case + tab-navigation per main menu, settings, save/load. Keyboard nav del board (frecce per spostare focus, Enter per selezionare) aggiunta come **best-effort**: target di ≥1 flusso completo giocabile da tastiera. Daltonismo deferred F11. |
| NFR-U-04 | §5       | **Coperto in light mode**: contrasto WCAG AA verificato per i token già definiti in §13.2 SPEC. Dark mode WCAG AA con verifica tool deferred a Fase 11 con il toggle.                       |
| NFR-M-02 | §5       | **Coperto pienamente**: gate JaCoCo `client` ≥ 60% sui package non-view. Vedi stop point §7.8.                                                                                              |
| NFR-M-04 | §5       | Spotless Google Java Style su tutto il codice F3.                                                                                                                                            |

### 3.3 Acceptance criteria globali (§17)

| AC §17  | Descrizione                                                                       | Coperto in F3 da                                                                |
|---------|-----------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| 17.1.1  | Partita SP vs IA Campione si conclude entro 30 minuti senza crash.                | **Chiuso operativamente** in F3: `SinglePlayerE2ETest` valida partita completa vs Esperto (più rapida da simulare); test marginale @Tag("slow") vs Campione documentato come opzionale. F2 ha già validato che Campione termina entro budget. |
| 17.1.7  | Pedina che raggiunge promozione durante una sequenza non continua.                 | **End-to-end UI coperto** da `BoardInteractionRulesTest#promotionStopsSequence`. F1 ha già la regola lato `RuleEngine`.                       |
| 17.1.8  | Pedina non può catturare la dama.                                                  | **End-to-end UI coperto** da `BoardInteractionRulesTest#manCannotCaptureKing`.                                                                |
| 17.1.9  | Salvataggio multi-slot single-player funziona: lista, carica, riprendi.            | **Coperto pienamente** da `SaveLoadE2ETest`.                                                                                                  |
| 17.1.12 | Sezione regole in-app accessibile e completa.                                      | **Coperto pienamente** da `RulesScreenTest` + revisione manuale dei contenuti.                                                                |
| 17.2.3  | Client a 60 FPS durante animazioni.                                                | **Indiretto**: animazioni implementate con primitive JavaFX standard (HW-accelerated). Misura formale a Fase 11.                              |
| 17.2.4  | Coverage ≥ 80% modulo `shared`.                                                    | Mantenuto da F1+F2 (gate JaCoCo).                                                                                                             |
| 17.2.5  | SAST SpotBugs senza warning High.                                                  | Mantenuto.                                                                                                                                    |
| 17.2.7  | Dark mode e light mode entrambi WCAG AA.                                           | **Parziale**: light mode WCAG AA in F3. Dark mode + toggle a Fase 11.                                                                         |

### 3.4 ADR coinvolti

| ADR     | Vincolo per Fase 3                                                                                                                                                                                                                  |
|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ADR-001 | Java 21 LTS → records, sealed types, pattern matching, virtual threads (per IA già in F2).                                                                                                                                          |
| ADR-003 | JavaFX 21+ con CSS custom design system (no AtlantaFX, no temi esterni).                                                                                                                                                            |
| ADR-004 | DI client: Spring Boot starter non-web. `@SpringBootApplication` con `@Configuration` per bean UI; `application.yml` per default lingua/path saves.                                                                                  |
| ADR-013 | Solo regole Dama Italiana FID → l'UI usa `RuleEngine` di F1 senza varianti.                                                                                                                                                         |
| ADR-015 | 3 livelli IA → setup modal con esattamente i 3 valori dell'enum `AiLevel`.                                                                                                                                                          |
| ADR-016 | Salvataggi single-player: multi-slot + autosave (formalizzato dallo SPEC). F3 lo realizza.                                                                                                                                           |
| ADR-020 | Notazione FID 1-32 → la cronologia mosse e i diagrammi delle regole usano `FidNotation`.                                                                                                                                            |
| ADR-024÷028 | Architettura IA F2 → `client` consuma `AiEngine.forLevel()` + `VirtualThreadAiExecutor` senza re-implementare logica IA.                                                                                                          |

**Possibili nuovi ADR generati da F3** (da formalizzare in `ARCHITECTURE.md`):

- **ADR-029 — JavaFX bootstrap con Spring Boot DI non-web**: `ClientApplication` (Spring Boot main) avvia il container, poi `Application.launch(JavaFxApp.class)` riceve l'`ApplicationContext` via singleton statico. Vedi stop point §7.1.
- **ADR-030 — Architettura schermate: FXML + Spring-aware controller factory**: ogni schermata è un FXML con controller iniettato via `FXMLLoader.setControllerFactory(applicationContext::getBean)`. Vedi stop point §7.2.
- **ADR-031 — Schema file salvataggi v1**: JSON con campi `schemaVersion`, `kind`, `name`, `createdAt`, `updatedAt`, `aiLevel`, `humanColor`, `moves` (lista FID), `currentState` (board + sideToMove + halfmoveClock + history). Loader rifiuta `schemaVersion` ignota. Vedi stop point §7.4.
- **ADR-032 — Autosave atomico tramite write-temp + ATOMIC_MOVE**: scrittura su `_autosave.json.tmp` poi `Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)`. Vedi stop point §7.5.
- **ADR-033 — Localizzazione: `MessageSource` Spring come SSOT, `LocaleService` come bridge fra preferenza utente e `Locale.setDefault()`**: chiave hierarchica (`menu.singleplayer.title` ecc.). Vedi stop point §7.9.

---

## 4. Decomposizione in task

I task sono ordinati. Ogni task termina con `mvn -pl client verify` verde e un commit Conventional Commits.

> **Nota branching** (CLAUDE.md §4.3): tutto il lavoro di Fase 3 si svolge sul branch `feature/3-ui-singleplayer` staccato da `develop`, mergiato `--no-ff` su `develop` a fine fase. Tag `v0.3.0` sul commit di merge in `main`. Vedi stop point §7.14.

### Task 3.1 — Bootstrap JavaFX + Spring Boot DI

**Output** (in `client/src/main/java/com/damaitaliana/client/`):

- `ClientApplication.java` (`@SpringBootApplication(scanBasePackages = "com.damaitaliana.client")`): `main` chiama `SpringApplication.run` con `WebApplicationType.NONE`, poi `Application.launch(JavaFxApp.class, args)`.
- `app/JavaFxApp.java extends javafx.application.Application`: `start(Stage)` carica il Primary Stage; ottiene l'`ApplicationContext` via `JavaFxAppContextHolder.get()` (singleton statico set in `ClientApplication.main`).
- `app/JavaFxAppContextHolder.java`: utility statico per condividere il context fra Spring e JavaFX (alternativa: `AbstractJavaFxApplicationSupport` di terze parti — scartato per minimizzare dipendenze).
- `app/SceneRouter.java` (Spring `@Component`): bean centrale che orchestra il cambio di scena. API: `void show(SceneId id)`, `void showModal(SceneId id, Object payload)`. Internamente usa `FxmlLoaderFactory` (Task 3.2).
- `client/pom.xml`: aggiungere `<configuration><mainClass>com.damaitaliana.client.ClientApplication</mainClass></configuration>` al `javafx-maven-plugin` (rimozione TODO Fase 3 dal commento esistente).
- `application.yml` in `client/src/main/resources/`: profilo `default` con `spring.main.web-application-type=none`, eventuale `dama.client.savesDir=${user.home}/.dama-italiana/saves` (override per test).
- Rimozione di `ClientSmokeTest` (CLAUDE.md §6.1 nota op: rimosso quando il modulo riceve test reali).

**Test** (`ClientBootstrapTest`): ~3 metodi.

- `springContextStartsWithoutWeb` (Spring Boot test, web-application-type=none).
- `mainBeanIsResolvable` (verifica `SceneRouter` bean presente).
- `applicationYmlOverridesSavesDir` (test profile + `@TestPropertySource`).

**Vincoli**:

- **Niente** Tomcat. La `spring-boot-starter-websocket` (con Tomcat escluso) è nel POM da F0 ma non viene attivata in F3 (LAN host = F7).
- `ClientApplication` non DEVE avere logica UI; solo bootstrap.
- Logging via SLF4J: `Logger log = LoggerFactory.getLogger(...)`.

**Dipendenze**: nessuna (prima task della fase).

---

### Task 3.2 — Design system: theme.css, font Inter, Ikonli setup

**Output** (in `client/src/main/resources/`):

- `css/theme-light.css`: tutti i token CSS variable di SPEC §13.2 (palette light, board, tipografia, spacing, radius). Stati interattivi `:hover`, `:pressed`, `:focused`, `:disabled` per `.button`, `.card`, `.list-cell`, `.text-field`. Ombre soft `dropshadow(...)` per `.card-elevated` e `.popover`.
- `css/theme-dark.css`: variante dark **non selezionabile** in Settings F3 (stub). Vedi stop point §7.3.
- `css/components.css`: classi specifiche board (`.board-cell-light`, `.board-cell-dark`, `.piece`, `.piece-king`, `.legal-target`, `.pulse-mandatory`, `.move-history-row`).
- `fonts/Inter-Regular.ttf`, `Inter-SemiBold.ttf`: file font caricati al bootstrap via `Font.loadFont(...)`.
- `app/ThemeService.java` (`@Component`): metodo `applyTheme(Scene scene)` che carica gli stylesheet in ordine corretto (`theme-light.css` poi `components.css`). Singleton bean.

**Test** (`ThemeServiceTest`): ~3 metodi (TestFX, ma minimi su Scene headless).

- `themeAppliesAllStylesheetsToScene`.
- `interactiveStatesAreDefinedForButton` (verifica via JavaFX CSS API che `.button:hover` ha regole).
- `interFontIsLoaded` (`Font.getFamilies().contains("Inter")`).

**Dipendenze**: Task 3.1.

---

### Task 3.3 — Localizzazione (ResourceBundle IT/EN, MessageSource, LocaleService)

**Output** (in `client/src/main/java/com/damaitaliana/client/i18n/` + `resources/`):

- `i18n/messages_it.properties` e `i18n/messages_en.properties`: chiavi minime di Task 3.1 (placeholder pronti per Task 3.4-3.18 che li popoleranno). Convenzione chiavi: `<screen>.<element>.<role>`, es. `menu.singleplayer.title`, `menu.singleplayer.subtitle`, `setup.level.principiante`, `setup.color.random`.
- `i18n/MessageSourceConfig.java` (`@Configuration`): `ReloadableResourceBundleMessageSource` con basename `classpath:i18n/messages`, encoding UTF-8, fallback locale `Locale.ITALIAN`.
- `i18n/LocaleService.java` (`@Component`): wraps `Locale` corrente (default da preferenza utente, vedi Task 3.4); espone `Locale current()`, `void switchTo(Locale newLocale)` (persiste preferenza, segnala via observer pattern).
- `i18n/I18n.java`: helper statico-style con istanza Spring (chiave + Object... params) per uso da FXML controller.

**Test** (`MessageSourceConfigTest`, `LocaleServiceTest`): ~5 metodi.

- `messageResolvesInItalianByDefault`.
- `messageResolvesInEnglishWhenLocaleSwitched`.
- `missingKeyReturnsBracketedKey` (per identificare facilmente stringhe non tradotte in dev).
- `localeServiceSwitchPersistsPreference` (mock di `PreferencesService` di Task 3.4).
- `bothBundlesHaveSameKeySet` (no chiave orfana in IT o EN).

**Dipendenze**: Task 3.1, **dichiara dipendenza forward** su Task 3.4 (`PreferencesService`) — risolta con bean injection lazy: l'ordine implementativo può essere 3.4 → 3.3 senza problemi.

---

### Task 3.4 — Preferences service (config.json)

**Output** (in `client/src/main/java/com/damaitaliana/client/persistence/`):

- `persistence/UserPreferences.java` (record): campi `Locale locale`, `String themeId` (default `"light"`), `int uiScalePercent` (default 100), `boolean firstLaunch`. Schema versionato (`int schemaVersion = 1`).
- `persistence/PreferencesService.java` (`@Component`): legge/scrive `~/.dama-italiana/config.json`. API: `UserPreferences load()`, `void save(UserPreferences prefs)`. Atomic write via `Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)`. Difensivo: se file mancante o malformato, log WARN e ritorna default.
- Path configurabile via `@Value("${dama.client.configFile:#{systemProperties['user.home']}/.dama-italiana/config.json}")`.

**Test** (`PreferencesServiceTest`): ~5 metodi.

- `loadReturnsDefaultsWhenFileMissing`.
- `saveThenLoadRoundTrips`.
- `loadFallsBackToDefaultsOnMalformedFile` + log assertion.
- `saveIsAtomic` (simula crash mid-write su tmp dir, verifica file target intatto).
- `unknownSchemaVersionFallsBackToDefaults` + log WARN.

**Vincoli**:

- **Nessun JWT cifrato** in F3 (deferred F5/6). Il record non ha campo `jwtTokenEncrypted` ancora.
- Jackson `ObjectMapper` configurato standard (no module aggiuntivi).

**Dipendenze**: Task 3.1.

---

### Task 3.5 — Splash screen

**Output** (in `client/src/main/java/com/damaitaliana/client/ui/splash/` + `resources/fxml/splash.fxml`):

- `splash.fxml`: VBox centrato con logo placeholder (icona Ikonli `mdi2c-checkerboard`), titolo "Dama Italiana", subtitle "Caricamento…", `ProgressIndicator` indeterminate.
- `SplashController.java` (`@Component @Scope("prototype")`): `@PostConstruct` non blocca; espone metodo `Task<Void> bootstrap()` che esegue in background:
  1. Caricamento font Inter (delegato a `ThemeService`).
  2. Carica `UserPreferences` (delegato a `PreferencesService`).
  3. `LocaleService.switchTo(prefs.locale())`.
  4. Verifica esistenza `_autosave.json` e setta flag su `SceneRouter` per il prompt al successivo passaggio in main menu.
  5. Sleep `Math.max(0, 1500 - elapsed)` ms (durata minima 1.5s SPEC §9.1).
- Al completamento: `SceneRouter.show(SceneId.MAIN_MENU)` o `SceneRouter.show(SceneId.MAIN_MENU_WITH_AUTOSAVE_PROMPT)`.

**Test** (`SplashControllerTest`): ~3 metodi (JavaFX threading via `Platform.runLater` + latch).

- `bootstrapCompletesWithinTwoSeconds`.
- `routesToMainMenuOnCompletion`.
- `setsAutosavePromptFlagWhenAutosaveExists`.

**Dipendenze**: Task 3.1, 3.2, 3.3, 3.4.

---

### Task 3.6 — Main menu (6 card, 3 disabilitate)

**Output** (in `client/src/main/java/com/damaitaliana/client/ui/menu/` + `resources/fxml/main-menu.fxml`):

- `main-menu.fxml`: GridPane 2x3 di 6 card. Ogni card è `VBox.card-elevated` con icona Ikonli, titolo, subtitle, button "Apri".
  1. **Single Player** (FR-SP-01) — abilitato.
  2. **LAN** — disabilitato con tooltip "Disponibile in Fase 7".
  3. **Online** — disabilitato con tooltip "Disponibile in Fase 6".
  4. **Regole** (FR-RUL-01) — abilitato.
  5. **Profilo** — non visibile (visibile solo se loggato, e in F3 nessuno è loggato).
  6. **Impostazioni** — abilitato.
- `MainMenuController.java`: gestisce click su card, naviga via `SceneRouter`.
- Chiavi i18n: `menu.singleplayer.title`, `menu.singleplayer.subtitle`, idem per le altre 5 card.
- Se `SceneRouter` ha flag `hasAutosavePrompt`, all'`@PostConstruct` mostra `Alert(CONFIRMATION)` "Riprendi partita interrotta?" (yes → carica autosave; no → cancella `_autosave.json`).

**Test** (`MainMenuControllerTest`): ~4 metodi.

- `clickSinglePlayerNavigatesToSetup`.
- `clickRulesNavigatesToRules`.
- `lanCardIsDisabled`.
- `autosavePromptShownWhenFlagSet`.

**Dipendenze**: Task 3.1, 3.2, 3.3, 3.5.

---

### Task 3.7 — Single-player setup modal

**Output** (in `client/src/main/java/com/damaitaliana/client/ui/setup/` + `resources/fxml/sp-setup.fxml`):

- `sp-setup.fxml`: modal con:
  - `ToggleGroup` livello difficoltà (Principiante / Esperto / Campione) — default Esperto.
  - `ToggleGroup` colore (Bianco / Nero / Casuale) — default Bianco.
  - `TextField` nome partita, default `"Partita del " + DateTimeFormatter.ofPattern("dd MMM HH:mm").format(LocalDateTime.now())` (localizzato).
  - Pulsanti `Annulla` / `Inizia partita`.
- `SinglePlayerSetupController.java`: validazione (nome non vuoto). Conferma → crea `SinglePlayerGame` (Task 3.8) e naviga a board view.
- `SinglePlayerGame` record/class (in `controller/`): `AiLevel level`, `Color humanColor`, `String name`, `GameState state`, `RandomGenerator rng`. Per `humanColor=RANDOM`, lancia `rng.nextBoolean()` al confirm.

**Test** (`SinglePlayerSetupTest`): ~4 metodi.

- `defaultValuesArePopulated`.
- `randomColorResolvesToWhiteOrBlack`.
- `emptyNameIsRejected`.
- `confirmCreatesGameAndNavigatesToBoard`.

**Dipendenze**: Task 3.1, 3.2, 3.3, 3.6.

---

### Task 3.8 — BoardRenderer + side panel skeleton

**Output** (in `client/src/main/java/com/damaitaliana/client/ui/board/`):

- `BoardRenderer.java`: componente JavaFX (estende `Region` o `Pane`). API:
  - `void renderState(Board board)`: ridisegna 64 case + pezzi presenti.
  - `void highlightLegalTargets(List<Square> targets)`: aggiunge style class `legal-target` alle case indicate.
  - `void clearHighlights()`.
  - `void highlightMandatorySources(List<Square> sources)`: style class `pulse-mandatory` + animazione `FillTransition` ciclica 800 ms (Task 3.10).
  - `WritableImage snapshot(int sizePx)`: per miniatura saves e diagrammi regole (FR-RUL-03).
- `BoardCellNode.java` (interno): nodo per casa, espone listener click; non è uno spinning JavaFX `Button` ma un `Region` custom.
- `PieceNode.java` (interno): rappresenta pedina/dama; `accessibleText` calcolato da posizione + tipo.
- `BoardViewController.java` (FXML controller, in `ui/board/board-view.fxml`): assembla `BoardRenderer` + side panel (placeholder per Task 3.11).
- `board-view.fxml`: BorderPane con `BoardRenderer` al centro, `VBox` laterale per cronologia + status (popolato in Task 3.11/3.12).

**Test** (`BoardRendererTest`): ~5 metodi (TestFX o headless).

- `initialPositionRendersTwentyFourPieces`.
- `highlightLegalTargetsAddsStyleClass`.
- `clearHighlightsRemovesAll`.
- `snapshotProducesNonEmptyImage`.
- `pieceAccessibleTextDescribesColorAndKind`.

**Dipendenze**: Task 3.1, 3.2, 3.7.

---

### Task 3.9 — Interazione board (click → highlight → applyMove)

**Output** (in `client/src/main/java/com/damaitaliana/client/controller/`):

- `controller/SinglePlayerController.java` (`@Component @Scope("prototype")`): orchestratore stato di gioco. Field `GameState state`, `RuleEngine ruleEngine`, `BoardRenderer renderer`, `MoveHistoryViewModel history`.
  - `void onCellClicked(Square s)`:
    1. Se nessun pezzo selezionato e `s` ha pezzo del lato a muovere → seleziona, calcola `legalMoves` filtrate sul pezzo, evidenzia targets.
    2. Se pezzo selezionato e `s` è un target legale → costruisce la `Move` corrispondente, chiama `applyMoveAndAnimate(move)`.
    3. Altrimenti deseleziona.
  - `applyMoveAndAnimate(Move move)`: anima (Task 3.10), applica `RuleEngine.applyMove`, aggiorna stato, autosave (Task 3.16), ricalcola mandatory captures, se `state.status().isOngoing()` e `state.sideToMove()` è dell'IA → schedula AI (Task 3.13).
- `MoveSelector.java`: helper puro che, dato `state` + `selectedSquare`, ritorna `List<Move>` legali da quella casa **dopo** filtraggio leggi italiane (riusa `RuleEngine.legalMoves(state)` e filtra per `move.from()`).

**Test** (`SinglePlayerControllerTest`, `MoveSelectorTest`): ~10 metodi.

- `clickOnOwnPieceShowsLegalTargets`.
- `clickOnEmptyCellDoesNothing`.
- `clickOnOpponentPieceDeselectsCurrent`.
- `clickOnLegalTargetAppliesMove`.
- `clickOnIllegalTargetDoesNotApply`.
- `mandatoryCaptureRestrictsLegalMoves` (legge della quantità).
- `manCannotSelectKingTarget` (FR-SP-... AC §17.1.8).
- `selectorReturnsOnlyMovesFromGivenSquare`.
- `applyMoveTriggersAutosave` (mock SaveService).
- `terminalStateStopsControllerLoop`.

**Dipendenze**: Task 3.8, **forward** Task 3.10 + 3.13 + 3.16 (mock-ate nei test).

---

### Task 3.10 — Animation engine

**Output** (in `client/src/main/java/com/damaitaliana/client/ui/board/animation/`):

- `MoveAnimator.java`:
  - `Animation slideMove(Square from, Square to, Duration d)`: `TranslateTransition` 250 ms, `Interpolator.OUT_QUAD`.
  - `Animation fadeCapture(Square s)`: `ParallelTransition(ScaleTransition 200 ms, FadeTransition 200 ms)`.
  - `Animation promotion(Square s)`: `RotateTransition 500 ms` su Y axis + flash dorato (overlay con `FillTransition`).
  - `Animation pulseMandatory(Square s)`: `FillTransition` ciclica 800 ms (autoreverse).
- I parametri (durate, interpolatori, colori flash) sono `final static` da SPEC §13.3.
- `AnimationOrchestrator.java`: compone in sequenza per una `Move` complessa (es. `CaptureSequence`: per ogni step `slideMove` + `fadeCapture`; se promozione finale → `promotion`).

**Test** (`MoveAnimatorTest`): ~4 metodi (verificano durate, interpolator, target node).

- `slideMoveProducesTransitionWithExpectedDuration`.
- `captureSequenceProducesParallelOfNAnimations`.
- `promotionAnimationIncludesFlashAndRotate`.
- `pulseMandatoryIsCyclic`.

**Vincoli**:

- Animazioni **mai blocking** sul JavaFX thread.
- Test verificano la struttura dell'`Animation` (durata, interpolatore) senza `play()` — più veloce e robusto.

**Dipendenze**: Task 3.8.

---

### Task 3.11 — Move history pane (cronologia FID)

**Output** (in `client/src/main/java/com/damaitaliana/client/ui/board/`):

- `MoveHistoryView.java`: ListView<MoveHistoryRow> dove `MoveHistoryRow` è un record `(int moveNumber, String whiteFid, Optional<String> blackFid)`. Cell factory custom per layout colonnare.
- `MoveHistoryViewModel.java`: ObservableList di righe; metodo `appendMove(Move m, Color color)` che usa `FidNotation.format(m)` per la stringa.
- Integrazione in `board-view.fxml` (Task 3.8 placeholder ora popolato).

**Test** (`MoveHistoryViewModelTest`): ~4 metodi.

- `appendingWhiteMoveCreatesNewRow`.
- `appendingBlackMoveCompletesRow`.
- `multipleTurnsProduceMultipleRows`.
- `formatsCaptureSequenceWithCrossNotation`.

**Dipendenze**: Task 3.8.

---

### Task 3.12 — Status pane (giocatori, side-to-move, esito)

**Output** (in `client/src/main/java/com/damaitaliana/client/ui/board/`):

- `StatusPaneController.java`: visualizza nome umano, livello IA, colore di ciascuno, indicatore "tocca al Bianco/Nero" (chip colorato), e — a fine partita — banner con `GameStatus` localizzato (es. "DRAW_REPETITION" → "Patta per triplice ripetizione").
- Chiavi i18n: `status.turn.white`, `status.turn.black`, `status.endgame.<status>`.
- Reagisce a `state` change via observer.

**Test** (`StatusPaneControllerTest`): ~4 metodi.

- `displaysHumanAndAiNames`.
- `turnIndicatorReflectsSideToMove`.
- `endgameBannerShownOnTerminalState`.
- `allSixGameStatusValuesHaveLocalizedString` (parametrizzato sui 6 valori di `GameStatus`).

**Dipendenze**: Task 3.3, 3.8.

---

### Task 3.13 — Integrazione IA (async, virtual thread)

**Output** (in `client/src/main/java/com/damaitaliana/client/controller/`):

- `controller/AiTurnService.java` (`@Component`): wraps `VirtualThreadAiExecutor`.
  - `CompletableFuture<Move> requestMove(GameState state, AiLevel level, RandomGenerator rng)`: crea `AiEngine.forLevel(level, rng)`, chiama `executor.submitChooseMove(ai, state, level.defaultTimeout())`, espone come `CompletableFuture` (wrapping `Submission`).
  - Espone `AiThinkingState` (`StringProperty` localizzato + `BooleanProperty thinking`) per binding UI ("L'IA sta pensando…").
- Integrazione in `SinglePlayerController` (Task 3.9):
  - Quando `state.sideToMove()` è dell'IA, mostra status "thinking", chiama `AiTurnService.requestMove`, su completamento applica la mossa con animazione, autosave, si rimette in attesa input umano.
  - Cancellazione: pulsante "Pausa" o uscita dalla schermata → `submission.cancelGracefully()`.
- **Non bloccante**: tutto su `Platform.runLater` per gli aggiornamenti UI.

**Test** (`AiTurnServiceTest`): ~5 metodi.

- `requestMoveReturnsLegalMoveAtPrincipiante` (test rapido, deterministico con seed).
- `cancellationStopsRequest`.
- `thinkingStateTogglesAroundCall`.
- `terminalStateRequestReturnsNullMove` (o documenta `IllegalStateException`).
- `parallelRequestsAreIndependent`.

**Vincoli**:

- **Riuso totale** di Fase 2: niente logica IA nel client.
- I default timeout sono in `AiLevel`/`AiEngine` constants di F2 (es. `CampioneAi.DEFAULT_TIMEOUT`).

**Dipendenze**: Task 3.9.

---

### Task 3.14 — Save model + SaveService

**Output** (in `client/src/main/java/com/damaitaliana/client/persistence/`):

- `persistence/SavedGame.java` (record): `int schemaVersion`, `String kind` (=`"SINGLE_PLAYER_GAME"`), `String name`, `Instant createdAt`, `Instant updatedAt`, `AiLevel aiLevel`, `Color humanColor`, `List<String> moves` (FID notation), `SerializedGameState currentState` (record annidato con board JSON, sideToMove, halfmoveClock, history opzionale).
- `persistence/SerializedGameState.java`: rappresentazione Jackson-friendly, riusa `RuleEngineCorpus`-style schema da F1 per il board (4 liste disgiunte di FID 1-32).
- `persistence/SaveService.java` (`@Component`):
  - `Path savesDir()`: `~/.dama-italiana/saves/` (override via `application.yml`).
  - `void save(String slot, SavedGame data)`: write atomico (tmp + ATOMIC_MOVE).
  - `Optional<SavedGame> load(String slot)`: leggi + valida `schemaVersion` (rifiuta se != 1).
  - `List<SaveSlotMetadata> listSlots()`: ordinabile.
  - `void delete(String slot)`.
  - `String slugify(String name)`: trasforma "Partita 12 aprile" → "partita-12-aprile" per filename.
  - Slot speciale `"_autosave"`: stesso meccanismo, riservato.
- `SaveSlotMetadata` (record): `slot`, `name`, `aiLevel`, `humanColor`, `updatedAt`, `currentMoveNumber`.

**Test** (`SaveServiceTest`): ~10 metodi.

- `saveThenLoadRoundTrips`.
- `listSlotsReturnsAllSavedGames`.
- `deleteRemovesSlot`.
- `loadReturnsEmptyWhenSlotMissing`.
- `loadFailsOnUnknownSchemaVersion`.
- `saveIsAtomic` (simulazione interrupt mid-write).
- `slugifyIsIdempotent`.
- `slugifyHandlesUnicodeAndSpaces`.
- `concurrentSavesToDifferentSlotsAreSafe`.
- `autosaveSlotIsTreatedAsRegularSlotForLoadList` (no, `listSlots` lo esclude — verifica).

**Dipendenze**: Task 3.1, 3.4.

---

### Task 3.15 — Save dialog + Load screen

**Output** (in `client/src/main/java/com/damaitaliana/client/ui/save/`):

- `save-dialog.fxml`: modal con `TextField` nome (precompilato dal `SinglePlayerGame.name`), pulsanti Salva/Annulla.
- `SaveDialogController.java`: chiama `SaveService.save(slug, ...)`, toast di conferma (Task 3.20 / inline).
- `load-screen.fxml`: `TableView<SaveSlotMetadata>` con colonne Nome, Data, Livello IA, Colore, Mossa corrente, **Miniatura** (cell factory che usa `BoardRenderer.snapshot()`). Pulsanti Carica/Elimina/Indietro. Sortable per ogni colonna.
- `LoadScreenController.java`: gestisce selezione, conferma "Carica `<nome>`?", deserializzazione → naviga a board view nel nuovo stato.
- Integrazione menu in board view: `MenuBar` con "Partita" → "Salva con nome" (apre save dialog), "Carica" (naviga a load screen), "Termina partita" (cancella autosave + naviga a main menu).

**Test** (`SaveDialogTest`, `LoadScreenTest`): ~6 metodi.

- `saveDialogPersistsWithEnteredName`.
- `loadScreenListstAllSaves`.
- `clickRowAndLoadResumesAtState`.
- `deleteRemovesRow`.
- `terminateGameClearsAutosaveAndReturnsToMenu`.
- `miniatureRendersWithoutErrors`.

**Dipendenze**: Task 3.8, 3.14.

---

### Task 3.16 — Autosave + recovery prompt

**Output** (in `client/src/main/java/com/damaitaliana/client/persistence/`):

- `AutosaveService.java` (`@Component`): wrap di `SaveService` ma vincolato allo slot `"_autosave"`.
  - `void writeAutosave(SinglePlayerGame game)`: serializza + atomic write.
  - `Optional<SinglePlayerGame> readAutosave()`: deserializza.
  - `void clearAutosave()`: cancella file.
  - `boolean autosaveExists()`.
- Hook in `SinglePlayerController.applyMoveAndAnimate` (Task 3.9): **dopo** ogni `applyMove`, chiama `autosaveService.writeAutosave(currentGame)`.
- Hook in `SplashController` (Task 3.5): se `autosaveExists()`, set flag su `SceneRouter`; main menu lo intercetta (Task 3.6).
- Hook in `MainMenuController` (Task 3.6): on autosave prompt, "yes" → carica + naviga a board view; "no" → `clearAutosave()` + main menu normale.
- Hook in "Termina partita" (Task 3.15): `clearAutosave()`.

**Test** (`AutosaveServiceTest`, `AutosaveE2ETest`): ~5 metodi.

- `writeAutosaveOverwritesExisting`.
- `readReturnsEmptyWhenAbsent`.
- `clearRemovesFile`.
- `e2e_promptOnRestartWhenAutosavePresent` (A3.5).
- `e2e_clearOnTerminate`.

**Dipendenze**: Task 3.9, 3.14.

---

### Task 3.17 — Settings screen (lingua, scaling UI)

**Output** (in `client/src/main/java/com/damaitaliana/client/ui/settings/` + `resources/fxml/settings.fxml`):

- `settings.fxml`: VBox con sezioni:
  - **Lingua**: `ChoiceBox` Italiano/English. Cambio richiede riavvio (mostra `Alert(INFORMATION)` "Riavvia il client per applicare la nuova lingua"). Vedi stop point §7.10 per cambio runtime.
  - **Scaling UI**: `ToggleGroup` 100% / 125% / 150%. Applicato runtime via `scene.getRoot().setStyle("-fx-font-size: ...")` o tramite CSS class.
  - **Tema**: stub disabilitato con label "Light (dark mode disponibile in Fase 11)". Vedi stop point §7.3.
  - Pulsante "Salva" → `PreferencesService.save`.
  - Pulsante "Indietro".
- `SettingsController.java`.

**Test** (`SettingsControllerTest`): ~3 metodi.

- `localeChangePersistsAndPromptsRestart`.
- `scalingChangeAppliesImmediately`.
- `themeOptionIsDisabledInF3`.

**Dipendenze**: Task 3.3, 3.4, 3.6.

---

### Task 3.18 — Rules screen (sezioni statiche)

**Output** (in `client/src/main/java/com/damaitaliana/client/ui/rules/` + `resources/fxml/rules.fxml`):

- `rules.fxml`: SplitPane con:
  - Sinistra: `ListView` di sezioni — Setup, Movimento, Cattura, Leggi di precedenza, Promozione, Fine partita, Notazione (chiavi i18n `rules.section.<id>.title`).
  - Destra: `ScrollPane` con contenuto della sezione selezionata: titolo + testo localizzato (chiavi `rules.section.<id>.body` con paragrafi separati da `\n\n`).
- `RulesController.java`: gestisce selezione sezione, monta il contenuto.
- **Diagrammi statici** (FR-RUL-03): per le sezioni `setup`, `movimento`, `cattura`, `promozione`, una `ImageView` ricavata da `BoardRenderer.snapshot(...)` di una posizione hardcoded (file JSON in `resources/rules/<sezione>.json` con board statica + caption).
- Almeno 5 diagrammi totali distribuiti su 4 sezioni.
- Integrazione: card "Regole" da main menu + icona ⓘ in board view.

**Test** (`RulesControllerTest`): ~4 metodi.

- `defaultSectionIsSetup`.
- `clickingSectionUpdatesContent`.
- `captureSectionRendersDiagram`.
- `allSevenSectionsHaveLocalizedTitleAndBody`.

**Dipendenze**: Task 3.3, 3.6, 3.8.

---

### Task 3.19 — Mini-animazioni regole (opzionale per FR-RUL-04)

**Output** (in `client/src/main/java/com/damaitaliana/client/ui/rules/`):

- `RulesAnimations.java`: 3 animazioni dimostrative riusando `MoveAnimator`:
  1. **Cattura semplice**: pedina W in 19 → cattura pedina B in 23 → atterra in 26.
  2. **Presa multipla**: applicazione di una `CaptureSequence` ricavata da una posizione del corpus F1.
  3. **Promozione**: pedina W in 25 → muove in 29 → animazione promotion.
- Pulsante "Riproduci" sotto ogni animazione, controllato da `RulesController` (Task 3.18) — solo per le sezioni `cattura` e `promozione`.
- Tutto puramente decorativo, nessun input utente sulla board.

**Test** (`RulesAnimationsTest`): ~3 metodi.

- `simpleCapturePlaysWithoutErrors`.
- `multipleCapturePlaysSequentially`.
- `promotionAnimationCompletesIn500ms`.

**Stop point §7.6**: in fase di REVIEW si può proporre lo skip di questo task per snellire la fase. Default proposta: **incluso** (il refactor è minimo e rispetta SPEC).

**Dipendenze**: Task 3.10, 3.18.

---

### Task 3.20 — Accessibility (accessibleText, keyboard nav, scaling)

**Output** (sparsi nei controller esistenti):

- `accessibleText` su:
  - Pezzi del board (`PieceNode`) — già parzialmente in Task 3.8.
  - Case del board (es. "Casa scura 19, vuota" / "Casa scura 19, pedina bianca").
  - Pulsanti delle card menu, pulsanti di setup, save dialog, rules.
- Keyboard navigation:
  - Main menu: focus traversal con `Tab`/`Shift-Tab`, `Enter` per attivare card.
  - Board: frecce per spostare focus fra case scure (mappato su FID 1-32 in ordine), `Enter` per selezionare/giocare la mossa, `Esc` per deselezionare.
  - Settings, save/load: standard JavaFX a11y.
- Scaling 100/125/150% applicato globalmente come CSS variable o root style.
- Toast/notifiche: `accessibleRoleDescription="Notification"`.

**Test** (`AccessibilityTest`): ~5 metodi.

- `pieceAccessibleTextDescribesPosition` (parametrizzato sui 4 tipi: vuota, MAN W/B, KING W/B).
- `tabTraversalReachesAllMenuCards`.
- `arrowKeysMoveBoardFocus`.
- `enterOnFocusedSourceSelectsAndShowsTargets`.
- `scalingAppliesToAllScenes`.

**Dipendenze**: tutti i task UI precedenti (3.5-3.18).

---

### Task 3.21 — TestFX E2E single-player

**Output** (in `client/src/test/java/com/damaitaliana/client/`):

- `SinglePlayerE2ETest`:
  - `completesGameVsEsperto` (A3.3): nuova partita Esperto, lato umano controllato dal test che gioca mosse via `RuleEngine.legalMoves` → applica → finché terminale. Verifica nessuna eccezione, status terminale raggiunto, banner mostrato. **Marcato `@Tag("slow")`** se durata > 30s.
  - `manCannotCaptureKingInUI` (A3.11, AC §17.1.8): caricamento posizione preparata (helper `loadStateFromCorpus("rule-quality-001")`), simula click su pedina umana, verifica che la dama avversaria non sia fra i `legal-target`.
  - `promotionStopsSequenceInUI` (A3.12, AC §17.1.7): posizione con presa multipla che attraversa la riga di promozione, verifica che la sequenza si fermi visibilmente alla promozione e il turno passi.
- `SaveLoadE2ETest`:
  - `saveThenLoadResumesAtSameState` (A3.4): nuova partita, alcune mosse, salva con nome, simula nuova istanza Application, naviga a Carica, click su slot, verifica `currentState`.
- `AutosaveE2ETest`:
  - `promptOnRestartWhenAutosavePresent` (A3.5).
- `LocalizationTest`:
  - tutti i nodi di testo dei principali stage hanno una key risolvibile in IT e EN (verifica via grep + `MessageSource.getMessage`).
- `RulesScreenTest`:
  - `openRulesAndNavigateSections` (A3.10).

**Vincoli TestFX**:

- Headless mode supportato (`-Dtestfx.headless=true -Dprism.order=sw`).
- Setup standard `ApplicationExtension` per JUnit 5.

**Dipendenze**: tutti i task precedenti.

---

### Task 3.22 — Coverage gate JaCoCo client + cleanup

**Output**:

- `client/pom.xml`: configurazione JaCoCo `haltOnFailure=true` con regola:
  - `BUNDLE` (escluse classi `**/ui/view/*` e FXML controller di rendering puro): ≥ 60% line + branch (NFR-M-02).
  - Esclusioni esplicite: `**/JavaFxApp.class`, `**/*Application.class` (bootstrap), eventuali `**/*Renderer$*` lambda di animazioni.
- Verifica `mvn -pl client verify` rispetta il gate.
- Eventuale `<excludes>` per classi puramente `.fxml`-driven dove la coverage è artificiosa.
- Rimozione `ClientSmokeTest` se non già rimosso in Task 3.1 (CLAUDE.md §6.1).

**Test**: nessun nuovo test prodotto qui; solo gate.

**Dipendenze**: Task 3.21.

---

### Task 3.23 — Documentazione, ADR, TRACEABILITY, README, CHANGELOG

**Output**:

- `client/src/main/java/.../package-info.java`:
  - `com.damaitaliana.client.app`
  - `com.damaitaliana.client.ui` (+ sotto-package `menu`, `setup`, `board`, `save`, `settings`, `rules`)
  - `com.damaitaliana.client.controller`
  - `com.damaitaliana.client.persistence`
  - `com.damaitaliana.client.i18n`
- `ARCHITECTURE.md`: ADR-029, 030, 031, 032, 033 (proposti in §3.4 sopra). Eventuale ADR-034 per scelte runtime emerse durante l'IMPLEMENTA.
- `tests/TRACEABILITY.md`: righe per FR-SP-01÷09, FR-RUL-01÷05, NFR-U-01, NFR-U-04 (light), NFR-M-02 client, AC §17.1.1 (chiusura), §17.1.7, §17.1.8, §17.1.9, §17.1.12 + AC F3 (A3.1 ÷ A3.21).
- `CHANGELOG.md`: voce in `[Unreleased]` con riepilogo F3.
- `AI_CONTEXT.md`: stato avanzato a "Fase 3 — IMPLEMENTA completa, REVIEW pending".
- `README.md`: sezione "Eseguire il client" (`mvn -pl client javafx:run`), nota sui salvataggi in `~/.dama-italiana/`.

**Dipendenze**: Task 3.22.

---

## 5. Strategia di test (Fase 3)

Riferimento: CLAUDE.md §2.4.

| Tipo                                        | Numero indicativo | Tooling                          | Cosa testa                                                                                                          |
|---------------------------------------------|------------------:|----------------------------------|---------------------------------------------------------------------------------------------------------------------|
| **Unit (servizi non-UI)**                   | ~30               | JUnit 5 + AssertJ + Mockito      | `PreferencesService`, `SaveService`, `AutosaveService`, `LocaleService`, `SinglePlayerGame`, `MoveSelector`, `MoveHistoryViewModel`. |
| **Unit (controller con dipendenze mock)**   | ~25               | JUnit 5 + Mockito                | `SinglePlayerController`, `MainMenuController`, `SettingsController`, `RulesController`, `AiTurnService`.            |
| **Component (TestFX su singolo stage)**     | ~15               | TestFX 4 + JUnit 5               | Splash, BoardRenderer interactions, Save dialog, Load screen, Rules screen.                                          |
| **E2E (TestFX, full app instance)**          | ~6                | TestFX + Spring Boot Test        | `SinglePlayerE2ETest`, `SaveLoadE2ETest`, `AutosaveE2ETest`, `LocalizationTest`, `RulesScreenTest`, `BoardInteractionRulesTest`. |
| **Animation structure**                     | ~4                | JUnit 5                          | `MoveAnimator`: durate, interpolatori, parallelizzazione.                                                            |
| **Localization grep**                       | 1 (script)        | grep / scripted test             | nessuna stringa hardcoded `setText("xxx")` in main/java sfugge a `MessageSource`.                                    |
| **Regression F1 (corpus 48 posizioni)**     | 48 (ereditati)    | JUnit 5 (Fase 1)                 | Rerun, garantisce che F3 non rompa il `RuleEngine`.                                                                  |
| **Regression F2 (gating IA)**               | 1 `@Tag("slow")` (ereditato) | JUnit 5             | `AiTournamentSimulationTest` ancora ≥ 95/100 (rerun on demand).                                                      |

**Coverage target Fase 3**:

| Scope                                          | Soglia minima (gate)                            | Target operativo |
|------------------------------------------------|-------------------------------------------------|------------------|
| `shared` (intero modulo)                       | **90%** line + branch (gate F1+F2 mantenuto)    | 92-95%           |
| `core-server`                                  | nessun gate ancora (F4)                         | smoke             |
| `client` (pkg non-view, vedi §7.8)             | **60%** line + branch (NFR-M-02)                | 65-75%           |
| `server`                                       | nessun gate ancora (F5)                         | smoke             |

**Naming convention** (CLAUDE.md §2.4.5):

- Unit: `<ClasseProduzione>Test`. Stile: `should<Espressione>_when<Condizione>` o nome descrittivo (es. `displaysMovesInFidNotation`).
- E2E TestFX: `<Feature>E2ETest`.

**Tag JUnit**:

- `@Tag("slow")` per `SinglePlayerE2ETest#completesGameVsEsperto` se la durata sale sopra 30s (riservato a CI o fast loop selettivo). Default: untagged.
- `@Tag("performance")` non aggiunto in F3 (perf misurata in F11).
- `mvn verify` esegue **tutto** di default. Fast loop locale: `mvn -pl client test -DexcludedGroups=slow`.

---

## 6. Rischi e mitigazioni

| ID    | Rischio                                                                                                                                | P    | I     | Mitigazione |
|-------|----------------------------------------------------------------------------------------------------------------------------------------|------|-------|-------------|
| R-01  | **TestFX su CI o headless instabile** (Prism software renderer, ritardi, flaky test).                                                  | Media | Alto | Configurare `-Dprism.order=sw -Dtestfx.headless=true`. Test E2E con `WaitForAsyncUtils.waitForFxEvents()` esplicito invece di `Thread.sleep`. Documentare requisito JDK + display server (Xvfb su Linux CI). |
| R-02  | **Spring Boot bootstrap rallenta JavaFX** (target SPEC §13: splash 1-2s).                                                              | Bassa | Medio | Misura concreta: Spring `application.yml` minimal + `spring.main.lazy-initialization=true` per beans non-critici. Splash garantisce minimo 1.5s percepito anche se boot più rapido. |
| R-03  | **Animazioni JavaFX su HW non accelerato cadono sotto 60 FPS** (NFR-P-01).                                                             | Bassa | Medio | Animazioni usano primitive standard (TranslateTransition ecc.) HW-accel by default. NFR-P-01 misurato formalmente in F11. In F3 verifica visiva. |
| R-04  | **i18n drift**: chiavi aggiunte in IT non in EN o viceversa.                                                                            | Alta  | Basso | Test `bothBundlesHaveSameKeySet` esegue diff fra i due `.properties`. Fail in CI se diverso. |
| R-05  | **Save format breaking change** durante F3 (cambio campo metà fase).                                                                   | Media | Medio | `schemaVersion=1` definito in Task 3.14, congelato per F3. Eventuali cambi → `schemaVersion=2` + migration loader. Documentato in ADR-031. |
| R-06  | **Autosave I/O su disco lento blocca UI thread**.                                                                                       | Media | Medio | `AutosaveService.writeAutosave` eseguito in `CompletableFuture.runAsync(...)` su executor dedicato (non virtual thread, per evitare contention con AI). Test verifica che la mossa successiva non aspetti l'autosave. |
| R-07  | **Cancellazione AI durante chiusura app lascia thread orfano**.                                                                         | Bassa | Medio | `JavaFxApp.stop()` chiama `VirtualThreadAiExecutor.close()`. Test in `ClientBootstrapTest`. |
| R-08  | **FXML controller factory non risolve bean Spring** (problema classico JavaFX + Spring).                                                | Media | Alto | Pattern noto: `FXMLLoader.setControllerFactory(applicationContext::getBean)`. Wrap in `FxmlLoaderFactory` (Task 3.1/3.2) verificato da test isolato. |
| R-09  | **Coverage 60% client difficile da raggiungere** se troppi controller sottili senza logica.                                            | Media | Medio | Esclusione mirata dei FXML controller di puro rendering (vedi §7.8). Logica di stato concentrata in service e controller "fat" testabili. |
| R-10  | **Memory leak da listener JavaFX** (binding non rimossi al cambio scena).                                                              | Media | Medio | `SceneRouter.show()` chiama `controller.onUnload()` se implementa `Detachable` interface. Test `MemoryLeakTest` su 100 cicli show→hide (deferred F11 se troppo costoso in F3). |
| R-11  | **Filename slug per saves perde informazione** (due partite con stesso slug → collisione).                                              | Media | Medio | `slugify` aggiunge suffix `-2`, `-3` se collisione. Test dedicato. |
| R-12  | **Diagrammi regole hardcoded non riflettono il `RuleEngine`** (drift di significato).                                                   | Bassa | Basso | I diagrammi sono solo posizioni (board state). Loaders dei JSON regole riusa lo stesso parser delle 48 posizioni del corpus F1 (che già passa per il `RuleEngine`). |
| R-13  | **Localizzazione runtime non funziona senza riavvio** (NFR-U-01 ambiguo).                                                               | Media | Basso | F3 richiede **riavvio** per cambiare lingua (documentato come limitation). Cambio runtime senza riavvio è feature di Fase 11. Vedi stop point §7.10. |
| R-14  | **Single-player end-to-end vs Campione troppo lento per CI** (mosse 5s × decine).                                                      | Media | Medio | `SinglePlayerE2ETest#completesGameVsEsperto` usa Esperto (≤2s/mossa). Vs Campione opzionale `@Tag("slow")`. |
| R-15  | **Drag-and-drop sui pezzi richiesto?** SPEC §13 non lo specifica esplicitamente; click-to-select è il flusso §9.2.1.                   | Bassa | Basso | F3 implementa **solo click-to-select** (più semplice + accessibile). Drag-and-drop aggiungibile in F11 se richiesto, senza breaking change. |
| R-16  | **Undo/redo con IA in mezzo**: undo riporta a uno stato dell'IA o dell'umano?                                                          | Media | Medio | Stop point §7.13. Proposta: undo è "torna al mio ultimo turno" (annulla mossa umana + risposta IA come unità). |

---

## 7. Stop point e decisioni che richiedono utente

Sotto-fase PIANIFICA — punti che richiedono chiarimento prima di IMPLEMENTA.

### 7.1 Bootstrap JavaFX + Spring Boot DI (ADR-029)

| Opzione | Descrizione | Trade-off |
|---|---|---|
| **A** (proposta) | `ClientApplication` (`@SpringBootApplication`, `WebApplicationType.NONE`) avvia Spring; salva il context in `JavaFxAppContextHolder` static; poi chiama `Application.launch(JavaFxApp.class)`. `JavaFxApp.start()` recupera il context dal holder. | Pattern documentato nella community, zero dipendenze esterne, Spring beans visibili a JavaFX. Minor "wart" del singleton statico ma è confinato al bootstrap. |
| B | Libreria di terze parti (`springboot-javafx-support` di Roskenet o simili). | Aggiunge dipendenza non in SPEC §6.4. Inutile per il caso semplice. |
| C | `spring-context` puro (senza Spring Boot starter). | Perdo `application.yml`, `@ConfigurationProperties`, profili. SPEC §6.4 dice esplicitamente Spring Boot starter non-web. Da escludere. |

**Proposta**: A.

### 7.2 FXML vs UI programmatica (ADR-030)

| Opzione | Descrizione | Trade-off |
|---|---|---|
| **A** (proposta) | FXML per il **layout** delle 6+ schermate (splash, main menu, setup, board view, settings, rules, save dialog, load screen). Programmatico solo per componenti dinamici (BoardRenderer, MoveHistoryView). | Separazione layout/logic, hot-reload con SceneBuilder, controller iniettati via `setControllerFactory(context::getBean)`. Standard JavaFX. |
| B | Tutto programmatico Kotlin-style con builder fluenti. | Più verboso in Java; perdo il SceneBuilder; meno leggibile per future review. |
| C | FXML per tutte le schermate **inclusa** la board view. | Forzare la board (canvas dinamico) in FXML è artificioso. La board è un componente custom che merita codice. |

**Proposta**: A.

### 7.3 Strategia tema dark in Fase 3 (NFR-U-02)

SPEC §13.2 definisce `.root.dark` con i token dark; SPEC §16 elenca "Dark mode con toggle runtime" come task di Fase 11. Questione: cosa fare in F3?

| Opzione | Descrizione | Trade-off |
|---|---|---|
| **A** (proposta) | F3 implementa `theme-light.css` completo + `theme-dark.css` come **stub minimo presente** ma non selezionabile in Settings (label "disponibile in Fase 11"). I token dark di SPEC §13.2 non sono ancora mappati in tutto. | Light pulito e WCAG AA verificato. Zero rischio drift dark. F11 implementa dark + toggle in un colpo solo con tutta l'attenzione che merita. |
| B | F3 implementa entrambi i temi e il toggle in Settings funziona già. | Anticipa F11. Rischio: dark mode non WCAG AA verificato → bug visibili. F3 si gonfia. |
| C | Solo light, dark non incluso neanche come stub. | Coerente con SPEC F3. Costa una piccola riscrittura di Settings in F11 per aggiungere il toggle. |

**Proposta**: A. Il file `theme-dark.css` esiste come "draft" nei resources ma non è caricato; il selettore tema in Settings esiste come `disabled`.

### 7.4 Schema file salvataggi (ADR-031)

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | `schemaVersion=1`, JSON Jackson, board come 4 liste FID disgiunte (riuso schema F1 corpus), `moves` lista di stringhe FID, `currentState` con board+sideToMove+halfmoveClock+history (history ridotta agli ultimi N stati per la triplice ripetizione). | Riuso totale del parser F1, niente codice nuovo. Schema compatto. |
| B | Schema "ricostruibile": salva solo `moves`, ricalcola `currentState` da `Board.initial() + replay`. Niente `currentState` materializzato. | Minimalista ma costa replay ad ogni load. La triplice ripetizione (ADR-021) replay-based è già O(n²) → non aggravare. |
| C | Schema con ridondanza: `currentState` materializzato **e** `moves` (entrambi). Verifica di coerenza al load. | Robusto a corruption parziale ma duplica dati. F3 è single-player single-user, non serve. |

**Proposta**: A. Il `currentState` materializzato evita replay ad ogni load; la lista `moves` è "vista cronologia" (UI-friendly) e per replay viewer futuro (F11 — al momento ridondante ma economica).

### 7.5 Atomicità autosave (ADR-032)

**Proposta**: scrittura `_autosave.json.tmp` poi `Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)`. Su filesystem che non supportano `ATOMIC_MOVE` (rari su Win/macOS/Linux moderni), fallback a `REPLACE_EXISTING` semplice con log WARN.

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | tmp + ATOMIC_MOVE + REPLACE_EXISTING. Eccezione → log + retry una volta + fail graceful (toast utente). | Standard, robusto. |
| B | fsync + rename. | Extra overhead non giustificato per single-player. |
| C | Scrittura diretta. | Rischio file corrotto se kill mid-write. |

**Proposta**: A.

### 7.6 Mini-animazioni regole (FR-RUL-04, Task 3.19)

SPEC §4.6 qualifica le mini-animazioni come "opzionali".

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Incluse in F3 come Task 3.19. 3 animazioni (cattura semplice, presa multipla, promozione) riusando `MoveAnimator`. | F3 risulta più "completa" alla acceptance demo. Task contenuto. |
| B | Rinviate a F11 ("animazioni avanzate"). Sezione regole solo statica in F3. | F3 più snella. Ma le animazioni esistono già (Task 3.10), riuso minimo. |

**Proposta**: A. Tuttavia, se durante l'IMPLEMENTA la fase si gonfia, Task 3.19 è il primo candidato a slip per non bloccare la chiusura.

### 7.7 Audio in Fase 3

SPEC §13.4 lista suoni discreti; SPEC §16 Fase 11 li raggruppa con "animazioni avanzate".

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Audio **escluso** da F3. Niente file audio nei resources, niente API placeholder. Si aggiunge in F11. | Coerente con roadmap. Riduce rischi (licensing audio, sourcing asset). |
| B | API stub `SoundService` con metodi vuoti, da popolare in F11. | Zero beneficio, +rumore. |

**Proposta**: A.

### 7.8 Coverage gate package `client` (NFR-M-02)

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | 60% line + branch sul bundle `client`, **escludendo** classi puramente di rendering FXML (`com.damaitaliana.client.ui.**.fxml-*` e i FXML controller con < 30 LOC che fanno solo binding). Esclusioni esplicite (whitelist). | Realistico. La logica testabile è in service/controller "fat" — quelli sono dentro il gate. |
| B | 60% senza esclusioni. | Difficile (controller FXML thin gonfiano la denominator). |
| C | Allineato al modulo `shared` (90%). | Irrealistico per UI. NFR-M-02 dice esplicitamente 60% per UI. |

**Proposta**: A. L'elenco preciso delle esclusioni è definito a chiusura Task 3.22 e tracciato nel POM con commento.

### 7.9 Granularità chiavi i18n (ADR-033)

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Hierarchica con `.` (es. `menu.singleplayer.title`, `setup.level.principiante`). Bundle unico per tutta l'app. | Standard, semplice. |
| B | Bundle separati per schermata (`menu.properties`, `setup.properties`, ...). | Overhead: gestione N MessageSource, possibili duplicazioni. |
| C | Codici opachi numerici (`menu.001`, `menu.002`). | Anti-pattern noto. Da escludere. |

**Proposta**: A.

### 7.10 Cambio lingua runtime vs riavvio

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Riavvio richiesto (mostra alert "Riavvia per applicare"). Cambio runtime in F11. | Semplice, robusto. Utenti accettano per "una volta". |
| B | Cambio runtime: tutti i testi rebindati on-the-fly. | Ogni schermata deve esporre `refreshTexts()`. Pattern complesso, +rischio bug. |

**Proposta**: A.

### 7.11 Pulsanti LAN/Online disabilitati: visibili o nascosti?

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Visibili ma `disabled` con tooltip "Disponibile in Fase 7" / "Disponibile in Fase 6". | Comunica la roadmap del prodotto. Layout main menu già nella sua forma definitiva. |
| B | Nascosti completamente in F3, riapparizione in F6/F7. | Layout main menu cambia visibilmente fra fasi → meno pulito. |

**Proposta**: A.

### 7.12 Selezione UI: click-to-select vs drag-and-drop

SPEC §9.2.1 descrive solo click-to-select.

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Solo click-to-select (click su pedina → highlight → click su target). | Coerente con SPEC, accessibile (compatibile keyboard nav). |
| B | Click-to-select + drag-and-drop come scorciatoia. | Più ergonomico per utenti scacchi-like ma costa codice e test. Valutabile in F11. |

**Proposta**: A.

### 7.13 Semantica undo/redo con IA in mezzo (FR-SP-06)

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Undo annulla **una coppia di mosse** (mossa umana + risposta IA), riportando al turno umano. Redo riapplica la stessa coppia. Stack lineare, no branching. | Naturale per il giocatore: "torna al mio ultimo turno". |
| B | Undo annulla **una sola mossa** (l'ultima qualunque sia stata). Se era IA, l'umano si ritrova nel turno IA → confusione. | Letterale ma poco usabile. |
| C | Stack con branching: posso "andare in un punto qualsiasi" della partita. | Complessità eccessiva, cambia modello mentale. F3 → no. |

**Proposta**: A.

### 7.14 Branch di lavoro

**Proposta**: `feature/3-ui-singleplayer` come unico branch di Fase 3, staccato da `develop`. Merge `--no-ff` su `develop` a fine fase. Merge `develop → main` con tag `v0.3.0` sul commit di merge in `main` (CLAUDE.md §4.4).

| Opzione | Descrizione |
|---|---|
| **A** (proposta) | Singolo branch `feature/3-ui-singleplayer`. Commit per Task. |
| B | Sotto-branch per Task (es. `feature/3-bootstrap`, `feature/3-board`, `feature/3-saves`). | Overhead di merge per single-developer; non giustificato. |

**Proposta**: A.

---

## 8. Stima di completamento

In numero di task (CLAUDE.md §2.1):

- Task 3.1 ÷ 3.18 + 3.20 ÷ 3.23 → **22 task** obbligatori.
- Task 3.19 (mini-animazioni regole) → **+1 opzionale** (proposta: incluso, vedi §7.6).
- Ogni task termina con un commit Conventional Commits. Esempi:
  - `feat(client): bootstrap javafx with spring boot di and scene router`
  - `feat(client): add design system theme.css with light tokens and inter font`
  - `feat(client): add localization with messagesource and it/en bundles`
  - `feat(client): add user preferences service with atomic config.json write`
  - `feat(client): add splash screen with bootstrap pipeline`
  - `feat(client): add main menu with six cards and disabled placeholders`
  - `feat(client): add single-player setup modal`
  - `feat(client): add board renderer with snapshot support`
  - `feat(client): add board interaction with legal-target highlights`
  - `feat(client): add move animations for slide capture promotion and pulse`
  - `feat(client): add move history pane in fid notation`
  - `feat(client): add status pane with side-to-move and endgame banner`
  - `feat(client): integrate ai turn service on virtual thread`
  - `feat(client): add save service with atomic write and slot listing`
  - `feat(client): add save dialog and load screen`
  - `feat(client): add autosave with recovery prompt at startup`
  - `feat(client): add settings screen for locale and ui scaling`
  - `feat(client): add rules screen with sections and static board diagrams`
  - `feat(client): add demonstrative rules animations` (opzionale)
  - `feat(client): add accessibility hooks for accessibletext keyboard nav and scaling`
  - `test(client): add testfx end-to-end tests for single-player save load and autosave`
  - `chore(client): add jacoco gate at 60% with view-class exclusions`
  - `docs(client): add package-info adr-029..033 traceability and changelog`

---

## 9. Output finale della Fase 3

Albero file atteso a chiusura fase (delta rispetto a F2, escludendo `target/`):

```
client/
├── pom.xml                                  (aggiornato: mainClass javafx-maven-plugin, JaCoCo gate 60%)
└── src/
    ├── main/
    │   ├── java/com/damaitaliana/client/
    │   │   ├── ClientApplication.java
    │   │   ├── package-info.java                         (aggiornato)
    │   │   ├── app/
    │   │   │   ├── package-info.java
    │   │   │   ├── JavaFxApp.java
    │   │   │   ├── JavaFxAppContextHolder.java
    │   │   │   ├── SceneRouter.java
    │   │   │   ├── SceneId.java
    │   │   │   ├── FxmlLoaderFactory.java
    │   │   │   └── ThemeService.java
    │   │   ├── controller/
    │   │   │   ├── package-info.java
    │   │   │   ├── SinglePlayerController.java
    │   │   │   ├── SinglePlayerGame.java
    │   │   │   ├── MoveSelector.java
    │   │   │   └── AiTurnService.java
    │   │   ├── i18n/
    │   │   │   ├── package-info.java
    │   │   │   ├── MessageSourceConfig.java
    │   │   │   ├── LocaleService.java
    │   │   │   └── I18n.java
    │   │   ├── persistence/
    │   │   │   ├── package-info.java
    │   │   │   ├── UserPreferences.java
    │   │   │   ├── PreferencesService.java
    │   │   │   ├── SavedGame.java
    │   │   │   ├── SerializedGameState.java
    │   │   │   ├── SaveSlotMetadata.java
    │   │   │   ├── SaveService.java
    │   │   │   └── AutosaveService.java
    │   │   └── ui/
    │   │       ├── package-info.java
    │   │       ├── splash/SplashController.java
    │   │       ├── menu/MainMenuController.java
    │   │       ├── setup/SinglePlayerSetupController.java
    │   │       ├── board/
    │   │       │   ├── package-info.java
    │   │       │   ├── BoardViewController.java
    │   │       │   ├── BoardRenderer.java
    │   │       │   ├── BoardCellNode.java
    │   │       │   ├── PieceNode.java
    │   │       │   ├── MoveHistoryView.java
    │   │       │   ├── MoveHistoryViewModel.java
    │   │       │   ├── StatusPaneController.java
    │   │       │   └── animation/
    │   │       │       ├── MoveAnimator.java
    │   │       │       └── AnimationOrchestrator.java
    │   │       ├── save/
    │   │       │   ├── SaveDialogController.java
    │   │       │   └── LoadScreenController.java
    │   │       ├── settings/SettingsController.java
    │   │       └── rules/
    │   │           ├── RulesController.java
    │   │           └── RulesAnimations.java                 (opzionale Task 3.19)
    │   └── resources/
    │       ├── application.yml
    │       ├── css/
    │       │   ├── theme-light.css
    │       │   ├── theme-dark.css                            (stub)
    │       │   └── components.css
    │       ├── fonts/
    │       │   ├── Inter-Regular.ttf
    │       │   └── Inter-SemiBold.ttf
    │       ├── fxml/
    │       │   ├── splash.fxml
    │       │   ├── main-menu.fxml
    │       │   ├── sp-setup.fxml
    │       │   ├── board-view.fxml
    │       │   ├── save-dialog.fxml
    │       │   ├── load-screen.fxml
    │       │   ├── settings.fxml
    │       │   └── rules.fxml
    │       ├── i18n/
    │       │   ├── messages_it.properties
    │       │   └── messages_en.properties
    │       └── rules/
    │           ├── setup.json
    │           ├── movement.json
    │           ├── capture.json
    │           ├── precedence.json
    │           └── promotion.json
    └── test/
        ├── java/com/damaitaliana/client/
        │   ├── ClientBootstrapTest.java
        │   ├── app/
        │   │   ├── SceneRouterTest.java
        │   │   └── ThemeServiceTest.java
        │   ├── i18n/
        │   │   ├── MessageSourceConfigTest.java
        │   │   └── LocaleServiceTest.java
        │   ├── persistence/
        │   │   ├── PreferencesServiceTest.java
        │   │   ├── SaveServiceTest.java
        │   │   └── AutosaveServiceTest.java
        │   ├── controller/
        │   │   ├── SinglePlayerControllerTest.java
        │   │   ├── MoveSelectorTest.java
        │   │   └── AiTurnServiceTest.java
        │   ├── ui/
        │   │   ├── splash/SplashControllerTest.java
        │   │   ├── menu/MainMenuControllerTest.java
        │   │   ├── setup/SinglePlayerSetupTest.java
        │   │   ├── board/
        │   │   │   ├── BoardRendererTest.java
        │   │   │   ├── BoardInteractionTest.java
        │   │   │   ├── BoardInteractionRulesTest.java
        │   │   │   ├── MoveHistoryViewModelTest.java
        │   │   │   ├── StatusPaneControllerTest.java
        │   │   │   └── animation/MoveAnimatorTest.java
        │   │   ├── save/
        │   │   │   ├── SaveDialogTest.java
        │   │   │   └── LoadScreenTest.java
        │   │   ├── settings/SettingsControllerTest.java
        │   │   └── rules/
        │   │       ├── RulesControllerTest.java
        │   │       └── RulesAnimationsTest.java              (opzionale)
        │   ├── e2e/
        │   │   ├── SinglePlayerE2ETest.java
        │   │   ├── SaveLoadE2ETest.java
        │   │   ├── AutosaveE2ETest.java
        │   │   ├── LocalizationTest.java
        │   │   └── RulesScreenTest.java
        │   └── AccessibilityTest.java
        └── resources/
            └── e2e/
                └── corpus-position-snapshots/         (riuso da shared corpus)
```

**File esterni a `client/` aggiornati**:

- `ARCHITECTURE.md` (ADR-029, 030, 031, 032, 033).
- `tests/TRACEABILITY.md` (righe FR-SP-01÷09, FR-RUL-01÷05, NFR-U-01, NFR-U-04 light, NFR-M-02 client, AC §17.1.1/7/8/9/12, AC F3).
- `CHANGELOG.md` (`[Unreleased]`).
- `AI_CONTEXT.md` (stato).
- `README.md` (sezione "Eseguire il client").
- `plans/PLAN-fase-3.md` → questo file.

**File rimossi**:

- `client/src/test/java/com/damaitaliana/client/ClientSmokeTest.java` (CLAUDE.md §6.1).

---

## 10. Definition of Done della Fase 3

- [ ] Task 3.1 ÷ 3.18 + 3.20 ÷ 3.23 completati e committati su `feature/3-ui-singleplayer`.
- [ ] Task 3.19 incluso (proposta §7.6) o esplicitamente deferred a F11 con CR.
- [ ] Acceptance criteria A3.1 ÷ A3.21 verificati.
- [ ] `mvn -pl client verify`: BUILD SUCCESS con coverage `client` (escluso view) ≥ 60% line + branch.
- [ ] `mvn clean verify` (root): BUILD SUCCESS (regression sui moduli `shared`/`core-server`/`server` assente).
- [ ] Spotless OK, SpotBugs 0 High su tutto `client`.
- [ ] **A3.3 (gating)**: `SinglePlayerE2ETest#completesGameVsEsperto` verde.
- [ ] **A3.4 (gating)**: `SaveLoadE2ETest#saveThenLoadResumesAtSameState` verde.
- [ ] **A3.5 (gating)**: `AutosaveE2ETest#promptOnRestartWhenAutosavePresent` verde.
- [ ] AC §17.1.1/7/8/9/12 + AC F3 mappati nella TRACEABILITY.
- [ ] `RuleEngineCorpusTest` (Fase 1, 48 posizioni) **continua a passare**.
- [ ] `AiTournamentSimulationTest` (Fase 2 gating) **continua a passare** (rerun on demand).
- [ ] Sotto-fase REVIEW (CLAUDE.md §2.3) eseguita → `reviews/REVIEW-fase-3.md` creato e chiuso.
- [ ] Sotto-fase TEST (CLAUDE.md §2.4) eseguita → `tests/TEST-PLAN-fase-3.md` creato; `tests/TRACEABILITY.md` aggiornato.
- [ ] Branch `feature/3-ui-singleplayer` mergiato `--no-ff` su `develop`.
- [ ] Merge `develop → main` + tag `v0.3.0` sul commit di merge.

---

**FINE PLAN-fase-3**
