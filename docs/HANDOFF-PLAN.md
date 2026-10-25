# Handoff plan

The last step for GregGuns: hand the mod to a developer who might adopt it, then archive the repository ([0014](design/0014-end-goal-beta-handoff-archive.md), [0018](design/0018-icbm-program.md)). This used to be part of M9 on the [roadmap](ROADMAP.md). It moved here so the roadmaps hold only build work.

**Entry gate:** both of these, and every other open item, are closed (owner, 2026-10-25):
- [ROADMAP.md](ROADMAP.md) M9 (self-contained polish) is done;
- [ROADMAP-ICBM.md](ROADMAP-ICBM.md) has passed its beta gate.

## Scope

- **Handoff guide** (`docs/HANDOFF.md`) for a developer who might adopt the mod. A draft from 2026-10-20 is on the `docs/handoff-draft` branch; it predates 0.3.1 and ICBM and must be refreshed. It covers:
  - the state of every area, ICBM included;
  - how to build and test, including the real-pack test with `-Dgregguns.testHooks=true`;
  - how the mod was made, honestly: AI-built under one human owner;
  - an open-ended path to inclusion. The policy is vague and its application is up to the pack's reviewers, so suggest starting with light edits and working out the requirements with the dev team.
- **Solicitation message.** We draft it and the owner posts it. It is open about how the mod was made.
- **README banner,** then **archive** the repository on the owner's go-ahead.

## Adopter list

What we leave for whoever adopts the mod:
- the GTNH modpack PR itself, with the GT5-Unofficial MTE ID and texture-page reservations;
- anything from the M9 polish list or the ICBM roadmap that isn't finished, including ICBM's [deferred items](ROADMAP-ICBM.md#deferred);
- commissioned art and sound to replace the generated placeholder assets ([0017](design/0017-accept-sounds-archive-sound-pipeline.md)).

## Exit gate

- [ ] `docs/HANDOFF.md` is written and the owner has reviewed it.
- [ ] The solicitation message is drafted and the owner has approved it. The owner posts it.
- [ ] The README carries the archive banner, and the GitHub repository is archived, on the owner's go-ahead.
