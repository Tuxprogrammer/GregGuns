# Textures

This page covers all 49 files in `textures/fx/`, plus the `textures/entity/` projectile textures that the renderers in this area bind. **No FX texture has a `.mcmeta` file.** All animation is code-driven: the sprite-sheet grid and frame count come from the code or data file that uses the sheet. Frames are numbered left to right, then top to bottom. The rest of a grid's cells stay empty (transparent or black).

Conventions used in the tables:
- **Blend** is how the texture is drawn (see [particle-system.md §3.4](particle-system.md#34-render-types)). Textures drawn additively are painted on **opaque black** (black = invisible), usually saved as RGB with no alpha channel or with alpha 255 everywhere. Alpha-blended textures use real transparency.
- "Tinted" means the grey or white art is multiplied by a code-supplied colour, so it should be painted neutral.
- Palette is the dominant colours in the visible parts.

## Sprite sheets (animated or variation grids)

| Path | Size | Grid (cell px) | Frames used | Depicts | Palette | Blend | Used by |
|---|---|---|---|---|---|---|---|
| `textures/fx/Explosion.png` | 480×480 | 5×5 (96) | 20 (17 visibly painted, the rest empty) | Fireball explosion lifecycle: small flicker, billowing yellow-white core with red-orange rim, then breaking into reddish smoke wisps that fade out. | white-yellow core, orange-red edges, pale pink smoke; transparent background | ALPHA | `bioblobExplosionExplosion` (tinted green), legacy `Explosion` and `ExplosionHuge` |
| `textures/fx/fireblast.png` | 512×512 (cells 102.4 px, not integer) | 5×5 | 25 (legacy) / 20 (data files, last row unused) | Full explosion cycle: dark seed, white-hot flash, yellow fire with black char, darkening into grey smoke, fading to pale white smoke. | white/yellow → orange/black → grey → pale; transparent | ALPHA | `LargeExplosion`, `LargeExplosionRing`, `LargeExplosionFrag`, `NukeExplosionMain`, `MushroomStem`, `MushroomCloud`, legacy `Fireblast` |
| `textures/fx/alienflare.png` | 512×512 | 4×4 (128) | 14 (rows 0–2 full, row 3 two cells) | Energy flare: tiny spark → large star-burst with long spikes → glowing cloud with electric filaments → shrinking wisps. | magenta/violet glow, white core; black background | ADDITIVE | `AlienFlare`, `AlienBlasterTrailFlare`, `BlueBlasterTrailFlare`/`BlueBlasterFlare` (tinted green-cyan), ScreenEffect `muzzleFlashAlienBlaster`, legacy `AlienBlasterFlare`, `MuzzleFlashAlienBlue` |
| `textures/fx/blasterflare01.png` | 512×512 | 4×4 (128) | 14 (`NukeExplosionBlast` uses 8) | Same composition as alienflare: spark → spiky burst → fiery cloud → fade. | orange-yellow, white core; black | ADDITIVE | `NetherBlasterImpactFlare(2)`, `CDB*Flares*`, `NukeExplosionBlast`, `NukeExplosionBall`, ScreenEffect `muzzleFlashBlaster`, legacy `BlasterFlare` |
| `textures/fx/teslaflare01.png` | 512×512 | 4×4 (128) | 14 | Same composition, electric style with a ring-shaped mid-phase. | electric blue, cyan-white core; black | ADDITIVE | `TeslaFlare`, `ChargeFlare`, ScreenEffect `muzzleFlashLightning`, legacy `TeslaFlare` |
| `textures/fx/laserflare01.png` | 512×512 | 4×4 (128) | 7 (row 0: 4, row 1: 3) | Lens flare with radial streaks: grows, peaks, shrinks to a point. | orange/amber glow, white core; black | (ADDITIVE) | **unused** (orange twin of laserflare02) |
| `textures/fx/laserflare02.png` | 512×512 | 4×4 (128); data files say 4 cols × 2 rows | 7 | As laserflare01. | saturated red glow, white core; black | ADDITIVE | `LaserFlareStart`/`End`, ScreenEffect `muzzleFlashLaser`, legacy `LaserFlare*`, `MuzzleFlashLaser` |
| `textures/fx/blobs.png` | 512×512 | 4×4 (128) | 16 variations | Sixteen glossy slime droplets/globs, each a different shape, with specular highlights. | lime/acid green, cyan highlights; black | ADDITIVE, variations | `bioblobExplosionBlobs` |
| `textures/fx/splat.png` | 384×384 | 3×3 (128) | 6 variations (rows 0–1) | Thin, cracked, vein-like splatter lines (like a web of liquid streaks). | grey-white lines (tinted green in use); black | ADDITIVE, variations | `biodeath`, `bioblobExplosionSplat`, `BioTrailSplat` |
| `textures/fx/blood4x4.png` | 256×256 (despite the name) | 2×2 (128) | 4 variations | Four blood-splatter puffs with spray droplets. | neutral grey (tinted to blood colour); transparent | ALPHA, variations | [`EntityBloodEffect`](death-effects.md#blood-burst-entitybloodeffect), [`FlyingGibs`](projectiles.md#319-flyinggibs) trail |
| `textures/fx/dustclouds.png` | 256×256 | 4×4 (64) | 16 | Grainy dust cloud: small dense puff → large → scattering and thinning. | mid grey; transparent | ALPHA_SHADED | legacy `DustClouds` (unused) |
| `textures/fx/dustclouds02.png` | 256×256 (cells 85.3 px) | 3×3 | 9 | Soft smoke puff that slowly spreads and thins. | mid grey (tinted); transparent | ALPHA / ALPHA_SHADED | `MediumExplosionSmoke`, `GrenadeLauncherTrail`, `JumpBoostSmoke`, `JumpPackFallRing`, no-blood death dust |
| `textures/fx/largesmoke.png` | 512×512 | 2×2 (256) | 4 variations | Large billowing smoke clouds of decreasing density (dense → faint). | grey with pinkish-mauve tint; transparent (max alpha 238) | ALPHA / ALPHA_SHADED | `LargeExplosionSmoke`, `laserdeathAsh`, `NukeExplosionSmoke`, legacy `LargeSmoke` |
| `textures/fx/fireball_4x4_add.png` | 256×256 | 4×4 (64) | 16 | Fire puff burning out: bright orange ball → dimmer embers → faint sparks. | orange/amber on black | ADDITIVE | legacy `Fireball`, `FireballImpact`, ScreenEffect `muzzleFlashFireball_add` (both unused) |
| `textures/fx/fireball_4x4_alpha.png` | 256×256 | 4×4 (64) | 16 | Rising flame puff fading to transparent. | yellow-orange-red; transparent | ALPHA | ScreenEffect `muzzleFlashFireball_alpha` (unused) |
| `textures/fx/flamethrower.png` | 512×512 | 4×4 (128) | 16 | Round turbulent fireball, swirling flame texture, changing each frame. | orange/yellow/white, dark red pockets; black | ADDITIVE | `laserdeathFire`, `CDBTrailFlames(_S)` |
| `textures/fx/flamethrower2.png` | 512×512 | 4×4 (128) | 16 | Upward flame tongues: tall thin flame → mushroom-shaped flare → licks. | orange/yellow; black | ADDITIVE (gun flash) | ScreenEffect `FlamethrowerMuzzleFlame` |
| `textures/fx/flamethrower_a.png` | 512×512 | 4×4 (128) | 16 | Fire ball with a soft white smoky halo: faint → full fire → whitish smoke. | yellow/orange core, white smoky edges; transparent | ALPHA | legacy [`Flamethrower`](../02-guns/per-gun-behaviour.md#flamethrower-flamethrower-flamethrower), `Jetpack`; `NukeExplosionRing` |
| `textures/fx/impact.png` | 256×256 | 2×2 (128) | 4 | Jagged white impact bursts / shards. | white-grey (tinted warm); black | ADDITIVE | legacy `impact` (power hammer) |
| `textures/fx/laserFlash2x2.png` | 256×256 | 2×2 (128) | 4 | Red spiky star flash, fading and breaking into dots in the second row. | red/pink; transparent | n/a | **unused** |
| `textures/fx/lightningflare4x4.png` | 512×512 | 4×4 (128) | 16 | Electric arc blob: forming → full jagged cyan blot → crackling fragments and dust. | cyan-white with a black speckle; transparent | n/a | **unused** |
| `textures/fx/lightningflare4x4_2.png` | 512×512 | 4×4 (128) | 12 painted (16 declared) | Faster variant of the above. | cyan-white; transparent | n/a | ScreenEffect `muzzleFlashLightningOld` (unused) |
| `textures/fx/muzzleflash2.png` | 256×256 | 4×4 (64) | 8 | Four-pointed "X" muzzle flash that grows, then dissolves into yellow speckle. | yellow/gold; transparent | ADDITIVE | ScreenEffect `muzzleFlash2`, legacy `MuzzleFlashYellow` |
| `textures/fx/muzzleflash4x4.png` | 256×256 | 4×4 (64) | 10 | Explosive orange flash puff that turns into grey dissipating smoke specks. | white-yellow centre, orange-red edge, grey; transparent | ADDITIVE | ScreenEffect `muzzleFlash`, legacy `MuzzleFlashExp` |
| `textures/fx/muzzleflash_blue.png` | 256×256 | 4×4 (64) | 8 | Three-pronged (tri-blade) energy flash that grows, then breaks into blue speckle. | light blue/periwinkle; transparent | ADDITIVE | ScreenEffect `muzzleFlash_blue`, legacy `MuzzleFlashBlue` |
| `textures/fx/nukebeamflare.png` | 512×512 | 4×4 (128) | 16 | Circular plasma orb with concentric ring structure, varying each frame. | yellow-gold with white hot centre, orange rim; black | ADDITIVE | `BeamGunMuzzleFlare`, `BeamGunImpactFlare`, ScreenEffect `muzzleFlashNukeBeam` |
| `textures/fx/sonicwave4x4.png` | 1024×1024 | 4×4 (256) | 16 (data files use 14) | Sonic ring: tiny ring → bright thick donut (dark hole in the middle) → breaking up into sparkles → fading. | blue/periwinkle, white highlights; black | ADDITIVE | `SonicShotgunWaves(2)`, `SonicShotgunImpactWave`, ScreenEffect `muzzleFlashSonic` |
| `textures/fx/nukebeam.png` | 128×1088 | 1 column × 17 rows (128×64 each) | 17 (strip animation, 1 frame per 2 ticks) | Horizontal band of turbulent nuclear plasma, a different noise pattern per frame. Dark top and bottom edges, bright centre line. | yellow/white core, orange-brown edges; black | SRC_ALPHA/ONE | [`RenderMovingBeam`](renderers.md#nuclear-death-ray-rendermovingbeam) (beam and spiral) |

## Single images

| Path | Size | Depicts | Palette / alpha | Layout notes | Used by |
|---|---|---|---|---|---|
| `textures/fx/fireball.png` | 64×64 | A single round fireball with a hot white-yellow centre and orange-red flaming rim. | orange/yellow; transparent | Full image | Default texture of a data-file system with no `Texture` key (none in practice); intended for [`RenderFlamethrowerProjectile`](renderers.md#catalogue) (unbound). |
| `textures/fx/blood01.png` | 256×256 | One large dark blood splatter: dense central blotches with spray droplets and streaks. | dark red (84, 10, 11 avg); transparent | Full image (1×1) | legacy `Blood` (unused) |
| `textures/fx/glow.png` | 64×64 | Plain white soft square glow: alpha 255 at the centre falling to 81 at the corners. | white RGB; alpha radial | Full image | [`EntityParticleTrail`](fx-catalogue.md#legacy-named-particles-clientproxyspawnparticle) (legacy `RailgunTrail`, unused) |
| `textures/fx/shockwave.png` | 64×64 | Thin faint ring (circle outline) near the edge of the square. | dim grey-white ring (peak ≈ 15–30/255) on black; RGB | Ring centred, radius ≈ 28 px | All shockwave/ring systems, [`RenderPowerHammerProjectile`](projectiles.md#1-registration), [`RenderSonicShotgunProjectile`](projectiles.md#1-registration), `RenderPiercingProjectile` |
| `textures/fx/bio.png` | 64×64 | Seamless mottled organic/slime texture: greens with white and yellow patches. | green (93, 176, 73 avg); opaque RGB | Tileable (wrapped over the whole model) | BIO death overlay ([`RendererDeadLivingEntity`](death-effects.md#6-dismember-unused)), `RenderPiercingProjectile` |
| `textures/fx/laserDeathfx.png` | 64×64 | Seamless lava/molten-crack texture: bright orange veins on dark red-black. | orange/red/black; opaque | Tileable | LASER death overlay |
| `textures/fx/laser2.png` | 128×64 | Horizontal beam band: bright white-yellow centre line, orange body, dark red-black top and bottom edges, soft noise. | orange/yellow; opaque | u = along the beam (tiles), v = across | **unused** |
| `textures/fx/laser2_a.png` | 128×64 | As laser2, with alpha fading out at the top and bottom edges instead of black. | orange/yellow; alpha 0–250 | as above | [`EntityLaserBeamFX`](fx-catalogue.md#legacy-named-particles-clientproxyspawnparticle) (legacy, unused) |
| `textures/fx/laser3.png` | 128×64 | As laser2, but red: bright pink-white core and red body, dark red edges. | red (197, 75, 59 avg); opaque | as above | [`RenderLaserBeam2`](renderers.md#laser-beam-renderlaserbeam2-rendergenericlaser), [`RenderGenericLaser`](renderers.md#laser-beam-renderlaserbeam2-rendergenericlaser), [`RenderBlasterRifleProjectile`](projectiles.md#1-registration) |
| `textures/fx/laser_blue.png` | 128×64 | As laser2, in blue: white-cyan core, blue body, dark navy edges. | blue; opaque RGB | as above | [`RenderTeslaBeam`](renderers.md#tesla-bolt-renderteslabeam) |
| `textures/fx/heatray.png` | 128×128 | Teardrop-shaped glowing flame (point up), white centre. | yellow/olive-gold; black background | Full image | [`RenderReactionChamber`](../08-machines-production/reaction-chamber.md#7-visuals-renderreactionchamber) ([`08-machines-production`](../08-machines-production/index.md)) |
| `textures/fx/heatray2.png` | 128×128 | Softer teardrop flame. | white→yellow→orange→pink; transparent | Full image | **unused** |
| `textures/fx/lensflare1.png` | 128×128 | Soft round radial glow (no rays). | white→black radial; RGB | Full image | `AlienBlasterTrailGlow`, `BlueBlasterTrailGlow`, `BioTrailGlow` (all tinted) |
| `textures/fx/lensflare2.png` | 128×128 | Soft multi-point star with a bright core. | white on black | Full image | **unused** |
| `textures/fx/lensflare3.png` | 128×128 | Anamorphic streak flare: a thin diagonal streak plus a small star core. | white on black | Full image | `BeamGunMuzzleSparks`, `BeamGunImpactSparks` (tinted orange) |
| `textures/fx/lensflare4.png` | 128×128 | Many-ray star burst with a bright core. | white on black | Full image | **unused** |
| `textures/fx/lightning.png` | 1024×512 | Eight vertical wiggly lightning bolts in 128 px-wide columns, each a thin dark-blue core line with a soft blue glow and faint secondary arcs. | blue; transparent | 8 columns, each full height | **unused** |
| `textures/fx/lightning2.png` | 1024×512 | Eight thick crackling white-blue bolts over a mottled grey-blue rock-like background, with one transparent notch (an artefact) left of centre. | blue/white/grey | 8 columns | **unused** |
| `textures/fx/lightning3.png` | 1024×512 | Eight thin jagged cyan bolts, some forking. | cyan; transparent | 8 columns | **unused** |
| `textures/fx/Testscope.png` | 3072×3072 | Rifle-scope overlay: fully opaque black, with a transparent circular window in the centre (diameter ≈ 974 px, about 32 % of the width) and a thin black cross-hair (1–2 px) through the centre. | black; alpha 0 inside the circle | Full screen quad | ScreenEffect `sniperScope` (scale 2.75) |
| `textures/fx/techscope.png` | 2048×2048 | Sci-fi scope overlay: opaque black border with a soft grey glow around a transparent rounded-rectangle window (≈ 1260 × 910 px). The window edge glows cyan, and inside it are a thin cyan hexagon reticle with a cross-hair, small bracket ticks at the left and right edges, and the text "TECHSCOPE 0.13" in the top-left. | black, cyan (0, 255, 255) lines; alpha 0 window | Full screen quad | ScreenEffect `techScope` (pulse rifle) |

## Projectile entity textures (outside `fx/`, bound by renderers in this area)

These live in `textures/entity/` and `textures/guns/`. The model textures for rocket, grenade and frag are described with their models in [`03-gun-visuals-audio`](../03-gun-visuals-audio/index.md). The `entity/` ones used only by projectiles are listed here for completeness.

| Path | Size | Depicts | Used region | Used by |
|---|---|---|---|---|
| [`textures/entity/bullet.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | 32×32 | Small brass bullet sprite drawn as a horizontal capsule. | Only the top-left 16×5 px (u 0–0.5, v 0–5/32) is sampled. Brass/cream (≈ 220, 210, 155), darker edges, rest transparent. | [`RenderGenericBullet`](projectiles.md#1-registration) |
| [`textures/entity/handgunBullet.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | 16×16 | Round grey stone/lead ball with shading. | Full | `RenderSimpleProjectile` (handcannon) |
| [`textures/entity/bioblob.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | 64×64 | Glossy green slime ball with a lighter highlight. | Full (additive billboard) | `RenderBiogunProjectile` |
| [`textures/entity/BlueBeam.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | 32×32 | Horizontal blue gradient band, white centre line. | top 5/32 of the left half | [`RenderRailgunProjectile`](projectiles.md#1-registration) (unused) |
| [`textures/entity/Laser.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | 32×32 | Horizontal red → orange → yellow gradient band. | top 5/32 of the left half | [`RenderBlasterProjectile`](projectiles.md#1-registration) (unbound) |
| [`textures/entity/launchergrenade.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | 32×16 | UV atlas for the 40 mm grenade OBJ: vertical stripes of cream/off-white, dark red and gold/brass. | per OBJ UVs | `RenderGrenadeLauncherProjectile` |
| [`textures/entity/gore.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | 128×128 | Biped-style skin atlas filled with raw red flesh and gore texture. Only the standard 64 × 64-layout regions used by `ModelGibsBiped` matter. | biped UV layout | `genericGore` gibs ([`RenderFlyingGibs`](death-effects.md#flying-gibs-rendering)) |

## Missing textures referenced in code

| Referenced path | Referenced by | Effect |
|---|---|---|
| `textures/fx/muzzleFlashYellow2x2.png` | [`ScreenEffect.muzzleFlashYellow`](renderers.md#screeneffect-muzzle-flashes-and-scope-overlays) | never drawn |
| `textures/fx/muzzleflashnew.png` | `ScreenEffect.muzzleFlashNew` | never drawn |
| `textures/fx/muzzleflashnew_2_add.png` | `ScreenEffect.muzzleFlashNew2` | never drawn |
| [`textures/items/testItem.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items) (exists) | [`SimpleProjectile`](projectiles.md#31-simpleprojectile) default field | never drawn |
