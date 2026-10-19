# Vector: clean-room model notes

Design notes for rebuilding the Techguns **Vector** look-alike (the Kriss-Vector-style SMG) in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelVector](../gun-models.md#modelvector-128×64-used-with-a-256×128-png-family-z-28-boxes-tabula-1-part), [Vector sheet](../gun-sheets.md#vector-vector), [rendering framework](../rendering-framework.md), [Techguns Vector stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = one texel at the native 128x64 layout scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention). **X = 0 is the back face of the butt pad** (the rearmost point of the gun); the muzzle tip is at X = 44. **Y = 0 is the top of the red-dot sight's roof plate** (the highest point of the gun); the bottom of the magazine is at Y = 27.4. **Z = 0 is the gun's mid-plane**; sizes across Z are total widths, centered. "Front" means +X, "rear" means -X, "above" means smaller Y. Side-view coordinates are written (x, y). Rounded to 0.1 unit. **Tilts:** every tilted slab tilts in the side plane, with its top edge fixed and its bottom swinging toward the rear (-X), so a tilt of 20 degrees leans a slab's bottom back by 0.34 units per unit of length.

## 1. Gestalt

A compact, boxy, **tactical submachine gun with a big slanted block under the receiver**. The silhouette is a thin dark slab on top (receiver, rail, red dot, skeletal stock) and a tan, raked "lower" hanging beneath it, with a long magazine that continues the slope of the lower's front edge. There is almost no barrel; the front of the gun is a short flat-faced handguard with a tiny muzzle stub. Everything is **very thin** (about 3 units wide for a 44-unit-long gun), so seen from the side it is large and graphic, and seen from the front it is a narrow column.

Defining features in order of visual importance:

1. **The raked tan lower block**: two stacked 20-degree-tilted slabs (10 units deep together) whose front edge slants back going down, continuing into the magazine in one straight line. It is the only big light-colored mass in the default camo.
2. **A raked pistol grip behind it**, parallel to the slabs (same 20 degrees), with a 3.6-unit open slot between them where a small trigger sits. The slot is see-through from the side.
3. **A dark, long upper receiver with a slotted top rail and a red-dot hood** that sits roughly in the middle of the receiver's length.
4. **A skeletal stock**: a flat top bar, a pad at the very back, and a diagonal strut plus two diamond-section cross bars between them. It has a real hole in the side view.
5. **A stubby front end**: the handguard ends flat, only 3 units of muzzle stub stick out, and a short vertical foregrip (diamond section) hangs under the handguard.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt pad back to muzzle tip) | 44 |
| Total height (red-dot roof top to magazine bottom) | 27.4 |
| Height without the magazine (roof top to floor underside) | 18.5 |
| Total thickness | 3.2 (receiver and hood); everything else is 1 to 3 wide |
| Length : height : thickness | about 13.8 : 8.6 : 1 |
| Upper receiver | 25 long (x 16 to 41), 3.2 wide, 3 tall (y 5.1 to 8.1) |
| Stock (pad back to receiver rear) | 16 long, 36 percent of the total |
| Lower block (slab pair) | 10.3 long at the receiver's underside, 10.3 tall (y 8.1 to 18.4) |
| Pistol grip | 10 long along its axis, 3 wide, 4 deep |
| Magazine | 11 long along its axis, 2 wide, 3 deep; hangs about 9 below the floor |
| Barrel beyond the handguard | 3 (the handguard itself is flush with the receiver's front) |
| Symmetry | left-right symmetric; only painted details may differ per side |

How the pieces sit along the length, from the rear: the butt pad (x 0 to 2); the stock bar (x 5 to 16); the receiver (x 16 to 41) with the pistol grip under its rear (grip's top-front corner at x 21.1); a 3.6-wide trigger slot (x about 21 to 24.7 at the receiver's underside); the lower block (x 24.7 to 35 at the receiver's underside); the handguard under the receiver's front 10 units (x 31 to 41); the barrel stub (x 41 to 44). The magazine is **forward of the pistol grip** and hangs lower than it: the grip's bottom is at y 17.4 while the magazine's bottom is at y 27.4.

The top line has four levels, from rear to front: the stock bar top at y 6.1; the receiver top at y 5.1 (a 1-unit step up, smoothed by a small wedge); the rail top at y 4.1; and the red-dot hood on top reaching y 0.

## 3. Part by part

All parts are boxes, tilted or turned where stated. Z widths are centered on the mid-plane (Z = 0). Overlaps between neighbors are intentional, so no part floats.

### Upper receiver
- A plain bar **25 long, 3.2 wide, 3 tall**, x 16 to 41, y 5.1 to 8.1. Square ends, no taper, no bevels.
- Its rear face (x = 16) meets the front end of the stock bar. Its underside (y = 8.1) is the reference line from which the lower block, grip and handguard hang.
- Detail only on the flanks: a small black ejection port and a few tiny light-gray pin dots (see section 5).
- Touches: the stock bar (rear), the rail (above), the red-dot base (above, in the middle), the handguard (below, front), the slabs (below, middle), the pistol grip (below, rear).

### Top rail
- A flat bar **23 long, 2 wide, 1 tall**, x 17 to 40, y 4.1 to 5.1, centered. It is inset 1 unit from the receiver's rear end and 1 unit from its front end.
- Painted slot pattern: across the top, a black slot every other half-unit (about 12 slots over 23 units) alternating with dark gray ribs. It is a Picatinny look; the geometry is a flat bar with no cut slots.

### Red-dot sight
Dark gray, three pieces plus a dot:
- **Base block:** 7.2 long, 3.2 wide, 2.4 tall, x 24.4 to 31.6, y 2.4 to 4.8. Its bottom 0.7 unit overlaps the rail. It is the long, low mount.
- **Two side walls (the hood):** each **3.2 long, 0.8 wide, 1.6 tall**, x 25.0 to 28.2, y 0.8 to 2.4, one on each side of the mid-plane: z -1.6 to -0.8 and z 0.8 to 1.6. Between them is a **1.6-wide, 1.6-tall channel** that is open at both ends.
- **Roof plate:** 3.2 long, 3.2 wide, 0.8 thick, x 25.0 to 28.2, y 0 to 0.8, lying on the walls. This is the highest point of the gun.
- **Red dot:** a single 1 by 1 unit plane (zero thickness, facing front and back) in the middle of the channel at **x = 26.0**, y 1.0 to 2.0, z -0.5 to 0.5. Pure red, the only saturated color on the gun. Seen from behind it is a red square in a dark tunnel.
- The base is 7.2 long but the hood is only 3.2 long and sits toward the rear: the hood begins 0.6 unit ahead of the base's rear end, leaving about 3.4 units of bare base in front of it. The sight is therefore a low mount with a short box on its rear half.

### Handguard
- A box **10 long, 3 wide, 3 tall**, x 31 to 41, y 8.1 to 11.1, directly under the receiver's front 10 units. Its front face is flush with the receiver's front face (x = 41), which makes a **flat, square front wall 3.2 wide and 6 tall** (receiver plus handguard) with only the barrel stub sticking out.
- **Tan** in the default camo (dark gray #2E2E2E in the Black variant).
- Touches: the receiver (above), the front slab's top-front corner (x 31 to 35 overlap), the bottom rail (below).

### Barrel stub
- **Barrel:** a 1x1 square bar **2 long** turned 45 degrees about its axis (diamond, about 1.4 across), x 41 to 43, centered at y 9.3 (1.2 below the handguard's top, so in its upper half), z = 0. It starts buried in the handguard's front face.
- **Muzzle collar:** a 2x2 square bar **1 long** turned 45 degrees (diamond), squashed to 70 percent in both cross directions so it reads about **2.0 across** (not 2.8), x 43 to 44, centered at y 9.3. It sits on the front of the barrel and is thicker (2.0) than the barrel (1.4).
- Both medium dark gray. No flash hider, no slots, no taper: two stacked diamond stubs, the thin one then the fat one. The muzzle tip is the front of the collar at (44, 9.3, 0).

### Bottom rail and foregrip
- **Bottom rail:** a flat bar **7 long, 2 wide, 1 tall**, x 33.5 to 40.5, y 11.1 to 12.1, under the handguard. Dark with a black slot pattern like the top rail (3 or 4 slots visible).
- **Foregrip mount plate:** a small block **4 long, 3 wide, 1 tall**, x 35.5 to 39.5, y 11.2 to 12.2, hugging the underside of the bottom rail. It is wider (3) than the rail (2).
- **Foregrip:** a 2x2 square bar **7 tall** turned 45 degrees about the vertical axis (a diamond column, **about 2.8 across**), centered at x = 37.5, z = 0, y 12.1 to 19.1. It hangs straight down from the mount plate and is **not tilted**. Medium dark gray with painted horizontal grooves every unit in its upper half (3 or 4 darker lines).
- The foregrip's bottom (y 19.1) is the lowest point of the front half and about 0.6 below the floor (y 18.5).

### Lower block: two tilted slabs
Two identical tilted slabs, 3 wide (z -1.5 to 1.5), each **5 deep and 11 tall** along its own axis, tilted **20 degrees** with the bottom swinging toward the rear. Tan.
- **Front slab:** top-front corner (hinge) at **(35.0, 8.1)**. Front edge runs down-back to (31.2, 18.4). Rear edge runs from (30.3, 6.4) (buried in the receiver) to (26.5, 16.7).
- **Rear slab:** the same shape shifted 5 units toward the rear, hinge at **(30.0, 8.1)**, front edge to (26.2, 18.4), rear edge from (25.3, 6.4) (buried) to (21.5, 16.7).
- Together they form one block **10.3 long at the receiver's underside** (x 24.7 to 35). Its **front edge is one straight 20-degree line** from (35.0, 8.1) to (31.2, 18.4); this line continues into the magazine's front edge.
- Cut square, so the bottom is a two-tooth sawtooth (each tooth sloping 20 degrees: the front slab's bottom runs (31.2, 18.4) to (26.5, 16.7), the rear slab's runs (26.2, 18.4) to (21.5, 16.7)). The floor plates (below) fill the sawtooth so it reads as a flat floor.
- Painted details: the rear slab's flank carries a 2 by 2 pattern of four round dimple screws; the front slab's flank carries two tall oval recesses with short horizontal rib lines beside them (vent slots). See section 5.

### Floor plates
- Two flat, untilted, 3-wide plates fill the sawtooth under the slab pair and give the block a flat bottom:
  - **Upper plate:** 9 long, 1 tall, x 21.5 to 30.5, y 16.5 to 17.5.
  - **Lower plate:** 10 long, 1 tall, x 21.2 to 31.2, y 17.5 to 18.5, directly below it. Its underside, y = 18.5, is the **floor** of the gun.
- Both tan.

### Magazine
- A box **2 wide, 3 deep, 11 long**, tilted the same **20 degrees**, hinge (top-front corner) at **(31.3, 17.1)** (inside the floor plates). Front edge runs down-back to (27.5, 27.4); rear edge from (28.5, 16.1) to (24.7, 26.4).
- Its front edge lies on the same straight line as the slab pair's front edge (a 0.5 unit offset is fine), so from the side the front of the gun is **one unbroken slanted line from y 8.1 to y 27.4**.
- It emerges through the floor at the plates' **front** end (x 28.5 to 31.3 at the floor), not the middle.
- Medium dark gray (#343434 family), plain, with only fine noise. No ribs, no base plate, no visible cartridges.
- It is narrower (2) than the lower block (3), so from the front you see a thin column.

### Pistol grip
- A box **3 wide, 4 deep, 10 long**, tilted **20 degrees**, hinge at **(21.1, 8.0)**, front edge to (17.7, 17.4), rear edge from (17.3, 6.6) (buried in the receiver) to (13.9, 16.0). Tan.
- It is parallel to the slab pair, separated by a 3.6-unit gap measured along X (about 3.4 perpendicular). At the receiver's underside the grip's front edge is at x 21.1 and the rear slab's rear edge is at x 24.7.
- Its bottom end is square to its own axis, so the bottom edge slopes up toward the rear (front corner (17.7, 17.4), rear corner (13.9, 16.0)).
- Painted details: stippled panels (two-tone tan, sand-grain look) on each flank, a few horizontal grip lines, and a small oval recess near the top.

### Trigger guard floor and trigger
- **Guard floor:** a 3-wide, 4.1-long, 1-thick plate tilted **17.8 degrees** (rear end higher), spanning from the grip's front-bottom to the floor plate, x 17.6 to 21.7, y 16.4 to 18.6. A smaller **wedge** (3 wide, 2 long, 1 thick, tilted 20 degrees the opposite way, so its rear end is lower) at x 19.8 to 22.0, y 16.4 to 18.0 sits on it near its front end. Together they close the bottom of the trigger slot with a shallow V. Both tan.
- **Trigger slot:** the 3.6-wide space between the grip and the rear slab, from the receiver's underside (y 8.1) down to about y 16.5. It is open on both flanks (no side plates), so you see through it in the side view, except where the trigger is.
- **Trigger:** a 1 wide, 3 tall, 1 deep dark bar, tilted **10 degrees** the opposite way to the slabs (its bottom swings forward), x 21.0 to 22.5, y 7.6 to 10.7. Its top is buried 0.5 into the receiver's underside. It sits against the grip's front edge, not in the middle of the slot: the open gap in front of it is about 2.2 units.

### Stock
All dark gray (#262626 family), 3 wide unless noted.
- **Stock bar:** **11 long, 3 wide, 2 tall**, x 5 to 16, y 6.1 to 8.1. Its top is **1 unit below** the receiver's top; its bottom is level with the receiver's bottom.
- **Ramp wedge:** a 3 wide, 3 long, 1 thick plate tilted **20 degrees the other way** (its rear end lower), x 13.2 to 16.3, y 5.1 to 7.1, lying on the bar's front end. It ramps the top line from the bar (y 6.1 at x 13.2) up to the receiver top (y 5.1 at x 16).
- **Butt pad:** a plain vertical plate **3 wide, 2 thick, 10 tall**, x 0 to 2, y 7.9 to 17.9. Square, no curve, no rubber.
- **Diagonal strut:** a thin plate **1 wide (Z), 2 thick, 10 long**, tilted **20 degrees**, hinge at its top-front corner **(6.9, 7.0)**, going down-back to (3.5, 16.4). Rear edge from (5.0, 6.3) to (1.6, 15.7). It rises into the stock bar and ends in the butt pad. The thinnest part of the gun.
- **Upper cross bar:** a 2x2 square bar **4 long** turned 45 degrees about its long axis (diamond, about 2.8 across), x 2 to 6, centered at y 9.9. It joins the pad's upper part to the strut, crossing the strut.
- **Lower cross block:** a 2x2x2 cube turned 45 degrees about the X axis (diamond, about 2.8 across, 2 long), x 2 to 4, centered at y 15.4, on the pad's front face near the bottom, just ahead of the strut's end.
- The side view of the stock therefore has **a triangular window** (bounded by the stock bar above, the pad behind and the strut diagonal), with one diamond bar crossing its upper part and a short stub near its bottom.

## 4. Silhouette checks

- **Side (the main view).** From the left: a vertical rectangle (the butt pad, 2 wide, 10 tall) at x 0 to 2; a horizontal bar leaving its top (the stock bar) ending at the receiver; between them a **triangular open window** with a thin diagonal strut and two small diamond blocks in it. Then the receiver as a long dark rectangle (25 by 3) with a rail strip above it (1 high) and a boxy hood on top toward the middle (the sight). Under the receiver: a short raked grip (leaning back), a **narrow raked slot** with a small trigger in it, then the tan, parallelogram-shaped lower block with a flat floor. Below the floor, a narrow magazine continues the lower block's slanted front line to its tip at y 27.4. Further forward: the flat-fronted handguard, a short vertical foregrip, then the muzzle stub, thin step then fat step. Negative space: the stock window; the trigger slot (3.6 wide, about 8 deep); the wedge of air between the pistol grip's bottom (y 17.4) and the magazine (the magazine hangs about 10 lower); the open space under the handguard between the lower block's slanted front edge and the foregrip (about 2 units at the top, widening to about 7 at the foregrip's lower end).
- **Top.** A very narrow, long strip: the stock bar (3), then the receiver (3.2) all along, with the 2-wide rail down its center and the 3.2-wide roof plate crossing the rail near the middle. The muzzle diamond (2.0) pokes out the front. The handguard, foregrip, magazine and grip are hidden below. Nothing is wider than 3.2.
- **Front.** A tall, thin column about 3.2 wide: the roof plate on top with the red dot visible through the hood channel, the receiver and handguard stacked as a 3.2 by 6 block with the small diamond muzzle (2.0) in the lower part of that face, the foregrip diamond (2.8 across) beneath, and behind it the lower block and magazine (2 wide).
- **Back.** The butt pad (3 wide, 10 tall) in front of everything, with the hood and rail peeking above the stock bar and the red dot facing the shooter.
- **Underside.** A strip: the pad, the stock bar, the grip bottom (3 wide), the guard floor, the floor plates (3 wide, 10 long) with the magazine's 2-wide bottom, the foregrip's diamond bottom ahead of them, and the muzzle stub.

## 5. Materials and palette

The original texture has two color schemes: the **default camo** (tan lower parts over dark gray upper parts) and **Black** (all dark). Colors below were sampled from the original's regions; the texture is at twice the model's layout resolution and has noise of about 2 to 3 levels per pixel.

| Part | Base | Highlight | Shadow / detail |
|---|---|---|---|
| Lower parts, default camo: slabs, pistol grip, floor plates, guard floor, handguard | #A7A76D | #A9A970 (top faces and edges), #A8A86F | #8E8E5D to #959561 (stipple, edges); recesses #6D6D47; small gray marks #7F7F7F |
| The same parts, Black variant | #2E2E2E | #2F2F2F | #282828 to #2C2C2C |
| Upper receiver, top rail, stock bar, strut, pad, cross bars, red-dot base | #262626 | #2A2A2A | #1C1C1C to #242424 |
| Rail slots, ejection port | #000000 (slots), #040404 (port) | | rail ribs between slots #252525 |
| Hood walls and roof, barrel, muzzle collar, magazine, foregrip, foregrip mount plate, trigger | #343434 | #373737 to #3B3B3B | #313131; foregrip grooves #262626 |
| Red dot | #FF0000 | | none, flat pure red |
| Small pin dots on the receiver (a few, 0.5 unit each) | #7F7F7F | | |

The Black variant changes **only the tan parts**; every dark part is identical in both variants. In the default camo, the whole gun averages about #585842.

Shading style:
- **Flat, matte, low-contrast, per-pixel noise only.** No gradients, no baked shadows, no highlights beyond the 2 to 3 level noise. The tan is a **muted olive-khaki** (slightly green), not a bright yellow or an orange-brown. The receiver and stock (#262626) are a half-step darker than the sight parts, barrel, magazine and foregrip (#343434).
- **Painted details only, no geometry:**
  - Receiver flank: one black rectangle (the ejection port, about 3.5 long and 2.5 tall, near the middle of the flank, a bit toward the rear of center) and one or two tiny gray dots.
  - Handguard flank: a small gray rectangle with a dark outline (about 4 by 1.5) near the front, and a few dots.
  - Rear slab flank: four dimple screws (2 by 2, spaced about 2 units, #6D6D47 on the tan).
  - Front slab flank: two tall rounded recesses (#6D6D47) and a column of 3 or 4 short horizontal rib lines beside them.
  - Pistol grip flank: two stippled panels (alternating #A7A76D and #8E8E5D in a fine checker), a column of horizontal grip lines, and one small oval recess near the top.
  - Rails: alternating black and dark gray slot pattern, pitch 1 unit, along the top of both rails.
  - Foregrip: horizontal grooves every unit, #262626 on #333333.
  - Sight base: a few gray (#7F7F7F) marks for the lens and housing, a few pixels.
- **Camo count: exactly two variants** exist (default camo and Black).

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up). Same family as the M4, AK-47 and SCAR. **Total length in game: 2.75 blocks at scale 1.0; 3.85 blocks at the first-person scale of 1.4.**

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person (idle) | yaw 225 degrees (after the 180 degree flip), scale 1.4 | offset about (-0.65, -0.55, -0.65). The camera sees the gun from behind and slightly to the side, with the sight and the pad most visible. |
| First person firing | pitch of **at most 5 degrees** (muzzle up) plus a shove back of **at most 0.15 block** along the gun axis | smooth single-bump curve; very small. The delay between shots is 2 ticks, so it is nearly continuous. |
| First person reloading | tips by **up to 50 degrees** about the lateral axis at mid-reload, then eases back | smooth rise and fall over the 40-tick (2.0 s) reload; offset unchanged. |
| First person aimed | no scope overlay; the FOV narrows (0.75) | the red dot is the sight; the model stays drawn. |
| Third person (held) | yaw 135 degrees, scale 0.8 | offset about (0.1, -0.7, 1.1). The grip sits in the hand and the muzzle points forward. |
| Third person aimed | extra offset (0, 0.1, 0) | |
| Ground (dropped) | scale 0.6 | lowered by 0.45 so it lies on the ground. |
| Inventory icon | yaw 90 degrees, scale 0.75 | offset about (0.3, -0.7, -0.6); drawn side-on, filling most of the slot. No sprite; the icon is the 3D model. |

**Muzzle flash point:** the front of the muzzle collar on its axis, model position (44, 9.3, 0), a flash sprite at 1.25 times scale. In third person the flash is drawn at about (0, 0.05, 0) from the hand-relative origin, lasting 5 ticks.

**No akimbo variant.** One gun, one hand.

## 7. Motion

- The **model has no moving parts.** The trigger and magazine never move; all animation is whole-gun transform.
- **Recoil (first person):** a tiny 5-degree muzzle-up kick and a 0.15-block shove back, then back to rest, repeated at a 2-tick cadence while the trigger is held.
- **Recoil (third person):** the gun pitches up about 15 degrees around the wrist in the first quarter of the first half of the animation, then settles back.
- **Reload:** 40 ticks. First person: the gun tilts about 50 degrees at mid-reload and returns. Third person: it tilts about 30 degrees and turns 40 degrees about a wrist pivot slightly above and ahead of the model's origin at mid-animation, then returns.
- **Optional (not in the original):** the builder may keep the magazine, trigger and stock as separate named parts so later code can animate them; the rest pose must match this document.

## 8. What to avoid

- **Do not give it a long barrel.** The stub is only 3 units beyond the flat-fronted handguard (2 thin plus 1 fat). A long barrel or a suppressor wrecks the look.
- **Do not build a vertical grip and a straight magazine.** The grip, both slabs and the magazine are tilted 20 degrees as a family. The front line from the receiver to the magazine tip must be one continuous slanted line.
- **Do not fill the trigger slot.** It must read as a see-through slot with a small trigger in it.
- **Do not make the stock a solid block.** It is a flat bar, a pad, a thin diagonal strut and two diamond cross bars, with an open triangular window.
- **Do not put the magazine under the pistol grip or the middle of the floor.** It sits at the **front** of the floor and hangs about 9 units below it, forward of the pistol grip.
- **Do not make the gun thick.** The thickness is 3 to 3.2 almost everywhere; the magazine is 2 and the strut 1.
- **Do not paint the tan bright yellow, and do not paint the receiver tan.** The tan is a muted olive-khaki (#A7A76D) and covers only the lower parts and handguard.
- **Do not give the red dot a lens tube.** It is a short box tunnel with a roof and a single red square.
- Do not add things the original lacks: no suppressor, no flash hider, no charging handle, no rear iron sight, no side rail, no sling mount, no cartridge window in the magazine.

## 9. Acceptance checklist

- [ ] Length : height : thickness is within about 10 percent of 13.8 : 8.6 : 1 (44 : 27.4 : 3.2).
- [ ] The gun is 3.2 wide at most; the magazine is 2 wide; the stock strut is 1 wide.
- [ ] Barrel stub: a thin diamond (about 1.4 across, 2 long) followed by a fat diamond (about 2.0 across, 1 long); nothing longer; the handguard's front face is flush with the receiver's front face.
- [ ] Upper receiver is 25 long, 3.2 wide, 3 tall, dark gray (#262626 family), with a 2-wide, 1-tall rail on top inset 1 from each end, with black slots.
- [ ] Red-dot sight: a 7.2-long base, two 0.8-wide walls 3.2 long, a 3.2 by 3.2 roof; the highest point of the gun; a single red (#FF0000) 1 by 1 plane in the open channel.
- [ ] Lower block is two 20-degree slabs (5 deep, 11 tall each, 5 apart); its front edge is one straight line continuing into the magazine's front edge.
- [ ] The magazine is 2 wide, tilted 20 degrees, bottom about 9 below the floor, emerging at the front of the floor.
- [ ] Pistol grip is tilted 20 degrees, parallel to the lower block, with a 3.6-unit slot between them and a small dark trigger (leaning forward 10 degrees) in the slot.
- [ ] The slot is open (see-through) in the side view; its bottom is closed by the guard floor.
- [ ] Floor is flat (y 18.5), 10 long, made of two 1-thick plates; the grip's bottom is about 10 higher than the magazine's bottom.
- [ ] Stock: flat top bar 1 below the receiver top (with the small ramp wedge), a 2-thick, 10-tall butt pad, one thin diagonal strut at 20 degrees, two diamond-section cross bars; an open triangular window in the side view.
- [ ] Foregrip: a 7-tall diamond-section column (about 2.8 across) hanging vertically, centered at x = 37.5, on a flat mount plate under a 7-long bottom rail.
- [ ] Default camo: tan (#A7A76D) lower block, grip, floor plates, handguard; everything else dark (#262626 to #343434). Black variant: the tan parts become #2E2E2E and nothing else changes.
- [ ] Painted details present: dimples on the rear slab, ovals on the front slab, stipple on the grip, ejection port on the receiver, slots on both rails.
- [ ] Side silhouette matches section 4 (stock window, parallelogram lower block, magazine continuing the front line); no part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Receiver port flank and position.** I could not determine which flank carries the black port, or exactly where along the receiver. Painting both flanks at about 55 to 60 percent of the way back from the front is a safe choice.
- **Trigger position.** By the numbers the trigger sits against the grip's front edge (about 2.2 empty in front of it). Keep it there; a trigger centered in the slot is a visible deviation.
- **"Camo" is not a pattern.** The default texture is called "Default Camo" but is two flat solid colors. A real camouflage pattern would be a deviation.
- **First-person framing numbers** come from formulas read, not seen in game. Treat the offsets as approximate.
- **Foregrip rotation.** The foregrip is turned 45 degrees about its vertical axis, so it shows a diamond from the front and top; from the side its width is the diamond's diagonal (about 2.8), not 2. Its center is within 0.1 unit of the mid-plane.
- **Recess shadow color.** #6D6D47 is sampled from a few pixel clusters; treat it as approximate.

## Round 2 corrections (orchestrator review)

Round 1 comparison (side 0.965, iso 0.95, front 0.91, **top 0.805**, below the 0.93 bar).

- **Width (the cause of the top and bottom error).** The original is **4 units wide (0.25 block)** at its widest, across the upper receiver and the tan slabs together. Ours is 3 units (0.188 block). Widen the upper receiver so the whole gun is 4 across, with the top rail and sight staying where they are. The grip, magazine and foregrip stay narrower than the body (the originals are about 3 units).
- **Tan slabs.** In the original the two tan slabs sit **flush against the receiver sides** and are flat: seen from the right the lower slab is one clean rhombus outline with a long recessed slot window near its middle and a checkered stipple band along its lower rear edge. Ours are tilted so they poke out of the body in the iso views, and carry scattered square dots and bars instead of the slot. Make the slabs flat and flush, give each one long slot window plus the checker band, and remove the scattered dots.
- **Rear stock.** The original's stock is a flat black bracket whose lower hook reaches down to about the grip's height with a notch; the shape reads the same, but the original's hook is narrower (about 3 units wide) than ours.
- **Top rail.** The original's rail teeth are fine and low; ours are chunkier in the iso view. Keep them as flat 1-unit steps without raised blocks.
- Keep the black sight with the red dot and the 2.75-block length.

## Round 3 corrections (measured)

Round 3 probe of the master model against the original: side 0.965, iso 0.952 and 0.947, front 0.910, back 0.911, **top and bottom 0.805**. Total length (44), tilts, the slab pair, grip, magazine, floor plates, stock, foregrip, rails, handguard and barrel already match the original to within 0.3 unit in every axis. The remaining error is almost all **width and the sight**. Measurements are in this page's frame (X from the butt-pad back, Y down from the roof-plate top, Z from the mid-plane). One global note: in the original, everything except the roof plate sits **0.1 lower** than the Y values in sections 2 and 3 (receiver top 5.2, receiver bottom 8.2, stock bar top 6.2, handguard 8.2 to 11.2, floor 18.6, magazine bottom 27.5). Apply +0.1 to every Y above except the roof plate; the roof plate stays at Y 0 to 1.

1. **Upper receiver width (top, bottom, front, back views).** Ours is 3.0 wide (z -1.5 to 1.5). Target **4.0 wide (z -2.0 to 2.0)**, same X 16 to 41, same 3 tall (Y 5.2 to 8.2). This is the only 4-wide part besides the sight. Everything under it (handguard, slabs, grip, floor plates, stock bar, pad) stays **3.0 wide (z -1.5 to 1.5)**, so the receiver overhangs each side by 0.5. The rail stays 2 wide (z -1 to 1). Round 2's statement that the tan slabs are also 4 wide is wrong; the slabs are 3.
2. **Red-dot base.** Ours is 7.0 to 7.2 long, 3 wide, 2.5 tall. Target: **9.0 long (X 23.5 to 32.5), 4.0 wide (z -2.0 to 2.0), 3.0 tall (Y 2.2 to 5.2)**. Its bottom 0 units overlap the rail: it sits directly on the receiver top (Y 5.2) and covers the rail's X range there. Dark gray (#262626 family).
3. **Hood roof plate.** Ours is 3.0 long, 3.0 wide, 1.0 thick. Target **4.0 long (X 24.6 to 28.6), 4.0 wide (z -2.0 to 2.0), 1.0 thick (Y 0 to 1.0)**. It overhangs the walls by 0.3 on each side.
4. **Hood walls.** Ours are 3.0 long, 1.0 wide, 1.5 tall at z 0.5 to 1.5. Target: **4.0 long (X 24.6 to 28.6), 1.0 wide, 2.0 tall (Y 0.7 to 2.7)**, at **z -1.7 to -0.7 and z 0.7 to 1.7**. The channel between them is **1.4 wide** (not 1.0), open at both ends. The base top is Y 2.2 and the wall bottoms are at Y 2.7, so the lower 0.5 of each wall overlaps the base. Colors #343434.
5. **Red dot.** Position matches (X 26.0, 1 by 1 plane, z -0.5 to 0.5); move it to **Y 1.1 to 2.1**.
6. **Muzzle collar.** Ours is squashed to 2.2 across and 2.2 tall (Y 8.2 to 10.4 in ours). Target **2.8 across in both Z and Y (z -1.4 to 1.4, Y 8.0 to 10.8)**, X 43 to 44. That means the 70 percent squash in section 3 is wrong: the collar is a full 2 by 2 bar turned 45 degrees, **2.8 diamond**. The barrel behind it is right (1.4 across, Y 8.7 to 10.1, X 41 to 43). Affects front view (visible ring) and top view (muzzle width).
7. **Foregrip lateral position.** Ours is centered (z -1.4 to 1.4). Target **z -1.3 to 1.5**, i.e. its center is 0.1 toward +Z; X 36.1 to 38.9 and length 7 are right (Y 12.2 to 19.2).
8. **Trigger.** Ours is X 21.3 to 22.8; target **X 21.0 to 22.5** (0.3 farther forward-back toward the rear), Y 7.7 to 10.8, z -0.5 to 0.5.
9. **Butt pad.** Y 8.0 to 18.0 (10 tall), X 0 to 2, z -1.5 to 1.5; ours is 0.1 higher. Stock cross bars: upper diamond X 2 to 6, Y 8.6 to 11.4, z -1.4 to 1.4; lower diamond X 2 to 4, Y 14.1 to 16.9, z -1.4 to 1.4.

Resulting widths along the top view, rear to front: pad, stock bar 3.0; receiver 4.0 with the 2.0 rail down its center; base and roof 4.0 across X 23.5 to 32.5 and 24.6 to 28.6; handguard hidden; muzzle diamond 2.8 at the very front. Palette is unchanged from section 5 (the 4-wide receiver flanks keep #262626; paint the ejection port across the full 3-unit height).
