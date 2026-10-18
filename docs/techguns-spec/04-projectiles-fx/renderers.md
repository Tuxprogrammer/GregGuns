# Projectile, beam and FX renderers

These are the renderers in `java/techguns/client/renderer/entity/` that draw non-living entities, plus `client/renderer/FX/ScreenEffect.java`. NPC and mob renderers ([`RenderAlienBug`](../10-entities-npcs/rendering-and-models.md#4-renderalienbug-extends-renderliving), [`RenderGenericNPC`](../10-entities-npcs/rendering-and-models.md#2-rendergenericnpc-extends-vanilla-renderbiped), [`RenderHelicopter`](../10-entities-npcs/rendering-and-models.md#5-renderhelicopter-extends-rendererlivingentity), [`RenderSimpleTurret`](../10-entities-npcs/rendering-and-models.md#6-rendersimpleturret-extends-plain-render-for-npcturret), [`RenderSuperMutant`](../10-entities-npcs/rendering-and-models.md#3-rendersupermutant-extends-rendergenericnpc)) belong to [`10-entities-npcs`](../10-entities-npcs/index.md). Projectile models ([`ModelRocket`](../03-gun-visuals-audio/gun-models.md#modelrocket-rocket-item--projectile-12864-11-boxes), [`ModelStielgranate`](../03-gun-visuals-audio/gun-models.md#modelstielgranate-3232-3-boxes), [`ModelFragGrenade`](../03-gun-visuals-audio/gun-models.md#modelfraggrenadeshowring-declared-3232-used-with-a-6464-png-11-boxes), [`models/entity/launchergrenade.obj`](../10-entities-npcs/rendering-and-models.md#other-obj-files-in-modelsentity-in-this-folder-used-elsewhere)) and the gun textures they use are documented in [`03-gun-visuals-audio`](../03-gun-visuals-audio/index.md). Entity-to-renderer bindings are listed in [projectiles.md §1](projectiles.md#1-registration).

## Shared conventions

- **"Cross quads":** several renderers copy the vanilla arrow technique. They translate to the entity, rotate by `yaw − 90` around Y, then by `pitch` around Z, so local +X lies along the flight axis. They roll 45° around X, scale by **0.0125** (80 model units = 1 block), and draw 4 quads, each rotated a further 90° around X. Seen end-on this forms an "X" cross section.
- **Full-bright:** `TGRenderHelper.enableFXLighting()` pushes the matrix, disables lighting and sets the lightmap to (240, 240). `disableFXLighting()` reverses it.
- **Blend modes:**

  | Mode | GL settings |
  |---|---|
  | `enableAlphaBlendAdditive()` | `GL_ONE, GL_ONE` (pure additive, ignores alpha) |
  | ADDITIVE (`enableBlendMode`) | `SRC_ALPHA, ONE` |
  | ALPHA | `SRC_ALPHA, ONE_MINUS_SRC_ALPHA` |
  | ALPHA_SHADED | blending enabled with the blend function left as is, and normal world lighting (not full-bright) |
  | SOLID | no blending, full-bright |
  | NO_Z_TEST | additive, and depth test and depth writes off |

- Several renderers skip the first ticks (`ticksExisted >= 2` or `>= 3`) so the projectile is not drawn inside the gun.

## Catalogue

| Renderer | Draws | Texture(s) | Geometry and animation |
|---|---|---|---|
| [`RenderDummy`](projectiles.md#1-registration) | nothing | none | Empty. Used for invisible carriers (flamethrower, laser, tesla, cyberdemon blaster, alien blaster, energy blast, invisible, beam projectile, particle system). |
| [`RenderGenericBullet`](projectiles.md#1-registration) | bullets ([`GenericProjectile`](projectiles.md#2-genericprojectile-the-base-for-almost-everything) and unregistered subclasses such as [`MiningDummyProjectile`](projectiles.md#314-miningdummyprojectile)) | [`textures/entity/bullet.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | From tick 2. Cross quads 16 × 4 units (0.2 × 0.05 blocks) centred on the entity. UV u 0–0.5, v 0–5/32 (the top-left slice of the 32 × 32 texture). Normal lighting, no blending. |
| `RenderSimpleProjectile(scale, texture)` | handcannon ball | [`textures/entity/handgunBullet.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) (scale 1.0) | Camera-facing billboard: rotate 180 − viewYaw around Y and −viewPitch around X. One quad from (−0.5, −0.25) to (0.5, 0.75), scaled by 0.1 × scale, so it is 0.1 blocks across. Full UV. The entity's own texture and scale fields are ignored. |
| `RenderBiogunProjectile(texture)` | bio blob | [`textures/entity/bioblob.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | Billboard quad as above, scaled 0.15 × s, where s = 1.25 / 2.5 / 4.0 for `level` 1 / 2 / 3 (otherwise 1). Additive (ONE, ONE) and full-bright. A glow pass with `lensflare1.png` is commented out. |
| `RenderRocketProjectile(model, texture)` | rocket, nuke rocket | `textures/guns/rocketlauncher.png`, `textures/guns/nukerocketlauncher.png` | Translate, rotate by interpolated yaw − 90 (Y) and interpolated pitch (Z), scale 0.9, render `ModelRocket` at 1/16 scale. |
| `RenderStielgranateProjectile(model, texture)` | stick grenade, frag grenade | [`textures/guns/stielgranate.png`](../03-gun-visuals-audio/rendering-framework.md#12-grenade-renderers-rendergrenade), [`textures/guns/fragGrenade_texture.png`](../03-gun-visuals-audio/rendering-framework.md#12-grenade-renderers-rendergrenade) | Adds +0.95 to Y, then orients by interpolated yaw and pitch. **Tumbles:** rotates about Z by −360 × (ticksExisted mod 20)/20 − 18 × partialTick degrees (one turn per second) around a pivot 0.85 below the origin. Scale 0.8, model at 1/16. |
| `RenderGrenadeLauncherProjectile(ModelModel, texture)` | 40 mm grenade | [`textures/entity/launchergrenade.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) + `models/entity/launchergrenade.obj` | Orients by interpolated yaw and pitch. Spins about Z by −360 × (ticks mod 40)/40 − 18 × pt (one turn per 2 s), with no pivot offset. Scale 0.8. |
| [`RenderBlasterRifleProjectile`](projectiles.md#1-registration) | blaster rifle bolt | [`textures/fx/laser3.png`](textures.md#single-images) | From tick 2. Cross quads 48 × 2 half-extent units (1.2 blocks long, 0.05 wide), full UV, additive ONE/ONE, full-bright, culling off. |
| [`RenderPowerHammerProjectile`](projectiles.md#1-registration) | expanding shock ring | [`textures/fx/shockwave.png`](textures.md#single-images) | A single quad perpendicular to flight (in the plane normal to local X), half-size 10 units. Scaled by 0.0125 × (ticksExisted + pt)², so the side length is 0.25 × t² blocks and grows quadratically. Additive ONE/ONE. |
| [`RenderSonicShotgunProjectile`](projectiles.md#1-registration) | sonic volley | `textures/fx/shockwave.png` (`sonicwave4x4.png` and `bullet.png` are referenced in commented code) | Main projectile only, from tick 3, pass 1. Oriented along the flight axis with **no 0.0125 scale** (units are blocks). Uses `prog = min(1, (t + pt)/20)`, `opacity = 1 − prog²` and `d = 0.3 × (t + pt)`. Draws **6 quads** perpendicular to the flight axis at axial offsets +2.5, +1.5 … −2.5 blocks. Each quad's half-size is d × (0.5 + r) and its colour is (opacity × r₁, opacity × r₂, opacity × (0.5 + 0.5 r₃), opacity). All r values come from a `Random` seeded with the entity id, so they are stable per projectile. Additive ONE/ONE. Non-main projectiles draw nothing. |
| `RenderTeslaBeam(xOffset)` | lightning bolt (TeslaBeam) | [`textures/fx/laser_blue.png`](textures.md#single-images) | See "Tesla bolt" below. |
| `RenderLaserBeam2` | laser beam (LaserBeam2) | `textures/fx/laser3.png` | See "Laser beam" below. |
| `RenderGenericLaser` | old laser beam (LaserBeam) | `textures/fx/laser3.png` | The same algorithm as RenderLaserBeam2, with the length computed from the synced target point. |
| `RenderMovingBeam` | death-ray beam (MovingBeam, NeutrinoBeam) | [`textures/fx/nukebeam.png`](textures.md#sprite-sheets-animated-or-variation-grids) | See "Nuclear death ray" below. |
| [`RenderRailgunProjectile`](projectiles.md#1-registration) | railgun slug (unused) | [`textures/entity/BlueBeam.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | From tick 2. Cross quads 160 × 4 units (2 blocks long). UV u 0–0.5, v 0–5/32. |
| [`RenderFlyingGibs`](death-effects.md#flying-gibs-rendering) | gore gibs | the dying entity's texture (from [`GoreData`](../01-core/client-effects-audio.md#deatheffectcreatedeatheffectentity-type-mx-my-mz)) | See [death-effects.md](death-effects.md#flying-gibs-rendering). |
| [`RenderBlasterProjectile`](projectiles.md#1-registration) (unbound) | deprecated blaster bolt | [`textures/entity/Laser.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | Cross quads 296 × 4 units (3.7 blocks), UV u 0–0.5, v 0–5/32. |
| `RenderFlamethrowerProjectile` (unbound; ClientProxy comment shows the intended [`textures/fx/fireball.png`](textures.md#single-images), scale 1.5) | flame blob | constructor argument | Billboard quad scaled 0.1 × scale. |
| [`RenderPiercingProjectile`](textures.md#single-images) (unbound) | deprecated wave | [`textures/fx/bio.png`](textures.md#single-images) | Two parallel square quads at local x = ±size/2 with half-extent `size` (interpolated min to max size over `sizeEnd`). Additive. |

### Laser beam (`RenderLaserBeam2`, `RenderGenericLaser`)

`maxTicks = Lasergun.BEAM_LIFETIME` (5), `laserWidth = 2`.

- `prog = ticksExisted / 5`. There is no partial-tick term, so the beam steps once per tick.
- Half-width `w = 2 × sin(√prog × π) × 2` units. It swells from 0 and returns to 0 at prog = 1.
- Brightness (vertex alpha) = sin(√prog × π).
- Length L = `beamlength` × 80 units, drawn from local x = −L to 0. The entity sits at the far end, and the yaw/pitch negation in LaserBeam2 points the strip back to the muzzle.
- Texture u runs from `prog` to `prog + (beamlength / 2) × 2` (it scrolls along with prog); v runs 0–1 across the width.
- Orientation: rotate −(yaw − 90) around Y, pitch around Z, then (45 + 180 × prog) + 90 around X, which spins the ribbon as it fades.
- Two quads at 90° to each other. Blend SRC_ALPHA/ONE, full-bright, culling off.

### Tesla bolt (`RenderTeslaBeam`)

Constants: vertex spacing `offset = 0.20` blocks, 5 overlapping sine terms (`sinCount`), base width 0.5, `SIN_DISTANCE = 10`, `maxTicks = Teslagun.BEAM_LIFETIME` (10). The binding constructor passes `xOffset = 0.25`.

1. `rand = Random(seed)`, where `seed` comes from spawn data, so every client draws the same bolt. For j = 1..5, pick fixed `dY[j]` and `dZ[j]` uniformly in [−0.5, 0.5].
2. `progress = ticksExisted / 10`. `distance = beamlength − 0.25`. Vertex count `n = round(distance / 0.2)`. The spacing is then rescaled to `distance / n`. This writes back into the shared renderer field (bug [PFX-11](index.md#suspected-bugs)). `xreps = max(1, round(distance / 10))`.
3. Orientation: translate to the entity, rotate −(yaw + 90) around Y and −pitch around Z. Additive ONE/ONE, full-bright, culling off.
4. For vertex i = 0..n with d = i/n and x = 0.25 + i × spacing:
   - For i > 1, y and z are sums over j = 1..5 of sin(d × π × j × xreps) × ((rand − 0.5) + progress × dY[j]) × 2/j. Same for z with dZ.
   - So the bolt keeps a stable overall shape that drifts with `progress`, plus per-frame jitter from `rand`. The jitter is deterministic, so it does not change between frames of the same tick.
   - `pulse = 1 − √(|progress − d| × 2)`. This is a bright pulse travelling from the shooter to the target over the bolt's lifetime.
   - Half-width = max(0, 0.5 × pulse).
5. Each segment from vertex i−1 to i is drawn as two quads (one in XY and one in XZ), rotated 45° about X. y, z and width are pre-scaled by 0.5. The quads use the previous and current widths, vertex alpha = pulse (previous for the first quad, current for the second), and full UV (u along the segment, v across).

### Nuclear death ray (`RenderMovingBeam`)

Constants: `laserWidth = 3`, `maxTicks = Beamgun.BEAM_LIFETIME` (10). `rand = Random(entityId)`.

1. `prog = (ticksExisted + pt) / 10`. Half-width `w = 3 × sin(√prog × π) × 2`. Brightness = sin(√prog × π).
2. The origin is computed from the **shooter** (via `shooterID`) relative to the local player's eye, not from the entity's own render offset. Pitch and head yaw are interpolated.
   - For the local player in first person, add the offset (0, −0.08, 0.12) rotated by pitch and yaw. It then applies a copy of the vanilla view-bobbing transform with both sign factors −1, so the beam moves with the bobbing gun.
   - Otherwise shift 0.16 sideways and raise by 0.1 + `get3PYOffset()`. This differs in sign from the entity's own origin (bug [PFX-16](index.md#suspected-bugs)).
3. Translate −0.25 along the beam and add 0.25 to the length. L = (beamlength + 0.25) × 80 units. Rotate (45 + 360 × prog) + 90 around X.
4. **Main beam:** 17-frame vertical strip animation. `frame = floor((t + pt) × 0.5) mod 17`, so the strip advances one frame every 2 ticks. v spans `[frame/17, (frame+1)/17]`. Two crossed quads; the second is offset by u + 1. u runs from 0 to L / (3 × 8 × 2), so the texture repeats every 48 units (0.6 blocks). Vertex alpha = brightness.
5. **Spiral:** the length is clamped to 400 units (5 blocks), with `segments = floor(L/8)`.
   - For segment i, with iprog = i/segments: radius = 0.5 × (1 − cos(2√iprog × π)) × 8 units.
   - Angle = `angleOffset − (π/8) × i`, where `angleOffset` is a random value seeded from the entity id.
   - Draw a ribbon of half-width 4 units along the local beam axis, connecting consecutive points on the helix.
   - u advances by `2cos((π − π/8)/2) × radius / 8` per segment. v uses the same frame band as the beam.
6. Blend SRC_ALPHA/ONE, full-bright, culling off.

## `ScreenEffect` (muzzle flashes and scope overlays)

A static helper used by the first-person gun renderers ([`03-gun-visuals-audio`](../03-gun-visuals-audio/index.md)) to draw a sprite-sheet flash at an offset in item-render space.

`doRender(progress, ox, oy, oz, scale[, rotX, rotY, rotZ])`:
- `frame = floor(numSprites × progress)`. Nothing is drawn once the frame is ≥ numSprites.
- Picks the cell (frame mod cols, frame div cols) from a cols × rows grid.
- Full-bright (brightness 15728880). Rotates X, Y, Z, scales by `scale`, then draws one **horizontal** quad (in the XZ plane at y = oy) of side `scale`, centred at (ox, oz).
- U is mirrored (U2 to U1 across +x).
- It does not set any blend mode. The caller's GL state applies.

| Static instance | Texture | Grid (cols × rows) / sprites | Used by (active calls) |
|---|---|---|---|
| `muzzleFlash` | [`textures/fx/muzzleflash4x4.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 10 | RenderCombatShotgun, RenderGrenadeLauncher, RenderHandgun, RenderRevolver, RenderRevolverAkimbo, RenderSawedoff |
| `muzzleFlash2` | [`textures/fx/muzzleflash2.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 8 | RenderAK, RenderAS50, RenderAUG, RenderBoltActionRifle, RenderLMG, RenderM4, RenderMac10, RenderMac10Akimbo, RenderMinigun, RenderPistol, RenderPistolAkimbo, RenderScar, RenderThompson, RenderVector |
| `muzzleFlash_blue` | [`textures/fx/muzzleflash_blue.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 8 | RenderPDW, RenderPulseRifle |
| `muzzleFlashYellow` | `textures/fx/muzzleFlashYellow2x2.png` (**file missing**) | 2×2 / 4 | unused |
| `muzzleFlashLaser` | [`textures/fx/laserflare02.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 7 | RenderBlasterRifle, RenderLasergun, RenderLaserGunNew |
| `muzzleFlashLightningOld` | [`textures/fx/lightningflare4x4_2.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 16 | unused |
| `muzzleFlashFireball_alpha` | [`textures/fx/fireball_4x4_alpha.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 16 | unused |
| `muzzleFlashFireball_add` | [`textures/fx/fireball_4x4_add.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 16 | unused |
| `muzzleFlashBlaster` | [`textures/fx/blasterflare01.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 14 | RenderCyberdemonBlaster |
| `muzzleFlashAlienBlaster` | [`textures/fx/alienflare.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 14 | RenderAlienBlaster, RenderMibGun, RenderMibGunAkimbo |
| `muzzleFlashLightning` | [`textures/fx/teslaflare01.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 14 | RenderTeslaGun |
| `FlamethrowerMuzzleFlame` | [`textures/fx/flamethrower2.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 16 | RenderFlamethrower |
| `muzzleFlashSonic` | [`textures/fx/sonicwave4x4.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 16 | RenderSonicShotgun |
| `muzzleFlashNukeBeam` | [`textures/fx/nukebeamflare.png`](textures.md#sprite-sheets-animated-or-variation-grids) | 4×4 / 16 | RenderBeamgun, RenderNeutrinoGun |
| `muzzleFlashNew` | `textures/fx/muzzleflashnew.png` (**file missing**) | 4×4 / 16 | unused |
| `muzzleFlashNew2` | `textures/fx/muzzleflashnew_2_add.png` (**file missing**) | 4×4 / 16 | unused |
| `sniperScope` | [`textures/fx/Testscope.png`](textures.md#single-images) | 1×1 / 1 | RenderAS50, RenderAUG, RenderBlasterRifle, RenderBoltActionRifle, RenderM4, RenderM4Scoped, RenderScar, RenderVector |
| `techScope` | [`textures/fx/techscope.png`](textures.md#single-images) | 1×1 / 1 | RenderPulseRifle |

`SNIPER_SCOPE_SCALE = 2.75`. Scope overlays are drawn with progress 0, which always selects frame 0.
