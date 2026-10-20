# 0005 · World presence

- Status: Accepted (by default during the MVP push; revisit after MVP). Loot ceiling refined by [0007](0007-pack-loot-and-infernal-mobs.md); spawn mechanism by [0009](0009-gtnh-integration-defaults.md)
- Gate: D5
- Date: 2026-10-17

## Context
Techguns adds hostile NPC factions, natural spawning, structures and ore generation. See [10-entities-npcs](../techguns-spec/10-entities-npcs/index.md) and [11-worldgen](../techguns-spec/11-worldgen/index.md).

## Decision
- **No ore generation of any kind.** That includes Techguns ores, ore clusters and oil holes. The GTNH ore system owns ores.
- **NPCs:** ship every registered Techguns mob. The Alien Bug and the Cyberdemon are boss or late-game content and are gated by config. Natural spawning is in the **Overworld only**, config-gated per faction, with Techguns' spawn weights halved as a starting point. Caps are enforced (NPC-14, NPC-15, NPC-17), and mini-bosses persist (NPC-28). The Cyberdemon is also behind its `cyberdemon` config flag.
- **Nether-only mobs (Pigman Soldier, Cyberdemon): egg, command or camp only; no natural spawn.**
- **Spawn architecture (amended, M7 N6):** there is no placeholder entity. The real NPC classes are registered as monster spawns, and a `CheckSpawn` handler gates dimension, faction switch, danger and caps (GGS-10-26). The danger level travels from the `CheckSpawn` position to the NPC for its equipment roll. This removes the Techguns placeholder and its side effects on Infernal Mobs, Mobs Info and EnderZoo, and gives Mobs Info real spawn data.
- **Structures:** the natural structures (bunkers, military camp, houses, the gas station, ships) generate in the Overworld only. Each is config-gated, uses whitelisted biomes, and never cascades worldgen (WGEN-04). The village bunker is not built.
- **Loot:** tier-appropriate. Loot never contains items above the tier the structure's location implies. By default that means LV–MV gear for surface structures.

## Consequences
- M7 and M8 build exactly this scope.
- Dimension support beyond the Overworld needs a new record.

## Alternatives considered
- **No natural world presence at all:** rejected, because the NPCs and camps are part of Techguns' identity.
