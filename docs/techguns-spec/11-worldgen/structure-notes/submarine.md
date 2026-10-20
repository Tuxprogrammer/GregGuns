# Notes: Submarine (`submarine`)

- **Purpose:** word-only brief for the clean-room layout builder. Write `assets/gregguns/structures/submarine.txt` in the [layout format](../../../gregguns-spec/11-worldgen/layout-format.md).
- **Sources:** [structure-catalog A8](../structure-catalog.md#a8-submarine-submarine), [structure-loot](../../../gregguns-spec/11-worldgen/structure-loot.md), plan M8 section 2.

## Footprint and orientation

- `size: 7 10 33` (x 7, y 10, z 33). Techguns declared 33 by 7 (WGEN-07, transposed); the data is 7 wide by 33 long. Use 7 by 33.
- **Long axis:** north-south (z). Authored at rotation 0 with the **bow at high z** and the stern at low z. The front is the bow.
- **Water placement:** the boat lies on the sea floor in open ocean. Layer 0 is the hull's lowest row and replaces the floor's top block (use `ground: 0`, so the boat rests in the sea-floor surface). Everything above the floor is water when generated; the generator checks that the columns under the footprint really are water (WGEN-08), so the layout does not place water. Interior rooms must be sealed with `air` so they stay dry.
- **Entry:** the conning tower hatch on top. A swimmer climbs the tower ladder and opens the trapdoor to reach the upper deck. There is no other opening.

## Shape

- **Hull:** a round section about 7 wide and 7 tall, running the whole 33 length. Sides curve in at top and bottom (a rounded cross-section, not a box).
- **Stern (z 0 to 1):** a vertical rudder fin and a horizontal fin row, with one hub block at the join.
- **Aft band (about z 5 to 7):** a ring of different plating around the hull as a visual band.
- **Bow (z 29 to 32):** tapers over four rows to a rounded nose.
- **Conning tower (sail):** about 3 long and 3 wide, rising to layer 9 toward the aft third (z 23 to 25). Hollow, with a ladder from the top hatch down to the upper deck. A trapdoor tops it. Keep the tower air-filled.

## Interior (two decks)

- **Lower deck (layers 1 to 3):** machinery and storage. Walls in a technical look. From the stern: a small engine room, a ladder up near the aft end, a mid compartment, and a forward torpedo room. A warning sign near the engine room door.
- **Upper deck (layers 4 to 5):** floored with a grating look. Crew quarters with three bunk beds (each a vanilla bed head and foot, `@facing` on both halves), a mess table, wooden furniture in a few spots. The command room sits under the conning tower, with the ladder at its center.
- **Bulkhead doors:** three between compartments on the upper deck, plus one for the engine room below. Both halves, `@door`.
- **Ladders:** one from the lower deck to the upper deck near the stern end; one from the upper deck to the tower hatch. `@facing`.
- **Lighting:** a few lamps, dim on purpose: two in the command room, one in the torpedo room, one in the engine room.

## Chests

| Mark | Count | Position | Category | Ceiling |
|---|---|---|---|---|
| `chest.vanilla` | 3 | upper deck crew quarters | `gregguns:weaponchest_t2` | MV |
| `chest.reinforced` | 2 | lower deck, aft (engine room) | `gregguns:reinforcedchest_t2` | MV |
| `chest.weapon` | 2 | lower deck, one mid and one forward | `gregguns:weaponchest_t2` | MV |
| `chest.weapon` | 2 | upper deck, quarters or command room | `gregguns:weaponchest_t2` | MV |

Totals: 3 vanilla, 2 reinforced, 4 weapon (9 chests). MV ceiling, `_t2` categories. Chests use `@facing` toward the room.

## Spawners and flags

None. No spawner, no camp flag, no NPCs.

## Palette roles

| Role | `PaletteKeys` key | Vanilla fallback | Where |
|---|---|---|---|
| Hull | `plating` | iron block | outer hull, fins, tower |
| Aft band and trim | `factory_wall` | iron block | band, bulkheads |
| Interior walls | `factory_floor` | double stone slab | lower deck walls |
| Upper deck floor | `brick` | stone brick | grating floor |
| Hub block | `hazard` | yellow hardened clay | rudder hub |
| Lamps | `lantern` | glowstone | interior |
| Ladders | `minecraft:ladder@facing` | same | two |
| Trapdoor | `minecraft:trapdoor@trapdoor` | same | tower top |
| Doors | `minecraft:iron_door@door` | same | four |
| Beds | `minecraft:bed@facing` | same | three |
| Furniture | `minecraft:planks` | same | table, shelves |
| Air | `air` | air | sealed interior |

## Rotation and known bugs

- WGEN-06, 07, 08: the generator handles water depth, the transposed size and the "no water here" case. Author 7 by 33 and nothing else.
- Beds need both halves with `@facing`; doors both halves `@door`; the trapdoor `@trapdoor`.
- WGEN-23 (exact rows), WGEN-24 (palette keys only), WGEN-25 (seeded chests).

## Confidence

Good: size, the 7-wide round hull, tower position and height, two decks, bed and chest counts, no flag. Soft: the exact room order on each deck, which chest sits where, the fin shapes and the nose curve. Whether the trapdoor kind and bed kinds exist in `RotationKind` should be checked against the format; use `@facing` or `none` if not.
