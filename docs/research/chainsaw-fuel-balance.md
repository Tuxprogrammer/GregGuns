# Chainsaw fuel balance (M5 gap G6, GGS-02-32, plan Q7)

Date: 2026-10-20. Scope: research only, no code change. Pack: GTNH 2.9.0-RC-2, GT5-Unofficial `5.09.54.205`.

**Question.** Is the GregGuns Chainsaw's fuel spend about right next to what GT's own electric chainsaw costs? The owner's method: sample the GT chainsaw's EU use, compare it with the EU the same fuel makes in a combustion generator, and accept or reject our spend. GT charges EU per block broken, so the fair unit is EU per log.

## Verdict

**Confirm** the current spend (3.33 mB per block, 300 rounds per 1,000 mB tank). With Diesel, the reference fuel, one log costs 1,600 EU of fuel and GT's HV chainsaw costs 1,600 EU: a ratio of 1.00. The four fuels the tank accepts span 0.40x to 1.20x. Only Ethanol falls just under the 0.5x line, and only because every fuel gets the same number of rounds. No change is needed now. Fuel-value scaling would fix that spread and is worth doing later only as polish (see the last section). One related finding is the flat per-block cost, which overcharges soft blocks such as leaves.

## Our Chainsaw

All of this is read from our own code and specs.

| Item | Value | Where |
|---|---|---|
| Tier, recipe | HV, GR-90 | `ChainsawDef` |
| Fluids accepted | Diesel (GT material name `Fuel`), regular Gasoline, Bio Diesel, Ethanol. Fixed on the tank item, not read from GT's fuel maps | `ComponentItems` (the `tank_fuel` item), fluid-ammo.md, index.md |
| Tank | 1,000 mB = 300 rounds, so 3.33 mB per round | `TankMath.CAPACITY`, `ChainsawDef.STATS.magazine(300)` |
| Block cost (left click) | 1 round (1,000 thousandths) for each mined block with hardness above 0, whatever the hardness | `ChainsawDef.TOOL.costs(1000, 1000)`, `ItemToolGun.onBlockDestroyed` |
| Melee hit cost | 1 round | same |
| Right click cut | 1 round every 3 ticks while held (fire rate 3, AUTO), 10 damage invisible projectile. It does not break blocks | `ChainsawDef.STATS` |
| Time to fell a log | Axe dig speed 14 on a log of hardness 2.0: 14 / 2.0 / 30 = 0.233 progress per tick, so 5 ticks (0.25 s) | `ChainsawDef.TOOL`, vanilla break-progress formula |
| Fuel-value scaling | None. A full tank is a fixed 300 rounds for any accepted fluid (GGS-02-32 deferred, Q7) | tool-guns.md, index.md |

Per log: 3.33 mB of fuel. A tank fells 300 logs. Mining continuously, the Chainsaw burns 3.33 mB every 5 ticks, about 0.67 mB per tick.

## GT's electric chainsaw

GT tools pay per block: the damage is `max(1, blockHardness * toolDamagePerBlockBreak)`, taken from the tool's battery. For an electric tool one point of tool damage is exactly one EU: `doDamage` calls `use(stack, amount)`, and `use` calls `discharge(stack, amount, ...)`, which removes `amount` from the stored charge with no multiplier. (The tool also gets 1 in 25 odds of real durability damage, which does not matter here.)

| Tier | Damage per block break | Source | EU per log (hardness 2.0) | EU per hardness point |
|---|---|---|---|---|
| LV | 50 (inherited from `ToolSaw`) | [ToolSaw L27-L30](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tools/ToolSaw.java#L27-L30), [ToolChainsawLV L27](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tools/ToolChainsawLV.java#L27) | 100 | 50 |
| MV | 200 | [ToolChainsawMV L13-L16](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tools/ToolChainsawMV.java#L13-L16) | 400 | 200 |
| HV | 800 | [ToolChainsawHV L13-L16](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tools/ToolChainsawHV.java#L13-L16) | 1,600 | 800 |

// Source (the formula): [MetaGeneratedTool.onBlockDestroyed L763-L773](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/MetaGeneratedTool.java#L763-L773)
// Source (damage to EU for an electric tool): [MetaGeneratedTool.doDamage L703-L735](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/MetaGeneratedTool.java#L703-L735), then [MetaBaseItem.use L401-L413](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/MetaBaseItem.java#L401-L413) and [MetaBaseItem.discharge L374-L388](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/MetaBaseItem.java#L374-L388)
// Source (the three chainsaws are registered): [MetaGeneratedTool01 L316-L342](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/MetaGeneratedTool01.java#L316-L342)

A vanilla log has hardness 2.0, so the HV row is the one that matches our HV Chainsaw: **1,600 EU per log**.

## Fuel energy

A GT fuel's `mSpecialValue` (the fuel value, same number as `FUEL_VALUE`) is the EU made from one **mB** burned. The generator multiplies it by its efficiency over 100 and burns 1 mB at a time. Fuel entries for the fluids come from the same material number that the filled cell gets in the Diesel fuel map.

// Source (per-mB value and efficiency): [MTEBasicGenerator.getFuelValue L283-L290](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicGenerator.java#L283-L290), `consumedFluidPerOperation` is 1 ([L274-L276](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicGenerator.java#L274-L276)), burn loop [L204-L219](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicGenerator.java#L204-L219)
// Source (Diesel Generator uses `dieselFuels`): [MTEDieselGenerator L74-L82](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tileentities/generators/MTEDieselGenerator.java#L74-L82)
// Source (cell recipe feeds the fuel map): [ProcessingCell L52-L62](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/oreprocessing/ProcessingCell.java#L52-L62); fluid lookup [FuelBackend L73-L103](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/recipe/maps/FuelBackend.java#L73-L103); `Fuel` dispatch to `dieselFuels` [GTRecipeConstants L746-L786](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTRecipeConstants.java#L746-L786)

Generator efficiency: Basic (LV) 95, Advanced (MV) 90, Turbo (HV) 85 ([LoaderMetaTileEntities L9118-L9140](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/preload/LoaderMetaTileEntities.java#L9118-L9140)). The owner's "Basic Combustion Generator" is the 95% one.

| Fuel (GT material) | EU per mB (raw) | Source |
|---|---|---|
| Diesel (`Fuel`) | 480 | [MaterialsInit L4860-L4869](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L4860-L4869) |
| Gasoline (`Gasoline`, regular) | 576 | [MaterialsInit L12821-L12831](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L12821-L12831) |
| Bio Diesel | 320 | [MaterialsInit L8037-L8047](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L8037-L8047) |
| Ethanol | 192 | [MaterialsInit L4871-L4878](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L4871-L4878) |

**Pack overrides:** none found. `reference/gtnh/2.9.0-RC-2/.minecraft/config/GregTech/` (`GregTech.cfg`, `MachineStats.cfg`, `Other.cfg`, `OverpoweredStuff.cfg`, `Pollution.cfg`) has no fuel value or generator efficiency setting; the only fuel-related keys there are pollution rates.

## The comparison

Our spend is 3.333 mB per log at every fuel. GT HV spends 1,600 EU per log. "Generator" columns use the Basic Combustion Generator at 95%.

| Fuel | Raw EU per log | Ratio to GT HV | EU per log at 95% | Ratio at 95% | mB that makes 1,600 EU at 95% |
|---|---|---|---|---|---|
| Diesel | 1,600 | **1.00** | 1,520 | 0.95 | 3.51 |
| Gasoline | 1,920 | 1.20 | 1,824 | 1.14 | 2.92 |
| Bio Diesel | 1,067 | 0.67 | 1,013 | 0.63 | 5.26 |
| Ethanol | 640 | 0.40 | 608 | 0.38 | 8.77 |

The same Diesel figure against the other GT tiers, for context: 1,600 / 100 = 16x the LV chainsaw, 1,600 / 400 = 4x the MV one. That is correct, since our Chainsaw is an HV item with an HV-level recipe cost.

Per-tick versus per-block: our mining spend is per block, like GT's, so the comparison is direct (5 ticks per log, 3.33 mB per log, about 320 EU per tick of Diesel). The right-click cut is a weapon, not a mining mode, and it is paid per shot: 1 round per 3 ticks is 22 mB per second, about 10,700 EU per second of Diesel. That is hard to compare with GT, which charges 3,200 EU per HV chainsaw entity attack ([ToolChainsawHV L28-L31](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tools/ToolChainsawHV.java#L28-L31)), about 1,600 EU per shot at 6.7 shots a second. The cut therefore costs half of GT's per attack but fires at a much higher rate. A full tank sustains 45 seconds of continuous cutting. I would leave this as is.

**Why confirm.** Diesel is the reference fuel and lands on 1.00x. Gasoline at 1.20x is inside the band and is the better fuel, so a bit more energy per tank is fine. Bio Diesel at 0.67x is inside. Ethanol at 0.40x is slightly under 0.5x, meaning Ethanol tanks give cheap logs: a tank of Ethanol fells 300 logs on 192,000 EU worth of fuel where GT's HV chainsaw would need 480,000 EU for those same 300 logs. Ethanol is the cheapest fuel to make, so that lean toward the player is an acceptable, and arguably intended, reward for the cheap route. A full Diesel tank of 480,000 EU against the same 300 logs for GT is a fair price.

**Tank life.** 300 logs per tank is about 50 to 60 average trees. That is a reasonable length of use for one 1,000 mB tank. No change.

## Findings worth a follow-up

1. **Flat per-block cost overcharges soft blocks.** GT scales with hardness (hardness x 800 at HV). We charge a whole round for any block with hardness above 0. Leaves (hardness 0.2) cost GT 160 EU but us 1,600 EU of Diesel, 10x too much. Dirt-like blocks (0.5) cost GT 400 EU, us 4x too much. Logs are exact. Stone (1.5) is 0.75x. A hardness-scaled cost of `500 thousandths of a round per hardness point` (log = 1,000, leaves = 100, stone = 750, with a minimum of 100) would match GT everywhere. It is a one-line change in `ItemToolGun.onBlockDestroyed` plus a `ToolStats` field, and it makes clearing a forest by chainsaw cheaper, which is what GT does too. This is worth more than fuel-value scaling.
2. **Fuel-value scaling (GGS-02-32): worth adding only as polish.** It would remove the Ethanol 0.40x and Gasoline 1.20x spread and make "better fuel lasts longer" true. With Diesel (480 EU per mB) as the reference, a full tank would hold `300 * value / 480` rounds: Diesel 300, Gasoline 360, Bio Diesel 200, Ethanol 120; or, in mB per log, Diesel 3.33, Gasoline 2.78, Bio Diesel 5.00, Ethanol 8.33. The cost is real: the shared reload machine has no channel for how much was loaded (index.md), and the tank would need to record a round count at reload. Since every fuel is already within 0.4x to 1.2x, keep it deferred (Q7) and revisit if players find Ethanol too good. If it is built, read the value from `FuelBackend.findFuel` as GGS-02-31 describes, so it picks up other mods' fuels.
