# Build and recipes: how a missile body is made

- **ICBM baseline:** [missile-item](../../icbm-spec/03-missiles/index.md) (ICBM crafts a missile from one recipe; nothing here is ported)
- **Design records:** [0018 §8](../../design/0018-icbm-program.md)
- **Decisions:** [D328, D329](../DECISIONS.md) (balance pass), [D8](../DECISIONS.md#d8-in-detail), [D27](../DECISIONS.md), [D31](../DECISIONS.md#d31-in-detail), [D34](../DECISIONS.md), [D41](../DECISIONS.md), [D44](../DECISIONS.md), [D57](../DECISIONS.md), [D64](../DECISIONS.md), [D281](../DECISIONS.md)
- **Pack mods:** GregTech (Circuit Assembler, Laser Engraver, Assembler, Assembly Line from LuV), Galacticraft `3.4.34-GTNH` (`SchematicRegistry`, NASA Workbench)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| One shaped recipe makes a missile | A chip, a schematic, then the body: by hand in the NASA Workbench, or by machine (the Assembler below LuV, the Assembly Line from LuV) | D8, D27, D281: the path every GTNH rocket takes, on machines the player has at the tier |
| The warhead is crafted into the missile item | The body is its own item; the warhead is a block stacked on its nose on the pad | D31, 0018 §8. **Amends D8's slot list: there is no warhead slot** |
| Guidance is a separate missile family | The guidance computer sets a "guidance" tag on the body (0 to 3 steps above the body's own tier) | D24 |

## The chain

1. **Missile Chip Tn**, in the GT **Circuit Assembler**, at the tier's voltage.
2. **Missile Schematic Tn**, in the GT **Laser Engraver**, from the chip and a non-consumed lens, at the tier's voltage.
3. The schematic goes into a Galacticraft **NASA Workbench**'s schematic slot and unlocks a page (`SchematicRegistry`, one `ISchematicPage` per tier). The page builds the body by hand.
4. Or, for automation, **one machine recipe per tier** for the guidance-0 body (plates, fins, engine, two circuits and 250 L lubricant; no computer). The machine depends on the tier (D281): GT's Assembly Line and its scanner research arrive at LuV in GTNH (pack quest 1412), so
   - **below LuV (T1, T2)** the body is an **Assembler** recipe at the tier's voltage, with no research and no programmed circuit (the HV and EV single-block Assemblers have the 9 item slots and the fluid tank it needs);
   - **from LuV (T3)** it is an **Assembly Line** recipe behind a research data stick. The research item is the hand-built body of that tier: a player scans one into a GT Data Stick in the Scanner. Each line tier has its own research item because GT's scanner keeps only the first recipe that matches a research item.
5. A guidance computer is fitted by an **Assembler** recipe: body + computer -> the body with that guidance (one recipe per computer that gives at least one step; 15 s at the body's voltage, programmed circuit 1 for T1 and 2 for T2).

### Numbers (`MissileBuildSpec`, unit tested)

| | T1 | T2 |
|---|---|---|
| Chip recipe | Circuit Assembler, HV (480 EU/t), 20 s: Advanced Plastic Circuit Board, 2 Advanced circuits, 4 fine gold wire | Circuit Assembler, EV (1,920 EU/t), 30 s: Advanced Epoxy Circuit Board, 2 Data circuits, 4 fine electrum wire |
| Schematic recipe | Laser Engraver, HV, 30 s: chip + non-consumed red lens (`craftingLensRed`) | Laser Engraver, EV, 30 s: chip + non-consumed blue lens (`craftingLensBlue`) |
| Body plates (one column, D69) | 3 compressed steel plates (Galacticraft's, GT prefix `compressed`) | 4 compressed titanium plates |
| Fins (4 slots, one each, D69) | Galacticraft Tier 1 rocket fins (`GalacticraftCore:item.rocketFins`) | Galacticraft Tier 2 rocket fins (`GalacticraftMars:item.itemBasicAsteroids` damage 2) |
| Engine (D69) | Galacticraft Tier 1 engine (`GalacticraftCore:item.engine` damage 0) | Galacticraft Tier 2 engine (`GalacticraftMars:item.itemBasicAsteroids` damage 1) |
| Guidance computers accepted | Mk I (0 steps), Mk II (1), Mk III (2) | Mk II (0), Mk III (1) |
| Machine body (guidance 0, D281) | Assembler, HV, 30 s, no research: plates + fins + engine + 2 Advanced circuits, 250 L lubricant | Assembler, EV, 45 s, no research: plates + fins + engine + 2 Data circuits, 250 L lubricant |
| Guidance fit (Assembler) | HV, 15 s, circuit 1: body + Mk II (1 step) or Mk III (2 steps) | EV, 15 s, circuit 2: body + Mk III (1 step) |
| Body cost, cheapest computer (in-code estimate, plate equivalents; measured in [Cost](#cost-d328)) | 161 (test band 150 to 175) | 490 (band 460 to 520) |

**Tier rule (D1).** The dev self-check refuses a recipe whose GregGuns input is of a higher tier than its output. A better guidance computer than its body is the point of D24, so the three computers implement the marker `TierUpgrade` and the check skips them as inputs. Chips and schematics are one item per tier (`missile_chip_t1`, `missile_chip_t2`, and the same for schematics), each Tiered at its own voltage, so the Laser Engraver and Circuit Assembler rows pass the machine-voltage rule.

**Guidance steps** = the computer's voltage tier minus the body's tier, capped at 3. The body item carries them as the integer tag `guidance` (`MissileBodies.K_GUIDANCE`), which the flight code reads for the scatter (D24).

### New GregGuns parts (Assembler, same house style as the warhead parts, D57)

Three more `IcbmPart`s (the two engines were retired in D69: bodies take Galacticraft's), each with its own Assembler recipe at its tier, unique programmed circuit (19 to 23), GT recycling data and a D41 tooltip. They show in NEI like the warhead parts.

| Part | Tier | Circuit | Inputs | Cost |
|---|---|---|---|---|
| Guidance Computer Mk I | HV | 21 | Advanced circuit, steel plate, 4 fine copper wire | 5 |
| Guidance Computer Mk II | EV | 22 | Data circuit, titanium plate, 4 fine electrum wire | 8 |
| Guidance Computer Mk III | IV | 23 | Elite circuit, tungstensteel plate, 4 fine platinum wire | 11 |

### The NASA Workbench page

- **Slots:** result, the plate slots (3 or 4, in one vertical column, D69), four fin slots, one engine slot, one guidance slot. Each slot takes only its own item (`MissileBenchRules`, matched on a key string so the rule is unit tested without Minecraft): plates by GT prefix and material (`compressed:Steel`), fins and engine by Galacticraft registry name and damage (`GalacticraftParts`), the computer by registry name.
- **Result:** the body of that tier with the computer's guidance. Taking it uses one item from every slot. Closing the page returns what is left to the player.
- **Unlock:** the schematic's page ID and GUI ID are fixed (`MissileSchematicPage`: pages 4310 and 4311, GUIs 7310 and 7311). A T2 schematic never opens the T1 page.
- **Screen:** a flat panel with Galacticraft's Back and Next buttons, drawn with plain rectangles (no GC texture is reused).
- Galacticraft types sit in `production/gc` only; the items implement `ISchematicItem` through `@Optional.Interface`.

### The T3 row (I3, D74)

LuV chip (Multifiberglass board, 2 Master circuits, 8 platinum fine wire), a yellow-lens schematic, four compressed titanium plates, four Heavy Duty Rocket Fins Tier 3 and a Heavy Duty Rocket Engine Tier 3 (NewHorizonsCoreMod). The machine body is the one **Assembly Line** recipe of the missile chain: LuV, 60 s, the same parts plus 2 Master circuits and 250 L lubricant, behind a 180 s research scan of a hand-built T3 body at LuV. The only guidance computer is Mk III, below the body's tier, so a T3 body has no guidance steps. See [missile-silo](missile-silo.md).

## Cost (D328)

The target is "about one Galacticraft rocket launch of the tier in plates, circuits and fuel" (0018 §5). The I7 balance pass measured it in the real pack: `/gregguns dumprecipes` on a GTNH 2.9.0-RC-2 server writes every recipe, GT's unification data and the NASA Workbench rocket pages, and `tools/icbm-balance/cost.py` prices each item by its cheapest recipe a player at the item's tier can run (only recipes at or below that voltage), down to GT materials in ingots, circuits by tier and fluids. **Units** = ingots + circuits (16 per HV circuit, doubling per tier: EV 32, IV 64, LuV 128) + 1 per 1,000 L + 4 per raw item. The reference is the rocket of the tier without its lander module, since a missile does not come back. The earlier estimates (161 and 490 plate equivalents, D69) were far off: the pack's rocket parts are built from compressed heavy duty plates and cost hundreds of ingots each.

| | Ingots | Circuits | Units | Reference rocket (no lander), units | Body / rocket |
|---|---|---|---|---|---|
| Missile Body T1 (HV) | 377 | none | 407 | Tier 1 (HV), 799 | 0.51 |
| Missile Body T2 (EV) | 882 | none | 1,081 | Tier 2 (EV), 2,264 | 0.48 |
| Missile Body T3 (LuV) | 2,998 | 1 IV | 3,519 | Tier 4 (LuV), 5,082 | 0.69 |

The parts: a Galacticraft Tier 1 engine is 131 units and a Tier 1 fin 66; the "Tier 2" engine (`itemBasicAsteroids` 1) 516 at EV and its fin 137; the Heavy Duty Rocket Engine Tier 3 2,093 at LuV and its fin 204. The machine body (Assembler or Assembly Line) pays the same parts, two circuits instead of the computer and 250 L of lubricant, so automation gives no discount. With the warhead (D328, [warheads-and-fill](../02-explosives/warheads-and-fill.md#cost-d328)), a missile costs 0.8 to 1.4 rockets of its tier and the warhead is about as costly as the body or more (0.7 to 1.6 at T1 and T2, up to 22 times at red matter). No body changed in the balance pass (D329): the T2 body's "Tier 2" parts are the pack's Tier 3 and 4 rocket parts, made with desh, so the first T2 missile needs a trip to Mars on the EV Tier 2 rocket; the T3 body's engine is a ZPM-rocket part that a LuV player can build. Fuel is not in the units: a missile burns one tank (2,000, 4,000 or 8,000 mB by tier), as a rocket burns its own.

## Recycling (D44)

Each body returns its compressed plates as metal (`MissileRecipes.recycle`; the Galacticraft fins and engine carry no GT data); each part returns its metal through `PartRecipes`; the launch control returns its casing. Circuits, motors and pumps are not returned. Chips and schematics return nothing.

## NEI

- Chip: Circuit Assembler tab. Schematic: Laser Engraver tab. Body: Assembler tab (T1, T2), Assembly Line tab (T3, research shown by GT) and **NASA Workbench (Missile)** tab: one page per body and computer (the by-hand build, guidance 0 included), drawn like Galacticraft's own rocket workbench pages. The slots sit in the shape of the missile on a rocket silhouette (warhead seat at the nose, plates down the middle, fins either side at the bottom, engine at the base, the guidance computer beside the nose). The rows come from a display-only GregGuns recipe map (`IcbmRecipeMaps`, no GT tab of its own) and the page is the client's `MissileWorkbenchHandler`. The in-game page uses the same layout (`MissileBenchLayout`, unit tested for overlap and symmetry) and the same plain-rectangle art (`MissileBenchArt`), so nothing is copied from Galacticraft's jar. Parts: Assembler tab. Launch Control: Assembler tab.

## Requirements

- **GGS-I2-01** Every chip, schematic, part and body has a recipe in a map NEI shows.
- **GGS-I2-02** The machine body recipe (guidance 0) and the Assembler guidance fit are the only automatic route to a body. Below LuV the body recipe is in the Assembler with no research, and GT's lookup on its inputs finds it; from LuV it is on the Assembly Line, and each line recipe has its own research item that GT's scanner resolves.
- **GGS-I2-06** (D281) No GregGuns Assembly Line recipe runs below LuV: `AssemblyLineTierTest` for the tables and bodies, `icbm.production.recipes` for the live map.
- **GGS-I2-03** A bench slot refuses an item of the wrong kind; the result is the body of that tier with the computer's guidance and nothing from an incomplete set.
- **GGS-I2-04** Each recipe runs at its tier's voltage and every input is at or below that tier.
- **GGS-I2-05** (D69) No ICBM recipe runs below HV: `MissileBuildSpecTest.noIcbmRecipeRunsBelowHv` fails for any part, body or warhead recipe under HV.

## Tests

`MissileBuildSpecTest` (tiers, machine fit, which body uses which machine, cost bands, guidance mapping, the bench rule) and `AssemblyLineTierTest`. Scenarios `icbm.production.recipes` (all the recipes in the live GT maps at the right voltage; one Assembler body recipe per tier below LuV that GT's lookup finds from its inputs, and no line recipe; one Assembly Line recipe per tier from LuV that GT's scanner reaches from its research item; no GregGuns line recipe below LuV; the Assembler fits) and `icbm.production.bench` (the registered pages, the unlock, the container).

## Open questions

- Whether a cluster missile ships: not in I2. A cluster body would be another body tier; D10 stays open for I3.
- How a charge is added to a warhead (D23): unchanged by this sheet; the warhead recipe (D57) fixes the fill.
