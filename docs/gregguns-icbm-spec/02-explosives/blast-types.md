# Blast types

- **ICBM baseline:** [blast-types](../../icbm-spec/02-explosives/blast-types.md), [gas-and-status](../../icbm-spec/02-explosives/gas-and-status.md), [explosive-registry](../../icbm-spec/02-explosives/explosive-registry.md) §3
- **Design records:** [0018 §5-7](../../design/0018-icbm-program.md)
- **Decisions:** [D10](../DECISIONS.md#d10-in-detail), [D11](../DECISIONS.md#decided), [D12](../DECISIONS.md#decided), [D34](../DECISIONS.md#decided)
- **Pack mods:** GregGuns pipeline (`ExplosionRules`, `BlockBreakGuard`, `ExplosionEvent.Detonate`); no pack mod
- **Roadmap:** [I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine) (T1 and T2); T3 and up in I3 and I5

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| 26 explosives with first-free numeric IDs | Named entries in one registry; the T1-T2 set below | D12, `ICBM-02-18` |
| Colors gas | Dropped | D10 |
| Each type is its own `Blast` class | An engine-owned crater and damage ramp plus a small **effect** per type | One block-breaking path (D11) |
| Per-blast `size` config | Full-casing radius and damage below, with the fill curve of [warheads-and-fill](warheads-and-fill.md) | D23 |
| Gas works through walls only by cell flood fill | A radius check around the center (no flood fill) | Cheaper; the gas is a status effect, not terrain |

## The T1 and T2 set

"Full" is a full casing (T1 fill 5, T2 fill 20). Radius is in blocks. Center damage is in half hearts before armor and falls to zero at the radius; it applies only to the damaging types. The T2 "EV-IV" casing is built at EV.

| Type | Tier | Terrain | Radius (full) | Center damage (full) | Effect |
|---|---|---|---|---|---|
| condensed | T1 | grid crater | 9 | 1,200 | none beyond the ramp |
| shrapnel | T1 | grid crater | 10 | 1,200 | everything in the radius burns 5 s |
| anvil | T1 | grid crater | 8 | 1,200 | slowness III for 10 s on what is in the radius |
| incendiary | T1 | none | 12 | none | entities burn 10 s; fire on up to 400 solid-topped cells |
| debilitation | T1 | none | 12 | none | 30 s cloud: slowness II, weakness II, nausea |
| chemical | T1 | none | 12 | none | 30 s cloud: poison II |
| smoke | T1 | none | 10 | none | 30 s marker cloud, harmless |
| repulsive | T1 | none | 12 | none | pushes entities outward, strongest at the center |
| attractive | T1 | none | 12 | none | pulls entities inward |
| fragmentation | T2 | sphere crater | 14 | 3,000 | everything in the radius burns 5 s |
| contagious | T2 | none | 14 | none | 30 s cloud: poison IV, hunger II |
| sonic | T2 | none | 14 | none | nausea 10 s and slowness III 5 s; glass in the radius breaks |
| breaching | T2 | none | 12 | none | a 3x3 tunnel 24 deep, straight down, through anything of hardness 50 or less |
| thermobaric | T2 | sphere crater | 20 | 3,000 | none beyond the ramp (the widest conventional crater) |

- **Payloads (D56, D57).** What each type is made of, besides its casing, a Fuze and Plastic Explosive. Full recipes are in [warheads-and-fill](warheads-and-fill.md#recipes).

  | Type | Payload |
  |---|---|
  | condensed | TNT, dynamite |
  | shrapnel | TNT, a Steel Fragmentation Sleeve |
  | anvil | TNT, 12 lead plates (a dense mass) |
  | incendiary | 2 Napalm Canisters (Dispersal Canister plus gasoline) |
  | debilitation | 2 Dispersal Canisters, nitrous oxide |
  | chemical | 2 Dispersal Canisters, sulfuric gas, a few mustard seeds when HarvestCraft is present |
  | smoke | 2 Dispersal Canisters, creosote, coal dust |
  | repulsive | a Field Generator (HV) |
  | attractive | a Field Generator (HV), magnetic steel rods |
  | fragmentation | Industrial TNT, a Titanium Fragmentation Sleeve |
  | thermobaric | Industrial TNT, 2 Dispersal Canisters, kerosene |
  | sonic | Industrial TNT, 2 Emitters (EV) |
  | breaching | Industrial TNT, 2 Shaped-Charge Liner Cones, titanium rods |
  | contagious | Industrial TNT, 4 Bio Lab culture dishes, biomass |

  The gas, smoke and fuel-air fills are general industrial fluids in canisters, chosen by theme; no chemical-warfare agent is used or named (D56 as amended).
- **Damage targets (D34):** the damaging T1 types deal at least 1,200 at ground zero at full fill, and T2 at least 3,000, before armor. The unit test `WarheadBlastTest` pins the numbers, and scenario `icbm.blast.condensed` (and the T2 ones) kills a target in the zone. Partial fills deal `D * sqrt(fill / cap)`.
- **Special types** (gas, fire, push, sonic, breaching, smoke) are balanced on their own effect, not on the damage table (0018 §6).
- **Thermobaric** is a plain crater here; ICBM's threaded thermobaric energy model is a later refinement if play shows the need.
- **Dropped:** colors gas, and the T3 types (exothermic, endothermic, anti-gravitational, ender, EMP) arrive with I3-I4.

## Pipeline and protection

- The engine does the crater and the damage ramp for the types that have terrain; every block change goes through `ExplosionRules`, `BlockBreakGuard` and `ExplosionEvent.Detonate`, and obeys `icbm.terrainDamageT<n>` (D52).
- The effects add only what the engine does not: status effects, fire, push, sonic glass and the breaching tunnel. Every block an effect changes needs `icbm.terrainDamageT<n>` on and a `BlockBreakGuard.mayBreak` pass for the shooter; with the config off, only status effects, fire on entities, pushes and particles remain.
- An effect keeps no state of its own. Gas ticks off the blast's age, which the engine saves, so a gas cloud survives a save and reload.
- Gas particles are sent as vanilla particle packets, at most 24 per pulse every 10 ticks per cloud.

## Requirements

- GGS-ICBM-02-20: every type in the table is a registry entry with the table's radius and damage at full fill.
- GGS-ICBM-02-21: a blast type's block changes respect `icbm.terrainDamageT<n>` and the break guard.
- GGS-ICBM-02-22: gas effects end after 30 s and are bounded by the radius.
- GGS-ICBM-02-23: a conventional type meets its tier's D34 damage at full fill.

## Tests

`WarheadBlastTest` (catalog, targets), `WarheadMathTest` (curve against the registry definition), scenarios `icbm.blast.<type>` (one per type) and `icbm.blast.block_damage_off`.

## Open questions

None.
