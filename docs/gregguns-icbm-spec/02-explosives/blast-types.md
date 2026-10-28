# Blast types

- **ICBM baseline:** [blast-types](../../icbm-spec/02-explosives/blast-types.md), [gas-and-status](../../icbm-spec/02-explosives/gas-and-status.md), [explosive-registry](../../icbm-spec/02-explosives/explosive-registry.md) §3
- **Design records:** [0018 §5-7](../../design/0018-icbm-program.md)
- **Decisions:** [D10](../DECISIONS.md#d10-in-detail), [D11](../DECISIONS.md#decided), [D12](../DECISIONS.md#decided), [D34](../DECISIONS.md#decided)
- **Pack mods:** GregGuns pipeline (`ExplosionRules`, `BlockBreakGuard`, `ExplosionEvent.Detonate`); no pack mod
- **Roadmap:** [I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine) (T1 and T2); T3 and up in I3 and I5

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| 26 explosives with first-free numeric IDs | Named entries in one registry; the T1-T2 set below | D12, `ICBM-02-18` |
| Colors gas | Dropped | D10 |
| Each type is its own `Blast` class | An engine-owned crater and damage ramp plus a small **effect** per type | One block-breaking path (D11) |
| Per-blast `size` config | Full-casing radius and damage below, with the fill curve of [warheads-and-fill](warheads-and-fill.md) | D23 |
| Gas works through walls only by cell flood fill | A radius check around the center (no flood fill) | Cheaper; the gas is a status effect, not terrain |

## The T1 and T2 set

"Full" is a full casing (T1 fill 5, T2 fill 20). Radius is in blocks. Center damage is in half hearts before armor and falls to zero at the radius; it applies only to the damaging types. The T2 "EV-IV" casing is built at EV.

| Type | Tier | Terrain | Radius (full) | Center damage (full) | Effect |
|---|---|---|---|---|---|
| condensed | T1 | grid crater | 9 | 1,200 | none beyond the ramp |
| shrapnel | T1 | grid crater | 10 | 1,200 | everything in the radius burns 5 s |
| anvil | T1 | grid crater | 8 | 1,200 | slowness III for 10 s on what is in the radius |
| incendiary | T1 | none | 12 | none | entities burn 10 s; fire on up to 400 solid-topped cells |
| debilitation | T1 | none | 12 | none | 30 s cloud: slowness II, weakness II, nausea |
| chemical | T1 | none | 12 | none | 30 s cloud: poison II |
| smoke | T1 | none | 10 | none | 30 s marker cloud, harmless |
| repulsive | T1 | none | 12 | none | pushes entities outward, strongest at the center |
| attractive | T1 | none | 12 | none | pulls entities inward |
| fragmentation | T2 | sphere crater | 14 | 3,000 | everything in the radius burns 5 s |
| contagious | T2 | none | 14 | none | 30 s cloud: poison IV, hunger II |
| sonic | T2 | none | 14 | none | nausea 10 s and slowness III 5 s; glass in the radius breaks |
| breaching | T2 | none | 12 | none | a 3x3 tunnel 24 deep, straight down, through anything of hardness 50 or less |
| thermobaric | T2 | sphere crater | 20 | 3,000 | none beyond the ramp (the widest conventional crater) |

- **Payloads (D56, D57).** What each type is made of, besides its casing, a Fuze and Plastic Explosive. Full recipes are in [warheads-and-fill](warheads-and-fill.md#recipes).

  | Type | Payload |
  |---|---|
  | condensed | TNT, dynamite |
  | shrapnel | TNT, a Steel Fragmentation Sleeve |
  | anvil | TNT, 12 lead plates (a dense mass) |
  | incendiary | 2 Napalm Canisters (Dispersal Canister plus gasoline) |
  | debilitation | 2 Dispersal Canisters, nitrous oxide |
  | chemical | 2 Dispersal Canisters, ammonia gas, a few mustard seeds when HarvestCraft is present |
  | smoke | 2 Dispersal Canisters, creosote, coal dust |
  | repulsive | a Field Generator (HV) |
  | attractive | a Field Generator (HV), magnetic steel rods |
  | fragmentation | Industrial TNT, a Titanium Fragmentation Sleeve |
  | thermobaric | Industrial TNT, 2 Dispersal Canisters, kerosene |
  | sonic | Industrial TNT, 2 Emitters (EV) |
  | breaching | Industrial TNT, 2 Shaped-Charge Liner Cones, titanium rods |
  | contagious | Industrial TNT, 4 Bio Lab culture dishes, biomass |

  The gas, smoke and fuel-air fills are general industrial fluids in canisters, chosen by theme; no chemical-warfare agent is used or named (D56 as amended).
## The T3 set (I3)

Casing T3 (LuV to ZPM, fill cap 64). Radius and damage are at a full casing; the fill curve is the same cube root.

| Type | Tier | Terrain | Radius (full) | Center damage (full) | Effect |
|---|---|---|---|---|---|
| exothermic | T3 (LuV) | sphere crater | 28 | 8,000 | everything in the radius burns 5 s |
| endothermic | T3 (LuV) | sphere crater | 28 | 8,000 | slowness III for 10 s on what is in the radius |
| nuclear | T3 (ZPM) | sphere crater | 64 | 8,000 outside the kill zone | annihilation in the kill zone (60% of the radius), bosses included; fallout, see [nuclear-and-fallout](nuclear-and-fallout.md) |
| ender | T3 (LuV) | none | 24 | none | pulls living things for 8 s; those that reach the middle are sent to one landing spot, see below |
| anti-gravitational | T3 (LuV) | none (blocks are lifted, not destroyed) | 20 | none | lifts up to 512 plain blocks as flying blocks and floats living things, see below |

Payloads: exothermic is Industrial TNT, Plastic Explosive, four Napalm Canisters and kerosene; endothermic is Industrial TNT, Plastic Explosive, four Dispersal Canisters and helium; nuclear is Industrial TNT, Plastic Explosive, a core of eight uranium-235 and four plutonium ingots. Ender is Industrial TNT, Plastic Explosive, a LuV Emitter and sixteen Eyes of Ender; anti-gravitational is Industrial TNT, Plastic Explosive, two LuV Field Generators and four Nether Stars. EMP shipped in I4.

- **Damage targets (D34):** the damaging T1 types deal at least 1,200 at ground zero at full fill, and T2 at least 3,000, before armor. The unit test `WarheadBlastTest` pins the numbers, and scenario `icbm.blast.condensed` (and the T2 ones) kills a target in the zone. Partial fills deal `D * sqrt(fill / cap)`.
- **Special types** (gas, fire, push, sonic, breaching, smoke) are balanced on their own effect, not on the damage table (0018 §6).
- **Thermobaric** is a plain crater here; ICBM's threaded thermobaric energy model is a later refinement if play shows the need.
- **Dropped:** colors gas.

### Ender (I5, D144 and D145)

- **Pull and send.** For 160 ticks (8 s) the blast pulls every living thing in the box around the center toward it (ICBM's curve, peak 0.06 blocks per tick, vertical pull 1.8x). A living thing within **4 blocks** of the center is sent to **one landing spot chosen at detonation**. At most `icbm.enderMaxTeleports` (64) are sent per blast; the rest are only pulled.
- **Landing spot.** 40 to 128 blocks from the center on each horizontal axis, so it is outside the pull. It must be on a **chunk that is already loaded** (no chunk is ever loaded for it), with solid, dry, non-cactus, non-fire ground under two free blocks. After 16 failed attempts the blast only pulls. If the landing chunk unloads while the blast runs, the blast stops. The landing spot is saved in the blast's state, so a reload keeps it.
- **Same dimension only.** Players and ordinary mobs are sent; **bosses** (anything with a boss bar) are pulled but never sent; riders are dismounted first. Fall distance and motion are reset on landing. **Roofed dimensions:** the landing scan starts at `EnderRules.ROOFED_SCAN_TOP` (120) in a dimension with no sky (the Nether), never on the bedrock roof.
- **The event is a veto, not the move.** `EnderTeleportEvent` is posted for each send (vanilla's way: a listener may cancel, or move the target); the code then moves the entity itself, with `setPositionAndUpdate` (or the player's network handler for a player). A pack mod that cancels the event stops that one entity from being sent, and it is pulled instead.
- **Sent means safe.** A sent entity is immune to the pull for 200 ticks (`ggEnderSentUntil` in its entity data), so a landing inside another blast's box (or a short test distance) never drags it back. The scenario once failed for exactly this reason: the landing lay inside the pull box, and the cow was sent and then pulled back; the event was never canceled by anyone.
- **No endermen.** ICBM's eight endermen at the end are dropped.
- Ignores the terrain switch (no block changes).

### Anti-gravitational (I5, D146 to D148)

- **Lift.** For 600 ticks (30 s) the blast turns solid blocks of its sphere into flying-block entities, **top layer first**, at most 8 a tick and at most `icbm.antigravMaxBlocks` (512) per blast. It lifts only plain blocks: full opaque cubes that have an item of their own (so torches, doors, plants, wire and the like stay put), **no tile entities** (no chest, machine or inventory is ever lifted, copied or lost), no fluids, no bedrock, nothing harder than 30. The layer scan is up to 3 layers a tick, not tied to the shared tick budget (the effect runs before the engine opens its budget each tick, so it cannot read it; a layer is at most about 1,260 cells). Each layer goes through the claim and protection veto (`ExplosionEvent.Detonate`, the break guard) once; the survivors wait in a saved queue so a reload resumes exactly. It reads the `icbm.terrainDamageT3` switch itself; with it off only living things are lifted.
- **Living things** within the radius and 20 blocks above the center float up (+0.3 a tick every other tick, below 0.4 of speed, no fall damage), at most 128 per pass; flying players are left alone.
- **Flying block.** Rises at 0.5 blocks per tick to 12 to 40 blocks above where it was (or the 250 height limit), drifts a little, hovers 400 to 600 ticks, then falls and **lands**: it puts the block back where it stops or at the first free cell within three above; when the spot is taken, its chunk is not loaded or a claim vetoes the cell (a one-cell `ExplosionEvent.Detonate`, as lifting is vetted) it **drops as an item**. Landing is vetted like lifting: a one-cell `ExplosionEvent.Detonate` is posted for the cell, and a claim that vetoes it makes the block drop as an item instead. A hard age cap of 3,600 ticks makes it land wherever it is. It is saved with its chunk, so a reload keeps it flying.
- **Conservation (D148).** Every lifted block is placed or dropped: never lost, never duplicated. The scenario `icbm.blast.antigrav_flight` proves it by lifting a plate of a rare block (quartz), then counting that block in the world, over a wide column that includes the ground the blocks fall onto, plus the new item entities, before and after. An earlier version counted only 3 blocks below the origin and reported about 430 "lost"; the blocks had fallen far below that window, so nothing was lost.
- **Sync.** The block and metadata ride in the spawn data; the motion is server side and the client interpolates (two-client check is in the I5 client session).

## The T4 and T5 set (I5)

| Type | Tier | Terrain | Radius (full, fill 128) | Damage | Effect |
|---|---|---|---|---|---|
| [antimatter](antimatter.md) | T4 (UV) | sphere crater | 80 | annihilation in the kill zone (60%, bosses included); ramp from 16,000 outside it | ends red matter within 200 blocks; no fallout |
| [redmatter](red-matter.md) | T5 (UHV) | none from the engine; the body eats a growing ball | 32 | annihilates everything inside the ball each tick | grows, eats, pulls, idles, ends within `icbm.redMatterLifetimeSeconds`; no fallout |

## Pipeline and protection

- The engine does the crater and the damage ramp for the types that have terrain; every block change goes through `ExplosionRules`, `BlockBreakGuard` and `ExplosionEvent.Detonate`, and obeys `icbm.terrainDamageT<n>` (D52).
- The effects add only what the engine does not: status effects, fire, push, sonic glass and the breaching tunnel. Every block an effect changes needs `icbm.terrainDamageT<n>` on and a `BlockBreakGuard.mayBreak` pass for the shooter; with the config off, only status effects, fire on entities, pushes and particles remain.
- An effect keeps no state of its own. Gas ticks off the blast's age, which the engine saves, so a gas cloud survives a save and reload.
- Gas particles are sent as vanilla particle packets, at most 24 per pulse every 10 ticks per cloud.

## Requirements

- GGS-ICBM-02-20: every type in the table is a registry entry with the table's radius and damage at full fill.
- GGS-ICBM-02-21: a blast type's block changes respect `icbm.terrainDamageT<n>` and the break guard.
- GGS-ICBM-02-22: gas effects end after 30 s and are bounded by the radius.
- GGS-ICBM-02-23: a conventional type meets its tier's D34 damage at full fill.

## Tests

`WarheadBlastTest` (catalog, targets), `EnderRulesTest` (pull curve, targets, range), `WarheadMathTest` (curve against the registry definition), scenarios `icbm.blast.<type>` (one per type), `icbm.blast.ender_teleport` (the send, the landing spot, a far cow stays) and `icbm.blast.antigrav_flight` (the cap, a chest stays, conservation, NBT round trip) and `icbm.blast.block_damage_off`.

## Open questions

None.
