# Golden Dome: OpenComputers control

- **ICBM baseline:**
  - ICBM-Classic 1.12 has no computer integration. Its `IMachineInfo` interface was meant for it.
  - The 1.6.4 ComputerCraft peripherals `ICBMLauncher` and `ICBMRadar`, and the `emptower` OC component, are in [A2-lineage/computer-integration.md](../../icbm-spec/A2-lineage/computer-integration.md): 7 launcher methods, 2 radar methods, 13 EMP callbacks, and no events.
  - Behavior links: [launch sequence](../../icbm-spec/04-launch-control/launch-sequence.md), [radar station](../../icbm-spec/05-detection-defense/radar-station.md), [EMP tower](../../icbm-spec/05-detection-defense/emp-tower.md).
- **Decisions:** [D13, D14, D15, D16 and D18](../DECISIONS.md#decided)
- **Pack mods:**
  - OpenComputers (`OpenComputers`, 1.12.64-GTNH)
  - OpenSecurity (`opensecurity`, 1.2.2-GTNH), for precedent only
- **Roadmap:** the ICBM track (M10 and later), behind `modules.icbm`

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| 1.6.4: ComputerCraft peripherals on the launchers and radar; 1.12: no computer support | OpenComputers components on every ICBM machine | GTNH ships OpenComputers and not ComputerCraft (D13) |
| Poll only; no events ever reach the computer | Event signals on state change | Scripts can react to threats without busy loops |
| Radar returns a flat table keyed `x_0`, `y_0`, `z_0` … | Lists of record tables | Usable from Lua without index juggling |
| Launcher frequency get/set (the radio bus) | Dropped. Targets come from coordinates or a GT data stick | The radio bus is replaced by data sticks (D13) |
| `IMachineInfo` constants exist but nothing reads them | A `getInfo()` method on every component | Programs adapt to pack config (range, EU costs, cooldowns) |
| Players write their own scripts | **Golden Dome** floppy with ready-made programs | Computer launches with no Lua skill needed (D18) |

## Integration points

### OpenComputers: machines as components
- **Pack config:** none. Components need no config.
- **Source:**
  - [`SimpleComponent`](https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/network/SimpleComponent.java) and [`Callback`](https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/machine/Callback.java);
  - for events: [`Environment`](https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/network/Environment.java) and [`Node`](https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/network/Node.java) (`sendToReachable("computer.signal", …)`).
- **GregGuns does:**
  - Each ICBM machine's tile is an OC environment, guarded with `@Optional.Interface(modid = "OpenComputers")`. Callbacks read and act on real GT tile state.
  - Events are sent only on a state change, never every tick, and the component address is the first argument.
  - Callbacks that change the world run on the server tick (`direct = false`); read-only getters may be `direct`.
- **Pack-side change:** none.

### OpenComputers: the Golden Dome floppy
- **Pack config:** none.
- **Source:**
  - [`Items.registerFloppy`](https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/Items.java) (name, dye color, `FileSystem` factory, recipe cycling), with [`FileSystem.fromClass`](https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/java/li/cil/oc/api/FileSystem.java) serving a read-only folder from our jar.
  - Precedent: OpenSecurity registers its SecureOS disk the same way and makes it craftable ([`ContentRegistry`](https://github.com/GTNewHorizons/OpenSecurity/blob/d94ead80992a7294362b45781f9b37a510c2f543/src/main/java/pcl/opensecurity/ContentRegistry.java)).
- **GregGuns does:**
  - In `init`, registers the floppy `Golden Dome` (dye: yellow) from `assets/gregguns/lua/golden_dome/`, with recipe cycling off.
  - Adds a GT Assembler recipe: blank OC floppy plus `circuit.MV` gives Golden Dome.
  - The disk's `.autorun.lua` links its `bin/` and `lib/` into the shell on mount and unlinks them on unmount, the same pattern OpenSecurity uses.
- **Pack-side change:** none.

## Components

| Component name | Machine | Methods | Signals |
|---|---|---|---|
| `gg_silo` | Missile silo (T3 and up, D8) | `getInfo()`, `getMissile()` → record or nil, `getFuel()` → `{fluid, amount, required}`, `getTarget()` / `setTarget(x, y, z)`, `setTargetFromStick()`, `canLaunch()` → bool, reason, `launch()` → bool, reason | `launcher_ready`, `launcher_fired` |
| `gg_launch_control` | Launch-control block beside a GC pad (T1–T2) | Same as `gg_silo` | Same |
| `gg_radar` | Single-block radar and the radar tower (D14) | `getInfo()`, `getContacts()` → list of `{id, x, y, z, tier, incoming, impactX, impactZ, eta}`, `getIncoming()` → incoming contacts only, `setProtectedRadius(r)` | `radar_contact`, `radar_incoming`, `radar_lost` |
| `gg_air_defense` | Air-defense battery (D15) | `getInfo()`, `getSamCount()`, `fireAt(contactId)` → bool, reason, `setAuto(bool)` | `sam_fired`, `sam_hit` |
| `gg_emp_tower` | EMP tower (D16) | `getInfo()`, `isReady()`, `getCooldown()`, `getMode()` / `setMode(m)` (`all`, `missiles`, `machines`), `getRadius()` / `setRadius(r)`, `fire()` → bool, reason | `emp_ready`, `emp_fired` |

`getInfo()` returns config-derived constants: range, EU cost per action and per tick, cooldown, maximum radius, and the device cap per pulse.

## Golden Dome programs

| Program | What it does |
|---|---|
| `launch` | `launch <x> <z>` or `launch --stick`: picks a linked silo or launch control, shows missile, fuel and range, asks for **confirmation**, then fires and reports |
| `radarwatch` | A live table of contacts from every reachable radar, with a beep or alarm on `radar_incoming` |
| `autodefense` | A daemon: on `radar_incoming` it calls `fireAt` on reachable air-defense batteries, and optionally fires an EMP tower in `missiles` mode. It logs every action |
| `emp` | `emp status`, `emp mode <m>`, `emp radius <r>`, `emp fire` (asks for confirmation) |
| `lib/goldendome.lua` | Shared helpers: find components by type, wait for signals, format contacts. Players may reuse it |

## Requirements

- **GGI-01-01** With OpenComputers absent, every ICBM machine loads and works, and no OC class is touched.
- **GGI-01-02** Each machine in the components table shows up as the named component when connected to an OC computer.
- **GGI-01-03** No callback that changes the world runs off the server thread.
- **GGI-01-04** `launch()` and `fire()` enforce the same checks as the GUI and redstone paths (power, fuel, range, missile present, cooldown), and return `false, "<reason>"` on refusal. A computer can never bypass a check a player can't.
- **GGI-01-05** Signals are sent on state transitions only. An unchanged state sends nothing.
- **GGI-01-06** `getContacts()` returns record tables with the fields listed, with `incoming` decided by the predicted impact point (D14).
- **GGI-01-07** The Golden Dome floppy exists only when OpenComputers is loaded, is crafted in the GT Assembler as specified, and is read-only.
- **GGI-01-08** On mount, `launch`, `radarwatch`, `autodefense` and `emp` run as shell commands, with no path setup by the player.
- **GGI-01-09** `launch` and `emp fire` both ask for confirmation before acting.

## Tests

| Requirement | How it is verified |
|---|---|
| GGI-01-01 | runServer boot without OpenComputers; unit test that the component classes are behind `@Optional` |
| GGI-01-02, 05, 06 | Unit tests on the component adapters with fake tiles; one batched client session with OC |
| GGI-01-03, 04 | Unit tests: each refusal reason from both the GUI path and the component path |
| GGI-01-07, 08, 09 | Batched client session: craft the floppy, mount it, run each program |

## Open questions

- Should `autodefense` also be able to call the GregGuns Turret (M6)? Lean no; that would need its own decision record.
- Does the data-stick target format need a GT-wide convention (for example, matching GT's own data-stick coordinate format)? To be checked when data-stick targeting is specced.
