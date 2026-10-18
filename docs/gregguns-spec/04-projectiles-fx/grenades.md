# Grenades as built (M2: Stielgranate, Frag Grenade, Flashbang)

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

## The Frag Grenade (MV)

- **Techguns baseline:** [the Frag Grenade sheet](../../techguns-spec/02-guns/sheets-launchers-tools-grenades.md#frag-grenade-fraggrenade); recipe row `GrenadeTable.FRAG_GRENADE` (GR-08, MV Assembler, aluminium plate gate).
- **Code:** `ItemFragGrenade`, `FragGrenadeProjectile` (entity ID 24, as Techguns), `FragMath` (pure), `FragBlast`. All throw rules come from `ItemThrownGrenade`, the new base of the three hand grenades.
- **Numbers kept:** damage 25, radius 6 (full damage to 3 blocks, zero at 9), 2 bounces, fuse 120 ticks, stack 16, no block damage by default, `guns.grenade.pin` on wind-up and `guns.grenade.throw` on release.
- **Fragments (new design).** The Techguns frag grenade is a plain blast with a different particle effect. GregGuns makes the steel rounds in its recipe real: after the blast (and only if no listener cancelled it), 64 fragments leave the centre on a fixed Fibonacci spiral over the sphere (no randomness, so a blast is repeatable). Each travels up to 10 blocks and stops at the first living entity on its line, or at an opaque block first. An entity takes the sum of its fragments in one `PROJECTILE` hit, each fragment doing 3 half hearts at the centre falling to 1 at 10 blocks, with 20% of the damage skipping armor. A body-sized target 1.5 blocks away is hit by about 2 to 3 fragments, one at 8 blocks by none or one: the fragments make a frag grenade deadly up close and in the open, and a wall or another body shields. The owner follows `explosions.selfDamage` like the blast. Fragments never touch blocks.

## The Flashbang (MV, a finished design)

Techguns constructed a Flashbang but never registered it ([GUN-24](../../techguns-spec/BUGS.md), [PFX-22](../../techguns-spec/BUGS.md)); its projectile was unreachable, had no entity registration, no sound or visual, and no line-of-sight or facing rule (it blinded through walls and blinded the thrower as hard as anyone). [0001](../../design/0001-tier-map.md) asks for it as a finished design. This is that design.

- **Code:** `ItemFlashbang`, `FlashbangProjectile` (entity ID 25), `FlashMath` (pure), `FlashBlast` (world glue), `LineOfSight`, `PacketFlash` + `FlashHooks` (server to client), client `FlashOverlay`; recipe row `GrenadeTable.FLASHBANG` (GR-09, MV Assembler, aluminium plate gate, magnesium dust is the flash charge).
- **Flight:** the grenade family's: throw `0.5 + 0.5 f`, 2 bounces, stack 16. Fuse **40 ticks** (Techguns' 200 ticks with 0 bounces made a hand grenade that pops on the first wall). It also pops on touching an entity, like the others.
- **Effect:** `intensity = falloff x visibility x facing`, each 0 to 1 (`FlashMath`).
  - *Falloff:* linear from 1 at the blast to 0 at 8 blocks (Techguns: `1 - d / sqrt(32)`, about 5.7 blocks).
  - *Visibility:* three lines from the blast, to the eyes (weight 0.6), chest (0.3) and feet (0.1). Opaque blocks stop a line; glass, panes and fences do not. A wall in the way gives 0 and the entity is untouched.
  - *Facing (players only):* `0.15 + 0.85 t^2` with `t = (dot + 1) / 2` and `dot` the cosine between the look direction and the direction to the blast. Facing it is 1, side-on about 0.36, with the blast directly behind 0.15, so a player who turns their back at 4 blocks gets nothing.
  - *Duration:* `intensity x 160` ticks (8 s at most); below 20 ticks nothing is applied (the Techguns threshold).
- **Players** get Blindness for the duration, Nausea for half of it, and a white-out on the screen (`PacketFlash`): solid white for the first 30% of the duration, then fading with an eased curve. The white-out is drawn after the whole HUD. No damage, no knockback.
- **Mobs** get Slowness and Weakness for the duration (Slowness IV from intensity 0.5, else II), and forget their attack and revenge targets. A mob's senses can bring a target back; it is slow when it does. No facing rule (a mob has no screen).
- **The thrower** is affected like anyone else, unless `explosions.selfDamage` is false (the same switch as the blast, PFX-25). The facing and wall rules replace the flat blinding of the thrower in Techguns (PFX-22).
- **Blocks and damage:** none. The flash never touches a block, whatever `explosions.blockDamage` says, and deals no damage. It is not an `ExplosionEvent`, so claim and spawn-protection hooks have nothing to filter.
- **GT hazards:** none fits. GregTech's hazards (heat, cold, biological, radiation, electricity, gas, space) are environmental conditions; a flash is a one-off sensory effect, so hazmat does not reduce it. Night Vision Goggles do not either (a later "flash protection" bonus for a helmet is an open question).
- **Sounds:** `guns.grenade.pin` and `guns.grenade.throw` as the frag; the detonation is `guns.flashbang.bang` (volume 4), with a burst of firework sparks.

## Recipes

Both are MV Assembler recipes at `RECIPE_MV`, 15 s, 16 grenades per run.

| Row | Output | Inputs | Circuit |
|---|---|---|---|
| GR-08 | Frag Grenade x16 | `plate.Aluminium` x2, `round.Steel` x16, 2 vanilla TNT | 6 |
| GR-09 | Flashbang x16 | `plate.Aluminium` x2, `dust.Magnesium` x4, `dust.Gunpowder` x2 | 7 |

Deviation from [the MV recipe table](../02-guns/recipes.md): the sheet asks for `dust.TNT` x4, but GT's `Materials.TNT` is built without dust items (its `MaterialBuilder` never calls `addDustItems`), so no TNT dust exists. The Frag Grenade takes 2 vanilla TNT blocks instead. The same sheet error affects RC-26, RC-32, RC-33 and RC-34 in [06-items/recipes.md](../06-items/recipes.md); they need the same fix when they are built.

## Tests

Unit: `FlashMathTest`, `FragMathTest`, `GrenadeTableTest`. Scenarios: `grenade.frag`, `grenade.flash`, `grenade.flash.noblocks` and the visual `mp.flashbang` ([docs/testing/README.md](../../testing/README.md)).

## Open

- The Grenade Launcher round reuses `GrenadeProjectile` (override `explode()` or use `runBlast()`); it belongs to a later M2 slice.
- Thrown grenades are drawn as their item sprite. The 32x32 model textures at `textures/models/guns/` are not used yet.
- A flash-protection bonus for helmets (goggles, gas mask) is not designed.
