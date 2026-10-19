# Combat Shotgun: clean-room model notes

Design notes for rebuilding the Techguns **Combat Shotgun** look-alike in GregGuns (gun name `combat_shotgun`). Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelCombatShotgun](../gun-models.md#modelcombatshotgun-6432-family-x-20-boxes-2-parts), [combat shotgun sheet](../gun-sheets.md#combat-shotgun-combatshotgun), [rendering framework](../rendering-framework.md), [Techguns combat shotgun stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = one texel at the native 64x32 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention), with **Y = 0 at the tops of the two sights**, the highest points of the gun. **X = 0 is the back face of the main receiver**, which is also the front face of the rear receiver. **Z = 0 is the gun's mid-plane**; sizes across Z are given as total width, centered. Rounded to the nearest half unit unless it matters. Angles are the lean of a part seen from the gun's right-hand side (muzzle to the right, up on the page).

## 1. Gestalt

A long, dark, **military pump-action shotgun with a skeleton folding stock**. At a glance you read: a slim, lightish-gray barrel assembly that runs far forward of the body; a **fat black fore-end slab** (the pump) wrapped around the barrel and the tube beneath it; a squat black receiver; a **steeply raked black pistol grip**; and, at the back, a **boxy black butt block held away from the receiver by two thin struts** with a slot of air between them. The gun is long and low: about 14 times as long as it is thick. It is a tactical "breacher" shotgun, with nothing wooden, nothing shiny and nothing ornamental.

Defining features in order of visual importance:

1. **Two-tone split:** a light steel-gray barrel, tube and struts against black polymer-looking receiver, stock, grip and pump. The pump is the biggest dark mass.
2. **The pump (fore-end):** a thick 12-long block, wider and taller than the receiver, wrapped around the barrel's lower half and the tube. It is a separate moving part.
3. **Barrel assembly:** a 24-long barrel that is plus-shaped in section, with a thin magazine tube hung beneath it and a small clamp tying the two together.
4. **Skeleton stock:** a tall butt block at the very back, a hinge block, a thick lower strut and a thin upper strut, both rising toward the receiver and closing the wedge of negative space between them.
5. **Raked pistol grip** leaning back about 19 degrees, with a small stub of a trigger and **no trigger guard**.
6. Two tiny matching sights, one on the rear receiver and one at the muzzle.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt plate to muzzle) | 55 (X from -21 to 34) |
| Total height (sight tops to grip bottom) | about 12.6 |
| Total thickness (pump width) | 4 (everything else is 3 or less) |
| Length : height : thickness | about 13.8 : 3.2 : 1 |
| Barrel assembly | 24 long, **about 44 percent** of the total length, all of it in front of the receiver |
| Receivers (rear 8 plus main 10) | 18 long, about 33 percent |
| Stock (butt plate to the rear receiver's back face) | 13 long, about 24 percent |
| Pump | 12 long, 5 tall, 4 wide; half the length of the barrel |
| Main receiver | 10 long, 5 tall, 3 wide |
| Rear receiver | 8 long, 4 tall, 3 wide |
| Grip | 7 long along its lean, 3 front-to-back, 2 wide; its bottom hangs about 6 below the receiver |

How the pieces sit along the length, from the rear: the butt plate and toe (X -21 to -19); the hinge block (-19 to -17); open air with two struts crossing it (-17 to -8); the rear receiver (-8 to 0); the main receiver (0 to 10), with the pump starting 1 unit inside its front face (9); the pump to X 21; barrel and tube continuing to the clamp (29 to 31), the tube end (32) and the muzzle (34).

The **top line steps**: the rear receiver's top plate is at Y 1, the main receiver's top plate is a half unit higher at Y 0.5, and the barrel's top is back at Y 1. Both sights stand up to Y 0, so they sit at exactly the same height and the sight line is level. The bottom line steps similarly: the rear receiver's bottom is at Y 6, the main receiver's bottom and the tube's bottom are both at Y 6.5, and the pump hangs 1 unit lower at Y 7.5.

## 3. Part by part

All parts are boxes. Z widths are centered on the mid-plane. "Touches" means faces in contact or overlapping.

### Barrel assembly
- **Center slab:** 24 long (X 10 to 34), **3 tall** (Y 1 to 4), **1 wide**.
- **Two side rails:** each 24 long, **1 tall** (Y 2 to 3, the middle third of the slab's height), 1 wide, one on each side of the slab, flush against it. Together slab and rails make a **plus-shaped section**, 3 wide and 3 tall overall, with the corners cut away. From the side it reads as one smooth 3-tall bar with a faint horizontal line (the rails stand proud). From the front and above you see the plus.
- Starts at the main receiver's front face and runs to the muzzle. No taper, no muzzle brake, no choke ring, no heat shield, no vent holes.
- The pump covers its lower half over X 10 to 21; the barrel is fully visible in front of the pump.

### Magazine tube
- A 2x2 bar, **22 long** (X 10 to 32), centered under the barrel (Y 4.5 to 6.5). It ends 2 units **short of the muzzle**.
- A **half-unit gap** separates its top from the barrel's bottom (Y 4 to 4.5). From the side that gap is a hairline of background; the clamp interrupts it.
- Its bottom is flush with the main receiver's bottom (Y 6.5). It starts at the receiver's front face, hidden by the pump for the first 11 units.

### Clamp
- A tiny block 2 long, 1 tall, 1 wide at **X 29 to 31, Y 4 to 5**. It straddles the half-unit gap: its top overlaps the barrel's bottom and its bottom overlaps the tube's top. It sits 1 unit back from the tube's end.
- It is the only thing that visibly joins the tube to the barrel in front of the pump. Do not replace it with a collar or a ring.

### Front sight
- A 2 long, 1 tall, 1 wide block at X 31 to 33, Y 0 to 1, sitting on the barrel's top and ending **1 unit before the muzzle**. Plain: no hood, no post-and-ring.
- It stands a full unit above the barrel's top.

### Rear sight
- An identical 2x1x1 block at X -5.5 to -3.5, Y 0 to 1, on the rear receiver's top plate. Plain block with no notch or aperture.
- The two sights are the **same height** and the same size, so the top of the gun has a matched pair of nubs.

### Main receiver
- A box **10 long, 5 tall, 3 wide** (X 0 to 10, Y 1.5 to 6.5).
- On top, a **top plate** 10 long, 1 tall, **2 wide** (Y 0.5 to 1.5), flush with the receiver's ends and 0.5 narrower on each side. Together they read as a 3-wide body with a 2-wide ridge.
- It is the tallest part of the body, and its front face is where the barrel assembly emerges.
- No ejection port, no cut-outs, no loading gate in the geometry. Any detail is painted.

### Rear receiver
- A box **8 long, 4 tall, 3 wide** (X -8 to 0, Y 2 to 6), directly behind the main receiver and sharing its width.
- A top plate **7 long, 1 tall, 2 wide** (X -7 to 0, Y 1 to 2) on top of it, starting 1 unit in from the back face.
- It is **0.5 shorter than the main receiver at both top and bottom**: the profile steps down by half a unit at X = 0 on the top and on the bottom. The back of the rear receiver is a flat wall where the struts, the grip and the sight meet it.

### Pump (separate part)
- A single block **12 long (X 9 to 21), 5 tall (Y 2.5 to 7.5), 4 wide**. It is the **widest** thing on the gun by 1 unit and the only part more than 3 wide.
- It surrounds the barrel's lower portion and the tube. It extends 1 unit past the receiver's bottom and 1 unit past the tube's bottom, and it overhangs the receiver by half a unit on each side. Its back 1 unit overlaps the receiver's front, so there is no visible gap in the rest pose.
- The barrel's center slab stands about 1.5 units above its top (Y 1 to 2.5). The front end of the pump is a blunt vertical wall, with no flare or finger cuts in the geometry. The grip ribs are painted (section 5).
- Must be built as **its own part** so code can slide it along X.

### Pistol grip
- A box **3 wide in the side plane, 7 long, 2 wide across Z**, leaning back about **19 degrees** from vertical: the bottom is about 2.3 farther back than the top.
- Its rear-top corner is at about X -7, Y 5, inside the rear receiver; its front-top corner about X -4.2, Y 6; its bottom corners about X -9.3 (rear) at Y 11.6 and X -6.5 (front) at Y 12.6. The bottom face is therefore slightly tilted.
- The top is buried in the lower part of the rear receiver (the front corner just touches the receiver's bottom). It sits under the rear receiver's middle (X -8 to -4), well behind the main receiver. The lowest point of the whole gun is the grip's front-bottom corner (Y 12.6), with the stock's toe (Y 11.5) about 1 unit above it.
- No finger grooves or backstrap; a plain slab.

### Trigger
- A small block **2 long, 1 tall, 1 wide** at X -5 to -3, Y 6 to 7, immediately under the rear receiver and overlapping the grip's front-top edge by about half a unit. It protrudes about 1.5 units forward of the grip.
- **There is no trigger guard.** The trigger is the only thing under the receiver in front of the grip.

### Skeleton stock
- **Butt plate:** a block 2 long, 5 tall, 3 wide at X -21 to -19, Y 3.5 to 8.5.
- **Toe:** 2 long, 3 tall, **2 wide** at X -21 to -19, Y 8.5 to 11.5, hanging directly below the butt plate. From behind the butt is an L or boot shape: 3 wide on top, 2 wide below.
- **Hinge block:** 2 long, 5 tall, 3 wide at X -19 to -17, Y 3 to 8, directly in front of the butt plate. From the side the butt plate and hinge form a 4-long, 5-tall block (X -21 to -17) with a toe hanging down the back.
- **Lower brace:** a 1 long, 2 tall, 2 wide block at X -19 to -18, Y 8 to 10, under the hinge block's rear half and against the toe's front. It makes the stock's bottom look stepped.
- **Lower strut:** a **12 long, 2 tall, 2 wide** bar that leaves the hinge block's lower middle at about Y 6 (top edge) and **rises toward the front at 15 degrees**, ending in the rear receiver's back wall at about X -7, with its top at about Y 2.9 at the end and its bottom at about Y 4.8. It is buried about 1 unit into the rear receiver.
- **Upper strut:** a **10 long, 1 tall, 1 wide** rod that leaves the hinge block's front half at about Y 3.5 and **rises toward the front at 8 degrees**, ending at the rear receiver's back wall at about X -7.5, Y 2 to 3. It runs above the lower strut and reads as a thin rod over a thick bar.
- **Negative space between the struts:** a thin wedge of air, about 1 unit tall at the hinge, closing to nothing at about X -12, so the struts read as nearly touching in the front third. Below the lower strut is a large open pocket (about 9 long by 4 to 5 tall) bounded by the toe at the back and the grip at the front.
- Everything in the stock is rigid; it never folds in the game.

## 4. Silhouette checks

- **Side (the main view):** from the left: a tall black stock block with a toe at the bottom; two thin struts rising to the receiver, leaving a thin sliver of air between them and a big pocket under; the squat receiver with its small step in the top and bottom lines and the matched sights; the steeply raked grip dropping to the lowest point; the small trigger stub forward of it; the fat pump block under the barrel; then the lighter barrel and tube, with the hairline gap between them, ending in a front sight and a flat muzzle that extends 2 units past the tube. Negative space: the pocket under the stock, the wedge between the struts, the open gap under the receiver between the trigger and the pump (no guard), and the hairline between barrel and tube.
- **Top:** a thin spine, with the 3-wide barrel-and-rail strip over the front half, the sights as 1-wide dots at both ends, a **4-wide bulge** in the front-middle (the pump), the 3-wide receivers with their lighter 2-wide plates, and the stock narrowing: the thin upper strut (1 wide) centered above the 2-wide lower strut, and the butt and hinge blocks 3 wide.
- **Front:** a stack. At the top a plus-shaped barrel (3x3, with cut corners) with the sight above it; below it the tube (2x2); both in front of a big 4-wide by 5-tall pump face. Behind that you see the 3-wide receiver edges, and below hangs the 2-wide grip. The stock is hidden behind.
- **Back:** a 3-wide by 5-tall butt plate with the 2-wide toe below it, the receiver and pump showing around it, and the 2-wide raked grip beyond.
- **Underside:** a long strip: the pump's 4-wide bulge, the tube, the 2-wide grip, the stub trigger and, at the back, the toe and lower brace.

## 5. Materials and palette

The original texture is **grayscale** (no hue at all), noisy and low contrast, with long horizontal banding on the light metal and darker mottling on the black parts. Values below were sampled from its regions (averages, with the observed range).

| Part | Material | Base | Range / highlight | Notes |
|---|---|---|---|---|
| Barrel, rails | mid-light gunmetal | #68696A | #535454 to #838485 | horizontal bands; rails slightly lighter, about #6E7070 |
| Magazine tube | same gunmetal | #676969 | #4B4C4C to #838486 | |
| Lower strut | gunmetal | #646666 | #484848 to #8D9090 | |
| Upper strut | lighter gunmetal | #727474 | #5D5F5F to #8B8C8D | the lightest large surface |
| Main receiver body | black polymer | #363636 | #272727 to #414141 | mottled; some near-black (#1C1C1C) patches |
| Receiver top plates | dark gray | #373737 | #181818 to #464646 | very slightly lighter than the receiver sides |
| Rear receiver | very dark gray | #2C2C2C | #1B1B1B to #414141 | |
| Pump | black polymer | #383838 to #3E3E3E | #101010 to #636464 | **vertical grip ribs**, dark grooves between lighter bands |
| Pistol grip | near-black | #232323 | #1C1C1C to #464646 | nearly uniform |
| Butt plate, toe, hinge block, brace | near-black | #1F1F1F | #191919 to #323232 | the darkest part of the gun |
| Sights, clamp, trigger | dark gray | #323232 | #303030 to #3F3F3F | |

Shading style:
- **Two value groups.** The barrel, tube and struts are mid-gray (#64 to #73); the rest is very dark (#1F to #3E). There is no color tint and no warm or cool shift. Nothing is white, brown or colored.
- **Metal parts:** satin, horizontal streaks, a few levels of per-pixel noise; lighter along top edges, a little darker underneath. No gloss, no scratches, no rust.
- **Black parts:** subtle mottled noise, with some slightly lighter patches (#40 to #46) on top faces and edges. The pump's side faces carry the clearest pattern: **vertical ribs** about 1 texel wide, 2 or 3 texels apart.
- No decals, logos, text or warning stripes, and no camo. The original has a single texture and **no camo variants**.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up). The gun is **big** in the first-person frame.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 2.25 | idle offset about (0.1, -0.25, -0.5). The camera sees the gun turned 45 degrees, side plus rear. At this scale the gun fills the right side of the view and the stock runs off-screen. |
| First person firing | first quarter: a kick back of about 0.6 block with about 10 degrees of roll about Z; then the **rechamber** | the rechamber is a smooth bump over the remaining 75 percent: roll 8 degrees, yaw -18 degrees, pitch -6 degrees at the peak. The whole gun moves about 0.5 block along its axis while the **pump moves about 0.1 block the other way**, so the pump racks about 0.6 block (about 9 model units) back relative to the gun. |
| First person reloading | first 80 percent: roll 10, yaw -50, pitch -10 degrees at the peak, and a shift of about (1.0, 0.5, 0.25) block at the peak | the gun turns so the receiver's side faces the camera, as if loading shells through the port. The last 20 percent repeats the pump-rack motion. |
| First person aimed | no scope and no zoom overlay | the aimed pose adds only a small shift (0, 0.25, -0.25). |
| Third person (held) | yaw 225 degrees, scale 1.15 | offset (-0.8, -0.4, 0). The grip sits in the hand and the muzzle points forward. |
| Third person recoil | rolls about 20 degrees about Z in the first quarter of the first half, then eases back | in the second half the **pump slides back about 0.45 block** and returns. |
| Third person reload | pitch 30 degrees and yaw 40 degrees at mid-reload | near the end the pump is pulled back about 0.5 to 0.6 block. |
| Ground (dropped) | yaw 90 degrees, scale 0.75 | lowered by 0.3 block so it lies on the ground. |
| Inventory icon | yaw 180 degrees, scale 0.6 | offset (-0.3, 0.1, 0). Drawn side-on from the model, with no sprite. The whole 55-unit length fits the slot, so the stock and barrel must read at tiny size: keep the two-tone split. |

**Muzzle flash point:** the front of the barrel (model X about 34, Y about 2.5 on the barrel's axis, Z = 0); sprite scale 1.85. The original draws it inside the pump's transform, so the flash follows the pump during the rechamber. The offsets in the renderer are in a rotated frame and are only approximate.

Gameplay data that affects the look: 8 shells per reload, fire delay 12 ticks (the recoil-then-pump animation runs the whole 12 ticks), muzzle flash 5 ticks, reload 50 ticks, pump sound after every shot.

## 7. Motion

- The model has **two parts**: part 0 is everything except the pump; part 1 is the pump. Only the pump has its own motion.
- **Pump:** slides along X, toward the rear, during the rechamber after every shot and at the end of a reload. Travel is about 7 to 10 model units (0.45 to 0.6 block, from the third-person and first-person transforms respectively). It does not rotate. Rest pose: X 9 to 21. At full travel the pump covers about X 0 to 12 and so overlaps the front of the main receiver and the barrel's first units. This is expected, and the pump's 4-wide body wraps the 3-wide receiver. Do not shorten it or hollow it out.
- **Everything else is rigid:** the stock does not fold, no hammer or bolt moves, and no shells are shown.
- **Whole-gun transforms:** recoil kick and the rechamber nod (first person), a tilt about the wrist (third person), and a turn toward the camera for the reload (first person). See section 6.
- **Optional (not in the original):** a builder may keep the grip, trigger and stock as named parts, but the rest pose must match this document. The pump must be separate.

## 8. What to avoid

- **A pump the same width as the receiver.** The pump is 4 wide against the receiver's 3, and its half-unit overhang on each side is part of the look.
- **A pump that does not reach below the tube.** It hangs 1 unit under both the tube and the receiver, which makes the underside a stepped line.
- **A plain round or square barrel.** Use the 3x3 plus-shaped section with its horizontal rails; from the side it must still read as one smooth bar.
- **A tube that runs to the muzzle.** It stops 2 units short; the clamp is 1 unit back from its end.
- **A solid stock.** The stock is a skeleton: butt block, hinge block, two struts, a big air pocket under, a thin wedge between. Do not fill it with a slab.
- **A trigger guard.** There is none; only a stub trigger.
- **An upright grip.** The grip leans back about 19 degrees. It must be a clear diagonal, not a vertical slab.
- **Wood, brown or colored parts.** The whole palette is gray and black. No wooden furniture, orange pump grips or tinted metal.
- **Different sight heights or elaborate sights.** The two sights are identical blocks at the same top height.
- **A rail, optic mount, bayonet or heat shield.** The original has none; the receiver top is a smooth 2-wide plate.

## 9. Acceptance checklist

- [ ] Length : height : thickness is within about 10 percent of 13.8 : 3.2 : 1 (55 : 12.6 : 4).
- [ ] The barrel assembly is about 44 percent of the total length (24 of 55) and sits entirely in front of the receiver.
- [ ] The barrel has a plus-shaped section (a 3-tall center slab with two 1-tall side rails), 3 wide overall, and reads as a single smooth bar from the side.
- [ ] The magazine tube is a 2x2 bar under the barrel with a half-unit gap, ends about 2 units short of the muzzle, and has a small clamp near its front.
- [ ] The pump is a separate part, 12 long, 5 tall and **4 wide**, the widest part of the gun, extends about 1 unit below the tube and the receiver, and overlaps 1 unit of the receiver's front in the rest pose.
- [ ] The receiver is two in-line boxes: the 10-long main (5 tall) and the 8-long rear (4 tall), both 3 wide, each with a 2-wide top plate, with the main one a half unit taller at top and bottom.
- [ ] Both sights are identical small blocks at the same top height, one on the rear receiver and one just behind the muzzle; neither has a notch or hood.
- [ ] The pistol grip is a 2-wide slab leaning back about 19 degrees, its top buried in the rear receiver; its bottom is the lowest point of the gun, with the stock toe about 1 unit higher.
- [ ] The trigger is a small stub just forward of the grip, and there is no trigger guard.
- [ ] The stock has a tall butt plate with a narrower toe, a hinge block, a 2x2 lower strut rising at about 15 degrees and a 1x1 upper strut rising at about 8 degrees, both ending buried in the rear receiver's back wall.
- [ ] There is a clear open pocket under the lower strut and a thin, closing wedge of air between the two struts.
- [ ] The palette is grayscale: barrel, tube and struts mid-gray (about #646666 to #727474), the rest near-black (about #1F1F1F to #3E3E3E); no colored or white regions.
- [ ] The pump has vertical ribs painted on its sides.
- [ ] The side silhouette matches section 4 (stock pocket, struts, stepped receiver, raked grip, big pump, then barrel and tube).
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Hidden texture detail.** The original texture has one small, bright patch (nearly white, about 4 by 2 texels) in the receiver's area. It is most likely on a face that the top plate hides, so it is not a visible feature. Ignore it.
- **Rail look.** The rails are flush with the slab's height range, so they only appear as a separate band from the front, above or at an angle. Painting a faint horizontal band on the sides to suggest them is within the original's look.
- **Pump travel numbers.** The first-person and third-person values differ (about 0.6 and 0.45 block). I read these from formulas and have not seen them in game. Treat them as approximate; anything from 7 to 10 units reads right.
- **Muzzle flash offset.** The renderer's numbers are in a rotated frame. The flash on the muzzle's axis is my reading of them.
- **Recoil roll sign.** Whether the first-person kick pitches the muzzle up or down depends on the frame; I read it as muzzle up. Check the feel in game.
- **Lower brace.** The 1-unit brace under the hinge is small and hard to see in any view. It is optional if it complicates the builder's layout, but the toe and butt plate are not.
