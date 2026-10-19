# T2 Riot armor ("Ranger Veteran" coat): clean-room model notes

Design notes for rebuilding the Techguns **Riot gear** worn look in GregGuns as `t2_riot`. Written by the dirty-room analyst from study of the original's model class, armor item class and textures. They are meant to be complete enough to build from blind. They contain no code, no box lists and no texture coordinates; the builder designs its own layout.

**Units and frame.** One unit = 1/16 block = one texel of a 64x32 skin. **Y points down**, the player **faces -Z**, the player's **right side is -X**. Positions are in the world frame of a standing player with the origin at the neck (Y = 0 is the top of the body; the soles are at Y = 24), and as offsets from the vanilla biped part each piece is attached to. Vanilla part boxes: head x -4..4, y -8..0, z -4..4; body x -4..4, y 0..12, z -2..2; arms 4 wide, pivot (+-5, 2); legs 4 wide, 12 tall, pivot (+-1.9, 12). "Inflated by s" means grown by s on every side. Rounded to half a unit unless it matters.

## 1. Gestalt

A long brown **leather duster coat** over a vanilla-sized body, with **big sloped shoulder pads**, a **wide skirt split into two thigh sleeves that move with the legs**, and a **plain olive-khaki helmet**. The look is "ranger veteran": mostly flat brown, bulkier than the player at shoulders and hips, with a pale olive head carrying a row of orange studs.

Defining features in order of visual importance:

1. **Two wide, flat leg sleeves.** Each leg wears a box about 6 wide, 11 tall and 6 deep, pushed 0.6 outward, **attached to the leg so it swings with it**. Together the hips are about 11 wide (vanilla 8).
2. **Torso coat made of two thick side panels and a thin back sheet**, 11 wide and 12 tall overall.
3. **Sloped shoulder pads** on both arms, about 6 wide, 4 tall and 7 deep, tilted 13 degrees so the outer end drops.
4. **Olive helmet**, one unit bigger than the head all around, lighter on top, with orange studs.
5. **Brown leather everywhere else**, warm and dark, with subtle noise.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Player height | 32 (the coat adds no height) |
| Hip width (leg sleeves, outer to outer) | about 11 (vanilla 8) |
| Torso coat width (side panels and back sheet) | about 11 |
| Torso coat length | 13, y -0.5 to 12.5 |
| Leg sleeve | 6 x 11 x 6, y 11.5 to 22.5 (ends about 1.5 above the soles) |
| Shoulder pad | about 6 x 4 x 7, tilted 13 degrees |
| Helmet | 10 x 10 x 10, plus an 11-cube outer layer |

The torso coat ends at the hips and the sleeves start there (overlapping by about 1 unit), so standing still they read as one long brown coat from shoulder to near-ankle. The seam shows when the legs move.

## 3. Part by part

The original has one model per armor slot, each a vanilla biped with a different inflation. The boxes below add to the vanilla part boxes of the same slot (which carry the base skin).

### Helmet slot (inflation 1.0)
- The vanilla head box inflated by 1: **10 x 10 x 10**, x -5..5, y -9..1, z -5..5.
- The vanilla hat layer inflated by 1.5 (an **11-cube**), drawn over it. Studs, band and mask are **painted** on these, not separate geometry.
- No extra boxes.

### Chestplate slot (inflation 0.51, about 0.5)
- **Body:** vanilla body inflated by 0.5: 9 wide, 13 tall, 5 deep.
- **Arms:** vanilla arms inflated by 0.5: 5 x 13 x 5.
- **Side panels** (one per side, mirrored): 2 x 12 x 5 before inflation, about **3 wide, 12 tall, 6 deep** after. Outer face at x = +-5.5, inner face at +-2.5, so each panel overlaps the body's side by 1.5 and sticks out 1.5. Centered front to back (z -3..3), y -0.6 to 12.4. Attached to the **body**.
- **Back sheet:** flat, **11 wide, 12 tall, about 1 thick**, in the plane z about 2.5 (z 2..3), x -5.5..5.5, y -0.6..12.4. Flush with the panels' back edges, against the back. Attached to the **body**. It is a zero-depth sheet inflated by half a unit, not a deep box.
- **Shoulder pads** (one per arm): a box 5 x 3 x 6 inflated by 0.5, about **6 x 4 x 7**. Centered at X = +-6 (one unit outward of the arm pivot), y -1.5..2.5, z -3.5..3.5. **Tilted 13 degrees about the front-back axis** so the outer end is about 0.7 lower than the inner end; its inner end reaches 0.5 into the shoulder side of the body, its outer end hangs about 1.5 past the arm's outer face. Attached to the **arm** at the pivot, so it follows the arm's rotation.
- What they are NOT: not spiked, strapped or round; plain flat slabs.

### Leggings slot (inflation 0.49, about 0.5)
- **Body:** vanilla body inflated by 0.5 (the belt zone), from the leggings skin layer.
- **Legs:** vanilla legs inflated by 0.5.
- **Leg sleeves** (one per leg): a box 5 x 10 x 5 inflated by 0.5, about **6 x 11 x 6**, attached to the **leg** so it rotates about the hip. In the standing world frame the left sleeve spans x -0.5..5.5 and the right x -5.5..0.5 (they overlap by 1 at the midline), y 11.5..22.5, z -3..3. The sleeve is centered 0.6 further outward than its leg, so each leg sits toward the inside of its own sleeve. It starts 0.1 above the hip pivot.

### Boots slot (inflation 0.75)
- Vanilla legs inflated by 0.75 only (5.5 wide). **No sleeves**: they belong to the leggings slot.

### The "floating, offset leg parts": settled
They are **real, intended geometry**: the two leg sleeves above. Evidence: they are children of the leg parts, not of the body, so they hang at the hip and rotate with each leg. In a front render they look offset because (a) they are 6 wide on a 4-wide leg and pushed 0.6 outward, overlapping each other at the midline, and (b) when the render shows the leggings piece alone or the legs in a walking pose, there is no torso coat above to connect them. When worn with the chestplate, the torso coat (to y 12.4) overlaps their top (from y 11.5), so they read as the coat's lower half. They are not floating: the top of each touches the hip.

## 4. Silhouette checks

- **Front (worn, standing):** a trapezoid in brown: shoulder pads sloping down and outward, torso 11 wide, then two sleeves each about 6 wide, touching or overlapping at the center, ending about 1.5 above the soles. Pale olive helmet on top.
- **Back:** a flat brown sheet 11 wide and 12 tall, pads at the sides, sleeves below with a seam at the hips.
- **Side:** a thick boxy coat 6 deep from shoulders to near-ankle, pad sloping 13 degrees, helmet 10 deep.
- **Top:** helmet 10 square in the middle, pads as two 6 x 7 rectangles at the sides at slight tilt, side panels peeking outside the body.
- **Worn look:** a padded, loose, tall outfit with no waist; shoulders and hips both wider than vanilla.

## 5. Materials and palette

| Part | Base | Highlight | Shadow |
|---|---|---|---|
| Coat, sleeves, pads, panels, back sheet (leather) | #54391C | #573B1D | #422D16 to #4B3319 |
| Helmet front (olive khaki) | #64664B | #6E7052 | #606248 |
| Helmet top | #757757 | #7A7C5C | #686A4E |
| Helmet interior and dark trim | #3E3F2E | | #323326 |
| Body skin under the coat (olive-brown) | #483F2B | #4A412C | #3C3423 |
| Studs and belt beads | orange-gold, about #C8861E (approximate) | | |

Shading: flat, low-contrast leather, one or two steps of per-pixel noise (brown family within about 10 levels per channel). The helmet is lighter than the coat on every face and carries a row of orange studs across the brow. No gradients, no wear. There is one color set only (no camo).

## 6. In-game presentation

| Slot | Draws | Skin layer |
|---|---|---|
| Helmet | head (1.0) and hat layer (1.5) | first layer sheet |
| Chestplate | body, arms (0.5), side panels, back sheet, shoulder pads | first layer sheet |
| Leggings | body belt zone, legs (0.5), leg sleeves | second layer sheet |
| Boots | legs (0.75) | first layer sheet |

Real textures are 2x resolution of the model grid. The original skin grid is 64 x 64 (taller than vanilla's 64 x 32). No scale beyond the inflations.

## 7. Motion

- Head and hat follow the head.
- Side panels and back sheet follow the body, including the sneaking lean.
- Pads follow the arm swing about the arm pivot, with the 13 degree tilt added on top.
- Leg sleeves follow leg swing, sneaking and riding, **not the torso**: when walking, they slide in front of and behind the back sheet and the hip seam opens.
- Nothing animates on its own.

## 8. What our earlier attempts got wrong

Our current 18-box model uses a single 11 x 13 x 6 coat box, a collar, arm sleeves with cuffs, a belt, front and back tails, and a 4 x 7 x 4 thigh plus short shin per leg.
- **Left out the real leg sleeves** as "floating". They are 6 x 11 x 6, outward-biased, leg-attached, ending 1.5 above the soles.
- **Solid torso box instead of side panels and a thin back sheet.**
- **No sloped shoulder pads**; ours has plain sleeves and cuffs. Original pads are 6 x 4 x 7 slabs tilted 13 degrees.
- **A collar, cuffs, belt and tails** are not geometry in the original; at most they are painted.
- **Helmet band and mask as separate boxes**; they are painted on the 10-cube head and 11-cube hat layer.
- **Bluish metal color** in our render; the original is brown leather and an olive-khaki helmet.

## 9. Acceptance checklist

- [ ] The leggings piece shows two leg sleeves, each about 6 x 11 x 6, attached to the legs.
- [ ] Hips measure about 11 wide from the front.
- [ ] Sleeves run from about the crotch (y 11.5) to about 1.5 above the soles.
- [ ] Sleeves swing with the legs; the torso coat does not.
- [ ] Torso coat is two side panels (about 3 x 12 x 6) and a thin back sheet (11 x 12).
- [ ] Each shoulder pad is about 6 x 4 x 7, tilted 13 degrees with the outer end lower, attached to the arm.
- [ ] Chestplate draws no sleeves; boots draw no sleeves.
- [ ] Helmet is a 10 cube with an 11-cube outer layer and no extra boxes.
- [ ] Boots are plain 5.5-wide legs.
- [ ] Leather #54391C, helmet front #64664B, helmet top #757757, orange studs.
- [ ] No blue-gray metal anywhere.
- [ ] Walking shows a seam at the hip between coat and sleeves.
- [ ] No part floats: every sleeve top touches the hip, every pad touches the shoulder.

## 10. Unsure

- **Sleeve outward bias (0.6)** comes from numbers, not from a worn view; high confidence.
- **Pad tilt direction** (outer end lower) comes from the rotation convention; fairly sure, matches the render.
- **Orange stud color** is approximate; the studs are tiny in the low-resolution render.
- **Exact painted layout of the helmet** (stripe positions) was read only from a coarse render.
