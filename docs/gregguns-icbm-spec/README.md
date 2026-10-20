# GregGuns ICBM spec

How GregGuns builds ICBM-style content (missiles, tiered explosives, launchers, radar, EMP and air defense) on GTNH. The [ICBM spec](../icbm-spec/README.md) gives the baseline; this spec gives **the delta and the integration**. It follows the [GregGuns spec conventions](../gregguns-spec/CONVENTIONS.md), with the Techguns baseline links replaced by ICBM spec links. Requirement IDs are `GGI-<area>-<nn>`.

Status: **planning**. Nothing here is on the roadmap yet. The build happens after 1.0, as an ICBM track behind the `modules.icbm` feature flag.

## Decisions and open questions

All of them are in **[DECISIONS.md](DECISIONS.md)**: what has been decided (D1–D22) and the open questions (Q11–Q15). Each open question gives its context, the options and a recommendation.

In short:
- Clean room, with MIT hard-math ports allowed.
- Baseline is ICBM-Classic 1.12.
- Tiers run HV to UHV; red matter is the UHV missile.
- Build after 1.0, behind `modules.icbm`.
- Missiles are built in the Galacticraft NASA Workbench from GT-gated schematics. T1–T2 launch from the GC pad and T3 and up from a GregGuns silo multiblock.
- Galacticraft is required by the ICBM module only.

## Standing constraints from existing records

- **Energy is EU** ([0003](../design/0003-energy-model.md), [0008](../design/0008-exact-tier-charging.md)). ICBM's Forge Energy buffers and its `use_energy` switch are replaced.
- **Machines follow [0011](../design/0011-machine-policy.md)**, as GT MetaTileEntities: single blocks kept, and a non-cube multiblock for each regularly used machine.
- **Prefer pack machines** (0012): recipes route through GT and GTNH machines, and we own only the final items.
- **One block-breaking path.** Every warhead uses the existing GregGuns blast pipeline (`ExplosionRules`, `BlockBreakGuard`, `ExplosionEvent.Detonate`), never a second path (see [explosions-pack](../gregguns-spec/04-projectiles-fx/explosions-pack.md)).
- **Hazards go through GT** (`Hazard` with `IHazardProtector`) in place of ICBM's protective-armor ratings, where that fits.

