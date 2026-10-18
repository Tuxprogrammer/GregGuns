# Machine blocks, metadata, placement and drops

## 1. `BasicMachine`: registry name `basicMachine`

| Property | Value |
|---|---|
| Block class / item | `BasicMachine` (`BlockContainer`); item `ItemBlockMetaDataMachine` |
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
| 0 | Ammo Press | `AmmoPressTileEnt` | 0 | 08 |
| 1 | Metal Press | `MetalPressTileEnt` | 1 | 08 |
| 2 | Turret (base) | `TurretBaseEnt` | 2 | 09 |
| 3 | Camo Bench | `CamoBenchTileEnt` | 3 | 09 |
| 4 | Chemical Laboratory | `ChemLabTileEnt` | 4 | 08 |
| 5 | Repair Bench | `RepairBenchTileEnt` | 5 | 09 |
| 6 | Explosive Charge | `ExplosiveChargeTileEnt` | 6 | 09 |
| 7 | Flag (camp flag; worldgen only) | `CampFlagTileEnt` | none | 09 / 11 |
| 8 | Advanced Explosive Charge | `ExplosiveChargeAdvTileEnt` | 6 (shared) | 09 |
| 9 | Charging Station | `ChargingStationTileEnt` | 9 | 08 |
| 10 | (Scrapper, disabled: tile creation commented out, not in creative) | none | 10 (registered but unreachable) | 09 |

### Interaction (`onBlockActivated`)

The branches are checked in this order:

1. **Held item has tool class `wrench`, and meta is not 6, 7 or 8.**
   - Not sneaking: `rotate()` (rotation +1 mod 4).
   - Sneaking (server only):
     1. Write the tile's data (inventory, energy, owner, everything) into a new item `basicMachine:<meta>` under NBT key `TileEntityData`.
     2. Empty the tile and set the block to air.
     3. Drop the item at the block corner.

   No security check is made in either case (MPRD-09). The item tooltip then shows `techguns.block.hasTileEntityData` ("Has TileEntity Data"), added by `ItemBlockMetaDataMachine`.
2. **Meta 4 (Chem Lab) and the held item is a registered fluid container.** Calls `onFluidContainerInteract` (see `chem-lab.md`).
   - If a new stack comes back and the held stack size is 1 (not creative), it replaces the held item.
   - Otherwise one item is removed from the held stack (except in creative) and the new stack goes into the inventory, or is dropped if the inventory is full.
3. **Meta 6 or 8.** If the charge isn't armed, open GUI 6.
4. **Meta 7.** Nothing.
5. **Other metas.** If the tile is a `BasicPoweredTileEnt` and the player passes `isUseableByPlayer`, open GUI = meta; otherwise show the security-denied chat line. Unpowered tiles (camo and repair benches) open GUI = meta directly.

### Placement

- `onBlockAdded` (server) sets a furnace-like default rotation from solid neighbours: start at 3; solid at z−1 only → 1; solid at z+1 only → 0; solid at x−1 only → 3; solid at x+1 only → 2.
- `onBlockPlacedBy`:
  - sets the owner (player placers only);
  - server, if the item has `TileEntityData`: loads it into the tile, which restores the previous owner and security as well, then sets rotation = (yaw quadrant + 2) mod 4;
  - turret bases spawn their turret;
  - calls `needUpdate()`.

  On the client, rotation is set to (yaw quadrant + 2) mod 4.
- `ItemBlockMetaDataMachine.placeBlockAt` runs after that:
  - metas 0, 1, 4 and 9: rotation = (facing + 2) mod 4, where facing = floor(yaw·4/360 + 0.5) & 3. **The machine front faces the player.**
  - metas 3 and 5: a different formula (09).
  - metas 6 and 8: store the placement side as the charge orientation (09).

### Breaking and drops

- `breakBlock` destroys the mounted turret (meta 2), spills the inventory and removes the tile entity.
- Drops are the block itself with the same meta (`damageDropped = meta`). Exception: meta 7 drops an iron ingot. Energy, fluids and the running operation are lost unless the block was wrench-dismantled.
- `onNeighborBlockChange` forwards to `BasicPoweredTileEnt.onNeighborBlockChange` (redstone).

### Crafting recipes for the machines in this area

Shaped ore recipes in `TGItems`; `06-items` owns the ingredient items.

| Output | Pattern | Key |
|---|---|---|
| Ammo Press ×1 (meta 0) | `ili / cec / iri` | i `ingotIron`, l `ingotLead`, c `ingotCopper`, e Electric Engine (shared meta 64), r redstone |
| Metal Press ×1 (meta 1) | `ibi / iei / iri` | i `ingotIron`, b `blockIron`, e Electric Engine, r redstone |
| Metal Press ×1 (meta 1, alt) | `ibi / iei / iri` | as above but b = `plateIron` |
| Chemical Laboratory ×1 (meta 4) | `igi / geg / iri` | i `ingotIron`, g glass bottle, e Electric Engine, r redstone |
| Charging Station ×1 (meta 9) | `sgs / cbc / sgs` | s `plateSteel`, g `wireGold`, b `circuitBasic`, c Coil (shared meta 69) |

## 2. `MultiBlockMachineBlock`: registry name `machineMultiBlock`

| Property | Value |
|---|---|
| Class / item | `MultiBlockMachineBlock` (`BlockContainer`, Chisel `ICarvable`, chiseling forbidden); item `ItemBlockMetaDataMachine` → names `techguns.tile.machineMultiBlock_<meta>` |
| Material / sound | iron / metal |
| Hardness / resistance | 8.0 / 50.0, pickaxe level 1 |
| Render type | Chisel CTM (`ClientUtils.renderCTMId`). `isNormalCube` true, `isOpaqueCube` false |
| Creative | metas 0–5 |

| Meta | Lang name | CTM texture (`textures/blocks/...`, catalogued in 07-blocks) | Tile |
|---|---|---|---|
| 0 | Reaction Chamber Housing | `rc_block` | `ReactionChamberTileEntSlave` |
| 1 | Reaction Chamber Controller | `rc_controller` | `ReactionChamberTileEntMaster` |
| 2 | Reaction Chamber Glass | `rc_glass` | `ReactionChamberTileEntSlave` |
| 3 | Fabricator Housing | `fabricatorHousing` | `FabricatorTileEntSlave` |
| 4 | Fabricator Controller | `fabricatorController` | `FabricatorTileEntMaster` |
| 5 | Fabricator Glass | `fabricatorGlass` | `FabricatorTileEntSlave` |
| 15 | (render-only variation) | `invisible` | used for every block of a formed structure |

- **Icon selection.** For metas ≤5, the block shows variation 15 (invisible) when its tile is a linked slave or a complete master. Otherwise it shows its own meta's texture.
- **Placement.** Metas 1 and 4 (controllers) set the owner. No rotation.
- **Activation.** See `fabricator.md` and `reaction-chamber.md`. Clicking an unformed controller tries to form the structure, using the clicked side as the direction. Clicking a linked slave opens the master's GUI. Holding the debug `blockReaderTool` on a Reaction Chamber block prints master or slave debug info to chat.
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

When structure validation fails, the server sends the client-bound packet `PacketTGMessage(type 0, x, y, z)` (fields: byte `messageType`, int `paramCoordx`, `paramCoordy`, `paramCoordz`). The client shows the chat line "`techguns.tgmessage.0` x, y, z", i.e. "Invalid Block for Reaction Chamber at x, y, z". The Fabricator uses the same message (MPRD-27).
