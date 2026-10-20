# M0 exit gate audit

Evidence for the five M0 exit-gate boxes in [ROADMAP.md](../../ROADMAP.md#m0--foundation), for the beta review gate. Audited on master at `354236f4`, by reading `src/`, `src/test`, `docs/`, `.github/` and the git history. Gradle was not run for this audit, so test results are taken from the commit records named below; the test classes and methods named here were confirmed to exist. Scenario names and what each asserts are in [docs/testing/README.md](../../testing/README.md).

Result: 3 met, 2 partially met, 0 unmet.

## 1. `./gradlew spotlessApply build` passes, with at least one real unit test

**Met.**

| Check | Evidence |
|---|---|
| Real unit tests | `src/test` holds JUnit 5 suites for every M0 area: `ConfigDefaultsTest`, `ConfigRulesTest`, `ModuleResolverTest`, `NamingTest`, `RegistrationLedgerTest`, `SelfCheckTest`, `RecipeTierCheckTest`, `ScenarioFrameworkTest`, `ScenarioRunTest`, `PacketPipelineTest`, `ServerboundPacketTest` and more. Commit `cec4da4f` records 459 unit tests passing at the M1 close |
| Build workflow | `.github/workflows/build-and-test.yml` calls GTNH's shared `build-and-test` workflow on every push and pull request to master. The shared workflow runs the build and the formatting check |
| Pass record | Commit `79495f04` ("Tick the last M0 gate: CI passes and the real GTNH server pack boots clean") records the first green CI run |
| Dependencies | `dependencies.gradle` pins GTNHLib 0.11.52, ModularUI2 2.3.92-1.7.10, NotEnoughItems 2.8.155-GTNH and IC2 2.2.828-experimental as `devOnlyNonPublishable`, with GTNHLib also on `testImplementation`. Landed by `524937bb` and `0dbfaa65` |

## 2. `./gradlew runServer` boots with GT5-Unofficial loaded and no errors from GregGuns

**Partially met.** The boot is proven repeatedly, but the repo does not show CI running `runServer`.

| Check | Evidence |
|---|---|
| Local dev boot | The ROADMAP note (2026-10-18), and `cec4da4f` (22 headless scenarios and the dev self-check, 6 items and 28 recipes); every later milestone's scenario run boots `runServer` with `gregguns test all` |
| Real pack boot | The ROADMAP note: the release jar booted clean on the GTNH 2.9.0-RC-2 dedicated server pack, with default config and with guns, armor and machines enabled. The bug it found is fixed in `7ba3036a` (register `EquipmentRefillRecipe` with `RecipeSorter`) |
| Side safety | `ClientProxyModulesTest` and `EventHandlerVisibilityTest` guard against client classes leaking into common code |
| CI smoke | **Gap.** `CONTRIBUTING.md` (Testing) says "CI runs this on every push", but `.github/workflows/build-and-test.yml` only calls the shared GTNH build workflow, and no script or workflow in the repo invokes `runServer`. The ROADMAP wording "The CI smoke test passes" rests on the green build, not on a booted server in CI |

## 3. `./gradlew runClient` reaches the main menu, and the GregGuns creative tab exists

**Met.**

| Check | Evidence |
|---|---|
| Tab class | `com.gregguns.core.GregGunsTab`; lang key `itemGroup.gregguns=GregGuns` in `assets/gregguns/lang/en_US.lang` |
| Seen in game | Commit `cec4da4f` ("Close the M1 exit gate and the M0 creative-tab box"): screenshots of the creative tab from the `mp.visual` scenario on real clients |
| Client boot | `mp.sync`, `mp.visual` and `mp.give` need a client that joins a world, which is past the main menu |

## 4. Config module flags load, save and are documented

**Partially met.** Load, save and documentation are met. The flag defaults do not follow the ROADMAP's feature-flag rule.

| Check | Evidence |
|---|---|
| Flags | `ModulesConfig` (`@Config(modid = "gregguns", category = "modules")`), one boolean per `ModuleId`; `ModuleFlags` reads them |
| Load and save | `ConfigDefaultsTest.everyModuleFlagDefaultsToOffUntilItsMilestoneIsMet`, `moduleFlagsAreExactlyTheModuleIds`, `flagsReflectFieldChanges`; `ConfigRulesTest.everyConfigClassTargetsTheSharedGregGunsFile`, `configFieldsArePublicStaticNonFinal` |
| Module wiring | `ModuleResolverTest`: `disabledModuleIsSkippedByConfig`, `moduleWithDisabledRequirementIsSkipped`, `requirementChainsPropagate`, `everyModuleIsDeclaredAfterItsRequirements`, `configKeysAreUnique` |
| Documented | [docs/config.md](../../config.md) lists every module flag with default, range, sync and requirements; each field carries an `@Config.Comment` |
| Gap | The ROADMAP says each module defaults to on once its exit gate is met. Every flag still defaults to `false` (`ModulesConfig.guns` and the rest, and the test is pinned to that), although M1 to M6 are ticked. Already noted as gap G6 in the [M2 exit gate](../02-guns/m2-exit-gate.md#gap-list) |

## 5. Every packet base class rejects out-of-range, wrong-dimension and unauthorized requests, with unit tests

**Met.**

| Check | Evidence |
|---|---|
| Base classes | `com.gregguns.network.ServerboundPacket` and `ClientboundPacket`, driven by `network.validation.PacketPipeline` with declarative `Rules`, `SenderRules` and `ClientRules` |
| Out of range | `ServerboundPacketTest.rejectsOutOfRangeIndex`, `rejectsOutOfReach`; `RulesTest.rejectsIndexAtOrAboveSizeAndNegative`, `finiteRejectsNaNAndInfinity` |
| Wrong dimension | `ServerboundPacketTest.rejectsWrongDimension` |
| Unauthorized | `ServerboundPacketTest.rejectsUnauthorizedSender`, `authorizationRunsAfterTargetValidation`, `rejectsDeadSenderEvenWhenThePacketAddsNoSenderRules`, `rejectsSpectatorLikeSender`; `SenderRulesTest` |
| Missing or wrong target | `ServerboundPacketTest.rejectsMissingAndWrongTypeTargets`; `RulesTest.missingTargetIsRejected`, `wrongTypeTargetIsRejected` |
| Containment | `PacketPipelineTest.invalidPacketNeverReachesHandlerAndIsReported`, `handlerExceptionIsContainedNotThrown`; `ServerboundPacketTest.truncatedPacketBytesNeverThrowIntoNettyAndAreDropped`; `ClientboundPacketTest`, `LimitedRejectionSinkTest` |
| Inherited bugs | `GregGunsNetworkTest.doesNotReproduceCORE05_*`, `CORE06_*` (arbitrary GUI id), `CORE08_*` (distant coordinates, missing tiles) |
| Real packets | The M1 packets use the same pipeline: `GunPacketsTest` (including GUN-12) |

## Scope items outside the exit gate

| Scope item | Evidence |
|---|---|
| Package skeleton | The layout under `com.gregguns` matches CONTRIBUTING.md Architecture; `524937bb` |
| Registries with module toggles | `core.registry.ModuleRegistrar`, `Naming`, `RegistrationLedger` (`NamingTest`, `RegistrationLedgerTest`); `core.module.ModuleManager` |
| `sounds.json` skeleton | `assets/gregguns/sounds.json`, now fully populated; `SoundChecksTest`, `GunSoundsJsonTest` |
| Scenario command | `core.command.TestSubCommand` and `core.testing.ScenarioRunner`; `ScenarioFrameworkTest`, `ScenarioRunTest`, `ScenarioAreaTest`; `harness.area` and `harness.entity` in `HarnessScenarios` |
| Dev self-check | `core.selfcheck.SelfCheckRunner` with `RecipeTierCheck`, `ContentChecks`, `SoundChecks`, `UnificationChecks`; `SelfCheckTest`, `RecipeTierCheckTest`, `ContentChecksTest`, `RecipeChecksTest` |

## Deviations recorded against the spec

The [01-core](index.md) pages record two, both in [networking-gtnhlib.md](networking-gtnhlib.md):

| Deviation | Where |
|---|---|
| Package is `com.gregguns.network`, not `com.gregguns.net` | networking-gtnhlib.md, "Deviation (package)" |
| Validation is declarative rule sets driven by one `PacketPipeline`, not static methods on each packet | networking-gtnhlib.md, "Deviation (validation shape)" |

The structural differences from Techguns (one `@Config` file set instead of `Techguns.cfg`, GTNHLib packets, GTNHLib teams) are the design of [config-gtnhlib.md](config-gtnhlib.md), [networking-gtnhlib.md](networking-gtnhlib.md) and [team-relations.md](team-relations.md), not deviations.

## Open polish items

| ID | Item | Kind |
|---|---|---|
| P1 | No `TODO` or `FIXME` in `src/main/java` or `src/test/java` | clean |
| P2 | Key-conflict check still open: [keybinds.md](keybinds.md) says to run `runClient` against the full pack in M0 and record any rebinding. No design record exists for it | open, needs a client |
| P3 | Open questions on the 01-core index: default `playerRelationMode` (`TEAMS` or `EVERYONE_FRIENDLY`, [team-relations.md](team-relations.md)), face and back cosmetics sync ([networking-gtnhlib.md](networking-gtnhlib.md)), GTTweaker generic recipe add ([mod-compat-tinkers-scripting.md](mod-compat-tinkers-scripting.md)), single or split config file ([config-gtnhlib.md](config-gtnhlib.md)) | open, docs |
| P4 | The Angelica HUD-caching trace in [hud-overlays.md](hud-overlays.md) was due when M1 built the HUD | open, docs |

## Needs owner decision

| ID | Box | Proposed resolution |
|---|---|---|
| D-1 | Box 2, CI smoke | Either add a `runServer` boot step to a workflow (run headless, grep for the `Scenarios: N passed` line), or reword the ROADMAP box and the CONTRIBUTING line to say the boot smoke test is run by the orchestrator before each release. Recommended: reword now, add the job in M9 |
| D-2 | Box 4, flag defaults | Flip `modules.guns`, `armor`, `machines`, `energyWeapons` and `utility` (and `npcs` and `worldgen` once their gates are ticked) to `true` for the beta, and update `ConfigDefaultsTest.everyModuleFlagDefaultsToOffUntilItsMilestoneIsMet`, `docs/config.md` and the pack notes. A default install cannot play the beta otherwise |
| D-3 | Polish P2 | Run the key-conflict check in the batched client pass of the beta gate, and write a design record only if a clash is found |
| D-4 | Polish P3 | Accept the current defaults (`TEAMS`, one config file, no cosmetic packet) as the beta's recorded decision, and close the open questions after the owner playthrough |
