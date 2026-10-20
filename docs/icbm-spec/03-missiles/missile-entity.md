# Missile Entities

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. This sheet covers the shared missile base (`EntityMissile`), the missile capability (`CapabilityMissile`), the EMP receiver (`CapabilityEmpMissile`) and the four concrete entities in this part: the **explosive missile**, the **generic (actionable) missile**, the **surface-to-air (SAM) missile** and the **held-item missile**. How they fly is [flight-logic.md](flight-logic.md); how they pick targets and survive unloaded chunks is [targeting-and-tracking.md](targeting-and-tracking.md); the items that spawn them are [missile-item.md](missile-item.md). The cluster missile (a second user of the generic entity) is part b of this area.

Conventions: "server" and "client" are labeled where it matters. Capabilities, DataParameters, the entity registry and the event bus are *1.12-only mechanisms*; the behavior they produce is described.

## 1. Identity

| Item | Value |
|---|---|
| Base classes | `lib/projectile/EntityProjectile` (arrow-like motion, impact, in-ground state) over `prefab/entity/EntityICBM` (health). `content/missile/entity/EntityMissile<E>` adds the missile capability, EMP receiver, riding, events and sync |
| Concrete classes | `content/missile/entity/explosive/EntityExplosiveMissile` (deprecated in favor of the generic one, but still the only one the missile item spawns), `.../explosive/EntityMissileActionable`, `content/missile/entity/anti/EntitySurfaceToAirMissile`, `content/missile/entity/itemstack/EntityHeldItemMissile` |
| Entity registry names (`content/reg/EntityReg.java`, `api/refs/ICBMEntities.java`) | `icbmclassic:explosive_missile`, `icbmclassic:generic_missile`, `icbmclassic:surface_to_air_missile`, `icbmclassic:held_item_missile`. An older name `icbmclassic:missile` is remapped to the explosive missile (renamed in 4.2.0) |
| Tracking (client sync) | range 500 blocks, update every 1 tick, velocity updates on, for all four |
| Size | 0.5 x 0.5 blocks. Fire-immune. No frustum culling (`ignoreFrustumCheck`) on explosive and generic |
| Display name | Explosive: `missile.<explosive registry key>.name`, for example `missile.icbmclassic:nuclear.name` = "Nuclear Missile"; fallback `missile.icbmclassic:generic.name` = "Missile". Generic: the same scheme using the action data's registry key. SAM and held-item missiles have no entity-name lang key of their own |
| Item form | `toStack()`: explosive = the explosive capability's stack; generic = the stored `originalStack`; SAM = a new SAM item; held = a held-item missile item carrying the held stack and action mode (see missile-item.md) |
| Creative tab / oredict | none (entities) |

## 2. Obtaining

Entities are spawned, never crafted.

| Entity | Spawned by |
|---|---|
| Explosive | `lib/capability/missile/CapabilityMissileStack.newMissile` (on a missile item, `ItemMissile`): builds the entity and copies the stack into its explosive capability. Used by launchers (04), the rocket launcher (part b), and the missile tracker when it re-enters a simulated missile |
| Generic | the cluster missile stack (`content/cluster/missile/CapabilityClusterMissileStack`, part b). The tracker cannot re-spawn it (bug 22) |
| SAM | `CapabilitySAMStack.newMissile` (stack id string `icbmclassic:missile.sam`) |
| Held-item | `CapabilityHeldItemMissile.newMissile` (stack id `icbmclassic:missile.item.held`): copies the held stack into slot 0 and sets the action mode |

All four go through the same sequence in the caller: build the entity, set target data, flight logic and source, call `launch()`, then `spawnEntity`.

## 3. Numbers

### Health

`EntityICBM` holds `HEALTH` and `MAX_HEALTH` data parameters (both default 1.0, synced to the client). `EntityMissile` turns health on.

| Config key (`icbmclassic/missile`) | Default | Intended use |
|---|---|---|
| `health_tier_1` | 50 | Tier ONE explosive, and the fallback for anything unknown |
| `health_tier_2` | 80 | Tier TWO; also written as max health of the SAM and held-item missiles |
| `health_tier_3` | 100 | Tier THREE |
| `health_tier_4` | 200 | Tier FOUR |

What actually happens (bugs 01, 02, 04):

| Entity | Initial HP | Max HP | Notes |
|---|---|---|---|
| Explosive | **50 always** (set in the constructor, before the explosive is known, so tier 1) | tier-based `getMaxHealth()` once the stack is set (50/80/100/200) | HP never rises to the tier value; tiers 2 to 4 only raise the clamp. After a reload HP is clamped to tier 1 again |
| Generic | 1 | 1 (data-parameter default), unless the cluster stack calls `initHealth` (part b) | |
| SAM | **1** | 80 (set with `setMaxHealth` only) | |
| Held-item | **1** | 80 (same) | |

### Other constants

| Name | Value | Where |
|---|---|---|
| In-air kill time (explosive, generic) | 144,000 ticks (2 hours), then silent `destroy()` with no blast | constructors; the base default is 1,200 ticks |
| In-ground kill time | 1,200 ticks (1 minute), base default | `EntityProjectile` |
| Fall-out-of-world kill | `posY < -100` | `EntityProjectile.shouldExpire` |
| Impact damage to a hit entity | `ceil(speed * impact_damage_scale)`, capped by `impact_damage_limit` when it is not negative | `EntityMissile.getImpactDamage` |
| `impact_damage_scale` | 20 (speed 2 gives 40; a descent at 3.9 gives 78) | config |
| `impact_damage_limit` | -1 (no limit); 0 disables | config |
| Knockback on a living hit entity | 0.6 horizontal along the missile's heading, +0.1 vertical (the two constants have swapped names in the source) | `applyKnockBack` |
| Base air resistance / gravity | x0.99 per tick, -0.05 per tick (applied only when the flight logic allows) | `EntityProjectile` |
| Hit-test padding | target box grown by 0.3 on each axis | `EntityProjectile.onUpdate` |
| Held-item `block_damage_multiplier` | 4 | `ConfigHeldItemMissile` |
| SAM `attack_damage` | 100 | `ConfigSAMMissile` |
| SAM `speed` | 4 blocks/tick | `ConfigSAMMissile` |

## 4. Behavior

### Per-tick order (all missiles, server unless noted)

1. Explosive only: `TargetRangeDet.update()` first (air-burst check, see the targeting sheet).
2. `EntityProjectile.onUpdate` (shared):
   1. `checkInGround`: look at the block at the entity's position; if it has a collision box containing the position, the entity is "in ground".
   2. In ground: count ticks, destroy after 1,200. In air: count `ticksInAir`; destroy (no blast) when `ticksInAir` reaches the kill time or `y < -100`.
   3. Ray-trace for blocks from the position to position + heading * (speed + 1), so impacts are seen one block early. Blocks without a collision box are ignored (no liquid stop).
   4. Entity pass: every entity in the box (position + motion, grown 1) that can be collided with (not a rider of this missile, not the shooter, not an `EntityPlayerSeat`) is tested against its box grown by 0.3; the nearest wins and replaces the block hit if there is one.
   5. If something was hit and `MissileEvent.PreImpact` is not canceled: block hits go to `handleBlockCollision`, entity hits to `handleEntityCollision`, then `MissileEvent.PostImpact` fires. Forge's `onProjectileImpact` can also cancel.
   6. `updateMotion()`.
3. The generic missile then runs its ticking trigger (below). The SAM and held-item missiles do their extras before calling the shared update.

### `updateMotion` (EntityMissile override)

- If `doFlight` is set: run the flight logic's tick; if its `shouldRunEngineEffects` is true, spawn engine smoke (client proxy call) and play `MISSILE_ENGINE` at the missile every tick; call the (empty) flight-update hook of the explosive registry.
- Motion is applied by the base class on the server only (the client does not move the entity itself): position += motion, optional rotate-toward-motion (only if the flight logic asks; default none), then friction and gravity **only if** the flight logic's `shouldDecreaseMotion` allows (a null logic allows), then block-collision callbacks.

### Impact

- `onImpact` is final and runs once (`hasImpacted`): it logs "Missile[id] (x, y, z, dim) impacted at (x, y, z)" at INFO, then calls `actionOnImpact`.
- The default `actionOnImpact` is `destroy()` (dismount riders, remove entity).
- Explosive: `destroy()` first, then the explosion at the exact hit point with an `ActionSource` whose cause is the missile entity. The explosive capability guards with `isExploding`, so a second call is ignored.
- Generic: runs `mainAction` at the hit point; only if the status is not BLOCKING does it then destroy. A blocking status leaves the missile alive (for example a condition not met).
- Held-item: see below. SAM: no impact handler of its own, so the default applies (destroy, no blast).
- Entity hit: the missile applies `DAMAGE_SOURCE` (`icbmclassic:projectile`, a projectile type) with the damage above, knocks back living entities, then impacts. The held-item missile overrides this.
- A **chicken** hit is special (all missile types): if the missile has no vehicle, play `MEEP` and the chicken **rides the missile**; the chicken's `CapSpaceChicken` flag is set to "space" (a rendering easter egg). No impact happens (bug 09).

### Taking damage

`EntityICBM.attackEntityFrom` needs `hasHealth`, damage > 0, and `canDamage` (not invulnerable and alive; the creative-player check looks at the missile itself, so it never fires). HP is reduced; at 0, `onDestroyedBy(source, damage)` runs.

| Entity | `onDestroyedBy` |
|---|---|
| Explosive | `destroy()`; then, **only if** the source is explosion or fire damage, run the explosive at the missile position (cause: itself). Other damage (arrows, melee, a SAM hit) just removes it. Fire damage normally cannot reach it (fire-immune) |
| Generic | only if the source is explosion or fire damage: run `mainAction`; if the status is not BLOCKING, destroy. **Any other lethal damage does nothing** (HP stays 0, and every later hit repeats the call). Bug 03 |
| SAM, held-item | default: `destroy()` |

An explosion that does 50 damage to an explosive missile sets off its warhead, so missiles chain-detonate. Explosion damage to entities is outside this area (02).

### EMP interaction (`CapabilityEmpMissile`; see [05 emp-tower](../05-detection-defense/emp-tower.md))

All `EntityMissile` subclasses expose the EMP capability. When the EMP action reaches a missile (`applyEmpAction` with the missile's own position and `doAction` true), and `ConfigBlast.emp.missiles.enabled` is on and the entity is alive, up to three random rolls run in order, stopping at the first that passes:

1. Roll <= `missileKillChance` (0.5): `setDead()` (the radar entry is removed); then roll <= `engineFullBlowChance` (1.0): a vanilla explosion of strength `engineFullBlowStrength` (1.0), fire off, at the missile.
2. Else roll <= `engineKillChance` (0.8): replace the flight logic with `DeadFlightLogic(0)` ("guidance fried": no fuel, so gravity and drag apply and the missile falls).
3. Else roll <= `missileTriggerChance` (0.1): fire the warhead where the missile is. Explosive = the explosive capability's `doExplosion`; generic = the main action; SAM = a vanilla explosion. **The entity is not removed.** For the explosive missile the capability's `isExploding` flag stays set, so the missile keeps flying and its later impact makes no second blast (bug 05, which resolves ICBM-05-14).

Product at defaults: 50% killed (with a size-1 blast), 40% falls, 1% fires the warhead and flies on, 9% unaffected.

### Riding (player seat)

- Right-click (server): if the missile has no passenger (or the clicker is the passenger) and `MissileRideEvent.Start` is not canceled, the player starts riding. Explosive and generic first offer the click to `EX_MISSILE_REGISTRY.onInteraction`, which is a stub that always returns false (bug 07).
- Launchers can move a player from their `EntityPlayerSeat` onto the new missile (04). The rocket launcher puts a sneaking player on the missile (part b).
- Dismounting is routed through `MissileEventHandler.onEntityMount`: for a player, `MissileRideEvent.Stop` is posted and a cancel keeps the player seated (hook for addons).
- A missile with a rider is never simulated by the tracker, and an arc missile uses the full arc even for long distances.
- Seat height (`getMountedYOffset`): `height` for the old ballistic logic before it has flown, `height / 10` for dead-stick, otherwise `height / 2 + motionY`. The rider rises with climb speed (several blocks) and sinks with descent speed (bug 08).
- A missile never collides with its own passengers (`isRider`, recursive).

### Explosive missile extras

- Fields: `explosive` (the shared explosive-on-an-entity capability, 02) and `targetRangeDet` (air-burst, targeting sheet).
- Capabilities exposed: EMP, missile, explosive.
- Spawn data: the explosive tag is written before the base tag.

### Generic missile extras

- Holds a `PotentialAction mainAction` with a field provider exposing `IMPACTED` (has hit), `HOST_ENTITY`, `HOST_POSITION` and `TARGET_POSITION` (from the target data, or null). Its precondition (a timer or proximity condition, 02) decides when it fires.
- **Ticking trigger:** after the shared update it updates the action's condition (server) and calls `doAction` at the current position **every tick**. If the status is not BLOCKING (the action ran), it destroys itself. A generic missile whose action has no gating condition would fire on its first tick; the "air-burst" behavior of cluster missiles comes entirely from that condition (part b).
- `originalStack` is the item `toStack()` gives back.

### SAM missile extras (server)

1. `scanLogic.tick()` (target scan; see the targeting sheet).
2. If a target exists, the SAM has not started following, and its current flight logic says `canSafelyExitLogic()` (the logic is dereferenced without a null check, bug 12): mark "following", reassign its own target data, and switch to `FollowTargetLogic` with the SAM `fuel` (200 ticks).
3. Proximity kill: if the target is within `speed` (4) blocks, deal `attack_damage` (100) as an entity damage source of type `missile` (not an explosion), then `setDead()` the SAM. No blast, no sound.
4. The normal update still runs after step 3 on the same tick, even though the SAM was just removed.
- The target it carries is its own `scanLogic`; before it first follows, the launcher's ballistic target applies (04).
- `getCapability` is a pass-through (a TODO would expose an anti-ballistic capability).
- The radar does not count SAMs as incoming missiles (05).

### Held-item missile extras (server)

- A one-slot `ItemStackHandler` exposed as the item-handler capability, and an action mode (`PRIMARY`, `SECONDARY`, `PRIMARY_FIRST`, `SECONDARY_FIRST`).
- The held item must pass `HeldItemMissileHandler.isAllowed` (a config ban/allow list; default ban list: balloon, parachute, cluster missile, explosive missile, SAM).
- It acts through the shared `FakePlayer` at the impact point (offset by the hit face).
- **On a block** (`specialHandleBlock`, once per impact while no action has been used):
  - Primary: if the item's `onBlockStartBreak` returns true, count it as used (shears on leaves). Else if the block can be harvested by the fake player and the item has at least one tool class: `digSpeed = destroySpeed * impactSpeed * block_damage_multiplier`; if `hardness <= digSpeed`, break and harvest the block (a special case calls Dynamic Trees' `futureBreak`). The remaining stack goes back to the slot.
  - Secondary: right-click: `onItemUseFirst`, then `onItemUse` if that passed. This path does **not** set `hasUsedAction`.
  - Mode logic: PRIMARY runs primary only; SECONDARY secondary only; PRIMARY_FIRST runs primary and then **always** secondary; SECONDARY_FIRST secondary then always primary. The tooltip says "if nothing happens" (bug 14).
  - Exceptions are caught, logged, and mark the action as used.
- **On an entity** (`onImpactEntity`): primary = a melee hit with the item's attack-damage attribute plus the enchantment bonus against the creature type, times impact speed (living targets only), attacker = the shooter or the fake player, fire aspect sets the target on fire, then the item's `hitEntity`. Secondary = `interactOn` with the main hand, then the off hand. Used stacks are re-stored. Then `onImpact`.
- `actionOnImpact`: if still alive, **drop the missile as an item** (with the possibly used or damaged held stack), then destroy. A held-item missile is reusable.
- Impact damage is 0. Homing is a TODO.

## 5. Persistence and sync

Entity NBT, the spawn packet and the description packet use the same save handlers.

| Key | Type | Owner | Meaning |
|---|---|---|---|
| `health`, `health_max` | float | `EntityICBM` | HP and max (the explosive missile computes max itself and ignores the saved value) |
| `ground` | compound | `EntityProjectile` | in-ground state (position, side, block state) |
| `ticks` { `air`, `ground` } | compound of ints | `EntityProjectile` | the **kill times**, not the counters; `ticksInAir` is not saved and restarts at 0 |
| `missile` | compound | `EntityMissile` | the missile capability, below |
| `missile.flags.doFlight` | boolean | `CapabilityMissile` | the flight logic may run |
| `missile.target` | buildable object | `CapabilityMissile` | target data through the target registry (`basic`, `ballistic`; the SAM's `anti.missile` is registered with a null builder and cannot be restored) |
| `missile.flight` | buildable object | `CapabilityMissile` | flight logic and its `next` chain through the flight registry |
| `missile.source` | compound | `CapabilityMissile` | the `ActionSource`: `dimension`, `pos`, `cause`. Causes are buildable: `entity` (`name`, `uuid`, `player`), `block` (`level`, `pos`, `state`), `block.redstone` (adds `side`); each also has `name` and a `parent` cause |
| `explosive` | compound | explosive missile | the explosive capability (02) |
| `potential_action`, `original_stack` | compound, item | generic missile | main action state, and the item to return |
| `inventory`, `action_mode` | item handler, string (enum name) | held-item missile | held stack and mode |

The registry-key layout of buildable objects is 01-core.

- **Client sync:** every `setFlightLogic` sets `syncClient`; on the next tick the server sends the codex packet `icbmclassic:missile` / `description` (id 1) to everyone nearby, carrying the `missile` tag. Spawn data carries the same tag. Motion goes through the vanilla tracker (every tick).
- Not persisted: `hasImpacted`, `ticksInAir`, `hasStartedFollowing` (SAM), the SAM target queue, `hasUsedAction` (held), the collision ignore list.

## 6. UI

No GUI. Tooltips belong to the items (missile-item.md). The entity name above appears in death messages. Lang keys used here: `missile.<key>.name`, `missile.icbmclassic:generic.name`.

## 7. Visuals

Rendering is `client/render/entity/RenderMissile` (07). It reads `toStack()` for the model (the held-item missile also draws its held item as a second layer). The render box is grown 5 blocks; the render distance rule is `edge length * 10 * 64 * weight`. Engine smoke comes from the client proxy using the flight logic's color (`engineSmokeRed/Green/Blue`: white by default; the follow logic uses 1.0 / 0.5 / 0.5). Pad smoke during warmup comes from the warmup logic.

## 8. Audio

| Sound | Trigger | Volume / pitch |
|---|---|---|
| `MISSILE_LAUNCH` | `launch()` (also on a tracker re-entry, bug 34) | 1.0 / `(1 + r) * 0.7`, r a random in +/-0.2 |
| `MISSILE_ENGINE` | every tick while engine effects run | `min(1, ticksInAir / 40)` / `(1 + r) * 0.7` |
| `MEEP` | a chicken boards a missile | 2.0 / 1.0 |

Sound files and lengths: 07-visuals-audio.

## 9. Relationships

| Area | Link |
|---|---|
| 01-core | Entity base, `NbtSaveHandler`, buildable registries, packet codex, events (`MissileEvent`, `MissileRideEvent`) |
| 02-explosives | Explosive capability on an entity (`doExplosion`, `isExploding`), `PotentialAction`, action causes, blast sizes |
| 04-launch-control | Builds the flight chain and target data, applies inaccuracy, moves the rider, sets the source |
| 05-detection-defense | `RadarRegistry` add at launch and remove at death; the EMP action calls the receiver |
| 07-visuals-audio | Renderer, models, sounds |
| This area | flight-logic, targeting-and-tracking and missile-item sheets; part b (cluster, rocket launcher, cargo) |

## 10. Bugs

Numbered list in [index.md](index.md#suspected-bugs). Entity-related: 01 to 09 and 12 to 16.
