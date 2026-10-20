# Config

Source root: `src/main/java/icbm/classic/config/` at `9c1d985a`. Covers the **top-level** config classes: `ConfigMain`, `ConfigBattery`, `ConfigClient`, `ConfigDebug`, `ConfigFlyingBlocks`, `ConfigItems`, `ConfigJEI`, `ConfigThread`.

The blast, missile, machine and block configs live in subfolders and are specified with their owners; they are not repeated here:

| Config group | Where specified |
|---|---|
| `config/blast/*` (fuse times, thermobaric, per-blast-type) | [02-explosives](../02-explosives/) |
| `config/missile/*` (missile, SAM, cluster, bomblet, cargo, held item) | [03-missiles](../03-missiles/) |
| `config/machines/ConfigLauncher` | [04-launch-control](../04-launch-control/) |
| `config/machines/ConfigRadar`, `ConfigEmpTower` | [05-detection-defense](../05-detection-defense/emp-tower.md) |
| `config/blocks/*` (spikes, radioactive) | [06-blocks-items](../06-blocks-items/) and [02-explosives](../02-explosives/) |

## 1. Mechanism (*1.12-only*)

Each class is a Forge 1.12 `@Config` class. The annotation `name` becomes the file `config/<name>.cfg`, so these files land in `config/icbmclassic/`: `main.cfg`, `battery.cfg`, `client.cfg`, `debug.cfg`, `flying_blocks.cfg`, `item.cfg`, `mods/jei.cfg`, `thread.cfg`. Static fields are the values. A nested static class becomes a category (`protective_armor`, `block_ban_allow`). Ranges are enforced by the Forge config system on load (out-of-range values clamp or reset). A key marked "restart" has `RequiresMcRestart`. GregGuns rebuilds this on the 1.7.10 `Configuration` API.

Changing any ICBM config in the in-game GUI fires one handler (`ConfigMain.onConfigChangedEvent`): re-sync every config class, then rebuild the seven derived hash lists (flying block ban/allow, cluster, cargo, held item, radioactive, protective armor, EMP replacements). `thread_count` is read once at server start and is not re-applied.

The GUI titles come from lang keys `config.icbmclassic:main.title` ("General"), `battery.title`, `thread.title`, `item.title`, `debug.title`. The client, JEI and flying-block classes use lang keys with no matching entries in `en_us.lang` (the config screen shows the raw key).

## 2. `main.cfg` (`ConfigMain`)

| Key | Type | Default | Range | Effect |
|---|---|---|---|---|
| `use_energy` | bool | `true` | | Master power switch. When `false`: `EnergyBuffer.consumePower` always succeeds; the EMP tower's firing and upkeep costs read as 0 and it exposes no energy capability; the launcher base, cruise launcher, EMP tower and radar stop exposing the energy capability; the launcher screen, launcher base, cruise launcher, EMP tower and radar report `NEEDS_POWER=false` in their tooltip data. The comment in code reads "Range of tier 1 launcher" (copy-paste error, ICBM-01-07). See [energy-model](energy-model.md) |
| `protective_armor.min_chemical_gas_protection` | float | `0.5` | 0..1 | Chemical gas blast (02) harms the entity only when its armor rating is below this |
| `protective_armor.min_viral_gas_protection` | float | `0.8` | 0..1 | Same for the contagious/viral gas blast |
| `protective_armor.min_radiation_protection` | float | `1.0` | 0..1 | Radioactive block (06/02) harms the entity when rating < this, or when rating < a fresh random float in `[0,1)`. With the default 1.0, a full set must sum to at least 1.0 |
| `protective_armor.min_debilitation_gas_protection` | float | `0.1` | 0..1 | Same for the debilitation gas blast |
| `protective_armor.require_helmet` | bool | `true` | | If true, an empty head slot gives rating 0 regardless of the other pieces |
| `protective_armor.item_ratings` | string[] | `[]` | | Extra per-item ratings, each `domain:item=float` (optionally with meta; ItemStack config grammar of the wiki page "config-itemstack"). Values clamp to 0..1 per item. Built-in defaults (set in code, config entries override): the eight `atomicscience` hazmat pieces: mask and color mask 0.75, body and color body 0.15, leggings and color leggings 0.05, boots and color boots 0.05 (sum 1.0). |

Rating = sum of the ratings of the four worn armor stacks (not clamped after the sum), or 0 with no helmet when `require_helmet` is on (`ProtectiveArmorHandler.getProtectionRating`).

## 3. `battery.cfg` (`ConfigBattery`)

| Key | Type | Default | Range | Effect |
|---|---|---|---|---|
| `battery_tier_1_capacity` | int | `100000` | >= 1 | Battery item capacity in FE |
| `battery_tier_1_input` | int | `10000` | >= 1 | Max FE accepted per receive call |
| `battery_tier_1_output` | int | `10000` | >= 1 | Max FE released per extract call (the battery can in practice never release, ICBM-01-03) |

Read live (suppliers), so edits apply to existing batteries without a restart.

## 4. `client.cfg` (`ConfigClient`, client only)

| Key | Type | Default | Effect |
|---|---|---|---|
| `missile_engine_smoke` | bool | `true` | Spawn exhaust particles behind an in-flight missile |
| `missile_launch_smoke` | bool | `true` | Spawn the cone of smoke below a missile on the pad |

## 5. `debug.cfg` (`ConfigDebug`)

All default `false`. Each only adds console output.

| Key | Effect |
|---|---|
| `debug_threads` | Log the state of the blast worker threads and threaded blasts (blast base class, thread classes, `WorkerThread`) |
| `debug_explosives` | Log blast logic (the antimatter blast, `EntityExplosion`) |
| `debug_missile_launches` | Log every missile launch with target and origin coordinates |
| `debug_missile_logic` | Log missile logic: flight steps and target changes |
| `debug_missile_tracker` | Log the world missile tracker |

## 6. `flying_blocks.cfg` (`ConfigFlyingBlocks`)

| Key | Type | Default | Effect |
|---|---|---|---|
| `enable` | bool | `true` | Master switch for turning destroyed blocks into flying block entities in blasts. Existing entities remain; other mods can still spawn them |
| `block_ban_allow.ban` | bool | `true` | `true`: the list is a ban list. `false`: the list is an allow list (only listed blocks may fly) |
| `block_ban_allow.list` | string[] | `["minecraft:fire"]` | Block / block-state entries in the ICBM block-state grammar (`domain:block`, `domain:block:meta`, `domain:block[prop:value,...]`, `~` wildcards; wiki page "config-block-states") |
| `damage_scale` | float | `2.0` | Damage dealt by a flying block = ceil(velocity * scale) |

Always banned regardless of config: fire-material blocks, fluid blocks (Forge fluids and vanilla liquids), and any block from the mod `dynamictrees` (hard-coded, with a TODO tied to upstream issue 420). So the default `minecraft:fire` entry is redundant. A commented-out `block_replacements` option ("water -> ice") was never implemented. Allow-list mode with an empty list bans everything.

## 7. `item.cfg` (`ConfigItems`)

All default `true`; all require a game restart (items are registered once). Effects are in [lifecycle-and-registration](lifecycle-and-registration.md#4-items-and-the-content-enable-logic).

| Key | Effect |
|---|---|
| `enable_crafting_items` | Master switch for ingots, clumps, plates, circuits and wires (also gates their loot entries, smelting and ore names) |
| `enable_circuits` | Register the `circuit` item (basic, advanced, elite) |
| `enable_ingots` | Register `ingot` (steel, copper) and `clump` (steel) and the clump smelting |
| `enable_plates` | Register `plate` (steel, iron) and the iron-plate smelting |
| `enable_wires` | Register `wire` (copper, gold) |
| `enable_battery` | Register the battery item. Its recipe JSON is not conditioned on this flag (ICBM-01-34) |
| `enable_sulfur_drops` | Add sulfur dust to creeper and blaze loot |
| `enable_loot_drops` | Add crafting items to dungeon and mineshaft chest loot |

## 8. `mods/jei.cfg` (`ConfigJEI`)

| Key | Type | Default | Effect |
|---|---|---|---|
| `disable` | bool | `false` | Skip all of the plugin (subtype interpreters and recipe pages). Restart needed |
| `disable_payload_recipes` | bool | `false` | Skip only the generated recipe pages for cargo, held-item and cluster missiles. The comment is "payload recipes" |

## 9. `thread.cfg` (`ConfigThread`)

| Key | Type | Default | Range | Effect |
|---|---|---|---|---|
| `thread_count` | int | `1` | 1..8 | Number of worker threads that run blast shape calculations, started at server start. The comment advises about 50% of the machine's cores |

## 10. Derived data tables built from config

These are not keys but they are built from keys above and from the 02/03 configs, with the shared parser in `config/util/`:

| Table | Built from | Used for |
|---|---|---|
| Flying block ban/allow list | `flying_blocks.block_ban_allow.list` | `FlyingBlock.isAllowed` |
| Protective armor ratings | `main.protective_armor.item_ratings` (+ atomicscience defaults) | armor rating for gas and radiation |
| EMP block swaps | EMP blast config replacement list (02) + Mekanism defaults ([integrations](integrations.md#3-mekanism)) | EMP replacing machine blocks |
| Cluster, cargo, held item allow lists | `config/missile/*` (03) | which items may be loaded |

The parser classes (`ResourceConfigList`, `BlockStateConfigList`, `BlockStateConfigListOld`, `ItemStackConfigList`, `ResourceConfigEntry`, `BlockReplacementData`) are 1.12-only plumbing, listed in [network-and-plumbing](network-and-plumbing.md#3-plumbing-classes).
