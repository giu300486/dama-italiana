# TEST PLAN — Fase 0: Setup infrastruttura

- **Data**: 2026-04-28
- **Commit codebase**: `c90c1eb`
- **SPEC version**: 2.0 (2026-04-26)
- **Reviewer**: Claude Code
- **Riferimenti**:
  - [`plans/PLAN-fase-0.md`](../plans/PLAN-fase-0.md) §5
  - [`reviews/REVIEW-fase-0.md`](../reviews/REVIEW-fase-0.md)
  - `CLAUDE.md` §2.4

---

## 1. Premessa: scope di test della Fase 0

La Fase 0 è **puramente infrastrutturale**. Non implementa logica di dominio, né flussi end-to-end, né requisiti funzionali (`FR-*`) o non funzionali (`NFR-*`) di feature dello SPEC. Di conseguenza:

- **Nessun FR/NFR di SPEC §4-5 è candidato a copertura in Fase 0.**
- I test scritti in Fase 0 sono **smoke test di pipeline**: validano che il classpath sia coerente, che JUnit 5 + AssertJ siano correttamente in test scope, che il binding `prepare-agent` di JaCoCo sia attivo e che la lifecycle Maven raggiunga la phase `verify` su tutti i moduli.
- La **traceability matrix** (`tests/TRACEABILITY.md`) resta **vuota** alla chiusura della Fase 0. La sua popolazione inizierà in Fase 1 (`shared`: dominio + RuleEngine).

> Tradotto: in Fase 0 si verifica che "il letto del fiume sia scavato"; l'acqua arriva in Fase 1.

---

## 2. Composizione test

| Tipo               | N. test in Fase 0 | Tooling                             | Scope effettivo                                                                 |
|--------------------|------------------:|-------------------------------------|---------------------------------------------------------------------------------|
| **Unit** smoke     |                 4 | JUnit 5 + AssertJ                   | Una asserzione triviale per modulo, attiva il pipeline JUnit/JaCoCo/Spotless.  |
| Integration        |                 0 | —                                   | Nessuna in Fase 0 (richiede dominio).                                           |
| E2E UI (TestFX)    |                 0 | —                                   | Nessuna in Fase 0 (richiede schermate, da Fase 3).                              |
| E2E multi-client   |                 0 | —                                   | Nessuna in Fase 0 (richiede protocollo STOMP, da Fase 6).                       |
| **Totale**         |                 **4** |                                 |                                                                                 |

### Mappa test smoke

| Modulo        | Test class                                                              | Test method            | Scopo                                                          | Da rimuovere in |
|---------------|-------------------------------------------------------------------------|------------------------|----------------------------------------------------------------|-----------------|
| `shared`      | `com.damaitaliana.shared.SharedSmokeTest`                               | `buildPipelineIsAlive` | Valida AssertJ + JUnit 5 in classpath; attiva JaCoCo agent.    | Fase 1          |
| `core-server` | `com.damaitaliana.core.CoreServerSmokeTest`                             | `buildPipelineIsAlive` | Idem.                                                          | Fase 4          |
| `client`      | `com.damaitaliana.client.ClientSmokeTest`                               | `buildPipelineIsAlive` | Valida che `spring-boot-starter-test` non rompa la pipeline.   | Fase 3          |
| `server`      | `com.damaitaliana.server.ServerSmokeTest`                               | `buildPipelineIsAlive` | Idem (NON bootstrap-a un Spring context, solo classpath).      | Fase 5          |

Ognuno ha tracciamento esplicito nel proprio Javadoc + voce in finding `F-005` di `REVIEW-fase-0.md` (status ACKNOWLEDGED).

### Esecuzione corrente

`mvn -B -ntp test` su tutto il reactor (commit `c90c1eb`):

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0  -- shared.SharedSmokeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0  -- core.CoreServerSmokeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0  -- client.ClientSmokeTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0  -- server.ServerSmokeTest
[INFO] BUILD SUCCESS
```

Totale: **4 test, 0 failures, 0 errors, 0 skipped**.

---

## 3. Coverage

### Target SPEC (NFR-M-01, NFR-M-02)

| Modulo        | Target SPEC | Soglia attiva in Fase 0 | Wiring JaCoCo `check`                              |
|---------------|-------------|--------------------------|-----------------------------------------------------|
| `shared`      | ≥ 80%       | n/a (`haltOnFailure=false`, regola placeholder 0%) | da attivare in **Fase 1** con threshold reale     |
| `core-server` | ≥ 80%       | n/a                      | da attivare in **Fase 4**                           |
| `client`      | ≥ 60% (escluse view JavaFX) | n/a               | da attivare in **Fase 3**                           |
| `server`      | ≥ 70%       | n/a                      | da attivare in **Fase 5**                           |

### Coverage misurata in Fase 0

JaCoCo report HTML è generato per ogni modulo (acceptance A0.4 ✓), ma su codebase vuota la copertura misurata è 0% di 0 istruzioni:

```
[INFO] Analyzed bundle 'shared' with 0 classes
[INFO] All coverage checks have been met.
```

Questo è coerente con lo stato della fase. La regola `<minimum>0.00</minimum>` configurata nel parent POM è un **placeholder esplicito** documentato nel POM stesso e confermato da finding `F-006` (ACKNOWLEDGED). La transizione a soglie reali è un task obbligatorio della **PIANIFICA Fase 1**.

---

## 4. Test corpus regole italiane

CLAUDE.md §2.4.4 prescrive un test corpus parametrizzato in `shared/src/test/resources/test-positions.json` con almeno 48 posizioni distribuite per area (movimento, cattura, leggi italiane, promozione, ripetizione, 40 mosse).

**Stato in Fase 0**: **inesistente**.

Motivazione: il corpus testa il `RuleEngine`, che verrà implementato in Fase 1. Non ha senso creare il corpus prima del codice testato. **La PIANIFICA della Fase 1 DEVE includere come task esplicito**:

- Task 1.x — Definire formato di rappresentazione del board (FEN-like vs explicit) — `PLAN-fase-0.md` §4 Task 0.3 e CLAUDE.md §2.4.4 lo elencano come decisione di Fase 1.
- Task 1.y — Creare `shared/src/test/resources/test-positions.json` con le 48 posizioni minime.
- Task 1.z — Implementare `RuleEngineCorpusTest` parametrizzato sul corpus.

Per Fase 0 la scelta è **non creare un file vuoto**: meglio lasciare l'assenza visibile che generare un placeholder semanticamente vacuo.

---

## 5. Traceability matrix

`tests/TRACEABILITY.md` allo stato corrente: tre tabelle vuote (FR / NFR / AC) con header coerente.

**Verifica regola CLAUDE.md §2.4.3**: ad ogni test aggiunto, una riga in matrice. In Fase 0 sono stati aggiunti 4 smoke test che non coprono alcun FR/NFR/AC — corretto non aggiungerli alla matrice. Quando in Fase 1 verranno introdotti `RuleEngineTest`, `RuleEngineCorpusTest`, ecc., ogni nuovo test produrrà righe come:

```markdown
| FR-SP-04 | Highlight mosse legali | RuleEngineTest | shouldGenerateLegalMovesForMan | Unit |
| FR-SP-05 | Cattura obbligatoria   | RuleEngineTest | shouldEnforceMandatoryCapture  | Unit |
| ...
```

**Stato Fase 0**: matrice vuota e coerente con il fatto che nessun FR/NFR è oggetto di copertura.

---

## 6. Naming convention applicata

CLAUDE.md §2.4.5:

| Convenzione                                          | Verifica in Fase 0                                         |
|------------------------------------------------------|------------------------------------------------------------|
| Unit test: `<ClasseProduzione>Test`                  | n/a (no classi produzione in Fase 0)                       |
| Unit test alternativa: `<Modulo>SmokeTest`           | ✅ Tutti e 4 gli smoke test seguono questa naming          |
| Integration test: `<Feature>IntegrationTest`         | n/a (no integration in Fase 0)                             |
| E2E test: `<Feature>E2ETest`                         | n/a                                                        |
| Test method `should<...>_when<...>` o `<Feature><Scenario>` | Smoke usa `buildPipelineIsAlive` (allineato all'intent del test, non a uno scenario di feature) |

---

## 7. Test infrastrutturali NON funzionali (gate di build)

I quality gate configurati nel parent POM agiscono come "test infrastrutturali" anche se non sono test JUnit. Verificati in Fase 0:

| Gate                                  | Strumento                          | Esito Fase 0 (commit `c90c1eb`) |
|---------------------------------------|-------------------------------------|------------------------------------|
| Build environment (Maven, Java)       | `maven-enforcer-plugin`            | ✅ Maven 3.9.9, Java 21.0.9        |
| Dependency convergence                | `maven-enforcer-plugin` (regola `dependencyConvergence` aggiunta in chiusura REVIEW) | ✅ Nessuna divergenza |
| Code formatting                       | Spotless 2.43 / google-java-format  | ✅ Tutti i file conformi           |
| Static analysis                       | SpotBugs 4.8 (threshold High)       | ✅ 0 warning                       |
| Coverage report (HTML)                | JaCoCo 0.8.12                       | ✅ Generato per tutti i 4 moduli   |
| Coverage threshold                    | JaCoCo `check`                      | ⏸️ Placeholder 0% (attivazione reale in Fase 1+) |

---

## 8. Validazione Definition of Done della Fase 0

Riferimento: `plans/PLAN-fase-0.md` §10.

- [x] Tutti i task 0.1 ÷ 0.9 completati e committati (10 commit IMPLEMENTA + 1 commit revisione post-feedback + 2 commit chiusura REVIEW).
- [x] Acceptance criteria A0.1 ÷ A0.5, A0.8 ÷ A0.11 verificati direttamente.
- [x] Acceptance A0.6 (CI verde): superseded da **ADR-019** (workflow disattivato fino a repo remoto). Rete di sicurezza coperta dai comandi Maven locali (`mvn verify` = build+lint+sast).
- [x] Acceptance A0.7 (Docker compose): superseded da **ADR-018** (MySQL locale).
- [x] `mvn clean verify` da root: BUILD SUCCESS.
- [x] `AI_CONTEXT.md` riflette stato corrente.
- [x] Sotto-fase REVIEW eseguita → `reviews/REVIEW-fase-0.md` creato e chiuso (2026-04-28).
- [x] Findings REVIEW risolti (F-001 ÷ F-004, F-007, F-008) o ACKNOWLEDGED (F-005, F-006, F-009 ÷ F-013).
- [x] Sotto-fase TEST eseguita → questo documento.
- [ ] Tag git `v0.0.0` applicato sul commit di chiusura. ⏳ **Da applicare dopo conferma utente.**

---

## 9. Closure della sotto-fase TEST

Riferimento: CLAUDE.md §2.4.6.

- [x] Coverage target raggiunti — n/a in Fase 0 (no codice di dominio); ribadito che le soglie reali entrano in Fase 1+.
- [x] Traceability matrix aggiornata — corretta nello stato vuoto in Fase 0; popolazione inizia in Fase 1.
- [x] Test corpus regole italiane aggiornato — n/a in Fase 0; creazione obbligatoria in Fase 1.
- [x] `mvn verify` passa pulito su tutti i moduli — verificato (38s, BUILD SUCCESS).
- [x] Test plan `tests/TEST-PLAN-fase-0.md` documenta scelte e copertura — questo documento.
- [x] Nessun test in stato `@Disabled` o `@Ignore` senza issue tracciata — verificato (0 test disabilitati).

**TEST chiuso il**: 2026-04-28
**Commit di chiusura**: aggiornato dopo l'eventuale tag `v0.0.0`.
