---
topic: overview
verified: 2026-11-26
verifiedAt: { dama-italiana: cd2d0de }
sources: [README.md, SPEC.md, ARCHITECTURE.md, pom.xml]
---
# Cos'è il progetto

- **Scopo:** applicazione desktop Java per giocare a Dama Italiana contro il computer, in LAN peer-to-peer e
  online tramite server centrale; partite singole e tornei (eliminazione diretta, round-robin).
- **Moduli** (monorepo Maven, parent `dama-italiana-parent`):
  - `shared/` — dominio puro, `RuleEngine`, IA; nessun framework.
  - `core-server/` — tournament engine e match manager; solo `shared` + `spring-context`, nessun trasporto.
  - `client/` — desktop JavaFX + Spring Boot non-web; installer MSI.
  - `server/` — server centrale Spring Boot (REST, JPA, Flyway, MySQL); skeleton completato in Fase 5
    (auth JWT + refresh token, rate limit per IP, OpenAPI, observability); dettagli e delta nelle note
    post-implementazione F5 di ADR-044…049.
- **Stack** (da `pom.xml`): Java 21, Spring Boot 3.4.5, JavaFX 21.0.5, springdoc-openapi 2.8.6, jjwt 0.12.6,
  Flyway, Caffeine, Micrometer Prometheus, logstash-logback-encoder 7.4, MySQL 8.0.16+ (dev/it/prod),
  H2 in modalità MySQL (profilo `test`).
- **Fonti di verità:** `SPEC.md` (v2.4) prevale su tutto e cambia solo con change request approvata
  dall'utente (CLAUDE.md §1); `ARCHITECTURE.md` raccoglie ADR-001…049, i nuovi da ADR-050 vanno in `docs/adr/`; roadmap in SPEC §16,
  acceptance criteria in SPEC §17. Storia del metodo precedente in `_legacy/sdd4/`.
- **Stato:** fasi 0–5 chiuse (tag `v0.0.0`…`v0.5.0`; Fase 5 chiusa il 2026-11-25); prossima fase da avviare.
