# Flag post: clean-room model notes

Design notes for rebuilding the Techguns **Camp Flag post** (a tall pole on a stepped base, with three waving flag panels) in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates.

**Units and axes.** One unit = 1/16 block. **Y points up.** Origin = the block's bottom-north-west corner; the block's own cell spans 0..16, but this model is **64 tall** (four blocks), so it draws well above its cell. The pole stands at the cell center (X 8, Z 8). **The flag extends toward -X (west)** from the pole, on the plane Z = 8, so it faces north and south. Converting from the original: built Y-down; the renderer moves the model to the cell center, one and a half blocks up, and flips it 180 degrees about Z; block X = 8 minus the original X, block Y = 24 minus the original Y, block Z = 8 plus the original Z. No scale. Rounded to half a unit.

## 1. Gestalt

A **slim metal flagpole** on a small stepped plinth, with a **white, red and blue star flag** flying near the top. The pole is thicker at the bottom and thinner higher up, like a telescoping mast, and ends in a small cap. The flag **waves** in a gentle rhythmic ripple. It is tall and thin, and mostly empty space.

Defining features in order of importance:

1. **A very tall, thin silhouette** (64 tall, never wider than 10).
2. **The waving three-panel flag** at the top: white with red stripes and a blue disc with a white star.
3. **A stepped, tapering pole** (thick plinth, thick lower pole, thinner mid pole, thin upper pole, cap).
4. **A weathered stone-gray plinth** with a lighter pole above.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total height | 64 (4 blocks) |
| Base plate | 10 x 2 x 10, Y 0 to 2 |
| Plinth block | 5 x 14 x 5, Y 2 to 16 |
| Lower pole | 3 x 23 x 3, Y 16 to 39 |
| Upper pole | 2 x 23 x 2, Y 39 to 62 |
| Cap | 3 x 2 x 3, Y 62 to 64 |
| Flag | 18 wide x 14 tall, no thickness, Y 47.5 to 61.5, from X 7 to X -11 |

All the pole pieces are centered on X 8, Z 8.

## 3. Part by part

### Base plate
- A flat **10 x 2 x 10** slab on the floor: X 3 to 13, Z 3 to 13, **Y 0 to 2**.

### Plinth
- **5 x 14 x 5**, centered, **Y 2 to 16**, standing on the base plate (5.5 of margin each side on the slab).
- The lower, thickest part of the mast; stone-gray.

### Lower pole
- **3 x 23 x 3**, centered, **Y 16 to 39**, on top of the plinth.

### Upper pole
- **2 x 23 x 2**, centered, **Y 39 to 62**, on top of the lower pole. Together the poles are a two-step taper, each 1 unit narrower in total than the one below.

### Cap
- **3 x 2 x 3**, centered, **Y 62 to 64**, a small knob on top; wider than the upper pole (it overhangs by 0.5 each side).

### Flag
- Three flat panels, each **6 wide x 14 tall and zero thick**, side by side with no gap, all in the plane Z = 8 and at **Y 47.5 to 61.5** (the top edge is 0.5 below the cap's base, 2.5 below the top). They run from the upper pole's west face at X 7 out to X -11: panel one from X 7 to 1, panel two from X 1 to -5, panel three from X -5 to -11.
- Each panel is visible from both sides (the back is a mirror image).
- Hinge: panel one is hinged at the pole (X 7); panel two at its own start (X 1); panel three at its own start (X -5).

## 4. Silhouette checks

- **Side (looking along the flag's plane edge-on):** a thin vertical mast: a 10-wide, 2-tall footing, a 5-wide plinth, a 3-wide pole and a 2-wide pole, and a 3-wide cap. The flag is a hairline (edge-on).
- **Front and back (looking at the flag):** the same mast, narrow, with an **18 x 14 flag** at the top reaching sideways to the west; on the side facing north it flies out to the viewer's right, on the south side to the left.
- **Top:** a 10 x 10 base with a 5 x 5 plinth and a lighter 3 x 3 pole; the flag is a thin line of 18 long reaching west.

## 5. Materials and palette

| Part | Material | Base | Highlight | Shadow |
|---|---|---|---|---|
| Base plate, plinth | weathered concrete or old steel | #5F5D58 | #7A7570 | #3C3A37 with rust specks #6B4A38 |
| Lower and upper pole, cap | brushed light metal | #C4C4C4 | #D4D4D4 to #DCDCDC | #9C9C9C and #949494 |
| Flag white | cloth | #D0D0D0 | #E0E0E0 | #A4A4A4 (folds) |
| Flag red | cloth | #B80808 | #C80808 | #980808 |
| Flag blue disc | cloth | #0808A8 | #0808C8 | #080898 |
| Star | cloth | #D8D8D8 | | |

Shading: the plinth is dark, speckled and grimy, getting lighter near the top; the poles are light and smooth with faint vertical streaks; the plinth's top face has a lighter square inset. The flag has a white field, a **red band near the top and a red band near the bottom**, and a **blue disc with a white five-pointed star centered** across the middle panel (the disc spans the panel borders a little). Fine crumple shading in the cloth.

## 6. In-game presentation

- A block entity drawn with this model; it ignores facing. The model sticks out far above the block cell, so its render bounds must be enlarged (at least 4 blocks up and 1 block to the west).
- The item form is not part of this model.

## 7. Motion

The flag waves; everything else is fixed. Time is the world tick count (integers, 20 per second), with no partial-tick smoothing, so the wave steps once per tick.

- All three panels swing about **vertical axes** (Y), sweeping each panel's free end back and forth horizontally.
- Each panel's angle is **30 degrees times sin(2 pi times phase)**, giving +-30 degrees. The periods are **60 ticks for panel one, 50 for panel two, 40 for panel three**.
- The motion is **chained**: panel two is attached to panel one's free end and inherits panel one's swing; panel three inherits both. So panel three's free end moves most, up to about 90 degrees combined at the extreme.
- Hinge points are each panel's pole-side edge: X 7, X 1 and X -5 (block X).

## 8. What our earlier attempts got wrong

GregGuns has only a **placeholder cube** for the flag post so far; there is no real model attempt to compare. The builder should avoid: a model confined to one block (this is 64 tall), a single rigid flag (three chained panels that wave), a uniformly thick pole (it tapers in steps), and a flag fixed to the pole's centerline (it starts from the pole's west face).

## 9. Acceptance checklist

- [ ] Total height is 64; the pole is centered at X 8, Z 8.
- [ ] Base plate 10 x 2 x 10, plinth 5 x 14 x 5, lower pole 3 x 23 x 3, upper pole 2 x 23 x 2, cap 3 x 2 x 3, stacked in that order.
- [ ] The three flag panels, each 6 x 14, are zero-thickness, side by side from X 7 to X -11, at Y 47.5 to 61.5.
- [ ] The flag reads as white with a red stripe top and bottom and a blue disc with a white star.
- [ ] Both sides of the flag are visible.
- [ ] Panels swing about vertical axes by +-30 degrees with periods 60, 50 and 40 ticks, chained.
- [ ] Plinth is dark and weathered, poles are light.
- [ ] The render bounds cover the full 64 height and the 18-unit flag span.
- [ ] No part floats.

## 10. Unsure

- **Flag direction.** I derived "toward -X (west)" from the renderer math, and the renders agree (the flag flies to the right as seen from the north). Confirm in game.
- **Wave direction and phase.** The sign of the swing is flipped by the renderer's frame; only the period and amplitude matter visually. Panels start in phase at time zero.
- **Flag artwork.** I read it from renders and a strip layout; the stripe widths are approximate (about 2 units each).
- **Plinth wear.** The rust specks are small and I am not sure of their count.
