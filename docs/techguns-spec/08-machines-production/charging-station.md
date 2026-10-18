# Charging Station (`basicMachine:9`)

The Charging Station has two modes:
- **Recipe mode:** turns an "empty" item into a "charged" item (Energy Cell (Empty) → Energy Cell).
- **Item mode:** pushes RF directly into any CoFH `IEnergyContainerItem`.

Classes: `ChargingStationTileEnt`, `ChargingStationContainer`, `GuiChargingStation`, `ChargingStationRecipe`, `RenderChargingStation` with `ModelChargingStation`.

## 1. Inventory

| Slot | Role | GUI pos | Container slot |
|---|---|---|---|
| 0 | Input | (19,17) | `Slot` |
| 1 | Output | (68,17) | `SlotOutput` |
| 2 | Upgrade | (150,50) | `SlotMachineUpgrade` |

- **Sided access.** Slots 0 and 1 from every side. Insert into slot 0 (hoppers also need `isItemValidForSlot`); extract slot 1.
- **Valid-slot routing:**
  - upgrade → slot 2;
  - any `IEnergyContainerItem` → slot 0;
  - any item that is the input of a charging recipe → slot 0;
  - anything else → rejected.
- `isItemValidForSlot(slot, item)` is true when routing(item) == slot.
- Capacity: 100 000 RF.
- **Extra NBT:** `charging` (bool). True while item-mode charging is in progress; used by the renderer.

## 2. Logic (each tick, if redstone allows)

1. **Recipe operation running.**
   1. Consume 20 RF × multiplier.
   2. On success: progress++ and call the effects routine (client).
   3. When progress ≥ totalTime (server): add output × multiplier to slot 1, reset, try to start the next operation, then `needUpdate()`.
2. **Else, if slot 0 holds an `IEnergyContainerItem`:**
   - **The item still accepts energy** (a simulated receive of 100 > 0):
     1. Transfer min(stored, 100) RF into it.
     2. Remove what it actually accepted from the station's buffer.
     3. Server: set `charging = true` the first time and sync.
     4. Client: call the effects routine.

     This transfer uses the buffer directly, so it **ignores** `MachinesNeedNoPower` (MPRD-15).
   - **The item is full** (server):
     1. If slot 1 is empty, move the item there.
     2. Set `charging = false` and call `needUpdate()`. This repeats every tick while slot 1 is occupied (MPRD-13).
3. **Else** (server):
   1. If `charging` is set, clear it and sync.
   2. Try to start a recipe operation; sync if it started.

**Starting a recipe operation.**
- The first recipe whose input matches slot 0 wins.
- Multiplier m:
  1. start at min(upgrades+1, 8);
  2. cap it at slot-0 size / recipe input size;
  3. cap it so the output fits in slot 1.

  With slot 1 empty, the "fits" step returns min(maxStack, output size × m), which is an item count rather than a multiplier (MPRD-10).
- If m > 0:
  1. Consume input size × m.
  2. `currentOperation` = output (one unit).
  3. totalTime = chargeAmount / (CHARGERATE·10) = chargeAmount / 1000 ticks.
- Total cost = 20·m RF/t × chargeAmount/1000 t, which is 2% of the nominal charge amount.

`isWorking()` is true when an operation is running **or** slot 0 holds an `IEnergyContainerItem`.

### Recipe list (`TGMachineRecipes`)

| Input | Output | chargeAmount (RF, as shown in NEI) | Time | Actual RF used |
|---|---|---|---|---|
| Energy Cell (Empty) (shared 16) | Energy Cell (shared 15) | 50 000 | 50 ticks | 1 000 per unit |

MineTweaker can add or remove recipes (`minetweaker.md`).

## 3. GUI (`GuiChargingStation`, `textures/gui/ChargingStation_Gui.png`)

- **Title.** `techguns.container.chargingStation` ("Charging Station"). Inventory label as usual.
- **Upgrade slot frame.** u7 v83, 18×18, drawn at (149,49).
- **Progress.** Between the two slots the texture prints a grey outline of a lightning-bolt arrow at about (38,18). While working, sprite u0 v167 (a red→orange→yellow lightning arrow), width progress·26/total + 1, height 10, is drawn at (39,19). It fills **left→right**. In item mode totalTime is 0, so only a 1-pixel sliver shows (MPRD-28).
- **Container sync:** 0 progress, 1 totalTime, 2 power/100.

## 4. Effects and sound (client only, called while working)

The effects routine:
- **ChargeFlare particles.** Only when the client player is within 10 blocks (squared distance < 100). Spawned every tick of activity at four points at block-centre height, skipping the one on the side matching `rotation`:
  - (0.5, 0.5, 0.3), skipped when rotation 2;
  - (0.5, 0.5, 0.7), skipped when rotation 0;
  - (0.3, 0.5, 0.5), skipped when rotation 1;
  - (0.7, 0.5, 0.5), skipped when rotation 3.

  The `ChargeFlare` particle is defined in `04-projectiles-fx`.
- **Sound.** `techguns:machines.chargingstationwork` every 20 calls (a countdown, `lastsound`), volume 1, pitch 1. The clip is 2.08 s, so it loops roughly continuously while charging.

## 5. Visuals

- **Renderer.** `RenderChargingStation` (extends `RenderAmmoPress`) with `ModelChargingStation` (declared 64×64) and texture `textures/blocks/ChargingStation.png` (128×128, catalogued in 07-blocks).
- **Look.** A cage: bottom and top plates (each with an inner plate), four corner pillars, three side plates (one side open) and three power coils with sockets (`Power1..3`, `PowerS1..3`), joined by connectors. The texture is light concrete-grey panels, black/grey sockets and red accent stripes. A glass part exists in the model but isn't rendered.
- **Item inside.** `currentOperation` (recipe mode), or the slot-0 item while `charging` (item mode), rendered as an `EntityItem` hovering in the middle of the cage. It's turned by −90° − 90°·rotation about Y and placed at local y −1.225 (block centre after the base translate).
- **Model orientation.** Rotated 180° + 90°·rotation about Y, i.e. 180° offset from the other machines' convention. The flare positions in §4 suggest the closed side sits at the back.
- No moving parts. The progress argument is passed in but ignored.
