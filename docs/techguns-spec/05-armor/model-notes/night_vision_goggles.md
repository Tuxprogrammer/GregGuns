# Night Vision Goggles: clean-room model notes

Design notes for rebuilding the Techguns **Night Vision Goggles** worn look in GregGuns, in both of its poses: **on** (lenses down over the eyes) and **off** (flipped up onto the forehead). Written by the "dirty room" analyst from study of the original's two models, renderer, item class and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [rendering-and-models § 2.6 face models](../rendering-and-models.md), [GregGuns Baubles slots](../../../gregguns-spec/05-armor/baubles-slots.md) (the goggles become a Baubles `head` item), [GregGuns implementation notes](../../../gregguns-spec/05-armor/implementation-notes.md).

**Units and frame.** One unit = 1/16 block = one texel of a 64x32 skin. **Y points down**, the player **faces -Z**, the player's **right side is -X**. Everything is given relative to the vanilla **head** part, origin at the neck (0, 0, 0). The head box runs x -4..4, y -8..0, z -4..4. Rounded to half a unit unless it matters. GregGuns accessory: `night_vision_goggles` (one texture, no camo variants).

**Which model is which.** The item draws one of **two models, chosen every frame**: the "on" model (class ModelNightVisionGoggles) while the wearer has a Night Vision potion effect (this includes the effect the goggles themselves apply), and the "off" model (ModelNightVisionGogglesOff) otherwise. Both share the same texture and the same five parts, in different poses. `model-ab/singles/acc_night_vision_goggles.png` shows the **on** pose only; there is no render of the off pose, so the off numbers below come from the pose angles, not from sight (see section 10).

## 1. Gestalt

A **black head-strap rig with two stubby binocular tubes**. At a glance, the on pose reads as: a thin black band around the forehead, a short strap over the crown, and two **diamond-section tubes pointing forward** off the eyes, each with a **green lens** on its front end, joined by a small bridge and a diagonal arm back up to the band. The off pose reads as the same rig **pushed up**: the tubes and a bridge stand above the forehead, tilted back like a pilot's goggles on a cap.

The head itself is **bare**: the head cube is transparent except for the straps. It is a thin, skeletal, all-black shape against the face. The only color is the two green lenses.

Defining features in order of visual importance:

1. **Two forward-pointing diamond-section tubes** with green lens ends (on pose), or the same two tubes tilted up above the forehead (off pose).
2. **A thin black headband** around the forehead, with a strap over the crown.
3. **A bridge block and a diagonal mount arm** tying the tubes to the band.
4. Everything is **near-black** apart from the lenses.

## 2. Overall proportions

All pieces are the same in both poses; only the placement differs.

| Measure | Value |
|---|---|
| Headband | wraps the head, 1.5 tall, at y about -7.5 to -6, drawn about 0.06 outside the skull |
| Crown strap | 2 wide, runs front to back over the top of the head, continues 0.5 down the front and back to meet the band |
| Tube (each) | 2 x 2 section (turned 45 degrees about its long axis into a diamond, about 2.8 across) x 4 long |
| Bridge block | 3 wide x 1 tall x 2 deep |
| Mount arm | 2 wide x 1 thick x 4 long |
| **On pose:** tube centers | x = plus and minus 1.7, y = -4, front end at z -8, back end at z -4 (touches the face) |
| **On pose:** reach beyond the face | 4 (front of tubes at z -8) |
| **On pose:** overall width | about 6.2 (tubes at x plus and minus 3.1 outer edge), narrower than the 8-wide head |

## 3. Part by part

### Headband and crown strap (identical in both poses)
- The head-sized cube (8 x 8 x 8, about 0.06 oversize) is **transparent almost everywhere**. Painted on it: a **1.5-tall band** around all four side faces at y -7.5 to -6 (just under the top edge, a forehead band), and a **2-wide strip across the top face** running front to back, centered on x 0. The strip drops a half unit down the front and back faces to meet the band, with a small tab on the back.
- There is **no buckle, no side pad, no chin strap**.

### Tubes (two)
- Each is a square bar 2 x 2 in section and **4 long**, turned about 45 degrees about its own long axis, so from the front it is a **diamond** about 2.8 across.
- **The two ends differ:** the end that faces away from the wearer has a **green lens** (a bright green diamond, center #2F7B15 with a darker green rim #1D4B0D); the other end (toward the wearer) has a **dark blue-gray** eyepiece (#2C2C47 center, #181826 rim). The long sides are near-black.
- **On pose:** the tubes lie flat, pointing straight forward (-Z), parallel and level, one in front of each eye: centers at x = plus and minus 1.7, y -4, running from z -8 (green lens, front) to z -4 (the face, blue-gray end hidden). Their inner edges pass about 0.3 from the center line, overlapping the bridge.
- **Off pose:** the tubes are flipped up and tilted. Each tube's center is at about (plus or minus 1.7, -9, -5), 1 unit above the head top and 1 in front of the face. The long axis points about 52 degrees below horizontal toward the front: its **lens end (green) is the upper end, at about y -10.6, z -3.8, above the front edge of the head, facing up and back**, and its **eyepiece end is the lower end, at about y -7.4, z -6.2**, hanging forward of the forehead. The tubes also carry a slight twist and splay (about 30 degrees yaw, 35 degrees roll). Both tubes use the **same** rotation (not mirrored), so the pair is very slightly asymmetric. The top of the diamonds reaches about 3.5 above the head top.

### Bridge
- A flat block, **3 wide, 1 tall, 2 deep**, between the tubes.
- **On pose:** untilted, at x -1.5..1.5, y -5..-4, z -7..-5: in front of the brow, between the tubes' inner halves. It overlaps both tubes so it reads as a binocular body.
- **Off pose:** the same block at about (0, -9.6, -5.2) (about 1.6 above the head top and 1.2 in front of the face), pitched 35 degrees so its front edge is lower than its back edge.

### Mount arm
- A flat plate, **2 wide, 1 thick, 4 long**, connecting the bridge to the headband.
- **On pose:** pitched 45 degrees, so it runs from the bridge (front, low) at about y -5, z -6.5 **back and up** to about y -7.8, z -3.7, where it meets the headband at the brow. The side view shows it as a diagonal strut from the band down to the tubes.
- **Off pose:** folded up to nearly **vertical** (pitched about 75 degrees from flat). It starts at about y -6, z -3.5 (inside the forehead, behind the face plane) and rises to about y -9.9, z -4.5, so it **stands up in front of the forehead and about 2 above the head top**, leaning a little forward (about 15 degrees from vertical). The bridge sits on its top.

### What it is NOT
No eye cups, no glass in front of the eyes, no rubber, no battery or cable. No lenses on the head-side (the blue-gray eyepiece ends are tiny and dark). The head is not covered.

## 4. Silhouette checks

**On pose**
- **Front:** a thin black horizontal band (8 wide, 1.5 tall) across the forehead, with a 2-wide strap rising above it; below, a narrow vertical neck (the bridge and arm) dropping to two **green diamonds** side by side (each about 2.8 across, centers 3.4 apart).
- **Back:** the same band and strap, with two small dark blue-gray diamonds (eyepiece ends) peeking below the bridge if viewed through. Mostly the band.
- **Side:** the band at the top, a **diagonal arm** going forward and down from it, and a block (the tube, 4 deep and 2.8 tall) projecting forward at eye level, ending at the green lens.
- **Top:** a thin strip down the center, the band around the sides, and two parallel bars (the tubes) extending forward past the forehead.

**Off pose**
- **Front:** the band, then above it an upright arm 2 wide topped by the bridge, flanked by the two tube ends leaning toward the viewer.
- **Side:** the band; the arm standing up in front of the forehead; the tube slanting from the top back corner down to the front, rising above the head top.
- **Worn, standing (both):** near-black hardware on an otherwise bare head: on, you see two green dots in front of the eyes; off, you see a black shape above the brow.

## 5. Materials and palette

Matte black plastic with two green lenses. Flat shading, almost no noise.

| Part | Base | Highlight | Shadow / frame |
|---|---|---|---|
| Strap and headband | #191919 | #1F1F1F | #151515 |
| Tube sides, bridge, mount arm | #141414 to #191919 | #1E1E1E | #121212 |
| Lens (front end of each tube) | #2F7B15 | #38901C (approximate) | rim #1D4B0D |
| Eyepiece (back end of each tube) | #2C2C47 | #292942 | rim #181826 |

## 6. In-game presentation

- **Original slot and draw order.** A **face slot** item, drawn during the player's **helmet armor pass** as an extra render of the biped **head only**, **before** the real helmet layer. The renderer picks the **on** model when the wearer has any Night Vision potion effect and the **off** model otherwise. (It does not read the item's own switch; the goggles' effect shows up as the potion.)
- **Frame and scale.** Both models' parts are children of the head and sit in the head frame given above. Drawn in the player's normal render transform: same 15/16 player scale as the body, no extra scale, no extra offset. The strap cube is about 0.06 oversize.
- **Helmet interaction.** The band and crown strap are only 0.06 outside the skull, so any helmet (at least 0.5 inflation) **covers them**. The tubes in the on pose stick **4 units forward of the face**, and the off-pose arm, bridge and tubes stand about 2 to 3.5 above the head top, so these pieces **poke out of** most helmets (visor or crown). The goggles are not drawn at all when the worn helmet is flagged "hide face slot" (the steam, power-armor and HEV helmets are; the exo helmet and every other helmet show them). In GregGuns the goggles are a Baubles `head` item and coexist with a helmet; the builder should draw them outside a closed helmet's front and top, and treat the helmet as hiding only the headband.
- **No camo.** One texture.

## 7. Motion

- Follows the **head** (yaw relative to body, look pitch). Nothing animates on its own.
- **Pose switching:** an instant swap between on and off poses, no transition, according to the night vision state.
- **Sneaking:** the head pivot drops 1 unit and the goggles follow.
- **Riding:** the head yaw is clamped relative to the mount.
- Body, arms and legs have no effect.

## 8. What our earlier attempts got wrong

GregGuns has **no worn goggles model yet**: they are a Baubles `head` item with no worn render, invisible when worn. There is nothing to compare. For the builder:

- **Do not put a lens plate or glass over the eyes.** The tubes stand out 4 units in front of the face.
- **Do not make square boxes.** The tubes are diamonds: a square bar turned 45 degrees about its long axis.
- **Do not leave the head covered.** It is bare apart from straps.
- **Do not forget the second pose.** The on and off poses are different shapes.
- **Keep the lenses the only color.** The rest is black.

## 9. Acceptance checklist

- [ ] Head is bare except for a 1.5-tall band at y -7.5 to -6 and a 2-wide crown strap.
- [ ] Two 2 x 2 x 4 tubes, turned 45 degrees about their long axis (diamond section, about 2.8 across).
- [ ] **On:** tubes level and forward, centers at x plus and minus 1.7 and y -4, front ends at z -8, back ends at the face (z -4).
- [ ] **On:** green lens (about #2F7B15, rim #1D4B0D) on each front end, dark blue-gray (about #2C2C47) eyepiece on each back end.
- [ ] **On:** bridge block 3 x 1 x 2 at y -5..-4, z -7..-5 between the tubes.
- [ ] **On:** mount arm 2 x 1 x 4 pitched 45 degrees, from the bridge back and up to the headband.
- [ ] **Off:** tubes lifted so their centers sit about 1 above the head top and 1 in front of the face, leaning forward-down with the green lens end up and back at about y -10.6.
- [ ] **Off:** mount arm nearly vertical, standing in front of the forehead, rising to about 2 above the head top.
- [ ] **Off:** bridge on top of the arm, about 1.6 above the head top, pitched 35 degrees.
- [ ] All metal is near-black (about #191919); only the lenses carry color.
- [ ] The pose is picked by the wearer's night vision state; no animation between.
- [ ] Attached to the head frame and follows head yaw and pitch.
- [ ] Not drawn when the worn helmet is flagged "hide face slot" (if it is an extra slot item).

## 10. Unsure

- **The off pose is computed, not seen.** There is no render of it. I derived each part's position from its pivot and rotation angles, assuming the renderer applies roll, then yaw, then pitch to each part. The numbers are good to about 1 unit; a builder should confirm that the tubes read as "flipped up and leaning forward".
- **Off pose tilt.** Because both tubes use the same rotation, they are slightly asymmetric; a builder may mirror them.
- **Texture strap details.** The back-face tab and the half-unit drops of the crown strap are from a very low-resolution texture and may be accidents.
- **Pose trigger.** The original keys the on pose off the Night Vision potion, not the item's switch; GregGuns may prefer its own toggle.
- **Helmet sizes.** Not measured per helmet; "tubes poke out of every helmet" is from the 4-unit reach against a half-unit-to-one-unit inflation.
