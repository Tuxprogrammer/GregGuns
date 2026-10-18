# Asset generators

Every GregGuns texture, sound, logo and infographic is generated procedurally by the scripts here, so nothing is copied from Techguns, which is All Rights Reserved. Re-running a generator reproduces its output from fixed seeds.

| Script | Makes | Output |
|---|---|---|
| `build_manifest.py` | the asset list, built from the spec and design records | `manifest.json`, [docs/assets/MANIFEST.md](../../docs/assets/MANIFEST.md) |
| `icons.py` (uses `pixel.py`) | 16×16 item icons | `src/main/resources/assets/gregguns/textures/items/` |
| `blocks.py` (uses `blocktex.py`, `machines.py`, `blockart.py`, `fx.py`) | block, machine-overlay, casing and particle textures, including animated strips with `.mcmeta` | `textures/blocks/`, `textures/particles/` |
| `gui.py`, `models.py` (use `modeltex.py`) | GUI sprite sheets; armor, entity and model textures. UV rectangles are listed in `model_layouts.md` | `textures/gui/`, `textures/models/` |
| `gen_sounds.py` (uses `sfx.py`, `sndlib.py`, `sndengines.py` and the per-family `sounds_guns.py`, `sounds_npcs.py`, `sounds_effects.py`, `sounds_misc.py`; `sound_preview.py` draws spectrograms) | `.ogg` sound effects, matched to the [audio profiles](../../docs/techguns-spec/AUDIO-PROFILES.md) and to the measured targets in `sound-targets.json` | `sounds/`, `sounds.fragment.json` |
| `sound_compare.py` | numeric A/B check of generated sounds against `sound-targets.json` (band balance, contour, decay points, onsets, loudness) | console |
| `branding.py` | logo, banner, social preview, infographics | [docs/assets/](../../docs/assets/README.md) |
| `merge_credits.py` | merges `credits/*.csv` into [CREDITS.md](../../docs/assets/CREDITS.md) | |

## Run

Requirements: Python 3 with numpy and Pillow, and ffmpeg with libvorbis on `PATH`. `branding.py` also needs a Playwright browser.

```sh
python -I -X utf8 tools/asset-gen/icons.py
python -I -X utf8 tools/asset-gen/blocks.py
python -I -X utf8 tools/asset-gen/gui.py
python -I -X utf8 tools/asset-gen/models.py
python -I -X utf8 tools/asset-gen/gen_sounds.py
python -I -X utf8 tools/asset-gen/branding.py
python -I -X utf8 tools/asset-gen/merge_credits.py
```

Contact sheets for review are in `docs/assets/previews/`. The sound loudness report is `docs/assets/previews/sounds-report.md`.

### Sounds: revising after an A/B review
- `sound-status.json` holds each sound's latest review verdict. `gen_sounds.py --non-keep` re-renders only the sounds that are not `keep`; `--only-file PATHS.txt` renders a chosen list. Keep sounds are never re-rendered and stay byte-identical (check with `sha256sum` before and after).
- Recipes live in the family modules, one function per sound family, registered by lower-case name prefix with `@recipe("name_prefix_*")`. Shared engines are in `sndengines.py` and the conforming stage (band balance, loudness contour, crest, silent end) is in `sndlib.py`. Give each family its own agent.
- `sound-targets.json` holds the numeric measurements of the sound being replaced: length, decay points, onset times, band energies, centroid, crest, loudness contour. These are numbers only; no Techguns audio is read, decoded, sampled or convolved.
- Check a change with `python -I -X utf8 tools/asset-gen/sound_compare.py --only SUBSTR -v`, which measures the generated `.ogg` the way the review does and prints the differences from the targets. Set `GG_DEBUG=1` while rendering to print the contour and band balance after each conforming stage.

## Rules
- Follow [0006 art direction](../../docs/design/0006-art-direction.md).
- Never open, trace or resample a Techguns asset. Use only the footprint numbers and the written briefs.
- After changing a generator, re-run it and `merge_credits.py`, then review the contact sheet.
