# Roadmap

How GregGuns gets from an empty mod to a GTNH-ready release. The work follows the [Techguns behavioural spec](techguns-spec/README.md), rebuilt on GregTech progression.

## How to read this roadmap

- **Milestones (M0–M9)** are delivered in order. Each one has an **entry gate**, which must be true before work starts, and an **exit gate**, which is its definition of done. A milestone isn't finished until every exit-gate box is ticked.
- **Decision gates (D1–D6)** are design questions settled before the milestones that depend on them start. All six are **accepted by default** for the MVP: see [docs/design/](design/README.md). They're revisited after MVP.
  - Each decision is recorded in [`docs/design/`](design/README.md), and the gate is closed by marking that record `Status: Accepted`.
  - The tables below summarise the accepted records; the records are authoritative.
- Every milestone also meets the universal **definition of done** in [CONTRIBUTING.md](../CONTRIBUTING.md#definition-of-done): tests, comments, lang keys, original assets, no reproduced Techguns bugs, and spec deviations recorded.
- **Feature flags:** each milestone's content ships behind a config toggle such as `modules.armor`, defaulting to on once its exit gate is met. That way, unfinished or unwanted modules can be switched off in the pack without a rebuild.

Tick a box only in the PR that completes it.

```
M0 Foundation ──► D1–D4 ──► M1 Combat core ──► M2 Ballistic arsenal ──► M5 Energy & exotic weapons
                    │             │                    │
                    │             ├──► M3 Armour & equipment ─┘
                    │             └──► M4 Production chain & machines
                    └──► D5 ──► M7 NPCs ──► M8 Worldgen         M6 Utility & defence (after M4)
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
- [ ] `./gradlew runServer` boots with GT5-Unofficial loaded and no errors from GregGuns. The CI smoke test passes. *(Local boot verified 2026-10-18; CI pending the first push.)*
- [ ] `./gradlew runClient` reaches the main menu, and the GregGuns creative tab exists. *(Main menu verified 2026-10-18 and the tab registers; the tab itself still needs an in-game look.)*
- [x] Config module flags load, save and are documented.
- [x] Every packet base class rejects out-of-range, wrong-dimension and unauthorised requests, with unit tests.

---

## Decision gates

Each gate produces a design record in [`docs/design/`](design/README.md). Nothing a gate blocks may be merged until that record is accepted.

### D1 · Tier map. *Blocks M1–M8.*

**Accepted:** [0001-tier-map.md](design/0001-tier-map.md)

Every weapon, ammo type, armour set, component and machine is assigned a GT voltage tier and a **gating mechanism**: the machine voltage needed to craft it, or a material that can't be obtained before that tier.

The table below lists example gate materials. **Verify each one against the current GTNH progression before accepting D1.**

| Tier | Example gate material | Proposed content (from the spec) |
|---|---|---|
| Steam / ULV | Bronze, wrought iron | Hand Cannon (stone bullets); T1 scout and miner sets |
| LV | Steel | 9mm Pistol, Revolver, Sawed Off, Bolt Action Rifle; T1 combat set; Combat Knife, Crowbar |
| MV | Aluminium | Submachine Gun (Thompson), Mac10, Combat Shotgun, M4, AK47, frag grenades; T2 combat, commando and riot sets; gas mask, night-vision goggles |
| HV | Stainless steel | AUG, SCAR, Vector, AS50, LMG, Grenade Launcher, Rocket Launcher, Flamethrower, Chainsaw, Power Hammer; Infiltrator; akimbo variants of earlier tiers; jump pack, glider, oxygen gear |
| EV | Titanium | Laser Rifle, PDW, Pulse Rifle, Minigun, Tesla Rifle, Sonic Rifle, Bio Gun, Power Hammer (Advanced); T3 combat set; jetpack, steam armour |
| IV | Tungstensteel | Blaster Rifle, De-Atomizer Pistol, Scatter Laser Rifle, Golden Revolver; power armour, exo suit; anti-grav device |
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
| Repair Bench | **Keep** | Armour repair using GT materials |
| Camo Bench | **Keep** | Cosmetic, tasteful, no GT equivalent |
| Ore Drill (multiblock) | **Drop** | GT miners and the GTNH ore system |
| Scrapper | **Drop** | Unfinished in Techguns. GT recycling covers it |
| Turret | **Keep** | Core to the defence theme |
| Explosive charges | **Keep, gated** | Check for overlap with GT mining explosives |
| Weapon and ammo chests | **Keep, low priority** | Decorative storage |
| Techguns ores, ingots, plates, wires, circuits | **Drop** | GT materials and parts through ore-dict unification |
| Techguns fluids (oil, fuel, acid) | **Drop** | GT fluids |

### D3 · Energy model. *Blocks M3, M4 and M5.*

**Accepted:** [0003-energy-model.md](design/0003-energy-model.md)

- All power is **GT EU**; there is no RF anywhere.
- Energy weapons, powered armour and tools are IC2 electric items, charged by GT chargers and battery buffers.
- Fuel-based items (flamethrower, chainsaw, power hammer) take GT fluids.
- The record defines capacity and use per tier.

### D4 · Damage model. *Blocks M1.*

**Accepted:** [0004-damage-model.md](design/0004-damage-model.md)

- Decide whether to keep the Techguns damage types, armour penetration and per-type resistances, or simplify them.
- Decide how GregGuns damage scales against GTNH mobs and other mods' armour.
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
- Asset provenance rules. Everything is original; see [CONTRIBUTING.md § Assets](../CONTRIBUTING.md#assets-and-localisation).

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
- [ ] The pistol is craftable in the dev pack only through its D1 tier gate, and the recipe shows in NEI.
- [ ] Fire, reload and zoom work, and the pistol hits and damages mobs and players.
- [ ] Unit tests cover the damage formulas, drop-off, spread, ammo accounting and the reload state machine.
- [ ] A dedicated server with two clients shows correct sync: shots, reloads, other players' animations and sounds.
- [ ] Each relevant inherited bug from [BUGS.md](techguns-spec/BUGS.md) has a test showing it isn't reproduced. These include GUN-13, GUN-29, GUN-32, GUN-33 and GUN-34.

## M2 · Ballistic arsenal

**Entry gate:** M1 is done.

**Scope:**
- Every ballistic gun, grenade and akimbo variant from [02-guns](techguns-spec/02-guns/index.md), delivered in tier order (D1).
- Their ammo items, models, textures and sounds.

**Exit gate (per tier batch):**
- [ ] Every gun in the batch matches its spec sheet, or a deviation is recorded.
- [ ] Every recipe is gated by tier and visible in NEI.
- [ ] Unit tests cover each gun's unique behaviour, such as shotgun pellets, burst fire and the grenade fuse.
- [ ] Original textures and sounds are in place, following D6.

## M3 · Armour and equipment

**Entry gate:** M1 is done; D1 and D3 are accepted.

**Scope:**
- The T1 to T3 sets, steam armour, power armour and exo suit.
- Set bonuses.
- The extra Techguns inventory: face, back, hand and ammo slots.
- Masks, goggles, tanks, jetpack, glider, jump pack and anti-grav.
- The spec is [05-armor](techguns-spec/05-armor/index.md).

**Exit gate:**
- [ ] Protection and penetration values follow the D4 model, with unit tests.
- [ ] Bonuses apply and are removed cleanly, with no step-height or potion interference (CORE-14, CORE-15).
- [ ] The special-slot inventory syncs, persists and works with automation where the spec says it should.
- [ ] Powered items run on EU through GT chargers.

## M4 · Production chain and machines

**Entry gate:** M1 is done; D2 and D3 are accepted.

**Scope:**
- GT recipe-map recipes for every component and ammo type (see [06-items/components-and-materials.md](techguns-spec/06-items/components-and-materials.md)).
- The machines D2 keeps, built as GT MetaTileEntities. The spec is [08-machines-production](techguns-spec/08-machines-production/index.md) and [09-machines-utility](techguns-spec/09-machines-utility/index.md).
- NEI integration for any non-GT logic.

**Exit gate:**
- [ ] No GregGuns item duplicates a GT material or part. Unification is checked by the dev self-check.
- [ ] Every kept machine has a ModularUI2 GUI, EU input, NEI recipes and integration scenarios.
- [ ] MetaTileEntity IDs are reserved and recorded (see [CONTRIBUTING.md § GTNH integration](../CONTRIBUTING.md#gtnh-integration)).

## M5 · Energy and exotic weapons

**Entry gate:** M2 and M4 are done, plus M3 for the powered items.

**Scope:**
- Laser Rifle, Blaster Rifle, Tesla Rifle, Sonic Rifle, Pulse Rifle, PDW, Bio Gun, De-Atomizer Pistol, Minigun, Power Hammers, Chainsaw, Scatter Laser Rifle (if D1 keeps it), Nether Blaster, Alien Blaster, Nuclear Death Ray, Grim Reaper and Nuke Launcher.
- Their beams, chaining and explosions (see [04-projectiles-fx](techguns-spec/04-projectiles-fx/index.md)).

**Exit gate:**
- [ ] Each weapon is tier-gated with EU or fluid ammo per D3.
- [ ] Explosions respect config and protection (fixing PFX-14, PFX-25 and CORE-19), with tests.
- [ ] The particle and FX system carries the spec's FX catalogue, and performance is checked with many emitters on screen.

## M6 · Utility and defence

**Entry gate:** M4 is done.

**Scope:** the turret, explosive charges, chests, camp flag and Bio Blob that D2 keeps. The spec is [09-machines-utility](techguns-spec/09-machines-utility/index.md).

**Exit gate:**
- [ ] Turret targeting respects owners and friends, with no NPEs (NPC-12, MUTL-25).
- [ ] Charges use permission checks (MUTL-31).
- [ ] An integration scenario exists for each block.

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

## M9 · Pack integration and release

**Entry gate:** every milestone selected for 1.0 is done.

**Scope:**
- A balance pass with the GTNH tier progression.
- NEI polish and config defaults.
- Translation readiness: every string is a lang key.
- Multiplayer and performance soak tests.
- Handing quest-line content to the GTNH modpack maintainers, since quests live in the pack, not in this repo.
- One PR to the GTNH modpack repository with every row of [PACK-CHANGES.md](gregguns-spec/PACK-CHANGES.md), using the final registry names; and the GT5-Unofficial MTE ID and texture-page reservations.
- Release tagging.

**Exit gate:**
- [ ] A full playthrough of every tier gate in a GTNH dev pack.
- [ ] No unresolved crash-severity bugs.
- [ ] The PACK-CHANGES modpack PR is open, with no placeholder names left.
- [ ] Release notes, a tagged version and published jars (CI `release-tags` workflow).
