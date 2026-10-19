# Oxygen Mask: clean-room model notes

Design notes for rebuilding the Techguns **Oxygen Mask** worn look in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer, item registration and texture. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates; the builder designs its own UV layout.

Related facts: [rendering-and-models § 2.6 face models](../rendering-and-models.md), [GregGuns oxygen gear](../../../gregguns-spec/05-armor/oxygen-gear.md), [GregGuns Baubles slots](../../../gregguns-spec/05-armor/baubles-slots.md).

**Units and frame.** One unit = 1/16 block = one texel of a 64x32 skin. **Y points down**, the player **faces -Z**, the player's **right side is -X**. Everything is given relative to the vanilla **head** part, origin at the neck (0, 0, 0). The head box runs x -4..4, y -8..0, z -4..4. Rounded to half a unit unless it matters. GregGuns accessory: `oxygen_mask` (one texture, no camo variants).

**Which model is which, and a warning about the render.** The Oxygen Mask is drawn by the shared **face mask model with its mouth plate switched on** (the same class the Tactical Mask uses), with its own gray texture. It is **not** ModelGasMask2. The reference render `model-ab/singles/acc_oxygen_mask.png` is labeled "ModelGasMask2", which is an unused model; its thin square frame and side filters **do not exist in game**. Ignore that render. Everything below comes from the model code and the real texture.

## 1. Gestalt

A **light-gray strap-and-plate breathing mask**: a thin metal-gray band wrapped around the lower half of the head at mouth height, a lens-shaped gray patch over the mouth, and a small **vented mouthpiece plate** in front of it. The rest of the head (top, upper face, eyes, hair) is **bare**. It is nothing like the gas mask: no hood, no snout, no filters. At a glance: a gray belt around the chin and cheeks with a little grilled plate on the mouth.

Defining features in order of visual importance:

1. **The mouthpiece plate:** 4 wide, 3 tall, 1 thick, with a two-slit vent, sitting flat on the mouth.
2. **The patch behind it:** a 6-wide lens-shaped gray cover over the mouth with a small peak toward the nose.
3. **The strap:** a thin (1.5-unit) gray band running around the lower head, over the cheeks and ears to the back.
4. **Bare head above and around it.** The head cube is mostly transparent.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Strap ring | wraps the 8 x 8 head cross-section, drawn about 0.06 outside the skull, 1.5 tall, from y about -2.5 to -1 |
| Front patch | 6 wide, about 2.5 tall (y about -3 to -0.5), with a peak 2 wide rising to about y -4.5 at the middle |
| Mouthpiece plate | 4 wide x 3 tall x 1 thick, at x -2..2, y -3..0, z -5..-4 |
| Depth beyond the face | 1 (plate front at z -5) plus 0.06 for the strap and patch |

## 3. Part by part

### Strap and patch (the transparent head cube)
- A head-sized cube (8 x 8 x 8, about 0.06 oversize) exists, but it is **transparent almost everywhere**. Only a thin band is painted on it: that painted band is the whole strap.
- **Strap:** a 1.5-unit-tall band on all four side faces, at y -2.5 to -1 (about 5.5 to 7 units below the head top). It meets itself at the back; there is no buckle or tail, just a gray band.
- **Front patch:** on the front face the band widens into a **lens or oval shape** 6 wide (x -3 to 3) and about 2.5 tall (y about -3 to -0.5), with a small peak (2 wide) rising to about y -4.5 at the middle, a nose-bridge hint. A brighter highlight lies in the middle of the patch.
- Because it is painted on a cube, it has no thickness of its own: it hugs the skull at about 0.06 outside it.
- **Not:** no top strap, no band over the head, no side hoses, no tank.

### Mouthpiece plate
- A flat slab, **4 wide, 3 tall, 1 thick**, centered on the face and attached to it: x -2..2, y -3..0, z -5..-4. Bottom edge flush with the chin line.
- The **front face** is mid-gray with a vent grille: two light vertical slits, each 0.5 wide and about 3 tall, flanked by dark stripes. The slits sit at about 1 and 2.5 units in from the plate's left edge (viewed from the front), so they are symmetric around the center line (about 0.75 either side).
- The **back face** (the side against the head) is a lighter gray (about #9E9E9E) than the rest; it is rarely seen.
- It is attached over the patch, so the patch frames it on all sides.

### What it is NOT
No tubes, no cylinder, no hood, no head-covering cube, no goggles, no tank, no lenses. Nothing above the nose. Nothing at the back of the head besides the band.

## 4. Silhouette checks

- **Front:** a gray lens shape (6 wide, 2.5 tall, with a small peak) at mouth level, with a 4 x 3 gray grilled plate on top of it. The upper face and eyes are bare.
- **Back:** only the thin gray band across the lower head (1.5 tall) with nothing else.
- **Side:** a thin gray band across the cheek and a small plate sticking 1 unit forward of the mouth. The band slopes gently with the head.
- **Top:** nothing visible, except the plate's tiny edge in front of the face.
- **Worn, standing:** a small gray mouth-and-chin piece with a belt around the lower head. A helmet worn on top covers none of it unless it has a full-face skirt; the band at y -2.5 to -1 sits right where a helmet's cheek guards or a closed visor would be.

## 5. Materials and palette

Brushed light-gray plastic or metal, flat shading with slight noise. It is a light neutral gray, far brighter than the gas mask.

| Part | Base | Highlight | Shadow / frame |
|---|---|---|---|
| Strap (sides and back) | #787878 | #888888 | #6B6B6B |
| Front patch | #888888 | #A2A2A2 to #ACACAC (center highlight) | #5D5D5D (edge) |
| Mouthpiece front | #727272 | #A2A2A2 (the two slits) | #494949 (the dark stripes) |
| Mouthpiece back | #9E9E9E | | |

## 6. In-game presentation

- **Original slot and draw order.** The item is a "shared item" in the **face slot** (an extra inventory slot, not armor). It is drawn during the player's **helmet armor pass** as an extra render of the biped **head only**, **before** the real helmet armor layer. It shares the draw routine (and the hide rule) with the Tactical Mask.
- **Frame and scale.** All parts are children of the head and sit in the head frame given above. Drawn in the player's normal render transform: same 15/16 player scale as the body, no extra scale, no extra offset. The strap cube is about 0.06 oversize.
- **Helmet interaction.** The strap and patch are painted at only about 0.06 outside the skull, so any helmet (at least half a unit of inflation) **covers them**. The 1-unit mouthpiece plate also sits inside most helmets, so with a helmet on you usually see little or nothing of the mask; with an open-face or low-cheek helmet it shows. The mask is not drawn at all if the worn helmet is flagged "hide face slot" (the steam, power-armor and HEV helmets are; the exo helmet and every other helmet show the mask).
- **GregGuns role.** In GregGuns the Oxygen Mask is a Galacticraft mask-slot item (or a Baubles fallback), so it can coexist with a helmet. A builder-made GregGuns model should be a worn head piece with the same footprint, drawn so that it is visible with an open-face helmet and hidden by closed ones.
- **No camo.** One texture only.

## 7. Motion

- Follows the **head** only (yaw relative to body, look pitch). Nothing animates on its own.
- **Sneaking:** the head pivot drops 1 unit and the mask follows.
- **Riding:** the head yaw is clamped relative to the mount.
- Body, arms and legs have no effect.

## 8. What our earlier attempts got wrong

GregGuns has **no worn Oxygen Mask model yet**; it is invisible when worn, and the texture sheet is a vanilla-biped layout that does not map onto it. There is nothing to compare. For the builder:

- **Do not build the model from the reference render.** That render is of an unused different model (a flat square frame with side filters).
- **Do not make a hood.** The head is bare above the strap.
- **Do not add tanks or hoses.** None exist on the mask.
- **Keep it thin.** The strap is only 1.5 tall and almost no thickness; the only real volume is the 4 x 3 x 1 plate.

## 9. Acceptance checklist

- [ ] The only solid volume is the 4 x 3 x 1 mouthpiece plate at x -2..2, y -3..0, z -5..-4 (plus a skin of about 0.06 for strap and patch).
- [ ] A 1.5-tall gray strap wraps the lower head at y -2.5 to -1 on all four sides, including the back.
- [ ] A front patch about 6 wide and 2.5 tall (with a small peak) frames the plate.
- [ ] Plate front has two thin light vertical vent slits with dark stripes beside them.
- [ ] Nothing at all above y -4.5 (upper face, eyes, forehead, top of the head all bare).
- [ ] Colors are neutral gray: strap about #787878, patch about #888888 with an #A2A2A2 highlight, vent slits about #A2A2A2 on #494949.
- [ ] No hood, no hose, no tank, no lenses.
- [ ] Front silhouette: gray oval with a small peak and a grilled square on it.
- [ ] Side silhouette: a thin band with a 1-unit plate sticking forward at mouth height.
- [ ] Attached to the head frame, follows head yaw and pitch, no animation.
- [ ] Hidden or covered with closed helmets; not drawn at all when the helmet is flagged "hide face slot".

## 10. Unsure

- **Which render to trust.** The reference render of this accessory is the unused ModelGasMask2; the only thing it has in common with the real model is the gray palette. I used the model code and the real texture instead. If a real render of the Oxygen Mask is ever made, compare it with this page.
- **Strap height.** The band's position (y -2.5 to -1) comes from the painted rows of a very low-resolution texture; it could be off by half a unit.
- **Left-right mirroring.** The cube is built mirrored; the strap and patch are left-right symmetric except for a faint 0.5-unit nick in the patch, so I ignored it.
- **Helmet inflation.** Not measured per helmet; "almost any helmet covers the strap" is from the 0.06 oversize against a half-unit-or-more inflation.
