# icbm-balance

Prices ICBM missiles, warheads and the pack's Galacticraft rockets from a recipe dump of a real GTNH pack, for the
balance pass (D328).

## Use

1. Boot a server with the full pack and GregGuns, with `debug.recipeDump=true` in the GregGuns config and the ICBM
   module on.
2. Run `/gregguns dumprecipes <file>` on the console. Besides the crafting and GT rows, the dump holds GT's unification
   data (`itemdata`: prefix, material and amount per item) and the NASA Workbench pages: Galacticraft's
   (`gc.nasa_workbench.*`) and GalaxySpace's rockets T1 to T8 (`gs.nasa_workbench.t<n>`), which GTNH builds every rocket
   from.
3. Run the tool:

```sh
python -I tools/icbm-balance/cost.py dump.json report.md [--json report.json]
```

4. Check the tool itself: `python -I tools/icbm-balance/test_cost.py`.

## How an item is priced

- For a player at the item's own tier: only machine recipes at or below that tier's voltage count (rockets at their
  tier, bodies T1 HV, T2 EV, T3 LuV; warheads at their casing tier, nuclear at ZPM).
- The cheapest such recipe wins. Recipe maps that take things apart or only show (macerator, cutting machine, recycler,
  scanner, research, the component assembly line and the like) never make an item; nor does a row whose output is one
  of its inputs.
- The expansion stops at GT materials (in ingots: a plate is 1, a rod 0.5, molten metal 144 L is 1), circuits (counted
  by tier, never expanded), fluids (liters, never expanded) and raw items (no recipe, no GT data). Crafting tools are
  free. EU is summed over every machine step.
- **Units** = ingots + circuits (2^(tier+1) each: HV 16, EV 32, IV 64, LuV 128) + 1 per 1,000 L + 4 per raw item. It is
  the one number used to pick between recipes and to compare items; the report also gives ingots, the main materials,
  circuits, fluids and EU.
- A rocket is also priced without its lander module, the reference a missile is held to (a missile does not come
  back).
- Warheads are priced as the bare inert warhead, its payload row and its full fill row, found by the blast's
  programmed circuit (the table `WARHEADS` mirrors `WarheadRecipeSpec`).

## Limits

- Units weigh every metal alike and every circuit of a tier alike; a rare metal (uranium-235, naquadria) counts as one
  ingot. Read the material columns too.
- The cheapest route is the one a player could take, not always the one they do (a pack's alternate recipes can be
  cheaper than the usual chain).
- Items made only in machines the dump does not list (a multiblock with its own recipe logic) end as raw items.
