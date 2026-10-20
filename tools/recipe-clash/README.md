# recipe-clash

Finds GregGuns recipes that collide with, or are hidden by, the rest of a GTNH pack.

## Use

1. Boot a server with the full pack and GregGuns, with `debug.recipeDump=true` in the GregGuns config.
2. Run `/gregguns dumprecipes <file>` on the console. It writes a JSON dump (`.json`, or gzip if you name it `.json.gz`).
3. Run the tool on the dump:

```sh
python -I tools/recipe-clash/clash.py dump.json.gz report.md [--limit 40] [--ignore-counts] [--ours-fluid OUT[=IN]]
```

4. Check the tool itself: `python -I tools/recipe-clash/test_clash.py`.

## Categories

| Category | Meaning |
|---|---|
| exact | Same recipe map, same inputs (items, counts, fluids, circuit), different output. A real clash. |
| shadow | Same map; inputs match after expanding ore names or ignoring stack sizes. |
| grid, shapeless | Crafting-table collisions. |
| duplicate | Same match and same output. Harmless. |
| subset | Same map; another row's item and fluid inputs (circuit 0 or ours) are all inside ours, at or below our counts. GT takes the first match and ignores extra items, so that row can fire on our inputs and win. Other GregGuns rows count as the shadowing side too, marked `(own)`. This is the "soft shadow" the beta review calls S1 to S8. |
| superset | The reverse: our row's inputs are inside a pack row's, so ours can fire on the pack's inputs and steal it. Informational. |

Fix a subset by giving our row an input the other lacks, or a circuit it does not accept, so that neither row's inputs
are contained in the other's.

## Which rows are ours

A row is ours if its source is `gregguns`, its map name starts with `gregguns`, or any output item is from the
`gregguns` mod. A fluid-only recipe has no item output, so the dump calls it `unknown`; the table
`OUR_FLUID_OUTPUTS` in `clash.py` names those (the Bio Tank's culture vat recipe: output `fluid:escherichiakolifluid`
with a `fluid:biomass` input, because the pack makes the same fluid from sugar and water). Add more with
`--ours-fluid OUT[=IN]`.

## Limits

- The dump does not record a recipe's special item (the controller-slot item). On `SPECIAL_SLOT_MAPS` (the Bacterial
  Vat) a subset is only a candidate and the report says `verify in game`: the vat map is special-slot sensitive, so
  a row with a different special item never matches. Confirm with a `findRecipeQuery` scenario, as
  `gun.bio_gun.vat_recipe_selected` does.
- Ore dictionary names are expanded only for names our own recipes use.
- `--ignore-counts` compares which inputs are present, not how many. It adds near-misses (a pack row wanting 3 of an
  item where ours has 1) and gives many more rows; use it to look for them, not as the gate.

## Last run (dump-v3, master 2edc5852)

Default flags: exact 0, subset 11, superset 18. The subset rows are S1 (Blaster Rifle against `AFSU:ALC`), S2 (HEV
Boots against T3 Combat Boots), S3 (Assault Rifle against SMG empty magazine), S5 (the vat recipe against BartWorks'
Biomass to Fermented Biomass row, flagged special slot), S8 (`casings:10` against the GT concrete row) and the
circuit-0 turret-armor and T3 armor rows of S6 and S7 (`(own)`). The superset rows are the turret armor rows (S6, S7)
firing on pack Assembler rows. With `--ignore-counts`, S4 (Laser Focus against the 3-diamond row) appears as well.
