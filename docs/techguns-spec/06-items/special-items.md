# Item behavior: shared items, placers, scanners, tools and debug items

## Base classes

| Class | Role |
|---|---|
| `GenericItem` | A plain `Item` with stack 64 and the Techguns creative tab. The no-arg constructor uses unlocalized name `testItem` and texture `techguns:testItem`. The `(name)` constructor sets unlocalized name = `name` and texture = `techguns:<name>`. Both `getUnlocalizedName` overloads put `techguns.` in front of the vanilla result, so the key becomes `techguns.item.<name>`. |
| `GenericItemShared` | The single meta item `TechgunsAmmo` (110 metas). It implements [`ITGSpecialSlot`](../05-armor/special-equipment.md#1-interfaces-and-base-classes) so its metas can sit in the Techguns face/back/ammo slots (the player inventory itself is [05](../05-armor/index.md)/[01](../01-core/index.md)). |
| [`GenericItemBucket`](fluids.md#creeper-acid-bucket-item) | `ItemBucket` with the `techguns.` prefix (see [fluids.md](fluids.md)). |

<a id="slot-types"></a>
## Slot types (`TGSlotType`)

Each shared meta carries one of these. Machines and the player inventory read it through `getSlot(stack)`. Non-shared items report `NORMAL`.

| Enum | `toString()` | Used by |
|---|---|---|
| NORMAL | "Normal Item" | crafting items |
| FACESLOT | "Face" | Oxygen Mask (Techguns face slot) |
| BACKSLOT | "Back" | Glider (Techguns back slot) |
| AMMOSLOT | "Ammo" | ammo, mags, tanks, cells (Techguns ammo slots 6–11) |
| FOODSLOT | "Food" | (no shared item) |
| HEALSLOT | "Healing" | (no shared item) |
| TURRETARMOR | "Turret armor" | turret armor plates |
| REACTION_CHAMBER_FOCUS | "Reaction Chamber Focus" | UV Emitter, Heat Ray focus |
| DRILL_SMALL / DRILL_MEDIUM / DRILL_LARGE | "Small Drill" / "Medium Drill" / "Large Drill" | ore drill heads |

## Shared item (`GenericItemShared`) behavior

### Stack size and bad metadata
- `getItemStackLimit` returns the per-meta value from the catalog. If the meta is ≥ 110, it prints `Techguns GenericItem has bad metadata:<m> Max is:109` and returns 64.
- `getIconFromDamage` falls back to meta 0's icon for an out-of-range meta.
- `getUnlocalizedName` and the right-click handler do **not** range-check (bug [ITEM-05](index.md#suspected-bugs)).

### Right-click (in air), in priority order
1. **Sneaking while holding a full magazine** (pistol mag 2, AR mag 12, advanced mag 23, SMG mag 5, AS50 mag 17, minigun drum 46, LMG mag 20): one magazine is removed from the held stack. Its empty version (×1) and its contents go to the player through `addAmmoToPlayerInventory`, which fills the Techguns ammo slots first and then the main inventory:

   | Mag | Gives back |
   |---|---|
   | 9mm Pistol Magazine | Empty ×1 + 9mm Bullets ×3 |
   | Assault Rifle Magazine | Empty ×1 + Rifle Bullets ×3 |
   | Advanced Magazine | Empty ×1 + Advanced Bullets ×3 |
   | SMG Magazine | Empty ×1 + 9mm Bullets ×2 |
   | AS50 Magazine | Empty ×1 + High Caliber Rounds ×2 |
   | Minigun Ammo Drum | Empty ×1 + Rifle Bullets Stack ×4 |
   | LMG Magazine | Empty ×1 + Rifle Bullets Stack ×2 |

   This runs on both client and server. Anything that does not fit anywhere is silently lost (bug [ITEM-06](index.md#suspected-bugs)). For the duplication in the helper, see [ITEM-07](index.md#suspected-bugs).
2. **Not sneaking, item's slot type is AMMOSLOT**: the whole held stack is merged into the Techguns ammo slots (6–11). It first tops up matching stacks, then fills empty slots. The held stack shrinks to whatever did not fit.
3. **Item's slot type is BACKSLOT (the Glider)**: if the Techguns back slot is empty, a copy goes there and the held stack becomes 0. Otherwise nothing happens.
4. Otherwise: vanilla behavior (nothing).

Sneak + right-click on a non-magazine ammo item does nothing (branch 1 matches only full mags, and branch 2 is skipped while sneaking).

### Tooltips (`addInformation`)
- Ore drill heads (metas 75–83) get two lines:
  - `"<techguns.oreDrillTooltip.size> <size>"`, where size is `1x1x1` (small), `1x1xN or 3x3xN` (medium) or `5x5xN or 7x7xN` (large). English: `Valid for drills with size 1x1x1`.
  - `"<techguns.oreDrillTooltip.mininglevel>: <n>"`, where n = 1 for Steel, 2 for Obsidian Steel, 3 for Carbon. English: `Bonus mining level: 2`.
- Metas registered with `addMagazineTooltip` (2, 5, 12, 17, 20, 23, 46) get two lines: `Right click to put in ammo inventory` (`techguns.item.ammotooltip.ammobag`) and `Sneak + Right click to empty` (`techguns.item.ammotooltip.empty`).

### Armor bonus
`getBonus(OXYGEN_GEAR)` returns 1.0 for the Oxygen Mask (meta 90) and 0 for everything else. The mask counts as oxygen gear while it is in the Techguns face slot. Combined with Oxygen Tanks it refills air underwater. That logic, and the worn-model rendering of the mask, belong to [05-armor](../05-armor/index.md).

### Glider (meta 51), back-slot tick
Every player tick (phase START, on both client and server), the Techguns tick handler first clears `isGliding` and then calls the back-slot item's `onPlayerTick`. For the glider:

```
if player is airborne AND sneaking:
    if motionY < -0.1:
        isGliding = true
        dir = horizontal look vector (x, z), normalized
        lift = min(0.08, -0.1 - motionY)        # brakes the fall toward -0.1 blocks/tick
        motionY += lift
        motionX += dir.x * lift                  # converts the braking into forward speed
        motionZ += dir.z * lift
        jumpMovementFactor += 0.03               # extra air control
    fallDistance = 0                             # no fall damage while sneaking in the air
```

The terminal sink rate is therefore about 0.1 blocks/tick while sneaking, with forward glide along the look direction. A comment says the algorithm was borrowed from Modular Powersuits' glider.

Client side, `handlePlayerGliding`: when `isGliding` becomes true and no loop is playing, it starts the looping sound `techguns:glider.loop` attached to the player (volume 1, pitch 1, repeat, category PLAYER_EFFECT). When gliding stops, the sound is stopped. `isGliding` is also cleared when another player stops being tracked.

The worn glider model ([`textures/armor/glider.png`](../05-armor/textures.md#10-back-gear-layout-d-6464), [`ModelGlider`](../05-armor/rendering-and-models.md)) is drawn by the additional-slot renderer ([05-armor](../05-armor/index.md)). The Jetpack also reuses this glider logic ([05](../05-armor/index.md)).

<a id="3d-rendered-shared-items"></a>
### 3D-rendered shared items (`RenderGenericItem`)
The client registers `RenderGenericItem` as the `IItemRenderer` for the whole shared item. It claims only these metas, for every render type (inventory, entity, equipped, first person) and with every render helper. All other metas use the normal 2D icon.

| Meta | Model | Texture |
|---|---|---|
| 12 Assault Rifle Magazine | [`ModelM4Mag`](../03-gun-visuals-audio/gun-models.md#modelm4mag--modelm4mage-item-models-12864) | [`textures/guns/m4Texture.png`](../03-gun-visuals-audio/textures.md#texturesgunsm4texturepng--camo-variants-m4-m4-magazine-items) |
| 13 AR Magazine (Empty) | `ModelM4MagE` | `textures/guns/m4Texture.png` |
| 14 Rocket | [`ModelRocket`](../03-gun-visuals-audio/gun-models.md#modelrocket-rocket-item--projectile-12864-11-boxes) | [`textures/guns/rocketlauncher.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) |
| 17 AS50 Magazine | `ModelAS50Mag(empty=false)` | [`textures/guns/as50Texture.png`](../03-gun-visuals-audio/textures.md#texturesgunsas50texturepng-as50-as50-magazine-items) |
| 18 AS50 Magazine (Empty) | `ModelAS50Mag(empty=true)` | `textures/guns/as50Texture.png` |
| 20 LMG Magazine | [`ModelLmgMag`](../03-gun-visuals-audio/gun-models.md#modellmgmag--modellmgmage-item-models-128128) | [`textures/guns/mg2_texture.png`](../03-gun-visuals-audio/textures.md#texturesgunsmg2_texturepng-lmg-lmg-magazine-items) |
| 21 LMG Magazine (Empty) | `ModelLmgMagE` | `textures/guns/mg2_texture.png` |
| 105 Rocket (Tactical Nuke) | `ModelRocket` | [`textures/guns/nukerocketlauncher.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) |

So the mags share the gun model's texture atlas, and the nuke rocket is the normal rocket model with the nuke launcher's texture. The models and textures are documented in [03-gun-visuals-audio](../03-gun-visuals-audio/index.md). Each sub-renderer binds its texture and draws the model at scale 1/16 after a per-context transform:

| Renderer | First person | Equipped (3rd person) | Entity (dropped) | Inventory |
|---|---|---|---|---|
| M4 mag | rot X 180°, rot Y 225°, translate (−0.3, −1.5, 0.6) | rot X 180°, rot Y 135°, scale 0.8, translate (0.1, −1.3, 1.35) | rot X 180°, scale 0.65, translate (0, −0.65, 0) | rot X 180°, scale 2, translate (0, −0.45, 0.1) |
| AS50 mag | as M4 but translate (−0.3, −1.2, 0.6) | as M4 but translate (0.1, −1.15, 1.45) | rot X 180°, scale 0.8, translate (0, −0.35, 0.2) | rot X 180°, scale 1.75, translate (0, −0.2, 0.4) |
| LMG mag | as M4 | as M4 | rot X 180°, rot Y 90°, translate (0, −0.45, 0), scale 0.65 | rot X 180°, scale 1.35, translate (0, −0.45, 0.1) |
| Rocket | translate (1, 0, 0.5), rot Z 90°, scale 1.2 | rot X 180°, rot Y 225°, scale 1.5, translate (−0.5, −0.25, 0) | rot X 180°, rot Y 90° | rot X 180°, scale 1.45, translate (0, 0, −0.025) |

The constructors also take a scale and an xyz offset (all passed as 1.0 / 0,0,0), but these are never applied.

`GenericItemShared.registerIcons` still asks for `techguns:<name>` icons for these eight metas. No PNG exists for them, which causes "missing texture" log entries (bug [ITEM-08](index.md#suspected-bugs)).

## Bunker Door item (`ItemTGDoor`, `Techguns:doorItem01`)
- A `GenericItem` with unlocalized name `bunkerDoor` (lang `Bunker Door`), icon [`textures/items/metalDoor.png`](textures.md#11-tools-placers-door-bucket-debug-items), stack 64. The door block ([`TGBlocks.door01`](../07-blocks/block-catalog.md#registered-blocks)) is linked in [`TGItems.postInit`](item-catalog.md).
- Use on a block: works only when clicking the **top face**. The door base goes one block above the clicked block. The player must be allowed to edit both that position and the one above, and the door block's `canPlaceBlockAt` must succeed. The facing is `floor((yaw + 180) × 4 / 360 − 0.5) & 3`, the same as a vanilla door. Placement uses vanilla `ItemDoor.placeDoorBlock`, which also handles double-door hinge mirroring. One item is consumed (also in creative). The door block is in [07-blocks](../07-blocks/index.md).

## Camo Net Placer (`ItemCamoNetPlacer`, `Techguns:camoNetPlacer`)
A creative-friendly area-fill tool that places camo net blocks from the player's inventory.

- Stack 1. Meta = camo type: 0 Woodland, 1 Snow, 2 Desert. Icon per meta: [`textures/items/camoNetPlacer0.png`](textures.md#11-tools-placers-door-bucket-debug-items), `…1.png`, `…2.png` (meta ≥ 3 falls back to the meta-0 icon). The display name is the same for all metas: "Camo Net Placer".
- NBT on the stack: `x1`, `y1`, `z1` (int), the stored first corner. A tag compound is created on first use.
- Constants: 3 camo types, at most **64** blocks per operation.

| Input | Effect |
|---|---|
| Sneak + right-click (air or block) | Meta becomes `(meta + 1) % 3`. Client chat: `Placing with Camo: <name>` (`techguns.item.camoNetPlacer.info.msg` + `techguns.item.camoNetPlacer.info.camo.<n>`). |
| Right-click in air (not sneaking) | Clears `x1/y1/z1`. Chat: `Cleared Position 1`. |
| Right-click block, no corner stored | Stores the clicked block coords as corner 1. Chat: `Position 1 set to: (x/y/z).` |
| Right-click block, corner stored | Tries to fill, then always clears the corner (see below). |

Fill algorithm (second click at (x, y, z), corner (x1, y1, z1)):
1. Plane detection: if `x1 == x` the plane is YZ (mode 0); else if `z1 == z`, XY (mode 2); else if `y1 == y`, horizontal XZ (mode 1). If none matches, chat `Can only Place in a Plane`, clear the corner and stop.
2. Box volume = (|Δx|+1)·(|Δy|+1)·(|Δz|+1). If it is above 64: chat `Can only Place a maximum of 64 in a single use.`, clear and stop.
3. Block type: a horizontal plane (mode 1) uses **Camo Net Roof** (`TGBlocks.camoNetRoof`) with meta = camo. A vertical plane uses **Camo Net** (`TGBlocks.camoNetPane`) with meta = camo.
4. The player's main inventory must hold at least *volume* blocks of that exact block/meta. Occupied cells are counted too. If not: chat `Not enough camo net blocks in inventory.`, clear and stop.
5. For a horizontal plane where the second click was on a top face (side 1), the whole box moves up by 1, so the roof is placed on top of the clicked surface.
6. Every **air** cell in the box gets the block (server only, meta = camo, through [`MBlock.setBlock`](../11-worldgen/structure-system.md#mblock) with flags 0). The number of air cells is counted on both sides, and exactly that many items are taken from the main inventory.
7. Chat `Camo net placed.`; clear the corner.

All chat messages are client-side only. The item has no durability and is never consumed.

<a id="medkit-unregistered"></a>
## Medkit (`ItemMedkit`): present in code, **not registered**
If it were enabled as `new ItemMedkit("medkit", 2)` (the commented-out line), it would behave as follows:
- Stack 16, metas 0..1, icons [`textures/items/medkit_0.png`](textures.md#12-unused--unregistered) (bandage roll) and [`textures/items/medkit_1.png`](textures.md#12-unused--unregistered) (red medkit). Unlocalized name `techguns.item.medkit_<meta>`, giving "Bandages" and "Medkit".
- Right-click starts using it **only if the player is below max health**. Use animation: none.

| Meta | Use time (ticks) | Instant heal (HP) | Regeneration effect |
|---|---|---|---|
| 0 Bandages | 60 | 2 | Regeneration I for 300 ticks (ambient) |
| 1 Medkit | 50 | 6 | Regeneration II for 400 ticks (ambient) |

- While in use, every 7 ticks it plays vanilla `random.eat` (volume 1.0, pitch 1.2) and swings the arm.
- On completion (`onEaten`) it consumes 1, plays `random.burp` (volume 0.5, pitch 0.9–1.0), heals, and on the server applies the regeneration effect.

## Ore Cluster Scanner (`OreClusterScanner`, `Techguns:oreClusterScanner`)
- Stack 1, texture [`textures/items/oreClusterScanner.png`](textures.md#11-tools-placers-door-bucket-debug-items). Implements CoFH `IEnergyContainerItem` (RF).
- Energy is stored in NBT int `Energy`. Capacity 30 000 RF, max receive 3 000/t, max extract 3 000/t. The durability bar is always shown, with fullness = 1 − energy/capacity.
- The creative-tab copy and freshly crafted copies (`onCreated`) are set to full energy (30 000).
- Tooltip line: `<energy>/30000 RF` (not localized).
- Use: right-click starts an "in use" state (duration effectively infinite). After 40 ticks of holding (`SCANDURATION`), use stops automatically.
- On stopping, by release or by the 40-tick auto-stop: 300 RF is extracted, **whether or not** 40 ticks passed. If the extraction yielded the full 300 and at least 40 ticks passed, the scan runs **client-side only**:
  1. It scans the **chunk the player stands in**, every column, from `floor(playerY) + 16` (clamped to world height) down to y = 1.
  2. For every ore-cluster block (`TGBlocks.isOrecluster`) it spawns the client FX `OreClusterPing` at the block center ([04-projectiles-fx](../04-projectiles-fx/index.md)) and records the name `translate(<blockUnlocalized>_<meta>.name)`, once per type.
  3. Chat: if any were found, `Found Ore Clusters in this Chunk` (`techguns.OreClusterScanner.msg.found`) followed by one line per type. Otherwise `No Clusters in this Chunk` (`techguns.OreClusterScanner.msg.notFound`).
- Ore clusters themselves are [07](../07-blocks/index.md)/11.

<a id="tools"></a>
## Tools

Tool material `TG_STEEL` = `EnumHelper.addToolMaterial("Steel", 2, 1000, 7.5F, 2.5F, 12)`: harvest level 2 (iron), 1000 uses, efficiency 7.5, damage bonus 2.5, enchantability 12. No repair item is set.

### Combat Knife (`TGSword`, `Techguns:combatKnife`)
- A vanilla `ItemSword` with TG_STEEL: attack-damage modifier 4 + 2.5 = +6.5 (7.5 total with the player's base 1), 1000 durability, blocks like a sword. Texture [`textures/items/combatKnife.png`](textures.md#11-tools-placers-door-bucket-debug-items) (96×96, drawn flat like any item icon).
- Unlocalized `techguns.item.combatKnife`.
- Damage-system hook ([`IToolTGDamageSystem`](../02-guns/damage-system.md#3-interfaces)): when a living entity holding it attacks, Techguns' `LivingAttackEvent` handler swaps the damage source for a [`TGDamageSource`](../02-guns/damage-system.md#2-tgdamagesource-extends-vanilla-entitydamagesource) with damage type **PHYSICAL**, death type **GORE**, gore chance 0.25, hurt-resistance respected. The resistance math are in [01](../01-core/index.md)/05.
- Tooltip: `DamageType: <colored localized type>`. The prefix is hard-coded; PHYSICAL is shown in dark gray as `techguns.TGDamageType.PHYSICAL=Physical`. An `ArmorPen: <p×100>%` line would be added if penetration > 0, but the knife's penetration is 0.

### Crowbar (`TGCrowbar` → `TGPickaxe`, `Techguns:crowbar`)
- A vanilla `ItemPickaxe` with TG_STEEL: mines pickaxe blocks at harvest level 2, efficiency 7.5, 1000 uses, attack-damage modifier 2 + 2.5 = +4.5. Texture [`textures/items/crowbar.png`](textures.md#11-tools-placers-door-bucket-debug-items).
- `getToolClasses` returns `{"default", "pickaxe", "wrench"}`, each mapped to level 2 for the tooltip. Because it reports the class `wrench`:
  - Right-click on a Techguns basic machine (except metas 6, 7, 8: explosive charges and flag) rotates it. Sneak + right-click dismantles it into an item that keeps its tile data in NBT `TileEntityData` ([08](../08-machines-production/index.md)/[09](../09-machines-utility/index.md)).
  - Sneak + right-click on a Reinforced or Weapon Chest the player may use picks the chest up with its contents ([07](../07-blocks/index.md)).
  - `doesSneakBypassUse` is true, so sneak-clicks still reach the block.
- **Ore clusters**: while held, [`OreCluster.getPlayerRelativeBlockHardness`](../07-blocks/block-catalog.md#registered-blocks) returns 0.005 for non-bedrock cluster types, so a cluster breaks in about 200 ticks (10 s). Cluster blocks drop nothing ([07](../07-blocks/index.md)/[11](../11-worldgen/index.md)).
- Damage hook: same as the knife (PHYSICAL, GORE, gore chance 0.25).
- Tooltip:
  - Always: `DamageType: Physical` (hard-coded prefix).
  - With Shift held: `Toolclasses` (`techguns.tooltip.toolclasses`), then one line per class `  <class> : <level>`, then `Can destroy Ore Clusters` (`techguns.tooltip.crowbar.destroycluster`).
  - Without Shift: `Press [SHIFT] for detailed tooltip` (`techguns.gun.tooltip.shift1/2/3`, with `[SHIFT]` in green).

### `GenericTool` (unused)
An `ItemTool` subclass (stack 1, no repair, Techguns tab, texture `techguns:<name>`) that implements [`ICamoChangeable`](../05-armor/armor-catalog.md#1-class-hierarchy) with stub methods (0 camos) and writes NBT byte `camo = 0` in `onCreated`. Nothing instantiates it. Its `getMeleeDamageSource` (PHYSICAL, GORE, gore chance 0.5) is dead code.

<a id="debug-and-scanner-tools"></a>
## Debug and developer tools

All are stack 1, always registered and visible in the creative tab, and have no recipe.

### Block Reader Tool (`BlockReader`)
Right-click on a block. Client chat:
- `Block:<numeric id> - Meta: <meta> - Side: <side>`
- for a Techguns door: `Door: i1 = <full door meta> - j1 = <facing 0-3> - flag = <open bit>`
- `Blockbounds: X:<minX>,<maxX>|Y:<minY>,<maxY>|Z:<minZ>,<maxZ>` (the server prints the same bounds to stdout).

Texture [`textures/items/blockReaderTool.png`](textures.md#11-tools-placers-door-bucket-debug-items).

### Building Scan Tool (`BuildingScanTool`)
A two-click region scanner that writes a structure template for the [11-worldgen](../11-worldgen/index.md) structure system.
- NBT `x1/y1/z1` as with the camo placer. First click: `Position1 set (x/y/z).` Second click: `Position2 set (…)` and `Size: (sx/sy/sz).`, then the corner is cleared.
- Server side: iterate the box with x, then y, then z (outer to inner). Skip air, plain dirt meta 0, and grass. Every other block/meta pair gets a palette index (first-seen order). Write `structure_scan.txt` to the game's working directory:
  1. `ArrayList<MBlock> blockList = new ArrayList<MBlock>();` followed by one `blockList.add(new MBlock(Block.getBlockFromName("<unlocalizedName>"), <meta>));` line per palette entry (a code snippet to paste into Java; it uses the unlocalized name, not the registry name),
  2. `---`, a blank line,
  3. the number of positions,
  4. one line per position in [`BlockPosInd.toString()`](../11-worldgen/structure-system.md#blockposind) format (relative x, y, z, palette index; see [11](../11-worldgen/index.md)).
- Texture [`textures/items/structureScanTool.png`](textures.md#11-tools-placers-door-bucket-debug-items).

### Structure Test Tool (`StructureTestTool`)
- Tooltip: `Tool for structure spawning/world editing [WIP]` (`techguns.structureTestTool.Tooltip`).
- NBT: `mode` (int), `x1/y1/z1`.
- Sneak + use on block cycles the mode. If no `mode` key exists, the first cycle sets 1, not 0. Otherwise `(mode + 1) % 7`. Chat: `structure tool mode set to "<name>".` The names are `Structure, Camp, Smooth, Flatten, Remove Junk, Sphere, Cylinder`.
- Two clicks define a box (same messages as the scan tool). On the second click, the server runs the mode (0 if no tag):

| Mode | Label | Action (algorithms in [11-worldgen](../11-worldgen/index.md) [`BlockUtils`](../01-core/utilities.md#blockutils) / structures) |
|---|---|---|
| 0 | Structure | Builds [`AircraftCarrierNew`](../11-worldgen/structure-catalog.md#a7-aircraft-carrier-aircraftcarriernew) at the box min corner, size (sx, 24, sz), random rotation 0–3, WOODLAND color, and prints the origin to stdout |
| 1 | Camp | Actually runs `GenericCity.testCity.createCity` over the box (the label is stale) |
| 2 | Smooth | 2D heightmap Gaussian 5×5 filter over the XZ area |
| 3 | Flatten | `flattenArea(…, 2)` |
| 4 | Remove Junk | `removeJunkInArea` |
| 5 | Sphere | Radius r = (sx + sz)/4. Hollow-ish sphere centered at (minX + sx/2, minY + r + 1, minZ + sz/2), filled with the air-marker block and a glass shell |
| 6 | Cylinder | A cylinder of radius 3 from corner 1 to corner 2, with air-marker inside and a glass shell |

Texture [`textures/items/structureTestTool.png`](textures.md#11-tools-placers-door-bucket-debug-items).

### Ore Cluster Debugger (`oreClusterDebugger`)
- Tooltip: `WARNING: Removes all destroyable Blocks in a huge Radius, used to debug Ore cluster spawns` (`techguns.oreClusterDebugger.Tooltip`).
- Use on block (server): for every y from the clicked y down to 1, and x/z in `[click − 16, click + 16)` (a 32×32 column), it sets to air every block with hardness ≥ 0, i.e. everything except bedrock-like unbreakables.
- Texture [`textures/items/testItem.png`](textures.md#11-tools-placers-door-bucket-debug-items).

<a id="spawn-eggs"></a>
## Spawn eggs (`TGMonsterPlacer`)
A copy of the vanilla spawn egg with a fixed entity per item (the list is in the catalog §5).
- Two render passes: pass 0 = `techguns:spawn_egg` tinted with the base color, pass 1 = `techguns:spawn_egg_overlay` tinted with the spots color.
- Display name: `"Spawn " + entityName` (no localization).
- The creative tab lists only meta 0.
- Use on a block (server): spawn position is the clicked face offset, at (x+0.5, y, z+0.5), plus 0.5 in y if the top face of a fence-type block (render type 11) was clicked. The entity is looked up as `"Techguns." + entityName` in `EntityList`. It is placed with a random yaw, then `onSpawnWithEgg(null)` and `playLivingSound()` run. A named egg applies its custom name as the name tag. One egg is consumed unless in creative. If the name is not registered, it prints `Entity not found <name>`.
- Right-click aimed at a liquid: spawns at the liquid block's integer corner (x, y, z), with the same naming and consumption rules.
- NPC behavior belongs to [10-entities-npcs](../10-entities-npcs/index.md).

---

# Entity sheets (consistency pass)

The sections above describe behavior. The sheets below restate each tool, the door and the shared item in the field order of [CONVENTIONS.md](../CONVENTIONS.md), so each can be read on its own. Texture figures are quoted from [textures.md](textures.md), which quotes [ASSET-FACTS.md](../ASSET-FACTS.md). Where a field has nothing to say it says *none*.

## Sheet: Bunker Door item (`doorItem01`)

| # | Field | Contents |
|---|---|---|
| 1 | Identity | Class `ItemTGDoor` (extends `GenericItem`); registry name `Techguns:doorItem01`; unlocalized name set to `bunkerDoor` and then prefixed with `techguns.`, giving the key stem `techguns.item.bunkerDoor`; English "Bunker Door", lang key `techguns.item.bunkerDoor.name`; meta: none (single item); creative tab Techguns; OreDict: none; stack size 64; `TGItems` field `door01`. |
| 2 | Obtaining | Crafting R71 in [recipes.md](recipes.md#4-tools-wearables-and-stand-alone-items): `pp / pp / pp` with p = [`plateIron`](item-catalog.md#1-the-shared-meta-item-techgunstechgunsammo), yields 2 doors. A commented-out variant used tin plates. No loot, no NPC drop. |
| 3 | Numbers | Stack 64; no durability; no config key. The door block is `TGBlocks.door01` ([07-blocks](../07-blocks/index.md)), linked once in `TGItems.postInit` through `setDoorBlock`. |
| 4 | Behavior | Server and client both run `onItemUse`. (1) Only the top face (side 1) is accepted; any other face returns false. (2) The target is the block above the clicked one. (3) The player must be allowed to edit that position and the one above it. (4) The door block's own placement test (`canPlaceBlockAt`) must pass; if it fails nothing happens and the item is kept. (5) Facing = floor((yaw + 180) x 4 / 360 - 0.5) masked to 2 bits, like a vanilla door. (6) The vanilla door-placement helper writes both halves and picks the hinge side from neighboring doors. (7) The stack is reduced by 1, also in creative mode. Edge case: if `doorBlock` were never linked (`postInit` not run) the call would throw a null pointer; in a normal start-up it is always linked. |
| 5 | Persistence and sync | *none*: no NBT. Block placement is synchronized by vanilla block-change packets. |
| 6 | UI | No tooltip lines, no chat messages. |
| 7 | Visuals | Plain 2D icon `textures/items/metalDoor.png`, 32x32, static, average #6F756B, dominant #707070 (30 %), #909090 (16 %), #505050 (12 %), opaque 73 %. Gray-green bunker door in front view with rivets and a handle bar. No item renderer. |
| 8 | Audio | *none* from the item. The placed door block plays its own sounds ([07-blocks](../07-blocks/index.md)). |
| 9 | Relationships | Uses `plateIron` (Metal Press, 08). Places `door01` in 07. Read by the Block Reader Tool (it prints the door's open/facing bits). |
| 10 | Bugs | None specific; the door block's behavior is in 07. |

## Sheet: tool material `TG_STEEL`

Created once in `TGItems` with the Forge tool-material factory under the name "Steel".

| Property | Value |
|---|---|
| Harvest level | 2 (iron tier: stone, iron, gold, diamond, redstone and lapis ores are minable; obsidian needs 3) |
| Maximum uses (durability) | 1000 |
| Efficiency (mining speed) | 7.5 |
| Damage bonus vs entities | 2.5 |
| Enchantability | 12 |
| Repair material | none set, so an anvil cannot repair with an ingot; only anvil combining or the Repair Bench ([09-machines-utility](../09-machines-utility/index.md)) apply |

Common to both tools: stack size 1 (all vanilla tool classes), creative tab Techguns, no config keys, no NBT written by the tool classes.

## Sheet: Combat Knife (`combatKnife`)

| # | Field | Contents |
|---|---|---|
| 1 | Identity | Class `TGSword` (extends vanilla `ItemSword`, implements `IToolTGDamageSystem`); registry `Techguns:combatKnife`; unlocalized `techguns.item.combatKnife`; English "Combat Knife", lang key `techguns.item.combatKnife.name`; creative tab Techguns; OreDict none; `TGItems.combatKnife`. |
| 2 | Obtaining | Crafting R82: `.s / p.` with s = [`plateSteel`](item-catalog.md#1-the-shared-meta-item-techgunstechgunsammo), p = `sheetPlastic` ([recipes.md](recipes.md#4-tools-wearables-and-stand-alone-items)). No loot or drop. |
| 3 | Numbers | Material `TG_STEEL`: durability 1000, enchantability 12. Melee damage = 4 (sword base) + 2.5 = 6.5 attribute bonus, so 7.5 including the player's base 1. Damage type `PHYSICAL`; armor penetration field 0.0 (the second constructor that sets it is unused). Death type GORE, gore chance 0.25, hurt-resistance time respected. Each hit costs 1 durability, each block mined 2 (vanilla sword rules). |
| 4 | Behavior | Blocking pose on right-click, as a vanilla sword. When the holder deals melee damage, the Techguns living-attack handler asks the tool for a `TGDamageSource` (see [damage-system](../02-guns/damage-system.md#3-interfaces)) so armor and resistances use the PHYSICAL channel. No other interaction. |
| 5 | Persistence and sync | Item damage only; no NBT keys. |
| 6 | UI | Vanilla name line, then `DamageType: <colored Physical>` (prefix hard-coded English, [ITEM-16](index.md#suspected-bugs)). The `ArmorPen:` line is absent because penetration is 0. The game adds its own attack-damage attribute line. |
| 7 | Visuals | Icon `textures/items/combatKnife.png`, 96x96, static, average #2F3532, dominant #303030 (57 %), #505050 (13 %), #101010 (9 %), opaque 7 %. Dark gunmetal blade on the diagonal. Held with the vanilla sword transform (no custom renderer). |
| 8 | Audio | *none* custom; vanilla hit sounds. |
| 9 | Relationships | Uses `plateSteel` and `sheetPlastic`. Damage hook in [02-guns](../02-guns/index.md). |
| 10 | Bugs | ITEM-16 (hard-coded tooltip prefix). |

## Sheet: Crowbar (`crowbar`)

| # | Field | Contents |
|---|---|---|
| 1 | Identity | Class `TGCrowbar` (extends `TGPickaxe`, which extends vanilla `ItemPickaxe`); registry `Techguns:crowbar`; unlocalized `techguns.item.crowbar`; English "Crowbar", lang key `techguns.item.crowbar.name`; creative tab Techguns; OreDict none; `TGItems.crowbar`. |
| 2 | Obtaining | Crafting R87: `..p / .p. / p..` with p = `plateSteel`. No loot or drop. |
| 3 | Numbers | Material `TG_STEEL`: harvest level 2 for class `pickaxe`, durability 1000, efficiency 7.5, enchantability 12. Attack damage = 2 (pickaxe base) + 2.5 = +4.5. `getToolClasses` returns three classes (`default`, `pickaxe`, `wrench`), each valued 2 in the tooltip map, but Forge's per-class harvest level is set only for `pickaxe`, so `wrench` reports -1 ([ITEM-15](index.md#suspected-bugs)). Cluster-block mining speed: the block's relative hardness is overridden to 0.005 per tick while held, about 200 ticks. |
| 4 | Behavior | As a pickaxe: fast on stone and iron-material blocks. As a wrench (Techguns' own machines check the tool class): right-click rotates a basic machine; sneak + right-click dismantles it to an item keeping NBT `TileEntityData`; sneak + right-click picks up a chest with its contents ([09-machines-utility](../09-machines-utility/index.md), [07-blocks](../07-blocks/index.md)). `doesSneakBypassUse` is true, so sneaking still interacts with the block. Breaks ore clusters. Damage hook: PHYSICAL, GORE, gore chance 0.25. |
| 5 | Persistence and sync | Item damage only; no NBT keys on the tool (`TileEntityData` is written on the dismantled machine item, not the crowbar). |
| 6 | UI | Always: `DamageType: <Physical>`. Shift held: `Toolclasses` (`techguns.tooltip.toolclasses`), then lines `  default : 2`, `  pickaxe : 2`, `  wrench : 2` (hash-map order, not fixed), then `Can destroy Ore Clusters` (`techguns.tooltip.crowbar.destroycluster`). Shift not held: "Press [SHIFT] for detailed tooltip" (`techguns.gun.tooltip.shift1`, `.shift2` in green, `.shift3`). |
| 7 | Visuals | Icon `textures/items/crowbar.png`, 32x32, static, average #713B3B, dominant #703030 (25 %), #503030 (24 %), #905050 (15 %), opaque 10 %. Thin maroon crowbar with a hook at the top right. Vanilla pickaxe-style held transform. |
| 8 | Audio | *none* custom. |
| 9 | Relationships | Uses `plateSteel`. Interacts with [`BasicMachine`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) ([08](../08-machines-production/index.md)/[09](../09-machines-utility/index.md)), chests ([07](../07-blocks/index.md)) and ore clusters ([07](../07-blocks/index.md), [11](../11-worldgen/index.md)). |
| 10 | Bugs | ITEM-15, ITEM-16. |

## Sheet: `GenericTool` (unused base class)

| # | Field | Contents |
|---|---|---|
| 1 | Identity | Class `GenericTool` extends vanilla `ItemTool`, implements `ICamoChangeable`. Not registered anywhere: no registry name, no lang key, no creative-tab entry. The constructor takes a damage value, a tool material, the effective-block set and a name; it sets unlocalized name = name and texture `techguns:<name>`. |
| 2 | Obtaining | *unobtainable*: nothing instantiates it. |
| 3 | Numbers | Stack 1; `setNoRepair` (no anvil repair); max damage comes from the material passed in; `getCamoCount` returns 0. |
| 4 | Behavior | `onCreated` writes NBT byte `camo = 0` if the stack has no tag. The camo-switch methods are stubs returning 0 or null. `getMeleeDamageSource` builds a PHYSICAL/GORE source with gore chance 0.5, but nothing calls it (dead code). |
| 5 | Persistence and sync | NBT `camo` (byte), written only by `onCreated`. |
| 6 | UI | *none*. |
| 7 | Visuals | *none* (no texture exists for any name). |
| 8 | Audio | *none*. |
| 9 | Relationships | Implements the camo interface used by armor and guns ([05-armor](../05-armor/armor-catalog.md#1-class-hierarchy)). |
| 10 | Bugs | None numbered; dead code. |

## Sheet: `TGPickaxe` and `TGSword` base classes

Both are plain wrappers. They set the creative tab, the unlocalized name and the texture (`techguns:<name>`), prefix the unlocalized name with `techguns.`, and implement the damage-source hook (PHYSICAL type, GORE death, gore chance 0.25, hurt resistance respected). A second constructor accepting a damage type and a penetration exists but is unused. Their tooltip adds `DamageType: ` plus the damage type's own colored, localized text and, if penetration is above zero, `ArmorPen: <percent>%`. No NBT, no sounds, no config.

## Sheet: debug and developer tools

Common fields for all four tools:

| Field | Value |
|---|---|
| Identity | Extend `GenericItem` (so `techguns.` prefix, creative tab Techguns); stack 1 via `setMaxStackSize(1)`. Registered unconditionally in [`Techguns.java`](../01-core/lifecycle-and-registration.md#lifecycle-overview) (see [item-catalog.md](item-catalog.md#4-debug--developer-items-registered-unconditionally-in-techgunsjava)). |
| Obtaining | Creative tab only; *no recipe*, no loot. |
| Numbers | No durability, no config key. |
| Audio | *none*. |
| Bugs | [ITEM-13](index.md#suspected-bugs) (destructive tools exposed), [ITEM-14](index.md#suspected-bugs) (structure tool labels). |

Per tool:

| Tool | Registry / lang key | Behavior summary | Persistence | UI strings | Texture |
|---|---|---|---|---|---|
| Block Reader Tool | [`blockReaderTool`](item-catalog.md#4-debug--developer-items-registered-unconditionally-in-techgunsjava), `techguns.item.blockReaderTool.name` | `onItemUse` returns true on both sides. Client chat: `Block:<id> - Meta: <meta> - Side: <side>`; for a Techguns door also `Door: i1 = <n> - j1 = <0-3> - flag = <bool>` (i1 is the door's packed state, j1 its low 2 bits, flag bit 4); client chat `Blockbounds: X:<min>,<max>\|Y:...\|Z:...`. The server prints `Server Bounds:Blockbounds: ...` to the console. | none | the strings listed, all hard-coded English | `blockReaderTool.png` 32x32, avg #5E5E5F, opaque 62 % |
| Building Scan Tool | [`buildingScanTool`](item-catalog.md#4-debug--developer-items-registered-unconditionally-in-techgunsjava), `techguns.item.buildingScanTool.name` | Two-click region scan; writes `structure_scan.txt` on the server (palette code plus position list; details above). | NBT ints `x1`, `y1`, `z1` for the first corner, cleared after the second click | `Position1 set (x/y/z).`, `Position2 set (x/y/z).`, `Size: (sx/sy/sz).` (client chat, hard-coded) | `structureScanTool.png` 32x32, avg #5A4C4C, opaque 50 % |
| Structure Test Tool | [`structureTestTool`](item-catalog.md#4-debug--developer-items-registered-unconditionally-in-techgunsjava), `techguns.item.structureTestTool.name` | Seven-mode world editor (table above). | NBT int `mode`, ints `x1`, `y1`, `z1` | tooltip `techguns.structureTestTool.Tooltip` = "Tool for structure spawning/world editing [WIP]"; chat `structure tool mode set to "<name>".`, `Position2 set`, `Size` | `structureTestTool.png` 32x32, avg #534444, opaque 62 % |
| Ore Cluster Debugger | `oreClusterDebugger`, `techguns.item.oreClusterDebugger.name` | Server only: clears the 32x32 column below the clicked block down to y=1 (everything with non-negative hardness). | none | tooltip `techguns.oreClusterDebugger.Tooltip` = "WARNING: Removes all destroyable Blocks in a huge Radius, used to debug Ore cluster spawns" | `testItem.png` 32x32, avg #000000, dominant #101010 (100 %), opaque 28 %; the same placeholder icon as the `GenericItem` default |

## Sheet: Ore Cluster Scanner, Camo Net Placer and spawn eggs (recap)

| Item | Numbers / persistence recap | Obtaining | Lang |
|---|---|---|---|
| Ore Cluster Scanner | Capacity 30 000 RF; receive and extract 3 000/t; scan costs 300 RF; hold 40 ticks; NBT int `Energy`; scan range one chunk, y from player+16 down to 1; the durability bar shows missing energy | Crafting R83 (output starts full via `onCreated`) | `techguns.item.oreClusterScanner.name`; chat `techguns.OreClusterScanner.msg.found` and `.notFound`; tooltip `<energy>/30000 RF` is hard-coded (ITEM-16) |
| Camo Net Placer | Stack 1; metas 0-2; NBT ints `x1`, `y1`, `z1`; at most 64 blocks; takes blocks from the main inventory | Crafting R84 to R86 | `techguns.item.camoNetPlacer.name` and the message keys `techguns.item.camoNetPlacer.info.*` |
| Spawn eggs | One item per entity (18); colors in [item-catalog.md](item-catalog.md#5-spawn-eggs-tgmonsterplacer-registered-by-tgentities); no NBT | Creative tab only | none; the name is hard-coded ([ITEM-12](index.md#suspected-bugs)) |

## Sheet: shared item behavior (`GenericItemShared`, `Techguns:TechgunsAmmo`)

| # | Field | Contents |
|---|---|---|
| 1 | Identity | Class `GenericItemShared` (extends `GenericItem`, implements `ITGSpecialSlot`); registry `Techguns:TechgunsAmmo`; `TGItems.sharedItem`; 110 metas (0-109) named by `names[]`; unlocalized `techguns.item.<internalName>`; lang `techguns.item.<internalName>.name`; creative tab Techguns lists every meta in order; OreDict names per meta are in [item-catalog.md](item-catalog.md#1-the-shared-meta-item-techgunstechgunsammo); `hasSubtypes` is true. |
| 2 | Obtaining | Per meta in [item-catalog.md](item-catalog.md#1b-lang-key-and-obtained-from-for-every-shared-meta-consistency-pass). |
| 3 | Numbers | Per-meta max stack (64 default; 1 for turret armor, glider, oxygen mask, drill heads and reaction-chamber foci; 7 for the stack upgrade); per-meta slot type; drill-head bonus level 1/2/3; no durability, no config key. |
| 4 | Behavior | Right-click ladder: sneak on a full magazine empties one (refund table above); otherwise ammo-slot types merge into the Techguns ammo slots 6-11; a back-slot item (Glider) goes into the back slot if empty; otherwise nothing. Back-slot tick for the Glider (formula above). Dispenser: rockets, nuke rockets and 40 mm grenades are launched as projectiles. Out-of-range meta handling is inconsistent (ITEM-05). |
| 5 | Persistence and sync | *none*: the shared item stores no NBT of its own; the meta value is the whole state. The Glider's `isGliding` flag lives in the player's extended properties ([01-core](../01-core/index.md)). |
| 6 | UI | Magazine tooltip (metas 2, 5, 12, 17, 20, 23, 46): `Right click to put in ammo inventory` (`techguns.item.ammotooltip.ammobag`), `Sneak + Right click to empty` (`techguns.item.ammotooltip.empty`). Drill heads (75-83): `Valid for drills with size <size>` and `Bonus mining level: <n>` (`techguns.oreDrillTooltip.size`, `techguns.oreDrillTooltip.mininglevel`). No chat messages. |
| 7 | Visuals | 2D icons from `textures/items/<internalName>.png` for 102 metas; 3D model renderer `RenderGenericItem` for the eight metas 12, 13, 14, 17, 18, 20, 21, 105 (table above); an out-of-range meta falls back to the meta-0 icon in the icon lookup. |
| 8 | Audio | *none* from the shared item. The Glider's loop sound is played by the client player tick ([sounds.md](sounds.md)). Dispensed projectiles use the vanilla dispenser sound. |
| 9 | Relationships | Consumed by guns ([02-guns](../02-guns/index.md)), machines ([08](../08-machines-production/index.md)), turrets, armor recipes ([05](../05-armor/index.md)), the player ammo inventory ([01](../01-core/index.md), [05](../05-armor/index.md)). |
| 10 | Bugs | ITEM-05, ITEM-06, ITEM-07, ITEM-08, [ITEM-20](index.md#suspected-bugs). |
