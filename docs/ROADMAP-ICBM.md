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
  - Small, scoped briefs. An agent that passes about 250k tokens of context writes a handoff file and a fresh agent takes over.
  - **One Opus reviewer agent per milestone,** run at the milestone's end. It reads the milestone's diff against this roadmap, the spec sheets and 0018, and returns one fix list. No per-feature reviews and no Opus coding agents.
- **Branch gate, automated only:**
  - `spotlessCheck`;
  - the build and unit tests;
  - the full headless scenario suite with every module enabled, in a fresh worktree;
  - the ported-code gate (I0).

  The orchestrator lands only when master is an ancestor of the verified branch, then merges with `--no-ff`.
- **Client checks.** One batched in-game session per milestone, on the test server. Shader runs only for shader questions.
- **Ported MIT code** follows [CONTRIBUTING § Ported code](../CONTRIBUTING.md#ported-code-mit): adapted, commented, tested and bug-fixed before it merges.
- **Assets.** Human-made, properly licensed assets first ([0018](design/0018-icbm-program.md) §10–11, D28). Generate an asset only when nothing licensed exists, and never run tuning loops. Each milestone sources the assets its content needs.
- **Releases.** Tags follow the main repo's release steps. Nothing is pushed or released without the owner's go-ahead, apart from what this roadmap names.

## Balance targets

These come from [0018](design/0018-icbm-program.md) §5–7, and every milestone checks against them.

- **Cost:** an automatable arsenal. A missile costs about one of its tier's Galacticraft rocket launches in plates, circuits and fuel, and is Assembly Line-automatable. The warhead is the expensive part, and cost rises steeply by tier.
- **Conventional center damage, at least:** T1 (HV) 1,200; T2 (EV–IV) 3,000; T3 (LuV) 8,000. Damage falls off to zero at the blast edge.
- **Nuclear (ZPM), antimatter (UV) and red matter (UHV):** every living entity in the kill zone dies, bosses included. Players are subject only to server damage settings.
- **Tiers** (D5): T1 HV; T2 EV–IV; T3 LuV–ZPM, with nuclear at ZPM; antimatter at UV; red matter at UHV.

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
- The GregGuns-side area index stubs (02–07) in [gregguns-icbm-spec](gregguns-icbm-spec/README.md), each listing which decisions it will carry.
- A CLAUDE.md map row for the ICBM roadmap and spec pair.

**Exit gate:**
- [ ] `runServer` boots with `modules.icbm=true` with Galacticraft present, and with it absent (the module turns itself off).
- [ ] `PortedCodeGateTest` fails on a planted bad sample and passes on a good one.
- [ ] The notices file, the third-party asset folder and the CREDITS section exist.
- [ ] The milestone review fix list is closed.

## I1 · Warheads and the blast engine

**Entry gate:** I0 done.

**Scope:**
- **One shared explosive registry** (D12). ICBM blast types become warheads in the GregGuns blast pipeline (`ExplosionRules`, `BlockBreakGuard`, `ExplosionEvent.Detonate`), never a second block-breaking path.
- **The blast engine:**
  - ray-cast blasts and threaded scheduling (`MIT-portable`);
  - one tick budget shared by every running blast;
  - correct save and load of blasts in progress.
- **Warhead items** with NBT `{explosive, fill}`, capped by casing tier (D23).
- **Warhead blocks.** A warhead can be placed in the world, and alone it is a demolition charge with a fuse (0018 §8).
- **The T1–T2 blast types kept by D10,** with their effects (gas, incendiary, smoke and so on) and the conventional damage targets.
- The annihilation rule's shared implementation, with a bosses flag (0018 §7). It is unit tested here; nuclear content arrives in I3.
- Warhead recipes through GT machines (Chemical Reactor, Implosion Compressor), tier-gated and visible in NEI.

**Exit gate:**
- [ ] Every I1 blast type detonates through the shared pipeline and respects protection and `explosions.blockDamage`. Scenario per type.
- [ ] Conventional warheads meet the T1 and T2 center-damage targets against Infernal-scale health pools. Unit test plus scenario.
- [ ] Several large blasts at once stay within the tick budget, and a blast survives a save and reload. Scenario.
- [ ] Warhead blocks fuse and detonate by redstone and by hand.
- [ ] The relevant `ICBM-02-*` bugs are avoided, with tests.
- [ ] The milestone review fix list is closed.

## I2 · Missiles and the Galacticraft pad (T1–T2)

**Entry gate:** I1 done.

**Scope:**
- **Getting a missile body:**
  - a missile chip in the Circuit Assembler, then a schematic in the Laser Engraver (D8);
  - our NASA Workbench page for building by hand;
  - Assembly Line recipes, behind a research data stick, for automation (D27).
- **The stack-on-pad build** (0018 §8): place the body on the Galacticraft pad and the warhead on its nose; arming fuses the stack into the missile entity.
- **Fuel** keyed to tier through Galacticraft's `RocketFuels` lists and fuel loader (D19). A fixed amount per tier, and unburned fuel adds to the blast (D20).
- **A launch-control block beside the pad.** Targeting by coordinates, data stick, ModularUI2 GUI, redstone or the GT machine-controller cover (D13).
- **Flight:**
  - a real entity for the whole flight, with a ballistic arc (`MIT-portable`);
  - a two-chunk sliding chunk window (D25);
  - a cap on missiles in flight (D11);
  - tiered guidance scatter (D24);
  - impact fusing only (D26).
- **The pad is never removed or damaged** (0018 §9).

**Exit gate:**
- [ ] A T1 and a T2 missile are built by hand and by Assembly Line, stacked, fueled, launched and detonated on target. Scenario.
- [ ] The Galacticraft pad, fuel loader and anything attached are intact after a launch. Scenario `icbm.pad.survives`.
- [ ] A missile never holds more than two chunk tickets, and every ticket is released on impact, on timeout and on unload. Scenario.
- [ ] Recipes and costs match the cost target, and everything is visible in NEI.
- [ ] The relevant `ICBM-03-*` and `ICBM-04-*` bugs are avoided, with tests.
- [ ] Batched client session: build, launch and impact look right.
- [ ] The milestone review fix list is closed.

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

**Exit gate:**
- [ ] The silo forms, loads, fuels and launches a T3 missile without the player touching the missile. Scenario.
- [ ] Nothing living survives inside the nuclear kill zone, a boss included. Players follow server damage settings. Scenario.
- [ ] Fallout stays within its caps: a scenario counts converted blocks and server tick time for a nuclear strike.
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

**Exit gate:**
- [ ] A radar flags an incoming missile and a linked battery intercepts it. Scenario.
- [ ] An EMP drains EU and causes maintenance issues within its device cap, with no tick spike in a dense base. Scenario.
- [ ] The OpenComputers components work as specified (GGI-01-01 to 06). Unit tests plus the batched client session.
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

**Exit gate:**
- [ ] Antimatter and red matter detonate, annihilate and finish within the tick budget, and red matter survives a save and reload. Scenario.
- [ ] Fallout blocks process into their yields in GT machines. Scenario.
- [ ] The Golden Dome programs run (GGI-01-07 to 09) in the batched client session.
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

**Exit gate:**
- [ ] Silos generate without cascading on the Overworld and on at least one planet of every rocket tier, and match the D17 layout. Scenario per dimension.
- [ ] Each activity level generates with its damage, spawner count and loot. Guards spawn and attack, and a broken spawner stays broken. Scenario.
- [ ] No loot exceeds the dimension's ceiling (or, on Ross128, the silo's rolled ceiling), checked over many generated chests. Unit test plus scenario.
- [ ] The milestone review fix list is closed.

## Alpha gate · 0.4.0-alpha

The one owner review before the beta.

**Entry gate:** I0–I6 done.

**Exit gate:**
- [ ] A completeness audit of I0–I6: every exit box is met or deferred with a recorded reason.
- [ ] A batched client pass with no crash or invisible-render bugs.
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
- NEI, lang keys, config documentation, and an ICBM tab in the GregGuns questbook.

**Exit gate:**
- [ ] The fix list is closed.
- [ ] The soak runs show no tick spikes past budget and no leaked chunk tickets.
- [ ] `modules.icbm` defaults to on.
- [ ] The milestone review fix list is closed.

## Beta gate · 0.4.0-beta

**Entry gate:** I7 done.

**Exit gate:**
- [ ] Tag `0.4.0-beta` as a pre-release.
- [ ] The owner's beta playthrough is done, and its blocking bugs are fixed and released as `0.4.x-beta`.

After this gate the [handoff plan](HANDOFF-PLAN.md) can start, once the main roadmap's M9 is also done.

## Deferred

Not planned for this roadmap. Each one can be picked up later or left for an adopter:
- cargo parachute, balloon, cargo hook and supply drops (D10);
- the glass button and pressure plate (D10);
- airburst and penetrator fuses (D26).
