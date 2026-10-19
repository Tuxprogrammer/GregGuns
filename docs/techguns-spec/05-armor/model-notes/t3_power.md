# T3 Power Armor: clean-room model notes

Design notes for rebuilding the Techguns **T3 Power Armor** look-alike in GregGuns (`t3_power`, with camo recolors `dark` and `dark2`). Written by the "dirty room" analyst from study of the original's current model class (not the retired "Old" one), its item and renderer wiring and its three textures. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [armor spec](../index.md), [GregGuns implementation notes (PWR-DEV-05, ARM-DEV-24)](../../../gregguns-spec/05-armor/implementation-notes.md), [revolver notes (template)](../../03-gun-visuals-audio/model-notes/revolver.md).

**Units and frame used in this document.** One unit = one texel of a 64x32 skin = 1/16 block. Minecraft model convention: **Y points down**, the player **faces -Z**, the player's **right side is -X** (so a "right" part has negative X). Every part is given **relative to the vanilla biped part it is attached to**, using that part's rotation point as origin:

| Part | Rotation point | Vanilla box (min to max) |
|---|---|---|
| head | (0, 0, 0) | x -4..4, y -8..0, z -4..4 |
| body | (0, 0, 0) | x -4..4, y 0..12, z -2..2 |
| right arm | (-5, 2, 0) | x -3..1, y -2..10, z -2..2 |
| left arm | (5, 2, 0) | x -1..3, y -2..10, z -2..2 |
| right leg | (-1.9, 12, 0) | x -2..2, y 0..12, z -2..2 |
| left leg | (1.9, 12, 0) | x -2..2, y 0..12, z -2..2 |

"Arm-local" and "leg-local" numbers below are relative to that arm's or leg's rotation point, so the vanilla box above is the yardstick. The original authors its arm and leg parts in absolute coordinates and converts with offsets; I have resolved those conversions for you. Everything is scaled 1.0 (no armor-layer inflation, no extra scale); the model is simply authored with its own half-unit oversize. Rounded to half a unit unless it matters. A positive rotation about a horizontal axis follows the usual Minecraft convention (positive X rotation swings a part's lower end toward +Z, the back).

## 1. Gestalt

A **bulky, light-gray, bolted-plate suit of heavy powered armor**, a sci-fi "Paladin" exo-frame. At a glance you read: a **big boxy helmet dome with a dark visor slit and a protruding respirator block**, ringed by a **chunky collar that carries black-and-olive hazard stripes**; a **very wide pair of sloping shoulder pauldrons** that sweep out and down like wings (about 25 units across, over 1.5 times a normal player); a **barrel chest with a thick front block**; thick sleeves; **two small tanks and a pipe bridge on the back**; a belted hip block; stout legs with **diamond knee pads and bowed side struts**; chunky boots with a toe cap. The feel is "armored tank in human proportions": everything is fatter than the skin it covers by half a unit per side, light gray with black panels, and a single dull accent family (hazard green).

Defining features in order of visual importance:

1. **The wing-like, 30 degree sloped pauldrons**, two stacked plates per shoulder, reaching about 7 units beyond the arm. They make the front and back silhouettes unmistakable.
2. **Light gray plate with black recessed panels**, accented by **olive-green and black hazard stripes on the neck collar** (the main color accent).
3. **A fat helmet dome** with the collar ring, a protruding respirator block on the front and a diamond-section lamp on the right side of the head.
4. **A deep chest block** that sticks out 2.5 units from the torso, with a slanted abdomen plate under it, and a twin-tank backpack.
5. **Sleeves, legs and boots 5 wide**: every limb is inflated by half a unit per side.
6. **Diamond knee pads and bowed side struts** on the legs.

## 2. Overall proportions

Worn on a standing player (absolute player coordinates: X 0 is the spine, Y 0 is the neck line, the ground is at Y 24).

| Measure | Value |
|---|---|
| Width, shoulder tip to shoulder tip | about 24.5 (X from about -12.5 to +12.5) |
| Torso core width | 9 (vanilla 8) |
| Hip belt width | 10 |
| One sleeve width | 5 (vanilla 4) |
| Helmet dome | 9 x 9 x 9, from y -8.5 to +0.5 |
| Depth, chest block front to tank backs | about 9 (z -5 to +4) |
| Helmet depth incl. respirator and collar bump | about 11.5 (z -6 to +5.5) |
| Height, helmet top to boot soles | about 33 (y -8.5 to +24.5; vanilla 32) |
| Width : depth : height | about 24.5 : 11.5 : 33 |

Against vanilla: the dome is 1 unit wider, taller and deeper than the 8-unit head and sits 0.5 high; the torso core is 1 wider and 1 deeper and spans y -0.5 to 11.5; sleeves, leg sleeves and boots are each 1 unit wider and deeper than the vanilla limb.

## 3. Part by part

All parts are boxes (some turned). Mirrored parts are symmetric left and right unless noted. "Touches" means faces in contact or overlapping.

### Helmet (head part, drawn by the helmet only)

- **Dome.** A 9 x 9 x 9 cube centered on the head: x -4.5..4.5, y -8.5..0.5, z -4.5..4.5. Smooth, no taper, no bevels. On the front face, a thin **dark horizontal visor slit** (painted, about 7 wide and 1.5 tall, around y -5.5..-4). It is painted, not recessed.
- **Respirator block.** 4 wide x 4 tall x 3 deep, centered on the face: x -2..2, y -3..1, z -6..-3. It sticks 1.5 out of the dome's front and hangs 0.5 below the dome's bottom. Dark gray with a grille hint; it is the only thing in front of the face.
- **Collar ring.** A loop of low bars (2 tall, y -2..0) around the dome's lower end, 10 wide overall:
  - Two front bars flanking the respirator, each 3 long x 2 tall x 2 deep: x -5..-2 and 2..5, z -5.5..-3.5 (so the ring's front is 1 unit proud of the dome, and the bars touch the respirator's sides).
  - Two side bars, each 1 thick x 2 tall x 8 long: x -5..-4 and 4..5, z -3.5..4.5 (0.5 proud of the dome's sides).
  - A back bar 9 wide x 2 tall x 1 deep: x -4.5..4.5, z 4..5, with a small **back bump** on its middle, 4 wide x 3 tall x 1 deep, z 4.5..5.5, y -2.5..0.5.
  - The ring is **hazard striped** (section 5). It overlaps the dome's bottom 2 units, so it reads as a collar bolted around the neck, not a hovering ring.
- **Side lamp.** On the **player's right** only (x negative): a 2 x 2 x 4 bar turned **45 degrees about its long, front-to-back axis**, so from the front it is a diamond about 2.8 tall and wide. Centered at about x -5.5, y -6, spanning z -3 to +1. It sticks about 2.5 out of the dome's right face, upper-middle, front half. The left side has **no** lamp.
- **Front stud.** A small 2 x 2 x 1 block at x 1..3, y -8..-6, z -5..-4: on the dome's front, high up, **left of center from the player's view** (positive x). It protrudes 0.5. Plain block.
- The helmet is NOT: horned, antennaed, glass-visored in geometry, ear-podded, or fitted with a symmetric lamp pair.

### Chestplate: torso (body part)

- **Core.** 9 wide x 12 tall x 5 deep: x -4.5..4.5, y -0.5..11.5, z -2.5..2.5. A plain box.
- **Chest block.** 8 wide x 5 tall x 3 deep: x -4..4, y 2..7, z -5..-2. It sticks 2.5 out of the core's front. Heavy and flat-fronted; its face carries two big dark inset panels (section 5).
- **Abdomen plate.** 7 wide x 5 tall x 2 deep, hanging below the chest block, **tilted about 21 degrees about X** so its top edge is forward (z about -4.5..-2.5, hinged at y 7) and its lower end tucks back toward the core (z about -2.5..-0.5 at the bottom). It spans about y 7..11.5 and x -3.5..3.5. From the side it is a slanted skirt under the chest block.
- **Back tanks.** Two upright 3 wide x 5 tall x 2 deep boxes: x -3.5..-0.5 and 0.5..3.5, y 5..10, z 2..4. They touch the core's back (z 2.5) and stick out 1.5 beyond it, with a 1-unit gap between.
- **Pipe bridge.** Above the tanks, an upside-down U of 1 x 1 section pieces at z 2.5..3.5: a horizontal bar 5 wide (x -2.5..2.5) at y 2..3, with two 2-tall legs below it (x -2.5..-1.5 and 1.5..2.5, y 3..5) landing on the tanks' tops (y 5).
- **Back diamond.** A 2 x 2 x 1 block turned 45 degrees about Z (a rhombus about 2.8 across) centered x 0, y 2.5, z 3..4: sitting on the bridge's bar, a half unit proud of it.
- The torso is NOT: yoked, belted (the belt is the leggings' job), or fitted with a front pack.

### Chestplate: arms (mirror-symmetric; right arm described)

Right arm, arm-local (origin at the shoulder pivot):

- **Sleeve.** 5 wide x 13 tall x 5 deep: x -3.5..1.5, y -2.5..10.5, z -2.5..2.5. The vanilla arm grown half a unit per side. Two dark painted **horizontal bands** circle it in its upper half; a few tiny round rivets near the bottom.
- **Shoulder cap.** 3 x 3 x 6 block on the shoulder top: x -3..0, y -2..1, z -3..3. Mostly buried in the sleeve and under the pauldron; only 0.5 of it shows front and back.
- **Lower pauldron plate.** A 7 long x 2 thick x 8 deep slab **hinged at its inner top edge and tilted 30 degrees so the outer end is down**. Inner top edge at about x 0, y -4, spanning z -4..4; it runs outward (toward -X on the right arm) and downward at 30 degrees. The outer tip ends about x -7, with its top outer corner at about y 0 and bottom outer corner at about y 1.5. In player coordinates the inner end rises to about y -1.5 (1.5 above the shoulder line) and the outer tip hangs at y 1.5..3.5, about 12 units out from the spine.
- **Upper pauldron plate.** 7 long x 1 thick x 6 deep, lying directly on the lower plate at the same 30 degree slope (hinge about y -5), spanning z -3..3. It is half a unit longer, so its outer tip reaches about x -7.5 arm-local (about 12.5 absolute). The pair reads as a **stepped two-layer wing**: a wide thin bottom layer and a narrower, slightly longer top layer.
- **Outer side plate.** A 1 x 5 x 4 plate on the outer face of the forearm: x -4.5..-3.5, y 4.5..9.5, z -2..2, hugging the sleeve.
- **Back plate.** 3 wide x 4 tall x 1 deep on the back of the upper arm: x -3..0, y 2..6, z 2..3, against the sleeve's back (z 2.5), sticking out 0.5.
- The **left arm** is the mirror: pauldron slopes down toward +X, outer side plate at x 3.5..4.5, back plate at x 0..3. **Exception:** the left sleeve is shifted 0.5 outward (x -1..4 instead of -1.5..3.5). Mirroring the right sleeve is an acceptable deviation (section 10).
- The arms are NOT: gauntleted (the sleeve ends at y 10.5, with no hands or fists), cannon-armed, or vented.

### Leggings: hips (body part)

- **Pelvis block.** 9 wide x 3 tall x 5 deep: x -4.5..4.5, y 11.5..14.5, z -2.5..2.5, directly under the torso core.
- **Belt.** 10 wide x 2 tall x 6 deep: x -5..5, y 11..13, z -3..3. It wraps the pelvis block's top and the torso's bottom, 0.5 proud all round.
- **Crotch plate.** 3 wide x 2 tall x 6 deep: x -1.5..1.5, y 13..15, z -3..3, hanging from the belt to y 15. The lowest hip feature.

### Leggings: legs (mirror-symmetric; right leg described)

Right leg, leg-local (origin at the hip pivot):

- **Thigh sleeve.** 5 wide x 9 tall x 5 deep: x -2.5..2.5 (shifted about 0.1 inward, ignorable), y -0.5..8.5, z -2.5..2.5. It stops at y 8.5 where the boot starts.
- **Knee pad.** A 3 x 3 x 1 block turned **45 degrees about Z**: a diamond about 4.2 across and 4.2 tall, 1 thick, centered about x -0.4 (a hair outward), y 4.6, z -3..-2 (sticking 0.5 out of the sleeve's front). Points aim up, down, left, right.
- **Side stud.** A 1 x 2 x 2 bar turned 45 degrees about **X**, so from the side it is a diamond about 2.8 tall and deep: x -3.1..-2.1, centered y 4.5, z 0, on the outer face of the thigh (0.5 proud), at knee height.
- **Side struts.** Two 1 wide x 3 tall x 2 deep bars on the outer face above and below the stud (x -3.1..-2.1): upper at y 0.5..3.5, lower at y 5.5..8.5. Each is tilted about 8.5 degrees so both lean **inward at their far ends**, a very shallow bowed "(" that is outward at the knee. The effect is under 0.5 unit, so straight bars are acceptable.
- The **left leg** is the mirror (x positive) with the same elements.
- The legs are NOT: fitted with thigh pods or greaves larger than the sleeve.

### Boots (leg parts, drawn by the boots only)

Right leg, leg-local:

- **Boot shaft.** 5 wide x 4 tall x 5 deep: x -2.6..2.4, y 8.5..12.5, z -2.5..2.5. It covers the leg's lowest 3.5 units and reaches 0.5 below the vanilla sole.
- **Toe cap.** 4 wide x 2 tall x 2 deep: x -2.1..1.9, y 10..12, z -4..-2. It sticks 1.5 out of the boot front (at z -2.5), overlapping it by 0.5.
- The left boot is the mirror.
- The boots are NOT: soled, heeled with spikes, or finned.

## 4. Silhouette checks

- **Front:** an inverted-wedge silhouette. A square helmet dome with a collar ring one unit wider on each side; two **sloped wing plates** shooting out from the neck sides at 30 degrees below horizontal, reaching about 12.5 from center and ending about level with the upper chest (tips at y about 1.5 to 3.5). Under each wing the sleeve hangs inboard of the tip by about 4 units, leaving an open triangle of air between plate and sleeve. The torso is a 9-wide trapezoid with the 8 x 5 chest block's face in its upper half; the 10-wide belt widens the waist slightly; below that, two 5-wide legs with diamond knee pads and 5-wide boots with small toe caps.
- **Back:** the wings seen as thin slanted bars, the dome's back with a small bump on the collar; **two vertical tanks** 3 wide with a 1-unit gap, a U-bridge above and a small rhombus at its top center; then belt and crotch plate.
- **Side (right):** a deep head (respirator 1.5 forward, collar bump behind), the wing as a stepped slab 8 deep (z -4..4) sloping down, the chest block hanging 3 forward with the abdomen plate slanting back beneath it, tanks 1.5 out the back, the 5-deep sleeve, a thigh with diamond stud and struts on its outside, boots with toe caps forward.
- **Top:** a wide flat "T": a 9-square dome with the collar as a rim, two long wide rectangles (the wings, 8 deep for the lower layer, 6 for the upper) pointing left and right, the chest block poking forward under the dome front, and the tanks poking behind.
- **Worn look on a standing player:** the player's own body is fully hidden. Torso and head are only 1 unit larger than vanilla, so the character does not look obese; the shoulder wings are what widen it. The model is symmetric except for the right helmet lamp, the left-of-center front stud, and the 0.5 shift of the left sleeve.

## 5. Materials and palette

The original Paladin skin is **light-to-mid gray plating with deep black recesses and olive-green hazard stripes**. Values were sampled from the original sheets (a 256x128 sheet used as a 2x version of a 128x64 layout).

### Base variant: Paladin (`t3_power`)

| Part | Material | Base | Highlight | Shadow / recess |
|---|---|---|---|---|
| Dome, torso core, sleeves, thighs, boots | painted gray steel plate | #A1A1A1 (range #979797 to #ACACAC) | up to #CFCFCF on top faces and edges | #767676, #6B6B6B on lower faces |
| Chest block, belt, crotch plate, pauldron plates | same gray, a touch cleaner | #9E9E9E to #A7A7A7 | #BDBDBD | #767676 |
| Respirator, visor slit, chest panels, tank faces, rivets | near-black panels | #171717 to #1B1B1B | #3F3F3F | #070707 |
| Collar ring (all bars) | **hazard stripes**, olive green alternating with black | #304400 and #4B6D00 | #4B6D00 | #293A00, #184200, #0E2A00 |
| Back diamond, bridge, knee pads, stud | mid gray, a bit darker than the plates | #6B6B6B to #767676 | #A1A1A1 | #313131 |

Shading style: flat, lightly speckled with a few gray levels (rough painted metal), highlights on top and edge faces only, no gradients. The **dark panels have a soft, slightly blurred edge**: dark inset windows, not crisp stickers. The hazard stripes run in short diagonal or banded runs along the collar bars; the green is dull olive, never bright lime. Sleeves carry two thin dark bands about 4 and 8 units below the sleeve top and a few small round dark rivets near the bottom edge. The chest block's front has two big dark panels side by side (the left bigger); the abdomen plate has a dark horizontal slot like a vent. The back tanks are dark with a lighter rim. The helmet's top has a faint darker center stripe running front to back.

### Camo variant `dark` (GregGuns name Outcast)

Same layout, plate recolored near-black with the accents turned **dark red**.

| Part | Value |
|---|---|
| Plates | #181818, #1B1B1B, #1E1E1E (mostly under #2A2A2A), brightest #434343 |
| Recesses and panels | #000000 to #101010 |
| Collar stripes | rust #581300 and crimson #4C0000 alternating with #2D0600 and black |
| Other red details | a vertical red stripe down the middle of the helmet top, thin red lines on the helmet dome's sides, two horizontal red bands around each sleeve, small red lights in the knee pads and a red band on the boots |

### Camo variant `dark2` (GregGuns name Outcast 2)

The same near-black plate palette (#000000 to #1E1E1E), but the clean red details are replaced by **irregular blood-rust splatter**: reddish-brown flecks and smears (#380000, #400808, #460E0E, #481010, up to a lightest #5A3B3B), scattered unevenly over helmet, collar, sleeves, tanks, belt and boots; the collar stripes stay #2D0600 and #581300. The splatter is sparser than the `dark` reds (about two thirds as many red pixels) and has no clean bands, so it reads as a **dirty, battle-worn black** suit.

## 6. In-game presentation

- **Which piece draws which part** (the original uses one model class, in two variants):
  - **Helmet:** the head parts only (everything in the helmet section).
  - **Chestplate:** the torso parts (core, chest block, abdomen plate, tanks, bridge, diamond) and **both arms** (sleeves, pauldrons, side and back plates, shoulder caps).
  - **Leggings:** the hip parts (pelvis block, belt, crotch plate) and **both legs'** thigh sleeves, knee pads, side studs and struts. No boots.
  - **Boots:** both legs' boot shafts and toe caps only. No thigh sleeves.
- The torso core and the belt overlap by half a unit (core bottom 11.5, belt top 11), so a lone leggings piece leaves no waist gap and a lone chestplate ends cleanly at 11.5.
- **Scale:** 1.0, no extra inflation. Everything is authored in the vanilla biped frame with the half-unit oversize built into the box sizes.
- **Texture:** a single sheet per variant serves all four pieces (the leggings do not use a second sheet). 128x64 declared UV space, drawn at 2x.
- **Item icon and dropped item:** the original draws the same model, scaled about 1.5x (helmet) or 2x (other pieces), shifted down so it centers and turned to show the side. There is no flat sprite. Not relevant to the worn model.

## 7. Motion

- **No moving parts** in the model. Every part is fixed to a vanilla biped part and follows it:
  - **Head parts** (dome, respirator, collar, lamp, stud): follow the head's yaw and pitch.
  - **Body parts** (torso, chest block, abdomen plate, tanks, pelvis, belt, crotch plate): follow the body; when sneaking the body leans forward and the whole upper suit leans with it. The hip parts belong to the body, not the legs, so they lean too.
  - **Arm parts:** swing with the arm, pauldrons included. Because the plates extend 7 outward and 8 deep, a swinging arm sweeps them forward and back like a flapping wing; the original does not compensate, so they can clip through the chest block and head on large swings. Accept that.
  - **Leg parts:** swing with the leg, thigh parts and boots included.
- Nothing glows, blinks or spins; no animated texture.
- Sneaking, riding and the child flag use stock biped handling.

## 8. What our earlier attempts got wrong

Our current `t3_power` model is a vanilla-biped box set with a purple-blue plate sheet and cyan glow strips.

- **Wrong palette.** Purple-blue with cyan lights reads as another suit. The original is **light gray with olive-green hazard stripes** (near-black with red in the camos). No cyan, no purple, no glow.
- **No wing pauldrons.** Ours has one flat 6 x 10 x 6 slab on the left arm only (plus a band) and two plain slabs by the right arm. The original has a symmetric pair of **30 degree sloped, two-layer, 7-unit pauldrons** reaching 12.5 units from center. Our silhouette is far too narrow and lopsided.
- **No sleeves.** The arm there is the pauldron slab itself, and a "fist" hangs on the left arm only (and is assigned to the leggings, the wrong piece). The original has a 5 x 13 x 5 sleeve on each arm and no fists.
- **A short, off-center helmet.** Ours is 9 x 7 x 9 and 0.25 off-center; the original dome is a full 9 x 9 x 9 cube centered on the head. Our ring is one 11 x 2 x 10 piece; the original is four low bars 10 wide with a back bump, hazard striped. Our lamp is a plain 2 x 2 x 3 box; the original is a 45 degree diamond bar, 4 deep. Ours adds an antenna the original does not have.
- **A flat chest.** Ours has an 8 x 5 x 1 plate and an 8 x 4 x 1 flat vent; the original has a **3-deep chest block** and an abdomen plate **slanted 21 degrees**. Our torso is 6 deep where the original's core is 5.
- **One central backpack.** Ours is a single 4 x 6 x 3 pack with a gauge; the original has **two 3 x 5 x 2 tanks** with a 1-unit gap, a pipe bridge and a rhombus. No gauge.
- **Blocky, lumpy legs.** Ours stacks a 5 x 6 x 6 thigh, a 5 x 6 x 6 shin and a 5 x 2 x 6 foot, with side slabs. The original has a 5 x 9 x 5 thigh sleeve, diamond knee pads, a side stud with bowed struts, and a **separate boot** (5 x 4 x 5 with a 4 x 2 x 2 toe cap) drawn only by the boots piece.
- **A thin dark belt.** Ours is one dark 9 x 2 x 6 belt; the original is a gray 10 x 2 x 6 belt over a 9 x 3 x 5 pelvis block, with a crotch plate.
- **Inconsistent oversize.** The original inflates every limb by half a unit per side; ours mixes near-vanilla boxes with oversized slabs, so proportions feel lumpy.

## 9. Acceptance checklist

- [ ] Width across the pauldron tips is about 24.5 (within 1 unit) and depth about 11.5; the front view reads as an inverted wedge with the wings widest.
- [ ] Each shoulder has two stacked plates sloped 30 degrees down toward the outside (lower 8 deep and 2 thick, upper 6 deep and 1 thick), mirror-symmetric, reaching about 12 units from the center line.
- [ ] Arms have 5 x 13 x 5 sleeves, with a side plate on the outside of the forearm and a small plate on the back of the upper arm; no hands or fists.
- [ ] The helmet dome is a 9-unit cube centered on the head (0.5 above and below vanilla), with a respirator block sticking out 1.5 at the front and a 10-wide collar of four low bars plus a back bump.
- [ ] A single diamond-section lamp bar on the **right** side of the helmet, none on the left, plus a small front stud left of center.
- [ ] The collar is hazard striped (olive green and black in the base camo); no other large green areas.
- [ ] Chest block 8 x 5 and 3 deep, sticking out 2.5 from the core; a slanted abdomen plate (about 21 degrees) below it.
- [ ] The back has two 3-wide tanks with a 1-unit gap, a U-shaped pipe bridge above and a rhombus at its top center; no single central pack.
- [ ] The leggings draw a 10-wide belt, a pelvis block and a 3-wide crotch plate, plus 5-wide thigh sleeves with diamond knee pads and outer side studs and struts.
- [ ] Boots are a separate part set (5 x 4 x 5 shaft and 4 x 2 x 2 toe cap) drawn only by the boots; leggings and boots do not draw each other's parts.
- [ ] Palette: gray plate near #A1A1A1 (shadows near #6B6B6B, highlights up to #CFCFCF), black panels near #171717, hazard green near #304400 and #4B6D00.
- [ ] `dark` is near-black plate (#181818 to #1E1E1E) with dark-red stripes (#581300, #4C0000, #2D0600); `dark2` is the same black with irregular rust-red splatter instead of clean stripes.
- [ ] No antenna and no glow.
- [ ] No part floats; every box overlaps or touches a neighbor (the lamp, struts and abdomen plate are the usual slip-ups).

## 10. Unsure

- **Left sleeve offset.** The original's left sleeve sits half a unit farther out than a mirror of the right (x -1..4 instead of -1.5..3.5, arm-local). It may be a slip in the original; the symmetric value is a safe deviation.
- **The helmet's front stud.** I know its geometry but not its purpose (a light or a badge); it is tiny, so paint it as you see fit.
- **Side struts' tilt direction.** Resolved by hand from the numbers (inward at the ends, outward at the knee); at 8.5 degrees the visible displacement is under half a unit, so straight bars are fine.
- **Palette placement.** Colors come from the whole sheet, not from per-face sampling, and the stripe and band placement in section 5 is read from a low-resolution sheet: a good guess, not a measurement. The collar is certainly the hazard-striped part.
- **Pauldron behavior in motion.** The reference render shows the arms at rest; the flapping and clipping described in section 7 is derived from the geometry, not watched in game. Check the worn look in a client run.
- **Respirator grille and chest-panel details** come from tiny texture patches; treat them as hints.
- **Texture scale.** The original sheet is 256x128 while its model declares 128x64; I treat it as a 2x version of the 128x64 layout. It should not affect a new sheet.
