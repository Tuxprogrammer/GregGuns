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

A warhead's explosive tier equals its casing tier: a T1 casing carries T1 explosives and a T2 casing T2 explosives. The fill cap is the casing's cap. A stack that carries an explosive of another tier reads as condensed.

**Starter tier.** The T1 casing is the entry point and keeps the smallest cap so the first warhead is cheap and bounded. Its cap of 5 is ICBM 2's second size. A fill of 1 is a legal "starter charge" and has its own cheap recipe; it is the cheapest way to a working demolition charge. The ICBM 2 size of 1 is not a separate tier.

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
- The radius shown is the final blast radius at that fill and the damage is the center damage at that fill.
- All numbers are formatted by `WarheadTooltipFormat` (no Minecraft types) with unit tests. Every line is a lang key.

## Recipes

NBT cannot flow through a GT recipe, so a warhead is not loaded one charge at a time. Instead each explosive has **one recipe that makes a full-casing warhead**, and NEI shows one entry per explosive (D23: one example, not one per fill level). The partial fills a player may want are covered by one cheap **starter** recipe (condensed, fill 1) and by creative and command stacks.

| Machine | Warheads | Voltage | Inputs (summary) |
|---|---|---|---|
| Assembler | T1 condensed, shrapnel, anvil, repulsive, attractive | HV | 4 stainless steel plates, 8 TNT, the type's payload, 1 L glyceryl trinitrate |
| Assembler | T1 condensed starter (fill 1) | HV | 2 stainless steel plates, 2 TNT |
| Chemical Reactor | T1 incendiary, debilitation, chemical, smoke | HV | 4 stainless steel plates, the payload, a reagent fluid |
| Chemical Reactor | T2 contagious | EV | 8 titanium plates, the payload, sulfuric acid |
| Implosion Compressor | T2 fragmentation, sonic, breaching, thermobaric | EV | 8 titanium plates, the payload, 16 units of explosive additive |

- Every recipe runs at 400 ticks and at exactly its warhead's tier voltage, so it is tier-gated by the machine. The dev recipe-tier self-check and the unification check pass (no GT part is duplicated; the output is a GregGuns block item).
- Cost (D34): the warhead is the expensive part, and cost rises by tier. A full T1 warhead costs four stainless plates, eight TNT, a payload and 1 L of glyceryl per craft; a T2 doubles the plates (eight titanium) and adds the Implosion Compressor's additive or a reagent. The starter (fill 1) stays at two plates and two TNT as the cheap way in. The missile body (I2) is balanced against a Galacticraft launch separately.
- **Cost review against a Galacticraft launch (D34, I1 fix round).** The comparison is an estimate: the pack's rocket recipes live in the GTNH scripts, which this repository does not carry, so the rocket side is the shape of GTNH's tier 1 and tier 2 rockets (nose cone, engine, fins, a stack of tier heavy-duty plates, a chip, and fuel worth several buckets), about 15 to 25 plate-equivalents at HV-class materials for a T1 rocket and roughly 1.5 to 2 times that at EV for T2.

  | | Casing plates | Explosive | Payload | Rough plate-equivalents | Against its tier's launch |
  |---|---|---|---|---|---|
  | T1 warhead (was) | 2 | 8 TNT | 4 to 32 items | 12 to 16 | well under one launch: too cheap for "the expensive part" |
  | T1 warhead (now) | 4 | 8 TNT, 1 L glyceryl | 4 to 32 items | 16 to 22 | about one T1 launch, so missile plus warhead is about two |
  | T2 warhead (was) | 4 | additive or reagent | 4 to 8 items | 14 to 20 | under one T2 launch |
  | T2 warhead (now) | 8 | additive or reagent | 4 to 8 items | 22 to 30 | about 1 to 1.5 T2 launches; steeper than T1 |

  Plate counts are the one lever changed: they double, which keeps each recipe inside its tier's voltage and machine. The starter recipe is deliberately cheap. If the pack's real rocket costs turn out higher, raise `T1_PLATES` and `T2_PLATES` in `WarheadRecipes` (the recycling data follows the same constants).
- GT recycling data (D44): each warhead recycles into its casing plates (stainless steel for T1, titanium for T2); the contents are lost.

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
