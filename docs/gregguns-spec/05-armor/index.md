# 05 Armor and equipment: GregGuns spec

## Overview

This area records how GregGuns armor, special-slot equipment and the Techguns inventory tab plug into the GTNH 2.9.0-RC-2 pack. The Techguns baseline is [techguns-spec/05-armor](../../techguns-spec/05-armor/index.md). Evidence rules are in [INTEGRATION.md](../INTEGRATION.md).

The main findings:

- The pack's slot system is **Baubles Expanded**, not TravellersGear. The TravellersGear config files in the pack are stale: there is no jar for it.
- GT's **hazard API** (`IHazardProtector`) reads only the four vanilla armor slots. Gas protection from a face item in a bauble slot would be invisible to GT.
- Galacticraft's oxygen check reads its own slots and worn armor. Its tanks cannot be replaced by an armor interface, so GregGuns tanks and mask must subclass Galacticraft's item classes.
- GT already ships electric **Mech Armor** with a jetpack, night vision, hazmat and fall-protection augments. The GregGuns Jetpack duplicates it.

## Contents

| File | Covers |
|---|---|
| [baubles-slots.md](baubles-slots.md) | Replacing the 12-slot Techguns tab with Baubles Expanded slots; night vision goggles |
| [hazard-protection.md](hazard-protection.md) | Gas mask, hazmat flags and the GT hazard API |
| [powered-flight.md](powered-flight.md) | IC2 electric armor, GT Mech Armor overlap, flight stacking with other pack mods |
| [oxygen-gear.md](oxygen-gear.md) | Oxygen mask and tanks in Galacticraft dimensions |
| [recipes.md](recipes.md) | Every armor and equipment recipe on GT recipe maps (absorbs the old [armor-recipes.md](armor-recipes.md), now a pointer) |
| [implementation-notes.md](implementation-notes.md) | M3 waves A to C: armor framework, T1, T2, T3 and steam sets, powered armor, bonus lifecycle, deviations and open questions |
| [m3-exit-gate.md](m3-exit-gate.md) | Beta-gate completeness audit of the M3 exit gate: evidence, status and the client checks still owed |

**No integration point, so no sheet.** See the Techguns spec and the [design records](../../design/README.md):

- Armor stats, damage-type values, protection math and camo: [armor-catalog.md](../../techguns-spec/05-armor/armor-catalog.md), with [0004](../../design/0004-damage-model.md) for the fixes ([ARM-02](../../techguns-spec/05-armor/index.md#suspected-bugs)).
- Set bonuses ([set-bonuses.md](../../techguns-spec/05-armor/set-bonuses.md)), models, textures, lang strings and sounds.
- Akimbo off-hand renderers: belong to the gun areas.
- Thaumcraft goggles and runic shielding: not worth it. Techguns armor has no Thaumcraft behavior, and GT's Mech Armor already implements `IGoggles` and `IVisDiscountGear` ([MechArmorBase.java#L70-L72](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/armor/MechArmorBase.java#L70-L72)).
- TravellersGear: not in the pack (see [baubles-slots.md](baubles-slots.md)).

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| 12-slot extra inventory tab | Dropped; typed Baubles slots | Baubles Expanded 2.2.25-GTNH | [baubles-slots.md](baubles-slots.md) |
| Face and back DataWatcher sync | Baubles tracking sync | Baubles Expanded | [baubles-slots.md](baubles-slots.md) |
| Back items (jetpack, jump pack, glider, anti-grav) | One `wings` bauble slot | Baubles Expanded, `config/Baubles.cfg` | [baubles-slots.md](baubles-slots.md) |
| Ammo slots (6) | Ammo Pouch item in `quiver` | Baubles Expanded | [baubles-slots.md](baubles-slots.md) |
| Night Vision Goggles | `head` bauble, ambient potion effect | Baubles Expanded; GT Mech Armor precedent | [baubles-slots.md](baubles-slots.md) |
| Gas Mask and hazmat resistances | Helmet-slot items implementing `IHazardProtector` | GregTech hazards API | [hazard-protection.md](hazard-protection.md) |
| RF-powered armor and packs | IC2 electric items at GT tiers, `ISpecialArmor` | IC2 API, GregTech Mech Armor as precedent | [powered-flight.md](powered-flight.md) |
| Jetpack, jump pack, anti-grav flight | Motion-based, never writes player abilities | GT Mech Armor, MagLev Harness, AdventureBackpack, Angel Ring, GraviSuite | [powered-flight.md](powered-flight.md) |
| Oxygen mask, oxygen tanks, `OXYGEN_GEAR` | Subclass GC item classes; `IBreathableArmor` for gear | Galacticraft 3.4.34-GTNH | [oxygen-gear.md](oxygen-gear.md) |
| Crafting-table armor recipes | GT Assembler at tier voltage; hand-made only for the Steam/ULV sets | GregTech recipe maps | [recipes.md](recipes.md) |
| Charging Station, battery items | Dropped | GT chargers ([0002](../../design/0002-machine-deconfliction.md)) | [powered-flight.md](powered-flight.md) |

## Pack-side changes

| File | Change | Why | Sheet |
|---|---|---|---|
| `config/GTNewHorizons/CustomToolTips.xml` | Optional: add a `customtooltip.recharge_tier_machine` row per powered GregGuns item, with the tier color and name | Matches how the pack shows charger tiers for GraviSuite and IC2 armor. GregGuns also adds the tier line itself | [powered-flight.md](powered-flight.md) |
| `config/Baubles.cfg` | Conditional: add `gregguns_back` and `gregguns_face` to `slotTypeOverrides` only if the slot-ownership record chooses dedicated slots. Not needed for the default mapping (`head`, `wings`, `quiver` already exist) | `manualSlotSelection` is on, so an unlisted type gets no slot | [baubles-slots.md](baubles-slots.md) |

## Related areas

- [01-core](../../techguns-spec/01-core/index.md): extended player properties, keybinds, HUD toggles. The equipment tab and its DataWatcher entries go away.
- [06-items](../../techguns-spec/06-items/index.md): gas-mask filters, compressed-air tanks, energy items. Their GT recipes are set with the recipes sheet.
- [08-machines-production](../../techguns-spec/08-machines-production/index.md) and [09-machines-utility](../../techguns-spec/09-machines-utility/index.md): Repair Bench and Camo Bench keep armor repair and camo.
- [11-worldgen](../11-worldgen/index.md): loot tables that hold armor.

## Open questions

Each needs a new design record.

1. **Jetpack versus GT Mech Armor.** Keep the GregGuns Jetpack ([0001](../../design/0001-tier-map.md) lists it at EV) or drop it for GT's jetpack augment (available at HV)? Recommendation: drop it. See [powered-flight.md](powered-flight.md).
2. **Equipment slot ownership.** Reuse `head`, `wings` and `quiver`, or request dedicated slots from the pack? See [baubles-slots.md](baubles-slots.md).
3. **Masks in a bauble slot.** Rejected for protective masks here because GT and Galacticraft cannot see baubles. Revisit only if the helmet-plus-mask feel matters more than clean integration.
4. **Auto-feeder food slots.** Keep inside the Ammo Pouch or drop?
5. **Armor hazard flags and balance.** The table in [hazard-protection.md](hazard-protection.md) is a proposal.
6. **GalaxySpace spacesuit behavior** is unverified because its source is not readable. Needs a source or a decompile.
7. **Tiers for the Beret, HEV and Tactical Mask (closed by [0010](../../design/0010-material-remap-and-tier-additions.md): MV, EV, HV).** [recipes.md](recipes.md) uses MV, EV and HV because [0001](../../design/0001-tier-map.md) lists none of them. The hazard flag table in [hazard-protection.md](hazard-protection.md) lists the Tactical Mask at MV, which its HV Oxygen Mask input rules out.
8. **Jump Pack power source.** Electric ([powered-flight.md](powered-flight.md)) or compressed air (06 fluids)? The recipe carries both until a record picks one.
