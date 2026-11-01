# Mob drops, NEI display and progression

- **Techguns baseline:** [per-mob.md § Drops](../../techguns-spec/10-entities-npcs/per-mob.md#drops), [per-mob.md § lifecycle and equipment](../../techguns-spec/10-entities-npcs/per-mob.md#lifecycle-and-equipment), [mob-catalog.md § equipment](../../techguns-spec/10-entities-npcs/mob-catalog.md#equipment-at-a-glance)
- **Design records:** [0001 Tier map](../../design/0001-tier-map.md), [0005 World presence](../../design/0005-world-presence.md)
- **Pack mods:**
  - Mobs Info (`mobsinfo`, 0.6.0, source tag `0.6.0-GTNH`)
  - NewHorizonsCoreMod (`dreamcraft`, 2.9.84), the Custom Drops module
  - Infernal Mobs (`InfernalMobs`, 1.10.7-GTNH), see [infernal-mobs.md](infernal-mobs.md)
- **Roadmap:** M7 (NPC loot), M9 (NEI polish and balance)

This sheet covers three things: how the pack's NEI "Mob Info" page finds our NPC drops, which pack-side hook can add drops to our NPCs without touching our code, and how NPC equipment and loot respect the tier map.

## What changes from Techguns

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| Equipment is rolled by `addRandomArmor(int difficulty)`, an overload. Vanilla's no-argument `addRandomArmor()` is not overridden ([per-mob.md](../../techguns-spec/10-entities-npcs/per-mob.md#lifecycle-and-equipment)). | The no-argument override is the single equipment entry point, and rolls the difficulty itself. | Mobs Info only simulates the vanilla entry point. Otherwise NEI shows no equipment drops. |
| Loot code is free-form. | Drop code draws only from the entity's own `rand` and touches no world state. | Mobs Info replaces that RNG to enumerate every outcome. |
| The held item drops at vanilla's equipment rate (8.5% plus 1% per Looting level) and again as the rare drop, a copy at 2.5% (5 in 200) on player kills: about 11% together. No ammo for the carried gun. | The held item never drops through the equipment roll. The rare drop is a copy of the gun only, at 2% plus 1% per Looting level on player kills. Every death that drops loot also drops what the gun fires: loose rounds, a full tank, or for an energy gun a few parts of its recipe (D312, GGS-10-38 to GGS-10-40). | Owner rule (alpha play session 2): a rare gun drop, and ammo from the gun a mob carried. Energy rifles have no ammo item and never drop a battery. |
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
- **Source (hard-coded rates):** the simulated rare drop is listed at 2.5% (`getchance(250)`) and every equipment stack at 8.5% (`getchance(850)`), whatever `equipmentDropChances` holds ([L915-L966](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java#L915-L966)). The dummy entity holds nothing while `dropFewItems` runs, so drops tied to the carried gun cannot be found by simulation.
- **Source (extra providers):** a class that implements `IMobExtraInfoProvider` and is a registered `LivingDropsEvent` or `LivingDeathEvent` listener edits each mob's list after the simulation ([ExtraLoader.java#L44-L69](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/extras/ExtraLoader.java#L44-L69), applied at [L146-L166](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/extras/ExtraLoader.java#L146-L166)).
- **GregGuns does:**
  - Keeps drop code simulation-friendly (GGS-10-15 to GGS-10-18).
  - Adds the carried gun's drops in `NpcGunDropsHandler`, a `LivingDropsEvent` handler that is also the `IMobExtraInfoProvider` (`@Optional`). On the page it removes any Additional gun row and adds, for each weapon the profile can roll, a Rare row at P × 2% (player only, lootable, "+1% per Looting level"; none for a profile without the rare drop) and Normal rows for the gun's ammo, tank or salvage at P times the average count per death. P, the chance the NPC carries the gun, comes from the profile itself (`NpcDropInfo.carryChances`: the weighted weapon pick, danger 0 to 3 as for an egg), not from Mobs Info's equipment enumeration: in the dev run that enumeration listed no equipment rows for our NPCs, although the same roll runs clean in Mobs Info's `DummyWorld` (its loop swallows exceptions, [L874-L876](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java#L874-L876)). Rows for the same item from the same gun merge; the same ammo from two guns stays two rows, each naming its gun and count range (D349). Armor rows, when Mobs Info lists them, stay at 8.5%. The arithmetic is `NpcDropInfo` (pure, unit-tested).
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

| NPC | Natural path under 0005 | Held guns (0001 tier) | Armor (0001 tier) | Fixed drops | Ceiling |
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
| Outcast | egg only | Laser, Minigun, Bio, Tesla (EV), Flamethrower, LMG (HV), Grim Reaper (LuV) | Power Armor (IV) | none | LuV |
| Stormtrooper | egg only | Blaster Rifle (IV), Laser (EV) | T3 Miner HEV suit (not in 0001) | none | IV |
| Super Mutants | egg only | Rocket Launcher (HV), AK47, Combat Shotgun (MV), Laser (EV) | none | none | EV |
| Dictator Dave | egg only | Golden Revolver (IV) | T2 Beret and T1 mix | Golden Revolver (IV), 9mm rounds | IV |

Held-gun and armor tiers come from [0001](../../design/0001-tier-map.md). Rosters come from [per-mob.md](../../techguns-spec/10-entities-npcs/per-mob.md). Egg-only NPCs are not a progression problem while only eggs, commands and creative tools spawn them. They become one the moment a spawner block or camp flag places them.

- **Pack-side change:** none.

### NPC loot tables

Each death draws 0 to 2 slots plus the Looting level from the mob's table, every slot equally likely (a slot listed twice is twice as likely), on top of the carried gun's own drop (GGS-10-39). Counts are uniform and never zero. References resolve as structure loot does (`LootCategories.stack`), and every one must resolve in the pack (`npc.loot_simulation`). The code is `NpcProfiles`.

| NPC (ceiling) | Table slots | Fixed drops |
|---|---|---|
| Zombie Soldier (MV) | Tanned Leather, iron ingot, gunpowder, shotgun shells 1–8, 9mm bullets 1–3 | none |
| Zombie Farmer (Steam) | Tanned Leather, wheat seeds 1–3, gunpowder, wheat 1–3, leather 1–3, bread 1–2, apple 1–2, stone bullets 2–6 | none |
| Zombie Miner (Steam) | Tanned Leather, coal, gunpowder, iron ingot, redstone, stone bullets 2–6 | none |
| Zombie Pigman Soldier (HV) | steel plate, steel ingot 1–2, gold ingot 1–3, gunpowder 1–5, shotgun shells 1–7, rifle bullets 1–3, 9mm bullets 1–3, rockets 1–2, a full Fuel Tank | none |
| Skeleton Soldier (MV) | bone 1–2, coal 1–3, Tanned Leather, iron ingot, gunpowder, 9mm bullets 1–4 (2 slots), stone bullets 2–6 | none |
| Army Soldier (MV) | Tanned Leather, iron ingot, gunpowder, shotgun shells 1–8, rifle bullets 1–3 | none |
| Bandit (MV) | Tanned Leather, iron ingot, gunpowder, shotgun shells 1–8, rifle bullets 1–3, 9mm bullets 1–4 | none |
| Commando (MV) | steel ingot, nothing, rubber plate (2), gunpowder (2), rifle bullets 1–3 (2), Explosive Charge, Advanced Explosive Charge | none |
| Outcast (LuV) | rifle bullets 8–24 (3), rockets 1–3, a full Fuel Tank, a full Bio Tank, Laser Barrel, Carbon Receiver, Carbon Stock, Mechanical Parts T3, Power Plating, Rhodium-Plated Palladium plates 1–2 (2), HSS-G ingots 1–2 (2), tungstensteel plates 1–3 | none |
| Stormtrooper (IV) | rubber plate (2), steel ingot (2), redstone (3), an LV circuit (`circuitBasic`), an LV battery (`batteryBasic`), Laser Barrel, Laser Focus, Carbon Receiver, Hardened Receiver, Carbon Stock, tungstensteel plates 1–2 (2), tungstensteel screws 2–4 (2), titanium plates 1–2 | none |
| Super Mutants, all three (EV) | ghast tear, blaze rod (4), Cybernetic Parts (15), rockets 1–2 (2), rifle bullets 3–9 (3), shotgun shells 2–8 (3) | none |
| Cyberdemon (LuV) | ghast tear, blaze rod (4), Cybernetic Parts (15) | none |
| Alien Bug (LV) | string 1–2 (2), spider eye | none |
| Psycho Steve (HV) | none | Chainsaw, full Fuel Tanks 4–8 |
| Dictator Dave (IV) | none | Golden Revolver, 9mm bullets 8–16 |
| Attack Helicopter (HV) | none | rifle bullets 16–31, rockets 8–15, Cybernetic Parts 2–3, iron blocks 4–7 |

Every mob that carries a gun with loose rounds or a tank has that ammo in its table or fixed drops, except the Cyberdemon (its Nether Charge comes only from the gun drop), and a mob whose guns are all energy guns drops energy-gun components (D349; `NpcLootTest.everyGunCarrierDropsItsAmmoOrComponents`).

## Requirements

| ID | Requirement |
|---|---|
| GGS-10-15 | Every random draw in `dropFewItems`, `dropRareDrop` and the equipment roll uses the entity's own `rand` field. None uses `worldObj.rand`, `Math.random()`, a static `Random` or the wall clock. |
| GGS-10-16 | NPC constructors and the three drop methods run against a stub world. They read no chunk, biome, difficulty-dependent world state or config that changes at runtime. |
| GGS-10-17 | The no-argument `addRandomArmor()` override is the only code path that rolls NPC equipment. It sets equipment through the vanilla equipment slots (`setCurrentItemOrArmor`). `enchantEquipment()` is overridden to do nothing. Natural spawns call the same logic with their danger level. |
| GGS-10-18 | Counts that Techguns draws as a rounded float (such as `1 + round(7y)`) use integer draws where the distribution allows. Where it does not, the NPC implements `IMobInfoProvider` (soft dependency) and lists those drops itself. |
| GGS-10-19 | Armor drops at the vanilla equipment rate (8.5% per slot, plus 1% per Looting level). The held item never drops through the equipment roll: its chance is `NpcEntity.NO_HELD_DROP` (below anything Looting adds; vanilla's roll is `nextFloat() - looting × 0.01 < chance`, so 0 would still drop 1% per Looting level), set in the constructor and again after reading a save. Mobs Info's hard-coded gun rows are corrected through `IMobExtraInfoProvider` (GGS-10-38 to GGS-10-40). |
| GGS-10-38 | Vanilla's rare drop (`dropRareDrop`) drops nothing. On a player kill (`recentlyHit`) of an NPC whose profile has the rare drop, a fresh copy of the held gun (count 1, no NBT) drops with chance 2% plus 1% per Looting level. Only an `ItemGun` copy-drops; vanilla tools never do. The mini-bosses have no rare drop. |
| GGS-10-39 | Every death that drops loot (`doMobLoot` on, adult NPC, any killer) drops what the held gun fires. Loose ammo: 25% to 75% of a magazine counted in loose items, one item a round as the player counts them (`ceil(0.25 × mag)` to `max(lo, floor(0.75 × mag))` items, uniform), at least 1 and at most one stack (64). An item that is several shots by design (`NpcDrops.MULTI_SHOT_ITEMS`: the Nether Charge, ten shots) counts the items one full magazine takes instead. A gun that fires from a tank drops one full tank. Table: 9mm Pistol 5–13 9mm bullets; Thompson 5–15; Revolver 2–4; AK-47, M4 8–22 rifle bullets; LMG 25–64; Minigun 50–64; Bolt Action 2–4; Combat Shotgun 2–6 shells; Sawed Off 1; Hand Cannon 1 stone bullet; Rocket Launcher 1 rocket; Grim Reaper 1–3; Nether Blaster 1 Nether Charge; Flamethrower and Chainsaw 1 full Fuel Tank; Bio Gun 1 full Bio Tank (D312, D349). |
| GGS-10-40 | An energy gun (Laser, Tesla, Blaster Rifle, any gun with `isEnergy()`) drops no battery and no ammo. Each loot-dropping death makes 2 weighted picks from its recipe tree (`GunSalvage`): its assembly row and, one level down, the rows of the GregGuns components it takes. GT parts (plates, screws, foil, small gears, springs, pipes, lenses) weigh 3 and drop 1 to half their recipe count (at most 4); GregGuns items (receivers, barrels, stocks, mechanical parts, focus) weigh 1 and drop 1. Circuits, GT `ItemList` parts (batteries, emitters), fluids and ore inputs are left out. |
| GGS-10-20 | Every registered NPC entity has a lang key `entity.gregguns.<Name>.name` with English text. This fixes the Techguns missing-key problems in [lang.md](../../techguns-spec/10-entities-npcs/lang.md). |
| GGS-10-21 | Each NPC has a data-driven tier ceiling. A dev self-check fails if any held gun, armor piece or fixed or table drop of that NPC has a recipe tier above its ceiling in 0001 (`NpcLootTest.ceilingsHold`: every gun, armor set and GregGuns round, part and tank). Exceptions are listed in the open questions; the Attack Helicopter's fixed Cybernetic Parts (EV over HV) are accepted (D349). |
| GGS-10-22 | A GregGuns class that implements a Mobs Info interface does so under `@Optional.Interface(iface=..., modid="mobsinfo")` and `@Optional.Method`, so the mod works without Mobs Info. |

## Tests

| ID | Verification |
|---|---|
| GGS-10-15, 16 | Unit test: construct each NPC with a stub world and a seeded `Random` set on the entity. Call `dropFewItems(true, 0)`, `dropFewItems(true, 1)`, `dropRareDrop(0)` and the equipment roll twice with the same seed. The results must be equal. |
| GGS-10-17 | Unit test: the no-argument call fills the same slots as `onSpawnWithEgg` for a given seed. A reflective check asserts `enchantEquipment` is declared on the NPC base class. |
| GGS-10-18, 19 | runServer check with Mobs Info loaded (`-Pgg.devMobsInfo=true`): the log has no "random call limit", "enumeration incomplete" or "error while loading" line for `gregguns.*`, and "Registered N Extra API providers" counts ours. Scenario `npc.mobsinfo_drops` (skips without Mobs Info): the Army Soldier, Outcast and Psycho Steve pages have no Additional gun row, a Rare gun row (none for Steve) and an ammo or salvage row. `npc.gun_drops_saved_chance`: the held-item chance is `NO_HELD_DROP` new and after a save that lists 0.085. `npc.gun_drops_rates` also kills at Looting 3 with the rare drop off and must see no gun. |
| GGS-10-38 | Scenario `npc.gun_drops_rates`: 2000 seeded player kills with an AK-47 at Looting 0 and 3 drop the gun within the binomial 3-sigma band of 2% and 5%; with the rare drop off, never. Unit test `NpcGunDropsTest`: `rareGunChance` for Looting 0 to 3. |
| GGS-10-39, 40 | Unit test `NpcGunDropsTest` pins the table and the magazine-share rule for every carried gun, the salvage pools (no circuit, no battery; the gun's own screws and plates and a receiver present), determinism and the NEI rows. Scenario `npc.gun_drops_every_gun`: 200 non-player kills per carried gun drop exactly that gun's rule on every death, within one stack, and never the gun. `npc.gun_drops_gamerule`: nothing drops with `doMobLoot` off. |
| GGS-10-20 | Unit test: every name in the registry has a lang key. The Mobs Info log has no "doesn't have localized name" line for `gregguns.*`. |
| GGS-10-21 | Dev self-check run in `./gradlew test`. |
| GGS-10-22 | runServer check without Mobs Info: no `NoClassDefFoundError`. |

## Open questions

1. **Psycho Steve** drops a Chainsaw, an HV item under [0001](../../design/0001-tier-map.md), as a fixed drop from a surface danger-1 mini-boss. [0005](../../design/0005-world-presence.md) says surface loot defaults to LV-MV. Options: cap his held weapon and drop at MV, or accept HV for this one NPC. It needs a one-line record.
2. **Nether-only NPCs.** The Zombie Pigman Soldier and Cyberdemon spawn only in the Nether in Techguns, and 0005 limits natural spawning to the Overworld. Under 0005 they never spawn naturally. See [spawning.md](spawning.md).
3. **T3 Miner (HEV suit) and the Cyberdemon Blaster** have no row in 0001. The ceiling check cannot cover them until a tier is assigned.
4. **Infernal Drops in NEI.** GregGuns tiered drops cannot appear on that page without joining the global lists. See [infernal-mobs.md](infernal-mobs.md), open question 3.
