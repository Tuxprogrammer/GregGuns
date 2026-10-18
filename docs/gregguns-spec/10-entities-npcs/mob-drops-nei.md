# Mob drops, NEI display and progression

- **Techguns baseline:** [per-mob.md § Drops](../../techguns-spec/10-entities-npcs/per-mob.md#drops), [per-mob.md § lifecycle and equipment](../../techguns-spec/10-entities-npcs/per-mob.md#lifecycle-and-equipment), [mob-catalogue.md § equipment](../../techguns-spec/10-entities-npcs/mob-catalogue.md#equipment-at-a-glance)
- **Design records:** [0001 Tier map](../../design/0001-tier-map.md), [0005 World presence](../../design/0005-world-presence.md)
- **Pack mods:**
  - Mobs Info (`mobsinfo`, 0.6.0, source tag `0.6.0-GTNH`)
  - NewHorizonsCoreMod (`dreamcraft`, 2.9.84), the Custom Drops module
  - Infernal Mobs (`InfernalMobs`, 1.10.7-GTNH), see [infernal-mobs.md](infernal-mobs.md)
- **Roadmap:** M7 (NPC loot), M9 (NEI polish and balance)

This sheet covers three things: how the pack's NEI "Mob Info" page finds our NPC drops, which pack-side hook can add drops to our NPCs without touching our code, and how NPC equipment and loot respect the tier map.

## What changes from Techguns

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| Equipment is rolled by `addRandomArmor(int difficulty)`, an overload. Vanilla's no-argument `addRandomArmor()` is not overridden ([per-mob.md](../../techguns-spec/10-entities-npcs/per-mob.md#lifecycle-and-equipment)). | The no-argument override is the single equipment entry point, and rolls the difficulty itself. | Mobs Info only simulates the vanilla entry point. Otherwise NEI shows no equipment drops. |
| Loot code is free-form. | Drop code draws only from the entity's own `rand` and touches no world state. | Mobs Info replaces that RNG to enumerate every outcome. |
| Mini-bosses always drop a fixed item, whatever the spawn context (Psycho Steve: Chainsaw; Dictator Dave: Golden Revolver). | Drops and held gear must not exceed the mob's tier ceiling. | [0005](../../design/0005-world-presence.md): "loot never contains items above the tier the location implies". |

## Integration points

### Mobs Info: how drops are discovered

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/mobsinfo/mobsinfo.cfg`.
  - `mobhandler.Enabled=true`, `IncludeEmptyMobs=true`, `MobTimeout=10.0` seconds per mob.
  - `MobBlacklist` lists `EntityList` names such as `SpecialMobs.SpecialCreeper` and `OpenBlocks.Luggage`.
  - `compatibility.enableInfernalDrops=true`.
- **Source:** [MobRecipeLoader.java](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java). It does this for every non-abstract `EntityLiving` registered with `EntityList`:
  - Builds the entity with its `(World)` constructor in a `DummyWorld` ([L514-L529](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java#L514-L529)).
  - Sets `captureDrops = true` and **replaces the entity's `rand` field** with a recording RNG that walks every branch ([L555-L559](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java#L555-L559)).
  - Calls `dropFewItems(true, 0)`, `dropFewItems(true, 1)`, `dropRareDrop(0)` and `dropRareDrop(1)` and records the captured stacks ([L739-L758](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java#L739-L758)).
  - Finds the nearest override of the vanilla `addRandomArmor` (`func_82164_bB`) in the class chain. If there is one, it calls it, reads the entity's equipment slots, and lists each stack as an *Additional* drop at the vanilla 8.5% equipment rate. If the override of `enchantEquipment` is missing, it also lists enchanted variants of each stack ([L772-L848](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java#L772-L848)).
  - Skips any name on `MobBlacklist` ([L492](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java#L492)).
  - Warns when an entity's display name still starts with `entity.` ([L550-L553](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java#L550-L553)).
- **Source (escape hatch):** an entity may implement `IMobInfoProvider`. The loader then skips the simulation and uses the list the entity supplies, unless the class also overrides `dropFewItems` or `dropRareDrop` without overriding `provideDropsInformation` ([L561-L601](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java#L561-L601), [IMobInfoProvider](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/api/IMobInfoProvider.java)).
- **Source (spawn info):** with Just Another Spawner absent, as in this pack, the mob's spawn tab is built from `BiomeGenBase.getSpawnableList(type)` for every biome and creature type. See [MobRecipe.java#L177-L190](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/api/MobRecipe.java#L177-L190).
- **GregGuns does:**
  - Keeps drop code simulation-friendly (GGS-10-15 to GGS-10-18).
  - Implements `IMobInfoProvider` only where a drop cannot be written that way. It is a soft dependency (`@Optional.Interface` for `mobsinfo`), the same pattern dreamcraft uses (next section).
  - Does **not** try to put infernal-tier drops into the NPC's page. The Infernal Drops page is built from the global lists only, per [MobHandlerInfernal#L304-L345](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/nei/MobHandlerInfernal.java#L304-L345). That is a limitation of the pack, not of GregGuns.
- **Pack-side change:** add `gregguns.SimpleTurret` to `mobhandler.MobBlacklist`. The turret has no drops and would otherwise show as an empty mob because `IncludeEmptyMobs=true`. If the spawn placeholder is kept, add `gregguns.TGDummySpawn` too.

### dreamcraft Custom Drops: per-class drops as a pack hook

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GTNewHorizons/CustomDrops.xml`. The module is on: `dreamcraft.cfg` has `CustomDrops=true`. The shipped file has 15 entries, all Twilight Forest, Witchery and BoP classes.
- **Source:** [CustomDropsHandler#L47-L48, L173-L218](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modcustomdrops/CustomDropsHandler.java#L173-L218).
  - It matches the entity's **fully qualified class name**, case-insensitively, with no inheritance ([CustomDrops#L27-L45](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modcustomdrops/CustomDrops.java#L27-L45)).
  - It rolls only on kills by a real player, using `source.getEntity()`. Fake players are skipped.
  - `Chance` is a percentage, `LimitedDropCount` caps the drops per player over their lifetime (0 means unlimited), and `RandomAmount` rolls 1 to `Amount`.
  - It already implements `IMobExtraInfoProvider` under `@Optional.Interface`, so these drops show in NEI automatically ([L220-L245](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modcustomdrops/CustomDropsHandler.java#L220-L245)). The same pattern is the model for any GregGuns soft dependency on Mobs Info.
- **GregGuns does:** nothing in code. It keeps the NPC class names stable (GGS-10-03) and sets the shooter as `getEntity()` (GGS-10-06), so gun kills count as player kills.
- **Pack-side change:** optional. The pack can give each mini-boss a once-per-player reward without touching GregGuns:

```xml
<CustomDrop EntityClassName="com.gregguns.entity.npc.PsychoSteve">
    <Drop Identifier="PsychoSteve_FirstKill" ItemName="<modid:item>" Amount="1" NBTTag="" Chance="100" LimitedDropCount="1" RandomAmount="false"/>
</CustomDrop>
```

The class name and package above are a placeholder until M7 fixes them.

### Equipment and loot against the tier map

- **Pack config:** none. The tier map is [0001](../../design/0001-tier-map.md), the surface ceiling is [0005](../../design/0005-world-presence.md).
- **GregGuns does:** gives each NPC a tier ceiling. Everything the NPC can hold, wear or drop is at or below it. The ceiling is data, and a dev self-check reads it against the 0001 table.

| NPC | Natural path under 0005 | Held guns (0001 tier) | Armour (0001 tier) | Fixed drops | Ceiling |
|---|---|---|---|---|---|
| Zombie Farmer, Zombie Miner | Overworld, danger 0 | hoes, picks, Hand Cannon (Steam) | T1 Miner (Steam) | none | Steam |
| Zombie Soldier | Overworld, danger 1 | Revolver (LV), Thompson (MV) | T1 Combat (LV) | none | MV |
| Skeleton Soldier | Overworld, danger 1 | Revolver (LV), Thompson (MV), Hand Cannon (Steam) | T1 Combat or Scout (LV, Steam) | none | MV |
| Bandit | Overworld, danger 2 | 9mm Pistol, Revolver, Sawed Off, Bolt Action (LV), AK47, Thompson (MV) | T1 Scout (Steam) | none | MV |
| Psycho Steve | Overworld, danger 1, weight 3 | Chainsaw (**HV**) | T1 Miner (Steam) | Chainsaw (**HV**), Fuel Tank | HV: **exceeds the 0005 default** |
| Army Soldier | camp flags | M4, Combat Shotgun (MV), Bolt Action (LV) | T2 Combat (MV) | none | MV |
| Commando | camp flags | silenced M4 (MV) | T2 Commando (MV) | none | MV |
| Zombie Pigman Soldier | Nether only | up to Rocket Launcher, LMG, Flamethrower (HV) | T2 Combat (MV) | none | HV, and not natural under 0005 |
| Cyberdemon | Nether only | Cyberdemon Blaster (not in 0001) | none | none | not in 0001 |
| Outcast | egg only | Laser, Minigun, Bio, Tesla (EV), Flamethrower, LMG (HV), Grim Reaper (LuV) | Power Armour (IV) | none | LuV |
| Stormtrooper | egg only | Blaster Rifle (IV), Laser (EV) | T3 Miner HEV suit (not in 0001) | none | IV |
| Super Mutants | egg only | Rocket Launcher (HV), AK47, Combat Shotgun (MV), Laser (EV) | none | none | EV |
| Dictator Dave | egg only | Golden Revolver (IV) | T2 Beret and T1 mix | Golden Revolver (IV), 9mm rounds | IV |

Held-gun and armour tiers come from [0001](../../design/0001-tier-map.md). Rosters come from [per-mob.md](../../techguns-spec/10-entities-npcs/per-mob.md). Egg-only NPCs are not a progression problem while only eggs, commands and creative tools spawn them. They become one the moment a spawner block or camp flag places them.

- **Pack-side change:** none.

## Requirements

| ID | Requirement |
|---|---|
| GGS-10-15 | Every random draw in `dropFewItems`, `dropRareDrop` and the equipment roll uses the entity's own `rand` field. None uses `worldObj.rand`, `Math.random()`, a static `Random` or the wall clock. |
| GGS-10-16 | NPC constructors and the three drop methods run against a stub world. They read no chunk, biome, difficulty-dependent world state or config that changes at runtime. |
| GGS-10-17 | The no-argument `addRandomArmor()` override is the only code path that rolls NPC equipment. It sets equipment through the vanilla equipment slots (`setCurrentItemOrArmor`). `enchantEquipment()` is overridden to do nothing. Natural spawns call the same logic with their danger level. |
| GGS-10-18 | Counts that Techguns draws as a rounded float (such as `1 + round(7y)`) use integer draws where the distribution allows. Where it does not, the NPC implements `IMobInfoProvider` (soft dependency) and lists those drops itself. |
| GGS-10-19 | Held guns and armour drop at the vanilla equipment rate (8.5% per slot, plus looting). If GregGuns changes it for any NPC, that NPC implements `IMobInfoProvider`. |
| GGS-10-20 | Every registered NPC entity has a lang key `entity.gregguns.<Name>.name` with English text. This fixes the Techguns missing-key problems in [lang.md](../../techguns-spec/10-entities-npcs/lang.md). |
| GGS-10-21 | Each NPC has a data-driven tier ceiling. A dev self-check fails if any held gun, armour piece or fixed or table drop of that NPC has a recipe tier above its ceiling in 0001. Exceptions are listed in the open questions. |
| GGS-10-22 | A GregGuns class that implements a Mobs Info interface does so under `@Optional.Interface(iface=..., modid="mobsinfo")` and `@Optional.Method`, so the mod works without Mobs Info. |

## Tests

| ID | Verification |
|---|---|
| GGS-10-15, 16 | Unit test: construct each NPC with a stub world and a seeded `Random` set on the entity. Call `dropFewItems(true, 0)`, `dropFewItems(true, 1)`, `dropRareDrop(0)` and the equipment roll twice with the same seed. The results must be equal. |
| GGS-10-17 | Unit test: the no-argument call fills the same slots as `onSpawnWithEgg` for a given seed. A reflective check asserts `enchantEquipment` is declared on the NPC base class. |
| GGS-10-18, 19 | runServer check with Mobs Info loaded: the log has no "random call limit" or "enumeration incomplete" line for `gregguns.*`. In NEI, each NPC's page lists its table items, and its guns as Additional at 8.5%. |
| GGS-10-20 | Unit test: every name in the registry has a lang key. The Mobs Info log has no "doesn't have localized name" line for `gregguns.*`. |
| GGS-10-21 | Dev self-check run in `./gradlew test`. |
| GGS-10-22 | runServer check without Mobs Info: no `NoClassDefFoundError`. |

## Open questions

1. **Psycho Steve** drops a Chainsaw, an HV item under [0001](../../design/0001-tier-map.md), as a fixed drop from a surface danger-1 mini-boss. [0005](../../design/0005-world-presence.md) says surface loot defaults to LV-MV. Options: cap his held weapon and drop at MV, or accept HV for this one NPC. It needs a one-line record.
2. **Nether-only NPCs.** The Zombie Pigman Soldier and Cyberdemon spawn only in the Nether in Techguns, and 0005 limits natural spawning to the Overworld. Under 0005 they never spawn naturally. See [spawning.md](spawning.md).
3. **T3 Miner (HEV suit) and the Cyberdemon Blaster** have no row in 0001. The ceiling check cannot cover them until a tier is assigned.
4. **Infernal Drops in NEI.** GregGuns tiered drops cannot appear on that page without joining the global lists. See [infernal-mobs.md](infernal-mobs.md), open question 3.
