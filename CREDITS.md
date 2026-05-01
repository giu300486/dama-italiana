---
last-updated: 2026-05-01
license-policy: CC0 only (verified per asset). No CC-BY, no CC-BY-NC, no CC-BY-SA, no "free with credit".
phase: F3.5 (Task 3.5.1 — asset acquisition approved 2026-05-01)
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

## Audio — Music

Sorgenti da [OpenGameArt.org](https://opengameart.org), filtrate per CC0.
Ogni pagina mostra la stringa "CC0" verbatim. 3 tracce su 4 sono WAV
all'origine; il re-encoding a OGG Vorbis q5 (`ffmpeg -i in.wav -c:a libvorbis
-q:a 5 out.ogg`) è permesso da CC0.

> **Stato binari**: il download dei 4 file e il re-encoding sono **deferred
> a Task 3.5.4** (AudioService implementation). Motivazione: il setup di
> ffmpeg portable in Task 3.5.1 ha richiesto un download non completato; il
> re-encoding sarà eseguito in 3.5.4 come parte naturale dell'integrazione
> AudioService. Selezioni e URL sotto sono **definitivi e approvati**.

| Local file | Source page | Direct file URL | License | Author | Origine size |
|---|---|---|---|---|---:|
| `audio/music/calm_piano_1.ogg` | https://opengameart.org/content/calm-piano-1-vaporware | https://opengameart.org/sites/default/files/003_Vaporware_2.mp3 | CC0 1.0 | The Cynic Project (cynicmusic) | MP3 6.6 MB (no re-encode) |
| `audio/music/first_light_particles.ogg` | https://opengameart.org/content/first-light-particles-%E2%80%93-cc0-atmospheric-pianoambient-track | https://opengameart.org/sites/default/files/first_light_particles_0.wav | CC0 1.0 | Yoiyami | WAV 25.3 MB (re-encode WAV→OGG) |
| `audio/music/at_home_orchestral.ogg` | https://opengameart.org/content/at-home-orchestral | https://opengameart.org/sites/default/files/cinematic-calm.wav | CC0 1.0 | Wolfgang_ | WAV 13.1 MB (re-encode) |
| `audio/music/peaceful_forest.ogg` | https://opengameart.org/content/peaceful-forest | https://opengameart.org/sites/default/files/Peaceful%20Forest.wav | CC0 1.0 | Samza | WAV 22.3 MB (re-encode) |

## Audio — SFX

Tutti i pack [Kenney.nl](https://kenney.nl) sono CC0 1.0 a livello di sito
(banner globale + stringa "License: Creative Commons CC0" su ogni pack page).
File specifici estratti dai pack ZIP, rinominati nel canonical naming e
copiati nel repo. Durate **verificate via mutagen** (Python OGG metadata
reader) il 2026-05-01.

| Local file | Source pack | Source filename in pack | License | Duration | Size on disk |
|---|---|---|---|---:|---:|
| `audio/sfx/move.ogg` | https://kenney.nl/assets/impact-sounds | `Audio/impactWood_light_000.ogg` | CC0 1.0 | 266 ms | 6.2 KB |
| `audio/sfx/capture.ogg` | https://kenney.nl/assets/impact-sounds | `Audio/impactWood_heavy_000.ogg` | CC0 1.0 | 313 ms | 6.1 KB |
| `audio/sfx/promotion.ogg` | https://kenney.nl/assets/interface-sounds | `Audio/confirmation_002.ogg` | CC0 1.0 | 539 ms | 13.8 KB |
| `audio/sfx/illegal.ogg` | https://kenney.nl/assets/interface-sounds | `Audio/error_001.ogg` | CC0 1.0 | 165 ms | 7.2 KB |
| `audio/sfx/win.ogg` | https://kenney.nl/assets/music-jingles | `Audio/Pizzicato jingles/jingles_PIZZI07.ogg` | CC0 1.0 | 1.32 s | 18.7 KB |
| `audio/sfx/lose.ogg` | https://kenney.nl/assets/music-jingles | `Audio/Pizzicato jingles/jingles_PIZZI03.ogg` | CC0 1.0 | 1.15 s | 16.6 KB |

Tutti i file sono OGG Vorbis 44100 Hz; SFX brevi sono stereo o mono a
seconda del pack (Impact Sounds: stereo 160 kbps; Interface Sounds: mono
96-239 kbps; Music Jingles: stereo 160 kbps). JavaFX Media supporta
nativamente entrambi i formati.

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
