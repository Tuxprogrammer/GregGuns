# Warheads and fill

- **ICBM baseline:** [explosive-registry](../../icbm-spec/02-explosives/explosive-registry.md) (catalog and tiers), [explosive-block](../../icbm-spec/02-explosives/explosive-block.md) (the block item and its NBT). ICBM 2's modular warhead sizes (1/5/20/64/128) are in the ICBM 2 appendix.
- **Design records:** [0018 §5-8](../../design/0018-icbm-program.md), [0001 tier map](../../design/0001-tier-map.md)
- **Decisions:** [D5](../DECISIONS.md#decided), [D56, D57](../DECISIONS.md#decided), [D282](../DECISIONS.md#decided) (the warhead chain), [D283](../DECISIONS.md#decided) (de-arming), [D20](../DECISIONS.md#d20-in-detail), [D23](../DECISIONS.md#d23-in-detail), [D34](../DECISIONS.md#decided), [D41](../DECISIONS.md#decided), [D44](../DECISIONS.md#decided)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205): recipe maps, `TierEU`, `GTOreDictUnificator`; NotEnoughItems for recipe display
- **Roadmap:** [I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine); extended in I3 (T3, nuclear) and I5 (antimatter, red matter)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| One block/item per explosive, damage value = explosive ID | One warhead item per **casing tier**, NBT `{explosive, fill}` | D23: one knob, one item per tier, one NEI entry |
| Blast size fixed by config per explosive | Radius and damage are computed from `fill` at detonation | The player chooses how much to spend |
| Tier is only a tooltip color | Tier is the **casing tier**: it caps `fill` and gates the recipe | GT progression (D5) |
| Everything crafted from a grid | A warhead is **built in steps**: an inert casing (Assembler), a payload (Assembler), then Plastic Explosive charges (crafting grid, or a full casing in the Assembler) | GT first (D57); the owner's knob (D23, D282) |
| A warhead is only spent by firing it | A warhead **de-arms** step by step back to its parts: charges out in a crafting grid with a Defuse Kit (or all at once in the Cutting Machine), then the payload and the casing in the Cutting Machine | The owner wants warheads repurposed and their materials returned, more modular than the GT recycler (D283) |

## The warhead chain (D282)

A warhead is built in three steps. Each step has its own item state, and only the last can explode.

1. **Inert warhead.** The Assembler makes a bare inert warhead from the casing plates and the casing's Fuze Assembly, at the casing tier ([recipes](#recipes)).
2. **Payload.** The Assembler fits a payload: one bare inert warhead plus the payload's materials (a sleeve, canisters and their fluid, field generators, a fissile core) gives the same inert item with that payload. A payload holds **no explosive**. Condensed has no payload. Fitting a payload onto an inert warhead that already has one replaces the old payload.
3. **Charges.** **Plastic Explosive is the only charge, and one charge is one unit of fill.** Loading charges turns the inert warhead into the live warhead of its casing (`gregguns:warhead_t1` to `warhead_t5`). There are two ways to do it:
   - **Crafting grid (the knob):** one inert warhead, or one live warhead that is not full, plus 1 to 8 Plastic Explosive. A live warhead can be topped up the same way. A load that would pass the cap makes no recipe, so nothing is ever lost. A bare T2 to T5 casing makes no recipe, because it needs a payload first. A bare T1 casing with charges becomes **Condensed**, plain high explosive.
   - **Assembler:** an inert warhead plus a full casing of Plastic Explosive, on the blast's circuit. The recipe is NBT-sensitive (GT `nbtSensitive`), so a casing with a Shrapnel payload only matches the Shrapnel fill.

No recipe takes both Industrial TNT and Plastic Explosive, because Plastic Explosive is made from Industrial TNT. Industrial TNT enters the chain only through the Plastic Explosive part.

### The inert warhead

`gregguns:inert_warhead_t1` to `inert_warhead_t5`, one item per casing (each has one tier, as the D1 self-check wants: HV, EV, then ZPM from T3 up, as the live warhead block), at most 16 to a stack. The only NBT is the optional `payload` (string), which holds a blast id of the casing's tier other than condensed. A missing payload, an unknown one or one from another tier reads as bare. Nothing else is ever written to the tag, because the Assembler's fill recipe matches the tag exactly.

- **Inert by construction.** It is a plain item, not a block. It cannot be placed, so it cannot sit on a missile nose or stand on a pad. The silo's Input Bus does not take it: with a body waiting, the silo says "the warhead in the Input Bus is inert". Nothing lights, detonates or defuses it. There is no dud missile, because an inert warhead never becomes part of a missile.
- **Name:** "Inert T1 Warhead", or "Inert Shrapnel T1 Warhead" with a payload (`gregguns.icbm.inert.name` and `.name_payload`, built by `InertWarheadTooltipFormat.displayName`).
- **Tooltip (D41):** the payload line ("none (charges alone make it Condensed)" on a bare T1, "none" on the others), the tier line, `Charges: 0 / cap` and the inert line. Shift adds the next step, how charges are loaded, and the way back: the Cutting Machine takes a payload out or scraps a bare casing (D283).
- **Looks:** the item renderer draws the live warhead's model, the payload's own model where it has one, dimmed to 60% so an inert casing reads apart from a live one. The flat icon (`textures/items/inert_warhead.png`, our own: a gray casing with a blue inert band) is only the fallback.
- NEI and the creative tab list the five bare casings, then every payload of each casing.

## De-arming (D283)

Every step of the chain runs backwards, so a warhead can be repurposed or taken back to its parts. What the crafting grid puts in, the crafting grid takes out; what the Assembler fits, GT's **Cutting Machine** (`RecipeMaps.cutterRecipes`) takes off, at the same voltage as the build step (`WarheadDearmRecipes`).

| Step | Where | Input | Output | Loss |
|---|---|---|---|---|
| 1. Unload charges | crafting grid | one live warhead and a **Defuse Kit**, nothing else | the same warhead with up to 8 fewer charges, or, once the last charge is out, the inert warhead with the payload it was built with (bare for Condensed). The charges go to the player's inventory (dropped at their feet when it is full) | none; the kit loses one use per craft |
| 1. Unload charges (bulk) | Cutting Machine, the blast's voltage | one **full** warhead (NBT-exact) | the inert warhead with its payload, and the full casing of Plastic Explosive (5, 20, 64, 128 or 128; 128 as two stacks) | the cutting fluid |
| 2. Remove the payload | Cutting Machine, the blast's voltage | one inert warhead with that payload (NBT-exact) | the bare inert warhead and every item of the payload recipe at its count (the sleeve, canisters, field generators, fissile core, dishes, and the mustard seeds when HarvestCraft gave them) | the payload's fluid (nitrous oxide, ammonia, creosote, kerosene, helium, biomass) and the cutting fluid |
| 3. Scrap the casing | Cutting Machine, the casing's voltage | one bare inert warhead (NBT-exact, no tag) | its casing plates and its Fuze Assembly | the cutting fluid |
| (GT default) | Macerator, Arc Furnace | a live or inert warhead | the casing metal and the fuze's metal, from GT recycling data (D44) | charges, payload and fluid |

- **Partial unloads mirror loading.** A grid load adds 1 to 8 charges; a grid unload takes out up to 8 (all of them when 8 or fewer remain). A Thermobaric T2 warhead with 20 charges unloads to 12, then 4, then the inert Thermobaric casing. To get an exact smaller fill, unload and load again: both directions are lossless. A partly filled warhead never matches the Cutting Machine's full-warhead recipe; the grid handles it.
- **Never a dupe.** The grid reads the warhead's own tag: it needs both `explosive` and `fill`, and the explosive must be one its casing carries. A tag-less warhead (which would read as full) or a hand-edited explosive makes no recipe. The fill is clamped to the cap, as the blast reads it. Every Cutting Machine input is NBT-sensitive, so a bare casing never matches a payload's removal and hands out a sleeve it never had. A build followed by a de-arm returns exactly what went in, minus the fluids above (`WarheadDearmingTest`, `icbm.warhead.dearm.round_trip`).
- **The Defuse Kit** is a container item that stays in the grid; each craft costs one of its 32 uses, and the craft on its last use still de-arms, then the kit breaks. It is the only tool, at every tier: de-arming any warhead is a careful job, and the kit is HV, the chain's first tier.
- **Why the Cutting Machine.** The GT Disassembler is not in this GT version, and the Assembler has one output slot. The cutter has four item outputs, enough for the largest step (the inert warhead and two stacks of charges, or the bare casing and three payload items), and its water or lubricant cut is how real munitions are opened. GT adds its water, distilled water, lubricant and superfluid variants. LuV and up runs in the Industrial Cutting Factory.
- **The grid's charge hand-back** is FML's `ItemCraftedEvent` (the akimbo split's precedent, GUN-26): FML fires it before the grid gives up its ingredients, so the handler reads the warhead the result was made from and gives exactly that many charges, on the server only.
- **NEI** lists the Cutting Machine steps, one row per payload. The grid unload is a custom recipe, not listed (as the grid load, D150, D282); the warhead's, the inert warhead's and the kit's tooltips explain it.
- **Self-check.** The D1 tier map judges what a recipe makes, and a de-arm hands back parts below the warhead's tier by design, so the boot self-check skips Cutting Machine recipes that take a warhead.

## The warhead item

One item per casing tier. I1 shipped T1 and T2. The item holds an NBT tag:

| Key | Type | Meaning |
|---|---|---|
| `explosive` | string | The registry name of the explosive (`gregguns:condensed`). Missing or unknown reads as `condensed` |
| `fill` | int | Charges loaded, one per Plastic Explosive. Clamped to `1..cap` on every read; a hand-edited stack never exceeds its casing. A live warhead always has at least one charge: with none it is the inert warhead |

| Casing tier | Voltage | Fill cap | Size (blocks) |
|---|---|---|---|
| T1 | HV | 5 | 1x1x1 |
| T2 | EV to IV | 20 | 1x1x1 |
| T3 (I3) | LuV to ZPM | 64 | 1x1x2, with a filler block |
| T4 (I5) | UV | 128 | 1x1x2, with a filler block (D131) |
| T5 (I5) | UHV | 128 | 1x1x2, with a filler block (D131) |

A warhead's explosive tier equals its casing tier: a T1 casing carries T1 explosives and a T2 casing T2 explosives. The fill cap is the casing's cap. A stack that carries an explosive of another tier reads as condensed.

**Starter tier.** The T1 casing is the entry point and keeps the smallest cap so the first warhead is cheap and bounded. Its cap of 5 is ICBM 2's second size. An inert T1 warhead plus one Plastic Explosive in a crafting grid is a fill-1 Condensed warhead, the cheapest way to a working demolition charge. The ICBM 2 size of 1 is not a separate tier.

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

- **Short:** explosive name; tier (`TierText.colored`) of the casing; `Charges: n / cap`; `Blast radius: r blocks`; center damage, or the annihilation line for nuclear and up; "Hold Shift for details".
- **Shift:** the casing cap (and that more Plastic Explosive tops it up in a crafting grid), the radius and damage curve in one line, the fuse behavior for the demolition use, the blast's special effect, how to de-arm it (D283), and "Added by: GregGuns".
- The radius shown is the final blast radius at that fill and the damage is the center damage at that fill.
- **Name (D58):** `<Explosive> T<casing> Warhead` from the stack (`gregguns.icbm.warhead.name`, built by `WarheadTooltipFormat.displayName`), such as "Condensed T1 Warhead". The block names with no NBT are "T1 Warhead" and "T2 Warhead". The tier line reads `Tier: ` plus the colored tier text: HV for T1, EV-IV for T2.
- **Lit warhead (client):** while the fuse burns, the placed block shows smoke and sparks (`randomDisplayTick`); the tile's description packet tells the client once when it is lit.
- All numbers are formatted by `WarheadTooltipFormat` (no Minecraft types) with unit tests. Every line is a lang key.

## Recipes

Decisions: D56 (realistic, pack-made inputs), D57 (the parts) and D282 (the chain). Every machine step is an **Assembler** recipe at the casing's or the blast's voltage (HV 480 EU/t for T1, EV 1,920 for T2, LuV and ZPM for T3, UV and UHV for T4 and T5). Every input is at or below that tier. `InertWarheadSpec` and `WarheadRecipeSpec` hold the tables as data, and `WarheadRecipeSpecTest` checks them:

- one payload spec per blast, and only condensed takes charges alone;
- machine tier equal to the casing tier, with inputs at or below it;
- no explosive and no casing part in a payload;
- valid, unique circuits;
- at most nine item slots and one fluid;
- the D34 cost band.

| Step | Inputs | Circuit | Output | Time |
|---|---|---|---|---|
| Inert warhead | the casing plates (4 stainless steel for T1; 8 titanium, tungstensteel, naquadria or neutronium for T2 to T5) and the casing's Fuze Assembly (Mk I, Mk II, then Mk III for T3 to T5) | 2 | Inert T*n* Warhead | 400 ticks |
| Payload | one bare Inert T*n* Warhead and the payload (table below) | the blast's | Inert *Blast* T*n* Warhead | 400 ticks |
| Fill (full) | one Inert *Blast* T*n* Warhead (bare T1 for Condensed) and a full casing of Plastic Explosive (5, 20, 64, 128 or 128; past 64 in two stacks) | the blast's | the live warhead, full | 200 ticks |
| Fill (any amount) | crafting grid: one inert or live warhead and 1 to 8 Plastic Explosive | none | the live warhead with that many more charges | n/a |
| Unload (any amount) | crafting grid: one live warhead and a Defuse Kit | none | the warhead with up to 8 fewer charges (inert with its payload once empty), and the charges | n/a |
| Unload (full), payload out, scrap | Cutting Machine (see [De-arming](#de-arming-d283)) | none | the inverse of the fill, payload and inert warhead steps | 200 ticks (400 with water) |

### The parts

Eight GregGuns items (module `icbm`, package `com.gregguns.icbm.recipe`, data in `IcbmPart`). Each is an Assembler recipe at its own tier, 15 s, with a unique programmed circuit (11 to 18), and carries GT recycling data (the metal and plastic come back, explosives and fluids are lost, D44).

| Part | Tier | Recipe (circuit) | Used by |
|---|---|---|---|
| **Plastic Explosive** (x4) | HV | 2 PTFE plates, 2 Industrial TNT (11) | the charges of every warhead, and the launcher warheads |
| Fuze Assembly Mk I | HV | 1 Advanced circuit, 1 steel plate, 4 fine copper wires (12) | the Inert T1 Warhead |
| Fuze Assembly Mk II | EV | 1 Data circuit, 1 titanium plate, 4 fine electrum wires (13) | the Inert T2 Warhead |
| Shaped-Charge Liner Cone (x2) | HV | 2 copper plates (14) | the Breaching payload |
| Fragmentation Sleeve (Steel) | HV | 16 steel rounds, 1 steel plate (15) | the Shrapnel payload |
| Fragmentation Sleeve (Titanium) | EV | 16 titanium rounds, 1 titanium plate (16) | the Fragmentation payload |
| Dispersal Canister (x2) | HV | 2 aluminium plates, 1 rubber plate, 2 empty cells (17) | the Debilitation, Chemical, Smoke, Thermobaric and Endothermic payloads, and the Napalm Canister |
| **Napalm Canister** | HV | 1 Dispersal Canister, 1,000 L gasoline (18) | the Incendiary and Exothermic payloads |

The Fuze Assembly Mk III (LuV: a Master circuit, a tungstensteel plate and four fine platinum wires; circuit 20) goes into the Inert T3, T4 and T5 Warheads.

- **Plastic Explosive** is the one charge for every casing (owner, D57 and D282). PTFE is Polytetrafluoroethylene. Its monomer, tetrafluoroethylene, comes from a Chemical Reactor recipe at MV, and the polymerization recipes (`addDefaultPolymerizationRecipes`) and the plate come after, so a PTFE plate is an MV-to-HV product. Industrial TNT is a High Voltage chemical recipe, so the part is HV.
- **Napalm Canister**: the plain fuel is Gasoline (`Materials.GasolineRegular`, fuel value 576), the strongest plain fuel the pack makes at HV. Toluene (328) and Light Fuel (305) are weaker, and High Octane Gasoline (2,500) needs an EV Mixer recipe. The recipe has no plastic or thickener: the name carries the theme.

### The payloads

| Blast | Tier | Circuit | Payload (besides the inert warhead) | Full warhead cost (plate eq.) |
|---|---|---|---|---|
| Condensed | HV | 1 | none: Plastic Explosive alone | 14.5 |
| Shrapnel | HV | 3 | 1 Fragmentation Sleeve (Steel) | 18.5 |
| Anvil | HV | 4 | 12 lead plates | 20.5 |
| Incendiary | HV | 5 | 2 Napalm Canisters | 21.5 |
| Debilitation | HV | 6 | 2 Dispersal Canisters, 2,000 L nitrous oxide | 20.5 |
| Chemical | HV | 7 | 2 Dispersal Canisters, 2,000 L ammonia gas, 4 HarvestCraft mustard seeds (only when HarvestCraft is loaded) | 20.5 |
| Smoke | HV | 8 | 2 Dispersal Canisters, 8 coal dust, 2,000 L creosote | 22.1 |
| Repulsive | HV | 9 | 1 Field Generator (HV) | 22.5 |
| Attractive | HV | 10 | 1 Field Generator (HV), 4 magnetic steel rods | 24.5 |
| Fragmentation | EV | 19 | 1 Fragmentation Sleeve (Titanium) | 45 |
| Thermobaric | EV | 24 | 2 Dispersal Canisters, 4,000 L GT++ kerosene | 45 |
| Sonic | EV | 21 | 2 Emitters (EV) | 50 |
| Breaching | EV | 22 | 2 Liner Cones, 6 titanium rods | 46 |
| Contagious | EV | 23 | 4 Escherichia (rotten-flesh) petri dishes from the Bio Lab, 2,000 L biomass | 51 |
| Exothermic | LuV | 1 | 4 Napalm Canisters, 8,000 L kerosene | 108 |
| Endothermic | LuV | 2 | 4 Dispersal Canisters, 8,000 L helium | 104 |
| Nuclear | ZPM | 3 | 8 uranium-235 ingots, 4 plutonium ingots (the charges are its implosion lens) | 170 |
| EMP | LuV | 4 | 2 Field Generators (LuV), 2 Emitters (LuV) | 134 |
| Ender | LuV | 5 | 1 Emitter (LuV), 16 Eyes of Ender | 132 |
| Anti-gravitational | LuV | 6 | 2 Field Generators (LuV), 4 Nether Stars | 138 |
| [Antimatter](antimatter.md) | UV | 1 | 2 Field Generators (UV), 1 Quantum Eye, 4 plutonium-241 ingots | 256 |
| [Red matter](red-matter.md) | UHV | 2 | 2 Field Generators (UHV), 2 Quantum Eyes, 2 Emitters (UHV) | 315 |

- **Circuits.** The T1 and T2 payload and fill recipes share the circuit space with the inert warhead (2) and the parts (11 to 18). The T3, T4 and T5 recipes take inert warheads that no other recipe has, so their circuits only differ among themselves. A payload recipe and the fill recipe of the same blast share a circuit: one takes payload materials and the other takes Plastic Explosive, so they never match each other's inputs.
- **Gas-effect fills are general industrial pack fluids, chosen by in-game theme** (D56 amended): nitrous oxide, ammonia, creosote, biomass and kerosene, held in Dispersal Canisters. No chemical-warfare agent is used or named anywhere. The mustard seeds are a pun and nothing more: the item is HarvestCraft's `harvestcraft:mustardseedsItem` ("Mustard Seeds"); the recipe omits them when the mod is not loaded.
- **Item names** checked in the pack source: `ItemList.Field_Generator_HV` and `Emitter_EV` (GT5-Unofficial 5.09.54.205 `ItemList`, used by the machine recipes in `MTERecipeLoader`; the parts' own recipes live in the pack's scripts), the `circuit` prefix with the material `Data` (the repo's tables use it already), and `BioCultureLoader.rottenFleshBacteria` with `BioCultureEnum.getPetriDish`.
- **The payload recipe is not NBT-sensitive.** The Bio Lab's petri dish carries NBT. Because of that, an inert warhead that already has a payload can be refitted, and the old payload is lost. The fill recipe is NBT-sensitive. GT's `nbtSensitive` matching compares the item, damage and the whole tag (`GTRecipe.RecipeItemInput.matchesRecipe`). Inputs of the same item are summed across slots, so 128 charges can sit in two stacks.
- **Cost (D34).** A full warhead costs the inert warhead (casing plates and fuze), plus the payload, plus a full casing of Plastic Explosive at 1.1 each. The comparison is an estimate: the pack's rocket recipes live in the GTNH scripts, which this repository does not carry. GTNH's tier 1 and tier 2 rockets have a nose cone, an engine, fins, a stack of tier heavy-duty plates, a chip, and fuel worth several buckets. That comes to about 15 to 25 plate-equivalents of HV-class materials for a T1 rocket (call it 20), and about 1.5 to 2 times that at EV for T2 (call it 27). Weights, in plate equivalents: plate 1, Plastic Explosive 1.1, Fuze Mk I 5, Mk II 8 and Mk III 12, Steel and Titanium Sleeves 4 and 7, Dispersal Canister 2.5, Napalm Canister 3.5, Field Generator 8, Emitter 6, a petri dish 3, and 1 per 2,000 L of fluid. `WarheadRecipeSpecTest` holds the bands: T1 14 to 26 (about one T1 launch), T2 38 to 52 (about one and a half to two T2 launches), T3 90 to 180, and T4 and T5 100 to 400. If the pack's real rocket costs turn out higher, scale the casing constants `T1_PLATES` to `T5_PLATES` in `WarheadRecipes` (the recycling data follows them) or the payload counts in `WarheadRecipeSpec`.
- **Tier ladder.** The explosive climbs with the casing through the charge count: 5, 20, 64, then 128 Plastic Explosive, each made from Industrial TNT and PTFE. The payloads climb from steel to titanium to tungstensteel-era field generators and exotics.
- GT recycling data (D44, D283): each live warhead and each inert warhead recycles into its casing plates and its Fuze Assembly's metal; a fitted inert warhead recycles the same, so its payload is lost. Each part recycles into its metal and plastic. Explosives and fluids are lost. The Cutting Machine's de-arm is the lossless way back.
- NEI: every machine step is an Assembler recipe, so NEI shows each one with its programmed circuit and its fluid, and NEI can follow the chain from the inert warhead to the live warhead. The crafting-grid load is a custom recipe and is not listed in NEI (as for the launcher warheads, D150). The inert warhead's tooltip explains it.

## Requirements

- GGS-ICBM-02-01: a warhead stack with a `fill` above its casing cap reads as the cap.
- GGS-ICBM-02-02: a warhead with a missing or unknown `explosive` reads as condensed.
- GGS-ICBM-02-03: `radius(f)` and `damage(f)` are monotonic, bounded by `R` and `D`, equal them at `f = 1`, and the radius is never below 3.
- GGS-ICBM-02-04: a full T1 conventional warhead has center damage of at least 1,200 and a full T2 at least 3,000.
- GGS-ICBM-02-05: the tooltip is built by `WarheadTooltipFormat`, and a nuclear-and-up warhead shows the annihilation line instead of damage.
- GGS-ICBM-02-06: a recipe never produces a warhead above its cap, and passes the unification and recipe-tier self-checks.
- GGS-ICBM-02-07 (D282): the inert warhead crafts in the Assembler at its casing tier and not one tier below; a payload recipe gives an inert warhead with that payload; the fill recipe only matches an inert warhead with that exact payload (a bare T1 for condensed).
- GGS-ICBM-02-08 (D282): the crafting grid loads 1 to 8 Plastic Explosive into an inert or a live warhead, one charge per fill, and refuses a bare T2 to T5 casing, a load past the cap, two warheads and any other item.
- GGS-ICBM-02-09 (D282): an inert warhead cannot be placed, a silo does not mate it, and a warhead made by the chain arms into a missile that carries its explosive.
- GGS-ICBM-02-38 (D283): a crafting grid of one live warhead and one Defuse Kit gives the warhead with `min(8, fill)` fewer charges, or the inert warhead with its payload (bare for Condensed) once empty, and hands exactly those charges to the player; the kit stays in the grid one use down and breaks after its last use.
- GGS-ICBM-02-39 (D283): the grid unload refuses a missing kit, two kits, two warheads, an inert warhead, any other item, a warhead with no `explosive` or `fill` tag, and an explosive its casing cannot carry.
- GGS-ICBM-02-40 (D283): the Cutting Machine, at the build step's voltage and not one tier below, empties a full warhead into its inert warhead and its charges, takes a payload out into the bare inert warhead and the payload recipe's items, and scraps a bare inert warhead into its plates and fuze; every input is NBT-exact, so a partial warhead or a bare casing matches no fuller recipe.
- GGS-ICBM-02-41 (D283): a build followed by a de-arm, through any sequence of grid loads, returns every item and charge that went in; only the payload's fluid and the cutting fluid are spent.
- GGS-ICBM-02-18 (D282): no warhead payload takes TNT, dynamite, Industrial TNT, Plastic Explosive or a fuze, and no launcher warhead takes vanilla TNT, Industrial TNT or dynamite alongside Plastic Explosive.

## Tests

`WarheadMathTest`, `WarheadNbtTest`, `WarheadTooltipFormatTest`, `WarheadRecipeSpecTest`, `WarheadChargingTest`, `InertWarheadTooltipFormatTest`, `WarheadDearmingTest`, `IcbmPartTest`, `PartTooltipFormatTest`, `LauncherWarheadClashTest`; recipe self-checks run at the dev boot; scenarios `icbm.warhead.item_nbt`, `icbm.recipes.assembler`, `icbm.warhead.chain.recipes`, `icbm.warhead.chain.grid`, `icbm.warhead.chain.inert`, `icbm.warhead.chain.launch`, `icbm.warhead.dearm.grid`, `icbm.warhead.dearm.round_trip`, `icbm.warhead.dearm.machine` and `icbm.silo.refusals`.

## Open questions

None open. The curve and the starter tier are settled above (orchestrator decision, 2026-10-25, for the owner to revise at the alpha review).
