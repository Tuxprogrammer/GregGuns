# Targeting and Tracking

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Three jobs live here: **target data** (what a missile is aiming at), **SAM target selection and interception**, and the **missile tracker** (taking missiles out of the world when they leave loaded chunks, and putting them back above the target). Flight math is [flight-logic.md](flight-logic.md); the entities are [missile-entity.md](missile-entity.md). The radar map and the radar's SAM trigger are 05; launchers, which write the target data, are 04.

Capabilities, `WorldSavedData`, `ForgeChunkManager`, the event bus and `@Mod.EventBusSubscriber` are *1.12-only mechanisms* (1.7.10 has equivalents for most of them; the behavior is what matters).

## 1. Identity

| Class | Registry key / role |
|---|---|
| `api/missiles/parts/IMissileTarget` | Interface: `getPosition()`, `isValid()`, `getX/Y/Z()`; buildable (can be saved and rebuilt) |
| `api/missiles/parts/IMissileTargetDelayed` | Adds `getFiringDelay()` and `cloneWithoutDelay()`; the launcher adds the delay to its own firing delay |
| `content/missile/logic/targeting/BasicTargetData` | `icbmclassic:basic`: a fixed position and an optional firing delay |
| `content/missile/logic/targeting/BallisticTargetingData` | `icbmclassic:ballistic`: a fixed position plus an air-burst height |
| `content/missile/entity/anti/SAMTargetData` | `icbmclassic:anti.missile`: a live entity target chosen by scanning; registered only to reserve the name (its builder returns null) |
| `content/missile/logic/TargetRangeDet` | Air-burst fuse for the explosive missile |
| `content/missile/logic/source/ActionSource`, `.../cause/*` | Who launched the missile: `ActionSource` (world, position, cause); causes `EntityCause` (`icbmclassic:entity`), `CausedByBlock` (`icbmclassic:block`), `RedstoneCause` (`icbmclassic:block.redstone`) |
| `content/missile/tracker/MissileTrackerHandler`, `MissileTrackerWorld`, `MissileTrackerData`, `LoadedChunkPair` | Simulation and chunk loading |
| `content/missile/MissileEventHandler` | Mount cancel hook and chunk-unload simulation |

## 2. Obtaining

Not items. The launcher builds `BallisticTargetingData(target, 2)` for every missile it fires (including SAMs). The cruise launcher passes its own target. The rocket launcher builds `BasicTargetData` from a ray trace (part b). The SAM entity creates its own `SAMTargetData` in the constructor.

## 3. Numbers

| Name | Value | Where |
|---|---|---|
| Air-burst height (launcher) | 2 blocks | launcher (`BallisticTargetingData(target, 2)`) |
| Air-burst height (cluster explosive, non-ballistic targets) | 10 blocks, but **dead code**: the explosive it compares against (`ICBMExplosives.CLUSTER`) is never assigned and stays null, and the entity never reports null data (bug 28) | `TargetRangeDet` constant |
| SAM scan delay | 10 ticks between scans while its queue is empty | `SAMTargetData.SCAN_DELAY` |
| SAM queue size | 5 | `MAX_TARGETS` |
| SAM `target_range` | 30 blocks | `ConfigSAMMissile` |
| SAM `use_radar_only` | false | `ConfigSAMMissile` |
| SAM kill radius | `sam.speed` = 4 blocks | `EntitySurfaceToAirMissile` |
| Tracker step | every 21 ticks (`ticks++ >= 20`) | `MissileTrackerWorld.onWorldTick` |
| `simulation_flight_speed` | 10 | config (misnamed: it is blocks per tracker step, about 9.5 blocks/s; bug 31) |
| `simulation_exit_height` | 500 | config |
| `simulation_enter_height` | 450 | config |
| `simulation_enter_speed` | 1 block/tick | config |
| Chunk hold time | 60 steps (about 63 s) | `unloadChunkCooldown` |
| Pre-load wait | 5 steps (about 5 s) | `preLoadChunkTimer` |
| Chunks forced per landing | 5 (target chunk and its four edge neighbors) | `onWorldTick` |

## 4. Behavior

### Target data

**`BasicTargetData`.** Position (`Vec3d`) and `firingDelay` (ticks). Built from x, y, z; a `Vec3d`; or a `BlockPos` (block center, +0.5 on each axis). `cloneWithoutDelay` copies the position only. `getX/Y/Z` return NaN when there is no position. `isValid()` is intended to mean "has a finite position" but is written so it can never be true (bug 26); nothing in this area calls it. `equals` compares positions only (no `hashCode`).

**`BallisticTargetingData`.** A basic target plus `impactHeightOffset` (a double, but the constructor takes an int). It exists only to tell the air-burst fuse how close to detonate.

**`SAMTargetData`.** Not a stored position: each coordinate getter calls `getTarget()` and returns the live entity's feet position, or 0 if there is none.

**NBT.** Basic: `x`, `y`, `z` (double), `firingDelay` (int); writing with a null position throws (bug 27). Ballistic adds `impact_height` (double).

### Air-burst fuse (`TargetRangeDet`, explosive missile only, server)

Runs at the start of every tick of an `EntityExplosiveMissile`, including while it sits on the pad.

```
if no target data: return
offset = target.impactHeightOffset   if the target is ballistic
         10                           else if the explosive is the cluster explosive
         0                            otherwise
if offset > 0 and |dx| <= offset and |dz| <= offset:
    if sqrt(dx^2 + dy^2 + dz^2) <= offset:
        run the explosive at the missile position (cause: the missile), then setDead()
```
with `d` = target minus missile. So a launcher missile detonates when it comes within 2 blocks of the (inaccuracy-shifted) target point, in the air or on the pad (bug 28). Because the explosion is at the missile and not at the target, and the descent speed is up to about 3.9 blocks/tick, a missile that is more than about 0.5 block off the target line can step past the 2-block ball between two ticks (the chance grows with the offset) and then detonate on ground impact instead.

### Sources and causes

Every launch records an `ActionSource` in the missile (`missile.source`). It is read by the blast code to blame a player or a block (02 and 04). Causes chain through `parent`. `EntityCause` keeps the entity only as a runtime reference; after a reload only `name`, `uuid`, `player` remain. `CausedByBlock` keeps the world as a dimension id and re-resolves it lazily.

### SAM target selection (server, from `EntitySurfaceToAirMissile.onUpdate`)

```
every tick:   scanLogic.tick()
tick():       if delay < 10: delay++
              else if queue is empty: delay = 0; refresh()
refresh():    if use_radar_only: seekByRadar() else seekByAABB()
seekByAABB(): all EntityMissile in the cube +/- target_range around the SAM
              that are valid; sort by squared distance to the SAM; keep the first 5
seekByRadar():candidates = radar objects of this world's radar map in the square
              +/- target_range (chunk-aligned, plus a one-chunk ring, any height; 05);
              keep valid ones; (intended) sort nearest first; keep the first 5
valid(e):     e is an EntityMissile, is not a SAM, and is alive
getTarget():  drop the current target if no longer valid; while there is none,
              poll the queue until a valid one is found
```
Notes:
- "Valid" has no friend/foe test. A SAM will engage any missile, including its own base's outgoing missiles, held-item missiles and missiles with riders (bug 10; the source has TODOs for FoF).
- In radar mode the sort is applied to the wrong list, so the queue keeps radar order instead of nearest first (bug 11). Radar mode finds anything in the radar map, which holds every launched missile (they register at `launch()`); it reaches farther than `target_range` because of the chunk rounding (05-01).
- The queue is only refilled when empty, so a stale far target can stay ahead of a new near threat.

### Interception

Once the SAM has a target and its flight logic allows (a warm-up pad hold blocks this; move steps do not), it switches to `FollowTargetLogic`, which steers directly at the target every tick at 4 blocks/tick (flight-logic.md). If the target dies first, the follow logic steers toward the world origin until fuel runs out (bug 25). The kill test is "target within 4 blocks of the SAM" on the SAM's tick, before it moves: `attackEntityFrom(missile damage, 100)` on the target, then the SAM is removed. Against an explosive missile (50 HP, bug 01) that removes it without a blast. Against the generic missile it does not kill it (bug 03). A target with more than 100 HP simply survives and the SAM is spent.

Radar hand-off at a high level (details 05, 04): the radar station detects launched explosive missiles, builds a threat list and broadcasts it by radio every 40 ticks; a launcher holding a SAM that is on the channel fires at the threat's position (as a ballistic point target, so the SAM launches like any other missile and then re-targets itself once it has a lock). The SAM never receives the radar's threat object directly.

### The missile tracker

**Purpose.** The arc logic sends a missile toward a target that may be thousands of blocks away, through chunks nobody has loaded. A missile in an unloaded chunk does not tick. Instead of loading the path, the tracker removes the missile from the world, counts down the travel time, force-loads the chunks around the target, and spawns the missile again straight above the target.

#### MIT-portable: lifecycle

Permalinks: [MissileTrackerHandler.java#L41-L56](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/missile/tracker/MissileTrackerHandler.java#L41-L56) (entry), [MissileTrackerWorld.java#L58-L91](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/missile/tracker/MissileTrackerWorld.java#L58-L91) (enqueue), [#L96-L202](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/missile/tracker/MissileTrackerWorld.java#L96-L202) (step), [#L204-L232](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/missile/tracker/MissileTrackerWorld.java#L204-L232) (re-entry).

**1. Triggers** (all server side):
- `ArcFlightLogic` and `BallisticFlightLogicOld` call `simulateMissile` each tick while their test is true (height >= 500, or the next chunk about to be entered is not loaded).
- `MissileEventHandler.chunkUnload`: when a chunk unloads, every radar-tracked `EntityExplosiveMissile` in it whose flight logic is `BallisticFlightLogicOld` is simulated (bug 30).

**2. Entry rules** (`MissileTrackerHandler.simulateMissile`): the missile must be non-null, alive, not riding another entity, and have no player passenger. `MissileEvent.EnteringSimQueue` (cancelable) is posted; a cancel refuses. Only `EntityExplosiveMissile` is accepted by signature (bug 22). The method returns false in every case (bug 29).

**3. Enqueue** (`MissileTrackerWorld.simulateMissile`): requires target data. Then:
```
missile.flightLogic = null                     # the guidance is gone for good
data.target = target.getPosition()
data.entity = full entity NBT without "Pos"    # includes health, missile cap, explosive
dx, dz     = target - missile (horizontal only)
data.ticksLeft = round( sqrt(dx^2 + dz^2) / simulation_flight_speed )
missileList.add(data); missile.setDead(); markDirty()
```

**4. Step** (once every 21 world ticks, per dimension):
```
for each entry in missileList:
    if ticksLeft <= 0:
        ensure one NORMAL chunk ticket exists (request it on first use)
        if no ticket: log "Unable to receive chunkloading ticket..."
        else: force the target chunk and its +x, -x, +z, -z neighbors   (event MissileChunkEvent.Load, cancelable)
        entry.preLoad = 5; move entry to spawnList
    else: ticksLeft--
for each forced chunk: timeLeft--; at 0 (and MissileChunkEvent.Unload not canceled): unforce
for each entry in spawnList: preLoad--; at <= 0: spawn it
markDirty()
```
So an entry whose travel count started at `N` spawns at step `N + 5`: `N` steps counting down, one to transition, four more of pre-load.

**5. Re-entry** (`spawnMissileOnDestination`): build a new `EntityExplosiveMissile`, load the saved NBT, then override:
```
x = target.x ; z = target.z ; y = simulation_enter_height (450)
motion = (0, -simulation_enter_speed (1), 0)
if no flight logic: flightLogic = Dead(100)
missile.launch()            # sets doFlight, plays the launch sound, posts PostLaunch, re-registers with the radar
world.spawnEntity(missile)
```
Dead(100) holds 1 block/tick for 100 ticks (no gravity), then gravity 0.05 and drag 0.99 apply. Stepped numerically from y=450: it reaches y=0 after 228 ticks (about 11 s) at about 3.9 blocks/tick; it reaches a y=64 surface at about 215 ticks. The fuse and impact then behave as for any missile (the saved target data survives, so the 2-block air-burst applies).

**Worked timeline** (target 400 blocks from the exit point): `N = 40` steps; spawn at step 45 = 945 ticks (47 s) after leaving the world; plus about 11 s of fall. The real arc to a 400-block target takes 800 ticks (40 s), so the totals are comparable.

**Persistence.** `WorldSavedData` named `icbmclassicMissileTracker<dimension id>` in the dimension's per-world storage (*1.12-only mechanism*; 1.7.10 has `WorldSavedData` too). Created at world load, dropped at world unload. NBT: `missiles` (list) and `spawns` (list); each entry: `ticks` (int, remaining count), `target` (position), `data` (entity NBT). An entry with no `id` in `data` is an old-format save and is run through `EntityMissileDataFixer`. Not saved: the pre-load timer (bug 38), the chunk ticket and the list of forced chunks.

**Events (01-core API).** `MissileEvent.EnteringSimQueue` (cancel to refuse), `MissileChunkEvent.Load` (cancel to skip, or edit the hold time) and `.Unload` (cancel to keep the chunk). All are posted on the Forge bus.

**Debug.** `ConfigDebug.DEBUG_MISSILE_TRACKER` logs each enqueue, each countdown, and each re-entry.

### GTNH server considerations

- One `ForgeChunkManager` NORMAL ticket is shared by all landings in a dimension. Forge's per-mod limits apply (ticket count and chunks per ticket; the stock defaults, from memory and unverified, are 200 tickets and 25 chunks per ticket). At 5 chunks per landing and a 63-second hold, a handful of near-simultaneous landings can overflow one ticket, which unforces the oldest chunk and can drop a missile whose chunk is still being waited on. A rebuild should use a ticket per landing or per missile, or release chunks as soon as the missile has spawned and landed.
- When no ticket can be had, the log warns "Unable to receive chunkloading ticket. You could try to increase the maximum loaded chunks for ICBM." and the missile still spawns after the pre-load wait, into a chunk that may not be loaded (bug 38).
- The ticket callback is an empty lambda, so tickets that Forge restores after a restart are never released by ICBM (unverified; possible permanent forced chunks after a crash).
- Nothing persists the forced-chunk list, so after a restart the pre-existing forced chunks are never unforced by ICBM.
- Only `EntityExplosiveMissile` is simulated. A SAM, held-item or cluster missile that reaches an unloaded chunk just stops (and is saved with its chunk).
- Re-entry is always vertical over the target, at 450 blocks. On a world capped at y=256 the missile spawns above the limit and falls; a target above 450 (sky base) is skipped over.
- The loop runs on the world tick, so it slows with server lag (unlike a wall clock).

## 5. Persistence and sync

NBT keys are given above. Target data and flight logic ride in the missile's `missile` tag (missile-entity.md).

## 6. UI

None. The launcher screen is meant to show the predicted path (TODO in source).

## 7. Visuals

None here.

## 8. Audio

Re-entry replays `MISSILE_LAUNCH` at the spawn point (bug 34).

## 9. Relationships

| Area | Link |
|---|---|
| 04-launch-control | Writes `BallisticTargetingData`, applies inaccuracy and delay, builds the flight chain |
| 05-detection-defense | Radar map used by the SAM radar mode and by the chunk-unload handler; `IncomingMissileMessage` triggers SAM launchers |
| 02-explosives | The explosive capability fired by the fuse, causes in blasts |
| 01-core | `NbtSaveHandler`, buildable registries, `MissileEvent`, `MissileChunkEvent`, `ConfigDebug`, data fixers |

## 10. Bugs

Targeting and tracking: 10, 11, 13, 25 to 35, 37, 38. See [index.md](index.md#suspected-bugs).
