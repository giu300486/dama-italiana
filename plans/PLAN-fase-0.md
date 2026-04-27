# PLAN — Fase 0: Setup infrastruttura

- **Riferimento roadmap**: `SPEC.md` sezione 16 — Fase 0.
- **SPEC version**: 2.0 (2026-04-26).
- **Data piano**: 2026-04-27.
- **Autore**: Claude Code.
- **Stato**: DRAFT — in attesa di approvazione utente.

---

## 1. Scopo della fase

Predisporre l'intera infrastruttura di sviluppo del progetto **Dama Italiana Multiplayer** in modo che:

1. Il monorepo Maven multi-modulo sia compilabile end-to-end.
2. Tutti i quality gate automatici (formato codice, coverage, static analysis) siano configurati e funzionanti, anche se applicati a una codebase ancora vuota.
3. La pipeline CI esegua lo stesso `mvn verify` che gira in locale.
4. I file di workflow Spec-Driven Development (`AI_CONTEXT.md`, `ARCHITECTURE.md`, `CHANGELOG.md`, struttura `plans/`, `reviews/`, `tests/`) siano in posizione e popolati con il loro skeleton.
5. L'ambiente di sviluppo locale (MySQL via Docker Compose) sia avviabile con un singolo comando.

Nessuna logica di dominio in questa fase. Nessuna dipendenza framework di runtime nei moduli, oltre a quelle strettamente necessarie per il bootstrap.

---

## 2. Acceptance criteria

Da `SPEC.md` sezione 16 (Fase 0):

> **Acceptance**: `mvn clean verify` completa con successo a tutti i moduli.

**Criterio operativo esteso** (per non rimandare bug strutturali alle fasi successive):

| ID  | Criterio                                                                                              | Verificabile come                                              |
|-----|-------------------------------------------------------------------------------------------------------|----------------------------------------------------------------|
| A0.1 | Il comando `mvn clean verify` dalla root completa con `BUILD SUCCESS`.                                | Output Maven                                                   |
| A0.2 | Tutti e 4 i moduli (`shared`, `core-server`, `client`, `server`) sono presenti e referenziati nel parent POM. | `mvn -pl shared,core-server,client,server help:effective-pom` |
| A0.3 | Il check Spotless passa (`mvn spotless:check`).                                                       | Output Maven                                                   |
| A0.4 | JaCoCo genera report (`target/site/jacoco/`) per ogni modulo, anche se vuoti.                         | Esistenza file                                                 |
| A0.5 | SpotBugs gira senza errori High su codebase vuota.                                                    | Output SpotBugs                                                |
| A0.6 | La pipeline GitHub Actions esegue `mvn verify` su push verso `main` e su PR.                          | Esecuzione workflow su un branch di test                       |
| A0.7 | `docker compose up -d` avvia MySQL 8 sano (healthcheck verde).                                        | `docker compose ps`, `docker compose logs mysql`               |
| A0.8 | Esistono i file `AI_CONTEXT.md`, `ARCHITECTURE.md`, `CHANGELOG.md`, `README.md` con skeleton coerente con CLAUDE.md §6. | Esistenza file + lettura visiva |
| A0.9 | Esistono le directory `plans/`, `reviews/`, `tests/` con README placeholder o `.gitkeep`.             | Esistenza directory                                            |
| A0.10 | Esiste `tests/TRACEABILITY.md` skeleton (header + tabelle vuote pronte ad accogliere righe da Fase 1). | Esistenza file                                                 |
| A0.11 | `.gitignore` copre `target/`, `*.iml`, `.idea/`, `~/.dama-italiana/` non applicabile (è in home utente, non nel repo) — coperti file di build, IDE, secret. | Verifica file |

---

## 3. Requisiti SPEC coperti

### 3.1 Funzionali

Nessun FR di dominio è coperto in Fase 0 (la fase è puramente infrastrutturale). Tutti gli FR del paragrafo 4 di SPEC sono delegati alle fasi successive.

### 3.2 Non funzionali coperti (configurazione, non verifica)

| NFR ID    | SPEC ref | Coperto in fase 0 come                                                      |
|-----------|----------|-----------------------------------------------------------------------------|
| NFR-M-01  | §5       | Configurazione JaCoCo con soglia ≥ 80% per `shared` (`haltOnFailure` attivo dalla Fase 1, in Fase 0 informativo). |
| NFR-M-02  | §5       | Configurazione JaCoCo con soglia ≥ 60% per `client` e `server`.             |
| NFR-M-03  | §5       | Predisposizione `springdoc-openapi` come dipendenza in `server` (non attivata in Fase 0; verificata in Fase 5). |
| NFR-M-04  | §5       | Plugin Spotless con preset Google Java Style configurato sul parent POM.    |
| NFR-O-01  | §5       | Predisposizione dipendenza `micrometer-registry-prometheus` nel `server` (non esposta in Fase 0). |
| NFR-O-02  | §5       | Predisposizione `logstash-logback-encoder` nel `server`.                    |

> **Nota**: in Fase 0 questi NFR sono "configurati", non "verificati". La verifica avviene quando la codebase produce output osservabile (Fasi 1+).

### 3.3 ADR coinvolti

Decisioni già fissate dallo SPEC che vincolano la Fase 0 (Appendice B):

| ADR     | Vincolo per Fase 0 |
|---------|--------------------|
| ADR-001 | Java 21 LTS in tutti i POM (`<maven.compiler.release>21</maven.compiler.release>`). |
| ADR-002 | Maven multi-modulo. |
| ADR-006 | Spring Boot 3.4+ → versione gestita nel parent. |
| ADR-007 | MySQL 8 in `docker-compose.yml`. |
| ADR-010 | Esattamente 4 sotto-moduli. |

---

## 4. Decomposizione in task

I task sono ordinati. Ogni task ha precondizioni esplicite. Non saltare l'ordine senza documentare in `AI_CONTEXT.md`.

### Task 0.1 — Inizializzazione file di workflow SDD

**Output**:
- `AI_CONTEXT.md` (skeleton da CLAUDE.md §6.1).
- `ARCHITECTURE.md` (skeleton: introduzione + tabella ADR già fissati dallo SPEC come riferimento, sezione "Decisioni nuove" vuota).
- `CHANGELOG.md` (formato Keep a Changelog 1.1.0, sezione `[Unreleased]` con sottosezioni `Added/Changed/Fixed/Removed`).
- `README.md` (introduzione progetto, link a SPEC, comandi standard da CLAUDE.md §5, prerequisiti).
- `plans/.gitkeep` (PLAN-fase-0.md è già qui, ma `.gitkeep` evita che la dir sparisca quando si svuota).
- `reviews/.gitkeep`.
- `tests/.gitkeep`.
- `tests/TRACEABILITY.md` skeleton con tre tabelle vuote (FR / NFR / AC) e intestazioni.
- `.gitignore` per Java + Maven + IDEA/Eclipse/VSCode + OS (Windows/macOS).
- `.editorconfig` (UTF-8, LF, 4 spazi Java, 2 spazi YAML/XML).
- `.gitattributes` (text auto + line endings consistenti).

**Moduli toccati**: nessuno (root del repo).

**Dipendenze**: nessuna.

### Task 0.2 — Parent POM

**Output**: `pom.xml` alla root con:

- `groupId = com.damaitaliana`, `artifactId = dama-italiana-parent`, `version = 0.1.0-SNAPSHOT`, `packaging = pom`.
- Java 21 (`maven.compiler.release`).
- Encoding UTF-8.
- `<modules>` con i 4 sottomoduli.
- `<dependencyManagement>` con BOM per:
  - Spring Boot 3.4.x (BOM completo).
  - Jackson (allineato a Spring Boot).
  - JUnit 5 BOM.
  - AssertJ 3.26+.
  - Mockito 5.x.
  - SLF4J / Logback (gestiti da Spring Boot BOM).
  - JmDNS 3.5.x.
  - Ikonli 12+.
  - JavaFX 21+.
  - JJWT 0.12+.
  - Flyway 10+.
  - mysql-connector-j 9.x.
  - springdoc-openapi 2.x.
  - Caffeine 3.x.
  - Testcontainers BOM 1.x.
  - JmDNS, JJWT, JmDNS.
- `<pluginManagement>`:
  - `maven-compiler-plugin` (release=21).
  - `maven-surefire-plugin` (latest, con `--enable-preview` solo se servisse — NO, Java 21 è LTS, no preview).
  - `maven-failsafe-plugin` (per integration test, naming `*IT` per ora differito; in Fase 0 il binding è disponibile ma non eseguito).
  - `spotless-maven-plugin` con preset `googleJavaFormat` (versione AOSP a 4 spazi se conforme a Google Java Style — verificare default Spotless), import order, removeUnusedImports, formatAnnotations.
  - `jacoco-maven-plugin`:
    - `prepare-agent` legato a `initialize`.
    - `report` legato a `verify`.
    - `check` legato a `verify` con regole per modulo (impostate ai threshold target ma con `haltOnFailure=false` in fase 0; passa a `true` in Fase 1 per `shared`).
  - `spotbugs-maven-plugin` 4.8+:
    - Goal `check` legato a `verify`.
    - `effort=Max`, `threshold=High` (cioè blocca solo High; Medium e Low informativi).
    - Esclusioni vuote per ora.
  - `maven-enforcer-plugin`:
    - `requireMavenVersion ≥ 3.9`.
    - `requireJavaVersion = 21`.
    - `dependencyConvergence`.
- `<build>` con plugin minimi attivi sul parent: enforcer, spotless (apply opzionale, check obbligatorio).

**Moduli toccati**: root.

**Dipendenze**: Task 0.1.

**Note**:
- Le versioni esatte vanno fissate in fase di implementazione dopo `mvn versions:display-dependency-updates` su un POM minimal di prova; questa scelta è interna e non un trade-off architetturale (no stop point).
- I plugin attivi sui sotto-moduli si attivano via `<pluginManagement>` + dichiarazione esplicita nel modulo, in modo da poter spegnere chirurgicamente ad esempio JaCoCo su `shared` se contiene solo configurazione (non si applica in F0).

### Task 0.3 — Modulo `shared`

**Output**: `shared/pom.xml`, `shared/src/main/java/com/damaitaliana/shared/.gitkeep` (struttura sotto-package vuota), `shared/src/test/java/com/damaitaliana/shared/.gitkeep`.

**Caratteristiche**:
- Packaging `jar`.
- Parent: parent POM.
- Dipendenze: solo Jackson Databind (provided/runtime per DTO), JUnit 5 (test), AssertJ (test).
- Vincolo CLAUDE.md §8.7: niente Spring, JavaFX, JPA, WebSocket. Da scrivere come commento nel POM.
- Plugin attivi: spotless:check, jacoco:report (haltOnFailure=false in F0).

**Moduli toccati**: `shared/`.

**Dipendenze**: Task 0.2.

### Task 0.4 — Modulo `core-server`

**Output**: `core-server/pom.xml`, src skeleton.

**Caratteristiche**:
- Packaging `jar`.
- Parent: parent POM.
- Dipendenze: `shared`, `spring-context`, `spring-messaging` (per STOMP DTO compatibili — solo classi DTO, NO Tomcat/Jetty/JPA), JUnit 5, Mockito.
- Vincolo CLAUDE.md §8.8: niente Tomcat, Jetty, JPA. Comment nel POM.
- Plugin attivi: spotless, jacoco.

**Moduli toccati**: `core-server/`.

**Dipendenze**: Task 0.3.

### Task 0.5 — Modulo `client`

**Output**: `client/pom.xml`, src skeleton.

**Caratteristiche**:
- Packaging `jar`.
- Parent: parent POM.
- Dipendenze:
  - `shared`, `core-server`.
  - `spring-boot-starter` (NON `-web`).
  - `spring-boot-starter-websocket` con esclusione esplicita di `spring-boot-starter-tomcat` e inclusione di `spring-boot-starter-jetty` (vincolo CLAUDE.md §8.6 + ADR-005). Le coordinate di esclusione/inclusione vanno verificate dalla doc Spring Boot 3.4 al momento del setup.
  - JavaFX (`javafx-controls`, `javafx-fxml`, `javafx-media`, `javafx-graphics`).
  - `org.openjfx:javafx-maven-plugin` per il goal `javafx:run`.
  - Ikonli (`ikonli-javafx`, `ikonli-materialdesign2-pack`, `ikonli-fontawesome5-pack`).
  - JmDNS.
  - Jackson.
  - SLF4J + Logback.
  - JUnit 5, AssertJ, TestFX, Mockito (test).
  - jpackage NON è una dipendenza Maven; predisposizione del config in Fase 11.
- Plugin attivi: spotless, jacoco (soglia 60% futura), `javafx-maven-plugin` con `mainClass` placeholder (es. `com.damaitaliana.client.ClientApplication` — la classe sarà creata in Fase 3, in F0 si lascia commentato o si usa una classe `Bootstrap` no-op per non rompere `mvn verify`).

**Decisione operativa**: in Fase 0 la classe `ClientApplication` può essere un `public static void main` no-op o assente (in tal caso il plugin javafx non viene eseguito durante `verify`). Si predispone solo. **Non è uno stop point**: scelta interna, reversibile.

**Moduli toccati**: `client/`.

**Dipendenze**: Task 0.4.

### Task 0.6 — Modulo `server`

**Output**: `server/pom.xml`, `server/src/main/resources/application.yml` (skeleton: porta 8080, datasource configurata via env var senza default password), `server/src/main/resources/db/migration/.gitkeep`, src skeleton.

**Caratteristiche**:
- Packaging `jar`.
- Parent: parent POM.
- Dipendenze:
  - `shared`, `core-server`.
  - `spring-boot-starter-web`.
  - `spring-boot-starter-websocket`.
  - `spring-boot-starter-security`.
  - `spring-boot-starter-data-jpa`.
  - `spring-boot-starter-validation`.
  - `spring-boot-starter-actuator`.
  - `mysql-connector-j` (runtime).
  - `flyway-core` + `flyway-mysql`.
  - `caffeine`.
  - `jjwt-api` + `jjwt-impl` + `jjwt-jackson`.
  - `springdoc-openapi-starter-webmvc-ui`.
  - `micrometer-registry-prometheus`.
  - `logstash-logback-encoder`.
  - Test: `spring-boot-starter-test`, `testcontainers-mysql`, `testcontainers-junit-jupiter`, `h2` (per test rapidi).
- Plugin attivi: spotless, jacoco (soglia 70% futura), `spring-boot-maven-plugin` con goal `repackage`.
- `application.yml` minimal:
  ```yaml
  spring:
    datasource:
      url: jdbc:mysql://localhost:3306/dama_italiana
      username: ${DB_USERNAME}
      password: ${DB_PASSWORD}
    jpa:
      open-in-view: false
    flyway:
      enabled: true
  ```
  Nessuna password committata (vincolo NFR-S e CLAUDE.md §8.5).

**Moduli toccati**: `server/`.

**Dipendenze**: Task 0.5.

### Task 0.7 — Docker Compose dev

**Output**: `docker-compose.yml` alla root.

**Contenuto**:
- Servizio `mysql`:
  - Image `mysql:8.0` (versione fissata, non `latest`).
  - Volume nominato per persistenza tra restart (`mysql_data:/var/lib/mysql`).
  - Env: `MYSQL_DATABASE=dama_italiana`, `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD` letti da `.env` o defaults dev (documentati in README).
  - Healthcheck `mysqladmin ping`.
  - Porta `3306:3306`.
- Eventuale `phpmyadmin` o `adminer` come servizio comodità (`adminer` su porta 8081). **Decisione**: includere `adminer` perché peso minimo, utile in dev. Reversibile.
- File `.env.example` committato (con valori dev placeholder), `.env` in `.gitignore`.

**Moduli toccati**: root.

**Dipendenze**: Task 0.1 (gitignore presente).

### Task 0.8 — CI GitHub Actions

**Output**: `.github/workflows/ci.yml`.

**Contenuto**:
- Trigger: `push` su `main`, `pull_request` su `main`, `workflow_dispatch`.
- Job `build`:
  - `runs-on: ubuntu-latest`.
  - `actions/checkout@v4`.
  - `actions/setup-java@v4` con `distribution: temurin`, `java-version: 21`, `cache: maven`.
  - Step `mvn -B -ntp clean verify` (no `-DskipTests`).
- Job `lint` (parallelo o nella stessa pipeline):
  - `mvn -B -ntp spotless:check` esplicito (anche se incluso in verify, lo si tiene separato per failure più chiari).
- Job `sast`:
  - `mvn -B -ntp spotbugs:check`.
- (Opzionale F0) Job `coverage-report`:
  - Upload artifact JaCoCo HTML per consultazione su PR.
- **Non includere** in F0:
  - Build Docker image server (Fase 11).
  - Deploy / release (Fase 11).
  - Test integration con Testcontainers (richiede Docker-in-Docker o servizio MySQL attivo nel runner — predisposizione possibile ma rinviata a Fase 5 quando serve davvero).

**Moduli toccati**: root, `.github/`.

**Dipendenze**: Task 0.2 ÷ 0.7.

### Task 0.9 — Smoke verify

**Output**: nessuno (azione di verifica).

**Procedura**:
1. `mvn clean verify` da root → deve passare.
2. `mvn spotless:apply && mvn spotless:check` → deve passare.
3. `mvn jacoco:report` → genera HTML report (vuoti ma esistono).
4. `docker compose up -d mysql` → healthcheck verde entro 30s.
5. `docker compose down -v` → cleanup.
6. Push branch di test su GitHub → workflow CI verde (verifica visiva su Actions).

**Moduli toccati**: tutti.

**Dipendenze**: tutti i task precedenti.

---

## 5. Strategia di test (Fase 0)

In Fase 0 **non c'è codice di produzione**, quindi non c'è codice di test funzionale. Tuttavia:

1. Si committa **un test smoke per modulo** che invoca `assertTrue(true)` in JUnit 5. Scopo: verificare che la pipeline test esegua, che AssertJ sia in classpath, che i report JaCoCo siano generati. Naming: `<modulo>SmokeTest`.
2. JaCoCo `check` con `haltOnFailure=false` su tutti i moduli in Fase 0; gli soglie reali (NFR-M-01, NFR-M-02) entreranno in vigore a partire dalla Fase 1 con `haltOnFailure=true` quando codice di dominio inizia a popolare la coverage.
3. Spotless check **obbligatorio** dalla Fase 0: tutto ciò che si committa deve passare il check.
4. SpotBugs check **obbligatorio** a livello High: codebase vuota → 0 warning.

I test smoke vengono **rimossi** non appena un modulo ha test reali (Fase 1 per `shared`, Fase 4 per `core-server`, Fase 3 per `client`, Fase 5 per `server`). Sono cantieri temporanei, tracciati nel CHANGELOG.

---

## 6. Rischi e mitigazioni

| ID  | Rischio                                                                                       | Probabilità | Impatto | Mitigazione                                                                                                                                                       |
|-----|-----------------------------------------------------------------------------------------------|-------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| R-01 | Conflitto versioni Spring Boot ↔ JavaFX ↔ Ikonli (transitive Jackson, SLF4J).                | Media       | Medio   | Usare Spring Boot BOM come unica fonte di verità per Jackson/SLF4J; allineare manualmente versione Ikonli con JavaFX 21.                                          |
| R-02 | Spotless `googleJavaFormat` AOSP vs non-AOSP (4 vs 2 spazi).                                  | Bassa       | Basso   | NFR-M-04 dice "Google Java Style Guide" → indentazione 2 spazi (default Google) o 4 (AOSP). **Stop point se non chiaro**. Default proposto: 2 spazi (Google standard). |
| R-03 | Esclusione Tomcat / inclusione Jetty in `client` ha sintassi non banale in Spring Boot 3.4.   | Media       | Medio   | Validare con minimal POM di prova prima di committare il `client/pom.xml`. Documentare il pattern in `ARCHITECTURE.md` come riferimento per Fase 7.                |
| R-04 | jpackage non disponibile sul runner CI o workflow Linux genera installer Linux only.          | Bassa       | Basso   | jpackage NON è in Fase 0, è in Fase 11. Non un rischio attuale.                                                                                                   |
| R-05 | Testcontainers richiede Docker nel runner GitHub Actions per il job `build`.                  | Bassa       | Medio   | I test Testcontainers non sono inclusi in F0. Verranno integrati in Fase 5 con `services: mysql:8.0` in workflow oppure docker-in-docker. Documentato nel piano F5. |
| R-06 | SpotBugs su codebase vuota può segnalare warning su classi generate (es. `module-info`).      | Bassa       | Basso   | Configurare `spotbugs-exclude.xml` con regola "excludeFromTest" e niente module-info in F0.                                                                       |
| R-07 | `mvn enforcer dependencyConvergence` può fallire per conflitti BOM tra Spring Boot e JavaFX.  | Media       | Medio   | Risolvere con `<dependencyManagement>` esplicito sui conflitti residui (es. forzare versione Jackson). In ultima istanza, esclusione mirata, non disabilitazione globale. |
| R-08 | Versioni JmDNS, Ikonli, springdoc rilasciate "ieri" potrebbero introdurre breaking change.    | Bassa       | Basso   | Pin su versione stabile più recente al momento dell'implementazione, documentata in `ARCHITECTURE.md` come "baseline F0".                                          |
| R-09 | `.env` o secret committato per errore.                                                        | Bassa       | Alto    | `.gitignore` strict (`*.env`, `*.env.local`), `.env.example` con valori placeholder, controllo manuale in `git status` prima di ogni commit del task 0.7.            |

---

## 7. Stop point e decisioni che richiedono utente

Sotto-fase PIANIFICA — punti che possono richiedere chiarimento prima di IMPLEMENTA:

1. **Spotless: 2 vs 4 spazi (Google standard vs AOSP)?** Default proposto in piano: **2 spazi (Google standard)**, allineato a NFR-M-04 letterale.
2. **GroupId Maven**: proposto `com.damaitaliana`. Coerente con package SPEC §6.6 (`com.damaitaliana.shared`, `com.damaitaliana.core`, `com.damaitaliana.client`, `com.damaitaliana.server`).
3. **Versione iniziale del progetto**: proposta `0.1.0-SNAPSHOT`. Verrà bumpata a `0.0.0` (chiusura F0 con tag `v0.0.0`) come indicato in CLAUDE.md §4.4 ("tag `v0.<fase>.0` ad ogni fase chiusa"). Il tag della Fase 0 sarà quindi `v0.0.0`.
4. **Adminer in `docker-compose.yml`**: utile in dev, ma non obbligatorio. Conferma o rimozione?
5. **CI: includere job di upload artifact JaCoCo**: comodo in PR per consultare coverage, ma costa tempo CI. Conferma o omissione?

Tutti gli altri punti sono coperti dallo SPEC e/o dai vincoli di CLAUDE.md e non richiedono decisione utente.

---

## 8. Stima di completamento

In numero di task (CLAUDE.md §2.1):

- Task 0.1 ÷ 0.9 → **9 task**.
- Ogni task termina con un commit Conventional Commits (CLAUDE.md §4.2). Esempi proposti:
  - `chore(claude): add SDD workflow scaffolding files`
  - `chore(arch): add parent POM with dependency management and quality plugins`
  - `chore(shared): add module skeleton`
  - `chore(core-server): add module skeleton`
  - `chore(client): add module skeleton with JavaFX + Spring Boot non-web`
  - `chore(server): add module skeleton with Spring Boot web`
  - `chore(arch): add Docker Compose with MySQL 8 for dev`
  - `chore(arch): add GitHub Actions CI pipeline`
  - `chore(arch): smoke verify Fase 0 setup` (può non essere un commit separato; tipicamente solo se serve correggere qualcosa post-smoke)

Aggiornamenti previsti durante IMPLEMENTA:
- `AI_CONTEXT.md` aggiornato dopo ogni task.
- `CHANGELOG.md` aggiornato in `[Unreleased]` dopo ogni task.
- `ARCHITECTURE.md` aggiornato con eventuali ADR nuovi se emergono (es. ADR-018 "Spotless Google standard a 2 spazi" se la decisione si formalizza).

---

## 9. Output finale della Fase 0

Albero file atteso a chiusura fase (escludendo `target/`):

```
dama-italiana/
├── .editorconfig
├── .env.example
├── .gitattributes
├── .github/
│   └── workflows/
│       └── ci.yml
├── .gitignore
├── AI_CONTEXT.md
├── ARCHITECTURE.md
├── CHANGELOG.md
├── CLAUDE.md                         (esistente, non toccato)
├── README.md
├── SPEC.md                           (esistente, non toccato)
├── docker-compose.yml
├── pom.xml                           (parent)
├── plans/
│   └── PLAN-fase-0.md                (questo file)
├── reviews/
│   └── .gitkeep
├── tests/
│   ├── .gitkeep
│   └── TRACEABILITY.md               (skeleton)
├── shared/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/damaitaliana/shared/.gitkeep
│       └── test/java/com/damaitaliana/shared/SharedSmokeTest.java
├── core-server/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/damaitaliana/core/.gitkeep
│       └── test/java/com/damaitaliana/core/CoreServerSmokeTest.java
├── client/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/damaitaliana/client/.gitkeep
│       └── test/java/com/damaitaliana/client/ClientSmokeTest.java
└── server/
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/damaitaliana/server/.gitkeep
        │   └── resources/
        │       ├── application.yml
        │       └── db/migration/.gitkeep
        └── test/java/com/damaitaliana/server/ServerSmokeTest.java
```

---

## 10. Definition of Done della Fase 0

- [ ] Tutti i task 0.1 ÷ 0.9 completati e committati.
- [ ] Acceptance criteria A0.1 ÷ A0.11 verificati.
- [ ] `mvn clean verify` da root: BUILD SUCCESS.
- [ ] CI GitHub Actions: ultimo run verde su `main`.
- [ ] `AI_CONTEXT.md` riflette stato "Fase 0 — IMPLEMENTA → REVIEW pending".
- [ ] Sotto-fase REVIEW (CLAUDE.md §2.3) eseguita → `reviews/REVIEW-fase-0.md` creato.
- [ ] Eventuali finding REVIEW risolti.
- [ ] Sotto-fase TEST (CLAUDE.md §2.4) eseguita → `tests/TEST-PLAN-fase-0.md` creato (per F0 sarà sintetico, dato che non c'è dominio da testare).
- [ ] Tag git `v0.0.0` applicato sul commit di chiusura.

---

**FINE PLAN-fase-0**
