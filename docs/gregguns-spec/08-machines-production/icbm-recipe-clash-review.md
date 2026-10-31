# ICBM recipe-clash review (I7)

Whether any ICBM recipe collides with, or is hidden by, a recipe of the rest of the GTNH pack. Sibling of the earlier GregGuns reviews (the soft-shadow fixes S1 to S8 in [05-armor implementation notes](../05-armor/implementation-notes.md)); the method is `tools/recipe-clash` ([README](../../../tools/recipe-clash/README.md)).

- **Design records:** [0018 ICBM program](../../design/0018-icbm-program.md), [0021 tier balance pass](../../design/0021-tier-balance-pass.md)
- **Sibling sheets:** [recipes.md](recipes.md), [recipe-map-replacements.md](recipe-map-replacements.md), [PACK-CHANGES](../PACK-CHANGES.md)
- **Decisions:** D339 (module default on), D340 (this review)
- **Roadmap:** [I7](../../ROADMAP-ICBM.md) ("The recipe-clash review finds no unresolved clash with ICBM content")

## Method

1. A server with the full GTNH 2.9.0-RC-2 pack and the I7 jar (`modules.icbm` at its default, Galacticraft present: the log said "ICBM module active (Galacticraft present)"), with `debug.recipeDump=true`.
2. `/gregguns dumprecipes` after the server finished loading. The dump holds every vanilla and Forge crafting recipe and every GT recipe map: 248,576 rows, of which 551 are GregGuns rows (ICBM and the rest of the mod) and 248,025 are the pack's. The maps cover crafting, the Assembler, the Assembly Line (its `fakeAssemblylineProcess` map, which is what NEI and GT show), the Circuit Assembler, the Cutting Machine and its fake map, the Laser Engraver, the Scanner, the Chemical Reactor, the Macerator and the Missile Workbench (`gregguns.missileWorkbench`).
3. `python -I tools/recipe-clash/clash.py dump.json report.md` with the default flags, and again with `--ignore-counts` to look for near misses.

### Tool change

The warhead de-arm recipes in the Cutting Machine (D283) are NBT-sensitive: the same inert warhead with a different payload or fill is a different recipe. The dump keyed a stack by item and damage only, so the first run reported 370 false "exact" clashes among them (all `(own)`, all the Cutting Machine). `RecipeDumper.key` now appends `#nbt<hash of the tag>` to a GregGuns stack that carries NBT, so those rows differ. Pack stacks keep the plain key, so a pack row that ignores NBT still compares equal to ours.

## Result

| Category | Default flags | `--ignore-counts` |
|---|---|---|
| exact | 0 | 0 |
| shadow | 0 | 0 |
| grid, shapeless | 0 | 0 |
| duplicate | 0 | 0 |
| subset | 1 | 22 |
| superset | 0 | 0 |

- **ICBM rows by map** (rows whose output is an ICBM part, warhead, missile or machine): Assembler about 100, Cutting Machine 172 (de-arm rows) plus 43 in its fake map, Missile Workbench 6, crafting 6, Ammo Press 7 (the launcher-warhead charge items), Laser Engraver 4, Circuit Assembler 3, Assembly Line 3, Scanner 3. The counts are by output name, so they can be a few rows off.
- **Default flags: nothing of ICBM is in the one subset row.** It is the Bacterial Vat row (S5, Biomass to E. coli culture against BartWorks' Biomass to Fermented Biomass row), which is a special-slot map and was confirmed in game by `gun.bio_gun.vat_recipe_selected`.
- **`--ignore-counts`:** the 22 subset rows are near misses that need a pack row to want fewer items than ours; none fires on ours. The ICBM ones, checked by hand:

| Our row | Near pack row | Why it is not a clash |
|---|---|---|
| Fuze Assembly Mk3 (1 EV plate, circuit 20, LuV circuit, 4 foil) | Railcraft Zeta machine (2 of the same plate, circuit 20) | The pack row needs 2 plates; ours holds 1, so the pack row cannot fire on our inputs, and ours has more inputs than the pack's, so it cannot be stolen |
| Incendiary Launcher Warhead (circuit 8, 2 plates) | GT casing (8 plates, circuit 8) | The pack row needs 8 plates; ours has 2 |

  The rest are armor, ammo and machine rows of the earlier reviews (S1 to S8) or GT casing rows that need 8 plates.
- **Assembly Line:** no Assembly Line row of ours shares its research item or its inputs with a pack row.
- **Crafting grid:** the 6 ICBM crafting recipes (shaped and shapeless) collide with no pack grid recipe.

## Unresolved

None. No ICBM recipe needed to change for the pack. The by-hand rows above are recorded so a later recipe change (balance pass) can be rechecked against them.

## Rerun on the balance pass

Coder P1's recipe changes (D328 to D331) are rechecked with the same tool before the integration branch. The result is added below by the coder who reruns it.

