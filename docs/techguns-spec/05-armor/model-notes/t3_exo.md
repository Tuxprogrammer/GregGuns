# T3 Exo Suit: clean-room model notes

Design notes for rebuilding the Techguns **Exo Suit** worn model in GregGuns as `t3_exo` and its camo variants `green` and `silver`. Written by the "dirty room" analyst from study of the original's model class, its per-slot setup and its three texture pairs. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [armor spec index](../index.md), [GregGuns armor spec](../../../gregguns-spec/05-armor/index.md).

**Units and axes used in this document.** One unit = one texel of a 64x32 skin = 1/16 block. **Y points down**, the player **faces -Z**, the player's **right side is -X**. Every position is given **relative to the rotation point of the vanilla biped part the piece is attached to** (head and body at (0, 0, 0); right arm (-5, 2, 0); left arm (5, 2, 0); right leg (-1.9, 12, 0); left leg (1.9, 12, 0)), as min..max ranges per axis. Vanilla part boxes in that frame: head x -4..4, y -8..0, z -4..4; body x -4..4, y 0..12, z -2..2; arms y -2..10, z -2..2 (right x -3..1, left x -1..3); legs x -2..2, y 0..12, z -2..2. Left and right are exact mirrors across the body's center line unless said otherwise. Rounded to the nearest half unit unless it matters.

## 1. Gestalt

A bulky, white and gunmetal **powered exoskeleton worn over a dark bodysuit**. At a glance you read: a **pale, open-faced helmet cube** with a dark visor band; **huge cube pauldrons** that make the shoulders about 20 units across (the torso is 8); **light cuffs, rods and joint blocks on the arms and legs**, like external actuators bolted to the outside of each limb; a **wide belt plate** with a hanging crotch plate; **wide knee bands**; a **T of bars on the back**; and ankle rings with a shin plate on the boots. The limbs underneath are a near-black undersuit, so the light plates pop against them. The attitude is "military exoskeleton": angular and chunky. There is nothing rounded or organic.

Defining features in order of visual importance:

1. **Oversized shoulder blocks**: 6x5x6 cubes on top of each arm that stick out sideways, so the upper body is far wider than a player.
2. **Light-on-dark contrast**: pale plates over a nearly black undersuit.
3. **External actuators**: a thin vertical rod and a small joint block on the outside of every arm and leg.
4. **A plain open-faced helmet**: a clean cube (not a sculpted helmet) with a dark visor band across the eyes and the lower face left open.
5. **Back bars**: a wide bar and a vertical spine forming a T on the back of the torso.
6. **Belt and knee bands** on the leggings; **ankle ring and shin plate** on the boots.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Width at the elbow-joint blocks (outer face to outer face) | 20, against 8 for the torso |
| Width across the pauldrons only | 19 (each reaches 5.5 beyond the torso's side) |
| Depth of the plates (pauldrons, cuffs, belt, knee bands, ankle rings) | 6, against 4 for the body |
| Helmet | a cube 10 on each side (head 8 plus 1 of inflation all round) |
| Height, helmet top to sole | about 33 (vanilla 32 plus helmet and boot inflation) |
| Pauldron top above the shoulder line | 1.5 |
| Hip width (belt plate) | 10 |
| Back bars | stick out 1.5 behind the torso's back face |

Every addition is a plain axis-aligned box: **no part is rotated**. The proportions come from many small additions on top of the vanilla biped; there is no single big shell.

## 3. Part by part

The suit is the vanilla biped, drawn with an inflation per armor slot, plus box additions parented to each limb. Both are described here. All additions are un-inflated.

### Vanilla layers (the undersuit)

Standard biped boxes, painted dark, inflated by the slot's amount (section 6).

- **Head (helmet slot), inflation 1.0:** a cube 10x10x10 centered on the head (x -5..5, y -9..1, z -5..5). There is **no hat layer**, no crest, no chin plate, no neck ring. The look comes entirely from the paint: pale shell, a dark visor band on the front, and see-through lower areas (section 5).
- **Body (chestplate and leggings slots):** the standard torso, inflated 0.75 for the chestplate and 0.5 for the leggings. Dark suit with a lighter chest plate painted on the front and a lighter top face.
- **Arms (chestplate slot), inflation 0.75:** the standard arms, painted as dark undersuit with a pale top face.
- **Legs (leggings slot inflation 0.5, boots slot inflation 0.75):** the standard legs, dark undersuit with some pale front stripes and a light strip on the underside.

### Pauldron (each arm, chestplate slot)
- A cube **6 wide, 5 tall, 6 deep**, the biggest addition.
- Right arm: x -4.5..1.5, y -3.5..1.5, z -3..3. Left arm: x -1.5..4.5, same y and z. It is centered 0.5 further out than the arm's centerline, **rises 1.5 above the arm's top**, reaches 3 in front and 3 behind (1 more than the arm each way), and its outer face sticks out 1.5 past the arm's outer face. Its inner face overlaps the torso's side by about 0.5.
- NOT a thin shoulder pad and NOT rounded: a full cube whose underside is also visible.

### Upper-arm rod (each arm)
- A vertical rod **2 wide, 8 tall, 2 deep** on the **outer** side of the arm: right x -4.5..-2.5, left x 2.5..4.5; y 1.5..9.5; z -1..1.
- Its outer face is flush with the pauldron's outer face, and its top touches the pauldron's bottom face, so the rod hangs from the pauldron's outer edge. About 0.5 of it is buried in the arm's outer face and 1.5 stands proud. It does not touch the arm's front or back.

### Elbow joint block (each arm)
- **2 wide, 3 tall, 3 deep**, on the outer side at the elbow: right x -5..-3, left x 3..5; y 4..7; z -1.5..1.5.
- Overlaps the rod (it sticks out 0.5 beyond the rod's outer face). It is the **outermost part of the suit**: its outer face is 10 from the body's center line.

### Cuff ring (each arm)
- A flat band **6 wide, 2 tall, 6 deep** around the lower arm: right x -4..2, left x -2..4; y 4.5..6.5; z -3..3.
- Reaches 1 beyond the arm on every side. It covers the middle of the elbow block's inner part, so the block and rod stand out beside it. A bracelet, not a sleeve: only 2 tall.

### Back bar (chestplate slot, body)
- **10 wide, 2 tall, 2 deep** across the upper back: x -5..5, y 1..3, z 1.5..3.5.
- Sticks 1.5 behind the back face. Its ends extend past the torso's sides and are **buried in the rear of the pauldrons** (the pauldrons reach 3 behind), so from the front its ends are hidden and from behind it joins the two shoulder blocks.

### Back spine (chestplate slot, body)
- **3 wide, 9 tall, 2 deep**, centered: x -1.5..1.5, y 3..12, z 1.5..3.5.
- Starts at the bar's bottom edge and runs to the torso's bottom. Bar plus spine form a **T**. Flat back face with a vent or chevron look painted on it.

### Belt plate (leggings slot, body)
- **10 wide, 2 tall, 6 deep**: x -5..5, y 11..13, z -3..3.
- Straddles the torso's bottom edge (body ends at 12), sticks out 1 each side and 1 front and back. One continuous plate, not two hip pads.

### Crotch plate (leggings slot, body)
- **2 wide, 2 tall, 6 deep**, centered: x -1..1, y 13..15, z -3..3.
- Hangs directly under the belt's center like a groin guard, as deep as the belt. It sits between the upper legs and shows in front of and behind them.

### Thigh joint block (each leg, leggings slot)
- **2 wide, 3 tall, 3 deep**: right leg x -4.1..-2.1, left leg x 2.1..4.1; y 4.5..7.5; z -1.5..1.5.
- Sits on the outer side of the leg with a small (0.4) overlap with the inflated leg face; also touches the leg rod and the knee band.

### Leg rod (each leg, leggings slot)
- **2 wide, 10 tall, 2 deep**, outer side: right x -3.6..-1.6, left x 1.6..3.6; y 0.5..10.5; z -1..1.
- Starts near the hip and ends just above the ankle; stands about 1 proud of the inflated leg.

### Knee band (each leg, leggings slot)
- **5 wide, 2 tall, 6 deep**: right leg x -3.1..1.9, left leg x -1.9..3.1; y 5..7; z -3..3.
- Sticks 1 out front and back, about 1 outward on the outer side, and ends right about on the leg's centerline on the inner side. A flat horizontal band like the arm cuff, not a forward-pointing kneecap.

### Shin plate (each leg, boots slot)
- A flat plate on the **front** of the lower leg, **3 wide, 3 tall, 1 thick**: x -1.5..1.5 (centered on the leg), y 9.5..12.5, z -3.5..-2.5.
- Hangs 0.5 below the vanilla leg's bottom and stands 0.5 in front of the ankle ring. Front only; no plate on the back.

### Ankle ring (each leg, boots slot)
- **6 wide, 2 tall, 6 deep** around the bottom of the leg: x centered on the leg, y 10..12, z -3..3.
- 1 beyond the leg on all four sides. No toe, no heel, no lengthened foot: the foot is just the leg's end, with 0.75 of inflation below the ring.

### What is NOT on the suit
No hat layer, no crest, no chin guard, no neck ring, no thigh vents, no forearm vents, no chest box in front of the torso, no big back shell, no lengthened foot, no rotated or angled plates.

## 4. Silhouette checks

- **Front:** a very wide upper body. A 10-wide pale cube (helmet) with a dark band across it; beneath it the pauldron cubes stick out left and right (19 wide), with the rods and elbow blocks hanging from their outer edges and the cuff bands at the lower arm. Between them an 8-wide dark torso with a pale painted chest. At the hips a 10-wide belt plate, then knee bands, outer rods and joint blocks on the legs, and boots with a shin plate and a 6-wide ankle ring. Between plates the body is nearly black.
- **Back:** the helmet's back, the two pauldrons joined by the 10-wide bar, the **T** of bar and spine on the torso, the belt plate, knee bands and ankle rings.
- **Side:** deep and layered. Pauldron 6 deep at the top, bar and spine standing 1.5 behind the back, belt and knee bands 6 deep, shin plate in front of the leg, ankle ring 6 deep. The helmet is a 10-deep cube.
- **Top:** about 20 wide: the two 6x6 pauldron tops, the helmet's 10x10 top in the middle, the torso's top face peeking out around it.
- **Worn look on a standing player:** upright, symmetric, heavy at the shoulders and hips; light plates over dark limbs; a boxy pale helmet with a dark visor and an open chin. The forearms below the cuffs and the hands stay dark undersuit.

## 5. Materials and palette

Two kinds of surface: the **undersuit** (the vanilla layers, painted dark) and the **plates** (the additions, painted gray). The helmet is its own pale shell. All metal is flat with subtle noise; no gradients, no wear. Hexes are sampled from the original sheets.

### Helmet paint (all variants)
- Front face: the upper part is opaque, with a dark visor strip across the eye line (about 3 units tall) split by a narrow center bar into two windows, framed by dark edges. The **lower quarter of the front face is transparent** so the player's face shows. The side and back faces are transparent in their lowest 1.5 units. The bottom face is mostly transparent, with a thin dark rim. The top face is fully opaque and pale.
- Default visor: near black with deep blue (#00127A) and bright blue (#0B43FF) highlights. Green and silver: near black, no bright blue.

### Default `t3_exo` (white and light blue)

| Part | Base | Light | Dark / seam |
|---|---|---|---|
| Helmet shell | #A7B3BE | #EDF5F8 (top up to #FFFFFF) | #2C2C2C (visor) |
| Torso (vanilla layer) | #737F86 | #E0E9EC (painted chest plate) | #161C25, bands #181828 to #282838 |
| Arms (vanilla layer) | #59656E | #C9D9DF (top face) | #0F151E |
| Legs (vanilla layer, boots sheet) | #556169 | #B3CCD3 | #181818 |
| Pauldrons | #C2D1D8 | #EBF0F2 | #8EAFBB |
| Cuff rings | #ABABAB | #E8E8E8 | #777777 |
| Arm rods | #949494 | #B0B0B0 | #777777 |
| Joint blocks (arm and leg) | #848484 | #9D9D9D | #737373 |
| Back bar | #828282 | #AFAFAF | #4A4A4A |
| Back spine | #7F7F7F | #ABABAB | #4C4C4C |
| Shin plates | #898989 | #BABABA | #727272 |
| Ankle rings | #ACACAC | #E8E8E8 | #7B7B7B |
| Leggings: body (vanilla layer) | #1D232B | #2B313A | #14181E |
| Leggings: legs (vanilla layer) | #232A33 | #2E343D | #15191F |
| Belt plate | #B0B0B0 | #E9E9E9 | #7B7B7B |
| Crotch plate | #909090 | #AEAEAE | #747474 |
| Knee bands | #A8A8A8 | #E3E3E3 | #787878 |
| Leg rods | #989898 | #B5B5B5 | #797979 |

Small details (about a tenth of the surface): a few tiny **red L-shaped marks** (#E10000, #FF0000) on the shoulders, chest and thigh; a small **indicator light** at the chest center (near-black housing with red, yellow and green dots); vertical ribbing or chevrons on the rods and spine (dark seams); thin dark panel seams on plates.

### Variant `silver` (monochrome gunmetal)
Neutral gray with **no blue anywhere**; the shell is a pale warm gray. Every part is recolored; the plates are darker than the default's.

| Part | Base | Light | Dark / seam |
|---|---|---|---|
| Helmet shell | #ADABA4 | #E9E9E9 (top up to #FFFFFF) | #2E2E2E |
| Torso (vanilla layer) | #6D6D6C | #D7D7D7 (chest plate) | #131313, bands #181818 to #282828 |
| Arms (vanilla layer) | #545454 | #C2C2C2 | #0E0E0E |
| Legs (vanilla layer, boots sheet) | #515151 | #AFAFAF | #131313 |
| Pauldrons | #A8A8A8 | #DADADA | #707070 |
| Cuff rings | #858585 | #CBCBCB | #484848 |
| Arm rods | #696969 | #888888 | #464646 |
| Joint blocks | #5B5B5B | #787878 | #454545 |
| Back bar | #5C5C5C | #888888 | #393939 |
| Back spine | #5D5D5D | #888888 | #3F3F3F |
| Shin plates | #5D5D5D | #8D8D8D | #454545 |
| Ankle rings | #858585 | #CBCBCB | #4E4E4E |
| Leggings: body (vanilla layer) | #111111 | #1D1D1D | #080808 |
| Leggings: legs (vanilla layer) | #171717 | #1F1F1F | #090909 |
| Belt plate | #8A8A8A | #CBCBCB | #494949 |
| Crotch plate | #656565 | #888888 | #454545 |
| Knee bands | #808080 | #CBCBCB | #484848 |
| Leg rods | #6C6C6C | #898989 | #484848 |

The red marks are nearly absent, so the set reads as gray metal on black.

### Variant `green` (dark olive)
A very dark set. **The helmet and the undersuit layers are olive-black with green bands. All the plates are neutral near-black, not green and not the default's light gray.** Contrast comes from edges and the green bands, not from value.

| Part | Base | Light | Dark / seam |
|---|---|---|---|
| Helmet shell | #1C2018 | #2E3327 (a few pixels to #4E5845) | #0C0D0A |
| Torso (vanilla layer, green bands) | #171C12 | #272B22; bands to #464F3C | #0A0C0A |
| Arms (vanilla layer) | #13180E | #1E2E14; bands to #2E451F | #060804 |
| Legs (vanilla layer, boots sheet) | #10150C | #212A18; green to #273B1B | #060705 |
| Pauldrons | #0F100D | #151712 | #050605 |
| Cuff rings | #181818 | #1F1F1F | #111111 |
| Arm rods | #1B1B1B | #1F1F1F | #181818 |
| Joint blocks | #1D1D1D | #292929 | #191919 |
| Back bar | #1D1D1D | #2A2A2A | #181818 |
| Back spine | #1C1C1C | #282828 | #181818 |
| Shin plates | #1C1C1C | #1F1F1F | #181818 |
| Ankle rings | #181818 | #1F1F1F | #111111 |
| Leggings: body (vanilla layer) | #151F0E | #243417 (green to #2D411E) | #0A0F07 |
| Leggings: legs (vanilla layer) | #151F0E | #233216 (green to #395524) | #0A0F08 |
| Belt plate | #171717 | #1F1F1F | #111111 |
| Crotch plate | #1B1B1B | #1F1F1F | #181818 |
| Knee bands | #181818 | #1F1F1F | #111111 |
| Leg rods | #1A1A1A | #1F1F1F | #181818 |

Where the default has red marks, green has small white number-like marks (#FFFFFF); the chest indicator light stays red and yellow. The visor is black. The brightest olive is about #395524, used only in the undersuit bands.

**The point for the builder:** green and silver are not a recolor of one band. Every row above changes value and hue. In green the plate rows are near-black neutral (about #181818 to #1D1D1D) and must not stay default light gray; the undersuit rows carry the green.

## 6. In-game presentation

- **Helmet slot:** draws only the head cube, inflation 1.0, no hat layer.
- **Chestplate slot:** draws the body (inflation 0.75) and both arms (0.75), with the pauldrons, rods, joint blocks, cuffs, back bar and spine. No legs, no head.
- **Leggings slot:** draws the body at inflation 0.5 and both legs at 0.5, with the belt plate, crotch plate, joint blocks, leg rods and knee bands. No arms.
- **Boots slot:** draws both legs at inflation 0.75, with the shin plates and ankle rings. No body.
- Sheets: helmet, chestplate and boots share one sheet; the leggings use a second one. A single sheet is fine if the leggings' undersuit stays darker than the chestplate's.
- No scale beyond the inflation. The additions are not inflated.
- Chestplate and leggings worn together: the leggings' 0.5 body hides inside the chestplate's 0.75 body, so only the belt and crotch plate show below it.
- The original lets a face-slot item show under the open helmet; in GregGuns the open lower face is where a mask accessory or the player's face shows.

## 7. Motion

- **Everything is rigid and follows its vanilla parent.** Pauldron, rod, joint block and cuff swing with the arm; bar, spine, belt and crotch plate move with the body; knee bands, rods and joint blocks swing with their legs; shin plate and ankle ring swing with their legs; the helmet follows the head.
- **Walking and running:** vanilla swing. The plates are 6 deep on 4-deep limbs, so they sweep a larger arc but never separate.
- **Sneaking:** the body leans; the bar, spine, belt and crotch plate lean with it.
- **Holding a gun:** both arms lift as vanilla, taking pauldrons and cuffs along. The pauldrons will clip into the body and head a little when the arms are raised; accept it.
- **No animation of its own**, no glow layer. The chest light is paint only.

## 8. What our earlier attempts got wrong

The current GregGuns `t3_exo` replaced the vanilla layers with its own big shells and invented parts.

- **Invented helmet parts.** A crest, two light strips over the visor, a chin plate, a neck ring, a back vent and a solid visor box in front of the face. The original is a plain 10-unit cube with the visor painted on and an open lower face.
- **Whole-limb shells instead of dark undersuit plus bolt-ons.** The torso, thighs and shins were rebuilt as pale plates, so the light-on-dark contrast is lost. Keep the limbs dark and add small light parts.
- **Missing parts.** No T of bar and spine (ours has one back block), no arm rods, no cuff rings on the right spot, no crotch plate, no leg rods, no ankle rings.
- **Parts the original does not have.** Thigh vents, forearm vents, a chest box, a lengthened foot and a forward-pointing knee plate.
- **Pauldrons too small and too low.** The original is a full 6x5x6 cube, 1.5 above the arm and 1.5 beyond its outer face. The suit's 20-wide stance is its main identifier.
- **Elbow blocks.** They belong on the outer side, held by the rod, and are the widest point.
- **Green left parts uncolored.** Earlier green recolored the shell and one band, leaving many plates at default light gray. In the original the plates are neutral near-black and the helmet and undersuit carry the olive. Use every row of the green table.
- **Silver too blue or too light.** Silver is gunmetal gray with no blue; do not reuse the default's pale blue plates.
- **Inflation.** Use 1.0 (helmet), 0.75 (chestplate, boots) and 0.5 (leggings) on the vanilla layers; additions are not inflated.

## 9. Acceptance checklist

- [ ] Width at the shoulders and elbow blocks is about 20 against the 8-wide torso (within 10 percent).
- [ ] Helmet is a plain 10x10x10 cube: no crest, chin plate, neck ring or hat layer.
- [ ] Helmet lower face is open below a dark visor band across the front.
- [ ] Each pauldron is a 6x5x6 cube, 1.5 above the arm's top and 1.5 beyond its outer face.
- [ ] Each arm has an outer rod (2x8x2) flush under the pauldron's outer edge, an elbow block (2x3x3) on it, and a 6x2x6 cuff ring.
- [ ] Back has a 10x2x2 bar over a 3x9x2 spine (a T), standing 1.5 behind the torso.
- [ ] Leggings have a 10x2x6 belt straddling the torso's bottom edge and a 2x2x6 crotch plate under it.
- [ ] Each leg has an outer rod (2x10x2), a joint block (2x3x3) and a 5x2x6 knee band.
- [ ] Boots have a 3x3x1 front-only shin plate and a 6x2x6 ankle ring; no lengthened foot.
- [ ] No part is rotated.
- [ ] Undersuit near black; plates light gray (default), mid-gray (silver) or near-black neutral (green).
- [ ] Default palette: shell about #A7B3BE, pauldrons about #C2D1D8, cuffs about #ABABAB, rods about #949494, visor with blue highlights.
- [ ] Silver has no blue anywhere; green plates are about #181818 to #1D1D1D and its undersuit has olive bands up to about #395524.
- [ ] Inflation by slot is 1.0 (helmet), 0.75 (chest, boots), 0.5 (leggings).
- [ ] No part floats: every addition overlaps its limb or a neighbor.

## 10. Unsure

- **Open lower face of the helmet.** Read from transparent areas of the sheet, not from a render: about 2.5 units at the front, 1.5 at the sides and back. A solid cube would be a harmless but visible deviation.
- **Face details of the vanilla layers.** I sampled whole unwraps; which face of the torso, arm or leg carries which painted detail (such as the red marks) is approximate.
- **Silver red marks.** Only a trace on the sheet; I treated them as absent.
- **Green white marks.** I read them as number-like glyphs; exact shape unknown.
- **Leggings alone.** Without a chestplate the leggings' body shows as a dark waist band with the belt plate; I could not check this in game.
- **Paint mirroring.** Geometry is mirror-symmetric; I could not confirm the paint is mirrored left to right.
- **Original pauldron/bar overlap.** The bar's ends being buried in the pauldrons comes from the numbers, not a render, and the render tool skipped these parts.
