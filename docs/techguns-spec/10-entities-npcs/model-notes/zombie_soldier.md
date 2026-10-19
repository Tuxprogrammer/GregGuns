# Zombie Soldier skin: clean-room model notes

Design notes for rebuilding the Techguns **zombie soldier skin** (`zombie_soldier.png`, 128x128) in GregGuns. Written by the "dirty room" analyst from study of the original's model class, renderer and texture. They are meant to be complete enough to paint from without seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [texture sheet](../textures.md#texturesentityzombie_soldierpng-128128), [generic NPC model](../rendering-and-models.md#71-modelgenericnpc-extends-modelbiped), [renderer bindings](../rendering-and-models.md#1-renderer-bindings), [Zombie Soldier](../per-mob.md#1-zombie-soldier-zombiesoldier), [Zombie Farmer](../per-mob.md#2-zombie-farmer-zombiefarmer), [Zombie Miner](../per-mob.md#3-zombie-miner-zombieminer).

**Frame.** The Minecraft entity-model frame: one unit is 1/16 block, **Y points down**, the mob **faces -Z** and its **right side is -X**. The model is the standard biped: head and body pivot at (0,0,0), right arm at (-5,2,0), left arm at (5,2,0), right leg at (-1.9,12,0), left leg at (1.9,12,0); the model origin is 24 units above the feet. This page is skin-only: it records what the painted skin shows, never UV coordinates.

## 1. Gestalt

A **rotting green soldier**. At a glance you read a mottled green head with two **bright-red eyes**, a torso and legs of dull gray-brown fatigue cloth streaked and pooled with dark blood, and a **ragged short sleeve ending in bare green forearms**. The look is "dead man in army trousers": green skin is the only saturated color besides the red.

Defining features in order of importance:

1. **Two small bright-red eyes** on a slightly lighter green face. They are the only pure bright red on the head and are the faction tell at range.
2. **Mottled zombie-green skin** on head, neck and forearms (two or three close greens with noise, never flat).
3. **Blood**: dark red smears and splotches, heaviest on the chest (one large wound just off center), on the head sides and back, and on the forearms.
4. **Torn, drab gray-brown clothing** (shirt and trousers) with a darker band at the shirt hem and near-black boot cuffs.
5. **A ragged, jagged sleeve edge** on the upper arm, where cloth gives way to green skin.

## 2. How the 128x128 texture maps onto the biped

It is a **scaled-up skin, not an overlay sheet.** The model declares a 64x64 logical texture and the standard biped layout fills only the **top half** of it, so at 128x128 the skin is drawn at **2x resolution** (each logical texel is a 2x2 block of pixels). The whole **lower half of the file is fully transparent** and unused. The left arm and left leg reuse the right-side art, mirrored, so the file holds one arm and one leg.

The **hat (overlay) layer is empty**: the area that would hold the 0.5-inflated head overlay is fully cut out. Nothing is painted over the head; any helmet comes from armor.

A builder may use any size from 64x32 up. 128 wide gives finer grain, but keep the biped proportions, keep the 2x logic, and do not add content to the lower half.

## 3. Part by part

**Standard biped.** Head 8x8x8, body 8x12x4, arms 4x12x4, legs 4x12x4, hat layer unused. No scale, no extra parts, no extra pivots. Head, body, both arms and both legs are siblings under the model root.

## 4. Silhouettes

Standard biped, no extras. The negative space is exactly that of a vanilla zombie: gaps between the legs and between the arms and torso. Armor pieces, not the skin, change the silhouette.

## 5. Materials and palette

Hand-painted, noisy, low-contrast shading at 2x resolution. There are no panel lines, no emissive pass and no transparency inside the used area: every used pixel is opaque, and the unused lower half and the empty hat area are fully cut out with no partial alpha.

| Region | Base | Highlight | Shadow |
|---|---|---|---|
| Head, neck and forearm skin | #488838 | #78A858 (the pale face patch) | #386828 |
| Forearm skin (lower arm) | #689848 | #78A858 | #488838 |
| Eyes | #E50000 to #F00100 (bright red), about 2 texels wide each | | #B80200 edge |
| Blood | #980700 | #C02020 | #581810 (dried) |
| Trousers | #585848 | #686050 | #484838 |
| Shirt | #483828 | #585848 | #382818 |
| Sleeve (upper arm) | #382828 | #582818 | #281818 |
| Boot cuffs and shirt hem | #282828 | #383838 | #181818 |

What each body region shows:

- **Head front:** a lighter green face patch, two red eyes set about 2 texels apart at mid height of the face, and a thin dark mouth line below them.
- **Head sides and back:** darker green, with a large blood patch on one side face and another on the back, and a small dark scratch or scar on the other side face. **Top:** plain mottled green. **Underside:** green with a dark square patch (hidden).
- **Torso front:** gray-brown cloth with a horizontal blood band at chest height and one big dark-red wound just off center toward the mob's left, plus thin vertical streaks. A pale green neck patch shows at the very top. A slightly darker band runs along the hem.
- **Torso back:** the same cloth with red streaks and a darker lower band.
- **Arm:** the upper third is a dark-brown sleeve with a **jagged torn lower edge** and a red patch near the shoulder. The remaining two thirds is green bare skin spattered with blood, with a darker blood pool toward the hand.
- **Leg:** gray-brown trousers with fine vertical wear and one small blood stain on the front of the thigh. The bottom third is near-black dark gray, reading as boots.

Faction read at a glance: green skin, red eyes, blood.

## 6. In-game presentation

- Render scale 1, shadow radius 0.5, no glow or blend tricks. The red eyes are painted, not emissive.
- Gun offset 0 (standard); held items use the generic NPC transform.
- **Mobs using this skin and the armor they wear over it:**

| Mob | Armor over the skin |
|---|---|
| ZombieSoldier | T1 Combat helmet, chest, legs and boots, each independently at 50% |
| ZombieFarmer | T1 Miner, one random camo (0 to 3) shared by all pieces: chest always, legs 50%, boots 50%, no helmet |
| ZombieMiner | T1 Miner, one shared random camo: helmet always, chest, legs and boots 50% each |

Armor is partial, so bare green skin is often visible on the head (Soldier, Farmer) and limbs. Make sure each region reads on its own.

## 7. Motion

Standard biped: the legs swing about X with amplitude about 1.4 rad x limb-swing amount, in opposite phase to each other; the arms swing in counter-phase to the legs; the head follows look yaw and pitch; the aimed-gun pose raises the arms when a gun is held; vanilla death topple. Nothing skin-specific moves.

## 8. What to avoid

- Do not paint a hat overlay or use the lower half of the file.
- Do not make the skin one flat green: it must be mottled.
- Do not make the eyes emissive or add a glow pass; the original does not.
- Do not put the chest wound dead center; it sits off center.
- Do not give the arms full sleeves; the forearms are bare green.
- Do not copy Techguns pixels. Paint your own noise, blood placement and wound.

## 9. Acceptance checklist

1. The file is a 2x skin of a 64x64 logical layout; the lower half is fully transparent.
2. The hat layer area is empty.
3. The face is a light mottled green with exactly two red eyes and a dark mouth line.
4. The eyes are the only bright red on the head.
5. The torso is gray-brown cloth with a large blood wound near the chest and a darker hem band.
6. The upper arm has a ragged dark sleeve; the forearm is green skin with blood.
7. The legs are gray-brown with the bottom third near-black.
8. Blood covers about 10 percent of the visible skin, heaviest on chest, head sides and forearms.
9. Greens stay within roughly #386828 to #78A858.
10. There is no partial alpha anywhere.
11. The skin reads as undead from 10 blocks away through green plus red.
12. The same skin works under all three armor sets in the table.
13. Left limbs mirror the right.

## 10. Unsure

- The older texture sheet in this folder says only that the head carries blood; the skin actually shows blood on one side face and the back, and a small scar on the other side face. Which side is which after mirroring was not verified.
- Whether the pale face patch is meant as a jaw or bone highlight is a guess from the painting.
