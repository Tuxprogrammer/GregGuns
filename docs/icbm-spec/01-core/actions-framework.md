# Actions Framework

Source roots (relative to `src/main/java/icbm/classic/` at `9c1d985a`): `lib/actions/`, `api/actions/`, `content/actions/` (EMP part excluded, see [emp-tower.md](../05-detection-defense/emp-tower.md)), plus the tag base in `api/data/meta/` and the registry base in `api/reg/obj/` and `lib/buildable/`. This sheet specifies what an *action* is, how the framework fires one, and how blasts, EMP pulses and entity spawns are expressed in it. The old Blast API (`IBlast` and friends) is being replaced by this framework; section 11 maps old to new. Mechanisms that exist only because of 1.12 (capabilities, Forge registry events) are marked *1.12-only mechanism*.

## 1. The idea in one paragraph

An **action** is a short-lived, instant object: "do this thing, at this place, because of this cause", created from registered **action data** and run once. It returns an **action status** (a tagged result with a message). Anything that lasts several ticks (a threaded blast, a fuse, a flying missile) is *not* an action; the action starts it or spawns an entity that carries it. Before an action runs it may pass through **listeners** that can veto it. Whether an action may run at all is decided by a **potential action**: action data plus an optional **condition** (timer, impact, distance, and so on). Who or what started it is recorded as a **cause chain**.

## 2. Vocabulary

| Term | Interface (in `api/actions/`) | Role |
|---|---|---|
| Action | `IAction` | One runnable instance. Has `doAction()`, a source, its data, a world and a position. Also a field provider and receiver (it can be configured by field) |
| Action data | `IActionData` | Registry entry that builds actions: `create(world, x, y, z, source, fieldProvider)`. Carries type tags and the list of supported fields |
| Potential action | `IPotentialAction` | Cached on a tile, entity or item. Action data plus condition. `checkAction(...)` predicts the status; `doAction(...)` validates, then fires |
| Action source | `cause/IActionSource` | World, position and cause of where an action started. For tracking and admin tools; never to be shown to players raw, because it reveals attacker locations |
| Action cause | `cause/IActionCause` | One link in the "who or what did this" chain |
| Condition | `conditions/ICondition` | Gate on a potential action; returns a status each time it is asked |
| Status | `status/IActionStatus` | Result or gate state, with tags and a message |
| Listener | `listener/IActionListener` | Pre-run veto hook, filtered by type tag |
| Action provider | `IActionProvider` | Looks up a potential action by tag, for example "the projectile-spawn action of this item" |
| Field | `data/ActionField` | Named, typed parameter (area size, target position, yaw, ...) |

## 3. Lifecycle of one action

1. **Choose data.** Either a fixed registry key (the EMP tower asks for `icbmclassic:emp.area`), or a stored `IActionData` (a missile carries its warhead data in NBT).
2. **Potential-action gate** (only when a potential action is used; direct callers skip to step 4):
   1. If there is no action data, return `MISSING_ACTION_DATA` (blocking).
   2. Ask the condition for its status. If the status carries the `blocking` tag, return it and stop. Anything else (ready, caution, waiting-without-blocking) passes.
   3. Build the cause chain (section 7). If the condition is a cause-providing one, its cause is put in front of the incoming cause.
   4. Reset the condition (a timer goes back to 0; the others do nothing).
3. **Create.** The data builds the action at the position with an `ActionSource` made of world, position and cause. Fields are applied to it. For generic data (`entity.spawn`) that is first the values stored in the data, then the values the caller's field provider supplies (the second layer wins, including with null; see ICBM-01-54). Custom data classes decide for themselves: the EMP data reads only `area.size`.
4. **Listeners.** The action goes to the listener handler. Listeners run in layer order (section 8). The first listener that returns *any* status stops the action and that status is the result, even if it is a "green" status. If none respond, `doAction()` runs.
5. **Result.** The status returns to the caller. Callers must test `isType(BLOCKING)` or `isType(GREEN)`, not compare keys; the keys may change.

Direct callers that skip the gate and the listeners: the blast commands, the nuclear blast chain, the explosive tile, the explosive entity capability and the red matter logic all call `data.create(...).doAction()` straight away (see ICBM-01-71). Only actions that go through a potential action (the EMP tower, the actionable and cluster missiles) reach the listeners.

## 4. Fields and action data

### 4.1 The field registry

An `ActionField` is a global singleton per string key, holding the key, a Java type and a pair of NBT save and load functions. Asking for an existing key with a different type is an error; the same type returns the existing field. Fields with no save/load pair are *runtime only* (they cannot be persisted).

| Field key | Type | NBT form | Meaning |
|---|---|---|---|
| `area.size` | float | float | Reach from the center. Size 3 means a 7-wide area (3 each way plus the center) |
| `host.entity` | Entity | none (runtime only) | The entity hosting the action, supplied by the entity |
| `host.vec3d` | Vec3d | compound (x, y, z) | Host position (runtime use) |
| `host.direction` | EnumFacing | byte | Which way the host faces (runtime use) |
| `impacted` | boolean | byte | True once the host hit a block or entity |
| `target.vec3d` | Vec3d | compound | Current target or aim point |
| `motion.vec3d` | Vec3d | compound | Motion to apply to something spawned |
| `yaw`, `pitch` | float | float | Rotation to apply |
| `entity.regName` | ResourceLocation | string | Entity type to spawn |
| `entity.data` | NBTTagCompound | compound | Extra entity save data to merge in |

### 4.2 Providers and receivers

A **field provider** answers "which fields do you have, and what are their values". Hosts build one from lambdas, so the values are read live (the actionable missile exposes `impacted`, `host.entity`, `host.vec3d` and `target.vec3d` this way). A **field receiver** accepts values; an action is both. "Apply fields" copies every field a provider declares into the receiver. A receiver that does not support a field ignores it.

### 4.3 Generic action data

`ActionDataBase` stores a map of field values, accepts only fields the data supports, and saves them as a list of `{key, value}` entries under `fields`. On load a key that is unknown, or that has no `value`, is dropped. `ActionDataGeneric` pairs a registry key, a "create the action" function and a supported-field list; it is how `icbmclassic:entity.spawn` is registered. Each registry build returns a fresh instance.

### 4.4 Base action

`ActionBase` holds world, a `Vec3d` position, source and data, and lazily derives the block position from the vector.

## 5. Type tags (MetaTag)

Action data and statuses are filtered by `MetaTag`: a globally unique, hierarchical label `namespace:path` with an optional parent. `isType(tag)` is true if any of the object's tags *is* the requested tag or a descendant of it. The tag tree (names are the stored paths):

| Tag | Parent | Meaning |
|---|---|---|
| `action` (root) | none | Everything |
| `action.destructive` / `.constructive` | action | Harmful / additive outcome |
| `action.blast` | action | Decorative: flavored like an explosion |
| `action.world`, `.world.edit`, `.world.area` | | World touched / edited / area-of-effect |
| `action.world.edit.block`, `.replace`, `.change`, `.remove`, `.add` | | Block edit sub-kinds (the last four hang off `...block`) |
| `action.entity`, `.creation`, `.projectile`, `.edit`, `.area` | | Entity actions; `projectile` spawns projectile entities |
| `action.status` (root) | none | Status tags |
| `...status.blocking` | status | **The real stop signal** |
| `...status.red`, `.red.error`, `.red.error.user`, `.red.error.dev` | status | Bad; error from the user / from code |
| `...status.yellow`, `.yellow.caution`, `.yellow.waiting` | status | Soft warning; "busy with a task" |
| `...status.green`, `.green.ready`, `.green.done` | status | Yes / ready / finished |

Reading guide from the source: *red* means "something is probably wrong, make the user pause" (a machine may let them override); *blocking* is the hard stop; *yellow* counts as yes; *green* is yes. Statuses combine tags (`error` plus `ready` means an error the user can force through).

Tag creation has two bugs (ICBM-01-40, ICBM-01-41).

## 6. Statuses

`IActionStatus` is a buildable object with tags and a `message()` text. Its old booleans `isError()` and `isBlocking()` are deprecated wrappers over the tags.

| Class | Behavior |
|---|---|
| `ImmutableStatus` | Fixed key plus tags; message key is `info.<regName>`. Factories: *blocking* (blocking tag), *error* (blocking plus error), *create* (given tags). Registered as shared constants |
| `MissingFieldStatus` | Blocking error naming a source and a field; the message takes those two as arguments. A generic one is registered as `icbmclassic:missing.field` (builds new, saves source and field) |
| `TimerCondition`'s `TimerTickingStatus` (`icbmclassic:conditional.timer.ticking`) | Tags waiting plus blocking; message shows current and target ticks; saves `tick_current`, `tick_target` |
| `ActionStatus` (legacy fluent builder) | Used by the launcher statuses. Overrides the two booleans and **has no tags**, so tag checks see nothing (ICBM-01-51) |

Shared constants in `ActionResponses` (all registered into the status registry; key prefix `icbmclassic:`):

| Constant | Key | Tags | When |
|---|---|---|---|
| `UNKNOWN_ERROR` | `action.error.unknown` | blocking, error | Catch-all |
| `ENTITY_SPAWN_FAILED` | `action.entity.spawning.failed` | blocking, error | `spawnEntity` returned false |
| `EXPLOSION_CANCELED` | `action.explosion.canceled` | blocking | Forge's explosion-start event was canceled |
| `VALIDATION_ERROR` | `action.validation.failed` | blocking | Block-state action found the wrong state |
| `BLOCK_PLACEMENT_FAILED` | `action.block.placement.failed` | blocking | `setBlockState` refused |
| `MISSING_WORLD`, `MISSING_HOST`, `MISSING_BLOCK_POS`, `MISSING_BLOCK_STATE`, `MISSING_VALIDATION`, `MISSING_ACTION_DATA` | `action.error.field.*` | blocking, error | A required piece is absent |
| `COMPLETED` | `action.completed` | done (green) | Success |
| `READY` | `action.ready` | ready (green) | Condition satisfied |
| `WORKING`, `WAITING` | `action.working`, `action.waiting` | blocking, waiting | Busy / not yet |

The English strings for all of these are missing from the lang file (ICBM-01-53).

## 7. Cause chain

An `IActionCause` is a buildable (registered, NBT-saved) link with a previous-cause pointer. The example in the source is "player to remote to screen to silo to cluster missile to missile". Specialized interfaces expose what a link knows: `ICausedByEntity` (an entity, null if it did not restore from save), `ICausedByBlock` (world, position, block state) and `ICausedByAction` (source plus action data).

Registered causes: `EntityCause`, `CausedByBlock`, `BlockScreenCause`, `RedstoneCause` (all described in `03-missiles` and `04-launch-control`, under `content/missile/logic/source/` and the launcher screen) and `CauseByTimer` (`icbmclassic:conditional.timer`, saves `timer`; here). The shared base saves a `name` and the `parent` link. `ActionSource` saves `dimension`, `pos` and `cause`.

Chain building in a potential action: a tile adds a *CausedByBlock* link in front of the incoming cause (`doAction(tile, cause)`); a cause-providing condition adds its own link in front. The "root cause" helper is wrong (ICBM-01-42), and the three composite conditions return no cause at all (ICBM-01-46), so condition causes never actually appear today.

Rule from the docs: the source is for mods and admin tooling only; players must not be shown raw coordinates.

## 8. Listeners

Listeners register against one or more tags. Internally they sit in a tree of layers that mirrors the tag tree: the root layer holds listeners with no tag, each child layer holds listeners for one tag plus deeper layers for sub-tags.

Dispatch for an action:
1. Run the listeners of the current layer in registration order; the first non-null status wins and ends dispatch.
2. For each child layer, if the action's data `isType` that layer's tag, recurse. First non-null status wins.
3. No response: the action runs.

Rules: a listener may modify the action (discouraged), must return null to stay out of the way, and returns any status to block (its status becomes the result). Replacing an action means returning a status that says so and firing a fresh action through the handler. The same listener object cannot be added twice to one layer (logged as an error), but can be added under different tags, in which case it can run twice for an action matching both (ICBM-01-55). Child layers are iterated from a hash map, so sibling order is unspecified.

*MIT-portable* (small; the only algorithm in this sheet): the layer insertion and dispatch in [`lib/actions/listners/ActionListenerLayer.java#L1-L82`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/lib/actions/listners/ActionListenerLayer.java#L1-L82). Note the map key is the tag, which hits ICBM-01-41.

Who registers listeners in ICBM itself: nobody. The handler exists for addons and protection mods.

## 9. Conditions

`ICondition` is a buildable with four hooks: `init(provider)` (receive the host's field provider), `onTick()` (advance), `getCondition()` (read-only status; "yes" means a non-blocking status), `reset()`. Conditions are asked every time, never cached by the caller. Composite conditions (`IConditionLayer`) forward init, tick and reset to children. `IConditionCause` conditions supply a cause for the chain; by convention only the *end-cap* conditions of an AND (the last one, or each branch of an OR) count, because earlier ones are validations.

| Condition (key `icbmclassic:`) | State saved | Behavior as designed |
|---|---|---|
| `timer` | `tick_current`, `tick_target`, `display_name` | Counts up to a target; intended to be ready when the count is reached. **Actually inverted** (ICBM-01-44). Cause: `CauseByTimer` |
| `impact` | `impact_desired` (default true) | Needs field `impacted`. Ready when the field equals the desired value, else `WAITING`. Missing field gives a blocking `MissingFieldStatus` |
| `projectile.target.distance` | `trigger_distance` (default 10), `invert` | Needs `target.vec3d` and `host.vec3d`. Each tick measures host-to-target distance. Not inverted: ready when closer than the trigger distance, else waiting. Inverted: ready when farther |
| `composition.and` | `conditions` list, `short_circuit` (default true) | Blocking if any child is blocking (returns that child's status), else `READY`. Ticks children in order and stops ticking at the first blocking child |
| `composition.or` | `conditions`, `short_circuit` | Designed: ready if any child is ready. **Actually returns the first child's status** (ICBM-01-43). Ticks children until one is non-blocking |

Example worked in the source and used by cluster missiles (`content/cluster/missile/CapabilityClusterMissileStack`): AND( impact must be *false*, distance to target under 3 ). That is "do not detonate on impact; detonate when within 3 blocks of the target". The same file shows the intended safety pattern in comments: timer (spin fuse), distance from shooter, then OR(no-hit kill timer, impact block, impact entity).

An empty AND or OR is ready.

## 10. Potential actions and providers

`PotentialActionImp` is the shared engine: a condition (`pre_check`), a field provider, and the gate/cause/create/listener flow of section 3. It is also ticked by its host (only on the server) to advance the condition. Variants:
- `PotentialAction` (`icbmclassic:basic`): holds an arbitrary action data object; saves `action_data` through the action registry.
- `PotentialActionKnown`: action data looked up lazily by registry key (the EMP tower's `empAction`); not itself registered.

Setting a condition or provider re-initializes the condition with the provider. **Loading from NBT does not**, so a reloaded condition has no provider (ICBM-01-50).

`ActionProvider` (`content/actions/ActionProvider.java`) is a map from tag to potential action, saved as `actions` (list of `key`, `value`). It is exposed as a capability *(1.12-only mechanism)*. Lookup is an exact tag match (the source notes "do key#isSubType" as undone), and its load depends on tag lookup that is broken (ICBM-01-40). Nothing in ICBM puts one on an item yet.

## 11. Concrete actions in this scope

| Action | Registry key | Tags | Fields | Behavior |
|---|---|---|---|---|
| `ActionSpawnEntity` | `icbmclassic:entity.spawn` | (none declared) | yaw, pitch, motion, entity name, entity data | Missing name: blocking `MissingFieldStatus`. Creates the entity by name (null: blocking status). Sets position to the action's. Motion: projectile entities use their own setter, others set raw motion. Yaw: given or random 0-360 (head and body follow for living). Pitch: given, otherwise buggy (ICBM-01-47). Entity data is merged over the entity's own save, then the UUID is restored. Spawns in the world; failure gives `ENTITY_SPAWN_FAILED`, else `COMPLETED` |
| `ActionUpdateBlockState` | not registered | not declared | none (lambdas) | Helper: if a validation function accepts the current state, apply a modify function and place the result. Missing function gives a `MISSING_*` status; validation false gives `VALIDATION_ERROR`; placement failure gives `BLOCK_PLACEMENT_FAILED`; else `COMPLETED` |
| EMP area | `icbmclassic:emp.area` | block edit, entity edit, destructive, world area, entity area | `area.size` | See [emp-tower.md](../05-detection-defense/emp-tower.md). Default size is the EMP blast scale from config; the field overrides it |
| Cluster | `ActionDataCluster` | | | Registered here, built in `content/cluster/`; see `03-missiles` |
| Blasts | the explosive registry (`IExplosiveData extends IActionData`) | | `area.size` among others | Each explosive is action data whose `create` makes a `Blast`; see `02-explosives` |

## 12. Replacing the deprecated Blast API

| Old (deprecated, `api/explosion/`) | Replaced by |
|---|---|
| `IBlast` (a blast that is also an action, with world/position, source entity, `clearBlast`) | `IAction` plus tags; world-edit tag for blasts. `getEntity()` and `getBlastSource()` stay for entity-hosted blasts |
| `IBlastFactory.create(world, x, y, z, source)` | `IActionData.create(...)` |
| `IBlastInit` setters (size, source, world, position, custom data, entity controller, explosive data) and `buildBlast()` | Field provider (`area.size` and so on) passed to `create` |
| `BlastBuildEvent` (posted from `buildBlast`, lets mods edit before the blast is locked) | Not replaced yet; the docs say a pre-run and post-run action event will replace it |
| `IBlastTickable` (blast ticks in the world) | Not an action concern; a ticking effect must spawn an entity or thread. `IBlastTickable.spawnEntity` says whether an explosive entity hosts it |
| `IBlastRestore` (save/load of an in-progress blast) | Actions are instant, so nothing to save |
| `IBlastMovable`, `IBlastVelocity`, `IBlastIgnore` | Planned as movement-action hooks and events |
| `IExplosiveCustomization` | Field provider |
| `ICBMExplosives` constants and the explosive registry | `ACTION_REGISTRY` with tags |

Housekeeping for the legacy blasts: `WorkTickingActionHandler` keeps a static list of live threaded blasts. On world unload it clears blasts of that dimension (this is buggy, ICBM-01-49). `removeNear(world, x, y, z, range)` clears and removes blasts closer than `range` (a negative range means the whole world; range 0 removes none) and returns the count; the lag command uses it.

## 13. Registries and setup (1.12-only mechanism for the events)

All five registries are `BuildableObjectRegistry` instances created in `ActionSystem.setup()` during mod pre-init, in this order: field definitions, then conditions, causes, statuses, action data, potential actions.

| Registry (field on the API class) | Unique name | Defaults registered | Event fired before locking |
|---|---|---|---|
| `CONDITION_REGISTRY` | `conditionals` | timer, impact, target distance, and, or | `ConditionalRegistryEvent` |
| `ACTION_CAUSE_REGISTRY` | `action.cause` | entity, block, block-screen, redstone, timer | `ActionCauseRegistryEvent` |
| `ACTION_STATUS_REGISTRY` | `action.status` | launcher statuses (partly, ICBM-01-52), `ActionResponses`, ticking, missing-field | `ActionStatusRegistryEvent` |
| `ACTION_REGISTRY` | `action.data` | EMP area, cluster, entity spawn | `ActionRegistryEvent` |
| `ACTION_POTENTIAL_REGISTRY` | `action.potential` | `basic` | `ActionPotentialRegistryEvent` |

Registry rules (`lib/buildable/BuildableObjectRegistry`): a key can be registered once (a second registration throws, naming the mod); an explicit "override" call replaces another mod's entry and logs it; after the event the registry is **locked** and any later registration throws as a "late registry". `getOrBuild(key)` calls the stored supplier, so each lookup gives a fresh object unless the supplier returns a shared one (the `register()` shortcut on a buildable shares the one instance).

Persistence rule (`IBuilderRegistry.save/load`): an object saves as `{id: <registry key>, data: <its NBT>}`; `data` is written only when the object is NBT-serializable and its tag is non-empty (or primitive). Load builds by `id` and, when the object is NBT-serializable and `data` exists, deserializes into it. Unknown ids load as null and are skipped in lists. Objects that are constants (most statuses) save only their id.

The action framework also publishes the `ACTION_LISTENER` handler, created before the registries.

## 14. What a rebuild has to keep

- A way to say "start X at P because of C" and get a tagged status back, with conditions able to gate it and a veto point. GregGuns can use plain Java types; the NBT-registry layer exists because 1.12 missiles persist their warheads, and GregGuns missiles will need the same persistence.
- The status vocabulary (blocking, error, waiting, ready, done) for GUI and redstone feedback.
- Cause chains, if admin/audit features are wanted. Cheap to drop otherwise.
- Not worth copying: the global tag map, the layer tree, the field registry's string keys (see the bugs).
