# Build and recipes: how a missile body is made

- **ICBM baseline:** [missile-item](../../icbm-spec/03-missiles/index.md) (ICBM crafts a missile from one recipe; nothing here is ported)
- **Design records:** [0018 §8](../../design/0018-icbm-program.md)
- **Decisions:** [D8](../DECISIONS.md#d8-in-detail), [D27](../DECISIONS.md), [D31](../DECISIONS.md#d31-in-detail), [D34](../DECISIONS.md), [D41](../DECISIONS.md), [D44](../DECISIONS.md), [D57](../DECISIONS.md)
- **Pack mods:** GregTech (Circuit Assembler, Laser Engraver, Assembly Line, Assembler), Galacticraft `3.4.34-GTNH` (`SchematicRegistry`, NASA Workbench)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| One shaped recipe makes a missile | A chip, a schematic, then the body: by hand in the NASA Workbench, or on the Assembly Line | D8, D27: the path every GTNH rocket takes |
| The warhead is crafted into the missile item | The body is its own item; the warhead is a block stacked on its nose on the pad | D31, 0018 §8. **Amends D8's slot list: there is no warhead slot** |
| Guidance is a separate missile family | The guidance computer sets a "guidance" tag on the body (0 to 3 steps above the body's own tier) | D24 |

## The chain

1. **Missile Chip Tn**, in the GT **Circuit Assembler**, at the tier's voltage.
2. **Missile Schematic Tn**, in the GT **Laser Engraver**, from the chip and a non-consumed lens, at the tier's voltage.
3. The schematic goes into a Galacticraft **NASA Workbench**'s schematic slot and unlocks a page (`SchematicRegistry`, one `ISchematicPage` per tier). The page builds the body by hand.
4. Or, for automation, **Assembly Line** recipes (one per guidance computer a body accepts) behind a research data stick. The research item is the hand-built T-n body: a player scans one into a GT Data Stick in the Scanner, so hand building comes first and the line opens once the player owns an Assembly Line (D27).

### Numbers (`MissileBuildSpec`, unit tested)

| | T1 | T2 |
|---|---|---|
| Chip recipe | Circuit Assembler, HV (480 EU/t), 20 s: Advanced Plastic Circuit Board, 2 Advanced circuits, 4 fine gold wire | Circuit Assembler, EV (1,920 EU/t), 30 s: Advanced Epoxy Circuit Board, 2 Data circuits, 4 fine electrum wire |
| Schematic recipe | Laser Engraver, HV, 30 s: chip + non-consumed red lens (`craftingLensRed`) | Laser Engraver, EV, 30 s: chip + non-consumed blue lens (`craftingLensBlue`) |
| Body plates | 4 stainless steel plates | 6 titanium plates |
| Fins (4 slots, one plate each) | steel | stainless steel |
| Engine | Missile Engine Mk I | Missile Engine Mk II |
| Guidance computers accepted | Mk I (0 steps), Mk II (1), Mk III (2) | Mk II (0), Mk III (1) |
| Research (Assembly Line) | scan 60 s at HV | scan 120 s at EV |
| Assembly Line recipe | HV, 30 s, 250 L lubricant, plates + fins + engine + computer | EV, 45 s, 250 L lubricant, plates + fins + engine + computer |
| Body cost, cheapest computer (D34 plate equivalents) | 18.5 (test band 14 to 22) | 26 (band 22 to 30) |

**Tier rule (D1).** The dev self-check refuses a recipe whose GregGuns input is of a higher tier than its output. A better guidance computer than its body is the point of D24, so the three computers implement the marker `TierUpgrade` and the check skips them as inputs. Chips and schematics are one item per tier (`missile_chip_t1`, `missile_chip_t2`, and the same for schematics), each Tiered at its own voltage, so the Laser Engraver and Circuit Assembler rows pass the machine-voltage rule.

**Guidance steps** = the computer's voltage tier minus the body's tier, capped at 3. The body item carries them as the integer tag `guidance` (`MissileBodies.K_GUIDANCE`), which the flight code reads for the scatter (D24).

### New GregGuns parts (Assembler, same house style as the warhead parts, D57)

Five more `IcbmPart`s, each with its own Assembler recipe at its tier, unique programmed circuit (19 to 23), GT recycling data and a D41 tooltip. They show in NEI like the warhead parts.

| Part | Tier | Circuit | Inputs | Cost |
|---|---|---|---|---|
| Missile Engine Mk I | HV | 19 | MV electric motor, MV electric pump, steel plate | 5.5 |
| Missile Engine Mk II | EV | 20 | HV electric motor, HV electric pump, stainless steel plate | 8 |
| Guidance Computer Mk I | HV | 21 | Advanced circuit, steel plate, 4 fine copper wire | 5 |
| Guidance Computer Mk II | EV | 22 | Data circuit, titanium plate, 4 fine electrum wire | 8 |
| Guidance Computer Mk III | IV | 23 | Elite circuit, tungstensteel plate, 4 fine platinum wire | 11 |

### The NASA Workbench page

- **Slots:** result, the plate slots (4 or 6), four fin slots, one engine slot, one guidance slot. Each slot takes only its own item (`MissileBenchRules`, matched on a key string so the rule is unit tested without Minecraft): plates and fins by GT prefix and material, engine and computer by registry name.
- **Result:** the body of that tier with the computer's guidance. Taking it uses one item from every slot. Closing the page returns what is left to the player.
- **Unlock:** the schematic's page ID and GUI ID are fixed (`MissileSchematicPage`: pages 4310 and 4311, GUIs 7310 and 7311). A T2 schematic never opens the T1 page.
- **Screen:** a flat panel with Galacticraft's Back and Next buttons, drawn with plain rectangles (no GC texture is reused).
- Galacticraft types sit in `production/gc` only; the items implement `ISchematicItem` through `@Optional.Interface`.

## Cost (D34)

The cost target is "about one Galacticraft rocket launch of the tier in plates, circuits and fuel". The I1 warhead recipes already spend that whole budget on the warhead alone (T1 about 20, T2 about 38 plate equivalents). The body adds 18.5 (T1) and 26 (T2) on top, so a **missile with its fuel is about 1.9 launches at T1 and 2.4 at T2**. That is a deliberate overshoot, put to the orchestrator for DECISIONS; trim the body counts in `MissileBuildSpec` (the plates, fins and part recipes) if the full-pack review finds it too dear. The Assembly Line pays the same parts and adds 250 L of lubricant, so automation gives no discount and the hand build stays worthwhile early.

## Recycling (D44)

Each body returns its plates and fins as metal (`MissileRecipes.recycle`); each part returns its metal through `PartRecipes`; the launch control returns its casing. Circuits, motors and pumps are not returned. Chips and schematics return nothing.

## NEI

- Chip: Circuit Assembler tab. Schematic: Laser Engraver tab. Body: Assembly Line tab (research shown by GT) and **NASA Workbench (Missile)** tab, a display-only GregGuns recipe map (`IcbmRecipeMaps`) with one row per body and computer. Parts: Assembler tab. Launch Control: Assembler tab.

## Requirements

- **GGS-I2-01** Every chip, schematic, part and body has a recipe in a map NEI shows.
- **GGS-I2-02** The Assembly Line rows are the only automatic route to a body and need the research.
- **GGS-I2-03** A bench slot refuses an item of the wrong kind; the result is the body of that tier with the computer's guidance and nothing from an incomplete set.
- **GGS-I2-04** Each recipe runs at its tier's voltage and every input is at or below that tier.

## Tests

`MissileBuildSpecTest` (tiers, machine fit, cost bands, guidance mapping, the bench rule). Scenarios `icbm.production.recipes` (all the recipes in the live GT maps at the right voltage) and `icbm.production.bench` (the registered pages, the unlock, the container).

## Open questions

- Whether a cluster missile ships: not in I2. A cluster body would be another body tier; D10 stays open for I3.
- How a charge is added to a warhead (D23): unchanged by this sheet; the warhead recipe (D57) fixes the fill.
