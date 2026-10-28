# Antidote

- **ICBM baseline:** [icbm-spec 06-blocks-items](../../icbm-spec/06-blocks-items/index.md) and [gas-and-status](../../icbm-spec/02-explosives/gas-and-status.md): the antidote cures the poison and virus effects of gas warheads.
- **Decisions:** [D10](../DECISIONS.md#d10-in-detail) ("Antidote: kept, mapped to GT hazards"), [D21](../DECISIONS.md#d21-in-detail), [D151](../DECISIONS.md), [D154](../DECISIONS.md)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205): `Hazard`, `HazardProtection`, Assembler
- **Roadmap:** [I5](../../ROADMAP-ICBM.md#i5--endgame-warheads-and-systems)
- **Code:** `com.gregguns.icbm.antidote`

## What GT gives, and what it cannot do

GT's hazards (`Hazard`: BIOLOGICAL, GAS, RADIOLOGICAL and the rest) are answered by **armor only**: `HazardProtection` loops the four vanilla armor slots ([hazard-protection](../../gregguns-spec/05-armor/hazard-protection.md)). Nothing outside armor can claim a hazard without a mixin, which the project does not use. So the antidote is **not** a hazard protector; armor stays the way to avoid a hazard (D21).

What every ICBM-side hazard becomes in the end is a potion effect:

| Source | Effects applied |
|---|---|
| Debilitation gas | slowness, weakness, nausea |
| Chemical gas | poison |
| Contagious gas | poison, hunger |
| Fallout and carried rubble (`GTUtility.applyRadioactivity`, which GT skips for undead, arthropods and full radio hazmat) | slowness, mining fatigue, nausea, weakness, hunger **and IC2 radiation**, the effect that actually damages |

**What `applyRadioactivity` adds.** Read at the pack tag ([GTUtility.java#L2021-L2072](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L2021-L2072)): six potion effects with the same amplifier, `moveSlowdown`, `digSlowdown`, `confusion`, `weakness`, `hunger` and `IC2Potion.radiation` ([L2063](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTUtility.java#L2063)). IC2 itself damages a player carrying the radiation effect. The antidote therefore clears all six, and nothing else: poison, hunger, weakness, slowness, nausea, mining fatigue and IC2 radiation (resolved once, with a null check, so a pack without the potion still works). It never clears a beneficial effect: every cured effect is harmful, and the scenario checks that regeneration survives a dose and the guard.

## The antidote (D151)

A syringe, stack of 8, HV. Hold use for 32 ticks (the drink animation), then:

1. **Cures at once**: poison, hunger, weakness, slowness, nausea, mining fatigue and radiation (IC2).
2. **Guards for 3 minutes**: every 10 ticks the guard clears the same seven effects from that player again, so gas and fallout can be walked through, though the player is still exposed and still takes any damage that is not one of these effects.

Per hazard: against gas (GAS/BIOLOGICAL) and radiological exposure it removes the effects; against fire, explosions, EMP and annihilation it does nothing. Wither is not cured.

The guard keeps its end time on the player's entity data and runs from the living-update event for players only, one compare per 10 ticks and one potion lookup per cured effect on protected players only. The end time is kept under Forge's `PlayerPersisted` sub-tag (`EntityPlayer.PERSISTED_NBT_TAG`), which Forge 1.7.10 copies in `clonePlayer`, so dying and leaving the End do not end it (a new dose is cheap); logging out does not end it either.

**PvP.** The guard makes a player immune for 3 minutes to GregGuns' own status weapons too: the slowness, mining fatigue, weakness and hunger of `SecondaryEffectApplier` and the slowness of `FlashBlast` are potion effects and are cleared the same way. That is accepted (a cheap HV syringe is the price of three minutes of cover), and it is stated here and in the tooltip; it also clears a player's own slowness or hunger while active.

## Crafting

One GT Assembler recipe at HV (D69), 200 ticks, circuit 6: 1 rubber plate, 1 stainless steel plate and 2 charcoal dust make **2** antidotes. Charcoal is the classic activated-carbon antidote. No recycling data.

## Tooltip (D41)

Short: tier, summary, Shift hint. Shift: what it cures, the guard time, and that armor still keeps the cause off.

## Requirements

- **GGI-06-20.** A dose clears the seven ailments at once, and no beneficial effect and starts exactly one 3-minute guard.
- **GGI-06-21.** Only a dosed player is guarded.
- **GGI-06-22.** Creative players keep their stack.

## Tests

`AntidoteRulesTest` (units); scenario `icbm.antidote.dose` (the player is made sick with the ICBM effects and with GT's own `applyRadioactivity`; the dose and the guard must clear IC2 radiation and leave regeneration alone).

## Open questions

None.
