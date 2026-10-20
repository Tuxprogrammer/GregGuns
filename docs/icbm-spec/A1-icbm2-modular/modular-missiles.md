# Modular missiles (ICBM 2)

Source: BuiltBrokenModding/ICBM ("ICBM 2", MIT, 1.7.10) @ `13e6a524`. Paths are relative to `src/main/java/com/builtbroken/icbm/content/` unless they start with `api/` or `resources/`. Permalink base: `https://github.com/BuiltBrokenModding/ICBM/blob/13e6a5245c76f5599baca521b462168b9ebe75d8/src/main/java/com/builtbroken/icbm/content/`.

ICBM 2 builds a missile from one **casing item** (the missile item, damage value = size) plus three **modules** stored in its NBT: a warhead, an engine and a guidance module. A fourth module, a **trigger**, sits inside the warhead. Everything else (range, accuracy, blast) is derived from those parts. This is the design GregGuns D8 wants to echo with workbench slots.

## 1. Size classes (`MissileSize`, `missile/data/missile/MissileSize.java`)

The enum is marked deprecated ("will be replaced by a data system"). The item damage value is the enum ordinal.

| Ordinal | Size | Class comment | Warhead casing | Max flight time | Body (w x h) | Entity HP (`getMaxHealth`) |
|---|---|---|---|---|---|---|
| 0 | MICRO | 1 m and below | EXPLOSIVE_MICRO | 1,200 ticks (1 min) | 0.2 x 0.8 | 2 |
| 1 | SMALL | 3 m and below | EXPLOSIVE_SMALL | 12,000 ticks (10 min) | 0.5 x 2 | 10 |
| 2 | STANDARD | 7 m and below | EXPLOSIVE_STANDARD | 72,000 ticks (1 h) | 0.6 x 4.6 | 30 |
| 3 | MEDIUM | 16 m | EXPLOSIVE_MEDIUM | 360,000 ticks (5 h) | 1.2 x 16 | 2 (bug, see ICBM-A1-04) |
| 4 | LARGE | 32 m | EXPLOSIVE_LARGE | 1,440,000 ticks (20 h) | 2.5 x 32 | 100 |

- Body width, height, mass, health and center offset come from content JSON (`resources/content/icbm/missile/<size>.json`: keys `id`, `bodySizeClassification`, `bodyWidth`, `bodyHeight`, `bodyCenterOffset`, optional `bodyMass`, `bodyHealth`, `default`). No shipped JSON sets `bodyMass` or `bodyHealth`, and the entity ignores the casing's health anyway.
- A casing is registered into its size's `casingDataMap` by id. The default casing for a size is the entry with id `icbm:<size>Missile`. Several casings per size are possible (alternate skins or bodies), selected by the item NBT key `missileCasingID`.
- `EntityMissile.setMissile` sets the kill timer (`inAirKillTime`) from the size's max flight time. The constructor default is 144,000 ticks (2 h).
- Crafting recipes exist in JSON for micro (`sheetSmallCylinderMetal` center, `sheetMicroFinMetal` fins, iron rods) and small (`casingSmallMissile` center, `sheetSmallFinMetal`, iron rods, grid ` rf / rcf / ` + ` rf`). Standard, medium and large casings have no recipe in JSON.
- Creative tabs exist for MICRO, SMALL, STANDARD and MEDIUM only (`ICBM.java:150-153`); there is no LARGE tab.

## 2. Module interfaces and assembly (`api/modules/*`, `missile/data/missile/Missile.java`)

| Interface | Method set | Implemented by |
|---|---|---|
| `IMissile` | get/set warhead, guidance, engine; `canLaunch()`; `getMissileSize()` | `Missile` |
| `IWarhead` | `getMaxExplosives()`, explosive stack get/set, `getSpaceForExplosives()`, `trigger(cause, world, x, y, z)` | `Warhead` and 5 size subclasses |
| `IRocketEngine` | `getSpeed(missile)`, `getMaxDistance(missile)`, `onLaunch`, `onDestroyed`, `generatesFire` | `RocketEngine` and subclasses |
| `IGuidance` | `getFallOffRange(missile)`, `getChanceToFail(missile)` | `Guidance` and subclasses |
| `ITrigger` / `ITriggerAccepter` | `shouldTrigger(cause, ...)`; warhead holds one trigger | `Trigger`, `Warhead` |

How a missile stores its parts:

- `Missile` holds up to **one** of each: warhead, engine, guidance. `installModule` fills an empty slot only. `canInstallModule` accepts any `RocketEngine`, `Warhead` or `Guidance`, regardless of size match.
- Persistence: NBT keys `warhead`, `engine`, `guidance`, each holding the module's own **item stack** written with `writeToNBT`. Loading reads the stack back and asks the item for its module. So a module is its item: all module state (fuel tank, loaded explosive, trigger) lives inside the module item's NBT, nested in the missile item's NBT.
- Module ids come from a registry (`MissileModuleBuilder`, marked deprecated: "not going to work for the long term"). Ids are `icbm.warhead_micro`, `icbm.engine.coal`, `icbm.guidance.gears.iron`, `icbm.trigger.impact.mechanical` and so on.
- `canLaunch()` = engine present AND `getMaxDistance > 0` AND `getSpeed > 0`. Nothing else is required: **no warhead and no guidance is a legal launch.**
- A warhead's size must match the missile only because the *warhead item* has a casing size and the workbenches check it; the `Missile` object does not.

### Item and entity facts

- `ItemMissile` is the missile item: `getSubItems` lists the empty casings, then one fully loaded missile per registered explosive item (warhead filled to its max count, creative engine and chip-three guidance via `Missile.createMissile`).
- In flight the missile is an `EntityMissile` (a projectile with health). It stores the whole missile as `missileStack` NBT and syncs it with spawn data. `fofTag` carries the friend-or-foe tag of the launching station.

## 3. Warhead casings (`missile/parts/warhead/*`)

Five classes, one per size, all extending `Warhead`. `WarheadCasings` is the enum (class, mass, factory, enabled flag).

| Casing | Max explosive items | Casing mass | Enabled in creative lists |
|---|---|---|---|
| EXPLOSIVE_MICRO | 1 | 1,000 | yes |
| EXPLOSIVE_SMALL | 5 | 3,500 | yes |
| EXPLOSIVE_STANDARD | 20 | 8,000 | yes |
| EXPLOSIVE_MEDIUM | 64 | 150,000 | **no** |
| EXPLOSIVE_LARGE | 128 | 400,000 | **no** |

(Masses are in the code's mass unit, apparently grams: the triggers use 54,431.1 for "120 pounds".)

Warhead behavior:

- It holds **one stack of explosive items** (NBT key `exItem`). The stack size is the explosive "size": `getExplosiveSize()` asks the explosive registry for the stack's size, which scales the blast. More items in the casing means a bigger blast, up to the casing cap. This is the explosive-mass limit.
- `trigger(cause, world, x, y, z)`: if there is no trigger, or the trigger's `shouldTrigger` says yes, and an explosive is present, it asks `ExplosiveRegistry.triggerExplosive` with size = `explosiveSize * cause.effectScaleChange`. Bad coordinates (null world or NaN) fail with an error log. The result is a change-result enum; COMPLETED or PARTIAL_COMPLETE_WITH_FAILURE counts as "blew".
- A destroyed (shot down) missile triggers its warhead with `effectScaleChange` 0.1, via `TriggerCauseMissileDestroyed`.
- The warhead forwards its per-tick `update` to its trigger when the trigger is itself a missile module.
- The same casings exist as **placeable warhead blocks** (`TileWarhead`, damage value = casing ordinal). The warhead station fills them (see [workstations.md](workstations.md)).

## 4. Engines and fuels (`missile/parts/engine/*`)

`Engines` enum (item damage = ordinal): CREATIVE, COAL, OIL, FUEL, GUNPOWDER. Base `RocketEngine` defaults: speed 0.5, range 1000, "has fuel" true, generates fire true, engine flame/smoke particles.

| Engine | Fuel storage | Speed | Max distance | Notes |
|---|---|---|---|---|
| Creative | none | 2 | 10,000 | Used by `Missile.createMissile` (test and creative missiles) |
| Coal | 1 item slot (inventory) | per fuel, default 0.5 | per fuel value x stack size | Fuel table below; mass 500,000 |
| Gunpowder | 1 item slot | 5 | 8 x stack size | Fast, short. Creative-filled = 64 gunpowder. Mass 500 |
| Oil | 1000 mB tank, accepts fluid `oil` only | 0.3 | 2 x tank mB (0 when empty) | Mass 104,326 |
| Fuel | 1000 mB tank, accepts fluid `fuel` only | 0.6 | 3 x tank mB (0 when empty) | Mass 90,326. Recipe uses an oil engine as an ingredient |

- A full bucket tank therefore gives a 2,000 (oil) or 3,000 (fuel) "distance". Solid engines accept stacks only through the workstation or by crafting (shapeless "add fuel" recipes combine an engine with 1 to 8 fuel items, or gunpowder 1 to 8).
- `generatesFire` is true while fuel remains (solid: stack present; fluid: tank non-empty). It drives flame effects, not flight.
- **Neither speed nor distance is a flight simulation.** See section 7 for what they actually control.

### Coal-engine fuel table (MIT-portable)

Permalink: [`RocketEngineCoalPowered.java` L148-L206](https://github.com/BuiltBrokenModding/ICBM/blob/13e6a5245c76f5599baca521b462168b9ebe75d8/src/main/java/com/builtbroken/icbm/content/missile/parts/engine/solid/RocketEngineCoalPowered.java#L148-L206).

The engine builds two maps at post-init from the ore dictionary. Base values: `VALUE_OF_COAL` = 15 distance per item, `SPEED_OF_COAL` = 0.5 (both public static, so configurable by code).

| Fuel (ore dict) | Distance per item | Speed |
|---|---|---|
| Coal | 15 | 0.5 |
| dustCoal | 15 | 0.525 (+5%) |
| fuelCoke | 30 (+100%) | 0.55 (+10%) |
| Logs | 12.75 (-15%) | 0.325 (-35%) |
| Wood planks | 3 (-80%) | 0.325 |
| Sticks | 0.75 (-95%) | 0.325 |
| Leaves, saplings | 1 | 0.325 |

Distance is `value x stack size` of the single fuel slot, and speed is the single value for that fuel. Charcoal and coal blocks are accepted by the slot filter (`canStore`) but have no table entry, so they give distance 0. The fuel-matching loops over the ore dictionary have an index bug (ICBM-A1-07).

## 5. Guidance (`missile/parts/guidance/*`)

`GuidanceModules` enum: WOOD_GEARS, STONE_GEARS, IRON_GEARS, DIAMOND_GEARS, CHIP_ONE, CHIP_TWO, CHIP_THREE. Every guidance exposes two numbers. Base defaults: fall-off range 10, chance to fail 0.5.

| Module | Fall-off range (blocks) | Chance to fail | Craft |
|---|---|---|---|
| Wood gears | 100 | 0.5 (default) | wood gears, sticks, planks, clock (`GSG/PCP/GSG`) |
| Stone gears | 100 | 0.43 | stone gears, stone rods, stone, clock |
| Iron gears | 80 | 0.30 | iron gears, rods, plates, clock |
| Diamond gears | 10 | 0.10 | diamond gears, rods, iron plates, clock |
| Chip 1 | 50 | 0.05 | `CrC/wtw/CrC`: basic circuit, redstone, tin wire, iron gears |
| Chip 2 | 40 | 0.03 | advanced circuit, copper wire |
| Chip 3 | 20 | 0.03 | elite circuit, gold wire |

Reading the table: gears are cheap and sloppy; chips are reliable. But diamond gears (10) beat every chip on fall-off, and the ordering is not monotonic (chip 1 at 50 is worse than chip 3 at 20 yet better than iron gears at 80). The design intent is clear (higher tier = less scatter and less failure); the numbers are not tuned.

**What guidance really does (important for borrowing):**

- **Fall-off range** is the only value used. It is the scatter radius when a tracked missile re-enters over its target (section 7). A missile with no guidance scatters by 100.
- **Chance to fail is never read by any code** (ICBM-A1-05). The interface method is defined and implemented, and nothing calls it.
- Guidance modules have no `update` behavior. The entity calls `guidance.update(missile)`, and the base implementation is empty.

## 6. Triggers (`missile/parts/trigger/*`)

Triggers sit inside a warhead (one per warhead, `ITriggerAccepter`). Three types, all "impact" triggers, item damage 0 to 2:

| Trigger | Mass | Toggle | Notes |
|---|---|---|---|
| Mechanical impact | 54,431.1 (120 lb) | toggleable | default off (`enabled` false) |
| Electrical impact | 27,215.5 (60 lb) | toggleable | |
| Redstone impact | 9,071.85 (20 lb) | cannot be toggled, always reports enabled | |

Behavior (MIT-portable): [`ImpactTrigger.java` L45-L50](https://github.com/BuiltBrokenModding/ICBM/blob/13e6a5245c76f5599baca521b462168b9ebe75d8/src/main/java/com/builtbroken/icbm/content/missile/parts/trigger/impact/ImpactTrigger.java#L45-L50).

1. `shouldTrigger` answers true only for an *impact* cause (entity or block impact). Any other cause (fire, shot-down, remote) returns false. So a warhead with a trigger installed will **not** go off when its missile is destroyed in flight.
2. For an impact, kinetic energy = 0.5 x mass x velocity squared, where mass is looked up from the VoltzEngine mass registry for the cause's source and velocity is the cause's recorded speed.
3. It triggers if KE is between `minimalForce` and `maximalForce`. Defaults: 0 and Integer.MAX_VALUE, so every impact triggers. No subclass sets other bounds, and `failureChance` is declared but unused, as is the `enabled` flag (ICBM-A1-20). The trigger system is a skeleton for a planned "sensitivity" design.

## 7. Flight behavior from the combined modules

This is the whole picture. It is much simpler than the module list suggests.

1. **Launch** (`TileAbstractLauncher.fireMissile(target)`, [silos-and-doors.md](silos-and-doors.md)): needs `canLaunch()`. The entity is placed at the launcher plus the launcher's launch offset, with upward motion `motionY = engine.getSpeed()`, target stored, and `setIntoMotion` called (ticks in air = 1). Engine speed is therefore blocks per tick of vertical climb.
2. **Climb** uses the generic projectile motion of the base entity. There is no steering, no pitch program and no fuel burn-down. `getMaxDistance` is **not consulted during flight at all** (ICBM-A1-06). It is only an "is greater than zero" gate for launching.
3. **Leave the world**: once a missile with a target climbs past Y = `MissileTracker.MAX_SPAWN_OUT_Y` (400), the per-dimension tracker saves it to NBT and removes the entity (`noReport` set, so the launcher is not told it died). It also leaves the radar registry.
4. **Respawn timer**: `respawnTicks = floor(horizontalDistance / engineSpeed) + 1`, using the tracker's saved position and target, speed 1 if no engine. So engine speed sets the travel time and nothing else.
5. **Respawn over target** when the tick count passes the timer *and the target chunk is loaded* (there is a TODO to add chunk loading).

### Respawn-over-target math (MIT-portable)

Permalinks: [`MissileTracker.java` L91-L117](https://github.com/BuiltBrokenModding/ICBM/blob/13e6a5245c76f5599baca521b462168b9ebe75d8/src/main/java/com/builtbroken/icbm/content/missile/tracking/MissileTracker.java#L91-L117) and [`MissileTrackingData.java` L41-L46](https://github.com/BuiltBrokenModding/ICBM/blob/13e6a5245c76f5599baca521b462168b9ebe75d8/src/main/java/com/builtbroken/icbm/content/missile/tracking/MissileTrackingData.java#L41-L46).

```
travelTicks = floor( horizontalDistance(start, target) / speed ) + 1       // speed = engine speed, or 1
accuracy    = guidance != null ? guidance.fallOffRange : 100
spawnPoint  = (target.x, 400, target.z) - (0, 10 + 100 * rand(), 0)         // 290..390 high
spawnPoint += ( accuracy*(rand() - rand()),                                 // each axis: triangular
                accuracy*(rand() - rand()),                                 // distribution in [-accuracy, +accuracy]
                accuracy*(rand() - rand()) )
motion = (0, -1, 0); spawn; setIntoMotion()                                  // falls straight down at 1 block/tick
```

Notes: Y is scattered too. The missile is not aimed (there is a TODO), so impact is wherever it falls: the X and Z offset of the spawn point is the miss distance. Each axis is the difference of two uniforms, so scatter clusters near zero. Fall time from 290 to 390 blocks is 290 to 390 ticks.

6. **Impact** (`EntityMissile.onImpact`): triggers the warhead (scaled by cause), lets the engine "blow" (`onDestroyed`, default false), then decides drops: a crashed-missile block can be placed, or scraps drop, depending on the explosive's `IBlastHandler` hooks (`doesDamageMissile`, `allowMissileToDrop`, `doesVaporizeParts`, `getDropsForMissile`). Default drops are 4 eighth-sheet-metal items plus engine, warhead and guidance **only if the blast did not damage the missile**.

## 8. Flight data classes (unfinished)

`missile/data/flight/` has `FlightData` (vector helpers), `FlightDataDirect` (straight line) and `FlightDataArk` (apex then dive). **Nothing instantiates them.** `FlightDataDirect.updatePath` never computes its motion (ICBM-A1-01) and `FlightDataArk` depends on a tick counter that its parent never advances. The arc/direct choice is passed as a boolean to `setTarget` and ignored (ICBM-A1-02). These are the unfinished halves of a trajectory system.

Intended (reading the dead code): Ark mode climbs, then when Y >= max(1000, distance/2) or half the distance is gone, re-aims at the target and descends at speed 1. The 1,000-block apex is above the build limit, so only the half-distance rule could ever fire.

## 9. Disabled and unfinished in the module system

- `WarheadCasings.enabled` is false for MEDIUM and LARGE: the warhead block lists hide them, though the classes, caps (64, 128) and missile JSON exist.
- `large.json` is registered with id `icbm:large`, so the LARGE default-casing lookup fails (ICBM-A1-03).
- The comment in `MissileSize` says it will be replaced by a data system; the JSON casing data already half does this.
- `Missile.getMaxHitPoints`, `getHeight`, `getWidth` carry "TODO implement" comments and fall back to constants.
- `ICBM.java` has the plain `itemMissile` registration commented out (in the item registration block of `ICBM.java`); the missile item comes from JSON instead.
- A missile cart (rail) item exists only when the AssemblyLine mod is present.

## 10. Observations for D8

See the "Ideas worth borrowing" section of [index.md](index.md).
