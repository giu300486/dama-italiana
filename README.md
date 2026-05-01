# Dama Italiana Multiplayer

Applicazione desktop Java per giocare a **Dama Italiana** in tre modalità: contro il computer, in LAN peer-to-peer, e online tramite server centrale. Supporta sia partite singole che tornei (eliminazione diretta e campionato round-robin).

> **Documentazione autoritativa**: [`SPEC.md`](SPEC.md). In caso di conflitto tra qualsiasi altro file e SPEC, prevale SPEC.
> **Workflow di sviluppo**: [`CLAUDE.md`](CLAUDE.md).

---

## Stato del progetto

Fase corrente: **Fase 3 — Client UI single-player** (vedi [`plans/PLAN-fase-3.md`](plans/PLAN-fase-3.md)).

Stato runtime corrente: vedi [`AI_CONTEXT.md`](AI_CONTEXT.md).

---

## Prerequisiti

| Tool      | Versione minima | Note                                                                                  |
|-----------|-----------------|---------------------------------------------------------------------------------------|
| JDK       | 21 LTS          | Adoptium Temurin raccomandato                                                          |
| Maven     | 3.9             | Verificato dal plugin `maven-enforcer`                                                 |
| MySQL     | 8.0+            | Istanza locale o di rete; richiesta dal `server` (Internet) a partire dalla Fase 5    |
| Git       | 2.x             |                                                                                        |

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
| `mvn -pl server spring-boot:run`     | Lancia il server centrale (richiede MySQL attivo)                  |
| `mvn spotless:apply`                 | Applica il formato Google Java Style                               |
| `mvn spotless:check`                 | Verifica formato senza modificare                                  |
| `mvn jacoco:report`                  | Genera report HTML coverage in `target/site/jacoco/`               |
| `mvn dependency:tree`                | Visualizza grafo dipendenze                                        |

---

## Setup ambiente di sviluppo

### 1. Database MySQL locale

Il `server` (modulo central server, Internet) si connette a un'istanza MySQL già installata sulla macchina, gestita con MySQL Workbench / DBeaver. Il client offline (single-player) e LAN **non** richiedono MySQL.

Setup una tantum:

```sql
-- Da Workbench / DBeaver / mysql CLI, come utente con privilegi sufficienti:
CREATE DATABASE dama_italiana CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'dama'@'localhost' IDENTIFIED BY 'una-password-robusta';
GRANT ALL PRIVILEGES ON dama_italiana.* TO 'dama'@'localhost';
FLUSH PRIVILEGES;
```

> Lo schema delle tabelle è gestito da **Flyway** (vedi `server/src/main/resources/db/migration/`). Le migrazioni gireranno automaticamente all'avvio del server a partire dalla Fase 5.

Configurazione applicativa via env var (sovrascrivono i default in `server/src/main/resources/application.yml`):

```bash
export DB_URL=jdbc:mysql://localhost:3306/dama_italiana
export DB_USERNAME=dama
export DB_PASSWORD=una-password-robusta
```

I default `application.yml` puntano già a `localhost:3306`, db `dama_italiana`, utente `dama`. Solo la password è obbligatoria via env (vincolo NFR-S — niente secret committati).

### 2. Build

```bash
mvn clean verify
```

Atteso: `BUILD SUCCESS` su parent + 4 moduli (`shared`, `core-server`, `client`, `server`). Tempo indicativo primo run: 1–2 minuti (download dipendenze).

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
