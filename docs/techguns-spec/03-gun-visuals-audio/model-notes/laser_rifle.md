# Laser Rifle: clean-room model notes

Design notes for rebuilding the Techguns **Laser Rifle** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout (64x64 is the native size).

Related facts: [ModelLasergun](../gun-models.md), [rendering framework](../rendering-framework.md), [Laser Rifle sheet](../../02-guns/sheets-energy-special.md#laser-rifle-lasergun).

**Units and axes used in this document.** One unit = one texel at the native 64x64 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention); **Y = 0 is the receiver's vertical mid-line, which is also the barrel axis**. **Z = 0 is the gun's mid-plane**; sizes across Z are total widths, centered, and positions across Z are given as +/- from the mid-plane. **X = 0 is a point on the receiver, 10 units in front of its rear face.** The gun is mirror-symmetric across the mid-plane: left and right sides are identical. Numbers are exact unless marked "about".

## 1. Gestalt

A long, heavy **sci-fi carbine with a skeletal wire stock**. At a glance you read: a thick, light-gray body and an even longer, light-gray **barrel built like a girder** (an H or double-T in cross-section) with **two bright red glowing windows** along each side; a **big dark battery cell** hanging under the body with a **red square window** on each side; a tall dark **gate-shaped rear sight with a red dot**; a stocky grip leaning back; and a thin, open **wire stock** ending in a tall butt plate. Light-gray metal dominates; dark gunmetal parts break it up; red is the only accent, and it is bright and glowing.

Defining features in order of visual importance:

1. **The barrel is a 16-long girder with two red glowing windows** on each flank, separated by a light cross-band in the middle.
2. **The red-windowed dark energy cell** under the receiver, a chunky box that sticks out 1 unit past each side of the body.
3. **A skeletal wire stock**: two thin rods and a tall butt plate, with the lower rod kinked up at an angle. Nothing fills the space between the rods.
4. **The gate rear sight** with a floating red dot, standing well above the top line.
5. **A short, hollow, plus-section muzzle tube** after a thick collar ring at the end of the barrel.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt plate back face to muzzle) | 53 (x -22 to 31) |
| Total height (rear-sight top to grip heel) | about 19 (y -7.5 to 11.5) |
| Total width | 6 (the barrel flanges, the collar, the energy cell) |
| Length : height : width | about 8.8 : 3.2 : 1 |
| Stock (butt plate to receiver rear) | 12 (x -22 to -10) |
| Receiver | 17 long (x -10 to 7) |
| Barrel assembly beyond the receiver (collar, girder, ring, tube) | 24 (x 7 to 31), about **45 percent** of the total length |
| Girder alone | 16 long (x 9 to 25) |
| Receiver height | 6 (y -3 to 3) |
| Energy cell | 8 long, about 6 tall (y 1 to 7), 6 wide; its base plate reaches y 8 |
| Grip | about 8.5 tall, leaning back about 20 degrees |

The **top line** is the receiver top (y -3). The barrel's side plates end at y -3 too, so the receiver and barrel top edges are flush. Above it: a dark rail 1 unit up (y -4 to -3), a sight base (to y -5), and the gate sight (to y -7.5). The muzzle tube's top is lower (y -2.5) than the barrel's side plates, so the muzzle steps down by half a unit.

## 3. Part by part

All parts are boxes (two are tilted). "Touches" means faces in contact or overlapping. Z widths are totals, centered on the mid-plane.

### Receiver
- One box, **17 long, 6 tall, 4 wide** (x -10 to 7, y -3 to 3, z +/-2). The main mass of the gun; light steel with a slight darker lower and rear shading.
- **Side strips:** two thin boxes, **15 long, 2 tall, 1 thick**, one per side, at x -8 to 7, y -1 to 1, on the outside of the receiver wall, proud of it by 0.5 (z +/-1.5 to 2.5). They are a darker, duller gray than the body and read as side panels.
- **Underplate:** a box **7 long, 1 tall, 3 wide** directly under the rear half of the receiver (x -9 to -2, y 3 to 4). Light-medium gray. The grip's top and the trigger attach here.
- **Rear step:** a small box **1 long, 2 tall, 3 wide** at the receiver's rear top (x -10 to -9, y -4.5 to -2.5): a lip that rises 1.5 above the receiver top right at its back edge. Light gray.
- **Top rail:** a dark box **14 long, 1 tall, 2 wide** lying on the receiver top, x -9 to 5, y -4 to -3. It is dark gray (#333333 on the sides, #4F4F4F on top), so the top of the receiver shows a dark center stripe 2 wide.
- Touches: the stock rods meet the receiver's rear face; the collar meets the front face; the energy cell and underplate meet the bottom.

### Gate rear sight
- **Sight base:** a dark box **6 long, 2 tall, 3 wide**, x -5 to 1, y -5 to -3, over the top rail (overlapping it by 1). Dark gray (#3A3A3A).
- **Two posts:** thin plates **3 long, 3 tall, 1 thick**, x -2.5 to 0.5, y -7.5 to -4.5, one at z -2 to -1 and one at z 1 to 2, so the gap between them is 2 wide and the outer width is 4. They stand on the base (overlap 0.5) and are dark gray (#3B3B3B to #474747).
- **Bridge:** a dark box **3 long, 1 tall, 2 wide** at the tops of the posts (x -2.5 to 0.5, y -7.5 to -6.5, z +/-1), closing the gate into an inverted U with a 2-wide, 2-tall opening beneath it.
- **Red dot:** a 1x1 square with no thickness (it faces forward and backward), at x -1, y -6.25 to -5.25, z +/-0.5, hanging in the middle of the opening just below the bridge. Pure red (#FF0000), drawn **fullbright** (a glow part).
- Touches: base-to-rail overlap; posts to base and bridge. Nothing floats.

### Grip
- One box, **3 wide (X), 8 tall, 2 thick (Z +/-1)**, hinged at its **top-front** point at about (-5, 4) and **tilted 20 degrees backward** (the lower end swings toward the stock). The tilted box spans x -10.6 to -5, y 3 to 11.5. Its top edge is buried in the underplate and receiver bottom (y 3 to 4).
- Dark olive-gray: sides about #4A4A45, top #63635C. A single flat box with no finger grooves or steps.
- **Trigger:** a tiny dark box **2 long, 1 tall, 1 wide** (x -6 to -4, y 4 to 5, z +/-0.5), tucked at the grip's top-front. Dark (#444440). There is **no trigger guard**.

### Energy cell (battery)
- **Housing:** a box **8 long, 6 tall, 6 wide** (x -1 to 7, y 1 to 7, z +/-3). It sticks out 1 unit beyond the receiver on each side, and its top 2 units are inside the receiver. Dark gunmetal: ends #2F2F2E, top #555555, bottom #5F5F5E; side faces #4E4E4D around the window.
- **Windows:** each side face (z +/-3) has a **4x4 square window**, centered at x 3 (x 1 to 5) and y 4 (y 2 to 6), leaving a frame 2 wide at the front and back and 1 tall above and below. The frame is a dark gray ring.
- **Glow core:** a box **4 x 4 x 6** (x 1 to 5, y 2 to 6, z +/-3) filling the window exactly, so the window is a solid, **fullbright red square** (#D60C0C to #DE0D0D) on both sides. Its top, bottom and ends are dark and hidden.
- **Base plate:** a box **8 long, 1 tall, 5 wide** (x -1 to 7, y 7 to 8, z +/-2.5). Very dark (#313130).
- Touches: housing overlaps the receiver bottom. The base plate is 0.5 narrower than the housing each side.

### Collar (receiver to barrel)
- A dark box **2 long, 4 tall, 6 wide** (x 7 to 9, y -2 to 2, z +/-3), color about #373735, with a thinner flange box above and below: **2 long, 1 tall, 5 wide** at y -3 to -2 and y 2 to 3 (z +/-2.5). Together the collar is 6 tall (y -3 to 3) and 6 wide in its middle, so its profile is a plus when seen from the side.
- It is darker than everything around it and reads as a clamp.

### Barrel girder (x 9 to 25)
All 16 long. The cross-section is an **H laid on its side**, built from four long parts:
- **Side plates:** two boxes **16 long, 6 tall, 1 thick**, x 9 to 25, y -3 to 3, at z +/-1 to 2 (so the plates are 2 apart and 4 wide outside). Light gray; their faces are the lightest on the gun (about #909089, with highlights to #9E9E99 on edges).
- **Flanges:** two boxes **16 long, 1 tall, 6 wide**, x 9 to 25, one at y -2 to -1 and one at y 1 to 2 (z +/-3). They overhang the side plates by 1 on each side. Light gray, slightly darker than the plates (#878780).
- **Glow windows:** two **fullbright red blocks** of **7 long, 4 tall, 4 wide** each (x 9 to 16 and x 18 to 25, y -2 to 2, z +/-2), filling the space between the flanges and flush with the outside faces of the side plates. What shows:
  - on each side face, a **red stripe 7 long and 2 tall** (y -1 to 1) per window, in a light-gray frame above and below it (the flanges);
  - on the top of the top flange and the bottom of the bottom flange, a **red stripe 7 long and 2 wide** (z +/-1) per window.
  - Red is about #DF0400 to #E60702, bright and slightly orange-red. The end faces of the glow blocks are transparent in the texture (nothing is painted).
- **Center band:** between the two windows (x 16 to 18) two light boxes: a vertical **2 long, 6 tall, 2 wide** post (y -3 to 3, z +/-1) and a horizontal **2 long, 2 tall, 6 wide** band (y -1 to 1, z +/-3). Light gray (#95958F) with no red, they read as a "knuckle" joining two barrel halves; the band sticks out 1 on each side like the flanges.
- Seen from the side the barrel is: light frame, 7-long red stripe, 2-long light block, 7-long red stripe.

### Muzzle ring and tube
- **Ring:** a light-gray box **2 long, 4 tall, 6 wide** (x 25 to 27, y -2 to 2, z +/-3), with a **2 long, 1 tall, 4 wide** rib above and below (y -3 to -2 and y 2 to 3, z +/-2). Same color family as the barrel (#888882 to #8D8D86).
- **Tube:** a short **hollow tube 4 long** (x 27 to 31). Its walls are four dark boxes: top and bottom plates **4 long, 1 tall, 3 wide** (y -2.5 to -1.5 and 1.5 to 2.5, z +/-1.5) and two side plates **4 long, 3 tall, 1 thick** (y -1.5 to 1.5, z +/-1.5 to 2.5). The section is therefore a **plus-shaped ring** (5 x 5 outside with the corners cut away, 3 x 3 bore). Dark gray (#333331 to #353533).
- The bore is open at the front, so the ring face behind it is visible down the tube. (See section 10 about the red emitter dot.)
- The muzzle is dark and visibly narrower (5 wide, 5 tall) than the barrel (6 by 6).

### Skeletal stock
- **Butt plate:** three stacked boxes, 2 thick (x -22 to -20): the middle one **6 tall, 4 wide** (y -2 to 4); a cap **1 tall, 3 wide** above it (y -3 to -2); and a foot **4 tall, 3 wide** below it (y 4 to 8). Total butt plate height 11 (y -3 to 8). Medium gray with a slight olive tint (#65655F to #7A7A73).
- **Upper rod:** a box **10 long, 1 tall, 2 wide** (x -20 to -10, y -2 to -1) from the plate to the receiver's rear face. Gray (#64645E).
- **Lower rod:** a straight section **4 long, 1 tall, 2 wide** (x -20 to -16, y 3.5 to 4.5), then a **4-long, 1-tall, 2-wide bar tilted 44 degrees** rising toward the front, hinged at (-16, 4.5) and ending at about (-13, 1.5), then a short **4 long, 1 tall, 2 wide** straight piece (x -13.8 to -9.8, y 1 to 2) that enters the receiver's rear face. All gray (#666660).
- The two rods are 10 units apart at the butt and converge toward the receiver, leaving a big open hole between them. The hole is the stock's main negative space.

## 4. Silhouette checks

- **Side (the main view):** a long horizontal body with a thin open stock to the left. From the left: a tall butt plate (11 tall, stepped), two thin rods enclosing a triangular-ish hole, the receiver block, a small rise at its rear-top, the gate sight above it, a grip leaning back below, the energy cell as a square-ish box under the front half of the receiver, then a long, flat-topped barrel of the same height as the receiver, a collar at its start, a ring and a narrower muzzle tube at the end. Negative space: the hole in the stock; the notch between the grip's front and the cell; no guard.
- **Top:** a long narrow body. The rod (2 wide) and butt (4 wide) at the back; a 4-wide receiver with 1-unit side strips (5 wide); the energy cell as a 6-wide bulge under the receiver (visible at the sides); then the barrel as a flat 6-wide strip with two red stripes and a light cross at the middle; a narrower muzzle tube at the end (5 wide).
- **Front:** a squarish 6x6 block (the barrel's H-section, a flange pair with a plus-shaped tube at the center), the gate sight rising 4.5 above it, the stock butt plate (4 to 3 wide, 11 tall) behind, and the grip below.
- **Back:** the 11-tall butt plate, with the gate sight and red dot above it, and the grip foot below.
- **Underside:** a long thin strip; the energy cell as a 6-wide patch; the grip; and the unbroken barrel.

## 5. Materials and palette

The original is a light gray metal gun with dark gunmetal furniture and bright red glow. Sampled from the texture's regions.

| Part | Material | Base | Highlight | Shadow / detail |
|---|---|---|---|---|
| Receiver | light steel | #898983 | #979791 | #6D6C65 (underside and one side) |
| Side strips | duller steel | #61615C (outer) | #6D6C67 (top) | #555551 (bottom) |
| Barrel side plates | light steel | #909089 | #9E9E99 | #86867F |
| Barrel flanges | steel | #878780 | #93938D (top) | #7A7972 (bottom) |
| Center band, post, ring | light steel | #95958F | #979790 | #888882 |
| Collar and its flanges | dark gunmetal | #373735 | #444443 | #30302E |
| Muzzle tube | dark gunmetal | #343432 | #373736 | #31312F |
| Top rail | dark gray | #333333 | #4F4F4F | #323232 |
| Sight base, posts, bridge | dark gray | #3A3A3A | #474747 | #363636 |
| Energy cell housing | dark gunmetal | #333331 | #555555 (top) | #2F2F2E |
| Cell base plate | very dark | #313130 | | #30302F |
| Grip, trigger | dark olive-gray | #4A4A45 | #63635C (top) | #444440 |
| Underplate, rear step | medium gray | #74736D | #777771 | #6D6D66 |
| Butt plate and stock rods | olive-gray | #666660 | #7A7A73 (foot) | #52524D |
| Glow windows | red, fullbright | #E20702 | #E60702 | #DF0400 |
| Cell window | red, fullbright | #DE0D0D | | #D60C0C |
| Sight dot | pure red, fullbright | #FF0000 | | |

Shading style:
- **Metal:** flat, with a few gray levels of noise per face, like a satin finish. The receiver and barrel are light; the "tech" parts (cell, collar, muzzle, rail, sights) are clearly darker, so the gun reads as a light body with a dark skeleton of details.
- **Glow:** pure saturated red, no gradient. The windows are crisp rectangles. They are drawn with lighting turned off so they stay bright in the dark.
- **Cell windows:** a 1-unit dark outline ring around the red square.
- No wear, no scratches, no logos.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units, after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 1.3 | offset (0, -0.5, 0) plus the idle shift (0.1, -0.25, -0.5). You see the gun side-on, turned 45 degrees toward the stock. |
| First person firing | the same, plus a slide along the gun axis of up to 0.25 block (backward) and back | the shift runs over the shot's animation. No roll. |
| First person reloading | roll about the Z axis up to 30 degrees (20 degrees times a 1.5 factor), yaw up to -15 degrees | plus a shift of up to (0.5, 0.25, 0.25) from idle, with a smooth rise and fall. |
| First person aimed | no scope and no zoom overlay on this model | |
| Third person (held) | yaw 225 degrees, scale 1.25 | offset (-0.7, -0.5, 0). |
| Third person aimed | extra offset (0, 0.1, -0.2) | the shared aimed rotation is applied first. |
| Ground (dropped) | yaw 90 degrees, scale 0.75 | lowered by 0.35. |
| Inventory icon | yaw 180 degrees, scale 0.55 | offset (-0.1, 0, 0); the icon is the 3D model, side-on. |

**Muzzle flash:** a laser-style additive flash sprite at 1.5 times scale, drawn in screen space near the muzzle in first person (offset about (-0.65, -1.0, -0.3), turned 90 degrees). In model terms the flash belongs at the front of the muzzle tube (x 31, y 0, z 0). The flash is a bright, slightly elongated star with a hot center, tinted red.

## 7. Motion

- The **model has no moving parts.** No glow pulsing, no charge-up animation, no sliding parts. All motion is whole-gun transforms.
- **Recoil (third person):** the gun slides back along its axis by up to 0.25 block in the first quarter of the first half of the animation, eases forward, and is still afterward.
- **Reload (third person):** the gun tips about 40 degrees about Z and turns 50 degrees about Y at mid-animation, then returns. Reload time is 45 ticks in the original; for energy weapons the GregGuns design may have no reload, see the milestone plan.

## 8. What to avoid

- **A thin pipe barrel.** The barrel is a 6x6 girder with flanges that overhang, not a round tube. Keep the H-section with the 1-unit overhang.
- **Red everywhere.** Red appears only in four places: the two barrel windows per side, the cell windows, the sight dot (and an optional emitter dot). The rest is gray.
- **A closed stock.** The area between the two stock rods is empty. Don't fill it.
- **A cell that is flush with the body.** The cell is 6 wide against the receiver's 4. It sticks out 1 on each side.
- **A pipe-shaped muzzle.** The tube is square, hollow and plus-shaped; the bore is 3x3.
- **A guard or magazine release.** The original has neither; only a tiny trigger stub.
- **Glow that dims with world light.** The red parts must look the same in a dark cave.
- Do not add things the original lacks: no scope, no handguard rails, no bipod, no vents on the barrel.

## 9. Acceptance checklist

- [ ] Length : height : width is within about 10 percent of 8.8 : 3.2 : 1.
- [ ] Barrel beyond the receiver (collar to muzzle) is about 45 percent of the length; the girder is 16 long.
- [ ] Barrel cross-section is an H: two 1-thick side plates 2 apart, and two 6-wide flanges overhanging by 1.
- [ ] Two red windows per side, 7 long and 2 tall each, separated by a 2-long light band; the same red appears on the top and bottom of the flanges as 7x2 stripes.
- [ ] A light cross-band at the barrel's middle that sticks out 1 on each side.
- [ ] A dark collar at the receiver-barrel joint and a light ring plus a hollow, plus-shaped dark tube at the muzzle (5x5 outside, 3x3 bore).
- [ ] Energy cell is 8 long, 6 wide and 6 tall, sticks out 1 unit beyond the receiver on each side, with a 4x4 red window on each side.
- [ ] Gate sight: two plates 2 apart, a bridge, and a red dot in the opening; top at y -7.5.
- [ ] A dark 2-wide top rail runs along the receiver top.
- [ ] Grip is a single box, 2 thick, leaning back about 20 degrees, with a tiny trigger stub and no guard.
- [ ] Stock is open: butt plate 11 tall and stepped, upper rod, and a kinked lower rod (44-degree rise), with a clear hole between the rods.
- [ ] All red parts are fullbright (drawn without world lighting).
- [ ] Metal is light gray (about #898983 to #909089); detail parts are dark (about #333331 to #3B3B3B).
- [ ] No part floats; every box overlaps or touches a neighbor.
- [ ] No moving parts in the model.

## 10. Unsure

- **Emitter dot.** One end face of the muzzle ring has a painted 2x2 red square with a dark outline, and the opposite end is plain. In the original the texture mapping is ambiguous: it could be on the rear (hidden) face. The sensible reading, and the one I recommend, is on the **front face, visible down the muzzle tube**, drawn as a normal (non-glowing) red dot. It is optional.
- **Rear step on the receiver.** It is a small 1x2x3 box; whether it is a sight stub or a bolt stop is unclear. Treat it as a minor bump.
- **First-person framing numbers** come from formulas read, not seen in game. Treat the offsets as approximate.
- **Third-person tilt hinge.** The reload and recoil rotations pivot about the model origin, which here lies on the receiver; wrist-relative placement was not verified.
- **Scale in first person.** The renderer's scale of 1.3 is a relative factor on a model 53 units long; the apparent size in hand was not measured.
