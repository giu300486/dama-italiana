# Fonts

This directory holds optional TTF/OTF binaries loaded by `ThemeService` at startup.

## Inter (recommended)

SPEC §13.2 specifies `Inter` as the preferred font family. To enable it, drop the
following files here:

- `Inter-Regular.ttf`
- `Inter-SemiBold.ttf`

Inter is licensed under SIL OFL 1.1 and is freely redistributable.
Source: https://rsms.me/inter/

If the files are absent, `ThemeService` logs an INFO line and JavaFX falls back to
the rest of the font-family chain (`"Segoe UI"`, `"Helvetica Neue"`, `sans-serif`)
declared in `theme-light.css`. The application renders correctly either way.

The binaries are intentionally NOT committed: keeping the repository free of
third-party binary assets simplifies licensing audits and reduces clone size.
Build pipelines and packagers (jpackage, Fase 11) are responsible for sourcing
the fonts before producing distributable installers.
