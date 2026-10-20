# M1 exit gate audit

Evidence for the five M1 exit-gate boxes in [ROADMAP.md](../../ROADMAP.md#m1--combat-core-the-vertical-slice), for the beta review gate. Audited on master at `354236f4`, by reading `src/`, `src/test`, `docs/` and the git history. Gradle was not run, so test results come from the commit records named below; every test class named here was confirmed to exist. Scenario names and what each asserts are in [docs/testing/README.md](../../testing/README.md), whose "M1 coverage" table maps each box to its tests. The pistol is also audited as one row of the [M2 exit gate](m2-exit-gate.md).

Result: 5 met, 0 partially met, 0 unmet. Two met boxes carry a caveat (boxes 1 and 4), and the module flag default is an M0 and M2 follow-up.

## 1. The pistol is craftable only through its D1 tier gate, and the recipe shows in NEI

**Met.**

| Check | Evidence |
|---|---|
| Gate rule | `GunTableTest` (`pistolMeetsTheD1GateRule` uses `GateRule`), `GateRuleTest`, `GunDefinitionsTest.everyAssemblyRowMakesItsGunAndMeetsTheGateRule` |
| Tier check | `RecipeChecksTest`, `RecipeTierCheckTest`; the dev self-check runs the tier rule over every registered recipe on every boot (commit `cec4da4f` records 6 items and blocks, 28 recipes) |
| Scenarios | `gun.recipe` (the Assembler map holds the pistol at LV and not below it), `production.recipes` (RC-28, RC-37, RC-44 at LV; RC-05 and RC-11 on the Assembler), `production.ammopress` |
| Production chain slice | LV Ammo Press MTE 20101 (`AMMO_PRESS_LV` in `GregGunsMTEIDs`), `MTEAmmoPress`, map `gregguns.ammoPress`; commit `af2cfc01`; [ammo-press.md, M1 slice deviation](../08-machines-production/ammo-press.md#m1-slice-deviation) |
| NEI | Caveat. The only evidence is the commit record of `cec4da4f` ("Screenshots show the creative tab and the pistol's NEI recipe") from the `mp.visual` scenario. The screenshots are not in the repo, and the headless scenarios assert GT's recipe map, not NEI's handler (the same limit the [M2 audit](m2-exit-gate.md#2-every-recipe-is-tier-gated-and-visible-in-nei) records as G1) |

## 2. Fire, reload and zoom work, and the pistol hits and damages mobs and players

**Met.**

| Check | Evidence |
|---|---|
| Fire | `gun.fire` (bullet launched, zombie loses the `DamageModel` figure, ammo 18 to 17, GUN-29 buffer), `gun.dryfire` |
| Reload | `gun.reload` (35 tick reload, magazine consumed and empty one returned), `gun.reload.forced` |
| Zoom | `gun.zoom` checks that the unscoped pistol refuses to zoom (GUN-47). A working scope is proven by M2's `gun.boltaction.zoom`, and by `SpreadAndZoomTest` and `ZoomAnimatorTest`. The pistol has no scope in the spec, so "zoom works" is met by the framework and its scoped guns |
| Hits mobs and players | `gun.player` (a fake player takes the `DamageModel` figure), `projectile.hit`, `projectile.falloff`, `projectile.blocks`, `projectile.shooter`, `combat.penetration`, `combat.player`, `combat.iframes` |

## 3. Unit tests cover the damage formulas, drop-off, spread, ammo accounting and the reload state machine

**Met.**

| Area | Test classes |
|---|---|
| Damage formulas | `DamageModelTest`, `DamageTypeTest`, `DamageSourceTest` |
| Drop-off | `DamageFalloffTest` (and `projectile.falloff`) |
| Spread | `SpreadAndZoomTest` |
| Ammo accounting | `StoreAmmoInventoryTest`, `MagazineLogicTest`, `GunNbtTest`, `AmmoIdsMatchItemsTest`, `GunControllerTest` |
| Reload state machine | `ReloadStateMachineTest`, `GunControllerTest` |
| Fire control | `FireControlTest` (semi, auto, burst, cooldown) |

All exist under `src/test`. Later milestones added `GunCatalogTest` and the per-gun logic suites on top.

## 4. A dedicated server with two clients shows correct sync

**Met.** Caveat: the run is dated to M1 and no later full re-run is recorded.

| Check | Evidence |
|---|---|
| Scenarios | `mp.sync` (A fires twice through the real input path and trigger packet and reloads; B takes damage; both clients log what they see), `mp.visual`, `mp.give` |
| Orchestrator | The `mp_check` run described in [docs/testing/README.md](../../testing/README.md): a server plus clients Alice and Bob, with any failure signature in a log aborting the run |
| Result record | Commit `cec4da4f`: "the two-client mp_check run (mp.sync, mp.visual, mp.give, client log evidence for remote shots, reloads and sounds)"; merge `a224d202` (m1/mp-check) |
| Other players' animations and sounds | `GunSoundEventsTest`, `GunSoundMathTest`, `ReloadSoundGateTest` and the client animation tests under `client/anim`; sound deviations in [gun-sound-pipeline.md](../03-gun-visuals-audio/gun-sound-pipeline.md) |
| Later runs | M3 recorded `mp.slots` with a relog (2026-10-18); the beta client pass should repeat the whole check |

## 5. Each relevant inherited bug has a test showing it is not reproduced

**Met.** The bugs the gate names:

| Bug | Test |
|---|---|
| GUN-13 | `ReloadStateMachineTest.doesNotReproduceGUN13_*`, `StoreAmmoInventoryTest.doesNotReproduceGUN13_forcedReloadKeepsLoadedRounds`, `gun.reload.forced` |
| GUN-29 | `FireControlTest.doesNotReproduceGUN29_semiClickDuringCooldownIsRemembered`, `gun.fire` |
| GUN-32 | `FireControlTest.doesNotReproduceGUN32_*` (2). The packet path is covered by `mp.sync`, not by a headless scenario |
| GUN-33 | `combat.iframes` only; the fix lives in the Forge handlers (`CombatEvents`), so there is no pure unit test |
| GUN-34 | `DamageModelTest.doesNotReproduceGUN34_*`, `combat.penetration` |

Beyond the gate's list, the coverage table records tests for GUN-01, 06, 07, 11, 12, 16, 18, 19, 35, 37, 39, 40, 43, 44, 45, 47, PFX-01, 03, 07, 08, 23, 27, 34, and GUN-38 (fixed in M2). GUN-36 is not applicable.

## Deviations recorded against the spec

| Area | Deviation | Where |
|---|---|---|
| Guns | NPC attack-time variance dropped; GUN-16 not reproduced; revolver, sawed-off and hand cannon loading; pellets; Hand Cannon tier and recipe; Assembler rows with circuits 1 to 3 | [index.md, Pistol, Revolver, Sawed Off and Hand Cannon](index.md#pistol-revolver-sawed-off-and-hand-cannon-m1-and-early-m2-deviations) |
| Damage | Event ordering, armor settled once, penetration travels with the source, vanilla armor is plain (GUN-45), i-frames (GUN-33), fire flag (GUN-44); worked example 5.7 changes to 3.92 | [damage-interop.md, Settled in M1](damage-interop.md#settled-in-m1-implementation-notes-and-deviations) |
| Projectiles | Shooter grace of 5 ticks, chunk coverage stop, nearer of block and entity wins, passable blocks, start position in NBT, gravity synced, no revenge or team factor yet (M7) | [Projectile framework deviations (M1)](../04-projectiles-fx/index.md#projectile-framework-deviations-m1) |
| Sound | Other players hear a reload where it started; the shooter's own reload sound follows them and is cut off | [gun-sound-pipeline.md](../03-gun-visuals-audio/gun-sound-pipeline.md) |
| Ammo Press | LV press only; stroke-shaped bar, animation, sound and TESR wait for M4; no programmed circuit on the `gregguns.ammoPress` map; bullet exempt from the gate-material rule (GGS-06-36); RC-51 refill is shapeless | [ammo-press.md, M1 slice deviation](../08-machines-production/ammo-press.md#m1-slice-deviation) |
| Ammo | Tooltips list compatible magazines and guns | [06-items/ammo.md](../06-items/ammo.md) |

## Open polish items

| ID | Item | Kind |
|---|---|---|
| P1 | No `TODO` or `FIXME` in `src/main/java` or `src/test/java` | clean |
| P2 | Damage open questions: the hazmat mapping (GGS-02-22) may be narrowed in M9 balance; a reused source reports `isUnblockable()` true between hits; a foreign `ISpecialArmor` that ignores `isUnblockable()` would reduce damage twice on the player path ([damage-interop.md](damage-interop.md)). None seen in the pack | open, docs |
| P3 | GUN-33 has only a scenario, no unit test; the packet path of GUN-32 has only the two-client run | accepted limit |
| P4 | The sub-bullet remainder (at most 5 rounds) is lost on a forced reload (GUN-13 rounding) | recorded deviation |
| P5 | The Angelica HUD-caching trace in [hud-overlays.md](../01-core/hud-overlays.md) was due when M1 built the ammo HUD | open, docs |
| P6 | `ModulesConfig.guns` still defaults to `false` (M2 gap G6), so the pistol is not craftable in a default install | see D-2 in the [M0 audit](../01-core/m0-exit-gate.md) |

## Needs owner decision

No M1 box is unmet. Items to sign off:

| ID | Item | Proposed resolution |
|---|---|---|
| D-1 | Box 1: NEI visibility rests on a commit message, with screenshots not kept in the repo | Re-prove it in the batched client pass of the beta gate (the `mp.visual` NEI shots for the pistol and the bullet), and keep the shot list in the pass report. Same session as M2 G1 |
| D-2 | Box 4: two-client sync was last run at M1 | Repeat `mp.sync`, `mp.visual` and `mp.give` in the batched client pass, now that M2 to M8 content is in |
| D-3 | Module flag default (P6) | Decided under D-2 of the M0 audit |
| D-4 | Box 2: the pistol's own zoom | Accept as met through the scoped guns, since the pistol has no scope in the spec |
