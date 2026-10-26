# M9 soak: GregGuns on a real GTNH 2.9.0-RC-2 dedicated server

Date of the run: 2026-10-25 (server time), on the Linux test box (`<test-box>`; real hosts and paths stay out of the repo). This closes the M9 item "multiplayer and performance soak tests on the real pack" as far as a headless dedicated server allows. Client-side checks (rendering, NEI, GuideNH pages, the HUD) are not covered; they stay with the [beta client pass](beta-client-pass.md).

## Verdict

- The mod boots in the real pack with no error and no warning attributable to GregGuns, all compat hooks log sanely, and the hidden questbook key is off by default.
- Three bots joined for 55 minutes of server time and were never kicked. The server held **20.00 TPS in every 30-second sample** (113 samples). One 2.2 s tick stall (not GregGuns, see "Findings").
- GregGuns costs almost nothing: **0.15 % self time** and 1.5 % inclusive (NPC AI) of the server thread; structure generation is 0.09 %.
- Scenario suite in the pack: 415 passed, 4 failed, 10 skipped on a fresh world. The 4 failures are test-environment sensitivity, not mod bugs (two are fixed in this branch, details below).
- Nothing in GregGuns needs a hot fix. Items to watch are at the end.

## Setup

| Item | Value |
|---|---|
| Pack | GT New Horizons 2.9.0-RC-2, public **server pack** `GT_New_Horizons_2.9.0-RC-2_Server_Java_17-26.zip` (from the GTNH downloads site, `ServerPacks/betas/`), unzipped as is: 216 jars in `mods/`, 295 mods in the FML handshake |
| GregGuns | built from master `d02c26da` in a worktree (`./gradlew build --max-workers=2 -Dorg.gradle.jvmargs=-Xmx2g` on the laptop), reobfuscated jar `0.3.2-beta` copied into `mods/gregguns.jar`. The later fixes (below) were rebuilt and re-run the same way |
| JVM | Temurin 21.0.12.1, `-Xms6G -Xmx6G` (the pack's own start script values), G1, pack `java9args.txt`, plus `-Dgregguns.testHooks=true` ([README § Testing in a real pack](README.md#testing-in-a-real-pack)) |
| World | pack defaults: `level-type=rwg`, difficulty 3 (Hard), `view-distance=8`, `online-mode=false` for the bots, own port, EULA flag set in the box copy |
| Config | GregGuns defaults, except `debug.recipeDump=true` for the dump |
| Profiling | the pack has no spark or sampler, so: `forge tps` every 30 s, `jstat -gcutil`, JDK Flight Recorder (`settings=profile`) and `-Xlog:gc*` |
| Bots | `tools/soak/bot.py`: a stdlib Python 1.7.10 (protocol 5) client that completes the FML handshake with the server's own mod list (read from the status ping), answers keep-alives, receives chunks and sends `/tp` and `/tpl` chat as an op. Needed because `build/mp_check.py` (real clients) is not in the repo and cannot run headlessly without a display, and no ready-made Node or Python Minecraft library does the FML 1.7.10 handshake |
| Driver | `tools/soak/soak.py` (schedule, console commands, metrics), `tools/soak/run_scenarios.sh`, `tools/soak/jfr_summary.py` and `tools/soak/jfr_gregguns.py` (JFR digest) |

Boot took 55 s (`Done (5.0 s)` for the world). Commands, in order:

```sh
# on the box, in a private folder: unzip the server pack, put the jar in mods/, set eula/port/online-mode
java <pack flags> -Dgregguns.testHooks=true -XX:StartFlightRecording=... -Xlog:gc*:file=gc.log -jar lwjgl3ify-forgePatches.jar nogui < console.fifo
gregguns test list ; gregguns test all ; gregguns test npc.soak ; gregguns test <one scenario>
SOAK_ROOT=<folder> python3 tools/soak/soak.py 3300     # dumprecipes, 3 bots, sweeps, loiter, 3 x npc.soak
python -I tools/recipe-clash/clash.py recipes.json.gz report.md [--ignore-counts --ours-fluid ...]
```

## 1. Boot

Grep of the full FML log (`WARN`, `ERROR`, anything mentioning gregguns):

- **No GregGuns warning or error at boot or during the soak.** The GregGuns lines are all `INFO`: loading, the `gregguns` channel (8 client-bound, 5 server-bound packets), "Module icbm is off", the three retired MTE placeholders, and `Registered /gregguns (dev environment or -Dgregguns.testHooks), 500 scenarios`.
- No crash report, no mixin or class-loading complaint about GregGuns, no missing-registry message.
- `Duplicate Enum found for GREGGUNS_*` (33 lines, `INFO`, tag `FML/gregguns`) comes from the pack's enum extension for our armor materials; 5 other, non-GregGuns lines of the same kind exist. Harmless noise.
- **Questbook key off by default:** `config/gregguns.cfg` has `questbook { B:install=false }`; no `Installed/updated GregGuns quest line` line, no GregGuns text in BetterQuesting's `QuestDatabase.json` (3,813 default quests). With the key turned on (extra check): `Installed/updated GregGuns quest line: 128 quests (128 added, 0 updated)`, the database ends with 3,941 quests (3,813 + 128) and 47 quest lines (one is ours, 121 quests plus 7 hidden triggers). A second boot re-installs and the database stays at 3,941, so no duplicates. The second boot also logs "128 added", not "128 updated": BetterQuesting reloads the pack's default database on each start, so the count is right for what it sees (watch item W3).
- **Compat hooks** (all in-memory, as documented):
  - `Backhand: 44 GregGuns guns added to its offhand lists`
  - `KamikazeConfig.butcherItems: GregGuns entries added` (AngerMod)
  - `MobHandler.mobBlacklist: GregGuns entries added` (Mobs Info)
  - `Thaumcraft: scan aspects registered for 19 GregGuns NPCs`
  - GuideNH and NEI have no server code in GregGuns (`client/nei` is client-only), so nothing logs on a server; they stay with the client pass.
- `/help` prints the raw key `gregguns.command.usage` for our command on the console (the lang file is not applied to the console text). Cosmetic (W5).

## 2. Scenario suite in the real pack

`gregguns test all` on the fresh world: **415 passed, 4 failed, 10 skipped** (500 scenarios are registered; the rest are the opt-in `mp.*`/showcase/GUI scenarios that need a real client, and `npc.soak`, run separately). The 10 skips are the ones the README announces: they need the dev-only probe tool gun or test NPC, which the pack jar does not register. The suite takes about 11 minutes in the pack.

The two Ross128 scenarios pass in the real pack with the real BartWorks providers: `worldgen.ross128b_structures` and `worldgen.ross128ba_structures` (dimension gate, a fresh square generates the test structure with no `PopulateGuard` violation or dropped write, rolled loot ceilings seen: `{2=6, 3=3, 4=1, 5=1, 7=1, 9=1}`).

The four failures, analyzed:

| Scenario | Cause | Status |
|---|---|---|
| `gun.nuke_launcher.killzone` | The pack rescales a spawned mob's maximum health and refills it by difficulty (measured: Hard 2200, so a full boss ends at 1100; Normal 2000 leaves the expected 1000; Easy ended at 900). The kill zone takes half of the real maximum (correct); the scenario hard-coded "1000 left". Failed on Easy and Hard, passed on Normal. | **Fixed** in this branch (expects half the maximum read at the impact). Passes on Easy, Normal and Hard in the pack. |
| `worldgen.no_cascade` | The nest phase compared the full loaded-chunk set. Other mods loaded 1 to 4 unrelated chunks thousands of chunks from the square while it ran (`PopulateGuard` had 0 violations and 0 dropped writes, and the structures phase matched). | **Fixed** (compare chunks in or within 3 of the square, note the far ones, print the nest diff); pure helper `CascadeScenarios.nearSquare` has a unit test. Passes in the pack with the note "4 chunks far from the squares ... ignored". |
| `utility.rampart_assembles`, `utility.rampart_break_hatch` | Stray `Sapling` item entities (Biomes O' Plenty leaf decay) in the arena, because the arena landed in a forest. Both pass when run alone. | Not fixed (flaky by biome; W1). |

A second full run later, in the lived-in world (after the soak, with NPCs and hostile mobs around spawn), failed four different scenarios that flip between runs: `grenade.fuse`, `grenade.protected`, `gun.alien_blaster.hit` (extra or missing damage from other entities near the arena) and `machines.repair.gantry.run` (item entities left). They passed on the fresh world and pass or fail at random when re-run alone, so they are arena interference, not regressions. Use a fresh world for the suite (W1).

`npc.soak` (in the pack, three runs during the soak; numbers in section 3).

### Recipe dump and clash review

`/gregguns dumprecipes` wrote **248,080 rows** (209 ours, 247,871 pack) in about a minute. `tools/recipe-clash/clash.py` on it: exact 0, shadow 0, grid 0, shapeless 0, duplicate 0, **subset 1** (the accepted Bacterial Vat row, verified in game before), superset 0; with `--ignore-counts --ours-fluid fluid:escherichiakolifluid=fluid:biomass` subset 20, the same 20 count-only rows as the v4 review in [recipe-clash-beta.md](recipe-clash-beta.md). No new clash with this pack build.

## 3. Soak

**Load.** Three bots (`Alice`, `Bob`, `Carol`, op, creative) for 3,300 s (55 min), all connected the whole time; a real Forge server with the full pack, normal natural spawning, `view-distance=8`.

| Phase | Time | What |
|---|---|---|
| Overworld sweep | 0 to ~10 min | a 12 by 12 grid, 192 blocks apart (about 2,100 blocks across, roughly 20,000 chunks generated), 12 s per stop, three bots in parallel |
| Ross sweep | ~10 to ~17 min | Bob on Ross128b (dimension 64), Carol on Ross128ba (63), 24 stops each; Alice kept sweeping the overworld |
| Loiter | ~17 to 55 min | the bots hop every 90 s between eight revisited sites in the overworld; `npc.soak` ran at 25, 35 and 45 min (100 NPCs each, plus natural spawns) |

Chunks sent per bot: Alice 115 MB, Bob 207 MB, Carol 205 MB of chunk data (compressed); the world ended at 331 MB. No chunk, packet or entity error on any bot, no disconnect.

**TPS and tick time** (`forge tps`, 30 s means, 113 samples):

| | median | p95 | max |
|---|---|---|---|
| Overall mean tick time | 5.7 ms | 27.0 ms | 31.9 ms |
| Overworld | 4.5 ms | 21.8 ms | 29.8 ms |
| Ross128b | 0.04 ms | 1.9 ms | 11.9 ms |
| Ross128ba | 0.01 ms | 7.5 ms | 9.6 ms |

- **Mean TPS was 20.000 in all 113 samples** (limit 50 ms per tick).
- By phase: overworld sweep (fresh chunks all the time) mean 24.1 ms; Ross sweep 11.1 ms; loiter 6.2 ms.
- One `Can't keep up!` warning in 55 minutes: a single 2,240 ms tick at 23:18:05 (skipping 44 ticks). It coincided with a bot hop and the first `npc.soak`, which therefore reports `max 2200.87 ms`. Not GC (below); the JFR samples around it are chunk loading, lighting and vanilla world ticking, nothing from GregGuns. A one-off, not reproduced in the next two `npc.soak` runs (max 189 ms and 172 ms).

**`npc.soak`** (PASS each time): baseline mean 22.0 / 9.8 / 7.1 ms; under load mean 12.4 / 7.4 / 7.3 ms, p95 16.7 / 14.2 / 12.8 ms; 100 NPCs spawned, 37 / 32 / 39 alive at the end (the rest killed each other or by fall damage; natural strays are swept). The first run was during the sweep-to-loiter hand-over, hence the higher baseline. On the fresh world before the soak: baseline mean 3.0 ms, load mean 3.4 ms, p95 5.6 ms, max 69 ms.

**Memory and GC** (`jstat` every 30 s, G1, 6 GB heap):

- Process RSS 6.84 GB at start, 7.21 GB at the end (heap fixed at 6 GB, the rest is metaspace, code cache and GC structures; native memory tracking: 7.16 GB committed).
- Old generation 56.7 % at the start, 47.9 % at the end: no growth, no leak visible in 55 min.
- 42 young collections in the soak (25 to 67), 23 concurrent cycles, **0 full GCs**, 0.59 s total GC time (0.018 % of the run), **longest pause 57.5 ms, p95 17.0 ms, median under that**.

**Where the time goes** (Flight Recorder, 55,739 samples of the server thread):

| Package (inclusive) | Share |
|---|---|
| vanilla `net.minecraft` (chunk generation, lighting, ticking) | ~100 % (it is the root of every stack) |
| Realistic World Gen (`rwg.*`) | 21 % |
| ArchaicFix and Hodgepodge (`org.embeddedt`, `com.mitchej123`; they hook vanilla, so these overlap the rows above) | 27 % / 42 % |
| Biomes O' Plenty | 14 % |
| EtFuturum | 8 % |
| BartWorks Ross128 providers (`bwcrossmod.*`) | 6 % |
| GregTech | 4 % |
| **GregGuns, inclusive** | **7.8 %** |
| of which inside the Ross128 provider wrapper | 6.0 % (nearly all of it is BartWorks' own `provideChunk`/`populate` underneath; only 7 samples are our structure generator) |
| GregGuns, own (leaf) frames | **0.15 %** (NPC cap counting and names, a few ray casts) |
| GregGuns NPC code, inclusive | 1.5 % |
| GregGuns structure generator, populator and stamper, inclusive | 0.09 % |

The top leaf methods are vanilla block and light access (`World.getBlock`, `NibbleArray.get`), fastutil chunk map lookups and RWG/BOP decoration. GT and AE2 (`CompassService`) show up under 2 %. The dumper and scenario classes appear only while they run.

**Structures and Ross128.** Plan records in `gregguns_structures.dat` after the soak (placed / rejected for terrain, biome or water): overworld 59 / 46, Ross128b 16 / 10, Ross128ba 68 / 5. The `Ross128ProviderHook` logged `Structures: hooked chunk population of dimension 64` and `63` at world load (the BartWorks ids from `bartworks.cfg`: `ross128BID=64`, `ross128BAID=63`), the wrapper ran on every Ross chunk the bots forced, and no exception came from it.

**Cascading worldgen.** No ArchaicFix or vanilla cascading-generation message appears in the log (the only matches for "cascad" are mixin method names). `worldgen.no_cascade` passes in the pack (structures phase: loaded set identical to the control; nest phase: identical after ignoring the far chunks).

**GregGuns log noise during the soak:** none at `WARN` or `ERROR` outside scenario runs. Under `npc.soak` the pack logs 177 `SimpleChannelHandlerWrapper`/`FMLIndexedMessageCodec` NullPointerExceptions per run from Witchery (`Shapeshift.initCurrentShift` on `EntityJoinWorldEvent`): the scenario spawns a connectionless `FakePlayer` (it has no netty channel). Test-harness noise only; it needs `testHooks` and cannot happen in play (W2). Other log noise is the pack's (Forestry/Railcraft item lookups, "Fetching addPacket for removed entity" 11 times, "Unable to find spawn biome", lootgames signature).

## 4. Multiplayer

Feasible and done headlessly: the stdlib bot (`tools/soak/bot.py`) speaks protocol 5, does the FML handshake with the server's own mod list (including the `GregGuns` network check) and joins as a real player. Three players at once: login, teleports between the overworld and two Galacticraft dimensions, 55 minutes of chunk streaming with NPC and structure activity, keep-alives, clean disconnect. This proves the join path, the `gregguns` channel registration and server-side load. It does **not** prove client behavior: no rendering, HUD, GUI, prediction (the `AUTOTEST` signals need the real client), shot sync between two clients (`mp.sync`) or the version check from a client jar. Those remain with the client pass; the `mp_check.py` orchestrator is still not in the repo.

## Findings and watch list

Fixed in this branch (separate commits, scenario code only, mod behavior unchanged):

1. `gun.nuke_launcher.killzone` expected a fixed 1000 health; the pack rescales boss-class mob health by difficulty (see section 2).
2. `worldgen.no_cascade` compared chunks other mods loaded far from the square.

To fix or watch:

- **W1, scenario flakiness in a lived-in or forested world.** `utility.rampart_*` fail on leaf-decay saplings in forest arenas; `grenade.*`, `gun.alien_blaster.hit` and `machines.repair.gantry.run` flip when hostile NPCs or other mobs are near the arena. Run the suite on a fresh world. A follow-up could make the scenario base clear or count only its own item entities and park the arena away from spawn.
- **W2, Witchery NPE storm under `npc.soak` in the pack.** Cosmetic, test-only. A fix would give the scenario's fake player a real `NetworkManager` channel or run the scenario with Witchery's join handler guarded; not worth it for a dev flag.
- **W3, installer count on repeat boots.** The log says "128 added" every boot because BetterQuesting reloads its default database each start; the database is correct (no duplicates). Reword the log if "updated" is wanted.
- **W4, the Ross128 wrapper cost is BartWorks' own generation** (6 % of the server thread while two players pull Ross chunks); there is no GregGuns overhead to tune there.
- **W5, `/gregguns` usage on the console** prints the raw key `gregguns.command.usage`; players see the translated text. Use `ChatComponentTranslation` for `getCommandUsage` too or a plain string.
- **W6, the single 2.2 s stall** under bot teleports is not explained by GregGuns; if it shows up for real players, take a thread dump during the stall (JFR could not name a culprit; the window is plain chunk loading and lighting).
- Not covered: client rendering and HUD, real-client multiplayer sync, GuideNH/NEI hooks, 4 or more simultaneous real players, a multi-hour run, `view-distance` above 8, and the shader packs.

## Files and clean-up

The box folder (private test folder, about 3 GB: pack server, scenario world, soak world, two JFR files, logs, recipe dump, a user-space Node tarball that turned out unnecessary) is left in place; a final delete needs the owner's go-ahead. Everything this run needs again is in `tools/soak/`.
