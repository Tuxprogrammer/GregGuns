# Launch Sequence

From the moment something says "fire" to the missile entity existing in the world. Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Flight after spawn is `03-missiles`; the radio bus and message classes are `01-core`; the radar that sends fire messages is `05-detection-defense`.

There are two launcher types, and they share the API but not the rules:

| | Ballistic pad ([launcher-base.md](launcher-base.md)) | Cruise launcher ([cruise-launcher.md](cruise-launcher.md)) |
|---|---|---|
| Implementation | `LauncherCapability` | `CLauncherCapability` |
| Range | min 10 per axis, max `max_range` per axis | min 20 (3D distance), no max |
| Inaccuracy | yes | none |
| Firing delay | yes (per pad, plus per target) | no, but waits for the barrel to aim |
| Flight | warm-up, climb, lock height, arc | direct flight with a fuel limit |

## 1. Triggers: what can say "fire"

All triggers end at `IMissileLauncher.launch(solution, cause, simulate)`. The table lists who calls it and what they pass.

| Trigger | Path | Cause object | Notes |
|---|---|---|---|
| Launch button on the screen GUI | client sends the `launch` packet; server calls `TileLauncherScreen.fireAllLaunchers` | block-screen cause | no check that the sender is near the block or has the GUI open (ICBM-04-13) |
| Redstone on the screen | each tick where `ticks % 3 == 0`, if the world's **strong** power at the screen is above 0, fire all | block-screen cause | level-triggered, so a held signal re-fires every 3 ticks (ICBM-04-20) |
| Radio message to the screen | `RadioScreen.onMessage` on the matching channel | block-screen cause | see section 5 |
| Launch button on the cruise GUI | `launch` packet, server calls the capability directly | entity cause (the player) | |
| Redstone on the cruise launcher | every 40 ticks, any side with power above 1 | redstone cause | see [cruise-launcher.md](cruise-launcher.md) |
| Radio message to the cruise launcher | `RadioCruise.onMessage` | entity cause if the sender is a player's fake sender, else none | |
| Other mods / API | `launch` on the capability | caller's | see section 6 |

A fire request on the screen is a **fire mission**: build one solution, then offer it to every launcher on the network (see [launcher-connector-and-network.md](launcher-connector-and-network.md)).

## 2. The solution and the checks (ballistic pad)

`LauncherCapability.launch(solution, cause, simulate)` is the whole sequence. Steps, in order. A "blocking" status ends the call and is returned to the caller. Errors are always blocking.

1. Ask the solution for the target for **this** launcher.
2. **Pre-check** (`preCheckLaunch`):
   1. Target or its position is null: `ERROR_TARGET_NULL` ("No target set").
   2. Too close: the target is within 10 blocks on **both** the X and Z axes of the block center (a square). `ERROR_MIN_RANGE`. Y is ignored. The 10 is hard-coded.
   3. Too far: the target is more than `max_range` away on **either** axis. `ERROR_MAX_RANGE`. Y is ignored.
   4. Then `getStatus`:
      1. The buffer cannot pay the firing cost (simulated): `ERROR_POWER`.
      2. The missile slot is empty: `ERROR_EMPTY_STACK`.
      3. A firing package exists with a countdown above 0: `FiringWithDelay(remaining ticks)` (blocking, not an error).
      4. Else `READY`.
3. Build the **source**: a cause of kind "block" (world, pad position, block state) whose previous cause is the one passed in, and an action source at the spawn position (see the offsets table in [launcher-base.md](launcher-base.md)).
4. Post the cancelable **PreLaunch** event on the Forge bus (`LauncherEvent.PreLaunch`: source, launcher, holder, the raw target "without offsets or inaccuracy", and the simulate flag). If canceled, return the event's reason, else `CANCELED`. The event also fires for simulated calls and fires again when a delayed shot finally launches (ICBM-04-31).
5. The held stack must carry the missile-stack capability; otherwise `ERROR_INVALID_STACK`.
6. **Apply inaccuracy** to the target (section 3). Computed now, with `max(1, solution.firingCount)` launchers.
7. **Delay.** Only when the pad has no firing package yet: delay = the pad's `firing_delay` plus, if the target data is of the delayed kind, that target's own delay. If delay is above 0: when not simulating, store a package (target, cause, delay); return `FiringWithDelay(delay)`. The inaccuracy computed in step 6 is thrown away; the package fires a fresh launch later, which recomputes it (with a different count, ICBM-04-02).
8. If this is the client, or the call is a simulation: return `LAUNCHED` (nothing changes).
9. **Fire** (server only), section 4.

When the countdown of a stored package reaches 0 the pad calls `launch` again with the package's target and cause, with the delay removed from the target, and with the package still set so step 7 is skipped. The status of that second call is dropped (ICBM-04-08).

The preview the GUI shows is built from step 2 only (`preCheckLaunch`) for every launcher in the group, every 5 ticks, plus an inaccuracy figure; see [launch-screen.md](launch-screen.md).

## 3. Inaccuracy model: MIT-portable

Permalink: [`LauncherCapability.java#L254-L294`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blocks/launcher/base/LauncherCapability.java#L254-L294)

Config (`icbmclassic/launcher`): `min_inaccuracy_range` = 2, `scaled_inaccuracy_range` = 10, `scaled_inaccuracy_per_launcher` = 1, `max_range` = 10,000.

**Radius (meters), the "maximum miss":**

```
d2     = squared 3D distance from the pad block (center) to the target point
scale  = d2 / max_range^2                 // a ratio of squared distances
radius = min_inaccuracy
       + scale * scaled_inaccuracy_range
       + (count - 1) * scaled_inaccuracy_per_launcher     // only when count > 1
```

**Verified against the brief:** minimum 2, plus up to 10 at range, plus 1 per additional launcher. Details the summary hides:

- The distance term is **quadratic** in distance (it uses squared distance over squared range). At 5,000 blocks it adds 2.5, not 5. It reaches 10 at exactly `max_range`, and because range is a square on X/Z but the term uses the 3D circle, a corner target (about 1.41 x `max_range`) gets scale 2 and a radius of 22 plus launcher terms.
- Y counts toward the distance term but not toward the range checks.
- `count` is the **number of launchers in the screen's group, ready or not** (the screen passes `launchers.size()`); a launcher that is empty or powerless still raises everyone's radius (ICBM-04-03). Delayed shots re-enter with count 1 (ICBM-04-02). A bare API call with no count also gets 1.
- Worked examples: one pad at 1,000 blocks: 2 + 0.1 = 2.1. Four pads at 5,000: 2 + 2.5 + 3 = 7.5. Four pads at 10,000: 2 + 10 + 3 = 15.

**Applying it:**

```
radius_used = radius * uniform(0, 1)
angle       = uniform(0, 360) as a yaw
offset      = (x(angle), z(angle)) * radius_used     // pitch 0
target'     = (target.x + offset.x, target.y, target.z + offset.z)
```

Properties: Y is never changed. The miss radius is uniform on [0, radius], not on the disc area, so hits cluster toward the center (mean miss is radius / 2). The source says the intent is a normal distribution as in ICBM 2 (`TODO`). The direction vector comes from the shared `EulerAngle.x()/z()` helpers, whose `z` is wrong, so the scatter is squashed by up to 16% along Z (ICBM-04-04). The random angle object is a static shared by all launchers, which is safe only because the server logic is single threaded.

## 4. Firing: spawn the missile

Permalink: [`LauncherCapability.java#L169-L252`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blocks/launcher/base/LauncherCapability.java#L169-L252)

1. Build a missile from the stack's capability (`newMissile`).
2. **Consume** the missile from the stack (the stack capability decides what is left, for example a stack of one becomes empty). Failure returns `ERROR_INVALID_STACK`.
3. Set the entity's rotation from the facing table and move it to the spawn position. There is no raytrace for a blocked shaft (the source has a TODO); the missile appears at the fixed offset even inside blocks (ICBM-04-26).
4. Set targeting data: `BallisticTargetingData(inaccurateTarget, 2)` (the second argument is hard-coded, with a TODO to take it from the targeting data). Set the flight logic (below). Set the missile source (the action source). Call `launch()` on the missile.
5. Spawn the entity. If the world refuses, `ERROR_SPAWN`; the missile is already consumed (ICBM-04-07).
6. **Pay power**: re-check, then really extract `power_per_launch`. The check runs after the spawn, so on failure the missile is already flying and free (ICBM-04-07). Practically unreachable, because step 2 of the checks already tested the same buffer in the same tick.
7. If the seat entity has passengers, move them onto the missile (`startRiding`).
8. Return `LaunchedWithMissile` carrying the missile object (runtime only).

**Flight logic assigned by the pad** (the logic classes themselves are `03-missiles`):

| Launch direction | Chain |
|---|---|
| UP | warm-up (40 ticks sitting still) then step 1: move along the facing 2 blocks at acceleration 0.005 (slow climb); step 2: move `lock_height - 2` more blocks at acceleration 0.1; then arc flight to the target |
| Any other | warm-up (40 ticks) then one step: move `lock_height` blocks along the facing at acceleration 0.2; then arc flight |

A `lock_height` below 2 on an UP pad gives a negative second distance (unvalidated field; the effect is `03-missiles`). The payload velocity the launcher reports for intercept math (used by radio targeting, section 5) is the SAM flight speed when the held item is the SAM missile, else the direct-fire speed (default 2 m/tick).

## 5. Radio firing

The radio bus (channels, registry, `FakeRadioSender`) is `01-core`. This is what the launchers do with messages. All handling is server side only and requires the receiver not to be disabled, the sender not to be itself, and the message channel to equal the receiver's channel string.

**Screen** (`RadioScreen`):

1. If the message is a **target message** (carries a position or an intercept): compute the velocity as the **max** payload velocity over the launchers in the screen's group, ask the message for an intercept point from the screen's position at that velocity, and set the screen's target to it. A null result replies "Target data missing". The "Target set" reply is skipped when the same message also triggers a fire.
2. If the message is a **trigger message** and its `shouldTrigger` is true: run `fireAllLaunchers`. Reply "Firing missile(s) at (x y z) with range d" on success, else "Failed to fire missile(s)". Range is the straight distance from the screen to the target.

A radar's "incoming missile" message is both kinds, so a radar on the same channel as a screen sets the target to the intercept of the incoming missile and fires, once per radar broadcast (40 ticks by default). A laser detonator sends target-and-trigger. A delayed or aiming shot counts as a failure for the reply (ICBM-04-09).

**Cruise** (`RadioCruise`): the same target step, using the cruise launcher's own payload velocity. On a trigger: if a firing package already exists, reply with the "queued" message and stop; else create a package for the current target (cause: the player if the sender is a fake sender, else none) with the message's trigger callback attached, and reply "Firing missile(s), aiming launcher(s)". The callback receives the final launch status.

The message and channel classes are linked from `01-core`; see also [05 detection and defense](../05-detection-defense/index.md).

## 6. Launch solutions API

`api/launcher` (public API, MIT):

**`ILauncherSolution`** gives the launcher its marching orders:

| Method | Meaning |
|---|---|
| `getTarget(launcher)` | target data for that launcher. May be called several times per launcher and must return the same value each time, so randomness has to be stored at creation |
| `getFiringCount()` | default 1. "0 or greater; -1 to fire all". The ballistic pad actually uses it as the count for the inaccuracy term |
| `getFiringGroup()` | default -1. "0 or group to match firing group" |

The implementation `LauncherSolution(target)` has group -1, count -1; `LauncherSolution(target, group, count)` is what the screen builds.

**`IMissileLauncher`** (the capability, one per launcher):

| Method | Meaning |
|---|---|
| `getUniqueId` | persisted UUID |
| `getStatus` | the launcher's own state, no target |
| `preCheckLaunch(target, cause)` | deprecated; target validation plus status |
| `launch(solution, cause, simulate)` | deprecated in favor of the action system; simulate must not change anything |
| `launch(target, cause, simulate)` | deprecated overload that wraps the target in a solution with defaults |
| `getLaunchIndex`, `getLauncherGroup` | default -1; "future state". **Neither launcher overrides them** (ICBM-04-01) |
| `getInaccuracy(predictedTarget, launchers)` | default 0; the pad overrides it |
| `getPayloadVelocity` | default is the direct-fire speed |

Statuses (`IActionStatus`; blocking statuses stop a launch, errors are blocking): registered constants `ERROR_GENERIC`, `ERROR_SPAWN`, `ERROR_SPAWN_COLLIDER`, `ERROR_MIN_RANGE`, `ERROR_MAX_RANGE`, `ERROR_TARGET_NULL`, `ERROR_POWER`, `ERROR_INVALID_STACK`, `ERROR_EMPTY_STACK`, `ERROR_QUEUED`, `ERROR_EMPTY_GROUP`, `ERROR_NO_NETWORK`, `READY`, `LAUNCHED`, `CANCELED`, and the blocking non-error `FIRING_AIMING`; plus the parameterized `FiringWithDelay` (NBT `delay`, message shows the ticks) and `LaunchedWithMissile` (holds the missile). Unused: `ERROR_QUEUED`, `ERROR_EMPTY_GROUP`, `ERROR_NO_NETWORK`, the lang keys `.fuel` and `.target.angle`.

Events: `LauncherEvent.PreLaunch` (cancelable) and `LauncherSetTargetEvent` (cancelable; lets a listener rewrite or reject a target before the screen or cruise launcher stores it; a null replacement becomes (0, 0, 0)). Both live in `api/events`.

**GregGuns note:** the API is a good shape to keep (solution, status with blocking/error flags, simulate). The real work is in the rules above.

## 7. Firing package

`FiringPackage` (shared, `content/blocks/launcher/FiringPackage`): a pending shot. Fields: target data, cause, countdown, a "has fired" flag, and an optional callback invoked with the launch status. Saved: `countdown`, `target`, `cause` (the last two are rebuilt from registries). Its `launch(launcher)` fires once, strips any delay from the target, and calls `launcher.launch` through a one-line solution, so every rule above applies again at fire time. The class's own `update` is an unfinished stub (never registered) and the owning tile does the counting.

## 8. Bugs

ICBM-04-02, -03, -04, -05, -06, -07, -08, -09, -13, -20, -26, -31. Table in the [index](index.md).

## Lang

| Key | English |
|---|---|
| `info.icbmclassic:launcher.error` | Unknown Error |
| `.error.power` | Firing Safety; Not enough power |
| `.error.fuel` | Firing Safety; Not enough fuel (unused) |
| `.error.missile.none` | Firing Safety; No missile |
| `.error.missile.multi` | `%1$s / %2$s` not ready (red) |
| `.error.network` | Self Check; No launcher network |
| `.error.network.status.missing` | No Status... server desync |
| `.error.network.launchers` | Self Check; No launcher(s) connected (the code builds a key without the dot, ICBM-04-16) |
| `.error.target.none` | Guidance Safety; No target set |
| `.error.target.min` / `.max` | Guidance safety; Target too close / too far |
| `.error.target.angle` | Guidance Safety; Aiming angle limits hit (unused) |
| `.error.missile.space` | Guidance Safety; launcher collision detected |
| `.error.launcher` | Firing Failure; Null launcher detected (unused) |
| `.error.missile.spawn` | Firing Failure; Missile Spawn Prevented |
| `.status.ready` / `.launched` / `.canceled` | Ready to fire / Launch Triggered / Launch Canceled |
| `.status.firing.delayed` | Launch Triggered; Firing in %s |
| `.status.firing.aiming` | Launch Triggered; Aiming at target |
| `radio.icbmclassic:message.target.null` / `.set` | Target data missing / Target set to (%f %f %f) |
| `radio.icbmclassic:message.launch.success` / `.failed` / `.triggered` | see section 5 |

Missing from `en_US.lang` (ICBM-04-17): `.error.missile.invalid`, `.error.missile.queued`, `.error.group.empty`.
