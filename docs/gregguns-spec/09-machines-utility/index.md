# 09 Machines (utility): GregGuns spec

Pairs with [techguns-spec/09-machines-utility](../../techguns-spec/09-machines-utility/index.md). Decisions on which machines survive are in [0002](../../design/0002-machine-deconfliction.md).

## Overview

Of the utility machines, only the Turret, the explosive charges and the Repair Bench touch something in the pack. Each of those has a sheet. The rest have no integration point.

## Contents

| Sheet | Covers |
|---|---|
| [turret.md](turret.md) | Turret vs OpenModularTurrets, ownership and friends through ServerUtilities, Infernal Mobs exclusion |
| [explosive-charges.md](explosive-charges.md) | Claim protection through the break event, GT ore drops, overlap with GT mining explosives |
| [repair-bench.md](repair-bench.md) | GT materials and unification as repair inputs |
| [recipes.md](recipes.md) | Recipes for every tier of the Turret and Repair Bench, the Camo Bench, the turret armor plates, the Armory Gantry and Rampart controllers and casings (incl. Bastion Plating), the Gun Mount Hatch and both explosive charges |

Machine kinds, tier ranges and IDs for the Turret, Repair Bench and Camo Bench are in the [machine roster](../08-machines-production/machine-roster.md). The Turret and the Repair Bench also have multiblock forms, the Rampart and the Armory Gantry, specified in [multiblocks.md](../08-machines-production/multiblocks.md). The single blocks stay as described in the sheets above.

No integration point; see the Techguns spec and the design records:

- **Camo Bench.** It only cycles the camo index of guns, armor and our own camo nets ([camo-bench.md](../../techguns-spec/09-machines-utility/camo-bench.md)). Chisel, CarpentersBlocks and ArchitectureCraft change the look of blocks, not items, so there is no scope clash and nothing to call.
- **Scrapper, Ore Drill.** Dropped in 0002. GT miners and the GTNH ore system replace the Ore Drill; see [GT machines in 08](../08-machines-production/).
- **Camp Flag.** A spawner for generated structures; owned by [10](../../techguns-spec/10-entities-npcs/index.md) and [11](../../techguns-spec/11-worldgen/index.md).
- **Bio Blob, weapon and reinforced chests.** Blocks; see [07-blocks](../07-blocks/index.md).
- **Kept machines as GT MetaTileEntities.** The machine IDs, base classes and GUI framework are researched in [08](../08-machines-production/). This area only links to it.

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Turret platform | GT MTE; gun-fed turret that does not clash with OMT | OpenModularTurrets 2.4.8 | [turret.md](turret.md) |
| Turret "friends" | ServerUtilities teams, owner-only fallback | ServerUtilities 2.4.14 | [turret.md](turret.md) |
| Mounted turret entity | Non-`IMob`, `IEntityOwnable` | Infernal Mobs 1.10.7-GTNH | [turret.md](turret.md) |
| Charge break event | Real player in the event | ServerUtilities 2.4.14 | [explosive-charges.md](explosive-charges.md) |
| Charges vs mining explosives | Directional bore, kept | GT5-Unofficial (Powder Barrel, GT++ Mining Explosives) | [explosive-charges.md](explosive-charges.md) |
| Repair Bench inputs | GT prefix and material, unified matching | GT5-Unofficial | [repair-bench.md](repair-bench.md) |
| Machine and charge recipes (B04 to B09, R58 to R61) | Hull-and-parts shaped recipes for the MTEs at every tier; Assembler for plates, casings, hatches and charges | GT5-Unofficial (`MTERecipeLoader`, `assemblerRecipes`) | [recipes.md](recipes.md) |

## Camo Bench built notes (M4 wave A)

Built as `MTECamoBench` (ID 20115, LV) with `PacketCamoCycle`. Decisions, each marked `// Deviation:` in code:

- **No EU.** The sheets give an LV tier and no process; a click needs no time, so the bench draws nothing. The recipe still uses `Hull_LV`.
- **Targets.** The item in the bench's slot and the four worn armor pieces. The Techguns back and face slots (buttons 11 to 14) do not exist in GregGuns.
- **Opt-in.** An item is cycled when it implements `CamoChangeable` (camo count, index, name key). Armor does, and so does every gun: `ItemGun` implements it with the camo count its `GunDefinition` declares (`bolt_action_rifle` has 3 skins, the other guns 1), storing the index in `GunNbt.KEY_CAMO` and leaving the rounds alone. A test pins each count to the `<name>_camoN.png` files.
- **Packet.** The click is a `ServerboundPacket`: same dimension, within 8 blocks, a known target, a Camo Bench at the position, then `isAuthorized` asks GT's `isUseableByPlayer`. Only the camo index changes; every other tag is kept.
- **GUI.** GT's frame with the slot, a pair of arrows per target and a text line showing index, count and camo name. Not built: the live player preview.
- **Automation.** No accessible slots, as in Techguns.

## Pack-side changes

None.

## Related areas

- [08-machines-production](../08-machines-production/): MetaTileEntity IDs and the machine framework.
- [10-entities-npcs](../../techguns-spec/10-entities-npcs/index.md): turret AI, factions and Infernal eligibility for hostile NPCs.
- [07-blocks](../07-blocks/index.md): chests, Waila, wrench pickup.

## Open questions

- Turret friend semantics (ALLY or MEMBER). Needs a turret-security record.
- Does the Repair Bench use EU? Settled in the build: yes, 2 s plus 8 s times the damage fraction at half the recipe voltage, charged at the button press ([repair-bench.md](repair-bench.md#built-notes-m4-wave-a)). Still needs a design record.
- Are charges balanced against GT and IC2 explosives? Needs a balance pass after the tier map is live.
- Turret and Repair Bench multiblocks (closed): the four turret armor plates are the crafting inputs of the Bastion Plating casing, and the tiered single blocks, controllers, casings and hatches are all in [recipes.md](recipes.md). Their costs are proposals for a balance pass.
- Machine tiers (Turret MV, Repair Bench LV, Camo Bench LV) and charge tiers (MV, HV) are proposals in [recipes.md](recipes.md), because [0001](../../design/0001-tier-map.md) lists no machines. Needs a line in 0001 or a short record.
