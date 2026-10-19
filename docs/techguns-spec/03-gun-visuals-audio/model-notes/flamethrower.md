# Flamethrower: clean-room model notes

Design notes for rebuilding the Techguns **Flamethrower** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelFlamethrower](../gun-models.md#modelflamethrower-6464-family-x-36-boxes-1-part), [Flamethrower sheet](../../02-guns/sheets-energy-special.md#flamethrower-flamethrower), [rendering framework](../rendering-framework.md), [revolver notes (the template)](revolver.md).

**Units and axes used in this document.** One unit = one texel at the native 64x64 scale = 1/16 block. The muzzle points toward **+X**. **X = 0 is the rear face of the stock's butt plate.** **Y points down** (Minecraft model convention), with **Y = 0 at the top of the highest part**, the small dome on top of the rear body. **Z = 0 is the gun's mid-plane**; sizes across Z are given as total width, centered. Rounded to the nearest half unit unless it matters. (The original's own origin is different: it sits 18 units in front of the butt, 3.5 above the dome top, and its mid-plane is half a unit off its origin. The first-person offsets in section 6 were tuned to that origin; see section 10.)

## 1. Gestalt

A bulky, industrial **backpack-less flamethrower**: a long, light-gray pipe on top, a fat **white fuel tank with red stripes and a hazard roundel slung underneath it**, a **red wire stock** at the back, a **red open-loop pistol grip** under the middle, and a **small red nozzle with a black mouth** at the front, with a thin **pilot-light tube** hooked up under it. It reads as "a gas tank strapped to a pipe." The tank is the visual anchor: it is the only large white mass, and it carries the only bright colors on the gun (pure red stripes, a yellow and orange roundel).

Defining features in order of visual importance:

1. **The white tank** under the pipe: long (14), fat (6 tall, 6 wide with its side plates), cream-white, with thin pure-red vertical stripes near both ends and a black-ringed yellow roundel in the middle of each side.
2. **Red and gray color split.** Everything that is not the tank, the pipe or the receiver is a dusty **maroon red**: stock butt, grip loop, nozzle collar, nozzle prongs and the pilot tube. The pipe and body are **gray**, from light (barrel housing) to dark charcoal (rear body, front bulkhead).
3. **A long, flat-topped horizontal profile**: stock, receiver, pipe and nozzle all lie on nearly the same line, and the tank hangs beneath. About 7 times as long as it is wide.
4. **The nozzle:** a small black-mouthed square tip with four stubby prongs around it behind a larger red collar, fronted by a tall dark plate; and the **hooked pilot tube** below it.
5. **Open, skeletal parts:** the zigzag stock bars and the open-loop grip leave real see-through holes.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt plate to nozzle tip face) | 42 (43 including the pilot tube's tip) |
| Total height (dome top to grip bottom) | 12.5 |
| Total width (across the tank's side plates) | 6 |
| Length : height : width | about 7 : 2.1 : 1 |
| Stock (butt to receiver) | 9 long (x 0 to 9) |
| Receiver | 10 long (x 9 to 19), 5 tall, 3 wide |
| Rear body block (pump head) | 4 long (x 19 to 23), 4 tall, 5 wide |
| Barrel housing (pipe) | 13 long (x 23 to 36), 4 tall, 4 wide at its widest |
| Fuel tank (core, between its end caps) | 14 long (x 20 to 34), 6 tall, 4 wide; 6 wide with side plates |
| Nozzle group (bulkhead to tip) | 6 long (x 36 to 42) |
| Bore axis (center of the nozzle and the pipe) | y = 3.0, z = 0 |
| Tank axis | y = 8.5, which is 5.5 below the bore axis |

How the pieces sit along the length, from the rear: the butt plate (x 0 to 2); three thin stock bars reaching to the receiver (x 2 to 9); the receiver (x 9 to 19) with the pistol grip hanging under its rear (x 10 to 15); the rear body block and the valve cluster (x 16 to 23); then the 13-long pipe above the 14-long tank (both end near x 34 to 36); a dark vertical plate at x 36 to 37; the red collar (x 37 to 39) and the nozzle tip with prongs (x 39 to 42).

The **top line** steps up and down a little but is nearly flat: stock top bar at y 3, butt top at y 2.5, receiver top at 1.5, spine on the receiver at 0.5, pipe and rear body top at 1.0, dome top at 0 (the highest point), nozzle top at 2.0. Everything above the grip and tank is within 3 units of the top. The **bottom line** is far lower and uneven: the grip bottom at 12.5, the tank bottom at 11.5, the butt's lower extension at 10.5.

## 3. Part by part

All parts are boxes except the two pilot-tube segments, which are tilted. Z widths are centered on the mid-plane. "Touches" means faces in contact or overlapping.

### Receiver
- A box **10 long x 5 tall x 3 wide** (x 9 to 19, y 1.5 to 6.5). The rear half of the gun's body, directly in front of the stock.
- **Top spine:** a flat bar on top, **10 long x 1 tall x 2 wide** (y 0.5 to 1.5), the full length of the receiver, narrower than it by half a unit each side.
- **Side pipes:** two round-looking rods drawn as **9 long x 1 x 1** boxes, one on each flank, outside the receiver (z from 1.5 to 2.5 each side, y 4 to 5), running from x 10 to x 19. They sit 2.5 below the receiver's top and are flush against its sides, so the receiver plus pipes is 5 wide at that height.
- Touches: the stock bars at its rear face (middle bars at x 9); the pump head and the tank bulkhead at its front face; the grip loop under its rear; the valve cluster under its front.

### Pump head (rear body block) and dome
- A block **4 long x 4 tall x 5 wide** (x 19 to 23, y 1 to 5). It is the widest thing above the tank besides the side pipes, and 2 units wider than the receiver.
- **Dome / gauge cap:** a box **4 long x 1 tall x 3 wide** (x 19 to 23, y 0 to 1) sitting on top of the pump head. It is the highest point of the gun, 0.5 higher than the receiver's spine.
- Touches: the receiver's front face behind it, the barrel housing's rear face in front of it.

### Barrel housing (the pipe)
- Built to read as a pipe with a "+" cross-section: a **core 13 long x 4 tall x 2 wide** (x 23 to 36, y 1 to 5), plus **two side rails, each 13 long x 2 tall x 1 wide**, one on each side of the core at mid-height (y 2 to 4). Overall at the rails it is **4 wide, 2 tall**; at the core it is **2 wide, 4 tall**.
- It is the largest light-gray surface on the gun. Its axis is y = 3.0, the same as the nozzle's.
- Its bottom (y 5) is **0.5 above** the tank core's top (y 5.5): a hairline slit visible side-on from x 20 to 36 (section 4). Behind the tank the slit widens: from x 19 to 20 there is a 1.5-tall opening between the pump head and the tank's rear cap.
- Touches: the pump head at the back, the tall bulkhead plate at the front (x 36). Not attached to the tank except through the bulkhead and the pump head; the thin gap is intentional.

### Fuel tank
The tank is the white mass hanging under the pipe.
- **Core:** a box **14 long x 6 tall x 4 wide** (x 20 to 34, y 5.5 to 11.5).
- **Side plates:** one on each side, each **14 long x 4 tall x 1 wide**, flush with the core's length (x 20 to 34), covering y 6.5 to 10.5 on the core's flanks (z from 2 to 3 on each side). The tank is therefore 6 wide overall at its middle band, and the core (4 wide) shows as 1-unit strips above and below the plates (y 5.5 to 6.5 and y 10.5 to 11.5).
- **Rear end cap:** a plate **1 long x 4 tall x 4 wide** (x 19 to 20, y 6.5 to 10.5) at the tank's rear.
- **Front end caps, stepped:** first **1 long x 4 tall x 4 wide** (x 34 to 35, y 6.5 to 10.5), then **1 long x 3 tall x 4 wide** (x 35 to 36, y 7 to 10). The front of the tank steps in like a rounded bullet nose, 1 unit per step, top and bottom.
- Touches: the receiver's lower front corner and the valve cluster at its rear cap; the front bulkhead (x 36) at its nose cap, along y 7 to 8 only. It does not touch the pipe.
- No filler cap, no straps, no hoses, no gauge on the tank: its decoration is entirely painted (section 5).

### Valve cluster
- A stepped lump of three small blocks under the receiver's front end, between the grip loop and the tank's rear cap: a **2 long x 1 tall x 2 wide** plate (x 16 to 18, y 6.5 to 7.5) directly under the receiver; a **1 x 1 x 2** block hanging under its rear half (x 16 to 17, y 7.5 to 8.5); and a **2 x 2 x 2** block (x 17 to 19, y 7.5 to 9.5) in front of that, touching the tank's rear cap.
- All 2 wide. Together they read as a small brass valve body or regulator, a stepped, tan blob about 3 long and 3 tall. It is lower than the receiver by 3 units.
- There is a **1-unit gap** between the valve cluster and the grip loop's front leg (x 15 to 16).

### Nozzle group
From back to front:
- **Bulkhead plate:** a tall plate **1 long x 7 tall x 4 wide** (x 36 to 37, y 1 to 8), at the end of the pipe. It is dark charcoal, and wider and taller than anything near it except the tank. It spans from above the pipe's top (y 1) to below the tank's nose caps' top, so it bridges pipe and tank.
- **Collar:** a block **2 long x 3 tall x 3 wide** (x 37 to 39, y 1.5 to 4.5), centered on the bore axis. Maroon red.
- **Tip:** a block **3 long x 2 tall x 2 wide** (x 39 to 42, y 2 to 4). The front face (the muzzle) is **black**, the 2x2 mouth. Dusty mauve-gray sides.
- **Prongs:** four small blocks **2 long x 1 x 1**, one on each of the tip's four sides, all at x 39 to 41 (flush with the tip's rear, ending 1 short of the front), each protruding **0.5 beyond** the tip's face: top (y 1.5 to 2.5), bottom (y 3.5 to 4.5), left and right (z 0.5 to 1.5 each side, y 2.5 to 3.5). They are cross-shaped fins, red, and they leave the last 1 unit of the tip bare.
- **Fuel stub:** a block **1 long x 2 tall x 2 wide** (x 37 to 38, y 4.5 to 6.5) hanging below the collar and against the bulkhead's front face. Red. It feeds the pilot tube.

### Pilot-light tube
A thin 1x1 pipe that leaves the fuel stub and hooks up under the nozzle's mouth. It is two segments:
1. **Lower run:** a 4-long, 1x1 rod starting at the fuel stub's front (x 38, y 5.0 to 6.0) and angled **21 degrees downward** as it goes forward, ending at about x 41.7, y 6.5 to 7.2.
2. **Upturned hook:** a 3-long, 1x1 rod that starts at the end of the lower run (bottom end near x 41 to 41.5, y 7.2) and rises forward, **29 degrees from vertical, leaning toward the muzzle**. Its tip ends near x 42 to 43, y 4.3 to 4.8, which is just **under and slightly ahead of the nozzle's mouth** (the tip's bottom face is y 4, so the hook's end is 0.3 to 0.8 below it and up to 1 in front).
- The angle between the two segments at the elbow is about 75 degrees (a sharp upturn). Both segments are the same darker maroon (section 5).
- Purpose: it holds the pilot flame in front of the nozzle. It must read as a small bent tube, not a fin.

### Pistol grip (open loop)
- Two vertical plates and a foot form an **open rectangular loop** hanging from the receiver's rear underside:
  - **Rear leg:** **2 long x 6 tall x 2 wide** (x 10 to 12, y 6.5 to 12.5), the full height of the grip.
  - **Front leg:** **1 long x 5 tall x 2 wide** (x 14 to 15, y 6.5 to 11.5).
  - **Foot:** **2 long x 1 tall x 2 wide** (x 12 to 14, y 11.5 to 12.5), connecting the two legs at the bottom.
- The loop's opening is **2 long x 5 tall** (x 12 to 14, y 6.5 to 11.5), closed above by the receiver's bottom. It is a stirrup or D-handle, not a solid grip. Everything is 2 wide.
- All maroon red.

### Stock
- **Butt plate:** a plate **2 long x 5 tall x 3 wide** (x 0 to 2, y 2.5 to 7.5), with a **lower extension 2 long x 3 tall x 2 wide** (x 0 to 2, y 7.5 to 10.5) hanging from its bottom. Together a rear block 8 tall, stepping in 0.5 per side below. Maroon red.
- **Three bars**, all **1 tall x 2 wide**, forming a zigzag skeleton between the butt and the receiver:
  - Top bar: **7 long** (x 2 to 9, y 3 to 4), on the receiver's upper-middle level.
  - Middle bar: **4 long** (x 5 to 9, y 5 to 6).
  - Lower bar: **4 long** (x 2 to 6, y 6 to 7), touching the middle bar's rear end at x 5 to 6.
- Together they leave **windows**: a 1-tall slot between the top and middle bars (x 5 to 9, y 4 to 5) and a 2-tall pocket (x 2 to 5, y 4 to 6, partly bounded by the lower bar's top at y 6). Dusty mauve-gray.
- The stock is **narrower than the receiver** (2 wide vs 3).

## 4. Silhouette checks

- **Side (the main view, muzzle right):** from the left: a tall maroon butt block with a lower tab; three skinny bars crossing like a Z toward the receiver, with see-through slots between them; the gray receiver with its low dark spine and a dome bump forward of it; the long pipe running right with the white tank under it; the dark tall plate and red nozzle at the front with a black mouth; the hooked pilot tube curling up under the nozzle. Beneath the receiver the **maroon grip loop hangs with a clear rectangular hole** (2 wide by 5 tall), then a 1-unit gap, then a tan stepped valve, then the white tank. **Negative space:** the 0.5-tall slit between pipe and tank; the stock windows; the grip hole; the 1-unit gap before the valve; the space below the receiver between the grip and the tank; the area under the nozzle and the tank's rounded nose.
- **Top:** a long, narrow spine. From the back: a 3-wide butt, a 2-wide bar run, the 3-wide receiver with 1-wide pipes poking out at each side (5 across), a 5-wide pump head (with a 3-wide dome), then the 4-wide pipe, with the white tank peeking out **1 unit on each side** along x 20 to 34 (6 across), then the 4-wide dark plate, the 3-wide collar and the 2-wide tip with tiny prong bumps. The pilot hook shows as a thin stub in front of the tip.
- **Front:** a tall, narrow arrangement: the black 2x2 mouth surrounded by a 3x3 red collar and the cross-shaped prongs (each sticking out 0.5), all in front of a dark 4-wide plate that is 7 tall; below it, the **white tank** shows as a 6-wide band with its stepped nose; the pilot tube's tip as a red dot under the mouth. Behind these, the grip and stock peek out as thin 2-wide columns below and behind.
- **Back:** a 3-wide, 8-tall maroon butt (with its narrower lower tab) in front of the 5-wide, gray pump head and the 6-wide, white tank below it and slightly behind. The grip loop shows through the stock's windows.
- **Underside:** the white tank's 6-wide, 14-long underside dominates the middle, with its red stripe bands across it; the grip's foot and valve at the rear of it; the pilot tube at the front.

## 5. Materials and palette

The original texture mixes light industrial metal, dark rear-body metal, a white-and-red painted tank, and dusty maroon for the "hand" parts. Values were sampled from its regions.

| Part | Material | Base | Highlight | Shadow / detail |
|---|---|---|---|---|
| Tank core, side plates (broad faces) | white-cream paint | #F7F7F7 (cream shade #EEEDE3, up to #F4F3E9 on tops) | #FBFBFB | #D1D1D1 to #DDDDDD near the stripes, #E2E2E2 elsewhere |
| Tank end caps (rear and front) | cream | #F2F1E7 | #FEFEFE | #ECEBE1 |
| Tank stripes | pure red | #FF0000 | | (no shading) |
| Tank roundel ring | black | #040404 and #151515 | | |
| Tank roundel center | yellow and orange | #FFF704 | | #FFB504 (checker with yellow) |
| Receiver (box) | light gray metal | #9E9E9E | #C4C4C4 on its rear edge, #BABABA on top | #8B8B8B body, #7B7B7B shadow |
| Receiver rivets | dark gray | #383838 | | |
| Receiver spine (top bar) | dark charcoal | #4A4A4A on sides | #4C4C4C | #353535 on top, #323232 |
| Side pipes | mid dark gray | #606060 | #676767 | #4E4E4E to #505050 |
| Pump head (rear body block) | dark gray | #5A5A5A | #656565 | #454545 to #484848 |
| Dome / gauge cap | dark gray | #5C5C5C | #656565 | #4A4A4A to #4C4C4C |
| Barrel housing core | light gray metal | #A3A3A3 | #B8B8B8 | #727272 to #7A7A7A (seams) |
| Barrel housing side rails | gray metal | #959595 to #A5A5A5 | #B3B3B3 | #737373 |
| Front bulkhead plate | dark gray | #606060 | #656565 | #464646 to #4B4B4B |
| Tank front step caps | dark gray | #636363 | #656565 | #474747 |
| Nozzle collar | maroon red | #A24B4B | #B16767 | #993D3D |
| Nozzle prongs | maroon red | #9C3F3F | #A34E4E | #8B3434 |
| Fuel stub | maroon red | #9F4343 | #A85555 | #963C3C |
| Nozzle tip (sides) | dusty mauve | #8F7373 | #A28B8B | #866969 |
| Nozzle tip (front face, the mouth) | black | #000000 | | |
| Pilot tube (both segments) | dark maroon | #943838 | #A24949 | #8E3636 |
| Grip loop (legs and foot) | maroon red | #A24B4B | #A85454 | #913838 |
| Stock butt plate | lighter maroon | #AA5A5A | #B46C6C | #993D3D (back face) |
| Stock butt lower extension | maroon | #9F4545 | #A85757 | #943A3A |
| Stock bars | dusty rose-gray | #8F7373 | #977E7E | #866969 |
| Valve cluster | tan, brass-like | #BBB99B | #D1CEAC | #ABA98E |

Shading style:
- **Tank:** flat white with a few levels of cream noise; **no gradient**. The red stripes are single-unit-wide, full-bright, with no shading. They sit about 1 unit in from each end of each side plate (at x 21 and x 32, running the full 4-unit height of the plate), and cross the top and bottom of the tank at the same two positions. The roundel (4 x 4) is centered on each side at x 27, y 8.5: black ring, with a 2 x 2 yellow-and-orange checker center. Same on both sides.
- **Pipe:** light, **satin** gray with a few levels of noise, and **two thin vertical darker seams** on each side (about 1.5 in from each end of the housing). Lighter streaks along the top.
- **Receiver:** brushed gray with a lighter rear edge and **two dark rivet dots on each side**, 6 apart (around x 11.5 and x 17.5), a third of the way down from its top (y of about 3). The top spine is dark and faintly ribbed.
- **Dark parts** (pump head, dome, bulkhead, side pipes, tank nose caps): evenly dark gray with minor noise, 1 or 2 levels darker on the shadow side. They make the pipe's rear and front read as heavy.
- **Maroon parts** are mottled: each face varies by plus or minus 8 levels, with no sharp pattern. Grip and stock-butt faces are a bit lighter on their outer sides.
- **Overall value structure:** the tank and the pipe are the two lightest masses; the pump head, bulkhead and side pipes are the darkest; the maroon sits in the middle. Do not make the pipe dark and do not make the tank cream-yellow.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up). There is **no scope and no zoom**.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person (idle) | yaw -45 degrees, scale 2.0 | shift about (0.55, -0.30, -0.70) in model-origin terms. The gun is seen from behind and to the side, turned 45 degrees, so you see the stock end, the receiver's flank and the tank's side. |
| First person firing | no change from idle | **There is no recoil animation**: firing applies only the idle offset. The gun does not kick. |
| First person reloading | roll about Z up to about 20 degrees, yaw -10 degrees, plus a shift of up to (0.5, 0.25, 0.25) | smooth rise to the middle of the reload and back. The motion is a sine arc with a flat top (the peak is clamped at about 2/3 of full strength). |
| Third person (held) | yaw 225 degrees, scale 1.5 | offset (-0.8, -0.6, 0). The gun is large: about 4 blocks long at this scale. |
| Third person aimed | extra offset (0, -0.15, -0.4) | |
| Third person reloading | up to -30 degrees about X and -40 degrees about Y at the middle of the animation, then back | triangular rise and fall. |
| Third person recoil | none | no recoil transform. |
| Ground (dropped) | yaw 90 degrees, scale 0.85 | lowered by 0.45 so it lies on the ground. |
| Inventory icon | yaw 180 degrees, scale 0.7 | offset (-0.3, 0, 0). There is no sprite; the icon is the 3D model, drawn side-on. |

**Muzzle flash:** none. The sprite call is disabled. Fire is expressed by the flame particles that fly out of the barrel, not by a flash on the model. The flame emerges from the nozzle's mouth: at x = 42, y = 3, z = 0.

**Pilot flame:** while the gun is idle (not reloading, not recoiling) the first-person view draws a small **looping fire sprite** (a 4x4 animated sheet of 16 frames, additive, a 30-tick loop) near the nozzle, as if from the pilot tube. In the original this is gated by a liquid check that looks wrong (see section 10).

**Akimbo:** not supported. Only one gun at a time.

## 7. Motion

- The **model has no moving parts.** Nothing on the gun rotates or slides, and the tank does not drain or change.
- **Firing:** full-auto, a shot every 2 ticks. Fire sound is a loop with a start sound. The 5-tick recoil timer exists but has **no visible whole-gun movement** (see section 6).
- **Reload:** 45 ticks (2.25 s). First person: the gun tilts and slides up and aside (arc described above), hangs, then drops back. Third person: tilts about 30 and 40 degrees, then returns.
- **Optional (not in the original):** the builder may keep the nozzle prongs, the tank and the pilot tube as separate named parts so later code can animate them, but the rest pose must match this document.

## 8. What to avoid

No previous GregGuns attempts exist for this gun. These are the traps predicted from the other models and from the geometry:

- **A cylindrical tank.** The original tank is a rectangular box with stepped ends. Do not round it, and do not drop the side plates (the tank must be 6 wide, wider than the pipe above it).
- **A gray or cream-yellow tank.** It is white with pure-red stripes and a black-yellow-orange roundel. Do not make it dark or add straps.
- **A solid grip.** The grip is an open loop with a 2-by-5 hole; do not fill it.
- **A solid stock.** Keep the zigzag of three thin bars with gaps between them.
- **Welding the pipe to the tank.** Keep the 0.5-unit slit between them along the 14-long overlap.
- **A round pipe.** The pipe is a "+" of boxes: a tall 2-wide core and two 2-tall side rails.
- **Dropping the dome** (the highest point) or the 1-unit-wide side pipes on the receiver.
- **A long, thin barrel.** The nozzle is short (6 long including the bulkhead) and the nozzle tip is only 3 long.
- **Forgetting the black mouth**, the four prongs, or the hooked pilot tube. The tube must be a bent, thin, upturned hook, not a straight fin.
- **Red everywhere.** Red is only the stock butt, grip, collar, prongs, stub and pilot tube. The stock bars are a muted rose-gray, and the valve is tan.
- **Adding a muzzle flash sprite, a scope, a rear sight, a rail or a trigger.** The original has none of these, and no trigger piece.
- **Adding recoil animation** to match other guns. The original has none.

## 9. Acceptance checklist

- [ ] Length : height : width is within about 10 percent of 7 : 2.1 : 1 (42 : 12.5 : 6).
- [ ] The tank is a box about 14 long, 6 tall and 4 wide, with side plates making it 6 wide, a stepped front nose and a flat rear cap.
- [ ] The tank is white-cream, with pure-red (#FF0000) stripes about 1 unit in from each end on both sides, and a black-ringed yellow-and-orange roundel at the center of each side.
- [ ] The pipe above the tank is a "+" cross-section (4 wide at the rails, 4 tall at the core), 13 long, light gray, with a visible 0.5-unit slit between it and the tank.
- [ ] A 5-wide, 4-tall dark block with a 3-wide dome on top sits between the receiver and the pipe; the dome is the highest point on the gun.
- [ ] The receiver is 10 long, 5 tall and 3 wide, with a dark spine on top, two 1-wide side pipes, and four dark rivet dots (two per side).
- [ ] The grip is a maroon open loop: two legs and a foot, with a 2-by-5 see-through hole.
- [ ] The stock has a maroon butt block and three thin bars in a zigzag, with see-through windows between them.
- [ ] The nozzle group is, in order: a tall dark plate, a 2x3x3 maroon collar, a 3-long tip with a black 2x2 mouth, and four 2-long prongs that stick out about 0.5 and stop 1 short of the mouth.
- [ ] The pilot tube is two thin segments: one angled 21 degrees down and forward, one hooking up about 29 degrees from vertical, ending just under and slightly ahead of the nozzle's mouth.
- [ ] A small tan stepped valve block hangs under the receiver's front, with a 1-unit gap in front of the grip.
- [ ] Colors: pipe and receiver light gray (#9E9E9E to #A3A3A3), dark parts around #5A5A5A, maroon around #A24B4B, stock bars #8F7373, valve #BBB99B, tank #F7F7F7.
- [ ] The model has no muzzle flash, no moving parts, no scope and no trigger.
- [ ] The muzzle (flame origin) is at the center of the nozzle's front face (x 42, y 3, z 0).
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Valve cluster function.** It is a stepped tan block under the receiver; I read it as a valve or regulator, but it could be meant as the trigger group. There is no separate trigger in the original.
- **Which side faces the camera.** Every side feature (roundel, rivets, stripes) is the same on both sides, so this does not matter.
- **Pilot tube end position.** It comes from rotated boxes; the numbers (tip about 0.3 to 0.8 below and up to 1 in front of the mouth) could be off by about 0.3.
- **Pilot flame gate.** The pilot flame is drawn only when a liquid-position check passes, which looks like an inverted condition. It may in practice appear only in water. The builder or orchestrator can choose to show it whenever the gun is idle.
- **First-person framing numbers** come from formulas read, not seen in game, and depend on the original's origin (18 units in front of the butt, 3.5 above the dome top, half a unit off the mid-plane). If the builder moves the origin, the offsets in section 6 must be adjusted by the same shift. Treat them as approximate.
- **Camo variants:** there is **one** texture and no variants.
- **Stripe positions on the tank's top and bottom** come from a low-resolution texture reading; the side-plate stripes are certain.
