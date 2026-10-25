# 07-visuals-audio (GregGuns ICBM)

## Overview

The art and sound for the module: warhead and missile 3D models, textures, effects and sounds. Sourcing follows D33: human-made, properly licensed assets first, generation only as a last resort. ICBM baseline: [icbm-spec 07-visuals-audio](../../icbm-spec/07-visuals-audio/index.md), which is reference only: its assets cannot be reused as is (license).

## Contents

All sheets are planned.

- asset-sourcing.md (planned): the source order, license rules, third-party folder and CREDITS entries (D28, D33, D37).
- warhead-and-body-models.md (planned): 3D models by tier, collision boxes, filler blocks, the shared model for the flying missile (D31).
- textures.md (planned): kept-asset entries only, one catalog line per other asset (D3).
- effects.md (planned): engine smoke, launch, blast visuals, red matter renderer rebuild.
- sounds.md (planned): launch, flight, blast and alarm sounds with provenance.
- lang.md (planned): module keys.

## Integration map

| ICBM feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Warhead and missile look | Real 3D models sized by tier, static block model when placed, same model in flight | GregGuns client | [D31](../DECISIONS.md#d31-in-detail) |
| ICBM art and sounds | Not reused (licenses); source licensed replacements | third-party folder | [D28](../DECISIONS.md#d28-in-detail), [D33](../DECISIONS.md) |
| Third-party code and assets | MIT preferred; CC0, CC-BY, MIT, LGPL accepted with notices | none | [D37](../DECISIONS.md) |
| Sounds of unknown origin | Re-source under a known license | freesound, CC0 | [D28](../DECISIONS.md#d28-in-detail) |
| Red matter renderer | Rebuilt | GregGuns client | [D10](../DECISIONS.md#d10-in-detail) |
| Style | GT conventions where [0006](../../design/0006-art-direction.md) needs it, restyling kept light | GT textures | [D28](../DECISIONS.md#d28-in-detail) |

## Decisions carried

D3, D10 (red matter), D28, D31 (models), D33, D37.

## Roadmap

[I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine) (warhead models; batched client session), [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2) (missile body and flight look), [I5](../../ROADMAP-ICBM.md#i5--endgame-warheads-and-systems) (red matter renderer).

## Pack-side changes

None yet.

## Related areas

[02-explosives](../02-explosives/index.md), [03-missiles](../03-missiles/index.md), [06-blocks-items](../06-blocks-items/index.md).

## Open questions

- Which licensed sources supply the models: to settle in the I1 asset-sourcing sheet.
- Sizes of the antimatter and red matter warheads (D31 says larger than 1x1x2): to settle in the I1 model sheet.
