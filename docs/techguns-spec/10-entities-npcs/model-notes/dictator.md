# Dictator skin: clean-room model notes

Design notes for rebuilding the Techguns **dictator skin** (`dictator.png`, 64x64, used by Dictator Dave) in GregGuns. Written by the "dirty room" analyst from study of the original's model class, renderer and texture. They are meant to be complete enough to paint from without seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [texture sheet](../textures.md#texturesentitydictatorpng-6464), [generic NPC model](../rendering-and-models.md#71-modelgenericnpc-extends-modelbiped), [renderer bindings](../rendering-and-models.md#1-renderer-bindings), [Dictator Dave](../per-mob.md#13-dictator-dave-dictatordave-mini-boss). It is a **variant of the bandit skin**: see [bandit](bandit.md).

**Frame.** The Minecraft entity-model frame: one unit is 1/16 block, **Y points down**, the mob **faces -Z** and its **right side is -X**. The model is the standard biped: head and body pivot at (0,0,0), right arm at (-5,2,0), left arm at (5,2,0), right leg at (-1.9,12,0), left leg at (1.9,12,0); the model origin is 24 units above the feet. This page is skin-only: it records what the painted skin shows, never UV coordinates.

## 1. Gestalt

**The bandit in sunglasses.** Everything outside the head and the bottom of the torso is the same as the bandit skin: black hair, light-gray sleeveless shirt, bare brown arms, dark brown trousers. Dave swaps the bandit's white eyes for a **black sunglasses bar**, adds a **mustache and beard** on the lower face, and gets a **black belt line** across the bottom of the shirt.

Defining features in order of importance:

1. **Black sunglasses**: one wide near-black bar across the face at eye height, with thin arms wrapping to the sides of the head.
2. **Heavy black hair** covering top, back and sides of the head, as on the bandit.
3. **Mustache and beard**: a darker brown band above the mouth and a stubbled lower face, reading as dark facial hair.
4. **Light-gray shirt** (#A8A8A8) with a **black belt line** about 1 to 2 texels tall along its hem.
5. Bare brown arms and dark brown trousers, identical to the bandit.

## 2. How the 64x64 texture maps onto the biped

It is a **1:1 standard biped skin** (no scale-up). The model declares a 64x64 logical texture, and the biped layout fills only the **top half**; the **entire lower half is fully transparent** and unused. This is the old vanilla layout, not the modern 64x64 skin layout, so there are no separate left-limb or overlay areas. The left arm and left leg reuse the right-side art, mirrored.

The **hat (overlay) layer is empty**. The hair is painted directly on the head.

Measured against the bandit skin, only three areas differ: the **head front**, the **head sides** (sunglasses arms) and the **bottom rows of the torso** (the belt line). Arms, legs, head top and back are pixel-identical in content, so a builder can reuse the bandit's art for those.

## 3. Part by part

**Standard biped.** Head 8x8x8, body 8x12x4, arms 4x12x4, legs 4x12x4, hat layer unused. No scale, no extra parts. Head, body, both arms and both legs are siblings under the model root.

## 4. Silhouettes

Standard biped with no extras. The head is a plain cube; the hair, sunglasses and beard are paint only. Armor changes the silhouette.

## 5. Materials and palette

Flat-ish, lightly noised painting at native 64x64, the same style as the bandit. No emissive areas and no partial alpha: every used pixel is opaque, the rest cut out.

| Region | Base | Highlight | Shadow |
|---|---|---|---|
| Hair | #181818 | #282828 | #180808 |
| Sunglasses | #080808 | #181818 (faint lens glint) | #000000 |
| Face skin | #886858 | #A88878 (forehead strip) | #684838 |
| Mustache and beard | #684838 | #886858 (stubble) | #483828 |
| Shirt | #A8A8A8 | #AEAEAE | #8C8C8C |
| Belt line | #080808 | #181818 | #000000 |
| Neck V collar | #684838 | #785848 | #483828 |
| Bare arms | #886858 | #806657 | #6C4B36 |
| Trousers | #483828 | #514031 | #2D241C |
| Boots | #282828 | #262626 | #151515 |

What each body region shows:

- **Head front:** a narrow brown face inside a black hair frame. A pale forehead strip under the fringe. A single wide black bar of sunglasses covers the eyes and extends a texel past each side of the face; beneath it the nose, then a darker mustache band, then a brown stubbled chin.
- **Head sides:** the sunglasses arms appear as a short black line at eye height running back toward the hair. Hair covers the rest; some brown skin near the front low edge.
- **Head top and back:** black hair, as the bandit.
- **Torso:** light-gray shirt with a brown V collar at the top and darker gray side panels, as the bandit. The difference is the hem: the bottom one to two rows are a **solid near-black belt line** on all four sides, in front of the dark brown trousers.
- **Arms and legs:** same as the bandit.

Faction read at a glance: it is "the bandit in sunglasses", and the one skin with a black belt line.

## 6. In-game presentation

- Render scale 1, shadow radius 0.5, no glow or blend tricks, gun offset 0.
- **Mob using this skin:** DictatorDave only (a mini-boss; weapon is a golden revolver).
- **Armor over the skin:** T2 Beret (default camo) on the head, T1 Combat chestplate, T1 Scout leggings (camo 2, snow) and T1 Combat boots. All four slots are always filled, so the skin is visible only at the face below the beret, the neck, the forearms and bits of leg. The sunglasses and beard therefore matter most.

## 7. Motion

Standard biped: the legs swing about X with amplitude about 1.4 rad x limb-swing amount, in opposite phase to each other; the arms swing in counter-phase to the legs; the head follows look yaw and pitch; the aimed-gun pose raises the arms when a gun is held; vanilla death topple. Nothing skin-specific moves.

## 8. What to avoid

- Do not use the modern 64x64 skin layout, a hat overlay or the lower half of the file.
- Do not let the sunglasses become lens-colored (blue, red); they are plain black.
- Do not make the belt line a different color from the sunglasses and hair; it is the same near black.
- Do not forget the bandit-identical arms, legs and shirt: this skin should stay a recognizable sibling.
- Do not model the sunglasses or beard as geometry.
- Do not copy Techguns pixels.

## 9. Acceptance checklist

1. The file is a 64x64 canvas with only the top half painted; the lower half is fully transparent.
2. The hat layer area is empty.
3. Hair is near black on top, back and sides.
4. A black sunglasses bar crosses the face at eye height and wraps onto the head sides.
5. A darker brown mustache and a stubbled chin sit under the nose.
6. The torso is light gray with a brown V collar and darker gray side panels.
7. A near-black belt line, 1 to 2 texels tall, runs along the bottom of the shirt on all four sides.
8. The arms are bare brown with a short gray sleeve at the shoulder.
9. The legs are dark brown with near-black boots.
10. Arms, legs, head top and back match the bandit skin in content.
11. There is no partial alpha.
12. The skin reads as bandit plus sunglasses from 10 blocks away.
13. Left limbs mirror the right.

## 10. Unsure

- The older sheet calls the lower face a "mustache-beard"; at this resolution it reads as a darker band under the nose over brown stubble, so either description fits.
- The exact length of the sunglasses arms on the sides of the head is a guess from a few texels.
