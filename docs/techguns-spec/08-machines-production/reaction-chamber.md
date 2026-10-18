# Reaction Chamber (3×4×3 multiblock)

The Reaction Chamber transmutes items with a "beam focus", a fluid bath set to a chosen level, and an intensity setting. During a reaction the preferred intensity **drifts**, and the player has to keep re-tuning to follow it. If the reaction fails, the input can be lost or the chamber can explode.

Classes:
- `ReactionChamberTileEntMaster` (`MultiBlockMasterTileEntBase`, `IFluidHandler`)
- `ReactionChamberTileEntSlave` (`MultiBlockSlaveTileEntBase`, `IEnergyReceiver`, `ISidedInventory`, `IFluidHandler`)
- `ReactionChamberOperation`, `ReactionChamberRecipe` (with enum `RiskType`), `ReactionBeamFocus`
- `ReactionChamberContainer`, `GuiReactionChamber`, `RenderReactionChamber`

## 1. Structure

Blocks (block `machineMultiBlock`):
- Reaction Chamber Controller (meta 1): ×1
- Reaction Chamber Housing (meta 0): ×17
- Reaction Chamber Glass (meta 2): ×18

**Shape.** A solid box 3 wide × 4 tall × 3 deep:

| Layer (y offset) | Content |
|---|---|
| 0 (bottom) | 3×3 housing, with the **controller at the front-centre** |
| 1 and 2 | 3×3 glass each, including the centre column (solid glass) |
| 3 (top) | 3×3 housing |

**Forming.** Right-click the unformed controller on one of its four side faces. The clicked face is `dir`, and the box extends away from that face. Top or bottom faces do nothing.

| `dir` | X range | Z range |
|---|---|---|
| 2 (north face) | −1..+1 | 0..+2 |
| 3 (south face) | −1..+1 | −2..0 |
| 4 (west face) | 0..+2 | −1..+1 |
| 5 (east face) | −2..0 | −1..+1 |

**Validation.**
1. All bottom- and top-layer positions except the controller must be meta 0 and unlinked.
2. Then all positions in layers 1–2 must be meta 2 and unlinked.

The first failure sends "Invalid Block for Reaction Chamber at x, y, z" (`techguns.tgmessage.0`) and stops.

**On success**, every slave links to the master and ports are assigned. The "middle" is the bottom-layer centre, one block behind the controller.

| Port | Position | Slave type |
|---|---|---|
| Power input | top-layer centre (middle + 3 up) | 2 |
| Item / fluid I/O | the bottom-layer centre-back, centre-left and centre-right (the middle's horizontal neighbours other than the controller) | 1 |
| Everything else | | 0 (inert) |

The master then sets `complete = true` and `dir`, and syncs. All 36 blocks render invisible and the TESR draws the chamber.

**Opening the GUI.**
- Complete controller: open GUI 18 if `isUseableByPlayer` (range **12 blocks**, `useRangeSquared` 144); otherwise send the denial chat line.
- Linked slave: the same check against the master, then GUI 18 at the master.
- Debug: holding `blockReaderTool` prints master or slave info instead of opening the GUI.

**Unforming.** Breaking any part, or a failed explosion, unlinks every block in the box, resets the slave types, sets the master incomplete with `dir` 0, and syncs. The master's `updateEntity` does nothing while incomplete, so a running reaction **pauses** and resumes when the structure is re-formed. Breaking the controller spills its inventory; the fluid and energy are lost.

## 2. Ports and storage

| Thing | Detail |
|---|---|
| Energy | 1 000 000 RF. The controller itself refuses connections. Only the power port (type 2) accepts RF and forwards it to the master. |
| Fluid tank | 10 000 mB (NBT `inputTank`). Only type-1 ports accept fluid. **Fill** is capped so the tank never exceeds `liquidLevel × 1000` mB. **Drain** only gives back the excess above `liquidLevel × 1000`. The controller's own fluid-handler methods refuse everything. There's no bucket interaction. |
| Items | Type-1 ports expose slots 0, 2, 3, 4, 5. Insert into slot 0 only; extract slots 2–5. The focus slot (1) is manual only. |

**Master NBT:** `intensity` (byte), `liquidLevel` (byte), `inputTank`, `hasPower` and `dir` (only when complete), `operationData` (see §4), plus the framework keys. **Slave NBT:** `type` (only when linked).

## 3. Inventory and controls

| Slot | Role | GUI pos | Container slot |
|---|---|---|---|
| 0 | Input | (35,17) | `Slot` (anything) |
| 1 | Beam focus | (93,17) | `Slot` (anything) |
| 2–5 | Outputs | (134,17), (152,17), (134,35), (152,35) | `SlotOutput` |

- Shift-click from the player inventory sends Techguns shared items whose slot type is `REACTION_CHAMBER_FOCUS` to slot 1, and everything else to slot 0.
- The **liquid level** setting is 0–10 buckets (`liquidLevel`). The **intensity** setting is 0–10 (`intensity`).

| Button | ID | Pos / size | Label | Action |
|---|---|---|---|---|
| Level + | 3 | (33,58) 10×10 | `+` | level +1 (max 10) |
| Level − | 4 | (43,58) 10×10 | `-` | level −1 (min 0) |
| Intensity + | 5 | (91,36) 10×10 | `+` | intensity +1 (max 10) |
| Intensity − | 6 | (101,36) 10×10 | `-` | intensity −1 (min 0) |
| Dump tank | 7 | (20,7) 8×8 | `x` | clear the tank (fluid lost). Tooltip "Dump Input Tank" / "Content will be lost" |

All of them require `isUseableByPlayer`. Settings reach the client through the container sync.

## 4. Reaction logic

### Beam foci (`ReactionBeamFocus`)

| Item | Field `powerConsumtion` | Sound each reaction tick |
|---|---|---|
| UV Emitter (shared 92, `techguns.item.uvemitter`) | 100 (unused) | `techguns:machines.chemlabWork` |
| Reaction Chamber Heat Ray Focus (shared 98, `techguns.item.rcHeatRay`) | 100 (unused) | `techguns:machines.rc_heatrayWork` |

A focus is identified by exact item and damage.

### Recipe fields (`ReactionChamberRecipe`)

| Field | Meaning |
|---|---|
| `ID` | the list index at registration time |
| `input` | `ItemStackOreDict` |
| `beamFocus` | which focus is required |
| `liquidIn` | which fluid is required |
| `outputs` | list of output stacks |
| `ticks` | duration in reaction ticks |
| `requiredCompletion` | reaction ticks that must be "good" |
| `preferredIntensity` | 0–10 |
| `intensityMargin` | 0–10: how far the preferred intensity may drift |
| `liquidLevel` | 0–10 buckets |
| `liquidConsumtion` | mB drained on success |
| `instability` | **unused** (MPRD-17) |
| `risk` | `RiskType` |
| `RFTick` | RF per reaction tick |

### Starting

The server checks this every tick while complete, redstone-enabled and idle. The first recipe in list order matches when **all** of the following hold:

- slot 0 matches the input (ore-dictionary aware);
- the focus in slot 1 is the recipe's focus;
- the tank's fluid is the recipe fluid;
- the `liquidLevel` setting equals the recipe level, **and** the tank holds **exactly** level × 1000 mB;
- the `intensity` setting equals the recipe's preferred intensity exactly.

If stored energy ≥ `RFTick` (checked even when `MachinesNeedNoPower` is on, MPRD-15), the operation starts:
1. One input item is consumed.
2. `currentOperation` = a copy of the input (size 1). It's used for `isWorking()` and for rendering.
3. A `ReactionChamberOperation` is created with preferred intensity = the recipe's, `nextTick` = 60, `duration` = 0, `maxDuration` = ticks × 60, completion 0, liquid level = the recipe's, power = `RFTick`.

While idle the server also:
- syncs when the slot-0 stack changes, so the floating input item re-renders;
- clears a zero-amount fluid stack.

### Ticking (`ReactionChamberOperation.tick`)

Every game tick, on both sides, `nextTick` goes down by 1 and `duration` goes up by 1. On the server, when `nextTick` reaches 0 **and** `duration` < `maxDuration`, a **reaction tick** happens:

1. `consumePower(RFTick)`. If it succeeds **and** the operation's preferred intensity equals the tile's intensity setting **and** the recipe level equals the tile's level setting, then `completion++`.
2. Intensity drift: roll r ∈ [0,1).
   - r ≤ 1/3: preferred −1, if it's above (recipe preferred − margin).
   - 1/3 < r ≤ 2/3: no change.
   - r > 2/3: preferred +1, if it's below (recipe preferred + margin).

   With margin 0 it never drifts.
3. `nextTick` resets to 60. The tick returns "state changed".

On any other tick, the result is "finished" when `duration` ≥ `maxDuration` or `completion` ≥ required.

Because the reaction tick at `duration == maxDuration` is excluded, a recipe with N ticks gives only N−1 chances to gain completion (MPRD-18).

### Finishing (server, whenever `tick` reports a change)

- **Success** (completion ≥ required):
  1. Each output is added to slots 2–5. It merges into matching stacks first, then fills empty slots; anything that doesn't fit drops as an item at the controller.
  2. `liquidConsumtion` mB is drained from the tank. The tank is then below the level, so it must be topped up before the next run.
- **Failure** (duration ≥ maxDuration): the input is already gone. Then the risk applies:

| `RiskType` | Effect |
|---|---|
| `NONE`, `BREAK_ITEM` | nothing more (the input is lost) |
| `EXPLOSION_LOW` | `explode(0)` |
| `EXPLOSION_MEDIUM` | `explode(1)` |
| `RAD_LOW`, `RAD_MEDIUM`, `RAD_HIGH`, `EXPLOSION_HIGH`, `UNFORSEEN_CONSEQUENCES` | not implemented (nothing happens) |

- In every case the server then calls `needUpdate()`, so the client receives the full state once per reaction tick.

**`explode(type)`** (server). The centre block is the middle column at layer 1.
1. Unform the structure.
2. Set the centre block to air.
3. Type 0 (low): set the centre's 4 horizontal neighbours to air (the glass ring of layer 1). Put the tank fluid's block at the centre if the fluid has one, otherwise air. Spawn particle system `FragGrenadeExplosion` at the centre for players within 50 blocks (see `04-projectiles-fx`) and play `random.explode` at volume 4 and a randomised pitch.
4. Type 1 (medium): set every other block of the 3×2×3 box (layers 1–2) to air, i.e. all the glass. Create a vanilla explosion of power 4 that damages terrain, with no source entity. Then, if the fluid has a block, place it at the centre and **a glass block right above it** (MPRD-19).
5. Both types: set stored energy to 0, empty the tank, and drop the slot-0 contents (the next queued inputs) as an item at the centre. Removed blocks don't drop anything.

### Client feedback

When the client-side `nextTick` reaches 59 (right after each server sync):
- the focus sound plays (if a known focus is in slot 1);
- 4 vanilla `spell` particles spawn at random points in the centre block, with velocity (0,1,0);
- `techguns:machines.rc_beep` plays if the current intensity setting equals the operation's preferred intensity, otherwise `techguns:machines.rc_warning`.

All sounds play at the controller, volume 1, pitch 1.

### NBT `operationData` (written only while a reaction runs)

Keys: `recipeID` (short, **but it's read back as `ID`**, MPRD-03), `duration`, `maxDuration`, `requiredCompletion`, `completion` (shorts), `preferedIntensity`, `liquidLevel`, `nextTick` (bytes), `powerPerTick` (int). On load, the recipe is looked up by **list index** with `getById`.

## 5. Full recipe list (`TGMachineRecipes`)

Fluids: "Liquid Redstone" is the fluid named `redstone` if some mod registers it, otherwise **Lava**. "Liquid Ender" is `ender` if registered, otherwise **Lava**. Water and Lava are vanilla; Creeper Acid is Techguns.

| # | Input | Focus | Fluid | Level (buckets) | Consumed on success | Outputs | Reaction ticks (×3 s) | Required | Preferred intensity | Margin | Instability (unused) | Risk | RF per reaction tick (NEI "RF/T" = /60) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Wheat Seeds | UV Emitter | Water | 1 | 1000 mB | Wheat ×1 + Wheat Seeds ×2 | 10 | 5 | 3 | 2 | 0 | BREAK_ITEM | 50 000 (833) |
| 2 | `gemDiamond` | Heat Ray | Liquid Redstone | 4 | 4000 mB | Laser Focus ×1 (shared 36) | 10 | 5 | 3 | 2 | 0 | BREAK_ITEM | 100 000 (1666) |
| 3 | Titanium Iron Ore (`oreTitanIron:0`) | Heat Ray | Creeper Acid | 3 | 100 mB | Titanium Ore ×2 (shared 94) + Iron Ore ×1 | 2 | 1 | 5 | 0 | 0 | BREAK_ITEM | 25 000 (416) |
| 4 | Quartz Rod (shared 99) | Heat Ray | Lava | 4 | 1000 mB | Blaze Rod ×1 | 5 | 3 | 7 | 2 | 0 | BREAK_ITEM | 250 000 (4166) |
| 5 | `blockRedstone` | Heat Ray | Lava | 4 | 1000 mB | Glowstone (block) ×1 | 5 | 3 | 7 | 2 | 0 | EXPLOSION_MEDIUM | 250 000 (4166) |
| 6 | Nether Star | Heat Ray | Liquid Ender | 4 | 4000 mB | Anti Gravity Core ×1 (shared 100) | 10 | 7 | 8 | 1 | 1 | EXPLOSION_LOW | 500 000 (8333) |
| 7 | `dustUranium` (Yellowcake) | Heat Ray | Water | 3 | 1000 mB | Enriched Uranium ×1 (shared 102) | 5 | 4 | 7 | 0 | 1 | BREAK_ITEM | 250 000 (4166) |

## 6. GUI (`GuiReactionChamber`, `textures/gui/ReactionChamber_Gui.png`)

| Element | Position | Detail |
|---|---|---|
| Title | centred, y 6 | `techguns.container.reactionChamber` ("Reaction Chamber") |
| Tank | fluid area (19,17) 10×50 | Fluid icon tiled bottom-up, height = amount·50/10000. Gauge overlay u176 v32, 12×52, at (18,17). |
| Level marker | (19, 17+50−level·5), 7×1 | sprite u177 v32 (red line) marks the target level |
| Level text | (34,50) | "\<level×10>%" |
| Tank tooltip | (18,16) 11×51 | fluid name or the literal "Empty" (not localized); "amount/capacitymB" |
| Intensity bar | (114, 16+px), 5 × (40−px), px = (10−intensity)·4 | Fills bottom-up. Sprite u190 v px (green) normally; u198 v px (orange) while working **and** the setting ≠ preferred |
| Preferred-intensity marker (working only) | (112, 15+40−pref·4), 5×3 | sprite u178 v22 (small red/white arrow) |
| Intensity text | (121,18) | the number |
| Completion bar (working) | (67,61), width round(completion/required·100), height 4 | sprite u0 v167 (green, with tick dots) |
| Duration bar (working) | (67,69), width round(duration/maxDuration·100), height 4 | sprite u0 v175 (orange) |

- The power bar, redstone and security controls come from the base class.
- **Container sync:** 0 progress, 1 totalTime (unused here, always 0), 2 power/100, 3 fluid ID, 4 fluid amount, 5 liquid level, 6 intensity. The reaction state itself arrives through the description packet.

## 7. Visuals (`RenderReactionChamber`)

Drawn only while complete.

### Model and textures

- OBJ model `models/entity/reactionchamber.obj`, scaled ×1/32. Group `CHAMBER` (324 faces) is 96×128×96 model units, i.e. 3×4×3 blocks. Group `GLASS` is the glass shell.
- `textures/entity/reactionchamber.png` (256×256) is the chamber atlas. Mostly black background with:
  - light-grey and white sci-fi panels, including a large framed panel with a black cross/vent symbol (right middle);
  - dark-grey grated and riveted plates with a diagonal-stripe vent (bottom left);
  - a small yellow/black hazard square (top centre);
  - a blue display panel (bottom).
  - Palette: white #f0f0f0, light greys, near-black, small yellow and blue accents.
- `textures/entity/reactionchamberglass.png` (128×128) is the glass shell texture: two square panes outlined in light grey with thin white diagonal glare streaks and corner brackets, mostly transparent.
- `textures/fx/heatray.png` (128×128) is used as the beam billboard: a soft yellow-white teardrop glow fading to black (additive). Catalogued in `04-projectiles-fx`.

### Transform

1. Translate to the controller, then to its block centre.
2. Rotate about Y by `dir`: 2 → −180°, 3 → 0°, 4 → −90°, 5 → +90°.
3. Translate −1 along local Z to the chamber centre column.

Render bounding box: ±2 blocks in X and Z, 4 tall.

### Draw order

1. **Floating item.** An `EntityItem` at local y 1.5: `currentOperation` while working, otherwise the slot-0 stack.
2. **Chamber model** with the main texture, every group except `GLASS`.
3. **Beam effect.** Only while working **and** a focus is in slot 1.
   - Depth-write is off, blending is additive, FX lighting is on, back-face culling is off.
   - It's a single quad billboarded about Y to face the player, textured with `heatray.png`.
   - Width 1.875·d, spanning local y 0.5 to 2.9375.
   - d pulses between 0.875 and 1.125 with a 10-tick cosine period.
   - Alpha = intensity/10·d − 0.25.
   - The heat-ray texture is used **for any focus**, including the UV emitter (MPRD-26).
4. **Liquid.** If the tank holds fluid: an alpha-blended cube 1.875 wide, from local y 0.5 up to 0.5 + 2.9375·(amount/10000), using the fluid's still icon (flowing icon while working).
5. **Glass group** with the glass texture.
