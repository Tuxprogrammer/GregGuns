# GTNH inclusion assessment and AI policy critique

This assessment, written on 2026-10-20, asks whether GregGuns could be added to the GT New Horizons modpack. It compares the repo as it stood at commit `70ada372` against the GTNH development wiki, GTNH-Dev-Doc (including its AI policy), the organization contribution guidelines and the modpack vision document. It ends with a critique of the AI policy and proposes rules and procedures to replace it.

## Summary

**GregGuns fits GTNH well technically. As built, it cannot be submitted.** The GTNH AI policy bans three things at the core of how GregGuns was made. The team has also not agreed to the content, and most of it sits in tiers the team wants to keep sparse.

- **Fit:**
  - It is built on the GTNH buildscript, Spotless and the shared CI workflows. It compiles to Java 8 bytecode with no mixins and is MIT licensed.
  - It is GT-native throughout: GT materials, recipe maps, EU and tier gating.
  - It has 0 recipe clashes with GTNH 2.9, and no similar mod is in the pack.
- **Blockers:**
  - Its textures and sounds are AI-generated.
  - 831 of 1,113 commits carry a Claude co-author line.
  - About 130,000 lines of Java were written in 3 days by one maintainer.
  - There has been no discussion with the team, and most content sits below IV.
- **Policy critique:** the AI policy mixes a few bright-line bans with several clauses that depend on a reviewer's impression. It does not define its key terms and cannot be enforced by inspecting a contribution, so in practice it penalizes disclosure rather than AI use. It also has no enforcement procedure or appeal.

## Sources

| Document | What it governs |
| --- | --- |
| [GTNH wiki: Development](https://wiki.gtnewhorizons.com/wiki/Development) | Where to discuss ideas, target branch, testing in dev and full pack |
| [GTNH wiki: Github policies](https://wiki.gtnewhorizons.com/wiki/Github_policies) | Review before merge, stale issues |
| [GTNH-Dev-Doc: AI_POLICY.md](https://github.com/GTNewHorizons/GTNH-Dev-Doc/blob/master/AI_POLICY.md) | AI use in code, text, media, commits, PRs |
| [Organization CONTRIBUTING.md](https://github.com/GTNewHorizons/.github/blob/master/CONTRIBUTING.md) | Expectations, PR requirements, balance PRs, content standards |
| [GTNH-Dev-Doc](https://github.com/GTNewHorizons/GTNH-Dev-Doc) contributing, start-here, testing and build-tools pages | Discussion before major content, PR expectations, code style, testing |
| GTNH-Dev-Doc `vision of the modpack/original vision doc.md` | Pack identity, tier gating, new-mod policy |
| [GT-New-Horizons-Modpack issue templates](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/tree/master/.github/ISSUE_TEMPLATE) | The "suggest a new mod" checklist (open-source license required) |
| [DreamAssemblerXXL](https://github.com/GTNewHorizons/DreamAssemblerXXL) | Pack assembly; admin approval for mod additions and removals |
| [ExampleMod1.7.10](https://github.com/GTNewHorizons/ExampleMod1.7.10) | Starter and buildscript for new standalone mods |
| [GitHub Docs: Creating a pull request](https://docs.github.com/en/pull-requests/how-tos/create-pull-requests/creating-a-pull-request) | PR mechanics, draft PRs |
| [GT-New-Horizons-Modpack #23217](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/issues/23217) | Flan's Mod request, closed as not a thematic fit |

The facts about GregGuns come from this repo: [ROADMAP](../ROADMAP.md), [CONTRIBUTING](../../CONTRIBUTING.md), [PACK-CHANGES](../gregguns-spec/PACK-CHANGES.md), [CREDITS](../assets/CREDITS.md), [recipe clash review](../testing/recipe-clash-beta.md), [NewGunrizons recon](newgunrizons-recon.md) and git history.

## Where GregGuns fits

| GTNH expectation | Source | GregGuns |
| --- | --- | --- |
| New standalone mods start from ExampleMod1.7.10 and do not edit `build.gradle` | ExampleMod README | GTNH buildscript, `gtnhShared/` and Blowdryer; `build.gradle.kts` is never edited |
| Use the repo's formatter (Spotless) | Dev-Doc code style | Spotless and Checkstyle are enforced in CI |
| CI through the shared GTNH workflows | ExampleMod | `build-and-test.yml` calls `GTNH-Actions-Workflows` |
| Mixins are an advanced feature most mods will not need | ExampleMod | `usesMixins = false`, no coremod, no access transformers |
| Optional integrations isolated in compat classes behind `Loader.isModLoaded` | Dev-Doc development practices | Followed, for example for ServerUtilities |
| Test on a dedicated server, across save and reload, and with optional mods present and absent | Dev-Doc testing expectations | 353 unit-test files, dedicated-server smoke test, two-client multiplayer check, 100-NPC soak (p95 4.45 ms), worldgen no-cascade test |
| New mods must be open source | Vision doc; new-mod issue template | MIT for code and all assets |
| "Balanced around the mod GregTech", with content allocated "to a fitting point within the progression" | Vision doc | GT materials, recipe maps and EU; no new ores or metals; tier gates from Steam to ZPM |
| "Tier materials are used to gate content" | Vision doc | Each gun and armor tier is gated by a GT tier material |
| "Nothing similar in the pack already" | New-mod issue template | Techguns is not in the pack, and NewGunrizons has been inactive since March 2026 |
| Recipes must be "gregified" | New-mod issue template | Machines use GT recipe maps; 0 exact and 0 superset recipe clashes with GTNH 2.9 |

The vision document's section "What is the place of combat in the pack?" is an empty heading. One recorded suggestion asks for "tougher mobs on every tier, to give a use to your ever more powerful offensive capabilities." GregGuns adds tiered weapons, tiered mobs and NPC factions. It is the most concrete answer to that open question that anyone has built for the pack.

Delivery to the pack is also planned in its native formats. There are 121 quests in BetterQuesting format, NEI recipe maps and structure previews, and module toggles that need a restart. A [pack change list](../gregguns-spec/PACK-CHANGES.md) records every config edit the pack would need.

## Where GregGuns misses

### AI policy

[AI_POLICY.md](https://github.com/GTNewHorizons/GTNH-Dev-Doc/blob/master/AI_POLICY.md) applies to code, written content, issues, discussions, pull requests and media. Every GTNH PR template asks the author to confirm compliance.

| Policy clause | GregGuns today |
| --- | --- |
| "No AI-generated media is allowed (textures, videos, audio, etc.)" | All 524 textures and 279 sounds come from generator scripts written and tuned by an AI agent. AI listeners scored the sounds. [CREDITS](../assets/CREDITS.md) credits the logo and infographics to Claude. |
| "No AI-authored or AI co-authored commits, including Co-authored-by AI tags" | 831 of 1,113 commits carry a Claude co-author line. |
| "The human-in-the-loop must fully understand all code" | About 129,600 lines of Java written between 2026-10-17 and 2026-10-20 by one maintainer with agents. |
| "Commit messages, PR descriptions must read in your own voice" | Most commit messages and the drafted pack PR were written by agents. |
| "For documentation and especially quests, the tone and the point you're trying to make has to come from you" | The 115 quests were drafted by agents. |
| "Only contribute fixes for bugs you have personally hit in-game" | Many fixes came from the Techguns bug list and automated scenarios, not from play. The owner's beta playthrough has not happened yet. |

### Process

- **Discussion comes first.** "Major new content must be discussed with the team before implementation." No discussion has happened.
- **Large mods need a champion.** "Large mods must not only be 'thematically appropriate' but also have a champion willing to work with the team both on development and balance." The vision also expects a fork in the GTNewHorizons organization.
- **Admins approve every mod addition.** DreamAssemblerXXL's CODEOWNERS requires "explicit admin approval" for any mod addition or removal.
- **Recipe chains need flowcharts, and balance needs the review team.** Recipe-chain PRs without a flowchart "may be rejected regardless of quality." Balance PRs need answers to the four balance questions and approval from the Balance Review team.

### Progression density

The organization guidelines say to "avoid adding new content below IV tier. The game is already dense." Most of GregGuns sits below IV:
- the Steam-to-EV Ammo Press line,
- the Hand Cannon through the EV guns,
- most of the 12 armor sets.

### Thematic precedent

The only recorded request for a gun mod was [Flan's Mod](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/issues/23217), closed as "does not fit in with NH well thematically." GregGuns is built on GT where Flan's is not, but the team has not yet accepted any gun mod.

### Maturity

- The project is 3 days old and has one contributor.
- Releases are alpha, and the README warns of world-breaking changes.
- The pack-integration and balance milestone, M9, has not started.
- The FX performance budget is unverified. The vision document warns against features that "encourage spamming laggy setup."
- The wiki says "the original author is expected to fix problems their contribution causes." GregGuns has no track record yet to show it will.

### Footprint

- The pack would need config edits to more than 9 other mods, plus new loot files and a Thaumcraft aspects file.
- GT5-Unofficial would need a PR reserving MTE IDs from 20100. That makes the inclusion a coordinated change across several repos.
- A planned ICBM module after 1.0 would widen the scope further.

## Case for inclusion

GTNH has deep tech progression but no combat progression to match it. A player at ZPM fights with weapons that barely scale, and the vision document leaves combat as an empty heading. GregGuns fills that gap in GT's own terms: no new metals, no RF, no parallel machine ecosystem, every item gated by tier, and recipes in GT recipe maps. It is open source and MIT licensed, built on the GTNH toolchain and heavily tested, including on a dedicated server and in multiplayer. Every module can be switched off, so the pack can take only the parts it wants. The only other candidate, NewGunrizons, is inactive and its license is unclear.

## Case against inclusion

The policy bans its media, its commit history and its drafted PR text outright. No dev or admin has agreed to champion it. The only recorded request for a gun mod was rejected on theme. Most of its content lands in tiers the team is trying to keep sparse. It is 3 days old, alpha, and has one maintainer. A 130,000-line mod that its maintainer cannot fully explain is exactly the review and maintenance risk the policy was written to prevent. Submitting it as it stands would most likely be closed, and could get the author blocked from contributing.

## Critique of the GTNH AI policy

The policy has a legitimate goal, but it is built badly as a policy. A governance rule should tell a contributor in advance whether their work qualifies, and tell a reviewer how to decide and what follows. This one does neither for its central terms. It combines a few bright-line bans with standards that depend on a reviewer's impression of the work. It adopts another project's text without adapting it, and it defines no procedure for enforcement.

### Problems

1. **The key term is undefined.** "AI-generated media" is never defined. The policy does not say whether these count:
   - a texture drawn by a Python script that an AI wrote;
   - a sound synthesized from parameters an AI chose;
   - an AI-upscaled human sketch;
   - a human repaint over an AI draft.

   The policy sets no threshold of human transformation and no test for meeting one. A contributor cannot tell beforehand whether their work qualifies.

2. **It cannot be enforced by inspection, so it penalizes disclosure.** No reviewer can tell from a PNG or an OGG how it was made. GregGuns' assets are MIT licensed. Anyone may fork them, repaint them, remaster the sounds and submit the result without saying where they came from. The policy has no threshold, so no one can say whether that derived work is "AI-generated." In practice the rule filters out contributors who disclose, not AI use. A rule that only honest people trip over trains people to conceal. That is the opposite of what a project wants from a provenance rule.

3. **It bans the disclosure mechanism it otherwise depends on.** The PR template asks "to what degree AI was used," yet the policy forbids "Co-authored-by AI tags," the standard, machine-readable way to record that exact fact in history. The project asks for disclosure in a form that is not durable and bans the form that is.

4. **It judges origin, not outcome.** AI-assisted code is allowed if a human understands it. AI-generated media is banned whatever its quality, review or reproducibility. GregGuns' asset generators are code, and code is the category the policy accepts. Their output is media, which the policy bans. The line is drawn by provenance, not by any property a player or reviewer would notice. Meanwhile the policy sets no quality bar for human-made assets beyond "ideally run past an admin."

5. **Its central standards are impressions, not criteria.** "Fully understand all code," "clearly AI (slop)," "read in your own voice" and "the tone… has to come from you" have no test, no evidence standard and no named decider. Two reviewers can apply them to the same PR and reach opposite answers, and neither would be wrong under the text.

6. **The penalty has no procedure.** "Bad AI use may be blocked." The policy does not say:
   - who decides,
   - on what evidence,
   - whether the contributor is notified and allowed to respond,
   - how long a block lasts,
   - how to appeal.

   A permanent sanction resting on an undefined standard, with no process, is the part of the policy most in need of rules.

7. **The bug-fix rule is overbroad.** "Only contribute fixes for bugs you have personally hit in-game" excludes:
   - crashes reported by other players with full logs,
   - failures caught by tests or static analysis,
   - server-only bugs that a developer may never meet in single player.

   The underlying concern, fixes for misunderstood problems, is better handled by requiring a reproduction.

8. **It was borrowed without being adapted.** The text credits Ghostty's AI policy. Ghostty is a terminal emulator with almost no art or audio. GTNH is a pack of about 240 mods, with large art, audio and quest workloads and a volunteer reviewer pool. The policy has no version, no review date and no record of why each clause exists.

### What the policy gets right

The criticism above is stronger if it is honest about the other side.

- **Reviewer time is the scarce resource.** AI makes large contributions cheap to produce and just as expensive as ever to review. A volunteer project is right to push back on that asymmetry.
- **Maintenance outlives the PR.** "The original author is expected to fix problems their contribution causes." A contributor who cannot explain their code cannot keep that promise.
- **GregGuns is the case the policy fears.** About 130,000 lines in 3 days, from one maintainer with no history in the project, is the scenario the policy was written for. The case is better than it looks, with extensive tests, written specs and design records, and those should count. But they do not answer who maintains this in two years.
- **The bright-line clauses are clear.** "No co-author tags" and "no AI media" are easy to apply, which is more than can be said for the judgment clauses. The problem is that they are clear about the wrong property.
- **There are real legal questions.** The copyright status of purely AI-generated media is unsettled. A pack that ships under many licenses may reasonably want certainty about every asset's author.

### Proposed rules and procedures

The policy could keep its goal, which is to protect reviewers and maintainers, and replace impressions with checkable rules.

1. **Definitions.** Define three levels of AI involvement and apply the same rules to code, text and media:
   - *AI-assisted:* a human authored the work and AI suggested or edited parts of it.
   - *AI-generated, human-directed:* AI produced the work under human specification and review.
   - *AI-generated, unreviewed:* AI output submitted as is.

   Only the third level is banned outright.
2. **Mandatory, durable disclosure.** Require a disclosure level in the PR template, and allow or require a commit trailer. Disclosure must never on its own be grounds for rejection, so that honesty costs nothing.
3. **Asset provenance instead of an asset ban.** Every new asset ships with a manifest row giving author, method and license. Generated assets must also ship the generator source and a fixed seed so anyone can reproduce them. All assets, of any origin, pass the same art-direction review by a member of the texture team against a written style guide.
4. **An understanding check instead of "fully understand."** For PRs above a size threshold, a maintainer picks several areas of the change, and the author explains them in a short review call or written Q&A without tools. Passing or failing that is a recorded decision, not an impression.
5. **Scale gates.** A new mod or a PR above a set size (for example 5,000 changed lines) needs:
   - a named champion on the team,
   - a design discussion recorded before the work starts,
   - submission in staged, separately reviewable PRs.

   This addresses review load directly, whatever tools were used.
6. **Reproduction instead of "personally hit."** A bug fix must include a reproduction, which can be a player report with logs, a failing test or recorded steps. The fix must show the reproduction no longer fails.
7. **An enforcement procedure.** Before any block:
   - a written notice citing the clause and the evidence,
   - a set window for the contributor to respond,
   - a decision by a named role (for example two admins),
   - a stated duration,
   - one appeal to the admin group.

   Publish the outcomes, without names.
8. **Versioning and review.** Version the policy, record why each clause exists, and review it on a fixed schedule, for example every six months.

## Path forward

If GregGuns is to be proposed to GTNH, this order respects both the current policy and the project's honesty:

1. **Pitch first.** Raise the idea in `#mod-dev` or `#github-discussion` before M9 work begins. Ask whether combat belongs in the pack, at which tiers, and who would champion it. The answer may change M9 entirely.
2. **Replace all media with human-made work**, by the owner or by GTNH texture creators. Treat the generated set as placeholders. This is the largest single cost.
3. **Disclose; never launder.** Do not strip co-author lines from history, and do not relabel or lightly edit generated assets to get them past the policy. That would be evasion, and it would justify the block the policy threatens. From the pitch onward, the owner writes commits, quests and PR text in their own voice.
4. **Cut the scope to a core the owner can explain**, likely guns, ammo and armor. Weight it toward IV and above, and stage it across reviewable PRs.
5. **Release standalone first.** Ship GregGuns as an opt-in addon players can add to GTNH themselves. That builds a maintenance record and finds bugs the owner has personally hit, which is the kind of evidence the current policy asks for.
6. **Argue for policy change separately.** Propose the rules above in a policy discussion, not inside a PR that depends on them passing.
