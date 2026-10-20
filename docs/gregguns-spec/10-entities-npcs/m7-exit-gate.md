# M7 exit gate audit

Evidence for the four M7 exit-gate boxes in [ROADMAP.md](../../ROADMAP.md#m7--npcs-and-spawning). Audited on the `m7/close-out` branch (master at `9518a9ac` plus the module-defaults change), after the NPC core (N1 to N9) and mob batches B1 to B4 merged. This audit was written from the docs, sources and test sources; the headless suite is run by the orchestrator and no in-game soak was made for it. Scenario names and what each asserts are in [docs/testing/README.md](../../testing/README.md). The spawn and pack-hook deviations are in [spawning.md](spawning.md), [infernal-mobs.md](infernal-mobs.md), [mob-drops-nei.md](mob-drops-nei.md) and [registry-and-pack-hooks.md](registry-and-pack-hooks.md).

The audit follows [the M2 audit](../02-guns/m2-exit-gate.md) and [the M5 audit](../04-projectiles-fx/m5-exit-gate.md).

## Verdicts

| Box | Verdict |
|---|---|
| 1. Spawning is config-gated, respects dimension and biome rules, and stays within caps (NPC-14, NPC-15, NPC-17) | Met headless |
| 2. NPC difficulty scaling is applied once (GUN-06) | Met headless |
| 3. Mobs despawn correctly (NPC-28) | Met headless |
| 4. A soak test with spawning enabled shows no TPS regression in the dev pack | Scenario `npc.soak` built; evidence pending the box run |

`modules.npcs` therefore stays `false` ([config.md](../../config.md)).

## 1. Spawning is config-gated, respects dimension and biome rules, and stays within caps

| Check | Evidence |
|---|---|
| Config gate: faction flag off or weight 0 leaves no table entry (NPC-14, NPC-15) | `SpawnCoreTest.doesNotReproduceNPC14_zeroWeightNeverPicked`, `tableOpensByDangerAndSkipsUnbound`, `netherOnlyAndDisabledHaveNoEntry`; scenarios `npc.spawn_config_gate`, `npc.weighted_pick` (10,000 draws, zero weights never picked), `npc.cyberdemon_spawn_gate` |
| Dimension rule (default `[0]`; -1, 1, 7, 28, 180 denied) | `SpawnCoreTest.gateAppliesRulesInOrder`; `npc.spawn_dimension_gate`; `NpcSpawnHandler` (`CheckSpawn` gate, GGS-10-23, GGS-10-26) |
| Biome rule | `NpcSpawnHandler` registers each enabled, weighted, bound mob with `EntityRegistry.addSpawn` as `monster` in the biomes it lists, filtered with `BiomeDictionary`. No unit or scenario test reads the biome list; that check is GGS-10-26's runServer item |
| Danger levels from distance to world spawn | `SpawnCoreTest.distanceDangerFollowsThresholds`, `totalIsClamped`; `npc.danger_levels` |
| Caps (per chunk, per player), no chunk loads | `NpcCapTest.capsAreStrict`, `doesNotReproduceNpc17_shortAndFullNamesBothMatch`; `npc.cap_enforced`, `npc.camp_port_counts` (counting an unloaded chunk returns 0, WGEN-27) |
| Config defaults | `npc.module_config`, `ConfigDefaultsTest` |

Commits: `cc1de20d` (spawn core), `1aec57bb` (N6 merge), `c07cb586` and `1b2998b3` (despawn rules and caps).

Verdict: met headless. The biome list and a live Moon visit (GGS-10-23, GGS-10-26) are runServer checks, listed under polish.

## 2. NPC difficulty scaling is applied once (GUN-06)

| Check | Evidence |
|---|---|
| The accuracy factor is applied once | `NpcFiringTest.doesNotReproduceGUN06_accuracyFactorIsAppliedOnce`, `difficultyTableMatchesTheSpec`, `difficultyIdsClamp`, `turretsUseUnitFactors` |
| No zoom bonus for NPCs (GUN-07) | `NpcFiringTest.doesNotReproduceGUN07_npcsGetNoZoomBonus` |
| Health and melee attack are not scaled by us (GGS-10-29) | The mob stat tests pin base values: `SoldierStatsTest`, `ZombieStatsTest`, `UndeadStatsTest`, `BossStatsTest`, `AlienBugTest`, `CyberDemonTest`, `SuperMutantTierTest`, `BanditTest`; egg-stat scenarios `npc.bandit_egg_stats`, `npc.army_soldier_egg_stats`, `npc.alien_bug.stats` and the others |
| Real shots use the profile once | `npc.gun_burst`, `npc.*_fires`, `npc.hurt_pipeline` (one `LivingHurtEvent` per hit) |

Commits: `2a417951` (gun-AI core), `3848f2f7`. Verdict: met headless. EnderZoo's own difficulty modifier is a pack-side third scaler, recorded in [spawning.md](spawning.md).

## 3. Mobs despawn correctly (NPC-28)

| Check | Evidence |
|---|---|
| Ordinary NPCs may despawn; bosses and the helicopter persist | `NpcCapTest.doesNotReproduceNpc28_bossesAndHelicopterPersist`; `HelicopterRulesTest`, `BossStatsTest`, `AlienBugTest` (their `DespawnRules` use); `DespawnRules` |
| Live check | `npc.persistence_hook` (an ordinary NPC may despawn, one with the hook may not), `npc.bosses_persist` (far-away player, save and load) |

Commits: `c07cb586`, `1b2998b3`, `85f7ba3a`. Verdict: met headless.

## 4. A soak test with spawning enabled shows no TPS regression

The opt-in scenario `npc.soak` (run `gregguns test npc.soak` with `modules.npcs` on; it is not part of `test all`) measures this. It samples `MinecraftServer.tickTimeArray` for 200 ticks with a fake player and no NPCs, then spawns 100 mixed NPCs (zombies, skeleton, army, commando, bandit, alien bug, super mutants; no bosses) around the player and samples 2400 ticks while they target, shoot and fight, with natural spawning left as configured. It passes when the load mean is under 50 ms (20 TPS) and rises less than 10 ms over the baseline (`-Dgregguns.soak.regressionMs` overrides the margin), logs mean, p95, max and the live NPC count every 200 ticks, cancels drops and removes everything it made with `World.removeEntity`. Per-tick spawn cost is also bounded by design (`NpcCap` counts loaded entities only; the handler is a `CheckSpawn` gate). GGS-10-25 (NPCs count toward the monster cap after a long soak) is still a manual check.

Evidence pending the box run. Numbers to fill in: baseline mean, load mean, p95, max, peak NPCs alive.

Verdict: pending the box run.

## Registered mobs and the all-eggs check

The catalog has 19 mobs plus the dev-only test NPC. `NpcRegistry.eggAudit` checks that each has a bound class, egg colors that resolve through `NpcMob.byId`, and (live) a registered entity and a creative egg subtype. Evidence: `NpcEggCoverageTest` (headless) and scenario `npc.all_eggs` (live).

| Check | Result |
|---|---|
| `SIMPLE_TURRET` | Exempt on purpose: the hostile camp turret is placed by the turret base and camps; it is bound and registered but has no egg |
| `OUTCAST`, `STORMTROOPER` | Implemented (M7 B5). Bound to `Outcast` and `StormTrooper` (entity IDs 70 and 76), registered with an egg; `NpcRegistry.NOT_YET_IMPLEMENTED` is empty |
| The 16 other mobs | Bound, registered, with an egg |

The last two mobs, Outcast and Stormtrooper, are egg-only ([mob-drops-nei.md](mob-drops-nei.md), tiers LuV and IV) and now exist.

## Deviations recorded in the area pages

| Page | Deviation |
|---|---|
| [spawning.md](spawning.md) | Real classes and a `CheckSpawn` gate instead of the `TGDummySpawn` placeholder (GGS-10-26, 0005 amended); halved spawn weights (GGS-10-24); one dimension list, default `[0]`, checked at spawn time (NPC-16); Nether-only mobs (Pigman Soldier, Cyberdemon) by egg, command or camp only; difficulty scales gun damage and accuracy only (GGS-10-29) |
| [infernal-mobs.md](infernal-mobs.md) | The hurt pipeline posts `LivingHurtEvent` once with indirect damage sources; infernal tiered loot via a soft-dependency drops handler; spawner NPCs opt out; helicopter and turret excluded |
| [mob-drops-nei.md](mob-drops-nei.md) | Simulation-friendly drops (entity RNG, vanilla `addRandomArmor()`); per-NPC tier ceilings |
| [registry-and-pack-hooks.md](registry-and-pack-hooks.md) | `gregguns.<Name>` entity names, one constant each; pack-side aspects and drop hooks |

## Open polish items

- The biome spawn list, Moon visit and long-soak monster-cap checks (GGS-10-23, GGS-10-25, GGS-10-26, GGS-10-27) and the AngerMod spawn-protection manual check (GGS-10-28).
- Pack-side changes in [PACK-CHANGES.md](../PACK-CHANGES.md) (Infernal Mobs config, Mobs Info blacklist, AngerMod, ThaumcraftMobAspects) land with M9.
- Final spawn weights come from the M9 balance pass.
- Visuals and sounds of the mobs wait for the batched client session.

## Needs owner decision

| Gap | Question |
|---|---|
| Soak test | Run a dev-pack soak with `modules.npcs=true` before the flag flips, or accept the headless evidence and defer the soak to the beta review gate? |
| Outcast and Stormtrooper | Implemented (M7 B5), no decision needed. Evidence: `SoldierStatsTest`, `NpcEggCoverageTest`, `NpcLootTest.stormtrooperTableIsNineSlotsAndOutcastHasNone`; scenarios `npc.outcast_egg_equipment`, `npc.stormtrooper_egg_equipment`, `npc.outcast_fires`, `npc.stormtrooper_fires`, `npc.outcast_stormtrooper_despawn`, `npc.loot_simulation` and `npc.all_eggs`. Outcast: T3 Power Armor (camo 1 or 2 per piece) and one of seven guns; Stormtrooper: T3 HEV suit (silver camo) and a Blaster Rifle or Laser Rifle. Both render as `biped` with the Army Soldier skin (no new texture) |
