# Tesla Rifle: clean-room model notes

Design notes for rebuilding the Techguns **Tesla Gun** look-alike (GregGuns "Tesla Rifle") in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout (64x64 is the native size).

Related facts: [ModelTeslaGun](../gun-models.md), [rendering framework](../rendering-framework.md).

**Units and axes used in this document.** One unit = one texel at the native 64x64 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention). The barrel axis is at **Y = 0.5**, so heights are given in absolute Y with that axis marked. **Z = 0 is the gun's mid-plane**; sizes across Z are total widths, centered, and positions across Z are +/- from the mid-plane. **X = 0 is a point on the body, 9 units behind the barrel's first face.** The gun is mirror-symmetric across the mid-plane (left and right are identical). Numbers are exact unless marked "about".

## 1. Gestalt

A **bulky, gray, laboratory-looking energy rifle** with a stubby coil-barrel. At a glance you read: a stack of boxy light-gray parts forming a long body; a **short barrel (a plus-shaped bar) wearing two rings of four spikes**, each spike ending in a **bright blue glowing tip**; a **pair of purplish capacitor blocks** on top of the body; thin rods running along the underside of the body at 45 degrees; a tilted grip and a vertical foregrip post; a see-through **open-frame stock**; and a tiny **red dot** between two thin posts above the body's rear. Navy-blue accents (#161840 and #181944) separate gray parts; the only glow is blue (and the single red dot).

Defining features in order of visual importance:

1. **The two coil rings on the barrel**: at each of two stations, four 45-degree-turned cubes (top, bottom, left, right), each carrying a glowing blue cube. Eight glowing blue tips total.
2. **The plus-shaped short barrel** with four dark rails in its corners, ending in a navy diamond bushing and a light diamond tip with a black end.
3. **The four 13-long thin rods along the underside** (two per side, 45-degree diamond section), which make the gun about twice as wide as its body.
4. **Two purplish capacitor boxes** on the top of the body, with a gap between them.
5. **An open frame stock** (two flat rails with a hole between them, and diagonal gussets up to the body) ending in a tall butt plate.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt plate back face to muzzle tip) | 49.5 (x -27.5 to 22) |
| Total height (rear-sight posts top to grip heel) | about 18.8 (y -6 to 12.8) |
| Total width | 10.8 across the underside rods (z +/-5.4); 9 across the glow tips (z +/-4.5); 6 for the main body (z +/-3) |
| Length : height : width | about 4.6 : 1.74 : 1 |
| Barrel group (plus bar, bushing and tip) | 13 long (x 9 to 22), about **26 percent** of the length |
| Body (x -17 to 9) | 26 long |
| Stock (butt plate to body) | about 10 (x -27.5 to -17) |
| Body height (the main mass) | about 6.5 (y -1.5 to 5) |
| Grip | about 7 tall, tilted 18 degrees back |
| Foregrip post | 6 tall (y 5 to 11) |

The gun is dominated by its **rear and middle**; the barrel is short for a rifle. Top silhouette is lumpy (capacitors and sight posts); the underside has a straight foregrip and the rods.

## 3. Part by part

All parts are boxes; many are tilted 45 degrees. Z widths are totals, centered on the mid-plane. "Touches" means faces in contact or overlapping.

### Barrel group (x 9 to 22)
The barrel axis is at y 0.5.
- **Plus-shaped bar, x 9 to 17 (8 long):** two crossing blades. A **vertical blade 8 long, 5 tall, 2 wide** (y -2 to 3, z +/-1) and a **horizontal blade 8 long, 2 tall, 5 wide** (y -0.5 to 1.5, z +/-2.5). The section is a plus 5x5 with 2-thick arms. Very light gray (#9C9C9C on the vertical blade; #9A9A9A with #A4A4A4 on the horizontal blade).
- **Four corner rails:** four thin bars, each **10 long, 1 tall, 1 wide** (x 9 to 19), tucked into the four notches of the plus: at y -1.5 to -0.5 or 1.5 to 2.5, and z -2 to -1 or 1 to 2. They run 2 units past the blades' front end. Dark gray (#626262 top, #676767 bottom, #646464 sides), with a lighter end face (#8F8F8F) at the rear end.
- **Navy bushing, x 17 to 19 (2 long):** a 3x3 box turned 45 degrees about the barrel axis, so it reads as a diamond about 4.2 across (y -1.6 to 2.6, z +/-2.1). Dark navy-gray (#404056 on its faces).
- **Tip, x 19 to 22 (3 long):** a 2x2 box turned 45 degrees about the axis: a diamond 2.8 across. Light gray (#969696 on the long faces) with **both ends painted pure black** (#000000). This is the muzzle.
- Touches: the plus bar joins the washer stack behind it; the rails are buried into the blades' corners.

### Coil stations (two rings of four spikes)
- Two stations, at **x = 11 and x = 15** (centers). At each station there are **four cubes, each 2 x 2 x 2**, one on each side of the plus bar: above (center y -2.5), below (center y 3.5), left and right (center z -3 and +3, at y 0.5).
- **Turns:** the top and bottom cubes are turned 45 degrees about the vertical axis (diamonds when seen from above; 2.8 long and 2 tall from the side). The left and right cubes are turned 45 degrees about the Z axis (diamonds when seen from the side; 2.8 across the diamond on their side faces, 2 wide in z). So each cube's long diagonal runs along the barrel.
- **Gray:** all eight cubes are mid-gray (#898989 to #999999 on faces, with the front-facing side a touch lighter (#989898) than the rest).
- **Glow tips:** on each cube's outer face, a **1x1x1 cube** (turned 45 degrees the same way as its cube, so about 1.4 across) centered 4 units from the barrel axis: y -3.5 (top), y 4.5 (bottom), z -4 and z +4 (sides). Each tip overlaps its cube by about 0.5. **Fullbright pure blue** (#0000FF) on all faces, drawn without world lighting.
- Each cube's inner end overlaps the plus bar's arm (e.g., the side cubes overlap the horizontal blade's end, the top cubes overlap the vertical blade).
- In side view the side cubes read as gray diamonds at the barrel's mid-height, each with a small blue diamond tip in front of it (z -4 or +4); the top tips show as small blue squares above the barrel (y -3.5) and the bottom tips as small blue squares below it (y 4.5).

### Washer stack (x 1 to 9)
Between the main body and the barrel, front to back:
- **Navy plate**, x 8 to 9, **1 long, 4 tall, 4 wide** (y -1.5 to 2.5, z +/-2). The x faces are gray (#686868), the thin edges a deep navy (#181944).
- **Light washer**, x 7 to 8, **1 long, 3 tall, 3 wide** (y -1 to 2, z +/-1.5), light gray (#969696).
- **Dark washer**, x 6 to 7, **1 long, 4 tall, 4 wide** (y -1.5 to 2.5, z +/-2), dark gray (#595959).
- **Front block**, x 1 to 6, a **5 x 5 x 5** cube (y -1.5 to 3.5, z +/-2.5), mid-dark gray (#5F5F5F top, #6B6B6B sides).
- **Top rib** on that block: **4 long, 1 tall, 2 wide** (x 1.5 to 5.5, y -2.5 to -1.5, z +/-1), nearly black (#282828 top, #1C1C1C sides).
- **Belly block** under it: **3 long, 2 tall, 3 wide** (x 3.5 to 6.5, y 3 to 5, z +/-1.5), deep navy (#161840).
- **Foregrip post:** a **2 x 6 x 2** post turned 45 degrees about the vertical axis (a 2.8 diamond, seen from above), **centered at x 5**, hanging from y 5 to 11, z = 0. Dark gray (#4E4E4D sides, #3D3D3D top).

### Main body (x -10 to 4)
- **Lower body:** a box **10 long, 4 tall, 6 wide** (x -8 to 2, y 0.5 to 4.5, z +/-3). The widest solid of the gun. Its sides are light (#9F9F9F) and its **bottom is light (#969696)** but its **top is dark (#474747)**; the front and back faces are mid-gray (#888888, #868686).
- **Belly plate:** **8 long, 1 tall, 5 wide** (x -7 to 1, y 4.5 to 5.5, z +/-2.5), dark gray (#5A5A5A).
- **Spine:** a light box **8 long, 2 tall, 3 wide** (x -7 to 1, y -1 to 1, z +/-1.5), very light (#A3A3A3 sides, #A6A6A6 top), partly buried in the lower body's top.
- **Capacitor blocks:** two boxes **3 long, 3 tall, 4 wide** (y -2 to 1, z +/-2), at **x -6.5 to -3.5** and **x -2.5 to 0.5**, with a 1-unit gap between them. Their faces are a **purplish gray**: top #65658F, front and back faces #6E6E8A, side faces a neutral #727272 to #737373. They sit on the spine and rise 1 above it.
- **Rear cheek:** **2 long, 2 tall, 3 wide** (x -10 to -8, y -3 to -1, z +/-1.5), dark (#3C3C3C), at the top of the body's rear.
- **Rear dark rib:** **8 long, 1 tall, 2 wide** (x -15.5 to -7.5, y -2.5 to -1.5, z +/-1), nearly black (#272727 top, #1C1C1C sides), running along the rear body's top.
- **Rear body, stepped (three boxes):**
  - x -10 to -7: **3 long, 5 tall, 5 wide** (y -1.5 to 3.5, z +/-2.5), #5E5E5E;
  - x -14 to -10: **4 long, 5 tall, 4 wide** (y -1.5 to 3.5, z +/-2), #626262 to #696969;
  - x -17 to -14: **3 long, 4 tall, 3 wide** (y -1.5 to 2.5, z +/-1.5), #595959.
  The body tapers in width from 6 to 5 to 4 to 3 as it goes back.
- **Navy underside block:** **4 long, 2 tall, 3 wide** (x -13.5 to -9.5, y 3 to 5, z +/-1.5), deep navy (#15163C), under the rear body.
- Touches: all of the above overlap their neighbors; the stack is a continuous mass from x -17 to 9.

### Rear sight pylons
- Two thin posts, each **1 long, 3 tall, 1 wide** (x -7.5 to -6.5, y -6 to -3), one at z -2 to -1 and one at z 1 to 2, so they are 2 apart. Dark (#3A3A3A to #3D3D3D). Their feet stand on the capacitor/spine region.
- At each post's foot, a **1 x 2 x 1 strut turned 45 degrees about Z**, leaning rearward, reaching from the post's foot down to the top of the rear body (bounding box x -8.6 to -6.5, y -3.7 to -1.6). Dark (#3E3E3E). Each post-plus-strut reads as a hooked leg.
- **Red dot:** a 1x1 square with no thickness, at x -7, y -5.5 to -4.5, z +/-0.5, hanging between the posts. Pure red (#FF0000), **fullbright**.

### Grip and trigger
- **Grip:** one box **3 wide (X), 7 tall, 2 thick (Z +/-1)**, hinged at the body's underside near (-10, 5) and **tilted 18 degrees backward**. The tilted box spans x -15 to -10, y 4.1 to 11.7. Dark gray (#4E4E4E sides, #3E3E3E top).
- **Heel pad:** a box **4 long, 1 tall, 3 wide**, tilted the same 18 degrees, bounding x -15.8 to -11.7, y 10.6 to 12.8, z +/-1.5, at the grip's bottom end. Medium-dark (#595959 to #5D5D5D).
- **Trigger:** a small box **1 long, 2 tall, 1 wide** (x -10 to -9, y 4 to 6, z +/-0.5), gray (#525252), in front of the grip's top. No trigger guard.

### Underside wire rods
Four 1x1 rods turned 45 degrees about the gun's long axis (diamonds 1.4 across), all light gray (#939393 to #949494 on all faces; the long rods #909090 top, #8C8C8C bottom, #9C9C9C end face).
- **Two long rods:** each **13 long** (x -9.5 to 3.5), centered at y 3.2 (y 2.5 to 3.9), one on each side at z -4.7 and +4.7 (bounding z -5.4 to -4.0 and 4.0 to 5.4).
- **Four short connectors:** each a 1x1x4 diamond-section rod at **45 degrees**, joining the body to a long rod. Two on each side, at **x -9.5 to -8.5** (rear) and **x 2.5 to 3.5** (front); each starts inside the body's side at about (y 0, z +/-1.5) and runs outward and down to the long rod's end (y about 3.2, z about +/-4.2).
- From the side the rods read as a **long thin bar under the body's lower half** with a diagonal stub at each end; from the front or back they read as two diagonal "arms" flaring out and down from the body, 10.8 wide in total.

### Open stock
- **Butt plate:** a box **1 thick, 6 tall, 4 wide** (x -27.5 to -26.5, y 0 to 6, z +/-2), plus a lower tab **1 thick, 2 tall, 3 wide** (y 6 to 8, z +/-1.5). Dark gray (#414141 to #434343). A **backing plate** **1 thick, 4 tall, 3 wide** (x -26.5 to -25.5, y 1 to 5, z +/-1.5) sits behind it, a hair darker (#3B3B3B to #404040).
- **Side rails:** two flat bars, each **8 long, 2 tall, 1 thick** (x -26.5 to -18.5, y 2 to 4), at z -2 to -1 and 1 to 2. Light gray (#989898 to #A1A1A1). They are 2 apart, so a **2-wide, 2-tall slot** is open between them.
- **Diagonal gussets:** two bars, each **2 wide, 5 long, 1 thick**, turned 45 degrees about the Z axis, hinged at the rails' front end (x -18.5, y 4), rising toward the front to meet the rear body (bounding x -19.9 to -15, y -0.9 to 4). Same light gray (#999999). They sit at the rails' z positions (z +/-1.5 centers).
- The stock is therefore a **skeletal frame**: no solid cheek, nothing between the rails.

## 4. Silhouette checks

- **Side (the main view):** from the left: a tall, thin butt plate; two rails running forward at mid-low height with an angled gusset rising from each toward the body; a body whose top is lumpy: a raised rear cheek, two square capacitor boxes with a gap, the pair of thin sight posts to the rear and a gap again in front; under it a long thin rod with diagonal stubs at its ends; a grip leaning back at the rear; a vertical foregrip post hanging in front of the body; then, in front of the body, a short barrel with **two diamonds-and-squares of blue tips** above and below, a navy diamond and a light diamond tip. Negative space: the hole between the stock rails; the open gap between the capacitors; below the rails.
- **Top:** a dumbbell shape. A thin stock (two rails, with a visible 2-wide gap between them), then the body widening to 6, with the two capacitor boxes as a 4-wide panel, a mid-section at 6 wide, the long rods as thin strips out at z +/-4.7 on each side of the body (10.8 wide in total), then a narrow barrel with four small blue squares on each side at each of the two stations (z +/-4), ending in a diamond.
- **Front:** an X-like silhouette: a plus-shaped barrel with a diamond in the center (the tip); four spikes with blue tips (top, bottom, left, right); the two long rods flaring out and down on both sides at 45 degrees, like arms; the foregrip post hanging below; the grip and butt plate behind.
- **Back:** the butt plate (6 to 8 tall and 4 wide) with the open rail pair behind it; two sight posts with a red dot above the body; the rods flaring at each side.
- **Underside:** a long, thin bar: the stock rails, the body with the belly plate, the two outer rods, the foregrip post, and the four spikes with blue tips.

## 5. Materials and palette

The original is a **mid-gray with a tint of purple-blue**: light gray for the visible metal, darker gray and near-black for details, deep navy for plates and bushings, and bright blue for the glow.

| Part | Material | Base | Highlight | Shadow / detail |
|---|---|---|---|---|
| Plus bar vertical blade | light metal | #9C9C9C | #9C9C9C | #989898 |
| Plus bar horizontal blade | light metal | #9A9A9A | #A4A4A4 (ends) | #999999 |
| Corner rails | dark gray | #646464 | #8F8F8F (rear end) | #626262 |
| Bushing diamond | navy-gray | #404056 | #505161 | #3F4055 |
| Tip diamond | light gray | #969696 | #969696 | #8F8F8F; ends #000000 |
| Spike cubes | mid-gray | #898989 | #999999 | #888888 |
| Glow tips | blue, fullbright | #0000FF | | |
| Front block | mid-gray | #6B6B6B | #696969 | #5F5F5F |
| Washers | dark and light | #595959 / #969696 | | |
| Navy plate and belly block | deep navy | #181944 / #161840 | #1A1C4B | #14163A |
| Foregrip post | dark gray | #4E4E4E | #4F4F4F | #3D3D3D |
| Lower body | light sides, dark top | #9F9F9F (sides) | #969696 (bottom) | #474747 (top) |
| Spine | very light | #A3A3A3 | #A6A6A6 | #909090 |
| Capacitor blocks | purple-gray | #6E6E8A (front/back) | #65658F (top) | #727272 (sides) |
| Rear body and cheeks | gray | #5E5E5E to #696969 | | #3C3C3C (cheek) |
| Dark ribs | near-black | #272727 | | #1C1C1C |
| Sight posts and struts | dark gray | #3A3A3A | #3E3E3E | |
| Red dot | pure red, fullbright | #FF0000 | | |
| Grip and heel | dark gray | #4E4E4E | #5D5D5D (heel) | #3E3E3E |
| Underside rods | light gray | #949494 | #9C9C9C (end face) | #8C8C8C |
| Stock rails and gussets | light gray | #999999 | #A1A1A1 | #989898 |
| Butt plate | dark gray | #414141 | #434343 | #3B3B3B |

Shading style:
- **Flat, matte metal** with very little noise; each box is nearly uniform. The gun reads as **panels of different grays** rather than a single texture, with deep navy plates used as a color accent in three places (the washer stack, the belly block, and the rear underside block) and purple-gray capacitors.
- **Glow:** pure, saturated blue (#0000FF), no gradient. Because the tips are tiny, they act as bright points of light. The red dot is the same type of glow part.
- No scratches, wear or labels.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units, after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 1.3 | offset (0, -0.5, 0) plus the idle shift (0.1, -0.25, -0.5). Seen side-on, turned 45 degrees toward the stock. |
| First person firing | the same, plus a slide along the gun axis of up to 0.25 block (backward) and back | no roll. |
| First person reloading | roll about Z up to 30 degrees, yaw up to -15 degrees | plus a shift up to (0.5, 0.25, 0.25) from idle. |
| First person aimed | no scope and no zoom overlay on this model | |
| Third person (held) | yaw 225 degrees, scale 1.25 | offset (-0.7, -0.5, 0). |
| Third person aimed | extra offset (0, 0.2, -0.55) | after the shared aimed rotation. |
| Ground (dropped) | yaw 90 degrees, scale 0.8 | offset (0.3, -0.35, 0). |
| Inventory icon | yaw 180 degrees, scale 0.55 | offset (-0.1, 0, 0); the icon is the 3D model, side-on. |

**Muzzle flash:** a lightning-style additive flash at 1.75 times scale (a bright, jagged blue-white arc), at the same screen offset as the Laser Rifle (about (-0.65, -1.0, -0.3), turned 90 degrees). In model terms the flash belongs at the tip, at about (x 22, y 0.5, z 0).

## 7. Motion

- The **model has no moving parts.** The coil spikes do not spin or extend, the glow tips do not pulse in the model itself, and nothing slides. All motion is whole-gun transforms.
- **Recoil:** the shared base recoil (the renderer adds nothing of its own for the third-person recoil). First person: the 0.25-block slide above.
- **Reload:** third-person reload adds no transform. First-person reload is the roll-and-shift above. Reload is 45 ticks in the original; for energy weapons the GregGuns design may have no reload.
- **Optional (not in the original):** the builder may keep the eight glow tips as separately named parts so later code can pulse or flicker them, but the rest pose must match this document.

## 8. What to avoid

- **A smooth round coil.** The coils are four flat cubes turned 45 degrees at each station, not a ring or a torus.
- **Blue everywhere.** Blue glow appears only on the eight small tips. The navy plates are dark and matte, not glowing.
- **A long barrel.** The barrel group is 13 long and the plus bar only 8. Do not stretch it.
- **A solid stock.** The slot between the stock rails is open and must stay open.
- **Rods hugging the body.** The four long rods stand out 1.4 to 2.4 beyond the body's side and are lower than its middle; the gun is 10.8 wide there.
- **A single capacitor.** There are two, with a 1-unit gap.
- **Dim glow.** Glow parts must stay bright in the dark.
- Do not add things the original lacks: no magazine, no scope, no trigger guard, no wires from the coils.

## 9. Acceptance checklist

- [ ] Length : height : width is within about 10 percent of 4.6 : 1.74 : 1.
- [ ] Barrel group is about 26 percent of the length; the plus bar is 8 long and the corner rails are 10 long.
- [ ] Plus-shaped bar with four dark corner rails, a navy diamond bushing, and a light diamond tip with black ends.
- [ ] Two coil stations at x 11 and x 15, each with four 2x2x2 cubes turned 45 degrees (top, bottom, left, right).
- [ ] Eight 1x1x1 blue glow tips, one on each cube, at 4 units from the axis; fullbright pure blue.
- [ ] Washer stack (navy plate, light washer, dark washer) and a 5x5x5 front block behind the barrel.
- [ ] Two purple-gray capacitor boxes with a 1-unit gap, on a light spine.
- [ ] Two thin sight posts with a 2-unit gap and a red fullbright dot between them.
- [ ] Four 45-degree diamond rods along the underside: two 13-long and two short stubs per side; total width about 10.8.
- [ ] A foregrip post (diamond section) hanging 6 below the front block.
- [ ] A grip tilted 18 degrees back with a heel pad and a small trigger stub.
- [ ] Open stock: two 8-long rails with a 2-wide slot, diagonal gussets, and a stepped butt plate (6 tall plus a 2-tall tab).
- [ ] Gray body (#898989 to #A6A6A6), dark details (#1C1C1C to #4E4E4E), navy accents (#161840 to #181944), purple-gray capacitors (#65658F to #6E6E8A).
- [ ] No part floats; every box overlaps or touches a neighbor.
- [ ] No moving parts in the model.

## 10. Unsure

- **Sight-post struts.** The small 45-degree struts at the posts' feet are tiny; their exact lean direction is my reading of a rotated box, and the shape could be a hook or a brace. They are minor.
- **Which end of the tip is black.** Both x-faces of the tip diamond are black in the texture, so it does not matter.
- **Spike cube faces.** Each spike cube has a slightly lighter front face; this is a detail of the original texture and optional.
- **Underside rods' purpose.** They have no visible connection to anything but the body; they read as cables or conduits. Keep them as drawn.
- **First-person framing numbers** come from formulas read, not seen in game. Treat the offsets as approximate. The scale 1.3 is a relative factor on a 49.5-unit-long model; the apparent size in hand was not measured.
- **Third-person recoil.** The Tesla renderer defers to the shared base recoil, which I did not read; assume a small kick.
