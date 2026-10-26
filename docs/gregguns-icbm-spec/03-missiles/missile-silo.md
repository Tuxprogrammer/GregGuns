# Missile Silo and T3 missiles

- **ICBM baseline:** [launcher platform](../../icbm-spec/03-missiles/index.md) (ICBM launches from a launcher block; GregGuns builds a multiblock)
- **Design records:** [0018 §8](../../design/0018-icbm-program.md), [0011](../../design/0011-machine-policy.md)
- **Decisions:** [D8](../DECISIONS.md#d8-in-detail), [D19](../DECISIONS.md), [D22](../DECISIONS.md), [D32](../DECISIONS.md), [D70 to D75](../DECISIONS.md)
- **Pack mods:** GT5-Unofficial (`5.09.54.205`): multiblock base, StructureLib, hatches; Galacticraft (`3.4.34-GTNH`): the pad block, `RocketFuels`; NewHorizonsCoreMod (`2.9.84`): the Tier 3 rocket parts
- **Roadmap:** [I3](../../ROADMAP-ICBM.md#i3--the-silo-and-heavy-warheads-t3-nuclear)
- **MTE id:** 20141 (`GregGunsMTEIDs.MISSILE_SILO`)

## What changes from ICBM

ICBM's launchers are single blocks that take a missile item. GregGuns' T3 missile does not fit the 3x3 pad plate, so it launches from a GT multiblock, the **Missile Silo**: a 5x5 tower dug into the ground, with an open 3x3 shaft. The player puts a body and a warhead in an Input Bus and fuel in an Input Hatch; the silo mates them into an armed missile in the shaft and fuels it. The player never touches the missile. The Launch Control fires it.

## Structure

One fixed piece, 5 wide, 5 deep and 12 layers tall, with the four corner columns cut (so it is not a cube). Source of truth: `SiloShape`.

| Layer (1 = floor) | Cells |
|---|---|
| 12, the rim | ring of 12 Gantry Beam |
| 5 to 11 | ring of 12 Gantry Casing per layer (84), no hatches |
| 2, 3, 4 | ring of 12 **glass** per layer, so the missile shows (36 cells with the controller) |
| 1, the floor | outer ring of 12 Gantry Casing (hatches allowed), inner 3x3 **Galacticraft launch pad** |

- **Glass:** GT's `chainAllGlasses`, the conventional helper: every borosilicate tier, GT's other glasses and even vanilla glass are accepted, mixed. It shares the `BOROGLASS` structure channel.
- **Hatches:** the Input Bus, the Input Hatch, the Energy Hatch and the Maintenance Hatch may replace any glass pane of layers 2 to 4 or any outer-ring cell of the floor. A hatch in a casing ring above layer 4 does not form the silo.
- **Controller:** the middle of the front wall, in the middle glass ring (layer 3).
- **Pad:** the nine blocks accept either the unformed Galacticraft pad blocks (`tile.landingPad`) or the formed pad (`tile.landingPadFull` at the center and `tile.dummyblock` meta 2 on the other eight), found by registry name through `GalacticraftPads` so no Galacticraft class loads. Without Galacticraft the module is off and the silo inert. The launch never removes or damages the pad (D32): a missile does not extend `EntityAutoRocket`.
- **Counts:** 96 Gantry Casing positions (84 needed, the rest may be hatches), 35 glass panes plus the controller, 12 beams, 9 pad blocks.
- **The shaft:** 3x3, open to the sky, left as "anything" so a body stacked by hand does not unform the silo. The missile climbs straight out; keep the opening above the rim free of blocks.

## Mating (0018 §8)

Each cycle the controller does the next job (`checkProcessing`):

1. **A missile stands in the shaft:** pump fuel (below).
2. **A hand-stacked body block stands in the shaft:** the controller waits ("arm it by hand, or take it apart"). A body placed on the pad by hand, a warhead on its nose and a sneak right-click arm it exactly as on an open pad.
3. **The shaft is empty:** look in the Input Buses for a missile body and a warhead block item whose casing tier the body carries (a body carries its own tier or lighter, D60). Take one of each, run `MATE_TICKS` = 400 (20 s) and, when the cycle ends, spawn an armed missile entity on the pad with the body's guidance and the warhead's explosive, fill and casing. If something took the shaft meanwhile, the parts drop as items. A pending mating is saved in the controller's NBT.

Refusals show on the controller as the cycle result: no body, no warhead, a warhead too heavy, a hatch too weak.

## Fuel (D22)

An Input Hatch holds the fuel. The pump takes the first fluid that `MissileTier.acceptsFuel` accepts, the same lists `MissileGc` registers with Galacticraft's `RocketFuels`, so pad and silo agree by construction; a test pins that. Each cycle moves up to 2,000 mB in 40 ticks. A fluid the tier does not accept is left alone ("not a fuel this missile accepts"). T3 takes Purple or Green; T1 and T2 keep their lists.

## Tiers (D70)

- **The Energy Hatch tier decides the missile:** the weakest hatch must reach the body's voltage, HV for T1, EV for T2, LuV for T3 (`SiloRules.requiredVoltageIndex`). Mating a heavier body is refused with GT's "insufficient machine tier", and a launch of a standing missile that is too heavy is refused with `LaunchResult.SILO_TIER`, which the Launch Control shows (`gregguns.icbm.control.status.silo_tier`) and which keeps its button locked.
- **T3 needs the silo; T1 and T2 may launch from it too.** A T3 body is refused on a Galacticraft pad at placement.
- The controller block is HV (D69 floor); its recipe is an HV Assembler recipe.

## The silo bonus (D73)

A missile launched from a silo gets scatter x0.5 (`icbm.siloScatterFactor`, on top of the guidance rule; the 10% floor of the base still applies to the guided share, and the distance term shrinks with it) and range x1.25 (`icbm.siloRangeFactor`, on top of `icbm.missileRangeFactor`). The open pad keeps today's numbers. The factors are passed to `EntityMissile.launch(tx, tz, source, scatter, range)` and kept for the flight; the Launch Control shows the range of the site it is linked to.

## The launch site API

`com.gregguns.icbm.missile.LaunchSite` (`centerX/Z`, `status`, `maxRange`, `expectedRadius`, `hold`, `launch`). `PadMissiles.site(world, x, y, z)` wraps the unchanged static pad API; `MTEMissileSilo.site()` is the silo. The Launch Control reaches both through `LaunchSites.find`, the silo first (its floor is a pad too, but a silo launch must obey the silo), within 6 blocks horizontally and 4 vertically of the silo controller (`SiloSearch`).

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

- **GGS-ICBM-03-30** The silo forms from glass rings, the pad floor and hatches in the glass or the floor ring, and not with a hatch above the glass or without the pad. Scenario `icbm.silo.structure`.
- **GGS-ICBM-03-31** From a body and a fitting warhead in a bus and Purple fuel in the hatch, the silo mates, fuels and, fired by the Launch Control, launches a T3 that detonates on target, leaving the silo intact and no chunk ticket. Scenario `icbm.silo.t3_launch`.
- **GGS-ICBM-03-32** The silo refuses a body with no warhead, a warhead too heavy and an unaccepted fluid, and loses nothing. Scenario `icbm.silo.refusals`.
- **GGS-ICBM-03-33** An HV hatch launches a T1 and refuses a T3; a LuV hatch launches a T3; the silo's range and scatter factors apply. Scenario `icbm.silo.tiers`.

## Tests

`SiloShapeTest`, `SiloRulesTest`, `SiloSearchTest`, `MissileTierTest`, `ScatterTest`; scenarios above.

## Open questions

None.
