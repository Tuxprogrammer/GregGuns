# Design records

Decisions that shape GregGuns, especially the [roadmap's decision gates](../ROADMAP.md#decision-gates) D1–D6. A gate is closed when its record reaches `Status: Accepted`. During the MVP push, records are **accepted by default** and revisited after MVP; the owner edits them later rather than approving each one in advance. Milestones it blocks can't start until then.

Name each record `NNNN-short-title.md` (for example `0001-tier-map.md`). Once a record is accepted, don't rewrite it. To change course, add a new record that supersedes the old one.

## Template

```markdown
# NNNN · Title

- Status: Proposed | Accepted | Superseded by NNNN
- Gate: D1 … D6 (or none)
- Date: YYYY-MM-DD

## Context
What problem this solves, with links to the spec sections and BUGS.md rows involved.

## Decision
What we will do, stated precisely enough to implement and test.

## Consequences
What this changes in the roadmap, the code and the pack. Spec deviations it introduces.

## Alternatives considered
The other options, and why each was not chosen.
```

## Records

| # | Title | Gate | Status |
|---|---|---|---|
| [0001](0001-tier-map.md) | Tier map | D1 | Accepted (by default) |
| [0002](0002-machine-deconfliction.md) | Machine deconfliction | D2 | Accepted (by default) |
| [0003](0003-energy-model.md) | Energy model | D3 | Accepted (by default) |
| [0004](0004-damage-model.md) | Damage model | D4 | Accepted (by default) |
| [0005](0005-world-presence.md) | World presence | D5 | Accepted (by default) |
| [0006](0006-art-direction.md) | Art direction | D6 | Accepted (by default) |
| [0007](0007-pack-loot-and-infernal-mobs.md) | Pack loot and Infernal Mobs | — | Accepted (by default) |
| [0008](0008-exact-tier-charging.md) | Exact-tier charging (amends 0003) | D3 | Accepted (by default) |
| [0009](0009-gtnh-integration-defaults.md) | GTNH integration defaults (amends 0001, 0004, 0005) | — | Accepted (by default) |
| [0010](0010-material-remap-and-tier-additions.md) | Material remap and tier additions (extends 0001) | D1 | Accepted (by default) |
| [0011](0011-machine-policy.md) | Machine policy: single blocks, multiblocks, Ammo Press (amends 0002) | D2 | Accepted (by default) |
| [0012](0012-retire-reaction-chamber-and-retort.md) | Retire the Reaction Chamber and the Retort (amends 0002, 0011) | D2 | Accepted |
| [0013](0013-default-key-map.md) | Default key map in the GTNH pack | — | Accepted (by default) |
| [0014](0014-end-goal-beta-handoff-archive.md) | End goal: a working beta, a handoff, then archive (replaces M9) | — | Accepted; amended by 0015 |
| [0015](0015-m9-self-contained-polish.md) | M9: polish as a self-contained jar, then hand off (amends 0014) | — | Accepted |
| [0016](0016-damage-balance-pass.md) | Damage balance pass: GregGuns owns its damage numbers (amends 0004) | D4 | Accepted |
| [0017](0017-accept-sounds-archive-sound-pipeline.md) | Accept the sounds as they are; archive the sound pipeline (amends 0006) | D6 | Accepted |
| [0018](0018-icbm-program.md) | The ICBM program: scope, balance and how it is built (amends 0014, 0015) | — | Accepted |
| [0019](0019-structures-on-ross128.md) | GregGuns structures on Ross128 (amends 0005) | D5 | Accepted |
| [0020](0020-omt-overlap.md) | OpenModularTurrets overlap: the Rampart stays, nothing is merged (M9 polish) | — | Accepted |
| [0021](0021-tier-balance-pass.md) | Tier balance pass: recipe cost, armor, NPCs, energy and ammo along the GT tiers (M9 polish) | D1, D3 | Accepted |
| [0022](0022-steam-armor-steam-age.md) | Steam armor moves to the Steam age: bronze, hand-crafted, coal-fired; Steam Armor Plating retired (amends 0001, 0021) | D1 | Accepted |
| [0023](0023-connected-casing-textures.md) | Connected textures for the base casings: MCPatcher `method=ctm` through the pack's Angelica (M9 polish) | D6 | Accepted |
| [0024](0024-vanilla-spawners.md) | Structure spawners are vanilla mob spawners with GregGuns NBT; the four GregGuns spawner blocks are retired (amends 0005) | D5 | Accepted |
