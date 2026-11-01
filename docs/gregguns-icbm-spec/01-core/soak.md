# Soak runs (I7)

- **ICBM baseline:** ICBM-Classic has no soak tooling. Its known load bugs are in the per-area sheets: ICBM-02-06 and ICBM-02-90 (the blast tick budget), ICBM-03-21, -32 and -35 (missile chunk tickets), ICBM-05-10 (the EMP's block walk).
- **Decisions:** [D11, D25](../DECISIONS.md#decided) (one ticket per missile, a two-chunk window), [D332](../DECISIONS.md#decided) (the soak bar), [D333](../DECISIONS.md#decided) (what the soaks turned up)
- **Pack mods:** the whole GTNH 2.9.0-RC-2 server pack. Forge 10.13.4.1614 owns the chunk tickets.
- **Roadmap:** [ROADMAP-ICBM § I7](../../ROADMAP-ICBM.md#i7--hardening), the soak bullets and the soak exit box
- **Results:** [docs/testing/i7-soak.md](../../testing/i7-soak.md)

## What a soak is

A soak is a repeatable load on a real GTNH dedicated server with every GregGuns module on, started with `-Dgregguns.testHooks=true`. Each one is a `gregguns test` scenario or a driver mode, so anyone can rerun it with the same build.

The scenarios are opt-in: `gregguns test all` and prefix runs such as `icbm.` skip them, and they run only by exact name.

| Scenario | The burst |
|---|---|
| `icbm.soak.missiles` | Missiles up to the configured cap (`icbm.maxMissilesInFlight`, 20) launched in one tick: 40% T1, 30% T2 and 30% T3. Targets are 300 to 4,000 blocks away, all around, and warheads are at full fill. One more launch past the cap must be refused. |
| `icbm.soak.missiles_max` | The same with 100 missiles, the highest cap the config allows. The cap is raised for the run only. |
| `icbm.soak.blasts` | The blast queue's worth of large blasts at once on real terrain: one antimatter, three nuclear, four exothermic, four endothermic and four thermobaric (16, `icbm.maxActiveBlasts`). A 17th must be refused. |
| `icbm.soak.redmatter` | Three red matter bodies at full fill: one on the surface, one underground, and one in a crowd of 100 mobs and 200 item stacks. |
| `icbm.soak.emp` | A dense GT base, about 8,000 tile entities (1,024 charged battery buffers, tin cables and 8 multiblock controllers). Four EMP pulses at the largest tower radius and two EMP warheads go off in one tick. |
| `icbm.soak.salvo` | Five short missile flights. The play soak repeats it every 10 minutes. |

Each burst works in its own region, about 1.5 km from the others, so one burst's craters never sit under another.

Two runs come from the driver, `tools/soak/icbm_soak.py`:

- **play:**
  - three bots (`tools/soak/bot.py`) visit the silo complex sites `gregguns locate complex` lists, ring each one, and sweep fresh land further out;
  - one bot spends five minutes on the Moon now and then;
  - a salvo runs every 10 minutes.
  - Tickets, forced chunks and loaded chunks are counted with nobody online, before the bots join and after they leave.
- **restart:**
  - a salvo starts, and the server is stopped while its missiles fly;
  - the server is started again;
  - after a settle time, no GregGuns ticket, forced chunk or missile in flight may be left.

## How a soak scenario runs

`SoakScenario` runs these phases, in order:

1. **Setup.** The burst builds what it needs, a little each tick, with a limit of 6,000 ticks. Far regions are loaded with scenario holds, one site per tick.
2. **Settle.** 200 ticks.
3. **Baseline.** 600 ticks with nothing of the soak running.
4. **Burst.** Everything goes off in one tick, and the burst runs until it says it is done. It is done when:
   - every missile has ended and no blast runs;
   - for the blast soak, the fallout has also finished converting.

   A burst still running at its limit is a runaway: it is stopped, and the soak fails.
5. **Recovery.** Ticks are recorded until the last 600 are back near the baseline, for up to 10 minutes.

Every second of the burst logs a `SOAK <name> t=<s>s` line. It holds:

- the mean and longest tick of that second, and the TPS that mean allows (the MSPT and TPS curve);
- the burst's own state;
- a `SoakProbe` sample.

The end logs one `SOAKRESULT` line. The driver turns it into the result table.

**`SoakProbe` sample**, server-wide:

- the mean and longest of the last 100 ticks;
- the chunk tickets GregGuns holds, from Forge's ticket limit minus `ticketCountAvailableFor`;
- the chunks GregGuns tickets force, and the chunks every mod's tickets force (`getPersistentChunksFor`);
- the loaded chunks and loaded worlds;
- the entities: all of them, missiles and missiles off their pads, anti-gravitational flying blocks, and item entities;
- the blasts running, their pending blocks and the memory of their ray-cast cubes;
- the irradiated blocks waiting to decay;
- the heap in use.

`gregguns soakstat [label]` logs the same sample as a `SOAKSTAT label=<label>` line. It registers with the test hooks only, and its text is a machine format, not player text.

## The bar (D332)

The owner's bar is "playable": vanilla Minecraft never handles explosions well, so lag during a burst is accepted. `SoakVerdict` judges what the owner asked for:

| Check | Rule |
|---|---|
| No runaway | The burst ends inside its limit: 12 minutes for missiles, 20 for blasts, the red matter lifetime plus a minute, 3 minutes for the EMP. |
| Recovers | Within 10 minutes of the burst's end, the last 30 seconds are within 10 ms of the baseline. The comparison uses trimmed means (the slowest 5% of ticks left out), so a world save in one window and not the other does not decide it. |
| Playable | That recovered window is under 50 ms a tick (20 TPS). |
| No leaked ticket | The GregGuns tickets and the chunks they force are the same before the burst and after the recovery. |
| No crash | The server keeps running, and the scenario does not throw. |

A burst's own spikes are recorded (mean, p95, longest tick, the per-second curve, and the seconds until it is back at 20 TPS for good), never judged. Peak heap, entities, loaded chunks and tickets are recorded too.

Each scenario adds checks of its own:

- every missile launches, one past the cap is refused, and each missile forces at most two chunks;
- every blast is accepted, one past the queue is refused, and the ray-cast cubes and flying blocks are all freed;
- each EMP pulse stays inside `icbm.empMaxDevices` and reaches the base.

## Integration points

### Forge: chunk tickets
- **Pack config:** `config/forgeChunkLoading.cfg` ships the defaults: `maximumTicketCount=200` and `maximumChunksPerTicket=25` per mod.
- **Source:** Forge 1.7.10-10.13.4.1614 `ForgeChunkManager`:
  - `setForcedChunkLoadingCallback` keeps one callback per mod, so a second one replaces the first;
  - `ticketCountAvailableFor(mod, world)` and `getPersistentChunksFor(world)` are what the probe reads.
- **GregGuns does:**
  - A mod has one loading callback, and GregGuns' is `MissileTickets`' (D333). A missile in flight adopts its ticket; every other GregGuns ticket loaded from disk is released, scenario holds included.
  - `ScenarioChunks` no longer registers its own. On a test server, its callback replaced the missiles' one.
- **Pack-side change:** none.

### Forge: a scenario's fake player and FML channels
- **Source:** Forge 1.7.10-10.13.4.1614 `FMLOutboundHandler.OutboundTarget.PLAYER` reads the player's dispatcher from `playerNetServerHandler.netManager.channel()`. It sends to nobody when the channel has no dispatcher, and it throws when there is no channel at all.
- **GregGuns does:** every scenario's fake player gets `QuietConnection`: a `NetworkManager` on an open `EmbeddedChannel` whose only outbound handler drops every write (D333).
- **Pack-side change:** none.

## Requirements

- **GGS-ICBM-01-01.** Every soak is a named, opt-in scenario or driver mode that reruns unchanged on any server with the test hooks.
- **GGS-ICBM-01-02.** A soak fails on a runaway burst, on a server that does not come back within 10 minutes, and on any changed count of GregGuns tickets or forced chunks. A spike during the burst alone never fails it.
- **GGS-ICBM-01-03.** Each soak records:
  - its tick curve;
  - its peak heap, entities, loaded chunks, tickets and forced chunks;
  - its counts before and after.
- **GGS-ICBM-01-04.** A server restarted while missiles fly ends with no GregGuns ticket, forced chunk or missile in flight once they land.
- **GGS-ICBM-01-05.** A scenario's fake player never makes another mod log an error by joining the world.

## Tests

- Unit: `SoakCurveTest` (buckets, the trimmed mean, the back-at-20-TPS point) and `SoakVerdictTest` (each failure of the bar, and that burst spikes are not judged).
- Scenarios (opt-in): `icbm.soak.missiles`, `icbm.soak.missiles_max`, `icbm.soak.blasts`, `icbm.soak.redmatter`, `icbm.soak.emp` and `icbm.soak.salvo`.
- Driver: `icbm_soak.py play` and `icbm_soak.py restart`, on the real pack.
- Real pack: `npc.soak` logs no FML channel error with Witchery loaded (GGS-ICBM-01-05).

## Open questions

None.
