# 06-blocks-items (GregGuns ICBM)

## Overview

The catalog that ties the module together: what ICBM content is kept, dropped or deferred (D10), the GregGuns registry of items, blocks and tile entities, and the recipe remap onto GT recipe maps. ICBM baseline: [icbm-spec 06-blocks-items](../../icbm-spec/06-blocks-items/index.md), whose recipe catalog is the table this area remaps.

## Contents

All sheets are planned.

- kept-dropped-deferred.md (planned): the D10 table applied to every ICBM item and block.
- registry-catalog.md (planned): GregGuns items, blocks, tile entities and entities for the module, with the area that documents each.
- recipe-remap.md (planned): each ICBM recipe mapped to a GT machine recipe or dropped; the ICBM crafting items replaced by GT parts.
- antidote.md (planned): the antidote mapped to GT hazards.
- silo-guard-spawner.md (planned): the spawner block, defined with the world sheets in [08-worldgen](../08-worldgen/index.md).

## Integration map

| ICBM feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| ICBM crafting items, battery, sulfur, saltpeter, concrete, reinforced glass | Dropped; GT, IC2 or GregGuns provide them | GT5-Unofficial | [D10](../DECISIONS.md#d10-in-detail) |
| Spikes, glass button, glass pressure plate | Dropped (owner, 2026-10-25) | none | [D10](../DECISIONS.md#d10-in-detail) |
| Cargo parachute, balloon, cargo hook, held-item missile | Deferred | none | [D10](../DECISIONS.md#d10-in-detail) |
| Handheld rocket launcher, grenades, Christmas mobs, Nightmare missile | Dropped | none | [D10](../DECISIONS.md#d10-in-detail) |
| Antidote | Kept, mapped to GT hazards | GT `Hazard` | [D10](../DECISIONS.md#d10-in-detail), [D21](../DECISIONS.md#d21-in-detail) |
| ICBM recipes | Rebuilt on GT machines, shown in NEI | GT5-Unofficial, NEI | [D8](../DECISIONS.md#d8-in-detail), [D27](../DECISIONS.md) |
| Registry catalog | One GregGuns list, with the owning area per entry | GregGuns | [D10](../DECISIONS.md#d10-in-detail) |

## Decisions carried

D3 (catalog depth), D10, D21 (antidote and hazards), D27 (recipes).

## Roadmap

Built across [I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine) to [I5](../../ROADMAP-ICBM.md#i5--endgame-warheads-and-systems): each milestone adds the items and recipes it needs. The antidote is I5.

## Pack-side changes

None yet.

## Related areas

[02-explosives](../02-explosives/index.md), [03-missiles](../03-missiles/index.md), [04-launch-control](../04-launch-control/index.md), [05-detection-defense](../05-detection-defense/index.md), [07-visuals-audio](../07-visuals-audio/index.md).

## Open questions

- Antidote recipe and cure scope under GT hazards: to settle in the I5 antidote sheet.
