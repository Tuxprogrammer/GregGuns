# Grim Reaper: clean-room model notes

Design notes for rebuilding the Techguns **Grim Reaper** (a four-tube rocket launcher) look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelGrimReaper](../gun-models.md), [rendering framework](../rendering-framework.md), [revolver notes (template)](revolver.md).

**Units and axes used in this document.** One unit = one texel at the native 128x64 scale = 1/16 block. The muzzle points toward **+X** (forward). **Y points down**, with **Y = 0 on the tube axis**. **Z = 0 is the tube axis too**; Z+ is one flank and Z- the other (the "skull flank" is Z-, where the side rod is also mounted). **X = 0 is the rear end of the cage panels**, which is 3 units behind the point where the rear body ends and the tube block begins. Sizes across Y or Z are total, centered unless a range is given. Rounded to the nearest half unit unless it matters.

## 1. Gestalt

A **huge, olive-drab, four-barreled shoulder rocket launcher**, long and boxy. At a glance you read: a thick 11x11 square tube block with four muzzles on its front face; a dark open mesh cage wrapping its rear half; a plain rear body with a skull emblem; a **four-quadrant exhaust vent** at the very back (a cross inside a square frame); two vertical handles hanging below; and a **small side-mounted rod** (lens and sensor) on one flank. It is heavy and utilitarian, with muted army-green and dark olive-black, and no bright colors except a white lens and a single red dot.

Defining features in order of visual importance:

1. **Four-tube square block** with a four-hole muzzle face (four dark 4x4 holes with a 2-wide divider cross) and a thick muzzle cap.
2. **Dark open cage** over the rear 16 units of the tube block: thin plates with see-through lattice and corner rails.
3. **Rear cross vent**: 12x12 frame with a plus-shaped cross, leaving four open quadrants, backed by a dark rear wall.
4. **Two vertical handles** beneath: a 2x2 diamond post at the rear and a 3x2 post with a foot plate near the middle.
5. **Side rod** on the skull flank: a 9-long chain of diamond-section bands ending in a white lens, held by a slanted strut.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (rear vent to muzzle cap) | 49 (X -18 to 31) |
| Total height (roof sheet to handle foot) | about 22 (Y -7 to 15) |
| Total width | 12 (cage), 14 with the rear sheet buttons, about 19.5 with the side rod |
| Length : height : width (body only, 12 wide) | about 4 : 1.8 : 1 |
| Tube block | 24 long x 11 x 11 (X 3 to 27) |
| Muzzle cap | 4 long x 12 x 12 (X 27 to 31) |
| Rear body | 17 long x 11 wide x 9 tall (X -14 to 3) |
| Rear vent | 4 long x 12 x 12 (X -18 to -14) |
| Cage | 16 long (X 0 to 16) |
| Handles | 7 tall; centers 11 apart in X |

Order along the length from the rear: vent (X -18 to -14); rear body (X -14 to 3); tube block (X 3 to 27) with the cage over X 0 to 16; muzzle cap (X 27 to 31). The rear handle is at X -8, the front handle at X 3 to 6.

## 3. Part by part

All parts are boxes unless stated. Y is down from the tube axis; Z is across, centered.

### Tube block (the four tubes)
- **Overall:** 24 long (X 3 to 27), **11 wide x 11 tall** (Y and Z each -5.5 to 5.5).
- Cross-section is a square with a **1x1 notch at each of the four corners** and a **1x1 groove at the middle of each of the four sides** (running the full 24 units). The four quadrants between the grooves are the four tubes, each a 5x5 lump with its outer corner cut. A 1x1 hole runs along the center (hidden).
- The lumps' lengthwise faces carry painted bands: a **light edge row** (about #A9AB96 to #BBBCA8) at each border, then mid olive (#898B76), then a darker row (#7F8169 to #707260). Together these read as four rounded bars.
- The rear body ends exactly at X 3, where the tube block begins (a butt joint). The cage starts 3 units further back (X 0), so its first 3 units wrap the rear body's front end.

### Muzzle cap
- A 4-long block, 12 wide x 12 tall (X 27 to 31), with the **four tube corners notched**: the cap itself is 10 wide x 12 tall, plus two 1-thick side plates (10 tall) on the flanks, so each corner is notched 1x1. It is 12 x 12 overall.
- **Front face (X = 31):** four **dark 4x4 holes** (color about #1F1F1B to #2C2D24), set in a 2x2 grid, with a 2-wide dark-olive divider cross between them and a lighter 1-pixel rim (#585948 to #61634F) around each hole. Holes are 4 from each outer edge. The cap's other faces are olive #616351 (top and bottom), #515241 (rear face), #585948 (flanks).

### Cage (dark mesh)
- Four plates over the rear 16 units of the tube block (X 0 to 16): a **top plate** (Y -6 to -5, 10 wide), a **bottom plate** (Y 5 to 6, 10 wide), and two **side plates** (Z +-5 to +-6, 8 tall, Y -4 to 4). All 1 thick, 16 long.
- **Four corner rails:** each 16 long, 1x2 in section (1 tall by 2 wide), joining the top and bottom plates to the side plates at the four corners (e.g. Y -5 to -4, Z 4 to 6).
- The cage's outer size is **12 x 12**, wrapping the 11x11 tube block with the plates overlapping it by 0.5.
- **Lattice:** the plates are not solid. About two-thirds of their pixels are painted; the rest are transparent so the tubes show through in a regular grid. The top and bottom plates show a rectangle frame with five vertical bars and two horizontal bars; the side plates show a thick frame with a large open window in the center. Use cut-outs or build the plates as frames with large square holes.
- Colors: near-black olive, #3B3C34 to #42433B.

### Rear body
- A **17 long x 11 wide x 9 tall** block (X -14 to 3, Z +-5.5, Y -4.5 to 4.5).
- **Top and bottom plates:** 17 long, 9 wide, 1 thick (Y -5.5 to -4.5 and 4.5 to 5.5), centered on Z. Slightly lighter on the front edge (#B8BAA7 edge highlight).
- **Top block:** at the rear top, 6 long x 10 wide x 2 tall (X -14 to -8, Y -6 to -4, Z +-5). Dark olive (#5E604E).
- **Side panels:** one on each flank, 6 long x 6 tall x 1 thick (X -14 to -8, Y -5 to 1), flush against the body sides (Z +-5 to +-6). Dark olive (#616350 to #686A56) with a light vertical detail line.
- **Skull emblem** on the Z- flank: about 6 wide x 7 tall, near-white (#D8D6D6 to #E6E6E6) with dark (#2D2E26) outline, eye sockets and a tooth row; centered about X -4.5, Y 0. It is the one bright mark on the body.
- **Rear face (X = -14):** very dark (#2C2D25), with lighter vertical lines in places; it is the back wall of the exhaust vent and shows through the four openings.
- Body base color #878974, highlights up to #989A86 on the rear faces of the tube slabs, shadow #595A49.

### Rear cross vent (the exhaust)
- 4 long (X -18 to -14), **12 x 12 overall** (Y and Z -6 to 6). A thin outer frame: four 1-thick plates around a plus-shaped cross.
  - **Frame plates:** top (Y -6 to -5, 10 wide), bottom (Y 5 to 6, 10 wide), and two sides (Z +-5 to +-6, 8 tall). At each corner there is a 1x2 L-piece (as in the cage), so the frame is closed.
  - **Core:** 3 x 3 box at the center (Y and Z +-1.5).
  - **Arms:** a vertical bar (1 wide in Z, Y -4 to 4, passing through the core) and a horizontal bar (1 tall in Y, Z -4 to 4), each ending in a **T-cap** 3 wide x 1 thick (cap at Y +-4 to +-5 spanning Z +-1.5; cap at Z +-4 to +-5 spanning Y +-1.5).
  - The result is four open quadrants, each about 3.5 x 3.5, through which the dark rear wall of the body shows.
- Colors: arms and frame #515243 to #5D5E4D; core #575948.

### Roof sheets (a canopy hood)
- A thin, zero-thickness **roof sheet**, 4 long x 11 wide (X -16 to -12), at Y -7 (1 above the top block).
- At each Z edge: a vertical drop 1 tall (Y -7 to -6) at Z +-5.5; a horizontal flare 1 wide at Y -6 (Z +-5.5 to +-6.5); then a vertical wall 4 tall (Y -6 to -2) at Z +-6.5. All the same 4 long and zero thickness.
- A **2x2x1 button** (X -15 to -13, Y -4.5 to -2.5) on the outside of each wall (Z +-6.5 to +-7).
- Near-black (#383931 to #404139). Draw double-sided.

### Handles (underneath)
- **Rear grip post:** 2x7x2, turned 45 degrees about the vertical axis (a diamond, 2.8 across), at X -8, Z 0, hanging from Y 6.5 to 13.5. Side faces light olive (#82846F to #848571); top and bottom #5F6050.
- **Front grip post:** 3 long x 7 tall x 2 wide, not turned (X 3 to 6, Y 7 to 14, Z +-1), with a **foot plate** below it, 4 long x 1 tall x 3 wide (X 2.5 to 6.5, Y 14 to 15). Post color #8B8D78 sides, #6B6D5D ends; foot plate #565847.
- **Mounting pads:** a 4-long x 1-tall x 4-wide plate under the cage's bottom at the rear (X -10 to -6, Y 5.5 to 6.5, Z +-2), and a 7-long x 1 x 4 plate under the middle (X 0 to 7, Y 6 to 7, Z +-2). Both dark olive (#525443).

### Side rod (the Z- flank)
- Axis: Y 0, **Z -10.5**, along X. Built from four diamond-section pieces (square bars turned 45 degrees about X):
  - **Rear band:** X -2 to 0, 4.2 across (3x3 section). Dark (#3F4035) with a **single red pixel** (#FF0000) on its rear end face.
  - **Neck:** X 0 to 1, 2.8 across. Dark.
  - **Main body:** X 1 to 5, 4.2 across, olive (#686A56 top, #5A5C4B side).
  - **Tip:** X 5 to 7, 2.8 across, with a **bright white end face** (#FFFFFF center, #BDBEB7 rim): the lens. Other faces #505242.
- **Mount:** a 4x4x1 plate on the body flank (X 1 to 5, Y 0.5 to 4.5, Z -7 to -6), then a 3x2x2 bracket (X 1.5 to 4.5, Y 1.5 to 3.5, Z -9 to -7), then a **slanted strut** 3 wide x 3 tall x 2 deep, hinged at (Y 3.5, Z -9) and tilted 36.5 degrees so it leans from the bracket up and outward to the rod's underside (spans Y about -0.1 to 3.5, Z -10.8 to -7.4). The strut and bracket are a flat, uniform #5E604E.
- The rod's bottom is 2.1 below the axis, so the strut's top overlaps its underside.

## 4. Silhouette checks

- **Side (Z- flank):** a long boxy slab. From the left: a square cross frame (vent), then a body with a bright skull; the cage's dark grid over the middle; the olive tube block continuing to the dark muzzle cap; below, two vertical handles (a thin rear one, a thicker front one with a foot); above the rear, a thin dark hood sheet; sticking off the flank, the rod with its white lens.
- **Top:** a long rectangle 11 to 12 wide; the roof sheet and its flares at the rear (14 wide at the buttons); the cage's dark top lattice, then bare olive tubes with four cross grooves, then the cap.
- **Front:** a 12x12 square with four dark holes in a 2x2 grid; the cap's corner notches; the handle post and foot below; the rod's small diamond off to one side.
- **Back:** a 12x12 frame with a plus inside, four dark openings; the hood sheet above; the grip posts below.
- **Underside:** a long box with the rear and front mounting pads and the two posts.

## 5. Materials and palette

| Part | Material | Base | Highlight | Shadow / detail |
|---|---|---|---|---|
| Tube block | olive-drab painted metal | #898B76 (side slabs #8D8F7A) | #BBBCA8 edge rows | #7F8169 |
| Rear body (top, bottom) | same | #878974 | #989A86 (rear edges) | #595A49 |
| Rear body plates | same, lighter | #868873 | #B8BAA7 front edges | #8F917C underside |
| Rear body back wall | very dark | #2C2D25 | | |
| Top block, side panels | dark olive | #5E604E, #616350 | #6A6C58 | #595A49 |
| Cage plates, rails | near-black olive | #3B3C34 | #42433B | #383931 |
| Rear vent frame and cross | dark olive | #515243 to #5D5E4D | #606250 | #4E4F40 |
| Muzzle cap | olive | #616351 | #646653 | #515241 |
| Muzzle holes | near-black | #1F1F1B | | |
| Roof sheets | near-black | #3B3C34 | #404139 | #37382F |
| Handles | light olive | #8B8D78 | #8F917B | #6B6D5D (ends) |
| Pads, foot plate | dark olive | #565847 | #5C5E4C | |
| Side rod body | dark / olive | #3F4035 to #686A56 | | |
| Lens | white | #FFFFFF | | rim #BDBEB7 |
| Rod eye | red | #FF0000 (one pixel) | | |
| Skull emblem | near-white | #E0DEDE | #E6E6E6 | #2D2E26 |

Shading style: flat, matte army-green with small noise; no gradients. Light panels have a **bright 1-pixel edge** (#A9 to #BB) and a darker bottom row, which makes the slabs read as separate bars. The dark cage and hood are nearly black-green and contrast strongly with the olive tubes. Only the lens, the skull and the red dot are bright.

## 6. In-game presentation

All transforms apply in block units after the 1/16 scale. The model is first rotated 180 degrees about X; additionally the model code turns the whole thing 180 degrees about the vertical axis before drawing, so the muzzle swings to the far side of the player's view. The builder should orient it so the **muzzle is forward**; the skull flank faces away from the player in first person.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 2.1 | hold shift (0.25, -0.65, -0.6). Aimed: shifts (0, -1.5, -0.4). |
| First person firing | roll up to about 12 degrees about the axis across, plus a forward-back shift up to 0.4 | mid-animation. |
| First person reloading | roll up to 60 degrees and yaw up to -30 degrees at mid-reload, shift about (-0.08, 0.2, -0.85) from hold | |
| Third person (held) | yaw 225 degrees, scale 1.5 | offset (-0.9, -1.8, 0). |
| Ground (dropped) | yaw 90 degrees, scale 1.0 | lowered by 0.25. |
| Inventory icon | yaw 180 degrees, scale 0.5 | offset (0.2, 0.1, 0.1). The icon is the 3D model. |

**Muzzle flash:** none is drawn by this renderer (the flash call is empty). Rocket fire is carried by the projectile and sound.

## 7. Motion

- The **model has no moving parts.** All motion is whole-gun transforms.
- **Recoil (third person):** the gun rolls back about 20 degrees about the Z axis (pivot about 0.9 behind and 0.3 above the origin) in the first quarter of the first half, then settles.
- **Reload (third person):** tilts 40 degrees about X and 60 degrees about Y, eased.

## 8. What to avoid

- **Four separate round barrels.** The tubes are one solid 11x11 block with notches; they read as four by the groove cross and the four-hole face.
- **A solid cage.** The cage must be an open lattice or frame with see-through windows.
- **A solid rear.** The vent has four open quadrants showing a dark wall.
- **Bright colors.** Keep the whole body olive/near-black. Only the lens, the skull, and a single red pixel are bright.
- **Putting the side rod on the top.** It is on the flank (Z-), 10.5 off the axis, held by a slanted strut.
- **One handle.** There are two: a diamond post at the rear and a flat post with a foot at the front.

## 9. Acceptance checklist

- [ ] Overall length about 49, height about 22, body width 12.
- [ ] Tube block 24 long x 11 x 11, with 1x1 corner notches and mid-side grooves.
- [ ] Muzzle cap 4 long x 12 x 12 with four dark 4x4 holes in a 2x2 grid and a 2-wide divider cross.
- [ ] Dark cage, 16 long, 12 x 12 outer, with see-through lattice and four 1x2 corner rails.
- [ ] Rear body 17 long x 11 wide x 9 tall, with top and bottom plates, a raised 6x10x2 top block and 6x6 side panels.
- [ ] Skull emblem (about 6x7) on the Z- flank.
- [ ] Rear vent 12 x 12 x 4 with a plus-shaped cross, T-caps and four open quadrants over a dark wall.
- [ ] Roof hood sheets at the rear (zero thickness) with 2x2x1 buttons on the walls.
- [ ] Two handles: 2.8-across diamond post at X -8, and a 3x2 post with a 4x3 foot at X 3 to 6.
- [ ] Side rod on the Z- flank at Z -10.5: four diamond pieces 9 long in total, white lens, red dot, held by a slanted strut.
- [ ] Olive base #898B76, cage near-black #3B3C34, body dark olive #5E604E.
- [ ] No part floats; every box overlaps or touches a neighbor (the roof sheets are the one exception, see section 10).

## 10. Unsure

- **Roof sheets.** They float 1 unit above the top block in the data; I could not tell what holds them. A builder may add a 1x4x1 spacer post at each end, or leave them floating; they are nearly invisible from most angles.
- **Which flank.** The skull and side rod are both on the Z- flank in model space. Because the model is turned 180 degrees about the vertical axis when drawn, I could not tell which hand side that is in game; I believe the skull faces the player's right in third person, but treat this as unverified.
- **Tube groove cross.** The data gives a solid 11x11 block with small notches rather than four separate tubes. The "four tubes" read comes from the texture bands and the front face holes. Four separate 5x5 boxes with 1-unit gaps would look different from the original.
- **Cage lattice.** Exact hole pattern is read from a low-resolution texture; the rough pattern (frame with five vertical bars, one window on the sides) is close, not exact.
- **First-person framing numbers** come from formulas read, not seen in game. Treat the offsets as approximate.
- **Camo variants:** none. One texture only.
