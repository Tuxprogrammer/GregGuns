# Hazards, tooltips and fuels

How the pack's hazard, tooltip and fuel data files relate to GregGuns items, and how GregGuns items become radioactive through GT.

- **Techguns baseline:** [special-items.md](../../techguns-spec/06-items/special-items.md) (tooltips), [lang.md](../../techguns-spec/06-items/lang.md), [ammo.md](../../techguns-spec/06-items/ammo.md#tanks-cells-and-other-refillables) (Nuclear Power Cell)
- **Design records:** [0004 damage model](../../design/0004-damage-model.md) (radiation damage type kept), [0001 tier map](../../design/0001-tier-map.md) (ZPM nuclear items)
- **Pack mods:**
  - NewHorizonsCoreMod (`dreamcraft`, 2.9.84): modules `HazardousItems`, `CustomToolTips`, `CustomFuels`
  - GregTech (`gregtech`, 5.09.54.205): hazards and radioactivity
- **Roadmap:** M2 (item registration), M3 (hazmat armour, area 05)

## What changes from Techguns

Techguns' nuclear items carry no world hazard: the Nuclear Power Cell is a plain item. In GTNH, radioactive materials irradiate their holder. GregGuns hooks into that system so the Nuclear Power Cell, the Small Nuclear Warhead and the Rad Emitter are dangerous to carry without protection. This is the only change that touches player-visible behaviour. The tooltip and fuel files need no entries.

| File | Verdict | Reason |
|---|---|---|
| `HazardousItems.xml` | **No entry** | It cannot express radiation (see below). Radiation comes from GT |
| `CustomToolTips.xml` | **No entry** | GregGuns items carry their own lang tooltips |
| `CustomFuels.xml` | **No integration point** | GregGuns registers no burn times (Techguns registers none either, [index](../../techguns-spec/06-items/index.md#key-facts)) |
| `CustomDrops.xml` | See [pack-loot.md](pack-loot.md#gtnh-customdrops) | Drops, not tooltips |

## Integration points

### dreamcraft HazardousItems

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GTNewHorizons/HazardousItems.xml`, enabled by `B:HazardousItems=true` in `dreamcraft.cfg`. Entries are `<Item ItemName ExactNameMatch OnContactCheck InventoryCheck>` or `<Fluid FluidName ...>` with child `<DamageEffect Source Amount/>` and `<PotionEffect PotionID TickDuration Level/>`. As shipped it covers lava buckets and cans (`Source="gregtech:hot"`), Bedrockium plates, marshmallows and the pollution bucket and fluid.
- **Source:** [`HazardDamageSources`](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modhazardousitems/HazardDamageSources.java#L19-L50): the valid `Source` values are the vanilla ids `inFire`, `onFire`, `lava`, `inWall`, `drown`, `starve`, `cactus`, `fall`, `outOfWorld`, `generic`, `magic`, `wither`, `anvil`, `fallingBlock`, plus the single extra `gregtech:hot`. There is no radiation source. Any other id throws "Unknown damage source id" when the file loads.
- **GregGuns does:** nothing. Radiation is not available through this file, so [GT radiation](#gt-radiation-for-nuclear-items) is used instead.
- **Pack-side change:** none. If a GregGuns item should poison (potion effect) or burn (`gregtech:hot`) its holder, the pack could add an `<Item ItemName="gregguns:...">` entry, but no GregGuns item needs it.

### GT radiation for nuclear items

- **Pack config:** none.
- **Source:**
  - [`GTProxy.applyItemEffects`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTProxy.java#L1306-L1311) runs on every stack in the player's main inventory and armour slots (L1295-L1304) and skips creative players. It calls `GTUtility.applyRadioactivity(player, getRadioactivityLevel(stack), stack.stackSize)`.
  - [`GTUtility.getRadioactivityLevel`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1971-L1980) returns the level from the stack's `ItemData` material (its armour or tool enchantment, when that enchantment is `EnchantmentRadioactivity`), or, failing that, the level of the Radioactivity enchantment on the stack itself.
  - [`GTUtility.applyRadioactivity`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L2018-L2022) applies slowness, mining fatigue and other effects scaled by level and stack size. It skips undead and arthropods, and skips anyone for whom `HazardProtection.isWearingFullRadioHazmat` is true.
  - `Uranium235` is level 2 in both slots: [`MaterialsInit.loadUranium235`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L2090-L2108).
  - [`GTOreDictUnificator.addItemData`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L350-L359) attaches composition data to a stack.
  - The protection side: [`Hazard`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/Hazard.java#L10-L18) (`BIOLOGICAL`, `FROST`, `HEAT`, `RADIOLOGICAL`, `ELECTRICAL`, `GAS`, `SPACE`) and [`IHazardProtector`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/IHazardProtector.java#L8-L12). The class comment says other mods may add their own hazmat gear that interacts with these checks. That side belongs to area 05.
- **GregGuns does:**
  - Gives the Nuclear Power Cell (full and depleted), the Small Nuclear Warhead, the tactical nuke rocket and the Rad Emitter `ItemData` whose material is `Materials.Uranium235` with a small amount, for example `new ItemData(Materials.Uranium235, OrePrefixes.dust.getMaterialAmount())`. GT then irradiates the holder at level 2, scaled by stack size.
  - Marks the depleted cell the same way (it is still radioactive).
  - The `ItemData` also feeds GT recycling, so a recycler returns uranium dust. If that is unwanted, use the Radioactivity enchantment on the stack instead (`EnchantmentRadioactivity.INSTANCE`, [file](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enchants/EnchantmentRadioactivity.java#L13-L15)), which does not touch recycling but shows an enchantment line.
  - Area 05 makes the hazmat and nuclear-capable armour implement `IHazardProtector` for `RADIOLOGICAL`.
- **Pack-side change:** none.

### dreamcraft CustomToolTips

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GTNewHorizons/CustomToolTips.xml`, enabled by `B:CustomToolTips=true`. Lines are `<ToolTip ItemName ToolTip FormatArgs NBT/>` where `ToolTip` is a lang key. Example: `GalaxySpace:item.JetPack` and `GalaxySpace:item.JetPack:100` carry `customtooltip.disabled_item`.
- **Source:** [`CustomToolTipsHandler.onToolTip`](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modctt/CustomToolTipsHandler.java#L154-L172) translates the key with `StatCollector` and appends each `\n`-separated line to the tooltip.
- **GregGuns does:** nothing. Its own items use their own lang keys. The file matters in one case: the pack marks another mod's jetpack as disabled, which tells area 05 the pack has a stance on jetpacks. The pack also loots the Adventure Backpack Coal Jetpack and Copter Pack in `chest4` ([pack-loot.md](pack-loot.md#tier-bands)).
- **Pack-side change:** none.

### dreamcraft CustomFuels

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GTNewHorizons/CustomFuels.xml`, enabled by `B:CustomFuels=true`. Entries are `<FuelItem ItemName BurnTime/>`; it lists vanilla diamonds, the pack's compressed coals and similar.
- **Source:** [`CustomFuelsHandler`](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modcustomfuels/CustomFuelsHandler.java#L19-L111) implements `IFuelHandler.getBurnTime`.
- **GregGuns does:** nothing. GregGuns has no solid-fuel items. Flamethrower and chainsaw fuel is a GT fluid ([fluids.md](fluids.md)), not a furnace fuel.
- **Pack-side change:** none.

## Requirements

- **GGS-06-23.** The Nuclear Power Cell (full and depleted), the Small Nuclear Warhead, the tactical nuke rocket and the Rad Emitter are radioactive through GT: `GTUtility.getRadioactivityLevel(stack)` returns a value above 0 for each.
- **GGS-06-24.** No GregGuns item other than those in GGS-06-23 is radioactive. Dangerous `ItemData` materials (radioactive or hot) are never attached to other items by accident.
- **GGS-06-25.** GregGuns writes no `HazardousItems.xml`, `CustomToolTips.xml` or `CustomFuels.xml` entry, and registers no burn time.

## Tests

| ID | Verified by |
|---|---|
| GGS-06-23 | Unit test calling `GTUtility.getRadioactivityLevel` on each stack |
| GGS-06-24 | Unit test: iterate GregGuns items and assert level 0 for all not in the list |
| GGS-06-25 | Code review (nothing to ship) |
| Manual | Carry a Nuclear Power Cell in survival: slowness and mining fatigue; with full radiation hazmat: none |

## Open questions

- **Radiation strength.** Level 2 comes from `Uranium235`. Whether a single cell should hit as hard as a uranium dust stack is a balance choice for M2, adjustable by the amount in `ItemData`.
- **Recycling side effect.** Attaching uranium `ItemData` makes the cell recyclable into uranium. Decide whether to use the enchantment route instead.
