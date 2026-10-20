# 0002 · Machine deconfliction

- Status: Accepted (by default during the MVP push; revisit after MVP). Machine roster, multiblocks and the Ammo Press reinstated by [0011](0011-machine-policy.md); the authoritative machine list is [machine-roster.md](../gregguns-spec/08-machines-production/machine-roster.md)
- Gate: D2. Amended by [0012](0012-retire-reaction-chamber-and-retort.md): the Reaction Chamber and Retort are retired
- Date: 2026-10-17

## Context
Techguns ships its own processing machines, and GT already covers most of their functions. Duplicating them clutters the pack and breaks GT's recipe economy. The specs are in [08-machines-production](../techguns-spec/08-machines-production/index.md) and [09-machines-utility](../techguns-spec/09-machines-utility/index.md).

## Decision
GT recipe maps replace every Techguns process that GT can do. A machine stays only when it is distinctive and has no GT equivalent, and it is rebuilt as a GT MetaTileEntity running on EU.

| Techguns machine | Decision | Replacement or rationale |
|---|---|---|
| Ammo Press | **Keep** (reinstated by [0011](0011-machine-policy.md)) | Animated GregGuns single block plus an ammunition manufacturing multiblock, on a GregGuns ammo recipe map |
| Metal Press | Drop | GT Bending Machine and Forming Press |
| Chem Lab | Drop | GT Chemical Reactor and Mixer |
| Fabricator | Replace | GT Assembler and Circuit Assembler recipes |
| Reaction Chamber | **Keep** | A GT single block plus a multiblock ([0011](0011-machine-policy.md)), on EU. Its focus, intensity and radiation gameplay is unique |
| Charging Station | Drop | GT chargers and battery buffers |
| Repair Bench | **Keep** | Armor and gun repair using GT materials. Single block plus the Armory Gantry multiblock ([0011](0011-machine-policy.md)) |
| Camo Bench | **Keep** | Cosmetic, with no GT equivalent |
| Ore Drill | Drop | GT miners and the GTNH ore system |
| Scrapper | Drop | Unfinished in Techguns; GT recycling covers it |
| Turret | **Keep** | Core to the defense theme. Single block plus the Rampart multiblock ([0011](0011-machine-policy.md)) |
| Explosive charges | **Keep** | Tier-gated. Revisit if they overlap GT mining explosives in play |
| Weapon and ammo chests | **Keep** | Low priority; decorative storage |
| Techguns ores, ingots, plates, wires, circuits | Drop | GT materials and parts through unification |
| Techguns fluids (oil, fuel, acid) | Drop | GT fluids |

## Consequences
- Production-chain work (M4) is mostly GT recipe registration.
- The kept machines need MetaTileEntity IDs reserved with the GTNH team.
- The Techguns machine GUIs and textures are only needed for the kept machines.

## Alternatives considered
- **Port every machine:** rejected, because it duplicates GT.
- **Drop every machine:** rejected, because it loses the Reaction Chamber's unique gameplay and the defensive turret.
