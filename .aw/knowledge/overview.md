---
topic: overview
verified: 2026-11-21
verifiedAt: { dama-italiana: 0d47711 }
sources: [README.md, SPEC.md, ARCHITECTURE.md, pom.xml]
---
# Cos'è il progetto

- **Scopo:** applicazione desktop Java per giocare a Dama Italiana contro il computer, in LAN peer-to-peer e
  online tramite server centrale; partite singole e tornei (eliminazione diretta, round-robin).
- **Moduli** (monorepo Maven, parent `dama-italiana-parent`):
  - `shared/` — dominio puro, `RuleEngine`, IA; nessun framework.
  - `core-server/` — tournament engine e match manager; solo `shared` + `spring-context`, nessun trasporto.
  - `client/` — desktop JavaFX + Spring Boot non-web; installer MSI.
  - `server/` — server centrale Spring Boot (REST, JPA, Flyway, MySQL); in costruzione in Fase 5.
- **Stack** (da `pom.xml`): Java 21, Spring Boot 3.4.5, JavaFX 21.0.5, springdoc-openapi 2.8.6, jjwt 0.12.6,
  Flyway, Caffeine, MySQL 8.0.16+ (dev/it/prod), H2 in modalità MySQL (profilo `test`).
- **Fonti di verità:** `SPEC.md` (v2.4) prevale su tutto e cambia solo con change request approvata
  dall'utente (CLAUDE.md §1); `ARCHITECTURE.md` raccoglie ADR-001…049, i nuovi da ADR-050 vanno in `docs/adr/`; roadmap in SPEC §16,
  acceptance criteria in SPEC §17. Storia del metodo precedente in `_legacy/sdd4/`.
- **Stato:** fasi 0–4.5 chiuse (tag `v0.0.0`…`v0.4.5`); Fase 5 in corso su `feature/5-server-skeleton`.
