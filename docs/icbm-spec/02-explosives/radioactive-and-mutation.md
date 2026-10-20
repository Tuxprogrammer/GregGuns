# Radioactive blocks, rot and mutation

How a nuclear blast leaves a contaminated landscape and mutated animals. The pieces: the **rot** blast that swaps blocks (a threaded blast, the block list is in a config), the **radioactive block** that damages nearby entities and slowly heals itself, and the **mutation** blast. The nuclear blast that triggers rot and mutation is part a (`02-explosives/blast-engine.md`); protective-armor ratings are in [gas-and-status.md](gas-and-status.md#4-protective-armor-contentgasprotectivearmorhandlerjava-configconfigmainjava).

Source root for all paths: `src/main/java/icbm/classic/` at `9c1d985a`. Block states, `ForgeRegistries` and the config annotation system are *1.12-only mechanisms*; the sheet describes what they produce.

## 1. Rot: block swaps (`content/blast/BlastRadioactiveBlockSwaps.java`, `content/radioactive/RadioactiveHandler.java`)

| | |
|---|---|
| Explosive | `rot`, ID 25, tier NONE: no item, block, missile or cart. Only the nuclear blast creates it (`onBlastCompleted`) |
| Class | `BlastRadioactiveBlockSwaps`, a threaded, tickable blast |
| Size | `nuclear.rotScale` (category `nuclear` in `icbmclassic/blast`, double, min 1, default 80); the sphere radius is `max(3, size)` |
| Shape | every cell of the solid sphere of radius R around the blast (about 2.1 million cells at 80) |
| Effect | each cell goes through the swap table once |

**Flow.** The worker thread enumerates the sphere (loop over a cube, keep cells with `x^2 + y^2 + z^2 <= R^2`), sorts the list by distance, and the main thread queues it through the block-edit handler (a few edits per tick, part a). For each queued cell the blast looks up the current block state in the swap table. If there is an entry it rolls the entry's chance and places the entry's block state (flag 3), optionally merging NBT into the new tile. Nothing else happens: no damage, no sound, no particles. The source has TODOs for "only contaminate the top few layers" and "reduce impact behind shielding", so currently caves, ore and deep stone are swapped just like the surface.

**Swap table (`RadioactiveHandler.radioactiveBlockSwaps`).** Defaults, in order of priority 0:

| Match | Becomes | Chance |
|---|---|---|
| `minecraft:stone` | radioactive block, type STONE | 0.2 |
| `minecraft:dirt`, `farmland`, `grass_path`, `grass`, `mycelium` | radioactive block, type DIRT | 0.2 each |
| every registered block whose material is plants, leaves or vine | air | always (no chance given) |

The plant rule is added by scanning the block registry once, at setup and on config reload. Mods that add plants or leaves with other materials are not covered. Chance semantics: an entry with a negative chance (the default when unspecified) always applies; otherwise it applies when `random() <= chance`.

**Config.** `icbmclassic/blast`, category `nuclear`, sub-category `radioactive_replacements`, key `list` (string array, default empty). It is read through the block-state list parser:

| Form | Meaning |
|---|---|
| `domain:block=domain:block` | match by block, replace with the default state |
| `domain:block@meta=...` | match a metadata value |
| `domain:block[prop:value,...]=...` | match by block-state properties; `~` means any value, `~text`/`text~` prefix and suffix match |
| value `domain:block;@chance(0.5);@nbt({...})` | replacement with an explicit chance and tile NBT; `~` as the block keeps the existing block (only the NBT applies) |
| value `domain:block,0.5` | legacy form: replacement then chance |

*1.12-only mechanism*: the parser and the metadata-and-property syntax. A GTNH rebuild needs a block-and-meta list; the behavior to keep is "list of match -> replacement with chance".

**Bugs and risks.** The sphere is enumerated and queued in full (ICBM-02-67 notes the load this creates once the blocks exist). The swap table is rebuilt per config event, not per blast.

## 2. Radioactive block (`content/radioactive/BlockRadioactive.java`)

| | |
|---|---|
| Registry | block `icbmclassic:radioactive`, two variants by property `type`: `dirt` (meta 0) and `stone` (meta 1); names "Radioactive Dirt" and "Radioactive Stone" (`tile.icbmclassic:radioactive.0.name`, `.1.name`) |
| Material | rock; creative tab: ICBM |
| Hardness | the dirt variant reports dirt's hardness (0.5), the stone variant stone's (1.5); the block's own constant is 0.5 |
| Explosion resistance | same as dirt or stone respectively |
| Drops | itself with its variant (`damageDropped` is the meta); creative sub-blocks list both |
| Textures and models | `blockstates/radioactive.json`, models `radioactive_dirt.json` and `radioactive_stone.json`, textures under `textures/blocks/radioactive/dirt/` and `.../stone/` (not measured) |
| Sound | `radioactive_tick` (12,158 bytes), played client-side by `randomDisplayTick` with probability 1/12 per display tick, volume 1.0 to 1.5, pitch 1.0 to 1.2. Subtitle "Radioactive Tick" |
| Death message | `death.attack.icbmclassic:radioactive_block` |

**Config** (file `icbmclassic/blocks`, category `radioactive`):

| Key | Default | Meaning |
|---|---|---|
| `random_world_tick` | false | true: only the world's random ticks run the logic (rate follows `randomTickSpeed`); false: the block also schedules its own ticks |
| `decay_delay` | 5 | maximum ticks between scheduled runs. The range annotation says min 1 although the comment says "set to zero to disable" (ICBM-02-66) |
| `decay_block_chance` | 0.0001 | chance per run that the block turns harmless; 0 disables |
| `decay_effect_chance` | 0.85 | chance per run, per entity in range, that the effect fires |
| `decay_effect_range` | 5 | half-width of the effect box in blocks |
| `decay_effect_damage` | 2 | damage per hit |
| `decay_effect_wither_duration` | 20 | on/off only: any value above 0 enables a 20-tick Wither (ICBM-02-65) |

**Run (`updateTick`, server only).**

1. **Self decay.** If `decay_block_chance` is above about 0: with that chance the block becomes harmless (DIRT variant becomes dirt, STONE variant becomes stone) and no further run is scheduled (the entity check below still runs once on that pass).
2. **Entity check.** If range and chance are above 0: take all `EntityLivingBase` in the box `block +/- range` on every axis (a cube measured from the block's face, so a reach of about 5.5 blocks from the center on each axis). Each one passes with `decay_effect_chance`, then the protection rule: affected when `rating < min_radiation_protection (1.0)` or `rating < random()`. An affected entity takes `decay_effect_damage` with the radioactive source, plus Wither for 20 ticks.
3. **Reschedule.** Unless the block decayed or `random_world_tick` is true, schedule another run in `random(decay_delay) + 1` ticks (1 to 5, mean 3 at the default).

`randomTick` calls the same function. `setTickRandomly(true)` is set only when `decay_delay > 0` at class construction. The block does not schedule a first tick when placed (`onBlockAdded` is not overridden), so a newly placed field is silent until each block gets its first random tick, on average about 68 seconds with the default `randomTickSpeed` of 3 (ICBM-02-66). After that the scheduled chain runs on its own, and the scheduled call dedupes with itself.

**Numbers.** Block lifetime with the default chance and a mean 3-tick interval is about 30,000 ticks, roughly 25 minutes. Standing within range of an active block hurts for 2 per run at 85% probability; with the 10-tick vanilla invulnerability window that is about 4 health per second (2 hearts) from a single block, and nothing extra from more blocks. Full protection (rating 1.0, all four armor slots with the default hazmat values, helmet present) negates it; any lower total is always hurt because the threshold is 1.0.

**Load.** An 80-radius rot with 20% swaps yields on the order of 200,000 live blocks, each doing an entity query every 3 ticks, which is thousands of box queries per tick (ICBM-02-67). The other costs are covered by the general threaded-blast notes in part a.

**Bugs.** ICBM-02-65, 02-66, 02-67.

## 3. Mutation (`content/blast/BlastMutation.java`)

| | |
|---|---|
| Explosive | `mutation`, ID 26, tier NONE; only the nuclear blast creates it (and gas uses its static helper) |
| Size | `nuclear.mutationScale` (double, min 1, default 80); radius `max(3, size)` |
| Entities | every `EntityLiving` (mobs and animals, **not** players) in the cube of half-width R |
| Sound | none (`playExplodeSound` is overridden empty) |

**Effect.** `applyMutationEffect(entity)`:

| Target | Becomes | Copied |
|---|---|---|
| `EntityPig` | `EntityPigZombie` (zombie pigman) | position only |
| `EntityVillager` | `EntityZombieVillager` | position and the villager's Forge profession |
| anything else | unchanged | n/a |

The old entity is set dead and the new one is spawned at the same point. The new entity gets no rotation, age (a baby villager becomes an adult zombie), name tag, equipment, health, leash, riders or home position, and no initial-spawn pass, so a pigman has no sword. `preventEntitySpawning` is set on the new entity but means "blocks placement", not "do not spawn". The helper returns true when it mutated something; the contagious gas uses that to skip the damage for the hit.

**Completion.** `doExplode` returns false, so the blast never completes and stays in the global active-blast list until the world unloads (ICBM-02-57). The effect itself is instant, once.

**Used by.** Nuclear blast completion (radius 80 by default, which is 160 x 160 x 160 cubes) and the contagious gas (section 2 of [gas-and-status.md](gas-and-status.md#2-the-four-gas-blasts)).

## 4. How nuclear uses these (summary)

`BlastNuclear.onBlastCompleted` (part a) runs three things in this order after the crater work: a concussion damage pass (radius `entityDamageScale` = 50, power `energy * entityDamageMultiplier`), then a `rot` blast of radius `rotScale` at the same point, then a `mutation` blast of radius `mutationScale`, then the `explosion` sound. The TODO list in that method (radiation-wave damage, radioactive gas, radioactive dust, fall-out) shows the intended direction; none of it exists.

## 5. Lang

| Key | English |
|---|---|
| `tile.icbmclassic:radioactive.0.name` | Radioactive Dirt |
| `tile.icbmclassic:radioactive.1.name` | Radioactive Stone |
| `death.attack.icbmclassic:radioactive_block` | %s died to radioactive fallout |
| `subtitle.icbmclassic:radioactive_tick` | Radioactive Tick |
| `potion.radiation` | Radiation (unused, no code references it) |

## 6. What a rebuild should keep

Behavior worth keeping: a list-driven rot swap with per-entry chance; a radioactive block pair with slow self-decay and a protection-gated damage zone; mutation as a pure replace. Behavior to change: the full-sphere enumeration, the per-block entity query, the missing first schedule, the ignored wither duration and the unbounded protection threshold. A GTNH rebuild would feed the swap list from ore-dictionary groups and the protection rating from the pack's hazmat gear (GregGuns rules are in `docs/gregguns-icbm-spec`).
