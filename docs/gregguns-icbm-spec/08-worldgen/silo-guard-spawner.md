# Silo guard spawner

- **Baseline:** none. Follows M8: [structure-placement](../../gregguns-spec/11-worldgen/structure-placement.md) (spawner marks), now vanilla mob spawners ([design record 0024](../../design/0024-vanilla-spawners.md)).
- **Decisions:** [D36](../DECISIONS.md#d36-in-detail), [D38](../DECISIONS.md#d38-in-detail), D180, D181, D268 to D271, D273 to D275
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
| `MaxNearbyEntities` | 5 | a cycle stops once 5 of the cycle's mob type stand within 8 blocks each way of the spawner (17 across) and 4 up or down |
| `SpawnRange` | 4 | spawns within 4 blocks each way, one block below to one above |
| `MinSpawnDelay`, `MaxSpawnDelay` | 100, 120 | a cycle every 5 to 6 seconds |
| `Delay` | 40 | the first cycle 2 seconds after a player comes in range |

The cap is per mob type, as vanilla counts: a guard post can hold up to 5 Army Soldiers and 5 Commandos.

## Breaking a post

**Breaking the spawner ends it, and it drops nothing.** The pack changes two things about every mob spawner, and GregGuns answers both in its own jar:

- **Hardness (GregTech `harderMobSpawner`, on in GTNH):** every mob spawner has hardness 500, so a post takes a long time to mine by hand (about a minute and a half with a diamond pick), with its guards firing. GregGuns accepts this pack-wide rule. Its own blasts are different: an ICBM blast counts a post on its origin as stone (resistance 6, `BlastGrid.cellResistance`), so a warhead destroys it and the blast carries on behind it (D274). Other spawners keep GregTech's resistance, about 3.6 million.
- **EnderIO's Broken Spawner:** in GTNH, a mob spawner broken by a survival player drops a Broken Spawner of its mob and runs up to 6 extra spawn cycles, and one broken by anything else drops one too. GregGuns puts every NPC on EnderIO's Powered Spawner blacklist by IMC (`EnderIoSpawners`), which EnderIO checks before both, so a post drops no Broken Spawner and bursts no extra guards. Our NPCs also cannot be farmed in a Powered Spawner (D273).

## Scaling and arms

Every `SpawnPotentials` entry and the `SpawnData` carry `GGSpawner: {MinDanger: n, X, Y, Z}`, the post's own block as its origin, with `n = RolledCeiling.danger(ceiling)` of the complex: EV and IV 3, LuV and ZPM 4, UV and above 5 (`SiloCeiling.guardDanger`, tested equal). Vanilla merges `SpawnData` into the new NPC's NBT and calls `readFromNBT` before the NPC joins the world. The NPC sees the key there and:

1. sets its danger to the larger of its spawn position's danger and `n`;
2. takes the Infernal Mobs opt-out marker (unless `infernal.allowSpawnerNpcs`);
3. runs its egg-style setup, which rolls its equipment at that danger.

Steps 1 and 2 apply only while the spawner placing it stands on its origin. A post carried elsewhere with its NBT (a JABBA diamond dolly moves spawners in GTNH) is a plain vanilla spawner of its mobs: the light rules apply, and its NPCs start at danger 0 with their gear rolled at it, so a deep post moved into a lit base spawns nothing, and in the dark only the weakest guards (D275).

The key is never saved back, so a reloaded guard keeps the gear it spawned with. The count per level is in [activity-levels](activity-levels.md).

## Light

Vanilla asks `getCanSpawnHere()` before it applies `SpawnData`, and a monster refuses bright spots, so a lit operational complex would never spawn guards. A GregGuns NPC skips the two light checks only when a GregGuns-marked spawner of its own type is placing it (a spawner within 4 blocks each way and 1 up or down whose current entry is that NPC and carries `GGSpawner` with the spawner's own position as its origin), and only when the `CheckSpawn` event has not marked it as a natural-spawn candidate. Peaceful, collision and liquid checks stay. Natural spawning, eggs and plain vanilla spawners keep every vanilla rule ([10-entities-npcs spawning](../../gregguns-spec/10-entities-npcs/spawning.md)).

## Tooltip and texture

None of its own: the post is the vanilla spawner block, with vanilla's name, texture and the spinning NPC inside.

## Requirements

1. GGI-08-40: `spawner.silo_guard` and `spawner.silo_elite` map to their kinds; the mobs are as the table says.
2. GGI-08-41: a placed post spawns guards when a player is near, also in a lit room, and respects the cap.
3. GGI-08-42: breaking a post stops it for good; it drops no Broken Spawner and releases no extra guards (D273); an ICBM blast destroys a post on its origin (D274).
4. GGI-08-43: guards start at the ceiling's danger or above, with equipment rolled at it.
5. GGI-08-44: a post moved off its origin keeps no light bypass and no danger floor (D275).

## Tests

Unit: `SpawnerKindTest`, `SpawnerSettingsTest`, `SpawnerNbtTest`, `SpawnerDataTest`, `SiloCeilingTest`, `EnderIoSpawnersTest`, `BlastGridTest`. Scenarios: `icbm.complex.guards` (both posts, on a pad lit by glowstone, spawn their own mobs through vanilla's own update at the ceiling's danger or above, at most 3 a cycle and 5 a type, at least one armed; a guard targets a nearby fake player; a broken post spawns nothing more, also after its chunk is saved, unloaded and reloaded, while the intact post reloads with its mobs and danger floor and keeps spawning, so the check can tell the two apart); `icbm.complex.levels` (every post placement is a marked spawner of its kind's mobs and floor, and no stray one exists). `icbm.blast.spawner_post` (a blast destroys a post on its origin and the stone behind it; a plain spawner and a moved post stand with GregTech's resistance). The base scenarios `worldgen.spawner_*` cover the light rule, the Infernal marker, the client preview, a moved spawner (`worldgen.spawner_moved`) and, in the real pack, EnderIO's break handling (`worldgen.spawner_enderio`).
