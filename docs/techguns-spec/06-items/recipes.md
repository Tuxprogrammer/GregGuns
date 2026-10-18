# Recipes registered in `TGItems.registerRecipes()` (and related)

`TGItems.registerRecipes()` runs in `Techguns.postInit` after `TGFluids.postInit`, `TGBlocks.postInit` and `TGItems.postInit`. The two fallback OreDict names are therefore already resolved when it runs:

| Variable | Value |
|---|---|
| `hardenedGlassOrGlass` | `"blockGlassHardened"` if that OreDict entry has at least one item, otherwise `"blockGlass"` |
| `electrumOrGold` | `"ingotElectrum"` if present, otherwise `"ingotGold"` |

Conventions:
- Grids are written row by row as `row1 / row2 / row3`. `·` is an empty cell. Shaped recipes have the normal vanilla behaviour: they can be placed anywhere in the 3×3 grid if smaller, and mirrored placement is accepted.
- Quoted names (`"ingotIron"`) are OreDict names. Anything else is an exact item. *meta N* means a shared `TechgunsAmmo` item with that metadata (see [item-catalogue.md](item-catalogue.md)).
- "Shaped (ore)" / "Shapeless (ore)" means Forge `ShapedOreRecipe` / `ShapelessOreRecipe`. Here a vanilla `Block` ingredient matches any metadata, and an `Item` ingredient matches meta 0. "Shapeless (vanilla)" means `GameRegistry.addShapelessRecipe`, which matches exact stacks.
- Recipes that make **blocks** are listed because they are registered here. The blocks themselves are documented in 07-blocks / 08 / 09.

## 1. Gun parts and components

| # | Output | Type | Grid | Key |
|---|---|---|---|---|
| R01 | Stone Barrel ×1 | Shaped (ore) | `sss / ··· / sss` | s = `"stone"` |
| R02 | Wooden Stock ×1 | Shaped (ore) | `ww / ·w` | w = `"logWood"` |
| R03 | Iron Barrel ×1 | Shaped (ore) | `iii / ··· / iii` | i = `"ingotIron"` |
| R04 | Mechanical Parts ×1 | Shaped (ore) | `·i· / ifi / ·i·` | f = flint, i = `"ingotIron"` |
| R05 | Plastic Gun Stock ×1 | Shaped (ore) | `ppp / ··p` | p = `"sheetPlastic"` |
| R06 | Hardened Steel Barrel ×1 | Shaped (ore) | `ooo / ··· / ooo` | o = Obsidian Steel Ingot (meta 40, exact) |
| R07 | Pump Mechanism ×1 | Shaped (ore) | `nnn / gpg / nnn` | n = `"nuggetIron"`, g = `"blockGlass"`, p = piston |
| R08 | Laser Barrel ×1 | Shaped (ore) | `fff / ggl / fff` | f = electrumOrGold, g = hardenedGlassOrGlass, l = Laser Focus (meta 36) |
| R09 | Heavy Cloth ×3 | Shaped (ore) | `·w· / wlw / ·w·` | w = wool (any colour), l = leather |
| R10 | Iron Receiver ×1 | Shaped (ore) | `iii / ·mn / ··n` | i = `"ingotIron"`, n = `"nuggetIron"`, m = Mechanical Parts |
| R11 | Steel Receiver ×1 | Shaped (ore) | `iii / ·mn / ··n` | i = `"ingotSteel"`, n = `"nuggetSteel"`, m = Mechanical Parts (Obsidian Steel) |
| R12 | Obsidian Steel Receiver ×1 | Shaped (ore) | `ppp / ·mi / ··i` | p = `"plateObsidianSteel"`, i = `"ingotObsidianSteel"`, m = Mechanical Parts (Carbon) |
| R13 | Carbon Receiver ×1 | Shaped (ore) | `ppp / ·mi / ··i` | p = `"plateCarbon"`, i = `"ingotObsidianSteel"`, m = Mechanical Parts (Carbon) |
| R14 | Copper Wire ×1 | Shaped (ore) | `·cc / ·c· / cc·` | c = `"nuggetCopper"` |
| R15 | Glider Backback ×1 | Shaped (ore) | `hhh / shs / hhh` | h = Heavy Cloth, s = `"ingotIron"` |
| R16 | Glider Wing ×1 | Shaped (ore) | `sss / hhh / hhh` | h = Heavy Cloth, s = `"ingotIron"` |
| R17 | Glider ×1 | Shapeless (ore) | Glider Backback + Glider Wing + Glider Wing | |
| R18 | Electric Engine ×1 | Shaped (ore) | `wrw / imi / wrw` | w = Copper Wire (meta 57), i = `"ingotIron"`, r = redstone, m = Mechanical Parts |
| R19 | Circuit Board ×4 | Shaped (ore) | `cgc / rir / cgc` | c = `"wireCopper"`, g = `"dyeGreen"`, r = redstone, i = `"plateIron"` |
| R20 | Elite Circuit Board ×1 | Shapeless (ore) | Circuit Board + Gold Wire + `"gemLapis"` | |
| R21 | Coil ×1 | Shaped (ore) | `·wi / wiw / iw·` | w = `"wireCopper"`, i = `"ingotIron"` |
| R22 | Machine Upgrade (Stack Operation) ×1 | Shaped (ore) | `pgp / ncn / pnp` | p = `"plateIron"`, n = `"ingotGold"`, g = `"dyeGreen"`, c = chest |
| R23 | Carbon Barrel ×1 | Shaped (ore) | `iii / ··· / iii` | i = `"plateCarbon"` |
| R24 | Carbon Gunstock ×1 | Shaped (ore) | `iii / ·pp` | i = `"plateCarbon"`, p = `"sheetPlastic"` |
| R25 | Gas Mask Filter ×1 | Shaped (ore) | `iii / ici / iii` | i = `"nuggetIron"`, c = coal (meta 0, not charcoal) |
| R26 | Bio Mass ×4 | Shaped (ore) | `·g· / gbg / ·g·` | g = `"dyeGreen"`, b = `"slimeball"` |
| R27 | Quartz Rod ×1 | Shaped (ore) | `q·· / ·q· / ··q` | q = `"gemQuartz"` |
| R28 | Quartz Rod ×1 | Shaped (ore) | `··q / ·q· / q··` | q = `"gemQuartz"` |
| R29 | Small Nuclear Warhead ×2 | Shaped (ore) | `pcp / tut / pcp` | p = `"plateLead"`, t = TGX, c = `"circuitBasic"`, u = `"ingotUraniumEnriched"` |
| R30 | Reaction Chamber Heat Ray Focus ×1 | Shaped (ore) | `cwc / plp / plp` | c = `"circuitElite"`, w = `"wireGold"`, p = `"plateSteel"`, l = redstone lamp |
| R31 | Oxygen Mask ×1 | Shaped (ore) | `·p· / rpr` | p = `"sheetPlastic"`, r = `"itemRubber"` |

## 2. Ammunition, magazines and tanks

| # | Output | Type | Grid | Key |
|---|---|---|---|---|
| R32 | Stone Bullet ×16 | Shapeless (ore) | `"cobblestone"` ×3 + gunpowder | |
| R33 | 9mm Bullets ×8 | Shaped (ore) | `clc / cgc / ccc` | c = `"nuggetCopper"`, l = `"ingotLead"`, g = gunpowder |
| R34 | Shotgun Shell ×5 | Shaped (ore) | `lll / cgc / ccc` | l = `"nuggetLead"`, c = `"nuggetCopper"`, g = gunpowder |
| R35 | 9mm Pistol Magazine (Empty) ×4 | Shaped (ore) | `i·i / imi / igi` | i = `"nuggetIron"`, g = `"ingotIron"`, m = Mechanical Parts |
| R36 | 9mm Pistol Magazine ×1 | Shapeless (vanilla) | Empty Pistol Mag + 9mm Bullets ×3 | |
| R37 | SMG Magazine (Empty) ×4 | Shaped (ore) | `i·i / i·i / imi` | i = `"nuggetIron"`, m = Mechanical Parts |
| R38 | SMG Magazine ×1 | Shapeless (vanilla) | Empty SMG Mag + 9mm Bullets ×2 | |
| R39 | Assault Rifle Magazine (Empty) ×4 | Shaped (ore) | `s·s / s·s / sms` | s = `"nuggetSteel"`, m = Mechanical Parts |
| R40 | Assault Rifle Magazine ×1 | Shapeless (vanilla) | Empty AR Mag + Rifle Bullets ×3 | |
| R41 | Bio Tank (Empty) ×1 | Shaped (ore) | `sss / sgs / sgs` | s = `"nuggetSteel"`, g = `"blockGlass"` |
| R42 | Rocket ×4 | Shaped (ore) | `·s· / sts / sgs` | s = `"nuggetIron"`, t = TNT, g = gunpowder |
| R43 | LMG Magazine (Empty) ×4 | Shaped (ore) | `·ii / imi / iii` | i = `"ingotSteel"`, m = Mechanical Parts |
| R44 | LMG Magazine ×1 | Shaped (ore) | `bbb / bmb / bbb` | b = Rifle Bullets, m = Empty LMG Mag |
| R45 | LMG Magazine ×1 | Shapeless (ore) | Empty LMG Mag + Rifle Bullets Stack ×2 | |
| R46 | AS50 Magazine (Empty) ×4 | Shaped (ore) | `s·s / s·s / sms` | s = `"ingotSteel"`, m = Mechanical Parts |
| R47 | AS50 Magazine ×1 | Shapeless (ore) | Empty AS50 Mag + High Caliber Rounds ×2 | |
| R48 | Advanced Magazine (Empty) ×4 | Shaped (ore) | `i·i / i·i / imi` | i = `"ingotObsidianSteel"`, m = Mechanical Parts (Obsidian Steel) |
| R49 | Advanced Magazine ×1 | Shapeless (ore) | Empty Advanced Mag + Advanced Bullets ×3 | |
| R50 | Fuel Tank (Empty) ×4 | Shaped (ore) | `p / g / p` (one column) | p = `"sheetPlastic"`, g = `"blockGlass"` |
| R51 | Rifle Bullets Stack ×1 | Shapeless (ore) | Rifle Bullets ×4 | |
| R52 | Rifle Bullets ×4 | Shapeless (ore) | Rifle Bullets Stack ×1 | |
| R53 | Minigun Ammo Drum (empty) ×4 | Shaped (ore) | `sss / pmp / sss` | s = `"ingotSteel"`, p = `"sheetPlastic"`, m = Mechanical Parts (Obsidian Steel) |
| R54 | Minigun Ammo Drum ×1 | Shapeless (ore) | Empty Drum + Rifle Bullets Stack ×4 | |
| R55 | Compressed Air Tank (Empty) ×7 | Shaped (ore) | `plp / p·p / ppp` | p = `"plateIron"`, l = lever |
| R56 | Compressed Air Tank (Empty) ×7 | Shaped (ore) | `plp / p·p / ppp` | p = `"plateTin"`, l = lever |
| R57 | Rocket (Tactical Nuke) ×1 | Shapeless (vanilla) | Rocket + Small Nuclear Warhead | |

## 3. Turret armour and drill heads

| # | Output | Type | Grid | Key |
|---|---|---|---|---|
| R58 | Iron Turret Armor | Shaped (ore) | `ppp / ·p· / ·p·` | p = `"plateIron"` |
| R59 | Steel Turret Armor | Shaped (ore) | same | p = `"plateSteel"` |
| R60 | Obsidian Steel Turret Armor | Shaped (ore) | same | p = `"plateObsidianSteel"` |
| R61 | Carbon Turret Armor | Shaped (ore) | same | p = `"plateCarbon"` |
| R62 | Small Ore Drill Head (Steel) | Shaped (ore) | `sps / sps / ·s·` | s = `"ingotSteel"`, p = `"plateSteel"` |
| R63 | Small Ore Drill Head (Obsidian Steel) | Shaped (ore) | `sps / sps / ·s·` | s = `"ingotObsidianSteel"`, p = `"plateObsidianSteel"` |
| R64 | Small Ore Drill Head (Carbon) | Shaped (ore) | `sss / sss / ·s·` | s = `"plateCarbon"` |
| R65–R67 | Medium Ore Drill Head (Steel / Obsidian Steel / Carbon) | Shaped (ore) | `d·d / ·d·` | d = Small head of the same material |
| R68–R70 | Large Ore Drill Head (Steel / Obsidian Steel / Carbon) | Shaped (ore) | `d·d / ·d·` | d = Medium head of the same material |

## 4. Tools, wearables and stand-alone items

| # | Output | Type | Grid | Key |
|---|---|---|---|---|
| R71 | Bunker Door (`doorItem01`) ×2 | Shaped (ore) | `pp / pp / pp` | p = `"plateIron"` |
| R72 | Gas Mask ×1 | Shaped (ore) | `grg / rrr / rfr` | r = `"itemRubber"`, f = Gas Mask Filter, g = `"paneGlass"` |
| R73 | Gas Mask (damage 0) ×1 | Shapeless (ore) | Gas Mask (any damage) + Gas Mask Filter | refills the mask |
| R74 | Nightvision Goggles (charged, damage 0) | Shaped (ore) | `rhr / cec / gsg` | r = `"itemRubber"`, h = Heavy Cloth, c = `"circuitBasic"`, e = Energy Cell, g = `"blockGlass"`, s = `"ingotIron"` |
| R75 | Nightvision Goggles (damage = max, i.e. empty) | Shaped (ore) | same | e = Energy Cell (Empty) |
| R76 | Jetpack | Shaped (ore) | `f·f / pgp / rmr` | f = Fuel Tank, p = `"plateObsidianSteel"`, g = Glider (meta 51), r = Rocket, m = Mechanical Parts (Obsidian Steel) |
| R77 | Jetpack | Shaped (ore) | same | g = Jump Pack (any damage) |
| R78 | Anti Gravity Device | Shaped (ore) | `tet / cac / mbm` | t = `"plateTitanium"`, e = Nuclear Power Cell, c = `"circuitElite"`, a = Anti Gravity Core, m = Mechanical Parts (Carbon), b = Glider Backback |
| R79 | Oxygen Tanks (`scubaTanks`) | Shaped (ore) | `p·p / aia / p·p` | p = `"sheetPlastic"`, a = Compressed Air Tank (full), i = `"plateIron"` |
| R80 | Jump Pack | Shaped (ore) | `n·n / pbp / c·c` | n = `"nuggetIron"`, p = `"plateIron"`, b = Glider Backback, c = Compressed Air Tank (full) |
| R81 | Tactical Mask | Shaped (ore) | `pnp / cgc / pop` | p = `"sheetPlastic"`, n = Nightvision Goggles (any damage), c = `"circuitBasic"`, g = Gas Mask (any damage), o = Oxygen Mask |
| R82 | Combat Knife | Shaped (ore) | `·s / p·` | s = `"plateSteel"`, p = `"sheetPlastic"` |
| R83 | Ore Cluster Scanner | Shaped (ore) | `g·g / pcp / pep` | g = `"wireGold"`, p = `"plateSteel"`, c = `"circuitElite"`, e = Energy Cell (full) |
| R84 | Camo Net Placer meta 0 (Woodland) | Shaped (ore) | `sns` (one row) | s = `"stickWood"`, n = Camo Net block meta 0 |
| R85 | Camo Net Placer meta 1 (Snow) | Shaped (ore) | `sns` | n = Camo Net meta 1 |
| R86 | Camo Net Placer meta 2 (Desert) | Shaped (ore) | `sns` | n = Camo Net meta 2 |
| R87 | Crowbar | Shaped (ore) | `··p / ·p· / p··` | p = `"plateSteel"` |

The tool and Ore Cluster Scanner outputs carry no NBT. The scanner's `onCreated` fills it to 30 000 RF when the player takes it from the crafting output. Tools get `camo = 0` on creation (see special-items).

## 5. Block recipes registered in `TGItems` (blocks belong to 07/08/09)

| # | Output | Type | Grid | Key |
|---|---|---|---|---|
| B01 | Ammo Press (`basicMachine:0`) | Shaped (ore) | `ili / cec / iri` | i = `"ingotIron"`, l = `"ingotLead"`, c = `"ingotCopper"`, e = Electric Engine, r = redstone |
| B02 | Metal Press (`basicMachine:1`) | Shaped (ore) | `ibi / iei / iri` | i = `"ingotIron"`, b = `"blockIron"`, e = Electric Engine, r = redstone |
| B03 | Metal Press (`basicMachine:1`) | Shaped (ore) | `ibi / iei / iri` | b = `"plateIron"` (alternative) |
| B04 | Turret (`basicMachine:2`) | Shaped (ore) | `pip / cec / prp` | p = `"plateIron"`, i = `"circuitBasic"`, c = Concrete (`concrete:0`), e = Electric Engine, r = redstone (key `b` is defined but unused) |
| B05 | Camo Bench (`basicMachine:3`) | Shaped (ore) | `ddd / ici / iii` | d = `"dye"` (any dye), c = crafting table, i = `"nuggetIron"` |
| B06 | Chemical Laboratory (`basicMachine:4`) | Shaped (ore) | `igi / geg / iri` | i = `"ingotIron"`, g = glass bottle, e = Electric Engine, r = redstone |
| B07 | Repair Bench (`basicMachine:5`) | Shaped (ore) | `pmp / ici / iii` | p = `"plateIron"`, m = Mechanical Parts, c = crafting table, i = `"nuggetIron"` |
| B08 | Explosive Charge (`basicMachine:6`) ×8 | Shaped (ore) | `rtr / tct / rtr` | r = `"itemRubber"`, t = TNT, c = `"circuitBasic"` |
| B09 | Advanced Explosive Charge (`basicMachine:8`) ×4 | Shaped (ore) | `rtr / tct / rtr` | r = `"sheetPlastic"`, t = TGX, c = `"circuitBasic"` |
| B10 | Charging Station (`basicMachine:9`) | Shaped (ore) | `sgs / cbc / sgs` | s = `"plateSteel"`, g = `"wireGold"`, b = `"circuitBasic"`, c = Coil |
| B11 | Metal Ladder (`ladder01`) ×16 | Shaped (ore) | `iii / ·i· / iii` | i = `"plateTin"` |
| B12 | Lamp (`lamp01:0`) ×16 | Shaped (ore) | `iii / grg / ggg` | i = `"nuggetIron"`, r = `"dustRedstone"`, g = `"paneGlass"` |
| B13 | `lamp01:0` ↔ `lamp01:8` | Shapeless (ore) | 1 → 1 each way (two recipes) | |
| B14 | Lamp (`lamp01:7`) ×16 | Shaped (ore) | `iii / grg / iii` | as B12 |
| B15 | `lamp01:15` ↔ `lamp01:7` | Shapeless (ore) | 1 → 1 each way (two recipes) | |
| B16 | Ore Drill Frame (`oreDrill:0`) ×8 | Shaped (ore) | `pbp / bpb / pbp` | p = `"plateIron"`, b = iron bars |
| B17 | Ore Drill Scaffold (`oreDrill:1`) ×8 | Shaped (ore) | `bbb / bpb / bbb` | p = `"plateIron"`, b = iron bars |
| B18 | Ore Drill Rod (`oreDrill:2`) ×4 | Shaped (ore) | `ipi / ipi / ipi` | p = `"plateIron"`, i = `"ingotIron"` |
| B19 | Ore Drill Engine (`oreDrill:3`) ×4 | Shaped (ore) | `bbb / beb / bbb` | b = iron bars, e = Electric Engine (key `p` unused) |
| B20 | Ore Drill Controller (`oreDrill:4`) ×1 | Shaped (ore) | `pcp / cec / pcp` | p = `"plateIron"`, c = `"circuitBasic"`, e = Electric Engine |
| B21 | Shipping Container (Red) (`container:0`) ×8 | Shaped (ore) | `i·i / ·c· / i·i` | i = `"ingotIron"`, c = chest |
| B22 | Sandbags ×8 | Shapeless (ore) | `"itemRubber"` + sand ×8 | |
| B23 | Camo Net (`camoNetPane:0`) ×8 | Shaped (ore) | `sds / sds` | s = stick, d = dirt |
| B24 | Camo Net Roof (`camoNetRoof:0`) ×16 | Shaped (ore) | `wdw / dsd / wdw` | w = stick, d = dirt, s = string |
| B25 | Reaction Chamber Housing (`machineMultiBlock:0`) ×9 | Shaped (ore) | `sms / pcp / ses` | s = `"plateSteel"`, m = Mechanical Parts (Carbon), p = Cybernetic Parts, e = `"circuitElite"`, c = Chemical Laboratory |
| B26 | Reaction Chamber Controller (`machineMultiBlock:1`) | Shaped (ore) | `·g· / crc / ·g·` | g = hardenedGlassOrGlass, c = `"circuitElite"`, r = RC Housing |
| B27 | Reaction Chamber Glass (`machineMultiBlock:2`) ×6 | Shaped (ore) | `rgr / rgr / rgr` | r = RC Housing, g = hardenedGlassOrGlass |
| B28 | Fabricator Housing (`machineMultiBlock:3`) ×4 | Shaped (ore) | `sms / pep / scs` | s = `"plateSteel"`, m = Mechanical Parts (Carbon), p = Cybernetic Parts, e = Electric Engine, c = `"circuitElite"` |
| B29 | Fabricator Controller (`machineMultiBlock:4`) | Shaped (ore) | `·g· / cfc / ·g·` | g = hardenedGlassOrGlass, c = `"circuitElite"`, f = Fabricator Housing |
| B30 | Fabricator Glass (`machineMultiBlock:5`) ×4 | Shaped (ore) | `fgf / g·g / fgf` | f = Fabricator Housing, g = hardenedGlassOrGlass |
| B31 | Metal Panel (`metalpanel:0`) ×32 | Shaped (ore) | `sss / sps / sss` | s = `"stone"`, p = `"plateCopper"` |
| B32 | Metal Panel (Connected) (`metalpanel2:0`) ×64 | Shaped (ore) | `sss / psp / sss` | s = `"stone"`, p = `"plateCopper"` |
| B33 | Reinforced Chest (`tgchest`) | Shaped (ore) | `n·n / ·c· / n·n` | n = `"nuggetIron"`, c = chest |
| B34 | Weapon Chest (`tgchest_weapon`) | Shaped (ore) | `pcp` (one row) | p = `"plateIron"`, c = chest |

Gun recipes are in `TGuns` (02-guns) and armour recipes in `TGArmors` (05-armor). They are not repeated here.

### Commented-out recipes (not active)
These recipes exist only as comments: spring, iron gears, Mechanical Parts T2/T3 crafting, pistol-mag fill via shaped grid, Rifle Bullets and High Caliber Rounds crafting, Fuel Tank crafting and fill, Bio Tank fill, Energy Cell crafting, Laser Focus crafting, Obsidian Steel crafting, Power Plating crafting, Medkit crafting, Nuclear Power Cell crafting, and Bunker Door from tin plates.

## 6. Smelting relevant to this area (registered in `TGMachineRecipes`)

| Input | Output | XP |
|---|---|---|
| Raw Rubber (70) | Rubber Bar (73) | 0 |
| Raw Plastic (71) | Plastic Sheet (72) | 0 |
| Titanium Ore (94) | Titanium Ingot (95) | 0 |
| Obsidian Steel Plate (65) | Obsidian Steel Ingot (40) | 0 |
| Titanium Plate (96) | Titanium Ingot (95) | 0 |

## 7. Config-dependent metal recipes (`Techguns.java`)

| Condition (config key, default false) | Recipe |
|---|---|
| `addBronzeCrafting` | Shapeless (ore): `"ingotCopper"` ×3 + `"ingotTin"` → 4 Bronze Ingot |
| `addSteelCrafting` | Smelting: iron ingot → Techguns Steel Ingot, 0.35 XP (the output item is only registered if `addSteelIngot`, see bug ITEM-02) |
| `addIronNuggets` | Shapeless (vanilla): iron ingot → 9 Iron Nugget; shaped (vanilla) `nnn/nnn/nnn` nuggets → iron ingot |
| `addCopperTinBronzeNuggets` | Shapeless (ore) `"ingotCopper"` → 9 Copper Nugget; shaped (ore) 9 `"nuggetCopper"` → Techguns Copper Ingot |
| `addLeadNuggets` | Shapeless (ore) `"ingotLead"` → 9 Lead Nugget; shaped (ore) 9 `"nuggetLead"` → Techguns Lead Ingot |
| `addSteelNuggets` and `addSteelIngot` | Shapeless (ore) Techguns Steel Ingot → 9 Steel Nugget; shaped 9 Steel Nugget → Techguns Steel Ingot |
| `addSteelNuggets` and not `addSteelIngot` (postInit) | if any `"ingotSteel"` exists: Shapeless (ore) `"ingotSteel"` → 9 Steel Nugget; shaped 9 `"nuggetSteel"` → first OreDict steel ingot |
| `doOreGenCopperTin` | Smelting: Techguns copper/tin ore → Copper/Tin Ingot, 0.7 XP each |
| `doOreGenLead` | Smelting: Techguns lead ore → Lead Ingot, 0.7 XP |

The ore blocks belong to 07/11.
