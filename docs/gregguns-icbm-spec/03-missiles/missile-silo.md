# Missile Silo and T3 missiles

- **ICBM baseline:** [launcher platform](../../icbm-spec/03-missiles/index.md) (ICBM launches from a launcher block; GregGuns builds a multiblock)
- **Design records:** [0018 §8](../../design/0018-icbm-program.md), [0011](../../design/0011-machine-policy.md)
- **Decisions:** [D8](../DECISIONS.md#d8-in-detail), [D19](../DECISIONS.md), [D22](../DECISIONS.md), [D32](../DECISIONS.md), [D70 to D75](../DECISIONS.md), [D286](../DECISIONS.md)
- **Pack mods:** GT5-Unofficial (`5.09.54.205`): multiblock base, StructureLib, hatches; Galacticraft (`3.4.34-GTNH`): the pad block, `RocketFuels`; NewHorizonsCoreMod (`2.9.84`): the Tier 3 rocket parts
- **Roadmap:** [I3](../../ROADMAP-ICBM.md#i3--the-silo-and-heavy-warheads-t3-nuclear)
- **MTE id:** 20141 (`GregGunsMTEIDs.MISSILE_SILO`)

## What changes from ICBM

ICBM's launchers are single blocks that take a missile item. GregGuns' T3 missile does not fit the 3x3 pad plate, so it launches from a GT multiblock, the **Missile Silo**: a 5x5 tower dug into the ground, with an open 3x3 shaft. The player puts a body and a warhead in an Input Bus and fuel in an Input Hatch; the silo stands the body on the pad, seats the warhead on it, arms the stack and fuels it, each step in view (D286). The player never touches the missile. The Launch Control fires it.

## Structure

One fixed piece, 5 wide, 5 deep and 12 layers tall, with the four corner columns cut (so it is not a cube). Source of truth: `SiloShape`.

| Layer (1 = floor) | Cells |
|---|---|
| 12, the rim | ring of 12 Gantry Beam |
| 5 to 11 | ring of 12 **Light or Dark Concrete** per layer (84); hatches allowed (D117) |
| 2, 3, 4 | ring of 12 **glass** per layer, so the missile shows (36 cells with the controller) |
| 1, the floor | outer ring of 12 **Light or Dark Concrete** (hatches allowed), inner 3x3 **Galacticraft launch pad** |

- **Glass (D115):** purely visual, so the missile can be seen: it never gates a launch, and there is no glass tier or effect on range, tiers or anything else; the Energy Hatch alone sets the silo's tier. The glass rings accept **Tinkers' Construct Clear Glass** (`TConstruct:GlassBlock`) and **EnderIO Quite Clear Glass** (`EnderIO:blockFusedQuartz` meta 1) or its **Enlightened Clear Glass** (meta 3, the same glass giving off light), mixed freely, and nothing else: GT's borosilicate frames made a Tier 3 missile hard to see. Dark Clear Glass and the Fused Quartz metas are refused. Blocks are found by registry name (`SiloGlass`), so no Tinkers' or EnderIO class loads. The hologram and auto-build place Tinkers' Clear Glass, or EnderIO's when Tinkers' is absent. When neither mod is loaded (the dev environment) the rings accept vanilla glass instead, with one logged warning. The ring no longer shares the `BOROGLASS` structure channel.
- **Walls and floor ring (D116):** GT's concrete block `gregtech:gt.blockconcretes` (`GregTechAPI.sBlockConcretes`), metas 0 to 7 Dark and 8 to 15 Light, mixed freely. Accepted: stone (0, 8), bricks (3, 11), cracked bricks (4, 12), mossy bricks (5, 13), chiseled (6, 14), smooth (7, 15). Cobblestone and mossy cobblestone (1, 2, 9, 10) are refused: they are the rubble form the plain stone drops as. The hologram and auto-build place Light Concrete (meta 8). GT gives concrete no casing texture index, so a hatch in the silo still wears the Gantry Casing texture.
- **Hatches (D117, amends D71):** the Input Bus, Input Hatch, Energy Hatch and Maintenance Hatch may replace any wall block (layers 5 to 11), any glass pane (layers 2 to 4) or any outer-ring cell of the floor. Never the rim (layer 12, Gantry Beam), and never the inner 3x3 pad floor, which a hatch would break. At least 84 of the 96 concrete positions must stay concrete.
- **Controller:** the middle of the front wall, in the middle glass ring (layer 3).
- **Window edges (D290, owner 2026-10-30):** the two cells either side of the controller in each of the three front-face glass rings (six cells, `W` in `SiloShape`) accept clear glass, a hatch **or the wall's concrete**, so the hatches can stand in the face's two outer columns and leave the middle column clear glass. Every other glass cell still takes glass or a hatch only.
- **Generated layout:** a silo in a complex has its four hatches in those two outer columns, two high, and glass everywhere else in the window. Seen by a viewer standing in front of the controller (looking at it, so the controller faces them): left column Input Hatch (controller row) over Maintenance Hatch; right column Energy Hatch (controller row) over Input Bus. The silo missing its Energy Hatch has glass in its place. A player-built silo may use the same layout or any other the structure accepts.
- **Launch Control (D288):** optional, at most one, in any hatch position (a wall, glass or floor-ring cell; `SiloLaunchControl`, a hatch element in every hatch adder). The structure check binds it, so the silo fires through its own Launch Control and no other; a second one fails its position. It keeps its own texture. Generated and dev-built silos put it in the front face's window, top row (layer 4), in the outer column on a viewer's right (`SiloShape.LAUNCH_CONTROL_COLUMN`, `LAUNCH_CONTROL_RISE`). The controller's tooltip lists it.
- **Pad:** the nine blocks accept either the unformed Galacticraft pad blocks (`tile.landingPad`) or the formed pad (`tile.landingPadFull` at the center and `tile.dummyblock` meta 2 on the other eight), found by registry name through `GalacticraftPads` so no Galacticraft class loads. Without Galacticraft the module is off and the silo inert. The launch never removes or damages the pad (D32): a missile does not extend `EntityAutoRocket`.
- **Counts:** 96 concrete positions (84 needed, the rest may be hatches), 35 glass-ring cells (29 glass-only, 6 window edges) plus the controller, 12 beams, 9 pad blocks.
- **The shaft:** 3x3, open to the sky, left as "anything" so a body stacked by hand does not unform the silo. The missile climbs straight out and on up to the build height before it turns (D287, [flight.md](flight.md#the-climb)), so a roof or a chimney built around the opening is safe; keep the opening itself free of blocks for 12 blocks above the pad plus the body, or the launch is `BLOCKED`.

## The doors (D89, D90)

Two hinged panels lie across the 3x3 opening at the rim, each 1.5 x 3 blocks and 2/16 thick, in Gantry Casing texture. A launch opens them (40 ticks, eased, 115 degrees: they rest leaning out about 65 degrees above horizontal), the countdown runs with them open, and they swing shut once the missile's bottom is 4 blocks above the rim top. They are cosmetic apart from one rule: a missile cannot ignite through closed doors.

- **State:** `SiloDoors` (CLOSED, OPENING, OPEN, CLOSING, start time, duration), saved in the controller's NBT. The controller derives the wanted state every 5 ticks from the missiles in the shaft column, so a reload resumes the sequence.
- **Carrier:** `EntitySiloDoors`, never saved, made by the controller, synced by `PacketSiloDoors` to the players tracking it (late joiners catch up within a second); drawn by `RenderSiloDoors`. It is not a block, so it never blocks the climb or the arc.
- **Abort:** doors turn around from the angle they are at and take only the way back.
- **Unformed mid-sequence:** a gap of 200 ticks aborts a launch still in its warm-up, resets the doors and removes the carrier; after ignition the missile flies on. A T3 also stands down by itself when no formed silo stands under it for 300 ticks.

## The assembly sequence (D286)

The silo builds the missile in the shaft where the player can watch it, one part at a time, and **each part stands in the shaft before its working cycle starts**. The order is always body, warhead, fuel. The stage machine is `SiloSequence` (pure data, unit-tested); the controller saves only its stage (`NONE`, `BODY`, `BODY_DONE`, `WARHEAD`). The parts are blocks in the world, never items inside the controller.

| Step | What the player sees | Cycle | Status line |
|---|---|---|---|
| 1. Body | A missile body from the Input Bus stands on the pad's center as a body block (the block a player places by hand, D31), unfueled, no warhead | `SiloRules.BODY_TICKS` = 200 (10 s) | "Setting up the missile body on the pad" |
| Wait | The body stands; no warhead the body carries is in the bus | none | "The body stands on the pad: waiting for a warhead in the Input Bus", or the warhead is too heavy, or inert |
| 2. Warhead | A warhead the body carries (D60) is seated on the nose as a warhead block | `WARHEAD_TICKS` = 200 (10 s); its end arms the stack into the missile entity (`MissileStacks.armIntoEntity`, as a sneak right-click does; no drops) | "Mating the warhead to the missile body" |
| 3. Fuel | The armed missile stands in the shaft throughout | `PUMP_TICKS` per portion (below) | "Fueling the missile", then "The missile is fueled and ready" |

- **Any order:** parts may arrive in any order or together. A warhead with no body waits in the bus ("Waiting for a missile body"); fuel waits in the hatch until a missile stands. A body alone goes up, runs its cycle and waits. The warhead stays in the bus for the whole body cycle.
- **Power and tier:** every step keeps the power gate (below) and the hatch tier rule (D70): no part leaves the bus, and no cycle starts, without both. A body too heavy for the Energy Hatch stays in the bus with GT's "insufficient machine tier".
- **The world wins:** the controller looks into the shaft every second (`watchShaft`). A body that is gone ends the sequence; a warhead taken off the nose steps back to waiting for one; a running cycle whose part is gone stops at once. Any change in the shaft asks GT for a check, because GT only checks on hatch events.
- **Nothing dupes or vanishes:** breaking the silo mid-sequence leaves the body and warhead standing; each drops its own item once when broken (or the player arms the stack by hand). A body broken under a seated warhead stops the mating cycle, and the silo breaks the loose warhead so it drops as one item. Arming happens only when the stack is complete and fits.
- **Adoption:** a body found on the pad with no sequence of the silo's (stacked by hand, or left by a controller that was broken and rebuilt) gets the body cycle and the sequence continues; a warhead seated on it by hand is mated like the silo's own. Arming by hand works at any point. A warhead on the nose that is too heavy or lit is not mated ("take it off"); a body off the pad's center, a loose warhead or a block in the column above the pad stops the silo ("Something in the shaft is in the way").
- **The Launch Control** sees a standing body or stack as present but not armed (`NOT_ARMED`), as on an open pad.
- **Old saves:** a controller saved by an earlier build mid-mating held the body and warhead inside itself. On its first check it stands them in the shaft (body, then warhead on the nose, stage `WARHEAD`) and runs the mating cycle again; if the shaft is taken, they drop as items.
- No fuel gauge is drawn: the missile model and WAILA have none.

Refusals show on the controller as the cycle result: no body, a warhead waiting for a body, no warhead, a warhead too heavy, an inert warhead, a hatch too weak, something in the way.

## Fuel (D22)

An Input Hatch holds the fuel. The pump takes the first fluid that `MissileTier.acceptsFuel` accepts, the same lists `MissileGc` registers with Galacticraft's `RocketFuels`, so pad and silo agree by construction; a test pins that. Each cycle moves up to 2,000 mB in 40 ticks. A fluid the tier does not accept is left alone ("not a fuel this missile accepts"). T3 takes Purple or Green; T1 and T2 keep their lists.

## Tiers (D70)

- **The Energy Hatch tier decides the missile:** the weakest hatch must reach the body's voltage, HV for T1, EV for T2, LuV for T3 (`SiloRules.requiredVoltageIndex`). Mating a heavier body is refused with GT's "insufficient machine tier", and a launch of a standing missile that is too heavy is refused with `LaunchResult.SILO_TIER`, which the Launch Control shows (`gregguns.icbm.control.status.silo_tier`) and which keeps its button locked.
- **T3 needs the silo; T1 and T2 may launch from it too.** A T3 body is refused on a Galacticraft pad at placement.
- The controller block is HV (D69 floor); its recipe is an HV Assembler recipe.

## The silo bonus (D73)

A missile launched from a silo gets scatter x0.5 (`icbm.siloScatterFactor`, on top of the guidance rule; the 10% floor of the base still applies to the guided share, and the distance term shrinks with it) and range x1.25 (`icbm.siloRangeFactor`, on top of `icbm.missileRangeFactor`). The open pad keeps today's numbers. The factors are passed to `EntityMissile.launch(tx, tz, source, scatter, range)` and kept for the flight; the Launch Control shows the range of the site it is linked to.

## The launch site API

`com.gregguns.icbm.missile.LaunchSite` (`centerX/Z`, `status`, `maxRange`, `expectedRadius`, `hold`, `launch`). `PadMissiles.site(world, x, y, z)` wraps the unchanged static pad API; `MTEMissileSilo.site()` is the silo. The Launch Control reaches both through `LaunchSites.find`: its own silo when the silo's structure holds it, else a Galacticraft pad it touches (D288, [launch-control-block](../04-launch-control/launch-control-block.md)). A formed silo's floor is never fired as a bare pad. `MTEMissileSilo.signName()` reads the first written line of a wall sign on the face one block above the controller, for the Launch Control's site line.

## T3 missiles

| | T3 |
|---|---|
| Body | 6 blocks tall, NTM model, `missile_body_t3`; entity id 122 |
| Fuel | 8,000 mB (times `icbm.missileFuelFactor`), Purple or Green |
| Range / speed | 20 to 8,000 blocks (10,000 from a silo) / 1.6 blocks per tick |
| Scatter | 6 blocks base (3 from a silo); no guidance steps (Mk III is IV, below the body's LuV) |
| Health | 60 |
| Warhead | T3 or lighter |
| Build | LuV chip and schematic (yellow lens), Assembly Line at LuV behind a research scan: 4 compressed titanium plates, 4 Heavy Duty Rocket Fins Tier 3, 1 Heavy Duty Rocket Engine Tier 3 (NewHorizonsCoreMod, found by registry name), 2 Master circuits, lubricant; Assembler fit of a Mk III computer |
| Recycling | the compressed titanium comes back |

Cost (estimates, D34): fins 150 and engine 700 plate equivalents each, about 1,300 for the body, one T3 rocket launch with its warhead. The nuclear coder owns the T3 and nuclear warheads; the silo mates whatever `BlockWarhead` item carries `{explosive, fill}`.

## Requirements

- **GGS-ICBM-03-30** The silo forms from clear glass rings (other glass is refused), the pad floor and hatches in the glass or the floor ring, and not with a hatch above the glass or without the pad. Scenario `icbm.silo.structure`.
- **GGS-ICBM-03-31** From a body and a fitting warhead in a bus and Purple fuel in the hatch, the silo mates, fuels and, fired by the Launch Control, launches a T3 that detonates on target, leaving the silo intact and no chunk ticket. Scenario `icbm.silo.t3_launch`.
- **GGS-ICBM-03-32** The silo stands a body but does not seat a warhead too heavy or inert, never takes a salvage body, does not pump an unaccepted fluid, and loses nothing. Scenario `icbm.silo.refusals`.
- **GGS-ICBM-03-34** Each part stands in the shaft before its cycle, in the order body, warhead, fuel, whatever order the parts arrive in; no part moves without power; breaking a part or the silo mid-sequence drops each part exactly once. Scenarios `icbm.silo.sequence`, `icbm.silo.sequence_any_order`, `icbm.silo.sequence_break`, `icbm.silo.power_gate`.
- **GGS-ICBM-03-33** An HV hatch launches a T1 and refuses a T3; a LuV hatch launches a T3; the silo's range and scatter factors apply. Scenario `icbm.silo.tiers`.
- **GGS-ICBM-03-34** A silo under a stone roof with a chimney around its opening launches a missile that stays on the shaft's column up to the build height, breaks no block of the silo, roof or chimney, and hits its target (D287). Scenario `icbm.silo.climb_clear`.

## Tests

`SiloDoorsTest`, `SiloShapeTest`, `SiloRulesTest`, `SiloSequenceTest`, `LaunchControlPairingTest`, `MissileTierTest`, `ScatterTest`; scenarios above and `icbm.silo.own_control`.

## Open questions

None.

## Unloaded shaft chunks (I4 carry-over)

Every step (placing a part, a cycle, arming, pumping) waits for every chunk of the shaft to be loaded, and the parts stay in the bus. The refusal is remembered; once the shaft loads, the controller asks GT for an immediate recheck (GT's multiblock checks are event-driven, so without it the silo would stay idle). Scenario: `icbm.silo.shaft_chunk_guard` ([D123](../DECISIONS.md)).

## Power gate

The silo draws the body tier's voltage for every body, mating and pump cycle (HV T1, EV T2, LuV T3). GT stops a
multiblock for good ("power loss") on the first tick it cannot pay, so the silo checks before it takes anything:

- A step (placing a body or warhead, a body or mating cycle, a pump cycle) starts only when the Energy Hatches hold at least
  `SiloRules.MIN_BUFFER_TICKS` (2) ticks of its draw. Otherwise the silo moves no body, warhead or fuel, shows the
  `gregguns_silo_no_power` status ("No power") and stays idle. It is not switched off and no power-fail is recorded.
- While it waits, the silo looks at the hatches every 10 ticks and asks GT for a recipe check as soon as they hold
  enough, so it starts on its own when power arrives. No machine-side packet, sound or state change happens meanwhile.
- Power that runs out mid-cycle still stops the machine as GT does (the parts stay standing in the shaft and the silo
  switches itself back on); the cycle then runs again, gated the same way. The doors and the launch draw no EU.
- Tests: `SiloRulesTest` (the gate rule), scenario `icbm.silo.power_gate`.
