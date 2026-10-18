# Explosions

Covers `java/techguns/util/ProjectileExplosion.java`, `java/techguns/util/TGExplosionDamageSource.java`, and every caller of `ProjectileExplosion`.

## `ProjectileExplosion`

A rewrite of vanilla `Explosion` with separate control over entity damage. It wraps a dummy vanilla `Explosion` object, used for Forge events, block callbacks and drop chances.

### Constructor

`ProjectileExplosion(world, x, y, z, exploder, explosionSize, entityDamage, dmgRadius1, dmgRadius2)`

| Field | Meaning | Default |
|---|---|---|
| `explosionSize` | Vanilla explosion "power". Drives block ray-casting, knockback falloff and drop chance. | n/a |
| `entityDamage` (int) | Maximum damage dealt to entities. | n/a |
| `dmgRadius1` | Full-damage radius. | n/a |
| `dmgRadius2` | Zero-damage radius. Entities beyond it are not affected at all. | n/a |
| `exploder` | `EntityLivingBase` that caused it (usually the shooter). **Excluded from the entity list**, so a shooter is never hurt by their own explosion. | null allowed |
| `damageSource` | Optional override set with `setDamageSource(TGDamageSource)`. | null |
| `explosionSound` | Set with `setExplosionSound`. | `random.explode` |
| `isFlaming` | Places fire. No code ever sets it to true. | false |

### `doExplosion(blockDamage, attacker)`

- **Server:** post Forge `ExplosionEvent.Start`. If it is not cancelled, run part A then part B.
- **Client:** run A and B unconditionally. Callers that run on both sides (alien blaster, grenades) therefore also execute it client-side. See bug [PFX-15](index.md#suspected-bugs).

### Part A: affected blocks and entity damage

1. **Block ray-cast** (identical to vanilla 1.7.10):
   - Cast a ray from the centre along each of the 16 × 16 × 16 grid directions that lie on the cube surface (1352 rays).
   - Each ray starts with intensity `size × (0.7 + rand × 0.6)` and steps 0.3 blocks at a time.
   - At each step, if the block is not air, subtract `(resistance + 0.3) × 0.3`. Resistance comes from the exploder's `func_145772_a` (entity explosion-resistance hook), or from the block's explosion resistance if there is no exploder.
   - Each step also costs `0.3 × 0.75`.
   - While intensity is above 0 and the exploder permits it (`func_145774_a`), record the block position.
   - The affected-block list is always computed, even when `blockDamage` is false.
2. Temporarily double `explosionSize` (matches vanilla).
3. **Entity list:** every entity except the exploder whose bounding box intersects the axis-aligned cube centred on the explosion with half-size `dmgRadius2 + 1`.
4. **Server:** post `ForgeEventFactory.onExplosionDetonate(world, dummyExplosion, list, size)`. Listeners may remove entities from the list.
5. For each entity, measure `distance` from its feet position to the centre. If `distance ≤ dmgRadius2`:
   - **Push direction:** the unit vector from the centre to the entity's eye.
   - **Damage:** `entityDamage × min(1, 1 − (distance − dmgRadius1) / (dmgRadius2 − dmgRadius1))`. This is flat inside r1 and falls linearly to 0 at r2. There is no occlusion, and block density is *not* used for damage.
   - If an attacker is given and both parties are living, multiply by `DamageSystem.getDamageFactor(attacker, entity)`. This is the friendly-fire and team factor, see [`01-core`](../01-core/index.md).
   - **Damage source:**
     - If no override: [`TGDamageSource.causeExplosionDamage(exploder, null, GORE)`](../02-guns/damage-system.md#2-tgdamagesource-extends-vanilla-entitydamagesource). That is name `tg_explosion`, type EXPLOSION, `goreChance = 0.35`, damaging entity = exploder, attacker = null.
     - Otherwise the override.
   - **Knockback:** strength = `(1 − distance / (2 × size)) × blockDensity` (vanilla `getBlockDensity` occlusion), passed through Blast Protection (`EnchantmentProtection.func_92092_a`). It is added to the entity's motion along the push direction.
   - Players are also recorded in a knockback map, `func_77277_b()`. **Nothing reads the map and no `S27PacketExplosion` is sent**, so server-side player knockback is never synced to the client (bug [PFX-06](index.md#suspected-bugs)).
6. Restore `explosionSize`.

### Part B: sound, block destruction and fire

1. Play `explosionSound` at the centre, volume **4.0**, pitch `(1 + (r1 − r2) × 0.2) × 0.7`, where r1 and r2 are two independent random floats.
2. For each affected block: compute (but do not use) the vanilla particle vector. The vanilla `explode`/`smoke` particles and the `hugeexplosion`/`largeexplode` particles are commented out. **All visuals come from the FX packets the callers send.**
3. If `blockDamage`, for each affected non-air block:
   - If `canDropFromExplosion`, call `dropBlockAsItemWithChance(chance = 1 / explosionSize)`.
   - Then `block.onBlockExploded(world, x, y, z, dummyExplosion)`, which sets air and triggers TNT and similar.
4. If `isFlaming` (never set): one in three affected air blocks that sit on an opaque block become fire.

`createExplosionEffect()` (private, never called) is an unused legacy particle routine. For `dmgRadius2 < 1.5` it spawns one legacy `"Explosion"` and one `"LargeSmoke"`. Otherwise it spawns `round(r2 × 3)` `"Explosion"` sprites within ±r2/3, `round(r2 × 8)` `"Fireblast"` sprites in a ring of radius 1 with outward speed 0.21 × rand(0.5..1), and `round(r2 × 2)` `"LargeSmoke"` sprites.

### Callers

| Caller | Size (power) | entityDamage | r1 | r2 | blockDamage | Sound | FX |
|---|---|---|---|---|---|---|---|
| [`RocketProjectile.explodeRocket`](projectiles.md#34-rocketprojectile-and-nukerocketprojectile) | `radius` (gun) | (int) damage | 0.5 × radius | 1.5 × radius | projectile `blockdamage` (safe mode) | `random.explode` | `RocketExplosion` (50 blocks) |
| [`NukeRocketProjectile.explodeRocket`](projectiles.md#34-rocketprojectile-and-nukerocketprojectile) | `radius` | (int) damage | 0.5 × radius | 1.5 × radius | `blockdamage` | `random.explode` **and** `techguns:effects.nukeexplosion` (volume 8) | `NukeExplosion` (150 blocks) |
| [`GrenadeProjectile.explode`](projectiles.md#37-grenadeprojectile-family) (incl. launcher and frag) | `radius` | (int) damage | 0.5 × radius | 1.5 × radius | grenade `blockdamage` (false after a hand-grenade bounce) | `techguns:effects.explosion1` | `RocketExplosion` / `FragGrenadeExplosion` (50 blocks) |
| [`AlienBlasterProjectile.doExplosion`](projectiles.md#1-registration) | 0.5 | (int) getDamage() | 0.25 | 0.75 | false | `random.explode` | `AlienExplosion` (50 blocks) |
| [`BioblobTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-bioblobtileent) (see [`07-blocks`](../07-blocks/index.md)) | blob-dependent | 15 | 0.5 × r | 1.5 × r | (block's own) | `techguns:effects.biodeath` | `bioblobExplosion` |
| [`BiogunProjectile.setBlockForLevel`](projectiles.md#35-biogunprojectile) (dead code) | 1.5 | 30 | 1.0 | 2.0 | false | n/a | n/a |
| [`EnergyBlastProjectile`](projectiles.md#310-energyblastprojectile-mib-gun) | commented out | n/a | n/a | n/a | n/a | n/a | `BlueBlasterExplosion` only |

"Gun-supplied" radius and damage values are in [`02-guns`](../02-guns/index.md). There is **no global config gate** on projectile block damage. The only gate is the per-player safe-mode toggle ([`TechgunsExtendedPlayerProperties.enableSafemode`](../01-core/player-state.md#techgunsextendedplayerproperties), see [`01-core`](../01-core/index.md)), read by [`GenericGun.getDoBlockDamage`](../02-guns/gun-lifecycle.md) (players with safe mode off cause block damage; NPCs never do).

### Direct-hit damage before the blast

- Rockets: raw `damage` with source `tg_explosion` (GORE, gore 1.0, knockback ×3). Nuke uses ×10.
- Grenades: `factor × damage` with the vanilla explosion `DamageSource` (`explosion` / `explosion.player` death messages).
- After the direct hit, the AoE also hits the same entity. Direct hits apply to anyone (no exploder exclusion), but the shooter cannot be hit directly because the projectile sweep ignores the shooter.

## `TGExplosionDamageSource`

`extends EntityDamageSource`. Fields: `deathtype` (DeathType, default DEFAULT) and `attacker`.
- `getSourceOfDamage()` returns the damaging entity.
- `getEntity()` returns `attacker`.
- The constructor is `(name, projectile, attacker, deathtype)`.

**Unused.** Nothing constructs it; its factory and death-message override are commented out. Explosions use `TGDamageSource.causeExplosionDamage` instead (see [`01-core`](../01-core/index.md)).

## Death messages (lang, `en_US.lang`)

These belong to the damage sources used here.

| Key | English |
|---|---|
| `death.attack.tg_explosion` | `%s was blown up by %s` |
| `death.attack.tg_explosion.item` | `%s was blown up by %s with %s` |

Because the AoE source has a null attacker, the killer name falls back to the damaging entity (the exploder). If both are null it falls back to the victim's own name: "X was blown up by X".
