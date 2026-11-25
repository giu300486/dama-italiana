# Flyway — guida tecnica nel contesto Dama Italiana

> **Scopo**: spiegare cos'è Flyway, perché esiste questa categoria di tool, come funziona, e cosa fa esattamente nel nostro progetto, in modo che chiunque (incluso il maintainer single-developer) possa intervenire senza accumulare debito tecnico.
>
> **Stato del documento**: vivente. Da aggiornare ad ogni nuova migration o cambio di approccio. Sostituisce informazioni sparse in ADR-046 + PLAN-fase-5.md + commenti yaml.

---

## Parte 1 — Le basi (descrittiva, indipendente dal progetto)

### 1.1 Perché esiste questa categoria di strumenti

In un progetto che usa un database relazionale, lo **schema** (tabelle, colonne, indici, foreign key, constraint) deve essere allineato al **codice che lo legge e scrive**. Se aggiungi un campo `last_login_at` alla classe `UserEntity` ma non lo aggiungi alla tabella `users` del DB, l'app crasha al primo INSERT/SELECT.

Per molto tempo questa sincronizzazione è stata fatta a mano:

```
1. Il dev scrive il codice nuovo.
2. Il dev scrive un'email all'admin: "scusa, applica questo SQL prima del deploy".
3. L'admin esegue ALTER TABLE ... da Workbench.
4. Il dev fa deploy.
```

Questo flusso fallisce in **decine di modi**:
- L'admin si dimentica.
- L'admin esegue l'ALTER su l'ambiente sbagliato (staging invece di prod).
- L'admin lo esegue due volte (errore).
- Lo schema di test diverge da quello di prod.
- Nessuno sa quale versione SQL è effettivamente applicata su quale ambiente.
- Quando si fa rollback di un'app, lo schema resta avanti.

I **database migration tool** (Flyway, Liquibase, dbmate, Alembic per Python, ActiveRecord migrations per Rails, ecc.) risolvono questo problema **trattando lo schema come codice**:

- Ogni cambiamento allo schema è un file SQL versionato e committato nel repository.
- Il tool legge questi file, li applica al DB in ordine, e tiene traccia di quali sono stati applicati.
- L'app stessa, al startup, verifica che il DB sia allineato; se no, applica le migration mancanti.
- Cross-ambiente: dev, test, staging, prod ricevono **identici cambiamenti nello stesso ordine**.

Flyway è uno dei tool più popolari per JVM, scritto in Java, integrato nativamente con Spring Boot.

### 1.2 Concetti fondamentali

#### Migration

Un **migration** è un file (SQL o Java) che descrive **un cambiamento atomico allo schema**: "crea tabella X", "aggiungi colonna Y a Z", "rinomina indice K". Ogni migration ha un **numero di versione** crescente.

Esempio di sequenza in un progetto reale:

```
V1__init_users.sql               -- crea users
V2__add_matches.sql              -- crea matches + FK su users
V3__add_index_on_matches_date.sql -- aggiunge indice CREATE INDEX
V4__rename_users_role_column.sql -- ALTER TABLE users RENAME COLUMN
```

#### Schema history (la tabella di tracking)

Flyway mantiene **dentro il DB stesso** una tabella speciale (default: `flyway_schema_history`) che funge da registro contabile:

| installed_rank | version | description | script | checksum | success |
|---|---|---|---|---|---|
| 1 | 1 | init users | V1__init_users.sql | -2074567... | true |
| 2 | 2 | add matches | V2__add_matches.sql | 1923485... | true |
| 3 | 3 | add index | V3__add_index_... | 8123... | true |

Quando Flyway parte:
1. Cerca i file nel classpath (default: `classpath:db/migration/`).
2. Legge `flyway_schema_history`: quali versioni sono già applicate?
3. Applica solo quelle nuove, in ordine.
4. Aggiorna `flyway_schema_history` con i nuovi record.

#### Checksum

Per ogni migration applicata, Flyway calcola un **CRC32 del contenuto del file** e lo salva in `flyway_schema_history`. Allo startup successivo, **ricalcola il checksum dei file presenti** e li confronta con quelli registrati.

Se non corrispondono → `FlywayException: Validate failed: Migration checksum mismatch for migration V1`.

**Perché è importante**: impedisce di modificare una migration già applicata. Una volta che V1 è andata in produzione, **non puoi più cambiarne il contenuto**. Devi scrivere V2 (o V1.1) che corregge l'errore.

Questa è la **regola d'oro** dei migration tool: **immutability after deploy**.

#### Baseline

Cosa succede se hai un DB già pieno di tabelle (creato manualmente prima di adottare Flyway) e vuoi iniziare a usare Flyway ora?

- Senza baseline: Flyway vede `flyway_schema_history` mancante, prova ad applicare V1 (che crea tabelle che già esistono) → fail.
- Con `baseline-on-migrate=true`: Flyway crea `flyway_schema_history` e ci inserisce un record `V0 (baseline)` per dire "qualunque cosa ci sia ora la accettiamo come stato iniziale; applicherò solo V1 in poi se diversa da quanto già presente".

Nel nostro progetto: `baseline-on-migrate=true` in `application.yml`. È una rete di sicurezza per casi edge (es. dev locale dove qualcuno ha creato tabelle a mano via Workbench).

### 1.3 Tipi di migration

Flyway supporta tre tipi, identificati dal prefisso del nome file:

#### Versioned (`V`) — quelli che usiamo

```
V1__init_users_and_refresh_tokens.sql
V2__add_matches_table.sql
```

- Applicata **una sola volta**, in ordine di versione.
- Una volta applicata, **non più rieseguibile** (sarebbe doppio CREATE → errore).
- Modificarla post-deploy → checksum mismatch → fail.
- Lo "scheletro" del DB schema.

#### Repeatable (`R`) — non usati in F5, possibili in futuro

```
R__rebuild_view_user_stats.sql
R__seed_admin_users.sql
```

- Riapplicate **ogni volta che il loro checksum cambia**.
- Sempre applicate **dopo** tutte le versioned.
- Utili per views, stored procedure, seed data che evolvono spesso.
- Esempio futuro: in F9 (classifiche) potremmo avere una `R__view_player_ranking.sql` che ridefinisce una view di reporting; quando cambia la query, la prossima esecuzione la riapplica.

#### Undo (`U`) — non usati, sono Pro feature

```
U1__init_users.sql   -- rollback di V1
```

- Solo Flyway Enterprise / Pro (a pagamento).
- In community edition (quella che usiamo), **rollback automatico non esiste**.
- Per annullare V2: scrivi V3 che fa l'operazione opposta (`ALTER TABLE ... DROP COLUMN ...`).
- Nel progetto seguiamo la regola "no rollback automatico, sempre forward migration" (ADR-046).

### 1.4 Stati di una migration

Allo startup, ogni migration può trovarsi in uno di questi stati:

| Stato | Significato | Cosa fa Flyway |
|---|---|---|
| **Pending** | File presente nel classpath, NON in `flyway_schema_history` | La applica |
| **Applied** | File presente + record in history + checksum match | Niente (già OK) |
| **Out of Order** | Es. V3 applicata, ma V2 manca | Default: fail; con `out-of-order=true`: la applica retroattivamente |
| **Mismatch** | File in classpath, in history, MA checksum diverso | Fail con errore esplicito |
| **Future** | Record in history, ma file mancante (es. rollback codice) | Default: fail; con `ignore-migration-patterns`: può essere ignorata |
| **Missing** | Record in history senza file → corruzione | Fail |

Lo stato si vede con `mvn flyway:info` (richiede il Flyway Maven plugin, che noi non abbiamo configurato perché Spring Boot lo orchestra implicitamente).

### 1.5 Naming convention dei file

Pattern formale: `<Tipo><Versione>__<Description>.sql`

- Tipo: `V`, `R`, `U` (singola lettera).
- Versione (solo per `V` e `U`): numero o semver-style. `V1`, `V1.1`, `V1.2.3`, `V2`. La numerazione **non deve essere contigua** (puoi saltare da V1 a V5), ma deve essere **monotonicamente crescente**.
- Separatore: **due underscore** `__` tra versione e descrizione (un solo `_` non funziona).
- Description: `snake_case` umano-leggibile, no spazi.
- Estensione: `.sql` (o `.java` per migration programmatiche, raro).

**Esempi validi**:
- `V1__init_users.sql`
- `V2.1__add_index_on_email.sql`
- `R__seed_default_admins.sql`

**Esempi NON validi**:
- `V1_init_users.sql` (singolo underscore)
- `v1__init.sql` (minuscolo)
- `V1__init users.sql` (spazio nel nome)
- `1__init.sql` (manca prefisso V)

---

## Parte 2 — Come Spring Boot lo orchestra

### 2.1 Le dipendenze

In `server/pom.xml`:

```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-mysql</artifactId>
</dependency>
```

- `flyway-core` è il motore base: legge i file, mantiene `flyway_schema_history`, applica le migration.
- `flyway-mysql` è l'add-on dialetto-specifico: contiene le strategie ottimizzate per MySQL (es. parsing degli statement, handling dei delimitatori per stored procedures, gestione di transazioni DDL — MySQL non supporta transazioni DDL come PostgreSQL, quindi Flyway disabilita automaticamente la transazione per i CREATE TABLE su MySQL).

Sono dichiarate senza versione perché eredità da `spring-boot-dependencies` BOM (la versione è pinnata dalla Spring Boot release).

### 2.2 L'autoconfig

Spring Boot, quando rileva `flyway-core` nel classpath, attiva la classe `FlywayAutoConfiguration`. Questo bean:

1. Cerca un `DataSource` nel context (lo stesso che userà JPA).
2. Cerca proprietà `spring.flyway.*` in `application.yml`/`application-<profile>.yml`.
3. Crea un'istanza `Flyway` con quella config.
4. La esegue automaticamente prima dei bean JPA (`hibernate.entity-manager-factory` ha una dipendenza su `flyway`).
5. Dopo, Hibernate fa `ddl-auto=validate` su uno schema **già al passo**.

**Tutto questo accade prima della prima request HTTP**. Garantito.

Se vuoi disattivarlo per qualche motivo: `spring.flyway.enabled=false`.

### 2.3 Le nostre proprietà

In `server/src/main/resources/application.yml` (common):

```yaml
spring:
  flyway:
    enabled: true                          # default, esplicito per chiarezza
    locations: classpath:db/migration       # dove cerca i file (default)
    baseline-on-migrate: true              # crea baseline V0 se trova DB con tabelle ma senza history
    fail-on-missing-locations: false       # tollera path vuoto al primo run
  jpa:
    hibernate:
      ddl-auto: validate                    # Hibernate solo valida, non genera DDL
```

E nei profili specifici:

| Profilo | `clean-disabled` | Note |
|---|---|---|
| `dev` | `false` | Consente `flyway.clean()` per reset manuale |
| `test` | `false` | H2 in-memory si distrugge sola comunque |
| `it` | `false` | Necessario per `flyway.clean() + migrate()` in `@BeforeEach` di `AbstractMysqlIntegrationTest` |
| `prod` | **`true`** | Bloccato per safety (vedi §3.2) |

### 2.4 Cosa NON è configurato (e perché)

| Proprietà | Default | Decisione progetto |
|---|---|---|
| `out-of-order` | `false` | Tenuto a `false`. Migration applicate solo se la sequenza è contigua dal punto attuale in avanti. Se manca una V2 e abbiamo V3, fail-fast — meglio di applicare in ordine sbagliato. |
| `validate-on-migrate` | `true` | Tenuto a `true`. Ogni startup verifica checksum di tutto. Catch immediato di modifiche accidentali. |
| `mixed` | `false` | Tenuto a `false`. Niente migration che combinano DDL + DML — sarebbero non-transazionali in MySQL. |
| `placeholders.*` | (none) | Non usiamo placeholder. Le migration sono SQL puro, no `${VAR}`. Semplifica review e copy-paste. |
| `table` | `flyway_schema_history` | Default. Mai cambiato. |

---

## Parte 3 — Nel nostro progetto Dama Italiana

### 3.1 Architettura a 3 layer

Riepilogo (in dettaglio in PLAN-fase-5 §1.2 e nei commenti dei file):

```
┌────────────────────────────────────────────────────────────┐
│ LAYER 1 — Bootstrap del SERVER MySQL                       │
│   File: scripts/db/setup-local-mysql.sql                   │
│   Esecutore: root (admin), una tantum                       │
│   Crea: i 2 DB (dama_italiana_dev + dama_italiana_it)      │
│          + utente applicativo 'dama'@'localhost'           │
│   Per prod: README §1.b documenta il setup analogo         │
└────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────┐
│ LAYER 2 — Schema dell'APPLICAZIONE                         │
│   File: server/src/main/resources/db/migration/V1__init... │
│   Esecutore: Flyway (auto, da Spring Boot)                  │
│   Crea: tabelle users + refresh_tokens                     │
│   Quando: a ogni startup, applica le V non ancora in history│
└────────────────────────────────────────────────────────────┘
                              ↓
┌────────────────────────────────────────────────────────────┐
│ LAYER 3 — DATI                                             │
│   File: nessuno; INSERT/UPDATE via JPA repository           │
│   Esecutore: app (controller → service → repository → DB)   │
│   Crea: utenti, refresh token, match, eventi...             │
│   Quando: a ogni request HTTP                               │
└────────────────────────────────────────────────────────────┘
```

Le tre cose sono distinte. Layer 1 è "operativo", layer 2 è "schema-as-code", layer 3 è "stato applicativo".

### 3.2 Lifecycle Flyway per profilo

#### Profilo `dev` — sviluppo locale interattivo

```
$ mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev

[Spring]   Bootstrap profilo `dev`
[Hikari]   Connect: jdbc:mysql://localhost:3306/dama_italiana_dev (user=dama)
[Flyway]   Look for flyway_schema_history → not found (first run)
[Flyway]   Create flyway_schema_history table
[Flyway]   Scan classpath:db/migration → found V1__init_users_and_refresh_tokens.sql
[Flyway]   Calculate checksum of V1 → -2074567...
[Flyway]   No applied migrations in history → apply V1
[Flyway]   Migrating schema `dama_italiana_dev` to version 1 - init users and refresh tokens
[Flyway]   Successfully applied 1 migration to schema `dama_italiana_dev` (execution time 00:00.123s)
[Hibernate] ddl-auto=validate: compare UserEntity ↔ table users → OK
[Tomcat]   Started on port 8080
```

Al **secondo restart** (Ctrl+C, ri-lancio):

```
[Flyway]   Scan classpath → V1 found, checksum -2074567...
[Flyway]   flyway_schema_history shows V1 already applied, checksum matches
[Flyway]   No migrations to apply
[Hibernate] validate OK
[Tomcat]   Started on port 8080
```

Nessun-op. **Dati preservati** (alice persiste).

#### Profilo `it` — integration test

```java
@SpringBootTest @ActiveProfiles("it") @Tag("slow")
public abstract class AbstractMysqlIntegrationTest {
  @Autowired private Flyway flyway;

  @BeforeEach
  void resetSchema() {
    flyway.clean();    // DROP TABLE users, refresh_tokens, flyway_schema_history
    flyway.migrate();  // RE-CREATE FROM V1
  }
}
```

Flow di una test class che estende `AbstractMysqlIntegrationTest`:

```
[JUnit]    Test class loaded
[Spring]   Bootstrap context, profilo `it`, datasource → dama_italiana_it
[Flyway]   Auto-apply on startup: V1 applied (or already there from previous run)
[JUnit]    @BeforeEach kicks in (first time)
[Flyway]   clean() → DROP TABLE users; DROP TABLE refresh_tokens; DROP TABLE flyway_schema_history
[Flyway]   migrate() → re-apply V1 from scratch
[Test]     Method 1 runs against fresh DB
[JUnit]    @BeforeEach kicks in (second time)
[Flyway]   clean() + migrate() again
[Test]     Method 2 runs against fresh DB
...
```

**Garanzia**: ogni metodo test parte da DB allineato esattamente a V1, zero side-effect tra test.

#### Profilo `test` — fast loop H2

Stesso comportamento di `dev` ma con datasource H2 in-memory. Vantaggio: H2 in-memory si crea e distrugge dentro la JVM, nessun side-effect su disco. Svantaggio: H2 non è MySQL — può nascondere bug specifici di MySQL (vedi finding di Task 5.3.bis sul gap traduzione exception).

#### Profilo `prod` — deploy reale

```
$ SPRING_PROFILES_ACTIVE=prod JWT_SECRET=... \
  SPRING_DATASOURCE_URL=jdbc:mysql://db.example:3306/dama_italiana \
  java -jar server.jar

[Spring]   Bootstrap profilo `prod`
[Hikari]   Connect: env-defined URL
[Flyway]   clean-disabled=true → flyway.clean() throws FlywayException if invoked
[Flyway]   Apply any pending migration in order (V1 al primo deploy; V2, V3, ... ai deploy successivi)
[Server]   Up, listening on :8080
```

Su deploy successivo (F6 introduce V2):
```
[Flyway]   V1 already applied (matches history)
[Flyway]   V2 found, no history record → apply
[Flyway]   Migrating schema `dama_italiana` to version 2 - add matches
[Flyway]   Done
```

Lo schema evolve in modo incrementale **senza intervento manuale**. È la ragione principale per cui usiamo Flyway.

### 3.3 Dissezione della nostra V1

`server/src/main/resources/db/migration/V1__init_users_and_refresh_tokens.sql`:

```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL DEFAULT 'USER',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMP NULL,
  CONSTRAINT chk_users_role CHECK (role IN ('USER','TOURNAMENT_ADMIN','SYSTEM_ADMIN'))
);
CREATE INDEX idx_users_email ON users(email);

CREATE TABLE refresh_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_hash VARCHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  revoked_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);
```

Annotato:

- **`BIGINT PRIMARY KEY AUTO_INCREMENT`**: MySQL syntax. H2 in MySQL-mode lo accetta. Era preferito su `BIGINT IDENTITY` (standard SQL ma meno supportato).
- **`VARCHAR(50)` per username**: limite arbitrario ma ragionevole. Bean Validation lato Java lo applicherà pure (`@Size(min=3, max=50)`).
- **`VARCHAR(255)` per email**: lunghezza massima RFC 5321 (local-part 64 + `@` + domain 253).
- **`VARCHAR(255)` per password_hash**: BCrypt hash è ~60 caratteri (`$2a$12$...`), 255 lascia margine per algoritmi futuri (es. Argon2 ~96 chars).
- **`role VARCHAR(32) + CHECK`**: decisione CR-F5-001 (vs `ENUM` MySQL). VARCHAR è portabile H2/MySQL; CHECK fornisce vincolo DB-level. Workbench-friendly: l'utente vede esplicitamente i 3 valori validi nel DDL.
- **`TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`**: standard, ma MySQL aveva storicamente quirk strani su `TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP`. Specificando `NOT NULL` esplicito + `NULL` per `last_login_at` evita ambiguità.
- **`FOREIGN KEY ... ON DELETE CASCADE`**: cancellare l'utente cancella i suoi refresh token. Coerente con GDPR right-to-be-forgotten + niente refresh token orfani.
- **Indici aggiuntivi**: `idx_users_email` per lookup login by email; `idx_refresh_tokens_user` per logout-all-by-user; `idx_refresh_tokens_expires` per cleanup batch dei token scaduti.

Tutto SQL standard. Funziona identica su H2 + MySQL nativo + (in passato) Testcontainers.

### 3.4 Le regole d'oro nel nostro progetto

1. **Mai modificare una V<n>.sql dopo che è stata applicata in qualunque ambiente (dev, test, prod, CI).**
   - Conseguenza: aggiungere/togliere/modificare anche un singolo carattere nel SQL produce checksum mismatch → fail al startup.
   - Per cambiare lo schema dopo deploy: **sempre nuova migration** `V2__add_xyz.sql` con `ALTER TABLE`.
   - Per cambiare la migration prima del primo deploy (es. trovi un bug in V1 durante review): puoi modificarla, ma poi devi fare `flyway:clean` su tutti i DB locali per resettare l'history e farla riapplicare.

2. **`hibernate.ddl-auto = validate` ovunque.**
   - Mai `update` (Hibernate genera DDL al volo, divergente da Flyway).
   - Mai `create-drop` (distrugge il DB ad ogni run, bypassa Flyway).
   - Mai `create` (idem ma una sola volta).
   - `validate`: Hibernate solo verifica che le entity Java corrispondano alle tabelle reali. Se aggiungi un campo a `UserEntity` ma scordi V2 → startup fallisce immediatamente.

3. **`clean-disabled=true` in `prod`.**
   - `flyway.clean()` su `prod` = drop di tutte le tabelle + dati = catastrofe.
   - La proprietà a `true` lo impedisce categoricamente: `mvn flyway:clean` su `prod` → `FlywayException: Unable to execute clean as it has been disabled with the 'flyway.cleanDisabled' property.`

4. **Niente seed data in V<n>.sql.**
   - Le migration creano **schema**, non popolano dati di business.
   - Dati di test → `@Sql` annotation o `data.sql` profilo `test`.
   - Dati di produzione iniziali (es. utente admin di sistema) → migration dedicata in F6+ con meccanismo trasparente (probabilmente una `R__seed_system_users.sql` repeatable).
   - Motivazione: separare DDL da DML rende rollback/forensic più semplici.

5. **Una migration per cambiamento logico.**
   - Non agglomerare 5 ALTER TABLE diverse in una V2. Tre cambiamenti distinti → V2, V3, V4.
   - Vantaggi: diff più leggibili in review, rollback (manuale, scrivendo una forward inversa) più chirurgico, log Flyway al startup più informativi.

6. **Migration testabili separatamente.**
   - Ogni volta che aggiungiamo una V<n>, scriviamo (o aggiorniamo) `FlywayMigrationIntegrationTest` per verificare che il risultato sia quello atteso (colonne, indici, constraint).
   - La V1 è già coperta in 6 test (vedi `FlywayMigrationIntegrationTest` + 2 in `FlywayMigrationMysqlIntegrationTest`).

### 3.5 Cosa NON fa Flyway (e cosa facciamo a mano)

- **Non crea il DATABASE/schema** (`CREATE DATABASE ...`). Il DB deve esistere. Per questo abbiamo `scripts/db/setup-local-mysql.sql`.
- **Non crea l'USER applicativo né i GRANT**. Idem.
- **Non gestisce dati cross-tabella complessi** (es. backfill di una colonna nuova guardando un'altra tabella). Per dati massivi conviene script separato o migration Java; per F5 non serve.
- **Non fa rollback automatico in community edition.** Se vuoi annullare V2 in prod, scrivi V3 che fa l'opposto.
- **Non sincronizza dati tra ambienti** (è uno strumento di schema, non di replica/ETL).
- **Non versiona le stored procedures/views** (se non come repeatable). Per F5 non ne abbiamo; quando arriveranno useremo `R__`.

---

## Parte 4 — Errori comuni e recovery

### 4.1 "Migration checksum mismatch for migration V1"

**Causa**: hai modificato `V1__init_users_and_refresh_tokens.sql` dopo che era già stato applicato a un DB (anche dev locale).

**Recovery**:

- Se sei in dev locale e nessuno ha applicato il file modificato altrove:
  ```bash
  # Reset DB locale, Flyway riapplica V1 al prossimo startup
  mvn -pl server flyway:clean -Dflyway.user=dama -Dflyway.password=dama -Dflyway.url=jdbc:mysql://localhost:3306/dama_italiana_dev
  ```
  Oppure manualmente in Workbench:
  ```sql
  USE dama_italiana_dev;
  DROP TABLE refresh_tokens;
  DROP TABLE users;
  DROP TABLE flyway_schema_history;
  ```
  Poi restart server.

- Se il file modificato è già in prod: **NON puoi tornare indietro**. Devi scrivere una V<n+1> che applichi la differenza, e ripristinare V<n> al contenuto originale. Imparato la lezione.

### 4.2 "Detected resolved migration not applied to database: 2"

**Causa**: hai V1 e V2 nei file, V1 applicata, V2 no, ma Flyway in qualche modo non la applica. Spesso perché `out-of-order=false` e c'è una V1.5 in mezzo.

**Recovery**: verifica `mvn flyway:info` per capire lo stato. Probabilmente devi rimuovere file fuori sequenza o abilitare `out-of-order` (solo se sei sicuro).

### 4.3 "Schema 'dama_italiana_dev' is not empty. Found: table 'users'..."

**Causa**: hai una tabella `users` creata a mano (es. via Workbench), Flyway al primo run non sa cosa fare.

**Recovery**: con `baseline-on-migrate=true` (nostro setting), Flyway crea history e una baseline. Ma se le tabelle attuali non corrispondono a quello che V1 vorrebbe creare, V1 fallirà su `CREATE TABLE users` perché esiste già. Soluzione:
1. Drop manuale di tutte le tabelle utente.
2. Restart → Flyway applica V1 da zero.

### 4.4 "Migration V1 failed!" (errore SQL)

**Causa**: il SQL della migration ha un errore (sintassi, vincolo violato, ecc.).

**Recovery**: 
1. Flyway ha lasciato `flyway_schema_history` con `success=false` per V1.
2. Spring Boot non parte.
3. Correggi il SQL in V1 (vedi §4.1 per le regole, ma se V1 non è mai andata a buon fine puoi modificarla).
4. Drop di `flyway_schema_history`.
5. Restart → riapplica V1 corretta.

### 4.5 Test integration falliscono solo su MySQL nativo

È esattamente quello che è successo a noi in Task 5.3.bis. Cause possibili:
- Discrepanza H2/MySQL su tipi dati, charset, collation.
- Differenze nei codici errore (vedi Spring `SQLErrorCodeSQLExceptionTranslator` gap su MySQL 8 code 3819).
- Sensibilità case-sensitivity dei nomi tabelle (Linux MySQL filesystem-case-sensitive, Windows insensitive, H2 dipende da `DATABASE_TO_LOWER`).

**Approach**: il test slow MySQL **deve esistere** proprio per catturare queste discrepanze. Quando ne emerge una, documentarla e adattare il test invece di "renderlo verde forzando" (anti-pattern CLAUDE.md §8 #14).

---

## Parte 5 — Roadmap nel progetto

### V1 (Fase 5, attuale)

`V1__init_users_and_refresh_tokens.sql` — auth core. Già applicata.

### V2 (Fase 6 — Server centrale completo)

Pianificata, non ancora scritta. Conterrà:
- `CREATE TABLE matches` (id, white_user_id FK, black_user_id FK, status, result, created_at, completed_at, ...).
- `CREATE TABLE match_events` (id, match_id FK, sequence_number, event_type, payload JSON, timestamp).
- Indici: `idx_matches_status`, `idx_match_events_match_seq`.

Dettagli in `plans/PLAN-fase-6.md` quando si aprirà.

### V3 (Fase 8 — Tornei)

Pianificata, non ancora scritta. Conterrà:
- `CREATE TABLE tournaments` (id, type, status, created_by_user_id, ...).
- `CREATE TABLE tournament_rooms` (id, tournament_id FK, name, ...).
- `CREATE TABLE tournament_participants` (id, tournament_id FK, user_id FK, seed, ...).
- `CREATE TABLE tournament_matches` (id, tournament_id FK, match_id FK, round, ...).
- `ALTER TABLE matches ADD COLUMN tournament_match_id BIGINT NULL`.

### Migration future (F9+)

Probabili: indici di reporting per classifiche, view repeatable `R__view_player_ranking`.

---

## Parte 6 — Riferimenti

- **ADR-046** (`ARCHITECTURE.md`): Flyway baseline strategy & schema portability — decisioni principali sullo schema, versioning, scelta `VARCHAR + CHECK` per `role`.
- **ADR-049** (`ARCHITECTURE.md`): MySQL nativo locale per `dev` e `it` — substrate scelto da F5.
- **CR-F5-001** (`SPEC.md` §8.4): `users.role` ENUM → VARCHAR + CHECK.
- **PLAN-fase-5.md** §4 Task 5.3 + Task 5.3.bis: dove la V1 è stata definita e dove la strategia test è stata refattorizzata.
- **SPEC.md** §6.5: tecnologie obbligatorie del progetto, include Flyway.
- **CLAUDE.md** §8 anti-pattern #14: niente "soppressione" di test che falliscono.
- **scripts/db/setup-local-mysql.sql**: setup admin del MySQL locale (Layer 1).
- **server/src/main/resources/db/migration/V1__init_users_and_refresh_tokens.sql**: la prima migration (Layer 2).
- **Flyway official docs**: https://documentation.red-gate.com/fd (riferimento canonico per i dettagli del tool).
- **Spring Boot Flyway autoconfig**: https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway

---

## Appendice — Glossario

| Termine | Definizione |
|---|---|
| **Migration** | File SQL/Java versionato che descrive un cambiamento atomico allo schema. |
| **Schema history** | Tabella `flyway_schema_history` nel DB che traccia quali migration sono state applicate. |
| **Checksum** | CRC32 del contenuto di una migration. Usato per detect modifiche post-applicazione. |
| **Baseline** | Punto di partenza convenzionale (V0 o V1) per DB pre-esistenti adottati da Flyway tardivamente. |
| **Pending** | Migration presente nei file ma non in history. Verrà applicata al prossimo startup. |
| **Applied** | Migration presente nei file E in history, con checksum coerente. Stato normale. |
| **Mismatch** | File modificato dopo applicazione → checksum diverso → fail. |
| **Versioned (V)** | Migration applicata una sola volta in ordine di versione. |
| **Repeatable (R)** | Migration riapplicata quando cambia il suo checksum. |
| **Undo (U)** | Migration di rollback (solo Flyway Pro, non in community). |
| **Clean** | Operazione che droppa tutte le tabelle utente + schema history. Distruttiva. Bloccata in `prod`. |
| **Migrate** | Operazione che applica le migration pending in ordine. |
| **Info** | Operazione che mostra lo stato di ogni migration (Pending/Applied/Mismatch). |
| **Validate** | Operazione che verifica checksum e ordini, fail su discrepanze. |
| **DDL** | Data Definition Language: CREATE, ALTER, DROP TABLE/INDEX. |
| **DML** | Data Manipulation Language: INSERT, UPDATE, DELETE. |
| **Out-of-order** | Modalità che permette di applicare migration con numeri non contigui rispetto allo stato. Default: off. |
