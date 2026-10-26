# 02-explosives (GregGuns ICBM)

## Overview

Warheads and blasts: the shared explosive registry, the blast engine, warhead items and blocks, annihilation and fallout. Every blast goes through the existing GregGuns pipeline (`ExplosionRules`, `BlockBreakGuard`, `ExplosionEvent.Detonate`), never a second block-breaking path. ICBM baseline: [icbm-spec 02-explosives](../../icbm-spec/02-explosives/index.md).

## Contents

The registry, engine and annihilation sheets are written; the rest are planned. Each is written before the milestone that builds it, as laid out in the [ICBM roadmap](../../ROADMAP-ICBM.md).

- [explosive-registry.md](explosive-registry.md): the one shared registry (D12), tiers on the GT ladder (D5), which blast types are kept (D10), config keys.
- [blast-engine.md](blast-engine.md): ray-cast blasts, threaded scheduling, one tick budget for all running blasts, save and load of blasts in progress. MIT-portable parts are named here.
- warheads-and-fill.md (planned): the warhead item with NBT `{explosive, fill}`, the casing-tier cap, the radius curve, the fuel bonus (D20, D23).
- warhead-blocks.md (planned): the placed warhead as a 3D model with filler blocks, collision box, fuse, demolition-charge use (D31).
- blast-types.md (planned): the kept T1-T3 blast types, their gas and status effects, and conventional center-damage targets (D34).
- [annihilation.md](annihilation.md): the shared kill-zone rule with a bosses flag (D30).
- fallout.md (planned): irradiated blocks, trinitite, goo, caps and GT yields (D21).
- red-matter.md (planned): the bounded rebuild of red matter (D10).

## Integration map

| ICBM feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Explosive registry and tiers | One shared registry; tiers spread HV to UHV | GT5-Unofficial tiers | [D5, D12](../DECISIONS.md) |
| Blast block removal | Existing GregGuns blast pipeline only | GregGuns `ExplosionRules`, `BlockBreakGuard` | [D11, D12](../DECISIONS.md), [explosions-pack](../../gregguns-spec/04-projectiles-fx/explosions-pack.md) |
| Terrain damage and protection | On by default, claims respected, per-tier config | Claim mods through `ExplosionEvent.Detonate` | [D11](../DECISIONS.md) |
| Nuclear blast | One implementation shared with the tactical nuke, different scale | GregGuns | [D12](../DECISIONS.md) |
| Warhead size | Item NBT `{explosive, fill}`, cap by casing tier | GT casings | [D23](../DECISIONS.md#d23-in-detail) |
| Entity damage of nuclear and up | Annihilation inside the kill zone, bosses included | GregGuns damage model | [D30](../DECISIONS.md), [0018](../../design/0018-icbm-program.md) |
| Radioactive blocks, rot, mutation | Irradiated blocks with caps, protection by any `IHazardProtector`, GT yields | GT hazards, Centrifuge, Chemical Reactor | [D21](../DECISIONS.md#d21-in-detail) |
| Antimatter, red matter | Kept at UV and UHV; red matter rebuilt with a bounded lifetime | GregGuns blast budget | [D10](../DECISIONS.md#d10-in-detail) |
| Dropped blast content (colors gas, Christmas, Nightmare) | Dropped | none | [D10](../DECISIONS.md#d10-in-detail) |
| Protective-armor rating | GT `Hazard` and `IHazardProtector` | GT5-Unofficial | [D21](../DECISIONS.md#d21-in-detail) |
| Warhead crafting | GT machines (Chemical Reactor, Implosion Compressor), visible in NEI | GT5-Unofficial, NEI | [ROADMAP-ICBM I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine) |

## Decisions carried

D5, D10, D11, D12, D20 (bonus applied here), D21, D23, D30, D31 (warhead blocks), D34 (damage targets).

## Roadmap

[I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine) (registry, engine, T1-T2 blasts, warhead blocks, annihilation implementation), [I3](../../ROADMAP-ICBM.md#i3--the-silo-and-heavy-warheads-t3-nuclear) (nuclear, basic fallout), [I5](../../ROADMAP-ICBM.md#i5--endgame-warheads-and-systems) (antimatter, red matter, full fallout).

## Pack-side changes

None yet.

## Related areas

[03-missiles](../03-missiles/index.md) carries the warheads. [05-detection-defense](../05-detection-defense/index.md) owns the EMP. [06-blocks-items](../06-blocks-items/index.md) holds the antidote. [07-visuals-audio](../07-visuals-audio/index.md) holds the warhead models.

## Open questions

- The radius curve over fill (D23 allows sub-linear): to settle in the I1 warheads-and-fill sheet.
- Exact fallout numbers (budgets, decay rate): to settle in the I3 fallout sheet.
