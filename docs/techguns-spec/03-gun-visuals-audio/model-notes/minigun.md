# Minigun: clean-room model notes

Design notes for rebuilding the Techguns **Minigun** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelMinigun](../gun-models.md#modelminigun-128128-family-x-73-boxes-techne-2-parts), [minigun sheet](../gun-sheets.md#minigun-minigun), [rendering framework](../rendering-framework.md), [Techguns minigun stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = 1/16 block = one texel of the 128x128 texture. Muzzle toward **+X**. **Y points down** (Minecraft model convention), so "up" is **negative Y**. **The origin sits on the barrel-cluster axis, at the middle of the housing's height and width:** Y = 0 and Z = 0 are the spin axis of the barrels, and are also the centerline of the gearbox body. X = 0 is a point on that axis 3 units behind the gearbox's front face. **-Z is the shooter's right-hand side; +Z is the shooter's left.** Sizes across Z are total widths. Rounded to the nearest half unit unless it matters. Every dimension below is a "units" value.

**The model has two parts:** a static **housing** (everything except the spinning barrels) and a **rotor** (the barrel cluster). The rotor is its own named part. Build it that way.

## 1. Gestalt

A big, grim, **six-barrel rotary cannon** on a gearbox, in the style of a vehicle-mounted or door-gunner minigun. It has no stock, no pistol grip and no trigger: it is held by a **flat trapezoid loop handle** at the back and a **carry handle** on top. At a glance you read: a **long cluster of six thin silver barrels** that is more than half the gun; a fat round charcoal-to-mid-gray **gearbox**; a **big pale, drum-shaped ammo box** hanging below the shooter's-right side; a **diamond-shaped drive housing** on the other side; a **dark Y-shaped frame** over the top that ends in a **small carry handle**; and a dark open loop at the rear. The attitude is "heavy mounted weapon you wrestle with two hands", clearly bigger than every other gun in the set.

Defining features in order of visual importance:

1. **The barrel cluster:** six 2x2 barrels in a ring, 21 long, with 12-long thin tips beyond, held together by three near-black octagon clamp discs. It is the only rotating part.
2. **The ammo drum:** an almost white, octagon-section drum under the right side of the gearbox. It is the single brightest object and the tallest thing below the axis.
3. **Round gearbox:** a long stepped-octagon body (reads round from front and back), mid-gray.
4. **The dark frame:** a thin charcoal frame (V struts, top plate, rear handle loop, yoke ring at the front of the gearbox, carry handle) that makes the gun look "caged".
5. **Light-versus-dark layering:** charcoal frame, mid-gray gearbox, silver barrels, near-white drum, black clamps and end caps.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (rear end post to tip ends) | **62** (X -24 to 38) |
| Total height (carry-handle top to drum bottom) | **21.5** (Y -11 to 10.5); the axis is almost exactly mid-height (11 above, 10.5 below) |
| Total width (drum outer plate to feed-housing tip) | **23** (Z -13.5 to 9.5) |
| Width without the drum and side housing | 10 (gearbox) to 14 (yoke top bar) |
| Length : height : width | about 2.9 : 1 : 1.07 |
| Rotor length (hub to tip ends) | 35 (X 3 to 38), which is 56 percent of the total |
| Barrels (the thick section) | 21 long (X 5 to 26) |
| Muzzle tips beyond the barrels | 12 long (X 26 to 38) |
| Gearbox | 16 long (X -13 to 3), 10 tall, 10 wide |
| Rear structure (handle loop) | X -24 to -13 (11 long) |
| Cluster diameter | about 7.3 (outer radius 3.64); clamp discs 8 across (hub and middle) and 7 across (front) |
| Ammo drum | 14 long at its core, about 12 at its outer skin; 11 tall; 10 wide |

How the pieces sit along the length, from the rear: the end post and loop handle (X -24 to -13); the gearbox (X -13 to 3); the yoke ring and carry handle (X 4 to 7); then 31 units of bare barrel and tip beyond the yoke. The drum sits **under the gearbox's back-to-front span** (X -11 to 3), hanging on the right side.

The **spin axis is the long axis of the gun**. The gearbox, hub, yoke opening and the center of the front clamp are all concentric on it.

## 3. Part by part

All positions are in units from the origin described above. "Touches" means faces in contact or overlapping.

### Gearbox (housing core)
- A long stepped-octagon body. Core box **16 long (X -13 to 3), 6 tall (Y -3 to 3), 10 wide (Z -5 to 5)**.
- Two **skin layers** on top and two on the bottom make the section round: a 16-long, **8-wide**, 1-tall layer above and below the core (Y -4 to -3 and 3 to 4, Z -4 to 4), then a 16-long, **6-wide**, 1-tall layer above and below that (Y -5 to -4 and 4 to 5, Z -3 to 3).
- So the cross-section is a stepped octagon, **10 tall and 10 wide**, widths 6, 8, 10, 8, 6 from the top down. From the front and back it reads as a round barrel-shaped block.
- Mid-gray. The front face (X = 3) carries the rotor hub; the rear face (X = -13) is dark and carries the handle post.
- Touches: the rotor hub (front face), the yoke ring (ahead of it), the handle post (rear), the drive housing (left side), the ammo drum (right side), the side pins (both sides), the top bridge block (top).

### Rotor hub (part of the rotor)
- A stepped-octagon disc **2 long (X 3 to 5), 8 across**. Section widths: 4 wide at the top and bottom edges (Y ±4, Z -2 to 2), 6 wide at the next steps (Y ±3, Z ±3), 8 wide through the middle four rows (Z ±4, Y ±2). Near-black. It rests against the gearbox's front face and rotates with the cluster.

### Barrels (part of the rotor)
- **Six** barrels, each a plain square bar **2x2 section, 21 long (X 5 to 26)**. Flat, with no taper, no muzzle crown, no cooling fins and no shroud.
- Evenly spaced every **60 degrees** on a circle of radius **2.5** around the spin axis (barrel center to axis). The barrel's inner face is 1.5 from the axis and its outer face 3.5. Each barrel keeps one face radial: the outer face is perpendicular to the radius.
- **Rest pose (angle zero):** one barrel is directly above the axis (center at Y -2.5, Z 0) and one directly below it (Y 2.5). The other four sit at about 60 degrees to the vertical: centers at about (Y ∓1.25, Z ±2.2). No barrel is at 3 o'clock or 9 o'clock. From the front you see a ring with a barrel at 12 and 6 o'clock.
- The six barrels are **touching or nearly touching** at their shoulders; the corner gaps are small slivers. The cluster has a hollow center of radius 1.5 with no central shaft, but the three clamp discs fill that hollow where they cross it (see below).
- **Two-tone barrels:** the rear segment (X 5 to 16) is warm medium gray; the front segment (X 18 to 26) is much lighter, nearly white-silver; a very dark 1-unit seam hides under the middle clamp at X 17.
- Touches: the hub (rear end), the middle clamp (X 17), and the front ends of the barrels feed straight into the tips at X 26.

### Muzzle tips (part of the rotor)
- Six thin bars **1x1 section, 12 long (X 26 to 38)**, one per barrel, centered on the barrel's own centerline (radius 2.5, same angles). Each is therefore half the barrel's width and sits in the middle of the barrel's end face.
- They end flat. The front clamp holds them at X 35 to 36, **2 units short of their ends**.
- Light silver with a darker root.

### Rotor clamp discs (part of the rotor)
Three flat stepped-octagon plates, all **near-black**, perpendicular to the axis. They look like round discs. The barrels pass through them (they cover the center and the gaps between barrels).
| Disc | X range | Thickness | Across | Notes |
|---|---|---|---|---|
| Rear hub | 3 to 5 | 2 | 8 | described above |
| Middle clamp | 17 to 18 | 1 | 8 | same shape as the hub; the dark seam on the barrels sits under it |
| Front clamp | 35 to 36 | 1 | 7 | smaller; holds the 12-long tips together. Top and bottom flats are 3 wide (Y ±3.5); the side flats are 3 tall (Z ±3.5); the corners are stepped by 0.5 |

### Yoke ring (housing, ahead of the gearbox)
A squarish frame that surrounds the rotor's hub and the root of the barrels, **X 4 to 7**, in dark gray (not as dark as the loop handle).
- **Top bar:** 3 long x 2 tall x **14 wide**, Y -7.5 to -5.5, Z -7 to 7.
- **Side plates (two):** thin vertical slabs **2 long x 6 tall x 1 thick**, X 4.5 to 6.5, Y -3.5 to 2.5, Z -6.5 to -5.5 and 5.5 to 6.5.
- **Top corner blocks:** a 3 x 2 x 2 block on the right (Z -7 to -5, Y -5.5 to -3.5) and a 3 x 3 x 2 on the left (Z 5 to 7, Y -5.5 to -2.5) join the top bar to the side plates.
- **Bottom bar:** 2 long x 1 tall x **8 wide**, X 4.5 to 6.5, Y 4.5 to 5.5, Z -4 to 4.
- **Bottom gussets (two):** 2 long, 4-tall and 1-thick plates tilted **45 degrees** about the barrel axis, joining the side plates' lower ends to the bottom bar.
- The opening in the middle is about 11 wide and 10 tall, so the 8-across rotor discs and the 7.3 cluster spin freely with about 1.5 clearance. The yoke **does not touch** the rotor.
- The yoke touches the carry handle (top), the V struts (top bar's rear), and the side pins (their front tips at X 4).

### Top plate and V struts (housing)
- **Top plate:** 13 long x 1 tall x 2 wide, X -13 to 0, **Y -7 to -6**, centered. It floats 1 above the gearbox's top (Y -5) and is joined at its rear by the handle's upright post and at its front by a small **bridge block** (2 long x 1 tall x 2 wide, X -2 to 0, Y -6 to -5) that sits on the gearbox.
- **V struts (two):** each 8 long x 1 tall x 2 wide, lying flat at Y -7 to -6. They start at the front end of the top plate (X -2) and fan out **55 degrees** off the barrel axis, one to each side, ending at the yoke's top-bar corners (about X 5.7, Z ±5.6). Seen from above, with the plate, they form a **Y or wishbone**: a stem going back to the handle and two arms reaching the yoke.

### Carry handle (housing)
- A small bridge on top of the yoke: **top bar 2 long x 1 tall x 7 wide** (X 4.5 to 6.5, Y -11 to -10, Z -3.5 to 3.5) and **two legs** 2 long x 3 tall x 1 thick at Z -4.5 to -3.5 and 3.5 to 4.5 (Y -10.5 to -7.5). The legs land on the yoke top bar (Y -7.5).
- Overall 9 wide (outer legs), 2 long, and 3.5 above the yoke. It is the **highest point of the gun** (Y -11).

### Rear handle loop (housing)
A flat **trapezoid loop**, 2 wide (Z ±1) throughout and centered on the mid-plane:
- **Front post:** 2 long x 11 tall (X -15 to -13, **Y -7 to 4**), butted against the gearbox's rear face.
- **End post:** 2 long x 5 tall (X -24 to -22, Y -4 to 1) at the very rear.
- **Upper bar:** 10 long, 2 tall, from the end post's top corner **rising 15 degrees** toward the front post: it starts at Y -4 to -2 at X -24 and ends at about Y -6.6 to -4.3 near X -14, meeting the top of the front post.
- **Lower bar:** 10 long, 2 tall, from the end post's middle **descending 15 degrees** toward the front post: it starts at Y -1 to 1 at X -24 and ends at about Y 1.4 to 3.6 near X -14, meeting the bottom of the front post.
- The loop is therefore **short (5) at the rear and tall (11) at the housing**, opening into a wedge-shaped hole (about 2 tall at the rear, about 5.5 tall at the front post, 9 long). The side view shows a dark triangle-ish bracket reaching back from the gearbox.
- Everything in the loop is dark charcoal. There is no grip cover, no trigger and no thumb switch.

### Ammo drum (housing, shooter's right side, -Z)
A big, pale, octagon-section drum built from stacked slabs, with its long axis along X. Overall: **X -11 to 3, Y -0.5 to 10.5, Z -13.5 to -3.5** (14 long at the core, 11 tall, 10 wide).
- **Core:** a mid-gray-cream box 14 long x 6 tall x 8 wide (X -11 to 3, Y 2 to 8, Z -12 to -4), with 14-long, 1-tall, 6-wide skins above and below it (Y 1 to 2 and 8 to 9, Z -11 to -5).
- **Outer drum:** a lighter, nearly white box **12 long x 7 tall x 9 wide** (X -10 to 2, Y 1.5 to 8.5, Z -12.5 to -3.5), with 12-long, 1-tall, 7-wide skins (Y 0.5 to 1.5 and 8.5 to 9.5, Z -11.5 to -4.5) and then 12-long, 1-tall, 4-wide caps (Y -0.5 to 0.5 and 9.5 to 10.5, Z -10 to -6).
- **Outside cover plate:** a 12-long x 4-tall x 1-thick plate on the outer face (X -10 to 2, Y 3 to 7, Z -13.5 to -12.5), and an equal plate on the inner face buried in the gearbox.
- Net section (top to bottom): widths 4, 7, 9, 9 (with the 10-wide cover plate in the middle), 9, 7, 4. It reads as a **rounded octagon drum lying along the gun**, not a box.
- **It is not a belt box:** there is no belt, no feed chute and no cable. It simply hangs off the right side of the gearbox, overlapping the gearbox wall at Z -5 to -3.5 by about 1.5.
- It hangs **below the axis** (its top skin at Y -0.5, its bottom at 10.5), so its upper portion overlaps the gearbox's lower right corner. It is the lowest part of the gun.
- Its top and bottom caps are at the same X span as the gearbox's rear-to-front center (X -10 to 2), so the front of the drum ends 1 behind the gearbox's front face.

### Drive housing (housing, shooter's left side, +Z)
A diamond-section motor or feed housing, centered at **Y 1.5, Z 6**, with its edges pointing up, down, left and right (a 45-degree-turned square tube).
- **Outer tube:** 10 long (X -10 to 0), 4x4 square section turned 45 degrees about the barrel axis, so it is about **5.7 across** (extents Y -1.3 to 4.3, Z 3.2 to 8.8).
- **Inner core:** a 12-long (X -11 to 1) tube, 3x3 section turned 45 degrees, **4.2 across**; it pokes 1 unit out of each end of the outer tube and reads as a dark sleeve.
- **Two dark end discs:** each **1 long, 5x5 section turned 45 degrees (7.1 across)** at **X -8.5 to -7.5** and **X -2.5 to -1.5**, near-black. They stand a little proud of the tube (Z up to 9.5), the farthest-out part on the left side.
- The tube's inner corner sinks about 2 units into the gearbox wall (Z 5), so it is attached.
- The drum is on the right; this is its counterweight on the left. It is **much smaller** than the drum.

### Side pins (housing, one each side)
- On each side, a light-gray **diamond bar 4 long (X -2 to 2), 3x3 section turned 45 degrees (about 4.2 across)**, centered at **Y -4.5, Z ±6**. They sit on the upper flanks of the gearbox, outside its wall (the inner point sinks 1 unit into the wall).
- A **rear end cap**: 1 long (X -3 to -2), 2x2 section turned 45 degrees, near-black. A **front end cap**: 2 long (X 2 to 4), 2x2 section turned 45 degrees, near-black. The front cap touches the yoke ring's front plate.
- A small **stub arm** (3 long, 2x2 section) tilts 135 degrees about the X axis from each pin's center toward the gearbox, running inward and slightly downward (to about Y -1.3, Z ±2.8). It ties the pin to the gearbox wall.
- From the front, these appear as a pair of **light diamonds with black centers** at the left and right of the gearbox's top half. They are among the gun's most recognizable details.

## 4. Silhouette checks

- **Side, from the shooter's right (muzzle to the right):** from the left: a dark trapezoid bracket (short at the rear, tall at the gearbox) with a wedge hole; a round gray gearbox; a **big pale drum** filling the lower half of the gearbox and hanging below it; the Y-shaped dark frame above, rising to a small carry handle (a notch of negative space under it, over the yoke); then a **long horizontal rod cluster** with three dark clamp bands (hub, middle, near the end) and a fringe of thin tips beyond the last band. Negative spaces: the wedge hole in the rear loop; the gap between the carry handle's legs; and the gap between the V struts and the gearbox top.
- **Side, from the shooter's left:** the same, but the drum is replaced by the **smaller diamond drive housing** mid-height, flanked by two dark end discs. The gearbox is more exposed and darker. The top silhouette is identical.
- **Top:** a **Y shape**: the narrow stem at the rear (the 2-wide top plate), then two arms fanning forward to the yoke's wide top bar (14 wide), then a thin rod cluster 7.3 wide running out to the front. The drum and the left housing stick out of the gearbox at its sides, with the drum bulging more (to Z -13.5 versus 9.5).
- **Front:** a round dark disc (the front clamp and hub) with a **ring of six barrel squares** (barrels at 12 and 6 o'clock), seen against the roughly 10 x 10 octagon of the gearbox behind. At the upper left and right are the two light diamonds with black centers (side pins), above them the yoke frame and the carry handle as a small "handle" shape on top. Lower-left (shooter's right) is the large pale octagon drum; lower-right is the diamond drive housing with dark centers.
- **Back:** a round dark octagon (the gearbox's rear face), the dark rear loop handle in the middle, the carry handle behind it above, the two light pin diamonds and the drum and drive housing at the lower sides.
- **Underside:** a long thin spine (cluster) in the front; the drum bulging on one side and the drive housing on the other; the yoke's bottom bar; the narrow bracket at the back.

## 5. Materials and palette

The original texture is a flat, neutral, **cool-gray metal palette** with four brightness tiers: charcoal frame, mid-gray gearbox, silver barrels, near-white drum, plus near-black clamps. No colored accents, no paint, no logos. The values below were sampled from its regions.

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Gearbox core sides | mid-gray steel | #999795 | #A7A2A1 | #383836 (darkest rear and end faces) |
| Gearbox skins (the 8-wide layers) | mid-gray steel | #9C9998 | #A5A2A1 | #696766 |
| Gearbox outer skins (the 6-wide layers) | mid-gray steel | #A29F9E | #A5A2A1 | #696765 |
| Ammo drum outer box | white-gray metal | #E4E4E4 (mean #D2D2D2) | #FFFFFF | straps #5A5A5A to #737373 |
| Ammo drum core box | cream-gray | #B6B2B0 | #D3CFCD | #ABA5A2 |
| Ammo drum skins (core layers) | light warm gray | #BCB8B6 | #C9C5C3 | #A6A2A0 |
| Ammo drum cover plate | light gray | #C5C5C5 | #F1F1F1 | #5B5B5B |
| Yoke ring (all pieces) | dark-medium gray | #6A6867 | #706D6D | #5D5B59 |
| Yoke bottom gussets and plates | dark-medium gray | #6E6C6B | #72706E | #686664 |
| Rear loop (posts and bars) | dark charcoal | #474545 | #555352 | #3F3E3D |
| Rear end post | dark charcoal | #424140 | #464444 | #3E3D3D |
| Top plate and V struts | dark charcoal | #444342 | #484746 | #3D3C3B |
| Carry-handle bar | dark gray | #4A4948 | #5D5B5A | #413F3E |
| Carry-handle legs | dark gray | #585555 | #585555 | #31302F |
| Side pins (bodies) | light gray | #CCC8C6 | #CFCBC9 | #96908D |
| Side pins (end caps) | near-black | #272624 | #2B2B2B | #1F1D1D |
| Side pin stub arms | medium gray | #6E6C6B | #6F6C6B | #646261 |
| Drive housing outer tube | light gray | #C3BFBD | #CDC9C7 | #ADA9A7 |
| Drive housing inner sleeve | medium gray | #696766 | #737170 | #656261 |
| Drive housing end discs | near-black | #272424 | #2D2B2B | #21211F |
| Barrels, rear segment (X 5 to 16) | warm gray steel | #A7A09D | #AAA4A0 | #6B6867 at the second column from the root |
| Barrels, front segment (X 18 to 26) | pale silver | #CDCAC9 | #D9D7D6 | #C6C2C0 |
| Barrel seam under the middle clamp, and the dark root ring | near-black | #2E2B29 | | |
| Muzzle tips | silver | #BEB9B7 | #D1CFCE | #9E9996; the very root pixel is black |
| Rotor discs (hub, middle, front) | near-black | #292727 | #2D2D2D | #232221 |

Shading style:
- **Flat and noisy:** each region is a narrow band of 2 to 6 gray levels, painted flat with per-pixel noise. No gradients across faces, no bevels, no rust, no wear, no decals.
- **The barrels are the exception:** they carry a **length-wise ramp**. The rear segment is warm gray; a bright 1-unit band at X 16; a black seam at X 17 (under the clamp); then the front segment is lighter, brightening toward the muzzle. The top and side faces of each barrel share the same ramp.
- **The drum** is the brightest and most neutral thing, nearly white, with **two thin dark strap lines** on each long side face, 1 unit in from each end (X about -9 and 1 on the outer box), flanked by a medium-light edge column. The end faces (the ones facing front and back) are a flat light gray with slight noise.
- **Darker faces on ends:** the gearbox's front and rear end faces are much darker than its sides (down to #383836).
- **Clamps and end caps:** uniformly near-black; they punctuate the silver barrels and make the cluster look banded.
- **No camo variants.** One single texture.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units, applied **before** the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up). Both parts get the same base transform.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 1.5 | idle offset (0.70, -0.30, -0.60): 0.7 block forward along the gun axis, 0.3 up and 0.6 toward the shooter's right. The camera sees the gun's right side at roughly 45 degrees, so the drum is the dominant close object, with the cluster running away from the player. |
| First person firing | roll about Z (the sideways axis) by up to about 1.5 degrees, plus a 0.05-block shove along the gun axis | both ride a sine wave through each 5-tick window. Very small. The rotor spins (section 7). |
| First person reloading | roll 25 degrees, yaw -15 degrees at full effect, with the offset moving from idle (0.70, -0.30, -0.60) to (1.20, -0.05, -0.35) | smooth rise, a flat hold in the middle ~45 percent of the reload, then a smooth return. Reload is 100 ticks (5 s). |
| First person aimed | no scope and no zoom overlay | none |
| Third person (held) | yaw 225 degrees, scale 1.25 | offset (-0.6, 0, 0) along the gun axis, i.e. the gun is shifted back so the hand is roughly at the yoke and carry handle (X about 10, ahead of the gearbox's front). |
| Third person aimed | extra offset (-0.05, -0.8, 0.95) | |
| Third person reloading | tilts about the barrel axis by up to 30 degrees and about the vertical by up to 40 degrees at mid-reload, then returns | |
| Third person recoil | none | no whole-gun recoil in third person; the only motion is the rotor |
| Ground (dropped) | yaw 90 degrees, scale 0.8 | shifted 0.6 block along the model's -Y (up) |
| Inventory icon | yaw 180 degrees, scale 0.5 | offset (0.25, 0.55, -0.6); drawn side-on, filling most of the slot. There is no sprite; the icon is the 3D model. |

**Muzzle flash point:** the front of the barrel cluster, X about 38, on the axis, with the flash drawn about 1.65 times scale and shown twice per firing window (the 8-frame flash plays two times per 5 ticks). In third person the flash sits slightly forward of the hand at roughly (0, -0.8, 0.4).

**Akimbo:** none. One gun, two hands.

**Fire cadence:** one shot per tick while the trigger is held; recoil window 5 ticks (250 ms).

## 7. Motion

The model has **two parts, and exactly one thing moves: the rotor.** Everything else is static; whole-gun transforms are the only other motion.

### The rotor (barrel cluster) as its own part
- **Contents:** all six barrels, all six muzzle tips and all three clamp discs (hub, middle, front). Nothing else. The gearbox, yoke and every piece of the frame belong to the housing and never turn.
- **Spin axis:** the **gun's long axis, X**, through **Y = 0, Z = 0**. That is the center of the cluster, the center of the gearbox and the center of the yoke opening. Every point of the rotor except the axis itself moves on a circle around it. No wobble and no offset: the pivot is the axis, whatever the position along X.
- **Direction:** positive rotation about +X (muzzle-forward). Seen **from behind the gun, looking toward the muzzle, it turns clockwise** (the barrel at 6 o'clock moves toward the shooter's left). See section 10 for how sure this is.
- **Rest pose:** angle 0, with a barrel directly above and one directly below the axis. The model must look correct at rest, and the builder should export it with the rotor at angle 0.
- **Speed while firing:** **one full turn (360 degrees) per 250 ms** (the 5-tick fire window), which is **4 turns per second, 1440 degrees per second**. The angle is a straight line in time through each window: 0 at the start, 360 at the end. At the end of a window it wraps to 0, which looks identical, so held fire shows a **continuous, constant-speed spin with no jitter**. Six barrels at 60 degrees apart mean the barrels blur at 24 barrel-passes per second.
- **Spin-up and spin-down: none in the original.** The rotor is at full speed on the first firing tick and **stops dead at angle 0 the instant firing stops**. There is no acceleration, no deceleration and no coasting, and no motion at all during idle or reload, or while the gun is just held in the world or on the ground. (A spin-up sound exists in the pack but is never played.)
- **Where it spins:** in first person (about the gun's own axis, so the cluster stays put and only turns) and in third person while firing. In the ground and icon views, it is at rest.
- **Optional (not in the original):** the builder may keep the rotor as an independently animatable part so later code can add a true ramp, for example about 10 ticks to full speed and 20 ticks to coast down. If so, the rest pose and the full-speed look must still match this document. The rest pose must be exactly angle 0.

### Whole-gun motion
- **Fire shudder (first person):** a very small shake per 5-tick window: roll up to 1.5 degrees about the sideways (Z) axis and a 0.05-block push along the gun axis, both sine-shaped, one cycle per window.
- **Reload (100 ticks, 5 s):** first person: the gun tips and slides up and aside by about 0.5 block along the axis, 0.25 up and 0.25 sideways, holds near the middle, then drops back. The rotor does **not** move. Third person: tilts as in section 6.
- **Muzzle flash:** 5 ticks, played twice, at the front of the cluster.

## 8. What to avoid

There are no earlier GregGuns attempts for this gun yet (the pack has only a texture sheet). These are the likely traps.
- **A fused rotor.** If the barrels, tips and clamps are not their own separate part, the gun cannot spin, and the original's key motion is lost. Do not bake the rotor into the housing.
- **Wrong barrel count or ring.** Six barrels at 60 degrees, with one straight up and one straight down at rest. Not four, not eight, not a single fat tube with grooves.
- **A central shaft or a core tube** through the barrel ring. There isn't one. The center is only covered by the thin clamp discs.
- **A shroud or heat shield** around the cluster. The barrels are bare.
- **A boxy ammo box, a belt or a feed chute.** The drum is a rounded octagon, near-white, with no belt.
- **A dark ammo drum.** It must be the lightest object. Dark drums make the gun read as one mass.
- **Making the rear handle a pistol grip or adding a stock.** The back is a thin flat trapezoid loop, 2 wide. There is no trigger.
- **An all-dark or all-light gun.** The layering (charcoal frame, mid-gray gearbox, silver barrels, white drum, black clamps) is the look.
- **Making the gearbox a plain cube.** It is a stepped octagon, round from the front.
- **Losing the side details:** the two light diamond pins with dark centers and the diamond drive housing with its two dark discs. They are why the front view reads as "machinery".
- **A carry handle that does not reach above everything.** It is the highest point.
- **Anything that floats.** The top plate hovers only 1 unit over the gearbox and is held by the bridge block and the rear post.
- Do not add a scope, rail, bipod, belt, muzzle brake, flash hider or power cable.

## 9. Acceptance checklist

- [ ] Length : height : width is within about 10 percent of 62 : 21.5 : 23 (2.9 : 1 : 1.07).
- [ ] The model has two named parts, housing and rotor, and the rotor contains only the barrels, the tips and the three clamp discs.
- [ ] Rotor spin axis is X through Y = 0, Z = 0, concentric with the gearbox and the yoke opening.
- [ ] Six barrels, each 2x2 and 21 long, centered on a 2.5 radius ring at 60-degree spacing; rest pose has one straight up and one straight down.
- [ ] Six 1x1 tips, 12 long, on the barrels' centerlines, giving a total length of 62 and a rotor length of 35.
- [ ] Three near-black octagon clamp discs: hub (2 thick, 8 across) at X 3 to 5, middle (1 thick, 8 across) at X 17, front (1 thick, 7 across) at X 35, 2 short of the tips' ends.
- [ ] Barrels are two-tone: warm gray rear half, pale silver front half, with a black seam under the middle clamp.
- [ ] Gearbox is a stepped octagon about 16 long and 10 by 10, mid-gray, reading round from the front and back.
- [ ] Ammo drum hangs under the shooter's-right (-Z) side, about 14 long, 11 tall and 10 wide, near-white, with two dark strap lines per side, and its bottom is the lowest point (Y 10.5).
- [ ] Left (+Z) side has a smaller diamond drive housing (about 5.7 across) with two near-black end discs.
- [ ] Yoke ring around the hub, about 11 wide by 10 tall inside, in medium-dark gray, with a 14-wide top bar.
- [ ] Top plate, V struts (55 degrees off the axis) and the carry handle make a Y when seen from above; the carry handle is the highest point (Y -11).
- [ ] Rear handle is a flat 2-wide trapezoid loop, 5 tall at the rear end post and 11 tall at the gearbox, in dark charcoal, with no trigger and no stock.
- [ ] Two light diamond side pins with near-black end caps sit at Y -4.5, Z ±6.
- [ ] Palette: charcoal frame (#474545), gearbox (#999795), barrels (#A7A09D to #CDCAC9), drum (#E4E4E4), clamps (#292727), all flat with 2 to 6 levels of noise.
- [ ] No part floats; every piece touches or overlaps a neighbor, and the rotor clears the yoke.

## 10. Unsure

- **Spin direction.** I worked it out from the transform sign and the model's handedness: clockwise as seen from behind. I have not seen it in game. Because the barrels repeat every 60 degrees, the direction is hard to see on the barrels alone, and it matters little visually.
- **Hand position in third person.** The offset I read is relative to the model origin, and the pack's registration adds no extra offset. I put the hand near the yoke and carry handle, but the exact grip point comes from formulas, not a screenshot. Treat the third-person numbers as approximate.
- **First-person framing numbers** come from formulas read, not seen in game, and the reload offset is the idle offset plus the formula's maximum terms.
- **Strap lines on the drum** are my reading of a low-resolution texture; their exact positions are within 1 unit.
- **Barrel ramp colors.** The warm-gray and pale-silver tiers come from sampled pixels; the in-between steps are a smooth ramp and need not match exactly.
- **What the left housing and the drum are.** The original is not labeled. I describe them as a drive housing and an ammo drum from shape alone; the builder only needs the shapes.
- **Rear loop hole size.** The 2 to 5.5 tall wedge is computed from the 15-degree bars and the end post; it may differ by up to 1 unit.
