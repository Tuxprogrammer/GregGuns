# Explosive Registry and Catalog

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. This sheet is the master catalog of every explosive ICBM registers, how the registry hands out IDs, how a tier switches the block, grenade, cart and missile forms on, the fuse times, and every per-blast config key. What each blast *does* is in `blast-engine.md` (shared machinery), `threaded-blasts.md` (nuclear, thermobaric, antimatter) and part b of this area (the individual T1 to T3 blasts). The explosive block itself is in `explosive-block.md`.

## 1. Identity

| Item | Value |
|---|---|
| Classes | `content/reg/ExplosiveInit` (the whole catalog); `lib/explosive/reg/ExplosiveRegistry`, `ExplosiveData`, `ExplosiveContentRegistry`, `ExBlockContentReg`, `ExGrenadeContentReg`, `ExMinecartContentReg`, `ExMissileContentReg`; `api/explosion/IBlastFactory`; `api/EnumTier` |
| Registry keys | `icbmclassic:<name>` (see the catalog). Names may not contain an underscore (`lib/explosive/reg/ExplosiveRegistry.java:64`), which is why the names are `antigravitational` and `redmatter`. Forge lowercases the path, so `redMatter` in the source is `icbmclassic:redmatter` |
| Numeric ID | A legacy integer per explosive. It is the item **damage value** of every explosive item (block, grenade, cart, missile) and the tile's stack damage, so it must stay stable. IDs are persisted in `config/icbmclassic/explosive_reg.json` (name to ID) |
| Persistence file | `icbmclassic/explosive_reg.json`: a flat JSON object `{ "icbmclassic:condensed": 0, ... }`. Loaded before registration, rewritten after the registry locks (`ICBMClassic.java:355-396`) |
| English names | Block form `explosive.icbmclassic:<name>.name` ("Condensed Explosives"); other forms have their own prefixes (section 7). Tier names `tier.icbmclassic.one` to `.four` = "One" to "Four" |
| Creative tab | the ICBM tab (the block and the item forms list only the IDs enabled for them) |

## 2. How a tier switches content on

`newEx(id, name, tier, factory)` (`content/reg/ExplosiveInit.java:290-311`) registers the explosive and then enables content forms by tier, using a rule that is *not* configurable:

| Tier | Block | Missile | Bomb cart | Grenade |
|---|---|---|---|---|
| `ONE` | yes | yes | yes | **yes** |
| `TWO`, `THREE`, `FOUR` | yes | yes | yes | no |
| `NONE` | no | no | no | no |

Tier `NONE` is the "blast only" tier: those IDs exist so code can create the blast through the registry, but they have no item. Tier is otherwise *visual*: the API comment on `IExplosiveData.getTier` says that going forward it "has no functionality". Its only gameplay-adjacent use is the tooltip line "Explosive tier: <name>" in a color (green, yellow, gold, red, white) from `ItemBlockExplosive.getDetailedInfo` (`info.misc.tier`). The tier line is skipped for `NONE`.

Counts: 23 block forms, 23 cart forms, 23 missile forms, 10 grenade forms. Four blast-only IDs (hypersonic, missile, rot, mutation) have no item at all.

A mod can add more: `ExplosiveContentRegistryEvent` (new content types, before the lock) and `ExplosiveRegistryEvent` (new explosives, after the built-ins have reserved their slots). After `completeLock` nothing can be added (late `enableContent` throws). *1.12-only mechanism*: the event buses. The behavior to keep is "built-ins register first, add-ons come after, everything locks once".

## 3. Master catalog

"Size" is the default of the knob the blast factory reads (config key names are in section 5). "Fuse" lists block / cart / grenade defaults in ticks; `-` means that form does not exist. A missile uses the same factory, so its blast is the same as the block's.

| ID | Registry name | English (block) | Tier | Blast class (where documented) | Default size or scale | Fuse (block / cart / grenade) |
|---|---|---|---|---|---|---|
| 0 | `condensed` | Condensed Explosives | 1 | `BlastTNT` (part b) | energy scale 6, entity damage 10 | 0 / 100 / 100 |
| 1 | `shrapnel` | Shrapnel Explosives | 1 | `BlastShrapnel` (part b) | 30 x 30 fragments, damage 11, burning (fire 100) and critical | 100 / 100 / 100 |
| 2 | `incendiary` | Incendiary Explosives | 1 | `BlastFire` (part b) | scale 14 | 100 / 100 / 100 |
| 3 | `debilitation` | Debilitation Explosives | 1 | `BlastDebilitation` (part b) | size 20, 600 ticks | 100 / 100 / 100 |
| 4 | `chemical` | Chemical Explosives | 1 | `BlastChemical` (part b) | size 20, 600 ticks | 100 / 100 / 100 |
| 5 | `anvil` | Anvil Explosives | 1 | `BlastShrapnel` with anvil fragments (part b) | 25 x 25 fragments, damage 30 | 100 / 100 / 100 |
| 6 | `repulsive` | Repulsive Explosives | 1 | `BlastTNT` repel (part b) | scale 2 | 120 / 120 / 120 |
| 7 | `attractive` | Attractive Explosives | 1 | `BlastTNT` attract (part b) | scale 2 | 120 / 120 / 120 |
| 8 | `fragmentation` | Fragmentation Explosives | 2 | `BlastShrapnel`, explosive fragments (part b) | 15 x 15 fragments, damage 11, impact blast 1.5 | 100 / 100 / - |
| 9 | `contagious` | Contagious Explosives | 2 | `BlastContagious` (part b) | size 20, 600 ticks | 100 / 100 / - |
| 10 | `sonic` | Sonic Explosives | 2 | `BlastSonic` (part b) | scale 15 | 100 / 100 / - |
| 11 | `breaching` | Breaching Explosives | 2 | `BlastBreach` (part b) | depth 7, width 1 (3 x 3), energy 15,376 | **0** / 0 / - |
| 12 | (free slot) | | | | | |
| 13 | `thermobaric` | Thermobaric Explosives | 2 | `BlastThermobaric` ([threaded-blasts](threaded-blasts.md)) | scale 30, energy 80 | 100 / 100 / - |
| 14 | (free slot) | | | | | |
| 15 | `nuclear` | Nuclear Explosives | 3 | `BlastNuclear` ([threaded-blasts](threaded-blasts.md)) | scale 50, energy 80 | 100 / 100 / - |
| 16 | `emp` | EMP Explosives | 3 | `ActionDataEmpArea` ([emp-tower](../05-detection-defense/emp-tower.md)) | EMP area 50 (`emp.scale`) | 100 / 100 / - |
| 17 | `exothermic` | Exothermic Explosives | 3 | `BlastExothermic` (part b) | scale 30 | 100 / 100 / - |
| 18 | `endothermic` | Endothermic Explosives | 3 | `BlastEndothermic` (part b) | scale 30 | 100 / 100 / - |
| 19 | `antigravitational` | Anti-Gravitational Explosives | 3 | `BlastAntiGravitational` (part b) | scale 30 | 100 / 100 / - |
| 20 | `ender` | Ender Explosives | 3 | `BlastEnder` (part b) | scale 30 | 100 / 100 / - |
| 21 | `hypersonic` | (name key only) | **NONE** | a zero-size `BlastTNT` placeholder | 0 | keys exist, never read |
| 22 | `antimatter` | Antimatter Explosives | 4 | `BlastAntimatter` ([threaded-blasts](threaded-blasts.md)) | size 55 | **300** / 300 / - |
| 23 | `redmatter` | Red Matter Explosives | 4 | `ActionSpawnRedmatter` (later: red matter) | spawn size 1 | 100 / 100 / - |
| 24 | `missile` | (none) | NONE | zero-size `BlastTNT` placeholder ("missile module") | 0 | - |
| 25 | `rot` | (none) | NONE | `BlastRadioactiveBlockSwaps` ([threaded-blasts](threaded-blasts.md#sub-blasts-rot-and-mutation)) | factory sets none; nuclear passes 80 | - |
| 26 | `mutation` | (none) | NONE | `BlastMutation` ([threaded-blasts](threaded-blasts.md#sub-blasts-rot-and-mutation)) | factory sets none; nuclear passes 80 | - |
| -1 (dynamic) | `colors` | Colorful Explosives | 1 | `BlastColor` (part b) | scale 10 | no keys: 100 / 100 / 100 |
| -1 (dynamic) | `smoke` | Smoke Marking Explosives | 1 | `ActionSpawnEntity` of the smoke entity (part b) | none | no keys: 100 / 100 / 100 |

Notes on the table:

- **IDs 12 and 14** were Regen and S-Mine in older versions and are commented out (`ExplosiveInit.java:195,202`). They are not reserved. `colors` and `smoke` register with ID `-1`, meaning "the next free slot". The registry counter starts at 0, so a **fresh** registry file gives them **12 and 14**, the first two free IDs. After the first run the file pins them. A world that predates `colors`/`smoke` therefore may show old Regen/S-Mine stacks as colors/smoke (ICBM-02-18).
- **Hypersonic** was removed in 4.3.0 and is kept only as a numeric placeholder (comment at `ExplosiveInit.java:240`). It has a lang name and fuse keys but no content.
- **`missile` (24)** is the blank "missile module" item's explosive. A code comment says "mutation" used to be 24.
- `nightmare` has lang keys (`explosive.icbmclassic:nightmare.name`, `bombcart.icbmclassic:nightmare.name`) but no registration (see `A2-lineage`).
- Grenades exist for tier 1 only, so Fragmentation (8) is the first explosive with no grenade. The grenade fuse key for condensed is `CONVENTIONAL`.
- **Breaching** has block and cart fuse 0, and the block skips the explosive entity completely (see `explosive-block.md`).
- **Redmatter** spawns an entity rather than running a `Blast`. Out of scope here (later sheets).
- The **Ender** explosive also registers a block-activation listener and a missile interaction listener that set its target from a GPS tool. Neither listener is ever called (ICBM-02-17).
- The `emp` factory is `ActionDataEmpArea.INSTANCE.create(...)`, so the registry returns the EMP *action*, not a legacy blast. See [emp-tower.md](../05-detection-defense/emp-tower.md) for the effect.

## 4. Registry mechanics

### Registration

1. `register(name, tier, factory)` rejects an underscore in the key, then picks the ID: the name's existing mapping if one was forced or loaded, otherwise the first free integer starting at a running counter. It stores an `ExplosiveData` (name, ID, tier, factory).
2. `forceID(name, id)` pre-seeds the name-to-ID map. `ExplosiveInit` calls it for every explosive with a fixed ID *before* `register`, then `lockForce()` after the last, so add-ons cannot force IDs.
3. `loadReg` reads the JSON file *before* any registration. A name in the file keeps its ID. A *different* name already holding that ID throws at startup (`checkDuplicate`). File keys are trimmed and lowercased.
4. `getExplosiveData(name, allowNull=false)` returns **condensed** for an unknown name; with `true` it returns null. `getExplosiveData(id)` returns null for an unknown ID.
5. Lock order: content types registered and locked; built-ins; add-ons; `lockNewExplosives` (freezes the all-explosives set that the block property uses for its allowed values); `completeLock` (each content registry builds immutable name, ID and data sets and the per-ID fuse tables); then the file is saved.

### `ExplosiveData.create` (the factory call)

`create(world, x, y, z, source, fields)` calls the factory. If the result is a legacy `IBlastInit` it sets the explosive data and the action source, and, **if the caller supplied an `AREA_SIZE` field**, overrides the blast size with it. Then it applies the caller's fields and the data's own fields. This is how a launcher or the EMP tower overrides size. The built-in item forms pass no fields and get the config default.

### Content registries (what a form needs)

Each form has a registry that stores its enabled set plus per-ID callbacks.

| Registry | Extra data | Behavior when no callback is set |
|---|---|---|
| Block (`ExBlockContentReg`) | fuse supplier (world, x, y, z); fuse tick listener; activation listener | fuse **100**; no tick effect; the activation callbacks are stored and never read |
| Grenade (`ExGrenadeContentReg`) | fuse supplier (entity); fuse tick listener | fuse 100 (a null explosive looks up ID 0) |
| Cart (`ExMinecartContentReg`) | fuse supplier (entity); fuse tick listener | fuse 100 |
| Missile (`ExMissileContentReg`) | the launch, update and interaction setters are **empty methods**; `triggerLaunch` posts `MissileEvent.PostLaunch`; `triggerFlightUpdate` does nothing; `onInteraction` returns false | none |

The only built-in fuse tick effect is on the **block** form of `incendiary` and `exothermic`: a lava particle at the entity every tick (`ExplosiveInit.java:79-81,217-218`). `endothermic` has a TODO for an ice animation.

`getDeviceStack(name)` builds the item for a form (`blockExplosive`, `itemGrenade`, `itemBombCart`, `itemExplosiveMissile`) with damage = the explosive ID. The cart and missile versions dereference the registry lookup without a null check (an unknown name gives an NPE). The base `enableContent` compares the data object against a set of names (`ExplosiveContentRegistry.java:55`), so that guard never matches; this is harmless because a set dedupes. Enabling an unknown name throws at lock time ("Failed to locate explosive by name").

### Block recipes (compact)

All block recipes live in `recipes/explosives/` (the full recipe catalog is `06-blocks-items`). Type `icbmclassic:explosive` is a shaped recipe whose ingredients can be explosives by registry name. Unless a row says otherwise, an explosive ingredient is the **block item** of that explosive.

| Explosive | Result count | Grid and ingredients |
|---|---|---|
| condensed | 3 | `trt/rtr/trt`: t = TNT, r = redstone |
| shrapnel | 1 | `aaa/ara/aaa`: a = arrow, r = condensed |
| incendiary | 1 | `aaa/ara/ala`: a = sulfur dust, l = lava bucket, r = shrapnel |
| debilitation | 3 | `sss/wrw/sss`: s = sulfur dust, w = water bucket, r = shrapnel |
| chemical | 1 | `ppp/prp/ppp`: p = poison powder, r = debilitation |
| anvil | 10 | `aaa/ara/aaa`: a = anvil, r = repulsive |
| repulsive | 1 | two condensed in a column |
| attractive | 1 | two condensed in a row |
| colors | 1 | `CCC/CrC/CCC`: C = any dye, r = repulsive |
| smoke | 16 | `CCC/CrC/CCC`: C = charcoal (coal meta 1), r = repulsive |
| fragmentation | 1 | ` a /ara/ a `: a = shrapnel, r = incendiary |
| contagious | 2 | ` c /crc/ c `: c = chemical, r = rotten flesh |
| sonic | 1 | `ini/nrn/ini`: i = iron ingot, n = note block, r = repulsive |
| breaching | 2 | `grg/grg/grg`: g = gunpowder, r = condensed |
| thermobaric | 1 | `cic/iri/cic`: c = chemical, i = incendiary, r = repulsive |
| nuclear | 1 | `tit/iii/tit`: t = thermobaric; i = see the variants below |
| emp | 1 | `rer/sas/sss`: r = redstone, e = battery, s = wire, a = repulsive |
| exothermic | 1 | `iii/igi/iii`: i = incendiary, g = glass |
| endothermic | 1 | `isi/sas/isi`: i = ice, s = snow, a = attractive |
| antigravitational | 1 | `eee/ere/eee`: e = eye of ender, r = shrapnel |
| ender | 1 | `ses/eae/ses`: s = end stone, e = eye of ender, a = attractive |
| antimatter | 1 | `nnn/nrn/nnn`: n = nether star (Mekanism present: atomic alloy), r = nuclear |
| redmatter | 1 | `nnn/nrn/nnn`: n = nether star (Mekanism present: atomic alloy), r = antimatter |

Nuclear's `i` ingredient depends on what is installed (resource conditions): `ingotUranium` or `pelletUranium235` if either exists; otherwise Mekanism's atomic alloy if present; otherwise wither skulls (skull meta 1). The GTNH pack replaces all of these (see `gregguns-icbm-spec`). Cart, grenade and missile recipes are in `recipes/bombcarts`, `recipes/grenades` and `recipes/missile/ex`.

## 5. Config keys (every blast config object)

Config file `config/icbmclassic/blast.cfg` (Forge `@Config`, name `icbmclassic/blast`). Each group below is a nested section. Where a field has no `@Config.Name`, its key is the Java field name, shown as written. The exact on-disk spelling of those keys was not verified (*1.12-only mechanism*: Forge config category naming).

### Global (`config/blast/ConfigBlast`)

| Key | Default | Meaning |
|---|---|---|
| `blast_do_block_updates` | false | "Whether the big explosions trigger block updates for all blocks". In practice only antimatter reads it (ICBM-02-12) |
| `allow_day_night_switch` | true | Deprecated. Read only by exothermic and endothermic, to change the time of day when the daylight cycle is on |

### Per-blast groups

Range column: `>= n` is a minimum with no upper bound; "1 to 2^31" is the declared `RangeDouble(min 1, max Integer.MAX_VALUE)`.

| Group (field) | Key | Default | Range | Used by |
|---|---|---|---|---|
| `condensed` | `energy_scale` | 6 | >= 1 | TNT blast power |
| | `damage` | 10 | >= 1 | entity damage |
| `shrapnel` | `fragments` | 30 | >= 1 | yaw and pitch steps; total = n x n |
| | `damage` | 11 | >= 0 | per fragment |
| `incendiary` (`ConfigExBasics`) | `scale` | 14 | 1 to 2^31 | fire blast size |
| `debilitation` | `size` | 20 | >= 1 | gas cloud size |
| | `duration` | 600 | >= 1 | ticks |
| `chemical` | `size` | 20 | >= 1 | gas size |
| | `duration` | 600 | >= 1 | ticks |
| | `toxicity_start` | 20 | >= 1 | toxicity before damage; 4 points per second |
| | `toxicity_scale` | 0.1 | >= 0 | damage = toxicity x scale, every 5 ticks |
| | `toxicity_min_damage` | 1.0 | >= 0 | minimum damage once started |
| `anvil` | `fragments` | 25 | >= 1 | n x n |
| | `damage` | 30 | >= 0 | per anvil fragment |
| `repulsive` (`ConfigExBasics`) | `scale` | 2 | 1 to 2^31 | push power |
| `attractive` (`ConfigExBasics`) | `scale` | 2 | 1 to 2^31 | pull power |
| `fragmentation` | `fragments` | 15 | >= 1 | n x n |
| | `damage` | 11 | >= 0 | per fragment |
| | `explosive_energy` | 1.5 | >= 0 | TNT blast on fragment impact |
| `contagious` | `size` | 20 | >= 1 | gas size |
| | `duration` | 600 | >= 1 | ticks |
| | `toxicity_start` | 10 | >= 1 | |
| | `toxicity_scale` | 0.05 | >= 0 | |
| | `toxicity_min_damage` | 1.0 | >= 0 | |
| `sonic` (`ConfigExBasics`) | `scale` | 15 | 1 to 2^31 | |
| `breaching` | `depth` | 7 | >= 1 | meters before energy reaches zero |
| | `size` | 1 | >= 0 | half-width; hole is 2 x size + 1 |
| | `damage` | 13 | >= 0 | entity damage |
| | `energy` | 15,376 | >= 0 | start energy per depth line ("enough for several layers of high-tier concrete") |
| | `energy_scale_distance` | 0.25 | >= 0 | start-energy loss per block from center: e = e0 - e0 x (w + h) x scale |
| | `energy_cost_distance` | 0.35 (1 - 0.65) | 0 to 1 | fraction of energy a zero-resistance block costs |
| `thermobaric` | `scale` | 30 | 1 to 2^31 | ray radius |
| | `entityDamageScale` | 50 | 1 to 2^31 | entity damage radius |
| | `energy` | 80 | 1 to 2^31 | ray start energy |
| | `entityDamageMultiplier` | 1000 | 1 to 2^31 | power = energy x multiplier |
| `nuclear` | `scale` | 50 | 1 to 2^31 | ray radius |
| | `rotScale` | 80 | 1 to 2^31 | rot sub-blast radius |
| | `mutationScale` | 80 | 1 to 2^31 | mutation sub-blast radius |
| | `entityDamageScale` | 50 | 1 to 2^31 | entity damage radius |
| | `energy` | 80 | 1 to 2^31 | ray start energy |
| | `entityDamageMultiplier` | 1000 | 1 to 2^31 | power = energy x multiplier |
| | `radioactive_replacements.list` | empty | string list | extra block-state swaps for the rot blast (`RadioactiveHandler`) |
| `emp` | `scale` | 50 | 1 to 2^31 | EMP area size (explosive form) |
| | `replacements.list` | empty | string list | block-state swaps during an EMP |
| | `allow_creeper_charging` | true | | lightning effect on creepers |
| | `missiles.enabled` | true | | EMP affects missiles |
| | `missiles.engine_kill_chance` | 0.8 | 0 to 1 | |
| | `missiles.engine_fuel_blow_chance` | 1.0 | 0 to 1 | |
| | `missiles.engine_fuel_blow_strength` | 1.0 | >= 0.1 | |
| | `missiles.missile_kill_chance` | 0.5 | 0 to 1 | |
| | `missiles.missile_trigger_chance` | 0.1 | 0 to 1 | |
| | `allow_entity_inventory` | true | | |
| | `allow_tile_inventory` | true | | |
| | `allow_item_inventory` | true | | |
| | `allow_ground_items` | true | | |
| | `allow_draining_energy_entity` | true | | |
| | `allow_draining_energy_items` | true | | |
| | `allow_draining_energy_tiles` | true | | |
| | `allow_entities` | true | | |
| | `allow_tiles` | true | | |
| `exothermic` (`ConfigExBasics`) | `scale` | 30 | 1 to 2^31 | |
| `endothermic` (`ConfigExBasics`) | `scale` | 30 | 1 to 2^31 | |
| `antigravitational` (`ConfigExBasics`) | `scale` | 30 | 1 to 2^31 | |
| `ender` (`ConfigExBasics`) | `scale` | 30 | 1 to 2^31 | |
| `antimatter` | `break_unbreakable` | true | | ignore hardness (bedrock, wards) |
| | `damage_on_redmatter_kill` | false | | if false, an antimatter blast that finds a red matter kills it and does nothing else (ICBM-02-10) |
| | `size` | 55 | >= 1 | sphere radius in meters |
| | `damage` | 2,147,483,647 (float) | >= 1 | entity damage at start and end |
| `redmatter` | `damage` | 2000 | >= 0 | crush damage at the center |
| | `redmatter_max_size` | 70 | | |
| | `redmatter_default_size` | 1 | | spawn size of block, missile and cart |
| | `redmatter_min_size` | 0.25 | | dies below this |
| | `redmatter_scale_rendering` | 0.05 | | render size multiplier |
| | `redmatter_scale_kill_radius` | 0.08 | | instant-kill center multiplier |
| | `redmatter_scale_gravity` | 2.0 | | pull multiplier |
| | `redmatter_starve_multiplier` | 0.99 | 0 to 1 | mass kept per starve tick |
| | `redmatter_max_edits_per_tick` | 100 | | |
| | `redmatter_raytrace_per_tick` | 500 | | at default size |
| | `redmatter_flying_block_spawn_chance` | 0.8 | | |
| | `redmatter_audio_enabled` | true | | |
| | `redmatter_spawn_flying_blocks` | true | | |
| | `redmatter_colored_beams` | true | | client render |
| `colorful` (`ConfigExBasics`) | `scale` | 10 | 1 to 2^31 | |

`ConfigRedmatter` also has a field `REDMATTER_MOVEMENT` (true) whose annotation is commented out, so it is effectively a constant. The red matter keys belong to the later red matter sheets and are listed here for completeness only.

### Fuse times (`config/blast/ConfigFuseTimes`, group `FUSE_TIMES`)

Three sub-groups with upper-case field-name keys, each an int from 0 to 2^31 - 1, in ticks (20 = 1 second). Titles come from `config.icbmclassic:blast.fusetimes.*.title` ("Fuse Times", "Bomb Carts", "Explosives", "Grenades").

| Key | `EXPLOSIVES` (block) | `BOMB_CARTS` | `GRENADES` |
|---|---|---|---|
| `CONDENSED` (`CONVENTIONAL` in grenades) | **0** | 100 | 100 |
| `SHRAPNEL`, `INCENDIARY`, `DEBILITATION`, `CHEMICAL`, `ANVIL` | 100 | 100 | 100 |
| `REPULSIVE`, `ATTRACTIVE` | 120 | 120 | 120 |
| `FRAGMENTATION`, `CONTAGIOUS`, `SONIC`, `THERMOBARIC`, `NUCLEAR`, `EMP`, `EXOTHERMIC`, `ENDOTHERMIC`, `ANTI_GRAVITATIONAL`, `ENDER`, `REDMATTER` | 100 | 100 | no key |
| `BREACHING` | **0** | **0** | no key |
| `ANTIMATTER` | **300** | **300** | no key |
| `HYPERSONIC` | 100 (dead) | 100 (dead) | no key |

There are no keys for `colors` or `smoke` in any group; they use the hard-coded fallback of 100. A fuse of 300 ticks is 15 seconds.

### Threads and debug

| Group | Key | Default | Meaning |
|---|---|---|---|
| `icbmclassic/thread` (`config/ConfigThread`) | `thread_count` | 1 (range 1 to 8) | number of blast worker threads |
| `icbmclassic/debug` (`config/ConfigDebug`) | `debug_threads` | false | log thread start, stop and timing |
| | `debug_explosives` | false | log blast controller creation and antimatter timings |

## 6. Persistence and sync

- The registry file above. The explosive ID travels in the damage value of the item stack. Besides item stacks, only `EntityExplosion` (NBT key `ex_id`, a registry-name string) and block tiles (a full stack under `explosive_stack`) store explosive identity.
- Blast factories read their config **when the blast runs**, not at registration, so changed values apply to the next detonation after Forge reloads the config (reload timing not verified).

## 7. Lang

| Key pattern | English text pattern |
|---|---|
| `explosive.icbmclassic:<name>.name` | "<Name> Explosives" (Anti-Gravitational with a hyphen; Colorful Explosives; Smoke Marking Explosives) |
| `explosive.icbmclassic:<name>.info` | one-line flavor text, for example condensed "Slightly more destructive version of TNT", nuclear "Hi! I'm a nuclearholic..", antimatter "Matter + antimatter = No matter" |
| `grenade.icbmclassic:<name>.name` | "<Name> Grenade", tier 1 only |
| `bombcart.icbmclassic:<name>.name` | "<Name> Minecart" |
| `missile.icbmclassic:<name>.name` | "<Name> Missile" |
| `bomblet.icbmclassic:<name>.name` | "<Name> Bomblet" (cluster missile content; `03-missiles`) |
| `tier.icbmclassic.one` to `.four` | One, Two, Three, Four (there is no `none` key) |
| `info.misc.tier` | Explosive tier |
| `explosive.icbmclassic:ender.info`, `.pos`, `.world` | "Let's relocate that base of yours"; "Target: %1$sx, %2$sy, %3$sz"; "World: [%1$s] %2$s" |
| `explosive.icbmclassic:redMatter.info.count` and `.0` to `.10` | 11 joke lines. The code asks for `icbm.explosive.redMatter.info...` instead, and only on a branch that cannot run, so they never show (ICBM-02-19) |
| `chat.launcher.toolTargetSet`, `chat.launcher.noTargetInTool` | chat messages for the (dead) ender GPS binding |

## 8. Relationships

- `03-missiles`: the missile form uses the same factory; launch events.
- `05-detection-defense`: the `emp` explosive and the EMP action.
- `06-blocks-items`: item classes `ItemGrenade`, `ItemBombCart`, `ItemMissile`; the recipe catalog.
- `01-core`: the explosive capability (`CapabilityExplosive*`), `ICBMClassicAPI` registries, `NBTConstants`.
- Fuse behavior and the explosive entity: `explosive-block.md`.

## 9. Bugs

ICBM-02-10 (antimatter and red matter config), ICBM-02-12 (block-update flag), ICBM-02-17 (ender listeners dead), ICBM-02-18 (dynamic IDs recycle old slots), ICBM-02-19 (tooltip key mismatch), ICBM-02-20 (fuse stored as a byte). The table is in `index-a.md`.
