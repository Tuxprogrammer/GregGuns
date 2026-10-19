# Anti Gravity Pack: clean-room model notes

Design notes for rebuilding the Techguns **Anti Gravity Pack** (back item) as the GregGuns `anti_gravity_pack` worn model. Written by the dirty-room analyst from the original's model, renderer and textures; no source, box lists or UV data.

**Units and axes.** One unit = 1/16 block = one texel at 64x32 skin scale. **Y down**, the player faces **-Z**, the player's right is **-X**. Origin: the body's rotation point (top-center of the torso). The torso is x -4..4, y 0..12, z -2..2, so its back face is **z = +2**. Half-unit rounding.

## 1. Gestalt

A **wide, flat steel slab on the back with two thin pillars at its sides and a glowing cyan diamond on its rear face**. It reads as a boxy gray backplate with a small crystal "core" on the outside and a few black clamps. From the front or side it is quiet and gray; from behind the **cyan diamond** is the focus.

1. **A wide gray slab** (6 x 6 x 3) with a thinner plate stuck to its back.
2. **Two thin vertical pillars** (2 x 7 x 2) at its left and right.
3. **A cyan diamond** (a 3x3x3 cube turned 45 degrees about the front-to-back axis) on the back.
4. **Four small black clamps** (2x1x1) joining slab and pillars.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Overall width (pillar outer faces) | 10 (x -5..5) |
| Overall height | 7 (y 0.5..7.5), pillars; slab 6 (y 1..7) |
| Overall depth | 5.5 (z 2..7.5) |
| Slab | 6 wide, 6 tall, 3 deep (x -3..3, y 1..7, z 2..5) |
| Back plate | 5 x 4 x 1 |
| Pillars | 2 x 7 x 2 |
| Core | 3 x 3 x 3 cube turned 45 degrees: diamond about 4.2 across |
| Clamps | 2 x 1 x 1 (four) |

## 3. Part by part

### Slab
- 6 wide (x -3..3), 6 tall (y 1..7), 3 deep (z 2..5), centered on the midline. Its front face touches the torso's back face (z = 2).

### Pillars
- Right x -5..-3, left x 3..5; y 0.5..7.5 (7 tall, 0.5 taller than the slab at each end); z 2.5..4.5 (2 deep, centered in the slab's depth).
- They touch the slab's sides (x +-3). Plain boxes, lighter-edged.
- Their outer faces are 1 outside the torso width.

### Back plate
- 5 wide (x -2.5..2.5), 4 tall (y 2..6), 1 deep (z 5..6), centered on the slab's back face. Slightly darker than the slab, like an inset panel with a visible rim around the core.

### Core (the diamond)
- A 3x3x3 cube turned 45 degrees about the front-to-back (Z) axis, so seen from behind it is a **diamond** about 4.2 wide and 4.2 tall, centered at about x 0, y 4 (diamond spans y about 2 to 6). Depth 3 (z 4.5..7.5): it sinks 1.5 into the slab and plate and stands 1.5 proud of the plate.
- Cyan, glossy-looking, with a faceted lighter and darker pattern. The only vivid color.
- NOT round, NOT a flat sticker: a solid prism. Its corner edges poke past the plate's rim at top, bottom and sides.

### Clamps
- Four small boxes, each 2 x 1 x 1, black. Two on each side, at y about 2.5..3.5 and 4.5..5.5, z 4.5..5.5. Right pair x -4..-2, left pair x 2..4. They bridge from the back plate's edge across the pillar's back face, so from behind they read as two black pairs flanking the diamond.

## 4. Silhouette checks

- **Back:** gray frame: two tall pillars, the plate, the cyan diamond in the middle, black pairs of clamps beside the diamond. Most distinctive view.
- **Front:** a plain gray rectangle (slab) with the pillars at the sides; no color. Hidden by any chestplate.
- **Side:** a gray vertical bar 7 tall; at the back a teal diamond edge pokes out 1.5 past the plate and two small black nubs.
- **Top:** a gray slab with pillars at the ends (like a wide T), a short step for the plate, a cyan wedge at the rear.
- **Worn standing:** a boxy gray backplate between the shoulder blades and waist, narrower than the shoulders-plus-chestplate, with the cyan diamond visible when the player faces away.

## 5. Materials and palette

| Part | Material | Base | Highlight | Shadow |
|---|---|---|---|---|
| Slab, pillars | brushed steel | #A8A8A8 | #C8C8C8 | #686868 (#585858 edges) |
| Back plate | darker steel | #787878 | #989898 | #585858 |
| Core | cyan crystal | #10B0D8 | #50D8F0 | #0C6A80 |
| Clamps | black | #282828 | #383838 | #181818 |

Camo variants (original has 5; GregGuns `anti_gravity_pack*.png` has the same five):
- **default:** steel gray as above, cyan core.
- **bronze:** gold and brown body (#C8A858, #D8B858, #887838, #685828), same dark clamps.
- **black:** #181818 to #383838 body, #484848 trim, red accents #981818 and #881808 (the core turns red).
- **dark green:** #182818 and #081808 body, #183818 mid, cream accents #D8D8C8.
- **silver:** #D8D8D8, #E8E8E8, #F8F8F8 highlights, #989898 shadows.

## 6. In-game presentation

- **Slot:** back (`wings`). Drawn only if the slot's "show" toggle is on.
- **Attachment:** the body part (the original uses the shared flasks renderer; its own dedicated renderer class exists but is unused). Scale 1/16, no extra offset, no inflation; coordinates are body-frame.
- **Over a chestplate:** the slab starts at z 2, so 1 unit of it (z 2..3) is inside the chestplate's back wall. The pillars (z from 2.5) are 0.5 buried. The pillars and core are visible past the chestplate: slab and plate 2 units proud, core 4.5 units proud (z up to 7.5 versus 3). The pillar tops (y 0.5) sit just below the chestplate top (y -1), so the pack does not rise above the shoulders.
- **Our current state:** no worn model; invisible when worn.

## 7. Motion

- Follows the body's pitch only (leans about 29 degrees when sneaking, pivoting about a point 1 below the neck and 1 to the player's right of center and 2 behind the body; a rigid body attachment is an acceptable simplification).
- No animation and no glow layer in the model: the cyan is painted, not emissive. Gameplay effects (a hover ring particle effect, a start-up sound) are commented out or world-side and not part of the worn model. A builder may add a faint emissive pulse on the core as a deviation, but the original has none.

## 8. What our earlier attempts got wrong

No worn model exists. Traps: making the core a flat square (it is a turned solid cube); centering the slab wrong (the original is centered, despite a one-unit offset in its setup); leaving out the black clamps; making it as tall as the other packs (it is only 7 tall and starts at y 0.5); dropping the cyan accent.

## 9. Acceptance checklist

- [ ] Slab 6x6x3 at x -3..3, y 1..7, touching the torso back (z = 2).
- [ ] Two pillars 2x7x2 at x -5..-3 and 3..5.
- [ ] Back plate 5x4x1 on the slab's back face, centered.
- [ ] Core is a 3x3x3 cube turned 45 degrees (a diamond from behind), centered at y about 4.
- [ ] The core stands 1.5 proud of the plate (z 7.5).
- [ ] Four black clamps 2x1x1 in two stacked pairs per side, flanking the core.
- [ ] Core cyan (#10B0D8), body steel gray (#A8A8A8).
- [ ] Overall width 10, height 7, depth 5.5.
- [ ] Pillar tops below the chestplate's top.
- [ ] Leans with the body when sneaking and nothing else.
- [ ] Hidden when the slot toggle is off.
- [ ] Symmetric left to right.
- [ ] Every part touches a neighbor.

## 10. Unsure

- **The sign of the core's 45-degree turn** is irrelevant for the diamond, but its center is derived as centered in x from the pivot math; the render agrees.
- **Clamp heights** read from the render within 0.5.
- **Whether the clamps are visible from the front**: they are mostly hidden behind the slab and pillars.
- **Pillar height** (7 against the slab's 6) is from the numbers; the render shows them slightly taller than the slab.
