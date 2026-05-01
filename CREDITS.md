---
last-updated: 2026-05-01
license-policy: |
  CC0 1.0 Universal for all visual + audio assets.
  SIL OFL 1.1 for fonts only (PLAN-fase-3.5 §3.2 "asset CC-BY/OFL"). OFL has no
  attribution requirement when embedded in software; the only obligation is to
  redistribute the license text alongside the binaries — see fonts/*-OFL.txt.
phase: F3.5 (Task 3.5.1 + 3.5.2)
---

# Dama Italiana — Asset Credits

This document inventories every third-party visual and audio asset bundled
with the client. Every entry is verified to be released under **CC0 1.0
Universal / Public Domain** before inclusion. Local filenames are relative to
`client/src/main/resources/assets/`.

CC0 imposes **no obligations** (no attribution required, no share-alike, free
commercial use, modifications allowed). This file is maintained for internal
audit and client diligence; nothing in it is required by the licenses
themselves.

---

## Visual assets

Tutte le texture provengono da [Poly Haven](https://polyhaven.com), che
rilascia l'intero catalogo con licenza CC0 1.0 dichiarata sia globalmente
([about page](https://polyhaven.com/about)) sia su ogni asset (campo
"License: CC0" nella sidebar destra). Per il client si è scelta la **2K JPG
diffuse** map (compromise tra dettaglio e size; le risoluzioni 4K/8K sono
sovradimensionate per celle di ~80 px del tavoliere).

| Local file | Source page | Direct file URL | License | Author | Size on disk |
|---|---|---|---|---|---:|
| `textures/board_dark.jpg` | https://polyhaven.com/a/dark_wooden_planks | https://dl.polyhaven.org/file/ph-assets/Textures/jpg/2k/dark_wooden_planks/dark_wooden_planks_diff_2k.jpg | CC0 1.0 | Poly Haven | 2.75 MB |
| `textures/board_light.jpg` | https://polyhaven.com/a/oak_wood_planks | https://dl.polyhaven.org/file/ph-assets/Textures/jpg/2k/oak_wood_planks/oak_wood_planks_diff_2k.jpg | CC0 1.0 | Poly Haven | 2.61 MB |
| `textures/frame.jpg` | https://polyhaven.com/a/wood_table_worn | https://dl.polyhaven.org/file/ph-assets/Textures/jpg/2k/wood_table_worn/wood_table_worn_diff_2k.jpg | CC0 1.0 | Poly Haven (foto: Dimitrios Savva, processing: Rico Cilliers) | 2.96 MB |

## Fonts

Aggiunti in Task 3.5.2 (PLAN-fase-3.5 §3 Foundation). Fonts variabili
(weight axis), in `client/src/main/resources/fonts/`. Caricati al primo
`ThemeService.applyTheme(Scene)` con `Font.loadFont`; CSS li referenzia via
`-fx-font-family: "Inter"` (UI base) e `-fx-font-family: "Playfair Display"`
(`.label-display`, hero titles).

| Local file | Source URL | License | Author | Size on disk |
|---|---|---|---|---:|
| `fonts/InterVariable.ttf` | https://github.com/rsms/inter — `docs/font-files/InterVariable.ttf` | SIL OFL 1.1 | Rasmus Andersson + Inter Project Authors | 859 KB |
| `fonts/Inter-OFL.txt` | https://github.com/rsms/inter/blob/master/LICENSE.txt | (license text) | — | 4.4 KB |
| `fonts/PlayfairDisplay-Variable.ttf` | https://github.com/google/fonts — `ofl/playfairdisplay/PlayfairDisplay[wght].ttf` | SIL OFL 1.1 | Claus Eggers Sørensen + Google Fonts contributors | 294 KB |
| `fonts/PlayfairDisplay-OFL.txt` | https://github.com/google/fonts/blob/main/ofl/playfairdisplay/OFL.txt | (license text) | — | 4.5 KB |

**OFL compliance**: SIL OFL 1.1 imposes a single obligation when bundling
fonts in software — redistribute the license text. The two `*-OFL.txt` files
satisfy this. **No attribution is required in the app UI**. A "Reserved Font
Name" clause forbids selling the font itself separately; it does not affect
embedding.

## Audio — Music

Sorgenti da [OpenGameArt.org](https://opengameart.org), filtrate per CC0.
Ogni pagina mostra la stringa "CC0" verbatim.

JavaFX Media legge nativamente MP3 e WAV PCM su Windows (Microsoft Media
Foundation backend). Le 4 tracce sono committate **as-downloaded**, niente
re-encoding: 1 brano è MP3 all'origine, 3 sono WAV PCM. Costo disco totale
~67 MB; CC0 permette esplicitamente la redistribuzione senza modifiche.

| Local file | Source page | Direct file URL | License | Author | Size on disk |
|---|---|---|---|---|---:|
| `audio/music/calm_piano_1.mp3` | https://opengameart.org/content/calm-piano-1-vaporware | https://opengameart.org/sites/default/files/003_Vaporware_2.mp3 | CC0 1.0 | The Cynic Project (cynicmusic) | 6.30 MB |
| `audio/music/first_light_particles.wav` | https://opengameart.org/content/first-light-particles-%E2%80%93-cc0-atmospheric-pianoambient-track | https://opengameart.org/sites/default/files/first_light_particles_0.wav | CC0 1.0 | Yoiyami | 24.12 MB |
| `audio/music/at_home_orchestral.wav` | https://opengameart.org/content/at-home-orchestral | https://opengameart.org/sites/default/files/cinematic-calm.wav | CC0 1.0 | Wolfgang_ | 12.50 MB |
| `audio/music/peaceful_forest.wav` | https://opengameart.org/content/peaceful-forest | https://opengameart.org/sites/default/files/Peaceful%20Forest.wav | CC0 1.0 | Samza | 21.23 MB |

## Audio — SFX

Tutti i pack [Kenney.nl](https://kenney.nl) sono CC0 1.0 a livello di sito
(banner globale + stringa "License: Creative Commons CC0" su ogni pack page).
File specifici estratti dai pack ZIP nel canonical naming. Durate
**verificate via mutagen** (Python OGG metadata reader) il 2026-05-01 sul
master OGG; le tabelle sotto mostrano sia il master OGG sia il WAV PCM
distribuito al runtime.

### Pipeline format (Task 3.5.4 follow-up, 2026-05-01)

JavaFX Media su Windows **non decodifica OGG Vorbis** — verifica empirica
del 2026-05-01 con un mini-`MediaPlayer` che ha restituito `MediaException —
Unrecognized file signature!` su tutti i file. I pack Kenney distribuiscono
**solo OGG**, niente WAV. Per preservare la selezione curata sopra senza
rifare l'asset acquisition, è stato introdotto:

- **Master OGG** in `client/src/main/resources/assets/audio/sfx-master/`
  (committati per audit trail, ~70 KB totali). Sorgente di verità.
- **WAV PCM** in `client/src/main/resources/assets/audio/sfx/` (committati,
  caricati a runtime da `Sfx.resourcePath()`).
- **Tool di conversione**: `com.damaitaliana.client.buildtools.OggToWavConverter`
  (`client/src/test/java/`), main Java pure-JOrbis (low-level
  Stream/Packet API, modellata su `com.jcraft.jorbis.DecodeExample`),
  decodifica deterministica anche per clip brevissimi (<400 ms — la rotta
  via `javax.sound.sampled` SPI ha mostrato un bug con clip <400 ms).
  Dipendenze `<scope>test</scope>`: `com.googlecode.soundlibs:jorbis:0.0.17.4`
  + `com.googlecode.soundlibs:vorbisspi:1.0.3.3` (LGPL — non ridistribuite,
  nessun obbligo). Esecuzione manuale al refresh asset:
  ```
  mvn -pl client exec:java \
      -Dexec.classpathScope=test \
      -Dexec.mainClass="com.damaitaliana.client.buildtools.OggToWavConverter" \
      -Dexec.args="client/src/main/resources/assets/audio/sfx-master \
                   client/src/main/resources/assets/audio/sfx"
  ```

| Local file | Source pack | Source filename in pack | License | Master OGG | WAV PCM |
|---|---|---|---|---:|---:|
| `audio/sfx/move.wav` | https://kenney.nl/assets/impact-sounds | `Audio/impactWood_light_000.ogg` | CC0 1.0 | 6.2 KB / 266 ms | 47.3 KB |
| `audio/sfx/capture.wav` | https://kenney.nl/assets/impact-sounds | `Audio/impactWood_heavy_000.ogg` | CC0 1.0 | 6.1 KB / 313 ms | 54.8 KB |
| `audio/sfx/promotion.wav` | https://kenney.nl/assets/interface-sounds | `Audio/confirmation_002.ogg` | CC0 1.0 | 13.8 KB / 539 ms | 46.5 KB |
| `audio/sfx/illegal.wav` | https://kenney.nl/assets/interface-sounds | `Audio/error_001.ogg` | CC0 1.0 | 7.2 KB / 165 ms | 29.8 KB |
| `audio/sfx/win.wav` | https://kenney.nl/assets/music-jingles | `Audio/Pizzicato jingles/jingles_PIZZI07.ogg` | CC0 1.0 | 18.7 KB / 1.32 s | 228.1 KB |
| `audio/sfx/lose.wav` | https://kenney.nl/assets/music-jingles | `Audio/Pizzicato jingles/jingles_PIZZI03.ogg` | CC0 1.0 | 16.6 KB / 1.15 s | 197.6 KB |

Master OGG: 44100 Hz, stereo (Impact + Music Jingles) o mono (Interface
brevi); WAV: 44100 Hz, 16-bit signed little-endian, stessi canali. Totale
WAV ~600 KB.

---

## Verifica licenze (audit trail)

Ogni asset è stato verificato in due modi:

1. **A livello catalogo**: i tre siti sorgente (Poly Haven, OpenGameArt.org
   filtrato CC0, Kenney.nl) hanno una dichiarazione di licenza CC0 globale
   leggibile sulla home / about page.
2. **A livello asset**: la pagina di ogni risorsa è stata aperta (WebFetch
   2026-05-01) e la stringa "CC0" / "CC0 1.0" / "Creative Commons CC0" è
   stata letta verbatim.

Per gli SFX, in aggiunta, ogni file è stato **provato in temp** (download
diagnostico, no commit) e analizzato con `mutagen.File` per ottenere durate
e bitrate esatti riportati nelle tabelle. Il temp è stato cancellato dopo
la probe; nei binari del repo restano solo i 6 file finali con i nomi
canonici.

Asset esclusi durante la ricerca (per trasparenza):

- `polyhaven.com/a/walnut_veneer` — bloccata dietro Wood Vault Patreon, non
  liberamente scaricabile pur restando CC0. Esclusa.
- `polyhaven.com/a/wood_planks_027` e `wood_planks_028` — restituiscono 404,
  non esistono nel catalogo Poly Haven.
- `freepd.com` — sito chiuso definitivamente nel 2025; URL morti.
- Qualsiasi traccia o effetto sonoro CC-BY (incluso quasi tutto incompetech,
  la maggior parte di Freesound/InspectorJ): esclusi per regola hard CC0-only.
- Mixkit, Pixabay (audio): rilasciano con licenza custom, non CC0; esclusi.
- Kenney `Audio/Pizzicato jingles/jingles_PIZZI*.ogg` con durata <1 s: filtrati
  per WIN/LOSE perché troppo corti come "fanfara" (selezionati i due più
  lunghi della cartella, 1.15 s e 1.32 s).
- Kenney sub-cartelle `8-Bit jingles` (NES chiptune), `Hit jingles` (drum-only
  marziale), `Sax jingles` (jazzy/lounge), `Steel jingles` (steeldrum
  tropicale): escluse per non aderenza al mood "videogame premium classico"
  (SPEC §13.4).
