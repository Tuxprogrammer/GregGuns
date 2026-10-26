# 0019 · GregGuns structures on Ross128

- Status: Accepted (owner, 2026-10-25)
- Gate: D5 (amends [0005](0005-world-presence.md))
- Date: 2026-10-25

## Context
[0005](0005-world-presence.md) generates GregGuns structures in the Overworld only, and M8 enforces this with the allow-list `worldgen.structureDimensions`, which defaults to `[0]` ([structure-placement § Dimension gate](../gregguns-spec/11-worldgen/structure-placement.md#dimension-gate)). While planning the ICBM silos ([D38](../gregguns-icbm-spec/DECISIONS.md#d38-in-detail)), the owner pointed out that Ross128b and Ross128ba are an alternate, challenge-start route in GTNH. A player who starts there never sees the Overworld's structures. The silos already treat Ross128 as a place where any tier can turn up.

## Decision
1. **The M8 structures also generate on Ross128b and Ross128ba**: the Small Factory, Train Station, Gas Station, Medium House, Big Bunker, Military Base and the Alien Bug Nest (still off by default). The Aircraft Carrier and the Submarine generate there only if the dimension has ocean that passes their water check.
2. **The defaults change.** `structureDimensions` gains the two Ross128 dimension IDs, read from BartWorks' config (`ross128BID`, `ross128BAID`; 64 and 63 in the pack), never hard-coded.
3. **Placement rules carry over:** the same sparse grid, never cascading, and a land or water check on the footprint. The per-structure biome whitelists gain the Ross128 biomes that fit each structure.
4. **Loot on Ross128 is rolled, as for the silos.** Each structure rolls its own loot ceiling, weighted toward the low end (config). The range runs from MV, the normal surface ceiling, up to UHV, the top of GregGuns' own content. Defenders and spawners scale with the rolled tier. This was decided by the orchestrator to match D38; the owner can change it.

## Consequences
- M9 gains a polish item and an exit box for this ([ROADMAP](../ROADMAP.md#m9--self-contained-polish)).
- Scenarios cover generation, the land and water checks and the loot ceiling on both Ross128 dimensions.
- **Checked (M9):** the standard path is not enough. Galacticraft drops Forge world generators on its worlds, so both Ross128 dimensions have their own chunk-provider hook ([structure-placement](../gregguns-spec/11-worldgen/structure-placement.md#ross128b-and-ross128ba-design-record-0019-m9)). Original question: whether BartWorks' Ross128 chunk providers call the standard populate path that M8 hooks must be checked first. If one doesn't, that dimension needs its own hook, recorded as a deviation.

## Alternatives considered
- **Leave structures Overworld-only and rely on server owners editing `structureDimensions`.** Rejected: the challenge start is a pack route, and its players should get the content by default.
- **Fixed MV loot on Ross128.** Rejected: it doesn't match the silos' "any tier" rule for the same dimensions.
