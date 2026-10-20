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
| A 176 x 166 texture screen (`explosive_charge.png`, `advanced_explosive_charge.png`) with a Detonate button, plus and minus pairs for radius, length and delay, and no state display. Right-clicking an armed charge does nothing | A ModularUI2 panel in GT's standard theme, the same family as the Turret and benches. It shows a status banner (green NOT ARMED, red ARMED with a live countdown), a fuse stepper and burn bar, depth and width steppers, the world direction the tunnel runs, a side diagram of the bore, the owner, and Arm and Disarm buttons. An armed charge opens the screen too, so the owner can watch the countdown or disarm | Owner request after an in-game look: the old screen "looked wrong" and gave no feedback. Diverging from Techguns is allowed |
| No way to stop a fuse once armed | New button 7, Disarm: stops the fuse and restores the delay the owner set. Owner or team only, like every button | Arming is otherwise a commitment; with a live GUI a disarm is the obvious control |
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

## The settings screen

- **Framework.** `TileExplosiveCharge` is its own ModularUI2 GUI holder (`IGuiHolder<PosGuiData>`), opened with `GuiFactories.tileEntity()` from the block's right-click. The panel is `ChargePanel`; the pure rules (labels, which button looks live, direction names) are `ChargeGuiModel`. The old FML GUI id 30, `ChargeContainer`, `ChargeGui` and the two Techguns-style screen PNGs are no longer used. The charge holds no items, so there are no inventory slots and no shift-click; the inventory key closes the screen like any container GUI.
- **Layout.** Title; banner; Fuse delay (3.0 to 10.0 s, one-second steps, amber readout, burn bar); Bore (Depth, Width, "Bores down/up/north/south/east/west", side diagram); Owner; an access line in plain words; Arm and Disarm.
- **TNT versus Advanced.** Both screens have the same layout. The TNT charge's width readout is fixed (3 x 3) with no buttons (MUTL-33 stays fixed); its depth stepper keeps its 2 to 5 range. The Advanced charge adds the width stepper (3 to 7) and a deeper depth range (2 to 10).
- **Direction.** The tunnel always runs into the block the charge is stuck to, so "relative to the face" is by construction. The screen names the world direction (a floor charge bores down, a ceiling charge up) and draws the charge on the surface with the bore going straight into the rock, one tick mark per block of depth.
- **Non-owners.** Anyone within reach may open the screen and read the state. For a non-owner every control reads as locked and the access line says "View only". The server still refuses: every press is a sync handler that calls `TileExplosiveCharge.pressBy`, which re-checks the owner (MUTL-31), and `PacketChargeButton` keeps the same check for packet-level callers.
- **Live values.** Fuse ticks, armed, depth, width, set fuse and the viewer's edit right are sync values the server pushes, so the countdown moves while the screen is open.
- **NEI.** The panel is centered and 176 wide; NEI's item list sits at the screen edge and does not overlap.

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
- **GGS-09-40** The charge screen is a ModularUI2 panel opened from the tile; GregGuns registers no FML GUI id for it.
- **GGS-09-41** The screen shows the armed state and a live fuse countdown, the set fuse, the depth, the width and the world direction of the tunnel.
- **GGS-09-42** Button 7 (Disarm) stops an armed fuse and restores the set delay. It does nothing on an unarmed charge. While armed, every other button is ignored.
- **GGS-09-43** The screen opens for an armed charge. A non-owner sees the state with all controls locked, and the server rejects their presses.
- **GGS-09-44** Fuse, depth and width stay in their ranges at both ends; the screen greys the button that would go past an end.

## Tests

| Req | Check |
|---|---|
| 10, 11 | Unit test with a listener that cancels; `runServer` check with ServerUtilities and a claimed chunk |
| 12 | Unit test that no `Explosion` is created |
| 13 | `/gregguns test` scenario that bores through a GT ore block and checks the drop |
| 14 | Unit test on the config bounds |
| 15 | Unit test with a stub friend provider (shared with the Turret) |
| 40, 41 | `ChargeGuiModelTest`; screenshots from `utility.charge_gui_shots` (armed, unarmed, not owner) |
| 42, 44 | `ChargeParamsTest` (disarm, ends of the ranges); scenario `utility.charge_gui_controls` |
| 43 | `ChargeGuiModelTest.aNonOwnerSeesNoLiveButton`; scenarios `utility.charge_gui_controls` and `utility.charge_foreign_button` |

## Open questions

- Should C4's cap of 100 be lowered or raised relative to the GT hardness range? `BlockReinforced` alone spans hardness 0.5 to 1500 ([L59-L137](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/blocks/BlockReinforced.java#L59-L137)). A balance pass against the tier map would settle it.
- Is the ITNT overlap real? Read IC2's ITNT first.
