# Annihilation

- **ICBM baseline:** none (a GregGuns rule), see [0018](../../design/0018-icbm-program.md) section 7 and [0016](../../design/0016-damage-balance-pass.md)
- **Decisions:** [D30](../DECISIONS.md)
- **Roadmap:** the shared implementation is [I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine); nuclear content arrives in [I3](../../ROADMAP-ICBM.md#i3--the-silo-and-heavy-warheads-t3-nuclear)

## What it is

Inside the kill zone of a nuclear (ZPM), antimatter (UV) or red matter (UHV) warhead, every living entity dies, bosses included, through armor and hurt-resistance frames. The Nuke Launcher keeps its own rule: bosses lose half their maximum health. One implementation serves both, with a **bosses flag**.

## The shared implementation

`com.gregguns.explosion.Annihilation` (pure, no Minecraft types):

- `killRadius(blastRadius)` is 60% of the radius; `inKillZone(distance, radius)` is false for NaN.
- `lethal(bosses, boss, health, absorption, maxHealth)`: health plus absorption plus a margin of 1; but a boss with `bosses` false takes half its maximum health.
- `amount(...)` is the ramp outside the zone and `lethal` inside it.

`DamageSources.annihilateLiving(target, attacker, bosses)` applies it through `annihilate`, so the server's damage settings still gate players. The old two-argument overload and `NukeAnnihilation` (the Nuke Launcher and the Golden Gun) call the shared rule with `bosses = false`; behavior and tests are unchanged.

An `ExplosiveDef` opts in with `annihilating(bosses)`; the blast engine then annihilates living victims inside the kill zone and applies the damage ramp outside it. I1 ships the rule and its tests; no I1 type sets it.

## Requirements

1. **GGI-02-20.** With `bosses` true a boss in the zone takes health plus absorption plus 1.
2. **GGI-02-21.** With `bosses` false a boss takes half its maximum health; a non-boss takes the lethal amount either way.
3. **GGI-02-22.** The Nuke Launcher's results are unchanged (`NukeAnnihilationTest`).

## Tests

`AnnihilationTest` (new), `NukeAnnihilationTest` (existing, untouched); scenarios `projectile.nuke.*` keep covering the launcher.

## Open questions

The kill-zone share for the ICBM warheads is 60% like the launcher's; I3 may tune it per type.
