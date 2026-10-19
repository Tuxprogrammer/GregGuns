# Skeleton Soldier: clean-room model notes

Design notes for rebuilding the Techguns **Skeleton Soldier** look-alike in GregGuns. Written by the "dirty room" analyst from study of the original's model class, renderer and entity class. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates. The mob wears the **vanilla Minecraft skeleton skin**, so this page describes only how its geometry, pose and held-gun placement differ from a standard biped.

Related facts: [model class](../rendering-and-models.md#72-modelskeletonsoldier-extends-modelgenericnpc), [renderer](../rendering-and-models.md#2-rendergenericnpc-extends-vanilla-renderbiped), [per-mob sheet](../per-mob.md#5-skeleton-soldier-skeletonsoldier).

**Frame used in this document.** The Minecraft entity-model frame. One unit = 1/16 block. **Y points down**. The model origin is 24 units above the feet, so the ground is at y = 24. The mob **faces -Z** and its **right side is -X**. It is a **biped** and uses the vanilla biped pivots: head and body at (0, 0, 0), right arm at (-5, 2, 0), left arm at (5, 2, 0), right leg at (-2, 12, 0) and left leg at (2, 12, 0) (the legs are 0.1 unit wider apart than a standard biped's, which is invisible). The skin is 64 x 32 (not the 64 x 64 of the other NPC skins). No conversion was needed.

## 1. Gestalt

A **vanilla skeleton, standing and holding a pistol-class gun**. At a glance you read: a normal-sized head and torso on **stick-thin arms and legs**, all in bone-gray and off-white with black eye sockets. It is the same silhouette as the vanilla skeleton. The only thing that makes it a "soldier" is what it carries and wears: a gun in the right hand, and sometimes a helmet and boots from the armor layers.

Defining features in order of visual importance:

1. **Thin limbs:** arms and legs are only 2 units square (a regular biped's are 4), so the body looks wide and the limbs look like sticks.
2. **Standard head and torso:** the head is an 8-unit cube and the torso is 8 wide, 12 tall and 4 deep, the same as any biped.
3. **Gun in the right hand,** held out in front in the two-handed aiming pose when it has a target.
4. **No hat layer showing:** the vanilla skeleton skin leaves the outer head layer empty, so nothing extra sits on the head unless armor is worn.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Height, feet to top of head | 32 units (**2.0 blocks**), same as a biped. The entity's hitbox is 0.6 wide x 1.8 tall, so the head pokes 0.2 above it. |
| Head | 8 x 8 x 8 |
| Torso | 8 wide x 12 tall x 4 deep |
| Arm (each) | **2 wide x 12 long x 2 deep** (a biped's is 4 x 12 x 4) |
| Leg (each) | **2 wide x 12 long x 2 deep** (a biped's is 4 x 12 x 4) |
| Width across the arms | 12 units (each arm's outer face is at x = +-6; a biped's is at +-8) |
| Depth | 4 (torso); 8 (head) |
| Every part is inflated by | 0.0625 unit, which makes them 0.125 larger in each dimension. This is only there to stop faces z-fighting where parts overlap; do not reproduce it as a visible feature. |
| Render scale | **1.0** (none) |
| Shadow | radius **0.5** block |

The limbs are the same length as a biped's but half as wide and half as deep, so a skeleton's limb volume is one quarter of a zombie's.

## 3. Part by part

The hierarchy is the vanilla biped's: **head** (with a hat layer as its child), **body**, **right arm**, **left arm**, **right leg**, **left leg**, all siblings under the model root. Nothing is a child of the torso; each part is placed absolutely. Parts overlap only at their joints.

### Head
- A cube **8 x 8 x 8**, x -4 to 4, y -8 to 0, z -4 to 4. Pivot at (0, 0, 0), the base of the neck. Rest angle 0.
- Standard biped head; its face looks toward -Z. Sits directly on the torso with no gap.
- Hat layer: a second 8-unit cube around it, drawn 0.5 unit larger on every side. It exists but is **transparent in the skin**, so it is invisible except when the skin is changed. Our model may omit it.

### Torso
- A box **8 wide x 12 tall x 4 deep**, x -4 to 4, y 0 to 12, z -2 to 2. Pivot (0, 0, 0). Rest angle 0.
- The ribcage and spine are painted on, not modeled: the torso is a plain box.

### Arms (the main difference)
- Each arm is a box **2 x 12 x 2**, running from 2 units above its pivot to 10 units below it (y -2 to 10 around the pivot).
- **Right arm** pivot (-5, 2, 0): the box spans x -6 to -4, z -1 to 1, so its inner face is exactly flush with the torso's side (x = -4). **Left arm** pivot (5, 2, 0), spanning x 4 to 6, a mirror image.
- Rest angle 0 (hanging straight down). The arms are mirrored copies: the left skin is the right skin flipped.
- Touches: the torso side (flush), and the shoulder joint at the top.
- NOT: the 4-unit-wide biped arm. If the arm were 4 wide it would stick out to +-8 and look like a zombie.

### Legs (the other difference)
- Each leg is a box **2 x 12 x 2**, running from its pivot straight down to the ground (y 0 to 12 around the pivot, so it ends at y = 24).
- **Right leg** pivot (-2, 12, 0): the box spans x -3 to -1, z -1 to 1. **Left leg** pivot (2, 12, 0): x 1 to 3. Mirrored.
- The gap between the legs is **2 units** (x -1 to 1). Each leg's outer face is 1 unit inside the torso's edge (x = +-3 versus +-4), so the legs look pulled in under the body.
- Rest angle 0.

## 4. Silhouettes

- **Front:** a head cube on a torso that is 8 wide, then two **thin arms 2 wide pressed against the torso sides** and two thin legs with a 2-unit gap between them. Overall width is 12 units at the arms. There is a visible 1-unit step in at the hips where the legs sit inside the torso edge.
- **Back:** the same as the front.
- **Side:** a torso 4 deep, with a 2-deep arm centered on it (arm z -1 to 1, torso z -2 to 2) and a 2-deep leg centered under it. Negative space: 1 unit of torso shows in front of and behind the arm.
- **Top:** an 8 x 8 head over a 12 x 4 band (torso plus arms).
- **Three-quarter:** a block head and torso, with sticks for limbs. With the gun raised, both thin arms stick forward at shoulder height.

## 5. Materials and palette

The model uses the **vanilla Minecraft skeleton skin** (64 x 32), unmodified, so the builder should paint its own bone-colored skin in the same spirit rather than copy the vanilla one. I did not measure the vanilla skin in this pass, so these values are approximate, taken from how the vanilla skeleton reads.

| Region | Base | Highlight | Shadow |
|---|---|---|---|
| Skull, ribs, limb bones | #C8C8C8 (light bone gray) | #E0E0E0 | #8A8A8A |
| Eye sockets and nose hole | #1A1A1A | n/a | n/a |
| Gaps between ribs and joints | #3A3A3A | n/a | n/a |

- Shading: flat bone pixels with a light/dark two-tone stripe pattern on the ribs and limb bones. No gradients, no glow.
- The face shows two dark eye sockets and a dark jaw line. The torso front shows 4 to 5 rows of rib bars with a dark spine strip down the middle.
- Transparency: only the unpainted hat layer is cut out. Everything else is opaque.
- No variants: one skin.

## 6. In-game presentation

- **Render scale** 1.0 and **shadow radius** 0.5.
- **Held item:** the gun is attached to the end of the **right arm** (it follows the arm's swing), about 7 units down from the shoulder, then moved by the standard gun transform: shifted by a per-mob **gun offset** of **(+0.1, 0, +0.05) blocks**, i.e. about 1.6 units toward the torso (+x in the arm's frame) and 0.8 unit back (+z). This offset exists because the arm is 2 units wide instead of 4: it recenters the gun in the thin hand. Then the gun is nudged (0, 0.125, -0.3125) blocks, turned -100 degrees about the vertical axis, scaled to 0.375, and rotated 20 degrees about Z and 55 degrees about the vertical axis. The result is the usual "pistol in a fist" look held out in front.
- **Armor layers:** it can wear a T1 Combat or T1 Scout **helmet** and **boots** (50/50 each; never a chestplate or leggings). Armor is drawn by the standard biped armor models, which are 4 units wide at the limbs. The boots will therefore look wider than the thin legs; accept that.
- **Weapon:** revolver, Thompson or hand cannon, one third each.
- No glow, blending or depth tricks.

## 7. Motion

The animation is plain vanilla biped; nothing is custom except the thin limbs.

- **Walk:** the legs swing about X. The right leg swings with angle 1.4 rad x walk-amount x cos(phase) and the left leg is exactly opposite (180 degrees out of phase). The arms swing the opposite way to the legs on the same side: the right arm is in phase with the left leg. Arm amplitude is about 1.0 rad x walk-amount. The phase advances at 0.6662 per unit of walked distance.
- **Head look:** the head yaws and pitches to follow the look target. Yaw is limited by the body's turn of about 75 degrees relative to the body; pitch is up to +-90 degrees (vanilla).
- **Idle:** the arms sway very slightly: a slow roll about Z of +-0.05 rad (period about 70 ticks) and a nod about X of +-0.05 rad (period about 94 ticks), opposite signs on the two arms.
- **Aiming pose (when it has a gun and a target):** both arms are raised about X to -90 degrees (pointing straight forward), plus the head's pitch. The right arm turns slightly inward (-0.1 rad about Y plus the head's yaw) and the left arm crosses further (+0.5 rad about Y plus the head's yaw), as in the vanilla bow pose. The idle sway above stays on top of the pose. The gun rides on the right arm.
- **Attack swing (melee, no gun):** the right arm swings forward and down about 1.2 rad in the vanilla melee swing.
- **Death:** vanilla: the whole body tips over backward onto its back over about 20 ticks, with a red hurt flash on damage. No custom animation.

## 8. What to avoid

- **4-unit arms or legs.** This is the single mistake that would make it look like a zombie.
- **Moving the arm pivots.** They stay at x = +-5, with the arm box centered on the pivot. The arm then sits flush against the torso rather than 1 unit away.
- **Dropping the gun offset.** Without the (0.1, 0, 0.05) shift the gun floats off to the outside of the thin hand.
- **Adding a big helmet or a bone-colored hat layer.** The head layer is empty by design.
- **Giving it glowing eyes or a blue tint.** It is not a wither skeleton. It is plain bone.
- **Using the 64 x 64 NPC skin layout.** This model's skin is 64 x 32.

## 9. Acceptance checklist

- [ ] Standard biped hierarchy: head, body, two arms, two legs; no extras.
- [ ] Head cube 8 x 8 x 8; torso 8 x 12 x 4; total height 32 units.
- [ ] Both arms 2 x 12 x 2, pivots at (+-5, 2, 0), flush with the torso sides.
- [ ] Both legs 2 x 12 x 2, pivots at (+-2, 12, 0), with a 2-unit gap between them.
- [ ] Total width across the arms is 12 units.
- [ ] No visible hat layer.
- [ ] Skin is bone-colored with dark eye sockets and visible ribs; no glow.
- [ ] The gun attaches to the right arm and follows its swing.
- [ ] The gun offset (+0.1, 0, +0.05) blocks is applied, so the gun sits centered in the hand.
- [ ] Walk swing: legs swing +-1.4 rad x walk-amount, opposite phases; arms opposite to their same-side leg.
- [ ] Aiming pose raises both arms to -90 degrees about X.
- [ ] Render scale 1.0, shadow radius 0.5.
- [ ] Armor layers (helmet, boots) draw over it without hiding the thin limbs.
- [ ] Skin size 64 x 32.

## 10. Unsure

- **Palette values.** The vanilla skeleton skin was not available to measure in this pass. The hex values above come from the look of the vanilla skeleton, not a pixel measurement.
- **Head yaw limit.** I used the vanilla 75 degrees relative to body; I did not check whether the entity overrides it.
- **Exact held-gun position.** The per-gun transform stacks several offsets and rotations; I described the final feel, not a measured position. Tune the gun on the arm by eye against a zombie soldier holding the same gun.
- **Melee swing.** The model class does not change the vanilla swing; the numbers come from the base biped.
