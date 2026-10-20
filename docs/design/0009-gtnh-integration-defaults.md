# 0009 · GTNH integration defaults

- Status: Accepted (by default during the MVP push; revisit after MVP)
- Gate: none (amends [0001](0001-tier-map.md), [0004](0004-damage-model.md) and [0005](0005-world-presence.md) where stated)
- Date: 2026-10-18

## Context
The [GregGuns spec](../gregguns-spec/README.md) areas raised open questions. Most come with a clear recommendation backed by pack evidence. This record takes those recommendations so that implementation isn't blocked. Questions that need the GTNH team, a running client or a balance pass stay open in their area indexes.

## Decision

| # | Question | Decision | Evidence |
|---|---|---|---|
| 1 | Jetpack vs GT Mech Armor | **Drop the GregGuns Jetpack.** GT Mech Armor already has a jetpack augment at HV. Keep the jump pack, glider and anti-grav device, which GT doesn't duplicate. This amends 0001's EV row. | [powered-flight.md](../gregguns-spec/05-armor/powered-flight.md) |
| 2 | Equipment slots | **Reuse Baubles Expanded's existing types:** night-vision goggles in `head`, back devices in `wings`, the ammo pouch in `quiver`. Request no new slot types. Protective masks stay helmet-slot items, because GT hazards and Galacticraft read only armor slots. | [baubles-slots.md](../gregguns-spec/05-armor/baubles-slots.md), [hazard-protection.md](../gregguns-spec/05-armor/hazard-protection.md) |
| 3 | Damage pipeline | **Run the damage model inside `LivingHurtEvent`**, applying the final amount, instead of canceling and re-implementing the hit. Projectile and other ranged sources are `EntityDamageSourceIndirect`, so Infernal Mobs' reflect modifiers and GT++ listeners see the real attacker. This amends 0004. | [damage-interop.md](../gregguns-spec/02-guns/damage-interop.md), [infernal-mobs.md](../gregguns-spec/10-entities-npcs/infernal-mobs.md) |
| 4 | Spawn architecture | **Register real NPC classes in the biome spawn lists, behind a `CheckSpawn` gate** (dimension, config and danger level), instead of Techguns' placeholder-entity swap. Infernal Mobs, mobsinfo and EnderZoo then see the real mob. This amends 0005's mechanism, not its scope. | [spawning.md](../gregguns-spec/10-entities-npcs/spawning.md) |
| 5 | Nether-only NPCs | The Pigman Soldier and the Cyberdemon have **no natural spawn**, because 0005 is Overworld only. They appear only from structures, spawners and eggs. | [spawning.md](../gregguns-spec/10-entities-npcs/spawning.md) |
| 6 | Team relations | **Turret and NPC friend checks use GTNHLib teams**, with ServerUtilities as a soft extra when present. A turret treats ALLY and above as friends. The default `playerRelationMode` is `TEAMS`. | [team-relations.md](../gregguns-spec/01-core/team-relations.md), [turret.md](../gregguns-spec/09-machines-utility/turret.md) |
| 7 | Gun zoom input | Keep right-click zoom. Guns go on Backhand's stop list and offhand blacklist ([PACK-CHANGES](../gregguns-spec/PACK-CHANGES.md)). | [offhand-backhand.md](../gregguns-spec/02-guns/offhand-backhand.md) |
| 8 | Loot tier ceiling | Settled by [0007](0007-pack-loot-and-infernal-mobs.md): nothing above HV, and HV only as a rare drop. A fixed boss drop above that (Psycho Steve's HV Chainsaw) is replaced with a tier-legal item. | [mob-drops-nei.md](../gregguns-spec/10-entities-npcs/mob-drops-nei.md) |

## Still open
These remain in the area indexes:
- the MTE ID block and texture page, which need GTNH team agreement;
- the material remap for obsidian steel and carbon;
- LuV and ZPM recipe voltage;
- explosive balance;
- whether the keybinds clash, which needs a running client.

## Consequences
- 0001's EV row loses the Jetpack. The dev self-check's tier table follows.
- M1 builds the damage model on `LivingHurtEvent`. M7 builds real spawn entries.

## Alternatives considered
The alternatives for each item are in the sheets linked in the table.
