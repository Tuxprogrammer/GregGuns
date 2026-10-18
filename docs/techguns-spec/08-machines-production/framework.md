# Machine framework (shared base classes)

Everything in this file is shared by the production machines in this area **and** by the utility machines documented in `09-machines-utility` (turret, camo bench, repair bench, explosive charges, camp flag, scrapper, ore drill, chest).

## 1. Class hierarchy

| Class | Extends / implements | Role |
|---|---|---|
| `BasicInventoryTileEnt` | `TileEntity`, `ISidedInventory` | Item array, rotation byte, custom name, NBT and description-packet sync, "spill on break", button hook, `canOutput` helper |
| `BasicOwnedTileEnt` | `BasicInventoryTileEnt`, `ITGTileEntSecurity` | Owner UUID and security level |
| `BasicPoweredTileEnt` | `BasicOwnedTileEnt`, CoFH `IEnergyReceiver` | RF buffer, redstone mode, security and redstone buttons, access check |
| `BasicMachineTileEntity` | `BasicPoweredTileEnt` | Recipe-machine state: current operation, progress, total time, stack multiplier |
| `MultiBlockMasterTileEntBase` | `BasicMachineTileEntity` | `complete` flag for multiblock controllers |
| `MultiBlockSlaveTileEntBase` | `TileEntity` | Link to master position (no inventory or energy of its own) |
| `ITGTileEntSecurity` | interface | `setOwner(player)`, `isOwnedByPlayer(player)`, `getOwner()`, `getSecurity()` |

Concrete machines in this area: `AmmoPressTileEnt`, `MetalPressTileEnt`, `ChemLabTileEnt`, `ChargingStationTileEnt` (all `BasicMachineTileEntity`), `FabricatorTileEntMaster` and `ReactionChamberTileEntMaster` (`MultiBlockMasterTileEntBase`), `FabricatorTileEntSlave` and `ReactionChamberTileEntSlave` (`MultiBlockSlaveTileEntBase`).

Tile-entity registry names (set in `Techguns.java`): `AmmoPressTileEnt`, `MetalPressTileEnt`, `ChemLabTileEnt`, `ChargingStationTileEnt`, `FabricatorTileEntMaster`, `FabricatorTileEntSlave`, `ReactionChamberTileEntMaster`, `ReactionChamberTileEntSlave` (plus the utility ones listed in 09).

## 2. `BasicInventoryTileEnt`

- **Inventory.** A fixed-size `ItemStack[]` sized by the subclass. The stack limit is 64. `decrStackSize`, `getStackInSlotOnClosing` and `setInventorySlotContents` follow vanilla chest behaviour; `setInventorySlotContents` clamps oversize stacks to 64.
- **Rotation.** `rotation` is a byte from 0 to 3. `rotate()` adds 1 and wraps at 4. On the server it then calls `needUpdate()`. Renderers turn the model by `90° × rotation` about Y (see `rendering.md`).
- **Custom name.** `name` is stored as `CustomName` and only written when it isn't empty. Nothing in the mod sets it (anvil-renamed blocks don't carry it over), so it's effectively unused.
- **Use range.** `useRangeSquared` defaults to 64 (8 blocks). `isUseableByPlayer` checks that the tile at the position is still this tile and that the player's squared distance to the block centre is within that range.
- **`needUpdate()`** (server only): marks the block for update, which re-sends the description packet, and marks the tile dirty.
- **Description packet.** `S35PacketUpdateTileEntity` carries the full NBT, including the whole inventory. The client applies it with `readFromNBT`. So every `needUpdate()` resyncs everything.
- **`onBlockBreak()`.** Spills every stack at a random point inside the block, as vanilla chests do: chunks of 10–30 items, slight random velocity with +0.2 upward, NBT copied. It then notifies comparators.
- **`emptyContent()`.** Nulls all slots. Used after wrench-dismantling so the contents aren't spilled as well.
- **`canOutput(stack, slot)`.** True if the stack is null, or the slot is empty and the stack size is ≤64, or the slot holds an equal item (`isItemEqual`: item and damage) and the combined size fits within the item's max stack size.
- **`buttonClicked(id, player)`.** Hook for GUI button packets. Does nothing at this level.

**NBT keys:** `CustomName` (string, optional); `Items` (list of compounds, each with byte `Slot` plus the standard item tags); `rotation` (byte).

## 3. Ownership and security (`BasicOwnedTileEnt`, `ITGTileEntSecurity`)

| Field | Values |
|---|---|
| `owner` | Player UUID, or null (unowned) |
| `security` | 0 = public, 1 = friends, 2 = owner only |

- **Setting the owner.** `setOwner(player)` is called from `onBlockPlacedBy` when a player places the block. For multiblocks only the controller metas (1 and 4) get an owner. It stores the profile UUID and resets security to 0.
- **Owner check.** `isOwnedByPlayer` compares UUIDs. It's always false when unowned.
- **Access rule.** `BasicPoweredTileEnt.isUseableByPlayer` first applies the base range check, then:
  - unowned, or security 0: allowed;
  - the player is the owner: allowed;
  - security 1: allowed when the owner and the player are friends. The check is `!TGNpcFactions.isHostile(owner, player)`, which asks the FTBUtilities friends API. Without FTBUtilities the dummy integration says everyone is a friend, so level 1 behaves like level 0. See `10-entities-npcs` for factions.
  - otherwise: denied.
- **Where access is enforced.**
  - Opening a GUI: `BasicMachine` checks before `openGui`. `MultiBlockMachineBlock` checks only for the Reaction Chamber (see bug MPRD-08 for the Fabricator).
  - On denial, the client prints the chat line `techguns.container.security.denied` ("[DENIED] You have no permission to use"). The click is still consumed.
  - `Container.canInteractWith` calls `isUseableByPlayer` every tick, so an open GUI closes if access is lost or the player walks out of range.
  - Machine-specific GUI buttons check `isUseableByPlayer` again on the server.
- **Not protected:** breaking the block, wrench rotation and wrench dismantling (MPRD-09), automation (hoppers, pipes) and energy input.
- **NBT keys:** `security` (byte) and `owner` (UUID string). Both are written **only when an owner exists**. An unparseable UUID loads as unowned.
- **GUI security button** (see `gui-textures.md` for the sprites). The tooltip shows the owner's last known username (Forge `UsernameCache`, or `UNKNOW_PLAYERNAME`), the colour-coded level, the level description and "Only the owner can change this setting". Unowned machines show `techguns.container.security.tooltip.unowned`.

## 4. Energy (`BasicPoweredTileEnt`)

- **API.** CoFH RF: `IEnergyReceiver` backed by `cofh.api.energy.EnergyStorage(capacity)`. Max receive and max extract both equal the capacity, so there's no per-tick input limit.
- **Connections.** `canConnectEnergy` is true on all six sides for single-block machines. Multiblocks override it (see their files). The block never pushes or exports energy.
- **`consumePower(amount)`.** If config `MachinesNeedNoPower` (category `general`, default `false`) is true, it returns true without draining anything. Otherwise it drains `amount` and returns true only if at least that much was stored. When there isn't enough, nothing is drained and progress stalls for that tick; it isn't lost.
- **NBT.** Compound `Energy` (CoFH's own key `Energy` inside), boolean `hasSignal`, byte `redstoneBehaviour`.

| Machine | Capacity (RF) | Consumption |
|---|---|---|
| Ammo Press | 20 000 | 5 RF/t × stack multiplier, 100 t per operation |
| Metal Press | 20 000 | 20 RF/t × stack multiplier, 100 t per operation |
| Chem Lab | 20 000 | recipe RF/t (**not** multiplied), 100 t per operation |
| Charging Station | 100 000 | recipe mode: 20 RF/t × multiplier; item mode: up to 100 RF/t transferred into the item |
| Fabricator | 100 000 | 80 RF/t × multiplier, 100 t per operation |
| Reaction Chamber | 1 000 000 | recipe `RFTick` once per reaction tick (every 60 game ticks) |

## 5. Redstone control

`redstoneBehaviour` (byte):

| Value | Mode | Lang key (English) | Machine runs when |
|---|---|---|---|
| 0 | Ignore | `techguns.redstonemode.ignore` ("ignored") | always |
| 1 | High | `techguns.redstonemode.high` ("high") | the block receives power |
| 2 | Low | `techguns.redstonemode.low` ("low") | the block doesn't receive power |

- `hasSignal` is updated only when `BasicMachine.onNeighborBlockChange` calls the tile's `onNeighborBlockChange`. The tile reads `isBlockIndirectlyGettingPowered` server-side and calls `needUpdate()` when the value changes. It isn't computed on placement.
- `MultiBlockMachineBlock` has that call commented out, so multiblock controllers never update `hasSignal` (MPRD-07).
- When `isRedstoneEnabled()` is false, the whole `updateEntity` work is skipped: progress doesn't advance and no power is used.
- **GUI.** There's a two-lamp indicator at (6,5): the left lamp is red when the machine is disabled, the right lamp green when enabled. A redstone button sits in a side tab. Both areas share one tooltip:
  - "Redstone Info"
  - "Required: \<mode>"
  - "Signal: high/low" (`techguns.redstonemode.signal.high` / `.low`)

## 6. GUI button protocol

The client sends packet `GUIButtonClick` (server-bound) with fields `x:int, y:int, z:int, buttonId:int`. The server looks up the tile at that position and calls `buttonClicked(buttonId, player)` if it's a `BasicInventoryTileEnt`. Button IDs:

| ID | Constant | Action | Who may press |
|---|---|---|---|
| 1 | `GuiTechgunsPoweredTileEnt.BUTTON2_ID` | cycle security 0→1→2→0, then `needUpdate()` | owner only |
| 2 | `GuiTechgunsPoweredTileEnt.BUTTON_ID` (= `BasicPoweredTileEnt.MAX_BUTTON_USED`) | cycle redstone mode 0→1→2→0, then `needUpdate()` | anyone passing `isUseableByPlayer` |
| 3+ | `MAX_BUTTON_USED + n` | machine-specific (see each machine file) | anyone passing `isUseableByPlayer` |

## 7. Recipe-machine state (`BasicMachineTileEntity`)

| Field | NBT key | Type | Meaning |
|---|---|---|---|
| `currentOperation` | `CurrentOperation` (compound; omitted when null) | ItemStack | Output of the running operation (one unit's worth). `isWorking()` means this is non-null. |
| `progress` | `Progress` | short | ticks done |
| `totalTime` | `TotalTime` | short | ticks needed |
| `stackmultiplier` | `StackMultiplier` | byte | how many recipe units this operation processes at once |

- **Client helpers.**
  - `getProgress()` = progress/totalTime (0 when totalTime is 0).
  - `getProgressScaled(n)` = progress·n/totalTime.
  - `getPowerScaled` is broken and unused (MPRD-02).
- **`onFluidContainerInteract(stack)`.** Hook for right-clicking with a fluid container. It returns the stack unchanged by default; only the Chem Lab overrides it.

### Common processing pattern

Ammo Press, Metal Press, Chem Lab, Fabricator and Charging Station all follow this pattern:

1. **Idle, redstone enabled.** Look up a recipe for the current inputs. If one matches and the output fits:
   - choose a stack multiplier;
   - consume the inputs immediately (multiplier × per-unit amounts);
   - set `currentOperation` and `progress = 0`, `totalTime = 100` (Charging Station: chargeAmount/1000);
   - `needUpdate()`.
2. **Working.** Each tick, `consumePower(rate × multiplier)`. On success, `progress++` and play the machine's sound schedule.
3. **`progress ≥ totalTime`, server side.**
   - Add `output × multiplier` to the output slot. This is done without re-checking space, because space was reserved at the start.
   - Reset progress, totalTime, operation and multiplier.
   - Immediately try to start the next operation, then `needUpdate()`.

The Ammo Press and Metal Press run the whole `updateEntity` (including starting operations and consuming inputs) on **both** sides as client-side prediction. Server state wins through the description packet. The Chem Lab, Fabricator and Charging Station only start and finish operations server-side.

### Machine upgrade: stack operation

- **Item.** Shared item meta 52, `techguns.item.machineStackUpgrade`, "Machine Upgrade (Stack Operation)". Its max stack size is 7. Recipe (owned by `06-items`): `pgp / ncn / pnp` with p = `plateIron`, g = `dyeGreen`, n = `ingotGold`, c = vanilla chest.
- **Slot.** `SlotMachineUpgrade` accepts only this item and has a slot limit of 7.
- **Effect.** The maximum multiplier is upgrades + 1, so 1 to 8 (the Fabricator and Charging Station clamp it at 8 explicitly). The machine picks the **largest** multiplier m ≤ max for which:
  - the inputs hold at least m units, and
  - the output slot can hold `output × m`.

  It then consumes m units and produces m outputs in the same 100 ticks. Power per tick is multiplied by m, except in the Chem Lab (MPRD-12).
- There are no other machine upgrades (no speed or energy upgrades).

## 8. Sync summary

| Data | Mechanism |
|---|---|
| Inventory, rotation, owner, security, energy, redstone, current operation, tanks, machine flags | full NBT in the description packet whenever `needUpdate()` runs (state changes, operation start and end) |
| progress, totalTime, energy, fluid amounts and types, machine modes while a GUI is open | `Container.detectAndSendChanges` → `sendProgressBarUpdate(id, value)`. Values are sent as **16-bit shorts**, so large RF values are divided by 100 (Fabricator, Reaction Chamber, Charging Station) and the client multiplies back, giving 100-RF granularity. Energy is resent every tick while the machine is working. |
| Fluid type in containers | fluid ID int, or −1 for none |

## 9. Multiblock framework

### Master (`MultiBlockMasterTileEntBase`)

- `complete` (NBT `complete`, boolean).
- `notifiyMultiblockBreak()` (sic) sets complete false and calls `needUpdate()`. Both concrete masters override it to call their full `unform()`.
- `needBlockUpdate()` (server) calls `notifyBlockChange` on its own position.
- `causeRenderUpdate()` (client) marks its own block for re-render. It's meant to run when `complete` changes on NBT load, but never does (MPRD-01).

### Slave (`MultiBlockSlaveTileEntBase`)

- NBT `hasMaster` (bool), plus `masterX` (int), `masterY` (short) and `masterZ` (int) when linked.
- Has its own description packet. On load it re-renders its block when `hasMaster` changed.
- `updateMaster(x,y,z)` links; `unlink()` clears.
- `notifyMasterMultiblockBreak()` tells the master tile at the stored position to break the multiblock.
- `getMasterPos()` returns null when unlinked.

### Block (`MultiBlockMachineBlock`)

See `blocks-and-items.md`. `breakBlock`:
- on a slave, notifies the master;
- on a master, unforms and then spills its inventory.

Both then remove the tile entity. Formed blocks render with the transparent `invisible` icon (CTM variation meta 15) and the controller's TESR draws the whole machine.

Multiblocks have no wrench rotation or dismantling. The structure orientation is the side of the controller that was clicked.
