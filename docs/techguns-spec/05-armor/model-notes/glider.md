# Glider: clean-room model notes (folded pack and deployed wings)

Design notes for rebuilding the Techguns **Glider** (back item) as the GregGuns `glider` worn model. The original has **two states**: a **folded** backpack always worn, and **deployed wings** drawn only while gliding. Written by the dirty-room analyst; no source, box lists or UV data.

**Units and axes.** One unit = 1/16 block = one texel at 64x32 skin scale. **Y down**, the player faces **-Z**, the player's right is **-X**. Origin: the body's rotation point (top-center of the torso). The torso is x -4..4, y 0..12, z -2..2, so its back face is **z = +2**. Half-unit rounding. The reference render (`acc_glider.png`) shows both states together.

## 1. Gestalt

**A brown canvas knapsack with a pair of long, flat, segmented wings.** Folded: a tall brown box on the back. Deployed: a **flat, rectangular, stepped wing about 33 units wide** spreads symmetrically from the pack, with pale cream-green sail panels in light gray frames. It reads as a hang-glider with square panels, not a bat wing.

1. **Brown backpack** (7 x 10 x 4), almost the whole back.
2. **Wings**: 2 panels per side, 33 units wide in total, in thin gray frames with pale cream sail panels.
3. **Stepped trailing profile**: each outer panel sits 1 unit lower than the inner one.
4. **Wing plane flush with the back of the pack**: the wing is thin (2 deep with a 1-deep sail) and sits against the pack's rear half.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Folded pack | 7 wide x 10 tall x 4 deep (x -3.5..3.5, y 0.5..10.5, z 2..6) |
| Deployed span | 33 (x -16.5..16.5) |
| Wing height | inner panel 8 (y 1..9), outer 7 (y 2..9) |
| Wing depth | 2 frame (z 4..6), 1 sail (z 4.5..5.5) |
| Each wing | 2 panels: inner 6 long and outer 6 long, plus end struts 1 wide |

## 3. Part by part

### Folded pack (state A, always worn when the slot is shown)
- A plain box: 7 wide (x -3.5..3.5), 10 tall (y 0.5..10.5), 4 deep (z 2..6). Touches the torso's back face (z = 2). It runs from just below the shoulder line to 1.5 above the torso's bottom.
- Brown canvas; a lighter gray strip is inset on its side faces. No straps, buckles or pockets in the geometry.

### Wings (state B, drawn when airborne and sneaking)
Per side (right is negative X; the left is a mirror image). Panels, from the pack outward:
- **Inner panel:** a frame x -9.5..-3.5 (6 long): a top rim 1 tall (y 1..2, z 4..6) and a vertical inner-rib / outer-rib pair. The rib at x -9.5..-8.5 spans y 2..9 and is 2 deep. Between the pack side and that rib is the sail: 5 wide (x -8.5..-3.5), 7 tall (y 2..9), 1 deep (z 4.5..5.5). So the inner panel reads as a gray-framed cream square with the frame's top and outer edges only (no bottom edge, no pack-side edge).
- **Outer panel:** frame x -16.5..-9.5: a top rim at y 2..3 (6 long, 1 lower than the inner rim), an outer rib at x -16.5..-15.5 (y 3..9, 2 deep), and a sail x -15.5..-9.5 (6 wide, y 3..9 = 6 tall, 1 deep). It starts at the inner panel's outer rib.
- Overall the wing's top edge steps down 1 at the panel joint, and the bottom edge is straight at y 9. The wings meet the pack at x +-3.5, touching its side faces. Their rear faces (z 6) are flush with the pack's back face.
- The sails are slightly translucent-looking pale cream-green, not see-through.

### What it is NOT
- Not curved, not feathered; flat rectangular boards. The wings do not tilt or fan out at an angle.

## 4. Silhouette checks

- **Front or back (deployed):** a wide horizontal band: brown pack in the center (10 tall), pale panels on both sides with gray frames, the outer panels 1 shorter at the top. Total 33 wide against the pack's 7.
- **Front or back (folded):** a plain brown rectangle 7 x 10.
- **Side:** brown 4-deep box; when deployed, a thin 2-deep gray line at the back.
- **Top:** a thin horizontal line 33 long and 2 thick with the brown box (7 x 4) at the center; the outer sections are 1 narrower.
- **Worn standing:** folded pack covers the upper back and sits 2 proud of the chestplate's back wall.

## 5. Materials and palette

| Part | Material | Base | Highlight | Shadow |
|---|---|---|---|---|
| Pack | brown canvas | #584838 | #685838 | #382828 |
| Pack side strip | gray panel | #787878 | #989898 | #585858 |
| Wing frame | light steel | #787878 | #989898 | #585858 |
| Wing sail | cream-green fabric | #C8C8A0 | #D8D8B0 | #A0A080 |

One skin; no camo variants. In the original the texture is shared: the wings ignore the camo logic (there is none).

## 6. In-game presentation

- **Slot:** back (`wings`). The original draws the **folded pack only if the slot's "show" toggle is on**, but draws the **wings regardless of the toggle** whenever the wearer is **in the air and sneaking** (the same condition as the glide physics).
- **Attachment:** body part, scale 1/16, no offset, no inflation. Coordinates are body-frame.
- **Over a chestplate:** the pack starts at z 2, so its first unit is inside the chestplate's back wall (z up to 3); 3 units stay proud. The wings are at z 4..6, so they hover clear behind the chestplate and the arms (arms z <= 2). The wing's span (+-16.5) passes far outside the arms.
- **Our current state:** no worn model; invisible when worn.

## 7. Motion

- **Folded to deployed** is a pop-in: there is no unfolding animation, just a swap. While deployed the folded pack is still drawn too.
- All parts follow the body's forward lean when sneaking (about 29 degrees). In the original each wing part leans about its own pivot rather than the body's, so while the player sneaks in the air the wings shear slightly out of line; a single rigid wing group would be cleaner.
- While gliding the wearer hears a looping wind sound. No flame, glow or particles on the model.

## 8. What our earlier attempts got wrong

No worn model exists. Traps: drawing the wings always; making them curved or angled; forgetting the 1-unit step on the outer panels; making the sails fully framed (the frame is open at the bottom and at the pack side); mixing up the pack's brown with the wing's pale cream.

## 9. Acceptance checklist

- [ ] Folded pack 7x10x4, touching the torso back, brown.
- [ ] Wings span 33 total (x +-16.5).
- [ ] Two panels per side; the outer panel's top is 1 lower than the inner one.
- [ ] Frames 2 deep (z 4..6), sails 1 deep (z 4.5..5.5), flush with the pack's back.
- [ ] Sails pale cream-green, frames gray.
- [ ] Wings are flat and unrotated.
- [ ] Wings shown only when airborne and sneaking, whatever the slot toggle says.
- [ ] Pack hidden when the slot toggle is off.
- [ ] Wings symmetric.
- [ ] Both states follow body lean.
- [ ] Every wing part touches its neighbor.
- [ ] Wing bottom edge at y 9.

## 10. Unsure

- **Frame edges** (which sides of each panel have a rim): read from numbers, with the render consistent. The pack-side edge of the inner panel is open.
- **Pack side gray strip**: the render shows a lighter vertical area inset on its sides; its exact size is my estimate.
- **Sneak shearing** is derived from the model code, not seen in game.
- **Deploy visibility for other players** depends on the original's sneak check, which for remote players may not match; I did not verify it.
