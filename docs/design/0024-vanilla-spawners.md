# 0024 · Structure spawners are vanilla mob spawners

- Status: Accepted (owner decision, 2026-10-28)
- Gate: D5 (world presence, [0005](0005-world-presence.md))
- Date: 2026-10-28

## Context

M8 (G11) built its own spawner blocks for the structures: the Zombie Hole (Big Bunker) and the Bug Hole (Alien Bug Nest), each a `BlockHoleSpawner` with a ticking `TileHoleSpawner`, its own textures, item, tooltip and NEI entry ([m8-exit-gate](../gregguns-spec/11-worldgen/m8-exit-gate.md#spawner-blocks-g11)). ICBM (I6) added two more kinds on the same system, the Silo Guard Post and the Silo Elite Post ([silo-guard-spawner](../gregguns-icbm-spec/08-worldgen/silo-guard-spawner.md)). Four invented blocks do what Minecraft's own `minecraft:mob_spawner` does with NBT, and they look like nothing a player knows.

The owner decided to drop every spawner block GregGuns invented and use the real mob spawner with NBT.

## Decision

1. **Hard removal.** `BlockHoleSpawner`, `ItemBlockHoleSpawner`, `TileHoleSpawner`, `HoleSpawnerRegistration`, `HoleSpawning` and `HoleTooltipFormat` are gone, with their lang keys, the two hole textures and their painters and credits, and the NEI rows. Existing blocks are not converted: in a world saved earlier they turn to air. `RetiredContent` lists `zombie_hole`, `bug_hole`, `silo_guard` and `silo_elite`, so Forge drops them with no missing-blocks prompt. The old tile id `gregguns:hole_spawner` is not a registry name; vanilla skips it with one log line when its chunk loads.
2. **Every spawner mark places `minecraft:mob_spawner`** (`VanillaSpawners`, the structure spawner hook, also called by `ComplexApplier`). A pure table, `SpawnerKind`, maps each mark to its NPCs; `SpawnerSettings.STANDARD` holds the vanilla fields; `SpawnerNbt` writes them: `SpawnPotentials` with the kind's registered NPCs at weight 1, `RequiredPlayerRange` 24, `SpawnCount` 3, `MaxNearbyEntities` 5, `SpawnRange` 4, `MinSpawnDelay` 100, `MaxSpawnDelay` 120, `Delay` 40. A kind with no registered NPC places nothing. Every `Properties` and the `SpawnData` carry `GGSpawner: {MinDanger: n}`, the structure's rolled-ceiling danger.
3. **Our NPCs work from a vanilla spawner** ([spawning § Spawner NPCs](../gregguns-spec/10-entities-npcs/spawning.md#spawner-npcs)):
   - vanilla reads `SpawnData` into the entity before it joins the world and skips `onSpawnWithEgg`; the NPC sees `GGSpawner` there and sets its danger (position's, raised to `MinDanger`), takes the Infernal opt-out, and runs its egg setup, which rolls the equipment;
   - vanilla checks `getCanSpawnHere()` before `SpawnData`, and the monster light checks would empty lit rooms; the NPC skips only the light checks when a GregGuns-marked spawner of its type is placing it and the natural-spawn `CheckSpawn` event has not marked it.

## Consequences

- No GregGuns block, item, tile, texture or tooltip for spawners. Players see the vanilla spawner with the NPC spinning inside, and break it like any spawner (it drops nothing).
- Caps follow vanilla: per mob type, not per spawner (a zombie spawner can hold 5 of each of its three zombies), counted within 8 blocks across instead of 12. A cycle that places nothing retries every tick until one try fits, as vanilla does.
- Natural spawning, spawn eggs and plain vanilla spawners keep every vanilla rule; `worldgen.spawner_light` proves it.
- Known risk: a spawner names its entity. If a world's NPCs stop being registered (the `npcs` module turned off in an existing world), the vanilla client renderer fails on a spawner that names a missing entity. Worldgen never writes one for a missing NPC; turning the module off afterwards is the admin's risk, as with any mod's mob in a vanilla spawner.
- Specs: [structure-placement § Spawner marks](../gregguns-spec/11-worldgen/structure-placement.md#spawner-marks), [spawning § Spawner NPCs](../gregguns-spec/10-entities-npcs/spawning.md#spawner-npcs), [infernal-mobs](../gregguns-spec/10-entities-npcs/infernal-mobs.md), [silo-guard-spawner](../gregguns-icbm-spec/08-worldgen/silo-guard-spawner.md); ICBM DECISIONS D268 to D271.

## Alternatives considered

- **Keep the blocks.** Rejected by the owner.
- **Convert old blocks to vanilla spawners on chunk load.** A chunk-load hook for content shipped only in 0.3.x betas; the owner accepted that old holes vanish.
- **No `SpawnData`, rely on `onSpawnWithEgg`.** Vanilla calls it only while no `SpawnData` and no potentials are set, which allows one mob type and no danger floor.
- **Relax the light check for any non-natural caller, or whenever any spawner is near.** Loosens spawns for other mods' spawners and for natural spawns beside a spawner; the marked-spawner test plus the `CheckSpawn` flag keep the change to exactly our spawners.
