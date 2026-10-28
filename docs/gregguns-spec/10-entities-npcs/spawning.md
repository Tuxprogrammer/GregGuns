# Spawning and coexistence with other spawn mods

- **Techguns baseline:** [spawning.md](../../techguns-spec/10-entities-npcs/spawning.md), [ai.md § factions](../../techguns-spec/10-entities-npcs/ai.md#1-factions-tgnpcfaction-tgnpcfactions), [per-mob.md § TGDummySpawn](../../techguns-spec/10-entities-npcs/per-mob.md#18-tgdummyspawn-tgdummyspawn)
- **Design records:** [0005 World presence](../../design/0005-world-presence.md)
- **Pack mods:**
  - SpecialMobs (`SpecialMobs`, 3.7.8)
  - EnderZoo (`EnderZoo`, 1.3.6)
  - AngerMod (`angermod`, 1.0.7)
  - Mobs Info (`mobsinfo`, 0.6.0) and Infernal Mobs (`InfernalMobs`, 1.10.7-GTNH), for the placeholder problem
- **Roadmap:** M7 (NPCs and spawning)

[0005](../../design/0005-world-presence.md) fixes the scope: natural spawning in the **Overworld only**, config-gated per faction, Techguns' weights halved as a starting point, caps enforced. This sheet records what in the pack touches that, and what GregGuns must do about it.

## What changes from Techguns

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| One placeholder mob (`TGDummySpawn`, an `EntityMob`) is added to vanilla spawning in every non-Nether, non-End biome. Its `EntityJoinWorldEvent` is intercepted and canceled, and a real NPC is spawned in its place ([spawning.md § 1](../../techguns-spec/10-entities-npcs/spawning.md#12-replacement)). | Recommended: register the real NPC classes as spawn entries and apply dimension and danger rules in a `CheckSpawn` handler. No placeholder. | The placeholder is a real `IMob`. Infernal Mobs, Mobs Info and EnderZoo's global modifier all see it. Mobs Info's spawn tab shows only registered classes. See the table below. |
| Dimension list per spawn is `[0]` by default, and other dimensions print "TotalWeight 0" for every attempt ([NPC-16](../../techguns-spec/10-entities-npcs/index.md#suspected-bugs)). | One config list, default `[0]`, checked at spawn time. Other dimensions are skipped silently. | [0005](../../design/0005-world-presence.md). |
| Weights: Farmer 200, Miner 200, Soldier 100, Skeleton 100, Bandit 50, Psycho Steve 3, placeholder 600. | Start at half, rounded down, minimum 1: Farmer 100, Miner 100, Soldier 50, Skeleton 50, Bandit 25, Psycho Steve 1. | [0005](../../design/0005-world-presence.md). Final numbers come from the M9 balance pass. |
| Zombie Pigman Soldier and Cyberdemon spawn only in the Nether. | Not spawned naturally under 0005. | Open question 2. |

## Integration points

### The placeholder against Infernal Mobs, Mobs Info and EnderZoo

| Mod | What it does to a placeholder `IMob` | Source |
|---|---|---|
| Infernal Mobs | Rolls on every join of a living entity, and keeps modified ones in a strong map. A placeholder that rolls is never removed. | [EntityEventHandler#L70-L80](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L70-L80), [InfernalMobsServer#L8-L24](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsServer.java#L8-L24) |
| Mobs Info | Lists it in NEI as a mob with no drops, because `IncludeEmptyMobs=true`. It also gives the real NPCs no spawn tab, because it reads the biome lists, which hold only the placeholder. | [MobRecipe#L177-L190](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/api/MobRecipe.java#L177-L190) |
| EnderZoo | Its join handler queues every `IMob` for the global difficulty modifier, which rewrites max health and attack damage. | [MobSpawnEventHandler#L96-L112](https://github.com/GTNewHorizons/EnderZoo/blob/1.3.6/src/main/java/crazypants/enderzoo/spawn/MobSpawnEventHandler.java#L96-L112) |

A handler that cancels the join at `EventPriority.HIGHEST` stops the first and third. Forge skips a canceled event for handlers that do not set `receiveCanceled`, and the Infernal Mobs and EnderZoo join handlers do not. It does nothing for the second.

- **GregGuns does:** per GGS-10-26, register real classes. If the open question 1 record keeps the placeholder, then GGS-10-04 applies and the pack blacklists it in Mobs Info.
- **Pack-side change:** only in the placeholder case: `mobhandler.MobBlacklist` gets `gregguns.TGDummySpawn`. See [mob-drops-nei.md](mob-drops-nei.md).

### SpecialMobs: no interaction

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/SpecialMobs.cfg`, `_general.dimension_blacklist=` (empty).
- **Source:** it replaces a mob on join only if `canReplace` passes. That requires `IMob`, then `monsterKey(EntityList.getEntityString(entity)) >= 0`, a lookup in a fixed list of vanilla names (Blaze, CaveSpider, Creeper, Enderman, Ghast, PigZombie and so on). [MobHelper#L338-L348](https://github.com/GTNewHorizons/SpecialMobs/blob/3.7.8/src/main/java/toast/specialMobs/MobHelper.java#L338-L348), [_SpecialMobs#L424-L430](https://github.com/GTNewHorizons/SpecialMobs/blob/3.7.8/src/main/java/toast/specialMobs/_SpecialMobs.java#L424-L430).
- **GregGuns does:** nothing. `gregguns.AlienBug` extends `EntitySpider`, but the lookup is by registry name, so it is not replaced.
- **Pack-side change:** none.

### EnderZoo: spawn tuning lever and a third difficulty scaler

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/enderzoo/SpawnConfig_User.xml` (all examples commented out) and `EnderZoo.cfg`, `difficulty.globalDifficultyModifierEnabled=true`, `globalEasyHealthModifier=0.9`, `globalHardHealthModifier=1.1`, with the same values for attack.
- **Source:**
  - EnderZoo reads `mobName` from the XML, resolves it through `EntityList.stringToClassMapping`, and calls `EntityRegistry.addSpawn` or `removeSpawn`. That works for **any registered mob**, including ours. [MobSpawns#L40-L90](https://github.com/GTNewHorizons/EnderZoo/blob/1.3.6/src/main/java/crazypants/enderzoo/spawn/MobSpawns.java#L40-L90). The file also supports `dimensionExclude` by name, id or id range, enforced in a `CheckSpawn` handler ([L77-L91](https://github.com/GTNewHorizons/EnderZoo/blob/1.3.6/src/main/java/crazypants/enderzoo/spawn/MobSpawnEventHandler.java#L77-L91)).
  - It loads this in `postInit` ([EnderZoo.java#L137-L142](https://github.com/GTNewHorizons/EnderZoo/blob/1.3.6/src/main/java/crazypants/enderzoo/EnderZoo.java#L137-L142)). A `remove` entry only takes effect if our spawns are already registered.
  - The global modifier multiplies the base `maxHealth` and `attackDamage` attributes of every `IMob` once, and calls `setHealth` ([L136-L200](https://github.com/GTNewHorizons/EnderZoo/blob/1.3.6/src/main/java/crazypants/enderzoo/spawn/MobSpawnEventHandler.java#L136-L200)). That is ±10% on Easy and Hard.
- **GregGuns does:**
  - Registers its spawns so that EnderZoo's `postInit` sees them (GGS-10-27). The pack can then add, remove or filter `gregguns.*` spawns by biome type and dimension without a GregGuns release.
  - Leaves max health and the melee `attackDamage` attribute alone, and scales only gun damage and accuracy by difficulty (GGS-10-29). This is the roadmap's "scaling applied once" ([GUN-06](../../techguns-spec/BUGS.md), [NPC-23](../../techguns-spec/10-entities-npcs/index.md#suspected-bugs)). EnderZoo already scales the other two.
- **Pack-side change:** none required. Optional: a normal EnderZoo spawn entry with `mobName="gregguns.<Name>"` retunes one NPC's rate, biomes or dimensions.

### AngerMod: spawn protection and Kamikaze

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/angermod.cfg`.
  - `spawn-protection.enabled=true`, `maxDuration=90`, `moveTolerance=5`. Players are made invulnerable after respawn or a dimension change, until they act or move.
  - `kamikaze.enabled=true`, `chance=10`, `doTerrainDamage=false`, with `butcherItems` listing GT knives and similar.
  - `block-break-anger` and `friendly-animal-revenge` are about the Nether, the End and passive animals. They do not touch GregGuns.
- **Source:**
  - Spawn protection sets `capabilities.disableDamage = true` for the protected player ([PlayerSpawnProtection#L73-L78](https://github.com/GTNewHorizons/AngerMod/blob/1.0.7/src/main/java/com/github/namikon/angermod/events/PlayerSpawnProtection.java#L73-L78)).
  - Kamikaze reacts to any `LivingDeathEvent` whose `getSourceOfDamage()` is a real player, not only passive animals. With 10% chance it explodes at the victim unless the player's held item has an unlocalized name containing a `butcherItems` entry ([KamikazeRevenge#L22-L50](https://github.com/GTNewHorizons/AngerMod/blob/1.0.7/src/main/java/com/github/namikon/angermod/events/KamikazeRevenge.java#L22-L50)).
- **GregGuns does:**
  - NPC target selection skips players with `capabilities.disableDamage` (GGS-10-28), so NPCs do not burn ammo on a protected player.
  - Melee kills put the player as the direct entity: the Combat Knife, Crowbar, Chainsaw, Power Hammer and gun-butt sources. Those can trigger Kamikaze on a GregGuns NPC. Bullets and projectiles have the projectile as the direct entity, so they cannot.
  - Item unlocalized names contain `gregguns.` (a convention for [06-items](../06-items/pack-loot.md) to follow).
- **Pack-side change:** add `gregguns.` to `kamikaze.butcherItems` in `config/angermod.cfg`. The match is a substring on the lowercase unlocalized name, so one entry covers every GregGuns item.

### Spawner NPCs

Every GregGuns structure spawner (the Big Bunker's, the Alien Bug Nest's and the ICBM silo posts) is the vanilla `minecraft:mob_spawner` with GregGuns tile NBT ([design record 0024](../../design/0024-vanilla-spawners.md), [11-worldgen structure-placement](../11-worldgen/structure-placement.md#spawner-marks)). Vanilla 1.7.10 `MobSpawnerBaseLogic` does three things that would break our NPCs, and each has a fix:

- **No egg setup.** With `SpawnData` set, vanilla copies it over the new entity's NBT, calls `readFromNBT` and spawns, and never calls `onSpawnWithEgg`. Our `SpawnData` holds `GGSpawner: {MinDanger: n}`. `NpcEntity.readEntityFromNBT` sees the key (server side, with a world) and sets the danger to the larger of the spawn position's (`NpcSpawnHandler.dangerAt`) and `n`, marks the Infernal opt-out ([infernal-mobs](infernal-mobs.md)), then runs `onSpawnWithEgg`, which rolls the equipment at that danger. The Alien Bug takes only the Infernal marker. The key is never written back.
- **Light before data.** Vanilla asks `getCanSpawnHere()` before it applies `SpawnData`, and the monster checks (`EntityMob#isValidLightLevel` and `#getBlockPathWeight`) refuse bright spots, so lit rooms would never spawn. `NpcEntity` and `AlienBug` skip only those two checks when a GregGuns-marked spawner of their own type is placing them (`SpawnerOrigin`: a spawner within 4 blocks across and 1 up or down whose current entry is that entity and carries `GGSpawner`; loaded chunks only). The `CheckSpawn` handler first marks every natural candidate, and a marked candidate always keeps every vanilla rule. Peaceful, collision and liquid checks always stay.
- **The client preview.** The client builds the spinning mob with a null world and reads `SpawnData` into it. The NPC skips all of the above without a world, so the preview has no gear and needs no server state.
- **Source:** Minecraft 1.7.10 `MobSpawnerBaseLogic#updateSpawner` and `#func_98265_a` (MCP-decompiled with Forge 10.13.4.1614, which does not patch the class); `SpawnerAnimals#findChunksForSpawning` fires `CheckSpawn` before `getCanSpawnHere`.

### Dimensions

[0005](../../design/0005-world-presence.md) allows the Overworld only, so the default list is `[0]` and every other dimension is excluded without being named. For reference, the pack's own dimension settings:

| Dimension | ID | Pack config |
|---|---|---|
| Galacticraft Moon | 28 | `Galacticraft/core.conf`, `idDimensionMoon` |
| Overworld orbit (and static orbit) | 27 (26) | `Galacticraft/core.conf`, `idDimensionOverworldOrbit` |
| Barnarda C, Barnarda E | 32, 81 | `GalaxySpace/dimensions.conf`, `dimensionIDBarnardaC`, `dimensionIDBarnardaE` |
| Personal Space | 180 and up | `personalspace.cfg`, `firstDimensionId=180` |
| Infernal Mobs blacklist | 50 | `InfernalMobs.cfg`, `dimensionIDBlackList` |

Hardcore Ender Expansion works in the End (`overrideBiomeMobs=false`, `overpoweredMobs=true`) and has no hook into Overworld NPC spawns. It needs nothing from us.

## Requirements

| ID | Requirement |
|---|---|
| GGS-10-23 | Natural NPC spawning runs only in dimensions listed in the config key for spawn dimensions (default `[0]`). The test is `world.provider.dimensionId` at spawn time. Other dimensions are skipped with no log output ([NPC-16](../../techguns-spec/10-entities-npcs/index.md#suspected-bugs)). |
| GGS-10-24 | Starting spawn weights are the halved values in the table above. A weight of 0 means the mob is not registered **and** cannot be selected ([NPC-14](../../techguns-spec/10-entities-npcs/index.md#suspected-bugs), [NPC-15](../../techguns-spec/10-entities-npcs/index.md#suspected-bugs)). |
| GGS-10-25 | NPCs spawn under the vanilla `monster` creature type, so they share the monster cap. Each faction has its own enable flag, default per [0005](../../design/0005-world-presence.md). |
| GGS-10-26 | (Accepted in [0005](../../design/0005-world-presence.md), M7 N6.) Each naturally spawning NPC class is registered with `EntityRegistry.addSpawn` as `monster`. Dimension and danger rules are applied in a `LivingSpawnEvent.CheckSpawn` handler that returns DENY. There is no placeholder entity. |
| GGS-10-27 | GregGuns registers its spawn entries no later than EnderZoo's `postInit`. The mod's dependency string declares `before:EnderZoo`, and the load order is covered by a test. |
| GGS-10-28 | NPC target selection ignores any player with `capabilities.disableDamage` set. That covers creative mode, spectator and AngerMod spawn protection. |
| GGS-10-29 | Difficulty scaling in GregGuns applies to gun damage and accuracy once ([GUN-06](../../techguns-spec/BUGS.md)). It does not change `maxHealth` or `attackDamage` by difficulty. |
| GGS-10-33 | An NPC that a GregGuns spawner places starts at the larger of its position's danger and the spawner's `MinDanger`, with its equipment rolled at that danger, before it joins the world. |
| GGS-10-34 | A GregGuns spawner spawns its NPCs in a lit spot. The same NPC as a natural-spawn candidate, away from any spawner, or beside a plain vanilla spawner keeps the vanilla light checks. |
| GGS-10-35 | The vanilla client preview path (entity with a null world, `SpawnData` read into it) works for every spawner NPC and rolls nothing. |

## Tests

| ID | Verification |
|---|---|
| GGS-10-23 | Unit test of the dimension check with ids 0, -1, 1, 7, 28 and 180. runServer check with a Moon visit: the log has no "TotalWeight" line. |
| GGS-10-24 | Unit test: the weight-to-registration function, and a 10,000-draw statistical check of the selection, with a 0-weight entry that never appears. |
| GGS-10-25 | runServer check: after a long soak, GregGuns NPCs count toward the monster cap (`SpawnerAnimals` count includes them). |
| GGS-10-26 | runServer check: `EntityRegistry` biome spawn lists hold the real classes, and Mobs Info's NEI tab for `gregguns.Bandit` lists biomes. |
| GGS-10-27 | runServer check with an `enderzoo/SpawnConfig_User.xml` entry that removes `gregguns.Bandit` from plains: the entry takes effect. |
| GGS-10-28 | Manual check: a freshly respawned player with AngerMod protection stands in view of an NPC. The NPC does not fire. |
| GGS-10-29 | Unit test: at each difficulty, `maxHealth` and `attackDamage` are the base values on a fresh NPC (before EnderZoo's tick). |
| GGS-10-33 | Unit test `SpawnerDataTest`. Scenarios `worldgen.spawner_caps` (UHV floor: every NPC at danger 5), `worldgen.spawner_infernal_marker` (danger set at join) and `icbm.complex.guards` (danger floor, armed). |
| GGS-10-34 | Scenarios `worldgen.spawner_light` and `icbm.complex.guards` (lit pad). |
| GGS-10-35 | Scenario `worldgen.spawner_preview`. |

## Open questions

1. **Resolved (0005 amended): real classes, no placeholder.** Spawn architecture was: Placeholder (Techguns) or real classes (GGS-10-26). The recommendation is real classes, because it removes the placeholder's side effects and gives Mobs Info real spawn data. It changes the Techguns design, so [0005](../../design/0005-world-presence.md) needs a short amendment. The danger-level equipment roll then needs a hook that carries the danger from the `CheckSpawn` position to the NPC, which is an M7 implementation choice.
2. **Resolved (0005 amended): egg, command or camp only.** Nether-only NPCs were: The Pigman Soldier and Cyberdemon exist only in the Nether in Techguns. Under 0005 they never spawn naturally and are reachable only by egg or command. Either say so in 0005, or add a Nether dimension record. 0005 itself says non-Overworld support needs a new record.
3. **Order of `before:EnderZoo`.** If other mods' biomes register after GregGuns' `postInit`, a biome list built at that point is incomplete. Check on a pack with Biomes O' Plenty and RWG, and fall back to a late-bound biome lookup if needed.
