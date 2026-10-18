# Spawning

Techguns mobs reach the world in five ways:
1. Natural spawning, through the **dummy-replacement spawn manager**.
2. Spawn eggs (`TGMonsterPlacer`).
3. The two **monster-spawner blocks** (`TGSpawnerTileEnt`).
4. **Camp flags** in generated structures, via `NPCUtil.spawnEntity` (the structures belong to [11-worldgen](../11-worldgen/index.md)).
5. The turret base block, for turrets ([09](../09-machines-utility/index.md)).

Source files: [`TGEntities.postInit`](mob-catalog.md#registration-javatechgunstgentitiesjava), `entities/spawn/TGNpcSpawn.java`, `entities/spawn/TGSpawnManager.java`, [`entities/npc/TGDummySpawn.java`](per-mob.md#18-tgdummyspawn-tgdummyspawn), the [`TechgunsEventhandler.onEntityJoinWorld`](../01-core/events.md#techgunseventhandler-forge-bus) hook, `tileentities/TGSpawnerTileEnt.java`, `items/TGMonsterPlacer.java` and `util/NPCUtil.java`.

---

## 1. Natural spawning: the dummy-replacement system

### 1.1 Vanilla hook
In `TGEntities.postInit()`, after all mods have registered their biomes:
1. It builds the list of **every** registered biome that is not tagged `BiomeDictionary.Type.NETHER` or `END`. This includes modded biomes and any biomes of other dimensions.
2. It adds `TGDummySpawn` to those biomes as `EnumCreatureType.monster`, with weight `spawnWeightTGOverworld` and group size 1 to 2.
3. It adds `TGDummySpawn` to every biome tagged NETHER, as monster, with weight `spawnWeightTGNether` and group size 1 to 2.

So vanilla `SpawnerAnimals` decides **whether and where** a Techguns mob appears. The dummy is an `EntityMob`, so normal monster rules apply: darkness check, solid floor, not Peaceful, and the monster mob cap. `TGSpawnManager` then decides **which** mob it becomes.

By default the dummy has 6× a vanilla zombie's weight in the overworld (600 vs 100) and 3× a zombie pigman's in the Nether (300 vs 100).

### 1.2 Replacement
`TechgunsEventhandler.onEntityJoinWorld`, on the server: if the joining entity is a `TGDummySpawn`, call `TGSpawnManager.handleSpawn(world, dummy)` and **cancel** the join. The dummy is never added to the world. Vanilla still calls `onSpawnWithEgg` on the discarded dummy afterward, which has no effect.

`handleSpawn(world, dummy)`:
1. Look up the biome at `(round(x), round(z))`.
2. Compute `danger = distanceDanger + biomeDanger` (range 0 to 5).
3. Sum the weights of every registered `TGNpcSpawn` in danger tables 0 through `danger` whose dimension list contains the current dimension ID. For each spawn, the weight counts only if its biome whitelist is null or contains the biome; otherwise it counts as 0.
4. If the total is 0, print `TotalWeight 0, nothing to spawn???` and stop. This happens in every dimension other than 0 and −1 (bug [NPC-16](index.md#suspected-bugs)).
5. Draw `roll = rnd.nextInt(total)`. Walk the same lists in the same order, accumulating weights. Choose the first spawn whose running total is `>= roll`.
   - This differs from a standard weighted pick. When `roll = 0`, the first dimension-matching entry is chosen **even if its weight is 0**, and every boundary is shifted by one (bug [NPC-14](index.md#suspected-bugs)).
6. Construct the chosen class reflectively with its `(World)` constructor.
   - Position it at the dummy's x, y, z and yaw, with pitch 0.
   - Call `onSpawnByManager(danger)`, which rolls equipment with **difficulty = danger** (0 to 5).
   - Spawn it into the world and mark the dummy dead.
   - Reflection exceptions are printed, and the dummy is marked dead in every case.

A pack of two dummies yields two independently chosen NPCs.

### 1.3 Danger level
**Distance danger.** Horizontal distance from the world spawn point (`w.getSpawnPoint()`) to the dummy. In the Nether the thresholds are halved, but the measurement is against the same spawn coordinates.

| Distance (overworld) | Distance (Nether) | Distance danger |
|---|---|---|
| < [`DistanceSpawnLevel0`](../01-core/config.md#category-npc-spawn) (500) | < 250 | 0 |
| < [`DistanceSpawnLevel1`](../01-core/config.md#category-npc-spawn) (1000) | < 500 | 1 |
| < [`DistanceSpawnLevel2`](../01-core/config.md#category-npc-spawn) (2500) | < 1250 | 2 |
| otherwise | otherwise | 3 |

**Biome danger.** The first matching rule wins:

| BiomeDictionary type | Biome danger |
|---|---|
| NETHER or END | 0 |
| WASTELAND, SANDY, SNOWY, SWAMP or SPOOKY | 2 |
| COLD, DEAD, SPARSE, SAVANNA or DRY | 1 |
| anything else | 0 |

`TGSpawnManager.isInArray` and the commented-out hard-coded biome arrays (overworld land, desert, ice) are leftovers and are not used.

### 1.4 Spawn tables (`TGEntities.postInit`)
`TGSpawnManager.registerSpawn(spawn, dangerLevel)` appends the spawn to table 0 to 5. An out-of-range level prints a message, but in practice `ArrayList.get` would throw first.

`TGNpcSpawn` defaults:
- the dimension list is `[0]`, overworld only, unless one is passed;
- the biome whitelist is null, meaning all biomes.

The class has constructors for (type, weight), (type, weight, biomes…), (type, weight, dims) and (type, weight, dims, biomes…). Only the first and third are used.

| Table (danger level) | Mob | Weight config key | Default | Dimensions |
|---|---|---|---|---|
| 0 | Zombie Farmer | [`SpawnWeightZombieFarmer`](../01-core/config.md#category-npc-spawn) | 200 | 0 |
| 0 | Zombie Miner | [`SpawnWeightZombieMiner`](../01-core/config.md#category-npc-spawn) | 200 | 0 |
| 0 | Zombie Pigman Soldier | [`SpawnWeightZombiePigmanSoldier`](../01-core/config.md#category-npc-spawn) | 100 | −1 |
| 0 | Cyberdemon | [`SpawnWeightCyberDemon`](../01-core/config.md#category-npc-spawn) | 30 | −1 |
| 1 | Zombie Soldier | [`SpawnWeightZombieSoldier`](../01-core/config.md#category-npc-spawn) | 100 | 0 |
| 1 | Skeleton Soldier | [`SpawnWeightSkeletonSoldier`](../01-core/config.md#category-npc-spawn) | 100 | 0 |
| 1 | Psycho Steve | [`SpawnWeightPsychoSteve`](../01-core/config.md#category-npc-spawn) | 3 | 0 |
| 2 | Bandit | [`SpawnWeightBandit`](../01-core/config.md#category-npc-spawn) | 50 | 0 |
| 3–5 | *(empty)* | | | |

Iteration order, which matters for the off-by-one pick, is table 0 then 1 then 2, in the order the rows are listed above. The first overworld entry is therefore Zombie Farmer, and the first Nether entry is Zombie Pigman Soldier.

All NPC spawn options live in config category `"NPC Spawn"` ([`TGConfig`](../01-core/config.md)):

| Key | Default | Range | Comment text |
|---|---|---|---|
| `Techguns Spawnweight Overworld` | 600 | 0–10000 | "Spawn weigth of Techguns NPCs, determines how many TG npcs spawn" |
| `Techguns Spawnweight NEther` | 300 | 0–10000 | "Spawn weigth of Techguns NPCs in the Nether, determines how many TG npcs spawn" |
| `SpawnWeightZombieSoldier` | 100 | 0–10000 | "…at 0 spawn will not be registered" |
| `SpawnWeightZombieFarmer` | 200 | 0–10000 | same |
| `SpawnWeightZombieMiner` | 200 | 0–10000 | same |
| `SpawnWeightZombiePigmanSoldier` | 100 | 0–10000 | "(Nether only)…" |
| `SpawnWeightCyberDemon` | 30 | 0–10000 | "(Nether only)…" |
| `SpawnWeightBandit` | 50 | 0–10000 | "Spawn weight for spawning Bandit groups…" |
| `SpawnWeightSkeletonSoldier` | 100 | 0–10000 | same |
| `SpawnWeightPsychoSteve` | 3 | 0–10000 | "…early game boss, don't set to high value…" |
| `DistanceSpawnLevel0` | 500 | 0–MAX_INT | "Up to which distance to worldspawn only mobs with danger level up to 0 will spawn" |
| `DistanceSpawnLevel1` | 1000 | 0–MAX_INT | "…up to 1…" |
| `DistanceSpawnLevel2` | 2500 | 0–MAX_INT | "…up to 2…" |

Despite the "will not be registered" wording, a weight of 0 **is** still registered. Combined with the off-by-one pick, a zero-weight mob can still spawn (bug [NPC-15](index.md#suspected-bugs)).

Related damage config, owned by [01-core](../01-core/index.md) but affecting NPCs (category `"Damage Factors"`):
- [`DamageTurretToPlayer`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (0.5): turret damage to players.
- [`DamageFactorNPC`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (1.0): damage from other NPCs to players. Helicopter bullets have no shooter and also count as NPC damage.
- [`ArmorPenetrationMultiplierNPCtoPlayer`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (1.0): multiplier on NPC armor penetration against players.

### 1.5 Which mobs never spawn naturally
Army Soldier, Commando, Outcast, Stormtrooper, Dictator Dave, the three Super Mutants, Alien Bug and Attack Helicopter. They come only from eggs, spawner blocks or camp flags.

---

## 2. Spawn eggs (`TGMonsterPlacer`)

Registration, colors and names are in [mob-catalog.md](mob-catalog.md#spawn-eggs-javatechgunsitemstgmonsterplacerjava). The egg extends vanilla `ItemMonsterPlacer` but ignores its entity-ID metadata.

**Use on a block** (`onItemUse`, server only):
- The target is the clicked block offset one step toward the clicked face.
- If the top face of a block with render type 11 (fences) was clicked, add 0.5 to Y.
- Spawn the entity at the block center (x + 0.5, z + 0.5).
- If the egg has a custom display name, apply it as the entity's name tag.
- In survival, consume one egg.
- Clicking a vanilla mob spawner does nothing special.

**Right-click in air or liquid** (`onItemRightClick`, server only):
- Ray-trace including liquids.
- The block must be minable and editable by the player.
- If the hit block is a `BlockLiquid`, spawn **at the block's integer corner** (no +0.5; bug [NPC-20](index.md#suspected-bugs)). Name tag and consumption work as above.

**`spawnEntity(world, x, y, z)`:**
1. Build `Techguns.<Name>` through `EntityList`.
2. Set its location with a random yaw in 0 to 360°.
3. Spawn it, then call `onSpawnWithEgg(null)`. For a GenericNPC this rolls equipment with a random difficulty 0 to 3.
4. Call `playLivingSound()`.

If the name is not registered it prints `Entity not found <name>`, but returns the **previously** spawned entity, because the result is held in a shared item field (bug [NPC-18](index.md#suspected-bugs)).

---

## 3. Monster-spawner blocks (`TGSpawnerTileEnt`)

`TGSpawnerTileEnt` extends vanilla `TileEntityMobSpawner` (registered as tile entity `TGSpawnerTileEnt`) and adds one helper:

`setValues(entityName, delay, minSpawnDelay, maxSpawnDelay, spawnCount, maxNearbyEntities, requiredPlayerRange, spawnRange)` writes the TE to NBT, overwrites these vanilla spawner keys, and reads the result back:

| NBT key | Type |
|---|---|
| `EntityId` | string |
| `Delay` | short |
| `MinSpawnDelay` | short |
| `MaxSpawnDelay` | short |
| `SpawnCount` | short |
| `MaxNearbyEntities` | short |
| `RequiredPlayerRange` | short |
| `SpawnRange` | short |

The Javadoc lists the vanilla defaults: 20 / 200 / 800 / 4 / 6 / 16 / 4.

The block [`BlockTGMonsterSpawner`](../07-blocks/special-blocks.md#monster-spawner-holes-blocktgmonsterspawner) ([07-blocks](../07-blocks/index.md)) creates one per metadata:

| Meta | Block name | EntityId | Delay | Min delay | Max delay | Count | Max nearby | Player range | Spawn range |
|---|---|---|---|---|---|---|---|---|---|
| 0 | Bug Hole | [`Techguns.AlienBug`](../01-core/lifecycle-and-registration.md#lifecycle-overview) | 20 | 100 | 300 | 3 | 5 | 24 | 3 |
| 1 | Zombie Hole | `Techguns.ZombieSoldier` | 20 | 100 | 300 | 3 | 5 | 24 | 3 |

The rest is vanilla spawner behavior:
- Active while a player is within 24 blocks.
- Every 100 to 300 ticks it tries 3 spawns within ±3 blocks horizontally (Y −1 to +1), unless 5 or more of the same class are nearby.
- Each candidate must pass `getCanSpawnHere`. Zombie Soldiers therefore need darkness; Alien Bugs accept any light level.
- Spawned mobs get `onSpawnWithEgg`, so GenericNPC equipment uses a random difficulty 0 to 3.
- Vanilla flame and smoke particles appear client-side.

---

## 4. Camp flags and `NPCUtil`

`NPCUtil.spawnEntity(world, x, y, z, name)` does the same as the egg's `spawnEntity`: it builds `Techguns.<name>`, sets a random yaw, spawns it, calls `onSpawnWithEgg(null)` and plays the living sound. It is server-only and returns the entity or null.

`NPCUtil.EntityByNameSelector(names)` is an `IEntitySelector` that accepts living `EntityLivingBase` entities whose `EntityList.classToStringMapping` name is in `names`.

The camp flag ([`CampFlagTileEnt`](../09-machines-utility/shared-rendering.md#1-basicmachine-metas-owned-by-area-09), [`BasicMachine`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) meta 7, owned by [09](../09-machines-utility/index.md) and [11](../11-worldgen/index.md)) uses both:
- Every 60 ticks it counts NPCs from its list inside the camp box.
- If the count is below `maxNPCs`, it spawns a random list entry at a random spawn point every 200 ticks.

The spawn lists are:
- Military Camp: `{"ArmySoldier"}`.
- Aircraft Carrier: `{"ArmySoldier", "Commando"}`, with max 9.

**The counting never matches.** FML stores the full name `Techguns.ArmySoldier` in `classToStringMapping`, but the selector is given the short name [`ArmySoldier`](per-mob.md#6-army-soldier-armysoldier). The count is therefore always 0, and camps keep spawning a soldier every 200 ticks without limit while loaded (bug [NPC-17](index.md#suspected-bugs)).
