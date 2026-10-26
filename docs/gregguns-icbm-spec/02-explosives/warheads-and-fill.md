# Warheads and fill

- **ICBM baseline:** [explosive-registry](../../icbm-spec/02-explosives/explosive-registry.md) (catalog and tiers), [explosive-block](../../icbm-spec/02-explosives/explosive-block.md) (the block item and its NBT). ICBM 2's modular warhead sizes (1/5/20/64/128) are in the ICBM 2 appendix.
- **Design records:** [0018 §5-8](../../design/0018-icbm-program.md), [0001 tier map](../../design/0001-tier-map.md)
- **Decisions:** [D5](../DECISIONS.md#decided), [D56, D57](../DECISIONS.md#decided), [D20](../DECISIONS.md#d20-in-detail), [D23](../DECISIONS.md#d23-in-detail), [D34](../DECISIONS.md#decided), [D41](../DECISIONS.md#decided), [D44](../DECISIONS.md#decided)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205): recipe maps, `TierEU`, `GTOreDictUnificator`; NotEnoughItems for recipe display
- **Roadmap:** [I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine); extended in I3 (T3, nuclear) and I5 (antimatter, red matter)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| One block/item per explosive, damage value = explosive ID | One warhead item per **casing tier**, NBT `{explosive, fill}` | D23: one knob, one item per tier, one NEI entry |
| Blast size fixed by config per explosive | Radius and damage are computed from `fill` at detonation | The player chooses how much to spend |
| Tier is only a tooltip color | Tier is the **casing tier**: it caps `fill` and gates the recipe | GT progression (D5) |
| Everything crafted from a grid | Every warhead is one **Assembler** recipe at its casing tier, from eight hardware parts and pack explosives | GT first (D57) |

## The warhead item

One item per casing tier. I1 ships T1 and T2. The item holds an NBT tag:

| Key | Type | Meaning |
|---|---|---|
| `explosive` | string | The registry name of the explosive (`gregguns:condensed`). Missing or unknown reads as `condensed` |
| `fill` | int | Charges loaded. Clamped to `1..cap` on every read; a hand-edited stack never exceeds its casing |

| Casing tier | Voltage | Fill cap | Size (blocks) |
|---|---|---|---|
| T1 | HV | 5 | 1x1x1 |
| T2 | EV to IV | 20 | 1x1x1 |
| T3 (I3) | LuV to ZPM | 64 | 1x1x2, with a filler block |
| T4 (I5) | UV | 128 | larger (I5) |
| T5 (I5) | UHV | 128 | larger (I5) |

A warhead's explosive tier equals its casing tier: a T1 casing carries T1 explosives and a T2 casing T2 explosives. The fill cap is the casing's cap. A stack that carries an explosive of another tier reads as condensed.

**Starter tier.** The T1 casing is the entry point and keeps the smallest cap so the first warhead is cheap and bounded. Its cap of 5 is ICBM 2's second size. A fill of 1 is a legal "starter charge" and has its own cheap recipe; it is the cheapest way to a working demolition charge. The ICBM 2 size of 1 is not a separate tier.

## The radius curve (settles the open question)

With `f = fill / cap` (0 < f <= 1), for an explosive with full-casing radius `R` and full-casing center damage `D`:

```
radius = max(MIN_RADIUS, R * f^(1/3))
center damage = D * f^(1/2)
```

- **Radius** grows with the cube root of fill: blast energy spread through a volume, so doubling the fill gives 26% more radius. This is sub-linear on purpose (D23): a full casing does not dwarf the next tier up, and a cheap partial fill still gives a useful blast. `MIN_RADIUS` is 3 blocks, as in ICBM.
- **Center damage** grows with the square root of fill. A full T1 or T2 casing meets the D34 target (1,200 and 3,000); a partial fill gives less. The warhead never claims the target below full fill.
- The D20 fuel bonus (I2) multiplies the final **radius** by `1 + bonus`, capped by config. The warhead function takes the bonus as a parameter, default 0.
- Both formulas are pure functions in `WarheadMath` with unit tests (monotonic, bounded by `R` and `D`, exact at `f = 1`).

Sample (condensed, T1: `R` 9, `D` 1,200):

| Fill | Radius | Center damage |
|---|---|---|
| 1 | 5.3 | 537 |
| 3 | 7.6 | 930 |
| 5 | 9 | 1,200 |

## Tooltip (D41)

- **Short:** explosive name; tier (`TierText.colored`) of the casing; `Fill: n / cap`; `Blast radius: r blocks`; center damage, or the annihilation line for nuclear and up; "Hold Shift for details".
- **Shift:** the casing cap, the radius and damage curve in one line, the fuse behavior for the demolition use, the blast's special effect, and "Added by: GregGuns".
- The radius shown is the final blast radius at that fill and the damage is the center damage at that fill.
- **Name (D58):** `<Explosive> T<casing> Warhead` from the stack (`gregguns.icbm.warhead.name`, built by `WarheadTooltipFormat.displayName`), such as "Condensed T1 Warhead". The block names with no NBT are "T1 Warhead" and "T2 Warhead". The tier line reads `Tier: ` plus the colored tier text: HV for T1, EV-IV for T2.
- **Lit warhead (client):** while the fuse burns, the placed block shows smoke and sparks (`randomDisplayTick`); the tile's description packet tells the client once when it is lit.
- All numbers are formatted by `WarheadTooltipFormat` (no Minecraft types) with unit tests. Every line is a lang key.

## Recipes

Decisions: D56 (realistic, pack-made inputs) and D57 (the final design). NBT cannot flow through a GT recipe, so a warhead is not loaded one charge at a time. Each explosive has **one Assembler recipe that makes a full-casing warhead**, and NEI shows one entry per explosive (D23: one example, not one per fill level). The partial fill a player may want is covered by one cheap **starter** recipe (condensed, fill 1) and by creative and command stacks.

**Rules.** Every warhead is made in the **Assembler**, at its casing tier's voltage (HV 480 EU/t for T1, EV 1,920 EU/t for T2), 400 ticks, with its own programmed circuit. Every input is at or below that tier. The casing is 4 stainless steel plates (T1) or 8 titanium plates (T2). Steel and titanium fragments belong to T2 and below; tungstensteel waits for T3. There is no Implosion Compressor or Chemical Reactor warhead recipe. (The Implosion Compressor only reads its explosive additive when the recipe has a single item input, and GT copies every implosion recipe into the Electric Implosion Compressor at UEV, so the old T2 recipes never matched their intent.) `WarheadRecipeSpec` holds the table as data and `WarheadRecipeSpecTest` checks it: exactly one full recipe per blast, machine tier equal to the casing tier, inputs at or below it, at most nine item slots and one fluid, unique circuits, and the D34 cost band.

### The parts

Eight GregGuns items (module `icbm`, package `com.gregguns.icbm.recipe`, data in `IcbmPart`). Each is an Assembler recipe at its own tier, 15 s, with a unique programmed circuit (11 to 18), and carries GT recycling data (the metal and plastic come back, explosives and fluids are lost, D44).

| Part | Tier | Recipe (circuit) | Used by |
|---|---|---|---|
| **Plastic Explosive** (x4) | HV | 2 PTFE plates, 2 Industrial TNT (11) | every T1 and T2 warhead with a payload, in different counts |
| Fuze Assembly Mk I | HV | 1 Advanced circuit, 1 steel plate, 4 fine copper wires (12) | every T1 warhead |
| Fuze Assembly Mk II | EV | 1 Data circuit, 1 titanium plate, 4 fine electrum wires (13) | every T2 warhead |
| Shaped-Charge Liner Cone (x2) | HV | 2 copper plates (14) | Breaching |
| Fragmentation Sleeve (Steel) | HV | 16 steel rounds, 1 steel plate (15) | Shrapnel |
| Fragmentation Sleeve (Titanium) | EV | 16 titanium rounds, 1 titanium plate (16) | Fragmentation |
| Dispersal Canister (x2) | HV | 2 aluminium plates, 1 rubber plate, 2 empty cells (17) | Debilitation, Chemical, Smoke, Thermobaric, Napalm Canister |
| **Napalm Canister** | HV | 1 Dispersal Canister, 1,000 L gasoline (18) | Incendiary |

- **Plastic Explosive** is one generic part for both tiers (owner, D57). PTFE is Polytetrafluoroethylene: its monomer (tetrafluoroethylene) comes from a Chemical Reactor recipe at MV, and the polymerization recipes (`addDefaultPolymerizationRecipes`) and a plate come after, so a PTFE plate is an MV-to-HV product. Industrial TNT is a High Voltage chemical recipe, so the part is HV.
- **Napalm Canister**: the plain fuel is Gasoline (`Materials.GasolineRegular`, fuel value 576), the strongest plain fuel the pack makes at HV. Toluene (328) and Light Fuel (305) are weaker, and High Octane Gasoline (2,500) needs an EV Mixer recipe. The recipe has no plastic or thickener: the name carries the theme.

### The warhead table

| Warhead | Tier | Circuit | Inputs (casing plates first; one Fuze of its tier in every recipe) | Cost (plate eq.) |
|---|---|---|---|---|
| Condensed | HV | 1 | 4 stainless plates, 8 TNT, 2 dynamite, 3 Plastic Explosive, Fuze Mk I | 20.7 |
| Condensed starter (fill 1) | HV | 2 | 2 stainless plates, 2 TNT, Fuze Mk I | 8.6 |
| Shrapnel | HV | 3 | 4 plates, 6 TNT, 2 Plastic Explosive, 1 Fragmentation Sleeve (Steel), Fuze Mk I | 20.0 |
| Anvil | HV | 4 | 4 plates, 6 TNT, 2 Plastic Explosive, 12 lead plates, Fuze Mk I | 22.0 |
| Incendiary | HV | 5 | 4 plates, 2 Napalm Canisters, 3 Plastic Explosive, Fuze Mk I | 19.3 |
| Debilitation | HV | 6 | 4 plates, 2 Dispersal Canisters, 3 Plastic Explosive, Fuze Mk I, 2,000 L nitrous oxide | 18.3 |
| Chemical | HV | 7 | 4 plates, 2 Dispersal Canisters, 3 Plastic Explosive, Fuze Mk I, 2,000 L ammonia gas, 4 HarvestCraft mustard seeds (only when HarvestCraft is loaded) | 18.3 |
| Smoke | HV | 8 | 4 plates, 2 Dispersal Canisters, 1 Plastic Explosive, Fuze Mk I, 8 coal dust, 2,000 L creosote | 17.7 |
| Repulsive | HV | 9 | 4 plates, 2 Plastic Explosive, 1 Field Generator (HV), Fuze Mk I | 19.2 |
| Attractive | HV | 10 | as Repulsive plus 4 magnetic steel rods | 21.2 |
| Fragmentation | EV | 19 | 8 titanium plates, 10 Industrial TNT, 4 Plastic Explosive, 1 Fragmentation Sleeve (Titanium), Fuze Mk II | 39.4 |
| Thermobaric | EV | 24 | 8 plates, 10 Industrial TNT, 4 Plastic Explosive, 2 Dispersal Canisters, Fuze Mk II, 4,000 L GT++ kerosene | 39.4 |
| Sonic | EV | 21 | 8 plates, 6 Industrial TNT, 2 Plastic Explosive, 2 Emitters (EV), Fuze Mk II | 37.4 |
| Breaching | EV | 22 | 8 plates, 10 Industrial TNT, 3 Plastic Explosive, 2 Liner Cones, 6 titanium rods, Fuze Mk II | 39.3 |
| Contagious | EV | 23 | 8 plates, 6 Industrial TNT, 2 Plastic Explosive, 4 Escherichia (rotten-flesh) petri dishes from the Bio Lab, Fuze Mk II, 2,000 L biomass | 38.4 |

- **Gas-effect fills are general industrial pack fluids, chosen by in-game theme** (D56 amended): nitrous oxide, ammonia, creosote, biomass and kerosene, held in Dispersal Canisters. No chemical-warfare agent is used or named anywhere. The mustard seeds are a pun and nothing more: the item is HarvestCraft's `harvestcraft:mustardseedsItem` ("Mustard Seeds"); the recipe omits them when the mod is not loaded.
- **Item names** checked in the pack source: `ItemList.Field_Generator_HV` and `Emitter_EV` (GT5-Unofficial 5.09.54.205 `ItemList`, used by the machine recipes in `MTERecipeLoader`; the parts' own recipes live in the pack's scripts), the `circuit` prefix with the material `Data` (the repo's tables use it already), and `BioCultureLoader.rottenFleshBacteria` with `BioCultureEnum.getPetriDish`.
- **Cost (D34).** The comparison is an estimate: the pack's rocket recipes live in the GTNH scripts, which this repository does not carry. The shape of GTNH's tier 1 and tier 2 rockets (nose cone, engine, fins, a stack of tier heavy-duty plates, a chip, fuel worth several buckets) is about 15 to 25 plate-equivalents at HV-class materials for a T1 rocket (call it 20) and about 1.5 to 2 times that at EV for T2 (call it 27). Weights, in plate equivalents: plate 1, TNT 0.8, Industrial TNT 1.2, dynamite 1, Plastic Explosive 1.1, Fuze Mk I 5 and Mk II 8, Steel and Titanium Sleeves 4 and 7, Dispersal Canister 2.5, Napalm Canister 3.5, Field Generator 8, Emitter 6, a petri dish 3, a fluid 1 per 2,000 L. T1 warheads come to 18 to 22 (about one T1 launch); T2 warheads to 37 to 39 (about 1.5 T2 launches), with the casing plates counted (T1 4 stainless, T2 8 titanium, doubled in the I1 fix round). `WarheadRecipeSpecTest` holds T1 to 16 to 26 and T2 to 34 to 46. If the pack's real rocket costs turn out higher, scale the counts in `WarheadRecipeSpec` and the casing constants `T1_PLATES` and `T2_PLATES` in `WarheadRecipes` (the recycling data follows the same constants).
- **Tier ladder.** T1 uses TNT (MV chain), dynamite (nitration, LV) and Plastic Explosive (Industrial TNT, so the HV nitration chain); T2 climbs to Industrial TNT in bulk. Higher tiers (I3 and later): the same part shapes upgrade to tungstensteel sleeves and penetrators, a Mk III Fuze, and GT++ rocket fuels.
- GT recycling data (D44): each warhead recycles into its casing plates (stainless steel for T1, titanium for T2); each part recycles into its metal and plastic. Explosives and fluids are lost.
- NEI: every recipe is an Assembler recipe, so NEI shows it, its programmed circuit and its fluid; each part appears in the creative tab and in NEI with its tooltip.

### T3 recipes (I3, D80)

Assembler, 400 ticks, casing of 8 tungstensteel plates, one Fuze Assembly Mk III (LuV: Master circuit, tungstensteel plate, four fine platinum wires; circuit 20) in every recipe. Circuits 1 to 3 (unique among T3).

| Warhead | Tier | Inputs after the casing and the Fuze |
|---|---|---|
| Exothermic | LuV | 16 Industrial TNT, 6 Plastic Explosive, 4 Napalm Canisters, 8,000 L kerosene |
| Endothermic | LuV | 16 Industrial TNT, 6 Plastic Explosive, 4 Dispersal Canisters, 8,000 L helium |
| Nuclear | ZPM | 12 Industrial TNT, 4 Plastic Explosive, 8 uranium-235 ingots, 4 plutonium ingots |

Each recycles into its tungstensteel plates (D44). The T3 cost band is 55 to 150 plate equivalents.

## Requirements

- GGS-ICBM-02-01: a warhead stack with a `fill` above its casing cap reads as the cap.
- GGS-ICBM-02-02: a warhead with a missing or unknown `explosive` reads as condensed.
- GGS-ICBM-02-03: `radius(f)` and `damage(f)` are monotonic, bounded by `R` and `D`, equal them at `f = 1`, and the radius is never below 3.
- GGS-ICBM-02-04: a full T1 conventional warhead has center damage of at least 1,200 and a full T2 at least 3,000.
- GGS-ICBM-02-05: the tooltip is built by `WarheadTooltipFormat`, and a nuclear-and-up warhead shows the annihilation line instead of damage.
- GGS-ICBM-02-06: a recipe never produces a warhead above its cap, and passes the unification and recipe-tier self-checks.

## Tests

`WarheadMathTest`, `WarheadNbtTest`, `WarheadTooltipFormatTest`, `WarheadRecipeSpecTest`, `IcbmPartTest`, `PartTooltipFormatTest`; recipe self-checks run at the dev boot; scenarios `icbm.warhead.items` and `icbm.recipes.assembler`.

## Open questions

None open. The curve and the starter tier are settled above (orchestrator decision, 2026-10-25, for the owner to revise at the alpha review).
