# Blast types

Compact per-blast sheets for the blasts that are plain classes with their own shape: condensed, repulsive and attractive (all `BlastTNT`), shrapnel, anvil and fragmentation (all `BlastShrapnel`), incendiary, sonic, breaching, exothermic and endothermic (the beam family), anti-gravitational and ender. The gas blasts are in [gas-and-status.md](gas-and-status.md), rot and mutation in [radioactive-and-mutation.md](radioactive-and-mutation.md), and the entities these blasts spawn in [effect-entities.md](effect-entities.md).

Source root for all paths: `src/main/java/icbm/classic/` at `9c1d985a`. The engine that runs these classes (the `Blast` base, ticking via `EntityExplosion`, worker threads, the block-edit queue, the registry and the defaults table) is `02-explosives/blast-engine.md` and `02-explosives/explosive-registry.md`; this file only restates what a sheet needs. Red matter and EMP are not here. Block-update flags, event buses and `Explosion` subclassing are *1.12-only mechanisms*; the sheets describe the behavior they produce.

## 0. Shared facts

| Fact | Value |
|---|---|
| Blast radius | `getBlastRadius()` is `max(3, size)`. Every blast whose factory passes a tiny size still works at radius 3. Ender overrides it to a fixed 20 |
| Factory size | The registry passes a per-blast config value as `size` (see section 9). For the shrapnel family it is the `fragments` count, for gas the cloud `size`, for the rest `scale` |
| Non-tickable blast | Runs once inside `doAction`. If `doExplode(-1)` returns true the blast is completed. Condensed, repulsive, attractive, breaching, incendiary, shrapnel, anvil, fragmentation and mutation are non-tickable |
| Tickable blast | `doAction` spawns an `EntityExplosion` (tracks the blast, see [effect-entities.md](effect-entities.md#5-explosion-controller-entityexplosion)) which calls `onBlastTick` each server tick; `doExplode` returns true to finish. Sonic, beam family, anti-gravitational, ender, gas, rot are tickable |
| Cancel hooks | `ExplosionEvent.Start` (cancelable, fired once in `doAction`). `ExplosionEvent.Detonate` is fired by `BlastTNT` only, with an empty list (ICBM-02-59) |
| Entity damage helper | `Blast.doDamageEntities(radius, power, destroyItem)` is shared by the TNT family. Entities within `radius` of the center take `(int)(((s*s + s) / 2) * 8 * power + 1)` where `s = (1 - distance / radius) * blockDensity` (vanilla exposure) and get knocked back by `s` along the outward unit vector. Items are skipped unless `destroyItem` is true. The search box is `2 * radius` plus 1 |
| Block damage | `block.onBlockExploded` after `dropBlockAsItemWithChance(chance 1 / radius)` when `canDropFromExplosion` allows it |
| World reads | Rays read `getBlockState` without a loaded-chunk check, so a large blast can load or generate chunks at its edge (vanilla does the same; see Known limits in [index.md](index.md)) |

## 1. Condensed, repulsive, attractive (`content/blast/BlastTNT.java`)

All three are the same class with different knobs, so one sheet.

| | Condensed | Repulsive | Attractive |
|---|---|---|---|
| Registry ID / tier | 0 / 1 | 6 / 1 | 7 / 1 |
| `size` passed | `condensed.energy_scale` = 6 | `repulsive.scale` = 2 (radius becomes 3) | `attractive.scale` = 2 (radius 3) |
| Entity damage | yes, `condensed.damage` = 10 | none | none |
| Entity push | none | `REPEL` | `ATTRACT` |
| "Destroy items" flag | no | set, but only read by the damage path, so inert | same |

**Rays (MIT-portable).** Permalink: [`BlastTNT.java#L118-L194`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/BlastTNT.java#L118-L194). It is the vanilla TNT algorithm with a configurable ray count.

```
raysPerAxis = 16
for every (xs, ys, zs) in 0..15 on the surface of the 16^3 cube        // 1,352 rays
    dir = normalize( (xs, ys, zs) / 15 * 2 - 1 )
    energy = R * (0.7 + 0.6 * random())          // R = blast radius
    p = center
    step = 0.3
    while energy > 0:
        if block at floor(p) is not air:
            energy -= (blockExplosionResistance + 0.3) * step
            if energy > 0: add that block position to the destroy list (once)
        p += dir * step
        energy -= step * 0.75                      // 0.225 per step
```

In open air a ray reaches 0.3 / 0.225 = 1.33 times its starting energy. Resistance comes from the block's `getExplosionResistance` (vanilla: the stored value divided by 5). `calculateDamage` is skipped on the client. The source comments call the shape asymmetric in hard blocks and say condensed digs "a 2x4 shaft with a few offshoots" (TODO in the source).

**Order inside `doExplode`.** (1) Build the destroy list. (2) Send one huge-explosion particle (size at least 2) or one large-explosion particle to nearby clients through the server world's counted-particle call. (3) Play the vanilla explode sound (volume 4). (4) Entities: `NO_PUSH` calls the damage helper with radius R, power = `damageToEntities`, destroyItem = the flag; otherwise `pushEntities(12, R * 4, type)`. (5) Fire the empty `Detonate` event. (6) For every listed position send one `PacketSpawnBlockExplosion` (range 256), then if the block is not air drop with chance `1 / R` and call `onBlockExploded`. Returns true.

**Push (`pushEntities`).** Region: cube of half-width 13 around the center (radius 12 plus 1); every `Entity` (items, arrows, flying blocks too) within 12 blocks. Let `u = distance / 12`.

| Type | Velocity added to the entity | Players |
|---|---|---|
| `REPEL` | `(1 - u) * force` along the outward unit vector | half |
| `ATTRACT` | `-u * force` (pull toward the center; strongest at the edge, zero at the center) | half |

`force = R * 4` = 12 at the default size 3. The velocity is added in blocks per tick, so a point-blank repulsive blast adds up to 12 blocks/tick (6 for a player). The push radius of 12 is fixed and does not follow the config scale (ICBM-02-58). The block pass still runs after the push, so both push blasts also dig a radius-3 crater.

**Config (`icbmclassic/blast`).** Category `condensed`: `energy_scale` (double, min 1, default 6) and `damage` (int, min 1, default 10). Categories `repulsive`, `attractive`: `scale` (double, min 1, default 2). Fuse times are part a.

**NBT.** `pushType` (int ordinal), `destroyItem` (bool), plus the base `callCount`, `explosionSize`, `blastExpEntId`. Loading indexes the enum by the stored int with no bounds check.

**Bugs.** ICBM-02-55, 02-58, 02-59, 02-60.

## 2. Shrapnel, anvil, fragmentation (`content/blast/BlastShrapnel.java`)

One class; the registry gives it a factory that builds the fragment entity ([`EntityFragments`](effect-entities.md#2-fragments-entityfragments)).

| | Shrapnel | Anvil | Fragmentation |
|---|---|---|---|
| Registry ID / tier | 1 / 1 | 5 / 1 | 8 / 2 |
| Config category | `shrapnel` | `anvil` | `fragmentation` |
| `fragments` (per axis) | 30 | 25 | 15 |
| Total entities (`fragments` squared) | 900 | 625 | 225 |
| `damage` | 11 | 30 | 11 |
| Fragment flags | critical, on fire (100 s) | anvil (renders as an anvil, anvil sound on landing) | explosive: `explosive_energy` = 1.5 (a flaming vanilla explosion where it lands or hits) |
| Grenade version | yes | yes | no (tier 2) |

**Fan (MIT-portable).** Permalink: [`BlastShrapnel.java#L16-L52`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/BlastShrapnel.java#L16-L52).

```
n = blast radius (= max(3, fragments))
step = 360 / n degrees
for yawIndex in 0..n-1:               // ceil(n) when n is not an integer
    for pitchIndex in 0..n-1:
        spawn fragment at (x, floor(y) + 1.5 - 0.1, z)  nudged by yaw (vanilla arrow offset)
        dir = ( -sin(yaw)cos(pitch), -sin(pitch), cos(yaw)cos(pitch) )
        scale each component of dir by an independent random() in [0,1)
        speed = 0.5 + 0.7 * random()          // 0.5 to 1.2 blocks/tick
        fragment.setHeading(dir, speed, inaccuracy 1)   // normalizes, adds gaussian * 0.0075
        spawn
```

Both angles run the full 360 degrees, so every direction is covered twice (once per equivalent angle pair) and the poles are dense. The per-component random scaling skews the directions toward whichever axis got the larger roll. Fragments start 1.4 blocks above the floor of the blast block. The method returns false, so the blast never completes (ICBM-02-57). Server only.

**Config.** Each category has `fragments` (int, min 1) and `damage` (int, min 0); fragmentation adds `explosive_energy` (float, min 0, default 1.5). Description in config: "total = fragments * fragments".

**Effect on the world.** No direct block damage. Anvil and shrapnel fragments only damage entities and disappear on landing; fragmentation fragments each trigger a flaming vanilla explosion of strength 1.5, up to 225 of them. What a fragment does in flight is in [effect-entities.md](effect-entities.md#2-fragments-entityfragments).

**Bugs.** ICBM-02-57, 02-71 to 02-73.

## 3. Incendiary (`content/blast/BlastFire.java`)

| | |
|---|---|
| Registry ID / tier | 2 / 1 |
| Config | category `incendiary`, key `scale` (double, min 1, default 14) = blast size |
| Blocks | places fire into air or replaceable cells; turns ice into air |
| Entities | none directly (the fire does the damage) |
| Sound | `explosionfire` (volume 4, pitch about 1) |

The ray loop is the TNT loop with two differences: the cube is `radius` rays per axis (not 16) and there is no destroy list. At every step, if the ray still has energy, a fire roll runs.

```
d = distance from the center to the step's block
chance = R - random() * d
if chance > 0.55 * d:
    if (replaceable or air) and fire can be placed here: place fire (flag 3)
    else if block is ice: set air
```

So the probability that a visited cell ignites is `min(1, R/d - 0.55)`: certain inside about 0.65 R, 45% at R, zero beyond 1.8 R. A ray can visit a cell several times, which raises the density. Solid blocks absorb ray energy (`resistance + 0.3` times 0.3) as in TNT, so fire stays on the near side of walls but burns through glass quickly. The blast completes in one call (returns true).

**Bugs.** None specific. Uses `Math.random()` for the roll and so ignores the world RNG.

## 4. Sonic (`content/blast/BlastSonic.java`)

| | |
|---|---|
| Registry ID / tier | 10 / 2 |
| Config | category `sonic`, key `scale` (double, min 1, default 15) = size and radius |
| Duration | ends when the blast tick count exceeds the radius: 16 ticks at the default |
| Blocks | removes blocks in an expanding shell; 10% of them become flying blocks |
| Entities | flings everything that is not a creative player |
| Sound | `sonicwave` at start (volume 4, pitch 0.7 +/- 0.14) |

**Phase 1, block search.** On tick 0 it starts a legacy worker thread ([`ThreadLargeExplosion`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/thread/ThreadLargeExplosion.java#L38-L133), engine area) with range = radius and energy = 2 x radius. That thread fires `ceil(pi * R)` x `2 * ceil(pi * R)` rays on a sphere; each ray has `energy * (0.5 to 1)` power, moves 0.5 block per step, pays 1.125 power per step plus the block's explosion resistance (0.25 for fluids), and records every block it still has power over. In open air that is at most `2R * 1 / 1.125 * 0.5 = 0.89 R`, about 13 blocks for R = 15, and shorter through dense blocks.

**Phase 2, shell (MIT-portable).** Permalink: [`BlastSonic.java#L46-L150`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/BlastSonic.java#L46-L150).

```
each tick:
    shell = max(1, tickCount)                       // grows 1 block per tick
    every 4th tick (param callCount % 4 == 0), and only once the thread has finished:
        if the result queue is empty: end the blast
        for each recorded position with distance <= shell:
            remove it from the queue
            if not air and hardness >= 0:
                if it is an ICBM explosive block: trigger it (no fire)
                10% chance: spawn a flying block (no velocity, default gravity)
                set the position to air (no drops)
    entities in a box of half-width 2 * tickCount (every Entity, not creative players):
        d = distance to center; push = max(0, 1 - d / R) * 3
        motionX/Z += outwardX/Z * push * random() * 0.2
        motionY   += |outwardY * push * random()|          // upward only, up to 3 blocks/tick
    finish when tickCount > R
```

**Consequences.** Because tick numbers are tested with the pre-increment counter and the blast finishes at tick R, the last block pass is at tick 12 (shell 13) for R = 15. If the thread has not finished by then nothing is removed at all (ICBM-02-54). The 10% flying blocks are spawned at rest, are then pushed by the wave like any entity, and land as blocks elsewhere, so they relocate rather than vanish. The pushed entity list includes dropped items and projectiles. The `hasShockWave` field and `setShockWave()` are never read (ICBM-02-56). Entities exactly at the center get a NaN velocity (ICBM-02-55); players' motion edits are not synced (ICBM-02-53).

**NBT.** Base only. A reload restarts the blast from the start (the thread state is not saved).

**Bugs.** ICBM-02-53 to 02-56.

## 5. Breaching (`content/blast/BlastBreach.java`)

A tunnel charge. It extends `BlastTNT` and replaces only the destroy-list step.

| | |
|---|---|
| Registry ID / tier | 11 / 2 |
| Config category `breaching` | `depth` 7 (int, min 1), `size` 1 (int, min 0; the hole is `2 * size + 1` square), `damage` 13, `energy` 15,376 (float), `energy_scale_distance` 0.25, `energy_cost_distance` 0.35 (= 1 - 0.65) |
| Entities | normal TNT damage helper with radius 3 (size is never set so the radius floor applies) and power `damage`; no push |
| Blocks | up to `(2 * size + 1)^2` straight lines of `depth` cells; a block is broken if the energy left is greater than its explosion resistance |
| Fallback | `depth <= 0` behaves as a plain TNT blast of radius 3 |
| Sound | an extra explode sound (volume 5) on top of the base blast sounds, three plays in all |

**Tunnel (MIT-portable).** Permalink: [`BlastBreach.java#L59-L171`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/BlastBreach.java#L59-L171).

```
direction = host direction field if set (explosive entity facing)
          else from the exploder's pitch: > 45 means DOWN (UP for a missile),
               < -45 means UP (DOWN for a missile),
               else the horizontal facing, flipped when it is EAST or WEST
          else DOWN if there is no exploder
for h in -size..size, w in -size..size:
    e = energy - energy * (h + w) * energyDistanceScale            // signed sum
    for i in 0..depth-1 while e > 0:
        cell = origin + direction * i, offset by (h, w) on the two perpendicular axes
        if cell is not air:
            if hardness < 0: stop this line
            cost = block.getExplosionResistance
            if cost < e: e -= cost; add cell to the destroy list        // otherwise the cell is skipped, e unchanged
        else:
            e *= (1 - energyCostDistance)                               // air costs 35% of what is left
```

Then the normal destroy pass runs (packets, drops, `onBlockExploded`). By the comment in the source the resistances are: planks 3, stone about 10, ICBM concrete 28, compact concrete 380, reinforced concrete 2,800 and obsidian 1,200. With the default energy a center line (e = 15,376) breaks five reinforced layers (5 x 2,800 = 14,000) and the whole depth of anything softer; the line at (h, w) = (+1, +1) starts at 7,688 and the line at (-1, -1) at 23,064 (ICBM-02-62).

**Quirks.** A block too tough for the remaining energy is skipped but the line continues behind it with no penalty (ICBM-02-61), so a thin obsidian wall in front of a soft room is passed through without being broken. The energy sum is signed, so one corner of the hole gets 50% more energy than the other (ICBM-02-62). The east/west flip is a workaround noted in the source.

**Bugs.** ICBM-02-59 to 02-62.

## 6. Beam family: exothermic and endothermic (`content/blast/BlastBeam.java` and subclasses)

A slow staged blast with a light beam entity. The base class drives the stages; subclasses supply the beam color and what happens to each block.

| | Exothermic | Endothermic |
|---|---|---|
| Registry ID / tier | 17 / 3 | 18 / 3 |
| Config | category `exothermic`, key `scale` (double, min 1, default 30) | category `endothermic`, key `scale` (default 30) |
| Beam color (r, g, b) | 0.7, 0.3, 0 | 0, 0.3, 0.7 |
| Time of day | `allow_day_night_switch` and `doDaylightCycle`: set world time to 18,000 (midnight) | same gates: set to 1,200 (morning) |
| Entity effect | none | every living `EntityLiving` (not players) in a cube of half-width R: Poison III for 10 s, Mining Fatigue III for 120 s, Slowness V for 120 s |

**Stages (server only).**

| Tick | What happens | Beam target progress |
|---|---|---|
| 0 | Play `beamcharging` (volume 4, pitch 0.8). Create a vanilla explosion of strength 4 with fire (`isFlaming` true, for both types). Spawn the `EntityLightBeam` at the center (beam size 1, glow 2) | 0.1, then 0.2 |
| 0 | Start the first worker task: collect every solid breakable block within `max(5, R / 10)` (a sphere; 5 for the default R = 30) | 0.2 |
| when the first task is done | For each collected block: spawn a flying block (gravity 0.04, no velocity) and set the cell to air. Start a 100-tick wait | 0.5 |
| after the 100 ticks | Start the second task: collect every solid breakable block within R (the full sphere, about 113,000 positions at R = 30) | 0.8 |
| when the second task is done | On the main thread, in one tick, run `mutateBlocks` over the whole list | 1.0 |
| next tick | Play `powerdown`, start the beam's death cycle, target 0, finish | 0 |

"Solid breakable" means not air and block hardness >= 0.

**Exothermic `mutateBlocks`.** Per position at distance `d` from the center, `radiusDecay = max(1, 0.3 R)`, `radiusEnsured = max(1, 0.1 R)`, `scale = 1 - d / R`:

1. Water or ice becomes air, always.
2. The rest runs if `d <= radiusDecay` or `random() < scale`:
   - Leaves, vine or plant material: air, or fire when it is replaceable and fire cannot be placed (the condition is inverted, ICBM-02-48).
   - Rock: 10% lava (flowing, flag 3); else 50% magma placed on the block *below* the target; else netherrack if a coin flip succeeds or `d <= radiusEnsured`. Netherrack gets a 50% fire above it.
   - Sand: 50% soul sand on the block *below*, else netherrack (with fire chance).
   - Dirt or grass material: netherrack (with fire chance).
   - Then with probability `scale` a fire is tried on the cell above.

**Endothermic `mutateBlocks`.** Same gate. Water becomes ice; fire becomes air; lava becomes obsidian; flowing lava becomes a snow layer with `max(1, min(8, level / 2))` layers; magma becomes stone; netherrack becomes dirt; soul sand becomes sand or gravel (coin flip); ground or grass material becomes ice or a full snow block (coin flip). Then with probability `scale` a snow layer of 1 to 7 is tried above (needs a replaceable cell and a solid top face below).

**Config shared.** `allow_day_night_switch` (bool, default true) in `icbmclassic/blast` gates the time change.

**MIT-portable.** The mutate tables above are the specification. Permalinks: [`BlastExothermic.java#L22-L110`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/BlastExothermic.java#L22-L110), [`BlastEndothermic.java#L30-L109`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/BlastEndothermic.java#L30-L109), stage machine [`BlastBeam.java#L55-L133`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/BlastBeam.java#L55-L133).

**Quirks.** The flying blocks of stage 3 are never recorded, so the intended "heavy gravity" step does nothing (ICBM-02-44). Workers read the world from another thread (ICBM-02-45). Nothing is saved, so a reload restarts the whole sequence and leaves the old beam for up to 5 minutes (ICBM-02-46). The initial vanilla explosion burns terrain even for the ice bomb (ICBM-02-49). Both mutate methods use `Math.random()` instead of the world RNG.

**Bugs.** ICBM-02-44 to 02-49.

## 7. Anti-gravitational (`content/blast/BlastAntiGravitational.java`)

| | |
|---|---|
| Registry ID / tier | 19 / 3 |
| Config | category `antigravitational`, key `scale` (double, min 1, default 30) = size |
| Duration | `RUNNING_TICKS` = 2,400 ticks (2 minutes), not configurable |
| Blocks | lifts one block per tick, top layers first; they float with zero gravity |
| Entities | gets +0.15 upward velocity per tick while below 0.4 |
| Sound | none (the `antigravity` sound call is commented out) |

**Setup.** On the server it starts a legacy small-rays thread ([`ThreadSmallExplosion`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/thread/ThreadSmallExplosion.java#L24-L91), engine area) with radius R and energy 0: the TNT-style cube-surface rays, where each ray pays only the block's full explosion resistance (no per-step cost), stops at unbreakable blocks, and records every cell it still has power over. Air cells are recorded too, and cells are recorded once per ray, so the list is full of air and duplicates.

**Each tick after the thread finishes.**

1. First time only: copy the results, shuffle, then stable-sort by descending Y. Empty the original queue.
2. Walk the list from the saved index (wrap to 0 at the end) until one flying block spawns; stop after that one. So at most one block per tick, 2,400 per blast. The spawn is skipped for air, unbreakable, fluid or banned blocks.
3. Each flying block: `yawChange` random up to 50 and `pitchChange` up to 100 (spin), `motionY += 1` (the source takes `max(random, 1)`, always 1: ICBM-02-41), horizontal velocity of `outwardUnit * (1 - random())`, gravity 0, in-air kill time 4,800 ticks.
4. Entities: a box meant to span `+/- R` in X and Z and from `-R` to `+max(R, 100)` in Y; any entity with `posY < y + max(R, 100)` and `motionY < 0.4` gets `motionY += 0.15`. The box is built wrongly (ICBM-02-40).
5. Finish when the tick count exceeds 2,400. Then every remembered flying block gets its default gravity (0.05) back and falls.

Flying blocks move through the air with 0.99 drag, so a block launched at 1 block/tick climbs about 100 blocks, stays there for the rest of the blast, and falls when gravity returns. A block that is still in the air when its in-air timer ends, or climbs above y = 400, is placed where it is ([effect-entities.md](effect-entities.md#1-flying-block-entityflyingblock)).

**Bugs.** ICBM-02-40 to 02-43, 02-53, 02-70.

## 8. Ender (`content/blast/ender/BlastEnder.java`, `EnderBlastCustomization.java`)

| | |
|---|---|
| Registry ID / tier | 20 / 3 |
| Config | category `ender`, key `scale` (default 30), **ignored**: the radius is fixed at 20 (ICBM-02-51) |
| Duration | `duration` = 160 ticks (8 s); not configurable, not saved |
| Blocks | none |
| Entities | pulled to the center, teleported when within 4 blocks |
| Sound | `portal.ambient` every tick (volume 2, pitch 0.8 to 1.2); enderman teleport sound per teleport |
| End | spawns 8 endermen at the center |

**Per tick.**

1. Pull every `Entity` in the 20-block cube except the controller entity, per axis: `motion -= (sign * 20 - delta) * |delta| * 0.0006` for X and Z (0.0011 for Y with the sign flipped). The pull is zero at the center and at 20 blocks, strongest at 10 blocks (0.06 blocks/tick per axis for X and Z, 0.11 for Y).
2. Any entity within 4 blocks of the center is teleported. If no target is set yet, pick one once for the whole blast: X and Z random within +/-150 of the *controller entity's* position, Y at the controller's Y (plus the broken air-gap search, ICBM-02-50). Then play the teleport sound and move the entity; players use the connection's position call, others `setPosition`. Every entity goes to the same point.
3. Once every 5 ticks, a huge-explosion particle call at one teleported entity (server side, so not visible).
4. The client branch (portal particles on the inner shell) never runs (ICBM-02-52).

**Targeting item.** The ender explosive block and missile accept a GPS-style tool (`ICBMClassicHelpers.getGPSData`): the tool writes an `EnderBlastCustomization` with a position and a dimension id onto the explosive. When the blast is built the position becomes the teleport target. The dimension is saved, shown in the tooltip, and never used (ICBM-02-51). Customization NBT: `pos` (vec3d), `dim` (int). Tooltip lang: `explosive.icbmclassic:ender.pos` = "Target: %1$sx, %2$sy, %3$sz", `explosive.icbmclassic:ender.world` = "World: [%1$s] %2$s". Chat on set: `chat.launcher.toolTargetSet` / `chat.launcher.noTargetInTool`, neither is in `en_us.lang`.

**Bugs.** ICBM-02-50 to 02-53, 02-79.

## 9. Config keys by blast

All in file `icbmclassic/blast` (`ConfigBlast`). Categories use the Java field name (no `@Name`).

| Category | Keys (defaults) |
|---|---|
| `condensed` | `energy_scale` 6, `damage` 10 |
| `shrapnel` | `fragments` 30, `damage` 11 |
| `anvil` | `fragments` 25, `damage` 30 |
| `fragmentation` | `fragments` 15, `damage` 11, `explosive_energy` 1.5 |
| `incendiary` | `scale` 14 |
| `repulsive`, `attractive` | `scale` 2 |
| `sonic` | `scale` 15 |
| `breaching` | `depth` 7, `size` 1, `damage` 13, `energy` 15376, `energy_scale_distance` 0.25, `energy_cost_distance` 0.35 |
| `exothermic`, `endothermic`, `antigravitational`, `ender` | `scale` 30 each (ender's is unused) |
| `colorful` | `scale` 10 (gas, see [gas-and-status.md](gas-and-status.md)) |
| top level | `blast_do_block_updates` false (engine), `allow_day_night_switch` true |

The gas cloud categories (`debilitation`, `chemical`, `contagious`) and the nuclear scales for rot and mutation are in their own sheets.

## 10. Audio catalog (this sheet)

| Sound event | File | Size | Used by |
|---|---|---|---|
| `beamcharging` | `sounds/beamcharging.ogg` | 24,792 bytes | beam family start |
| `powerdown` | `sounds/powerdown.ogg` | 115,835 bytes | beam family end |
| `explosionfire` | `sounds/explosionfire.ogg` | 58,976 bytes | incendiary |
| `sonicwave` | `sounds/sonicwave.ogg` | 110,070 bytes | sonic |

Durations were not measured. Subtitles: `subtitle.icbmclassic:beam.charge` = "Energy Beam Start", `subtitle.icbmclassic:explosion.fire` = "Fire Explosion", `subtitle.icbmclassic:explosion.sonicwave` = "Sonic Blast". Full audio and texture catalogs are `07-visuals-audio`.

## 11. Lang

| Key | English |
|---|---|
| `explosive.icbmclassic:<id>.name` | "<Condensed, Shrapnel, Incendiary, Anvil, Repulsive, Attractive, Fragmentation, Sonic, Breaching, Exothermic, Endothermic, Ender> Explosives"; "Anti-Gravitational Explosives" for `antigravitational`; "Colorful Explosives" for `colors` |
| `.info` | condensed "Slightly more destructive version of TNT"; shrapnel "Erupts in a barrage of metal fragments"; incendiary "Catches the place on fire"; anvil "What's up doc"; repulsive "Pushes everyone away"; attractive "Pulls everyone in"; fragmentation "Devastates the landscape with high velocity fragments"; sonic "Who needs to be a ninja to sneak in after that much noise?"; breaching "When you need a door"; exothermic "You thought the sun was hot"; endothermic "They call me Mr. Freeze"; antigravitational "I think your base looks better floating"; ender "Let's relocate that base of yours" |

Missile, bomb cart, grenade and bomblet variants of each name exist as parallel keys and belong to their own areas.
