# LMG: clean-room model notes

Design notes for rebuilding the Techguns **LMG** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelLMG](../gun-models.md), [LMG sheet](../gun-sheets.md), [rendering framework](../rendering-framework.md), [Techguns LMG stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = one texel at the native 128x128 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention), with **Y = 0 at the top face of the main top cover** (the long flat cover over the receiver); things above it have negative Y. **X = 0 is the rear face of the stock butt**, so the muzzle tip is at X = 73. **Z = 0 is the gun's mid-plane**; sizes across Z are total widths, centered unless stated. "Left" and "right" are the shooter's, holding the gun. **Z is positive toward the shooter's left**, which is the **belt side** (the side where the ammunition belt climbs out of the box). Rounded to the nearest half unit unless it matters.

The scope is the gun plus its **ammo box and the six visible belt rounds** as they appear on the gun. The separate magazine items are not covered.

## 1. Gestalt

A big, long, dark **belt-fed light machine gun** in near-black gunmetal. At a glance you read: a long, slab-sided receiver with a flat top and a rail; a **skinny barrel much thinner than the receiver**, ending in a tall A-shaped front sight; a **boxy dark-green ammo box hanging under the middle of the gun** with a short **brass ammo belt** curling up its left side into the receiver; a pistol grip leaning back and a vertical foregrip under the handguard; a small **red-dot sight in a box hood** on top; and a chunky, **stepped, two-plate stock** at the rear. It is heavy and squat at the back, lean at the front. The palette is almost monochrome: charcoal and black, with two small flashes of color (dark teal-green box, bright brass belt) plus a single red dot.

Defining features in order of visual importance:

1. **Long, tall receiver with a flat top rail**, about half the gun's length, plus a long thin barrel in front of it.
2. **Wide ammo box underneath**, wider than anything else on the gun (13 across), with one 45-degree-chamfered shoulder.
3. **Brass belt of rounds** climbing the left side of the receiver from the box. The only bright yellow on the gun.
4. **Stepped rear stock** with two converging side plates, a tall butt block and a stair-stepped top.
5. **A-shaped front sight** and a **square hooded red-dot sight** on top, the highest point of the gun.
6. A leaning pistol grip and a vertical foregrip with a diamond cross-section.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt to muzzle) | **73** |
| Total height (red-dot hood top at Y -5 to foregrip bottom at Y 18.5) | about **23.5** |
| Total width | **13** (the ammo box); everything else is 5 or less |
| Length : height : width | about 5.6 : 1.8 : 1 |
| Receiver (rear plug to front of the forward body) | X 16 to 55, about 39 long |
| Barrel | 18 long, X 55 to 73, a quarter of the total length |
| Stock | X 0 to about 17 (rear 23 percent) |
| Receiver body width | 5 (main and forward bodies) |
| Barrel width | 2.8 (diamond) |

How the pieces sit along the length, from the rear: butt block X 0 to 8; two side plates and a tube feeding forward, X 6 to 18; the main receiver with the sight on top and the pistol grip below, X 16 to 37; the forward body with the handguard rail and foregrip underneath, X 37 to 55; the gas collar at X 55 to 57; then 16 more units of thin barrel with a gas tube under it (X 57 to 65) and a front sight (X 57 to 63).

The **top line** is stepped: the red-dot hood is highest (Y -5, X 22.5 to 26.5); the long top rail is 1 above the cover (Y -1, X 17.5 to 34.5); the main top cover is at Y 0 from X 17 to 37 (a narrower cover continues to X 53); the front sight reaches Y -2 near the muzzle; the barrel top is at Y 1.5. The **bottom line** is irregular: grip down to Y 17, box to Y 18, foregrip to Y 18.5.

## 3. Part by part

Sizes are length (X) by height (Y) by width (Z). Positions are X range, Y range.

### Top cover and rail
- **Top cover, rear:** 20 x 1 x 4, X 17 to 37, Y 0 to 1. The flat deck of the whole gun.
- **Top cover, forward:** 16 x 1 x 3, X 37 to 53, Y 0 to 1. Narrower (3 across), so a 0.5 ledge shows each side where the rear cover is 4.
- **Top rail:** 17 x 1 x 2, X 17.5 to 34.5, Y -1 to 0, centered. A picatinny strip above the rear cover; the red-dot sight sits on it.
- **Shoulder strip:** 18 x 1 x 4, X 37 to 55, Y 1 to 2, directly under the forward cover.

### Receiver (main, forward and lower bodies)
- **Main body:** 21 x 5 x 5, X 16 to 37, Y 1 to 6. The thick block of the receiver; it sticks out 0.5 each side past the top cover.
- **Forward body:** 18 x 6 x 5, X 37 to 55, Y 2 to 8. Same width as the main body, **hanging 2 lower at the bottom** than the main body (Y 8 vs 6). The long handguard-like block.
- **Lower body:** 10 x 3 x 5, X 27 to 37, Y 6 to 9. Under the main body, over the ammo box.
- **Rear lower block:** 8 x 3 x 3, X 19 to 27, Y 6 to 9. Narrower (3 wide); the base the pistol grip hangs from.
- **Rear plug:** 2 x 2 x 3, X 17 to 19, Y 6 to 8. Where the receiver meets the stock tube.
- Joints are tight; there are no windows or cut-outs in the geometry. All detail on the receiver sides is painted (section 5).

### Barrel, gas tube and collar
- **Barrel:** a 2x2 square bar **18 long** turned 45 degrees about its own axis, so it shows a **diamond**, about 2.8 across and 2.8 tall. X 55 to 73. Top corner at Y 1.5, bottom corner Y 4.3 (axis Y 2.9). Mid-plane centered. Plain bar: no flash hider, no taper, no ribs.
- **Gas tube:** a 2x2 bar **8 long**, also a diamond (2.8 across). X 57 to 65, top corner Y 4.5, axis Y 5.9, bottom corner Y 7.3. Runs directly beneath the barrel with a hairline (about 0.2) gap. Ends 8 short of the muzzle.
- **Gas collar:** a 3x3 square block turned 45 degrees (diamond, 4.2 across), only **2 long**, X 55 to 57, axis Y 5.9 (top corner Y 3.8, bottom corner Y 8). The fat ring where the barrel and tube leave the forward body; it is wider than both (4.2 vs 2.8) and gives the front a "coupler" look.

### Front sight
A small A-frame tower on the barrel, X 57 to 63.
- **Sight base:** 4 x 2 x 1, X 57 to 61, Y 3 to 5. Thin block overlapping the barrel's top corner.
- **Post:** 1 x 3 x 1, X 60 to 61, Y 0 to 3, vertical, rising from the front of the base.
- **Cap:** 2 x 2 x 1, X 61 to 63, Y -2 to 0, on top of the post, protruding 1 forward. The highest point of the front half.
- **Strut:** 1 x 4 x 1, tilted 25 degrees (top forward, bottom rearward), from the cap's rear (X about 60.5, Y -1) down to the base's rear (X about 58.5, Y 2.5). Post and strut together form a leaning "A" in side view.
- All 1 wide across Z, centered, in lighter gunmetal than the receiver (#414141).

### Underside: handguard rail and foregrip
- **Underrail:** 12 x 1 x 2, X 37 to 49, Y 8 to 9, centered; hangs under the forward body's rear half.
- **Foregrip mount:** 4 x 2 x 3, X 41.5 to 45.5, Y 8.5 to 10.5, overlapping the rail.
- **Foregrip:** a vertical post **2x2 in section turned 45 degrees about the vertical axis** (diamond, 2.8 across), **8 tall**, Y 10.5 to 18.5, centered at X 43.5, one corner pointing forward. Almost black (#111216).

### Pistol grip and trigger
- **Grip:** a plain slab 3 wide, 4 long, 9 tall, **leaning back 25.6 degrees from vertical** (bottom rearward). Its top (Y 7.3) meets the rear lower block; its bottom reaches Y 17.1. Along X it runs from about X 23 at the top front to X 15.5 at the bottom rear corner.
- **Trigger guard:** a front post 1 x 3 x 2, X 26 to 27, Y 9 to 12; a **floor** 5 x 1 x 2, X 21 to 26, Y 11 to 12. An open loop closed at the rear by the grip face. Dark blue-charcoal (#21242C).
- **Trigger blade:** 1 x 2 x 1, X 23 to 24, Y 8.5 to 10.5, tilted 15 degrees (bottom forward). Hidden inside the loop, barely visible.

### Ammo box and belt
- **Box lower block:** 7 long x 6 tall x 13 wide, X 28 to 35, Y 12 to 18, Z -7 to 6 (7 to the right of the mid-plane, 6 to the left). The widest part of the gun.
- **Box upper block:** 7 x 4 x 9, X 28 to 35, Y 8 to 12, Z -3 to 6. Flush with the lower block on the left, stepped in by 4 on the right.
- **Chamfer:** a 3 x 6 x 7 slab turned 45 degrees about the length axis, filling the step on the right as a **diagonal shoulder from the upper block's right edge (Z -3, Y 8) down to the lower block's right edge (Z -7, Y 12)**. From front or back the box is a **crate with a flat roof on the left half, a 45-degree slope on the right, vertical sides and a flat bottom**.
- The box sits directly under the lower body: its top (Y 8) overlaps the lower body's bottom (Y 9) by 1. It is centered along the length at X 31.5 and only 7 long: a short deep box, not a long magazine.
- **Belt:** six brass cartridges, each a 1x1x6 bar turned 45 degrees (diamond, about 1.4 across), lying lengthwise along X (6 long, X 28.5 to 34.5). They form a **short curved column on the left side**, listed bottom to top by center (Z, Y):
  1. (5.0, 8.2), on the box's upper left corner
  2. (4.5, 7.2)
  3. (4.5, 6.2)
  4. (4.5, 5.2)
  5. (4.0, 4.2)
  6. (3.0, 3.7), the last round, tucked against the receiver's left face (which is at Z 2.5)

  The belt rises about 4.5 units and curves inward by 2 at the top; each round sits about 1 unit above the previous one, so they touch.
- Belt rounds are **shown according to remaining ammo**: the top round only with more than 1 round loaded, the next with more than 2, and so on to all six at more than 6. During the first half of a reload all six are hidden; in the second half they return under the same ammo rules.

### Stock
- **Butt block:** 8 long x 7 tall x 3 wide, X 0 to 8, Y 3 to 10. The big rear brick.
- **Stair top:** two thin 3-wide plates on top: 7 x 1, X 0 to 7, Y 2 to 3, and 6 x 1, X 0 to 6, Y 1 to 2. With the butt block, the **rear face is 9 tall (Y 1 to 10) and the top steps down toward the front** in 1-unit steps (Y 1 up to X 6, Y 2 to X 7, Y 3 to X 8).
- **Heel nub:** 1 x 1 x 3, X 8 to 9, Y 9 to 10, a small stub at the lower front of the butt block.
- **Stock tube:** 8 long x 3 tall x 3 wide, tilted about 10 degrees so its **rear end is higher** than its front. It runs from the rear plug (X 17 to 19, Y 6 to 8) back toward the butt: Y 5 to 8 at the front, rising to about Y 3.6 to 6.6 at the rear end near X 10.
- **Side plates:** two slabs, each 10 long x 5 tall x 3 wide, X about 6 to 16, one on each side of the tube. Front ends at X 16 (Y 1 to 6); rear ends drop about 2 (Y 3 to 8). Each plate is **yawed 6 degrees inward toward the rear**: at the front the pair spans about 5 across (Z -2.5 to 2.5, overlapping at the middle), at the rear they converge to 3 across and merge with the butt block. From behind or above the stock is a **narrow wedge**, wide at the receiver, narrow at the butt.
- Stock color is nearly black (#15161A), darker than the receiver.

### Red-dot sight (on top)
- **Base:** 8 x 2 x 3, X 21.5 to 29.5, Y -2 to 0, on the top rail.
- **Cheeks:** two 5 x 2 x 1 plates on the base's sides, X 22.5 to 27.5, Y -2 to 0, at Z -2 to -1 and Z 1 to 2 (so base plus cheeks is 4 wide).
- **Uprights:** two 4 x 3 x 1 posts, X 22.5 to 26.5, Y -5 to -2, at Z -2 to -1 and Z 1 to 2, leaving a 2-wide open gap between them.
- **Roof:** 4 x 1 x 2, X 22.5 to 26.5, Y -5 to -4, closing the top of the gap.
- **Window:** a 1x1 flat pane facing the shooter, in the gap at X 23.5, Y -3.5 to -2.5, showing **two red pixels**. Nothing else is lit.
- Dark gray-black (#23262C). A plain box hood, open toward the muzzle. The highest point of the gun.

## 4. Silhouette checks

- **Side (the main view):** a long, horizontal, dark bar. From the rear: a tall brick-like stock with a stepped top and a wedge drop to the receiver; a flat top line with the small boxy red-dot hood sticking up in the rear third; the receiver as a thick bar of constant depth; a **deep rectangular ammo box hanging below** the middle (7 along the gun, 10 deep); the pistol grip leaning back **behind** the box with an open trigger loop in front of it; the vertical foregrip hanging forward of the box, about as deep as the box; the thin barrel with a thinner gas tube beneath and a fat ring where they emerge; and the leaning A-shaped front sight at the end. The brass belt shows only from the left. Negative space: the gap between the grip/trigger loop and the box; the gap between the box and the foregrip; the long slot beneath the barrel; and the large empty area under the stock tube between the butt and the grip.
- **Top:** a long narrow strip (receiver 5 wide) tapering at the rear where the stock plates wedge from 5 to 3. A **7-long, 13-wide protrusion in the middle** (the box), reaching 6 to the left and 7 to the right. The barrel is a thin line (2.8) forward of the receiver. Rails and the red-dot hood form a thin 4-wide spine.
- **Front:** a tall block: receiver 5 wide, red-dot hood on top, the diamond barrel in the middle (with the 4.2 collar behind it) and a thin tube below; the **trapezoid ammo box** behind and below, much wider (13) than the receiver, with a sloped right shoulder and the belt column rising on the left. The foregrip is a dark diamond column in front of the box's center.
- **Back:** a tall narrow stock (3 wide, 9 tall) in the center with the sight hood above it. The box, wider on both sides, rises behind it as a flat-bottomed trapezoid. The belt column shows on the left.
- **Underside:** a long thin bar with the box as a 13-wide rectangle in the middle, the grip and trigger guard behind it, the foregrip diamond ahead of it, and the thinner barrel and gas tube beyond.

## 5. Materials and palette

The original texture is a single dark sheet with almost no color: near-black gunmetal in several close shades, a dull gray barrel, a dark teal-green box and brass rounds. Values were measured from its regions.

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Receiver (main, forward, lower bodies, covers) | dark blued steel | #24272F (also #252830) | #272A33 | #21242C; black #111216 for slots |
| Rear lower block, rear plug | same steel | #24272F | #262931 | #21242C |
| Top rail | near-black steel | #272A33 | #272A33 | #000000 (alternating stripes) |
| Underrail | near-black steel | #272A33 | #272A33 | #000000 |
| Barrel, gas tube | gunmetal gray | #414141 | #414141 | #393939 |
| Gas collar | gunmetal gray | #393939 | #393939 | #393939 |
| Front sight (all pieces) | gunmetal gray | #414141 | #414141 | #23262E (edges) |
| Pistol grip | blue-charcoal | #24272F | #2E323C | #21242C |
| Trigger and guard | blue-charcoal | #21242C | #21242C | #21242C |
| Foregrip and its mount | near-black | #111216 | #191B21 | #111216 |
| Stock (butt, plates, tube, stair, heel) | very dark gray | #15161A | #17181C | #131418 |
| Ammo box | dark teal-green | #223331 | #223331 (nearly flat) | #1D2C2A, #1A2725 |
| Belt rounds | brass | #E7DD71 | #E7DD71 | #D5CC6C (second face), #363419 (dark tip or gap) |
| Red-dot hood | dark gray-black | #23262C | #23262C | #23262C |
| Red-dot window | red | #FF0000 | | |

Shading style:
- **Metal:** flat and dark, with extremely subtle per-pixel noise: only two or three neighboring blue-charcoal shades (#21242C, #24272F, #252830, #272A33). Nothing is shiny. Black (#111216 or #000000) is used as a deliberate accent, not as shadow.
- **Receiver sides:** a **row of four narrow black vent slots** on each side of the forward body: each slot 1 wide and 4 tall, on a 2-unit pitch (black, gray, black, gray...), starting about 3.5 units behind the body's front and ending about 10 units behind it (X about 45 to 51.5), in the upper 4 of the body's 6 rows (Y 2 to 6). The main body's sides carry a thin 1-unit lighter line along the middle. On its **left (belt-side) face** a dark patch 7 long and 2 tall (lower area, around X 28 to 35) marks the **feed slot** where the belt enters, right above the box.
- **Top rail:** alternating black and dark-gray stripes along its length (picatinny teeth).
- **Barrel:** even gray with a faint lighter side. The collar is slightly darker (#393939) than the barrel (#414141).
- **Ammo box:** nearly flat dark teal-green with small darker patches (#1D2C2A), like painted ammo-can steel. No labels, handles or latches. An optional faint diagonal lighter panel on the side faces.
- **Belt rounds:** two alternating brass yellows (#E7DD71 and #D5CC6C) with a dark olive (#363419) at the ends; no separate lead or primer colors.
- **Stock and foregrip:** the darkest parts of the gun, almost black.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first flipped 180 degrees so Y-down becomes screen-up.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw 225 degrees, scale 1.4 | offset about (-0.55, -0.35, -0.65). The camera sees the gun from the shooter's right-rear, nearly side-on, muzzle pointing forward; the stock and the right (no belt) side are nearest. |
| First person firing | pitch up to about 5 degrees at peak, plus a slide back of up to about 0.15 | mild recoil. |
| First person reloading | pitch up to **55 degrees**, ramping in over the first 30 percent, holding to 70 percent, ramping out over the last 30 percent | plus the idle offset. A big lift, no sideways movement. |
| First person aimed | no scope overlay | a small shift of (0, -0.05, -0.1) from the aimed base so the red dot sits on the view axis. |
| Third person (held) | yaw 135 degrees, scale 0.8 | offset (0.1, -0.65, 1.1). Hangs from the hand, muzzle forward. |
| Third person recoil | pitch -15 degrees peaking about one quarter into the first half of the animation, then back | |
| Third person reload | offset (0, -0.2, 0.05), pitch up to 30 degrees and yaw -40 degrees at mid-animation, then back | |
| Ground (dropped) | scale 0.6 | lowered by 0.5. |
| Inventory icon | scale 0.48, yaw 90 degrees | offset (0.3, 0, 0). The icon is the 3D model, side-on. |

**Muzzle flash point:** the front of the barrel on its axis (model X 73, Y about 2.9, Z = 0), a flash sprite at about 1.5 times scale.

## 7. Motion

- The **model has no moving parts.** The belt rounds only toggle visibility (section 3). All motion is whole-gun transforms.
- **Recoil:** mild; with a high rate of fire it reads as a small shudder.
- **Reload:** the gun lifts about 55 degrees and holds, then drops back. The belt rounds vanish for the first half of the reload and return in the second.
- **Optional (not in the original):** the builder may keep the belt rounds, the box and the sight as separately named parts so later code can animate or hide them, but the rest pose must match this document.

## 8. What to avoid

- **A thick barrel.** The barrel is 2.8 across against a 5-wide receiver; no heat shroud, no flash hider.
- **A magazine-style box.** The box is short (7 long) and 13 across, wider than the receiver, not a long stick. Include the sloped right shoulder.
- **Bright or colorful metal.** The whole gun is near-black; only the box (teal-green), belt (brass) and dot (red) carry color. No camo, wood, orange or white.
- **A rounded or modern receiver.** Everything is flat slabs, with no bevels or rounded covers.
- **A centered belt, or no belt.** The belt is on the left side only and six rounds long.
- **A one-block stock.** It needs two converging side plates, the tilted tube and the stair-stepped top.
- **A missing foregrip diamond or gas tube.** Keep the diamond foregrip under the rail and the thin tube under the barrel.
- **A plain single-post front sight.** It is an A-frame with a cap and a leaning strut.
- **A roofless hood or a glowing lens.** The sight is a boxy hood with a two-pixel red dot only.
- Do not add a bipod, carry handle, scope, charging handle, suppressor or magazine well; the original has none.

## 9. Acceptance checklist

- [ ] Length : height : width is within about 10 percent of 5.6 : 1.8 : 1 (73 : 23.5 : 13).
- [ ] Barrel is a thin diamond bar about 2.8 across and 18 long, a quarter of the total length.
- [ ] A shorter parallel gas tube (about 8 long, ending well short of the muzzle) sits under the barrel, with a fat 4.2-wide diamond collar at the barrel's base.
- [ ] Receiver is a 5-wide slab body with a 4-wide flat top cover; a 2-wide striped top rail sits above it.
- [ ] Forward body hangs lower than the main body (to Y 8 vs Y 6) and has four black vent slots on each side.
- [ ] Ammo box hangs under the receiver, is the widest part (about 13), about 7 long and 10 tall, with a 45-degree sloped shoulder on the **right** upper corner.
- [ ] Six brass belt rounds climb the **left** side from box to receiver, curving inward at the top; the top rounds disappear as ammo drops.
- [ ] Pistol grip leans back about 25 degrees with an open trigger guard in front.
- [ ] A vertical diamond-section foregrip about 8 tall hangs under the handguard rail, nearly black.
- [ ] Stock has a tall butt block (9 tall at the rear face), a stepped top, a tube rising toward the butt (about 10 degrees) and two side plates converging in plan.
- [ ] Front sight is a leaning A-frame with a cap about 3.5 above the barrel's top corner, not a single post.
- [ ] Red-dot sight is a boxy hood with a roof, 4 wide, the highest point (5 above the top cover), with two red pixels.
- [ ] Palette: receiver #24272F, barrel #414141, stock #15161A, foregrip #111216, box #223331, brass #E7DD71, red dot #FF0000.
- [ ] No part floats; every box overlaps or touches a neighbor.
- [ ] Side silhouette matches section 4 (box under the middle, grip behind the box, foregrip in front of it, A sight at the muzzle).

## 10. Unsure

- **Left versus right.** The belt side and the vent-slot faces were read from the original's side renders (the belt shows when the camera is on the gun's left); the texture's face order is ambiguous. If a render disagrees, keep the belt and feed slot together on one side and the box's chamfer on the other.
- **Stock joints.** The junction of the tube, side plates and rear plug is the most tangled area; match the stock's silhouette more than the exact joint.
- **Trigger blade.** Tiny and mostly hidden; its tilt direction is a best reading.
- **First-person framing numbers** come from formulas read, not seen in game; treat the offsets as approximate. The muzzle flash scale is likewise approximate.
- **Camo variants:** none. The original has a single texture and no alternate skins.
- **Vent slot count and positions** are read from a low-resolution texture; four slots per side on a 2-unit pitch is the best reading.
