# Traceability Matrix

> Mappatura cumulativa **requisito SPEC → test**.
> Aggiornata insieme ad ogni nuovo test. Vedi `CLAUDE.md` §2.4.3.
>
> In Fase 0 questa matrice è **vuota** (nessun requisito di dominio implementato).
> Dalla Fase 1 in poi cresce ad ogni FR/NFR/AC coperto da almeno un test.

---

## Requisiti funzionali (FR)

| FR ID  | Descrizione SPEC (breve) | Test class | Test method(s) | Tipo |
|--------|--------------------------|------------|----------------|------|
| _(vuota — popolata dalla Fase 1)_ |  |  |  |  |

---

## Requisiti non funzionali (NFR)

| NFR ID | Descrizione SPEC | Test class | Test method(s) | Tipo |
|--------|------------------|------------|----------------|------|
| _(vuota — popolata dalla Fase 1)_ |  |  |  |  |

---

## Acceptance criteria (SPEC sezione 17)

| AC ID  | Descrizione | Test class | Test method(s) | Tipo |
|--------|-------------|------------|----------------|------|
| _(vuota — popolata dalla Fase 1)_ |  |  |  |  |

---

## Note di manutenzione

- Una nuova riga è obbligatoria ad ogni test aggiunto che copre un FR/NFR/AC.
- Un FR senza nessuna riga è un finding `REQUIREMENT_GAP` alla review successiva (CLAUDE.md §2.4.3).
- Quando un test viene rimosso o spostato, aggiornare lo status; non lasciare righe orfane.
- Un FR può avere più righe: è normale e desiderabile per requisiti complessi.
