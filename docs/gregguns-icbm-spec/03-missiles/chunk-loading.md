# Chunk loading

- **ICBM baseline:** [targeting-and-tracking](../../icbm-spec/03-missiles/targeting-and-tracking.md)
- **Design records:** none
- **Decisions:** [D11](../DECISIONS.md), [D25](../DECISIONS.md#d25-in-detail)
- **Pack mods:** Forge (`ForgeChunkManager`, pack config `forgeChunkLoading.cfg`)
- **Roadmap:** [I2](../../ROADMAP-ICBM.md#i2--missiles-and-the-galacticraft-pad-t1t2)

## What changes from ICBM

ICBM simulates a missile with a tracker while its chunk is unloaded, with one shared landing ticket and a "chunk ahead" test that never fires toward lower coordinates. GregGuns keeps the missile a real entity and loads only the two chunks it needs.

| ICBM bug | How it is avoided |
|---|---|
| ICBM-03-21 chunk-ahead test skips negative directions | The next chunk is found by a heading vector, with floor shifts, so every direction works |
| ICBM-03-32 chunks compared by reference | Chunks are compared by value (`ChunkWindow.same`, and `ChunkCoordIntPair.equals` for the tickets) |
| ICBM-03-33 unforce loop skips entries; all landings share one ticket | One ticket per missile; stale chunks are unforced from a copy of the list |
| ICBM-03-35 negative coordinates truncate to the wrong chunk | `floorChunk` floors before shifting |
| ICBM-03-30 chunk-unload simulation covered only one logic | There is no simulation; the missile holds its own chunks |
| ICBM-03-38 pre-load timer not saved | There is no pre-load; tickets are re-requested by the entity |

## The window

- The ticket is requested in `launch`, with the pad's chunk, so a refusal is a refused launch. Each tick of the flight the missile asks `ChunkWindow.wanted(x, z, vx, vz)`: its own chunk, and the chunk that a step of 16 blocks along its horizontal heading lands in, when that differs. **Never more than two chunks.**
- `MissileTickets.update` holds them on **one** Forge ticket (type `ENTITY`, bound to the missile). It forces the new chunk, unforces the one behind it, and loads the new chunk now, so the ray cast at the next step sees real blocks.
- If Forge refuses a ticket (the mod's budget is used up) `launch` returns `NO_TICKET` and the missile stays on the pad (one warning is logged for that missile, not one per tick); the in-flight cap keeps this from happening in normal play: 20 missiles hold at most 40 chunks on 20 tickets, against Forge's per-mod default of 200 tickets and 25 chunks per ticket.

## When tickets are released

| Event | How |
|---|---|
| Impact | `detonateAt` releases the ticket before the blast starts |
| Timeout | the timeout path is `detonateAt` |
| Shot down | the same |
| Disarmed, or any other `setDead` | `setDead` releases it |
| Unload | Forge saves the entity ticket and, when the world loads, re-binds it to the missile and loads the missile's chunk before our loading callback runs. The callback hands the ticket back to a live missile that is in flight (`MissileTickets.adopt`; its next step trims it to the window) and releases every other ticket, such as one whose entity is gone. A flight therefore resumes where it was after a restart without a player nearby (its position is a function of its saved tick) |

## Requirements

- **GGS-ICBM-03-60** A flying missile holds at most two chunks and one ticket, at every tick.
- **GGS-ICBM-03-61** The wanted chunks include the next chunk on the heading for every heading, and negative coordinates pick the right chunk.
- **GGS-ICBM-03-62** All tickets are released on impact, on timeout, when shot down, on disarm and by the load callback for any ticket that is not a live in-flight missile's; a live one is adopted (03-64).
- **GGS-ICBM-03-64** After a save and reload a flying missile holds exactly one ticket again and flies on to its target.
- **GGS-ICBM-03-63** The ticket budget is not used up by missiles: the in-flight cap is below Forge's per-mod limits.

## Tests

`ChunkWindowTest`; scenario `icbm.missile.chunk_tickets` (a 200-block flight; checks the maximum chunks and tickets at every tick and that the window moved across several chunks), `icbm.missile.timeout` (a short `missileTimeoutTicks` fires the timeout branch mid-air), `icbm.missile.restart_adopt` (the load callback on a live and a stray ticket, a save and reload of the entity),  checks the maximum chunks and tickets at every tick, that the window moved across several chunks, the load callback and the timeout path both release it) and `icbm.missile.launch_detonate_t1` (released on impact).

## Open questions

- Whether a restart should keep a flying missile loaded without a player nearby. The first build lets it wait; a persistent-ticket mode can be added in I7 if playtests ask for it.
