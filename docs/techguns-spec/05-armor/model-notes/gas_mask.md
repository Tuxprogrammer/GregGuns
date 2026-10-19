# Gas Mask: clean-room model notes

Design notes for rebuilding the Techguns **Gas Mask** worn look in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer, item class and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [rendering-and-models § 2.6 face models](../rendering-and-models.md), [special-equipment § 3.1 Gas Mask](../special-equipment.md), [GregGuns hazard protection](../../../gregguns-spec/05-armor/hazard-protection.md), [GregGuns implementation notes](../../../gregguns-spec/05-armor/implementation-notes.md).

**Units and frame.** One unit = 1/16 block = one texel of a 64x32 skin. **Y points down**, the player **faces -Z**, the player's **right side is -X**. Everything is given relative to the vanilla **head** part, origin at the neck (0, 0, 0). The head box runs x -4..4, y -8..0, z -4..4. Rounded to half a unit unless it matters. GregGuns accessory: `gas_mask` (one texture, no camo variants).

**Which model is which.** The Gas Mask item draws the class called ModelGasMask (hood, mouth plate, filter plate, filter canister). A second class, ModelGasMask2 (twin side filters), exists but its renderer registration is commented out: **it is never drawn**. `model-ab/singles/acc_gas_mask.png` is of the real one. (The `acc_oxygen_mask.png` render, by contrast, shows the unused ModelGasMask2; see the oxygen mask notes.)

## 1. Gestalt

A **solid, dark olive-green box over the whole head** with two small eye holes and a **dark gray filter snout** poking out of the lower face. At a glance you read: a featureless green cube (the head is entirely covered, not just the face); two tiny dark-rimmed eye openings; and a short, chunky, slightly downward-tilted snout of dark gray hardware with a small diamond-shaped canister on it. It is a military rubber hood with a respirator, drawn with a box head. The colors are near-black green and charcoal: very dark and low contrast.

Defining features in order of visual importance:

1. **The full head hood.** Every face of the 8x8x8 head is covered, top included.
2. **The snout:** a stack of three pieces on the lower face (a flat mouth plate, a diamond canister, and a tilted square filter plate in front), reaching about 4 units forward of the face.
3. **Two small eye holes** with black rims on the front face. They are the only openings.
4. **Panel look:** each hood face is a flat lighter panel inside a darker half-unit frame, like plates with a seam around every edge.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Hood | 8 x 8 x 8, drawn about 0.06 unit larger on every side, centered on the vanilla head |
| Snout depth beyond the face | 4 (face at z -4, frontmost point at z -8) |
| Snout height | about 3 (y about -2.5 to +0.5) |
| Snout width | 4 at the mouth plate, 3 for the filter plate, about 3 for the diamond canister |
| Snout vertical center | about 1 unit above the chin line (y about -1) |
| Eye holes | each about 1.5 wide x 1.5 tall, with a 0.5 black rim around them |
| Eye spacing | centers about 3 apart, at about y -3.75 |

## 3. Part by part

All parts are children of the head and move with it.

### Hood
- A cube the size of the head: 8 wide, 8 tall, 8 deep, x -4..4, y -8..0, z -4..4, with a hair of extra size (about 0.06) so it covers the player's own skin without fighting it.
- **Opaque on every face**, top and bottom included (the bottom simply ends at the neck line, y 0). It is *not* a strap or a visor; it hides the whole head.
- **Eye holes:** two small transparent openings on the front face (-Z), at about y -4.5 to -3, one each side of the center line (centers about 1.5 left and right of it). Each is 1.5 wide and 1.5 tall with a small 0.5 nub of extra opening above its center, ringed in near-black (0.5 wide). You see the wearer's own face through them. The pair sits a quarter unit toward the wearer's left of true center; the builder may center them.
- **Not:** no brim, no strap, no ears, no hose, no goggles.

### Mouth plate
- A flat slab, **4 wide, 3 tall, 1 thick**, centered on the face and glued to it: x -2..2, y -3..0, z -5..-4. Its bottom edge is flush with the chin line.
- Green, close to the hood tone but slightly darker, with a faint border. It is the visible "face seal" behind the filter.

### Filter canister
- A **2 x 2 x 2 cube turned about 45 degrees about its long axis** so it reads as a **diamond** (visible width about 2.8), also pitched about 20 degrees and yawed about 13 degrees toward the wearer's right.
- Center at about (0, -1.3, -6.1): in front of the mouth plate and behind the filter plate. About 2 units of it stick out beyond the mouth plate.
- Dark gray (charcoal), with faint lighter dots on its front.
- It touches the mouth plate at the back and the filter plate at the front; it must not float.

### Filter plate (the mount)
- A flat square slab, **3 wide, 3 tall, 1 thick**, the frontmost piece, centered at about (0, -1, -7).
- **Tilted about 19 degrees:** its top edge sits further forward (z about -8) than its bottom edge (z about -7), so its front face looks forward and slightly **downward**. The top is at about y -2.2 and the bottom at about y +0.5 (the bottom corner dips half a unit below the chin line).
- Charcoal gray with a lighter rounded patch in the middle of its front face (a stylized filter grille).
- It overlaps the canister, so the snout reads as one chunky stack.

### What it is NOT
No hose to the chest, no straps, no side filters (that is the unused variant), no lenses, no visor, no tinted glass. The eye holes are open holes, not glass.

## 4. Silhouette checks

- **Front:** a flat green square (8 x 8) with two small dark eye holes in the upper middle; below them the gray filter plate (3 x 3, slightly foreshortened by its tilt) centered. The mouth plate is hidden behind it, the diamond canister barely peeks out beside it.
- **Back:** a plain green 8 x 8 square, no openings. If you look from slightly below, the lowest edge of the snout shows under it.
- **Side:** a green square with a **gray wedge sticking out of its lower front**: a short stepped snout about 4 deep with a tilted front face. The plate's bottom corner dips slightly below the hood's lower edge.
- **Top:** a green square with a small dark stack protruding at the front center, about 3 wide.
- **Worn, standing:** the head looks like a green box with a gray snout over the neck. With a helmet on, the helmet covers the hood and only the snout still shows in front of it; see section 6.

## 5. Materials and palette

Rubberized fabric (hood) and dull metal or plastic (snout). Flat shading with very little noise, no gloss, no wear.

| Part | Base | Highlight | Shadow / frame |
|---|---|---|---|
| Hood panels (all faces) | #263124 | #293527 | frame #1B231A |
| Hood front face | #1F271E | #283326 | #191918 |
| Eye hole rim | #191919 | | near-black |
| Mouth plate | #273225 | #2A3628 | #171916 |
| Filter plate | #2B2B2B | #3C3C3C to #3E3E3E (center patch) | #232323 |
| Filter canister | #2A2A2A | #343434 (dots) | #232323 |

Shading style: every hood face is a slightly lighter panel with a **darker 0.5-unit frame** along its edges. The whole look is murky and dim; nothing is bright.

## 6. In-game presentation

- **Original slot and draw order.** The item sits in the Techguns **face slot** (an extra inventory slot, not armor). It is drawn during the player's **helmet armor pass**, as an extra render of the biped **head only**; no other body part is drawn. It is drawn **before** the real helmet armor layer, so a helmet drawn after it overlays the hood.
- **Frame and scale.** The model is a standard biped whose parts are children of the head, so they sit in the head frame above. It is drawn in the player's normal render transform: it gets the same 15/16 player scale as the body and no extra scale of its own. There is no extra offset; the hood is only about 0.06 larger than the vanilla head, so it sits on the skull.
- **Helmet interaction.** Helmets inflate beyond the head (roughly 0.5 to 1 unit per side), so a helmet hides the hood but **not the snout**: the snout stands 4 units in front of the face and pokes through the helmet front. The mask is not drawn at all when the worn helmet is flagged "hide face slot" (the steam, power-armor and HEV helmets are; the exo helmet and every other helmet show the mask). In GregGuns the Gas Mask is itself a helmet-slot item, so it never shares the slot with a helmet; model it as a worn head piece slightly larger than the head.
- **Spent filter.** The original draws nothing once the mask's durability is fully used up. Keep that: no model while spent.
- **No camo.** One texture only.

## 7. Motion

- The whole model follows the **head**: it yaws with the head (relative to the body) and pitches with the player's look pitch. Nothing animates on its own.
- **Sneaking:** the head pivot drops 1 unit (the vanilla sneak pose) and the mask follows.
- **Riding:** the head yaw is clamped relative to the mount, as the vanilla head is.
- Body, arms and legs have no effect on the model.

## 8. What our earlier attempts got wrong

GregGuns has **no worn gas mask model yet**. The mask is a helmet-slot item whose armor texture is a 128 x 64 sheet laid out for the vanilla biped; it does not map correctly, so the mask is not recognizable when worn. There is nothing to compare. For the builder, the main risks are:

- **A face-only visor.** The original hides the **whole head** in a green box.
- **Dropping the tilt.** The filter plate leans about 19 degrees and its bottom corner dips below the chin; a straight plate looks wrong.
- **Making it bright or glossy.** It is near-black green.
- **Adding glass lenses, hoses or straps.** There are none.
- **Using the twin-filter variant.** It is never drawn in the original.

## 9. Acceptance checklist

- [ ] Hood covers the whole head: all faces, top included, within 0.1 per side of the head box (8 x 8 x 8).
- [ ] Exactly two small eye holes (about 1.5 x 1.5 with a black rim) on the front, upper half, about 3 apart.
- [ ] Snout reaches 4 units in front of the face (front point at z about -8) and is about 3 tall, centered about 1 unit above the chin line.
- [ ] Snout is three attached pieces: a 4 x 3 x 1 mouth plate on the face, a diamond-turned 2 x 2 x 2 canister, and a 3 x 3 x 1 filter plate in front.
- [ ] Filter plate is tilted about 19 degrees, top forward, front face looking slightly down; its bottom corner dips about 0.5 below the chin.
- [ ] Canister reads as a diamond and overlaps both the plate and the mouth plate.
- [ ] Hood is dark olive green (about #263124) with a darker frame (about #1B231A) on every face.
- [ ] Snout hardware is charcoal (about #2B2B2B) with a lighter center patch (about #3C3C3C).
- [ ] Eye rims are near-black (about #191919); the holes are open.
- [ ] No hoses, straps, side filters, visor glass or lenses.
- [ ] Front silhouette: green square with two small holes over a gray 3 x 3 filter plate.
- [ ] Side silhouette: green square with a gray stepped wedge 4 deep at the lower front.
- [ ] Everything is attached to the head frame and moves with it; no animation.
- [ ] No model is drawn while the filter is spent.

## 10. Unsure

- **Left-right mirroring.** The original hood is built with a mirror flag, but the vanilla head box is drawn on top of it and wins. The green texture is symmetric apart from the eye holes, so it does not matter.
- **Eye hole nudge.** The holes sit a quarter unit toward the wearer's left; I could not tell if that is deliberate.
- **Helmet sizes.** I did not measure each helmet's inflation, so "the snout pokes through every helmet" holds only for helmets less than about 4 units deeper than the head, which I believe is all of them.
- **Render angle.** In the reference render the window-like shapes appear on the left and back faces, not the front, while the texture puts the eye holes on the front. I trusted the model and texture, not the render labels.
