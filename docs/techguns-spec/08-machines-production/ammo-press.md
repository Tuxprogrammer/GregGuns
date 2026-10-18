# Ammo Press (`basicMachine:0`)

The Ammo Press turns 1 "metal 1" + 2 "metal 2" + 1 gunpowder into a batch of ammo. A build-plan selector picks which ammo type.

Classes: [`AmmoPressTileEnt`](framework.md#1-class-hierarchy) (extends [`BasicMachineTileEntity`](framework.md#7-recipe-machine-state-basicmachinetileentity)), `AmmoPressContainer`, `GuiAmmoPress`, `AmmoPressBuildPlans` (the live recipe logic), `AmmoPressRecipe` (legacy, never used).

## 1. Inventory

| Slot | Role | GUI pos | Container rule | Sided access |
|---|---|---|---|---|
| 0 | Metal 1 (casing) | (100,17) | plain `Slot` | insert if in metal1 list; not extractable |
| 1 | Metal 2 (bullet; needs 2) | (120,17) | plain `Slot` | insert if in metal2 list |
| 2 | Powder | (140,17) | plain `Slot` | insert if in powder list |
| 3 | Output | (120,60) | `SlotOutput` | extract only |
| 4 | Upgrade | (152,60) | `SlotMachineUpgrade` | not exposed |

- Accessible slots from every side: 0, 1, 2, 3. Insertion is allowed if `isItemValidForSlot` (= `AmmoPressBuildPlans.isValidFor`). Extraction only from slot 3.
- Capacity: 20 000 RF.

## 2. Input lists (`AmmoPressBuildPlans.init`, called from `TGMachineRecipes`)

Built once at recipe init from ore dictionary names, holding every registered ore stack for each name.

| List | Ore names (default) | Stored size |
|---|---|---|
| metal1 | [`ingotSteel`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems), [`ingotLead`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | reference to the ore stack |
| metal2 | [`ingotCopper`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems), [`ingotTin`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems), `ingotIron`, [`ingotBronze`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | copy with stackSize 2 (display only) |
| powder | `itemGunpowder` | reference |

Validity matching uses Forge `itemMatches` (wildcard-tolerant). MineTweaker can add or remove entries (see `minetweaker.md`).

## 3. Build plans and output

`buildPlan` is a byte from 0 to 3 (`maxPlan = 3`), NBT `BuildPlan`.

| Plan | Output (shared item) | Amount per unit |
|---|---|---|
| 0 | 9mm Bullets (meta 1) | 12 |
| 1 | Shotgun Shell (meta 4) | 16 |
| 2 | Rifle Bullets (meta 7) | 8 |
| 3 | High Caliber Rounds (meta 19) | 4 |

- **Recipe check (`getOutputFor`).** The recipe matches when all three slots hold valid items, slot 0 has ≥1, slot 1 has ≥2 and slot 2 has ≥1. Any metal combination gives the same result; only the plan decides the output.
- **Cost per unit:** 1 metal1, 2 metal2, 1 powder. **Time:** 100 ticks. **Power:** 5 RF/t × multiplier (500 RF per unit).
- **Stack multiplier.** Max = upgrades + 1. Try m from the max down to 2 and take the first m where:
  - output × m fits in slot 3, and
  - slot 0 ≥ m, slot 1 ≥ 2m, slot 2 ≥ m.

  Otherwise m = 1. Inputs are consumed at the start.
- The output is fixed when the operation starts. Changing the plan mid-operation doesn't affect it.

## 4. Processing loop

This runs on both sides; the output is only created server-side. Each tick, if redstone allows:

- **Operation running:**
  1. Consume 5·m RF.
  2. On success, progress++ and run the sound check.
  3. When progress ≥ 100: add output×m to slot 3 (server), reset, try to start the next operation, then `needUpdate()`.
- **Idle:** if slots 0–2 are all non-empty, try to start an operation.

## 5. GUI (`GuiAmmoPress`, texture `textures/gui/ammoPress_GUI.png`)

- **Title.** The translated `techguns.container.ammopress` ("Ammo Press"), centred at y 6. "Inventory" (vanilla `container.inventory`) at (8, 72).
- **Plan text.** The literal string "Building:" at (20,30), not localized, then the translated name of the plan's ammo item at (20,40).
- **Buttons.**
  - ID 3 `>` at (40,50), 20×20: next plan.
  - ID 4 `<` at (20,50), 20×20: previous plan.

  The plan wraps at both ends (3→0 and 0→3). Pressing either requires `isUseableByPlayer`.
- **Upgrade slot frame.** A 20×20 copy of the output-slot frame (u119 v59) drawn at (151,59).
- **Progress.** While working, the funnel sprite u176 v0, 19 wide × (progress·21/total + 1) tall, is drawn at (119,36). It fills **top-down** between the input row and the output slot.
- The power bar, redstone and security controls come from the base GUI (`gui-handler.md`).
- **Container sync (progress bars):** 0 = progress, 1 = totalTime, 2 = buildPlan, 3 = power (raw RF).
- **Shift-click.**
  - From machine slots 0–3: merge into slots 3–38, reverse order.
  - From slots 4–38: route to the valid machine slot (0, 1, 2, or 4 for upgrades).
  - The ranges are off by one ([MPRD-20](index.md#suspected-bugs)).

## 6. Sounds

Both play client and server side, with the "distance delay" flag on, at the machine position, volume 1, pitch 1. For a 100-tick operation:

| Event | When (progress tick) |
|---|---|
| `techguns:machines.ammopressWork1` | tick 5 and tick 55 (round(5% of total), and that + round(half of total)) |
| `techguns:machines.ammopressWork2` | tick 30 and tick 80 (round(30% of total) and that + half) |

## 7. Visuals

TESR [`RenderAmmoPress`](rendering.md#2-renderammopress-generic-model--rotation--progress-tesr) with [`ModelAmmoPress`](../07-blocks/block-rendering-and-models.md#modelammopress-uv-12864-texture-ammopresspng) (texture size declared as 128×64) and texture [`textures/blocks/AmmoPress.png`](../07-blocks/textures.md#11-model-textures) (256×128, catalogued in [07-blocks](../07-blocks/index.md)). The rendering transform is in `rendering.md`.

- **Look.** A grey-and-silver steel workbench press: a boxy frame with two horizontal side punches (left `pa1/pa2`, right `pb1/pb2`), a top stamp (`pc1`), a small metal blank (`MetalPiece`) and three cartridge shapes (`bullet1..3`). The texture includes rust-grey panels, small green accents and a yellow/brass cartridge strip (bottom-right of the atlas).
- **Animation.** Driven by p = progress/totalTime. It runs **twice per operation**; each half is remapped to 0..1 (call it q):

| q range | Visible extras | Motion |
|---|---|---|
| 0–0.25 | metal blank | side punches slide inward by 0.125·√(4q) blocks (left −x, right +x) |
| 0.25–0.66 | three cartridges | side punches ease back on a sine curve |
| 0.66–1.0 | cartridges until q 0.9 | the top stamp drops by 0.09375·c blocks, c = 0.5+sin((√p′+0.75)·2π)/2 where p′ = min(1, (q−0.66)·3) |

  When idle, all punches sit at rest and no blank or cartridges are shown.
- **Item form.** [`RenderBlockModelItem`](../09-machines-utility/shared-rendering.md#item-rendering-inventory-and-hand) with the same model and texture (`rendering.md`).

## 8. Entity sheet summary (consistency pass)

The numbered fields follow `CONVENTIONS.md`. Detail lives in the sections above; this table collects what the sections don't state in one place.

| # | Field | Contents |
|---|---|---|
| 1 | Identity | Block [`basicMachine`](../07-blocks/block-catalogue.md#registered-blocks) meta 0; item class [`ItemBlockMetaDataMachine`](../07-blocks/special-blocks.md#16-itemblockmetadatamachine); tile `AmmoPressTileEnt` (registered under that name); item name key `techguns.tile.BasicMachine_0.name`, English **Ammo Press**; container `AmmoPressContainer`, GUI ID 0; creative tab Techguns; oredict names none |
| 2 | Obtaining | Shaped ore recipe, `ili / cec / iri` (i `ingotIron`, l `ingotLead`, c `ingotCopper`, e Electric Engine, r redstone); full table in [blocks-and-items.md](blocks-and-items.md#crafting-recipes-for-the-machines-in-this-area). Not found in loot or worldgen |
| 3 | Numbers | Buffer 20 000 RF; 5 RF/t times the stack multiplier; 100 ticks per operation; multiplier 1 to 8 (upgrades + 1); build plans 0 to 3; block hardness 4.0, resistance 20.0, pickaxe level 1 |
| 4 | Behaviour | Sections 3 and 4. Runs on both sides (client prediction), output created on the server only |
| 5 | Persistence and sync | NBT `BuildPlan` (byte), plus the base keys `Items`, `rotation`, `owner`, `security`, `Energy`, `hasSignal`, `redstoneBehaviour`, `CurrentOperation`, `Progress`, `TotalTime`, `StackMultiplier` (see [framework.md](framework.md)). Full tile NBT goes to the client on every `needUpdate()`. GUI progress-bar fields: 0 progress, 1 totalTime, 2 buildPlan, 3 power (raw RF). Wrench-dismantle stores the tile NBT in the item under `TileEntityData` |
| 6 | UI | Layout in section 5. Item tooltip: one line, `techguns.block.hasTileEntityData` ("Has TileEntity Data"), only when the stack carries `TileEntityData`; otherwise none. In-GUI tooltips: power ("Power: n/max RF"), redstone and security, exact strings in [lang.md](lang.md#3-shared-machine-gui). Chat: `techguns.container.security.denied` on a denied right-click |
| 7 | Visuals | Block render type −1 (TESR only), so no per-face block texture; the break-particle icon is `techguns:screen_top` ([`textures/blocks/screen_top.png`](../07-blocks/textures.md#6-radar-screen-and-control-panel), 32×32, average #534540). TESR `RenderAmmoPress` + `ModelAmmoPress`; atlas `textures/blocks/AmmoPress.png`, 256×128, average #292724, dominant #101010 66 %, #505050 5 %, #909090 3 %, 100 % opaque. Item renderer [`RenderMachineBlockItem`](rendering.md#5-machine-item-rendering) delegate 0 (`RenderBlockModelItem`, same model and texture). Particles: none |
| 8 | Audio | `machines.ammopressWork1` (volume 1, pitch 1; ticks 5 and 55; clips 2.57 / 2.77 / 2.06 s, 44100 Hz mono) and `machines.ammopressWork2` (volume 1, pitch 1; ticks 30 and 80; 1.06 / 1.35 / 1.14 s, 44100 Hz mono). Details in [sounds.md](sounds.md#5-per-event-call-parameters-volume-pitch-side-exact-condition) |
| 9 | Relationships | Reads `AmmoPressBuildPlans` lists built by [`TGMachineRecipes`](index.md#tgmachinerecipesaddrecipes-what-it-registers); ammo and casings come from [`06-items`](../06-items/index.md); NEI handler in [nei.md](nei.md); MineTweaker has no Ammo Press hook ([minetweaker.md](minetweaker.md)) |
| 10 | Bugs | [MPRD-20](index.md#suspected-bugs) shift-click ranges; [MPRD-30](index.md#suspected-bugs) NEI duplicates; [MPRD-31](index.md#suspected-bugs) hard-coded "Building:"; [MPRD-33](index.md#suspected-bugs) legacy `AmmoPressRecipe`; wrench and power bugs shared with all machines ([MPRD-09](index.md#suspected-bugs)) |
