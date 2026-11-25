---
topic: build-deploy
verified: 2026-11-21
verifiedAt: { dama-italiana: 0d47711 }
sources: [pom.xml, README.md, server/src/main/resources/application-*.yml, scripts/db/]
---
# Build and deploy

- **Prerequisiti:** JDK 21, Maven 3.9 (installato: 3.9.9, verificato), MySQL nativo 8.0.16+ su
  `localhost:3306` per i profili `dev`/`it` del server (DB `dama_italiana_dev` / `dama_italiana_it`, utente
  `dama`, creati con `scripts/db/setup-local-mysql.sql`). Docker **non** serve (ADR-018, ADR-049).
- **Build completa:** `mvn clean verify` (test, JaCoCo, Spotless, SpotBugs; include i test `slow`)
  — non eseguita in questa verifica.
- **Fast loop server:** `mvn -pl server verify -DexcludedGroups=slow` (H2, niente MySQL). Eseguito il
  2026-11-21 dalla sandbox di Claude: 76 test, 44 verdi, 32 in errore per "Unable to establish loopback
  connection" (tutti i `@SpringBootTest(RANDOM_PORT)`): limite della sandbox, vedi `pitfalls.md`. L'ultimo
  run completo documentato (fuori sandbox, 2026-05-30) era 76/76 verdi.
- **Fast loop client:** `mvn -pl client verify -DexcludedGroups=slow,performance` (~30 s); richiede una
  tantum `mvn install -pl .,shared,core-server -DskipTests -Dspotless.check.skip=true -Dspotbugs.skip=true -Djacoco.skip=true`
  — da documentazione, non eseguito.
- **Tag JUnit:** `slow` (simulazione IA ~16 min in `shared`; test MySQL nativo in `server`) e
  `performance` (NFR-P-02 in `shared`).
- **Run locale:** client `mvn -pl client javafx:run`; server
  `mvn -pl server spring-boot:run -Dspring-boot.run.profiles=dev` (senza profilo attivo il server si ferma).
- **Formato:** `mvn spotless:apply` prima di ogni commit.
- **Deploy:** installer MSI del client con `mvn -pl client -Pinstaller -DskipTests package` (jpackage, serve
  WiX 3.x). Server `prod`: MySQL nativo + env var `SPRING_DATASOURCE_*` e `JWT_SECRET` (fail-fast se
  mancano). CI GitHub Actions disattivata (`.github/workflows/ci.yml.disabled`, ADR-019).
