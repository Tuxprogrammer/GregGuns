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
