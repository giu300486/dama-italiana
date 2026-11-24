---
topic: conventions
verified: 2026-11-24
verifiedAt: { dama-italiana: d167e57 }
sources: [CLAUDE.md, .editorconfig, .gitattributes]
---
# Conventions

- **Codice:** identificatori e Javadoc in inglese; stringhe UI solo in `messages_it/en.properties`; Google Java
  Style via Spotless (2 spazi); solo SLF4J (niente `System.out`, `printStackTrace`); niente TODO/FIXME senza
  riferimento tracciato; record e sealed type per il dominio; `Optional` solo come ritorno. Regole di
  architettura dei moduli e del design system F3.5 in CLAUDE.md §8. Fine riga LF (`.gitattributes`).
- **Test:** CLAUDE.md §2.4 (piramide, coverage minima per modulo, corpus regole in
  `shared/src/test/resources/test-positions.json`, naming `<Classe>Test` / `<Feature>IntegrationTest` /
  `<Feature>E2ETest`).
- **Commit:** Conventional Commits `<type>(<scope>): <subject>` (CLAUDE.md §4.2). **Mai** il trailer
  `Co-Authored-By` né altre attribuzioni a Claude, in commit, merge, tag e PR (la storia è stata riscritta il
  2026-04-30 per toglierlo; attribuzione disattivata in `~/.claude/settings.json`).
- **Branch e versioni:** GitFlow leggero (CLAUDE.md §4.3–4.4): `develop` default, `feature/<fase>-<topic>`,
  `fix/review-N-F-<id>`, merge `--no-ff`; a fine fase `develop → main` e tag `v0.<fase>.0` sul merge in `main`.
- **Documenti:** `SPEC.md` solo via change request approvata (`docs(spec): ...` + CHANGELOG); `CLAUDE.md` solo
  dopo discussione (`docs(claude): ...` + CHANGELOG); nuove dipendenze fuori da SPEC §6 solo con approvazione.
- **Lavoro nuovo:** skill Matt Pocock (`.aw/FRAMEWORKS.md`), ma CLAUDE.md e `SPEC.md` prevalgono; ticket e spec
  in `.scratch/<feature>/` fuori da git, label canoniche come riga `Status:`; ADR nuovi da ADR-050 in
  `docs/adr/` (CLAUDE.md "Agent skills").
- **Ritmo:** chiedere conferma all'utente prima di iniziare ogni nuovo task numerato.
