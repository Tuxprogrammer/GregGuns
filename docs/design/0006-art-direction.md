# 0006 · Art direction

- Status: Accepted (by default during the MVP push; revisit after MVP)
- Gate: D6
- Date: 2026-10-17

## Context
Every GregGuns asset must be original (Techguns is All Rights Reserved), yet still fit next to GT's items. The footprints come from [ASSET-FACTS.md](../techguns-spec/ASSET-FACTS.md) and each area's `textures.md` and `sounds.md`. [AUDIO-PROFILES.md](../techguns-spec/AUDIO-PROFILES.md) describes every sound and gives the loudness to match.

## Decision
- **Item icons:** 16×16, in GT-style shading (soft top-left light, darker outlines, a restrained palette). Materials take their colour from the GT material colour wherever an item is "made of" one.
- **Model textures:** keep each Techguns footprint (size, UV layout and frame count) so a model can be rebuilt to the spec. Paint new art in that footprint.
- **Sounds:** match the role, length and loudness of the original. Aim for integrated loudness within ±3 LU of the measured original. Positional sounds are mono (GVA-23); only UI and ambience sounds may be stereo.
- **Provenance:** every asset is listed in `docs/assets/CREDITS.md` with its author, source and licence. Generated assets record the tool and prompt.

## Consequences
- An artist or an asset-generation agent can work from the spec plus this record, without seeing the Techguns originals.

## Alternatives considered
- **A new visual style unrelated to GT:** rejected, because it wouldn't fit the pack.
