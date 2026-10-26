# 0022: Steam armor moves to the Steam age

- **Status:** Accepted
- **Amends:** [0001](0001-tier-map.md), [0021](0021-tier-balance-pass.md) (change 1: the plating it cut is now retired)
- **Origin:** owner request, 2026-10-25
- **Date:** 2026-10-25

## Context

The Steam armor (Techguns "Steam Armor") was an EV set: 24 Steam Armor Plating (RC-23, titanium + bronze + steel plate) from the Assembler, refueled by a Compressed Air Tank (HV, stainless steel). After 0021 it still cost 24 titanium plates and protected 20 (the MV Ranger Veteran's level). The owner asked for a Steam-age set instead: less protection, cheaper, bronze as the main material and nothing from EV.

## Decision

1. **Tier.** `ArmorCatalog.STEAM` is `Tier.STEAM`. It is hand-crafted like the Scout and Miner sets. The 0001 tier map moves it from the EV row to the Steam / ULV row.
2. **Recipe.** Four shaped recipes (`ArmorTable.STEAM`, rows AR-34 to AR-37, which once named the EV Assembler rows). `B` is `ore:plateBronze`, `c` is Heavy Cloth:

   | Piece | Grid | Bronze plates |
   |---|---|---|
   | Steam Helmet | `BBB` / `c c` | 3 |
   | Steam Chestplate | `B B` / `BcB` / `cBc` | 5 |
   | Steam Legplates | `BcB` / `BcB` / `c c` | 4 |
   | Steam Boots | `c c` / `B B` | 2 |

   The set is 14 plates and 8 cloth, against 24 titanium, 24 bronze and 24 steel plates before. Mechanical Parts T1 (RC-03) were considered and left out: they live in the guns module's `ComponentItems`, and the armor module must build without it. Every grid differs from the Scout and Miner grids (`ArmorTableTest` pins that no two hand grids repeat), and none uses a machine.
3. **Steam Armor Plating is removed.** The item (`part_steam_armor_plating`), `ArmorCatalog.STEAM_ARMOR_PLATING`, its `ArmorParts` registration, `PartCatalog` entry, `ArmorTable.STEAM_PLATING`, the lang line and the RC-23 recipe are gone. **RC-23 is retired and never reused.** The texture file `part_steam_armor_plating.png` is left in place (the owner said to leave art alone); it is an orphan. Plating held in an existing world disappears (FML drops the unknown item).
4. **Repair** is `plate.Bronze` plus Heavy Cloth, the same pattern as the other sets (`repairFor(slot, "plate", "Bronze", HEAVY_CLOTH)`). Recycling reads the hand row: each piece returns its bronze plates.
5. **Protection and perks** sit between the Scout and Miner (13) and the T1 Combat set (15):

   | Number | Old (EV) | New (Steam) |
   |---|---|---|
   | Physical and projectile | 20 | **14** |
   | Explosion, energy, ice, lightning | 18 / 16 / 16 / 16 | **10.5** each (75% of 14) |
   | Fire | 18 | **12** |
   | Poison, radiation | 10, 6 | **0, 0** |
   | Penetration resistance | 0.10 | **0.05** |
   | Piece durability base | 200 (2,750) | **66 (908)** |
   | Vanilla armor bar | 5+6+5+4 = 20 | **4+4+4+3 = 15** |
   | Speed per piece (powered) | 0.05 (set 0.20) | **0.04 (set 0.16)** |
   | Jump (powered) | 0.03 x3, boots 0.15 | **0.02 x3, boots 0.10** |
   | Break speed per piece (powered) | 0.05 (set 0.20) | **0.03 (set 0.12)** |
   | Extra health (powered) | 1 / 2 / 1 / 1 = 5 | **0.5 / 1 / 0.5 / 0.5 = 2.5** |
   | Knockback resistance (powered) | 0.15 / 0.30 / 0.20 / 0.15 = 0.80 | **0.05 / 0.15 / 0.10 / 0.05 = 0.35** (under the T1 Combat set's passive 0.40) |
   | Heat insulation | partial HEAT | unchanged (partial HEAT, not powered) |
   | Step assist, fall reduction, free fall | boots | unchanged |

   Capacity stays 3,600 units. The physical value 14 keeps `TierBalanceTest` true: the best set per tier never falls going up (Steam 14, LV 15, MV 19, EV 22, IV 24).
6. **Power.** The chestplate stays a unit store in the stack tag. It burns Steam-age fuel from the wearer's main inventory:
   - **Coal and Charcoal** (vanilla `Items.coal` damage 0 and 1, or any item in the ore dictionary as `gemCoal` or `gemCharcoal`; GT registers both under those names in `LoaderGTOreDictionary`, 5.09.54.205) add `PowerRules.FUEL_REFUEL_UNITS` = 900 units each, a quarter of a full charge, about 3.75 minutes of walking in a full set. One is burned only when the chestplate has room for all 900 (no waste); the inventory is scanned once a second, and at once when the chestplate is empty.
   - **Compressed Air Tank** still refills an empty chestplate fully and hands back an empty GT cell (existing behavior, unchanged). Coal is tried first.
   - The tooltip line reads "Steam charge: n / 3600" (was "Compressed air").
   The 900 is a constant in `PowerRules`, not config, like the other power numbers (0003's config request is still open).
7. **Questbook.** The quest keeps its key `gg.ev.steam_armor` (quest ids are `uuid5(key)` and must not change) and moves to the Steam chapter: tier Steam, prerequisite `gg.steam.intro`, a `crafting` task, position 132, 132. The pack-side lang block is updated with it.
8. **Out of scope.** Models, textures and sounds are untouched (the set keeps its look).

## Consequences

- Rows AR-34 to AR-37 changed from Assembler rows to hand rows; `ArmorTable.MACHINE` lost five rows (the plating and the four pieces). `RecipeClashGuardTest` no longer lists them.
- The EV tier of 0001 no longer has an armor set of the Steam kind; its armor is T3 Combat and the HEV suit.
- Infographic sheets that show Steam armor at EV need a rebuild: the armory sheet (`armor-and-gear`, `ARMOR_SETS` in `pages_more.py` is updated) and the questbook chapters sheet (`CHAPTERS` blurbs updated); the recipes sheet reads `recipes.json` and only needs the usual export refresh.
- Headless scenarios `armor.steam.power` (new numbers, plus a coal refuel) and `armor.recipes` (now 12 hand rows) were updated; `armor.steam.recipes` is removed. They were not run (no Minecraft on the laptop).
