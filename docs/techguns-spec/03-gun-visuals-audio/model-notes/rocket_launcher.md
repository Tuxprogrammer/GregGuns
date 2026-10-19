# Rocket Launcher: clean-room model notes

Design notes for rebuilding the Techguns **Rocket Launcher** look-alike in GregGuns, plus a short section on the **rocket projectile** model. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelRocketLauncher](../gun-models.md#modelrocketlauncher-rocket-and-nuke-launcher-128x64-family-x-29-boxes-1-part), [ModelRocket](../gun-models.md#modelrocket-rocket-item--projectile-128x64-11-boxes), [rendering framework](../rendering-framework.md), [Techguns rocket launcher stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = one texel at the native 128x64 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention). **Y = 0 is the axis of the launch tube** (so the top of the tube is about -3 and the bottom about +3). **Z = 0 is the tube's axis too**; widths across Z are totals, centered. **X = 0 is the back face of the rear cap**; the muzzle ring ends at X = 45. Rounded to the nearest half unit unless it matters. The original authors the tube axis half a unit off a round number; that offset is removed here.

## 1. Gestalt

A **shoulder-fired tube**: a very long, plain, blue-gray steel pipe with a squat black collar in the middle that carries a short pistol grip underneath and a tiny flip-up sight on top, a black flared cap at the back and a black muzzle ring at the front. A **small red stepped nose** pokes out of the muzzle when the launcher is loaded. At a glance you read: "a bazooka": one long horizontal cylinder, almost no detail, one short grip hanging from its middle. It is by far the longest and thinnest of the guns, and it has the least visual clutter.

Defining features in order of visual importance:

1. **A tube 36 long and 6 across**, light blue-gray metal, an octagonal pipe, with no breaks. It is more than three-quarters of the total length.
2. **Two black end pieces**: a short flared cap at the rear and a slightly shorter, plus-shaped muzzle ring at the front. They make the tube look capped.
3. **A black central collar** ("housing") with side plates, a small top bridge, and a flip-up sight loop.
4. **A short, slim pistol grip** that sits under the collar and leans back about 17 degrees, with a tiny trigger stub in front of it.
5. **The loaded rocket's red nose**, a three-step red cone sticking out of the muzzle (only when loaded).

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length, empty (rear cap to muzzle ring) | 45 |
| Total length, loaded (to the tip of the red nose) | 48 |
| Total height (sight loop top to grip bottom) | about 16.5 |
| Tube and cap height | tube 6, cap 8, muzzle ring 8 |
| Total thickness | 8 (cap and muzzle ring), 7 across the collar's side plates |
| Length : height : thickness | about 5.6 : 2.1 : 1 |
| Tube length | 36, **80 percent** of the empty length |
| Collar | 6 long, 7 tall, 5 wide (7 with its side plates) |
| Grip | 3 wide fore-aft, 7 long, 2 across; reaches about 6.5 below the collar's underside |

How the pieces sit along the length, from the rear: the cap fills X 0 to 3; the tube runs X 3 to 39; the muzzle ring X 39 to 45; the loaded rocket's nose reaches X 48. The collar is centered at X 28, about 62 percent of the way forward, so it sits in the **front-middle**, not at the balance point of the tube. The grip hangs from its front half. The sight loop sits at the collar's front edge (X 29 to 30).

The **top line** is the tube's top (Y -3), with the collar rising about 0.5 above it and the sight loop rising to about -6.5 at X 29.5. Nothing else rises above the tube.

## 3. Part by part

All parts are boxes except the grip (rotated). Z widths are centered on the axis.

### Tube
- Octagonal cross-section built from three overlapping slabs: a 5 by 5 core and two cross slabs, one 6 tall by 3 wide and one 3 tall by 6 wide. The visible section is a **6 by 6 octagon** with flat sides 3 wide and corners cut by 1.5 units.
- Runs X 3 to 39 (36 long). No taper, no bands, no rings, no rail. One continuous pipe.
- Touches: the rear cap at the back, the muzzle ring at the front; the collar clamps over X 25 to 31.

### Rear cap
- 3 long (X 0 to 3). A 6 by 6 core plus four 4-wide strips, one per face, each 1 thick and centered on that face (4 wide, 1 out). Seen end on, an **8 by 8 octagon-like plus shape**: a 6-wide square with a 4-wide bump on every side.
- It is wider than the tube by 1 unit on each side, so it reads as a flare. Black.
- A **flat, solid back face**: the tube is not open at the rear in the original model (no visible hole, no blast shield cutout).

### Muzzle ring
- 6 long (X 39 to 45). A 6 by 6 square block plus four **3-wide, 1-thick bumps** on the middle of each face: an **8 by 8 plus shape** end-on, with the square corners intact (not cut). Black, slightly shorter bumps than the rear cap's strips (3 versus 4 wide).
- It is the same width as the cap but twice as long, so the front reads heavier than the back.
- Nothing is cut out of its front face; it is solid (the loaded rocket and its nose protrude out of the flat face). When empty, the front face is a flat black plate.

### Collar (housing)
- A block **6 long, 7 tall, 5 wide** (X 25 to 31, Y -3.5 to 3.5). It wraps the tube; because it is only 5 wide while the tube's cross slabs are 6 wide, the tube's side flats pass through it.
- **Side plates:** two thin plates, each **6 long, 5 tall, 1 thick**, on the left and right faces (Y -2.5 to 2.5, Z at 2.5 to 3.5 each side). They stand 0.5 unit proud of the tube flats and make the collar look wider (7 total) than the tube is across its flats.
- **Top bridge:** a **3 long, 1 tall, 3 wide** block on the collar's top (X 26 to 29, Y -4 to -3), centered in Z, so the collar's top steps up in the middle by 0.5 over the housing and 1 over the tube.
- Black gunmetal throughout.

### Flip-up sight
- Built from three **zero-thickness planes**: two vertical vanes (each 1 long, 3 tall, at Z = -1 and Z = +1, X 29 to 30, Y -6.5 to -3.5) and one flat plane across their tops (1 long, 2 wide, at Y -6.5). Together a tiny open loop 2 wide, 1 long and 3 tall.
- Because the planes are paper thin, the sight is visible as two thin dark rectangles from the side and as a thin bar from the top; **seen straight from the front or back it nearly vanishes**. If the builder gives it thickness, keep it 0.25 or less.
- It stands at the collar's front edge, just ahead of the top bridge.

### Grip
- One slab **3 long (fore-aft) by 7 tall by 2 wide**, hung from the **underside** of the collar at its front half (pivot at X 29, Y 3.5), then **tilted about 17 degrees** so its lower end swings back toward the rear. After tilting, the lower end sits at about X 24 to 27 and Y 9.5 to 10, and the upper rear corner sits slightly inside the collar. It is a plain straight slab; no finger grooves, no palm swell, no guard.
- Black gunmetal, a few levels lighter than the collar.

### Trigger stub
- A **2 long, 1 tall, 1 wide** block at X 28 to 30, Y 3.5 to 4.5, centered in Z, directly under the collar and just in front of the grip's top. A tiny bump, not a shaped trigger. No trigger guard.

### Loaded rocket (visible only when loaded)
The launcher model contains a copy of the rocket (section 11). Inside the muzzle ring it is hidden, so only its **front three steps** show past the muzzle face (X 45):
- X 45 to 46: a **4 by 4** red collar (the end of the warhead).
- X 46 to 47: a **3 by 3** step (the end of the body), light gray with red.
- X 47 to 48: a **2 by 2** red nose tip.
Together a tiny stepped red cone, 3 long, centered on the axis. Pieces behind it sit entirely inside the muzzle ring and are not seen.

## 4. Silhouette checks

- **Side (the main view):** a very long, thin horizontal bar. From the rear: a black cap taller than the tube (8 vs 6); then the long gray octagonal tube; in the front-middle a black collar rising slightly above and below the tube, with a small sight loop poking up on its front edge; one short grip hanging down and back from the collar, ending about 6.5 below the tube's bottom; a trigger stub just ahead of the grip top; then the black muzzle ring, as tall as the cap; then a short red stepped nose, if loaded. Negative space: the whole under-tube space behind and ahead of the grip is empty; the grip's rearward lean leaves a long triangular notch to its rear.
- **Top:** a very thin strip. Width 8 at the cap, narrowing to 6 along the tube (the flats show lighter), a slightly wider (7) collar with the small bridge in its middle, then the 8-wide muzzle ring. The sight shows as a thin bar at the front of the collar. The red nose is a small pointed block beyond the ring.
- **Front (looking at the muzzle):** a black plus-shaped square about 8 by 8 on a gray octagon; with a rocket loaded, a small red stepped square sits in its center (a 4 by 4 red collar, then 3 by 3, then 2 by 2). The grip is a narrow black column hanging about 6.5 below, 2 wide. The tube and the collar are hidden behind the ring.
- **Back:** the same plus-shaped black disc (cap) with nothing in the middle; the grip's rear face hanging below it; the sight loop is edge-on and nearly invisible.
- **Underside:** a long strip, 6 wide along the tube with the cap and ring wider at the ends, the collar's lower face, and the grip's top, 2 wide, in the middle.

## 5. Materials and palette

The original texture is a cool blue-gray tube against near-black hardware. Values were sampled from its regions.

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Tube | brushed blue-gray steel | #5A6467 | #79888E, up to #AABCC2 on the top ridge | #32373A, down to #101213 on edge lines |
| Rear cap | black gunmetal | #222323 | #343737 | #1F2020 |
| Muzzle ring | black gunmetal | #202121 | #2A2D2D | #1E1F1F |
| Collar housing | black gunmetal | #2D2F2F | #383B3B | #1F2020 |
| Collar side plates | black gunmetal | #292B2B | #383B3B | #232424 |
| Grip | black gunmetal | #2B2D2E | #2E3030 | #212222 |
| Trigger stub, sight loop | black gunmetal | #2D2F2F | #2F3131 | #202121 |
| Rocket nose, collar, bands | signal red | #FE1C1C | #FE3737 | #FD1414 |
| Rocket body (the visible 3 by 3 step) | white to pale gray | #F0F0F0 | #F9F9F9 | #BEBEBE |

Shading style:
- **Tube:** horizontal streaking along its length: lighter streaks on the upper ridge and flats facing up, darker streaks beneath, with a thin near-black line at the edge between two flats. Subtle noise, a few levels of blue-gray. **No** dents, no scratches, no paint.
- **Black parts:** flat, very dark, with a few levels of per-pixel noise. They separate from the tube by value (very dark against a mid-light tube), not hue. No highlights stronger than #383B3B. There is no wear and no label.
- **Rocket pieces:** crisp white and red, with a clear light-to-mid gray step. They are the only saturated color on the gun.
- The nuke launcher (same model) swaps the tube to a dark brown-black (#3A3028-ish), with yellow (#FFF600) rocket accents. That is a separate retexture, not a camo.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 2.25 | idle offset about (0.15, -0.25, -0.5). The camera sees the tube nearly side-on, turned 45 degrees so the muzzle points away to the left of the screen and the cap and grip come toward you. Scale 2.25 makes the 45-unit tube about 6.3 blocks long on screen, much of it off-screen. |
| First person firing | roll about Z of up to -12.5 degrees at mid-animation | plus a shift of about 0.4 back along the barrel. Over the fire animation it dips and returns; there is no large flip. |
| First person reloading | roll about Z up to 40 degrees, yaw -20 degrees | plus a shift of about (0.5, 0.15, 0.15) at the peak, rising and falling smoothly over the reload. |
| First person aimed | extra offset about (0, -1.8, 0.2) on top of the generic aimed pose | no scope and no zoom overlay. The flip-up sight is purely cosmetic. |
| Third person (held) | yaw 225 degrees, scale 1.5 | offset (-1.2, -1.8, 0). The tube rests on the shoulder with the grip in the hand, the muzzle forward. |
| Ground (dropped) | yaw 90 degrees, scale 1.0 | lowered by 0.25. |
| Inventory icon | yaw 180 degrees, scale 0.68 | offset (-0.3, 0.3, 0); drawn side-on, tube spanning the slot's width. There is no sprite; the icon is the 3D model. |

**Muzzle flash:** none is drawn for this gun (the flash call is disabled in the original). Fire effects come from the projectile, not the launcher. The loaded rocket's nose is the only visual that changes on firing.

**Loaded versus empty:** the rocket parts draw only when the gun has ammo **and** it is not in the first half of a reload. An empty launcher shows no red nose at all; a fresh reload shows the nose appearing at the reload's midpoint.

**Akimbo:** none; the launcher is single-handed only.

## 7. Motion

- The **model has no moving parts.** The sight does not flip, the trigger does not move, and the rocket does not slide. All motion is whole-gun transforms, plus the rocket nose switching on and off.
- **Recoil (fire animation):** in first person a slight shove back along the barrel and a small downward roll (see section 6); in third person the launcher rolls about the shoulder: pivot about 0.3 up and 0.9 back from the model origin, rolling to about -20 degrees in roughly the first eighth of the animation, then easing back to zero over the rest of its first half; the second half is still.
- **Reload:** 40 ticks (2 s). First person: the gun rolls up and aside, hangs near mid-reload, then drops back, and the nose reappears at the halfway point. Third person: pitches about -20 degrees and yaws about -30 degrees at mid-reload, then returns.
- **Optional (not in the original):** the builder may keep the rocket and the sight as separate named parts so later code can animate them, but the rest pose must match this document.

## 8. What to avoid

- **A thick or shortened tube.** Keep it 36 long and 6 across; the gun's character is its length. Do not make a chunky "launcher barrel" 8 or 10 wide.
- **A detailed, open-mouth tube with a visible rocket sitting deep inside.** The front is a flat plate and only a three-step red cone pokes out.
- **A round muzzle or flared bell.** Both ends are plus-shaped squares (8 by 8 end-on), not smooth cones.
- **Lots of gadgets**: no scope, no rails, no second grip, no shoulder stock, no sling loops, no stripes, no labels. The original has none.
- **A full trigger guard or a curved pistol grip.** The grip is one tilted slab, and the trigger is a stub.
- **Making the collar the same color as the tube.** The collar, caps and grip are near-black; only the tube is light.
- **Centering the collar at the middle of the tube.** It sits at 62 percent of the way forward.
- **A beefy sight.** The flip-up sight is a tiny two-vane loop, 1 long and 3 tall.
- **Muzzle flash geometry** on the model itself.

## 9. Acceptance checklist

- [ ] Length : height : thickness is within about 10 percent of 5.6 : 2.1 : 1.
- [ ] Tube is about 80 percent of the empty length (36 of 45) and 6 across, an octagon with flat sides about 3 wide.
- [ ] The tube is light blue-gray (about #5A6467) with lighter top streaks; the caps, collar, grip and sight are near-black (about #2D2F2F).
- [ ] Rear cap is about 3 long and 8 by 8 across (a plus-shaped octagon), wider than the tube by 1 unit on each side.
- [ ] Muzzle ring is about 6 long and 8 by 8 (plus shape with square corners), as wide as the cap and twice as long, with a flat front face.
- [ ] Collar is 6 long and 7 tall and sits with its center about 62 percent of the way from the back; it has two thin side plates standing 0.5 proud of the tube flats.
- [ ] A small bridge block stands 1 above the tube's top on the collar; a tiny open sight loop stands on its front edge, topping out about 6.5 above the axis.
- [ ] Grip is one straight slab about 3 by 7 by 2, tilted about 17 degrees with its lower end toward the rear, reaching about 6.5 below the tube's bottom.
- [ ] A 2 by 1 by 1 trigger stub sits under the collar just ahead of the grip; there is no trigger guard.
- [ ] A loaded rocket shows only a three-step red cone past the muzzle (4 by 4, then 3 by 3, then 2 by 2, about 1 unit each); the empty gun shows a flat front face.
- [ ] Side silhouette matches section 4 (cap, tube, collar with sight, one raked grip, ring, nose); the underside is otherwise empty.
- [ ] Nothing rises above the tube except the collar bridge and the sight.
- [ ] No part floats; every box overlaps or touches a neighbor.
- [ ] The nuke launcher variant is a straight retexture with the same geometry.

## 10. Unsure

- **White bars on the muzzle ring.** In the original's flat render, two small white bars appear on the ring's sides (about 2 long and 0.5 to 1 tall). They are almost certainly leftovers from the loaded rocket's fin pieces, whose faces coincide with the ring's faces (a depth-fighting artifact). They are optional and probably not intended.
- **Hidden fins.** The rocket's fins and canards sit entirely inside the ring in the launcher model; the builder need not model them in the launcher.
- **Whether the rear cap is closed.** By the numbers it is a solid back face. In renders the rear looks like a flat dark disc; a shallow dimple would be a harmless deviation.
- **First-person numbers** come from formulas read, not seen in game. Treat the offsets as approximate. The firing roll depends on how the fire animation progress is sampled.
- **The sight's thinness.** The planes have zero thickness in the original; they read as hairlines from the front. Some thickness may be wise for readability.

## 11. The rocket projectile model

A separate short model, used for the flying rocket and for the rocket ammo item. The loaded launcher shows a copy of its front steps. A later projectile model should follow this.

**Axes.** Nose toward **+X**, Y down, axis at Y = 0. X = 0 is the middle of the rocket. In flight, the model is turned to face its velocity: yaw follows the heading and pitch tilts it up or down. It is drawn at **0.9 scale**.

**Gestalt.** A small, stubby **white missile with red bands**, a black tail fin set and a red nose, like a toy or old training rocket. It looks like two fat white pods joined by a thin, pale waist.

| Measure | Value |
|---|---|
| Length | 21 (X -10 to 11) |
| Warhead (front pod) | 6 long, 4 by 4 (X 3 to 9) |
| Motor (rear pod) | 6 long, 4 by 4 (X -10 to -4) |
| Waist (body) | 3 by 3, 20 long, running through both pods, so visible between them from X -4 to 3 |
| Nose | 1 long, 2 by 2 at X 10 to 11, with a 1-long 3 by 3 step where the body ends at X 10 |
| Fins | two sets, each a pair of thin crossing plates (about 2 long, 1 thick, 6 span), so a plus-shaped silhouette from the end |
| Weight in the frame | at 0.9 scale the rocket is about 19 units, or 1.2 blocks, long |

- **Tail fins**: at the rear (X -10 to -8): two crossing plates, black, 2 long and 6 across (1.5 beyond the 4 by 4 motor on each side).
- **Wing fins** at the warhead's rear (X 4 to 6): a second black pair at the same 6 span, but 2 long.
- **Wing panels**: two white slabs crossing at the warhead's middle (X 4 to 8): 4 long, 5 across by 2 thick, and 2 across by 5 thick. They show as a small white cross-piece on the warhead.
- **Nozzle**: the rear 4 by 4 face of the motor is a dark square (graduated, near black) inside a thin white rim, i.e. an open-looking nozzle.
- **Red nose**: the front is red, stepping down from the 4-wide warhead tip to the 3-wide body end to the 2-wide red nose tip.

**Palette.**

| Part | Base | Notes |
|---|---|---|
| Warhead, motor, wing panels | #FFFFFF to #D8D9D9 | white, slightly cool; flat; a lighter strip down the middle |
| Waist body | #F0F0F0 and #BEBEBE | alternating pale and mid gray with one **red band** in the middle |
| Red bands, nose | #FE1C1C | a red ring near the front of the warhead, one around the front of the motor, one in the waist, and the whole nose |
| Fins | #161616 | near black |
| Nozzle | #000000 to #2D2F2F | graduated, darkest at center |

The **nuke rocket** uses the same model with a yellow (#FFF600), black and brown retexture; the geometry is identical.

**Checklist (projectile).**
- [ ] Length is about 21, with two fat 4 by 4 pods 6 long each and a thin 3 by 3 waist between them.
- [ ] Both pods white with red rings; waist pale with a red band; nose red, stepping 4, 3, 2.
- [ ] Two black crossed fin sets, 6 span, at the tail and mid-warhead.
- [ ] Rear face reads as a dark nozzle in a white rim.
- [ ] Drawn at 0.9 scale, facing its direction of flight, with no spin.
