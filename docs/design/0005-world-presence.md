# 0005 · World presence

- Status: Accepted (by default during the MVP push; revisit after MVP). Loot ceiling refined by [0007](0007-pack-loot-and-infernal-mobs.md); spawn mechanism by [0009](0009-gtnh-integration-defaults.md). Amended 2026-11-01 (beta, owner direction): Nether and End spawn tables, red Military Bases and Outcast command posts ([D353](../gregguns-icbm-spec/DECISIONS.md#d353-in-detail))
- Gate: D5
- Date: 2026-10-17

## Context
Techguns adds hostile NPC factions, natural spawning, structures and ore generation. See [10-entities-npcs](../techguns-spec/10-entities-npcs/index.md) and [11-worldgen](../techguns-spec/11-worldgen/index.md).

## Decision
- **No ore generation of any kind.** That includes Techguns ores, ore clusters and oil holes. The GTNH ore system owns ores.
- **NPCs:** ship every registered Techguns mob. The Alien Bug and the Cyberdemon are boss or late-game content and are gated by config. Natural spawning runs in three realms, each with its own table and dimension list, config-gated per faction. Caps are enforced in every realm (NPC-14, NPC-15, NPC-17), and mini-bosses persist (NPC-28).
  - **Overworld:** Techguns' Overworld mobs, with Techguns' spawn weights halved as a starting point.
  - **Nether (amended, D353):** the Zombie Pigman Soldier everywhere, the Super Mutants stepping up from Basic to Elite as the Nether danger rises, and the Cyberdemon as a rare boss deep in the Nether, alone and spaced apart. The Cyberdemon is also behind its `cyberdemon` config flag.
  - **End (amended, D353):** Commando squads, kept clear of the dragon's island.
- **Every egg-only mob has a home (amended, D353).** Dictator Dave commands red Military Bases (a share of all bases: red flag, pad and standards), placed once at his post; blue bases have no commander. The Outcast joins the elite posts of silo complexes with a LuV or higher ceiling.
- **Spawn architecture (amended, M7 N6):** there is no placeholder entity. The real NPC classes are registered as monster spawns, and a `CheckSpawn` handler gates dimension, faction switch, danger and caps (GGS-10-26). The danger level travels from the `CheckSpawn` position to the NPC for its equipment roll. This removes the Techguns placeholder and its side effects on Infernal Mobs, Mobs Info and EnderZoo, and gives Mobs Info real spawn data.
- **Structures:** the natural structures (bunkers, military camp, houses, the gas station, ships) generate in the Overworld only. Each is config-gated, uses whitelisted biomes, and never cascades worldgen (WGEN-04). The village bunker is not built.
- **Loot:** tier-appropriate. Loot never contains items above the tier the structure's location implies. By default that means LV–MV gear for surface structures.

## Consequences
- M7 and M8 build exactly this scope.
- Natural spawning in dimensions beyond the Overworld, Nether and End realms needs a new record.

## Alternatives considered
- **No natural world presence at all:** rejected, because the NPCs and camps are part of Techguns' identity.
