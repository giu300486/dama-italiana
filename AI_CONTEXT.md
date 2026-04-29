# AI Context

> File di stato corrente del progetto.
> Da aggiornare ad ogni task completato (CLAUDE.md §2.2).

## Stato corrente

- **Branch corrente**: `feature/3-ui-singleplayer`, staccato da `develop` al commit `d6168ad`. Modello GitFlow leggero (CLAUDE.md §4.3): merge `--no-ff` su `develop` a chiusura fase, poi `develop → main` con tag `v0.3.0` sul commit di merge in `main`.
- **Fase roadmap**: Fase 3 — Client UI single-player.
- **Sotto-fase**: **IMPLEMENTA** (PIANIFICA chiusa il 2026-04-28 con approvazione utente in blocco opzione A su tutti gli stop point §7.1-§7.14 di `plans/PLAN-fase-3.md`).
- **Ultimo task completato**: Task 3.15 — Save dialog + Load screen. Nuovo pkg `client.ui.save` + integrazione menu sul board. `SceneId.LOAD` aggiunto al router (mapping `/fxml/load-screen.fxml`). `SinglePlayerController.currentSnapshot()` esposto pubblico per il save dialog; `SinglePlayerGame.fromSaved(SavedGame, RandomGenerator)` static factory per il reverse. `UserPromptService.confirm` esteso con `Object... formatArgs` (default no-arg overload preserva i caller esistenti) + nuovo `info(titleKey, contentKey, Object... contentArgs)` per i toast (Alert.INFORMATION); JavaFx impl applica i format args via `i18n.t(key, args)` su title/header/content. `SaveDialogController` `@Component @Scope("prototype")` con `show(SinglePlayerGame, Window)` che carica `save-dialog.fxml` via FXMLLoader+`setController(this)`, modal stage, blocking `showAndWait`; `confirm(name)` (visible-for-test) restituisce `ConfirmResult` enum (SAVED/NAME_EMPTY/NAME_INVALID/OVERWRITE_DECLINED/IO_ERROR); slugify via `SaveService.slugify`; overwrite prompt se slot esiste; toast success/error via `UserPromptService.info`; Clock injectable per timestamp deterministici nei test. `LoadScreenController` `@Component @Scope("prototype")` su `load-screen.fxml`: `TableView<SaveSlotMetadata>` con 6 colonne (Nome / Data / Livello / Colore / Mossa / Anteprima — sortable nelle prime 5; miniatura non-sortable); cell factory dell'anteprima istanzia `MiniatureCell` che on-demand carica il `SavedGame` completo e chiama `MiniatureRenderer.render(state, 64)`; pulsanti Carica/Elimina disabled via Bindings.isNull selezione; `loadSelected`/`deleteSelected` (visible-for-test) ritornano typed enum (LoadResult/DeleteResult); typed handling `UnknownSchemaVersionException` → toast `load.toast.error.schema.*`, `UncheckedIOException` → toast `load.toast.error.io.*` (entrambi con `selected.name()` come arg); `refresh()` null-tolerant su `slotsTable` (test harness skip UI side-effects, popola solo `currentSlots`); `Supplier<RandomGenerator>` injectable per RNG deterministico nei test. `MiniatureRenderer` `@FunctionalInterface` con prod impl `CanvasMiniatureRenderer` `@Component` che disegna su `Canvas` (8x8 grid + dischi pezzi + ring oro per dame) e `canvas.snapshot()` → `Image`. `BoardViewController` ridisegnato: `board-view.fxml` ora ha `MenuBar` con menu "Partita" (Salva con nome / Carica / Termina partita) sopra il titolo; `onSaveAs()` → `openSaveDialog()` (visible-for-test) prende `gameController.currentSnapshot()` e apre il dialog modal (owner Window dalla scene); `onLoad()` chiama `gameController.stop()` e naviga a `SceneId.LOAD`; `onTerminate()` → `terminate()` (visible-for-test) confirm prompt + `saveService.delete(AUTOSAVE_SLOT)` + `gameSession.clear()` + `sceneRouter.show(MAIN_MENU)`. **i18n** keys IT/EN: `board.menu.*`, `board.terminate.*`, `save.dialog.*`, `save.toast.*`, `load.title`, `load.column.*`, `load.button.*`, `load.empty`, `load.confirm.*` (con `{0}` per nome), `load.toast.error.*`. **22 nuovi test**: SaveDialogControllerTest 6 (persists, empty, unsluggable, overwriteDeclined, overwriteConfirmed, ioError), LoadScreenControllerTest 10 (lists 3 saves, click+load resumes, cancel, schema mismatch toast, IO error toast, no selection, delete, delete cancel, delete no selection, onBack), BoardViewControllerTest 4 (terminate success, terminate cancel, onLoad navigates+stops AI, openSaveDialog null guard), CanvasMiniatureRendererTest 2 (rejects non-positive size, miniatureRendersWithoutErrors via Platform.startup + runLater + CountDownLatch con `Assumptions.assumeTrue` per skip su headless). `mvn -pl client verify -DexcludedGroups=slow,performance` BUILD SUCCESS, **183 test totali** (+22 vs Task 3.14), JaCoCo client gate ✅, SpotBugs 0 High, Spotless OK.
- **Prossimo task**: Task 3.16 — Autosave + recovery prompt. `AutosaveService` `@Component` wrap di `SaveService` vincolato a `_autosave`; hook in `SinglePlayerController.applyMoveAndAnimate` (impl di `AutosaveTrigger`); hook in `SplashController` (rilevazione + flag) e `MainMenuController` (resume/discard, sostituisce il delete diretto). Dipendenze: Task 3.9, 3.14 (entrambi chiusi). Da fase 3.16 arriverà anche l'`AutosaveTrigger` impl che oggi è `Optional.empty` di default in `SinglePlayerController`.
- **Stato test (baseline pre-Fase 3)**: `mvn clean verify` (root) BUILD SUCCESS in ~2 min — `shared` 391 test (387 default + 1 slow + 3 performance), JaCoCo 97.3% modulo + 96.2% package `rules` + 97.7% package `ai`, SpotBugs 0 High, Spotless OK. `core-server`/`client`/`server` ancora con il singolo smoke test ciascuno (lo smoke `client` verrà rimosso in Task 3.1).
- **Test gating A2.2 (slow)**: ✅ PASSED il 2026-04-28 in 16:02 min (Campione ≥ 95/100 vs Principiante). Da rieseguire on demand a chiusura Fase 3 (regression).
- **Piano di riferimento Fase 3**: [`plans/PLAN-fase-3.md`](plans/PLAN-fase-3.md).
- **Piani precedenti**: [`plans/PLAN-fase-2.md`](plans/PLAN-fase-2.md), [`reviews/REVIEW-fase-2.md`](reviews/REVIEW-fase-2.md), [`tests/TEST-PLAN-fase-2.md`](tests/TEST-PLAN-fase-2.md).

## Decisioni recenti

- Approvate dall'utente in data 2026-04-27 (Task PIANIFICA Fase 0):
  - Spotless: indentazione **2 spazi** (Google standard, conforme a NFR-M-04).
  - Maven `groupId`: `com.damaitaliana`.
  - Tag chiusura Fase 0: `v0.0.0`.
- Revisione post-implementazione (2026-04-27, dopo feedback utente):
  - **ADR-018**: Docker Compose rimosso. L'ambiente di sviluppo usa il MySQL locale dell'utente (porta 3306, gestito via Workbench/DBeaver).
  - **ADR-019**: workflow GitHub Actions rinominato in `ci.yml.disabled` (non eseguibile). Verrà riattivato quando un repository remoto sarà disponibile.
- Adozione **GitFlow leggero** (2026-04-28): `main` = production / tag, `develop` = integrazione e default branch GitHub, branch effimeri `feature/<fase>-<topic>` e `fix/review-N-F-<id>` staccati da `develop`, mergiati `--no-ff`. Tag delle fasi (`v0.<fase>.0`) sul commit di merge in `main`.
- Approvati dall'utente in data 2026-04-28 (Task PIANIFICA Fase 2): tutti gli stop point §7.1-§7.10 in opzione A in blocco. Task 2.14 (Zobrist-based threefold repetition) deferred-F4. Si è applicata in F2 una hardening fix non-Zobrist (catch `IllegalMoveException` → `false`) per supportare il search su stati hand-built (REVIEW finding F-001/F-002, ACKNOWLEDGED).
- ADR-024 ÷ ADR-028 registrati durante Fase 2 (architettura `AiEngine` sealed + 3 livelli, Evaluator modulare, Zobrist + TT, cancellation cooperativa, rumore Principiante deterministico).
- Approvati dall'utente in data 2026-04-28 (Task PIANIFICA Fase 3): tutti gli stop point §7.1-§7.14 in opzione A in blocco. Riepilogo decisioni:
  - §7.1 Bootstrap: `ClientApplication` Spring Boot non-web + `JavaFxApp.launch` + `JavaFxAppContextHolder` (ADR-029).
  - §7.2 FXML per layout schermate, `setControllerFactory(context::getBean)` (ADR-030); board view custom programmatica.
  - §7.3 Tema F3: solo light selezionabile; `theme-dark.css` come stub presente non collegato; toggle dark in F11.
  - §7.4 Schema saves v1: JSON con `currentState` materializzato + lista `moves` FID; rifiuto schemaVersion ignota (ADR-031).
  - §7.5 Autosave atomico: tmp + `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` con fallback log WARN (ADR-032).
  - §7.6 Mini-animazioni regole: incluse (Task 3.19), candidate primo slip se la fase si gonfia.
  - §7.7 Audio: escluso da F3, riservato a F11.
  - §7.8 Coverage gate `client` 60% line+branch su pkg non-view, esclusioni esplicite tracciate nel POM.
  - §7.9 Chiavi i18n hierarchiche `screen.element.role`, bundle unico `messages_*.properties` (ADR-033).
  - §7.10 Cambio lingua richiede riavvio in F3; runtime dynamic in F11.
  - §7.11 Card LAN/Online visibili e disabilitate con tooltip "Disponibile in Fase X".
  - §7.12 Solo click-to-select; drag-and-drop valutabile in F11.
  - §7.13 Undo/redo annulla la coppia `(mossa umana + risposta IA)` come unità.
  - §7.14 Branch unico `feature/3-ui-singleplayer`, merge `--no-ff` su `develop`, tag `v0.3.0` sul commit di merge in `main`.

## SPEC clarifications needed

Nessuna al momento.

## Note operative

- **Fast loop di sviluppo F3**: durante l'IMPLEMENTA usare `mvn -pl client verify -DexcludedGroups=slow,performance` (~30s). Il gating slow di F2 (`AiTournamentSimulationTest`, ~16 min) si esegue solo a chiusura fase prima della REVIEW, oppure on demand. Prerequisito una tantum: `mvn install -pl .,shared,core-server -DskipTests -Dspotless.check.skip=true -Dspotbugs.skip=true -Djacoco.skip=true` per popolare il local Maven repo con parent + shared + core-server jar.
- Inter TTF binaries non committati (vedi `client/src/main/resources/fonts/README.md`). `ThemeService` carica i font se presenti, altrimenti log INFO + fallback su font-family chain di SPEC §13.2 ("Segoe UI", "Helvetica Neue", sans-serif). Il developer/asset pipeline aggiunge i file in `resources/fonts/` se vuole l'estetica Inter. Decisione documentata nel commit di Task 3.2.
- Rappresentazione del board per il corpus regole **decisa in Fase 1** (ADR-022): JSON con quattro liste disgiunte `whiteMen`/`whiteKings`/`blackMen`/`blackKings` indicizzate in FID 1-32 (ADR-020).
- Smoke test `SharedSmokeTest` **rimosso** in Task 1.11. Restano `CoreServerSmokeTest`, `ClientSmokeTest`, `ServerSmokeTest`: da rimuovere quando i rispettivi moduli ricevono test reali (Fase 4 / 3 / 5 rispettivamente).
- JaCoCo `haltOnFailure=true` su `shared` con soglie 90% bundle + 90% package `rules` (Fase 1) + 85% package `ai` (Fase 2). Per `client`/`core-server`/`server` il gate resta lasco fino alle rispettive fasi.
- `isThreefoldRepetition` (ADR-021) replay-based con hardening fix Fase 2 (catch `IllegalMoveException` → `false`). Limite documentato per stati hand-built. Ottimizzazione Zobrist-incremental deferred a Fase 4 (REVIEW-fase-2 finding F-001/F-002).
- Tag JUnit attivi nel modulo `shared`: `slow` (1 test, gating A2.2 ~16 min) + `performance` (3 test, NFR-P-02). Default `mvn verify` include tutto; loop di sviluppo veloce: `-DexcludedGroups=slow,performance`.
- Determinismo Principiante (ADR-028): `RandomGenerator` iniettato; in test `SplittableRandom(42L + gameIndex)` per simulazioni riproducibili. Default produzione: `SplittableRandom(System.nanoTime())`.
- Determinismo Zobrist (ADR-026): seed costante `0xDA4A172L`. Stesso jar = stessi hash = stesso comportamento TT.
