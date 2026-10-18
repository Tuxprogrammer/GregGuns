# Powered armour and flight

- **Techguns baseline:** [armor-catalogue.md § 9 power system](../../techguns-spec/05-armor/armor-catalogue.md), [special-equipment.md § 4.2 to 4.5](../../techguns-spec/05-armor/special-equipment.md#42-glider-shared-item-and-its-physics-genericitemsharedonplayertickglider) (Glider, Jetpack, Jump Pack, Anti Gravity Device)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0003 energy model](../../design/0003-energy-model.md), [0004 damage model](../../design/0004-damage-model.md)
- **Pack mods:** IndustrialCraft 2 (`IC2`, 2.2.2.828, API source only), GregTech (`gregtech`, 5.09.54.205), GraviSuite (`GraviSuite`, 2.0.3, no source) and Gravitation-Suite-Neo (`gravisuiteneo`, 1.3.16), GalaxySpace (`GalaxySpace`, 1.1.143-GTNH, no public source), AdventureBackpack2 (`adventurebackpack`, 1.4.26-GTNH), Extra Utilities (`ExtraUtilities`, 1.2.12, no source), dreamcraft (`config/GTNewHorizons/CustomToolTips.xml`)
- **Roadmap:** M3 (armour and equipment), M5 (energy items)

## What changes from Techguns

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| Chestplate holds RF (3600, 7200, 10000), refills the other pieces; empty chest eats a battery item | Every powered piece is an IC2 electric item at its [0001](../../design/0001-tier-map.md) tier. The chestplate holds the charge. Charging is by GT charger or battery buffer, never by consuming a battery item | [0003](../../design/0003-energy-model.md): EU only, no RF |
| Jetpack, Jump Pack, Anti Gravity Device use durability as fuel and reload from items | They are electric items in a `wings` bauble ([baubles-slots.md](baubles-slots.md)). Fuel-burning versions (Steam armour, flamethrower) keep GT fluids | [0003](../../design/0003-energy-model.md) |
| Flight and fall cancel are the item's own logic, with no thought for other mods | Flight is motion-based and never touches player abilities. Fall cancel is limited to what the item's own energy pays for | See "Flight stacking" |
| Protection works with no power, bonuses need power | Unchanged | Keeps the Techguns feel |

## Integration points

### IC2 and Forge: the electric item and special armour contracts

- **Pack config:** none for the contract. For how the pack shows charger tiers, `config/GTNewHorizons/CustomToolTips.xml` has rows such as `<ToolTip ItemName="GraviSuite:advJetpack" ToolTip="customtooltip.recharge_tier_machine" FormatArgs="§eHV" .../>` and the same for `IC2:itemArmorQuantumBoots` (`§8EV`).
- **Source:**
  - `IElectricItem` has `canProvideEnergy`, `getChargedItem`, `getEmptyItem`, `getMaxCharge` (double), `getTier` (int), `getTransferLimit` (double). IC2 publishes only its API as source: `reference/maven/Industrial Craft 2/industrialcraft-2-2.2.828-experimental-api.jar`, `ic2/api/item/IElectricItem.java` and `ISpecialElectricItem.java`. This is API-source evidence, not a tag permalink.
  - The pack precedent is GT's own armour. `MechArmorBase` extends `ItemArmor` and implements `ISpecialArmor`, `ISpecialElectricItem` and `IHazardProtector`: [MechArmorBase.java#L70-L72](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/armor/MechArmorBase.java#L70-L72).
  - Its electric methods: `getTier` returns a `VoltageIndex` value (LV = 1, MV = 2 and so on) and `getTransferLimit` returns `GTValues.V[tier]`: [MechArmorBase.java#L484-L519](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/armor/MechArmorBase.java#L484-L519). [VoltageIndex.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/VoltageIndex.java#L5-L7) gives the numbers.
  - Its `ISpecialArmor` implementation reduces absorption to 0 for unblockable damage and drains energy in `damageArmor`: [MechArmorBase.java#L384-L410](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/armor/MechArmorBase.java#L384-L410).
  - GT's older class `ItemEnergyArmor` carries the same interfaces but nothing in the GT5-Unofficial tree constructs it, so it is not a live precedent.
- **GregGuns does:**
  - Powered armour and packs implement `IElectricItem` with `getTier` = the item's `VoltageIndex` and `getTransferLimit` = `GTValues.V[tier]`. The charge is stored by IC2's item manager (the standard NBT), not by custom NBT, so GT chargers and buffers see it.
  - `getMaxCharge` is a config option per item. The baseline is a GT battery of the same tier ([0003](../../design/0003-energy-model.md)). For scale, GT's Mech Armor cores hold 1,000,000 EU at HV and 10,000,000 EU at IV ([MechArmorAugmentRegistries.java#L285-L308](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/armor/MechArmorAugmentRegistries.java#L285-L308)).
  - Armour implements Forge `ISpecialArmor` with the [0004](../../design/0004-damage-model.md) formulas. Only the chestplate drains charge for hits; `damageArmor` never breaks a piece (Techguns: at 1 durability left a piece gives nothing).
  - Bauble-slot devices are charged by taking them off and putting them in a GT charger. GregGuns does not rely on any in-slot charging.
- **Pack-side change:** add `CustomToolTips.xml` rows with `customtooltip.recharge_tier_machine` for each powered GregGuns item, with the tier colour and name as in the existing rows. This is cosmetic. GregGuns also adds the tier line itself, so the pack row is optional.

### GT Mech Armor: the overlap

GT already ships configurable electric armour with a **Jetpack** augment, a **Vectored Jetpack**, **Night Vision**, **Hazmat**, **Fall Protection**, **Space Suit**, **Step Assist**, **Jump Boost**, **Speed Boost** and **Creative Flight** (ZPM-class). Cores: Nano (tier 1, HV), Quantum (tier 2, IV), Living (tier 3, ZPM), Singularity (tier 4, infinite).

- **Source:** [MechArmorAugmentRegistries.java#L409-L460](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/armor/MechArmorAugmentRegistries.java#L409-L460) (Hazmat, Fall Protection, Space Suit, Water Breathing, Jetpack, all with minimum core tier 1); [Night Vision, L497-L505](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/armor/MechArmorAugmentRegistries.java#L497-L505); [Vectored Jetpack, L558-L568](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/armor/MechArmorAugmentRegistries.java#L558-L568); [Creative Flight, L652-L664](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/armor/MechArmorAugmentRegistries.java#L652-L664).
- **Consequence:** a GregGuns Jetpack at EV arrives *after* GT's jetpack at HV. [CONTRIBUTING § GTNH integration](../../../CONTRIBUTING.md#gtnh-integration) says never to duplicate what GT provides. The Mech Armor augment enums are closed, so GregGuns cannot add an augment to them; any GregGuns flight is a separate item. See Open questions.
- **GregGuns does:** treats Mech Armor as a coexisting system. It registers no augment, and takes no dependency on Mech Armor classes.

### Flight stacking: what else in the pack can move a player upward

| Source | Mechanism | Evidence |
|---|---|---|
| GT Jetpack and Vectored Jetpack | Sets `motionY` (capped with `Math.min`), calls `moveFlying`, zeroes `fallDistance` on the server. Drains 20 charge units per active tick | [JetpackBehavior.java#L56-L114](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/armor/behaviors/JetpackBehavior.java#L56-L114) |
| GT Creative Flight augment | Sets `capabilities.allowFlying` and `isFlying`, clears them when unpowered or unequipped (unless the player is creative), cancels all fall damage while powered | [CreativeFlightBehavior.java#L22-L55](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/armor/behaviors/CreativeFlightBehavior.java#L22-L55), [ArmorEventHandlers.java#L161-L195](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/armor/ArmorEventHandlers.java#L161-L195) |
| GT MagLev Harness (bauble `belt` or `wings`) | `capabilities.allowFlying` near a pylon; clears it on unequip | [ItemMagLevHarness.java#L96-L110](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/ItemMagLevHarness.java#L96-L110) |
| AdventureBackpack Coal Jetpack | Own back slot (not Baubles). Adds `motionY += 0.1` up to a cutoff speed that falls to nothing between y 135 and y 185, reduces `fallDistance` | [ItemCoalJetpack.java#L104-L168](https://github.com/GTNewHorizons/AdventureBackpack2/blob/1.4.26-GTNH/src/main/java/com/darkona/adventurebackpack/item/ItemCoalJetpack.java#L104-L168) |
| Extra Utilities Angel Ring | A baubles ring. `config/ExtraUtilities.cfg`, `AngelRingEnabled` = `true`. No source; the mechanism is not verified here | pack config only |
| GraviSuite and Gravitation-Suite-Neo | Electric chestplate and jetpack. `config/GraviSuite.cfg` keeps `hoverModeFallSpeed` = `0.0` and sets `advjetpack` to tier 3 and `gravichestplate` to tier 4, with most recipes disabled (supplied by the pack). The old mod has no source; Neo is a mixin layer. Mechanism not verified | pack config only |
| IC2 jetpacks and Quantum armour | IC2 internal, no source. Not verified | none |
| GalaxySpace JetPack | Disabled: `config/GTNewHorizons/CustomToolTips.xml` marks `GalaxySpace:item.JetPack` and `:100` with `customtooltip.disabled_item`, whose text in `config/txloader/load/customtooltips/lang/en_US.lang` is "Disabled Item; No recipe". The pack does not say why | pack config only |

The pack's pattern is clear. Two GT systems write the player's abilities (`allowFlying`), and each of them clears the flag when it unequips. Motion-based systems stack by addition, and each pays its own energy or fuel. The pack removes duplicate jetpacks (the GalaxySpace one) rather than balancing them.

- **GregGuns does:**
  - GregGuns flight (Jetpack, if kept; Jump Pack; Anti Gravity Device; Glider) is **motion-based**. It never writes `capabilities.allowFlying` or `isFlying`. That way GT's and the Harness's "clear on unequip" code cannot cancel GregGuns flight, and GregGuns cannot cancel an Angel Ring.
  - Each GregGuns device caps only its own contribution, with `Math.min` on `motionY` as GT does. It does not read or limit another mod's thrust.
  - Fall damage: a GregGuns handler on `LivingFallEvent` (priority `LOW`, like GT's) reduces the fall distance by the amount its own gear pays for in energy. It never calls `setCanceled(true)` for fall damage it did not pay for.
  - The Anti Gravity Device and the Jetpack are in the same `wings` bauble slot, so a player can have only one GregGuns back device at once. The Glider (no energy, no thrust) shares the slot too.
  - The remaining exploit class would be free thrust. None exists: every active tick drains the device's own charge, and GregGuns never feeds energy into another mod's item.
- **Pack-side change:** none required. Optional: if the pack decides GregGuns's Jetpack duplicates GT's, add `customtooltip.disabled_item` and a recipe removal for it, as for the GalaxySpace JetPack.

## Requirements

- **GGS-05-14.** Every powered GregGuns item implements `ic2.api.item.IElectricItem`, with `getTier` equal to the item's [0001](../../design/0001-tier-map.md) `VoltageIndex` and `getTransferLimit` equal to `GTValues.V[tier]`.
- **GGS-05-15.** Capacity, EU per tick and EU per hit are config values per item, with a GT-battery baseline.
- **GGS-05-16.** GregGuns armour implements Forge `ISpecialArmor`. A piece never breaks; protection does not depend on charge.
- **GGS-05-17.** No GregGuns code writes `EntityPlayer.capabilities.allowFlying` or `isFlying`.
- **GGS-05-18.** Thrust is applied once per player tick and clamped by that item's own maximum speed. Energy is drained on the server only.
- **GGS-05-19.** The `LivingFallEvent` handler reduces or cancels fall damage only for distance paid for by GregGuns energy, and never for a player whose GregGuns device is empty.
- **GGS-05-20.** At most one GregGuns back device can be worn, enforced by the `wings` slot.
- **GGS-05-21.** Powered armour shows its charger tier in the tooltip.

## Tests

- GGS-05-14, 15: unit test over the registry.
- GGS-05-16, 21: unit test on `ISpecialArmor.getProperties` and the tooltip.
- GGS-05-17: unit test using a mock player with `allowFlying = true` from another source: after a GregGuns tick the flag is unchanged.
- GGS-05-18, 19, 20: `/gregguns test` scenario with a GregGuns device worn and, in a second run, GT's Jetpack augment on the chestplate; verify energy drain from each and fall damage with an empty device.

## Open questions

- **Keep the GregGuns Jetpack?** [0001](../../design/0001-tier-map.md) places it at EV, but GT's Jetpack augment is available at HV with a Nano core, and the pack disables the GalaxySpace jetpack. Recommendation: drop the Jetpack and keep the Jump Pack, Glider and Anti Gravity Device, which have no GT equivalent. This contradicts an accepted record, so it needs a new record: "Jetpack versus GT Mech Armor".
- **Anti Gravity Device versus GT Creative Flight.** Creative Flight is ZPM-class. The Anti Gravity Device at IV is earlier. Is it hover only, with no free flight? Needs a number in M5.
- **Charging worn bauble devices.** Is "take it off to charge" acceptable, or should GT chargers be able to charge a bauble inventory? This needs a source check of GT's charging code before a record.
