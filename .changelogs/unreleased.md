# Unreleased

Notes for the next release. Fold these into that release's changelog when it is cut.

## Silo complexes

- Some mess halls have a Vending Machine on the wall.
- **Complexes stay whole after world generation.** GregTech ore veins and stone layers (however tall, Fortunate One included), lava lakes, dungeons and other mods' generation leave no ore, stone or holes in rooms, halls, shafts or the escape ladder; the complex puts back whatever they changed, once. Ore inside the complex is lost; the rock between rooms keeps its ore.
- **Escape hatches, silo shafts and radar huts over water.** Under a river or lake, the escape ladder climbs inside a concrete riser to a trapdoor one block above the water, a silo shaft rises through the water to its mouth, and a radar hut stands on a foundation from the bed. On land the hatch stays a camouflaged trapdoor in the ground.

## Everything else

- **Structures carve their own site and bring their own foundation.** Every surface structure, the Military Base with all its tents, buildings, roads, fence and flag pad included, stands on solid ground filled down to the natural ground, with no hillside, tree or overhang left inside or over it, and a cut bank around it on slopes. The Big Bunker also gets a foundation under its buried floor.
- **NPC drops.** Every NPC drops a quarter to three quarters of a magazine of loose rounds for its gun (a Thompson gives 5 to 15 9mm bullets), a full tank, or parts of an energy gun. Zombie Farmers and Miners, Skeleton Soldiers, Zombie Pigman Soldiers and Super Mutants also drop ammo for their guns, Stormtroopers drop energy-weapon parts, and the Outcast has a full loot table. Mobs Info lists the drops.
- **Every NPC has a home.** Zombie Pigman Soldiers and Super Mutants spawn in the Nether, the mutants tougher the deeper in you go, with a rare Cyberdemon far out; Commando squads roam the End away from the dragon's island; one Military Base in five is a red base, where Dictator Dave holds his post by the red flag; and the Outcast guards silo complexes of LuV and up.
- Super Mutants stand on the ground; their legs no longer sink into the floor.
- **Reloading.** A magazine gun with no loaded magazine at hand loads one loose round per reload. The Revolvers, Golden Revolvers, Combat Shotgun and Sawed Off load one chamber at a time, a trigger pull stops the reload and fires what is loaded, and held without firing for 2 seconds they top themselves up (`guns.idleReloadDelayTicks`).

## Config

- **New keys:** `guns.idleReloadDelayTicks`, the Nether and End spawn tables (`npcs.netherSpawnDimensions`, `npcs.endSpawnDimensions`, `npcs.weightNether*`, `npcs.weightEndCommando`, `npcs.endMinDistance`, `npcs.bossSpacing`), `npcs.outcast` and `worldgen.redBaseChanceMilitaryBase`.
