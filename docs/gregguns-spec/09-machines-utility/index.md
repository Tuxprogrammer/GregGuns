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

## M6 utility core built notes

`com.gregguns.utility` holds `UtilityModule` (module `utility`, needs `machines`), one registration stub per later chunk, `UtilityGuis` (FML GUI ids 30 and up, forwarded from `EquipmentGuiHandler`), `TurretPlate`/`ItemTurretArmor`/`UtilityParts` (the four plates and their recipes) and `security` (`SecurityLevel`, `FriendProvider`, `OwnerAccess`, `ServerUtilitiesFriends`, `FriendProviders`, `OwnerProfiles`). ServerUtilities is a `compileOnly` dependency; its adapter loads only behind `Loader.isModLoaded("serverutilities")` (GGS-09-06). MTE ids 20116 to 20124 are reserved in `GregGunsMTEIDs`. All tuning is in `UtilityConfig` (`docs/config.md`). Deviation: the Iron plate uses `plate.CastIron` (see [recipes.md](recipes.md)).

## Explosive charges built notes (M6 chunk 3)

One block `gregguns:explosive_charge` with two metas (0 TNT at MV, 1 Advanced at HV) and `TileExplosiveCharge`, in `com.gregguns.utility.charge`. Pure rules: `ChargeType`, `ChargeParams` (buttons 0 to 6), `ChargeFuse`, `ChargeGeometry`, `ChargeRecipeSpec`. Server work: `ChargeBlast` (break event for a real player from `OwnerProfiles`, fortune 0 drops before removal, no `Explosion`). The settings screen is GUI id 30 (`ChargeContainer`, `client.utility.ChargeGui`) and every button is a `PacketChargeButton`. Decisions, each marked in code:

- **Permissions (MUTL-31).** All seven buttons, arm included, need the owner or a ServerUtilities team member. An ownerless charge cannot be configured.
- **Tier.** The block reports HV (its highest meta) because the self-check reads one tier per block; `ChargeType.tier()` holds the per-meta MV/HV.
- **Liquids.** Liquid blocks in the tunnel are skipped, not removed (the baseline cleared them).
- **Armed and GUI.** A right-click on an armed charge does nothing, and the open GUI closes when the charge arms.
- **Falling off.** A charge whose mounting block disappears drops as an unarmed item.
- **Placeholder look.** `ChargeModel` holds the box list (1/16 block units, floor pose, rotated to the six faces); `client.utility.ChargeRenderer` draws it with a casing texture. Particles on detonation are not built. Visual check owed. Asset gaps: charge model textures (block icon uses `casings/gantry_casing`, the world render `casings/works_casing`).
- **Sounds.** Six events `effects.tnt_charge_*` and `effects.c4_charge_*` in `sounds.json`.

## Turret built notes (M6 chunk 2)

`MTETurret` (MTE 20116 to 20119, MV to IV; `TurretMachines`, `TurretRecipeSpec`) implements `TurretHost`; `TurretEntity` (registry id 60, `gregguns.turret`) talks to its base only through that interface, so the Rampart can reuse it. The pure rules are `TurretLogic` (upkeep, heal, repair, respawn), `TurretTargetSelector`, `TurretAmmo`, `TurretLink` and `RedstoneMode`. Decisions, each marked `// Deviation:` in code:

- **Link.** The base saves the entity UUID and finds it again with an AABB search filtered to `TurretEntity`; nothing casts an entity id (MUTL-25). A turret with no host rejoins by the base position it saved and is removed after 100 ticks if no base adopts it. The death notice with no host is a no-op (NPC-12).
- **Energy.** 4 EU/t upkeep, 120 EU per healed HP, 10 EU/t for 200 ticks of repair (`UtilityConfig`). The buffer is 64 ticks of the tier voltage. Energy enters on every side but the top.
- **Gun tier gate.** The gun slot refuses a gun above the Turret's tier. A gun that got there anyway (NBT edit) is ignored by the entity.
- **Ammo.** Magazine guns take their `ammoItemsPerReload` all or nothing and return the empty container; incremental guns are topped up; nothing is dropped unless a real overflow exists (MUTL-24).
- **Security.** Level 0, 1, 2 as in Techguns; the GUI and the redstone and animals buttons follow the level, and only the owner changes the level. Right-click by a refused player says so and opens nothing.
- **Not built.** Retaliation against whoever hurt the turret (M7 factions), `turretBeep` and `turretGunequip` sounds, the wrench-dismantle NBT carry, the death-at-placement flag, armor-dependent textures, the Techguns GUI art. The Turret recipe's Concrete ballast is GT's concrete block until the GregGuns Concrete exists.
- **Placeholder rendering.** The block is a GT casing with the Gun Mount Hatch front. The entity uses `TurretModel`, built from the one box list in `com.gregguns.client.utility.TurretGeometry` (units, frame and moving parts are in its Javadoc); the base boxes in that list are not drawn yet.

## Chests, Bio Blob and Camp Flag built notes (M6 world blocks)

- **Chests.** `reinforced_chest` and `weapon_chest` are one block class with a flag. Single chests only, 36 slots (3 x 12), public. The Weapon Chest accepts only non-block items registered under `gregguns:` (`WeaponChestFilter`), for players, shift-click and hoppers alike. Neither gives redstone power (MUTL-35); the viewer count is the chest's own (MUTL-34); shift-click has no special slots (MUTL-36). Not built: double chests, wrench pickup, the auto-food and ammo slots, the Bogosorter opt-in (GGS-07-10, -11, -14 stay open). The GUI is drawn with flat rectangles because `textures/gui/storage_chest.png` is a widget sheet, not a background.
- **Bio Blob.** `bio_blob`, size 1 to 3, burst past 3 with no terrain damage, one decay step per `bioBlobDecayTicks`, Poison III for 100 ticks on any living entity (players too unless `bioBlobSparesPlayers`). Only the server ticks; clients get the size in the description packet. The Biogun projectile calls `BlockBioBlob.onProjectileImpact(world, x, y, z, side, level, shooter)`.
- **Camp Flag.** `camp_flag`, hidden from the creative tab. World generation calls `TileCampFlag.init(...)`. Spawning and counting go through `CampSpawnPorts.get()`, which counts and spawns nothing until the NPC module installs its port. The cap (`maxNPCs`) is saved (MUTL-38); an uninitialized flag is inert (MUTL-39); breaking drops one iron ingot (MUTL-48). Placeholder geometry and animation numbers live in `FlagPostGeometry`.

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
