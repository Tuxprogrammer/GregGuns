# GregGuns spec

How GregGuns differs from Techguns, and how it fits into GT New Horizons.

The [Techguns spec](../techguns-spec/README.md) records what Techguns does, bugs included. That is the baseline. This spec records the **delta**: what GregGuns changes, and how each feature connects to GregTech and the other mods in the pack. Read a GregGuns sheet next to the Techguns sheet it links to.

| Document | What it holds |
|---|---|
| [CONVENTIONS.md](CONVENTIONS.md) | When a sheet exists, the folder layout, the sheet template and requirement IDs |
| [INTEGRATION.md](INTEGRATION.md) | How to research another mod's API: the reference pack, source first, decompiling as a fallback, and citation comments |
| [PACK-CHANGES.md](PACK-CHANGES.md) | Every config or data change the GTNH modpack needs for GregGuns, collected from the areas |
| [Design records](../design/README.md) | The accepted decisions (tier map, machines, energy, damage, world presence, art) that this spec builds on |

## Areas

An area has a folder only when it has integration points. Numbers match the Techguns spec.

| Area | Techguns baseline | Integration focus |
|---|---|---|
| 01-core | [01-core](../techguns-spec/01-core/index.md) | Config, networking, HUD, keybinds, replaced mod integrations |
| 02-guns | [02-guns](../techguns-spec/02-guns/index.md) | GT recipe maps, IC2 electric weapons, damage sources, offhand |
| 03-gun-visuals-audio | [03-gun-visuals-audio](../techguns-spec/03-gun-visuals-audio/index.md) | Angelica rendering, shaders, dynamic lights, sound categories |
| 04-projectiles-fx | [04-projectiles-fx](../techguns-spec/04-projectiles-fx/index.md) | Explosions, block protection, particles under Angelica |
| 05-armor | [05-armor](../techguns-spec/05-armor/index.md) | Baubles slots, IC2 electric armour, GT hazard protection, flight and oxygen gear |
| 06-items | [06-items](../techguns-spec/06-items/index.md) | GT materials and fluids, pack loot tables, tooltips, hazards |
| 07-blocks | [07-blocks](../techguns-spec/07-blocks/index.md) | Block IDs, decorative-block overlap |
| 08-machines-production | [08-machines-production](../techguns-spec/08-machines-production/index.md) | GT MetaTileEntities, recipe maps, NEI, ModularUI2 |
| 09-machines-utility | [09-machines-utility](../techguns-spec/09-machines-utility/index.md) | Turret overlap, camo, repair, explosive charges |
| 10-entities-npcs | [10-entities-npcs](../techguns-spec/10-entities-npcs/index.md) | Infernal Mobs, mob-drop display, aspects, spawning rules |
| 11-worldgen | [11-worldgen](../techguns-spec/11-worldgen/index.md) | RWG and biomes, GT ore and oil generation, structure loot |

The area links are added as each area's sheets land.
