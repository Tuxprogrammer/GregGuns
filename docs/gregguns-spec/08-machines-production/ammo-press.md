# Ammo Press (reinstated, animated single block)

The Techguns Ammo Press comes back as a GregGuns machine with its own recipe map. It overrides the "Replace" row for the Ammo Press in [0002](../../design/0002-machine-deconfliction.md) (owner decision: ammo is the exception to "use GT machines everywhere"). The multiblock that scales it is the [Ammunition Works](multiblocks.md#ammunition-works).

- **Techguns baseline:** [ammo-press.md](../../techguns-spec/08-machines-production/ammo-press.md), [rendering.md](../../techguns-spec/08-machines-production/rendering.md), [sounds.md](../../techguns-spec/08-machines-production/sounds.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0002](../../design/0002-machine-deconfliction.md) (to be amended, see Open questions), [0003 energy model](../../design/0003-energy-model.md), [0006 art direction](../../design/0006-art-direction.md)
- **Pack mods:** GT5-Unofficial (`gregtech`, 5.09.54.205), StructureLib not needed (single block)
- **Roadmap:** M4

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| RF, 5 RF/t, 100 ticks per plan | GT EU. Recipe `eut` is the ammo's tier voltage. Durations are the ones in [06 recipes](../06-items/recipes.md) | [0003](../../design/0003-energy-model.md) |
| A "build plan" selector with four plans | Plans are ordinary recipes in the machine's own map. A programmed circuit is **not** needed to tell them apart because the map is separate. NEI lists every recipe | The map is ours, so no other recipe competes for the inputs |
| Fixed four slots | GT-style slots: up to 6 item inputs, 1 fluid, 3 outputs ([map definition below](#the-recipe-map)) | GT machine UI |
| Stack upgrade item | Dropped. Throughput comes from tier, overclock and the Ammunition Works | No GT counterpart ([recipe-map-replacements.md](recipe-map-replacements.md#what-changes-from-techguns)) |
| Static TESR block | Animated: ram stroke, feed belt, ejected rounds | Owner decision, immersion |
| One machine | Tiers Steam, LV, MV, HV, EV, IDs 20100 to 20104 | Recipes span ULV to EV |

## Tiers and what each runs

| Tier | MTE ID | Base class | Runs |
|---|---|---|---|
| Steam (Bronze) | 20100 | `MTEBasicMachineBronze` subclass, as GT's steam machines ([MTESteamCompressorBronze#L31](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/steam/MTESteamCompressorBronze.java#L31)) | Every recipe up to LV, steam-tagged or not: the Stone Bullet (ULV, moved here from shapeless crafting, see below) and the LV rows such as the 9mm bullet. Bronze runs them at half speed (see Steam tier built notes) |
| LV | 20101 | `MTEBasicMachineWithRecipe` subclass | 9mm, shotgun shell, rifle bullet, 9mm pistol magazines |
| MV | 20102 | same | 40 mm grenade, SMG and assault-rifle magazines |
| HV | 20103 | same | High-caliber round, rocket, AS50 and LMG magazines |
| EV | 20104 | same | Advanced bullet, advanced magazine, minigun drum |

A machine runs any recipe at or below its tier voltage, as every GT machine does. Higher tiers overclock and so finish sooner. LuV and ZPM ammo (Nether Charge, tactical nuke rocket) stays in the Assembly Line ([02 recipes](../02-guns/recipes.md)); it is single craft, one-off items.

## The recipe map

```java
public static final RecipeMap<RecipeMapBackend> ammoPressRecipes = RecipeMapBuilder
    .of("gregguns.ammoPress")
    .maxIO(6, 3, 1, 0)
    .progressBar(AMMO_PRESS_PROGRESS)   // our own stroke-shaped bar, see the UI note
    .build();
```

`RecipeMapBuilder` and the "name must be unique" rule are the same ones used for the Reaction Chamber ([reaction-chamber-multiblock.md](reaction-chamber-multiblock.md#recipe-map-and-the-7-techguns-recipes)). The map is registered once and read by both the single blocks and the Ammunition Works. NEI makes one tab per map with catalysts from `MTE#getRecipeMap` ([nei-handlers.md](nei-handlers.md)).

### Recipe move (applied)

The following rows in [06-items/recipes.md](../06-items/recipes.md) have moved from `assemblerRecipes` to `gregguns.ammoPress` (rows marked `AP`). **Inputs, EU/t and durations are unchanged.** The programmed-circuit numbers are dropped because the map is separate (a circuit may stay as a harmless selector if the lead prefers one-to-one rows). Row IDs may have shifted because that file is being edited, so go by the output names.

| Section of `06-items/recipes.md` | Outputs that move |
|---|---|
| Bullets, shells, rockets | `ammo_9mm_bullet`, `ammo_shotgun_shell`, `ammo_rifle_bullet`, `ammo_high_caliber_round`, `ammo_advanced_bullet`, `ammo_40mm_grenade`, `ammo_rocket` |
| Stone Bullet | `ammo_stone_bullet`: shapeless crafting becomes a Steam (Bronze) Ammo Press recipe at `TierEU.RECIPE_ULV`, 5 s (proposal). The shapeless recipe stays as the no-machine route if the lead wants a hand path |
| Empty magazines | `mag_9mm_pistol_empty`, `mag_smg_empty`, `mag_assault_rifle_empty`, `mag_as50_empty`, `mag_lmg_empty`, `mag_advanced_empty`, `mag_minigun_drum_empty` |
| Filled magazines in bulk | `mag_9mm_pistol`, `mag_smg`, `mag_assault_rifle`, `mag_as50`, `mag_lmg`, `mag_advanced`, `mag_minigun_drum` |

Do not move: the Nether Charge and tactical nuke rocket (Assembly Line), fuel, bio and air tanks (Assembler; they are tanks, not ammo), the energy cell, the crafting-table magazine refills. Requirement [GGS-06-38](../06-items/recipes.md) says GregGuns registers no Ammo Press recipe; it needs the same lead decision and a rewrite.

## Animation

The press must look busy. Three layers, from cheapest to richest. All three are client side and cosmetic. None changes the recipe.

### 1. Active-state overlay textures (all tiers)

How GT does it: `MTEBasicMachine#getTexture` picks one of a set of overlays by side and by the `active` flag it is given, for example front active versus front inactive ([MTEBasicMachine#L290-L300](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachine.java#L290-L300)). `active` is `IGregTechTileEntity#isActive`, which the server syncs when a recipe starts and stops. Animated overlays are ordinary vanilla animated textures: a PNG strip plus a `.png.mcmeta`. GT's Mixer front overlay is the precedent ([OVERLAY_FRONT_ACTIVE.png.mcmeta](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/resources/assets/gregtech/textures/blocks/basicmachines/mixer/OVERLAY_FRONT_ACTIVE.png.mcmeta): `{"animation": {"frametime": 1}}`). GT ships a `press` overlay set of six PNGs (front, side, top, each active and inactive) under `basicmachines/press/`.

GregGuns does:

- A set of overlays per tier, in the same eight-slot layout as `MTEBasicMachine` (front, side, top and bottom, each active and inactive), plus a glow layer for the active front.
- Front active: a press-ram strip, 8 frames. Side active: a belt strip, 4 frames, scrolling. Top active: a hopper with rounds dropping, 6 frames. Glow: the ram head heats orange when it strikes.
- Frame time is a multiple of the stroke length so the strip loops cleanly (see below). Per-tier tint on the casing, not the overlay, as GT does.

### 2. TESR moving parts (all tiers except Steam)

How GT does it: `BaseMetaTileEntity` has a tile-entity special renderer registered once for the whole block ([GTClient#L229](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTClient.java#L229)). It forward to any MTE that implements `IMTERenderer`, passing the world offset and the partial tick ([BaseMetaTileEntityRenderer#L8-L16](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/render/BaseMetaTileEntityRenderer.java#L8-L16), [IMTERenderer](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/render/IMTERenderer.java)). Two worked precedents:

- **A single block that draws itself.** `MTEIceCreamMachine extends MTEBasicMachine implements IMTERenderer`. It returns **no** textures from `getTexture`, so the block cube is invisible, and draws its whole model in `renderTESR` ([MTEIceCreamMachine#L396-L402](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/basic/MTEIceCreamMachine.java#L396-L402), [#L456-L469](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/basic/MTEIceCreamMachine.java#L456-L469)).
- **A part spun by the client from active state.** `MTEIndustrialCuttingMachine` sets `needsClientTick`, counts `activeTicks` up while `isActive` and down when not, ramps a blade rotation, and draws the blade in `renderTESR` with the partial tick for smoothness. It also finishes the spin-down at a clean angle, takes a larger `getRenderBoundingBox` so the part is not culled, picks a texture per tier, and lets a player turn all rendering off with a screwdriver ([class](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialCuttingMachine.java#L74), [client tick](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialCuttingMachine.java#L385-L420), [renderTESR](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialCuttingMachine.java#L485-L552), [toggle](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialCuttingMachine.java#L443), [tier textures](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialCuttingMachine.java#L91-L95)).

GregGuns does (LV and up): the Ammo Press implements `IMTERenderer`, keeps the cube textures from layer 1 (so the block still looks right with rendering off or at a distance), and draws three moving parts on top, driven by recipe progress:

| Part | Motion | Driven by |
|---|---|---|
| **Ram** (a piston with a flat head, on the top face under a hood cutout) | Strokes down, holds one tick at the bottom, rises. Eased with a sine curve. The head flashes the glow texture at the bottom | Stroke phase |
| **Feed belt** (a strip of boxed round models on the front-bottom chute) | Scrolls toward the front at a rate that scales with the tier | Active ticks |
| **Ejected rounds** (a few small round models) | Pop out of the chute on each stroke, arc, and fade at the output tray | Stroke count |

**Synced to recipe progress.** The ram does not just run on a timer. A recipe lasting `D` ticks has `S` strokes where `S = clamp(D / 20, 2, 8)` (ammo recipes are 40 to 200 ticks, so 2 to 8). The stroke phase is `progress / D * S` modulo 1, where `progress` and `D` are the machine's synced progress and maximum progress. `BaseMetaTileEntity#getProgress` and `#getMaxProgress` exist ([BaseMetaTileEntity#L971-L978](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/BaseMetaTileEntity.java#L971-L978)); the basic machine's own GUI progress bar reads the same pair ([MTEBasicMachine#L1580](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachine.java#L1580)). The last stroke lands on the tick the output appears, which makes the output look like it was just pressed. When the machine is idle the ram rests at the top. When it stops mid-recipe (power loss, soft mallet) the phase freezes and the ram settles at the top over 10 ticks.

**Fallback for a client without progress.** Whether the client receives `getProgress` outside an open GUI is **not verified** here; the M4 boot test checks it. If it does not sync, the client uses a free-running `activeTicks` counter exactly as the cutting machine does, with a fixed 20-tick stroke. This is the same animation without the sync.

**Cost control.** `getRenderBoundingBox` is the default 1 x 1 x 1 (the parts stay inside the block). The renderer draws at most 12 small quads per press. A client config `ammoPressAnimation` (default on) and the screwdriver toggle of the Cutting Machine are both honored.

### 3. Steam tier

The Bronze press has no TESR. Layer 1 alone animates it (ram strip, steam puff frames on the top overlay). That matches GT's steam machines, which are animated by overlay only.

### Sound

One mechanical stroke sound on each ram bottom, played from the client at the stroke phase wrap. Sound files are area 03 and 06 work; this sheet only asks for the hook.

## Integration points

### GT5-Unofficial: a machine for a custom recipe map
- **Source:** `MTEBasicMachineWithRecipe` constructs a tiered machine around a `RecipeMap`: slots, auto-output, progress bar, NEI catalyst. GT's own machines call it with `RecipeMaps.<x>` and a tier. Our press calls it with `ammoPressRecipes`. The Steam tier uses `MTEBasicMachineBronze`. Both are in [metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-base-classes-to-extend).
- **GregGuns does:** five instances for the tiers in the table above. Recipe voltage is the gate.
- **Pack-side change:** none. The optional NEI ordering row for `gregguns.ammoPress` is in [PACK-CHANGES](../PACK-CHANGES.md) if the lead adds it.

## M4 tiers deviation

Built in M4 (branch `m4/ammo-press-tiers`): the Steam, MV, HV and EV presses, IDs 20100, 20102, 20103 and 20104, beside the M1 LV press. The Ammunition Works (20105) is a separate branch.

- **One class, tier parameter.** LV to EV are `MTEAmmoPress` with the GT voltage index; all have 6 item inputs, 3 item outputs and 1 fluid input. There is **no parallel** in a single block (throughput is overclock and the Ammunition Works). `AmmoPressTier` (pure) holds the IDs, D1 tiers and the `canRun` rule: a press runs rows at or below its tier, which GT enforces through the recipe EU/t against the machine voltage.
- **Steam press** is `MTEAmmoPressSteam extends MTEBasicMachineBronze`, as GT's Bronze Compressor: steam from any side but the front, and a steam vent through the front after every run (the base class does both). Deviation: **4 item inputs and 1 item output**, not 6 and 3, because the Bronze recipe check fills one output and the Stone Bullet needs few inputs. Tier rule: it runs **every Ammo Press row up to LV**, as every GT steam machine does (`MTEBasicMachineBronze#checkRecipe` queries at `TierEU.LV`; [source](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachineBronze.java#L222-L225)). `AmmoPressTier.STEAM.recipeCeiling()` is LV, so `canRun` and `ProductionTable.pressRowsFor(STEAM)` list the ULV and LV rows; MV and above need an electric press. Steam use is GT's own (`SteamOverclockDescriber(BRONZE, 1, 2)`: EU/t unchanged, duration doubled, 2 L of steam pay 1 EU; the steel variant would be EU/t doubled at normal duration). Not computed here.
- **Steam GUI (fixed).** The press opens GT's bronze steam GUI (steam gauge, input and output slots, progress bar) from the base class. It used to open nothing: `MTEBasicMachineBaseGui` reads `progressBarTextureSteamMUI2.get(variant)` from the recipe map's UI properties, GT's builder leaves that null, and our map set no steam GUI textures, so building the GUI threw a `NullPointerException` on the server. `GregGunsRecipeMaps.AMMO_PRESS` now sets `progressBarTextureSteamMUI2` (GT's steam arrow) and a steam slot overlay getter. Scenarios: `production.ammo_press_steam_gui`, `production.ammo_press_steam_lv`, `mp.steam_press` (screenshots).
- **No High Pressure (steel) steam variant.** GT pairs each bronze machine with a steel one. `machine-roster.md` gives no ID for it, so none is built. Gap: if wanted, the roster needs an ID (the free range is 20125 to 20163).
- **Steam hand recipe** `XXX / PMP / BBB`: small bronze pipes, a piston (`craftingPiston`), `Hull_Bronze`, bronze plates (recipes.md). LV to EV use the `VPV / CMC / WEW` grid from M1, with the tier's hull, circuit, cable, motor, piston and conveyor.
- **Progress-synced front (LV to EV).** The active front overlay strip (`OVERLAY_FRONT_ACTIVE`, 16 x 128, 8 frames, and its glow strip) is not animated by the vanilla timer. The block shows the still front when active; `AmmoPressRenderer` (client) draws one frame of the raw strip on the front face from `AmmoPressAnimation`: `S = clamp(D / 20, 2, 8)` strokes, phase `progress / D * S` modulo 1, frame `floor(phase * 8)`. GT does not send recipe progress to the client outside a GUI, so the press sends `(progress, max, running)` through GT's `IMTERenderer` render-data packet on start, stop and every 10 ticks, and the client counts ticks between samples. With no fresh sample (older than 30 ticks) it falls back to a free-running 20-tick stroke, the spec's fallback. Client config `client.ammoPressAnimation` (default on); off shows the still front. A press whose front faces up or down shows the still front.
- **Not built (needs art or models).** The 3D ram, feed belt and ejected rounds of layer 2, and the stroke sound: no models or sounds exist for them. The front strip is the only synced part. The Steam press uses layer 1 only (vanilla animated overlays), as specified.
- **Recipes.** This branch adds no ammo recipe. `ProductionTable.pressRow(...)` builds a row for any tier and `ProductionTable.pressRowsFor(AmmoPressTier)` lists what a press can run. The Stone Bullet ULV row and the MV, HV and EV ammo rows come from the M2 branch.

## M1 slice deviation

Pulled into M1 for the vertical slice (the 9mm Pistol must be craftable through its D1 gate, and the recipe must show in NEI); animation, the multiblock and the remaining tiers stay M4. What M1 builds:

- The map `gregguns.ammoPress` (`GregGunsRecipeMaps`), built from `GunsModule.preInit`. It has the sheet's `maxIO(6, 3, 1, 0)` and GT's default arrow progress bar; the stroke-shaped bar waits for M4.
- Only the **LV** press, MTE **20101** (`GregGunsMTEIDs.AMMO_PRESS_LV`), class `MTEAmmoPress`, with 6 item inputs, 3 item outputs and one fluid input. It is constructed from `GunsModule.init`, not from `sAfterGTPreload`: GregGuns loads after GregTech, so GT's preload is already over, and construction is still inside the legal window (after preload starts, before postload starts). CropsNH does the same.
- Overlays only: `textures/blocks/machines/ammo_press/OVERLAY_*` through `Textures.BlockIcons.customOptional`, in `MTEBasicMachine`'s eight-slot order. There is no TESR, no progress-synced ram and no sound of its own (it borrows GT's compressor loop).
- Its hand recipe is the sheet's `VPV / CMC / WEW` grid at LV, passed to GT's machine-recipe helper.
- Recipes on the map: RC-28 (9mm bullet), RC-37 (empty pistol magazine), RC-44 (loaded pistol magazine). **Deviation:** no programmed circuit, because the map is separate (the option this sheet's recipe-move section allows). RC-05 and RC-11 go on GT's Assembler with `circuit(1)` as written. The RC-51 refill is a shapeless crafting recipe.
- The bullet is exempt from the "one input of the tier's gate material" rule (GGS-06-36). The exemption list is `ProductionTable.GATE_EXEMPT` and is fixed.

## Requirements

Continue from [machine-roster.md](machine-roster.md#requirements).

- **GGS-08-53** The Ammo Press exists as five MTEs, IDs 20100-20104, Steam to EV. The Steam tier uses the Bronze base class.
- **GGS-08-54** Every ammo and magazine recipe lives in `gregguns.ammoPress`. No ammo recipe stays in `assemblerRecipes` after the move listed above.
- **GGS-08-55** The Ammo Press has the animation of "Animation" layers 1 and 2: active-state overlays, a ram synced to recipe progress, a feed belt and ejected rounds. The Steam tier has layer 1 only.
- **GGS-08-56** The animation is client only, reads only synced progress and active state, and never changes a recipe or an inventory. A client config switch turns it off.

## Tests

| Req | Verification |
|---|---|
| 53 | Unit test over the MTE registry (IDs, base classes) |
| 54 | Unit test: after `GregGunsRecipes` runs, no recipe with an ammo-chain output exists in `assemblerRecipes`; all are in the ammo map |
| 55 | Manual client check: run a recipe at each tier. The ram bottoms out at the moment the output appears. Pull power and check it settles |
| 56 | Dedicated-server boot: no client class loaded (`runServer`); manual toggle of the config |

## Open questions

- **0002 amendment.** 0002's Ammo Press row needs to change from Replace to Keep, and its "Alternatives" no longer fits. The lead records this.
- **Stone Bullet.** Does the shapeless hand recipe stay beside the Steam press recipe?
- **Texture work.** The overlays and the three models are area 03/07 art; they need a spec in [0006](../../design/0006-art-direction.md).
