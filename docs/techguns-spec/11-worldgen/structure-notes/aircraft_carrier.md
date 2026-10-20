# Notes: Aircraft Carrier (`aircraft_carrier`)

- **Purpose:** word-only brief for the clean-room layout builder. Write `assets/gregguns/structures/aircraft_carrier.txt` in the [layout format](../../../gregguns-spec/11-worldgen/layout-format.md).
- **Sources:** [structure-catalog A7](../structure-catalog.md#a7-aircraft-carrier-aircraftcarriernew), [structure-loot](../../../gregguns-spec/11-worldgen/structure-loot.md), plan M8 section 2 and Q12 (no NPC spawn at generation).

## Footprint and orientation

- `size: 54 24 21` (x 54, y 24, z 21). Authored at rotation 0: the long axis runs east-west, the bow at low x, the stern at high x.
- **Ground/waterline:** the hull sits partly in the sea. The waterline is layer 2 and the flight deck is about layer 7, four blocks above the water. Set `ground` so the waterline layer is the anchor layer (layers 0 and 1 are below the surface), and say which value you chose. The generator places the ship on open ocean; nothing under the hull needs filling.
- **Front:** the bow (low x) is the front. There is no street door. Players board by climbing the hull or from a boat; the only real way inside is from the flight deck down into the island and the hold hatches.
- **Island side:** the island is on the north side (low z). Rotation turns everything together; give the flag as a mark so the generator rotates it for you (WGEN-10).

## Hull and decks

- **Hull shape:** about 54 long by 21 wide. The bow tapers to a point over its first dozen blocks or so; the stern is square and flat. Keep the sides sloping slightly inward below the waterline so it reads as a ship.
- **Hold level (layers 0 to 3):** steel plating shell. A forward hold and a mid hold, separated by bulkheads with doors. The forward-hold chests live here. Interior is air, with a few lamps.
- **Hangar level (layers 3 to 6):** a long open bay under the flight deck, lit by lamps. A couple of crafting tables along one wall, technical-looking floor panels, and an elevator-style shaft (a wide vertical opening framed in steel, about 6 blocks long, in the east third) that connects hangar to flight deck. Doors link the hangar to the hold and to the island base.
- **Flight deck (layer 7):** one continuous steel-gray slab over the whole length. A hazard-stripe rim at both ends. Leave a clear runway lane down the middle; mark it with a row of darker or hazard cells and a few lamps along the edges.
- **Catwalks:** one-block-wide ledges with iron-bar rails hang off the north and south edges of the deck at a few spots.

## Features

- **Island superstructure:** on the north side, covering roughly the middle-to-east third of the length (about 20 blocks long, 8 deep), rising to about layer 19. Three to four levels: a base level opening off the hangar, mid levels with offices and a ready room, and a bridge on top with a ring of glass windows and an iron-bar rail. Interior stairs and ladders connect the levels. Use `@facing` on ladders and `@stairs` on stairs.
- **Antenna mast:** an iron-bar mast on the island roof, reaching about layer 23.
- **Deck props:** parked aircraft or helicopter props on the flight deck, built from a few blocks each (steel body, glass cockpit, a short rotor line of iron bars for helicopters). Place two to four, away from the runway lane and the flag. Add a few crates and fuel-drum stand-ins (hazard-colored blocks) near the island.
- **Flag spot:** a camp flag stands on the flight deck forward of the island, in the open. Declare it with the mark `flag`; do not place a block. See "Spawners and flags".
- **Lighting:** lamps in every interior room, along the deck edges and on the island.

## Chests

| Mark | Count | Position | Category | Ceiling |
|---|---|---|---|---|
| `chest.vanilla` | 2 | forward hold, side by side | `gregguns:weaponchest_t2` | MV |
| `chest.vanilla` | 2 | hangar level, side by side near the middle | `gregguns:weaponchest_t2` | MV |
| `chest.vanilla` | 2 | island, one upper level | `gregguns:weaponchest_t2` | MV |
| `chest.reinforced` | 8 | spread over hold, hangar and island rooms, one or two per room | `gregguns:reinforcedchest_t2` | MV |
| `chest.weapon` | 12 | spread the same way, with a cluster of four in the island ready room | `gregguns:weaponchest_t2` | MV |

Totals: 6 vanilla, 8 reinforced, 12 weapon (26 chests). Techguns used its top loot tables here; GregGuns caps ships at MV and uses the `_t2` categories (not `_t3`). Chests face into their room with `@facing`. The sheet fixes only the six vanilla positions; the rest of the spread is your design, so keep chests in rooms, not on open deck.

## Spawners and flags

- **No spawner blocks.** NPCs come only from the camp flag.
- One `flag` mark on the flight deck. It is initialized with the NPC types `ArmySoldier` and `Commando` and a cap of 9, which is saved. The generator never spawns NPCs while generating (Q12); the flag's own timer does it, in loaded chunks only.
- **Spawn points** for the flag: nine, none over water. Put them on the deck near the flag, in the hangar, on two island levels, and in the hold. Declare them as marks of your own choosing (for example `spawn.npc`) if the format supports it; otherwise list them in your report so G10 can add them. They must be standing room, not inside walls.

## Palette roles

| Role | `PaletteKeys` key | Vanilla fallback | Where |
|---|---|---|---|
| Hull and island steel | `factory_wall` | iron block | hull shell, island walls |
| Deck slab | `plating` | iron block | flight deck |
| Hazard trim | `hazard` | yellow hardened clay | deck rim, lane edges, props |
| Floor panels | `factory_floor` | double stone slab | hangar and hold floors |
| Glass | `glass_pane` | glass pane | bridge, windows |
| Iron bars | `minecraft:iron_bars` | same | rails, mast |
| Lamps | `lantern` | glowstone | all rooms, deck edge |
| Stairs | `minecraft:stone_brick_stairs@stairs` | same | island stairs |
| Ladders | `minecraft:ladder@facing` | same | hold hatches, island, shaft |
| Doors | `minecraft:iron_door@door`, both halves | same | bulkheads (about 30 across the ship) |
| Crafting tables | `minecraft:crafting_table` | same | hangar |
| Air | `air` | air | every room |

No custom decor blocks are needed here.

## Rotation and known bugs

- WGEN-06: Techguns checked water on an unrotated box. The generator now checks the rotated footprint and the hull depth; the layout needs nothing special, but the footprint is 54 by 21 or 21 by 54.
- WGEN-10 and WGEN-11: Techguns passed an unrotated flag offset and lost the cap on reload. Both are fixed by the generator; give only the `flag` mark and let it rotate.
- WGEN-23: a short or truncated file disables this structure only; keep rows exact. WGEN-24: palette keys only, no Chisel names. WGEN-25: chests filled from the seeded draw.
- WGEN-04: write only inside the structure box; the stamper clips by window.

## Confidence

Good: size, deck height, island side and rough extent, the six fixed chest spots, the flag spot, the NPC list and cap, the mast. Soft: the hull taper curve, exact room divisions, the placement of the other 20 chests, the nine spawn points (the sheet lists them, these notes only name the areas), and what the aircraft props look like.
