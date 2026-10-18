# Item recipes

Every GregGuns recipe for the items in area 06, plus the **component index** that areas 02 (guns), 05 (armor) and 07 to 09 (blocks and machines) use to name the parts they consume.

- **Techguns baseline:** [recipes.md](../../techguns-spec/06-items/recipes.md) (R01 to R87, B01 to B34), [ammo.md](../../techguns-spec/06-items/ammo.md), [components-and-materials.md](../../techguns-spec/06-items/components-and-materials.md), [item-catalog.md](../../techguns-spec/06-items/item-catalog.md); the replaced machines: [ammo-press.md](../../techguns-spec/08-machines-production/ammo-press.md), [metal-press.md](../../techguns-spec/08-machines-production/metal-press.md), [chem-lab.md](../../techguns-spec/08-machines-production/chem-lab.md), [fabricator.md](../../techguns-spec/08-machines-production/fabricator.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md), [0003 energy model](../../design/0003-energy-model.md), [0008 exact-tier charging](../../design/0008-exact-tier-charging.md), [0009 integration defaults](../../design/0009-gtnh-integration-defaults.md)
- **Pack mods:** GregTech (`gregtech`, GT5-Unofficial 5.09.54.205), IC2 (hard dependency, API only)
- **Roadmap:** M2 (ballistic parts and ammo), M4 (production chain), M5 (EV and above)

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| About 70 shaped crafting grids, plus Ammo Press, Metal Press, Chem Lab and Fabricator plans | Crafting table only for Steam/ULV items and for magazine refills. Everything else is a GT machine recipe at the item's tier voltage | [0001](../../design/0001-tier-map.md), [0002](../../design/0002-machine-deconfliction.md) |
| Ammo Press: one plan per bullet, 4 to 16 per cycle | One **GregGuns Ammo Press** (`AP`) recipe per ammo type, selected by a programmed circuit, 8 to 16 rounds per run. The Ammo Press is reinstated by [0011](../../design/0011-machine-policy.md) as the one exception to GT maps; it was previously to be replaced by GT maps ([recipe-map-replacements](../08-machines-production/recipe-map-replacements.md#ammo-press)) |
| Bullets from copper, lead and iron nuggets by OreDict lookup ([ITEM-02](../../techguns-spec/06-items/index.md#suspected-bugs)) | Bullets are built from GT `round`, `foil` and `itemCasing` parts and vanilla gunpowder (GT `dust.Gunpowder`) | GT first. No casing item of our own |
| Iron / steel / obsidian steel / carbon material tiers | Steel / aluminum / stainless steel / titanium hardware, with GT's fiber-reinforced epoxy standing in for carbon | [Material remap](#material-remap), fixed by [0010](../../design/0010-material-remap-and-tier-additions.md) |
| Energy Cell and Nuclear Power Cell are consumables filled in the Charging Station | No Energy Cell: energy weapons are their own IC2 batteries ([0003](../../design/0003-energy-model.md), [0008](../../design/0008-exact-tier-charging.md), [0010](../../design/0010-material-remap-and-tier-additions.md)). Nuclear Power Cell is a consumable refueled in a ZPM machine | [0010](../../design/0010-material-remap-and-tier-additions.md) |
| Tanks filled in the Chem Lab | Tanks are crafted in the Assembler and filled in the GT Fluid Canner | [fluids.md](fluids.md#gregtech-fluid-containers) |
| Rifle Bullets Stack (compression item) | Dropped. Magazines are assembled from loose bullets | Assembler inputs already take stacks |
| Medkit and Bandages unregistered | Built. Bandages at LV, Medkit at MV | [0001](../../design/0001-tier-map.md): "Build: Medkit (MV)" |

## How to read the tables

- **Part notation** follows [CONVENTIONS](../CONVENTIONS.md#recipe-sheets): `plate.Steel` is `OrePrefixes.plate` with `Materials.Steel`, fetched through `GTOreDictUnificator.get`. `circuit.EV` is `OrePrefixes.circuit` with `Materials.EV`. `ItemList.Emitter_EV` is a GT component. A bare `part_*`, `ammo_*`, `mag_*`, `tank_*` or `cell_*` name is a GregGuns item from the [component index](#component-index). Item counts follow `×`.
- **Tiers and EU/t.** `TierEU.RECIPE_*` constants ([TierEU.java#L25-L39](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TierEU.java#L25-L39)): LV 30, MV 120, HV 480, EV 1,920, IV 7,680, LuV 30,720, ZPM 122,880. Durations are in seconds (20 ticks), written with `SECONDS`.
- **Maps.** `AP` is `gregguns.ammoPress`, the GregGuns Ammo Press map ([ammo-press.md](../08-machines-production/ammo-press.md), [0011](../../design/0011-machine-policy.md)), run by the Ammo Press single blocks and the Ammunition Works multiblock. All loose ammo and magazine recipes use it; circuits are kept as selectors. `Asm` is `RecipeMaps.assemblerRecipes` ([L1266](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L1266): up to 9 item inputs, 1 fluid input, 1 output). `c=N` is `.circuit(N)`, the programmed circuit. `Bath` is `chemicalBathRecipes` ([L611](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/RecipeMaps.java#L611)). `AL` is the Assembly Line ([`GTRecipeConstants.AssemblyLine` L562](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeConstants.java#L559-L567)).
- **Crafting grids** use GT's tool letters ([`GTModHandler.addCraftingRecipe`, L619-L624](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L619-L624)): `h` hammer, `f` file, `s` saw. A tool in the grid loses one durability.
- **Gate** is the input material that the tier's machine voltage pairs with ([0001](../../design/0001-tier-map.md)). The voltage is enforced by `eut`: a `RECIPE_MV` recipe cannot start in an LV machine ([ParallelHelper#L399-L401](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/ParallelHelper.java#L399-L401)).
- **Programmed circuits.** Loose ammo uses `c=1` to `c=7`, empty magazines `c=8` to `c=14`, filled-magazine packs `c=15` to `c=21`. Parts and other items use `c=1` unless a row says otherwise; their inputs differ, so no two recipes share (map, inputs, circuit). Circuit numbers stay within GT's 1 to 24 range.

### Material remap

Techguns' four material tiers have no GT counterpart for obsidian steel and carbon. [0010](../../design/0010-material-remap-and-tier-additions.md) fixes the rule: the hardware is the [0001](../../design/0001-tier-map.md) gate material of the tier the consuming item sits in, and carbon is `plate.EpoxidFiberReinforced` plus that tier material. The table shows how each Techguns tier lands.

| Techguns tier | GregGuns tier | Hardware (the gate) | Notes |
|---|---|---|---|
| Stone, wood | Steam / ULV | `Bronze` | Hand-made |
| Iron | LV | `Steel` screws and rings on `Iron` parts | The part name stays "Iron" |
| Steel | MV | `Aluminium` screws on `Steel` parts | |
| Obsidian steel | HV | `StainlessSteel` | Renamed "Hardened" |
| Carbon | EV | `Titanium`, with `EpoxidFiberReinforced` plates for the carbon parts | `Materials.EpoxidFiberReinforced` has metal items ([MaterialsInit#L10359-L10376](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L10359-L10376); declared at [Materials#L707](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/Materials.java#L707)). GT has no carbon-fiber item in core |

Areas [05](../05-armor/recipes.md#open-questions) and [09](../09-machines-utility/recipes.md#open-questions) follow the same rule: an MV armor or turret plate that was "obsidian steel" is Aluminum, an HV gun receiver is Stainless Steel. These are instances of one rule, not conflicting ladders.

## Component index

Stable placeholder registry names, all in the `gregguns:` namespace. Areas 02, 05 and 07 to 09 reference **only** these names ([GGS-06-48](#requirements)). Display names follow the Techguns names, because 02 and 05 already use them, except where the name is a material GT lacks ("Obsidian Steel Receiver" becomes "Hardened Receiver"). The registry name keeps the Techguns lineage so existing placeholders in [pack-loot.md](pack-loot.md) stay valid. "Recipe" links to the row below.

### Gun parts and components

| Registry name | Display name | Techguns (meta) | Tier | Gate | Recipe | Consumed by |
|---|---|---|---|---|---|---|
| `part_barrel_stone` | Stone Barrel | Stone Barrel (25) | ULV | Bronze | [RC-01](#part-barrel-stone) | 02 Hand Cannon |
| `part_barrel_iron` | Iron Barrel | Iron Barrel (27) | LV | Steel (screws) | [RC-04](#part-barrel-iron) | 02 |
| `part_barrel_hardened` | Hardened Steel Barrel | Hardened Steel Barrel (33) | LV | Steel | [RC-05](#part-barrel-hardened) | 02 (the LV Pistol and Bolt Action Rifle use it) |
| `part_barrel_carbon` | Carbon Barrel | Carbon Barrel (85) | EV | Titanium | [RC-06](#part-barrel-carbon) | 02 |
| `part_barrel_laser` | Laser Barrel | Laser Barrel (35) | EV | Titanium | [RC-07](#part-barrel-laser) | 02 |
| `part_stock_wooden` | Wooden Stock | Wooden Stock (26) | ULV | none | [RC-02](#part-stock-wooden) | 02 |
| `part_stock_plastic` | Plastic Gun Stock | Plastic Gun Stock (31) | MV | Aluminum | [RC-08](#part-stock-plastic) | 02 |
| `part_stock_carbon` | Carbon Gunstock | Carbon Gunstock (84) | EV | Titanium | [RC-09](#part-stock-carbon) | 02 |
| `part_pump_mechanism` | Pump Mechanism | Pump Mechanism (32) | LV | Steel | [RC-10](#part-pump-mechanism) | 02 Flamethrower, Bio Gun, Nether Blaster |
| `part_receiver_iron` | Iron Receiver | Iron Receiver (53) | LV | Steel (screws) | [RC-13](#part-receiver-iron) | 02 |
| `part_receiver_steel` | Steel Receiver | Steel Receiver (54) | MV | Aluminum (screws) | [RC-14](#part-receiver-steel) | 02 |
| `part_receiver_hardened` | Hardened Receiver | Obsidian Steel Receiver (55) | HV | Stainless steel | [RC-15](#part-receiver-hardened) | 02 |
| `part_receiver_carbon` | Carbon Receiver | Carbon Receiver (56) | EV | Titanium | [RC-16](#part-receiver-carbon) | 02 |
| `part_mechanical_1` | Mechanical Parts | Mechanical Parts (30) | ULV | Bronze | [RC-03](#part-mechanical-1) | 02, 05, 07 to 09 |
| `part_mechanical_2` | Mechanical Parts T2 | Mechanical Parts T2 (34) | LV | Steel | [RC-11](#part-mechanical-2) | 02, 05, 07 to 09 |
| `part_mechanical_3` | Mechanical Parts T3 | Mechanical Parts T3 (37) | EV | Titanium | [RC-12](#part-mechanical-3) | 02, 05, 07 to 09 |

### Emitters, foci and electronics

| Registry name | Display name | Techguns (meta) | Tier | Gate | Recipe | Consumed by |
|---|---|---|---|---|---|---|
| `part_laser_focus` | Laser Focus | Laser Focus (36) | EV | Titanium | Reaction Chamber, owned by [08 recipes](../08-machines-production/recipes.md#laser-focus-ev) | 02 Laser Barrel (RC-07) |
| `part_uv_emitter` | UV Emitter | UV Emitter (92) | EV | Titanium | [RC-17](#part-uv-emitter) | 08 Reaction Chamber focus |
| `part_sonic_emitter` | Sonic Emitter | Sonic Emitter (97) | EV | Titanium | [RC-18](#part-sonic-emitter) | 02 Sonic Rifle, De-Atomizer |
| `part_heat_ray_focus` | Heat Ray Focus | Reaction Chamber Heat Ray Focus (98) | per 08 | per 08 | Owned by [08 recipes](../08-machines-production/recipes.md#heat-ray-focus-1-recipe) | 08 Reaction Chamber (non-consumed input) |
| `part_rad_emitter` | Rad Emitter | Rad Emitter (108) | ZPM | Naquadah alloy | [RC-19](#part-rad-emitter) | 02 Nuclear Death Ray |
| `part_anti_gravity_core` | Anti Gravity Core | Anti Gravity Core (100) | IV | Tungstensteel | Reaction Chamber, owned by [08 recipes](../08-machines-production/recipes.md#anti-gravity-core-iv) | 05 Anti Gravity Device |
| `part_cybernetic` | Cybernetic Parts | Cybernetic Parts (91) | EV | Titanium | [RC-20](#part-cybernetic) | 02 Nether Blaster |

### Plating, cloth and filters

| Registry name | Display name | Techguns (meta) | Tier | Gate | Recipe | Consumed by |
|---|---|---|---|---|---|---|
| `part_heavy_cloth` | Heavy Cloth | Heavy Cloth (39) | ULV | none | [RC-21](#part-heavy-cloth) | 05 |
| `part_treated_leather` | Treated Leather | Treated Leather (109) | LV | Steel (Bath at LV) | [RC-22](#part-treated-leather) | 05 |
| `part_steam_armor_plating` | Steam Armor Plating | Steam Armor Plating (67) | EV | Titanium | [RC-23](#part-steam-armor-plating) | 05 Steam armor |
| `part_power_plating` | Power Plating | Power Plating (89) | IV | Tungstensteel | [RC-24](#part-power-plating) | 05 Power armor, exo suit |
| `part_gas_mask_filter` | Gas Mask Filter | Gas Mask Filter (74) | MV | Aluminum | [RC-25](#part-gas-mask-filter) | 05 Gas Mask |
| `part_nuclear_warhead` | Small Nuclear Warhead | Small Nuclear Warhead (103) | ZPM | Naquadah alloy | [RC-26](#part-nuclear-warhead) | RC-35 (this sheet) |

### Ammunition

| Registry name | Display name | Techguns (meta) | Tier | Gate | Recipe | Fired by |
|---|---|---|---|---|---|---|
| `ammo_stone_bullet` | Stone Bullet | (0) | ULV | Bronze | [RC-27](#ammo-stone-bullet) | Hand Cannon |
| `ammo_9mm_bullet` | 9mm Bullets | (1) | LV | Brass casing (see [GGS-06-36](#requirements)) | [RC-28](#ammo-9mm-bullet) | Pistol, revolvers, SMG magazines |
| `ammo_shotgun_shell` | Shotgun Shell | (4) | LV | Brass casing | [RC-29](#ammo-shotgun-shell) | Sawed Off, Combat Shotgun |
| `ammo_rifle_bullet` | Rifle Bullets | (7) | LV | Steel | [RC-30](#ammo-rifle-bullet) | Bolt Action Rifle, rifle magazines |
| `ammo_high_caliber_round` | High Caliber Rounds | (19) | HV | Stainless steel | [RC-31](#ammo-high-caliber-round) | AS50 magazine |
| `ammo_advanced_bullet` | Advanced Bullets | (22) | EV | Titanium | [RC-32](#ammo-advanced-bullet) | Advanced magazine |
| `ammo_40mm_grenade` | 40mm Grenade | (93) | MV | Aluminum | [RC-33](#ammo-40mm-grenade) | Grenade Launcher |
| `ammo_rocket` | Rocket | (14) | HV | Stainless steel | [RC-34](#ammo-rocket) | Rocket Launcher, Grim Reaper |
| `ammo_rocket_nuke` | Rocket (Tactical Nuke) | (105) | ZPM | Naquadah alloy | [RC-35](#ammo-rocket-nuke) | Nuke Launcher |
| `ammo_nether_charge` | Nether Charge | (63) | LuV | HSS-G | [RC-36](#ammo-nether-charge) | Nether Blaster |

### Magazines, drums, tanks and cells

| Registry name | Display name | Techguns (meta) | Tier | Gate | Recipes | Used by |
|---|---|---|---|---|---|---|
| `mag_9mm_pistol` / `mag_9mm_pistol_empty` | 9mm Pistol Magazine | (2) / (3) | LV | Steel | [RC-37](#mag-9mm-pistol-empty), [RC-44](#mag-9mm-pistol), [RC-51](#mag-refill) | Pistol, akimbo |
| `mag_smg` / `mag_smg_empty` | SMG Magazine | (5) / (6) | MV | Aluminum | [RC-38](#mag-smg-empty), [RC-45](#mag-smg), [RC-51](#mag-refill) | Thompson, Mac10, Vector |
| `mag_assault_rifle` / `mag_assault_rifle_empty` | Assault Rifle Magazine | (12) / (13) | MV | Aluminum | [RC-39](#mag-assault-rifle-empty), [RC-46](#mag-assault-rifle), [RC-51](#mag-refill) | M4, AK47, AUG, SCAR |
| `mag_as50` / `mag_as50_empty` | AS50 Magazine | (17) / (18) | HV | Stainless steel | [RC-40](#mag-as50-empty), [RC-47](#mag-as50), [RC-51](#mag-refill) | AS50 |
| `mag_lmg` / `mag_lmg_empty` | LMG Magazine | (20) / (21) | HV | Stainless steel | [RC-41](#mag-lmg-empty), [RC-48](#mag-lmg), [RC-51](#mag-refill) | LMG |
| `mag_advanced` / `mag_advanced_empty` | Advanced Magazine | (23) / (24) | EV | Titanium | [RC-42](#mag-advanced-empty), [RC-49](#mag-advanced), [RC-51](#mag-refill) | PDW, Pulse Rifle |
| `mag_minigun_drum` / `mag_minigun_drum_empty` | Minigun Ammo Drum | (46) / (47) | EV | Titanium | [RC-43](#mag-minigun-drum-empty), [RC-50](#mag-minigun-drum), [RC-51](#mag-refill) | Minigun |
| `tank_fuel` | Fuel Tank | Fuel Tank (8, 9) | HV | Stainless steel | [RC-52](#tank-fuel), [RC-56](#tank-fill-fuel) | Flamethrower, Chainsaw |
| `tank_bio` | Bio Tank | Bio Tank (10, 11) | EV | Titanium | [RC-53](#tank-bio), [RC-57](#tank-fill-bio) | Bio Gun |
| `tank_air` | Compressed Air Tank | Compressed Air Tank (28, 29) | HV | Stainless steel | [RC-54](#tank-air), [RC-58](#tank-fill-air) | Power Hammers, Jump Pack, Oxygen Tanks (05) |
| `cell_energy` | Energy Cell | Energy Cell (15, 16) | withdrawn | none | [RC-55](#cell-energy), withdrawn | Not built. Energy weapons are their own batteries ([note](#energy-cell-note)) |
| `cell_nuclear` | Nuclear Power Cell | Nuclear Power Cell (106) | ZPM | Naquadah alloy | [RC-59](#nuclear-power-cell), [RC-60](#nuclear-power-cell) | Nuclear Death Ray, Anti Gravity Device (05) |
| `cell_nuclear_depleted` | Nuclear Power Cell (Depleted) | (107) | ZPM | Naquadah alloy | not craftable; made by use | Returned on reload; input of RC-60 |

### Consumables and tools

| Registry name | Display name | Techguns | Tier | Gate | Recipe |
|---|---|---|---|---|---|
| `bandages` | Bandages | Medkit meta 0 | LV | Steel | [RC-61](#consumables) |
| `medkit` | Medkit | Medkit meta 1 | MV | Aluminum | [RC-62](#consumables) |
| `combat_knife` | Combat Knife | Combat Knife | LV | Steel | Owned by [02 recipes](../02-guns/recipes.md#lv-steel) |
| `crowbar` | Crowbar | Crowbar | LV | Steel | Owned by [02 recipes](../02-guns/recipes.md#lv-steel) |
| `bunker_door` | Bunker Door | Bunker Door (`doorItem01`) | LV | Iron plates | Owned by [07 recipes](../07-blocks/recipes.md#chests-and-the-door) |
| `camo_net_placer` (metas 0 to 2) | Camo Net Placer (Woodland, Snow, Desert) | Camo Net Placer | ULV | none | [RC-63](#tools-and-placeables) |

Guns, grenades, armor, blocks and machines are in their own areas. The Frag Grenade, Stielgranate and Flashbang are guns ([02](../02-guns/recipes.md)); the Glider, wings and backpack, Oxygen Mask and Tactical Mask are armor ([05](../05-armor/armor-recipes.md)); turret armor is [09](../09-machines-utility/index.md); the Camo Net block is [07](../07-blocks/decorative-blocks.md).

## Gun parts

Replaces [R01 to R13, R23, R24](../../techguns-spec/06-items/recipes.md#1-gun-parts-and-components). Each part's tier is the lowest tier of a gun that uses it, with the exceptions listed under [Open questions](#open-questions).

### Steam / ULV crafting-table parts

<a id="part-barrel-stone"></a>
**RC-01 · `part_barrel_stone` ×1** · ULV · shaped crafting · gate `pipeTiny.Bronze` · replaces R01

```
S S S
f P h
S S S
```

`S` = ore name `stone` ×6, `P` = `pipeTiny.Bronze`, `f` = file, `h` = hammer. The Bronze pipe is the bore liner and the gate.

<a id="part-stock-wooden"></a>
**RC-02 · `part_stock_wooden` ×1** · ULV · shaped crafting · no gate · replaces R02

```
W W
s W
```

`W` = ore name `logWood` ×3, `s` = saw.

<a id="part-mechanical-1"></a>
**RC-03 · `part_mechanical_1` ×1** · ULV · shaped crafting · gate `Bronze` · replaces R04

```
h G f
G S G
· G ·
```

`G` = `gearGtSmall.Bronze` ×4, `S` = `springSmall.Bronze`, `h` = hammer, `f` = file. Wrought iron may stand in for bronze (0001 lists both for Steam/ULV); the recipe is registered once per material that exists.

### Machine-made parts

All rows are `Asm`, `c=1` unless noted.

| ID | Output | Tier | Inputs | EU/t, time | Gate, notes |
|---|---|---|---|---|---|
| RC-04<a id="part-barrel-iron"></a> | `part_barrel_iron` ×1 | LV | `pipeSmall.Iron` ×1, `ring.Iron` ×2, `screw.Steel` ×2 | `RECIPE_LV`, 5 s | `Steel` (screws). Replaces R03. Barrel = liner pipe plus two mounting rings. Mirrors GT's [Large Fluid Cell (Steel) recipe](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblerRecipes.java#L104-L112): 10 s at LV for four double plates and four rings |
| RC-05<a id="part-barrel-hardened"></a> | `part_barrel_hardened` ×1 | LV | `pipeSmall.Steel` ×1, `ring.Steel` ×2, `plate.Steel` ×1 | `RECIPE_LV`, 6 s | `Steel`. Replaces R06. LV because [02](../02-guns/recipes.md#lv-steel) builds the 9mm Pistol and the Bolt Action Rifle from it; the steel jacket plate is what "hardened" means here |
| RC-06<a id="part-barrel-carbon"></a> | `part_barrel_carbon` ×1 | EV | `pipeSmall.Titanium` ×1, `ring.Titanium` ×2, `plate.EpoxidFiberReinforced` ×2 | `RECIPE_EV`, 8 s | `Titanium`. Replaces R23 (the "carbon" wrap is fiber-reinforced epoxy) |
| RC-07<a id="part-barrel-laser"></a> | `part_barrel_laser` ×1 | EV | `part_laser_focus` ×1, `lens.Glass` ×1, `pipeTiny.Titanium` ×2, `foil.Electrum` ×4 | `RECIPE_EV`, 10 s | `Titanium`. Replaces R08 (electrum or gold, glass, Laser Focus). `lens.Glass` is registered by GT ([MTEIndustrialLaserEngraver#L482](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialLaserEngraver.java#L482)) |
| RC-08<a id="part-stock-plastic"></a> | `part_stock_plastic` ×1 | MV | `plate.Polyethylene` ×3, `screw.Aluminium` ×2 | `RECIPE_MV`, 4 s | `Aluminium`. Replaces R05. Polyethylene is GT's plastic (`Materials.Polyethylene`, no `Plastic` material exists) |
| RC-09<a id="part-stock-carbon"></a> | `part_stock_carbon` ×1 | EV | `plate.EpoxidFiberReinforced` ×3, `plate.Polyethylene` ×2, `screw.Titanium` ×2 | `RECIPE_EV`, 5 s | `Titanium`. Replaces R24 (3 carbon plates, 2 plastic sheets) |
| RC-10<a id="part-pump-mechanism"></a> | `part_pump_mechanism` ×1 | LV | `springSmall.Steel` ×2, `pipeTiny.Steel` ×1, `ring.Steel` ×2, `plate.Rubber` ×2 | `RECIPE_LV`, 4 s | `Steel`. Replaces R07 (nuggets, glass, piston). Rubber seals instead of an electric piston |
| RC-11<a id="part-mechanical-2"></a> | `part_mechanical_2` ×2 | LV | `gearGtSmall.Steel` ×2, `springSmall.Steel` ×2, `screw.Steel` ×4 | `RECIPE_LV`, 5 s | `Steel`. Replaces Metal Press recipe 9 (obsidian steel plate + quartz) |
| RC-12<a id="part-mechanical-3"></a> | `part_mechanical_3` ×2 | EV | `gearGtSmall.Titanium` ×2, `springSmall.Titanium` ×2, `screw.Titanium` ×4 | `RECIPE_EV`, 6 s | `Titanium`. Replaces Metal Press recipe 10 (carbon plate + blaze rod, ×2) |
| RC-13<a id="part-receiver-iron"></a> | `part_receiver_iron` ×1 | LV | `plate.Iron` ×3, `part_mechanical_1` ×1, `screw.Steel` ×2 | `RECIPE_LV`, 5 s | `Steel` (screws). Replaces R10 (`iii / ·mn / ··n`) |
| RC-14<a id="part-receiver-steel"></a> | `part_receiver_steel` ×1 | MV | `plate.Steel` ×3, `part_mechanical_2` ×1, `screw.Aluminium` ×2 | `RECIPE_MV`, 6 s | `Aluminium` (screws). Replaces R11 |
| RC-15<a id="part-receiver-hardened"></a> | `part_receiver_hardened` ×1 | HV | `plate.StainlessSteel` ×3, `part_mechanical_2` ×1, `screw.StainlessSteel` ×2 | `RECIPE_HV`, 8 s | `StainlessSteel`. Replaces R12, which asked for Mechanical Parts T3 (carbon). That would put an EV input under an HV part, so the Mk II part is used |
| RC-16<a id="part-receiver-carbon"></a> | `part_receiver_carbon` ×1 | EV | `plate.Titanium` ×3, `plate.EpoxidFiberReinforced` ×1, `part_mechanical_3` ×1, `screw.Titanium` ×2 | `RECIPE_EV`, 8 s | `Titanium`. Replaces R13 |

## Emitters, foci and electronics

Replaces the Fabricator plans ([fabricator.md](../../techguns-spec/08-machines-production/fabricator.md)) and R30. GT components: `ItemList.Emitter_<tier>` ([L465-L471](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L465-L471)), `Electric_Piston_EV` ([L438](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L438)).

| ID | Output | Tier | Method | Inputs | EU/t, time | Gate, notes |
|---|---|---|---|---|---|---|
| RC-17<a id="part-uv-emitter"></a> | `part_uv_emitter` ×1 | EV | Asm | `ItemList.Emitter_EV` ×1, `lens.Diamond` ×1, `plate.Titanium` ×2, `circuit.EV` ×1 | `RECIPE_EV`, 8 s | `Titanium`. [0001](../../design/0001-tier-map.md) keeps the UV Emitter as an EV component. Techguns had no recipe ([ITEM-18](../../techguns-spec/06-items/index.md#suspected-bugs)). `lens.Diamond` is registered by GT ([laser engraver L484](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/machines/multi/MTEIndustrialLaserEngraver.java#L484)) |
| RC-18<a id="part-sonic-emitter"></a> | `part_sonic_emitter` ×1 | EV | Asm | `ItemList.Emitter_EV` ×1, `ItemList.Electric_Piston_EV` ×1, `part_mechanical_3` ×1, `plate.Titanium` ×1 | `RECIPE_EV`, 10 s | `Titanium`. Replaces Fabricator plan 4 (coil, 2 elite circuits, T3 parts, titanium plate). The piston is the oscillator |
| RC-19<a id="part-rad-emitter"></a> | `part_rad_emitter` ×1 | ZPM | AL | `ItemList.Emitter_ZPM` ×1, `plate.Lead` ×2, `dust.Uranium235` ×1, `part_mechanical_3` ×2, `circuit.ZPM` ×2; fluid `Materials.SolderingAlloy.getMolten(144)` | `RECIPE_ZPM`, 30 s | `NaquadahAlloy` through the ZPM emitter. Research item `ItemList.Emitter_LuV`, `Scanning(2 * MINUTES, RECIPE_IV)`. Replaces Fabricator plan 5. The radiation comes from the dust through GT ([hazards-and-tooltips.md](hazards-and-tooltips.md#gt-radiation-for-nuclear-items)) |
| RC-20<a id="part-cybernetic"></a> | `part_cybernetic` ×2 | EV | Asm | `wireFine.Electrum` ×4, `circuit.EV` ×2, `plate.Polyethylene` ×2, `dust.Redstone` ×1, `dust.SoulSand` ×1 | `RECIPE_EV`, 8 s | `Titanium` via `circuit.EV`. Replaces Fabricator plan 2 (soul sand, gold wire, redstone, plastic). Hits [0001](../../design/0001-tier-map.md)'s EV tier even though its only gun user, the Nether Blaster, is LuV |

**Owned by area 08.** The Heat Ray Focus (Techguns R30), the Laser Focus (Reaction Chamber recipe 2, EV) and the Anti Gravity Core (Reaction Chamber recipe 6, IV) are written in [08 recipes](../08-machines-production/recipes.md). This sheet reserves their registry names and does not restate their recipes. The Quartz Rod (Techguns R27/R28) is dropped: the Reaction Chamber row that used it is dropped.

## Plating, cloth and filters

Replaces [R09, R25, R29](../../techguns-spec/06-items/recipes.md#1-gun-parts-and-components) and the Metal Press, Chem Lab and Fabricator rows for these items.

<a id="part-heavy-cloth"></a>
**RC-21 · `part_heavy_cloth` ×3** · ULV · shaped crafting · no gate · replaces R09

```
· W ·
W L W
· W ·
```

`W` = ore name `blockWool` ×4 (any color), `L` = vanilla leather. Unchanged from Techguns.

| ID | Output | Tier | Method | Inputs | EU/t, time | Gate, notes |
|---|---|---|---|---|---|---|
| RC-22<a id="part-treated-leather"></a> | `part_treated_leather` ×2 | LV | Bath | Vanilla leather ×2; fluid `Materials.SulfuricAcid.getFluid(500)` | `RECIPE_LV`, 10 s | The Bath's own LV voltage. Replaces Chem Lab recipe 15 (leather, slimeball, creeper acid). Creeper acid is dropped ([fluids.md](fluids.md#fluid-mapping)); GT sulfuric acid takes its place and the slimeball is dropped |
| RC-23<a id="part-steam-armor-plating"></a> | `part_steam_armor_plating` ×1 | EV | Asm, `c=1` | `plate.Titanium` ×2, `plate.Bronze` ×1, `plate.Steel` ×1 | `RECIPE_EV`, 6 s | `Titanium`. Replaces Metal Press recipe 15 (steel plate + bronze plate). Matches the proposal in [05 recipes](../05-armor/recipes.md#assumed-components), which [recipe-map-replacements.md](../08-machines-production/recipe-map-replacements.md#metal-press-18-recipes) gates at EV |
| RC-24<a id="part-power-plating"></a> | `part_power_plating` ×2 | IV | Asm | `plate.TungstenSteel` ×2, `plate.EpoxidFiberReinforced` ×4, `part_mechanical_3` ×1, `circuit.EV` ×2 | `RECIPE_IV`, 15 s | `TungstenSteel`. Replaces Fabricator plan 3 (2 titanium ingots, 4 basic circuits, T3 parts, 4 carbon plates, ×2) |
| RC-25<a id="part-gas-mask-filter"></a> | `part_gas_mask_filter` ×1 | MV | Asm | `ItemList.ActivatedCarbonFilterMesh` ×1, `plate.Aluminium` ×1, `ring.Aluminium` ×1 | `RECIPE_MV`, 4 s | `Aluminium`. Replaces R25. The mesh is GT's ([ItemList#L2205](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L2205)); confirm it is obtainable by MV in the pack |
| RC-26<a id="part-nuclear-warhead"></a> | `part_nuclear_warhead` ×2 | ZPM | AL | `plate.Lead` ×4, `dust.TNT` ×4, `circuit.LuV` ×2, `dust.Uranium235` ×2; fluid `Materials.SolderingAlloy.getMolten(144)` | `RECIPE_ZPM`, 20 s | Research item `ammo_rocket`, `Scanning(1 * MINUTES, RECIPE_EV)`. Replaces R29 (lead plates, circuits, 2 TGX, enriched uranium). TGX becomes `Materials.TNT`; `Materials.TNT` exists ([Materials.java#L389](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/Materials.java#L389)) |

## Ammunition

Replaces R32 to R34, R42, R57 and the Ammo Press plans ([ammo-press.md](../../techguns-spec/08-machines-production/ammo-press.md)). Absorbs the design table in [ammo.md](ammo.md#gregtech-recipe-maps-for-ammo).

### Where the parts come from

- **Slug.** GT `round`: a metal slug worth one ninth of an ingot ([OrePrefixes#L960-L969](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/OrePrefixes.java#L960-L969)), made by [`ProcessingRound`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/oreprocessing/ProcessingRound.java#L28-L48) in the Lathe from a nugget (`RECIPE_ULV`) and by hand with a file and hammer.
- **Case, small.** GT `foil` ([OrePrefixes#L925-L933](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/OrePrefixes.java#L925-L933)), one quarter of an ingot, made in the Bending Machine. Brass is `Materials.Brass` ([MaterialsInit#L5853-L5876](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L5853-L5876)).
- **Case, large.** GT `itemCasing`, "Casing made of 1/2 Ingot" ([OrePrefixes#L913-L922](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/OrePrefixes.java#L913-L922)). GT already makes it from a plate (hand: `h X`), by Cutting Machine, Extruder (`Shape_Extruder_Casing`) and Alloy Smelter (`Shape_Mold_Casing`): [ProcessingPlate#L394-L466](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/oreprocessing/ProcessingPlate.java#L394-L466). Used only for the AS50 round.
- **Powder.** `dust.Gunpowder` is the vanilla gunpowder item: GT removes its own dust prefix for the material ([MaterialsInit#L4951-L4966](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L4951-L4966)). GT's own recipes fetch it as `GTOreDictUnificator.get(OrePrefixes.dust, Materials.Gunpowder, 1)` ([MixerRecipes#L1618](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/MixerRecipes.java#L1618)).
- **No casing item and no Extruder or Forming Press recipe for ammo.** GT already makes every case shape. This answers the "extruder shape" question in [ammo.md](ammo.md#open-questions): none is needed.

### Batch sizes

A GT recipe consumes its inputs once, so ammo is made in batches, the way GT turns one ingot into several screws or rounds. Each batch is sized so that the inputs fill whole stacks (16 foil, 16 rounds) and a run takes 4 to 10 seconds.

| ID | Output | Tier | Method | Inputs | EU/t, time | Gate, notes |
|---|---|---|---|---|---|---|
| RC-27<a id="ammo-stone-bullet"></a> | `ammo_stone_bullet` ×16 | ULV | Shapeless crafting | Ore name `cobblestone` ×3, `dust.Gunpowder` ×1, `nugget.Bronze` ×1 | none | `Bronze` (the band). Replaces R32. No machine exists at this tier |
| RC-27b<a id="ammo-stone-bullet-press"></a> | `ammo_stone_bullet` ×24 | Steam | AP (Steam Ammo Press) | Ore name `cobblestone` ×3, `dust.Gunpowder` ×1, `nugget.Bronze` ×1 | `RECIPE_ULV`, 5 s | Bronze. The Steam Ammo Press yields more than the hand recipe, which rewards building it ([ammo-press.md](../08-machines-production/ammo-press.md#tiers-and-what-each-runs)) |
| RC-28<a id="ammo-9mm-bullet"></a> | `ammo_9mm_bullet` ×16 | LV | AP, `c=1` | `foil.Brass` ×16, `round.Lead` ×16, `dust.Gunpowder` ×2 | `RECIPE_LV`, 5 s | Replaces R33 and Ammo Press plan 0 (×12). See the cost note below |
| RC-29<a id="ammo-shotgun-shell"></a> | `ammo_shotgun_shell` ×8 | LV | AP, `c=2` | `foil.Brass` ×8, `round.Lead` ×24, `dust.Gunpowder` ×4 | `RECIPE_LV`, 6 s | Replaces R34 and plan 1 (×16). Three pellets per shell |
| RC-30<a id="ammo-rifle-bullet"></a> | `ammo_rifle_bullet` ×16 | LV | AP, `c=3` | `foil.Brass` ×16, `round.Steel` ×16, `dust.Gunpowder` ×4 | `RECIPE_LV`, 6 s | `Steel`. Replaces plan 2 (×8). Techguns' crafting recipe was commented out |
| RC-31<a id="ammo-high-caliber-round"></a> | `ammo_high_caliber_round` ×8 | HV | AP, `c=4` | `itemCasing.Brass` ×8, `round.StainlessSteel` ×16, `dust.Gunpowder` ×4 | `RECIPE_HV`, 8 s | `StainlessSteel`. Replaces plan 3 (×4). Two slugs per round |
| RC-32<a id="ammo-advanced-bullet"></a> | `ammo_advanced_bullet` ×16 | EV | AP, `c=5` | `foil.Titanium` ×16, `round.Titanium` ×16, `dust.TNT` ×2 | `RECIPE_EV`, 4 s | `Titanium`. Replaces Metal Press recipe 13 (obsidian steel plate + TGX, ×16) |
| RC-33<a id="ammo-40mm-grenade"></a> | `ammo_40mm_grenade` ×8 | MV | AP, `c=6` | `plate.Aluminium` ×2, `dust.TNT` ×2, `dust.Gunpowder` ×1 | `RECIPE_MV`, 8 s | `Aluminium`. Replaces Metal Press recipe 17 (iron plate + TNT, ×16) |
| RC-34<a id="ammo-rocket"></a> | `ammo_rocket` ×4 | HV | AP, `c=7` | `plate.StainlessSteel` ×4, `dust.TNT` ×2, `dust.Gunpowder` ×4, `circuit.HV` ×1 | `RECIPE_HV`, 10 s | `StainlessSteel`. Replaces R42 (5 nuggets, TNT, gunpowder, ×4). The circuit is the fuze and guidance |

**Cost note.** A `foil` is a quarter ingot and a `round` a ninth, so a 9mm bullet costs about 0.36 ingot (a quarter of brass, a ninth of lead). Techguns' 8 bullets for one lead ingot, 8 copper nuggets and gunpowder cost about 0.24 ingot each, so GregGuns ammo is about 1.5 times dearer per bullet. That buys the GT-native parts and is a balance question for M2 ([Open questions](#open-questions)).

### Tactical Nuke Rocket and Nether Charge (ZPM and LuV)

[CONVENTIONS](../CONVENTIONS.md#recipe-sheets) sends LuV and up to the Assembly Line. The research item is the previous-tier item, as in GT's own LuV motor ([AssemblyLineRecipes#L61-L74](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/postload/recipes/AssemblyLineRecipes.java#L61-L74)). Scanning times are starting values for M5. `eut` is the item's own tier per 0001; GT itself builds its LuV motor at `RECIPE_IV`, which [02](../02-guns/recipes.md#open-questions) discusses.

| ID | Output | Tier | Method | Inputs | EU/t, time | Research | Gate, notes |
|---|---|---|---|---|---|---|---|
| RC-35<a id="ammo-rocket-nuke"></a> | `ammo_rocket_nuke` ×1 | ZPM | AL | `ammo_rocket` ×1, `part_nuclear_warhead` ×1, `plate.NaquadahAlloy` ×2, `plate.Lead` ×2, `circuit.ZPM` ×1; fluid `Materials.SolderingAlloy.getMolten(288)` | `RECIPE_ZPM`, 30 s | `ammo_rocket`, `Scanning(1 * MINUTES, RECIPE_EV)` | `NaquadahAlloy`. Replaces R57 (rocket + warhead, shapeless) |
| RC-36<a id="ammo-nether-charge"></a> | `ammo_nether_charge` ×16 | LuV | AL | `dust.Netherrack` ×16, `dust.SoulSand` ×8, `plate.HSSG` ×2, `circuit.LuV` ×1, `dust.Gunpowder` ×4; fluid `FluidRegistry.LAVA` ×4000 mB | `RECIPE_LuV`, 20 s | `ammo_advanced_bullet`, `Scanning(1 * MINUTES, RECIPE_EV)` | `HSSG`. Replaces Chem Lab recipe 11 (netherrack, soul sand, lava, ×4) |

## Magazines and drums

Replaces [R35 to R49, R51 to R54](../../techguns-spec/06-items/recipes.md#2-ammunition-magazines-and-tanks). Reload, emptying and tooltips are unchanged ([Techguns magazine items](../../techguns-spec/06-items/ammo.md#magazine-items)). Bullets per magazine are Techguns' counts, which tie to `shotsPerBullet` in 02:

| Magazine | Bullets per magazine | Clip |
|---|---|---|
| `mag_9mm_pistol` | 3 × `ammo_9mm_bullet` | 18 |
| `mag_smg` | 2 × `ammo_9mm_bullet` | 20 to 32 |
| `mag_assault_rifle` | 3 × `ammo_rifle_bullet` | 20 to 30 |
| `mag_as50` | 2 × `ammo_high_caliber_round` | 10 |
| `mag_lmg` | 8 × `ammo_rifle_bullet` | 100 |
| `mag_advanced` | 3 × `ammo_advanced_bullet` | 12 to 20 |
| `mag_minigun_drum` | 16 × `ammo_rifle_bullet` | 200 |

### Empty magazines (Assembler)

| ID | Output | Tier | Method | Inputs | EU/t, time | Gate, notes |
|---|---|---|---|---|---|---|
| RC-37<a id="mag-9mm-pistol-empty"></a> | `mag_9mm_pistol_empty` ×8 | LV | AP, `c=8` | `plate.Steel` ×2, `springSmall.Steel` ×8, `part_mechanical_2` ×1 | `RECIPE_LV`, 4 s | `Steel`. Replaces R35 (×4) |
| RC-38<a id="mag-smg-empty"></a> | `mag_smg_empty` ×8 | MV | AP, `c=9` | `plate.Aluminium` ×2, `springSmall.Aluminium` ×8, `part_mechanical_2` ×1 | `RECIPE_MV`, 4 s | `Aluminium`. Replaces R37 |
| RC-39<a id="mag-assault-rifle-empty"></a> | `mag_assault_rifle_empty` ×8 | MV | AP, `c=10` | `plate.Aluminium` ×3, `springSmall.Aluminium` ×8, `part_mechanical_2` ×1 | `RECIPE_MV`, 5 s | `Aluminium`. Replaces R39 |
| RC-40<a id="mag-as50-empty"></a> | `mag_as50_empty` ×8 | HV | AP, `c=11` | `plate.StainlessSteel` ×3, `springSmall.StainlessSteel` ×8, `part_mechanical_2` ×1 | `RECIPE_HV`, 5 s | `StainlessSteel`. Replaces R46 |
| RC-41<a id="mag-lmg-empty"></a> | `mag_lmg_empty` ×4 | HV | AP, `c=12` | `plate.StainlessSteel` ×4, `springSmall.StainlessSteel` ×4, `part_mechanical_2` ×1 | `RECIPE_HV`, 6 s | `StainlessSteel`. Replaces R43 (seven steel ingots) |
| RC-42<a id="mag-advanced-empty"></a> | `mag_advanced_empty` ×8 | EV | AP, `c=13` | `plate.Titanium` ×3, `springSmall.Titanium` ×8, `part_mechanical_3` ×1 | `RECIPE_EV`, 5 s | `Titanium`. Replaces R48 |
| RC-43<a id="mag-minigun-drum-empty"></a> | `mag_minigun_drum_empty` ×4 | EV | AP, `c=14` | `plate.Titanium` ×4, `plate.Polyethylene` ×2, `part_mechanical_3` ×1 | `RECIPE_EV`, 6 s | `Titanium`. Replaces R53 |

### Filled magazines in bulk (Assembler)

Four magazines per run, one stack of bullets in. Same voltage as the empty. The crafting-table refill below is for single magazines and for magazines returned from reloads.

| ID | Output | Tier | Method | Inputs | EU/t, time |
|---|---|---|---|---|---|
| RC-44<a id="mag-9mm-pistol"></a> | `mag_9mm_pistol` ×4 | LV | AP, `c=15` | `mag_9mm_pistol_empty` ×4, `ammo_9mm_bullet` ×12 | `RECIPE_LV`, 2 s |
| RC-45<a id="mag-smg"></a> | `mag_smg` ×4 | MV | AP, `c=16` | `mag_smg_empty` ×4, `ammo_9mm_bullet` ×8 | `RECIPE_MV`, 2 s |
| RC-46<a id="mag-assault-rifle"></a> | `mag_assault_rifle` ×4 | MV | AP, `c=17` | `mag_assault_rifle_empty` ×4, `ammo_rifle_bullet` ×12 | `RECIPE_MV`, 2 s |
| RC-47<a id="mag-as50"></a> | `mag_as50` ×4 | HV | AP, `c=18` | `mag_as50_empty` ×4, `ammo_high_caliber_round` ×8 | `RECIPE_HV`, 2 s |
| RC-48<a id="mag-lmg"></a> | `mag_lmg` ×4 | HV | AP, `c=19` | `mag_lmg_empty` ×4, `ammo_rifle_bullet` ×32 | `RECIPE_HV`, 3 s |
| RC-49<a id="mag-advanced"></a> | `mag_advanced` ×4 | EV | AP, `c=20` | `mag_advanced_empty` ×4, `ammo_advanced_bullet` ×12 | `RECIPE_EV`, 2 s |
| RC-50<a id="mag-minigun-drum"></a> | `mag_minigun_drum` ×4 | EV | AP, `c=21` | `mag_minigun_drum_empty` ×4, `ammo_rifle_bullet` ×64 | `RECIPE_EV`, 3 s |

The Techguns shaped LMG recipe (R44, a ring of eight bullets) and the Rifle Bullets Stack recipes (R51, R52, R54) are dropped; R45 and R54 become the refill below.

<a id="mag-refill"></a>
**RC-51 · Magazine refill** · shapeless crafting · any tier · replaces R36, R38, R40, R44 to R45, R47, R49, R54

One recipe per magazine type: one empty magazine plus the bullet count from the table above gives one full magazine. A crafting-table recipe has no voltage, so the gate sits in the inputs: both the empty magazine and the bullets are tier-gated items.

| Output | Inputs (shapeless) |
|---|---|
| `mag_9mm_pistol` ×1 | `mag_9mm_pistol_empty` + `ammo_9mm_bullet` ×3 |
| `mag_smg` ×1 | `mag_smg_empty` + `ammo_9mm_bullet` ×2 |
| `mag_assault_rifle` ×1 | `mag_assault_rifle_empty` + `ammo_rifle_bullet` ×3 |
| `mag_as50` ×1 | `mag_as50_empty` + `ammo_high_caliber_round` ×2 |
| `mag_lmg` ×1 | `mag_lmg_empty` + `ammo_rifle_bullet` ×8 |
| `mag_advanced` ×1 | `mag_advanced_empty` + `ammo_advanced_bullet` ×3 |
| `mag_minigun_drum` ×1 | `mag_minigun_drum_empty` + `ammo_rifle_bullet` ×16 |

Sneak-use to empty a full magazine returns the empty magazine and the same bullet counts (not stacks).

## Tanks and cells

Replaces R41, R50, R55, R56 and the Chem Lab fill and drain plans ([fluids.md](fluids.md), [02 fluid-ammo](../02-guns/fluid-ammo.md)). Tanks are `IFluidContainerItem` items with a 1,000 mB capacity (config). The empty tank is the same item with no fluid; there is no separate "empty" registry name.

| ID | Output | Tier | Method | Inputs | EU/t, time | Gate, notes |
|---|---|---|---|---|---|---|
| RC-52<a id="tank-fuel"></a> | `tank_fuel` ×4 | HV | Asm, `c=1` | `ItemList.Cell_Empty` ×4, `plate.StainlessSteel` ×4, `ring.StainlessSteel` ×4, `plate.Rubber` ×2 | `RECIPE_HV`, 10 s | `StainlessSteel`. Replaces R50 (plastic, glass, ×4). Mirrors the Large Fluid Cell recipe cited at RC-04; `Cell_Empty` is [ItemList#L200](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L200) |
| RC-53<a id="tank-bio"></a> | `tank_bio` ×4 | EV | Asm, `c=1` | `ItemList.Cell_Empty` ×4, `plate.Titanium` ×4, `ring.Titanium` ×4, `plate.Polyethylene` ×2 | `RECIPE_EV`, 8 s | `Titanium`. Replaces R41 (steel nuggets, glass) |
| RC-54<a id="tank-air"></a> | `tank_air` ×4 | HV | Asm, `c=2` | `ItemList.Cell_Empty` ×4, `plateDouble.StainlessSteel` ×4, `ring.StainlessSteel` ×4 | `RECIPE_HV`, 10 s | `StainlessSteel`. Replaces R55, R56 (7 plates and a lever, ×7). Double plates for pressure |
| RC-55<a id="cell-energy"></a> | (withdrawn) | n/a | n/a | n/a | n/a | Withdrawn by [0010](../../design/0010-material-remap-and-tier-additions.md) decision 4. The ID is not reused or renumbered |

<a id="tank-fills"></a>
### Filling tanks

The Fluid Canner fills any `IFluidContainerItem` on the fly: `FluidCannerBackend.findFallback` builds a recipe at `eut(1)` lasting `max(amount / 64, 16)` ticks, so a 1,000 mB tank takes 16 ticks ([FluidCannerBackend#L28-L68](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/maps/FluidCannerBackend.java#L28-L68)). It fills only an **empty** container, and only when the supplied fluid is at least the whole capacity ([GTUtility#L1186-L1209](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1186-L1209)). GregGuns registers no Canner recipe. The rows below record what the fill gives, replacing the Chem Lab plans 5a, 6b, 6c, 9 and 10.

| ID | Method | Input | Fluid in | Output |
|---|---|---|---|---|
| RC-56<a id="tank-fill-fuel"></a> | `cannerRecipes` (dynamic), `eut(1)`, 16 ticks | `tank_fuel` (empty) | 1,000 mB of `Materials.Diesel`, `GasolineRegular`, `BioDiesel` or `Ethanol` (config; `NitroFuel`, `GasolinePremium` optional) | `tank_fuel` holding that fluid |
| RC-57<a id="tank-fill-bio"></a> | same | `tank_bio` (empty) | 1,000 mB of `Materials.Biomass` | `tank_bio` holding `Biomass` |
| RC-58<a id="tank-fill-air"></a> | same | `tank_air` (empty) | 1,000 mB of `Materials.Air.getGas` | `tank_air` holding `Air` |

RC-55 is withdrawn (see below). RC-56 to RC-58 are the three dynamic fills, which GregGuns does not register.

<a id="energy-cell-note"></a>
**Energy Cell (withdrawn).** [0010](../../design/0010-material-remap-and-tier-additions.md) decision 4 drops `cell_energy`: each energy weapon is its own IC2 battery ([02](../02-guns/energy-electric-items.md)), so no recipe consumes a cell. RC-55 and GGS-06-41 are withdrawn, not renumbered; the anchor is kept so old links resolve.

### Nuclear Power Cell

`cell_nuclear` is **not** an electric item. It is a consumable: a reload consumes the full cell and returns `cell_nuclear_depleted` ([Techguns reload table](../../techguns-spec/06-items/ammo.md#gun--ammo-table)). Both items are radioactive through GT ([hazards-and-tooltips.md](hazards-and-tooltips.md#gt-radiation-for-nuclear-items)).

| ID | Output | Tier | Method | Inputs | EU/t, time | Gate, notes |
|---|---|---|---|---|---|---|
| RC-59<a id="cell-nuclear"></a> | `cell_nuclear` ×2 | ZPM | AL | `plate.NaquadahAlloy` ×4, `plate.Lead` ×4, `dust.Uranium235` ×8, `dust.Redstone` ×4, `circuit.ZPM` ×1; fluid `Materials.SolderingAlloy.getMolten(288)` | `RECIPE_ZPM`, 30 s | Research item `ItemList.Energy_LapotronicOrb` ([ItemList#L713](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/ItemList.java#L713)), `Scanning(1 * MINUTES, RECIPE_EV)` (was `cell_energy`, withdrawn). `NaquadahAlloy`. Replaces Fabricator plan 6 plus Chem Lab plan 13 as one step from raw material |
| RC-60<a id="cell-nuclear-refuel"></a> | `cell_nuclear` ×1 | ZPM | Asm, `c=1` | `cell_nuclear_depleted` ×1, `dust.Uranium235` ×4 | `RECIPE_ZPM`, 20 s | `Uranium235` plus a ZPM Assembler. Replaces Chem Lab plan 13 (depleted cell, enriched uranium, water). **Exception to the Assembly Line rule**: it converts a returned item, not a new craft. See [Open questions](#open-questions) |

`cell_nuclear_depleted` has no crafting recipe. Techguns' Fabricator plan 6 for an empty cell is dropped: the full-cell recipe above starts from raw material.

## Consumables

<a id="bandages"></a>
**RC-61 · `bandages` ×2** · LV · Asm, `c=2`, `RECIPE_LV`, 5 s · gate `ring.Steel`

Inputs: string ×3, ore name `slimeball` ×1, `ring.Steel` ×1 (the clasp, and the gate). Not listed in [0001](../../design/0001-tier-map.md); LV per [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5, so it is a machine recipe like the rest of LV. Heals 2 HP and gives Regeneration I (use time 60 ticks), per the [Techguns behavior](../../techguns-spec/06-items/special-items.md#medkit-itemmedkit-present-in-code-not-registered).

| ID | Output | Tier | Method | Inputs | EU/t, time | Gate, notes |
|---|---|---|---|---|---|---|
| RC-62<a id="medkit"></a> | `medkit` ×1 | MV | Asm, `c=1` | `plate.Aluminium` ×2, `bandages` ×4, `dust.Redstone` ×1; fluid `Materials.Ethanol.getFluid(250)` | `RECIPE_MV`, 10 s | `Aluminium`. [0001](../../design/0001-tier-map.md): Medkit at MV. Techguns had no recipe (commented out). Heals 6 HP, Regeneration II, use time 50 ticks. Ethanol is the disinfectant ([Materials.java#L364](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/Materials.java#L364)) |

## Tools and placeables

Replaces [R84 to R86](../../techguns-spec/06-items/recipes.md#4-tools-wearables-and-stand-alone-items). Three 06 items have their recipe written by another area, so the two sheets cannot disagree:

| Item | Recipe owner |
|---|---|
| `combat_knife`, `crowbar` | [02 recipes](../02-guns/recipes.md#lv-steel) (LV Assembler rows) |
| `bunker_door` ×2 | [07 recipes](../07-blocks/recipes.md#chests-and-the-door) (Assembler, `plate.Iron` ×6, `RECIPE_LV / 2`, 12 s). The door block is 07's too |

<a id="camo-net-placer"></a>
**RC-63 · `camo_net_placer` meta 0, 1, 2** · ULV · shaped crafting · no gate · replaces R84 to R86

```
S N S
```

`S` = ore name `stickWood`, `N` = the Camo Net block of the matching camo (meta 0 Woodland, 1 Snow, 2 Desert). The Camo Net block is owned by [07](../07-blocks/decorative-blocks.md); its registry name is an assumption here (`gregguns:camo_net`).

## Techguns items with no GregGuns item

For completeness: every Techguns meta not covered above, and where it went. See also the [part-by-part mapping](components-and-materials.md#part-by-part).

| Techguns | Result | Replacement |
|---|---|---|
| Metal plates, ingots, titanium ore (40 to 45, 65, 94 to 96, 104) | Not registered | GT `plate`, `ingot` |
| Wires (57, 86), circuit boards (68, 87) | Not registered | GT `wireFine`, `circuit.<tier>` |
| Raw rubber, rubber bar, raw plastic, plastic sheet (70 to 73) | Not registered | GT `plate.Rubber`, `plate.Polyethylene` |
| Carbon fibers, carbon plate (61, 62) | Not registered | `plate.EpoxidFiberReinforced` ([material remap](#material-remap)) |
| Electric Engine (64) | Not registered | `ItemList.Electric_Motor_<tier>` |
| Coil (69) | Not registered | Folded into emitter and motor recipes |
| TGX (60) | Not registered | `Materials.TNT` |
| Quartz Rod (99) | Dropped | Vanilla blaze rods; the Reaction Chamber row is dropped |
| Yellowcake, Enriched Uranium (101, 102) | Not registered | GT `dust.Uranium235` |
| Bio Mass (38) | Dropped | GT `Materials.Biomass` fluid |
| Rifle Bullets Stack (88) | Dropped | Loose bullets in the Assembler |
| Energy Cell, Energy Cell (Empty) (15, 16) | Dropped | Energy weapons are their own batteries ([0010](../../design/0010-material-remap-and-tier-additions.md)) |
| Machine Upgrade (52), ore drill heads (75 to 83) | Dropped | Machines dropped ([0002](../../design/0002-machine-deconfliction.md)) |
| Turret armor (48 to 50, 66) | Area 09 | |
| Glider, wing, backpack (51, 58, 59), Oxygen Mask (90) | Area 05 | |
| Ore Cluster Scanner, Creeper Acid Bucket, debug tools | Not built | [nei-visibility.md](nei-visibility.md) |

## Requirements

Numbering continues from the highest existing ID in this area (GGS-06-33).

- **GGS-06-34.** The sheet has 63 rows, one of them (RC-55) withdrawn: 59 registered rows (67 recipes, counting the 7 magazine refills and 3 camo-net variants) and 3 dynamic tank fills that GT builds itself. Every registered recipe is added from a `sBeforeGTPostload` callback or later. The dev self-check fails the boot if any input resolves to `null`.
- **GGS-06-35.** Every machine recipe's `eut` is the `TierEU.RECIPE_*` constant of the output's [0001](../../design/0001-tier-map.md) tier. No recipe is craftable below its tier.
- **GGS-06-36.** Every machine recipe (RC-61 included) has at least one input of the tier's gate material, except the loose LV ammo `ammo_9mm_bullet` and `ammo_shotgun_shell`, whose gate is the LV machine voltage and the Brass case. The exemption list is fixed in code and in this requirement.
- **GGS-06-37.** Loose ammo, magazine and tank recipes use the output counts, circuit numbers and durations in this sheet. Circuit numbers are unique per (map, inputs).
- **GGS-06-38.** Bullet recipes take slugs and cases from GT `round`, `foil` and `itemCasing`, and powder from `dust.Gunpowder`. GregGuns registers no casing item. Every loose-ammo and magazine recipe is registered to `gregguns.ammoPress` only, not to GT's Assembler, Forming Press or Extruder ([0011](../../design/0011-machine-policy.md)). The Stone Bullet also keeps its shapeless hand recipe.
- **GGS-06-39.** Crafting-table recipes exist only for the ULV rows (RC-01 to RC-03, RC-21, RC-27, RC-63) and the magazine refill (RC-51). Every other output is made on a GT recipe map.
- **GGS-06-40.** `tank_fuel`, `tank_bio` and `tank_air` implement `IFluidContainerItem`, capacity 1,000 mB. A GT Fluid Canner fills an empty tank only from at least 1,000 mB of an accepted fluid.
- **GGS-06-41.** (withdrawn by [0010](../../design/0010-material-remap-and-tier-additions.md)) There is no `cell_energy` item and no recipe produces one.
- **GGS-06-42.** `cell_nuclear` is not an `IElectricItem`. `cell_nuclear_depleted` has no crafting recipe, and RC-60 is its only way back to a full cell.
- **GGS-06-43.** Every LuV and ZPM craft (RC-19, RC-26, RC-35, RC-36, RC-59) is an Assembly Line recipe with both `RESEARCH_ITEM` and `SCANNING` metadata. RC-60 is the stated exception.
- **GGS-06-44.** Dropped items (the last table) have no registry entry, and no recipe in any area names them.
- **GGS-06-45.** Magazine refill and sneak-empty use the bullet counts in the Magazines table. The bulk Assembler recipes use four times those counts.
- **GGS-06-46.** Every kept item made from GT materials carries `ItemData` ([GGS-06-12](components-and-materials.md#requirements)) so GT recycling returns its metals.
- **GGS-06-47.** The registry names in the component index are reserved. Renaming one needs a design record, because areas 02, 05 and 07 to 09 and [pack-loot.md](pack-loot.md) reference them.
- **GGS-06-48.** Recipes in areas 02, 05 and 07 to 09 name GregGuns components only by a registry name from the component index.

## Tests

| ID | Verified by |
|---|---|
| GGS-06-34 | Unit test over the recipe loader with a stubbed unificator returning `null` (expects a logged skip); `runServer` boot check that every recipe is present in its map |
| GGS-06-35 | D1 dev self-check: for each recipe, read back `mEUt` from the map and compare with the tier table |
| GGS-06-36 | The same self-check, with the fixed exemption list |
| GGS-06-37 | Unit test: output counts and durations equal the table; (map, inputs, circuit) is unique |
| GGS-06-38 | Unit test: ammo recipe inputs are `round`, `foil`, `itemCasing` or `dust.Gunpowder` only; the item registry has no `casing` entry; every ammo and magazine output appears in `gregguns.ammoPress` and in no GT map |
| GGS-06-39 | Unit test: the crafting registry's GregGuns outputs equal the listed set |
| GGS-06-40 | Unit test on `fill`; `/gregguns test` scenario that fills each tank through a Fluid Canner |
| GGS-06-41 | (withdrawn) Unit test: the registry has no `cell_energy` entry |
| GGS-06-42 | Unit test: no `IElectricItem`; no recipe outputs `cell_nuclear_depleted` |
| GGS-06-43 | Unit test: the five recipes in the Assembly Line map carry both metadata keys |
| GGS-06-44 | Unit test: registry has no entry from the dropped list; grep check on the recipe packages |
| GGS-06-45 | Unit test comparing the refill table, the bulk recipes and the sneak-empty return counts |
| GGS-06-46 | Unit test, as GGS-06-12 |
| GGS-06-47 | Unit test: the registry contains every name in the component index |
| GGS-06-48 | Unit test scanning other areas' recipes for `gregguns:` names not in the index |

## Open questions

Each needs a decision before this sheet is final.

- **Material remap record (closed).** Settled by [0010](../../design/0010-material-remap-and-tier-additions.md) decisions 1 and 2: tier-material hardware and `EpoxidFiberReinforced` for "carbon".
- **Tier conflicts with 0001 (closed).** [0010](../../design/0010-material-remap-and-tier-additions.md) decision 5 adopts Bandages LV, Cybernetic Parts EV, High Caliber Rounds HV and Advanced Bullets EV.
- **Material remap versus 05 and 09 (closed).** One rule, from [0010](../../design/0010-material-remap-and-tier-additions.md) decision 1: the tier material of the consuming item.
- **Receiver and barrel gating.** The Iron Barrel, Iron Receiver and Steel Receiver are made of iron or steel parts with higher-tier hardware as the gate. If 0001's gate rule must apply to the part itself, they move to the hardware's material.
- **Loose LV ammo has no Steel input.** 9mm bullets and shotgun shells are made from brass and lead. Either 0001's gate rule is relaxed for loose ammo, or those two recipes gain a steel part (GGS-06-36).
- **Brass in the pack.** `foil.Brass` and `itemCasing.Brass` should exist for `Materials.Brass`; confirm in GTNH 2.9.0-RC-2 before M2.
- **Energy Cell (closed).** Dropped by [0010](../../design/0010-material-remap-and-tier-additions.md) decision 4: [02](../02-guns/energy-electric-items.md) makes each energy gun its own battery.
- **LuV and ZPM voltage.** RC-19, RC-26, RC-35, RC-36 and RC-59 use the item's own tier. GT's LuV motor uses one tier less. 02 proposes keeping 0001 as written for weapons ([02 open questions](../02-guns/recipes.md#open-questions)); this sheet follows that proposal. A record is needed only if components must follow GT's one-tier-below precedent.
- **Nuclear refuel route.** RC-60 uses the plain Assembler at ZPM. If the Assembly Line rule must hold for conversions, RC-60 becomes an Assembly Line recipe.
- **Tank capacity (closed).** 1,000 mB, fixed by [0010](../../design/0010-material-remap-and-tier-additions.md) decision 3; [02's fluid-ammo sheet](../02-guns/fluid-ammo.md) restates per-shot consumption against it.
- **Gas Mask Filter progression.** `ActivatedCarbonFilterMesh` may be later than MV in the pack's progression (GGS-06-35).
- **Ammo cost balance.** The cost note under the ammo table: GT rounds make a 9mm bullet about 1.5 times the Techguns cost. Tune batch sizes in M2.
