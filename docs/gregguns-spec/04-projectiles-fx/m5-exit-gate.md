# M5 exit gate audit

Evidence for the three M5 exit-gate boxes in [ROADMAP.md](../../ROADMAP.md#m5--energy-and-exotic-weapons). Audited on master at `88a96c82`, after infrastructure pieces I1 to I13 and gameplay batches B1 to B6 merged. It covers the 17 weapons of the plan (the Power Hammer and its Advanced variant count as one) as 18 gun items, plus the Akimbo De-Atomizer Pistols. This audit was written from the docs, specs and test sources; no gradle build and no scenario run was made for it. Scenario names and what each asserts are in [docs/testing/README.md](../../testing/README.md). The gun deviations are in the [02-guns index](../02-guns/index.md).

The audit follows [the M2 audit](../02-guns/m2-exit-gate.md). The detail of the explosion rules (the original content of this page) is kept at the end.

## Verdicts

| Box | Verdict |
|---|---|
| 1. Each weapon is tier-gated with EU or fluid ammo per D3 | Met headless |
| 2. Explosions respect config and protection (PFX-14, PFX-25, CORE-19), with tests | Met headless |
| 3. The particle and FX system carries the FX catalog, and performance is checked with many emitters | Not met: catalog and budget are proven headless, real frame cost needs a client |

## 1. Each weapon is tier-gated with EU or fluid ammo per D3

| Check | Evidence |
|---|---|
| Every assembly row passes `GateRule` with distinct circuits per tier | `GunDefinitionsTest.everyAssemblyRowMakesItsGunAndMeetsTheGateRule` and the table-driven rules over all `GunDefinitions.ALL` entries; `RowIdsAndFluidsTest` (unique row IDs, fluid inputs); the dev self-check tier rule at every scenario boot |
| EV ballistic guns | `EvBallisticLogicTest`, `EvAmmoTableTest`; scenarios `gun.<pdw,pulse_rifle,minigun>.recipe` and `.gate` (a machine one tier down is not offered the row). Merge `d5042637` (B1, B2, B4 integration), `b5e9656b` (I12 EV ammo) |
| EV energy guns | `EvEnergyGunsTest`, `EnergyGunLogicTest`; `gun.<laser_rifle,tesla_rifle,sonic_rifle>.recipe` and `.gate`; `gun.energy.core`, `gun.energy.item`, `gun.energy.charger` (an LV buffer charges an LV gun, an MV buffer refuses it: exact tier, design 0008). Merges `172f09ac` (I1), `191d9307` (I2) |
| IV energy guns | `EnergyBatchDefinitionsTest`; `gun.energy_iv.tier_gate` (IC2 tier 5, 8192 EU/t, never gives energy back, tungsten steel gate); `gun.blaster_rifle.recipe`, `gun.deatomizer_pistol.recipe`, `gun.scatter_laser_rifle.recipe`. Merge `d5042637` |
| Fluid and tool guns | `BioGunLogicTest`, `ToolGunDefinitionsTest`, `ToolStatsTest`, `ToolDrainTest`, `ChargeLogicTest`; `gun.bio_gun.recipe`, `.tiergate`, `gun.bio_gun.reload`; `gun.tool.reload`, `gun.power_hammer.recipe`, `gun.power_hammer_adv.upgrade`, `gun.chainsaw.recipe`. Merges `8fa5423a` (B3a), `386aed0c` (B3b), `48a633dc` (I11), `1295b19d` (I10) |
| LuV | `NetherBlasterDefinitionTest`, `AlienBlasterDefinitionTest`, `GrimReaperDefinitionTest`; `gun.alien_blaster.tiergate`, `gun.grim_reaper.tiergate`, `gun.nether_blaster.recipe` (Assembly Line list), `gun.nether_blaster.reload`. Merges `55705eeb` (B5), `96fff378` (I13) |
| ZPM | `NukeLauncherDefTest`, `BeamMathTest`, `MovingBeamTest`, `BeamSweepTest`; `gun.nuclear_death_ray.tiergate` (ZPM, Naquadah alloy plate), `gun.nuclear_death_ray.recipe`, `gun.nuke_launcher.recipe`. Merges `55705eeb` (B6 NDR), `009434a7` (B6 Nuke Launcher), `1bd1feec` (I5) |
| EU is spent, not rounds (D3) | `gun.laser_rifle.fire` and the other `*.fire` scenarios assert the exact EU per pull; `gun.nuclear_death_ray.drain` (a charge for exactly 3 ticks of beam) |
| Fluid ammo (D3) | Bio Tank, Air Tank and Fuel Tank through `ItemTank` and `FluidAmmo`: `gun.bio_gun.reload`, `gun.tool.reload`, `gun.power_hammer.shockwave` (rounds spent per block) |

Recipes appear in the GT maps NEI reads (assembler, Assembly Line). As in the M2 audit, NEI's own display is not checked headless; that is tracked as gap G3 below, not as an M5 box.

Verdict: met headless.

## 2. Explosions respect config and protection (PFX-14, PFX-25, CORE-19), with tests

Met. The full table (`ExplosionGateTest.doesNotReproducePFX25_...`, `doesNotReproduceCORE19_...`, `doesNotReproducePFX14_...`, the mobGriefing gate, the audit log line) is in [Explosion detail](#explosion-detail-piece-i9) below. The weapons that use the rules:

| Rule | Weapon scenario |
|---|---|
| Block damage off keeps blocks | `gun.nuke_launcher.safe`, `gun.grim_reaper.direct`, `gun.alien_blaster.blast` |
| Block damage on breaks blocks | `gun.grim_reaper.blocks`, `fx.stress.nuke` (a real nuke into a dirt mass) |
| Protection veto (`BreakEvent`) | `grenade.protected`; `gun.power_hammer.shockwave_denied`, `gun.tool.break_denied` (tool guns break through `BlockBreakGuard`) |
| Safe mode and `PlaceEvent` (PFX-28) | `gun.bio_gun.blob.safe`, `gun.bio_gun.blob.veto` |
| Fire only when block damage is on (GUN-05) | `gun.nether_blaster.fire.safe`, `gun.alien_blaster.blast` |
| Nuke scale: 1500 blocks per tick, time-sliced | `explosion.nuke.sliced`, `gun.nuke_launcher.fire` |

Merges: `613f4e26` (I9), `009434a7`. Verdict: met headless.

## 3. The particle and FX system carries the FX catalog, and performance is checked

| Check | Evidence | Where |
|---|---|---|
| The M5 catalog is shipped as data (14 lists, 44 systems) | `FxCatalogTest`, `FxParserTest`, `FxRegistryTest` (fails on a slow-path class, GGS-04-07); scenario `fx.core` | headless |
| The budget holds under load | `ParticleBudgetTest` (500 emitters), `FxStressTest`; `fx.stress` (50 catalog emitters for 200 ticks: live never passes the 2000 ceiling, spilled 0) and `fx.stress.nuke` (a nuke and a salvo of three, 300 ticks) | headless |
| FX packets | `FxPacketsTest` (encode and decode), `BlastFxSelectorTest` | headless |
| Weapon FX actually fire | `gun.alien_blaster.trail` (PFX-01: a tracked entity), `projectile.laser.hit` and `projectile.tesla.chain` (beam visuals) | headless |
| What the FX look like, and the frame cost with many emitters on screen | none | client |

Merges: `f5dba9a7` (I7), `685677d9` (I8). The headless checks prove the particle count stays within the ceiling and that the catalog parses. They do not measure frame time. The box asks for performance checked with many emitters on screen, so it stays unticked until the batched client session reads it (see [Pending batched client check](#pending-batched-client-check)).

Verdict: not met.

## Per-weapon table

Ammo: EU means an IC2 electric item charged by a GT charger of exactly the tier. Recipe ID is the `ProductionTable` row or Assembly Line spec.

| Gun | Tier | Ammo | Recipe ID | Scenarios | Unit tests |
|---|---|---|---|---|---|
| PDW | EV | Advanced Magazine (20) | GR-60 | `gun.pdw.auto`, `.reload`, `.reload.forced`, `.recipe`, `.gate`, `.burst` | `EvBallisticLogicTest`, `ShotPatternTest` |
| Pulse Rifle | EV | Advanced Magazine (12) | GR-61 | `gun.pulse_rifle.auto`, `.reload`, `.reload.forced`, `.recipe`, `.gate`, `.burst`, `.zoom` | `EvBallisticLogicTest` |
| Minigun | EV | Minigun Ammo Drum (200) | GR-62 | `gun.minigun.auto`, `.reload`, `.reload.forced`, `.recipe`, `.gate`, `.rate` | `EvBallisticLogicTest` |
| Laser Rifle | EV | EU | GR-70 | `gun.laser_rifle.recipe`, `.gate`, `.fire`; `projectile.laser.hit`, `.blocks` | `EvEnergyGunsTest`, `HitscanTest` |
| Tesla Rifle | EV | EU | GR-71 | `gun.tesla_rifle.recipe`, `.gate`, `.fire`; `projectile.tesla.chain`, `.wall` | `EvEnergyGunsTest`, `ChainSelectTest` |
| Sonic Rifle | EV | EU | GR-72 | `gun.sonic_rifle.recipe`, `.gate`, `.fire`; `gun.pattern` | `EvEnergyGunsTest`, `ShotPatternTest` |
| Bio Gun | EV | Bio Tank (30) | GR-65 | `gun.bio_gun.fire`, `.charge`, `.reload`, `.blob`, `.blob.safe`, `.blob.veto`, `.recipe`, `.tiergate`; `gun.charge.hold` | `BioGunLogicTest`, `ChargeLogicTest` |
| Power Hammer | HV | Air Tank (300) | GR-91 | `gun.power_hammer.shockwave`, `.shockwave_denied`, `.shockwave_hit`, `.recipe`; `gun.tool.*` | `ToolGunDefinitionsTest`, `ToolStatsTest`, `ToolDrainTest` |
| Power Hammer (Advanced) | EV | Air Tank (300) | GR-92 (upgrade of a Power Hammer) | `gun.power_hammer_adv.shockwave`, `.upgrade` | as the hammer |
| Chainsaw | HV | Fuel Tank (300) | GR-90 | `gun.chainsaw.cut`, `.left_click`, `.recipe`; `gun.tool.mining`, `.melee` | `ToolGunDefinitionsTest`, `ToolDrainTest` |
| Blaster Rifle | IV | EU | GR-80 | `gun.blaster_rifle.fire`, `.hit`, `.recipe`; `gun.energy_iv.tier_gate` | `EnergyBatchDefinitionsTest` |
| De-Atomizer Pistol | IV | EU | GR-81 | `gun.deatomizer_pistol.fire`, `.hit`, `.recipe`; `gun.energy_iv.tier_gate` | `EnergyBatchDefinitionsTest` |
| Akimbo De-Atomizer Pistols | IV | EU (sum of both) | none (crafting-table combine of two pistols) | `gun.akimbo_deatomizer_pistols.fire`, `.combine` | `EnergyBatchDefinitionsTest`, `AkimboNbtTest` |
| Scatter Laser Rifle | IV | EU | GR-82 | `gun.scatter_laser_rifle.fire`, `.hit`, `.recipe`; `gun.pattern` | `EnergyBatchDefinitionsTest`, `ShotPatternTest` |
| Nether Blaster | LuV | Nether Charge (10 shots, no empty item) | GR-100 (Assembly Line; ammo RC-36) | `gun.nether_blaster.fire`, `.reload`, `.fire.block`, `.fire.safe`, `.recipe`; `production.ammo_line` | `NetherBlasterDefinitionTest` |
| Alien Blaster | LuV | EU | GR-95 | `gun.alien_blaster.fire`, `.hit`, `.blast`, `.trail`, `.recipe`, `.tiergate` | `AlienBlasterDefinitionTest` |
| Grim Reaper | LuV | Rocket (4 loaded) | GR-97 (upgrade of a Rocket Launcher) | `gun.grim_reaper.reload`, `.volley`, `.direct`, `.blocks`, `.recipe`, `.upgrade`, `.tiergate` | `GrimReaperDefinitionTest` |
| Nuclear Death Ray | ZPM | EU (20,000,000 per beam tick) | GR-103 (Assembly Line) | `gun.nuclear_death_ray.fire`, `.drain`, `.recipe`, `.tiergate`; `projectile.beam.line` | `BeamMathTest`, `MovingBeamTest`, `BeamSweepTest` |
| Nuke Launcher | ZPM | Tactical Nuke Rocket (1; ammo RC-35) | GR-102 (Assembly Line) | `gun.nuke_launcher.reload`, `.fire`, `.recipe`, `.safe`; `fx.stress.nuke`; `explosion.nuke.sliced` | `NukeLauncherDefTest` |

Models: every row has a clean-room profile (a `*ProfileTest` for each gun). The Nuke Launcher reuses the Rocket Launcher geometry with its own skin.

## Deviations

From the [02-guns index](../02-guns/index.md) sections for batches B1, B3a, B4, B5 and B6, and from OPEN-ITEMS rows 63 to 76.

| Topic | Deviation | Source |
|---|---|---|
| Plan questions (row 64) | Laser Rifle is an instant hitscan with a beam visual. Energy guns have no reload; the reload key does nothing and the HUD shows charge. The other questions took the plan's recommended answers | PLAN-M5 section 5 |
| Nuclear Death Ray (Q2) | Runs on EU, not cells: 20,000,000 EU per beam tick from the ZPM baseline of 10,000,000,000 EU. The Nuclear Power Cell is a component and a radiation item, not ammo | index, B6 |
| Energy shot cost (Q9) | Capacity is the tier baseline (IV 100M, LuV 1B, ZPM 10B EU); one shot is capacity over the sheet's clip. Tunable by config; to be tuned in game | index, B4, B5, B6 |
| Recipes | Alien Blaster has no Techguns recipe and is a plain LuV Assembler row (GR-95) rather than the sheet's Assembly Line chain. Scatter Laser Rifle has no Techguns recipe and takes the `recipes.md` IV row. NDR and Nuke Launcher are Assembly Line specs because their inputs exceed the assembler's slots | index, B4, B5, B6 |
| Solder (rows 71 and 76) | First omitted, then restored by `Row.molten` and `Row.fluid` (merge `fc6b5f05`): 144 L on GR-61 and GR-70 to GR-72, 288 L on GR-80 to GR-82. PDW, Minigun, Bio Gun and the upgrades list none | index, B1, B4 |
| Blaster Rifle lens | The glass lens stays dropped, because solder freed no input slot | index, `BlasterRifleDef` |
| Burst lines | PDW and Pulse Rifle spend one round for a 2 or 3 projectile line with one shared spread roll | index, B1 |
| Minigun | The start sound is implemented (commented out in Techguns); the spin-down ramp is not built, as it needs a persistent spin phase in the model state | index, B1; row 65 |
| Bio Gun | Poison IV is raised only through the POISON type's own effect, so full hazmat blocks it; blobs obey `explosions.blockDamage` and `PlaceEvent` (PFX-28); no charge sound (the original plays none); the first-person charge shift is drawn (client pass to confirm) | index, B3a |
| Charge fixes | GUN-02 (ammo less than level), GUN-10 and GUN-31 do not reproduce | index, `ChargeLogicTest` |
| Scatter Laser Rifle | The fan ignores the main spread of 0.1 (Techguns rolled one beam at 0.1 and four at 0.15) | index, B4 |
| Alien Blaster | `AlienBlasterProjectile` is a registered, tracked entity so its trail shows (PFX-01); the blast is power 0.5 and server side only (PFX-15); only a player's bolt ignites | index, B5 |
| NDR sounds | The beam plays its own start and loop sounds, so the per-shot fire sound is suppressed; the `fire` and `reload` events exist only to satisfy the sound table | index, B6 |
| Akimbo De-Atomizer | Combine and split conserve EU (the second gun takes the odd EU); no machine row, so the tier is the input gun's | index, B4 |
| Chained GT machine explosions (Q4) | GT behavior kept; `explosions.machineExplosions=false` leaves machines standing | Explosion detail below |
| De-Atomizer flash tint | Built: the blue flash sheet is tinted (0.2, 1.0, 0.7) through `MuzzleFlashSpec.tint`; visual check pending the client pass | index, B4 |
| Models | AUG stays at 0.914 worst view (below the 0.93 bar), accepted for the MVP. The other M5 models passed after round 3 (row 69) | OPEN-ITEMS 63, 65, 69 |
| Landing overrides | N4 and I5 landed with a recorded manual override for the `gun.kind` and `gun.bio_gun.fire` pair; fixed in `975d71f4` (rows 74, 75). Closed | OPEN-ITEMS 74, 75 |

## Pending batched client check

These need a running client and stay open. They are the reason box 3 is unticked.

| Item | What to check |
|---|---|
| In-hand fit | The M5 guns use `InHandTargets` and `InHandFit` (row 68). Screenshot each gun in first and third person, and the left Akimbo De-Atomizer gun |
| FX visuals | The 14 catalog lists on screen: laser, tesla, bio, blaster, alien, nether, sonic, rocket and nuke FX; nuke FX out to 150 blocks |
| NDR starburst pulse | The firing blade pulses 0.8 to 1.0, one cycle per fire. A shot every tick may restart the recoil window each tick, so the pulse may never pass its first quarter in a held burst (see the B6 deviation) |
| Alpha cut-outs | The Scatter Laser Rifle sight window (the renderer turns alpha test on for a part) and the Grim Reaper cage |
| Frame cost | Real frame time with many emitters on screen, at the default 2000-particle ceiling, including a nuke |
| Minigun rotor | Rotor turn under `checkRecoil` and the missing spin-down |
| Energy tuning | EU per shot feel (Q9) and the HUD charge line |
| NEI | The M5 recipes listed in NEI (M2 gap G1) |

## Gap list

| ID | Gap | Kind | Status |
|---|---|---|---|
| G1 | Box 3: frame cost with many emitters on screen | pending client | Pending the batched client session |
| G2 | In-hand fit, FX visuals, NDR pulse, alpha cut-outs | pending client | Pending the batched client session |
| G3 | NEI display of the M5 recipes | pending client | Pending the batched client session |
| G4 | Minigun spin-down, Bio Gun charge shift, De-Atomizer flash tint | Bio Gun shift and De-Atomizer tint built (client check pending); Minigun ramp not built | Owner (2026-10-20): build all three before the beta |
| G5 | AUG model at 0.914 | assets | Accepted for the MVP by the owner (2026-10-20) |
| G6 | GGS-02-32 fuel-value scaling for the Chainsaw | deferred (Q7) | Revisit after M5 |

---

## Explosion detail (piece I9)

Evidence for the roadmap line "Explosions respect config and protection (fixing PFX-14, PFX-25 and CORE-19), with tests", plus the nuke-scale rules the Nuke Launcher (ZPM) needs.

### Gate evidence

| Item | Rule | Test |
|---|---|---|
| PFX-25 | The owner stays in the victim list unless `explosions.selfDamage` is off (`ExplosionRules.victims`) | `ExplosionGateTest.doesNotReproducePFX25_ownerHurtByOwnBlast`; scenario `grenade.protected` |
| CORE-19 | A player's push travels in an `S27PacketExplosion` with an empty block list (`ProjectileExplosion.pushPacket`) | `ExplosionGateTest.doesNotReproduceCORE19_playerPushPacketCarriesNoBlocks` |
| PFX-14 | A block whose `BlockEvent.BreakEvent` is cancelled is kept (`BlastSlicer.keepAllowed` models the listener; `BlockBreakGuard` posts the real event) | `ExplosionGateTest.doesNotReproducePFX14_cancelledBreakEventKeepsBlock`; scenario `grenade.protected` (a real cancelled BreakEvent) |
| mobGriefing | An NPC-caused blast needs the `mobGriefing` gamerule, a player's does not; config off always wins | `ExplosionGateTest.mobGriefingGatesNpcBlastsOnly` |
| Audit log | One line per blast that broke blocks, logger `gregguns.explosions`: dimension, position, radius, shooter, weapon, blocks | `ExplosionGateTest.auditLineNamesDimensionPositionRadiusShooterWeaponAndBlocks` |

### Nuke scale

- **Per-tick cap.** `ExplosionRules.BLOCKS_PER_TICK` (1500). `ProjectileExplosion` breaks that many blocks in the blast's first tick, nearest the center first, and hands the rest to `BlastTicker`, which breaks one batch per queued blast at the end of each server tick. A radius 10 sphere is about 4200 blocks, so three ticks. Entity damage and the `Detonate` filter are unaffected and happen at once. A queued block still goes through `BlockBreakGuard` when it is broken, so a late veto still keeps it. Queued work is dropped if its world unloads. Tests: `ExplosionGateTest.slicerNever...`, `aRadiusTenSphere...`; scenario `explosion.nuke.sliced`.
- **150-block FX packet.** A blast with power at or above `NUKE_FX_MIN_SIZE` (8) sends `PacketBlastFx` (center and radius) to every player within `NUKE_FX_RANGE` (150 blocks), past the roughly 64 blocks vanilla particles reach. The client side is `BlastFxHooks.sink()`, a no-op until the FX system (I8) installs the flash and cloud.

### Decision: chained GT machine explosions (Q4)

GregTech behavior is kept: blocks in a blast get `onBlockExploded`, so a charged GT machine can explode and chain. The audit line counts every block, machines included. A server owner can set `explosions.machineExplosions=false` to leave GregTech machines (any block whose tile entity is an `IGregTechTileEntity`) standing. Default true. Synced.

### Tool guns

The Power Hammers and Chainsaw (piece I11, `ItemToolGun`) break blocks through `BlockBreakGuard.mayBreak`; `gun.power_hammer.shockwave_denied` and `gun.tool.break_denied` cover the veto.
