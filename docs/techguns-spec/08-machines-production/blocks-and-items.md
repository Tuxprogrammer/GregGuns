# Machine blocks, metadata, placement and drops

## 1. `BasicMachine`: registry name `basicMachine`

| Property | Value |
|---|---|
| Block class / item | `BasicMachine` (`BlockContainer`); item [`ItemBlockMetaDataMachine`](../07-blocks/special-blocks.md#16-itemblockmetadatamachine) |
| Unlocalized name | `tile.BasicMachine`. Item stacks are named `techguns.tile.BasicMachine_<meta>` (lang key `techguns.tile.BasicMachine_<meta>.name`) |
| Material / sound | iron / metal |
| Hardness / resistance | 4.0 / 20.0. Metas 6 and 8 (explosive charges) override hardness to 0.1 and are harvestable by hand |
| Harvest tool | pickaxe level 1 for metas 0–5 and 7 |
| Render type | −1 (TESR only). Not opaque, not "render as normal block". `isNormalCube` is true for metas 0–5 only |
| Block texture name | `techguns:screen_top` (used only for break particles) |
| Mob spawning | never |
| Creative tab | Techguns. Lists metas 0–9 except 7 |

| Meta | Machine | Tile entity | GUI ID | Owning area |
|---|---|---|---|---|
| 0 | Ammo Press | [`AmmoPressTileEnt`](framework.md#1-class-hierarchy) | 0 | 08 |
| 1 | Metal Press | [`MetalPressTileEnt`](framework.md#1-class-hierarchy) | 1 | 08 |
| 2 | Turret (base) | [`TurretBaseEnt`](../09-machines-utility/turret.md#tile-entity-turretbaseent) | 2 | [09](../09-machines-utility/index.md) |
| 3 | Camo Bench | [`CamoBenchTileEnt`](../09-machines-utility/index.md#key-facts) | 3 | [09](../09-machines-utility/index.md) |
| 4 | Chemical Laboratory | [`ChemLabTileEnt`](framework.md#1-class-hierarchy) | 4 | 08 |
| 5 | Repair Bench | [`RepairBenchTileEnt`](../09-machines-utility/index.md#key-facts) | 5 | [09](../09-machines-utility/index.md) |
| 6 | Explosive Charge | [`ExplosiveChargeTileEnt`](../09-machines-utility/explosive-charges.md#nbt-explosivechargetileent) | 6 | [09](../09-machines-utility/index.md) |
| 7 | Flag (camp flag; worldgen only) | [`CampFlagTileEnt`](../09-machines-utility/shared-rendering.md#1-basicmachine-metas-owned-by-area-09) | none | [09](../09-machines-utility/index.md) / 11 |
| 8 | Advanced Explosive Charge | [`ExplosiveChargeAdvTileEnt`](../09-machines-utility/explosive-charges.md) | 6 (shared) | [09](../09-machines-utility/index.md) |
| 9 | Charging Station | [`ChargingStationTileEnt`](framework.md#1-class-hierarchy) | 9 | 08 |
| 10 | (Scrapper, disabled: tile creation commented out, not in creative) | none | 10 (registered but unreachable) | [09](../09-machines-utility/index.md) |

### Interaction (`onBlockActivated`)

The branches are checked in this order:

1. **Held item has tool class `wrench`, and meta is not 6, 7 or 8.**
   - Not sneaking: `rotate()` (rotation +1 mod 4).
   - Sneaking (server only):
     1. Write the tile's data (inventory, energy, owner, everything) into a new item `basicMachine:<meta>` under NBT key `TileEntityData`.
     2. Empty the tile and set the block to air.
     3. Drop the item at the block corner.

   No security check is made in either case ([MPRD-09](index.md#suspected-bugs)). The item tooltip then shows `techguns.block.hasTileEntityData` ("Has TileEntity Data"), added by `ItemBlockMetaDataMachine`.
2. **Meta 4 (Chem Lab) and the held item is a registered fluid container.** Calls `onFluidContainerInteract` (see `chem-lab.md`).
   - If a new stack comes back and the held stack size is 1 (not creative), it replaces the held item.
   - Otherwise one item is removed from the held stack (except in creative) and the new stack goes into the inventory, or is dropped if the inventory is full.
3. **Meta 6 or 8.** If the charge isn't armed, open GUI 6.
4. **Meta 7.** Nothing.
5. **Other metas.** If the tile is a [`BasicPoweredTileEnt`](framework.md#4-energy-basicpoweredtileent) and the player passes `isUseableByPlayer`, open GUI = meta; otherwise show the security-denied chat line. Unpowered tiles (camo and repair benches) open GUI = meta directly.

### Placement

- `onBlockAdded` (server) sets a furnace-like default rotation from solid neighbors: start at 3; solid at z−1 only → 1; solid at z+1 only → 0; solid at x−1 only → 3; solid at x+1 only → 2.
- `onBlockPlacedBy`:
  - sets the owner (player placers only);
  - server, if the item has `TileEntityData`: loads it into the tile, which restores the previous owner and security as well, then sets rotation = (yaw quadrant + 2) mod 4;
  - turret bases spawn their turret;
  - calls `needUpdate()`.

  On the client, rotation is set to (yaw quadrant + 2) mod 4.
- `ItemBlockMetaDataMachine.placeBlockAt` runs after that:
  - metas 0, 1, 4 and 9: rotation = (facing + 2) mod 4, where facing = floor(yaw·4/360 + 0.5) & 3. **The machine front faces the player.**
  - metas 3 and 5: a different formula ([09](../09-machines-utility/index.md)).
  - metas 6 and 8: store the placement side as the charge orientation ([09](../09-machines-utility/index.md)).

### Breaking and drops

- `breakBlock` destroys the mounted turret (meta 2), spills the inventory and removes the tile entity.
- Drops are the block itself with the same meta (`damageDropped = meta`). Exception: meta 7 drops an iron ingot. Energy, fluids and the running operation are lost unless the block was wrench-dismantled.
- `onNeighborBlockChange` forward to `BasicPoweredTileEnt.onNeighborBlockChange` (redstone).

### Crafting recipes for the machines in this area

Shaped ore recipes in [`TGItems`](../06-items/item-catalog.md); [`06-items`](../06-items/index.md) owns the ingredient items.

| Output | Pattern | Key |
|---|---|---|
| Ammo Press ×1 (meta 0) | `ili / cec / iri` | i `ingotIron`, l [`ingotLead`](../06-items/item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems), c [`ingotCopper`](../06-items/item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems), e Electric Engine (shared meta 64), r redstone |
| Metal Press ×1 (meta 1) | `ibi / iei / iri` | i `ingotIron`, b `blockIron`, e Electric Engine, r redstone |
| Metal Press ×1 (meta 1, alt) | `ibi / iei / iri` | as above but b = [`plateIron`](../06-items/item-catalog.md#1-the-shared-meta-item-techgunstechgunsammo) |
| Chemical Laboratory ×1 (meta 4) | `igi / geg / iri` | i `ingotIron`, g glass bottle, e Electric Engine, r redstone |
| Charging Station ×1 (meta 9) | `sgs / cbc / sgs` | s [`plateSteel`](../06-items/item-catalog.md#1-the-shared-meta-item-techgunstechgunsammo), g `wireGold`, b `circuitBasic`, c Coil (shared meta 69) |

## 2. `MultiBlockMachineBlock`: registry name `machineMultiBlock`

| Property | Value |
|---|---|
| Class / item | `MultiBlockMachineBlock` (`BlockContainer`, Chisel `ICarvable`, chiseling forbidden); item `ItemBlockMetaDataMachine` → names `techguns.tile.machineMultiBlock_<meta>` |
| Material / sound | iron / metal |
| Hardness / resistance | 8.0 / 50.0, pickaxe level 1 |
| Render type | Chisel CTM (`ClientUtils.renderCTMId`). `isNormalCube` true, `isOpaqueCube` false |
| Creative | metas 0–5 |

| Meta | Lang name | CTM texture (`textures/blocks/...`, cataloged in [07-blocks](../07-blocks/index.md)) | Tile |
|---|---|---|---|
| 0 | Reaction Chamber Housing | `rc_block` | `ReactionChamberTileEntSlave` |
| 1 | Reaction Chamber Controller | `rc_controller` | [`ReactionChamberTileEntMaster`](framework.md#1-class-hierarchy) |
| 2 | Reaction Chamber Glass | `rc_glass` | `ReactionChamberTileEntSlave` |
| 3 | Fabricator Housing | `fabricatorHousing` | `FabricatorTileEntSlave` |
| 4 | Fabricator Controller | `fabricatorController` | [`FabricatorTileEntMaster`](framework.md#1-class-hierarchy) |
| 5 | Fabricator Glass | `fabricatorGlass` | `FabricatorTileEntSlave` |
| 15 | (render-only variation) | `invisible` | used for every block of a formed structure |

- **Icon selection.** For metas ≤5, the block shows variation 15 (invisible) when its tile is a linked slave or a complete master. Otherwise it shows its own meta's texture.
- **Placement.** Metas 1 and 4 (controllers) set the owner. No rotation.
- **Activation.** See `fabricator.md` and `reaction-chamber.md`. Clicking an unformed controller tries to form the structure, using the clicked side as the direction. Clicking a linked slave opens the master's GUI. Holding the debug [`blockReaderTool`](../06-items/item-catalog.md#4-debug--developer-items-registered-unconditionally-in-techgunsjava) on a Reaction Chamber block prints master or slave debug info to chat.
- **Breaking.** See the multiblock notes in `framework.md`. The block drops itself with the same meta.

### Crafting recipes

`hardenedGlassOrGlass` is `blockGlassHardened` if that ore name exists, else `blockGlass`.

| Output | Pattern | Key |
|---|---|---|
| Reaction Chamber Housing ×9 | `sms / pcp / ses` | s `plateSteel`, m Mechanical Parts (Carbon) (shared 37), p Cybernetic Parts (shared 91), e `circuitElite`, c Chem Lab block (`basicMachine:4`) |
| Reaction Chamber Controller ×1 | ` g / crc /  g ` | g hardened glass or glass, c `circuitElite`, r RC Housing |
| Reaction Chamber Glass ×6 | `rgr / rgr / rgr` | r RC Housing, g hardened glass or glass |
| Fabricator Housing ×4 | `sms / pep / scs` | s `plateSteel`, m Mechanical Parts (Carbon), p Cybernetic Parts, e Electric Engine, c `circuitElite` |
| Fabricator Controller ×1 | ` g / cfc /  g ` | g glass, c `circuitElite`, f Fabricator Housing |
| Fabricator Glass ×4 | `fgf / g g / fgf` | f Fabricator Housing, g glass |

## 3. Multiblock error message

When structure validation fails, the server sends the client-bound packet `PacketTGMessage(type 0, x, y, z)` (fields: byte `messageType`, int `paramCoordx`, `paramCoordy`, `paramCoordz`). The client shows the chat line "`techguns.tgmessage.0` x, y, z", i.e. "Invalid Block for Reaction Chamber at x, y, z". The Fabricator uses the same message ([MPRD-27](index.md#suspected-bugs)).

## 4. Entity sheet summary (consistency pass)

### 4.1 `basicMachine` (all metas used by 08)

| # | Field | Contents |
|---|---|---|
| 1 | Identity | Class `BasicMachine`; registry [`basicMachine`](../07-blocks/block-catalog.md#registered-blocks); unlocalized `tile.BasicMachine`; item `ItemBlockMetaDataMachine`; metas 0, 1, 4, 9 belong to 08 (lang keys in [lang.md](lang.md#1-blocks)); creative tab Techguns; oredict none |
| 2 | Obtaining | Section 1 recipe table; each machine file repeats it in its sheet |
| 3 | Numbers | Hardness 4.0, resistance 20.0, iron material, metal step sound, pickaxe level 1, never spawns mobs |
| 4 | Behavior | Interaction, placement, breaking sections above |
| 5 | Persistence and sync | Item NBT `TileEntityData` (the whole tile compound) on wrench-dismantled machines; tile keys in [framework.md](framework.md) |
| 6 | UI | Item tooltip: `techguns.block.hasTileEntityData`, "Has TileEntity Data", added by `ItemBlockMetaDataMachine.addInformation` whenever the stack's NBT has `TileEntityData`; no other item tooltip lines. Chat: "[DENIED] You have no permission to use" |
| 7 | Visuals | Render type −1; no face textures. Break-particle icon [`textures/blocks/screen_top.png`](../07-blocks/textures.md#6-radar-screen-and-control-panel) (32×32, average #534540, dominant #503030 49 %, #505030 20 %, #707070 12 %). Per-machine models in [rendering.md](rendering.md) |
| 8 | Audio | Step and break sound is the vanilla metal type; machine sounds in [sounds.md](sounds.md). Explosive charges (meta 6, 8) play a plant sound at volume 4.0, pitch 1.0 on placement ([09](../09-machines-utility/index.md)) |
| 9 | Relationships | Metas 2, 3, 5, 6, 7, 8 and 10 belong to [`09-machines-utility`](../09-machines-utility/index.md) |
| 10 | Bugs | MPRD-09, [MPRD-24](index.md#suspected-bugs), [MPRD-34](index.md#suspected-bugs) |

### 4.2 `machineMultiBlock`

| # | Field | Contents |
|---|---|---|
| 1 | Identity | Class `MultiBlockMachineBlock`; registry [`machineMultiBlock`](../07-blocks/block-catalog.md#registered-blocks); metas 0 to 5 plus the render-only 15; names in [lang.md](lang.md#1-blocks); creative tab Techguns |
| 2 | Obtaining | Section 2 recipe table |
| 3 | Numbers | Hardness 8.0, resistance 50.0, pickaxe level 1, Chisel CTM, chiseling forbidden |
| 4 | Behavior | Section 2 and the machine files |
| 5 | Persistence and sync | Slave `type` byte and master link in [framework.md](framework.md#9-multiblock-framework) |
| 6 | UI | Same tooltip rules as `basicMachine` (same item class) |
| 7 | Visuals | Textures per meta in the section 2 table. Measured: `rc_block` 32×32 #C5C5C5; `rc_controller` 32×32 #C8D3DC; `rc_glass` 32×32 #D7D7D7 (52 % opaque); `fabricatorHousing` 32×32 #949494; `fabricatorController` 32×32 #787D7E; `fabricatorGlass` 32×32 #1A1919 (23 % opaque); `invisible` 16×16, fully transparent. Per-face CTM variations were not examined |
| 8 | Audio | vanilla metal block sound; machine sounds in [sounds.md](sounds.md) |
| 9 | Relationships | [fabricator.md](fabricator.md), [reaction-chamber.md](reaction-chamber.md) |
| 10 | Bugs | [MPRD-01](index.md#suspected-bugs), [MPRD-07](index.md#suspected-bugs), [MPRD-08](index.md#suspected-bugs), MPRD-27, [MPRD-35](index.md#suspected-bugs) |
