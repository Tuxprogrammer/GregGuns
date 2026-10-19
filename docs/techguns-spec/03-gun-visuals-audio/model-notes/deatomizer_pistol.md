# De-Atomizer Pistol (and Akimbo): clean-room model notes

Design notes for rebuilding the Techguns **De-Atomizer** (and its **Akimbo** pair) look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderers and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [gun-models](../gun-models.md), [rendering framework](../rendering-framework.md), [Techguns gun stats](../../02-guns/gun-catalog.md).

**Units and axes used in this document.** One unit = 1/16 block. Muzzle toward **+X**. **X = 0 is the rearmost point of the body** (the rear plate's lower edge reaches about -0.5). **Y points down** (Minecraft model convention), with **Y = 0 at the top corner of the laser-sight rod**, the highest point. **Z = 0 is the gun's mid-plane**; widths are total widths, centered. Rounded to the nearest half unit unless it matters. "Ring" means the cluster of thin rods around the central core; the ring's axis is the gun's bore axis at **Y 3.5, Z 0**.

## 1. Gestalt

A small, **pale-gray sci-fi pistol whose business end is a cluster of twelve thin rods around a white core**, like a tiny Gatling or a "flower" of emitters, ending in a **glowing blue disc** and four white prongs that pinch in toward the beam. Behind the cluster is a short rod-wrapped body, a **black grip** raked slightly back, a skinny open trigger guard, and a **thin diamond laser-sight rod** on top with an **orange emitter dot** at its front. The cluster is half the gun's length.

Defining features in order of visual importance:

1. **Twelve-rod ring around a white core** at the front, round in cross-section (about 3.6 across), 6 long, each rod capped with a small cube.
2. **Blue glowing disc** at the very front of the core (2x2, 1 thick), the only saturated color besides the orange dot.
3. **Four white prongs** (1x1x2) splayed inward at about 23 degrees beyond the ring, like pincers.
4. **Black grip** against the pale-gray metal, 2 wide, raked about 12 degrees.
5. **Thin laser rod on top**, a 1.4-wide diamond running the length of the body, with a hot orange 1x1 front face.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (rear plate to prong tips) | about 19 (X -0.5 to 18.3) |
| Total height (laser rod top to grip bottom) | about 12 (Y 0 to 11.9) |
| Total width | about 4 (ring 3.7; the side strips 3; the side button reaches Z -2) |
| Length : height : width | about 5 : 3.2 : 1 |
| Cluster (core + caps + blue disc + prongs) | X 9 to 18.3, about 9 long, 48 percent of the length |
| Ring rods | 6 long (X 9 to 15) |
| Body | 9 long (X 0 to 9), 3 to 3.4 tall, 2 to 3 wide |
| Grip | 7 long, 2 wide, 3 deep, tilted 12 degrees back |

The ring axis, the body's center line and the core are all at Y 3.5.

## 3. Part by part

### Core
- A **2x2 square bar, 6 long** (X 9 to 15, Y 2.5 to 4.5, Z -1 to +1). White-gray.
- **Cap:** a 2x2x1 white plate (X 15 to 16) on its front end.
- **Glow disc:** a 2x2x1 **blue** plate (X 16 to 17) in front of the cap. This is the front-most flat part, the "emitter". Its front is where the beam appears to come from.

### Ring of twelve rods
- Twelve **1x1 square rods, 6 long** (X 9 to 15), the same length as the core, evenly spaced around the core at about **30 degrees apart on a circle of radius about 1.35** from the bore axis. They are tangent-ish: each square is turned to the local angle, so from the front the ring reads as a round "flower" of squares, about 3.7 across overall (the cardinal rods are centered 1.3 from the axis: the top rod at Y 2.2, the bottom rod at Y 4.8, the side rods at Z +/-1.3).
- The four **cardinal rods** (top, bottom, left, right) are axis-aligned; the eight **intermediate rods** are rotated 24 degrees (the ones nearest the horizontal) or 67 degrees (the ones nearest the vertical) about the bore axis.
- The rods touch or overlap each other and the core's sides by 0.2 or less. The ring is solid-looking, not airy.
- Each rod ends in a **1x1x1 cube cap**, flush with the front of the rod, at X 15 to 16 (the same plane as the core's cap). So the ring plus core end in a flat plane at X 16, with the blue disc standing 1 further forward.
- Rods are mid-light gray; the caps are slightly darker.

### Prongs
- **Four prongs**, each **1x1x2**, at the top, bottom, left and right of the ring at X 16 to 18.3, hinged at their rear ends (X 16) outside the ring (radius about 1.6 on the left and right, about 1.1 above and below the axis; top prong at Y 1.9, bottom at Y 4.6, sides at Z +/-1.1). Each is tilted **23 degrees inward**, so their **tips converge toward the bore axis** (pincer shape). Together they surround the blue disc, which stands between them at X 16 to 17.
- Brightest parts of the gun (#C1C1C1 to #C5C5C5).

### Body (rear housing)
- **Top slab:** 2 wide, 2 tall, 9 long (X 0 to 9, Y 2 to 4, Z -1 to +1).
- **Bottom bar:** 2 wide, 1 tall, 9 long (Y 4 to 5, X 0 to 9), directly under the slab.
- **Six thin strips wrap the housing**, each 1x1 and 9 long (X 0 to 9): two **flat side strips** (Z +/-0.5 to +/-1.5, Y 3 to 4), and four **corner strips** tilted 28 degrees about the long axis, centered at Z +/-0.8 and Y 2.8 (upper) or 4.2 (lower). They give the housing a faceted, ribbed, chamfered look and a width of 3. Visually it is the same "bundle of rods" idea as the ring, in miniature.
- **Rear plate:** 2 wide, 3 tall, 1 thick (X 0 to 1, Y 2 to 5), tilted about 10 degrees so its bottom sticks 0.5 behind the top.
- **Rear wedge:** a 2x1x2 little wedge tilted about 27 degrees under the rear of the housing (X 0.2 to 2.5, Y 4.2 to 6), blending the housing's bottom into the grip's rear.

### Grip
- A 2-wide, 7-long, 3-deep block (Z -1 to +1), hinged at its top front corner (X 5, Y 5) and **tilted 12 degrees** so the bottom swings back. Corners: top front (5, 5), top rear (2.1, 4.4), bottom front (3.6, 11.9), bottom rear (0.6, 11.2). It hangs from the housing's bottom bar. Straight-sided.

### Trigger guard and trigger
- **Front post:** 1 wide, 3 tall, 1 long (X 7 to 8, Y 5 to 8).
- **Bottom bar:** 1 wide, 1 tall, 3 long (X 4 to 7, Y 7 to 8), overlapping the grip's front face by about 0.5.
- **Trigger:** a 1x2x1 blade, tilted about 31 degrees (bottom leaning forward), at X 4.5 to 6, Y 4.5 to 6.7, centered on the mid-plane.
- The guard is a plain skinny open loop; no side faces.

### Laser sight (on top)
- A **1x1 square rod turned 45 degrees** (a diamond 1.4 across), 7 long (X 2 to 9, Y 0 to 1.4, Z -0.7 to +0.7). It is the highest part of the gun. Its **front end face (1x1, facing the muzzle) is orange**.
- **Supports:** two **zero-thickness vertical plates** at Z +/-0.3 (each 1 tall, 6 long, X 2 to 8, Y 1 to 2) under the rod, and one **small post** 1x2x1 (X 2 to 3, Y 1 to 3) tilted about 31 degrees at the rod's rear, joining it to the housing. The plates are paper-thin; do not give them thickness.
- Its front end is flush with the housing's front face (X 9), directly above the ring's top rod.

### Side button
- A small 1x1x1 cube, turned 45 degrees about the long axis (a diamond 1.4 across), on the **-Z (left) side only**, at X 5 to 6, Z -2 to -1, centered about Y 4.2. It is a single bump on the left strip; **there is no matching button on the other side**. Light gray.

## 4. Silhouette checks

- **Side:** a small, tall-ish pistol. From the left: a rear plate, a laser rod running along the top, the grip hanging down and back, a skinny open guard with a short trigger, then the **big round-ended cluster** in front: twelve rods, then a blue disc and four prongs converging. The cluster is the same height as the housing (about 3.6) and so reads as a barrel continuing the body, with tiny notches between the rods. Negative space: the gap between the guard and the grip; the thin gap above the housing around the paper-thin plates.
- **Top:** a narrow spine: the housing (3 wide, with ribbed edges), the laser rod as a slim centered stripe, then the ring (3.7 wide) and prongs converging. The button bump is visible on the left.
- **Front:** a round "flower" of twelve rods (3.6 across) with the white core and the blue disc in the center and four prongs in front of it, all inside a 4-wide outline; a black grip column below it and a thin diamond laser rod above.
- **Back:** the 2-wide rear plate on the 3-wide ribbed housing, with the black grip column below.
- **Underside:** the housing and the bottom bar, the guard, the grip bottom, the ring in front.

## 5. Materials and palette

The original texture is pale gray with a black grip, one blue glow and one orange dot. It is 128x128 for a model drawn on a 64x64 grid (two texture pixels per model unit); unused areas of the file are plain white.

| Part | Base | Highlight | Shadow / seam |
|---|---|---|---|
| Ring rods | #BABABA | #C5C5C5 | #A7A7A7 |
| Rod caps (roundels) | #B2B2B2 (flat, no noise) | | |
| Core (bar and cap) | #C5C5C5 (flat) | | |
| Prongs | #C1C1C1 | #C5C5C5 | #BCBCBC |
| **Blue glow disc** | #28A8FF | #50C5FF (a bright spot in the middle of the front face) | #22A2FF |
| Top slab (front face) | #A7A7A7 | | the side faces #AEAEAE; top and bottom #BFBFBF |
| Bottom bar | #BDBDBD | #C5C5C5 | #ADADAD |
| Side strips and corner strips | #BEBEBE | #C5C5C5 | #A7A7A7 (ends) |
| Rear plate | #C3C3C3 | #C5C5C5 | #BDBDBD |
| Rear wedge | #B5B5B5 | #BDBDBD | #ADADAD |
| Grip | near-black #141313 | a narrow **light-gray (#C5C5C5) vertical stripe** between two black panels | |
| Trigger guard | #C5C5C5 | | #ADADAD |
| Trigger | #C5C5C5 (flat) | | |
| Laser rod | #BCBCBC; **front face orange #FF5E1E** (mid #FF4800 to #FF743D) | | |
| Laser supports | #BFBFBF | | #A7A7A7 |
| Side button | #A7A7A7 (flat) | | |
| Status light on the side strips | a **single tiny green pixel** (#42FF42), half a unit across, on each side strip's outer face about 2.5 units from the rear | | |

Shading style:
- **Pale, flat, satin gray metal** in a narrow range (#A7A7A7 to #C5C5C5), with only a couple of levels of variation: lighter on top faces, darker on end faces. It is **not** dark and **not** shiny. Several small parts are one flat tone.
- The only strong contrasts are the **black grip**, the **blue disc** and the **orange dot**.
- The blue disc is a flat saturated cyan-blue, brighter in the middle. It should look like an emitter, not a lens.

## 6. In-game presentation

All transforms apply to the whole model after its 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw 225 degrees, scale **2.5** | idle shift (-0.75, -0.45, -0.8). No scope. |
| First person firing | up to about 12 degrees of pitch about a point 0.4 above the gun origin | peaks at mid-animation. |
| First person reloading | about 35 degrees pitch at mid-animation | smooth rise and fall. |
| First person aimed | extra offset (0, 0, 0.2) | no zoom overlay. |
| Third person (held) | yaw 135 degrees, scale 1.5 | offset (0.05, -0.8, 0.6). |
| Ground (dropped) | no yaw, no extra scale | lowered by 0.6. |
| Inventory icon | yaw 90 degrees, scale 1.4 | offset (0.3, -0.7, 0.4). |

**Muzzle flash:** the alien-blaster style flash sprite, **tinted cyan-green (0.2, 1.0, 0.7)**, additive blend, scale 1.0, at about (-2.15, -2.45, -0.85) in the gun's flash frame (at the front of the cluster, on the right side of the screen). In the muzzle-particle settings the gun uses a small offset (0, -0.1, 0.1).

## 7. Motion

- The **model has no moving parts.** The ring does not spin, the prongs do not open, the core does not pulse. All motion is whole-gun transforms. The blue disc is a static, fully-lit texture region.
- **Recoil (third person):** about -15 degrees pitch in the first quarter of the first half of the animation, around a point about 0.3 forward and 0.5 up, then settles.
- **Reload (third person):** about 35 degrees pitch at the middle of the animation, around the same pivot.
- **Optional (not in the original):** the builder may keep the ring, prongs and disc as separate named parts so later code can spin or pulse them, but the rest pose must match this document.

## 8. What to avoid

- **No flat single barrel.** The front is a ring of twelve rods around a core, not one tube. Do not simplify to four rods; the "flower" is the identity.
- **No dark metal.** The gun is pale gray with a black grip only.
- **No extra glow.** Only the disc is blue, and the orange dot is a tiny 1x1 face.
- **No symmetric side button.** There is one button, on the left only.
- **Do not thicken the paper-thin plates** under the laser rod.
- **Do not mirror the geometry for the akimbo version** (see the next section).
- Do not add a scope, a magazine, a slide, or sights. The original has none.

## Akimbo (two instances, mirrored)

- The Akimbo item uses **exactly the same model and texture** as the single De-Atomizer. The renderer I read **does not mirror the geometry**: there is no negative scaling. The "left" gun is a **second, identical, un-mirrored copy** moved sideways. The model's one asymmetric detail (the button, on the -Z side) therefore appears on the same side of both guns.
- **Mirroring that does exist:** only the *muzzle flash* is mirrored between the two guns. The right gun's flash sits at about X -2.15 and the left gun's at about X +2.55 (same Y -2.45, Z -0.85, scale 1.0), i.e. roughly symmetric about the center of the pair, each at its own gun's front.
- **First person:** both guns use the same pose as the single gun (yaw 225 degrees, scale 2.5, idle shift (-0.75, -0.45, -0.8)); the left gun is additionally moved **2.05 units of the hand-frame sideways** (along the gun's own width axis) after the scale is set, so the two guns sit side by side about two gun widths apart.
- **Third person (held):** same pose as the single gun but shifted to (0.05, -0.8, 0.7); no per-gun offset there.
- **Ground and icon:** the left copy is nudged (0.2, 0.2, 0) on the ground, and drawn smaller (0.9) with offset (0.3, 0.4, -0.6) plus a (0, 0, -0.5) shift in the icon; the icon shows two guns side by side.
- **Firing:** the guns alternate. Only the gun that fires animates its recoil (-12 degrees pitch around the same pivot); the other stays still. Reload tilts both (35 degrees).
- **Worn off-hand slot:** when the akimbo item is carried in the additional slot, one copy of the model is drawn on the player's left hand at scale 0.55, at about (0.1, -0.7, -1.7), with 35-degree reload and -15-degree recoil tilts.
- **Builder advice:** model one gun. The akimbo item draws it twice; **do not build a left-handed, mirrored variant** unless you also flip the button (which the original does not).

## 9. Acceptance checklist

- [ ] Length : height : width within about 10 percent of 5 : 3.2 : 1.
- [ ] The front cluster is about 48 percent of the length (core and ring 6 long, plus a 1-long cap plane, the 1-long blue disc and the 2-long prongs).
- [ ] Twelve rods of 1x1 on a ring of about 3.6 across around a 2x2 white core, each 6 long, each capped with a 1x1x1 cube.
- [ ] A 2x2x1 **blue** disc (#28A8FF, bright center) at the very front of the core, 1 in front of the cap plane.
- [ ] Four 1x1x2 white prongs tilted about 23 degrees inward.
- [ ] Housing is 9 long, 3 tall, 2 to 3 wide, wrapped in six thin strips (two flat, four at 28 degrees).
- [ ] Grip is black, 2 wide, tilted 12 degrees back, with one light stripe.
- [ ] Trigger guard is a thin open loop (1 wide), with a front post and a 3-long bottom bar; a small trigger tilted about 31 degrees.
- [ ] A 1.4-wide diamond laser rod on top, 7 long, with an orange front face, held up by two paper-thin plates and a small post.
- [ ] One small diamond button on the left side only.
- [ ] Metal is pale gray (#A7A7A7 to #C5C5C5), not dark.
- [ ] The only saturated colors are the blue disc, the orange dot and one tiny green pixel.
- [ ] Muzzle flash is cyan-green and additive.
- [ ] Akimbo is two identical un-mirrored copies; only the flash positions are mirrored.
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Ring rod positions.** The twelve rods are placed by pivots and small rotations; I worked out their centers (about 1.35 from the axis, 30 degrees apart) by calculation, not by a render. The exact tangent orientation of the eight rotated rods may be off by a few degrees; the ring should simply read as a clean circle of squares.
- **Akimbo "mirroring".** The brief called the pair mirrored; the code I read shows a plain translation plus mirrored flash positions. If the original game view looked mirrored, that is likely the viewing angle (both guns turned 225 degrees) and the button's side, not a flipped model.
- **Akimbo spacing.** The sideways offset is given in the hand frame after the scale; it is large (2.05 at scale 2.5). Treat it as approximate and tune by eye.
- **Green pixel.** Only one pixel is green; it is very small and may not be visible in game. Optional.
- **Camo variants:** none. The original has a single texture, shared by the single and akimbo items.
