# 02-explosives, part a: the engine

This file holds the Contents, Coverage, Known limits and Suspected bugs sections for part a of area `02-explosives` (registry, blast engine, threaded blasts, explosive block). The orchestrator merges it into `index.md` with part b (the individual blast types). Source: ICBM-Classic `prod/1.12` @ `9c1d985a`; cite classes relative to `src/main/java/icbm/classic/`.

## Contents

- [explosive-registry.md](explosive-registry.md): the master catalog (ID, name, tier, size, fuse of all 26 registered explosives plus the two dynamic ones), how a tier switches the block, grenade, cart and missile forms on, registry mechanics and the ID file, the compact block-recipe table, **every per-blast config key and default in one table**, the fuse-time table, lang.
- [blast-engine.md](blast-engine.md): blast build and lifecycle, the controller entity, the worker pool and legacy threads, the two legacy ray casters (MIT-portable), the block-edit budget (MIT-portable), how blocks are removed and why nothing drops, entity damage (MIT-portable), block-update policy, events.
- [threaded-blasts.md](threaded-blasts.md): nuclear, thermobaric and antimatter: shapes, ray counts and energy budgets (MIT-portable), the timeline from trigger to crater, antimatter's feathered sphere (MIT-portable), the rot and mutation sub-blasts, entity damage numbers, visuals and audio.
- [explosive-block.md](explosive-block.md): the explosive block, item and tile, every trigger, the primed entity and its fuse, persistence, textures.

## Key facts

- Tier is visual. The only thing it does is choose content forms: tiers 1 to 4 get a block, cart and missile; **only tier 1 gets a grenade**; tier NONE gets nothing (hypersonic, missile module, rot, mutation).
- The explosive identity is an integer in the item damage value. IDs 0 to 26 are fixed (12 and 14 unused); `colors` and `smoke` take the next free IDs, which on a fresh registry are 12 and 14.
- All legacy blasts share one lifecycle. Ticking blasts are driven by an `EntityExplosion`. Nuclear, thermobaric and antimatter are not ticking: they have no entity, are not saved, and run on **one** worker thread by default.
- Nuclear and thermobaric are 50,000 and 18,000 energy rays. Every ray pays 1.125 per half block even in air, so a nuclear blast with `scale` 50 and `energy` 80 reaches at most about 36 blocks. Stone costs about 4.25 per block, obsidian stops everything.
- Blocks are removed with `onBlockExploded` (flag 3), with no drops. Explosive blocks and TNT chain. Removal is spread over ticks by an edit queue (35 ms budget, nearest first), and because the queue handler runs on both tick phases the real budget is 70 ms.
- The post-blast effects (second damage wave, rot, mutation, second sound) fire when the crater is *queued*, not when it is finished.
- Entity damage is `(s^2 + s) / 2 x 8 x power + 1` with `s = (1 - d) x shielding`. With the default power 80,000 any entity within 50 m that is not fully enclosed dies.
- Antimatter: radius 55, damage 2^31 - 1, deletes bedrock by default, kills all red matter within a 221-block box, and with the default config does nothing else when it finds one.
- The explosive block stores nothing in metadata but its facing. The explosive is in the tile's stack. Triggers: redstone, flint and steel or fire charge, a burning arrow, any explosion (chain reaction), and adjacent fire or lava at placement. Breaching skips the entity and fires instantly into the block it is placed on.
- Capabilities (`CapabilityBlast*`), the registry events, blockstates, the `explosive` block property, datafixers and Forge `@Config` are *1.12-only mechanisms*.

## Related areas

| Area | Why |
|---|---|
| `01-core` | The action framework (`IAction`, `IActionSource`, statuses), the explosive capability classes, `NBTConstants`, `ICBMClassicAPI` registries, `ConfigThread`/`ConfigDebug` |
| `02-explosives` part b | The individual blasts (TNT, shrapnel, fire, gas, sonic, breach, beam, ender, anti-gravitational), mutation and rot details, radioactive blocks, grenades |
| `03-missiles` | The missile form of each explosive; EMP and warhead triggers |
| `05-detection-defense` | EMP action and the `emp` explosive; defuser and detonators |
| `06-blocks-items` | Recipes, grenade, cart and missile items, block replacement data |
| `07-visuals-audio` | Full texture, model and sound catalog |
| red matter (later) | `ActionSpawnRedmatter`, `EntityRedmatter`; antimatter kills it |

## Coverage

Source files read in full: **56 of 56** in scope for part a.
- `content/blast/`: `Blast`, `BlastHelpers`, `BlastStatus` (3); `caps/` 2 of 2; `helpers/` 1 of 1; `imp/` 1 of 1; `thread/` 5 of 5; `threaded/` 4 of 4.
- `lib/explosive/` `ThreadWorkBlast` and `reg/` 7 of 7; `lib/thread/` 3 of 3; `api/explosion/` 8 of 8 (including `redmatter/IBlastVelocity`).
- `config/blast/` 16 of 16 (`ConfigBlast`, `ConfigFuseTimes`, `ConfigThermobaric`, and 13 in `types/`).
- `content/reg/ExplosiveInit` 1 of 1; `content/blocks/explosive/` 4 of 4.

Read to resolve references, in full: `content/entity/EntityExplosion`, `lib/actions/WorkTickingActionHandler`, `lib/transform/BlockEditHandler`, `lib/transform/PosDistanceSorter`, `config/ConfigThread`, `prefab/tile/BlockICBM`, `prefab/item/ItemBlockAbstract`, `lib/capability/ex/CapabilityExplosiveStack`, `content/blast/BlastRadioactiveBlockSwaps`, `content/blast/BlastMutation`, `api/EnumTier`, `api/events/BlastBuildEvent`. Read in part: `content/entity/EntityExplosive` (fuse, tick and NBT; lines 28 to 200), `ICBMClassic` (registry setup and thread start only), `api/events/BlastEvent`, `api/reg/IExplosiveData`, `lib/NBTConstants` and `config/ConfigDebug` (keys), `client/ICBMSounds` (enum entries).

Assets at catalog depth: block textures `textures/blocks/explosive/` (31 files; sizes measured, all 16 x 16 except the two animated 16 x 320 strips, `antimatter` and `redmatter_top`); blockstates `explosives.json` and `explosives/` (23 files, read for texture mapping); sounds `explosion1.ogg`, `antimatter.ogg` (file sizes only); `recipes/explosives/` (all block recipes, summarized). Lang keys in all sheets.

Not covered here: `BlastTNT`, `BlastShrapnel`, `BlastFire`, gas, sonic, breach, beam, ender, anti-gravitational (part b); red matter; the radioactive block swap table and `BlockReplacementData`; `EntityExplosive` beyond the lines listed; `CapabilityExplosiveEntity`; the explosive renderers and item models (`ClientReg`, `RenderExBlock`, model mappers); the grenade, bomb cart and missile item classes; cart/grenade/missile recipes.

## Known limits

- No code was run. Every number (ray counts, energy reach, damage tables, edit counts, timings) comes from reading the source and arithmetic on the config defaults.
- The exact on-disk spelling of Forge config keys for fields without `@Config.Name` (the fuse times, nuclear, thermobaric) was not verified in-game; the Java field names are given.
- ICBM-02-22 (wrong item dropped when an explosive block is mined) is inferred from the block state having no explosive stored; it was not confirmed in-game and no override of `getDrops` was found.
- The burning-arrow trigger depends on whether a stuck arrow reaches `onEntityCollidedWithBlock`; not verified.
- Whether the remote detonator, laser detonator and defuser act on the block, the entity or the capability was not checked here (see `05-detection-defense`).
- The worker-thread safety claims (reading chunks off-thread) follow the code's own TODO and vanilla 1.12 behavior; no crash was reproduced.
- Sounds and textures were not seen or heard. Sound durations and sample rates were not measured.
- Forge `ExplosionEvent.Start` cancels the blast but a mod that cancels it receives `EXPLOSION_CANCELED` through the action status; how callers handle that status is in `01-core` and part b.

## Suspected bugs

"Logic" means we could inherit it in a rebuild. "1.12-only" means it does not matter to a 1.7.10 rebuild. IDs 40 and above in this area belong to part b.

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-02-01 | `getBlastSource` recurses forever (logic) | `content/blast/Blast.java:469-473` | The override returns a call to itself. Any listener that calls `BlastEvent.getBlastSource()` on a legacy blast overflows the stack | crash |
| ICBM-02-02 | "Once per block" test never fires in the legacy ray caster (logic) | `content/blast/thread/ThreadLargeExplosion.java:80,92,116` | `prevPos != blockPos` compares two `BlockPos` objects by reference, so it is always true and a block's resistance is charged at every half-step inside it, about twice a straight crossing. The nuclear and thermobaric copies compare by value. Sonic uses this caster | gameplay |
| ICBM-02-03 | Legacy ray caster casts every direction twice (logic) | `ThreadLargeExplosion.java:59,67` | `phi_n` runs to `2 x steps` but the yaw increment is `2 pi / steps`, so yaw spans 0 to 4 pi. Twice the work and double the hit chances | gameplay |
| ICBM-02-04 | Edit list is never deduplicated (logic) | `BlastNuclear.java:98-100`, `BlastThermobaric.java:96-98`, `ThreadLargeExplosion.java:107-111`, `ThreadSmallExplosion.java:78-81`, `BlastThreaded.java:61-85` | Each ray adds every block it destroys. Near the center thousands of rays add the same block. The sorted list, the queue and the per-edit lookup all carry the duplicates, and the rot sweep queues behind them | gameplay |
| ICBM-02-05 | Configured radius exceeds what the energy can reach (logic) | `BlastNuclear.java:78`, `BlastThermobaric.java:76`; `ConfigNuclear.scale` 50, `energy` 80 | Every half-step costs 1.125 even in air, so the longest ray (power 80) stops at 35.6 m and the average at about 27 m. `scale` above about 36 does nothing and a thermobaric ray reaches its 30 m radius only 31% of the time. `scale` and `energy` must be tuned together | gameplay |
| ICBM-02-06 | Edit queue runs twice per tick (logic and 1.12-only) | `lib/transform/BlockEditHandler.java:37-44` | The world-tick handler has no phase check, so it runs at start and end of every tick. The 35 ms budget doubles to 70 ms per tick, more than the 50 ms tick. Batch size is 51, not 50 (`<= limit`) | gameplay |
| ICBM-02-07 | One exception kills a worker thread for good (logic) | `lib/thread/WorkerThread.java:38-78` | The run loop has no try/catch around `doRun` or `onCompleted`. An exception (for example from reading a chunk on the wrong thread) ends the thread. With the default `thread_count` of 1 every later threaded blast is queued and never runs | crash |
| ICBM-02-08 | World unload throws while clearing blasts (logic) | `lib/actions/WorkTickingActionHandler.java:37-47` with `Blast.java:654-666` | The unload handler streams over the active-blast list and `clearBlast` removes each blast from that same list during the loop, a concurrent modification | crash |
| ICBM-02-09 | Clearing a threaded blast does not stop it (logic) | `Blast.java:654-666`, `BlastThreaded.java:28-31,95-100` | `endBlast` only kills a legacy `ThreadExplosion`. A task already given to the worker pool has no cancel flag, so after `/icbm remove` or a dimension unload the thread still finishes and the scheduled task still queues the edits | gameplay |
| ICBM-02-10 | Antimatter's red matter rule works through an error path and leaks the blast (logic) | `BlastAntimatter.java:28-33,156-168`, `Blast.java:142-145,209-219` | With `damage_on_redmatter_kill` false, finding a red matter makes the start-up pass return false, which `doAction` turns into `SETUP_ERROR`. The blast is already registered as active and is never ended, so it stays in the active list forever | gameplay |
| ICBM-02-11 | Post-blast effects fire at queue time (logic) | `BlastThreaded.java:61-85`, `BlastNuclear.java:188-218`, `BlastAntimatter.java:100-124,148-153` | `onPostThreadJoinWorld` runs right after `BlockEditHandler.queue`, not when the queue drains. The second damage wave, the rot and mutation blasts and the second sound happen before the crater is dug, so the "after" pass hits entities that are about to be in the crater and the intended order is lost | gameplay |
| ICBM-02-12 | `blast_do_block_updates` only affects antimatter (logic) | `config/blast/ConfigBlast.java:105`, `BlastAntimatter.java:43` | The key claims to control "the big explosions". The default block removal always uses flag 3 (`onBlockExploded`). Only antimatter reads the flag, and with the default false it removes blocks without neighbor updates | gameplay |
| ICBM-02-13 | Antimatter clears all fluids and falling blocks in one tick (logic) | `BlastAntimatter.java:84-105` | The "remove first" list is applied directly with `setBlockState` in a single main-thread task, bypassing the edit budget. A 55 m sphere over water can be hundreds of thousands of writes in one tick | gameplay |
| ICBM-02-14 | Mutation blast never completes (logic) | `content/blast/BlastMutation.java:51-65`, `Blast.java:148-151` | `doExplode` always returns false and the blast is not ticking, so `completeBlast` is never called and the instance stays in the active-blast list. Every nuclear detonation leaks one | cosmetic |
| ICBM-02-15 | `BlastBase` radius is clamped the wrong way and the class is dead (logic) | `content/blast/imp/BlastBase.java:54-58` | `min(size, 1)` instead of `max`. No class extends `BlastBase`, so it is harmless today | cosmetic |
| ICBM-02-16 | Entity at the exact blast center gets NaN motion (logic) | `Blast.java:371-389` | The direction is computed by dividing the offset by its length with no zero check. A zero offset gives 0 / 0 and the entity receives NaN velocity | gameplay |
| ICBM-02-17 | Ender target listeners are dead (logic) | `lib/explosive/reg/ExMissileContentReg.java:43-66`, `ExBlockContentReg.java:87`, `content/reg/ExplosiveInit.java:235-236` | `setInteractionListener` is an empty method and `onInteraction` returns false. The block activation callbacks are stored but nothing in `BlockExplosive` ever looks them up. A player cannot set an ender explosive's target by using a GPS tool on a block or missile; only an item stack already carrying the customization works | gameplay |
| ICBM-02-18 | Dynamic IDs reuse the retired slots (logic) | `ExplosiveInit.java:267-268`, `ExplosiveRegistry.java:69-83` | `colors` and `smoke` register with ID -1 and take the first free integers. A fresh registry file gives them 12 and 14, the old Regen and S-Mine IDs, so a world or pack that predates them can show old stacks as colors or smoke | gameplay |
| ICBM-02-19 | Red matter tooltip keys do not match the lang file (logic) | `content/blocks/explosive/ItemBlockExplosive.java:56-65,138-146`, `en_us.lang:366-377` | The code asks for `icbm.explosive.redMatter.info...`; the file has `explosive.icbmclassic:redMatter.info...`. The branch is also only reachable when the explosive is not registered, so the 11 joke lines never show | cosmetic |
| ICBM-02-20 | Fuse saved as a byte (logic) | `content/entity/EntityExplosive.java:117,127` | Written with `setByte`. The antimatter default of 300 reloads as 44 and any fuse above 127 can reload negative, which means "explode now". Spawn data uses a full int, so only a save and load is affected | gameplay |
| ICBM-02-21 | Nuclear and thermobaric cloud is spawned on the server (logic) | `BlastNuclear.java:132-154,169-183`, `BlastThermobaric.java:130-152,167-183` | Particle spawning on the server world does nothing, and the client ring in `doExplode` can never run (blasts tick on the server only). No cloud is ever visible | visual |
| ICBM-02-22 | Mined explosive may drop the wrong type (logic, unverified) | `content/blocks/explosive/BlockExplosive.java:52-55`, `prefab/tile/BlockICBM.java:50-54` | `damageDropped` reads the `explosive` property from the raw block state, but only the facing is stored in the world, so the property is the registry's default. Pick-block uses the actual state and is correct. Breaking the block by hand therefore likely drops the default explosive and drops no customizations | gameplay |
| ICBM-02-23 | Tile customizations are lost (logic) | `TileEntityExplosive.java:56-63`, `lib/capability/ex/CapabilityExplosiveStack.java:66-79` | `toStack` copies the stack only ("TODO save customizations") and the tile saves only the stack. A customization added to the tile (the dead ender binding) vanishes on save and never reaches the primed entity | gameplay |
| ICBM-02-24 | Registry soft spots (logic) | `ExplosiveContentRegistry.java:55`, `ExMinecartContentReg.java`/`ExMissileContentReg.java` (`getDeviceStack`), `ExplosiveRegistry.java:134-141` | `enableContent` tests a data object against a set of names (never matches, harmless); enabling an unknown name crashes at lock time; the cart and missile `getDeviceStack` dereference a possibly-null lookup; an unknown name silently becomes condensed | cosmetic |
| ICBM-02-25 | Worker queue counter is unsynchronized (logic) | `lib/thread/WorkerThread.java:80-86,99-103`, `WorkerThreadManager.java:26-45` | `workCount` is a plain int incremented on the main thread and decremented on the worker. It can drift and mis-balance the "least busy" choice. It also excludes the running task | cosmetic |
| ICBM-02-26 | Antimatter kills red matter across a 221-block box (logic) | `BlastAntimatter.java:156-168` | The damage radius passed in is already doubled (110), and the entity search doubles it again (box half-size 221). The per-entity hook that kills red matter runs before any distance test, so every red matter in the box dies, not just those inside the 110 m damage radius | gameplay |
| ICBM-02-27 | Blast center is shifted half a block (logic) | `content/entity/EntityExplosive.java:105-109` with `TileEntityExplosive.java:98` | The entity is spawned at the block center (+0.5) and `explode` adds 0.5 to each axis again, so the blast is centered up to 0.5 blocks past the block center in x, y and z | cosmetic |

Minor, not given IDs: `Blast.getEntities` truncates its box corners to ints; `ConfigRedmatter.REDMATTER_MOVEMENT` has its annotation commented out; `ICBMClassicAPI.BLAST_CAPABILITY` and `BLAST_VELOCITY_CAPABILITY` are registered with stub implementations; the `HYPERSONIC` fuse keys are never read; the `config.icbmclassic:blast.*` lang titles for most groups are not in `en_us.lang` (only `blast.title` and the `fusetimes` titles were found); `BlastStatus.TRIGGERED_THREADING` is never used.
