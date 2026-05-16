# Dama Italiana Multiplayer

Applicazione desktop Java per giocare a **Dama Italiana** in tre modalità: contro il computer, in LAN peer-to-peer, e online tramite server centrale. Supporta sia partite singole che tornei (eliminazione diretta e campionato round-robin).

> **Documentazione autoritativa**: [`SPEC.md`](SPEC.md). In caso di conflitto tra qualsiasi altro file e SPEC, prevale SPEC.
> **Workflow di sviluppo**: [`CLAUDE.md`](CLAUDE.md).

---

## Stato del progetto

Fase corrente: **Fase 5 — Server centrale skeleton** (vedi [`plans/PLAN-fase-5.md`](plans/PLAN-fase-5.md)).

Stato runtime corrente: vedi [`AI_CONTEXT.md`](AI_CONTEXT.md).

---

## Prerequisiti

| Tool      | Versione minima | Note                                                                                  |
|-----------|-----------------|---------------------------------------------------------------------------------------|
| JDK       | 21 LTS          | Adoptium Temurin raccomandato                                                          |
| Maven     | 3.9             | Verificato dal plugin `maven-enforcer`                                                 |
| MySQL     | 8.0.16+         | Richiesto per profili `dev`, `it`, `prod` del `server` (ADR-049). Il fast loop test (`-DexcludedGroups=slow`) usa H2 in-memory e non richiede MySQL. Il client offline e LAN non richiedono MySQL |
| Git       | 2.x             |                                                                                        |

> **Nota Docker**: da F5+ il modulo `server` NON richiede Docker per nessun layer (ADR-018 + ADR-049). Lo sviluppo, i test slow e il deploy `prod` usano MySQL nativo installato sul rispettivo host. Docker resta una scelta facoltativa per containerizzare l'eseguibile `prod` in futuro (deploy concern, fuori scope F5).

Per Windows è richiesto **Bash** (Git Bash o WSL) se si desidera replicare i comandi descritti qui letteralmente.

---

## Layout monorepo

```
dama-italiana/
├── shared/         # Modello di dominio puro + RuleEngine + IA (no framework)
├── core-server/    # Tournament engine + match manager (DI Spring, no transport)
├── client/         # Applicazione desktop JavaFX + Spring Boot non-web
└── server/         # Server centrale Spring Boot (REST + STOMP + JPA + MySQL)
```

Dettagli architetturali: [`ARCHITECTURE.md`](ARCHITECTURE.md) e `SPEC.md` sezioni 6-7.

---

## Comandi standard

| Comando                              | Scopo                                                              |
|--------------------------------------|--------------------------------------------------------------------|
| `mvn clean verify`                   | Build completo + test + coverage + Spotless + SpotBugs             |
| `mvn -pl shared verify`              | Verify modulo specifico                                            |
| `mvn -pl core-server verify`         | Verify core-server                                                 |
| `mvn -pl client javafx:run`          | Lancia il client desktop (da Fase 3)                               |
| `mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev` | Lancia il server centrale in modalita sviluppo (MySQL nativo locale `dama_italiana_dev`, da Fase 5, ADR-049) |
| `mvn -pl server spring-boot:run`     | Lancia il server centrale (richiede `SPRING_PROFILES_ACTIVE=prod` + DB e JWT secret env vars; deploy reale) |
| `mvn -pl server verify -DexcludedGroups=slow` | Fast loop test server: H2 in-memory, no MySQL nativo richiesto (da Fase 5) |
| `mvn -pl server verify`              | Test server full (incluso `@Tag("slow")` su MySQL nativo locale `dama_italiana_it`, da Fase 5) |
| `mvn spotless:apply`                 | Applica il formato Google Java Style                               |
| `mvn spotless:check`                 | Verifica formato senza modificare                                  |
| `mvn jacoco:report`                  | Genera report HTML coverage in `target/site/jacoco/`               |
| `mvn dependency:tree`                | Visualizza grafo dipendenze                                        |

---

## Setup ambiente di sviluppo

### 1. Server: 4 profili Spring (da Fase 5)

Il `server` (modulo central server, Internet) supporta 4 profili distinti — vedi [`ARCHITECTURE.md`](ARCHITECTURE.md) ADR-044 + ADR-049. Il client offline (single-player) e LAN **non** richiedono il server.

| Profilo | Quando | Datasource | Setup richiesto |
|---|---|---|---|
| `dev` | Sviluppo locale via `mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev` | MySQL nativo `localhost:3306/dama_italiana_dev` | MySQL 8 locale + script setup una tantum (§1.a) |
| `test` | Fast loop unit/slice test (`mvn -pl server verify -DexcludedGroups=slow`) | H2 in-memory MySQL-compat | Niente |
| `it` | Integration test full-stack `@Tag("slow")` (`mvn -pl server verify` o `mvn clean verify`) | MySQL nativo `localhost:3306/dama_italiana_it` | MySQL 8 locale + script setup una tantum (§1.a) |
| `prod` | Deploy reale | MySQL nativo installato sul server di deploy | MySQL 8.0+ + 4 env var (vedi §1.b) |

#### 1.a Sviluppo locale (`dev` + `it` profile)

Prerequisito: **MySQL 8 nativo installato e in esecuzione su `localhost:3306`** (l'unica volta che serve è una piccola sequenza di SQL admin per creare i 2 DB locali + l'utente applicativo `dama`).

**Setup una tantum** (eseguire come admin MySQL, di solito `root`):

```bash
mysql -u root -p < scripts/db/setup-local-mysql.sql
```

Su Windows con percorso completo:

```cmd
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < scripts\db\setup-local-mysql.sql
```

Lo script è idempotente (`CREATE ... IF NOT EXISTS`) e crea:
- DB `dama_italiana_dev` — usato a runtime dal profilo `dev` per playground interattivo (curl, Workbench). I dati persistono tra restart del server.
- DB `dama_italiana_it` — usato dai test `@Tag("slow")` per integration test contro MySQL reale. **Wiped + ricreato (`flyway.clean() + migrate()`) prima di ogni test method** in `AbstractMysqlIntegrationTest`.
- Utente `dama`@`localhost` con password `dama` e `GRANT ALL` su entrambi i DB.

**Reverse (teardown)** se vuoi ripartire pulito:

```bash
mysql -u root -p < scripts/db/teardown-local-mysql.sql
```

**Avvio server dev**:

```bash
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
```

Spring Boot:
1. Si connette a `dama_italiana_dev` come utente `dama` (default).
2. Flyway applica V1 al primo avvio (crea tabelle `users` + `refresh_tokens`); ai restart successivi vede V1 già applicata e non fa nulla.
3. Hibernate `ddl-auto=validate` verifica corrispondenza entity ↔ schema.
4. Server in ascolto su `http://localhost:8080`.

**Ispezione DB con MySQL Workbench**: connetti a `localhost:3306` con credenziali `dama`/`dama` (o `root` se preferisci visione completa). Vedrai `dama_italiana_dev` e `dama_italiana_it` nel tree, ciascuno con le tabelle `users` + `refresh_tokens` + `flyway_schema_history` (la quarta colonna serve a Flyway per tenere traccia delle migration applicate).

**Override credenziali** se l'utente locale `dama` ha credenziali diverse dal default:

```bash
export SPRING_DATASOURCE_USERNAME=miouser
export SPRING_DATASOURCE_PASSWORD=miapwd
mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev
```

> Tutto persistente: a differenza di un container effimero, i dati di sviluppo restano tra un restart e l'altro del server. Per resettare manualmente: o `DROP DATABASE dama_italiana_dev; CREATE DATABASE dama_italiana_dev;` via Workbench, oppure `flyway:clean` da Maven (esegue clean automatico).

> **Approfondimento Flyway**: se vuoi capire a fondo come funziona la migration tool che gestisce lo schema, leggi [`docs/flyway-in-our-context.md`](docs/flyway-in-our-context.md) — guida tecnica completa con descrizione generale, lifecycle per profilo, dissezione della V1, regole d'oro nel progetto, errori comuni + recovery.

#### 1.b Deploy production (`prod` profile)

Setup MySQL nativo una tantum sul server di deploy:

```sql
-- Da Workbench / DBeaver / mysql CLI, come utente con privilegi sufficienti:
CREATE DATABASE dama_italiana CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'dama'@'%' IDENTIFIED BY 'una-password-robusta';
GRANT ALL PRIVILEGES ON dama_italiana.* TO 'dama'@'%';
FLUSH PRIVILEGES;
```

> Lo schema delle tabelle e' gestito da **Flyway** (vedi `server/src/main/resources/db/migration/`).

Variabili d'ambiente OBBLIGATORIE (server fa fail-fast all'avvio se assenti):

```bash
export SPRING_PROFILES_ACTIVE=prod
export SPRING_DATASOURCE_URL=jdbc:mysql://db.example:3306/dama_italiana
export SPRING_DATASOURCE_USERNAME=dama
export SPRING_DATASOURCE_PASSWORD=una-password-robusta
export JWT_SECRET=$(openssl rand -base64 48)  # >= 32 byte (HS256 minimum)
java -jar server/target/server-*.jar
```

> JWT secret: vedi [`ARCHITECTURE.md`](ARCHITECTURE.md) ADR-045 per la strategia di rotation (90gg + grace 24h).

### 2. Build

```bash
mvn clean verify
```

Atteso: `BUILD SUCCESS` su parent + 4 moduli (`shared`, `core-server`, `client`, `server`). Tempo indicativo primo run: 1-2 minuti (download dipendenze). I test `@Tag("slow")` del modulo `server` richiedono MySQL nativo locale con setup completato (§1.a); per saltarli in fast loop: `mvn -pl server verify -DexcludedGroups=slow`.

---

## Eseguire il client (Fase 3+)

Il client desktop JavaFX si lancia tramite il `javafx-maven-plugin`:

```bash
mvn -pl client javafx:run
```

Pre-requisito una tantum (popola il local repo Maven con i parent + i moduli `shared` e `core-server`):

```bash
mvn install -pl .,shared,core-server -DskipTests -Dspotless.check.skip=true -Dspotbugs.skip=true -Djacoco.skip=true
```

Atteso: splash screen → main menu (Single Player / LAN [Fase 7] / Online [Fase 6] / Regole / Impostazioni). Il primo avvio applica la lingua di sistema (con fallback Italiano) e crea la directory utente `~/.dama-italiana/` per preferenze e salvataggi.

### Storage utente

Il client persiste su file:

| Path                                          | Contenuto                                                                 |
|-----------------------------------------------|---------------------------------------------------------------------------|
| `~/.dama-italiana/config.json`                | Preferenze (locale, scaling UI 100/125/150%, theme placeholder).           |
| `~/.dama-italiana/saves/<slot>.json`          | Salvataggi multi-slot single-player (FR-SP-07).                           |
| `~/.dama-italiana/saves/_autosave.json`       | Autosave riservato; sovrascritto ad ogni mossa (FR-SP-08, ATOMIC_MOVE).   |

Schema dei salvataggi documentato in [`ARCHITECTURE.md`](ARCHITECTURE.md) ADR-031. Per resettare lo stato utente è sufficiente cancellare la directory `~/.dama-italiana/`.

### Toggle lingua

Le lingue disponibili in Fase 3 sono **Italiano** (default) e **Inglese**. Il cambio avviene da Settings → Lingua e richiede il riavvio del client (runtime hot-swap rinviato a Fase 11; vedi ADR-033).

### Build installer Windows (Fase 3.5+)

Il client può essere impacchettato in un installer **MSI** Windows tramite `jpackage` (JDK 21) e il plugin `org.panteleyev:jpackage-maven-plugin`. L'installer include il JRE bundled, scorciatoia desktop, voce nel menu Start "Dama Italiana" e icona dedicata.

**Prerequisiti**:

| Tool | Versione | Note |
|---|---|---|
| JDK | 21 LTS | `jpackage` ne fa parte da JDK 14+ |
| **WiX Toolset** | **3.x (3.11+)** | Solo per `--type MSI`. Scaricabile da [wixtoolset.org](https://wixtoolset.org/releases/). Aggiungere `bin/` al `PATH` di sistema (deve esporre `candle.exe` e `light.exe`). WiX 4.x **non** è supportato da `jpackage` di JDK 21 |

**Comando**:

```bash
mvn -pl client -Pinstaller -DskipTests package
```

Output: `client/target/jpackage/Dama Italiana-0.3.5.msi` (~150-200 MB col JRE bundled).

**Fallback senza WiX** (smoke test / distribuzione zip ad-hoc):

```bash
mvn -pl client -Pinstaller -Djpackage.type=APP_IMAGE -DskipTests package
```

Genera `client/target/jpackage/Dama Italiana/` con il `.exe` di lancio + JRE — non installer ma cartella eseguibile portabile.

> Nota: l'`-DskipTests` è raccomandato perché la phase `package` con il profilo `installer` esegue dep-staging + jpackage, non i test (già coperti da `mvn verify`). Per smoke test pre-build: `mvn -pl client -Pinstaller dependency:copy-dependencies@jpackage-stage-runtime-deps` popola `target/jpackage-input/` senza invocare `jpackage`.

**Demo cliente Win 10/11**: l'MSI è pensato per la consegna come pacchetto autosufficiente. Il cliente riceve il file, fa **doppio-click**, completa l'installer wizard standard Windows (accetta i default, scope per-utente in `%LocalAppData%\Dama Italiana\`), e dal Start menu trova la voce "Dama Italiana" pronta al lancio. **Nessun prerequisito Java** sul target (JRE bundled). Le build successive con stesso `--win-upgrade-uuid` (UUID stabile committato nel POM) fanno upgrade in-place automatico senza disinstallazione manuale. Il flusso è coperto dal manual demo run di Task 3.5.14 (vedi `tests/TEST-PLAN-fase-3.5.md §7`).

### Headless / dev mode

I test FXML smoke usano `Platform.startup` con guard `Assumptions.assumeTrue(fxToolkitReady)`: in ambienti senza display vengono saltati anziché fallire. Per il fast loop di sviluppo F3:

```bash
mvn -pl client verify -DexcludedGroups=slow,performance
```

Tempo indicativo: ~30s. Esegue 264 test JUnit + JaCoCo (gate 60% line+branch) + Spotless + SpotBugs.

---

## Continuous Integration

Il repository remoto su GitHub esiste e i commit vengono pushati regolarmente, ma il workflow GitHub Actions è **disattivato per scelta** in questa fase del progetto (vedi `ARCHITECTURE.md` ADR-019). La codebase è ancora minimale e i test sono smoke: far girare la pipeline ad ogni push consumerebbe minuti CI senza catturare regressioni reali. La riattivazione è prevista quando il dominio (Fase 1+) sarà sostanzioso.

### Stato del file CI

Il workflow è preservato come [`.github/workflows/ci.yml.disabled`](.github/workflows/ci.yml.disabled). GitHub Actions ignora i file con estensione diversa da `.yml`/`.yaml`, quindi anche dopo `git push` la pipeline non viene eseguita. Per riattivarla:

```bash
git mv .github/workflows/ci.yml.disabled .github/workflows/ci.yml
git commit -m "chore(arch): re-enable CI workflow"
git push
```

### Validazione locale equivalente al CI

Finché la pipeline è disattivata, i tre quality gate del workflow sono validabili in locale via Maven:

| Job CI                | Comando equivalente in locale                  |
|-----------------------|------------------------------------------------|
| `build` (mvn verify)  | `mvn -B clean verify`                          |
| `lint` (Spotless)     | `mvn spotless:check`                           |
| `sast` (SpotBugs)     | `mvn -DskipTests verify`                       |

Il comando `mvn clean verify` da solo copre tutti e tre, perché Spotless e SpotBugs sono legati alla phase `verify` nel parent POM.

### Proposte per CI/CD self-hosted (futuro)

Se in futuro si vorrà CI completamente self-hosted (senza dipendere dai runner GitHub-hosted), ci sono due alternative pratiche:

1. **Gitea (o Forgejo) self-hosted + Gitea Actions**.
   - Server Git completo installabile come servizio Windows o standalone su una macchina Linux.
   - **Gitea Actions** è compatibile con la sintassi GitHub Actions: il file `ci.yml` esistente girerebbe quasi senza modifiche.
   - Ottimo trade-off: hai un repo remoto + CI in casa tua.
2. **Drone CI / Woodpecker CI** con runner installati direttamente sulla tua macchina.
   - Più leggeri di Jenkins.
   - Sintassi pipeline diversa rispetto a GitHub Actions (richiede riscrittura del workflow).

> Non installiamo niente di tutto questo ora. La proposta resta come riferimento per quando deciderai di aggiungere una macchina di build.

---

## Workflow Spec-Driven Development

Il progetto adotta un workflow **PIANIFICA → IMPLEMENTA → REVIEW → TEST** descritto in `CLAUDE.md` §2. Ogni fase della roadmap (`SPEC.md` §16) attraversa queste 4 sotto-fasi nell'ordine indicato.

Output di ogni sotto-fase:

- `plans/PLAN-fase-N.md` — output di PIANIFICA.
- `reviews/REVIEW-fase-N.md` — output di REVIEW.
- `tests/TEST-PLAN-fase-N.md` — output di TEST.
- `tests/TRACEABILITY.md` — matrice cumulativa requisito → test.

---

## Convenzioni

- Identificatori e Javadoc: **inglese**.
- Stringhe UI: solo via `ResourceBundle` (`messages_it.properties`, `messages_en.properties`).
- Stile codice: Google Java Style Guide via Spotless (verificato in `mvn verify`).
- Commit: [Conventional Commits](https://www.conventionalcommits.org/).
- **Branch model**: GitFlow leggero (`main` = production / tag, `develop` = integrazione / default branch GitHub, `feature/<fase>-<topic>` e `fix/review-N-F-<id>` da `develop`). Merge `--no-ff`.

Dettaglio: `CLAUDE.md` §4.

---

## Licenza

_(da definire prima del rilascio in Fase 11)_
