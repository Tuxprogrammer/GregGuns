# PDW: clean-room model notes

Design notes for rebuilding the Techguns **PDW** (personal defense weapon) look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer, transforms and textures. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelPDW](../gun-models.md#modelpdw-6464-family-x-41-boxes-1-part), [PDW sheet](../gun-sheets.md#pdw-pdw), [rendering framework](../rendering-framework.md), [Techguns PDW stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = one texel at the native 64x64 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention). **Z = 0 is the gun's mid-plane**; sizes across Z are total widths, centered. **Y = 0 is the top face of the main receiver block** (the big light-colored box; see section 3), and **X = 0 is a point about 13 units behind the receiver's front face**, so the receiver runs from x -13 to x 12 and the butt plate sits at x -15. Positions are rounded to a tenth of a unit. Angles are "tilts about the Z axis" unless stated: a **positive tilt leans the bottom of the part toward the rear (-X)**, like a grip raked back.

## 1. Gestalt

A compact, futuristic, **P90-style bullpup SMG**, in clean white and ice blue over dark slate. The look is: a long, pale, slab-sided **receiver** that is the whole top half of the gun; a short black **diamond muzzle** poking out the front; a **flat carry-handle rail floating above** the receiver with a see-through window under it; and everything below the receiver colored dark blue-gray: a **raked pistol grip/magazine well**, a **thumbhole stock** (a slanted slit behind the grip), and a stepped **butt** at the very back. The bottom outline slopes up toward the rear in one long diagonal. There is one accent, a **single thin red stripe** across the receiver, plus a few black vent slots in front of it.

Defining features in order of visual importance:

1. **Pale ice-blue receiver over dark underparts.** Two big color blocks: top half light, bottom half dark slate.
2. **A tall raked pistol grip (25 degrees) with a thumbhole slit behind it** and a long diagonal underline that rises to the rear.
3. **Floating carry-handle rail** on top: a 13-long flat bar held up by a rear pillar and a front post, with a one-unit open window beneath it.
4. **Black diamond muzzle**, short (5 units past the receiver), with two small ears at its base.
5. **Red stripe and three black vent slots** on the receiver's sides.
6. **Stepped butt block** at the back, 4 wide and 4 tall, wider than the rest of the stock.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt plate to muzzle tip) | **32** (x -15 to 17) |
| Total height (front-sight tip to grip bottom) | about **18.8** (y -5 to 13.8) |
| Total width | **4** (the receiver and the butt block; everything else is 1 to 3) |
| Length : height : width | about 8 : 4.7 : 1 |
| Receiver (main block) | 25 long, 3 tall, 4 wide (x -13 to 12, y 0 to 3) |
| Muzzle protrusion beyond receiver | 5 (x 12 to 17) |
| Butt block | 2 long, 4 tall, 4 wide, at the very back (x -15 to -13) |
| Grip (pistol grip + magazine well) | 4 wide in profile, 8 long on its own axis, 2 across Z, tilted 25 degrees; bottom reaches about y 12.3 |
| Rail above the receiver | top of rail at y -4: 4 above the receiver top, 3 above the cover plate |

How the pieces sit along the length, from the rear: butt block (x -15 to -13), then the 25-long receiver (x -13 to 12) which carries the rail, the sight posts and the underbody. The thumbhole slit sits at about x -6 to -2 below the receiver; the grip is at about x -4 to 4 at the top, leaning back as it goes down; the trigger and underbody block run from x -1.5 to 11.5 under the receiver; the muzzle begins at x 12.

The **top line**: the cover plate (y -1) is flat from x -13 to 12 with a 1-unit raised hump over the rear 9 units. The rail floats above it at y -4 to -3. Only the sight ears, planes and rear pillar exceed the rail top (by 0.5 to 1).

## 3. Part by part

All parts are boxes unless noted. "Touches" means faces in contact or overlapping.

### Receiver (main block)
- **Box 25 long x 3 tall x 4 wide**: x -13 to 12, y 0 to 3, z -2 to 2. This is the biggest part of the gun and, together with the butt block, the widest (4).
- **Cover plate:** a 25 x 1 x 3 plate on top: x -13 to 12, y -1 to 0, z -1.5 to 1.5. It is half a unit narrower than the receiver per side, so the receiver's top edge shows as a 0.5-wide ledge on both sides.
- **Rear hump:** a 9 x 1 x 3 plate on the cover plate over the rear part: x -13 to -4, y -2 to -1, z -1.5 to 1.5. It makes the rear of the top one unit taller than the front.
- Colors: the sides are pale (upper 2 units) with a dark 1-unit bottom band; the underside is dark; the top is pale. See section 5.
- No windows, no ejection port, no cutouts in the geometry; all detail (vents, stripe) is painted.

### Carry-handle rail and sights
The whole top assembly reads as a flat bar held on two supports with an open window under it.
- **Rail:** a 13 x 1 x 2 bar, x -3.5 to 9.5, y -4 to -3, z -1 to 1. Centered; 0.5 narrower than the cover plate on each side.
- **Rear pillar:** a 3 x 2 x 3 block under the rear of the rail: x -4 to -1, y -3 to -1, z -1.5 to 1.5. It overlaps the cover plate and the rail's rear end (the rail starts 0.5 inside the pillar).
- **Rear sight ears:** two 2 x 2 x 1 blocks on the pillar's two sides, x -3.9 to -1.9, y -4.5 to -2.5. One at z 0.4 to 1.4, the other at z -1.4 to -0.4. They leave a **0.8-wide notch** in the center, and rise 0.5 above the rail's top. Seen from the rear they form a small two-eared notch.
- **Support rail:** an 8 x 1 x 2 bar sitting on the cover plate: x -1 to 7, y -2 to -1, z -1 to 1. It runs under the rail with a **1-unit-tall open window** between it and the rail (y -3 to -2), open to both sides.
- **Front support:** a 2 x 1 x 2 block on the cover plate, x 9 to 11, y -2 to -1, z -1 to 1. Between the support rail's end (x 7) and this block (x 9) the cover plate is bare for 2 units.
- **Front post:** a 1 x 2 x 2 block, x 9.5 to 10.5, y -4 to -2, z -1 to 1. It continues the line of the rail's front end (rail ends x 9.5) down to the front support; it rises to the rail's top height (y -4).
- **Front sight:** three zero-thickness vertical planes, each 1 x 1: one centered at x 9.5 to 10.5, y -4.5 to -3.5; two at y -5 to -4, offset to z -0.5 and z +0.5 (x 9.5 to 10.5). These read as a thin horned post, the highest points of the whole gun (y -5). A builder may thicken them to 0.2 so they show from the front and back; they are barely visible from the side.
- The rail is **not** one thick bar and the window must stay open: this is the key negative space on the top.

### Muzzle
All pieces are centered on the barrel axis, **y = 1.5** (the receiver's mid-height), z = 0.
- **Base diamond:** a 1 x 2 x 2 block turned 45 degrees about the X axis (a diamond about 2.8 across), x 12 to 13. Touches the receiver's front face.
- **Ring:** a 1 x 1 x 1 cube, x 13 to 14, axis-aligned (a small square collar, 1 across).
- **Ring 2:** a 1 x 1 x 1 cube turned 45 degrees (a diamond, 1.4 across), x 13 to 14, in the same slot as the square collar, so the pair reads as an eight-pointed star in the front view.
- **Barrel tube:** a 3 x 2 x 2 block turned 45 degrees about the X axis (a diamond about 2.8 across and 2.8 tall), x 14 to 17. Its front face is the muzzle; it is painted **pure black** so the front view shows a black diamond.
- **Muzzle ears:** two 1 x 2 x 1 posts at the base of the muzzle, flanking it: x 12 to 13, y 0.5 to 2.5, one at z 1 to 2 and one at z -1 to -2. They sit on the receiver's front face, outside the base diamond. They are dark gray.
- **Gas block (tiny):** a 1 x 1 x 1 cube turned 45 degrees, x 11.5 to 12.5, y 3.3 to 4.7 (centered at y 4, z 0), under the receiver's front bottom edge. It reads as a tiny diamond nub.
- In short, the whole muzzle is a short black nozzle: 5 units long and 2.8 across, so about as tall as the receiver (3).

### Underbody (trigger housing)
All dark, under the receiver, spanning x -1.5 to 11.5:
- **Rear block:** 2 x 2 x 3 at x -1.5 to 0.5, y 3 to 5, z -1.5 to 1.5.
- **Middle block:** 4 x 3 x 3 at x 0.5 to 4.5, y 3 to 6, z -1.5 to 1.5. The tallest part of the underbody.
- **Front block:** 6 x 2 x 3 at x 4.5 to 10.5, y 3 to 5, z -1.5 to 1.5.
- **Belly plate:** 6 x 1 x 2 at x 4.5 to 10.5, y 5 to 6, z -1 to 1, under the front block.
- **Front wall:** 1 x 3 x 3 at x 10.5 to 11.5, y 3 to 6, z -1.5 to 1.5, closing the front end.
- **Foot:** 1 x 1 x 2 at x 10.5 to 11.5, y 6 to 7, z -1 to 1: a small nub hanging from the front wall's bottom.
- The underline of this assembly is y 6 along the whole length (belly plate bottom, middle block bottom); the front wall and foot step down to y 7 at the front. Total depth about 3 below the receiver.

### Pistol grip and magazine well
- **Main grip box:** 4 x 8 x 2 (4 along X, 8 along the tilt axis, 2 across Z), tilted **25 degrees** (bottom leaning to the rear). Hinge point (top front corner): about x 3.5, y 5, z 0. After tilting it spans x -3.5 to 3.5 and y 3.3 to 12.3. This is the big dark slab under the gun's middle.
- **Front strap:** a 1 x 8 x 1 thin plate at the same hinge, same 25 degree tilt, on the front face of the grip: x 0.1 to 4.4, y 5 to 12.7, z -0.5 to 0.5. It makes a narrow rib on the grip's front, half the grip's width in Z.
- **Floor bar:** an 11 x 1 x 2 bar, also tilted 25 degrees about the same hinge, spanning x -8.9 to 1.5 and y 8.2 to 13.8. Its front end is the lowest point of the whole gun (13.8), at the grip's bottom. Its upper edge **rises to the rear** at about 25 degrees: the underline runs from about (1.5, 13) up to (-8.5, 8.2). This is the long diagonal in the silhouette.
- **Trigger:** a zero-thickness 2 x 2 plane tilted -45 degrees at x -2.5 to -1.1, y 4.6 to 6, z -1 to 1: effectively invisible from the side; an optional small black trigger is a harmless addition (section 10).
- **Guard floor:** a zero-thickness 3 x 2 plane at y 6, x -5.5 to -2.5, z -1 to 1: also near-invisible from the side. There is no visible guard bar.

### Thumbhole stock
The part that makes the PDW read as a bullpup: a loop behind the grip.
- **Rear strut:** a 3 x 6 x 2 box tilted 25 degrees, hinge about x -5, y 3.5, z 0. It spans x -9.3 to -4.1 and y 2.7 to 9.4. A leaning pillar directly behind the thumbhole slit, parallel to the grip.
- **Top strap:** a 5 x 1 x 3 plate under the receiver, x -6.5 to -1.5, y 3 to 4, z -1.5 to 1.5, the roof of the thumbhole. A second strap, 7 x 1 x 2, x -13 to -6, y 3 to 4, z -1 to 1, continues it to the butt.
- **Brace:** a 2 x 1 x 2 block tilted 50 degrees at about x -8.8 to -6.7, y 3 to 5.2, tying the strut to the strap. A small diagonal gusset.
- **Lower stock leg:** a 6 x 2 x 2 box tilted 15 degrees, hinge at about x -11.5, y 6.5: x -13.9 to -7.6, y 6 to 9.5. It slopes down toward the front and meets the floor bar's rear end.
- **Heel hook:** a 2 x 3 x 2 box tilted **-27.7 degrees** (the opposite way), hinge at x -15, y 6.5: x -15 to -11.8, y 5.6 to 9.2. A small dark wedge at the back bottom; it ties the butt's lower step to the stock leg.
- **Negative spaces** (what you must be able to see through): (a) a **slanted thumbhole slit** between the grip's rear edge and the strut's front edge, about 3 wide at the top (y 4 to 5, x -5 to -2), narrowing to about 1.5 wide near y 9, **about 5 to 6 tall** and leaning back 25 degrees; closed at the bottom by the floor bar. (b) A small **rear notch** under the receiver between the butt block and the strut: about 4 wide (x -13 to -9) and 2 to 2.5 tall (y 4 to 6.5), roughly triangular.

### Butt block
A stack of steps at the very back; the rear face is a flat wall.
- **Cap:** 2 x 1 x 3, x -15 to -13, y -2.5 to -1.5, z -1.5 to 1.5. Pale. It rises above the cover plate.
- **Main block:** 2 x 4 x 4, x -15 to -13, y -1.5 to 2.5, z -2 to 2. The same width as the receiver (4). Pale on the top and the upper part of the sides, dark below.
- **Lower step:** 2 x 2 x 3, x -15 to -13, y 2.5 to 4.5, z -1.5 to 1.5. Dark.
- **Heel step:** 2 x 2 x 2, x -15 to -13, y 4.5 to 6.5, z -1 to 1. Dark. The butt steps in from 4 wide to 3 to 2 as it goes down: a stepped, narrowing profile seen from the back.
- The butt block's front face meets the receiver's rear face (x -13).

## 4. Silhouettes

- **Side (the main view):** a long pale horizontal bar (the receiver) with a 1-unit step on top at the rear (hump), then the pillar and the floating rail with its window over the center, ending at the front post; a short black nozzle at the front; below, a dark underbody block, a tall grip leaning back, a long diagonal line sweeping from the grip's foot up toward the butt, and a **slanted slit** behind the grip; at the very back a stepped column (butt). **Negative space:** the 1-unit window under the rail; the thumbhole slit; the small notch behind it under the receiver; the open gap between the front foot nub and the grip (about 4 units wide).
- **Top:** a narrow spine: the receiver 4 wide, the cover plate inside it with a 0.5-wide ledge each side; the rail 2 wide down the center; the muzzle diamond (2.8) beyond the receiver, with its two ears just visible at the base; at the rear the butt block as wide as the receiver. A red stripe crosses the receiver at about x 3 to 4. Nothing sticks out wider than 4.
- **Front:** a tall, narrow figure: the black diamond (2.8 across) at the center of a pale 4-wide, 3-tall rectangle, topped by a thin sight post with two horns and the rail (2 wide), and the dark underbody (3 wide) and grip (2 wide) hanging below as a narrow column about 10 tall below the receiver.
- **Back:** the butt block's flat wall (4 x 4, pale on top, dark in the lower half), the two-eared rear sight notch above it, and the dark stock steps below, narrowing from 3 to 2.
- **Underside:** a long dark thin bar: the receiver's 4-wide dark bottom, the underbody (3 wide) in the middle and the grip's 2-wide strip behind it.

## 5. Materials and palette

The original texture is white and pale blue over slate, with strict **two-tone separation**: light parts are the receiver, cover plate and butt block; dark parts are everything else. All hex values sampled from the texture, rounded to typical values.

### Default skin

| Part | Material | Base | Highlight | Shadow / detail |
|---|---|---|---|---|
| Receiver sides, upper 2 units | ice-blue composite | #B8DEE1 | #D7EEEF (lightest streaks) | #AED4D7 |
| Receiver sides, bottom 1-unit band | dark slate | #323740 | #373C45 | #2F343D |
| Receiver top and cover plate | ice-blue | #B6DCDF | #D0EBED | #929CA0 at the ledge |
| Receiver underside | dark slate | #31363F | #373C45 | #2F343D |
| Rear hump and butt cap | ice-blue | #AED4D7 | #B2D8DB | #829FA1 (end row) |
| Butt block (upper) | ice-blue | #B4DADD | #B3D9DC | #323841 (lower rows) |
| Butt steps, strut, stock straps, grip, floor bar, leg | dark blue-gray slate | #2E343D | #434850 (edge rows) | #24282F |
| Underbody middle blocks | blue-gray | #2D313A | #383D46 | #2C3038 |
| Sights, rail, pillar, ears, muzzle parts, front wall, foot, strap, front underbody | neutral dark gray | #2B2B2B | #2F2F2F | #1F1F1F |
| Rail top | near-black | #252525 | #2C2C2C | #000000 for the sight planes |
| Muzzle front face | pure black | #000000 | | |
| Vent slots (3, painted) | near-black | #121616 | | |
| Stripe | red | #B80000 | #B70000 | #AC0000 |

Where a dark part has a light edge row (grip, stock), it is a **thin one-pixel lighter line** along the leading edge, not a gradient.

Shading style:
- **Receiver:** flat, matte, bright, **with a few levels of per-pixel noise (about 3)**, like a molded composite. Highlights lie on the top and front faces; the lower band is dark and flat. No gradient.
- **Dark parts:** two distinct darks: **bluish slate #2E343D** for the stock and grip, **neutral graphite #2B2B2B** for the sights, muzzle and front underbody. Keep them slightly different; it separates the parts.
- **Vents:** three **1-unit-wide, 2-unit-tall vertical black slots** on each side of the receiver's front part, spaced 2 apart (slot, 1-wide pale post, slot, ...), at about x 7, 9 and 11 (centers 7.5, 9.5, 11.5). They fill the upper two rows of the receiver side. In side view they look like a tiny piano keyboard at the front.
- **Red stripe:** one vertical 1-unit-wide band, **#B80000**, about 4 units behind the last slot, at about x 3 to 4. It crosses the receiver's side (upper 2 units) and also the top face (across the whole 3-wide cover plate). It is the only saturated color.

### Camo variants (2 variants, 3 textures in total)

| Skin | Receiver light | Receiver dark parts | Underparts | Accent |
|---|---|---|---|---|
| **Default** (camo 0) | #B8DEE1 | #323740 | #2E343D / #2B2B2B | red #B80000, vents #121616 |
| **Silver** (camo 1) | #C7C7C7, highlights #E9E9E9 / #DCDCDC | #1D1D1D, #202020 | slate #202020, front blocks #0E0E0E | **no red**; vents #1D1D1D; a single yellow texel in the stripe spot |
| **Dark Green** (camo 2) | #12140E (near-black olive) | #192610 and #16230D | #192610, highlight #2B3723, front blocks #0E0E0E | no red; a one-texel pale tick in the stripe spot; slots #020302 |

The Silver skin is **grayscale** with black parts; the Dark Green skin is **near-black with mottled green** (greens #14210B to #1F2C16 and #25311C). Geometry is identical for all three.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, **scale 2.1** | idle offset about (0.70, -0.30, -0.60). Nearly side-on, turned 45 degrees to show the side plus the butt. Shown larger than the revolver (2.1 against 1.5). |
| First person firing | roll about Z of up to **2.5 degrees** (tiny), shift 0.125 back | two small bumps per window, one per burst round |
| First person reloading | roll 25 degrees, yaw -15 degrees, shift about (+0.5, +0.25, +0.25) from idle at peak | smooth rise and fall over the reload; the peak is held near the middle |
| First person aimed | no scope and no zoom overlay | only the front-sight horns and rear notch |
| Third person (held) | yaw 225 degrees, scale 1.25 | offset (-0.85, -0.60, 0) |
| Third person aimed | extra offset (0, +0.1, +0.2) | |
| Ground (dropped) | yaw 90 degrees, scale 0.8 | offset (0, -0.60, 0) |
| Inventory icon | yaw 180 degrees, scale 0.75 | offset (-0.25, +0.1, 0); drawn side-on as a 3D model |

**Muzzle flash point:** the front of the barrel on its axis (model position x 17, y 1.5, z 0). A **blue** flash sprite at about 1.65 times scale is drawn there, rotated to face down the barrel. In third person the flash is drawn slightly forward of the hand at (0, 0.05, -0.35).

## 7. Motion

- The **model has no moving parts.** Nothing slides, spins or cocks. All motion is whole-gun transforms. The separate magazine item is not part of this model (the dark grip slab is fixed).
- **Recoil (first person):** 5-tick whole-gun nudge of at most 2.5 degrees around Z and 0.125 unit back, played twice per shot (a **2-round burst**, so two bumps). **Third person:** the gun pitches up to about 20 degrees about a wrist point (about 0.9 behind and 0.3 above the model origin) over the first quarter of the first half of the animation, then settles.
- **Reload:** 40 ticks (2.0 s). First person: the gun rolls 25 degrees, yaws 15 degrees and slides up and aside, then drops back. Third person: it tilts about 15 degrees in pitch and 20 degrees in yaw at mid-animation, then returns.
- **Optional:** the builder may keep the grip, muzzle and rail as separate named parts for later animation, but the rest pose must match this document.

## 8. What to avoid

- **A short or pencil-thin receiver.** It is 25 of 32 units long and 4 wide, and must dominate. The muzzle is only 5 long.
- **A merged, solid rail.** The 1-unit window below the rail is essential; do not fill it.
- **Dark everywhere.** The receiver and butt top must be **pale ice-blue/white**; the dark must be a clear lower half.
- **A straight, vertical grip.** It is raked **25 degrees**; the whole underline slopes diagonally. Do not leave out the thumbhole slit.
- **A floating stock.** Every stock piece overlaps its neighbor: the strap under the receiver, the strut, the floor bar and the leg all touch.
- **Round or tapered barrels.** The muzzle is a short black diamond with ears and two small rings.
- **Adding a visible magazine, a bayonet lug, a stock hinge, a scope or sling loops.** The original has none.
- **Overusing red.** Only the one thin stripe.

## 9. Acceptance checklist

- [ ] Length : height : width is within about 10 percent of 8 : 4.7 : 1 (32 : 18.8 : 4).
- [ ] Receiver 25 long, 3 tall, 4 wide, pale ice-blue on the upper two units of the sides, with a dark 1-unit lower band.
- [ ] Cover plate 25 x 1 x 3 with a 9-long, 1-high rear hump.
- [ ] Rail 13 x 1 x 2 floating at y -4 to -3, with an open 1-unit window below it from x -1 to 9.5.
- [ ] Rear sight: a 3 x 2 x 3 pillar and two ears with a 0.8 notch; front post and horned front sight at x 9.5 to 10.5.
- [ ] Muzzle: a short (5 long) black diamond, a base diamond, a ring pair and two ears; the muzzle face is black.
- [ ] Underbody: stepped dark blocks from x -1.5 to 11.5, 3 deep at the middle, with a small foot nub at the front.
- [ ] Grip tilted 25 degrees with a thin front strap, reaching y 12.3 or more.
- [ ] A diagonal floor bar rising to the rear, lowest point (y 13.8) near the grip's bottom.
- [ ] Thumbhole slit visible, slanted 25 degrees, about 3 wide at the top, 5 to 6 tall; plus the small notch behind it under the receiver.
- [ ] Butt block 2 x 4 x 4 at x -15 to -13 with a pale top and three dark steps below, narrowing 4, 3, 2.
- [ ] Three black vent slots and one red (#B80000) stripe on the receiver's sides, the stripe crossing the top.
- [ ] Palette: ice-blue #B8DEE1, slate #2E343D, graphite #2B2B2B, black muzzle, red #B80000.
- [ ] No part floats; every box overlaps or touches a neighbor.
- [ ] Optional: Silver (grayscale) and Dark Green (black-olive) variants share the geometry.

## 10. Unsure

- **Positions of the vents and the red stripe.** Their x positions come from reading a 64x64 texture strip, not from a rendering; treat them as plus or minus 1.5 units.
- **Thin planes.** The front sight planes, the trigger plane and the guard floor plane have zero thickness in the original; they are close to invisible from some views. Thicken them slightly or skip the trigger plane. A tiny dark trigger (at most 1 wide, 2 tall) in front of the thumbhole is a harmless deviation.
- **Tilt directions.** The 25-degree and 15-degree tilts lean the bottom toward the rear (checked against the reference render). The heel hook and brace lean the other way, as listed.
- **Muzzle ring pair.** The square ring and the diamond ring share one slot; it reads as a small star. If it looks noisy, a single octagonal collar is a harmless alternative.
- **Single-texel dots** on the Silver and Dark Green stripe spots are optional.
- **First-person framing numbers** come from formulas read, not seen in game. Treat them as approximate.
