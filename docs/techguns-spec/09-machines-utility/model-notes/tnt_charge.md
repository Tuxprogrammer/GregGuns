# TNT charge: clean-room model notes

Design notes for rebuilding the Techguns **TNT Charge** (a wall- or floor-mounted bundle of three red dynamite sticks with a detonator plate) in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderers and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates.

Related: [exp_charge](exp_charge.md) (same plate, different body).

**Units and axes.** One unit = 1/16 block. **Y points up.** Origin = the block's bottom-north-west corner; the block spans 0..16. **Default orientation (described below): the bundle hugs the north wall (Z = 0 side) and the detonator plate faces south (+Z).** Converting from the original: it is built Y-down; the renderer flips it 180 degrees about Z around the block center, so block X = 8 minus the original X, block Y = 24 minus the original Y, block Z = 8 plus the original Z. No scale on the block. Rounded to half a unit.

## 1. Gestalt

A **bundle of three fat red dynamite sticks** standing upright side by side, with a small **gray detonator plate** (green "RDT" lettering) strapped across the front of the middle. From the front it reads as a wide red block made of three vertical rounded cylinders; from above, as three red diamonds in a row. Compact and plain.

Defining features in order of importance:

1. **Three vertical sticks, red**, each a thick pillar about 12 tall.
2. **Diamond cross-section** from above (square pillars turned 45 degrees), overlapping in a zigzag row.
3. **A flat gray plate** on the front center, wide, low, thin, bearing green letters.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Stick height | 12 (Y 2 to 14) |
| Stick cross-section | a 4 x 4 square turned 45 degrees, so a diamond 5.7 across |
| Stick spacing | 4 apart (centers at X 4, 8, 12) |
| Bundle width | about 15.7 (X 0.2 to 15.8), nearly the full block |
| Bundle depth | about 5.7 (Z 0.2 to 5.8) |
| Plate | 8 wide x 4 tall x 2 deep, X 4 to 12, Y 6 to 10, Z 4 to 6 |

## 3. Part by part

### Sticks
- Three boxes, each **4 x 12 x 4**, each turned **45 degrees about the vertical axis** so each shows a diamond. Centers at **X 4, 8 and 12**, all at **Z 3**.
- **Y 2 to 14**. The middle stick sits about 0.1 unit lower than the outer two (it spans 1.9 to 13.9); this is negligible and optional.
- Because each diamond is 5.7 across and spacing is 4, neighbors **overlap** in the corners: from above you see a zigzag, not three separate diamonds.
- They hug the block's north wall: the diamonds' back corners are 0.2 from the wall.
- Plain cylinders-by-boxes: no caps, no fuse, no straps, no wire.

### Detonator plate
- A flat box **8 x 4 x 2**: X 4 to 12, **Y 6 to 10** (centered at mid height), **Z 4 to 6**.
- It lies on the south side of the bundle's middle: it overlaps the sticks' south corners, and sticks out about 0.2 beyond them. So it reads as a gray plate laid over the front of the bundle, covering the middle stick and about half of each outer stick.
- It carries the "RDT" label (section 5).

## 4. Silhouette checks

- **Front (from the south, plate side):** a red block about 16 wide and 12 tall, split into three pillars by dark vertical shading, with the gray plate (8 x 4) at mid height, centered.
- **Back (from the north):** the same red block, no plate.
- **Side:** a narrow 6-deep, 12-tall pillar of red, with the plate a thin gray stub at mid height on the south side.
- **Top:** three red diamonds in a row, overlapping, with the gray plate as a thin strip along the south side of the middle diamond.
- Negative space: the block's 2-unit top and bottom margins, and the corners beside the sticks, are empty.

## 5. Materials and palette

| Part | Material | Base | Highlight | Shadow |
|---|---|---|---|---|
| Sticks | glossy red paper-wrapped dynamite | #841414 | #941414 to #B02020 | #4C0C0C to #540C0C |
| Plate | gray plastic or metal | #4C4C4C | #606060 | #3C3C3C |
| Lettering | green "RDT" on a dark green-black field | #18A030 (green) | | #103020 |

Shading: each stick has a lit center and darker edges on the diamond's flat faces, about three red levels; the stick tops are darker red with a lighter diamond center. The plate is mid-dark gray with the green three-letter label, centered, blocky.

**Armed state:** a second texture is drawn once the charge is armed. It is the same except that a patch of about 80 pixels on the plate shifts from gray to red-pink (an indicator light or display going red). The builder may do this as a texture swap on the plate region only.

## 6. In-game presentation

- **Placed block (a block entity):** drawn with this model. It attaches to any of the six faces it was placed against. The default orientation above is the one for a charge on the south side of a wall (hugging the wall at Z = 0). For the other three walls, turn it about the vertical axis through the block center by 180 (against the south wall, hugging Z = 16), 90 and 270 degrees. For the floor, lay it back-down on the bottom face with the plate facing up; for the ceiling, back-up with the plate facing down. In all cases the bundle's rear touches the supporting face and the plate faces out into the block.
- **Item (hand, ground, icon):** the same model drawn at about 1.2 scale (first person, turned about 120 degrees, lowered) and 1.5 scale (held in third person, on the ground and in the icon, turned 45 and 135 degrees). The icon is the 3D model.

## 7. Motion

None. The armed state is only a texture swap. The ticking and explosion are handled elsewhere.

## 8. What our earlier attempts got wrong

GregGuns has only a **placeholder cube** for the TNT charge so far; there is no real model attempt to compare. The builder should avoid: one big red cube (it is three narrow diamonds), square sticks aligned with the axes (they are turned 45 degrees), a centered plate flush with the front (it overlaps the bundle's front corners), and adding a fuse, wires or timer, which the original lacks.

## 9. Acceptance checklist

- [ ] Three sticks, each 4 x 12 x 4 turned 45 degrees about the vertical axis, centers 4 apart (X 4, 8, 12).
- [ ] Sticks span Y 2 to 14 and hug the wall (Z center 3).
- [ ] The diamonds overlap each other in the top view (zigzag).
- [ ] Plate 8 x 4 x 2 at X 4 to 12, Y 6 to 10, Z 4 to 6, protruding just past the sticks.
- [ ] Bundle width is about 15.7 and depth about 5.7.
- [ ] Sticks are red (about #841414), plate gray (about #4C4C4C), lettering green.
- [ ] Armed variant recolors part of the plate to red.
- [ ] Mounts on all six faces by turning, with the rear against the support face.
- [ ] No fuse, caps, straps or wire.
- [ ] No part floats; the plate overlaps the sticks.

## 10. Unsure

- **Face numbering.** I derived which wall each rotation hugs from the renderer's math: the default hugs the north wall of the cell and corresponds to the "clicked south face" case. Floor and ceiling mapping was computed by hand; check it against placement.
- **Armed patch.** I know about 80 plate pixels change toward red; I did not see them rendered.
- **Plate overlap.** The 0.2 unit protrusion past the sticks is by numbers only.
