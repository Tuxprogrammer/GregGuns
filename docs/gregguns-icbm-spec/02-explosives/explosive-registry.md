# Explosive registry

- **ICBM baseline:** [explosive-registry](../../icbm-spec/02-explosives/explosive-registry.md), [blast-types](../../icbm-spec/02-explosives/blast-types.md)
- **Design records:** [0018](../../design/0018-icbm-program.md) sections 5-7
- **Decisions:** [D5, D10, D11, D12, D20, D23, D34](../DECISIONS.md)
- **Pack mods:** GT5-Unofficial (`gregtech`, 5.09.54.205) for the voltage ladder
- **Roadmap:** [I1](../../ROADMAP-ICBM.md#i1--warheads-and-the-blast-engine)

## What changes from the ICBM baseline

| ICBM baseline | GregGuns | Why |
|---|---|---|
| Legacy integer ids in `explosive_reg.json`, handed out first-free, doubling as item damage values | Names. The id (`condensed`, `nuclear`) is the registry key and the warhead NBT `explosive` value. No numbers | Reused retired slots in the original (ICBM-02-18); a name cannot be reassigned |
| Tier is cosmetic, plus a switch for block, cart and grenade forms | Tier is the GT ladder (D5) and decides the damage target (D34), the recipe voltage and the `icbm.terrainDamage` key | One scale for cost, damage and griefing |
| Block, grenade, cart and missile forms per explosive | One registry entry; the warhead item and block (coder 2's area) read it. No grenades or carts (D10, D12) | One shared registry (D12) |
| Size is a per-blast config knob | Radius from the warhead **fill** (D23): `baseRadius * fill^exponent`, exponent 0 to 1, plus the D20 fuel bonus | Fill is the quantity knob; sub-linear so a full casing is not absurd |
| Registry unlocked by event buses | Plain `Explosives.register` during init; a duplicate id throws (ICBM-02-24) | No event bus to port; the failure is loud |

## The API

Package `com.gregguns.icbm.explosive`, Java 8 final classes, no Minecraft types except in `BlastContext` and `IcbmBlasts`.

- `ExplosiveTier` (T1 to T4): number, minimum GT voltage tier (HV 3, EV 4, LuV 6, UV 8), label, minimum center damage (1,200 / 3,000 / 8,000 / none for annihilation).
- `BlastShape`: `RAY_SPHERE` (the ported large caster), `RAY_GRID` (the small caster), `NONE` (effect and damage only).
- `ExplosiveDef` (immutable, built with `builder(id, tier)`): shape, base radius, radius exponent, max fill, center damage, annihilation flags, ray energy, `BlastEffect`. `radius(fill)` and `damage(fill)` are the pure math the tooltip and the engine share. `hidden()` marks test-only defs.
- `Explosives`: `register`, `get(id)`, `all()` (hidden ones left out), `allIncludingHidden()`.
- `BlastEffect`: the per-type hook (`onDetonate`, `onTick`, `onFinish`). Stateless; per-blast state goes in `BlastContext.state()`, which is saved with the blast.
- `IcbmBlasts.detonate(world, x, y, z, def, fill, bonusRadius, source)`: the one entry point.

## How a type is defined

A type is a definition plus an optional effect. The engine does the crater and the damage ramp; the effect adds what is specific (gas, fire, smoke, shrapnel). Example from the content area:

```java
Explosives.register(ExplosiveDef.builder("condensed", ExplosiveTier.T1)
    .shape(BlastShape.RAY_GRID).baseRadius(6).radiusExponent(1.0 / 3.0).maxFill(5).build());
```

## Damage

Center damage is `centerDamage * sqrt(fill / maxFill)`: a full casing meets the D34 target (T1 1,200, T2 3,000, T3 8,000) and a partial fill deals less. It stays flat in the inner 10% of the radius and falls linearly to zero at the radius. Checked against Infernal-scale pools in `DamageRampTest`: T1 beats a 1,080 pool, T2 a 2,700 pool.

## Config

See [config.md](../../config.md#icbm-blasts-icbm). `icbm.maxBlastTier` refuses a detonation of a higher tier. A blast removes terrain according to its `icbm.terrainDamageT<n>` key alone, on by default (D11, D52). `explosions.blockDamage` governs hand-held weapons and is not consulted, so warhead craters need no pack change. Claims and protection always apply.

## Requirements

1. **GGI-02-01.** Every explosive is one `ExplosiveDef` in `Explosives`; the id is unique, lowercase, without `_` or `:`.
2. **GGI-02-02.** `detonate` refuses a client world, a cancelled `ExplosionEvent.Start`, a tier above `icbm.maxBlastTier` and a full blast queue (`icbm.maxActiveBlasts`).
3. **GGI-02-03.** `radius(fill)` and `damage(fill)` clamp fill to 1 to `maxFill`.
4. **GGI-02-04.** Terrain breaks only through `ProjectileExplosion.breakBlock`, never another path.

## Tests

`ExplosivesTest`, `DamageRampTest` (units); scenario `icbm.blast.pipeline`.

## Bugs avoided

ICBM-02-18 (names, not first-free integers), ICBM-02-24 (duplicates and late registration are errors).

## Open questions

None. The fill-to-radius exponent per type is set by the warheads-and-fill sheet.
