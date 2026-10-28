# Silo guard spawner

- **Baseline:** none. Follows M8: [structure-placement](../../gregguns-spec/11-worldgen/structure-placement.md) (spawner marks), now vanilla mob spawners ([design record 0024](../../design/0024-vanilla-spawners.md)).
- **Decisions:** [D36](../DECISIONS.md#d36-in-detail), [D38](../DECISIONS.md#d38-in-detail), D180, D181, D268 to D271
- **Roadmap:** [I6](../../ROADMAP-ICBM.md#i6--world-missile-silos-on-every-planet)

## What a post is

A guard post is the vanilla `minecraft:mob_spawner`, placed by `ComplexApplier` through `VanillaSpawners.place`, with tile NBT that spawns the complex guards. GregGuns adds no block, item or tile for it. The two kinds are `SpawnerKind` constants beside the M8 ones (`ZOMBIE`, `BUG`):

| Kind | Layout tag | NPCs | Where |
|---|---|---|---|
| `SILO_GUARD` | `spawner.silo_guard` | Army Soldier, Commando | `post` and `room` marks, and `room` marks nearer the hall |
| `SILO_ELITE` | `spawner.silo_elite` | Commando, Stormtrooper | `deep` marks, and 40% of `room` marks at depth band 2 or more |

`SpawnPotentials` lists the registered NPCs of the kind with weight 1 each, so vanilla picks evenly, one type per cycle. A kind with no registered NPC this run places no spawner (a spawner must never name a missing entity: the vanilla client draws it).

## Rules (vanilla's, set from `SpawnerSettings.STANDARD`)

| NBT field | Value | Meaning |
|---|---|---|
| `RequiredPlayerRange` | 24 | acts only while a player is closer than 24 blocks, in a loaded chunk |
| `SpawnCount` | 3 | at most 3 tries a cycle |
| `MaxNearbyEntities` | 5 | a cycle stops once 5 of the cycle's mob type stand within 8 blocks across and 4 up or down |
| `SpawnRange` | 4 | spawns within 4 blocks across, one block below to one above |
| `MinSpawnDelay`, `MaxSpawnDelay` | 100, 120 | a cycle every 5 to 6 seconds |
| `Delay` | 40 | the first cycle 2 seconds after a player comes in range |

The cap is per mob type, as vanilla counts: a guard post can hold up to 5 Army Soldiers and 5 Commandos. **Breaking the spawner ends it, and it drops nothing,** as any vanilla spawner.

## Scaling and arms

Every `SpawnPotentials` entry and the `SpawnData` carry `GGSpawner: {MinDanger: n}`, with `n = RolledCeiling.danger(ceiling)` of the complex: EV and IV 3, LuV and ZPM 4, UV and above 5 (`SiloCeiling.guardDanger`, tested equal). Vanilla merges `SpawnData` into the new NPC's NBT and calls `readFromNBT` before the NPC joins the world. The NPC sees the key there and:

1. sets its danger to the larger of its spawn position's danger and `n`;
2. takes the Infernal Mobs opt-out marker (unless `infernal.allowSpawnerNpcs`);
3. runs its egg-style setup, which rolls its equipment at that danger.

The key is never saved back, so a reloaded guard keeps the gear it spawned with. The count per level is in [activity-levels](activity-levels.md).

## Light

Vanilla asks `getCanSpawnHere()` before it applies `SpawnData`, and a monster refuses bright spots, so a lit operational complex would never spawn guards. A GregGuns NPC skips the two light checks only when a GregGuns-marked spawner of its own type is placing it (a spawner within 4 blocks across and 1 up or down whose current entry is that NPC and carries `GGSpawner`), and only when the `CheckSpawn` event has not marked it as a natural-spawn candidate. Peaceful, collision and liquid checks stay. Natural spawning, eggs and plain vanilla spawners keep every vanilla rule ([10-entities-npcs spawning](../../gregguns-spec/10-entities-npcs/spawning.md)).

## Tooltip and texture

None of its own: the post is the vanilla spawner block, with vanilla's name, texture and the spinning NPC inside.

## Requirements

1. GGI-08-40: `spawner.silo_guard` and `spawner.silo_elite` map to their kinds; the mobs are as the table says.
2. GGI-08-41: a placed post spawns guards when a player is near, also in a lit room, and respects the cap.
3. GGI-08-42: breaking a post stops it for good.
4. GGI-08-43: guards start at the ceiling's danger or above, with equipment rolled at it.

## Tests

Unit: `SpawnerKindTest`, `SpawnerSettingsTest`, `SpawnerNbtTest`, `SpawnerDataTest`, `SiloCeilingTest`. Scenarios: `icbm.complex.guards` (both posts, on a pad lit by glowstone, spawn their own mobs through vanilla's own update at the ceiling's danger or above, at most 3 a cycle and 5 a type, at least one armed; a guard targets a nearby fake player; a broken post spawns nothing more, also after its chunk is saved, unloaded and reloaded, while the intact post reloads with its mobs and danger floor and keeps spawning, so the check can tell the two apart); `icbm.complex.levels` (every post placement is a marked spawner of its kind's mobs and floor, and no stray one exists). The base scenarios `worldgen.spawner_*` cover the light rule, the Infernal marker and the client preview.
