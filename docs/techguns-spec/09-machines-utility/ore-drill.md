# Ore Drill (multiblock)

The Ore Drill is a configurable, axis-aligned multiblock. It is built in a line from a **controller** towards an **ore cluster** block (the cluster blocks are owned by [07-blocks](../07-blocks/index.md) / [11-worldgen](../11-worldgen/index.md)). It produces that cluster's ore, or fluid, **indefinitely**, without consuming the cluster.

It runs on RF, on furnace fuel, or on liquid fuel (lava, fuel, biofuel). It needs an **ore drill head** item ([06-items](../06-items/index.md)) that matches its size class.

| Class | Role |
|---|---|
| `blocks/machines/OreDrillBlock` | The block, registry name [`oreDrill`](../07-blocks/block-catalogue.md#registered-blocks), metas 0–4. Chisel `ICarvable` with CTM rendering. |
| `blocks/ItemBlockOreDrill` | Item block. Placement restrictions and the cluster helper functions. |
| `tileentities/OreDrillTileEntMaster` | Controller tile (meta 4), registered as `OreDrillTileEntMaster`. Does all the logic. |
| `tileentities/OreDrillTileEntSlave` | Tile for metas 0–3, registered as [`OreDrillTileEntSlave`](index.md#quick-reference). Stores its link to the master. |
| `inventory/OreDrillContainer`, `gui/GuiOreDrill` | GUI id 16 |
| `client/renderer/tileentity/RenderOreDrill`, `client/renderer/block/OreDrillCube` | TESR for the rotating drill head / rod |
| `blocks/machines/OreDrill`, `tileentities/OreDrillTileEnt` | Legacy prototype, unused (§11) |

Shared base classes ([`MultiBlockMasterTileEntBase`](../08-machines-production/framework.md#master-multiblockmastertileentbase), [`MultiBlockSlaveTileEntBase`](../08-machines-production/framework.md#slave-multiblockslavetileentbase), [`BasicMachineTileEntity`](../08-machines-production/framework.md#7-recipe-machine-state-basicmachinetileentity), [`BasicPoweredTileEnt`](../08-machines-production/framework.md#4-energy-basicpoweredtileent)) are documented by 08.

---

## 1. Blocks

`OreDrillBlock`:

- Material iron, **hardness 8**, **resistance 20**, metal sound, pickaxe level 1, Techguns creative tab.
- Drops itself with the same meta.
- `isOpaqueCube` false, `renderAsNormalBlock` false, `isNormalCube` **true** for every meta.
- Render type: Chisel's CTM renderer.
- Creatures cannot spawn on it.
- Chiselling is forbidden.
- The creative list shows the variations with block meta ≤ 4.

Item names come from [`ItemBlockOreDrill`](../07-blocks/special-blocks.md#17-itemblockoredrill-placement-rules): `techguns.tile.oreDrill_<meta>.name`.

| Meta | Lang key → English | Role | Recipe (shaped, ore-dict) |
|---|---|---|---|
| 0 | `techguns.tile.oreDrill_0.name` = "Ore Drill Frame" | **Heavy frame.** Corners of every slice, the whole ring of the first and last slices. | ×8: `pbp`/`bpb`/`pbp`, p = [`plateIron`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo), b = iron bars |
| 1 | `techguns.tile.oreDrill_1.name` = "Ore Drill Scaffold" | **Light frame.** Ring edges of middle slices, and the fill of the last slice. | ×8: `bbb`/`bpb`/`bbb` |
| 2 | `techguns.tile.oreDrill_2.name` = "Ore Drill Rod" | Rod, the centre line between the engines and the cluster | ×4: `ipi`/`ipi`/`ipi`, i = `ingotIron`, p = `plateIron` |
| 3 | `techguns.tile.oreDrill_3.name` = "Ore Drill Engine" | Engine: centre line next to the controller and the inner fill of engine slices | ×4: `bbb`/`beb`/`bbb`, b = iron bars, e = Electric Engine ([06](../06-items/index.md)) |
| 4 | `techguns.tile.oreDrill_4.name` = "Ore Drill Controller" | Controller / master | ×1: `pcp`/`cec`/`pcp`, p = `plateIron`, c = `circuitBasic`, e = Electric Engine |

These lang keys are present but unused (stale): `techguns.tile.oreDrill.name` = "Ore Drill" and `techguns.tile.OreDrillBlock_0..3.name` = "Ore Drill Heavy Frame / Light Frame / Rod / Engine".

### 1.1 Placement restrictions (`ItemBlockOreDrill.placeBlockAt`)

**Rod (meta 2):**

1. Look for an ore cluster among the six neighbours, in the order up, down, −Z, +X, +Z, −X. The candidates are any block in [`TGBlocks.oreclusters`](../07-blocks/block-catalogue.md#registered-blocks), any meta. Only the first neighbour found is used.
2. **If a cluster is adjacent:** flood-fill through connected cluster blocks of the same block and meta, 6-connectivity. If any block of that cluster already touches a rod (meta 2), placement fails. The client gets chat `techguns.msg.error.alreadyrod` = "Can't place here, this cluster already has a connected drill rod". Otherwise the rod is placed. The result is **one rod per cluster**.
3. **If no cluster is adjacent:** the rod may be placed only next to another rod. Otherwise placement fails **silently**.

**Controller (meta 4):**

- It cannot be placed next to another controller. The client gets chat via the vanilla `I18n.format("msg.error.oredrill.controllernext")`. The lang entry is prefixed `techguns.`, so the raw key is shown ([MUTL-20](index.md#suspected-bugs)).
- When placed, the controller's owner is set to the placing player (`OreDrillBlock.onBlockPlacedBy`).

**Metas 0, 1, 3:** no restrictions.

### 1.2 Block textures and face usage

The textures are catalogued by **[07-blocks](../07-blocks/index.md)**. The CTM variations are registered in `TGBlocks`; each meta maps to a texture base name.

| Meta (state) | Texture base | Files | Faces |
|---|---|---|---|
| 0 unformed | `drillFrame_outer` | [`textures/blocks/drillFrame_outer.png`](../07-blocks/textures.md#ore-drill-oredrill-08) (32²) | all six: rusty riveted steel plate with a recessed square centre (brown-red rust, grey) |
| 1 unformed | `drillFrame` | [`textures/blocks/drillFrame.png`](../07-blocks/textures.md#ore-drill-oredrill-08) (32²) | all: grey steel X-brace on a transparent background (see-through) |
| 2 unformed | `drillRod` (Chisel vertical-CTM) | [`textures/blocks/drillRod-ctmv.png`](../07-blocks/textures.md#ore-drill-oredrill-08) (64²), [`textures/blocks/drillRod-top.png`](../07-blocks/textures.md#ore-drill-oredrill-08) (32²) | sides: ribbed chrome pipe that connects vertically. Ends: the round flange with a cross hub. |
| 3 unformed | `drillEngine` | [`textures/blocks/drillEngine.png`](../07-blocks/textures.md#ore-drill-oredrill-08) (32²) | all: dark machinery panel with a gear housing, pipes and one green LED |
| 4 (always) | `drillEngineController` | [`textures/blocks/drillEngineController.png`](../07-blocks/textures.md#ore-drill-oredrill-08) (32²) | all: the same machinery frame with a riveted centre hatch and four coloured corner LEDs (red, green, yellow, blue) |
| 0 formed (meta + 8 = 8) | `drillFrame_outer_c` (CTM) | [`textures/blocks/drillFrame_outer_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillFrame_outer_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08) (64²) | connected rusty frame plates that merge into a continuous riveted border |
| 1 formed (9) | `drillFrame_c` (CTM) | [`textures/blocks/drillFrame_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillFrame_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | connected X-brace scaffold |
| 2 formed (10) | `invisible` | [`textures/blocks/invisible.png`](../07-blocks/textures.md#4-camo-nets-sandbags-tent-flag-air-marker-invisible) (16², fully transparent) | rods disappear; the TESR draws the drill instead |
| 3 formed (11) | `drillEngine_c` (CTM) | [`textures/blocks/drillEngine_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillEngine_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | connected engine panels |
| (12) | `drillEngineTop_c` (CTM) | [`textures/blocks/drillEngineTop_c.png`](../07-blocks/textures.md#ore-drill-oredrill-08), [`textures/blocks/drillEngineTop_c-ctm.png`](../07-blocks/textures.md#ore-drill-oredrill-08) | Intended for engine faces that point along the drill axis. The selection code is marked "does not work" in the source, and the CTM path only adds 8, so this variant is effectively unused ([MUTL-18](index.md#suspected-bugs)). |

"Formed" means a slave tile with `hasMaster` true. `getIcon` and `getManager` then add 8 to the meta. The controller (meta 4) is never remapped.

These files exist but nothing references them ([07](../07-blocks/index.md) catalogues them):

- [`textures/blocks/drillFrameCorner.png`](../07-blocks/textures.md#ore-drill-oredrill-08): green X-brace in a frame
- [`textures/blocks/drillRodSide.png`](../07-blocks/textures.md#ore-drill-oredrill-08)
- [`textures/blocks/drillRodTop.png`](../07-blocks/textures.md#ore-drill-oredrill-08)

**Face culling (`shouldSideBeRendered`)**, evaluated for the block whose face is being drawn:

| Condition | Result |
|---|---|
| A formed light-frame (meta 1) block flagged `bottomrow` | no faces, so the fill plate in front of the cluster is hidden and the drill head shows |
| A formed rod (meta 2) | no faces |
| Otherwise, a face between two blocks of different meta | drawn |
| Otherwise, a face against the same block | culled |

---

## 2. Multiblock geometry

The drill is built along one of six axes from the controller C. Let **dir** be the axis code:

| dir | 0 | 1 | 2 | 3 | 4 | 5 |
|---|---|---|---|---|---|---|
| axis | +Y | −Y | +X | −X | +Z | −Z |

Number the positions along the axis p = 1, 2, … from C. The centre line holds

- **E engines** (meta 3) at p = 1..E,
- then **R rods** (meta 2) at p = E+1..E+R,
- then the **ore cluster** at p = E+R+1.

Each position p ≥ 1 is a square **slice** perpendicular to the axis, of side 2r + 1, where **r** is the **radius**, 0 to 4.

### Slice contents (r ≥ 1; the centre is always the centre-line block)

| Slice | Corners (4) | Ring edges (non-corner) | Interior (inside the ring, excluding centre) |
|---|---|---|---|
| p = 1 (first engine slice) | heavy frame (0) | heavy frame (0) | engines (3) |
| p = 2..E (other engine slices) | heavy frame (0) | light frame (1) | engines (3) |
| p = E+1..E+R−1 (rod slices) | heavy frame (0) | light frame (1) | **air** |
| p = E+R (last rod slice, against the cluster) | heavy frame (0) | heavy frame (0) | light frame (1) |

For r = 1 the slice is 3 × 3, so the interior is empty and only the ring matters.

Example, r = 2 (5 × 5), first engine slice:

```
0 0 0 0 0
0 3 3 3 0
0 3 3 3 0      centre = engine
0 3 3 3 0
0 0 0 0 0
```

Middle rod slice, r = 2:

```
0 1 1 1 0
1 . . . 1
1 . R . 1      . = air, R = rod
1 . . . 1
0 1 1 1 0
```

### Size classes and minimum lengths

| Radius r | Footprint | Class (drill head size) | Engines E | Rods R | Extra rule | Error key if violated |
|---|---|---|---|---|---|---|
| 0 | 1×1 | small | exactly 0 | exactly 1 | — | `techguns.msg.error.oredrill.invalid2` = "A drill bigger than 1 rod and 1 engine needs a frame" |
| 1 | 3×3 | medium | ≥ 1 | ≥ 2 | — | `techguns.msg.error.oredrill.invalid2.1` = "A medium drill needs at least 1 engine and a rod of length 2 or more" |
| 2 | 5×5 | medium | ≥ 2 | ≥ 3 | — | `techguns.msg.error.oredrill.invalid2.2` = "A large drill needs at least 2 engine rows and a rod of length 3 or more" |
| 3 | 7×7 | large | ≥ 2 | ≥ 3 | E + R ≥ 7 | same `invalid2.2` text |
| 4 | 9×9 | large | ≥ 2 | ≥ 3 | E + R ≥ 9 | same `invalid2.2` text |

A radius-0 drill is therefore just controller → one rod → cluster.

---

## 3. Formation (right-clicking the controller)

When a player right-clicks the controller **without sneaking** and it is not complete, the formation check below runs on **both sides**. Chat messages are added on the client only. Tiles are linked on the server only.

1. **Axis.** Exactly one of the controller's six neighbours may be a rod or engine. That neighbour gives `dir`. If there are none, or more than one, the result is `techguns.msg.error.oredrill.invalidmulti.dir` = "The main engine block must have exactly 1 neighboring engine or rod".
2. **Centre line.** Walk along `dir`:
   - First, any number of engines.
   - Then one or more rods.
   - It must end on an ore cluster block (any registered cluster, any meta).
   - Anything else gives `techguns.msg.error.oredrill.invalid1` = "There must be engines followed by rods that are connected to a cluster".
   - Rods must come after engines; an engine after a rod fails.
3. **Radius.** At p = 1, test r = 4, 3, 2, 1 in that order. Take the first r for which the slice has heavy-frame corners and ring with an engine interior. If none fits, r = 0. Then apply the minimum-length table above.
4. **Slices.**
   - Check engine slices p = 2..E: heavy corners, light edges, engine interior.
   - Check rod slices p = E+1..E+R−1: heavy corners, light edges, air interior.
   - Check the last slice p = E+R: heavy corners and edges, light interior.
   - The first mismatch gives `techguns.msg.error.oredrill.invalid3` = "Invalid block in slice around  " followed by `(x,y,z) ` of the slice centre.
5. **Success.**
   - Server: link the tiles (below).
   - Client chat: `techguns.msg.oredrill.formed1` + r + " " + `techguns.msg.oredrill.formed2` + E + " " + `techguns.msg.oredrill.formed3` + R. In English: "Ore driller multiblock formed with radius: 2 , extra engine size: 2  and drill size: 3".

### Linking (`createMultiblockDrillerTileentities`, server)

For every slice p = 1..E+R:

1. Choose which blocks to link:
   - In **solid** slices (engine slices and the last slice), every block is linked.
   - In **hollow** slices (middle rod slices), only the ring and the centre rod are linked.
2. For each chosen slave:
   - Set its master position and set `direction = dir`.
   - In the last slice, set `bottomrow = true`.
   - Send it to clients.

Then the master records `dir`, `radius`, `enginesize = E`, `rodsize = R` and `complete = true`. It sets `hasDrill` by checking whether slot 0 holds a valid head for this size class, and syncs.

### Unforming

These events call the master's `unform()`:

- Breaking any linked slave notifies the master.
- Breaking the master itself.

`unform()` scans the box from the controller along `dir` for E + R blocks at radius r. It unlinks every slave in that box (clears the master and refreshes the block so the unformed textures come back). Then it sets `complete = false`.

If an operation finds **no cluster** at the target position (§5), the master sets `complete = false` **without unlinking** its slaves ([MUTL-10](index.md#suspected-bugs)).

### Other interactions

| Action | Result |
|---|---|
| Right-click the complete controller (not sneaking) | Server: if `hasDrill` is false, re-scan (`checkAndStartOperation`). Then open GUI 16. |
| Sneak + right-click the controller | Open GUI 16 even if not formed |
| Right-click any linked slave | Opens the master's GUI |
| Wrench | No special handling; the generic [`BasicMachine`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) wrench logic does not apply to this block |

---

## 4. Drill heads (items owned by 06)

The heads are [`TGItems.sharedItem`](../06-items/item-catalogue.md) metas with slot types `DRILL_SMALL`, `DRILL_MEDIUM` and `DRILL_LARGE`. The head must match the drill's size class. Each head material gives a **head mining level**; the GUI renderer also uses it as `drillType` to pick a texture.

| Size class (radius) | Steel (level 1, drillType 1) | Obsidian Steel (level 2, drillType 2) | Carbon (level 3, drillType 3) |
|---|---|---|---|
| Small (r = 0) | [`oreDrillSmallSteel`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) "Small Ore Drill Head (Steel)" | [`oreDrillSmallObsidianSteel`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) | [`oreDrillSmallCarbon`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) |
| Medium (r = 1–2) | [`oreDrillMediumSteel`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) | [`oreDrillMediumObsidianSteel`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) | [`oreDrillMediumCarbon`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) |
| Large (r = 3–4) | [`oreDrillLargeSteel`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) | [`oreDrillLargeObsidianSteel`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) | [`oreDrillLargeCarbon`](../06-items/item-catalogue.md#1-the-shared-meta-item-techgunstechgunsammo) |

A head of the wrong size, or any other item, counts as **no drill** (level −1). Heads never wear out.

Head recipes and the tooltips `techguns.oreDrillTooltip.size` / `.mininglevel` belong to 06. In outline: small heads are crafted from ingots and plates, and each larger size is made from three heads of the next smaller size.

---

## 5. Operation (`OreDrillTileEntMaster`)

### 5.1 Machine data

- Extends `MultiBlockMasterTileEntBase` → `BasicMachineTileEntity`: **11 slots**, **500 000 RF** buffer.
- Redstone mode, security, owner and the GUI buttons come from `BasicPoweredTileEnt` ([08](../08-machines-production/index.md)).
- `useRangeSquared` = 128 (≈ 11.3 blocks).
- Only the master is an RF receiver (any side) and a fluid handler. Slaves expose nothing.

| Slot | Role | Automation |
|---|---|---|
| 0 | drill head | not exposed |
| 1–9 | output | extract allowed |
| 10 | furnace fuel | insert allowed if the item has a furnace burn time > 0 |

| Tank | Capacity | Accepts |
|---|---|---|
| input | 8 000 mB | Techguns `LAVA`, `FUEL` or `BIOFUEL` ([`TGFluids`](../06-items/fluids.md), owned by [06](../06-items/index.md)/[08](../08-machines-production/index.md)) via `fill`. `canFill` says yes to everything, but `fill` filters. |
| output | 32 000 mB | filled by liquid clusters. `drain` (any side) empties it. |

`getTankInfo` returns [input, output].

### 5.2 Tick logic

Nothing happens unless `complete`, `hasDrill` and the redstone condition all hold.

1. **No current operation:** the server calls `checkAndStartOperation()`.
2. **Operation in progress (progress < totalTime):**
   - Server, if `checkDrill()` passes: try to draw `currentRFperTick` energy (§5.4).
     - On success: progress + 1. If `haspower` was false, set it true and sync.
     - On failure: if `haspower` was true, set it false and sync.
   - Client: if `haspower`, progress + 1 and play the ambient sound (§8). The server also reaches this branch whenever `checkDrill()` fails ([MUTL-11](index.md#suspected-bugs)).
3. **Operation finished (server):**
   - **Item operation:** insert the result into slots 1–9. Any overflow is dropped as an item entity at the controller + 1 Y. Clear the operation and start the next one.
   - **Liquid operation:** pour the fluid stack into the output tank. If the tank is full, the excess is lost ([MUTL-14](index.md#suspected-bugs)). Start the next operation.

`checkDrill()` runs on the server every 20 ticks (world time mod 20):

- If no head was remembered and slot 0 is empty: `hasDrill = false`. The machine then stops until the controller is right-clicked again.
- If slot 0 now differs from the remembered head (by item and damage): reset the operation, progress, totalTime and RF/t, remember the new stack, and fail this tick. A new operation then starts.

### 5.3 Starting an operation (`checkAndStartOperation`)

1. **Target position** = controller + (E + R + 1) × dir. If it is not an ore cluster, `complete = false` and the method stops.
2. **Head level** L_h from slot 0, using the size-class table. If L_h ≤ 0: `hasDrill = false`, `drillType = 0`, sync, stop.
3. Set `hasDrill = true` and `drillType`.
4. **Effective mining level** E_m = L_h + r − L_c, where L_c is the cluster meta's mining level. L_c is defined per cluster type in [07](../07-blocks/index.md)/11. In the default set it is 1 for iron, coal, copper and tin; 2 for gold, redstone, lapis, lead, osmium, silver, nickel, aluminium, platinum, nether quartz and glowstone; 3 for diamond, emerald, uranium, titanium, certus quartz and oil.
5. **If E_m ≥ 0:**
   - Cluster size S_c = flood-fill count of connected blocks with the same cluster block and meta (6-connectivity).
   - Effective size S = min(S_c, E + R). Longer drills can exploit bigger clusters.
   - Output = one of the cluster's ore stacks for that meta (count 1). If the cluster meta is a **liquid** cluster (oil), the operation outputs a copy of its fluid stack instead (1 000 mB of oil by default).
   - **Ores per hour:**
     orePerHour = (3·S + 0.5·S·E_m) × [`oreDrillMultiplierOre`](../01-core/config.md#category-ore-drills) × amountMultiplier[cluster, meta]
   - **Ticks per ore:** totalTime = int(72 000 ÷ orePerHour). The source writes it as 60 ÷ orePerHour × 1 200.
   - **Power:** RF/t = int(80 × orePerHour × max(r, 1) × [`oreDrillMultiplierPower`](../01-core/config.md#category-ore-drills) × powerMultiplier[cluster, meta]).
6. **If E_m < 0 (head too weak):** the drill mines **cobblestone** ×1 per 400 ticks at RF/t = int(240 × `oreDrillMultiplierPower`).
7. Progress = 0, sync.

**Energy per ore** therefore does not depend on rate: it is 5 760 000 × max(r, 1) × powerMultiplier × `oreDrillMultiplierPower` RF, regardless of S, E_m or `oreDrillMultiplierOre`. A larger S or E_m makes the drill faster but not cheaper per ore. A larger radius makes each ore more expensive, and raises E_m by r.

**Worked example.** Iron cluster (L_c = 1, amount 2.0, power 0.15), radius 0 with a small steel head (L_h = 1):

- E_m = 0
- S = min(cluster size, 1) = 1
- orePerHour = 3 × 2.0 = 6, so totalTime = 12 000 ticks (10 min) per ore
- RF/t = 80 × 6 × 1 × 0.15 = 72

Medium drill, r = 2, E = 2, R = 3, carbon medium head (L_h = 3), on a gold cluster (L_c = 2, amount 1.0, power 0.4) of 20 blocks:

- E_m = 3 + 2 − 2 = 3
- S = min(20, 5) = 5
- orePerHour = (15 + 7.5) × 1 = 22.5, so totalTime = 3 200 ticks
- RF/t = int(80 × 22.5 × 2 × 0.4) = 1 440

**Per-cluster multipliers** (config, category "Ore Drills"; defaults):

| [`oreClusterMultipliersAmount`](../01-core/config.md#category-ore-drills) / [`oreClusterMultipliersPower`](../01-core/config.md#category-ore-drills) (cluster block [`oreCluster`](../07-blocks/block-catalogue.md#registered-blocks), metas 0–15) | iron | gold | redstone | coal | lapis | diamond | emerald | copper | tin | lead | osmium | silver | nickel | aluminium | platinum | uranium |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| amount | 2.0 | 1.0 | 1.0 | 1.2 | 1.0 | 0.25 | 0.25 | 2.0 | 2.0 | 1.0 | 1.0 | 1.0 | 1.0 | 1.0 | 0.35 | 0.25 |
| power | 0.15 | 0.4 | 0.3 | 0.25 | 0.3 | 1.0 | 1.0 | 0.1 | 0.1 | 0.2 | 0.3 | 0.35 | 0.3 | 0.2 | 1.0 | 1.0 |

| `…Amount2` / `…Power2` (cluster block [`oreCluster2`](../07-blocks/block-catalogue.md#registered-blocks)) | nether quartz | titanium | certus quartz | oil | glowstone | metas 5–15 |
|---|---|---|---|---|---|---|
| amount | 1.5 | 0.5 | 1.5 | 1.0 | 1.5 | 1.0 |
| power | 0.35 | 0.8 | 0.4 | 0.5 | 0.35 | 1.0 |

The optional MineTweaker clusters [`oreClusterCustom`](../07-blocks/block-catalogue.md#registered-blocks) and [`oreClusterCustom2`](../07-blocks/block-catalogue.md#registered-blocks) use 1.0 for everything. The full cluster catalogue (which ore each meta yields, colours, rarity, depth) belongs to [07](../07-blocks/index.md)/11.

Other config keys used here:

| Key | Default | Range | Meaning |
|---|---|---|---|
| `oreDrillMultiplierOre` | 1.0 | 0.001–1000 | global rate multiplier |
| `oreDrillMultiplierPower` | 1.0 | 0–1000 | global power multiplier |
| [`oreDrillFuelMultiplier`](../01-core/config.md#category-ore-drills) | 1000 | 1–100 000 | RF per fuel-buffer unit |
| [`oreDrillFuelValueFuel`](../01-core/config.md#category-ore-drills) | 96 | 1–100 000 | buffer units per mB of fuel |
| [`oreDrillFuelValueBioFuel`](../01-core/config.md#category-ore-drills) | 48 | 1–100 000 | buffer units per mB of biofuel |
| [`MachinesNeedNoPower`](../01-core/config.md#category-general) | false | — | general category; makes all consumption free |

### 5.4 Paying for a tick (`consumePower` override)

The master keeps a **fuel buffer** (integer units). Each unit is worth `oreDrillFuelMultiplier` RF (FACTOR, default 1000). To pay *amount* RF:

1. If `MachinesNeedNoPower` is set, succeed.
2. If buffer × FACTOR ≥ amount: subtract max(amount ÷ FACTOR, 1) from the buffer and succeed.
3. Otherwise:
   1. Use the rest of the buffer: amount −= buffer × FACTOR, then buffer = 0 and bufferMax = 0.
   2. **Refill the buffer:**
      - **From slot 10:** if it holds an item with a furnace burn time B > 0, consume one item and set buffer = bufferMax = B. If the item has a container item (for example a lava bucket leaves an empty bucket), the container goes into slots **1–9**, which are the output slots ([MUTL-13](index.md#suspected-bugs)); overflow is dropped above the controller.
      - **Else from the input tank:** if it holds lava, fuel or biofuel, drain up to 1 000 mB and set buffer = bufferMax = drained mB × value. The value is 20 for lava (the same as a lava bucket's 20 000 burn ticks), or the configured fuel/biofuel values.
   3. If the new buffer covers the rest: subtract and succeed.
4. Otherwise take the rest from the RF buffer, if it holds enough. A remaining amount of 0 counts as success.

Fuel equivalents at the default FACTOR:

| Fuel | RF |
|---|---|
| Coal item (burn time 1 600) | 1.6 M |
| 1 000 mB lava | 20 M |
| 1 000 mB fuel | 96 M |
| 1 000 mB biofuel | 48 M |

---

## 6. NBT

**Master:**

| Source | Keys |
|---|---|
| `BasicMachineTileEntity` and its bases | `Items`, `rotation`, `CustomName`, `security`, `owner`, `Energy`, `hasSignal`, `redstoneBehaviour`, `Progress` (short), `TotalTime` (short), `StackMultiplier`, `CurrentOperation` |
| Master | `complete`; when complete also `dir`, `radius` (bytes), `enginesize`, `rodsize` (shorts), `haspower`, `currentRFperTick`, `hasDrill`, `drillType` (byte) |
| Master, always | `fuelBuffer`, `fuelBufferMax`, `inputTank`, `outputTank` (Forge tank tags), `currentLiquidOp` (fluid stack, only when set) |

`Progress` and `TotalTime` are shorts: see [MUTL-09](index.md#suspected-bugs).

**Slave:** `hasMaster`. If linked, also `masterX` (int), `masterY` (short), `masterZ` (int), `bottomrow` (bool) and `direction` (byte; −1 = invalid, else the dir code). Slaves sync with full-NBT description packets.

---

## 7. GUI (`GuiOreDrill`, id 16)

The GUI extends [`GuiTechgunsPoweredTileEnt`](../08-machines-production/gui-handler.md#3-guitechgunspoweredtileent-base-gui-for-every-powered-machine) ([08](../08-machines-production/index.md)), so it has:

- a power bar at (8,17) 4 × 48, with tooltip "Power: n/max RF";
- redstone and security side buttons at x = −22, y = 10 and 40.

Its background is [`textures/gui/oreDrill_GUI.png`](../08-machines-production/gui-textures.md#9-texturesguioredrill_guipng-owned-by-09), 256 × 256, catalogued by 08.

### Container slots (`OreDrillContainer`)

| Index | Slot | Position |
|---|---|---|
| 0 | drill head | (30, 17) |
| 1–9 | outputs; index 1 + 3·col + row (column-major) | (102 + 18·col, 17 + 18·row) |
| 10 | fuel | (30, 51) |
| 11–37 | player main | (8 + 18·col, 84 + 18·row) |
| 38–46 | hotbar | (8 + 18·i, 142) |

Shift-click rules:

| From | To |
|---|---|
| Head slot (0) | blocked ("no shift-click out") |
| Slots 1–10 | player inventory, from the end |
| Player slot | slot 0 if the item is any drill head; else slot 10 if it is a furnace fuel; else nothing |

The player range check stops one short, so the last hotbar slot is ignored ([MUTL-12](index.md#suspected-bugs)).

### Progress-bar sync

Ids 0–8 travel as window-property shorts:

| Id | Value | Sent |
|---|---|---|
| 0 | progress | when changed |
| 1 | totalTime | when changed |
| 2 | power ÷ 100 | when changed or while working |
| 3 | input fluid id (−1 = none) | when the type changes |
| 4 | output fluid id (−1 = none) | when the type changes |
| 5 | input amount | when changed or while working |
| 6 | output amount | when changed or while working |
| 7 | fuel buffer ÷ 10 | when changed |
| 8 | fuel buffer max ÷ 10 | when changed |

### Texture use

| Texture region (px) | Depicts | Drawn |
|---|---|---|
| (0,0) 176×166 | Grey panel. Left: the power-bar slot (≈(7,16)–(12,66)); next to it the input-tank window (≈(15,16)–(26,67)) with a ruler scale. Head slot with a downward drill-bit silhouette. A pipe connecting down to a small flame window and the fuel slot (flame silhouettes). A large hollow downward triangle (drill progress) at ≈(72,16)–(98,54). 3×3 output grid. Output-tank window at the right edge (≈(157,16)–(168,67)). Player inventory. | always at (0,0) |
| (177,1) 25×(n+1) | Filled drill cone: light-grey stepped triangle, white highlight on the right edge | while working, at (73,17); n = progress scaled to 36, so it fills top-down |
| (188, 39+s) 6×(12−s) | Fuel flame gauge: vertical gradient from dark red (top) through orange to yellow (bottom) | when bufferMax > 0, at (35, 36+s), where s = 12 − buffer scaled to 12; it fills bottom-up |
| (176,40) 11×52 and 12×52 | Tank gauge overlay: transparent window, white rim, dark tick marks (long tick every 5) and a red max line at the top | over the input tank at (15,17) and the output tank at (157,17) |

The tank fluids are drawn first, using the fluid's still icon from the block atlas:

- input tank: 10 px wide at (16,17)
- output tank: 10 px wide at (158,17)
- fill height = amount × 50 ÷ capacity, tiled bottom-up.

### Text and tooltips

- The title is centred at y = 6 (`techguns.container.oredrill` = "Ore Drill").
- "Inventory" is drawn at (8, 72).
- When the multiblock is **not complete**, two red (0xFFFF2020) lines are centred around x = 88 − 16:
  - y = 44: `techguns.container.oredrill.broken` = "Multiblock broken"
  - y = 52: `techguns.container.oredrill.broken2` = "or invalid Drill"

| Hover rect (x, y, w, h) | Tooltip |
|---|---|
| (74, 16, 27, 39), the progress cone | "RF/t: <rf>", "<pct with 2 decimals>%", "Ores/h: <72 000 ÷ totalTime, 2 decimals>". The labels are hardcoded English. |
| (15, 16, 11, 51), input tank | translated fluid name or `techguns.gui.empty` = "Empty"; then "<amount>/<capacity>mB" |
| (157, 16, 11, 51), output tank | the same for the output tank |
| (34, 36, 8, 14), fuel flame | If bufferMax > 0: "`techguns.oredrill.currentfuel` <buf>/<max>" = "Fuel n/m", then "`techguns.oredrill.remaining`: <ticks> `techguns.oredrill.ticks`" = "Remaining time: N ticks.", where ticks = buffer ÷ (RF/t ÷ FACTOR), or −1 when RF/t = 0. Otherwise `techguns.oredrill.nofuel` = "No Fuel". |

---

## 8. Sounds

The work loop plays only on the **client**, during each tick in which progress advances while `haspower` is set. It uses `World.playSound` at the controller with pitch 1.0 and distance delay. After it plays, it is not repeated for 61 ticks (≈ 3.1 s, the length of the clips).

| Radius | sounds.json event | Volume | Files (one picked at random) |
|---|---|---|---|
| 0 | `techguns:machines.oredrillSmallWork` (category block) | 1.5 | [`sounds/machines/oredrill_s_work_a.ogg`](../08-machines-production/sounds.md#1-events-and-files), `_b`, `_c`, `_d` |
| 1–2 | `techguns:machines.oredrillMediumWork` | 2.25 | [`sounds/machines/oredrill_m_work_a.ogg`](../08-machines-production/sounds.md#1-events-and-files) … `_d` |
| 3–4 | `techguns:machines.oredrillLargeWork` | 3.0 | [`sounds/machines/oredrill_l_work_a.ogg`](../08-machines-production/sounds.md#1-events-and-files) … `_d` |

The files themselves are catalogued in [08](../08-machines-production/index.md)'s machine sound catalogue.

---

## 9. TESR (`RenderOreDrill`)

The renderer is bound to the master. It draws nothing unless the drill is complete.

**Culling box.** When complete, the render bounding box spans from the controller along `dir` for E + R blocks, with half-width 2 when r = 2 and 1 otherwise ([MUTL-15](index.md#suspected-bugs)).

**Placement.** The column is drawn on the drill axis. It starts at the first rod block (E + 1 blocks from the controller) and runs R blocks towards the cluster:

1. The renderer works in a local frame whose +Y is the drill direction. For −Y it rotates 180° about Z. For ±X it rotates −90°·sign about Z. For ±Z it rotates 90°·sign about X. Small offsets (±0.75, −1.5) centre the column.
2. It then translates 2·(E + 1) along local Y. Each segment takes the lighting of the world block it passes through ([MUTL-16](index.md#suspected-bugs)).

**Spin.** When `hasDrill`, the redstone condition and `haspower` all hold, the whole column rotates about its axis by angle = (worldTime mod 360) × 10° plus a partial-tick term. That is 10° per tick, one turn per 36 ticks.

**Drill head, when `hasDrill`.**

- The texture is chosen by `drillType`:

  | drillType | Texture |
  |---|---|
  | 1 | [`textures/blocks/drillhead.png`](../07-blocks/textures.md#ore-drill-oredrill-08) |
  | 2 | [`textures/blocks/drillheadObsidian.png`](../07-blocks/textures.md#ore-drill-oredrill-08) |
  | 3 | [`textures/blocks/drillheadCarbon.png`](../07-blocks/textures.md#ore-drill-oredrill-08) |

- N = 4·R stacked boxes (4 per rod block). Each step advances 0.25 along the axis. Each box has half-height 0.25, so the boxes overlap and form a continuous body.
- Box i (0 = nearest the engines) has half-width
  size_i = R_eff × ln(N − i + 1) ÷ ln(N + 1) × f,
  where R_eff = max(r, 1) and f is the radius factor:

  | r | 0–1 | 2 | 3 | 4 |
  |---|---|---|---|---|
  | f | 0.35 | 0.50 | 0.55 | 0.60 |

  The result is a logarithmic taper to a point at the cluster.
- Every odd-numbered box is turned 45° about the axis, which gives a twisted, star-shaped auger.
- Each box (`OreDrillCube`) maps the **whole texture** onto every face.

**Bare rod, when no head.**

- R boxes, one per rod block, each with half-width 0.2 and half-height 0.5, all turned 45° (a diamond cross-section).
- They always use `drillhead.png`.
- The column starts 0.2 blocks back from the first rod position.

### Drill head textures (32 × 32, no animation)

| File | Depicts / palette |
|---|---|
| `textures/blocks/drillhead.png` | Vertical metallic streaks: a dark charcoal band down the centre fading to light grey at the left and right edges, noisy brushed-steel grain. Wrapped around each box it reads as a polished steel cylinder. |
| `textures/blocks/drillheadObsidian.png` | The same shading in desaturated violet / blue-grey (obsidian steel) |
| `textures/blocks/drillheadCarbon.png` | The same shading in near-black to dark grey (carbon) |

---

## 10. Behaviour summary for implementers

- The cluster is never depleted. Output is unlimited, with a rate set by cluster size (capped by drill length), mining-level surplus and config multipliers.
- The drill must be **started by right-clicking the controller** after a head is inserted, or after `hasDrill` was cleared by removing the head.
- A head that is too weak still runs, but produces only cobblestone.
- Liquid clusters (oil) put their fluid into the 32-bucket output tank.

---

## 11. Legacy prototype (dead code)

`blocks/machines/OreDrill` is a `BasicMachine` subclass named "OreDrill". It creates `OreDrillTileEnt` and refuses activation. Its registration is commented out.

`OreDrillTileEnt` is still registered as a tile entity id (`OreDrillTileEnt`). It is an `ISidedInventory` with zero slots. Each tick, when the block **below** it is `TGBlocks.oreCluster`:

1. Count a timer down from 100.
2. At zero, flood-fill the cluster size and drop one **iron ore** above itself (server).
3. Reset the timer to 1000 ÷ size.

No block creates this tile in 0.1.2.
