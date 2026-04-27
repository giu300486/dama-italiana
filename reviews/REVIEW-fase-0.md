# REVIEW — Fase 0: Setup infrastruttura

- **Data**: 2026-04-27
- **Commit codebase**: `c6d8ee8`
- **SPEC version**: 2.0 (2026-04-26)
- **Reviewer**: Claude Code
- **Piano di riferimento**: [`plans/PLAN-fase-0.md`](../plans/PLAN-fase-0.md)

---

## Sommario

| Categoria       | Critical | High | Medium | Low | Totale |
|-----------------|---------:|-----:|-------:|----:|-------:|
| BLOCKER         |        0 |    0 |      0 |   0 |      0 |
| REQUIREMENT_GAP |        0 |    0 |      0 |   0 |      0 |
| BUG             |        0 |    0 |      0 |   0 |      0 |
| SECURITY        |        0 |    0 |      0 |   1 |      1 |
| PERFORMANCE     |        0 |    0 |      0 |   0 |      0 |
| CODE_QUALITY    |        0 |    0 |      1 |   6 |      7 |
| DOC_GAP         |        0 |    0 |      0 |   5 |      5 |
| **Totale**      |        0 |    0 |      1 |  12 |     13 |

**Lettura sintetica**: nessun finding bloccante per chiusura fase. Un finding `Medium` (F-001, regola Enforcer) è raccomandato come fix immediato. Tutti gli altri sono `Low`: 4 sono fix opzionali brevi, 9 sono `ACKNOWLEDGED` (scelte consapevoli o debiti tracciati con scadenza in fasi successive).

---

## Acceptance criteria coverage

Riferimento: `plans/PLAN-fase-0.md` §2.

| AC ID  | Descrizione                                                  | Status         | Note                                                                                                              |
|--------|--------------------------------------------------------------|----------------|-------------------------------------------------------------------------------------------------------------------|
| A0.1   | `mvn clean verify` BUILD SUCCESS                             | ✅ COVERED     | Verificato 2026-04-27 19:54: parent + 4 moduli, 38s.                                                              |
| A0.2   | 4 moduli registrati nel parent POM                           | ✅ COVERED     | `pom.xml` `<modules>`: shared, core-server, client, server.                                                       |
| A0.3   | `spotless:check` passa                                       | ✅ COVERED     | Eseguito da `mvn verify` (parent build/plugins).                                                                  |
| A0.4   | JaCoCo HTML report per ogni modulo                           | ✅ COVERED     | `<modulo>/target/site/jacoco/index.html` esistente per tutti i 4 moduli.                                          |
| A0.5   | SpotBugs senza errori High                                   | ✅ COVERED     | Output `BugInstance size is 0` su tutti i moduli.                                                                 |
| A0.6   | CI GitHub Actions verde su push/PR                           | ⏸️ DEFERRED    | Workflow disattivato in ADR-019 (no repository remoto disponibile). Rimossa dall'acceptance bloccante.            |
| A0.7   | `docker compose up -d` healthy                               | ⛔ SUPERSEDED  | Docker Compose rimosso in ADR-018. Setup DB ora documentato come MySQL locale + procedura SQL manuale.            |
| A0.8   | File workflow SDD esistono                                   | ✅ COVERED     | `AI_CONTEXT.md`, `ARCHITECTURE.md`, `CHANGELOG.md`, `README.md`, `tests/TRACEABILITY.md`.                         |
| A0.9   | Directory `plans/`, `reviews/`, `tests/` con `.gitkeep`      | ✅ COVERED     | Tutte e tre presenti.                                                                                              |
| A0.10  | `tests/TRACEABILITY.md` skeleton                              | ✅ COVERED     | Tabelle FR/NFR/AC vuote, pronte per Fase 1.                                                                        |
| A0.11  | `.gitignore` copre target/IDE/secret                          | ✅ COVERED     | Maven, IntelliJ/Eclipse/VSCode, OS, secret (`.env`, `*.key`, `*.pem`).                                            |

**Coverage netta**: 9/11 verificati direttamente, 2/11 archiviati con ADR. Acceptance della fase considerati soddisfatti.

---

## Findings

### F-001 — [CODE_QUALITY, Medium] Maven Enforcer: regola `dependencyConvergence` mancante

- **Posizione**: `pom.xml:312-335` (parent `<build><plugins>`, execution `enforce-build-environment`).
- **SPEC reference**: nessuna diretta. PLAN sez. 4, Task 0.2: la regola era esplicitamente prevista.
- **Descrizione**: Le regole attive sono solo `requireMavenVersion` e `requireJavaVersion`. La regola `dependencyConvergence` documentata nel PLAN non è stata implementata. La sua assenza permette a versioni transitive divergenti (es. due librerie che chiedono Jackson 2.17 vs 2.18) di convivere senza warning. Con il BOM Spring Boot ben gestito è raro che si presentino problemi reali a Fase 0, ma in Fase 5+ (server con molte dipendenze native) o in Fase 7 (client + Jetty) può nascondere regressioni di compatibilità.
- **Proposta di fix**: aggiungere `<dependencyConvergence/>` come terza regola nell'execution `enforce-build-environment`. Eseguire `mvn verify`: se emergono divergenze, esplicitarle in `<dependencyManagement>` del parent.
- **Status**: RESOLVED in commit di chiusura review. Aggiunto `<dependencyConvergence/>` alle regole Enforcer; `mvn verify` passa pulito senza divergenze (Spring Boot BOM gestisce correttamente).

---

### F-002 — [CODE_QUALITY, Low] `spring-boot-maven-plugin` con configurazione vestigiale Lombok

- **Posizione**: `server/pom.xml:145-153`.
- **SPEC reference**: nessuna.
- **Descrizione**: Il plugin esclude `org.projectlombok:lombok` dal repackage, ma Lombok non è dichiarato come dipendenza in nessun modulo e il progetto non lo usa (Records + sealed types coprono lo use case secondo CLAUDE.md §4.1). Configurazione copia-incolla "ricordo del template Spring Initializr".
- **Proposta di fix**: rimuovere il blocco `<configuration><excludes>...</excludes></configuration>` dal plugin. Il plugin resta dichiarato per ereditare il binding `repackage` dal Spring Boot BOM.
- **Status**: RESOLVED in commit di chiusura review.

---

### F-003 — [CODE_QUALITY, Low] `application.yml`: password vuota di default maschera errori di setup

- **Posizione**: `server/src/main/resources/application.yml:13`.
- **SPEC reference**: nessuna diretta. NFR-S generale (no secret in chiaro).
- **Descrizione**: `password: ${DB_PASSWORD:}` con stringa vuota come default. Se un developer avvia il server senza esportare `DB_PASSWORD`, il datasource prova a connettersi con password vuota e MySQL restituisce `Access denied for user 'dama'@'localhost'`, errore poco diagnostico che fa pensare a un problema di credenziali piuttosto che di setup. Meglio omettere il default così Spring fallisce con `Could not resolve placeholder 'DB_PASSWORD'`, che indica chiaramente il problema. Il README documenta già la necessità dell'env var, quindi nessun impatto sulla DX corretta.
- **Proposta di fix**: `password: ${DB_PASSWORD}` (senza `:`).
- **Status**: RESOLVED in commit di chiusura review. Commento esplicativo aggiunto sopra la riga.

---

### F-004 — [CODE_QUALITY, Low] SpotBugs non attivo a livello parent

- **Posizione**: `pom.xml:312-342` (parent `<build><plugins>`).
- **SPEC reference**: nessuna diretta. NFR-M generale.
- **Descrizione**: SpotBugs è dichiarato in `<pluginManagement>` ma non attivato in `<build><plugins>` del parent. Ogni modulo deve dichiararlo esplicitamente. Tutti i 4 moduli attuali lo fanno, ma in fasi future un nuovo modulo potrebbe dimenticarlo: la rete di sicurezza non sarebbe globale come Spotless ed Enforcer.
- **Proposta di fix**: aggiungere il plugin in `<build><plugins>` del parent (eredita la configurazione `effort=Max threshold=High` da `<pluginManagement>`):
  ```xml
  <plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
  </plugin>
  ```
  Sul parent (packaging=pom) non gira nulla perché manca `target/classes`, quindi nessun side effect.
- **Status**: RESOLVED in commit di chiusura review. SpotBugs ora ereditato globalmente; build passa pulito.

---

### F-005 — [DOC_GAP, Low] Smoke test con asserzione triviale

- **Posizione**: `<modulo>/src/test/java/.../<Modulo>SmokeTest.java` (4 file).
- **SPEC reference**: nessuna.
- **Descrizione**: I test smoke contengono `assertThat("modulo").isEqualTo("modulo")`. L'asserzione è sempre vera: i test esistono solo per validare il classpath e attivare JaCoCo. Il PLAN già documenta che vanno rimossi (Fase 1 per shared, Fase 4 per core-server, Fase 3 per client, Fase 5 per server). Il debito è tracciato anche in `AI_CONTEXT.md` e nel javadoc di ogni smoke test.
- **Proposta di fix**: nessuna ora. La REVIEW di ogni fase successiva DEVE verificare che il rispettivo smoke test sia stato eliminato e sostituito con test reali; in caso contrario, alzare un finding.
- **Status**: ACKNOWLEDGED

---

### F-006 — [DOC_GAP, Low] JaCoCo `check` con regola placeholder

- **Posizione**: `pom.xml:248-271` (parent `<pluginManagement>`, execution `jacoco-check`).
- **SPEC reference**: NFR-M-01 (≥80% shared), NFR-M-02 (≥60% client/server).
- **Descrizione**: La regola attiva è `<minimum>0.00</minimum>` (sempre soddisfatta), coerente con `haltOnFailure=false` in Fase 0 e con il commento esplicito nel POM. La regola "vera" deve essere wired in Fase 1 per `shared` (≥80%), in Fase 3 per `client` (≥60%), in Fase 5 per `server` (≥70%), in Fase 4 per `core-server` (≥80%).
- **Proposta di fix**: nessuna ora. Il PIANO di Fase 1 DEVE includere come task: "JaCoCo check con soglia ≥80% per shared, `haltOnFailure=true`".
- **Status**: ACKNOWLEDGED

---

### F-007 — [DOC_GAP, Low] `javafx-maven-plugin` senza `mainClass` nel client

- **Posizione**: `client/pom.xml:115-118`.
- **SPEC reference**: SPEC §13 (UI), Fase 3 roadmap.
- **Descrizione**: Il plugin è dichiarato con tag vuoto. Eseguire `mvn -pl client javafx:run` ora fallisce perché manca `<mainClass>`. Il README in tabella "comandi standard" annota "(da Fase 3)" ma il significato può sfuggire a un nuovo lettore.
- **Proposta di fix**: aggiungere un commento XML accanto al plugin: `<!-- mainClass added in Fase 3 (com.damaitaliana.client.ClientApplication). -->`. Nessuna modifica al README.
- **Status**: RESOLVED in commit di chiusura review. Commento `TODO Fase 3` aggiunto al plugin in `client/pom.xml`.

---

### F-008 — [CODE_QUALITY, Low] `.gitignore` voci ridondanti

- **Posizione**: `.gitignore:53` e `.gitignore:54`.
- **SPEC reference**: nessuna.
- **Descrizione**: `*.exec` (riga 53) cattura già `jacoco.exec` (riga 54). La voce specifica è ridondante. Rumore minimale.
- **Proposta di fix**: rimuovere la riga `jacoco.exec`.
- **Status**: RESOLVED in commit di chiusura review.

---

### F-009 — [CODE_QUALITY, Low] `package-info.java` elenca sotto-package futuri che possono drift-are

- **Posizione**: `<modulo>/src/main/java/com/damaitaliana/<modulo>/package-info.java` (4 file).
- **SPEC reference**: nessuna.
- **Descrizione**: Es. `client/package-info.java`: "Sub-packages added in Fase 3+: ui, controller, network, lan.discovery, lan.host, persistence". Se in Fase 3 si decide di rinominare (es. `network` → `transport`), il commento diventa stale e potenzialmente fuorviante.
- **Proposta di fix**: nessuna ora. La REVIEW di Fase 1, 3, 4, 5 DEVE controllare che il rispettivo `package-info.java` sia allineato alla realtà del codice, e aggiornarlo.
- **Status**: ACKNOWLEDGED

---

### F-010 — [DOC_GAP, Low] `spotbugs-exclude.xml` previsto dal PLAN ma non creato

- **Posizione**: `plans/PLAN-fase-0.md` §6 (Rischio R-06).
- **SPEC reference**: nessuna.
- **Descrizione**: Il rischio R-06 menzionava di "Configurare `spotbugs-exclude.xml`". Il file non è stato creato. È OK in pratica (la codebase è quasi vuota e SpotBugs non emette warning), ma il PLAN ne suggeriva la presenza.
- **Proposta di fix**: nessuna ora. Documentare in REVIEW-fase-1 (o successive) che il file verrà creato solo quando emergerà una falsa positiva concreta da escludere; non c'è motivo di crearlo vuoto.
- **Status**: ACKNOWLEDGED

---

### F-011 — [CODE_QUALITY, Low] Spotless `googleJavaFormat` produce wrap aggressivo nei javadoc

- **Posizione**: `<modulo>/src/main/java/.../package-info.java` (es. dove `{@code rules}` viene wrappato a fine riga).
- **SPEC reference**: NFR-M-04.
- **Descrizione**: Il preset `googleJavaFormat` con `style=GOOGLE` (2 spazi) wrappa `{@code rules}` su due righe quando supera la lunghezza massima. Esteticamente strano ma è il comportamento standard del tool. NFR-M-04 cita esplicitamente Google Java Style → questa è la fonte di verità.
- **Proposta di fix**: nessuna; convivere col formattatore.
- **Status**: ACKNOWLEDGED

---

### F-012 — [SECURITY, Low] `spring-boot-starter-security` senza configurazione esplicita

- **Posizione**: `server/pom.xml:51-54` (dipendenza presente) + `server/src/main/resources/application.yml` (nessuna sezione `spring.security`).
- **SPEC reference**: NFR-S-01, NFR-S-02 (BCrypt ≥12, JWT 15min/7gg).
- **Descrizione**: `spring-boot-starter-security` è dipendenza del modulo `server` ma non c'è ancora `SecurityFilterChain`. Spring genera all'avvio una password admin random e la stampa nei log. Per Fase 0 OK perché il server non viene avviato; in Fase 5 va sostituita con la security custom (registrazione, JWT, BCrypt cost 12).
- **Proposta di fix**: PLAN di Fase 5 DEVE prevedere come task esplicito "configurare `SecurityFilterChain` custom, BCrypt encoder cost 12, JWT filter".
- **Status**: ACKNOWLEDGED

---

### F-013 — [DOC_GAP, Low] `CHANGELOG.md` sezione `Fixed` vuota in Fase 0

- **Posizione**: `CHANGELOG.md` `[Unreleased]` § Fixed.
- **SPEC reference**: nessuna.
- **Descrizione**: Sezione `Fixed` è vuota con placeholder `_(nessuna voce in Fase 0)_`. Coerente con Keep a Changelog 1.1 (la sezione esiste anche se vuota a fine fase). Nessun problema reale.
- **Proposta di fix**: nessuna.
- **Status**: ACKNOWLEDGED

---

## SPEC change requests

> Vuota. La Fase 0 è puramente infrastrutturale e non tocca requisiti SPEC. ADR-018 e ADR-019 (in `ARCHITECTURE.md`) sono decisioni operative interne che non modificano nulla in `SPEC.md`.

---

## Raccomandazione di fix prioritari

In ordine di priorità per chiudere la review:

1. **F-001** (CODE_QUALITY Medium) — aggiungere `dependencyConvergence` all'Enforcer. ~3 righe XML, beneficio futuro reale.
2. **F-004** (CODE_QUALITY Low) — attivare SpotBugs nel parent `<build><plugins>`. ~4 righe XML, rete di sicurezza globale.
3. **F-002** (CODE_QUALITY Low) — rimuovere config Lombok vestigiale dal `server/pom.xml`. ~5 righe.
4. **F-003** (CODE_QUALITY Low) — rimuovere il default vuoto da `${DB_PASSWORD:}`. 1 carattere.
5. **F-007** (DOC_GAP Low) — commento sul `javafx-maven-plugin` nel `client/pom.xml`. 1 riga.
6. **F-008** (CODE_QUALITY Low) — rimuovere riga ridondante da `.gitignore`. 1 riga.

Tutti gli altri findings sono `ACKNOWLEDGED`: scelte consapevoli o debiti che la REVIEW della fase di pertinenza dovrà controllare.

---

## Closure

Condizioni richieste da CLAUDE.md §2.3 per chiudere la review:

- [x] Tutti i `BLOCKER` risolti (0 finding)
- [x] Tutti i `REQUIREMENT_GAP` risolti (0 finding)
- [x] Tutti i `Critical/High` `BUG` risolti (0 finding)
- [x] Tutti i `Critical/High` `SECURITY` risolti (0 finding)
- [x] `PERFORMANCE` che violano NFR risolti (0 finding)
- [x] SPEC change requests con stato non-PENDING (nessuna CR)
- [x] Findings Medium/Low concordati con utente: F-001 ÷ F-004, F-007, F-008 → RESOLVED. F-005, F-006, F-009 ÷ F-013 → ACKNOWLEDGED (deferred a fasi successive con tracciamento esplicito).

**Esito post-fix**: `mvn clean verify` → `BUILD SUCCESS` in 38s con `dependencyConvergence` attivo, SpotBugs ereditato globalmente, 0 warning.

**Review chiusa il**: 2026-04-28
**Commit di chiusura**: `983446b` (fix(arch): apply REVIEW-fase-0 findings F-001 to F-008).
