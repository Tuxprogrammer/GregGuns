# Nuclear blast and fallout

- **ICBM baseline:** [radioactive-and-mutation](../../icbm-spec/02-explosives/radioactive-and-mutation.md) (what ICBM's rot and mutation blasts do; ours is simpler and capped)
- **Design records:** [0018 §7](../../design/0018-icbm-program.md) (annihilation); [05-armor/hazard-protection](../../gregguns-spec/05-armor/hazard-protection.md)
- **Decisions:** [D12, D21, D30](../DECISIONS.md), [D83, D84](../DECISIONS.md)
- **Pack mods:** GregTech (`GTUtility.applyRadioactivity`, `HazardProtection`, `GTUtility.getRadioactivityLevel`)
- **Roadmap:** [I3](../../ROADMAP-ICBM.md#i3--the-silo-and-heavy-warheads-t3-nuclear); the byproduct chain is I5

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| Radioactive blocks spread and ticks randomly | Fixed footprint, no spreading, no random ticks | D21: the count is an upper bound |
| A nuclear blast and a tactical nuke are separate code | One nuclear scale shared by the Nuke Launcher and the warhead | D12 |
| Per-item radiation ratings | Any hazmat-capable armor protects, as GT says | D21 |

## The shared nuclear blast

`com.gregguns.explosion.nuclear`:

| Piece | Job |
|---|---|
| `NuclearScale` | Pure functions of the blast radius: the kill radius (60%, the shared `Annihilation` rule) and the fallout radius (1.5 x radius, capped by `falloutMaxRadius`) |
| `Fallout.start` | Queues the conversion of the surface around ground zero; both the launcher and the warhead call it |
| `FalloutEngine` | The per-tick scan, the decay and the world save record (`FalloutSavedData`) |
| `RadiationExposure` | Exposure of players near irradiated earth |

| | Nuke Launcher | Nuclear warhead (ZPM, T3 casing) |
|---|---|---|
| Blast radius | 10 | 64 at fill 64 (16 at fill 1), cube-root curve of [warheads-and-fill](warheads-and-fill.md) |
| Kill zone | 6 | 38.4 at full fill |
| Bosses in the zone | lose half their maximum health | die |
| Crater | vanilla-style explosion, `explosions.blockDamage` | the blast engine, `icbm.terrainDamageT3` |
| Fallout | disc 15, only when blocks broke | disc up to 96, only when the crater was allowed to break terrain and completed |

The warhead's definition sets `annihilating(true)`; the kill zone, the damage ramp outside it (8,000 at the center) and the knockback are the engine's. Its effect (`Effects.Nuclear`) starts fallout when the blast finishes, never for a blast cancelled by a world unload.

## Fallout

- **Footprint:** a disc of radius `ceil(1.5 x blast radius)`, at most `falloutMaxRadius` (96). The scan walks the square of columns in index order, skips columns outside the disc for free and never loads a chunk.
- **What converts:** in a column, the first solid surface block (water or an opaque non-earth block stops the column) if it is dirt, grass, stone, sand, gravel or cobblestone, then up to two blocks below it, each with chance one half and each only if it is also convertible. Caves and deep stone are never touched. Every conversion asks the protection mods first (`BlockBreakGuard`, with a fake player), then sets the block with no neighbor update.
- **How many:** the chance at ground zero (`FalloutMath.peakChance`) is chosen so the expected total is 80% of `falloutMaxBlocks`; the chance falls linearly to 30% of the peak at the rim. The cap is a hard stop. Rolls are deterministic from a per-job seed.
- **When:** the job waits 100 ticks after the blast ends, then scans at most `falloutColumnsPerTick` columns and `falloutBudgetMs` milliseconds a tick across all jobs and worlds. Its cursor and its converted positions are saved.
- **Blocks:** `gregguns:irradiated_earth`, five variants by metadata, vanilla textures tinted green on the client; hardness 1, no random ticks. Mined it drops `irradiated_rubble`, a radioactive item. It carries **no GT item data** (data naming uranium-235 makes GT generate recycling recipes, so free rubble would become U-235). Instead its `onUpdate` calls `GTUtility.applyRadioactivity` every 120 ticks with the stack size, as GT's carried-item check does, and GT's hazmat check protects. The block cannot be silk harvested (it would never decay).
- **Exposure:** every 20 ticks, a non-creative player with an irradiated block in the 3x3 around their feet or the layer below gets `GTUtility.applyRadioactivity(player, falloutRadiationLevel, 1)`. GT's own check skips anyone in full radiological hazmat (GT hazmat, GregGuns armor that declares it, the Hazmat enchantment, any hazard protector), undead and arthropods.
- **Decay:** when a job finishes, its blocks form a decay record due `falloutDecayMinutes` later (default 60). The record reverts at most `falloutRevertPerTick` blocks (32) a tick per world to the original block (grass returns as dirt) and skips blocks that are no longer irradiated. A block whose chunk is unloaded is never loaded for it: the entry is kept and retried every 10 seconds, so it reverts once the chunk loads, until a seven-day age cap gives up on it.
- **Antimatter and red matter** leave no fallout.

### The byproduct chain (I5, D140 to D143)

A ZPM or better nuclear warhead (`Fallout.GRADE_ZPM`) has a hot **inner fireball**: out to `FalloutMath.INNER_SHARE` (40%) of the fallout radius, converted earth is **hot** (metadata bit 8, shown yellow-tinted), and a top block can become **trinitite** (sand, at any of the three depths) or **fallout goo** (top block only). Hot earth and trinitite thin out smoothly: the chance is `1 - (distance / inner radius)^2` (`FalloutMath.hotChance`, D155), one at ground zero and zero at the rim. Launcher-scale blasts (`GRADE_LAUNCHER`) leave only plain irradiated earth, and the Nuke Launcher's rubble stays plain.

- **Hot earth** drops **Hot Irradiated Rubble** (rubble item, damage 1), radioactive one level above plain rubble.
- **Trinitite** (block, hardness 2, glassy): a share of the inner sand blocks, thinning toward the rim. Mined it drops a **Trinitite Shard**. It cannot be silk harvested.
- **Fallout goo** (block, hardness 0.8, shovel; metadata 1 is hot goo): rare, chance `GOO_PEAK` (3%) at ground zero falling to zero at the inner rim, about one percent on average, at most `explosions.falloutMaxGoo` (24) per blast. Mined it drops **Fallout Goo** (or **Hot Fallout Goo**). **While any goo stands in a chunk, that chunk's irradiated earth does not decay**: the decay record keeps its entries and retries, with no age cap for such a chunk, until the goo is dug up. The world's goo ledger (`FalloutSavedData`) learns of goo when fallout places it and when it is broken, and is saved.
- **Items** carry no GT item data (the I3 rubble lesson) and irradiate a holder through GT's own check while carried. Shard and crushed trinitite are one item (damage 0, 1); goo, hot goo and their dissolved forms are another (damage 0 to 3).

Processing, all GT recipe maps, so the chain reads in NEI:

| Step | Machine | In | Out |
|---|---|---|---|
| 1 | Centrifuge, HV | Irradiated Rubble | lead (small, 50%), thorium (tiny, 30%), uranium (tiny, 20%) |
| 1 | Centrifuge, HV | Hot Irradiated Rubble | lead, thorium 40%, uranium 40%, uranium-235 (tiny, 0.8%), plutonium (tiny, 0.5%) |
| 2 | Macerator, LV | Trinitite Shard | Crushed Trinitite |
| 3 | Centrifuge, HV | Crushed Trinitite | uranium 30%, thorium 20%, plutonium 0.4%, uranium-235 0.2% (all tiny) |
| 4 | Chemical Reactor, LuV | Goo or Hot Goo and nitric acid | Dissolved Fallout Goo (or Dissolved Hot) |
| 5 | Centrifuge, EV | Dissolved Goo | technetium, promethium (40% each), neptunium, plutonium-241 (30%), americium 20%, curium 10% (tiny), plus krypton 200 L and xenon 100 L |
| 5 | Centrifuge, EV | Dissolved Hot Goo | neptunium, plutonium-241 40%, americium 30%, curium 20%, californium 5%, fermium 1% (tiny), plus the same gases |

**The conservation rule (D142):** one warhead's whole fallout returns at most **a quarter** of the fissile material in its core (8 uranium-235 and 4 plutonium ingots), at the default `falloutMaxBlocks`. `FalloutYield.maxFissileShare` computes the bound from the data tables and `FalloutYieldTest` pins it, so editing a yield cannot make a warhead a net fuel source. The bulk of the value is in the heavier actinides and the fission products, which GT has recipes for elsewhere but no free source.

Config (`explosions` category): `falloutMaxGoo` 24 (I5), `fallout`, `falloutMaxBlocks` 6000, `falloutMaxRadius` 96, `falloutColumnsPerTick` 400, `falloutBudgetMs` 3, `falloutDecayMinutes` 60, `falloutRevertPerTick` 32, `falloutRadiationLevel` 2.

## Requirements

1. **GGS-ICBM-02-30.** Every living entity inside the nuclear kill zone dies, bosses included; outside it the damage ramp applies.
2. **GGS-ICBM-02-31.** Fallout converts at most `falloutMaxBlocks` blocks, none outside the disc, none below the surface layers, and spends at most its tick budget.
3. **GGS-ICBM-02-32.** Fallout never spreads, never random-ticks, never loads a chunk and asks the protection mods before each conversion.
4. **GGS-ICBM-02-33.** Irradiated blocks decay back, at a bounded rate.
5. **GGS-ICBM-02-34.** The Nuke Launcher's annihilation results are unchanged; it leaves fallout only when its blast broke blocks.

## Tests

Units: `FalloutYieldTest` (the quarter-of-the-core bound, no fissile in plain rubble or goo), `FalloutGooLedgerTest`, `FalloutGooMathTest`, `FalloutTooltipFormatTest`, `FalloutMathTest` (expected total under the budget, chance falls, deterministic rolls, radii), `BlastJobStateTest` (DECIDE round trip), `WarheadBlastTest` (T3 damage target and the nuclear definition). Scenarios: `icbm.blast.crater_nuclear`, `icbm.nuclear.kill_zone` (a Wither, a 100,000-health mob with resistance and hurt frames, a cow, a survival and a creative fake player), `icbm.nuclear.fallout_caps` (converted count, disc, surface depth, worst fallout tick, exposure bare and in hazmat, decay), `icbm.nuclear.rubble` (no GT data, no macerator, arc or recycler recipe, carried irradiation, no silk touch), `icbm.nuclear.fallout_products` (the chain: recipes exist at the stated tiers and not below, no item data, no arc or macerator route), `icbm.nuclear.fallout_goo` (zones, caps, ledger, goo holds decay, release), `icbm.nuclear.decay_unloaded` (an unloaded chunk reverts when it loads again, the age cap drops what never does), `icbm.blast.infernal_exothermic` and `_endothermic` (a 7,200-health mob dies at ground zero of a full T3 warhead), and the per-type `icbm.blast.<type>` scenarios for the three new types. The Nuke Launcher keeps `projectile.nuke.*`.

## Open questions

None. Players in the kill zone follow the server's damage settings through `DamageSources.annihilateLiving`; the scenarios use mobs.
