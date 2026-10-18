# 07-blocks: texture catalogue (`textures/blocks/`, all 163 files)

Every PNG in `assets/techguns/textures/blocks/`, including the ones used by machine blocks (owned by [08-machines-production](../08-machines-production/index.md) and [09-machines-utility](../09-machines-utility/index.md), which refer here).

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

| Path | Size | Depicts / layout | Palette | Anim | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/blocks/container_red.png` | 16×16 | Shipping-container side: vertical corrugation ribs (one bright rib left of centre) inside a 2-px frame. Lighter outer edge, dark inner shadow. | #602020, #b04040, #903030 | – | container:0 base | avg #853232 · #703030 30%, #903030 25%, #B05050 12% · 100% opaque |
| `textures/blocks/container_red-ctm.png` | 32×32 | CTM sheet of the same corrugation. A: ribs only; B: frame on left/right; C: frame top/bottom; D: corner notches. | #903030, #803030, #802020 | – | container:0 | avg #8B3636 · #903030 55%, #703030 18%, #D07070 8% · 100% opaque |
| `textures/blocks/container_green.png` | 16×16 | as red, dark green | #103010, #204020, #407040 | – | container:1 | avg #294D2B · #103010 33%, #305030 31%, #507050 12% · 100% opaque |
| `textures/blocks/container_green-ctm.png` | 32×32 | as red CTM, green | #305030, #204020, #609060 highlights | – | container:1 | avg #2F5330 · #305030 56%, #103010 21%, #709070 8% · 100% opaque |
| `textures/blocks/container_blue.png` | 16×16 | as red, navy blue | #202060, #303080, #4040b0 | – | container:2 | avg #323685 · #303070 30%, #303090 24%, #5050B0 12% · 100% opaque |
| `textures/blocks/container_blue-ctm.png` | 32×32 | as red CTM, blue | #303090, #303080, #202060 | – | container:2 | avg #363A8B · #303090 51%, #303070 18%, #7070D0 7% · 100% opaque |
| `textures/blocks/container_orange.png` | 16×16 | as red, orange/ochre | #704010, #905010, #d08020 | – | container:3 | avg #975D20 · #705010 22%, #905010 20%, #D09030 14% · 100% opaque |
| `textures/blocks/container_orange-ctm.png` | 32×32 | as red CTM, orange | #905010, #a06010, #704010 | – | container:3 | avg #9E6323 · #905010 26%, #B07030 17%, #B07010 15% · 100% opaque |
| `textures/blocks/container_scifi1.png` | 32×32 | Futuristic container: light-grey steel plate. A chamfered octagonal recessed hatch in the centre with a vertical seam, ringed by yellow/black hazard-stripe segments. Four round bolt sockets in the corners. | #909080, #706060, #404040, yellow #e0d020 | – | container:4 base | avg #777262 · #707070 16%, #909090 10%, #505050 10% · 100% opaque |
| `textures/blocks/container_scifi1-ctm.png` | 64×64 | CTM sheet: grey plating with a vertical seam. The borders are hazard-striped strips with a light bevel (exact A–D layout, see connected-textures). | #706060, #606060, #303030 + yellow | – | container:4 | avg #6F6A5B · #707070 40%, #707050 9%, #505050 9% · 100% opaque |
| `textures/blocks/container_scifi_rusty.png` | 32×32 | as `container_scifi1`, rusted orange-brown, with scorched hazard stripes | #907050, #806040, #403020 | – | container:5 base | avg #826947 · #907050 21%, #B09070 8%, #301010 6% · 100% opaque |
| `textures/blocks/container_scifi_rusty-ctm.png` | 64×64 | rusty CTM sheet, same layout | #806040, #906040, #907050 | – | container:5 | avg #856444 · #907050 39%, #503030 7%, #905050 7% · 100% opaque |

## 2. Concrete (`concrete`, metas 0–6) and concrete hole

| Path | Size | Depicts / layout | Palette | Anim | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/blocks/concrete01.png` | 32×32 | smooth light concrete, fine grey-beige noise | #b0b0a0, #b0a0a0, #a0a090 | – | concrete:0 "Light"; also side of TGMonsterSpawner:1 | avg #B3AFA6 · #B0B0B0 79%, #B0B090 14%, #D0B0B0 3% · 100% opaque |
| `textures/blocks/concrete02.png` | 32×32 | the same, darker grey | #808070, #807070, #908080 | – | concrete:1 "Dark" | avg #89847B · #909070 44%, #909090 28%, #907070 17% · 100% opaque |
| `textures/blocks/concrete03.png` | 32×32 | dark mottled concrete with vertical streaks | #706060, #707060, #606050 | – | concrete:5 (lang "Light Noise", but the art is dark) | avg #776F69 · #707070 53%, #907070 15%, #707050 12% · 100% opaque |
| `textures/blocks/concrete04.png` | 32×32 | light mottled pinkish-grey concrete | #b0a0a0, #a0a090, #a09090 | – | concrete:6 (lang "Dark Noise", but the art is light) | avg #ACA49D · #B0B0B0 37%, #B0B090 25%, #B09090 19% · 100% opaque |
| `textures/blocks/concrete_r.png` | 32×32 | Light concrete with an octagon-cornered recessed outline (dark line about 3 px in) and a vertical steel lattice beam down the centre (two bright rails with dark rungs, 10 px wide) | #404040, #808080, #b0b0a0 | – | concrete:2 base | avg #7A7772 · #707070 18%, #909090 18%, #505050 16% · 100% opaque |
| `textures/blocks/concrete_r-ctm.png` | 64×64 | CTM sheet: the beam continues across tiles; the recessed-groove border is split per A–D | #b0b0a0, #404040, #a0a090 | – | concrete:2 | avg #8D8A85 · #B0B0B0 27%, #909090 15%, #505050 14% · 100% opaque |
| `textures/blocks/concrete_r02.png` | 32×32 | dark-concrete version of `concrete_r` | #202020, #404040, #505050 | – | concrete:3 base | avg #605C59 · #707070 21%, #505050 19%, #303030 15% · 100% opaque |
| `textures/blocks/concrete_r02-ctm.png` | 64×64 | dark CTM sheet | #808070, #505050, #707070 | – | concrete:3 | avg #706C68 · #707070 19%, #505050 15%, #909070 14% · 100% opaque |
| `textures/blocks/concrete_old2-ctmv.png` | 64×64 | CTMV column sheet of weathered concrete. TL: single block (dark drip rim at top, rust-brown stain at bottom). TR: column top (rim, fading down). BL: column middle (plain). BR: column bottom (rust stain). | #b0b0a0, #a0a090, rust #604030 | – | concrete:4 sides | avg #9E968B · #B0B0B0 34%, #B0B090 12%, #909090 7% · 100% opaque |
| `textures/blocks/concrete_old2-top.png` | 32×32 | plain light concrete (top and bottom of the column) | #b0b0a0, #b0a0a0 | – | concrete:4 top/bottom | avg #B8B3AB · #B0B0B0 67%, #B0B090 11%, #D0D0D0 9% · 100% opaque |
| `textures/blocks/concrete_hole.png` | 32×32 | light concrete with a large ragged black hole filling about 70% of the face | #000000, #101010, #b0b0a0 | – | top of TGMonsterSpawner:1 (Zombie Hole) | avg #4D4B48 · #101010 47%, #B0B0B0 21%, #303030 9% · 100% opaque |

## 3. Metal panels (`metalpanel` 0–7, `metalpanel2` 0–6) and steel frame glass

| Path | Size | Depicts / layout | Palette | Anim | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/blocks/metalpanel_rusty.png` | 32×32 | weathered grey-brown steel plate, pitted, with 4 small bright rivets 3 px in from the corners | #404030, #505040, #707060 | – | metalpanel:0 | avg #676154 · #505030 13%, #303030 11%, #707050 10% · 100% opaque |
| `textures/blocks/metalpanel_rusty2.png` | 32×32 | the same, heavier brown rust | #403020, #302010, #504030 | – | metalpanel:1 | avg #4F4334 · #303010 16%, #503030 15%, #505030 11% · 100% opaque |
| `textures/blocks/metalpanel_hazard.png` | 32×32 | the same plate with diagonal yellow/black hazard stripes (about 6 px bands, rising to the right) | #202010, yellow #c0b030, #101010 | – | metalpanel:2 | avg #645E31 · #303030 15%, #303010 12%, #101010 6% · 100% opaque |
| `textures/blocks/metalpanel_dark.png` | 32×32 | near-black steel plate with 4 rivets | #101010, #202020, #202010 | – | metalpanel:3 | avg #2D2B27 · #303030 38%, #101010 29%, #303010 11% · 100% opaque |
| `textures/blocks/metalgrate_rusty.png` | 32×32 | Grate: a 2-px weathered frame around a black void with 6 vertical grey bars | #000000, #101000, grey bars #808070 | – | metalpanel:4 | avg #38352F · #101010 49%, #303030 12%, #707070 7% · 100% opaque |
| `textures/blocks/metalgrate_rusty2.png` | 32×32 | grate with brown-rust bars | #000000, #100000, #706050 | – | metalpanel:5 | avg #342C23 · #101010 51%, #303030 7%, #303010 5% · 100% opaque |
| `textures/blocks/metalgrate_hazard.png` | 32×32 | grate with yellow hazard tint on the frame | #000000, #101000, yellow edges | – | metalpanel:6 | avg #373425 · #101010 51%, #303030 10%, #707070 6% · 100% opaque |
| `textures/blocks/metalgrate_dark.png` | 32×32 | grate with dark frame and dull bars | #000000, #101010, #202020 | – | metalpanel:7 | avg #262522 · #101010 60%, #303030 15%, #707070 7% · 100% opaque |
| `textures/blocks/metalpanel2_rusty.png` | 32×32 | rusty plate with 4 large domed rivets at the corners | #605040, #504030, #706050 | – | metalpanel2:0 base | avg #715E48 · #707050 11%, #705030 10%, #505030 9% · 100% opaque |
| `textures/blocks/metalpanel2_rusty-ctm.png` | 64×64 | CTM sheet: four plates with seams; rivets only where the plate corners meet | #605040, #706050, #807060 | – | metalpanel2:0 | avg #776856 · #707050 19%, #705050 15%, #909070 8% · 100% opaque |
| `textures/blocks/metalpanel2_hazard.png` | 32×32 | diagonal yellow/black hazard plate, scuffed, 4 domed rivets | #403020, yellow #a09020, #000000 | – | metalpanel2:1 base | avg #6B5A1F · #101010 11%, #301010 6%, #303010 6% · 100% opaque |
| `textures/blocks/metalpanel2_hazard-ctm.png` | 64×64 | hazard CTM sheet with continuous stripes and rivets at the seams | #403020, #908020, #302020 | – | metalpanel2:1 | avg #71642D · #909030 10%, #303030 9%, #503030 7% · 100% opaque |
| `textures/blocks/metalpanel16_largeborder.png` | 16×16 | dark grey panel with a 2×2 grid of recessed square sockets separated by bevelled bars | #303030, #505050, #404040 | – | metalpanel2:2 base | avg #4F4E4D · #505050 33%, #303030 25%, #707070 20% · 100% opaque |
| `textures/blocks/metalpanel16_largeborder-ctm.png` | 32×32 | CTM sheet: A is a light silver plate with faint cross lines; borders are bevelled silver bars with small square bolts at the junctions | #a0a0a0, #404040, #b0b0b0 | – | metalpanel2:2 ("Silver Bars") | avg #727170 · #B0B0B0 23%, #505050 16%, #707070 16% · 100% opaque |
| `textures/blocks/steelframe_dark.png` | 16×16 | as `metalpanel16_largeborder`, darker | #202020, #303030, #101010 | – | metalpanel2:3 base | avg #2F2E2E · #303030 49%, #101010 26%, #505050 19% · 100% opaque |
| `textures/blocks/steelframe_dark-ctm.png` | 32×32 | CTM sheet: dark diamond-plate centre with bevelled frame bars | #404040, #303030, #202020 | – | metalpanel2:3 ("Dark Bars") | avg #424141 · #505050 33%, #303030 32%, #707070 14% · 100% opaque |
| `textures/blocks/steelframe_blue.png` | 16×16 | blue-grey steel frame with four bright white square studs | #101010, #d0d0d0, #203030 | – | metalpanel2:4 base | avg #5B6066 · #303030 25%, #101010 12%, #D0D0D0 11% · 100% opaque |
| `textures/blocks/steelframe_blue-ctm.png` | 32×32 | CTM sheet: brushed blue-steel panels with frame bars | #708090, #607080, #606070 | – | metalpanel2:4 ("Shiny") | avg #616973 · #303030 14%, #707090 12%, #709090 12% · 100% opaque |
| `textures/blocks/steelframe_scaffold.png` | 16×16 | dark square frame with an X cross-brace inside | #303030, #202020, #404040 | – | metalpanel2:5 base | avg #4A4A4A · #303030 44%, #505050 21%, #909090 12% · 100% opaque |
| `textures/blocks/steelframe_scaffold-ctm.png` | 32×32 | CTM sheet: X-lattice continuing across tiles | #202020, #404040, #505050 | – | metalpanel2:5 ("Dark") | avg #444445 · #303030 34%, #505050 32%, #707070 18% · 100% opaque |
| `textures/blocks/metalpanel_techgrid-v9.png` | 48×48 | V9 sheet (3×3 tiles of 16 px): light grey concrete-like plate with faint embossed overlapping squares that form one large pattern over 3×3 blocks | #a0a0a0, #b0b0b0, #909090 | – | metalpanel2:6 ("Silver") | avg #A2A5A5 · #B0B0B0 47%, #909090 28%, #90B0B0 6% · 100% opaque |
| `textures/blocks/steelframe_glass.png` | 32×32 | black 3-px frame around a semi-transparent smoky black pane (average alpha about 85/255) | #000000, #101010 | – | unregistered `steelframe_glass` block (unused) | avg #0B0B0B · #101010 85%, #303030 15% · 52% opaque |

## 4. Camo nets, sandbags, tent, flag, air marker, invisible

| Path | Size | Depicts / layout | Palette | Anim | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/blocks/camoNet.png` | 32×32 | Woodland camo net on a transparent background (33%): a dark brown cord grid with clusters of green leaf scraps | #304020, #709050, #405030, #80a060 | – | camoNetPane:0, camoNetRoof:0 | avg #465834 · #709050 14%, #101010 12%, #305030 10% · 66% opaque |
| `textures/blocks/camoNet_snow.png` | 32×32 | the same net with white/grey scraps | #d0d0d0, #c0c0c0, #a0a0a0 | – | camoNetPane:1, camoNetRoof:1 | avg #757572 · #D0D0D0 15%, #101010 12%, #B0B0B0 11% · 66% opaque |
| `textures/blocks/camoNet_desert.png` | 32×32 | the same net with sand/khaki scraps | #d0c080, #e0d0a0, #303020 | – | camoNetPane:2, camoNetRoof:2 | avg #887F5C · #D0D090 11%, #303030 8%, #707050 8% · 66% opaque |
| `textures/blocks/sandbags-side.png` | 32×32 | three courses of rounded olive-khaki sandbags, offset like brickwork, with dark gaps | #505040, #404030, #000000 | – | sandbags horizontal faces | avg #514D3E · #505030 18%, #101010 14%, #707050 13% · 100% opaque |
| `textures/blocks/sandbags-top.png` | 32×32 | bags seen from above in a staggered brick pattern | #505040, #606050, #000000 | – | sandbags top/bottom | avg #706A59 · #707050 17%, #505050 10%, #101010 9% · 100% opaque |
| `textures/blocks/tent.png` | 32×32 | plain beige canvas | #c0b090, #b0a080 | – | unused | avg #C3B692 · #D0B090 69%, #B0B090 24%, #D0D090 2% · 100% opaque |
| `textures/blocks/flagBlock.png` | 32×32 | grey cloth background with a waving red/white/blue striped flag carrying a black star | #e0e0e0, red #e04040, blue #4040e0 | – | unused | avg #A296A4 · #B0B0B0 17%, #D0D0D0 15%, #F0F0F0 15% · 100% opaque |
| `textures/blocks/airMarker.png` | 32×32 | transparent with a 2-px dark grey frame and the outlined letters "AIR" | #303030, #404040, #707070 | – | airMarker | avg #4E4E4E · #505050 38%, #303030 35%, #707070 21% · 38% opaque |
| `textures/blocks/invisible.png` | 16×16 | fully transparent | – | – | camo-net pane top/edge strip; oreDrill:10; machineMultiBlock:15 ([08](../08-machines-production/index.md)) | avg fully transparent ·  · 0% opaque |

## 5. Lamps, lanterns, ladders, door

| Path | Size | Depicts / layout | Palette | Anim | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/blocks/lamp01.png` | 32×32 | **Projected layout.** Black field. A bright yellow, horizontally scan-lined square at px about 9–23 (the lens front) and four yellow bars about 3–4 px thick centred on each edge, at rows/cols about 2–6 and 26–30 (the lens sides). Everything else stays dark: the socket. | #101010, yellow #f0e070, #f0d050, #f0f0a0 | – | lamp01:0–6 | avg #746E45 · #101010 58%, #F0D050 9%, #F0F0B0 9% · 100% opaque |
| `textures/blocks/lamp02.png` | 32×32 | same layout, white glow on a light grey surround | #e0e0e0, #c0c0c0, #f0f0e0 | – | lamp01:8–14 | avg #DCDCDA · #F0F0F0 57%, #D0D0D0 31%, #B0B0B0 11% · 100% opaque |
| `textures/blocks/lantern01_side.png` | 32×32 | **Projected.** Black with a wide yellow scan-lined band across rows about 9–23 (the lens body), slightly brighter in the middle third | #101010, #f0f0a0, #f0e050 | – | lamp01:7 sides | avg #888250 · #101010 49%, #F0F0B0 17%, #F0F050 12% · 100% opaque |
| `textures/blocks/lantern01_top.png` | 32×32 | black with faint nested square outlines (post and ring caps) | #101010, #202020 | – | lamp01:7 top/bottom | avg #1B1B1B · #101010 83%, #303030 16% · 100% opaque |
| `textures/blocks/lantern02_side.png` | 32×32 | white version of `lantern01_side` | #e0e0e0, #e0e0d0, #c0c0c0 | – | lamp01:15 sides | avg #DADAD6 · #F0F0F0 40%, #D0D0D0 27%, #F0F0D0 17% · 100% opaque |
| `textures/blocks/lantern02_top.png` | 32×32 | light grey with a small square in the centre | #c0c0c0, #d0d0d0, #e0e0e0 | – | lamp01:15 top/bottom | avg #D4D4D4 · #D0D0D0 59%, #F0F0F0 29%, #B0B0B0 11% · 100% opaque |
| `textures/blocks/metalLadder.png` | 32×32 | **Projected.** Blue-grey steel: rails in the 4-px left and right columns and four rung bands across. The three gaps between rungs (rows about 6–10, 14–18, 22–26) are transparent (28%). | #607070, #506060, #708090 | – | ladder01 variant 0 | avg #627076 · #507070 23%, #709090 18%, #707070 17% · 71% opaque |
| `textures/blocks/metalLadderRusty.png` | 32×32 | same layout, dark brown-orange rust | #201000, #402010, #503010 | – | variant 1 | avg #40240F · #301010 36%, #503010 30%, #303010 11% · 71% opaque |
| `textures/blocks/metalLadderShiny.png` | 32×32 | same layout, bright silver-white | #d0d0d0, #e0e0e0, #f0f0f0 | – | variant 2 | avg #DDD9DB · #F0F0F0 43%, #D0D0D0 30%, #B0B0B0 10% · 71% opaque |
| `textures/blocks/metalLadderCarbon.png` | 32×32 | same layout, near-black | #101010, #202020 | – | variant 3 | avg #191919 · #101010 80%, #303030 19% · 71% opaque |
| `textures/blocks/ladder01.png` | 16×16 | old 16-px grey ladder with transparent rung gaps | #707070, #606060 | – | unused | avg #757575 · #707070 76%, #909090 18%, #505050 5% · 71% opaque |
| `textures/blocks/ladder01_inv.png` | 32×32 | UV skin for [`ModelLadder01`](block-rendering-and-models.md#modelladder01-uv-3232): 2×16×2 rails at (0,0) and 12×2×2 rungs at (0,19), grey, rest transparent | #808080, #909090 | – | passed to the ladder and lamp renderers but never bound (unused) | avg #8D8D8D · #909090 94%, #707070 5% · 23% opaque |
| `textures/blocks/door01.png` | 32×32 | plain grey-green weathered steel | #708070, #809080, #707070 | – | door top/bottom faces | avg #888E82 · #909090 37%, #707070 19%, #709070 14% · 100% opaque |
| `textures/blocks/door01_upper.png` | 32×32 | Upper door leaf: steel slab with a round-topped recessed panel framed by a ring of small red-brown rivets, a vertical hinge strip at the left with a horizontal strap, and a small square plate low in the panel | #606060, #809080, rivets #602020 | – | upper half (mirrored copy auto-generated) | avg #747970 · #909090 24%, #707070 22%, #505050 10% · 100% opaque |
| `textures/blocks/door01_lower.png` | 32×32 | Lower leaf: a round-bottomed recessed panel continuing from the upper one, rivets, hinge strip, grime at the bottom | #404030, #505040, #809080 | – | lower half | avg #666A5D · #909090 16%, #707070 16%, #505050 15% · 100% opaque |

## 6. Radar Screen and Control Panel

| Path | Size | Depicts / layout | Palette | Anim | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/blocks/screen_front.png` | 32×32 | brown-grey metal casing with a large black rounded-rectangle screen (px about 2–30) and a seam line at mid-height on the frame edges | #101010, #000000, #706060 | – | screen front | avg #2F2C2B · #101010 58%, #303030 17%, #707070 12% · 100% opaque |
| `textures/blocks/screen_overlay_front.png` | 32×256 | 8 frames, each 32×32. Transparent outside a rounded CRT matching the screen. Green radar: concentric rings, a centre dot, horizontal scanlines, and a bright sweep blip that moves around the rings from frame to frame. | #001000–#004000, #00f000 | 8 × 2 ticks (16-tick loop) | screen overlay, front, pass 1 | avg #004300 · #101010 28%, #103010 25%, #105010 20% · 74% opaque · 8 frames |
| `textures/blocks/screen_side.png` | 32×32 | casing with a round speaker grille at upper left, a button/vent block at lower left and a horizontal seam | #403030, #504030, #706060 | – | screen sides/back | avg #4D403B · #503030 29%, #505030 14%, #707070 11% · 100% opaque |
| `textures/blocks/screen_top.png` | 32×32 | plain brown-grey casing with a slight bevel | #403030, #504030 | – | screen top/bottom; also the [`basicMachine`](block-catalogue.md#registered-blocks) break-particle icon ([08](../08-machines-production/index.md)/[09](../09-machines-utility/index.md)) | avg #534540 · #503030 49%, #505030 20%, #707070 12% · 100% opaque |
| `textures/blocks/controlpanel_front.png` | 32×32 | Console face: a black display window at top-left (px about 4–20 × 4–12) and a 2×3 grid of black keys at top-right. Below: a keypad of white/grey/red buttons, a rotary knob, a slider, toggle switches and a red push button at bottom-right. | #000000, #403030, #706060, red #c02020 | – | controlpanel front | avg #4A3D3A · #101010 17%, #503030 17%, #303030 16% · 100% opaque |
| `textures/blocks/controlpanel_overlay_front.png` | 32×256 | 8 frames. Transparent except: a green oscilloscope trace inside the display window (px 4–20 × 4–12) that changes each frame; blinking red/green/yellow LEDs at about (23–28, 3–9); a steady green LED pair at about (22–25, 27–29) | #002000, #00f000, red, yellow | 8 × 2 ticks | controlpanel overlay | avg #095200 · #103010 31%, #101010 29%, #10F010 15% · 13% opaque · 8 frames |
| `textures/blocks/controlpanel_side.png` | 32×32 | like `screen_side` but mirrored (grille upper right, vents lower left), no seam | #403030, #504030 | – | controlpanel sides/back | avg #4F413D · #503030 32%, #505030 17%, #707070 12% · 100% opaque |
| `textures/blocks/controlpanel_top.png` | 32×32 | plain casing | #403030, #504030 | – | controlpanel top/bottom | avg #534540 · #503030 49%, #505030 20%, #707070 12% · 100% opaque |

## 7. Ores

| Path | Size | Depicts / layout | Palette | Anim | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/blocks/oreTitanIron.png` | 16×64 | 4 frames: vanilla-style grey stone with pale whitish-cyan metallic flecks that shimmer between frames | #707070, #808080, #b0c0c0, #c0d0d0 | 4 × 8 ticks | oreTitanIron:0 | avg #878989 · #707070 65%, #909090 19%, #D0D0D0 6% · 100% opaque · 4 frames |
| `textures/blocks/oreUranium.png` | 16×64 | 4 frames: stone with bright green flecks that pulse | #707070, #708070, green #40c040 | 4 × 8 ticks | oreTitanIron:1 | avg #7F927C · #707070 38%, #909090 19%, #709070 14% · 100% opaque · 4 frames |
| `textures/blocks/oreCopperBlock.png` | 16×16 | stone with orange-copper blotches | #707070, #808080, #c07030 | – | `oreCopper` block ([01-core](../01-core/index.md)) | avg #897D75 · #707070 48%, #909090 26%, #907070 8% · 100% opaque |
| `textures/blocks/oreTinBlock.png` | 16×16 | stone with pale blue-white flecks | #707070, #808080, #a0c0c0 | – | `oreTin` block ([01-core](../01-core/index.md)) | avg #838789 · #707070 49%, #909090 30%, #B0D0D0 5% · 100% opaque |
| `textures/blocks/oreLeadBlock.png` | 16×16 | stone with dull blue-violet flecks | #707070, #808080, #7080b0 | – | `oreLead` block ([01-core](../01-core/index.md)) | avg #7E8087 · #707070 51%, #909090 27%, #7090B0 5% · 100% opaque |

### Ore-cluster overlays

Greyscale on a transparent background. They are tinted at render time by the type's RGB and drawn over stone, netherrack, end stone or bedrock.

| Path | Size | Depicts | Transparent | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|
| `textures/blocks/oreClusterNew1.png` | 32×32 | three large elongated shiny ore chunks plus several small specks | 62% | overlay type 0 (iron, gold, redstone, copper, tin, lead, silver, nickel, uranium, quartz) | avg #696969 · #303030 20%, #101010 15%, #707070 15% · 37% opaque |
| `textures/blocks/oreClusterNew2.png` | 32×32 | about nine small rounded nuggets scattered evenly | 66% | type 1 (coal, lapis, osmium, aluminium, platinum, titanium iron) | avg #545454 · #303030 29%, #505050 20%, #707070 15% · 33% opaque |
| `textures/blocks/oreClusterNew3.png` | 32×32 | one large faceted crystal in the centre plus four small cubes toward the corners | 76% | type 2 (diamond, emerald, certus, glowstone) | avg #838383 · #303030 17%, #707070 16%, #B0B0B0 15% · 23% opaque |
| `textures/blocks/oreClusterNew4.png` | 32×32 | a large blotchy square puddle filling most of the face, with a ragged dark rim | 42% | type 3 (oil hole) | avg #929292 · #909090 30%, #B0B0B0 30%, #D0D0D0 12% · 57% opaque |

### Legacy ore-cluster art

All unused. The `registerIcon` calls are commented out. Bases have transparent holes; overlays fill them.

| Path | Size | Depicts | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|
| `textures/blocks/oreCluster.png` | 32×32 | stone base with large oval holes | avg #727272 · #707070 59%, #909090 26%, #505050 14% · 62% opaque |
| `textures/blocks/oreCluster1.png` | 32×32 | overlay: white/grey chunky ore lumps | avg #808080 · #707070 16%, #909090 15%, #303030 13% · 37% opaque |
| `textures/blocks/oreCluster_1.png` | 32×32 | stone base, many small holes | avg #797979 · #707070 52%, #909090 43%, #505050 3% · 66% opaque |
| `textures/blocks/oreCluster_11.png` | 32×32 | overlay: dark curled nuggets | avg #6F6F6F · #303030 20%, #505050 19%, #909090 18% · 33% opaque |
| `textures/blocks/oreCluster_2.png` | 32×32 | stone base, one big central hole and four small ones | avg #7A7A7A · #909090 48%, #707070 47%, #505050 4% · 76% opaque |
| `textures/blocks/oreCluster_21.png` | 32×32 | overlay: white crystal and corner cubes | avg #A9A9A9 · #F0F0F0 26%, #707070 19%, #909090 16% · 23% opaque |
| `textures/blocks/oreCluster_3.png` | 32×32 | netherrack-red base with holes | avg #612F2D · #301010 20%, #503030 17%, #501010 11% · 62% opaque |
| `textures/blocks/oreCluster_31.png` | 32×32 | overlay: black/white shards | avg #6B6B6B · #101010 30%, #F0F0F0 13%, #303030 11% · 37% opaque |
| `textures/blocks/oreCluster_4.png` | 32×32 | dark grey rim with one huge hole | avg #4D4D4D · #505050 39%, #303030 27%, #909090 15% · 42% opaque |
| `textures/blocks/oreCluster_41.png` | 32×32 | overlay: grey blob | avg #A1A1A1 · #B0B0B0 36%, #909090 27%, #D0D0D0 19% · 57% opaque |
| `textures/blocks/oreCluster_5.png` | 32×32 | netherrack-red base with a central hole and four corner holes | avg #6B3331 · #503030 16%, #301010 15%, #905050 12% · 76% opaque |

## 8. Bug nest, bio blob

| Path | Size | Depicts / layout | Palette | Anim | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/blocks/bugnestblock.png` | 16×16 | coarse sandy beige grain | #b0a070, #c0b080, #a09070 | – | bugnest:0; sides of TGMonsterSpawner:0 | avg #B8A77F · #D0B090 30%, #B0B070 24%, #B09070 23% · 100% opaque |
| `textures/blocks/bugnestblock_hole.png` | 16×16 | the same sand with a large irregular black hole | #000000, #101010, #b0a070 | – | top of TGMonsterSpawner:0 (Bug Hole) | avg #423C30 · #101010 50%, #303030 9%, #707050 5% · 100% opaque |
| `textures/blocks/bugnesteggs.png` | 16×64 | 4 frames: a sand-coloured rim around clustered glossy eggs in yellow-green and dark green that glow and pulse | sand #b0a070, egg #c0d020, #206020 | frames 0,1,2,3,2,1,0 at 2 ticks | bugnesteggs | avg #9AB262 · #B0B070 6%, #D0B090 4%, #F0F0B0 4% · 100% opaque · 4 frames |
| `textures/blocks/bugnestslimy.png` | 16×16 | transparent (46%) with vertical lime-to-yellow slime streaks | #c0f070, #d0f070, #90c070 | – | bugnestslimy | avg #B3E571 · #D0F070 12%, #D0F090 10%, #B0F050 6% · 53% opaque |
| `textures/blocks/bio.png` | 16×16 | mottled bright green slime | #30a000, #30b000, #209000 | – | Bioblob, all faces | avg #33A40C · #30B010 55%, #309010 28%, #307010 6% · 100% opaque |
| `textures/blocks/Bioblock.png` | 64×32 | UV skin for a lost slime-cube model: green translucent-looking slime, black corners at the top | #001000, #40c010, #206000 | – | unused (its model is commented out) | avg #2E7C10 · #101010 25%, #307010 15%, #30B010 11% · 100% opaque |

## 9. Fluids

| Path | Size | Depicts | Palette / alpha | Anim | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/blocks/acid_still.png` | 16×512 | vanilla-water-style still liquid, recoloured murky olive-mustard with faint darker orange ripples | #908000, #907000; alpha 168–226 (average 178) | 32 frames × 2 ticks | acid block top/bottom; fluid still icon | avg #918100 · #909010 90%, #907010 9%, #B07010 0% · 100% opaque · 32 frames |
| `textures/blocks/acid_flow.png` | 32×1024 | flowing variant with vertical streaks | #908000, #907000; alpha 165–211 | 32 frames × 1 tick (`{"animation":{}}`) | acid block sides; fluid flowing icon | avg #928100 · #909010 82%, #907010 17%, #B07010 0% · 100% opaque · 32 frames |
| `textures/blocks/milk_still.png` | 16×512 | the same structure in light grey-white | #c0c0c0, #d0d0d0; same alpha | 32 × 2 ticks | Milk fluid still icon, only if Techguns registered `milk` ([01-core](../01-core/index.md) event handler) | avg #CDCDCD · #D0D0D0 100% · 100% opaque · 32 frames |
| `textures/blocks/milk_flow.png` | 32×1024 | flowing milk | #c0c0c0, #d0d0d0 | 32 × 1 tick | Milk flowing icon (same condition) | avg #CDCDCD · #D0D0D0 100% · 100% opaque · 32 frames |

## 10. Machine block textures (owned by 08 and 09)

### Ore drill (`oreDrill`, 08)

| Path | Size | Depicts / layout | Palette | Anim | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/blocks/drillFrame_outer.png` | 32×32 | heavy frame: rusty pinkish-brown riveted steel, a raised border with corner rivets around a recessed square | #705050, #806060, #504040 | – | oreDrill:0 | avg #705854 · #705050 25%, #907070 13%, #503030 7% · 100% opaque |
| `textures/blocks/drillFrame_outer_c.png` | 32×32 | the same, with extra rivets mid-side and a centre bolt | #705050, #806060, #302020 | – | oreDrill:8 base (CTM) | avg #705853 · #705050 20%, #907070 14%, #303030 8% · 100% opaque |
| `textures/blocks/drillFrame_outer_c-ctm.png` | 64×64 | CTM sheet. A (interior) is an open lattice: transparent with diagonal X struts. B–D are riveted edge plates and corner pieces. | #705050, #806060, 17% transparent | – | oreDrill:8 | avg #715A57 · #705050 22%, #907070 15%, #503030 8% · 82% opaque |
| `textures/blocks/drillFrame.png` | 32×32 | light frame: transparent square with a thin dark border and a big diagonal X brace of pinkish-grey steel | #303030, #605050, 51% transparent | – | oreDrill:1 | avg #6A5F62 · #303030 19%, #505050 17%, #705050 9% · 48% opaque |
| `textures/blocks/drillFrame_c.png` | 32×32 | byte-identical to `drillFrame.png` | same as `drillFrame.png` | – | oreDrill:9 base | avg #6A5F62 · #303030 19%, #505050 17%, #705050 9% · 48% opaque |
| `textures/blocks/drillFrame_c-ctm.png` | 64×64 | CTM sheet of the X lattice running continuously across tiles | #303030, #504040, 66% transparent | – | oreDrill:9 | avg #6F6467 · #303030 19%, #505050 14%, #B0B0B0 11% · 33% opaque |
| `textures/blocks/drillFrameCorner.png` | 32×32 | green-painted frame with an X brace and transparent gaps | #306040, #205030 | – | unused (commented) | avg #2D5638 · #305030 28%, #307050 27%, #103010 17% · 55% opaque |
| `textures/blocks/drillEngine.png` | 32×32 | dark machinery: riveted border, round hatch, pipework, green status LED at lower right | #202020, #101010, #404040, LED #00e000 | – | oreDrill:3 | avg #4A4745 · #303030 24%, #101010 20%, #505050 20% · 100% opaque |
| `textures/blocks/drillEngineController.png` | 32×32 | dark panel with a central brushed-steel plate (4 screws) and four coloured LEDs: red top-left, green top-right, yellow bottom-left, blue bottom-right | #101010, #202020, #404040 | – | oreDrill:4 | avg #4F4C4A · #101010 20%, #303030 19%, #505050 17% · 100% opaque |
| `textures/blocks/drillEngine_c.png` | 32×32 | formed engine face: dark machinery with a ring hatch and green LED | #101010, #202020, #404040 | – | oreDrill:11 base | avg #4B4746 · #101010 22%, #303030 20%, #505050 19% · 100% opaque |
| `textures/blocks/drillEngine_c-ctm.png` | 64×64 | CTM sheet. A is a smooth brushed-steel cover plate with corner screws. Other tiles are machinery compartments with gauges and green LEDs. | #101010, #202020, #505050 | – | oreDrill:11 | avg #4F4B49 · #101010 21%, #303030 18%, #505050 18% · 100% opaque |
| `textures/blocks/drillEngineTop_c.png` | 32×32 | byte-identical to `drillEngine_c.png` | same as `drillEngine_c.png` | – | oreDrill:12 base | avg #4B4746 · #101010 22%, #303030 20%, #505050 19% · 100% opaque |
| `textures/blocks/drillEngineTop_c-ctm.png` | 64×64 | byte-identical to `drillEngine_c-ctm.png` | same as `drillEngine_c-ctm.png` | – | oreDrill:12 | avg #4F4B49 · #101010 21%, #303030 18%, #505050 18% · 100% opaque |
| `textures/blocks/drillRod-ctmv.png` | 64×64 | CTMV column sheet: a silver ribbed rod inside a rounded-rectangle cage window, rounded caps where the column ends | #505050, #a0a0a0, #101010 | – | oreDrill:2 sides | avg #767574 · #505050 16%, #B0B0B0 15%, #707070 14% · 100% opaque |
| `textures/blocks/drillRod-top.png` | 32×32 | rod end: grey square with a round four-spoke hub | #303030, #505050, #606060 | – | oreDrill:2 top/bottom | avg #5A5A5A · #303030 25%, #505050 22%, #707070 18% · 100% opaque |
| `textures/blocks/drillRodSide.png` | 32×32 | noisy grey vertically brushed metal with horizontal scratches | #303030, #808080 | – | unused | avg #6A6A6A · #505050 18%, #707070 18%, #303030 17% · 100% opaque |
| `textures/blocks/drillRodTop.png` | 32×32 | dark blurred square end | #000000, #101010 | – | unused | avg #4D4D4D · #101010 31%, #303030 16%, #707070 13% · 100% opaque |
| `textures/blocks/drillhead.png` | 32×32 | cylinder shading: light edges, dark vertical centre band, steel grey | #202020, #303030, #707070 | – | [`RenderOreDrill`](../09-machines-utility/ore-drill.md#9-tesr-renderoredrill) drill head, tier 1 ([08](../08-machines-production/index.md)) | avg #5E5E5E · #303030 26%, #505050 21%, #707070 17% · 100% opaque |
| `textures/blocks/drillheadObsidian.png` | 32×32 | the same in blue-violet obsidian | #202030, #303040, #505070 | – | drill head, obsidian tier | avg #393A4D · #505070 19%, #303030 17%, #303050 15% · 100% opaque |
| `textures/blocks/drillheadCarbon.png` | 32×32 | the same, near-black | #101010, #202020 | – | drill head, carbon tier | avg #252525 · #303030 45%, #101010 44%, #505050 10% · 100% opaque |

### Reaction chamber and fabricator multiblock (`machineMultiBlock`, 08)

| Path | Size | Depicts / layout | Palette | Anim | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/blocks/rc_block.png` | 32×32 | white/light-grey panel with concentric square frame lines, square corner caps and a dark perforated grille square in the centre | #e0e0e0, #f0f0f0, #a0a0a0 | – | machineMultiBlock:0 | avg #C5C5C5 · #F0F0F0 56%, #B0B0B0 28%, #505050 7% · 100% opaque |
| `textures/blocks/rc_controller.png` | 32×32 | the same frame with a blue horizontally striped screen in the centre | #f0f0f0, #a0a0a0, blue #3080e0 | – | machineMultiBlock:1 | avg #C8D3DC · #F0F0F0 50%, #B0B0B0 34%, #B0D0F0 2% · 100% opaque |
| `textures/blocks/rc_glass.png` | 32×32 | the same frame border; the centre is transparent with two diagonal white glint lines | #f0f0f0, #a0a0a0, 47% transparent | – | machineMultiBlock:2 | avg #D7D7D7 · #F0F0F0 55%, #B0B0B0 42%, #D0D0D0 2% · 52% opaque |
| `textures/blocks/fabricatorHousing.png` | 32×32 | white rounded panel inside a soft black border | #e0e0e0, #101010 | – | machineMultiBlock:3 | avg #949494 · #F0F0F0 36%, #101010 24%, #D0D0D0 13% · 100% opaque |
| `textures/blocks/fabricatorController.png` | 32×32 | housing with an inner bevel and a teal terminal screen showing white glyph rows and a prompt | #101010, #c0c0c0, teal #4090a0 | – | machineMultiBlock:4 | avg #787D7E · #101010 24%, #D0D0D0 21%, #B0B0B0 12% · 100% opaque |
| `textures/blocks/fabricatorGlass.png` | 32×32 | black 2-px border, fully transparent centre | #101010, 76% transparent | – | machineMultiBlock:5 | avg #1A1919 · #101010 99%, #301010 0% · 23% opaque |

### Legacy reaction-chamber placeholders

All unused: flat programmer art.

| Path | Size | Depicts | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|
| `textures/blocks/reactionChamber.png` | 32×32 | 2-px white border, black fill | avg #3B3B3B · #101010 76%, #F0F0F0 23% · 100% opaque |
| `textures/blocks/reactionChamber2.png` | 32×32 | 2-px red border, black fill | avg #3B0000 · #101010 76%, #F01010 23% · 100% opaque |
| `textures/blocks/reactionChamberController.png` | 32×32 | white border, black fill, grey rectangle in the centre | avg #505050 · #101010 58%, #F0F0F0 23%, #707070 17% · 100% opaque |
| `textures/blocks/reactionChamberController2.png` | 32×32 | red border, black fill, grey-framed green display showing "10+0" | avg #411808 · #101010 58%, #F01010 23%, #105030 8% · 100% opaque |
| `textures/blocks/reactionChamberGlass.png` | 32×32 | white border, transparent centre | avg #FFFFFF · #F0F0F0 100% · 23% opaque |

### Camo bench and repair bench (09)

| Path | Size | Depicts / layout | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|
| `textures/blocks/paintmachine_textures.png` | 64×64 | Atlas in quadrants. **TL (0–32, 0–32):** dark grey panel with a bevelled frame (sides, back, bottom). **TR:** front, three tall paint canisters (red, green, blue liquid windows) in a recessed bay. **BL:** top, three square lamps (red, green, blue) above a white slatted grille, flanked by two cylindrical rollers. **BR:** transparent, unused. | [`PaintMachineTesselatorCube`](block-rendering-and-models.md#tesselatorcube-family-fixed-uvs-for-tesrs-and-items) (Camo Bench TESR and item) | avg #5A5B5A · #505050 23%, #909090 17%, #303030 15% · 75% opaque |
| `textures/blocks/repairbench_textures.png` | 64×64 | **TL:** grey panel. **TR:** front, a shelf of jars and cans (yellow, blue, green) above a shelf holding a circular saw blade and a red tool. **BL:** top, a brown pegboard worktop with tools (hammer, screwdriver, saw, drill, wrench, nuts). **BR:** transparent. | `RepairBenchTesselatorCube` (Repair Bench TESR and item; also the Flag item) | avg #5B554E · #505050 21%, #909090 17%, #705030 11% · 75% opaque |
| `textures/blocks/paintmachine_top.png` | 32×32 | standalone copy of the atlas BL quadrant | unused | avg #555555 · #101010 24%, #909090 18%, #707070 14% · 100% opaque |
| `textures/blocks/paintmachine_front.png` | 32×32 | standalone copy of the atlas TR quadrant | unused | avg #676B68 · #909090 21%, #B0B0B0 13%, #101010 11% · 100% opaque |
| `textures/blocks/paintmachine_side.png` | 32×32 | standalone grey panel | unused | avg #515150 · #505050 48%, #303030 26%, #909090 12% · 100% opaque |
| `textures/blocks/paintmachine_bottom.png` | 32×32 | byte-identical to `paintmachine_side.png` | unused | avg #515150 · #505050 48%, #303030 26%, #909090 12% · 100% opaque |

## 11. Model textures

All are 2× the model's UV size. The UV origin of each part is in [block-rendering-and-models.md](block-rendering-and-models.md).

| Path | Size (UV) | Depicts / layout | Palette | Use | Measured (ASSET-FACTS): average · dominant · opaque |
|---|---|---|---|---|---|
| `textures/blocks/AmmoPress.png` | 256×128 (128×64) | Black (unused) background with box unwraps. **Worn light grey/beige riveted metal:** floor plate (UV 0–64, 0–18; the top face has a chamfered inset border), rails, housings (66–86, 0–16) and top head. **Dark olive-green paint:** corner posts and brackets (50–64, 0–13) and die plate (100–128, 24–33). **Darker steel:** motor, discs and jaws. **Brass/gold:** the blank and bullets (110–122, 46–56). | #000000, #807070, #908080, green #406020, brass #d0b060 | [`ModelAmmoPress`](block-rendering-and-models.md#modelammopress-uv-12864-texture-ammopresspng) (TESR and item) | avg #292724 · #101010 66%, #505050 5%, #909090 3% · 100% opaque |
| `textures/blocks/MetalPress.png` | 256×128 (128×64) | Same frame art as the ammo press, all grey (no green). Riveted panels for the top beam and head. A ribbed band for the press guides. A diagonal-striped silver swatch for the metal sheet (104–128, 46–53). | #000000, #908080, #807070, #a09090 | [`ModelMetalPress`](block-rendering-and-models.md#modelmetalpress-uv-12864-texture-metalpresspng) | avg #383532 · #101010 58%, #909090 7%, #505050 5% · 100% opaque |
| `textures/blocks/ChemLab.png` | 256×128 (128×64) | Black background, light grey riveted metal for bench parts, and **semi-transparent white glass** regions (G parts, upper-left area). Liquid swatches: blue (front-left tube, 114–126, 22–26), cyan (front-right tube, 114–126, 27–31), orange (test-tube stands, 81–89, 32–41), magenta (flask liquid, 103–115, 43–51), and red / green / blue (the three test tubes, 123–127, 41–49). Pale grey tubing at 105–128, 0–11. | #000000, #d0d0d0, #c0c0c0, plus the liquid colours | [`ModelChemLab`](block-rendering-and-models.md#modelchemlab-uv-12864-texture-chemlabpng) | avg #363435 · #101010 63%, #D0D0D0 9%, #707070 4% · 96% opaque |
| `textures/blocks/ChargingStation.png` | 128×128 (64×64) | Transparent background. Pale grey-green panels. **Side plates** (38–64, 19–43): white with a grey square and a black zig-zag cable. **Top/bottom plates** (0–64, 46–64): concentric grey squares with a black centre pad. **Red electrode tips** (0–8, 27–31). A grey translucent-looking square at (40–52, 0–12) for the disabled glass. | #606060, #000000, #b0b0b0, red #a02020 | [`ModelChargingStation`](block-rendering-and-models.md#modelchargingstation-uv-6464-texture-chargingstationpng) | avg #7A7B74 · #707070 20%, #505050 12%, #B0B0B0 11% · 43% opaque |
| `textures/blocks/Fabricator.png` | 256×256 (128×128) | Transparent background. **Dark charcoal:** base and inner plates (top area). **White/light grey:** walls and gantry bars. **Teal terminal screen:** controller (103–125, 0–11). **Grey:** work head and tubes. **Red:** laser emitter (≈14–18, 102–110). **Thin dark outline frames:** the glass hood (8–128, 86–128); the glass itself is clear. | #303030, #202020, #909090, teal, red | [`ModelFabricator`](block-rendering-and-models.md#modelfabricator-uv-128128-texture-fabricatorpng) | avg #535151 · #303030 56%, #909090 10%, #707070 8% · 36% opaque |
| `textures/blocks/TurretBase.png` | 128×128 (64×64) | Transparent background. Worn medium-grey steel/concrete: the core cube (8–64, 36–64) has recessed square panels on each face. Dark corner posts (0–8, 30–47), skirts, sockets, braces. The top-right area also holds the turret-stand parts used by [`ModelTurret`](block-rendering-and-models.md#modelturret-and-modelsimpleturret-uv-6464--6432) (base 32–64, 22–31; mount 34–50, 12–17; struts 9–17, 30–43) and a dark radial spot. | #909090, #a0a0a0, #808080, #202020 | [`ModelTurretBase`](block-rendering-and-models.md#modelturretbase-uv-6464-texture-turretbasepng) (turret machine block and item, 09) | avg #717171 · #909090 32%, #B0B0B0 18%, #303030 15% · 47% opaque |
| `textures/blocks/TurretBase_2.png` | 128×128 | same layout, clean light grey with soft edges | #404040, #a0a0a0, #b0b0b0 | [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) texture variant ([10-entities-npcs](../10-entities-npcs/index.md)) | avg #757575 · #505050 27%, #B0B0B0 26%, #909090 18% · 47% opaque |
| `textures/blocks/TurretBase_3.png` | 128×128 | same layout, white-grey speckled granite look | #202020, #808080, #909090 | `NPCTurret` variant | avg #585858 · #303030 26%, #909090 20%, #101010 17% · 47% opaque |
| `textures/blocks/TurretBase_4.png` | 128×128 | same layout, purple-grey speckled obsidian look | #101010, #403040, #504050 | `NPCTurret` variant | avg #322A34 · #303030 30%, #101010 25%, #503050 17% · 47% opaque |
| `textures/blocks/TurretBase_5.png` | 128×128 | same layout, near-black carbon look | #000000, #101010, #202020 | `NPCTurret` variant | avg #161616 · #101010 70%, #303030 28%, #505050 0% · 47% opaque |
| `textures/blocks/ExpCharge.png` | 64×64 (32×32) | Charge pack (0–30, 0–15): dark green-grey crate panels with steel-blue cross straps. Detonator (0–20, 17–23): grey box with a dark-blue display and green "RDY" text. | #203020, #202020, #304040, blue #000080, green | [`ModelExpCharge`](block-rendering-and-models.md#modelexpcharge-uv-3232-textures-expchargepng--expcharge_2png), idle | avg #283337 · #303030 52%, #505050 12%, #305050 11% · 50% opaque |
| `textures/blocks/ExpCharge_2.png` | 64×64 | identical except the detonator text is red "ARM" | as `ExpCharge.png`, red "ARM" text instead of green | armed | avg #2D3136 · #303030 52%, #505050 11%, #305050 11% · 50% opaque |
| `textures/blocks/tntCharge.png` | 64×64 (32×32) | Dynamite sticks (0–16, 0–16): dark red with vertical highlight stripes. Detonator box with green "RDY". | #801010, #500000, #404040 | [`ModelTNTCharge`](block-rendering-and-models.md#modeltntcharge-uv-3232-textures-tntchargepng--tntcharge_2png), idle | avg #5F201D · #901010 25%, #501010 24%, #505050 20% · 29% opaque |
| `textures/blocks/tntCharge_2.png` | 64×64 | identical except red "ARM" | as `tntCharge.png`, red "ARM" text instead of green | armed | avg #671D1C · #901010 25%, #501010 24%, #701010 20% · 29% opaque |
| `textures/blocks/flagpost.png` | 128×128 (64×64) | **Base slab top** (0–40, 0–12): grey concrete with a black square socket. **Plinth** (0–20, 14–33): weathered grey. **Poles** (22–42, 14–40): white/light grey. **Cap** (33–45, 3–8). **Flag strip** (0–36, 50–64): white field, red bands top and bottom, blue discs with white stars. | #c0c0c0, #b0b0b0, #d0d0d0, red, blue | [`ModelFlagPost`](block-rendering-and-models.md#modelflagpost-uv-6464-texture-flagpostpng) (Flag machine, 09) | avg #8B7D86 · #D0D0D0 16%, #B0B0B0 14%, #909090 12% · 40% opaque |
| `textures/blocks/reinforced_chest.png` | 128×128 (64×64) | Vanilla single-chest skin layout. Oak-plank panels framed by dark iron bands, dark interior faces, small grey metal latch (0–6, 0–5). | #303030, #807040, #505050, #706040 | Reinforced Chest TESR and item | avg #5B513E · #907050 22%, #303030 19%, #505050 16% · 40% opaque |
| `textures/blocks/reinforced_chest_double.png` | 256×128 (128×64) | vanilla double-chest skin layout, same style | #303030, #807040, #706040 | double Reinforced Chest | avg #5B503C · #907050 23%, #303030 16%, #505050 13% · 36% opaque |
| `textures/blocks/weapon_chest.png` | 128×128 (64×64) | single-chest layout, dark olive-drab military crate with darker band seams, black interior | #202010, #101010, #102010 | Weapon Chest TESR and item | avg #1C2117 · #303010 50%, #101010 35%, #103010 11% · 40% opaque |
| `textures/blocks/weapon_chest_double.png` | 256×128 (128×64) | double-chest layout, same style | #202010, #101010 | double Weapon Chest | avg #1B2016 · #303010 52%, #101010 36%, #103010 9% · 36% opaque |

## 12. Region maps and full descriptions (consistency pass)

Everything above is kept. This section adds exact pixel rectangles (texture pixels, origin top-left, inclusive corners) measured from the files, and the full description of the unused files. "Island" means a connected group of non-transparent pixels (alpha above 16), measured by flood fill, so it is a measured bounding box rather than a UV guess. Rectangles read from an enlarged view by eye are marked *approx*.

### 12.1 `paintmachine_textures.png` (64×64, used) and the four unused `paintmachine_*` files

**Atlas layout** (4 quadrants of 32×32; opaque share 75%, measured average #5A5B5A):

| Rect (x0,y0)-(x1,y1) | Role | Mapped by |
|---|---|---|
| (0,0)-(31,31) | side, back, left, right and bottom faces of the cube | `PaintMachineTesselatorCube` (u 0–0.5, v 0–0.5) |
| (32,0)-(63,31) | front face | u 0.5–1, v 0–0.5 |
| (0,32)-(31,63) | top face | u 0–0.5, v 0.5–1 |
| (32,32)-(63,63) | unused; fully transparent (no pixel above alpha 16) | none |

**The four standalone files are the unused atlas quadrants.** Each is 32×32 and 100% opaque. A pixel comparison against the atlas shows they are exact copies:

| File | Equals atlas quadrant | Measured average · dominant | What it shows |
|---|---|---|---|
| `paintmachine_side.png` | TL (0,0)-(31,31) | #515150 · #505050 48%, #303030 26%, #909090 12% | A flat, slightly mottled dark-grey steel panel with a lighter bevelled 1-px outer rim and a faint darker inner frame line about 2 px in. No details. Greyscale only (no hue). |
| `paintmachine_bottom.png` | TL (byte-identical to `paintmachine_side.png`) | same as side | As the side panel. |
| `paintmachine_front.png` | TR (32,0)-(63,31) | #676B68 · #909090 21%, #B0B0B0 13%, #101010 11% | A dark recessed bay holding three tall paint canisters side by side. Each has a ribbed grey cap and neck at the top (rows 2–13), a coloured liquid window in the middle and a grey base ring at the bottom (rows 25–30). Liquid windows (measured by colour): red x 3–9, green x 11–19, blue x 21–28, all rows 14–24. Colours are saturated primaries with a lighter vertical highlight strip down the left third of each window. |
| `paintmachine_top.png` | BL (0,32)-(31,63) | #555555 · #101010 24%, #909090 18%, #707070 14% | The top face. Three small square lamps in a row near the top edge: red (9–12, 4–7), green (14–17, 4–7), blue (19–22, 4–7) (measured). Below them a white slatted grille, a square about x 9–23, y 9–19 with five horizontal white bars (*approx*). Either side of the grille, a vertical cylindrical roller (a white-to-grey shaded tube) about x 4–8 and x 24–28, y 13–24 (*approx*), set in black recesses; a small grey square sits at the bottom centre of the lower black bar. The top-left corner has a darker square pad (*approx* x 2–8, y 2–8). |

No live code loads any of the four standalone files. The Camo Bench uses only the atlas ([PaintMachineTesselatorCube](block-rendering-and-models.md#tesselatorcube-family-fixed-uvs-for-tesrs-and-items)), so they look like leftovers from before the atlas was made.

### 12.2 `flagBlock.png` (32×32, unused)

A **fully opaque** 32×32 square (average #A296A4; dominant #B0B0B0 17%, #D0D0D0 15%, #F0F0F0 15%). It has no `.mcmeta`, no animation and is referenced by no code (the Flag machine uses `flagpost.png`, not this file).

| Region (rect) | Content |
|---|---|
| (0,0)-(31,31) | Background: mottled mid-to-light grey with soft darker corners, like out-of-focus cloud or smoke; no pattern. |
| about (4,5)-(27,27) | A waving flag, drawn as if blowing right. Its top and bottom edges are wavy bands in two colours: **red** (measured x 4–27, y 5–24, 72 px of strong red) outside, and **blue** (measured x 4–27, y 7–27, 79 px of strong blue) inside, each band about 2–3 px thick and stepping up and down along the width. |
| about (5,9)-(26,25) | The cloth field between the bands: near-white (220 pixels in the whole file are #D8D8D8 or brighter). |
| about (7,9)-(23,24) *approx* | A large five-pointed star in black fading to dark grey at its right-hand tips (the part darker than #282828 is x 16–22, y 12–22, 49 px, measured). |

Palette in words: near-white cloth, saturated red and blue edge bands, a black star, all on neutral grey. It reads as a patriotic flag with a single star, in a different style from the flag strip in `flagpost.png` (which has blue discs with white stars). It is probably an abandoned flat-flag block texture from before the 3D `ModelFlagPost` was made. It is not loaded anywhere, so it is *unused* and *unobtainable* in game.

### 12.3 Multi-region model textures: measured islands

Opaque islands for the model textures in section 11. Rectangles are texture pixels (twice the UV rectangles quoted above for the 2× textures).

| Texture (size) | Islands (x0,y0)-(x1,y1), pixel count in brackets | Reading |
|---|---|---|
| `reinforced_chest.png` and `weapon_chest.png` (128×128) | (0,0)-(111,85) [6,496 px]; the latch island (0,0)-(11,9) [112 px] is counted inside the first | One opaque block 112×86, which is twice the vanilla single-chest unwrap (56×43): lid unwrap above, body unwrap below. Latch 12×10 at the origin. Everything right of x 111 or below y 85 is transparent (60% of the file, matching the measured 40% opaque). |
| `reinforced_chest_double.png` and `weapon_chest_double.png` (256×128) | (0,0)-(175,85) [12,000 px]; latch (0,0)-(11,9) | The same unwrap, 176×86 (twice vanilla's 88×43 double). |
| [`TurretBase.png`](block-catalogue.md#blocks-constructed-but-not-registered-not-in-game) and `TurretBase_2` to `TurretBase_5` (128×128, all five identical in layout) | (0,0)-(27,23); (60,0)-(123,19); (68,24)-(99,33); (0,30)-(51,57); (72,36)-(103,41); (64,44)-(127,61); (0,60)-(15,93); (18,60)-(33,85); (16,72)-(127,127) | Nine islands. The big one (16,72)-(127,127) is the core cube's faces (112×56). The others are the corner posts, skirts, sockets and the stand parts for `ModelTurret`. The opaque share is 47% in all five files, so a variant is a pure recolour. Variants: 1 clean grey, 2 light grey, 3 white speckled granite, 4 purple-grey obsidian, 5 near-black carbon (see section 11). |
| `ExpCharge.png` and `ExpCharge_2.png` (64×64) | (0,0)-(59,29) [1,600 px]; (0,34)-(39,45) [448 px] | Charge pack unwrap 60×30 (twice UV 30×15) and detonator unwrap 40×12. The `_2` file differs only in the detonator text colour. |
| `tntCharge.png` and `tntCharge_2.png` (64×64) | (0,0)-(31,31) [896 px]; (4,34)-(39,45) [288 px]; (0,38)-(3,45) [32 px] | Dynamite bundle unwrap 32×32, detonator box unwrap, plus a small 4×8 strip at the left. |
| `flagpost.png` (128×128) | (0,0)-(79,23) [1,120]; (66,6)-(89,15) [168]; (0,28)-(39,65) [1,320]; (44,28)-(83,79) [1,944]; (0,100)-(71,127) [2,016] | Base slab (0,0)-(79,23); cap (66,6)-(89,15); plinth (0,28)-(39,65); poles (44,28)-(83,79); flag strip (0,100)-(71,127). |
| `ChargingStation.png` (128×128) | (80,0)-(103,23); (76,38)-(127,87); (0,48)-(63,65); (0,68)-(79,89); (0,92)-(127,127); (0,94)-(15,121) | Six islands: the disabled-glass square (80,0)-(103,23), side plates (76,38)-(127,87), top and bottom plate groups, the base group (0,92)-(127,127) and the red electrode strip (0,94)-(15,121). |
| `Fabricator.png` (256×256) | 15 islands of 6 px or more; the largest are (0,0)-(255,67) [9,264 px], (0,76)-(159,165) [6,319 px] and (16,172)-(255,255) [892 px] | Many small parts (for example the controller (206,0)-(243,21)) lie in and around the three large groups. See the colour notes in section 11. |
| `AmmoPress.png`, `MetalPress.png`, `ChemLab.png` (256×128) | one island each, the whole file (0,0)-(255,127) | The background is opaque near-black (#101010 dominant, 58–66%), so the islands are not separable by alpha. Part rectangles come from the model UVs in [block-rendering-and-models.md](block-rendering-and-models.md). ChemLab is 96% opaque because of its semi-transparent glass regions. |

### 12.4 Atlas and sheet region roles (summary)

| Texture family | Region rectangles | Role |
|---|---|---|
| `-ctm` sheets, 2×2 tiles (32×32 sheet: 16 px tiles; 64×64 sheet: 32 px tiles) | A (0,0), B (w/2,0), C (0,h/2), D (w/2,h/2), each w/2 by h/2 | A no border, B left and right borders, C top and bottom borders, D corner notches |
| `-ctmv` sheets (`concrete_old2-ctmv`, `drillRod-ctmv`, 64×64) | four 32×32 quadrants: TL (0,0), TR (32,0), BL (0,32), BR (32,32) | TL single block, TR column top, BL column middle, BR column bottom (as in sections 2 and 10) |
| `-v9` sheet (`metalpanel_techgrid-v9`, 48×48) | nine 16×16 tiles on a 3×3 grid | one cell of a 3×3-block pattern |
| animated strips ([`oreTitanIron`](block-catalogue.md#registered-blocks), `oreUranium`, [`bugnesteggs`](block-catalogue.md#registered-blocks) 16×64; acid and milk 16×512 and 32×1024) | frame n is the rectangle (0, n × frame height) to (frame width − 1, (n+1) × frame height − 1) | one animation frame each; counts and times are in each entry's Anim column |
| overlay sheets (`screen_overlay_front`, `controlpanel_overlay_front`, 32×256) | 8 frames of 32×32 at y = 0, 32, … 224 | animated front overlay (8 frames × 2 ticks) |

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
| 12. Region maps (no new files) | 0 |
| **Total** | **163** |
