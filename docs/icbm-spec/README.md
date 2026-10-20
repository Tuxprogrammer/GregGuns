# ICBM behavioral spec

A description of **ICBM-Classic for Minecraft 1.12.2**, branch `prod/1.12` at commit [`9c1d985a`](https://github.com/BuiltBrokenModding/ICBM-Classic/tree/9c1d985a375adc2868ff47707b3a636452d0b63a). It records what ICBM does, bugs included, so GregGuns can rebuild the parts it wants on GTNH. [docs/gregguns-icbm-spec/](../gregguns-icbm-spec/README.md) records how GregGuns differs and how it plugs into the pack. Read the two side by side.

Status: **scaffold**. The areas below are planned. Each one gets an `index.md` when its sheets are written.

## Which sources this spec covers

| Source | Role | License (see [sources-and-licensing](../icbm/sources-and-licensing.md)) |
|---|---|---|
| ICBM-Classic `prod/1.12` @ `9c1d985a` | **Baseline.** Every area describes this. | Code MIT; assets ARR |
| BuiltBrokenModding/ICBM ("ICBM 2", 1.7.10) | Appendix [A1](#appendices): its modular missile system, as a design option | Code MIT; assets excluded |
| ICBM-Classic `prod/1.7` @ `198999ed` | Appendix [A2](#appendices): one line per feature that 1.7.10 had and 1.12 dropped or changed | **No license.** Read only, never ported |

## Code policy

The policy is owner-approved (2026-10-20).

- **Clean room by default**, as with Techguns: describe behavior in your own words, tables or pseudocode. Builders work from this spec.
- **Hard-math exception.** Named algorithms may be ported from the MIT 1.12 code, keeping the MIT notice in the third-party notices file. Mark each such section `MIT-portable` and give a permalink pinned to `9c1d985a`. Candidates: blast ray-casting, threaded blast scheduling, ballistic and arc flight, radar sweep, and SAM intercept.
- Nothing from `prod/1.7` is ever ported, because it has no license.
- **Ported code meets the full GregGuns bar before it is committed or pushed** (owner rule, 2026-10-20). That means comments, tests, [CONTRIBUTING](../../CONTRIBUTING.md) patterns, the definition of done, and a fix for every `ICBM-xx-nn` bug in the ported logic. Treat a port as a rewrite that starts from MIT logic, never as a copy made for speed.

## Areas

| Area | Covers |
|---|---|
| 01-core | Lifecycle and registration, all config groups (~35 classes), the radio bus, packets, commands, the public `api/` package, datafixers, the energy model (FE buffers, `use_energy`) |
| 02-explosives | Explosive registry and tiers, every blast type (26 IDs), the blast engine (threaded blasts, ray-casting, block updates), fuse times, explosive block, grenade, bomb cart, flying blocks and fragments, gas, potions and protective armor, radioactive blocks, red matter |
| 03-missiles | Missile entities and health, flight logic (direct, arc, warmup, follow-target), targeting, missile tracker and chunk loading, cluster missiles and bomblets, SAM, held-item missiles, parachute and balloon cargo, rocket launcher |
| 04-launch-control | Launcher base, frame, connector and screen, launcher network (shared energy and inventory), cruise launcher, inaccuracy model, cargo hook |
| 05-detection-defense | Radar station and the radar map, EMP tower and EMP action and capability, radar gun, remote detonator, laser detonator, defuser |
| 06-blocks-items | Concrete, reinforced glass, glass button and pressure plate, spikes, crafting items, powders, antidote, battery, recipes |
| 07-visuals-audio | Renderers and models, a catalog of every texture, sound and lang key (one line per asset; full entries only for assets GregGuns keeps) |

## Appendices

| Appendix | Covers |
|---|---|
| A1-icbm2-modular | ICBM 2's modular missiles: sizes, casings, engines and fuels, guidance, triggers, workstation, silos and doors, antenna command network |
| A2-lineage | Features that ICBM-Classic 1.7.10 had and 1.12 dropped or changed: camouflage block, signal disrupter, S-Mine, rejuvenation, reinforced rail, proximity detector, Nightmare |

## Reference files (when written)

- [CONVENTIONS.md](CONVENTIONS.md): how this spec differs from the Techguns conventions.
- `BUGS.md`: every suspected ICBM bug, merged across areas. IDs are `ICBM-<area>-<nn>`, for example `ICBM-02-07`.
