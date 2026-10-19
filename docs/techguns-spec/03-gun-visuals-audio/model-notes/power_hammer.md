# Power Hammer and Advanced Power Hammer: clean-room model notes

Design notes for rebuilding the Techguns **Power Hammer** and **Advanced Power Hammer** look-alikes in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and textures. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout (the original is on a 64x64 sheet, which is a hint about its level of detail, nothing more).

**One geometry, two skins.** Both items use the same model in two parts: a static **body** and a sliding **piston and head**. The Advanced skin differs from the plain one in **one place only**, the striker plate at the front of the head (section 5). Everything else, in shape and in color, is identical.

Related facts: [ModelPowerHammer](../gun-models.md), [rendering framework](../rendering-framework.md), [Power Hammer sheet](../../02-guns/sheets-launchers-tools-grenades.md#power-hammer-powerhammer).

**Units and axes used in this document.** One unit = one texel at the native 64x64 scale = 1/16 block. The piston points toward **+X** (that is the "muzzle" and the striking end). **Y points down** (Minecraft model convention). **X = 0 is the rear edge of the side gear boss** (the round-ish plate on the crank side, section 3); **Y = 0 is that boss's top edge**. **Z = 0.5 is the gun's mid-plane**: almost everything is centered on z = 0.5. **+Z is the "crank side"**, the side that carries the pin and boss. Sizes across Z are total widths. Angles are rotations about Z unless said, with a positive angle swinging the lower end of a part toward -X (the rear). Rounded to the nearest half unit unless it matters.

## 1. Gestalt

A short, heavy, **pneumatic hammer**: a dark rust-brown boxy body with gray shell plates, a stock-like butt at the rear, a slanting pistol grip, and out of the front a **pale cream piston** (four thin rods around a fatter core) ending in a **dark head with a striking plate**. It reads as a chunky industrial tool: the body is dark and low-contrast, and the single bright element is the cream piston, which is also the only moving part.

Defining features in order of visual importance:

1. **The pale cream piston group** (four small diamond rods around a fat diamond core, 7 long) projecting from the front of the body, ending in a **dark head** (a stack of plates) with the striker plate at the very front.
2. **Dark rust-brown body** (about 22 long) with a lighter gray shell and a few small gray fittings.
3. **A butt assembly at the rear**, joined to the body by three short gray rods, with a gap you can see through.
4. **Slanted pistol grip** (15 degrees back) under the rear half of the body.
5. **A small silver block** on top, between two gray bracket plates, with a dark cap.
6. **A pin that sticks out sideways** from the crank side (6 long, diamond section), with a round-ish gray boss around its base.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length, rest pose (butt rear to head front) | **31** (x -16 to 15) |
| Body only (butt rear to front collar) | 22 (x -16 to 6) |
| Piston and head, rest pose | 9 (x 6 to 15), 29 percent of the length |
| Piston exposed length when at rest | 7 (x 6 to 13 for the rods, then the 2-long head) |
| Total height (cap top to grip bottom) | about 14.8 (y -4 to 10.8) |
| Body height (cap top to butt-bottom cap) | about 9.5 (y -4 to 5.5) |
| Total width of the body | **7** (z -3 to 4); the side pin reaches z 9.5 |
| Length : height : body width | about 4.4 : 2.1 : 1 |

Sequence along the length from the rear: the butt (3 long, x -16 to -13), three neck rods (3 long, x -13 to -10), a collar plate (1 long, x -10 to -9), the main housing (x -9 to 4, 13 long), the front flange and collar (x 4 to 6), then the piston group (rods x 6 to 13) and the head (x 13 to 15).

The **top line** is stepped: the butt's top cap is at y -2.5, the housing's top at -1 (with a brown strip at -2 to -1), the silver block rises to y -3, and its dark cap to y -4, which is the highest point. The **bottom line** is stepped too: the butt's bottom cap reaches y 5.5, the front flange reaches y 5.5, and the body between is at y 3 to 5; the grip hangs well below.

## 3. Part by part

All parts are boxes unless noted. Z widths are centered on z = 0.5. "Touches" means faces in contact or overlapping. Colors are in section 5.

### Body (static)

**Butt assembly (rear).**
- **Main butt block:** 2 long x 5 tall x 5 wide (x -15 to -13, y -1.5 to 3.5, z -2 to 3), brown.
- **Rear step:** 1 long x 5 tall x 3 wide (x -15.5 to -14.5, y -1.5 to 3.5, z -1 to 2), brown, set into the butt's rear face so the back is stepped (the block narrows to 3 wide at the very rear).
- **Top cap:** 2 long x 1 tall x 4 wide (x -16 to -14, y -2.5 to -1.5, z -1.5 to 2.5), brown, on top of the block, with its rear end 0.5 behind the step.
- **Bottom cap:** 2 long x **2** tall x 4 wide (x -16 to -14, y 3.5 to 5.5, z -1.5 to 2.5), brown. It hangs 2 units below the block, so the butt is taller at its bottom, a hooked butt-plate look.

**Neck rods (three, joining the butt to the body).**
- One **3 long x 2 x 2** rod (x -13 to -10, y 1 to 3, z -0.5 to 1.5) in the lower half, and two **3 long x 1 x 1** rods (x -13 to -10, y -1 to 0), one at z -1 to 0 and one at z 1 to 2. All mid-gray.
- The rods show as three thin bars with a gap of air between them (from the side you see through between the upper thin rod and the lower fat rod at y 0 to 1).

**Collar plate.** 1 long x 5 tall x 4 wide (x -10 to -9, y -1.5 to 3.5, z -1.5 to 2.5), gray. It closes the neck against the housing.

**Main housing.** Two overlapping parts:
- **Inner core:** 13 long x 4 tall x 5 wide (x -9 to 4, y -1 to 3, z -2 to 3). Dark rust brown, with a few near-black vent marks.
- **Outer shell:** 11 long x 4 tall x 6 wide (x -7 to 4, y -0.5 to 3.5, z -2.5 to 3.5). Gray. The shell is **0.5 wider on each side** than the core and **0.5 lower**, so the brown core pokes out 0.5 above the shell's top and shows at both ends (2 long at the rear, x -9 to -7), while the gray shell shows on both sides and 0.5 below. The result is a two-tone housing: gray flanks, brown top and rear.
- On the sides, near the middle of the shell, there are **two short black vertical slits** (vent marks), 1 wide, 2 tall, 2 to 3 apart.

**Under-rails.**
- **Brown strip:** 12 long x 1 tall x 4 wide (x -9 to 3, y 3 to 4, z -1.5 to 2.5).
- **Gray rail:** 8 long x 1 tall x 3 wide (x -5 to 3, y 4 to 5, z -1 to 2), under the strip.

**Top fittings (rear to front).**
- **Rear brown strip:** 2 long x 1 tall x 4 wide (x -9 to -7, y -2 to -1, z -1.5 to 2.5).
- **Rear bracket plate:** 1 long x 2 tall x 6 wide (x -7 to -6, y -2.5 to -0.5, z -2.5 to 3.5), gray.
- **Silver block:** 4 long x 2 tall x 4 wide (x -6 to -2, y -3 to -1, z -1.5 to 2.5), **light silver**, directly behind and in front of its brackets.
- **Dark cap:** 3 long x 1 tall x 3 wide (x -5.5 to -2.5, y -4 to -3, z -1 to 2), near black, on top of the silver block. It is the highest point of the model.
- **Front bracket plate:** 1 long x 2 tall x 6 wide (x -2 to -1, y -2.5 to -0.5, z -2.5 to 3.5), gray, matching the rear one. The silver block sits snugly between the two plates.
- **Front brown strip:** 5 long x 1 tall x 4 wide (x -1.5 to 3.5, y -2 to -1, z -1.5 to 2.5).

**Front end of the housing.**
- **Flange plate:** 1 long x **6 tall x 7 wide** (x 4 to 5, y -0.5 to 5.5, z -3 to 4), brown. It is wider than the housing (7 vs 6) and drops 2 below the shell's bottom, so it reads as a bulkhead at the front.
- **Muzzle collar:** 2 long x 4 tall x 6 wide (x 4 to 6, y -0.5 to 3.5, z -2.5 to 3.5), brown with black on its front face; the piston emerges from its front face at x 6.
- **Upper jaws:** two stacked plates at the top of the front end: 3 long x 1 tall x 4 wide (x 3 to 6, y -1.5 to -0.5, z -1.5 to 2.5) and, above it, 3 long x 1 tall x 3 wide (x 3 to 6, y -2 to -1, z -1 to 2), both brown, with black front faces on the wider one.
- **Lower jaws:** mirrored under the collar: 3 x 1 x 4 (x 3 to 6, y 3.5 to 4.5, z -1.5 to 2.5), then 3 x 1 x 3 (x 3 to 6, y 4 to 5, z -1 to 2). So the housing's front looks like a clamp with a rectangular opening for the piston.
- **Gusset fin:** a plate 4 long x 1 thick x **7 wide** (z -3 to 4), hinged at the top front of the flange (x 5, y -0.5) and **tilted 45 degrees**, so it rises toward the rear: its upper end is at about x 1.5, y -3.3. Brown. A small shed-roof shape on top of the front end.

**Crank side (the +Z face).**
- **Gear boss:** 3 long x 3 tall x 2 thick (x 0 to 3, y 0 to 3, z 2.5 to 4.5), gray. It sits on the housing's +Z flank.
- **Pin:** a bar **2 x 2 in section, 6 long along Z** (z 3.5 to 9.5), **turned 45 degrees** about its own axis so it shows as a diamond (about 2.8 across) from the side. Centered at x 1.5, y 1.5. It sticks out about 5.5 beyond the boss. Gray with a lighter tip.
- This is the only thing that breaks the 7-wide body width.

**Grip.**
- A **3 wide (X) x 6 long x 2 thick (Z)** slab (z -0.5 to 1.5), hinged at its top end (x -2, y 5) and **tilted 15 degrees** so its lower end swings toward the rear. The lower end ends at about y 10.8 and x -6.5 to -4. Mid gray.
- It attaches under the gray rail and the strip. There is no thumb rest, no texture groove.
- **Trigger guard (zero-thickness planes).** Three flat planes with no depth: a **3 x 2** plane under the body at y 7 (x -3.5 to -0.5, z -0.5 to 1.5), a **3-tall x 2-wide** plane facing front/back tilted 15 degrees near x -0.5 (y 4.1 to 7), and a small **2-tall x 1-wide** trigger plane tilted -12 degrees at x about -1.5 (y 4.5 to 6.5). Edge-on they vanish, so in the side view the guard is **invisible**: just the grip standing alone with a gap in front. Same as the revolver, they are visible only from front, back, top or bottom.

### Piston and head (the moving part, "part 1")

Everything in this group moves together along X.

- **Piston core:** a **7 long** bar of **3 x 3** section, turned **45 degrees** about its own long axis so it shows as a diamond (about 4.2 across). Axis at y 1.5, z 0.5. It spans x 6 to 13. Pale cream.
- **Four corner rods:** four **7 long x 1 x 1** bars, each also turned 45 degrees (diamond, about 1.4 across), x 6 to 13, parallel to the core. Their axes: **y -0.7 (upper pair) and y 3.7 (lower pair)**, each pair at **z -0.5 and z 1.5**. So the rods form a 2-wide x 4.4-tall rectangle around the core, outside its vertical diamond points. The upper pair stands about 0.8 above the core's top point and the lower pair about 0.8 below its bottom point. Same pale cream.
- **Head back plate:** 1 long x 5 tall x 5 wide (x 13 to 14, y -1 to 4, z -2 to 3), very dark brown.
- **Head top cap and bottom cap:** 1 long x 1 tall x 4 wide each (x 13 to 14), at y -2 to -1 (top) and y 4 to 5 (bottom), z -1.5 to 2.5, dark brown.
- **Striker plate:** 1 long x **6 tall x 4 wide** (x 14 to 15, y -1.5 to 4.5, z -1.5 to 2.5). It is taller than the back plate and narrower, so the head's silhouette from the front is a plus-like cross (5 wide x 5 tall back plate behind a 4 wide x 6 tall striker plate). **This is the one part with two skins** (section 5).
- The head is a stepped stack; there is no taper or rounded face.
- Rest pose: head front face at x = 15. At full retraction the head front is at about x 7.8 and the head back plate is almost against the collar (x 6): see section 7.

## 4. Silhouette checks

- **Side (main view):** the cream piston group on the right, 7 long and 4 to 6 tall at the head end; a dark head stack of plates; the front flange taller than the body, with a brown clamp (jaws) around the piston entry; a brown-and-gray housing; a small tilted gusset fin on top of the flange; a silver block with a black cap on top; a shorter brown butt on the left, the hooked butt plate lower than the top cap; three thin rods between the butt and the housing; a slanted gray grip under the rear half; a gap in front of the grip (the guard is invisible). The side pin on the crank side shows as a small diamond in the middle of the housing.
- **Top:** a stocky T: the body 6 to 7 wide, the piston a 2-wide cream bundle (the rods look like parallel bars of 4.4 wide total), the head 5 wide at its back plate and 4 at the striker plate, the pin sticking out of one side 5.5 beyond the body. The silver block shows as a square on top. The slim neck rods (3 across) are narrower than the butt (5) and the collar (4).
- **Front:** a squarish brown frame (flange 7 wide x 6 tall) with the head cross (5x5 behind 4x6) at the center; the cream rods peek out around it. The pin is a bar on one side. The grip is a narrow column below.
- **Back:** the butt (5 wide, stepped in to 3 at the rear) over the housing's brown and gray, silver block and dark cap above, grip below.

## 5. Materials and palette

All values are sampled from the originals. The two skins differ only in the striker plate.

| Part | Plain skin | Advanced skin | Notes |
|---|---|---|---|
| Housing core, butt blocks, collar, jaws, strips | rust brown, base #422C26 | same | highlight #4C342B to #58413C; shadow #35221B; some near-black vent marks #130E0D |
| Housing shell | warm gray, base #5F5B58 | same | highlight #6B6665, up to #726F6D; shadow #432E27 at the seams |
| Neck rods, collar plate, bracket plates, grip, gear boss | mid gray, base #5F5B59 | same | highlight #6A6765 to #797270; shadow #55504E |
| Pin | dark gray, base #5E5855 | same | highlight #747170 on the tip; shadow #444140 |
| Silver block | light silver, base #B5B5B7 | same | highlight #C3C7C8; shadow #837D81 |
| Dark cap | near black, base #312E30 | same | #282728 to #3D3B3C |
| Gusset fin | brown, base #4A3329 | same | highlight #5D463F; shadow #3F2B25 |
| Front strips and jaws (top) | dark brown, base #382720 | same | highlight #413029; shadow #32221B |
| Piston core and four rods | **pale cream-pink, base #DAC7C3** | same | highlight #E7DCD5 up to #EEE3DE; shadow #A29592 on the rod edges |
| Head back plate | very dark brown, base #3B2724 | same | highlight #473330; shadow #18100E |
| Head caps | dark brown, base #402D27 | same | #3C2723 to #4C342C |
| **Striker plate, front face (+X side)** | **mottled warm gray, base #7A7573** | **dark steel blue, base #232A33** | Plain range #575351 to #8C8784; Advanced range #1B2027 to #2B343F |
| **Striker plate, other faces** | **mid gray, base #64605F** | **very dark blue-black, base #1F252D** | Plain #5C5655 to #696564; Advanced #1D232A to #272F38 |

Shading style:
- **Metal and brown:** flat, matte, low-contrast, with 3 to 4 levels of noise per patch; seams are 1 texel darker lines. No gradients, no scratches, no wear. Overall the body reads as **dark and warm**, close to chocolate.
- **Piston:** bright, very light and slightly pink-cream, noticeably the lightest part of the model apart from the silver block. Its edges are one step darker (a rod's lower face is #A29592).
- **Striker plate:** the plain plate has a grainy, uneven gray; the Advanced plate looks like blued or dark tool steel, with the same grain pattern but a dark hue. Everything else in the Advanced skin stays the same, so the only difference visible from the side is the dark front disc of the head.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 1.5 | idle offset (0, -0.75, -0.3). While the use button is held (charge), the body slides back by up to 0.15 and the piston by up to 0.45 over 20 ticks. |
| First person aimed | no scope, no zoom overlay | extra offset (0, -0.15, 0.3) |
| First person reloading | roll up to 32 degrees about Z, yaw up to 15 degrees | translate up to (0.05, 0.4, -0.2) from idle at the peak; the peak plateau lasts from about 27 percent to 73 percent of the 45-tick reload. |
| Third person (held) | yaw 225 degrees, scale 1.45 | offset (-0.5, -0.65, 0). |
| Third person aimed | extra offset (0, -0.15, 0.3) | |
| Ground (dropped) | yaw 90 degrees, scale 0.8 | lowered by 0.6. |
| Inventory icon | yaw 180 degrees, scale 1.0 | no extra offset, drawn side-on. There is no sprite; the icon is the 3D model. |

**Muzzle point and flash:** none. The original's muzzle flash is disabled for this tool. The business end is the head's striker plate at x = 15, at y 1.5, z 0.5. Impact FX come from the projectile and the hit.

## 7. Motion

**The piston group is a pure slide along X**; it does not rotate. Slide axis: the piston's own long axis (x), at y 1.5, z 0.5. There is no pivot. The body is a separate part, and the two are transformed independently. The guide mouth (the front jaws and collar) hides the rods as they retract.

**Travel.** The maximum retraction is **0.45 block = 7.2 units**. At full retraction the head's striker front is at about x 7.8 and the back plate touches the muzzle collar (x 6). The rods slide 7 deep into the housing (their length equals the travel).

**Timing, first person.** Let *p* be the progress (0 to 1) of the fire animation (its length is the recoil time of the item, see the 02-guns sheet).
- **Primary attack (type 0).**
  - Piston: it **retracts linearly** from rest to the full 7.2 units by *p* = 0.25 (a fast snap back), then **returns to rest** over the remaining 75 percent with an ease-out curve (it comes back quickly at first, then slows to stop). It ends at the rest position at *p* = 1.
  - Body: a slight roll of **+/- 1.5 degrees** about Z (one swing, then the opposite swing, ending at zero) and a **forward lunge** that peaks at 0.15 block (2.4 units) at *p* = 0.5 and returns to zero.
- **Secondary attack (type 1, the left-hand variant).**
  - Piston: it starts retracted by 4.8 units (0.30 block) and **extends forward to the rest position in the first 1/15 of the animation**, then stays.
  - Body: starts 0.15 block back, returns to zero with an ease-out curve; a roll of **+/- 2.5 degrees**.
- **Charge (before the shot).** Hold the use button for up to 20 ticks: the piston slides back to 7.2 units and the body back to 2.4 units (so the piston pulls in 4.8 units relative to the body). Both return when the shot fires.

**Third person.** The body shows no recoil. The piston is **retracted 4.8 units at rest** (so the exposed rods are only about 2.2 units long), and on a shot it **slams forward to full extension within the first 1/15 of the animation**, then holds. During reload the whole gun tilts about 30 degrees about the long axis and 40 degrees about the vertical.

**Reload.** 45 ticks (2.25 s). Both parts move together (the first-person plateau above).

## 8. What to avoid

- **A one-piece model.** The piston group must be its own part so it can slide; the body is the second part.
- **A gun-shaped long barrel.** The piston is 7 long and cream: not a gray barrel.
- **Gray everywhere.** The body is mostly rust brown; the gray is the shell, rods and fittings.
- **A solid cube cylinder for the piston.** The core and the four rods are all turned 45 degrees (diamond sections); do not use squared-off boxes.
- **Making the Advanced skin a color-swap of everything.** Only the striker plate changes. The Advanced hammer's body must still be brown and its piston cream.
- **Hiding the grip's guard under thick geometry.** The guard is zero-thickness planes: from the side, nothing is drawn.
- **Extras the original lacks:** no scope, no sights, no hand guard, no glow, no sling loop.

## 9. Acceptance checklist

- [ ] The model is two parts: a static body and a piston-and-head group that slides on X.
- [ ] Length : height : body width is within about 10 percent of 31 : 14.8 : 7, with the piston about 9 of the 31 at rest.
- [ ] The piston has one 3x3 core and four 1x1 rods, all turned 45 degrees (diamond), 7 long, pale cream.
- [ ] The four rods sit at y -0.7 and y 3.7 and z -0.5 and 1.5, outside the core's vertical diamond points.
- [ ] The head is a stack of a 5x5 back plate, two 1-tall caps, and a 4-wide x 6-tall striker plate, all dark; the striker is taller than the back plate.
- [ ] The retraction is 7.2 units along X, and at full retraction the head touches the muzzle collar.
- [ ] The housing is two-tone: a brown inner core 13 long and a gray outer shell 11 long, 0.5 wider each side.
- [ ] The front flange is 7 wide x 6 tall and drops below the housing, with brown upper and lower jaws around the piston entry.
- [ ] A 4-long gusset fin at 45 degrees leans backward on top of the flange.
- [ ] The silver block (4x2x4) sits between two gray bracket plates, with a black cap, and is the highest point.
- [ ] The butt is 3 long, brown, stepped (rear narrows to 3 wide), with a bottom cap 2 tall that hangs below the block; three short gray rods link it to the housing.
- [ ] The grip is a gray slab leaning back 15 degrees, with an invisible zero-thickness guard.
- [ ] The crank pin is 2x2x6 turned 45 degrees, and sticks out of the +Z side to z 9.5 from a 3x3x2 gray boss.
- [ ] The two skins differ **only** in the striker plate: plain mottled gray (#7A7573 front face), Advanced dark steel blue (#232A33).
- [ ] No part floats; every box overlaps or touches a neighbor.

## 10. Unsure

- **Which side faces the camera.** Models of this family show the +Z (crank) side in first person; I did not determine which skin side the player sees in each view. The pin on the left or right is therefore a mirror question. Build as described.
- **Piston third-person idle.** The retracted-4.8 idle pose comes from the renderer's recoil formula applied at zero progress. It looks odd (the piston is partly pulled in while idle), but that is what the formulas give. Treat it as low priority.
- **Recoil duration.** The fire animation's length is the gun's recoil time; I did not find the number in the sources I read. Use the 02-guns sheet.
- **Slit and vent marks on the shell.** I read two black slits on the side face from a very low-resolution texture; their exact position is approximate (center of the shell, 2 to 3 apart).
- **Sign of the roll and lunge.** The signs come from formulas; the direction of the lunge (forward) is certain, the roll direction is not.
