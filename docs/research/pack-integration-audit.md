# Pack integration audit: bio ammo and the intermediates sweep

- Date: 2026-10-20
- Pack: GTNH 2.9.0-RC-2. GT5-Unofficial, BartWorks, GT++ and GoodGenerator all ship from one repository, tag `5.09.54.205`. Every link below is pinned to that tag.
- Scope: docs only. No Java changed. Final ammo and guns are out of scope (they stay ours).
- Related: [0012 retire Reaction Chamber and Retort](../design/0012-retire-reaction-chamber-and-retort.md) (branch `docs/retire-reaction-chamber-retort`) is taken as given and not redone here.

## 1. Bio-ammo recommendation

### What GregGuns does today

The Bio Gun is EV ([0001](../design/0001-tier-map.md): "bio tanks" at EV). Its Bio Tank (`tank_bio`, `ComponentItems.TANK_BIO`) accepts `Materials.Biomass` only, filled by any fluid-container machine (RC-57). One full tank loads 30 rounds. The tank itself is built from `Cell_Empty`, titanium and polyethylene (RC-53).

Two problems:

1. Biomass is the cheapest bio fluid in the pack. The Brewery makes it from Forestry mulch or fertilizer plus water at 4 EU/t ([BreweryRecipes L444-L460][brew]), Pyrolyse makes it at 10 EU/t ([PyrolyseRecipes L172-L181][pyro]), and GT++ makes it from saplings. An EV weapon whose ammo is an LV fluid never touches the pack's actual biology chain.
2. Docs disagree with code. [fluids.md](../gregguns-spec/06-items/fluids.md) maps `BIOFUEL` to `BioDiesel` for the Bio Tank; `ComponentItems` and RC-57 use `Biomass`.

### What the pack has (verified in source)

All of this is in the `bartworks` mod ID, which is inside the GT5-Unofficial jar the mod already depends on. **No new dependency jar is needed.**

| Piece | Where | Tier | Notes |
|---|---|---|---|
| Bio Lab (single block) | [`MTEBioLab`, registered HV to ZPM][biolab-reg] | HV and up | Recipe map `bioLabRecipes` ([BartWorksRecipeMaps L14][maps]) |
| Culture petri dishes | [Bio Lab: Empty Petri Dish + organic item + water][biolab-dish] | HV (25 s) | Rotten flesh gives Escherichia cadaver (33%); fermented spider eye gives **E. coli** (45%); dough gives common yeast (75%); dirt gives Pseudomonas veronii (EV, 1 min 15 s) |
| Bacterial Vat (multiblock) | `MTEBioVat`, map `bacterialVatRecipes` ([L46][maps]) | MV-class recipes, needs tiered glass | The petri dish sits in the controller slot as a **non-consumed** "special" item; the recipe carries a minimum glass tier (`GLASS` metadata, [GTRecipeConstants L263][glass]) |
| Culture fluids | [`FluidLoader`][fluidloader] registers one fluid per breedable culture ("<Culture> Fluid") | n/a | Made only by the vat sugar recipe below |
| Pack's own culture-fluid recipe | [Vat: 64 sugar + dish + circuit 1 + water gives 10 mB culture fluid][vat-sugar], MV, glass 3, 50 s | MV / glass 3 | Yield is tiny on purpose: culture fluids feed small-batch products |
| Culture-fluid consumers | [Centrifuge: 1,000 mB E. coli fluid gives Plasma Membrane plus 10 mB enzyme solution][centrifuge] (HV); similar for yeast fluid | HV | The only fluid consumers in the pack |
| Fermented Biomass to Fulvic Acid | [Vat: Pseudomonas culture + 10,000 mB Fermented Biomass][vat-fulvic] | LV recipe | The only pack vat recipe that eats a Biomass-line fluid |

Rejected as not fitting: Forestry (bio-fuel and ethanol are GT's `BioDiesel` and `Ethanol` already; no gun-tier machine), GT++ bio-gas and GoodGenerator (generator fuels, not ammo), Gendustry liquid DNA (the pack's `getLiquidDNA` falls back to Biomass anyway, [GTModHandler L219-L223][dna]), and Thaumcraft (gated by magic progress, same objection as 0012).

### Options

**Option A (recommended): the Bio Tank takes a Bacterial Vat culture fluid, and GregGuns adds one vat recipe that makes it from Biomass.**
- Accepted fluid: the **E. coli culture fluid** (`BioCultureLoader.eColi.getFluid()`, resolved at post-init and null-checked, never by a hard-coded name). Biomass is no longer accepted by default (config switch `bioGun.acceptBiomass`, default false, so a dev environment without BartWorks still works).
- New recipe in `bacterialVatRecipes`: E. coli petri dish in the controller (non-consumed), 1,000 mB Biomass + 100 mB distilled water + circuit 2, gives 1,000 mB culture fluid, EV voltage, `GLASS` 4. Numbers are a starting point for tuning.
- Player path: Forestry mulch or fertilizer to Biomass (Brewery, LV) -> **Bio Lab (HV)** makes the E. coli dish (empty dish + fermented spider eye + water, repeat until the 45% roll lands; one dish lasts forever) -> **Bacterial Vat (EV glass)** turns Biomass into culture -> Fluid Canner fills the Bio Tank -> Bio Gun.
- Tier fit: the Bio Lab is HV, one tier below the EV gun; the vat recipe and glass are EV, matching the gun. The tank stays EV.
- Why not the pack's own sugar recipe alone: 10 mB per 64 sugar means 6,400 sugar for a 1,000 mB tank. That recipe is built for small-batch products, so ammo would be unplayable. The extra GregGuns recipe is the amplification step and is the only new recipe.
- Cost: one small recipe, one fluid resolution, one config key, scenario and doc updates. Soft integration (`Loader.isModLoaded("bartworks")`), falling back to Fermented Biomass (Option C) if the culture fluid is `null`.

**Option B: a Bio Lab-only gate; Biomass stays the ammo.**
- The Bio Tank keeps Biomass. The Bio Tank recipe (RC-53) gains a consumed `ItemList.CultureEscherichiaKoli` petri dish ([ItemList L3150][itemlist-bio]), so the Bio Lab is required once per tank batch.
- Cheapest. But the ammo is still LV Biomass, and a cultured dish in a crafting recipe is a one-time toll, not a production chain. Fixes the oversight in letter, not in spirit.

**Option C: GT-only chain, Fermented Biomass.**
- The tank accepts `Materials.FermentedBiomass` (Fermenter, [FermenterRecipes L22-L23][ferm], or Pyrolyse). No BartWorks code at all.
- Safest and trivially buildable, but it does not use the Bio Lab or the vat, which was the owner's explicit point. Keep it only as the fallback inside Option A when BartWorks is missing.

**Recommendation: Option A.** It is the only option where the Bio Lab, the dish and the Bacterial Vat are all on the path to firing, it limits GregGuns' contribution to one amplification recipe on a pack map, and it needs no extra jar.

### Open checks before building Option A

1. Run `/gregguns dumprecipes` in the pack and confirm the vat map accepts a third-party recipe with a special item and `GLASS` metadata, and which breedable cultures are tier 0 (the pack's sugar recipe filters on `bioCulture.getTier() == 0`; E. coli's tier was not confirmed here).
2. Confirm the registered fluid name of the E. coli culture fluid at runtime (the factory builds it from the culture name; a runtime lookup avoids guessing).
3. Confirm no pack script or config overrides the Bio Lab or vat maps (the pack `config` folder was not searched for this).

## 2. Sweep table

Verdicts: **keep** (ours, already built from GT parts or has no pack equivalent), **feed** (stays ours, but a pack item or chain becomes an input), **replace** (use the pack item and delete ours). Items come from `PartCatalog`; fluids come from [fluids.md](../gregguns-spec/06-items/fluids.md) (GregGuns registers none). Final ammo, magazines and guns are skipped. `part_heat_ray_focus` is retired by 0012 and not counted.

| # | Item | Today | Verdict | Pack source (mod, machine or map, tier) | Why |
|---|---|---|---|---|---|
| 1 | `part_stock_wooden` | planks, crafting | keep | none | No pack equivalent; ULV hand craft |
| 2 | `part_barrel_stone` | crafting | keep | none | ULV, same |
| 3 | `part_mechanical_1` | crafting | keep | none | ULV, same |
| 4 | `part_barrel_iron` | Assembler LV, GT long rod, rings, screws | keep | GT `stickLong`, `ring`, `screw` (already) | Already built from GT parts |
| 5 | `part_barrel_hardened` | Assembler LV | keep | GT `pipeSmall.Steel` (already) | Same |
| 6 | `part_mechanical_2` | Assembler LV, gears, springs, screws | keep | GT `gearGtSmall` (already) | No GT item has this meaning ([components-and-materials](../gregguns-spec/06-items/components-and-materials.md)) |
| 7 | `part_mechanical_3` | Assembler EV | keep | GT small gears (already) | Same |
| 8 | `part_receiver_iron` | Assembler LV | keep | GT plates (already) | Gun-specific |
| 9 | `part_receiver_steel` | Assembler MV | keep | GT plates (already) | Same |
| 10 | `part_receiver_hardened` | Assembler HV | keep | GT plates (already) | Same |
| 11 | `part_receiver_carbon` | Assembler EV | keep | GT titanium, epoxy plate (already) | Same |
| 12 | `part_barrel_carbon` | Assembler EV | keep | GT `EpoxidFiberReinforced` (already) | Same |
| 13 | `part_barrel_laser` | Assembler EV | keep | Laser Focus (item 24), GT lens (already) | Same |
| 14 | `part_stock_plastic` | Assembler MV | keep | GT polyethylene (already) | Same |
| 15 | `part_stock_carbon` | Assembler EV | keep | GT epoxy plate (already) | Same |
| 16 | `part_pump_mechanism` | Assembler LV, springs, pipe, rubber | keep | `Electric_Pump_LV` considered; a pump-action stroke is not electric | Mechanical meaning |
| 17 | `part_uv_emitter` | Assembler EV | keep | `ItemList.Emitter_EV`, diamond lens (already) | Already pack-fed |
| 18 | `part_sonic_emitter` | Assembler EV | keep | `Emitter_EV`, `Electric_Piston_EV` (already) | Already pack-fed |
| 19 | `part_rad_emitter` | Assembly Line ZPM | keep | `Emitter_ZPM`, U-235 (already) | Already pack-fed |
| 20 | `part_cybernetic` | Assembler EV: fine wire, EV circuits, polyethylene, redstone, soul sand | feed | **Bio Lab** (HV): `ItemList.PlasmaMembrane` ([L3206][itemlist-bio]), made by Centrifuging E. coli fluid ([L91-L98][centrifuge]) | The one part whose theme is biological. A Plasma Membrane input ties the Bio Lab chain into a second EV product |
| 21 | `part_power_plating` | Assembler IV | keep | GT tungsten steel, epoxy (already) | Gun-specific |
| 22 | `part_nuclear_warhead` | Assembly Line ZPM | keep | GT U-235, TNT (already) | Already pack-fed |
| 23 | `part_heavy_cloth` | crafting grid from wool | keep | none verified | No pack cloth intermediate found at the tier; low value |
| 24 | `part_laser_focus` | moves to Laser Engraver EV (0012) | keep | `laserEngraverRecipes` (0012) | Already moved by 0012 |
| 25 | `part_anti_gravity_core` | moves to Assembler IV (0012) | keep | `assemblerRecipes` (0012) | Already moved by 0012 |
| 26 | `part_gas_mask_filter` | Assembler MV | keep | `ActivatedCarbonFilterMesh` (already) | Already pack-fed |
| 27 | `part_treated_leather` | Chemical Bath LV, sulfuric acid | keep | `Materials.SulfuricAcid` (already) | Already on a pack map |
| 28 | `part_steam_armor_plating` | Assembler EV | keep | GT plates (already) | Gun-specific |
| 29 | `part_glider_backpack` | flight-gear Assembler | keep | not audited in depth | Flight gear is area 05; revisit with armor |
| 30 | `part_glider_wing` | flight-gear Assembler HV | keep | not audited in depth | Same |
| 31 | `tank_bio` | Assembler EV, `Cell_Empty`, titanium plate and ring, polyethylene | feed | Fluid: culture fluid from the **Bacterial Vat** (section 1). Body: [`Large_Fluid_Cell_Titanium`][lfc] | The ammo fluid is the main fix; the body can take the pack's large cell as one input |
| 32 | `tank_fuel` | Assembler HV, `Cell_Empty`, stainless | feed | [`Large_Fluid_Cell_StainlessSteel`][lfc] | RC-52 already mirrors the Large Fluid Cell recipe; use the cell itself |
| 33 | `tank_air` | Assembler HV, `Cell_Empty`, double plates | feed | `Large_Fluid_Cell_StainlessSteel` | Same; check capacity against 1,000 mB first |
| 34 | `cell_nuclear` | Assembly Line ZPM | keep | GT U-235, Naquadah alloy (already) | Pack-fed; the deeper tie is item 35 |
| 35 | `cell_nuclear_depleted` and RC-60 refuel | returned item, refuel with U-235 dust | feed | GT depleted rod items (`ItemList.DepletedRodUranium*`, [L1896-L1908][itemlist-rod]) via the pack's reactor and Centrifuge loop | Let the depleted cell return through the pack's own reprocessing instead of a private refuel step. Needs a spike; unverified |
| 36 | `bandages` | crafting LV | keep | none verified | No GT or Forestry bandage found |
| 37 | `medkit` | Assembler MV | keep | `Materials.Ethanol` (already) | Already pack-fed |
| F1 | Fluid: Biomass (Bio Tank) | `Materials.Biomass` | feed | Vat culture fluid (section 1); Biomass stays as the vat input | Main deliverable |
| F2 | Fluid: Diesel, Gasoline, BioDiesel, Ethanol (Fuel Tank) | GT fluids | keep | GT refinery chain | Already GT |
| F3 | Fluid: Air, Oxygen (tanks) | `Materials.Air`, Oxygen | keep | GT | Already GT |
| F4 | Fluid: sulfuric acid, solder, Enderium, lava (recipe inputs) | GT fluids | keep | GT | Already GT |
| F5 | Fluid: molten Redstone, creeper acid, oil | dropped | keep | n/a | Already removed |

**Counts (37 items + 5 fluid rows = 42): keep 36, feed 6, replace 0.**
Items: keep 32, feed 5 (20, 31, 32, 33, 35). Fluids: keep 4 (F2 to F5), feed 1 (F1).

Why zero **replace**: every GregGuns intermediate is a gun-specific part with no pack equivalent, or is already built from GT inputs. The one candidate, the tank bodies, still need our fluid filter and `IFluidContainerItem` logic, so the item stays ours and takes the pack cell as an input.

## 3. Prioritized next steps

Each is one small implementation branch.

| # | Branch | Owner area | Change | Size |
|---|---|---|---|---|
| 1 | `m2/bio-ammo-culture-fluid` | guns and ammo (gregguns-03) | `TANK_BIO` accepts the E. coli culture fluid (post-init lookup, null-checked), Biomass behind `bioGun.acceptBiomass`; update `BioGunScenarios` and `BioGunDef` tank text; fix the `BioDiesel` / `Biomass` mismatch in fluids.md and recipes.md RC-57 | S |
| 2 | `m2/bio-vat-recipe` | machines and items (gregguns-8b) | Register the Biomass-to-culture recipe in `bacterialVatRecipes` (dish in controller, EV, `GLASS` 4) from the `sBeforeGTPostload` hook; recipe-clash dump test; do open checks 1 to 3 first | S |
| 3 | `docs/bio-chain-pack-and-quest` | machines and items (gregguns-8b) | Add the chain to PACK-CHANGES and the questbook spec (Bio Lab dish, Vat, tank, gun), plus a deviation note in 06-items | S |
| 4 | `m4/tanks-from-large-cells` | machines and items (gregguns-8b) | Rebuild RC-52, RC-53, RC-54 to take `Large_Fluid_Cell_*` as the body; check cell capacity against the 1,000 mB tank | S |
| 5 | `m4/cybernetic-plasma-membrane` | machines and items (gregguns-8b) | Add `PlasmaMembrane` to the Cybernetic Parts recipe (RC-20); depends on step 2 for Bio Lab reachability | XS |
| 6 | `spike/nuclear-cell-reprocessing` | machines and items (gregguns-8b) | Read-only spike: can `cell_nuclear_depleted` come back through GT depleted-rod outputs? Writes findings only | S |

## 4. Risks

- **Culture-fluid name and registration timing.** The fluids are created in BartWorks' loader and named from the culture; the tank must resolve through the culture object at post-init, not by string. A late or `null` fluid disables the tank, so the Fermented Biomass fallback must exist.
- **Third-party recipe on a BartWorks map.** The vat map uses a special slot and `GLASS` metadata; a malformed recipe could be silently ignored or break NEI. It needs the dump check (open check 1) and a headless boot.
- **Balance.** The 1:1 amplification and the EV glass gate are guesses. The 45% E. coli roll is a one-time grind (the dish is not consumed), so it is mild; if it feels too soft, raise the glass tier or the voltage.
- **Pack overrides.** GTNH scripts could change Bio Lab or vat recipes. Not searched yet.
- **Progression shift.** Biomass is LV, and the vat needs a built multiblock with EV glass: the gate moves from a cheap fluid to a real structure. Intended, but confirm against the owner's EV pacing in the beta playthrough.
- **Soft dependency.** BartWorks lives inside the GT jar, so a build-time dependency is free, but code that touches `bartworks.*` must stay behind `Loader.isModLoaded("bartworks")` so unit tests with a stubbed `Materials` keep passing (GGS-06-15).
- **Unverified rows.** Items 23, 29, 30, 35 and 36 were not checked against pack alternatives in depth; those verdicts are provisional.
- **No runtime check.** This audit read source only. Nothing was run in game.

[brew]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/BreweryRecipes.java#L444-L460
[pyro]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/PyrolyseRecipes.java#L172-L181
[ferm]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/FermenterRecipes.java#L22-L23
[maps]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/API/recipe/BartWorksRecipeMaps.java#L14
[glass]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeConstants.java#L263
[biolab-reg]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/loaders/recipes/CraftingRecipes.java#L420-L459
[biolab-dish]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/loaders/BioRecipeLoader.java#L45-L121
[fluidloader]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/loaders/FluidLoader.java#L80-L100
[vat-sugar]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/system/material/processingLoaders/AdditionalRecipes.java#L221-L233
[vat-fulvic]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/loaders/BioRecipeLoader.java#L155-L163
[centrifuge]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/loaders/recipes/Centrifuge.java#L91-L98
[dna]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L219-L223
[itemlist-bio]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L3150
[itemlist-rod]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L1896-L1908
[lfc]: https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L204-L212
