# Explosive charges: claims, drops and GT explosives

- **Techguns baseline:** [explosive-charges.md](../../techguns-spec/09-machines-utility/explosive-charges.md), [parameters](../../techguns-spec/09-machines-utility/explosive-charges.md#parameters), [explosion algorithm](../../techguns-spec/09-machines-utility/explosive-charges.md#explosion-algorithm)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (charges kept, tier-gated, "revisit if they overlap GT mining explosives"), [0001 tier map](../../design/0001-tier-map.md)
- **Pack mods:** ServerUtilities (`serverutilities`, 2.4.14), GT5-Unofficial (`gregtech`, 5.09.54.205, which also ships GT++ as `miscutils`)
- **Roadmap:** the charges milestone in [ROADMAP](../../ROADMAP.md)

## What changes from Techguns

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| `BlockEvent.BreakEvent` is posted with the owner looked up by UUID, which can be null ([MUTL-30](../../techguns-spec/BUGS.md)) | The event always carries a real `EntityPlayerMP`: the owner if online, otherwise a Forge `FakePlayer` built from the owner's profile | ServerUtilities skips claim protection for a null player (see below) |
| Anyone who opens the GUI can change the settings ([MUTL-31](../../techguns-spec/BUGS.md)) | Only the owner, or a ServerUtilities team member, may change them | Same ownership rules as the Turret |
| Hardness caps 30 (TNT) and 100 (C4) | Same defaults, config-driven, and tied to the tier gate in [0001](../../design/0001-tier-map.md) | GT blocks span a very wide hardness range |
| No statement about GT explosives | Kept as distinct; see below | [0002](../../design/0002-machine-deconfliction.md) asked for a check |

## Overlap with GT mining explosives

GT's own explosives are radial and destructive. Ours is a directional bore with no explosion.

| | GT5-Unofficial | GregGuns charge |
|---|---|---|
| Powder Barrel | A `BlockReinforced` meta (hardness 2.5), primed as an `EntityTNTPrimed` subclass with an 80-tick fuse. A normal vanilla-style explosion | n/a |
| Mining Explosives (GT++) | `BlockMiningExplosives`, primed entity with a 160-tick fuse. It calls `ExplosionHandler.createExplosion` with strength 100 | n/a |
| Shape | Sphere. Damages entities, can cause fire | Rectangular tunnel into the mounted block. No entity damage, no fire, no crater |
| Placement | Placed or dispensed as a block | Stuck to a face, configured in a GUI |

- **Source:** [BlockReinforced.java#L87-L92](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockReinforced.java#L87-L92) (Powder Barrel), [EntityPowderBarrelPrimed.java#L7-L12](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/entity/EntityPowderBarrelPrimed.java#L7-L12), [BlockMiningExplosives.java#L22](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gtPlusPlus/core/block/general/BlockMiningExplosives.java#L22), [EntityPrimedMiningExplosive.java#L342-L346](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gtPlusPlus/core/entity/EntityPrimedMiningExplosive.java#L342-L346).
- **Not verified:** IC2's Industrial TNT (ITNT) is an IC2 block, not a GT one. This sheet did not read it. Check it before the balance pass.
- **Verdict:** the mechanics do not overlap, so the "keep" in [0002](../../design/0002-machine-deconfliction.md) stands. They overlap in role (mining), so the tier gate matters. See Open questions.

## Integration points

### ServerUtilities: claimed-chunk protection
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/serverutilities/serverutilities.cfg`: `teams` → `grief_protection=true` (line 301), `enable_explosions=TRUE` (line 337).
- **Source:**
  - [ClaimedChunks.blockBlockEditing](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/data/ClaimedChunks.java#L244-L262) returns false straight away if the player is not an `EntityPlayerMP`. A null player is never blocked.
  - [ClaimedChunks.onBlockBreak](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/data/ClaimedChunks.java#L480-L485) cancels the `BlockEvent.BreakEvent` when that check says so.
  - [Universe.getPlayer(ICommandSender)](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/Universe.java#L602-L626) treats a Forge `FakePlayer` as the universe's fake player.
  - [ForgeTeam](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/ForgeTeam.java#L84) starts its fake-player status at ALLY, so a fake player gets the team's ally rights.
  - The explosion hook ([ServerUtilitiesWorldEventHandler.java#L105-L135](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/handlers/ServerUtilitiesWorldEventHandler.java#L105-L135)) only sees `ExplosionEvent.Detonate`. Our charge makes no `Explosion`, so the break event is its **only** protection.
- **GregGuns does:**
  - posts `BlockEvent.BreakEvent` for each block, with the owner's `EntityPlayerMP` when online, else a `FakePlayer` from the stored owner profile;
  - stops the column when the event is canceled (unchanged from Techguns);
  - needs no ServerUtilities code. Any protection mod that listens to the break event works.
- **Pack-side change:** none. A server that wants charges to ignore claims can already set `grief_protection=false`.

### GT: drops from GT ore blocks
- **Source:** [GTBlockOre.getDrops](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/GTBlockOre.java#L184-L204) derives drops from block metadata. It applies fortune and silk touch only when the harvester is a real player.
- **GregGuns does:** keeps the Techguns rule of fortune 0, no silk touch, and calls `getDrops` before the block is set to air. A charge therefore returns the base ore drops from GT ore blocks.
- **Pack-side change:** none.

## Requirements

- **GGS-09-10** The break event's player is never null. Owner online: the owner. Owner offline: a `FakePlayer` carrying the owner's profile.
- **GGS-09-11** A canceled event stops that column and breaks nothing beyond it.
- **GGS-09-12** Blocks are not broken by an `Explosion`, so no entity damage and no fire occur.
- **GGS-09-13** Drops use fortune 0 and are gathered before the block is removed.
- **GGS-09-14** Both hardness caps are config options. Negative hardness is never broken.
- **GGS-09-15** Only the owner, or a ServerUtilities team member when that mod is loaded, can change a charge's settings.

## Tests

| Req | Check |
|---|---|
| 10, 11 | Unit test with a listener that cancels; `runServer` check with ServerUtilities and a claimed chunk |
| 12 | Unit test that no `Explosion` is created |
| 13 | `/gregguns test` scenario that bores through a GT ore block and checks the drop |
| 14 | Unit test on the config bounds |
| 15 | Unit test with a stub friend provider (shared with the Turret) |

## Open questions

- Should C4's cap of 100 be lowered or raised relative to the GT hardness range? `BlockReinforced` alone spans hardness 0.5 to 1500 ([L59-L137](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockReinforced.java#L59-L137)). A balance pass against the tier map would settle it.
- Is the ITNT overlap real? Read IC2's ITNT first.
