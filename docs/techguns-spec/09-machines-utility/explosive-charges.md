# Explosive charges (TNT charge and advanced / C4 charge)

| | TNT charge | Advanced charge (C4) |
|---|---|---|
| [`BasicMachine`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) meta | 6 | 8 |
| Name (`techguns.tile.BasicMachine_N.name`) | "Explosive Charge" | "Advanced Explosive Charge" |
| Tile entity (registry id) | `ExplosiveChargeTileEnt` | [`ExplosiveChargeAdvTileEnt`](index.md#key-facts) (subclass) |
| GUI id | 6 | 6 (same GUI class) |
| TESR | `RenderExplosiveCharge` + [`ModelTNTCharge`](../07-blocks/block-rendering-and-models.md#modeltntcharge-uv-3232-textures-tntchargepng--tntcharge_2png) | `RenderExplosiveCharge` + [`ModelExpCharge`](../07-blocks/block-rendering-and-models.md#modelexpcharge-uv-3232-textures-expchargepng--expcharge_2png) |
| Recipe | ×8: `rtr`/`tct`/`rtr`; r = `itemRubber`, t = TNT, c = `circuitBasic` | ×4: same grid; r = `sheetPlastic`, t = TGX ([06](../06-items/index.md)), c = `circuitBasic` |

## Concept

A charge is a small plate-shaped block stuck onto the face of another block. The player configures it in a GUI, then arms it.

When the fuse runs out, it **bores a rectangular tunnel into the block it is attached to**, along the direction from the charge into that block:

- Every block in the tunnel is broken and **dropped as items**, like normal harvesting.
- No vanilla explosion takes place: entities are not damaged and there is no fire or crater.
- The charge removes itself.

## Placement and shape

- [`ItemBlockMetaDataMachine`](../07-blocks/special-blocks.md#16-itemblockmetadatamachine) stores the clicked face (0–5) as `orientation` and plays the charge's **plant** sound on the server: volume 4.0, pitch 1.0, at the block centre.
- The owner is set to the placing player ([`BasicOwnedTileEnt`](../08-machines-production/framework.md#3-ownership-and-security-basicownedtileent-itgtileentsecurity)).

**Blast direction** (`getDirection`): the charge always blasts *into* the block it is mounted on.

| Orientation (clicked face) | Charge sits… | Blast direction |
|---|---|---|
| 0 (bottom face clicked) | under a ceiling | +Y (UP) |
| 1 (top face) | on a floor | −Y (DOWN) |
| 2 (north face) | on a north-facing wall | +Z (SOUTH) |
| 3 (south face) | | −Z (NORTH) |
| 4 (west face) | | +X (EAST) |
| 5 (east face) | | −X (WEST) |

**Selection and collision box** (`BasicMachine.setBlockBoundsBasedOnState`), with inset w = 0.2 and thickness h = 0.3:

- The box is a 0.6 × 0.6 plate, 0.3 thick, flush against the mounting face.
- For example, orientation 1 gives (0.2, 0, 0.2)–(0.8, 0.3, 0.8), and orientation 0 gives (0.2, 0.7, 0.2)–(0.8, 1, 0.8).
- Hardness is 0.1 and the block is harvestable by hand. Breaking it drops the charge item unarmed, which is an effective way to defuse it.
- Right-clicking an **armed** charge does nothing. Wrenches are ignored.

## Parameters

| Parameter | TNT (min / default / max) | C4 (min / default / max) | GUI step |
|---|---|---|---|
| `blastradius` (cross-section size) | 3 / 3 / 3 (fixed; the buttons do nothing) | 3 / 5 / 7 | ±1 |
| `blastlength` (depth in blocks) | 2 / 3 / 5 | 2 / 10 / 10 | ±1 |
| `fusetime` (ticks) | 60 / 100 / 200 | 60 / 100 / 200 | ±20 (1 s) |
| Max breakable hardness | hardness ≥ 0 and **<** [`ExplosiveChargeMaxHardness`](../01-core/config.md#category-general) (default 30.0) | hardness ≥ 0 and **≤** [`ExplosiveChargeAdvancedMaxHardness`](../01-core/config.md#category-general) (default 100.0) | — |

Both config keys are in the general category, range 0 to Float.MAX. The comment notes obsidian is 50, so the TNT charge cannot break obsidian but C4 can. Negative hardness (bedrock and other unbreakables) is never broken.

**Cross-section from radius:** half-size k = blastradius ÷ 2 (integer), giving a (2k+1)² square. When blastradius is **even**, the four corner columns are dropped (a "rounded" square).

| blastradius | Cross-section |
|---|---|
| 3 | 3×3 |
| 4 | 5×5 without corners (21 columns) |
| 5 | 5×5 |
| 6 | 7×7 without corners (45 columns) |
| 7 | 7×7 |

## Arming and fuse (`ExplosiveChargeTileEnt`)

**Buttons** (`buttonClicked`; all are ignored once armed):

| Id | Action |
|---|---|
| 0 | **Arm.** Requires the usable check (same tile, within 8 blocks). Sets `armed`, plays the **init** sound (volume 4, pitch 1) and syncs. |
| 1 / 2 | radius +1 / −1, clamped. Syncs. |
| 3 / 4 | length +1 / −1, clamped |
| 5 / 6 | fuse +20 / −20 ticks, if currently below max / above min |

Ids 1–6 skip the usable check ([MUTL-31](index.md#suspected-bugs)).

**Every tick while armed** (this runs on both client and server; the client copy only spawns particles):

1. `fusetime` − 1.
2. Server: when `fusetime` > 0 and divisible by 20, play the **tick** sound (volume 4, pitch 1), so it beeps once per second.
3. When `fusetime` ≤ 0, explode.

## Explosion algorithm

1. Server: play vanilla `random.explode` at the charge, volume 4.0, pitch (1 + (r1 − r2) × 0.2) × 0.7 with random r1 and r2.
2. Build the cross-section: the set of columns in the plane through the **charge's own position**, perpendicular to the blast direction, sized and rounded as above.
3. For each column, walk up to `blastlength` steps along the blast direction. The first step is the block immediately beyond the charge's plane, which is the mounting block for the centre column. At each step:
   1. If the block's hardness is breakable for this charge type, post a Forge `BlockEvent.BreakEvent`. The event uses the block and metadata, with the **owner player** looked up by UUID; that player can be null ([MUTL-30](index.md#suspected-bugs)).
   2. If the event is not cancelled:
      - Server: drop the block's items (fortune 0) and set it to air.
      - Client: with 50% chance, spawn one "LargeSmoke" and one "Explosion" particle at a random point in the block, rising at 0.045 (particle definitions in [04](../04-projectiles-fx/index.md)).
      - Continue the column.
   3. If the hardness is too high or the event is cancelled, **stop this column**. The column is blocked, and blocks beyond it survive.
   4. Air counts as breakable and does not stop the column.
4. Server: set the charge's own block to air.

Each column is independent, so the tunnel follows a ragged face where hard blocks interrupt individual columns.

## NBT (`ExplosiveChargeTileEnt`)

| Source | Keys |
|---|---|
| Base | `Items` (always empty; 0 slots), `rotation`, `CustomName`, `security`/`owner` (from `BasicOwnedTileEnt`) |
| Charge | `orientation` (byte), `blastradius`, `blastlength`, `fusetime` (shorts), `armed` (bool) |

Other details:

- Inventory name: `techguns.inventory.explosivecharge` = "Explosive Charge", used as the GUI title for both types.
- Automation: none.

## Container and GUI

[`ExplosiveChargeContainer`](../08-machines-production/gui-handler.md#1-techgunsguihandler-complete-gui-id-table) has **no slots**. It permits interaction while the player passes the usable check, and shift-click returns nothing.

[`GuiExplosiveCharge`](../08-machines-production/gui-textures.md#10-texturesguiexpchargeguipng-owned-by-09) extends [`GuiTechgunsTileEntBase`](../08-machines-production/gui-handler.md#2-guitechgunstileentbase-abstract-guicontainer). Size is the default 176 × 166.

| Charge | Background (drawn (0,0) 176×166) | Look |
|---|---|---|
| TNT | [`textures/gui/tntChargeGui.png`](../08-machines-production/gui-textures.md#11-texturesguitntchargeguipng-owned-by-09) | Three vertical red dynamite sticks with dark shading between them, black rounded border. A light-grey bevelled display window (≈(28,19)–(148,100)) holds a flat grey screen. |
| C4 | [`textures/gui/expChargeGui.png`](../08-machines-production/gui-textures.md#10-texturesguiexpchargeguipng-owned-by-09) | A dark olive-green C4 block crossed by grey duct-tape bands: two vertical bands, plus horizontal tape at the left and right edges. Same grey display window. |

Both files are 256 × 256 and catalogued by [08](../08-machines-production/index.md); only the 176 × 166 panel is used.

**Text** (colour 0x404040; hardcoded English except the title):

| Position | Text |
|---|---|
| centred, y = 28 | title |
| (37, 40) | `Radius: N` |
| (37, 50) | `Length: N` |
| (37, 60) | `Delay: Ns`, where N = fusetime ÷ 20, integer |

**Buttons** (`GuiButtonExt`; each sends [`GUIButtonClick`](../01-core/networking.md#0--guibuttonclick-cs)):

| Id | Label | Rect |
|---|---|---|
| 0 | `Detonate` | (57, 73, 60, 20); clicking it also closes the GUI immediately on the client |
| 1 / 2 | `+` / `-` | (120, 40) / (130, 40), 10×10 |
| 3 / 4 | `+` / `-` | (120, 50) / (130, 50) |
| 5 / 6 | `+` / `-` | (120, 60) / (130, 60) |

## World rendering (`RenderExplosiveCharge`)

1. Bind texture 1 (unarmed) or texture 2 (armed).
2. Apply standard block lighting.
3. Flip 180° about Z and offset (−0.5, −1.5, 0.5), the usual model setup.
4. Orient by `orientation`:

   | Orientation | Rotation |
   |---|---|
   | 2 | 180° yaw |
   | 3 | 0° yaw |
   | 4 | 90° yaw |
   | 5 | 270° yaw |
   | 0 (ceiling) | −90° about X, then shift (0, −1, 1) |
   | 1 (floor) | +90° about X, then shift (0, −1, −1) |

   At yaw 0 the model's back faces the wall it is stuck to; for floor and ceiling the model is tipped flat against the face.
5. Render the model at 1/16 scale.

### Models (32 × 32 texture units on 64 px textures, 2 px per unit)

**`ModelTNTCharge`:**

| Part | Box | Placement | UV |
|---|---|---|---|
| Three dynamite sticks | 4×12×4 each | standing side by side, each turned 45° about its vertical axis (diamond cross-section), spaced ≈4 units apart | (0,0) |
| Detonator | 8×4×2 | strapped across the middle of the sticks, at the front | (0,17) |

**`ModelExpCharge`:**

| Part | Box | Placement | UV |
|---|---|---|---|
| Plastic-explosive brick | 10×10×5 | flat against the wall | (0,0) |
| Detonator | 8×4×2 | the same box as the TNT model | (0,17) |

### Textures (64 × 64, no animation)

| File | Depicts |
|---|---|
| [`textures/blocks/tntCharge.png`](../07-blocks/textures.md#11-model-textures) | Sticks region (0,0)–(16,16 units): dark-red dynamite paper with darker vertical shading lines; the stick tops are darker red. Detonator region at (0,17): dark grey brushed metal, with a small black LCD on its front face reading **"RDY"** in green pixel letters. |
| [`textures/blocks/tntCharge_2.png`](../07-blocks/textures.md#11-model-textures) | Identical, except the LCD reads **"ARM"** in red. Used while armed. |
| [`textures/blocks/ExpCharge.png`](../07-blocks/textures.md#11-model-textures) | Brick region: dark olive / grey-green plastic with lighter blue-grey grid lines (tape wraps); the top and edges are slightly darker. Detonator: as the TNT charge, plus a dark-blue rectangular panel on its side; LCD **"RDY"** green. |
| [`textures/blocks/ExpCharge_2.png`](../07-blocks/textures.md#11-model-textures) | Identical, but the LCD reads **"ARM"** in red. |

The item form uses texture 1 of each type, through [`RenderBlockModelItemTNTCharge`](shared-rendering.md#item-rendering-inventory-and-hand) (transforms in shared-rendering.md).

## Sounds

All are played with `World.playSoundEffect` on the server.

| Event (`sounds.json`, category block) | File | When |
|---|---|---|
| `techguns:tntcharge.plant` | [`sounds/effects/DemoCharge_Plant.ogg`](../04-projectiles-fx/sounds.md) | TNT charge placed (vol 4, pitch 1) |
| `techguns:tntcharge.init` | [`sounds/effects/DemoCharge_Init.ogg`](../04-projectiles-fx/sounds.md) | TNT charge armed (Detonate pressed) |
| `techguns:tntcharge.tick` | [`sounds/effects/DemoCharge_Tick.ogg`](../04-projectiles-fx/sounds.md) | once per second while the TNT fuse burns |
| `techguns:c4charge.plant` | [`sounds/effects/C4Charge_Plant.ogg`](../04-projectiles-fx/sounds.md) | C4 placed |
| `techguns:c4charge.init` | [`sounds/effects/C4Charge_Init.ogg`](../04-projectiles-fx/sounds.md) | C4 armed |
| `techguns:c4charge.tick` | [`sounds/effects/C4Charge_Tick.ogg`](../04-projectiles-fx/sounds.md) | once per second while the C4 fuse burns |
| vanilla `random.explode` | — | detonation (vol 4, pitch ≈ 0.7 ± 0.14) |
