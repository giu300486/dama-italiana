# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`SPEC.md`** at the repo root: the sections relevant to the topic. It is the authoritative source and wins over every other document.
- **`GLOSSARY.md`** at the repo root.
- **ADRs**: read the ones that touch the area you're about to work in, in both places:
  - **`ARCHITECTURE.md`**: ADR-001…ADR-049 (historical, they stay there and are never moved);
  - **`docs/adr/`**: new ADRs, from ADR-050 on.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and `/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

Single-context repo:

```
/
├── SPEC.md
├── ARCHITECTURE.md          ← ADR-001…ADR-049
├── GLOSSARY.md              ← local only, not in git
├── docs/adr/                ← new ADRs, from 0050
│   ├── 0050-<slug>.md
│   └── 0051-<slug>.md
├── shared/  core-server/  client/  server/
```

## This repo

- **ADR numbering** continues the series in `ARCHITECTURE.md`: the first file in `docs/adr/` is `0050-<slug>.md` (ADR-050). The next number is the highest one across `ARCHITECTURE.md` and `docs/adr/`, plus one; never restart from `0001`.
- **`GLOSSARY.md` is local only** (listed in `.git/info/exclude`): a fresh clone may not have it. Committed files must not depend on it.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `GLOSSARY.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-049 (native local MySQL for dev and it), but worth reopening because…_
