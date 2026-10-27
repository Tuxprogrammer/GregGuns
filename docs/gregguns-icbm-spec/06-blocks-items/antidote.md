# Antidote

- **ICBM baseline:** [icbm-spec 06-blocks-items](../../icbm-spec/06-blocks-items/index.md) and [gas-and-status](../../icbm-spec/02-explosives/gas-and-status.md): the antidote cures the poison and virus effects of gas warheads.
- **Decisions:** [D10](../DECISIONS.md#d10-in-detail) ("Antidote: kept, mapped to GT hazards"), [D21](../DECISIONS.md#d21-in-detail), [D151](../DECISIONS.md)
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
| Fallout and carried rubble (`GTUtility.applyRadioactivity`, which GT skips for hazmat) | slowness, mining fatigue, nausea, and more at higher levels |

## The antidote (D151)

A syringe, stack of 8, HV. Hold use for 32 ticks (the drink animation), then:

1. **Cures at once**: poison, hunger, weakness, slowness, nausea and mining fatigue.
2. **Guards for 3 minutes**: every 10 ticks the guard clears the same six effects from that player again, so gas and fallout can be walked through, though the player is still exposed and still takes any damage that is not one of these effects.

Per hazard: against gas (GAS/BIOLOGICAL) and radiological exposure it removes the effects; against fire, explosions, EMP and annihilation it does nothing. Wither and the Radiation damage of GregGuns weapons are not cured.

The guard keeps its end time on the player's entity data and runs from the living-update event for players only, one compare per 10 ticks and one potion lookup per cured effect on protected players only. Dying does not end it by design (a new dose is cheap); logging out does not end it.

It also clears a player's own slowness or hunger while active; that is the price of working at the effect level and is stated in the tooltip.

## Crafting

One GT Assembler recipe at HV (D69), 200 ticks, circuit 6: 1 rubber plate, 1 stainless steel plate and 2 charcoal dust make **2** antidotes. Charcoal is the classic activated-carbon antidote. No recycling data.

## Tooltip (D41)

Short: tier, summary, Shift hint. Shift: what it cures, the guard time, and that armor still keeps the cause off.

## Requirements

- **GGI-06-20.** A dose clears the six ailments at once and starts exactly one 3-minute guard.
- **GGI-06-21.** Only a dosed player is guarded.
- **GGI-06-22.** Creative players keep their stack.

## Tests

`AntidoteRulesTest` (units); scenario `icbm.antidote.dose`.

## Open questions

None.
