# 0004 · Damage model

- Status: Accepted (by default during the MVP push; revisit after MVP). Damage pipeline and source classes refined by [0009](0009-gtnh-integration-defaults.md)
- Gate: D4
- Date: 2026-10-17

## Context
Techguns has its own damage system: damage types, armour penetration and per-type resistances (see [02-guns/damage-system.md](../techguns-spec/02-guns/damage-system.md)). [BUGS.md](../techguns-spec/BUGS.md) lists many flaws in it, GUN-33 to GUN-45 among them.

## Decision
- **Keep the model.** Keep Techguns' damage types (physical, projectile, explosion, energy, fire, ice, lightning, poison, radiation, unresistable), armour penetration and per-type resistances. They are central to how the guns and armour feel.
- **Fix the known flaws.** Penetration applies to every living target, not just players (GUN-34). Penetration is computed once per hit, not compounded across armour pieces (GUN-39/ARM-02). Invulnerability frames are respected (GUN-33). Vanilla and other mods' armour behave as vanilla armour against GregGuns damage (GUN-45). NPC difficulty scaling is applied once (GUN-06).
- **Initial numbers.** Use Techguns' damage values as the starting point. Balance them against GTNH mobs and armour in M9.
- **Implementation.** The formulas live in a pure-logic class under `com.gregguns.combat`, with unit tests covering the spec's worked example and each listed bug.

## Consequences
- M1 implements this model and its regression tests before the first gun ships.

- Implemented in M1 as `com.gregguns.combat`. The settled details and deviations (vanilla armour is plain vanilla; Worked example 5.7 now gives 3.92 for a zombie) are in [damage-interop.md](../gregguns-spec/02-guns/damage-interop.md#settled-in-m1-implementation-notes-and-deviations).

## Alternatives considered
- **Vanilla damage only:** rejected, because armour penetration and the energy/explosive identity would be lost.
- **A new model:** rejected, because it isn't needed for the MVP.
