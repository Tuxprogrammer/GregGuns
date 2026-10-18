# Grenades as built (M2: Stielgranate)

- **Techguns baseline:** [projectiles.md § 3.7](../../techguns-spec/04-projectiles-fx/projectiles.md#37-grenadeprojectile-family), [the Stielgranate sheet](../../techguns-spec/02-guns/sheets-launchers-tools-grenades.md#stielgranate-stielgranate), [explosions.md](../../techguns-spec/04-projectiles-fx/explosions.md)
- **Pack rules:** [explosions-pack.md](explosions-pack.md)
- **Code:** `com.gregguns.projectile.GrenadeProjectile` (the family base), `GrenadeMath` (pure), `com.gregguns.explosion.ProjectileExplosion`, `BlastFalloff`, `ExplosionRules`, `BlockBreakGuard`; item `com.gregguns.item.grenade.ItemStielgranate`; recipe row `MeleeTable.STIELGRANATE` (GR-07, LV Assembler)

## Numbers kept

Damage 20, radius 5 (full damage to 2.5 blocks, linear to zero at 7.5), 3 bounces, fuse 200 ticks, stack 16, throw speed `0.5 + 0.5 f`, gravity `0.015 / f`, bow charge curve, `random.bow` on release, `mob.irongolem.hit` on a bounce, `effects.explosion1` (volume 4) on the blast, entity ID 11.

## Deviations

| Techguns | GregGuns | Bug |
|---|---|---|
| Gravity `0.015 / f` divides by zero at `f = 0` | The charge is floored at 0.1 for gravity | GUN-22 |
| Use duration argument ignored (always 72000) | 72000, stated | GUN-23 |
| The throw consumes the first matching stack in the inventory | It consumes from the held stack | GUN-30 |
| A bounce kills the grenade and spawns a new entity | The same entity changes velocity (no flicker, no lost state) | |
| A fourth contact explodes, however gentle | A contact slower than 0.08 blocks per tick settles the grenade (normal speed removed, slide slowed) and costs no bounce, so a dropped grenade waits for its fuse | |
| A bounced grenade loses block damage | Block damage is the config flag on every bounce | PFX-10 |
| A grenade entering water vanishes without exploding | It keeps flying (drag only) and explodes on schedule | PFX-24 |
| Direct hit: a separate hit with the vanilla explosion source, then the blast | The blast alone (the second hit would land inside the target's invulnerability frames) | |
| The exploder (shooter) is excluded from the entity list | The owner takes the blast like anyone else; `explosions.selfDamage` (default true) turns that off | PFX-25 |
| Explosion runs in full on the client | Client copies do nothing | PFX-15 |
| Knockback map never sent | Players hit get an `S27PacketExplosion` carrying the push and no blocks | CORE-19 |
| Block damage gated only by the per-player safe mode | `explosions.blockDamage` (default false, as Techguns hand grenades never broke blocks) plus protection hooks | |

## Explosion pipeline

`ProjectileExplosion.explode(blockDamage)`, server only:

1. `ExplosionEvent.Start` (cancel: nothing at all happens, not even the sound).
2. Ray-cast the blocks (1352 rays) using each block's full-signature resistance; always computed.
3. `ExplosionEvent.Detonate` with the entity list and the live affected-block list.
4. Entities still listed take `BlastFalloff` damage as `EXPLOSION` through `DamageSources.strike` (damage factors, then the typed model) and a vanilla-style push.
5. If block damage is on (config, and `mobGriefing` for NPC-caused blasts), each block still in the list is offered to `BlockEvent.BreakEvent` for a player owner, then dropped and removed with `onBlockExploded`.
6. One line per blast that broke blocks goes to the `gregguns.explosions` logger.

## Open

- The Frag Grenade, Flashbang and Grenade Launcher round reuse `GrenadeProjectile` (override `explode()`); they belong to later M2 slices.
- The thrown grenade is drawn as its item sprite. The 32x32 model texture at `textures/models/guns/stielgranate.png` is not used yet.
