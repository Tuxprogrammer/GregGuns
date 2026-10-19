# Blaster Rifle: clean-room model notes

Design notes for rebuilding the Techguns **Blaster Rifle** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [gun-models](../gun-models.md), [rendering framework](../rendering-framework.md), [Techguns gun stats](../../02-guns/gun-catalog.md).

**Units and axes used in this document.** One unit = 1/16 block. Muzzle toward **+X**. **X = 0 is the rearmost point** (the rear cap). **Y points down** (Minecraft model convention), with **Y = 0 at the top face of the top rail**. **Z = 0 is the receiver's mid-plane**; widths across Z are total widths, centered. **+Z is the magazine side** (the side that carries the side-mounted cell kit); **-Z is the clean, flat side**. Rounded to the nearest half unit unless it matters. Several parts are squares turned 45 degrees about the gun's long axis, so they read as diamonds from the front; "diamond N across" means the corner-to-corner width.

## 1. Gestalt

A long, dark, **sci-fi sniper-style energy rifle**: nearly black gunmetal from end to end, with no second color anywhere. At a glance you read: a very long finned barrel tube (about 40 percent of the gun) running out of a boxy receiver; a short fat scope riding on top; a pistol grip with a trigger guard under the receiver; a diamond-shaped cap at the rear; and, on one side only, a **bulky bolted-on cell assembly** (a tall box beside the scope and a flat slab that sticks out sideways). The muzzle end is a small diamond plate with a black square on its face.

Defining features in order of visual importance:

1. **Long diamond-section barrel guard with an X of cooling fins**, 19 long, ending in a thin diamond muzzle plate. Seen from the front it is a diamond with four small fin tips poking out of its four flat faces.
2. **Three-segment diamond scope** on top (fat rear tube, fat middle tube, thin front tube), short and stubby, set back over the rear half of the receiver.
3. **Asymmetric side kit** on the +Z side: a tall box at the rear of the scope, a socket box and a wide flat slab sticking about 8 units out sideways. The other side is flat and plain.
4. **Rear diamond cap**, bigger than the receiver (it sticks out about 1.5 units each side and 0.5 above the rail).
5. **Raked pistol grip** (about 25 degrees) with an open trigger guard and a lighter, slim **rail bar under the barrel**.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (rear cap to muzzle plate) | 50 |
| Total height (scope top to grip bottom) | about 20 (scope top at Y -4, grip bottom at Y 16) |
| Total width, body only | 4 (receiver), 7 at the rear cap's corners |
| Total width including the side kit | about 14 (Z from -3.5 to +10.5) |
| Length : height : body width | about 12.5 : 5 : 1 |
| Receiver | 28 long (X 2 to 30), 4 wide, 5 tall (Y 1 to 6) |
| Barrel guard | 19 long (X 30 to 49), 5.7 across |
| Barrel plus muzzle plate | 20, 40 percent of the total |
| Scope | 15 long (X 8 to 23) |
| Grip | 9 long, 3 wide, 4 deep, rake about 25.6 degrees back |

Along the length, from the rear: the diamond cap (X 0 to 2); the receiver (X 2 to 30) carrying the scope over its rear half (X 8 to 23) and the grip and guard under X 10 to 22; then the barrel guard (X 30 to 49); then the 1-long muzzle plate (X 49 to 50).

## 3. Part by part

All parts are boxes; "diamond" parts are square bars turned 45 degrees about the X axis. Z widths are centered on the mid-plane unless a Z range is given.

### Receiver
- Main block: **28 long, 5 tall, 4 wide** (X 2 to 30, Y 1 to 6, Z -2 to +2). A plain slab-sided block with no cut-outs.
- **Top rail:** a 2-wide, 1-tall strip along nearly all of the receiver top (X 2 to 29, Y 0 to 1, Z -1 to +1).
- **Lower receiver:** a 3-wide, 2-tall block under the front-middle of the main block (X 14 to 29, Y 6 to 8, Z -1.5 to +1.5). It forms the base of the grip and magazine area and continues forward as the under-barrel rail.
- **Rear cap:** a 5x5 square plate, 2 thick (X 0 to 2), centered on the receiver's mid-height (Y 3), **turned 45 degrees so it is a diamond 7 across and 7 tall** (Y about -0.5 to 6.5, Z about -3.5 to +3.5). It is the biggest cross-section on the gun and its corners overhang the receiver on all four sides.

### Barrel guard (the long tube)
- A 4x4 square bar turned 45 degrees: **diamond 5.7 across, 19 long** (X 30 to 49), axis at Y 3.5, Z 0. Its top corner is at Y 0.7 (just under the rail top) and its bottom corner at Y 6.3.
- Butts into the receiver's front face.
- **Cooling fins:** two thin crossed blades, each 1 thick, 5 across (so they reach 2.5 from the axis), 16 long (X 30 to 46, stopping 3 short of the guard's front end). Tilted +45 and -45 degrees they form an **X**. Their four tips reach 2.5 from the axis along the diagonals, while the guard's flat faces are only 2 from the axis there, so each fin tip **pokes out about 0.5 unit** from the middle of one flat face. Visible as four thin ridges running most of the barrel's length. They are darker than the guard.
- **Muzzle plate:** a 3x3 square, 1 thick (X 49 to 50), turned 45 degrees: a diamond 4.2 across, centered on the barrel axis. Its front and back faces each carry a small black mark (see section 5).

### Under-barrel rail and fairing
- **Rail bar:** a 2x2 square bar, 15 long (X 29 to 44, Y 5.5 to 7.5, Z -1 to +1), directly under the barrel. It is the lighter, slimmer-looking bar beneath the guard. Its top overlaps the guard's lower corner; its bottom is about 1.2 below the guard's bottom corner. It begins 1 unit behind the receiver's front face and lines up with the lower receiver as if it were its continuation.
- **Front fairing:** a 3-wide, 2-tall, 7-long wedge (Z -1.5 to +1.5) near the muzzle end, between X 41 and 48. It is tilted about 13 degrees so that its **front end is higher** (top at Y 4.75, hidden inside the guard) and its rear end drops to Y 6.3 to 8.3, emerging below the guard. Seen from the side, a short slanted keel under the front third of the barrel.

### Scope (on top)
Three diamond tubes, all centered on the receiver's mid-plane, stacked front to back:

| Tube | X range | Diamond across | Center Y | Vertical extent |
|---|---|---|---|---|
| Front, thin | 21 to 23 | 2.8 | -1 | about -2.4 to 0.4 |
| Middle | 16 to 21 | 4.2 | -1.5 | about -3.6 to 0.6 |
| Rear, long | 8 to 16 | 4.2 | -2 | about -4.1 to 0.1 |

The rear tube sits 0.5 higher than the middle one, so the scope's top line steps up slightly toward the rear. All three overlap the top rail a little (the diamonds' lower corners dip to Y 0.1 to 0.6). There is no objective bell, no turret knobs and no mount rings.

### Side cell kit (+Z side only)
- **Tall side box:** 3 wide, 5 tall, 8 long (X 10 to 18, Y -2 to 3, Z +2 to +5). It rises 2 units above the top rail and 2 above the receiver's top edge. It is flush against the receiver's +Z face, beside the rear tube of the scope.
- **Socket box:** 4 wide, 3 tall, 7 long (X 21 to 28, Y 2 to 5, Z +1.5 to +5.5), flush against the receiver (it overlaps 0.5 into it).
- **Flat slab (the "magazine"):** 5 wide (Z +5.5 to +10.5), 2 tall, 5 long (X 22.5 to 27.5, Y 2.5 to 4.5). It attaches to the socket box's outer face and sticks straight out sideways. This is what makes the gun about 14 wide.
- **Two small diamond rods** lying on top of the socket box, each 1x1 turned 45 degrees (1.4 across), 6 long (X 22 to 28), centered at Z +3.25 and +4.75, Y 1.75, half embedded in the socket top.
- **Little cap plate:** 3 wide, 1 tall, 1 long (X 21 to 22, Y 1 to 2, Z +2.5 to +5.5) on the socket box's front-top edge.

### Grip and trigger guard
- **Grip:** a 3-wide, 9-long, 4-deep block (Z -1.5 to +1.5), hinged at its **top front corner (X 18, Y 8)** and tilted about **25.6 degrees** so the bottom swings back. Corners: top front (18, 8), top rear (14.4, 6.3), bottom front (14.1, 16.1), bottom rear (10.5, 14.4). Its top touches the underside of the lower receiver. Straight-sided, no finger grooves.
- **Trigger guard:** a front post, 2 wide, 3 tall, 1 long (X 21 to 22, Y 8 to 11) and a floor bar, 2 wide, 1 tall, 5 long (X 16 to 21, Y 10 to 11). The floor's rear end overlaps the grip's front face by about 1 unit. The opening is about 3 long.
- **Trigger:** a 1x2x1 blade (Z -0.5 to +0.5) at X 18 to 19, Y 7.5 to 9.5, tilted about 15 degrees so its bottom leans forward.

## 4. Silhouette checks

- **Side (from the clean -Z side):** a long low bar, with a small short scope bump on its rear half, a diamond rear cap (taller than the receiver, so a small bump at the very back), a sloping grip with an open trigger guard, then a fine, long barrel with a thin lighter bar under it that ends before the muzzle, and a slanted fairing under the front third. The gun's silhouette is mostly flat on top. Negative space: the triangle between the grip's front and the guard; the gap between the scope's front tube and the barrel.
- **Side (from the +Z side):** the same, plus the tall box at the rear and the socket box under the scope's middle; the 5-long slab hides most of the scope's lower half.
- **Top:** a very long thin spine (4 wide receiver, narrowing to 5.7 diamond barrel), a **flat T-shaped bulge** in the middle where the socket and slab stick out to the +Z side (about 8 beyond the receiver), the scope as a 4-wide stripe on the centerline, the rear cap as a hexagon-like 7-wide shape.
- **Front:** a diamond (the muzzle plate, 4.2) in front of a larger diamond (guard, 5.7) with four tiny fin bumps, with the receiver 4x5 block behind it and the scope diamond peeking above. The slab and boxes stick out to the +Z side.
- **Back:** the big 7x7 diamond cap dominates, with the scope top and the grip bottom visible above and below it.
- **Underside:** a narrow receiver, the lower receiver, the grip and guard in the rear half, the rail bar and fairing in the front half.

## 5. Materials and palette

The original texture is near-black gunmetal throughout. All values below are measured from the texture (its pixels are two per model unit; noise is slight).

| Part | Base | Highlight | Shadow / seam |
|---|---|---|---|
| Receiver (main block, top rail, lower receiver) | #212121 | #292929 to #313131 on edges | #171717 |
| Rear cap | #2A2A2A (front), #2F2F2F (rim) | #3C3C3C | #1B1B1B |
| Barrel guard | #1C1C1C | #272727 | #080808 to #131313 |
| Cooling fins | #111111 | #161616 | #0A0A0A |
| Muzzle plate (side faces) | #1A1A1A | #212121 | #171717 |
| Muzzle plate end faces | #1A1A1A, each with one small **black (#000000) square** about 1.5 to 2 units across, low on the face | | |
| Rail bar (under barrel) | #303030 | #484848 | #2A2A2A. The **lightest metal on the gun**. |
| Front fairing | #2A2A2A | #3F3F3F | #232323 |
| Scope tubes | #121212 | #161616 | #0D0D0D (the **darkest**, nearly black) |
| Grip | #242424 | #323232 | #1B1B1B. A fine **checkerboard** pattern (alternating 1-unit light and dark squares) on its front and sides. |
| Trigger and guard | #1F1F1F to #242424 | #272727 | #161616 |
| Side kit (tall box, socket, slab, rods) | #1E1E1E | #2F2F2F to #383838 | #101010 |

Shading style:
- **Flat, matte, very dark gunmetal**: nothing is shiny and nothing is colored. The whole gun lives between #101010 and #303030, with the rail bar and rear cap slightly lighter and the scope and fins slightly darker.
- Faint **per-pixel noise** (a few levels), a few tiny **white-gray rivet specks** along some seams, and **thin darker rectangular panel outlines** near the lower edges of the receiver faces. These are subtle.
- No glow, no emissive part, no paint stripes. Any energy look comes from the muzzle flash and the projectile.

## 6. In-game presentation

All transforms apply to the whole model after its 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up). The model is drawn once; there is only one part.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw 225 degrees, scale 1.4 | offset (0.1, -0.1, 0), then an idle shift of (-0.55, -0.35, -0.65). Scoped overlay when aiming (a sniper-style scope is drawn; the gun has a zoom). |
| First person firing | pitch kick of up to about 5 degrees about X plus a pushback of up to 0.15 along the gun axis | the kick peaks at mid-animation. |
| First person reloading | tilts about 50 degrees about X at mid-animation | smooth rise and fall. |
| First person aimed | scope overlay; extra offset (0, 0.05, 0.2) in the held view | |
| Third person (held) | yaw 135 degrees, scale 0.95 | offset (0.1, -0.5, 0.9). |
| Ground (dropped) | no yaw, scale 0.65 | lowered by 0.45. |
| Inventory icon | yaw 90 degrees, scale 0.48 | offset (0.3, 0, 0); drawn side-on. There is no sprite; the icon is the 3D model. |

**Muzzle flash:** a laser-style flash sprite, scale 1.5, drawn at about (-1.05, -2.8, -0.45) in the gun's flash frame (just in front of the muzzle, slightly to the clean side), additive blend. **Scope overlay:** offset (1.795, 0, -0.685) in its own frame.

## 7. Motion

- The **model has no moving parts.** No bolt, no sliding magazine, no spinning fins. All motion is whole-gun transforms.
- **Recoil (third person):** the gun pitches about 15 degrees in the first quarter of the first half of the animation, then settles.
- **Reload (third person):** about 30 degrees pitch and 40 degrees yaw at the animation's middle, with a small offset (0, -0.2, 0.05), then back.
- **Optional (not in the original):** the builder may keep the slab, the scope and the fins as separate named parts for later animation, but the rest pose must match this document.

## 8. What to avoid

- **Do not make it symmetric.** The cell kit (tall box, socket, slab, rods) is on **one side only**; the other side stays flat.
- **Do not make it colorful or glowing.** No blue or green emissive cells. It is black.
- **Do not make the scope big.** Its tubes are only 4 across, about the width of the receiver.
- **Do not round the barrel.** It is a 45-degree diamond with four fin ridges.
- **Do not forget the rear diamond cap** and its overhang; without it the back looks like a plain box.
- **No stock, no bipod, no top-rail teeth, no side rails.** The original has none.
- Do not add a magazine in the grip; the "magazine" is the sideways slab.

## 9. Acceptance checklist

- [ ] Length 50, height about 20, body width 4; overall proportions within 10 percent of 12.5 : 5 : 1 (without the side kit).
- [ ] Barrel assembly is about 40 percent of the total length; the barrel guard is 19 long.
- [ ] Barrel guard is a diamond (5.7 across) with four fin tips poking out about 0.5 from its flat faces.
- [ ] Muzzle plate is a thin diamond (4.2 across) with a small black square on its end.
- [ ] A slim, lighter 2x2 rail bar sits under the barrel for 15 units and ends before the muzzle.
- [ ] Scope is three diamond tubes (2.8, 4.2, 4.2 across) in 2 / 5 / 8 unit lengths, front to back, over the rear half of the receiver.
- [ ] Rear cap is a diamond 7x7, larger than the receiver.
- [ ] The side kit exists on one side only, and the slab sticks out about 8 beyond the receiver.
- [ ] Grip is tilted about 25 degrees, 3 wide, 9 long, with a checkerboard texture.
- [ ] Trigger guard is open, with a front post, a floor bar and a small tilted trigger.
- [ ] The whole gun is near-black (#121212 to #303030); no hue anywhere.
- [ ] Top line is flat; only the scope and the side box rise above the rail.
- [ ] No part floats; every box overlaps or touches a neighbor.
- [ ] No moving parts; first-person scope overlay is supported by the gun, not the model.

## 10. Unsure

- **Which side the cell kit shows on in first person.** The transforms suggest the kit sits on the side facing away from the player (the camera sees the clean side), but this comes from reading formulas, not from watching the game. Treat as approximate.
- **The sideways slab.** It looks odd as a magazine; it is exactly what the model contains (a 5x2x5 slab attached outside the socket box). Keep it; do not tuck it back inside.
- **Black mark on the muzzle plate.** The texture region for this 3x3 plate has one small black square on each end face; the size and the low position are read from a very low-resolution image, so treat them as approximate.
- **Fin tip length.** By the numbers each tip sticks out 0.5; this is a fine detail that may read as a hairline.
- **Camo variants:** none. The original has a single texture.
