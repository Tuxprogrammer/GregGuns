# 01-core (GregGuns ICBM)

## Overview

Cross-cutting ICBM integration: the module flag, the Galacticraft dependency, and computer control. ICBM baseline: [icbm-spec 01-core](../../icbm-spec/README.md#areas).

## Contents

- [golden-dome.md](golden-dome.md): OpenComputers components on every ICBM machine and the Golden Dome program floppy.

## Integration map

| ICBM feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Computer control (1.6.4 ComputerCraft; none in 1.12) | OC components plus the Golden Dome floppy | OpenComputers 1.12.64-GTNH | [golden-dome.md](golden-dome.md) |
| Radio bus | GT data sticks plus the GT machine-controller cover | GT5-Unofficial | [D13](../DECISIONS.md#decided) |
| Forge Energy, `use_energy` | EU | GT5-Unofficial | [0003](../../design/0003-energy-model.md) |
| Module gating | `modules.icbm`, which turns off without Galacticraft | Galacticraft 3.4.34-GTNH | [D6, D9](../DECISIONS.md#decided) |

## Pack-side changes

None yet.

## Related areas

04 (launch control) and 05 (detection and defense) own the machines whose components are listed in [golden-dome.md](golden-dome.md).

## Open questions

See [golden-dome.md § Open questions](golden-dome.md#open-questions).
