# 01-core (GregGuns ICBM)

## Overview

Cross-cutting ICBM integration: the module flag, the Galacticraft dependency, and computer control. ICBM baseline: [icbm-spec 01-core](../../icbm-spec/01-core/index.md).

## Contents

- [golden-dome.md](golden-dome.md): OpenComputers components on every ICBM machine and the Golden Dome program floppy.

## Integration map

| ICBM feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Computer control (1.6.4 ComputerCraft; none in 1.12) | OC components plus the Golden Dome floppy | OpenComputers 1.12.64-GTNH | [golden-dome.md](golden-dome.md) |
| Radio bus | GT data sticks plus the GT machine-controller cover | GT5-Unofficial | [D13](../DECISIONS.md#decided) |
| Forge Energy, `use_energy` | EU | GT5-Unofficial | [0003](../../design/0003-energy-model.md) |
| Module gating | `modules.icbm`, which turns off without Galacticraft | Galacticraft 3.4.34-GTNH | [D6, D9](../DECISIONS.md#decided) |

## Built in I0

- The `modules.icbm` flag, which turns itself off (with one logged line) when Galacticraft is missing ([D6, D9](../DECISIONS.md#decided)). Galacticraft and OpenComputers are optional dependencies. Scenario `icbm.module_gate`.
- The [ported-code gate](../../../CONTRIBUTING.md#ported-code-mit) and `THIRD-PARTY-NOTICES.md`, scoped by [D45](../DECISIONS.md#decided).
- The [third-party asset folder](../../../src/main/resources/assets/gregguns/thirdparty/README.md) and its CREDITS section.
- Everything else in this area (OC components, then the Golden Dome floppy) is still planned for [I4](../../ROADMAP-ICBM.md#i4--detection-and-defense) and [I5](../../ROADMAP-ICBM.md#i5--endgame-warheads-and-systems).

## Pack-side changes

None yet.

## Related areas

04 (launch control) and 05 (detection and defense) own the machines whose components are listed in [golden-dome.md](golden-dome.md).

## Open questions

See [golden-dome.md § Open questions](golden-dome.md#open-questions).
