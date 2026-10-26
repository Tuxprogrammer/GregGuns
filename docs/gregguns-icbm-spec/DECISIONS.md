# GregGuns ICBM: decisions and open questions

Each question here lists its context, the options and a recommendation. Context means how GregGuns already does it (design records) and how GTNH does it (pack precedent). Answered questions keep the option the owner chose. Once the spec settles, decided rows become design records in [docs/design/](../design/README.md). [0018](../design/0018-icbm-program.md) records the program-level decisions (D29–D38).

**From I0 on the orchestrator decides.** During the build ([ROADMAP-ICBM](../ROADMAP-ICBM.md#working-rules-autonomous-build)) the orchestrator answers open questions itself and adds them below, each marked `(orchestrator, <date>)`. The owner may revise any of them at the alpha review.

To decide an open question, reply with its number and an option letter, or write your own answer under it.

## Decided

The owner decided D1–D28 on 2026-10-20 and D29–D42, D47 and D50 on 2026-10-25, and D51, D55 and D56 on 2026-10-26. D43–D46, D48, D49, D52–D54, D57 and D59 are the orchestrator's.

| # | Question | Decision |
|---|---|---|
| D1 | How the spec is laid out | Two folders: [icbm-spec](../icbm-spec/README.md) (how ICBM does it) and this one (how we do it) |
| D2 | Code policy | Clean room by default. Named hard-math algorithms may be ported from ICBM-Classic's MIT 1.12 code with the notice kept. Nothing comes from the unlicensed `prod/1.7`. **Quality gate:** ported code is not committed or pushed until it meets the existing GregGuns bar: comments, tests, CONTRIBUTING patterns, the definition of done, and every known `ICBM-xx-nn` bug in it fixed. Never clone for speed. **Amended by D37** (other licenses) |
| D3 | Spec depth | Full sheets for mechanics; one catalog line per asset; full asset entries only for kept assets |
| D4 | Baseline | ICBM-Classic `prod/1.12` @ `9c1d985a`, plus an ICBM 2 modular-missile appendix and a 1.7.10 lineage appendix |
| D5 | Tiers on the GT ladder | Spread over HV to UHV. T1 HV; T2 EV–IV; T3 LuV–ZPM, with nuclear at ZPM beside the Nuke Launcher ([0001](../design/0001-tier-map.md)); T4 antimatter at UV; red matter at UHV (D10) |
| D6 | Roadmap | **Superseded by D29.** Was: spec now; build after 1.0 as an ICBM track (M10 and later) behind `modules.icbm` |
| D7 | Galacticraft | The GTNH Galacticraft fork is an **integration target**: launch pad, fuel loader, per-tier fuels and the NASA Workbench |
| D8 | How missiles are built and launched | Galacticraft workbench build, with the GC pad and our own silo. See [D8 in detail](#d8-in-detail). **Amended by D31 and D32** |
| D9 | Galacticraft dependency | Required by `modules.icbm` only. Without GC the ICBM module turns itself off with a log line, and the core mod is unaffected. The GC API is compiled against as an optional dependency |
| D10 | Which ICBM-Classic 1.12 content to keep (was Q2) | See [D10 in detail](#d10-in-detail) |
| D11 | Griefing defaults (was Q3) | **Terrain damage on by default**, with claimed territory respected through the existing pipeline (`ExplosionEvent.Detonate` and `BlockBreakGuard`). Config: `icbm.maxBlastTier` and per-tier `icbm.terrainDamage`. A missile in flight holds one chunk ticket and releases it on impact or timeout. A config caps how many missiles fly at once |
| D12 | Overlap with GregGuns weapons (was Q4) | **One shared explosive registry.** ICBM blast types become warheads that existing GregGuns launchers can carry later. The tactical nuke and the ICBM nuclear warhead share one nuclear blast implementation at different scales. ICBM's own handheld launcher and grenades are dropped. **Amended by D30** (annihilation) |
| D13 | Launch control (was Q5) | **OpenComputers first.** The ComputerCraft methods that ICBM 1.6.4 gave its launcher, cruise launcher, radar and EMP tower are ported as OC components. GregGuns ships ready-made programs on **craftable floppies**, so computer launches need no Lua from players. Also available: a ModularUI2 targeting GUI, data-stick target copying, and firing by redstone or the GT machine-controller cover. The floppy and its programs are D18 |
| D14 | Radar form (was Q7a) | **Short-range single-block radars in HV and EV hulls**, none below HV, because missiles start at HV just as GTNH moon rockets do. **A multiblock radar tower from IV up**, with range scaling by voltage tier. Both scan the missile tracker's list of missiles in flight rather than chunks, and treat a missile as "incoming" only if its predicted impact point falls inside the protected radius. Outputs: redstone, a GT cover, an OC component, and a data-stick link to SAM batteries |
| D15 | Where SAMs fire from (was Q7b) | **A separate air-defense battery**: a single-block machine that holds SAMs and fires when a linked radar reports a threat. It is independent of the silo |
| D16 | What an EMP does (was Q7c) | It **drains stored EU** in GT machines, cables and batteries, and gives **each multiblock one random maintenance issue**. Missiles use ICBM's kill-chance table. It loops over loaded tile entities, not block positions. **Each pulse affects at most N devices** (config, default 64), nearest first, so a dense base can't stall the server. **EU cost grows continuously with radius.** 1.6.4 and 1.7 divided the radius as integers, so every radius below the maximum cost the minimum ([A2-lineage](../icbm-spec/A2-lineage/index.md)) |
| D17 | Worldgen (was Q8) | **Abandoned Missile Silos**, on the M8 placement system. They are rare, Overworld-only and config-gated. See [D17 in detail](#d17-in-detail). Silos on Galacticraft planets are **approved but deferred** until after the MVP. **Amended by D36** (activity levels and guards) **and D38** (EV cap, planet silos now in scope) |
| D18 | OpenComputers programs (was Q10) | **One floppy, "Golden Dome"**, holding every program and the shared library. It is crafted in the GT Assembler from a blank OC floppy plus an MV circuit. The spec is [01-core/golden-dome.md](01-core/golden-dome.md) |
| D19 | Which fuel each missile needs (was Q6a) | **Keyed to the missile's GT voltage, matching GTNH rockets:** HV needs RP-1 (or Cetane-Boosted Diesel); EV–IV Dense Hydrazine; LuV Purple (CN3H7O3); ZPM and up Green (H8N4C2O4). Each tier accepts "this fuel or better". Missile classes register these lists in GC's `RocketFuels`, so the GC pad and loader enforce them |
| D20 | How much fuel (was Q6b) | **A fixed amount per missile tier**, shown in NEI and on the tooltip, times one config factor. **Unburned fuel strengthens the blast:** a missile that hits a closer target arrives with fuel left, and that fuel adds to its blast radius, as leftover propellant does in real life. The bonus is capped (config). See [D20 in detail](#d20-in-detail) |
| D21 | Fallout (was Q9) | Option A, with owner amendments: hard server-safety caps, protection from any hazmat-capable armor, damaging rubble, and radiation kept within 2 blocks of the surface. See [D21 in detail](#d21-in-detail) |
| D22 | How the silo is fueled (was Q6c) | **A GT fluid input hatch** on the silo, checking the same `RocketFuels` lists as the GC pad, so fuel rules match exactly |
| D23 | Warhead size (was Q11) | **A quantity knob, stored in NBT.** A warhead is one item that records its explosive type and fill count (up to a cap set by the casing tier, after ICBM 2's 1/5/20/64/128). The blast is computed from that NBT at detonation, together with the D20 fuel bonus. NEI shows the warhead's recipes once, not one entry per fill level. See [D23 in detail](#d23-in-detail) |
| D24 | Guidance and fins (was Q12) | **The guidance computer's circuit tier sets scatter; there are no duds; fins are structural.** Scatter is triangular per axis (the difference of two uniform rolls, after ICBM 2), and its radius shrinks for each guidance tier above the missile's tier. ICBM-Classic's distance term `10·(d/range)²` is added on top. There is no failure roll. Fins are a tier-material gate, like GC fins |
| D25 | Flight model (was Q13) | **A real entity for the whole flight, with a sliding chunk window.** The missile keeps loaded only the chunk it is in and the next chunk on its path, and releases chunks behind it, so it never accumulates loaded chunks. See [D25 in detail](#d25-in-detail) |
| D26 | Fusing (was Q14) | **Deferred.** Impact fusing only in the first build. Airburst and penetrator fuses can be revisited after missiles ship |
| D27 | Automating assembly (was Q15) | **The NASA Workbench for hand building, plus GT Assembly Line recipes for automation.** The Assembly Line needs a research data stick, so early players build by hand and automation opens once they have an Assembly Line. The warhead's NBT (D23) carries through to the missile, which needs custom recipe handling. Silos load missiles through a GT input bus |
| D28 | Art and audio (was Q16) | **Licensed tiers first; generate only the gaps.** For ICBM content this is an exception to [0006](../design/0006-art-direction.md). See [D28 in detail](#d28-in-detail). **Amended by D33** |
| D29 | Placement and packaging | **A module in the GregGuns jar** (`modules.icbm`), built next on its own [ROADMAP-ICBM](../ROADMAP-ICBM.md): I0–I7, an alpha gate (one owner review) and a beta gate (owner playthrough). The module defaults to off until the beta gate. The handoff ([HANDOFF-PLAN](../HANDOFF-PLAN.md)) waits for it. [0018](../design/0018-icbm-program.md) §1–4 |
| D30 | Nuclear and higher warheads | **Annihilation:** inside the kill zone of a nuclear (ZPM), antimatter (UV) or red matter (UHV) warhead every living entity dies, **bosses included**, through armor and hurt-resistance frames. Players are subject only to server damage settings. The Nuke Launcher keeps its own rule (bosses lose half). One shared implementation with a bosses flag. 0018 §7 |
| D31 | Building the missile in the world | **Stack on the pad.** The Workbench or Assembly Line makes the body; the player places it on the pad or in the silo and places a warhead block on its nose; arming fuses the stack into a live missile. Silos can mate body and warhead from input buses. A warhead block alone is a demolition charge. **Warheads and bodies are real 3D models**, not textured cubes: see [D31 in detail](#d31-in-detail). 0018 §8 |
| D32 | The Galacticraft pad | **Never removed or damaged.** Galacticraft's `EntityAutoRocket.onLaunch` deletes the pad on every rocket launch (player rockets get it back on landing; cargo rockets lose it). Our missile implements the docking and fuel interfaces without extending `EntityAutoRocket`, and we deny `EventLandingPadRemoval` for pads holding our missile. Scenario `icbm.pad.survives`. 0018 §9 |
| D33 | Asset sourcing | **Human-made, properly licensed assets first; generation only as a last resort; no tuning loops.** **Amended by D47** (assets from licensed mod jars; generation announced first). CC0, CC-BY, MIT and LGPL allowed, in a third-party folder with license text and CREDITS entries ([CONTRIBUTING](../../CONTRIBUTING.md#assets-and-localization)). 0018 §10–11 |
| D34 | Cost and damage | **An automatable arsenal:** a missile costs about one of its tier's GC rocket launches, Assembly Line-automatable; the warhead is the expensive part. **Conventional center damage, at least:** T1 1,200, T2 3,000, T3 8,000, sized against Infernal Mobs pools (base × modifiers × 1.8; up to about 2,700 for modded mini-bosses). 0018 §5–6 |
| D35 | How it is built | Autonomous: one or two Sonnet coding agents, one Opus reviewer per milestone, an automated per-branch gate, batched client checks per milestone, and the [ported-code gate](../../CONTRIBUTING.md#ported-code-mit) for MIT ports. The orchestrator records its own decisions here. **Amended by D39** (a third agent for guides), **D42** (balanced pair) and **D46** (the no-Galacticraft gate run). 0018 §12–15 |
| D36 | Silo activity levels and guards | Each Abandoned Missile Silo rolls an **activity level**: **abandoned** (heavily damaged, no spawners), **partly abandoned** (moderate damage, spawners in some sections) or **operational** (intact and lit, spawners in every section). Guards are GregGuns NPCs from **spawner blocks**, so the player fights through the silo like a dungeon and breaks the spawners to take it over, or lives with the shooting until they do. Loot grows with activity. See [D36 in detail](#d36-in-detail). Supersedes D17's "no NPC guards". 0018 §16 |
| D37 | Third-party code licenses | **MIT preferred; CC0, CC-BY, MIT and LGPL code acceptable.** LGPL logic is preferably described in the spec and rebuilt clean-room, which makes the result ours and MIT; the quality gate rewrites ports anyway. LGPL code adapted from its source, rather than rebuilt from a description, stays LGPL and lives in the third-party folder with its license text and a notices entry. Nothing unlicensed, NC or All Rights Reserved. 0018 §11 |
| D38 | Silo loot ceilings and planet silos | **Overworld silos cap at EV.** **Silos generate on other planets too**, with the ceiling rising one voltage tier per rocket tier: Moon EV, Mars group IV, T3 planets LuV, and so on up to T9 at UMV. **Ross128b and Ross128ba roll any tier** per silo. Asteroid belts have none. See [D38 in detail](#d38-in-detail). Supersedes D17's HV cap and its deferral of planet silos. 0018 §17 |
| D39 | Visuals and player guides | Owner, 2026-10-25: the module gets the guide sheets, preview sheets, renders and quests GregGuns has, planned per milestone in [ROADMAP-ICBM § Visuals and player guides](../ROADMAP-ICBM.md#visuals-and-player-guides). **A third Sonnet agent, the guides agent,** owns everything players see outside the code. The owner is told whenever new visuals land, without blocking the build. Amends D35 |
| D40 | ICBM quests | Owner, 2026-10-25: **ICBM quests go in the GregGuns tab**, not in a tab of their own. Keys are `gg.icbm.*` in `tools/questbook/quests.json`, added in the milestone that ships their content and installed from the jar like the rest of the line. Replaces I7's "ICBM tab" |
| D41 | Tooltips | Owner, 2026-10-25: ICBM items and machines carry the GregGuns tooltip polish: GT tier colors (`TierText`), "Added by: GregGuns", Shift for details, unit-tested `*TooltipFormat` classes, lang keys, compatibility lists, the annihilation line, and labeled GUI controls that say why they are locked. See [ROADMAP-ICBM § Carried over](../ROADMAP-ICBM.md#carried-over-from-the-gregguns-roadmap) |
| D42 | Agent balance | Owner, 2026-10-25: the two coding agents split each milestone by effort along a clean seam, both writing code, and message each other directly about the seam. The orchestrator tracks their time and tokens and rebalances. Amends D35 |
| D43 | Model format (orchestrator, 2026-10-25) | **Wavefront OBJ** with one texture per model, loaded by Forge's built-in OBJ loader for the placed and flying models. A licensed source in another format is converted once, and the conversion is noted in CREDITS as a modification. The guide renderer reads the same OBJ files, so a sheet always shows the model the game draws |
| D44 | Roadmap parity (orchestrator, 2026-10-25) | Every check the GregGuns roadmap made of a feature applies to ICBM content too: NEI, unification, MTE ID reservation, GT recycling data, a scenario per block, two-client sync, the recipe-clash review, PACK-CHANGES, GuideNH and server-owner docs. Listed in [ROADMAP-ICBM § Carried over](../ROADMAP-ICBM.md#carried-over-from-the-gregguns-roadmap) |
| D45 | Ported-code gate scope (orchestrator, 2026-10-25) | The [ported-code gate](../../CONTRIBUTING.md#ported-code-mit) checks **ICBM-Classic MIT ports** now. When the first port from another source arrives (CC0, CC-BY, MIT or LGPL per [D37](#decided)), the header becomes generic (`Ported from <project> (<license>)`, a pinned permalink, a notices entry) and the gate covers it. Adapted LGPL code lives in `com.gregguns.thirdparty.<project>` with its license text |
| D46 | No-Galacticraft gate (orchestrator, 2026-10-25) | Every ICBM branch's gate also runs the **full scenario suite with `--no-galacticraft` and icbm on**. Galacticraft is in every dev run now, so GC-conditional fallbacks would otherwise go untested. Amends D35 |
| D47 | Asset source order | Owner, 2026-10-25. **1. Human-made, licensed assets**, from asset libraries and **from inside licensed mod jars**. Look first in the GTNH pack's own mods (their licenses are in the reference pack's `MOD-INDEX.md`), then on Modrinth, CurseForge and similar. **The mod's declared license covers the whole jar, assets included,** as Modrinth and CurseForge projects usually declare one license for everything. So an MIT mod's textures, models and sounds can be extracted from its jar and used. The exception is a mod that **carves out its assets separately**, for example a LICENSE or README that says "code MIT, assets All Rights Reserved": then the asset license decides. Check the jar's own LICENSE files and the project page before extracting. Allowed licenses are CC0, CC-BY, MIT and LGPL (D37). Record the mod, its version, the jar's download URL and the license in CREDITS. Never Techguns, and never the current ICBM-Classic 1.12 assets. **2. Only then, generation** by our own procedural scripts, with no tuning loops. **3. Generation is announced.** Before generating any asset, the orchestrator names it and the planned fallback in that milestone's owner update; the build doesn't wait. Amends D33 |
| D48 | Warhead model source (orchestrator, 2026-10-25) | The warhead models and textures for every tier come from **HBM's Nuclear Tech Mod** (Modrinth; the README declares LGPL-3.0 for the whole project, no separate asset license), at tag `1.0.27_X5846`, its standard `mp_w_*` warheads only. They are modified (scaled, recentered, recolored, converted to strict OBJ, one label painted out) and stay LGPL in `thirdparty/ntm/` with the LGPL and GPL texts. The GTNH pack's own rocket models (Galacticraft, GalaxySpace) are "Explicit Permission" and not usable. Risk recorded in [asset-sourcing](07-visuals-audio/asset-sourcing.md): a community project's license is only as good as its contributors' rights. T3 conventional reuses the T1 model at full size until a distinct licensed model turns up. NTM's fuselage, fin and thruster models are the planned source for I2's missile bodies |
| D49 | Endgame warhead size (orchestrator, 2026-10-25) | Antimatter and red matter warheads occupy **2×2×4** blocks (D31 said "larger than 1×1×2"). Model files live in `models/thirdparty/<source>/` and their textures in `textures/models/thirdparty/<source>/`, bound directly by the renderer since they sit outside the block atlas |
| D50 | Local previews, committed guide sheets, placeholder drafts | Owner, 2026-10-25. **Preview sheets and the visuals gallery page stay local,** git-ignored (`docs/assets/previews/icbm/`, `docs/icbm-visuals.html`), and never enter the repository's history; the owner opens them from the checkout. No hosted pages unless the owner asks. **Player guide sheets (infographics) are committed.** **Placeholder guide sheets are allowed,** drafted from the spec's planned numbers with a visible "Draft: numbers pending" stamp, so the guides agent works alongside the coders; the final sheet takes its numbers from the code and drops the stamp. Amends D39 |
| D51 | Demolition fuse length | Owner, 2026-10-26: a 5-second hand-lit fuse is too short to outrun a 14–20 block blast. **The fuse scales with the warhead's actual blast radius at its fill: 3 seconds to react plus 1 second per 3 blocks of radius, rounded up** (T1 condensed, radius 9: 6 s; T2 thermobaric, radius 20: 10 s), times a config multiplier. A warhead set off by another explosion keeps the short chain-reaction fuse (10 ticks). Applied in the I1 fix round, with the guide sheet's final pass |
| D52 | ICBM terrain damage switch (orchestrator, 2026-10-26) | ICBM blasts break terrain according to their own per-tier `icbm.terrainDamageT<n>` settings, **on by default as D11 decided**, and are **not** gated by GregGuns' `explosions.blockDamage`, which keeps governing hand-held weapons only. Claims and protection are always respected through `ExplosionEvent.Detonate` and `BlockBreakGuard`. I1 as first built required both switches; corrected in the I1 fix round. The I1 exit box wording follows this |
| D53 | Blast engine details (orchestrator, 2026-10-26, from coder 1) | Center damage is `centerDamage × √(fill / maxFill)`, flat over the inner 10% of the radius and falling to zero at the radius. Blasts drop no items by default (`icbm.blastDrops` off). Radius is capped at 96 blocks. A saved blast resumes at its cursor once its crater is decided, and before that restarts from its snapshot. Terrain breaks only through the shared `ProjectileExplosion.breakBlock` path. Only the ray caster is ported (MIT); the scheduler, snapshot and budget are our own |
| D54 | Warhead content details (orchestrator, 2026-10-26, from coder 2) | A warhead's explosive tier equals its casing tier. Radius is `R × (fill / cap)^(1/3)`; fill caps are 5 (T1), 20 (T2) and 64 (T3). Each explosive has one GT recipe that makes a full-casing warhead, plus a cheap fill-1 condensed starter, since NBT cannot flow through a GT recipe. Placed-warhead textures are stitched into the block atlas through a custom sprite (`WarheadSprite`) so the static model draws in the chunk mesh. The dev self-check skips GT's auto-copied UEV electric implosion map, which always breaks the tier rule |
| D55 | Minecraft content: follow Mojang's usage guidelines | Owner, 2026-10-26. **The rule is not "no Mojang content"; it is "never break Mojang's usage guidelines."** Those guidelines let fans use game assets (textures, screenshots, mob skins) in guides and other fan content, as wikis and guides across the community do, as long as nothing looks official and Mojang's assets aren't redistributed in a product. So guide sheets may show **real vanilla textures** instead of stand-in icons: read at build time from the Minecraft jar in the reference pack, never committed as loose texture files, and credited "Minecraft textures © Mojang / Microsoft; not an official Minecraft product." Mojang assets never ship in the mod jar. The older GregGuns sheets may switch over when they are next rebuilt |
| D56 | Realistic warhead recipes | Owner, 2026-10-26. Warhead recipes use **GTNH-available explosives and GregGuns' own**, not vanilla curiosities: the explosive fill is a real explosive the pack makes (TNT, Industrial TNT, glyceryl trinitrate and stronger ones where the pack has them), climbing the ladder with the casing tier. Each blast type's payload is a **realistic analog made from GT materials** (fragments from steel or tungstensteel, napalm-style incendiaries, the gas, smoke and fuel-air effects as **general industrial pack chemicals held in canisters, chosen by in-game theme, with no chemical-warfare agent used or named**, a BartWorks culture for the contagious type, a copper-lined shaped charge for breaching, a metal-dust fuel-air mix for thermobaric, field generators for the sci-fi push and pull). Vanilla items stay only where they are the pack's standard ingredient. **Nuclear** combines a high-explosive lens with refined fissile material from GT's nuclear chain; **antimatter and red matter** use GTNH's own endgame exotics. Costs still meet D34. Replaces the first I1 recipes (spider eyes, slimeballs, anvils, ender pearls and similar) |
| D57 | Warhead recipes and parts | Orchestrator, 2026-10-26, from the owner-approved rework. **Every T1 and T2 warhead is one Assembler recipe at its casing tier** (HV 512 EU/t, EV 2,048 EU/t), 400 ticks, with its own programmed circuit and inputs at or below that tier; the Implosion Compressor and Chemical Reactor warhead recipes are removed (the Implosion Compressor ignores its additive metadata when a recipe has two item inputs, and GT copies every implosion recipe to the UEV Electric one). **Eight GregGuns parts** (own Assembler recipes, circuits 11-18, GT recycling data, D41 tooltips): **Plastic Explosive** (one generic part for both tiers: 2 PTFE plates plus 2 Industrial TNT make 4), Fuze Assembly Mk I (HV: Advanced circuit) and Mk II (EV: Data circuit), Shaped-Charge Liner Cone, Fragmentation Sleeve in steel (T1) and titanium (T2; tungstensteel waits for T3), Dispersal Canister, and **Napalm Canister** (a Dispersal Canister plus 1,000 L gasoline, the strongest plain fuel the pack makes at HV; no plastic or thickener, the name carries the theme). Gas-effect fills are general industrial fluids (nitrous oxide, sulfuric gas, creosote, biomass, kerosene) in canisters; the Chemical warhead also takes a few HarvestCraft mustard seeds as a pun, guarded by `Loader.isModLoaded` and omitted when the mod is absent. Other inputs: field generators (HV) and emitters (EV), lead, magnetic steel rods, titanium rods, and the Bio Lab culture dish. **Costs (D34):** the casing doubled earlier (4 stainless, 8 titanium) and is counted; T1 about one launch (18-22 plate equivalents), T2 trimmed to about 1.5 launches (37-39): fewer Industrial TNT and Plastic Explosive and one sleeve. The table is data (`WarheadRecipeSpec`, `IcbmPart`) with unit tests, and scenario `icbm.recipes.assembler` checks the live Assembler map. Item textures come from NTM (LGPL-3.0, D47/D48) except the liner cone, which is generated. Details: [warheads-and-fill](02-explosives/warheads-and-fill.md#recipes) |
| D59 | Blast radius means crater radius in stone | Orchestrator, 2026-10-26, from the I1 review. **An explosive's radius is the radius of the crater it digs in vanilla stone (explosion resistance 6)**, for both ray casters, not only the distance a ray can travel through open air. The I1 build gave the small caster (`castGrid`) the radius as its energy, so a radius-9 Condensed blast dug a 3x3 pit two blocks deep, and the sphere caster's energy was calibrated to air. Now both casters take a starting energy calibrated to stone: an average ray clears `radius` blocks of stone (`gridEnergy(R) = R x (6.0 + 0.75)`; `defaultEnergy(R)` = `R x (2.25 + 6.0) / 0.75`, never below the air value), and the small caster casts `max(16, 2R)` samples per cube edge. Softer blocks dig farther inside the same bounding sphere, harder ones less. Guards: stone half-space unit tests for both casters (depth at least R-2 and at least half the hemisphere) and scenario `icbm.blast.crater_size`. Details: [blast-engine](02-explosives/blast-engine.md#crater-size-d59) |

### D8 in detail

GTNH builds every rocket the same way, and missiles reuse that path:

1. A **missile chip Tn** is made in the GT Circuit Assembler at the tier's voltage, the way GTNH makes rocket schematic chips (T2 HV … T8 UV).
2. The Laser Engraver turns the chip into a **missile schematic Tn**. Using the schematic unlocks our page in the GC **NASA Workbench**, registered through GC's `SchematicRegistry`.
3. The workbench page has fixed slots:

   | Slot | Source |
   |---|---|
   | Warhead | The Assembler (D57) |
   | Body plates × N | The tier's plate |
   | Engine | — |
   | Fins × 4 | — |
   | Guidance computer | Assembler, at the circuit tier |

   No new crafting machine.
4. **T1–T2 launch from the GC launch pad**, fueled by the GC fuel loader. The missile entity implements `IDockable`. The player stacks the body and warhead on the pad (D31), and the pad is never removed (D32).
5. **T3 and up need the GregGuns silo**, a GT multiblock built under [0011](../design/0011-machine-policy.md) and powered by EU.

Evidence: the GTNH rocket recon (Galacticraft `3.4.34-GTNH`, NewHorizonsCoreMod `2.9.84`). Fuel: the GC fuel loader checks its tank against a `RocketFuels` whitelist keyed by entity class, so our missiles register their own fuel lists there. See Q6.

### D10 in detail

| Content | Decision |
|---|---|
| ICBM crafting items, battery, sulfur and saltpeter, concrete and reinforced glass, handheld rocket launcher and grenades, colors gas, Christmas mobs and the Nightmare missile | **Dropped.** GT, IC2 or GregGuns already provide them, or they are joke content |
| Red matter | **Kept as a UHV missile.** The owner tests it in-game, because it is resource-heavy (500 ray-traces a tick). Unit tests and headless boots only on our side. Our rebuild must not inherit ICBM's red matter ([red-matter.md](../icbm-spec/02-explosives/red-matter.md)), which: never ends or idles (starvation works only at size ≤ 1); keeps tracing 501 rays a tick forever once the ball is clear; reloads at full size (`ICBM-02-80`); has no range on its max-size config; and restarts its sound every tick. Ours needs a bounded lifetime, an idle state, correct save and load, and a tick budget shared with the blast engine |
| Antimatter | Kept, UV |
| Ender blast | Kept, T3 |
| Smoke | Kept, T1 |
| Rot and mutation | Kept. Nuclear blasts leave **radioactive blocks**, not just GT pollution, because many servers switch pollution off. Their debris yields GT radioactive byproducts; details in [D21](#d21-in-detail) |
| Antidote | Kept, mapped to GT hazards |
| Cargo parachute, balloon, cargo hook, held-item missile ("supply drops") | **Deferred.** Not a core feature |
| Spikes | **Dropped** (owner, 2026-10-25): a 1-damage trap block is meaningless against GTNH health pools |
| Glass button and pressure plate | **Dropped** (owner, 2026-10-25): vanilla button and pressure plate with a glass texture, no function of their own |

### D17 in detail

- **Placement:**
  - M8's grid system ([structure-placement](../gregguns-spec/11-worldgen/structure-placement.md)) on its own sparse grid: one cell per 64 × 64 chunks, 25% chance per cell, so roughly one silo per 2,000 × 2,000 blocks.
  - Overworld only, with a biome whitelist (plains, desert, tundra) and its own `enabled` flag.
  - It never cascades, so a silo bigger than the populate window is built across several populate calls, as M8 already allows.
- **Size: huge**, big enough for a player to build a megabase inside. The starting target to settle in the sheet:
  - a vertical shaft about 21 blocks across, running from the surface to about y = 12;
  - a main hall about 64 × 64 × 24 off the shaft's base;
  - side rooms for a control room and storage.
- **Mostly underground, with a clear surface marker.** One of three variants is chosen per silo:

  | Variant | Surface |
  |---|---|
  | **O, open** | The shaft is fully open to the sky |
  | **D, half open** | Half of the shaft mouth is open; the other half is a collapsed cap |
  | **Closed** | A capped shaft with a random selection of ceiling blocks replaced by **air columns that run all the way to build height**, so nothing generated later (trees, other structures) hides it |

- **Contents:** inert decorative GT casings, not working machines. No live warheads.
- **Loot:**
  - ~~Capped at HV~~ Superseded by [D38](#d38-in-detail): EV on the Overworld, and one tier up per rocket tier on other planets.
  - The main find is a salvageable damaged T1 missile body (GT recycling recipes).
  - A rare T1 missile schematic chip is a shortcut, never a requirement.
- **Guards and activity levels:** superseded by [D36](#d36-in-detail).

### D31 in detail

Owner, 2026-10-25: a placed warhead is a warhead-shaped object, not a cube with a texture on it.

- **Shape:** each warhead is a 3D model (a cone or ogive nose, with tier detailing), and its collision box and selection outline follow the model.
- **Size by tier:** T1–T2 warheads fit in 1×1×1. Bigger warheads take more space; the starting targets are 1×1×2 for T3 and nuclear, and larger again for antimatter and red matter. A warhead bigger than one block fills the extra cells with invisible filler blocks, so the whole model collides, breaks and drops as one piece.
- **Orientation:** placed alone it stands nose up. Placed on a missile body it seats onto the body's nose.
- **The body is modeled too:** on the pad or in the silo, the body is a full-height missile model standing on its fins, several blocks tall, with filler blocks for its height. Body plus warhead reads as one continuous missile.
- **Same model in flight:** arming swaps the placed blocks for the missile entity, which uses the same model, so there's no visual jump at launch.
- **Rendering cost:** placed warheads and bodies use a static block model, not a per-tick renderer, so a base full of charges stays cheap. Only the flying missile is an animated entity.
- **Assets:** the models follow D33 (licensed human-made sources first).

### D36 in detail

Owner, 2026-10-25: silos come in activity levels. An abandoned silo is a ruin to salvage; an operational one is a dungeon to clear and then take over.

| Level | Starting weight (config) | State | Guards | Loot |
|---|---|---|---|---|
| **Abandoned** | 50% | Fairly damaged: collapsed sections, holes in walls and floors, rubble, broken casings, no lights | None: nobody is home | The D17 salvage loot (damaged T1 missile body, scrap), sparse |
| **Partly abandoned** | 35% | Moderate damage, some sections intact, some lights | Spawners in some sections, usually the deeper ones | More and better than abandoned |
| **Operational** | 15% | Intact, lit, machines in place (inert decorative casings, as D17), signs of use | **Lots of spawners**, in every section including the shaft and the control room | The best, within the silo's ceiling ([D38](#d38-in-detail)), with a good chance of the schematic chip and rarely an intact missile body |

- **Guards are GregGuns NPCs** that shoot: the Army Soldier and Commando, with the Stormtrooper deeper in. They come from a new **silo guard spawner** block built on the M8 spawner system (`BlockHoleSpawner` and its layout marks), with its own kind and mob list.
- **The player pays to take it over.** Breaking a spawner stops it for good. Until the spawners are gone, the guards keep coming and keep shooting. The spawner rules (range, caps, light) follow M8's, so a cleared silo stays clear.
- **Damage is applied to the layout,** not carved after generation. Each level has a damage pass in the layout (removed blocks, rubble, broken casings) that runs inside the populate window, so it never cascades (D17).
- **Activity is independent of the surface variant** (O, D or Closed), though a closed, fully abandoned silo is the classic ruin.
- The weights, spawner counts per section and the guard list are config, with the starting values above. The orchestrator tunes them in I7 (hardening).

### D38 in detail

Owner, 2026-10-25: Overworld silos may hold EV loot, since other Overworld worldgen already rewards up to EV (AE2 meteors carry the EV-era presses). Silos on other planets scale up one voltage tier per rocket tier.

| Rocket tier | Planets with silos | Loot ceiling |
|---|---|---|
| — | Overworld | EV |
| T1 | Moon | EV |
| T2 | Mars, Phobos, Deimos | IV |
| T3 | Ceres, Callisto, Europa, Ganymede | LuV |
| T4 | Io, Mercury, Venus | ZPM |
| T5 | Enceladus, Titan, Miranda, Oberon | UV |
| T6 | Proteus, Triton | UHV |
| T7 | Pluto, Haumea, Makemake | UEV |
| T8 | Barnard C, E and F, Centauri A, T Ceti E, Vega B | UIV |
| T9 | Anubis, Horus, Maahes, Neper, Seth | UMV |
| Any | **Ross128b and Ross128ba** | **Rolled per silo, anywhere from EV to UMV** |

- **The planet tiers are GTNH's own.** They come from GT's dimension table, [`DimensionHelper`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/a3e1e11241a814c9fa0dd0973d5699548428f689/src/main/java/gtneioreplugin/util/DimensionHelper.java), at the pack's GT5-Unofficial tag.
- **Ross128 is the exception** (owner, 2026-10-25). Ross128b and Ross128ba are an alternate, challenge-start route, so their silos don't follow the ladder. Each silo rolls its own ceiling from the whole ladder (EV to UMV), weighted toward the low end by default (config). Its guards and salvage follow the rolled tier, and its decorative casings use that tier's GT machine hull, so a player can read the danger from the walls.
- **Asteroid belts have no silos** (Asteroids, Kuiper Belt, Mehen Belt, End asteroids): there's no ground for a shaft. A planet whose terrain generator can't host the layout is skipped and logged (orchestrator, 2026-10-25).
- **Activity levels (D36) apply everywhere.** The ceiling is the same at every level; richness, the salvaged missile and the guards change with activity.
- **What scales with the planet:**
  - **loot:** GT parts, plates and circuits up to the ceiling. Above UHV, the top of GregGuns' own content, the loot is GT material only;
  - **salvage:** the damaged or intact missile body follows the planet's tier, capped at the highest missile tier;
  - **guards:** spawner count and NPC equipment scale with the tier.
- **Placement:** each planet uses the D17 sparse grid with its own `enabled` flag and spacing. Planets are config-gated per dimension, so a server can keep silos to the Overworld.
- **Dependencies:** Galacticraft, GalaxySpace and Amun-Ra planets are all in the pack. A missing planet mod only removes its silos.

### D20 in detail

- **The tank is full at launch.** Every missile of a tier carries the same fuel (D19 fuel, fixed amount × config factor), whatever its target.
- **Burn grows with distance.** The missile uses a share of the fuel equal to the distance flown divided by its tier's maximum range.
- **Leftover fuel adds to the blast.** At impact, the unburned fraction `f = 1 − distance / maxRange` adds to the warhead's radius: `radius × (1 + bonusMax × f)`. `bonusMax` is a config value with a starting default of 0.25, so a point-blank strike is up to 25% bigger.
- **The minimum launch range still applies,** so firing at your own feet can't farm the bonus.
- **Tooltips and the launch GUI show the expected radius** for the current target, so the bonus is never hidden.

### D21 in detail

**Blocks and rewards.** Option A, as proposed:

| Block | Made by | Cleanup yields |
|---|---|---|
| **Irradiated earth**: dirt, stone, sand, gravel and cobble variants | Common. Converts ~20% of matching blocks in the rot radius | Mined as **irradiated rubble**. GT Centrifuge (HV): lead, thorium, uranium-238, a little uranium-235 and plutonium-239 |
| **Trinitite** | Sand within the inner fireball radius | Macerated and centrifuged: small radioactive dust yields |
| **Fallout goo** | Rare, about 1% of converted blocks, concentrated near ground zero. While goo remains in a chunk, that chunk's irradiated earth does not decay | Dissolved in nitric acid in a GT Chemical Reactor (LuV), then centrifuged: technetium, promethium, neptunium, plutonium-241, americium, curium, californium, plus krypton and xenon |
| Goo from ZPM+ warheads | The Ivy Mike analog | Adds trace fermium |

- Yields scale with warhead tier and with distance from ground zero.
- Antimatter and red matter leave no fallout.
- GT pollution is still added when it is enabled.

**Owner amendments:**
- **Surface only.** Conversion touches only the top solid block of each terrain column inside the rot radius, plus at most **2 blocks below it**. Caves and deep stone are never converted, so the reward is on the surface, not in a dig.
- **Any hazmat-capable armor protects.** This uses GT's `HazardProtection` against `RADIOLOGICAL`, exactly as [05-armor/hazard-protection](../gregguns-spec/05-armor/hazard-protection.md) already does: GT hazmat, GregGuns armor that declares radiological protection, the Hazmat enchantment, or any `IHazardProtector`. ICBM's per-item ratings are dropped; we aren't modeling fallout dose.
- **Exposure:** unprotected players standing on or near irradiated blocks get GT's radioactivity effect (`GTUtility.applyRadioactivity`).
- **Rubble and goo hurt while carried.** They are GT radioactive items, so an unprotected player carrying them takes radiation through GT's existing radioactive-item mechanism.
- **Hard caps, so the server never crashes.** Every number is config with a safe default:
  - A per-blast budget of converted blocks, applied over several ticks through the blast engine's shared edit budget.
  - **No spreading.** ICBM's radioactive blocks spread; ours don't. This is how the conversion budget stays an upper bound.
  - **No random ticking.** Decay is a per-chunk countdown, kept as a record per irradiated chunk. It reverts a bounded number of blocks per tick, server-wide.
  - Exposure is checked per player about once a second in a small radius, not per block.
  - A cap on goo blocks per blast.

### D23 in detail

- **Item:** one warhead item per casing tier, with NBT `{explosive, fill}`. The tooltip shows the explosive, the fill and the cap, plus the expected radius at that fill.
- **Filling:** how charges are added (a crafting recipe that adds one charge per craft up to the cap, or a GT machine step) is settled in the missiles sheet. Either way, NEI shows one example per warhead, not one per fill level.
- **Blast:** radius grows with fill up to the tier cap. The exact curve goes in the explosives sheet, and it may be sub-linear so that a full casing doesn't dwarf the next tier up. The D20 fuel bonus multiplies the result.

### D25 in detail

- **Chunks:** the missile holds a ticket for at most two chunks: the one it is in, and the next one along its path. On crossing a chunk boundary it loads the next and releases the one behind it.
- **Caps:** D11's limit on missiles in flight is set so that two chunks per missile stay within Forge's per-mod budget (`forgeChunkLoading.cfg`; 25 chunks per ticket by default).
- **Radar:** radar reads the live entity list, and the missile tracker keeps flight records for prediction (D14).
- **Not inherited:** ICBM-Classic's tracker bugs from [targeting-and-tracking](../icbm-spec/03-missiles/targeting-and-tracking.md), notably the shared landing ticket and the broken "unloaded chunk ahead" test.

### D28 in detail

- **Source order:**
  1. The 2021 MIT snapshot of ICBM-Classic (`2f807bf8`).
  2. Calclavia's LGPL 1.6.4 set.
  3. HBM (LGPL) and CC0 libraries, plus pack textures referenced at runtime.
  4. Our own generated assets, only as a last resort (D33).
- **Exceptions:**
  - The current ICBM 1.12 assets are never used ([07-visuals-audio](../icbm-spec/07-visuals-audio/index.md)).
  - Sounds of unknown origin must be re-sourced from freesound under their own license, or replaced with CC0 sounds.
- **Provenance:** every copied asset gets its author, source, commit and license in `docs/assets/CREDITS.md`, as 0006 already requires.
  - LGPL files live in a carve-out folder with their license text.
  - MIT files keep their notice in the third-party notices file.
- **Style:** copied assets are restyled to GT conventions only where 0006 needs it (for example, item icons in GT material colors). Keep restyling light, so the licensed-first approach stays fast.
- **Repo rules:** [CONTRIBUTING](../../CONTRIBUTING.md#assets-and-localization) records the third-party asset exception, and [0018](../design/0018-icbm-program.md) amends 0006 (2026-10-25). The licensing research itself stays out of the repo (owner, 2026-10-20), so each license is checked at its source before an asset is added.

### Fuel context (D19, D20, D22)

Context: how GTNH fuels rockets. The evidence is the SpaceRace quest line, Galacticraft `3.4.34-GTNH` `RecipeManagerGC.addUniversalRecipes`, and GT++ `RecipeLoaderRocketFuels`.

- **Four GT++ rocket fuels form an "or better" ladder.** Each is made in the GT++ Chemical Plant, gated by **casing tier** rather than voltage (every final step runs at 240–480 EU/t):

  | Fuel (fluid name) | Chemical Plant casing | Rocket tiers that need at least this |
  |---|---|---|
  | Cetane-Boosted Diesel (`nitrofuel`) or RP-1 (`rocketfuelmixb`) | 3, Stainless | T1–T2 (moon) |
  | Dense Hydrazine (`rocketfuelmixd`) | 4, Titanium | T3–T4 |
  | CN3H7O3, "Purple" (`rocketfuelmixc`) | 5, Tungstensteel | T5–T6 |
  | H8N4C2O4, "Green" (`rocketfuelmixa`) | 6 | T7–T8 |

  Rocket schematic chips by voltage, for comparison: T2 HV, T3 EV, T4 IV, T5–T6 LuV, T7 ZPM, T8 UV.
- **Enforcement.**
  - The GC fuel loader checks its tank against a whitelist, `RocketFuels`, keyed by rocket entity class. Galacticraft registers four lists for its rockets (t2, t4, t6, t8).
  - **Our missile classes can register in the same whitelist** with `RocketFuels.addFuel`, so the GC pad and loader enforce missile fuel tiers for free.
  - The rocket's own tank stores all fuels as generic GC `fuel`. The loader is the only gate.
- **ICBM itself uses no fuel.** Its launchers spend energy. Fuel is new GregGuns design that comes from D8.

## Open

None right now. New questions are added here as the GregGuns-side sheets raise them.
