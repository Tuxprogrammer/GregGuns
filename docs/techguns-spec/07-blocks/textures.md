# 07-blocks: texture catalogue (`textures/blocks/`, all 163 files)

Every PNG in `assets/techguns/textures/blocks/`, including the ones used by machine blocks (owned by 08-machines-production and 09-machines-utility, which refer here).

**Columns.**

- **Size:** pixel size.
- **Palette:** the first colours listed are the dominant 4-bit-quantised colours, measured from the files. Named accent colours (e.g. "yellow #c0b030", "LED #00e000") are visual estimates.
- **Anim:** frame count and frame time from `.mcmeta`.
- **Use:** the block, meta or renderer that loads the texture. "unused" means no live code loads it.

**Resolution.** Most block textures are 32×32 (a 2× "HD" style). Model textures are drawn at 2× their model's declared UV size.

**Projected-layout textures** (ladders, lamps, lanterns) are sampled by block-space position (see [block-rendering-and-models.md](block-rendering-and-models.md) §2). Paint each texel where the geometry sits on a 32×32 face grid.

**CTM sheet notation.** `-ctm` sheets use the 2×2 tile layout from [connected-textures.md](connected-textures.md):

- **A** (top-left): no border
- **B** (top-right): left and right borders
- **C** (bottom-left): top and bottom borders
- **D** (bottom-right): corner notches only

The base file is the bordered "alone" look.

---

## 1. Shipping containers (`container`, metas 0–5)

| Path | Size | Depicts / layout | Palette | Anim | Use |
|---|---|---|---|---|---|
| `textures/blocks/container_red.png` | 16×16 | Shipping-container side: vertical corrugation ribs (one bright rib left of centre) inside a 2-px frame. Lighter outer edge, dark inner shadow. | #602020, #b04040, #903030 | – | container:0 base |
| `textures/blocks/container_red-ctm.png` | 32×32 | CTM sheet of the same corrugation. A: ribs only; B: frame on left/right; C: frame top/bottom; D: corner notches. | #903030, #803030, #802020 | – | container:0 |
| `textures/blocks/container_green.png` | 16×16 | as red, dark green | #103010, #204020, #407040 | – | container:1 |
| `textures/blocks/container_green-ctm.png` | 32×32 | as red CTM, green | #305030, #204020, #609060 highlights | – | container:1 |
| `textures/blocks/container_blue.png` | 16×16 | as red, navy blue | #202060, #303080, #4040b0 | – | container:2 |
| `textures/blocks/container_blue-ctm.png` | 32×32 | as red CTM, blue | #303090, #303080, #202060 | – | container:2 |
| `textures/blocks/container_orange.png` | 16×16 | as red, orange/ochre | #704010, #905010, #d08020 | – | container:3 |
| `textures/blocks/container_orange-ctm.png` | 32×32 | as red CTM, orange | #905010, #a06010, #704010 | – | container:3 |
| `textures/blocks/container_scifi1.png` | 32×32 | Futuristic container: light-grey steel plate. A chamfered octagonal recessed hatch in the centre with a vertical seam, ringed by yellow/black hazard-stripe segments. Four round bolt sockets in the corners. | #909080, #706060, #404040, yellow #e0d020 | – | container:4 base |
| `textures/blocks/container_scifi1-ctm.png` | 64×64 | CTM sheet: grey plating with a vertical seam. The borders are hazard-striped strips with a light bevel (exact A–D layout, see connected-textures). | #706060, #606060, #303030 + yellow | – | container:4 |
| `textures/blocks/container_scifi_rusty.png` | 32×32 | as `container_scifi1`, rusted orange-brown, with scorched hazard stripes | #907050, #806040, #403020 | – | container:5 base |
| `textures/blocks/container_scifi_rusty-ctm.png` | 64×64 | rusty CTM sheet, same layout | #806040, #906040, #907050 | – | container:5 |

## 2. Concrete (`concrete`, metas 0–6) and concrete hole

| Path | Size | Depicts / layout | Palette | Anim | Use |
|---|---|---|---|---|---|
| `textures/blocks/concrete01.png` | 32×32 | smooth light concrete, fine grey-beige noise | #b0b0a0, #b0a0a0, #a0a090 | – | concrete:0 "Light"; also side of TGMonsterSpawner:1 |
| `textures/blocks/concrete02.png` | 32×32 | the same, darker grey | #808070, #807070, #908080 | – | concrete:1 "Dark" |
| `textures/blocks/concrete03.png` | 32×32 | dark mottled concrete with vertical streaks | #706060, #707060, #606050 | – | concrete:5 (lang "Light Noise", but the art is dark) |
| `textures/blocks/concrete04.png` | 32×32 | light mottled pinkish-grey concrete | #b0a0a0, #a0a090, #a09090 | – | concrete:6 (lang "Dark Noise", but the art is light) |
| `textures/blocks/concrete_r.png` | 32×32 | Light concrete with an octagon-cornered recessed outline (dark line about 3 px in) and a vertical steel lattice beam down the centre (two bright rails with dark rungs, 10 px wide) | #404040, #808080, #b0b0a0 | – | concrete:2 base |
| `textures/blocks/concrete_r-ctm.png` | 64×64 | CTM sheet: the beam continues across tiles; the recessed-groove border is split per A–D | #b0b0a0, #404040, #a0a090 | – | concrete:2 |
| `textures/blocks/concrete_r02.png` | 32×32 | dark-concrete version of `concrete_r` | #202020, #404040, #505050 | – | concrete:3 base |
| `textures/blocks/concrete_r02-ctm.png` | 64×64 | dark CTM sheet | #808070, #505050, #707070 | – | concrete:3 |
| `textures/blocks/concrete_old2-ctmv.png` | 64×64 | CTMV column sheet of weathered concrete. TL: single block (dark drip rim at top, rust-brown stain at bottom). TR: column top (rim, fading down). BL: column middle (plain). BR: column bottom (rust stain). | #b0b0a0, #a0a090, rust #604030 | – | concrete:4 sides |
| `textures/blocks/concrete_old2-top.png` | 32×32 | plain light concrete (top and bottom of the column) | #b0b0a0, #b0a0a0 | – | concrete:4 top/bottom |
| `textures/blocks/concrete_hole.png` | 32×32 | light concrete with a large ragged black hole filling about 70% of the face | #000000, #101010, #b0b0a0 | – | top of TGMonsterSpawner:1 (Zombie Hole) |

## 3. Metal panels (`metalpanel` 0–7, `metalpanel2` 0–6) and steel frame glass

| Path | Size | Depicts / layout | Palette | Anim | Use |
|---|---|---|---|---|---|
| `textures/blocks/metalpanel_rusty.png` | 32×32 | weathered grey-brown steel plate, pitted, with 4 small bright rivets 3 px in from the corners | #404030, #505040, #707060 | – | metalpanel:0 |
| `textures/blocks/metalpanel_rusty2.png` | 32×32 | the same, heavier brown rust | #403020, #302010, #504030 | – | metalpanel:1 |
| `textures/blocks/metalpanel_hazard.png` | 32×32 | the same plate with diagonal yellow/black hazard stripes (about 6 px bands, rising to the right) | #202010, yellow #c0b030, #101010 | – | metalpanel:2 |
| `textures/blocks/metalpanel_dark.png` | 32×32 | near-black steel plate with 4 rivets | #101010, #202020, #202010 | – | metalpanel:3 |
| `textures/blocks/metalgrate_rusty.png` | 32×32 | Grate: a 2-px weathered frame around a black void with 6 vertical grey bars | #000000, #101000, grey bars #808070 | – | metalpanel:4 |
| `textures/blocks/metalgrate_rusty2.png` | 32×32 | grate with brown-rust bars | #000000, #100000, #706050 | – | metalpanel:5 |
| `textures/blocks/metalgrate_hazard.png` | 32×32 | grate with yellow hazard tint on the frame | #000000, #101000, yellow edges | – | metalpanel:6 |
| `textures/blocks/metalgrate_dark.png` | 32×32 | grate with dark frame and dull bars | #000000, #101010, #202020 | – | metalpanel:7 |
| `textures/blocks/metalpanel2_rusty.png` | 32×32 | rusty plate with 4 large domed rivets at the corners | #605040, #504030, #706050 | – | metalpanel2:0 base |
| `textures/blocks/metalpanel2_rusty-ctm.png` | 64×64 | CTM sheet: four plates with seams; rivets only where the plate corners meet | #605040, #706050, #807060 | – | metalpanel2:0 |
| `textures/blocks/metalpanel2_hazard.png` | 32×32 | diagonal yellow/black hazard plate, scuffed, 4 domed rivets | #403020, yellow #a09020, #000000 | – | metalpanel2:1 base |
| `textures/blocks/metalpanel2_hazard-ctm.png` | 64×64 | hazard CTM sheet with continuous stripes and rivets at the seams | #403020, #908020, #302020 | – | metalpanel2:1 |
| `textures/blocks/metalpanel16_largeborder.png` | 16×16 | dark grey panel with a 2×2 grid of recessed square sockets separated by bevelled bars | #303030, #505050, #404040 | – | metalpanel2:2 base |
| `textures/blocks/metalpanel16_largeborder-ctm.png` | 32×32 | CTM sheet: A is a light silver plate with faint cross lines; borders are bevelled silver bars with small square bolts at the junctions | #a0a0a0, #404040, #b0b0b0 | – | metalpanel2:2 ("Silver Bars") |
| `textures/blocks/steelframe_dark.png` | 16×16 | as `metalpanel16_largeborder`, darker | #202020, #303030, #101010 | – | metalpanel2:3 base |
| `textures/blocks/steelframe_dark-ctm.png` | 32×32 | CTM sheet: dark diamond-plate centre with bevelled frame bars | #404040, #303030, #202020 | – | metalpanel2:3 ("Dark Bars") |
| `textures/blocks/steelframe_blue.png` | 16×16 | blue-grey steel frame with four bright white square studs | #101010, #d0d0d0, #203030 | – | metalpanel2:4 base |
| `textures/blocks/steelframe_blue-ctm.png` | 32×32 | CTM sheet: brushed blue-steel panels with frame bars | #708090, #607080, #606070 | – | metalpanel2:4 ("Shiny") |
| `textures/blocks/steelframe_scaffold.png` | 16×16 | dark square frame with an X cross-brace inside | #303030, #202020, #404040 | – | metalpanel2:5 base |
| `textures/blocks/steelframe_scaffold-ctm.png` | 32×32 | CTM sheet: X-lattice continuing across tiles | #202020, #404040, #505050 | – | metalpanel2:5 ("Dark") |
| `textures/blocks/metalpanel_techgrid-v9.png` | 48×48 | V9 sheet (3×3 tiles of 16 px): light grey concrete-like plate with faint embossed overlapping squares that form one large pattern over 3×3 blocks | #a0a0a0, #b0b0b0, #909090 | – | metalpanel2:6 ("Silver") |
| `textures/blocks/steelframe_glass.png` | 32×32 | black 3-px frame around a semi-transparent smoky black pane (average alpha about 85/255) | #000000, #101010 | – | unregistered `steelframe_glass` block (unused) |

## 4. Camo nets, sandbags, tent, flag, air marker, invisible

| Path | Size | Depicts / layout | Palette | Anim | Use |
|---|---|---|---|---|---|
| `textures/blocks/camoNet.png` | 32×32 | Woodland camo net on a transparent background (33%): a dark brown cord grid with clusters of green leaf scraps | #304020, #709050, #405030, #80a060 | – | camoNetPane:0, camoNetRoof:0 |
| `textures/blocks/camoNet_snow.png` | 32×32 | the same net with white/grey scraps | #d0d0d0, #c0c0c0, #a0a0a0 | – | camoNetPane:1, camoNetRoof:1 |
| `textures/blocks/camoNet_desert.png` | 32×32 | the same net with sand/khaki scraps | #d0c080, #e0d0a0, #303020 | – | camoNetPane:2, camoNetRoof:2 |
| `textures/blocks/sandbags-side.png` | 32×32 | three courses of rounded olive-khaki sandbags, offset like brickwork, with dark gaps | #505040, #404030, #000000 | – | sandbags horizontal faces |
| `textures/blocks/sandbags-top.png` | 32×32 | bags seen from above in a staggered brick pattern | #505040, #606050, #000000 | – | sandbags top/bottom |
| `textures/blocks/tent.png` | 32×32 | plain beige canvas | #c0b090, #b0a080 | – | unused |
| `textures/blocks/flagBlock.png` | 32×32 | grey cloth background with a waving red/white/blue striped flag carrying a black star | #e0e0e0, red #e04040, blue #4040e0 | – | unused |
| `textures/blocks/airMarker.png` | 32×32 | transparent with a 2-px dark grey frame and the outlined letters "AIR" | #303030, #404040, #707070 | – | airMarker |
| `textures/blocks/invisible.png` | 16×16 | fully transparent | – | – | camo-net pane top/edge strip; oreDrill:10; machineMultiBlock:15 (08) |

## 5. Lamps, lanterns, ladders, door

| Path | Size | Depicts / layout | Palette | Anim | Use |
|---|---|---|---|---|---|
| `textures/blocks/lamp01.png` | 32×32 | **Projected layout.** Black field. A bright yellow, horizontally scan-lined square at px about 9–23 (the lens front) and four yellow bars about 3–4 px thick centred on each edge, at rows/cols about 2–6 and 26–30 (the lens sides). Everything else stays dark: the socket. | #101010, yellow #f0e070, #f0d050, #f0f0a0 | – | lamp01:0–6 |
| `textures/blocks/lamp02.png` | 32×32 | same layout, white glow on a light grey surround | #e0e0e0, #c0c0c0, #f0f0e0 | – | lamp01:8–14 |
| `textures/blocks/lantern01_side.png` | 32×32 | **Projected.** Black with a wide yellow scan-lined band across rows about 9–23 (the lens body), slightly brighter in the middle third | #101010, #f0f0a0, #f0e050 | – | lamp01:7 sides |
| `textures/blocks/lantern01_top.png` | 32×32 | black with faint nested square outlines (post and ring caps) | #101010, #202020 | – | lamp01:7 top/bottom |
| `textures/blocks/lantern02_side.png` | 32×32 | white version of `lantern01_side` | #e0e0e0, #e0e0d0, #c0c0c0 | – | lamp01:15 sides |
| `textures/blocks/lantern02_top.png` | 32×32 | light grey with a small square in the centre | #c0c0c0, #d0d0d0, #e0e0e0 | – | lamp01:15 top/bottom |
| `textures/blocks/metalLadder.png` | 32×32 | **Projected.** Blue-grey steel: rails in the 4-px left and right columns and four rung bands across. The three gaps between rungs (rows about 6–10, 14–18, 22–26) are transparent (28%). | #607070, #506060, #708090 | – | ladder01 variant 0 |
| `textures/blocks/metalLadderRusty.png` | 32×32 | same layout, dark brown-orange rust | #201000, #402010, #503010 | – | variant 1 |
| `textures/blocks/metalLadderShiny.png` | 32×32 | same layout, bright silver-white | #d0d0d0, #e0e0e0, #f0f0f0 | – | variant 2 |
| `textures/blocks/metalLadderCarbon.png` | 32×32 | same layout, near-black | #101010, #202020 | – | variant 3 |
| `textures/blocks/ladder01.png` | 16×16 | old 16-px grey ladder with transparent rung gaps | #707070, #606060 | – | unused |
| `textures/blocks/ladder01_inv.png` | 32×32 | UV skin for `ModelLadder01`: 2×16×2 rails at (0,0) and 12×2×2 rungs at (0,19), grey, rest transparent | #808080, #909090 | – | passed to the ladder and lamp renderers but never bound (unused) |
| `textures/blocks/door01.png` | 32×32 | plain grey-green weathered steel | #708070, #809080, #707070 | – | door top/bottom faces |
| `textures/blocks/door01_upper.png` | 32×32 | Upper door leaf: steel slab with a round-topped recessed panel framed by a ring of small red-brown rivets, a vertical hinge strip at the left with a horizontal strap, and a small square plate low in the panel | #606060, #809080, rivets #602020 | – | upper half (mirrored copy auto-generated) |
| `textures/blocks/door01_lower.png` | 32×32 | Lower leaf: a round-bottomed recessed panel continuing from the upper one, rivets, hinge strip, grime at the bottom | #404030, #505040, #809080 | – | lower half |

## 6. Radar Screen and Control Panel

| Path | Size | Depicts / layout | Palette | Anim | Use |
|---|---|---|---|---|---|
| `textures/blocks/screen_front.png` | 32×32 | brown-grey metal casing with a large black rounded-rectangle screen (px about 2–30) and a seam line at mid-height on the frame edges | #101010, #000000, #706060 | – | screen front |
| `textures/blocks/screen_overlay_front.png` | 32×256 | 8 frames, each 32×32. Transparent outside a rounded CRT matching the screen. Green radar: concentric rings, a centre dot, horizontal scanlines, and a bright sweep blip that moves around the rings from frame to frame. | #001000–#004000, #00f000 | 8 × 2 ticks (16-tick loop) | screen overlay, front, pass 1 |
| `textures/blocks/screen_side.png` | 32×32 | casing with a round speaker grille at upper left, a button/vent block at lower left and a horizontal seam | #403030, #504030, #706060 | – | screen sides/back |
| `textures/blocks/screen_top.png` | 32×32 | plain brown-grey casing with a slight bevel | #403030, #504030 | – | screen top/bottom; also the `basicMachine` break-particle icon (08/09) |
| `textures/blocks/controlpanel_front.png` | 32×32 | Console face: a black display window at top-left (px about 4–20 × 4–12) and a 2×3 grid of black keys at top-right. Below: a keypad of white/grey/red buttons, a rotary knob, a slider, toggle switches and a red push button at bottom-right. | #000000, #403030, #706060, red #c02020 | – | controlpanel front |
| `textures/blocks/controlpanel_overlay_front.png` | 32×256 | 8 frames. Transparent except: a green oscilloscope trace inside the display window (px 4–20 × 4–12) that changes each frame; blinking red/green/yellow LEDs at about (23–28, 3–9); a steady green LED pair at about (22–25, 27–29) | #002000, #00f000, red, yellow | 8 × 2 ticks | controlpanel overlay |
| `textures/blocks/controlpanel_side.png` | 32×32 | like `screen_side` but mirrored (grille upper right, vents lower left), no seam | #403030, #504030 | – | controlpanel sides/back |
| `textures/blocks/controlpanel_top.png` | 32×32 | plain casing | #403030, #504030 | – | controlpanel top/bottom |

## 7. Ores

| Path | Size | Depicts / layout | Palette | Anim | Use |
|---|---|---|---|---|---|
| `textures/blocks/oreTitanIron.png` | 16×64 | 4 frames: vanilla-style grey stone with pale whitish-cyan metallic flecks that shimmer between frames | #707070, #808080, #b0c0c0, #c0d0d0 | 4 × 8 ticks | oreTitanIron:0 |
| `textures/blocks/oreUranium.png` | 16×64 | 4 frames: stone with bright green flecks that pulse | #707070, #708070, green #40c040 | 4 × 8 ticks | oreTitanIron:1 |
| `textures/blocks/oreCopperBlock.png` | 16×16 | stone with orange-copper blotches | #707070, #808080, #c07030 | – | `oreCopper` block (01-core) |
| `textures/blocks/oreTinBlock.png` | 16×16 | stone with pale blue-white flecks | #707070, #808080, #a0c0c0 | – | `oreTin` block (01-core) |
| `textures/blocks/oreLeadBlock.png` | 16×16 | stone with dull blue-violet flecks | #707070, #808080, #7080b0 | – | `oreLead` block (01-core) |

### Ore-cluster overlays

Greyscale on a transparent background. They are tinted at render time by the type's RGB and drawn over stone, netherrack, end stone or bedrock.

| Path | Size | Depicts | Transparent | Use |
|---|---|---|---|---|
| `textures/blocks/oreClusterNew1.png` | 32×32 | three large elongated shiny ore chunks plus several small specks | 62% | overlay type 0 (iron, gold, redstone, copper, tin, lead, silver, nickel, uranium, quartz) |
| `textures/blocks/oreClusterNew2.png` | 32×32 | about nine small rounded nuggets scattered evenly | 66% | type 1 (coal, lapis, osmium, aluminium, platinum, titanium iron) |
| `textures/blocks/oreClusterNew3.png` | 32×32 | one large faceted crystal in the centre plus four small cubes toward the corners | 76% | type 2 (diamond, emerald, certus, glowstone) |
| `textures/blocks/oreClusterNew4.png` | 32×32 | a large blotchy square puddle filling most of the face, with a ragged dark rim | 42% | type 3 (oil hole) |

### Legacy ore-cluster art

All unused. The `registerIcon` calls are commented out. Bases have transparent holes; overlays fill them.

| Path | Size | Depicts |
|---|---|---|
| `textures/blocks/oreCluster.png` | 32×32 | stone base with large oval holes |
| `textures/blocks/oreCluster1.png` | 32×32 | overlay: white/grey chunky ore lumps |
| `textures/blocks/oreCluster_1.png` | 32×32 | stone base, many small holes |
| `textures/blocks/oreCluster_11.png` | 32×32 | overlay: dark curled nuggets |
| `textures/blocks/oreCluster_2.png` | 32×32 | stone base, one big central hole and four small ones |
| `textures/blocks/oreCluster_21.png` | 32×32 | overlay: white crystal and corner cubes |
| `textures/blocks/oreCluster_3.png` | 32×32 | netherrack-red base with holes |
| `textures/blocks/oreCluster_31.png` | 32×32 | overlay: black/white shards |
| `textures/blocks/oreCluster_4.png` | 32×32 | dark grey rim with one huge hole |
| `textures/blocks/oreCluster_41.png` | 32×32 | overlay: grey blob |
| `textures/blocks/oreCluster_5.png` | 32×32 | netherrack-red base with a central hole and four corner holes |

## 8. Bug nest, bio blob

| Path | Size | Depicts / layout | Palette | Anim | Use |
|---|---|---|---|---|---|
| `textures/blocks/bugnestblock.png` | 16×16 | coarse sandy beige grain | #b0a070, #c0b080, #a09070 | – | bugnest:0; sides of TGMonsterSpawner:0 |
| `textures/blocks/bugnestblock_hole.png` | 16×16 | the same sand with a large irregular black hole | #000000, #101010, #b0a070 | – | top of TGMonsterSpawner:0 (Bug Hole) |
| `textures/blocks/bugnesteggs.png` | 16×64 | 4 frames: a sand-coloured rim around clustered glossy eggs in yellow-green and dark green that glow and pulse | sand #b0a070, egg #c0d020, #206020 | frames 0,1,2,3,2,1,0 at 2 ticks | bugnesteggs |
| `textures/blocks/bugnestslimy.png` | 16×16 | transparent (46%) with vertical lime-to-yellow slime streaks | #c0f070, #d0f070, #90c070 | – | bugnestslimy |
| `textures/blocks/bio.png` | 16×16 | mottled bright green slime | #30a000, #30b000, #209000 | – | Bioblob, all faces |
| `textures/blocks/Bioblock.png` | 64×32 | UV skin for a lost slime-cube model: green translucent-looking slime, black corners at the top | #001000, #40c010, #206000 | – | unused (its model is commented out) |

## 9. Fluids

| Path | Size | Depicts | Palette / alpha | Anim | Use |
|---|---|---|---|---|---|
| `textures/blocks/acid_still.png` | 16×512 | vanilla-water-style still liquid, recoloured murky olive-mustard with faint darker orange ripples | #908000, #907000; alpha 168–226 (average 178) | 32 frames × 2 ticks | acid block top/bottom; fluid still icon |
| `textures/blocks/acid_flow.png` | 32×1024 | flowing variant with vertical streaks | #908000, #907000; alpha 165–211 | 32 frames × 1 tick (`{"animation":{}}`) | acid block sides; fluid flowing icon |
| `textures/blocks/milk_still.png` | 16×512 | the same structure in light grey-white | #c0c0c0, #d0d0d0; same alpha | 32 × 2 ticks | Milk fluid still icon, only if Techguns registered `milk` (01-core event handler) |
| `textures/blocks/milk_flow.png` | 32×1024 | flowing milk | #c0c0c0, #d0d0d0 | 32 × 1 tick | Milk flowing icon (same condition) |

## 10. Machine block textures (owned by 08 and 09)

### Ore drill (`oreDrill`, 08)

| Path | Size | Depicts / layout | Palette | Anim | Use |
|---|---|---|---|---|---|
| `textures/blocks/drillFrame_outer.png` | 32×32 | heavy frame: rusty pinkish-brown riveted steel, a raised border with corner rivets around a recessed square | #705050, #806060, #504040 | – | oreDrill:0 |
| `textures/blocks/drillFrame_outer_c.png` | 32×32 | the same, with extra rivets mid-side and a centre bolt | #705050, #806060, #302020 | – | oreDrill:8 base (CTM) |
| `textures/blocks/drillFrame_outer_c-ctm.png` | 64×64 | CTM sheet. A (interior) is an open lattice: transparent with diagonal X struts. B–D are riveted edge plates and corner pieces. | #705050, #806060, 17% transparent | – | oreDrill:8 |
| `textures/blocks/drillFrame.png` | 32×32 | light frame: transparent square with a thin dark border and a big diagonal X brace of pinkish-grey steel | #303030, #605050, 51% transparent | – | oreDrill:1 |
| `textures/blocks/drillFrame_c.png` | 32×32 | byte-identical to `drillFrame.png` | | – | oreDrill:9 base |
| `textures/blocks/drillFrame_c-ctm.png` | 64×64 | CTM sheet of the X lattice running continuously across tiles | #303030, #504040, 66% transparent | – | oreDrill:9 |
| `textures/blocks/drillFrameCorner.png` | 32×32 | green-painted frame with an X brace and transparent gaps | #306040, #205030 | – | unused (commented) |
| `textures/blocks/drillEngine.png` | 32×32 | dark machinery: riveted border, round hatch, pipework, green status LED at lower right | #202020, #101010, #404040, LED #00e000 | – | oreDrill:3 |
| `textures/blocks/drillEngineController.png` | 32×32 | dark panel with a central brushed-steel plate (4 screws) and four coloured LEDs: red top-left, green top-right, yellow bottom-left, blue bottom-right | #101010, #202020, #404040 | – | oreDrill:4 |
| `textures/blocks/drillEngine_c.png` | 32×32 | formed engine face: dark machinery with a ring hatch and green LED | #101010, #202020, #404040 | – | oreDrill:11 base |
| `textures/blocks/drillEngine_c-ctm.png` | 64×64 | CTM sheet. A is a smooth brushed-steel cover plate with corner screws. Other tiles are machinery compartments with gauges and green LEDs. | #101010, #202020, #505050 | – | oreDrill:11 |
| `textures/blocks/drillEngineTop_c.png` | 32×32 | byte-identical to `drillEngine_c.png` | | – | oreDrill:12 base |
| `textures/blocks/drillEngineTop_c-ctm.png` | 64×64 | byte-identical to `drillEngine_c-ctm.png` | | – | oreDrill:12 |
| `textures/blocks/drillRod-ctmv.png` | 64×64 | CTMV column sheet: a silver ribbed rod inside a rounded-rectangle cage window, rounded caps where the column ends | #505050, #a0a0a0, #101010 | – | oreDrill:2 sides |
| `textures/blocks/drillRod-top.png` | 32×32 | rod end: grey square with a round four-spoke hub | #303030, #505050, #606060 | – | oreDrill:2 top/bottom |
| `textures/blocks/drillRodSide.png` | 32×32 | noisy grey vertically brushed metal with horizontal scratches | #303030, #808080 | – | unused |
| `textures/blocks/drillRodTop.png` | 32×32 | dark blurred square end | #000000, #101010 | – | unused |
| `textures/blocks/drillhead.png` | 32×32 | cylinder shading: light edges, dark vertical centre band, steel grey | #202020, #303030, #707070 | – | `RenderOreDrill` drill head, tier 1 (08) |
| `textures/blocks/drillheadObsidian.png` | 32×32 | the same in blue-violet obsidian | #202030, #303040, #505070 | – | drill head, obsidian tier |
| `textures/blocks/drillheadCarbon.png` | 32×32 | the same, near-black | #101010, #202020 | – | drill head, carbon tier |

### Reaction chamber and fabricator multiblock (`machineMultiBlock`, 08)

| Path | Size | Depicts / layout | Palette | Anim | Use |
|---|---|---|---|---|---|
| `textures/blocks/rc_block.png` | 32×32 | white/light-grey panel with concentric square frame lines, square corner caps and a dark perforated grille square in the centre | #e0e0e0, #f0f0f0, #a0a0a0 | – | machineMultiBlock:0 |
| `textures/blocks/rc_controller.png` | 32×32 | the same frame with a blue horizontally striped screen in the centre | #f0f0f0, #a0a0a0, blue #3080e0 | – | machineMultiBlock:1 |
| `textures/blocks/rc_glass.png` | 32×32 | the same frame border; the centre is transparent with two diagonal white glint lines | #f0f0f0, #a0a0a0, 47% transparent | – | machineMultiBlock:2 |
| `textures/blocks/fabricatorHousing.png` | 32×32 | white rounded panel inside a soft black border | #e0e0e0, #101010 | – | machineMultiBlock:3 |
| `textures/blocks/fabricatorController.png` | 32×32 | housing with an inner bevel and a teal terminal screen showing white glyph rows and a prompt | #101010, #c0c0c0, teal #4090a0 | – | machineMultiBlock:4 |
| `textures/blocks/fabricatorGlass.png` | 32×32 | black 2-px border, fully transparent centre | #101010, 76% transparent | – | machineMultiBlock:5 |

### Legacy reaction-chamber placeholders

All unused: flat programmer art.

| Path | Size | Depicts |
|---|---|---|
| `textures/blocks/reactionChamber.png` | 32×32 | 2-px white border, black fill |
| `textures/blocks/reactionChamber2.png` | 32×32 | 2-px red border, black fill |
| `textures/blocks/reactionChamberController.png` | 32×32 | white border, black fill, grey rectangle in the centre |
| `textures/blocks/reactionChamberController2.png` | 32×32 | red border, black fill, grey-framed green display showing "10+0" |
| `textures/blocks/reactionChamberGlass.png` | 32×32 | white border, transparent centre |

### Camo bench and repair bench (09)

| Path | Size | Depicts / layout | Use |
|---|---|---|---|
| `textures/blocks/paintmachine_textures.png` | 64×64 | Atlas in quadrants. **TL (0–32, 0–32):** dark grey panel with a bevelled frame (sides, back, bottom). **TR:** front, three tall paint canisters (red, green, blue liquid windows) in a recessed bay. **BL:** top, three square lamps (red, green, blue) above a white slatted grille, flanked by two cylindrical rollers. **BR:** transparent, unused. | `PaintMachineTesselatorCube` (Camo Bench TESR and item) |
| `textures/blocks/repairbench_textures.png` | 64×64 | **TL:** grey panel. **TR:** front, a shelf of jars and cans (yellow, blue, green) above a shelf holding a circular saw blade and a red tool. **BL:** top, a brown pegboard worktop with tools (hammer, screwdriver, saw, drill, wrench, nuts). **BR:** transparent. | `RepairBenchTesselatorCube` (Repair Bench TESR and item; also the Flag item) |
| `textures/blocks/paintmachine_top.png` | 32×32 | standalone copy of the atlas BL quadrant | unused |
| `textures/blocks/paintmachine_front.png` | 32×32 | standalone copy of the atlas TR quadrant | unused |
| `textures/blocks/paintmachine_side.png` | 32×32 | standalone grey panel | unused |
| `textures/blocks/paintmachine_bottom.png` | 32×32 | byte-identical to `paintmachine_side.png` | unused |

## 11. Model textures

All are 2× the model's UV size. The UV origin of each part is in [block-rendering-and-models.md](block-rendering-and-models.md).

| Path | Size (UV) | Depicts / layout | Palette | Use |
|---|---|---|---|---|
| `textures/blocks/AmmoPress.png` | 256×128 (128×64) | Black (unused) background with box unwraps. **Worn light grey/beige riveted metal:** floor plate (UV 0–64, 0–18; the top face has a chamfered inset border), rails, housings (66–86, 0–16) and top head. **Dark olive-green paint:** corner posts and brackets (50–64, 0–13) and die plate (100–128, 24–33). **Darker steel:** motor, discs and jaws. **Brass/gold:** the blank and bullets (110–122, 46–56). | #000000, #807070, #908080, green #406020, brass #d0b060 | `ModelAmmoPress` (TESR and item) |
| `textures/blocks/MetalPress.png` | 256×128 (128×64) | Same frame art as the ammo press, all grey (no green). Riveted panels for the top beam and head. A ribbed band for the press guides. A diagonal-striped silver swatch for the metal sheet (104–128, 46–53). | #000000, #908080, #807070, #a09090 | `ModelMetalPress` |
| `textures/blocks/ChemLab.png` | 256×128 (128×64) | Black background, light grey riveted metal for bench parts, and **semi-transparent white glass** regions (G parts, upper-left area). Liquid swatches: blue (front-left tube, 114–126, 22–26), cyan (front-right tube, 114–126, 27–31), orange (test-tube stands, 81–89, 32–41), magenta (flask liquid, 103–115, 43–51), and red / green / blue (the three test tubes, 123–127, 41–49). Pale grey tubing at 105–128, 0–11. | #000000, #d0d0d0, #c0c0c0, plus the liquid colours | `ModelChemLab` |
| `textures/blocks/ChargingStation.png` | 128×128 (64×64) | Transparent background. Pale grey-green panels. **Side plates** (38–64, 19–43): white with a grey square and a black zig-zag cable. **Top/bottom plates** (0–64, 46–64): concentric grey squares with a black centre pad. **Red electrode tips** (0–8, 27–31). A grey translucent-looking square at (40–52, 0–12) for the disabled glass. | #606060, #000000, #b0b0b0, red #a02020 | `ModelChargingStation` |
| `textures/blocks/Fabricator.png` | 256×256 (128×128) | Transparent background. **Dark charcoal:** base and inner plates (top area). **White/light grey:** walls and gantry bars. **Teal terminal screen:** controller (103–125, 0–11). **Grey:** work head and tubes. **Red:** laser emitter (≈14–18, 102–110). **Thin dark outline frames:** the glass hood (8–128, 86–128); the glass itself is clear. | #303030, #202020, #909090, teal, red | `ModelFabricator` |
| `textures/blocks/TurretBase.png` | 128×128 (64×64) | Transparent background. Worn medium-grey steel/concrete: the core cube (8–64, 36–64) has recessed square panels on each face. Dark corner posts (0–8, 30–47), skirts, sockets, braces. The top-right area also holds the turret-stand parts used by `ModelTurret` (base 32–64, 22–31; mount 34–50, 12–17; struts 9–17, 30–43) and a dark radial spot. | #909090, #a0a0a0, #808080, #202020 | `ModelTurretBase` (turret machine block and item, 09) |
| `textures/blocks/TurretBase_2.png` | 128×128 | same layout, clean light grey with soft edges | #404040, #a0a0a0, #b0b0b0 | `NPCTurret` texture variant (10-entities-npcs) |
| `textures/blocks/TurretBase_3.png` | 128×128 | same layout, white-grey speckled granite look | #202020, #808080, #909090 | `NPCTurret` variant |
| `textures/blocks/TurretBase_4.png` | 128×128 | same layout, purple-grey speckled obsidian look | #101010, #403040, #504050 | `NPCTurret` variant |
| `textures/blocks/TurretBase_5.png` | 128×128 | same layout, near-black carbon look | #000000, #101010, #202020 | `NPCTurret` variant |
| `textures/blocks/ExpCharge.png` | 64×64 (32×32) | Charge pack (0–30, 0–15): dark green-grey crate panels with steel-blue cross straps. Detonator (0–20, 17–23): grey box with a dark-blue display and green "RDY" text. | #203020, #202020, #304040, blue #000080, green | `ModelExpCharge`, idle |
| `textures/blocks/ExpCharge_2.png` | 64×64 | identical except the detonator text is red "ARM" | | armed |
| `textures/blocks/tntCharge.png` | 64×64 (32×32) | Dynamite sticks (0–16, 0–16): dark red with vertical highlight stripes. Detonator box with green "RDY". | #801010, #500000, #404040 | `ModelTNTCharge`, idle |
| `textures/blocks/tntCharge_2.png` | 64×64 | identical except red "ARM" | | armed |
| `textures/blocks/flagpost.png` | 128×128 (64×64) | **Base slab top** (0–40, 0–12): grey concrete with a black square socket. **Plinth** (0–20, 14–33): weathered grey. **Poles** (22–42, 14–40): white/light grey. **Cap** (33–45, 3–8). **Flag strip** (0–36, 50–64): white field, red bands top and bottom, blue discs with white stars. | #c0c0c0, #b0b0b0, #d0d0d0, red, blue | `ModelFlagPost` (Flag machine, 09) |
| `textures/blocks/reinforced_chest.png` | 128×128 (64×64) | Vanilla single-chest skin layout. Oak-plank panels framed by dark iron bands, dark interior faces, small grey metal latch (0–6, 0–5). | #303030, #807040, #505050, #706040 | Reinforced Chest TESR and item |
| `textures/blocks/reinforced_chest_double.png` | 256×128 (128×64) | vanilla double-chest skin layout, same style | #303030, #807040, #706040 | double Reinforced Chest |
| `textures/blocks/weapon_chest.png` | 128×128 (64×64) | single-chest layout, dark olive-drab military crate with darker band seams, black interior | #202010, #101010, #102010 | Weapon Chest TESR and item |
| `textures/blocks/weapon_chest_double.png` | 256×128 (128×64) | double-chest layout, same style | #202010, #101010 | double Weapon Chest |

## Count check

| Section | Files |
|---|---|
| 1. Containers | 12 |
| 2. Concrete | 11 |
| 3. Metal panels | 22 |
| 4. Nets, sandbags, misc | 9 |
| 5. Lamps, ladders, door | 15 |
| 6. Screen and panel | 8 |
| 7. Ores | 5 + 4 overlays + 11 legacy = 20 |
| 8. Bug and bio | 6 |
| 9. Fluids | 4 |
| 10. Machine blocks | 20 drill + 6 multiblock + 5 legacy RC + 6 benches = 37 |
| 11. Model textures | 19 |
| **Total** | **163** |
