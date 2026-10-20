# Gas clouds, potions and protective armor

The four gas blasts (debilitation, chemical, contagious, colors), the smoke marker, the poison helper classes, the protective-armor rating and the antidote. Radioactive blocks use the same armor rating and are in [radioactive-and-mutation.md](radioactive-and-mutation.md). The smoke entity and the other spawned entities are in [effect-entities.md](effect-entities.md). The blast engine that ticks these blasts is `02-explosives/blast-engine.md`.

Source root for all paths: `src/main/java/icbm/classic/` at `9c1d985a`. Config annotations, the `DamageSource` registry and the potion registry are *1.12-only mechanisms*.

## 1. Shared gas engine (`content/blast/gas/BlastGasBase.java`)

All four gas blasts extend `BlastGasBase`, a tickable blast that grows an invisible cloud of block cells and, every 5 ticks, applies an effect to the living entities standing in it.

| Item | Value |
|---|---|
| Registry IDs | debilitation 3, chemical 4, contagious 9 (all by the registry); colors and smoke have ID -1 (new in 1.12, no legacy ID) |
| Tiers | debilitation 1, chemical 1, colors 1, smoke 1, contagious 2 |
| Run interval | every 5 ticks (`TICKS_BETWEEN_RUNS`); the cloud and the entity effects update on the same pass |
| Cloud radius | `floor(ceil(R) * min(1, 2t / duration + 0.1))` where `R = max(3, size)` and `t` is the tick count: starts at 10% of full size, grows linearly, full at `0.45 * duration` (270 ticks at the default 600) |
| Cell test | a cell is inside when `floor(distance squared to the blast block) <= radius squared` (a sphere of whole-block cells) |
| End | the first 5-tick pass where the tick count exceeds `duration` |
| NBT | `duration` (int), `playShortSoundFX` (bool), plus the base `callCount`, `explosionSize`, `blastExpEntId`. The cloud cells and the hit counts are not saved, so a reload rebuilds the cloud from the start |
| Start sound | `debilitation` at tick 0, volume 4, pitch 0.7 +/- 0.14, unless `playShortSoundFX` is set. The flag is never set by any code (ICBM-02-79), so the `gasleak` sound (played every pass when the flag is on) is unused |

**Cloud growth: flood fill (MIT-portable).** Permalink: [`BlastGasBase.java#L173-L284`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blast/gas/BlastGasBase.java#L173-L284).

```
state: affected = set of cells, edge = queue of cells, lastRadius = 0
every pass:
    r = current radius (formula above)
    if r == lastRadius: return                       // nothing to do until the radius steps up
    lastRadius = r
    if affected is empty: add the blast cell to affected and edge
    if edge is empty:     put a random half of affected into edge   // revive a stalled front
    next = empty; failed = empty
    while edge is not empty:
        c = edge.pop()
        for each of the 6 neighbors n of c:
            if n is in failed or affected: continue
            if n is within the radius test:
                if passable(n, direction): add n to affected and next; spawn a gas particle at n
                else: add n to failed
    edge = next
passable(n, dir):
    box = block collision box at n
    if no box: true                               // air, plants, liquids, torches, open space
    blocked when the box spans the full block across both axes perpendicular to dir
        (UP/DOWN: full in X and Z; NORTH/SOUTH: full in X and Y; EAST/WEST: full in Z and Y)
    return not blocked
```

So gas flows through anything that is not a full-face solid in the direction of travel: air, water, lava, fences, open doors, trapdoors in some positions. Slabs, stairs and closed doors block it only on the faces where their box is full. A sealed room fills and stays inside; the cloud wraps around corners one cell ring per radius step. The set of cells is held in a Java `HashSet`: a radius-20 open-air cloud is about 33,500 cells.

**Per-cell particle.** Each new cell sends one gas particle (scale 7, lifetime = `duration` ticks, random drift of about 0.25 blocks/tick and a slight downward bias, color from the blast). The server cannot spawn client particles itself, so each cell becomes one `PacketSpawnAirParticle` (13 fields, about 70 bytes) sent to clients; on an integrated server the particle is created directly. A radius-20 cloud therefore sends tens of thousands of small packets over about 13 seconds (ICBM-02-63).

**Which entities are hit.** Every pass, all `EntityLivingBase` in the cube of half-width `R` (not the current radius) that pass `canGasEffect`:

1. Alive.
2. Not a creative-mode player.
3. The block cell of the entity's feet is in the affected set.
4. (Chemical and contagious only) not invulnerable to their damage source.

Armor stands, bosses and every mob qualify. Then the protection check (section 4) runs. An entity that fails it is counted in a map `entity -> hit count` (the map keeps strong references to the entity until the blast ends, and counts never decay, ICBM-02-64), and `applyEffect(entity, hitCount)` is called. Effects are re-applied every pass, so the timers are topped up as long as the entity stays inside.

## 2. The four gas blasts

| | Debilitation | Chemical | Contagious | Colors |
|---|---|---|---|---|
| Class | `BlastDebilitation` | `BlastChemical` | `BlastContagious` | `BlastColor` |
| Config category | `debilitation` | `chemical` | `contagious` | `colorful` |
| `size` (cloud radius, blocks) | 20 | 20 | 20 | 10 (key `scale`) |
| `duration` (ticks) | 600 | 600 | 600 | 600 (constant, not configurable) |
| `toxicity_start` (hits before damage) | none | 20 | 10 | none |
| `toxicity_scale` | none | 0.1 | 0.05 | none |
| `toxicity_min_damage` | none | 1.0 | 1.0 | none |
| Damages entities | no (effects only) | yes | yes | no, `canEffectEntities` is false |
| Particle color (r, g, b) | 1, 1, 1 (white) | 0.8, 0.8, 0 (yellow) | 0.3, 0.8, 0 (green) | cycle of 100 random colors |
| Min protection (config) | 0.1 | 0.5 | 0.8 | n/a |
| Death message key | n/a | `death.attack.icbm.chemical` | `death.attack.icbm.contagious` | n/a |

Config file `icbmclassic/blast`. Hits happen once per 5-tick pass, so 4 per second.

**Debilitation.** Each hit adds Poison I (18 s), Mining Fatigue I (60 s) and Slowness III (60 s), built with the 3-argument `CustomPotionEffect`, which keeps the default curative item (a milk bucket). No damage beyond vanilla poison.

**Chemical.** Each hit calls the "Chemical" poison helper with amplifier 0: Poison I 30 s, Nausea I 30 s, Hunger I 30 s, Weakness I 35 s, Mining Fatigue I 60 s (section 3). After more than `toxicity_start` (20) hits, i.e. 5 seconds of exposure, it also deals `max(toxicity_min_damage, hits * toxicity_scale)` damage with the `icbm.chemical` source, which is an ordinary damage type (armor counts; it is not marked magic or unblockable). Damage per hit is 2.1 at hit 21, 3 at hit 30, 6 at hit 60, 12 at hit 120 (the longest exposure the default duration allows). Vanilla invulnerability frames limit how much of a repeat hit applies.

**Contagious.** Each hit calls the "Contagious" poison helper with amplifier 3 (level IV): Poison IV 45 s, Blindness IV 15 s, Hunger IV 30 s, Weakness IV 35 s, Mining Fatigue IV 60 s. Then it tries the mutation effect ([radioactive-and-mutation.md](radioactive-and-mutation.md#3-mutation-contentblastblastmutationjava)): a pig becomes a zombie pigman and a villager a zombie villager, which skips the damage for that hit. For everything else, after more than 10 hits (2.5 s) it deals `max(1, hits * 0.05)` damage with the `icbm.contagious` source. The name is misleading: nothing spreads from entity to entity; "contagious" means high-level poison plus mutation.

**Colors.** A 600-tick cloud of size 10 with no effect on entities: a visual and sound test. At the first blast of a game session it builds a static list of 100 colors (random hue, saturation 0.9, brightness 0.5); each cell uses the next color of the cycle. The list is a static, shared by all blasts and never rebuilt.

**Smoke marker (explosive `smoke`).** ID -1, tier 1. The factory is a generic "spawn entity" action that creates the `smoke` entity ([EntitySmoke](effect-entities.md#3-smoke-entitysmoke)) at the blast point: no blast, no gas cloud, no config. It has grenade, block, missile and cart forms like any tier 1 explosive, and a tooltip "Short smoke signal for target testing".

## 3. Poison helpers and potions (`content/potion/`)

| Class | Role |
|---|---|
| `Poison` (deprecated) | Abstract registry of named poison helpers, `name <-> int id`. `poisonEntity(emitPos, entity, amplifier)` skips creative players, then calls the subclass. The armor check inside it is commented out and returns false, so protection is decided only by the gas blast (section 4) |
| `ContagiousPoison` (deprecated) | One class for both helpers. Constructor `(name, unusedId, isContagious)`. Instances: `ICBMClassic.chemicalPotion` ("Chemical", flag false) and `ICBMClassic.contagiousPotion` ("Contagious", flag true) |
| `CustomPotionEffect` | A vanilla `PotionEffect` with an optional explicit list of curative items. The 4-argument form with `null` sets an empty list (no item cures it). The 3-argument form keeps vanilla's default (milk) |
| `CustomPotion` | Abstract vanilla `Potion` wrapper that registers by integer id. Nothing extends it; it is dead code |

`ContagiousPoison.doPoisonEntity(emit, entity, amp)`:

| Effect | Contagious (flag true) | Chemical (flag false) |
|---|---|---|
| Poison (empty curative list, so milk does not cure it) | 45 s | 30 s |
| Blindness | 15 s | none |
| Nausea | none | 30 s |
| Hunger | 30 s | 30 s |
| Weakness | 35 s | 35 s |
| Mining Fatigue | 60 s | 60 s |

All at the given amplifier (0 from chemical, 3 from contagious). Effects of the same type combine under the vanilla rule: a higher amplifier replaces, an equal amplifier with a longer remaining time replaces; the repeated 5-tick refresh therefore keeps each at full duration while the entity is in the cloud, and they last their full time after it leaves. Vanilla poison cannot reduce health below one half-heart; the chemical and contagious damage above does.

Lang: `potion.toxin` = "Toxin", `potion.virus` = "Virus", `potion.frostBite` = "Frost Bite", `potion.radiation` = "Radiation". No code uses these keys (ICBM-02-79); the effects shown to players are vanilla potions. Death messages (`en_us.lang`): `death.attack.icbm.chemical` = "%s suddenly started feeling not so well.", `death.attack.icbm.contagious` = "%s breathed in too much of the green stuff.", `death.attack.icbmclassic:radioactive_block` = "%s died to radioactive fallout".

## 4. Protective armor (`content/gas/ProtectiveArmorHandler.java`, `config/ConfigMain.java`)

**Rating.** `getProtectionRating(entity)`:

1. If `require_helmet` is true and the head slot is empty: 0.
2. Else the sum, over the four armor slots, of the item's configured rating clamped to 0..1. An item without an entry counts 0. The sum is not clamped, so extra armor can exceed 1.

**Default ratings** (all `atomicscience:` items, which do not exist in a GTNH pack, so the defaults protect nothing there):

| Item | Rating |
|---|---|
| `hazmat_mask`, `hazmat_color_mask` | 0.75 |
| `hazmat_body`, `hazmat_color_body` | 0.15 |
| `hazmat_leggings`, `hazmat_color_leggings` | 0.05 |
| `hazmat_boots`, `hazmat_color_boots` | 0.05 |

A full set sums to exactly 1.0 in single-precision floats (checked), which is the number the radiation threshold needs.

**Config** (file `icbmclassic/main`, category `protective_armor`):

| Key | Default | Meaning |
|---|---|---|
| `min_chemical_gas_protection` | 0.5 | threshold for chemical |
| `min_viral_gas_protection` | 0.8 | threshold for contagious |
| `min_debilitation_gas_protection` | 0.1 | threshold for debilitation |
| `min_radiation_protection` | 1.0 | threshold for radioactive blocks |
| `require_helmet` | true | an empty head slot counts as rating 0 |
| `item_ratings` | empty list (the atomicscience defaults are added in code) | strings `domain:resource=float` or `domain:resource@meta=float` (the shared resource-list parser; NBT is not supported) |

**Rule.** An entity is affected when `rating < threshold` **or** `rating < random()` (a fresh roll per check). So: below the threshold, always affected; at or above it, affected with chance `1 - rating`; rating 1.0 or more, never. A mask alone (0.75) against chemical gas (threshold 0.5) is hit on 25% of passes; against contagious (0.8) it is always hit. With the radiation threshold at 1.0, anything under a full set is always hit. Creative players are excluded earlier. The rating applies to all living entities, not just players, so a zombie in armor listed in the config is protected.

**Reload.** The ratings list is rebuilt on config change (`ProtectiveArmorHandler.loadFromConfig`).

## 5. Antidote (`content/items/ItemAntidote.java`)

| | |
|---|---|
| Item | `icbmclassic:antidote`, name "Antidote", info "Cures all effects" (`item.icbmclassic:antidote.name`, `.info`). Recipe `recipes/antidote.json` (06-blocks-items) |
| Use | hold right-click for 32 ticks with the eating animation (`getMaxItemUseDuration` 32, action EAT) |
| Effect | at the end, on the server: `clearActivePotions()` removes **every** potion effect, good or bad, including the poison that milk cannot cure. Consumes one item unless the player is in creative mode |
| Interaction with gas | none directly. It does not give immunity: standing in the cloud re-applies everything within 5 ticks |
| Milk | a vanilla milk bucket cures the debilitation effects and the non-poison effects of chemical and contagious, but not their Poison (empty curative list) |

Item texture and model `antidote` are catalog entries for `07-visuals-audio`.

## 6. Audio and assets (this sheet)

| Sound event | File | Size | Used by |
|---|---|---|---|
| `debilitation` | `sounds/debilitation.ogg` | 222,470 bytes | every gas blast start |
| `gasleak` | `sounds/gasleak.ogg` | 52,097 bytes | gas pass sound when the unused flag is set |

Subtitles: `subtitle.icbmclassic:explosion.debilitation` = "Debilition Gas" (sic), `subtitle.icbmclassic:gas.leak` = "Gas Attack". No dedicated textures; clouds use the client air particle (`ParticleAirICBM`, `07-visuals-audio`).

## 7. Bugs

ICBM-02-63, 02-64, 02-79.
