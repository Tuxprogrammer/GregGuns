# Nuclear Death Ray: clean-room model notes

Design notes for rebuilding the Techguns **Nuclear Death Ray** (NDR) look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [ModelNDR](../gun-models.md#modelndr-nuclear-death-ray-6464-68-boxes-tabula-with-child-parts-1-part), [NDR sheet](../gun-sheets.md#nuclear-death-ray-beamtestgun-field-ndr-unlocalized-nucleardeathray), [rendering framework](../rendering-framework.md), [Techguns NDR stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition).

**Units and axes used in this document.** One unit = one texel at the native 64x64 scale = 1/16 block. Muzzle toward **+X**. **Y points down** (Minecraft model convention), with **Y = 0 on the barrel axis** (the center of the hexagonal tube and of the reactor). **Z = 0 is the gun's mid-plane**; **+Z is the gun's left side** as the holder sees it, looking toward the muzzle. Sizes across Z are totals, centered, unless a Z range is given. **X = 0 is the rear tip of the shoulder bracket**; the emitter's front rim ends at X = 43. Rounded to the nearest half unit unless it matters. Directions around the barrel are given as clock positions seen from behind (12 o'clock = straight up, 3 = the gun's right).

## 1. Gestalt

A **bulky, tool-built sci-fi radiation cannon**: a skeletal red-and-gray shoulder bracket at the back, a long chunky **hexagonal tube** wrapped in a stepped top rail with a red fin, a squat **off-white reactor block** in the middle with a short pistol grip hanging off its rear, and at the front a **three-clawed emitter**: three thin arms sweep forward past the reactor, then bend inward into a small hexagonal heat-sink cup. One long red mast sticks out sideways from the lower-left arm like an antenna. At a glance you read: "a hex-barreled lab ray-gun with a pale reactor, red accents and a claw at the front".

The gun is **not symmetric**: the left-pointing mast and the right-shifted grip (both see section 3) break the left-right symmetry.

Defining features in order of visual importance:

1. **The three-prong emitter.** Three 9-long bars at 12, 4 and 8 o'clock run forward along the reactor and then bend 45 degrees inward into the hexagonal cup at the front. This is the gun's trademark.
2. **The pale reactor block** (a 6 by 6 cross-section with notched corners, 9 long with caps) between the gray hex tube and the gray emitter. It is the lightest, brightest mass on the gun.
3. **The hexagonal gray tube behind it**, two chunky hex rings with a narrower sleeve between them, topped by a three-tier rail and a red slanted fin.
4. **Red accents**: shoulder-bracket top bar, hook and neck; the fin on top; the trigger; the side mast. Red is the only saturated color besides the yellow hazard checkers.
5. **A long skeletal shoulder bracket**, thin (1 unit) and flat, unlike anything else on the gun.
6. **The sideways mast** on the left (sticks out 6 units past the tube's side). Easy to forget and very recognizable.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total length (bracket tip to emitter front rim) | 43 |
| Total height (fin top at about Y -8.6 to grip bottom at Y +10) | about 18.5 |
| Total thickness, excluding the mast | 10 (the hex rings across their corners) |
| Total thickness, with the mast | about 16 (Z -5 to +11) |
| Length : height : thickness (no mast) | about 4.3 : 1.9 : 1 |
| Shoulder bracket | X 0 to 11 (26 percent of the length) |
| Hex tube (rings, sleeve, plates) | X 11 to 27 (16 long, 37 percent) |
| Reactor with its two caps | X 27 to 36 (9 long, 21 percent) |
| Emitter (hex core and cup) | X 36 to 43 (7 long, 16 percent) |
| Prong arms | X 27 to 36 along the reactor, then tips to about X 39 |
| Hex ring | 3 long, 10 across corners (Z), 8.7 across flats (Y) |
| Reactor cross-section | 6 by 6, with 1 by 1 corner notches |
| Grip | hangs 10 below the axis, X 24 to 26 |

The **top line**: a flat bracket bar at Y -2; the block at X 9 to 11 stepping up to -3; the hex tube's flat top at -4.3; the stepped rail rising to -5, -6 and -7; the red fin peaking at -8.6 near X 18; then the rail ends and the line drops to the arm bars at about -4.8 and the reactor top at -3. The highest point is the fin, in the **rear third** of the gun.

The **bottom line**: the bracket's web hangs to Y +4; the tube's flat bottom is at +4.3; the grip drops to +10 at X 24 to 26; the reactor bottom is at +3; the emitter's flat bottom is at about +3.5.

## 3. Part by part

All parts are boxes except where tilt is stated. Z is centered unless marked. "Pale" means the off-white pinkish gray of the reactor; "dark" is the charcoal gray of the heat sink; "red" is the crimson accent.

### Shoulder bracket (X 0 to 11)
Thin, almost flat parts, all **1 unit thick in Z** except the last block.
- **Top bar:** 6 long, 2 tall, 1 thick, X 0 to 6, Y -2 to 0. Red.
- **Hook:** 1 long, 4 tall, 1 thick, X 0 to 1, Y 0 to 4, hanging down from the bar's rear end. Red.
- **Web:** a **zero-thickness flat plane**, 5 long by 4 tall, filling the area under the bar behind the neck (X 1 to 6, Y 0 to 4), on the mid-plane. Light gray-pink. It is visible from both sides and nearly vanishes seen from straight ahead or behind. If the builder gives it thickness, keep it 0.25 or less.
- **Corner stud:** a 1 by 1 by 1 red cube at X 1 to 2, Y 0 to 1, at the inside corner where hook and bar meet.
- **Neck:** 3 long, 4 tall, 1 thick, X 6 to 9, Y -2 to 2. Red, centered under the bar's front end.
- **Cheek plates:** two 2 long by 2 tall by 1 thick plates, X 7 to 9, Y -1 to 1, one on each side of the neck (Z -1.5 to -0.5 and +0.5 to +1.5). Dark brown-gray.
- **End block:** 2 long, 6 tall, 4 wide, X 9 to 11, Y -3 to 3. Dark gray. It is where the bracket meets the hex tube.

Together the bracket reads as a **flat rectangular skeleton stock, about 6 long by 6 tall**, with a hook at the heel, narrowing at the neck, widening into a block where it meets the tube. There is no butt pad and no cheek riser.

### Hex tube (X 11 to 27)

**Hex rings (two).** Each is **3 long**, made of six slabs 3 long by 3 thick by 5 wide, placed around the axis at 60-degree steps to make a regular hexagon:
- Side length 5 (the slab width), **across flats 8.7** (outer flats at Y -4.3 and +4.3, flat on top and bottom), **across corners 10** (corners at Z -5 and +5 on the horizontal mid-plane).
- Wall 3 thick: inner flats at 1.3 from the axis, so the ring has a 2.7-across hexagonal core, capped off at both ends (it is not visible from outside).
- **Rear ring at X 11 to 14, front ring at X 24 to 27.** Identical.
- Color: flat mid steel gray, a little brighter on the big outer faces.

**Plates (two).** 1 long, 7 tall, 4 wide, Y -4 to +3 (half a unit above center), at X 14 to 15 and X 23 to 24. Dark charcoal. They sit between each ring and the sleeve, so the sleeve looks recessed.

**Hex sleeve (between the plates, X 15 to 23).** 8 long. A hexagon about **7.8 across the flats and 9 across the corners**, centered about 0.4 below the axis (flats at Y -3.5 and +4.3, corners at Z -4.5 and +4.5). Built from six slabs 3 thick that alternate **narrow (3 wide) and wide (6 wide)**: going around, top flat narrow, upper-right wide, lower-right narrow, bottom wide, lower-left narrow, upper-left wide. So the sleeve looks like an irregular hexagon: slightly smaller and more bevelled than the rings. The three wide flats carry the **hazard windows** (section 5); the three narrow flats carry a polished silver strip.

**Top rail (three tiers).** All centered in Z, on top of the tube:
- **Tier 1:** 14 long, 1 tall, 4 wide, X 12 to 26, Y -5 to -4. Dark charcoal.
- **Tier 2:** 10 long, 1 tall, 3 wide, X 16 to 26, Y -6 to -5. Pale.
- **Tier 3:** 6 long, 1 tall, 3 wide, X 17 to 23, Y -7 to -6. Pale.
The rail's front end is aligned at X 26 / 23, which is 1 behind the end of the front ring.

**Fin.** A 1 thick, 4 wide, 4 long slab tilted **45 degrees about the Z axis**, pivoting at its top end near X 18.2, Y -8.6. Its upper end leans forward and its lower end swings back and down to about X 15.4, Y -5.8, which is on the rail's rear tier. It reads as a **swept-back red dorsal fin**, about 3.5 long and 3.5 high in side view, 4 wide in Z (so it is blade-like seen from the front and wide from above). Crimson. On its **rear-top face** (the face looking up and back) sits **one pale blue pixel**, 1 by 1 texel near that face's upper-left corner, like an indicator lamp.

### Pistol grip (X 24 to 27)
Everything here is **shifted 2 units toward the gun's right** (Z -3 to -1, centered at Z -2), so the grip is off the mid-plane. This is a quirk of the original and is kept.
- **Upper block:** 3 long, 2 tall, 2 wide, X 23.5 to 26.5, Y 3 to 5. Dark gray-brown. It sits under the front ring.
- **Grip:** 2 long, 5 tall, 2 wide, X 24 to 26, Y 5 to 10. Pale. It is vertical, a straight slab with no finger grooves and no palm swell.
- **Trigger:** 1 by 3 by 1, red, hanging from the block's front end (top at about X 26.5 and Y 4), **tilted about 23 degrees so its lower end swings forward** by about 0.8 toward the muzzle. Z centered at -2 like the grip. No trigger guard.

### Reactor (X 27 to 36)
A **cross-shaped 6 by 6 cross-section** ("plus" with the arms 4 wide: one 6-tall-by-4-wide block unioned with one 4-tall-by-6-wide block, so the four corners are notched by 1 by 1).
- **Housing:** 7 long, X 28 to 35, composed of a 6 tall by 4 wide block and a 4 tall by 6 wide block. Pale warm off-white. Seen from the side or the top, it is a 6-wide band with 1-unit steps at each end of the cross-section.
- **Rear cap:** 1 long, X 27 to 28. A 4 tall by 6 wide plate plus two 1 tall by 4 wide bars above and below, so it repeats the plus section. The plate is pale; the bars are brighter pale.
- **Front cap:** identical, X 35 to 36. The rear cap's rear face and the front cap's front face (the two outward faces) each carry **one black 2 by 2 square**, centered on the face (the face is 6 wide by 4 tall). They read as bolt holes or vent ports.
- **Core glow:** a 7 by 4 by 4 box at X 28 to 35, **entirely inside the housing**. In the original this box is never visible from any angle (it is fully enclosed by the housing's cross, and the caps cover its ends). It is drawn at full brightness, but there is nothing to see. The model's visible glow is the firing starburst (section 7). A builder may expose the core through a small window; see section 10.

### Prongs (three arms, X 24.5 to 39)
Three identical arm assemblies at **12, 4 and 8 o'clock** (0, +120 and -120 degrees about the barrel axis; the model's 120-degree arms sit lower right and lower left). Each lies at a **center radius of about 4.3** from the axis:
- **Base knob:** a 2 by 2 by 2 knob (3 long on the lower two arms) at X 24.5 to 27.5, centered at radius 4.3. Its outer edge reaches radius 5.3, so the top one rises 1 above the front hex ring's flat top (-4.3 to -5.3), and the lower ones poke from the ring's lower corners. Dark gray.
- **Arm bar:** 9 long, 1 by 1, X 27 to 36. Centerline at radius 4.3 (the top arm's bar at Y -4.8 to -3.8). Light steel gray. It passes outside the reactor with about 0.8 clearance.
- **Claw tip:** 4 long, 1 by 1, hinged at the arm's front end (X 36), **bent 45 degrees inward toward the axis** so its end lands at about X 38.8, radius 2.0, i.e. diving into the hex core's wall. Light steel gray.

In the original, **the lower-left arm carries extra parts**: a **1 by 1 by 1 pale cube** at radius 5 to 6 (X 25 to 26) and a **thin red mast** at radius 6 to 12 along the same radial direction (X about 24.1 to 26.9). The mast is a 2 by 6 by 2 bar **rotated 45 degrees about its long axis** (a diamond cross-section about 2.8 across). It points **outward to the left and down at 30 degrees below horizontal**, so its end is at about 10.4 left of and 6 below the axis. This is the biggest asymmetry on the gun.

### Hex heat-sink (emitter) (X 36 to 43)
- **Core:** 3 long (X 36 to 39), a regular hexagon with side 3 and **across flats 5.2** (flats up and down, corners at Z +-3), made from six 3 by 2 by 3 slabs. Dark charcoal. This is a stubby hex block in front of the front cap.
- **Cup walls:** six thin plates **1 thick and 4 wide**, forming a hexagonal tube about **7 across the flats** (flats at Y -3.5 and +3.5), starting at X 39 (the front of the core). The lengths differ: the **top plate and the two lower-diagonal plates are 3 long** (to X 42); the **bottom plate and the two upper-diagonal plates are 4 long** (to X 43). So the rim is ragged: ends at 42 on the top, lower right and lower left, and at 43 on the bottom, upper right and upper left. Dark charcoal.
- **Cup interior:** open hexagonal recess about 5 across the flats, 3 to 4 deep. Its floor is the core's front face. Dark.

The claw tips from the prongs dive into the core's flats from behind and above, 2 units before it ends; they are not visible entering the cup.

## 4. Silhouettes per view

- **Side (the main view, muzzle to the right):** from the left: a thin, flat **L-shaped bracket** (hook down at the heel, bar across the top, web filling the lower half) steps up through a taller dark block into a long hex band 8.7 tall. A stepped rail (1, then 2, then 3 units of rise) crowns its top with the red fin leaning forward over its rear third; the band's middle section is slightly slimmer. Then the pale 6-tall reactor, with a pair of thin arm bars above it, passing forward and bending down into a small hex cup. A short vertical grip hangs from under the front ring, 2 long and 5 tall, with a small red trigger raked forward in front of it. The mast pokes down at the side (visible only in the near-side view). **Negative space:** the area under the bracket and under the tube behind the grip is empty; the notch between the grip and the reactor rear is a narrow vertical slot; the tilt of the claw tips leaves a V-shaped gap between each tip and the arm.
- **Top:** a long, thin figure: the 1-thick bracket (a sliver) widens to 4 at the end block, then 10 across the rings, narrowing to 4 at the plates and 9 at the sleeve's corners; the rail runs down its middle (4 wide, narrowing to 3); the reactor is 6 wide; the arms appear as two thin lines at the cross-section's sides; the cup is about 8 wide at its corners. The **mast sticks out to the left**, 10 from the axis, 6 beyond the ring corners; the grip is slightly right of center and appears as a small 2-wide box under the ring.
- **Front (looking at the muzzle):** the hexagonal cup (7 across flats) in the middle, with three arms around it at 12, 4 and 8 o'clock, each bent inward like a claw; behind them the 6 by 6 pale reactor and the larger gray hex ring (10 across corners); the red fin pokes up above the rail, the grip sticks 10 down and a little right, and the mast comes out of the 8 o'clock arm toward the lower left.
- **Back:** the thin red-and-gray bracket in a narrow column, the hex ring (10 across) behind it, the fin peaking above, the grip below and the mast on the left. The bracket's thickness is only 1 unit, so it nearly vanishes in the rear view.
- **Underside:** the hex tube is a broad 9-to-10-wide strip; the sleeve's bottom flat shows a hazard window; the grip is a small box under the front ring, shifted right of center; the mast shows on the left. The reactor is a 6-wide band in front.

## 5. Materials and palette

The original texture is **gunmetal gray with pale off-white and red accents**. All values were sampled from regions of the texture; the "range" gives the 20th to 80th percentile of the region's luminance.

| Part | Material | Base | Highlight | Shadow |
|---|---|---|---|---|
| Hex rings (all six slabs, both rings) | flat mid steel gray | #8E8B8D (big outer faces #9E9B9D) | #ABA7A8 | #8C888B |
| Sleeve, narrow flats | polished silver strip | #B8B7B7 | #DEDEDE (a bright center line) | #938D8E |
| Sleeve, wide flats (panels) | gray with inset window | #747369 overall | #FFFC00 (hazard checker) | #383737 |
| Sleeve window inset | dark steel panel | #4E5050 | #575554 | #434142 |
| Rail tier 1, plates (X 14 and 23) | charcoal | #534E50 (plates #4F4949) | #5B5758 | #4A4549 |
| Rail tiers 2 and 3, tops | pale | #B1A8AB | #C6BCBE | #898183 |
| Reactor housing | warm off-white | #C7BCBA | #D6CBC8 | #B6A9A8 |
| Reactor caps, grip | pale | #C3B7B6 (grip #CDC6C8) | #CBC6C4 | #A59C9D |
| Reactor cap bars | brighter pale | #CBC6C4 | #D3CECD | #B7AFAE |
| Bracket bar, hook, neck, corner stud | crimson red | #AD3030 | #B93434 | #A52E2E |
| Bracket end block, cheek plates | brown-charcoal | #595352 (cheeks #585556) | #5D5756 | #534F4D |
| Bracket web (plane) | light gray-pink | #A19B9F | #ACA5A6 | #938D92 |
| Fin | deep crimson | #9A2A2A | #AD3030 | #7F2323 |
| Fin lamp (1 pixel) | pale periwinkle | #9198F6 | | |
| Prong base knobs | dark gray | #524C4F | #565051 | #4D4A4D |
| Prong bars and claw tips | steel gray | #909091 (tips #8A898A) | #979797 | #7F8282 |
| Mast | crimson red | #A92F2F | #B43232 | #9F2C2C |
| Mast connector cube | pale | #B5ABB2 | | |
| Heat-sink core | dark noisy charcoal | #474345 | #565152 | #302D2E |
| Heat-sink cup plates | charcoal | #4A4446 to #504B4D | #555254 | #453F40 |
| Grip upper block | dark gray-brown | #544E4D | #5E5958 | #504B4A |
| Trigger | crimson red | #AE3030 | #B33232 | #9F2C2C |
| Core glow (hidden) | warm yellow | #FBCE3A | #FFE551 | #ECBC28 |
| Firing starburst | faint yellow | #FAD03C at about 11 percent opacity (alpha 28 of 255) | | |

Shading style:
- **Gray parts:** soft per-pixel noise in a few levels, no scratches, no dents, no labels. Hex ring slabs are almost flat: the big faces carry the light tone, the thin ends a uniform #8E8B8D.
- **Pale parts:** low-contrast with a slight warm pink cast (reactor, grip, upper rails). They are the brightest part of the gun.
- **Red:** saturated crimson with a slight darkening toward shadows. The fin has more contrast (#7F2323 to #AD3030).
- **Hazard windows:** on each of the three **wide sleeve flats**, a **6 long by 4 wide dark inset panel** (#4E5050-ish, vignette to #434142 at the edge), centered on the flat with 1 unit of gray frame all around. In its exact center sits a **2 by 2 checker of pure yellow (#FFFC00) and pure black (#000000)**: yellow top-left and bottom-right, black the other two (the two upper wide flats share this variant; the bottom flat is mirrored, yellow top-right and bottom-left). This is a tiny radiation-trefoil stand-in. Only these checkers use pure yellow and pure black.
- **End-cap marks:** two small black 2 by 2 squares, one centered on the rear cap's rear face and one centered on the front cap's front face. The only black on the gun besides the checkers.

## 6. In-game presentation

All transforms apply to the model as a whole, in block units after the model's 1/16 scale. The model is first rotated 180 degrees about X (Y-down becomes screen-up).

| View | Orientation and scale | Offset and notes |
|---|---|---|
| First person | yaw -45 degrees, scale 1.1 | idle offset about (0.2, -0.5, -0.2). **The original applies this offset twice** (once in the base transform, again in the state branch), so the effective offset is about double; see section 10. Camera sees the gun nearly side-on, muzzle away to the left of the screen. At scale 1.1 the 43-unit gun is about 3 blocks long. |
| First person firing | tiny shake: shift of up to 0.05 in X and 0.025 in Y, roll up to 0.5 degrees about Z | follows a full sine over the recoil window. The shake is subliminal; the starburst and the muzzle flash do the work. |
| First person reloading | roll about Z up to 20 degrees, yaw up to -10 degrees | plus a shift of up to (0.5, 0.25, 0.25) at the peak, rising and falling smoothly. |
| First person aimed | extra offset about (0, 0.1, -0.2) on top of the generic aimed pose | no scope and no zoom overlay. |
| Third person (held) | yaw 225 degrees, scale 1.25 | offset (-0.7, -0.6, 0.1). |
| Third person recoil | slides back along the barrel up to 0.25 in the first eighth of the recoil, then eases back to zero over the first half | no rotation. |
| Third person reload | roll about Z up to 40 degrees, yaw up to -50 degrees, pivot near (-0.2, -0.1) | rises and returns over the reload. |
| Ground (dropped) | yaw 90 degrees, scale 0.75 | offset (0.6, -0.2, 0). |
| Inventory icon | yaw 180 degrees, scale 0.65 | offset (-0.1, -0.3, 0.4); drawn side-on. There is no sprite; the icon is the 3D model. |

**Muzzle flash:** a **beam-flash sprite**, drawn additively at about (-0.7, -0.2, -0.3) in model-space blocks, scale 1.5, turned 90 degrees about Y, in first person only. It is a texture effect (not geometry), and a builder can substitute any additive glow flare at the emitter.

**Loaded versus empty:** the model draws identically at all ammo levels. There is no ammo-dependent geometry.

**Akimbo:** none.

## 7. Motion

**No model part moves.** The prongs, the mast, the grip, the trigger and the reactor are all static. All motion is whole-gun transforms (section 6), plus one effect: the **firing starburst**.

**Starburst (visible only while firing):**
- Eight thin flat blades, each **7 long (X 28 to 35, exactly the housing length), 5 deep and 1 thick**, radiating from the barrel axis. Each blade's inner edge is **1 unit from the axis** and its outer edge **6 units** out, so they emerge 3 units from the reactor's flats.
- Positions around the axis, measured from straight up: **+-25, +-63, +-117 and +-155 degrees**. So there is no blade straight up and none straight down (a 50-degree gap at each), and the other gaps alternate 38 and 54 degrees. The original renders the 25-degree blade twice, so that one is slightly denser.
- Texture: the faintly translucent yellow (#FAD03C at about 11 percent opacity), alpha-blended (not additive), **full-bright** (unaffected by world light), double-sided (no culling).
- **Pulse:** the whole fan scales uniformly about the barrel axis, between **0.8 and 1.0** of its size, following a sine of the fire progress (one full cycle across the progress 0 to 1, and starting from 0.9). In first person, the fire progress rises while the trigger is held (about 1 second to reach 1.0) and is replaced by the recoil progress while a shot is recoiling; in third person it is the recoil progress. The starburst is hidden when the progress is zero.
- Net effect: while firing, a faint golden eight-bladed halo throbs around the reactor, poking out of the housing by about 3 units, extending fore and aft to the reactor's ends.

**Beam:** the ray itself is a separate projectile effect (see the [Techguns NDR stats](../../02-guns/gun-catalog.md#table-a-identity-and-ammunition)); nothing in the model fires it.

**Optional (not in the original):** the builder may keep the starburst blades, the mast and the fin as separate named parts so later code can animate them. The rest pose must match this document.

## 8. What to avoid

- **A symmetric emitter.** There must be exactly **three** prongs at 12, 4 and 8 o'clock; not four, not six, and not fully symmetric: the mast belongs on the lower-left one only.
- **A fat or tapered barrel.** The hex tube is 8.7 across flats and the reactor is 6; they are not round and not cone-shaped.
- **Making the bracket thick.** It is 1 unit thick (a flat blade); a 4-wide stock changes the whole rear silhouette.
- **A lit core visible in the middle.** The original has none; the glow is only the starburst. Do not make the housing translucent or neon unless section 10's window option is chosen.
- **Muzzle flash geometry** on the model (the original draws a sprite effect).
- **A rotating or opening anything.** The model has no animation.
- **A centered grip.** It sits 2 units to the gun's right. (The trigger too.)
- **Removing the hazard checkers.** They are tiny but they are the gun's only pure yellow and pure black; keep them at the sleeve's three wide flats.
- **A black or dark reactor.** It is the brightest mass on the gun.
- **Camo.** The Nuclear Death Ray has one texture only. There is no camo variant.

## 9. Acceptance checklist

- [ ] Length : height : thickness is within about 10 percent of 4.3 : 1.9 : 1 (43 by 18.5 by 10, mast excluded).
- [ ] Shoulder bracket is 11 long, 1 unit thick over most of its length, with a 4-tall hook at the heel, a 6-long top bar, a 5 by 4 web, and a 2 by 6 by 4 end block.
- [ ] Two regular hex rings, 3 long, 10 across corners and 8.7 across flats, with flats facing up and down, at X 11 to 14 and X 24 to 27.
- [ ] A narrower hex sleeve (about 7.8 across the flats) between 1-long dark plates, 8 long, with alternating narrow and wide flats.
- [ ] Three hazard windows (6 by 4 dark panels with a 2 by 2 yellow-and-black checker) on the three wide sleeve flats.
- [ ] Three-tier rail on the tube (14 by 4, 10 by 3, 6 by 3, each 1 tall), with a swept red fin tilted 45 degrees near X 18 reaching about Y -8.6.
- [ ] Reactor is 9 long including caps, with a 6 by 6 cross-section with 1 by 1 corner notches, in warm off-white.
- [ ] Three prongs at 12, 4 and 8 o'clock, each a 9-long 1 by 1 bar at about 4.3 radius bending 45 degrees inward at the reactor's front end.
- [ ] The lower-left prong carries a pale cube and a red 2 by 6 by 2 diamond-section mast pointing left and 30 degrees down, reaching about 10 left of the axis.
- [ ] Emitter has a hex core (5.2 across flats, 3 long) and a hex cup of 1-thick plates (7 across flats, 3 to 4 long, ragged rim, ending at X 43).
- [ ] Pistol grip is vertical, 2 by 5 by 2 pale, hanging to Y +10, **2 units right of center**, with a red 1 by 3 by 1 trigger raked forward about 23 degrees.
- [ ] Palette matches section 5: gray tube, pale reactor, red accents, dark charcoal emitter, no other saturated colors.
- [ ] No part floats; every box touches or overlaps a neighbor.
- [ ] No camo variants; one skin.
- [ ] While firing, an eight-bladed faint-yellow starburst pulses around the reactor (blade 7 by 5, radius 1 to 6), with no blade straight up or down; hidden when not firing.

## 10. Unsure

- **Which end is the muzzle.** The notes assume the prongs and cup at +X are the muzzle (consistent with the rest of the spec) and the skeletal bracket at -X is the stock. The model could be read the other way, but the render transforms and the claw geometry argue strongly for this.
- **Left and right.** "+Z is the gun's left" follows from the handedness of the model after the 180-degree flip, not from a render. If wrong, the mast and the grip swap sides. Whichever side the builder picks, keep the mast and grip on **opposite** sides (the mast on the side opposite the grip).
- **The grip offset (2 units right) and the antenna.** These are in the original's numbers, but they may be author slips. They are kept to match; a builder who centers the grip loses very little.
- **The invisible core glow.** The core box is fully enclosed in the original. If a visible glow is wanted, a small pale-yellow window on each side of the housing (4 by 4) is the harmless fix. The starburst blades extend from the same region and will look right with it.
- **First-person offset.** The double application of the idle offset makes the real placement about twice the declared offset. The numbers in section 6 are read from formulas, not seen in game. Treat as approximate and tune by eye.
- **Starburst timing.** The exact fire progress sampling (item-use time against recoil time) is read from the formulas and may differ in play. The scale pulse is small (0.8 to 1.0), so errors are hard to see.
- **Hazard checker mirroring.** The upper two wide flats and the bottom wide flat have mirrored checkers; which way it reads on each side of the gun is not determined. Either is fine.
- **Stripe on the sleeve's narrow flats.** The brightest line is on both the top and bottom faces of the narrow slabs; only one of these faces points outward on each flat. The builder may put it on every narrow flat.

## Round 3 corrections (measured)

Round 3 probe of master (model compared facing the same way as the original): **right 0.891 (worst)**, front 0.929, top and bottom 0.960, iso 0.922 and 0.972. Overall length (43), width and the cross-shaped reactor housing, the pistol grip, the prongs' arm bars and the red mast match the original within 0.1 unit. This page's frame is used: X from the bracket's rear tip, **Y down from the barrel axis**, **+Z the gun's left**. All values are re-measured and replace any conflicting figure above.

1. **Heights relative to the barrel axis (the cause of the top red edge in the side comparison).** The antenna rod (the 1 by 4 rod turned 45 degrees at the top, about X 15 to 19) is **8.6 above the barrel axis at its highest point (Y -8.6)**, not 9.0. Ours is 0.4 too high. Put the rod's four corners at **(15.4, -5.8), (16.1, -5.1), (18.2, -8.6) and (18.9, -7.9)**; ours has the same shape but sits at X 15.0 to 18.6 (0.4 rearward) and Y -9.0 to -5.5. Net: move the rod **0.4 toward the muzzle and 0.4 down**. The rest of the model then stays where it is.
2. **Hex tube: the four slanted sleeve slabs sit 0.7 too high.** The hex tube is X 15 to 23. Its top plate (Y -3.5 to -0.5) and bottom plate (Y 1.3 to 4.3) are right. The four slabs, each 8 long, rolled 30 degrees about the barrel axis, have these measured side-view bounds: **upper pair Y -3.5 to +3.2; lower pair Y +0.2 to +4.3**. Ours are Y -4.2 to +2.5 and Y -0.5 to +3.6: raise nothing, **lower both pairs by 0.7**.
3. **The same four slabs sideways (front and top views).** Measured Z ranges. Upper pair: one slab spans **Z -1.1 to +4.5**, its mirror **Z -4.5 to +1.1**. Lower pair: **Z +0.4 to +4.5** and **Z -4.5 to -0.4**. Ours: upper pair -0.7 to +4.9 and -4.9 to +0.7 (0.4 too far out); lower pair 0 to +4.1 and -4.1 to 0 (0.4 too far in, touching). Move each upper slab **0.4 inward** and each lower slab **0.4 outward**, so the lower pair has a 0.8 gap between them at the mid-plane and the upper pair overlap by 2.2.
4. **Reactor region (X 28 to 35): ours has ten extra slabs.** Besides the cross-shaped housing, the original has **exactly one extra plate** here: 7 long (X 28 to 35), about 1 thick and 5 wide, **rolled 25 degrees about the X axis**, with side-view bounds **Y -5.6 to -0.7** and **Z -3.0 to 0.0** (on the gun's right, upper side; its corner positions in the Y/Z plane are at Y -5.6 and -5.2 on one edge and Y -1.1 and -0.7 on the other). Ours instead has these, all of which must be **removed**:
   - the mirror of that plate at Z 0 to +2.6 (Y -4.8 to -0.7 in ours);
   - two plates rolled 25 degrees below the axis, Y +0.6 to +4.7, Z +2.6 to 0 and 0 to -2.6;
   - four plates rolled 27 degrees, 7 long, 2.7 tall, at Y -2.8 to -0.1 and Y 0.0 to +2.7, Z +4.7 to +0.7 and Z -0.7 to -4.7;
   - two 4 long, 3 tall, 1 wide boxes at X 29.5 to 33.5, Y -1.5 to +1.5, Z +2 to +3 and Z -2 to -3.
   This is the blue patch hanging below the reactor (side view, X 28 to 35, Y +3 to +4.7) and the extra bulk in the front view. Keep ours' retained plate at Z 0 to -2.6 but retarget it to the original's size and position above (Y -5.6 to -0.7, Z -3.0 to 0.0).
5. **Front ring claw tips and hex core slabs (X 35 to 39).** The two 45-degree claw tips (X 35.3 to 38.8, Y -4.8 to -1.3) and the six hex core slabs (the ones rolled 30 degrees at X 35.3 to 38.8, Y +0.2 to +2.8) are **0.3 to 0.4 too far forward in ours** (ours X 35.6 to 39.2). Pull them back **0.3 toward the rear**. The hex core is therefore X 35.3 to 38.8 long (3.5 across the roll AABB), the cup starting at X 39.
6. **Upper knob block and trigger.** The 2 by 2 knob under the front ring is at **X 25.5 to 27.5**, Y -5.3 to -3.3, Z -1 to +1 (it is the top arm's base knob). Ours is at X 24.5 to 26.5: move **1.0 toward the muzzle**. The trigger (1 by 3, turned 23.5 degrees, lower end toward the muzzle) is at **X 25.2 to 27.3, Y +4.1 to +7.2, Z -1.5 to -2.5**; ours is at X 26.0 to 28.1: move **0.8 toward the rear**.
7. **Rear bracket underside.** Ours sits 0.4 lower relative to the antenna only because of item 1; relative to the barrel axis the bracket, grip and hex are already correct. No separate change.

Palette: unchanged.
