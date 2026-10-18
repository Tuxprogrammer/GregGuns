# 04: Projectiles and FX (GregGuns delta)

## Overview

Techguns' projectiles, renderers, explosions, particle system and death effects are in the [Techguns spec](../../techguns-spec/04-projectiles-fx/index.md). GregGuns rebuilds them from that spec. Most of the area has **no integration point**. Four sheets cover what the pack changes: how effects are lit, how particles are drawn and counted, and what explosions do to protected blocks and GregTech machines.

No integration point; see [techguns-spec](../../techguns-spec/04-projectiles-fx/index.md) and the design records:

- Projectile classes, flight, fall-off, collision, NBT and sync ([projectiles.md](../../techguns-spec/04-projectiles-fx/projectiles.md)). Entity registration uses mod-local IDs via `EntityRegistry.registerModEntity`. No pack-side constraint on entity registration was found (Angelica, Hodgepodge and ServerUtilities were searched).
- Projectile renderers' geometry and textures, except the GL and batching rules in [03's Angelica sheet](../03-gun-visuals-audio/angelica-item-rendering.md).
- Death effects and gore ([death-effects.md](../../techguns-spec/04-projectiles-fx/death-effects.md)).
- FX textures and lang keys.

## Contents

| File | What it covers |
|---|---|
| [dynamic-lights.md](dynamic-lights.md) | Angelica dynamic lights for held guns, armour and projectiles: the contract, the cost and the rules. |
| [particle-rendering.md](particle-rendering.md) | What `EntityParticle` must do to be instanced by Angelica, and the pack's 8000-particle cap. |
| [explosions-pack.md](explosions-pack.md) | ServerUtilities' explosion filter and claim protection, GregTech machine chain explosions and logging, resistance lookup. |

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Muzzle flash and projectile glow (sprites only) | Optional dynamic light from held guns and slow projectiles; off with shader packs | Angelica dynamic lights | [dynamic-lights.md](dynamic-lights.md) |
| `EntityParticle` with its own texture bind and draw per particle | One quad through the given `Tessellator`, so Angelica can instance it | Angelica particle batching | [particle-rendering.md](particle-rendering.md) |
| Unbounded particle bursts | Global ceiling below the pack's cap of 8000 | Hodgepodge (`particleLimit`) | [particle-rendering.md](particle-rendering.md) |
| Part B block destruction ([explosions.md](../../techguns-spec/04-projectiles-fx/explosions.md#part-b-sound-block-destruction-and-fire)) | Break only blocks left in the affected list after `ExplosionEvent.Detonate` | ServerUtilities | [explosions-pack.md](explosions-pack.md) |
| `MiningDummyProjectile` and other block breakers (PFX-14) | Post `BlockEvent.BreakEvent` for the owner | ServerUtilities claims | [explosions-pack.md](explosions-pack.md) |
| `block.onBlockExploded` on a dummy explosion | Kept. GT machines may chain a second explosion. | GT5-Unofficial | [explosions-pack.md](explosions-pack.md) |
| Sounds for projectiles and explosions | Same rules as gun sounds | Hodgepodge, Extreme Sound Muffler | [03 sound sheet](../03-gun-visuals-audio/gun-sound-pipeline.md) |

## Pack-side changes

None required. Every setting involved is a player or server-owner choice (Angelica dynamic-light mode, ServerUtilities explosion options, GregTech `machineExplosions`, Hodgepodge `particleLimit`, `numberNormalChannels`).

| File | Change | Why | Sheet |
|---|---|---|---|
| none | none | none | none |

## Related areas

- [03-gun-visuals-audio](../03-gun-visuals-audio/index.md): item renderers, HUD, gun sounds.
- [09 machines utility (Techguns)](../../techguns-spec/09-machines-utility/index.md): explosive charges that call the same explosion class (M6).
- [../INTEGRATION.md](../INTEGRATION.md): evidence rules.

## Open questions

- Held-gun muzzle light: how the item learns that a shot just happened. Possibly a design record 0007. See [dynamic-lights.md](dynamic-lights.md#open-questions).
- Whether high-tier explosives should skip `onBlockExploded` on GT machines. See [explosions-pack.md](explosions-pack.md#open-questions).
- Shooter immunity to own explosions (PFX-25) against ServerUtilities team settings. Same sheet.
