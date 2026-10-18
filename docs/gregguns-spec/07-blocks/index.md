# 07 Blocks: GregGuns spec

Pairs with [techguns-spec/07-blocks](../../techguns-spec/07-blocks/index.md).

## Overview

Most of the Techguns block catalogue has no integration point: shipped shapes, models, door sounds and textures are covered by the Techguns spec and the design records. Three topics do touch the pack, and each has a sheet.

## Contents

| Sheet | Covers |
|---|---|
| [decorative-blocks.md](decorative-blocks.md) | Chisel carving groups and connected textures, overlap with Chisel, ChiselTones and Ztones, the ForgeMultipart side effect |
| [storage-chests.md](storage-chests.md) | Reinforced and Weapon chests: InventoryBogoSorter, overlap with IronChest, JABBA and StorageDrawers |
| [block-platform.md](block-platform.md) | GT wrench and `IWrenchable`, Waila providers, EndlessIDs and ID space |
| [recipes.md](recipes.md) | Every recipe that makes a kept block, as GT Assembler, Mixer and crafting-table recipes |

No integration point; see the Techguns spec and the design records:

- Ore clusters, titanium iron and uranium ore blocks, Creeper Acid fluid: dropped by [0005](../../design/0005-world-presence.md) and [0002](../../design/0002-machine-deconfliction.md). GT owns ores and fluids.
- Alien nest blocks and the Zombie Hole: world gen, owned by [11](../../techguns-spec/11-worldgen/index.md).
- Door, ladder, lamp, sandbag, camo net, radar and panel models, sounds and textures: [block-rendering-and-models](../../techguns-spec/07-blocks/block-rendering-and-models.md), [sounds](../../techguns-spec/07-blocks/sounds.md), [textures](../../techguns-spec/07-blocks/textures.md).
- Machine blocks (`basicMachine`, `oreDrill`): [08](../08-machines-production/) and [09](../09-machines-utility/index.md).
- Block hardness and harvest tiers: the Techguns values stand. GT wrench rules are in block-platform.md. No pack mod changes them.

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Chisel CTM and carving groups | Soft dependency, `gregguns_` group names | Chisel 2.17.34-GTNH | [decorative-blocks.md](decorative-blocks.md) |
| Decorative overlap (concrete, panels) | Kept, separate groups | Chisel, ChiselTones, Ztones | [decorative-blocks.md](decorative-blocks.md) |
| Inactive ForgeMultipart code | Microblock materials come from the Chisel helper | ForgeMultipart 1.7.16 | [decorative-blocks.md](decorative-blocks.md) |
| Chest GUI | Opts in to the sorter | InventoryBogoSorter 1.3.54-GTNH | [storage-chests.md](storage-chests.md) |
| Wrench pickup | Harvest tool `wrench` plus `IWrenchable` | GT5-Unofficial, IC2 API | [block-platform.md](block-platform.md) |
| Block tooltips | Waila provider via IMC | Waila 1.19.34, GT5-Unofficial | [block-platform.md](block-platform.md) |
| ID space | Name-based registration only | EndlessIDs 1.7.4 | [block-platform.md](block-platform.md) |
| Block crafting recipes (B11 to B34) | Bulk recipes in the GT Assembler, concrete in the Mixer, small ones kept in the crafting table | GT5-Unofficial recipe maps | [recipes.md](recipes.md) |

## Pack-side changes

None.

## Related areas

- [09-machines-utility](../09-machines-utility/index.md): the charge, turret and bench blocks that use the wrench and Waila rules.
- [08-machines-production](../08-machines-production/): MTE blocks and their Waila support.
- [11-worldgen](../../techguns-spec/11-worldgen/index.md): nest blocks.

## Open questions

- Join Chisel groups or not for metal panels (decorative-blocks.md).
- Weapon Chest filter or drop (storage-chests.md).
- Wrench availability at early tiers (block-platform.md).
- Do blocks get a tier in [0001](../../design/0001-tier-map.md)? Closed by [0010](../../design/0010-material-remap-and-tier-additions.md): decorative blocks are LV; the chests are LV (Steel).
