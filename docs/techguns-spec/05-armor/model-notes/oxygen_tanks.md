# Oxygen Tanks: clean-room model notes

Design notes for rebuilding the Techguns **Oxygen Tanks** (scuba tanks, a back item) as the GregGuns `oxygen_tanks` worn model. Written by the dirty-room analyst; no source, box lists or UV data. In the original the item class is a thin gameplay class; the visual comes from a separate "oxygen flasks" model and renderer.

**Units and axes.** One unit = 1/16 block = one texel at 64x32 skin scale. **Y down**, the player faces **-Z**, the player's right is **-X**. Origin: the body's rotation point (top-center of the torso). The torso is x -4..4, y 0..12, z -2..2, so the torso's back face is **z = +2**. Half-unit rounding.

## 1. Gestalt

**Two stubby diving cylinders strapped side by side to the back**, joined by a small black harness block. Pale gray bottles with **two black bands each** and a small valve nub on top. Plain, utilitarian, no color accent. It is the smallest and quietest of the back items.

1. Two gray cylinders, 3 x 9 x 3, standing upright with a 2-unit gap.
2. **Two black bands** around each cylinder (near top and near bottom).
3. A **black harness block** between them.
4. A small **valve** on top of each cylinder.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Overall width | 8 (x -4..4), exactly the torso width |
| Overall height (valve top to cylinder bottom) | 10 (y -1..9) |
| Depth | cylinders 3 (z 2.5..5.5); harness block 3 (z 2..5) |
| Cylinder | 3 x 9 x 3 |
| Gap between cylinders | 2 (x -1..1) |
| Harness block | 2 wide, 6 tall, 3 deep |
| Valve | 2 x 1 x 2 |

## 3. Part by part

### Cylinders
- Right x -4..-1, left x 1..4; both y 0..9, z 2.5..5.5. Square-section boxes; roundness is painted (lighter vertical center stripe, darker edges).
- Their inner faces are 2 apart, with the harness block between. Their outer faces align with the torso's sides (x +-4).
- Bands: two black rings per cylinder, each about 1 tall, around all four sides. The upper at y about 2..3, the lower at y about 6..7.
- The bottom end is a slightly darker gray (no separate nozzle).

### Harness block
- 2 wide (x -1..1), 6 tall (y 1..7), 3 deep (z 2..5). Black. It touches the torso's back face (z = 2) and links the two cylinders.
- It is 0.5 shallower than the cylinders at the back (z 5 vs 5.5) and 0.5 proud at the front (z 2 vs 2.5).

### Valves
- 2 x 1 x 2 each, centered on a cylinder's top: right x -3.5..-1.5, left x 1.5..3.5; y -1..0, z 3..5.
- Light gray, a touch lighter than the bottle. A nub, not a tube.
- Their upper front edge (z 3..4, y -1..0) slips into the head box's lower back (the head reaches z 4), so it is hidden unless the head tilts down.

## 4. Silhouette checks

- **Back:** two gray bars with black bands, a black block between them, nubs on top. The bands cross the full bar width.
- **Side:** a vertical bar 3 deep and 10 tall; the harness shows as a dark sliver 0.5 in front.
- **Top:** two 3x3 gray squares (each with a 2x2 valve inset) joined by a black square between them. Overall 8 x 3.
- **Worn standing:** as wide as the torso, from the shoulder line down to 3 above the hips (bottom y 9 of 12). The valve rises 1 above the torso top.

## 5. Materials and palette

| Part | Material | Base | Highlight | Shadow |
|---|---|---|---|---|
| Cylinder body | painted steel | #888888 | #989898 | #585858 |
| Cylinder top and bottom | | #686868 | | #484848 |
| Bands | black strap | #181818 | #282828 | #080808 |
| Harness block | black | #080808 | #181818 | |
| Valve | light steel | #989898 | #A8A8A8 | #686868 |

One skin, no camo variants. The texture sheet has small cyan and gold details that do not show on the worn model.

## 6. In-game presentation

- **Slot:** the back slot. Drawn only when the slot's "show" toggle is on (the original honors it for this item).
- **Attachment:** the body part. Scale 1/16, no extra offset, no inflation; the coordinates above are body-frame.
- **Over a chestplate:** the chestplate (body inflated by 1, z up to 3) swallows the harness block's first unit (z 2..3) and the cylinders' first 0.5 (z 2.5..3). The cylinders stand 2.5 proud of the chestplate. Their outer faces (x +-4) sit 1 inside the chestplate's sides (x +-5), so the pack looks narrower than the chestplate shoulders.
- **Our current state:** no worn model; invisible when worn.

## 7. Motion

- Follows the body's forward lean when sneaking (about 29 degrees), pivoting about a point 1 below the neck line and 2 behind the body center (the original's own pivot; a rigid body attachment is an acceptable simplification). Nothing else moves.
- No animation, no glow, no particles and no sound on the model. Gameplay: it tops up the wearer's breath when low, with no visual.

## 8. What our earlier attempts got wrong

No worn model has been built. Likely traps: making the harness block full height (it is 6, not 9); leaving out the bands; making the pack wider than the body; floating it (the harness must touch the torso).

## 9. Acceptance checklist

- [ ] Two cylinders 3x9x3, x -4..-1 and 1..4, y 0..9, z 2.5..5.5.
- [ ] A gap of 2 between them, filled by a black block 2x6x3.
- [ ] The block touches the torso back at z = 2.
- [ ] Two black bands per cylinder, about 1 tall, at about y 2..3 and 6..7.
- [ ] Valves 2x1x2 on top, reaching y -1.
- [ ] Overall width 8, height 10.
- [ ] Bottle gray #888888 with a lighter center stripe.
- [ ] Harness block near-black.
- [ ] Leans with the body when sneaking and nothing else.
- [ ] Hidden when the slot toggle is off.
- [ ] Symmetric left to right.
- [ ] No part floats.

## 10. Unsure

- **Band heights** are estimated from a low-resolution render, plus or minus 0.5.
- **Whether the bands wrap all four faces.** They appear on every visible face in all views, so I say yes.
- **Cylinder end darkness** is a reading from the render.
