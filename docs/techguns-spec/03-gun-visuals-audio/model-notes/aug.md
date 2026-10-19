# AUG: clean-room model notes

Design notes for rebuilding the Techguns **AUG** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and textures. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelAUG](../gun-models.md#modelaug-128x64-family-z-24-boxes-tabula-2-parts), [AUG sheet](../gun-sheets.md#aug-aug), [rendering framework](../rendering-framework.md), [Techguns AUG stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = 1/16 block (one texel at the native 128x64 scale). Muzzle toward **+X**. **X = 0 is the back face of the butt plate**, so the muzzle tip is at X = 70. **Y points down** (Minecraft model convention), with **Y = 0 at the top of the scope's rear housing**. **Z = 0 is the gun's mid-plane**; sizes across Z are total widths, centered, unless a side is named. **+Z is the gun's right side**, the side with the charging handle. "Tilt" means a lean measured from vertical in the side view; "lean back" means the lower end is nearer the butt (smaller X). A "diamond" bar is a square bar turned 45 degrees about its own long axis, so it shows a corner at the top and bottom and is 1.41 times its nominal width, edge to edge.

## 1. Gestalt

A big, dark-green **bullpup assault rifle with an integrated optic**, in the manner of the Austrian Steyr AUG. The whole weapon is a long, almost straight **horizontal tube**: a fat green receiver that runs all the way back to a tall butt plate, with a **very long, thin black barrel** sticking out the front, a **black scope on a slanted carry-handle** riding on top, a **banana magazine behind the pistol grip**, and a **tall vertical foregrip post** under the front of the receiver. The color story is simple: dark forest green for the body, near-black for the optic and barrel, mid-gray for the magazine, and a lighter green only on the foregrip.

Defining features in order of visual importance:

1. **Bullpup layout.** The pistol grip and trigger sit far forward of the butt; the magazine is **behind** the grip. About 37 percent of the gun's length lies ahead of the receiver, and it is only a barrel.
2. **Scope on a handle.** A black two-stage tube (a narrow front tube, a fatter rear housing) sits about 2 to 3 units above the receiver ridge, held by two slanted struts that make a leaning handle. It is the highest thing on the gun.
3. **Long, thin barrel** (26 long, under 3 thick) that makes the front 37 percent of the gun a skinny black needle in the top view.
4. **Deep, tapered stock/receiver rear:** the underside slopes down toward the butt, ending in a tall flat butt plate (12 tall, the tallest vertical slab on the gun).
5. **Curved gray magazine** behind the grip, bending forward as it goes down, and the **tall green foregrip post** in front, which has a diamond cross-section and a lighter green than the rest.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt back to muzzle tip) | 70 |
| Total height (scope top to magazine bottom) | about 25.4 |
| Total thickness (receiver diamond, 5.7; with the handle nub, about 6.5) | about 6.5 |
| Length : height : thickness | about 10.8 : 3.9 : 1 |
| Receiver (diamond) length | 44 (X 0 to 44) |
| Barrel with muzzle cap | 26 (X 44 to 70), about **37 percent** of the total length |
| Scope length | 20 (X 18 to 37), top at Y 0 |
| Receiver ridge (top corner of the diamond) | Y 6.5 |
| Receiver bottom corner / lower plate underside | Y 12.2 / Y 12.5 |
| Barrel axis | Y 8.4, about 1 unit above the receiver's axis (Y 9.3) |
| Butt plate | 4 wide, 12 tall, 4 deep, Y 9.5 to 21.5 |
| Pistol grip bottom | about Y 20.6 |
| Foregrip bottom | Y 20.5 |
| Magazine bottom (lowest point of the gun) | Y 25.4 |

How the pieces sit along the length, from the rear: butt plate (X 0 to 4); a deepening rear body (X 4 to 24); the pistol grip and guard (X 21 to 36); the foregrip post (X 41 to 45, centered at 43); the receiver ends at X 44; then 26 units of barrel. The scope occupies X 18 to 37, entirely above the receiver and well behind its front end. The magazine is at X 12 to 20, behind the grip.

The **top line is stepped:** scope top (Y 0), then a gap, then the receiver ridge (Y 6.5) running the full 44, then the barrel top (Y 7.0) 0.5 below the ridge. The barrel does not continue the ridge; it steps down by half a unit.

## 3. Part by part

Z widths are centered on the mid-plane. "Buried" means overlapped by at least 0.5 unit so nothing floats.

### Receiver (main shell)
- A **diamond-section bar**, nominal 4x4 (5.66 across, 5.66 tall), **44 long** (X 0 to 44). Top corner at **Y 6.5**, bottom corner at Y 12.2, widest at Y 9.3 where it is 5.7 across (from about -2.8 to +2.8 about the mid-plane).
- It is the backbone: scope struts bury in its top, the foregrip post buries in its underside at the front, and everything from the stock buries in its rear and underside.
- Its rear end is flush with the butt plate's back face (X 0) and its front end (X 44) is a flat face from which the barrel and gas tube emerge.
- No rail, no ejection port cut, no notches. The shell reads as a clean rounded-looking bar because of its diamond section.

### Lower receiver plate
- A flat box, **18 long, 2 tall, 3 wide**, at Y 10.5 to 12.5, X 24 to 42. It fills the lower flanks of the diamond and gives the underside a flat floor 0.3 below the diamond's bottom corner. It is the same material as the receiver. The grip and guard attach to this plate.

### Barrel
- A diamond bar, nominal 2x2 (2.83 across and tall), **23 long**, from X 44 to X 67. Axis at Y 8.4; top corner Y 7.0, bottom corner Y 9.8. It is black.
- **Muzzle cap:** a continuation of the same diamond section, **3 long** (X 67 to 70), a separate part with its own marking only (not thicker, not a flash hider). It ends the gun at X = 70.
- Buried 1 unit or more into the receiver's front face (the receiver face is large enough to hold it entirely).
- No taper, no brake slots, no front sight.

### Gas tube
- A thin diamond bar, nominal 1x1 (1.41 across), **6 long** (X 44 to X 50), centered **1.5 units to the gun's left (-Z)** of the mid-plane, at Y 9.5 axis (Y 8.8 to 10.2).
- Rear end buried in the receiver's front face. It runs parallel to the barrel, about 0.5 unit clear of the barrel's flank in the side-to-side direction, leaving a hairline gap at the top and bottom.
- Color: black like the barrel. On the left side only; the right side has nothing equivalent.

### Scope
Two black diamond tubes in line, resting on the handle struts:
- **Rear housing:** nominal 3x3 diamond (4.24 across and tall), **10 long** (X 18 to X 28), Y 0 to 4.24 (axis Y 2.1). This is the highest part of the gun.
- **Front tube:** nominal 2x2 diamond (2.83 across and tall), **9 long** (X 28 to X 37), Y 0.5 to 3.3 (axis Y 1.9). Its top corner sits 0.5 below the housing's top, and the two butt together at X 28, so the top line steps down by 0.5 at the join.
- The scope floats about 2.3 units above the ridge at the rear (housing bottom at Y 4.24 against the ridge at Y 6.5) and about 3.2 units above it at the front (tube bottom Y 3.3). The struts bridge that gap.
- No lens detail is needed. The original has no visible lenses.

### Scope struts (the carry handle)
Two slanted flat plates, each **2 wide (Z)**, **1 thick** (measured along X before tilting) and about 7 to 8 long. Both lean the same way: **their lower ends are further forward (higher X) than their upper ends**.

| Strut | Length | Top end (buried in the scope, at the scope's axis) | Lower end (buried in the receiver) | Tilt from vertical |
|---|---|---|---|---|
| Front | 8 | X 36, Y 1.5 | X 40.6, Y 8.1 | 35 degrees, lower end forward |
| Rear | 7 | X 27, Y 1.5 | X 30, Y 7.8 | 25 degrees, lower end forward |

- The two tops are 9 apart and the two bottoms are about 10.6 apart, so the pair fans out slightly toward the receiver and reads as a leaning trapezoid handle.
- The enclosed window between the struts, the scope underside and the receiver ridge is a **skewed quadrilateral hole**, about 9 to 10 wide at the top, 3 to 4 tall at the front and 4 to 5 tall at the rear. It must stay open from side to side (negative space).
- Same color as the receiver (green), not black.

### Charging handle (bolt, separate part)
- A small diamond block, nominal 1x2x2 (a bar 2 long along X), on the gun's **right side only** (+Z), at **X 40 to 42**, spanning Y 6.5 to 8.6, above the receiver's widest line (Y 9.3). It overlaps the receiver's right flank and pokes out by about **0.9 unit** past the receiver's widest point, reaching about 3.7 from the mid-plane.
- Color: same green as the receiver, slightly lighter on its outer corner.
- **It is its own part** so it can slide. The builder must give it its own part/group; the rest pose sits at the position above.

### Foregrip post
- A tall vertical post with a **diamond cross-section**: nominal 3x3, turned 45 degrees about the vertical axis, so from the side and front it is 4.24 wide and 4.24 deep; centered at **X = 43, Z = -0.4**. It is **9 tall**, Y 11.5 to 20.5, with its top buried 1 unit into the receiver's underside.
- Side view: a straight vertical column 4.24 wide at X 40.9 to 45.1. It is **not** angled. Front view: a diamond-section post below the barrel.
- It is the lightest-colored part of the gun (a mid-green; see section 5), and shows a darker vertical seam on each corner edge.
- It is much farther below the barrel than a normal foregrip: the post's bottom (Y 20.5) is about 10.7 under the barrel's bottom (Y 9.8).

### Pistol grip
- A box 3 wide, **9 long (the lean direction), 4 thick**, tilted **25.6 degrees** from vertical, with the lower end toward the butt (lean back).
- Top front corner at **X 29, Y 12.5** (against the lower receiver plate). Bottom front corner at X 25.1, Y 20.6. Bottom back corner at X 21.5, Y 18.9. Top back corner at X 25.4, Y 10.8 (buried in the receiver's flank, so nothing floats).
- So the grip is a slanted block, about 3.7 deep (fore-aft) at its upper end and swept back about 7.5 units from top to bottom.

### Trigger guard
- **Front plate:** 3 wide, 1 thick, 9 long, tilted **34 degrees** from vertical, lean back (lower end toward the butt). Top at **X 36, Y 12.5** (buried in the lower receiver plate), bottom at about X 31, Y 20.
- **Floor plate:** 3 wide, 1 tall, **6 long**, flat, **X 25 to X 31, Y 19.5 to 20.5**, joining the front plate's foot to the grip's foot. It is a real, thick floor (visible in side view as a short horizontal bar).
- Opening between the front plate and the grip (at Y 15): about 6.5 across (X 27.8 to 34.3). The guard encloses the trigger, so side view shows a closed, leaning **triangle-like loop** made of the plate, the floor and the grip.

### Trigger
- A small flat box **1 wide, 3 tall, 2 long**, X 28 to 30, Y 12 to 15, hanging from the lower plate inside the guard loop. Same dark green as the receiver (it is not separately colored).

### Rear body (under and behind the receiver)
Five stacked, 3-wide boxes (Z -2 to 1 around the mid-plane; 3 wide) fill out the stock. All are green, all bury into the receiver diamond. Read as one **deepening underside** that slopes from Y 12.5 at X 24 down to Y 21.5 at the butt:
- **Slope slab** (3 wide, 3 tall, 7 long): top-front at X 24, Y 9.5, tilted **18 degrees** with its rear end lower, ending at about X 17.4, Y 11.7. It blends the lower receiver plate into the rear body.
- **Cheek plate:** 3 wide, 2 tall, 4 long at X 17 to 21, Y 9.5 to 11.5. A small flat spacer.
- **Main rear block:** 3 wide, **5 tall**, **10 long**, **X 8 to X 18, Y 9.5 to 14.5**.
- **Ramp slab:** 3 wide, 3 thick, 10 long, tilted **26 degrees** with its rear end lower: starts about X 11, Y 12.8 (buried in the main block) and runs back and down to X 2 to 3, Y 17 to 20. It makes the rear underside a smooth diagonal.
- **Neck block:** 3 wide, **7 tall**, 4 long, **X 4 to 8, Y 9.5 to 16.5**.
- **Butt plate:** **4 wide, 12 tall, 4 deep**, **X 0 to 4, Y 9.5 to 21.5**. It is 1 unit wider than the rest of the rear body (a visible shoulder from behind). Flat back face, no pad detail beyond painted texture.

### Magazine well and magazine
- **Socket:** 3 wide, 3 tall, 8 long, tilted **12 degrees** (rear end lower), X 10 to 18, Y 10.5 to 15.5. Green, same as the body.
- **Upper magazine segment:** 2 wide, 6 deep (fore-aft), 5 long, tilted **12 degrees**, leaning so the lower end is further forward. Top front at about X 17.6, Y 15.5; bottom front at X 18.6, Y 20.4; bottom back at X 12.8, Y 21.6.
- **Lower magazine segment:** 2 wide, 6 deep, 4 long, tilted **21 degrees** the same way. Top front at about X 18.4, Y 19.5; bottom front at X 19.8, Y 23.2; bottom back at X 14.2, Y 25.4.
- The two segments overlap by about 1 unit and bend the magazine into a **shallow curve**, bowing forward as it descends. Total magazine length about 9, depth 6, width 2 (it is narrower than the 3-wide body). It is gray, and the gap between the grip's back and the magazine's front is about 4 to 7 units at mid-height.

## 4. Silhouettes

- **Side (the main view):** a long, fairly flat, low-slung rifle. From the left (butt): a tall rectangle (butt plate) with a diagonal underside sweeping forward up to the receiver; a steady green bar (receiver) at mid-height; a **black scope that stands about 6 above the receiver** on two leaning struts with a skewed open window under it; at the front of the receiver a **vertical post** (foregrip) and in front of it a **long, thin, black barrel** with a small gas-tube stub on the left side, ending at X 70. Below the receiver: the **slanted pistol grip with a closed guard loop** and a **curved gray magazine behind it**. Negative space: the window under the scope; the slot between guard and magazine; the long clear space under the barrel (about 10 units deep) in front of the foregrip; the empty pocket under the rear body between the butt and the magazine.
- **Top:** a very thin spine. A 4.2-wide scope housing over the receiver's rear half, narrowing to 2.8 for the scope tube; a 5.7-wide receiver (X 0 to 44) with a small handle nub bump on the right near X 40 to 42; then a skinny barrel at 2.8 wide for 26 units, with the 1.4-wide gas tube just left of it for its first 6. The butt plate is 4 wide, 1.7 narrower than the receiver.
- **Front:** a tall, narrow shape. At the very top, a black diamond (the scope housing, 4.2 across) over a small gap; a black diamond (the barrel, 2.8) below it; below that the 5.7 receiver diamond; the foregrip diamond post 4.2 wide hangs below it; and below that a dark gray stub (the magazine bottom, 2 wide) peeking out at the very bottom. The handle nub pokes out the right at receiver level; the gas tube pokes out on the left.
- **Back:** the butt plate fills the view: a 4-wide, 12-tall green slab, with the receiver diamond rising above it (about 2.8 higher at the top corner), and the scope housing as a black diamond above that (4.2 across) with the mount struts visible behind it. The gray magazine shows below the butt plate as a 2-wide stub.
- **Underside:** a thin green bar with the narrow foregrip diamond, the pistol grip and guard footprint, the gray magazine (2 wide), and the rear body widening to the 4-wide butt plate. The barrel appears as a thin black line forward of X 44.

## 5. Materials and palette

The textures are dark, desaturated and low-contrast, with fine per-pixel noise on the green body. The values below were sampled from regions of the default texture (the camo variants are in section 5b).

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Receiver, lower plate, rear body, struts, trigger | forest-green composite | #224333 | #244736 | #1F3E2F |
| Butt plate and rear block | same green, a touch lighter | #244837 | #264B39 | #214232 |
| Pistol grip, guard | same green | #224333 | #234635 | #214232 |
| Charging handle | same green | #224434 | #234635 | #1F3E2F |
| Foregrip post | lighter mint-green | #3B775A | #3D7A5D | #3A7458 (dark edge lines #000000) |
| Barrel, muzzle cap, gas tube | black | #010101 | #040404 | #000000 |
| Scope (both tubes) | black | #010101 | #030303 | #000000 |
| Magazine (both segments) | mid gray | #606060 | #606060 | #515151, with dark stripes #414141 |
| Magazine socket | same green as the body | #224333 | #244736 | #214232 |

Shading style:
- **Green body:** nearly flat, with fine per-pixel noise between about #1F3E2F and #264B39 (3 or 4 neighboring shades), no gradients, no scratches, no edge wear. The effect is a matte painted look.
- **Black parts:** essentially pure black with the faintest per-pixel noise (#000000 to #060606). There is no gloss and no highlight to speak of; the barrel and scope read as silhouettes against the green.
- **Foregrip:** the same noise style but in the lighter mint green, with small black pixels along its vertical edges.
- **Magazine:** horizontal ribs hinted as darker gray stripes (#414141 to #515151) over the #606060 base, about 4 or 5 stripes along its length.
- **Texture sheet extras:** the original sheet also carries a few small cyan dots and a gray block that do **not** appear anywhere on the model in renders. Leave them out.

### 5b. Camo variants (4 alternates besides the default)

The default is the green set above. The camo variants only swap the body green and the foregrip; black parts and the magazine gray stay the same in all of them.

| Variant | Body base | Body highlight | Foregrip base |
|---|---|---|---|
| 0 Default (green) | #224333 | #244736 | #3B775A |
| 1 Gray | #242424 | #282828 | #414141 |
| 2 Wood (woodland green) | #184322 | #1B4B26 | #2C7B3E |
| 3 Sand | #C3C684 | #C6C98A (shadow #C0C37C) | #E4E5C6 |
| 4 Nether | #680000 | #860000 | #8A0000 (bright flecks #B00000) |

The Wood variant is a slightly more saturated green (a bit brighter and more yellow-green than the default). The Nether variant is a deep red.

## 6. In-game presentation

All transforms apply to the whole model, in block units after the 1/16 scale. The model is first rotated 180 degrees about its long axis so Y-down becomes screen-up.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw 225 degrees about the vertical axis, scale 1.4 | idle shift about (-0.55, -0.35, -0.65). The gun is seen from behind and slightly from the side, muzzle pointing away and across the screen. |
| First person firing | pitch up by at most about 5 degrees, and a shift back along the gun axis of about 0.15 block | at the middle of the animation. The charging handle slides back 0.6 block (9.6 units) in the first 15 percent of the shot, then returns linearly. |
| First person reloading | the gun tips up to about 50 degrees at the middle of the reload | smoothly rises and falls. |
| First person aimed | scope overlay (a sniper-style scope picture) and a zoom factor 0.5 | the overlay is drawn at a position about 1.8 blocks along the barrel and 0.7 off to the side. Scope recoil nudges the view about (0.15, 0.20, 0.10) per unit of recoil. |
| Third person (held) | yaw 135 degrees, scale 0.8 | offset (0.1, -0.6, 1.4). Aimed: extra shift of 0.1 up. |
| Ground (dropped) | scale 0.6 | lowered by 0.45. |
| Inventory icon | yaw 90 degrees, scale 0.48 | offset (0.3, 0, 0); drawn side-on, filling the slot. There is no sprite; the icon is the 3D model. |

**Muzzle flash point:** the muzzle tip on the barrel axis (X 70, Y 8.4, Z 0), flash scale about 1.5. In third person the flash is drawn about 0.025 block above the origin of the hand position.

## 7. Motion

- Moving parts: **only the charging handle.** It slides straight back (toward the butt, -X) along the side of the receiver by 9.6 units, taking 15 percent of the fire animation to reach full travel, then eases forward over the remaining 85 percent.
- Everything else is rigid: the magazine does not drop, the scope does not move, the trigger does not pull.
- **Recoil:** 5-tick fire animation, flash 5 ticks. Third person: the whole gun pitches up about 15 degrees about the wrist in the first quarter of the first half, then settles.
- **Reload:** 45 ticks (2.25 s). First person tips the gun up (to about 50 degrees) and back down. Third person: about 30 degrees pitch and 40 degrees yaw at mid-animation about a point 0.2 below and 0.05 behind the origin.
- **Optional:** the builder may keep the magazine as a separate part so later code can animate a drop, but the rest pose must match this document.

## 8. What to avoid

(No earlier attempt exists for this gun. These warnings come from the shared lessons on the other guns, and from the details that are easy to get wrong here.)

- **A short barrel.** The barrel (plus cap) is 26 long, 37 percent of the gun. A stubby barrel loses the character completely.
- **A conventional layout.** The magazine is **behind** the grip and the stock is part of the body; do not put the magazine in front of the grip, and do not add a separate shoulder stock tube.
- **A solid, filled-in scope handle.** There must be a clear, open, skewed window between the struts, scope and receiver, visible from the side.
- **Upright struts.** Both struts lean (35 and 25 degrees), with lower ends forward. A vertical post pair or an X pattern is wrong.
- **A straight magazine.** It is two segments bowing forward (12 then 21 degrees).
- **A tilted foregrip.** It is a vertical diamond-section post, 9 tall, never angled, with a lighter green.
- **A flat-colored model.** The black parts, the green body, the lighter foregrip and the gray magazine need clearly different colors. Do not paint the magazine green or the foregrip black.
- **A floating charging handle or scope.** Everything must bury in a neighbor.
- Do not add things the original lacks: no rail, no flash hider or muzzle brake, no iron sights, no sling mount, no rear cheek riser, no lens glints.

## 9. Acceptance checklist

- [ ] Length : height : thickness is within about 10 percent of 10.8 : 3.9 : 1.
- [ ] Barrel plus cap is about 37 percent of the length (26 of 70), a thin diamond bar under 3 across.
- [ ] Receiver is a 44-long diamond (5.7 across) running the full length from the butt plate to the barrel's base.
- [ ] Scope is two black diamond tubes: a 4.2-wide rear housing (10 long) and a 2.8-wide front tube (9 long), top at the rear housing, front tube 0.5 lower.
- [ ] Two green struts lean, lower ends forward, at about 25 and 35 degrees, leaving a skewed open window under the scope.
- [ ] Scope bottom clears the receiver ridge by 2 to 3 units; scope is the tallest thing on the gun.
- [ ] Foregrip is a vertical 9-tall diamond-section post 4.2 wide, in a lighter green, at X 41 to 45.
- [ ] Pistol grip leans back about 25 degrees; the trigger guard is a closed loop (front plate at about 34 degrees, 6-long floor, grip).
- [ ] Magazine is behind the grip, gray, 2 wide, two segments bending forward, reaching Y 25.4, the lowest point of the gun.
- [ ] Rear body deepens toward a 4-wide, 12-tall butt plate (Y 9.5 to 21.5), with a diagonal underside.
- [ ] Gas tube is a 6-long thin diamond on the left side of the barrel's base only.
- [ ] Charging handle is a small separate part on the right side near X 40 to 42, poking out about 0.9 past the receiver.
- [ ] Palette: body green about #224333, foregrip about #3B775A, barrel and scope black (about #010101), magazine gray #606060.
- [ ] Four camo variants (Gray, Wood, Sand, Nether) are supported by swapping body and foregrip colors only.
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Charging handle side.** I read it as the right side (the side whose renders show the nub); the left view hides it. Either side is a harmless deviation if the builder keeps it on one side only, but the gas tube should be on the opposite side.
- **Strut direction.** From the numbers both struts lean with their lower ends forward. The side render shows a fan of two lines under the scope, which is consistent, but the lean is low-resolution and the exact angles may be off by a few degrees.
- **The two-stage scope.** The original's rear housing is thicker than the front tube. The overlay and flash numbers are read from formulas, not seen in game; treat the first-person offsets as approximate.
- **Cheek plate and neck block** are small filler pieces under the receiver; their exact outline is hard to separate from the diamond in renders. The overall underside slope (Y 12.5 at X 24 to Y 21.5 at the butt) matters, not the exact box breaks.
- **Texture noise** was sampled by region, not by pixel; the black parts have noise at the 1-in-256 level that is not worth reproducing.
- **Muzzle flash offset** (in the renderer's own units) cannot be turned into a model-space point with certainty; the barrel tip is the safe choice.

## Round 2 corrections (orchestrator review)

Round 1 comparison (side-view IoU 0.961, front 0.857, back 0.867, worst view below the 0.93 bar). Facts the builder must fix, in words:

- **Overall width.** The original's widest part across Z is 6.5 units (0.408 block); ours is 6.4. Width is right, so the front and back error comes from the *shape* of the front and back faces, not the total.
- **Front face.** The original's receiver front reads as one solid green slab with a single black diamond (the scope tube) above it and a small black block for the front sight. Ours shows two black vertical stripes running down the receiver front and a second small diamond overlapping the receiver. Remove the stripes: the front face of the receiver and of the light-green foregrip post below it are plain, unbroken green. The post is a solid lighter-green slab with no dark bands on any face.
- **Carry handle and scope.** In the side view the original has an open frame: a flat black rail along the top, with two thin black struts forming a hollow triangle under the scope, so the gap between the rail and the receiver top is see-through. Ours fills this with a solid green riser under the scope. Make it open again, and make the scope and its mount black, not green.
- **Magazine.** The original's magazine is a dark gray, mostly plain slab with only faint ribbing; ours is a light gray with strong stripes. Darken it and cut the contrast of the ribs by about half.
- **Muzzle end.** The original barrel is thinner and longer past the foregrip; it reads as a thin black needle. Ours is slightly thick near the muzzle.
- Keep the 0.402 to 0.408 width, the 4.375-block length, and the camo set.
