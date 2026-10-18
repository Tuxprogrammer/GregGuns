# Oxygen mask and tanks (Galacticraft)

- **Techguns baseline:** [special-equipment.md § 3.4 Oxygen Mask](../../techguns-spec/05-armor/special-equipment.md#34-oxygen-mask-shared-item), [§ 4.1 Oxygen Tanks](../../techguns-spec/05-armor/special-equipment.md#41-oxygen-tanks-itemscubatanks), [set-bonuses.md](../../techguns-spec/05-armor/set-bonuses.md) (`OXYGEN_GEAR`)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md) (oxygen mask and tanks at HV)
- **Pack mods:** Galacticraft (`GalacticraftCore`, 3.4.34-GTNH), GalaxySpace (`GalaxySpace`, 1.1.143-GTNH, no public source), GregTech (`gregtech`, 5.09.54.205)
- **Roadmap:** M3 (armour and equipment)

## What changes from Techguns

Techguns oxygen gear only tops up the player's vanilla air meter: the tanks add 20 air and cost 1 damage when air is 280 or less, and only if some other worn item gives `OXYGEN_GEAR`. It has no idea what a breathable atmosphere is. In the GTNH pack, space dimensions run on Galacticraft's oxygen system, where a player with no valid setup suffocates, however full the vanilla air meter is. So GregGuns oxygen gear must plug into Galacticraft, or it is useless in space and a duplicate on Earth.

| Techguns | GregGuns | Why |
|---|---|---|
| Oxygen Mask (face, provides `OXYGEN_GEAR`) | A subclass of Galacticraft's `ItemOxygenMask`, worn in GC's mask slot | GC accepts only its own item classes in its slots |
| Oxygen Tanks (back, 600 durability, reload from compressed air) | A subclass of Galacticraft's `ItemOxygenTank`, worn in GC's tank slots; GC drains it | GC reads its own tank slots for air |
| `OXYGEN_GEAR` from T2 Commando helmet, HEV helmet, Tactical Mask | These pieces implement GC's `IBreathableArmor` for the *gear* and *helmet* components | GC reads worn armour through that interface |
| Compressed-air reload by item swap | Refilled with GregGuns's compressed-air item ([0001](../../design/0001-tier-map.md) lists it at HV). Whether GC's own refill machines accept the subclass is expected but not verified | Keep the Techguns loop; GC machines are a bonus |
| Underwater breathing from the same gear | Kept: GC grants water breathing from a valid setup with air in a tank | GC already does it |

## Integration points

### Galacticraft: oxygen setup validity

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/Galacticraft/core.conf`: `I:"Suffocation Cooldown"` = `100`, `I:"Suffocation Damage"` = `10`. Nothing in the file lists oxygen items, so GregGuns need not edit it.
- **Source:**
  - A setup is valid when four components are present: mask (slot 0), gear (slot 1), and at least one of the tanks (slots 2 and 3). Each can be satisfied by an item in GC's own extended inventory, or by a worn armour piece that implements `IBreathableArmor` and handles that component: [OxygenUtil.java#L300-L373](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/util/OxygenUtil.java#L300-L373).
  - The contract: `handleGearType(EnumGearType)` and `canBreathe(stack, player, type)`, with `HELMET`, `GEAR`, `TANK1`, `TANK2`: [IBreathableArmor.java#L9-L33](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/api/item/IBreathableArmor.java#L9-L33). It scans `player.inventory.armorInventory` only; a bauble is not seen.
  - GC's slots accept items by **class**: mask slot `instanceof ItemOxygenMask`, gear slot `ItemOxygenGear`, tank slots `ItemOxygenTank`: [OxygenUtil.java#L375-L385](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/util/OxygenUtil.java#L375-L385).
  - **`IBreathableArmor` cannot replace tanks.** Even when it satisfies the tank component, GC sets `oxygenSetupValid = hasValidOxygenSetup(player) && !airEmpty`, and `airEmpty` is computed from GC's own tank slots only: [GCPlayerHandler.java#L721-L726](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/entities/player/GCPlayerHandler.java#L721-L726). The only bypass is a tank with `getMaxDamage() == Integer.MAX_VALUE`: [L664](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/entities/player/GCPlayerHandler.java#L664). So Oxygen Tanks must be real `ItemOxygenTank` items.
  - GC drains a tank by `damageItem(1)` once per 9 ticks while the player is outside a breathable block: [GCPlayerHandler.java#L679-L690](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/entities/player/GCPlayerHandler.java#L679-L690) and [OxygenUtil.java#L287-L298](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/util/OxygenUtil.java#L287-L298). A tank's durability is its charge, which matches how Techguns stores tank charge.
  - `ItemOxygenTank(int tier, String assetName)` sets stack size 1, `setMaxDamage` from the tier (tier 1 = 1000, doubling per tier), and no-repair: [ItemOxygenTank.java#L19-L27](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/items/ItemOxygenTank.java#L19-L27). A subclass can call `setMaxDamage` again from config.
  - Suffocation fires a cancelable `GCCoreOxygenSuffocationEvent.Pre` before damage: [GCPlayerHandler.java#L732-L734](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/core/entities/player/GCPlayerHandler.java#L732-L734), [GCCoreOxygenSuffocationEvent.java#L29-L30](https://github.com/GTNewHorizons/Galacticraft/blob/3.4.34-GTNH/src/main/java/micdoodle8/mods/galacticraft/api/event/oxygen/GCCoreOxygenSuffocationEvent.java#L29-L30).
- **GregGuns does:**
  - Galacticraft is a soft dependency (`Loader.isModLoaded("GalacticraftCore")`). The two item classes that extend GC classes live in a separate package loaded only when GC is present.
  - **With GC:** `OxygenMaskGC extends ItemOxygenMask` and `OxygenTanksGC extends ItemOxygenTank`, registered under the GregGuns names. GC itself does the air accounting, the HUD and the suffocation rule.
  - **Without GC (dev runs):** plain items with the same names. They only restore the vanilla air meter, like Techguns, and are marked as a fallback in the code.
  - T2 Commando Headgear, the HEV helmet, the power-armour helmet and the Tactical Mask implement `IBreathableArmor`: `handleGearType` returns true for `HELMET` and `GEAR`, and `canBreathe` is true while the piece is not empty or unpowered. They never claim `TANK1` or `TANK2`.
  - The GregGuns oxygen tank is a tier-matched `ItemOxygenTank`: the base capacity uses GC's tier-2 value (2000) and is a config option.
- **Pack-side change:** none.

### Alternatives checked and rejected

- **Cancel `GCCoreOxygenSuffocationEvent.Pre` when GregGuns's own setup is valid.** It works, but GC's HUD and `oxygenSetupValid` flag would show the player as suffocating while taking no damage. Held as a fallback if the subclass approach fails in testing.
- **A bauble mask or tank.** GC's validity check cannot see baubles.

### GalaxySpace and the Space hazard

GalaxySpace's jar contains spacesuit classes (`galaxyspace/core/item/armor/ItemSpacesuit`, `ItemSpacesuitHelmetGlasses`, `ItemSpacesuitJetPlate`, `galaxyspace/core/handler/GSSpaceSuitHandler`). The repository `GTNewHorizons/Galaxy-Space-GTNH` is not readable at tag `1.1.143-GTNH` (the GitHub API returns 404), so how its dimensions judge a spacesuit, thermal or pressure protection included, is **not verified**. GT's tooltip code says the `SPACE` hazard is "handled by GalaxySpace" ([GTClient.java#L463-L487](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTClient.java#L463-L487)).

- **GregGuns does:** promises only what the Galacticraft oxygen check needs, and never claims `Hazard.SPACE` ([hazard-protection.md](hazard-protection.md)). Wherever GalaxySpace needs a full spacesuit, a GregGuns kit is not a substitute. The Oxygen Mask tooltip says so.
- **Pack-side change:** none.

## Requirements

- **GGS-05-22.** When Galacticraft is loaded, the Oxygen Mask is an `ItemOxygenMask` subclass and the Oxygen Tanks are an `ItemOxygenTank` subclass.
- **GGS-05-23.** GregGuns oxygen classes that reference GC types are loaded only if `GalacticraftCore` is present. Server start without GC must not throw `NoClassDefFoundError`.
- **GGS-05-24.** The tank capacity (durability) is a config option, defaulting to GC's tier-2 value.
- **GGS-05-25.** T2 Commando, HEV, power-armour helmets and the Tactical Mask implement `IBreathableArmor` for `HELMET` and `GEAR` only.
- **GGS-05-26.** No GregGuns item claims `TANK1` or `TANK2` through `IBreathableArmor`.
- **GGS-05-27.** GregGuns does not edit `GCPlayerStats`, GC's extended inventory or GC's config at runtime.
- **GGS-05-28.** Oxygen item tooltips state that GalaxySpace dimensions may need a full spacesuit.

## Tests

- GGS-05-22, 24: unit test on class hierarchy and config default (GC on the test classpath).
- GGS-05-23: `runServer` boot without GC.
- GGS-05-25, 26: unit test of `handleGearType` for each gear type.
- `/gregguns test` scenario (needs GC): wear the GregGuns mask in GC's mask slot, tanks in the tank slots, a Commando helmet for gear, enter a vacuum dimension and check no suffocation; remove the tanks and check suffocation.
- GGS-05-27: code review; grep for writes into GC classes.

## Open questions

- **Gear component.** Is it acceptable that the Oxygen Mask in GC's mask slot also needs GC's Oxygen Gear item or a GregGuns helmet? Techguns has the same dependency (`OXYGEN_GEAR`), so this is consistent, but a player with only the mask and tanks suffocates. A record "Oxygen gear without GC gear" could let the Mask itself handle `GEAR`.
- **Registry names when GC is absent.** The fallback items reuse the GregGuns names, so a world moved between environments is not guaranteed to keep them. The pack always has GC; confirm that is enough.
- **GalaxySpace behaviour.** Needs a source (the repository is not readable) or a decompile, after which the Mask tooltip text can be made exact.
