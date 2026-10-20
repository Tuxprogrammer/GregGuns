# Flight Logic

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. A missile is a plain projectile entity whose motion is edited every tick by one **flight logic** object (a small "flight computer"). Logics can be chained: when a step finishes it hands over to its `next` step. This sheet specifies every logic in `content/missile/logic/flight/` and the interface in `api/missiles/parts/`, with the arc math in full. Who builds which chain is the launcher's job (04); what happens to a missile at the top of its arc is [targeting-and-tracking.md](targeting-and-tracking.md).

## 1. Identity

| Registry key (`icbmclassic:`) | Class | Parent | Role |
|---|---|---|---|
| `engine.warmup` | `WarmupFlightLogic` | `FlightLogic` | Sit on the pad for a timer, spawn pad smoke, then hand over |
| `engine.move.facing` | `move/MoveByFacingLogic` | `AccelerateByFacingLogic` | Accelerate along one of the six block directions until a distance is covered |
| `engine.move.vector` | `move/MoveByVec3Logic` | `AccelerateByVec3Logic` | Same along an arbitrary direction vector (used by the rocket launcher) |
| `engine.move.ticks` | `move/MoveForTicksLogic` | `AccelerateByFacingLogic` | Accelerate along a facing for a number of ticks |
| `path.arc` | `ArcFlightLogic` | none (own class) | Ballistic arc from launch point to target; the main long-range logic |
| `ballistic` | `BallisticFlightLogicOld` | none | Deprecated all-in-one version (warmup, climb, lock height, arc) kept for old saves |
| `direct` | `DirectFlightLogic` | `DeadFlightLogic` | Straight line toward the target at a fixed speed, for a fuel time (cruise launcher) |
| `direct.follow` | `FollowTargetLogic` | `DeadFlightLogic` | Re-aim at the live target every tick (SAM) |
| `dead` | `DeadFlightLogic` | none | No guidance; motion left alone while fuel lasts, then gravity |

Support classes: `prefab/FlightLogic` (step base), `prefab/AccelerateByFacingLogic`, `prefab/AccelerateByVec3Logic`. The registry (`ICBMClassicAPI.MISSILE_FLIGHT_LOGIC_REGISTRY`, a locked builder registry, extendable by addons through `MissileFlightLogicRegistryEvent`, 01-core) maps each key to a no-argument constructor for loading saves.

## 2. Obtaining

Logics are not items. Each is built by:

| Builder | Chain |
|---|---|
| Silo launcher, facing UP (`LauncherCapability.buildFLightPath`, 04) | `Warmup(40)` then `MoveByFacing(2 blocks, accel 0.005)` then `MoveByFacing(lockHeight - 2 blocks, accel 0.1)` then `Arc`. `lockHeight` defaults to 3 |
| Silo launcher, other direction | `Warmup(40)` then `MoveByFacing(lockHeight blocks, accel 0.2)` then `Arc` |
| Cruise launcher (`CLauncherCapability`) | `Direct(cruise_fuel = 200)` |
| Rocket launcher, "fire up/down" (javelin) mode (part b) | `MoveByVec3(3 blocks, accel 0.2, along the look vector)` then `Arc` |
| Rocket launcher, normal mode (part b) | `Dead(handheld_fuel = 200)` |
| SAM | whatever the launcher gave it, then `FollowTarget(200)` once it has a target |
| EMP hit | `Dead(0)` |
| Missile tracker re-entry | `Dead(100)` if none |

All move steps are non-relative (distance measured from actual motion). `PAD_WARM_UP_TIME` = 40 ticks and `MISSILE_CLIMB_HEIGHT` = 2 blocks are constants in the launcher (marked TODO config).

## 3. Numbers

| Config key (`icbmclassic/missile`) | Default | Range | Used by |
|---|---|---|---|
| `speed_direct_fire` | 2 blocks/tick | 0.0001 to 10 | `Direct` speed; rocket launcher shot speed; launcher payload velocity |
| `arc_distance_limit` | 500 blocks | >= 0 | `Arc`: above this flat distance, fly straight up and let the tracker take over |
| `simulation_exit_height` | 500 (y) | >= 1 | `Arc`: above this the missile is handed to the tracker |
| `cruise_fuel` | 200 ticks | >= 0 | `Direct` fuel from the cruise launcher |
| `handheld_fuel` | 200 ticks | >= 0 | `Dead` fuel from the rocket launcher |
| `sam.speed` | 4 blocks/tick | 0.0001 to 10 | `FollowTarget` speed |
| `sam.fuel` | 200 ticks | >= 0 | `FollowTarget` fuel |

Other constants are hard-coded in `ArcFlightLogic.calculatePath`: 2 ticks per meter of flat distance, a base arc height of 160 above 100 meters, a height cap of 1,000, a height scale of 3 above 1,000 meters, the vertical and horizontal speed of the straight-up path (2 and 0.05), and the X/Z lock tolerance of 0.1 block.

Base projectile physics when a logic lets them run: motion x0.99 per tick, then -0.05 on Y.

## 4. Behavior

### The interface contract

`IMissileFlightLogic` (all calls on the server and the client unless the logic checks `world.isRemote`):

| Hook | When | Default |
|---|---|---|
| `calculateFlightPath(world, x, y, z, target)` | each time the logic becomes active (`launch()` or `switchFlightLogic`), before `start` | no-op |
| `start(entity, missile)` | right after that | no-op |
| `onEntityTick(entity, missile, ticksInAir)` | every tick while `doFlight` is set | no-op |
| `shouldDecreaseMotion(entity)` | after motion is applied | true (gravity and drag apply) |
| `shouldRunEngineEffects(entity)` | every tick | true |
| `shouldAlignWithMotion(entity)` | rotation step | false (logic sets rotation itself) |
| `engineSmokeRed/Green/Blue` | smoke spawn | 1, 1, 1 |
| `canSafelyExitLogic()` | SAM checks this before taking over | true |
| `predictPosition(entity, builder, ticks)` | used by `Arc` for its own look-ahead | `Dead`: straight line, no gravity. Step logics: null |

`switchFlightLogic(next)` = `setFlightLogic(next)` (marks the client for a resync) then `calculateFlightPath` and `start` on the new logic. Switching to `null` leaves the missile with ordinary projectile physics.

### `FlightLogic` (step base)

- `start`: if `isValid()` is false, log an error and switch straight to `next`.
- `onEntityTick`: **server only**, if `isDone()` then `missile.switchFlightLogic(next)`. A null `next` clears the logic.
- `shouldDecreaseMotion` = `!isValid()`. A valid step turns off gravity and drag, so motion is whatever the steps made it.
- NBT: `next` (buildable object). Subclasses add their fields.

### `WarmupFlightLogic` (`engine.warmup`)

- Per tick: parent check (`isDone()` when `timer <= 0`, then switch), then `timer--`; while `timer > 0` spawn pad smoke (client proxy).
- `shouldRunEngineEffects` and `canSafelyExitLogic` are true only when `timer < 0`, which in a normal chain is never observed (the step switches at 0). So no engine sound or flame during warmup, and a SAM cannot lock on until warmup is over.
- `shouldDecreaseMotion`: only when `timer < 0` and there is no next step.
- The launcher sets the timer to 40 ticks (2 seconds). The timer is not written to NBT (bug 23).

### `AccelerateBy*` and `MoveBy*` (steps)

- Each tick (after the parent check, only while valid and not done): `motion += direction * acceleration` and `velocityAdded += acceleration`. Direction is the unit vector of the block face (facing) or the given vector (vec3). A null direction makes the step invalid.
- `MoveByFacing` / `MoveByVec3`: `distance` counts down. Non-relative (the launcher's mode): `distance -= dot(direction, motion)` after the acceleration. Relative: `distance -= velocityAdded` (the speed built up by this step alone). Done when `distance <= 0`. A step could run forever if something blocks movement (noted in the source). `velocityAdded` is not saved (bug 24).
- `MoveForTicks`: `ticks` counts down (only while >= 0); done at `ticks <= 0`.
- NBT: `acceleration`, `direction` (facing name or vec3), `distance` or `ticks`. The `relative` flag is not saved.

Silo climb, worked out: step 1 adds 0.005 blocks/tick per tick, so after n ticks it has covered `0.0025 * n * (n + 1)` blocks; 2 blocks takes 28 ticks (speed then 0.14). Step 2 adds 0.1 per tick on top; the default 1 block more takes about 3 ticks (speed about 0.45). `Arc` then overwrites the motion in one tick (a visible snap). If `lockHeight` is below 2 the second distance is negative and it is skipped at once.

### `DeadFlightLogic` (`dead`)

- `fuelTicks` counts down every tick (it can go negative; the counter is not clamped).
- While fuel > 0: engine effects on; rotation is set from the motion vector each tick; **no gravity or drag** (`shouldDecreaseMotion` = out of fuel). When fuel is out: engine effects off, normal gravity and drag apply.
- Never touches motion itself. The launch speed comes from the spawner (rocket launcher shot) or from `Direct.start`.
- NBT: `fuel`.
- `predictPosition`: position + motion * ticks, no gravity.

### `DirectFlightLogic` (`direct`, cruise missile)

- `calculateFlightPath` (once, at launch): vector from the launch point to the target position, normalized (float math), scaled by `speed_direct_fire` (2). If target data is null nothing is set.
- `start`: copy that vector into the entity motion and set yaw and pitch from it.
- Then it behaves as `Dead(fuel)`: flies a straight line at 2 blocks/tick for `cruise_fuel` = 200 ticks (up to 400 blocks), then falls under gravity with drag. It never corrects. The motion vector is not saved, but it is applied only at start so reloads are safe. A zero-length vector gives NaN (bug 25).

### `FollowTargetLogic` (`direct.follow`, SAM)

- Extends `Dead`, with smoke color (1.0, 0.5, 0.5).
- Each tick, **server only**, while fuel > 0 and the missile has target data: vector from the missile to `target.getX/Y/Z()` (the feet position of the target entity; if the SAM's target data has no live entity it answers 0,0,0, so the SAM turns toward the world origin, bug 25), normalized, times `sam.speed` (4), written straight into the motion. No turn limit and no lead: it re-aims perfectly every tick, so it cannot be dodged but also never leads a fast target.
- When fuel ends it falls like a dead rocket.

### `ArcFlightLogic` (`path.arc`) - MIT-portable

Permalink: [ArcFlightLogic.java#L59-L213](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/missile/logic/flight/ArcFlightLogic.java#L59-L213) (path set-up, start, per-tick) and [#L232-L265](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/missile/logic/flight/ArcFlightLogic.java#L232-L265) (simulation test).

The arc is a constant horizontal velocity plus a constant downward acceleration on Y. Nothing else acts on the missile (`shouldDecreaseMotion` is false, so no drag, no gravity).

**Inputs.** `start` = missile position on the first flight tick (not the launch point; it is after the pad climb). `end` = the target data position (x, y, z) read in `calculateFlightPath`. If there is no target data, `end` stays (0, 0, 0) and the missile arcs toward the world origin (bug 19). The target Y is stored but never used (bug 18).

**Derived values** (floats), with `D` = flat distance `sqrt(dx^2 + dz^2)`:

```
if D < arc_distance_limit  OR  a player is riding:
    scale = 3 if D > 1000 else 1
    base  = 160 if D > 100 else 0
    H     = min(1000, base + D * scale)         # apex height above the start altitude
    T     = ceil(2 * D)                         # flight time in ticks (2 ticks per meter)
    a = H / D;  b = H / T;  c = T / D
    g = ((H - a) * a) / (T / c) / (b * D)       # downward acceleration per tick
else:
    straight-up mode (see below)
```

This simplifies exactly to `g = H * T * (1 - 1/D) / D^3`, which is about `2H / D^2` since `T` is about `2D`. The `(1 - 1/D)` factor is an artifact of the odd formula; it matters only for `D` under about 10.

**Launch tick** (the first tick the logic runs; `hasStartedFlight` is set):
```
vx = dx / T ;  vz = dz / T                       # horizontal speed D/T = 0.5 block/tick
vy = g * T / 2                                   # chosen so the apex is at about T/2
```
**Each later tick** (server): `vy -= g`; if `|x - end.x| <= 0.1` and `|z - end.z| <= 0.1` then `vx = vz = 0`; set rotation (pitch from the velocity, yaw from vx, vz); then the base class adds the velocity to the position. The setup tick also moves the missile by `(vx, vy)` once.

**What it produces** (verified by stepping the discrete update in a scratch script):
- Horizontal arrival is exactly at tick `T`: the position after `n` moves is `start + n * v`, so at `n = T` the X/Z test fires and horizontal motion stops.
- The apex is `H` above the start altitude, reached near tick `T / 2`.
- Height returns to the start altitude at tick `T + 1`. The target altitude is ignored: a target below the start is reached by the free fall that follows the horizontal stop (vertical speed keeps growing by `g` per tick, so it lands within a few ticks); a target above hits whatever it flies into (bug 18).

| Flat D | H (apex) | T (ticks) | g | launch vy | notes |
|---|---|---|---|---|---|
| 1 | 1 | 2 | 0 (flat flight) | 0 | degenerate |
| 10 | 10 | 20 | 0.18 | 1.8 | silo minimum is 10 |
| 100 | 100 | 200 | 0.0198 | 1.98 | no base height yet |
| 101 | 261 | 202 | 0.0507 | 5.12 | base 160 kicks in: the fastest launch |
| 250 | 410 | 500 | 0.0131 | 3.27 | |
| 400 | 560 | 800 | 0.00698 | 2.79 | apex above y=500 with a typical y=64 base, so the tracker takes over |
| 499 | 659 | 998 | 0.00528 | 2.64 | largest unridden arc |
| 1000 (rider) | 1000 | 2000 | 0.0020 | 2.00 | |
| 2000 (rider) | 1000 | 4000 | 0.0005 | 1.00 | scale 3 only matters above 1000, where H is already capped |
| 5000 (rider) | 1000 | 10000 | 0.00008 | 0.40 | 8 minutes of flight |

Horizontal speed is always 0.5 block/tick (10 blocks/s). Degenerate inputs (bug 17): `D = 0` makes everything NaN; `0 < D < 1` makes `g` negative so the missile dives; `D = 1` has `g = 0`.

**Straight-up mode** (`D >= arc_distance_limit` and no rider): `flightUpAlways`. The missile leaves at `vy = 2` with a nudge of 0.05 toward the target, no gravity, and climbs until it crosses `simulation_exit_height` (500), where the tracker removes it (see the tracking sheet). It never arcs. If a player starts riding after launch, the logic recomputes the arc from the current position but forgets to reset the velocity, so the missile keeps climbing at 2 blocks/tick under the new gravity and never reaches the target (bug 20). `flightUpAlways` itself is not saved.

**Simulation hand-off** (`shouldSimulate`, server, only for `EntityExplosiveMissile`, bug 22): no rider and not previously blocked, and either `posY >= simulation_exit_height`, or one of the chunks between the current chunk and the chunk of the position two ticks ahead (inclusive range, `from..to`) is not loaded. When the range is walked from low to high index only, a missile moving toward lower X or lower Z skips that check on that axis (bug 21). On true, `MissileTrackerHandler.simulateMissile` is called every tick; its result is wrongly negated into `wasSimulationBlocked` (bug 29).

**Safe to exit / effects.** `canSafelyExitLogic` and `shouldRunEngineEffects` are `hasStartedFlight`. Rotation is written directly each tick; `shouldAlignWithMotion` is false.

**NBT** (`path.arc`): `flags.flight_started`, `flags.block_simulation`; `inputs` {`start_x/y/z`, `end_x/y/z`}; `calculated` {`flight_time`, `acceleration`, `delta_x/y/z`}; `timers.flight`.

**Inaccuracy inputs.** The arc itself has none. The error comes from the target the launcher passes in (04): `applyInaccuracy` moves the target horizontally by `inaccuracy * rand()` in a random direction, where `inaccuracy = min_inaccuracy + (distance^2 / range^2) * scaled_inaccuracy_distance + (launchers - 1) * scaled_inaccuracy_launchers` (`ConfigLauncher` defaults: min 2, distance scale 10, per extra launcher 1, range 10,000). The missile then flies to that shifted point exactly (the 0.1 lock makes the arrival exact). The rocket launcher and cruise launcher add no spread to the arc or direct path (the rocket launcher's shot has none either: `random` is unused in `throwProjectile`).

### `BallisticFlightLogicOld` (`ballistic`)

Deprecated; registered so old saves load. It is the same arc math with these differences:
- The arc-versus-straight-up threshold is **200** (hard-coded), not `arc_distance_limit`. It has no rider exception.
- It carries its own phases: 40-tick pad warmup (pitch forced to 90), a slow climb (`+0.005` motion per tick for 2 blocks), a "lock height" climb (`+0.1` motion per tick, X/Z zeroed, counting `lockHeight` down), then the arc. Engine effects start after the warmup.
- Its simulation test is "is the area between the position and the 2-tick-ahead position loaded" (`isAreaLoaded`), which handles all directions.
- Its gravity edit is a plain `motionY -= g`.
- It is also the only logic the chunk-unload handler simulates (bug 30). The silo no longer builds it.
- NBT adds `timers.engine_warm_up`, `timers.climb_height`, `timers.lock_height`.

### Interaction summary

- **Reload safety:** all logics save their state; `Warmup.timer` and the `velocityAdded` counter do not.
- **Client:** logics run on both sides, but all state-changing branches (`isDone` switch, follow aim, arc gravity and lock) are server-only. The client mirrors via the description packet and tracker motion.
- **EMP:** replaces the logic with `Dead(0)`; the missile falls (gravity 0.05, drag 0.99) from wherever it is.
- **Re-entry after simulation:** see targeting-and-tracking.md.

## 5. Persistence and sync

See the NBT lists above; registry keys are in the identity table. Any change of logic marks the client for a description packet (missile-entity.md).

## 6. UI

None. The source has TODOs to show the predicted path on the launcher screen.

## 7. Visuals

Engine smoke every tick while effects are on, pad smoke during warmup; colors from the logic (white; the follow logic is light red). Spawned through the client proxy (07).

## 8. Audio

`MISSILE_ENGINE` per tick while effects are on (missile-entity.md section 8).

## 9. Relationships

| Area | Link |
|---|---|
| 04-launch-control | Builds the chains, applies inaccuracy, silo `lockHeight`, `isTargetTooClose` (10 blocks) and `isTargetTooFar` (`ConfigLauncher.RANGE`) |
| 01-core | Buildable registry, `NbtSaveHandler`, `ConfigDebug.DEBUG_MISSILE_LOGIC` (logs every switch) |
| 05-detection-defense | EMP swaps in `Dead(0)` |
| This area | tracker (simulation), SAM (`FollowTarget`), missile entity (calls the hooks) |

## 10. Bugs

Flight-related: 17 to 25, plus 29 to 30. Full list: [index-a.md](index-a.md#suspected-bugs).
