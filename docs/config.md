# GregGuns configuration

Reference for every option in `config/gregguns.cfg`. GTNHLib creates the file on first run, with a comment above each key, and the in-game config GUI lists the same options. The classes behind it are in `com.gregguns.core.config`; the design is in [gregguns-spec/01-core/config-gtnhlib.md](gregguns-spec/01-core/config-gtnhlib.md).

Keep this file in step with the code: a PR that adds or changes an option updates its row here.

## Module flags (`modules`)

Each roadmap content area is a module. A module's flag decides whether its code runs at all: when it is `false`, the module registers no items, blocks, entities, recipes or event handlers, so nothing is left behind to crash or to orphan a recipe.

**All flags default to `false` for now.** The roadmap says a flag defaults to on once its milestone's exit gate is met, and no content milestone is met yet. The PR that completes a milestone flips that milestone's default, in `ModulesConfig` and in the table below.

A module also needs the modules listed in "Needs". If a flag is on but a needed module is off, the module is skipped and a warning is logged.

| Key | Type | Default | Range | Synced | Needs | Milestone | What it enables |
|---|---|---|---|---|---|---|---|
| `modules.guns` | boolean | `false` | n/a | no | none | M1, M2 | Damage system, guns, projectiles, ammo |
| `modules.armor` | boolean | `false` | n/a | no | `guns` | M3 | Armour, set bonuses, special slots |
| `modules.machines` | boolean | `false` | n/a | no | `guns` | M4 | Production chain and GregTech machines |
| `modules.energyWeapons` | boolean | `false` | n/a | no | `guns`, `machines` | M5 | Energy and exotic weapons |
| `modules.utility` | boolean | `false` | n/a | no | `machines` | M6 | Utility and defence machines |
| `modules.npcs` | boolean | `false` | n/a | no | `guns` | M7 | NPCs and spawning |
| `modules.worldgen` | boolean | `false` | n/a | no | `npcs` | M8 | Structures and worldgen loot |

All module flags need a game restart. They are not synced from server to client (GGS-01-03): content is registered once at startup, so a value that arrived after joining could not add or remove it. Start the client and the server with the same flags.

The "Needs" column is the module order in `ModuleId`, taken from the roadmap's entry gates.

## Debug (`debug`)

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `debug.enableDebugKeybinds` | boolean | `false` | n/a | no | Registers the numpad debug keybinds. Nothing reads it until the M1 keybinds exist. Kept from Techguns' `enableDebugKeybinds`. |

## Client (`client`)

Display options for the local player only. Not synced.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `client.showAmmoHud` | boolean | `true` | n/a | no | Draws the ammo HUD (rounds in the gun, magazines carried, reload progress) while a gun is held. |
| `client.hudOffsetX` | int | `6` | 0 to 2000 | no | Pixels from the right screen edge to the right edge of the ammo HUD text. |
| `client.hudOffsetY` | int | `40` | 0 to 2000 | no | Pixels from the bottom screen edge to the bottom of the ammo HUD. The default clears the InGame-Info-XML potion lines in the same corner; M1 still owes a screenshot check with several potions active. |

## Rules for new options

- Declare it in a `@Config` class listed in `GregGunsConfig.CLASSES`; never read a `Configuration` by hand (GGS-01-01).
- Every field has a `@Config.Comment` that includes `Default: ...`, and every numeric field has a `@Config.Range*` (GGS-01-04). Unit tests enforce both.
- Fields the client reads, or that gate server behaviour at runtime, are `@Config.Sync` (GGS-01-03). Add them to `EXPECTED_SYNCED` in `ConfigRulesTest` and mark "Synced: yes" here.
- Do not add keys for ore generation, ingots, nuggets, ore drills, oil holes, DataWatcher IDs, GUI tab IDs or a version checker (GGS-01-02). A test rejects those names.
- Config classes hold fields only and import no Minecraft types (GGS-01-06).
- Document the option in this file.
