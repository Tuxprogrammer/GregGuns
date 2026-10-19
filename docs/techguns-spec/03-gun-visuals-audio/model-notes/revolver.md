# Revolver: clean-room model notes

Design notes for rebuilding the Techguns **Revolver** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelRevolver](../gun-models.md#modelrevolver-6432-family-x-20-boxes-1-part), [revolver sheet](../gun-sheets.md#revolver-revolver-and-golden-revolver), [rendering framework](../rendering-framework.md), [Techguns revolver stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = one texel at the native 64x32 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention), with **Y = 0 at the top face of the top strap**. **Z = 0 is the gun's mid-plane**; sizes across Z are given as total width, centered. **X = 0 is the back face of the main frame block**, which is also the front face of the recoil shield. Rounded to the nearest half unit unless it matters.

## 1. Gestalt

A big, silvery, old-west **single-action six-shooter**, drawn with chunky Minecraft-scale boxes. At a glance you read: a long, light-metal barrel that is clearly longer than the body; a fat drum in the middle with seams; a thin rod hanging under the barrel; a small dark hammer at the back; and an **orange-brown wooden grip that sweeps down and back**. The attitude is classic and slightly oversized: a "hand cannon from a western", not a modern service revolver. The metal is bright and low-contrast, and the only strong color contrast is the warm brown grip.

Defining features in order of visual importance:

1. **Light polished-steel body and barrel** against an orange-brown grip. Two materials, two clear colors.
2. **Long diamond-section barrel** with a shorter ejector rod below it. The barrel is about as long as the frame, drum and grip-top combined.
3. **A fat, wider-than-the-frame drum** with painted flutes or seams. It bulges out to both sides.
4. **Curved wooden grip**, built from stepped slabs that lean back, and a small dark hammer perched behind the frame.
5. **A stubby, open trigger guard**: two short posts under the frame, with nothing connecting them when seen from the side.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (grip heel to muzzle) | about 28 |
| Total height (front-sight tip to grip bottom) | about 14.3 |
| Total thickness (drum width) | 5 (everything else is 2 to 3) |
| Length : height : thickness | about 5.6 : 2.9 : 1 |
| Barrel length | 14, about **50 percent** of the total length |
| Barrel vs frame | about 1.5 times the frame block's length (9) |
| Frame (top strap to lower block) height | 5 (6 including the belly plate) |
| Grip | reaches about 8 units below the frame's underside, so the grip is taller than the whole frame |
| Grip thickness | 2 across Z (slimmer than the frame at 3 and the drum at 5) |

How the pieces sit along the length, from the rear: the grip heel and hammer spur sit about 4 to 5 units behind the frame; then the recoil shield; the 9-long frame (with the drum in its middle); then 14 units of barrel beyond the frame's front face. The ejector rod starts at the frame front and ends 6 units short of the muzzle.

The **top line is continuous**: the top of the top strap, the highest point of the diamond barrel and the base of the front sight form one nearly straight line from the shield to the muzzle. Only the sight pokes above it, by about 0.3 to 0.5 unit.

## 3. Part by part

All parts are boxes. Z widths are centered on the mid-plane. "Touches" means faces in contact or overlapping.

### Barrel
- A square bar 2x2 in section and **14 long**, turned 45 degrees about its own axis so it shows a **diamond**: edge-on, about 2.8 units tall and 2.8 wide.
- Starts at the frame's front face (x about 9) and runs to the muzzle (x about 23).
- Its axis is about 1.5 units below the top of the top strap, so the top corner of the diamond is flush with the strap's top. The diamond is slightly wider (2.8) than the 2-wide strap and slightly narrower than the 3-wide upper frame block.
- Touches: buried into the frame's front face; the front sight sits on its top corner; the rod's top corner nearly touches its bottom corner.
- No taper, no muzzle crown, no rib, no underlug, no notches. It is one clean bar.

### Ejector rod
- A 1x1 square rod, **8 long**, also turned 45 degrees (diamond, about 1.4 across).
- Starts at the frame's front face and ends about 6 units short of the muzzle.
- Axis about 3.5 below the strap top, directly under the barrel's axis. The rod's top corner meets the barrel's bottom corner almost exactly (an overlap or gap of 0.3 unit at most). Seen from the side this reads as a thin dark seam or hairline gap between two parallel bars; seen from the front and top, the two diamonds form a small notch between them. It must **not** read as one thick merged bar and it must **not** hang visibly far below the barrel.
- Touches: the frame's front face only (and the barrel at the tips).

### Front sight
- A small block about 2 long, 1 tall, 1 wide, placed about 0.5 to 1 unit behind the muzzle (x about 20.5 to 22.5).
- Sits on the barrel's top corner and rises about 0.3 to 0.5 above the top line.
- Plain block: no notch, no hood.

### Top strap
- A flat bar, **9 long, 1 tall, 2 wide**, along the whole length of the frame at the very top (y 0 to 1).
- Directly on top of the upper frame block, narrower than it by half a unit each side. It overlaps the top slice of the drum, which it bridges over.
- No rear sight, no notch.

### Frame
Three stacked boxes, all but the belly plate 9 long (x 0 to 9):
- **Upper block:** 3 wide, 2 tall (y 1 to 3). The widest part of the frame; it forms the visible "shoulders" beside the top strap.
- **Lower block:** 2 wide, 2 tall (y 3 to 5), directly under the upper one. The frame is therefore slightly narrower toward the bottom: a stepped profile seen from the front.
- **Belly plate:** 2 wide, 1 tall, **6 long**, under the lower block (y 5 to 6) from about x 2 to x 8. It lets the trigger guard attach and makes the frame feel deeper in the middle.
- The frame has no window cut-outs; the drum is simply a wider thing overlapping its middle part.

### Recoil shield
- A thin vertical plate, **1 long, 4 tall, 2 wide**, at the frame's back face (x -1 to 0), from the top of the upper block down to the bottom of the lower block (y 1 to 5).
- The back of the frame is therefore a flat wall; the hammer and grip meet it from behind.
- Same steel as the frame.

### Cylinder drum
- About **5 long** (x 1.5 to 6.5): a 1.5 gap behind it, then 2.5 of frame in front of it.
- **How it reads as round with only boxes:** three stacked slabs form an octagon-like cross-section. The middle slab is **5 wide x 2 tall**; the top and bottom slabs are **4 wide x 1 tall**, each stepped in by half a unit per side. Together: 5 wide, 4 tall, with cut corners. The drum sticks out about 1 unit past the frame on each side and sits within the frame's height (y about 0.5 to 4.5).
- Vertically it is centered about 1 unit below the barrel's axis.
- It is the **widest** part of the whole gun.
- The flutes and chamber hints are **painted** on the sides (section 5); the geometry has no cut grooves.
- Touches: it is embedded in the frame's middle; the strap and the lower block overlap its top and bottom.

### Hammer
- A small dark block that rises from the grip's top, behind the recoil shield.
- **Body:** 2 long, 2 tall, **1 wide** (a thin plate, centered on the mid-plane), at x about -2.5 to -0.5, y about 1.5 to 3.5. Its front half overlaps the shield by about half a unit, and its lower part overlaps the grip crown, so it is fully attached (not floating).
- **Spur:** a 1x1x1 cube on the rear top corner of the body (x about -3.5 to -2.5, y about 1.5 to 2.5), jutting backward so the hammer reads as a hook.
- The hammer's top is about 1.5 units **below** the top strap's top, and below the shield's top. It is never the tallest thing at the back.
- A **different material** from the frame: dark gunmetal.

### Trigger guard
- Two stubby posts and a bottom plane, under the belly plate.
- **Two posts:** each 1 long, 2 tall, 2 wide (y 6 to 8), one at the rear (x about 2 to 3) and one at the front (x about 6 to 7), so the opening between them is **3 long** and 2 tall.
- A **floor** of zero thickness (a flat 3x2 plane at y 8) between the posts. Because it has no thickness it is invisible from the side and visible only from above or below. In side views the guard therefore looks like **two short legs hanging under the frame with an open gap between them and no bottom line**.
- **Trigger:** the original has **no separate trigger part**. See section 10.
- The rear post touches the front face of the grip's web slab.

### Grip
Wood, 2 wide (Z) throughout, built from four stacked slabs that step backward as they go down. Values are y below the top strap's top.

| Slab | y range | x range | Size (x by y) | Note |
|---|---|---|---|---|
| Crown | 3 to 5 | about -3 to -1 | 2 by 2 | right behind the shield; the hammer rises out of its top; this is the bump behind the frame |
| Web | 5 to 7 | about -4 to 2 | **6 by 2** | the widest slab; it runs forward under the frame's rear to meet the guard's rear post |
| Neck | 7 to 9 | about -4 to 0 | 4 by 2 | |
| Butt | 9 to 14 | about -5 to -1 | 4 by **5** | the longest, bottom slab |

How the curve and rake work: the back edge steps back as you go down (about -3, -4, -4, -5), and the front edge drops back after the web (-1, 2, 0, -1). The slab centers drift about -2, -1, -2, -3 in x. The web is the front-most part, tucked under the frame; the butt's back face is a full unit behind the neck's. The net effect is a grip that **curves like a flattened S**: a bulge at the web, a taper at the neck, and a backward lean in the butt. The average rake is about 10 to 15 degrees back from vertical. It is *not* a single straight or rotated slab.

## 4. Silhouette checks

- **Side (the main view):** a long horizontal body. From the left: the grip's heel, with a small brown bump and a dark hammer hook at the back top; the grip stepping down and back; the frame and drum as a tall rectangle (5 to 6 high); a long light bar (the barrel) running right; a small sight bump at the muzzle; a shorter, thinner bar under the barrel (ejector rod) ending well before the muzzle; **two short legs** under the frame (the guard) with open space between them and no bottom line. The grip is brown, everything else is light gray. Negative space: the hairline seam under the barrel; the slot between the guard posts; the notch between the grip's front edge and the frame's underside.
- **Top:** a thin spine. A narrow barrel (about 3) over the whole front half; a sudden **5-wide bulge** in the middle (the drum, with the 3-wide frame and 2-wide strap visible as a lighter spine through it); behind it the narrow brown grip and the small dark hammer centered on the spine. The rod is hidden under the barrel, with only slivers showing at the diamond notches.
- **Front:** an upright, roughly round (octagon-ish) blob 5 wide and about 5 tall: the drum, with the 2-wide strap on top. The barrel's diamond sits above the center, the rod's diamond a little below it, both inside the drum's outline. The grip is a narrow brown column hanging below, centered, about 2 wide and taller than the blob is wide.
- **Back:** the same octagon blob (shield and drum from behind); the small dark hammer at the center; a brown grip column below it, 2 to 4 wide, about 8 tall.
- **Underside:** a long thin bar: the rod and barrel overlap into one stripe in the front half; the drum's bulge in the middle; the guard floor as a small plane; the brown grip at the back.

## 5. Materials and palette

The original texture is bright and low-contrast: nickel or polished steel, a warm wooden grip and a dark hammer. The values below were sampled from its regions.

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Barrel, rod, front sight | polished steel (nickel-ish) | #A9A6A2 | #BDBBB8, up to #C8C6C3 on the top ridge | #8F8C88 |
| Frame (upper, lower, belly), strap, shield | same polished steel | #9E9A97 | #BDBBB8 | #76746D |
| Drum | same steel, more broken up | #9E9A97 | #B5B2AE | seams #6F6D69 to #76746D |
| Hammer | dark gunmetal | #575553 | #5C5B59 | #52504E |
| Grip (all slabs) | warm wood, walnut-like | #77491F | #B7712F (orange panels) | #6A431F |
| Grip screw | two small dark dots | near-black brown | | |

Shading style:
- **Metal:** flat, light, with subtle per-pixel noise (a few levels of gray), like a satin finish. It is **not** dark and **not** blued. The drum and the frame are close in color (the drum is separated by seams and shape, not by hue). Highlights sit on top faces and edges only; no gradients, no scratches, no wear.
- **Drum:** the sides carry a few **vertical dark seam lines**, roughly 2 or 3 per visible side, separating long lighter panels. These are the painted flutes. A faint ring pattern on the front face hinting at six chambers is optional.
- **Barrel and rod:** a lighter strip along the top and a slightly darker strip beneath; very little noise. The barrel reads lighter than the frame.
- **Hammer:** flat dark gray, uniform.
- **Grip:** mostly mid-brown with darker streaks of grain running vertically, and **lighter orange panels** on the lower side faces of the lower slabs. A small dark pin dot on each side near the web slab (a grip screw).

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 1.5 | idle offset about (0.20, -0.50, -0.45). The camera sees the gun nearly side-on, turned 45 degrees so you see its side plus the **back** of the frame (shield, hammer, grip heel). The muzzle points away and slightly across. |
| First person firing | roll about the Z axis, up to about 17 degrees at mid-animation | plus a shift of about 0.25 along the gun axis and 0.2 up. A strong "muzzle flip". |
| First person reloading | roll 25 degrees, yaw -20 degrees, shift about (0.6, 0.3, 0.25) from idle | smooth rise and fall over the reload. |
| First person aimed | no scope and no zoom overlay | the revolver has no sight beyond the front post. |
| Third person (held) | yaw 225 degrees, scale 0.85 | offset (-1.4, -0.8, 0). The grip sits in the hand and the muzzle points forward. |
| Third person aimed | extra offset (0, -0.05, 0.2) | |
| Ground (dropped) | yaw 90 degrees, scale 0.8 | lowered by 0.35 so it lies on the ground. |
| Inventory icon | yaw 180 degrees, scale 1.0 | offset (-0.5, -0.2, 0); drawn side-on, filling most of the slot. There is no sprite; the icon is the 3D model. |

**Muzzle flash point:** the front of the barrel on its axis (model position x about 23, y about 1.5 below the strap top, z = 0), a flash sprite at roughly 1.85 times scale. In third person the flash is drawn slightly forward of the hand at about (0, 0.05, -0.45).

**Akimbo:** the same model is used for two guns. In first person the second (left) gun is shifted about 1.75 blocks sideways after scaling. The guns **alternate** shots by ammo parity; only the gun that fires animates, and the flash is drawn at that gun's own muzzle. On a player's body the off-hand gun is drawn at scale 0.3 with a 90 degree yaw at about (2.7, -0.55, 0.15). Its recoil and reload tilts are 30 and 40 degrees.

## 7. Motion

- The **model has no moving parts.** The drum does not rotate, the hammer does not cock and the rod does not move. All motion is whole-gun transforms.
- **Recoil:** 6-tick fire delay (the whole-gun flip above), flash 5 ticks. Third person: the gun pitches up about 30 degrees around the wrist (a point about 1 block behind and 0.25 up from the model origin) in roughly the first quarter of the first half of the animation, then settles back.
- **Reload:** 45 ticks (2.25 s). First person: the gun tips and slides up and aside, hangs near the middle, then drops back. Third person: it tilts about 40 degrees about the wrist at mid-animation, then returns.
- **Optional (not in the original):** the builder may keep the drum and hammer as separate named parts so later code can animate them, but the rest pose must match this document.

## 8. What our earlier attempts got wrong

- **A floating hammer.** Overlap the hammer with the shield and grip crown by at least 1 unit; never let it hang as a lone box.
- **A cube cylinder.** Do not use a 7-unit cube wider than the frame. Use the 5-long, 5-wide, 4-tall octagon-ish drum with painted seams.
- **A stubby barrel with a sawtooth top.** Use a clean, smooth diamond bar, 14 long. No notches, no rail look.
- **A grip detached from the frame and mottled gray-brown like stone.** The grip's top must tuck under and behind the frame, and the wood must be warm brown with orange panels and streaky grain, not a checkerboard.
- **Dark gray metal everywhere, with the drum and frame the same color.** Make the metal bright and light. The hammer alone is dark.
- **No trigger guard or ejector rod.** Include the rod under the barrel and the two-post guard.
- Do not add things the original lacks: no underlug, no rear sight, no top rail, no fins.

## 9. Acceptance checklist

- [ ] Length : height : thickness is within about 10 percent of 5.6 : 2.9 : 1.
- [ ] Barrel is about 50 percent of the total length (14 of 28) and about 1.5 times the frame length.
- [ ] Barrel is a diamond-section bar (a square bar turned 45 degrees) with no notches.
- [ ] Ejector rod is a thinner diamond bar about 8 long, ending well short of the muzzle; its tip and the barrel's touch or sit within 0.3 unit, with a visible seam in the side view.
- [ ] Top line (strap top, barrel top, sight base) is straight; only the front sight pokes above it, by under half a unit.
- [ ] Drum is the widest part (about 5 across), about 5 long and 4 tall, with cut corners and visible painted vertical seams; it sticks out about 1 unit on each side of the frame.
- [ ] Frame is stacked: a 2-wide top strap on a 3-wide upper block, then a 2-wide lower block, then a belly plate.
- [ ] Recoil shield is a thin, tall plate on the frame's back face.
- [ ] Hammer is dark, attached to both the shield and the grip crown (overlap at least 0.5 unit), has a rear spur, and sits lower than the top strap.
- [ ] Trigger guard is two short posts with an open gap and no bottom line from the side.
- [ ] Grip is four stepped slabs, 2 wide, with the butt's back face at least 1 unit behind the neck's; overall rake about 10 to 15 degrees; the grip extends below the frame by more than the frame's height.
- [ ] Metal is light (about #9E9A97 to #A9A6A2), the hammer is dark (about #575553), the grip is warm brown (about #77491F) with orange highlights.
- [ ] Grip carries two small dark pin dots near its upper part.
- [ ] Side silhouette matches section 4 (two legs under the frame, barrel then rod, brown bump behind the frame).
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Trigger.** The original model has no distinct trigger; the thin plane between the guard posts is the only candidate and it is invisible from the side. If the builder adds a tiny dark trigger (at most 1 wide, 2 tall) inside the opening, that is a harmless deviation, but it is not in the original.
- **Gap between barrel and rod.** By the numbers the two diamonds' corners touch (overlap about 0.1 unit). In renders it reads as a hairline. A gap of 0 to 0.3 is fine.
- **First-person framing numbers** come from formulas read, not seen in game. Treat the offsets as approximate.
- **Drum chamber dots** on the front face are my reading of a very low-resolution texture, so they are optional.
