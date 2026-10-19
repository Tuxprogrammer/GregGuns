# Super Mutant: clean-room model notes

Design notes for rebuilding the Techguns **Super Mutant** look-alike in GregGuns. It is one model shared by three tiers (Basic, Heavy, Elite) that differ in render scale, shadow and skin. Written by the "dirty room" analyst from study of the original's model, renderer, entity classes and three textures. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout and paints its own textures.

Related facts: [rendering and models](../rendering-and-models.md#74-modelsupermutant-extends-modelbiped-tabula-export-credited-pwn3d), [renderer](../rendering-and-models.md#3-rendersupermutant-extends-rendergenericnpc), [texture sheet](../textures.md#texturesentitysupermutant_texture_1png-_2png-_3png-128128-each), [per-mob sheet](../per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite).

**Frame used in this document.** The Minecraft entity-model frame. One unit = 1/16 block, **before** the per-tier render scale is applied. **Y points down.** The mob **faces -Z** and its **right side is -X** (its left is +X). The model origin is 24 units above the feet. The vanilla biped pivots apply: head and body at (0, 0, 0), right arm at (-5, 2, 0), left arm at (5, 2, 0), but the legs are **1 unit higher** than vanilla, at (-1.9, 11, 0) and (1.9, 11, 0). The original's textures are 128 x 128 (the model is authored on a 64 x 64 grid, drawn at double resolution), so the builder may use a 64 x 64 skin at 2x or its own layout at any size. All three tiers use the **same geometry**; only the skin, the render scale, the vertical offset and the shadow change.

## 1. Gestalt

A big, mustard-yellow **brute built like a gorilla**: a normal-sized humanoid torso with **huge bulging shoulders**, long heavy arms that hang to the belt, a small head and a dark leather belt and boots. At a glance you read: yellow-orange skin covered in scars, veins and glyph-like marks; arms and shoulder sleeves clearly wider than the body; a dark belt with a silver buckle; big dark boots wider than the legs. The three tiers are the same brute with more gear and more size: Basic is bare-chested; Heavy adds a leather harness and gray metal chest plates; Elite adds a gray steel helmet and steel shoulder pauldrons, and is the biggest.

Defining features in order of visual importance:

1. **Mustard-yellow skin** (#B99421 range) with dark scar lines, over the whole body except the belt, boots, plates and helmet.
2. **Massive shoulders:** 5-wide, 7-tall sleeve blocks on each arm make the upper body 17.8 units wide, more than twice the 8-unit torso.
3. **Dark brown boots and belt:** the boots are wider and deeper than the legs, and the belt hangs a little below the torso.
4. **Per-tier gear:** gray chest plates and a brown harness (Heavy, Elite); a gray helmet and pauldrons (Elite).
5. **A scaled-up body:** Basic is 1.35 times a normal humanoid, Heavy 1.5 times, Elite 1.65 times.

## 2. Overall proportions

Unscaled geometry (all tiers), then the per-tier table.

| Measure | Value (units, scale 1.0) |
|---|---|
| Height, head top to boot soles | 31 units (head top y = -8, soles y = 23). The soles sit 1 unit above the model's ground line. |
| Width, outer edge of one sleeve to the other | 17.8 units (each sleeve reaches 8.9 from the centerline) |
| Width of the torso | 8 units; with the belt 9 units |
| Depth, torso | 4 units; with the chest plates (1 unit each, front and back) 6 units; the belt is 5 deep |
| Head | 8 x 8 x 8, plus a 0.5-unit-inflated hat layer (9 x 9 x 9) |
| Torso height | 12 units (y 0 to 12); the belt drops 1 unit below, to y = 13 |
| Arm length | 12 units (shoulder line y = 0 to hand end y = 12) |
| Leg length | 12 units (y 11 to 23), with the lower 6 units covered by a boot |

Per-tier table. The renderer multiplies every size and pivot position of the model by the scale, around the model origin, then raises the whole model by an extra vertical offset so the boots clear the ground.

| | Basic | Heavy | Elite |
|---|---|---|---|
| **Render scale** | **1.35** | **1.50** | **1.65** |
| **Vertical offset added to the render position** | 0.55 block | 0.75 block | 0.95 block |
| **Shadow radius** | **0.6** block | **0.65** block | **0.7** block |
| Height of the model, head top to soles (blocks) | 2.62 | 2.91 | 3.20 |
| Head top above the entity's feet (blocks) | about 2.73 | about 3.00 | about 3.28 |
| Soles above the entity's feet (blocks) | about 0.11 | about 0.09 | about 0.08 |
| Width across the sleeves (blocks) | 1.50 | 1.67 | 1.84 |
| Head edge length (blocks) | 0.68 | 0.75 | 0.83 |
| Hitbox (reference, set by the entity, not the model) | 0.6 x 2.43 | 0.6 x 2.70 | 0.6 x 2.97 |
| Skin | Basic | Heavy | Elite |

The visual model is about 0.3 block **taller** than its hitbox and about 2.5 to 3 times as wide. The shadow radius is **not scaled** with the model: it is a small disc under a body 1.5 to 1.8 blocks wide.

The offset is the key to the feet: without it the scaled model's soles would sink about 0.4 to 0.6 block into the ground (the scaling is about the model origin, which is 1.5 blocks above the feet). The offset puts the soles about 0.1 block above the ground, which is why the three offsets differ.

## 3. Part by part

Everything is a plain box. Positions are relative to the model origin in unscaled units, x positive toward the mob's left. Z widths are centered unless stated.

### Hierarchy
- **Head node** (pivot at the origin): head, hat layer.
- **Body node** (pivot at the origin): torso, front plate, back plate, belt.
- **Right arm node** (pivot at (-5, 2, 0)): arm, shoulder sleeve. **Left arm node:** the same, mirrored.
- **Right leg node** (pivot at (-1.9, 11, 0)): leg, boot. **Left leg node:** the same, mirrored.
- The whole model is drawn at the tier's scale.

### Head
- A standard cube **8 x 8 x 8**: x -4 to 4, y -8 to 0, z -4 to 4. It turns about the base of the skull at the origin.
- The face is painted on the front (section 5). No separate nose, brow, jaw or tusks are modeled; they are painted.

### Hat layer
- A second cube, the same center, **inflated by 0.5** on every side (9 x 9 x 9), which gives a visible outer shell.
- On the Basic and Heavy skins this layer is **fully transparent**, so it is invisible. On the Elite skin it is a **steel helmet** (section 5). It must exist in all three tiers so the Elite can show it.

### Torso
- **8 wide, 12 tall, 4 deep**: x -4 to 4, y 0 to 12, z -2 to 2.
- A normal biped torso. It is not widened; the width comes from the arm blocks.

### Chest plates (front and back)
- **Front plate:** 6 wide, 7 tall, **1 thick**: x -3 to 3, y 1 to 8, z -3 to -2. It is stuck flat on the front of the torso, 1 unit proud of it.
- **Back plate:** the same size, at z = +2 to +3, flat on the back, 1 unit proud of it.
- They are visible only on the Heavy and Elite skins (gray steel); on Basic the plates' skin is fully transparent so they vanish.
- They do not reach the shoulder or the belt: 1 unit of torso shows above and about 4 units (y 8 to 12) below, partly covered by the belt (the belt starts at y 9).

### Belt
- **9 wide, 4 tall, 5 deep**: x -4.5 to 4.5, y 9 to 13, z -2.5 to 2.5. It wraps the waist, half a unit proud of the torso on every side, and **hangs 1 unit below the torso** (to y 13), so it covers the top of the legs.
- Dark brown leather with a silver buckle at the center of the front (section 5). It is on all three tiers.

### Arm
- **4 wide, 12 tall, 4 deep.** Right: x -8 to -4, y 0 to 12, z -2 to 2. Left: x 4 to 8. It hangs from the shoulder pivot at (±5, 2, 0).
- The arm box sits entirely **outside** the torso: its inner face is at |x| = 4, the torso side face. It is skin colored everywhere; there is no separate hand or fist.
- It swings about the shoulder pivot.

### Shoulder sleeve
- A block **5 wide, 7 tall, 5 deep**, a child of the arm node: right x -8.9 to -3.9, y -0.2 to 6.8, z -2.5 to 2.5; left x 3.9 to 8.9.
- It encloses the top 7 units of the arm and protrudes about 0.9 unit past the arm's outer face and 0.5 unit past its front and back; its inner face reaches 0.1 unit into the torso side. So each shoulder is a fat cube that is 1 unit wider than the arm below it.
- It is **skin colored on Basic and Heavy** and a **steel pauldron on Elite** (the outer faces are gray, a bit of skin shows on the underside).
- Touches: the torso side at its inner edge, the arm along its full length.

### Leg
- **4 wide, 12 tall, 4 deep**: right x -3.9 to 0.1, y 11 to 23, z -2 to 2; left x -0.1 to 3.9. The two legs touch (they overlap by 0.2 at the center).
- The top of the leg is 1 unit above the vanilla position (y = 11), so it overlaps the torso's bottom and the belt.
- Only the **upper 6 units** show skin (y 11 to 17), and the top 2 of those are covered by the belt (to y 13). So about **4 units of bare leg** show between the belt and the boot top.
- The leg swings about the hip pivot at (±1.9, 11, 0).

### Boot
- A block **5 wide, 6 tall, 5 deep**, a child of the leg node: right x -4.5 to 0.5, y 17 to 23, z -2.7 to 2.3; left x -0.5 to 4.5.
- It encloses the leg's lower half and is **wider and deeper** than the leg: 0.6 unit wider on the outside, 0.4 on the inside, 0.7 further forward and 0.3 further back. The two boots overlap by 1 unit at the center. The boot sole is at y = 23.
- Dark brown leather. It has no sole, no toe and no heel; it is a box.

## 4. Silhouettes

- **Front:** a top-heavy shape. A small square head on a neckless torso; **huge square shoulder blocks** on both sides (each 5 wide, flush with the arm's outer face plus 1 more unit); the arms hanging beside the torso, narrowing to 4 below the sleeve; the belt as a dark band that pokes out 0.5 unit each side; two legs with 4 units of bare skin showing, then two wide boot boxes overlapping at the center. Negative space: a small slot beside each side of the neck between the head and the sleeves; no gap between the arms and the torso below the sleeves (the arm is flush against the torso, so only the sleeve's lower edge makes a step).
- **Back:** the same, with the back plate (Heavy and Elite) as a gray square on the back, the spine and ribs painted on the skin of Basic, and the boot backs.
- **Side:** a tall rectangle with a tiny forward bulge at the chest (the plate, 1 unit), a 5-deep belt, a boot sticking 0.7 unit out in front of the leg and 0.3 behind; the arm and sleeve hang in the middle of the side; the head is a cube on top.
- **Top:** a wide rectangle for the shoulders (17.8 wide, 5 deep sleeves) with the 9-wide hat layer in the middle; on the Elite the hat layer is a gray helmet, on the other tiers it shows only the 8-wide head.
- **Three-quarter:** the read is "big yellow ape in boots": the shoulder cubes dominate, the head looks small, and the plates (Heavy, Elite) stand out as gray flat shields on yellow skin.

## 5. Materials and palette

All three skins share the same **skin, boot and belt** art. They differ only in what is painted on the plate, hat layer, sleeve and body (harness) regions. A hex table per part, one column per variant.

| Part | Basic | Heavy | Elite |
|---|---|---|---|
| Head (face) | skin | same as Basic | same as Basic |
| Hat layer | fully transparent | fully transparent | **steel helmet**, #757575 base, #858585 highlight, #4A4A4A shadow |
| Torso | skin, no harness | skin **plus brown harness straps** | same as Heavy |
| Front plate | transparent | gray steel #515151 base, #757575 highlight, #3D3D3D shadow | same as Heavy |
| Back plate | transparent | gray steel #6C6C6C base, #808080 highlight, #3F3F3F shadow | same as Heavy |
| Shoulder sleeves | skin | skin | **steel pauldron** #4E4E4E base, #575757 highlight, #3F3F3F shadow (a little yellow skin left on the underside) |
| Arms | skin | skin | skin |
| Legs | skin | skin | skin |
| Boots | dark leather | same | same |
| Belt | dark leather, steel strips, silver buckle | same | same |

Shared values:

| Material | Base | Highlight | Shadow / detail |
|---|---|---|---|
| Skin | #BE9822 (about #B99421 to #C49D23) | #D1A725 | #95771B for scar lines; #655112 for the deepest marks |
| Face surround (brow, cheeks) | #A36D1D (reddish amber) | | |
| Teeth and tusks | #848475 (gray-cream) | | |
| Eyes | yellow-green | | dark pupil |
| Harness straps (Heavy, Elite) | #5A4202 (dark amber-brown) | | |
| Boots | #463B29 | #4B3F2C | #292218; a black square patch #000000 |
| Belt | #463B29 | #5E5E5E (steel edge strips) | #352C22; buckle near-white gray |
| Hat top (Elite) | | | a darker steel panel |

Shading style:
- **Skin:** mustard yellow-orange with a fine, mottled, high-frequency noise of 3 or 4 levels, over which **darker scar and vein lines** are drawn: short curved scratches, a few small circular or glyph-like marks (rings, hooks, tally strokes), and ribs and a spine on the back. The marks are 1 to 2 units, dark amber, not black. The head top has a large, faint, darker disc. No gradients.
- **Face:** a furrowed brow with a **red-brown tint** around the eyes, two small yellow-green eyes, a flat nose and a wide mouth showing **gray-cream tusk teeth** at the bottom edge. It is a grim, heavy-browed brute.
- **Harness (Heavy, Elite):** on the torso, two brown straps cross the chest in a V or W line from the shoulders, with a darker brown oval patch at the upper chest and a horizontal band across the stomach. The front plate covers most of the straps; they show at the edges and on the back.
- **Plates:** flat gray steel, a thin lighter bevel on the outer edge and a recessed rectangular panel in the middle with **small rivets or a ribbed inset**. The back plate is a lighter gray than the front. Their thin edge faces are mostly transparent, so the plates read as thin slabs.
- **Helmet (Elite):** a flat gray steel dome with panel lines and a **notch cut from the bottom center of the front**, about 3 units wide and 2.5 tall, with a thin vertical bar down the middle of the notch, through which the face shows. The helmet has no horns or crest.
- **Pauldrons (Elite):** dark steel, flat faces, panel seams; the outer, top, front and back faces are steel and only the underside shows skin.
- **Boots:** dark leather with stitch lines and creases, a small black square patch with three short horizontal slits on the outer side (a vent or zipper plate).
- **Belt:** dark leather with a **gray steel strip** along each end and a **silver-gray round buckle** in the center of the front, with two darker diagonal straps flanking it.
- **Emissive:** none. No glowing parts.
- **Transparency:** only the regions the model does not use, the unpainted plate and hat layers on Basic and Heavy, the hat layer's notch on Elite, and the plates' thin edges.

## 6. In-game presentation

- **Render scale:** see the table in section 2 (1.35, 1.50, 1.65). The scale multiplies all sizes **and all pivot positions** of the model, around the model origin.
- **Vertical offset:** added to the entity's render position: 0.55, 0.75, 0.95 block, so the boots clear the ground. It moves everything, the held gun included.
- **Shadow:** 0.6, 0.65, 0.7 block radius (not scaled by the model scale).
- **Skin per tier:** Basic uses skin 1, Heavy skin 2, Elite skin 3.
- **Held weapon:** one gun per mob (a rocket launcher, an assault rifle, a combat shotgun or a laser gun), drawn at the right hand with the standard gun transform for humanoid mobs (see [rendering-and-models.md section 2](../rendering-and-models.md#2-rendergenericnpc-extends-renderbiped)), with an offset of about (-0.10, 0.30, 0) for Basic, (-0.15, 0.30, 0) for Heavy and (-0.20, 0.40, 0) for Elite. **The gun is not scaled up** with the mutant: it is attached through the plain, unscaled arm transform, so it looks like a normal-sized gun held by an oversized brute, and it sits closer to the body than the scaled fist would. The offsets pull it toward the hand. If the builder attaches the gun to the scaled hand, it should still keep the gun at its normal size or it will look comically huge.
- **Armor layers:** the standard armor layers are applied by the engine, but these mobs never wear armor. Nothing is drawn over the model.
- **Pose with a gun:** the aimed-arms pose is on (section 7).
- **Glow / blend / depth:** none. A plain opaque render with transparency only on the hat layer and plates.
- **Sounds** (context): the same rough growl set as the Cyberdemon; see [sounds.md](../sounds.md).

## 7. Motion

All motion is the **inherited vanilla biped animation**. The model's own render scale does not change any angles.

- **Walk and limb swing** (phase = limb swing distance x 0.6662, amount = limb swing amount, 0 to 1):
  - **Legs** swing about X at the hip with amplitude **1.4 rad x amount**, in opposite phase to each other.
  - **Arms** swing about X at the shoulder with amplitude **1.0 rad x amount**, each arm in opposite phase to the leg on its own side (the right arm with the left leg).
  - The sleeve and boot move rigidly with their arm and leg. There is no elbow or knee bend.
  - The walk speed is slower for the bigger tiers (movement speed 0.30, 0.25, 0.20), so the swing amount and cadence are naturally lower for the Elite. No tier-specific animation numbers.
- **Aimed-arms pose** (while it holds a gun, which all three do): both arms raise **horizontally forward** (about -90 degrees about X) and follow the head's pitch, so the gun is shouldered. The right arm turns slightly inward (0.1 rad about Y) and the left arm turns more inward (about 0.5 rad), so the two arms converge on the gun. The aimed pose replaces the walk's arm swing; the legs keep swinging.
- **Idle sway:** a slow drift of both arms: a roll of about 0.05 rad on a roughly 70-tick cycle plus 0.05 rad of pitch on a 94-tick cycle, mirrored left to right. Subtle.
- **Head look:** the head turns with the look yaw and pitch, no model-side clamp; the engine limits head yaw to about 75 degrees from the body. The hat layer moves with it.
- **Attack:** a melee swing uses the vanilla attack-swing pose (a body twist of up to 0.2 rad and an arm sweep). No custom attack animation.
- **Spinning parts:** none.
- **Death:** the vanilla death topple (the body falls over onto its side, 90 degrees) and a gib burst using the generic gore fallback ([04-projectiles-fx](../../04-projectiles-fx/index.md)). Nothing tier-specific.

## 8. What to avoid

- **Do not build three different models.** One geometry, three skins, three scales. The Heavy and Elite plates, helmet and pauldrons are **painted** into the same boxes; the plate and hat boxes exist on Basic too, just transparent.
- **Do not skip the hat layer.** It is needed for the Elite helmet; leave it fully transparent on the other two.
- **Do not widen the torso.** It stays 8 wide; the width comes only from the 5-unit sleeves on 4-unit arms.
- **Do not put the legs at the vanilla height.** They start 1 unit higher (y = 11), and the lower half of each is covered by the boot.
- **Do not forget the vertical offset.** Scaling about the model origin sinks the feet; the offset is what keeps them at about 0.1 block above the ground, and it differs per tier.
- **Do not scale the shadow with the body.** It is 0.6 to 0.7 block regardless of the 1.35 to 1.65 scale.
- **Do not scale the gun with the mutant.**
- **Do not make the skin gray, green or red.** It is mustard yellow; there is no tint difference between tiers.
- **Do not add a glow, a mask, horns, fur or teeth geometry.** None exist.
- **Do not forget the belt and boots on Basic.** They are on all three.
- **Do not paint the plates on Basic.** Leave them transparent.

## 9. Acceptance checklist

- [ ] One model, three tiers: render scales 1.35, 1.50, 1.65; offsets 0.55, 0.75, 0.95; shadows 0.6, 0.65, 0.7.
- [ ] Unscaled height is 31 units (head top y = -8, boot soles y = 23); the soles clear the ground by about 0.1 block at each tier.
- [ ] Width across the sleeves is 17.8 units; the torso is 8 wide and 4 deep.
- [ ] Head is 8 x 8 x 8 with a separate 0.5-inflated hat layer, transparent on Basic and Heavy.
- [ ] Shoulder sleeves are 5 x 7 x 5, flush with the torso's side, on 4 x 12 x 4 arms.
- [ ] Legs start at y = 11; the boots are 5 x 6 x 5, wider and deeper than the legs, and overlap each other at the center by about 1 unit.
- [ ] The belt is 9 x 4 x 5, hanging 1 unit below the torso, on all three tiers, with a silver buckle at the front center.
- [ ] Front and back plates are 6 x 7 x 1, flat on the torso, painted gray steel on Heavy and Elite and fully transparent on Basic.
- [ ] Heavy and Elite have brown harness straps on the torso; Basic does not.
- [ ] Elite has a gray helmet on the hat layer with a notch at the bottom of the front, and gray pauldrons on the sleeves.
- [ ] Skin is mustard yellow (#B99421 to #C49D23) with dark scar and glyph marks; boots and belt are dark brown (#463B29).
- [ ] Walk swing: legs 1.4 rad x amount, arms 1.0 rad x amount, opposite phase; with a gun the arms raise horizontally forward.
- [ ] The held gun is normal size, offset per tier, and does not scale with the mutant.
- [ ] No glow, no extra geometry, no armor layers.
- [ ] Nothing floats; every part overlaps or touches its neighbor.

## 10. Unsure

- **The plates' paint.** The front and back plates are 1 unit thick, and I read a ribbed rectangular panel with rivets from a small, soft texture. The exact panel pattern is approximate.
- **The helmet notch shape.** From the front face of the Elite hat layer I read a notch cut from the bottom center with a thin bar in the middle. It could instead be a visor slot with two small cutouts. Either reads fine.
- **Skin marks.** I describe the scars and glyph marks as seen at double resolution; the exact marks are not meaningful, only their density and darkness.
- **Gun position.** The offsets come from formulas I read, not from a look at the in-game result. Treat the statement that the gun floats near the body, not at the fist, as approximate.
- **Hitbox mismatch.** The hitbox is shorter and narrower than the visible model; I report it for reference, not as something to copy.
- **Feet height.** The 0.1-block clearance comes from the offsets, not from seeing the mob stand on the ground.
