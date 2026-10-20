# 0012 · Retire the Reaction Chamber and the Retort

- Status: Proposed
- Gate: D2 (amends [0002](0002-machine-deconfliction.md) and [0011](0011-machine-policy.md); confirms [0001](0001-tier-map.md) is unchanged)
- Date: 2026-10-20

## Context
[0002](0002-machine-deconfliction.md) kept the Reaction Chamber as the one Techguns production machine. [0011](0011-machine-policy.md) gave it a single block (EV, IV) and a multiblock, the Retort. Together they run **two recipes**: the Laser Focus (EV) and the Anti Gravity Core (IV). A third, enriched uranium, was planned for M5 and never built.

The owner has decided the pair is not worth keeping. GTNH already has dozens of crafting methods, and a bespoke machine for two recipes is weak justification in a pack this rich. The mod's main pitch to the pack maintainers is tight integration, so every recipe moves onto an existing pack method. GregGuns still owns final ammo and gun production. Only intermediates move.

What the code and docs actually contain (inventory of the `machine/reaction` package, area 08 `recipes.md`, `machine-roster.md`, `multiblocks.md`, areas 06 and 12):

| Thing | Where | Notes |
|---|---|---|
| Recipe map `gregguns.recipe.reactionchamber` | `ReactionRecipeMaps` | Only these two recipes use it |
| Laser Focus recipe | `ReactionRecipes` | Numbers in the mapping table below |
| Anti Gravity Core recipe | `ReactionRecipes` | Same |
| Heat Ray Focus (Assembler, EV) | `ReactionCraftingRecipes` | Non-consumed input of both recipes. Nothing else uses it |
| Reaction Casing, chamber hull and Retort controller build recipes | `ReactionCraftingRecipes`, [recipes.md](../gregguns-spec/08-machines-production/recipes.md) | Machine build recipes only |
| Enriched uranium (planned, M5) | [reaction-chamber-multiblock.md](../gregguns-spec/08-machines-production/reaction-chamber-multiblock.md) row 7 | Never built. Dropped |
| Bio ammo or bio raw material | none | **No Reaction Chamber or Retort recipe makes it.** The Bio Tank takes GT `Materials.Biomass` directly (RC-57 in [06 recipes](../gregguns-spec/06-items/recipes.md)), and the pack already makes Biomass (for example the GT Brewery, [BreweryRecipes.java#L444](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/BreweryRecipes.java#L444)). Nothing to remap, so no Bio Lab or Bio Vat recipe is needed |

Consumers of the outputs: the **Laser Focus** is an input of the Laser Barrel (`ComponentTable`, `part_laser_focus`, RC-07, area 02). The **Anti Gravity Core** is an input of the Anti Gravity Device (`FlightTable`, area 05).

## Decision
1. **Delete** the Reaction Chamber (EV and IV single blocks), the Retort multiblock, the `gregguns.recipe.reactionchamber` recipe map, the focus, intensity, drift and risk state machine, and the Reaction Casing recipes.
2. **Laser Focus moves to the GT Laser Engraver** (`RecipeMaps.laserEngraverRecipes`, EV). A diamond cut and etched by a laser is what the item is. The map already takes a non-consumed lens, which replaces the non-consumed Heat Ray Focus.
3. **Anti Gravity Core moves to the GT Assembler** (`RecipeMaps.assemblerRecipes`, IV), with the Nether Star, an IV Emitter, IV circuits and molten Enderium. The Assembler is where GT builds IV components, and GT's LSAA scales it for free.
4. **Heat Ray Focus is deleted.** Its only job was to be the reaction's non-consumed input. The Emitter it was built around now goes straight into the recipes.
5. **Uranium enrichment is dropped.** GT supplies Uranium-235 (`dust.Uranium235`) and the ZPM nuclear cell recipes (RC-59, RC-60) already use it.
6. **The risk gameplay is gone.** No intensity buttons, no breakage, no small explosions, no radiation burst, no Retort pollution. This is an intentional gameplay loss and it is the point of the decision.
7. **The Retort is preserved, not lost.** The full Retort, including its interior parts, is archived before removal on branch `archive/retort-multiblock` and tag `archive/retort-v1`. The owner may pitch it to the GTNH community as a standalone bulk-reaction multiblock. (The orchestrator creates both refs; they do not exist when this record is written.)

## Mapping table

All GT citations are pinned to the pack tag `5.09.54.205` of GT5-Unofficial. GT++ and BartWorks live inside that repository in this pack, so one tag covers all three.

| # | Recipe | Old method (tier, EU/t, ticks, inputs) | New method | Tier | EU/t | Duration | Inputs | Outputs | Consumer |
|---|---|---|---|---|---|---|---|---|---|
| 1 | Laser Focus | Reaction Chamber or Retort, EV, 1,920, 600 ticks; diamond, non-consumed Heat Ray Focus, 1,152 mB molten Redstone; intensity 3 | **GT Laser Engraver**, `RecipeMaps.laserEngraverRecipes` | EV | 1,920 (`TierEU.RECIPE_EV`) | 600 (30 s), unchanged | `gem.Diamond` x1; `ItemList.Emitter_EV` x1; lens `craftingLensWhite` x0 (non-consumed, the stack-size-0 idiom) | `part_laser_focus` x1 | Laser Barrel (RC-07) |
| 2 | Anti Gravity Core | Reaction Chamber IV or Retort, IV, 7,680, 600 ticks; Nether Star, non-consumed Heat Ray Focus, 1,152 mB molten Enderium; intensity 8, `EXPLOSION_LOW` risk | **GT Assembler**, `RecipeMaps.assemblerRecipes` | IV | 7,680 (`TierEU.RECIPE_IV`) | 600 (30 s), unchanged | `gem.NetherStar` x1; `ItemList.Emitter_IV` x1; `circuit.IV` x2; fluid `Materials.Enderium.getMolten(1152)` | `part_anti_gravity_core` x1 | Anti Gravity Device |
| 3 | Heat Ray Focus | GT Assembler, EV (a machine intermediate) | **Removed**; its Emitter is now an input of #1 | n/a | n/a | n/a | n/a | n/a | none |
| 4 | Enriched uranium (planned, M5) | never built | **Dropped**; GT provides Uranium-235 | n/a | n/a | n/a | n/a | n/a | RC-59 and RC-60 already use `dust.Uranium235` |
| 5 | Reaction Chamber and Retort build recipes | crafting table and GT Assembler | **Removed** | n/a | n/a | n/a | n/a | n/a | none |

Sources relied on for the maps and the builder:
- Laser Engraver map: [RecipeMaps.java#L514](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L514) (4 item inputs; slot 0 is the workpiece, the others take lenses).
- Lens idiom (`new OreDictItemStack("craftingLensBlue", 0)` plus `.addTo(laserEngraverRecipes)` at `RECIPE_EV`): [LaserEngraverRecipes.java#L83-L91](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/LaserEngraverRecipes.java#L83-L91); the white lens specifically: [#L400-L404](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/LaserEngraverRecipes.java#L400-L404).
- Assembler map: [RecipeMaps.java#L1266](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1266) (9 item inputs, 1 fluid input, at least one item input).
- `Emitter_EV` and `Emitter_IV`: [ItemList.java#L468-L469](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L468-L469). `TierEU.RECIPE_EV` and `RECIPE_IV`: [TierEU.java#L29-L30](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java#L29-L30).
- Registration hook is the one GregGuns already uses (`sBeforeGTPostload`): [GTMod.java#L440-L447](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/GTMod.java#L440-L447).
- Builder usage: [GTRecipeBuilder.java#L485-L506](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeBuilder.java#L485-L506).

Both new recipes live with the other component recipes (`ComponentTable`), so the machines package registers no recipes any more.

### Tier check against 0001
- Laser Focus: 0001 puts the Laser Rifle and the UV Emitter at EV. An EV Laser Engraver recipe with an EV Emitter keeps the focus at exactly EV: not earlier, because both the machine tier and the Emitter are EV, and not later, because nothing above EV is required.
- Anti Gravity Core: the Anti Gravity Device is IV in 0001. The IV Emitter, IV circuits and a 7,680 EU/t Assembler recipe gate it at IV. The Nether Star and Enderium inputs are unchanged, so the material gate is unchanged too.
- Cost shift to watch: the Emitter used to be a one-time cost inside the non-consumed Heat Ray Focus. It is now consumed per Laser Focus. That is steeper per item, but the chamber hull, pump, sensor and casing costs go away. Tune in review if playtests find it too steep.

### Options considered and rejected
- **Gravistar (Autoclave) as the core input:** the Autoclave makes it from a Quantum Star plus molten Neutronium ([AutoclaveRecipes.java#L191-L198](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AutoclaveRecipes.java#L191-L198)), far above IV. It would unlock the Anti Gravity Device late. Rejected on tier.
- **Assembly Line for the core:** needs a research item and a line slot for one IV part. Overbuilt for the tier.
- **Bio Lab (`BartWorksRecipeMaps.bioLabRecipes`) or Bacterial Vat (`bacterialVatRecipes`):** no retired recipe produces biological material, so there is nothing to move. Maps exist at [BartWorksRecipeMaps.java#L14](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/API/recipe/BartWorksRecipeMaps.java#L14) and [#L46](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/API/recipe/BartWorksRecipeMaps.java#L46) should a later bio item need them.
- **Chemical Reactor, LCR, Electrolyzer, Mixer, Distillery:** neither recipe is a chemical conversion. Forcing one in would be a fiction.
- **Autoclave or Chemical Bath for the Laser Focus:** a lens-and-laser operation reads better, and the Autoclave map takes no lens.
- **Thaumcraft, Botania, IC2, Forestry:** rejected on progression. Thaumcraft infusion and Botania rune recipes are gated by magic progress, not GT voltage, which breaks the D1 rule that every item is gated by a voltage and a material. They would also add hard dependencies and registration code for two items. IC2 and Forestry have no machine that fits an EV laser etch or an IV gravity part.

## Items and fluids removed or replaced

| Thing | Fate |
|---|---|
| `part_heat_ray_focus` | Deleted. Remove from `ReactionParts`, `PartCatalog` (`Owner.REACTION_PARTS` entry) and lang |
| `part_laser_focus`, `part_anti_gravity_core` | **Kept**, same registry names. Ownership moves from `ReactionParts` to the components module (`ComponentItems`, `PartCatalog.Owner`), so they exist with only `modules.guns` on |
| `part_uv_emitter` | Unchanged (separate EV component, RC-17) |
| Reaction Chamber EV and IV blocks, Retort controller | Deleted (MTE IDs below) |
| Reaction Casing (meta 2 of `gregguns:casings`) | See migration notes. The meta number is reserved forever |
| Molten Redstone and molten Enderium uses | Redstone use dropped (nothing else uses it). Enderium use stays, moved to the Assembler recipe. GregGuns registers no fluid |
| Config keys `machines.retortPollutionPerSecond`, `machines.reactionFailurePollution`, `machines.reactionRadiationRadius` | Deleted from `MachinesConfig` and [docs/config.md](../config.md) |
| Textures `machines/reaction_chamber/*`, `machines/retort/*`, `gui/reaction_chamber.png` | Removed. `casings/reaction_casing.png` stays while casing meta 2 stays |
| Infographic `docs/assets/infographics/multiblock-retort.png` and its README embed | Removed from the player-facing set |

## MTE IDs retired and reserved forever

Never reused, never renumbered, within the 20100-20163 block:

| ID | Was |
|---|---|
| 20106 | Reaction Chamber, EV |
| 20107 | Reaction Chamber, IV |
| 20108 | Retort |

Implementation: remove `REACTION_CHAMBER_EV`, `REACTION_CHAMBER_IV` and `RETORT` from `GregGunsMTEIDs` and `all()`, leave a comment listing 20106-20108 as retired, and change `GregGunsMTEIDsTest` to assert the retired IDs are absent from `all()` and that no future constant takes one of them. In [PACK-CHANGES.md](../gregguns-spec/PACK-CHANGES.md), the `MetaTileEntityIDs` entry `GregGunsRetort(20108)` stays, marked retired, so the pack never hands 20108 to another addon. If the maintainers prefer to drop it, the ID stays reserved anyway.

## Migration notes for existing worlds
- The mod is `0.1.0-alpha` and public, with no stable-world promise. These notes cover worlds that already hold the blocks.
- **Machine tile entities (20106-20108):** with the registry entries gone, GT drops the unknown MTE at load and leaves a blank base tile. Items inside are lost. Say so in the changelog.
- **Item stacks:** `gregguns:part_heat_ray_focus` stacks become unknown items. Add a one-release `FMLMissingMappingsEvent` handler that ignores that name. No refund: the alpha has no stability promise.
- **Casing meta 2:** keep it registered for one alpha as an inert decorative "Reaction Casing" with the same texture, so placed Retorts do not become holes. Then remove it. Meta 2 stays reserved and is never assigned to anything else.
- **Quest progress:** the `gg.mach.rc_*` quests are removed from the pack questbook. Stored completion for them is harmless.
- **In-progress recipes:** a Laser Focus or Core being made inside a retired machine is lost with the machine.

## Consequences
- Fewer machines to ship, test and balance. M4 shrinks to the Ammo Press family and the Ammunition Works, and M6 no longer carries a Retort.
- The gate items are made in machines players already own, scaled by GT's own multiblocks.
- The roster's "bottleneck" claim for these two items no longer applies. They are one-offs per player, not a production line.
- NEI loses one tab. The two recipes show in GT's Laser Engraver and Assembler tabs.
- GregGuns adds no recipe map, MTE ID, block or casing for this feature, which suits the integration pitch.
- Risk: pack scripts that edit or remove Laser Engraver or Assembler recipes now see two more entries. Run the recipe-clash tool (`/gregguns dumprecipes`, `tools/recipe-clash`) on both maps before merge.

## Alternatives considered
- **Keep the single block, drop the Retort:** still a bespoke machine for two recipes. Rejected for the same reason as the whole decision.
- **Keep both and invent more recipes to justify them:** rejected by the owner.
- **Move the recipes into the Ammunition Works map:** they are not ammo, and the Works is for ammo.

## Knock-on checklist (implementation plan for the machines session)

One branch. Recipes first, then deletions, then docs. Docs only in this record; nothing below is done yet.

**Code (`src/main`)**
- [ ] Add the two recipes (mapping rows 1 and 2) to `ComponentTable` (or the components registrar), with `// Source:` links from the citations above, registered on `sBeforeGTPostload`.
- [ ] Move `part_laser_focus` and `part_anti_gravity_core` registration out of `ReactionParts` into the components module; delete `part_heat_ray_focus`. Update `PartCatalog` (the three rows and `Owner.REACTION_PARTS`), the `ComponentItems` javadoc and the `FlightTable` comment.
- [ ] Delete the package `com.gregguns.machine.reaction` (14 classes: `MTEReactionChamber`, `MTERetort`, `RetortShape`, `ReactionState`, `ReactionRisk`, `ReactionEffects`, `ReactionGuiWidgets`, `ReactionControlled`, `ReactionMachines`, `ReactionParts`, `ReactionRecipeMaps`, `ReactionRecipes`, `ReactionCraftingRecipes`, `ReactionScenarios`).
- [ ] `MachinesModule`: remove the reaction imports and calls (`ReactionRecipeMaps.init`, `ReactionParts.register`, `ReactionMachines.register`, `ReactionRecipes.register`, `ReactionCraftingRecipes.register`, `ReactionScenarios.registerAll`).
- [ ] `GregGunsMTEIDs` and `GregGunsMTEIDsTest`: retire 20106-20108 as above.
- [ ] `GregGunsCasings`: keep meta 2 as a decorative casing; fix the javadoc ("Retort wall, retired").
- [ ] `MachinesConfig`: delete the three reaction keys; update `docs/config.md`.
- [ ] `en_US.lang`: remove the 27 `reaction` and `retort` keys (keep the two kept parts).
- [ ] Textures: delete `machines/reaction_chamber`, `machines/retort`, `gui/reaction_chamber.png`.
- [ ] Add a missing-mappings handler ignoring `gregguns:part_heat_ray_focus` and any removed block or item names.

**Tests and scenarios**
- [ ] Delete `ReactionConfigAndLangTest`, `ReactionStateTest`, `RetortShapeTest`.
- [ ] Remove scenarios `machines.reaction.place`, `.run`, `.run.iv`, `.failure`, `.radiation`, `.retort`, `.retort.run` (rows in [docs/testing/README.md](../testing/README.md)).
- [ ] Add scenarios `components.laser_focus` (an EV Laser Engraver with the inputs makes one focus) and `components.anti_gravity_core` (an IV Assembler makes one core), plus a unit test that every input resolves at boot (diamond, Nether Star, both Emitters, IV circuit, Enderium fluid, white lens).
- [ ] Run `tools/recipe-clash` on `laserEngraverRecipes` and `assemblerRecipes`.
- [ ] Headless `runServer` boot to confirm GT loads with the map gone and the recipes in the pack's own maps.

**Docs (spec)**
- [ ] Delete `docs/gregguns-spec/08-machines-production/reaction-chamber-multiblock.md`. In `multiblocks.md` replace the Retort section with a pointer to this record and the archive refs, and change "four multiblocks" to three. Rewrite `recipes.md` (title, Heat Ray Focus, casing, controller and chamber sections, GGS-08-39, -45, -87, -89) to cover the Ammo Press family, the Works and the two component recipes only.
- [ ] `machine-roster.md`: drop the Reaction Chamber and Retort rows, the `gregguns.recipe.reactionchamber` row and the throughput row. Sweep `index.md`, `m4-exit-gate.md`, `metatileentity-framework.md`, `modularui2-guis.md`, `nei-handlers.md`, `recipe-scripting.md`, `ammo-press.md`.
- [ ] Area 06: `components-and-materials.md` (Heat Ray Focus row, Quartz Rod note), `fluids.md` (the `LIQUID_REDSTONE` row; the `LIQUID_ENDER` row now cites the Assembler), `index.md`, `recipes.md` (rows `part_laser_focus`, `part_heat_ray_focus`, `part_anti_gravity_core`, the "owned by area 08" paragraph, the Quartz Rod row).
- [ ] `05-armor/recipes.md`, `07-blocks/recipes.md`, `docs/ROADMAP.md` (the Reaction Chamber row and M4 and M6 lines), `docs/design/0002` and `0011` (add "Amended by 0012" to Status).
- [ ] [PACK-CHANGES.md](../gregguns-spec/PACK-CHANGES.md): mark `GregGunsRetort(20108)` retired.
- [ ] Questbook (`docs/gregguns-spec/12-questbook/index.md` and `quest-line.md`): delete `gg.mach.rc_build`, `rc_run`, `rc_risk`; reword `gg.mach.intro` to name only the Repair Bench and the Turret; optionally add one checkbox quest pointing at the Laser Engraver recipe.
- [ ] Assets: `docs/assets/MANIFEST.md`, `CREDITS.md`, `README.md`, and `tools/asset-gen/` (`machines.py`, `gui.py`, `build_manifest.py`, `credits/blocks.csv`, `credits/gui.csv`, `infographics/scenes.py`, `infographics/pages_more.py`): remove the reaction and Retort entries; remove the Retort infographic and the root `README.md` embed; rework any sheet that shows the Retort.
- [ ] `.changelogs/` entry: removal, retired IDs, migration behavior.
- [ ] Leave `docs/techguns-spec/` alone. It describes Techguns, and its Reaction Chamber pages stay as the baseline.

## Archive
The full Retort, including its interior parts (the `RetortShape` structure definition, the bulb-layer pieces, the hatch rules, the reaction state machine and its risks, the textures and the spec), is preserved before removal on:
- branch `archive/retort-multiblock`
- tag `archive/retort-v1`

Neither ref exists when this record is written; the orchestrator creates them from the last commit that still contains the Retort. The removal PR must not merge until both refs exist.

## Open risks
1. The Emitter is consumed per Laser Focus (see the tier check). Tune after playtesting.
2. The engraver is a workpiece-plus-lens map. A boot test must confirm the three-input recipe (diamond, Emitter, non-consumed lens) is accepted; if slot rules require otherwise, reorder the inputs.
3. `craftingLensWhite` could be renamed in a later pack. It is verified against 2.9.0-RC-2 only. A boot-time resolution check logs and skips the recipe, like the other GregGuns recipes.
4. Anyone who built a Retort loses it. Acceptable in the alpha and recorded in the changelog.
