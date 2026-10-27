# Red matter

- **ICBM baseline:** [red-matter](../../icbm-spec/02-explosives/red-matter.md) (read it for what not to reproduce)
- **Design records:** [0018](../../design/0018-icbm-program.md) section 7
- **Decisions:** [D10](../DECISIONS.md#d10-in-detail), [D30](../DECISIONS.md), [D131, D132, D133, D134, D135](../DECISIONS.md)
- **Pack mods:** GregTech (UHV Assembler, Field Generator UHV, Emitter UHV, Quantum Eye, neutronium)
- **Roadmap:** [I5](../../ROADMAP-ICBM.md#i5--endgame-warheads-and-systems); the owner tests it in game

## What changes from ICBM

| ICBM red matter | GregGuns red matter | Bug avoided |
|---|---|---|
| An entity that never ends (starvation works only at size 1 or less) | A blast effect with a **hard lifetime** (`icbm.redMatterLifetimeSeconds`, default 60) and an **idle state** that ends after 10 s | ICBM-02-81 |
| 501 ray traces a tick forever, queue rebuilt each cycle | A **cursor walk** over each new shell once, in scheduler slices under the **shared tick budget**; at most 384 edits a tick (a GT machine counts as 8, since breaking a charged one can chain explosions); nothing allocated per visited cell; idle means no scan at all | 02-90, 02-91, 02-88 |
| Reloads at full size (the max is read back as the size) | The whole state (radius, shell, cursor, mode, age) is in the blast's saved effect state; it resumes where it was | 02-80 |
| Size overwritten by a growth rule, unbounded by merging | An integer radius that grows one block per pass up to the blast radius (32 at fill 128); no merging | 02-82, 02-84 |
| No protection, break event or cause | Claims and the break guard see every batch (`ExplosionEvent.Detonate`), the source is the blast's | 02-96 |
| Eats lava and water sources, fluids half-handled | Liquids are not eaten; bedrock is not eaten | 02-89 |
| NaN velocity at zero distance, coasting underflow | Pull is zero at the center and for NaN; speed capped at 1.5 blocks a tick | 02-86, 02-87 |
| A 1-second sound every tick, negative pitches | One sound at ignition and one at the end | 02-93 |
| Two GLU spheres allocated per frame | One reused quadric, one orb per body ([blast-visuals](../07-visuals-audio/blast-visuals.md#orbs)) | 02-97 |

## Behavior

`redmatter` is a `WarheadBlast` of tier T5, shape `NONE` (the engine digs nothing), `Kind.REDMATTER`, full radius 32; its radius at a fill is the blast radius of [warheads-and-fill](warheads-and-fill.md) (about 8 at fill 2, 13 at fill 8). Its effect (`RedMatterEffect`, math in `RedMatterModel`) lives in the blast's saved effect state:

1. **Ignite.** Radius 2, one sound, a pull and kill every tick from now on.
2. **Eat.** One pass per radius from 2 to the maximum. A pass visits the cube cells of its shell (distance beyond the last radius, up to this one) with a cursor, 4,096 cells and 32 edits a slice, and removes breakable, non-liquid blocks that claims and the break guard allow. Unloaded chunks are skipped, never loaded. Work is offered through `BlastEffect.wantsWork/work`, so the scheduler shares the budget with every crater.
3. **Idle.** When the full-size pass ends, the body stops scanning. It lingers 200 ticks (pull and kill continue), then ends. With `icbm.terrainDamageT4` off it idles from the start.
4. **Pull and kill.** Per tick, at most 128 entities: living ones inside the ball (radius, at least 1.5) are annihilated, bosses included, through the damage settings; other entities inside it are removed (players are never removed, creative players are skipped); entities within `max(2 x radius, ball + 8)` are pulled toward the center.
5. **End.** After the lifetime, after idling, or when antimatter goes off within 200 blocks ([antimatter](antimatter.md)): no aftermath. A body that ends by itself plays one sound; one that antimatter cancels ends under the antimatter flash and plays none. It leaves no fallout.

## Recipe and cost

An Assembler recipe at UHV, circuit 2: 8 neutronium plates, 2 UHV Field Generators, 2 Quantum Eyes, 2 UHV Emitters, 8 Industrial TNT, 4 Plastic Explosive and a Fuze Assembly Mk III. About 190 plate equivalents ([D134](../DECISIONS.md)).

## Requirements

- GGS-ICBM-02-34: a body never lives past `icbm.redMatterLifetimeSeconds` and ends within 200 ticks of going idle.
- GGS-ICBM-02-35: a body saved mid-growth resumes with the same radius, shell and cursor.
- GGS-ICBM-02-36: a body does at most 384 block edits and checks at most 128 entities in a tick, and does no scan work while idle.
- GGS-ICBM-02-37: the pull is capped and has no zero-distance or NaN case.

## Tests

`RedMatterModelTest` (shell walk, growth, pull, clamp, bounds, state round trip, idle at full size), `OrbPlanTest`; scenarios `icbm.redmatter.lifecycle` (eats its ball, kills an item and a cow in it, pulls an item from outside, goes idle, ends before the lifetime, worst tick within the budget plus slack) and `icbm.redmatter.save_reload` (saved while growing, same radius and cursor after the reload, finishes and ends).

## Open questions

- The eat rate and lifetime are a first pass for the owner's playtest; `RedMatterModel` holds the numbers.
- Two red matter bodies do not pull or merge with each other (ICBM's merge and red-on-red pull are dropped).
