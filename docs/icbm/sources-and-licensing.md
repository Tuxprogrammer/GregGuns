# ICBM features: sources, licenses and reuse plan

- Status: Accepted (owner decisions recorded 2026-10-20)
- Scope: the missile, launcher, radar and explosives work GregGuns pulls in from ICBM-style mods for Minecraft 1.7.10.
- Living copy: [ICBM mods for 1.7.10: licenses and reuse plan](https://claude.ai/code/artifact/e14ed4ff-062e-4ac7-bbb7-fce1aa0dcf67). This file is a snapshot of that doc.

> **For agents:** this document is the source of truth for which third-party ICBM code and assets GregGuns may copy, where each copied file goes, and what notices it needs. For the sources listed in the plan below, it is an exception to the "assets are original" rule in [CONTRIBUTING](../../CONTRIBUTING.md). Copy only from the sources and commits named here. Anything else stays design reference only, the same as Techguns.

## Bottom line

- **No repo is MIT for both code and assets.** Every ICBM line puts its textures, models and sounds under different, stricter terms than its code.
- **Code: use BuiltBrokenModding/ICBM-Classic.** Its code is MIT, it is actively maintained, and about 94% of the current lines were written by the BBM team. Copy logic with the MIT notice, then port it onto GTNH APIs.
- **Assets, most free first: BBM's frozen MIT snapshot, then Calclavia's LGPL 1.6.4 set, then other free sources, then new art.**
  - ICBM-Classic at commit `2f807bf8` (2021-11-28) is the last state under a plain MIT LICENSE, before the asset notice was added. It holds 235 PNG textures, 35 OBJ models and 31 sounds.
  - Calclavia's 1.6.4 assets are LGPL-3.0. Per Spencer, the 2014 handoff covered only the 1.7.10 rewrite.
  - HBM's Nuclear Tech (also LGPL-3.0) and CC0 libraries fill the gaps before anything is drawn from scratch.
  - LGPL assets can sit in GregGuns the same way the GT5U textures already do: a carved-out folder with its own license file and only the notices that license requires.
- **Decision: no outreach to BBM.** The plan relies only on licenses as they were published, and accepts the small residual risk that comes with that.

## Candidates at a glance

| Repo | MC | Code | Assets | Verdict |
| --- | --- | --- | --- | --- |
| [BuiltBrokenModding/ICBM-Classic](https://github.com/BuiltBrokenModding/ICBM-Classic) `prod/1.12` | 1.12 (1.14 in dev) | **MIT**, code and data only | ARR, licensed to BBM "for project only" | **Best code base** |
| ICBM-Classic `prod/1.7` | 1.7.10 | No LICENSE file on this branch; old class headers pointed at the restrictive VoltzEngine license | Mostly Calclavia-era (see below) | Closest to 1.7.10, but has no LICENSE file (read, don't copy) and depends on VoltzEngine |
| [BuiltBrokenModding/ICBM](https://github.com/BuiltBrokenModding/ICBM) ("ICBM 2") | 1.7.10 | **MIT**, code only (LICENSE since 2018); the README still shows the old educational terms | Excluded from MIT | Usable code, but a hard dependency on VoltzEngine; last code change 2018 |
| [calclavia/ICBM-Classic](https://github.com/calclavia/ICBM-Classic) `development` | 1.6.4 | **LGPL-3.0**, added by Calclavia on 2015-06-13 | Same LGPL notice covers the whole repo | Asset source, tier 2 |
| [HbmMods/Hbm-s-Nuclear-Tech-GIT](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT) `master` | 1.7.10 | **LGPL-3.0** | Same LGPL-3.0, no carve-out | **Asset source for gaps**; code is heavily coupled to the rest of NTM |
| [aidancbrady/DefenseTech](https://github.com/aidancbrady/DefenseTech) | 1.7.10 | No license; README says any public redistribution must go through DarkGuardsman | Calclavia-era (163 of 185 files byte-identical to Calclavia's) | **Avoid** |
| [BuiltBrokenModding/VoltzEngine](https://github.com/BuiltBrokenModding/VoltzEngine) | 1.7.10, 1.12 | Built Broken Public License: no redistribution without permission | Same | **Avoid**; replace with GTNH equivalents |
| Flan's Mod, MCHeli, Rival Rebels | 1.7.10 | CC BY-NC-SA, ARR, or unknown | Same | Avoid |

**[jdawg3636/ICBM](https://github.com/jdawg3636/ICBM) ("Intercontinental Ballistic Redux").** This is not a clean rewrite. It is a fork of BBM's repo, with the same root commit from Calclavia in 2012, and it branched from `prod/1.12` in 2020.

- **Code:** MIT. The 1.16.5 port on `master` was active until 2026-09. Its `1.7.10` branch is just a mirror of BBM's `prod/1.7`, with the same head commit and no LICENSE file.
- **Its LICENSE names only jdawg3636 and drops BBM's MIT notice.** If you copy from it, keep both notices.
- **Assets:** its README says "All models/texture/sound licenses are ambiguous and inherited. All rights reserved." Of its 839 asset files, 109 are byte-identical to Calclavia's originals; the rest are 1.16 JSON models and newer textures of unstated authorship.
- **Verdict:** reference only. Its code is MIT but sits further from 1.7.10 than BBM's 1.12 code. Its own processing machines (Metallurgic Leaching Chamber, Centrifugal Deposition Unit) are the only parts that clearly belong to jdawg3636 alone.

GTNH ships no missile mod today. Its build tooling has a GTNH fork of HBM (frozen in 2021) on its list of org repos it deliberately does not track as pack mods ([repo-blacklist.json](https://github.com/GTNewHorizons/DreamAssemblerXXL/blob/master/repo-blacklist.json#L38)). That is housekeeping, not a ban. The pack's per-mod license data shows 73 MIT and 69 LGPL-3.0 mods alongside ARR mods with explicit permission, so LGPL assets are not a barrier to inclusion. No written license rule for inclusion was found.

## How the ICBM lineage untangles

1. **2012–2014, Calclavia.** One repo, no license, so all rights reserved. The sentry guns belonged to DarkGuardsman (Robin Seifert).
2. **2014 transfer.** The BuiltBrokenModding/ICBM README says ownership "was shifted fully to DarkGuardsman … in exchange for majority ownership of Resonant Induction to Calclavia". DefenseTech's README, written in 2016 by Aidan Brady, independently confirms an "agreement made back in 2014". Calclavia's own account of the deal is not documented anywhere.
3. **The split.** Calclavia's repo and BBM's share history up to 2014-07-03. After that, Calclavia added only 10 commits: README edits, a banner, and on **2015-06-13 the LGPL-3.0 license** for the 1.6.4 code and assets.
4. **BBM era.**
   - Educational Public License from 2014-11, then **MIT from 2018-12-30** (commit 475c3fef).
   - "Applies to Code only" was added in 2022.
   - An asset license with per-file credits was added in 2021–2023.
   - Definitions were updated on 2026-10-17, adding a new exclusion for "names, likeness".
   - BBM's CLA assigns contributions to the team, which supports its right to relicense.

**Resolved: the 1.6.4 assets are LGPL-3.0.** Per Spencer, the 2014 handoff covered the 1.7.10 rewrite. The 1.6.4 code stayed with Calclavia, who licensed it and its assets LGPL-3.0 on 2015-06-13.

**The plain-MIT window (2018-12-30 to 2021-12-02).** ICBM-Classic's LICENSE was unqualified MIT until the asset notice was added in December 2021; "code only" came in June 2022. A snapshot from that window (`2f807bf8`, 2021-11-28, the last commit before the notice) holds 347 asset files, not counting 1.12 JSON:

- 138 are byte-identical to Calclavia's LGPL set, so they are covered either way.
- 203 are BBM-era files: 35 OBJ models with their MTL files, 47 connected-glass textures by Ole Henrik Stabell (2017), 36 edited versions of Calclavia textures, and the rest committed by Robin.
- Only 24 of them are still in today's build.

MIT has no revocation clause, so files published under it stay MIT for anyone who copied them then, and the later notices can't take that back. The one limit is that BBM could only license what it had rights to. That matters mainly for the sounds of unknown origin (see the plan).

## Assets in detail

**ICBM-Classic today (1.12).**

| Owner | Files | License |
| --- | --- | --- |
| Darkguardsman | 110 | ARR, licensed to BBM for project |
| Morton | 55 | CC BY-NC-SA 4.0 |
| Unknown (mostly Calclavia-era sounds), marked for replacement | 30 | Unknown |
| Other | 3 | Freely usable: `meep.ogg` CC BY 4.0, `missileinair_short.ogg` CC BY 3.0, `radioactive_tick.ogg` CC0 |

The asset license says assets "can't be distributed as part of a new project". Custom versions need "major changes" and must not use the ICBM name, and permission is revocable. **From the current build, only the three permissive sounds are reusable.** Use the frozen snapshot instead.

**Calclavia-era assets.** Calclavia's LGPL repo has 263 resource files. Of these, 223 are byte-identical to ICBM-Classic `prod/1.7` and 163 to DefenseTech. So the familiar 1.7.10-era ICBM look is mostly Calclavia's LGPL-released set. Only about 23 of those files (sounds, plus two glass textures) survive in today's 1.12 build.

**HBM's Nuclear Tech.**

- **License history:** ARR until 2020-06-07, GPLv3 until 2022-07-10, and **LGPL-3.0 since then**. Use a current checkout.
- **Credits:** contributors are credited only in `gradle.properties`, with no per-file attribution and no CLA. Sound sources are undocumented, so that is a small residual risk.
- **Missile content:** about 8,500 lines across `com/hbm/entity/missile`, launch pads, radar and missile assembly, plus about 294 missile, launch and radar asset files.
- **Coupling:** the code depends tightly on NTM's own power, fluid and explosion systems, so take its assets and design, not its code.

## Recommended plan

1. **Code.**
   - Port ICBM-Classic's MIT logic from 1.12 down to 1.7.10 on GTNH APIs: blast shapes, missile flight, radar, launcher and targeting.
   - BuiltBrokenModding/ICBM (ICBM 2) is also MIT and already targets 1.7.10, so it's a useful reference and copy source. Read `prod/1.7` too, but don't copy from it, since that branch has no LICENSE file.
   - Keep BBM's MIT copyright and permission notice in a third-party notices file. That is all MIT requires.
   - Replace VoltzEngine with GT energy and GTNH UI libraries; that is GTNH integration work anyway.
2. **Assets, tier 1: BBM's frozen MIT snapshot.**
   - Pin ICBM-Classic commit `2f807bf80ab2fb4b534ccaadfd174b40942c11a6` (2021-11-28) and copy from that commit only.
   - Take its PNG textures, its 35 OBJ/MTL models (1.7.10 loads OBJ) and its sounds. Take the 1.12 JSON block and item models too. 1.7.10 can't load them as-is, but they are MIT geometry worth converting to OBJ or rewriting as 1.7.10 block and item renderers before modeling anything from scratch.
   - Record the commit hash and BBM's 2018 MIT notice in the third-party notices file. These files are MIT, so they need no carve-out.
3. **Assets, tier 2: Calclavia's LGPL 1.6.4 set** ([calclavia/ICBM-Classic](https://github.com/calclavia/ICBM-Classic), `development` branch).
   - Use it for anything the snapshot lacks. 138 of its files are already in the snapshot, where the MIT copy is the more convenient one to use.
   - Keep these files in a carved-out LGPL-3.0 folder with the LGPL-3.0 text and Calclavia's copyright line (what the LGPL requires), mirroring the GT5U exception in the GregGuns README.
4. **Assets, tier 3: other free sources before generating anything.**
   - **HBM's Nuclear Tech** (LGPL-3.0) for missiles, launch pads and radar neither ICBM set has. Its models are native 1.7.10 OBJ. Put them in the same LGPL folder, keeping HBM's copyright notice and license text.
   - **Textures already in the pack.** Reference GT5U, IC2 and other GTNH mods' icons at runtime instead of copying them, which costs nothing license-wise.
   - **Three permissive sounds** in today's ICBM-Classic: `meep.ogg` (CC BY 4.0), `missileinair_short.ogg` (CC BY 3.0) and `radioactive_tick.ogg` (CC0).
   - **CC0 libraries:** Kenney (kenney.nl), and OpenGameArt and freesound filtered to CC0.
5. **Assets, tier 4: generate the rest** with GregGuns' existing asset pipeline.
6. **Sounds of unknown origin.** About 20 sounds in both the snapshot and the 1.6.4 set came from freesound with no recorded source. Neither MIT nor LGPL fixes that, because BBM and Calclavia could only license what they had rights to. Find each original on freesound, download it fresh from there, and ship it under the license freesound lists, recording its author, source URL and license in the notices file, which is what CC BY requires. CC0 and CC BY are fine. CC BY-NC is acceptable as well. GTNH is non-commercial, its own pack repo is CC BY-NC-SA 4.0, and it already ships NC-licensed mods. Keep NC sounds in their own carve-out folder, because GregGuns' MIT core tells everyone they may sell it, and prefer CC0 or CC BY when both exist. Where no original can be found, swap in a CC0 sound.
7. **Naming.** Use "ICBM" freely in-game as the generic word for the weapon class, for example "ICBM Silo" or "Tier 3 ICBM". BBM's own asset license says "We do not own the concept of ICBM". Just don't brand the mod or its listing as "ICBM", "ICBM Classic" or BBM's project, and don't imply it is endorsed by them.
8. **Attribution: the legal minimum, nothing more.** Give no sign of approval, endorsement or collaboration.
   - Put every required notice in one third-party notices file, plus a LICENSE file inside each carve-out folder. Required means MIT and LGPL copyright notices with their license text, and author, source and license for CC BY sounds.
   - Mark changed LGPL files as modified in that notices file, which the LGPL requires.
   - Keep BBM, Calclavia, HBM and their projects out of the mod name, logo, `mcmod.info` credits, README feature text and listing pages. One line can point to the notices file.
   - Add one disclaimer line to the README and listings: "Not affiliated with or endorsed by Built Broken Modding, Calclavia, or the HBM's Nuclear Tech team."
   - Don't copy their logos or banners (for example `icbm_banner.png`), and don't describe GregGuns as a port, continuation or successor of their mods.
9. **Keep GregGuns' MIT core.** MIT snapshot files stay MIT, LGPL files stay LGPL inside the jar, and everything you write stays MIT.

*Not legal advice. Every claim above was checked against git history and license files on 2026-10-20 unless marked as reported.*
