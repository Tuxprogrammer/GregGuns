# 0018 · The ICBM program: scope, balance and how it is built

- Status: Accepted (owner, 2026-10-25)
- Gate: none (amends [0014](0014-end-goal-beta-handoff-archive.md) and [0015](0015-m9-self-contained-polish.md); supersedes ICBM decision D6 and amends D2, D8, D12, D17 and D28 in [DECISIONS.md](../gregguns-icbm-spec/DECISIONS.md))
- Date: 2026-10-25

## Context
The ICBM spec pair ([icbm-spec](../icbm-spec/README.md), [gregguns-icbm-spec](../gregguns-icbm-spec/README.md)) was written on 2026-10-20 with decisions D1–D28. D6 placed the build "after 1.0, as M10". The plan changed after that: the MVP was signed off, and the end goal became polish, then handoff and archive (0014, 0015). The roadmap had no place for ICBM. On 2026-10-25 the owner made ICBM the next body of work, to be finished before the handoff, and settled how it is scoped, balanced and built.

Two facts from the pack shaped the decisions:
- **Galacticraft removes the launch pad on every rocket launch.** `EntityAutoRocket.onLaunch` deletes the 3×3 pad. A player rocket gets the 9 pad blocks back on landing; a cargo rocket's pad is lost unless a powered Launch Controller has pad removal turned off. The removal is in the rocket base class and posts a cancelable `EventLandingPadRemoval`. Source: [`EntityAutoRocket.java`](https://github.com/GTNewHorizons/Galacticraft/blob/fbdc41b97e8178ac07e58ccaa93da2474b1e2664/src/main/java/micdoodle8/mods/galacticraft/api/prefab/entity/EntityAutoRocket.java#L686-L745) at `3.4.34-GTNH`.
- **Infernal Mobs health pools are large.** Max health is base × modifier count × `mobHealthFactor`, which is 1.8 in the pack. An Infernal mob has 8–15 modifiers, so an Infernal blaze or enderman (base 40) reaches about 1,080 and a modded mini-boss (base 100) about 2,700. Nothing caps incoming damage per hit; `maxOneShotDamage` only limits the Berserk modifier's outgoing damage. Source: [`MobModifier.getActualMaxHealth`](https://github.com/GTNewHorizons/Infernal-Mobs/blob/7427bdd1490dbdc54ef5983de3c671ae055cf38c/src/main/java/atomicstryker/infernalmobs/common/modifiers/MobModifier.java#L306-L314) at `1.10.7-GTNH`.

## Decision

### Placement and packaging
1. **ICBM is a module in the GregGuns jar**, `modules.icbm`, as 0015 requires. It turns itself off without Galacticraft (D9). It shares the GregGuns blast pipeline, damage code and test harness.
2. **It has its own roadmap**, [ROADMAP-ICBM.md](../ROADMAP-ICBM.md), with milestones I0–I7, an alpha gate and a beta gate. [ROADMAP.md](../ROADMAP.md) keeps the GregGuns final polish (M9).
3. **The handoff moves off the roadmap** to [HANDOFF-PLAN.md](../HANDOFF-PLAN.md). It starts only once the M9 polish is done and the ICBM beta gate has passed. This supersedes the handoff part of 0015's M9.
4. `modules.icbm` **defaults to off until the ICBM beta gate**, so `0.3.x-beta` fixes can keep shipping from master while ICBM is built.

### Balance
5. **Cost: an automatable arsenal.** At its own tier, a missile costs about what one of that tier's Galacticraft rocket launches costs in plates, circuits and fuel. The Assembly Line can automate it, so a player at the tier can stock dozens. The warhead is the expensive part, and cost rises steeply by tier. Missiles are never as cheap as in ICBM, and never so rare that a player launches only one or two.
6. **Damage: missiles matter against GTNH mobs.** Center damage targets for conventional warheads, before armor, falling off to zero at the blast edge:

   | Missile tier | Center damage, at least | Benchmark |
   |---|---|---|
   | T1 (HV) | 1,200 | An Infernal blaze or enderman (about 1,080) dies at ground zero |
   | T2 (EV–IV) | 3,000 | An Infernal modded mini-boss (about 2,700) dies at ground zero |
   | T3 conventional (LuV) | 8,000 | Clear margin over every Infernal pool in the pack config |

   Special blast types (gas, incendiary, EMP, smoke and the like) are balanced on their own effect, not on this table. Damage to players still passes the server's `damagefactors` and PvP settings.
7. **Nuclear and every warhead above it annihilates.** Inside the kill zone of a nuclear (ZPM), antimatter (UV) or red matter (UHV) warhead, **every living entity dies, bosses included**. The kill is absolute: it bypasses armor and hurt-resistance frames, as the Nuke Launcher's does ([0016](0016-damage-balance-pass.md)). Players are subject only to the server's damage settings. The Nuke Launcher keeps its own rule (bosses lose half their health). Both use one implementation, with a flag for bosses.

### The missile in the world
8. **Missiles are stacked on the pad.** The NASA Workbench, or the Assembly Line, makes the missile body. The player places the body on the Galacticraft pad (T1–T2) or in the silo (T3 and up), then places a warhead block on its nose. Arming fuses the stack into a live missile. In the silo, the body and warhead can also arrive through GT input buses and mate automatically. A warhead block placed on its own is a demolition charge, as ICBM's explosive blocks are. This amends D8.
9. **The Galacticraft pad is never removed or damaged** by a GregGuns missile. The missile implements Galacticraft's docking and fuel interfaces without extending `EntityAutoRocket`, and GregGuns also denies `EventLandingPadRemoval` for a pad that holds one of its missiles. A scenario proves the pad survives a launch.

### Assets
10. **Human-made, properly licensed assets first; generation is a last resort.** D28's source order stands, except that generating our own assets drops from "fill the gaps" to "only if nothing licensed exists". There are no asset-tuning loops (see [0017](0017-accept-sounds-archive-sound-pipeline.md)). This amends D28.
11. **Third-party code (D37):** MIT preferred; CC0, CC-BY, MIT and LGPL acceptable (owner, 2026-10-25). LGPL logic is preferably described in the spec and rebuilt clean-room, which makes it ours and MIT. LGPL code adapted from its source stays LGPL and lives in the third-party folder with its license.
    **Third-party assets are an exception to "MIT for everything".** Assets under CC0, CC-BY, MIT or LGPL may be used, with no NC, ND, share-alike-only or All Rights Reserved terms. Each keeps its own license, lives in a third-party folder with its license text, and is credited in `docs/assets/CREDITS.md`. [CONTRIBUTING](../../CONTRIBUTING.md#assets-and-localization) records this exception. Each license is checked against its source before the asset is added, because the licensing research lives outside the repository.

### How it is built
12. **Autonomous build, with owner input only at the gates.** The owner's decision time is this planning pass. From I0 on, the orchestrator decides open questions itself and records each decision in DECISIONS.md, marked as an orchestrator decision. The owner reviews once at the alpha gate, then plays and gives feedback from the beta gate, as for GregGuns. The aim is maximum working functionality before a human plays it.
13. **Agents:**
    - one or two **Sonnet** coding agents at a time, in their own worktrees;
    - one **Opus** reviewer agent, run **once per milestone**, which checks the milestone's changes against the plan and returns a single fix list;
    - no per-feature review and no Opus coding agents.
14. **Per-branch gate, automated only:** spotless, build and unit tests, the full headless scenario suite in a fresh worktree, and the ported-code gate. The orchestrator merges on green. Client checks run once per milestone, batched.
15. **Ported code passes a style gate before it merges.** It is adapted, not copied: GregGuns structure and naming, Javadoc and comments, unit tests, and every known `ICBM-xx-nn` bug in it fixed (D2). An automated test enforces the checkable part (see [CONTRIBUTING § Ported code](../../CONTRIBUTING.md#ported-code-mit)). The milestone reviewer checks the rest.

### Worldgen
16. **Abandoned Missile Silos have activity levels (D36):** abandoned (fairly damaged, no spawners), partly abandoned (some spawners) and operational (intact, lots of spawners). Guards are shooting GregGuns NPCs from spawner blocks on the M8 spawner system, so the player clears the silo like a dungeon and breaks the spawners to take it over. Loot grows with activity.
17. **Silo loot ceilings and planet silos (D38).** Overworld silos cap at EV, in line with other Overworld rewards (AE2 meteors). Silos also generate on Galacticraft, GalaxySpace and Amun-Ra planets, with the ceiling rising one voltage tier per GTNH rocket tier: the Moon is EV, the Mars group IV, T3 planets LuV, up to T9 at UMV. Ross128b and Ross128ba, an alternate challenge-start route, roll any tier from EV to UMV per silo. This supersedes D17's HV cap and its deferral of planet silos.

## Consequences
- ROADMAP.md's M9 becomes "Self-contained polish"; its handoff boxes and the adopter list move to HANDOFF-PLAN.md.
- DECISIONS.md D6 is superseded, and D2, D8, D12, D17 and D28 carry amendment notes. New decisions D29–D38 record the items above.
- CONTRIBUTING gains a "Ported code (MIT)" section and a third-party asset exception.
- The handoff waits for ICBM, which is several milestones of work.

## Alternatives considered
- **A separate addon jar.** Rejected: it would need a public API between two jars and double the build and release work, against 0015's one-jar rule.
- **Keep ICBM after 1.0 or on the adopter list.** Rejected by the owner: ICBM is the next work.
- **Strategic pricing (a few missiles per session).** Rejected: the owner wants missiles used, not hoarded.
- **The Nuke Launcher's boss rule for missiles.** Rejected: at missile scale the owner wants no living entity left in the kill zone.
- **Building missiles entirely as items, or entirely as placed blocks.** Rejected: stacking on the pad gives the hands-on build without losing GT gating and Assembly Line automation.
