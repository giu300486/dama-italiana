# Development frameworks

## Active
- **none**: nessun framework di sviluppo. aw gestisce sessioni e workstream; ogni workstream organizza il
  proprio lavoro nel suo `RESUME.md` e `CONTEXT.md`.

Regole di progetto che inquadrano il lavoro nuovo:
- `SPEC.md` è la fonte di verità: si modifica solo dopo una change request approvata dall'utente, con commit
  `docs(spec): ...` e voce in `CHANGELOG.md` (CLAUDE.md §1).
- `CLAUDE.md` si modifica solo dopo discussione con l'utente, con commit `docs(claude): ...` e voce in
  `CHANGELOG.md` (CLAUDE.md §10).
- Anti-pattern di CLAUDE.md §8 (architettura dei moduli, niente SQL diretto, niente stringhe UI hardcoded,
  niente dipendenze fuori da SPEC §6 senza approvazione, design system F3.5 per ogni nuova schermata).
- Regole di qualità dei test di CLAUDE.md §2.4.1/2.4.2/2.4.4/2.4.5 (piramide, coverage minima per modulo,
  corpus delle regole italiane in `shared/src/test/resources/test-positions.json`, naming dei test).
- Chiedere conferma all'utente prima di iniziare ogni nuovo task numerato.

## Retired
- **SDD-4** (Spec-Driven Development: PIANIFICA → IMPLEMENTA → REVIEW → TEST per fase, con `AI_CONTEXT.md`,
  `plans/`, `reviews/`, test plan e traceability matrix), usato dal 2026-04-27 al 2026-11-21, archivio in
  `_legacy/sdd4/` (vedi il suo README).
