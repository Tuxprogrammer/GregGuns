# SCAR: clean-room model notes

Design notes for rebuilding the Techguns **SCAR** (a scoped, desert-tan battle rifle) look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and textures. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists, no field names and no texture coordinates; the builder designs its own UV layout. The magazine is covered as far as it shows on the gun.

Related facts: [ModelScar](../gun-models.md#modelscar-12864-used-with-a-256128-png-family-z-45-boxes-tabula-2-parts), [SCAR sheet](../gun-sheets.md#scar-scar), [rendering framework](../rendering-framework.md), [Techguns SCAR stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = 1/16 block = one texel at the native 128x64 scale (the original PNG is 256x128, so there are 2 pixels per unit). Muzzle toward **+X**. **Y points down** (Minecraft model convention), with **Y = 0 at the top of the scope**, the highest point of the gun. **Z = 0 is the gun's mid-plane**; **+Z is the shooter's left side, -Z the shooter's right side**. Sizes across Z are total width, centered unless a Z range is given. **X = 0 is the rear face of the black buttpad**, so the muzzle tip is at X = 71.8. Rounded to a tenth of a unit where it matters, otherwise to the nearest half unit. Angles are described as "tilt" with the direction stated in words (X-Y side view, with the muzzle on the right).

## 1. Gestalt

A long, lean, **flat-dark-earth (tan) battle rifle** in the FN SCAR-H mold, drawn with chunky boxes. At a glance you read: one very long, straight **tan receiver with a flat toothed top rail** running nearly the full length of the gun; a **black telescopic scope** riding on the rear half of that rail; a **tan, hooked, folding-style stock** that slopes down toward the rear and ends in a tall **black buttpad**; a **tan, stippled pistol grip** raked well back; a **plain vertical dark-tan magazine**; an **angled (triangular) foregrip** under the front handguard; and a long **black barrel with a thicker black muzzle device** that sticks out beyond the handguard. The gun is **mostly one color** (sandy tan, about #A2A379). Only the scope, buttpad, barrel, rails and sights are black, and the magazine and foregrip are a darker tan.

Defining features in order of visual importance:

1. **A long, low, tan body with a dead-straight top line** (the rail) and a long barrel in front. The receiver alone is half the gun's length.
2. **Black scope** (a fat diamond-section tube with bells at both ends) sitting on a riser over the rear half of the receiver. It is the tallest thing and the strongest dark mass on the gun.
3. **Sloped stock with a tall black buttpad**: the top line falls toward the rear at about 10 degrees, the bottom is flat, and a heel and buttpad hang below it.
4. **Triangular angled foregrip** under the handguard, and a **raked, stippled pistol grip**, with a **see-through trigger window** between the grip and the magazine.
5. **Black barrel stub and thicker muzzle device**, with a small gray collar where it leaves the handguard, and black handguard side rails that carry painted vent slots.
6. **A tiny charging-handle knob** on the left side (the only moving part).

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (buttpad to muzzle tip) | **71.8** |
| Total height (scope top to magazine bottom) | **23.3** |
| Height without the scope (rail top to magazine bottom) | 18 (rail top at Y 5.3) |
| Total thickness | **6** at the handguard side rails (Z -3 to +3); about **7** including the charging knob (to Z +4) |
| Length : height : thickness | about **12 : 3.9 : 1** (using 6 as the thickness) |
| Receiver (the main 4-wide box) | 37 long (X 15.8 to 52.8), 5 tall (Y 6.3 to 11.3), 4 wide; about 51 percent of the total length |
| Stock (buttpad to receiver back) | about 16 long, 22 percent of the length |
| Forward of the receiver (collar, barrel, muzzle) | 19 long, about 26 percent of the length |
| Muzzle device | 6 long, 2.8 across |
| Scope | 14 long (X 16.8 to 30.8), 4.2 tall, 3.8 across; its centerline is 2.1 below the scope top (Y 2.1) |
| Magazine | 7 long, 6 tall, only 2 wide (the narrowest body part) |

Key levels (Y): top of the scope 0; top rail top 5.3; receiver top 6.3; **barrel axis 9.5**; receiver bottom 11.3; stock bottom 12.8; trigger-guard plank 16.3 to 17.3; magazine bottom 23.3. Stock heel and buttpad bottom: 19.3.

How the pieces sit along the length, from the rear (X):

| X range | What |
|---|---|
| 0 to 2 | black buttpad |
| 1 to 7 | stock rear (heel and slanted strut) |
| 4 to 16 | stock body (cheek slab above, flat strip below) |
| 14.3 to 16.3 | the 2-long stock-hinge block against the receiver's back face |
| 15.8 to 52.8 | receiver; rail on top (X 16.3 to 52.3) |
| 16.8 to 30.8 | scope |
| 14.3 to 21.8 | pistol grip (rakes back from its top front corner at X 21.8) |
| 21 to 25.8 | open trigger window |
| 25.8 to 33.8 | magwell; magazine X 26.3 to 33.3 |
| 38 to 49 | foregrip mount plate; angled grip hangs below at X 38 to 44.5 |
| 40.3 to 52.3 | handguard side rails (the 12-long black vented panels) |
| 52.8 to 57.8 | gray collars and gas block |
| 57.8 to 65.8 | thin barrel |
| 65.8 to 71.8 | muzzle device |

## 3. Part by part

All parts are boxes unless noted. Z is the half-width around the mid-plane. "Overlaps" means the boxes interpenetrate by the stated amount so nothing floats. Hinge points are the top front corner of the tilted slab unless said otherwise.

### Receiver (main body)
- A box **37 long x 5 tall x 4 wide**: X 15.8 to 52.8, Y 6.3 to 11.3, Z -2 to +2.
- Plain: no window cut-outs, no stepped sections, no raised bumps; every feature is painted. The back face (X 15.8) is flat and is covered by the stock-hinge block.
- It is a single tan slab for the full length, including the handguard region. The "handguard" is just the front 12 units of this box plus the side rails below.

### Top rail
- A bar **36 long x 1 tall x 2 wide** on top of the receiver: X 16.3 to 52.3, Y 5.3 to 6.3, Z -1 to +1. It starts 0.5 unit in from the receiver's rear and ends 0.5 in from the front.
- **Picatinny teeth are painted, not modeled**: alternating light and dark transverse stripes, 0.5 unit wide each (a 1-unit period, about 70 stripes), on the top and on the upper edge of both side faces. The silhouette stays a clean straight line. Light #A2A57B, dark #616249 (a dark olive, not black).

### Lower receiver, magwell and chin bumps
- **Lower receiver:** 3 wide x 3 tall x 16 long, directly under the main receiver: X 17.8 to 33.8, Y 11.3 to 14.3, Z -1.5 to +1.5. It overlaps the receiver's bottom face. The pistol grip, trigger window and magwell all hang from it.
- **Magwell socket:** 3 wide x 3 tall x 8 long under the front half of the lower receiver: X 25.8 to 33.8, Y 14.3 to 17.3, Z -1.5 to +1.5. Flush with the lower receiver's front face.
- **Rear chin bump:** 3 wide x 2 tall x 2 long: X 15.8 to 17.8, Y 10.8 to 12.8, Z -1.5 to +1.5. It sits under the back of the receiver and pokes 1.5 below the receiver's bottom.
- **Front chin bump:** the same size, X 33.8 to 35.8, Y 10.8 to 12.8, at the front end of the lower receiver. Textured in a slightly darker tan (#8A8A66 shadings on the sides) so it reads as a pin or latch block.

### Magazine
- **2 wide x 6 tall x 7 long**: X 26.3 to 33.3, Y 17.3 to 23.3, Z -1 to +1. Inside the 3-wide socket with 0.5 unit of socket showing at the front and back.
- **Straight and vertical.** No curve, no taper, no base plate step, no cut-outs.
- It overlaps the socket's bottom by nothing; it simply starts where the socket ends at Y 17.3. (A 0.5-unit insertion into the socket is fine.)
- Darker tan than the body (#807550). The side faces carry 3 faintly lighter and darker vertical bands (a ribbed polymer look) and a slightly olive patch (#6A6C49) in the lower rear corner.

### Pistol grip
- A slab **3 wide x 9 long x 4 deep**, Z -1.5 to +1.5 (a hair narrower than the receiver at 0.99 times).
- **Hinge:** the grip's top front corner is at **X 21.8, Y 14.3** (the underside of the lower receiver). From there the slab tilts **25.6 degrees (about 26)** so the **bottom swings toward the rear**. The front edge slopes from (21.8, 14.3) down and back to (17.9, 22.4); the rear edge from about (18.5, 14.3 plus the depth) to (14.3, 20.7). The bottom face is a slanted edge rising toward the rear.
- Lowest point: Y 22.4, 0.9 above the magazine's bottom. Rearmost point: X 14.3, just in front of the stock's heel.
- Surface: **stippled** with a checkerboard of two tans on both sides (see section 5); the front and back edges are plain.

### Trigger group and window
- **Trigger guard plank:** a flat plank **2 wide x 1 tall x 6 long**: X 19.8 to 25.8, Y 16.3 to 17.3, Z -1 to +1. Its rear end overlaps the grip's front face; its front end touches the magwell socket's rear face (X 25.8) at the socket's top level.
- **Open window:** between the lower receiver's bottom (Y 14.3) and the plank's top (Y 16.3), from the grip's front face to the magwell's rear face (about X 21 to 25.8) there is **empty space 2 units tall and about 4.5 long**. This is a true see-through opening in the side view.
- **Trigger blade:** a thin dark box **1 wide x 2 tall x 1 long**, Z -0.5 to +0.5, hanging in that window from the lower receiver: top at Y 13.8, bottom at Y 16.0, tilted **15 degrees** with its bottom swung toward the muzzle (the hinge is its top, at about X 22.8). It sits about 1 unit in front of the grip face. Black (#282828).

### Stock (folding-style, one rigid piece)
All stock parts are tan except the buttpad. The stock is 4 wide at the top slab and 3 wide below.

- **Hinge block** against the receiver's back face: **4 wide x 7 tall x 2 long**: X 14.3 to 16.3, Y 5.8 to 12.8, Z -2 to +2. It overlaps the receiver by 0.5 and the stock slab by about 1.5.
- **Hinge pin (right side only):** a vertical diamond-section post, 6 tall, about 1.4 across (a 2x2 post turned 45 degrees about the vertical axis and shrunk to 0.6), at X 14.5 to 16.1, Y 6.3 to 12.3, on the **right side** (Z -2.8 to -1.2), half buried in the stock block. It reads as a hinge rod on the right face. Darker tan (#797B5B on its top).
- **Cheek slab (upper stock):** **4 wide x 4 tall x 11 long**, Z -2 to +2. Its front end is at X 14.8 with the top edge at **Y 5.8**; it **tilts down 10 degrees toward the rear**, so the rear end (X about 4) has its top at about Y 7.7 and its bottom at Y 11.6. Seen from the side this is the long, gently falling top line of the stock. It starts at rail height, 0.5 above the receiver top and 0.5 below the rail's top (so the rail overhangs it).
- **Underside strip:** **3 wide x 3 tall x 10 long**, level and horizontal: X 4.8 to 14.8, Y 9.8 to 12.8, Z -1.5 to +1.5. This gives the stock a **flat bottom at Y 12.8**. It overlaps the cheek slab's lower half.
- **Slanted strut (heel):** a **3 wide x 10 tall x 3 deep** slab hinged at its top (X 6.8, Y 8.8) and **tilted 15 degrees with its bottom swung toward the rear**: X 1.3 to 6.8, Y 8.0 to 18.5. It gives the stock a rear "toe" that hangs about 5.5 below the flat bottom.
- **Rear column:** a **3 wide x 11 tall x 3 deep** vertical column behind the strut: X 1 to 4, Y 7.8 to 18.8, Z -1.5 to +1.5. It overlaps the strut and the buttpad, and closes the gap between them.
- **Buttpad:** a vertical **black plate 4 wide x 12 tall x 2 deep**: X 0 to 2, Y 7.3 to 19.3, Z -2 to +2. Its top (Y 7.3) is 0.4 above the cheek slab's rear top (about 7.7), its bottom is the lowest point of the stock. Dead vertical.
- **Latch pin (left only):** a 1x1x2 diamond (a bar turned 45 degrees) at X 2.5 to 4.5, Y 10.6 to 12.0, on the left side (Z +0.6 to +2.0), dark (#202020). A tiny detail on the stock's left lower rear.
- The stock's underside, front of the heel, carries short **diagonal ribs** painted as hatch marks (see section 5).

### Scope
All black (#1C1C1C), assembled from boxes turned 45 degrees so the tube reads round (diamond cross-section). Center height Y 2.1, which is 3.2 above the rail's top (rail top 5.3).

- **Mount plate on the rail:** **2.7 wide x 1 tall x 4 long**, X 23.3 to 27.3, Y 4.8 to 5.8, Z -1.4 to +1.4. It overlaps the rail's top by 0.5.
- **Riser:** **1.8 wide x 3 tall x 6 long**, X 21.8 to 27.8, Y 2.3 to 5.3, Z -0.9 to +0.9, between the mount and the tube (dark gray #282828 base, a lighter strip along the front face). It overlaps the mount and the tube.
- **Tube (middle):** a square bar 2x2 turned 45 degrees, 6 long: X 19.8 to 25.8, Y 0.8 to 3.6, about 2.6 across. Its axis is the scope's axis.
- **Rear bell (eyepiece):** 3x3 box turned 45 degrees, 3 long, 0.9 wide-scaling: X 16.8 to 19.8, Y 0 to 4.2, Z about -1.9 to +1.9 (about 3.8 across, 4.2 tall).
- **Front bell (objective):** the same, 5 long: X 25.8 to 30.8, Y 0 to 4.2, about 3.8 across. The front bell is the longer one; the whole scope is a "dumbbell": fat 3 and 5 bells with a thinner 6-long waist.
- **Turret on top:** a 2x2x2 cube turned 45 degrees about the vertical axis and shrunk to 0.7: X 21.8 to 23.8, Y 0.3 to 1.7, Z -1 to +1. It sits on the tube's top, centered over the riser, and makes a small notch in the top silhouette between the bells. Dark gray (#222222 with #4B4B4B edge pixels).
- **Side turret (left only):** a 2x2x2 cube turned 45 degrees about the length axis and shrunk to 0.8 across, 0.7 up: X 21.8 to 23.8, Y 1.1 to 3.1, Z about 0.1 to 1.7 (flush with the tube's side, not protruding). Seen from the left it is a small dark diamond on the waist.

### Rear sight (flip-up)
- **Base plate:** 2.7 wide x 1 tall x 2 long, X 19.3 to 21.3, Y 4.8 to 5.8, black (#232323), directly on the rail under the scope's rear bell.
- **Pivot post:** a 4x2x2 box turned 45 degrees about the side axis and shrunk (0.8 wide, 0.7 tall and deep): X 17.8 to 19.8, Y 4.3 to 6.3, Z -1.6 to +1.6. It reads as a small black diamond (a chamfered block) at the scope's rear base, with faint #424242 edge pixels.
- **Notch blade:** a **1 wide x 1 tall x 3 long** bar, X 15.3 to 18.3, Y 4.6 to 5.6, Z -0.5 to +0.5, black, on the rail behind the pivot. It extends 1 unit behind the rail's rear end (X 16.3). The very top is a darker (#0C0C0C) strip.

### Handguard side rails
- Two black slabs, **1 wide x 2 tall x 12 long**, one each side of the receiver's front section: X 40.3 to 52.3, Y 8.1 to 10.1. Left at **Z +2 to +3**, right at **Z -3 to -2**. They butt against the receiver's sides (no overlap needed) and widen the gun from 4 to 6 here.
- Black (#282828) base, with the **vent slots** painted on the outer faces (section 5). They are the only thing that makes the front half look "wide" from the top.

### Bottom rail and foregrip mount
- **Bottom rail:** **2 wide x 1 tall x 17 long**, X 35.8 to 52.8, Y 11.3 to 12.3, Z -1 to +1, black. It starts at the front chin bump and runs to the receiver's front face. Visible in side view only as short black stubs at the rear end (X 35.8 to 38) and the front end (X 49 to 52.8); the foregrip hides the middle.
- **Front mount plate:** **3 wide x 2 tall x 6 long**, X 43.3 to 49.3, Y 10.8 to 12.8, Z -1.5 to +1.5, dark tan (#807550). It overlaps the bottom rail and the receiver's bottom edge.
- **Rear mount plate:** **3 wide x 1 tall x 5 long**, X 38.3 to 43.3, Y 11.8 to 12.8, Z -1.5 to +1.5, the same tan. Continuous with the front plate (together an 11-long, stepped plate).

### Angled foregrip
- Dark tan (#807550), both struts **2 wide** (Z -1 to +1), 1 thick, hanging from the mount plate and meeting at a point below it:
  - **Front strut:** 6 long, hinged at its top at **X 44.3, Y 11.8**, tilted **40 degrees**, so it descends toward the **rear**: its bottom end is at about X 39.7, Y 16.4.
  - **Rear strut:** 5 long, hinged at its top at **X 38.3, Y 12.2**, tilted **110 degrees** (a steeper tilt in the same sense as the front strut's 40) so it descends **steeply and slightly toward the front**: its bottom end is at about X 40.9, Y 16.9. It is nearly vertical (about 20 degrees off), leaning forward.
- The two struts form a **narrow triangle** with the mount plate as its top edge: width about 6 at the top (X 38.3 to 44.3), a point at X 40 to 41 about 4.6 below the mount. The inside of the triangle is empty (see-through).
- **Two tiny tan nubs**: a 2x1x1 block at X 48.1 to 49.1, Y 12.6 to 13.6 under the front end of the mount plate (the front of the plate), and a 1.6x1x1 block on the front strut's outer face around X 42.3 to 43.7, Y 13.4 to 14.8, tilted with the strut. Both are rail-clip details.

### Gas block and collars (X 52.8 forward)
Everything in this group is on the barrel axis at **Y 9.5** (except the gas block above it) and centered at Z 0.

- **Barrel root:** a 2x2 diamond bar (a square bar turned 45 degrees, 2 across after shrinking), **1 long**, X 52.8 to 53.8, Y 8.5 to 10.5. Black. It is the thin stub that leaves the receiver's front face.
- **Front collar:** a **gray** diamond 2.8 across and 2.8 tall (a 2x2 bar turned 45 degrees), **4 long**: X 53.8 to 57.8, Y 8.1 to 10.9, Z -1.4 to +1.4. Mid-gray (#646464), with flat noise of one or two levels. It is the only gray on the gun.
- **Gas block:** a second **gray** diamond, 2.3 across (the 2x2 bar turned 45 degrees and shrunk to 0.8), **5 long**: X 52.8 to 57.8, Y 6.5 to 8.8, Z -1.1 to +1.1. It sits **above** the collar and overlaps its top corner by about 0.3; it starts flush with the receiver's front face and ends flush with the collar's front. Same gray (#646464).
- **Front sight block:** **2 wide x 1 tall x 4 long**, black (#232323): X 54.3 to 58.3, Y 5.3 to 6.3, Z -1 to +1. Its top is level with the rail's top (Y 5.3); it **continues the top line** and does not rise above it. It rests on the gas block's top.
- **Front sight post (support):** a 1.6 wide x 2 tall x 1 long block, black, at X 53.3 to 54.3, Y 5.5 to 7.5, Z about -1 to +0.6 (a hair off-center toward the right). It joins the sight block to the gas block.

### Barrel and muzzle device
- **Barrel:** a bar of 2x2 section turned 45 degrees (a diamond, 2 across and 2 tall), **8 long**: X 57.8 to 65.8, Y 8.5 to 10.5, black (#282828 base, with a near-uniform fill). It starts inside the collar (overlap 0.5) and ends at the muzzle device.
- **Muzzle device:** a **fatter diamond bar** (2x2 turned 45 degrees, not shrunk: 2.8 across and 2.8 tall), **6 long**: X 65.8 to 71.8, Y 8.1 to 10.9, Z -1.4 to +1.4. Black with painted darker bands (section 5), which read as a short flash hider or suppressor. It is 0.8 wider than the barrel and the same width as the front collar.
- No taper, no slots, no flutes; the muzzle device's end is flat.

### Charging handle (the moving part, "part 1")
- A tiny **dark knob** on the **left** side of the receiver: 2 long x 1 tall x 1 wide (a bar turned 45 degrees about the length axis and shrunk, so it reads as a small diamond), at X 37.8 to 38.8, Y 7.0 to 8.0, **Z +2 to about +4** (it sticks out about 1.5 to 2 from the receiver's left face). Near-black (#232323).
- Sits at the **front end of the painted slot** on the receiver's left side (section 5). In the original this is **model part 1**, separate from the rest.

## 4. Silhouettes per view

- **Side (the main view, muzzle right):**
  - A long horizontal tan bar (the receiver and rail, 37 long) with a straight top line, a **black scope** above it standing 5.3 higher (dumbbell: the front bell is taller than the middle and the rear bell is shorter).
  - To the left: the **stock**, with its top falling about 10 degrees from the rail down to the buttpad, a flat bottom 1.5 below the receiver's bottom, and a **heel** hanging below that, ending in a black vertical plate, so the stock reads as a notched wedge with a black butt.
  - Under the receiver: a **stippled grip** leaning back about 26 degrees, then a **see-through window** (2 tall, about 4.5 long) with the thin trigger blade inside, then the **magazine** (dark tan, straight) hanging lower than the grip's bottom by 0.9.
  - To the right: the **triangular foregrip**, an open triangle hanging below the handguard and ending **before** the front of the handguard (about 12 short of the receiver front); then the black **barrel root**, the **gray collar and gas block** (a stepped, two-tier gray block), the **front sight block** level with the rail, a **thin barrel**, and a slightly thicker **muzzle**.
  - Negative space: the window at the trigger; the open triangle of the foregrip; the long notch between the stock's flat bottom and the grip (about 2.5 wide between the stock's heel and the grip's rear); the gap between the grip and the magazine below the guard plank (the plank is 1 tall, and the space under it to the magwell is open).
- **Top:** a thin tan bar, **4 wide** over the stock and receiver, with the **6-wide handguard** section (side rails, X 40 to 52). The **scope** is a black bar **3.8 wide, 14 long** centered on the spine over the rear-middle of the receiver, the mount and rail visible at its two ends. A **left-side knob** pokes out 1.5 to 2 units at X 37.8 to 38.8. Barrel and muzzle form a thin black centerline, the muzzle slightly fatter. The stock top is 4 wide, same as the receiver, ending in a black end cap.
- **Front:** a small, tall, narrow blob: at the top the **black scope bell** (3.8 across, 4.2 tall, diamond) on the riser, below it the receiver (4 wide, with side rails to 6 at the handguard level), the **gray collar diamond** (2.8) in the center, and the **black muzzle diamond** (2.8) in front of it, centered on the axis at Y 9.5. The gas block makes a second gray diamond above the muzzle. Below the receiver: the foregrip triangle and the magazine column (2 wide), then the lower grip.
- **Back:** the **black buttpad** dominates, 4 wide x 12 tall, with the **scope's rear bell** (a small diamond) above and behind it, and the tan receiver and sides visible as a thin 6-wide strip at the handguard. The buttpad reaches Y 19.3 and the magazine's bottom is at 23.3, so the magazine and grip appear below the buttpad as a narrow tan column.
- **Underside:** a very thin tan bar, 4 wide; the stock's strip and heel at the rear; the lower receiver (3 wide) and the guard plank, magazine (2 wide) and foregrip mount plates (3 wide) under it; a black stripe of bottom rail; the barrel and muzzle as a black center line at the front.

## 5. Materials and palette

The texture is a flat, desert **tan polymer** with black accents. All values measured from the original's 256x128 texture (the body tan has per-pixel noise of about 1 level, so treat the variants as one color).

### Default camo (the tan "Default Camo")

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Receiver, stock slabs, strut, hinge block, lower receiver, magwell, grip, chin bumps | flat-dark-earth polymer | #A2A379 (noise range #A2A379 to #A3A47A, #A2A478, #A2A57B) | #A3A47A | #727354 (panel edges), #8A8A66 (stipple, grip pits), #37372E (dark seam and slot edges) |
| Top rail | same tan, with stripes | #A2A57B | #A3A47A | #616249 (the 0.5-unit alternating dark stripes) |
| Magazine, foregrip, mount plates, rear chin bump front | darker tan | #807550 | #827A52 | #6A6C49 (olive patch on the mag's bottom), #605838 (seams) |
| Buttpad, handguard side rails, bottom rail, barrel, trigger | black polymer | #282828 | #292929 | #000000 (slots and gaps), #181818 (darker bands on the muzzle) |
| Muzzle device | black, two banded | #282828 | #292929 | #181818 (bands) |
| Front sight, rear sight, pivots, knob | near-black | #232323 | #242424 | #0C0C0C (rear sight's top strip), #1B1B1B |
| Scope body and bells | very dark gray | #1C1C1C | #1D1D1D | #000000 (outline pixels) |
| Scope riser, mount plate, turret | dark gray | #282828 (turret #222222) | #292929 (turret #4B4B4B) | #FCFCFC small lens highlights on the bells' faces |
| Gray collar and gas block | mid-gray metal | #646464 | #656565, #666666 | #616161, #626262 |
| Stock latch pin | near-black | #202020 | #282828 | |
| Accent dots | maroon | #3B1D1A (44 pixels at 256x128, about 11 tiny dots) | | |

### Black camo (the second texture)
Everything black stays black; the tan turns into a very dark gray. The gray collars are unchanged and the maroon dots disappear.

| Part | Base | Highlight | Shadow / seam |
|---|---|---|---|
| Receiver, stock slabs, grip, lower receiver, magwell | #202020 | #232323 | #181818, #1A1A1A, #101010 |
| Lighter panels (plates, rails, strut faces) | #383838 (large areas) | #404040 | #303030 |
| Handguard side rails, bottom rail, barrel, muzzle, buttpad | #3B3B3B | #3A3A3A | #000000 slots, #232323 bands |
| Magazine, foregrip, mount plates | #3F3F3F | #404040 | #2F2F2F |
| Scope | #292929 | #282828 | #000000 |
| Gray collar and gas block | #646464 | #656565 | #626262 (unchanged) |
| Sights, knob | #343434 | #333333 | #282828, #0C0C0C |

### Camo variants: **2** (Default tan, Black). Same geometry; only the texture changes.

### Shading style
- **Flat, low-noise, low-contrast.** Faces have almost no gradient: a single base color, one-level noise, and painted detail. No scratches, no weathering.
- **Painted detail on the receiver sides (what makes the gun read as a SCAR):** on **both** sides, a **long thin dark slot** (the charging-handle track) at about Y 7.5, from about X 25.7 to X 37.7 (12 long, 0.7 to 1 tall), and below it a **long dark rounded "pill" slot** (an ejection or magazine-release recess) at about Y 9.5 to 10.8, from about X 24.3 to X 35.5, with a slightly lighter frame around it. A small **light gray rectangle** (about 1 x 0.6) near X 27.5, Y 12.8, in the lower receiver's side (the mag release). A few dark rivet dots (1 pixel) and about 11 tiny maroon dots scattered around the lower receiver and stock hinge.
- **Handguard side rails:** each outer face has **10 short vertical black slots** (about 0.7 wide, 1.5 tall, 0.9 apart) in the lower part of the face, filling the 12-long panel from X 40.3 to 52.3; above them, **3 short horizontal dark slots** (about 3 long x 0.5 tall) across the upper face at about Y 7.3 on the receiver's side face above the rails. The 12 x 2 panel is black with the slots darker (#000000).
- **Stock:** a faint vertical **fold seam** line near the hinge (X about 14 to 16), a darker #727354 line. A small maroon dot near the cheek top. The heel's underside has **3 to 4 short diagonal hatch marks** along its lower-rear edge, tan on darker tan (#8A8A66).
- **Pistol grip:** a **checkerboard stipple** (alternating #A2A379 and #8A8A66, 0.5-unit cells) on both side faces, the front and rear strips plain.
- **Magazine:** darker tan with 3 vertical light and dark bands on the sides; a small olive (#6A6C49) block near the bottom rear.
- **Muzzle device:** two darker bands (#181818) across the otherwise #282828 surface, separated by about half its length.
- **Scope bells:** a small #FCFCFC lens-glint pixel cluster on the front face of the objective bell and the back face of the eyepiece bell; the rest is flat #1C1C1C.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units, in this order for the base pose: flip 180 degrees about X (Y-down becomes screen-up), then yaw, then scale. The values come from formulas read, not seen in game.

| View | Yaw and scale | Offset | Notes |
|---|---|---|---|
| First person (idle) | yaw 225 degrees, scale 1.4 | translate (-0.55, -0.35, -0.65) in the model frame | the camera sees the gun from the **left-rear**, so the stock's buttpad, the scope's eyepiece and the grip show; the muzzle points away and slightly across |
| First person, firing | up to **5 degrees** pitch about the cross axis at mid-animation | plus a backward shift of 0.15 at mid-animation | a small, quiet recoil; much less than the revolver |
| First person, firing (charging handle) | none | the knob slides back along the gun axis up to **0.7 block** (about 11 units, the length of its slot), peaking at 15 percent of the cycle, then returns | **only part 1 moves**, and only the knob |
| First person, reloading | tilts up to **50 degrees** about the cross axis at mid-reload | smooth rise and fall; the pose peaks at 50 percent progress | the muzzle drops and the magazine rises toward the eye |
| First person, aimed | no model shown; a **sniper scope overlay** is drawn and the view zooms | zoom factor 0.65 (the field of view shrinks to 65 percent) | the overlay is anchored at (1.795, 0, -0.685) in model blocks; the scope recoil nudge is (0.15, 0.20, 0.10) times the recoil |
| Third person (held) | yaw 135 degrees, scale 0.8 | translate (0.1, -0.6, 1.1) | the grip is in the hand, the muzzle forward |
| Third person recoil | pitches up to **15 degrees** about the cross axis in the first quarter of the first half, then eases back | | |
| Third person reload | tilts **30 degrees** about the cross axis and **40 degrees** about the vertical axis at mid-reload, about a pivot of (0, -0.2, 0.05) | | |
| Ground (dropped) | scale 0.6 | translate (0, -0.45, 0) | lies flat on its side |
| Inventory icon | yaw 90 degrees after the flip, scale 0.52 | translate (0.3, 0, 0) | the icon is the 3D model, side-on, filling the slot |

**Muzzle flash point:** at the front of the muzzle device on the barrel axis (model X about 72, Y about 9.5, Z 0). Flash sprite scale 1.5; third-person flash offset about (0, 0.03, 0).

**Gun facts that affect the look:** a 20-round magazine; an automatic rifle (4-tick fire delay); a 45-tick (2.25 s) reload. The SCAR **always has a scope**: the aimed view uses the scope overlay and the scope is a permanent part of the model.

## 7. Motion

- Only **one moving part**: the **charging handle** knob. When the gun fires it slides **back** (toward the stock) about 0.7 block (11 units) along its 12-long slot on the receiver's left side, reaching the back at about 15 percent of the fire animation, and returns by the end. The slot and its travel are what justify the 12-long painted track. The builder should make the knob a separate named part.
- Everything else is whole-gun transforms (section 6). Nothing else moves: the magazine, trigger, stock and sights are static. The magazine does not drop out during reload; the whole gun tilts.
- Fire cadence is quick (4 ticks), so the knob reads as a flicker; keep it a single small block.

## 8. What to avoid

- **Making it look like the M4.** The SCAR is **tan**, has a scope (not a red-dot), a **straight** magazine, a **triangular** foregrip (not a vertical post), a hooked folding stock with a vertical black buttpad (not a collapsible buffer-tube wedge) and a **black muzzle device** with a **gray collar**. Do not give it a quad-rail handguard or a black body.
- **A dark body.** The body is bright sand (#A2A379). Only the accents are black.
- **A thick barrel or a 12-long muzzle brake.** The barrel is a 2-across stub, 8 long; the muzzle is 6 long and 2.8 across.
- **A modeled rail with teeth.** The teeth are painted; the rail is one smooth bar.
- **A floating scope.** The scope sits on a mount plate and a riser that overlap the rail and the tube.
- **A closed trigger window.** The window between the grip and the magwell is open; the trigger blade hangs in it.
- **A solid triangular foregrip.** The triangle's inside is open.
- **A vertical grip.** It is raked back by about 26 degrees.
- **A curved or tapered magazine.** It is straight, 2 wide.
- **A thick stock without the sloped top.** The cheek slab falls about 10 degrees toward the rear, and the heel and the buttpad hang below the flat bottom.
- **Moving the charging handle to the right side**, or giving it a big handle. It is a 1.5 to 2 unit knob on the left.
- **A muzzle brake with slots, a suppressor much fatter than 2.8, or fins**: none.

## 9. Acceptance checklist

- [ ] Length : height : thickness is within about 10 percent of 12 : 3.9 : 1 (71.8 x 23.3 x 6).
- [ ] The receiver is a plain 37 x 5 x 4 box that is about half the gun's length; the barrel and muzzle extend 19 beyond it.
- [ ] The top line (rail top, front sight top) is straight at Y 5.3 and nothing but the scope rises above it.
- [ ] The scope is black, dumbbell-shaped (3 and 5-long bells, 6-long waist), 14 long and 4.2 tall, with a small turret on top and its center line 3.2 above the rail.
- [ ] Stock: top slab falls about 10 degrees toward the rear; flat bottom at Y 12.8; heel strut leaning back about 15 degrees; vertical black buttpad 4 x 12 x 2.
- [ ] Pistol grip is 3 wide, about 9 long, raked about 26 degrees with the bottom toward the rear, and has a stipple pattern.
- [ ] The trigger window is open (2 tall, about 4.5 long), with a small dark trigger blade inside tilted about 15 degrees.
- [ ] Magazine is straight, vertical, 2 wide x 6 tall x 7 long, in a 3-wide socket, and darker than the body.
- [ ] Foregrip is an open triangle (two 2-wide struts, one tilted 40 degrees, one steep) hanging from a stepped mount plate, in the darker tan.
- [ ] Handguard side rails: two black 1 x 2 x 12 slabs, left and right, with 10 painted vertical slots each.
- [ ] Front end: black barrel root, a gray 2.8-diamond collar (4 long), a gray gas block above it, a black front sight block on top level with the rail, a thin 8-long barrel, and a fatter 6-long black muzzle device.
- [ ] Palette: body about #A2A379, mag and foregrip about #807550, black parts about #282828, scope about #1C1C1C, gray collars about #646464, 0.5-unit rail stripes in #616249.
- [ ] Receiver sides carry the long charging slot (12 long) and the lower pill-shaped slot; a separate small knob sits at the front end of the slot on the left only.
- [ ] Two texture variants exist (tan and black) with identical geometry.
- [ ] No part floats: every box overlaps or touches a neighbor.

## 10. Unsure

- **Which side the charging knob is on.** The original's documentation calls it "right side", but the model's sign convention and renders put it on the same side as the shooter's **left**. This note uses left; the builder may mirror it without affecting the checklist.
- **Muzzle flash coordinates** are in a frame I could not confirm; use the muzzle tip.
- **Exact stipple cell size and the slot pixel positions** were read from a 2x texture at low resolution; treat them as accurate to about 0.5 unit.
- **The gray gas block versus the collar overlap.** The two gray diamonds overlap and both read as "gray steel"; the exact split (one box or two) is less important than the stepped two-tier look.
- **The 11 maroon dots** are an in-joke detail (safety, rivets) of unknown intent. Optional.
- **The 0.99 and 0.8 and 0.9 shrinks** in the original (a few boxes are 1 percent to 20 percent narrower than their nominal size) are not worth copying except where stated: the scope bells (3.8 across), the collar (2.8), the gas block (2.3) and the barrel (2).

## Round 2 corrections (orchestrator review)

Round 1 comparison (side 0.975, iso 0.98, top 0.945, front 0.878, back 0.877, below the 0.93 bar on front and back).

- **Width.** The original is 7.4 units wide (0.463 block) at its widest; ours is 7.0 (0.438). Widen by 0.4 unit. The extra width belongs to the handguard side rails and stock slabs (the front half and rear half), not the middle.
- **Back view.** The original's buttpad is a black plate ribbed with many fine horizontal stripes (about 12 dark and light bands over its height). Ours is a flat black plate. Paint the horizontal ribs. Above the buttpad the original shows a cheek slab with a visible step down; ours is a smooth block.
- **Front view.** The original's receiver face shows a distinct wide step (the handguard rails sticking out on both sides of the barrel root, about 1 unit each side), with the foregrip and magazine visible below as tan. Ours is a narrow column; the rails are too thin.
- **Black camo (checked against the original's black texture).** The original's black variant is purely neutral gray: there are no olive or green-tinted pixels anywhere on it. Ours is wrong in two places: the **magazine's lower rear olive patch (#6A6C49)** must become a neutral gray (use #2F2F2F to #404040, matching the table above), and the **diagonal hatch marks under the stock heel** must be the neutral dark gray of that region (#202020 with #181818 grooves), not the tan-era dark olive (#616249). The Picatinny stripes in this variant also must be neutral: alternate #383838 and #202020, no olive.
- Keep the tan variant as is.
