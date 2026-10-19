# Alien Blaster: clean-room model notes

Design notes for rebuilding the Techguns **Alien Blaster** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelAlienBlaster](../gun-models.md), [rendering framework](../rendering-framework.md), [revolver notes (template)](revolver.md).

**Units and axes used in this document.** One unit = one texel at the native 128x64 scale = 1/16 block. The muzzle points toward **+F** (forward). **Y points down**, with **Y = 0 on the gun's long axis** (the center of the diamond body). **X = 0 is the mid-plane**; widths are total, centered. **F = 0 is the middle of the first stepped ring**, which sits 1 unit in front of the main body's front face. Rounded to the nearest half unit unless it matters.

**Key idea.** Almost every part is a **square bar turned 45 degrees about the long axis**, so it presents a **diamond (rhombus) cross-section**. "Across" below means the full diagonal of the diamond. A square bar of side s is s x 1.414 across: 1 gives 1.4, 2 gives 2.8, 3 gives 4.2, 4 gives 5.7, 5 gives 7.1, 6 gives 8.5.

## 1. Gestalt

A **pale, silvery, alien energy pistol** built as a stack of diamond-section rings along a single axis: a fat main body at the back, stepping down in rings to a thin needle barrel with dark collars, a small blue cell at the very rear, **six small blue studs** on the shoulders, two **swept-back blade wings** at the sides, and a **dorsal fin** raked up and back. A short, slanted grip hangs under the rear half. Cool white-silver body, mid-gray trim, near-black muzzle rings, and spots of pure blue.

Defining features in order of visual importance:

1. **Diamond cross-section everywhere**: from the front it is a rhombus, not a square. The silhouette from the top is a pointed spindle.
2. **Stepped needle front**: body, then rings of decreasing size down to a thin barrel with two dark collars and a dark muzzle block.
3. **Six blue studs** (two rows of three) on the upper facets, and a blue cell at the rear.
4. **Swept wings and dorsal fin**: thin 1-unit blades, wings swept back about 25 degrees, fin raked up and back about 25 degrees.
5. **Slanted grip** (about 25 degrees back from vertical) with a small trigger and no trigger guard.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (rear nub to muzzle) | 36 (F -18 to 18) |
| Total height (fin tip to grip bottom) | about 20 (from Y -6.5 to 13.6 relative to the axis, where the fin tip is at -6.5 and the grip bottom at +13.6) |
| Total width (wing tip to wing tip) | about 13 (X +-6.5) |
| Length : height : width | about 2.8 : 1.5 : 1 |
| Main body | 12 long, 8.5 across (F -13 to -1) |
| Needle barrel | 12 long, 1.4 across (F 5 to 17) |
| Grip | 9 long, 3 wide, 4 deep |

Along the axis from the rear: a 1-unit rear nub (F -18 to -17); a blue cell (F -17 to -15); a rear ring (F -15 to -13); the 12-long main body (F -13 to -1); two stepped rings (F -1 to 1 and 1 to 3); two small dark rings (F 3 to 4 and 4 to 5); the needle barrel (F 5 to 17) carrying collars; the muzzle block (F 16 to 18).

The **top line is a ridge**: the diamond's top corner (4.2 above the axis) runs the length of the main body; the dorsal fin rises out of it near the rear, up to 2.2 above the ridge.

## 3. Part by part

### Main body
- A 6x6 square bar, **12 long**, turned 45 degrees: **8.5 across** (top corner Y -4.2, bottom corner Y +4.2, side corners X +-4.2), F -13 to -1.
- It is the widest part of the gun. The four lengthwise facets are painted with thin panel seams (section 5).
- Touches: the stepped rings at its front and rear faces; the wings emerge from its side corners; the grip tang sits under its rear half; the fin and studs sit on top.

### Front stepped rings (all concentric on the axis)
| Ring | F range | Section | Across |
|---|---|---|---|
| Step 1 | -1 to 1 | 5x5 turned 45 | 7.1 |
| Step 2 | 1 to 3 | 4x4 turned 45 | 5.7 |
| Step 3 (dark) | 3 to 4 | 3x3 turned 45 | 4.2 |
| Step 4 (dark) | 4 to 5 | 2x2 turned 45 | 2.8 |

Each is a short disc-like ring. The step-down looks like a telescoping lens barrel.

### Needle barrel
- A 1x1 bar turned 45 degrees (**1.4 across**), **12 long**, F 5 to 17, on the axis. Light silver.
- **Collar A:** 3x3 turned 45 (4.2 across), 1 long, F 12 to 13, dark gunmetal.
- **Collar B:** same 3x3 (4.2 across), 1 long, F 14 to 15, dark gunmetal.
- **Muzzle block:** 2x2 turned 45 (2.8 across), **2 long**, F 16 to 18, dark gunmetal. The barrel runs into it; its front face is the gun's tip.
- Between the collars and the muzzle the barrel is visible as a thin silver needle, 1 unit of bar between collars A and B, 1 between B and the muzzle block.

### Rear section
- **Rear ring:** 5x5 turned 45 (7.1 across), 2 long, F -15 to -13, mid-gray with a **dark grid pattern** (section 5).
- **Cell:** 2x2 turned 45 (2.8 across), 2 long, F -17 to -15. Pure blue.
- **Rear nub:** a **1x1x1 cube, not turned** (axis-aligned, so it shows a square, not a diamond), F -18 to -17. Dark gray. It sits on the axis.

### Studs (six)
- Six 1x1x1 cubes, **turned 45 degrees** so they sit flush on the body's upper facets. Each protrudes about half a unit from the facet. Pure blue.
- Two rows (left and right, X = +-2 from the axis, Y about -2.3), in **three pairs along F: -4.5, -6.5 and -8.5** (each stud 1 long). They sit on the two upper sloped facets of the main body, mirrored left and right.

### Wings (two)
- Each is a thin blade, **1 thick (Y -0.5 to 0.5), 3 wide, 7 long** before sweeping, lying on the horizontal plane through the axis, hinged at the body's side corner.
- **Hinge point:** X +-3.5, F -4 (just inside the body's side corner at +-4.2). The blade is yawed **25 degrees back and outward** about a vertical axis through that point.
- Resulting corners per wing (left wing; the right is a mirror): front-outer (X -3.5, F -4), front-inner (X -0.8, F -5.3), rear-outer (**X -6.5, F -10.3**), rear-inner (X -3.7, F -11.6). Most of the blade is inside the body; the visible part is a thin swept triangle that pokes about 2.2 beyond the body's side corner, with its tip at F -10.3 and its trailing edge reaching F -11.4 where it re-enters the body.
- Mid-gray, a bit darker than the body.

### Dorsal fin
- A thin blade, **1 thick, 3 tall, 7 long**, centered on the mid-plane, hinged at the body's top at F -4 and **raked 25 degrees**, so the rear rises.
- Top edge from (F -4, Y -3.5) up to (F -10.3, Y -6.5); bottom edge from (F -5.3, Y -0.8) to (F -11.6, Y -3.7).
- It emerges from the ridge at about F -5.6 and rises to **2.2 above the ridge** at its rear tip, then the trailing edge drops back into the body at F -11.4. Mid-gray.

### Grip and trigger
- **Grip tang:** a 3x3x5 box (F -10 to -5, Y 3 to 6, X +-1.5) directly under the body's rear half, not turned. Very light (#D8DBE0).
- **Grip:** 3 wide, 9 long, 4 deep, **hinged at the tang's front-top** (F -6, Y 5.5) and tilted **25.6 degrees back** (bottom further to the rear). Resulting corners: top-front (F -6, Y 5.5); top-rear (F -9.6, Y 3.8); bottom-front (F -9.9, Y 13.6); bottom-rear (F -13.5, Y 11.9). The bottom edge slopes, with the front corner lowest. Overlaps the tang's upper part.
- **Trigger:** a small 1 wide x 3 tall x 2 deep block, tilted **15 degrees** back, hinged at (F -4, Y 4.0), hanging in front of the grip tang. Bottom at Y about 6.9; its bottom touches the grip's front edge. Dark gray. **No trigger guard.**

## 4. Silhouette checks

- **Side:** a long spindle. The body is a tall, fat middle with the dorsal fin raking up behind its center-top; wings are barely visible as a thin line through the axis; to the right, the stepped rings narrow to a needle with two dark collars and a dark muzzle block; to the left, a small cell and a nub; the grip hangs from the rear half, leaning back; the small trigger sits just in front of it. Blue dots along the upper edge.
- **Top:** a pointed diamond-shaped body with the two swept wings spreading to 13 wide near the rear, the fin as a thin center line, and six blue dots (two columns of three) just off the center.
- **Front:** a rhombus (8.5 across) with a smaller rhombus inside from each ring, a tiny rhombus (barrel) and a tinier dark one (muzzle) at the center; wings as two thin horizontal tips; the fin a thin vertical line above; the grip below.
- **Back:** the rear nub and blue cell at the center of a 7.1 rhombus ring, then the 8.5 rhombus, wings sweeping out, the fin, and the grip hanging below.
- **Negative space:** none to speak of. The gun is solid, with the thin needle barrel clear between the collars.

## 5. Materials and palette

| Part | Material | Base | Highlight | Shadow / detail |
|---|---|---|---|---|
| Main body | polished silvery white | #D0D3D7 | #E1E5E8 | seams #636465 |
| Needle barrel | silver | #D0D3D7 | #E0E3E8 | #BDBFC4 |
| Step 1, step 2 | mid gray | #AAACAE | #B1B2B4 | #A0A2A3 |
| Dark rings (steps 3, 4), collars, muzzle block | dark gunmetal | #666869 | #6F7172 | #5C5E5F |
| Rear ring | mid gray with grid | #9B9C9E | #AFB0B2 | grid #646667 |
| Cell, studs | pure blue | #0055FF (flat) | | |
| Rear nub | dark gray | #6B6C6E | | |
| Wings | mid gray | #A9AAAD | #AEB0B3 | #959699 |
| Dorsal fin | mid gray | #A5A7A9 | #ADAFB2 | #9EA0A3 |
| Grip tang | near-white | #D8DBE0 | #EAEDF1 | #C0C3C7 |
| Grip | light gray with ridges | #C5C7CB | #D7DBDF | ridges #A5A7A8 |
| Trigger | dark gray | #656769 | #707173 | #616264 |

Shading style:
- **Metal:** a cool, slightly blue-tinted silver (blue channel about 4 above red). Flat with fine noise.
- **Main body panel lines:** on each of the four lengthwise facets, **two thin dark lines** (1 pixel, #636465) run the length: on the upper facets at about one-third and five-sixths across, on the side facets at about one-sixth and two-thirds down. They read as panel seams, not grooves.
- **Rear ring:** a **grid of dark 1-pixel lines** forming roughly 4-pixel squares on #AAAAAA.
- **Grip:** vertical **ridge stripes** every 3 pixels (#A5A7A8 on #C5C7CB) on its side faces, so it reads as a molded handle; the top and bottom are plain.
- **Blue parts:** flat, saturated blue; in the original model they get no special glow pass, so they are lit like the rest. A builder may make them emissive, but not required.

## 6. In-game presentation

All transforms apply in block units after the 1/16 scale. The model is first rotated 180 degrees about X.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw 225 degrees, scale 1.4 | hold shift (-0.55, -0.35, -0.65). No scope. |
| First person firing | pitch of up to about 5 degrees, backward kick of up to 0.15 along the axis | at mid-animation |
| First person reloading | pitch of up to 50 degrees | |
| Third person (held) | yaw 135 degrees, scale 0.8 | offset (0.1, -0.75, 0.9). |
| Aimed | extra offset (0, -0.1, 0) | |
| Ground (dropped) | scale 0.6 | lowered by 0.45. |
| Inventory icon | yaw 90 degrees, scale 0.8 | offset (0.2, 0, 0); the icon is the 3D model. |

**Muzzle flash:** an additive blaster-flash sprite at about 1.5 scale near (-0.55, -1.5, 1.01) in block units from the model origin; 10-tick duration. The flash is drawn at the gun tip (F 18).

## 7. Motion

- The **model has no moving parts.** All motion is whole-gun transforms.
- **Recoil (third person):** the gun pitches back about 25 degrees (pivot about 0.9 block behind the origin) in the first quarter of the first half, then settles.
- **Reload (third person):** tilts 50 degrees about the same pivot, eased.

## 8. What to avoid

- **Square cross-section.** Every ring, the barrel and the body must be turned 45 degrees (diamonds).
- **A thick barrel.** The barrel is a needle only 1.4 across; the collars and muzzle block are the dark features.
- **A rectangular grip hung straight down.** It is tilted back 25 degrees.
- **Putting the wings in front.** They sweep backward (rear tips wider than their roots) and sit on the axis plane.
- **Dark body.** The body must be near-white silver with the dark parts limited to rings, collars, the nub, and the trigger.
- Do not add a trigger guard, scope, iron sights or ammo window; the original has none.

## 9. Acceptance checklist

- [ ] Length : height : width within about 10 percent of 2.8 : 1.5 : 1.
- [ ] Main body is a 12-long diamond bar, 8.5 across, with two panel lines per facet.
- [ ] Four stepped rings in front: 7.1, 5.7, 4.2 (dark), 2.8 (dark).
- [ ] Needle barrel 12 long, 1.4 across, with two dark 4.2-across collars at F 12 to 13 and 14 to 15.
- [ ] Dark 2.8-across, 2-long muzzle block at the very tip (F 16 to 18).
- [ ] Rear: gray grid ring (7.1 across), blue 2.8-across cell, and a small axis-aligned dark nub.
- [ ] Six blue studs flush on the upper facets in three pairs.
- [ ] Two thin wings swept back about 25 degrees, tips about 6.5 off the axis.
- [ ] Dorsal fin raked up and back about 25 degrees, 2.2 above the ridge at its tip.
- [ ] Grip tilted about 25 degrees back, with a light-gray tang above it and a small dark trigger in front.
- [ ] Body palette #D0D3D7; trim #A5A7A9 to #AAACAE; rings #666869; blue #0055FF.
- [ ] No part floats; every piece overlaps or touches a neighbor.

## 10. Unsure

- **Blue studs.** I believe they are not emissive in the original (no glow pass in the model), but the flat #0055FF reads as a light. Making them emissive is a harmless deviation.
- **Wing visibility.** Because the wing blades lie on the axis plane and are 1 thick, they read only as thin lines from the side. This matches the original; do not thicken them.
- **Rear nub.** It is the only part that is not turned 45 degrees. I read this from the data as intentional.
- **First-person framing numbers** come from formulas read, not seen in game. Treat the offsets as approximate.
- **Camo variants:** none. One texture only.
