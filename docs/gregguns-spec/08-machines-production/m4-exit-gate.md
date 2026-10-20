# M4 exit gate audit

Evidence for the three M4 exit-gate boxes in [ROADMAP.md](../../ROADMAP.md#m4--production-chain-and-machines): one row per built machine against the four machine criteria, then the unification and ID-recording boxes. Audited on master 37b62ca plus this branch.

- **Roster:** [machine-roster.md](machine-roster.md). The 16 MTEs below are every machine the roster marks as M4.
- **Rules audited:** [modularui2-guis.md](modularui2-guis.md) (GGS-08-26 to 30), [nei-handlers.md](nei-handlers.md) (GGS-08-31 to 34), [metatileentity-framework.md](metatileentity-framework.md) (GGS-08-01, 48).
- **Scenario names** are in [docs/testing/README.md](../../testing/README.md). Class names are under `com.gregguns.machine` unless a package is given.

## Decisions made while auditing

1. **A GT-built GUI counts as a ModularUI2 GUI.** `MTEBasicMachineWithRecipe`, `MTEBasicMachineBronze` and every `MTEMultiBlockBase` return `true` from `useMui2()`, so GT builds their panel with MUI2 and GregGuns writes none. Checked in the pinned source: `MTEBasicMachineWithRecipe#L542`, `MTEBasicMachineBronze#L358`, `MTEMultiBlockBase#L3576`. A GregGuns GUI class exists only where the spec asks for extra controls (intensity, repair targets, camo cycle).
2. **"NEI recipes" is met for a machine with a recipe map by GT's automatic tab and catalyst, and is not applicable to a machine with no recipe.** The Repair Bench, Camo Bench and Armory Gantry run an action, not a recipe ([repair-bench.md](../09-machines-utility/repair-bench.md), [multiblocks.md](multiblocks.md#armory-gantry)), so there is nothing for NEI to list. Their hand recipes show in NEI's crafting-grid and Assembler tabs, which is GGS-08-34's wording ("every recipe a GregGuns item is made by"). This is the documented exception, GGS-08-31 for the Gantry, extended to the two benches by the same reason. The Gantry "repair materials per item" info page stays the open question in [multiblocks.md](multiblocks.md#open-questions).
3. **The Camo Bench draws no EU.** Techguns' bench has no energy use ([camo-bench.md](../../techguns-spec/09-machines-utility/camo-bench.md)) and the roster lists it as cosmetic, so the "EU input" criterion does not apply. It is an `MTETieredMachineBlock` that takes no energy (`isEnetInput` false).
4. **The Steam Ammo Press uses steam, not EU,** as the task and [ammo-press.md](ammo-press.md#tiers) say. It is GT's Bronze steam machine.
5. **"Integration scenario" means an in-world headless scenario that places or builds the real machine and exercises it.** A registration-only scenario does not count for a machine that has a process. One machine family had only that, so this branch added `machines.repair.bench.tiers` (MV to IV benches).
6. **Two literal-wording exceptions that do not break the intent.** `client/autotest/AutotestCommands` imports `codechicken.nei.recipe.GuiCraftingRecipe` to open the NEI recipe screen from the dev-only client test harness; it ships no handler (GGS-08-31 is about handlers and `IConfigureNEI`). `EquipmentGuiHandler` is an `IGuiHandler` for the armor equipment screen; no machine uses it (GGS-08-26 is about machine GUIs).

## Per-machine table

| Machine (MTE ID) | ModularUI2 GUI | EU input | NEI recipes | Integration scenario |
|---|---|---|---|---|
| Ammo Press Steam (20100) | `MTEAmmoPressSteam` extends GT `MTEBasicMachineBronze`: GT's MUI2 panel (decision 1) | Steam, by design (decision 4); Bronze steam machine | `gregguns.ammoPress` tab (lang key `gregguns.ammoPress`); the machine is a `RecipeMapWorkable`, so GT adds it as a catalyst. Asserted by `production.ammopress.tiers` | `production.ammopress.run.steam`, `production.ammopress.tiers` |
| Ammo Press LV, MV, HV, EV (20101 to 20104) | `MTEAmmoPress` extends GT `MTEBasicMachineWithRecipe`: GT's MUI2 panel | GT energy input at the tier (basic machine; amperage 1) | Same tab; catalysts by `RecipeMapWorkable` | `production.ammopress.run.lv`, `.mv`, `.hv`, `.ev`, `production.ammopress.tiers` |
| Ammunition Works (20105) | `MTEAmmoWorks` extends `MTEExtendedPowerMultiBlockBase`: GT's default multiblock MUI2 panel | Energy Hatch or Multi-Amp Hatch (HV controller) | `gregguns.ammoPress` tab; the controller is a catalyst (`production.ammoworks.registered` asserts `RecipeMapWorkable` and the map) | `production.ammoworks.run`, `.broken`, `.registered` |
| ~~Reaction Chamber EV, IV (20106, 20107) and Retort (20108)~~ | Retired by [0012](../../design/0012-retire-reaction-chamber-and-retort.md); inert placeholders only | n/a | n/a | n/a |
| Repair Bench LV (20109) | `repair.MTERepairBench#buildUI` with `RepairBenchGui extends MTETieredMachineBlockBaseGui` | GT energy input, `isEnetInput` true; the job's EU is taken from the buffer at the button press ([repair-bench.md](../09-machines-utility/repair-bench.md)) | Not applicable: no recipe map (decision 2). Hand recipe shown in the crafting grid, asserted by `machines.repair.registered` | `machines.repair.bench.lv`, `machines.repair.bench.refusals`, `machines.repair.registered` |
| Repair Bench MV, HV, EV, IV (20110 to 20113) | Same class and GUI | Same, at the tier | Not applicable (decision 2) | `machines.repair.bench.tiers` (added by this branch), `machines.repair.registered` |
| Armory Gantry (20114) | `repair.MTEArmoryGantry` extends `MTEExtendedPowerMultiBlockBase`: GT's default multiblock MUI2 panel | Energy Hatch or Multi-Amp Hatch; item tier gated by the lowest hatch | Not applicable, documented in the class header: GGS-08-31 (decision 2) | `machines.repair.gantry.forms`, `machines.repair.gantry.run` |
| Camo Bench (20115) | `repair.MTECamoBench#buildUI` with `CamoBenchGui extends MTETieredMachineBlockBaseGui`; the cycle button sends `PacketCamoCycle` | None, by design (decision 3) | Not applicable (decision 2) | `machines.camo.cycle`, `machines.repair.registered` |

The former standalone `*.sweep` scenarios are gone: each machine scenario now asserts that no item entity is left in its own arena. No GUI is exercised headlessly: a GUI's layout needs a client, see "Visual checks" below.

## Unification box

"No GregGuns item duplicates a GT material or part. Unification is checked by the dev self-check."

- **The check exists.** `core.selfcheck.UnificationChecks` over `UnificationDescriptor` (pure logic), fed by `SelfCheckRunner` at load-complete: no GregGuns item is ore-dictionaried under a GT `OrePrefixes` name, and `GTOreDictUnificator.getAssociation` must not treat a GregGuns stack as a `prefix.material` part. A violation fails the dev boot. Documented in [docs/testing/self-check.md](../../testing/self-check.md#checks). `ComponentChecks` adds the component-coverage rule from the same commit.
- **Unit tests:** `UnificationChecksTest`.
- **It passes.** Every scenario run boots with the self-check on, with all three modules, and the boot finishes. The run for this audit is below.

## ID-recording box

"MetaTileEntity IDs are reserved and recorded."

| Source | State |
|---|---|
| `GregGunsMTEIDs` | 16 constants, 20100 to 20115, all in `all()`, all inside 20100-20163 |
| [machine-roster.md](machine-roster.md#roster) | The same 16 IDs, same machines |
| [metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-mte-id-reservation) | Reservation of 20100-20163, the CropsNH pattern, and the "Built IDs (M4)" list of the 16 |
| [PACK-CHANGES.md](../PACK-CHANGES.md#gt5-unofficial-needs-agreement-with-the-gtnh-team) | One `MetaTileEntityIDs` named-entry row per ID (16 rows, `GregGunsAmmoPressSteam(20100)` to `GregGunsCamoBench(20115)`) |
| Unit test | `GregGunsMTEIDsTest`: unique, in range, 64-ID block, and the 16 built are the contiguous run from 20100 |

The GT5-Unofficial PR that adds the named entries is pack-side and cannot land from this repository. It is recorded as a pack-side change in PACK-CHANGES. Until it merges, IDs are held by the constants and the `-Dgt.debug=true` check in [metatileentity-framework.md](metatileentity-framework.md#gt5-unofficial-mte-id-reservation). GGS-08-01's "unit test asserts both agree" becomes possible only after the PR ships in the pinned GT.

## Visual checks for the orchestrator

These need a client and were not run here.

1. Open each GUI once: Ammo Press (any tier), Steam press, Repair Bench, Camo Bench, and the Ammunition Works and Gantry controllers. Look for missing lang keys (raw `gregguns.` text) and overlapping widgets.
2. NEI: search `bullet`, press R, confirm the Ammo Press tab is badged GregGuns.
3. Placed machines: the Ammo Press front animation while it runs, and the Gantry structure hologram.

## Result of this audit

Every machine meets GUI, EU (or the stated exception), NEI (or the stated exception) and scenario criteria. No machine needed a new GUI or NEI handler. The gaps found were scenarios (MV to IV benches) and the PACK-CHANGES rows (one summary row, now 16), all filled.
