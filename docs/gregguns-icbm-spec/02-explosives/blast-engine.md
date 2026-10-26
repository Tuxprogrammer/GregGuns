# Blast engine

- **ICBM baseline:** [blast-engine](../../icbm-spec/02-explosives/blast-engine.md), [threaded-blasts](../../icbm-spec/02-explosives/threaded-blasts.md)
- **Design records:** [0018](../../design/0018-icbm-program.md); the existing pipeline in [explosions-pack](../../gregguns-spec/04-projectiles-fx/explosions-pack.md)
- **Decisions:** [D11, D12, D34, D52, D59](../DECISIONS.md)
- **Pack mods:** ServerUtilities and other claim mods, through `ExplosionEvent.Detonate` and `BlockEvent.BreakEvent`
- **Roadmap:** [I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine)

## What changes from the ICBM baseline

| ICBM baseline | GregGuns | Why |
|---|---|---|
| Worker thread reads the world while the main thread edits it | The main thread copies a resistance cube (`BlastGrid`) in slices; the worker casts rays over the copy only | ICBM-02-45; the world is never touched off the server thread |
| Edit queue per dimension, 35 ms, run at both tick phases | **One budget** (`icbm.blastBudgetMs`, default 12 ms) shared by every running blast, round-robin, run once at the end of the tick | ICBM-02-06 |
| Edits applied with `onBlockExploded`, no events | The `Detonate` event carries the real block and entity lists before anything is hurt or broken; breaks go through `ProjectileExplosion.breakBlock` (protection, machine rule) | ICBM-02-59, ICBM-02-96, D12 |
| Threaded blasts not saved | Saved per dimension in a `WorldSavedData`; a blast resumes at its stage and cursor | ICBM-02-46 |
| No cancel for a task in the pool | A cancel flag the worker polls; world unload cancels a copy of the list | ICBM-02-08, ICBM-02-09 |
| One exception ends a worker for good | The task catches its own failure and reports it to the job | ICBM-02-07 |

## Stages of a blast

1. **Accept** (`BlastEngine.start`): post `ExplosionEvent.Start` (a cancel refuses the blast), decide whether terrain may break (`TerrainRule`: the tier's `icbm.terrainDamageT<n>` only, never `explosions.blockDamage`, D52), play the sound and particles, queue the job.
2. **Snapshot** (server thread, one slab per slice): copy the resistance of every block in the radius into `BlastGrid`. Unloaded chunks become a wall; the blast never loads a chunk (ICBM-02-95).
3. **Cast** (worker thread): `BlastRayCaster` returns the reached cells, each once, nearest first.
4. **Decide** (server thread, slices, see [Incremental crater](#incremental-crater-i3)): post `ExplosionEvent.Detonate` with the cell list and the victim list, a batch at a time; listeners may remove from either. Hurt the remaining victims on the damage ramp (or annihilate, see [annihilation](annihilation.md)). Every victim in the radius is also pushed away from the center on the same ramp (`Knockback`: 2.0 blocks per tick in the core, zero at the radius, reduced by knockback resistance; players get the vanilla push packet with no blocks). Knockback is part of the shared engine, so no blast type adds its own. Keep the surviving cells in order.
5. **Apply** (server thread, slices under the shared budget): break 48 blocks per slice through `ProjectileExplosion.breakBlock`, skipping unloaded cells. No drops unless `icbm.blastDrops`.
6. **Effect:** the type's `BlastEffect` ticks every server tick from acceptance; the blast ends when both the crater and the effect are done, and `onFinish` runs then (ICBM-02-11).

A type with shape `NONE`, or a blast that may not break terrain, skips stages 2, 3 and 5.

## MIT-portable parts

Only `BlastRayCaster`, from the ICBM spec's [ray casting section](../../icbm-spec/02-explosives/threaded-blasts.md#5-ray-casting-mit-portable) and [legacy ray casters](../../icbm-spec/02-explosives/blast-engine.md#5-legacy-ray-casters-mit-portable) (`cast` is the large caster, `castGrid` the small one). The scheduler, snapshot, save and load, damage and registry are native. The class carries the ported-code header, the notices entry covers it, and `BlastRayCasterTest` is its test class.

Fixed in the port:

| Bug | What it was | Fix and test |
|---|---|---|
| ICBM-02-02 | "once per block" compared objects by reference, so resistance was charged at every half-step | cells compared by value; `icbm0202AThickBlockIsChargedOncePerRay...` |
| ICBM-02-03 | yaw ran to 4 pi, every direction cast twice | slices are `pi/steps` apart over `2 x steps`; `icbm0203EachDirectionIsCastOnce` |
| ICBM-02-04 | every ray added every block it reached, duplicates | a bit set; `icbm0204AndIcbm0260...` |
| ICBM-02-05 | the configured radius exceeded what the energy reaches | `defaultEnergy(radius)` is calibrated to stone, which is more than the weakest ray needs to cross open air to the edge; `icbm0205...`, [D59](#crater-size-d59) |
| ICBM-02-60 | destroy list scanned with `List.contains` | a bit set and a sort, linear in the result |

## Crater size (D59)

An explosive's radius is the **radius of the crater it digs in vanilla stone** (resistance 6), not only the reach of a ray through open air. Both casters take a starting energy:

| Caster | Energy | Per block of stone | Samples |
|---|---|---|---|
| `cast` (sphere) | `defaultEnergy(R)` = `R x (2.25 + 6.0) / 0.75`, never below the open-air value | 2.25 for the half-steps plus 6.0 resistance, charged once per block | `2 x ceil(pi R)^2` rays |
| `castGrid` (small) | `gridEnergy(R)` = `R x (6.0 + 0.75)`, times 0.7 to 1.3 per ray | 6.0 resistance plus 0.75 for the steps | `max(16, 2R)` per cube edge |

The sphere caster also casts the straight-down pole too (an extra pitch slice): the original's pitch stops just short of it, which left a spire of stone under the center once craters dug deep. An average ray clears `R` blocks of stone. A type may set `rayEnergy` to override either default. Guards: `BlastRayCasterTest` (a stone half-space at R=9 digs at least R-2 deep and fills at least half the hemisphere, for both casters; the old energy does not) and scenario `icbm.blast.crater_size`. The pipeline scenario scales its block floor with the radius.

## Incremental crater (I3)

I1 decided the whole crater in one server-thread pass (`BlastJob.decide`): fine at radius 20 (about 13,000 cells), but a radius-64 nuclear crater is about a million cells, each a `ChunkPosition` object in a vanilla `Explosion` list. The decide step is now its own **DECIDE** stage that runs in slices under the shared budget:

- A slice takes the next `DECIDE_PER_SLICE` (4,096) cells of the cast result, posts one `ExplosionEvent.Detonate` for them and keeps the cells the listeners left, writing them back **in place at the front of the same array** (`kept` is the write cursor, `cursor` the read cursor). Claim and protection listeners therefore see every cell, just in batches.
- The first slice also gathers the victims, posts them with its batch and hurts them (once: `damaged` is saved). Later slices post an empty victim list.
- When the cursor reaches the end the array is trimmed to `kept` and the blast moves to APPLY, or to EFFECT when nothing is left.
- **Save and load:** `BlastJobState` keeps the cast result, `cursor` and `kept` for a blast saved in DECIDE, so a reload resumes inside the stage (the cast is not repeated). A blast saved before the cast finished still restarts at the snapshot.
- **Memory:** the cast result is 4 bytes a cell and a sphere holds at most 0.52 of its cube, so it never exceeds the 3 bytes a cube cell the snapshot cube held. It is reserved in `GridMemory` under `icbm.maxGridMemoryMb` when the cast ends and released when the stage does; a blast whose result does not fit waits queued, and a lone blast always runs.
- **Budget:** a slice is one scheduler turn like a snapshot slab or a break slice, so the shared `TickBudget` bounds it. Scenario `icbm.blast.crater_nuclear` detonates a full nuclear warhead (radius 64) over a stone slab with a 6 ms budget, requires at least three ticks in the decide stage, saves and reloads in the middle of it, and checks that no tick spent more than the budget plus slack on blasts and that the crater is dug to the radius. `BlastJobStateTest` covers the save round trip.

## Effects that change blocks

Fire (Incendiary) and Sonic's glass, like the Breaching tunnel, collect their cells and post `ExplosionEvent.Detonate` before changing anything, so claims can veto them (D52); fire is placed only for the surviving cells, glass and tunnels break through `ProjectileExplosion.breakBlock`. Every such change also needs the tier's `icbm.terrainDamageT<n>` switch, the break guard, and `mobGriefing` when a mob caused the blast. Scenario `icbm.blast.effect_veto` covers fire and glass.

## Memory cap

Each blast copies a `(2R+1)^3` cube of 2-byte cells (about 3 bytes with the result). `icbm.maxBlastRadius` keeps its range (8 to 200); instead `icbm.maxGridMemoryMb` (default 256) caps the cubes of all running blasts together (`GridMemory`). A blast that would go past the cap waits in the queue, still counted as active, until another blast finishes its cast; a lone blast always runs, so one huge radius is never refused. Only annihilating explosives send the long-range flash packet; other blasts use the normal sound and particles.

## Save and load

`BlastJobState` holds the explosive id, center, fill, radius, source, age, stage, flags, the crater as one packed `int` per block and the cursor, plus the effect's state compound. `BlastSavedData` writes the live jobs when the world saves and hands them back when it loads. A blast saved before its crater is decided restarts at the snapshot (nothing in the world had changed); one saved while breaking resumes at its cursor. An unknown explosive id is dropped with a warning. World unload saves, then cancels a copy of the list.

## Requirements

1. **GGI-02-10.** The world is read and changed on the server thread only; the worker sees a snapshot.
2. **GGI-02-11.** All running blasts share one `TickBudget`, opened once per server tick at the end phase; the first blast in the rotation always gets one slice, and the rotation start moves each tick.
3. **GGI-02-12.** A blast with terrain damage off breaks nothing, posts `Detonate` and still hurts and pushes entities.
4. **GGI-02-13.** A blast saved while breaking resumes with exactly the blocks left.
5. **GGI-02-14.** A world unload cancels its blasts without touching the list being iterated.

## Tests

Units: `BlastRayCasterTest`, `BlastSchedulerTest`, `BlastJobStateTest`, `DamageRampTest`, `KnockbackTest`, `TerrainRuleTest`. Scenarios: `icbm.blast.pipeline` (every explosive, three modes: allowed, the tier switch off, listener veto; `explosions.blockDamage` is set off in the allowed mode to prove it is ignored), `icbm.blast.budget` (three radius-18 blasts at once, worst tick against the budget), `icbm.blast.save_reload` (saved and reloaded mid-crater; blocks broken equal blocks pending at the save), `icbm.blast.crater_size` (D59), `icbm.blast.effect_veto` (fire and glass). Unit: `GridMemoryTest`.

## Bugs avoided

ICBM-02-01 (no recursive accessor), 02-02, 02-03, 02-04, 02-05, 02-06, 02-07, 02-08, 02-09, 02-11, 02-20 and 02-76 (counters saved as ints), 02-45, 02-46, 02-59, 02-60, 02-90, 02-95, 02-96.

## Open questions

- Entity knockback is not applied in I1 (the ICBM push has NaN cases, 02-16 and 02-55); add with a zero-distance guard if playtests want it.
- Maximum radius stays 96 (the nuclear warhead is 64 at a full casing, so a large fuel bonus still fits).
