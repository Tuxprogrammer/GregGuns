# Projectile Base (`lib/projectile`)

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. This sheet describes the shared projectile base once. The entities built on it are the missiles ([missile-entity.md](missile-entity.md)), the bomblet ([cluster-missile.md](cluster-missile.md)) and the parachute and balloon ([cargo.md](cargo.md)). The grenade and the bomb cart do **not** use it (the grenade is a plain `Entity`, the cart is a vanilla TNT minecart).

The package also holds the **projectile data registry**: a small system that turns an item stack into the entity a cluster missile, parachute or balloon should spawn. Capabilities and the registry events are *1.12-only mechanisms*; the sheet describes what they produce.

## 1. Identity

| Item | Value |
|---|---|
| Classes | `lib/projectile/EntityProjectile<P>` (755 lines, abstract), `InGroundData`, `ProjectileDataRegistry`, `ProjectileItemConversion`, `CachedProjectileData`, `CapabilityProjectileStack`, `ProjectileStack`; `vanilla/ArrowProjectileData`, `SpectralArrowProjectileData`, `TippedArrowProjectileData`, `ItemProjectileData`, `EntitySpawnProjectileData` |
| Superclass | `prefab/entity/EntityICBM` (health data parameters, `hasHealth` flag, `attackEntityFrom`, `destroy`; see [missile-entity.md](missile-entity.md) "Taking damage") |
| Interfaces | vanilla `IProjectile`, `IMissileAiming` (`api/missiles`), `IEntityAdditionalSpawnData`, `IProjectileThrowable` (`api/missiles/projectile`) |
| Subclasses (all in this spec) | `EntityMissile` and its four concrete missiles, `EntityBombDroplet`, `EntityParachute`, `EntityBalloon` |
| Registry names of the data types | `minecraft:arrow.normal`, `minecraft:arrow.spectral`, `minecraft:arrow.tipped`, `minecraft:item`, `minecraft:entity`; ICBM's own: `icbmclassic:bomblet`, `icbmclassic:holder.parachute`, `icbmclassic:holder.balloon` |
| Damage source | `icbmclassic:projectile` (projectile type), the default for entity hits |
| Creative tab / oredict | none (base classes) |

## 2. Obtaining

Not obtainable. Entities of subclasses are spawned by their items or by actions; the registry is filled during startup. The data registry is built in `ICBMClassic.handleProjectileDataRegistry` (registers the three ICBM data types), then `registerVanillaDefaults` runs late, fires `ProjectileDataRegistryEvent` for other mods, and locks.

## 3. Numbers

| Name | Value | Where |
|---|---|---|
| Entity size | 0.5 x 0.5 blocks (subclasses override: bomblet 0.25) | constructor |
| In-air kill time | 1,200 ticks (`ICBMConstants.TICKS_MIN`) | `inAirKillTime` default; missiles raise it |
| In-ground kill time | 1,200 ticks | `inGroundKillTime` default |
| Fall-out-of-world kill | `posY < -100` | `shouldExpire` |
| Air resistance | x0.99 per tick | `getAirResistance` |
| Gravity | -0.05 per tick | `DEFAULT_GRAVITY` |
| Entity hit padding | target box grown 0.3 on each axis | `onUpdate` |
| Shooter immunity | the shooter cannot be hit for the first 5 ticks of flight | `onUpdate` |
| Block look-ahead | the block ray extends 1 block past the motion step | `onUpdate` |
| Default impact damage | `ceil(speed * 2)` | `getImpactDamage` |
| Knockback | 0.6 along the horizontal heading, +0.1 up (the two constants have swapped names in the source) | `applyKnockBack` |
| Portal cooldown / max portal time | 10 / -1 | overrides |
| Render distance | `average edge length * render weight * 256` | `isInRangeToRenderDist` |
| Rotation smoothing (when aligned to motion) | 5% of the way to the target angle per tick | `rotateTowardsMotion(0.05)` |
| Aim randomness | gaussian * 0.0075 * `random`, sign flipped half the time, per axis | `shoot` |

## 4. Behavior

### Tick (`onUpdate`)

1. `checkInGround()`: look at the block at the entity's block position (or at the stored in-ground position). If the block is not air and its collision box contains the entity's position, the entity is "in ground" (`InGroundData` holds position, side and block state; the side is always UP, a TODO). The check re-runs only when there is no ground data or the block state at the stored position has changed. If the entity was in the ground and no longer is (the block was removed), its motion is multiplied by a random 0 to 0.2 per axis and both tick counters reset.
2. **In ground:** count `ticksInGround`; destroy when `shouldExpire()`.
3. **In air:** count `ticksInAir`; destroy and stop when `shouldExpire()`. Otherwise run the collision pass below, then `updateMotion()`.

### Collision pass (MIT-portable)

Permalink: [EntityProjectile.java#L225-L307](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/lib/projectile/EntityProjectile.java#L225-L307).

```
v      = |motion|
rayEnd = pos + motion / v * (v + 1)            // one block longer than the step
blockHit = world.rayTraceBlocks(pos, rayEnd, stopOnLiquid=false, ignoreNoBox=true, last=false)
entityEnd = blockHit ? blockHit.point : pos + motion
best = none
for e in entities in (myBox moved by motion, grown 1):
    if shouldCollideWith(e) and (e != shooter or ticksInAir >= 5):
        hit = e.box.grow(0.3).calculateIntercept(pos, entityEnd)
        if hit and (best none or dist(pos, hit) < bestDist): best = e
if best: hit = entity hit on best
if hit and not ignoreImpact(hit):
    entity hit: aim point = best.box.calculateIntercept(pos, best.pos)   // may be null
        if Forge ProjectileImpactEvent not canceled: handleEntityCollision; postImpact
    block hit: if event not canceled: handleBlockCollision(hit, v); postImpact
updateMotion()
```

Notes on the algorithm:
- Impacts are therefore seen up to one block before contact. The entity search uses the block hit point as the far end, so an entity behind a wall is not hit.
- `shouldCollideWith` default: the entity can be collided with (`canBeCollidedWith`) and is not an `EntityPlayerSeat`. Bomblets additionally skip other bomblets. Parachutes and balloons additionally skip the shooter.
- `ignoreImpact` default: false. Parachute and balloon return true for any entity hit, so they only ever react to blocks (a nearer entity hit replaces the block hit and is then ignored, which also drops the block hit that tick).
- The ray uses `stopOnLiquid = false`, so liquids do not stop a projectile; blocks with no collision box are skipped.

### Block hit (`handleBlockCollision`)

1. Record the block as in-ground data (position, side from the ray, block state).
2. `specialHandleBlock` asks the shared `ProjectileBlockInteraction` (`lib/world`, `01-core`) whether the block does something special (it exists for ender gateways; the reaction can stop the handling or continue without an impact).
3. Nudge the position to the hit point plus a half-width offset (`moveTowards`; bug ICBM-03-41).
4. Call the block's `onEntityCollidedWithBlock`.
5. Unless the entity is changing dimension, dead, in a portal, or the reaction said "continue, no impact": `onImpactTile` then zero the motion. `onImpactTile` defaults to `onImpact`, which defaults to `setDead` (no effect).

### Entity hit (`handleEntityCollision`)

Server only: damage = `getImpactDamage(entity, speed, hit)` (default `ceil(speed * 2)`), source = `getImpactDamageSource` (default the projectile source). If the target accepts the damage and is a living entity, apply knockback. Then `onImpact(hit)` (default `setDead`).

### Motion (`updateMotion`, server only, skipped when `freezeMotion`)

1. Position += motion.
2. If `shouldAlignWithMotion()` (default true): set yaw/pitch toward the motion direction, 5% per tick. The pitch convention is `atan2(motionY, horizontal speed)`, so **positive pitch means pointing up**, the opposite of Minecraft's own convention (matters for [cluster-missile.md](cluster-missile.md), ICBM-03-55).
3. Motion *= air resistance, then `motionY -= gravity` (`decreaseMotion`; subclasses override either number or the whole method).
4. `setPosition`, then `doBlockCollisions`.

Clients do not move the entity; they take position updates from the tracker (`setPositionAndRotationDirect` is overridden to snap to the sent position and rotation, no interpolation).

### Aiming and throwing

- `shoot(x, y, z, speed, random)` normalizes the direction, adds the gaussian error when `random > 0`, scales by speed, sets motion, sets yaw/pitch to match and resets `ticksInGround`.
- `initAimingPosition(x, y, z, yaw, pitch, offsetScale, speed)` (the `IMissileAiming` entry used by launchers and the rocket launcher): records the source position, moves the entity 0.16 * offsetScale sideways from the yaw and 0.1 down, builds a unit heading from yaw and pitch, and shoots at `speed` with no randomness.
- `initAimingPosition(shooter, offsetScale, speed)` additionally stores the shooter and uses the shooter's eye position and rotation.
- `throwProjectile(...)` (the `IProjectileThrowable` entry used by the throwable cargo items) calls the position form with offset 1 and the given velocity; the `random` argument is ignored (a TODO) and the shooter is **not** set.
- A deprecated `init(shooter, target, multiplier, random)` aims at a living target (eye height minus 0.1; aims at one third of the target's height; adds `distance * 0.2` of lift). Nothing in this part calls it.

### Expiry and destruction

`shouldExpire()` = `ticksInAir >= inAirKillTime || ticksInGround >= inGroundKillTime || posY < -100`. `destroy()` dismounts the entity from its vehicle, removes its passengers, then `setDead`. No blast or drop happens in the base class.

### Health

The projectile reports `canBeDestroyed` = `hasHealth`. The base default for `hasHealth` is false (from `EntityICBM`); missiles turn it on, the bomblet explicitly keeps it off, so a bomblet can never be shot down (ICBM-03-57).

## 5. Persistence and sync

NBT (via `NbtSaveHandler`):

| Key | Type | Meaning |
|---|---|---|
| `ground` | compound | `InGroundData`: `pos` (block pos), `side` (facing), `state` (block state) |
| `ticks.air` | int | the **in-air kill limit** (`inAirKillTime`), not a counter |
| `ticks.ground` | int | the in-ground kill limit |
| (`EntityICBM`) `health`, `health_max` | float | health |

Neither tick counter is saved, so a reloaded projectile starts its lifetime again (ICBM-03-42). The shooter entity, its UUID and the source position are declared but their save block is commented out, so they are lost on reload (ICBM-03-43).

Spawn packet (`IEntityAdditionalSpawnData`): one boolean, `freezeMotion`. Subclasses append their own data.

Health and max health are entity data parameters (client-synced). Position and velocity ride the normal entity tracker.

## 6. UI

None.

## 7. Visuals

None in the base. Subclass renderers: bomblet and balloon render the held item as a billboarded item (balloon) or a rotated item entity (bomblet); the parachute renders its item scaled up when carrying a non-item passenger. Catalog in [cluster-missile.md](cluster-missile.md) and [cargo.md](cargo.md); full texture catalog in `07-visuals-audio`.

## 8. Audio

None in the base (the `playSound` call for block impacts is a TODO comment).

## 9. Relationships

| Direction | What |
|---|---|
| Used by | missiles ([missile-entity.md](missile-entity.md)), bomblets, parachutes, balloons |
| Uses | `01-core`: `NbtSaveHandler`, `ProjectileBlockInteraction`, `ICBMClassicAPI.PROJECTILE_DATA_REGISTRY`, `BuildableObject(Registry)`; `EntityPlayerSeat` (excluded from collision; see [cargo.md](cargo.md)) |

### Projectile data registry (`ProjectileDataRegistry`)

A `BuildableObjectRegistry` of `IProjectileData` (a recipe for creating one kind of entity), plus a table of item conversions. Used by the cluster action and the throwable cargo items.

`find(stack)` picks the data for a stack in this order:
1. Walk the conversions registered for the stack's item, in registration order; the first conversion that returns non-null wins.
2. Else, if the stack has the projectile-stack capability, return that capability's data. This can be **null** (an empty parachute), and then `find` returns null instead of falling through.
3. Else wrap the stack itself as an item entity (`ItemProjectileData`).

`spawnProjectile(data, world, x, y, z, source, allowPickup, preSpawn)`: build the entity from the data, set its position, run the caller's pre-spawn callback (the cluster action sets motion and rotation here), `world.spawnEntity`, and only then call the data's `onEntitySpawned(entity, source, MAIN_HAND)` (the cargo data adds its passenger there). Returns the entity, or null on any failure.

Registered conversions (`registerVanillaDefaults`):

| Item | Produces |
|---|---|
| Arrow | a shared `ArrowProjectileData` (tipped-arrow entity with pickup allowed or not) |
| Spectral arrow | `SpectralArrowProjectileData` (spawns a plain tipped arrow, ICBM-03-44) |
| Tipped arrow | a new `TippedArrowProjectileData` carrying the stack, applied with `setPotionEffect` |
| Snowball | meant: a `minecraft:snowball` entity-spawn data; actually null (ICBM-03-40) |
| Egg | meant: a `minecraft:egg` entity-spawn data; actually null (ICBM-03-40) |
| Spawn egg | `EntitySpawnProjectileData` with the egg's entity id, custom name and `EntityTag` merged in |

`EntitySpawnProjectileData` builds any registered entity by key; if extra NBT is given it merges it over the entity's fresh save while keeping the new entity's UUID. `ItemProjectileData` builds a dropped item (infinite pickup delay when pickup is not allowed). Each data type has its NBT: `item` (stack), `entity_id` / `custom_name` / `entity_data`.

The ICBM data types are described in [cluster-missile.md](cluster-missile.md) (`icbmclassic:bomblet`) and [cargo.md](cargo.md) (parachute and balloon). The `ProjectileStack` capability holds one `IProjectileData` and saves it under `data` through the registry (a *buildable object* node, so the type key is saved too). `CapabilityProjectileStack` is the read-only variant used for fixed items (default: the arrow data), with an empty storage (*1.12-only mechanism*).

## 10. Bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-03-40 | Cached data builder never builds (logic) | `lib/projectile/CachedProjectileData.java:18`, used at `ProjectileDataRegistry.java:63-64` | The null check is inverted: it builds only when an instance already exists, so `instance` stays null forever and the conversion returns null. Snowballs and eggs fall through to the item fallback and are spawned as inert dropped items instead of thrown entities | gameplay |
| ICBM-03-41 | Impact nudge uses the wrong axis for Y (logic) | `lib/projectile/EntityProjectile.java:422` | `vecY` is computed from `deltaX`, not `deltaY`. The half-width nudge after a block hit is skewed vertically by up to a quarter block | cosmetic |
| ICBM-03-42 | Lifetime counters are not saved; saved keys are limits (logic) | `lib/projectile/EntityProjectile.java:738-747` | `ticks.air` and `ticks.ground` store the kill limits, not `ticksInAir` / `ticksInGround`. A projectile that survives a reload gets a fresh minute (or two hours for missiles) | cosmetic |
| ICBM-03-43 | Shooter is lost on reload (logic) | `lib/projectile/EntityProjectile.java:64-75,749-754` | `shootingEntity`, `shootingEntityUUID` and `sourceOfProjectile` are never written or read (the save block is commented out). The shooter's 5-tick immunity and the parachute's "skip shooter" rule fall away after a reload. `throwProjectile` never sets the shooter at all | cosmetic |
| ICBM-03-44 | Spectral arrow data spawns a plain arrow (logic) | `lib/projectile/vanilla/SpectralArrowProjectileData.java:22` | It builds an `EntityTippedArrow` with no effect, not a spectral arrow, so the glowing effect is lost | gameplay |
| ICBM-03-45 | Impact event can receive a null hit (unverified, logic) | `lib/projectile/EntityProjectile.java:284-289` | The aim point is the entity box intercept from the projectile to the entity's position; that is null when the projectile is already inside the box. The code handles null for its own use but passes the null to Forge's `onProjectileImpact` listeners, which may not expect it | crash |
| ICBM-03-46 | Empty holder data short-circuits `find` (logic) | `lib/projectile/ProjectileDataRegistry.java:56-61` | A stack whose capability holds no data returns null instead of falling back to the item entity. The cluster action covers this by dropping the item, but other callers of `spawnProjectile(stack...)` get null | cosmetic |

Minor, not given IDs: `ProjectileItemConversion.sortStack` is stored but never used for ordering (a TODO says conversions should be sorted most complex to least); the render-distance scale `256` is a hard-coded TODO; the `readme.md` in the package says the system "will eventually be moved to a library".
