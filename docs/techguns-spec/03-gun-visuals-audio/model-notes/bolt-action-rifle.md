# Bolt-action rifle: clean-room model notes

Design notes for rebuilding the Techguns **Bolt Action Rifle** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and three textures. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelBoltaction](../gun-models.md), [rendering framework](../rendering-framework.md), [Techguns bolt-action behavior](../../02-guns/per-gun-behavior.md#bolt-action-rifle-boltaction), [rifle sheet](../../02-guns/sheets-rifles-shotguns.md#bolt-action-rifle-boltaction).

**Units and axes used in this document.** One unit = one texel at the native 128x32 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention), with **Y = 0 at the top of the scope's two bells**, the highest point of the gun. **Z = 0 is the gun's mid-plane**; sizes across Z are total width, centered. **-Z is the shooter's right side** (the side the bolt lever sticks out); **+Z is the left side**. **X = 0 is the back face of the butt.** Rounded to the nearest half unit unless it matters.

## 1. Gestalt

A slim, long, **old-fashioned hunting rifle**: a dark-brown wooden body that runs unbroken from the butt to the middle of the barrel, a very thin dark-gray barrel that carries on a long way past the wood, a **small black scope** riding on two little steel mounts, and a **straight horizontal bolt lever** sticking out the right side of the action. It is lean and elegant, nothing like a bulky modern sniper. There is no magazine, no bipod, no pistol grip, no rail and no muzzle brake. Brown and black dominate; the only light notes are the little steel bolt stub, the mounts and the lever.

Defining features in order of visual importance:

1. **Slim proportions**: about 53 long and only 12 tall, with a body 3 wide. It reads as a thin line in the top and front views.
2. **Wooden body with a flat top line and a dropped heel**: the stock, receiver and fore-end are one brown wooden mass whose top is dead flat; only the heel of the butt drops below.
3. **A thin, free-floating dark barrel** that continues about 15 units past the end of the wood.
4. **A compact black scope**: a thin tube with a fat bell at each end, held up on two short steel posts.
5. **A plain bolt lever**: a short steel stick pointing straight out to the right, at the back of the action.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt to muzzle) | 53.5 |
| Total height (scope top to heel bottom) | 12 |
| Total thickness | 3 for the body; 5 across the stock cheek plates; 6 including the bolt lever (3.5 out to the right of the mid-plane, 2.5 to the left at the cheek plates) |
| Length : height : body thickness | about 18 : 4 : 1 |
| Barrel length | 38, about **70 percent** of the total length |
| Barrel section | 2 by 2, no taper |
| Wooden mass (butt back to fore-end front) | 38 long (x 0 to 38) |
| Barrel beyond the wood | 15.5 (x 38 to 53.5) |
| Scope length | 13 (x 12 to 25); bells 3 by 3 by 3; tube 2 by 2 |
| Butt (heel) height | 7 (y 5 to 12); the rest of the stock is 4 tall |

Our earlier attempt is about 82 long, roughly **1.5 times** this. The original is short-bodied: do not stretch it.

How the pieces sit along the length, from the rear: the butt (x 0 to 3); the wrist (x 3 to 11); the rear of the action (x 11 to 13); the receiver (x 13 to 20); the fore-end under the barrel (x 20 to 33); a thin top plate that carries on to x 38; and the bare barrel to the muzzle at x 53.5. The scope sits above x 12 to 25. The bolt lever is at x 14.5 to 15.5.

The **top line of the wood is perfectly flat** at y = 5 from the butt (x 0) to x 38. Everything else (barrel, scope, bolt stub) rises above it.

## 3. Part by part

All parts are boxes. Z widths are centered on the mid-plane.

### Butt
- A vertical block, **3 long (x 0 to 3), 7 tall (y 5 to 12), 3 wide.** Its top is flush with the flat top line, and it hangs 3 units lower than the wrist's underside, so the heel drops. Its back face is a flat butt plate: no hook, no curve, no pad.
- Wood.

### Wrist
- A box **8 long (x 3 to 11), 4 tall (y 5 to 9), 3 wide**, attached to the butt's front face. Top flush with the flat line.
- Wood. This is the straight "arm" of the stock; there is no comb rise, no pistol grip.

### Cheek plates (stock sides)
- Two thin plates, one on each side of the stock, each **9 long (x 0 to 9), 2 tall (y 6 to 8), 1 thick**, flush to the stock's flanks. With the 3-wide body they make the stock **5 wide** along its rear 9 units. They sit at mid-height, not at the top or bottom, so from the side they read as a slightly different-toned panel in the stock's middle, and from the top as small wings beside the butt and wrist.
- Wood, a hair lighter than the stock body.

### Stock underside bumps
- Two small low bumps under the stock: one **3 long, 1 tall, 3 wide** at x 3 to 6, y 9 to 10 (just ahead of the butt), and one **2 long, 1 tall, 3 wide** at x 10 to 12, y 9 to 10 (under the wrist's front end). The stock's underside is therefore slightly stepped, with a shallow gap of 4 between the bumps.
- Wood. The original has no trigger guard loop and no trigger.

### Action block (rear of the receiver)
- A box **2 long, 3 tall, 3 wide** at x 11 to 13, y 6 to 9. It bridges the wrist and the receiver and reaches the same depth as the wrist (y 9).
- Wood.

### Receiver
- A box **7 long (x 13 to 20), 2 tall (y 6 to 8), 3 wide**, directly under the top plate. The underside steps up here from the wrist (y 9) to y 8.
- Wood.

### Top plate
- A long thin plate, **24 long (x 14 to 38), 1 tall (y 5 to 6), 3 wide**, forming the top of the receiver and then the top of the fore-end. It ends 15.5 units short of the muzzle and continues the flat top line past the stock.
- Wood.

### Fore-end under-plate
- A plate **13 long (x 20 to 33), 1 tall (y 6 to 7), 3 wide**, directly under the top plate. Together they make a fore-end **2 tall and 3 wide from x 20 to 33**, then 1 tall from x 33 to 38. The underside therefore climbs in small steps toward the front (y 9, then 8, then 7, then 6).
- Wood.

### Barrel
- A square bar **38 long (x 15.5 to 53.5), 2 by 2** in section, **not rotated**: a plain square, not a diamond. Centered at y 4.5 (y 3.5 to 5.5) and z = 0.
- It sits on top of the top plate, sunk about 0.5 unit into it, so its top is **1.5 above the flat wood line**. Its rear 4.5 units (x 15.5 to 20) lie within the receiver region, and the scope mounts straddle it.
- It is **narrower than the 3-wide wood**: 0.5 unit of wood shows on each side in the top view.
- No taper, no muzzle crown, no front or rear sight, no rib, no underlug. One clean dark bar; the **free-floating front 15.5 units are the most visible thing in the silhouette**.
- Dark gray.

### Bolt stub
- A small light-steel bar, **7 long, 1 square** (x 13.5 to 20.5, y 4 to 5, centered on z = 0), lying on the top plate and half sunk into the barrel's rear.
- Most of it is hidden inside the barrel. The only visible part is a **2-unit light stub behind the barrel's rear end** (x 13.5 to 15.5), just under the scope's rear bell. It reads as a small light dot or short bar from the side. Do not make it bigger.

### Bolt lever
- A steel stick, **1 long (x 14.5 to 15.5), 1 tall (y 4 to 5), 3 long across Z**, attached to the stub's right side, from z = -0.5 out to z = -3.5. It points **straight sideways, perpendicular to the barrel**, and sticks out **2 units past the right flank** of the 3-wide body.
- Plain stick: **no knob, no ball, no downward angle.** It is the only part that crosses the right side of the body.
- Mid-gray steel, darker than the stub.
- From above it is a short stub on the right of the spine, just under the scope's rear bell.

### Scope
Black, compact, held above the barrel.
- **Two bells**, each **3 by 3 by 3** (y 0 to 3): the rear bell at x 12 to 15 and the front bell at x 22 to 25. They are the highest points of the whole gun.
- **Tube:** **8 long (x 14 to 22), 2 by 2** (y 0.5 to 2.5), between the bells. It is thinner than the bells by half a unit on every side, so the scope reads as a dumbbell from the side.
- The scope has **no colored lens**: the bell ends are the same black as the rest. Do not add blue lenses (see section 8).
- **Mounts:** two steel posts, each **1 long, 2 tall, 3 wide** (y 3 to 5), at x 16 to 17 and x 20 to 21. They run from under the tube down to the top plate and straddle the barrel (they are wider than it). Each carries a **1x1x1 steel cap** on top (y 2 to 3) that tucks into the tube's underside.
- From the side the gap between the bell bottoms (y 3) and the plate top (y 5) is 2 units tall and shows the posts as two short light bars with the dark barrel behind them.
- **Turrets:** one **2 by 2 by 1** turret (x 17.5 to 19.5, y 0.5 to 2.5) on the **left** side of the tube (z 1 to 2). One tiny **1x1x1 knob** (x 18 to 19, y 1 to 2) on the **right** side (z -1.5 to -0.5), sticking out 0.5 past the tube. Both are black.

## 4. Silhouette checks

- **Side (the main view):** a long slim horizontal line. From the rear: a brown butt that drops 3 units below the wrist, a straight brown top edge running to x 38, a stepped brown underside (shallow notch between the stock's bumps, then steps up at the receiver and the fore-end), a small black dumbbell-shaped scope above the middle on two light posts, and a thin dark barrel carrying on far beyond the wood. Negative space: the 2-unit gap between scope and plate with the barrel visible in it; the long empty space under the barrel past x 38; the notch under the wrist; the open steps under the receiver. **Nothing hangs under the barrel and nothing under the action except the stock steps.**
- **Top:** a very thin spine. A 2-wide dark barrel flanked by 0.5 unit of wood on each side over the wood mass, with the 5-wide cheek plates making faint wings at the back (x 0 to 9). A black scope with fat 3-wide bells and a thin tube between them, a small knob on the right and a turret nub on the left. A **steel stick crossing to the right at x 14.5 to 15.5**, reaching 2 units beyond the wood: the one lateral feature.
- **Front:** a tall narrow column. On top, a black 3x3 square (the front bell). Below it a 2x2 dark barrel dot. Below that a brown 3-wide fore-end and the taller brown butt behind it (down to y 12), with the cheek plates just visible at the sides at mid-height. A light steel lever peeks out on the gun's right (viewer's left) at about barrel height.
- **Back:** a brown column 3 wide and 7 tall (the butt face), the cheek plates at its sides above its middle, and the black rear bell on top. The lever tip sticks out to the right.
- **Underside:** a narrow brown bar, 3 wide (5 across the cheek plates at the back), with a thin dark barrel carrying on at the front. Nothing else.

## 5. Materials and palette

The original texture is **dark and warm**: the whole wooden body is a very dark chocolate brown, the barrel a dark charcoal gray, the scope nearly black, with a few light steel accents. The values below were sampled from its regions.

**Default variant**

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Butt, wrist, receiver, action block, top and under plates | dark walnut | #3C2B1D | #402F1F | #35281B |
| Cheek plates | same wood, slightly lighter | #402F1F | #433120 | #392B1C |
| Underside bumps | same wood | #372A1B | #3C2B1D | #35281B |
| Barrel | dark gunmetal | #3C3C3C | #404040 | #383838 |
| Scope bells and tube | near-black | #161616 | #1E1E1E | #0F0F0F |
| Scope turret and knob | near-black | #161616 | #181818 | #151515 |
| Scope mounts and caps | mid-light steel | #737373 | #777777 | #717171 |
| Bolt stub | light steel | #B5B5B5 | #BBBBBB | #B2B2B2 |
| Bolt lever | mid steel | #646464 | #656565 | #606060 |

**Camo 1, "Olive":** same layout; the wood becomes dark olive green and the metal parts are unchanged.

| Part | Base | Highlight | Shadow |
|---|---|---|---|
| Wooden body | #2E3C1A | #32401E (#344220 on the cheek plates) | #2A3816, #283614 |
| Barrel, scope, mounts, bolt, lever | as the default | | |

**Camo 2, "Tactical Black":** everything dark.

| Part | Base | Highlight | Shadow |
|---|---|---|---|
| Wooden body | #292929 | #2C2C2C | #252525 |
| Barrel | #1B1B1B | #1C1C1C | #151515 |
| Bolt stub | #252525 | #262626 | #0F0F0F |
| Bolt lever | #101010 | #101010 | #0F0F0F |
| Mounts and caps | #131313 | #151515 | #121212 |
| Scope | #090909 | #0D0D0D | #050505 |

Shading style:
- **Everything is flat with light per-pixel noise**: two to four neighboring levels per surface. No gradients, no scratches, no wear.
- **Wood** is a nearly uniform dark brown with faint streaks. It is not orange, not red and not light. At a distance the default reads as almost black-brown.
- **Barrel** is uniform dark gray, a little lighter in value than the wood, hairline-lighter along the top.
- **Scope** is flat black with almost no variation.
- **Steel accents** are the only light notes, in a clear value ladder: bolt stub lightest, then mounts, then lever. They are tiny, so keep them clearly lighter than their surroundings.
- The builder needs three textures (default, olive, tactical black) over one geometry.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up). The original model scale is 1.0.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person idle | yaw -45 degrees, scale 1.5 | offset about (-0.09, -0.37, 0.07) plus the hold offset (0.10, -0.25, -0.50). The gun lies diagonally, muzzle away and to the left of screen center, butt at the lower right. At 53.5 units long it fills much of the view at scale 1.5; the builder's own profile will scale to fit. |
| First person firing | a quick kick in the first quarter, then a slow rechamber | Kick: roll about Z up to 25 degrees with a shift of about 0.5 along the gun axis; the gun jumps back and up. Rechamber: gentle sway of up to 4 degrees roll, 16 degrees yaw and 4 degrees pitch, with a 0.25 slide toward the shooter. This fakes working the bolt. |
| First person reloading | yaw up to 50 degrees, roll 10, pitch 10 | the gun swings outward, rises about 0.5 and moves about 1 block at mid-reload, then returns. A big, slow swing. |
| First person aimed | model hidden; screen overlay | A sniper-scope overlay replaces the model (overlay scale 2.75). Zoom: field of view times 0.35. |
| Third person (held) | yaw 225 degrees, scale 1.5 | offset (-0.65, -0.25, 0). |
| Third person recoil | roll up to 20 degrees about a point about 0.9 behind and 0.3 above the model origin | first half of the animation only. |
| Third person reload | pitch up to 30 degrees and yaw up to 40 degrees at mid-animation | |
| Third person aimed | extra offset (0, 0.2, -0.25) | |
| Ground (dropped) | yaw 90 degrees, scale 0.9 | lowered by 0.25. |
| Inventory icon | yaw 180 degrees, scale 0.6 | offset (-0.4, 0.1, 0); drawn side-on. There is no sprite; the icon is the 3D model. |

**Muzzle flash point:** the front of the barrel on its axis (model position x 53.5, y 4.5, z = 0), a flash sprite at about 1.85 times scale, drawn forward of the muzzle.

**Scope overlay:** the usual sniper-scope reticle, drawn on screen when zoomed. It is not part of the model, and the model carries no iron sights.

## 7. Motion

- The **model has no moving parts.** The bolt stub and lever are static geometry; the cycling is faked by the whole-gun rechamber motion above.
- **Recoil and rechamber:** a 15-tick animation: the kick in the first quarter, then the sway for the remaining three quarters. Fire delay 25 ticks.
- **Reload:** about 50 ticks (2.5 s).
- **Optional (not in the original):** the builder may keep the stub and lever as one separate named part so later code can slide it back during the rechamber. The rest pose must match this document: stub at x 13.5 to 15.5, lever out to z = -3.5, nothing else visible.

## 8. What our earlier attempts got wrong

Restated from the review of our current rifle:

- **A black slab stock.** Ours is a plain 24x9x4 box in near-black: no wrist, no drop, no cheek plates, no fore-end. The stock is the biggest mass and it must be **dark brown wood** with a flat top, a 3-unit heel drop and a stepped underside, continuing into a receiver and fore-end of the same wood.
- **Too long: about 82 against 53.5.** Keep the whole gun at 53.5, with the barrel 38.
- **An extra bipod.** The original has none. The flat 9x9x2 tab under the fore-end must go. Nothing hangs under the barrel.
- **An extra magazine.** The original has no magazine and nothing under the receiver but the stepped wood. The 5x9x3 block must go.
- **No bolt handle.** The defining feature is missing. Add the light stub and the 3-long lever sticking straight out to the right.
- **A fat, heavy barrel.** The original is a thin 2x2 dark-gray bar.
- **Blue scope lenses.** The original scope is solid black with no color.
- Do not add things the original lacks: no pistol grip, no trigger guard loop, no iron sights, no muzzle crown, no rail, no flash hider.

## 9. Acceptance checklist

- [ ] Length : height : body thickness is within about 10 percent of 18 : 4 : 1 (53.5 : 12 : 3).
- [ ] Total length is 53.5 units, not our old 82.
- [ ] Barrel is a plain 2x2 square bar, 38 long, dark gray, with no taper or crown, extending about 15 units past the wood.
- [ ] The wooden body (butt, wrist, receiver, fore-end) is dark brown with a perfectly flat top line.
- [ ] The butt drops 3 units below the wrist's underside and its back face is flat.
- [ ] The stock has two thin cheek plates making it 5 wide over its rear 9 units, from a 3-wide body.
- [ ] The underside shows the two small stock bumps and the stepped climb (stock, receiver, fore-end).
- [ ] The scope is a black dumbbell: two 3x3x3 bells and a 2x2 tube between them, 13 long overall, on two short steel posts with caps.
- [ ] The scope's bells are the highest point of the gun and the scope has no colored lens.
- [ ] A light bolt stub sits behind the barrel's rear end, and a mid-gray 1x1x3 lever sticks straight out to the right, 2 units past the body.
- [ ] No bipod, no magazine, no pistol grip, no trigger guard loop, no iron sights.
- [ ] Palette: wood about #3C2B1D, barrel about #3C3C3C, scope about #161616, mounts about #737373, bolt stub about #B5B5B5, lever about #646464. Olive wood about #2E3C1A; tactical black wood about #292929.
- [ ] Three texture variants (default, olive, tactical black) share one layout.
- [ ] Side silhouette matches section 4; the top view is a thin spine with one lever crossing to the right.
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Left and right.** I read the lever as on the shooter's right from the front render. A renderer that mirrors differently could flip it. The rule: the lever is on the same side as the small scope knob and opposite the larger turret nub.
- **Wood color.** The default texture is very dark brown (about #3C2B1D); lighting in the reference renders makes it look lighter. Trust the hex values over render brightness.
- **Cheek plates.** Their size and position are certain, but I do not know what the designer meant them to be (a cheek rest or an inlay).
- **Action block and underside bumps.** Sizes are measured, but whether they were meant as a trigger area or a grip is unclear. They are the only features under the stock; the original has no guard loop, trigger or magazine.
- **Reload time and scope overlay scale.** The reload time (about 50 ticks) comes from the catalog row and the overlay scale from the renderer; neither was seen in game.
- **First-person framing numbers** come from formulas read, not seen in game. Treat the offsets as approximate.
