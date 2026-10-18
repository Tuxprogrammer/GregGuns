# Particle system

Techguns has two particle mechanisms, both client-side:

1. **Data-driven particle systems** (`FXList`, `FXType`, `FXListType`, `ParticleSystemType`, `EntityParticleSystem`, `EntityParticle`). They are defined in [`particles/FXList.txt`](fx-catalogue.md) and [`particles/Nuke.txt`](fx-catalogue.md) and spawned by name. The full catalogue is in [fx-catalogue.md](fx-catalogue.md).
2. **Legacy hard-coded sprites** (`EntityParticleAnimated` plus the [`ClientProxy.spawnParticle(String, …)`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) name switch). This mechanism is marked `@Deprecated` but is still used heavily. Catalogue: [fx-catalogue.md §Legacy](fx-catalogue.md#legacy-named-particles-clientproxyspawnparticle).

It also contains a few special `EntityFX` classes: [`EntityParticleTrail`](fx-catalogue.md#legacy-named-particles-clientproxyspawnparticle), [`EntityBloodEffect`](death-effects.md#blood-burst-entitybloodeffect), [`EntityLaserBeamFX`](fx-catalogue.md#legacy-named-particles-clientproxyspawnparticle), and `EntityMuzzleFlashFX` (fully commented out).

---

## 1. Entry points and networking

| Call | Side | Effect |
|---|---|---|
| `Techguns.proxy.createFX(name, world, x, y, z, mx, my, mz)` | client (CommonProxy no-op) | Looks up `name` (case-insensitive) and spawns one `EntityParticleSystem` per system it contains into `world`, at the position with "system motion" (mx, my, mz). |
| `Techguns.proxy.createFXOnEntity(name, entity)` | client | As above, but each system is attached to `entity`: it follows the entity's position and rotation and dies with it. |
| `Techguns.proxy.createFXOnEntity_ID(name, entity)` | client | As above, and returns the spawned system entity ids. Only commented-out code uses it. |
| `PacketSpawnParticle(name, x, y, z[, mx, my, mz])` | server → client | Fields: name (`short` length + raw bytes in the platform charset), then x, y, z, mx, my, mz (doubles; motion defaults to 0). The handler calls `ClientProxy.createFX` in the local player's world. |
| `PacketSpawnParticleOnEntity(name, entity)` | server → client | Fields: name (`short` length + bytes), entity id (int). The handler looks up the entity and calls `createFXOnEntity`. If the entity is missing it prints "Got Packet for FX … but ent was null". |
| `Techguns.proxy.spawnParticle(name, world, x, y, z, mx, my, mz)` | client | Legacy: builds one hard-coded `EntityFX` and adds it to `Minecraft.effectRenderer`. |
| `ClientProxy.spawnParticle(EntityFX)` (static) | client | Adds a pre-built particle to the effect renderer. |

Notes:
- Lookups go through `FXList.FXList` (a `HashMap<String, FXType>` with lowercase keys). **An unknown name prints `FX '<name>' not found!` and returns null, and the ClientProxy callers then iterate the null list and throw `NullPointerException`.** At least one shipped call (`AntiGravRingGreen`) hits this (bug [PFX-02](index.md#suspected-bugs)).
- Server-side code paths that call `proxy.createFX` or `proxy.spawnParticle` do nothing on a dedicated server, because [`CommonProxy`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) methods are empty. In single-player the integrated server thread *also* has `ClientProxy` as its proxy, so these calls spawn particle-system entities into the **server** world, or push `EntityFX` built from the server world into the client effect renderer from the server thread (bug [PFX-04](index.md#suspected-bugs)).
- Packet handlers run on the Netty thread in 1.7.10 and spawn entities into the client world directly. This is not thread-safe (part of bug PFX-04).
- `FXList.killAllParticles()` is a debug helper. It removes every `EntityFX` from the client world's entity list, which is ineffective because particles are not world entities.

---

## 2. Data-file format (`particles/FXList.txt`, `particles/Nuke.txt`)

Both files are loaded by `FXList.loadFXList()` from `/assets/techguns/particles/`, in the order `FXList.txt` then `Nuke.txt`. Loading is triggered through `proxy.loadFXList()` during mod init. Names are global across files, so a later file may `EXTENDS` an earlier one.

### 2.1 Lexical rules

The file is read with `java.util.Scanner` (English locale). Its token delimiter regex treats all of the following as separators:

- `=` with optional surrounding whitespace
- whitespace followed by `{`
- `//` up to the end of the line (comments)
- CRLF
- any whitespace

So `Key = a b`, `Key =a b` and `Key= a b` all work. A block opens with `Name {` (the `{` must follow whitespace) and closes with a `}` token. Keys are case-insensitive. Values are whitespace-separated numbers, booleans (`true`/`false`) or identifiers. The parser does not recognise unknown keys: it silently consumes only the key token, so any value tokens left behind are then read as keys.

### 2.2 Block types

```
FXLIST <ListName> {
<SystemName>
<SystemName>
...
}

<SystemName> {
<Key> = <values...>
...
}
```

- An **FX list** is an ordered group of system names spawned together at the same position and motion. Members are stored lowercase and resolved at spawn time; unknown members are silently skipped. A list may name another list, which then recurses.
- A **particle system** block defines one emitter (`ParticleSystemType`). On a successful parse it prints `Successfully parsed ParticleSystem <name>`. A file-level summary is printed at the end.
- Parse errors (bad enum value or unknown `EXTENDS` target) print `Error(s) while parsing particle <name> in file '<file>'` followed by the literal word `error`, and abort the *rest of that file*. An `InputMismatchException` (for example a non-number where a number was expected) also aborts the file.

### 2.3 Particle-system keys

All `A B` pairs are uniform random ranges [A, B]. Integer ranges are inclusive. Each particle draws its own value unless noted. A key given twice: the later value wins (`Size`, `AnimationSpeed`, `InitialDelay` and `VelocityDamping` are duplicated in the shipped files).

| Key | Values | Default | Meaning |
|---|---|---|---|
| `EXTENDS` | system name | n/a | Copy settings from a previously parsed system. The copy is partial and buggy, see 2.4. Place it first in the block. |
| `Texture` | resource location | `techguns:textures/fx/fireball.png` | Sprite sheet. |
| `RenderType` | `ADDITIVE`, `ALPHA`, `ALPHA_SHADED`, `SOLID`, `NO_Z_TEST` | `SOLID` | Blending (section 3.4). |
| `Rows` | int | 1 | Parsed, but **unused** by the renderer. |
| `Columns` | int | 1 | Number of grid columns. Also used as the row count (section 3.3). |
| `Frames` | int | 1 | Number of sprites used, numbered left to right then top to bottom. |
| `HasVariations` | bool | false | true: each particle picks one random frame for its whole life. false: frames animate over the particle's life. |
| `RandomRotation` | bool | false | Each particle gets one of 4 UV orientations (0°, 90°, 180°, 270°). |
| `Lifetime` | int int | 20 20 | Particle lifetime in ticks. |
| `SystemLifetime` | int int | 5 5 | How long the emitter keeps emitting (section 3.1). |
| `InitialDelay` | int int | 0 0 | Ticks before the emitter starts. |
| `SpawnDelay` | int int | 1 1 | Pause between bursts. The effective interval is value + 1 ticks (section 3.1). |
| `ParticleCount` | int int | 1 1 | Particles per burst. |
| `AnimationSpeed` | float float | 1 1 | Animation cycles per lifetime. Values above 1 loop. |
| `Size` | float float | 1 2 | Start size. The rendered quad half-width is 0.1 × size blocks. |
| `SizeRate` | float float | 1 1 | Added to size each tick. |
| `SizeRateDamping` | float float | 1 1 | Multiplies SizeRate each tick. |
| `StartSizeRate` | float float | 0 0 | Per-*system*: added to a running "start size" each active tick. Later particles are born larger. |
| `StartSizeRateDamping` | float float | 1 1 | Multiplies StartSizeRate each tick. |
| `Alpha` | value time | none | Alpha key at normalised lifetime `time` (0–1). Repeat for multiple keys, in ascending time. See 3.5 for the quirks. |
| `Color` | R G B time | none | Colour key (0–255 ints) at normalised time. See 3.5. |
| `VelocityType` | `SPHERICAL`, `HEMISPHERICAL`, `OUTWARD`, `ORTHO` | `SPHERICAL` | Initial velocity rule (section 3.2). |
| `Velocity1`, `Velocity2`, `Velocity3` | float float | 0 0 | Velocity parameters, in blocks per **second** (× 0.05 per tick). |
| `VelocityDamping` | float float | 0.98 0.98 | Multiplies velocity each tick. |
| `VelocityDampingOnGround` | float float | 1 1 | Extra damping on ground. Never triggers (section 3.1). |
| `Gravity` | float | 0.02 | Constant downward drift in blocks/tick (section 3.1). Negative values rise. |
| `SystemVelocityFactor` | float float | 0 0 | Fraction of the system's own motion added to each new particle (per tick, not × 0.05). |
| `VolumeType` | `POINT`, `SPHERE`, `HEMISPHERE`, `CYLINDER`, `CYLINDER2`, `TRAIL` | **none** | Spawn-position rule (section 3.2). **Required**: a system without it throws `NullPointerException` when it spawns. |
| `Volume1`, `Volume2`, `Volume3` | float | 0 | Volume parameters in blocks. `Volume3` is unused. |
| `IsHollow` | bool | false | Sphere and cylinder: spawn on the surface (radius = Volume1) instead of inside it. |
| `RemoveOnGround` | bool | false | Kill a particle that touches the ground. Never triggers. |
| `offset` | x y z | 0 0 0 | Local offset added to the spawn position before rotation. |
| `particlesMoveWithSystem` | bool | n/a | Not a key: it is only commented out in the data and has no parser case. |

### 2.4 `EXTENDS` semantics (as implemented)

`extend(parent)` copies, by **reference** for the list and array fields:

- alpha keys and colour keys (list references)
- animation speed, columns, frames, gravity, has-variations, is-hollow, lifetime min/max, random rotation, remove-on-ground, render type, rows, size min/max, size-rate and size-rate-damping min/max, spawn-delay min/max, texture, velocity damping and on-ground damping
- velocity data arrays (references), velocity type, volume data array (reference), volume type, offset, particles-move-with-system

Quirks:
- `initialDelayMax` is set to the parent's **min**. `initialDelayMin` keeps the default 0.
- `particleCountMin`, system lifetime, start-size-rate (and damping) and system velocity factor are **not** copied; they keep their defaults (count min 1, system lifetime 5 5, and so on).
- Because lists and arrays are shared, any `Alpha`, `Color`, `VelocityN` or `VolumeN` line in the child **also changes the parent**. The only shipped child (`NukeExplosionBlastwave`) does not touch those keys.

---

## 3. Runtime behaviour

### 3.1 `EntityParticleSystem` (the emitter)

It is a real `Entity` (registered as [`ParticleSystem`](projectiles.md#1-registration), ID 17, renderer [`RenderDummy`](projectiles.md#1-registration)) but is only spawned client-side. Fields:

- `type`
- `systemLifetime` = rand(SystemLifetime)
- `initialDelay` = rand(InitialDelay)
- `startSizeRate`, `startSizeRateDamping` = rand
- `startSize` = 0
- `spawnDelay` = 0
- its own `ticksExisted` (shadows Entity's) and its own `rotationYaw`/`rotationPitch`
- optional parent `entity`

Spawn data: a boolean "has entity" flag, then the entity id. A copy received from a server (only possible through the single-player bug) has no `type`; it kills itself on its first tick.

Each tick:
1. If `type` is null, die.
2. **Position:** if attached, copy the entity's position (feet) and its `rotationYaw`/`rotationPitch`. Otherwise add the system motion to the position. Free systems drift with the motion passed to `createFX`.
3. If `initialDelay` (post-decremented) is still > 0, stop here.
4. `startSize += startSizeRate`, then `startSizeRate *= startSizeRateDamping`.
5. **Burst:** if `spawnDelay-- ≤ 0`, spawn `count = rand(ParticleCount)` particles, then set `spawnDelay = rand(SpawnDelay)`. Bursts happen on the first active tick and then every SpawnDelay + 1 ticks. SpawnDelay 0 means every tick, 99 effectively means once.
6. **Lifetime:**
   - Attached systems die when the entity is dead, or when `systemLifetime > 0` and `ticksExisted ≥ systemLifetime`. `SystemLifetime 0` therefore means "for as long as the entity lives".
   - Free systems die once `ticksExisted ≥ systemLifetime`. `SystemLifetime 0` or `1` gives exactly one active tick.
7. `ticksExisted++`. The counter only advances after the initial delay.

**Per particle *i* of *count* in a burst:**
1. `dir` = zero vector.
2. `pos` = `volume.getPosition(system, dir, i, count)`. This may also set `dir` to the spawn direction. Add `offset`.
3. `vel` = `velocityType.getVelocity(system, dir)`.
4. Rotate `vel` (and `pos`, unless the volume is TRAIL) into world space: first about the local Z axis by `pitch`, then about Y by `−90 − yaw`, with degrees converted to radians. The helper rotations are `rotZ(a): x' = x·cos a + y·sin a, y' = y·cos a − x·sin a` and `rotY(a): x' = x·cos a + z·sin a, z' = z·cos a − x·sin a`. With these, the local axes map as follows:
   - Local **+X** becomes the look direction of an entity with that yaw and pitch (vanilla entity convention, for example yaw 0 → +Z, yaw 90 → −X, positive pitch tilts down).
   - Local +Y stays roughly up.
   - Free systems have yaw = pitch = 0, so their local +X is world +Z. All shipped free effects are rotationally symmetric about Y, so this does not show.
   - Projectiles store yaw in the *projectile* convention (`atan2(mx, mz)`), which mirrors X. For systems attached to projectiles, local +X therefore does not follow the flight direction. All shipped projectile trails use TRAIL volumes (unrotated) and direction-less velocities, so this has no visible effect. `BeamGunMuzzleFX` is attached to [`MovingBeam`](fx-catalogue.md#triggers-which-game-event-spawns-which-fx), whose yaw is the shooter's head yaw, so there the beam forward axis is correct.
5. Spawn `EntityParticle` at system position + pos, with initial motion = vel × **0.05** (per-second to per-tick).

### 3.2 Volume and velocity rules

| VolumeType | Position (local) | `dir` set to |
|---|---|---|
| `POINT` | (0, 0, 0) | unchanged (zero) |
| `SPHERE` | Uniform direction on the unit sphere (y uniform in [−1, 1], azimuth uniform) × r, where r = Volume1 if hollow else uniform [0, Volume1] | that unit direction |
| `HEMISPHERE` | As SPHERE with y uniform in [0, 1] (upper half) | that unit direction |
| `CYLINDER` | Horizontal unit vector at a random azimuth × r (as above), with y uniform in [0, Volume2]. A vertical cylinder from the base upward. | the horizontal unit radial vector |
| `CYLINDER2` | x uniform in [0, Volume2] along local X, with (y, z) = a unit vector rotated about X at a random angle × r. A cylinder along the forward axis. | that radial vector in the YZ plane |
| `TRAIL` | Requires an attached entity. Position = −normalised(entity motion) × (i / count) × Volume1. Particles are spread evenly along the segment behind the entity. **Not rotated.** With no entity it returns the system's absolute position as an offset (bug, unused). | unchanged (zero) |

| VelocityType | Velocity (local, per second) |
|---|---|
| `SPHERICAL` | `r = rand(Velocity1)`. If `dir` is non-zero: dir × r. Otherwise a uniform random direction on the sphere × r. |
| `HEMISPHERICAL` | As SPHERICAL, but the random direction is in the upper hemisphere. |
| `OUTWARD` | `r = rand(Velocity1)`, `y = rand(Velocity2)`. If `dir` is non-zero: (dir.x × r, y, dir.z × r). Otherwise a random horizontal direction × r, with Y = y. Negative r points inward. |
| `ORTHO` | (rand(Velocity1), rand(Velocity2), rand(Velocity3)) in local axes. Velocity1 is "forward". |

Each velocity function creates a fresh unseeded `java.util.Random`.

### 3.3 `EntityParticle` (one particle)

At birth:
- `lifetime` = rand(Lifetime). `size` = rand(Size) + the system's current `startSize`.
- `sizeRate`, `sizeRateDamping`, `animationSpeed`, `velocityDamping`, `velocityDampingOnGround` and `systemVelocityFactor` = rand each.
- Motion += `systemVelocityFactor` × system motion.
- The stored velocity is set to the motion.
- `variationFrame` = rand(0..Frames−1).
- If `RandomRotation`, an orientation 0–3.
- `noClip = true`.

Each tick:
1. Age +1. Die when age ≥ lifetime (checked before the increment).
2. Motion = stored velocity, then `motionY −= Gravity`. Move by setting the position directly (no collision).
3. `velocity *= VelocityDamping`.
4. On ground: extra damping, and removal if `RemoveOnGround`. **Because the position is set directly, `onGround` is never true, so these never fire** (bug [PFX-17](index.md#suspected-bugs)).
5. `size += sizeRate`, then `sizeRate *= sizeRateDamping`.

Note: Gravity does **not** accumulate. It is subtracted from a velocity that is re-read every tick, so the particle sinks at a constant `Gravity` blocks/tick on top of its damped velocity.

Rendering (`getFXLayer() = 2`, render pass 1):
- `progress = (age + partialTick) / lifetime`.
- **Frame:** `variationFrame` if `HasVariations`; otherwise `floor(Frames × progress × animationSpeed) mod Frames`.
- **UV cell:** column = frame mod Columns, row = frame div Columns. The cell size is 1/Columns × 1/**Columns** (rows are ignored). Sheets must be square grids, or have `Rows ≤ Columns` with the same cell aspect.
- **Quad:** a camera-facing billboard (vanilla particle basis vectors) of half-size `0.1 × size`, where size is interpolated between the previous and current tick. With `RandomRotation`, the UV corners are permuted to rotate the sprite by a multiple of 90°.
- The particle flushes the effect-renderer tessellator, binds its own texture, draws one quad, rebinds `textures/particle/particles.png` and restarts the batch. That is one draw call per particle.

### 3.4 Render types

| RenderType | Blend | Lighting | Depth |
|---|---|---|---|
| `ADDITIVE` | SRC_ALPHA, ONE | full-bright | normal |
| `ALPHA` | SRC_ALPHA, ONE_MINUS_SRC_ALPHA | full-bright | normal |
| `ALPHA_SHADED` | blend on, function unchanged (effectively alpha) | world-lit (not full-bright) | normal |
| `SOLID` | none | full-bright | normal |
| `NO_Z_TEST` | SRC_ALPHA, ONE | full-bright | depth test and depth writes **off** (visible through walls) |

### 3.5 Alpha and colour curves

Keys are searched in file order. Let `prev` be the last key with `time ≤ progress` and `next` the first key with `time > progress`.

- Segment weight `p = (progress − prev.time) / (next.time − prev.time)`.
- **Colour:** both keys are converted to HSB. The particle colour is `HSB(prev × p + next × (1 − p))`, converted back to RGB. Note the **swapped weights**: at the start of a segment the colour equals the *later* key and it approaches the *earlier* key by the end of the segment.
- **Alpha:** the stored value is `1 − Alpha` from the file, interpolated with the same swapped weights. Within a segment from key (α₁ at t₁) to key (α₂ at t₂), the displayed alpha runs linearly from **1 − α₂** at t₁ to **1 − α₁** at t₂. When α₁ + α₂ = 1, as in the common `0 → 1 → 0` envelopes, this happens to equal the intended α₁ → α₂ curve. Otherwise it differs. For example, `0@0, 0@1` (TeslaFlare) displays as fully opaque.
- If `progress` lies before the first key (first key time > 0), or after the last key, or there is only one key, then prev = next and `p = 0/0 = NaN`. The colour and alpha become NaN, which renders as black or undefined. Every shipped system has its first key at time 0 and its last key at time 1, so this never happens with the shipped data.
- Hue is interpolated linearly with no wrap-around.

Effective per-system curves are tabulated in [fx-catalogue.md](fx-catalogue.md#particle-systems-alpha-and-colour-keys).

---

## 4. Legacy sprite particle: `EntityParticleAnimated`

`@Deprecated`, but used by `ClientProxy.spawnParticle`, `EntityBloodEffect` and [`FlyingGibs`](projectiles.md#319-flyinggibs).

Constructor `(world, x, y, z, scale, textureFileName, cols, rows, numSprites, renderType)`:
- Texture path `techguns:textures/fx/<textureFileName>`.
- `particleScale = scale`, white colour, `noClip`, gravity 0.
- `maxAge = 10 + rand(0..4)`.

Setters. "Random" means a value drawn with `v × (1 − R) + 2 × v × R × rand`, where R is the `randomness` *at the time of the call* (default **0.20**):

| Setter | Effect |
|---|---|
| `setMaxAge(n)` | maxAge = round(random(n)) |
| `setGravity(g)` | motionY −= 0.04 × g per tick (accumulating, vanilla style) |
| `setScaleIncrease(min, max, factor, soft)` | Scale grows from random(min) to random(max). Applied scale = `min(minScale + (max − min) × p / factor, maxScale)` with p = progress, or √progress if `soft`. The initial scale is min. |
| `setDynamicColor(R1, G1, B1, A1, R2, G2, B2, A2, type)` | 0–255 values. Interpolates RGBA from the first set to the second over the life. |
| `setDynamicColorStart(s)` | The colour progress stays 0 until life fraction s, then is rescaled to [s, 1] → [0, 1]. |
| `setRandomRotation(true)` | One of 4 UV orientations. |
| `setRandomRotation(min, max)` | Stores an angle that is never used. Deprecated. |
| `setRandomness(R)` | Affects later random() calls only. |
| `setMotionDamping(d)` | Motion multiplier per tick (default 0.98). |
| `setDelay(n)` | Invisible and frozen for n ticks. |
| `setFramesAsVariations(true)` | Choose one random sprite for the whole life. |

Dynamic colour types (applied to the colour progress c ∈ [0, 1]):

| Type | Curve |
|---|---|
| `LINEAR` | c |
| `TENT` | 2c, folded back above 1 (up then down) |
| `SINUS` | sin(2πc), which goes negative in the second half |
| `SMOOTH` | sin(πc) |
| `FAST` | √c |
| `NONE` | colour not updated (default) |

Update:
- Skip while delayed.
- Age; die at maxAge.
- motionY −= 0.04 × gravity.
- `moveEntity` (noClip, so no collision).
- Multiply motion by damping. On ground multiply X and Z by 0.7.
- Update the dynamic colour.

Render:
- Frame = floor(numSprites × age / maxAge), or the fixed variation frame.
- Draw only while frame < numSprites.
- UV cell = 1/cols × 1/rows (rows are honoured here, unlike `EntityParticle`).
- Billboard half-size 0.1 × scale.
- Blend per render type: ADDITIVE (SRC_ALPHA, ONE); ALPHA (SRC_ALPHA, 1 − SRC_ALPHA); alpha test on. Full-bright except ALPHA_SHADED.

---

## 5. Other `EntityFX` classes

| Class | Behaviour |
|---|---|
| `EntityParticleTrail(world, x, y, z, item, r, g, b, scale)` | A vanilla-motion particle tinted (r, g, b), scale × the vanilla random scale, no gravity, noClip. It always renders [`textures/fx/glow.png`](textures.md#single-images) as an alpha-blended billboard of half-size 0.1 × scale. The item icon argument is set but not drawn. Used only by the legacy `"RailgunTrail"` name (diamond icon, colour (0.1, 0.5, 1.0), scale 0.5), which nothing calls. |
| `EntityBloodEffect` | An invisible, 5-tick emitter used by the gore death. See [death-effects.md](death-effects.md#blood-burst-entitybloodeffect). |
| `EntityLaserBeamFX(world, source xyz, target xyz)` | Experimental. Draws a quad from source to target using [`textures/fx/laser2_a.png`](textures.md#single-images), width from the viewer cross product, alpha-blended. It prints debug text **every frame** ("I AM A NEW LASERBEAM HARRRRRRR!" in the constructor, plus position dumps). Only the legacy name `"LaserBeam"` creates it, and the only caller is commented out. Its quad vertex order and world-space coordinates are wrong (no interpolation offset), so it would not render correctly. |
| `EntityMuzzleFlashFX` | The whole file is commented out (a vanilla flame-like particle sketch). |
