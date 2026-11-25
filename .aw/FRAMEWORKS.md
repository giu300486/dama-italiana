# Framework di sviluppo

<!-- Adapter per le skill di Matt Pocock, scritto da /aw:framework. È l'unico posto che nomina il framework.
     Le regole di progetto in CLAUDE.md e gli override qui sotto prevalgono sulle istruzioni delle skill. -->

## Attivo: Skills For Real Engineers, Matt Pocock

### 1. Identità
| Campo | Valore |
|---|---|
| Framework | Skills For Real Engineers, Matt Pocock (`github.com/mattpocock/skills`, licenza MIT) |
| Plugin | `mattpocock-skills@claude-plugins-official` (skill mostrate come `mattpocock-skills:<nome>`) |
| Versione verificata | `1.3.1` |
| Installazione | `claude plugin install mattpocock-skills@claude-plugins-official --scope local`, da terminale (la schermata `/plugin` non ha lo scope local) |
| Review read-only | 2026-11-21: hook no, server MCP no, script: solo `diagnosing-bugs/scripts/hitl-loop.template.sh` (modello, non eseguito in automatico) e gli script di build del repository del plugin (`scripts/*.sh`, `sync-plugin-version.mjs`, non usati da Claude Code) |
| Aggiornamenti automatici | non disattivati sul marketplace ufficiale: rifare la review a ogni nuova versione |

### 2. Fase → skill
Router ufficiale: `/ask-matt` (lo lancia l'utente). Una volta per progetto, per primo:
`/setup-matt-pocock-skills`, guidato.

| Fase | Situazione | Skill | Chi la lancia |
|---|---|---|---|
| Allineamento | idea o funzionalità nuova, nel progetto | `/grill-with-docs` (aggiorna glossario di dominio e ADR) | **utente** |
| Allineamento | un piano senza repository sotto | `/grill-me` | **utente** |
| Specifica | dopo il grilling, nella stessa finestra | `/to-spec` | **utente** |
| Specifica | uno sforzo enorme e nebuloso, su più sessioni | `/wayfinder`, poi `/to-spec` | **utente** |
| Scomposizione | spec pronta, lavoro su più sessioni | `/to-tickets` | **utente** |
| Implementazione | ticket o spec da costruire | `/implement` (usa `tdd`, chiude con `code-review`); `/implement-spec` per l'esito di `/to-spec` + `/to-tickets` | **utente** |
| Implementazione | un comportamento, test prima, senza spec | `tdd` | agente |
| Review | un diff da un punto fisso | `mattpocock-skills:code-review` | agente |
| Review | sicurezza | regola di progetto: `/security-review` integrata (il progetto non ha un documento di policy); il framework **non** la copre | agente |
| Diagnosi | bug difficile, regressione, test instabile | `diagnosing-bugs` | agente |
| Diagnosi | segnalazioni grezze da smistare | `/triage` | **utente** |
| Cura del design | salute del codice, nei momenti tranquilli | `/improve-codebase-architecture` | **utente** / agente |
| Confine di fase | continuare, `/clear`, `/handoff`, subagent, `/compact` | la guida ai confini di fase del framework (dentro `ask-matt`) | l'agente suggerisce |
| Retrospettiva | fine di una sessione di lavoro significativa | `/retro` | **utente** |
| — | un messaggio poco chiaro dell'agente | `/wait-what` | **utente** |

**Igiene del contesto (dal framework):** grilling, spec e ticket nella **stessa** finestra; ogni `/implement`
parte pulito dal suo ticket.

**Workstream:** un workstream aw per funzionalità; il suo `RESUME.md` punta alla spec e ai ticket invece di
copiarli.

### 3. Dove vivono gli artefatti
| Artefatto | Dove | In git |
|---|---|---|
| Spec e ticket (tracker locale) | `.scratch/<feature>/spec.md` e `.scratch/<feature>/issues/NN-slug.md` | no (`.git/info/exclude`) |
| Glossario di dominio | `GLOSSARY.md` alla radice del repository (non `.aw/…/CONTEXT.md`, il diario aw) | no (`.git/info/exclude`) |
| Decisioni | ADR-001…ADR-049 restano in `ARCHITECTURE.md`; i nuovi in `docs/adr/`, a partire da `0050-<slug>.md` (regola in `docs/agents/domain.md`) | sì |
| Configurazione del framework | `docs/agents/issue-tracker.md` (locale), `docs/agents/triage-labels.md` (label canoniche), `docs/agents/domain.md` (single-context), più la sezione `## Agent skills` in `CLAUDE.md`; scritti dal setup il 2026-11-21 | sì |
| Principi di progetto (lo standard della review) | `CLAUDE.md` §2.4, §4.1, §8; `SPEC.md` | sì |

Nota: `docs/agents/domain.md` (in git) nominerà `GLOSSARY.md`, che resta locale: chi clona il repository non
ha il glossario finché non lo si decide di versionare.

### 4. Override: regole di progetto che prevalgono
| Skill | Istruzione del framework | Regola che vince |
|---|---|---|
| `implement`, `implement-spec`, `diagnosing-bugs` | fanno commit da sole | commit **solo dopo conferma dell'utente**, Conventional Commits (CLAUDE.md §4.2), senza trailer `Co-Authored-By`; **mai push** |
| `prototype` | fa commit su un branch usa-e-getta | solo dopo conferma, mai push; branch `prototype/<nome>` da `develop` |
| `to-spec`, `to-tickets`, `grill-with-docs` | scrivono la spec della funzionalità | `SPEC.md` resta la fonte di verità e si cambia solo con change request approvata, commit `docs(spec): …` e voce in `CHANGELOG.md` (CLAUDE.md §1); una spec in `.scratch/` non lo modifica mai |
| `domain-modeling`, `grill-with-docs` | scrivono ADR in `docs/adr/` | ADR-001…049 restano in `ARCHITECTURE.md` e non si spostano; i nuovi proseguono la numerazione da ADR-050 in `docs/adr/` |
| `setup-matt-pocock-skills` | scrive un blocco `## Agent skills` in `CLAUDE.md` | modifica di `CLAUDE.md` solo dopo discussione, commit dedicato `docs(claude): …` e voce in `CHANGELOG.md` (CLAUDE.md §10) |
| `implement`, `implement-spec`, `tdd` | ciclo di test del framework | valgono le regole di CLAUDE.md §2.4 (piramide, coverage per modulo, corpus `test-positions.json`, naming) e gli anti-pattern di §8 |
| `implement`, `implement-spec` | passano al ticket successivo | chiedere conferma all'utente prima di ogni nuovo task numerato |
| `research` (e ogni accesso alla rete) | fonti primarie, web compreso | rete consentita dal perimetro (`security.network: allow`); mai dati del progetto o personali in richieste esterne |
| `wizard` | scrive `.env` e segreti | i percorsi dei segreti sono negati: non usarla per credenziali |
| `code-review` | stesso nome della `/code-review` integrata | chiamarla sempre `mattpocock-skills:code-review` |
| tutte | — | dopo `code-review`, sempre `/security-review` |

### 5. Skill escluse o limitate
- `setup-matt-pocock-skills`: eseguita il 2026-11-21 (tracker locale, label canoniche, single-context).
  Rilanciarla solo per cambiare tracker; le modifiche quotidiane si fanno a mano in `docs/agents/`.
- `wizard`: mai per credenziali o segreti (override §4).

## Ritirati
- **SDD-4** (Spec-Driven Development: PIANIFICA → IMPLEMENTA → REVIEW → TEST per fase, con `AI_CONTEXT.md`,
  `plans/`, `reviews/`, test plan e traceability matrix), usato dal 2026-04-27 al 2026-11-21, archivio in
  `_legacy/sdd4/` (vedi il suo README).
