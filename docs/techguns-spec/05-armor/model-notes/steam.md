# Steam armor: clean-room model notes

Design notes for rebuilding the Techguns **Steam Armor** look-alike (GregGuns set `steam`, camo variants `hazard`, `iron`, `rusty`). Written by the "dirty room" analyst from study of the original's model class, its slot logic, its four textures and the render pair. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [armor catalog](../armor-catalog.md), [GregGuns armor spec](../../../gregguns-spec/05-armor/index.md), [implementation notes ARM-DEV-13](../../../gregguns-spec/05-armor/implementation-notes.md).

**Units and axes used in this document.** One unit = one texel of a 64x32 skin = 1/16 block. Minecraft model convention: **Y points down**, the player **faces -Z**, the player's **right side is -X**. Every part is given **relative to the vanilla biped part it hangs on, using that part's rotation point as origin**:

| Part | Rotation point | Vanilla box (min to max) |
|---|---|---|
| head | (0, 0, 0) (the neck) | x -4..4, y -8..0, z -4..4 |
| body | (0, 0, 0) | x -4..4, y 0..12, z -2..2 |
| right arm | (-5, 2, 0) | x -3..1, y -2..10, z -2..2 |
| left arm | (5, 2, 0) | x -1..3, y -2..10, z -2..2 |
| right leg | (-1.9, 12, 0) | x -2..2, y 0..12, z -2..2 |
| left leg | (1.9, 12, 0) | x -2..2, y 0..12, z -2..2 |

"Local" below means this frame. Arms and legs are described for the **right** side; the left side is an exact mirror (negate x). The original applies **no extra scale and no armor-inflation parameter**: the boxes are simply built about 0.5 unit larger than the vanilla ones on each side, and the numbers below already include that. Sizes are width (X) by height (Y) by depth (Z). Rounded to half a unit unless it matters.

## 1. Gestalt

A bulky, **cream-tan steampunk exosuit** trimmed with **bright lemon-yellow brass** bands, with **gray smokestack chimneys angled out of the shoulders** and a **twin-tank pack with blue pressure windows on the back**. A glance reads: a box-headed helmet with a dark visor slit and two vertical pipes behind it; wide square shoulders carrying a tilted gray chimney each; yellow rings around the shoulders, elbows, belt, knees and boots; a pale beige torso with raised chest plates; and a back wall carrying two squat tanks. The player looks broader and much thicker than vanilla, and boxy, like a diving suit.

Defining features in order of visual importance:

1. **Yellow trim over tan armor.** Two main colors, tan (plates) and yellow (rings, rails, boots, pelvis), with gray metal accents. Yellow is the loudest thing from every view.
2. **The shoulders:** wide yellow pad rings, each with a **gray chimney tilted 30 degrees outward**, plus a short crossbar under the chimney.
3. **The helmet:** a cube head 9 across, with a small snout block and jaw strip on the face and two clamped vertical pipes on the back.
4. **The back pack:** an 8-wide flat plate behind the torso with two small tanks, each with a blue gauge window, banded by horizontal bars.
5. **Outer rails and lamp blocks:** a thin yellow strip down the outside of each arm and leg, with a gray block bulging out at the elbow and the knee.
6. **Boots and pelvis:** a yellow sole band and toe plate; a thick yellow pelvis ring and a crotch plate on the leggings.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Overall width, chestplate worn (outer elbow block to outer elbow block) | about 20 (vanilla 16) |
| Width across the shoulder rings | about 19 |
| Torso shell | 9 wide x 12 tall x 5 deep (vanilla 8 x 12 x 4) |
| Depth of torso plus back pack | from 3.5 in front of the body mid-plane to 6.5 behind it, about 10 (vanilla 4) |
| Helmet cube | 9 x 9 x 9 (vanilla 8 x 8 x 8) |
| Helmet front to back, with snout and pipes | from 6 in front of the neck to 6.5 behind it |
| Helmet width with ear plates | 11 |
| Chimney tip | about 4 above the shoulder line, about halfway up the head |
| Arm shell | 5 wide x 13 tall x 5 deep (vanilla 4 x 12 x 4) |
| Leg | upper sleeve 5 x 9 x 5, boot sleeve 5 x 4 x 5 |
| Height | the helmet cube is half a unit taller than vanilla; nothing else adds height |

The suit is **thicker front to back than a normal player** (back pack and pipes) and wider at the shoulders and elbows. The front silhouette is a wide "T" (shoulders and chimneys) narrowing to legs that are only a little wider than vanilla, plus the 1-unit outer rails.

## 3. Part by part

### Helmet (helmet piece, head frame)

- **Skull cube.** A 9 x 9 x 9 cube centered on the head: x -4.5..4.5, y -8.5..0.5, z -4.5..4.5. A plain smooth block and the biggest single face on the model. The dark **visor slit is painted** on the front face (section 5), not cut into the geometry. It is NOT a dome, hat or rounded shape; it is a cube like a diving helmet.
- **Snout block.** 4 x 4 x 2, x -2..2, y -3..1, z -6..-4. It sticks 1.5 units out of the skull's front, centered, in the lower half of the face. From the side it is a short nub in front of the face.
- **Jaw strip.** 7 x 2 x 1, x -3.5..3.5, y -1.5..0.5, z -5.5..-4.5, sticking 1 out of the front. It sits across the bottom of the face and overlaps the snout's lower edge, so the two read as a stepped chin guard. It is NOT a band around the whole head.
- **Ear plates.** Two identical flat plates, 1 x 6 x 6, one each side of the skull: x -5.5..-4.5 and 4.5..5.5, y -7..-1, z -3..3. They touch the skull sides and widen the head to 11. Plain flat slabs, no lamp, no angle.
- **Back pipes.** Two vertical square pipes 1 x 6 x 1 behind the skull, centered at x -2.5 and 2.5, y -6.5..-0.5, z 5.5..6.5. They stand 1 behind the skull's back face and are tied to it by **four clamp blocks**, each 2 x 2 x 1 at z 4.5..5.5, two per pipe (top at y -7..-5, bottom at y -2..0), centered on the pipe in x (x -3.5..-1.5 and 1.5..3.5). From behind, the helmet reads as a cube with two pipes strapped on, like twin exhaust risers.
- Not present: no antenna, crest, goggle geometry or hose to the chest.

### Chestplate, body (body frame)

- **Torso shell.** 9 x 12 x 5, x -4.5..4.5, y -0.5..11.5, z -2.5..2.5. It covers the vanilla body and sticks out 0.5 on every side.
- **Chest plate.** A 7 x 4 x 1 slab on the front, x -3.5..3.5, y 1.5..5.5, z -3.5..-2.5 (1 proud of the shell), centered, over the upper chest.
- **Abdomen plate.** A 5 x 4 x 1 slab on the front, x -2.5..2.5, y 6.5..10.5, z -3.5..-2.5. A 1-unit gap (y 5.5..6.5) separates it from the chest plate, reading as a waist seam. It is narrower than the chest plate by 1 each side.
- **Back plate.** An 8 x 10 x 2 slab, x -4..4, y 0.5..10.5, z 2.5..4.5, directly behind the shell. A flat wall on which all the back hardware mounts.
- **Two tanks**, mirrored about the mid-plane, each built from stacked pieces in z 4..6.5:
  - Casing: 3 x 4 x 2, x -3.5..-0.5 and 0.5..3.5, y 3.5..7.5, z 4.5..6.5.
  - Core: 2 x 3 x 2, y 4.5..7.5, z 4..6, centered in each casing (x -3..-1 and 1..3), mostly hidden inside the casing. This is where the **blue gauge window** comes from.
  - Cap on top: 2 x 1 x 2, y 1.5..2.5, z 4..6, centered on each tank. Foot at the bottom: the same piece at y 8.5..9.5.
  - Result: a 3-wide, about 8-tall (y 1.5..9.5) capsule with narrower 2-wide caps, standing out to 6.5 behind the mid-plane.
- **Rungs.** Four bars, each 3 x 1 x 2, one across each tank at y 2.5..3.5 and one at y 7.5..8.5, flush with the casings (x -3.5..-0.5 and 0.5..3.5, z 4.5..6.5). They read as banding on the tanks, not as bars between them.
- Between the tanks is a 1-unit gap (x -0.5..0.5) showing the back plate.

### Chestplate, arms (right arm frame; left is a mirror)

- **Arm shell.** 5 x 13 x 5, x -3.5..1.5, y -2.5..10.5, z -2.5..2.5. It covers the vanilla arm, sticks out 0.5 around it, and is shifted 0.5 outward in x, overlapping the torso shell by about 1.
- **Shoulder ring.** A 6 x 2 x 6 slab, x -4.5..1.5, y -1.5..0.5, z -3..3, wrapping the top of the arm. The widest point of the shoulder (reaching 4.5 outward from the arm's center line of the shell).
- **Shoulder crossbar.** A 2 x 2 x 6 bar on top of the shoulder, x -1.5..0.5, y -3.5..-1.5, z -3..3, running front to back and standing 2 above the ring's top. It sits under the chimney.
- **Chimney.** Two stacked parts hinged at the local point (-2, -1.5, 0), both **rotated 30 degrees about the front-to-back (Z) axis so the top tips outward**: a base block 3 x 2 x 3 (occupying the 2 units directly above the pivot, centered on the axis) and a tube 2 x 2 x 2 on top of it (the next 2 units up). Height along its own axis is about 4 from the pivot; after tilting, the top center is about 2 outward and 3.5 above the pivot. A gray smokestack leaning away from the head, with the **top face painted dark** as the mouth. The right chimney leans toward -X, the left toward +X; they never lean forward or back.
- **Elbow band.** A 6 x 2 x 6 slab, x -4..2, y 4.5..6.5, z -3..3 (same footprint as the shoulder ring).
- **Outer rail.** A 2 x 9 x 2 yellow strip on the outside of the arm, x -4.5..-2.5, y 0.5..9.5, z -1..1. It is centered front to back and sticks out about 1 beyond the shell, so it shows as a stripe, not a wall.
- **Elbow block.** A 2 x 3 x 3 gray block on the outside at the elbow, x -5..-3, y 4..7, z -1.5..1.5. The outermost point of the arm (10 from the body's mid-plane); it reads as a lamp or valve.
- No hand covering: the arm shell simply ends at y 10.5.

### Leggings (body frame plus leg frame)

- **Pelvis ring.** 10 x 2 x 6, x -5..5, y 11..13, z -3..3, the widest part of the lower body.
- **Pelvis skirt.** 9 x 3 x 5, x -4.5..4.5, y 11.5..14.5, z -2.5..2.5. It sits inside the ring and hangs 1.5 below it: a stepped hip, the narrower skirt showing under the broader ring.
- **Crotch plate.** A 2 x 2 x 6 bar, x -1..1, y 13..15, z -3..3, centered under the skirt and running front to back, between the legs.
- **Leg sleeve (per leg).** 5 x 9 x 5, x -2.5..2.5 about the leg's center, y -0.5..8.5, z -2.5..2.5. It ends at y 8.5, where the boots begin; the leggings never cover the lowest 3.5 units of the leg.
- **Knee band.** A 5 x 2 x 6 slab, right leg x -3..2, y 5..7, z -3..3, yellow.
- **Knee block.** A 2 x 3 x 3 gray block on the outside, right leg x -4..-2, y 4.5..7.5, z -1.5..1.5. The outermost point of the leg; matches the elbow block.
- **Outer rail.** A 2 x 10 x 2 yellow strip, right leg x -3.5..-1.5, y 0.5..10.5, z -1..1. It runs from the thigh top past the knee, ending 2 below the sleeve's end (so it shows over the boot top). Sticks out about 1 beyond the sleeve.

### Boots (leg frame)

- **Boot sleeve.** 5 x 4 x 5, x -2.5..2.5, y 8.5..12.5, z -2.5..2.5. It starts exactly where the leggings sleeve ends and sticks 0.5 below the vanilla leg bottom.
- **Sole band.** A 6 x 2 x 6 slab, x -3..3, y 10..12, z -3..3, around the lower two units of the boot. The widest part of the boot, yellow.
- **Toe plate.** A 3 x 3 x 1 plate on the front, x -1.5..1.5, y 9.5..12.5, z -3.5..-2.5, centered, 0.5 proud of the sole band. Reads as a toe cap.
- The boots stop at the ankle; they are short, nearly cubic and do NOT have a long foot.

## 4. Silhouette checks

- **Front:** a wide symmetrical "T". Gray chimneys tilt out like horns from the shoulders, about as high as the middle of the helmet. The helmet cube is flanked by the 11-wide ear plates and shows the visor slit, the snout and the jaw strip. Wide yellow shoulder rings, with outer rails and gray elbow blocks hanging below. A beige torso with two raised plates (a wide upper, a narrower lower) and a seam. The yellow pelvis ring is the hip. Legs are a little wider than vanilla, with yellow knee bands, gray knee blocks at the outside, and short yellow-banded boots.
- **Back:** the helmet's two pipes rise side by side with clamp brackets; below, an 8-wide flat back plate with two squat tanks side by side, each with a blue window in the middle and horizontal bands. The shoulders show the chimneys' flat sides. The pelvis and legs are as in front, without plates.
- **Side:** very deep (about 10). In front, the 1-unit chest and abdomen plates. Behind, the tanks reaching 6.5, and up at the head the pipe 6.5 behind the neck. The head is a large box with a nub in front (snout and jaw), an ear plate in the middle and the pipe behind. The chimney is a short tilted stack seen end-on (it leans only sideways). Elbow and knee blocks appear as small gray squares.
- **Top:** the 9 x 9 helmet with the ear plates sticking out 1 each side; at each side a yellow shoulder ring and a gray chimney with a **dark circular mouth** inside a lighter ring, with the crossbar visible; the pipe tops peeking out behind the helmet.
- **Worn look on a standing player:** a stocky armored giant. A big box helmet, wide shoulders with horn-like chimneys, a barrel torso, tanks behind, yellow-ringed legs, spat-like boots. Tan dominates the middle; yellow dominates the limbs and pelvis.

## 5. Materials and palette

The original texture is a **256 x 128 sheet drawn at twice the 128 x 64 layout the model uses**. The material reads as **painted tan plates with polished brass trim**: smooth, lightly noisy (a few levels per channel), with a few darker rivet and seam marks. No gradients, no wear in the base variant.

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Helmet cube, ear plates | tan plate | #B09168 | #BA9F75 | #A5855C |
| Arm and leg shells | tan plate | #B59A6E | #D2B48B | #594935 (dark seams) |
| Torso shell and chest plates | paler tan, soft vertical shading like sculpted muscle | #CDB38F | #D2B48B | #594935 |
| Back plate | darker tan | #A5855C | #B09168 | #7B5E45 |
| Shoulder and elbow rings, knee and sole bands, rails, belt, pelvis ring, toe plate | lemon-yellow brass | #E8E35A | #FFFF7B | #BCB848 (guess) |
| Chimneys, elbow and knee blocks, pipes, clamps, tank casings | light gray metal | #CBC7C4 | #FFFFFF (specular dots) | #9E9891 |
| Chimney mouth, visor slit | dark | #1F1F1F | | |
| Blue gauge windows | blue glass | #5E66FF | #FFFFFF (glint) | #5058FF |

**Camo variants** (texture-only recolors on the same model; the blue windows are identical in all four):

| Variant | Plates (the tan areas) | Trim (the yellow areas) | Chimneys, gray blocks, pipes |
|---|---|---|---|
| `steam` (base) | tan #B09168, torso #CDB38F | yellow #E8E35A | gray #CBC7C4 |
| `rusty` | warm gold-brown #C08B3E, highlights #DDB55E, orange rust patches #A34711 and #612502 | dark rust #6E2E0A, shadows #3F1603 and #300F02 | weathered gray #9E9891, shadow #807A75 |
| `iron` | cool steel gray #7D7D7D (range #767679 to #84858A) | very dark iron #272422, highlights #4E4A41 | dark gunmetal #403C38, shadow #322E2C |
| `hazard` | near-black #0B0B0B with yellow bars #C3C000 and #CECB00 | yellow #A5A600 to #E5E22D striping over black #101010 | **red** #DA201B, shadow #72110E |

`hazard` reads as yellow-and-black caution striping with red chimneys and blocks; `iron` as dark and sooty steel; `rusty` as orange-brown, aged brass.

## 6. In-game presentation

- **Slots.** One part set serves four items. The **helmet** draws the head parts (skull, snout, jaw, ear plates, pipes, clamps). The **chestplate** draws the torso parts (shell, plates, back plate, tanks, rungs) and both arms (with chimneys). The **leggings** draw the pelvis parts (ring, skirt, crotch plate) and both leg sleeves with their knee bands, knee blocks and rails. The **boots** draw only the boot sleeve, sole band and toe plate on each leg. Wear only the chestplate and you see no pelvis; wear only the leggings and you see no torso shell.
- **Accessory hides.** In Techguns the chestplate hides the back accessory slot (its own tanks fill that space) and the helmet hides the face accessory slot (the snout and jaw occupy it). Keep these rules.
- **No scale.** All boxes are at 1/16 block, with no sneak or pose offset beyond the biped's.
- **Texture.** One sheet per variant, used by all four pieces; the leggings have no separate layer.
- **Item icon.** The original draws each piece in the inventory as the 3D piece itself (front view, filling the slot).

## 7. Motion

- The model has **no animated parts of its own**. Every part is attached to a vanilla biped part and follows it.
- **Head parts** turn and nod with the head; the back pipes swing behind it as the player looks up and down.
- **Torso parts** follow the body, including the forward lean when sneaking.
- **Arm parts** swing with the arms (walk swing, the raise when holding a gun or bow). The chimney is rigid on the arm, so its 30 degree tilt is relative to the arm and it swings with it.
- **Leg parts** swing with the legs. The pelvis parts are on the body and do not swing, so the legs pivot under the skirt; the skirt's lower 3 units hang over the leg tops.
- No chimney particles or other effects are drawn by the model. Steam leggings play a hissing jump sound only.

## 8. What our earlier attempts got wrong

Our current model (ARM-DEV-13) is a plain-box imitation textured as brown planks with orange and black.

- **Wood, not tan-and-yellow.** Our overall color is brown planks with dark stripes. The original is cream-tan plates with **bright lemon-yellow** trim and gray metal, and none of our variants (green-gray, yellow-black, light gray, rust) matches the original's four palettes.
- **No real chimneys.** Our shoulder has a short vertical tube. The original has **30 degree outward-leaning gray chimneys** with a 3 x 2 x 3 base and a 2 x 2 x 2 tube, plus a crossbar under each; they give the figure its horned look.
- **Helmet too small and plain.** Ours is about 8 wide and 7 tall with a separate glass visor box and one thin yellow ring. The original is a 9 x 9 x 9 cube with a snout, a jaw strip, 6 x 6 ear plates and four clamps. Our pipes are loose boxes; the original's are clamped to the skull.
- **Torso lacks plates and a back plate.** Ours has a gauge box and a belt, and two small tanks 2.5 behind the body. The original has a 7-wide chest plate and a 5-wide abdomen plate with a seam, an 8 x 10 back plate, and capsule tanks with caps, bands and blue windows.
- **Arms lack rails and lamp blocks.** The original has 9-tall yellow outer rails and gray elbow blocks that put the arms 10 from the center; ours has none, so our figure is far narrower.
- **Legs lack the pelvis structure, rails and knee blocks.** Ours has a single hip band; the original has a 10 x 2 x 6 ring, a skirt, a crotch plate and 10-tall outer rails.
- **Boots wrong.** Ours has a long foot sticking forward. The original boot is nearly cubic: a 6 x 2 x 6 sole band (0.5 deeper than the sleeve) plus a 3 x 3 x 1 toe plate.
- **Everything too thin.** The original's shells are 5 wide and deep (limbs) and 9 x 12 x 5 (torso), with parts reaching 1 to 1.5 further out; ours are mostly vanilla size plus 0.5.
- **Do not add** hoses, a neck gap, glowing parts, a forward-leaning chimney or a long boot foot.

## 9. Acceptance checklist

- [ ] The color story reads as tan plates, lemon-yellow trim and gray metal, with blue windows on the back.
- [ ] The helmet is a 9 x 9 x 9 cube with a painted dark visor slit, a 4 x 4 snout nub (1.5 out), a 7-wide jaw strip (1 out), and two 6 x 6 ear plates 1 out on each side.
- [ ] Two vertical 1 x 6 x 1 pipes stand 1 behind the helmet, each with a clamp at the top and bottom.
- [ ] Each shoulder has a yellow 6 x 2 x 6 ring, a 2 x 2 x 6 crossbar and a gray chimney (3 x 2 x 3 base, 2 x 2 x 2 tube) leaning **30 degrees outward**, with a dark mouth.
- [ ] The chimney tip is about 4 above the shoulder line and about 2 out from its pivot.
- [ ] The torso is 9 x 12 x 5, with a 7 x 4 chest plate and a 5 x 4 abdomen plate (both 1 proud) and a 1-unit seam between them.
- [ ] The back plate is 8 x 10 x 2 and carries two tanks, each 3 wide and about 8 tall with narrower caps, with four 3 x 1 bands and a blue window; the pack reaches about 6.5 behind the mid-plane.
- [ ] Arms are 5 x 13 x 5, with a 6 x 2 x 6 elbow band, a 2 x 9 x 2 yellow outer rail and a 2 x 3 x 3 gray elbow block.
- [ ] Width across the elbow blocks is about 20 (10 each side).
- [ ] The pelvis is a 10 x 2 x 6 ring plus a 9 x 3 x 5 skirt, with a 2 x 2 x 6 crotch plate below.
- [ ] Leg sleeves are 5 x 9 x 5, with a 5 x 2 x 6 knee band, a 2 x 3 x 3 gray knee block and a 2 x 10 x 2 outer rail.
- [ ] Boots are a 5 x 4 x 5 sleeve, a 6 x 2 x 6 sole band and a 3 x 3 x 1 toe plate, with no long foot.
- [ ] Slot split is right: helmet draws only head parts, chestplate the torso and arms, leggings the pelvis and legs, boots only the boot parts.
- [ ] Left and right are exact mirrors; every part touches or overlaps a neighbor.
- [ ] The four sheets match section 5 (red chimneys on `hazard`, dark gunmetal on `iron`, orange-brown on `rusty`).

## 10. Unsure

- **Visor and snout painting.** I could not settle whether the dark slit is on the skull above the snout or on the snout. The front render reads as a slit in the upper face with a small squarish insert (gray ring, dark center) below it. Treat the exact painting as free; the geometry is certain.
- **Where the blue window sits.** In the original it shows on the back of each tank. By the numbers the core is partly inside the casing, so the blue may be on the casing's back face or the core. Paint a small blue window with a white glint on each tank's back face.
- **Torso shading.** The front render shows soft modeled shading (a pale, ribbed look) on the torso; its pattern is not recoverable at this resolution.
- **Yellow shadow tone** (#BCB848) is a guess; sampled values are dominated by bright pixels.
- **Rivets and seams** are tiny dark marks on the tan plates and optional.
- **Hazard stripe pattern** (bar width and angle) was not recorded; any bold yellow-and-black caution striping is acceptable.
