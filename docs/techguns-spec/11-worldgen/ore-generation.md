# Ore generation, ore clusters and oil holes

Classes covered: `worldgen/OreGenerator`, `worldgen/OreClusterWorldspawn`, `worldgen/WorldGenOilHoles`, plus the cluster-related parts of `blocks/OreCluster` and [`TGBlocks`](../07-blocks/block-catalogue.md#registered-blocks), and the items [`OreClusterScanner`](../06-items/special-items.md#ore-cluster-scanner-oreclusterscanner-techgunsoreclusterscanner) and [`oreClusterDebugger`](../06-items/special-items.md#ore-cluster-debugger-oreclusterdebugger). Block definitions are owned by **[07-blocks](../07-blocks/index.md)**, items by **[06-items](../06-items/index.md)**, and the Ore Drill that mines clusters by **[08-machines-production](../08-machines-production/index.md)**. Textures are catalogued in [assets.md](assets.md).

---

## 1. Ore veins (`OreGenerator`)

* **Registration:** FML world generator registered in [`Techguns.preInit`](../01-core/lifecycle-and-registration.md#lifecycle-overview), weight **1**, but only if at least one of [`doOreGenLead`](../01-core/config.md#category-general), [`doOreGenCopperTin`](../01-core/config.md#category-general), [`doOreGenTitanium`](../01-core/config.md#category-general) or [`doOreGenUranium`](../01-core/config.md#category-general) is true. The four flags are passed into the constructor.
* **Dimension:** Overworld only (`dimensionId == 0`). No biome rules.
* **Vein algorithm:**
  1. For each ore, the vein size is rolled once per chunk.
  2. The generator then makes `chances` attempts.
  3. Each attempt picks `x = chunkX·16 + rnd(16)`, `z = chunkZ·16 + rnd(16)`, `y = minY + rnd(maxY − minY)` (range minY … maxY−1).
  4. It runs vanilla `WorldGenMinable(block, meta, veinSize, Blocks.stone)` there: the standard elongated-blob vein that replaces only plain stone.

| Ore | Block (meta) | Config gate | Vein size per chunk | Attempts / chunk | Y range |
|---|---|---|---|---|---|
| Copper | `Techguns.oreCopper` (registry `oreCopper`, texture `techguns:oreCopperBlock`) | `doOreGenCopperTin` | 5 + rnd(3) → 5–7 | 12 | 5–79 |
| Tin | `Techguns.oreTin` (`oreTin`, `techguns:oreTinBlock`) | `doOreGenCopperTin` | 4 + rnd(3) → 4–6 | 10 | 5–59 |
| Lead | `Techguns.oreLead` (`oreLead`, `techguns:oreLeadBlock`) | `doOreGenLead` | 4 + rnd(2) → 4–5 | 8 | 5–49 |
| Titanium | `TGBlocks.oreTitanIron` meta 0 | `doOreGenTitanium` | 4 + rnd(2) → 4–5 | 4 | 4–23 |
| Uranium | `TGBlocks.oreTitanIron` meta 1 | `doOreGenUranium` | 4 + rnd(4) → 4–7 | 5 | 4–31 |

Facts about the ore blocks that worldgen depends on (owned by [07-blocks](../07-blocks/index.md) and [01-core](../01-core/index.md)):
* Copper, tin and lead are plain `GenericBlock(Material.rock)` blocks. They are created and registered only when their config flag is on, and get OreDict names `oreCopper`, `oreTin` and `oreLead`.
* [`oreTitanIron`](../07-blocks/block-catalogue.md#registered-blocks) is a 2-meta block:
  * hardness 4, resistance 7
  * meta 0: "Titanium Iron Ore", texture `techguns:oreTitanIron`, harvest level pickaxe 3, light 3
  * meta 1: "Uranium Ore", texture `techguns:oreUranium`, harvest level pickaxe 2, light 5

  It is always registered, even when generation is off.

---

## 2. The ore-cluster system

Ore clusters are unbreakable "deposit" blocks that the Ore Drill multiblock mines indefinitely (see [08](../08-machines-production/index.md)). One `OreCluster` block holds up to 16 cluster types, one per metadata.

### 2.1 `OreCluster` block (worldgen-relevant facts)
* **Material and strength:** `Material.rock`, stone step sound, hardness **−1** (unbreakable), resistance 6,000,000. Drops nothing.
* **Crowbar exception:** a player holding [`TGItems.crowbar`](../06-items/item-catalogue.md) breaks a cluster with relative hardness 0.005, unless that meta is flagged `isBedrock`.
* **Per-meta arrays** (filled by `addOre` / `addLiquid`):

| Array | Meaning |
|---|---|
| `ores[]` | Output ore ItemStack, or the cluster block itself for liquid clusters |
| `mininglevels[]` | Required drill mining level; 0 marks an unused placeholder slot |
| `colors[][3]` | RGB tint (0–1) for the overlay |
| `textureType[]` | Overlay index 0–3 |
| `rarity[]` | Rarity 1–4 |
| `maxy[]` | Maximum generation Y |
| `fluids[]` | FluidStack for liquid clusters, else null |
| `isBedrock[]` | Base texture is bedrock and the crowbar cannot break it |

* **Rendering** (render type set by the block; renderer [`RenderOrecluster`](../07-blocks/block-rendering-and-models.md#1-isbrh-renderers-and-render-ids), owned by [07](../07-blocks/index.md)/[03](../03-gun-visuals-audio/index.md)):
  * **Base:** stone, or netherrack if the client player is in dimension −1, or end_stone in dimension 1, or bedrock when `isBedrock`.
  * **Overlay:** the same cube faces drawn again with `overlay_texture[textureType[meta]]`, tinted by `colors[meta]`.
  * Overlay icons: index 0 `techguns:oreClusterNew1`, 1 `oreClusterNew2`, 2 `oreClusterNew3`, 3 `oreClusterNew4`.
* **Creative listing and naming:** `getSubBlocks` lists only metas whose mining level is > 0. Item and lang names are `techguns.tile.<oreCluster|oreCluster2>_<meta>.name`.
* **Config multipliers:** each block receives per-meta amount and power multipliers from config category `Ore Drills`. [`oreCluster`](../07-blocks/block-catalogue.md#registered-blocks) uses [`oreClusterMultipliersAmount`](../01-core/config.md#category-ore-drills) and [`oreClusterMultipliersPower`](../01-core/config.md#category-ore-drills); [`oreCluster2`](../07-blocks/block-catalogue.md#registered-blocks) uses the `…2` variants. Only the Ore Drill consumes them (see [08](../08-machines-production/index.md)). Custom clusters default to 1.0.

### 2.2 Registered cluster types (`TGBlocks.init` / `postInit`)
`TGBlocks.oreclusters` is the list of all cluster blocks: `oreCluster`, `oreCluster2`, and optionally [`oreClusterCustom`](../07-blocks/block-catalogue.md#registered-blocks) / [`oreClusterCustom2`](../07-blocks/block-catalogue.md#registered-blocks) when config `addCustomOreCluster` / `addCustomOreCluster2` is on. The custom clusters start empty and are filled through MineTweaker (see [01](../01-core/index.md)/[06](../06-items/index.md)).

The metas 7–15 of `oreCluster` and 2–3 of `oreCluster2` are added in `postInit`. Each modded ore is looked up as the first OreDictionary entry for its name. If none exists, a **placeholder** (vanilla stone, mining level 0, all zeros) takes the slot, so meta numbering never shifts.

**`oreCluster`** (registry `oreCluster`):

| Meta | Ore (OreDict / item) | Mining level | Tint RGB | Overlay | Rarity | Max Y | Lang (English) | Default amount / power mult. |
|---|---|---|---|---|---|---|---|---|
| 0 | vanilla iron_ore | 1 | .8 .6 .5 | 0 | 1 | 64 | Ore Cluster (Iron) | 2.0 / 0.15 |
| 1 | gold_ore | 2 | 1 1 .6 | 0 | 3 | 48 | Ore Cluster (Gold) | 1.0 / 0.4 |
| 2 | redstone_ore | 2 | .8 0 0 | 0 | 2 | 32 | Ore Cluster (Redstone) | 1.0 / 0.3 |
| 3 | coal_ore | 1 | .3 .3 .3 | 1 | 1 | 64 | Ore Cluster (Coal) | 1.2 / 0.25 |
| 4 | lapis_ore | 2 | .5 .5 1 | 1 | 2 | 48 | Ore Cluster (Lapis) | 1.0 / 0.3 |
| 5 | diamond_ore | 3 | .6 .9 1 | 2 | 4 | 32 | Ore Cluster (Diamond) | 0.25 / 1.0 |
| 6 | emerald_ore | 3 | .2 1 .45 | 2 | 4 | 32 | Ore Cluster (Emerald) | 0.25 / 1.0 |
| 7 | `oreCopper` | 1 | .65 .5 .05 | 0 | 1 | 48 | Ore Cluster (Copper) | 2.0 / 0.1 |
| 8 | `oreTin` | 1 | .65 .95 1 | 0 | 1 | 48 | Ore Cluster (Tin) | 2.0 / 0.1 |
| 9 | `oreLead` | 2 | .55 .65 1 | 0 | 2 | 48 | Ore Cluster (Lead) | 1.0 / 0.2 |
| 10 | `oreOsmium` | 2 | .75 .75 1 | 1 | 2 | 48 | Ore Cluster (Osmium) | 1.0 / 0.3 |
| 11 | `oreSilver` | 2 | .9 .9 1 | 0 | 3 | 32 | Ore Cluster (Silver) | 1.0 / 0.35 |
| 12 | `oreNickel` | 2 | 1 .8 .6 | 0 | 2 | 32 | Ore Cluster (Nickel) | 1.0 / 0.3 |
| 13 | `oreAluminium` | 2 | .95 .95 .95 | 1 | 2 | 32 | Ore Cluster (Aluminium) | 1.0 / 0.2 |
| 14 | `orePlatinum` | 2 | .9 .9 1 | 1 | 4 | 32 | Ore Cluster (Platinum) | 0.35 / 1.0 |
| 15 | `oreUranium` | 3 | .5 1 .5 | 0 | 4 | 32 | Ore Cluster (Uranium) | 0.25 / 1.0 |

**`oreCluster2`** (registry `oreCluster2`):

| Meta | Ore | Mining level | Tint | Overlay | Rarity | Max Y | Bedrock | Lang | Default amount / power |
|---|---|---|---|---|---|---|---|---|---|
| 0 | quartz_ore | 2 | 1 1 1 | 0 | 1 | 64 | no | Ore Cluster (Nether Quartz) | 1.5 / 0.35 |
| 1 | `oreTitanIron` meta 0 | 3 | .85 .85 .95 | 1 | 4 | 32 | no | Ore Cluster (Titanium Iron) | 0.5 / 0.8 |
| 2 | `oreCertusQuartz` (or placeholder) | 3 | .75 .95 1 | 2 | 3 | 32 | no | Ore Cluster (Certus Quartz) | 1.5 / 0.4 |
| 3 | **Oil**: FluidStack `oil` 1000 mB (or stone placeholder if no `oil` fluid is registered) | 3 | .1 .1 .1 | 3 | 3 | 5 | **yes** | Ore Cluster (Oil Hole) | 1.0 / 0.5 |
| 4 | glowstone block | 2 | .9 .9 .2 | 2 | 2 | 96 | no | Ore Cluster (Glowstone) | 1.5 / 0.35 |

Metas 5–15 of `oreCluster2` are unused. Their config multipliers default to 1.0.

### 2.3 Natural cluster generation (`OreClusterWorldspawn`): disabled
The registration line in `Techguns.preInit` is commented out. **In this version, ore clusters never generate naturally, except oil holes (§ 3).** In normal play, players can only obtain them from the creative tab. The class is fully written and would behave as follows if re-enabled:

* Overworld only. For every cluster block in `oreclusters` and every meta with mining level > 0, one `WorldGenMinable` vein per attempt replaces stone.
* Vein size: `n + rnd(n)` with `n = 10 − 2·rarity`.

| Rarity | Vein size | Attempts per chunk |
|---|---|---|
| 1 | 8–15 | 2 |
| 2 | 6–11 | 1 |
| 3 | 4–7 | 1 |
| 4 | 2–3 | 1 |

* Y range: 0 … `maxy − 1`, at a random XZ in the chunk.
* As written, it uses the meta-less `WorldGenMinable` constructor, so every vein would be meta 0 (iron, or nether quartz) ([WGEN-02](index.md#suspected-bugs)). A rarity of 5 or more would give `nextInt(0)` and crash.

### 2.4 Ore Cluster Scanner (`items/OreClusterScanner`)
The item side is owned by 06. Its interaction with the cluster system:
* Registry and unlocalized name [`oreClusterScanner`](../06-items/item-catalogue.md#2-stand-alone-items-registered-by-tgitems), texture [`textures/items/oreClusterScanner.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items), lang `techguns.item.oreClusterScanner.name=Ore Cluster Scanner`. Max stack 1.
* **Energy:** CoFH `IEnergyContainerItem`. Capacity 30,000 RF, max receive and max extract 3,000 RF per call, stored in the NBT int `Energy`.
  * The creative-tab instance and any crafted instance (`onCreated`) start full.
  * The durability bar is always shown, at `1 − energy/capacity`.
  * The tooltip line is `<energy>/30000 RF`.
* **Use:** right-click starts using, with an unlimited use duration. `onUsingTick` force-stops use once 40 ticks (`SCANDURATION`) have elapsed.
* **On release** (`onPlayerStoppedUsing`):
  1. Extract 300 RF, on both sides, for any hold length ([WGEN-19](index.md#suspected-bugs)).
  2. On the **client only**, if exactly 300 was extracted and the hold lasted ≥ 40 ticks, scan the player's current chunk: all 16×16 columns from player Y + 16 (capped at world height) down to Y 1.
  3. For every block that is in `TGBlocks.oreclusters`, spawn the client particle effect `"OreClusterPing"` at the block centre (FX defined in [04-projectiles-fx](../04-projectiles-fx/index.md)) and collect the translated name `techguns.tile.<block>_<meta>.name`.
  4. Chat output:
     * if any were found: `techguns.OreClusterScanner.msg.found` = "Found Ore Clusters in this Chunk", then one line per distinct cluster name
     * otherwise: `techguns.OreClusterScanner.msg.notFound` = "No Clusters in this Chunk"

### 2.5 Ore Cluster Debugger (`items/oreClusterDebugger`)
* Registry `oreClusterDebugger`, unlocalized `oreClusterDebugger`, texture `techguns:testItem` ([`textures/items/testItem.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items)). Max stack 1.
* Lang: `techguns.item.oreClusterDebugger.name=Ore Cluster Debugger`. Tooltip `techguns.oreClusterDebugger.Tooltip=WARNING: Removes all destroyable Blocks in a huge Radius, used to debug Ore cluster spawns`.
* **Right-click on a block (server):** for every y from the clicked y down to 1, and every x in `[x−16, x+16)` and z in `[z−16, z+16)`, any block with hardness ≥ 0 is set to air. Bedrock, ore clusters and other unbreakable blocks remain, which exposes every cluster in a 32×32 column for inspection. There is no confirmation and no undo.

---

## 3. Oil holes (`WorldGenOilHoles`)

* **Registration:** in `Techguns.postInit`, weight **3**, when config [`spawnOilHoles`](../01-core/config.md#category-spawn-controll-sic) is true **and** `TGFluids.OIL != TGFluids.LAVA`. `OIL` is `FluidRegistry.getFluid("oil")` and may be null, in which case this test still passes ([WGEN-03](index.md#suspected-bugs)).
* **Dimension:** Overworld only.
* **Per chunk:**
  1. With probability [`oilHoleFrequency`](../01-core/config.md#category-spawn-controll-sic) (default 0.05), pick a random column (x, z) in the chunk.
  2. Starting at y = 0, check whether bedrock occupies at least one cell of the "plus" pattern: (x,z) and its 4 horizontal neighbours.
  3. While that holds and y ≤ 10:
     * place `oreCluster2` meta **3** (Oil cluster) at (x, y, z) with flag 2
     * set the 4 horizontal neighbours to bedrock
     * y++, then re-check the plus pattern at the new y
* **Result:** a vertical oil-cluster column of typically 1–5 blocks, at most 11 (y 0–10), sheathed in bedrock and following the natural bedrock layer. Because the oil meta is flagged `isBedrock`, it renders with a bedrock base and a dark oil-puddle overlay (`oreClusterNew4`, tint .1/.1/.1), and the crowbar cannot break it.

---

## 4. Per-ore and per-cluster sheets

The sheets below restate the tables above in the entity-sheet shape from `CONVENTIONS.md` (identity, obtaining, numbers, behaviour, persistence, UI, visuals, audio, relationships, bugs) so each ore and cluster can be read on its own. Nothing in this area plays a sound (*none*), has a GUI (*none*) or stores NBT on a world block (*none*; the ore blocks and `OreCluster` are plain metadata blocks without tile entities).

### 4.1 Vein ores generated by `OreGenerator`

Shared by all five ores:

| Field | Value |
|---|---|
| Generator | `OreGenerator` (FML `IWorldGenerator`, weight 1), registered in preInit when any of the four flags is on |
| Dimensions | Overworld (`dimensionId` 0) only. Nether, End and modded dimensions get nothing |
| Biomes | all. The generator never reads the biome |
| Replaces | vanilla `stone` only (the target block passed to `WorldGenMinable`). Dirt, gravel and other materials are never replaced |
| Roll per attempt | X and Z uniform in the chunk, Y uniform in `[minY, maxY)` (max exclusive). The vein is then grown by vanilla `WorldGenMinable` from that point |
| Vein size | rolled once per chunk per ore, then reused for every attempt of that ore in that chunk. The value is the maximum block count of the vein, so the real count is lower when the blob meets non-stone blocks |
| Persistence and sync | none. Ore blocks are metadata blocks; no NBT, no packets |
| UI, audio | none |
| Bugs | none specific |

Per-ore sheets:

| Field | Copper | Tin | Lead | Titanium Iron | Uranium |
|---|---|---|---|---|---|
| Registry name | `oreCopper` | `oreTin` | `oreLead` | `oreTitanIron` | `oreTitanIron` |
| Metadata | 0 (single) | 0 | 0 | 0 | 1 |
| Class | [`GenericBlock`](../07-blocks/block-catalogue.md#conventions-and-defaults) | `GenericBlock` | `GenericBlock` | [`GenericBlockMetadata`](../07-blocks/special-blocks.md#10-titanium-iron-and-uranium-ore-oretitaniron-genericblockmetadata) (2 metas) | same block |
| Lang key / English | `techguns.tile.oreCopper.name` / Copper Ore | `techguns.tile.oreTin.name` / Tin Ore | `techguns.tile.oreLead.name` / Lead Ore | `techguns.tile.oreTitanIron_0.name` / Titanium Iron Ore | `techguns.tile.oreTitanIron_1.name` / Uranium Ore |
| OreDict | `oreCopper` | `oreTin` | `oreLead` | `oreTitaniumIron`, `oreIllmenite` | `oreUranium` |
| Texture | `oreCopperBlock` | `oreTinBlock` | `oreLeadBlock` | `oreTitanIron` (4-frame strip) | `oreUranium` (4-frame strip) |
| Config gate (default) | `doOreGenCopperTin` (false) | `doOreGenCopperTin` (false) | `doOreGenLead` (false) | `doOreGenTitanium` (true) | `doOreGenUranium` (true) |
| Block exists when | gate is on | gate is on | gate is on | always | always |
| Vein size (max blocks) | 5 + rnd(3), i.e. 5-7 | 4 + rnd(3), i.e. 4-6 | 4 + rnd(2), i.e. 4-5 | 4 + rnd(2), i.e. 4-5 | 4 + rnd(4), i.e. 4-7 |
| Veins per chunk (attempts) | 12 | 10 | 8 | 4 | 5 |
| Y range | 5-79 | 5-59 | 5-49 | 4-23 | 4-31 |
| Mean vein size | 6.0 | 5.0 | 4.5 | 4.5 | 5.5 |
| Upper bound, ore blocks per chunk (attempts x mean size) | 72 | 50 | 36 | 18 | 27.5 |
| Hardness / resistance | see [07-blocks](../07-blocks/block-catalogue.md#registered-blocks) | same | same | 4 / 7 | 4 / 7 |
| Harvest | rock material, pickaxe | same | same | pickaxe 3 | pickaxe 2 |
| Light emitted | 0 | 0 | 0 | 3 | 5 |
| Yield (what mining returns) | the block itself, same meta | same | same | the block itself, meta 0 | the block itself, meta 1 |
| Smelting | furnace to Copper Ingot, 0.7 XP ([06-items](../06-items/index.md) `recipes.md` section 6) | Tin Ingot, 0.7 XP | Lead Ingot, 0.7 XP | none documented | none documented |
| Ore Drill link | `oreCluster` meta 7 resolves `oreCopper` | meta 8 | meta 9 | `oreCluster2` meta 1 | `oreCluster` meta 15 (`oreUranium`) |

The "upper bound" row is arithmetic on the table above (attempts times mean size), not a measured output. Real yield is lower because a vein is clipped by caves, by non-stone blocks and by the world's Y limits.

### 4.2 Oil holes (`WorldGenOilHoles`)

| Field | Value |
|---|---|
| Identity | world generator `WorldGenOilHoles` (FML `IWorldGenerator`, weight 3). Places `oreCluster2` meta 3, lang `techguns.tile.oreCluster2_3.name` = "Ore Cluster (Oil Hole)" |
| Obtaining | worldgen only (or creative; the meta is hidden from creative when it is the stone placeholder, see WGEN-03) |
| Registration gate | config `spawnOilHoles` (category Spawn Controll, default true) and `TGFluids.OIL != TGFluids.LAVA` |
| Frequency | config `oilHoleFrequency` (Spawn Controll, default 0.05, range 0-1). One roll per generated chunk: a uniform float is compared with the setting, so 0.05 is about one chunk in twenty |
| Dimensions, biomes | Overworld only; biome ignored |
| Column | one (x, z) chosen uniformly in the chunk. Starting at y = 0, while the cell (x, y, z) or any of its 4 horizontal neighbours is bedrock and y <= 10, it places the oil cluster at (x, y, z) with flag 2 and bedrock at the 4 horizontal neighbours, then moves up one. The maximum column is 11 blocks (y 0-10) |
| Neighbour overwrite | the 4 neighbouring cells are set to bedrock unconditionally, replacing whatever was there, including stone and ore. The code also reads the block at the centre before overwriting it and never uses the value |
| Cluster numbers | mining level 3, rarity 3, max Y 5, flagged bedrock (the crowbar cannot break it), tint 0.1 / 0.1 / 0.1, overlay 3 (`oreClusterNew4`) |
| Yield | when mined with an Ore Drill, one 1000 mB `oil` FluidStack per operation. Default multipliers amount 1.0, power 0.5; reference case (cluster size 1, effective level 0, radius 0) gives 3.0 operations per hour, 120 RF/t, 2,880,000 RF per 1000 mB |
| Persistence | none |
| Audio, UI | none |
| Bugs | WGEN-03 (generates without an oil fluid) |

The first test needs bedrock at or beside y = 0, so the hole is effectively anchored on the bottom bedrock layer.

### 4.3 Cluster types: the common sheet

Every cluster type shares the following fields. They differ only in the per-meta numbers in 4.4.

| Field | Value |
|---|---|
| Classes | `OreCluster` (blocks `oreCluster`, `oreCluster2`, optional `oreClusterCustom`, `oreClusterCustom2`) |
| Obtaining | creative tab only in this build, because natural cluster generation is disabled ([WGEN-01](index.md#suspected-bugs)), except meta 3 of `oreCluster2` (oil holes, 4.2). `getSubBlocks` hides metas whose mining level is 0 |
| Breaking | hardness -1 (unbreakable), resistance 6,000,000, drops nothing. A held crowbar breaks it at relative hardness 0.005 unless the meta is flagged bedrock |
| Natural generator (disabled) | `OreClusterWorldspawn`: Overworld only, every cluster block, every meta with mining level > 0. Vein size `n + rnd(n)` with `n = 10 - 2 x rarity`; attempts per chunk 2 for rarity 1 and 1 for rarity 2-4; Y in `[0, maxY)`; replaces stone only. Because of WGEN-02 every vein would be meta 0 |
| Biomes | none checked |
| Persistence | none (metadata block, no tile entity). The cluster identity is the metadata value |
| Visuals | `RenderOrecluster` draws a base cube (stone, netherrack in dimension -1, end stone in dimension 1, bedrock when flagged) then the overlay icon tinted by the meta colour. Overlay textures are listed in [assets.md](assets.md) |
| Config | `oreClusterMultipliersAmount` / `oreClusterMultipliersPower` (block 1) and the `...2` arrays (block 2), category Ore Drills. Defaults are in 4.4. [`oreDrillMultiplierOre`](../01-core/config.md#category-ore-drills) (1.0) and [`oreDrillMultiplierPower`](../01-core/config.md#category-ore-drills) (1.0) scale every cluster |
| Audio, UI | none |
| Bugs | WGEN-01, WGEN-02 |

### 4.4 Per-cluster numbers

Columns: output ore, mining level (L_c), rarity, max Y, natural vein size if the disabled generator ran, attempts per chunk, then the default Ore Drill multipliers and the derived reference yield for a cluster of size 1 mined at effective level 0 and radius 0 (ores per hour = 3 x amount; RF per tick = int(80 x ores per hour x power); energy per ore = 5,760,000 x power). The yield formula is documented in [09-machines-utility](../09-machines-utility/ore-drill.md).

**`oreCluster`**

| Meta | Output | L_c | Rarity | Max Y | Natural vein (disabled) | Attempts | Amount / power | Ores per hour | RF/t | RF per ore |
|---|---|---|---|---|---|---|---|---|---|---|
| 0 | iron_ore | 1 | 1 | 64 | 8-15 | 2 | 2.0 / 0.15 | 6.0 | 72 | 864,000 |
| 1 | gold_ore | 2 | 3 | 48 | 4-7 | 1 | 1.0 / 0.4 | 3.0 | 96 | 2,304,000 |
| 2 | redstone_ore | 2 | 2 | 32 | 6-11 | 1 | 1.0 / 0.3 | 3.0 | 72 | 1,728,000 |
| 3 | coal_ore | 1 | 1 | 64 | 8-15 | 2 | 1.2 / 0.25 | 3.6 | 72 | 1,440,000 |
| 4 | lapis_ore | 2 | 2 | 48 | 6-11 | 1 | 1.0 / 0.3 | 3.0 | 72 | 1,728,000 |
| 5 | diamond_ore | 3 | 4 | 32 | 2-3 | 1 | 0.25 / 1.0 | 0.75 | 60 | 5,760,000 |
| 6 | emerald_ore | 3 | 4 | 32 | 2-3 | 1 | 0.25 / 1.0 | 0.75 | 60 | 5,760,000 |
| 7 | `oreCopper` | 1 | 1 | 48 | 8-15 | 2 | 2.0 / 0.1 | 6.0 | 48 | 576,000 |
| 8 | `oreTin` | 1 | 1 | 48 | 8-15 | 2 | 2.0 / 0.1 | 6.0 | 48 | 576,000 |
| 9 | `oreLead` | 2 | 2 | 48 | 6-11 | 1 | 1.0 / 0.2 | 3.0 | 48 | 1,152,000 |
| 10 | `oreOsmium` | 2 | 2 | 48 | 6-11 | 1 | 1.0 / 0.3 | 3.0 | 72 | 1,728,000 |
| 11 | `oreSilver` | 2 | 3 | 32 | 4-7 | 1 | 1.0 / 0.35 | 3.0 | 84 | 2,016,000 |
| 12 | `oreNickel` | 2 | 2 | 32 | 6-11 | 1 | 1.0 / 0.3 | 3.0 | 72 | 1,728,000 |
| 13 | `oreAluminium` | 2 | 2 | 32 | 6-11 | 1 | 1.0 / 0.2 | 3.0 | 48 | 1,152,000 |
| 14 | `orePlatinum` | 2 | 4 | 32 | 2-3 | 1 | 0.35 / 1.0 | 1.05 | 84 | 5,760,000 |
| 15 | `oreUranium` (resolves to `oreTitanIron` meta 1) | 3 | 4 | 32 | 2-3 | 1 | 0.25 / 1.0 | 0.75 | 60 | 5,760,000 |

**`oreCluster2`**

| Meta | Output | L_c | Rarity | Max Y | Natural vein (disabled) | Attempts | Bedrock base | Amount / power | Ores per hour | RF/t | RF per ore |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 0 | quartz_ore | 2 | 1 | 64 | 8-15 | 2 | no | 1.5 / 0.35 | 4.5 | 126 | 2,016,000 |
| 1 | `oreTitanIron` meta 0 | 3 | 4 | 32 | 2-3 | 1 | no | 0.5 / 0.8 | 1.5 | 96 | 4,608,000 |
| 2 | `oreCertusQuartz` or stone placeholder | 3 | 3 | 32 | 4-7 | 1 | no | 1.5 / 0.4 | 4.5 | 144 | 2,304,000 |
| 3 | oil FluidStack 1000 mB, or stone placeholder | 3 | 3 | 5 | 4-7 | 1 | yes | 1.0 / 0.5 | 3.0 | 120 | 2,880,000 |
| 4 | glowstone block | 2 | 2 | 96 | 6-11 | 1 | no | 1.5 / 0.35 | 4.5 | 126 | 2,016,000 |

Remaining facts for the two blocks:

* `oreCluster2` metas 5-15: unused, mining level 0, hidden from creative, multipliers default to 1.0.
* `oreClusterCustom` and `oreClusterCustom2`: registered only when `addCustomOreCluster` / `addCustomOreCluster2` (default false) is true. All 16 metas start empty (mining level 0). MineTweaker's `addOreCluster` fills them (see [08-machines-production](../08-machines-production/minetweaker.md)). Their multipliers default to 1.0, and a custom cluster's rarity and max Y are whatever the script passes. Nothing generates them while WGEN-01 stands.
* A placeholder slot (an ore name with no OreDict entry, for example `oreOsmium` without a mod providing it) keeps its meta number but is not listed in creative and cannot be mined by a drill (mining level 0).
