# Turret head: clean-room model notes

Design notes for rebuilding the Techguns turret **head** (the yoke the gun is mounted on) in GregGuns. Written by the "dirty room" analyst from study of the original's model, renderer and textures. They are meant to be complete enough to build from without ever seeing the original. They contain no source, no box lists and no texture coordinates.

Related: [turret_base](turret_base.md) (what the head stands on).

**Units and axes.** One unit = 1/16 block. **Y points up.** Positions are relative to the **entity's feet center** (X = 0, Z = 0 at the center of the base's top plate, Y = 0 at its surface). The head is **left-right symmetric** about the gun's firing line: the two uprights stand on the left and right of the gun, and the gun points along Z. Converting from the original: it is built Y-down; the renderer lifts it 1.5 blocks and flips it 180 degrees about X, so our height = 24 minus the original's Y, and our Z is the negative of the original's. The model itself has no scale; the gun gets one (below). Rounded to half a unit.

## 1. Gestalt

A **small pedestal-style gun yoke**: a thin square foot plate, two skinny vertical posts at left and right, and a small square mount plate between the posts near their tops. The gun item hangs on that plate and pitches up and down. Nothing else: no armor shell, no sensor dome. It reads as a **gun on a stand**, not a robot.

Defining features in order of importance:

1. **Two thin uprights** (2 x 2, 11 tall), one each side, like a fork or goalposts.
2. **A flat foot plate** (8 x 8, 1 thick) at the floor.
3. **A small square mount plate** (4 x 4, 1 thick) between the uprights near their tops; this pitches with the gun.
4. The gun, drawn at 0.375 scale on the mount.

## 2. Overall proportions

| Measure | Value |
|---|---|
| Foot plate | 8 x 1 x 8, centered, Y 0 to 1 |
| Uprights | 2 x 11 x 2 each, Y 1 to 12 |
| Gap between uprights | 4 (inner faces at X = -2 and +2) |
| Width (X) | 8 overall (the uprights' outer faces are flush with the foot's edges) |
| Depth (Z) | 8 at the foot, 2 at the uprights |
| Mount plate | 4 x 1 x 4, Y 10 to 11 |
| Yoke height | 12 |

The original's entity hit box is 1 block tall, so the yoke (12) is shorter than the hit box (16).

## 3. Part by part

### Foot plate
- A flat 8 x 8 plate, **1 thick**, centered on the feet point, **Y 0 to 1**; it covers the base's 8 x 8 top plate.
- Plain steel.

### Uprights
- Two posts, each **2 wide (X), 2 deep (Z), 11 tall**, centered in Z, standing on the foot plate: one at **X -4 to -2**, one at **X 2 to 4**, **Y 1 to 12**.
- No braces, no taper.

### Mount plate (the pitching part)
- A small flat plate **4 x 1 x 4**, X -2 to 2, Z -2 to 2, **Y 10 to 11**, between the uprights. Its sides touch the uprights' inner faces exactly.
- This plate and the gun rotate about a horizontal axis through **X = 0, Z = 0, Y about 9.5** that runs **left to right (the X axis)**, so they pitch up and down. The foot and uprights do not pitch.

### Gun
- The held gun (any gun item) is drawn on the mount at **scale 0.375**, about 10 units above the feet, within 1 unit of X = 0, Z = 0. A small per-gun offset is added. The gun is turned so its muzzle points along the head's forward direction. It pitches with the mount.

## 4. Silhouette checks

- **Front or back (along the firing line):** a thin "U": two vertical posts 2 wide and 4 apart on an 8-wide foot, with the gun in the gap.
- **Side:** an 8-wide foot with one 2-wide post rising from its center; the 4-deep mount plate near the top; the gun extends forward.
- **Top:** an 8 x 8 square foot with two 2 x 2 squares at its left and right edges and a 4 x 4 mount between them.

## 5. Materials and palette

The head shares the 64 x 64 texture sheet of the turret base, so it follows the same five armor tiers: light gray steel (about #949494, highlight #A4A4A4) at the lowest tier through to near-black (about #1C1C1C) at the top. Foot and uprights take the frame tone of the base; the mount plate is a slightly lighter gray. Flat, noise-level shading, no logos.

## 6. In-game presentation

- It is an **entity** standing on the turret base block; both share the foot point.
- The texture follows the turret's armor total: 0, 5, 10, 15 and 20 select the five tiers.
- It is drawn in two passes: the fixed yoke (foot and uprights, yawed to the head yaw), then the pitching mount with the gun.

## 7. Motion

- **Yaw:** the whole head (foot, uprights, mount, gun) rotates about the vertical axis through the feet center, following the target.
- **Pitch:** the mount and gun rotate about the left-right axis at about Y 9.5, X 0, Z 0, by the entity's pitch, while the yoke stays still. Positive pitch looks down, as in vanilla.
- No other animation: no recoil on the mount and nothing spinning.

## 8. What our earlier attempts got wrong

GregGuns has only a **placeholder cube** for the turret entity so far; there is no real model attempt to compare. The builder should avoid: a solid block (the original is open between the uprights), a gun fixed to the base (it must pitch), and pitching about the feet (pitch happens at gun height).

## 9. Acceptance checklist

- [ ] Foot plate 8 x 1 x 8 at Y 0 to 1.
- [ ] Two uprights 2 x 11 x 2 at X -4..-2 and 2..4, Y 1 to 12.
- [ ] Mount plate 4 x 1 x 4 at Y 10 to 11 between the uprights, touching both.
- [ ] Yaw pivots about the vertical axis through the foot center.
- [ ] Pitch pivots about the left-right axis at about Y 9.5; only the mount and gun pitch.
- [ ] Gun is drawn at 0.375 scale at about 10 units height.
- [ ] Yoke height is 12, well under a block.
- [ ] Uses the same tier textures as the base block.
- [ ] No part floats.

## 10. Unsure

- **Rest facing.** The model is symmetric; which Z direction is "forward" at yaw 0 depends on renderer flips I could not settle without running it.
- **Pitch sign.** The renderer works in a flipped frame, so the sign may invert; make positive pitch look down.
- **Per-gun offsets.** Each gun has its own small offset that I did not collect; use about (0, 10, 0) and adjust.
- **Unused variant.** The original also holds a one-post version (a single upright at X -3 to -1, mount 2 wide) that no renderer I found uses. Ignored.
