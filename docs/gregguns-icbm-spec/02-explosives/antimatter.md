# Antimatter

- **ICBM baseline:** [threaded-blasts](../../icbm-spec/02-explosives/threaded-blasts.md) (antimatter kills red matter, ICBM-02-26), [explosive-registry](../../icbm-spec/02-explosives/explosive-registry.md)
- **Design records:** [0018](../../design/0018-icbm-program.md) section 7
- **Decisions:** [D30](../DECISIONS.md), [D130, D131, D133, D134, D135](../DECISIONS.md)
- **Pack mods:** GregTech (UV Assembler, Field Generator UV, Quantum Eye)
- **Roadmap:** [I5](../../ROADMAP-ICBM.md#i5--endgame-warheads-and-systems)

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| A tier-4 explosive block | The **T4 casing** (UV, fill cap 128, 1x1x2, [D131](../DECISIONS.md)) carrying `antimatter`; the T3 missile body carries it | One warhead item per casing tier (D23); no T4 body |
| A threaded crater of a configured size | The shared blast engine: sphere crater of **80 blocks** at fill 128 (cube-root curve for less), the tick budget, saved mid-crater | D12, [blast-engine](blast-engine.md) |
| Kills what is near | [Annihilation](annihilation.md): every living entity in the kill zone (60% of the radius, 48 blocks at full fill) dies, bosses included; outside it the damage ramp starts at **16,000** | D30 |
| Kills red matter within 221 blocks | The same, **200 blocks**: running [red matter](red-matter.md) bodies end when antimatter goes off | ICBM-02-26, D133 |
| Rot and fallout | **None.** Antimatter never calls the fallout system | D21 |

## Behavior

`antimatter` is a `WarheadBlast` of tier T4, shape `RAY_SPHERE`, `Kind.ANTIMATTER`, full radius 80, annihilating with bosses. The effect (`AntimatterEffect`) runs once at ignition: it sends one swelling **flash orb** to players within 256 blocks ([blast-visuals](../07-visuals-audio/blast-visuals.md#orbs)) and ends every `redmatter` blast within 200 blocks through `BlastEngine.cancelNear`. The crater, damage and knockback are the engine's. It obeys `icbm.terrainDamageT4`; with it off, the kill zone still kills.

## Recipe and cost

An Assembler recipe at UV, circuit 1: 8 naquadria plates (the casing), 2 UV Field Generators, a Quantum Eye, 4 plutonium-241 ingots, 8 Industrial TNT, 4 Plastic Explosive and a Fuze Assembly Mk III. About 130 plate equivalents ([D134](../DECISIONS.md)). It recycles into its 8 naquadria plates.

## Requirements

- GGS-ICBM-02-30: antimatter at a full casing has radius 80 and annihilates its kill zone with bosses.
- GGS-ICBM-02-31: antimatter leaves no irradiated block and starts no fallout.
- GGS-ICBM-02-32: an antimatter blast ends every red matter body within 200 blocks.
- GGS-ICBM-02-33: the recipe runs at UV (voltage index 8) and recycles into its casing plates.

## Tests

`WarheadBlastTest` (definition), `WarheadRecipeSpecTest` (recipe, tier, cost band, circuits); scenarios `icbm.antimatter.annihilate` (a Wither, a 100,000-health mob and a cow die; the crater is dug; no irradiated block) and `icbm.antimatter.ends_red_matter`; `icbm.recipes.assembler` checks the UV voltage.

## Open questions

None. Whether antimatter should ignore block resistance (ICBM's does not either) is left to the owner's playtest.
