# Roadmap

How GregGuns gets from an empty mod to a working beta, a polished self-contained release and a handoff ([0014](design/0014-end-goal-beta-handoff-archive.md), [0015](design/0015-m9-self-contained-polish.md)). The work follows the [Techguns behavioral spec](techguns-spec/README.md), rebuilt on GregTech progression.

## How to read this roadmap

- **Milestones (M0–M9)** are delivered in order. Each one has an **entry gate**, which must be true before work starts, and an **exit gate**, which is its definition of done. A milestone isn't finished until every exit-gate box is ticked.
- **Decision gates (D1–D6)** are design questions settled before the milestones that depend on them start. All six are **accepted by default** for the MVP: see [docs/design/](design/README.md). They're revisited after MVP.
  - Each decision is recorded in [`docs/design/`](design/README.md), and the gate is closed by marking that record `Status: Accepted`.
  - The tables below summarize the accepted records; the records are authoritative.
- Every milestone also meets the universal **definition of done** in [CONTRIBUTING.md](../CONTRIBUTING.md#definition-of-done): tests, comments, lang keys, original assets, no reproduced Techguns bugs, and spec deviations recorded.
- **Feature flags:** each milestone's content ships behind a config toggle such as `modules.armor`, defaulting to on once its exit gate is met. That way, unfinished or unwanted modules can be switched off in the pack without a rebuild.

Tick a box only in the PR that completes it.

```
M0 Foundation ──► D1–D4 ──► M1 Combat core ──► M2 Ballistic arsenal ──► M5 Energy & exotic weapons
                    │             │                    │
                    │             ├──► M3 Armor & equipment ─┘
                    │             └──► M4 Production chain & machines
                    └──► D5 ──► M7 NPCs ──► M8 Worldgen         M6 Utility & defense (after M4)
                                                    all ──► Beta gate ──► M9 Self-contained polish, then handoff
```

---

## M0 · Foundation

No gameplay. This milestone builds everything later milestones need.

**Entry gate:** none. The repo is scaffolded and CI is green.

**Scope:**
- **Dependencies** in [`dependencies.gradle`](../dependencies.gradle), pinned to the versions in the current GTNH pack: GT5-Unofficial, GTNHLib, ModularUI2, NotEnoughItems, and the IC2 version GT uses. Add them as `devOnlyNonPublishable` or `compileOnly`, as appropriate.
- **Package skeleton:** the layout in [CONTRIBUTING.md § Architecture](../CONTRIBUTING.md#architecture).
- **Registries:** per-module registration (items, blocks, entities, tile entities, sounds), with module toggles.
- **Infrastructure:**
  - GTNHLib-annotated config, with module flags;
  - a network channel with validated packet base classes;
  - a creative tab and `en_US.lang`;
  - a `sounds.json` skeleton.
- **Test harness:** JUnit 5 under `src/test/java` running in `./gradlew test`, plus the in-game scenario command (`/gregguns test …`, dev environment only) described in [CONTRIBUTING.md § Testing](../CONTRIBUTING.md#testing).
- **Dev self-check:** a dev-only postInit check that every GregGuns recipe resolves and respects the tier map.

**Exit gate:**
- [x] `./gradlew spotlessApply build` passes, with at least one real unit test.
- [x] `./gradlew runServer` boots with GT5-Unofficial loaded and no errors from GregGuns. The CI smoke test passes. *(Local boot verified 2026-10-18. GitHub CI build-and-test passed on the first push, 2026-10-19. The release jar also booted clean on the real GTNH 2.9.0-RC-2 dedicated server pack, with default config and with guns, armor and machines enabled.)*
- [x] `./gradlew runClient` reaches the main menu, and the GregGuns creative tab exists. *(Seen in game 2026-10-18 by the M1 visual autotest.)*
- [x] Config module flags load, save and are documented.
- [x] Every packet base class rejects out-of-range, wrong-dimension and unauthorized requests, with unit tests.

Beta-gate audit: [M0 exit gate](gregguns-spec/01-core/m0-exit-gate.md). It finds 3 boxes met and 2 partly met (the CI smoke wording, and module flags that still default to off), with owner decisions listed.

---

## Decision gates

Each gate produces a design record in [`docs/design/`](design/README.md). Nothing a gate blocks may be merged until that record is accepted.

### D1 · Tier map. *Blocks M1–M8.*

**Accepted:** [0001-tier-map.md](design/0001-tier-map.md)

Every weapon, ammo type, armor set, component and machine is assigned a GT voltage tier and a **gating mechanism**: the machine voltage needed to craft it, or a material that can't be obtained before that tier.

The table below lists example gate materials. **Verify each one against the current GTNH progression before accepting D1.**

| Tier | Example gate material | Proposed content (from the spec) |
|---|---|---|
| Steam / ULV | Bronze, wrought iron | Hand Cannon (stone bullets); T1 scout and miner sets |
| LV | Steel | 9mm Pistol, Revolver, Sawed Off, Bolt Action Rifle; T1 combat set; Combat Knife, Crowbar |
| MV | Aluminum | Submachine Gun (Thompson), Mac10, Combat Shotgun, M4, AK47, frag grenades; T2 combat, commando and riot sets; gas mask, night-vision goggles |
| HV | Stainless steel | AUG, SCAR, Vector, AS50, LMG, Grenade Launcher, Rocket Launcher, Flamethrower, Chainsaw, Power Hammer; Infiltrator; akimbo variants of earlier tiers; jump pack, glider, oxygen gear |
| EV | Titanium | Laser Rifle, PDW, Pulse Rifle, Minigun, Tesla Rifle, Sonic Rifle, Bio Gun, Power Hammer (Advanced); T3 combat set; jetpack, steam armor |
| IV | Tungstensteel | Blaster Rifle, De-Atomizer Pistol, Scatter Laser Rifle, Golden Revolver; power armor, exo suit; anti-grav device |
| LuV+ | LuV materials, then naquadah and beyond | Nether Blaster, Alien Blaster, Nuclear Death Ray, Grim Reaper, Nuke Launcher (ZPM or higher) |

The D1 record must also cover:
- **Ammo and component tiers:** for example, bullets at LV and energy cells at EV.
- **Unfinished Techguns content:** whether to build the Scrapper, Flashbang, Medkit, UV Emitter and Scatter Laser Rifle [WIP], or drop them.

### D2 · Machine deconfliction. *Blocks M4 and M6.*

**Accepted:** [0002-machine-deconfliction.md](design/0002-machine-deconfliction.md)

The rule: GT already has machines for most Techguns processes, so GregGuns uses **GT recipe maps** instead of re-adding them. Only machines that are distinctive *and* have no GT equivalent stay, rebuilt as GT MetaTileEntities.

| Techguns machine | Proposal | Replacement or rationale |
|---|---|---|
| Ammo Press | **Replace** | GT Assembler, Forming Press and Extruder recipes per ammo type |
| Metal Press | **Drop** | GT Bending Machine and Forming Press |
| Chem Lab | **Drop** | GT Chemical Reactor and Mixer |
| Fabricator (multiblock) | **Replace** | GT Assembler and Circuit Assembler recipes |
| Reaction Chamber (multiblock) | **Retired** ([0012](design/0012-retire-reaction-chamber-and-retort.md)) | Recipes moved to the GT Laser Engraver and Assembler. The Retort is archived on `archive/retort-multiblock` / `archive/retort-v1` |
| Charging Station | **Drop** | GT battery buffers and chargers. Items use the IC2 electric-item API |
| Repair Bench | **Keep** | Armor repair using GT materials |
| Camo Bench | **Keep** | Cosmetic, tasteful, no GT equivalent |
| Ore Drill (multiblock) | **Drop** | GT miners and the GTNH ore system |
| Scrapper | **Drop** | Unfinished in Techguns. GT recycling covers it |
| Turret | **Keep** | Core to the defense theme |
| Explosive charges | **Keep, gated** | Check for overlap with GT mining explosives |
| Weapon and ammo chests | **Keep, low priority** | Decorative storage |
| Techguns ores, ingots, plates, wires, circuits | **Drop** | GT materials and parts through ore-dict unification |
| Techguns fluids (oil, fuel, acid) | **Drop** | GT fluids |

### D3 · Energy model. *Blocks M3, M4 and M5.*

**Accepted:** [0003-energy-model.md](design/0003-energy-model.md)

- All power is **GT EU**; there is no RF anywhere.
- Energy weapons, powered armor and tools are IC2 electric items, charged by GT chargers and battery buffers.
- Fuel-based items (flamethrower, chainsaw, power hammer) take GT fluids.
- The record defines capacity and use per tier.

### D4 · Damage model. *Blocks M1.*

**Accepted:** [0004-damage-model.md](design/0004-damage-model.md)

- Decide whether to keep the Techguns damage types, armor penetration and per-type resistances, or simplify them.
- Decide how GregGuns damage scales against GTNH mobs and other mods' armor.
- The record lists which damage-system bugs in [BUGS.md](techguns-spec/BUGS.md) (GUN-33 to GUN-45) the new design avoids.

### D5 · World presence. *Blocks M7 and M8.*

**Accepted:** [0005-world-presence.md](design/0005-world-presence.md)

- Which NPC factions and structures ship, and in which dimensions and biomes.
- Spawn caps relative to GTNH's difficulty curve.
- **No ore generation:** the GTNH ore system owns ores.

### D6 · Art direction. *Blocks every milestone that adds assets.*

**Accepted:** [0006-art-direction.md](design/0006-art-direction.md)

- Style rules so new textures sit naturally next to GT: resolution, palette and shading.
- Sound loudness targets.
- Asset provenance rules. Everything is original; see [CONTRIBUTING.md § Assets](../CONTRIBUTING.md#assets-and-localization).

---

## M1 · Combat core: the vertical slice

One gun, working end to end inside the GTNH dev pack.

**Entry gate:** M0 is done; D1, D3 and D4 are accepted.

**Scope:**
- **Damage system:** see [02-guns/damage-system.md](techguns-spec/02-guns/damage-system.md).
- **Gun framework:** the fire and reload lifecycle, semi and auto fire, zoom, spread, and the NPC-use hooks. See [02-guns/gun-lifecycle.md](techguns-spec/02-guns/gun-lifecycle.md).
- **Projectile framework:** see [04-projectiles-fx/projectiles.md](techguns-spec/04-projectiles-fx/projectiles.md).
- **Ammo and magazines:** see [06-items/ammo.md](techguns-spec/06-items/ammo.md).
- **Networking and display:** the fire and reload packets, and the ammo HUD.
- **Gun rendering framework:** recoil, muzzle flash and transforms. See [03-gun-visuals-audio/rendering-framework.md](techguns-spec/03-gun-visuals-audio/rendering-framework.md).
- **Sound framework.**
- **The first gun:** the **9mm Pistol** and its magazine, crafted through an LV GT recipe chain.
- **Production chain slice (pulled into M1 from M4 for the vertical slice):** the `gregguns.ammoPress` recipe map, the LV Ammo Press (MTE 20101), the two components the pistol needs (`part_barrel_hardened`, `part_mechanical_2`), and the 9mm bullet, empty-magazine and loaded-magazine recipes plus the crafting-table refill. The animation synced to progress, the Steam, MV, HV and EV presses, the Ammunition Works multiblock and the rest of the M4 recipes stay in M4. See [ammo-press.md](gregguns-spec/08-machines-production/ammo-press.md#m1-slice-deviation).

**Exit gate:**
- [x] The pistol is craftable in the dev pack only through its D1 tier gate, and the recipe shows in NEI. *(`gun.recipe`; NEI screenshot from the visual autotest.)*
- [x] Fire, reload and zoom work, and the pistol hits and damages mobs and players. *(`gun.*` scenarios.)* The pistol has no scope, so zoom is proven on the scoped guns; aiming down its iron sights is on the adopter list ([0014](design/0014-end-goal-beta-handoff-archive.md)) ([M1 audit D-4](gregguns-spec/02-guns/m1-exit-gate.md#needs-owner-decision)).
- [x] Unit tests cover the damage formulas, drop-off, spread, ammo accounting and the reload state machine. *(See the M1 coverage table in docs/testing/README.md.)*
- [x] A dedicated server with two clients shows correct sync: shots, reloads, other players' animations and sounds. *(`mp_check`: `mp.sync`, `mp.visual`, `mp.give` and client log evidence.)*
- [x] Each relevant inherited bug from [BUGS.md](techguns-spec/BUGS.md) has a test showing it isn't reproduced. These include GUN-13, GUN-29, GUN-32, GUN-33 and GUN-34.

Beta-gate audit: [M1 exit gate](gregguns-spec/02-guns/m1-exit-gate.md). It finds all 5 boxes met, with the NEI view and the two-client run to repeat in the batched client pass.

## M2 · Ballistic arsenal

**Entry gate:** M1 is done.

**Scope:**
- Every ballistic gun, grenade and akimbo variant from [02-guns](techguns-spec/02-guns/index.md), delivered in tier order (D1).
- Their ammo items, models, textures and sounds.

**Exit gate (per tier batch):**
- [x] Every gun in the batch matches its spec sheet, or a deviation is recorded. Evidence: stats pinned by `GunCatalogTest`, `MvGunsLogicTest`, `HvRiflesLogicTest`, `HeavyGunLogicTest` and the launcher, flame and golden revolver tests; deviation sections for every batch in [02-guns](gregguns-spec/02-guns/index.md) ([M2 exit gate](gregguns-spec/02-guns/m2-exit-gate.md#1-every-gun-matches-its-spec-sheet-or-a-deviation-is-recorded)).
- [x] Every recipe is gated by tier and visible in NEI. Gating: `GunDefinitionsTest` over every row with `GateRule`, a `*.recipe` or `*.combine` scenario per gun. NEI visibility: confirmed in game by the owner, 2026-10-25 ([M2 exit gate](gregguns-spec/02-guns/m2-exit-gate.md#2-every-recipe-is-tier-gated-and-visible-in-nei)).
- [x] Unit tests cover each gun's unique behavior, such as shotgun pellets, burst fire and the grenade fuse. Evidence: `ShotgunAndAkimboLogicTest`, `GrenadeMathTest`, `LauncherMathTest`, `FlamethrowerLogicTest` and the per-gun scenarios ([M2 exit gate](gregguns-spec/02-guns/m2-exit-gate.md#3-unit-tests-cover-each-guns-unique-behavior)).
- [x] Original textures and sounds are in place, following D6. Evidence: every gun has a generated texture, a clean-room profile and `sounds.json` events whose files exist; 20 sound files are still at the `tweak` verdict and the Hand Cannon has no notes file, both listed as gaps ([M2 exit gate](gregguns-spec/02-guns/m2-exit-gate.md#4-original-textures-and-sounds-are-in-place-following-d6)).

## M3 · Armor and equipment

**Entry gate:** M1 is done; D1 and D3 are accepted.

**Scope:**
- The T1 to T3 sets, steam armor, power armor and exo suit.
- Set bonuses.
- The extra Techguns inventory: face, back, hand and ammo slots.
- Masks, goggles, tanks, jetpack, glider, jump pack and anti-grav.
- The spec is [05-armor](techguns-spec/05-armor/index.md).

**Exit gate:**
- [x] Protection and penetration values follow the D4 model, with unit tests. Evidence: `ArmorProtectionTest`, `ArmorTiersTest`, `PowerArmorTest` and `armor.damage` ([M3 exit gate](gregguns-spec/05-armor/implementation-notes.md#m3-exit-gate)).
- [x] Bonuses apply and are removed cleanly, with no step-height or potion interference (CORE-14, CORE-15). Evidence: `Core14And15RegressionTest`, `BonusLifecycleTest`, `armor.lifecycle` and `armor.compat` ([M3 exit gate](gregguns-spec/05-armor/implementation-notes.md#m3-exit-gate)).
- [x] The special-slot inventory syncs, persists and works with automation where the spec says it should. Evidence: `mp.slots` and `mp.slots.after_relog` through `mp_check` with a relog (both clients match the server, 2026-10-18), plus `armor.slots.pouch`, `armor.power.slot_gate` and `armor.slots.automation` ([M3 exit gate](gregguns-spec/05-armor/implementation-notes.md#m3-exit-gate)).
- [x] Powered items run on EU through GT chargers. Evidence: `armor.power.charger` (exact-tier GT Battery Buffers, 0008) ([M3 exit gate](gregguns-spec/05-armor/implementation-notes.md#m3-exit-gate)).
- [x] Armor and accessories carry GT recycling data. Evidence: `ArmorCompositionTest` and `armor.recycling` ([M3 exit gate](gregguns-spec/05-armor/implementation-notes.md#m3-exit-gate)).

## M4 · Production chain and machines

**Entry gate:** M1 is done; D2 and D3 are accepted.

**Scope:**
- GT recipe-map recipes for every component and ammo type (see [06-items/components-and-materials.md](techguns-spec/06-items/components-and-materials.md)).
- The machines D2 keeps, built as GT MetaTileEntities. The spec is [08-machines-production](techguns-spec/08-machines-production/index.md) and [09-machines-utility](techguns-spec/09-machines-utility/index.md).
- NEI integration for any non-GT logic.

**Exit gate:**
- [x] No GregGuns item duplicates a GT material or part. Unification is checked by the dev self-check. Evidence: `UnificationChecks` with its unit tests, passing on every boot ([M4 exit gate](gregguns-spec/08-machines-production/m4-exit-gate.md)).
- [x] Every kept machine has a ModularUI2 GUI, EU input, NEI recipes and integration scenarios. Evidence: the per-machine table, with recorded exceptions: the Steam press runs on steam, the Camo Bench needs no EU, and the benches and Gantry have no recipe map to show in NEI ([M4 exit gate](gregguns-spec/08-machines-production/m4-exit-gate.md)).
- [x] MetaTileEntity IDs are reserved and recorded (see [CONTRIBUTING.md § GTNH integration](../CONTRIBUTING.md#gtnh-integration)). Recorded in `GregGunsMTEIDs`, the roster and `GregGunsMTEIDsTest`. The GT5-Unofficial `MetaTileEntityIDs` named-entry reservation is a pack-side PR listed in [PACK-CHANGES.md](gregguns-spec/PACK-CHANGES.md), now on the adopter list ([0014](design/0014-end-goal-beta-handoff-archive.md)) ([M4 exit gate](gregguns-spec/08-machines-production/m4-exit-gate.md)).

## M5 · Energy and exotic weapons

**Entry gate:** M2 and M4 are done, plus M3 for the powered items.

**Scope:**
- Laser Rifle, Blaster Rifle, Tesla Rifle, Sonic Rifle, Pulse Rifle, PDW, Bio Gun, De-Atomizer Pistol, Minigun, Power Hammers, Chainsaw, Scatter Laser Rifle (if D1 keeps it), Nether Blaster, Alien Blaster, Nuclear Death Ray, Grim Reaper and Nuke Launcher.
- Their beams, chaining and explosions (see [04-projectiles-fx](techguns-spec/04-projectiles-fx/index.md)).

**Exit gate:**
- [x] Each weapon is tier-gated with EU or fluid ammo per D3. Evidence: `GunDefinitionsTest` over every row with `GateRule`, a `*.recipe` or `*.tiergate` scenario per gun, and the per-gun fire scenarios asserting EU or fluid spend ([M5 exit gate](gregguns-spec/04-projectiles-fx/m5-exit-gate.md#1-each-weapon-is-tier-gated-with-eu-or-fluid-ammo-per-d3)).
- [x] Explosions respect config and protection (fixing PFX-14, PFX-25 and CORE-19), with tests. Evidence: `ExplosionGateTest` (`doesNotReproducePFX25_*`, `CORE19_*`, `PFX14_*`), `grenade.protected`, `explosion.nuke.sliced` ([M5 exit gate](gregguns-spec/04-projectiles-fx/m5-exit-gate.md#2-explosions-respect-config-and-protection-pfx-14-pfx-25-core-19-with-tests)).
- [x] The particle and FX system carries the spec's FX catalog, and performance is checked with many emitters on screen. The catalog and the 500-emitter budget are proven headless (`FxCatalogTest`, `ParticleBudgetTest`, `fx.stress`, `fx.stress.nuke`). Frame cost with many effects on screen: confirmed fine in game by the owner, 2026-10-25. The shader-pack rendering bug is tracked separately in the 0.3.2-beta known issues ([M5 exit gate](gregguns-spec/04-projectiles-fx/m5-exit-gate.md#3-the-particle-and-fx-system-carries-the-fx-catalog-and-performance-is-checked)).

## M6 · Utility and defense

**Entry gate:** M4 is done.

**Scope:** the turret, explosive charges, chests, camp flag and Bio Blob that D2 keeps. The spec is [09-machines-utility](techguns-spec/09-machines-utility/index.md).

**Exit gate:**
- [x] Turret targeting respects owners and friends, with no NPEs (NPC-12, MUTL-25). Evidence: `utility.turret_spares_owner`, `utility.turret_kills_zombie`, `utility.turret_no_npe`, `utility.rampart_mount_fires` and `TurretTargetSelectorTest` ([M6 exit gate](gregguns-spec/09-machines-utility/m6-exit-gate.md#turret-targeting-respects-owners-and-friends-with-no-npes-npc-12-mutl-25)).
- [x] Charges use permission checks (MUTL-31). Evidence: `utility.charge_foreign_button`, `utility.charge_respects_cancel`, `utility.charge_offline_owner` and `ChargePermissionTest` ([M6 exit gate](gregguns-spec/09-machines-utility/m6-exit-gate.md#charges-use-permission-checks-mutl-31)).
- [x] An integration scenario exists for each block. Evidence: one or more `utility.*` scenarios per block, 149 of 149 scenarios passing on 2026-10-19 ([M6 exit gate](gregguns-spec/09-machines-utility/m6-exit-gate.md#an-integration-scenario-exists-for-each-block)).

## M7 · NPCs and spawning

**Entry gate:** M2 is done and D5 is accepted.

**Scope:**
- The factions and mobs D5 selects, their AI with guns, loot, and spawn eggs.
- The spawn system. The spec is [10-entities-npcs](techguns-spec/10-entities-npcs/index.md).

**Exit gate:**
- [x] Spawning is config-gated, respects dimension and biome rules, and stays within caps (NPC-14, NPC-15, NPC-17). Evidence: `SpawnCoreTest`, `NpcCapTest`, `npc.spawn_config_gate`, `npc.spawn_dimension_gate`, `npc.cap_enforced` ([M7 exit gate](gregguns-spec/10-entities-npcs/m7-exit-gate.md#1-spawning-is-config-gated-respects-dimension-and-biome-rules-and-stays-within-caps)).
- [x] NPC difficulty scaling is applied once (GUN-06). Evidence: `NpcFiringTest.doesNotReproduceGUN06_accuracyFactorIsAppliedOnce` and the mob stat tests ([M7 exit gate](gregguns-spec/10-entities-npcs/m7-exit-gate.md#2-npc-difficulty-scaling-is-applied-once-gun-06)).
- [x] Mobs despawn correctly (NPC-28). Evidence: `NpcCapTest.doesNotReproduceNpc28_*`, `npc.persistence_hook`, `npc.bosses_persist` ([M7 exit gate](gregguns-spec/10-entities-npcs/m7-exit-gate.md#3-mobs-despawn-correctly-npc-28)).
- [x] A soak test with spawning enabled shows no TPS regression in the dev pack. `npc.soak`, with 100 NPCs: load mean 2.78 ms, p95 4.45 ms ([M7 exit gate](gregguns-spec/10-entities-npcs/m7-exit-gate.md#4-a-soak-test-with-spawning-enabled-shows-no-tps-regression)).

## M8 · Worldgen and structures

**Entry gate:** M7 is done and D5 is accepted.

**Scope:** the structures D5 selects, their loot and their NPC camps. There is **no** ore generation. The spec is [11-worldgen](techguns-spec/11-worldgen/index.md).

**Exit gate:**
- [x] No cascading worldgen (WGEN-04), verified by the chunk-load test. Evidence: `CascadeHarnessTest`, `worldgen.no_cascade` (shipped structures and the Alien Bug Nest) ([M8 exit gate](gregguns-spec/11-worldgen/m8-exit-gate.md#no-cascading-worldgen-wgen-04)). The ArchaicFix log check is a beta-gate client item.
- [x] Structures respect the dimension whitelist and GTNH's world types. Evidence: `WorldgenGateTest`, `worldgen.dimension_gate` ([M8 exit gate](gregguns-spec/11-worldgen/m8-exit-gate.md#structures-respect-the-dimension-whitelist-and-gthns-world-types)). A live RWG world is a beta-gate client item.
- [x] Loot is tier-appropriate for where the structure spawns. Evidence: `LootCategoriesTest`, `worldgen.loot_ceiling` ([M8 exit gate](gregguns-spec/11-worldgen/m8-exit-gate.md#loot-is-tier-appropriate-for-where-the-structure-spawns)). Structure screenshots, the Chisel palette and the GT ore pass are beta-gate client checks.

## Beta review gate · 0.3.0-beta

**Entry gate:** M8 is done.

**Scope:** a completeness review of every milestone from M0 to M8 before the first beta. The owner then plays the beta on their own local server. This gate replaces the alpha pre-releases. M9 work doesn't start until it closes.

**Process:**
- **Per-milestone audit.** For each milestone, an audit doc like the `m2-exit-gate.md` and `m5-exit-gate.md` pages records:
  - every exit-gate box, with the evidence for it;
  - every deviation recorded against the spec;
  - every open polish item.

  Any box that isn't met is either fixed or explicitly deferred, with the owner's sign-off.
- **Recipe-clash review.** Check every GregGuns recipe against the full GTNH 2.9 pack's recipes. That means:
  - crafting-table shapes (shaped and shapeless);
  - every GT recipe map we add to (assembler, ammo press, assembly line and others);
  - our own machines' maps;
  - NEI and ore-dictionary overlaps.

  A clash means identical inputs, or an input set that shadows another recipe, which either steals or hides an existing GTNH recipe. Do the check mechanically: dump the recipes from a real GTNH server with our jar loaded, then diff the dump. Fix every clash or list it in [PACK-CHANGES.md](gregguns-spec/PACK-CHANGES.md).
- **Pack-integration pass: retire the Reaction Chamber and the Retort.** Both machines have weak justification in a pack this rich. Remap every recipe they run onto an existing GTNH method, from any pack mod, not only GregTech. For example, the bio-ammo raw material comes from the GTNH Bio Lab. GregGuns keeps final ammo production; tight pack integration is the mod's main pitch to the pack maintainers.
  - Done: [0012](design/0012-retire-reaction-chamber-and-retort.md) holds the mapping table, the Laser Focus now uses the GT Laser Engraver and the Anti Gravity Core the GT Assembler. Wording below is the original brief.
  - The mapping table and the reasoning go in a design record.
  - The removed MTE meta IDs are retired and stay reserved; they are never reused.
  - Before removal, the complete Retort, including its interior parts, is preserved on branch `archive/retort-multiblock` and tag `archive/retort-v1`, and neither is ever deleted. The owner may pitch it to the GTNH community as a standalone bulk-reaction multiblock for the wider pack.
- **Recon: NewGunrizons** ([vegeta1k95/NewGunrizons](https://github.com/vegeta1k95/NewGunrizons)), another GTNH gun mod. Find out what it implements, how it integrates with GT, how active it is, its license, its pack status and its overlap with GregGuns. Weigh two options: merge with it, or take its place as the GTNH gun mod at release. Nobody contacts its author without the owner.
- **Client pass.** A batched in-game session covers every gun, armor set, machine, multiblock, NPC and structure. It records screenshots, and checks that the game runs with and without shaders.
- **Release.** Tag `0.3.0-beta`, cut it as a GitHub pre-release, and write release notes aimed at server owners.
- **Owner playthrough.** The owner plays the beta on a local multiplayer server. Bugs found there are fixed in `0.3.x-beta` builds.

**Exit gate:**
- [x] Every milestone M0–M8 has a completeness audit, with each exit box met or deferred with the owner's sign-off. Owner, 2026-10-21: M0–M8 signed off as the MVP.
- [x] The recipe-clash review found no unresolved clashes with GTNH 2.9 recipes. Each clash it found was fixed, or recorded in PACK-CHANGES with the owner's sign-off. Evidence: v4 on master `b19ae575` found 0 exact clashes and 0 superset clashes. The one subset hit, the Bacterial Vat culture recipe, is verified in game by `gun.bio_gun.vat_recipe_selected`. All 8 v3 shadows are fixed ([review](testing/recipe-clash-beta.md)).
- [x] The Reaction Chamber and the Retort are removed (design record 0012, branch `fix/retire-reaction-retort`). Their recipes are remapped to GTNH methods per the design record, and their MTE IDs are retired and reserved. The Retort is archived on `archive/retort-multiblock` and `archive/retort-v1`.
- [x] The NewGunrizons recon note is written, and the owner has picked a direction: merge, or take its place. Owner, 2026-10-20: take its place, with no merge and no contact; Superseded by [0014](design/0014-end-goal-beta-handoff-archive.md): no submission to GTNH; the mod is handed off to an adopter. See the [recon note](research/newgunrizons-recon.md#owner-decisions-2026-10-20).
- [x] The batched client pass is clean, including with shaders on, and has no crash-severity or invisible-render bugs. Dev client on Xvfb (parts 1–3), BSL on NVIDIA, and a live run in the real GTNH 2.9.0-RC-2 pack with Complementary on AMD (406 passed, 0 failed, 15 skipped).
- [x] `0.3.0-beta` is published (2026-10-21): [GitHub pre-release](https://github.com/Tuxprogrammer/GregGuns/releases/tag/0.3.0-beta).
- [ ] The owner's beta playthrough on a local server is done, and its blocking bugs are fixed.

## M9 · Self-contained polish, then handoff

Amended by [0015](design/0015-m9-self-contained-polish.md). The owner signed off M0–M8 as the MVP on 2026-10-21. M9 polishes the mod and fits it into the pack **from inside our own jar**, then hands it off ([0014](design/0014-end-goal-beta-handoff-archive.md)). GregGuns stays one portable jar: nothing depends on a PR to GTNH or any other repository.

**Entry gate:** `0.3.0-beta` is published.

**Scope, polish (in the jar):**
- **Questbook from the jar.** The GregGuns quest line ([12-questbook](gregguns-spec/12-questbook/index.md)) ships as resources in our jar. A hidden config key (not in the config GUI, off by default) installs it into the server's BetterQuesting database. Quest IDs stay stable so an install is repeatable.
- **PACK-CHANGES in the jar.** Each row of [PACK-CHANGES.md](gregguns-spec/PACK-CHANGES.md) is done from our jar or written up as an optional server-owner step. No PR to GTNH.
- **The rest of the polish:**
  - a balance pass along the GTNH tier progression;
  - NEI polish and config defaults;
  - translation readiness;
  - multiplayer and performance soak tests on the real pack;
  - GuideNH in-game wiki pages bundled in the jar;
  - the OpenModularTurrets overlap investigation (the Rampart stays);
  - aim down sights for iron-sight and red-dot guns.
- **Fixes from the owner's beta playthrough,** released as `0.3.x-beta`.

**Scope, handoff (last):** the handoff starts only after every other open item above is closed (owner, 2026-10-25).
- **Handoff guide** (`docs/HANDOFF.md`) for a developer who might adopt the mod. It covers:
  - the state of every area;
  - how to build and test, including the real-pack test with `-Dgregguns.testHooks=true`;
  - how the mod was made;
  - an open-ended path to inclusion. The policy is vague and its application is up to the pack's reviewers, so start with light edits and work out the requirements with the dev team.
- **Solicitation message.** We draft it and the owner posts it. It is open about how the mod was made.
- **README banner,** then **archive** the repository on the owner's go-ahead.

**Adopter list** (what M9 leaves for whoever adopts the mod):
- the GTNH modpack PR itself, with the GT5-Unofficial MTE ID and texture-page reservations;
- anything from the polish list that M9 doesn't finish;
- commissioned art and sound to replace the generated placeholder assets ([0017](design/0017-accept-sounds-archive-sound-pipeline.md)).

**Exit gate:**
- [x] The questbook installs from the jar behind its hidden config key, on a real GTNH 2.9 server. `0.3.1-beta`, 2026-10-21: 121 quests installed on the owner's GTNH 2.9.0-RC-2 server and saved by BetterQuesting.
- [ ] Every PACK-CHANGES row is done in the jar or documented as an optional server-owner step.
- [ ] The polish items above are done, or moved to the adopter list with the owner's sign-off.
- [ ] `docs/HANDOFF.md` is written and the owner has reviewed it.
- [ ] The solicitation message is drafted and the owner has approved it. The owner posts it.
- [ ] The README carries the archive banner, and the GitHub repository is archived, on the owner's go-ahead.

## Later

- [x] ~~Revisit the sound generators against [SOUND-DIFFERENCES](assets/SOUND-DIFFERENCES.md).~~ Closed 2026-10-25: the owner accepts the sounds as they are, and the sound pipeline is archived ([0017](design/0017-accept-sounds-archive-sound-pipeline.md)).
