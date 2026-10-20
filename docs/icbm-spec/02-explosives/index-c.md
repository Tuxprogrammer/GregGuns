# 02-explosives, part c: red matter

This file holds the Contents, Coverage, Known limits and Suspected bugs sections for part c of area `02-explosives` (the red matter entity). The orchestrator merges it into `index.md` with parts a and b. Source: ICBM-Classic `prod/1.12` @ `9c1d985a`; cite classes relative to `src/main/java/icbm/classic/`.

## Contents

- [red-matter.md](red-matter.md): the entity sheet (all 10 fields) for red matter: spawn action, size and scan-radius growth, gravity pull, block consumption, flying-block conversion, starvation, merge, death; a **performance budget** (rays, edits, entity scan, memory, sound, worst-case lifetime and steady state, growth curve table); the pull-and-consume algorithm as pseudocode with permalinks (MIT-portable); the procedural renderer in prose; config, persistence, audio, asset and lang catalogs.

## Key facts

- Red matter is an **entity with no end**: starvation only works below size 1, nothing caps its lifetime, and after the ball is clear it keeps tracing 501 rays per tick and scanning a cube 284 blocks wide at default max, forever.
- Hard per-tick ceilings at defaults: **501 ray traces, 101 block removals** (plus up to 6 uncounted ice edits per removed water block), **one entity scan** of edge `4 x size`, about 20 debris entities per tick on average at full speed.
- Growth is by scan radius: +1 per empty cycle. From nothing, size 1 reaches the cap (R = 70, size 71) in about **5,500 ticks (4.6 minutes)**. A cycle at R = 70 is 119,286 ray targets. Clearing a solid ball of radius 70 takes at least 14,800 ticks (12.4 minutes) at the 101-edit cap.
- Size is rewritten to `R + 1` at each growth step, so merged mass and eaten debris do not persist, and the size settles one above the configured max.
- A reloaded red matter comes back at size 70 and scans at R = 70 at once (NBT bug). Avoid saving and loading during a test.
- The sound is the cheapest thing to cut: the 1.01 s `redmatter` clip is started every tick (20 overlapping copies), pitch drawn from a range that is partly negative. `redmatter_audio_enabled` turns it off.
- Everything is removed with no drops, no event and no protection check; tile contents are lost.
- Capabilities, data parameters, the entity entry and GLU rendering are *1.12-only mechanisms*.

## Related areas

| Area | Why |
|---|---|
| `02-explosives` part a | registry row (ID 23, tier 4), antimatter's kill rule (ICBM-02-10, 02-26), the flying-block entity, `IBlastVelocity`, `BlastBlockHelpers` |
| `03-missiles` | the missile carrier and its impact |
| `06-blocks-items` | the block, cart and missile items, recipes, the tooltip bug (ICBM-02-19) |
| `07-visuals-audio` | full texture and sound catalog |

## Coverage

Source files read in full: **10 of 10** in scope.
- `content/blast/redmatter/`: `ActionSpawnRedmatter`, `DamageSourceRedmatter`, `EntityRedmatter`; `logic/` 2 of 2 (`RedmatterLogic`, `RedmatterBlockCollector`); `caps/` 2 of 2 (`BlastRedmatterWrapper`, `CapRedmatterPull`); `render/` 2 of 2 (`RedmatterClientLogic`, `RenderRedmatter`).
- `config/blast/types/ConfigRedmatter` in full; `ConfigBlast` and `ConfigFuseTimes` (redmatter lines only).

Read to resolve references, in full: `content/entity/flyingblock/FlyingBlock`, `EntityFlyingBlock` (from line 40), `BlockCaptureData`; `api/explosion/redmatter/IBlastVelocity`; `content/blast/helpers/BlastBlockHelpers` (the two fluid tests); `content/blast/threaded/BlastAntimatter` (the red matter rule). Read in part: `content/reg/EntityReg` (the entity entry), `content/reg/ExplosiveInit` (the redmatter row), `client/ICBMSounds` (enum and `play`), `client/ClientReg` (renderer registration), `lib/projectile/EntityProjectile` (grep for `noClip`, expiry and trace calls), `lib/CalculationHelpers` (random range), `content/blocks/explosive/ItemBlockExplosive` (tooltip keys, by grep).

Assets at catalog depth: `textures/entity/redmatter/blackhole.png` and `textures/models/grey.png` (sizes read from the PNG header), `sounds/redmatter.ogg` and `collapse1` to `collapse8.ogg` (durations read from the Ogg headers), block and item textures by name, `sounds.json` entries, `Credits.md` rows, lang keys in `en_US.lang`, recipes for the missile and cart.

Not covered: the other language files; the vanilla methods the entity depends on (decompiled engine code is not in the repo).

## Known limits

- No code was run and nothing was profiled. All counts, ramp times and memory sizes come from reading the source and arithmetic on the config defaults. Object sizes (about 24 bytes per position and per list node) are estimates for a typical 64-bit JVM.
- The vanilla 1.12 behaviors that the performance numbers lean on were taken from knowledge of the game and not checked against a decompile: the block ray trace caps at about 200 steps and does not check whether chunks are loaded; block-change packet batching; the sound packet range of 16 x volume; the client clamp of pitch to 0.5 to 2; the 28-channel sound limit; the ender dragon ignoring damage that does not come through its parts; the void kill at y below -64.
- The debris lifetime (10 to 20 ticks) is an estimate from the pull acceleration, not a simulation. `EntityProjectile` handles collision for debris; its full behavior is in `03-missiles` and `effect-entities.md`.
- Whether `/icbm blast trigger` forwards the size to the action's `setValue` was inferred from the call site and the override; not traced in `01-core`.
- Whether `/icbm` has a remove sub-command that reaches `BlastRedmatterWrapper.clearBlast` was not checked.
- The sounds and textures were not seen or heard. The renderer was read, not run; the descriptions of the disk, sphere and beams come from the draw calls.
- Pull velocities were computed assuming the default config; real entity motion also includes vanilla drag, which differs per entity type.

## Suspected bugs

"Logic" means we could inherit it in a rebuild. "1.12-only" means it does not matter to a 1.7.10 rebuild. IDs 80 to 98 are used; 99 is free.

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-02-80 | NBT load puts max size into size; reload gives size 70 (logic) | `content/blast/redmatter/EntityRedmatter.java:112-122` | `readEntityFromNBT` reads `blast_size`, then sets the *size* again from `blast_size_max`. The max is never restored (it stays at the config default). Every saved and reloaded red matter comes back at its old max (70) whatever its size, and the scan radius restarts at that value on its first tick, so a small body instantly becomes a 70-block eater | gameplay |
| ICBM-02-81 | Starvation never triggers for size above 1 (logic) | `logic/RedmatterLogic.java:133-160,194-221` | The shrink test is `size <= cs`, but `cs` is reset to 0 by every growth step and incremented once per cycle, so it is never above 1. Shrinking only happens at size 1 or less, and the next growth step resets size to `R + 1` anyway. A red matter in empty space grows to the cap and stays alive forever; only a body spawned at 0.25 or less dies, on tick 2 | gameplay |
| ICBM-02-82 | Growth overwrites size with the scan radius plus 1 (logic) | `RedmatterLogic.java:498-502`, `:196-201` | `setCurrentBlockDestroyRadius` clamps the radius to max but sets the entity size to the *unclamped* argument. Each growth step therefore sets size to `R + 1`: merged mass (`:453-458`) and eaten-debris growth (`:489-492`) are discarded, and at the cap the size becomes 71, above the configured max of 70. Size and radius are two values that should not be one | gameplay |
| ICBM-02-83 | Flying-block chance is inverted (logic) | `RedmatterLogic.java:290`, `config/blast/types/ConfigRedmatter.java:57-59` | Debris spawns when a random float is *greater than* the "chance" 0.8, so the real chance is 20%. Raising the value lowers the debris | cosmetic |
| ICBM-02-84 | Size has no upper bound (logic) | `EntityRedmatter.java:181-190`, `RedmatterLogic.java:445-458`, `ActionSpawnRedmatter.java:44-46` | The clamp in `setBlastSize` is a floor only. `AREA_SIZE` (for example from the dev command), merging two bodies (cube-sum), and ICBM-02-82 all push size past max. Gravity reach is `2 x size`, so the entity query box grows with size while the block scan stays capped: a size-500 body scans a 2,000-block cube every tick | gameplay (performance) |
| ICBM-02-85 | Red-on-red pull is inverted (logic) | `content/blast/redmatter/caps/CapRedmatterPull.java:39-66` | The pulled body is accelerated by `own^3 / (own^3 + other^3)`: the larger body is pulled harder, the smaller barely moves. The code comment says it should simulate gravity | gameplay |
| ICBM-02-86 | Coasting motion becomes NaN (logic) | `EntityRedmatter.java:65-107` | The motion-normalize step divides by the speed with no zero check. Motion decays 2% per tick and the speed is a float, so after about 5,100 ticks from 0.5 it underflows to 0 and the motion becomes NaN, making the position NaN. A pushed lone red matter breaks in about 4 minutes | gameplay |
| ICBM-02-87 | Zero distance gives NaN velocity (logic) | `RedmatterLogic.java:402-437`, `CapRedmatterPull.java:39-55` | Pull direction is `difference / distance` with no zero guard. An entity exactly at the body's position (debris spawned at a block center equal to the body, a primed carrier at the same point) receives NaN velocity. Related to ICBM-02-16 | gameplay |
| ICBM-02-88 | Ice from water removal bypasses the edit budget (logic) | `RedmatterLogic.java:281-287,300-311` | Each removed vanilla water block freezes its six neighbors with `setBlockState` calls that are not counted, so one tick can do 101 counted plus 606 uncounted edits. A lake is a worst case. Lava and Forge-style fluids are not frozen | gameplay (performance) |
| ICBM-02-89 | Fluid rules do not match their names (logic) | `RedmatterLogic.java:327-335`, `content/blast/helpers/BlastBlockHelpers.java:23-37` | `isFlowingWater` is true for any vanilla liquid (lava too) with level below 7. But the ray trace is run with stop-on-liquid on, which only hits source blocks (level 0), so flowing liquid is never targeted. Forge-style fluid blocks are not removed at all. Water is swallowed only as sources, then refilled by flow from neighbors | gameplay |
| ICBM-02-90 | Budgets are off by one (logic) | `RedmatterLogic.java:255-260` | Both budget tests use "greater than", so a config of 500 traces and 100 edits allows 501 and 101. Same pattern as ICBM-02-06 | cosmetic |
| ICBM-02-91 | Queue rebuilt every cycle: duplicates, garbage, unbounded memory (logic) | `RedmatterLogic.java:194-221`, `logic/RedmatterBlockCollector.java:11-51`, `ConfigRedmatter.java:15-17` | The six cube faces share their edges and corners, so `6(2R+1)^2` targets include about `24R + 4` repeats of `24R^2 + 2` distinct points. Each cycle allocates and shuffles all of them (about 5.7 MB at R = 70). `redmatter_max_size` has no range: at 500 it is 6 million entries, about 290 MB per body | gameplay (performance) |
| ICBM-02-92 | Tile NBT and stacks captured for every ray hit (logic) | `RedmatterLogic.java:269-296`, `content/entity/flyingblock/BlockCaptureData.java:33-42` | A capture object is built (item stack plus a full tile-entity NBT write) before the removal test, so it happens for blocks that are not removed and for the 80% that never become debris. For GT machines this can be a large write per ray hit | gameplay (performance) |
| ICBM-02-93 | Sound spam and negative pitch (logic) | `RedmatterLogic.java:105-119` | The 1.01 s `redmatter` clip is played every tick (20 overlapping instances per listener), and the pitch is drawn uniformly from -0.8 to 1.2, of which about 65% clamps to the client minimum. The collapse sound adds another 2.5 starts per second at volume 5 to 6. This can fill the client's sound channels and starve every other sound. The code comment already asks whether it should play every tick | gameplay |
| ICBM-02-94 | Debris `noClip` does nothing; debris re-places or hangs (logic) | `RedmatterLogic.java:344-348`, `lib/projectile/EntityProjectile.java:244,353` | The callback sets `noClip` on the flying block, but the projectile base moves by its own block ray trace and never reads the flag. Debris collides with the carved cavity after a 5-tick grace and places its block back; debris that lives out its 1,200-tick flight places its block in mid-air (see ICBM-02-70) | gameplay |
| ICBM-02-95 | Scan may load or generate chunks (logic, unverified) | `RedmatterLogic.java:233-248` | The body does not hold chunks; the scan radius (70) and the gravity reach (142) can exceed the loaded area. The vanilla block-read path used by the ray trace does not check that the chunk is loaded in 1.12 (not verified). On a rebuild, check first and skip unloaded chunks | gameplay (performance) |
| ICBM-02-96 | No protection, break event, or cause tracking (logic) | `RedmatterLogic.java:281`, `ActionSpawnRedmatter.java:37-54` | Blocks are set to air directly: no break event, no region or claim check, no explosion-resistance check (obsidian equals dirt), nothing dropped. The action source is discarded and the damage source has a TODO, so kills cannot be credited to the player who fired it | gameplay |
| ICBM-02-97 | Client allocates two GLU spheres each frame (1.12-only) | `render/RenderRedmatter.java:60-128` | `new Sphere().draw(r, 32, 32)` twice per frame per body, immediate mode, no display list: about 2,000 quads per frame per red matter | visual (performance) |
| ICBM-02-98 | Client size easing is frame-rate dependent and starts at 0 (logic) | `render/RedmatterClientLogic.java:15-26`, `RenderRedmatter.java:56-58` | `visual += partialTicks x (size - visual)` runs once per frame, and the visual size starts at 0, so the body grows in on first sight and the speed of the easing depends on frame rate. The disk spin uses the raw tick count, not interpolated | cosmetic |

Minor, not given IDs: `EntityRedmatter` is registered with an update interval of 1 and range 500, so every player within 500 blocks gets its position each tick; the beam color cache is a static list shared by all red matters (capped at 30 entries, harmless); `RenderRedmatter.colorIn` carries a TODO that its role is unknown; the pulse on the outer sphere is 1% of its radius and is effectively invisible; `textures/entity/Credits.md` lists the texture under a misspelled name; `sounds.json` entries misspell the `stream` key as `sream`; `BlastRedmatterWrapper` is marked deprecated with a TODO to remove it; `getEntityTexture` returns the grey model texture, unused by the custom draw; the explosive-capability branch in `attackEntity` runs a full explosive creation inside the entity tick, with no depth guard against a chain of red matter explosives.
