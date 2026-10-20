# Renderers, models and effects

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Paths below are relative to it. Everything here is client-only (`@SideOnly(Side.CLIENT)` or loaded through the client proxy). The renderers for red matter (`content/blast/redmatter/render/RenderRedmatter`) are specified in [02-explosives/red-matter.md](../02-explosives/red-matter.md) and only linked here.

All render registration is *1.12-only mechanism* (Forge `ModelRegistryEvent`, `RenderingRegistry`, `ClientRegistry`, item mesh definitions, state mappers). This sheet describes what each renderer draws. A 1.7.10 rebuild replaces the registration with `RenderingRegistry.registerEntityRenderingHandler`, `ClientRegistry.bindTileEntitySpecialRenderer` and `IItemRenderer`, and rebuilds the JSON models as Java geometry (see [models.md](models.md)).

## 1. Registration (`client/ClientReg`)

`ClientReg` subscribes to the client event bus. It does three things.

1. **Model locations.** For each simple block or item it binds the item to the model named after the block or item registry key, with a variant string (`inventory` for nearly everything, `inventory_0` and `inventory_1` for the two EMP tower metas, `render=2d` and `render=3d` for the balloon). Blocks with sub-types add a suffix to the registry key: spikes `_poison` and `_fire`, concrete `_compact` and `_reinforced`, radioactive `_dirt` and `_stone`. Crafting items (ingot, clump, plate, circuit, wire) bind each sub-item to the variant `name=<sub item>` of the shared blockstate file; they are skipped if their config flag disables the item.
2. **Explosive-driven models.** Five registries (explosive blocks, grenades, bomb carts, missiles, bomblets) get a map from explosive data to model location, built by looping every registered explosive. The model path is `<domain>:explosives/<key>`, `grenades/<key>`, `bombcarts/<key>`, `missiles/<key>`, and one fixed `icbmclassic:explosive_bomblet` for bomblets. A custom mesh definition (`ItemModelMapperExplosive`) picks the model for a stack, with a fallback (see section 5). Missiles also register `missiles/missile` as the fallback and as the model for the missile module. The SAM, cluster and held-item missiles are bound directly to `missiles/surface_to_air`, `missiles/cluster` and `missiles/held_item`.
3. **Renderers.** The entity and tile renderer bindings are the tables below. `RenderMissile` is built by a factory that also stores the instance in a static field, so the launcher tile renderers can reuse it to draw a stored missile.

It also registers a **block color handler** for the EMP tower (section 4).

### Entity renderer bindings

| Entity | Renderer | Section |
|---|---|---|
| `EntityExplosive` (primed explosive block) | `RenderExBlock` | 2.1 |
| `EntityRedmatter` | `RenderRedmatter` | linked, 02-explosives |
| `EntityFlyingBlock` | `RenderEntityBlock` (in `content/entity/flyingblock/`) | 2.2 |
| `EntityExplosion` (invisible blast host) | `RenderExplosion` | 2.3 |
| `EntityGrenade` | `RenderAsItem` with the grenade's own stack, billboard on | 2.4 |
| `EntityParachute` | `RenderParachute` (in `content/cargo/parachute/`) | 2.5 |
| `EntityBalloon` | `RenderAsItem` with the balloon's render stack, no billboard | 2.4 |
| `EntityLightBeam` | `RenderLightBeam` | 2.6 |
| `EntityFragments` | `RenderFragments` | 2.7 |
| `EntityBombDroplet` | `RenderBombDroplet` (in `content/cluster/bomblet/`) | 2.8 |
| `EntityPlayerSeat` | `RenderSeat` | 2.9 |
| `EntitySmoke` | `RenderSmoke` | 2.9 |
| `EntityMissile` (and subclasses: SAM, cluster, held-item) | `RenderMissile` | 2.10 |

Chickens get an extra render layer (section 3).

## 2. Entity renderers

### 2.1 `RenderExBlock` (primed explosive)

Draws the explosive block model for the entity's explosive type, standing upright, so a primed block looks exactly like the placed one. Each frame it builds the block state with that explosive and the "up" rotation (the TODO in the source says the direction should come from the entity rotation, which is not implemented), moves to the block center, and renders the model with the entity's brightness.

Two fuse effects:

- **Swell.** In the last 10 ticks of the fuse the model scales up. The scale is `1 + 0.3 * f^3` where `f` ramps from 0 to 1 over those ticks, so the block grows by up to 30%.
- **Flash.** While `fuse / 5` is even (integer division), a white translucent copy of the block is drawn on top with polygon offset. Its alpha is `0.8 * (1 - (fuse - partial + 1) / 100)`. The overlay therefore flashes in 5-tick on/off pairs, and is fainter the longer the fuse still has to run; for fuses above 100 ticks the alpha is negative and the engine clamps it to zero.

No texture of its own; it binds the block atlas.

### 2.2 `RenderEntityBlock` (flying block)

Draws the block state carried in the entity's block data, upright and centered, lit by the entity brightness, with a 0.5 shadow. The renderer guards against third-party blocks whose models throw during rendering: on the first exception it logs an error once and remembers the failing state. After that it substitutes a leaves block (if the failed block's material is leaves) or stone, so the flying block is still visible.

### 2.3 `RenderExplosion`

Draws nothing. `doRender` is empty, and the grey texture it names is never bound. It exists only because every entity needs a renderer.

### 2.4 `RenderItemImp` and `RenderAsItem` (the shared item-style renderer)

`RenderItemImp<E>` is the base for all entities that look like an item model. It renders an item stack with the baked item model through the vanilla item renderer, in the style of a dropped item.

- Lookup: the stack comes from an abstract `getRenderItem(entity, index)`. If it is empty, a fire block is used as a visible placeholder.
- Pipeline per call: set up blending and standard item lighting, push the matrix, translate (default: entity position raised by a quarter of the model's ground-scale Y), rotate (default: entity yaw around Y, or, if the billboard flag is on, so the model always faces the camera), scale (default none), push another matrix, apply the model's camera transform for the transform type returned by `getTransformType(index)` (default `NONE`), draw, pop, then restore state and the texture blur setting. Team-color outline mode is honored when the renderer is asked to draw outlines.
- Hooks (`translate`, `rotate`, `scale`, `getYaw`, `getPitch`, `getTransformType`) are overridden by subclasses. Yaw is already interpolated by the render manager; pitch is interpolated in `doRender`.
- Shadow: size 0.15, opacity 0.75.
- The model lookup runs every frame; the source notes a caching TODO.

`RenderAsItem<E>` is the concrete version that takes a function from entity to stack. It is used for grenades (billboarded, so the flat grenade icon always faces the player) and balloons (not billboarded; the stack is the balloon's own render stack, which `ClientReg` points at the "3d" balloon variant).

### 2.5 `RenderParachute`

A `RenderItemImp` that renders the parachute's render stack. It always faces the camera (billboard rotation, with a TODO for a wind wiggle) and scales uniformly by the entity's render scale, (the scale value is owned by the entity, see 03-missiles/cargo.md).

### 2.6 `RenderLightBeam` (laser designator beam, EMP/target beams)

Draws a vertical beacon-style beam from just below the entity up to world height 255, reusing the vanilla beacon beam texture. It is not tied to a tile.

- **Growth.** The entity holds a server-synced beam progress and a client-only progress. Each frame the client value moves toward the server value by `growthRate * partialTicks` (growth rate default 0.05) and clamps at the target. This is the animation by which the beam extends and retracts; the speed depends on frame rate (bug ICBM-07-07).
- **Size.** Core radius is progress times the entity's `beamSize` (default 0.5); glow radius is progress times `beamGlowSize` (default 1.0).
- **Geometry.** Two square tubes of four faces each, centered on the block (0.5, 0.5) offset. The origin is placed 5 blocks below the entity (hard-coded; the TODO says to ray trace the ground instead), and the height is `255 - floor(entity Y)`.
- **Core tube**: opaque (blending off, depth write on), vertex alpha 0.125, texture scrolling upward. The scroll value is `frac(time * 0.2 - floor(time * 0.1))` where `time = ticksExisted + partial`. Texture V runs from `-1 + scroll` over `height` units, so it tiles once per block.
- **Glow tube**: drawn after, with blending on and depth write off, no scroll, same vertex alpha.
- Color is the entity's RGB. Lighting and culling are disabled for the draw, and fog is turned off around the whole render and back on after.

### 2.7 `RenderFragments`

Two looks, chosen by the entity's anvil flag.

- **Normal fragment** (shrapnel, an arrow-like sliver): draws vanilla-arrow-style geometry using the 32 x 32 `entity/fragments/fragment.png`. It is two end caps plus four long faces (a cross-shaped 4 x 4 shaft, 16 units long) at scale 0.05625, rotated to the interpolated yaw (minus 90) and pitch, then 45 degrees about its own axis. If the arrow shake counter is positive (the fragment has just stuck in something) the shaft wobbles with an amplitude of `-sin(3 * shake) * shake` degrees. Lighting is off while drawing.
- **Anvil fragment**: draws a vanilla anvil block model at the entity position, using the entity brightness. The anvil damage state and facing are chosen with `world.rand` every frame (ICBM-07-05, 07-06).

### 2.8 `RenderBombDroplet` (bomblet and droplet)

Draws the droplet's own item stack as a small missile-shaped model. It reuses the deprecated `RenderEntityItem2` by feeding it a fake item entity (with a null world) each frame. Transform: translate up by 0.15 (center of the collider), rotate Y by `yaw - 180` and X by `pitch + 90` (so the model's tip points along travel), then translate 0.2 up to the model's rotation point. Texture bind is `entity/fragments/fragment.png`, which is not used by the model itself.

`RenderEntityItem2` (`client/render/entity/`) is a copy of vanilla's dropped-item renderer with a configurable camera transform type. It is marked deprecated in favor of `RenderItemImp` and is used only here.

### 2.9 `RenderSeat` and `RenderSmoke`

Both draw nothing visible. They return a null texture and call only the base class (shadow size 0). `EntityPlayerSeat` is the invisible seat a player rides on (cruise launcher aiming, cargo); `EntitySmoke` is a server entity whose smoke is spawned as particles elsewhere.

### 2.10 `RenderMissile` (missile entities and missile items in the world)

A `RenderItemImp<EntityMissile>`. The missile is drawn as an item model: the stack comes from `entity.toStack()`, which is why the missile model is chosen by explosive type (via the mesh definition in section 5) and by nothing in this class. The tier look is in the JSON model (hull texture `tier1`, `tier2` or `tier3`, see [models.md](models.md#modelsitemmissiles-27)).

Transforms (for the entity case):

1. Translate to the entity position, raised by 0.2 (the center of the collider). The TODO says to move this offset into the JSON model.
2. Yaw is the entity yaw minus 180. Pitch is the entity pitch minus 90. Both are corrections for the model being authored pointing up.
3. Rotate Y by yaw, then X by pitch.
4. Translate down 0.8 along the rotated axis, to the model's rotation point (also a TODO for JSON).

The same class is reused when no entity exists: `renderItem(stack, world, x, y, z, yaw, pitch, partial)` is called by the launcher tile renderers. In that case no 0.2 raise and no 0.8 offset are applied; the caller supplies position and angles.

**Held-item missile.** A missile that carries an item draws two models: the missile (index 0), then the held item (index 1). The held item uses the third-person-right-hand camera transform, is rotated 10 degrees more in pitch than the missile, and is moved forward to sit on the missile's shoulder (offset 0 / 0.27 / -0.12 in the entity case, 0 / 1.1 / -0.27 when drawn from the item stack with no entity). Items that are not "full 3D" tools (`isFull3D` false) are scaled to 0.5. The source marks the full-3D test as a placeholder for a config list.

**Debug.** With the client bounding-box debug overlay on (F3+B), it draws a green line from the missile along twice its motion vector. The alpha value in the source is a typo, 2555 (ICBM-07-09).

`RenderMissile.INSTANCE` is the static instance assigned at registration; `TESRLauncherBase` and `TESRCruiseLauncher` call it.

## 3. Chicken helmet layer (`client/render/entity/layer/LayerChickenHelmet`, `client/models/ModelChickenHelmet`)

A layer added to the vanilla chicken renderer at client init (it is added only if the chicken renderer is the stock one). If the chicken has the "space chicken" capability set (a chicken that has ridden a missile), the layer draws a small box "helmet" over the head using `entity/space_chicken.png` (64 x 32).

- Model: a single 6 x 7 x 6 box at head height; its pivot is at (0, 15, -4), the chicken head position. The head pitch and yaw rotate it to follow the head; for baby chickens it is shifted down and back (5 and 2 pixels) to match the baby head.
- Render state: alpha blending on, standard blend function, alpha test lowered to near zero, then blending off. The alpha test is not restored afterwards (ICBM-07-32).
- Trigger: `CapSpaceChicken` (a capability set when a missile collides with a chicken; the missile also plays the `meep` sound, see [sounds.md](sounds.md)).

## 4. Tile entity renderers

### TESRLauncherBase (`content/blocks/launcher/base/`)

Draws the missile sitting on a launcher pad. It does nothing for an empty pad. The missile is drawn by `RenderMissile.renderItem` at the block center plus a 2-block offset along the launch direction (so the 2-block-tall missile stands on the pad face). Yaw and pitch come from the tile's `getMissileYaw(true)` and `getMissilePitch(true)`: yaw 0 for north and for up and down, 180 south, 90 west, -90 east; pitch 0 for up, -180 for down, -90 for horizontal launch directions. The offset is a constant with a TODO to drive it from the model. See [04-launch-control/launcher-base.md](../04-launch-control/launcher-base.md).

### TESRCruiseLauncher (`content/blocks/launcher/cruise/`)

Draws the moving top of the cruise launcher and its loaded missile. See [04-launch-control/cruise-launcher.md](../04-launch-control/cruise-launcher.md) for aiming.

- **Top model.** Texture `models/cruise_launcher_top.png` (64 x 64), model `CruiseLauncherTopModel`. The renderer moves to the block center, 2.5 blocks up, and rotates the model 180 degrees about Z (the model is evidently authored upside down). It passes the negated aim yaw and pitch (converted to radians) into the model.
- **Model structure** (`CruiseLauncherTopModel`, authored in Blockbench; a `ModelICBM` with a `render(scale, yaw, pitch)` method; texture 64 x 64): a `yaw` root part (a 16 x 2 x 6 base plate) rotates about Y; three children hang off it. A `pitch` child (two side cheeks 2 x 5 x 14 and two cross braces) rotates about X: this is the cradle that tilts with the aim pitch. Two identical `arm` children (a 2 x 5 x 6 head and two 2 x 10 x 2 legs each) stand at the plate's two ends, the second offset 14 units. Scale 0.0625.
- **Missile.** If the tile has a cached missile stack, it is drawn at the block center plus 2 up with the aim yaw and `pitch - 90`.
- No partial-tick interpolation is applied to the aim, so it steps at server tick rate (ICBM-07-11).
- A disabled `debugMissileRotations` method draws a sphere of missiles at 8 x 8 yaw and pitch steps, for tuning; it is not called.

### TESREmpTower (`content/blocks/emptower/`)

Bound to both the tower tile and the extender tile (`TileEmpTowerFake`). It draws the spinning coil and, when charged, the electric arcs. This is the only rotating-part renderer in ICBM: **the radar station has no tile renderer and no rotating dish**; its look is a static cube whose front face swaps between four screen textures by state (see [models.md](models.md), `radarstation.json`).

- **Rotation.** For the tower tile the angle comes from the tile's `rotation` and `prevRotation` fields (the tile advances the angle each tick by charge fraction times 15 degrees, so the coil spins faster the more charged it is, and the renderer interpolates with the partial tick). For an extender tile it reads the host tower's values. Extenders at an odd height above the host add 45 degrees so the stacked coils interleave.
- **Coil.** Draws the `COIL` block state (`emp_tower_coil` model) at the block center, rotated about Y, with lighting off and full brightness.
- **Arcs.** Only if the tower's charge fraction exceeds 0.2: draws the `ELECTRIC` block state (`emp_tower_electric`, the animated 16 x 64 texture, translucent render layer) with the same rotation, blending on, alpha 0.5, and the tint from the color handler below.
- The extender and tower both use the same renderer; extenders without a host draw with rotation 0 and no arcs.

**Block color handler** (`ClientReg.registerBlockColor`): a tint for the EMP tower (tower and extender). The red channel is `cos(charge) * 255` and the blue channel is `sin(charge) * 255`, with charge the 0 to 1 fraction used as radians, so the tint shifts from red (empty) toward purple-blue (full) but only reaches about 138 red / 214 blue at full charge (ICBM-07-10). Green is 0.

## 5. Mappers and registration helpers (`client/mapper/`, `client/SpikeMeshDefinition`, `client/ICBMCreativeTab`)

- `ItemModelMapperExplosive` (an item mesh definition): given a stack, look up the explosive on the stack; if the explosive and its model are present return that model, otherwise return the fallback model. The fallback is the condensed model for grenades, carts, bomblets and blocks, and `missiles/missile` for missiles.
- `BlockModelMapperExplosive` (a state mapper): given a block state of the explosive block, read the explosive property and the rotation property, look up the model for that (explosive, facing) pair, and return the fallback (the condensed model facing up) when anything is missing.
- `SpikeMeshDefinition`: an item mesh definition that picks `spikes`, `spikes_poison` or `spikes_fire` by damage value 0, 1, 2. **It is never initialized**; the spikes item models are bound through `ClientReg.newBlockModel` instead (ICBM-07-03).
- `ICBMCreativeTab`: orders the creative tab: launcher base, screen, frame, connector, cruise launcher, EMP tower, radar station; concrete, reinforced glass, spikes; rocket launcher, ballistic launcher, radar gun, remote detonator, laser designator, defuser; explosive missile, SAM, held-item missile, cluster missile; grenade, explosive block, bomb cart; then any other item that claims the tab. Within each item, sub-stacks are sorted by explosive tier (ascending), then by registry ID. Non-explosive stacks keep their order. The tab icon is the explosive missile item.

## 6. Particles and effects

Spawning goes through the proxy. The common proxy does nothing; `ClientProxy` implements the following.

| Proxy method | Effect | Callers |
|---|---|---|
| `spawnMissileSmoke` | Per tick, when the missile config flag for engine smoke is on: 10 smoke particles behind the missile (offset 1.2 blocks along its axis), velocity opposite to missile motion times 0.5, color from the flight logic's engine smoke RGB, size `6 / (1 + posY / 100)` truncated to an int (min 1), life 100 ticks. The intent was a small random jitter per particle, which is a no-op (ICBM-07-01). | `EntityMissile` engine tick |
| `spawnPadSmoke` | When the launch-smoke config flag is on: 10 `ParticleLauncherSmoke` particles at the missile base (1.2 below the entity), velocity in a downward cone (horizontal spread +/-0.3, down speed 0.5 to 1.0), white, size 1 to 3, life 180 ticks. | warm-up flight logic and old ballistic flight logic |
| `spawnAirParticle` | One `ParticleAirICBM` of the given color, size and life. | the `PacketSpawnAirParticle` packet, gas blasts |
| `spawnExplosionParticles` | For one destroyed block: a random point inside the block, a direction away from the blast center, a speed `0.5 / (distance / blastScale + 0.1)` times a random factor of 0.3 to 1.3 (the product of two random numbers plus 0.3), one vanilla explosion particle at the midpoint between the point and the center, and one vanilla smoke particle at the point. | `PacketSpawnBlockExplosion` |
| `spawnSmoke` | A `ParticleSmokeICBM` at a position. No callers (ICBM-07-04). | none |

Particle classes (`client/fx/`):

- `ParticleSmokeICBM`: vanilla smoke particle with a settable life and color. Setting a color with the variant flag multiplies all three channels by one random value in 0 to 0.9, so smoke varies from dark to the base color. Not used (see above).
- `ParticleAirICBM`: vanilla smoke particle with its upward acceleration removed: it drifts with the velocity given, decays by 4% a tick, speeds up horizontally by 10% a tick while not climbing (a quirk copied from vanilla smoke), slows by 30% a tick on ground, and animates through the 8 smoke texture frames over its life. Same color variant behavior.
- `ParticleLauncherSmoke`: the same but collides with the world using its own collision routine. `ParticleCollisionLogic.getCollisionBoxes` walks every block in the bounding box expanded by the move vector (plus one on each side, skipping the top corner and edge cells, and unloaded columns) and collects collision boxes for blocks passing a filter. The filter excludes the launcher pad, launcher frame ("support") and launcher screen blocks, so the smoke passes through the launcher it came from, plus anything in a public ignore set that nothing ever fills (ICBM-07-04). Unlike vanilla, a blocked downward move does not zero the vertical velocity (ICBM-07-33).
- `FXAntimatterParticle`: a vanilla-style particle with random dark gray color, fixed light level 240, life of `10 / (0.2 to 1.0)` ticks times its scale, and no movement update. No code creates it (ICBM-07-04).

## 7. `ICBMSounds`

The sound enum is specified in [sounds.md](sounds.md).

## Coverage

Read in full: `client/ClientProxy`, `ClientReg`, `ICBMSounds`, `ICBMCreativeTab`, `SpikeMeshDefinition`, `fx/` (5), `mapper/` (2), `models/` (3), `render/entity/` (11 files including `item/` and `layer/`). Also read in full, because they hold renderers the client folder registers: `content/blocks/emptower/TESREmpTower`, `content/blocks/launcher/base/TESRLauncherBase`, `content/blocks/launcher/cruise/TESRCruiseLauncher`, `content/cargo/parachute/RenderParachute`, `content/cluster/bomblet/RenderBombDroplet`, `content/entity/flyingblock/RenderEntityBlock`. Not read in detail: `RenderRedmatter` (linked).
