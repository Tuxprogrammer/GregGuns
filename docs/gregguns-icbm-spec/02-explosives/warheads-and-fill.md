# Warheads and fill

- **ICBM baseline:** [explosive-registry](../../icbm-spec/02-explosives/explosive-registry.md) (catalog and tiers), [explosive-block](../../icbm-spec/02-explosives/explosive-block.md) (the block item and its NBT). ICBM 2's modular warhead sizes (1/5/20/64/128) are in the ICBM 2 appendix.
- **Design records:** [0018 §5-8](../../design/0018-icbm-program.md), [0001 tier map](../../design/0001-tier-map.md)
- **Decisions:** [D5](../DECISIONS.md#decided), [D20](../DECISIONS.md#d20-in-detail), [D23](../DECISIONS.md#d23-in-detail), [D34](../DECISIONS.md#decided), [D41](../DECISIONS.md#decided), [D44](../DECISIONS.md#decided)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205): recipe maps, `TierEU`, `GTOreDictUnificator`; NotEnoughItems for recipe display
- **Roadmap:** [I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine); extended in I3 (T3, nuclear) and I5 (antimatter, red matter)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| One block/item per explosive, damage value = explosive ID | One warhead item per **casing tier**, NBT `{explosive, fill}` | D23: one knob, one item per tier, one NEI entry |
| Blast size fixed by config per explosive | Radius and damage are computed from `fill` at detonation | The player chooses how much to spend |
| Tier is only a tooltip color | Tier is the **casing tier**: it caps `fill` and gates the recipe | GT progression (D5) |
| Everything crafted from a grid | Filled in GT machines (Chemical Reactor, Implosion Compressor) | GT first |

## The warhead item

One item per casing tier. I1 ships T1 and T2. The item holds an NBT tag:

| Key | Type | Meaning |
|---|---|---|
| `explosive` | string | The registry name of the explosive (`gregguns:condensed`). Missing or unknown reads as `condensed` |
| `fill` | int | Charges loaded. Clamped to `1..cap` on every read; a hand-edited stack never exceeds its casing |

| Casing tier | Voltage | Fill cap | Size (blocks) |
|---|---|---|---|
| T1 | HV | 5 | 1x1x1 |
| T2 | EV to IV | 20 | 1x1x1 |
| T3 (I3) | LuV to ZPM | 64 | 1x1x2 |
| T4 (I5) | UV | 128 | larger (I5) |
| T5 (I5) | UHV | 128 | larger (I5) |

The explosive must be allowed in the casing: an explosive's tier is the **lowest** casing that can carry it, and any higher casing carries it too. A T2 casing can hold a T1 explosive; a T1 casing cannot hold a T2 explosive.

**Starter tier.** The T1 casing is the entry point and keeps the smallest cap so the first warhead is cheap and bounded. Its cap of 5 is ICBM 2's second size. A fill of 1 is a legal "starter charge" and is craftable; it is the cheapest way to a working demolition charge. The ICBM 2 size of 1 is not a separate tier.

## The radius curve (settles the open question)

With `f = fill / cap` (0 < f <= 1), for an explosive with full-casing radius `R` and full-casing center damage `D`:

```
radius = max(MIN_RADIUS, R * f^(1/3))
center damage = D * f^(1/2)
```

- **Radius** grows with the cube root of fill: blast energy spread through a volume, so doubling the fill gives 26% more radius. This is sub-linear on purpose (D23): a full casing does not dwarf the next tier up, and a cheap partial fill still gives a useful blast. `MIN_RADIUS` is 3 blocks, as in ICBM.
- **Center damage** grows with the square root of fill. A full T1 or T2 casing meets the D34 target (1,200 and 3,000); a partial fill gives less. The warhead never claims the target below full fill.
- The D20 fuel bonus (I2) multiplies the final **radius** by `1 + bonus`, capped by config. The warhead function takes the bonus as a parameter, default 0.
- Both formulas are pure functions in `WarheadMath` with unit tests (monotonic, bounded by `R` and `D`, exact at `f = 1`).

Sample (condensed, T1: `R` 9, `D` 1,200):

| Fill | Radius | Center damage |
|---|---|---|
| 1 | 5.3 | 537 |
| 3 | 7.6 | 930 |
| 5 | 9 | 1,200 |

## Tooltip (D41)

- **Short:** explosive name; tier (`TierText.colored`) of the casing; `Fill: n / cap`; `Blast radius: r blocks`; center damage, or the annihilation line for nuclear and up; "Hold Shift for details".
- **Shift:** the casing cap, the radius and damage curve in one line, the fuse behavior for the demolition use, the blast's special effect, and "Added by: GregGuns".
- All numbers are formatted by `WarheadTooltipFormat` (no Minecraft types) with unit tests. Every line is a lang key.

## Recipes

- A **casing** is built per tier in a GT machine at the tier's voltage: T1 in the Assembler from HV-grade plates and a circuit; T2 uses titanium or tungstensteel plates.
- **Loading** one charge is a **Chemical Reactor** recipe (explosive fluid or compound in, warhead with `fill + 1` out) up to the cap, in the GT recipe map. NEI shows one example (an empty casing becoming `fill 1`), not one per fill level (D23).
- Warheads with a brisance charge (condensed, thermobaric) also use the **Implosion Compressor** for the shaped charge, tier-gated.
- The exact inputs, EU/t and times are in the code and exported by the recipe export; the cost target is D34: the warhead is the expensive part of the missile, rising steeply by tier. A full T1 warhead costs about a quarter of one HV-tier Galacticraft launch in plates and circuits plus its explosives; a full T2 warhead about a launch.
- GT recycling data (D44): each warhead recycles into its casing material and plates, and the contents are lost.

## Requirements

- GGS-ICBM-02-01: a warhead stack with a `fill` above its casing cap reads as the cap.
- GGS-ICBM-02-02: a warhead with a missing or unknown `explosive` reads as condensed.
- GGS-ICBM-02-03: `radius(f)` and `damage(f)` are monotonic, bounded by `R` and `D`, equal them at `f = 1`, and the radius is never below 3.
- GGS-ICBM-02-04: a full T1 conventional warhead has center damage of at least 1,200 and a full T2 at least 3,000.
- GGS-ICBM-02-05: the tooltip is built by `WarheadTooltipFormat`, and a nuclear-and-up warhead shows the annihilation line instead of damage.
- GGS-ICBM-02-06: a recipe never produces a warhead above its cap, and passes the unification and recipe-tier self-checks.

## Tests

`WarheadMathTest`, `WarheadNbtTest`, `WarheadTooltipFormatTest`; recipe self-checks run at the dev boot; scenario `icbm.warhead.items`.

## Open questions

None open. The curve and the starter tier are settled above (orchestrator decision, 2026-10-25, for the owner to revise at the alpha review).
