# Explosions, protection and GregTech machines

- **Techguns baseline:** [explosions.md](../../techguns-spec/04-projectiles-fx/explosions.md) ([Part A](../../techguns-spec/04-projectiles-fx/explosions.md#part-a-affected-blocks-and-entity-damage), [Part B](../../techguns-spec/04-projectiles-fx/explosions.md#part-b-sound-block-destruction-and-fire)), bugs [PFX-14, PFX-25, CORE-19](../../techguns-spec/BUGS.md)
- **Design records:** [0004 damage model](../../design/0004-damage-model.md) (explosion damage type kept), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (explosive charges kept, gated)
- **Pack mods:** ServerUtilities (`serverutilities`, 2.4.14), GT5-Unofficial (`gregtech`, 5.09.54.205)
- **Roadmap:** M5 exit gate "Explosions respect config and protection (fixing PFX-14, PFX-25 and CORE-19), with tests"; M6 for charges

## What changes from Techguns

Entity damage, knockback and the ray-cast are unchanged. What changes is what happens to **blocks**, because the pack has a protection mod that works through Forge events, and GregTech machines react to being blown up.

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| Part B breaks "each affected block" from the list it computed | Part B breaks the blocks that remain in the explosion's affected-block list **after** `ExplosionEvent.Detonate` has been posted | ServerUtilities removes protected blocks from that list. |
| `MiningDummyProjectile` removes blocks with no `BreakEvent` (PFX-14) | Any GregGuns code that breaks blocks on a player's behalf posts `BlockEvent.BreakEvent` with the owner, and honours cancellation | ServerUtilities cancels `BreakEvent` in claimed chunks. |
| Blocks explode through `onBlockExploded` on a dummy `Explosion` | Same. A GregTech machine in the blast may then explode too. | `BlockMachines.onBlockExploded` chains a machine explosion. |
| Resistance read through the exploder hook or the block | The block's full-signature `getExplosionResistance(Entity, World, x, y, z, ex, ey, ez)` | `BlockMachines` overrides that form. |

## Integration points

### ServerUtilities: explosion filter on `ExplosionEvent.Detonate`

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/serverutilities/serverutilities.cfg`: `world.enable_explosions` = `TRUE`, `world.safe_spawn` = `false`, `world.chunk_claiming` = `false`.
- **Source:** [ServerUtilitiesWorldEventHandler.java#L105-L139](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/handlers/ServerUtilitiesWorldEventHandler.java#L105-L139). On the server it copies `event.getAffectedBlocks()`, clears it, and re-adds each position that passes a test. The test is: not inside the spawn area when `safe_spawn` is on; then, if `enable_explosions` is `DEFAULT`, the claimed chunk's own explosion setting (unclaimed chunks allow it); otherwise the global `TRUE` or `FALSE`.
- **What it means as shipped:** `TRUE` and no claiming, so the filter lets everything through. A server owner who sets `enable_explosions=FALSE`, turns on `safe_spawn`, or enables claiming with `DEFAULT` will see blocks removed from the list. Only the **block list** is filtered. Entity damage is not.
- **GregGuns does:** posts `ForgeEventFactory.onExplosionDetonate` as Techguns does, then reads the affected positions back from the explosion object that the event wraps, not from a local copy. Entity damage still applies in a protected area, so a rocket still hurts but does not dig.
- **Pack-side change:** none. Server owners choose the settings.
- **Not verified:** that `ExplosionEvent.Detonate.getAffectedBlocks()` returns the explosion's live list in the pack's Forge build. This is expected from Forge 1.7.10 but was not read here. GGS-04-10 has a test for it.

### ServerUtilities: claim protection for direct block breaking

- **Pack config:** `serverutilities.cfg`, `teams.grief_protection` = `true`, `teams.interaction_protection` = `true`; effective only when claiming is on (`world.chunk_claiming` = `false` as shipped).
- **Source:** [ClaimedChunks.java#L244-L262](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/data/ClaimedChunks.java#L244-L262) decides for an `EntityPlayerMP`; [L480-L485](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/data/ClaimedChunks.java#L480-L485) cancels `BlockEvent.BreakEvent` at highest priority.
- **GregGuns does:** the mining projectile, ore drill and any charge that breaks blocks one by one post `BlockEvent.BreakEvent` for the owner (an `EntityPlayerMP` or a fake player) and skip the block if the event is cancelled. This also fixes PFX-14 for any other protection mod.
- **Pack-side change:** none.

### GregTech: machines react to nearby explosions

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/GregTech/GregTech.cfg`: `machineExplosions` = `true` (L374), `machineNonWrenchExplosions` = `true` (L386), `loggingExplosions` = `true` (L181), `explosionItemDrop` = `false` (L145).
- **Source:**
  - [BlockMachines.java#L463-L470](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockMachines.java#L463-L470): `onBlockExploded` logs "exploded due to near explosion" and calls `doEnergyExplosion()`, then removes the block.
  - [BaseMetaTileEntity.java#L1321-L1337](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/BaseMetaTileEntity.java#L1321-L1337): that only triggers a second explosion if stored EU is at least a fifth of capacity, and its power grows with stored EU. A related wire-fire branch is at [L1345-L1350](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/BaseMetaTileEntity.java#L1345-L1350).
  - [BlockMachines.java#L686-L695](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockMachines.java#L686-L695) and [MetaTileEntity.java#L617-L619](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/MetaTileEntity.java#L617-L619): machine resistance is 10.0 unless a machine overrides it. [BlockCasingsAbstract.java#L83-L86](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockCasingsAbstract.java#L83-L86): casings use the iron block's resistance, but through the one-argument method.
  - [GTLog.java#L139-L171](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTLog.java#L139-L171): the log line holds dimension, position, block name, owner name and detail text. It never names the cause.
- **What it means:** a charged EU machine caught in a Grenade Launcher blast can explode a second time, and one more machine-sized hole follows. This is how GT machines already react to any explosion in the pack, and a dense base can chain. GregGuns does not suppress it.
- **GregGuns does:**
  - Calls the full-signature resistance method for the ray-cast, with the exploder (or `null`), so GT's override is used.
  - Writes its own line to a `gregguns.explosions` logger for each explosion that breaks blocks: dimension, position, radius, shooter name and weapon. Server owners can then match it to GT's "near explosion" lines in `logs/Explosion.log`.
  - Does not call `GTLog` or write into GT's log file.
- **Pack-side change:** none. GT already logs its half. A server owner who wants no chained machine blasts can set `machineExplosions=false`; that is theirs to decide.

### Vanilla gamerule for NPC-caused block damage

No pack file changes `mobGriefing`. A GregGuns NPC explosion that breaks blocks should respect it, like a vanilla creeper does. This is a vanilla rule, listed here only so the check is not forgotten (GGS-04-14).

## Requirements

- **GGS-04-10.** After posting `ExplosionEvent.Detonate`, block destruction reads positions from the explosion's affected-block list, and a test proves that a listener which clears the list prevents all block removal.
- **GGS-04-11.** Entity damage and knockback are not changed by the Detonate listener's block filtering.
- **GGS-04-12.** Every GregGuns path that removes blocks for a player (mining projectile, ore drill, charges when armed by a player) posts `BlockEvent.BreakEvent` for that player and stops on cancel.
- **GGS-04-13.** The ray-cast uses the full-signature `getExplosionResistance` of each block.
- **GGS-04-14.** Block damage from an NPC-caused explosion is skipped when the world's `mobGriefing` gamerule is false.
- **GGS-04-15.** Each explosion that removes at least one block writes one line to the `gregguns.explosions` logger.
- **GGS-04-16.** GregGuns never changes GregTech's explosion logger or config.

## Tests

- GGS-04-10, GGS-04-11, GGS-04-13: unit tests on `ProjectileExplosion` with a fake world and a registered Forge listener. If a fake world is not practical, a `/gregguns test` scenario in `runServer`.
- GGS-04-12: `/gregguns test` scenario with a cancelling `BreakEvent` listener.
- GGS-04-14: unit test of the gating function.
- GGS-04-15, GGS-04-16: unit test of the log message; GGS-04-16 by code review.
- Pack behaviour: `runServer` with ServerUtilities loaded and `enable_explosions=FALSE`; a grenade must damage a mob and leave blocks intact.

## Open questions

- **Chained GT machine explosions.** Is that wanted for GregGuns' balance, or should high-tier explosives skip `onBlockExploded` on GT machines? The Techguns behaviour calls it. Settle in the M5 or M6 design record on explosives.
- **Shooter immunity (PFX-25).** Keeping it is simpler. ServerUtilities has team and PvP settings that may expect friendly fire. Decide with the damage-model owner; no pack file forces either choice.
- **Position conversion in the Detonate handler.** The handler builds a `ChunkDimPos` from the block coordinates it reads from each `ChunkPosition`. Whether that constructor converts block to chunk coordinates was not checked. If it does not, claim-based filtering would be wrong for every mod's explosions, not just ours. Check before relying on `enable_explosions=DEFAULT` in docs.
