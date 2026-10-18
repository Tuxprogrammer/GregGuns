# Recipes

All recipes are Forge `ShapedOreRecipe` or `ShapelessOreRecipe` registered in the crafting table. Each output is a stack of 1.
- Strings in quotes are OreDictionary names.
- Other ingredients are Techguns items (the shared-item metadata entries are documented in [06-items](../06-items/index.md)) or vanilla items.
- "any" means the ingredient matches any damage/metadata (wildcard).

Grids are written top row first; `_` is an empty cell. A 2-row recipe can be placed in either the top or the bottom two rows.

Armour recipes are in [`TGArmors.init()`](armor-catalogue.md). Special-equipment recipes are in [`TGItems`](../06-items/item-catalogue.md) ([06-items](../06-items/index.md)), listed here because the items belong to this area.

Crafted multi-camo armour gets `camo = 0`, and crafted powered armour gets `power = 0` (via `onCreated`).

## 1. Armour

### T1 Combat (key: `i` = "ingotIron", `c` = Heavy Cloth)
| Output | Grid |
|---|---|
| Soldier's Helmet | `iii` / `c_c` |
| Soldier's Vest | `c_c` / `iii` / `ccc` |
| Soldier's Pants | `iii` / `c_c` / `c_c` |
| Soldier's Boots | `c_c` / `i_i` |

### T1 Miner (key: `i` = "ingotIron", `c` = Heavy Cloth, `y` = "dyeYellow"; boots use `i` = "nuggetIron")
| Output | Grid |
|---|---|
| Miner's Helmet | `iyi` / `c_c` |
| Miner's Jacket | `c_c` / `ccc` / `ici` |
| Miner's Pants | `ici` / `c_c` / `c_c` |
| Miner's Boots | `c_c` / `i_i` (`i` = "nuggetIron") |

### T1 Scout (key: `c` = Heavy Cloth)
| Output | Grid |
|---|---|
| Bandit Mask | `ccc` / `c_c` |
| Bandit Vest | `c_c` / `ccc` / `ccc` |
| Bandit Pants | `ccc` / `c_c` / `c_c` |
| Bandit Boots | `c_c` / `c_c` |

### T2 Combat (key: `h` = Heavy Cloth, `i` = "ingotObsidianSteel")
| Output | Grid |
|---|---|
| Combat Helmet | `iii` / `h_h` |
| Combat Vest | `h_h` / `iii` / `iii` |
| Combat Pants | `ihi` / `i_i` / `h_h` |
| Combat Boots | `h_h` / `i_i` |

### T2 Commando (key: `h` = "itemRubber", `i` = "ingotObsidianSteel", `g` = "paneGlass")
| Output | Grid |
|---|---|
| Commando Headgear | `ihi` / `hgh` |
| Commando Vest | `h_h` / `hih` / `iii` |
| Commando Pants | `hih` / `i_i` / `h_h` |
| Commando Boots | `i_i` / `h_h` |

### T2 Riot / Ranger Veteran (key: `p` = "plateLead", `l` = Treated Leather; the centre item is the matching T2 Combat piece, any damage and any camo)
| Output | Grid |
|---|---|
| Ranger Veteran Helmet | `php` / `l_l` (`h` = Combat Helmet) |
| Ranger Veteran Jacket | `p_p` / `lcl` / `lpl` (`c` = Combat Vest) |
| Ranger Veteran Pants | `lhl` / `p_p` / `l_l` (`h` = Combat Pants) |
| Ranger Veteran Boots | `lhl` / `p_p` (`h` = Combat Boots) |

### T3 Combat (key: `p` = "plateCarbon", `f` = "fiberCarbon")
| Output | Grid |
|---|---|
| Advanced Combat Helmet | `ppp` / `f_f` |
| Advanced Combat Chestplate | `p_p` / `ppp` / `fpf` |
| Advanced Combat Leggings | `pfp` / `p_p` / `f_f` |
| Advanced Combat Boots | `f_f` / `p_p` |

### Steam Armour (key: `s` = Steam Armor Plating)
| Output | Grid |
|---|---|
| Steam Helmet | `sss` / `s_s` |
| Steam Chestplate | `s_s` / `sss` / `sss` |
| Steam Legplates | `sss` / `s_s` / `s_s` |
| Steam Boots | `s_s` / `s_s` |

### T3 Power Armour (key: `p` = Power Plating, `g` = "paneGlass", `c` = "circuitElite", `e` = Electric Engine)
| Output | Grid |
|---|---|
| Power Armor Helmet | `cpc` / `pgp` |
| Power Armor Chestplate | `p_p` / `ece` / `ppp` |
| Power Armor Leggings | `pcp` / `e_e` / `p_p` |
| Power Armor Boots | `e_e` / `p_p` |

### T3 Hazardous Environment / HEV (key: `p` = "plateCarbon", `l` = "plateTitanium", `c` = "circuitElite", `g` = "paneGlass")
| Output | Grid |
|---|---|
| Hazardous Environment Helmet | `pcp` / `lgl` |
| Hazardous Environment Chestplate | `p_p` / `lcl` / `lpl` |
| Hazardous Environment Leggings | `ppp` / `c_c` / `l_l` |
| Hazardous Environment Boots | `l_l` / `p_p` |

### T3 Exo Suit: shapeless upgrades of T3 Combat
The input piece's damage and camo are discarded; the output is a fresh piece.

| Output | Ingredients |
|---|---|
| Exo Suit Helmet | Advanced Combat Helmet (any) + Mechanical Parts T3 + "circuitElite" + Electric Engine + "ingotTitanium" |
| Exo Suit Chestplate | Advanced Combat Chestplate (any) + Mechanical Parts T3 + "circuitElite" + Electric Engine + "ingotTitanium" ×2 |
| Exo Suit Leggings | Advanced Combat Leggings (any) + Mechanical Parts T3 + "circuitElite" + Electric Engine + "ingotTitanium" ×2 |
| Exo Suit Boots | Advanced Combat Boots (any) + Mechanical Parts T3 + "circuitElite" + Electric Engine + "ingotTitanium" |

### Beret (key: `c` = Heavy Cloth, `l` = vanilla leather)
| Output | Grid |
|---|---|
| Beret | `cc_` / `l_l` |

## 2. Special-slot equipment (registered in `TGItems`)

| Output | Grid / ingredients | Key |
|---|---|---|
| Glider Backback (shared item) | `hhh` / `shs` / `hhh` | `h` Heavy Cloth, `s` "ingotIron" |
| Glider Wing (shared item) | `sss` / `hhh` / `hhh` | same |
| Glider (shared item) | shapeless: Glider Backback + Glider Wing + Glider Wing | |
| Gas Mask Filter (shared item) | `iii` / `ici` / `iii` | `i` "nuggetIron", `c` vanilla coal (passed as an Item, which `ShapedOreRecipe` turns into metadata 0, so coal only and not charcoal) |
| Gas Mask | `grg` / `rrr` / `rfr` | `g` "paneGlass", `r` "itemRubber", `f` Gas Mask Filter |
| Gas Mask (refilter) | shapeless: Gas Mask (any damage) + Gas Mask Filter → Gas Mask at damage 0 | |
| Nightvision Goggles (charged) | `rhr` / `cec` / `gsg` | `r` "itemRubber", `h` Heavy Cloth, `c` "circuitBasic", `e` Energy Cell, `g` "blockGlass", `s` "ingotIron" |
| Nightvision Goggles (empty, damage = 24000) | same grid with `e` = Energy Cell (Empty) | |
| Oxygen Mask (shared item) | `_p_` / `rpr` | `p` "sheetPlastic", `r` "itemRubber" |
| Tactical Mask | `pnp` / `cgc` / `pop` | `p` "sheetPlastic", `n` Nightvision Goggles (any), `c` "circuitBasic", `g` Gas Mask (any), `o` Oxygen Mask |
| Oxygen Tanks | `p_p` / `aia` / `p_p` | `p` "sheetPlastic", `a` Compressed Air Tank, `i` "plateIron" |
| Jump Pack | `n_n` / `pbp` / `c_c` | `n` "nuggetIron", `p` "plateIron", `b` Glider Backback, `c` Compressed Air Tank |
| Jetpack | `f_f` / `pgp` / `rmr` | `f` Fuel Tank, `p` "plateObsidianSteel", `g` Glider, `r` Rocket (ammo), `m` Mechanical Parts T2 |
| Jetpack (alternative) | same grid with `g` = Jump Pack (any) | |
| Anti Gravity Device | `tet` / `cac` / `mbm` | `t` "plateTitanium", `e` Nuclear Power Cell, `c` "circuitElite", `a` Anti Gravity Core, `m` Mechanical Parts T3, `b` Glider Backback |

## 3. Repair

**Anvil:** an armour piece can be repaired with its "repair item" (the metal repair mat, or the cloth mat if there is no metal). Special-slot items have no anvil repair item.

**Repair Bench:** uses each piece's repair-material list (armor-catalogue.md §3 and §5; machine in [09-machines-utility](../09-machines-utility/index.md)). T2 Commando has no repair mats.

**Vanilla two-item crafting repair** works on all of these damageable items, because nothing disables it. This also covers the fuel-based special items (bug [ARM-22](index.md#suspected-bugs)).

## 4. Loot (registered in `TGItems`; chest categories belong to 11-worldgen)

| Chest | Entries (min–max, weight) |
|---|---|
| `techgunsMilBaseBarracks` | T1 Combat helmet, vest, pants, boots (1–1, w 2 each); T2 Combat helmet, vest, pants, boots (1–1, w 1 each); Glider (1, w 1); Gas Mask (1, w 2); Gas Mask Filter (1, w 1); Nightvision Goggles (1, w 1); Oxygen Mask (1, w 1); Oxygen Tanks (1, w 1) |
| vanilla dungeon chest | Nightvision Goggles (1, w 1); Gas Mask (1, w 1) |

Loot armour has no NBT, so its camo name shows "No Camo" (bug [ARM-11](index.md#suspected-bugs)).
