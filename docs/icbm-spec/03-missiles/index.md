# 03-missiles: missiles and carriers

## Overview

Everything that carries an explosive to its target in ICBM-Classic `prod/1.12` @ `9c1d985a`:
- **Part a:** the missile entities (explosive, generic, SAM, held-item), every flight logic (the key hard-math sheet), targeting, the missile tracker and its chunk loading, and the missile item.
- **Part b:** cluster missiles and bomblets, the bomb cart, the shared projectile base, and compact sheets for content GregGuns drops or defers: the grenade, the rocket launcher, cargo and the player seat ([D10](../../gregguns-icbm-spec/DECISIONS.md#d10-in-detail)).

Launchers are in [04-launch-control](../04-launch-control/index.md), radar and SAM triggering in [05-detection-defense](../05-detection-defense/index.md), and warheads in [02-explosives](../02-explosives/index.md).

## Contents

- [missile-entity.md](missile-entity.md): the shared missile base, the explosive, generic, SAM and held-item missile entities; health, damage, impact, EMP, riding, NBT.
- [flight-logic.md](flight-logic.md): every flight logic (warmup, move steps, arc with its full math (MIT-portable), direct, follow, dead, the old ballistic one), speeds, heights and chains.
- [targeting-and-tracking.md](targeting-and-tracking.md): target data types, the air-burst fuse, SAM target selection and interception, the missile tracker (simulation, chunk loading, re-entry; MIT-portable) and GTNH server notes.
- [missile-item.md](missile-item.md): the missile item, the meta-to-explosive map, recipes, tooltips; also the SAM item and the held-item missile item with its two dynamic recipes.
- [cluster-missile.md](cluster-missile.md): the cluster missile item, load recipe and size budget, the burst action (MIT-portable), bomblet items and entity, config, recipes, tooltip.
- [bomb-cart.md](bomb-cart.md): the bomb cart item and entity, rail and dispenser placement, fuse times, breaking and exploding.
- [thrown-and-handheld.md](thrown-and-handheld.md): compact sheets for the grenade (item, entity, dispenser) and the two rocket launchers.
- [cargo.md](cargo.md): compact sheets for the parachute and balloon cargo holders (items, entities, recipes, cargo modes), the dead cargo hook block, and the player seat entity.
- [projectile-base.md](projectile-base.md): the shared projectile entity base (collision pass MIT-portable, in-ground state, motion, aiming) and the projectile data registry.

## Key facts

### # 03-missiles (part a: missile core)

- One entity class per missile kind, all on a projectile base. Motion is applied only on the server. A missile flies by a **chain of flight logics**; the silo builds `Warmup(40 ticks)` -> climb steps -> `Arc`.
- The arc is closed-form: constant horizontal speed 0.5 block/tick (`D / ceil(2D)`), constant downward acceleration `g = H * T * (1 - 1/D) / D^3` with `T = ceil(2D)` ticks and apex `H = min(1000, base + D * scale)` above the launch altitude (`base` 160 when D > 100). It lands at the launch altitude after `T + 1` ticks; the target's height is ignored.
- Beyond `arc_distance_limit` (500 blocks, no rider) the missile climbs straight up at 2 blocks/tick and is handed to the tracker at y = 500. Arcs that peak above y = 500 (about D >= 276 from y = 64) are also handed over.
- The **tracker** removes the missile, counts down `round(dist / 10)` steps of 21 ticks (so the real speed is about 9.5 blocks/s, not "blocks per tick" as the config says), force-loads 5 chunks around the target through **one shared ticket**, and respawns the missile at y = 450 above the target falling 1 block/tick. Only `EntityExplosiveMissile` can be simulated.
- Tier health is effectively **dead**: every explosive missile has 50 HP, and the SAM and held-item missiles have 1 HP. The generic missile does not die to non-explosion damage at all.
- SAMs pick any non-SAM missile within 30 blocks (no friend or foe), steer straight at it at 4 blocks/tick with perfect aim, and kill it with 100 damage inside 4 blocks. They cannot kill a generic (cluster) missile.
- EMP at defaults: 50% kill, 40% falls, 1% fires the warhead and **flies on**, 9% nothing (resolves ICBM-05-14).
- The held-item missile is a reusable item projectile: it uses the held item through a fake player on impact and drops itself.
- Capabilities, data parameters, the entity and item registries, `WorldSavedData`, `ForgeChunkManager` and the event bus are *1.12-only mechanisms*; the sheets describe the behavior.

### Cargo, thrown and carted)

- A **cluster missile** is a generic missile holding up to 200 size points of item stacks (default size 1 each; bomblets 2; parachutes and balloons 2; another missile 10 to 20). It is armed to burst when within **3 blocks** of the target and ignores impacts. The burst throws the list from the end, in rings of 10, each ring 22.5 degrees rotated, slightly behind the last and 0.1 faster (0.3 base). The burst geometry is portable.
- **Bomblets** are projectiles carrying one tier-1 explosive (conventional, shrapnel, incendiary, debilitation, chemical, anvil, repulsive, attractive, colors, smoke). They explode on any impact, cannot be damaged, ignore each other, and hit for `ceil(speed * 3)`. Only the conventional one has a crafting recipe (16 empty droplets per craft from steel, a basic circuit and a button).
- The **load recipe** has no unload; it counts the cluster item's own size against the budget (so 180 usable), and with two clusters in the grid it matches and returns null.
- The **ban/allow list** only gates crafting and the tooltip; the burst spawns whatever is in the list.
- **Bomb cart**: vanilla TNT cart subclass holding one ICBM explosive, 23 recipes (explosive block above a minecart), fuse 100 ticks (120 repulsive and attractive, 300 antimatter, 0 breaching). Breaking a moving or burning cart both drops the item and explodes. Rail and dispenser placement are the only custom hooks.
- The **projectile base** is arrow-like: server-side motion, block ray one block ahead, entity boxes grown 0.3, in-ground state, 1,200-tick lifetimes, drag 0.99 and gravity 0.05. Its **data registry** maps stacks to entities and is how bomblets, arrows and cargo are thrown; snowball and egg conversions never work.
- **Cargo**: parachutes fall at a terminal 0.2 block/tick; balloons rise at 0.2 for 600 ticks, then drift down at 0.05 and pop at 5% per tick. Crafting always produces ITEM-mode holders. All holders are released after 60 seconds, even in mid-air.
- The **grenade** is a fuse bomb thrown at up to 1.8 blocks/tick (hold up to 3 s, but holding the full time loses the throw), 100-tick fuse. The **rocket launcher** takes the first missile in the inventory; its click delay is client-side only and its energy cost is a stub.
- Capabilities, entity and recipe registration, the config annotation system and renderers are *1.12-only mechanisms*. The sheets describe the behavior they produce.

## Related areas

### # 03-missiles (part a: missile core)

| Area | Why |
|---|---|
| `01-core` | `NbtSaveHandler`, buildable registries, packet codex, `MissileEvent` and `MissileChunkEvent`, `MissileRideEvent`, data fixers, `ConfigDebug` flags |
| `02-explosives` | The explosive capability on an entity (`doExplosion`, `isExploding`), IDs and tiers, `PotentialAction`, action causes |
| `04-launch-control` | Builds the flight chain, target data, inaccuracy and source; moves the rider; cruise launcher |
| `05-detection-defense` | Radar map and registry, the EMP action and the kill chain, the radar's SAM trigger |
| `06-blocks-items` | Recipe types, oredict and conditions |
| `07-visuals-audio` | `RenderMissile`, models, textures, sounds, lang |
| part b of `03-missiles` | Cluster missile (the only user of the generic entity), rocket launcher, cargo, grenades, bomb cart, bomblets |

### Cargo, thrown and carted)

| Area | Why |
|---|---|
| `01-core` | `ICBMClassicAPI` registries, `NbtSaveHandler`, `RotationHelper`, the config list parsers (`ItemStackConfigList`) used for the ban and size lists |
| `02-explosives` | The explosive registry and its per-content registries (carts, grenades), explosive item/entity capabilities, flying blocks (used by cargo), the blasts that bomblets, carts and grenades fire |
| part a of `03-missiles` | The generic missile entity that carries a cluster, flight logic, target data, the held-item missile (reuses the cargo ban list) |
| `04-launch-control` | Launchers that spawn the player seat and fire cluster missiles |
| `05-detection-defense` | The defuser (`onDefuse` for grenades and carts), the EMP cart and bomblet, the stubbed electrical item base (ICBM-05-09) |
| `06-blocks-items` | Explosive blocks used as ingredients, the launcher screen, crafting items |
| `07-visuals-audio` | Renderers and the full texture, model, sound and lang catalog |

## Coverage

### # 03-missiles (part a: missile core)

Source files read in full: 58 of 61 in scope.
- `content/missile/` 41 of 41: `entity/` (16: `EntityMissile`, `CapabilityMissile`, `CapabilityEmpMissile`, `explosive/` 2, `anti/` 4 including `SAMTargetData`, `itemstack/` 7), `logic/` (20: `flight/` 12 including `move/` 3 and `prefab/` 3, `targeting/` 2, `source/` 5, `TargetRangeDet`), `tracker/` 4, `MissileEventHandler`.
- `api/missiles/` 13 of 13 (the five `projectile/` files were read for code only, with comments stripped; they belong to part b in practice).
- `config/missile/`: `ConfigMissile`, `ConfigSAMMissile`, `ConfigHeldItemMissile` (3 of 6; `ConfigBomblet`, `ConfigCargoItem`, `ConfigClusterMissile` were skipped on purpose and belong to part b).
- `content/items/ItemMissile` 1 of 1.

Also read, to resolve references (out of scope, so only the lines that matter): `lib/projectile/EntityProjectile` and `prefab/entity/EntityICBM` (in full: they hold the tick, impact and health logic); `lib/capability/missile/CapabilityMissileStack`; `lib/capability/ex/CapabilityExplosiveEntity.doExplosion`; `lib/explosive/reg/ExMissileContentReg`; `content/reg/ExplosiveInit` (IDs and tiers), `EntityReg`, `ItemReg` (remap); `content/blocks/launcher/base/LauncherCapability` (flight chain, inaccuracy, firing), `cruise/CLauncherCapability`; `content/items/ItemRocketLauncher` (flight setup); `lib/actions/PotentialActionImp.doAction`; `content/actions/emp/ActionEmpArea` (receiver call sites); `config/blast/types/ConfigEmp` (missile chances); `lib/radar/RadarMap.getRadarObjects`; `prefab/item/ItemBase.addInformation`; `content/blocks/explosive/ItemBlockExplosive.getDetailedInfo`; `api/events/MissileEvent`, `MissileChunkEvent`; `lib/buildable/BuildableObject`.

Assets at catalog depth: 27 item models in `models/item/missiles/` and the textures in `textures/items/missiles/` (names only, sizes not measured); 29 recipe files under `recipes/missile/` (listed in missile-item.md); lang keys `missile.*.name`, `item.icbmclassic:surface_to_air_missile.*`, `item.icbmclassic:held_item_missile.*`, `projectile.icbmclassic:holder.*`, `tier.icbmclassic.*`, `info.misc.tier`. Not covered: sound durations and sample rates, texture pixel data, `RenderMissile` internals, the NBT-node implementation (`NbtSaveHandler`) behind the exact byte layout of buildable objects.

### Cargo, thrown and carted)

Source files read in full: **49 of 49** in scope.
- `content/cluster/` 11 of 11 (`action/` 2, `bomblet/` 5, `missile/` 4).
- `content/cargo/` 14 of 14 (`balloon/` 2, `parachute/` 3, `placer/` 3, and 6 at the top level).
- `lib/projectile/` 13 of 13 (7 top-level classes, `vanilla/` 5, and `readme.md`).
- `content/items/` `ItemGrenade`, `ItemBombCart`, `ItemRocketLauncher`, and `behavior/` 2: 5 of 5.
- `content/entity/` `EntityGrenade`, `EntityBombCart`, `EntityPlayerSeat`: 3 of 3.
- `config/missile/` `ConfigClusterMissile`, `ConfigBomblet`, `ConfigCargoItem`: 3 of 3 (plus `ConfigMissile` in full for the group structure).

Also read, to resolve references: `prefab/entity/EntityICBM` and `prefab/item/ItemICBMElectrical` (full); `lib/capability/ex/CapabilityExplosiveEntity` (full); `lib/explosive/reg/ExMinecartContentReg` and `ExGrenadeContentReg` (full), `ExplosiveContentRegistry` (first half); `content/reg/ExplosiveInit` (registration and fuse lines only, and the `newEx` helper); `content/actions/conditionals/ConditionTargetDistance` (full); `config/blast/ConfigFuseTimes` (full); `RotationHelper` (`rotateX`, `rotateY` only); registration lines in `ICBMClassic`, `ItemReg`, `EntityReg`, `ICBMEntities`; the recipe JSON files (one bomb cart, one grenade, both bomblet, cluster, parachute, balloon, rpg and rpgBallistic read in full; the other cart and grenade files checked by name), 10 item models, the lang entries for every key named in the sheets, and PNG sizes via the file type.

Assets at catalog depth: textures `items/bomblet.png` (16 x 16), `items/missiles/cluster.png` (32 x 32), `items/grenade/*.png` (10 x 16 x 16), `items/rpg.png` (32 x 32), `items/parachute.png`, `items/balloon.png`, `items/balloon_craft.png`, `blocks/parachute.png` (16 x 16 each), `entity/fragments/fragment.png` (32 x 32); item models for cluster, bomblets (2), grenades (11), bomb carts (23, all on the vanilla TNT-cart texture), launchers (2), parachute and balloon; the balloon item blockstate. No sound belongs to this part.

Not covered: `RenderAsItem`, `RenderSeat`, `RenderEntityItem2` and the vanilla minecart renderer; `ItemBase` (tooltip gating); `ProjectileBlockInteraction`; the config list parsers; the JEI wrappers; the datafixers; PNG pixel palettes and model geometry; the held-item missile recipes (part a).

## Known limits

### # 03-missiles (part a: missile core)

- No code was run. Arc numbers come from algebra plus a scratch stepping of the discrete update (not from the game); timings, probabilities and counts come from reading the source and default config values.
- Forge chunk-ticket limits quoted in the GTNH notes (200 tickets, 25 chunks per ticket) are from memory of stock Forge, not read from source here.
- `NbtSaveNode` behavior for a getter that returns null (the warmup timer, bug 23) and for a buildable object whose registry builder returns null (the SAM target, bug 13) was not read; both are marked unverified.
- Whether `ForgeChunkManager.forceChunk` also loads the chunk, and what `World.spawnEntity` does in an unloaded chunk, are Forge behaviors assumed from 1.12 (bug 38 is marked unverified).
- Whether a pickaxe-held missile really breaks hardness -1 blocks (bug 15) depends on `canHarvestBlock` for those blocks; not traced.
- The renderer decides how the entity looks; seat height effects (bug 08) are inferred from the offset formula.
- `ExMissileContentReg` and the launcher were read only where they touch missiles; the full launcher behavior is 04.
- The tracker only handles `EntityExplosiveMissile`; nothing else was checked for an equivalent path.

### Cargo, thrown and carted)

- No code was run. Every timing, probability and count comes from reading the source. The physics numbers (terminal speeds, balloon climb) come from the stated constants with the standard drag-then-gravity step.
- Behavior inherited from vanilla (the TNT minecart's wall, fall and fire triggers, the item-use flow that cancels a throw after the full charge, `Entity.move` stopping on walls, ticksExisted not being saved) is stated from the vanilla 1.12.2 classes, which are not in this repository and were not re-read. Treat those lines as unverified.
- Forge's handling of the cluster health config (default 100, declared maximum 10) was not checked (ICBM-03-52).
- Whether the cluster ring is tilted for non-vertical approaches (ICBM-03-55) and whether a cluster can miss its 3-block trigger on a target well below the launcher (ICBM-03-56) were reasoned from the code, not seen in play.
- How a grenade behaves when the dispenser spawns it inside its own block (ICBM-03-63) was not checked.
- The cart's `ConfigFuseTimes` keys are the raw field names; whether Forge writes them upper or lower case in the file was not checked.
- The cluster recipe's JEI wrapper and the creative tab's behavior with the BLOCK-mode cargo were not read.
- Sounds and textures were not seen or heard; descriptions come from names and use.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-03-01 | Explosive missile HP ignores tier (logic) | `content/missile/entity/explosive/EntityExplosiveMissile.java:51,55-67`, `prefab/entity/EntityICBM.java:49-52` | The constructor sets HP to `getMaxHealth()` before the explosive is known, so it is tier 1 (50). The stack is set afterwards and HP is never raised, so tiers 2 to 4 only raise the clamp: every explosive missile has 50 HP. Fixing it would expose a second issue: the SAM does 100 damage, below tier 4's 200 | gameplay |
| ICBM-03-02 | SAM and held-item missiles have 1 HP (logic) | `content/missile/entity/anti/EntitySurfaceToAirMissile.java:29`, `.../itemstack/EntityHeldItemMissile.java:66`, `prefab/entity/EntityICBM.java:40` | They call `setMaxHealth(80)` only. HP is the data parameter default 1, so any hit with damage >= 1 destroys them. The generic missile also has 1 HP unless the cluster stack calls `initHealth` | gameplay |
| ICBM-03-03 | Generic missile survives lethal non-explosion damage (logic) | `content/missile/entity/explosive/EntityMissileActionable.java:65-74` | `onDestroyedBy` does nothing unless the source is an explosion or fire. Arrows, melee and the SAM's `missile` damage take HP to 0 and leave the entity alive; each later hit repeats the call. SAMs therefore cannot intercept cluster missiles | gameplay |
| ICBM-03-04 | Health is clamped to the wrong max on load (logic) | `prefab/entity/EntityICBM.java:149-153`, `lib/projectile/EntityProjectile.java:733-734`, `.../EntityExplosiveMissile.java:153-158` | `readEntityFromNBT` loads `health` in the base class before the subclass loads `explosive`, so the clamp uses the tier-1 max. Any HP above 50 would be cut to 50 on reload. Latent while bug 01 holds | gameplay |
| ICBM-03-05 | EMP-triggered warhead leaves the missile flying (logic; resolves ICBM-05-14) | `content/missile/entity/CapabilityEmpMissile.java:47-58`, `lib/capability/ex/CapabilityExplosiveEntity.java:52-66` | The trigger branch calls `doExplosion` (explosive), the main action (generic) or a vanilla explosion (SAM) and never removes the entity. `isExploding` stays set, so the later impact makes no blast. The missile keeps flying as a harmless dud after a real explosion at its position | gameplay |
| ICBM-03-06 | `moveTowards` uses the X delta for Y (logic) | `lib/projectile/EntityProjectile.java:422` | `vecY` is computed from `deltaX`, not `deltaY`, so the post-impact position nudge is wrong vertically. The blast uses the raw hit point, so only the logged position and the source position are off | cosmetic |
| ICBM-03-07 | Missile interaction and listener hooks are empty (logic) | `lib/explosive/reg/ExMissileContentReg.java:30-46,61-64`, `content/reg/ExplosiveInit.java:235` | `setLaunchListener`, `setMissileUpdateListener` and `setInteractionListener` do nothing and `onInteraction` always returns false. The ender missile's "right-click with a GPS to set coordinates" listener is registered into this stub, so it can never be used on a missile (it works on the block) | gameplay |
| ICBM-03-08 | Rider seat height follows vertical speed (logic) | `content/missile/entity/EntityMissile.java:236-248` | Outside the old ballistic logic the offset is `height / 2 + motionY`. A climbing missile lifts the rider several blocks above it; a descent drops the rider inside or below the model | cosmetic |
| ICBM-03-09 | Chicken boarding checks the wrong entity (logic) | `content/missile/entity/EntityMissile.java:145-158` | It tests whether the missile is riding something (`getRidingEntity()`), not whether it already has a passenger. A second chicken (or any chicken while a player rides) plays the sound and fails to board each time; chickens also never cause an impact, so a missile flies through them | cosmetic |
| ICBM-03-10 | SAM has no friend or foe filter (logic) | `content/missile/entity/anti/SAMTargetData.java:123-130` | "Valid" = any live non-SAM missile. A SAM next to a silo can lock on the base's own outgoing missiles, held-item missiles and ridden missiles | gameplay |
| ICBM-03-11 | SAM radar mode sorts the wrong list (logic) | `content/missile/entity/anti/SAMTargetData.java:71-87` | The sort is applied to `entries` but the targets come from the earlier `valid` list, so priority is radar order, not nearest first. The queue also keeps stale entries until it is empty | gameplay |
| ICBM-03-12 | SAM crashes when it has no flight logic (logic) | `content/missile/entity/anti/EntitySurfaceToAirMissile.java:44` | `getFlightLogic().canSafelyExitLogic()` is called without a null check. A SAM with no logic (spawned by command, or after the logic chain was cleared) throws on the first tick that a target is in range | crash |
| ICBM-03-13 | SAM follow state is not saved (logic; save behavior unverified) | `content/missile/entity/anti/EntitySurfaceToAirMissile.java:24,44-53`, `ICBMClassic.java:299` | `hasStartedFollowing` and the target queue are not persisted, and the SAM target type cannot be rebuilt (registered with a null builder). After a reload a following SAM re-runs the switch and gets a fresh 200 ticks of fuel | gameplay |
| ICBM-03-14 | Held-item `*_FIRST` modes always run both actions (logic) | `content/missile/entity/itemstack/EntityHeldItemMissile.java:87-97,223-234` | `PRIMARY_FIRST` runs primary then secondary unconditionally (and the reverse), contrary to the tooltip "or ... if nothing happens". The block-secondary path never sets `hasUsedAction` | gameplay |
| ICBM-03-15 | Held-item block breaking bypasses protections and tool wear (logic; bedrock case unverified) | `content/missile/entity/itemstack/EntityHeldItemMissile.java:138-172` | It calls `removedByPlayer` and `harvestBlock` through a shared fake player with no break event (claims and protections cannot veto) and never damages the tool. `hardness <= digSpeed` is true for hardness -1, so an unbreakable block may be broken if the item can harvest it | gameplay |
| ICBM-03-16 | Held-item recipe loses an already-held item (logic) | `content/missile/entity/itemstack/item/RecipeHeldItemMissile.java:61,80,100-109` | The result is built from the recipe's template stack, not the input missile, so the "already holds an item" guard never fires; a loaded missile plus a new item produces a missile holding only the new item and the old item is destroyed (the decraft recipe is the only safe way to unload) | gameplay |
| ICBM-03-17 | Arc breaks for tiny flat distances (logic) | `content/missile/logic/flight/ArcFlightLogic.java:97-118` | `D = 0` divides by zero (NaN motion); `0 < D < 1` gives a negative `g` and the missile dives; `D = 1` gives `g = 0`. Launchers refuse closer than 10 blocks, but the rocket launcher minimum and addons do not | gameplay |
| ICBM-03-18 | Arc ignores target height (logic) | `content/missile/logic/flight/ArcFlightLogic.java:90-92,115-118` | The target Y is stored but unused. The arc returns to launch altitude at `T + 1` ticks; a higher target is hit on the way down only if the terrain allows, a lower target is reached by free fall after the horizontal stop | gameplay |
| ICBM-03-19 | Arc with no target aims at the world origin (logic) | `content/missile/logic/flight/ArcFlightLogic.java:47-48,67-72` | `end` defaults to (0, 0, 0) and is only overwritten when target data exists. A null-target arc missile flies toward (0, 0) | gameplay |
| ICBM-03-20 | Rider joining mid-ascent leaves the arc without velocity (logic) | `content/missile/logic/flight/ArcFlightLogic.java:128-139` | When a player boards a straight-up missile, the arc is recomputed from the current position but the launch velocity is not applied, so the missile keeps climbing at 2 blocks/tick under the new gravity and never reaches the target. `flightUpAlways` is not saved | gameplay |
| ICBM-03-21 | Chunk-ahead test skips negative directions (logic) | `content/missile/logic/flight/ArcFlightLogic.java:247-262` | The loop runs from the current chunk index up to the future chunk index. If the missile moves toward lower X (or Z), that range is empty and the "about to enter an unloaded chunk" test never triggers, so the missile flies into the gap and freezes instead of being simulated. The old logic's `isAreaLoaded` test has no such flaw | gameplay |
| ICBM-03-22 | Only the explosive missile can be simulated (logic) | `content/missile/logic/flight/ArcFlightLogic.java:208`, `content/missile/tracker/MissileTrackerHandler.java:41`, `MissileTrackerWorld.java:58,207` | Every entry point is typed to `EntityExplosiveMissile`. A generic (cluster), SAM or held-item missile reaching an unloaded chunk stops and is saved with the chunk; nothing re-spawns it as a generic missile | gameplay |
| ICBM-03-23 | Warmup timer is not saved (logic; unverified) | `content/missile/logic/flight/WarmupFlightLogic.java:85` | The save node's getter returns null. A missile saved during its 40-tick pad warmup likely loads with timer 0 and skips the warmup | cosmetic |
| ICBM-03-24 | `velocityAdded` is not saved (logic) | `content/missile/logic/flight/prefab/AccelerateByFacingLogic.java:28,47`, `AccelerateByVec3Logic.java:29,48` | The relative-distance counter restarts at 0 after a reload while the missile keeps its speed, so a relative move step runs longer than intended. The silo uses non-relative steps | cosmetic |
| ICBM-03-25 | Zero-length vectors give NaN motion; follow logic steers to the origin (logic) | `.../DirectFlightLogic.java:43-46`, `.../FollowTargetLogic.java:50-57`, `SAMTargetData.java:161-173` | Normalizing a zero vector divides by zero. Worse, `SAMTargetData` returns (0, 0, 0) when it has no target, so a SAM whose target died steers toward the world origin until its fuel ends | gameplay |
| ICBM-03-26 | `BasicTargetData.isValid` can never be true (logic) | `content/missile/logic/targeting/BasicTargetData.java:61-66` | The check is `isNaN(x) && isFinite(x)`, which is impossible (and the coordinates are checked for NaN where "not NaN" is meant). Nothing calls it today, so it is latent | cosmetic |
| ICBM-03-27 | `BasicTargetData` save throws on null position; equals without hashCode (logic) | `.../BasicTargetData.java:97-103,116-121` | `serializeNBT` dereferences the position; `equals` ignores `firingDelay` and `hashCode` is not overridden | cosmetic |
| ICBM-03-28 | Air-burst fuse fires on the pad; cluster branch is dead (logic) | `content/missile/logic/TargetRangeDet.java:17-46,27`, `api/refs/ICBMExplosives.java:54` | The fuse runs from the first tick, even during warmup: a target within 2 blocks of the missile detonates it in the silo. The 10-block cluster branch compares with `ICBMExplosives.CLUSTER`, a static that is never assigned | gameplay |
| ICBM-03-29 | `simulateMissile` always returns false (logic) | `content/missile/tracker/MissileTrackerHandler.java:41-56` | Success, refusal and cancel all return false. Callers store `!result` in `wasSimulationBlocked`, so every attempt marks the logic "blocked". Harmless after a success (the entity is dead) but it means a refused attempt is never retried | cosmetic |
| ICBM-03-30 | Chunk-unload simulation only covers the deprecated logic (logic) | `content/missile/MissileEventHandler.java:64-66` | Only `EntityExplosiveMissile` with `BallisticFlightLogicOld` is simulated when its chunk unloads. Missiles on `ArcFlightLogic` freeze with the chunk and resume when it loads again, possibly much later | gameplay |
| ICBM-03-31 | Simulation speed is not "meters per tick" (logic) | `content/missile/tracker/MissileTrackerWorld.java:82,98`, `config/missile/ConfigMissile.java:21-24` | The count is `round(dist / simulation_flight_speed)` and each count lasts one 21-tick step (`ticks++ >= 20`), so the default 10 means about 9.5 blocks/s, not 10 blocks/tick. Raising it by a tick-based mental model gives wrong travel times | gameplay |
| ICBM-03-32 | Forced chunks are compared by reference (logic) | `content/missile/tracker/MissileTrackerWorld.java:239` | `currentLoadedChunks.get(i).chunkPos == chunkPos` compares objects; a fresh `ChunkPos` is never the same object, so a chunk wanted again gets a second entry instead of a timer refresh. The first expiry unforces a chunk that a later missile still needs | gameplay |
| ICBM-03-33 | Chunk unforce loop skips entries; ticket limits unhandled (logic; restart case unverified) | `.../MissileTrackerWorld.java:161-176,51` | Removing an entry by index while counting up skips the next one for a step. All landings share one ticket whose depth Forge limits, and the restart callback is an empty lambda, so restored tickets are never released | gameplay |
| ICBM-03-34 | Re-entry replays the launch (logic) | `.../MissileTrackerWorld.java:225` | `launch()` replays `MISSILE_LAUNCH` at the target, posts `PostLaunch` a second time and re-registers the radar entry. Addon listeners see two launches | cosmetic |
| ICBM-03-35 | Chunk coordinate truncates negative targets (logic) | `.../MissileTrackerWorld.java:126-139` | `(int) x >> 4` casts before shifting, so a negative fractional coordinate rounds toward zero and can pick the neighboring chunk. The four neighbor chunks hide most of it | cosmetic |
| ICBM-03-36 | `CapabilityMissile` equals and hashCode disagree (logic) | `content/missile/entity/CapabilityMissile.java:215-228` | `equals` compares logic, target and flag; `hashCode` uses the entity. Equal capabilities of different entities hash differently | cosmetic |
| ICBM-03-37 | Re-entry sets position fields directly (1.12-only) | `.../MissileTrackerWorld.java:211-213` | `posX/posY/posZ` are written without `setPosition`, so the bounding box is stale for the first tick. Impact detection uses ray traces, so the effect is small; unverified | cosmetic |
| ICBM-03-38 | Pre-load timer is not saved and the spawn result is ignored (logic; unverified) | `content/missile/tracker/MissileTrackerData.java:33-59`, `.../MissileTrackerWorld.java:189-196,228` | After a restart a waiting entry loads with timer 0 and spawns on the next step, before its chunk has been re-forced (the forced list is not saved). `spawnEntity`'s false result for an unloaded chunk is not checked, so the missile is lost without a message. The same happens when no ticket was available | gameplay |
| ICBM-03-40 | Cached data builder never builds | `lib/projectile/CachedProjectileData.java:18` | Inverted null check; snowball and egg conversions return null and become inert items | gameplay |
| ICBM-03-41 | Impact nudge uses X for Y | `lib/projectile/EntityProjectile.java:422` | `vecY` is derived from `deltaX` | cosmetic |
| ICBM-03-42 | Lifetime counters not saved; saved keys are limits | `lib/projectile/EntityProjectile.java:738-747` | `ticks.air` and `ticks.ground` hold the kill limits; a reload restarts the clock | cosmetic |
| ICBM-03-43 | Shooter lost on reload; never set by throws | `lib/projectile/EntityProjectile.java:64-75,749-754` | Shooter, UUID and source position are not saved (commented out); `throwProjectile` never sets the shooter | cosmetic |
| ICBM-03-44 | Spectral arrow data spawns a plain arrow | `lib/projectile/vanilla/SpectralArrowProjectileData.java:22` | Builds a tipped arrow with no effect | gameplay |
| ICBM-03-45 | Impact event may get a null hit (unverified) | `lib/projectile/EntityProjectile.java:284-289` | The entity intercept can be null and is passed to Forge listeners | crash |
| ICBM-03-46 | Empty holder data short-circuits `find` | `lib/projectile/ProjectileDataRegistry.java:56-61` | Returns null rather than the item fallback | cosmetic |
| ICBM-03-47 | Load recipe counts the cluster's own size | `content/cluster/missile/RecipeCluster.java:55-62` | Capacity is 180, not 200 | gameplay |
| ICBM-03-48 | Nested cluster passes `matches`, returns null | `content/cluster/missile/RecipeCluster.java:55-62,91` | A second cluster matches then yields a null result | crash |
| ICBM-03-49 | Ban/allow list not enforced at run time | `content/cluster/action/ActionCluster.java:154`, `RecipeCluster.java:58`, `ItemClusterMissile.java:100-103` | Only crafting and the tooltip use it | gameplay |
| ICBM-03-50 | Random jitter uses the layer constant | `content/cluster/action/ActionCluster.java:140-143` | `motionRandomScale` only switches the jitter on | cosmetic |
| ICBM-03-51 | Tilt vector not rotated with the missile | `content/cluster/action/ActionCluster.java:128-137` | World-space bias of about 0.11 block/tick toward -Z at any non-vertical heading | visual |
| ICBM-03-52 | Cluster health default outside its range | `config/missile/ConfigClusterMissile.java:12-13` | Default 100, declared max 10 | gameplay |
| ICBM-03-53 | Bomblets blame the bomblet, not the player | `content/cluster/bomblet/EntityBombDroplet.java:57,85` | Cause is the bomblet entity | gameplay |
| ICBM-03-54 | Bomblet entity name keys missing | `content/cluster/bomblet/EntityBombDroplet.java:111-113` | `bomb.droplet.*` not in the lang file | cosmetic |
| ICBM-03-55 | Pitch sign convention mismatch (unverified) | `content/cluster/action/ActionCluster.java:125,147`, `ActionDataCluster.java:43-44`, `lib/projectile/EntityProjectile.java:588,678` | Ring assumes Minecraft pitch; the projectile base uses the opposite sign | visual |
| ICBM-03-56 | Cluster that misses its 3-block trigger is lost (unverified) | `content/cluster/missile/CapabilityClusterMissileStack.java:45-48` | Impact disabled, so a miss leaves the missile in the ground until it is silently removed | gameplay |
| ICBM-03-57 | Bomblet health config and hit-detonate code dead | `content/cluster/bomblet/EntityBombDroplet.java:41,74-86`, `config/missile/ConfigBomblet.java:10-15` | `hasHealth` false, so `bomblet.health` and the explode-on-damage code never run | gameplay |
| ICBM-03-58 | Breaking a moving cart drops the item and explodes | `content/entity/EntityBombCart.java:68-84` | Item dropped unless the damage is an explosion, even when the cart also explodes | gameplay |
| ICBM-03-59 | Dead and missing cart fuse entries | `config/blast/ConfigFuseTimes.java:82`, `content/entity/EntityBombCart.java:93`, `content/reg/ExplosiveInit.java:301-308` | Hypersonic cart key does nothing; colors and smoke have none; cart tick callback unused | cosmetic |
| ICBM-03-60 | Grenade forgets its fuse and thrower on reload | `content/entity/EntityGrenade.java:282,324-336` | `ticksExisted` and the thrower are not saved | gameplay |
| ICBM-03-61 | Over-charging loses the throw; a tap throws at zero speed | `content/items/ItemGrenade.java:68-90` | Only release throws; energy not clamped | gameplay |
| ICBM-03-62 | Missing fuse config for colors and smoke grenades | `config/blast/ConfigFuseTimes.java` (`Grenades` class) | Eight keys for ten grenades | cosmetic |
| ICBM-03-63 | Dispenser grenade has no forward offset | `content/items/behavior/GrenadeDispenseBehavior.java:35-37` | Spawns at the dispenser center; vertical dispensers lob upward | gameplay |
| ICBM-03-64 | Rocket launcher click delay is client-only | `content/items/ItemRocketLauncher.java:221,235-240` | Timestamp stored after the server `return` | gameplay |
| ICBM-03-65 | Rocket launcher energy is a stub | `content/items/ItemRocketLauncher.java:97,203`, `prefab/item/ItemICBMElectrical.java:12-18` | Same root cause as ICBM-05-09 | gameplay |
| ICBM-03-66 | Launcher fires the first missile in the whole inventory | `content/items/ItemRocketLauncher.java:100-103,213-218` | No ammo choice; gives up when the first one cannot be aimed | gameplay |
| ICBM-03-67 | Wrong or unused launcher strings | lang `rocketLauncher.error.IMissileAiming`, `.info.creative`, `ballisticLauncher.info` | `%1` never substitutes; creative line unused; "20m y+ offset" is cluster-only | cosmetic |
| ICBM-03-68 | Throw item consumption has an operator-precedence error | `content/cargo/ItemThrowableProjectile.java:88` | Survival stacks are used up on failure and on both sides; a failed non-player throw casts wrongly | gameplay |
| ICBM-03-69 | Cargo recipe depends on slot order and allows nesting | `content/cargo/RecipeCargoData.java:46-67` | Overwritten `hasCargoItem`; missile refusal is order-dependent | gameplay |
| ICBM-03-70 | Only ITEM mode craftable; PROJECTILE tooltip key missing | `content/cargo/CargoProjectileData.java:58,74`, `RecipeCargoData.java` | Other modes only from creative, NBT or API | cosmetic |
| ICBM-03-71 | Spawn-egg mode names the holder, not the mob | `content/cargo/CargoProjectileData.java:125-129` | `setCustomNameTag` called on the holder | cosmetic |
| ICBM-03-72 | 60-second timer releases cargo mid-air | `lib/projectile/EntityProjectile.java:81-85,353`, `content/cargo/parachute/EntityParachute.java:83` | Falls over about 240 blocks release early | gameplay |
| ICBM-03-73 | Cargo hook block is unregistered dead code | `content/cargo/placer/BlockCargoHook.java:40-45` | No registration, behavior or valid meta mapping | cosmetic |
| ICBM-03-74 | Seat dismount check compares Y with Z | `content/entity/EntityPlayerSeat.java:91` | `prevY - posZ` instead of `posY` | gameplay |
| ICBM-03-75 | Decraft returns the held stack itself and matches empty holders | `content/cargo/RecipeCargoDataDecraft.java:30-48,99` | Result is the input's object, not a copy | cosmetic |

**# 03-missiles (part a: missile core):**

"Logic" means we could inherit it in a rebuild. "1.12-only" means it does not matter to a 1.7.10 rebuild.
Minor, not given IDs: `EntityMissile.logImpact` logs every impact at INFO (TODOs to make it configurable); the engine sound is played every tick on whichever side runs `updateMotion` (possible duplicate on the client; not traced); `BallisticFlightLogicOld` hard-codes the 200-block arc threshold; `ItemMissile` has no `.info` lang keys so the generic tooltip line is empty; the entity-name lang keys for the SAM and held-item missiles are missing; `EntityMissile.shouldCollideWith` tests `shootingEntity`, which missiles never set; the red-matter taunt branch in the block-explosive tooltip is unreachable for missiles.

**Cargo, thrown and carted):**

"Logic" means we could inherit it in a rebuild. "1.12-only" means it does not matter to a 1.7.10 rebuild. Every bug in this part is a logic bug unless marked.
Minor, not given IDs: the cluster size defaults for tiers 2 to 4 are dead (no such bomblet exists); the lang file carries names for 13 bomblet types and a hypersonic and a nightmare cart that cannot exist; `ConfigCargoItem`'s comment mentions the cluster; the cart ignores its speed argument; the render class for bomblets binds a texture it never uses; `ProjectileItemConversion.sortStack` is unused.
