# NewGunrizons recon

Read-only competitive recon of [vegeta1k95/NewGunrizons](https://github.com/vegeta1k95/NewGunrizons), done 2026-10-20 against `master` (HEAD of 2026-03-18) and the `1.0.1` release tag. Method: `gh` API queries plus a shallow clone that was read, never built or run, and has been deleted. Nothing from the repo was copied. Everything below is described in our own words.

## Summary verdict

**Stale, and not a merge candidate. Recommend GregGuns take the "GTNH gun mod" slot at release, with a courteous coexistence note instead of any code merge.**

- **Stale.** Last push 2026-03-18, about seven months ago. The author's entire history is 18 days (2026-03-01 to 2026-03-18), then silence. Two issues were filed after the first release; the one open bug report (LV progression) has had no author reply, and a user posted a workaround zip instead.
- **Its own master regressed.** The `1.0.1` tag has about 100 guns and a GregTech recipe class. Current `master` carries 3 guns (a shotgun, a plasma rifle, a laser rifle) and no GregTech recipe code at all. A later port PR itself notes "GregTech recipes are still absent from the code (removed upstream in March 2026)." The README on `master` still advertises 100+ guns and GT crafting, so the public face is inaccurate.
- **Wrong foundation for a merge.** It is a heavy fork of Vic's Modern Warfare (real-world firearms, plain items). GregGuns is a clean-room Techguns-style design with machines, multiblocks, NPCs and worldgen. The two share only "guns in GTNH".
- **Licensing is murky** (see License): derived work, README defers to the original's terms, no real LICENSE file in the root. Borrowing code is off the table; borrowing ideas is fine.
- **Demand is thin but real.** 4 stars, 2 forks, 153 release downloads in total across two releases. Not in the GTNH pack. The Discord interest is best served by shipping something maintained.

## Features

Counts are from the `1.0.1` tag unless noted. `master` is a cut-down subset.

**Guns.** 101 gun factory classes: 32 assault rifles and carbines, 3 LMGs, 18 pistols, 8 shotguns, 20 SMGs, 21 sniper and marksman rifles, 2 special. Real-world models (AK family, M4/M16, G36, AUG, SCAR-H, MP5, P90, Barrett, SMAW, and so on). `master` keeps only a Doom-style shotgun, a plasma rifle and a laser rifle.

**Ammo and magazines.** Caliber-specific ammunition. Detachable magazines that must be loaded with the matching bullet type. Three bullet-casing size classes (small, medium, large) exist as crafted intermediates. Selective fire (semi, burst, auto). Shotgun shells and a rocket for the SMAW.

**Attachments.** Scopes and sights (ACOG, holographic, reflex, Russian optics), caliber-specific suppressors, grips (angled, stubby, vertical, bipod) and lasers. A per-gun compatible-attachment table and an attachment mode key. Scopes render a real zoomed picture and cooperate with shaders.

**Grenades.** Fuse and impact grenades, with an explosion entity.

**Armor, machines, blocks, NPCs, worldgen, loot.** None. The only block asset is a blood-particle texture. Entities are projectile-side only: bullet, grenade, ejected shell casing, generic projectile, explosion.

**Rendering.** Models are Bedrock-style geometry JSON with Bedrock-style animation JSON, converted from the original Java models by Python scripts in `tools/`. The animation system was migrated to that format on 2026-03-14. Muzzle flash with dynamic light, tracers, ejected casings, a camera recoil controller, blur and scope post-processing shaders (GLSL), plus Mixins into the renderer. Explicit Angelica/Iris compatibility work.

**Config.** No config file in the repo. A user's workaround on the issue tracker adds a hold/toggle option for aiming, which suggests the author never added one.

**Controls.** RMB aim, LMB fire, R reload, Right Shift fire mode, M attachment mode, L laser, bracket keys zoom.

**Sounds.** About 212 `.ogg` files in the tree.

## GregTech integration depth

**Shallow: recipes only.** On the `1.0.1` tag, a single class (`GTRecipes`) registers recipes using GT materials, ore prefixes and `GTValues`. By count: about 29 Assembler recipes and 10 Forming Press recipes. There are no MetaTileEntities, no custom recipe maps, no multiblocks, no machines, no EU consumers, no GT-material-driven stats, and no GT tier scaling of damage or ammo behavior. The mod has no block of its own.

**Tier gating is done through ingredients.** The README table: LV guns use steel barrel, steel receiver and wood stock; MV adds stainless and plastic; HV titanium and stainless; EV and IV tungsten steel with a carbon stock. Component items are one class per material (barrel, receiver, stock) plus firing mechanisms, lenses, casings and part kits. This is a coarse "higher voltage unlocks better parts" ladder, not a system.

**It is partly broken as shipped.** A user reported that LV and MV Forming Press recipes for firing mechanisms need more input slots than the machine has ([issue #2](https://github.com/vegeta1k95/NewGunrizons/issues/2)), so those items cannot be made at those tiers. A third-party fix exists only as a zip attached to the issue.

**On `master` the integration is gone.** No GT import or recipe code remains, and `dependencies.gradle` carries only NEI, GTNHLib and (runtime) Angelica, with no GT dependency.

## Activity and health

| Measure | Value |
|---|---|
| First commit | 2026-03-01 (repo created on GitHub 2026-03-08) |
| Last commit / push | 2026-03-18 (21 commits on `master` since `1.0.1`) |
| Active span | 18 days, roughly 57 commits (a burst of refactors, then cleanups) |
| Cadence since | zero commits for about 7 months |
| Contributors | 1 (the owner) |
| Releases | `1.0.0` (2026-03-08, 23 downloads), `1.0.1` (2026-03-10, 130 downloads); 153 total |
| Issues | 2 total: #1 "Doesn't work on servers" (closed, fixed in `1.0.1`), #2 "LV progression" (open, no author reply) |
| Pull requests | 0 |
| Stars / forks | 4 / 2 |
| Target version | GTNH 2.8.4 only; README warns it may not work on other versions |

Notes:
- The `1.0.1` fix was a dedicated-server crash from client classes loaded on the server. The first release did not run on any server, so the mod has had little multiplayer exposure.
- The commit log shows AI-assisted authoring (a commit removing an agent-instructions file carries a Claude co-author trailer) and a refactor-first style.
- Forks: [EmoUsedHM01/new-gunrizons](https://github.com/EmoUsedHM01/new-gunrizons) (last pushed 2026-04-14, the workaround author) and [danglol240/NewGunrizons](https://github.com/danglol240/NewGunrizons) (see Pack status).
- A search for other "Gunrizons" repositories returns only the original.

## License

- GitHub reports **MIT**, but the repo root has only a `LICENSE-template` file, and the `LICENSE` under `src/main/resources` is the unfilled MIT placeholder (`[year] [fullname]`). No clean root LICENSE exists.
- The README says the mod is a derivative of [Vic's Modern Warfare](https://github.com/vic4games/modern-warfare) and tells readers to refer to that project's terms. The upstream repo is **Apache-2.0** with a NOTICE file, last pushed 2023-11-02. Apache-2.0 allows derivatives but requires preserved notices, and says nothing reliable about the models, sounds and textures, whose provenance (many real-gun models and sounds) is unknown.
- **What this allows us.**
  - *Merging code:* not advisable. The legal chain is unclear, and it conflicts with the GregGuns original-work posture (we refuse to copy Techguns for a similar reason). Importing a third project's inherited code would also muddy the clean-room story.
  - *Assets:* do not reuse. Provenance is unknown and the art style does not match ours.
  - *Ideas:* free. Facts about what a mod does are not copyrightable.
  - *Collaboration:* only at the owner's explicit say-so. This recon made no contact.

## Pack status

- **Not in GTNH 2.9.0-RC-2.** The local `MOD-INDEX.md` lists 242 mods and has no entry for NewGunrizons, Techguns, Modern Warfare or any firearm mod (case-insensitive search for those names and "firearm" returned zero hits).
- **No pack discussion found.** `gh search issues`, `gh search prs` and code search (for "NewGunrizons" and "Gunrizons", scoped to the GTNewHorizons and GTNH-Distribution orgs) return nothing in those orgs. The only hits are the author's own two issues and the fork PRs below.
- **Third-party port activity (a fork, not the pack).** [danglol240/NewGunrizons](https://github.com/danglol240/NewGunrizons) opened three PRs on 2026-10-16, all inside that fork, none upstream:
  - [#1](https://github.com/danglol240/NewGunrizons/pull/1), merged: bumps GTNHLib, NEI and Angelica to the 2.9.0-RC-2 versions.
  - [#2](https://github.com/danglol240/NewGunrizons/pull/2), open draft: port of the `1.0.1` full weapon set and GT recipes to 2.9.
  - [#3](https://github.com/danglol240/NewGunrizons/pull/3), open: "IV-UHV weapons", extending the ladder beyond IV.

  Last push there 2026-10-18; the PR text says it was generated with Claude Code. This is a plausible source of the current Discord interest. It shows demand exists while the original is abandoned. The fork has 0 stars.

## Overlap with GregGuns

| Area | NewGunrizons (`1.0.1`) | GregGuns | Gap |
|---|---|---|---|
| Guns | ~100 real-world guns, 3 on `master` | 43 Techguns-inspired guns | Theirs is a bigger real-world catalog; ours is a tech-tier lineup |
| Ammo and magazines | Caliber items plus magazines, 3 casing classes | GT-tiered ammo | They model magazines and calibers literally; we tier ammo by GT |
| Attachments | Scopes, suppressors, grips, lasers, per-gun compatibility | Attachment and scope slots per our spec | Comparable; their per-gun compatibility table is worth a look |
| Armor | None | Armor sets with set bonuses | We lead |
| Machines and multiblocks | None | GT MetaTileEntity machines and multiblocks | We lead |
| NPCs and mobs | None | Factions, AI, spawning | We lead |
| Worldgen and loot | None | Structures, ore clusters, loot | We lead |
| GT materials / tiers | Recipe ingredients only | Materials, recipe maps, tiers, EU, custom machines | We lead by a wide margin |
| Rendering | Bedrock-format geometry and animation JSON, shaders, dynamic-light flash | Clean-room models, our own renderer | Different approach; their shader and light work is Angelica-aware |
| Config | None | Config plus pack-change doc | We lead |
| Maintenance | Abandoned | Active | We lead |
| Server safety | Crashed on servers until `1.0.1` | Boot smoke test on a dedicated server | We lead |
| Real-world arsenal | Yes | No | They lead |

## Ideas worth considering

All in our own words; none requires any of their code.
1. **Real-gun fantasy as optional content.** Players asking for AKs and M4s are a visible slice of the audience. A later "classic arsenal" set of variants on our gun frames could capture them without adopting their codebase. Owner decision.
2. **Attachment compatibility as data.** A per-gun list of allowed attachments plus an explicit attachment-edit mode is a clean pattern. Check that our attachment spec expresses this as data, not code.
3. **Shader safety for optics.** They did explicit Angelica and Iris work for scopes, tracers and muzzle light. Worth a line in our client-test checklist: scopes, tracers and flash light must work with shaders on and off.
4. **Aim hold versus toggle.** A user wrote a fix for this and the thread praised it. Make sure ADS has a hold/toggle config option.
5. **Recipe-slot sanity check.** Their LV and MV recipes were uncraftable because the machine had too few input slots. We have `/gregguns dumprecipes` and `clash.py`; add a check that every recipe fits its machine's slot count.
6. **Server-safety lesson.** Their first release crashed dedicated servers on a client class load. Keep our headless boot smoke test as a release gate.

## Open questions for the owner

1. **Positioning.** Is "take the GTNH gun mod slot" the intent, or would you prefer a short coexistence note in the README (theirs is a real-world arsenal, ours is Techguns-style progression)?
2. **Outreach.** Do you want anyone to contact the NewGunrizons author or the fork author? This recon made no contact and left no trace (no issues, stars or comments).
3. **Discord.** Should we post to the interested players once there is a release candidate, and where?
4. **Real-world arsenal.** Is a classic-gun content set on our roadmap, and if so, at which milestone?
5. **Fork activity.** The danglol240 fork is reviving the mod for 2.9.0-RC-2. Watch it, or ignore it? If it gains traction it could become a competing "maintained" option and change the verdict.
6. **Pack route.** Neither mod is in the pack. Do we plan to propose GregGuns to GTNH ourselves (after the beta gate), and when?

## Sources

- Repo: https://github.com/vegeta1k95/NewGunrizons (tags [1.0.0](https://github.com/vegeta1k95/NewGunrizons/releases/tag/1.0.0) and [1.0.1](https://github.com/vegeta1k95/NewGunrizons/releases/tag/1.0.1))
- Issues: [#1](https://github.com/vegeta1k95/NewGunrizons/issues/1), [#2](https://github.com/vegeta1k95/NewGunrizons/issues/2)
- Upstream: https://github.com/vic4games/modern-warfare (Apache-2.0)
- Fork port PRs: https://github.com/danglol240/NewGunrizons/pulls
