# Config on GTNHLib

- **Techguns baseline:** [config.md](../../techguns-spec/01-core/config.md) (63 keys in `config/Techguns.cfg`), [CORE-02](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-07](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-12](../../techguns-spec/01-core/index.md#suspected-bugs)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md), [0003 energy model](../../design/0003-energy-model.md), [0005 world presence](../../design/0005-world-presence.md)
- **Pack mods:** GTNHLib (`gtnhlib`, 0.11.52)
- **Roadmap:** M0 (skeleton); each later milestone adds its own keys

Every option and module flag is documented in [docs/config.md](../../config.md).

## What changes from Techguns

Techguns reads a Forge `Configuration` by hand in `TGConfig`. GregGuns declares each config as a plain Java class annotated with GTNHLib's `@Config`. GTNHLib then loads the file, writes comments and ranges, builds the in-game config GUI, and can sync chosen fields from server to client.

| Techguns | GregGuns | Why |
|---|---|---|
| `TGConfig` with hand-written `config.get(...)` calls | `@Config(modid = "gregguns")` classes with `public static` fields | Fewer lines, GUI for free, ranges enforced |
| One file `Techguns.cfg` with 8 categories | `config/gregguns.cfg` (main) plus one file per big area (for example `gregguns-weapons.cfg`, mirroring `Techguns_WeaponStats.cfg`) | Matches how GTNH mods lay config out |
| Server-only values read on the client by accident (for example damage factors) | Fields that the client needs are marked `@Config.Sync` | The server value wins on a multiplayer client, and is restored on disconnect |
| Edits need a restart | Fields that are safe to change live are marked `@Config.Reloadable` | Pack makers can tune balance without a restart |

### What happens to each Techguns key

| Techguns category and keys | GregGuns | Reason |
|---|---|---|
| `general`: `doOreGen*`, `addCustomOreCluster*` | **Removed** | No ore generation of any kind ([0005](../../design/0005-world-presence.md)) |
| `general`: `addSteelCrafting`, `addBronzeCrafting`, `add*Ingot`, `add*Nuggets` (including `addIronNuggets`, [CORE-01](../../techguns-spec/01-core/index.md#suspected-bugs)) | **Removed** | Metals come from GT materials ([0002](../../design/0002-machine-deconfliction.md)) |
| `general`: `SpawnStructures`, `StructureSpawnWeight*` | Kept | Structures stay, config-gated ([0005](../../design/0005-world-presence.md)) |
| `general`: `disableAutofeeder`, `ForceSafeModeForNonOps`, `RestrictedLoottables` | Kept | Gameplay switches |
| `general`: `MachinesNeedNoPower` | Kept, renamed `machinesNeedNoEU` | Applies to the kept GT MetaTileEntities |
| `general`: `ExplosiveChargeMaxHardness`, `ExplosiveChargeAdvancedMaxHardness` | Kept | Charges stay |
| `ID Conflicts`: `DataWatcherID_FaceSlot`, `DataWatcherID_BackSlot`, `TechgunsGUI_TabID` | **Removed** | No DataWatcher slots and no config-driven GUI ID; see [networking-gtnhlib.md](networking-gtnhlib.md) |
| `NPC Spawn` (weights, distances) | Kept; weights halved | [0005](../../design/0005-world-presence.md) |
| `Ore Drills` (all 10 keys, including the RF multipliers) | **Removed** | Ore Drill is dropped ([0002](../../design/0002-machine-deconfliction.md)) |
| `Spawn Controll`: `spawnOilHoles`, `oilHoleFrequency` | **Removed** | No oil holes ([0005](../../design/0005-world-presence.md)); the pack's `GenerateOil` owns oil, see below |
| `Damage Factors` (5 keys) | Kept; `@Config.Sync` | Used by combat on both sides ([0004](../../design/0004-damage-model.md)) |
| `Debug`: `enableDebugKeybinds` | Kept | Developer aid |
| `Clientside`: death-effect and FOV keys | Kept; `FixedSprintFovMultiplier` is wired up | Fixes [CORE-12](../../techguns-spec/01-core/index.md#suspected-bugs) |
| `Clientside`: `EnableVersionChecker`, `EnableVersionCheckerBeta` | **Removed** | The GTNH pack manages updates; also removes [CORE-02](../../techguns-spec/01-core/index.md#suspected-bugs) |
| New: per-item energy capacities | Added | [0003](../../design/0003-energy-model.md) ("every value is a config option") |

No RF key survives. The Techguns spec has RF only in `Ore Drills` and in the machine power values, and [0003](../../design/0003-energy-model.md) rules RF out.

## Integration points

### GTNHLib: annotated config
- **Pack config:** none. GTNHLib creates `config/gregguns.cfg` on first run. The pack ships `config/gtnhlib-number-formatting.cfg` only, which does not affect us.
- **Source:**
  - [`Config`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/config/Config.java#L14): the `@Config` annotation. `modid` is required; `category`, `configSubDirectory` and `filename` are optional.
  - [`@Config.Comment`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/config/Config.java#L88) and [`@Config.RangeInt`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/config/Config.java#L106): comments and bounds.
  - [`@Config.Sync`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/config/Config.java#L251): server to client sync, restored on disconnect.
  - [`@Config.RequiresMod`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/config/Config.java#L290): hides an option in the GUI when a mod is missing.
  - [`@Config.Reloadable`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/config/Config.java#L323) with [`ConfigurationManager.reloadConfig`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/config/ConfigurationManager.java#L121).
  - [`ConfigurationManager.registerConfig`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/config/ConfigurationManager.java#L66): "should be done before or during preInit".
- **GregGuns does:** registers every config class in `preInit` with `ConfigurationManager.registerConfig`. Config classes hold only fields (no logic), so unit tests can set them directly.
- **Pack-side change:** none.

### dreamcraft: oil generation
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GTNewHorizons/dreamcraft.cfg`, key `GenerateOil` = `true`. The pack owns oil spheres and fountains.
- **GregGuns does:** nothing. We drop `spawnOilHoles`, so there is no overlap to resolve.
- **Pack-side change:** none.

## Requirements

- **GGS-01-01** All GregGuns config is declared with GTNHLib `@Config` classes registered in `preInit`. No hand-written `Configuration` reads.
- **GGS-01-02** No config key exists for ore generation, ingots, nuggets, ore drills, oil holes, DataWatcher IDs, GUI tab IDs or the version checker.
- **GGS-01-03** Every field the client reads (damage factors, and toggles that gate server behavior) is `@Config.Sync`, so a server value overrides a client value while connected.
- **GGS-01-04** Every numeric field carries a `@Config.Comment` with its default, and a `@Config.Range*` where Techguns had a range.
- **GGS-01-05** `FixedSprintFovMultiplier` changes the sprint FOV (fixes CORE-12).
- **GGS-01-06** Config classes have no dependency on Minecraft classes, so they load in a plain unit test.

## Tests

- GGS-01-01, GGS-01-06: a unit test loads each config class with no Forge on the classpath and reads the defaults.
- GGS-01-02: a unit test reflects over the config classes and fails on a banned field name.
- GGS-01-03: manual check in a two-player `runServer` plus `runClient` session. Change a synced value on the server, join, and read it on the client.
- GGS-01-04: a unit test checks that every field has a comment.
- GGS-01-05: manual check with `runClient`.

## Open questions

- Should the main file be split by area from the start (`gregguns-weapons.cfg`, `gregguns-npcs.cfg`), or stay one file until it grows? A short design record would settle it if the pack team has a preference.
