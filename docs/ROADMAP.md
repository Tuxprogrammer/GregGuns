# Roadmap

How GregGuns gets from an empty mod to a GTNH-ready release. The work follows the [Techguns behavioral spec](techguns-spec/README.md), rebuilt on GregTech progression.

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
                                                    all ──► M9 Pack integration & release
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
| Reaction Chamber (multiblock) | **Keep, candidate** | Unique focus, intensity and radiation gameplay. Rebuild as a GT multiblock running on EU |
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
- [x] Fire, reload and zoom work, and the pistol hits and damages mobs and players. *(`gun.*` scenarios.)*
- [x] Unit tests cover the damage formulas, drop-off, spread, ammo accounting and the reload state machine. *(See the M1 coverage table in docs/testing/README.md.)*
- [x] A dedicated server with two clients shows correct sync: shots, reloads, other players' animations and sounds. *(`mp_check`: `mp.sync`, `mp.visual`, `mp.give` and client log evidence.)*
- [x] Each relevant inherited bug from [BUGS.md](techguns-spec/BUGS.md) has a test showing it isn't reproduced. These include GUN-13, GUN-29, GUN-32, GUN-33 and GUN-34.

## M2 · Ballistic arsenal

**Entry gate:** M1 is done.

**Scope:**
- Every ballistic gun, grenade and akimbo variant from [02-guns](techguns-spec/02-guns/index.md), delivered in tier order (D1).
- Their ammo items, models, textures and sounds.

**Exit gate (per tier batch):**
- [x] Every gun in the batch matches its spec sheet, or a deviation is recorded. Evidence: stats pinned by `GunCatalogTest`, `MvGunsLogicTest`, `HvRiflesLogicTest`, `HeavyGunLogicTest` and the launcher, flame and golden revolver tests; deviation sections for every batch in [02-guns](gregguns-spec/02-guns/index.md) ([M2 exit gate](gregguns-spec/02-guns/m2-exit-gate.md#1-every-gun-matches-its-spec-sheet-or-a-deviation-is-recorded)).
- [ ] Every recipe is gated by tier and visible in NEI. Gating is met (`GunDefinitionsTest` over every row with `GateRule`, a `*.recipe` or `*.combine` scenario per gun); NEI visibility is pending the batched in-game session ([M2 exit gate](gregguns-spec/02-guns/m2-exit-gate.md#2-every-recipe-is-tier-gated-and-visible-in-nei)).
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

## M4 · Production chain and machines

**Entry gate:** M1 is done; D2 and D3 are accepted.

**Scope:**
- GT recipe-map recipes for every component and ammo type (see [06-items/components-and-materials.md](techguns-spec/06-items/components-and-materials.md)).
- The machines D2 keeps, built as GT MetaTileEntities. The spec is [08-machines-production](techguns-spec/08-machines-production/index.md) and [09-machines-utility](techguns-spec/09-machines-utility/index.md).
- NEI integration for any non-GT logic.

**Exit gate:**
- [x] No GregGuns item duplicates a GT material or part. Unification is checked by the dev self-check. Evidence: `UnificationChecks` with its unit tests, passing on every boot ([M4 exit gate](gregguns-spec/08-machines-production/m4-exit-gate.md)).
- [x] Every kept machine has a ModularUI2 GUI, EU input, NEI recipes and integration scenarios. Evidence: the per-machine table, with recorded exceptions: the Steam press runs on steam, the Camo Bench needs no EU, and the benches and Gantry have no recipe map to show in NEI ([M4 exit gate](gregguns-spec/08-machines-production/m4-exit-gate.md)).
- [x] MetaTileEntity IDs are reserved and recorded (see [CONTRIBUTING.md § GTNH integration](../CONTRIBUTING.md#gtnh-integration)). Recorded in `GregGunsMTEIDs`, the roster and `GregGunsMTEIDsTest`. The GT5-Unofficial `MetaTileEntityIDs` named-entry reservation is a pack-side PR listed in [PACK-CHANGES.md](gregguns-spec/PACK-CHANGES.md) and lands with M9 ([M4 exit gate](gregguns-spec/08-machines-production/m4-exit-gate.md)).

## M5 · Energy and exotic weapons

**Entry gate:** M2 and M4 are done, plus M3 for the powered items.

**Scope:**
- Laser Rifle, Blaster Rifle, Tesla Rifle, Sonic Rifle, Pulse Rifle, PDW, Bio Gun, De-Atomizer Pistol, Minigun, Power Hammers, Chainsaw, Scatter Laser Rifle (if D1 keeps it), Nether Blaster, Alien Blaster, Nuclear Death Ray, Grim Reaper and Nuke Launcher.
- Their beams, chaining and explosions (see [04-projectiles-fx](techguns-spec/04-projectiles-fx/index.md)).

**Exit gate:**
- [x] Each weapon is tier-gated with EU or fluid ammo per D3. Evidence: `GunDefinitionsTest` over every row with `GateRule`, a `*.recipe` or `*.tiergate` scenario per gun, and the per-gun fire scenarios asserting EU or fluid spend ([M5 exit gate](gregguns-spec/04-projectiles-fx/m5-exit-gate.md#1-each-weapon-is-tier-gated-with-eu-or-fluid-ammo-per-d3)).
- [x] Explosions respect config and protection (fixing PFX-14, PFX-25 and CORE-19), with tests. Evidence: `ExplosionGateTest` (`doesNotReproducePFX25_*`, `CORE19_*`, `PFX14_*`), `grenade.protected`, `explosion.nuke.sliced` ([M5 exit gate](gregguns-spec/04-projectiles-fx/m5-exit-gate.md#2-explosions-respect-config-and-protection-pfx-14-pfx-25-core-19-with-tests)).
- [ ] The particle and FX system carries the spec's FX catalog, and performance is checked with many emitters on screen. The catalog and the 500-emitter budget are proven headless (`FxCatalogTest`, `ParticleBudgetTest`, `fx.stress`, `fx.stress.nuke`); real frame cost, the FX visuals, the NDR pulse and the alpha cut-outs wait for the batched client session ([M5 exit gate](gregguns-spec/04-projectiles-fx/m5-exit-gate.md#3-the-particle-and-fx-system-carries-the-fx-catalog-and-performance-is-checked)).

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
- [ ] Spawning is config-gated, respects dimension and biome rules, and stays within caps (NPC-14, NPC-15, NPC-17).
- [ ] NPC difficulty scaling is applied once (GUN-06).
- [ ] Mobs despawn correctly (NPC-28).
- [ ] A soak test with spawning enabled shows no TPS regression in the dev pack.

## M8 · Worldgen and structures

**Entry gate:** M7 is done and D5 is accepted.

**Scope:** the structures D5 selects, their loot and their NPC camps. There is **no** ore generation. The spec is [11-worldgen](techguns-spec/11-worldgen/index.md).

**Exit gate:**
- [ ] No cascading worldgen (WGEN-04), verified by the chunk-load test.
- [ ] Structures respect the dimension whitelist and GTNH's world types.
- [ ] Loot is tier-appropriate for where the structure spawns.

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
- **Client pass.** A batched in-game session covers every gun, armor set, machine, multiblock, NPC and structure. It records screenshots, and checks that the game runs with and without shaders.
- **Release.** Tag `0.3.0-beta`, cut it as a GitHub pre-release, and write release notes aimed at server owners.
- **Owner playthrough.** The owner plays the beta on a local multiplayer server. Bugs found there are fixed in `0.3.x-beta` builds.

**Exit gate:**
- [ ] Every milestone M0–M8 has a completeness audit, with each exit box met or deferred with the owner's sign-off.
- [ ] The recipe-clash review found no unresolved clashes with GTNH 2.9 recipes. Each clash it found was fixed, or recorded in PACK-CHANGES with the owner's sign-off.
- [ ] The batched client pass is clean, including with shaders on, and has no crash-severity or invisible-render bugs.
- [ ] `0.3.0-beta` is published.
- [ ] The owner's beta playthrough on a local server is done, and its blocking bugs are fixed.

## M9 · Pack integration and release

**Entry gate:** the beta review gate is closed, including the owner's beta playthrough, and every milestone selected for 1.0 is done.

**Scope:**
- A balance pass with the GTNH tier progression.
- NEI polish and config defaults.
- Translation readiness: every string is a lang key.
- Multiplayer and performance soak tests.
- Handing quest-line content to the GTNH modpack maintainers, since quests live in the pack, not in this repo.
- Drafting, but not opening, the PR to the GTNH modpack repository. The draft carries every row of [PACK-CHANGES.md](gregguns-spec/PACK-CHANGES.md), using the final registry names, plus the GT5-Unofficial MTE ID and texture-page reservations. The owner decides when to open it.
- An outreach package for the GTNH pack maintainers, to give the mod the best chance of being accepted into GTNH. It covers:
  - a short pitch: what the mod adds and why it fits GT progression;
  - how it respects GTNH's conventions (GT materials, recipe maps, tiers, no ore generation, config-gated content);
  - performance and compatibility evidence (soak tests, TPS, FX budget, shader check);
  - license and originality (MIT, a clean-room rewrite, no Techguns code or assets);
  - the maintenance commitment;
  - the quest-line handoff;
  - links to the release, the docs and the infographics.
- GuideNH pages: an in-game wiki for the mod, covering getting started, the arsenal by tier, ammo and magazines, armor, machines and multiblocks, NPCs, and structures. Build them from the docs and infographics. This work may start during the beta, before the rest of M9.
- Release tagging.

**Exit gate:**
- [ ] A full playthrough of every tier gate in a GTNH dev pack.
- [ ] No unresolved crash-severity bugs.
- [ ] The GTNH modpack PR text is drafted (not opened), with every PACK-CHANGES row and no placeholder names left. The owner reviews it and decides when to open it.
- [ ] The outreach package for the GTNH pack maintainers is drafted and reviewed by the owner.
- [ ] The GuideNH in-game wiki pages ship in the mod, cover every content area, and pass the owner's review in game.
- [ ] Release notes, a tagged version and published jars (CI `release-tags` workflow).

## Later

- [ ] Revisit the sound generators against [SOUND-DIFFERENCES](assets/SOUND-DIFFERENCES.md) (the doc is written when the clean-room sound loop ends).
