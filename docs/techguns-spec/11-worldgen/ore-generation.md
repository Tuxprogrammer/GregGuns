# Ore generation, ore clusters and oil holes

Classes covered: `worldgen/OreGenerator`, `worldgen/OreClusterWorldspawn`, `worldgen/WorldGenOilHoles`, plus the cluster-related parts of `blocks/OreCluster` and `TGBlocks`, and the items `OreClusterScanner` and `oreClusterDebugger`. Block definitions are owned by **07-blocks**, items by **06-items**, and the Ore Drill that mines clusters by **08-machines-production**. Textures are catalogued in [assets.md](assets.md).

---

## 1. Ore veins (`OreGenerator`)

* **Registration:** FML world generator registered in `Techguns.preInit`, weight **1**, but only if at least one of `doOreGenLead`, `doOreGenCopperTin`, `doOreGenTitanium` or `doOreGenUranium` is true. The four flags are passed into the constructor.
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

Facts about the ore blocks that worldgen depends on (owned by 07-blocks and 01-core):
* Copper, tin and lead are plain `GenericBlock(Material.rock)` blocks. They are created and registered only when their config flag is on, and get OreDict names `oreCopper`, `oreTin` and `oreLead`.
* `oreTitanIron` is a 2-meta block:
  * hardness 4, resistance 7
  * meta 0: "Titanium Iron Ore", texture `techguns:oreTitanIron`, harvest level pickaxe 3, light 3
  * meta 1: "Uranium Ore", texture `techguns:oreUranium`, harvest level pickaxe 2, light 5

  It is always registered, even when generation is off.

---

## 2. The ore-cluster system

Ore clusters are unbreakable "deposit" blocks that the Ore Drill multiblock mines indefinitely (see 08). One `OreCluster` block holds up to 16 cluster types, one per metadata.

### 2.1 `OreCluster` block (worldgen-relevant facts)
* **Material and strength:** `Material.rock`, stone step sound, hardness **−1** (unbreakable), resistance 6,000,000. Drops nothing.
* **Crowbar exception:** a player holding `TGItems.crowbar` breaks a cluster with relative hardness 0.005, unless that meta is flagged `isBedrock`.
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

* **Rendering** (render type set by the block; renderer `RenderOrecluster`, owned by 07/03):
  * **Base:** stone, or netherrack if the client player is in dimension −1, or end_stone in dimension 1, or bedrock when `isBedrock`.
  * **Overlay:** the same cube faces drawn again with `overlay_texture[textureType[meta]]`, tinted by `colors[meta]`.
  * Overlay icons: index 0 `techguns:oreClusterNew1`, 1 `oreClusterNew2`, 2 `oreClusterNew3`, 3 `oreClusterNew4`.
* **Creative listing and naming:** `getSubBlocks` lists only metas whose mining level is > 0. Item and lang names are `techguns.tile.<oreCluster|oreCluster2>_<meta>.name`.
* **Config multipliers:** each block receives per-meta amount and power multipliers from config category `Ore Drills`. `oreCluster` uses `oreClusterMultipliersAmount` and `oreClusterMultipliersPower`; `oreCluster2` uses the `…2` variants. Only the Ore Drill consumes them (see 08). Custom clusters default to 1.0.

### 2.2 Registered cluster types (`TGBlocks.init` / `postInit`)
`TGBlocks.oreclusters` is the list of all cluster blocks: `oreCluster`, `oreCluster2`, and optionally `oreClusterCustom` / `oreClusterCustom2` when config `addCustomOreCluster` / `addCustomOreCluster2` is on. The custom clusters start empty and are filled through MineTweaker (see 01/06).

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
* As written, it uses the meta-less `WorldGenMinable` constructor, so every vein would be meta 0 (iron, or nether quartz) (WGEN-02). A rarity of 5 or more would give `nextInt(0)` and crash.

### 2.4 Ore Cluster Scanner (`items/OreClusterScanner`)
The item side is owned by 06. Its interaction with the cluster system:
* Registry and unlocalized name `oreClusterScanner`, texture `textures/items/oreClusterScanner.png`, lang `techguns.item.oreClusterScanner.name=Ore Cluster Scanner`. Max stack 1.
* **Energy:** CoFH `IEnergyContainerItem`. Capacity 30,000 RF, max receive and max extract 3,000 RF per call, stored in the NBT int `Energy`.
  * The creative-tab instance and any crafted instance (`onCreated`) start full.
  * The durability bar is always shown, at `1 − energy/capacity`.
  * The tooltip line is `<energy>/30000 RF`.
* **Use:** right-click starts using, with an unlimited use duration. `onUsingTick` force-stops use once 40 ticks (`SCANDURATION`) have elapsed.
* **On release** (`onPlayerStoppedUsing`):
  1. Extract 300 RF, on both sides, for any hold length (WGEN-19).
  2. On the **client only**, if exactly 300 was extracted and the hold lasted ≥ 40 ticks, scan the player's current chunk: all 16×16 columns from player Y + 16 (capped at world height) down to Y 1.
  3. For every block that is in `TGBlocks.oreclusters`, spawn the client particle effect `"OreClusterPing"` at the block centre (FX defined in 04-projectiles-fx) and collect the translated name `techguns.tile.<block>_<meta>.name`.
  4. Chat output:
     * if any were found: `techguns.OreClusterScanner.msg.found` = "Found Ore Clusters in this Chunk", then one line per distinct cluster name
     * otherwise: `techguns.OreClusterScanner.msg.notFound` = "No Clusters in this Chunk"

### 2.5 Ore Cluster Debugger (`items/oreClusterDebugger`)
* Registry `oreClusterDebugger`, unlocalized `oreClusterDebugger`, texture `techguns:testItem` (`textures/items/testItem.png`). Max stack 1.
* Lang: `techguns.item.oreClusterDebugger.name=Ore Cluster Debugger`. Tooltip `techguns.oreClusterDebugger.Tooltip=WARNING: Removes all destroyable Blocks in a huge Radius, used to debug Ore cluster spawns`.
* **Right-click on a block (server):** for every y from the clicked y down to 1, and every x in `[x−16, x+16)` and z in `[z−16, z+16)`, any block with hardness ≥ 0 is set to air. Bedrock, ore clusters and other unbreakable blocks remain, which exposes every cluster in a 32×32 column for inspection. There is no confirmation and no undo.

---

## 3. Oil holes (`WorldGenOilHoles`)

* **Registration:** in `Techguns.postInit`, weight **3**, when config `spawnOilHoles` is true **and** `TGFluids.OIL != TGFluids.LAVA`. `OIL` is `FluidRegistry.getFluid("oil")` and may be null, in which case this test still passes (WGEN-03).
* **Dimension:** Overworld only.
* **Per chunk:**
  1. With probability `oilHoleFrequency` (default 0.05), pick a random column (x, z) in the chunk.
  2. Starting at y = 0, check whether bedrock occupies at least one cell of the "plus" pattern: (x,z) and its 4 horizontal neighbours.
  3. While that holds and y ≤ 10:
     * place `oreCluster2` meta **3** (Oil cluster) at (x, y, z) with flag 2
     * set the 4 horizontal neighbours to bedrock
     * y++, then re-check the plus pattern at the new y
* **Result:** a vertical oil-cluster column of typically 1–5 blocks, at most 11 (y 0–10), sheathed in bedrock and following the natural bedrock layer. Because the oil meta is flagged `isBedrock`, it renders with a bedrock base and a dark oil-puddle overlay (`oreClusterNew4`, tint .1/.1/.1), and the crowbar cannot break it.
