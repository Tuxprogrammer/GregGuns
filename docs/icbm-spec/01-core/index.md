# 01-core: core systems

## Overview

The plumbing every other area stands on, in ICBM-Classic `prod/1.12` @ `9c1d985a`:
- **Part a:** mod lifecycle and registration, every top-level config key, the Forge Energy model, the `/icbm` commands, the JEI, Mekanism and datafix integrations, and a catalog of network packets.
- **Part b:** the actions framework that expresses blasts, EMP and spawns; the radio bus; the event tracker; and a catalog of the public API (interfaces, registries, events and capabilities).

GregGuns replaces most of this with GT equivalents: EU instead of Forge Energy, and data sticks instead of the radio ([D13](../../gregguns-icbm-spec/DECISIONS.md#decided)). These sheets are the behavioral contract the replacements must cover.

## Contents

- [lifecycle-and-registration.md](lifecycle-and-registration.md): mod identity, the five load phases and what registers when, the registry build order, the content-enable flags and the unconditional recipes, loot injection, creative tab, dispenser behaviors, the GUI handler and client/server proxy hooks.
- [config.md](config.md): every key of the eight top-level config classes (key, default, range, effect), how the config reload works, and the pointers to the blast, missile and machine config sheets.
- [energy-model.md](energy-model.md): the Forge Energy buffer, the adapter layer, how each machine accepts, stores and spends power, the battery item, and a keep/drop list for the EU replacement.
- [commands.md](commands.md): `/icbm` and its subcommands (`help`, `remove`, `lag`, `blast list/trigger/spread`) with arguments, permission and effects.
- [integrations.md](integrations.md): JEI plugin, Mekanism hooks (EMP swap, teleporter, recipes), the Atomic Science armor defaults, and what each of the 7 datafixers migrates.
- [network-and-plumbing.md](network-and-plumbing.md): the 6-packet catalog, the codex (lambda) packet catalog with fields and intended direction, the server's trust model, and one-line lists of the 1.12-only plumbing classes.
- [actions-framework.md](actions-framework.md): what an action is, the lifecycle (potential-action gate, cause chain, listeners, status), action data and fields, the type-tag tree, statuses, conditions, the listener layers, the registries, and how it replaces the deprecated Blast API. Also covers `entity.spawn` and the block-state helper action.
- [radio-bus.md](radio-bus.md): channels, the radio map and routing, senders and receivers, the five message types, what each block and item does with them, and a checklist for replacing the bus with data sticks (decision D13).
- [event-tracker.md](event-tracker.md): the read-only structured-event channel in `lib/tracker`, its data model, the read-only payload check, and the four packet diagnostics that are its only users.
- [public-api.md](public-api.md): catalog of `api/`: entry class and helpers, every Forge event, the registries and registry events, capabilities, the action and radio API index, the deprecated blast and explosive API, data and function types, references.

## Key facts

### Part a: lifecycle, config, energy, commands, integrations

- `EnergyBuffer` reports the full requested amount as extracted even when less is stored. As a result every machine runs on any non-zero charge: a launcher fires with 1 FE, the EMP tower is ready with 1 FE, the radar runs on a trickle. The effective cost of an action is whatever is in the buffer, capped at the stated cost (ICBM-01-01, -02).
- The battery item can receive power but never give it back (extraction is never enabled), and it spawns empty. It is a dead end as a power source (ICBM-01-03).
- The server trusts client packets heavily. Every machine packet only checks dimension and loaded chunk, never distance or the open GUI, and the declared direction flags are ignored. A modified client can fire any EMP tower, launch from any screen or cruise launcher, set stored energy, and rewrite any missile's flight state through the entity packet (ICBM-01-20 to -25).
- Content flags (`enable_battery`, plates, circuits ...) only stop item registration. Their recipes still load and fail (ICBM-01-31).
- `use_energy=false` zeroes the EMP tower's costs and hides the energy capability on four machines.
- Almost all integration code targets 1.12 mods that GTNH lacks (JEI, Mekanism, Atomic Science); datafixers do not exist on 1.7.10.
- Forge `@Config`, capabilities, registry events, `ObjectHolder`, `IFixableData`, netty `IMessage` plumbing, and the whole Forge Energy stack are *1.12-only mechanisms*. The sheets describe the behavior they produce.

### Part b: actions framework, radio bus, event tracker, public API

- An **action** is instant and one-shot: data plus place plus cause gives a tagged status. Multi-tick work is never an action. Only actions run through a **potential action** (EMP tower, actionable and cluster missiles) pass the listener hooks; blasts started by commands, tiles, the nuclear chain and capabilities call `create(...).doAction()` directly and skip them.
- The status tag `blocking` is the only hard stop; `red`/`error` means "probably wrong", `yellow` and `green` count as yes.
- The **radio bus is a dimension-wide, name-matched broadcast**: no range, no ownership, no security. Channels default to 4 random alphanumeric characters. A radar re-sends a "trigger" with the first threat every 40 ticks; the launcher screen, cruise launcher and EMP tower react. In practice every receiver is "full map"; the chunk index, range overlap and sender cache are dead or broken code.
- Condition logic is fragile: the **timer is inverted**, the **OR condition returns only its first child**, and conditions lose their field provider when a missile is reloaded from save, so a cluster missile in flight across a world save probably never detonates.
- The **MetaTag registry is keyed inconsistently** (stored by path, searched by `namespace:path`), so lookups fail and tags duplicate. As a map key, `MetaTag` very likely overflows the stack through a cyclic hash.
- The event tracker is a well-meant audit/debug channel that ICBM uses for four packet warnings only. Its "read-only payload" check lets mutable collections through.
- The public API has a small, consistent event model (cancelable "pre", plain "post") that is worth keeping in a GregGuns version; the legacy explosive and blast API is labeled "going away" by its own authors.
- Capabilities, the Forge registry events, and `@CapabilityInject` handles are *1.12-only mechanisms*; the sheets describe the behavior they produce.

## Related areas

### Part a: lifecycle, config, energy, commands, integrations

| Area | Why |
|---|---|
| Part b of `01-core` | The radio bus, action system and API registries that `preInit` builds and that commands/packets call |
| `02-explosives` | Explosive registry contents and IDs, blast configs, flying blocks, gas/radiation armor use |
| `03-missiles` | Missile entity (the `description` codex), cargo, cluster and held-item allow lists, flight and target registries |
| `04-launch-control` | Launcher family energy and packets, `ConfigLauncher` |
| `05-detection-defense` | Radar and EMP tower (energy numbers are specified there), the EMP block-swap table |
| `06-blocks-items` | Item behavior, recipes, battery recipe, loot-affecting crafting items |
| `07-visuals-audio` | Particle classes, lang keys, textures |

### Part b: actions framework, radio bus, event tracker, public API

| Area | Why |
|---|---|
| `01-core` part a | Config groups (`icbmclassic/radar` `sam_delay`), lifecycle order in which `ActionSystem.setup()` runs, energy |
| `02-explosives` | The legacy `Blast` classes that implement `IBlast`, `BlastBuildEvent`, explosive registries, the blast commands that call `create(...).doAction()` |
| `03-missiles` | `IMissile`, flight logic and target registries, causes (`EntityCause`, `CausedByBlock`, `RedstoneCause`), `ActionDataCluster`, the missile events, `EntityMissileActionable` which uses conditions |
| `04-launch-control` | Launcher screen and cruise launcher radio receivers, `LauncherStatus`, `LauncherEvent`, `LauncherSetTargetEvent` |
| `05-detection-defense` | The EMP action itself ([emp-tower.md](../05-detection-defense/emp-tower.md)), the radar as radio sender, detonator items, `IEMPReceiver` use |
| `06-blocks-items` | Recipes for the radio-capable blocks |
| `07-visuals-audio` | Lang entries for the status and radio messages |

## Coverage

### Part a: lifecycle, config, energy, commands, integrations

Source files read in full: 74 of 74 in the full-read scope.
- `ICBMClassic`, `CommonProxy`, `ICBMConstants` (3 of 3).
- `config/` top level: `ConfigMain`, `ConfigBattery`, `ConfigClient`, `ConfigDebug`, `ConfigFlyingBlocks`, `ConfigItems`, `ConfigJEI`, `ConfigThread` (8 of 8).
- `lib/energy/` storage and system (7 of 7) and `api/energy/` (2 of 2).
- `command/` (13 of 13: `ICBMCommands`, `CommandUtils`, `sub/` 3, `sub/blast/` 3, `system/` 5, ...).
- `mods/` (`ModInteraction`, `ModProxy`, `mekanism/MekProxy`, `jei/` 7: 10 of 10).
- `datafix/` (9 of 9, including `DataFixerHelpers`).
- `lib/network/` (22 of 22: transport, 5 packet classes, the codex classes, `PacketEvents`, `IPacket`, `IPacketIDReceiver`, the exception).
- Also read in full to resolve behavior: `client/ICBMCreativeTab`, `client/ClientProxy`, `content/items/behavior/*` (2), `content/items/ItemBattery`, `prefab/item/ItemICBMElectrical`, `ItemStackCapProvider`, `LootEntryItemStack`, `ItemRadio`, `prefab/inventory/InventorySlot`, `content/entity/flyingblock/FlyingBlock`, `content/gas/ProtectiveArmorHandler`, `content/actions/emp/EmpHandler`, `config/util/BlockReplacementData`.

Read in part (for cross-references only): `ItemReg` (`registerItems`, the object-holder names), `BlockReg` (`registerBlocks`), `ClientReg` (model registration for crafting items and battery), `TileEMPTower`, `TileRadarStation`, `TileLauncherBase`, `TileLauncherScreen`, `TileCruiseLauncher` (their energy fields and `register`/codex blocks), `ActionEmpArea` and `CapabilityEmpInventory` (the callers of the block-swap table and of `setEnergy`), `BlockRadioactive` (the radiation rule), `GuiEMPTower` (tooltip), `ItemLaserDetonator` and `ItemRadarGun` (`read`), `EntityMissile` (codex and `read`), `ICBMClassicHelpers.getExplosive`, `ExplosiveInit` (the explosive ID list), `NBTConstants` (key names), `ConfigLauncher`, `ConfigRadar`, `ConfigEmpTower` (key names and defaults only), `en_us.lang` (command and config strings), recipe JSON (`battery`, `_factories`, `parts/vanilla`, `parts/mod`, `explosives/mekanism`), `ConditionOreValue`.

Skimmed only (one-line catalogs, no behavior): `config/util/` (6 classes; `ResourceConfigList` and `BlockStateConfigList` partly read), `lib/saving/` (21), `lib/transform/` (16), `prefab/` GUI, button, tooltip and base classes (the rest of the 36), `client/` proxies.

Assets at catalog depth: none of this part owns assets. Lang keys quoted: `command.icbmclassic:icbm.*`, `config.icbmclassic:*.title`, `item.icbmclassic:battery.*`.

### Part b: actions framework, radio bus, event tracker, public API

Source files read in full: 143 of 143 in scope, plus 2 of the 3 EMP files; `content/actions/emp/ActionEmpArea.java` was excluded as assigned (documented in 05).
- `lib/actions/` 18 of 18; `api/actions/` 24 of 24; `content/actions/` 8 of 8 outside `emp/` (and `ActionDataEmpArea`, `EmpHandler`).
- `lib/radio/` 11 of 11; `api/radio/` 9 of 9.
- `lib/tracker/` 9 of 9.
- `api/events/` 12 of 12; `api/reg/` 23 of 23; `api/caps/` 4 of 4; `api/data/` 13 of 13; `api/refs/` 2 of 2; `api/tile/` 2 of 2; `api/explosion/` 8 of 8.

Also read to resolve references (only the parts that matter): `ICBMClassicAPI`, `ICBMClassicHelpers`, `EnumTier` (the head), `lib/buildable/BuildableObjectRegistry` (in full); `content/missile/logic/source/ActionSource` and `cause/ActionCause` (in full); the radio receivers `RadioScreen`, `RadioCruise`, `RadioEmpTower`, `RadioRadar`, `prefab/FakeRadioSender`, `prefab/item/ItemRadio` (in full); the tile `onLoad`/`invalidate` registration in `TileLauncherScreen`, `TileEMPTower`, `TileCruiseLauncher`; `TileRadarStation` (only the radio lines and broadcast tick); the post sites of every event (about 15 lines each); `EntityMissileActionable` and `CapabilityClusterMissileStack` (condition use); `LauncherStatus` (head); `Blast.buildBlast` and `clearBlast`; `ICBMClassic.java` (setup order); `PacketEvents`, `PacketEventUtils`, `PacketTileEvents` (in full); `ConfigRadar`; the radio, status and info sections of `en_us.lang`.

Assets: none in scope. Lang strings are cited in the sheets; the English text for action statuses and for `tile.emptower.radio.success` does not exist.

## Known limits

### Part a: lifecycle, config, energy, commands, integrations

- No code was run. All behavior comes from reading the source. The consequences of the extract defect (machines running on any non-zero charge) assume the default costs are not above the capacities, which holds for the launcher family (1200 / 1200), the radar (1000 / 20,000) and the EMP tower (derived).
- Mekanism block metadata numbers (basic block 7 = teleporter frame, machine block 11 = teleporter) are taken from the code and its comments; they were not checked against Mekanism.
- Forge-version details were not verified: the exact moment the config classes are first synced relative to the registry events, how chat translation handles the `%d` / `%f` formats given to the lag and blast messages, and the order in which registry events fire. The lifecycle sheet gives the order as it appears in the code.
- The packet codex IDs in the network sheet follow the order of `register` calls and assume `EntityMissile` loads after the tile classes; the actual order depends on class loading and was not traced.
- `lib/saving`, `lib/transform` vector math and most of `prefab/gui` were not read, only named. The `EnergyBuffer` extraction analysis, the packet trust analysis and the datafixer findings are the parts a rebuild relies on.
- Sounds, textures and models could not be seen or heard; this part has none.
- The Mekanism teleporter hook's rotation math is described in outline, not verified for every entry/exit combination.

### Part b: actions framework, radio bus, event tracker, public API

- No code was run. Bugs that depend on Java behavior (the `MetaTag` hash, the concurrent-modification on world unload, the `Boolean` rejection in the tracker scan) are read from source and marked "unverified at runtime" in the table where they matter.
- `api/missiles/`, `api/launcher/`, `api/energy/`, and the `content/missile/logic/source/` cause classes (except `ActionSource` and `ActionCause`) are other areas and were not catalogued here.
- The wire format of radio messages does not exist (messages are in-memory objects, server only); there is no packet to document.
- The event tracker has no listeners other than the four packet loggers; the "audit trail" use is described from the author's comments, not from behavior.
- Which listener registrations or conditions other addons use is unknown. ICBM registers no action listener and no `ActionProvider` entry itself.
- The EMP action (`ActionEmpArea`) and its event posts are described in 05; this area only lists the events.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity | Kind |
|---|---|---|---|---|---|
| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-01-01 | Extraction reports more than is stored | `lib/energy/storage/EnergyBuffer.java:111-115` | `extractEnergyInternal` returns `min(capacity, requested)` instead of `min(stored, requested)`; the stored value is clamped at 0 but the caller is told the full amount was removed | gameplay | logic |
| ICBM-01-02 | Machines run on any non-zero charge | `EnergyBuffer.java:125`; callers `TileEMPTower.java:323`, `LauncherCapability.java:69`, `TileRadarStation.java:149` | Consequence of -01: `consumePower(cost)` is true with 1 FE stored, so launchers fire, the EMP tower is ready and the radar runs on a trickle | gameplay | logic |
| ICBM-01-03 | Battery can never discharge, and starts empty | `content/items/ItemBattery.java:40`; `EnergyBuffer.java:158` | The battery buffer sets an output limit but never enables extraction (default false), so `dischargeItem` gets 0. No charger exists, no creative full variant | gameplay | logic |
| ICBM-01-04 | Item discharge ignores the machine's receive limit | `EnergyBuffer.java:132-135` | Takes all free room out of the item, then `receiveEnergy` applies the receive limit; the difference vanishes (EMP tower limit 10,000) | gameplay | logic |
| ICBM-01-05 | `setEnergy` always drains everything | `lib/energy/system/EnergySystemFE.java:79-88` | Writes the target value by reflection, then removes `Integer.MAX_VALUE`; the target is never kept. For non-vanilla storages (including the ICBM battery) the drain does nothing | gameplay | logic |
| ICBM-01-06 | Change callback receives the unclamped value | `EnergyBuffer.java:176` | `current` is the requested energy, not the stored amount; also fires when clamping leaves the value unchanged | cosmetic | logic |
| ICBM-01-07 | Wrong config comment for `use_energy` | `config/ConfigMain.java:28` | Comment reads "Range of tier 1 launcher" | cosmetic | logic |
| ICBM-01-08 | EMP tower tooltip spends power client-side | `content/blocks/emptower/gui/GuiEMPTower.java:66` | The tooltip lambda calls `consumePower(cost, false)` (not a simulation) on the client's copy, draining its charge display each time the tooltip is evaluated | visual | 1.12-only |
| ICBM-01-09 | Client item GUI uses the wrong path | `CommonProxy.java:97` | Client branch for `GUI_ITEM` looks up an entity by ID and calls the server overload instead of the client slot overload | gameplay | 1.12-only |
| ICBM-01-10 | `/icbm lag` radius is read from the wrong argument | `command/sub/CommandLag.java:45` | Reads the second argument after `lag`; the help text shows the first, so `/icbm lag 50` uses the 1000 default | gameplay | logic |
| ICBM-01-11 | Malformed format in `/icbm remove` output | `lang/en_us.lang:51` | `%1%d` never inserts the count; prints "Removed %d entities within N meters" | cosmetic | logic |
| ICBM-01-12 | `remove explosions` misses running blasts | `command/sub/CommandRemove.java:136` | Selector matches only `EntityExplosive`, not the running-blast entity `EntityExplosion`; only `all` and `lag` remove those | gameplay | logic |
| ICBM-01-13 | Blast commands have no limits | `command/sub/blast/CommandBlastTrigger.java:88,106`; `CommandBlastSpread.java:53-66` | Size, count and distance are unbounded; `spread 100` queues 40,401 blasts in one tick; raw float parsing throws on bad input | gameplay | logic |
| ICBM-01-14 | Radar fixer reads only one spelling of the old keys | `datafix/TileRadarStationDataFixer.java:25,33` | Checks for either `alarmBanJing` or `alarmRadius` but always reads `alarmBanJing`; saves with the second spelling get range 0 (same for the safety range) | gameplay | 1.12-only |
| ICBM-01-15 | Hypersonic-to-sonic fix on missiles never matches | `datafix/EntityMissileDataFixer.java:61-63` | Tests for the key `explosive` then reads the compound `explosive_stack`; the entity saves the stack under `explosive` | gameplay | 1.12-only |
| ICBM-01-16 | Bomb cart fixer expects an int | `datafix/EntityBombCartDataFixer.java:20` | `explosive` is a compound in current saves; `getInteger` reads 0 and the fix applies only to the very old format | gameplay | 1.12-only |
| ICBM-01-17 | S-Mine ID shift is inconsistent | `datafix/TileExplosivesDataFixer.java:25-29`; `EntityExplosiveDataFixer.java:49`; `EntityMissileDataFixer.java:108` | Only the tile fixer remaps ID 14 and shifts higher IDs, and it compares to the current hypersonic ID before shifting (an old ID 21 maps to sonic). Entities and missiles use the raw old ID; an unknown ID returns null and is dereferenced | gameplay | 1.12-only |
| ICBM-01-18 | Dispenser grenades ignore vertical facing and spawn inside the block | `content/items/behavior/GrenadeDispenseBehavior.java:37` | Heading uses only X and Z of the facing plus a fixed 0.1 up; position is the dispenser center | gameplay | logic |
| ICBM-01-19 | Sulfur and saltpeter ore names registered twice, once with a possibly null item | `content/reg/ItemReg.java:187-188` | Second registration uses the `ObjectHolder` fields while the item registry event is still running; likely an empty stack (logged and denied) | cosmetic | 1.12-only |
| ICBM-01-20 | Packet direction flags are never enforced | `lib/network/lambda/PacketCodex.java:52-54` (flags); `PacketLambdaTile.java:94`; `PacketLambdaEntity.java:93` | `fromClient()` / `fromServer()` set flags that no handler reads, so clients can send server-to-client codexes | gameplay | logic |
| ICBM-01-21 | Machine packets have no reach, GUI or ownership check | `lib/network/lambda/tile/PacketLambdaTile.java:94-125` | Only dimension and loaded-chunk checks; any client can fire an EMP tower, launch a screen or cruise launcher, retarget, retune radios, at any distance | gameplay | logic |
| ICBM-01-22 | Client can set stored energy | `TileEMPTower.java:428-433` (`gui` codex); `TileCruiseLauncher.java:410-415` | The GUI codex writes the received energy value straight into the buffer; on the cruise launcher it is declared serverbound | gameplay | logic |
| ICBM-01-23 | Client can rewrite any missile | `content/missile/entity/EntityMissile.java:373-374` | The `description` codex loads a received NBT compound into the missile with no direction or ownership check | gameplay | logic |
| ICBM-01-24 | `PacketPlayerItem` trusts the slot ID and runs off-thread | `lib/network/packet/PacketPlayerItem.java:64-77` | Negative IDs call the receiver of any item ID; positive IDs index the inventory unchecked; receivers run on the netty thread | gameplay | logic |
| ICBM-01-25 | Laser detonator and radar gun trust client coordinates | `content/items/ItemLaserDetonator.java:92-125`; `ItemRadarGun.java:167-177` | Target position is whatever the client sends; no line of sight, range or hit check | gameplay | logic |
| ICBM-01-26 | One network packet per destroyed block | `CommonProxy.java:165`; `lib/network/packet/PacketSpawnBlockExplosion.java` | Each broken block sends a packet to everyone within 256 blocks; large blasts flood the network | gameplay | logic |
| ICBM-01-27 | Chest loot can be empty and uses a private random | `prefab/item/LootEntryItemStack.java:33` | `nextInt(count)` yields 0 to count-1, so empty stacks are added and the maximum is never reached; uses a static `Random`, not the loot context | cosmetic | logic |
| ICBM-01-28 | Packet codec swallows encode errors | `lib/network/netty/PacketEncoderDecoderHandler.java:48,68` | An exception while writing is logged and the partly written buffer is still sent, desynchronizing the stream | gameplay | 1.12-only |
| ICBM-01-29 | EMP tower range has no lower bound | `content/blocks/emptower/TileEMPTower.java:288` | `setRange` applies only a maximum; a client can send zero or negative ranges | gameplay | logic |
| ICBM-01-30 | Config list defaults accumulate on reload | `config/util/ResourceConfigList.java:150-153,160` | `reset()` does not clear default matchers, and the callbacks add defaults every reload | cosmetic | 1.12-only |
| ICBM-01-31 | Content flags do not gate recipes | `recipes/parts/battery.json`, `recipes/parts/plate.steel.json` etc.; `ConfigItems` | Disabling the battery or a crafting family leaves recipes that reference missing items, which fail to load | cosmetic | logic |
| ICBM-01-32 | Unknown codex ID dereferences null | `lib/network/lambda/tile/PacketLambdaTile.java:68-72` | `decodeInto` does not null-check the codex (the entity packet does); a client-supplied bad ID throws and is logged | cosmetic | logic |
| ICBM-01-33 | Codex IDs depend on registration order | `lib/network/lambda/PacketCodexReg.java`; `EntityMissile.java:376-378` | IDs are assigned by call order, and the missile codex registers in a static initializer, so IDs shift with class loading and add-on order | gameplay | logic |
| ICBM-01-34 | Toggle packets carry the sender's state | `lib/network/lambda/PacketCodex.java:149-151` | The receiver stores the negation of the sent value rather than toggling its own; stale clients flip to the wrong state and a client may force any value | cosmetic | logic |
| ICBM-01-40 | Tag lookup key never matches the stored key (logic) | `api/data/meta/MetaTag.java:72,111-118`; `content/actions/ActionProvider.java:60-62` | Tags are stored in the map under the bare path but looked up under `namespace:path`. So `find` always returns null and `getOrCreate` never finds an existing tag; each call builds a new tag and adds a duplicate child to the parent. Two namespaces with the same path would also overwrite each other. `ActionProvider` load uses `find`, so every restored entry gets a null tag key (and `load` may also add null values) | gameplay |
| ICBM-01-41 | `MetaTag` hash/equals recurse through parent and children (logic, unverified) | `api/data/meta/MetaTag.java:39` (Lombok `@Data`); used as map key in `lib/actions/listners/ActionListenerLayer.java` (layer map) and `content/actions/ActionProvider.java` | `@Data` generates equals and hash over `parent` and `children`, and a parent lists its children while a child points at its parent, so hashing a tag with a parent or children should overflow the stack. Latent because ICBM adds no listeners and no provider entries, but any addon calling `addListener` would crash | crash |
| ICBM-01-42 | "Root cause" returns the grandparent, not the root (logic) | `api/actions/cause/IActionCause.java:25-31` | Returns the previous cause's previous cause. A chain of two gives null, a chain of four gives the second link; a single cause gives null. The TODO about infinite loops (cycles in the chain) is also unhandled | gameplay |
| ICBM-01-43 | OR condition evaluates only its first child (logic) | `content/actions/conditionals/ConditionOR.java:36-47` | With the default `short_circuit` true, the loop returns the first child's status unconditionally (the return sits outside the "is it ready" test), so OR(A, B) behaves as A. Empty OR is ready. Not used by shipped content yet | gameplay |
| ICBM-01-44 | Timer condition is ready only at count 0 (logic) | `lib/actions/conditionals/timer/TimerCondition.java:40-52` | `getCondition` returns ready when the count is at or below zero, and a "ticking" blocking status for every count above zero, **including when the target is reached**. So a timer is ready at the start and never again after its first tick, until `reset`. Also the cached status object is cleared only in `onTick`, so a reset leaves a stale cache (harmless today since the count 0 branch comes first). Not used by shipped content | gameplay |
| ICBM-01-45 | Target-distance condition is ready before it has measured (logic) | `content/actions/conditionals/ConditionTargetDistance.java:37,46-55,59-75` | The initial distance is `Double.MIN_VALUE` (the smallest positive number, about zero), not "unknown", so a non-inverted condition reports ready until the first `onTick` measurement. `onTick` also reads the provider without a null check (NPE if ticked before `init`), and the error status names `ConditionalImpact` as its source (copy-paste) | gameplay |
| ICBM-01-46 | Composite and distance conditions give no cause (logic) | `content/actions/conditionals/ConditionAnd.java:72`, `ConditionOR.java` (`getCause`), `ConditionTargetDistance.java:99`; `lib/actions/PotentialActionImp.java:94-98` | All three are marked as cause providers but return null ("TODO implement"), so the potential action never adds a condition cause (for example "distance" or "impact") to the chain; only the timer would. Cause tracking is shorter than the design claims | cosmetic |
| ICBM-01-47 | Spawn action re-randomizes yaw when pitch is not given (logic) | `content/actions/entity/ActionSpawnEntity.java:78-83` | The else-branch for a missing pitch assigns a random value to `rotationYaw` (copy of the yaw branch), overwriting the yaw that was just set, and leaves pitch at its default. A caller that sets yaw but not pitch loses its yaw | gameplay |
| ICBM-01-48 | Spawn data merge can override the action's position (logic, unverified) | `content/actions/entity/ActionSpawnEntity.java:55,85-92` | Position is set first, then the entity's whole NBT (which includes `Pos` and `Rotation`) is merged with the supplied data and read back. If the supplied data carries `Pos`, the entity ends up there instead of at the action's position, and rotation set earlier is reverted by `Rotation` if present | gameplay |
| ICBM-01-49 | World unload removes blasts while iterating the list (logic) | `lib/actions/WorkTickingActionHandler.java:40-46`; `content/blast/Blast.java:649-656` | `worldUnload` streams over `activeBlasts` and calls `clearBlast` on each, and `clearBlast` removes the blast from that same list. A modification during iteration should throw a concurrent-modification error after the loop, aborting the event handler (and leaving other handlers for this unload at risk). The list is also touched by blast threads without locking. Also `removeNear` treats range 0 as "remove nothing" | crash |
| ICBM-01-50 | Conditions lose their provider on reload (logic, unverified) | `lib/actions/PotentialActionImp.java:37-45,126`; `content/missile/entity/explosive/EntityMissileActionable.java:143-146`; `content/cluster/missile/CapabilityClusterMissileStack.java:42-49` | Condition init is called only by `withCondition` and `withProvider`. NBT load sets the condition through a private setter that does not init it. An actionable (cluster) missile saved in flight reloads with an impact and distance condition that have no provider, so they report a blocking "missing field" status forever and the warhead cannot trigger via the condition path | gameplay |
| ICBM-01-51 | Legacy `ActionStatus` has no type tags (logic) | `lib/actions/status/ActionStatus.java:16,54-57` | It overrides the two deprecated booleans but not the tag list, while new code (`PotentialActionImp`, the AND/OR conditions, `EntityMissileActionable`, the EMP tower) tests tags. Launcher statuses therefore look neither blocking nor error to any tag check | gameplay |
| ICBM-01-52 | Two launcher statuses are never registered (logic) | `content/blocks/launcher/status/LauncherStatus.java:23-24,45-58` | `ERROR_EMPTY_GROUP` and `ERROR_NO_NETWORK` are defined but missing from the registration list, so saving them keeps the id and loading returns null. Belongs to `04-launch-control`; found while reading the registry | gameplay |
| ICBM-01-53 | Status and radio-reply strings missing from the lang file (logic) | `lib/actions/status/ImmutableStatus.java:23`; `content/blocks/emptower/RadioEmpTower.java:18`; `en_us.lang` | Keys `info.icbmclassic:action.*`, `...:missing.field`, the timer status key, and `tile.emptower.radio.success` have no entries, so players would see raw keys (the registry-derived status keys have a different prefix scheme and none exist). Only launcher statuses and the radio messages are translated | cosmetic |
| ICBM-01-54 | Caller's null fields overwrite configured action data (logic, latent) | `lib/actions/ActionDataGeneric.java:43-44`; `api/actions/data/IActionFieldReceiver.java:14-20` | `create` applies the data's stored values and then every field the provider *declares*, with whatever it returns, including null. A provider that declares `entity.regName` but has nothing to supply erases a value configured on the data | gameplay |
| ICBM-01-55 | Listener duplicate check is per layer (logic) | `lib/actions/listners/ActionListenerLayer.java:42,49` | The same listener registered under two tags is accepted. For an action that carries both tags the listener runs twice. Sibling layers also iterate from a hash map in unspecified order | cosmetic |
| ICBM-01-56 | Radio "overlap" test is containment on any one axis (logic) | `lib/radio/RadioMap.java:93-103` | `doesOverlap` is true if the other box lies *inside* this one on any single axis. Partial overlap on all axes is missed and containment on one axis is enough to count. Unreachable in ICBM because every receiver is full-map | gameplay |
| ICBM-01-57 | Range-change update check is inverted and its map is never written (logic) | `lib/radio/RadioMap.java:37,159-168` | `update` proceeds when the cached range is absent, null, or *equal* to the current one; the cache `receive_to_range` is never filled, so it always proceeds. Old chunk entries are not removed when a range changes | gameplay |
| ICBM-01-58 | Sender receiver-cache is never built (logic) | `lib/radio/RadioMap.java:229-232,~291-301`; `updateCache` has no caller outside `lib/radio` | The fast path for cached senders is dead, and the cache would be consulted *instead of* the full-map list if it were ever built, which would skip any receiver registered afterward. Messages always use the slow scan | gameplay |
| ICBM-01-59 | Channel is lost when saved before first use (logic) | `lib/radio/imp/Radio.java:26-44` | The channel is generated on first read and left out of NBT while null. On load the missing key reads as the empty string, which is a real value, so the radio stays on the empty channel and a radar with the empty channel never broadcasts. A radar normally reads its channel each powered tick so it usually survives; receivers read it only when a message arrives | gameplay |
| ICBM-01-60 | Intercept calculation is a stub (logic) | `lib/radio/messages/IncomingMissileMessage.java:19-43` | The loop compares the *absolute* magnitude of a predicted position to `velocity * time` (ignoring the shooter coordinates passed in) and every branch returns the missile's current position. Launchers aim at where the missile is now, not where it will be | gameplay |
| ICBM-01-61 | Screen-fired SAMs never lock on (logic) | `content/blocks/launcher/screen/RadioScreen.java:48-51` | The trigger callback is commented as "TODO implement" in the screen path, so the SAM target lock in `IncomingMissileMessage.onTriggerCallback` only runs for the cruise launcher (whose firing package wires it) | gameplay |
| ICBM-01-62 | Any device that knows a channel controls the base (logic) | `lib/radio/imp/RadioTile.java:40-48`; `prefab/item/ItemRadio.java:25-38` | No owner, team, range or authentication. Channels are 4 alphanumerics (about 14.7 million values) generated with non-secure random; the radio item copies a channel from any radio-capable block by right-click with no permission check; a message reaches the whole dimension. Design weakness rather than coding error | gameplay |
| ICBM-01-63 | Radar re-trigger refires and reset-spams the screen (logic) | `content/blocks/launcher/screen/TileLauncherScreen.java:333`; `content/blocks/radarstation/TileRadarStation.java:163-172` | The screen compares the new target to the old by reference (`!=`), unlike the cruise launcher which uses value equality, so every 40-tick radar message posts `LauncherSetTargetEvent` and marks the tile dirty even for the same target. Combined with the trigger, launchers refire each interval while any threat exists | gameplay |
| ICBM-01-64 | Radar-gun event changes are ignored (logic) | `content/items/ItemRadarGun.java:183-192` | After `RadarGunTraceEvent` the code stores the original input position, not `event.pos`, so a listener that edits the position has no effect (only the cancel works). Also only a null check on `event.pos` follows the post | gameplay |
| ICBM-01-65 | Tracker type-level listener drops its condition (logic) | `lib/tracker/EventTrackerType.java:176-179` | `Builder.listen(name, conditional, listener)` ignores the condition and uses the unconditional constructor. The three "debug only" packet loggers therefore run for every event (log level still filters output, but the string formatting always runs) | cosmetic |
| ICBM-01-66 | Failing tracker listener is logged without its exception (logic) | `lib/tracker/ListenerEntry.java:30-36` | The catch block logs a fixed message with the listener name and event type but drops the exception and stack trace, and only catches `Exception` (not errors) | cosmetic |
| ICBM-01-67 | Read-only check passes mutable payloads (logic) | `lib/tracker/EventTrackerData.java:57-62,100-130` | Interfaces and arrays have no declared fields so they are always "valid"; `Date` is whitelisted though mutable; a class is checked by field declaration only, not by what a `final` field holds beyond its type. `Object` is whitelisted, so anything whose static type is `Object` passes at the class level | cosmetic |
| ICBM-01-68 | Tracker caches and listener lists are not thread safe (logic) | `lib/tracker/EventTrackerData.java:21-23`; `lib/tracker/EventTracker.java` (listener list) | The class docs say events may arrive from several threads (blast threads), but the valid/invalid class caches and the scan set are plain hash sets, and the listener list is an array list. Concurrent first-use can corrupt a cache | cosmetic |
| ICBM-01-69 | Four packet event types share one name; type ids are never set (logic) | `lib/network/PacketEvents.java:15,40`; `lib/network/lambda/tile/PacketTileEvents.java:16,44`; `lib/tracker/EventTrackerType.java` (package-private id setter, no callers) | All four use the name `packet.tile.error.handling`, so any tool keying by type name cannot tell them apart, and `getId()` is always 0 | cosmetic |
| ICBM-01-70 | Entry lookup fails on an unknown column (logic) | `lib/tracker/EventTrackerEntry.java:20,24` | `get` and `getString` call the type's field lookup and then the accessor without a null check; asking for a key the type does not define throws a null-pointer error inside a listener (which `ListenerEntry` then swallows) | cosmetic |
| ICBM-01-71 | Most blasts bypass the action listeners (logic) | `command/sub/blast/CommandBlastSpread.java:91`, `CommandBlastTrigger.java:152`; `content/blast/threaded/BlastNuclear.java:202,210`; `content/blocks/explosive/TileEntityExplosive.java:94`; `lib/capability/ex/CapabilityExplosiveEntity.java:63`; `content/blast/redmatter/logic/RedmatterLogic.java:476`; `api/actions/listener/IActionListenerHandler.java` | The listener contract says all created actions must pass the handler so protection mods can intercept, but these callers run `create(...).doAction()` directly. Only potential actions (EMP tower, actionable and cluster missiles) are interceptable; real explosion protection still depends on Forge's explosion-start event and `BlastBuildEvent` | gameplay |
| ICBM-01-72 | Radar's radio disable flag does nothing (logic) | `content/blocks/radarstation/TileRadarStation.java:163-172,398,415`; `content/blocks/radarstation/RadioRadar.java` | The radar saves `disabled` and a packet toggles it, but neither the tick nor the sender checks it, and the radar GUI shows no disable button. Receivers honor the flag; the radar does not | cosmetic |

**Part a: lifecycle, config, energy, commands, integrations:**

"Logic" means we could inherit it in a rebuild. "1.12-only" means it does not matter to a 1.7.10 rebuild.

**Part b: actions framework, radio bus, event tracker, public API:**

"Logic" means we could inherit it in a rebuild. "1.12-only" means it does not matter to a 1.7.10 rebuild. Line numbers refer to the source file at the pinned commit.
Minor, not given IDs: the `IEMPReceiver` javadoc points at a `registerBlockEmpHandler` method that no longer exists; `ActionStatusTypes` javadoc and `ICondition` javadoc refer to a `GREEN` "condition" with wording that mixes up statuses and conditions; `MetaTag.isAssignable`'s javadoc example contradicts its own rule; `ActionField` keeps its registry in a plain static map (not thread safe); `BlastEvent` keeps a generic parameter it never uses; `MissileTrackerHandler.simulateMissile` returns false on both the canceled and the simulated path (a `03-missiles` item); `IncomingMissileMessage` and `ConditionTargetDistance` allocate a fresh status object on every call; `RadioMap.popMessage` logs its error as "RadarMap".
