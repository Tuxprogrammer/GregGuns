# ICBM roadmap

How the GregGuns ICBM module (`modules.icbm`) gets from a finished spec to a beta: missiles, warheads, launchers, detection and defense, rebuilt on GT and Galacticraft. The program is set by [0018](design/0018-icbm-program.md). The behavior baseline is the [ICBM spec](icbm-spec/README.md), and the GregGuns delta is the [GregGuns ICBM spec](gregguns-icbm-spec/README.md) with its [decisions](gregguns-icbm-spec/DECISIONS.md).

GregGuns' own final polish stays on [ROADMAP.md](ROADMAP.md) (M9). The handoff comes after both, per [HANDOFF-PLAN.md](HANDOFF-PLAN.md).

## How to read this roadmap

- **Milestones I0–I7** are delivered in order, with an entry gate and an exit gate each, as on the main roadmap. The universal [definition of done](../CONTRIBUTING.md#definition-of-done) applies to every change.
- **Spec first.** Each milestone's first task writes or finishes its GregGuns-side sheets in [gregguns-icbm-spec](gregguns-icbm-spec/README.md), following the [GregGuns spec conventions](gregguns-spec/CONVENTIONS.md). Builders work from those sheets and the ICBM baseline, never from the ICBM source, except for `MIT-portable` sections (D2).
- **Two gates involve the owner:** the alpha gate (one review) and the beta gate (playthrough and feedback). Everything between them is autonomous.
- **The module stays off by default** (`modules.icbm=false`) until the beta gate, so GregGuns `0.3.x-beta` fixes keep shipping from master.

```
I0 Foundation ──► I1 Warheads & blast engine ──► I2 Missiles & the GC pad (T1–T2)
   ──► I3 Silo & heavy warheads (T3, nuclear) ──► I4 Detection & defense ──► I5 Endgame warheads & systems
   ──► I6 World: missile silos on every planet ──► Alpha gate (owner review) ──► I7 Hardening
   ──► Beta gate (owner playthrough) ──► HANDOFF-PLAN
```

## Working rules (autonomous build)

From I0 on, the build runs without the owner ([0018](design/0018-icbm-program.md) §12–15).

- **Decisions.** The orchestrator decides every open question itself. It adds each decision to [DECISIONS.md](gregguns-icbm-spec/DECISIONS.md) as `D<n>`, marked "orchestrator" with the date, and doesn't wait for anyone. The owner can revise any of them at the alpha review. Changes after that are reactive: they answer a found problem.
- **Agents.**
  - At most **two Sonnet coding agents** at once, each in its own worktree on a branch `icbm/<milestone>-<topic>`.
  - **A balanced pair** ([D42](gregguns-icbm-spec/DECISIONS.md)): the milestone is split by effort, not by kind, so both agents write code. The cut follows a clean seam, such as an API one side defines and the other uses. Each agent gets the other's ID and messages it directly about anything on the seam or anything that needs both; the orchestrator hears only of a disagreement. The orchestrator records each agent's time and tokens and rebalances the next split.
  - **One Sonnet guides agent** ([D39](gregguns-icbm-spec/DECISIONS.md)) owns what players see outside the code: guide sheets, preview sheets, quests, GuideNH pages, README and release-note text, and the wording pass over tooltips and lang keys. It drafts sheets with placeholder numbers alongside the coders (D50) and finalizes them from the code once the content lands, so the finished facts match the code. See [Visuals and player guides](#visuals-and-player-guides).
  - Small, scoped briefs. An agent that passes about 250k tokens of context writes a handoff file and a fresh agent takes over.
  - **One Opus reviewer agent per milestone,** run at the milestone's end. It reads the milestone's diff against this roadmap, the spec sheets and 0018, and returns one fix list. No per-feature reviews and no Opus coding agents.
- **Branch gate, automated only:**
  - `spotlessCheck`;
  - the build and unit tests;
  - the ICBM scenarios in a fresh worktree, `gregguns test icbm.` with every module enabled ([D68](gregguns-icbm-spec/DECISIONS.md));
  - a `--no-galacticraft` boot with icbm on, which must log the module turning itself off ([D46](gregguns-icbm-spec/DECISIONS.md));
  - when a branch touches base GregGuns code, one targeted prefix run of the touched areas at the agent's final wrap-up (D68);
  - the ported-code gate (I0).

  The full suite, with and without Galacticraft, runs only at the alpha and beta checkpoints (D68).

  The orchestrator lands only when master is an ancestor of the verified branch, then merges with `--no-ff`.
- **Client checks.** One batched in-game session per milestone, on the test server. Shader runs only for shader questions. Its screenshots go to the visuals gallery.
- **Ported MIT code** follows [CONTRIBUTING § Ported code](../CONTRIBUTING.md#ported-code-mit): adapted, commented, tested and bug-fixed before it merges.
- **Assets** ([D47](gregguns-icbm-spec/DECISIONS.md), [0018](design/0018-icbm-program.md) §10–11). Each milestone sources the assets its content needs, in this order:
  1. **Human-made, properly licensed assets:** asset libraries, and assets extracted from mod jars under CC0, CC-BY, MIT or LGPL. A mod's declared license covers its whole jar, assets included, unless the mod licenses its assets separately. Look first in the GTNH pack's own mods, then on Modrinth, CurseForge and similar.
  2. **Our own procedural generation,** only when nothing licensed exists, and never with tuning loops.
  3. **Before generating anything,** the orchestrator names the asset and the planned fallback in that milestone's owner update. The build doesn't wait.
- **Releases.** Tags follow the main repo's release steps. Nothing is pushed or released without the owner's go-ahead, apart from what this roadmap names.

## Balance targets

These come from [0018](design/0018-icbm-program.md) §5–7, and every milestone checks against them.

- **Cost:** an automatable arsenal. A missile costs about one of its tier's Galacticraft rocket launches in plates, circuits and fuel, and is Assembly Line-automatable. The warhead is the expensive part, and cost rises steeply by tier.
- **Conventional center damage, at least:** T1 (HV) 1,200; T2 (EV–IV) 3,000; T3 (LuV) 8,000. Damage falls off to zero at the blast edge.
- **Nuclear (ZPM), antimatter (UV) and red matter (UHV):** every living entity in the kill zone dies, bosses included. Players are subject only to server damage settings.
- **Tiers** (D5): T1 HV; T2 EV–IV; T3 LuV–ZPM, with nuclear at ZPM; antimatter at UV; red matter at UHV.

---

## Carried over from the GregGuns roadmap

Comparing this roadmap with [ROADMAP.md](ROADMAP.md) found checks that GregGuns made of every feature but that this plan didn't name ([D44](gregguns-icbm-spec/DECISIONS.md)). They apply to every milestone on top of its own exit gate:

- **Tooltips follow the GregGuns conventions** ([D41](gregguns-icbm-spec/DECISIONS.md)):
  - tier names in GT colors through `TierText.colored`;
  - the "Added by: GregGuns" line on every machine (`AddedBy`);
  - a short tooltip with the essentials, and the details behind Shift;
  - numbers formatted in a Minecraft-free `*TooltipFormat` class with unit tests, as `GunTooltipFormat` and `AmmoTooltipFormat` do;
  - every line from a lang key;
  - compatibility lists where items fit together (which warheads fit which body, which fuels a tier burns), as the ammo and magazine tooltips do;
  - the annihilation line the Nuke Launcher already uses, for nuclear and higher warheads;
  - labeled GUI buttons with tooltips, and a locked or disabled control that says why, as on the Turret and charge GUIs.

  A warhead shows its explosive, tier, fill and blast radius, with its center damage or the annihilation line. A missile body shows its tier, range and fuel.
- **NEI:** every recipe and every non-GT process is visible (M2, M4).
- **Unification:** no ICBM item duplicates a GT material or part. The dev self-checks (`UnificationChecks`, the recipe tier check) cover ICBM content (M0, M4).
- **MTE IDs** for ICBM machines are reserved in `GregGunsMTEIDs` and its test before the first ICBM machine lands (M4).
- **GT recycling data** on missile bodies, warheads and machines (M3).
- **A scenario for every block and machine** (M6).
- **Multiplayer sync:** a dedicated server with two clients sees the same launch, flight and blast (M1's `mp_check`), first in I2.
- **A recipe-clash review** against the full GTNH pack with `tools/recipe-clash` before the beta (the GregGuns beta gate).
- **PACK-CHANGES:** anything the pack would have to change for ICBM is done from our jar or written up as an optional server-owner step (M9).
- **GuideNH pages:** if GregGuns has shipped its own by then (M9), they get an ICBM section.
- **Server-owner docs:** release notes with every tag, and every config key in [config.md](config.md).

## Visuals and player guides

GregGuns explains itself to players through rendered guide sheets and its quest line, and the ICBM module gets the same ([D39](gregguns-icbm-spec/DECISIONS.md)). A dedicated guides agent owns this track (see the working rules).

**What exists** (audit, 2026-10-25):
- **Player guide sheets:** 13 stand-alone infographics in `docs/assets/infographics/`, built by [tools/asset-gen/infographics](../tools/asset-gen/infographics/README.md) from real renders of our models and GT textures. The tooling was written on Windows: it drives Edge through Playwright, and its docs use `cygpath`. On the Linux host it needs Playwright's own Chromium and plain paths.
- **The renderer** (`render3d.py`) draws 1.7.10 box models and voxel scenes only. The ICBM models are meshes ([D43](gregguns-icbm-spec/DECISIONS.md)), so it needs a triangle-mesh path that reads the same OBJ files the game loads.
- **Preview sheets** in `docs/assets/previews/` (models, icons, blocks, GUIs, particles) for reviewing new art before it reaches a guide.
- **The quest line,** built by [tools/questbook](../tools/questbook/README.md) from `quests.json` with stable keys and installed from the jar.
- **Screenshots** from the batched client sessions.

**Rules,** on top of the [infographic writing rules](../tools/asset-gen/infographics/README.md#writing-rules) (player language; no IDs, specs or repo links; the mod described as finished; strong contrast on every element; GT textures credited):
- **Credit travels with the image.** A sheet that shows a CC-BY model carries a one-line credit in its footer. ICBM-Classic's own art is never shown.
- **Minecraft content follows Mojang's usage guidelines** (D55). Real vanilla textures are fine on guide sheets: read from the reference pack's Minecraft jar at build time and credited "Minecraft textures © Mojang / Microsoft; not an official Minecraft product." Nothing may look official, and Mojang assets never ship in the mod jar.
- **Final numbers come from the code.** On a finished (unstamped) sheet, radii, damage, ranges, costs and loot ceilings are read from the code or its exports. When a balance change moves one, the sheet is rebuilt in the same milestone.
- **Files:** ICBM guide sheets sit beside the GregGuns ones with an `icbm-` prefix and are committed. **Preview sheets and the gallery page stay local** ([D50](gregguns-icbm-spec/DECISIONS.md)): previews are built into `docs/assets/previews/icbm/`, which is git-ignored, and never enter the repository's history.
- **Placeholder sheets are allowed** (D50), so the guides agent works alongside the coders instead of waiting. A guide sheet may be drafted from the spec's planned numbers with a visible "Draft: numbers pending" stamp, and committed. Once the content lands, the sheet is rebuilt with numbers from the code and the stamp removed.
- **Quests live in the GregGuns tab** ([D40](gregguns-icbm-spec/DECISIONS.md)), keyed `gg.icbm.*`, and are added in the milestone that ships their content. There is no separate ICBM tab.

**Planned guide sheets:**

| Sheet | Shows | Milestone |
|---|---|---|
| `icbm-warheads` | Every warhead by tier: model, blast type, radius, center damage or kill zone, and the casing that caps its fill | I1 (T1–T2), extended in I3 and I5 |
| `icbm-first-missile` | Step by step: chip, schematic, body, warhead, stacking on the Galacticraft pad, fuel, launch control, launch | I2 |
| `icbm-missile-silo` | The silo multiblock build guide, with hatches and block counts, like the GregGuns multiblock guides | I3 |
| `icbm-detection-and-defense` | Radars and their ranges by tier, the air-defense battery and its SAMs, the EMP tower, and how they link | I4 |
| `icbm-fallout` | Fallout zones, protection, the processing chain and the antidote | I5 |
| `icbm-silos` | Abandoned silos as dungeons: activity levels, guards, and the loot to expect on each planet | I6 |
| `icbm-recipes` | The ICBM crafting chains, read from the recipe export | I7 |
| `icbm-hero` | The module at a glance, for the README and for posting | I7 |

**Owner check-ins** (not a gate): when new visuals land on master, the orchestrator tells the owner that day what is new and where to look. Every ICBM visual is collected, newest first, in the local gallery page `docs/icbm-visuals.html`, which links the local previews and the committed guide sheets. The page and the previews are git-ignored (D50), and nothing is hosted unless the owner asks. The owner's notes, if any, join the next milestone's fix list, and the build doesn't wait for them. The alpha review covers all visuals formally.

---

## I0 · Foundation

**Entry gate:** [0018](design/0018-icbm-program.md) accepted, which is this plan.

**Scope:**
- The `modules.icbm` flag (default off) and module registration.
  - It turns itself off, with one log line, when Galacticraft is absent (D9).
  - The Galacticraft and OpenComputers APIs are optional compile dependencies, guarded with `@Optional`.
- **The ported-code gate:**
  - `THIRD-PARTY-NOTICES.md` with the ICBM-Classic MIT notice.
  - An automated test, `PortedCodeGateTest`. For every source file with a `Ported from ICBM-Classic` header, it checks:
    - the header has a permalink pinned to `9c1d985a`;
    - a matching notice entry exists;
    - the class has Javadoc;
    - a test class exists;
    - every `Avoids: ICBM-xx-nn` ID exists in [BUGS.md](icbm-spec/BUGS.md).
- **The third-party asset folder and its rules** (license text beside the files, a CREDITS section), per CONTRIBUTING.
- The GregGuns-side area index stubs (02–08) in [gregguns-icbm-spec](gregguns-icbm-spec/README.md), each listing which decisions it will carry.
- A CLAUDE.md map row for the ICBM roadmap and spec pair.
- **ICBM MTE IDs** reserved in `GregGunsMTEIDs`, with its test.
- **Visual tooling on Linux** (guides agent): the guide and preview tools run on this host with Playwright's Chromium, and `render3d.py` gains a triangle-mesh path for OBJ models ([D43](gregguns-icbm-spec/DECISIONS.md)).

**Exit gate:**
- [x] `runServer` boots with `modules.icbm=true` with Galacticraft present, and with it absent (the module turns itself off). Evidence: `icbm.module_gate` on both branches; the runner asserts the single self-disable warning without Galacticraft (`--expect-log-once`). Full suite at `4c0ff661` on the test server: 427 passed, 0 failed with Galacticraft and 427 passed, 0 failed without it, 2026-10-25.
- [x] `PortedCodeGateTest` fails on a planted bad sample and passes on a good one. It also fails on a missing or near-empty source root, an unmarked ICBM-Classic mention, an empty `Avoids:` line and an untested bug ID (I0 review R6), each with a fixture.
- [x] The notices file, the third-party asset folder and the CREDITS section exist. The notices file also ships in the jar, with a test that keeps both copies identical (R8).
- [x] The 13 GregGuns guide sheets rebuild on Linux with no visible change, checked side by side, and a test OBJ model renders on a preview sheet. With Segoe UI and Bahnschrift installed, 12 of 13 sheets rebuild at the same size with 0.5–3% of pixels differing (glyph rasterization), and the Ammunition Works sheet is 24 px shorter because one paragraph wraps differently. The test ogive renders through `obj_preview.py`, with the reader held to Forge's OBJ grammar (R9).
- [x] The milestone review fix list is closed: R1–R12 from the review plus O1 (gradlew mode) and O2 (preview tooling output redirect; the committed previews predate the current model set), 2026-10-25.

## I1 · Warheads and the blast engine

**Entry gate:** I0 done.

**Scope:**
- **One shared explosive registry** (D12). ICBM blast types become warheads in the GregGuns blast pipeline (`ExplosionRules`, `BlockBreakGuard`, `ExplosionEvent.Detonate`), never a second block-breaking path.
- **The blast engine:**
  - ray-cast blasts and threaded scheduling (`MIT-portable`);
  - one tick budget shared by every running blast;
  - correct save and load of blasts in progress.
- **Warhead items** with NBT `{explosive, fill}`, capped by casing tier (D23).
- **Warhead blocks.** A warhead can be placed in the world, and alone it is a demolition charge with a fuse (0018 §8). It is a **real 3D warhead model**, sized by tier (T1–T2 in 1×1×1, larger warheads with filler blocks), with a matching collision box, rendered as a static block model ([D31](gregguns-icbm-spec/DECISIONS.md#d31-in-detail)).
- **The T1–T2 blast types kept by D10,** with their effects (gas, incendiary, smoke and so on) and the conventional damage targets.
- The annihilation rule's shared implementation, with a bosses flag (0018 §7). It is unit tested here; nuclear content arrives in I3.
- Warhead recipes in the GT Assembler at the casing tier (D57), tier-gated and visible in NEI.
- **Visuals:** a preview sheet of the warhead models by tier, the first `icbm-warheads` sheet (T1–T2), and quests for the first warheads.

**Exit gate:**
- [x] Every I1 blast type detonates through the shared pipeline and respects protection and the per-tier `icbm.terrainDamageT<n>` settings (D52). Scenario per type.
- [x] Conventional warheads meet the T1 and T2 center-damage targets against Infernal-scale health pools. Unit test plus scenario.
- [x] Several large blasts at once stay within the tick budget, and a blast survives a save and reload. Scenario.
- [x] Warhead blocks fuse and detonate by redstone and by hand. Each shows its 3D model with a matching collision box, and multi-block warheads break as one piece.
- [x] Batched client session: placed warheads look like warheads, not cubes.
- [x] Warhead tooltips follow the GregGuns conventions, with unit-tested formatting.
- [x] The warhead preview sheet is built locally, `icbm-warheads` (T1–T2, final numbers) and the warhead quests have landed, and the owner has been told.
- [x] The relevant `ICBM-02-*` bugs are avoided, with tests.
- [x] The milestone review fix list is closed.

## I2 · Missiles and the Galacticraft pad (T1–T2)

**Entry gate:** I1 done.

**Scope:**
- **Getting a missile body:**
  - a missile chip in the Circuit Assembler, then a schematic in the Laser Engraver (D8);
  - our NASA Workbench page for building by hand;
  - Assembly Line recipes, behind a research data stick, for automation (D27).
- **The stack-on-pad build** (0018 §8): place the body on the Galacticraft pad and the warhead on its nose; arming fuses the stack into the missile entity. The body is a full-height 3D missile model on its fins, the warhead seats onto it, and the flying entity uses the same model ([D31](gregguns-icbm-spec/DECISIONS.md#d31-in-detail)).
- **Fuel** keyed to tier through Galacticraft's `RocketFuels` lists and fuel loader (D19). A fixed amount per tier, and unburned fuel adds to the blast (D20).
- **A launch-control block beside the pad.** Targeting by coordinates, data stick, ModularUI2 GUI, redstone or the GT machine-controller cover (D13).
- **Flight:**
  - a real entity for the whole flight, with a ballistic arc (`MIT-portable`);
  - a two-chunk sliding chunk window (D25);
  - a cap on missiles in flight (D11);
  - tiered guidance scatter (D24);
  - impact fusing only (D26).
- **The pad is never removed or damaged** (0018 §9).
- GT recycling data on bodies and warheads.
- **Carried over from I1: blast visuals.** Gas warheads (Chemical, Debilitation, Smoke, Contagious) show only a few vanilla swirl particles, which don't read as a cloud. Give each gas type a dense, colored cloud that fills its radius for its duration, and give large conventional blasts a scaled smoke plume of their own (the long-range nuke flash now belongs to annihilating explosives only). Client-side, cheap, and checked in the batched client session.
- **Visuals:** previews of the missile bodies, the stacked missile and the launch-control GUI; the `icbm-first-missile` sheet; quests from the chip to the first launch; screenshots and a short clip of build, launch, flight and impact from the client session.

**Exit gate:**
- [x] A T1 and a T2 missile are built by hand and by Assembly Line, stacked, fueled, launched and detonated on target. Scenario.
- [x] The Galacticraft pad, fuel loader and anything attached are intact after a launch. Scenario `icbm.pad.survives`.
- [x] A missile never holds more than two chunk tickets, and every ticket is released on impact, on timeout and on unload. Scenario.
- [x] Recipes and costs match the cost target, and everything is visible in NEI.
- [x] The relevant `ICBM-03-*` and `ICBM-04-*` bugs are avoided, with tests.
- [x] Batched client session: build, launch and impact look right, and gas clouds and large blasts read clearly.
- [x] Two clients on a dedicated server see the same launch, flight and impact (`mp_check`).
- [x] `icbm-first-missile` and the launch quests have landed, and the owner has been told.
- [x] The milestone review fix list is closed.

## I3 · The silo and heavy warheads (T3, nuclear)

**Entry gate:** I2 done.

**Scope:**
- **The missile silo:** a GT multiblock under [0011](design/0011-machine-policy.md), powered by EU.
  - Fuel through a GT fluid input hatch (D22).
  - Body and warhead through input buses or by hand, mated automatically (0018 §8).
- **T3 missiles,** and the nuclear warhead at ZPM sharing the nuclear blast with the Nuke Launcher at its own scale (D12).
- **Annihilation** in nuclear kill zones: every living entity, bosses included (0018 §7).
- **Basic fallout:** irradiated surface blocks with GT radioactivity, protection by any hazmat-capable armor, and every hard cap from D21 (surface only, no spreading, no random ticks, a per-blast budget). The full byproduct chain waits for I5.
- The T3 conventional damage target.
- **Carried over from I2:** the Launch Control's Shift tooltip is wider than a small window and clips at the left edge; wrap its lines the way the warhead tooltip does. Take one NEI shot of a real Galacticraft rocket page beside our missile workbench page (the harness couldn't open GC's page by item or output id).
- **Carried over from I1:** the blast engine works out a crater's blocks in one pass on the server thread (`BlastJob.decide()`). That is fine up to radius 20, about 13,000 blocks, but a radius-96 blast would mean about 1.2 million. Make it incremental under the tick budget before nuclear scale, and give `icbm.blast.crater_size` a nuclear-radius case.
- **Visuals:** a silo preview, the `icbm-missile-silo` build guide, `icbm-warheads` extended to T3 and nuclear, and quests for the silo and the nuclear warhead.

**Exit gate:**
- [ ] The silo forms, loads, fuels and launches a T3 missile without the player touching the missile. Scenario.
- [ ] Nothing living survives inside the nuclear kill zone, a boss included. Players follow server damage settings. Scenario.
- [ ] A nuclear-radius crater is worked out incrementally, within the tick budget. Scenario.
- [ ] Fallout stays within its caps: a scenario counts converted blocks and server tick time for a nuclear strike.
- [ ] `icbm-missile-silo`, the extended `icbm-warheads` and the silo quests have landed, and the owner has been told.
- [ ] The milestone review fix list is closed.

## I4 · Detection and defense

**Entry gate:** I3 done.

**Scope:**
- **Radar:** single-block radars at HV and EV, and a multiblock radar tower from IV, with range by tier (D14).
  - They read the missile tracker, and a missile counts as "incoming" by its predicted impact point.
  - The sweep is `MIT-portable`.
- **The air-defense battery** (D15): it holds SAMs and fires at threats a linked radar reports. The intercept is `MIT-portable`.
- **The EMP tower** (D16):
  - it drains stored EU and gives each multiblock one random maintenance issue;
  - missiles use the kill-chance table;
  - each pulse affects at most N devices, nearest first;
  - EU cost grows continuously with radius.
- **Outputs on every machine:** redstone, GT covers, data-stick links, and OpenComputers components (D13; the component list in [golden-dome.md](gregguns-icbm-spec/01-core/golden-dome.md)).
- Handheld tools that are kept (radar gun, remote detonator, defuser), per the 05 sheet.
- **Visuals:** previews of the radars, the battery, the EMP tower and the handheld tools; the `icbm-detection-and-defense` sheet; quests for defense.

**Exit gate:**
- [ ] A radar flags an incoming missile and a linked battery intercepts it. Scenario.
- [ ] An EMP drains EU and causes maintenance issues within its device cap, with no tick spike in a dense base. Scenario.
- [ ] The OpenComputers components work as specified (GGI-01-01 to 06). Unit tests plus the batched client session.
- [ ] `icbm-detection-and-defense` and the defense quests have landed, and the owner has been told.
- [ ] The milestone review fix list is closed.

## I5 · Endgame warheads and systems

**Entry gate:** I4 done.

**Scope:**
- **Antimatter (UV)** and **red matter (UHV),** both annihilating.
  - Red matter is rebuilt with a bounded lifetime, an idle state, correct save and load, and the shared tick budget (D10).
  - The owner tests red matter in game; our side runs unit tests and headless boots.
- **The full fallout chain** (D21): irradiated rubble, trinitite and fallout goo, with their GT processing yields.
- **The Golden Dome floppy** and its programs (D18).
- **Warheads for existing GregGuns launchers** (D12).
- **The antidote,** mapped to GT hazards (D10).
- **Visuals:** antimatter and red matter previews and effects, the `icbm-fallout` sheet, the complete `icbm-warheads`, and the endgame quests.

**Exit gate:**
- [ ] Antimatter and red matter detonate, annihilate and finish within the tick budget, and red matter survives a save and reload. Scenario.
- [ ] Fallout blocks process into their yields in GT machines. Scenario.
- [ ] The Golden Dome programs run (GGI-01-07 to 09) in the batched client session.
- [ ] `icbm-fallout`, the complete `icbm-warheads` and the endgame quests have landed, and the owner has been told.
- [ ] The milestone review fix list is closed.

## I6 · World: missile silos on every planet

**Entry gate:** I5 done.

**Scope:**
- **Abandoned Missile Silos** (D17): huge, mostly underground, with the O, D and Closed surface variants. Rare, config-gated, on the M8 placement system, and never cascading.
- **Activity levels** (D36): abandoned, partly abandoned and operational, each with its own damage pass built into the layout.
- **A silo guard spawner block** with shooting GregGuns NPCs (Army Soldier, Commando, Stormtrooper), so operational silos are dungeons to clear and take over.
- **Loot ceilings and planet silos** (D38):
  - the Overworld caps at EV;
  - silos generate on every Galacticraft, GalaxySpace and Amun-Ra planet with solid ground, with the ceiling rising one tier per rocket tier (Moon EV, up to T9 at UMV);
  - Ross128b and Ross128ba silos roll any tier from EV to UMV, shown by their casings;
  - salvage and guards scale with the planet;
  - every dimension has its own flag.
- **Visuals:** screenshots of each activity level and of silos on several planets, the `icbm-silos` sheet, and a silo quest.

**Exit gate:**
- [ ] Silos generate without cascading on the Overworld and on at least one planet of every rocket tier, and match the D17 layout. Scenario per dimension.
- [ ] Each activity level generates with its damage, spawner count and loot. Guards spawn and attack, and a broken spawner stays broken. Scenario.
- [ ] No loot exceeds the dimension's ceiling (or, on Ross128, the silo's rolled ceiling), checked over many generated chests. Unit test plus scenario.
- [ ] `icbm-silos` and the silo quest have landed, and the owner has been told.
- [ ] The milestone review fix list is closed.

## Alpha gate · 0.4.0-alpha

The one owner review before the beta.

**Entry gate:** I0–I6 done.

**Exit gate:**
- [ ] A completeness audit of I0–I6: every exit box is met or deferred with a recorded reason.
- [ ] A batched client pass with no crash or invisible-render bugs.
- [ ] The visuals gallery holds every ICBM visual so far, and the review guide links it.
- [ ] Tag `0.4.0-alpha-pre` as a pre-release, with `modules.icbm` still off by default. The notes give the owner a review guide: what to enable, a test world, and what to look at.
- [ ] The owner has reviewed it once, and their notes are written up as the I7 fix list.

## I7 · Hardening

**Entry gate:** the alpha gate has passed.

**Scope:**
- The owner's alpha fix list.
- A balance pass on cost and damage against the targets above, in the real pack.
- **Soak tests on a real GTNH server:**
  - many missiles in flight;
  - simultaneous large blasts;
  - red matter;
  - EMP in a dense base;
  - chunk tickets after hours of play.
- A multiplayer check, and a shader check with the owner's shader pack.
- NEI, lang keys and config documentation. The ICBM quests in the GregGuns tab are checked end to end on a real server install.
- A recipe-clash review of the ICBM recipes against the full GTNH pack, and the PACK-CHANGES pass.
- **Visuals:** `icbm-recipes` from the recipe export, `icbm-hero`, an ICBM section in the README, and every ICBM sheet rebuilt after the balance pass.

**Exit gate:**
- [ ] The fix list is closed.
- [ ] The soak runs show no tick spikes past budget and no leaked chunk tickets.
- [ ] The recipe-clash review finds no unresolved clash with ICBM content.
- [ ] Every ICBM guide sheet matches the code after the balance pass.
- [ ] `modules.icbm` defaults to on.
- [ ] The milestone review fix list is closed.

## Beta gate · 0.4.0-beta

**Entry gate:** I7 done.

**Exit gate:**
- [ ] Tag `0.4.0-beta` as a pre-release, with release notes for server owners.
- [ ] The owner's beta playthrough is done, and its blocking bugs are fixed and released as `0.4.x-beta`.

After this gate the [handoff plan](HANDOFF-PLAN.md) can start, once the main roadmap's M9 is also done.

## Deferred

Not planned for this roadmap. Each one can be picked up later or left for an adopter:
- cargo parachute, balloon, cargo hook and supply drops (D10);
- airburst and penetrator fuses (D26).
