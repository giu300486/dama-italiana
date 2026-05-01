# Fonts

This directory holds the variable TTF binaries loaded by `ThemeService` at
startup. From Fase 3.5 onwards both fonts are committed alongside the OFL
license texts (SPEC §13.2 wood premium direction).

## Bundled fonts

| File | Family used in CSS | Role | License |
|---|---|---|---|
| `InterVariable.ttf` | `Inter` | UI base font (`-fx-font-family`) | [SIL OFL 1.1](Inter-OFL.txt) |
| `PlayfairDisplay-Variable.ttf` | `Playfair Display` | Display / hero titles (`.label-display`) | [SIL OFL 1.1](PlayfairDisplay-OFL.txt) |

Both are variable fonts: a single TTF supplies all weight axes (Inter:
Thin..Black; Playfair Display: Regular..Black). JavaFX 17+ resolves
`-fx-font-weight` against the variable axis automatically.

## Sources

- **Inter**: <https://github.com/rsms/inter> — `docs/font-files/InterVariable.ttf`
  (verified 2026-05-01). Author: Rasmus Andersson + Inter Project Authors.
- **Playfair Display**: <https://github.com/google/fonts> —
  `ofl/playfairdisplay/PlayfairDisplay[wght].ttf` (verified 2026-05-01).
  Authors: Claus Eggers Sørensen et al.

## Licensing

SIL OFL 1.1 imposes one obligation when redistributing the font: include the
license text. The two `*-OFL.txt` files in this directory satisfy that
requirement. No attribution is required in the application UI.

## Fallback behaviour

If either binary is absent at runtime (e.g., a stripped-down build), the
service logs an INFO line and JavaFX falls back to the next entry in the CSS
font-family chain declared by `theme-light.css`:

- UI: `Inter -> Segoe UI -> Helvetica Neue -> sans-serif`
- Display: `Playfair Display -> Cormorant Garamond -> Georgia -> serif`

The application renders correctly either way; only the visual character
changes.
