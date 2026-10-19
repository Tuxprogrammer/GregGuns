# Nether Blaster: clean-room model notes

Design notes for rebuilding the Techguns **Nether Blaster** (the "Cyberdemon Blaster") look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelCyberdemonBlaster](../gun-models.md), [rendering framework](../rendering-framework.md), [revolver notes (template)](revolver.md).

**Units and axes used in this document.** One unit = one texel at the native 64x64 scale = 1/16 block. The muzzle points toward **+F** (forward). **Y points down** (Minecraft model convention), with **Y = 0 on the center line of the three-block barrel body**. **X = 0 is the gun's mid-plane**; widths are given as total width, centered, unless a range is given. **F = 0 is the middle of the center block.** Rounded to the nearest half unit unless it matters.

## 1. Gestalt

A **chunky, dark-gray sci-fi cannon**, built as a short fat barrel body of three separate blocks, with a **glowing orange-red core showing through the gaps and at the muzzle**, a lean rail frame on top and bottom, and a **skeletal, upward-raking stock** at the back. At a glance you read: a monochrome gray gun with a hot orange heart; a segmented body (block, slit, block, slit, block); a short flat muzzle plate with an orange eye; two thin dark fins on the roof; and an open cage stock that rises toward the rear like a fan. It is stubby and heavy, closer to a demonic "BFG" than a rifle.

Defining features in order of visual importance:

1. **Orange-red emissive glow**: two thin glowing bands in the body gaps (5 wide x 4 tall each, 1 unit long) and a glowing 2x2 eye in the muzzle plate. Everything else is neutral gray.
2. **Three-block segmented body**: a front block, a center block with side plates, a rear block, separated by 1-unit slots that expose the glow.
3. **Skeletal stock**: a solid collar followed by an open frame of four corner rails, the whole thing pitched up about 22 degrees toward the rear, with a thin plate ridge on its top and underside.
4. **Roof fins and rails**: a narrow top plate with two zero-thickness dark fins and two dark rails.
5. **Long lower spine** that runs the full body length and past the rear block, with two dark rails.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (stock rear to muzzle plate) | about 32 (23 without the stock) |
| Total height (stock rear-top corner to spine bottom) | about 13 |
| Total width | 8 (stock and the center-block side plates); the body blocks are 6 |
| Length : height : width | about 4 : 1.6 : 1 |
| Barrel body (front block front face to rear block rear face) | 16 (F -8 to 8), 50 percent of total |
| Body block cross-section | 6 wide x 5 tall |
| Lower spine | 19 long x 4 wide x 3 tall |
| Stock | 10 long along its own axis, 8 x 8 section |

Along the length from the rear: stock end (F about -23) rising up and back; collar at F -14; the 7x7 rear housing (F -14 to -8); rear block (F -8 to -4); slot; center block (F -3 to 3); slot; front block (F 4 to 8); muzzle plate (F 8 to 9).

The **top line** is stepped: the 1-unit top plate sits above the blocks (Y -3.5 to -2.5) from F -8 to 6; the fins rise another 0.5 above that. The stock is the highest point at its rear end (Y about -7.5).

## 3. Part by part

All parts are boxes unless stated. Y is down from the barrel-body center line.

### Barrel body: three blocks
- **Front block:** 6 wide, 5 tall (Y -2.5 to 2.5), 4 long (F 4 to 8).
- **Center block:** 6 wide, 5 tall, 6 long (F -3 to 3).
- **Rear block:** 6 wide, 5 tall, 4 long (F -8 to -4).
- The slots between them are each 1 long (F 3 to 4 and F -4 to -3). They are not empty: they show the glow core (below).
- **Side plates** on the center block only: one each side, 1 thick, 4 tall (Y -2 to 2), 6 long (F -3 to 3), flush against the block sides (X +-3 to +-4). The center section is therefore 8 wide, the front and rear blocks 6 wide. The plates carry a painted horizontal vent slot (section 5).

### Glow core
- A 5 wide x 4 tall x 8 long box (F -4 to 4, Y -2 to 2, X +-2.5), **fully inside the blocks** except in the two 1-unit slots. In each slot it is visible as a 5x4 glowing band, recessed half a unit from the block sides and 1.5 from the side plates. Seen from the side the slots read as thin vertical orange lines; seen from the top they are hidden by the top plate.
- A second glow bar (two pieces) lies low in the spine: 3 wide, 3 tall, from F -7 to 7, X +-1.5, mostly buried. It adds a faint orange strip between the lower rails in the slots only. It is minor; see section 10.
- **Muzzle eye:** a 2x2x1 glowing square at the front of the muzzle plate (F 8 to 9, Y -1 to 1), centered, set in the 4x4 plate below.

### Muzzle plate
- 4 wide x 4 tall x 1 thick (F 8 to 9, Y -2 to 2) on the front block's front face. Plain gray; the 2x2 orange eye fills its center, leaving a 1-unit gray frame.

### Top plate, fins and rails
- **Top plate:** 4 wide, 1 tall, 14 long (F -8 to 6), Y -3.5 to -2.5, centered. It sits on the three blocks, flush with the rear block's rear face and stopping 2 short of the front block's front face.
- **Rails:** two 1x1x13 bars along the top-plate edges (X +-1.5 to +-2.5, Y -3 to -2, F -7.5 to 5.5). They are half a unit lower than the plate and stick out half a unit past its edges. Very dark gray.
- **Fins:** two **zero-thickness** vertical sheets, 10 long (F -4.5 to 5.5), 1 tall (Y -4 to -3), at X = +-1. They stand on the top plate and rise 0.5 above it. They are visible edge-on from the top as thin lines and from the side as dark strips. Draw them double-sided and dark.

### Lower spine
- 4 wide, 3 tall (Y 2.5 to 5.5), **19 long** (F -11 to 8). It carries the blocks on its top face and extends 3 behind the rear block (to F -11), where it is tucked into the housing and the stock collar.
- **Lower rails:** two 1x1x15 bars at X +-1.5 to +-2.5, Y 2 to 3, F -7.5 to 7.5, flanking the spine's top. Dark gray. They stick out half a unit past the spine's sides.
- **Tail stub:** 4 wide, 1 tall, 2 long (F -13 to -11, Y 3.5 to 4.5) behind the spine, tucked into the housing and collar.

### Rear housing
- A fat block 7 wide x 7 tall x 6 long (X +-3.5, Y -3.5 to 3.5, F -14 to -8) directly behind the rear block. It is taller and wider than the body blocks (1 unit more all around). Its sides carry a recessed painted panel. The stock grows out of its rear.

### Stock (skeletal, raked)
Built as one group that **pivots about its front-bottom edge** at (F -14, Y 3.7) and is **pitched 22 degrees** so the rear end rises (the stock axis climbs as it goes back). Dimensions measured along the group's own axis:
- **Collar:** solid, 8 wide x 8 tall x 3 long, at the front of the group. Fully painted, mid-gray.
- **Cage:** directly behind the collar, 8 wide x 8 tall x 7 long, but **open**: only the four long edges are solid, as **2x2 rails running the 7-unit length**. The faces between the rails are transparent, and the front and rear faces are open. You see through the stock from the side and top, and the rear is a square 4x4 hole framed by the rails.
- **Ridge plates:** two 4-wide x 1-thick x 6-long strips, one on top of the whole stock and one under it, centered, starting 1 unit behind the stock's front face and ending 3 before its rear end.
- Resulting corners (side view): front face goes from (F -14, Y 3.7) bottom to (F -11, Y -3.7) top (leans forward at the top); rear face goes from (F -23.3, Y -0.05) bottom to (F -20.3, Y -7.5) top (the butt face leans back at the bottom). The top edge climbs from Y -3.7 to -7.5 over about 9 units.

## 4. Silhouette checks

- **Side:** a long low body (three blocks with thin vertical orange slits between them), a flat muzzle plate with a small orange eye, two thin dark bars above and two below; a longer spine under it that continues behind the rear block; behind that, a fat housing, then a **fan-like skeletal stock** pitched upward at the rear. Negative space: the stock's open middle is see-through; the slots are filled with orange, not empty.
- **Top:** a narrow spine (the 4-wide top plate with two dark fin lines on it), widening to 6 at the blocks and 8 at the center plates; behind it, the housing 7 wide, then an 8-wide stock showing its open top as a center channel between two rails.
- **Front:** a squarish 6x5 block with a 4x4 gray plate and an orange 2x2 eye in the center, a narrower spine below, and the fins above.
- **Back:** the 8x8 open frame of the stock (four rails) around a view into the housing, with the 7x7 housing behind it.
- **Underside:** the spine with its two rails, the stock's lower rails, and a 4-wide ridge plate.

## 5. Materials and palette

The texture is almost entirely **neutral gray** except the glow parts, which are saturated orange-red.

| Part | Material | Base | Highlight | Shadow / detail |
|---|---|---|---|---|
| Front and rear blocks | satin dark-gray metal | #7F7F7F | #A9A9A9 on the rear block's side edges | #626262 |
| Center block | same, slightly darker | #787878 | #888888 | #636363 |
| Side plates | gray with a vent slot | #797979 | #AAAAAA top and bottom rows | slot #252525 to #333333 |
| Spine | gray | #777777 | #959595 | #3B3B3B (small cut-outs on its sides) |
| Top plate | gray, dark top edge | #7A7A7A (sides) | #8B8B8B | top #696969 with a #484848 edge |
| Rails, fins | near-black gray | #424242 to #4C4C4C | #5A5A5A | #3E3E3E |
| Rear housing | gray with a recessed panel | #757575 | #A2A2A2 | panel marks #1F1F1F |
| Stock collar and rails | gray | #7B7B7B (collar), #787878 (rails) | #A1A1A1 | #4F4F4F |
| Ridge plates | gray | #777777 | #979797 | #606060 |
| Muzzle plate | gray | #787878 | #919191 | #6D6D6D |
| **Glow parts** | emissive orange-red | **#FF4500** | #FF9100 (hot spots) | #FF0C00 (deep red) |

Shading style:
- **Metal:** flat, low-contrast gray with per-pixel noise of a few levels (like cast metal), no gradients. Edges are 1 pixel lighter. There is **no color tint** in the gray (pure neutral).
- **Side plate (center block):** from top to bottom, a bright edge row (#A0A0A0 range), a very dark slot row (#252525), a dark-gray row (#555555), a bright edge row. It reads as a horizontal vent.
- **Glow:** mottled fire texture: base #FF4500 with streaks from deep red (#FF0C00) to amber (#FF9100). The slits and the eye must be **fullbright** (not affected by world light).
- **Stock rails:** plain gray; the transparent faces use a cut-out (alpha) texture.

## 6. In-game presentation

All transforms apply in block units after the 1/16 scale. The model is first rotated 180 degrees about X.

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw 225 degrees, scale 2.0 | then a hold shift of (-0.85, -0.20, -0.75). No scope or zoom. |
| First person firing | pitch up by up to about 10 degrees (about X, peak at mid-animation) | plus a backward kick of about 0.25 along the gun axis. |
| First person reloading | pitch of up to 50 degrees at mid-reload, eased in and out | hold shift unchanged. |
| Third person (held) | yaw 135 degrees, scale 1.35 | offset (0.20, -0.25, 0.07). |
| Ground (dropped) | scale 0.8 | lowered by 0.45. |
| Inventory icon | yaw 90 degrees, scale 0.68 | offset (0.3, 0, 0); drawn side-on. The icon is the 3D model, with its glow. |

**Muzzle flash:** an additive "blaster" flash sprite at about 1.5 scale, drawn at roughly (-0.72, -1.0, 1.35) in block units from the model origin, lasting 10 ticks. Fire color is orange-red to match the glow.

## 7. Motion

- The **model has no moving parts.** All motion is whole-gun transforms.
- **Recoil (third person):** the gun tips back about 30 degrees (pivot about 1 block behind the origin) in the first quarter of the first half of the animation, then settles.
- **Reload (third person):** tilts 40 degrees about the same pivot, eased.
- **Glow:** constant, not pulsed.

## 8. What to avoid

- **A single solid barrel.** Keep the three blocks separated by slots that show orange.
- **A tinted body.** The gray must be neutral; only the glow is colored.
- **A solid stock.** The cage must be open (four 2x2 rails), and raked up at the back, not horizontal.
- **Glow that is lit by the world.** The slits and the eye must stay bright in the dark.
- **Making the fins thick.** They are zero-thickness; use thin dark sheets or 0.1-unit slabs.
- Do not add a trigger, grip, scope or sights; the original has none.

## 9. Acceptance checklist

- [ ] Length : height : width within 10 percent of 4 : 1.6 : 1.
- [ ] Three body blocks of equal 6x5 section, with 1-unit slots between them.
- [ ] Center block carries 1-thick side plates (8 wide overall) with a painted vent slot.
- [ ] Orange-red glow visible in both slots (5x4) and as a 2x2 eye in the 4x4 muzzle plate; all fullbright.
- [ ] 4-wide top plate with two dark rails and two thin dark fins, stepping up toward the center.
- [ ] 19-long spine under the body, with two dark rails, extending 3 behind the rear block.
- [ ] 7x7x6 rear housing behind the rear block.
- [ ] Stock is pitched about 22 degrees up at the rear, with a solid 3-long collar and an open 7-long cage of four 2x2 rails.
- [ ] 4-wide ridge plates on top of and under the stock.
- [ ] Body palette neutral gray (about #7F7F7F); rails and fins near-black (#424242); glow #FF4500.
- [ ] Highest point is the stock's rear-top corner (about 7.5 above the barrel line).
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Hidden glow in the spine.** Two 3x3 glow bars are buried in the spine and only show through the slots. They are cheap to include and harmless to omit.
- **Muzzle eye overlap.** The 2x2 eye shares a plane with the 4x4 plate; the glow wins. A builder can offset the eye 0.05 unit forward.
- **Stock cage details.** Reading the open frame from an 8x8x7 box: the middle of every face is transparent; a few single pixels inside look like decoration. Treat the cage as four plain rails.
- **First-person framing numbers** come from formulas read, not seen in game. Treat the offsets as approximate.
- **Camo variants:** none. One texture only.
