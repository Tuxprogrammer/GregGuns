# Recipe-clash review: beta gate (0.3.0-beta)

This review closes the beta-gate box "the recipe-clash review found no unresolved clashes". Method: build the jar from master, boot a full GTNH 2.9.0-RC-2 server pack with it, run `/gregguns dumprecipes` (`debug.recipeDump`), then run [tools/recipe-clash/clash.py](../../tools/recipe-clash/README.md) on the dump. Three passes ran: v2, then v3 (8 soft shadows, all fixed), then v4 (clean, below).

# Recipe-clash review v4: GregGuns master b19ae575 vs full GTNH 2.9 RC-2 pack server

Dump: `dump-v4.json.gz` (248,077 rows, jar built on the test box from origin/master b19ae575). Raw tool output: `r4-default.md`, `r4-ic.md` (--ignore-counts), `r4-vat.md` (--ours-fluid). v3 for comparison: `r3-default.md`, `r3-ic.md`.

## Counts
| Run | exact | shadow | subset | superset |
|---|---|---|---|---|
| default (v4) | 0 | 0 | 1 | 0 |
| default + `--ours-fluid fluid:escherichiakolifluid=fluid:biomass` (v4) | 0 | 0 | 1 | 0 |
| `--ignore-counts` + ours-fluid (v4) | 0 | 0 | 20 | 0 |
| default (v3, for diff) | 0 | 0 | 11 | 18 |
| `--ignore-counts` + ours-fluid (v3) | 0 | 0 | 68 | 138 |

grid, shapeless and duplicate are 0 in every run. (The tool already knows the vat's fluid output by default, so the default and `--ours-fluid` runs agree.)

## S1 to S8
| ID | v4 status |
|---|---|
| S1 Blaster Rifle vs `AFSU:ALC` | gone (absent from default and --ignore-counts) |
| S2 HEV Boots vs T3 Combat Boots | gone |
| S3 AR magazine vs SMG magazine | gone in the default run. Under --ignore-counts, the ammo-press rows of different weapons still overlap (see below); that's count-only |
| S4 Laser Focus vs 3-diamond row | gone (engraver row not listed even under --ignore-counts) |
| S5 vat recipe vs BartWorks Biomass to Fermented Biomass | still present, the only default hit: `accepted: verified in game`. The vat map is special-slot sensitive; scenario `gun.bio_gun.vat_recipe_selected` PASSed on the test box (verify-fix-recipe-shadows-03 scenario logs, 08:00:08 and 08:35:42: `PASS gun.bio_gun.vat_recipe_selected`). It selects our recipe with the E. coli dish and not without it |
| S6/S7 turret armor rows | gone (all about 50 rows, subset and superset) |
| S8 `casings:10` vs GT concrete | gone |

## New hits
None in the default run. The superset category dropped from 18 to 0.

Under --ignore-counts, three own-vs-own rows appear that v3's list did not have: the HEV helmet, chestplate and leggings against the T3 Combat helmet, chestplate and leggings (same circuit 1/2/3 and same plates, but different counts, e.g. HEV helmet 2x 17610 against Combat 3x 17610). This is a count-only near-miss: it fires wrongly only if a player puts in the other row's larger stacks in the same machine. Proposed fix (optional, cheap): move HEV helmet, chestplate and leggings to circuits 5, 6 and 7 (boots are on 5 already, so the helmet should go elsewhere, e.g. helmet 6, chestplate 7, leggings 8). That makes the pairing unambiguous. Not gating.

## Remaining --ignore-counts rows (20, all count-only, informational)
- 1 vat row: S5, above.
- 6 ammo-press (own) rows: AR/SMG, LMG/AS50, 9mm/shotgun shell, each in both directions. The rows share inputs but differ in counts. They match only on an over-stocked ammo-press bus. No fix needed.
- 13 Assembler rows. Pack rows: SteelBars (3x steel plates, circuit 3) against Combat Shotgun; the 8x plate rows for Rocket Launcher (circuit 8), Stielgranate (circuit 7) and Power Hammer Adv (circuit 8); and the 9x Machine Hull row against Flamethrower (circuit 9). All need 3 to 9 of one item in the bus, more than our recipe holds. They are theoretical over-supply cases. Own armor and accessory rows overlap in the same way (e.g. Gas Mask against T2 Commando Helmet). No pack change. If anyone wants zero near-misses, move these onto circuits no pack Assembler row uses (a high-numbered, unused circuit).

No PACK-CHANGES entry is needed: nothing remains that the pack must change.

## Pack server
Restored exactly: `mods/gregguns.jar`, `config/gregguns.cfg`, `server.properties` match `bak-v3` and `bak-v4` by sha256 (jar 31f3d007..., cfg d07c5627..., properties 674c91ba...). Dumped on port 25655 and stopped cleanly (done.marker, process gone).
