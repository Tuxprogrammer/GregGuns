# M5 exit gate: explosions

Evidence for the roadmap line "Explosions respect config and protection (fixing PFX-14, PFX-25 and CORE-19), with tests", plus the nuke-scale rules the Nuke Launcher (ZPM) needs.

## Gate evidence

| Item | Rule | Test |
|---|---|---|
| PFX-25 | The owner stays in the victim list unless `explosions.selfDamage` is off (`ExplosionRules.victims`) | `ExplosionGateTest.doesNotReproducePFX25_ownerHurtByOwnBlast`; scenario `grenade.protected` |
| CORE-19 | A player's push travels in an `S27PacketExplosion` with an empty block list (`ProjectileExplosion.pushPacket`) | `ExplosionGateTest.doesNotReproduceCORE19_playerPushPacketCarriesNoBlocks` |
| PFX-14 | A block whose `BlockEvent.BreakEvent` is cancelled is kept (`BlastSlicer.keepAllowed` models the listener; `BlockBreakGuard` posts the real event) | `ExplosionGateTest.doesNotReproducePFX14_cancelledBreakEventKeepsBlock`; scenario `grenade.protected` (a real cancelled BreakEvent) |
| mobGriefing | An NPC-caused blast needs the `mobGriefing` gamerule, a player's does not; config off always wins | `ExplosionGateTest.mobGriefingGatesNpcBlastsOnly` |
| Audit log | One line per blast that broke blocks, logger `gregguns.explosions`: dimension, position, radius, shooter, weapon, blocks | `ExplosionGateTest.auditLineNamesDimensionPositionRadiusShooterWeaponAndBlocks` |

## Nuke scale

- **Per-tick cap.** `ExplosionRules.BLOCKS_PER_TICK` (1500). `ProjectileExplosion` breaks that many blocks in the blast's first tick, nearest the center first, and hands the rest to `BlastTicker`, which breaks one batch per queued blast at the end of each server tick. A radius 10 sphere is about 4200 blocks, so three ticks. Entity damage and the `Detonate` filter are unaffected and happen at once. A queued block still goes through `BlockBreakGuard` when it is broken, so a late veto still keeps it. Queued work is dropped if its world unloads. Tests: `ExplosionGateTest.slicerNever...`, `aRadiusTenSphere...`; scenario `explosion.nuke.sliced`.
- **150-block FX packet.** A blast with power at or above `NUKE_FX_MIN_SIZE` (8) sends `PacketBlastFx` (center and radius) to every player within `NUKE_FX_RANGE` (150 blocks), past the roughly 64 blocks vanilla particles reach. The client side is `BlastFxHooks.sink()`, a no-op until the FX system (I8) installs the flash and cloud.

## Decision: chained GT machine explosions (Q4)

GregTech behavior is kept: blocks in a blast get `onBlockExploded`, so a charged GT machine can explode and chain. The audit line counts every block, machines included. A server owner can set `explosions.machineExplosions=false` to leave GregTech machines (any block whose tile entity is an `IGregTechTileEntity`) standing. Default true. Synced.

## Not done here

The Power Hammer and Chainsaw do not exist yet (piece I11). They must break blocks through `BlockBreakGuard.mayBreak`; that rule is recorded on I11's row, and `BlockBreakGuard` needs no change.
