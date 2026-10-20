# Lifecycle and Registration

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Classes: `ICBMClassic`, `CommonProxy`, `ICBMConstants`, `client/ICBMCreativeTab`, `content/items/behavior/*DispenseBehavior`, `content/reg/ItemReg`, `content/reg/BlockReg`, `client/ClientProxy`.

The Forge event names, `@ObjectHolder`, `@SidedProxy`, `RegistryEvent` and `@Config` are *1.12-only mechanisms*. This sheet records what the mod does and in what order, so a 1.7.10 / GTNH rebuild can reproduce the order of effects with `FMLPreInitializationEvent`, `GameRegistry` and `Configuration`.

## 1. Identity

| Item | Value |
|---|---|
| Mod ID / domain | `icbmclassic` (`ICBMConstants.DOMAIN`); `PREFIX` = `icbmclassic:` |
| Display name | "ICBM-Classic" |
| Data fixer version | 5 (`ICBMClassic.DATA_FIXER_VERSION`); see [integrations](integrations.md#4-datafixers) |
| Dev flag | JVM property `development=true` sets `runningAsDev` (read nowhere else in the files covered) |
| Tick constants | `TICKS_SEC` 20, `TICKS_MIN` 1200, `TICKS_MIN_HALF` 600 |
| Map height | `MAP_HEIGHT` 255 (a constant; used by flight and radar code in other areas) |
| Static singletons held by the mod class | `packetHandler` (netty channel named `icbmclassic`), `chemicalPotion` ("Chemical", id 0, not contagious), `contagiousPotion` ("Contagious", id 1, contagious), `CREATIVE_TAB`, `MAIN_TRACKER` (event tracker used for packet error events), `modFixs` |
| Test hook | `isJUnitTest()` scans the stack for JUnit frames; used only to skip network sends in tests |

## 2. Load order

Phases in the order they run. "Registry events" are *1.12-only mechanism*; the behavior they produce is "this object exists and has this name".

| # | Phase | What happens |
|---|---|---|
| 0 | Registry events (before `preInit` on 1.12) | Blocks (`BlockReg.registerBlocks`): glass pressure plate, glass button, spikes, concrete, reinforced glass, explosive block, EMP tower, radar station, launch frame (support), launch connector, launcher base, launch screen, cruise launcher, radioactive block; then tile entities (`explosive`, EMP tower, `emptower_fake`, radar, `launcherframe`, `launcher_connector`, launcher base, launcher screen, `multiblock`, cruise launcher). Items (`ItemReg.registerItems`): see §4. Entities, potions and sounds register in other areas. Recipes event: see §5. A missing-mapping handler remaps the old item name `icbmclassic:missile` to the explosive missile item (name changed in v4.2.0) |
| 1 | `preInit` | (a) `proxy.preInit()` runs `ModInteraction.preInit()`, which runs the Mekanism proxy (adds EMP block-swap defaults). (b) Register the Forge Energy wrapper with `EnergySystem` ([energy-model](energy-model.md)). (c) Register capabilities: EMP, missile, explosive, blast, blast velocity, missile holder, missile stack, projectile stack, missile launcher, radio, `CapSpaceChicken`, GPS data. (d) Create the mod fixer set and register 7 fixers. (e) Put `RadarRegistry` and `RadioRegistry` on the Forge event bus. (f) Register the GUI handler (the proxy). (g) Build, fire an event for, and lock the registries in the order given in §3. |
| 2 | `init` | `proxy.init()` (Mekanism teleporter hook; client adds the chicken-helmet render layer). `packetHandler.init()` opens the netty channel. `CREATIVE_TAB.init()`. `ProjectileBlockInteraction.register()` (vanilla block hit reactions; owned by 03). `setup()` on the handler classes in this order: cluster missile, cargo holder, held-item missile, radioactive, protective armor, EMP (each loads its config lists into hash-friendly form). Projectile data registry: register vanilla defaults, fire `ProjectileDataRegistryEvent`, lock. Dispenser behaviors (§6). `FlyingBlock.loadFromConfig()`. |
| 3 | `postInit` | `proxy.postInit()`, which is empty in the covered code (Mekanism `postInit` is a no-op). |
| 4 | `serverStarting` | Build the `icbm` command tree and register `/icbm` ([commands](commands.md)). Create `WorkerThreadManager` with `thread_count` threads and start them. |
| 5 | `serverStopping` | Kill the worker threads. |

Config reload (`ConfigMain.onConfigChangedEvent`, any config change for the mod): re-sync all config classes, then reload the same seven derived lists as in `init` step order (flying block, cluster, cargo, held item, radioactive, protective armor, EMP). The thread count is **not** re-applied (needs a server restart).

## 3. Registry build order inside `preInit`

Each of the registries below is created fresh, filled with ICBM defaults, offered to other mods through a Forge event, then locked. Late registration after the lock is refused.

| Registry (static on `ICBMClassicAPI`) | Defaults registered | Event | Notes |
|---|---|---|---|
| Missile target data | `BasicTargetData`, `BallisticTargetingData`, `SAMTargetData` (name reserved, builder returns null: "can't be restored from save") | `MissileTargetRegistryEvent` | `BuildableObjectRegistry` name→factory |
| Missile flight logic | direct, ballistic-old, arc, warmup, move-by-facing, move-by-vec3, move-for-ticks, dead, follow-target (9) | `MissileFlightLogicRegistryEvent` | |
| Action system | `ActionSystem.setup()` | (part b, [actions framework](actions-framework.md)) | |
| Explosive customization | ender blast customization | `ExplosiveCustomizationRegistryEvent` | |
| Explosive registry | see below | `ExplosiveContentRegistryEvent`, then `ExplosiveRegistryEvent` | Persisted to disk |
| Projectile data | bomblet, parachute, balloon in `preInit`; vanilla defaults in `init` | `ProjectileDataRegistryEvent` (fired in `init`) | locked at end of `init` because it needs content registered first |

Explosive registry sequence:

1. Create the registry and the four content registries (block, grenade, minecart, missile).
2. If a config folder exists, load `config/icbmclassic/explosive_reg.json`. This file reserves numeric IDs so saved IDs survive across versions.
3. Register the four content registries, then fire `ExplosiveContentRegistryEvent` and lock the set of content types.
4. `ExplosiveInit.init()` registers all internal explosives with fixed numeric IDs (condensed = 0 ... hypersonic = 21 ... see 02) "to reserve slots for backwards compatibility".
5. Fire `ExplosiveRegistryEvent` (other mods add explosives), lock new explosives, lock everything.
6. Save the registry back to `explosive_reg.json`.

For GregGuns the on-disk ID reservation matters only if saves must survive; a rebuild with a fresh ID table can drop it. See [02-explosives](../02-explosives/) for the explosive list.

## 4. Items and the content-enable logic

All item registrations in `ItemReg.registerItems`, in order. Items marked **(flag)** only exist when the named `item` config key allows it ([config](config.md)).

| Group | Items (registry names) |
|---|---|
| Plain | `grenade`, `bombcart`, `poisonPowder`, `sulfurDust`, `saltpeter`, `saltpeter_ball`, `antidote`, `empty_bomblet`, `explosive_bomblet`, `parachute`, `balloon` |
| Tools | defuser, radar gun, remote detonator, laser detonator, rocket launcher (`rocketLauncher`) and ballistic launcher (`ballisticLauncher`, the same class with the up/down-fire flag set) |
| Missiles | `explosive_missile`, `surface_to_air_missile`, `cluster_missile`, `held_item_missile` |
| Block items | glass pressure plate, glass button, spikes (subtypes), concrete (subtypes), reinforced glass, explosive block, EMP tower (2 metas), radar station, launch frame, launcher base, launch connector, launch screen, cruise launcher, radioactive (subtypes) |
| Crafting items **(flag `enable_crafting_items` AND the specific flag)** | `ingot` (steel, copper) and `clump` (steel) with `enable_ingots`; `plate` (steel, iron) with `enable_plates`; `circuit` (basic, advanced, elite) with `enable_circuits`; `wire` (copper, gold) with `enable_wires` |
| Battery **(flag `enable_battery`)** | `battery` |

Registered unconditionally at the end: ore dictionary entries `dustSulfur` and `dustSaltpeter` (a second registration of what the plain group already registered; see ICBM-01-19).

How the flags work:

- `enable_crafting_items` is the master switch for the four crafting-item families. Each family also has its own switch. Turning a family off simply does not register the item; the static `ItemReg` field stays null.
- Everything that touches those fields checks the same flags: client model registration (`newItemModel` is null-safe), ore-name registration, smelting recipes, and the loot code.
- **Recipes are not gated.** The recipe JSON files that produce or consume the crafting items and the battery carry no config condition. A disabled item means those recipes fail to load (logged errors) and machines whose recipes use the ore names (`plateSteel`, `circuitElite`, `wireGold`) only craft if another mod supplies those ores. The config comment says exactly that: "only disable if other mods provide the resources or custom recipes are implemented".
- Ore-dictionary-conditional recipes exist for the gunpowder and for the Mekanism explosive swaps: a custom condition factory `icbmclassic:ore_value` (declared in `recipes/_factories.json`) checks whether an ore name has any non-empty stack. See [integrations](integrations.md#3-mekanism).

### Recipes registered in code

Registered on the recipe event (dynamic, NBT-driven recipes; their behavior is in 03/06):

| Registry name | Class | Does |
|---|---|---|
| `balloon_cargo`, `parachute_cargo` | `RecipeCargoData` | put an allowed item into a balloon or parachute |
| `balloon_cargo_decraft`, `parachute_cargo_decraft` | `RecipeCargoDataDecraft` | take the cargo back out |
| `cluster_missile` | `RecipeCluster` | add items to a cluster missile |
| `held_item_missile`, `held_item_missile_decraft` | `RecipeHeldItemMissile(+Decraft)` | load or unload the held item missile |

Smelting registered in code: steel clump -> steel ingot (0.1 xp) when ingots are on; iron plate -> vanilla iron ingot (0 xp; "fix for removing recipe of plate") when plates are on; saltpeter ball -> saltpeter dust (0.1 xp) always.

Ore-name registration (highest-priority subscriber on the same recipe event, so the names exist before JSON recipes read them): ingot, clump, plate, circuit, wire each call their own `registerOreNames()` when enabled.

### Loot injection

`LootTableLoadEvent` handler. All entries are added to the pool named `main` of the target table.

| Table | Condition | Entries (name suffix, stack, weight, quality) |
|---|---|---|
| `chests/simple_dungeon`, `chests/abandoned_mineshaft` | `enable_loot_drops` and `enable_crafting_items`, per family flag | copper ingot x10 (15, 5); steel ingot x10 (20, 3); steel plate x5 (30, 3); copper wire x20 (15, 5); gold wire x15 (30, 3); basic circuit x15 (15, 5); advanced circuit x11 (30, 3); elite circuit x8 (30, 3) |
| `entities/creeper`, `entities/blaze` | `enable_sulfur_drops` | sulfur dust x10 (2, 0) |

`LootEntryItemStack` picks the stack count as a uniform random integer in `[0, N-1]` using its own static `Random`, so "x10" yields 0 to 9 and can yield an empty stack (ICBM-01-27). Counts at or above the max stack size are split into several stacks.

## 5. Creative tab

`ICBMCreativeTab` (name `icbmclassic`, icon = the explosive missile item). `init()` runs in `init` because the item registry must be frozen. It builds an ordered item list:

1. Launch base, launch screen, launch support (frame), launch connector, cruise launcher, EMP tower, radar station.
2. Concrete, reinforced glass, spikes.
3. Rocket launcher, ballistic launcher, radar gun, remote detonator, laser detonator, defuser.
4. Explosive missile, SAM, held-item missile, cluster missile.
5. Grenade, explosive block, bomb cart.
6. Then every other registered item whose creative tabs include this tab (battery, antidote, powders, parachute, crafting items, and so on), in registry order.

Within each item, the sub-item stacks are sorted: explosives by tier ordinal, then by registry ID (`CONDENSED` is used when an explosive has no data). Non-explosive stacks compare as equal and keep their order.

## 6. Dispenser behaviors

Registered in `init` only if the item exists.

| Item | Behavior class | What a dispenser does |
|---|---|---|
| Grenade | `GrenadeDispenseBehavior` | Server side: spawn a grenade entity at the dispenser's position (block center, not offset in front), set its stack, throw it with horizontal direction = the dispenser facing's X and Z, a fixed small upward component (0.1), speed 0.5, inaccuracy 1.0. Returns the stack minus one item. Facing up or down gives zero horizontal motion (ICBM-01-18) |
| Bomb cart | `BombCartDispenseBehavior` | Same rail logic as the vanilla minecart dispenser: place a bomb cart entity 1.125 blocks in front; height offset 0.6 if the target is an ascending rail, 0.1 for a flat rail; if the target is air with a rail below, -0.4 (ascending rail below and not facing down) or -0.9; otherwise fall back to the default item dispense. Copies a custom display name to the cart. Shrinks the stack by 1 |

Nothing is registered for missiles, the rocket launcher or any machine.

## 7. GUI handler and proxies

`CommonProxy` is also the Forge GUI handler. GUI IDs: `GUI_ENTITY` = 10001, `GUI_ITEM` = 10002. Dispatch:

| Call | Resolves target as |
|---|---|
| ID = `GUI_ITEM` | the player inventory slot given in `x`; the real GUI ID is passed in `y` |
| ID = `GUI_ENTITY` | entity with network ID `x`, real GUI ID in `y` |
| any other ID | the tile entity at `(x, y, z)` |

The target must implement `IGuiTile` (6 methods of `getServerGuiElement`/`getClientGuiElement` per target kind in the proxy). On the client the item branch is wired to the wrong overload (it looks up an entity and calls the server path), so item GUIs cannot open on the client (ICBM-01-09). Which items use item GUIs is in 06.

Side hooks on the proxy (client overrides in `ClientProxy`):

| Hook | Server/common | Client |
|---|---|---|
| `spawnSmoke` | no-op | spawn a smoke particle with color, scale, age |
| `spawnAirParticle` | send `PacketSpawnAirParticle` to everyone within 256 blocks | spawn the particle |
| `spawnExplosionParticles` | send **one `PacketSpawnBlockExplosion` per destroyed block** to everyone within 256 blocks (ICBM-01-26) | spawn a vanilla explosion particle and a smoke particle near the block, moving away from the blast center; speed scales with `0.5 / (distance/blastScale + 0.1)` times a random `0.3..1.3` |
| `spawnMissileSmoke` | no-op | gated by `missile_engine_smoke`: 10 air particles behind the missile (1.2 m back along its pitch/yaw), velocity = -50% of the missile's motion, lifetime 100, tint from the flight logic's engine color |
| `spawnPadSmoke` | no-op | gated by `missile_launch_smoke`: 10 white launcher-smoke particles below the missile, cone-shaped, age 180 |
| `isShiftHeld` | n/a | either shift key |

## 8. Related areas

| Area | What it owns here |
|---|---|
| [actions-framework](actions-framework.md) and the radio bus (part b) | `ActionSystem.setup()`, `RadioRegistry`, the explosive/other registries' public API |
| `02-explosives` | explosive IDs, `ExplosiveInit`, `FlyingBlock` spawn logic, gas and radiation handlers |
| `03-missiles` | flight logic and target data classes, cargo, cluster and held-item handlers, `ProjectileBlockInteraction` |
| `06-blocks-items` | what each item and block does, recipes |
