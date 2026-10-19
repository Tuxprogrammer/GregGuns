# M2 exit gate audit

Evidence for the four M2 exit-gate boxes in [ROADMAP.md](../../ROADMAP.md#m2--ballistic-arsenal). Audited on master at `7f3d8cb` (batches B1 to B6 merged), over every entry of `GunDefinitions.ALL` (24 guns, akimbo variants included), the three grenades and the two melee items. Scenario names and what each asserts are in [docs/testing/README.md](../../testing/README.md). The table-driven `GunDefinitionsTest` holds every gun to the same rules with no per-gun edit: ammo ids exist, assembly rows pass `GateRule` with distinct circuits per tier, fire and reload sound events exist in `sounds.json`, camo count equals the texture files on disk, and `ClientGunDefinitions.FLAT_ICON_FALLBACK` is empty (every gun has a clean-room profile).

"Deviation" below means a section of [index.md](index.md). The Techguns sheets are in [sheets-handguns-smgs.md](../../techguns-spec/02-guns/sheets-handguns-smgs.md), [sheets-rifles-shotguns.md](../../techguns-spec/02-guns/sheets-rifles-shotguns.md) and [sheets-launchers-tools-grenades.md](../../techguns-spec/02-guns/sheets-launchers-tools-grenades.md).

## 1. Every gun matches its spec sheet, or a deviation is recorded

Stats are the sheets' figures unchanged for every gun, and a unit test pins them. Differences are recorded in the deviation sections.

| Gun | Tier | Stats pinned by | Deviation notes |
|---|---|---|---|
| `pistol_9mm`, `revolver`, `sawed_off`, `handcannon` | LV, LV, LV, Steam (ULV) | `GunCatalogTest` (one `...MatchesItsSheet` test each, plus GUN-16), `gun.pistol.damage` | [Pistol, Revolver, Sawed Off and Hand Cannon](index.md#pistol-revolver-sawed-off-and-hand-cannon-m1-and-early-m2-deviations) |
| `bolt_action_rifle` | LV | `GunCatalogTest.boltActionMatchesItsSheet` | [Bolt Action Rifle](index.md#bolt-action-rifle-m2-deviations) |
| `thompson_smg`, `mac10`, `m4`, `ak47` | MV | `MvGunsLogicTest` | [B1](index.md#mv-rifles-and-smgs-batch-b1-m2-deviations) |
| `combat_shotgun`, `akimbo_pistols`, `akimbo_revolvers` | MV, LV, LV | `ShotgunAndAkimboLogicTest` | [B2](index.md#combat-shotgun-and-the-akimbo-pair-batch-b2-m2-deviations) |
| `rocket_launcher`, `grenade_launcher` | HV | `LauncherDefinitionsTest`, `LauncherMathTest` | [B5](index.md#launchers-batch-b5-m2-deviations) |
| `as50`, `lmg`, `akimbo_mac10s` | HV, HV, MV | `HeavyGunLogicTest` | [B4](index.md#as50-lmg-and-akimbo-mac10s-batch-b4-m2-deviations) |
| `aug`, `scar`, `vector`, `infiltrator` | HV | `HvRiflesLogicTest` | [B3](index.md#hv-rifles-and-smg-batch-b3-m2-deviations) |
| `flamethrower`, `golden_revolver`, `akimbo_golden_revolvers` | HV, IV, IV | `FlamethrowerLogicTest`, `GoldenRevolversLogicTest` | [B6](index.md#flamethrower-golden-revolver-and-akimbo-golden-revolvers-batch-b6-m2-deviations) |
| Stielgranate, Frag Grenade, Flashbang | LV, MV, MV | `GrenadeMathTest`, `FlashMathTest`, `GrenadeTableTest` | [grenades.md](../04-projectiles-fx/grenades.md) |
| Combat Knife, Crowbar | LV | `MeleeMathTest`, `MeleeTableTest` | the melee rows in [recipes.md](recipes.md) |

## 2. Every recipe is tier-gated and visible in NEI

| Check | Evidence |
|---|---|
| Every gun's assembly row passes `GateRule` | `GunDefinitionsTest.everyAssemblyRowMakesItsGunAndMeetsTheGateRule` over all rows, plus `GunTableTest`, `GateRuleTest` and `RecipeTierCheckTest`; the dev self-check's tier rule runs at every scenario boot |
| Grenade and melee rows are gated | `GrenadeTableTest.everyRowMeetsTheAluminiumGate`, `MeleeTableTest.everyRowMeetsTheSteelGate` |
| Hand Cannon (crafting table, ULV) | Bronze gate through the Stone Barrel (RC-01) and the stone bullet row (RC-27); `gun.handcannon.recipe` |
| Akimbo guns (crafting-table combine, no machine row) | The tier is the inputs' tier, recorded on [0001](../../design/0001-tier-map.md); `gun.akimbo_*.combine` |
| Upgrade rows (Infiltrator, Golden Revolver) | `gun.infiltrator.upgrade`, `gun.golden_revolver.upgrade` |

| Guns | `*.recipe` scenario (the GT map holds the row at its tier and not below it) |
|---|---|
| Pistol, Revolver, Sawed Off, Hand Cannon, Bolt Action | `gun.recipe`, `gun.revolver.recipe`, `gun.sawedoff.recipe`, `gun.handcannon.recipe`, `gun.boltaction.recipe` |
| Thompson, Mac10, M4, AK47, Combat Shotgun | `gun.<name>.recipe` |
| AUG, SCAR, Vector, Infiltrator, AS50, LMG | `gun.<name>.recipe` |
| Rocket Launcher, Grenade Launcher, Flamethrower, Golden Revolver | `gun.<name>.recipe` |
| The four akimbo guns | `gun.<name>.combine` (shapeless crafting recipes, which NEI lists) |

**NEI visibility is pending the batched in-game session.** No headless NEI check exists. The scenarios above assert the GT recipe maps NEI reads from (`assemblerRecipes`, `gregguns.ammoPress`), not NEI's own handler. This box stays unticked.

## 3. Unit tests cover each gun's unique behavior

| Unique behavior | Unit test | Scenario |
|---|---|---|
| Shotgun pellets (8 from one round, seeded fan) | `ShotgunAndAkimboLogicTest.oneRoundFiresEightPelletsWithTheCentreAtMainSpreadAndSevenAtPelletSpread`, `theSameSeedRollsTheSamePellets` | `gun.pellets`, `gun.sawedoff.fire` |
| Sawed Off shell-by-shell reload | `GunCatalogTest.sawedOffLoadsShellByShellAndKeepsItsRounds` | `gun.sawedoff.reload`, `.reload.forced` |
| Bolt rechamber and scope | `GunCatalogTest.boltActionLoadsAllSixRoundsFromOneRifleBullet`, `BoltActionRifleProfileTest` | `gun.boltaction.rechamber`, `.zoom` |
| Hand Cannon gravity ball and stone bullet | `GunCatalogTest.handCannonLoadsOneStoneBulletWithNoContainerAndNoRefund` | `gun.handcannon.fall`, `.damage` |
| Revolver loading and drop-off | `GunCatalogTest.revolverLoadsStraightFromOneBulletWithNoContainer` | `gun.revolver.damage`, `.reload.forced` |
| Fire intervals, magazine swap and refund (Thompson, Mac10, M4, AK47) | `MvGunsLogicTest` | `gun.<name>.auto`, `.reload`, `.reload.forced` |
| Scopes, SCAR clip, Vector magazine, Infiltrator silenced and upgrade | `HvRiflesLogicTest` | `gun.<name>.zoom`, `gun.infiltrator.upgrade` |
| AS50 single shot and drop-off, LMG sustain, Akimbo Mac10 double shot | `HeavyGunLogicTest` | `gun.as50.zoom`, `gun.akimbo_mac10s.fire` |
| Akimbo alternation, combine and split, camo kept (GUN-26) | `ShotgunAndAkimboLogicTest`, `AkimboNbtTest` | `gun.akimbo_*.combine`, `.fire` |
| Rocket direct hit versus splash, PFX-14, PFX-25 | `LauncherMathTest`, `LauncherDefinitionsTest` (GUN-04) | `gun.rocket_launcher.direct`, `.wall.*` |
| 40 mm grenade bounce and fuse | `LauncherMathTest.aBounceTakesTwentyTicksOffTheFuseAndNeverBelowOne` | `gun.grenade_launcher.bounce`, `.direct`, `.blocks` |
| Flamethrower tank, start sound, ignite rules (GUN-05, GUN-13) | `FlamethrowerLogicTest`, `FlameRulesTest` | `gun.flamethrower.*` |
| Golden Revolver, 16 against 14 pair, upgrade | `GoldenRevolversLogicTest` | `gun.golden_revolver.upgrade` |
| Grenade fuse, bounce and settle | `GrenadeMathTest` | `grenade.fuse`, `grenade.bounce` |
| Frag blast and fragments, Flashbang blindness | `GrenadeMathTest`, `FlashMathTest` | `grenade.frag`, `grenade.flash`, `grenade.flash.noblocks` |
| Knife and Crowbar damage and tool class | `MeleeMathTest` | `melee.knife`, `melee.crowbar` |
| Burst fire mode | `FireControlTest` (no M2 gun is burst-fed; the mode is tested in the framework) | none |

## 4. Original textures and sounds are in place, following D6

| Asset | Evidence |
|---|---|
| Model textures | All 21 non-akimbo guns have `textures/models/guns/<name>.png`; akimbo guns reuse their single gun's sheet (`GunDefinitionsTest.camoCountMatchesTheTextureFilesOnDisk`). Item icons exist for all 24 guns, the grenades and the melee items. Generated by `tools/asset-gen`; logged in [CREDITS.md](../../assets/CREDITS.md) and [MANIFEST.md](../../assets/MANIFEST.md) |
| Models | Every gun has a clean-room profile (`FLAT_ICON_FALLBACK` is empty, pinned by `theFlatIconFallbackListsExactlyTheGunsStillWithoutAModel`). Layouts and role maps are in `tools/asset-gen/gun-paint/` |
| Clean-room notes | `techguns-spec/03-gun-visuals-audio/model-notes/` has notes for the pistol, revolver, sawed off, bolt action, Thompson, Mac10, M4 (with the Infiltrator delta), AK47, Combat Shotgun, AS50, LMG, AUG, SCAR, Vector, Rocket Launcher, Grenade Launcher and Flamethrower. The Golden Revolver reuses the revolver's notes and profile. The Hand Cannon has no notes file (gap G3) |
| Sounds | Every `sounds.json` event resolves to an `.ogg` that exists (checked for all 59 events; `GunSoundsJsonTest`, `GunDefinitionsTest.everyGunOwnsItsFireAndReloadSoundsAndRechamberWhenItHasOne`). The sounds are generated by `tools/asset-gen`, none copied |
| Sound review state | In `tools/asset-gen/sound-status.json` at round 7, 91 of the 112 event files are keep or accept; 20 are at `tweak` (below the accept tier) and `flashbang_bang.ogg` has no gate entry yet (gap G4) |

## Gap list

| ID | Gap | Kind | Status |
|---|---|---|---|
| G1 | NEI visibility of every recipe needs a client | pending client | Pending the batched in-game session |
| G2 | The Pistol, Revolver, Sawed Off and Hand Cannon had no deviation section | docs | Fixed in this change |
| G3 | No `model-notes/handcannon.md`: the Hand Cannon model predates the clean-room notes process, and its originality record is in the `HandCannonProfile` Javadoc | docs and assets | Open. A notes agent writes the notes; a fresh builder rebuilds only if the owner wants strict process parity |
| G4 | 20 sound files are at `tweak` (revolver, handcannon, bolt action, Thompson, AK47, Vector, Mac10 shot c, Combat Shotgun shot b, AS50 shot b, LMG and grenade launcher reloads, the akimbo and golden reloads, grenade pin a and b), and `flashbang_bang.ogg` is not gated | assets | Open, owned by the sound review loop. The files exist and are original |
| G5 | Code comments still describe the fallback list as pending: the Javadoc on `GunDefinitionsTest.theFlatIconFallbackListsExactlyTheGunsStillWithoutAModel` ("While the four B3 guns await...") and on `ClientGunDefinitions.FLAT_ICON_FALLBACK` | code | Open, comment only; not changed here |
| G6 | `ModulesConfig.guns` still defaults to false; ROADMAP M0 says it flips at the M2 exit gate | code | Open; flips once the NEI session passes |
