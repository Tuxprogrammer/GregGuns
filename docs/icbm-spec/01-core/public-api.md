# Public API Catalog

Source root (relative to `src/main/java/icbm/classic/` at `9c1d985a`): `api/`. Read in full for this sheet: `api/events/` (12), `api/reg/` (23), `api/caps/` (4), `api/data/` (13), `api/refs/` (2), `api/tile/` (2), `api/explosion/` (8), `api/actions/` (24), `api/radio/` (9), and the entry classes `ICBMClassicAPI`, `ICBMClassicHelpers`, `EnumTier`. `api/missiles/`, `api/launcher/` and `api/energy/` are described by `03-missiles`, `04-launch-control` and part a of this area (energy); their types are only named here where an event or registry carries them.

This package is the **extension surface** other mods use. ICBM itself reaches most of its own content through it, which is why it matters for a rebuild: each row says what the type is for, when it fires or who implements it, and whether it is deprecated. "Deprecated" is the source's own mark; the replacement is usually the action framework ([actions-framework.md](actions-framework.md)).

Kinds: **I** interface, **E** Forge event (posted on `MinecraftForge.EVENT_BUS`), **R** registry or registry-event, **C** capability *(1.12-only mechanism; the behavior it expresses is listed)*, **F** function type, **K** constants.

## 1. Entry point: `ICBMClassicAPI`

A static holder, mod id `icbmclassic`. Fields are filled at pre-init (`ICBMClassic.java`; `ActionSystem.setup()` fills the action ones).

| Field | Kind | Purpose | Set up by |
|---|---|---|---|
| `MISSILE_TARGET_DATA_REGISTRY` | R | Save/load of missile target data | Mod pre-init (`MissileTargetRegistryEvent`) |
| `MISSILE_FLIGHT_LOGIC_REGISTRY` | R | Save/load of flight logic | Mod pre-init (`MissileFlightLogicRegistryEvent`) |
| `CONDITION_REGISTRY`, `ACTION_REGISTRY`, `ACTION_POTENTIAL_REGISTRY`, `ACTION_STATUS_REGISTRY`, `ACTION_CAUSE_REGISTRY` | R | The five action registries | `ActionSystem.setup()` |
| `ACTION_LISTENER` | I | Handler for action listeners | `ActionSystem.setup()` |
| `PROJECTILE_DATA_REGISTRY` | R | Projectile data (for the rocket launcher and similar) | Mod pre-init (`ProjectileDataRegistryEvent`) |
| `EMP_CAPABILITY`, `MISSILE_CAPABILITY`, `MISSILE_HOLDER_CAPABILITY`, `MISSILE_LAUNCHER_CAPABILITY`, `RADIO_CAPABILITY`, `GPS_CAPABILITY`, `PROJECTILE_STACK_CAPABILITY` | C | Capability handles (injected by Forge). The comments say use the injector rather than these where possible. `MISSILE_CAPABILITY` applies only to entities | Registered in `registerCapabilities()` |
| `EXPLOSIVE_REGISTRY` and the four content registries `EX_MISSILE_REGISTRY`, `EX_GRENADE_REGISTRY`, `EX_BLOCK_REGISTRY`, `EX_MINECART_REGISTRY`; `EXPLOSIVE_CUSTOMIZATION_REGISTRY`; names `EX_MISSILE`, `EX_GRENADE`, `EX_BLOCK`, `EX_MINECART` | R | Legacy explosive system (section 6) | Mod init |
| `EXPLOSIVE_CAPABILITY`, `MISSILE_STACK_CAPABILITY` (item stacks only), `BLAST_VELOCITY_CAPABILITY`, `BLAST_CAPABILITY` | C | Legacy/explosive capabilities, deprecated | |

`ICBMClassicHelpers` is a static convenience layer over the capabilities: `getExplosive(int | name | entity | stack)` (falls back to the **condensed** explosive when an id or name is unknown and the caller did not ask for null), `isMissile(entity | stack)`, `getMissile(entity)` (does not check the capability exists), `isExplosive(entity)`, `isLauncher(tile, side)`, `getLauncher(tile, side)`, `getGPSData(stack)`. `EnumTier` is `ONE` (green), `TWO` (yellow), `THREE` (gold), `FOUR` (red), `NONE` (white), each with a tooltip color; the source notes colors should move into translations. Tier on explosive data is deprecated and now visual only.

## 2. Forge events (gameplay hooks)

| Event | Cancelable | When it fires | Fired by (area) |
|---|---|---|---|
| `LauncherEvent.PreLaunch` | yes | Before a launcher creates a missile. Carries the action source, the launcher, the missile holder, the target (before inaccuracy), a `simulate` flag and an optional `cancelReason` status (default: launcher "canceled" status). Fires for simulated launches too | `LauncherCapability` (04) |
| `MissileEvent.PostLaunch` | no | Right after the missile has its launch data, before motion | explosive-missile content registry (03) |
| `MissileEvent.PreImpact` | yes | Before the missile's impact code. Cancel = the missile ignores the hit and keeps going. The missile may be a tick short of the hit point; the ray result is supplied | `EntityMissile` (03) |
| `MissileEvent.PostImpact` | no | After impact code | `EntityMissile` (03) |
| `MissileEvent.EnteringSimQueue` | yes | A missile in an unloaded area is about to join the offline simulation queue. Cancel = no simulation. Docs warn cross-dimension handling belongs in flight logic, not here | `MissileTrackerHandler` (03) |
| `MissileChunkEvent.Load` / `.Unload` | yes | A missile is about to load a chunk (ticket plus time) / a missile-loaded chunk is about to unload. Unload cancel means "keep it loaded", and the listener must change the time or the same attempt repeats next tick | `MissileTrackerWorld` (03) |
| `MissileRideEvent.Start` / `.Stop` | yes | Before a player mounts / dismounts a missile. `Stop` is posted from the Forge mount event and its cancel result cancels the dismount | `EntityMissile`, `MissileEventHandler` (03) |
| `LauncherSetTargetEvent` | yes | A launcher screen or cruise launcher is given a new target. The listener may replace `target`; a null target becomes the zero vector | Screen, cruise launcher (04) |
| `RemoteTriggerEvent` | yes | Server side, player uses the remote detonator. Cancel = nothing is broadcast | `ItemRemoteDetonator` (05) |
| `LaserRemoteTriggerEvent` | yes | Server side, laser detonator target received. Listener may change the position (never null) and set a `cancelReason` lang key | `ItemLaserDetonator` (05) |
| `RadarGunTraceEvent` | yes | Server side, radar gun stores a position. The listener may change `pos` or cancel. (The change is ignored, ICBM-01-64) | `ItemRadarGun` (05) |
| `ExplosiveDefuseEvent.ICBMExplosive` / `.TNTExplosive` / `.ICBMBombCart` | yes | A player left-clicks an explosive with the defuser. Cancel = do not defuse | `ItemDefuser` (05) |
| `EmpEvent.EntityPre` / `.BlockPre` | yes | Before the EMP affects an entity / a block. Cancel removes all effects including item effects and entity changes | `ActionEmpArea` (05) |
| `EmpEvent.EntityPost` / `.BlockPost` | no | After effects were applied (the block post event is skipped on the block-swap path, per sheet 05) | `ActionEmpArea` (05) |
| `BlastBuildEvent` | no | A legacy blast's `buildBlast()` runs, just before the blast locks its settings; the listener can change size and so on through `IBlastInit`. Deprecated (a pre/post-run action event is planned) | `Blast.buildBlast` (02) |
| `BlastEvent` (base) | n/a | Abstract base of `BlastBuildEvent`: carries the action and gives world, x, y, z and the source entity. Deprecated | |

`EmpEvent` and `ExplosiveDefuseEvent` are marked deprecated, `LauncherEvent`, `MissileEvent` and `LaserRemoteTriggerEvent` too, all pointing at the future action-based events. They are the only events ICBM fires.

## 3. Registry and registry events

### 3.1 The buildable-object pattern (`api/reg/obj/`)

| Type | Kind | Purpose |
|---|---|---|
| `IBuildableObject` | I | Anything that can be recreated from a saved id: registry key, owning registry, default display name and tooltip (translation key is `<registry unique name>.<key>`), and a `register()` shortcut that registers a shared instance |
| `IBuilderRegistry<T>` | R | `register(key, supplier)`, `getOrBuild(key)`, `getUniqueName()`, `isLocked()`, plus NBT helpers: save/load one object or a list as `{id, data}` |

Implementation: `lib/buildable/BuildableObjectRegistry` (single registration per key, lockable, with an explicit override call). See [actions-framework.md section 13](actions-framework.md#13-registries-and-setup-112-only-mechanism-for-the-events) for the rules.

### 3.2 Registry events (`api/reg/events/`) *(1.12-only mechanism: Forge event bus used as a registration window)*

Each is posted once during setup; the carried registry is locked right after, so late registration throws.

| Event | Carries | Lets an addon register |
|---|---|---|
| `ConditionalRegistryEvent` | condition registry | New condition types |
| `ActionCauseRegistryEvent` | cause registry | New cause types (missile sources) |
| `ActionStatusRegistryEvent` | status registry | New status types |
| `ActionRegistryEvent` | action data registry | New action data (blasts, area effects, spawns) |
| `ActionPotentialRegistryEvent` | potential-action registry | New potential-action types |
| `MissileTargetRegistryEvent` | target data registry | New missile target data |
| `MissileFlightLogicRegistryEvent` | flight logic registry | New flight logics |
| `ProjectileDataRegistryEvent` | projectile data registry | New projectile data |
| `ExplosiveCustomizationRegistryEvent` | customization registry | New blast customizations (deprecated) |
| `ExplosiveContentRegistryEvent` | explosive registry | New content types for explosives (deprecated; "removing in 1.13"). Has `register(id, contentRegistry)` |
| `ExplosiveRegistryEvent` | explosive registry | New explosives (deprecated). Chained `register(id, tier, blastFactory)` then `enableContent(contentId)` for the last one, then `done()`. An unknown content id is only logged |

### 3.3 Legacy explosive registries (`api/reg/`, `api/reg/content/`), all deprecated ("being removed in 1.13")

| Type | Purpose |
|---|---|
| `IExplosiveData` | An explosive: a legacy numeric id (saved to config and assigned automatically), a tier (default NONE), and it **is** an `IActionData`. Comparable |
| `IExplosiveRegistry` | Registers explosives (name, tier, blast factory), looks them up by name or id (name lookup can be told to return null), lists all, holds the content registries by name |
| `IExplosiveCustomization` | A layer that edits a blast before it spawns, plus tooltip lines; replaced by field providers |
| `IExplosiveContentRegistry` | A "kind of device an explosive can exist as": which explosives are enabled for it, the stack for a given explosive, lock. Four are used: missile, grenade, block, minecart |
| `IExMissileRegistry` | Adds launch listener, per-tick update listener, player-interaction listener (for example the Ender missile storing teleport coordinates), and the triggers that run them |
| `IExFuseBlockRegistry`, `IExBlockRegistry` | Block explosives: fuse time supplier, fuse tick listener, activation (click) listener |
| `IExFuseEntityRegistry`, `IExGrenadeRegistry`, `IExMinecartRegistry` | Entity explosives (grenade, bomb cart): fuse time supplier and fuse tick listener |

## 4. Capabilities (`api/caps/`) *(1.12-only mechanism)*

| Type | Attached to | Behavior it expresses |
|---|---|---|
| `IEMPReceiver` | Any item, entity or tile (or registered by block) | How a thing reacts to an EMP: `applyEmpAction(world, x, y, z, action, power, doAction)` returns remaining power for shielding (power 0 to 1, above 1 means overpowered; `doAction` false simulates). Overload with the containing object, so stored items can be reached (inventory of a player, chest, chest cart). `getEmpResistance`, `isEmpResistanceFlat` (flat vs ratio) for simulation and display; `shouldEmpSubObjects` lets the EMP descend into contents. Docs: ICBM's EMP is game-like, it does not weaken with distance; only shielding reduces it, and shielding should never reach 100 percent. Described with its use in [emp-tower.md](../05-detection-defense/emp-tower.md) |
| `IExplosive` | Entities (explosive, grenade, cart) and stacks | Deprecated. Provides the explosive data, accepts customizations and applies them to a blast, lists tooltip info, converts to a stack, and has a deprecated `onDefuse` |
| `IGPSData` | Item stacks (radar gun) | Stores a position and a dimension id, either clearable. World helpers (`getWorld` resolves through the dimension manager on the server) |
| `IMissileHolder` | Launcher inventory (may be a wrapper on a multiblock) | The missile slot: get the stack, `hasMissile`, insert with simulate, consume, and `canSupportMissile`. Source says it should become a list |

Other capabilities named by the API class (`IMissile`, `IMissileLauncher`, `IProjectileStack`, `ICapabilityMissileStack`, `IRadio`, `IBlast`, `IBlastVelocity`) are in their own areas.

## 5. Actions API (`api/actions/`) and radio API (`api/radio/`)

These are specified by behavior in [actions-framework.md](actions-framework.md) and [radio-bus.md](radio-bus.md). Index:

| Package | Types |
|---|---|
| `actions/` | `IAction`, `IActionData`, `IPotentialAction`, `IActionProvider` |
| `actions/cause/` | `IActionCause`, `IActionSource`, `ICausedByAction`, `ICausedByBlock`, `ICausedByEntity` |
| `actions/conditions/` | `ICondition`, `IConditionCause`, `IConditionLayer` |
| `actions/data/` | `ActionField`, `ActionFields`, `IActionFieldProvider`, `IActionFieldReceiver`, tag holders `ActionTypes`, `WorldActionTypes`, `BlockActionTypes`, `EntityActionTypes` |
| `actions/listener/` | `IActionListener`, `IActionListenerHandler` |
| `actions/status/` | `IActionStatus`, `ActionStatusTypes` |
| `radio/` | `IRadio`, `IRadioChannelAccess`, `IRadioReceiver`, `IRadioSender`, `IRadioMessage` |
| `radio/messages/` | `IIncomingMissileMessage`, `ITargetMessage`, `ITextMessage`, `ITriggerActionMessage` (deprecated) |

## 6. Legacy blast API (`api/explosion/`), all deprecated

| Type | Purpose | Implemented by |
|---|---|---|
| `IBlast` | A blast that is also an action: size hint (default -1), completion flag (default true), explosive data, hosting entity, source entity (defaults to the host), `clearBlast()` for admin tools, world and position bridging | `Blast` and subclasses (02) |
| `IBlastInit` | The builder step: set action source, size, scale, source entity, world, position, custom NBT, controlling entity, explosive data, then `buildBlast()` which locks it. Most setters default to a no-op | `Blast` (02) |
| `IBlastFactory` | `create(world, x, y, z, source)` returning an action; what an explosive registers | Each explosive in `lib/explosive` (02) |
| `IBlastTickable` | A blast that ticks in the world; `onBlastTick(age)` returns true to finish; `spawnEntity()` says whether an entity hosts it | Ticking blasts (02) |
| `IBlastRestore` | Save and load an in-progress blast | Restorable blasts (02) |
| `IBlastMovable` | A blast that can be moved (red matter): `isMovable`, `onPositionUpdate` | Red matter (02) |
| `IBlastIgnore` | Entity-side: "can I ignore this blast" | Entities (02) |
| `redmatter/IBlastVelocity` | Entity-side: handle being pushed or pulled by a blast yourself (`onBlastApplyMotion`) | Entities that opt in |

## 7. Data and function types (`api/data/`)

| Type | Kind | Purpose |
|---|---|---|
| `meta/MetaTag`, `meta/ITypeTaggable` | K, I | Hierarchical labels and the `isType` check; see [actions-framework.md section 5](actions-framework.md#5-type-tags-metatag) |
| `IBoundBox<POS>` | I | Lower and upper corner; used for radio ranges |
| `IWorldPosition` | I (deprecated) | An object with a world and 3D position; helpers for client/server and block or vector position |
| `BlockActivateFunction` | F | `(world, pos, player, hand, facing, hit xyz) -> boolean` for explosive block clicks |
| `EntityInteractionFunction` | F | `(entity, player, hand) -> boolean` for missile interaction |
| `EntityTickFunction` | F | `(entity, tick)` fuse tick |
| `WorldTickFunction` | F | `(world, x, y, z, tick)` block fuse tick |
| `WorldEntityIntSupplier` | F | `(entity) -> int` entity fuse time |
| `WorldPosIntSupplier` | F | `(world, x, y, z) -> int` block fuse time |
| `D3Consumer`, `Int3Consumer`, `Int3Looper` | F | Coordinate callbacks; the looper stops when it returns false |

## 8. References and tile marker types

| Type | Purpose |
|---|---|
| `refs/ICBMEntities` (K) | Registry names of ICBM entities, all in the `icbmclassic` domain: `block.gravity`, `block.fragment`, `block.explosive`, `explosive_missile`, `generic_missile`, `surface_to_air_missile`, `held_item_missile`, `holder.explosion`, `beam.light`, `item.grenade`, `cart.bomb`, `holder.seat`, `bomb_droplet`, `parachute`, `balloon`, `redmatter`, `smoke`. The source notes the names are inconsistent and should match vanilla style |
| `refs/ICBMExplosives` (K, deprecated) | Static slots filled at registration with each explosive's data. Tier 1: condensed, shrapnel, incendiary, debilitation, chemical, anvil, repulsive, attractive, color, smoke. Tier 2: fragmentation, contagious, sonic, breaching, thermobaric. Tier 3: nuclear, EMP, exothermic, endothermic, anti-gravitational, ender, hypersonic (deprecated placeholder, kept so old saves load). Tier 4: antimatter, red matter. Blast-only: missile module (deprecated), mutation, rot, cluster. Full blast catalog in `02-explosives` |
| `tile/IRotation`, `tile/IRotatable` | Deprecated markers for facing direction (read) and set direction (not all support it). Slated for removal |

## 9. Observations for the rebuild

- The **live extension points** worth keeping as GregGuns events: launch (pre), set-target, remote and laser trigger, missile impact (pre), missile ride, EMP per entity and block, and defuse. They are the veto points protection and claim mods need. The "pre" events being cancelable and the "post" ones not is a consistent rule.
- The registry events and the buildable pattern solve 1.12 save/load of polymorphic objects. GregGuns needs the persistence but not the event-window mechanism.
- Everything under legacy explosives and `api/explosion/` is labeled "going away" by its own authors; do not model it.
- `IEMPReceiver` is the one capability with real semantics (shielding). Its GT mapping is a design question for `05-detection-defense`.
