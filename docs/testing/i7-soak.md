# I7 soak: ICBM on a real GTNH 2.9.0-RC-2 dedicated server

Run on 2026-10-31 (server time) on the Linux test box. Real hosts and paths stay out of the repo. This closes the I7 soak bullets of [ROADMAP-ICBM § I7](../ROADMAP-ICBM.md#i7--hardening). How the soaks work and the bar they are judged by: [soak.md](../gregguns-icbm-spec/01-core/soak.md) (D332). What they turned up: D333.

## Verdict

**Every soak meets the owner's bar.**
- Nothing crashed.
- No burst ran away.
- The server came back near its baseline after every burst: at once for missiles, red matter and the EMP, and 8 seconds after the last crater for sixteen large blasts.
- No GregGuns chunk ticket or forced chunk was left, including after 2.5 hours of play with silo complexes generating.

The soaks found three bugs, all fixed on this branch:
1. **A missile in flight across a restart hung in the air and kept its chunk ticket for good.** Forge saves a ticket without its chunks, so the adopted ticket forced nothing and the missile never ticked again. It now forces its window again when it adopts the ticket. This fix also freed the three missiles a first restart run had stranded.
2. **On test servers, a second chunk-loading callback replaced the missiles' one.** GregGuns now has one callback.
3. **Scenario fake players made Witchery log FML error pairs.** It was 32 lines per `npc.soak` run, and 0 after the fix.

## Setup

| Item | Value |
|---|---|
| Pack | GT New Horizons 2.9.0-RC-2 public server pack, unzipped as is (216 jars). `level-type=rwg`, Hard, `view-distance=8` |
| GregGuns | branch `icbm/i7-p2`, every module on (`modules.icbm=true`), `-Dgregguns.testHooks=true`. Config defaults otherwise: 20 missiles in flight, 16 active blasts, a 12 ms blast budget, a 64-device EMP cap |
| JVM | Temurin 21, `-Xms6G -Xmx6G` (the pack's start script), G1, the pack's `java9args.txt`, `-Xlog:gc*` |
| Servers | Three pack servers side by side: the scenario soaks, the long play soak, and the restart check, each on its own port and in its own folder |
| Driver | `tools/soak/icbm_soak.py`, with the bots from `tools/soak/bot.py` |

Rerun:

```sh
# a pack server started with -Dgregguns.testHooks=true, stdin a FIFO (console.fifo), stdout to a log
python3 -I tools/soak/icbm_soak.py --root <rig> --log <log> --out <out> --timeout 2400 \
    scenarios icbm.soak.missiles icbm.soak.blasts icbm.soak.redmatter icbm.soak.emp icbm.soak.missiles_max
python3 -I tools/soak/icbm_soak.py --root <rig> --log <log> --out <out> --port <port> play 9000
python3 -I tools/soak/icbm_soak.py --root <rig> --log <log> --log2 <log after restart> \
    --start "<command that starts the server again>" --out <out> restart icbm.soak.salvo 70 150
```

## Scenario soaks

These are tick times, in ms, on a fresh world. The baseline and recovery figures are trimmed means: the slowest 5% of ticks are left out (D332).

| Soak | Verdict | Baseline ms | Burst mean / p95 / max ms | Burst s (server s) | 20 TPS again after s | First 30 s after ms | Back near baseline after s (ms) | Tickets before -> after | Peak tickets / forced chunks | Peak heap MB | Peak entities |
|---|---|---|---|---|---|---|---|---|---|---|---|
| icbm.soak.missiles | PASS | 5.10 | 6.90 / 15.44 / 435.59 | 130.05 (17.94) | 0.00 | 2.44 | 0.00 (2.44) | 2->2 | 22 / 89 | 4336 | 420 |
| icbm.soak.blasts | PASS | 11.21 | 69.20 / 132.82 / 325.75 | 51.00 (70.59) | 51.00 | 32.82 | 8.00 (21.11) | 47->47 | 47 / 1013 | 5204 | 3328 |
| icbm.soak.redmatter | PASS | 3.81 | 3.80 / 5.14 / 68.70 | 25.85 (1.96) | 0.00 | 2.44 | 0.00 (2.44) | 7->7 | 7 / 172 | 3894 | 455 |
| icbm.soak.emp | PASS | 4.84 | 5.25 / 5.25 / 5.25 | 0.05 (0.01) | 0.00 | 4.86 | 0.00 (4.86) | 11->11 | 11 / 265 | 4574 | 240 |
| icbm.soak.missiles_max | PASS | 2.52 | 22.03 / 62.12 / 427.78 | 130.75 (57.60) | 29.00 | 2.29 | 0.00 (2.29) | 2->2 | 102 / 248 | 5031 | 1627 |

The tickets before and after are the scenario's own chunk holds. They are released when the scenario ends.

Peak process memory (RSS, 5-second samples): 6.5 GB for missiles, 6.8 GB for blasts, red matter and EMP, and 7.1 GB for 100 missiles. Across the whole run, G1 made 70 young collections, with the longest pause at 46 ms, and no full collection.

### Missiles in flight (20, then 100)

- **20 missiles** (8 T1, 6 T2, 6 T3):
  - All 20 launched, and the 21st was refused at the cap.
  - Every missile ended, and all 20 blasts were accepted.
  - The 20 missiles held 20 tickets and 40 forced chunks, at most two each.
  - The mean tick during the flights was 7 ms. The longest tick was 436 ms, a missile's first look into a fresh RWG chunk.
- **100 missiles**, the highest cap the config allows (40 T1, 30 T2, 30 T3):
  - The first 20 seconds ran at 36 to 58 ms a tick, with 100 missiles generating terrain under their windows.
  - The server was back at 20 TPS for good after 29 s.
  - Loaded chunks peaked at 5,329 and fell back to 377 after the run.
  - The loaded chunks a flight passes through unload by themselves: there is no loaded-chunk leak.
  - Every ticket came back.

### Large blasts (16 at once)

- **The blasts:** one antimatter, three nuclear, four exothermic, four endothermic and four thermobaric, at full fill, on RWG terrain about 220 blocks apart. All 16 were accepted, and the 17th was refused (`icbm.maxActiveBlasts`).
- **The burst:** it took 51 s of game time and 71 s of server time, about 14 TPS on average. The tick rose to 120 to 140 ms in its last seconds, while the nuclear craters finished and their fallout started converting.
- **The recovery:** the first 30 s after the burst were 33 ms. The server was back within 10 ms of its baseline 8 s after the burst ended.
- **Freed:** the ray-cast cubes and the flying blocks.
- **Item entities:** about 2,600 lay around the sites afterward. They are not GregGuns drops (`icbm.blastDrops` is off). They are what the pack's blocks drop when their support goes: Biomes O' Plenty saplings and flowers, and mineshaft rails. They despawn in five minutes.
- **The baseline:** 11 ms, higher than the other soaks. The holds keep 1,013 chunks forced.

### Red matter (3 bodies)

- The surface, underground and crowd bodies all ended inside the 60-second lifetime, at 26 s once fully grown and idle.
- The crowd body took 294 of its 300 mobs and items.
- The server never left 20 TPS.

### EMP over a dense base

- **The base:** 8,200 GT tile entities, with 1,024 charged MV battery buffers among them.
- **The four pulses** (radius 100):
  - each hit exactly 64 devices, the cap, and skipped 968 more;
  - each walked about 8,300 tiles in 2.6 to 7.9 ms.
- **The two EMP warheads** were accepted.
- **Drained:** 379 buffers. The pulses overlap.
- **The whole burst** took one tick of 5.3 ms.

## Restart with missiles in the air

`icbm_soak.py restart icbm.soak.salvo 70 150` stops the server 70 s into a salvo and starts it again.

| Build | In flight at the stop | Just after the restart | 150 s later |
|---|---|---|---|
| Before the fix | 5 flying, 7 tickets | 3 flying, 3 tickets, **0 forced chunks** | 0 flying (their chunks unloaded), **3 tickets left**, no blast |
| After the fix | 3 flying, 5 tickets | 1 flying, 1 ticket | 0 flying, 0 tickets, 0 forced chunks. The missile landed and blew up |

The missiles that disappear from the count between the stop and the restart landed during the 5-second save before the stop. Their blasts are in the log.

The first boot of the fixed build loaded the world the failed run had left. Its three stranded missiles forced their windows on load, flew on and blew up. That world's tickets went from 3 to 0 within 150 s.

## Long play soak

`icbm_soak.py play 9000`: 2.5 hours on a fresh world, with three bots in creative.

- **What the bots did:**
  - Alice and Bob toured the eight silo complex sites `gregguns locate complex` listed. They spent 2 minutes at each, then ringed it with four stops 160 blocks out, then swept fresh land further out each round.
  - Carol did the same, with five minutes on the Moon now and then.
  - A missile salvo (`icbm.soak.salvo`) ran every 10 minutes, 15 in all.
- **Traffic:** 340,000 chunk packets reached the bots, about 1.7 GB of chunk data. No bot was kicked or lost its connection.
- **Complexes:** silo complex sites decided while the bots moved. The log names 6 built under a village (sunk deeper, D302) and 7 skipped (terrain, biome or water).

| Play soak | Value |
|---|---|
| `forge tps` overall, every 30-second sample | 20.000 TPS in all of them |
| Mean tick, 100-tick means: median / max | 11.88 / 45.67 ms |
| `Can't keep up` warnings in 2.5 h | 2 |
| GregGuns tickets with nobody online, before -> after | 0 -> 0 |
| Forced chunks with nobody online, before -> after | 0 -> 0 |
| Loaded chunks with nobody online, before -> after | 445 -> 444 |
| Peak tickets / forced chunks while playing (a salvo in the air) | 7 / 59 |
| Peak heap / process RSS | 5.0 GB / 7.8 GB |
| G1: full collections / longest pause | 0 / 50 ms |

**The salvos' own verdicts are informational here.** 13 passed and 2 failed, both on "did not recover". The play server's baseline wandered from 6 to 27 ms as the bots generated terrain, so a 30-second baseline is no reference for the next 30 seconds. On top of that, this server ran the build from before the bounded recovery wait. Every salvo left its tickets as it found them.

This server ran the branch at its first soak commit. The later fixes do not touch what this run exercises: the restart adoption acts only at world load, and the rest is test code.

## Fake players and Witchery (log noise)

`npc.soak` on the pack server:
- **Before:** the run logged 25 `SimpleChannelHandlerWrapper exception` and 7 `FMLIndexedMessageCodec exception caught` errors. The cause was a `NullPointerException` in FML's per-player target, on the fake player's channel, which was `null`.
- **After:** 0. The scenario passes either way.

All scenario fake players now get `QuietConnection` (D333).

## Mess galley frame rate (I6b note)

- **K1's figure (I6b):** a furnished mess galley ran at about 19 FPS against about 42 in a furnished cafeteria (software GL), with low confidence. In both clients the mess galley was the first room measured, a few seconds after a teleport into the complex.
- **Re-measured on the real pack:**
  - **Setup:** one client, software GL, an operational complex generated at cell 0,1. The client got a warm-up of a minute in each room first, then the rooms were measured in alternating order (cafeteria, mess, mess, cafeteria, cafeteria, mess), 10 s each after a 20-second settle.
  - **Mess galley:** 13.3 FPS (13.5, 13.8, 12.7).
  - **Cafeteria:** 17.2 FPS (15.3, 18.2, 18.1). The first cafeteria sample ran while a Gradle build loaded the box.
- **Reading:**
  - This mess galley is 59 by 50 blocks, about 6.5 times the cafeteria's floor (30 by 15), with that many more tables and chairs in view.
  - It ran at 0.77 of the cafeteria's rate, which is what its size predicts.
  - The two rooms share one blueprint (`MessGalley.dining`) and one style group.
  - There is no outlier left to explain. K1's halved rate fits a client still meshing the complex's chunks.
- **No change.** Shots: `docs/assets/previews/i7/p2/` (git-ignored).

## Files

- **On the box:**
  - the server logs;
  - the GC logs;
  - the driver output: `metrics.csv`, `stats.csv`, `tps.csv`, `summary.md`, the bot logs and `restart.txt`.

  They stay private and are removed with the box folder.
- **In the repo:** the driver and the scenarios.
