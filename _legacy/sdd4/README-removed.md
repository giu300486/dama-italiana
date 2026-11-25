# README.md — sezione rimossa (metodo SDD-4)

> Testo rimosso da `README.md` il 2026-11-21 al ritiro del metodo SDD-4, copiato parola per parola.
> Righe 286–295 del file originale. I percorsi citati ora sono sotto `_legacy/sdd4/`. Archivio, non regole attive.

## Workflow Spec-Driven Development

Il progetto adotta un workflow **PIANIFICA → IMPLEMENTA → REVIEW → TEST** descritto in `CLAUDE.md` §2. Ogni fase della roadmap (`SPEC.md` §16) attraversa queste 4 sotto-fasi nell'ordine indicato.

Output di ogni sotto-fase:

- `plans/PLAN-fase-N.md` — output di PIANIFICA.
- `reviews/REVIEW-fase-N.md` — output di REVIEW.
- `tests/TEST-PLAN-fase-N.md` — output di TEST.
- `tests/TRACEABILITY.md` — matrice cumulativa requisito → test.
