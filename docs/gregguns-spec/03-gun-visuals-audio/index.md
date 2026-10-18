# 03: Gun visuals and audio (GregGuns delta)

## Overview

Techguns' gun models, item renderers, textures and sounds are in the [Techguns spec](../../techguns-spec/03-gun-visuals-audio/index.md). GregGuns rebuilds them from that spec under [design 0006](../../design/0006-art-direction.md). Most of the area has **no integration point**: model geometry, animation curves, textures and the per-gun sheets behave as the Techguns spec says.

Two things in the pack change how that behaviour must be built:

- **Angelica 2.2.28** replaces the GL layer, batches entity model parts, instances particles, caches the HUD and adds a zoom key. Our `IItemRenderer`s, muzzle flashes and scope overlays must work under it.
- **Hodgepodge 2.7.215** and **Extreme Sound Muffler 1.1.1** change how gun sounds are decoded, positioned, counted and muted.

## Contents

| File | What it covers |
|---|---|
| [angelica-item-rendering.md](angelica-item-rendering.md) | GL calls, entity batching eligibility, HUD caching and the zoom key, as they affect gun renderers, muzzle flashes and scope overlays. |
| [gun-sound-pipeline.md](gun-sound-pipeline.md) | Stereo handling, channel cap, mute-ability and event naming for gun sounds. |
| [../04-projectiles-fx/dynamic-lights.md](../04-projectiles-fx/dynamic-lights.md) | Angelica dynamic lights for held guns (muzzle glow). It lives in area 04 because it also covers projectiles. |

No integration point; see [techguns-spec](../../techguns-spec/03-gun-visuals-audio/index.md) and the design records: gun models, animations, per-gun transforms, texture layouts, camo textures, lang keys.

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| `IItemRenderer` per gun, drawing `ModelBase` boxes with raw GL ([rendering-framework §1](../../techguns-spec/03-gun-visuals-audio/rendering-framework.md)) | Keep plain `GL11`/`Tessellator` calls. Angelica rewrites them at class-load time. Never bypass the classloader. | Angelica (`angelica` 2.2.28) | [angelica-item-rendering.md](angelica-item-rendering.md) |
| First-person muzzle flash `ScreenEffect` ([§7](../../techguns-spec/03-gun-visuals-audio/rendering-framework.md#7-muzzle-flash-first-person-screen-effect)) | Unchanged geometry. Add an optional held-item light. | Angelica dynamic lights | [dynamic-lights](../04-projectiles-fx/dynamic-lights.md) |
| Scope overlay and zoom ([§9](../../techguns-spec/03-gun-visuals-audio/rendering-framework.md#9-zoom-and-scopes)) | Draw the overlay from `RenderGameOverlayEvent` types that Angelica's HUD cache re-runs every frame. Tolerate Angelica's own zoom key. | Angelica HUD caching and zoom | [angelica-item-rendering.md](angelica-item-rendering.md) |
| Gun sounds, 13 stereo files ([GVA-23](../../techguns-spec/03-gun-visuals-audio/index.md#suspected-bugs)) | Ship mono, per design 0006. The pack also repairs stereo, at twice the memory. | Hodgepodge | [gun-sound-pipeline.md](gun-sound-pipeline.md) |
| One-shot sounds of 1 to 3 s on every shot | Budget against a 64-channel cap. | Hodgepodge | [gun-sound-pipeline.md](gun-sound-pipeline.md) |
| Player-facing mute of gun sounds | Event names must be mute-able. | Extreme Sound Muffler | [gun-sound-pipeline.md](gun-sound-pipeline.md) |

## Pack-side changes

None required. Every item is a code-side rule. Optional pack tuning is listed in the sheets (for example `numberNormalChannels`).

## Related areas

- [04-projectiles-fx](../04-projectiles-fx/index.md): dynamic lights, particles and explosions.
- [../INTEGRATION.md](../INTEGRATION.md): how evidence is collected.
- Techguns baseline: [03-gun-visuals-audio](../../techguns-spec/03-gun-visuals-audio/index.md).

## Open questions

- Held-gun muzzle light: how does `getLuminance(ItemStack)` know a gun is firing? See [dynamic-lights.md](../04-projectiles-fx/dynamic-lights.md#open-questions). Needs a decision record if it changes the item NBT or packet layout.
- Channel budget for automatic weapons. See [gun-sound-pipeline.md](gun-sound-pipeline.md#open-questions).
