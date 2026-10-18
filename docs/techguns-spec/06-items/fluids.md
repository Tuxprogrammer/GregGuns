# Fluids (`TGFluids`) and fluid containers

`TGFluids` holds static references to the fluids Techguns uses. It **creates** at most two fluids (`creeper_acid` always, `milk` only if no other mod does). Every other field just points at another mod's fluid, with a fallback when that fluid is missing.

## Lifecycle

| Phase | What happens |
|---|---|
| preInit, `TGFluids.init()` (before [`TGBlocks.init`](../07-blocks/block-catalogue.md#registered-blocks)) | `WATER` and `LAVA` are set to Forge's water and lava. `ACID` is constructed but **not registered** yet. |
| preInit, `TGBlocks.init()` | `FluidRegistry.registerFluid(ACID)`. The block [`BlockFluidAcid`](../07-blocks/special-blocks.md#14-creeper-acid-acid-blockfluidacid) is created and set as the acid's block; the block is registered as [`acid`](../07-blocks/block-catalogue.md#registered-blocks) ([07-blocks](../07-blocks/index.md)). |
| client texture stitch | `BlockFluidAcid.registerBlockIcons` registers `techguns:acid_still` / `techguns:acid_flow` and assigns them to the fluid as still/flowing icons. If milk was added by Techguns, an event handler registers `techguns:milk_still` / `techguns:milk_flow` and assigns them to `MILK`. |
| postInit, `TGFluids.postInit()` | Looks up the optional fluids (table below) and applies the fallbacks. |
| postInit, [`TGItems.postInit()`](item-catalogue.md) | Creates and registers the acid bucket. Registers the fluid-container mappings. |

## Fluid table

| Field | Registry name | Created by Techguns? | Properties | Fallback if missing |
|---|---|---|---|---|
| `WATER` | `water` | no (Forge) | – | – |
| `LAVA` | `lava` | no (Forge) | – | – |
| `ACID` | `creeper_acid` | **yes, always** | unlocalized `creeperAcid`; gaseous false; luminosity 0; density **100**; viscosity and temperature left at Forge defaults (1000 and 295 K) | – |
| `MILK` | `milk` | only if no `milk` fluid exists at postInit (then `addedMilk = true`) | unlocalized `milk`; gaseous false; density 1050; viscosity 1050 | creates its own |
| `OIL` | `oil` | no | – | **none**: stays `null` |
| `FUEL` | `fuel` | no | – | **none**: stays `null` (callers check for null and use lava instead) |
| `LIQUID_REDSTONE` | `redstone` | no | – | `LAVA` (prints "No Liquid Redstone, fallback to lava") |
| `LIQUID_COAL` | `coal` | no | – | intended oil, else water. The code always ends up with `WATER` (bug [ITEM-03](index.md#suspected-bugs)). Prints "No Liquid coal, fallback to oil/water" |
| `LIQUID_ENDER` | `ender` | no | – | `LAVA` (prints "No Liquid ender, fallback to lava") |
| `BIOFUEL` | `biofuel` | no | – | `LAVA` (prints "No Biofuel, fallback to lava") |

Because of the fallbacks, recipes that name "liquid redstone", "liquid ender" or "biofuel" accept lava when those mods are absent. `LIQUID_COAL` is never used outside `TGFluids`.

When Techguns adds milk, it also registers vanilla milk bucket ↔ bucket as a Forge container for 1000 mB of `milk`.

## Lang

| Key | English |
|---|---|
| `fluid.creeperAcid` | Creeper Acid |
| `techguns.fluid.creeperAcid` | Creeper Acid (unused alias) |
| `techguns.tile.creeperAcid.name` | Creeper Acid (block name) |
| `techguns.item.bucket_acid.name` | Creeper Acid Bucket |
| `fluid.milk` | Milk |

## Fluid containers

| Filled item | Empty item | Fluid and amount | Registered |
|---|---|---|---|
| Creeper Acid Bucket (`Techguns:bucket_acid`) | vanilla bucket | `creeper_acid` 1000 mB | `TGItems.postInit` |
| Fuel Tank (shared meta 8) | Fuel Tank (Empty) (meta 9) | `fuel` 250 mB if a `fuel` fluid exists, otherwise `lava` **500 mB** | `TGItems.postInit` |
| vanilla milk bucket | vanilla bucket | `milk` 1000 mB | `TGFluids.postInit`, only if Techguns created milk |

The Bio Tank, Compressed Air Tank and Nuclear Power Cell are **not** Forge fluid containers. They are filled only by Chem Lab recipes (see [ammo.md](ammo.md)).

### Creeper Acid Bucket item
- Class `GenericItemBucket` extends vanilla `ItemBucket`, bound to the acid fluid block. Its unlocalized name gets the `techguns.` prefix, so the full key is `techguns.item.bucket_acid`.
- Container item: vanilla bucket. Max stack 1. Creative tab Techguns. Texture [`textures/items/bucket_acid.png`](textures.md#11-tools-placers-door-bucket-debug-items).
- Behaviour is exactly the vanilla filled bucket: right-click places a source block of acid on the clicked face (it cannot replace a solid block) and leaves an empty bucket (in survival).
- Picking acid back up: Techguns registers **no** `FillBucketEvent` handler. The acid block uses `Material.water` and its source has meta 0, so the vanilla empty-bucket logic treats an acid source as water. It removes the acid and gives a **water bucket** (bug [ITEM-04](index.md#suspected-bugs)). An acid bucket can therefore only be obtained from the creative tab, or by filling through a Forge fluid-container-aware machine using the registered container mapping.
- How the acid block behaves (damage, displacement, gore) is in [07-blocks](../07-blocks/index.md).

## Where the fluids are used (cross-reference, all 08 unless noted)

| Fluid | Users |
|---|---|
| Creeper Acid | Chem Lab: bio mass + gunpowder + water → acid; itemBioFuel → acid; ore uranium + acid → yellowcake; leather + slime + acid → treated leather. Reaction Chamber: titanium-iron ore in acid. |
| Water | many Chem Lab recipes; Reaction Chamber (seeds, uranium) |
| Lava | Chem Lab: carbon fibers, nether charge, TGX/fuel-tank fallback; Reaction Chamber (quartz rod, redstone block); Ore Drill fuel |
| Oil | Chem Lab (slime + coal + water → oil; raw rubber + oil → raw plastic); ore cluster liquid ([07](../07-blocks/index.md)/[11](../11-worldgen/index.md)); oil-hole worldgen ([11](../11-worldgen/index.md)) |
| Fuel | Chem Lab (TGX, fuel-tank fill/drain); Ore Drill fuel |
| Biofuel | Ore Drill fuel (falls back to lava) |
| Liquid redstone | Reaction Chamber (diamond → laser focus) |
| Liquid ender | Reaction Chamber (nether star → anti-gravity core) |
| Milk | Chem Lab (green dye + raw rubber + milk → slimeball) |

## Fluid textures (in `textures/blocks/`, shown here because the fluid icons are assigned from `TGFluids`)

| Path | Size | Animation | Look |
|---|---|---|---|
| [`textures/blocks/acid_still.png`](../07-blocks/textures.md#9-fluids) | 16×512 (32 frames of 16×16) | `.mcmeta` frametime 2 | Semi-transparent (alpha about 165–226, average 178) murky mustard/olive yellow, around #918200, with subtle darker ripples. Same pattern as vanilla water, tinted acid-yellow. |
| [`textures/blocks/acid_flow.png`](../07-blocks/textures.md#9-fluids) | 32×1024 (32 frames of 32×32) | `.mcmeta` default frametime 1 | Same colour, with vertical streaks for flowing liquid |
| [`textures/blocks/milk_still.png`](../07-blocks/textures.md#9-fluids) | 16×512 (32 frames) | frametime 2 | The same ripple pattern in neutral light grey (#cdcdcd), semi-transparent |
| [`textures/blocks/milk_flow.png`](../07-blocks/textures.md#9-fluids) | 32×1024 (32 frames) | default frametime | Light grey flowing streaks |

The acid textures are also used by the acid fluid block ([07-blocks](../07-blocks/index.md)).
