# NPC Turret: clean-room model notes

The Techguns **NPC turret entity** (`NPCTurret`, registered as `SimpleTurret`) is drawn with the **same geometry as the turret block's head**. It is the same box set, the same texture sheet and the same two-pass yaw and pitch drawing. **The full geometry, palette and checklist already live in [turret_head.md](../../09-machines-utility/model-notes/turret_head.md)** (the yoke) and [turret_base.md](../../09-machines-utility/model-notes/turret_base.md) (the block it stands on). The builder should use those pages for shapes and colors. This page lists only what differs when the model is used as the **entity**, and confirms that nothing else differs.

**Frame used in this document.** The Minecraft entity-model frame, one unit = 1/16 block, **Y down**, origin 24 units above the feet, the mob faces -Z and its right side is -X. The turret_head page uses a Y-up, feet-relative frame instead; the two agree once converted (height above the feet = 24 minus model Y). Everything below is stated in height above the feet.

## 1. Verdict: the same geometry

Checked item by item against the head page:

| Part | Entity model | Head page | Same? |
|---|---|---|---|
| Foot plate | 8 x 1 x 8, centered, height 0 to 1 | 8 x 1 x 8, Y 0 to 1 | yes |
| Two uprights | 2 x 11 x 2, at x -4..-2 and 2..4, height 1 to 12 | the same | yes |
| Mount plate | 4 x 1 x 4, x -2..2, height 10 to 11 | the same | yes |
| Pitch axis | left-right axis, 9.6 units above the feet (0.6 block) | "about 9.5" | yes (rounding) |
| Texture | `TurretBase` sheets, same layout as the block | same sheet | yes |
| Armor tiers | five recolors, 0 / 5 / 10 / 15 / 20 | same | yes |
| Gun scale | 0.375 | 0.375 | yes |

The entity's model is exactly the head. There is no extra part, no dome, no armor shell and no second variant. (The original also contains a one-post "simple turret" model with a single upright; the head page ignores it, and so does this page.)

## 2. Differences when it is an entity

### Texture selection
- The sheet is chosen from the turret's **total armor value**: 0 selects the base sheet; 5, 10, 15 and 20 select the iron, steel, obsidian-steel and carbon sheets. **Any other value falls back to the base sheet.** (Armor values come in steps of 5, so in play only those five occur.) The sheets are the same 128 x 128 files as the block, which uses 64-unit UVs at 2 pixels per unit.
- The texture is re-chosen every frame, so adding an armor plate re-skins the turret at once.

### Placement and scale
- Render scale **1.0**. No shadow. The renderer is a plain one, not a living-entity renderer, so there is **no name tag, no red hurt flash and no death topple**. When it dies it simply stops being drawn.
- The head stands with its foot plate on the base block's top plate: the entity's feet are at the block center, one block above the block's bottom (y + 1).
- Hitbox: about 0.6 wide x 1.0 tall (the yoke is 0.75 tall, so the hitbox is taller than the model); eye height 0.8.

### Motion
- **Yaw:** the whole yoke (foot, uprights, mount and gun) rotates about the vertical axis through the feet center by the entity's **head yaw**. It is not interpolated between ticks, so at low frame rates it moves in 20-per-second steps. Our version may interpolate.
- **Pitch:** the mount plate and the gun rotate about the left-right axis 9.6 units above the feet by the entity's pitch; the foot and uprights do not. Positive pitch looks down, as in vanilla. Also not interpolated.
- The AI turns the turret toward its target at no more than 30 degrees of yaw and 30 degrees of pitch per tick, so it swings fast. There are no limits on yaw (it can face any way) and the pitch range is whatever the AI produces.
- There is no idle animation, no recoil and nothing spinning. When it has no target it looks around at idle, using the same two motions.

### Gun placement
- The gun is drawn after the pitching mount, in the pitching frame, so it pitches with the mount. Its position is about 10 units above the feet (0.625 block), within 0.4 unit of the centerline sideways and about 0.2 unit forward of the center, **plus a small per-gun offset** that each gun item declares for turret use.
- It is scaled to **0.375** and turned so its muzzle points along the head's forward direction (a fixed -55 degree twist of the item about the vertical axis relative to the usual held-item orientation, on top of a 170-degree turn of the frame).
- The per-gun offsets were not collected; the head page's guess of about (0, 10, 0) units still applies.

### Lighting and blending
- None. Drawn opaque with normal world lighting at the entity's position.

## 3. What to avoid

- Duplicating the head's geometry in a second, different model. Use the same shapes as the block's head, with only the differences above.
- Giving it a shadow or a hurt flash.
- Pitching the whole yoke instead of only the mount and gun.
- Using the tier by block metadata: the tier comes from the armor value, 0 / 5 / 10 / 15 / 20.

## 4. Acceptance checklist

- [ ] Geometry identical to the head page: foot 8 x 1 x 8, uprights 2 x 11 x 2, mount 4 x 1 x 4.
- [ ] Five armor tiers pick the five sheets; unknown values show the base sheet.
- [ ] Whole yoke yaws about the feet-center vertical axis.
- [ ] Only the mount and gun pitch, about 9.6 units above the feet.
- [ ] Gun drawn at 0.375 scale at about 10 units height, pitching with the mount.
- [ ] No shadow, no name tag, no hurt flash, no death animation.
- [ ] Hitbox about 0.6 x 1.0; eye height 0.8.
- [ ] No part floats; the foot plate rests on the base block's top plate.

## 5. Unsure

- **Pitch sign.** The original draws in a flipped frame; I made positive pitch look down and did not verify in game.
- **Exact gun offset.** The per-gun turret offsets were not collected.
- **Which Z is forward at yaw 0.** The yoke is symmetric about the firing line, so I left it undecided, as the head page does.
- **Our M6 placeholder.** GregGuns already draws the head and base from its own box list in the utility client package; this page assumes that code follows the head page and was not re-checked here.
