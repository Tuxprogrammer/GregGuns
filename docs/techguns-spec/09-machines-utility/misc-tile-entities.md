# Storage and miscellaneous tile entities

This file covers the Reinforced and Weapon chests, the Camp Flag, and the Bio Blob.

---

## 1. Reinforced Chest and Weapon Chest

| | Reinforced Chest | Weapon Chest |
|---|---|---|
| Block | `BlockTGChest(type 0)`, registry [`tgchest`](../07-blocks/block-catalogue.md#registered-blocks) | `BlockTGChest(type 1)`, registry [`tgchest_weapon`](../07-blocks/block-catalogue.md#registered-blocks) |
| Item block | [`ItemBlockTGChest`](../07-blocks/block-catalogue.md#conventions-and-defaults) | `ItemBlockTGChest` |
| Item name | `techguns.tile.tgchest.name` = "Reinforced Chest" | `techguns.tile.tgchest_weapon.name` = "Weapon Chest" |
| Container title, single | `techguns.container.tgchest` = "Reinforced Chest" | `techguns.container.tgchest_weapon` = "Weapon Chest" |
| Container title, double | `techguns.container.tgchest.double` = "Large Reinforced Chest" | `techguns.container.tgchest_weapon.double` = "Large Weapon Chest" |
| Recipe | `n n` / ` c ` / `n n`; n = [`nuggetIron`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems), c = vanilla chest | `pcp`; p = [`plateIron`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo), c = vanilla chest |
| World textures | [`textures/blocks/reinforced_chest.png`](../07-blocks/textures.md#11-model-textures), [`textures/blocks/reinforced_chest_double.png`](../07-blocks/textures.md#11-model-textures) | [`textures/blocks/weapon_chest.png`](../07-blocks/textures.md#11-model-textures), [`textures/blocks/weapon_chest_double.png`](../07-blocks/textures.md#11-model-textures) |

Both share tile entity `TGChestTileEnt` (registered `TGChestTileEnt`), GUI id 20, container `TGChestContainer`, GUI `GuiTechgunsChest` and TESR `RenderTGChest`.

### Block (`BlockTGChest`, adapted from the vanilla chest)

- Material wood, **hardness 5**, **resistance 500** (blast-proof), wood step sound, Techguns creative tab.
- Bounds are the vanilla chest's: 1/16 inset, 14/16 tall. A half that joins a neighbour extends to that side.
- Render type −1 (TESR). The particle and break icon is vanilla `planks_oak`.
- **Placement and double chests:**
  - Vanilla logic. A chest joins **one** horizontally adjacent chest **of the same block**; the two types never merge.
  - You cannot place a chest next to two chests, or next to a chest that is already double.
  - Facing (metadata 2–5) comes from the player's yaw. The halves of a double chest are aligned.
  - On placement (server), if the item carries `TileEntityData`, the stored `Items` and `CustomName` are loaded into the new tile.
- **Opening:** right-click, if the player passes the usable check (same tile, within 8 blocks). It opens GUI 20 with the vanilla combined-inventory lookup. The chest **cannot open** when:
  - a solid block is above it or above its partner, or
  - a sitting ocelot is on top.

  If the usable check fails, the client is told `techguns.container.security.denied`. That branch cannot actually trigger, because chest security is commented out.
- **Wrench pickup:** sneak + right-click with a wrench (item tool class `wrench`, usable check passed). Server side:
  1. Write the chest's `Items` (only if non-empty) and `CustomName` into `TileEntityData` on a new chest item. The item uses the same block, meta 0.
  2. Empty the tile and set the block to air.
  3. Drop the item.

  The item tooltip then shows `techguns.block.chestHasContents` = "Has contents". Only the clicked half is picked up.
- **Breaking** spills the contents, vanilla style.
- **Comparator** output is computed from the (possibly double) inventory. It is 0 while the chest is blocked from opening.
- **Weapon chest redstone:** `canProvidePower` is true for type 1, so the weapon chest gives weak power equal to its viewer count (0–15), and strong power downward, exactly like a vanilla **trapped chest** ([MUTL-35](index.md#suspected-bugs)).

### Tile entity (`TGChestTileEnt`)

- Extends [`BasicInventoryTileEnt`](../08-machines-production/framework.md#2-basicinventorytileent) with **36 slots**: 3 rows × 12, a wider layout than vanilla. A double chest has 72 slots in 6 rows.
- Automation: every side exposes every slot; insert and extract are allowed everywhere; everything is valid.
- Lid animation, open/close sounds and the adjacent-chest bookkeeping are the vanilla chest's:
  - the lid opens and closes by 0.1 per tick;
  - `random.chestopen` plays when the lid starts opening and `random.chestclosed` when it passes half-closed, both at volume 0.5 and pitch 0.9–1.0, centred on a double chest;
  - the viewer count travels by block event id 1.
- The periodic viewer recount (every 200 ticks, offset by position) only counts players whose open container is a vanilla `ContainerChest`. The Techguns container is not one, so the server count drops to 0 while players are looking ([MUTL-34](index.md#suspected-bugs)).
- Render bounding box: from (x−1, y, z−1) to (x+2, y+2, z+2).
- **NBT:** `Items`, `rotation`, optional `CustomName`. The owner and security code is fully commented out, so chests are always public.

### Container (`TGChestContainer`)

It shows **12 slots per row**, rows = inventory size ÷ 12 (3 single, 6 double). With offset o = (rows − 4) × 18:

| Slots | Position |
|---|---|
| chest slots | (8 + 18·col, 18 + 18·row) |
| player main | (8 + 18·col, 103 + 18·row + o) |
| hotbar | (8 + 18·i, 161 + o) |
| Techguns extended inventory: 3 **auto-food** slots ([`SlotFood`](../05-armor/extra-inventory.md), [`TGPlayerInventory.SLOTS_AUTOFOOD_START`](../05-armor/extra-inventory.md#1-data-model-tgplayerinventory-iinventory-12-slots) + 0..2) | (172 + 18·i, 31 + rows·18) |
| Techguns extended inventory: 6 **ammo** slots ([`SlotTG`](../05-armor/extra-inventory.md), type `AMMOSLOT`, `SLOTS_AMMO_START` + 0..5) in 2 rows of 3 | (172 + 18·i, 54 + rows·18 + 18·row) |

The extended-inventory slots belong to the player-inventory system, owned by [05](../05-armor/index.md)/06. Opening calls the chest's `openInventory` and closing calls `closeInventory`.

**Shift-click:** a chest slot moves to everything after the chest slots, filled from the end, so the TG ammo and food slots come first ([MUTL-36](index.md#suspected-bugs)). Anything else moves to the chest slots, from the start.

### GUI (`GuiTechgunsChest`)

The GUI extends [`GuiTechgunsTileEntBase`](../08-machines-production/gui-handler.md#2-guitechgunstileentbase-abstract-guicontainer).

- **xSize = 230**, ySize = 114 + rows × 18 (168 single, 222 double).
- Background [`textures/gui/tgchest_gui.png`](../08-machines-production/gui-textures.md#13-texturesguitgchest_guipng-owned-by-09) (256 × 256, catalogued by [08](../08-machines-production/index.md)). Two parts are drawn:
  1. Top part: (0,0), height rows × 18 + 17.
  2. Bottom part: from v = 126, height 96, placed directly below the top part.

| Texture region (px) | Depicts |
|---|---|
| (0,0)–(230,~125) | Grey panel top border and **6 rows × 12** slot wells, starting at (7,17) |
| (0,126)–(230,222) | Gap, then the 3×9 player inventory wells and the 9-slot hotbar on the left. On the right (x ≈ 171–225): a row of 3 wells (auto-food), and below it a 2×3 block of wells (ammo). Bottom-right corner plain. |

- **Text** (colour 0x404040): the chest inventory name at (8,6), via `I18n` of the names above; the player inventory name ("Inventory") at (8, ySize − 94).
- There are no buttons; the security button is commented out.

### World and item rendering

`RenderTGChest` is the vanilla chest TESR:

- It uses vanilla `ModelChest` (single) or `ModelLargeChest` (double). The double model is drawn only by the half at −X or −Z.
- Yaw by metadata: 2 → 180°, 3 → 0°, 4 → 90°, 5 → −90°.
- The lid angle is interpolated with the vanilla cubic ease-out and follows the more-open partner.

The item form ([`RenderBlockModelItemChest`](../07-blocks/block-rendering-and-models.md#4-block-item-renderers)) draws [`ModelChestInv`](../07-blocks/block-rendering-and-models.md#modelchestinv-uv-6464-textures-reinforced_chestpng--weapon_chestpng), a vanilla chest copy, with the **single** texture of its type:

| View | Transform |
|---|---|
| First person | translate (−0.1, −1, −0.9), scale 0.9 |
| Equipped | translate (0.5, −1.6, −0.5) **plus the debug keybind offsets** ([MUTL-37](index.md#suspected-bugs)) |
| Ground | translate (0, −1, 0) |
| Icon | translate (−0.5, 0.3, 0.5), scale 1.1, yaw −90° |
| Base flip | 180° about X |

### Chest textures (no animation)

The layout is the **vanilla chest UV layout at 2× resolution**:

- Single, 64 × 64 units on 128 px: lid box 14×5×14 at (0,0); latch 2×4×1 at (0,0); body 14×10×14 at (0,19).
- Double, 128 × 64 units on 256 × 128 px: the vanilla large-chest layout (lid 30×5×14 at (0,0), latch at (0,0), body 30×10×14 at (0,19)).

| File | Size | Look |
|---|---|---|
| `textures/blocks/reinforced_chest.png` | 128×128 | Oak-plank wood faces (tan, horizontal grain) framed on every edge by dark-grey iron banding. Lid underside and body top are dark brown/black (the interior). Small grey steel latch with a black keyhole at the top-left. |
| `textures/blocks/reinforced_chest_double.png` | 256×128 | The same style, stretched for the double chest |
| `textures/blocks/weapon_chest.png` | 128×128 | The same layout in flat dark olive-drab military paint with near-black edge banding and faint scratches. Interior black. Grey latch. |
| `textures/blocks/weapon_chest_double.png` | 256×128 | The double version |

---

## 2. Camp Flag

[`BasicMachine`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) **meta 7**. Name `techguns.tile.BasicMachine_7.name` = "Flag". Tile entity [`CampFlagTileEnt`](shared-rendering.md#1-basicmachine-metas-owned-by-area-09), registered `CampFlagTileEnt`. TESR [`RenderBlockTileEntModel`](../08-machines-production/rendering.md#3-renderblocktileentmodel-used-by-09s-turret-base-and-flag) + [`ModelFlagPost`](../07-blocks/block-rendering-and-models.md#modelflagpost-uv-6464-texture-flagpostpng) + [`textures/blocks/flagpost.png`](../07-blocks/textures.md#11-model-textures).

### Role

The flag is the NPC spawner and controller for generated military structures. World-gen ([11](../11-worldgen/index.md)) places and initialises it:

- **Military camps** ([`MilitaryCamp`](../11-worldgen/structure-catalogue.md#a6-military-base-militarybasestructure--militarycamp)) place it in the camp centre.
- The **aircraft carrier** ([`AircraftCarrierNew`](../11-worldgen/structure-catalogue.md#a7-aircraft-carrier-aircraftcarriernew)) places it at a fixed deck position and initialises it with `maxNPCs` = 9.

Players cannot obtain it normally:

- It is excluded from the creative list.
- Breaking it drops an **iron ingot**.
- Right-clicking does nothing.
- Wrenches are ignored.

### Initialisation (`init`)

The structure generator calls `init` with:

- the camp size X/Z;
- the flag's offset from the camp origin (x, y, z);
- a flat int array of spawn points, given as (x, y, z) offsets from the origin;
- a list of NPC entity names;
- optionally an explicit `maxNPCs`. Otherwise maxNPCs = int(((sizeX + sizeZ) ÷ 2) × 0.75).

The camp origin = flag position − offset.

### Spawning (server tick)

Constants:

| Constant | Value |
|---|---|
| check interval | 60 ticks |
| spawn interval | 200 ticks |
| area margin | 4 |
| camp height | 20 |

1. Every 60 ticks, count the living entities whose entity name is in the NPC list. The counting box runs from the origin to origin + (sizeX, 20, sizeZ), starting 3 below the origin, widened by 8 blocks on each horizontal side. If count < maxNPCs, spawning is enabled. Otherwise it is disabled and the spawn timer resets to 200.
2. While enabled and there is at least one spawn point, the spawn timer counts down. At ≤ 0:
   1. Pick a random spawn point and a random NPC name.
   2. Spawn it via [`NPCUtil.spawnEntity`](../10-entities-npcs/spawning.md#4-camp-flags-and-npcutil) ([10](../10-entities-npcs/index.md)) at origin + point, centred in X/Z (+0.5).
   3. Reset the timer to 200.

The result is at most one NPC per 10 s while the camp is below its cap.

### Destruction

`onBlockBreak`, which runs whenever the block is removed through `BasicMachine.breakBlock`, spawns an **[`AttackHelicopter`](../10-entities-npcs/per-mob.md#16-attack-helicopter-attackhelicopter-boss)** ([10-entities-npcs](../10-entities-npcs/index.md)) 50 blocks above the flag on the server.

### NBT

| Key | Content |
|---|---|
| `sizeX`, `sizeZ`, `xoffset`, `yoffset`, `zoffset` | shorts |
| `spawnPositions` | int array |
| `npcListSize` | short |
| `npcName0` … `npcNameN` | strings |
| base | `Items` (0 slots), `rotation` |

`maxNPCs` is not saved. It is recomputed from the size formula on load ([MUTL-38](index.md#suspected-bugs)).

Inventory name `container.campflag` has no lang entry, but it is never shown.

### Rendering

`RenderBlockTileEntModel` draws `ModelFlagPost`, a pole about 4 blocks tall standing on the block. Render bounding box: 1 × 4 × 1 blocks.

`ModelFlagPost`, 64 × 64 units on a 128 px texture (2 px per unit). Y is downward, the block spans y 8..24, and the model reaches y = −40, 4 blocks above the block bottom.

| Part | Box | Placement | UV |
|---|---|---|---|
| Foot plate | 10×2×10 | y 22..24 | (0,0) |
| Lower post | 5×14×5 | y 8..22 | (0,14) |
| Middle pole | 3×23×3 | y −15..8 | (22,14) |
| Top pole | 2×23×2 | y −38..−15 | (34,15) |
| Finial cap | 3×2×3 | y −40..−38 | (33,3) |
| Flag segments 1, 2, 3 | 6×14×0 each (zero-thickness planes) | hanging from the top, at x offsets 1, 7 and 13 from the pole, y −37.5..−23.5 | (0,50), (12,50), (24,50) |

**Waving animation** (client, using world time t):

| Segment | Hinge (about Y) | Angle |
|---|---|---|
| 1 | at x = 1 unit | 30° × sin(2π · (t mod 60) / 60) |
| 2 | at x = 7, relative to segment 1 | 30° × sin(2π · (t mod 50) / 50) |
| 3 | at x = 13, relative to segment 2 | 30° × sin(2π · (t mod 40) / 40) |

The chained rotation produces a rippling flag.

### Texture `textures/blocks/flagpost.png` (128 × 128, no animation)

| Region (units) | Look |
|---|---|
| (0,0) foot plate | Weathered grey metal with a dark square socket mark |
| (0,14) lower post | Darker grey metal with reddish rust specks |
| (22,14)/(34,15) poles, (33,3) cap | Pale whitish-grey metal with vertical streaks |
| (0,50)–(36,64) flag strip | Cloth flag spread over the three segments (front and back faces side by side for each): a red stripe along the top and bottom edges, a white field, and dark-blue roundels each holding a white five-pointed star. Soft cloth shading. |

[`textures/blocks/flagBlock.png`](../07-blocks/textures.md#4-camo-nets-sandbags-tent-flag-air-marker-invisible) (32 × 32) exists but is not referenced in code; [07](../07-blocks/index.md) catalogues it.

---

## 3. Bio Blob

The Bio Blob is the slime block left behind by Biogun projectiles ([02](../02-guns/index.md)/[04](../04-projectiles-fx/index.md)).

| | |
|---|---|
| Block | [`Bioblob`](../07-blocks/special-blocks.md#13-bio-blob-bioblobsmall-bioblob), [`TGBlocks.bioBlobSmall`](../07-blocks/block-catalogue.md#registered-blocks), registry [`BioBlobSmall`](../07-blocks/block-catalogue.md#registered-blocks), unlocalized `Bioblob` |
| Tile entity | `BioblobTileEnt`, registered `BioblobTileEnt` |
| Lang | `techguns.tile.Bioblob.name` = "Bio Blob" (the default item block looks up `tile.Bioblob.name`, so the name is unresolved; see [MUTL-43](index.md#suspected-bugs)) |

### Block

- Material rock. No hardness set, so it breaks instantly.
- No creative tab.
- `canHarvestBlock` is always false, so it never drops anything.
- Icon `techguns:bio` → [`textures/blocks/bio.png`](../07-blocks/textures.md#8-bug-nest-bio-blob).
- Rendered with the default cube renderer: not opaque, not a normal block, and every side drawn. The bounds vary by state.
- **Metadata = the side it sticks to**: the face of the hit block, the same codes as a block side (0 = hit the bottom face, so the blob hangs from a ceiling; 1 = on a floor; 2–5 = walls).
- **Bounds by size** s (1–3), in 1/32 units:
  - width w = 10 + 6s (16, 22, 28 → 0.5, 0.69, 0.875 blocks);
  - thickness h = 2 + 4s (6, 10, 14 → 0.19, 0.31, 0.44 blocks).

  The box is centred on the face and flush against the supporting block. For example, meta 1 spans (½ − w/2) to (½ + w/2) in X and Z, and 0..h in Y.

  Selection uses these bounds. The collision box is not recomputed per position ([MUTL-42](index.md#suspected-bugs)).
- **Contact effect:** any entity touching the block that is an `EntityLiving` (mobs, not players; [MUTL-41](index.md#suspected-bugs)) gets **Poison, amplifier 2 (Poison III), 100 ticks**.
- **Breaking** removes the tile.

### Placement and growth (driven by `BiogunProjectile`, owned by 04)

- **Hitting an existing blob** calls `hitBlob(level, shooter)`.
- **Hitting another block** (server), if the neighbouring cell on the hit side can take it:
  1. Place a new blob there with meta = hit side.
  2. If the projectile's level is above 1, call `hitBlob(level − 1)`.

  When that cell already holds a blob, it is hit instead.

### Tile entity (`BioblobTileEnt`)

| Field | Meaning |
|---|---|
| `size` | 1–3, starts at 1 |
| `ticks` | decay timer, reset to 60 |
| `side` | never written by the code; it stays 0 |

**`hitBlob(power, attacker)`:**

1. size += power.
2. If size > 3, the blob **bursts**:
   1. Create a [`ProjectileExplosion`](../04-projectiles-fx/explosions.md#projectileexplosion) ([04](../04-projectiles-fx/index.md)) at the block centre: radius 3, damage 15, inner radius 1.5, outer radius 4.5, no exploder entity.
   2. Its damage source is TG poison damage, death type BIO, with gore chance 1.0 and armour penetration 0.35. Its explosion sound is `techguns:effects.biodeath`.
   3. Set the block to air.
   4. Run the explosion without terrain damage, crediting the attacker.
   5. Server: broadcast particle effect `bioblobExplosion` ([04](../04-projectiles-fx/index.md)) to players within 50 blocks.
3. Reset `ticks` to 60 and sync.

**Tick:** `ticks` − 1. At 0:

- If size > 1: size − 1, reset to 60 and sync from the server.
- Else: set the block to air. The blob evaporates.

An un-fed blob therefore shrinks one size step every 3 s and vanishes 3 s after reaching size 1. This runs on both sides ([MUTL-44](index.md#suspected-bugs)).

A client-side size change triggers a render refresh, so the bounds update.

**NBT:** `BlobTicks` (int), `side` (byte), `size` (byte). Synced through full-NBT description packets.

### Texture and sound

| Asset | Details |
|---|---|
| `textures/blocks/bio.png` | 16 × 16, no animation. Mottled bright slime green (#2a9a10–#5cd030) with darker green blotches. Used on every face. |
| [`sounds/effects/biodeath.ogg`](../04-projectiles-fx/sounds.md) (event `techguns:effects.biodeath`) | Played by the burst explosion. The file is catalogued by [04](../04-projectiles-fx/index.md)/10. |

The projectile's own texture [`textures/entity/bioblob.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) belongs to 04.
