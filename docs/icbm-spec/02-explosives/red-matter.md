# Red Matter

Source root for all paths: `src/main/java/icbm/classic/` at `9c1d985a`. Red matter is the one explosive that is not a blast: it spawns a long-lived **entity** that pulls everything toward itself, eats blocks in a growing ball, and has no built-in end. This sheet is written for a performance test, so section 4 (Lifecycle) and section 5 (Performance budget) carry numbers. Everything is derived from reading the source and arithmetic on the config defaults; **nothing was run or measured**, and estimates are labeled as such.

Related: the registry row (ID 23, tier 4, fuse 100) is in [explosive-registry.md](explosive-registry.md); the shared blast machinery, the `IBlastVelocity` capability and the entity-damage routine in [blast-engine.md](blast-engine.md); the flying-block entity this sheet spawns in [effect-entities.md](effect-entities.md); antimatter (which kills red matter) in [threaded-blasts.md](threaded-blasts.md). Index: [index-c.md](index-c.md).

## 1. Identity

| Item | Value |
|---|---|
| Classes | `content/blast/redmatter/ActionSpawnRedmatter` (the spawn action), `EntityRedmatter` (the entity), `DamageSourceRedmatter`; `logic/RedmatterLogic` (all server behavior), `logic/RedmatterBlockCollector` (shell positions); `caps/BlastRedmatterWrapper` (deprecated `IBlast` facade), `caps/CapRedmatterPull` (red-matter-on-red-matter pull); `render/RenderRedmatter`, `render/RedmatterClientLogic` (client only); config `config/blast/types/ConfigRedmatter` |
| Registry names | entity `icbmclassic:redmatter` (`ICBMEntities.REDMATTER`, tracker range 500, update interval 1, velocity updates on: `content/reg/EntityReg.java:60,86`); explosive `icbmclassic:redmatter` (registered as `redMatter`, ID 23, tier FOUR: `content/reg/ExplosiveInit.java:254-256`) |
| English names | explosive block "Red Matter Explosives" (`explosive.icbmclassic:redmatter.name`), missile "Red Matter Missile", minecart "Red Matter Minecart". The entity has **no** `entity.*.name` lang key; its display name comes from the registry string |
| Entity size | bounding box 0.98 x 0.98, `noClip`, `preventEntitySpawning`, `ignoreFrustumCheck` (renders at any angle), cannot be collided with, does not trigger walking. The box never changes with blast size |
| Creative tab / oredict | none for the entity; the explosive items are in the ICBM tab (`06-blocks-items`) |
| 1.12-only mechanisms | The two capabilities (`ICBMClassicAPI.BLAST_CAPABILITY`, `BLAST_VELOCITY_CAPABILITY`) the entity answers, the `DataParameter` pair, the `EntityEntry` registration. Behavior they carry is described below |

## 2. Obtaining

The entity is never an item. It exists only after an explosive of type `redmatter` fires. Three carriers (recipes belong to `06-blocks-items`; summarized here):

| Carrier | Recipe | Fuse |
|---|---|---|
| Explosive block | `nnn / nrn / nnn`, n = nether star (atomic alloy if Mekanism is present), r = an antimatter explosive block ([explosive-registry.md](explosive-registry.md) row 137) | `fusetimes.EXPLOSIVES.REDMATTER` = 100 ticks |
| Bomb cart | red matter explosive over a vanilla minecart (`recipes/bombcarts/redmatter.json`) | `fusetimes.BOMB_CARTS.REDMATTER` = 100 ticks |
| Missile | a missile module and a red matter block side by side (`recipes/missile/ex/redmatter.json`); flight is `03-missiles` | n/a |

Tier FOUR gets the block, cart and missile forms and **no grenade**; the lang key `bomblet.icbmclassic:redmatter.name` ("Red Matter Bomblet") is an unused leftover. Dev spawn: `/icbm blast trigger redmatter <x y z> [size]` (`command/sub/blast/CommandBlastTrigger.java:152`) passes the size as `AREA_SIZE`, the only way to spawn bigger or smaller than the default (see ICBM-02-84).

## 3. Numbers

All keys are in `config/icbmclassic/blast.cfg`, group `redmatter` (`ConfigBlast.java:95-97`; field names are `ConfigRedmatter`). No key has a range except `damage` (min 0) and `STARVE_SCALE` (0 to 1).

| Key | Default | Meaning |
|---|---|---|
| `redmatter_default_size` | 1 | spawn size when no size is given |
| `redmatter_max_size` | 70 | cap on the **scan radius** (not on size, see ICBM-02-82, 02-84) |
| `redmatter_min_size` | 0.25 | floor for `setBlastSize`; at or below it a starving body dies |
| `redmatter_scale_gravity` | 2 | gravity reach = size x this |
| `redmatter_scale_kill_radius` | 0.08 | kill reach = max(1, size x this) |
| `redmatter_scale_rendering` | 0.05 | client sphere radius = size x this (min 0.1) |
| `redmatter_starve_multiplier` | 0.99 | mass kept per starve tick (bodies of size 1 or more) |
| `damage` | 2000 | damage per tick to a living entity inside the kill radius |
| `redmatter_max_edits_per_tick` | 100 | block removals per tick (really 101, ICBM-02-90) |
| `redmatter_raytrace_per_tick` | 500 | ray traces per tick (really 501) |
| `redmatter_flying_block_spawn_chance` | 0.8 | read inverted: the real chance is 0.2 (ICBM-02-83) |
| `redmatter_spawn_flying_blocks` | true | master switch for debris entities |
| `redmatter_audio_enabled` | true | master switch for the two server-played sounds |
| `redmatter_colored_beams` | true | client: random-hue beam tips |
| `REDMATTER_MOVEMENT` | true | **dead**: the `@Config` annotations are commented out and nothing reads it |

Constants in code: entity speed clamp `MAX_SPEED` 0.5 blocks/tick, drag `SPEED_REDUCTION` 0.98 per tick (`EntityRedmatter.java:31-32`); flying-block growth +0.05 per block eaten (`RedmatterLogic.java:491`); shrink factor 0.9 below size 1 (`:156`); pull factor 0.01 (`:425`).

Fuse keys: `icbmclassic/blast` group `FUSE_TIMES`, `EXPLOSIVES.REDMATTER` and `BOMB_CARTS.REDMATTER`, both 100.

## 4. Behavior

Terms. **size** = the synced float `blast_size` (what gravity, kill range and rendering read). **R** = the integer scan radius `currentBlockDestroyRadius`. **T(R)** = `6 x (2R+1)^2`, the number of ray targets in one cycle. A **cycle** = one pass over all T(R) targets. **B** = blocks removed this cycle. **cs** = `cyclesSinceLastBlockRemoved`, starting at -1. Server-only unless stated.

### 4.1 Lifecycle

**Spawn.** `ActionSpawnRedmatter.doAction` (`ActionSpawnRedmatter.java:37-54`) builds the entity at the action position, sets size to `redmatter_default_size`, max to `redmatter_max_size`, then overrides size with the `AREA_SIZE` field when given, and calls `spawnEntity`. Failure returns `ENTITY_SPAWN_FAILED`, success `COMPLETED`. The action source is ignored: no cause is stored on the entity (the damage source has a TODO for it), no event is fired, no protection hook runs. `setBlastSize` floors the size at 0.25 and has **no upper limit**; `setBlastMaxSize` stores the value as is. Both are synced to clients as data parameters.

**Tick order** (`EntityRedmatter.onUpdate:65-82`): vanilla `Entity.onUpdate`; if any motion component is nonzero, damp by 0.98, renormalize, clamp speed to 0.5, and move with no collision; then, on the server only, `RedmatterLogic.tick()`. Within `tick()` (`RedmatterLogic.java:77-130`):

1. `preTick`: zero this tick's ray and edit counters. On the first tick only, R = floor(size) (unclamped, so a size above max starts R above max; the next step fixes it).
2. `detectAndDestroyBlocks`: if R > max, reset R from the size; if the target queue is empty, start the next cycle; then drain the queue until a budget is hit.
3. `doEntityEffects`: one entity scan, pull and attack (4.3).
4. Audio (if enabled): 1-in-8 chance of a collapse sound at a random point in a cube of half-edge `size` around the body, volume 5 to 6, pitch 0.6 to 1; **every tick** the `redmatter` sound at the body, volume 3, pitch uniform in -0.8 to 1.2 (ICBM-02-93).
5. `postTick`: if B is 0 (nothing removed yet this cycle), call `decreaseScale`.

**Growth and the scan radius.** At the start of each cycle (`startNextBlockDestroyCycle:194-221`):

1. If cs is above 0: cs = 0, R = R + 1 (clamped to max, minimum 1) **and size is set to the unclamped R + 1 as an integer** (`setCurrentBlockDestroyRadius:498-502`).
2. Clear the queue, collect all T(R) positions of the six faces of the cube of half-edge R around the origin (a face is `(2R+1)^2` points; edges and corners are listed up to three times), shuffle.
3. If B is 0, cs = cs + 1. Then B = 0.

Removing any block sets cs to 0. Consequences, worked through from the code:

- Starting from cs = -1, an empty cycle takes cs to 0, then to 1; from then on **every empty cycle grows R by 1**. After a productive cycle, two empty cycles pass before growth resumes.
- cs can never exceed 1, because growth resets it to 0 and the increment is once per cycle.
- Size is not integrated over time; it is **overwritten with R + 1 at each growth step**. Mass gained from eaten debris (+0.05) or from merging is discarded at the next growth step (ICBM-02-82).
- At R = max (70), growth keeps firing each cycle but R stays 70 and size is rewritten to 71, so the body settles at **size 71, above the configured max of 70**.

**Block consumption** (`cycleDestroyBlocks`, `rayTraceTowardsBlock`, `processNextBlock`: `:182-298`). Loop while the tick's budgets are not exceeded and the queue is not empty: count a ray, pop a target, add the body's position to it, and trace from the body's center toward it with the world's block ray trace (stop on liquid, do not ignore boxless blocks, do not return the last uncollidable). The **first** block hit is the candidate. It is eaten only if its center is within `R + 1` of the body's center (so the rays carve whatever is nearest in each of T(R) directions, not a thin shell). A ray removes at most one block per pass; the same direction is tried again next cycle.

Candidate rules (`shouldRemoveBlock:327-335`): not air, and either a "flowing water" test passes or (not a fluid and hardness is 0 or more). The "flowing water" test is any vanilla liquid block (water **and lava**) whose level is below 7. In practice rays can only hit liquid *source* blocks because of how the stop-on-liquid flag works in the vanilla trace (ICBM-02-89). Bedrock and other unbreakable blocks (hardness -1) are never removed and also block the ray. There is no blast-resistance check at all: obsidian costs the same as dirt. Forge-style fluid blocks are never removed.

On removal (`:281-295`): set the block to air with flag 3 (notify neighbors and clients). No drops, no break event, no protection check. A tile entity's contents are gone. If the removed block was vanilla water: turn each of its six water neighbors into ice, which is **not** counted against the edit budget (ICBM-02-88). If the block is not a fluid and a random roll passes, spawn a flying block (4.4). Finally B, the per-tick edit counter, and cs = 0 are updated.

**Budgets stop the loop** (`shouldStopBreakingBlocks:255-260`): more than 500 rays this tick, more than 100 edits this tick, or the body is dead. Strict "greater than" gives 501 rays and 101 edits (ICBM-02-90). The leftover queue carries over to the next tick.

**Starvation** (`decreaseScale:133-160`). Called each tick while B is 0. It shrinks only if `size <= cs`. With cs at most 1, this is true only for a body of size 1 or less (and cs = 1). Then: if size is at or below the 0.25 floor, set size to 0, kill the body and log "Redmatter[id] has starved to death at x y z dim"; otherwise multiply size by 0.9 (size below 1) or by the starve multiplier 0.99. In every real scenario the next growth step (to R + 1 of at least 2) overwrites that shrink. Only a body spawned at size 0.25 or less dies, on its second tick. **A body of any useful size never starves** (ICBM-02-81).

**Merge** (`attackEntity:448-467`). A second red matter inside the kill radius: new size = cube root of (size^3 + other^3); the host's motion is zeroed; the other is killed. Not capped by max (ICBM-02-84). Because the scan radius is not touched, the merged mass is later overwritten (ICBM-02-82).

**Death.** There is no timer and no cap on lifetime. The body dies when: starved (above), killed by an antimatter blast within 221 blocks (ICBM-02-26), `clearBlast` on the facade (which a remove command can call; not checked), the vanilla void rule (y below -64), or `/kill`. It does **not** die when its chunk unloads: it stops ticking, and on reload it is rebuilt with the wrong size (ICBM-02-80).

### 4.2 Motion

A red matter is only moved by other red matters. Each pull adds velocity toward the other (4.3); the body then glides with 0.98 drag and a 0.5 blocks/tick cap, with no collision. The renormalize step divides by the speed with no zero check: after about 5,100 ticks (4 minutes) of coasting from 0.5, the float speed underflows to exactly 0 and the motion becomes NaN (ICBM-02-86). Other blasts that push entities also move it.

### 4.3 Entity effects (`doEntityEffects`, `moveEntity`, `attackEntity`)

Each tick: one box query for **all** `Entity` instances (vanilla excludes spectators) in a cube of half-edge `2 x size` centered on the body. Skip: itself; players in creative or with `disableDamage` (the latter also covers spectators); anything implementing `IBlastIgnore` that says it ignores this blast. Every other entity gets the pull then the attack check.

**Pull.** If the entity's `BLAST_VELOCITY_CAPABILITY` handles it (only red matter), use that. Otherwise, with `d` = center distance and `range = 2 x size`:

```
scale = max(0, 1 - d / range)
power = min(1, scale x size x 0.01)
entity.addVelocity(direction_to_body x power); mark velocity changed (resend to clients)
```

That is up to `size / 100` blocks/tick of added velocity per tick, no speed cap (a TODO in the code). At size 71 and 10 blocks away: 0.65 per tick. Red-matter-on-red-matter pull (`CapRedmatterPull`): direction to the source, magnitude `own^3 / (own^3 + source^3)` (each cube floored at 1) added to the **pulled** body, so the larger body is pulled harder (ICBM-02-85).

**Attack**, when `d` is under `max(1, 0.08 x size)`, first match wins:

| Entity | Result |
|---|---|
| another red matter that is alive | merge (above) |
| `EntityExplosion` (a ticking blast's controller) with a blast | clear that blast |
| anything with the explosive capability (primed TNT, bomb cart, etc.) | create and run that explosive at its position with this red matter as the cause, then kill the carrier. A red matter explosive spawns a new size-1 red matter, which merges next tick |
| any living entity | `attackEntityFrom(DamageSourceRedmatter, 2000)` every tick. The source bypasses armor and is absolute. Bosses that route damage through parts (the ender dragon) are not harmed |
| anything else | `setDead`; if it was a flying block and size is under max, size + 0.05 |

`DamageSourceRedmatter` is named `icbm.redmatter`; the death message `death.attack.icbm.redmatter` = "%s was crushed by the immense mass of the mighty black hole." Its true source is the body.

### 4.4 Flying-block conversion (`spawnFlyingBlock:342-349`)

For each removed non-fluid block, a random float is drawn and the block becomes debris **if the float is greater than 0.8**, so 20% of removed blocks (ICBM-02-83), if `redmatter_spawn_flying_blocks` is on and the global flying-block config allows the state (banned: fire, any fluid, listed states; default ban list contains `minecraft:fire` and the dynamictrees mod). The entity (`EntityFlyingBlock`, see [effect-entities.md](effect-entities.md)) carries the block state, a stack and tile NBT, is placed at the block center, gets random spin values up to 50, is flagged `noClip` (which `EntityProjectile` never reads: ICBM-02-94), and is handed once to the red matter's own pull/attack routine right after spawning. After that the red matter's per-tick scan pulls it like anything else.

Debris is eaten silently (dies in the kill radius, no drop). Debris that reaches the ground more than 5 ticks after spawning places its block back or drops its item; debris that lives to its 1,200-tick flight limit places its block mid-air at its position. Debris leaving above y = 400 does the same.

### 4.5 Client behavior

`RedmatterClientLogic.lerpSize` runs once per rendered frame at the end of `doRender`: `visual += partialTicks x (size - visual)`, starting from 0 for each client's copy. The easing is frame-rate dependent and the body scales in from nothing when first seen (ICBM-02-98).

`RenderRedmatter` draws three layers at the entity position each frame (no model file; all procedural):

1. **Disk.** A flat square in the horizontal plane, half-edge `max(0.2, 0.15 x visual)` (that is, three times the sphere scale), textured with `textures/entity/redmatter/blackhole.png` (32 x 32), tinted pure red, blended, unlit, drawn on both faces (the second pass is flipped). It spins about the vertical axis at 1 degree per tick (`-ticksExisted`, not interpolated).
2. **Sphere.** An opaque black inner ball of radius `0.8 x r` where `r = max(0.1, 0.05 x visual)`, lit, with the grey texture; and over it a translucent (alpha 0.8) very dark blue ball of radius `r` plus a pulse. The pulse is a 40-tick triangle wave peaking at 1% of `r`, so it is nearly invisible. Both balls are GLU spheres of 32 x 32 segments, a new quadric object per ball per frame, in immediate mode (ICBM-02-97).
3. **Beams.** Up to 30 thin spikes radiating from the center, an adaptation of the ender dragon's death light: a fan from the center to three points at the tip, length `(1 to 2) x visual`, width `max(0.1, random) x visual / 10`, random orientation from a seed fixed at 432 (so the pattern is stable), additive blend, unlit, no texture. Count = `(t + t^2) / 2 x 30` with `t` a triangle wave over 4,000 ticks (rises for 2,000 ticks, falls for 2,000); the whole beam set also rotates once per 200 ticks. Center color is white; tip color is a cached random-hue (saturation 0.9, brightness 0.5) color per beam index, or black when colored beams are off. The color cache is a static list capped at 30.

At full size (visual 71) the sphere is 3.5 blocks in radius, the disk 21 blocks across and the beams up to 142 blocks long.

## 5. Performance budget

Per red matter, per server tick, at the default config. "Order of" means a bound derived from the code, not a measurement. The vanilla-engine claims (trace step cap, packet thresholds) are from knowledge of 1.12 and were not checked against a decompile.

| Work | Per-tick bound | Notes |
|---|---|---|
| Ray traces | **501** at most (config 500 + 1) | each a vanilla block ray trace from the center to a point `R` to `1.73 R` away; the vanilla trace is capped at about 200 block steps, so at R = 70 a trace costs up to about 120 block lookups. Empty space is the worst case: all 501 are used |
| Block removals | **101** at most (config 100 + 1) | each `setBlockState(…, 3)`: lighting update, neighbor notify, client packet. Hits at the sphere edge are not counted until removed |
| Extra edits from water | up to **6 per removed water block**, so up to 606 | ice replacement, not counted against the 101 (ICBM-02-88) |
| Tile NBT writes and item-stack builds | up to 501 | a capture-data object is built for **every ray hit inside the radius check**, even when the block is not removed; it reads the block item and writes the tile entity's full NBT (ICBM-02-92). For machine-heavy GTNH bases this is the biggest hidden cost |
| Flying-block spawns | average 0.2 per removed non-fluid block, at most 101; **about 20 per tick average at full speed** | each is a full entity with NBT spawn data to every tracking client |
| Live debris entities | rate x lifetime. Lifetime is short (about 10 to 20 ticks, estimated, when the debris falls into the kill radius) up to **1,200 ticks** worst case | at 20 per tick, a few hundred live at once is expected; the hard bound is about 24,000 |
| Entity scan | **1 box query per tick**, edge `4 x size` | size 2: 8 blocks; 11: 44; 21: 84; 51: 204; 71: 284 (about 19 x 19 chunk columns). Cost is the entity count inside it, every class |
| Per entity in the box | one capability lookup, one velocity add, one velocity packet if it is tracked; distance and attack test | no early-out for far entities beyond the radius test; no cap on count |
| Target-queue rebuild | once per cycle: **T(R) = 6(2R+1)^2** positions allocated and shuffled | R = 10: 2,646; 30: 22,326; 70: **119,286**. Each is a position object plus a list node, about 48 bytes (estimated), so about 5.7 MB of short-lived garbage per cycle at R = 70, plus a shuffle copy |
| Queue memory | up to T(max) entries held | default max: about 5.7 MB per red matter. `redmatter_max_size` has no range: at 500 the queue is 6 million entries, about 290 MB per red matter, and one cycle takes about 12,000 ticks |
| Sound packets | 1 per tick + 1 in 8 per tick | to every player within 48 blocks (volume 3) and 80 to 96 (volume 5 to 6); the 1.01 s `redmatter` clip is started every tick, so 20 copies overlap per player (ICBM-02-93). Disable with `redmatter_audio_enabled` |
| Entity tracker | tracking range 500, every tick, with velocity | the body's size parameter is re-sent only on change; it changes at each growth step |
| Loop bounds | none unbounded | the only `while` is the ray loop, bounded by 501 and by the queue. No recursion. No growth of any map or list across cycles: the queue is cleared each cycle, and the static beam color list on the client stops at 30 |

### Curve: how big, how fast

Rows use size = R + 1 (the value after a growth step), the default config, and the empty-space cycle length `ceil(T / 501)`. "Solid" is a lower bound on the time to clear the whole ball if every cell were stone, `4/3 pi (R+1)^3 / 101`.

| R | size | T(R) | ticks per empty cycle | gravity reach | kill radius | pull at 10 blocks | ball volume | ticks to clear if all solid |
|---|---|---|---|---|---|---|---|---|
| 1 | 2 | 54 | 1 | 4 | 1 | 0 (out of reach) | 34 | under 1 |
| 5 | 6 | 726 | 2 | 12 | 1 | 0.06 x 0.17 = 0.01 | 905 | 9 |
| 10 | 11 | 2,646 | 6 | 22 | 1 | 0.11 x 0.55 = 0.06 | 5,575 | 55 |
| 20 | 21 | 10,086 | 21 | 42 | 1.7 | 0.21 x 0.76 = 0.16 | 38,792 | 384 |
| 30 | 31 | 22,326 | 45 | 62 | 2.5 | 0.31 x 0.84 = 0.26 | 124,788 | 1,235 (1 min) |
| 50 | 51 | 61,206 | 123 | 102 | 4.1 | 0.51 x 0.90 = 0.46 | 555,647 | 5,501 (4.6 min) |
| 70 | 71 | 119,286 | 239 | 142 | 5.7 | 0.71 x 0.93 = 0.66 | 1,499,214 | 14,843 (12.4 min) |

- **Void ramp.** With nothing to eat, R climbs 1 per cycle and the sum of the cycle times from R = 1 to 70 is about **5,500 ticks (4.6 minutes)**. The body then sits at R = 70, size 71, running 239-tick empty cycles forever.
- **Hard rate ceiling.** 101 removals per tick = **2,020 blocks per second**, whatever the terrain; and 501 traces per tick = 10,020 per second.
- **Dense terrain.** Every trace hits a nearby block and the 101-edit cap binds first (about 101 traces per tick), so a cycle is `T / 101` ticks (R = 30: 221 ticks).
- **Mixed terrain** sits between the two; the sphere is eaten inside-out, one nearest block per direction per cycle, so tunnels widen as a cone before the ball empties.

### Worst case lifetime and steady state

- **Lifetime: unbounded.** Starvation is unreachable for any body of size above 1 (ICBM-02-81), merging removes the cap on size (ICBM-02-84), and nothing else expires the entity. Only antimatter, a remove command or `/kill` ends it.
- **Steady state after the ball is clear: not idle.** At R = 70 it still runs 501 traces per tick (all of them empty-space traces), one 284-block entity scan, a rewrite of the 119,286-entry queue every 239 ticks, and 1.125 sound packets per tick, forever. The cost of a finished red matter is about that of one actively eating in sparse terrain.
- **Reload.** A saved and reloaded red matter comes back at size = its max (70) regardless of its old size, and with the scan radius restarted at 70 on its first tick (ICBM-02-80). Do not stop the server mid-test unless that is the goal.
- **Unbounded or unclamped values:** the entity size (spawn size and merging), the config max size, the pull speed given to entities (no cap), and the total number of live red matters (no limit; merging only happens inside a kill radius of at least 1 block).
- **Chunk loading (unverified).** The body does not hold chunks. The vanilla ray trace and block read paths are not known to guard against unloaded chunks, and a gravity reach of 142 blocks plus a scan radius of 70 is far beyond a typical loaded area. Whether unloaded chunks are loaded or generated by the scan could not be checked here (ICBM-02-95).

## 6. MIT-portable: pull and consume

Permalinks (all at commit `9c1d985a`):
- Tick, scale decay, cycle and block logic: [`RedmatterLogic.java#L77-L298`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/redmatter/logic/RedmatterLogic.java#L77-L298)
- Entity pull, attack and merge: [`RedmatterLogic.java#L356-L503`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/redmatter/logic/RedmatterLogic.java#L356-L503)
- Shell positions: [`RedmatterBlockCollector.java#L11-L51`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/redmatter/logic/RedmatterBlockCollector.java#L11-L51)
- Red-on-red pull: [`CapRedmatterPull.java#L28-L66`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/redmatter/caps/CapRedmatterPull.java#L28-L66)
- Motion damp and clamp: [`EntityRedmatter.java#L65-L107`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/redmatter/EntityRedmatter.java#L65-L107)

Pseudocode of the intended algorithm, with the quirks of 4.1 shown as comments so a port can choose to keep or fix them.

```
state: R (int, starts floor(size)), cs = -1, B = 0, queue = []

each tick (server):
    rays = 0; edits = 0
    if R > maxSize: R = clamp(floor(size), 1, maxSize); size = floor(size)
    if queue empty: startCycle()
    while rays <= rayBudget and edits <= editBudget and queue not empty:   // budgets are "<=": one extra each
        rays++
        target = queue.pop() + center
        hit = firstBlockHit(center, target, stopOnLiquid=true, ignoreBoxless=false)
        if hit and distance(center, hit.center) < R + 1:
            if removable(hit):                       // not air; (liquid with level < 7) or (not fluid and hardness >= 0)
                setAir(hit, notify=true)
                if hit was water: replace each water neighbor with ice      // uncounted edits
                if not fluid and random() > 0.8: spawnDebris(hit)           // quirk: 20%
                B++; edits++; cs = 0
    entityPass()
    if B == 0: starve()

startCycle():
    if cs > 0: cs = 0; R = clamp(R+1, 1, max); size = R+1        // quirk: overwrites size, can exceed max
    queue = shuffle(shellPositions(R))
    if B <= 0: cs++
    B = 0

shellPositions(R):    // 6 faces of the cube [-R,R]^3; each face (2R+1)^2 points; edges repeated
    for each axis-aligned face f:
        for a in -R..R: for b in -R..R: yield faceCenter(f, R) + a*u(f) + b*v(f)

starve():
    if size <= cs:                                // cs <= 1, so only for size <= 1
        if size <= minSize: size = 0; kill
        else size *= (size < 1 ? 0.9 : starveScale)

entityPass():
    for e in entities in cube(center, 2*size):
        skip self, creative or invulnerable players, blast-ignoring entities
        d = distance(e, center)
        if e handles blast velocity (another red matter): e.addVelocity(dir(e -> center) * own^3/(own^3+src^3))
        else: e.addVelocity(dir(e -> center) * min(1, max(0, 1 - d/(2*size)) * size * 0.01))
        if d < max(1, 0.08*size): attack(e)       // merge | clear blast | detonate+kill | 2000 damage | kill (+0.05 if debris)
```

Port notes for a GT-first 1.7.10 rebuild:
- The ray-from-center scheme eats the nearest block per direction. A shell-only or sphere-sorted sweep gives the same result with a deterministic rate and no shuffle; the 101-per-tick edit budget is the real throttle either way.
- Do the pull as the same linear falloff but **clamp the added speed** and skip the divide when distance is 0.
- Give the entity an idle or sleep state when the ball is clear. This is the single biggest saving versus ICBM.
- Cap size at the config max in every setter, persist both floats correctly, and persist or recompute R from the *scan state* rather than from the size.
- Count freeze edits against the budget, or drop the freeze entirely.
- Skip capturing tile NBT for blocks that will not become debris.

## 7. Persistence and sync

| What | Where | Notes |
|---|---|---|
| `blast_size` (float) | entity NBT; synced data parameter | |
| `blast_size_max` (float) | entity NBT; synced data parameter | **read back into the size field, not the max** (ICBM-02-80); a reloaded body has size = old max and max = the config default |
| R, cs, B, queue, counters | not saved | rebuilt from size on the first tick after load |
| Motion | vanilla entity NBT | |
| Client | size and max data parameters; position and velocity from the tracker; no spawn-data extras | `visualSize` is client-local, starts at 0 |

## 8. UI

None. No GUI, no chat messages. Tooltip joke lines for the explosive item (`explosive.icbmclassic:redMatter.info.0` to `.10`, count key `...info.count` = 11) never display, because of a key mismatch in the tooltip code (ICBM-02-19). The only text a player can see from the entity is the death message above and, in logs, the starvation line.

## 9. Audio

| Trigger | Event | Volume | Pitch | Rate |
|---|---|---|---|---|
| Server tick (if `redmatter_audio_enabled`) | `icbmclassic:redmatter` | 3.0 | uniform -0.8 to 1.2 (the client clamps pitch to 0.5 to 2, so about 65% of plays land at 0.5) | every tick, at the body |
| Server tick | `icbmclassic:collapse` (random of 8) | 5 to 6 | 0.6 to 1.0 | 1 in 8 per tick, at a random point in a cube of half-edge `size` |

Sound packets reach players within 16 x volume blocks (48 and 80 to 96) in vanilla 1.12. Both events are in the `block` sound category.

## 10. Relationships

- Spawned by the explosive registry's `redmatter` entry (block, cart, missile). Missile flight and impact: `03-missiles`. Carrier items and recipes: `06-blocks-items`.
- Uses `EntityFlyingBlock`/`FlyingBlock` ([effect-entities.md](effect-entities.md)), `BlockCaptureData`, `BlastBlockHelpers.isFluid` and `isFlowingWater` ([blast-engine.md](blast-engine.md) section 10), `ICBMSounds`, `IBlastIgnore`, `IBlastVelocity`, and the explosive capability on primed entities (explosive block, `01-core`).
- Killed by antimatter ([threaded-blasts.md](threaded-blasts.md); ICBM-02-10, 02-26). Only red matter implements `IBlastVelocity`, and only red matter consults it; other blasts push it with ordinary entity motion.
- `BlastRedmatterWrapper` is a deprecated `IBlast` facade (its `doAction` does nothing and returns `COMPLETED`; `clearBlast` kills the entity; its action data is the red matter explosive) so command and API code that expects a blast can remove it.

## 11. Asset catalog (catalog depth)

| Path | Size / duration | Used by |
|---|---|---|
| `textures/entity/redmatter/blackhole.png` | 32 x 32, 8,673 bytes | the disk layer (`RenderRedmatter.TEXTURE_FILE`) |
| `textures/models/grey.png` | 512 x 512, 1,858 bytes | both sphere layers (the renderer's entity texture) |
| `sounds/redmatter.ogg` | 1.01 s, 44.1 kHz stereo, 17,403 bytes | sound event `redmatter` (`sounds.json`, category `block`, subtitle `subtitle.icbmclassic:explosion.redmatter`) |
| `sounds/collapse1` to `collapse8.ogg` | 3.09, 2.42, 3.42, 2.42, 1.98, 2.20, 3.20, 3.09 s; 35 to 55 KB each; 44.1 kHz stereo | event `collapse`, picked at random |
| `textures/blocks/explosive/redmatter_side.png`, `_top.png` (animated 16 x 320 strip), `_bottom.png` | 16 x 16 each (top animated) | the explosive block (see [explosive-block.md](explosive-block.md)); blockstate `blockstates/explosives/redmatter.json` |
| `textures/blocks/redmatter_flow.png` | not measured | not referenced by any class read here |
| `textures/items/missiles/redmatter_body.png`, `redmatter_top.png`; `models/item/missiles/redmatter.json`; `models/item/bombcarts/redmatter.json` | not measured | missile and cart items (`07-visuals-audio`) |
| `sounds.json` | entries `redmatter`, `collapse` | the `sream` key is misspelled in every entry, so the streaming hint is ignored |

Credits (`Credits.md`): the entity texture is "ARR, licensed to BBM", `redmatter.ogg` is "Unknown, marked for removal/replacement" (freesound origin, likely free to use), the block textures are CC BY-NC-SA 4.0. `textures/entity/Credits.md` spells the file `blockhole.png`; the real file is `blackhole.png`. **None of these assets can be reused**; GregGuns needs its own.

## 12. Lang keys

| Key | English |
|---|---|
| `explosive.icbmclassic:redmatter.name` | Red Matter Explosives |
| `missile.icbmclassic:redmatter.name` | Red Matter Missile |
| `bombcart.icbmclassic:redmatter.name` | Red Matter Minecart |
| `bomblet.icbmclassic:redmatter.name` | Red Matter Bomblet (unused) |
| `death.attack.icbm.redmatter` | %s was crushed by the immense mass of the mighty black hole. |
| `subtitle.icbmclassic:explosion.redmatter` | Redmatter Blast |
| `explosive.icbmclassic:redMatter.info.count` | 11 (then `.info.0` to `.info.10`: 11 joke lines, such as "Oh god it's sucking me in", "Not even light....", "Into the void") |
| `config.icbmclassic:blast.redmatter.title` | referenced by the config class; no English string found |

## 13. Bugs

Full list with locations is in [index-c.md](index-c.md); IDs for this sheet are ICBM-02-80 to ICBM-02-98 (and ICBM-02-10, 02-19, 02-26 from part a).

| ID | Short title | Kind |
|---|---|---|
| ICBM-02-80 | NBT load puts max size into size and never restores max; reload gives size 70 | logic |
| ICBM-02-81 | Starvation unreachable for any body above size 1 | logic |
| ICBM-02-82 | Growth overwrites size with R + 1; merge and debris mass lost; size exceeds max by 1 | logic |
| ICBM-02-83 | Flying-block chance read inverted (20%, not 80%) | logic |
| ICBM-02-84 | Size is unbounded via spawn size and merging | logic |
| ICBM-02-85 | Red-on-red pull weight is inverted | logic |
| ICBM-02-86 | Coasting motion underflows to NaN after about 5,100 ticks | logic |
| ICBM-02-87 | Zero distance gives NaN velocity | logic |
| ICBM-02-88 | Freeze edits bypass the edit budget | logic |
| ICBM-02-89 | Fluid rules: lava eaten, Forge fluids and flowing water ignored | logic |
| ICBM-02-90 | Budgets are off by one (501 rays, 101 edits) | logic |
| ICBM-02-91 | Queue rebuilt per cycle: duplicates, garbage, unbounded memory with config | logic |
| ICBM-02-92 | Tile NBT and stack captured for every ray hit | logic |
| ICBM-02-93 | Sound spam: 1.01 s clip every tick, negative pitches | logic |
| ICBM-02-94 | `noClip` on debris is a no-op; debris re-places or hangs mid-air | logic |
| ICBM-02-95 | Possible chunk loading by ray trace (unverified) | logic |
| ICBM-02-96 | No protection, break event, or cause tracking | logic |
| ICBM-02-97 | Client allocates two 32 x 32 GLU spheres per frame | 1.12-only |
| ICBM-02-98 | Client size easing is frame-rate dependent and starts at 0 | logic (visual) |
