# GregGuns spec conventions

How the GregGuns spec is laid out. The [Techguns spec](../techguns-spec/README.md) records what Techguns does, bugs included. This spec records **where GregGuns differs** and **how it plugs into GTNH**. Read the two side by side: the Techguns sheet gives the baseline, and the GregGuns sheet gives the delta and the integration.

## When a sheet exists

A sheet exists only when there is something in the pack to link it to. That means one of these:

- another mod's config in the GTNH pack;
- another mod's API, registry, event or IMC message;
- a GTNH-owned data file (loot tables, custom drops, tooltips, hazards, quests);
- a GT system that replaces Techguns behaviour (recipe maps, materials, MetaTileEntities, EU).

A Techguns feature with no integration point gets no sheet. Its behaviour is the Techguns spec plus the [design records](../design/README.md). An area with no integration points gets no folder.

## Folder layout

Area folders use the Techguns spec's numbers and names, so `gregguns-spec/05-armor/` pairs with `techguns-spec/05-armor/`. Each folder has:

- an `index.md` with these sections:
  - Overview
  - Contents
  - Integration map
  - Pack-side changes
  - Related areas
  - Open questions
- one sheet per integration topic, for example `baubles-slots.md` or `pack-loot.md`.

The **Integration map** is a table:

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|

## Sheet template

```markdown
# <Topic>

- **Techguns baseline:** links into ../../techguns-spec/…
- **Design records:** links into ../../design/…
- **Pack mods:** Mod name (`modid`, pack version), one per line
- **Roadmap:** milestone(s) that build this

## What changes from Techguns
Short prose, then a table if there is more than one item: Techguns behaviour → GregGuns behaviour → why.

## Integration points
### <Mod>: <contract in a few words>
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/<file>`, key `<key>` = `<value as shipped>`, and what it means for us.
- **Source:** pinned permalink(s) to the code that implements the contract.
- **GregGuns does:** what our code does, and with which API.
- **Pack-side change:** a config or data change the GTNH modpack must make, if any (or "none").

## Requirements
Numbered, testable statements. IDs are `GGS-<area>-<nn>`, for example `GGS-10-03`.

## Tests
How each requirement is verified: a unit test, a `/gregguns test` scenario, a runServer check or a manual check.

## Open questions
Anything that needs a decision. Name the record that would settle it.
```

## Recipe sheets

Recipe sheets (`recipes.md` in an area) give **every** GregGuns recipe in that area, not a summary. One row or block per craftable:

| Field | Content |
|---|---|
| Output | GregGuns item (placeholder registry name) × count |
| Tier | D1 tier from [0001](../design/0001-tier-map.md) |
| Method | Crafting table (with GT tool letters) or a GT recipe map (`RecipeMaps.assemblerRecipes`, …) |
| Inputs | GT parts written as `OrePrefixes.<prefix>` + `Materials.<material>` (for example `plate.Steel`), GT components as `ItemList.<name>` (for example `Electric_Motor_LV`), circuits as `circuit.<tier>`, fluids as `Materials.<m>.getMolten(L)` or a named fluid with litres, GregGuns components by name |
| EU/t, time | `TierEU.RECIPE_<tier>` (or a fraction of it) and duration in seconds/ticks |
| Gate | Which input and which machine voltage enforce the tier |
| Notes | Programmed circuit number, GT precedent it mirrors, Techguns recipe it replaces |

Rules for recipes:
- **GT shapes.** Early tiers use shaped crafting with GT tools the way GT's own early recipes do. From LV up, the final step is a GT machine at the item's tier voltage. LuV and up use the Assembly Line, with a research item, the way GT gates its own LuV+ parts.
- **Inputs exist.** Every input is a real GT prefix-and-material pair, `ItemList` entry or GregGuns component. Check it against the GT5-Unofficial clone at `reference/src/GT5-Unofficial@5.09.54.205/`.
- **GT precedent.** Cite the GT recipe for a comparable item at the same tier (for example a GT electric tool, battery or component), with a pinned permalink. Costs scale like it.
- **Techguns link.** Link the Techguns recipe being replaced, so the reader can compare.

## Rules

- **Evidence.** Follow [INTEGRATION.md](INTEGRATION.md): source first, pinned to the pack's tag. Every claim about another mod cites a `Source:` permalink or the pack config file and key. A claim that rests on decompilation says so.
- **Reference paths.** `reference/` is git-ignored, so its paths are written as code (`reference/gtnh/…`), never as links. Links go to GitHub permalinks.
- **Pack-side changes.** GregGuns never writes into another mod's config folder at runtime. When the pack needs a config or data change (a loot XML, a drop list, NEI handler ordering), the sheet says which file, what change and why. These changes are collected in [PACK-CHANGES.md](PACK-CHANGES.md) and land as a PR to the GTNH modpack repository, not in this repo.
- **Soft dependencies.** Integrations with optional mods are guarded with `Loader.isModLoaded("<modid>")` (or `@Optional`), and say so in the sheet. GT, GTNHLib, ModularUI2 and IC2 are hard dependencies.
- **Requirement IDs** are stable. Once a requirement is published, don't renumber it. Retire it with "(withdrawn)".
- **Don't restate the Techguns spec.** Link to it. Only write what differs.
- **Techguns bugs.** When an integration also fixes a Techguns bug, cite its ID from [BUGS.md](../techguns-spec/BUGS.md).
