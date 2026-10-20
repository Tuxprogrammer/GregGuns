# Threaded Blasts: Nuclear, Thermobaric, Antimatter

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Three explosives compute their crater on a worker thread and then remove blocks through the edit queue. All three extend `content/blast/threaded/BlastThreaded`, whose scheduling is described in [blast-engine.md](blast-engine.md#4-threads). This sheet gives their shapes, numbers, timelines and the two sub-blasts (rot and mutation) that the nuclear blast triggers. Catalog and config keys: [explosive-registry.md](explosive-registry.md).

## 1. Identity

| Item | Nuclear | Thermobaric | Antimatter |
|---|---|---|---|
| Class | `content/blast/threaded/BlastNuclear` | `BlastThermobaric` | `BlastAntimatter` |
| Registry key / ID / tier | `icbmclassic:nuclear` / 15 / 3 | `icbmclassic:thermobaric` / 13 / 2 | `icbmclassic:antimatter` / 22 / 4 |
| Forms | block, cart, missile | block, cart, missile | block, cart, missile |
| English name (block) | Nuclear Explosives | Thermobaric Explosives | Antimatter Explosives |
| Flavor text (`.info`) | "Hi! I'm a nuclearholic.." | "Mini-nuke in a can. Or at least as close as the North can get" | "Matter + antimatter = No matter" |
| Shape | ray-cast blob | ray-cast blob | sphere with a soft edge |
| Controller entity | none | none | none |
| Fuse (block / cart) | 100 / 100 ticks | 100 / 100 | 300 / 300 |

Unlike most ticking blasts, none of the three is `IBlastTickable`: they start from `doAction` directly, have no controller entity, are not saved with the world, and cannot be moved. If the server stops while the worker thread runs or while edits are queued, the rest of the crater is simply never made.

## 2. Obtaining

See the recipe table in [explosive-registry.md](explosive-registry.md#block-recipes-compact): thermobaric = 4 chemical + 4 incendiary around a repulsive; nuclear = 4 thermobaric + 5 of a uranium or alloy ingredient; antimatter = nuclear surrounded by 8 nether stars (or atomic alloy). Cart, missile and block forms share the same explosive.

## 3. Numbers

### Nuclear and thermobaric

| Name | Nuclear | Thermobaric | Config (group.key) |
|---|---|---|---|
| Ray radius (blast "scale" = size) | 50 m | 30 m | `nuclear.scale`, `thermobaric.scale`; also `AREA_SIZE` override; the radius used is `max(3, size)` |
| Ray start energy | 80 | 80 | `*.energy` |
| Entity damage radius | 50 m | 50 m | `*.entityDamageScale` |
| Entity damage multiplier | 1000 | 1000 | `*.entityDamageMultiplier` |
| Entity damage power | `energy x multiplier` = 80,000 | 80,000 | derived |
| Rot radius / mutation radius | 80 m / 80 m | none | `nuclear.rotScale`, `nuclear.mutationScale` |
| Ray count | `2 x steps^2`; steps = ceil(pi x 50) = 158; **49,928 rays** | steps 95; **18,050 rays** | derived |

### Per-ray energy budget (both)

| Quantity | Value |
|---|---|
| Start power of one ray | uniformly 50% to 100% of `energy`: 40 to 80 |
| Cost per half-block step (every step, air included) | 0.3 x 0.75 x 5 = **1.125** |
| Cost per whole block of straight travel | 2.25 |
| Maximum reach in open air | 17.8 to 35.6 m (power / 2.25) |
| Extra cost on entering a block | its explosion resistance, charged **once** per distinct block (`blastResistance / 5` by default; liquids a flat 0.25) |
| Block is recorded for removal if | hardness >= 0 and power is still above 0 after paying |
| Examples (default resistances) | stone resistance 10 -> 2.0, so about **4.25** per block of stone: 9 to 19 m of stone penetration; dirt about 0.1 -> 2.35 per block: 17 to 34 m; water and lava 0.25 -> 2.5 per block; cobblestone and iron blocks about 6 -> 8.25 per block; obsidian (resistance 2000, so 400) stops every ray |

Because a ray in air still pays 2.25 per block, **a nuclear blast with `scale` 50 never reaches farther than about 36 m**: the extra 14 m of configured radius do nothing unless `energy` is raised (ICBM-02-05). A thermobaric ray reaches its full 30 m only 31% of the time (needs power above 67.5; P = (80 - 67.5) / 40).

### Antimatter

| Name | Default | Config key |
|---|---|---|
| Sphere radius | 55 m | `antimatter.size` (integer; also `AREA_SIZE` override; `max(3, size)`) |
| Entity damage | 2,147,483,647 (float) at start and again at end | `antimatter.damage` |
| Entity damage radius | 110 m (2 x size) | none |
| Unbreakable blocks (bedrock, wards) are destroyed | yes | `antimatter.break_unbreakable` |
| Damage and block loss when a red matter is found | false: kills the red matter and does nothing else | `antimatter.damage_on_redmatter_kill` |
| Block updates | flag 2 (no neighbor updates) unless `blast_do_block_updates` is true | `blast_do_block_updates` |
| Edge feather | outer 5% of the squared radius | constant |
| Positions in the sphere | about 697,000 (in a 111^3 = 1.37 million cube) | derived |

## 4. Behavior

### Timeline (nuclear; thermobaric is the same without the sub-blasts)

1. The explosive entity (block or cart) or missile calls the blast's `doAction`: Forge `ExplosionEvent.Start`, vanilla generic explosion sound (volume 4).
2. `setupBlast`: server-side smoke particles (see visuals), **first entity damage pass** (section 7), nuclear-style `explosion1` sound at volume 7.
3. `doExplode`: hands the ray task to the worker pool and returns. The blast now waits, with no controller and no ticks.
4. Worker thread: runs the ray cast (section 5), sorts the position list nearest-first, and schedules the rest on the main thread.
5. Main thread, in one tick: queues all positions in the dimension's edit queue (destroyed over the following ticks, nearest first, through [the edit budget](blast-engine.md#6-block-edit-budget-mit-portable)), then **immediately** runs `onPostThreadJoinWorld`: the blast ends (`isAlive` false), and the post effects run: **second entity damage pass**, nuclear only: rot blast, mutation blast, and an `explosion1` sound at volume 10.
6. Over the next ticks (up to many seconds, depending on queue length) the crater opens from the center outward.

The post effects at step 5 therefore fire before a single crater block has been removed (ICBM-02-11). A nuclear detonation is effectively: kill everything nearby at t=0 (twice), turn pigs and villagers around into zombies at t=0, queue the crater, then queue 2.1 million block-state swap checks behind it.

### Antimatter timeline

1. `doAction`: Forge start event, generic sound, `setupBlast`: `antimatter` sound (volume 7, pitch 0.9 to 1.0, distance delay on), then the **first entity pass** (radius 110, with the red matter rule below).
2. `doExplode` queues the worker task. The worker lists every position in the sphere that survives the feather test (including air), sorts nearest-first.
3. Still on the worker thread it pre-selects "fluids and falling blocks": any loaded position whose material is water or whose block is a falling block (sand, gravel, anvils). The vanilla lava is *not* in this set.
4. Main thread, one tick: **all pre-selected positions are set to air immediately with flag 2, bypassing the edit budget** (a 55 m sphere over ocean means hundreds of thousands of block writes in one tick; ICBM-02-13). Then the complete list (including those positions again) is queued for the normal budget, then the blast ends and the **second entity pass** runs (again at queue time, not after the crater).
5. Each queued position: if not air and (hardness >= 0 or `break_unbreakable`), set to air with flag 2 or 3.

**Red matter rule.** Entity damage is overridden. If `damage_on_redmatter_kill` is false, the pass collects all entities in the box (half-size 2 x 110 + 1 = 221 blocks, not just 110), calls the per-entity hook that **kills every red matter entity found** (`setDead`, regardless of distance within the box; ICBM-02-26), and if at least one was found the pass returns false, so **no one else is damaged**. At the start of the blast, that false return makes `doFirstSetup` report failure, `doAction` returns `SETUP_ERROR`, `doExplode` is never called, and **no blocks are destroyed** (the config's intent works, but through an error path that leaves the blast object registered forever; ICBM-02-10). With the flag true, red matter is still killed and everyone else is damaged normally.

### Antimatter edit test (MIT-portable)

Permalink: [BlastAntimatter.java L132-L146](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/threaded/BlastAntimatter.java#L132-L146). For each integer offset (x, y, z) in the solid sphere of radius R (`BlastHelpers.forEachPosInRadius`):

```
distSQ = x^2 + y^2 + z^2
blastSQ = R^2
feather = floor(blastSQ * 0.05)
delta = floor(blastSQ - distSQ)
if delta < feather:
    p = 1 - delta / feather        # 1 at the very edge, near 0 just inside the band
    edit only if rand() > p        # so P(edit) = delta / feather: 0% at the rim rising to ~100%
else:
    edit
```

At R = 55: blastSQ 3025, feather 151, so the band starts at distance `sqrt(3025 - 151)` = 53.6 m. The rim is ragged, not smooth. Random numbers are drawn from the world's random on the worker thread. The block position is the blast center truncated to ints plus the offset.

## 5. Ray casting (MIT-portable)

Permalinks: [BlastNuclear.java L34-L124](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/threaded/BlastNuclear.java#L34-L124), [BlastThermobaric.java L32-L122](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/threaded/BlastThermobaric.java#L32-L122), scheduling [BlastThreaded.java L28-L116](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/threaded/BlastThreaded.java#L28-L116). The two ray casters are identical apart from the config object and (as written) the nuclear one's sub-blasts. This is the fixed version of the legacy `ThreadLargeExplosion` caster ([blast-engine.md](blast-engine.md#5-legacy-ray-casters-mit-portable)).

```
R      = max(3, size)
steps  = ceil(pi * R)
for yawSlice in 0 .. 2*steps - 1:                 # yaw 0 .. 2*pi in pi/steps increments
  for pitchSlice in 0 .. steps - 1:               # pitch 0 .. pi in pi/steps increments
     power  = energy - energy * rand(0..1) / 2    # 50%..100% of energy
     yaw    = pi / steps * yawSlice
     pitch  = pi / steps * pitchSlice
     step   = 0.5 * (sin(pitch)cos(yaw), cos(pitch), sin(pitch)sin(yaw))   # half-block steps
     p      = blast center (double)
     prev   = none
     while distance(p, center) <= R and power > 0:
         power -= 1.125                            # every step, including air
         cell = floor(p)
         if cell != prev (by value):
             if cell is in an unloaded chunk: continue    # retry same p; power keeps draining
             state = block at cell
             if state is not air:
                 power -= resistance(state)       # liquid: 0.25, else blastResistance / 5
                 if hardness(state) >= 0 and power > 0: edits.add(cell)
         prev = cell
         p += step
```

Notes for a port:

- Every yaw slice also casts the pitch-0 ray (straight up), so the vertical direction is sampled `2 x steps` times while directions near the equator are sampled once per slice. Ray density is highest at the poles; the pitch range stops just short of straight down.
- Unbreakable blocks (bedrock) are *not* recorded but their (huge) resistance ends the ray, so bedrock shields what is behind it.
- The legacy caster this was copied from compares block positions by reference; this version compares by value, so each block is charged once per ray.
- The ray cast cost: up to 49,928 rays x 100 half-steps = about 5 million block lookups for a nuclear blast, on one thread.

## 6. Sub-blasts: rot and mutation

Triggered by `BlastNuclear.onBlastCompleted` (so at queue time, section 4, step 5), after the second damage pass, with the nuclear blast's exploder as source and **no action source**. Both are registered as tier NONE explosives (IDs 25 and 26).

| | Rot (`rot`, ID 25) | Mutation (`mutation`, ID 26) |
|---|---|---|
| Class | `content/blast/BlastRadioactiveBlockSwaps` (a `BlastThreaded` that is also tickable, so it gets its own `EntityExplosion`) | `content/blast/BlastMutation` (a plain `Blast`) |
| Size | `nuclear.rotScale` = 80 | `nuclear.mutationScale` = 80 |
| Shape | every integer offset in the solid sphere of radius `max(3, 80)`: about 2.14 million positions (from a 161^3 = 4.17 million cube), **air included**, no ray casting (TODOs mention shielding and "top few layers only") | the axis-aligned **cube** of half-width 80 around the center, all living entities |
| Effect | for each queued position, looks up the block state in the radioactive swap table (`RadioactiveHandler`, config `nuclear.radioactive_replacements.list`) and applies the replacement (dead/radioactive versions of dirt, stone, plants...) | pig -> zombie pigman; villager -> zombie villager (keeps profession). Other entities untouched. The new entity is placed at the old position with default stats and the old one is removed |
| Scheduling | worker task, then edit queue behind the crater's queue | runs synchronously inside the nuclear post step |
| Sound | generic explode sound (volume 4) | silenced |

Details of the swap table and the radioactive blocks are in [radioactive-and-mutation.md](radioactive-and-mutation.md) (part b) and `06-blocks-items`. The rot blast's task is the single heaviest unit of work in the mod: 2.14 million block-position objects on a single worker thread, sorted, then queued; the edit queue processes about 51 positions per call within a 35 ms budget, so the swap can take minutes of game time and delays any other blast queued behind it in the same dimension.

`BlastMutation.doExplode` always returns false, so the blast never completes and stays in the active-blast list forever (ICBM-02-14).

## 7. Entity damage numbers

Formula: [blast-engine.md section 8](blast-engine.md#8-entity-damage-mit-portable). For nuclear and thermobaric `power` = 80,000 and `radius` = 50, so `damage = (s^2 + s) / 2 x 640,000 + 1`, where `s = (1 - distance / 50) x shielding` and shielding is vanilla block density (0 to 1).

| s | Damage | Example |
|---|---|---|
| 1.0 | 640,001 | standing at the center, open air |
| 0.5 | 240,001 | 25 m out, open air |
| 0.1 | 35,201 | 45 m out, or 25 m out with 80% cover |
| 0.01 | 3,233 | |
| 0.001 | 321 | |
| 0.00006 | about 20 | a full-health player still dies |

Anything with any exposure at all within 50 m dies in both passes (the damage is applied at the start and again at queue time). Only a fully enclosed entity (density 0) takes the minimum of 1. Armor and Protection enchantments reduce the number but not enough to matter. Knockback is `s` blocks per tick (up to 1), also applied at both passes.

Antimatter: power 2.147e9 over radius 110. The int cast saturates, so every entity within 110 m with any exposure takes 2,147,483,647.

## 8. Persistence and sync

- No NBT: threaded blasts have no entity and are not saved (`Blast.save/load` exist but are used only by `EntityExplosion`).
- The only client-visible effects are the two `explosion1` sounds (nuclear, thermobaric; volumes 7 and 10, pitch `(1 + (r1 - r2) x 0.2) x 0.7`) or the antimatter sound, and the block changes themselves.

## 9. UI

*None.* (The block's tooltip is in [explosive-block.md](explosive-block.md); the lang lines are in the registry sheet.)

## 10. Visuals

- **Nuclear and thermobaric "cloud"**: `setupBlast` loops 26 height slices (y = 0 to 25). For each, a radius profile: y < 8 -> `max(min((8 - y) x 2, 10), 4)`; 8 to 15 -> 4; y > 15 -> `max(min((y - 15) x 2, 15), 5)`. In each slice, every integer (x, z) in the square whose distance is between `r - 3` and `r` gets one `SMOKE_NORMAL` particle at the center + (2x, 2(y - 2), 2z) with a small outward and downward velocity. This is a stem-and-cap mushroom shape up to 52 m tall. **It runs on the server world, where particle spawning does nothing**, so the cloud never appears (ICBM-02-21; same family as ICBM-05-13).
- `doExplode` also has a client-side "expanding smoke ring" block (a ring of radius `callCount` with a small probability per cell). `Blast` ticks run on the server only and this blast is not even ticked, so it is dead code.
- Antimatter has no particles.
- Block textures and models: see [explosive-block.md](explosive-block.md).

## 11. Audio

| Sound | Event / file | Trigger | Volume | Pitch |
|---|---|---|---|---|
| Generic start | vanilla `entity.generic.explode` | `doAction` of any legacy blast | 4.0 | `(1 + (r1 - r2) x 0.2) x 0.7` |
| `explosion` | `explosion1.ogg` (about 109 KB) | nuclear / thermobaric setup | 7.0 | same pitch formula |
| `explosion` (end) | `explosion1.ogg` | after queue, nuclear / thermobaric | 10.0 | same |
| `antimatter` | `antimatter.ogg` (about 169 KB) | antimatter setup | 7.0 | `0.9 + rand x 0.1`, distance delay on |

Durations and sample rates were not measured. `sounds.json` registers both with category `block` and subtitle keys (`subtitle.icbmclassic:explosion`, `subtitle.icbmclassic:explosion.antimatter`). `ICBMSounds.play` is a helper in `client/ICBMSounds`.

## 12. Relationships

- `blast-engine.md` for the lifecycle, thread pool, edit queue and damage routine.
- Rot depends on the radioactive block swap tables and blocks ([radioactive-and-mutation.md](radioactive-and-mutation.md); `06-blocks-items`).
- Antimatter vs red matter: red matter entities are killed by antimatter (`content/blast/redmatter/EntityRedmatter`; later sheets).
- The missile and cart forms: `03-missiles`, `explosive-block.md`.
- The EMP explosive is a separate action: [emp-tower.md](../05-detection-defense/emp-tower.md).

## 13. Bugs

ICBM-02-04, 02-05, 02-10, 02-11, 02-13, 02-14, 02-21, 02-26. The table is in `index.md`.

## 14. For a GTNH rebuild (notes, not part of the spec)

- The shape (energy rays with 50-100% random power, resistance-drained, recorded nearest-first) is cheap to port and good. The numbers (cost 1.125 per half step plus resistance) were tuned against vanilla's `blastResistance / 5`. GT blocks (Frames, machines, GT stone, concrete) report their own resistance values, so these tables will need a GT pass.
- Replace the worker thread's direct world reads with a snapshot, add a catch-all in the worker loop, deduplicate edits, and run the "post" effects when the queue drains.
