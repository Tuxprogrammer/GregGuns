# 0015 · M9: polish as a self-contained jar, then hand off

- Status: Accepted (owner, 2026-10-21)
- Gate: none (amends [0014](0014-end-goal-beta-handoff-archive.md))
- Date: 2026-10-21

## Context
On 2026-10-21 the owner signed off M0–M8 as the MVP and asked for `0.3.0-beta` to be tagged and shared with friends. [0014](0014-end-goal-beta-handoff-archive.md) had made M9 only a handoff and an archive, and moved polish and pack nice-to-haves to an adopter list. The owner now wants M9 to do that polish first, while GregGuns stays out of the GTNH repositories.

## Decision
1. **M9 is polish and pack fit, delivered inside our own jar.** GregGuns stays **fully self-contained**: one portable jar that a server owner drops into a GTNH 2.9 pack. Nothing in M9 depends on a PR to, or a change in, the GTNH modpack or any other mod's repository.
2. **The questbook ships in the jar.** The GregGuns quest line ([12-questbook](../gregguns-spec/12-questbook/index.md)) is installed from resources in our jar. A **hidden config key** (not shown in the in-game config GUI, off by default) installs it into the server's BetterQuesting database. It is not delivered as a `DefaultQuests` change to the pack.
3. **PACK-CHANGES rows become in-jar work where possible.** Each row of [PACK-CHANGES.md](../gregguns-spec/PACK-CHANGES.md) is either done from our jar (a config default, a runtime hook, a bundled resource) or recorded as an optional server-owner step. None is a PR to GTNH.
4. **Back in scope from the 0014 adopter list:**
   - the balance pass;
   - NEI polish and config defaults;
   - translation readiness;
   - multiplayer and performance soak tests;
   - quests (as above);
   - GuideNH pages bundled in the jar;
   - the OpenModularTurrets investigation;
   - aim down sights.
5. **The 0014 handoff stays as the last step of M9:** `docs/HANDOFF.md`, the solicitation the owner posts, the README banner, then the archive on the owner's go-ahead. Still no submission to GTNH, and still open about how the mod was made.

## Consequences
- The roadmap's M9 becomes "Self-contained polish, then handoff". The adopter list shrinks to what M9 doesn't finish, plus the GTNH PR itself, which stays an adopter's choice.
- The questbook design changes from "hand JSON to the pack maintainers" to "install from the jar on request". Quest IDs stay stable UUIDs so an install is repeatable.
- The beta is shared directly: the GitHub pre-release jar is the distribution.

## Alternatives considered
- **Hand off right after the beta** (0014 as written). Rejected by the owner: the mod should be polished and pleasant inside the pack first.
- **Ship quests as a pack `DefaultQuests` change.** Rejected: it needs the pack's repository, which is against the self-contained rule.
