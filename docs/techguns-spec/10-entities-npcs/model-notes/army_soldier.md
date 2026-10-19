# Army Soldier skin: clean-room model notes

Design notes for rebuilding the Techguns **army soldier skin** (`army_soldier.png`, 128x128) in GregGuns. Written by the "dirty room" analyst from study of the original's model class, renderer and texture. They are meant to be complete enough to paint from without seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [texture sheet](../textures.md#texturesentityarmy_soldierpng-128128), [generic NPC model](../rendering-and-models.md#71-modelgenericnpc-extends-modelbiped), [renderer bindings](../rendering-and-models.md#1-renderer-bindings), [Army Soldier](../per-mob.md#6-army-soldier-armysoldier), [Outcast](../per-mob.md#8-outcast-outcast), [Commando](../per-mob.md#9-commando-commando), [Stormtrooper](../per-mob.md#10-stormtrooper-stormtrooper-registry-name-stormtrooper), [Psycho Steve](../per-mob.md#12-psycho-steve-psychosteve-early-game-mini-boss).

**Frame.** The Minecraft entity-model frame: one unit is 1/16 block, **Y points down**, the mob **faces -Z** and its **right side is -X**. The model is the standard biped: head and body pivot at (0,0,0), right arm at (-5,2,0), left arm at (5,2,0), right leg at (-1.9,12,0), left leg at (1.9,12,0); the model origin is 24 units above the feet. This page is skin-only: it records what the painted skin shows, never UV coordinates.

## 1. Gestalt

A **modern infantry grunt in gray-green fatigues**. At a glance you read a tanned, close-cropped man with dark brown hair, a muted olive-gray uniform with two **ammo pouches on the chest** and a **dark webbing belt**, bare tanned forearms and near-black boots. Nothing is bright: the only color accents are the tan skin and a tiny red-white-blue **flag patch** on the sleeve.

Defining features in order of importance:

1. **Desaturated olive-gray fatigues** (#485848 to #586858) covering torso, upper arms and legs, with noisy fabric shading.
2. **Two small chest pouches** and a **dark horizontal belt or webbing band** across the lower torso.
3. **Tan face with a dark brown hair cap**: hair on top, back and the upper rows of the sides, with a ragged fringe over the forehead; the lower sides and back of the head show skin.
4. **Bare tanned forearms** below short-ish gray-green sleeves.
5. **Flag patch**: a tiny dark roundel with red, white and blue squares on the outer face of the sleeve.

Most of the time this skin is mostly or fully hidden by armor (see section 6).

## 2. How the 128x128 texture maps onto the biped

It is a **scaled-up skin, not an overlay sheet.** The model declares a 64x64 logical texture, and the standard biped layout fills only the **top half** of it. At 128x128 each logical texel is a 2x2 pixel block, and the **whole lower half of the file is fully transparent** and unused. The left arm and left leg reuse the right-side art, mirrored, so the file holds one arm and one leg.

The **hat (overlay) layer is empty**: the area that would hold the 0.5-inflated head overlay is fully cut out. Helmets come from armor, not from the skin. (The older sheet in this folder mentions a small rear patch in the hat layer; none was found when measured.)

A builder may use any size from 64x32 up, but should keep the biped proportions and not add content to the lower half.

## 3. Part by part

**Standard biped.** Head 8x8x8, body 8x12x4, arms 4x12x4, legs 4x12x4, hat layer unused. No scale, no extra parts, no extra pivots. Head, body, both arms and both legs are siblings under the model root.

## 4. Silhouettes

Standard biped with no extras. The skin adds no volume: pouches, belt and pockets are painted, not modeled. Armor changes the silhouette, not the skin.

## 5. Materials and palette

Hand-painted, noisy, low-contrast shading at 2x resolution: grainy fabric on the clothes, soft vertical grain on skin, a light edge on pouch flaps. No emissive areas. Every used pixel is opaque; the lower half and the empty hat area are fully cut out with no partial alpha.

| Region | Base | Highlight | Shadow |
|---|---|---|---|
| Hair | #382818 | #483828 | #281808 |
| Face and neck skin | #A87858 | #B88868 | #824C32 (brow and nose shading) |
| Eyes | whites with #2040A0-ish blue irises, dark brown brows | | |
| Fatigue cloth | #485848 | #586858 | #383838 |
| Pouches and pockets | #383838 body, lighter top flap #707868 | | #181818 |
| Belt and webbing | #383838 | #586858 thin stripe | #181818 |
| Forearm skin | #B88878 | #A87868 / #BE8B70 | #754533 |
| Boot area | #181818 | #282828 | #080808 |
| Flag patch | dark roundel #101010 with red, white and blue texels | | |

What each body region shows:

- **Head front:** a tan face with white eyes and blue irises, low dark-brown brows, a small nose and mouth smudge in a slightly darker tan. A hair fringe, ragged and darker, runs across the forehead.
- **Head top:** dark brown hair, mottled. **Head back and sides:** hair across the upper two to three rows, ragged lower edge, then skin below (ears and neck area).
- **Torso front:** gray-green shirt. A rounded **tan neckline** (skin or undershirt) at the top center, about 4 texels wide and 2 rows deep. **Two small pouches** (lighter top flap, dark body) at chest height, one each side of center. Roughly at the lower third, a **dark belt or webbing band** about 2 to 3 rows tall with a thin lighter stripe, running across all four sides of the torso.
- **Torso sides:** a dark vertical oval slot on each side (an armhole or strap opening). **Torso back:** plain gray-green cloth.
- **Arm:** gray-green sleeve down the upper third to a darker cuff line, then **bare tan forearm** with vertical grain. The flag patch sits on the outer face of the sleeve.
- **Leg:** gray-green trousers with darker blotching. A dark patch on the front of the thigh and two flap pockets (lighter tops) near the upper rear and one side. The bottom third is a near-black boot with a slightly lighter bottom edge.

Faction read at a glance: tan skin, olive-gray fatigues and a dark belt, no color accents.

## 6. In-game presentation

- Render scale 1, shadow radius 0.5, no glow or blend tricks, gun offset 0.
- **Mobs using this skin and the armor they wear over it:**

| Mob | Armor over the skin | How much skin shows |
|---|---|---|
| ArmySoldier | Helmet about two thirds of the time (half T2 Combat helmet, half T2 Beret camo 2 green); T2 Combat chest, legs and boots at 50% each | Often: head, forearms and parts of legs |
| Outcast | Full T3 Power Armor, each piece camo 1 (dark) or 2 (dark2) | Almost none |
| Commando | Full T2 Commando (helmet, chest, legs, boots) | Almost none |
| StormTrooper | Full T3 Miner (HEV suit), camo 1 silver | Almost none |
| PsychoSteve | Full T1 Miner, camo 1 red | Almost none |

Only ArmySoldier is the true showcase for the skin. For the other four it is a hidden base layer, so it matters at the gaps (neck, hands, between plates).

## 7. Motion

Standard biped: the legs swing about X with amplitude about 1.4 rad x limb-swing amount, in opposite phase to each other; the arms swing in counter-phase to the legs; the head follows look yaw and pitch; the aimed-gun pose raises the arms when a gun is held; vanilla death topple. Nothing skin-specific moves.

## 8. What to avoid

- Do not paint a hat overlay or use the lower half of the file.
- Do not saturate the greens: the cloth is gray-green, close to neutral.
- Do not paint the flag patch large; it is only about 2 to 3 texels across.
- Do not give the forearms sleeves; they are bare.
- Do not model the pouches or belt as geometry.
- Do not copy Techguns pixels. Paint your own noise and placement.

## 9. Acceptance checklist

1. The file is a 2x skin of a 64x64 logical layout; the lower half is fully transparent.
2. The hat layer area is empty.
3. The head has a hair cap with a fringe and a tan face with blue-iris eyes.
4. The torso shows two chest pouches and a dark belt band wrapping all four sides.
5. A tan neckline sits at the top of the chest.
6. The sleeves are gray-green to about one third down the arm, then bare tan forearms.
7. The flag patch sits on the outer sleeve face and is tiny.
8. The legs are gray-green with pockets, and the bottom third is near-black.
9. Cloth colors stay within roughly #383838 to #586858.
10. Skin tones stay within roughly #754533 to #BE8B70.
11. There is no partial alpha.
12. The skin reads as regular army with no strong color from 10 blocks away.
13. Left limbs mirror the right.

## 10. Unsure

- The side-face oval slots on the torso might be painted armholes or strap shadows; either reading gives the same result in game.
- Which face of the leg holds the pockets, relative to inner and outer side after mirroring, was not verified.
- The blue of the irises is estimated from a few texels.
