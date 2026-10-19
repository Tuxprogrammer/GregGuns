# Mac-10 (and Akimbo Mac-10): clean-room model notes

Design notes for rebuilding the Techguns **Mac-10** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderers and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelMac10](../gun-models.md#modelmac10-6432-family-z-23-boxes-tabula-1-part), [rendering framework](../rendering-framework.md), [Techguns Mac-10 stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = one texel at the declared 64x32 scale = 1/16 block. (The original PNG is 128x64, a 2x paint of that scale, so paint detail is finer than the geometry.) Muzzle toward **+X**. **Y points down** (Minecraft model convention), with **Y = 0 at the top face of the receiver**. **Z = 0 is the gun's mid-plane**; sizes across Z are total width, centered. **X = 0 is the rear-most face of the gun** (the back of the stock butt plate), so the muzzle tip is at X = 22. Rounded to the nearest half unit unless it matters. The original is authored along a different axis and facing the other way; everything here is already converted to this convention.

## 1. Gestalt

A compact, boxy **machine pistol in the Ingram Mac-10 mold**: a fat rectangular "bread-box" receiver, a stubby barrel that barely sticks out, a pistol grip with a long light-colored magazine hanging well below it, and a **thin open wire stock** that loops off the back. It is nearly monochrome: dark gunmetal gray, with the magazine and a few metal details noticeably lighter. The gun is small in the hand but tall in profile, because the magazine is long. It is slim (3 units thick) and reads as a thin slab from the front and top.

Defining features in order of visual importance:

1. **Long rectangular receiver slab**, 17 long, 4 tall, 3 wide, with a flat, uncluttered top.
2. **Pistol grip under the rear third** of the receiver, with a **long, pale magazine** (about 6.4 tall) dropping far below the grip's bottom. The magazine is lighter than the grip.
3. **Open wire stock at the rear**: two vertical wire posts that rise **above** the receiver's top line, and thin rods running forward along the top rear corners. It reads as an open frame, not a solid stock.
4. **Very short barrel stub**, a thin diamond bar poking only 3 units past the front face.
5. **Open trigger guard**: a front post, a floor bar, and the grip's front face closing it, with a forward-leaning trigger inside.
6. **Small sight furniture on top**: a tiny diamond post, a thin front blade, and two thin vertical "ear" planes beside the blade.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (stock butt to muzzle tip) | 22 |
| Total height (top of wire posts to magazine bottom) | about 18.4 |
| Height without the posts (receiver top to magazine bottom) | about 17.4 |
| Total thickness | 3 (receiver and butt plate; everything else is 2 or less) |
| Length : height : thickness | about 7.3 : 6.1 : 1 |
| Receiver | 17 long (X 2 to 19), 4 tall (Y 0 to 4), 3 wide |
| Barrel protrusion beyond the receiver | 3 (X 19 to 22), about 14 percent of the length |
| Grip body | 3 long, 7 tall, 2 wide, at X 6 to 9: its front face is 10 behind the receiver's front face and its back face is 4 ahead of the receiver's back face |
| Magazine | about 2.4 long, 6.4 tall, 1.4 wide; below the grip (Y 11 to 17.4) |
| Grip plus magazine drop below the receiver | about 13.4, more than three times the receiver's height |
| Wire stock reach behind the receiver | 2 (the receiver's back is at X 2; the stock ends at X 0) |

Layout from the rear: butt plate and posts at X 0 to 1; connector block at X 1 to 2; receiver X 2 to 19 (grip at X 6 to 9 under it, trigger guard at X 9 to 13); barrel stub X 19 to 22. The **top line is flat** along the whole receiver; only the sight bits rise 0.4 to 0.7 above it, and the wire posts rise 1 above it at the very back.

## 3. Part by part

All parts are boxes or thin planes. "Touches" means faces in contact or overlapping.

### Receiver
- One box, **17 long x 4 tall x 3 wide**, X 2 to 19, Y 0 to 4. No bevels, no ejection cut-outs, no rail. All detail is painted (section 5).
- It is the visual mass of the gun. Everything else attaches to it.

### Barrel
- A thin square bar, **1 x 1 in section, 6 long**, turned 45 degrees about its own axis, so it shows as a **diamond about 1.4 across** both ways.
- Runs from X 16 to X 22. **Half of it is buried inside the receiver**, so only X 19 to 22 (3 units) is visible.
- Its axis is at Y about 2, the vertical middle of the receiver's front face, at Z = 0.
- No taper, no muzzle brake, no suppressor, no shroud.

### Sights and top furniture
- **Small diamond post:** a 1 x 1 x 1 cube turned 45 degrees about the vertical axis, so it reads as a diamond about 1.4 across; at X about 12.6, centered. It spans Y -0.7 to 0.3, so it rises about 0.7 above the top and is partly buried.
- **Front blade:** a very thin bar about 0.4 x 0.4 in section and 1.4 long (X 16.8 to 18.8), centered, rising about 0.4 above the top face (Y -0.4 to 0). Almost a line.
- **Ear planes:** two zero-thickness vertical plates, each 1 long and 1 tall, standing on the top face at Z about +/-1 (inside the receiver's 1.5 half-width), at X about 17.3 to 18.3, Y -1 to 0. They flank the front blade like sight wings. From the front they form a small "U" notch around the blade; from the side they are invisible slivers. A builder who needs real thickness should use the thinnest slab the pipeline allows (0.2 or less) and paint it like the receiver.

### Trigger guard and trigger
- **Front post:** 1 x 3 x 1 (long x tall x wide), at X 12 to 13, Y 4 to 7, centered. It hangs from the receiver's underside, ahead of the grip.
- **Floor bar:** 3 long, 1 tall, 1 wide (X 9 to 12, Y 6 to 7), joining the post's lower end to the grip's front face.
- **Opening:** 3 long (X 9 to 12) and 2 tall (Y 4 to 6), open and visible from the side. The grip's front face closes it at the back.
- **Trigger:** a small block 1 long, 2 tall, 1 wide, hanging from the receiver's underside just ahead of the grip (top at X about 9 to 9.5, Y 3.5 to 5.5), **tipped about 23 degrees so its lower end leans forward** toward the muzzle. It lies inside the guard opening, touching the grip's front at the top and swinging clear at the bottom. Same dark metal as the grip.

### Grip
- **Main block:** 3 long x 7 tall x 2 wide, X 6 to 9, Y 4 to 11, centered on the mid-plane. Its top is flush with the receiver's underside. A plain box, **not raked**: it is vertical.
- **Backstrap plates:** two thin plates (about 0.8 thick, 1.8 wide) just behind the grip's back face (X 5 to 6):
  - an **upper** one about 3 tall (Y 3.5 to 6.5), tilted about 12 degrees so its lower end leans forward into the grip;
  - a **lower** one about 5 tall (Y 6.2 to 11.2), tilted about 7 degrees so its lower end swings back.
  - Together they give the grip's rear edge a very slight S-curve and a swell of under 1 unit at mid-height, without changing the front face. Barely noticeable at a glance; they matter mainly in the side silhouette and for the stippled back panel.
- Grip bottom is at Y 11. It touches the receiver on top and the magazine below.

### Magazine
- A box about **2.4 long x 6.4 tall x 1.4 wide** at X 6.3 to 8.7, Y 11 to 17.4, **centered on the grip**. It is slightly narrower (1.4 vs 2) and shorter (2.4 vs 3) than the grip: a visible step of 0.3 each side at the junction.
- It is the lowest point of the gun and a strong vertical bar in the silhouette. It is **lighter** than the grip (section 5), and its bottom face is the lightest face on the gun.
- No curve, no feed lips, no ammo window. It never animates (section 7).

### Wire stock
A small open frame of thin rods behind the receiver, built from **wire sections about 0.6 across**, mostly turned 45 degrees so they show as diamonds. Left and right members sit about 0.9 to 1 from the mid-plane.
- **Vertical posts (two):** each about 5 tall (Y -1 to 4), a 0.6 square turned 45 degrees about the vertical axis (a diamond about 0.85 across), at X about 0.5 and Z = +/-1. They rise **1 above** the receiver's top line and end level with its underside. They are the tallest thing at the back.
- **Side rods (two):** bars about 0.6 x 0.6 and 3.6 long, turned 45 degrees about their own axis, running along the receiver's top rear corners at Z = +/-0.9 and Y about -0.4 (center), from X about 4.3 back to X about 0.3 where they meet the posts. They tilt 3 to 5 degrees off the length axis (slightly splayed) rather than being perfectly parallel; the exact tilt is unimportant.
- **Short rod stubs (two):** 1.6 long, same thin diamond section, at Z = +/-0.9, Y about -0.4, X 3.8 to 5.8, extending each rod's run forward over the receiver's top rear corner.
- **Cross bar:** a short transverse bar about 1.6 wide and 0.6 square, turned 45 degrees about the length axis, at X about 5.6, Y about -0.4, joining the two side rods across the top of the receiver: a small bridge.
- **Butt plate:** 3 wide, 2 tall, 1 deep (X 0 to 1, Y 3 to 5), centered. It is the widest and lowest part of the stock, hanging 1 below the receiver's underside, and joins the posts' lower ends.
- **Connector block:** 2 wide, 1 tall, 1 deep (X 1 to 2, Y 3 to 4), centered, linking the butt plate to the receiver's back face.
- **Back plate (nearly hidden):** a thin diamond plate about 3 across and 0.1 thick, standing at X about 2.6, inside the receiver's rear end. It is almost entirely buried and not visible in any view; omit it.
- The stock is rigid and fixed; it does not fold or telescope.

## 4. Silhouette checks

- **Side (the main view):** a wide rectangle (the receiver) with a short stub on the front; a tall pale bar dropping from its rear third (grip and magazine); a small open "D" under the front (guard with the trigger angled forward); and at the back an open frame: two thin vertical posts rising above the top line, a small bump (butt plate) hanging below, and thin rods across the top rear. Tiny bumps on top: the diamond post and the front blade. Negative space: the empty guard opening; the gap between the posts and the receiver (only the small connector block bridges it low down); the notch between the magazine's front face and the guard.
- **Top:** a long, narrow rectangle (3 wide) with a thinner spine for the barrel stub at the front, a dark blade and ears near the front, the diamond post midway, and at the rear the stock rods and cross bar as a small open rectangle. The butt plate (3 wide) is the widest bit at the very back.
- **Front:** an upright bar about 3 wide: the receiver face (3 x 4) with the barrel's dark diamond in its center, two tiny ears and the blade above, a thin grip column below, then a slimmer pale magazine (1.4 wide) extending far down.
- **Back:** the butt plate and two posts as a thin frame at the top, then the dark stippled backstrap, then the narrower pale magazine. Narrow overall.
- **Underside:** a long thin bar; the guard's floor bar and front post near the grip; the magazine's floor plate as a small bright rectangle at the grip's center; the butt plate at the back.

## 5. Materials and palette

Mostly **dark gunmetal** with a cool blue-green cast (parkerized steel), with lighter steel on the magazine, sights and stock wires. Values were sampled from the original texture's regions.

| Part | Material | Base | Highlight | Shadow / detail |
|---|---|---|---|---|
| Receiver (sides, top) | parkerized steel | #565D5B | #868887 on edge panels | #444947 to #454A49 |
| Receiver slots and port | dark recess paint | #373838 | | near-black, about #2A2B2B |
| Grip | dark metal or polymer | #505755 | #565D5B | #444947 to #424745 |
| Grip back panel | stippled dark | #454A49 | | checker pattern with #333434 |
| Magazine | pale steel | #9D9F9E | #A8AAA9 | #868887 / #858686 |
| Magazine floor plate | brightest metal | #A8AAA9 | | |
| Barrel | steel, dark body | #565D5B | #A6A8A7 on the top strip | #7E807F; end face #333434 |
| Sights (post, blade, ears) | steel | #9D9F9E | #A8AAA9 | #868887 |
| Wire stock (posts, rods, bar) | lighter steel | #747575 | #7B7D7C | #757776 |
| Butt plate, connector block | dark gunmetal | #565D5B | | #555B59 |
| Trigger guard post and floor | dark gunmetal | #505755 | | #454A49 |

Shading style:
- **Metal:** satin, flat, low-contrast noise of a few gray levels (neighbor pixels differ by 1 to 4 levels). No gradients, rust or wear. A slight green-blue tint (green about 5 to 8 above red, blue 0 to 4 above red).
- **Receiver:** the large side panels carry **long horizontal dark slots**. On one flank (the gun's left, from the shooter's view) several thin horizontal lines and one long dark slot in the upper half run most of the length, like cooling grooves and a bolt channel. On the other flank (the right) a short dark rectangle in the front-middle area reads as an ejection port. The top face is plain mid-gray with a slightly lighter edge and a thin dark center line over its middle third. Flank placement is approximate (section 10).
- **Barrel:** the end face seen from the front is a small near-black diamond inside a dark ring; the side faces are lighter on the top strip and darker beneath.
- **Grip:** dark and uniform with a faint stipple; **darker than the receiver** and **much darker than the magazine**, which gives the gun its "light bar below" signature.
- **Magazine:** pale steel, the lightest large surface; its bottom face is the brightest.
- **Stock wires:** clearly lighter than the receiver, but not as bright as the magazine.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up). Angles and offsets are the original's and apply in its own axes, so treat them as design intent: the gun should read as described in each view.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw 225 degrees, scale 2.0 | idle offset about (-0.55, -0.45, -0.8). The gun is large in frame, seen mostly from behind with a little of its side. The wire stock and grip are close to the camera; the barrel points away into the screen. |
| First person firing | pitch up to about 3.5 degrees at the peak | plus a rearward slide of up to about 0.15 block. A tiny, fast jolt (2-tick recoil time), not a big flip. |
| First person reloading | pitch about 50 degrees (muzzle up) at mid-reload, eased in and out | the idle offset is kept. |
| First person aimed | no scope and no zoom overlay | shifts 0.2 block along the gun axis; iron sights only. |
| Third person (held) | yaw 135 degrees, scale 1.2 | offset (0.05, -0.6, 0.4); the grip sits in the hand. |
| Third person recoil | up-pitch about 10 degrees around the wrist (a point about 0.9 block behind the model origin) | fast kick in the first quarter of the first half, then it settles. |
| Third person reload | tilt about 40 degrees about the wrist | wrist pivot about (0, 0.3, 0.9) from the model origin; smooth return. |
| Ground (dropped) | scale 0.9, flipped only | offset (0, -0.5, -0.3). |
| Inventory icon | yaw 90 degrees, scale 1.1 | offset (0.3, -0.5, 0); the icon is the 3D model drawn side-on. |

**Muzzle flash point:** just beyond the barrel tip, drawn at about (-0.65, -1.25, 1.6) in the first-person frame at scale 1.0 with the second muzzle-flash sprite. Our barrel stub is only 3 units long, so the flash should sit right at its end on the barrel axis (model position X 22, Y 2, Z 0).

### Akimbo variant (shares the model)

The **Akimbo Mac-10 uses exactly the same model and texture**: no second model, no color change. The item just renders it twice.
- **First person:** two copies with the same yaw 225 and scale 2.0 as the single gun. The second ("left") one is shifted sideways by about **1.85** along the gun's own lateral axis.
- **Held (third person), ground and icon:** the same transforms as the single gun, plus small offsets for the second gun (on the ground about 0.2 up and 0.2 sideways; in the icon about 0.3 sideways, 0.4 up and 0.6 back) so two guns lie side by side and the icon shows both.
- **Firing:** the pitch is stronger, up to about 5 degrees (vs 3.5). The guns alternate; only the one that fires animates.
- **Muzzle flash:** drawn at that gun's own position, the first at about (-0.65, -1.25, 1.6), the second at about (-0.65, -1.25, -2.0).
- **Recoil (third person):** a 15-degree up-kick, not the single gun's 10, around a pivot about 1 block behind and 1 up.
- **Off-hand gun on the player's body:** drawn at scale 0.5 at about (0.2, -0.45, -2.15), with a 15-degree recoil tilt around a pivot 0.7 block back.

## 7. Motion

- The **model has no moving parts.** The magazine does not eject, the bolt does not cycle, the trigger does not move, the stock does not fold, and nothing changes with ammo count. All motion is whole-gun transforms.
- **Recoil:** a very short fire cycle (2 ticks between shots), so the jolt is small and fast: pitch up a few degrees and slide back about 0.15 block, then return. Third person is up to 10 degrees about the wrist (15 for akimbo).
- **Reload:** first person: the gun tips muzzle-up about 50 degrees, hangs at mid-reload, and returns. Third person: about 40 degrees about the wrist. The reload length is in the Mac-10 gun sheet.
- **Optional (not in the original):** the builder may keep the magazine, trigger and stock as separate named parts so later code can animate them, but the rest pose must match this document.

## 8. What to avoid

- **A box with a long barrel.** The barrel is a 3-unit stub; this is a bread box, not a carbine.
- **A solid stock.** It is open wire: thin posts and rods with a small butt plate. A solid block or long rectangular stock is wrong.
- **A grip in the middle of the receiver.** It sits under the rear third (X 6 to 9 of the 2 to 19 receiver).
- **A raked pistol grip.** It is vertical; only the thin backstrap plates add a slight curve.
- **Light, shiny paint all over.** The body is dark and cool (#565D5B); only the magazine, sights and stock wires are light.
- **A magazine as wide as the grip, or darker than it.** It is slightly narrower and clearly lighter.
- **Things the original lacks:** no suppressor, rail, scope, foregrip, charging handle or cut-out ejection port. Slots are paint only.
- **Large sight parts.** All of them rise less than 1 unit above the top.
- **A round barrel tube.** It is a diamond-section bar, and no longer than 3 visible units.

## 9. Acceptance checklist

- [ ] Length : height : thickness is within about 10 percent of 7.3 : 6.1 : 1 (22 : 18.4 : 3).
- [ ] Receiver is a plain box about 17 x 4 x 3 with a flat top and no cut-outs.
- [ ] Barrel is a 1.4-wide diamond-section stub; about 3 units show beyond the receiver's front face (about 14 percent of the length).
- [ ] Grip is a vertical box about 3 long x 7 tall x 2 wide, under the rear third of the receiver (its front face about 10 units behind the receiver's front).
- [ ] Magazine is pale, narrower than the grip (about 1.4 vs 2 wide), and drops about 6.4 below the grip's bottom; grip plus magazine hang about 13 below the receiver.
- [ ] The grip is darker than the receiver, and the magazine is much lighter than both.
- [ ] Wire stock is open: two thin vertical posts rising about 1 above the top line, thin rods along the receiver's top rear corners, and a cross bar bridging them.
- [ ] Butt plate is 3 wide and about 2 tall, hangs 1 below the receiver's underside, with a smaller connector block between it and the receiver.
- [ ] Trigger guard is a front post plus a floor bar with a 3 x 2 opening; the trigger is tipped about 23 degrees forward inside it.
- [ ] Top furniture is small: a diamond post (about 0.7 high), a thin front blade (about 0.4 high) and two tiny ear planes.
- [ ] Receiver carries painted horizontal dark slots on one flank and a short dark port on the other; the barrel's end face is a near-black diamond.
- [ ] Metal palette: receiver near #565D5B, grip near #505755, magazine near #9D9F9E, sights near #9D9F9E, stock wires near #747575.
- [ ] The grip's back panel carries a stippled or checkered dark texture.
- [ ] Side silhouette matches section 4: wide box, tall pale bar under the rear third, open frame at the back, tiny stub at the front.
- [ ] No part floats; every box overlaps or touches a neighbor (the wire stock ties to the receiver via the rods, cross bar and connector block).

## 10. Unsure

- **Flank placement** of the long slots and the short port (which side is which) comes from two oblique renders; the paint is small and low-contrast. The builder may swap sides.
- **Wire stock rod details.** The exact tilt (3 to 5 degrees) and the doubled run (rod plus stub) read as one thin line in renders. A single thin rod per side from X about 0.5 to 5.8, plus the cross bar, is an acceptable simplification.
- **Back plate** inside the receiver's rear is invisible, so it may be dropped.
- **Magazine floor plate** brightness is estimated from the bottom view; at most it is the brightest value in the texture (#A8AAA9).
- **First-person framing numbers** come from formulas read, not seen in game; treat the offsets as approximate. The akimbo lateral shift of 1.85 may be 1.85 or twice that in blocks, depending on whether it is applied before or after scaling.
- **Camo variants:** none. The gun uses one texture; no camo or color-variant textures exist for the Mac-10 or its akimbo form.

## Round 2 corrections (orchestrator review)

These facts come from comparing the first GregGuns build (B1 round 1) against the original, in the same axes as above (X 0 at the butt, Y 0 at the receiver top, Z 0 at the mid-plane). The first build matched the receiver (17 x 4 x 3), barrel stub, guard, wire stock and palette to within 0.1 unit. Silhouette overlap was 0.99 to 1.00 from the front, back, top and bottom, but only 0.87 from the side (0.94 to 0.96 in the two iso views). Most of the side loss comes from the first two items below; the rest is rim noise of 0.2 unit or less.

**Overall size to hit:** 22 long, about 18.6 tall (receiver top to magazine bottom is 17.4; the wire posts and sights add the rest), 3 wide.

### Backstrap plates (the largest side-view error)

Both plates are **hinged on their front edge, and that hinge sits at or inside the grip's back face**. They are thin shells wrapped on the back of the grip, not slabs stuck on behind it. The grip's back face is the plane X 6.
- **Upper plate:** 3 tall (Y 3.6 to 6.4), 0.8 thick (X 5.1 to 5.9 at its top), 1.8 wide. The hinge is its front-top edge, on the plane X 6. It leans so its lower end swings **forward** by about 0.65 (12.5 degrees). It therefore stands 0.9 proud of the grip at its top, tapering to about 0.25 proud at its bottom.
- **Lower plate:** 4.8 tall (Y 6.3 to 11.1), 0.8 thick, 1.8 wide. Its hinge is its front-top edge, **0.7 inside the grip** (the plane X 6.7), so the plate runs from X 5.8 to 6.6 at the top and only **0.2 of it shows behind the grip** there. It leans so its lower end swings **back** by about 0.62 (7.4 degrees). It stands 0.2 proud at its top, growing to about 0.85 proud at its bottom, which is its maximum.
- Round 1 put a full 1.0-thick slab entirely behind the grip with its hinge at mid-thickness, so the lower plate stood 1.0 proud at the top and 1.6 proud at the bottom. That is the dark wedge sticking out behind the grip in the render. Moving the hinges to the planes above, with the same two angles, lifted the full side silhouette from 0.88 to about 0.92 in a test render.
- Colors: the plates are a shade darker than the grip: base #454A49, stipple shadow #333434.

### Trigger

The trigger is 1 long, 2 tall, 1 wide, tipped 23.4 degrees so its lower end swings **forward** by about 0.8. Its hinge is the front-top edge at X 9.5, Y 3.5. The block runs X 8.5 to 9.5 at the top: **half of it is buried in the grip's front face (X 9) and half of it stands in front of the face**. At its tip (Y 5.5) it spans about X 9.3 to 10.3, leaning into the guard opening. The section above says the top is at X 9 to 9.5; the correct figure is X 8.5 to 9.5. Round 1 put the whole trigger in front of the face (X 9 to 10), a full 0.5 too far forward, so the render shows a pair of offset ghost edges around it.

### Magazine size (the only remaining front and back view loss)

The magazine is a 2 x 7 x 3 block shrunk by 0.3 on every face, so it is **1.4 wide, 6.4 tall and 2.4 long** (Y 11.0 to 17.4, X 6.3 to 8.7). Round 1 built 1.5 x 6.5 x 2.5. That extra 0.05 per face shows up as a thin strip down both sides from the front and back and cost about 0.07 of front-view overlap. If the pipeline cannot do sub-half-unit sizes, keep 1.5 wide and say so in the model's comment; 6.4 tall and 2.4 long are worth matching if a per-face inset is available.

### Paint (from the round 1 sheet)

- The flank with the long slot carries a **three-band slot**, not one black bar. The slot is about 10 long and 1.5 tall, set about 1.5 from the rear end of the flank and 0.5 below its top edge. From top to bottom: a dark line (about #242727, 0.5 tall), a mid-dark line (about #363A39, 0.5), then a dark line (about #272A2A, 0.5). Below it sits a lighter row of about #464B4A. Round 1 used one solid near-black block, which reads too stark.
- The panel base is the receiver base #565D5B (the panel varies from about #4F5654 to #575F5D) with a darker edge row of about #3D4140 along the bottom. The other flank carries one small dark port about 3 long and 1 tall, about #2D3030, in the front-middle.
- Round 1 had almost none of the #383838-family darks that make up about 12 percent of the original texture (slots, barrel ring, shadow edges), and its receiver averaged about 4 levels lighter. Add the darks above and the receiver mean should land near #565B59.
