# Cyberdemon: clean-room model notes

Design notes for rebuilding the Techguns **Cyberdemon** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer, entity and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout and paints its own texture.

Related facts: [rendering and models](../rendering-and-models.md#73-modelcyberdemon-extends-modelbiped), [texture sheet](../textures.md#texturesentitycyberdemonpng-128128-fully-opaque-on-a-black-background), [per-mob sheet](../per-mob.md#11-cyberdemon-cyberdemon).

**Frame used in this document.** The Minecraft entity-model frame. One unit = 1/16 block. **Y points down.** The mob **faces -Z** and its **right side is -X** (so its left is +X). The model origin is 24 units above the feet, so the soles are at y = 24 and the top of the head is at y = -8. The vanilla biped pivots apply: head and body at (0, 0, 0), right arm at (-5, 2, 0), left arm at (5, 2, 0), right leg at (-1.9, 12, 0), left leg at (1.9, 12, 0). Every part of this model hangs off one of those six pivots, and each pivot is an empty node that moves like a vanilla biped limb. The original's texture is 128 x 128 (the model is authored on a 64 x 64 grid and the file is drawn at double resolution), so the builder may use a 64 x 64 skin at 2x or its own layout at any size. Sizes below are in model units, to the nearest half unit unless it matters.

## 1. Gestalt

A hulking, **skinless red demon wearing cheap cybernetics**. At a glance you read: a wide, top-heavy body of raw **blood-red muscle**; a **bull-horned head** with a heavy, toothed snout; massive arms hanging forward like a gorilla's; and **zig-zag digitigrade legs** ending in flat gray hooves. Gray steel boxes, a red glowing lens and tan-orange cables are bolted onto the red meat, so it looks like an old arcade demon retrofitted with machine parts. The shins are the one big non-red area: bone-white, pinkish metal plating with orange cables.

Defining features in order of visual importance:

1. **Saturated red flesh over the whole body**, dark and mottled, with lighter red muscle striations. Red is the dominant color of the silhouette in any view.
2. **Forward-curling horns.** Each horn leaves the head sideways, then bends forward and slightly up like a bull's. The horns are pale bone-tan, not red.
3. **Heavy toothed snout** jutting forward from the lower face, with a black mouth and a row of white teeth.
4. **Hulking shoulders and arms.** The arms are wider than the body is, and the forearms angle forward and down, so the hands hang in front of the hips.
5. **Digitigrade legs** (thigh and shin both pitched forward 30 degrees, shin tucked behind the thigh) ending in flat gray hooves.
6. **Cyber-details:** one red glowing eye lens set in a gray plate, a gray box implant on the chest with an orange cable, a few cables over the muscle, pink-white metal shins.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Total height (head top to hoof soles) | 32 units = **2.0 blocks** |
| Width across the arms (outer edge to outer edge) | 22 units = about 1.4 blocks |
| Width across the horns | about 18 units (each horn tip reaches about 9 from the centerline) |
| Chest (widest part of the torso) | 12 wide, 7 tall, 6 deep |
| Pelvis (waist block below the chest) | 9 wide, 5 tall, 5 deep |
| Head | 8 wide, 8 tall, 7 deep (set 0.5 back from the pivot), plus a snout 2 deep |
| Depth, front to back | about 9 units from the snout tip (z -5) to the back of the head (z +4) |
| Render scale | **1.0** (no extra scaling, unlike the super mutants) |
| Shadow | radius **0.75** block |
| Hitbox (for reference) | 0.6 wide x 1.8 tall; the model is a little taller and much wider than its hitbox |

Relative sizes: the head is about a quarter of the total height. The chest is 12 wide against a 9-wide pelvis, so the torso tapers downward. The arms (5 and 4 wide) are about as thick as the legs. The arms are as long as the torso is tall (the shoulder is at y = 2, the hand ends at about y = 13). The horns do not rise above the top of the skull.

## 3. Part by part

Everything is a plain box. Positions are relative to the model origin (y = 0 at the shoulder line and top of the skull base, y = 24 at the ground), x positive to the mob's left. "Pivot" means the point a part rotates around.

### Hierarchy

Six empty pivot nodes, exactly the vanilla biped set. Children:

- **Head node:** skull, snout, two temple implants, two horn bases, two horn tips.
- **Body node:** chest, pelvis.
- **Right arm node:** upper arm, forearm. **Left arm node:** the same, mirrored.
- **Right leg node:** thigh, shin, hoof. **Left leg node:** the same, mirrored.

All children rotate with their node. Rest rotations on children are fixed offsets inside the swinging node.

### Skull
- 8 wide, 8 tall, **7 deep**: x -4 to 4, y -8 to 0, z -3 to +4. The face is at z = -3, so the skull is shifted 1 unit backward compared with a standard cube head (which would span z -4 to +4); the back of the head is at z = +4 either way.
- The head turns about the node's pivot at (0, 0, 0), the base of the skull.
- Touches: the snout on the front face, the implant blocks on the sides, the body below (the head bottom at y = 0 meets the chest top).
- NOT a vanilla head: there is no hat layer, no ears, no hair.

### Snout
- A block **6 wide, 5 tall, 2 deep**: x -3 to 3, y -5 to 0, z -5 to -3. It covers the lower 5 units of the face (it is flush with the chin at y = 0 and stops 3 units below the head top).
- It carries the mouth and teeth: a black mouth cavity across the front, with white tooth rows along it.
- NOT a long muzzle or beak: it is a short, wide, 2-unit slab.

### Eyes (painted on the skull's front, above the snout)
- Two eyes in the 3 units of face above the snout: one is a **gray steel plate with a round bright red lens** (a cyber eye, the plate about 3 units square, the lens about 1.5), the other is an organic **yellow slit** with a black outline. The red lens is on the mob's **right** (-X), the yellow slit on its left (+X). See section 10 for the doubt on that.

### Temple implants
- Two blocks, one each side, each **2 wide, 4 tall, 4 deep**: left x 4 to 6, right x -6 to -4, y -7 to -3, z -2 to +2.
- They are red flesh with darker shading. They are the mounting blocks for the horns, so they read as horn roots or bolts.
- Touches: the skull's side faces (flush) and the horn bases.

### Horn bases (the sideways horns)
- Each is **4 long, 3 tall, 3 wide**, pivoting at (±6, -5, +1.5) (the outer face of the implant, near its rear half), and **yawed 40 degrees**.
- At rest the box points out from the head sideways and **forward**: it leaves the implant at about x = 5, z = +0.5 and ends at about x = 8, z = -2.2. The outer and forward-most corner reaches about x = 9.
- Pale bone-tan color (#867E68, a bit lighter on top).
- Touches: the implant on its inner end; the horn tip on its outer end (the tip's rear half overlaps the base's end by about 1 unit).

### Horn tips (the forward horns)
- Each is **2 wide, 2 tall, 5 long**, pivoting at (±7.5, -5.5, -1) and **pitched upward 25 degrees** at the front. It extends forward from that pivot along -Z, to about z = -6, and its tip ends about 2 units higher than its root.
- Same bone-tan color as the bases, a bit lighter (#827A64) at the tip.
- The two horn pieces together make a hooked horn: out, then forward and up. The tips are 15 units apart and point forward, so from the front you see two horn tips flanking the face, each about 6 units from the centerline outward from the snout edge.
- NOT swept back, NOT upward spikes, NOT symmetric to vanilla horns.

### Chest
- A block **12 wide, 7 tall, 6 deep**: x -6 to 6, y 0 to 7, z -3 to +3. Pivot at (0, 0, 0).
- The widest part of the torso, much wider than a vanilla body (8). Muscle pecs and a ribbed abdomen are painted, not modeled. A gray box implant with an orange cable is painted on one side of the front (section 5).

### Pelvis
- A block **9 wide, 5 tall, 5 deep**: x -4.5 to 4.5, y 7 to 12, z -2.5 to +2.5. Directly below the chest; the chest overhangs it by 1.5 units on each side and 0.5 front and back, giving a V-shaped torso.
- Painted with a dark, slightly necrotic belly patch and a cable.

### Upper arm
- A block **5 wide, 7 tall, 5 deep** hanging from the shoulder pivot (right at (-5, 2, 0)): spans x -11 to -6 (right) or 6 to 11 (left), y 0 to 7, z -2.5 to +2.5.
- It sits **flush against the chest's sides** (the chest's outer face is at 6), so the shoulder line reads as one wide, continuous slab with no gap.
- The arm node swings about the shoulder pivot.

### Forearm
- A block **4 wide, 8 tall, 4 deep**, a child of the arm node (so it rotates about the shoulder pivot, not about an elbow) with a **30 degree forward pitch** (its lower end swings forward, toward -Z).
- Before rotation it is behind the upper arm: x -10.5 to -6.5 (right) or 6.5 to 10.5 (left), y 3 to 11, z +0.5 to +4.5. After the 30 degree pitch its top is hidden behind the lower half of the upper arm, and its lower end comes out in front of the body: at rest the lower end is about 3.3 units in front of the arm's centerline and at about y = 13, level with the top of the thighs.
- The visible effect is an arm that hangs from the shoulder, with a forearm swung forward like a gorilla's, the forearm 1 unit narrower on each side than the upper arm.
- NOT jointed at an elbow (no pivot there) and NOT hands (the fists are the box ends).

### Thigh
- A block **5 wide, 7 tall, 5 deep**, a child of the leg node, with a **30 degree forward pitch** (the lower end goes forward). Before the pitch: x -5.5 to -0.5 (right) or 0.5 to 5.5 (left), relative to the body centerline, y -1 to +6 (so it starts 1 unit above the hip pivot), z -2.5 to +2.5.
- After the pitch the thigh slopes down and **forward**, with the knee about 3 units in front of the hip line and 5.5 below the hip pivot (at about y = 17.5).
- Each thigh is centered 3 units from the centerline, so the gap between the two legs is 1 unit.

### Shin
- A block **4 wide, 7 tall, 4 deep**, a child of the leg node, pitched **30 degrees forward** (the same as the thigh), but **shifted back 1.5 units behind the thigh's line** before the pitch: y 3 to 10, z +1.5 to +5.5.
- After the pitch the shin's top is behind and below the thigh's lower end, and its lower end comes to rest roughly above the **heel** of the hoof, about 1 to 2 units in front of the hip line.
- Result: a stepped, Z-shaped leg. The thigh's lower end bulges forward as a knee, and the shin starts behind it and plants itself under the hoof. From the side the leg reads as two parallel diagonal slabs offset by about 2 units.
- It is the **pale pink-white metal shin guard** (section 5), the most unlike-the-flesh element after the horns.

### Hoof
- A block **5 wide, 3 tall, 5 deep**, **not rotated** (flat): y 9 to 12 relative to the leg pivot (so y 21 to 24 in the model, soles on the ground), z -4 to +1, x -5.5 to -0.5 (right) or 0.5 to 5.5 (left).
- The toes extend 4 units forward of the hip line, the heel 1 unit behind it.
- Dark gray-brown (#655F5D) with a worn, slightly lighter top.

## 4. Silhouettes

- **Front:** a wide, top-heavy red figure. From the top: two forward-pointing horn tips and two sideways horn arcs framing a squarish skull (8 wide) with a small gap on each side; then the shoulder line: 22 wide, flat, as the upper arms are flush with the chest; the arms hang down on both sides, narrowing slightly where the forearms emerge; the pelvis steps in at y = 7; two legs side by side with a 1-unit gap, a pink-white band at the shins, and two flat gray hooves at the bottom. Negative space: a small notch beside the neck on each side between the head and the arms, and the gap between the legs.
- **Back:** the same, with the back of the skull (7 deep, no snout), the horns' roots visible beyond the head's sides, the arms' backs (the upper arms 5 wide), the legs' backs with the shin tops visible behind the thighs, and the hooves' heels.
- **Side (the main view):** a hunched, forward-leaning silhouette. The skull (7 deep) with the 2-unit snout in front and the horn arcs reaching the same height as the snout; the chest block (6 deep) with the arm in front of and over it; the forearm angled forward in front of the belly; the Z-leg (the thigh forward, the shin behind it); a flat hoof with 4 units of toe. Negative space: the triangle between the forearm and the legs, and the gap between the knee and the hoof's toes.
- **Top:** a wide cross: the shoulders and arms span 22 units; the head sits in the middle with the snout forward; the horns spread as two small hooks at the sides of the head, ending in forward points; the forearms show as two blocks slightly in front of the shoulders.
- **Three-quarter:** the key reading is the **red mass with a pale horn and pale shin**, the arm slab filling most of the near side, the muzzle jutting out, and the leg with its forward-pitched knee. The hooves are barely visible.

## 5. Materials and palette

The original texture is a dark, saturated red "meat" skin with mottled, blotchy noise, plus three accent materials: pale bone-tan horns, pink-white shin plates and gray steel implants with tan-orange cables. About half of its canvas is black unused space, but the model itself has no transparency: every pixel the model uses is opaque.

| Part | Material | Base | Highlight | Shadow / seam |
|---|---|---|---|---|
| Skull | raw red flesh | #5C1006 | #B41F0D (on top and brow) | #400A04 |
| Snout | red flesh with a black mouth | #590F06 | #B51F0C | mouth #000000, teeth white-pink |
| Temple implants | darker red flesh | #540D05 | #891508 | #330703 |
| Horns (both pieces) | bone-tan | #867E68 | #948C76 | #565144 |
| Chest | red muscle, striated | #570E06 | #B41F0D | #400A05 |
| Pelvis | red muscle | #5E0F06 | #AF1E0C | #4E0D05 |
| Upper arms | red muscle | #5A0F06 | #9F1A0B | #490C05 |
| Forearms | red muscle | #5C0F06 | #94190B | #480C05 |
| Thighs | red muscle | #540E06 | #95190A | #440C04 |
| Shins | pale pink-white plating | #9C7D71 | #C1A69C | #73564B |
| Hooves | gray-brown hoof | #655F5D | #696361 | #332B28 |
| Cyber eye plate | gray steel | #484848 | #7D7D7D | #313131 |
| Cyber eye lens | glowing red | #BE0000 | | |
| Cables | tan-orange | #C0875A to #B8703F (approx.) | | |
| Steel implant boxes | gray steel | #6D6D6D | #7B7B7B | #202020 |

Shading style:
- **Flesh:** a blotchy, high-noise red, patches of dark crimson (#400A04 to #5C1006) beside brighter wet-looking red (#B41F0D), with short vertical striations suggesting muscle fibers. It is dark overall (the average color is a very dark red), with highlights on the top faces of the chest, shoulders and thighs. No gradients; no uniform fills.
- **Chest:** pectoral bulges and a ribbed abdomen painted as two lighter lobes over a darker line; a dark crimson oval on the belly or flank.
- **Implants:** gray steel boxes with a dark outline, about 3 x 3 units, with one **tan-orange cable** running out from each one across the flesh (one cable on the head from the eye plate over the temple, one across the chest to a box implant on one side, one on the pelvis ending in a dark round plug, one on the forearm). The cables are thin (about 0.5 unit), slightly curved and darker where they dive into the flesh.
- **Shins:** pink-white metal plates, scuffed and slightly warm (like old brass-rusted steel), with **two crossing tan-orange cable lines and gray bracket marks** on each shin face. They are the lightest large area of the mob.
- **Hooves:** flat gray-brown, a lighter band on the top edge, darker toward the sole.
- **Horns:** bone-tan with faint darker banding; no glow.
- **Emissive:** only the red eye lens is meant to read as glowing; the engine does not add any glow pass (section 6), so it is painted bright red against a dark steel plate.
- **Hidden art:** the skull's two side faces carry black square patches; they are covered by the temple implants and need not be painted.

## 6. In-game presentation

- **Render scale:** 1.0 (nothing is scaled).
- **Shadow:** 0.75 block radius.
- **Held weapon:** always the Cyberdemon Blaster. It is drawn at the right hand with the standard gun transform used for all gun-carrying humanoid mobs (a 0.375 scale, a 20 degree roll and a 55 degree turn), plus an offset of about (-0.11, 0.15, -0.18) blocks to compensate for the bulky arm. Details of the gun transform are in [rendering-and-models.md section 2](../rendering-and-models.md#2-rendergenericnpc-extends-renderbiped).
- **Arms with a gun:** the **aimed-arms pose is turned off** for this mob. The right arm hangs and swings like a walking arm with a slight forward tilt (see section 7), so the gun is held low at the hip, not shouldered.
- **Armor layers:** the standard armor layers are applied by the engine, but this mob never wears armor ("no armor for cyberdemons"). Nothing is drawn over the model.
- **Glow / blend / depth:** none. The model is a plain opaque render; it has no emissive pass, no transparency, no outline.
- **Sounds** (for context): idle, hurt and death sounds exist; a step sound plays at low volume (0.15) on each step. See [sounds.md](../sounds.md).

## 7. Motion

All motion is the **inherited vanilla biped animation**, applied to the six pivots. Children follow their pivot.

- **Walk and limb swing** (phase = limb swing distance x 0.6662, amount = limb swing amount from 0 to 1):
  - **Legs** swing about the X axis through the hip with amplitude **1.4 rad x amount** (about 80 degrees at a full run), the two legs in opposite phase. The right leg and the left arm swing together; the left leg and the right arm swing together.
  - **Arms** swing about X through the shoulder with amplitude **1.0 rad x amount**, in opposite phase to the leg on the same side. This is the plain vanilla arm swing; with the aimed-arms pose off, it is all you see.
  - Because the forearm, thigh and shin are fixed 30 degree offsets **inside** the swinging node, the whole limb swings as one unit about the hip or shoulder; there is no elbow or knee bend during the swing. The Z-shaped leg simply swings stiffly.
- **Held gun:** while it holds the blaster, the right arm's swing is halved and tipped 18 degrees (0.31 rad) forward (the vanilla held-item pose), so the right arm swings less than the left.
- **Idle sway:** both arms drift slowly. The right arm rolls outward by 0.05 rad on a roughly 70-tick cycle and pitches by 0.05 rad on a 94-tick cycle; the left does the mirror. It is subtle, a few degrees.
- **Head look:** the head turns with the look yaw and pitch, with no extra clamp in the model. The engine limits the head's yaw to about 75 degrees from the body. The horns and snout turn with it.
- **Attack:** a melee swing uses the vanilla attack-swing pose (a quick twist of the body around its vertical axis, up to 0.2 rad, with the right arm sweeping). There is no custom attack animation, no separate bite, no charge-up.
- **Aiming pose:** none; see "Arms with a gun" above.
- **Spinning parts:** none.
- **Death:** the vanilla death topple (the mob falls onto its side over about 20 ticks, rotating 90 degrees), and a gib burst of six body pieces in a red-flesh palette, handled by the death-effect system in [04-projectiles-fx](../../04-projectiles-fx/index.md). The cyberdemon is fire-immune.

## 8. What to avoid

- **Do not make it a standard biped.** The skull, torso, arms and legs all differ: the chest is 12 wide, not 8; the arms are 5 wide; the head is 7 deep.
- **Do not give it a hat, ears or hair layer.** There are none.
- **Do not sweep the horns back.** They go out and forward and curve up. A backward-swept horn reads as a different monster.
- **Do not make the legs straight.** The 30 degree thigh and shin, offset from each other, is the identity. Straight legs make it a bulky human.
- **Do not make the forearms vertical.** They angle forward 30 degrees and hang in front of the hips.
- **Do not make the shins red.** They are pale, pink-white metal and are the second-most-noticeable color element.
- **Do not make the horns red or glowing.** They are pale bone-tan.
- **Do not add glow or transparency.** The red lens is just a bright pixel patch.
- **Do not add a tail, wings, spikes or extra eyes.** None exist.
- **Do not add an elbow or knee joint animation.** The limbs swing as stiff units.
- **Do not forget the hooves are flat and unrotated**, 5 x 3 x 5, and wider than the shin.

## 9. Acceptance checklist

- [ ] Height is 32 units (2.0 blocks) from head top to soles; width across the arms is 22 units; render scale 1.0.
- [ ] Chest is 12 wide, 7 tall, 6 deep; pelvis is 9 wide, 5 tall, 5 deep, so the torso tapers downward.
- [ ] Skull is 8 wide, 8 tall, 7 deep, set so its front is 1 unit further back than a standard head; the snout is 6 x 5 x 2 on the lower front.
- [ ] Two temple implants (2 x 4 x 4) sit on the head's sides at x = +-4 to +-6.
- [ ] Each horn has two parts: a base yawed 40 degrees out and forward, and a forward tip pitched 25 degrees up; the pair reads as a bull horn, not swept back.
- [ ] The horns are bone-tan (#867E68-ish), not red.
- [ ] The upper arms are 5 x 7 x 5 and flush against the chest sides; the forearms are 4 x 8 x 4, pitched 30 degrees forward and hidden at the top behind the upper arms.
- [ ] Legs are two stacked pitched boxes (thigh 5 x 7 x 5 and shin 4 x 7 x 4, both 30 degrees forward, shin offset back about 1.5) plus a flat, unrotated 5 x 3 x 5 hoof.
- [ ] Legs are centered 3 units from the centerline with a 1-unit gap.
- [ ] Flesh is dark mottled red (#5C1006 base, #B41F0D highlights); shins are pink-white (#9C7D71); hooves gray-brown (#655F5D).
- [ ] One gray eye plate with a bright red lens on the right (-X), a yellow slit eye on the left, both above the snout.
- [ ] At least three orange cables and two gray steel implant boxes on the flesh; two crossing cable lines on each shin.
- [ ] Walk swing: legs 1.4 rad x amount, arms 1.0 rad x amount, opposite phases, all as stiff units.
- [ ] No hat layer, no glow pass, no transparency, no aimed-arms pose.
- [ ] Nothing floats; every part overlaps or touches its neighbor.

## 10. Unsure

- **Which eye is on which side.** The head box in the original uses a mirrored texture mapping, and I could not verify in a render which way it flips. By reading the layout I put the red cyber eye on the mob's right (-X). If wrong, the builder may swap them; it is a cosmetic detail.
- **The horn direction conflicts with the earlier spec.** The existing spec sheet says the horns sweep "back". By working the rotation through (positive yaw sends the part's +X end toward the front), the base horns end up pointing out and slightly **forward**, and the tips forward and up. I trust the math, but I have not seen it in game; treat "bull horns curling forward" as my reading.
- **Exact cable colors.** I measured the steel and the flesh directly. The orange cable tones are my estimate from a few pixels, so treat the hex as approximate.
- **Teeth detail.** The snout is only 2 units deep and its front face is about 12 x 10 texels at double resolution; I read a black mouth with a white-pink tooth row, but the exact tooth count is not meaningful.
- **Stiff-limb swing.** I assume the engine applies only the vanilla swing to each pivot. I did not see the model animate in game.
