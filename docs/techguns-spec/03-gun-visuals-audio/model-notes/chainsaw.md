# Chainsaw: clean-room model notes

Design notes for rebuilding the Techguns **Chainsaw** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout (the original is on a 128x64 sheet, which is a hint about its level of detail, nothing more).

Related facts: [ModelChainsaw](../gun-models.md), [rendering framework](../rendering-framework.md), [Chainsaw sheet](../../02-guns/sheets-launchers-tools-grenades.md#chainsaw-chainsaw).

**Units and axes used in this document.** One unit = one texel at the native 128x64 scale = 1/16 block. The cutting bar points toward **+X**. **Y points down** (Minecraft model convention). **X = 0 is 3 units in front of the rear face of the main engine housing** (the housing spans x -3 to 10). **Y = 0 is 4 units below the housing's top face** (the housing spans y -4 to 5). **Z**: the engine is centered on z = 0.5 (it spans z -5 to 6). **-Z is the bar side** (the cutting bar is bolted to that flank); **+Z is the "handle side"** (side post, pull-start emblem). Sizes across Z are total widths. Angles are rotations about the stated axis. Rounded to the nearest half unit unless it matters.

## 1. Gestalt

A big, chunky, **safety-orange chainsaw**: an orange engine block with a silver top cover, a **long gray cutting bar** bolted to one flank, a **black tubular top handle and front hand guard**, and an orange loop handle at the rear with a black rubber grip. It is not a lean tool; it reads as a heavy, boxy industrial saw. The bar is **about half the total length** and sits off to one side (the -Z flank), not on the centerline.

Defining features in order of visual importance:

1. **The long, flat gray bar** (33 long) with a stepped, rounded tip and a **serrated chain lip** above and below it that alternates every frame while firing (section 7).
2. **Bright safety-orange engine block** (13 x 9 x 11 housing plus a taller rear column), with a silver top plate.
3. **Black tube frame**: an arching top handle, a tall front hand guard like a small ladder, and a vertical side post on the +Z flank.
4. **Orange rear loop handle** with a slanted black rubber grip bar on top.
5. **A diamond-shaped emblem** and a gray panel plus black grille square on the +Z flank.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (rear handle tip to bar tip) | **70** (x -22 to 48) |
| Bar (flange face to tip, with chain) | 36 (x 12 to 48), about **51 percent** of the length |
| Body (rear handle tip to bar mount) | 34 (x -22 to 12) |
| Total height (hand-guard top to skid underside) | about **19.5** (y -14 to 5.5) |
| Engine housing | 13 long x 9 tall x 11 wide |
| Total width (side post to housing -Z flank) | 16 (z -5 to 11); engine only 11 (z -5 to 6) |
| Length : height : width | about 4.4 : 1.2 : 1 |
| Bar | 33 long, 6 tall, 1 thick, plus 1-unit chain lips above and below |

How the pieces sit along the length from the rear: the orange loop handle (x -22 to -8, 2 wide in Z), the tall engine rear column (x -8 to -3), the housing (x -3 to 10), the front flange and bar mount (x 10 to 12), then the bar (x 12 to 45) and its stepped tip (x 45 to 48).

The **top line** is stepped: the housing's top at y -4; a cover at y -7; the silver plate peaks at y -9; the top handle sits at y -13 to -11, and the hand guard peaks at y -14. The **bottom line** is nearly flat at y 5, with a gray skid bar at y 4.5 to 5.5.

## 3. Part by part

All parts are boxes unless noted. Colors are in section 5.

### Engine block (orange)
- **Main housing:** 13 long x 9 tall x 11 wide (x -3 to 10, y -4 to 5, z -5 to 6). Orange, with darker-orange panel lines on the +Z flank. The **-Z flank (bar side) is a bare gray plate** the full 13 x 9, a clean silver-gray panel.
- **Rear column:** 5 long x **12 tall** x 8 wide (x -8 to -3, y -7 to 5, z -3 to 5). Orange. It stands 3 units taller than the housing, and is narrower in Z by 2 on the -Z side and 1 on the +Z side.
- **Top cover:** 10 long x 3 tall x 8 wide (x -3 to 7, y -7 to -4, z -3 to 5), orange, on top of the housing. With the rear column it makes an L-shaped upper body, all at y -7 at the top.
- **Silver top plate:** 8 long x 2 tall x 6 wide (x -3 to 5, y -9 to -7, z -2 to 4), light silver. With two small steps at its ends: a **1x1x6 silver strip** behind it (x -4 to -3, y -8 to -7), and a **3x1x6 orange step** in front (x 5 to 8, y -8 to -7).
- **Front flange:** 1 long x 7 tall x 7 wide (x 10 to 11, y -3 to 4, z -2 to 5), orange.
- **Bar mount (clamp):** 2 long x 8 tall x 2 wide (x 10 to 12, y -3.5 to 4.5, z -4 to -2), mid-gray with dark slots. It sits on the -Z flank and ends exactly where the bar starts.
- **Skid bar:** 3 long x 1 tall x **11 wide** (x 4.5 to 7.5, y 4.5 to 5.5, z -3 to 8), light gray, under the housing's belly. It pokes 2 units out past the +Z flank (z 6 to 8).

### Flank details
- **+Z flank (handle side):**
  - **Gray square panel:** 3 x 3 x 1 (x -7 to -4, y -3 to 0, z 5 to 6), mid-gray, on the rear column.
  - **Black grille square:** 4 x 4 x 1 (x -7.5 to -3.5, y 0.5 to 4.5, z 5 to 6), near black, below the gray panel; with a faint grille texture.
  - **Emblem:** a 5 x 5 x 1 plate (x -2 to 5, y -3 to 4, z 6 to 7) **turned 45 degrees about Z**, so it shows as a **diamond about 7 across** at center (x 1.5, y 0.5). Dark brown, gray and orange, a pull-start or logo plate.
- **-Z flank (bar side):**
  - **Gray square:** 3 x 3 x 1 (x -7 to -4, y -2.5 to 0.5, z -4 to -3), light gray.
  - **Black block:** 2 x 3 x 2 (x -6.5 to -4.5, y -4 to -1, z -5 to -3), near black; this is where the rear leg of the top handle lands (below).

### Black tube frame
All tubes are **2 x 2 in section**, near black, unless noted.
- **Top handle bar:** 7 long (along Z) at x 5 to 7, y -13 to -11, z 0 to 7. It is the +Z half of the handle.
- **Angled cross-piece:** 6 long, **yawed 35 degrees about Y**, from the bar's -Z end (x 7, z 0) toward -Z and the rear, ending near x 2, z -5. Extents: x 1.9 to 7, y -13 to -11, z -4.9 to 1.2.
- **End block:** 2 x 2 x 2 (x 1.5 to 3.5, y -13 to -11, z -5 to -3), the knuckle where the cross-piece meets the rear leg.
- **Rear leg:** 12 long, **tilted -49 degrees about Z**, running from the end block (top, x about 1.5 to 3, y -13) down and rearward to the black block on the -Z flank (x -6.4, y -2.7). Extents: x -6.4 to 3, y -13 to -2.7, z -5 to -3.
- **Side post (+Z):** 2 x 10 x 2 (x 5 to 7, y -7 to 3, z 9 to 11), vertical, outside the +Z flank.
- **Slanted strut:** 2 x 7 x 2, **tilted 35 degrees about X**, from the post's top (z 9 to 11, y -7) up and inward to the top handle bar's +Z end (about y -12.8, z 5.4). Extents: x 5 to 7, y -12.8 to -5.9, z 5.4 to 11.
- **Post foot:** a 2 x 1 x 1 stub (x 5 to 7, y 3.5 to 4.5, z 7 to 8), a 2 x 2 x 2 block (x 5 to 7, y 3 to 5, z 8 to 10), and a 2 x 1 x 1 stub (x 5 to 7, y 3 to 4, z 10 to 11), linking the skid bar's +Z end to the post's base. Near black.
- So the top handle is an **arch**: it rises on the +Z side via the post and strut, crosses at y -13 to -11, jogs toward -Z, and drops via the long rear leg to the rear column on the -Z side.
- **Front hand guard:** a vertical ladder frame at x 11.5 to 12.5, y -14 to -9, z -3 to 5 (6 wide, 5 tall):
  - Three **rungs**, each 1 x 1 x 6 (z -2 to 4): at y -14 to -13, y -12 to -11 and y -10 to -9.
  - Two **posts**, each 1 x 5 x 1, at z -3 to -2 and z 4 to 5 (y -14 to -9).
  - Two **braces**, each 1 x 6 x 1, **tilted 42 degrees about Z** (their upper end toward the front), from the post bottoms (x 12.5, y -9) back and down to (x 7.8, y -4.5), at z -3 to -2 and z 4 to 5. They anchor the guard to the housing's top edge.

### Rear loop handle (orange, 2 wide in Z: z 0 to 2)
A closed loop made of 2 x 2 bars and stepped corners, 14 long:
- **Lower bar:** 9 x 2 x 2 (x -19 to -10, y 3 to 5).
- **Rear-lower post:** 2 x 3 x 2 (x -10 to -8, y 2 to 5), joining the lower bar's front end up to the column.
- **Front upright:** 1 x 5 x 2 (x -9 to -8, y -3 to 2).
- **Front-upper block:** 3 x 2 x 2 (x -11 to -8, y -5 to -3).
- **Rear cap (stepped, a rounded corner):** 2 x 3 x 2 (x -22 to -20, y -1 to 2), a 1 x 1 x 2 cap above it (x -22 to -21, y -2 to -1), a 1 x 1 x 2 below (x -22 to -21, y 2 to 3), a 2 x 2 x 2 block above (x -21 to -19, y -3 to -1), a 2 x 2 x 2 block below (x -21 to -19, y 2 to 4), and a 1 x 1 x 2 foot (x -20 to -19, y 4 to 5).
- **Grip bar:** 9 x 2 x 2, **tilted -13 degrees about Z**, hinged at its rear end (x -19, y -1); it rises to the front, reaching about x -10.2, y -4.9 at the top. The middle of the bar is **near-black rubber** with **about 2 units of orange at each end**.
- The loop's inner opening is about 9 long and 4 to 6 tall, visible from the side.

### Cutting bar
- **Bar slab:** 33 long x 6 tall x 1 thick (x 12 to 45, y -2.5 to 3.5, z -3.5 to -2.5), mid-gray with metal plate noise and **small dark rivet dots** near the edges.
- **Tip step 1:** 1 x 5 x 1 (x 45 to 46, y -2 to 3), darker gray.
- **Tip step 2:** 1 x 3 x 1 (x 46 to 47, y -1 to 2), darker gray. So the bar's end is a stepped, rounded nose (33 long, then 1 unit at 5 tall, then 1 at 3 tall).
- **Chain plane (animated; see section 7):** a plane with **no thickness**, 36 long x 8 tall (x 12 to 48, y -3.5 to 4.5), at z = -3, **the middle of the bar slab**. Because it is buried in the slab, only its edges show: a **1-unit lip above** the bar (y -3.5 to -2.5), a **1-unit lip below** (y 3.5 to 4.5), and the extra 1 to 3 units at the tip (x 45 to 48) that fills out the stepped nose.

## 4. Silhouette checks

- **Side, from the bar side (-Z):** the 36-long gray bar at right, 8 tall with the serrated lips and a stepped nose; a gray flank panel on the housing; the housing and the rear column rising behind it; the silver top plate; the black arch of the handle high above; the black ladder hand guard at the front of the engine; the orange loop handle at the left with its slanted black grip. Negative space: the loop handle's window; the gap between the engine and the top handle (2 units); the open frame of the hand guard.
- **Side, from the handle side (+Z):** the same outline, but the bar is hidden behind the housing and you see the orange flank with the gray square, the black grille square, the diamond emblem and the black side post with its slanted strut.
- **Top:** the housing about 11 wide, the rear column and cover 8 wide, the silver plate 6 wide; the bar a very thin line (1 wide plus chain), offset to one side of the axis; the black tubes as a thin outline arch; the loop handle a thin 2-wide stub at the rear; the side post sticking out 5 beyond the +Z flank.
- **Front:** a tall, narrow frame: the hand guard ladder (6 wide, 5 tall) above the orange flange; the bar on the left side; the side post on the right; the skid bar as a wide foot at the bottom (11 wide).
- **Back:** a squat orange loop handle (2 wide) in front of the 8 to 11-wide engine; the black arch looming above.

## 5. Materials and palette

Values sampled from the original texture. Three families: **safety orange**, **near-black tube**, **light gray metal**.

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Engine housing, rear column, cover, flange, rear handle | safety orange | #FFA00A (median #FFA20D) | #FFA413, #FFA516, up to #FFB642 (edge rows) | #FF9F07; panel lines and borders #FF7508 (down to #FF6F03) |
| Top plate | light silver | #A4A5A5 | #B4B4B6 | #868A87 |
| Orange step in front of the plate | orange | #FFA20D | #FFA413 | #FF9F07 |
| -Z flank gray panel (housing) | brushed gray | #9A9D9E | #A0A3A3 | #8E9192 |
| Gray squares and skid bar | light gray | #A1A1A1 | #AEB2B2, #BCBCBD | #949495 |
| Black grille square | near black | #161818 | #2C2E30 | #121312 |
| Tube frame, side post, hand guard | near-black tube | #151513 (range #111110 to #191B1B) | #A7A9A8 (rare edge pixels) | #0E0F0E |
| Grip bar | black rubber with orange ends | #0D0E0D to #151616 | | orange ends #FFA10C |
| Bar slab | mid-gray metal | #959796 | #AAADAC, up to #BDC0BF | #797C7A; rivets near black |
| Bar tip steps | darker gray | #7A7B7B | #9A9D9D | #747573 |
| Bar clamp | mid-gray with black slots | #6B6C6D | #A1A4A3 | slots #272728 |
| Emblem diamond | dark brown, gray and orange | brown #261801, gray #949898 | orange #FFA20D | |
| Chain lip teeth | dark gray | #515050 | | gaps are empty |

Shading style:
- **Orange:** flat saturated orange with a few levels of noise, small bright-edge highlights; darker orange seams outline panels. The whole body is a single clean color; it must look new and glossy.
- **Gray metal:** light, flat; the bar has a fine plate texture with bolt-like dots.
- **Black tubes:** uniform very dark gray with almost no highlight.
- No glow, no dirt, no wear, no brand text.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up). The renderer is constructed with a scale value that is not used by the transforms.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 1.35 | idle offset (1.1, 0, -1.6): the saw is shoved far forward and to the side, so the camera sees the rear handle and the bar's side. |
| First person firing | see section 7 | the idle offset applies throughout. |
| First person aimed | no scope, no zoom overlay | |
| First person reloading | roll up to 32 degrees about Z, yaw up to 15 degrees | translate up to (0.05, 0.4, -0.2) from idle; the plateau lasts from about 27 percent to 73 percent of the 45-tick reload. |
| Third person (held) | yaw 225 degrees, scale 1.30 | offset (-1.1, 0.55, -0.1). |
| Third person aimed | extra offset (0, -0.15, 0.3) | |
| Ground (dropped) | yaw 90 degrees, scale 0.7 | lowered by 0.6. |
| Inventory icon | yaw 180 degrees, scale 0.5 | offset (-0.3, 0.3, -0.4). Side-on, the 3D model itself. |

**Muzzle flash:** none (disabled in the original). The business point is the bar tip at x = 48, y 0.5, z -3.

## 7. Motion

### The chain flicker (the only animated part of the model)
- The chain is a **pair of alternating frames**, A and B, each a 36 x 8 plane at the middle of the bar's thickness (z = -3). Both are plain light gray (#A4A4A1) over most of their area, which is hidden inside the slab; the only visible detail is the **top and bottom lip rows**, which are **checkerboard-like teeth**: 1 texel dark gray (#515050) alternating with 1 texel empty.
- **Frame A:** the top-lip teeth sit on the even texel positions (counting from the rear, x 12) and the bottom-lip teeth on the odd positions.
- **Frame B:** the same pattern shifted by one texel: top-lip teeth on odd positions, bottom-lip on even.
- Swapping A and B makes the teeth appear to crawl around the bar, i.e. **the chain looks like it is running**.
- **When it swaps.** The frame is chosen from the firing progress *p* (0 to 1 over one fire animation): it flips every **1/40 of the animation** (the frame index is the rounded value of *p* x 40, modulo 2). At **p = 0 (idle, inventory, ground, held without firing) it shows frame A**. Because the chainsaw fires every 3 ticks and each shot restarts the animation, the swap is much faster than the display rate, so in play the chain looks like a **flicker of the two frames at frame rate** while the trigger is held. A builder can implement this as "alternate A and B every render frame (or about every 20 ms) while firing; A while idle".
- The two frames are exact one-texel offsets; there is no smooth movement, no blur and no change to the bar itself.
- Both faces of the plane carry the pattern (the back face is shifted by one more texel), but only the face turned toward the viewer is drawn.

### Whole-gun vibration while firing (first person)
The motion is a shake of the whole model; no part moves relative to another.
- **Primary (attack type 0):** one full sine cycle across the fire animation. The amplitude: a 0.15 block (2.4 units) forward-and-back slide along X, and rotations of -5 degrees about Z, -2.5 degrees about Y and -1.5 degrees about X, all times the sine.
- **Secondary (attack type 1):** a single positive hump (sine squared): a 0.3 block (4.8 units) slide along +X, rotations of -10 degrees about Z, -5 about Y and -2.5 about X at the peak.
- **Third person:** no recoil on the body; the chain flickers as above. Reload tilts the saw about 30 degrees about X and 40 degrees about Y at mid-animation.
- **Reload** is 45 ticks (2.25 s): the first-person plateau in section 6.

## 8. What to avoid

- **A bar on the centerline.** The bar is bolted to the -Z flank: its mid-plane is 3.5 units off the engine's center (z -3 vs 0.5).
- **A thick bar.** It is a 1-thick slab with a thin chain lip, not a 2-thick or rounded bar.
- **A static, tooth-less chain.** The lip must alternate; do not build 3D teeth or a spinning loop.
- **Gray or black tubes in orange, or orange tubes in black.** The top handle, hand guard and side post are black; the engine and rear handle are orange; the grip bar is black with orange ends.
- **A closed, solid rear handle.** It is a loop with a window.
- **Extras the original lacks:** no fuel cap, no brand logo text, no sling, no exhaust, no spark.
- **Making the saw tiny.** At 70 units it is more than four blocks long before the in-game scale of 1.3 to 1.35; the long bar is the point.

## 9. Acceptance checklist

- [ ] Length : height : width is within about 10 percent of 70 : 19.5 : 16, and the bar (with chain) is about half the length.
- [ ] The housing is 13 x 9 x 11 orange, with a 5-long x 12-tall rear column, a cover and a silver 8x2x6 top plate.
- [ ] The bar is a 33 x 6 x 1 gray slab on the -Z flank with a 3-unit stepped nose and an 8-tall, 2-long clamp at its base.
- [ ] A chain plane gives a 1-unit serrated lip above and below the bar (and fills the nose steps to x 48), with two frames, A and B, offset by one texel.
- [ ] Idle, inventory and ground show frame A; while firing, A and B alternate at frame rate.
- [ ] The black top handle is an arch: a 7-long bar at y -13 to -11, a 35 degree cross-piece, an end block and a 12-long leg at -49 degrees onto the black block at the rear column.
- [ ] A 10-tall black side post stands on the +Z side (z 9 to 11) with a 7-long strut at 35 degrees to the top bar.
- [ ] A ladder hand guard (three rungs, two posts, two 42 degree braces) stands at x 11.5 to 12.5, y -14 to -9.
- [ ] The rear loop handle is orange, 2 wide, 14 long, with a window and a black rubber grip bar tilted 13 degrees.
- [ ] The +Z flank carries a gray square above a black grille square, and a 45 degree diamond emblem.
- [ ] The skid bar (11 wide) lies under the housing and pokes out 2 beyond the +Z flank.
- [ ] Palette: orange #FFA00A, tube black #151513, bar gray #959796, silver plate #A4A5A5.
- [ ] No part floats; every box overlaps or touches a neighbor.
- [ ] No glow, no flash, no extra animation besides the chain flicker.

## 10. Unsure

- **Which flank carries which detail.** I read the housing's -Z side as a bare gray panel and the +Z side as orange with panels, based on the texture's face layout; if the in-game view shows the reverse, swap them. The bar is certainly on the -Z side.
- **Chain visibility.** The chain plane is buried in the bar's slab, so only its lips show; I did not verify the exact appearance of the back face or of how the culling hides one face. A builder may simply paint serrated lips on the bar's top and bottom edges and alternate two textures.
- **Swap rate.** The original swaps every 1/40 of the fire animation; the actual display rate depends on the recoil time of the saw, which I did not read. Alternating per frame is the practical equivalent.
- **Emblem artwork.** I only read its colors (dark brown, gray, orange) from a very low-resolution patch; the exact motif is a guess. Any 7-across diamond with dark brown, gray and orange will do.
- **Shake signs.** The amplitudes are from the renderer's formula; the directions (positive or negative) are approximate.
