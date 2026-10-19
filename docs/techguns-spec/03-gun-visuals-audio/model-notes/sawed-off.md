# Sawed-off shotgun: clean-room model notes

Design notes for rebuilding the Techguns **Sawed-off** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelSawedoff](../gun-models.md#modelsawedoff-6432-family-x-20-boxes-1-part), [rendering framework](../rendering-framework.md), [Techguns sawed-off stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = one texel at the native 64x32 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention), with **Y = 0 at the top face of the barrels** (the beads therefore sit at negative Y). **Z = 0 is the gun's mid-plane**; sizes across Z are given as total width, centered. **X = 0 is the rear face of the barrels.** Everything is whole-unit and axis-aligned: **there are no rotated parts at all**. (The original's own coordinates sit half a unit off-center sideways; this document has already corrected for that, so the gun is symmetric left to right.)

## 1. Gestalt

A **cut-down side-by-side double-barrel shotgun**: two fat, bright, light-gray barrel tubes laid side by side and joined by thin ribs, with a **short, dark-brown wooden stock stub** directly behind them. It is a break-action coach gun with the stock chopped, drawn with chunky boxes. At a glance you read: one long pale slab (the barrels) that is almost three quarters of the whole gun; a small dark-brown wedge at the back that steps down toward the rear; and, from the front, **two black bores side by side**. There is **no pump, no forend, no pistol grip, no magazine, no trigger guard, no hammer and no rail**. It is a plain, blunt, brutal-looking tool.

Defining features in order of visual importance:

1. **Twin barrels**: two parallel tubes, each 2 wide and 4 tall, with ribs filling the gaps, that read from the front as two black bores side by side.
2. **Bright pale-gray metal against dark wood.** Two materials, two clear colors; the barrel is the lightest thing, the wood the darkest.
3. **Barrels are about 72 percent of the length**; the wooden stock is a short stub, about 28 percent.
4. **Stepped stock**: the wood's top and bottom lines both step *down* as you go rearward, ending in a tall, slightly wider butt plate. No curve, no rake.
5. **Two tiny bead sights** at the very muzzle, and a small flat metal lever on top where barrels meet wood.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt back to muzzle) | 39 |
| Total height (bead tip to butt bottom) | 9 |
| Total width (outer ribs) | 7 |
| Length : height : width | about 5.6 : 1.3 : 1 |
| Barrel length | 28, about **72 percent** of the total |
| Barrel block (tubes) height | 4; with ribs, width 7 |
| Wooden stock length | 11 (x -11 to 0) |
| Stock height | 4 at the grip section, **6 at the butt** |
| Stock width | 3 at the grip, **5 at the butt and at the side plates** |
| Everything sits within | x -11 to 28, y -1 to 8, z -3.5 to 3.5 |

The **top line** runs: barrel top at Y 0 for the whole barrel length, the beads poke 1 unit above it at the muzzle, then the top **drops by 1** at the small lever (Y 1) and **by 2** at the stock (Y 2). The **bottom line** runs: barrel bottom at Y 4, a thin wood strip under the rear barrels (to Y 5), then the stock steps down to Y 6, 7 and 8 toward the back.

## 3. Part by part

All parts are plain boxes. Z widths are centered on the mid-plane. "Touches" means faces in contact or overlapping.

### Barrel tubes (two)
- Each tube is a box **28 long, 4 tall, 2 wide**, side by side: the left tube covers z -2.5 to -0.5 and the right tube z 0.5 to 2.5, so there is a **1-wide gap** between them at the center line. X 0 to 28, Y 0 to 4.
- They are **not round**. The roundness is painted (section 5); the geometry is square-section.
- The muzzle faces (x = 28) are two 2x4 rectangles, each painted with a black bore.
- No taper, no muzzle crown, no flare.

### Ribs (three)
- Each rib is 28 long and 2 tall (Y 1 to 3), 1 wide, running the full barrel length (x 0 to 28).
- **Center rib:** z -0.5 to 0.5, in the 1-wide gap between the tubes. It fills the gap only between Y 1 and 3, so above (Y 0 to 1) and below (Y 3 to 4) the gap is open: a shallow groove runs along the top and the bottom between the tubes.
- **Outer ribs:** one on each outside face (z -3.5 to -2.5 and z 2.5 to 3.5), also Y 1 to 3. They make the barrel block 7 wide, with a half-height ridge on each outside flank (2 of the 4 units).
- Ribs are the same metal as the tubes.
- Seen from the front, the barrel block is therefore a **cross-like outline**: 5 wide in the top and bottom rows (the two tubes with a groove between them) and 7 wide in the middle two rows (tubes plus the outer ribs).

### Bead sights (two)
- Two 1x1x1 cubes on top of the barrels, **flush with the muzzle face** (x 27 to 28), at Y -1 to 0. They sit side by side on the inner edge of each tube: z -1 to 0 and z 0 to 1 (so the two beads almost touch across the center line).
- Same metal. They are the only parts rising above the top line.

### Breech nubs (four)
- Tiny pieces on the rear face of the barrels (x -1 to 0): two **1 wide, 2 tall** nubs (Y 1 to 3) at the outer positions (z about -2.5 to -1.5 and 1.5 to 2.5), and two **1x1x1** cubes on the top row (Y 0 to 1, one on each tube, at z about -1 to 0 and 0 to 1).
- They read as small stubs closing the barrel ends. Same metal. They are barely visible; do not omit them but do not exaggerate them.

### Top lever
- A flat metal bar, **4 long, 1 tall, 3 wide** (x -4 to 0, Y 1 to 2, z -1.5 to 1.5), lying on top of the stock's front end and ending against the barrels' rear face.
- Its top is **1 unit below the barrel top**, so it reads as a small step between barrels and wood. It is the break-action lever.

### Lock block
- A short wooden box **2 long, 3 tall, 3 wide** (x -2 to 0, Y 2 to 5, z -1.5 to 1.5), directly behind the barrels' lower rear. It joins the barrels to the stock and carries the lever on top.

### Grip section of the stock (the "hand" part)
- A wooden box **7 long, 4 tall, 3 wide** (x -9 to -2, Y 2 to 6, z -1.5 to 1.5). Its top is flat at Y 2, level with the butt's top.
- **Two side plates:** each a thin wooden slab **7 long, 2 tall, 1 wide**, at Y 3 to 5, one on each flank (z -2.5 to -1.5 and 1.5 to 2.5), over the same x range. They make the middle of the stock 5 wide: a lock-plate look with a ridge on each side, visible from above as a widening and from the front or back as "wings".
- **Chin block:** a small box **2 long, 1 tall, 3 wide** (x -9 to -7, Y 6 to 7) hanging under the grip section's rear end, forming a step on the underside.

### Butt
- A wooden box **2 long, 6 tall, 5 wide** (x -11 to -9, Y 2 to 8, z -2.5 to 2.5). It is the **tallest and widest** part of the stock and the rearmost thing: a flat butt plate.
- Its top is flush with the grip section's top (Y 2) and it extends 2 units below the grip section's bottom (Y 8 versus 6).
- The back face (x = -11) is a flat vertical wall, 5 wide by 6 tall. There is no rake, no curve and no cheek rest.

### Under-barrel strip and sliver
- **Wood strip:** a box **6 long, 1 tall, 3 wide** (x 0 to 6, Y 4 to 5, z -1.5 to 1.5), directly under the barrels' rear half, touching the lock block. It reads as a thin wooden lower edge extending forward from the stock (there is no actual trigger).
- **Sliver:** an 8x1x1 wood bar (x 0 to 8, Y 3 to 4, z -0.5 to 0.5) tucked into the groove between the tubes on the underside. It is mostly hidden: only a thin line of wood shows between the barrels from below.

### What is not there
No trigger, no trigger guard, no hammers, no safety, no forend, no pump, no pistol grip, no rear sight, no rails, no scope mount, no sling swivels. Do not add any of these.

## 4. Silhouette checks

- **Side (main view):** a long, pale, thick horizontal bar (4 tall) with two small bead bumps at the very front end. At the rear it meets a small flat step (the lever, 1 down), then the wood: a dark block whose top is 2 below the barrel top, and whose bottom steps down in stages (thin strip under the barrels, lock block, grip section, chin, then the butt hanging lowest). The butt's back edge is a straight vertical line. Negative space: the notch above the stock where the barrel top overhangs (2 units high, 9 long), and the stepped notches beneath the grip section. The light barrel reads about **2.5 times as long as the dark wood**.
- **Top:** a 7-wide pale slab for most of the length (with a visible groove down the middle and the two beads at the front end); behind it, a narrow (3-wide) dark neck about 2 long, then a 5-wide plate-and-wings section, then the 5-wide butt.
- **Front:** two near-black rectangular bores side by side inside a pale block; the outer flanks show the small half-height ribs sticking out on each side; the dark stock peeks out below and behind, wider at the butt than the grip. The two beads are at the top.
- **Back:** the butt plate dominates: a 5-wide, 6-tall brown rectangle with the pale barrel block showing as light "ears" sticking out above its top-left and top-right (the barrels are 7 wide, wider than the butt), with the lever and breech nubs between them.
- **Underside:** the pale barrels' bottoms with a faint groove; the wood strip and the sliver along their rear half; then brown stock with the side plates and the butt.

## 5. Materials and palette

The original texture is neutral gray (no tint) for the metal and a dark olive-brown for the wood. Values sampled from its regions:

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Barrel tubes and ribs | bright steel | #B6B6B6 | #BEBEBE (up to about #C4C4C4 on top faces) | #959595 (underside and outer flanks) |
| Bead sights, breech nubs | steel, a bit darker | #9A9A9A | #A6A6A6 | #8E8E8E |
| Top lever | steel | #A4A4A4 | #BABABA | #8E8E8E |
| Muzzle bores | black | #000000 | | |
| Stock wood (all wooden parts) | dark olive-brown wood | #4E4123 | #5E4C28 | #3D331C |

In renders the wood reads slightly warmer, closer to a dark walnut brown, than the raw values; the builder may warm it a little but should keep it **dark**: clearly darker than orange oak, and much darker than the metal.

Shading style:
- **Metal:** bright, nearly white-gray with **horizontal brushed streaks** (a few levels of gray noise running along the barrel's length). The top faces are lightest; the underside and the outer flanks are around #959595. It is not blued or dark. No wear, no engraving.
- **Bores:** the front face of each tube is almost completely **black**, with a thin pale rim (about 1 unit) at the top and bottom edges of the face. It reads as a round-ish dark hole.
- **Wood:** dark, mottled grain; vertical streaks on the butt, horizontal streaks on the sides of the grip section. The brightest wood pixels are only mid brown (#5E4C28). No orange panels, no screws.
- All wooden parts share one wood look; all metal parts one metal family.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 2.2 | idle offset about (0.1, -0.1, -0.5). At that scale the 39-unit model is about 5.4 blocks long, so it fills the right of the screen and runs off the frame: you see the butt and the barrels' rear, turned 45 degrees. The muzzle is out of view. |
| First person firing | roll about the Z axis, up to about 15 degrees at mid-animation | plus a shift of about 0.25 backward and 0.15 up at the peak. A single quick kick. |
| First person reloading | roll up to 70 degrees at mid-animation | shift about 1.0 sideways and 0.5 up at the peak, on top of the idle offset. The gun swings up and aside, then comes back (a sine hump over the reload). |
| First person aimed | no scope and no zoom overlay | nothing beyond the bead sights. |
| Third person (held) | yaw 225 degrees, scale 1.5 | offset (-0.5, -0.25, 0). |
| Third person aimed | extra offset (0, 0.15, 0) | |
| Ground (dropped) | yaw 90 degrees, base scale | no extra lift recorded. |
| Inventory icon | yaw 180 degrees, scale 0.7 | no sprite; the icon is the 3D model, side-on. |

**Muzzle flash:** one flash for the whole gun (both barrels fire together as one shot), a sprite at scale 1.85 placed at a fixed offset of about (-0.55, -2.15, 1.05) in the first-person frame. In gun-model terms the muzzle point is the **center of the front face**: x 28, y 2, z 0, between the two bores.

Our current model is 51 units long and was scaled down to 0.9 to fit. With the right 39-unit length that compensation should be unnecessary; the original's scale numbers are the starting point (adjust with screenshots).

## 7. Motion

- The **model has no moving parts**. The barrels do not break open and no hammer or lever moves. Do not invent a hinge. All motion is whole-gun transforms.
- **Recoil:** fire interval 4 ticks, fire animation 5 ticks, flash 5 ticks. Third person: the gun pitches up about 40 degrees around the wrist (a point about 0.2 behind and 0.1 above the model origin) in the first quarter of the first half of the animation, then settles back.
- **Reload:** 28 ticks (1.4 s). First person: the big 70 degree tip-up and swing described above, as a smooth hump. Third person: the gun tilts about 40 degrees around the wrist at mid-animation and returns.
- **Optional (not in the original):** the builder may keep the lever as a separate named part, but the rest pose must match this document.

## 8. What our earlier attempts got wrong

(Restating the review findings for this gun.)

- **Wrong part breakdown.** Ours came from the generic shotgun archetype: receiver, barrel, **pump forend**, stock, **pistol grip** and a rib tube. The original has no pump, no grip and no rib on top. Remove them.
- **Wrong length and proportion.** Ours spans 51 units with a 14-long stock, a 14-long receiver and an 11-long forend: the parts list of a full-size pump shotgun. The original is 39 units, 28 of them barrel, with an 11-long stub stock.
- **A single barrel block.** Our front view showed one dark rectangle. The original is **two tubes** with a 1-wide groove and visible black bores.
- **Wrong materials.** Ours used a tan checker texture for the furniture, which reads as sand or camo, and near-black barrels. The original is **bright pale-gray** barrels and **dark brown wood**: the reverse contrast of ours.
- **Thin, detached-looking stock.** Ours was 3 deep against a 4-deep receiver and narrowed from above. The original's stock is 3 wide at the grip with 5-wide wings and a 5-wide butt, wider at the butt than the grip.
- **Missing details of the original:** the two beads at the muzzle, the half-height outer ribs, the top lever, and the stepped underside.
- **Things added that do not belong:** a tube rib on top, a forend and a pistol grip. Also do not add a hammer or trigger guard even though one is tempting on a double-barrel; the original has neither. (The review lists them as missing; that is its guess, not a feature of the original.)

## 9. Acceptance checklist

- [ ] Length : height : width is within about 10 percent of 5.6 : 1.3 : 1 (39 : 9 : 7).
- [ ] Barrels are about 72 percent of the length (28 of 39); the wood stub is about 11.
- [ ] Front view shows **two** separate bores, near-black, side by side, in a pale block.
- [ ] Barrel block is 7 wide with a 1-wide groove between the tubes and half-height ribs on each outer flank.
- [ ] Top line is the barrel top for its full length; only the two beads rise above it (by 1) and they sit at the very muzzle.
- [ ] A small metal lever (about 4 x 1 x 3) sits where barrels meet wood, one unit below the barrel top.
- [ ] Stock top is 2 below the barrel top and is flat from the lever to the back.
- [ ] Stock underside steps down toward the rear in stages, ending in a butt that hangs 2 lower than the grip section.
- [ ] Butt is the widest and tallest stock part (5 wide, 6 tall) and its back face is a vertical flat wall (no rake).
- [ ] Grip section has a 1-unit side plate on each flank, making it 5 wide in the middle.
- [ ] A thin wood strip runs under the barrels' rear half; no trigger guard, no hammer, no pump, no pistol grip, no rail.
- [ ] Metal is bright neutral gray (about #B6B6B6 base, #959595 shadow), bores are #000000, wood is dark olive-brown (about #4E4123).
- [ ] Gun is left-right symmetric about the mid-plane.
- [ ] No rotated parts: every part is axis-aligned.
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Bore paint.** The front faces look black with a pale rim in a very low-resolution render; the exact rim width (0 to 1 unit) is my reading. A fully black face is also acceptable.
- **Wood warmth.** The raw texture values are quite dark and olive; the renders look warmer. If the builder's wood looks too dark in game, warming slightly is fine.
- **Breech nubs.** They are tiny and hard to see in the renders; their exact positions are from the numbers, not from looking.
- **Idle framing.** The first-person numbers come from the formulas read, not from play. At scale 2.2 the gun may run off the frame; treat the offsets as approximate.
- **Gameplay fit (not a model question).** The original fires one shot (eight pellets) per trigger pull from a 2-shell gun at one shot per 4 ticks, reloading shell by shell (28 ticks for a full load), with a single muzzle flash. Our definition matches those numbers (2-shell magazine, incremental reload of up to 2, 28-tick reload, auto at 4 ticks). The only wrinkle is thematic: a double-barrel that autofires every 4 ticks and shows no break-open on reload. The original behaves the same way, so I recommend no change; the owner may decide later whether a break-open animation is worth adding.
