# Bio Gun: clean-room model notes

Design notes for rebuilding the Techguns **Bio Gun** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout (the original is on a 64x64 sheet, which is a hint about its level of detail, nothing more).

Related facts: [ModelBiogun](../gun-models.md), [rendering framework](../rendering-framework.md), [Bio Gun sheet](../../02-guns/sheets-energy-special.md#bio-gun-biogun).

**Units and axes used in this document.** One unit = one texel at the native 64x64 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention). **Y = 0 is the top face of the glowing core** (the top of the cage's inner glow). **X = 0 is the rear face of the cage** (where the glowing core starts, directly in front of the breech block). **Z = 0.5 is the gun's mid-plane**: almost everything is centered on z = 0.5 (the model is built about half a unit off zero). Sizes across Z are total widths. Angles are rotations about Z, with a positive angle swinging the lower end of a part toward -X (the rear). Rounded to the nearest half unit unless it matters.

## 1. Gestalt

A bulky, **green-and-gunmetal sci-fi rifle** with a visible **glowing green energy-fluid core** in a hexagonal cage, a **thin wire skeleton stock**, a flat **carry handle** on top, a stubby pistol grip leaning back and a **fat glowing canister hanging under the gun, tilted forward**. It should look like a biological-waste projector: dark industrial gray plates, a very dark-green fitting at the muzzle, and bright, fully lit green fluid showing through the middle and through the canister's windows. The glow is the whole point; if you remove the glow it reads as a dull gray rifle.

Defining features in order of visual importance:

1. **The bright-green glowing core** in the middle of the gun: a block 8 long, seen between six thin gray rails, with a glowing 1-unit disc just ahead of it.
2. **The tilted canister under the gun** (30 degrees, bottom forward), light gray casing with a glowing green window band on all four sides.
3. **The long skeleton stock** made of 1x1 dark-green rods, much thinner than the body, so it contrasts with the chunky front half.
4. **The open carry handle**: two thin rails on posts above the rear receiver.
5. **The muzzle block**: a short, wide, near-black dark-green drum with gray plates on top and bottom, wider than anything else.
6. **A rearward-leaning pistol grip** (20 degrees) under the receiver.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt plate to muzzle face) | **38** (x -23 to 15) |
| Total height, body only (top of carry handle to guard floor) | 11 (y -2.5 to 8.5) |
| Total height, including the tilted canister | about 18.4 (canister bottom corner at y about 15.9) |
| Total width (muzzle block and breech) | **7** (z -3 to 4) |
| Length : height (body) : width | about 5.4 : 1.6 : 1 |
| Stock (butt plate to receiver) | 10 long (x -23 to -13), 1 unit square rods |
| Receiver (rear cap to breech) | 9 long (x -13 to -4) |
| Breech block | 4 long (x -4 to 0) |
| Cage | 8 long (x 0 to 8), plus a 1-unit flange (x 8 to 9) and a 1-unit glowing disc (x 9 to 10) |
| Muzzle block | 5 long (x 10 to 15) |
| Pistol grip | about 7.5 long, lower end at y about 13.6 |
| Canister | 8 long, 4x4 section, axis from (x -2.2, y 8) at its top to about (x 1.8, y 14.9) at its bottom |

Rough proportions from the rear: a 10-long wire stock is about **26 percent** of the length; the receiver and breech about 34 percent; cage, flange and disc 26 percent; muzzle block 13 percent. The cage-plus-muzzle section (x 0 to 15) is the widest and chunkiest part; the stock is by far the thinnest (1 wide).

## 3. Part by part

Parts are boxes unless noted. All are centered on z = 0.5 unless a Z range is given. "Touches" means faces in contact or overlapping.

### Glowing core (cage interior)
- Two overlapping boxes, both **8 long** (x 0 to 8): one **6 wide x 3 tall** (z -2.5 to 3.5, y 0.5 to 3.5) and one **5 wide x 4 tall** (z -2 to 3, y 0 to 4). Together they make a plus-shaped (cross) cross-section, 6 wide at its midline and 4 tall at its center, inside the cage.
- **Rendered fully bright (self-lit)**, no shading, so it looks the same in the dark.
- A third glowing piece, the **disc**, is 1 long x 4 tall x 5 wide (x 9 to 10, y 0 to 4, z -2 to 3), sitting between the front flange and the muzzle block.

### Cage rails (six)
- Six thin bars, each **8 long x 1 x 1** (x 0 to 8), forming a hexagonal frame around the core:
  - **Top pair** at y -0.5 to 0.5, at z -1.5 to -0.5 and z 1.5 to 2.5.
  - **Side pair** at y 1.5 to 2.5, at z -3 to -2 and z 3 to 4 (these are the outermost, giving the cage its 7-wide stance).
  - **Bottom pair** at y 3.5 to 4.5, at z -1.5 to -0.5 and z 1.5 to 2.5.
- Cage outline is therefore **7 wide and 5 tall** (y -0.5 to 4.5), with the glow visible in the gaps between rails on all sides. The core's 6-wide section pokes slightly toward the side rails.
- Light gray metal (section 5).

### Front flange
- One plate **1 long** (x 8 to 9), shaped like a ring cap: a **3-tall x 7-wide** slab (y 0.5 to 3.5, z -3 to 4) with a **1-tall x 6-wide** strip above (y -0.5 to 0.5, z -2.5 to 3.5) and below (y 3.5 to 4.5). Outline 5 tall x 7 wide.
- Same light gray as the rails; it caps the cage and holds the rails together. The glowing disc is immediately in front.

### Muzzle block
- A short wide drum **5 long** (x 10 to 15): the main part is **3 tall x 7 wide** (y 0.5 to 3.5, z -3 to 4) and is nearly black, with dark-green hints; on top and bottom a **1-tall x 6-wide x 5-long** gray plate (y -0.5 to 0.5 and y 3.5 to 4.5, z -2.5 to 3.5). Total 5 tall x 7 wide.
- The muzzle block is the widest and tallest piece at the front; its outline matches the flange in size, just longer.
- No visible bore; no tapered nose.

### Breech block (rear of the cage)
- Three pieces stacked, all **4 long** (x -4 to 0): a main block **3 tall x 7 wide** (y 0.5 to 3.5, z -3 to 4), and **1-tall x 6-wide** plates above (y -0.5 to 0.5) and below (y 3.5 to 4.5), z -2.5 to 3.5.
- It looks like a mirror of the muzzle block's shape, but shorter (4 vs 5) and darker steel gray (not black-green).
- Touches: the cage's rear face on its front; the rear body on its rear.

### Rear body
- **6 long x 4 tall x 5 wide** (x -10 to -4, y 0.5 to 4.5, z -2 to 3). Dark charcoal. This is the receiver proper.
- **Top plate**: 8 long x 1 tall x 4 wide (x -12 to -4, y -0.5 to 0.5, z -1.5 to 2.5), lighter gray, covering the receiver and extending 2 units back over the rear cap.
- **Rear cap**, two stacked pieces: a **2 long x 4 tall x 4 wide** block (x -12 to -10, y 0.5 to 4.5, z -1.5 to 2.5), mid-gray; and behind it a **1 long x 4 tall x 3 wide** end piece (x -13 to -12, y 0 to 4, z -1 to 2), dark gray. They step in so the back of the receiver tapers.

### Carry handle
- Two parallel rails, each **8 long x 1 x 1** (x -11 to -3, y -2.5 to -1.5), at z -1.5 to -0.5 and z 1.5 to 2.5. They are 2 apart (open slot between them).
- Four short posts, each **1 x 2 x 1** (y -2.5 to -0.5): two at the front end of the rails (x -3 to -2, at both z positions) and two at the rear end (x -12 to -11, at both z positions). Posts land on the top plate (y -0.5).
- Dark green (the same tubing material as the stock).
- Reads as a hollow rectangular "bridge" 9 long, 2 tall (including post), with an open slot, 2 units wide, between the rails.

### Skeleton stock
All rods are **1 x 1** in section, centered on z about 0.5 (z 0 to 1), dark green. The butt plate is slightly wider.
- **Butt plate**: **1 long x 7 tall x 2 wide** (x -23 to -22, y 0 to 7, z -0.5 to 1.5).
- **Upper rod**: 9 long (x -22 to -13), at y 0.5 to 1.5, running straight from the butt plate's top to the receiver rear cap.
- **Lower rod**: 4 long (x -22 to -18), at y 5.5 to 6.5, running forward from the butt plate's bottom.
- **Strut**: 1 long x 4 tall (x -18 to -17, y 2.5 to 6.5), rising from the lower rod's front end up to the middle rod.
- **Middle rod**: 4 long (x -17 to -13), at y 2.5 to 3.5, from the strut to the rear cap.
- The result is a stock frame with one **triangular-ish window**: the top rod above, the middle rod and strut forming a step below, a small low hole between the lower and middle rods. From the side, the stock is open with three bars; from above it is a single thin rod 1 wide.

### Pistol grip
- **Root block**: 4 long x 3 tall x 3 wide (x -11 to -7, y 4.5 to 7.5, z -1 to 2), very dark gray, directly under the receiver's rear part.
- **Grip slab**: **3 wide (X) x 7 long x 2 thick (Z)**, hinged at its top corner (x -11, y 6, z -0.5 to 1.5) and **tilted 20 degrees backward** (lower end swings toward the rear). The lower end reaches about x -13.4 to -10.4 at y 13.6. Mid gray, a little lighter than the receiver.
- Overall angle from vertical: 20 degrees, leaning back. It is a plain slab, with no finger grooves.

### Trigger housing and guard
- **Housing**: 6 long x 3 tall x 4 wide (x -7 to -1, y 4.5 to 7.5, z -1.5 to 2.5), dark gray, hanging under the receiver and breech.
- **Guard floor**: 3 x 1 x 3 (x -7 to -4, y 7.5 to 8.5, z -1 to 2), dark green, under the housing's rear half. It makes a small ledge in front of the grip; there is no separate trigger.

### Feed hose (canister mount)
- A box **5 long x 2 thick x 5 wide** (z -2 to 3), dark green, **tilted 30 degrees** (the end farther forward sits higher). It runs from the underside of the breech (x about -0.9, y 5.2) down toward the rear to about (x -5.2 to -4.2, y 7.7 to 9.4). It is mostly hidden behind the canister's top and the housing; see section 4.
- Reads as a short sloping pipe between the housing and the canister.

### Canister (bio tank)
- A **4 x 4 section, 8-long** box (casing), pivoted at its top end (x -2.2, y 8, z 0.5) and **tilted 30 degrees** so its **lower end swings forward** (toward +X). Its axis runs from (x -2.2, y 8) to roughly (x 1.8, y 14.9). Light gray metal.
- Two glowing windows form a **cross of glow panels** through the casing, each 6 long (from 1 unit below the top to 1 unit above the bottom): one **5 wide (in the canister's X direction) x 3 thick**, one **3 x 5 thick (Z)**. Both are wider than the 4-wide casing, so the glow **bulges about 0.5 unit out on all four faces**. Rendered fully bright, like the core, with a gray bezel (frame) around the green.
- Bounding extents (rotated): x -3.9 to 3.5, y 7 to 15.9, z -2 to 3 (the glow panels' wider depth).

## 4. Silhouette checks

- **Side (the main view):** from the left, a thin three-bar wire stock with a taller butt plate; a chunky dark receiver with a flat bridge handle on top; a pistol grip sloping back and down at 20 degrees; a lighter hexagonal cage with a visible bright-green core (two rails at top, two at bottom, green between them); the flange, the glowing disc and a thick near-black muzzle block that is taller than the cage; under the gun, the **canister tilted forward** with a bright window band; the dark-green hose running diagonally between the housing and the canister. Negative space: the open slot under the carry handle; the window in the stock; the gap between grip and canister.
- **Top:** a thin 1-wide rod at the rear (stock), widening at the rear cap, the receiver and breech (about 5 to 7 wide), a bright-green rectangle (the core between two top rails with the side rails outside) in the middle, then the near-black 7-wide muzzle block. The carry handle shows as two parallel rails on the receiver. The canister is hidden except where it sticks out past the cage.
- **Front:** a 7-wide x 5-tall dark block (muzzle) with the gray top and bottom plates. Behind it the cage and the tank are hidden. The canister's bottom pokes out below, about 5 wide, bright green.
- **Back:** the butt plate (2 wide, 7 tall) in front of a mass of receiver (5 to 7 wide) and the muzzle block's outline beyond. The grip is a narrow column below.

## 5. Materials and palette

All values are sampled from the original texture. Two styles share the sheet: **dull gunmetal gray** with a slight green cast, and **saturated green** for the glow and tubing.

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Glowing core (both boxes) | self-lit green fluid | #54FF54 | #90FF90 up to #B2FFB2 | #19FF19 (still saturated, never dark) |
| Glowing disc | green with gray rim | #28FF28 to #87FF87 | #B0FFB0 | gray bezel #54594E, #6C7065 |
| Cage rails and front flange | light green-gray metal | #9FA495 (flange #A5A99A) | #AFB4A5, up to #B4BAAA | #959A8B |
| Muzzle block, main body | near-black green | #080808 / #01350B | #01380B | |
| Muzzle block, gray plates | mid-gray metal | #5D6155 | #6B6E63 | #515449 |
| Breech block | dark gunmetal | #3B3E36 | #5A5F53, #5E6357 | |
| Rear body (receiver) | charcoal | #40433A | #51544C | #31342D |
| Receiver top plate | lighter gray | #616659 | #848A7B | #515449 |
| Rear cap pieces | dark to mid gray | #5E6255 | #747A6A, #84887A | #3B3D35 |
| Pistol grip slab | mid gray | #5C6154 | #6E7367, #777D6D | #575D50 |
| Grip root block and trigger housing | very dark gray | #3E4138 | #4F524A | #31342D, #181916 (deepest corners) |
| Stock rods, butt plate, carry handle, posts, guard floor | dark industrial green | #093D13 | #1B4B24, up to #1B5326 | #01340B |
| Feed hose | green with gray flecks | #1B4E25 | #494C43 (gray bits) | #012E0A |
| Canister casing | light gray metal | #A0A696 (#A5AA9A) | #A8AD9D, #ACB2A2 | #54584C (edge rows) |
| Canister windows | self-lit green | #67FF67 | #B2FFB2 | #23FF23 |

Shading style:
- **Gray metal:** flat, low-contrast with 3 to 4 levels of noise; panel-seam lines are a darker gray, usually 1 texel wide, with light edges on top faces. No gradients, no scratches. The receiver is clearly darker than the cage and canister.
- **Glow:** the fluid has visible **mottling** (bright and slightly darker greens in the same pixel patch), like bubbling, and the gray metal bezel frames the green on the canister and the disc.
- **Green tubing:** flat deep green, hint of a lighter edge on the top rows.
- **Self-lit parts** are drawn without light-level shading, so they ignore ambient darkness. Everything else is shaded normally.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 2.0 | idle offset (0, -0.35, -0.4). While the use button is held (charge), the gun slides back along its own axis by up to 0.25 over the full charge (30 ticks), so the gun pulls toward the camera as it charges. |
| First person firing | slide back and return along the axis by up to 0.25, a roll of up to 7.5 degrees about the Z axis at mid-animation | the pulse is symmetric: 0 at the start, maximum at the middle, 0 at the end. |
| First person reloading | roll up to 20 degrees, yaw up to -10 degrees at the peak | translate up to (0.5, 0.25, 0.25) relative to idle at the peak; the peak lasts from about 27 percent to 73 percent of the reload. |
| First person aimed | no scope and no zoom overlay | |
| Third person (held) | yaw 225 degrees, scale 1.5 | offset (-0.35, -0.7, 0). |
| Third person aimed | extra offset (0, 0, -0.15) | |
| Ground (dropped) | yaw 90 degrees, scale 1.0 | offset (0.30, -0.65, 0). |
| Inventory icon | yaw 180 degrees, scale 0.8 | offset (0.2, 0.1, 0). Side-on, the 3D model itself. |

**Muzzle point:** the front face of the muzzle block on its axis: x = 15, y about 2, z = 0.5. The original has **no muzzle-flash sprite** for this gun (it is disabled), so the glow is the only light effect. Impact and shot visuals belong to the projectile (a green blob), not the model.

## 7. Motion

- The **model has no moving parts**; the canister, grip and core are static.
- **Recoil (third person):** the whole gun pitches up about 30 degrees around a point about 0.9 behind the model's origin and 0.3 up, in the first quarter of the first half of the animation, then settles back.
- **Reload (third person):** the gun tilts about 30 degrees about its long axis and 40 degrees about the vertical at mid-animation, then returns. Reload time is 45 ticks (2.25 s).
- **Charge:** first-person only, as described in section 6 (the gun slides back while the button is held).
- **Optional (not in the original):** the builder may keep the core, disc and canister windows as separate named parts, so later code can pulse their brightness with charge or ammo. The rest pose must match this document.

## 8. What to avoid

- **A dull gray rifle with no glow.** The core, the disc and the canister windows must be fully bright green and visibly different from the shaded gray.
- **A wide, chunky stock.** The stock is only 1 unit thin: dark-green rods with a taller butt plate. Do not use a solid stock block.
- **A vertical canister.** It tilts 30 degrees with its bottom forward. It is not a straight hanging magazine.
- **Rounding or tapering the muzzle.** It is a flat-faced, wide drum.
- **Gray everywhere.** The tubing parts (stock, handle, hose, guard floor) are dark green, not gray.
- **A forward-leaning pistol grip.** The grip leans back (20 degrees).
- **Extras the original lacks:** no scope, no rail, no flash hider, no visible trigger, no magazine well in front of the grip.

## 9. Acceptance checklist

- [ ] Length : body height : width is within about 10 percent of 38 : 11 : 7, and the canister adds about 7 units below the body.
- [ ] The glowing core is 8 long, a plus-shaped cross-section (6 wide at mid, 4 tall), self-lit bright green, seen between six gray rails.
- [ ] Six rails form a hexagonal cage 7 wide x 5 tall; the outer pair at mid-height are the widest.
- [ ] A 1-long flange and a 1-long glowing disc follow the cage, then a 5-long, 7-wide, near-black muzzle block with gray top and bottom plates.
- [ ] The breech block (4 long) sits behind the cage with the same stacked profile, in steel gray.
- [ ] The receiver is darker than the cage, with a lighter top plate and a stepped two-piece rear cap.
- [ ] The carry handle is two 8-long rails, 2 apart, on four 2-tall posts.
- [ ] The stock is a wire frame of 1x1 dark-green rods (upper, middle, lower, strut) 10 long, ending in a 7-tall x 2-wide butt plate.
- [ ] The pistol grip is a 7-long slab leaning back 20 degrees, mid gray.
- [ ] The canister is a light gray 4x4x8 casing tilted 30 degrees forward, with a green window band that bulges 0.5 on all four sides and is self-lit.
- [ ] A dark-green hose (5 long, tilted 30 degrees) links the trigger housing and the canister top.
- [ ] Dark-green tubing (stock, handle, hose, guard floor) and near-black muzzle are distinct from the gray metal.
- [ ] No part floats; every box overlaps or touches a neighbor.
- [ ] Side silhouette matches section 4 (three-bar stock, handle bridge, canister under the gun).

## 10. Unsure

- **Canister tilt direction.** My arithmetic from the original's angles puts the bottom of the tank forward (toward the muzzle), and the rest of the layout agrees (the hose slopes the other way to meet it). If a render shows it leaning back, flip the sign.
- **Which side the canister windows face.** Both glow panels are visible from all four sides; I did not confirm which has the gray bezel on the long faces.
- **Exact disc/flange order.** The glowing disc is at x 9 to 10 and the flange at 8 to 9; the flange overlaps the disc's rear by nothing, so the two touch.
- **Self-lit rendering.** The glow parts are drawn with full-bright lightmap and no light shading, with no blending; whether the original also uses a bloom is not visible in the sources. Treat as plain full-bright.
- **Charge slide figures** (0.25 block over 30 ticks) come from the renderer's formula, not a recording.
