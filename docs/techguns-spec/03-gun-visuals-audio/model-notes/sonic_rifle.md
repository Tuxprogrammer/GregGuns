# Sonic Rifle: clean-room model notes

Design notes for rebuilding the Techguns **Sonic Shotgun** look-alike (GregGuns "Sonic Rifle") in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout (128x64 is the native size).

Related facts: [ModelSonicShotgun](../gun-models.md), [rendering framework](../rendering-framework.md).

**Units and axes used in this document.** One unit = one texel at the native 128x64 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention); **Y = 0 is about the receiver's upper third**, and the barrel's central slot sits at **Y = -1**. **Z = 0 is the gun's mid-plane**; sizes across Z are total widths, centered, and positions across Z are +/- from the mid-plane. **X = 0 is the rear face of the receiver's rear block** (2 units behind the housing's rear face). The gun is mirror-symmetric across the mid-plane. Numbers are exact unless marked "about".

## 1. Gestalt

A **long, dark, heavy sci-fi rifle** in a cool blue-black palette, with a **thumbhole stock**, a **carry-handle-style top rail**, a **slotted barrel with silver coil rods visible in its gaps**, a tall slab-sided **energy cell magazine**, and a **flared, near-black horn** at the muzzle. In the middle of the housing, behind two small side windows, a **pale-cyan glow core** lights up. The gun is darker and heavier than the Laser and Tesla rifles; its only strong color is the cyan glow (and two teal gauge stripes on the magazine).

Defining features in order of visual importance:

1. **A glowing cyan core** in the receiver housing, visible through a 4x2 window on each side. (The special feature; section 3 gives its exact form.)
2. **A slotted barrel**: upper and lower plates with a 2-tall open slot between them, bridged by three dark collars; in the two gaps between collars sit pairs of silver diamond-section coil rods.
3. **The flared horn muzzle**: a 7-long, near-black frame with a tall narrow mouth (2 wide, 5 tall) and a black slot at its core.
4. **A thumbhole stock**: a big hole between the stock, a slanted lower strut, and the pistol grip.
5. **A raised carry-handle rail** on top, 1.5 units above the receiver in two places, ramping down at the front onto the barrel.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (butt plate back face to horn tip) | 59 (x -24 to 35) |
| Total height (sight bead to grip heel) | about 19 (y -7.5 to 11.5) |
| Total width | 7 at the receiver housing (z +/-3.5); 5.6 at the coil rods (z +/-2.8); the barrel plates and horn are about 5 |
| Length : height : width | about 8.4 : 2.7 : 1 |
| Stock (butt plate to receiver rear face) | 22 (x -24 to -2) |
| Receiver (rear block, housing) | 12 (x -2 to 10) |
| Barrel (bulkhead to horn) | 18 (x 10 to 28) |
| Horn | 7 (x 28 to 35), 12 percent of the length |
| Barrel and horn beyond the receiver | 25 (x 10 to 35), about **42 percent** of the total length |
| Receiver housing height | 4 (y -3 to 1); with plates, 7 (y -4.5 to 2) |
| Magazine | 8 long, 7 tall (y 4 to 11), 4 wide |
| Barrel height (upper plate top to under-tube bottom) | about 9 (y -4.5 to 4.5) |

The **top line** has three levels: the high rail (y -7 to -6) from x -0.5 to 17.5; the receiver and barrel tops (y about -4.5); and the front rail section (y -5 to -4) from x 19 to 28. The stock's top is lower than the receiver (y -4 to -5).

## 3. Part by part

All parts are boxes (a few are tilted). Z widths are totals, centered on the mid-plane. "Touches" means faces in contact or overlapping.

### Receiver housing and glow core (the special feature)
- **Housing:** a box **8 long, 4 tall, 7 wide** (x 2 to 10, y -3 to 1, z +/-3.5). Dark steel-blue gray (#3F4244 on the sides, #444649 top, #414346 bottom, #393C3E on the end faces).
- **Windows:** each side face (z +/-3.5) has a **rectangular cutout 4 long and 2 tall**, at **x 4 to 8** and **y -2 to 0**, going through the housing wall. A pale light-gray lintel (about #6E7175) 1 unit tall shows above the cutout (y -3 to -2), and a slightly darker lower lip (about #5B5E62) below it (y 0 to 1); there are 2-unit dark frames at the front and back (#222325 to #3A3C3F). The bezel is 1 unit tall at the top and bottom, 2 wide at the ends.
- **Glow core:** a solid box **7 long, 3 tall, 6 wide** (x 2.5 to 9.5, y -2.5 to 0.5, z +/-3), **entirely inside the housing** and recessed **0.5 unit** from each side face. It has no animated parts, no child parts and no pivot. Every face is opaque **pale cyan**: top #A2ECF4, bottom #8EE4EC, sides #8AE3EE to #B0F0F6 (average about #8FE4EE). The core is drawn **last with world lighting off** ("fullbright") so it stays bright in the dark and is not tinted by the surroundings.
- What you see: from either side, a 4x2 cyan rectangle framed by dark steel and a pale bezel, set 0.5 deep in the wall. From above or below, nothing (the housing covers the core). The core is not visible from the front or back.
- The 0.5 recess matters: the window should read as a lit slot behind the wall, not as a stuck-on patch. The cyan has no gradient or flicker in the model; it is one flat color per face.
- **Top plates of the housing:** a box **8 long, 1 tall, 6 wide** (x 2 to 10, y -4 to -3, z +/-3) and, on top of it, a narrower box **8 long, 1 tall, 4 wide** (y -4.5 to -3.5, z +/-2). Together they make a stepped top. Colors #404345 and #444649.
- **Bottom plates of the housing:** **8 long, 1 tall, 6 wide** (y 1 to 2, z +/-3) and, below it, **8 long, 2 tall, 5 wide** (y 2 to 4, z +/-2.5). Colors #404345 and #424446 (the lowest one is darker underneath: #252728).
- **Rear block:** a box **4 long, 5 tall, 4 wide** (x -2 to 2, y -4 to 1, z +/-2), very dark (#292B2D), directly behind the housing. A **riser** on it: **4 long, 1 tall, 3 wide** (y -5 to -4, z +/-1.5), and a **post** **2 long, 1 tall, 2 wide** (x -1 to 1, y -6 to -5, z +/-1). A **forward plate** below the block's front: **1 long, 4 tall, 4 wide** (x 1 to 2, y 1 to 5, z +/-2) plus **1 long, 1 tall, 3 wide** (y 5 to 6, z +/-1.5), all #2B2D2F.
- **Bulkhead:** a plate **1 long, 9 tall, 4 wide** (x 10 to 11, y -4 to 5, z +/-2) at the housing's front, with a **1 x 1 x 3 foot** below it (y 5 to 6, z +/-1.5). #2C2E30.
- Touches: the housing overlaps the rear block (x 2 plane), the bulkhead (x 10), and the magazine below.

### Magazine (energy cell)
- A box **8 long, 7 tall, 4 wide** (x 2 to 10, y 4 to 11, z +/-2), hanging under the housing and overlapping it by the lowest plate. Near-black blue-gray (#262729 top, #2C2E30 and #2F3133 ends, #343639 bottom).
- Each side face (z +/-2) carries a **gauge:** two **vertical teal stripes**, each **1 wide, 4 tall** (y 6 to 10 on the face, at 2 and 5 units from the face's front edge, so they are 3 apart). Teal is #008082 to #00B4B6, brighter in the middle (#00A0A3 to #00B4B6), darker at the ends. Between the stripes is a pale gray inset panel (#4C4F50 to #7B7C7E, 2 wide). These are **painted, not glowing**.
- Also painted: a 1-unit dark border around the stripes (#242628 to #3B3E41).
- A **lower housing:** a box **8 long, 2 tall, 5 wide** (x 2 to 10, y 2 to 4, z +/-2.5) between housing and magazine, dark (#424446 top).

### Top carry-handle rail
- **Rail:** a bar **18 long, 1 tall, 2 wide** (x -0.5 to 17.5, y -7 to -6, z +/-1), almost black (#0F1011 on top, #151618 to #18191A on the sides). It sits 1.5 units **above** the housing top (y -4.5), so there is an open gap (y -6 to -4.5) beneath it from x 1 to x 10, and from x 11 to x 17.5 above the barrel.
- **Rear riser:** the post and riser on the rear block (above).
- **Mid riser:** at x 10 to 11 (above the bulkhead), a **1 x 1 x 2** (y -6 to -5, z +/-1) on a **1 x 1 x 3** (y -5 to -4, z +/-1.5). Dark (#17181A and #323436).
- **Beads:** two **1 x 1 x 1** cubes on the rail's top, one at the rear (x -0.4 to 0.6) and one at the front (x 16.5 to 17.5), at y -7.5 to -6.5, z +/-0.5. Near-black (#111314 to #171719). Two zero-thickness 1x1 planes at x -0.5 to 0.5, y -8 to -7 are transparent in the texture and invisible; ignore them.
- **Ramp:** a bar **3 long, 1 tall, 2 wide** turned 45 degrees about the Z axis, hinged at the rail's front end (17.5, -7), bounding x 16.8 to 19.6, y -7 to -4.2. Near-black (#141516). It descends toward the muzzle.
- **Front rail:** a bar **8 long, 1 tall, 2 wide** (x 19 to 27, y -5 to -4, z +/-1) sitting on the barrel's top ridge, near-black (#141617). A **1 x 1 x 3** end cap (x 27 to 28, y -5 to -4, z +/-1.5) joins it to the muzzle flange.
- So the profile is: a raised handle that steps down by a 45-degree ramp onto the barrel.

### Barrel (x 11 to 27)
- **Upper plate:** a box **16 long, 2 tall, 5 wide** (x 11 to 27, y -4 to -2, z +/-2.5), with a narrower **top ridge** **16 long, 1 tall, 3 wide** (y -4.5 to -3.5, z +/-1.5). Dark steel (#3B3E40, ridge #3F4245).
- **Lower plate:** a box **16 long, 2 tall, 5 wide** (x 11 to 27, y 0 to 2, z +/-2.5), #3B3E40 (end faces lighter at #474A4D).
- **Under-tube:** a box **16 long, 3 tall, 3 wide** (x 11 to 27, y 1.5 to 4.5, z +/-1.5), the lightest part of the barrel: #525A5D on the sides, #5C676B underneath, #2A2C2E on top (hidden).
- **Forend:** a box **10 long, 2 tall, 4 wide** (x 14 to 24, y 3 to 5, z +/-2), near-black teal (#222B2E), with a thinner **9 x 1 x 3** under it (x 14.5 to 23.5, y 4.5 to 5.5, z +/-1.5), #20292C. They hang below the under-tube.
- **Open slot:** between the upper and lower plates (y -2 to 0), the barrel is a **2-tall slot**. Three **collar blocks** bridge it, each **2 long, 2 tall, 5 wide** (z +/-2.5), at **x 11 to 13, x 18 to 20, and x 25 to 27**; color #3C3F42 on top and #505458 on the faces that look along the barrel (lighter, so the collars look like clamps).
- **Coil rods in the gaps:** two gaps remain, **x 13 to 18** and **x 20 to 25** (5 long each, 2 tall). In each gap lie **two silver rods side by side**, each a **5 long, 2 x 2 box turned 45 degrees** about the barrel's long axis (a diamond about 2.8 across). Their centers are at **y -1** and **z -1.4 and +1.4** (the two diamonds touch at the mid-plane; together they span z +/-2.8, which is 0.3 wider than the plates on each side). Silver-gray (#656668 on top, #67696B beneath, #67696A on the faces that look sideways, ends #525355). They are the **only light-gray parts** of the gun, and the slot gaps are the place where they show.
- Touches: the rods end flush against the collars; the upper and lower plates and the collars form a frame around them.

### Muzzle flange and horn (x 27 to 35)
- **Flange:** a plate **1 long, 8 tall, 4 wide** (x 27 to 28, y -4 to 4, z +/-2), #323436.
- **Horn core:** a box **5 long, 4 tall, 4 wide** (x 28 to 33, y -3 to 1, z +/-2), #2C2E30, with a **cap** above and below, each **5 long, 1 tall, 3 wide** (y -4 to -3 and y 1 to 2, z +/-1.5), same color. The **front face** of the core is a near-black slot: a **2 wide, 4 tall** column of almost pure black (#030304 to #070708) centered in the face, bordered by a dark gray outline (#141516 to #18191A). The front faces of the two caps are also near-black (#151617). (If the builder is unsure which side of the model this face is on, put it on the **front**, deep inside the mouth. See section 10.)
- **Top and bottom plates:** two boxes **7 long, 1 tall, 4 wide** (x 28 to 35), one at y -4.5 to -3.5 and one at y 1.5 to 2.5 (z +/-2). Near-black (#141516).
- **Side plates (four):** each **7 long, 3 tall, 1 thick**, in two stacked pairs per side:
  - Upper pair: hinged on the top plate's lower edge at y -4.5 and covering y -4.5 to -1.5.
  - Lower pair: hinged on the bottom plate's upper edge at y 2.5 and covering y -0.5 to 2.5.
  - Each plate sits at z +/-1 to 2 and is **tilted 12 degrees** about the barrel's long axis, so that its **free (inner) edge swings outward by about 0.6**. The result: the two plates on each side leave a **1-tall horizontal slit** (y -1.5 to -0.5) in the middle and bow out slightly, like a barrel-shaped flare, up to z +/-2.6 at the slit.
  - Near-black (#111314 to #161819).
- The side plates and top and bottom plates run 2 units past the horn core's front face (x 33 to 35), forming a **mouth** 2 wide and 5 tall (z +/-1, y -3.5 to 1.5) in front of the black slot.
- The horn is the darkest object on the gun.

### Stock and thumbhole
- **Butt plate:** a box **2 thick, 7 tall, 4 wide** (x -24 to -22, y -1 to 6, z +/-2), dark teal (#273134 on the faces, #252F33 on the edges), with a **horn** above and below it, each **2 long, 2 tall, 3 wide**: y -3 to -1 and y 6 to 8, z +/-1.5. Total 11 tall (y -3 to 8). Dark teal (#263034).
- **Stock body:** a box **8 long, 8 tall, 3 wide** (x -22 to -14, y -3 to 5, z +/-1.5). Dark steel (#3D4042).
- **Stock lower plate:** a box **9 long, 6 tall, 2 wide** (x -22 to -13, y 2 to 8, z +/-1), lighter cyan-gray (#616C70). It is narrower than the body (2 versus 3), so it reads as an inset panel; the part below the body (y 5 to 8) hangs down as the stock's belly.
- **Top spine:** a bar **20 long, 1 tall, 2 wide** (x -22 to -2, y -4 to -3, z +/-1), #424547.
- **Cheek block:** a very dark box **10 long, 4 tall, 4 wide** (x -18 to -8, y -4.5 to -0.5, z +/-2), #1F282B, with a **1 x 10 x 3 cap** above it (y -5 to -4, z +/-1.5), same color.
- **Mid body:** a box **12 long, 4 tall, 3 wide** (x -14 to -2, y -3 to 1, z +/-1.5), a lighter gray (#474A4D), in front of the cheek block's lower part.
- **Under plate:** a box **8 long, 1 tall, 2 wide** (x -14 to -6, y 1 to 2, z +/-1), cyan-gray (#626C70).
- **Lower strut:** a bar **2 wide, 7 long, 2 thick** (z +/-1), tilted about **-66 degrees about the Z axis**, hinged at (-14, 8) so it runs **forward and slightly downward** toward the grip's heel (bounding x -14 to -6.8, y 6.2 to 10.8). Cyan-gray (#616C70). It forms the bottom edge of the thumbhole.
- **Grip:** a box **3 wide (X), 8 tall, 2 thick (Z +/-1)**, hinged at its top-rear at about (-6, 3) and **tilted 18 degrees back**. It spans x -8.5 to -3.1, y 3 to 11.5. Cyan-gray (#5F6A6D). On top, a **grip block**: **3 long, 4 tall, 2 wide** (x -6 to -3, y 1 to 5, z +/-1), #5C676B.
- **Gussets:** two small cyan-gray boxes at the hole's corners, both 2 thick: **2 x 1** tilted 138 degrees at about (-14, 2) (bounding x -14.2 to -12, y 1.3 to 3.3), and **3 x 1** tilted 28 degrees at (-8, 2) (bounding x -8 to -4.9, y 1.1 to 3.4). #5C676B to #5F6A6E.
- **Trigger guard:** a bar **4 long, 2 tall, 2 wide** (x -3 to 1, y 1 to 3, z +/-1), #606A6E; a front post **1 long, 2 tall, 2 wide** (x 0 to 1, y 3 to 5, z +/-1), #6C7A7F; and a **floor** that is a 3x2 plane with no thickness (x -3 to 0, y 5), invisible from the side. There is **no trigger**: the opening (x -3 to 0, y 3 to 5) is empty. (The guard's rear edge is the grip block.)
- **The thumbhole:** the open space bounded by the stock's belly (above/behind it is the body), the lower strut (below), the grip (in front) and the mid body and under plate (above). It is about 5 wide and 6 tall (x -13 to -8, y 2 to 8), a hole you can see through. Keep it open.

## 4. Silhouette checks

- **Side (the main view):** from the left: a tall butt plate with two horns; a long stock body, its belly hanging lower; **a see-through hole** with a slanted strut under it; a grip leaning back; the receiver with the lit cyan rectangle in the middle and a 7-tall magazine hanging below; a flat carry-handle rail floating above the body from x 0 to 17.5, then ramping down; a long slotted barrel with three dark collars and silver diamonds between them, an under-tube and a dark forend; the horn at the end, a taller flared frame (7 tall) that is darker than all else. Negative space: the thumbhole; the handle gap (y -6 to -4.5) in two places; the slot gaps (silver rods visible); the trigger opening.
- **Top:** a long strip. A thin stock (3 to 4 wide), the receiver housing bulging to 7 wide, then a 5-wide barrel (top ridge 3 wide, rail 2 wide) with collars at 5 wide; the silver rods show as thin lighter bars sticking out 0.3 on each side in the two gaps; the horn at 4 to 5 wide, with the side plates bowing slightly.
- **Front:** an upright, squarish block: the horn mouth (2 wide, 5 tall) with a black slot behind it, the barrel plates (5 wide) behind, the housing (7 wide) behind that, and the magazine hanging below; a 2-wide rail and beads on top; the grip and butt plate (11 tall) behind.
- **Back:** the butt plate (11 tall, 4 wide, with horns), above it the cheek block and rail; the magazine and the grip below.
- **Underside:** the magazine as a rectangle under the housing; the forend and under-tube as a long strip; the stock belly; the grip.

## 5. Materials and palette

A **cool blue-black gunmetal** gun. Measured from the texture's regions.

| Part | Material | Base | Highlight | Shadow / detail |
|---|---|---|---|---|
| Housing sides | dark steel | #3F4244 | #444649 (top) | #393C3E (ends) |
| Housing windows (bezel) | pale gray | #6E7175 (lintel) | #707476 | #5B5E62 (lip) |
| **Glow core** | cyan, fullbright | #8FE4EE | #B0F0F6 | #8AE3EE |
| Rear block, bulkhead, plates | very dark gray | #2B2D2F | #2E3032 | #27292B |
| Top rail, beads, ramp | near-black | #151618 | #18191A | #0F1011 |
| Barrel plates | dark steel | #3B3E40 | #424548 | #3D4043 |
| Barrel top ridge | dark steel | #3F4245 | #434548 | #3A3D40 |
| Collars | dark blue-gray | #3C3F42 | #505458 (end faces) | #3B3E41 |
| Coil rods | silver | #67696A | #67696B | #525355 (ends) |
| Under-tube | steel-blue | #525A5D | #5C676B | #2A2C2E |
| Forend | near-black teal | #222B2E | #262C2E | #20292C |
| Magazine | near-black | #262729 | #2F3133 | #343639 |
| Magazine gauge | teal, painted | #00A0A3 | #00B4B6 | #008082 |
| Gauge panel | pale gray | #6B6D70 | #7B7C7E | #454749 |
| Muzzle flange and horn core | very dark | #2C2E30 | #323436 | #303234 |
| Horn plates | near-black | #141516 | #161819 | #111314 |
| Horn slot | black | #030304 | #070708 | |
| Stock body | dark steel | #3D4042 | #424547 | #2A2C2E |
| Butt plate and horns | dark teal | #263034 | #273134 | #20292C |
| Stock lower plate, strut, grip | cyan-gray | #616C70 | #748084 | #5C676B |
| Cheek block and cap | very dark teal | #1F282B | #20282C | #1E272A |
| Mid body | gray | #474A4D | #484B4D | #45474A |

Shading style:
- **Flat, matte metal** with a few levels of per-pixel noise, like worn gunmetal. The color temperature is cool: every gray leans slightly toward blue or teal.
- The gun is **darker than the other energy rifles**: most parts are between #1F282B and #474A4D. The lightest parts are the cyan-gray stock plate, strut and grip (#5C676B to #748084), the silver rods (#67696B), the under-tube (#525A5D) and the pale bezel (#6E7175).
- **Glow core:** pale, slightly aqua cyan (#A2ECF4 top), flat per face.
- **Magazine gauge:** the only saturated painted color besides the glow. Two thin teal bars, brightest in the middle.
- No wear, no scratches, no logos.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units, after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 2.0 | idle offset (0.2, -0.2, -0.7). The model is shown much larger than the Laser and Tesla rifles (scale 2.0 against 1.3). |
| First person firing | roll about Z up to -8 degrees (a sinusoidal kick) | plus a backward slide along the gun axis of up to 0.6 block, on top of the idle offset. |
| First person reloading | roll about Z up to 25 degrees (times a 1.5 factor, up to about 37), yaw up to -15 degrees | plus a shift of up to (0.5, 0.25, 0.25) from the idle offset. |
| First person aimed | no scope and no zoom overlay on this model | |
| Third person (held) | yaw 225 degrees, scale 1.25 | offset (-0.85, -0.6, 0). |
| Third person aimed | extra offset (0, 0.1, 0.2) | after the shared aimed rotation. |
| Ground (dropped) | yaw 90 degrees, scale 0.65 | lowered by 0.6. |
| Inventory icon | yaw 180 degrees, scale 0.55 | offset (-0.25, 0.1, 0); the icon is the 3D model, side-on. |

**Muzzle flash:** a sonic-style additive flash sprite at 2.5 times scale (a bright ring or wave burst), drawn in screen space near the muzzle in first person (offset about (0.95, -2.0, 0.35), turned -90 degrees). In model terms the flash belongs at the horn's mouth (x 35, y -1, z 0).

## 7. Motion

- The **model has no moving parts.** The glow core does not pulse or scale; the coil rods do not spin; the horn plates do not move. All motion is whole-gun transforms.
- **Recoil (third person):** the gun pitches up about 20 degrees about a point behind and above the model origin (roughly 0.9 block behind the center of the model and 0.3 up) over the first quarter of the animation, then settles back.
- **Reload (third person):** the gun rolls about 30 degrees about X and turns about 40 degrees about Y at mid-animation, then returns. Reload time is long in the original; for energy weapons the GregGuns design may have no reload.
- **Optional (not in the original):** the builder may keep the glow core as a separately named part so later code can pulse its brightness or scale, but the rest pose must match this document.

## 8. What to avoid

- **A glowing slab on the outside.** The core is 0.5 unit behind the wall, seen through a 4x2 cutout with a bezel. Do not paint a cyan patch on the surface.
- **A glow that dims in the dark.** The core must be drawn without world lighting.
- **A bright or light-gray gun.** This is the darkest of the energy rifles. Do not lighten the barrel plates, housing or horn.
- **A closed barrel.** The slot between the plates is open; the silver rods and the collars sit in it. Do not fill the slot or make the rods dark.
- **A flat muzzle.** The horn has a narrow, tall mouth and a black slot at its core. Do not use a plain end.
- **A grip fused to the stock.** The thumbhole is a see-through hole.
- **A floating handle.** The rail is held by the rear post and the mid riser; it must visibly connect at both.
- **Painted teal as a glow.** The magazine's teal stripes are plain paint and should be lit normally.
- Do not add things the original lacks: no scope lens, no trigger, no flash hider, no rail teeth.

## 9. Acceptance checklist

- [ ] Length : height : width is within about 10 percent of 8.4 : 2.7 : 1.
- [ ] Barrel plus horn beyond the receiver is about 42 percent of the length.
- [ ] Glow core is a 7x3x6 box inside the 8x4x7 housing, recessed 0.5 from each side face; all faces opaque pale cyan; drawn fullbright.
- [ ] Each side of the housing has a 4x2 window (x 4 to 8, y -2 to 0) with a pale lintel, a dark frame at each end, and a darker lip.
- [ ] The core is invisible from above, below, front and back.
- [ ] Barrel has upper and lower plates with a 2-tall open slot, three dark collars (2 long) and two gaps (5 long).
- [ ] Each gap holds two silver diamond-section rods side by side (2.8 across each, 5 long), touching at the mid-plane; they stick out 0.3 beyond the plates on each side.
- [ ] The rail is 18 long, near-black, floating 1.5 above the housing top, held by a rear post and a mid riser, ending in a 45-degree ramp onto an 8-long front rail.
- [ ] Horn: 7 long, near-black, with top and bottom plates, four tilted side plates (12 degrees) leaving a 1-tall slit each side, and a 2x4 black slot at its core's front face.
- [ ] Magazine: 8 x 7 x 4 hanging below the housing, with two teal vertical stripes on each side.
- [ ] Stock: tall butt plate (11 tall with horns), a lighter inset lower plate, a slanted strut, a 3-wide grip leaning back 18 degrees, and an open thumbhole.
- [ ] Trigger guard is a 2-tall bar, a front post and an invisible floor; there is no trigger.
- [ ] The palette is blue-black gunmetal (#1F282B to #474A4D), with cyan-gray stock furniture (#5C676B to #748084), silver rods, and a cyan core.
- [ ] No part floats; every box overlaps or touches a neighbor.
- [ ] No moving parts in the model.

## 10. Unsure

- **Which face of the horn core is black.** The texture paints a black slot on one x-face of the 5x4x4 core and the two cap boxes, and a plain face on the other. The original's mirrored texture layout makes it ambiguous whether the black face points forward or back. The sensible reading, and the one I recommend, is that the **front** face (toward the mouth) is black, because the other face is hidden inside the flange. Optional, since the mouth is small.
- **Horn plate tilt direction.** I derived the 12-degree tilt from rotations of boxes hinged on their top or bottom edges; the visible effect is a bow of about 0.6 units. If the builder cannot reproduce the exact hinge, a straight set of plates with a 1-tall slit is an acceptable approximation.
- **Why the rail is raised.** The 1.5-unit gap under the rail is real by the numbers (rail y -7 to -6 against housing top y -4.5), and it looks like a carry handle. A builder who finds it odd should still keep it.
- **Gusset shapes.** The two tiny tilted boxes near the thumbhole are small (2x1 and 3x1) and their exact function is not clear; keep them small and attached.
- **Thumbhole size.** The hole's extents are derived from neighboring parts; give or take 1 unit.
- **First-person framing numbers** come from formulas read, not seen in game. Treat the offsets as approximate. The scale 2.0 is a relative factor on a 59-unit-long model; the apparent size in hand was not measured.
