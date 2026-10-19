# T2 Beret: clean-room model notes

Design notes for rebuilding the Techguns **Beret** worn look in GregGuns as `t2_beret`, with the variants `black` and `green`. Written by the dirty-room analyst from the model class, armor item class and the three textures. They contain no code, no box lists and no texture coordinates; the builder designs its own layout.

**Units and frame.** One unit = 1/16 block = one texel of a 32x32 skin (the real textures are drawn at 2x). **Y points down**, the player **faces -Z**, the player's **right side is -X**, origin at the neck (the head's rotation point). Vanilla head box: x -4..4, y -8..0, z -4..4. "Inflated by s" means grown by s on every side.

## 1. Gestalt

A **flat, thick military beret** on top of the head, **off-center toward the player's left**, with a **droopy fold hanging down over the right temple**. Single flat color, with a darker inside visible only from below.

1. **Pancake crown**: about 7 x 3 x 9 over the top of the head.
2. **Droop on the right**: a slab tilted 35 degrees down and outward, reaching about eye level.
3. **One flat color** with subtle noise.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Crown | about 7 wide, 3 tall, 9 deep |
| Crown center | 1 unit toward the player's left of the head center |
| Crown top above head top | about 1 |
| Droop slab | about 4 long, 3 thick, 9 deep |
| Droop tilt | 35 degrees, outer end down |
| Total width (droop to crown) | about 10.5 (x -6 to 4.5) |
| Total depth | about 9 (z -4.5 to 4.5), 1 more than the head |

## 3. Part by part

The beret model has **no head box**: the player's own head shows. Its two boxes attach to the head and turn with it.

### Crown
- A 6 x 2 x 8 box inflated by 0.6: **about 7.2 wide, 3.2 tall, 9.2 deep**.
- X -2.6 to 4.6 (centered at +1, toward the player's left), Y -9.1 to -5.9, Z -4.6 to 4.6.
- Its lower 2 units are buried in the head (head top is y -8), so about 1 unit shows above the head, with a 0.6 overhang at the front, back and left.
- NOT round: a slab with flat top, flat sides and square corners.

### Droop
- A 3 x 2 x 8 slab inflated by 0.55: about **4 long, 3 thick, 9 deep**, hinged at its inner end on the crown's right upper edge (about x -2.7, y -8.3).
- **Tilted 35 degrees**, sloping down and to the player's right. Side-view corners (x, y): inner top (-2.6, -9), outer top (-6, -6.5), outer bottom (-4, -4), inner bottom (-1, -6.5). Depth z -4.5..4.5, matching the crown.
- Reaches about 2 beyond the head's right side; its lowest corner is at about y -4, roughly eye level.
- Touches the crown along the shared upper-right edge; its inner lower corner is buried in the head.
- NOT a brim: only on the right side.

## 4. Silhouette checks

- **Front:** a low rectangle on the head, shifted toward the viewer's right (player's left), with a drooped lobe on the viewer's left going past the head side and down to eye level.
- **Back:** the same, mirrored.
- **Side from the player's right:** the droop's big face, a tilted slab across the full head depth.
- **Side from the left:** a flat 3-tall, 9-deep rectangle overhanging the head by 0.6 at front, back and left.
- **Top:** a 9-deep, 10.5-wide rectangle, with the droop sloping away on the right.
- **Worn look:** a thick cap pulled over the right ear.

## 5. Materials and palette

Matte cloth with one or two steps of noise. Underside is darker.

| Variant | Base | Range | Interior and shadow |
|---|---|---|---|
| default (red) | #6A0000 | #650000 to #740000 | very dark red, near #200000 |
| `black` | #353535 | #323232 to #3E3E3E | near #101010 |
| `green` | #0C3107 | #0A2A06 to #0D3608 | near #051503 |

A thin dark line marks the seam between crown and droop.

## 6. In-game presentation

- Helmet slot only. Draws just the two boxes; the vanilla hat layer is hidden for this piece.
- One texture per variant (no two-layer split).
- No scale beyond the inflations. The variants are texture-only recolors.

## 7. Motion

Both boxes follow the head rigidly (yaw, pitch and the sneaking head offset). Nothing else moves.

## 8. What our earlier attempts got wrong

Our current model has a crown 8 x 3 x 10 centered at y -11..-8, a separate 8 x 1 x 10 lining, and a 3 x 4 x 10 flap tilted -35 degrees hanging at the head's bottom.
- **Crown floats above the head and is centered.** It should be about 7.2 x 3.2 x 9.2, shifted 1 toward the player's left, buried 2 into the head.
- **The lining is not in the original.** The dark inside is only paint on the underside.
- **The flap is wrong in place and size.** The original droop is about 4 x 3 x 9, hinged at the crown's right top edge and reaching eye level.
- **Red should be deep saturated red** (#6A0000), not brighter.

## 9. Acceptance checklist

- [ ] Exactly two parts: crown and droop. No head box, no lining.
- [ ] Crown about 7.2 x 3.2 x 9.2, centered 1 unit toward the player's left.
- [ ] Crown overlaps the head by 2 and shows about 1 above it.
- [ ] Crown overhangs front, back and left of the head by about 0.6.
- [ ] Droop about 4 x 3 x 9, tilted 35 degrees with the outer end down, on the player's right only.
- [ ] Droop's lowest corner near eye level (y -4), outer corner about 2 past the head side.
- [ ] Both parts follow the head only.
- [ ] Vanilla hat layer hidden.
- [ ] Red #6A0000, black #353535, green #0C3107.
- [ ] Underside darker than the outside.
- [ ] No brim on the left, front or back.
- [ ] One texture per variant.

## 10. Unsure

- **Interior colors** are read from the render's underside, not sampled.
- **Droop tilt direction**: computed from the box and rotation sign; matches the reference render (lobe on the player's right).
- **Variant order**: the three textures are red, black, green; I assume `t2_beret` default is red.
