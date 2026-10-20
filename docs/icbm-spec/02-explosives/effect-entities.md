# Effect entities

The entities that blasts spawn or run inside: the **flying block**, **fragments**, the **smoke** marker, the **light beam**, the **explosion controller** and the **primed explosive**. The grenade, bomb cart and player seat entities belong to `03-missiles`. The blasts that use these are in [blast-types.md](blast-types.md), [gas-and-status.md](gas-and-status.md) and [radioactive-and-mutation.md](radioactive-and-mutation.md).

Source root for all paths: `src/main/java/icbm/classic/` at `9c1d985a`. Entity registration through `EntityEntryBuilder`, `DataParameter` and `IEntityAdditionalSpawnData` is Forge 1.12 plumbing (1.7.10 has the same ideas under other names); the sheets describe behavior.

## 0. Registry summary (`content/reg/EntityReg.java`)

| Registry name | Class | Tracking range | Update frequency | Size (w x h) | Sync notes |
|---|---|---|---|---|---|
| `icbmclassic:block.gravity` | `EntityFlyingBlock` | 128 | 1 | 0.98 x 0.98 | velocity updates on; spawn data carries the block |
| `icbmclassic:block.fragment` | `EntityFragments` | 40 | 1 | 0.5 x 0.5 | spawn data: explosive and anvil flags |
| `icbmclassic:block.explosive` | `EntityExplosive` | 50 | 5 | 0.98 x 0.98 | spawn data: fuse, facing, stack |
| `icbmclassic:holder.explosion` | `EntityExplosion` | 100 | 5 | 0.98 x 0.98 | spawn data: explosive id and Y offset |
| `icbmclassic:beam.light` | `EntityLightBeam` | 80 | 5 | 1 x 1 | spawn data: color and sizes; one synced float for progress |
| `icbmclassic:smoke` | `EntitySmoke` | 100 | 15 | 0.1 x 0.1 | spawn data: color and time to live |

Tracking arguments are `(range, frequency, sendsVelocity = true)`. The `block.*` names, `holder.*`, `beam.light` and `smoke` are never renamed to the vanilla underscore style (a TODO in `ICBMEntities`).

## 1. Flying block (`EntityFlyingBlock`, `FlyingBlock`, `BlockCaptureData`)

A block turned into a projectile that carries its block state and tile data and puts them back when it lands. Used by the anti-gravitational, beam and sonic blasts, and by the red matter (part of this area, not here). Files: `content/entity/flyingblock/EntityFlyingBlock.java`, `FlyingBlock.java`, `BlockCaptureData.java` (the renderer `RenderEntityBlock.java` is `07-visuals-audio`).

### Spawning (`FlyingBlock`)

`spawnFlyingBlock(world, pos, preSpawn, postSpawn)`:

1. Fail unless the block is not air, is allowed (below) and has hardness of 0 or more.
2. Capture: the block state, the pick-block item (`Block.getItem`) as `sourceStack`, and a copy of the tile entity's NBT if there is one.
3. Remove the block with `setBlockState(AIR, flag 2)`: sent to clients, no neighbor updates, **no drops**. If that fails, fail.
4. Create the entity at the block center, run `preSpawn` (the blast sets gravity, spin and velocity here), spawn it, then run `postSpawn`. Returns true if the world accepted it.

A second overload takes already-captured data and a remove-block supplier for callers that do their own removal (red matter).

**Allowed (`isAllowed`).** Not allowed when: the `enable` config is false; the state is null; the material is fire; the block is a fluid (`IFluidBlock` or `BlockLiquid`). Then the ban/allow list: with `ban` true (default) any listed block is refused; with `ban` false only listed blocks are allowed. The mod `dynamictrees` is always in the list (issue #420 in the source). The list is built by a block-state list parser (the same syntax as the rot table in [radioactive-and-mutation.md](radioactive-and-mutation.md#1-rot-block-swaps-contentblastblastradioactiveblockswapsjava-contentradioactiveradioactivehandlerjava)) and rebuilt on config change.

**Config** (file `icbmclassic/flying_blocks`): `enable` (bool, true), `block_ban_allow.ban` (bool, true), `block_ban_allow.list` (string array, default `minecraft:fire`), `damage_scale` (float, 2).

### The entity

It extends the shared projectile base `lib/projectile/EntityProjectile` (an arrow-style entity with per-tick ray casts, an in-ground state and kill timers; described with the other projectiles in `03-missiles`). What matters here:

| Property | Value |
|---|---|
| Gravity | its own field, default 0.05 per tick; blasts set it to 0 (anti-gravitational) or 0.04 (beam). Saved as NBT `gravity` |
| Air drag | 0.99 per tick |
| Flight time limit | 1,200 ticks (`TICKS_MIN`); the anti-gravitational blast sets 4,800 |
| Ground time limit | 20 ticks (`TICKS_SEC`) |
| Other expiry | `posY > 400` or `posY < -100` |
| Immunities | fire (`isImmuneToFire`) |
| Collides with | everything with a collision box except other flying blocks and seats; it can be pushed and collided with |
| Spin | `yawChange` and `pitchChange` are added to the rotation each tick and drop by 2 per tick until they reach 0 (render-only) |
| Impact grace | no placement or item drop in the first 5 ticks of flight (so a block lifted from the ground does not instantly re-place); damage to an entity hit in that window still applies |

**Block impact (after the grace).** `placeBlockIntoWorld(floor(hit vector), hit)`: kill the entity, then on the server try the placement; if it fails, drop the stored source item at the entity's position.

**Placement (MIT-portable).** Permalink: [`EntityFlyingBlock.java#L169-L224`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/entity/flyingblock/EntityFlyingBlock.java#L169-L224).

```
tryPlacement(pos, hit):
    if sourceStack is not empty:
        use the shared mock player; put the stack in its main hand
        result = stack.item.onItemUse(player, world, pos, mainHand, hit.side, hit vector)
        clear the hand
        if result is SUCCESS: return true               // tile data is NOT restored on this path
    // fallback
    if the world allows the block at pos (mayPlace, replaceable check, no entity in the way):
        setBlockState(pos, state, flag 11)
        if saved tile data exists and the new tile has the same "id": merge and write the saved NBT, fix x/y/z, mark dirty
        play the block's place sound (volume (v+1)/2, pitch * 0.8)
        return true
    return false
```

The item path lets multi-part and orientation-sensitive blocks place correctly, but throws away the tile data (a chest arrives empty and its contents are lost, because the original block was removed without drops: ICBM-02-68). The fallback restores the tile only when the tile ids match.

**Entity impact.** Damage = `ceil(speed * damage_scale)` (speed in blocks/tick; 2 at the default scale for a block moving 1 block/tick) with the vanilla falling-block damage source, then the shared knockback (a push of 0.6 in the direction of travel and a 0.1 lift). Then the block **drops its source item and keeps flying** (it is not killed), so every hit tick of an overlap drops another item (ICBM-02-69).

**End of life without an impact.** When the flight timer ends, the block leaves the world above y = 400, or the ground timer ends, the entity places its block at wherever it is, even mid-air (ICBM-02-70). So a block that survives the anti-gravitational 4,800-tick limit ends up as a floating block in the sky.

**NBT.** Projectile base: ground data, source position and UUID. Own: `gravity` (float), `mimic_block` = `{ block_state, source_stack, block_entity }`. Spawn data (to clients): the capture-data NBT, gravity, yaw change, pitch change.

**Rendering.** `RenderEntityBlock` draws the captured block state through the vanilla block renderer (static model only, no tile renderer), shadow 0.5. If a block throws while rendering, the state is remembered as failed, logged once with the entity and block, and drawn as stone (or vanilla leaves for leaf material) from then on.

**Bugs.** ICBM-02-68, 02-69, 02-70 (and 02-40 to 02-44 in the blasts).

## 2. Fragments (`EntityFragments`)

The projectile of the shrapnel, anvil and fragmentation blasts. A hand-written arrow clone (not the shared projectile base); the class is marked deprecated with a TODO to split it per type.

| Field | Meaning | Default |
|---|---|---|
| `damage` | damage multiplier | 11 (the blast sets it) |
| `isExplosive` | explode on first contact | false |
| `isAnvil` | draw as an anvil; use the anvil sound | false |
| `arrowCritical` | set by shrapnel; no code reads it | false |
| `explosionSize` | strength of the contact explosion | 1.5 |
| `flatDamage` | use `damage` without the speed factor; never set | false |

**Flight (per tick, both sides).**

1. If standing in a block, run the ground handler (below). If the block under it changed and nothing collides, it leaves the ground with its speed scaled by a random 0 to 0.2.
2. Else: count a tick in the air. Ray-cast blocks along `2 * motion`; ray-cast entities (not other fragments, and only after 5 ticks in the air) along `motion` up to the block hit, with each box grown by 0.3. An entity hit wins over a block hit.
3. Impact handling (below), then move by `motion`, update the rotation, apply drag (0.99, or 0.6 in water, where it also extinguishes and makes bubbles) and gravity 0.05, then block collision callbacks.

There is no flight-time limit. A fragment ends on contact, by falling out of the world (y < -64 by the entity base), or by the burning rule below.

**Entity hit.**

- Target must be alive, else the fragment just dies.
- Explosive fragments explode first (a flaming vanilla explosion of `explosionSize` at the fragment; the fragment is killed).
- Damage = `ceil(speed * damage)` with an "arrow" indirect source (`shootingEntity` is never set, so there is no attacker, ICBM-02-73). With `speed` 0.5 to 1.2 at launch, shrapnel does about 6 to 14, anvil 15 to 36, fragmentation 6 to 14, falling off with drag.
- A burning fragment (shrapnel is set on fire for 100 s) sets a non-enderman target on fire for 5 s.
- If the target took damage, the fragment dies (except against an enderman, which it passes through). If the hit did nothing it bounces back at 10% speed, resets its air counter and dies when slower than about 0.03 blocks/tick.

**Block hit.** Special blocks (the projectile block-interaction hooks, `01-core`) can absorb or redirect it. Otherwise it plays the arrow hit sound, buries itself 0.05 into the surface, and becomes "in ground" with the arrow shake counter at 7. On the next ticks the ground handler runs:

- Explosive: explode (flaming vanilla explosion, strength 1.5 by default) and die.
- Others: play the landing sound with 50% probability (the anvil sound for anvil fragments, else the arrow hit) and die. The fragment is gone the tick after landing.

**Contact explosion.** `world.createExplosion(this, x, y, z, explosionSize, flaming = true)`, a vanilla `Explosion` with fire, blocks and entity damage (not an ICBM blast). 225 of these at 1.5 for a fragmentation shell. Entity collision with a fragment younger than 2 seconds also detonates an explosive one through `applyEntityCollision`; since the fragment cannot be pushed or collided with, that path is unlikely to run.

**NBT.** `shake` (byte) and `isExplosive` (bool) only. Position and motion come from the entity base. After a save and load an anvil fragment is a plain fragment, its damage is 11 and its explosion strength 1.5, and its remembered contact block is the world origin (ICBM-02-71, 02-72).

**Visuals.** `textures/entity/fragments/fragment.png` (5,699 bytes) on a spinning quad; anvil fragments render a vanilla anvil block with a random damage level and facing each frame. Sounds are vanilla (`entity.arrow.hit`, `block.anvil.hit`).

**Bugs.** ICBM-02-71, 02-72, 02-73.

## 3. Smoke (`EntitySmoke`)

An invisible entity that emits smoke puffs. Spawned by the `smoke` explosive ([gas-and-status.md](gas-and-status.md#2-the-four-gas-blasts)).

| | |
|---|---|
| Time to live | 100 ticks (`ticksToLive`), then it dies on the next update |
| Per tick | 3 to 6 vanilla `SMOKE_NORMAL` particles at (x, y + 0.1, z) with an upward speed of 0.1 and horizontal spread of about +/-0.05 |
| Color | `red` 1, `green` 0, `blue` 0 are stored and synced, but the particle is the vanilla gray smoke and the renderer draws nothing (it returns no texture), so the color is unused |
| Collision | none; cannot be pushed |
| Spawn data | three floats (color) and the int time to live |

Particles are spawned on the entity's own side: the client copy of the entity runs the same update and makes them visible. **NBT:** `ticksToLive`, then a `color` sub-tag in which `red` is written twice and `green` is written from the red field, with no blue (ICBM-02-74).

## 4. Light beam (`EntityLightBeam`)

A vertical beam of light used by the beam blasts. Rendering is a modified beacon beam (`textures/entity/beacon_beam.png` from vanilla); `RenderLightBeam` draws it from 5 blocks below the entity up to build height (255), with a core radius `progress * beamSize` and a glow radius `progress * beamGlowSize`.

| Field | Meaning | Default (beam blast values) |
|---|---|---|
| `red`, `green`, `blue` | color | 1, 0, 0 (blast sets orange 0.7/0.3/0 or blue 0/0.3/0.7) |
| `beamSize` | core radius at full progress | 0.5 (blast: 1) |
| `beamGlowSize` | glow radius | 1 (blast: 2) |
| progress (synced float, `DataParameter`) | current growth, clamped 0 to 1 | -1 until the first update |
| `targetBeamProgress` | where progress is heading | 1 |
| `beamGrowthRate` | change per tick | 0.05 |
| `deathCycle` | die when progress reaches the target | false |

**Update.** Server and client each move progress toward the target by 0.05 per tick. With `deathCycle` set and progress within 0.01 of the target, the entity dies. Safety: it dies after 5 minutes (6,000 ticks) in any case. The render bounding box is 10 wide and goes from y = -10 to infinity, so the beam is drawn at any height. The renderer further eases a client copy of the progress toward the synced value at `growthRate * partialTicks`.

The target and the death flag are plain fields that are **not** in the spawn data or the synced float, so the client's own update always aims at 1 and never dies by itself; it relies on the server sending changes. During the plateaus of the staged beam blast the client copy likely runs ahead (ICBM-02-75, visual and unverified). No NBT is stored, so a reloaded beam has default color, aims at 1, and lives its full 5 minutes unless the blast kills it (ICBM-02-46).

**Bugs.** ICBM-02-46, 02-75.

## 5. Explosion controller (`EntityExplosion`)

A hidden entity that gives a tickable blast a place in the world, a tick, and persistence. Created by `Blast.doAction` for every `IBlastTickable` blast; the class is deprecated in the source.

| | |
|---|---|
| Size | 0.98 x 0.98, no clip, no collision, ignores the frustum check, `move` does nothing |
| Position | non-movable blasts (everything except ender): X and Z at the blast, **Y = -1**; the real height is kept as `blastYOffset = blast.y + 1` and added back when the blast is rebuilt. Movable (ender): at the blast's Y with offset 0 |
| Tick (server and client) | dies if its blast is gone, is not tickable, has a different controller, or is completed. Otherwise runs the blast tick (`onBlastTick`, server only inside the blast), and dies when the blast says it is done |
| Spawn data | the explosive registry id string and the Y offset; the client rebuilds a blast object from the id (so `doExplode` exists on the client but the tick guard keeps it from running, see ICBM-02-52) |
| NBT | `blast` (compound: `ex_id` plus whatever the blast saves, only for blasts that support restore) and `blastPosY` |

After a world load the entity rebuilds the blast from `ex_id` and the saved Y, calls `load` on it (restores only `callCount`, `explosionSize`, the exploder UUID and any per-class fields such as gas `duration`), and the blast starts again from its first step: threads restart, gas clouds regrow, beam stages restart (ICBM-02-46). An unknown or missing id kills the entity with a log line.

**Dead code.** The movement block applies to blasts that implement the `IBlastMovable` interface; no blast class implements it, so entity motion, the 0.98 drag and the 0.5 speed cap never run (ICBM-02-78). "Movable" in `Blast.isMovable()` only decides the Y placement above.

## 6. Primed explosive (`EntityExplosive`)

The lit explosive block: a falling, bouncing block with a fuse.

| | |
|---|---|
| Spawn | by `TileEntityExplosive.trigger` at the block center (`pos + 0.5` on each axis) with the block's facing and the item stack of the explosive; initial motion: horizontal 0.02 in a random direction, vertical +0.2 |
| Motion per tick | horizontal x 0.95, vertical -0.045; `move` with block collision; on the ground horizontal x 0.7 and vertical x -0.5 (a half-strength bounce). No water, lava or fire handling |
| Fuse | `-1` until the first update, then `getFuseTime(world, pos, explosive id)` from the block registry (config fuse times, part a). Each tick the registry's fuse-tick listener runs (visual: lava particles for incendiary and exothermic). The explosion runs when the counter before the decrement is below 1, so a fuse of F explodes after F + 1 ticks and a fuse of 0 explodes on the first tick |
| Explosion | `explosiveCapability.doExplosion(x + 0.5, y + 0.5, z + 0.5, source = entity)` then `setDead`. The blast is created on the server only; the capability's `isExploding` flag stops a second trigger. The +0.5 offsets on all three axes shift the blast away from the entity (ICBM-02-77) |
| Default explosive | condensed when the stack has no explosive |
| Capabilities | EMP receiver (`CapabilityEmpKill`, an EMP pulse kills it, see `05-detection-defense/emp-tower.md`) and the ICBM explosive capability (so the defuser and other code can read the explosive). *1.12-only mechanism* |
| Persistence | NBT `Fuse` as a **byte**, `explosive_stack` (the capability's NBT). Entity facing is not saved |
| Spawn data | the fuse int, the facing byte, the capability NBT |
| Flags | collides and can be pushed |

A fuse above 127 does not survive saving: `Fuse` is stored as a signed byte, so a loaded value goes negative and the entity explodes on its first tick. Of the defaults, antimatter (300) is above the limit for its first 172 ticks and the repulsive/attractive (120) just below (ICBM-02-76).

**Bugs.** ICBM-02-76, 02-77.

## 7. Asset catalog (this sheet)

| Asset | Used by | Notes |
|---|---|---|
| `textures/entity/fragments/fragment.png` | fragments | 5,699 bytes |
| vanilla `textures/entity/beacon_beam.png` | light beam | not an ICBM file |
| none | flying block (draws its block), explosion controller (no render), smoke (returns no texture), primed explosive (draws the explosive block model) | `RenderEntityBlock`, `RenderExBlock`, `RenderExplosion`, `RenderSmoke` are in `07-visuals-audio` |

## 8. Lang

No entity-name keys are referenced by this area; entity names come from `getName()` strings in code (`Fragments`, `Explosive[<id>]`, `Explosion[...]`, `Flying Block [...]`).
