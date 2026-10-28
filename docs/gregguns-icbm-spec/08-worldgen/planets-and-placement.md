# Planets and placement (D17, D38, D128)

## Grid

A second, sparse M8 grid dedicated to the complex: one cell per 64 x 64 chunks (1024 blocks), at most one attempt per cell, and only `siloComplexChancePercent` (default 25) of the attempts are kept, decided from the plan's seed. The grid keeps M8's spawn exclusion (no box within 8 chunks of chunk 0,0). The complex is 240 x 240 blocks and sits at a seeded offset in its cell, so it fits whatever the offset.

## Decision, once

The first populate window that overlaps a complex measures and records the decision in the dimension's `PlanLedger` (`StructureState`); later windows obey it. The decision runs these checks in order, and a failure is logged (`Silo complex skipped at dimension ...`) and recorded as rejected:

1. **Biome.** On the Overworld the biome types at the box center must include one of `siloComplexOverworldBiomes`. Other dimensions accept any biome.
2. **Terrain.** M8's land check (height spread of 8 or less over the columns the window can see, no water, no village blocks), so a planet whose terrain cannot host the layout (a dimension without solid ground) is skipped.
3. **Depth.** The recorded ground must be at least 30 so the level (24 blocks under the ground) sits above bedrock.

The level floor is the recorded ground minus 24. Silo shafts and the escape ladder rise to the ground height read in their own window, so uneven ground is fine.

## Never cascading

The plan and its raster are pure functions of the plan seed, so every window writes only its own columns through the clipped sink. A complex covers about 225 windows; none of them reads or writes another chunk. Unit tests prove it with the cascade harness pattern (writes outside the window, rejected writes, foreign chunk requests, duplicates, order independence) and the `icbm.complex.places` scenario proves it in a live world.

## Dimensions (D38)

The flag list is `siloComplexDimensions`, by `WorldProvider.getDimensionName()`, compared **case-insensitively**: Galacticraft's providers say "Moon", "Mars" and "Ross128b" where the table writes "moon" and "ross128b" (found in the first live run: with a case-sensitive match no planet ever hosted a complex). Default: Overworld and every planet of the table. A dimension absent from the list gets no complex. **Galacticraft drops Forge's world generators on every planet** (except a short whitelist of other mods), so the generator would never run there. `Ross128ProviderHook` therefore wraps the chunk provider of Ross128b and Ross128ba and of every non-Overworld world where the complex is enabled (`StructureGenerator.complexWanted`), and runs the generator right after the provider's own populate, in the same window; the call that comes second is skipped. The generator runs in a dimension when either the ordinary structures are allowed there or the complex is, and then only places what is allowed.

| Rocket tier | Dimension names | Ceiling (GT voltage index) |
|---|---|---|
| none | Overworld | EV (4) |
| T1 | moon | EV (4) |
| T2 | mars, phobos, deimos | IV (5) |
| T3 | ceres, callisto, europa, ganymed | LuV (6) |
| T4 | iojupiter, mercury, venus | ZPM (7) |
| T5 | enceladus, titan, miranda, oberon | UV (8) |
| T6 | proteus, triton | UHV (9) |
| T7 | pluto, haumea, makemake | UEV (10) |
| T8 | barnarda2, barnarda4, barnarda5, centauribb, tcetie, vega1 | UIV (11) |
| T9 | anubis, horus, maahes, neper, seth | UMV (12) |
| any | ross128b, ross128ba | rolled per complex, EV to UMV |

Asteroid and belt dimensions are absent: no ground for a shaft. `PlanetTable` holds this table.

## Data stick (nearest other complex)

The comms data stick holds the real block coordinates (x, z of the assembly bay) of the nearest other complex. `NeighborComplex.nearest` asks the placement grid (`StructurePopulator.complexPlanAt`) which of the cells within 3 of this one roll a complex at the configured chance, takes the anchor of each, and picks the nearest. It reads only the seed, the dimension and the grid: no world, no chunk, so it can never load or generate one. It cannot know that terrain will later reject that neighbor (skip-and-log), so a stick may name a place with no complex. No complex in range: no stick.

## Per-dimension checks

`icbm.complex.dim.<overworld|moon|mars|ross128b|ross128ba>` force the chance to 100 percent, pick cells that were never loaded, load the 17 x 17 chunk region through the dimension's own provider and check: the plan was decided; no dropped write, no PopulateGuard violation; no chunk loaded beyond the request plus the populate margin; trapdoor flush and ladder facing; shaft rings meet the terrain and an open bore sees the sky; radar huts, doorway and dish; the stick's coordinates; the planned deep posts; the armory frames each hold a gun and a valid wall. A cell the terrain rejects is noted and the next is tried. `icbm.complex.skips` runs water, too-low ground, a ridge repeated in every window and an off-whitelist biome through the harness and checks one logged reason each, nothing written, and flat ground placed. Run in the dev environment: Overworld, Moon, Mars, Ross128b and Ross128ba (Galacticraft, BartWorks). GalaxySpace and Amun-Ra are not in the dev environment, so their planets are untested; the table and the hook are by name and apply unchanged. Known limit: the terrain check samples only the first window that reaches the complex, so relief that appears only elsewhere in the 240 x 240 box is not seen.

## Config

`icbm.siloComplexEnabled`, `siloComplexDimensions`, `siloComplexChancePercent`, `siloComplexOverworldBiomes`. The complex also needs `modules.worldgen` and `worldgen.spawnStructures`.
