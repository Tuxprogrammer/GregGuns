# Outputs and components (I2 part)

- **ICBM baseline:** [launcher-connector-and-network](../../icbm-spec/04-launch-control/launcher-connector-and-network.md)
- **Decisions:** [D13](../DECISIONS.md), [D18](../DECISIONS.md)
- **Roadmap:** I2 (inputs only), [I4](../../ROADMAP-ICBM.md#i4--detection-and-defense) (outputs on every machine, OpenComputers)

I2 ships the Launch Control's inputs. The rest of this sheet is filled in by I4.

## Inputs the Launch Control takes in I2

| Input | How | Section |
|---|---|---|
| Typed coordinates, player position | ModularUI2 fields and button | [launch-control-block](launch-control-block.md#targeting) |
| GT data stick | Slot with Load and Save | same |
| Redstone | Rising edge fires once | [launch-control-block](launch-control-block.md#triggers) |
| GT machine controller cover | Rising edge of "allowed to work" fires once | same |

## Planned for I4

Redstone and cover outputs (armed, fueled, in flight, last result) on every machine, and the OpenComputers component with the floppy programs (D18). They link to [golden-dome.md](../01-core/golden-dome.md).
