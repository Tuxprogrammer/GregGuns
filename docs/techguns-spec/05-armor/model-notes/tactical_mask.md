# Tactical Mask: clean-room model notes

Design notes for rebuilding the Techguns **Tactical Mask** worn look in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer, item class and textures. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [rendering-and-models § 2.6 face models](../rendering-and-models.md), [special-equipment](../special-equipment.md), [GregGuns hazard protection](../../../gregguns-spec/05-armor/hazard-protection.md), [GregGuns implementation notes](../../../gregguns-spec/05-armor/implementation-notes.md).

**Units and frame.** One unit = 1/16 block = one texel of a 64x32 skin. **Y points down**, the player **faces -Z**, the player's **right side is -X**. Everything is given relative to the vanilla **head** part, origin at the neck (0, 0, 0). The head box runs x -4..4, y -8..0, z -4..4. Rounded to half a unit unless it matters. GregGuns accessory: `tactical_mask`, with four recolors (base black, green, white, light green).

**Which model is which.** The Tactical Mask draws the shared **face mask model with its mouth plate switched on**, the same class as the Oxygen Mask but with an opaque, painted texture. The reference render `model-ab/singles/acc_tactical_mask.png` is of this model.

## 1. Gestalt

A **full-head tactical balaclava-helmet**: a matte charcoal box over the whole head, a **dark-red visor strip** painted across the eye line, and a small black **vent plate** over the mouth. It is a faceless, sinister "special-forces" head. Unlike the Gas Mask there are no eye holes and no snout, and unlike the Oxygen Mask the head is entirely covered. At a glance: a black cube with a red slit.

Defining features in order of visual importance:

1. **The full 8 x 8 x 8 head covering** in matte near-black.
2. **The red visor band** across the eyes, 6 wide, with a small notch for the nose.
3. **The mouth plate** (4 x 3 x 1) in black, with a dark vent, over a black mouth guard.
4. Very **little contrast** apart from the red.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Head cover | 8 x 8 x 8, about 0.06 oversize on every side |
| Visor band | 6 wide, about 1.5 tall, at y about -4.5 to -3 |
| Mouth guard (painted) | 7 wide, 1.5 tall, at y about -2.5 to -1 |
| Mouthpiece plate | 4 wide x 3 tall x 1 thick, at x -2..2, y -3..0, z -5..-4 |
| Depth beyond the face | 1 (plate front at z -5) |

## 3. Part by part

### Head cover
- A cube the size of the head (8 x 8 x 8, x -4..4, y -8..0, z -4..4, about 0.06 oversize), **opaque on every face**, top and bottom included (the bottom ends at the neck line). It is a full cowl; hair, face and ears are all hidden.
- **Visor (painted):** on the front face, a dark-red band 6 wide (x -3.5 to 3.5 at its widest) and 1.5 tall, with slightly bevelled ends and a small dark notch at the center of its lower edge for the nose. Visor sits at y -4.5 to -3.
- **Mouth guard (painted):** below the visor on the front face, a black band about 7 wide and 1.5 tall at y -2.5 to -1, with tiny nubs at both ends. It is mostly hidden behind the plate.
- **Sides and back:** plain matte panels, each with a slightly darker 0.5-unit frame.
- **Top:** plain matte panel. **Bottom:** a dark vignette, darker in the middle (rarely seen).
- **Not:** no hood point, no lenses, no goggles, no eye holes, no hose.

### Mouthpiece plate
- A flat slab, **4 wide, 3 tall, 1 thick**, centered on the face and glued to it: x -2..2, y -3..0, z -5..-4. Bottom edge flush with the chin line.
- Black with a faint vent on its front: two very dark vertical slits with slightly lighter stripes beside them, much lower contrast than the Oxygen Mask's grille. Its back face (against the head) is dark charcoal.

### What it is NOT
No filter snout, no tubes, no goggles lens, no strap. The visor is **paint on the cube**, not a separate part: nothing sticks out of the head except the 1-unit plate.

## 4. Silhouette checks

- **Front:** a dark square 8 x 8, a red 6 x 1.5 slit across the upper middle with a tiny nose notch, and a black 4 x 3 plate centered below it at the chin.
- **Back:** a dark square, plain (the matte frame and a faint panel).
- **Side:** a dark square with a 1-unit stub at the lower front (the plate). The red visor does not wrap around the corner.
- **Top:** a plain dark square.
- **Worn, standing:** a black cube head with a red slit and a mouth plate. With a helmet over it, the cube disappears inside the helmet (it is only 0.06 oversize) and only the plate may show.

## 5. Materials and palette

Matte tactical fabric and plastic, flat, almost no noise. Colors per variant (the original has four textures, picked by the item's camo index):

| Variant | Head base | Highlight | Shadow / frame | Visor | Mouth guard / plate |
|---|---|---|---|---|---|
| Base (black) | #1F1F1F | #222222 | #161616 | #6A1A1A (range #541515 to #762222) | #131313 (plate #151515) |
| Green | #111D11 | #132013 | #0D160D | #2D0B0B (dark maroon, range #1F0808 to #352013) | #0B120B |
| Light green | #293A20 | #2D3F23 | #1E2A18 | #5B0000 (range #3D0000 to #6A3D22) | #192314 |
| White | #B1B1B1 | #C0C0C0 | #828282 to #989898 | #181B40 (navy, range #10122A to #2C2C47) | #717171 (plate #7E7E7E) |

Shading style: each head face is a slightly lighter panel inside a **darker 0.5-unit frame**, like the Gas Mask. The visor is a flat saturated band, a touch brighter in the middle and darker at its ends.

## 6. In-game presentation

- **Original slot and draw order.** The item is a **face slot** item, drawn during the player's **helmet armor pass** as an extra render of the biped **head only**, **before** the real helmet armor layer. It is the same draw routine as the Oxygen Mask, with the camo index choosing one of the four textures.
- **Frame and scale.** All parts are children of the head and sit in the head frame given above. Drawn in the player's normal render transform: same 15/16 player scale as the body, no extra scale, no extra offset. The cube is about 0.06 oversize.
- **Helmet interaction.** Any helmet (at least half a unit of inflation) **covers the cube completely**; only the 1-unit plate might peek out through an open-face helmet. The mask is not drawn at all when the worn helmet is flagged "hide face slot" (the steam, power-armor and HEV helmets are; the exo helmet and every other helmet show the mask).
- **GregGuns role.** The Tactical Mask is a helmet-slot item in GregGuns (it also gives night vision and gas protection), so it is the **only head piece** worn; build it as the helmet itself, a head cover slightly larger than the head, not as an extra layer under another helmet.
- **Camo.** Four recolors, same shape.

## 7. Motion

- Follows the **head** only (yaw relative to body, look pitch). Nothing animates, including while night vision is on.
- **Sneaking:** the head pivot drops 1 unit and the mask follows.
- **Riding:** the head yaw is clamped relative to the mount.
- Body, arms and legs have no effect.

## 8. What our earlier attempts got wrong

GregGuns has **no worn Tactical Mask model yet** (the mask is a helmet-slot item with a 128 x 64 vanilla-biped armor sheet that does not map correctly, so it is not recognizable when worn). For the builder:

- **Do not model a visor as a protruding part.** It is paint on a flat cube.
- **Keep the head fully covered.** No open face, no eye holes.
- **Keep the palette dark.** Only the visor is colored; do not make the whole mask gray or green by default.
- **Include the 4 x 3 x 1 mouth plate.** It is the only thing that breaks the cube.

## 9. Acceptance checklist

- [ ] Head cover is a full 8 x 8 x 8 cube, opaque on all faces, within 0.1 per side of the head box.
- [ ] Red visor band, about 6 wide and 1.5 tall at y -4.5 to -3, with a small nose notch; painted flat, not raised.
- [ ] Black mouth guard band below the visor, mostly hidden behind the plate.
- [ ] Mouthpiece plate 4 x 3 x 1 at x -2..2, y -3..0, z -5..-4, attached to the face.
- [ ] Nothing else protrudes from the head.
- [ ] Base palette: head about #1F1F1F, frame #161616, visor about #6A1A1A, plate about #151515.
- [ ] Each face is a lighter panel in a darker 0.5-unit frame.
- [ ] Green, light green and white recolors exist with the palette lines in section 5.
- [ ] Front silhouette: dark square, red slit, black plate; side silhouette: dark square with a 1-unit stub at the chin.
- [ ] Attached to the head frame, follows head yaw and pitch, no animation.
- [ ] Not drawn when the worn helmet is flagged "hide face slot" (if it is an extra slot item).

## 10. Unsure

- **Render labels.** In the reference render the red visor shows on the left and back faces rather than the front, while the texture puts it on the front. I trusted the model and texture.
- **Visor size.** The band's size and position come from reading a very low-resolution texture; it could be off by half a unit.
- **Mouth guard ends.** The little nubs at either end of the guard may be a texture accident rather than design; they can be dropped.
- **Variant visor colors.** Green and light green visors read as dark maroon and dark red with a faint brown fringe; the exact fringe color is approximate.
- **Mirroring.** The cube is built mirrored but the vanilla head box on top wins; the faces are symmetric apart from the nose notch.
