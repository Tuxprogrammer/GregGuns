# Reaction Chamber (3×4×3 multiblock)

The Reaction Chamber transmutes items with a "beam focus", a fluid bath set to a chosen level, and an intensity setting. During a reaction the preferred intensity **drifts**, and the player has to keep re-tuning to follow it. If the reaction fails, the input can be lost or the chamber can explode.

Classes:
- [`ReactionChamberTileEntMaster`](framework.md#1-class-hierarchy) ([`MultiBlockMasterTileEntBase`](framework.md#master-multiblockmastertileentbase), `IFluidHandler`)
- [`ReactionChamberTileEntSlave`](blocks-and-items.md#2-multiblockmachineblock-registry-name-machinemultiblock) ([`MultiBlockSlaveTileEntBase`](framework.md#slave-multiblockslavetileentbase), `IEnergyReceiver`, `ISidedInventory`, `IFluidHandler`)
- `ReactionChamberOperation`, `ReactionChamberRecipe` (with enum `RiskType`), `ReactionBeamFocus`
- [`ReactionChamberContainer`](gui-handler.md#1-techgunsguihandler-complete-gui-id-table), `GuiReactionChamber`, `RenderReactionChamber`

## 1. Structure

Blocks (block [`machineMultiBlock`](../07-blocks/block-catalogue.md#registered-blocks)):
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
- Debug: holding [`blockReaderTool`](../06-items/item-catalogue.md#4-debug--developer-items-registered-unconditionally-in-techgunsjava) prints master or slave info instead of opening the GUI.

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
| 2–5 | Outputs | (134,17), (152,17), (134,35), (152,35) | [`SlotOutput`](ammo-press.md#1-inventory) |

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
| `input` | [`ItemStackOreDict`](gui-handler.md#6-itemstackoredict-recipe-ingredient-wrapper) |
| `beamFocus` | which focus is required |
| `liquidIn` | which fluid is required |
| `outputs` | list of output stacks |
| `ticks` | duration in reaction ticks |
| `requiredCompletion` | reaction ticks that must be "good" |
| `preferredIntensity` | 0–10 |
| `intensityMargin` | 0–10: how far the preferred intensity may drift |
| `liquidLevel` | 0–10 buckets |
| `liquidConsumtion` | mB drained on success |
| `instability` | **unused** ([MPRD-17](index.md#suspected-bugs)) |
| `risk` | `RiskType` |
| `RFTick` | RF per reaction tick |

### Starting

The server checks this every tick while complete, redstone-enabled and idle. The first recipe in list order matches when **all** of the following hold:

- slot 0 matches the input (ore-dictionary aware);
- the focus in slot 1 is the recipe's focus;
- the tank's fluid is the recipe fluid;
- the `liquidLevel` setting equals the recipe level, **and** the tank holds **exactly** level × 1000 mB;
- the `intensity` setting equals the recipe's preferred intensity exactly.

If stored energy ≥ `RFTick` (checked even when [`MachinesNeedNoPower`](../01-core/config.md#category-general) is on, [MPRD-15](index.md#suspected-bugs)), the operation starts:
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

Because the reaction tick at `duration == maxDuration` is excluded, a recipe with N ticks gives only N−1 chances to gain completion ([MPRD-18](index.md#suspected-bugs)).

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
3. Type 0 (low): set the centre's 4 horizontal neighbours to air (the glass ring of layer 1). Put the tank fluid's block at the centre if the fluid has one, otherwise air. Spawn particle system `FragGrenadeExplosion` at the centre for players within 50 blocks (see [`04-projectiles-fx`](../04-projectiles-fx/index.md)) and play `random.explode` at volume 4 and a randomised pitch.
4. Type 1 (medium): set every other block of the 3×2×3 box (layers 1–2) to air, i.e. all the glass. Create a vanilla explosion of power 4 that damages terrain, with no source entity. Then, if the fluid has a block, place it at the centre and **a glass block right above it** ([MPRD-19](index.md#suspected-bugs)).
5. Both types: set stored energy to 0, empty the tank, and drop the slot-0 contents (the next queued inputs) as an item at the centre. Removed blocks don't drop anything.

### Client feedback

When the client-side `nextTick` reaches 59 (right after each server sync):
- the focus sound plays (if a known focus is in slot 1);
- 4 vanilla `spell` particles spawn at random points in the centre block, with velocity (0,1,0);
- `techguns:machines.rc_beep` plays if the current intensity setting equals the operation's preferred intensity, otherwise `techguns:machines.rc_warning`.

All sounds play at the controller, volume 1, pitch 1.

### NBT `operationData` (written only while a reaction runs)

Keys: `recipeID` (short, **but it's read back as `ID`**, [MPRD-03](index.md#suspected-bugs)), `duration`, `maxDuration`, `requiredCompletion`, `completion` (shorts), `preferedIntensity`, `liquidLevel`, `nextTick` (bytes), `powerPerTick` (int). On load, the recipe is looked up by **list index** with `getById`.

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

- OBJ model [`models/entity/reactionchamber.obj`](../10-entities-npcs/rendering-and-models.md#other-obj-files-in-modelsentity-in-this-folder-used-elsewhere), scaled ×1/32. Group `CHAMBER` (324 faces) is 96×128×96 model units, i.e. 3×4×3 blocks. Group `GLASS` is the glass shell.
- [`textures/entity/reactionchamber.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) (256×256) is the chamber atlas. Mostly black background with:
  - light-grey and white sci-fi panels, including a large framed panel with a black cross/vent symbol (right middle);
  - dark-grey grated and riveted plates with a diagonal-stripe vent (bottom left);
  - a small yellow/black hazard square (top centre);
  - a blue display panel (bottom).
  - Palette: white #f0f0f0, light greys, near-black, small yellow and blue accents.
- [`textures/entity/reactionchamberglass.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) (128×128) is the glass shell texture: two square panes outlined in light grey with thin white diagonal glare streaks and corner brackets, mostly transparent.
- [`textures/fx/heatray.png`](../04-projectiles-fx/textures.md#single-images) (128×128) is used as the beam billboard: a soft yellow-white teardrop glow fading to black (additive). Catalogued in [`04-projectiles-fx`](../04-projectiles-fx/index.md).

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
   - The heat-ray texture is used **for any focus**, including the UV emitter ([MPRD-26](index.md#suspected-bugs)).
4. **Liquid.** If the tank holds fluid: an alpha-blended cube 1.875 wide, from local y 0.5 up to 0.5 + 2.9375·(amount/10000), using the fluid's still icon (flowing icon while working).
5. **Glass group** with the glass texture.

## 8. Entity sheet summary (consistency pass)

| # | Field | Contents |
|---|---|---|
| 1 | Identity | Block `machineMultiBlock` metas 0 (Housing), 1 (Controller), 2 (Glass); tiles `ReactionChamberTileEntSlave` (0, 2) and `ReactionChamberTileEntMaster` (1); names `techguns.tile.machineMultiBlock_0/1/2.name` = **Reaction Chamber Housing**, **Reaction Chamber Controller**, **Reaction Chamber Glass**; container `ReactionChamberContainer`; creative tab Techguns; oredict none |
| 2 | Obtaining | Shaped ore recipes: Housing ×9, Controller ×1, Glass ×6 ([blocks-and-items.md](blocks-and-items.md#crafting-recipes)). The Housing recipe consumes a Chemical Laboratory block |
| 3 | Numbers | Buffer 1 000 000 RF; tank 10 000 mB; structure 3×4×3; intensity and level settings 0 to 10; reaction tick every 60 game ticks; hardness 8.0, resistance 50.0; 2 foci, 7 recipes |
| 4 | Behaviour | Sections 3 and 4 |
| 5 | Persistence and sync | Master NBT: `complete`, `hasPower`, `dir`, `inputTank`, `intensity`, `liquidLevel`, `operationData`, plus base keys. Slave NBT: `hasMaster`, `masterX/Y/Z`, `type`. GUI fields: 0 progress, 1 totalTime (unused), 2 power/100, 3 fluid ID, 4 fluid amount, 5 liquid level, 6 intensity. Buttons are in section 3 |
| 6 | UI | Section 6. Exact strings: title "Reaction Chamber", tank tooltip fluid name or the literal "Empty", level text "n0%" (level times 10), intensity number; NEI shows "Unstable" / "Risk" with the risk names None, Destroy Input Item, Small Explosion, Medium Explosion |
| 7 | Visuals | Unformed blocks use CTM textures `rc_block` (32×32, average #C5C5C5, dominant #F0F0F0 56 %, #B0B0B0 28 %, #505050 7 %), `rc_controller` (32×32, average #C8D3DC, #F0F0F0 50 %, #B0B0B0 34 %, #B0D0F0 2 %) and `rc_glass` (32×32, average #D7D7D7, #F0F0F0 55 %, #B0B0B0 42 %, 52 % opaque). Formed: TESR `RenderReactionChamber`, OBJ chamber and glass groups (section 7 and the glass-geometry subsection), textures `textures/entity/reactionchamber.png` (256×256, average #3B3B3B, dominant #101010 64 %, #F0F0F0 9 %, #303030 7 %) and `textures/entity/reactionchamberglass.png` (128×128, average #C7C7C7, 4 % opaque), beam `textures/fx/heatray.png` (128×128). Particle: vanilla `spell` ×4 per reaction tick. Item renderer: ordinary block icons |
| 8 | Audio | Focus sound (`machines.rc_heatrayWork`, 4.00 s, 44100 Hz mono, or `machines.chemlabWork`, 3.50 s), `machines.rc_beep` (0.11 s, 44100 Hz), `machines.rc_warning` (1.35 s, 22050 Hz); all volume 1, pitch 1; plus `random.explode` volume 4.0, pitch 0.7·(1 ± 0.2·random) on a low-risk explosion. Triggers in [sounds.md](sounds.md#5-per-event-call-parameters-volume-pitch-side-exact-condition) |
| 9 | Relationships | `ReactionChamberRecipe`/`ReactionBeamFocus` from [`TGMachineRecipes`](index.md#tgmachinerecipesaddrecipes-what-it-registers) and [`ReactionChamberTweaker`](minetweaker.md#1-loading); NEI handler; foci, fluids and outputs in [`06-items`](../06-items/index.md)/[`04-projectiles-fx`](../04-projectiles-fx/index.md) |
| 10 | Bugs | [MPRD-01](index.md#suspected-bugs), [MPRD-03](index.md#suspected-bugs), [MPRD-04](index.md#suspected-bugs), [MPRD-07](index.md#suspected-bugs), [MPRD-15](index.md#suspected-bugs), [MPRD-17](index.md#suspected-bugs), [MPRD-18](index.md#suspected-bugs), [MPRD-19](index.md#suspected-bugs), [MPRD-22](index.md#suspected-bugs), [MPRD-26](index.md#suspected-bugs), [MPRD-31](index.md#suspected-bugs), [MPRD-35](index.md#suspected-bugs) |

### 9. Glass group geometry (`models/entity/reactionchamber.obj`, group `GLASS`)

Earlier passes recorded only that the glass is a shell. Reading the OBJ directly gives the exact geometry.

- **File layout.** 284 vertices, 620 texture coordinates, 284 normals and 334 triangular faces in total. The `CHAMBER` group (324 triangles) starts at file line 1171; `GLASS` starts at line 1535 and has **10 triangles**, which is 5 quads. The header comment says "10 faces".
- **Vertices.** The glass uses the last eight vertices (numbers 277 to 284), forming an axis-aligned box:
  - X from −32 to +32 and Z from −32 to +32 (one of the Z values is −31.999998, a rounding artefact);
  - Y from 34 to 112.
- **In blocks.** The renderer scales by 1/32, so the box is **2.0 × 2.4375 × 2.0 blocks**, from 1.0625 to 3.5 blocks above the model origin. It sits inside the 3×4×3 hull (the `CHAMBER` group spans ±48 units in X and Z, i.e. 3 blocks): a smaller glass core, not a full-size shell.
- **Faces.** Four side quads plus one **top** quad (at Y = 112). There is **no bottom face**, so from below the box is open.
- **Texture mapping.** The glass texture is 128×128 and the UV map is 1:1 with the box size in model units, which makes the mapping exact once the loader's V flip (V = 1 − v) is applied:
  - each of the four sides uses the rectangle **(0,0)–(63,77)**, i.e. 64 × 78 px (source V values 0.390625 to 1.0 before the flip);
  - the top uses **(64,0)–(127,63)**, i.e. 64 × 64 px (source U 0.5 to 1.0, V 0.5 to 1.0 before the flip).
  The right-hand 64 columns below row 64 and the bottom 50 rows of the left column are unused.
- **What the texture holds in those rectangles** (measured, 683 non-transparent pixels out of 16 384): the side-pane rectangle contains 377 of them and the top-pane rectangle 306; nothing outside the two rectangles is opaque. Both panes are outline-and-glare patterns on a transparent field: a one-pixel frame in light grey (about #C5C5C5 to #CACACA; the commonest colour is #C9C9C9) plus a few diagonal streaks. That matches the 4 % opaque figure.
- **Normals.** The `vn` entries are rotation-style triples (values of 1.570796, i.e. π/2) rather than unit vectors, so the file carries no usable lighting normals; the renderer doesn't light the glass.
- **Rendering state.** Per section 7, the glass group is drawn last with the glass texture. Whether alpha blending is enabled for that draw is not set in the OBJ itself.
