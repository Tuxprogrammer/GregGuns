# Thompson: clean-room model notes

Design notes for rebuilding the Techguns **Thompson** ("Submachine Gun") look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelThompson](../gun-models.md#modelthompson-6464-family-x-24-boxes-1-part), [thompson sheet](../gun-sheets.md#thompson-thompson), [rendering framework](../rendering-framework.md), [Techguns thompson stats](../../02-guns/sheets-handguns-smgs.md#submachine-gun-thompson-thompson).

**Units and axes used in this document.** One unit = one texel at the native 64x64 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention), with **Y = 0 at the top face of the receiver** (the long main box). **Z = 0 is the gun's mid-plane**; sizes across Z are given as total width, centered. **X = 0 is the back face of the butt plate** (the rearmost point of the stock). Rounded to the nearest half unit unless it matters. Negative Y is above the top line.

## 1. Gestalt

A big, long, **1920s-style tommy gun** in chunky Minecraft boxes: a gray steel receiver and barrel, a **wooden forend, wooden blocky stock and a small wooden pistol grip**, a **tall vertical gray slab** hanging under the receiver ahead of the grip, and a tiny open rear sight with a post front sight. The gun is very long and thin: you read it first as a **long horizontal bar with a brown stock at the back and a brown sleeve at the front**, with gray in the middle and a short gray barrel stub poking out ahead of the brown forend.

Defining features in order of visual importance:

1. **Very long, slim profile:** about 13 : 3.5 : 1 (length : height : thickness). Almost everything is 3 wide.
2. **Brown-gray-brown banding along the length:** wooden stock, steel receiver, wooden forend, steel barrel tip. Two materials, strongly alternating.
3. **A bold vertical slab under the receiver** (3 long, 10 tall) that is the tallest thing on the gun and is gray, like the receiver.
4. **A pistol grip that leans back about 25 degrees**, short and brown, behind the vertical slab and below the receiver's rear.
5. **A diamond-section barrel** that pokes through the wooden forend: a ridge on top of the forend and a 4-unit stub past its front.
6. **A stepped, lowered stock** that starts lower than the receiver and has a taller butt plate.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt plate to muzzle) | 52 |
| Total height (sight tops to bottom of the vertical slab) | 14 (from Y = -1 to Y = 13) |
| Total thickness | 4 at the widest (the receiver's base ledge); 3 for almost everything else |
| Length : height : thickness | about 13 : 3.5 : 1 |
| Receiver | 22 long, 3 tall, 3 wide |
| Barrel | 18 long (about 35 percent of the total), 2.8 across the diamond |
| Forend | 13 long, 3 tall, 3 wide |
| Stock (butt plate to the receiver's back face) | 12 long |
| Vertical slab | 3 long, 10 tall, 2 wide |
| Pistol grip | 3 wide by 6 long before tilt, 2 across Z |

Layout along the length (X): the butt plate sits at 0 to 2; the stock body 2 to 12; the receiver 12 to 34; the vertical slab 27 to 30 under it; the forend 35 to 48; the barrel from inside the receiver to 52. There is a visible 1-unit bare-barrel gap between the receiver's front face (34) and the forend's rear face (35).

The **top line** is the receiver top (Y = 0). The barrel's top corner reaches Y = 0.1, so the receiver top and the barrel top form one nearly straight line. Only the two sights rise above it, by 1 unit.

## 3. Part by part

All parts are boxes unless stated. Z widths are centered on the mid-plane. "Touches" means faces in contact or overlapping.

### Receiver
- One long box, **22 long (X 12 to 34), 3 tall (Y 0 to 3), 3 wide**. Flat sides and a flat top; no ejection port, no cuts, no vent holes.
- Both the stock and the barrel attach to it: the barrel is buried in its front, and the stock butts against its back face at a lower level.

### Receiver base ledge
- A flat **dark** plate, **15 long (X 12 to 27), 1 tall (Y 3 to 4), 4 wide** (half a unit wider than the receiver on each side), directly under the rear two thirds of the receiver.
- It is the only place where the gun is 4 wide. Seen from above it shows as a thin lip beside the receiver.
- Under the front third of the receiver (X 30 to 34, Y 3 to 4, 3 wide) there is a small **dark** plate in the same dark gunmetal, filling the space between the vertical slab and the receiver front.

### Barrel
- A square bar 2x2 in section and **18 long**, turned 45 degrees about its own axis so it shows a **diamond**: edge-on, about 2.8 units tall and 2.8 wide.
- Runs from inside the receiver front (X 34, buried) to the muzzle at **X 52**. Its axis sits at **Y 1.5**, so the top corner is at Y 0.1 and the bottom corner at Y 2.9.
- Visible portions: 1 unit bare between the receiver and the forend (X 34 to 35); a 1.4-unit ridge above the forend's top the whole length of the forend; and the **4-unit bare stub** past the forend's front (X 48 to 52). The lower half is buried in the forend.
- No taper, no muzzle device, no cooling fins, no compensator. A clean bar.
- Its two end faces are plain; the muzzle face reads as a dark bore (near-black).

### Front sight
- A post **1 long, 2 tall, 1 wide**, at X 50.5 to 51.5, Y -1 to 1: it sits on the barrel's top corner about 0.5 to 1.5 back from the muzzle and rises **1 unit above the top line**.

### Rear sight
- A tiny open "U", on the receiver top at **X 13 to 15** (right at the receiver's back end), rising **1 unit** above the top line (Y -1 to 0).
- Made only of thin sheets, not thick boxes: two side leaves 2 long and 1 tall, separated by 2 across Z (centered, so each sits half a unit in from the receiver's edge), plus a small cross leaf 1 wide at the middle of their length, X about 14. Seen from the side it reads as a small rectangular tab; from the top as a narrow rectangle with a bar across it.
- Optional: the builder may give the leaves a hair of thickness (0.2) so they render from both sides.

### Forend
- A wooden box, **13 long (X 35 to 48), 3 tall (Y 1.5 to 4.5), 3 wide**, wrapped around the lower half of the barrel. Its top face is at the barrel's axis height, so the barrel's upper half stands up out of it as a ridge.
- The sides carry a **painted darker slot** (section 5) running most of its length. The geometry has no grooves.

### Vertical slab
- A gray box **3 long (X 27 to 30), 10 tall (Y 3 to 13), 2 wide**, hanging straight down (no tilt) from the underside of the receiver, directly behind the dark front plate and just ahead of the base ledge's front end (X 27).
- It is the **lowest point** of the gun (Y 13) and 2 units lower than the pistol grip's bottom. Reads as a front grip or stick magazine (see section 10).
- Touches: the receiver's underside (Y 3); the trigger guard's front post on its rear face.

### Trigger group and pistol grip top
- **Grip top block:** a gray box **3 long (X 20 to 23), 2 tall (Y 5 to 7), 3 wide**, under the receiver, behind the guard. It carries the pistol grip.
- **Rail under the receiver:** a gray box **7 long (X 20 to 27), 1 tall (Y 4 to 5), 3 wide** connecting the grip block to the guard post and base ledge.
- **Guard front post:** a gray box **1 long (X 26 to 27), 2 tall (Y 5 to 7), 3 wide**, just behind the vertical slab. Touches the slab's rear face.
- **Guard floor:** a zero-thickness plane 3 long (X 23 to 26) by 2 wide at Y 7. It is invisible in side views and visible from above or below. In side views the opening between the grip top block and the post reads as an **open 3-by-2 notch with no bottom line**.
- There is **no trigger part**.

### Pistol grip
- A wooden box **3 long, 6 tall, 2 wide**, **tilted 25 degrees so its bottom swings backward (toward the stock)**. Pivot at the grip's top-front corner, about (X 23, Y 6.5).
- Corner positions after the tilt: top front (23, 6.5), top back (20.3, 5.2), bottom front (20.5, 11.9), bottom back (17.7, 10.7). So the grip is narrower than the guard opening above it and its bottom hangs about 1 unit **above** the vertical slab's bottom.
- It hangs out of the grip top block and is overlapped by it at the top.

### Stock
All wood, built stepwise and lower than the receiver. Parts are 3 wide unless noted.
- **Butt plate:** X 0 to 2, Y 5 to 11, 3 wide. It is the tallest part of the stock (6 tall).
- **Main body:** X 2 to 12, Y 5 to 8 (10 long, 3 tall). The stock's top (Y 5) is 2 units below the receiver's underside (Y 3), so the bridge blocks below fill the gap.
- **Lower comb block:** X 2 to 8, Y 8 to 10 (6 long, 2 tall); a small **foot** X 8 to 10, Y 8 to 9 (2 long, 1 tall). Together with the butt plate they make the stock's underside step down at the back: a heavy, boxy stock with a notch between the comb block and the pistol grip.
- **Bridge to the receiver:** a shallow block X 9 to 12, Y 4 to 5 (3 long, 1 tall), a 4-wide wooden block X 12 to 17, Y 4 to 5, then a 4-long, 2-tall block X 12 to 16, Y 5 to 7; plus two 1x1 notches X 16 to 18. These fill the space under the receiver's rear and make the stock grow a step toward the receiver. The 4-wide block is the only wooden part wider than 3.
- The stock reads as one wooden mass: a heavy chunky butt, a thick body, and a step up into the receiver. The back of the receiver (X 12, Y 0 to 3) is a bare gray wall above it.

## 4. Silhouette checks

- **Side (the main view):** a long horizontal shape, muzzle at the right. From the left: the butt plate (a tall brown rectangle, 6 tall); the stock stepping up to the receiver; the flat gray receiver with a tiny rear sight tab on top at the back end; the **tall gray vertical slab** hanging below, 10 tall; ahead of it a bare dark underside and then the **brown forend** with a thin gray ridge on its top; a 4-unit gray barrel stub with a small post sight on top. The pistol grip is a short brown slanted slab between the stock and the slab, leaning back. Negative space: the 3-by-2 open notch under the receiver between the grip and the slab's guard post; the gap above the stock (receiver's back wall); the 1-unit bare barrel between receiver and forend.
- **Top:** a thin spine. 3 wide almost everywhere with the receiver's 4-wide ledge barely visible as a slim lip beside it; a rear sight rectangle at the back of the receiver; the forend, the same width, then the barrel stub narrowing to a diamond tip about 3 wide. The stock is the same width as the receiver, so no visible shoulders.
- **Front:** a narrow tall column. At the top, a black diamond (the muzzle) on a brown rectangle (the forend, 3 wide), with the sight post above; below it a gray column (the vertical slab, 2 wide) and the brown grip peeking out sideways. Everything stays within 3 to 4 wide.
- **Back:** a brown butt plate (3 wide, 6 tall) in the lower half, the gray receiver wall and a small sight bump above it, a small step between them. Narrow and tall.
- **Underside:** a long thin line: brown stock, narrow dark ledge (4 wide) in the middle, the 2-wide slab and grip crossing it, brown forend at the front, then the barrel tip.

## 5. Materials and palette

The original texture is warm brown and mid-dark gray. Black pixels in it mark unpainted areas of the sheet; the barrel's end caps are black, which is a happy accident that reads as a bore. Values below were sampled from regions of the texture.

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Barrel, front sight, rear sight | dark steel | #57534E | #5D5955 | #54504B |
| Receiver | dark steel | #55524C | #58544F | #53504A |
| Vertical slab, grip top block, rail, guard post | steel, same family | #57534E | #625B57 (a few lighter pixels) | #54504B |
| Base ledge and front plate | darker gunmetal | #43403C | #474440 | #403D39 |
| Rail under the receiver (X 20 to 27) | steel, a touch lighter | #5B5753 | #5B5651 | #595550 |
| Stock (body, butt plate, comb block, bridge blocks) | warm wood | #7A4A1F | #844F1F | #6A431F (butt plate and edges) |
| Forend | warm wood | #7C4B1F | #804D1F | #76481F |
| Pistol grip | warm wood | #74471F | #7D4B1F | #6E451F |

Shading style:
- **Metal:** dark, flat and a little warm, with noise of two or three levels (about +-4 in value). Not blued, not shiny. The receiver and barrel are nearly identical; the vertical slab is only a touch lighter, with a few pixels up to #625B57. The ledge and front plate are clearly darker (#43403C), the darkest non-black color on the gun.
- **Wood:** orange-brown, about #77491F to #844F1F, with streaky per-pixel noise of a few levels and no clear grain direction. The butt plate and its edges run slightly darker (#68 to #6C range in red).
- **Forend slot:** a **darker brown horizontal band** on each side of the forend (a painted slot, about 2 rows tall, roughly 2 to 3 units in from each end) so that the forend reads as a ribbed or slotted handguard. In renders it shows as a long darker inset in the wood.
- **Pin dots:** a tiny near-black dot on each side of the stock near the pistol grip (a screw or pin), about 1 pixel.
- No wear, no scratches, no gradients.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 1.8 | idle offset about (0.1, -0.35, -0.5) on top of the framework default. The gun is seen nearly side-on, turned 45 degrees so the stock's back end is also visible. |
| First person firing | roll about the Z axis, up to about 5 degrees at mid-animation | with a shift of about 0.15 backward. A small kick (the gun is full-auto; it fires every 3 ticks). |
| First person reloading | roll up to about 25 degrees, yaw about -15 degrees | shift about (0.6, -0.1, -0.25) from idle; smooth rise and fall. |
| First person aimed | no scope, no zoom overlay | the open rear sight and post front sight are the only aids |
| Third person (held) | yaw 225 degrees, scale 1.15 | offset (-0.85, -0.6, 0). |
| Ground (dropped) | yaw 90 degrees, scale 0.8 | lowered by 0.45. |
| Inventory icon | yaw 180 degrees, scale 0.65 | offset (-0.25, 0.1, 0); drawn side-on. There is no sprite; the icon is the 3D model. |

**Muzzle flash point:** the front of the barrel on its axis (model position X 52, Y 1.5, Z = 0); a flash sprite at roughly 1.5 times scale. The flash lasts 5 ticks. The fire delay is 3 ticks (about 400 rounds a minute).

The model is **not** an akimbo gun.

## 7. Motion

- The **model has no moving parts.** The vertical slab, the grip and the sights do not move. All motion is whole-gun transforms.
- **Firing:** each shot rocks the whole gun about 5 degrees about Z and slides it back about 0.15 block, then settles.
- **Recoil (third person):** the gun pitches up about 20 degrees around a point about 0.9 block toward the shooter and 0.3 up from the model origin, during the first quarter of the first half of the animation, then returns.
- **Reload:** 40 ticks. First person: the gun tips up to about 25 degrees and slides aside, hangs near the middle, then returns. Third person: about 15 degrees of pitch and 20 degrees of yaw at mid-animation.
- **Optional (not in the original):** the builder may keep the vertical slab as a separate named part so later code can animate it as a magazine, but the rest pose must match this document.

## 8. What to avoid

- **A floating or off-balance pistol grip.** It must hang out of the grip top block, tilted back 25 degrees, not be straight and not touch the stock.
- **A solid wooden stock like a rifle stock.** It is stepped: lower than the receiver, heavy at the butt plate, with a notch under it.
- **A curved or round barrel.** It is a diamond bar. No fins, no compensator, no drum magazine.
- **A visible trigger.** None. The guard is a post, an invisible floor and a notch.
- **A uniform color.** The brown-gray banding along the gun is the identity. Do not make the forend gray or the stock gray.
- **Dropping the 4-unit bare barrel stub** past the forend or covering the 1-unit gap behind the forend.
- **Shortening the vertical slab.** It is the tallest thing on the gun (10), lower than the grip.
- **Making the gun fat.** It is 3 wide throughout; only the ledge is 4.
- **Adding things the original lacks:** no top rail, no scope mount, no bolt handle, no ejection port, no cooling holes, no drum.

## 9. Acceptance checklist

- [ ] Length : height : thickness is within about 10 percent of 13 : 3.5 : 1.
- [ ] Total length about 52; the barrel exceeds the forend by 4 units at the muzzle.
- [ ] Receiver is a 22-long, 3-tall, 3-wide plain box with a flat top.
- [ ] Barrel is a diamond bar (square turned 45 degrees), the top corner flush with the receiver top and half buried in the forend.
- [ ] A 1-unit bare barrel gap shows between the receiver front and the forend.
- [ ] Front post sight rises about 1 unit above the top line, near the muzzle; a tiny open rear sight rises about 1 unit at the receiver's back end.
- [ ] Forend is wood, 13 long, 3 tall and 3 wide, with a visible darker slot on the sides.
- [ ] A 3-long, 10-tall gray slab hangs under the receiver and is the lowest point of the gun.
- [ ] Pistol grip is wood, 2 wide, tilted about 25 degrees backward, and its bottom is about 1 unit above the vertical slab's bottom.
- [ ] Trigger guard is a post and an open notch, with no bottom line from the side and no trigger part.
- [ ] Stock is wood, stepped and lower than the receiver, with a taller butt plate (6) and a heavy body.
- [ ] A dark ledge (4 wide) runs under the receiver's rear; no other part is wider than 3.
- [ ] Metal is dark gray (about #55524C to #5D5955); the ledge is darker (#43403C); wood is orange-brown (about #7A4A1F).
- [ ] Side silhouette matches section 4 (brown, gray, brown, gray banding).
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Vertical slab:** it could be a front grip or a stick magazine. Its texture is plain steel, with no feature of either. Build it as a plain gray slab.
- **Stock bridge parts:** I read the small blocks between the stock and receiver from sizes only. In renders they read as one stepped wooden mass; the exact internal steps matter little as long as the outline matches.
- **Pin dots** on the stock are my reading of a few dark pixels; they are optional.
- **Muzzle black:** the barrel's end faces are pure black because the original leaves them unpainted. A dark bore is fine.
- **First-person framing numbers** come from formulas read, not seen in game. Treat the offsets as approximate.
- **Camo variants:** none. There is a single texture.
