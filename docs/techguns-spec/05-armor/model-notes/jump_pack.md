# Jump Pack: clean-room model notes

Design notes for rebuilding the Techguns **Jump Pack** (the "jumppack" back item) as a worn model in GregGuns (`jump_pack`). Written by the dirty-room analyst from the original's model class, renderer and texture. The builder should be able to work from this page alone. No source, no box lists and no texture coordinates are given; the builder designs its own UV layout.

**Units and axes.** One unit = 1/16 block = one texel at 64x32 skin scale. **Y points down**, the player **faces -Z**, the player's right is **-X**. Everything below is in the **vanilla body frame**: origin at the body's rotation point (the middle of the top of the torso, between the shoulders). The torso box is x -4..4, y 0..12, z -2..2, so **z = +2 is the back face of the torso**. Rounded to half a unit unless it matters.

**Important note on the reference renders.** The sheet `model-ab/singles/acc_jump_pack.png` shows the *Jetpack* variant (it has side wings). The Jump Pack proper is the **same model without the wings**. This page describes the wingless Jump Pack. The wing frames are mentioned once in section 3 so the builder knows what to leave out.

## 1. Gestalt

A **compact rocket-pack**: two fat pale-gray cylinders standing side by side on the wearer's back with a dark, narrow spine between them, each cylinder topped by a **small yellow-and-black checkered cap**. It reads as "two scuba-style bottles with hazard-striped nozzles", like the Oxygen Tanks but wider, with a thicker center and hazard caps. It is symmetric left to right and sits high on the back, from shoulder height to just above the waist.

Defining features in order of importance:

1. **Two tall, light-gray cylinders** (3 wide, 9 tall, 3 deep, boxes) at the left and right edges of the body width.
2. **Yellow and black checkered caps** (2x2x2) on top of each cylinder. They are the only bright color.
3. **A dark central spine** (4 wide, 7 tall) between the cylinders, deeper than they are, with a few lighter strap bands.
4. Dark lower ends on the cylinders (nozzles).

## 2. Overall proportions

| Measure | Value |
|---|---|
| Overall width (outer faces of the cylinders) | 10 (x -5..5) |
| Overall height (cap top to cylinder bottom) | 11 (y -2..9) |
| Overall depth (spine front to spine back) | 4 (z 2..6); the cylinders alone are 3 deep (z 2.5..5.5) |
| Cylinder | 3 x 9 x 3 |
| Gap between the cylinders | 4 (x -2..2), filled by the spine |
| Spine | 4 wide, 7 tall, 4 deep |
| Caps | 2 x 2 x 2 |

## 3. Part by part

All parts are boxes, and the original builds them as one rigid unit hung from the spine.

### Spine (central pack)
- 4 wide (x -2..2), 7 tall (y 1..8), 4 deep (z 2..6). Centered on the body's midline.
- Touches the torso's back face exactly (z = 2) and pokes 0.5 behind and 0.5 in front of the cylinders' depth range, so from the side you see a thin dark rim in front of and behind each cylinder.
- It is shorter than the cylinders: it starts 1 below their tops (y 1 vs 0) and ends 1 above their bottoms (y 8 vs 9).
- NOT a cylinder; a plain square box.

### Right and left cylinders
- Each 3 wide, 9 tall, 3 deep. Right: x -5..-2. Left: x 2..5. Both y 0..9, z 2.5..5.5.
- They butt against the spine's sides (the spine ends at x -2 and 2). Their outer faces (x +-5) are 1 unit outside the torso width and flush with the outer edge of a chestplate's shoulders.
- They overhang the spine at the top (1) and bottom (1).
- Not round: square-section boxes. "Roundness" comes from the paint (a lighter vertical stripe in the middle of each face, darker at the edges).

### Caps
- A 2 x 2 x 2 cube on top of each cylinder, centered on it: right x -4.5..-2.5, left x 2.5..4.5, y -2..0, z 3..5.
- The cylinders span z 2.5..5.5, so each cap sits 0.5 in from the cylinder's front and back faces.
- The caps' top-front edge (z 3..4, y -2..0) overlaps the lower back of the head box (the head reaches z 4 and y 0). It is hidden in the head when the head is level and shows only when the head tilts down.

### Wings (NOT in the Jump Pack; Jetpack only)
- The Jetpack adds a thin panel, a top rim and an outer edge on each side, reaching x +-11. **Leave these out.**

## 4. Silhouette checks

- **Back:** two pale-gray bars with a dark rectangle between, and caps over the bars. Yellow-black checks on top. The spine shows lighter horizontal bands (straps) across its height.
- **Front:** same outline; the spine is plain dark (visible only without a chestplate).
- **Side:** a tall narrow bar, 4 deep at the spine and 3 deep at the cylinder, 11 tall including the cap, standing flat against the back. The cap pokes above the shoulder line.
- **Top:** two 3x3 gray squares with a 2x2 cap on each, and a dark 4x4 square between. Overall 10 x 4.
- **Worn on a standing player:** covers the back from shoulder blades to 1 above the hips. The cylinder tops rise 1 above the chest top and the caps 2 above. The pack stays behind the arms (arms are at z <= 2).

## 5. Materials and palette

| Part | Material | Base | Highlight | Shadow |
|---|---|---|---|---|
| Cylinders | light brushed steel | #A8A8A8 | #B8B8B8 (center stripe) | #686868 (edges) |
| Cylinder lower ends | black nozzle | #282828 | #383838 | #181818 |
| Caps | hazard checker | yellow #F0D818 and black #181818 | | gray rim #787878 |
| Spine | dark brown-gray metal | #382828 | #484038 | #282018 |
| Spine straps | lighter gray bands | #888888 | #A8A8A8 | |

Camo variants of the original (shared with the Jetpack): default (above), **bronze** (browns and golds: #B89858, #C8A858, #887838, dark #282818), **black** (#181818 to #383838, #484848 trim, a few red #980808 accents), **dark green** (#182818 to #283828, near-black, a few #F8F8F8 specks). GregGuns has one texture (`jump_pack.png`), so only the default look is needed.

## 6. In-game presentation

- **Slot:** the back (GregGuns `wings` bauble) slot. In the original it is always drawn when worn; the inventory "show slot" toggle does not hide it.
- **Attachment:** the **body** part. The model is a biped-shaped object that uses the player's body transform: scale 1/16 per unit, no extra offset, no inflation. The coordinates in section 3 are literally body-frame. The renderer applies no further translation.
- **Over a chestplate:** a chestplate inflates the body by 1 (x +-5, y -1..13, z -3..3). The spine starts at z 2, so its first unit (z 2..3) is **inside the chestplate's back wall** and invisible. The cylinders (from z 2.5) are 0.5 buried and stand 2.5 proud. The cylinders' outer faces (x +-5) align with the chestplate's side faces. Nothing clips visibly, because the pack starts inside the shell.
- **Pack vs head:** the caps' corners pass into the head box's lower back (see Caps).
- **Our current state:** no worn model exists; the pack is invisible when worn.

## 7. Motion

- The pack follows the **body's** pitch only. When the player sneaks, the body leans forward about 0.5 radian (about 29 degrees) and the pack leans with it. The original rotates the pack about its own pivot (a point 1 below the neck line and 2 behind the body center) instead of the body's, so it shifts slightly differently from a rigid attachment. A rigid body attachment is an acceptable simplification.
- Nothing follows the head, arms or legs. No part moves independently.
- **Effects:** the renderer is shared with the Jetpack. It spawns two small flame particles (animated flame sprite, about 8 ticks of life, growing as they age, random rotation, drifting down) below the pack on the wearer's back while a "jump key held" flag is set. That flag is set only by the Jetpack item's tick, so for the Jump Pack **no continuous flame shows**. The Jump Pack's effects are one-shot world effects: a "JumpPackBoost" burst on the player and a steam-jump sound on each boost, and a "JumpPackFall" burst when it absorbs a fall. None are part of the model. No glow layer and no emissive texture.

## 8. What our earlier attempts got wrong

There is no worn model yet, so there is nothing to compare. Likely traps for the builder:

- Using the render on the sheet (which has wings) as the Jump Pack.
- Making the cylinders round or taller or shorter than 9.
- Floating the pack: it must touch the torso's back at z = 2.
- Omitting the checkered caps (the only color accent).
- Making the spine as tall as the cylinders (it is 2 shorter).

## 9. Acceptance checklist

- [ ] Two cylinders 3x9x3 at x -5..-2 and 2..5, y 0..9.
- [ ] Spine 4x7x4 between them, starting exactly at the torso's back face (z = 2).
- [ ] The spine is 1 shorter at top and bottom than the cylinders and 0.5 proud front and back.
- [ ] Caps 2x2x2, centered on the cylinder tops, reaching y -2.
- [ ] Caps yellow-and-black checkered.
- [ ] Cylinders light gray (#A8A8A8) with a lighter center stripe; lower ends dark.
- [ ] Spine dark with 2 to 3 lighter strap bands.
- [ ] No wings.
- [ ] Overall width 10, height 11, depth 4.
- [ ] Outer cylinder faces flush with a chestplate's sides when one is worn.
- [ ] The pack follows body lean when sneaking and nothing else.
- [ ] Every part touches a neighbor.
- [ ] Symmetric left to right.

## 10. Unsure

- **Strap bands on the spine.** Seen only at low resolution in the (wing-variant) render; count (2 or 3) and height are my estimate.
- **The render shows the wing variant.** The wingless geometry comes from the model description, not a render of it.
- **Camo skins** of the Jump Pack are the four Jetpack skins by reuse; whether GregGuns needs any is a product decision.
- **Sneak pivot behavior** is read from the model code and not confirmed in game.
