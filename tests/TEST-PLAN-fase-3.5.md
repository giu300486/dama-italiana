# TEST PLAN — Fase 3.5: Visual polish + Audio + Demo release

- **Data piano**: 2026-05-01.
- **SPEC version**: 2.2 (post CR-F3.5-001..005).
- **Branch**: `feature/3.5-visual-polish-and-audio`.
- **PLAN di riferimento**: [`plans/PLAN-fase-3.5.md`](../plans/PLAN-fase-3.5.md).
- **Stato**: DRAFT — sezione §7 in popolamento durante Task 3.5.14.

---

## 1. Scope

Vedi `plans/PLAN-fase-3.5.md` §1. In sintesi: visual rework 8 schermate + audio (ambient music + 4 SFX) + animation polish (juicy easing, particle effects, glow halo) + Windows MSI installer via jpackage.

## 2. Strategia (riferimento)

Vedi `plans/PLAN-fase-3.5.md` §5 e CLAUDE.md §2.4.1.

## 3. Coverage target

- `client` ≥ 60% line+branch (gate JaCoCo invariato dal F3).
- Nuovi sotto-package: `client.audio` ≥ 70% line.
- `shared`/`core-server`/`server` invariati (no codice prod toccato in F3.5).

## 4. Test corpus regole italiane

Invariato rispetto a F3 (nessuna modifica a `RuleEngine`).

## 5. Esecuzioni pianificate

| Esecuzione | Quando | Scope | Esito |
|---|---|---|---|
| `mvn -pl client verify -DexcludedGroups=slow,performance` | Ad ogni task IMPLEMENTA | client unit + smoke | Da popolare |
| `mvn clean verify` (root) | Pre-REVIEW (Task 3.5.16) e pre-closure (Task 3.5.17) | tutti i moduli | Da popolare |
| `mvn -pl shared verify -Dgroups=slow` | Pre-closure (regression gate IA F2) | shared slow | Da popolare |

## 6. Test code aggiunto/modificato in F3.5

- `AudioServiceTest` (Task 3.5.4) — volume scaling, mute, shuffle deterministica con seed, SFX dispatch, persistence round-trip.
- `PieceNodeTest` (Task 3.5.7) — rendering uomo/dama bianco/nero, marker.
- `ParticleEffectsTest` (Task 3.5.8) — count + lifecycle.
- `UserPreferencesTest` extension (Task 3.5.4) — campi audio persistence.
- `*FxmlSmokeTest` aggiornati al nuovo tree (Task 3.5.13).

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
| Splash | `splash.png` | ⚠️ NON CATTURATO | Splash troppo rapido (~1.5s); look verificato a occhio dall'utente, conforme. Non bloccante. |
| Main menu | `main-menu.png` | ✅ PASS | Wood frame + 6 card cream + serif display |
| Single-player setup | `sp-setup.png` | ✅ PASS | Card "Nuova partita" su wood, bottone gold "Inizia" |
| Board (mid-game) | `board-game.png` | ✅ PASS | Texture legno + pezzi 3D bombati + side panel storia mosse |
| Save dialog | `save-dialog.png` | 🔧 RIFATTO post-fix | Pre-fix: dialog Stage modale senza stylesheet wood (finding REQUIREMENT_GAP, fixato in Task 3.5.14a). Post-fix: PASS (ri-screenshot post-rebuild MSI). |
| Load screen | `load-screen.png` | ✅ PASS | Lista partite + anteprima miniatura |
| Settings | `settings.png` | ✅ PASS | Slider Musica + Effetti + Muto (A3.5.12 confermato) |
| Rules | `rules.png` | ✅ PASS | Sidebar nav + diagram board cream |

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
- **F-001 [REQUIREMENT_GAP, High]** save-dialog non skinned wood. Causa: `SaveDialogController` creava `Stage` modale + `new Scene(root)` senza chiamare `ThemeService.applyTheme(scene)`, quindi gli stylesheet `theme-light.css` + `components.css` non erano caricati nella scene del dialog. **Fix applicato**: iniettata dipendenza `ThemeService` nel costruttore, chiamata `applyTheme(scene)` dopo `new Scene`. Test aggiornati: `SaveDialogControllerTest`, `FxmlLoadingSmokeTest`. `SaveDialogSpringContextTest` automaticamente OK (il bean si risolve via Spring DI con la nuova signature). Status: **RESOLVED** — sarà tracciato come F-001 in `reviews/REVIEW-fase-3.5.md`.
- **Splash screen non catturabile a screenshot** (~1.5s di durata). Comportamento by design (vedi PLAN-fase-3.5 Task 3.5.9 — splash non blocca). Non genera finding. La review visuale è stata fatta a occhio dall'utente; conforme.

---

## 8. Closure check

- [ ] Coverage target raggiunti (`mvn jacoco:report`).
- [ ] TRACEABILITY aggiornata: A3.5.x → test.
- [ ] `mvn clean verify` (root) BUILD SUCCESS.
- [ ] §7 Manual demo run popolato con esito PASS.
- [ ] Nessun test in stato `@Disabled` o `@Ignore` senza issue.

**Test plan chiuso il**: _(da popolare)_
**Commit di chiusura**: _(da popolare)_
