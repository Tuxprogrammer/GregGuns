# Bandit skin: clean-room model notes

Design notes for rebuilding the Techguns **bandit skin** (`bandit.png`, 64x64) in GregGuns. Written by the "dirty room" analyst from study of the original's model class, renderer and texture. They are meant to be complete enough to paint from without seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [texture sheet](../textures.md#texturesentitybanditpng-6464), [generic NPC model](../rendering-and-models.md#71-modelgenericnpc-extends-modelbiped), [renderer bindings](../rendering-and-models.md#1-renderer-bindings), [Bandit](../per-mob.md#7-bandit-bandit). Its sibling skin is [dictator](dictator.md).

**Frame.** The Minecraft entity-model frame: one unit is 1/16 block, **Y points down**, the mob **faces -Z** and its **right side is -X**. The model is the standard biped: head and body pivot at (0,0,0), right arm at (-5,2,0), left arm at (5,2,0), right leg at (-1.9,12,0), left leg at (1.9,12,0); the model origin is 24 units above the feet. This page is skin-only: it records what the painted skin shows, never UV coordinates.

## 1. Gestalt

A **wild-haired desert raider**. At a glance you read a head swallowed by **thick black hair that frames a small, dark-brown face**, a **pale light-gray sleeveless shirt** with a brown V of neck skin, **bare brown arms** and **dark brown trousers** over black boots. It is a very high-contrast skin: near-black at the top, light gray in the middle, dark brown below.

Defining features in order of importance:

1. **Black, heavy hair** covering the top, back and sides of the head and hanging down past the ears, leaving only a narrow face visible.
2. **Light-gray vest or shirt** (#A8A8A8) on the torso and short gray shoulders on the arms. It is by far the lightest part, so the chest is the brightest area.
3. **Small, hard face**: white eyes with dark pupils under heavy dark brows, a dark mouth and beard area.
4. **Bare brown forearms**, two shades of brown with vertical grain.
5. **Dark brown trousers** with a near-black boot band.

## 2. How the 64x64 texture maps onto the biped

It is a **1:1 standard biped skin** (no scale-up). The model declares a 64x64 logical texture and the biped layout fills only the **top half**; the **entire lower half of the file is fully transparent** and unused. This is the old vanilla layout, not the modern 64x64 skin layout: there are no separate left-limb or jacket and sleeve overlay areas. The left arm and left leg reuse the right-side art, mirrored.

The **hat (overlay) layer is empty**: the area for the head overlay is fully cut out. The heavy hair is painted straight onto the head, not as an overlay.

## 3. Part by part

**Standard biped.** Head 8x8x8, body 8x12x4, arms 4x12x4, legs 4x12x4, hat layer unused. No scale, no extra parts. Head, body, both arms and both legs are siblings under the model root.

## 4. Silhouettes

Standard biped with no extras. The thick hair is only paint, so the head silhouette is a plain cube. Armor changes the silhouette.

## 5. Materials and palette

Flat-ish, lightly noised painting at native 64x64: small blocky shading, vertical-grain streaks on the arms and trousers, soft folds on the shirt. No emissive areas, and no partial alpha: every used pixel is opaque, the rest cut out.

| Region | Base | Highlight | Shadow |
|---|---|---|---|
| Hair | #181818 | #282828 | #180808 |
| Face skin | #886858 | #A88878 (forehead) | #683838 (mouth and brow) |
| Eyes | white, near-black pupil | | |
| Shirt | #A8A8A8 | #AEAEAE | #8C8C8C |
| Neck V collar | #684838 | #785848 | #483828 |
| Bare arms | #886858 | #806657 | #6C4B36 |
| Trousers | #483828 | #514031 | #2D241C |
| Boots and hem | #282828 | #262626 | #151515 |

What each body region shows:

- **Head front:** a narrow brown face, about 6 texels wide, inside a black hair frame. A pale forehead strip sits under the fringe. White eyes with dark inner pupils sit under dark brows; below them a darker brown nose and a dark mouth and beard patch. Cheeks show small brown highlights.
- **Head top, back and sides:** black hair with slightly lighter and darker clumps, covering the whole top and back, and the sides down to the ears; a patch or two of brown skin (ears) shows low on the side faces near the front.
- **Torso front and back:** light-gray shirt with faint vertical folds. A **brown V-shaped collar** (neck skin) at the top center of the front, about 4 texels wide. The side faces are a slightly darker gray (shadow panels). The bottom two rows turn dark brown and black, the trousers and belt line showing under the shirt.
- **Arm:** the upper third is a short **light-gray sleeve**, then **bare brown skin** to the hand, with vertical grain in two browns.
- **Leg:** dark brown trousers with darker vertical wrinkle lines and a few lighter spots; the bottom two rows are near-black boots.

Faction read at a glance: black mop of hair above a bright gray shirt.

## 6. In-game presentation

- Render scale 1, shadow radius 0.5, no glow or blend tricks, gun offset 0.
- **Mob using this skin:** Bandit only.
- **Armor over the skin:** T1 Scout chestplate, leggings and boots always (default camo), and a T1 Scout helmet half the time. So the bright shirt and trousers are almost always covered; the visible parts are the head (half the time bare), the forearms and the neck.
- The Bandit's gib texture is this skin too.

## 7. Motion

Standard biped: the legs swing about X with amplitude about 1.4 rad x limb-swing amount, in opposite phase to each other; the arms swing in counter-phase to the legs; the head follows look yaw and pitch; the aimed-gun pose raises the arms when a gun is held; vanilla death topple. Nothing skin-specific moves.

## 8. What to avoid

- Do not use the modern 64x64 skin layout with overlay layers or separate left limbs; use the old half-sheet.
- Do not add anything in the lower half of the file, or a hat overlay.
- Do not lighten the hair; the top of the head must stay near black.
- Do not tint the shirt: it is a neutral light gray.
- Do not make the face large; the hair covers the sides of it.
- Do not copy Techguns pixels.

## 9. Acceptance checklist

1. The file is a 64x64 canvas with only the top half painted; the lower half is fully transparent.
2. The hat layer area is empty.
3. Hair is near black (#181818 range) on top, back and sides.
4. The face is narrow and dark brown with white eyes and heavy brows.
5. The torso is light gray (#A8A8A8 range) with a brown V collar.
6. The side faces of the torso are slightly darker gray.
7. The sleeves are gray at the shoulder for about one third of the arm, then bare brown.
8. The trousers are dark brown with a near-black bottom band.
9. There is no partial alpha.
10. The brightest region is the torso.
11. The skin reads as black hair plus gray shirt from 10 blocks away.
12. Left limbs mirror the right.
13. The result stays clearly different from the dictator skin by having plain eyes and no black belt.

## 10. Unsure

- The older sheet calls the hair "dreadlocks"; the painting only shows heavy black hair with clumps, so a plain thick mop satisfies it.
- Whether the lower face is a beard or just shadow is ambiguous at this resolution.
- Whether the side-face darker gray is intended shading or a vest edge is not clear.
