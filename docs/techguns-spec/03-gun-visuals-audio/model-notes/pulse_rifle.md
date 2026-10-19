# Pulse Rifle: clean-room model notes

Design notes for rebuilding the Techguns **Pulse Rifle** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelPulseRifle](../gun-models.md#modelpulserifle-12864-family-x-72-boxes-1-part), [Pulse Rifle sheet](../gun-sheets.md#pulse-rifle-pulserifle-field-pulserifle), [rendering framework](../rendering-framework.md), [revolver notes (template)](revolver.md).

**Units and axes used in this document.** One unit = one texel at the native 128x64 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention). **X = 0 is the rearmost point of the stock's butt pad.** **Y = 0 is the top of the scope's highest step.** **Z = 0 is the gun's mid-plane**; sizes across Z are total width, centered. Values are rounded to the nearest half unit unless the exact figure matters. (This frame is simply shifted from the original model's own origin; use it as given.)

## 1. Gestalt

A **bullpup-style sci-fi assault rifle**: a long, slab-sided, pale ice-blue upper body with a dark, boxy lower half, a stubby dark barrel and muzzle device at the front, a scope block riding in the middle of the top, and a **large triangular open frame under the receiver** (a thumbhole-like loop formed by an angled pistol grip, a rear strut and a flat bottom bar). At a glance you read: **two tones**, light ice-white on top and near-black blue-gray underneath; a **single red stripe** wrapped around the front of the receiver; a short dark barrel poking out of a chunky handguard; a small angled foregrip hooked under the handguard; and a dark scope housing with a tiny **glowing cyan lens dot**. It is "futuristic marine rifle": clean, stepped, blocky, no curves except the tilted grip and strut slabs.

Defining features in order of visual importance:

1. **Two-tone body.** Pale mottled ice-blue upper (spine, receiver, scope body, top rails) over a dark gunmetal lower half (stock underside, grip, loop frame, handguard underside).
2. **The red stripe** (about 1 unit wide) wrapped around the front receiver block on all four sides: the only saturated accent besides the lens.
3. **Triangular grip loop** under the middle of the gun: slanted pistol grip, slanted rear strut and a flat bottom bar, with open space inside.
4. **Stepped scope tower** on top, between two dark end bells, with a cyan lens dot.
5. **Short black barrel and muzzle block** with two flanking prongs, ending a chunky light handguard that is capped by a dark plate.
6. **Slab butt stock** that is as thick as the body is wide (5), with a slightly raked butt pad.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt pad to muzzle face) | **48** |
| Total height (scope top to lowest point of the rear strut / mag-like slab) | about **18.3** |
| Total width | **5** (stock, front receiver block, scope-tower rear and the plain receiver cap are 5; most other parts are 2 to 4) |
| Length : height : width | about 9.6 : 3.7 : 1 |
| Muzzle device + barrel past the handguard cap | muzzle block ends at x 48; the visible barrel is x 33 to 42 and the muzzle device x 42 to 48 |
| Spine (upper receiver bar) | 24 long (x 2 to 26), 2 tall (y 6.5 to 8.5), 4 wide |
| Stock | x 0 to about 14, a tall slab 7 high at the butt pad |
| Scope tower | x 16 to 26.5, y 0 to 4 |
| Lower loop | x 12.5 to 27.5, y 8.5 to 18.3 |
| Foregrip | x 30 to 37, y 11 to 16 |

Layout along the length from the rear (x):
- 0 to about 3: the butt pad and stock core.
- 3 to 14: stock body and cheek.
- 12 to 19: the receiver rear and the first scope mount.
- 16 to 26.5: the scope tower, with its stepped body at x 19 to 26.
- 19 to 26: the receiver cap (5 wide).
- 26 to 33: the red-striped front receiver block (5 wide).
- 33 to 40: the handguard (4 to 5 wide).
- 33 to 42: the barrel.
- 42 to 48: the muzzle device.

The **top line** is stepped, not flat: the scope tower is the highest point (y 0), the receiver tops sit at y 4 to 5, the handguard top at y 4.5 to 5 and the barrel centerline at y 7.

## 3. Part by part

All parts are boxes. Z widths are centered on the mid-plane. "Light" means the ice-blue family; "dark" means the gunmetal and black family (section 5). Tilt angles are about the Z axis (the side-view tilt). Positive tilt = the part's lower end swings toward the rear (toward x = 0).

### Muzzle device and barrel (all dark or near-black)
- **Barrel:** a 2x2 square bar turned 45 degrees (a diamond, 2.8 across both ways), **9 long** (x 33 to 42), axis at y 7 (it runs y 5.6 to 8.4). It starts buried in the handguard and ends where the muzzle device begins.
- **Muzzle neck:** two short 2-long, 1x1 bars, one flat and one turned 45 degrees (together they read as an eight-point star section), x 42 to 44, centered at y 7. They bridge the barrel and the muzzle block.
- **Muzzle block:** 4 long x 2 tall x 2 wide, x 44 to 48, y 6 to 8, centered on the mid-plane. This is the frontmost solid.
- **Muzzle prongs:** two 3-long, 1x1 bars on either side of the block's front, x 45 to 48, y 6.5 to 7.5, at z about -1 and +1 (one each side, each overlapping the block's flank by half a unit). Together the block and prongs read as a 3-wide, 2-tall forked muzzle. No taper, no hole drawn.
- **Gas block:** a tiny 1x1x1 diamond (1.4 across) under the barrel at x 40.5 to 41.5, y 8.8 to 10.2.

### Handguard and front receiver (light with dark underside)
- **Front receiver block:** 7 long x 4 tall x **5 wide**, x 26 to 33, y 5 to 9. Light. This is the block that carries the red stripe (section 5). It is the widest piece along with the receiver cap and the stock core.
- **Handguard core:** 7 long x 3 tall x 4 wide, x 33 to 40, y 5 to 8. Light.
- **Handguard side plates:** two 2-long x 3-tall x 1-wide plates, x 33 to 35, y 6 to 9, one on each side (z about +/-2 to 2.5), so the handguard front is 5 wide at its rear half. Light.
- **Handguard lower:** 10 long x 2 tall x 4 wide, x 29.5 to 39.5, y 8 to 10, **dark**, under the core and extending back under the front receiver block. Beneath it, a 10 x 1 x 3 dark plate at y 10 to 11 (same x range), and a 1-wide x 2-tall x 4-wide dark step at x 28.5 to 29.5, y 9 to 11 at the rear end of this stack, with a 1x1x3 dark bar at y 10.5 to 11.5.
- **Handguard end cap:** a thin dark plate **1 long x 7 tall x 3 wide**, x 39.5 to 40.5, y 4.5 to 11.5. It stands upright across the front of the handguard and is taller than the handguard core on both ends (the barrel passes through its middle).
- **Top rails of the front receiver:** a light 5x1x3 bar at x 33 to 38, y 4.5 to 5.5, and a dark thin 11x1x2 bar at x 27 to 38, y 3 to 4 (a sight rail running over the receiver and handguard, dark, narrow).

### Receiver and scope tower
- **Main spine:** 24 long x 2 tall x 4 wide, x 2 to 26, y 6.5 to 8.5. Light. It is the long backbone of the whole gun; stock and receiver stack hang from it.
- **Receiver cap:** 7 long x 2 tall x 5 wide, x 19 to 26, y 5 to 7. Light. It is the widest part of the top, 0.5 wider than the spine each side.
- **Receiver stack above the spine, from front to back:**
  - a 14x1x4 light bar at x 19 to 33, y 4 to 5;
  - an 8x1x4 light bar at x 19 to 27, y 3 to 4;
  - a 7x1x4 light bar at x 19 to 26, y 2 to 3;
  - the scope tube cap: 6 long x 2 tall x 4 wide, x 19 to 25, y 0 to 2, light (the highest step).

  Together these make a **stepped pyramid**: each step up is 1 unit and ends 0.5 to 1 unit sooner in the rear-to-front direction, with the top step 6 long.
- **Rear receiver stack:** a 7x2x4 light block at x 12 to 19, y 4.5 to 6.5, and an 8x1x3 light bar at x 11 to 19, y 3.5 to 4.5. Behind it, a 2-long x 2-tall x 3-wide light block at x 10 to 12, y 4.5 to 6.5, and a thin 1x1x4 bar at x 11 to 12, y 5.5 to 6.5. They form a low rear "hump" in front of the stock.
- **Stock-top bars:** an 8x1x3 light bar x 2 to 10, y 5.5 to 6.5 over the stock, a 6x1x4 bar at x 3 to 9, y 5.5 to 6.5 and a dark 6x1x3 bar at x 3 to 9, y 5 to 6 (a dark inset strip in the stock top).
- **Scope end bells (dark):**
  - **Rear (eyepiece) bell:** 2 long x 3 tall x 3 wide, x 17 to 19, y 0.5 to 3.5. **It carries the cyan lens dot** (section 5).
  - **Front bell:** 2 x 3 x 3, x 24.5 to 26.5, y 0.5 to 3.5. Plain dark.
  - **Rear mount frame**, a rectangular dark frame behind the eyepiece bell: top bar 2x1x4 at x 16 to 18, y 0 to 1; bottom bar 2x1x4 at x 16 to 18, y 3 to 4; and two 2x2x1 side plates at x 16 to 18, y 1 to 3, z about +/-2 (outside faces at 2.5). The frame is open in the middle (the bell fills it).
- **Front sight wedge** (the slanted dark piece on the front rail): a 3x1x2 dark slab turned about 36 degrees at x 37.4 to 40.4, y 3 to 5.6. It sits on the top rail ahead of the front receiver block like a small front-sight wedge: it rises toward the front.

### Lower loop and grips (all dark unless noted)
- **Pistol grip:** a 3-wide-in-X, 9-long, 2-thick slab turned about **35 degrees**, so its lower end sits rearward. Top near x 22.5, y 8.2; bottom near x 17 to 19, y 17.3. Overall x 16.3 to 23.9, y 8.2 to 17.3. Dark #333840 family.
- **Grip front extension:** a 1x5x2 slab, also tilted 35 degrees, x 23.8 to 27.5, y 12.4 to 17.1 (the trigger guard front, a thin dark slab leaning the same way).
- **Trigger group:** a 3x2x2 dark block at x 23 to 26, y 8.5 to 10.5 (the trigger housing), a small 2x2x1 slab at x 22 to 24.6, y 9.6 to 12.3 tilted about 23 degrees (the trigger blade, **1 wide**, darker #272727), and a flat guard floor of zero thickness (3x2 plane) tilted -45 degrees at x 24.5 to 26.6, y 10.4 to 12.5, plus a second flat 3x2 plane at y 12.5 (x 21.5 to 24.5). The two planes are invisible edge-on.
- **Under-receiver rail:** a thin 9x1x2 dark bar, x 14 to 23, y 8.5 to 9.5.
- **Rear strut** (the third side of the triangle): three stacked dark slabs: a 2x4x2 vertical at x 12.5 to 14.5, y 9 to 13; a 2x4x2 slab tilted 35 degrees at x 12.6 to 16.5, y 8.4 to 12.8; and a 2x5x2 slab tilted 38 degrees the other way (lower end toward the front) at x 12.5 to 17.2, y 11.8 to 16.9.
- **Bottom of the loop:** a 3x3x2 dark block at x 15.5 to 18.5, y 14.5 to 17.5 and a 7x1x2 dark flat bar at x 18 to 25, y 16.5 to 17.5 (the floor of the opening).
- The loop's open space (the thumbhole) is a triangle bounded by the spine's underside (y 8.5), the pistol grip's rear edge and the rear strut. **It must stay see-through** from the side.
- **Front grip:** hangs from the handguard's underside. A 7x1x2 dark plate at x 30 to 37, y 11 to 12; a 1x3x2 dark post at x 30 to 31, y 12 to 15; a 2x1x2 dark foot at x 31 to 33, y 14 to 15; **three 5-long slabs turned 36 degrees** (lower end toward the rear), all x 32.4 to 37, y 11.2 to 16: a dark one (y 11.2 to 14.9, 2 wide), a **light** one (y 12.3 to 16, 2 wide) and a wider light one (y 11.8 to 15.5, 3 wide); plus light 1x1x3 and 2x1x3 bars at x 30.5 to 33, y 14.5 to 15.5 and a light 2x1x2 at x 31 to 33, y 15 to 16. The foregrip reads as a **small swept hook** under the handguard, partly dark and partly pale (the pale part is the lowest and the most forward).

### Stock and rear frame
- **Butt core:** 2 long x 5 tall x **5 wide**, x 0.2 to 3, y 6 to 11.3, light, tilted about **-10 degrees** (face leaning: the top is rearmost, the bottom sits about 1 unit farther forward).
- **Butt top plate:** 2x1x4, same tilt, x 0 to 2.2, y 5 to 6.3; **butt bottom plate** 2x1x4, same tilt, x 1.1 to 3.2, y 10.9 to 12.2. The pad face is therefore about 7 high and 5 wide at the middle.
- **Cheek and body:** light 3x2x4 at x 2 to 5, y 8.5 to 10.5 and a light 9x1x4 shelf at x 5 to 14, y 8.5 to 9.5. A light 2x1x4 at x 11 to 13, y 9.5 to 10.5.
- **Lower stock (dark):** 3x1x3 at x 2 to 5, y 10.5 to 11.5; 6x1x3 at x 5 to 11, y 9.5 to 10.5; 5x4x2 at x 5 to 10, y 9.5 to 13.5; 2x1x3 at x 10 to 12, y 10.5 to 11.5.
- **Stock strut (dark):** a 5x5x2 slab tilted about 15 degrees at x 5 to 11, y 12.2 to 18.3 (the lowest point of the gun at its tip), a 3x3x2 dark block at x 15.5 to 18.5, y 14.5 to 17.5 and the 7x1x2 floor bar (see above). These three form the bottom edge of the gun: a long dark heel that sweeps toward the rear.

Every dark part overlaps the light spine or the neighboring dark part: nothing floats.

## 4. Silhouettes per view

- **Side (main view):** a long horizontal rifle, 48 x 18. Reading from the stock (left in a muzzle-right view): a tall slab butt with a raked pad; the stepped top with the scope tower as the one bump (y 0), flanked by two dark bells; the red-striped light receiver block; a chunky handguard ending at an upright dark cap taller than the handguard; a black barrel and a forked muzzle block beyond; a small hook-shaped foregrip under the handguard; and under the middle a **big triangular open loop** (the grip, rear strut and flat bar). Negative space: the triangle opening; the gap between the loop bottom and the foregrip; the stepped notch in front of the scope tower; the barrel gap past the cap.
- **Top:** a narrow spine (4 wide) with 5-wide bulges at the stock, receiver cap and front receiver block; the scope tower stepped in the middle; a thin dark top rail; the handguard (4 to 5 wide) ending at the 3-wide cap; the barrel (2.8 wide) and the forked muzzle (3 wide) beyond. Red stripe visible as a thin cross-band on the front receiver block.
- **Front (from the muzzle):** a small dark forked muzzle in the center, over the handguard cap (3 wide x 7 tall), over a pale 4 to 5 wide body; the dark scope tower peeks 3 units above the rest; the foregrip and loop frame hang below as a dark column about 2 to 3 wide.
- **Back (from the shooter):** a 5 x 7 butt pad slab (light, with a thin dark top and bottom strip), the scope's rear mount frame with the **cyan lens dot** in a dark square visible above it, the dark pistol grip as a column below.
- **Underside:** a long narrow bar; the dark loop frame, foregrip and handguard underside are black; the front receiver block shows its red band as a thin cross stripe.

## 5. Materials and palette

The material is a **matte painted composite**: pale mottled ice-blue upper, black-blue gunmetal lower. There is no gloss. The measured values come from the default texture (128x64).

| Part | Base | Highlight | Shadow / detail |
|---|---|---|---|
| Spine, receiver stack, scope tube cap, handguard core, stock cheek (light parts) | #BCE2E5 | #E3F3F4 (on top faces and edges) | #97AEAF |
| Top rail bars (stepped pyramid) | #C3E5E8 | #DEF1F2 | #AFD5D8 |
| Scope tube cap | #CAE8EA | #E1F2F3 | #ACC5C7 |
| Handguard side plates, handguard core | #B6DCDF | #C3E5E8 | #AFD5D8 |
| Front receiver block (light faces) | #CBE9EB | #DFF2F3 | #AAD0D3 |
| Red stripe (wraps the front receiver block) | #CA0000 | #F00000 | #BD0000 |
| Dark lower stock, handguard lower, pistol grip, rear strut | #32373F to #383D45 | #3A3F47 | #2F343C |
| Magazine-like slabs (stock strut, bottom block, floor bar) | #333840 | #3A3F47 | #2E333B |
| Foregrip plates (dark) | #333840 | #373C44 | #31363E |
| Foregrip plates (light) | #CEEAEC | #E1F2F3 | #BCE2E5 |
| Scope end bells, mounts, top rail (dark) | #2B2B2B | #2E2E2E | #222222 |
| Handguard end cap, lower handguard plate, grip step | #2C2C2C | #353A42 | #292929 |
| Trigger group, guard planes | #282828 | #32373F | #252525 |
| Stock inset strips (dark) | #292929 | #2D2D2D | #262626 |
| Barrel, muzzle block, prongs, gas block | #191919 | #1B1B1B | #161616 (some faces pure #000000) |

**Glow and emissive parts (painted in the texture, with no lighting trick in the original renderer):**
- **Cyan lens dot.** A 3x3-pixel mark on the scope's rear (eyepiece) bell, on the face that looks toward the shooter: center pixel **#00FFFF**, four orthogonal neighbors **#008080**, corners the bell's dark color. It reads as a tiny glowing cyan sensor. Treat it as an **emissive** element in GregGuns (full brightness at any light level).
- **Red stripe.** The 1-unit-wide band (#BD0000 to #F00000, bright pure red) wrapped around the front receiver block, on the top, bottom and both sides, about 4 units in from the block's rear end (x 30 to 31). It is painted flat, not glowing in the original, but it is the second accent color; making it emissive is optional.
- No other glow. The flash at the muzzle is the shared blue muzzle-flash sprite (section 6).

**Camo variants:** 2 camo variants (default plus 2 alternates, 3 textures in all):

| Variant | Light parts | Dark parts | Accents |
|---|---|---|---|
| Default | ice-blue #BCE2E5 | gunmetal #333840 | cyan lens #00FFFF/#008080, red stripe #CA0000 |
| **Gray** (1) | neutral light gray #C8C8C8 (highlight #E7E7E7, shadow #838383) | near-black #1E1E1E to #272727, barrel and bells #070707 to #0C0C0C | **yellow** stripe #F2E323 to #FDFF2C, lens dot not saturated |
| **Dark Green** (2) | almost-black olive #10130D to #12140E | dark moss green #17240E to #202D17 (grip, stock, handguard lower) | **red** lens dot #FF0000 center / #800000 cross, **no stripe** |

In the Dark Green variant even the "light" parts are near-black green, so the gun reads dark overall with only the green lower parts and the red lens showing.

Shading style:
- **Light parts:** a soft **mottled cloud** noise of 3 to 5 levels (an icy, slightly noisy white-blue), with the lightest highlights on top faces. The surface has a faint dither of a few lighter and darker pixels, not stripes or scratches. A thin darker rim runs along some bottom edges.
- **Dark parts:** near-flat, very subtle noise of 2 to 3 levels. A little blue in the gunmetal (#333840 reads as blue-gray, not neutral).
- **Barrel and muzzle:** almost flat near-black with a few pure-black pixels.
- **Scope bells and rails:** neutral dark gray (no blue tint), flat.
- A thin 1-pixel gray line sits along the top face of the spine and one other long bar.
- Four small transparent holes are punched in the texture near the stock's cheek faces; they are not needed. (The builder should not make holes in the model.)

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, **scale 2.1** | idle offset (0.70, -0.30, -0.60). The camera sees the gun nearly side-on, turned 45 degrees so the receiver's side and the butt are visible. |
| First person firing | roll about Z of up to -7.5 degrees (a quick spike, then back) | plus a backward kick of up to 0.15 along the gun axis. A short, tight flip. |
| First person reloading | roll up to 25 degrees, yaw up to -15 degrees | shifts from idle by up to (+0.5, +0.25, +0.25) at the peak of a smooth rise and fall (the peak is held flat for a short stretch). |
| First person aimed | scoped: a 0.35 zoom multiplier and a full-screen tech-scope overlay | the model's idle offset shifts about (0, +0.1, +0.2). The rifle has a scope. |
| Third person (held) | yaw 225 degrees, **scale 1.25** | offset (-0.85, -0.6, 0). The grip sits in the hand, the muzzle points forward. |
| Third person aimed | extra offset (0, +0.1, +0.2) | |
| Ground (dropped) | yaw 90 degrees, scale 0.8 | lowered by 0.6. |
| Inventory icon | yaw 180 degrees, scale 0.7 | offset (-0.2, -0.3, +0.1); drawn side-on, filling most of the slot. |

**Muzzle flash:** the shared **blue** muzzle-flash sprite, about 1.75 times scale, drawn at the muzzle. In model space the muzzle face is at x 48, y 7, z 0. Note the flash is blue, matching the cyan accent.

## 7. Motion

- The **model has no moving parts.** Nothing slides, spins or glows on a timer. All motion is whole-gun transforms.
- **Recoil (third person):** the gun pitches up to 15 degrees about a point about 0.9 block behind and 0.3 up from the model origin, reaching peak in the first quarter of the first half of the animation, then settling back.
- **Reload (third person):** the gun tilts about 15 degrees and yaws about 20 degrees at mid-animation, then returns. Reload sound is 2.22 s.
- **Optional (not in the original):** the builder may keep the foregrip, scope and trigger as separate named parts so later code can animate them, but the rest pose must match this document. The cyan lens may pulse in GregGuns; keep its rest value as the default.

## 8. What to avoid

- **A single-color gun.** The two-tone ice-blue and dark layout is the identity. Do not paint it all blue-gray.
- **A smooth, rounded scope.** The scope is a stepped pyramid with two dark end bells, not a cylinder.
- **A closed loop.** The triangle under the receiver must stay open.
- **A long barrel.** The barrel is short (9) and almost hidden by the handguard; the muzzle block is only 6 past the barrel's start of exposure.
- **No red stripe or a stripe on only one face.** It wraps top, both sides and the bottom of the 5-wide block.
- **A floating foregrip.** Overlap the foregrip's top plate into the handguard's lower plate by at least 0.5 unit.
- **Glowing everything.** Only the lens dot is cyan; the rest is matte.
- Do not add things the original lacks: no magazine box on the right or left (the gun has none), no bayonet, no side rails, no charging handle and no stock cheek riser.

## 9. Acceptance checklist

- [ ] Length : height : width is within about 10 percent of 9.6 : 3.7 : 1 (48 : 18.3 : 5).
- [ ] Light ice-blue upper (#BCE2E5 family) and dark gunmetal lower (#333840 family); barrel and muzzle near-black (#191919).
- [ ] A 24-long, 4-wide spine with 5-wide front receiver block and receiver cap (x 19 to 33).
- [ ] The scope is a three-step pyramid (6, 7 and 8 long steps) topped at y 0, with a dark bell at each end.
- [ ] A 1-unit-wide red stripe (#CA0000 family) wraps all four sides of the 7-long front receiver block.
- [ ] A 3x3 cyan lens dot (#00FFFF center, #008080 cross) on the scope's eyepiece bell, treated as emissive.
- [ ] Barrel is a 9-long diamond bar followed by a 4x2x2 muzzle block with two 3-long prongs.
- [ ] Handguard core 7 x 3 x 4 with side plates, a dark lower plate and a 1 x 7 x 3 upright dark end cap that is taller than the handguard.
- [ ] A pistol grip 9 long turned about 35 degrees with its lower end to the rear.
- [ ] The triangle under the receiver is open from the side.
- [ ] A hook-shaped foregrip (three slabs at 36 degrees, partly pale) under the handguard.
- [ ] Butt pad is 5 wide, about 7 tall and raked about 10 degrees.
- [ ] No moving parts, no magazine box.
- [ ] Camo textures: Gray (yellow stripe) and Dark Green (red lens, no stripe) are supplied.
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Lens face.** The cyan dot sits on the eyepiece bell's end face. In renders it is visible from the shooter's side. The original's texture mirroring makes this a reading, not a proof.
- **Exact stripe position.** The stripe is measured at about x 30 to 31; the original's mirrored texture could put it at x 28 to 29 instead (2 from the front block's rear). Within 2 units either way is fine.
- **Foregrip slabs.** The three 36-degree slabs overlap heavily; their exact stacking in Z (2, 2 and 3 wide) is a reading from partly hidden faces.
- **Trigger and guard.** The trigger parts are a few tiny pieces; the zero-thickness planes are invisible edge-on. A tiny dark trigger blade is enough.
- **First-person offsets** come from formulas read, not seen in game. Treat them as approximate. The base per-gun scale (registered elsewhere) is not covered here.

## Round 2 corrections

Round 1 measured against the Techguns model with equal length normalization: worst view 0.865 (right-side silhouette, iso front-right), mean 0.951. Top 0.998, front 0.983 and back 0.980 are already good. All gaps are in the side view. Distances below are in model units (1 unit = 1/16 block), measured from the rear end of the stock, and "down" is measured from the top of the scope housing.

1. **Rear stock grip block tilt is mirrored.** The 5 x 5 x 2 block under the rear stock leans 15 degrees the wrong way. Its top must lean toward the muzzle and its bottom toward the rear, a tilt of -15 degrees in the side view, opposite the current +15. Its bottom edge ends about 21 units down.
2. **Forward V-shaped grip bars are mirrored.** The three 5 x 1 slabs (two 2 wide, one 3 wide) beneath the handguard tilt 36 degrees the wrong way. They form a V whose point is at the lower rear, with one arm rising toward the muzzle and the other toward the rear. Flip the sign of the tilt. The assembly spans x = 30 to 37.5 and 11 to 16 down. This crossed-bar mismatch is the largest single side-view error.
3. **Missing solid block under the handguard.** There is a solid white block about 2.5 wide x 2.5 tall at x = 26 to 28.5, 9 to 11.5 down, joining the underside of the front receiver to the foregrip strut. It currently reads as empty space.
4. **Strut between that block and the foregrip.** A thin 1.5-unit-wide white strut runs from the block's lower edge (about x = 27, 11.5 down) to the foregrip's lower rear corner (about x = 25, 16 down) at roughly 20 degrees from vertical. The current strut is shorter and sits further forward.
5. **Underside taper at the front.** From x = 33 to 38 the underside of the handguard rises smoothly from 16 down to 13 down, so the hand area has no square step. The current model stays at 15 to 16 down until x = 37.
6. **Rear bottom edge.** At x = 0 to 2 the stock bottom sits at 12.4 down. The current model is at 11.3, so lower it about 1 unit.
7. **Grip and magazine well bottom.** At x = 9 to 11 the bottom edge is at 17.5 down. The current model is at 18.4, so raise it about 1 unit.
8. **Small top slot.** A 1-unit-wide dark slot cuts into the top rail at x = 36 to 38, 3.5 to 4.5 down. It is currently filled.
9. **Back view palette.** Seen from behind, the stock block and rear receiver face are dark gunmetal, about #34383f, with only thin white rails at the left and right edges (white about #b7d0d1). The current model shows the whole rear face pale (about #8da9ab). Paint the rear-facing and lower stock faces dark, and keep white only on the side rails and top.
10. **Silhouette tolerance.** Front, back and top views are already at 0.97 or better, so leave widths and heights alone and change only the items above.
