# TEST PLAN — Fase 3: Client UI single-player

- **Riferimento roadmap**: `SPEC.md` §16 — Fase 3.
- **SPEC version**: 2.0 (2026-04-26, aggiornata il 2026-04-28 con CR-001).
- **Data**: 2026-04-30.
- **Autore**: Claude Code.
- **Stato**: DRAFT — sezioni 1, 7 popolate durante closure REVIEW (REVIEW-fase-3 finding F-002, opzione B); sezioni 2-6, 8-10 saranno completate nella sotto-fase TEST di Fase 3.

---

## 1. Scopo della sotto-fase TEST

Documentare la strategia, la composizione e la copertura della suite di test della Fase 3 (Client UI single-player), validare gli **acceptance criteria operativi A3.1–A3.21** del PLAN-fase-3 §2.2 e gli AC SPEC §17 rilevanti (17.1.1, 17.1.7, 17.1.8, 17.1.9, 17.1.12), finalizzare la matrice di tracciabilità requisiti → test (`TRACEABILITY.md`) per i requisiti coperti dalla Fase 3.

> **Nota workflow**: questo file è stato creato durante la closure della sotto-fase REVIEW per documentare la procedura di validazione manuale di A3.3 (vedi §7), risolvendo il finding F-002 secondo l'opzione B scelta dall'utente. Le sezioni rimanenti (composizione test, coverage, naming, regression cross-modulo, closure) saranno scritte nella sotto-fase TEST di Fase 3, dopo che la sotto-fase REVIEW sarà chiusa.

---

## 2. Strategia di test (CLAUDE.md §2.4)

> _Da popolare nella sotto-fase TEST._

Pianificazione: piramide classica con traceability esplicita (Approccio C). Il modulo `client` riceve in F3 la prima suite di unit test + 4 E2E TestFX (`SinglePlayerE2ETest`, `SaveLoadE2ETest`, `AutosaveE2ETest`, `RulesScreenE2ETest`, `LocalizationE2ETest`). Conteggio attuale al commit `74de2af`: **280 test verdi** (264 baseline F3 + 16 nuovi per Task 3.24 undo/redo).

### 2.1 Composizione effettiva (modulo `client`)

> _Tabella dettagliata da popolare con conteggi per package._

### 2.2 Coverage effettiva (`mvn -pl client jacoco:report`)

> _Da popolare con misura post-Task 3.24._
> Snapshot al commit `74de2af`: line **74.18%**, branch **60.95%** (gate 60% line+branch con `haltOnFailure=true`).

### 2.3 SAST e style

> _Da popolare._
> Snapshot al commit `74de2af`: SpotBugs 0 High, Spotless OK.

---

## 3. Test corpus regole italiane (regression)

> _Da popolare nella sotto-fase TEST. Il corpus F1 (48 posizioni) deve restare verde — vedi A3.18._

---

## 4. Naming convention

> _Da popolare nella sotto-fase TEST. Conforme a CLAUDE.md §2.4.5._

---

## 5. Tag JUnit

> _Da popolare nella sotto-fase TEST._
> In Fase 3 non sono stati introdotti nuovi tag `@Tag("slow")` o `@Tag("performance")` rispetto a F2 (vedi §7 per la giustificazione del deferral del test full-game).

---

## 6. Aggiornamento `TRACEABILITY.md`

> _Da popolare nella sotto-fase TEST. Le righe F3 sono già state aggiunte in Task 3.23 (commit `9e83337`); restano da fissare gli aggiornamenti per Task 3.24 (undo/redo) e i fix di drift identificati in F-003._

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

> _Da compilare al primo run manuale._

| Data       | Esito           | N. mosse | Sotto-feature OK | Eccezioni / note      | Eseguito da |
|------------|-----------------|---------:|------------------|-----------------------|-------------|
| _TBD_      | _TBD_           |    _TBD_ | _TBD_            | _TBD_                 | _TBD_       |

---

## 8. Regressione cross-modulo

> _Da popolare nella sotto-fase TEST. `mvn clean verify -DexcludedGroups=slow,performance` (root) deve restare BUILD SUCCESS. `mvn clean verify` con `slow`+`performance` inclusi deve essere lanciato almeno una volta (≥ 16 min) per validare F1 corpus + F2 gating._

---

## 9. Limiti documentati e debiti tecnici tracciati

> _Da popolare nella sotto-fase TEST con la lista completa._
> Già noti (da REVIEW-fase-3): NFR-U-03 a11y in inglese hardcoded (deferred F11, vedi finding F-005); NFR-U-02 dark mode toggle runtime (deferred F11); NFR-P-01 misura formale 60 FPS (deferred F11).

---

## 10. Closure della sotto-fase TEST (CLAUDE.md §2.4.6)

> _Checklist da spuntare nella sotto-fase TEST._

- [ ] Coverage target raggiunti (`client` ≥ 60% line+branch già confermato dal gate JaCoCo)
- [ ] Traceability matrix aggiornata — ogni FR/NFR/AC della fase ha almeno un test (post Task 3.24)
- [ ] Test corpus regole italiane invariato (48 posizioni F1 verdi)
- [ ] **§7.4 popolato** con esito della validazione manuale A3.3 (F-002 RESOLVED via manual)
- [ ] `mvn verify` (default `-DexcludedGroups=slow,performance`) passa pulito su tutti i moduli
- [ ] `mvn clean verify` con `slow`+`performance` inclusi BUILD SUCCESS almeno una volta (regression F1+F2)
- [ ] Test plan documenta scelte e copertura — questo file
- [ ] Nessun test in stato `@Disabled`/`@Ignore` senza issue tracciata

---

**FINE TEST-PLAN-fase-3 (DRAFT)**
