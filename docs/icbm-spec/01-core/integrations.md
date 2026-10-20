# Integrations and Datafixers

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Classes: `mods/ModInteraction`, `mods/ModProxy`, `mods/jei/*` (7 classes), `mods/mekanism/MekProxy`; `datafix/*` (9 classes); plus the data files `recipes/explosives/mekanism/*.json` and the Atomic Science defaults in `content/gas/ProtectiveArmorHandler`.

Everything here is optional glue for 1.12 mods. **GTNH does not ship Mekanism or JEI** (it uses NEI), and the 1.7.10 line has no data fixers, so almost nothing in this sheet carries over. It is recorded so the rebuild can decide what to replace with NEI, GT and pack hooks.

## 1. Hook points

| Hook | What it does |
|---|---|
| `ModInteraction.preInit/init/postInit` | Called from the proxy's three phases. Forwards to the Mekanism proxy only (the one mod proxy). `ModProxy` is the base class with empty `preInit/init/postInit/onTileValidate/onTileInvalidate` |
| `@JEIPlugin` `JEIProxy` | Discovered by JEI itself, no ICBM call |
| Recipes JSON | Conditions on mod items (`minecraft:item_exists`) and on ore names (`icbmclassic:ore_value`) |
| `ProtectiveArmorHandler` defaults | Names the Atomic Science hazmat items |

## 2. JEI (*1.12-only; NEI equivalent*)

Config: `mods/jei.cfg` keys `disable` and `disable_payload_recipes` ([config](config.md#8-modsjeicfg-configjei)).

### Subtype interpreters

JEI normally treats all stacks of one item as the same ingredient. The plugin registers interpreters so that stacks holding different contents show up as different ingredients. Each returns a string key: the item metadata, and if there is content, `:` + the content's registry name + `:` + the content's subtype info (or its metadata).

| Item | Key adds |
|---|---|
| Parachute, balloon | the held item of the cargo projectile data (if it is a cargo type) |
| Cluster missile | the first stack in the cluster's spawn list |
| Held-item missile | the held item |

Registered only when `disable` is false.

### Generated recipe pages

When neither config flag is set, the plugin walks **every `ItemStack` ingredient JEI knows** and, for each that passes the corresponding allow-list check, adds crafting-category "recipes" that represent the dynamic recipes in code:

| Check (owner) | Pages added (registry name shown by JEI) | Wrapper class |
|---|---|---|
| cargo holder allow list (03) | balloon with item (`balloon_cargo`), parachute with item (`parachute_cargo`), and the two decraft pages | `CargoItemWrapper`, `DecraftCargoItemWrapper` |
| held-item missile allow list (03) | held-item missile with item (`held_item_missile`) and its decraft | `HeldItemMissileWrapper`, `DecraftHeldItemMissileWrapper` |
| cluster allow list (03) | cluster missile with item (`cluster_missile`) | `ClusterItemWrapper` |

Wrapper behavior: a craft page shows the holder (balloon, parachute, missile) plus the item as inputs and a copy of the holder with the payload set as the output; a decraft page shows the loaded holder as input and the item as output. Result stacks are built by writing the payload through the item's projectile-stack or missile-stack capability.

A fourth wrapper, `ClusterItemAllWrapper`, would show the all-items "fill the whole 3x3 grid, random or fixed" cluster recipe but its registration is commented out in the plugin (dead code).

Nothing is registered for machines, blasts, or any process. There are no JEI categories of ICBM's own.

For GregGuns: the equivalent is NEI recipe handlers or, better, a real GT recipe map for the cargo and cluster crafts. See [docs/gregguns-icbm-spec](../../gregguns-icbm-spec/README.md).

## 3. Mekanism

`MekProxy` (singleton, all methods guarded by `@Optional.Method(modid="mekanism")`). Content depends on Mekanism 1.12 block and item names.

### EMP block swaps (`preInit`)

Adds default entries to the EMP block replacement table (the table that decides what blocks change when an EMP pulse hits them; owner 02/05) for the four Mekanism blocks `MachineBlock`, `MachineBlock2`, `MachineBlock3`, `EnergyCube`: with order 0, an entry that places no block and only merges the block entity NBT value `electricityStored` = 0 into the existing tile (the replacement data type applies a chance, an optional block state and optional NBT; here the state is unset and the chance is -1, meaning always). The EMP action applies a matching entry and skips its own per-tile handling for that block. Net effect: an EMP empties the stored electricity of every Mekanism machine and energy cube in range without destroying it. The comment notes "replace with Mek's API", and it is needed because Mekanism uses a central block with many sub-blocks.

(`BlockReplacementData` and the config list parse the same grammar as the user-facing EMP replacement list; see [config](config.md#10-derived-data-tables-built-from-config).)

### Teleporter interaction (`init`)

If both Mekanism blocks are present (`MachineBlock` and `BasicBlock`), registers a projectile-hit reaction on the state "basic block meta 7" (commented as the teleporter frame): when a projectile (missile, grenade, etc., see `ProjectileBlockInteraction` in 03) hits a frame:

1. Take the block adjacent on the hit side. Search that position and the one to three blocks below it for a Mekanism teleporter (machine block meta 11).
2. If found, **teleport the projectile**: projectiles are moved slightly back along their path (`moveTowards(hit, -0.5)`), other entities are placed above the teleporter. Then call the teleporter block entity's `teleport` method by reflection (class name checked first).
3. If the entity survives in the same world (a cross-dimension teleport removes it), look for the teleporter at the new position, find a horizontal side that is air, and step the entity one block out through that side at the same relative height. Rotate the entity's horizontal motion and facing to match the exit side (a missile re-aims along its new motion; a plain entity's yaw is adjusted by 0/90/180/270 degrees by side). Send a `PacketEntityPos` to all players within 400 blocks.
4. The hit reaction returns `TELEPORTED` if a teleporter was found, else `CONTINUE` (pass through).

Any exception in the reflection call is logged and swallowed.

So a missile can be fired into a Mekanism teleporter portal and come out of its destination. The yaw rotation uses simple modular arithmetic; some axis combinations leave the motion unchanged (the motion swap only runs when the entry and exit axes differ).

### Recipes (data)

`recipes/explosives/mekanism/` swaps the default recipes for three explosives to use Mekanism's `atomicalloy` item, enabled only when that item exists:

| Output | Pattern | Condition |
|---|---|---|
| nuclear explosive (id 15) | `tit / iii / tit`; t = thermobaric explosive (id 13), i = atomic alloy | atomic alloy exists AND no `ingotUranium` ore AND no `pelletUranium235` ore |
| antimatter explosive | atomic alloy ring around the nuclear explosive | atomic alloy exists |
| red matter explosive (id 23) | atomic alloy ring around the antimatter explosive (id 22) | atomic alloy exists |

`recipes/explosives/vanilla/` holds the non-Mekanism variants (same three outputs). Recipe details for all explosives are in 02/06.

### Other flags

`isMekanismLoaded()` returns true after `init` runs the Mekanism branch (even when the two Mekanism blocks were missing).

## 4. Datafixers

*1.12-only mechanism* (Forge `IFixableData`, applied while loading old saves). Seven fixers registered in `preInit` under mod fixer version 5. They migrate saves of older ICBM versions. A fixer runs for worlds saved at a lower version number than its own `getFixVersion`.

| Fixer | Applies to | Version | What it migrates |
|---|---|---|---|
| `EntityExplosiveDataFixer` | entity `icbmclassic:block.explosive` | 2 | (a) If the entity saves an `explosive_stack` with damage = hypersonic, change damage to sonic and wipe `tag` and `ForgeCaps` ("hypersonic" was removed in 4.3.0). (b) Else if it has the old int `explosiveID`, build a stack of the explosive block with that damage and store it under `explosive_stack`, deleting `explosiveID` and `data` |
| `EntityBombCartDataFixer` | entity `icbmclassic:cart.bomb` | 2 | Rewrites the **int** tag `explosive` from hypersonic to sonic. Current saves store `explosive` as a compound, so the fix only matches a very old format (ICBM-01-16) |
| `EntityGrenadeDataFixer` | entity `icbmclassic:item.grenade` | 1 | Renames the int tag `haoMa` (the original Chinese-pinyin name for "ID") to `explosiveID` |
| `EntityMissileDataFixer` | entity `icbmclassic:missile` (v4.0.1 and older) and `icbmclassic:explosive_missile` | 2 | See below |
| `TileExplosivesDataFixer` | tile `icbmclassic:explosive` | 1 | Converts the int `explosiveID` into an `explosive_stack` item compound. Remaps IDs: 14 (S-Mine, removed) becomes condensed (0); hypersonic becomes sonic; any ID above 14 shifts down by 1. If a stack already exists, converts a hypersonic damage to sonic (wiping `tag` and `ForgeCaps`) |
| `TileRadarStationDataFixer` | tile `icbmclassic:radarstation` | 5 | Renames `alarmBanJing` / `alarmRadius` to the detection-range key, and `safetyBanJing` / `safetyRadius` to the trigger-range key. Reads only the first spelling of each (ICBM-01-14) |
| `ItemStackDataFixer` | item stacks of explosive missile, bomb cart and explosive block | 2 | Hypersonic damage becomes sonic; wipes `tag` and `ForgeCaps` |

### Old missile entity conversion (`EntityMissileDataFixer`)

Missiles were rewritten in v4.2.0 (new registry name, new save layout). For a save with the old id `icbmclassic:missile`:

| Old field | New location / meaning |
|---|---|
| id | changed to the current explosive missile entity id |
| `inGround` | `flags.ground` |
| `xTilePos`, `yTilePos`, `zTilePos`, `sideTilePos`, `inTileState` | `ground.pos` (x, y, z), `ground.side`, `ground.state` (the block state decoded from the old combined ID) |
| `ticksInAir`, `life` | `ticks.air`, `ticks.ground` |
| `explosiveID` | `explosive` = the missile item stack for that explosive (from the explosive registry) |
| `missileType` | selects the flight logic and target type below |
| `target` (xyz), `targetHeight` | `missile.target` = ballistic target data (type 0, with `impact_height`) or basic target data (all others) |
| `launcherPos` (type 0), `sourcePos` (types 1, 2) | `missile.source.pos` (no dimension was stored) |
| `lockHeight`, `preLaunchSmokeTimer`, Pos | type 0 (launcher missile): ballistic "old" flight logic with `flight_started` false (force a recalculation), start = entity position, end = target, `timers.engine_warm_up` and `timers.lock_height` |

Old `missileType` values: 0 launcher, 1 cruise, 2 rpg, 3 homing, 4 dead-aim. Mapping: type 0 uses ballistic-old flight; every other type becomes the "dead" (no-op) flight logic, with the stored `fuel` set to the cruise fuel (type 1) or handheld fuel (type 2) from the missile config. The flight-started flag is set to "doFlight = true" for all ("any missile in the world is moving"). The code has TODOs: "verify for cruise and hand held", "wrap in optionals".

All old keys are then deleted. Current-format missiles with a hypersonic stack are meant to be converted by the second branch, which reads the wrong tag name and never matches (ICBM-01-15).

`getExplosiveData(explosiveId)` for an ID that no longer exists would return null and the next call dereferences it (no guard). The S-Mine shift is applied only to block entities, not to entities or items (ICBM-01-17).

### Helpers

`DataFixerHelpers.removeTags` (delete named keys) and `removeNestedTag` (delete one key at a nested path, ignoring missing parents).

### Rebuild note

None of this carries over: GregGuns starts a fresh save format. Keep only the list of legacy names (`haoMa`, `alarmBanJing`, `safetyBanJing`) if a migration from ICBM-Classic worlds is ever wanted.

## 5. Atomic Science defaults

`ProtectiveArmorHandler` pre-fills ratings for the Atomic Science hazmat suit (mask 0.75, body 0.15, leggings 0.05, boots 0.05, plus the four "color" variants) so that the suit gives full protection without config. The mod is not present on GTNH. The GregGuns analogue is the GT hazmat suits (see the GregGuns spec).

## 6. Summary for the rebuild

| Hook | Port? |
|---|---|
| JEI plugin | Replace with NEI/GT recipe display |
| Mekanism EMP swap | Replace with a GT-aware EMP rule (empty GT machine and battery energy; see 05) |
| Mekanism teleporter | Drop (no Mekanism) |
| Mekanism recipes | Drop; the vanilla-recipe variant shows the shape |
| Datafixers | Drop |
| Atomic Science defaults | Replace with the GT hazmat armor list |
