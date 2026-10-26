# GregGuns ICBM spec

How GregGuns builds ICBM-style content (missiles, tiered explosives, launchers, radar, EMP and air defense) on GTNH. The [ICBM spec](../icbm-spec/README.md) gives the baseline; this spec gives **the delta and the integration**. It follows the [GregGuns spec conventions](../gregguns-spec/CONVENTIONS.md), with the Techguns baseline links replaced by ICBM spec links. Requirement IDs are `GGI-<area>-<nn>`.

Status: **planned for build** on the [ICBM roadmap](../ROADMAP-ICBM.md) (milestones I0–I7), as a module in the GregGuns jar behind `modules.icbm` ([0018](../design/0018-icbm-program.md)). Each milestone writes its area sheets here before building.

## Areas

| Area | Covers | ICBM baseline | Built in |
|---|---|---|---|
| [01-core](01-core/index.md) | Module flag, Galacticraft dependency, OpenComputers and the Golden Dome floppy | [01-core](../icbm-spec/01-core/index.md) | I0, I4, I5 |
| [02-explosives](02-explosives/index.md) | Shared explosive registry, blast engine, warheads, annihilation, fallout | [02-explosives](../icbm-spec/02-explosives/index.md) | I1, I3, I5 |
| [03-missiles](03-missiles/index.md) | Missile bodies, stack-on-pad build, fuel, flight | [03-missiles](../icbm-spec/03-missiles/index.md) | I2, I3, I5 |
| [04-launch-control](04-launch-control/index.md) | Launch-control block, silo multiblock, outputs | [04-launch-control](../icbm-spec/04-launch-control/index.md) | I2, I3, I4 |
| [05-detection-defense](05-detection-defense/index.md) | Radar, air-defense battery, EMP, handheld tools | [05-detection-defense](../icbm-spec/05-detection-defense/index.md) | I4 |
| [06-blocks-items](06-blocks-items/index.md) | Kept, dropped and deferred content, registry, recipe remap, antidote | [06-blocks-items](../icbm-spec/06-blocks-items/index.md) | I1-I5 |
| [07-visuals-audio](07-visuals-audio/index.md) | 3D models, textures, effects, sounds, asset sourcing | [07-visuals-audio](../icbm-spec/07-visuals-audio/index.md) | I1, I2, I5 |
| [08-worldgen](08-worldgen/index.md) | Abandoned Missile Silos on every planet (GregGuns only) | none | I6 |

## Decisions and open questions

All of them are in **[DECISIONS.md](DECISIONS.md)**: what has been decided (D1–D49) and any open questions. From I0 on, the orchestrator adds and records decisions itself. Each open question gives its context, the options and a recommendation.

In short:
- Clean room, with MIT hard-math ports allowed.
- Baseline is ICBM-Classic 1.12.
- Tiers run HV to UHV; red matter is the UHV missile.
- Built next, on [ROADMAP-ICBM](../ROADMAP-ICBM.md), as a module in the GregGuns jar behind `modules.icbm` (off by default until the ICBM beta).
- Nuclear and every warhead above it kill every living entity in the kill zone, bosses included. Conventional missiles have center-damage targets sized against Infernal Mobs health pools.
- Missiles are an automatable arsenal: about one of their tier's rocket launches in cost, and Assembly Line-automatable.
- The player stacks the missile body and a warhead block on the pad; the Galacticraft pad is never removed.
- Assets are human-made and properly licensed first; generation is a last resort.
- Abandoned Missile Silos come in activity levels, from a guard-free ruin to an operational silo full of spawners to clear and take over. They generate on the Overworld (EV loot) and on every planet, with loot rising one tier per rocket tier.
- Missile bodies are built in the Galacticraft NASA Workbench from GT-gated schematics, or on the Assembly Line. T1–T2 launch from the GC pad and T3 and up from a GregGuns silo multiblock.
- Galacticraft is required by the ICBM module only.

## Standing constraints from existing records

- **Energy is EU** ([0003](../design/0003-energy-model.md), [0008](../design/0008-exact-tier-charging.md)). ICBM's Forge Energy buffers and its `use_energy` switch are replaced.
- **Machines follow [0011](../design/0011-machine-policy.md)**, as GT MetaTileEntities: single blocks kept, and a non-cube multiblock for each regularly used machine.
- **Prefer pack machines** (0012): recipes route through GT and GTNH machines, and we own only the final items.
- **One block-breaking path.** Every warhead uses the existing GregGuns blast pipeline (`ExplosionRules`, `BlockBreakGuard`, `ExplosionEvent.Detonate`), never a second path (see [explosions-pack](../gregguns-spec/04-projectiles-fx/explosions-pack.md)).
- **Hazards go through GT** (`Hazard` with `IHazardProtector`) in place of ICBM's protective-armor ratings, where that fits.

