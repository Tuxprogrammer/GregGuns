# Alien Bug: clean-room model notes

Design notes for rebuilding the Techguns **Alien Bug** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer, entity class and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout and paints its own texture.

Related facts: [rendering and models](../rendering-and-models.md#75-modelalienbug-extends-modelbase-tabula-411-export), [renderer](../rendering-and-models.md#4-renderalienbug-extends-renderliving), [texture sheet](../textures.md#texturesentityalienbugpng-6464-about-34-opaque), [per-mob sheet](../per-mob.md#15-alien-bug-alienbug).

**Frame used in this document.** The Minecraft entity-model frame. One unit = 1/16 block. **Y points down**, and **y = 24 is the ground** (the model origin is 24 units above the feet). The mob **faces -Z**: the snout points toward -Z and the tail toward +Z. Its **right side is -X**. **This is not a biped.** The model has no vanilla biped pivots; every part is placed in absolute coordinates in this frame. Positions below are absolute in this frame, to the nearest half unit unless it matters. The original's texture is 64 x 64 and only the top three quarters are painted; the builder may use its own layout and size.

## 1. Gestalt

A **sand-tan, four-legged insect** about 1.8 blocks long and 1.25 blocks tall, crawling low on long, spiky legs. At a glance you read: a long, narrow **toothed snout with two jaws gaping open**; a small head under a **raised carapace ridge and a flat, spiky frill**; a segmented body whose **rear curls up into a tail**; and four legs that rise out from the sides of the thorax, **peak above the body** and then drop to the ground, like a spider's but with only four. It is warm tan and orange with cream teeth and bone-gray lower legs; there is no bright or glowing color.

Defining features in order of visual importance:

1. **Long gaping snout:** two jaws about 9 units long, the lower one swung down about 26 degrees and the upper one raised about 10 degrees, with cream teeth and two black eyes on the sides.
2. **Four legs with a high knee:** each leg rises up and out at about 28 degrees from the thorax side, then bends sharply down to the ground. The knees are higher than the back.
3. **Raised segmented abdomen:** three tapering segments (6, 4, then 2 units wide) that curl upward behind the thorax to a small stinger-like tip.
4. **Crest and frill:** a wedge-shaped carapace ridge on top of the head and a thin, flat, mostly cut-out, spiky plate above it, both tilted up toward the back.
5. **Sand-tan chitin** with orange and brown striping, and paler bone-gray feet.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Length, snout tip (z about -13) to tail tip (z about +16) | about 29 units, **1.8 blocks** |
| Height, ground (y = 24) to the top of the frill (y about 3.5) | about 20.5 units, **1.3 blocks** |
| Width across the splayed feet (x about +-11.5, plus the foot thickness) | about 24 units, **1.5 blocks** |
| Width of the thorax | 5 units; head 5 units; snout 4 units; abdomen 6, 4, 2 |
| Thorax | 5 wide, 5 tall, 9 long |
| Head | 5 x 5 x 6 |
| Render scale | **1.0** (none) |
| Shadow | radius **1.0** block |
| Hitbox (for reference) | 1.1 wide x 1.2 tall; the model is about 40 percent wider than its hitbox |

Ratios: the snout is nearly a third of the total length. The thorax is the main mass: 5 x 5 x 9. The head is as wide as the thorax. The tail is higher than the thorax's top by about 3 units. The legs' knees are about 1 unit above the thorax's top, and the feet are about 11.5 units out from the centerline: the legs are twice as wide as the body.

## 3. Part by part

Everything is a plain box. All parts are placed with absolute coordinates (y = 24 is the ground, z = 0 is about the front legs' attachment). "Pivot" is the point a part rotates around. The head, jaws, crest and frill are one group that turns as a unit; each leg is a two-piece chain; the body segments are fixed.

### Hierarchy
- **Head group (pivot at the head's center, (0, 11.5, -1)):** head, upper jaw, lower jaw, crest, frill. The jaws, crest and frill are children of the head. The head turns as a whole.
- **Body, fixed (no children):** thorax, three abdomen segments.
- **Four leg chains**, each: upper leg, with a lower leg as its child.

### Thorax
- A block **5 wide, 5 tall, 9 long**: x -2.5 to 2.5, y 14 to 19, z -2 to +7. No rotation.
- The main mass. The head sits on its front end, the abdomen rises from its rear end, and the legs come out of its sides.

### Head
- A block **5 x 5 x 6**: x -2.5 to 2.5, y 9 to 14, z -4 to +2, centered at (0, 11.5, -1). Its bottom face rests on the top of the thorax's front end (y = 14), and it overhangs the thorax's front by 2 units.
- It turns about its own center with the look direction (yaw and pitch, section 7). At rest it is level.
- Plain tan carapace. It is the base for the jaws and crest.

### Upper jaw (snout)
- A block **4 wide, 4 tall, 10 long** (before tilt): x -2 to 2, y 8 to 12, z -13 to -3 (it extends 9 units forward of the head's front face and overlaps the head by 1 unit). Its pivot is at its rear, at (0, 10.5, -3), the top-rear of the lower jaw's hinge.
- **Tilted up about 10 degrees** (its front end raised by about 1.8 units, so the tip is at y 6 to 10, z -12.5). It idles with a small wobble (section 7).
- Painted with a pair of **black eyes** on its sides near the head end, and **cream teeth along its lower edge**.
- Touches: the head's front face; the lower jaw below it at the hinge.

### Lower jaw
- A block **3 wide, 4 tall, 9 long** (before tilt): x -1.5 to 1.5, y 10 to 14, z -12.5 to -3.5. Pivot at its rear, at (0, 10, -3.5).
- **Tilted down about 26 degrees** (its front end swings down by about 4 units: the tip is at y 14 to 17.5, z -11.5 to -10). It is narrower than the upper jaw by 1 unit, so it looks like it sits inside the upper one.
- Cream **teeth along its upper edge**, interleaving with the upper jaw's teeth.
- The two jaws together make a V-shaped open mouth, with a gap of about 3.5 units between the tips at rest. It is never closed at rest.

### Carapace crest
- A block **6 wide, 3 tall, 8 long** on top of the head, **tilted up toward the back by 20 degrees** (the front end is low, touching the head's top face at about y = 11; the rear end rises to about 4 to 4.5 units above the head's top, y about 4.5 to 7).
- Its pivot is (-3, 5, 2.5) in model space (3 to the right of the head's centerline, 6.5 above the head's center, 3.5 behind it). The crest is centered on the centerline (x -3 to 3).
- It overlaps the head's top at its front end and floats above it at its rear end, as a wedge-shaped ridge. Plain tan carapace.
- Touches: the head only, at the front.

### Frill (flat plate)
- A **flat square plate 14 x 14 with zero thickness**: x -7 to 7, z -4.5 to +9.5 (before tilt), at y about 6, **tilted up toward the back by 20 degrees** (the front edge at y about 8.3, the rear edge at y about 3.5). It has the same pivot as the crest, so it rides just above the crest and extends 3.5 units beyond it on each side and several units in front of and behind it.
- Almost entirely **cut out**: only about 29 percent of it is painted, with a dark brown spiky pattern, so from above you see a branching, antenna-like silhouette, not a plate. It is visible only from above (the underside is unpainted, so it does not show from below).
- NOT a solid plate, and NOT a thick part. It is an alpha-cutout decal.

### Abdomen segments (three)
- **Segment 1:** a block 6 wide, 4 tall, 4 long, pivoting at (-3, 13, 6) (x -3 to 3, y 13 to 17, z 6 to 10 before tilt), **pitched 34 degrees** so its rear rises: the rear top edge is at y about 10.8 and its rear end at z about 9.3 to 11.5. It overlaps the thorax's rear by 1 unit.
- **Segment 2:** a block 4 wide, 3 tall, 4 long, pivoting at (-2, 11, 10) (x -2 to 2, y 11 to 14, z 10 to 14), **pitched 13 degrees**, rear raised. Its rear end is at z about 14 and y about 10 to 13.
- **Tail tip:** a block **2 x 2 x 2** at x -1 to 1, y 10.5 to 12.5, z 14.2 to 16.2, not rotated.
- Together they form an arc that tapers and curls up: 6, 4, 2 wide; the highest points are about y 10 to 11, 3 units above the thorax's top, and the tip projects a little past the last segment. They are all fixed (do not move during walk or idle).

### Upper legs (four)
- Each is a block **9 long, 3 tall, 3 wide** (3 x 3 section), pivoting at its inner end, **inside the thorax** at y = 17: x = -1.5 (right legs) or +1.5 (left legs), z = 0 (front pair) or z = 4 (rear pair).
- At rest, each leg points **outward and up at about 28 degrees** above horizontal, and is **swung 20 degrees toward the end of the body it belongs to** (the front legs lean 20 degrees forward of straight sideways; the rear legs lean 20 degrees back). The end of the upper leg is about 7.3 units out from the pivot sideways, 4.2 up (y about 12.8), and 3 units forward or back.
- The legs emerge from the thorax's sides and arch **above** the thorax top (y 14) at the knee.

### Lower legs (four)
- Each is a block **11 long, 2 tall, 2 wide**, a child of the upper leg, pivoting 7.5 units along the upper leg (so it starts 1.5 units before the upper leg's end and overlaps it), bent **about 100 degrees** from the upper leg's line.
- The result points **mostly straight down**, leaning about 20 degrees outward, and about 0.5 unit forward or back. Its tip reaches the ground (y about 24) at about x = +-11.5 from the centerline, and about z = -2 for the front feet and z = +6 for the rear feet (so the feet are 8 units apart front to back).
- It is thinner than the upper leg (2 versus 3) and bone-gray, not tan.

## 4. Silhouettes

- **Side (the main view):** from the left, a long open snout, the upper jaw nearly level and slightly up, the lower jaw angled down with a V gap between them and cream teeth; the small head with the ridge rising toward the back and the thin spiky frill above; the thorax block; and then the abdomen arcing up and back to the small tip, higher than the thorax. Two of the four legs show: each rises from the thorax in a narrow arch, drops steeply to the ground, and the feet are about 8 units apart front to back. Negative space: the gap between the legs and under the body (about 5 units under the thorax, since its bottom is at y = 19 and the ground at 24); the V between the jaws; the space between the thorax and the raised tail.
- **Front:** a low, wide "A" or "M" shape. The narrow snout (4 wide) with the jaw gap in the center; the head (5 wide) behind it with the crest above; the frill's spiky outline above; on each side a leg arching out and up from behind the head, peaking about 4 units above the thorax and then dropping to the ground 11.5 units out. Negative space: two tall triangles between each leg and the body.
- **Back:** the abdomen arc rising from the thorax (6, 4, 2 wide), the tail tip; both rear legs arching out from the sides, and the front legs partly behind them; the frill and crest from behind.
- **Top:** a long narrow body: the snout in front, the head, the wedge crest, the frill spreading out (14 wide) as an open, spiky pattern, the 5-wide thorax with the legs splayed out to the sides in an X (the front legs angled forward, the rear back), the abdomen narrowing behind to the tip.
- **Three-quarter:** the strongest reading is the **toothed gape** and the **high-kneed legs**, with the tail curling up behind and the frill as a spiky halo.

## 5. Materials and palette

The original texture is small, warm and low-contrast: a sand-tan chitin with darker orange and brown striping, plus cream for the teeth. About 66 percent of the canvas is unused and transparent; most of the rest is opaque, except the frill (about 71 percent cut out) and a few jaw pixels.

| Part | Base | Highlight | Shadow / detail |
|---|---|---|---|
| Head | #B88F51 | #CAAF71 | #B3753D (orange patches) |
| Upper jaw (snout) | #AE7D44 to #C5A364 | #DAC1A2 | #9B6F39; eyes #000000; teeth #F1DCC8 |
| Lower jaw | #B59054 to #C8A869 | #E1C19A | #9F763F; teeth cream |
| Crest | #CAA968 | #C9A462 | #925E2F |
| Frill (the painted part only) | #584124 (dark brown) | spikes #BFA36D | #4A351C |
| Thorax | #C29958 | #CAAF72 | orange patches #BA7941, #B7763F; shadow #AC6D38 |
| Abdomen segments | #B68D4F | #CBAF71 | bands #835E2D, #A36C38 |
| Tail tip | #AC723E | #BA9759 | #97562E |
| Upper legs | #B78E50 | #CCAC6C | stripes #906934, #8F5A2E |
| Lower legs | #8E8877 (bone-gray) | #D4CDB9 (feet) | #868171 |

Shading style:
- **Chitin:** flat tan with chunky, low-contrast pixel noise of several levels, and **darker orange-rust patches and banded stripes** (brown bands across the abdomen and thorax, a pair of dark ")(" bracket marks on one side of the second abdomen segment or the thorax). Highlights are a pale sand (#CAAF71) on the top faces of the head, thorax and crest. No gradients, no glow, no wetness.
- **Eyes:** two black marks, 1 unit wide and 2 tall, one on each side of the upper jaw near the head.
- **Teeth:** a **cream zigzag** (1 unit tall, white-cream #F1DCC8) along the lower edge of the upper jaw's side faces and along the upper edge of the lower jaw's side faces: a row of about 4 or 5 teeth per side.
- **Legs:** the upper legs are warm orange-tan with 2 or 3 dark brown stripes across their length; the lower legs are a distinct cooler bone-gray with the lightest cream at the tips.
- **Frill:** a dark brown, roughly oval central blob with thin tan spikes radiating out of it on all sides (about 8 spikes, 3 to 5 units long), drawn on an otherwise fully transparent 14 x 14 plane.
- **Emissive:** none.
- **Transparency:** a hard alpha cutout (a pixel is either fully opaque or fully transparent; the engine discards the transparent ones). The frill is 71 percent transparent; the jaw faces have a few cut pixels at the teeth and corners; the rest of the canvas is unused.

## 6. In-game presentation

- **Render scale:** 1.0 (no scaling). **Shadow:** 1.0 block radius, larger than the 1.1-wide hitbox needs.
- **Held items:** none.
- **Armor layers:** none.
- **Glow / blend / depth:** none. The model is drawn opaque with alpha cutout; no blending, no emissive, no depth trick.
- **Parts that are not drawn directly:** the crest, frill and both jaws are drawn only as children of the head group (the original skips drawing them on their own).
- **Gibs and blood:** the bug uses its own eight-piece gib set in the same texture and yellow-green blood ([04-projectiles-fx](../../04-projectiles-fx/index.md)).
- **Sounds** (context): its own set (idle, hurt, death, step, bite, aggro); see [sounds.md](../sounds.md).

## 7. Motion

The bug is not a biped; it has its own animation. All numbers are in radians; the walk amount is the limb swing amount from 0 (standing) to 1 (full speed; the bug's speed attribute is 1.0, so it usually runs near 1).

- **Head look:** the head group's yaw follows the look yaw and its pitch the look pitch, directly, with no model-side limits (the engine limits head yaw to about 75 degrees from the body). The crest, frill and jaws turn with it. The head's 7.8-degree rest tilt is replaced by the pitch each frame, so at pitch 0 the head is level.
- **Legs:** each frame every leg starts from its rest angles, then offsets are added. Let the phase be the limb swing distance x 0.6662 and let a = 0.4 x amount. The offsets, **per leg** (all four are mirror-symmetric left-right, so I describe the right-hand leg and the left mirrors it):
  - **Sweep** (the leg swinging forward and back about vertical): amplitude **0.5 rad x amount** (about 29 degrees at full speed), a cosine at twice the phase rate, **the two front legs in unison** and **the two rear legs in unison**, **front and rear pairs in opposite phase**.
  - **Twist** (the upper leg rolling about its own length, which swings and lifts the lower leg): amplitude **0.4 rad x amount**, the same frequency and phases as the sweep. On the left and right it is mirrored.
  - **Lift** (the upper leg raising and lowering): amplitude **0.3 rad x amount**, about 17 degrees at full speed. It oscillates at **three times** the phase rate for the front legs and **four times** for the rear legs, in unison within each pair.
  - At amount 0 (standing) the legs sit at their rest angles. Walking is a fast, scissoring scuttle, not a diagonal pairs gait.
- **Idle jaws:** at all times the jaws wobble: the upper jaw's pitch oscillates by **+-0.1 rad (about 6 degrees)** with a period of about 63 ticks, and the lower jaw by **+-0.1 rad** with a period of about 31 ticks. They are not synchronized, so the mouth opens and closes slowly and irregularly. The rest gape is the tilt in section 3.
- **Bite attack:** when the bug bites, a 10-tick timer starts. In the first five ticks (timer 10 down to 6) the jaws **snap shut**: the upper jaw drops by up to **0.3 rad** and the lower jaw rises by up to **0.4 rad**, easing in and out (a smooth cosine curve), so the mouth goes from a 37-degree gape to fully shut (the jaws even cross by a few degrees) at the start of the bite and reopens smoothly over those ticks; below timer 6 the jaws return to idle. Nothing else moves; there is no lunge.
- **Spinning parts:** none.
- **Body, abdomen and tail:** completely rigid; they do not sway.
- **Death:** the vanilla topple onto the side (90 degrees over about 20 ticks), plus the gib burst. The renderer's attempt to make it a full 180 degrees does not take effect.

## 8. What to avoid

- **Do not make it a spider with eight legs.** It has four legs, two segments each.
- **Do not give it a biped skeleton or vanilla limb names.** It has no body/arm/leg pivots; the head group is the only look-driven part.
- **Do not make the legs straight out.** They rise about 28 degrees, then drop; the knee is higher than the thorax's top.
- **Do not close the mouth at rest.** The jaws gape by about 37 degrees, with the tilts given above.
- **Do not make the frill a solid plate.** It is a zero-thickness, mostly transparent plane with a spiky dark pattern.
- **Do not make the tail droop.** It curls **up**, with the tip above the thorax's top.
- **Do not make the legs tan all the way.** The lower legs are cooler, bone-gray.
- **Do not add glowing eyes, wings, stingers, hair or armor plates.**
- **Do not scale the model or the shadow with size.** Scale is 1.0, shadow 1.0.
- **Do not animate the abdomen or tail.**

## 9. Acceptance checklist

- [ ] Length about 29 units (1.8 blocks), height about 20 units (1.25 blocks), width across the feet about 24 units (1.5 blocks); render scale 1.0; shadow radius 1.0.
- [ ] Thorax is 5 x 5 x 9, with the head (5 x 5 x 6) sitting on its front end.
- [ ] A 10-long toothed snout made of an upper jaw (4 x 4 x 10, tilted up about 10 degrees) and a lower jaw (3 x 4 x 9, tilted down about 26 degrees); a visible open gap.
- [ ] Two black eyes on the sides of the upper jaw; cream teeth along both jaws.
- [ ] A wedge crest (6 x 3 x 8) tilted up 20 degrees to the rear on top of the head, plus a flat 14 x 14 zero-thickness frill, mostly transparent, with a dark spiky pattern.
- [ ] Three abdomen segments (6, 4, 2 wide) rising behind the thorax, the tip higher than the thorax's top.
- [ ] Four legs, each a 9-long 3 x 3 upper leg rising about 28 degrees and an 11-long 2 x 2 lower leg dropping to the ground; front legs lean 20 degrees forward, rear legs 20 degrees back; the feet are about 11.5 units out.
- [ ] Upper legs tan and striped, lower legs bone-gray.
- [ ] Chitin is sand-tan (#B88F51 base, #CAAF71 highlight) with orange-brown stripes; teeth cream (#F1DCC8).
- [ ] The texture is an alpha cutout; about two thirds of the canvas is unused.
- [ ] Head look drives the head group (head, jaws, crest, frill) by yaw and pitch.
- [ ] Walk: front pair in unison, rear pair in unison, opposite phases; sweep 0.5, twist 0.4, lift 0.3 rad x amount.
- [ ] Idle jaw wobble of +-0.1 rad with periods of about 63 and 31 ticks.
- [ ] Bite snap: the jaws shut over a 10-tick timer and reopen smoothly.
- [ ] Nothing floats; every part overlaps or touches its neighbor.

## 10. Unsure

- **Leg angles.** I worked the rest rotations through by hand (elevation about 28 degrees, a 20-degree forward or back lean, a 100-degree knee bend). The resulting foot positions (about 11.5 out, 8 apart) are computed, not seen in game. The exact stance may be a few units different.
- **Which way each leg's twist moves the foot.** I give the amplitudes and the phase relationships, and I am sure that the left and right sides mirror each other, but I did not verify visually which direction (forward or back, up or down) each sign moves the foot. The builder should tune it by eye to a "front pair forward while the rear pair goes back" scuttle.
- **Frill and crest placement.** Their pivot is high above the head and the tilt is about it, so my coordinates for the rear edge (y about 3.5) are computed, not seen. The frill is the most uncertain part of the geometry.
- **Frill visibility from below.** It looks like only the top face is painted; whether the underside is visible depends on face culling in the engine. The builder may paint both sides without harm.
- **Eyes.** I read two black marks near the head end of the upper jaw's sides. They could also be on the head's front; either reads the same.
- **Bite timing.** The 10-tick timer and the curve are read from formulas, not seen; the original's own code marks the animation as unfinished.
