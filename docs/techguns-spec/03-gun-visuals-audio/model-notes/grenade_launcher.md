# Grenade Launcher: clean-room model notes

Design notes for rebuilding the Techguns **Grenade Launcher** look-alike in GregGuns, plus the look of its launched grenade. Written by the "dirty room" analyst from study of the original's mesh, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [rendering framework](../rendering-framework.md), [gun models](../gun-models.md), [Techguns grenade launcher stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**The original is a triangle mesh, not a set of boxes.** Most of it is box-like, so our builder will approximate it with boxes. Three bits are not boxes and are called out where they occur: the hexagonal drum, the hexagonal launch tube and its sleeve, and the slanted pistol grip. Everything else is an axis-aligned block.

**Units and axes used in this document.** One unit = 1/16 block. Muzzle toward **+X**. **Y points UP** (this mesh is not Y-down like the box-model guns). **Z = 0 is the gun's mid-plane**; sizes across Z are given as total width, centered. **X = 0 is the center of the drum** (the middle of its length). **Y = 0 is the drum's axis.** Rounded to the nearest half unit unless it matters. The mesh is built in two pieces, a static body and the drum, and the drum is the only moving part.

## 1. Gestalt

A fat, black, **revolver-fed grenade launcher in the style of a six-shot "multiple grenade launcher"**: a long rifle-like black body with a wire stock out the back, a pistol grip hanging under the rear, a stubby gray launch tube out the front, and in the middle a huge **hexagonal drum that is much wider and taller than the body**. It reads as chunky, heavy and military, with a roughly even split between a dark receiver and the bulky drum. The only bright colors are the **red grenades visible in the drum**, the brushed-silver tube sleeve and the white-silver drum rails.

Defining features in order of visual importance:

1. **The drum.** A hexagonal prism standing proud of the body by 5 to 6 units on each side and 8 to 9 above and below the axis line. Its sides show red grenades between silver rails; its ends show six round domed chamber bumps.
2. **A dark, boxy, ridged-top receiver** running back from the drum, topped by a long ribbed rail with a small fork sight at its front end.
3. **A short fat launch tube** with a wider silver sleeve around the rear two thirds, sticking out the front, with a small bar under it.
4. **A thin wire stock**: a slim rod ending in a small end plate, projecting straight back from the receiver.
5. **A pistol grip raked back about 20 degrees**, with a small trigger and a thin bar guard under the receiver.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (stock plate to muzzle) | 44 (X from -26 to +18) |
| Total height (grip bottom or drum bottom to fork-sight tip) | 21 (Y from -8 to +13) |
| Total width (drum rail plates) | about 13.9 |
| Length : height : width | about 3.2 : 1.5 : 1 |
| Stock section (plate to receiver back) | 9 long (X -26 to -17) |
| Receiver (back collar to drum rear plate) | 12 long (X -17 to -5) |
| Drum, with its two end plates | 10 long (X -5 to +5), 16 tall, 13.9 wide |
| Drum body only (the turning part) | 8 long (X -4 to +4), 14 tall, 12.1 wide |
| Forward section (front plate to muzzle) | 13 long (X +5 to +18) |
| Receiver body width | 4 to 5 (so the drum is about 3 times wider) |
| Launch tube | 12 long, about 4.8 across; sleeve 8 long, about 6.5 across |
| Grip | about 6 long along its slant, 3 front-to-back, 2 wide |

Key vertical stations (Y): fork sight tip +13; rail top +11; receiver housing top +10; drum end-plate top vertex +8; receiver main block top +8; tube axis about +4.2; drum axis 0; grip top about +2; underplate bottom +1; guard bar bottom -1.5; grip bottom about -4.6; drum bottom vertex -8.

The **top line** is the ribbed rail from X -12 to +4, with the tiny rear-sight bump and the fork front sight. The tube sits about 4 units above the drum axis, **level with the upper part of the receiver**, so the drum hangs mostly **below** the bore line: the gun fires from the top chamber of the drum.

## 3. Part by part

All sizes are X by Y by Z. "Touches" means faces in contact or overlapping.

### Drum (the one moving part)
- A **hexagonal prism**, 8 long, with **points up and down** and flat faces left and right. Across the points it is 14 (Y -7 to +7); across the flats it is 12.1 (Z about +/-6.06). Axis along X through the origin.
- **Six chambers.** Each of the six faces carries a grenade; the side flat shows two stacked red grenade bodies with the next one showing at the slanted faces above and below. Each grenade body lies along X, about 6 long and 3 tall, with a **brass-colored end cap at each end** and a red band between.
- **End faces** (front and rear): a circle of **six round domed bumps** (the chamber mouths) around a seventh, slightly lower, central bump, in mid gray. About 3 units each.
- **End plates:** two thin hexagonal discs, each **1 thick**, slightly bigger than the drum (14 -> 16 across the points, 13.9 across the flats), one at the rear (X -5 to -4) and one at the front (X +4 to +5), both static. They show as **light silver rims** around the drum and as a white-silver ladder look on the side flats, with horizontal rungs between the grenades. The drum body sits between them with a hairline of 0 to 1 units to spare.
- The body of the drum behind the front plate is hidden; only the rims, the grenades and the end bumps are visible.

### Receiver (main body)
- **Main block:** 9 x 6 x 4 (X -16 to -7, Y +2 to +8). The long black box that dominates the rear.
- **Upper housing:** a 6 x 6 x 5 block (X -13 to -7, Y +4 to +10), wider than the main block by 0.5 each side, with the **rear-lower corner chamfered** about 2 by 2. It reads as a taller, slightly flared hump on top of the main block, ahead of the middle.
- **Rear collar:** 1 x 3 x 3 (X -17 to -16, Y +4.5 to +7.5), where the stock rod emerges.
- **Rear top plate:** 3 x 1 x 4 (X -15 to -12, Y +8 to +9), sitting on top of the main block.
- **Underplate:** 5 x 1 x 3 (X -14.5 to -9.5, Y +1 to +2), hanging under the main block.
- **Drum rear shield:** a tall plate 2 x 9 x 5 (X -7 to -5, Y +1 to +10), the wall just behind the drum's rear end plate. It is wider than the main block (5 against 4) and taller. A dark circular **bore opening** is visible from straight behind, about 3 across, near Y +4 to +5, where the barrel line meets the rear.
- **Drum bridge:** a 10 x 2 x 4 spine (X -5 to +5, Y +6 to +8) running through the drum's top zone from the rear shield to the front block, and a 9 x 1 x 5 **top cover plate** over it (X -5 to +4, Y +9 to +10).
- **Front block:** 2 x 2 x 5 (X +4 to +6, Y +8 to +10), the forward end of the cover.

### Top rail and sights
- **Rail:** a long flat bar 16 x 1 x 3 (X -12 to +4, Y +10 to +11), centered. Its top is **ribbed**: about ten thin vertical slots across the top surface, like a ventilated rail.
- **Rear sight bump:** a small block 1 x 1 x 2 (X -11.5 to -10.5, Y +10.5 to +11.5) on the rail, about a third of the way along. Barely visible.
- **Front fork sight:** two thin posts, each 1 x 2 x 1 (X +3 to +4, Y +11 to +13), at the rail's front end, **at Z about -1 and +1 with a 1-unit gap** between. From the front this reads as a small "U" notch on the top line.

### Launch tube, sleeve and handguard
- **Tube:** a hexagon-section tube, about 4.8 across, **12 long** (X +6 to +18), axis about Y +4.2. It is the front-most part. The muzzle end shows a **black circular bore** about 2.5 across on a light plate.
- **Sleeve:** a larger hexagon-section collar around the tube, about 6.5 across and 6.2 tall, **8 long** (X +7 to +15). It starts 1 unit forward of the front plate and ends 3 units short of the muzzle, so the tube shows as a thin neck beyond it. Brushed silver, lighter than anything else on the gun.
- **Front plate:** a vertical plate 1 x 10 x 5 (X +5 to +6, Y -2 to +8), behind the tube, flush with the drum's front end plate. It is the wall the tube mounts against.
- **Underbarrel bar (handguard):** 8 x 2 x 4 (X +6 to +14, Y -1.5 to +0.5), under the tube, with a 1 x 1 x 4 **collar** at its front end (X +13 to +14, Y +0.5 to +1.5) tying it back up to the sleeve. It reads as a thick lower fore-end, slightly narrower than the sleeve.

### Stock
- **Rod:** 6 x 2 x 2 (X -23 to -17, Y +5 to +7), a thin gray tube.
- **Buffer block:** 2 x 5 x 2 (X -25 to -23, Y +2.5 to +7.5).
- **End plate:** 1 x 7 x 3 (X -26 to -25, Y +1 to +8), the butt. Together they read as a **thin rod ending in a slightly raised plate**, much thinner than the receiver and at roughly its mid-height.

### Pistol grip
- A 2-wide slab, about 3 front-to-back and 6 long, **leaning back about 20 degrees** from vertical (its lower end is farther back than its top). Its top, tucked under the rear of the receiver at X -14 to -11 and Y about +2; its bottom at X -16 to -13, Y about -4.5.
- Touches: the receiver's underside and the underplate.
- It is a single plain slab with no finger grooves or backstrap.

### Trigger and guard
- **Trigger:** 1 x 3 x 1 (X -9.5 to -8.5, Y -0.5 to +2.5), hanging from the underplate just in front of the grip.
- **Guard:** a bare bar 4 x 1 x 1 (X -12.5 to -8.5, Y -1.5 to -0.5) running beneath the trigger. It reads as a short horizontal line below the receiver, with no front post.

## 4. Silhouettes per view

- **Side (main view):** left to right (stock to muzzle): a thin rod and a small end plate at mid-body height; the black receiver, with its ribbed rail and the tiny sight posts rising above; a slanted grip hanging down at the back with the small trigger and guard bar; then the **huge drum**, a vertical rounded-hex block about 16 tall that pokes up past the receiver top slightly less than it hangs below it; a short tube and sleeve beyond it, with the bar underneath. Negative space: the gap between the grip and the drum (the **hand space**) at the bottom; the gap below the stock rod; the gap between the tube sleeve's underside and the drum's lower part.
- **Top:** a narrow dark spine (4 to 5 wide) with the **13.9-wide drum band** crossing it near the middle. Behind the drum, the receiver with its ribbed rail; a thin stock rod ending in a plate; ahead of the drum, the tube sleeve (6.5 wide) and the tube end. The drum is the dominant feature.
- **Front:** an upright **hexagon** about 14 wide and 16 tall with six round bumps around a center and the black bore of the tube above center. Above it, a narrow column (the rail) with the **U-notch fork** on top.
- **Back:** the same hexagon seen from behind, the receiver as a column in the middle, the dark circular bore opening visible near the top of the column, and the grip hanging below it.
- **Underside:** a long thin body with the drum's wide band in the middle; the grip's slab, the trigger and the guard bar behind the drum; the underbarrel bar and the sleeve ahead.

## 5. Materials and palette

The texture is dark, gritty, desaturated black-and-gray metal with a slight cool green-gray tint on the lighter panels. The values below were sampled from the original texture, and the drum and grenade colors from its drum strip.

| Part | Material | Base | Highlight | Shadow |
|---|---|---|---|---|
| Receiver, upper housing, shield, grip, stock block | black matte metal | #1E1E1E | #2A2A2A | #121212 |
| Rail top | same black, with vertical dark ribs | #2A2A2A | #363636 | #060606 |
| Lighter panels on the receiver sides (inset plates) | cool gray metal | #4E5A5A | #666666 | #424E4E |
| Stock rod, underbarrel bar, guard | mid gray metal | #5A6666 | #7E7E7E | #424E4E |
| Tube sleeve | brushed silver, with diagonal sheen | #A2A2A2 | #AEAEAE (up to #C8C8C8) | #727272 |
| Tube and muzzle plate | light gray metal | #969696 | #AEAEAE | #727272 |
| Bore openings | near-black | #060606 | | |
| Drum rims and end plates | silver-white | #C8C8C8 | #E8E8E8 | #8A8A8A |
| Drum chamber bumps and end faces | dull stone-gray | #5A6666 | #727272 | #363636 |
| Grenade bodies (visible on the drum sides) | bright red | #D84838 | #E85838 | #782818 (dark edge #481808) |
| Grenade end caps | brass or cream | #D8C8A8 | #D8D8B8 | #A09357 |

Shading style: **gritty noise** on the black, a few gray levels per pixel, looking like worn painted steel; thin, 1-pixel **lighter edge lines** around inset panels; the rail's vertical ribs as dark slots; brushed streaks on the sleeve. The grenades have a **cylindrical vertical gradient**: bright red in the middle of each body, darker at the top and bottom edges, with thin dark rings where the cap meets the body. There is no weathering color, no stenciling and no logo.

## 6. In-game presentation

The mesh is drawn with the model's Y up and no 180 degree flip. All offsets are in blocks after the model's 1/16 scale.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw 45 degrees, scale 2.0 | rest offset about (1.05, 0.1, 0.85); the gun is seen side-on and turned toward the viewer, so you see the left face of the drum and the grip |
| First person firing | whole-gun roll about Z up to 10 degrees at mid-animation | shift back about 0.25 and up about 0.1; the drum **turns 60 degrees** (one chamber) about its axis |
| First person reloading | roll -25 degrees about Z, yaw -25 to -55 degrees (net) | shift about (-0.25, -0.2, -0.15) at mid-reload; smooth rise and fall, then drop |
| First person aimed | no scope and no zoom overlay | |
| Third person (held) | yaw 135 degrees, scale 1.3 | offset (-0.2, 0.35, 0) |
| Ground (dropped) | yaw -90 degrees, scale 0.8 | raised 0.35 |
| Inventory icon | rotated 180 degrees about both X and Z, scale 0.7 | offset (0, 0.15, -0.15); drawn side-on, the whole 3D model fills the slot, no sprite |

**Muzzle flash point:** the front of the tube on its axis (model position X about +18, Y about +4, Z = 0). The flash sprite is drawn at about 1.5 times scale, positioned slightly ahead and above the tube.

**Weapon type:** semi-auto, 6 grenades in the drum, 5-tick fire delay, 100-tick (5 s) reload.

## 7. Motion

- **The drum is the only moving part.** On every shot it **rotates 60 degrees** about its own axis (X), advancing one chamber, driven by a smooth ease over the animation. The end plates, receiver and tube do not move. In the rest pose the drum sits with a point up, so the top chamber is aligned with the tube.
- **Recoil:** whole-gun, in first person a roll of about 10 degrees with a 0.25-block pullback. In third person the gun pitches up about 40 degrees about a point near the wrist in the first quarter of the first half, then settles.
- **Reload:** 100 ticks. First person: the gun rolls about 25 degrees and yaws, slides slightly back and down, hangs, then returns. Third person: a 40 degree roll at mid-animation. The drum does not move during reload.
- Our version should keep the drum as a separate named part so it can turn, with the static plates in the body.

## 8. What to avoid

- **A drum the same width as the body.** The drum is the point: 13.9 wide against a 4 to 5 wide body. Do not shrink it to a box on the receiver.
- **A round or square drum.** Use a hexagon with points up and down, with visible silver rims and six chamber bumps on each end.
- **A body painted bright.** The body is nearly black; the bright parts are the red grenades, the silver sleeve and the white-silver rims.
- **A long, thin barrel.** The tube is short (12) and fat (4.8), and the silver sleeve is fatter still.
- **A solid stock.** It is a thin rod with a small plate.
- **A grip as thick as the body or standing straight up.** It is a 2-wide slab leaning back about 20 degrees.
- **A scope, a magazine, a carry handle or fins.** The only sights are the ribbed rail, a tiny rear bump and the fork.

## 9. Acceptance checklist

- [ ] Length : height : width is within about 10 percent of 3.2 : 1.5 : 1.
- [ ] The drum is a hexagonal prism with points up and down, about 14 across the points, 12 across the flats and 8 long, plus two thin silver end plates.
- [ ] The drum is about 3 times wider than the receiver and is the widest part of the model.
- [ ] Each drum side shows red grenade bodies with brass caps between white-silver rungs; each end shows six round bumps around a center.
- [ ] The drum is a separate part that can turn 60 degrees about the long axis.
- [ ] The receiver is black and boxy, with a flared, taller upper housing in its front half.
- [ ] A long, ribbed rail runs along the top from the rear housing to the drum's front end.
- [ ] The fork front sight is two thin posts with a gap, at the rail's front end; the rear sight is only a tiny bump.
- [ ] The launch tube is a hex-section tube about 12 long, with a black bore at the muzzle, and the silver sleeve covers its rear two thirds, leaving about 3 units of tube bare.
- [ ] An underbarrel bar about 8 long sits under the sleeve.
- [ ] The stock is a thin rod, 6 long, ending in a small buffer and a raised end plate, at about the receiver's mid-height.
- [ ] The grip is a slab about 2 wide leaning back about 20 degrees, under the rear of the receiver, with a small trigger and a bare guard bar.
- [ ] The dark bore opening is visible from the rear, near the top of the rear shield.
- [ ] Colors: body about #1E1E1E, light panels about #4E5A5A, sleeve about #A2A2A2, rims about #C8C8C8, grenades about #D84838.
- [ ] No part floats; every block overlaps or touches a neighbor.

## 10. Unsure

- **Hex section orientation of the tube and sleeve.** The mesh has them as rough hexagons, slightly rotated, with a different height and width (4.6 tall, 4.9 wide). A round or octagonal tube of about 4.8 across is an acceptable approximation.
- **The 1-unit gap in the receiver top.** The bridge spine's top is at Y +8 and the cover plate starts at Y +9. The renders show the area as continuous black, so close the gap with either a bridge that fills it or a plate overlapping the spine.
- **Grip angle.** About 20 degrees measured from the slab's corners; its exact lower end may be 1 unit off.
- **Rear bore.** A black hole on the texture's rear plate suggests a visible bore from behind. I did not confirm that the geometry has a real opening.
- **First-person transform values** come from reading the renderer, not from seeing it in game. Treat the offsets as approximate. The pivot of the drum's 60 degree turn is also approximate; the drum's own axis is the right answer.
- **Camo variants:** none. The original has one texture only.

## 11. The launched grenade (projectile)

This is the 40 mm grenade that flies after firing; it is a separate small mesh with its own 32 x 16 texture.

**Shape.** A short, chunky **bullet-shaped slug with a square cross-section**: a body about **6 long** with a **4-sided pyramid nose 2 long**, so 8 long overall and **4 across** the diagonal of its square section. The section is a **diamond** (square turned 45 degrees to its axis): its corners point up, down and to both sides. The rear end is a **flat** square cap. The nose pyramid comes to a sharp point. Facet shading makes it read as a faceted shell, not a round one. Length : width is 2 : 1.

**Look.** A brass-cream shell with a **wide red band** around the middle (about 2 units wide, a third of the body length), flanked by thin dark-olive rings, and thinner brass-colored rings between the band and the caps. The nose is plain cream; the rear is slightly darker cream with a black-and-red center spot on its flat cap.

| Part | Base | Highlight | Shadow |
|---|---|---|---|
| Body and nose | #E5DAAA (cream) | #F6F3E2 | #BBAF77 |
| Lower faces (shaded side) | #A49861 | | #82763F |
| Red band | #DE4C37 | #EA4E36 | #702410 |
| Rings next to the band | #716B4F | | #3D371D |
| Rear cap center | #060606 | #C6422A (red dot) | |

Shading: a smooth **vertical gradient** from light on the top corner to dark on the bottom, repeated on each face, with soft 1-pixel steps. No noise.

**In flight.** It is drawn at 0.8 scale (about 0.4 block long), pointing along its flight direction, and **spins about its own long axis** at one full turn per 40 ticks (2 s), so the diamond faces visibly rotate. It leaves a smoke trail (separate effect). On a bounce it continues as a new grenade with 75 percent speed.
