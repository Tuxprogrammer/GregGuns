# Lang keys and GUI strings (machines, from `lang/en_US.lang`)

## 1. Blocks

| Key | English |
|---|---|
| `techguns.tile.BasicMachine_0.name` | Ammo Press |
| `techguns.tile.BasicMachine_1.name` | Metal Press |
| `techguns.tile.BasicMachine_2.name` | Turret |
| `techguns.tile.BasicMachine_3.name` | Camo Bench |
| `techguns.tile.BasicMachine_4.name` | Chemical Laboratory |
| `techguns.tile.BasicMachine_5.name` | Repair Bench |
| `techguns.tile.BasicMachine_6.name` | Explosive Charge |
| `techguns.tile.BasicMachine_7.name` | Flag |
| `techguns.tile.BasicMachine_8.name` | Advanced Explosive Charge |
| `techguns.tile.BasicMachine_9.name` | Charging Station |
| `techguns.tile.machineMultiBlock_0.name` | Reaction Chamber Housing |
| `techguns.tile.machineMultiBlock_1.name` | Reaction Chamber Controller |
| `techguns.tile.machineMultiBlock_2.name` | Reaction Chamber Glass |
| `techguns.tile.machineMultiBlock_3.name` | Fabricator Housing |
| `techguns.tile.machineMultiBlock_4.name` | Fabricator Controller |
| `techguns.tile.machineMultiBlock_5.name` | Fabricator Glass |
| `techguns.block.hasTileEntityData` | Has TileEntity Data (tooltip line on wrench-dismantled machines) |

There's no key for `BasicMachine_10` (the scrapper).

## 2. GUI titles

| Key | English |
|---|---|
| `techguns.container.ammopress` | Ammo Press (defined twice in the file) |
| `techguns.container.metalpress` | Metal Press |
| `techguns.container.chemlab` | Chemical Laboratory |
| `techguns.container.fabricator` | Fabricator |
| `techguns.container.reactionChamber` | Reaction Chamber |
| `techguns.container.chargingStation` | Charging Station |
| `container.inventory` (vanilla) | Inventory |

## 3. Shared machine GUI

| Key | English | Where |
|---|---|---|
| `techguns.container.power` | Power | power tooltip "Power: n/max RF" |
| `techguns.container.redstone` | Redstone Info | redstone tooltip, line 1 |
| `techguns.container.redstone.mode` | Required | "Required: \<mode>" |
| `techguns.container.redstone.signal` | Signal | "Signal: \<state>" |
| `techguns.redstonemode.ignore` | ignored | mode 0 |
| `techguns.redstonemode.high` | high | mode 1 |
| `techguns.redstonemode.low` | low | mode 2 |
| `techguns.redstonemode.signal.high` | high | signal present |
| `techguns.redstonemode.signal.low` | low | no signal |
| `techguns.container.security.owner` | Owned by | security tooltip, line 1 |
| `techguns.container.security.tooltip.owner` | Owner | prefix before the owner name |
| `techguns.container.security.tooltip.unowned` | Unowned (Replace Block to set ownership) | when there's no owner |
| `techguns.container.security.tooltip` | Security | line 2 prefix |
| `techguns.container.security.tooltip.0` | Public | level 0 (green) |
| `techguns.container.security.tooltip.1` | Friends | level 1 (yellow) |
| `techguns.container.security.tooltip.2` | Owner | level 2 (red) |
| `techguns.container.security.tooltip.description.0` | Anyone can access | line 3 |
| `techguns.container.security.tooltip.description.1` | Friends can access | |
| `techguns.container.security.tooltip.description.2` | Only onwer can access (sic) | |
| `techguns.container.security.tooltip.descr2` | Only the owner can change this setting | line 4 |
| `techguns.container.security.denied` | [DENIED] You have no permission to use | chat line on a denied right-click |
| `techguns.gui.empty` | Empty | Chem Lab tank tooltip |

The security tooltip's first line is built as "Owned by: Owner: \<name>", since both keys are concatenated.

## 4. Machine-specific

| Key | English | Where |
|---|---|---|
| `techguns.container.metalpress.autosplit` | Stack split | Metal Press label (code appends ":") |
| `techguns.container.info.on` / `.off` | On / Off | Metal Press mode text |
| `techguns.button.switch` | Switch | Metal Press button |
| `techguns.chemlab.dumpinput` | Dump Input Tank | Chem Lab and RC button tooltip |
| `techguns.chemlab.dumpinput.tooltip` | Content will be lost | |
| `techguns.chemlab.dumpoutput` | Dump Output Tank | Chem Lab |
| `techguns.chemlab.dumpoutput.tooltip` | Content will be lost | |
| `techguns.chemlab.toogledrain` (sic) | Toggle drained Tank | drain-toggle tooltip title |
| `techguns.chemlab.toggledrain.tooltip.0` | Fluids are drained from Output Tank | |
| `techguns.chemlab.toggledrain.tooltip.1` | Fluids are drained from Input Tank | |
| `techguns.chemlab.nofluidin` | No Fluid Input | NEI tank tooltip |
| `techguns.chemlab.nofluidout` | No Fluid Output | NEI tank tooltip |
| `techguns.tgmessage.0` | Invalid Block for Reaction Chamber at | multiblock form failure (both multiblocks), followed by " x, y, z" |
| `techguns.reactionChamberNEI.Instability` | Unstable | NEI risk tooltip |
| `techguns.reactionChamberNEI.Danger` | Risk | NEI risk tooltip |
| `techguns.reactionChamber.RiskType.none` | None | `RiskType.toString`, white |
| `techguns.reactionChamber.RiskType.breakItem` | Destroy Input Item | green |
| `techguns.reactionChamber.RiskType.expLow` | Small Explosion | yellow |
| `techguns.reactionChamber.RiskType.expMed` | Medium Explosion | red |
| `techguns.reactionTooltip.consume` | Consumed | NEI RC tank tooltip "Consumed n mB" |
| `techguns.yes` / `techguns.no` | Yes / No | NEI "Unstable" value |

The other risk types use the literal "NOT_YET_IMPLEMENTED".

Item names referenced by the machines (Machine Upgrade, ammo, plates, foci, etc.) are documented in `06-items`. Notable ones: `techguns.item.machineStackUpgrade.name` "Machine Upgrade (Stack Operation)", `techguns.item.uvemitter.name` "UV Emitter", `techguns.item.rcHeatRay.name` "Reaction Chamber Heat Ray Focus".

## 5. Hard-coded (not localized) strings

| String | Where |
|---|---|
| `Building:` | Ammo Press GUI (MPRD-31) |
| `Empty` | Reaction Chamber tank tooltip (MPRD-31) |
| ` RF`, `RF/t`, `RF/T`, ` ticks`, ` mB`, `mB`, `%` | GUI and NEI number suffixes |
| `>` `<` `+` `-` `x` | button labels |
| `UNKNOW_PLAYERNAME` (sic) | owner name when the username cache misses |
| `Master:…`, `Pos:…`, `Type:…`, `Complete:…`, `Direction:…` | block-reader debug chat |
