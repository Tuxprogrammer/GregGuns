# 0011 · Machine policy: single blocks, multiblocks and the Ammo Press

- Status: Accepted (by default during the MVP push; revisit after MVP)
- Gate: D2 (amends [0002](0002-machine-deconfliction.md))
- Date: 2026-10-18

## Context
[0002](0002-machine-deconfliction.md) kept a handful of Techguns machines. It made the Reaction Chamber a multiblock only, and it replaced the Ammo Press with GT Assembler recipes. The owner has since set three rules:

1. **GTNH is a large-scale production pack.** GregGuns keeps Techguns-style single blocks. It also gives a multiblock to every machine players will use regularly, and at minimum to every machine that becomes a bottleneck.
2. **Multiblocks must have distinctive shapes.** No plain 3×3×3 cubes.
3. **Ammo is the exception to "use GT machines everywhere".** Ammo is central to a gun mod, so GregGuns has its own animated single-block Ammo Press and an ammunition manufacturing multiblock.. Amended by [0012](0012-retire-reaction-chamber-and-retort.md): the Reaction Chamber and Retort are retired

## Decision
- **The roster.** [machine-roster.md](../gregguns-spec/08-machines-production/machine-roster.md) is the single authoritative list of the machines GregGuns implements. For each one it gives the single block, the multiblock (or why there is none), the usage class (bottleneck, regular, or occasional and cosmetic), the tier range and the MTE IDs. The multiblock designs are linked from it.
- **Single blocks.** Every kept processing machine exists as a tiered GT single block. That includes the Reaction Chamber, which 0002 had made multiblock only.
- **Multiblocks.** Every machine classed as regular or bottleneck in the roster also gets a StructureLib multiblock. It shares the single block's recipe map, adds parallels and has a non-cube silhouette. Occasional and cosmetic machines (expected: Camo Bench, chests) stay single-block, and the roster says why.
- **Ammo Press reinstated.** This reverses 0002's "Replace":
  - The Ammo Press is a GregGuns single-block MTE with **visible animation** synced to recipe progress.
  - It runs a GregGuns **ammo recipe map**.
  - The ammunition manufacturing multiblock runs the same map, and is the flagship bottleneck machine.
  - Ammo recipes move from `assemblerRecipes` to that map ([06 recipes](../gregguns-spec/06-items/recipes.md)), keeping their inputs, EU/t and durations.
- **Everything else stays on GT.** Gun parts, armor and components keep using GT recipe maps, and GT's own multiblocks scale them. The roster records which GT multiblock parallelizes each GT map we rely on.

## Consequences
- M4 builds the Ammo Press, its recipe map and the Reaction Chamber single block. M6 builds the remaining multiblocks.
- More MTE IDs come from the 20100–20163 block ([metatileentity-framework.md](../gregguns-spec/08-machines-production/metatileentity-framework.md#gt5-unofficial-mte-id-reservation)). The block has room.
- NEI shows a GregGuns ammo tab, with both the Ammo Press and the multiblock as catalysts.

## Alternatives considered
- **Ammo on GT Assembler only, as 0002 had it:** rejected by the owner, because it loses immersion for the mod's most central loop.
- **Multiblocks only for bottleneck machines:** this is the minimum the owner set. The preference is that every regularly used machine has one.
