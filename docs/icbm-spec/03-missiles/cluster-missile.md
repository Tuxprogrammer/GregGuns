# Cluster Missile and Bomblets

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. A **cluster missile** is a missile item that holds a list of item stacks. Near its target it bursts open and throws every stack out as a projectile; when the stacks are **bomblets**, each one is a small bomb that explodes on impact. This sheet covers the cluster missile item, its load recipe and size budget, the burst action, the bomblet items and the bomblet entity. The generic missile entity that carries the payload is in [missile-entity.md](missile-entity.md); how it flies is [flight-logic.md](flight-logic.md). The projectile base the bomblet sits on is [projectile-base.md](projectile-base.md). Parachutes and balloons are the other common payload ([cargo.md](cargo.md)).

Capabilities, DataParameters, the recipe registry and the config annotation system are *1.12-only mechanisms*; the behavior they produce is described.

## 1. Identity

| Item | Value |
|---|---|
| Classes | `content/cluster/missile/`: `ItemClusterMissile`, `CapabilityClusterMissileStack`, `ClusterMissileHandler`, `RecipeCluster`. `content/cluster/action/`: `ActionDataCluster`, `ActionCluster`. `content/cluster/bomblet/`: `ItemBombDroplet`, `BombletProjectileData`, `BombletProjectileStack`, `EntityBombDroplet`, `RenderBombDroplet`. Configs: `config/missile/ConfigClusterMissile`, `ConfigBomblet`, `ConfigMissile` |
| Items | `icbmclassic:cluster_missile` (stack size 1, no subtypes in use); `icbmclassic:empty_bomblet` (a plain base item); `icbmclassic:explosive_bomblet` (stack size 16, metadata = explosive ID) |
| Missile id string | `icbmclassic:missile[cluster]` (the stack's missile capability) |
| Entities | the cluster missile spawns an `EntityMissileActionable` (`icbmclassic:generic_missile`); each bomblet is an `EntityBombDroplet` (`icbmclassic:bomb_droplet`, tracking range 500, update every 1 tick) |
| Registry keys | action data `icbmclassic:entity.cluster.spawning` (built per missile, never registered statically: `register()` throws); projectile data `icbmclassic:bomblet` |
| English names | `item.icbmclassic:cluster_missile.name` = "Cluster Missile"; `missile.icbmclassic:cluster.name` = "Cluster Missile"; `item.icbmclassic:empty_bomblet.name` = "Droplet"; `item.icbmclassic:explosive_bomblet.name` = "Bomblet"; per-type `bomblet.icbmclassic:<explosive>.name` (see below) |
| Tooltips | `item.icbmclassic:cluster_missile.info` = "Deploys a spread of projectiles in proximity to target. Lacks impact sensor."; `item.icbmclassic:empty_bomblet.info` = "Empty unguided projectile" |
| Creative tab | the ICBM tab. Cluster missile lists four stacks: empty; 200 arrows; 100 conventional bomblets; 50 parachutes each holding a cookie. Bomblets list one stack per grenade-capable explosive |
| Oredict | none |
| Death message | `death.attack.icbmclassic:bomblet` = "%s got smacked in the head with a bomb" (bomblet direct hit) |

Bomblet type names (`bomblet.icbmclassic:<key>.name`): condensed = "Conventional Bomblet", shrapnel = "Shrapnel Bomblet", incendiary = "Incendiary Bomblet", debilitation = "Debilitation Bomblet", chemical = "Chemical Bomblet", anvil = "Anvil Bomblet", repulsive = "Repulsive Bomblet", attractive = "Attractive Bomblet", colors = "Colorful Bomblet", smoke = "Smoke Marking Bomblet". The lang file also has keys for fragmentation, contagious, sonic, breaching, thermobaric, nuclear, emp, exothermic, endothermic, antigravitational, hypersonic, antimatter and redmatter bomblets; no such item exists in the game (see section 3).

## 2. Obtaining

| Thing | How |
|---|---|
| Empty cluster missile | Shaped, 1 -> 1: `circuitBasic` over a vanilla dispenser over a missile item with metadata 24 (the "missile module", `ICBMExplosives.MISSILEMODULE`). Conditioned on the `circuitBasic` oredict having entries (`recipes/missile/special/missile.cluster.json`) |
| Loading it | Dynamic recipe `RecipeCluster` (`icbmclassic:cluster_missile`): one cluster missile plus any number of other items in the grid (1 to 8 other stacks, one of each slot's items consumed, no container items returned). The result is the same cluster with the other items appended to its list, one entry per consumed item. See the size rules below |
| Unloading | none. There is no recipe to take items back out (the held-item missile and the cargo holders have one; the cluster does not) |
| Droplets | Shaped, 5 `ingotSteel`, 1 `circuitBasic`, 1 wooden button -> **16** `empty_bomblet` (`recipes/bomblet/bomblet.json`; pattern `_i_` / `ici` / `ipi`), conditioned on both oredicts having entries |
| Conventional bomblet | `empty_bomblet` + a conventional explosive block, shapeless in a row -> 1 `explosive_bomblet` meta 0 (`recipes/bomblet/condensed.json`, type `icbmclassic:explosive`) |
| Other bomblet types | **no recipe files exist** (only `condensed.json` is in `recipes/bomblet/`). The other nine tier-1 types come from the creative tab, JEI or commands |
| Grenade-capable explosives | the bomblet item lists exactly the explosives enabled for the grenade registry: tier ONE only (conventional, shrapnel, incendiary, debilitation, chemical, anvil, repulsive, attractive, plus colors and smoke). Set in `content/reg/ExplosiveInit.java:301-308` |

## 3. Numbers

### Config (`icbmclassic/missile`)

| Key | Default | Meaning |
|---|---|---|
| `cluster.health` | 100 declared; **range 0.0001 to 10** (ICBM-03-52) | Health given to a newly built cluster missile (`initHealth`) |
| `cluster.item_sizes.max_size` | 200 (min 1) | The cluster's volume budget |
| `cluster.item_sizes.default_size` | 1 (min 1) | Size of any item with no explicit size |
| `cluster.item_sizes.list` | empty | `domain:item=whole_number` entries that override the defaults below |
| `cluster.item_ban_allow.ban` | true | true = the list below is a ban list; false = it is an allow list |
| `cluster.item_ban_allow.list` | empty | `domain:item` entries; an entry ending `=true` is ignored while loading. With the defaults (ban mode, empty list) **everything is allowed** |
| `bomblet.impact_damage` | 3.0 (min 1) | Direct-hit damage = `ceil(speed * impact_damage)` |
| `bomblet.health` | 5 (min 1) | Declared as the bomblet's hit points; has no effect (ICBM-03-57) |

### Built-in item sizes (loaded into the size table before the user list)

| Item | Size |
|---|---|
| Balloon, parachute | 2 each |
| Cluster missile, explosive missile | 20 each |
| SAM missile, held-item missile | 10 each |
| Explosive bomblet (any metadata not set below) | 2 |
| Explosive bomblet by explosive tier: ONE / TWO / THREE / FOUR | 2 / 5 / 10 / 20 (tiers 2 to 4 cannot occur for real bomblets, section 2) |
| Anything else | `default_size` (1) |

### Burst action constants (`ActionCluster`; the setters exist but nothing calls them, so these are fixed)

| Name | Value |
|---|---|
| Projectiles per disc | 10 (36 degrees apart) |
| Yaw offset per later disc | +22.5 degrees |
| Ring offset from the missile center | 0.25 block |
| Stack offset per disc, along the missile axis (backward) | 0.1 block |
| Base ejection factor | 0.3 |
| Extra factor per disc after the first | +0.1 per disc |
| Tilt vector ("back vector") | up rotated -22.5 degrees about X = (0, 0.924, -0.383), added to the ring direction before scaling |
| Random jitter | each axis `(rand - rand) * 0.1` (triangular, +/- 0.1) |
| Declared but unused | `ejectionScale` 0.3, `allowPickupItem` default true (the action data never changes it, so spawned items stay pickup-able) |

### Cluster missile trigger

Generic missile with main-action condition AND of: "impact not desired" and "within 3 blocks of the target" (`ConditionTargetDistance`, 3D distance from the missile to the target position, trigger distance 3).

### Bomblet entity

| Name | Value |
|---|---|
| Size | 0.25 x 0.25 |
| Health | none (`hasHealth` false) |
| Gravity / air resistance | -0.05 per tick / x0.99 (the base defaults) |
| Lifetime | 1,200 ticks in air; 1,200 in ground (base) |
| Impact damage | `ceil(speed * 3)` with the bomblet damage source (`icbmclassic:bomblet`) |
| Collision | other bomblets are ignored (always, a TODO says "first few ticks only") |

## 4. Behavior

### Loading (`RecipeCluster`)

`matches` scans the grid:
- Empty slots are skipped. The first stack equal to the cluster item (same item and metadata) is taken as the cluster.
- Every other stack must pass the ban/allow check; one rejected stack fails the whole recipe.
- The "new size" sums the size of **every** non-empty stack, including the cluster itself (ICBM-03-47).
- It matches when the cluster exists, the new size is at least 1, and `current contents size + new size <= max_size`.

`getCraftingResult` copies the cluster (count 1) and appends one copy (count 1) of each other stack. A second cluster in the grid returns **null** instead of a stack ("can't nest", ICBM-03-48). The grid needs at least 2 cells.

The contents are a flat list: 100 conventional bomblets are 100 list entries, not stacks. Size is computed per entry from the table above.

### Flight and trigger

The cluster stack's `newMissile` builds a generic missile and gives it the cluster action data. It disables the impact trigger and arms a 3-block proximity trigger on the target; flight is the normal chain from [flight-logic.md](flight-logic.md) (launcher: warmup, climb, arc). The generic missile re-evaluates its main action **every tick** (see [missile-entity.md](missile-entity.md) "Generic missile extras"):
- Within 3 blocks of the target position: the burst runs, then the missile destroys itself (status not blocking).
- The missile hits terrain or an entity first: the condition says "impact not wanted", so the action stays blocked and the missile just sits in the ground until the 1,200-tick in-ground timer destroys it silently. The payload is lost (ICBM-03-56).
- Lethal damage: `EntityMissileActionable.onDestroyedBy` runs the burst only for explosion or fire damage (ICBM-03-03 in the part a bug list).
- Only missiles launched with a target position can ever trigger. Fired with no target, there is nothing to measure distance against.
- The cluster's health is `cluster.health` (see ICBM-03-52).

### The burst (`ActionCluster`, server only)

MIT-portable: [ActionCluster.java#L110-L162](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/cluster/action/ActionCluster.java#L110-L162) (the geometry), `RotationHelper` in `01-core` (rotation conventions).

Inputs: the list of stacks, the missile's yaw and pitch (copied from the host entity when the action is built), the missile position.

```
n = number of stacks; remaining = n; disc = 0
while remaining > 0:
    for i in 0 .. 9, while remaining > 0:
        remaining -= 1
        stack = list[remaining]                        // the list is consumed from the END
        yaw   = wrap(36 * i + 22.5 * disc)
        ring  = rotateY( rotateX( rotateY(SOUTH, yaw), pitch - 90 ), missileYaw )   // unit vector
        back  = rotateX(UP, -22.5)                      // world space, not rotated with the missile
        motion = (ring + back) * 0.3 + (ring + back) * 0.1 * disc
        motion += (rand - rand) * 0.1 on each axis
        axis  = rotateY( rotateX(SOUTH, pitch - 180), missileYaw )                   // points backward along the missile
        offset = ring * 0.25 + axis * 0.1 * disc
        spawn the stack's projectile at missilePos + offset with that motion, heading set to the motion
    disc += 1
```

So the payload leaves in rings of up to 10, each later ring slightly behind, rotated 22.5 degrees from the one before and thrown faster (+0.1 factor per ring). For a missile coming straight down the rings are horizontal and everything is also pushed up by the world-space tilt, so the burst looks like an upward-biased fountain. With the default 200-entry payload there are 20 rings and the last one is thrown at 2.2 times the base factor.

Spawning each entry goes through the projectile registry ([projectile-base.md](projectile-base.md)): the stack becomes an arrow, a bomblet, a parachute, a snowball (not working), or a dropped item. If no entity can be made, the stack is spawned as a plain dropped item at that position with the same motion, so nothing is lost. The result is COMPLETED if at least one thing spawned, else `ENTITY_SPAWN_FAILED`. The ban/allow list is **not** consulted here (ICBM-03-49). A client calling the action returns immediately.

### Bomblet entity

- Flight, collision and expiry come from the projectile base; no flight logic, just gravity and drag.
- **Impact** (block or entity): an entity hit first applies `ceil(speed * bomblet.impact_damage)` damage with the bomblet source and knockback; then in both cases the entity is removed and the explosive capability runs the stack's explosive at the exact hit point, with the bomblet as the cause (not the player who fired the missile). A `isExploding` guard stops a second blast.
- **Damage:** the bomblet has no hit points, so it can never be shot or blown up. The code that would set off the bomblet on explosion or fire damage is unreachable (ICBM-03-57).
- It ignores other bomblets, so a dense burst does not collide with itself. It does collide with parachutes, items with collision and everything else with a hitbox.
- The explosive's blast is whatever the stack's explosive does when run with its normal blast factory; sizes and effects belong to [02-explosives](../02-explosives/blast-types.md). Thermobaric, nuclear and so on cannot be bomblets.
- The projectile's direction vector for rendering is the normalized motion (a divide by zero at rest, harmless since the entity dies on impact).

## 5. Persistence and sync

| Where | Key | Type | Meaning |
|---|---|---|---|
| Cluster item (stack capability `missile`) | `cluster_action` | compound | the action data |
| ... | `cluster_action.clusterSpawnEntries` | list of compound | one full item-stack NBT per payload entry |
| Bomblet item (capabilities `explosive`, `projectile`) | explosive capability data | compound | the explosive ID from the metadata |
| Bomblet entity | `explosive` | compound | the explosive stack's NBT (item id, count, metadata, tag) |
| Bomblet entity | + base keys | | see [projectile-base.md](projectile-base.md) |
| Projectile data `icbmclassic:bomblet` | `explosive` | compound | the explosive stack's NBT |

The bomblet entity sends its save NBT in the spawn packet, so clients know which explosive it is. The cluster missile entity's own sync (data parameters, target data) is in part a.

Item stack NBT size grows with the payload: 200 entries make a stack with 200 tag compounds, which every server-to-client item sync carries. A TODO in the source suggests condensing to stacks of 64.

## 6. UI

Tooltip of a loaded cluster missile (client):
1. If the payload is not empty: `projectile.icbmclassic:holder.held.multiple` = "Held Items" (colored dark gray, underlined).
2. Then one line per distinct display name: `  <count> x <name>` (the map is a plain hash map, so line order is unspecified). A name whose stack fails the ban/allow check is prefixed with `projectile.icbmclassic:holder.disabled.prefix` = red "(D): %1$s".
3. If any entry is disabled, the last line is that prefix applied to `projectile.icbmclassic:holder.disabled.config` = red "Held item disabled by config, may not work correctly!!".

An empty cluster shows only the item description. Bomblet tooltips reuse the explosive block's detail lines (the explosive's own description and its customization info). There is no GUI. JEI integration (`mods/jei/ClusterItemWrapper`) is out of scope here.

## 7. Visuals

| Thing | Asset |
|---|---|
| Cluster item model | `models/item/missiles/cluster.json`; texture `textures/items/missiles/cluster.png`, 32 x 32 |
| Empty and explosive bomblet item models | `models/item/empty_bomblet.json`, `explosive_bomblet.json`: small box models by "QueenOfMissiles" sharing `textures/items/bomblet.png` (16 x 16). The explosive bomblet uses a mesh mapper keyed by explosive, with condensed as the fallback (`ClientReg`) |
| Bomblet entity | `RenderBombDroplet` draws the bomblet **item stack** through a fake item entity: translate up 0.15, yaw `-180` and pitch `+90` added to the entity's smoothed rotation, then up another 0.2. It also binds `textures/entity/fragments/fragment.png` (32 x 32), which it never draws with (leftover) |
| Burst effect | none (no particle or sound of its own) |

Entity renderers and the recipe-book/JEI models are *1.12-only mechanisms*. Full texture catalog: `07-visuals-audio`.

## 8. Audio

None of its own. The bomblet's blast plays its explosive's sound; the missile's engine sound belongs to the flight logic in part a.

## 9. Relationships

| Direction | What |
|---|---|
| Needs | the generic missile entity and `ConditionAnd`, `ConditionalImpact`, `ConditionTargetDistance` ([missile-entity.md](missile-entity.md)); the projectile data registry ([projectile-base.md](projectile-base.md)); the explosive registry and `EX_GRENADE_REGISTRY` ([02-explosives](../02-explosives/explosive-registry.md)) |
| Config sync | `ConfigMain` reloads the size and ban tables on a config change (`ClusterMissileHandler.loadFromConfig`) |
| Used by | launchers ([04-launch-control](../04-launch-control/launch-sequence.md)); the rocket launcher fires a cluster missile and lifts its target by 20 blocks ([thrown-and-handheld.md](thrown-and-handheld.md)); cargo recipes reject clusters ([cargo.md](cargo.md)); the held-item missile bans clusters as payload |
| GregGuns note | The owner keeps cluster missiles and bomblets. Cargo (parachutes, balloons) is deferred, so the payload list is mostly bomblets, arrows and items for now |

## 10. Bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-03-47 | Load recipe counts the cluster's own size (logic) | `content/cluster/missile/RecipeCluster.java:55-62` | The first branch picks the cluster but does not skip to the next slot, so the size sum also includes the cluster item (default 20). Real capacity per craft is 180 instead of 200, and a creative cluster with a full 200 can never take another item | gameplay |
| ICBM-03-48 | Nested cluster passes `matches`, returns null (logic) | `content/cluster/missile/RecipeCluster.java:55-62,91` | A second cluster is treated as an ordinary payload item (allowed by default, size 20), so the recipe matches, then `getCraftingResult` returns null. A null result is not a valid stack in 1.12. A grid with only the cluster also matches and returns an identical copy | crash |
| ICBM-03-49 | Ban/allow list is not enforced at run time (logic) | `content/cluster/action/ActionCluster.java:154`, `RecipeCluster.java:58`, `ItemClusterMissile.java:100-103` | Only the load recipe and the tooltip use it. Stacks put in by command, creative, another mod or an older config go off regardless. The tooltip itself says "may not work correctly" | gameplay |
| ICBM-03-50 | Random jitter uses the layer constant (logic) | `content/cluster/action/ActionCluster.java:140-143` | The jitter is scaled by `motionScaleLayer` (0.1), not `motionRandomScale` (0.1). Same value today; the random scale only switches it on or off | cosmetic |
| ICBM-03-51 | Tilt vector is not rotated with the missile (logic) | `content/cluster/action/ActionCluster.java:128-137` | The "shoot at an angle" vector is built in world space and added to the ring direction, which is rotated to the missile's heading. Only a missile pointing down gets the intended cone; any other angle gets a sideways bias of about 0.11 block/tick toward -Z | visual |
| ICBM-03-52 | Cluster health default is outside its own range (logic) | `config/missile/ConfigClusterMissile.java:12-13` | Default 100, declared maximum 10. Depending on how Forge clamps, the real default is 100 or 10. Either way it differs from the tier-1 missile (50). Unverified which | gameplay |
| ICBM-03-53 | Bomblets blame the bomblet, not the player (logic) | `content/cluster/bomblet/EntityBombDroplet.java:57,85` | Both blast calls use the bomblet as the cause (a TODO says to include the impact info), so protected-area and kill-credit logic never see the launcher's owner | gameplay |
| ICBM-03-54 | Bomblet entity name keys are missing (logic) | `content/cluster/bomblet/EntityBombDroplet.java:111-113` | `bomb.droplet.<explosive>.name` and the generic fallback do not exist in `en_us.lang`, so death messages and commands show the raw key | cosmetic |
| ICBM-03-55 | Pitch sign convention mismatch (unverified, logic) | `content/cluster/action/ActionCluster.java:125,147`, `ActionDataCluster.java:43-44`, `lib/projectile/EntityProjectile.java:588,678` | The ring math is written for Minecraft's pitch (positive = looking down). The host's `rotationPitch` comes from the projectile base, where positive = pointing up. They agree at vertical (the common case) and disagree in between; the ring plane would be tilted off the heading. Not verified in play | visual |
| ICBM-03-56 | Cluster that misses its 3-block trigger is lost (unverified, logic) | `content/cluster/missile/CapabilityClusterMissileStack.java:45-48` | Impact is disabled and the trigger is a 3 block sphere around the target point. Part a records that the arc ends at launch altitude and ignores target height; a target far below the launcher may be passed outside the sphere (the missile has about 0.13 block of sideways travel per block of fall), and then it sits in the ground and is silently removed, payload and all | gameplay |
| ICBM-03-57 | Bomblet health config and hit-detonate code are dead (logic) | `content/cluster/bomblet/EntityBombDroplet.java:41,74-86`, `config/missile/ConfigBomblet.java:10-12` | `hasHealth` is false, so `attackEntityFrom` always refuses. `bomblet.health` and `onDestroyedBy` (explode on explosion or fire) never run, so chain reactions between bomblets do not happen. The config comment calls the value "hearts" | gameplay |

Minor, not given IDs: the lang file has bomblet names for 13 types that cannot exist; the size defaults for tiers 2 to 4 are therefore dead; `ConfigCargoItem`'s comment for `item_ban_allow` is copied from the cluster ("insertion into cluster missile") although it governs cargo holders; `ItemBombDroplet.getUnlocalizedName` falls back to the stackless base name when the stack has no explosive.
