# Item behaviour: shared items, placers, scanners, tools and debug items

## Base classes

| Class | Role |
|---|---|
| `GenericItem` | A plain `Item` with stack 64 and the Techguns creative tab. The no-arg constructor uses unlocalized name `testItem` and texture `techguns:testItem`. The `(name)` constructor sets unlocalized name = `name` and texture = `techguns:<name>`. Both `getUnlocalizedName` overloads put `techguns.` in front of the vanilla result, so the key becomes `techguns.item.<name>`. |
| `GenericItemShared` | The single meta item `TechgunsAmmo` (110 metas). It implements `ITGSpecialSlot` so its metas can sit in the Techguns face/back/ammo slots (the player inventory itself is 05/01). |
| `GenericItemBucket` | `ItemBucket` with the `techguns.` prefix (see [fluids.md](fluids.md)). |

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
| TURRETARMOR | "Turret armor" | turret armour plates |
| REACTION_CHAMBER_FOCUS | "Reaction Chamber Focus" | UV Emitter, Heat Ray focus |
| DRILL_SMALL / DRILL_MEDIUM / DRILL_LARGE | "Small Drill" / "Medium Drill" / "Large Drill" | ore drill heads |

## Shared item (`GenericItemShared`) behaviour

### Stack size and bad metadata
- `getItemStackLimit` returns the per-meta value from the catalogue. If the meta is ≥ 110, it prints `Techguns GenericItem has bad metadata:<m> Max is:109` and returns 64.
- `getIconFromDamage` falls back to meta 0's icon for an out-of-range meta.
- `getUnlocalizedName` and the right-click handler do **not** range-check (bug ITEM-05).

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

   This runs on both client and server. Anything that does not fit anywhere is silently lost (bug ITEM-06). For the duplication in the helper, see ITEM-07.
2. **Not sneaking, item's slot type is AMMOSLOT**: the whole held stack is merged into the Techguns ammo slots (6–11). It first tops up matching stacks, then fills empty slots. The held stack shrinks to whatever did not fit.
3. **Item's slot type is BACKSLOT (the Glider)**: if the Techguns back slot is empty, a copy goes there and the held stack becomes 0. Otherwise nothing happens.
4. Otherwise: vanilla behaviour (nothing).

Sneak + right-click on a non-magazine ammo item does nothing (branch 1 matches only full mags, and branch 2 is skipped while sneaking).

### Tooltips (`addInformation`)
- Ore drill heads (metas 75–83) get two lines:
  - `"<techguns.oreDrillTooltip.size> <size>"`, where size is `1x1x1` (small), `1x1xN or 3x3xN` (medium) or `5x5xN or 7x7xN` (large). English: `Valid for drills with size 1x1x1`.
  - `"<techguns.oreDrillTooltip.mininglevel>: <n>"`, where n = 1 for Steel, 2 for Obsidian Steel, 3 for Carbon. English: `Bonus mining level: 2`.
- Metas registered with `addMagazineTooltip` (2, 5, 12, 17, 20, 23, 46) get two lines: `Right click to put in ammo inventory` (`techguns.item.ammotooltip.ammobag`) and `Sneak + Right click to empty` (`techguns.item.ammotooltip.empty`).

### Armour bonus
`getBonus(OXYGEN_GEAR)` returns 1.0 for the Oxygen Mask (meta 90) and 0 for everything else. The mask counts as oxygen gear while it is in the Techguns face slot. Combined with Oxygen Tanks it refills air underwater. That logic, and the worn-model rendering of the mask, belong to 05-armor.

### Glider (meta 51), back-slot tick
Every player tick (phase START, on both client and server), the Techguns tick handler first clears `isGliding` and then calls the back-slot item's `onPlayerTick`. For the glider:

```
if player is airborne AND sneaking:
    if motionY < -0.1:
        isGliding = true
        dir = horizontal look vector (x, z), normalised
        lift = min(0.08, -0.1 - motionY)        # brakes the fall towards -0.1 blocks/tick
        motionY += lift
        motionX += dir.x * lift                  # converts the braking into forward speed
        motionZ += dir.z * lift
        jumpMovementFactor += 0.03               # extra air control
    fallDistance = 0                             # no fall damage while sneaking in the air
```

The terminal sink rate is therefore about 0.1 blocks/tick while sneaking, with forward glide along the look direction. A comment says the algorithm was borrowed from Modular Powersuits' glider.

Client side, `handlePlayerGliding`: when `isGliding` becomes true and no loop is playing, it starts the looping sound `techguns:glider.loop` attached to the player (volume 1, pitch 1, repeat, category PLAYER_EFFECT). When gliding stops, the sound is stopped. `isGliding` is also cleared when another player stops being tracked.

The worn glider model (`textures/armor/glider.png`, `ModelGlider`) is drawn by the additional-slot renderer (05-armor). The Jetpack also reuses this glider logic (05).

<a id="3d-rendered-shared-items"></a>
### 3D-rendered shared items (`RenderGenericItem`)
The client registers `RenderGenericItem` as the `IItemRenderer` for the whole shared item. It claims only these metas, for every render type (inventory, entity, equipped, first person) and with every render helper. All other metas use the normal 2D icon.

| Meta | Model | Texture |
|---|---|---|
| 12 Assault Rifle Magazine | `ModelM4Mag` | `textures/guns/m4Texture.png` |
| 13 AR Magazine (Empty) | `ModelM4MagE` | `textures/guns/m4Texture.png` |
| 14 Rocket | `ModelRocket` | `textures/guns/rocketlauncher.png` |
| 17 AS50 Magazine | `ModelAS50Mag(empty=false)` | `textures/guns/as50Texture.png` |
| 18 AS50 Magazine (Empty) | `ModelAS50Mag(empty=true)` | `textures/guns/as50Texture.png` |
| 20 LMG Magazine | `ModelLmgMag` | `textures/guns/mg2_texture.png` |
| 21 LMG Magazine (Empty) | `ModelLmgMagE` | `textures/guns/mg2_texture.png` |
| 105 Rocket (Tactical Nuke) | `ModelRocket` | `textures/guns/nukerocketlauncher.png` |

So the mags share the gun model's texture atlas, and the nuke rocket is the normal rocket model with the nuke launcher's texture. The models and textures are documented in 03-gun-visuals-audio. Each sub-renderer binds its texture and draws the model at scale 1/16 after a per-context transform:

| Renderer | First person | Equipped (3rd person) | Entity (dropped) | Inventory |
|---|---|---|---|---|
| M4 mag | rot X 180°, rot Y 225°, translate (−0.3, −1.5, 0.6) | rot X 180°, rot Y 135°, scale 0.8, translate (0.1, −1.3, 1.35) | rot X 180°, scale 0.65, translate (0, −0.65, 0) | rot X 180°, scale 2, translate (0, −0.45, 0.1) |
| AS50 mag | as M4 but translate (−0.3, −1.2, 0.6) | as M4 but translate (0.1, −1.15, 1.45) | rot X 180°, scale 0.8, translate (0, −0.35, 0.2) | rot X 180°, scale 1.75, translate (0, −0.2, 0.4) |
| LMG mag | as M4 | as M4 | rot X 180°, rot Y 90°, translate (0, −0.45, 0), scale 0.65 | rot X 180°, scale 1.35, translate (0, −0.45, 0.1) |
| Rocket | translate (1, 0, 0.5), rot Z 90°, scale 1.2 | rot X 180°, rot Y 225°, scale 1.5, translate (−0.5, −0.25, 0) | rot X 180°, rot Y 90° | rot X 180°, scale 1.45, translate (0, 0, −0.025) |

The constructors also take a scale and an xyz offset (all passed as 1.0 / 0,0,0), but these are never applied.

`GenericItemShared.registerIcons` still asks for `techguns:<name>` icons for these eight metas. No PNG exists for them, which causes "missing texture" log entries (bug ITEM-08).

## Bunker Door item (`ItemTGDoor`, `Techguns:doorItem01`)
- A `GenericItem` with unlocalized name `bunkerDoor` (lang `Bunker Door`), icon `textures/items/metalDoor.png`, stack 64. The door block (`TGBlocks.door01`) is linked in `TGItems.postInit`.
- Use on a block: works only when clicking the **top face**. The door base goes one block above the clicked block. The player must be allowed to edit both that position and the one above, and the door block's `canPlaceBlockAt` must succeed. The facing is `floor((yaw + 180) × 4 / 360 − 0.5) & 3`, the same as a vanilla door. Placement uses vanilla `ItemDoor.placeDoorBlock`, which also handles double-door hinge mirroring. One item is consumed (also in creative). The door block is in 07-blocks.

## Camo Net Placer (`ItemCamoNetPlacer`, `Techguns:camoNetPlacer`)
A creative-friendly area-fill tool that places camo net blocks from the player's inventory.

- Stack 1. Meta = camo type: 0 Woodland, 1 Snow, 2 Desert. Icon per meta: `textures/items/camoNetPlacer0.png`, `…1.png`, `…2.png` (meta ≥ 3 falls back to the meta-0 icon). The display name is the same for all metas: "Camo Net Placer".
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
6. Every **air** cell in the box gets the block (server only, meta = camo, through `MBlock.setBlock` with flags 0). The number of air cells is counted on both sides, and exactly that many items are taken from the main inventory.
7. Chat `Camo net placed.`; clear the corner.

All chat messages are client-side only. The item has no durability and is never consumed.

<a id="medkit-unregistered"></a>
## Medkit (`ItemMedkit`): present in code, **not registered**
If it were enabled as `new ItemMedkit("medkit", 2)` (the commented-out line), it would behave as follows:
- Stack 16, metas 0..1, icons `textures/items/medkit_0.png` (bandage roll) and `textures/items/medkit_1.png` (red medkit). Unlocalized name `techguns.item.medkit_<meta>`, giving "Bandages" and "Medkit".
- Right-click starts using it **only if the player is below max health**. Use animation: none.

| Meta | Use time (ticks) | Instant heal (HP) | Regeneration effect |
|---|---|---|---|
| 0 Bandages | 60 | 2 | Regeneration I for 300 ticks (ambient) |
| 1 Medkit | 50 | 6 | Regeneration II for 400 ticks (ambient) |

- While in use, every 7 ticks it plays vanilla `random.eat` (volume 1.0, pitch 1.2) and swings the arm.
- On completion (`onEaten`) it consumes 1, plays `random.burp` (volume 0.5, pitch 0.9–1.0), heals, and on the server applies the regeneration effect.

## Ore Cluster Scanner (`OreClusterScanner`, `Techguns:oreClusterScanner`)
- Stack 1, texture `textures/items/oreClusterScanner.png`. Implements CoFH `IEnergyContainerItem` (RF).
- Energy is stored in NBT int `Energy`. Capacity 30 000 RF, max receive 3 000/t, max extract 3 000/t. The durability bar is always shown, with fullness = 1 − energy/capacity.
- The creative-tab copy and freshly crafted copies (`onCreated`) are set to full energy (30 000).
- Tooltip line: `<energy>/30000 RF` (not localized).
- Use: right-click starts an "in use" state (duration effectively infinite). After 40 ticks of holding (`SCANDURATION`), use stops automatically.
- On stopping, by release or by the 40-tick auto-stop: 300 RF is extracted, **whether or not** 40 ticks passed. If the extraction yielded the full 300 and at least 40 ticks passed, the scan runs **client-side only**:
  1. It scans the **chunk the player stands in**, every column, from `floor(playerY) + 16` (clamped to world height) down to y = 1.
  2. For every ore-cluster block (`TGBlocks.isOrecluster`) it spawns the client FX `OreClusterPing` at the block centre (04-projectiles-fx) and records the name `translate(<blockUnlocalized>_<meta>.name)`, once per type.
  3. Chat: if any were found, `Found Ore Clusters in this Chunk` (`techguns.OreClusterScanner.msg.found`) followed by one line per type. Otherwise `No Clusters in this Chunk` (`techguns.OreClusterScanner.msg.notFound`).
- Ore clusters themselves are 07/11.

<a id="tools"></a>
## Tools

Tool material `TG_STEEL` = `EnumHelper.addToolMaterial("Steel", 2, 1000, 7.5F, 2.5F, 12)`: harvest level 2 (iron), 1000 uses, efficiency 7.5, damage bonus 2.5, enchantability 12. No repair item is set.

### Combat Knife (`TGSword`, `Techguns:combatKnife`)
- A vanilla `ItemSword` with TG_STEEL: attack-damage modifier 4 + 2.5 = +6.5 (7.5 total with the player's base 1), 1000 durability, blocks like a sword. Texture `textures/items/combatKnife.png` (96×96, drawn flat like any item icon).
- Unlocalized `techguns.item.combatKnife`.
- Damage-system hook (`IToolTGDamageSystem`): when a living entity holding it attacks, Techguns' `LivingAttackEvent` handler swaps the damage source for a `TGDamageSource` with damage type **PHYSICAL**, death type **GORE**, gore chance 0.25, hurt-resistance respected. The resistance maths are in 01/05.
- Tooltip: `DamageType: <coloured localized type>`. The prefix is hard-coded; PHYSICAL is shown in dark grey as `techguns.TGDamageType.PHYSICAL=Physical`. An `ArmorPen: <p×100>%` line would be added if penetration > 0, but the knife's penetration is 0.

### Crowbar (`TGCrowbar` → `TGPickaxe`, `Techguns:crowbar`)
- A vanilla `ItemPickaxe` with TG_STEEL: mines pickaxe blocks at harvest level 2, efficiency 7.5, 1000 uses, attack-damage modifier 2 + 2.5 = +4.5. Texture `textures/items/crowbar.png`.
- `getToolClasses` returns `{"default", "pickaxe", "wrench"}`, each mapped to level 2 for the tooltip. Because it reports the class `wrench`:
  - Right-click on a Techguns basic machine (except metas 6, 7, 8: explosive charges and flag) rotates it. Sneak + right-click dismantles it into an item that keeps its tile data in NBT `TileEntityData` (08/09).
  - Sneak + right-click on a Reinforced or Weapon Chest the player may use picks the chest up with its contents (07).
  - `doesSneakBypassUse` is true, so sneak-clicks still reach the block.
- **Ore clusters**: while held, `OreCluster.getPlayerRelativeBlockHardness` returns 0.005 for non-bedrock cluster types, so a cluster breaks in about 200 ticks (10 s). Cluster blocks drop nothing (07/11).
- Damage hook: same as the knife (PHYSICAL, GORE, gore chance 0.25).
- Tooltip:
  - Always: `DamageType: Physical` (hard-coded prefix).
  - With Shift held: `Toolclasses` (`techguns.tooltip.toolclasses`), then one line per class `  <class> : <level>`, then `Can destroy Ore Clusters` (`techguns.tooltip.crowbar.destroycluster`).
  - Without Shift: `Press [SHIFT] for detailed tooltip` (`techguns.gun.tooltip.shift1/2/3`, with `[SHIFT]` in green).

### `GenericTool` (unused)
An `ItemTool` subclass (stack 1, no repair, Techguns tab, texture `techguns:<name>`) that implements `ICamoChangeable` with stub methods (0 camos) and writes NBT byte `camo = 0` in `onCreated`. Nothing instantiates it. Its `getMeleeDamageSource` (PHYSICAL, GORE, gore chance 0.5) is dead code.

<a id="debug-and-scanner-tools"></a>
## Debug and developer tools

All are stack 1, always registered and visible in the creative tab, and have no recipe.

### Block Reader Tool (`BlockReader`)
Right-click on a block. Client chat:
- `Block:<numeric id> - Meta: <meta> - Side: <side>`
- for a Techguns door: `Door: i1 = <full door meta> - j1 = <facing 0-3> - flag = <open bit>`
- `Blockbounds: X:<minX>,<maxX>|Y:<minY>,<maxY>|Z:<minZ>,<maxZ>` (the server prints the same bounds to stdout).

Texture `textures/items/blockReaderTool.png`.

### Building Scan Tool (`BuildingScanTool`)
A two-click region scanner that writes a structure template for the 11-worldgen structure system.
- NBT `x1/y1/z1` as with the camo placer. First click: `Position1 set (x/y/z).` Second click: `Position2 set (…)` and `Size: (sx/sy/sz).`, then the corner is cleared.
- Server side: iterate the box with x, then y, then z (outer to inner). Skip air, plain dirt meta 0, and grass. Every other block/meta pair gets a palette index (first-seen order). Write `structure_scan.txt` to the game's working directory:
  1. `ArrayList<MBlock> blockList = new ArrayList<MBlock>();` followed by one `blockList.add(new MBlock(Block.getBlockFromName("<unlocalizedName>"), <meta>));` line per palette entry (a code snippet to paste into Java; it uses the unlocalized name, not the registry name),
  2. `---`, a blank line,
  3. the number of positions,
  4. one line per position in `BlockPosInd.toString()` format (relative x, y, z, palette index; see 11).
- Texture `textures/items/structureScanTool.png`.

### Structure Test Tool (`StructureTestTool`)
- Tooltip: `Tool for structure spawning/world editing [WIP]` (`techguns.structureTestTool.Tooltip`).
- NBT: `mode` (int), `x1/y1/z1`.
- Sneak + use on block cycles the mode. If no `mode` key exists, the first cycle sets 1, not 0. Otherwise `(mode + 1) % 7`. Chat: `structure tool mode set to "<name>".` The names are `Structure, Camp, Smooth, Flatten, Remove Junk, Sphere, Cylinder`.
- Two clicks define a box (same messages as the scan tool). On the second click, the server runs the mode (0 if no tag):

| Mode | Label | Action (algorithms in 11-worldgen `BlockUtils` / structures) |
|---|---|---|
| 0 | Structure | Builds `AircraftCarrierNew` at the box min corner, size (sx, 24, sz), random rotation 0–3, WOODLAND colour, and prints the origin to stdout |
| 1 | Camp | Actually runs `GenericCity.testCity.createCity` over the box (the label is stale) |
| 2 | Smooth | 2D heightmap Gaussian 5×5 filter over the XZ area |
| 3 | Flatten | `flattenArea(…, 2)` |
| 4 | Remove Junk | `removeJunkInArea` |
| 5 | Sphere | Radius r = (sx + sz)/4. Hollow-ish sphere centred at (minX + sx/2, minY + r + 1, minZ + sz/2), filled with the air-marker block and a glass shell |
| 6 | Cylinder | A cylinder of radius 3 from corner 1 to corner 2, with air-marker inside and a glass shell |

Texture `textures/items/structureTestTool.png`.

### Ore Cluster Debugger (`oreClusterDebugger`)
- Tooltip: `WARNING: Removes all destroyable Blocks in a huge Radius, used to debug Ore cluster spawns` (`techguns.oreClusterDebugger.Tooltip`).
- Use on block (server): for every y from the clicked y down to 1, and x/z in `[click − 16, click + 16)` (a 32×32 column), it sets to air every block with hardness ≥ 0, i.e. everything except bedrock-like unbreakables.
- Texture `textures/items/testItem.png`.

<a id="spawn-eggs"></a>
## Spawn eggs (`TGMonsterPlacer`)
A copy of the vanilla spawn egg with a fixed entity per item (the list is in the catalogue §5).
- Two render passes: pass 0 = `techguns:spawn_egg` tinted with the base colour, pass 1 = `techguns:spawn_egg_overlay` tinted with the spots colour.
- Display name: `"Spawn " + entityName` (no localization).
- The creative tab lists only meta 0.
- Use on a block (server): spawn position is the clicked face offset, at (x+0.5, y, z+0.5), plus 0.5 in y if the top face of a fence-type block (render type 11) was clicked. The entity is looked up as `"Techguns." + entityName` in `EntityList`. It is placed with a random yaw, then `onSpawnWithEgg(null)` and `playLivingSound()` run. A named egg applies its custom name as the name tag. One egg is consumed unless in creative. If the name is not registered, it prints `Entity not found <name>`.
- Right-click aimed at a liquid: spawns at the liquid block's integer corner (x, y, z), with the same naming and consumption rules.
- NPC behaviour belongs to 10-entities-npcs.
