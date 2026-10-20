# Blast Engine

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. The shared machinery every legacy blast runs on: lifecycle, the controller entity, worker threads, the two ray casters, the block-edit budget, how blocks are removed (and why nothing drops), entity damage, block updates and events. The three big threaded blasts that sit on this engine are in [threaded-blasts.md](threaded-blasts.md). Individual blast types are in part b of this area. The catalog and config keys are in [explosive-registry.md](explosive-registry.md).

Most of this code is marked `@Deprecated` in the source ("being phased out in favor of `IAction`"). That describes the *API*, not the behavior: every built-in legacy blast still runs through it.

## 1. Identity

| Item | Value |
|---|---|
| Blast base classes | `content/blast/Blast` (extends vanilla `Explosion`; implements `IBlastInit`, `IBlastRestore`), `content/blast/imp/BlastBase` (unused), `content/blast/threaded/BlastThreaded` |
| Helpers | `content/blast/BlastHelpers` (sphere and cube loops), `content/blast/helpers/BlastBlockHelpers` (fluid tests), `content/blast/BlastStatus` (two status constants) |
| Legacy threads | `content/blast/thread/ThreadExplosion`, `ThreadLargeExplosion`, `ThreadSmallExplosion`, `IThreadCallBack`, `BasicResistanceCallBack` |
| Worker pool | `lib/thread/WorkerThread`, `WorkerThreadManager`, `IThreadWork`; adapter `lib/explosive/ThreadWorkBlast` |
| Interfaces | `api/explosion/IBlast`, `IBlastInit`, `IBlastTickable`, `IBlastRestore`, `IBlastMovable`, `IBlastFactory`, `IBlastIgnore`, `api/explosion/redmatter/IBlastVelocity` |
| Capabilities (*1.12-only mechanism*) | `content/blast/caps/CapabilityBlast` (registered, stub: its `doAction` returns `SETUP_ERROR`), `CapabilityBlastVelocity` (stub that never handles motion). Only red matter hands out the real capabilities |
| Controller entity | `content/entity/EntityExplosion` (registry name `holder.explosion`) |
| Related, other sheets | the edit queue (`lib/transform/BlockEditHandler`) and sorter (`lib/transform/PosDistanceSorter`) are documented here because only blasts use them |
| Config | `blast_do_block_updates`, `thread_count`, `debug_threads`, `debug_explosives` (see [explosive-registry.md](explosive-registry.md#5-config-keys-every-blast-config-object)) |

## 2. Two families of blast

1. **Legacy `Blast` subclasses** (this sheet): `BlastTNT`, `BlastShrapnel`, `BlastFire`, gas, sonic, beam (exo/endothermic), mutation, ender, anti-gravitational and the three threaded blasts. A `Blast` *is* a vanilla `Explosion` object so it can be handed to `Block.onBlockExploded`, damage sources and Forge's explosion events. The vanilla `doExplosionA`/`doExplosionB` are overridden to log an error and do nothing.
2. **`IAction` based**: the EMP area action, the smoke-spawn action and the red matter spawn action. They have no lifecycle, no thread, no `Explosion` object. The factory returns the action, `doAction()` runs it once.

`BlastBase` (`content/blast/imp/BlastBase.java`) is a third, newer-looking base that nothing extends (dead code; its `getBlastRadius` returns `min(size, 1)`, almost certainly a typo for `max`; ICBM-02-15).

## 3. Lifecycle of a legacy blast

### Build (`IBlastInit`)

A blast is created by the registry factory (see [explosive-registry.md](explosive-registry.md#explosivedatacreate-the-factory-call)) and configured through chained setters: world, position, size, explosive data, action source, exploder entity. `buildBlast()` posts `BlastBuildEvent` (listeners may change the blast) and then locks it. Every setter afterward throws (`Blast`), so configuration happens before `buildBlast()` only; the one exception is `setEntityController`, which is allowed afterward on purpose.

State flags on a `Blast`: `isAlive` (starts true), `hasBuilt`, `hasSetupBlast`, `callCount` (ticks run).

### Run (`doAction`, server only)

```
doAction():
  if world is client: clientRunBlast() (empty hook); return COMPLETED
  if Forge ExplosionEvent.Start (cancelable) is canceled: return EXPLOSION_CANCELED
  play the generic explosion sound
  if blast is IBlastTickable:
      spawn an EntityExplosion that owns this blast;
      on failure: isAlive = false; return ENTITY_SPAWN_FAILED
      return COMPLETED                      # the entity drives the rest
  else:
      if doFirstSetup() is false: return SETUP_ERROR
      if doExplode(-1) is true: completeBlast()
      return COMPLETED                      # even if the blast is still working
  any exception: log, endBlast(), return UNKNOWN_ERROR
```

- `doFirstSetup()` runs once (guarded by `hasSetupBlast`): registers the blast in the global active list, then calls the subclass `setupBlast()` hook. A false from `setupBlast()` only means "do not continue *this* call"; the flag is already set, so a later call returns true.
- `doExplode(callCount)` is the subclass work. Return true = finished, false = "still going" (a ticking blast) or "handed to a thread". `-1` means "not a ticking blast".
- `completeBlast()` ends the blast (`isAlive` false, removes it from the active list, kills any legacy thread, and kills the controller entity) and then calls `onBlastCompleted()`, which by default ends it again.
- The default play-sound is the vanilla generic explode sound at volume 4.0, pitch `(1 + (r1 - r2) x 0.2) x 0.7` (r1, r2 random 0 to 1). Mutation silences it.

### Ticking blasts (`IBlastTickable`) and the controller entity

`EntityExplosion` (a non-colliding, no-clip entity, 0.98 x 0.98 box, no vanilla movement) is the clock. Each entity tick it:

1. Dies if its blast is not tickable, is not the blast's controller, or is completed.
2. For a movable blast with motion: damps motion by 0.98, normalizes it, clamps speed to 0.5, moves its box, and calls the blast's `onPositionUpdate` (red matter only).
3. Calls `onBlastTick(ticksExisted)`; a true return kills the entity.

`Blast.onBlastTick` is: server only; run `doFirstSetup()` (return false if it says no); if alive, `completed or doExplode(callCount++)` then `completeBlast()` and return true.

Non-movable blasts park the entity at y = -1 and remember `y + 1` as an offset so the entity never occupies the blast's chunk section.

**Persistence.** The entity saves NBT: `blast` (a sub-compound holding `ex_id` = registry name string plus the blast's own `callCount` and `explosionSize`, and `blastExpEntId` = exploder UUID when set) and `blastPosY` (the offset). On load it rebuilds the blast from the registry (so config defaults apply again) and calls the blast's `load`. Nothing about threads, queued edits, or fuse of children is saved. **Non-tickable blasts (nuclear, thermobaric, antimatter, mutation, TNT variants that finish in one call) have no entity and are never saved**: stopping the server mid-blast loses the rest of it.

Client sync: `writeSpawnData` sends the registry name and the offset so the client can rebuild a client-side blast shell (used only for visuals).

### Global active-blast list

`WorkTickingActionHandler` keeps a static list of every blast that passed `doFirstSetup` and has not ended. It exists so admin tools can cancel blasts (`removeNear(world, x, y, z, range)`; a negative range means everywhere) and so a dimension unload can clear its blasts. See bugs ICBM-02-08 and ICBM-02-09.

## 4. Threads

Two independent systems exist. The newer one is used by all `BlastThreaded` blasts; the older one only by sonic and anti-gravitational.

### 4.1 Worker pool (`BlastThreaded`)

`WorkerThreadManager` is created when the server starts (`thread_count` threads, default **1**) and told to stop when the server stops. Each `WorkerThread` is a minimum-priority daemon that loops: run the current task one step; when a task reports it is done call its completion hook and clear it; otherwise poll the queue; if there is nothing to do, sleep 100 ms. `addWork` hands a task to the thread with the shortest queue (the count excludes the task currently running, and the counter is a plain int updated from two threads).

```
BlastThreaded.doExplode(call):                         # main thread, once
    if not started: started = true; pool.addWork(ThreadWorkBlast(this::doRun, this::onWorkerThreadComplete))
    return false                                       # never "finished" here

worker thread:
    doRun(loops, edits)  -> adds BlockPos entries to the edit list; returns false = done
    onWorkerThreadComplete(edits):                     # still on the worker thread
        sort edits by squared distance to the center, nearest first
        server.addScheduledTask( main thread:
            BlockEditHandler.queue(world, edits, destroyBlock)    # or run all at once if skipQueue()
            onPostThreadJoinWorld() )                  # doExplode(-1); onBlastCompleted()
```

Consequences:

- **One worker thread serializes every threaded blast on the server**, including the 2.1-million-position rot sweep after each nuke.
- The calculation reads world blocks from the worker thread. The only guard is `isBlockLoaded` (the code carries a TODO: "find better fix for non main thread loading"). Chunk edits on the main thread during a long calculation can be observed half-done.
- **There is no exception handling in the worker loop.** An exception in a task ends the thread for good; with the default single thread no threaded blast will run until restart (ICBM-02-07).
- A threaded blast's "post" hook (`onBlastCompleted`) runs **right after the edits are queued, not after they are applied** (ICBM-02-11).
- Clearing a threaded blast (`clearBlast`) has no effect on a task already handed to the pool (ICBM-02-09).

### 4.2 Legacy per-blast thread (`ThreadExplosion`)

A `Blast` can start its own thread with `createAndStartThread`. The thread is a minimum-priority daemon that runs `doRun` once, then marks itself complete and tells the blast. Results arrive in a concurrent queue (`addThreadResult`). The blast polls `isThreadCompleted()` on later ticks and applies results. The thread has a cooperative `kill` flag checked in its loops. Two implementations follow.

## 5. Legacy ray casters (MIT-portable)

Permalinks: [ThreadLargeExplosion.java L38-L133](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/thread/ThreadLargeExplosion.java#L38-L133) and [ThreadSmallExplosion.java L24-L91](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/thread/ThreadSmallExplosion.java#L24-L91). Sonic uses the first; anti-gravitational uses the second. The threaded blasts (nuclear, thermobaric) use a fixed copy of the first in their own class (see [threaded-blasts.md](threaded-blasts.md#ray-casting-mit-portable)).

### Large (spherical sweep)

Inputs: radius (int), energy (float), optional resistance callback (default: liquids cost 0.25, everything else `block.getExplosionResistance`, which is `blastResistance / 5` unless the block overrides it).

```
steps = ceil(pi * radius)
for phi_n in 0 .. 2*steps-1:
  for theta_n in 0 .. steps-1:
    power = energy - energy * rand(0..1) / 2           # uniformly 50% to 100% of energy
    yaw   = 2*pi / steps * phi_n                       # NOTE: spans 0 .. 4*pi (two full turns)
    pitch = pi / steps * theta_n                       # 0 .. pi (just short of the pole)
    step vector = 0.5 * (sin(pitch)cos(yaw), cos(pitch), sin(pitch)sin(yaw))
    walk from the center while distance <= radius and power > 0:
        power -= 0.3 * 0.75 * 5  (= 1.125)             # every half-block step, air included
        block = floor(position)
        if block is a new position (see bug):          # compared by reference, always true
            skip if the chunk is not loaded (ADVANCE NOT SKIPPED: see note)
            if block is not air and hardness >= 0:
                power -= resistance(block)
                if power > 0: record the position
        position += step
```

- A position is recorded **once per ray visit**. Rays overlap heavily near the center, so the result list holds many duplicates; the blast applies each harmlessly (air test) but pays for them.
- The "only act once per block" guard compares block-position objects by reference, so it never fires; the resistance of a block is therefore charged for every half-step spent inside it (about twice per straight crossing) (ICBM-02-02).
- Yaw covers 0 to 4 pi, so every ray direction is cast twice with different random power (ICBM-02-03).
- An unloaded chunk makes the loop `continue` *without moving*, so the same unloaded position is retried until the 1.125-per-step cost has drained the ray's power (about 30 to 70 iterations); it ends the ray rather than skipping the chunk.
- Unbreakable blocks (hardness below 0) are skipped without cost: a ray passes through bedrock for free.

### Small (vanilla-style grid)

The old vanilla TNT algorithm: for `radius` samples per axis over the *surface* of a cube (x, y or z at 0 or radius - 1), map each cell to [-1, 1], normalize to a unit direction, cast a ray with power `radius x (0.7 + rand x 0.6)` that steps 0.3 blocks and loses `0.3 x 0.75` per step. A non-air block costs its resistance (`blastResistance / 5`) and an unbreakable block (hardness below 0) **stops the ray immediately**. Every position with power left is recorded (again with duplicates). Anti-gravitational passes radius 30 (default scale), so about 5,000 rays (30^3 - 28^3 shell cells), each worth 21 to 39 power.

## 6. Block-edit budget (MIT-portable)

Permalink: [BlockEditHandler.java L21-L120](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/lib/transform/BlockEditHandler.java#L21-L120).

The edit queue spreads a big list of block changes over many ticks.

- State: per dimension, a first-in-first-out queue of `EditQueue` (a list of positions, a per-position callback, an optional completion callback).
- `maxTickTimePercentage` = **0.7**. The budget is `50 ms x 0.7 = 35 ms` per world tick event.
- On every world tick event for a dimension that has a queue: record the start time; while now is before start + 35 ms and the queue is not empty, take the head `EditQueue` and run `doWork(50)`; remove the head when it is empty.
- `doWork(limit)` pops and applies positions while the count is `<= limit`, so **51 edits per call**, then checks the clock again at the next loop turn. When the last position has been applied it fires the completion callback if one is set.
- The dimension's queue is dropped on world unload.
- Callers supply an already distance-sorted list; the handler never reorders.

Behavior to know:

- The handler subscribes to `WorldTickEvent` **without checking the phase**, so it runs at both start and end of the world tick: the real budget is up to **70 ms per tick** on a server whose tick budget is 50 ms (ICBM-02-06).
- A queue is global per dimension and FIFO. A second blast's edits wait behind the first one's (including its duplicates).
- The callback runs on the main thread; an exception in it escapes the world tick handler.

### Sorting (`PosDistanceSorter`)

Permalink: [PosDistanceSorter.java L25-L47](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/lib/transform/PosDistanceSorter.java#L25-L47). Compares two positions by integer distance to the center (squared, square-root-truncated, or Manhattan; threaded blasts use squared with Y included), ascending, so **blocks near the center are removed first** and the crater opens outward. The center is truncated to ints.

## 7. How blocks are destroyed, and what drops

- Default (`BlastThreaded.destroyBlock`): if the position is not air, call the block's `onBlockExploded(world, pos, blast)`. Vanilla/Forge behavior of that method: `setBlockToAir` (flag 3: block update plus client sync), then `onBlockDestroyedByExplosion`. Blocks may override it: **ICBM's own explosive block triggers itself** (chain reactions) and vanilla TNT spawns primed TNT.
- **No drops.** The explosion drop pass of vanilla `Explosion.doExplosionB` is never run, so no block drops its item, no loot rolls happen, and `canDropFromExplosion` is not consulted. Containers still run their block-break hook when the block is replaced (chests, furnaces and hoppers spill their contents as item entities), which are then subject to the entity damage pass if one follows. Tile entities without such a hook vanish with their contents.
- Antimatter overrides it: any non-air block is replaced by air with flag 3 when `blast_do_block_updates` is true and flag 2 (client sync only, **no neighbor updates**) when false (default). Unbreakable blocks are replaced only when `break_unbreakable` is true. See [threaded-blasts.md](threaded-blasts.md#antimatter).
- Hardness is the breakability test (hardness below 0 = unbreakable); resistance only drains ray energy. A block with hardness 0 and high resistance still stops a ray.
- No `BlockEvent.BreakEvent` and no `ExplosionEvent.Detonate` are posted by the threaded blasts, so claim and protection mods that hook those events cannot veto individual block changes (they may veto the whole blast through `ExplosionEvent.Start`, section 9). `BlastTNT` does post `Detonate` with an empty block list (part b).

### Block updates

- Default destroy: always flag 3 (updates on), whatever the config says.
- `blast_do_block_updates` = false (default): affects antimatter only (ICBM-02-12). Water and lava next to an antimatter crater will not flow until something updates them. The blast mitigates the worst case by first deleting all water and falling blocks in its sphere (see antimatter).
- The rot sweep and any block-state replacement use the replacement data's own flags (`BlockReplacementData.apply`; `06-blocks-items`).

## 8. Entity damage (MIT-portable)

Permalink: [Blast.java L337-L398](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/Blast.java#L337-L398). Used by most legacy blasts (TNT variants use their own copy in part b).

```
damageEntities(radius, power, destroyItems = true):
  entities = all entities in a box of half-size 2 * radius + 1 around the center (ints, truncated)
  for each entity:
      if blast.onDamageEntity(entity) returns true: skip       # per-blast special handling
      if entity is a dropped item and not destroyItems: skip
      d = distance(entity feet position, center) / radius
      if d > 1: skip
      dir = unit vector center -> entity feet position
      density = world.getBlockDensity(center, entity bounding box)  # 0..1 share of unobstructed sight lines
      s = (1 - d) * density
      damage = (int)((s*s + s) / 2 * 8 * power + 1)
      entity.attackEntityFrom(explosion damage source, damage)
      entity.motion += dir * s                                # always, even if damage was reduced to nothing
```

- Damage source is the vanilla explosion damage source for this blast (so armor, blast protection and "caused by" attribution work; the attacker is the exploder entity if it is a living entity).
- The box is larger than the radius (twice plus one), then the exact distance test cuts it down to a sphere.
- `getBlockDensity` is the vanilla shielding test. Full cover gives density 0 and then damage 1 (the `+1`).
- Knockback is `s` blocks per tick of velocity along the outward direction, not scaled by mass or armor. The `IBlastVelocity` capability that lets an entity override motion is consulted **only by red matter**, not by this routine. `IBlastIgnore` ("entity ignores this blast") is likewise only consulted by red matter.
- If an entity is exactly at the center, the direction is 0 / 0 and the entity receives NaN motion (ICBM-02-16).
- Examples with nuclear numbers are in [threaded-blasts.md](threaded-blasts.md#entity-damage-numbers).

## 9. Events fired

| Event | When | Notes |
|---|---|---|
| `BlastBuildEvent` (ICBM, `api/events`) | `buildBlast()` of a legacy `Blast` | Listeners may call setters before the lock. `BlastBase` does not fire it |
| Forge `ExplosionEvent.Start` | start of `doAction` (server) | Cancelable. Canceling returns `EXPLOSION_CANCELED`; the blast was never started so it leaks nothing |
| Forge `ExplosionEvent.Detonate` | `BlastTNT` only, with an empty block list | Part b |
| `BlastEvent.getBlastSource()` | API accessor | Calling it on a `Blast` recurses forever (ICBM-02-01) |
| `MissileEvent.PostLaunch` | from the missile content registry | `03-missiles` |

No event is posted when a blast completes. Mods cannot observe or veto individual block removals or entity hits.

## 10. Helpers

- `BlastHelpers.forEachPosInCube(xs, ys, zs, consumer)`: loops x, y, z from `-size` to `+size` inclusive (a 3 x 3 x 3 cube for size 1), outermost x, innermost z; a false return from the callback stops the loop.
- `forEachPosInRadius(r, consumer)` / `forEachPosInRadiusUntil(r, consumer, stopper)`: loop the cube of `ceil(r)` and keep positions with `x^2 + y^2 + z^2 <= r^2` (a solid sphere of integer offsets). `Until` checks a stop supplier on every cell.
- `BlastBlockHelpers.isFluid(state)` (vanilla liquid or Forge fluid block) and `isFlowingWater(state)` (vanilla liquid with level below 7).
- `BlastStatus`: `blast.setup.error` and `blast.threading` (the second is never returned).

## 11. Persistence and sync summary

| What | Saved? |
|---|---|
| Ticking blast progress (`callCount`, size, exploder UUID) | Yes, in the controller entity's NBT |
| Threaded or one-shot blast in flight | No |
| Queued block edits | No (memory only) |
| Registry IDs | Yes (`explosive_reg.json`) |
| Spawn data to client | Registry name plus y offset; client builds a shell blast for visuals |

## 12. Relationships

- Used by: every `Blast`; the explosive entity and block (`explosive-block.md`); missile warheads, cluster bomblets, grenades (explosive form calls the same factory).
- Uses: `01-core` action framework (`IAction`, `IActionSource`, status constants), `06-blocks-items` replacement data.

## 13. Bugs

ICBM-02-01 to ICBM-02-09, 02-11, 02-12, 02-15, 02-16. The table is in `index.md`.
