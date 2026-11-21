---
name: Phase transition protocol — riparti da CLAUDE.md §7
description: Alla chiusura di ogni fase della roadmap, la fase successiva inizia ripartendo dalla procedura "Avvio sessione" di CLAUDE.md §7 — non saltare lo stato-check.
type: project
originSessionId: 5d0757cb-3aac-41bf-8ad2-9235e350ad8f
---
Quando una fase della roadmap si chiude (review chiusa + test plan completato + tag `v0.<fase>.0` su `main`), la fase successiva DEVE essere avviata seguendo CLAUDE.md §7 "Avvio sessione Claude Code" — anche se la sessione è continuativa rispetto a quella di chiusura.

**Why**: l'utente ha esplicitato questo come requisito permanente il 2026-05-01 durante la chiusura di Fase 3.5. Vuole che ad ogni transizione di fase la PIANIFICA della fase nuova parta con un check di contesto fresco (SPEC sezioni rilevanti, AI_CONTEXT, CHANGELOG, eventuali review precedenti) anziché passare direttamente al lavoro. Riduce il rischio di drift e di assumption non verificate dopo lunghi cicli IMPLEMENTA.

**How to apply**: appena tag `v0.<N>.0` è applicato e merge `develop → main` chiuso, prima di scrivere `plans/PLAN-fase-<N+1>.md`, eseguire i passi 1-7 di CLAUDE.md §7 nell'ordine indicato:

1. Rileggere SPEC.md sezione 1 + 16 + 17 + sezioni rilevanti per la fase N+1.
2. Rileggere AI_CONTEXT.md.
3. Rileggere ultime 20 righe di CHANGELOG.md.
4. Identificare fase corrente nella roadmap (sezione 16 SPEC).
5. Identificare sotto-fase corrente (sarà PIANIFICA per la fase nuova).
6. Leggere review/test plan precedenti se rilevanti.
7. Sintetizzare in 3-5 righe lo stato compreso e confermare con l'utente prima di scrivere il PLAN.

Vale per ogni transizione futura, non solo F3.5 → F4.
