# Hazard protection (GT hazards API)

- **Techguns baseline:** [special-equipment.md § 3.1 Gas Mask](../../techguns-spec/05-armor/special-equipment.md#31-gas-mask-itemgasmask), [armor-catalogue.md § 5.10 HEV](../../techguns-spec/05-armor/armor-catalogue.md), [set-bonuses.md](../../techguns-spec/05-armor/set-bonuses.md)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0004 damage model](../../design/0004-damage-model.md)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205), GalaxySpace (`GalaxySpace`, 1.1.143-GTNH; no public source), dreamcraft (`GTNewHorizons/HazardousItems.xml`)
- **Roadmap:** M3 (armour and equipment)

## What changes from Techguns

Techguns protects against poison and radiation through its own per-damage-type resistances, and its Gas Mask just removes Poison and Wither effects. GT already has a hazard system for the things that hurt a player in a GTNH base: pollution gas, radioactive items, heat, frost and electric shock. Protection against those is a **GT hazard**, not a Techguns damage type, so GregGuns armour must say so through GT's `IHazardProtector`.

The two systems are separate and both apply. A Techguns-style `RADIATION` damage type (kept by [0004](../../design/0004-damage-model.md)) is combat damage from a GregGuns weapon. GT's `RADIOLOGICAL` hazard is exposure to GT items and machines.

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| Gas Mask is a face item that cures Poison and Wither | Gas Mask is a **helmet** item that protects against the GT `GAS` hazard fully, and still cures Poison and Wither while it has filter charge | GT only reads armour slots (below) |
| No protection against heat, frost, shock | Per-piece hazard flags, table below | GT supplies these hazards |
| Protection needs no power except HEV bonuses | HEV and power armour hazard flags need charge in the chestplate | Matches design 0003 |

## Integration points

### GregTech: `IHazardProtector` and `HazardProtection`

- **Pack config:** none for the API itself. `reference/gtnh/2.9.0-RC-2/.minecraft/config/GregTech/GregTech.cfg` has no hazard keys. `config/GTNewHorizons/dreamcraft.cfg`, `B:HazardousItems` = `true` turns on `HazardousItems.xml`, which lists hazardous *items* (lava buckets, bedrockium plates) with `DamageEffect` and `PotionEffect` entries. It contains no armour entries, so there is nothing for GregGuns armour to register there.
- **Source:**
  - The hazards: `BIOLOGICAL, FROST, HEAT, RADIOLOGICAL, ELECTRICAL, GAS, SPACE`, plus the sets `CBRN_HAZARDS` (biological, gas, radiological), `TEMPERATURE_HAZARDS` and `STANDARD_HAZARDS`: [Hazard.java#L12-L18](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/Hazard.java#L12-L18).
  - The contract: `protectsAgainst(ItemStack, Hazard)` is *partial* protection (counts only when every armour piece has it). `protectsAgainstFully(entity, stack, hazard)` is protection by that piece alone: [IHazardProtector.java#L8-L21](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/IHazardProtector.java#L8-L21).
  - The check reads **only the four vanilla armour slots**: it loops `entity.getEquipmentInSlot(1..4)`, returns true if any piece protects fully, otherwise needs all four to protect: [HazardProtection.java#L44-L67](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/HazardProtection.java#L44-L67). Anything in a bauble slot is invisible to it.
  - An armour piece also counts if it carries GT's Hazmat enchantment: [HazardProtection.java#L95-L102](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/hazards/HazardProtection.java#L95-L102).
  - GT mixes `IHazardProtector` into every `ItemArmor`. By default a piece protects only if its material is `ArmorMaterial.CHAIN` and the hazard is `ELECTRICAL`: [ItemArmorMixin.java#L13-L18](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/mixin/java/gregtech/mixin/mixins/early/minecraft/accessors/ItemArmorMixin.java#L13-L18). So a GregGuns material that is vanilla `CHAIN` would give full electrical protection by accident.
  - GT does the same for IC2: Nano and Quantum armour protect against everything ([MixinIc2Quantum.java#L12-L16](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/mixin/java/gregtech/mixin/mixins/late/ic2/MixinIc2Quantum.java#L12-L16)); IC2 Hazmat protects against `STANDARD_HAZARDS` ([MixinIc2Hazmat.java#L26-L29](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/mixin/java/gregtech/mixin/mixins/late/ic2/MixinIc2Hazmat.java#L26-L29)). These are the precedent for tiered armour.
  - Who reads the hazards:

    | Hazard | Consumer |
    |---|---|
    | `GAS` | pollution effects (weakness, slowness, mining fatigue, nausea and the like): [Pollution.java#L188](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/pollution/Pollution.java#L188), [L217](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/pollution/Pollution.java#L217) |
    | `HEAT`, `FROST`, `ELECTRICAL`, `RADIOLOGICAL` | `GTUtility.applyHeatDamage`, `applyFrostDamage`, `applyElectricityDamage`, `applyRadioactivity`: [GTUtility.java#L1989-L2030](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L1989-L2030) |
    | `SPACE` | no consumer in GT source. GT's tooltip code says it is "handled by GalaxySpace": [GTClient.java#L463-L487](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTClient.java#L463-L487) |

  - GT adds hazard lines to the tooltip of any `IHazardProtector` item itself (same `GTClient` range), so GregGuns writes no hazard tooltip text.
  - Mech Armor (GT's own electric armour) implements the interface through "behaviours": [MechArmorBase.java#L457-L482](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/armor/MechArmorBase.java#L457-L482), with a Hazmat augment that protects fully against everything but `SPACE`: [HazmatBehavior.java](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/armor/behaviors/HazmatBehavior.java).
- **GregGuns does:**
  - Every GregGuns armour class declares `implements IHazardProtector` (a hard dependency, since GT is one) and answers from a small table in code, driven by config.
  - The Gas Mask and Tactical Mask are helmet-slot items returning `protectsAgainstFully` for `GAS`. One mask therefore gives full pollution protection on its own, which is how GT intends "gas mask" to work.
  - All other flags are `protectsAgainst` (partial), so they count toward GT's full-set rule.
  - Powered pieces answer `false` for their power-dependent flags when the chestplate's charge is zero.
  - GregGuns never claims `SPACE`. Space protection is GalaxySpace's, and its mechanism has no public source (see [oxygen-gear.md](oxygen-gear.md)).
  - All GregGuns armour materials are custom `EnumHelper.addArmorMaterial` values, never `CHAIN`.
- **Pack-side change:** none.

### Proposed flag table (starting values, tuned in M3 and M9)

| Set or item | Tier ([0001](../../design/0001-tier-map.md)) | `protectsAgainstFully` | `protectsAgainst` (partial) |
|---|---|---|---|
| T1 Combat, Scout, Miner | LV or ULV | none | none |
| Gas Mask (helmet) | MV | `GAS` | `BIOLOGICAL` |
| Tactical Mask (helmet) | HV ([0010](../../design/0010-material-remap-and-tier-additions.md)) | `GAS` | `BIOLOGICAL` |
| T2 Combat, Beret | MV | none | none |
| T2 Commando | MV | none | `ELECTRICAL` (rubber) |
| T2 Riot (lead plate) | MV | none | `RADIOLOGICAL` |
| T3 Combat | EV | none | none |
| Steam armour | EV | none | `HEAT` |
| T3 HEV (powered) | EV | none | `BIOLOGICAL`, `GAS`, `RADIOLOGICAL`; and `HEAT`, `FROST`, `ELECTRICAL` while the chestplate is charged |
| T3 Power armour, Exo suit (powered) | IV | none | `HEAT`, `FROST`, `ELECTRICAL` while charged |

A full HEV set covers every `STANDARD_HAZARDS` entry, so it counts as a hazmat suit, as the Techguns name suggests. These flags are the **GregGuns proposal** and have no Techguns source; the Techguns spec records resistances, not hazards.

## Requirements

- **GGS-05-08.** Every GregGuns armour item implements `gregtech.api.hazards.IHazardProtector`.
- **GGS-05-09.** The Gas Mask and Tactical Mask are armour items in the helmet slot and return true from `protectsAgainstFully(..., GAS)`.
- **GGS-05-10.** No GregGuns armour material is `ArmorMaterial.CHAIN`. A unit test asserts that no GregGuns piece protects against `ELECTRICAL` unless the flag table says so.
- **GGS-05-11.** Power-dependent hazard flags return false when the chestplate has no charge. Passive flags do not depend on charge.
- **GGS-05-12.** GregGuns armour never returns true for `Hazard.SPACE`.
- **GGS-05-13.** The Gas Mask removes Poison and Wither only while it has filter charge. The filter item is a [06-items](../../techguns-spec/06-items/index.md) consumable (its GT recipe is set in M3).

## Tests

- GGS-05-08, 10, 12: unit test over the armour registry, reading the flag table.
- GGS-05-09, 11: unit test calling `HazardProtection.isWearingFullGasHazmat` with a mock entity wearing the mask; a charged and an uncharged HEV chestplate for `HEAT`.
- GGS-05-13: unit test of the filter counter.
- `/gregguns test` scenario: stand in a polluted chunk with and without the mask (manual check of Weakness and Slowness).

## Open questions

- **Mask in a bauble slot?** A face mask in `head` would keep Techguns' "helmet plus mask" feel, but GT cannot see it. The alternative is that every GregGuns armour piece forwards the hazard query to the worn bauble. That only works if the player wears some GregGuns armour. Rejected here; revisit with the slot-ownership record named in [baubles-slots.md](baubles-slots.md).
- **Are the proposed flags right for the balance of GTNH hazards?** Needs the M9 pass, or a record "Armour hazard flags".
