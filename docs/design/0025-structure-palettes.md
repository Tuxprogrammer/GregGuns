# 0025 · Structure palettes: roles by look, one family per mix

- Status: Accepted (by default, 2026-10-30; owner report "the color palette of blocks is very jarring")
- Gate: D6 (art direction, [0006](0006-art-direction.md)); D5 (world presence, [0005](0005-world-presence.md))
- Date: 2026-10-30

## Context

The surface structures (Small Factory, Train Station, Gas Station, Medium House, Big Bunker, Aircraft Carrier, Submarine and the Military Base parts) draw their blocks from palette roles ([chisel-palette](../gregguns-spec/11-worldgen/chisel-palette.md)). The first role table took meta 0 of a Chisel block for each role, chosen by the block's name rather than its look:

- `hazard` was Chisel's radiation warning sign, so every hazard rim, pump and tank cap repeated a sign icon;
- `plating` was Chisel's hex plating, a near-black plate with an animated glowing overlay, on the carrier deck, the submarine hull and the Gas Station roof;
- `factory_wall` was Chisel's "dots" plate, a purple-brown panel with pink rivets, on the carrier hull and factory walls;
- `factory_floor` was Chisel's X-braced scaffold; `brick` was Chisel's dark mason bricks; `lantern` was a flame lantern.

The layouts then mixed unrelated roles per cell (`factory_wall*3 | concrete*1`, `concrete*6 | brick*3 | factory_wall*1`, `brick | concrete`, a roof of `stone_stairs | sandstone_stairs`), which turned walls and roofs into a speckle of beige, near-black and purple. The owner called it jarring in fresh terrain. The [Techguns spec](../techguns-spec/11-worldgen/structure-catalog.md) describes each structure with one material character: a rusty steel shed on concrete, a weathered brick station under a light roof, a gray stone house under a dark red roof, a gray concrete bunker under camo, a gray steel ship.

## Decision

1. **A role is a look, not a block.** Each role is a chain of candidates, best first: a Chisel variant, sometimes a GregTech block, then a vanilla stand-in of the same look (gray steel stays gray steel down the chain). The first registered candidate wins; Chisel is read by registry name, and a Chisel meta Chisel defines no variation for is skipped (`ChiselVariants`).
2. **Every role belongs to one family** (concrete, masonry, red brick, steel, rust, hazard, camo, ...), declared in the role table. A layout's weighted entry may draw only from one family, plus air for gaps: weathering is `concrete*3 | concrete_dark*1` or `red_brick*3 | red_brick_worn*1`, never concrete with steel.
3. **A role key takes its own meta.** `concrete#4` is rejected at load: meta 4 means something else for every block of the chain. A different look is a different role.
4. **No signs, icons or animated overlays as building material.** Hazard is Chisel's hazard stripe; lights are Chisel's light-stone panel (the same light the ICBM silo complex uses).
5. **The block a role places in the pack is not one GregTech's stone layers overwrite** (stone, stained clay, cobblestone, GT granites and stones): sandbags are plain hardened clay, containers Chisel's red hempcrete. Ground fill under the train tracks may stay cobblestone.
6. **Per structure** (the role tables are in the structure notes and [chisel-palette](../gregguns-spec/11-worldgen/chisel-palette.md#roles-and-families)):

| Structure | Character (Techguns spec) | Palette now |
|---|---|---|
| Small Factory (and its camp copy) | rusty steel shed on a concrete base | rust walls, concrete base with a weathered band, steel grate floors |
| Train Station | weathered brick house, light roof with holes | red brick with a sooty share, gray stone-brick roof and slabs with gaps |
| Gas Station | asphalt forecourt, brick shop, steel canopy | asphalt apron, concrete panels and pillars, gray stone-brick shop, steel canopy with a whole iron-bar railing, hazard-stripe pumps |
| Medium House | gray stone house, dark red roof | gray stone bricks with a worn share and stone-panel corner posts, nether-brick roof, brown carpets |
| Big Bunker | gray concrete under an earth berm and camo | concrete with panel and weathered shares, plain-clay sandbags, leaf camo |
| Aircraft Carrier | gray steel ship, hazard rims | steel-plate hull and island, plated deck, grate floors, hazard-stripe rims |
| Submarine | dark steel hull | dark steel hull, steel-plate band, plated interior, grate floors |
| Military Base parts | concrete, steel, wood, camo, red containers | as the families above; camo by biome (leaves, Chisel desert camo, snow) |

The ICBM silo complex keeps its own palette code ([palette.md](../gregguns-icbm-spec/08-worldgen/palette.md)); it already builds rooms from three different blocks per role chain and was not part of this report.

## Consequences

- `PaletteKeys` is a role table with families; `lantern` is now `lamp`; new roles `concrete_panel`, `concrete_dark`, `asphalt`, `brick_worn`, `red_brick`, `red_brick_worn`, `rust_wall`, `hull_dark`, `crate`, `camo_woodland`, `camo_desert`, `camo_snow`, `sandbag`, `container`, `brick_trim`; `lab_wall` and `iron_plate` are gone (the house corner posts were Chisel iron storage blocks: pink-gray, and each mined into iron).
- `LayoutRegistry` rejects a role key with a meta. `StructurePaletteRulesTest` checks every shipped layout and role against items 2 to 5.
- Steel stand-ins are smooth stone slabs, not iron blocks, so a stripped install does not hand out iron.
- Structures look the same in shape; only blocks change. Existing worlds keep what they generated.
- `worldgen.palette_showcase` and `worldgen.palette_showcase_cam` (dev and test hooks only, never in `test all`) build every structure on flat lots and frame fixed cameras, so later palette work can be compared before and after.

## Alternatives considered

- **Keep the blocks, drop only the random mixes.** The radiation signs, hex overlay and purple plate were jarring on their own.
- **GregTech concrete everywhere.** GT's light concrete is tan and its dark concrete olive; next to Chisel's gray steel it reads warmer and muddier than Chisel's concrete. It stays the second candidate.
- **GregGuns' own concrete, lamp, sandbag and camo-net blocks.** Area 07 has none; new decorative blocks were ruled out for the silo complex too (prefer the pack's blocks).
- **No randomness at all.** In-family weathering keeps the Techguns "worn" feel without the speckle.
