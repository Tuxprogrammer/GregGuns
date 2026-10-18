# Chemical Laboratory (`basicMachine:4`)

The Chem Lab is a two-item, one-fluid-in machine. Each recipe gives an optional item output and/or an optional fluid output. It has an input tank and an output tank.

Classes: `ChemLabTileEnt` (`BasicMachineTileEntity` + Forge `IFluidHandler`), `ChemLabContainer`, `GuiChemLab`, `GuiButtonToggleDrainTank`, `ChemLabRecipes` (inner classes `ChemLabRecipe` and `ChemLabOutput`).

## 1. Inventory and tanks

| Slot | Role | GUI pos | Container slot |
|---|---|---|---|
| 0 | Input 1 | (35,17) | `Slot` |
| 1 | Input 2 | (57,17) | `Slot` |
| 2 | "Flask" slot (glass bottle only) | (35,40) | `Slot` |
| 3 | Output | (135,17) | `SlotOutput` |
| 4 | Upgrade | (135,47) | `SlotMachineUpgrade` |

| Tank | Capacity | NBT | Filled by | Drained by |
|---|---|---|---|---|
| `inputTank` | 8 000 mB | `inputTank` | pipes (`fill` from any side, any fluid; `canFill` always true); right-click with a filled container | the output-selector toggle (pipes or empty containers when `drainInput` is set); the dump button |
| `outputTank` | 16 000 mB | `outputTank` | recipes only | pipes or empty containers when `drainInput` is false; the dump button |

- `getTankInfo` returns [input, output].
- **Sided item access.** Slots 0–3 from every side, insert only if `isItemValidForSlot`, extract slot 3 only.
- **`isItemValidForSlot`:**
  - slot 2: glass bottle only;
  - slot 0 when both inputs are empty: any item used by a recipe;
  - slot 0 when only slot 1 is filled: must pair with slot 1 in some recipe;
  - slot 0 when filled: the same item;
  - slot 1 when slot 0 is filled: must pair with slot 0;
  - slot 1 when only slot 1 is filled: the same item;
  - anything else: false.
- **Shift-click target**, first match wins: same as slot 0 → 0; same as slot 1 → 1; slot 0 empty and used in a recipe → 0; pairs with slot 0 → 1; same as slot 2 → 2; glass bottle → 2; upgrade → 4. The container ranges (0–4 machine, 5–40 player) are correct here.
- **Extra NBT:** `drainInput` (bool) and `currentChemOperation` (compound with optional `output` item, optional `liquidOutput` fluid, int `powerPerTick`).
- Capacity: 20 000 RF.

## 2. Right-click with fluid containers

`BasicMachine.hasBucketInteraction` is true only for meta 4. Behaviour of `onFluidContainerInteract`:

- **Empty registered container** (bucket, bottle…): pick the tank selected by `drainInput` (input if true, else output). If it holds at least the container's capacity of its fluid, fill the container and drain that amount. The full container is returned.
- **Filled container:** if the input tank is empty or holds the same fluid, and the whole content fits, empty the container into the input tank. The empty container is returned.
- Otherwise nothing happens.

The block code then swaps the held item (see `blocks-and-items.md`). This runs on both client and server.

## 3. Recipe model

A `ChemLabRecipe` has:

| Field | Meaning |
|---|---|
| `slot1`, `slot2` | `ItemStackOreDict`. A null stack means "slot must be empty" |
| `slot3` | ItemStack for the flask slot. Always null in practice |
| `amounts[0..3]` | consumed amounts for slot1, slot2, slot3 and input fluid. The fluid amount = `fluidIn.amount` |
| `fluidIn` | required fluid, or null for none |
| `fluidOutput` | produced fluid, or null |
| `output` | produced item, or null |
| `allowSwap` | slot 1 and slot 2 may be swapped |
| `powerPerTick` | RF/t |

**Matching (`isValidInput`).**
1. slot1 matches input 1 and slot2 matches input 2, or swapped if allowed.
2. **and** the flask slot matches: recipe slot3 null ⇒ the flask slot must be empty.
3. **and** the fluid matches: recipe fluid null ⇒ anything (including other fluid in the tank) is fine; otherwise the same fluid with amount ≥ required.
4. **and** the amounts are present: each slot has at least its amount, and a slot with amount 0 must be empty.

Because no recipe uses slot3, **a glass bottle in the flask slot blocks every recipe** (MPRD-14). The first matching recipe in list order wins.

**Operation.**
- **Start** (server only), if the item output fits and the fluid output can merge into the output tank (same fluid and space, or empty tank):
  1. Choose the multiplier m: the largest m ≤ upgrades+1 where m× output fits and m× all amounts are present.
  2. Consume `amount[i]·m` from slots 0, 1, 2 and `amount[3]·m` mB from the input tank. With a swapped match the amounts aren't swapped (MPRD-11).
  3. totalTime = 100.
- **Each tick:** consume `powerPerTick` RF. This is **not** multiplied by m (MPRD-12).
- **Finish** (server): add item×m to slot 3 and fluid×m to the output tank, then reset and try the next operation.

## 4. Full recipe list (`TGMachineRecipes`, in registration order)

Fluids: Water, Lava (vanilla); Creeper Acid (`creeper_acid`, Techguns); Milk (`milk`, taken from another mod or registered by Techguns); Oil (`oil`) and Fuel (`fuel`) **only if another mod registers them**. "—" means none, or the slot must be empty.

| # | Condition | Input 1 (amt) | Input 2 (amt) | Fluid in | Item out | Fluid out | Swap | RF/t |
|---|---|---|---|---|---|---|---|---|
| 1a | ore name `dustCoal` exists | `dustRedstone` (1) | `dustCoal` (1) | Water 250 | Gunpowder ×1 | — | yes | 5 |
| 1b | otherwise | `dustRedstone` (1) | Coal (meta 0) (1) | Water 250 | Gunpowder ×1 | — | yes | 5 |
| 2 | | Sand (1) | Gravel (1) | Water 250 | Concrete (Light, `concrete:0`) ×2 | — | yes | 5 |
| 3a | Oil registered | Slimeball (1) | Coal (1) | Water 1000 | — | Oil 250 | yes | 20 |
| 3b | Oil registered | `itemRawRubber` (1) | — | Oil 500 | Raw Plastic ×1 | — | no | 25 |
| 3c | Oil not registered | `itemRawRubber` (1) | Coal (1) | Water 1000 | Raw Plastic ×1 | — | yes | 25 |
| 4 | | Bio Mass (1) | Gunpowder (1) | Water 1000 | — | Creeper Acid 1000 | yes | 20 |
| 5a | ore name `itemBioFuel` exists | `itemBioFuel` (4) | Bio Tank (Empty) (1) | Water 500 | Bio Tank ×1 | — | no | 1 |
| 5b | ore name `itemBioFuel` exists | `itemBioFuel` (4) | Gunpowder (1) | Water 1000 | — | Creeper Acid 1000 | no | 20 |
| 6a | Fuel registered | `itemGunpowder` (1) | `gemLapis` (1) | Fuel 250 | TGX ×1 | — | yes | 20 |
| 6b | Fuel registered | Fuel Tank (Empty) (1) | — | Fuel 250 | Fuel Tank ×1 | — | no | 1 |
| 6c | Fuel registered | Fuel Tank (1) | — | — | Fuel Tank (Empty) ×1 | Fuel 250 | no | 1 |
| 6d | Fuel not registered | `itemGunpowder` (1) | `gemLapis` (1) | Lava 500 | TGX ×1 | — | yes | 20 |
| 6e | Fuel not registered | Fuel Tank (Empty) (1) | — | Lava 500 | Fuel Tank ×1 | — | no | 1 |
| 6f | Fuel not registered | Fuel Tank (1) | — | — | Fuel Tank (Empty) ×1 | Lava 500 | no | 1 |
| 7 | | `gemDiamond` (1) | Blaze Powder (1) | Lava 1000 | Carbon Fibers ×2 | — | yes | 25 |
| 8 | | `logWood` (1, any meta) | — | Water 1000 | Raw Rubber ×1 | — | no | 20 |
| 9 | | Bio Mass (1) | Bio Tank (Empty) (1) | Water 500 | Bio Tank ×1 | — | yes | 1 |
| 10 | | Coal (1) | Compressed Air Tank (Empty) (1) | Water 250 | Compressed Air Tank ×1 | — | yes | 5 |
| 11 | | Netherrack (1) | Soul Sand (1) | Lava 1000 | Nether Charge ×4 | — | yes | 20 |
| 12 | | `oreUranium` (1, any meta) | — | Creeper Acid 250 | Yellowcake ×1 | — | no | 20 |
| 13 | | Nuclear Power Cell (Depleted) (1) | Enriched Uranium (1) | Water 1000 | Nuclear Power Cell ×1 | — | yes | 40 |
| 14 | Milk exists (always true) | Cactus Green (`dye:2`) (1) | `itemRawRubber` (1) | Milk 500 | Slimeball ×1 | — | yes | 25 |
| 15 | | Leather (2) | Slimeball (1) | Creeper Acid 500 | Treated Leather ×2 | — | no | 20 |
| 16 | | Dirt (1) | Gravel (1) | Water 250 | Clay (block) ×1 | — | yes | 5 |
| 17 | | Rotten Flesh (1) | — | Water 1000 | Leather ×1 | — | no | 5 |

- Every operation takes 100 ticks. Total RF = RF/t × 100. NEI shows "RF/t".
- Techguns item names are shared-item metadata (see `06-items`): Raw Plastic 71, Bio Mass 38, Bio Tank 10, Bio Tank (Empty) 11, TGX 60, Fuel Tank 8, Fuel Tank (Empty) 9, Carbon Fibers 61, Raw Rubber 70, Compressed Air Tank 28, Compressed Air Tank (Empty) 29, Nether Charge 63, Yellowcake 101, Nuclear Power Cell 106, Nuclear Power Cell (Depleted) 107, Enriched Uranium 102, Treated Leather 109.
- Techguns registers these ore names: `itemRawRubber` (Raw Rubber); `dustUranium` (Yellowcake); `oreUranium` (presumably the Uranium Ore block, `oreTitanIron:1`, see 07/11).

### Furnace recipes also registered in `TGMachineRecipes` (0 XP)

| Input | Output |
|---|---|
| Raw Rubber | Rubber Bar |
| Raw Plastic | Plastic Sheet |
| Titanium Ore (shared item 94) | Titanium Ingot |
| Obsidian Steel Plate | Obsidian Steel Ingot |
| Titanium Plate | Titanium Ingot |

## 5. GUI (`GuiChemLab`, `textures/gui/chemLab_Gui.png`)

| Element | Position | Detail |
|---|---|---|
| Title | centred, y 6 | `techguns.container.chemlab` ("Chemical Laboratory") |
| Input tank | fluid area (19,17) 10×50 | Fluid icon tiled bottom-up, height = amount·50/8000. Gauge overlay u176 v32, 12×52 at (18,17): tick marks plus a red top line. |
| Output tank | fluid area (158,17) 10×50; overlay at (157,17) | height = amount·50/16000 |
| Tank tooltips | (18,16) 11×51 and (157,16) 11×51 | line 1: translated fluid name or `techguns.gui.empty` ("Empty"); line 2: "amount/capacitymB" |
| Dump-input button (ID 3) | (20,7), 8×8, label "x" | Empties the input tank (fluid is lost). Tooltip "Dump Input Tank" / "Content will be lost". |
| Dump-output button (ID 4) | (159,7), 8×8, "x" | Empties the output tank. Tooltip "Dump Output Tank" / "Content will be lost". |
| Drain-toggle button (ID 5) | (156,68), 14×14 | `GuiButtonToggleDrainTank`: flips `drainInput`. Tooltip `techguns.chemlab.toogledrain` (sic) "Toggle drained Tank", plus `techguns.chemlab.toggledrain.tooltip.<0/1>`. |
| Upgrade slot frame | (134,46) | copied from u134 v16, 20×20 |
| Progress animation | see below | five stages, each 20% of the operation |

**Progress animation** (prog = progress/total). Each stage reveals part of a lab-glassware illustration printed in the sprite area:

| Stage | prog | Draw at | Sprite (u, v) | Size | Fill |
|---|---|---|---|---|---|
| 1 | 0–0.2 | (81,19) | (178,5) | 8 × (0→25) | funnel drip, top-down |
| 2 | 0.2–0.4 | (88,39) | (186,25) | (0→9) × 1 | connecting pipe, left→right |
| 3 | 0.4–0.6 | (97, 23+20−h) | (194, 9+20−h) | 14 × h, h 0→20 | round flask fills bottom-up |
| 4 | 0.6–0.8 | (108,17) | (205,3) | (0→16) × 6 | distillation tube, left→right |
| 5 | 0.8–1.0 | (117,20) | (214,6) | 14 × (1→25) | receiving Erlenmeyer flask, top-down |

Each finished stage stays fully drawn until the operation ends.

- **Container sync:** 0 progress, 1 totalTime, 2 power (raw), 3 input fluid ID (−1 = none), 4 output fluid ID, 5 input amount, 6 output amount. Amounts are resent every tick while working.
- **NEI.** GUI offset registered (see `nei.md`).

## 6. Sounds and particles (client side only, while working and powered)

- `techguns:machines.chemlabWork` at progress tick 1 and tick 51 (1 + round(half of total)). Volume 1, pitch 1, distance delay on.
- A vanilla `spell` particle every 10 progress ticks at a random point inside the block, with velocity (0,1,0).

## 7. Visuals

TESR `RenderAmmoPress` with `ModelChemLab` (declared 128×64) and texture `textures/blocks/ChemLab.png` (256×128, catalogued in 07-blocks).

- **Look.** A lab bench with a steel frame, grey checker-plate panels, glassware and coloured liquid elements. The atlas has blue, cyan, orange, magenta and red liquid swatches.
- **Glass.** 11 glass parts (`G1..G11`) are always rendered with alpha blending.
- **Animation**, while working (progress p > 0):
  - Parts `L4` and `L5` (liquid bits) are visible.
  - **Three test glasses** (`L1`, `L2`, `L3`) start filling at p = 0.2, 0.4 and 0.6. Each fills to 16 layers (1/32 block per layer) following √ of its local progress (scaled ×5 over 20%). All three drain linearly to empty during p 0.8–1.0.
  - **Two flasks:** `L7` drains from 16 layers to 0 during the first third (p 0–0.333). `L8` fills from 0 to 16 layers during the last third (p 0.666–1).
- The model's first three loop blocks compute fills but render nothing (leftover code).
