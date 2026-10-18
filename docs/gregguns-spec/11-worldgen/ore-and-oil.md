# Ores and oil: owned by GT and dreamcraft

- **Techguns baseline:** [ore-generation.md](../../techguns-spec/11-worldgen/ore-generation.md) ([ore veins](../../techguns-spec/11-worldgen/ore-generation.md#1-ore-veins-oregenerator), [clusters, disabled](../../techguns-spec/11-worldgen/ore-generation.md#23-natural-cluster-generation-oreclusterworldspawn-disabled), [oil holes](../../techguns-spec/11-worldgen/ore-generation.md#3-oil-holes-worldgenoilholes)); bugs [WGEN-01, 02, 03](../../techguns-spec/11-worldgen/index.md#suspected-bugs)
- **Design records:** [0005](../../design/0005-world-presence.md) ("No ore generation of any kind")
- **Pack mods:**
  - GT5-Unofficial (`gregtech`, 5.09.54.205)
  - NewHorizonsCoreMod (`dreamcraft`, 2.9.84)
  - Galacticraft (oil, switched off by the pack)
- **Roadmap:** M8 (a deliberate non-feature; the exit gate has "no ore generation")

## What changes from Techguns

GregGuns registers no ore, cluster or oil generator. Nothing here is built.

| Techguns system | GregGuns | Replaced by |
|---|---|---|
| Titanium and uranium veins (on), copper, tin and lead veins (off) | Not built. The four `doOreGen*` keys do not exist | GT ore veins and small ores |
| Ore clusters (never generate naturally, [WGEN-01](../../techguns-spec/11-worldgen/index.md#suspected-bugs)) | Not built | GT ore veins |
| Oil holes in bedrock, 5 % of chunks ([WGEN-03](../../techguns-spec/11-worldgen/index.md#suspected-bugs) spawns them even with no oil fluid) | Not built. `spawnOilHoles` and `oilHoleFrequency` do not exist | dreamcraft's oil spheres and fountains |

## Integration points

### GT5-Unofficial: ore veins

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GregTech/WorldGeneration.cfg`, `worldgen.general`: `disableVanillaOres` = `true`, `generateBasaltOres`, `generateBlackGraniteOres`, `generateMarbleOres`, `generateRedGraniteOres`, `generateUndergroundDirtGen` and `generateUndergroundGravelGen` all `true`. Vein definitions are in GT's code, not in this file.
- **Source:** the generator is [`GTWorldgenerator`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTWorldgenerator.java) (see [structure-placement.md](structure-placement.md) for its ordering); veins are [`WorldgenGTOreLayer`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/WorldgenGTOreLayer.java) and small ores are [`WorldgenGTOreSmallPieces`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/WorldgenGTOreSmallPieces.java).
- **GregGuns does:** nothing. Titanium, uranium, copper, tin and lead come from GT materials ([0001](../../design/0001-tier-map.md) gate materials). GregGuns recipes use GT ore dictionary entries, not Techguns ore blocks.
- **Pack-side change:** none.

### dreamcraft: oil

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GTNewHorizons/dreamcraft.cfg`: `GenerateOil` = `true`, `oilgen.OilDimensionWhitelist` = `0`, `OilSphereChance` = `60.0`, `OilDepostMinDistance` = `2`, `OilBiomeBoostFactor` = `3.5`, plus an `OilBiomeIDBlackList`. `reference/gtnh/2.9.0-RC-2/.minecraft/config/Galacticraft/core.conf`: `Oil Generation Factor` = `0`, so Galacticraft adds no oil.
- **Source:** [`OilGeneratorFix`](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modfixes/oilgen/OilGeneratorFix.java) (config options at L67-L96; dimension check at [L379](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modfixes/oilgen/OilGeneratorFix.java#L379)).
- **GregGuns does:** nothing. Whether GregGuns uses the pack's oil fluid as an input is a 06-items question, not a worldgen one.
- **Pack-side change:** none.

## Requirements

- **GGS-11-16** GregGuns registers no ore, cluster or oil `IWorldGenerator`, and no `OreGenEvent` or `DecorateBiomeEvent` handler.
- **GGS-11-17** GregGuns defines no config key named `doOreGen*`, `spawnOilHoles` or `oilHoleFrequency`.
- **GGS-11-18** No GregGuns recipe or loot table depends on a Techguns ore, ore cluster or oil-hole block existing in the world.

## Tests

- GGS-11-16, 17: a source scan test over the GregGuns packages for the generator, event and config names.
- GGS-11-18: the recipe dev self-check from [0001](../../design/0001-tier-map.md) rejects a recipe input that is not a GT item, ore dictionary entry or vanilla item.
- Manual: generate 100 chunks and check that every ore found is a GT ore.

## Open questions

- The Ore Cluster Scanner, the Ore Drill and the cluster blocks only make sense with clusters in the world. Their fate belongs to areas 06, 07 and 08, and a design record is needed to say whether they are dropped or turned into GT-vein tools.
