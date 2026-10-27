# EMP Tower and the EMP pulse

- **ICBM baseline:** [emp-tower](../../icbm-spec/05-detection-defense/emp-tower.md), [computer-integration](../../icbm-spec/A2-lineage/computer-integration.md)
- **Design records:** [0018](../../design/0018-icbm-program.md)
- **Decisions:** [D16](../DECISIONS.md), [D80](../DECISIONS.md), [D41](../DECISIONS.md), [D100 to D104](../DECISIONS.md)
- **Pack mods:** GregTech (single block, EU, multiblock maintenance, covers), IC2 (energy stores), ModularUI2
- **Roadmap:** [I4](../../ROADMAP-ICBM.md#i4--detection-and-defense)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| Walks every block of a skewed cube: 8 x size^3 blocks, about 1.7 million at range 60 (ICBM-05-10) | Walks the tile entities of chunks that are already loaded, inside a true sphere; never loads a chunk | D16: loop over tiles, not positions |
| Zeroes the energy of everything with settable energy | Drains the **nearest N devices** (config `empMaxDevices`, default 64) | D16: a dense base cannot stall the server |
| Machines are not hurt beyond energy | A multiblock that was hit also loses **one working maintenance part** (wrench, screwdriver, soft mallet, hard hammer, soldering iron or crowbar), picked at random among those still working | D16 |
| Fires 100 FE per square block, plus an upkeep of 10 FE per block of range per tick | **25 EU per square block** of the real radius, taken after the pulse runs; no upkeep | D16: continuous in the radius; GT machines hold their own charge |
| Extender blocks stacked above the tower add 20 blocks of range | None. The radius is set in the GUI, up to `empMaxRadius` (default 100) | One GT block; no extender column to keep in line |
| Range clamped only from above (ICBM-05-11) | Clamped from 4 to the maximum | ICBM-05-11 |
| A held redstone signal re-fires at every cooldown end | Fires once per **rising** edge of redstone or of GT's work-allowed flag (a machine controller cover) | Same rule as the Launch Control (ICBM-04-20) |
| A missile that is "triggered" can survive the pulse (ICBM-05-14) | It detonates and is removed in one step | ICBM-05-14 |
| EMP smoke particles spawned on the server never show (ICBM-05-13) | Particles and the sound are sent to the clients | ICBM-05-13 |
| Radio frequency and radio messages | None. Redstone, a cover, a computer, or the remote detonator ([handheld-tools.md](handheld-tools.md)) | D13 |

## The block

- GT single block, **EV**, MTE ID **20146** (`GregGunsMTEIDs.EMP_TOWER`), English name **EMP Tower**. No item slots.
- Energy: one input, two amps, from any side. It stores the cost of a pulse at the **largest** allowed radius (25 x 100^2 = 250,000 EU at the default), so any radius fits.
- Recipe: Assembler, EV, 60 s, circuit 24: EV hull, EV field generator, 2 EV emitters, 4 HV lithium batteries, 2 Data circuits. Recycling returns the hull's titanium. Tier-gated at EV; nothing below HV is ICBM (D69).
- Cost reading (D63 style): the pulse at the default radius 60 costs 90,000 EU, about 44 seconds of one EV input, and the tower waits 10 seconds between pulses.

## The pulse

1. **Gate (shared by the button, redstone, a cover and a computer).** The ICBM module is on; the tower is not cooling down; it holds at least the cost. The first failure names the status line. A refused pulse costs nothing.
2. **Machines** (mode `all` or `machines`). For each loaded chunk that overlaps the sphere, take its tile entities. A tile is a **device** when it is a GT tile entity that stores EU or runs a multiblock, or an IC2 energy store. Keep the nearest `empMaxDevices` inside the radius (the tower itself is spared), nearest first. Each kept device has its stored EU set to zero, and (D102a) so do the charged batteries in a Battery Buffer's slots and a Lapotronic Supercapacitor's stored EU (each such machine is one device); items in player inventories are not touched; a multiblock controller also loses one maintenance part it still had. GT cables hold no EU of their own, so there is nothing in them to drain.
3. **Missiles** (mode `all` or `missiles`). Every missile in flight (climbing or in its arc) inside the sphere rolls ICBM's chain, once per pulse: destroyed 50%, else guidance fried 80% of the rest (so 40% overall), else warhead triggered 10% of the rest (1% overall), else spared (9%). Missiles on a pad or in a countdown are not reachable.
   - *Destroyed:* removed at once with a flash and a bang that break nothing; the warhead does not go off.
   - *Fried:* no thrust. The missile keeps its sideways drift, falls under gravity (0.04 blocks per tick squared, at most 3 blocks per tick) and detonates where it hits.
   - *Triggered:* the warhead goes off where the missile is.
   The three chances are config (`empMissileKillChance` 0.5, `empMissileGuidanceChance` 0.8, `empMissileTriggerChance` 0.1).
4. **After the pulse.** The tower takes its cost, starts a 200 tick cooldown, and shows the result (devices, issues, missiles) on its screen. The blast sound and sparks are sent to clients.

## Triggers and the screen

- Fire button, redstone rising edge, rising edge of the work-allowed flag, the OpenComputers `gg_emp_tower` component, and the remote detonator.
- ModularUI2 panel: a dark status screen (state, EU against cost, reach, last pulse), the radius field with the maximum, a mode button that cycles `all`, `missiles`, `machines`, and the Fire button, gray with the reason as its tooltip until the gate passes. Every action re-checks the pressing player (alive, within 8 blocks); the radius is parsed and clamped on the server.
- Tooltip per D41 (summary, shift details, "Added by").

## The T3 EMP warhead (D80)

A T3 blast type, `emp`, on the tungstensteel casing at **LuV**, 1x1x2, fill cap 64. Radius `48 x (fill/64)^(1/3)` blocks (never below 3), no crater, no damage; the effect is one pulse in mode `all` at the blast radius, centered on the blast, with the same device cap, nearest-first order and missile table as the tower. Recipe: Assembler at LuV, circuit 4: 8 tungstensteel plates, 4 Industrial TNT, 2 Plastic Explosive, 2 LuV field generators, 2 LuV emitters, a Fuze Assembly Mk III (cost in the T3 band of D34).

## Requirements

- **GGI-05-01** The pulse reads tile entities of loaded chunks only and never loads a chunk.
- **GGI-05-02** At most `empMaxDevices` devices are hit, the nearest first; the sender is not one of them.
- **GGI-05-03** A hit multiblock gains exactly one maintenance issue while it has a part left to lose.
- **GGI-05-04** The cost is `ceil(25 x radius^2)` EU, strictly increasing with the radius, and is taken only after a pulse ran.
- **GGI-05-05** The radius clamps from 4 to `empMaxRadius`; a typed value is never trusted.
- **GGI-05-06** Missiles roll the table; a triggered missile is removed; a destroyed missile's warhead does not go off.
- **GGI-05-07** Every way of firing passes the same gate.
- **GGI-05-08** The EMP warhead runs the same pulse as the tower, and breaks no block.

## Tests

| Requirement | How it is verified |
|---|---|
| GGI-05-01, 02, 03, 04 | `EmpRulesTest`, `NearestPickerTest`; scenarios `icbm.emp.dense_pulse`, `icbm.emp.modes` |
| GGI-05-05, 07 | `EmpRulesTest`; scenarios `icbm.emp.gate`, `icbm.emp.triggers` |
| GGI-05-06 | `MissileTableTest`; scenarios `icbm.emp.missile_destroyed`, `_fried`, `_triggered`, `_unaffected`, `icbm.emp.tower_missile` |
| GGI-05-08 | `WarheadBlastTest`, `WarheadRecipeSpecTest`; scenarios `icbm.blast.emp`, `icbm.silo.t3_emp_launch` |

## Open questions

None. Players' item batteries and creepers (which ICBM's pulse also touched) are out of scope for D16.
