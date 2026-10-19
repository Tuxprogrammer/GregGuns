# Scatter Laser Rifle: clean-room model notes

Design notes for rebuilding the Techguns **Scatter Laser Rifle** (scatter beam gun) look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

This gun has **its own geometry**. It is **not** the Laser Rifle (that one uses a different model and texture); do not reuse or adapt the Laser Rifle's model.

Related facts: [gun-models](../gun-models.md), [rendering framework](../rendering-framework.md), [Techguns gun stats](../../02-guns/gun-catalog.md).

**Units and axes used in this document.** One unit = one texel at the native 128x128 scale = 1/16 block. Muzzle toward **+X**. **X = 0 is the rearmost face (the butt plate)**. **Y points down** (Minecraft model convention), with **Y = 0 at the top face of the top plate** that covers the receiver. **Z = 0 is the gun's mid-plane**; widths are total widths, centered. **+Z is the battery side** (the side carrying the battery kit and its glowing rods); **-Z is the clean side**. Rounded to the nearest half unit unless it matters. Parts called "diamond" are square bars turned 45 degrees about the gun's long axis.

## 1. Gestalt

A big, **dark-gray, boxy sci-fi energy rifle** with a skeletal stock: a long two-stage body (a wide receiver and a slightly narrower front box), a **chamfered stepped muzzle nose** with a hood, a **light-gray diamond pipe slung under the front** and propped by a slanted strut, a **holographic sight frame** on top, a **battery pack with glowing yellow-green rods** on one side, and a **stock built from a thick dark block with light-gray spine panels, a diagonal brace and a tall butt plate**. Orange indicator bars glow on the sides of the receiver.

Defining features in order of visual importance:

1. **Skeletal stock**: a 3-wide dark block on top, three stepped light-gray spine panels hanging below it, a long diagonal brace and a tall 10-unit butt plate. The open triangle under the stock is the silhouette signature.
2. **Chamfered muzzle nose**: the front box ends in a 3-long hood on top and a **45-degree slanted plate** dropping back to the bottom front edge, with a small stair of blocks behind it.
3. **Light-gray diamond pipe** under the front box (2.8 across, 16 long), with a square cap and a slanted strut to the muzzle nose.
4. **Holographic sight**: a flat base with two thin posts, 3 units tall, and a **transparent window with a single red dot**.
5. **Battery kit on the +Z side**: a light-gray plate with three glowing yellow-green rods (visible only as slivers) and two end caps; plus **orange bar lights** on the receiver sides.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt plate to muzzle hood) | 63 (X 0 to 63) |
| Total height (sight posts to grip bottom) | about 21.5 (Y -4.5 to 17) |
| Receiver body width | 5 (Z -2.5 to +2.5); front box 4; stock 3 |
| Total width including the battery kit | about 6.5 (Z -2.5 to +3.5) |
| Length : height : width | about 12.5 : 4.3 : 1 |
| Receiver | 27 long (X 14 to 41), 5 tall (Y 1 to 6) |
| Front box | 19 long (X 41 to 60), 4 wide, 4 tall (Y 1 to 5) |
| Stock block | 15 long (X 0 to 15), 3 wide, 4 tall (Y 1 to 5) |

Along the length, from the rear: the butt plate (X 0 to 2); the stock (X 0 to 15); the receiver (X 14 to 41) with the sight above its rear part (X 24 to 27) and the grip and guard under it; the front box (X 41 to 60); then the muzzle nose out to X 63.

The **top line** is almost flat at Y 0 to 1 from the stock's front to the muzzle hood; only the top rail (1 higher) and the sight rise above it.

## 3. Part by part

All parts are boxes; widths are centered on the mid-plane unless a Z range is given.

### Receiver (main body)
- **Main block:** 27 long, 5 wide, 5 tall (X 14 to 41, Y 1 to 6, Z -2.5 to +2.5).
- **Top plate:** 4 wide, 1 thick, 27 long (X 15 to 42, Y 0 to 1, Z -2 to +2), covering the top; it sticks out 1 past the main block's front face.
- **Top rail:** 2 wide, 1 tall, 12 long (X 23 to 35, Y -1 to 0), centered, on the top plate.
- **Lower body:** 3 wide, 3 tall, 15 long (X 18 to 33, Y 6 to 9, Z -1.5 to +1.5) under the receiver's middle. It houses the trigger and grip.
- **Side plates (front pair):** two 1-thick, 3-tall, 6-long plates on the sides at the receiver/front-box joint (X 41 to 47, Y 1.5 to 4.5), outer faces flush with the receiver's width (Z +/-2.5 to +/-1.5 at the joint). They **toe in about 5 degrees** toward the muzzle, so their front ends sit about 0.5 closer to the center. They are a step in width from the receiver (5) to the front box (4).
- **Side plates (rear pair):** two 1-thick, 3-tall, 6-long plates at the receiver's rear (X 8 to 14, Y 1.5 to 4.5), starting at the receiver's full width and **converging about 10 degrees** toward the rear so they bury into the stock block's sides. A funnel from receiver width (5) to stock width (3).
- **Orange bar lights:** on **both** side faces of the receiver, three vertical bars, each 1 wide and 3 tall (Y 2 to 5), spaced 2 apart, in the front third of the receiver (centers about 1.5, 3.5 and 5.5 units behind the receiver's front face; X about 35 to 40), color #FF3C00. They are flat paint on the faces, with no geometry.

### Front box
- 4 wide, 4 tall, 19 long (X 41 to 60, Y 1 to 5, Z -2 to +2). A plain slab-sided box, narrower and 1 shorter than the receiver at the bottom (its bottom is at Y 5, the receiver's at Y 6).
- **Top ridges:** two 1x1 strips (Z -1.5 to -0.5 and +0.5 to +1.5), 6 long (X 42 to 48), sloping about 10 degrees: at their rear end (X 42) they sit on the top (Y 0 to 1, flush with the top plate) and at their front end (X 48) they dip into the box top (Y 1 to 2), so they fade out into the box.
- **Handguard:** 3 wide, 4 tall, 8 long (X 35 to 43, Y 5 to 9, Z -1.5 to +1.5) under the joint of the receiver and the front box. A chunky block that surrounds the pipe's rear part.

### Pipe (under the front)
- A 2x2 bar turned 45 degrees: **diamond 2.8 across, 16 long** (X 33 to 49), center at Y 6.9 (so Y 5.5 to 8.3), Z 0. It runs through the handguard and sticks out in front of it (X 43 to 49), 3.3 below the front box's bottom. Light gray.
- **Pipe cap:** a 2x2x2 square block (X 49 to 51, Y 6 to 8, Z -1 to +1), directly at the pipe's front end. Dark.
- **Slanted strut:** a 2-wide, 5-long bar tilted about **36.5 degrees below horizontal** going rearward: its front-top end is at about (X 55, Y 4 to 5), where it enters the front box's bottom, and its rear-bottom end is at about (X 49 to 51, Y 7.5 to 8), meeting the pipe cap. It braces the pipe to the front box. Dark.

### Muzzle nose
The front box ends at X 60; the nose is built on it:
- **Hood:** 4 wide, 1 tall, 3 long (X 60 to 63, Y 1 to 2) on top of the front box's front, an overhang that is the front-most part of the gun.
- **Slanted plate:** 4 wide, about 1 thick, 5.7 across the slant, tilted **45 degrees** so its top edge is at the hood's front tip (X 63, Y 2) and its bottom edge at (X 60.2, Y 4.8). It closes off the front box's face like a chamfer.
- **Stair blocks behind the slant:** three 4-wide blocks in a diagonal stair under the hood: 1x1 at (X 60.8 to 61.8, Y 2 to 3), 1x2 at (X 59.8 to 60.8, Y 2 to 4), 1x1 at (X 59.2 to 60.2, Y 4 to 5). They fill the space under the slanted plate; from outside you see a stepped chamfer with the plate on top of it.

### Stock
- **Stock block:** 3 wide, 4 tall, 15 long (X 0 to 15, Y 1 to 5, Z -1.5 to +1.5). Its top is flush with the receiver top (Y 1).
- **Top cover:** 3 wide, 1 thick, 6 long (X 9 to 15, Y 0 to 1), sloping about 10 degrees so its rear end dips into the stock top. A cheek-piece ramp.
- **Light spine panels:** three 1-thick (Z -0.5 to +0.5) light-gray panels hanging under the stock block, stepping lower toward the rear:
  - X 9 to 12: Y 5 to 10 (5 tall),
  - X 7 to 9: Y 5 to 12 (7 tall),
  - X 2 to 7: Y 5 to 13 (8 tall).
  Each carries a few small cut-out squares (transparent). They are the **lightest large areas** on the stock.
- **Front stock block:** 2 wide, 4 tall, 2 long (X 12 to 14, Y 5 to 9, Z -1 to +1) under the stock's front, behind the lower body.
- **Bottom bar:** 2 wide, 2 tall, 4 long (X 2 to 6, Y 12 to 14, Z -1 to +1) at the spine's bottom.
- **Butt plate:** 2 wide, 10 tall, 2 thick (X 0 to 2, Y 4 to 14, Z -1 to +1) at the very back, running from just above the stock's lower edge down past the spine's bottom.
- **Diagonal brace:** 2 wide, 15 long, about 2 thick, running from under the lower body's rear (X 19, Y 7) **down and back at about 27.5 degrees below horizontal** to the bottom bar (X 5, Y 13.5). It passes behind the grip and crosses the spine panels. Together with the stock block and the butt plate it makes an **open triangular window** under the stock.

### Grip and trigger group
- **Grip:** 3 wide, 9 long, 4 deep (Z -1.5 to +1.5), hinged at its top front corner (X 22, Y 9) and **tilted about 25.6 degrees** so its bottom swings back. Corners: top front (22, 9), top rear (18.4, 7.3), bottom front (18.1, 17), bottom rear (14.5, 15.4). Its top tucks under the lower body.
- **Guard front slope:** a 2-wide, 1-thick, 5-long plate hinged at the lower body's front-bottom (X 29, Y 9) and sloping **down and back about 36 degrees** to (X 24.5 to 25, Y 12). Together with the floor bar it makes a V-shaped guard front.
- **Guard floor:** 2 wide, 1 tall, 5 long (X 20 to 25, Y 11 to 12, Z -1 to +1), overlapping the grip's front face by about 1.
- **Trigger:** 1x2x1 (Z -0.5 to +0.5) at X 22.5 to 23.5, Y 8.5 to 10.5, tilted about 15 degrees so its bottom leans forward.

### Holographic sight (on top)
- **Base:** 4 wide, 1 tall, 3 long (X 24 to 27, Y -1.5 to -0.5, Z -2 to +2), sitting on the top rail (it overlaps the rail by 0.5).
- **Posts:** two vertical 1x4x1 posts at **X 25 to 26**, Y -4.5 to -0.5, one on each side (Z -2.5 to -1.5 and +1.5 to +2.5), so each overhangs the base's edge by 0.5.
- **Window:** a **zero-thickness 3x3 plane** at X 25.5 (Y -4.5 to -1.5, Z -1.5 to +1.5) between the posts. It is **transparent**, with a **single red (#FF0000) pixel** at its center. It is only visible edge-on as a thin line, so the sight reads as a **frame**: two posts and a base.
- Base and posts are dark (#353535 to #3C3C3C).

### Battery kit (+Z side only)
- **Plate:** 1 wide, 3 tall, 4 long (X 25.3 to 29.3, Y 2 to 5, Z +2.5 to +3.5), flush against the receiver's +Z face. **Light gray #9A9A9A**, with a few tiny yellow flecks.
- **Top and bottom caps:** two 2-wide, 1-tall, 4-long blocks (X 25.3 to 29.3, Z +1 to +3): one on top (Y 0.5 to 1.5), one at the bottom (Y 5.5 to 6.5). Lighter gray than the body (#5E5E5E to #626262).
- **Three glowing rods:** vertical 1x4x1 rods turned 45 degrees (diamond 1.4 across), centered at Z +2.5, one every 1 unit along X (centers at about X 28.3, 27.3 and 26.3), Y 1.5 to 5.5. Their middle sections are behind the plate (Y 2 to 5); only a 0.5-tall sliver shows above and below the plate, between the caps. **Yellow-green (#B0BA5E)**.
- **Mount foot:** 1 wide, 3 tall, 3 long (Y 1.9 to 4.9), on the +Z face near the receiver's front (X about 29 to 32), angled about 20 degrees so its front end leans toward the center. Gray #5E5E5E.

## 4. Silhouette checks

- **Side (from the clean -Z side):** a long gun with an almost flat top. From the rear: a tall **butt plate** (10 tall) at the very back; the stock's block, the spine panels stepping down under it and the **diagonal brace** crossing the open triangle; the receiver, with the little sight frame and rail on top and the grip with its guard under it; the front box with a pipe hanging under it, a thin strut up to the muzzle, and a **chamfered nose with an overhanging hood**. Negative space: the triangle under the stock, the pocket between the grip and the diagonal brace, the gap under the front box beside the pipe.
- **Side (+Z side):** the same, with the light battery plate and the end caps on the receiver's middle, the orange bars visible on the clean side only if you add them to both sides (they are on both).
- **Top:** a very long narrow spine (5 wide receiver, 4 wide front box, 3 wide stock) with the sight's base and two posts as a small cross shape and the battery plate sticking out 1 on the +Z side. The nose hood is 3 long at the front.
- **Front:** a 4x4 box with the hood and slanted face, the diamond pipe below it, the handguard and the receiver behind it wider (5) with its shoulders; the sight posts above the center.
- **Back:** a tall narrow butt plate (2 wide, 10 tall) in front of the 3-wide stock block and the 5-wide receiver behind; the sight frame above.
- **Underside:** the lower body, handguard, pipe and strut in a line; the grip and guard under the rear; the spine panels and brace under the stock.

## 5. Materials and palette

The original texture (128x128) is **mid-to-dark gray with a few light-gray parts**, one set of orange lights and a yellow-green battery. Values are measured.

| Part | Base | Highlight | Shadow / seam |
|---|---|---|---|
| Receiver main block, front box, top plate, lower body | #4D4D4D | #565656 | #3C3C3C |
| Handguard, front ridges, side plates (front pair) | #4F4F4F to #545454 | #585858 | #414141 |
| Muzzle nose (hood, slant, stairs) | #4D4D4D to #525252 | #595959 | #474747 |
| Grip, trigger, guard (front slope and floor) | #4B4B4B to #535353 | #585858 | #393939 |
| Stock block, top cover, bottom bar, butt plate, front stock block, diagonal brace | #353535 to #3D3D3D | #424242 | #303030 |
| Rear side plates | #3A3A3A | #424242 | #353535 |
| **Spine panels (light)** | #9C9C9C to #9F9F9F | #A8A8A8 | #969696 |
| **Pipe (light)** | #999999 | #A4A4A4 | #939393 |
| Pipe cap and strut | #4F4F4F | #565656 | #4C4C4C |
| Top rail | #484848 (top), #555555 (front) | #575757 | #3B3B3B |
| Sight base and posts | #363636 | #3E3E3E | #323232 |
| Battery plate | #9A9A9A | #9F9F9F | #969696 |
| Battery caps and mount foot | #5E5E5E | #646464 | #5A5A5A |
| Battery rods | #B0BA5E | #B5BF62 | #AEB85C |
| **Orange bar lights** | #FF3C00 (flat, 1x3) | | |
| Sight window | transparent; red dot #FF0000 | | |

Shading style:
- **Flat, matte, mid-dark gray** with 2 to 4 levels of noise, like brushed polymer. The front half is **lighter** (#4D to #55), the stock is **darker** (#35 to #3D), and the pipe, the spine panels and the battery plate are the **light accents** (#99 to #9F).
- A few **thin darker slits** (short horizontal vent lines, 1 unit high) are painted on the receiver faces near the rear and on the front box; they are subtle.
- The only saturated colors are the **orange bars** (receiver sides), the **yellow-green rods** and the **red dot**. They are flat, not glowing; any glow comes from lighting effects.

## 6. In-game presentation

All transforms apply to the whole model after its 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up). One part; no scope overlay.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw 225 degrees, scale 1.4 | idle shift (-0.55, -0.35, -0.65). |
| First person firing | pitch kick of up to about 5 degrees, plus a pushback of up to 0.15 | peaks at mid-animation. |
| First person reloading | about 50 degrees pitch at mid-animation | smooth rise and fall. |
| First person aimed | extra offset (0, 0.05, 0.2) | no zoom overlay. |
| Third person (held) | yaw 135 degrees, scale 0.8 | offset (0.1, -0.5, 0.9). |
| Ground (dropped) | no yaw, scale 0.6 | lowered by 0.45. |
| Inventory icon | yaw 90 degrees, scale 0.48 | offset (0.3, 0, 0). |

**Muzzle flash:** a laser-style flash sprite, scale 1.5, at about (-1.05, -2.8, -0.45) in the flash frame (in front of the nose, slightly to the clean side), additive blend. The sight has **no scope overlay**; the holo frame is decorative.

## 7. Motion

- The **model has no moving parts.** The rods do not pulse, the pipe does not move. All motion is whole-gun transforms.
- **Recoil (third person):** about -15 degrees pitch in the first quarter of the first half of the animation.
- **Reload (third person):** about 30 degrees pitch and 40 degrees yaw at mid-animation, with a small offset (0, -0.2, 0.05), then back.
- **Optional:** the builder may keep the battery rods, sight and pipe as separate named parts for later animation, but the rest pose must match this document.

## 8. What to avoid

- **Do not reuse the Laser Rifle model.** Different gun, different shapes.
- **Do not make the stock a solid block.** It must have the light-gray spine panels, the diagonal brace and the open triangle under it.
- **Do not make the muzzle plain.** It needs the hood, the 45-degree slanted plate and the stair.
- **Do not put the battery kit on both sides.** It is on +Z only (the orange bars are on both).
- **Do not make the pipe round or dark.** It is a light-gray diamond.
- **Do not make the holo window opaque.** It is transparent with one red dot.
- Do not add a scope, a magazine or a bipod. The original has none.

## 9. Acceptance checklist

- [ ] Length 63, height about 21.5, receiver width 5; proportions within 10 percent of 12.5 : 4.3 : 1 (without the battery kit).
- [ ] Receiver (27 long, 5x5) plus a 19-long, 4x4 front box, plus a 3-wide, 15-long stock block.
- [ ] Muzzle nose: a 3-long hood, a 45-degree slanted plate and a three-block stair.
- [ ] A light-gray diamond pipe (2.8 across, 16 long) under the front, with a square cap and a slanted strut of about 36 degrees.
- [ ] A handguard (3x4x8) under the receiver/front joint.
- [ ] Stock: three stepped light-gray spine panels (5, 7 and 8 tall), a 15-long diagonal brace at about 27.5 degrees, a 2x10x2 butt plate and a 2x2x4 bottom bar.
- [ ] Grip tilted about 25.6 degrees, 3 wide, 9 long; open guard with a sloped front and a floor; a small tilted trigger.
- [ ] Holographic sight: base, two posts of 4 high, a transparent window with a red dot; top rail beneath it.
- [ ] Battery kit on +Z only: light plate, two caps and three yellow-green rods showing as slivers.
- [ ] Three orange 1x3 bars on each side of the receiver's front third.
- [ ] Color: mid-dark gray body (#4D4D4D), darker stock (#353535 to #3D3D3D), light accents (#999999 to #9F9F9F).
- [ ] No moving parts; no scope overlay.
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Orange bar positions.** They are measured on a low-resolution texture and mirrored faces, so the exact spacing (1.5, 3.5, 5.5 from the front) and which face's bars sit 1 unit further forward could be off by 1. Keep the pitch at 2 and the three-bar group in the front third.
- **Which side the battery kit shows on in first person.** The kit is on the model's +X side. By the transforms, the camera likely sees the clean side, but this is from formulas, not from the game.
- **The rods behind the plate.** The three rods are almost entirely hidden by the gray plate; only slivers show above and below. A builder may expose them more, but the original barely does.
- **Diagonal brace.** Its pivot and tilt are exact, but the way it intersects the grip and the spine panels is hard to see without a render; make sure it joins the lower body at the top and the bottom bar at the bottom.
- **Muzzle stair.** The three stair blocks are almost completely covered by the slanted plate; their details matter little.
- **Camo variants:** none. The original has a single texture.
