# Exp charge: clean-room model notes

Design notes for rebuilding the Techguns **advanced explosive charge** ("Exp charge": a dark green banded brick with a detonator plate) in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates.

Related: [tnt_charge](tnt_charge.md) (same plate and mounting rules).

**Units and axes.** One unit = 1/16 block. **Y points up.** Origin = the block's bottom-north-west corner; the block spans 0..16. **Default orientation: the brick hugs the north wall (Z = 0 side) and the detonator plate faces south (+Z).** Converting from the original: built Y-down, flipped 180 degrees about Z around the block center by the renderer; block X = 8 minus the original X, block Y = 24 minus the original Y, block Z = 8 plus the original Z. No scale. Rounded to half a unit.

## 1. Gestalt

A **squat, dark olive-green explosive brick** wrapped in a **pale blue-gray cross of straps**, with a **gray detonator plate with green "RDT" lettering** on the front. From the front it reads as a green block quartered by a horizontal and a vertical strap. It looks military and heavier than the TNT bundle.

Defining features in order of importance:

1. **A near-cubic dark green brick**, 10 x 10 wide and tall, 5 deep.
2. **A blue-gray strap cross** painted across its faces: one band around the middle, one band near each side edge.
3. **A gray plate** (8 x 4) on the front center with green letters.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Brick | 10 wide x 10 tall x 5 deep: X 3 to 13, Y 3 to 13, Z 0 to 5 |
| Plate | 8 x 4 x 2: X 4 to 12, Y 6 to 10, Z 4 to 6 |
| Overall depth | 6 (brick 5, plate sticks out 1 more) |
| Margin to the block | 3 on each side in X and Y; the brick rests on the wall |

## 3. Part by part

### Brick
- One box, **10 x 10 x 5**: **X 3 to 13, Y 3 to 13, Z 0 to 5**, centered in X and Y, back face on the wall plane (Z 0).
- No bevels or notches. The strap detail is painted.

### Detonator plate
- The same plate as the TNT charge: **8 x 4 x 2**, **X 4 to 12, Y 6 to 10, Z 4 to 6**.
- It overlaps the brick's front 1 unit (Z 4 to 5) and **sticks out 1 unit** beyond the brick's front face (Z 5 to 6). Centered on the front.
- It carries the "RDT" label.

## 4. Silhouette checks

- **Front (from the south):** a 10 x 10 dark green square quartered by straps, with the gray 8 x 4 plate in the middle.
- **Back:** the same square with no plate, strap cross visible.
- **Side:** a 10-tall, 5-deep rectangle with a horizontal pale strap at mid height and a 4-tall, 1-deep gray stub on the front, centered.
- **Top:** a 10 wide by 5 deep rectangle with two vertical blue-gray strap stripes near the left and right ends and a thin gray plate strip at the front center.

## 5. Materials and palette

| Part | Material | Base | Highlight | Shadow |
|---|---|---|---|---|
| Brick faces | dark olive-green casing | #242C24 and #2C342C | #2C3C3C | #1C241C |
| Straps | blue-gray tape | #344444 | #3C4C4C | #2C3C3C |
| Plate | gray | #444444 | #505050 | #3C3C3C |
| Lettering | green "RDT" | #18A030 | | #103020 |

Shading: nearly flat, very low contrast and dark; per-pixel noise of about two levels. The straps are a subtle lighter blue-gray band across the faces: on the front, one horizontal strap at mid height and two short vertical straps near the left and right edges; on the top and bottom, two strap stripes near the left and right ends; on the sides, a horizontal strap at mid height.

**Armed state:** a second texture has the same look, except that about 80 pixels of the plate change from gray to red-pink (an indicator going red). It can be done as a texture swap.

## 6. In-game presentation

- **Placed block (a block entity):** attaches to any of the six faces. Default orientation is as above; for the other walls turn it about the vertical axis through the block center by 180, 90 and 270 degrees; on the floor it lies back-down with the plate facing up; on the ceiling, back-up with the plate down. The brick's back always touches the support face.
- **Item (hand, ground, icon):** the same transforms as the TNT charge: about 1.2 scale (first person), 1.5 scale (held, ground, icon), turned and lowered to sit in view.

## 7. Motion

None. The armed state is only a texture swap.

## 8. What our earlier attempts got wrong

GregGuns has only a **placeholder cube** for this charge so far; there is no real model attempt to compare. The builder should avoid: a full-block cube (the brick is 10 x 10 x 5, hugging the wall), a plate flush with the front (it sticks out 1 unit), and a bright body (it is very dark green with subtle straps).

## 9. Acceptance checklist

- [ ] Brick is 10 x 10 x 5, at X 3 to 13, Y 3 to 13, Z 0 to 5.
- [ ] Plate is 8 x 4 x 2 at X 4 to 12, Y 6 to 10, Z 4 to 6, centered, sticking out 1 unit.
- [ ] Brick is very dark olive (about #242C24), straps blue-gray (about #344444).
- [ ] The strap cross reads on the front, with straps on the top and sides too.
- [ ] Plate gray with green "RDT"; armed variant turns a patch of it red.
- [ ] The back face lies on the supporting face in all six mounts.
- [ ] No part floats; the plate overlaps the brick.

## 10. Unsure

- **Strap layout.** I read the straps from a very dark, low-resolution texture; their exact widths are approximate (about 3 to 4 units).
- **Face numbering.** The mounting rules share the TNT charge's renderer; the same caveat applies.
- **Armed patch.** I know which pixels change but did not see them rendered.
