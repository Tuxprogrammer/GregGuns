# 10 Entities and NPCs: GregGuns spec

## Overview

This area records how GregGuns hostile NPCs plug into the GTNH 2.9.0-RC-2 pack. The Techguns baseline is [techguns-spec/10-entities-npcs](../../techguns-spec/10-entities-npcs/index.md). Evidence rules are in [INTEGRATION.md](../INTEGRATION.md). [0005](../../design/0005-world-presence.md) governs world presence: Overworld only, config-gated, halved weights.

The main findings:

- **Infernal Mobs works with our NPCs almost for free.** Every hostile NPC is an `IMob` and not an `IEntityOwnable`, so it is eligible. The turret is not. Three things need care: the damage pipeline must post `LivingHurtEvent` and apply its final amount; ranged damage sources must be `EntityDamageSourceIndirect` or the contact modifiers (Fiery, Poisonous, Wither) hit the shooter at range; and the config keys are **class simple names** in this pack.
- **Tiered loot for infernal NPCs** is a small soft-dependency `LivingDropsEvent` handler. It must run before Infernal Mobs' own handler, which removes the entity from its map. The loot tables are owned by [06-items](../06-items/pack-loot.md).
- **Mobs Info finds drops by simulation.** It swaps the entity's RNG and walks every branch, so drop code must use the entity's own `rand`, and equipment must come from the vanilla no-argument `addRandomArmor()` or NEI shows none.
- **The Techguns spawn placeholder is a liability in this pack.** It is a real `IMob` that Infernal Mobs, Mobs Info and EnderZoo's global modifier all see. Registering the real classes fixes that and also lets EnderZoo's spawn config tune GregGuns spawns.
- **Smaller hooks.** AngerMod's Kamikaze explodes melee-killed mobs (10%) unless the held item is whitelisted. EnderZoo scales every `IMob`'s health and melee attack by difficulty, which is a third scaler next to ours. ThaumcraftMobAspects needs a `gregguns.cfg`.

## Contents

| File | Covers |
|---|---|
| [infernal-mobs.md](infernal-mobs.md) | Eligibility, config keys, damage-pipeline rules, modifier interplay, tiered loot, spawner-block opt-out |
| [mob-drops-nei.md](mob-drops-nei.md) | How Mobs Info discovers drops, dreamcraft Custom Drops as a pack hook, NPC equipment against the tier map |
| [spawning.md](spawning.md) | The placeholder against the pack, SpecialMobs, EnderZoo spawn config, AngerMod, dimensions |
| [registry-and-pack-hooks.md](registry-and-pack-hooks.md) | Entity names as each pack mod sees them, ThaumcraftMobAspects values |
| [m7-exit-gate.md](m7-exit-gate.md) | The M7 exit-gate audit: evidence per box, the all-eggs check, deviations, polish and open decisions |

**No integration point, so no sheet.** See the Techguns spec and the [design records](../../design/README.md):

- AI task list, the burst ranged AI, hurt-by-target rules and factions: [ai.md](../../techguns-spec/10-entities-npcs/ai.md). The only pack touch is target selection (GGS-10-08, GGS-10-28), which is in the sheets above.
- **Deviation, friendly fire:** a bullet or hitscan shot passes through an NPC of its shooter's own faction (`FactionRules.sparesFriend`, [D218](../../gregguns-icbm-spec/DECISIONS.md)), so a crowd of guards firing at a player doesn't wound itself. Techguns bullets hit any entity on their path and only skipped the revenge. Explosions and area effects still hit everyone.
- Models, renderers, animations, textures and sounds: [rendering-and-models.md](../../techguns-spec/10-entities-npcs/rendering-and-models.md), [textures.md](../../techguns-spec/10-entities-npcs/textures.md), [sounds.md](../../techguns-spec/10-entities-npcs/sounds.md), with [0006](../../design/0006-art-direction.md). Infernal Mobs' glow is a particle effect and needs no renderer cooperation.
- Attack Helicopter flight and the Alien Bug's legacy AI: [ai.md](../../techguns-spec/10-entities-npcs/ai.md#6-attack-helicopter). The helicopter is excluded from Infernal Mobs.
- Turret entity: its overlap with OpenModularTurrets is a block and machine matter, in [09-machines-utility](../09-machines-utility/index.md). The entity-side facts (not `IMob`, no drops) are in [infernal-mobs.md](infernal-mobs.md).
- Spawn eggs and the two spawner blocks: [spawning.md](../../techguns-spec/10-entities-npcs/spawning.md#2-spawn-eggs-tgmonsterplacer) and [07-blocks](../../techguns-spec/07-blocks/index.md). The infernal opt-out for spawner NPCs is in [infernal-mobs.md](infernal-mobs.md).
- Camp flags and structures that place NPCs: [11-worldgen](../11-worldgen/index.md).
- Hardcore Ender Expansion: End-only, no hook into Overworld NPC spawns.

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| Hostile NPC classes (`EntityMob`, `IMob`) | Eligible for elite, ultra and infernal rolls. Turret and any non-hostile NPC are not `IMob`. | Infernal Mobs 1.10.7-GTNH | [infernal-mobs.md](infernal-mobs.md) |
| `DamageSystem` re-implementation of damage | Posts `LivingHurtEvent` once and applies the final amount. Ranged sources are `EntityDamageSourceIndirect`. | Infernal Mobs | [infernal-mobs.md](infernal-mobs.md) |
| Own loot tables and the held-item drop | Kept. Infernal NPCs also drop tiered GregGuns items. | Infernal Mobs, [06-items](../06-items/pack-loot.md) | [infernal-mobs.md](infernal-mobs.md) |
| Mob drop code and equipment roll | Simulation-friendly: entity `rand` only, vanilla no-argument `addRandomArmor()`. | Mobs Info 0.6.0 | [mob-drops-nei.md](mob-drops-nei.md) |
| (none) | Per-class and once-per-player drops through pack XML. | dreamcraft Custom Drops | [mob-drops-nei.md](mob-drops-nei.md) |
| Fixed drops and held guns | Each NPC has a tier ceiling checked against the tier map. | [0001](../../design/0001-tier-map.md), [0005](../../design/0005-world-presence.md) | [mob-drops-nei.md](mob-drops-nei.md) |
| `TGDummySpawn` placeholder and `TGSpawnManager` | Recommended: register real classes and gate in `CheckSpawn`. | Infernal Mobs, Mobs Info, EnderZoo | [spawning.md](spawning.md) |
| Spawn weights and dimension lists | Halved weights. Dimensions default to `[0]`, checked at spawn time. | [0005](../../design/0005-world-presence.md), EnderZoo `SpawnConfig_User.xml` | [spawning.md](spawning.md) |
| NPC difficulty scaling on gun damage | Applied once, and not on health or melee attack. | EnderZoo global difficulty modifier | [spawning.md](spawning.md) |
| Melee kills with TG tools | Pack whitelists `gregguns.` item names against Kamikaze. | AngerMod 1.0.7 | [spawning.md](spawning.md) |
| NPC targeting of players | Ignores `disableDamage` players. | AngerMod spawn protection | [spawning.md](spawning.md) |
| `Techguns.<Name>` entity names | `gregguns.<Name>`, one constant per name. | Five pack consumers, one key each | [registry-and-pack-hooks.md](registry-and-pack-hooks.md) |
| (none) | Aspects for NPC scans. | ThaumcraftMobAspects 1.2.1-GTNH | [registry-and-pack-hooks.md](registry-and-pack-hooks.md) |

## Pack-side changes

| File | Change | Why | Sheet |
|---|---|---|---|
| `config/InfernalMobs.cfg` | `permittedentities`: add `AttackHelicopter=false`, and explicit `true` for `ZombieSoldier`, `ZombieFarmer`, `ZombieMiner`, `ZombiePigmanSoldier`, `SkeletonSoldier`, `ArmySoldier`, `Bandit`, `Outcast`, `Commando`, `StormTrooper`, `CyberDemon`, `PsychoSteve`, `DictatorDave`, `SuperMutantBasic`, `SuperMutantHeavy`, `SuperMutantElite`, `AlienBug` | Keys are class simple names. Without entries Infernal Mobs writes its own. The helicopter has its own boss bar. | [infernal-mobs.md](infernal-mobs.md) |
| `config/InfernalMobs.cfg` | `entitybasehealth`: add the same classes with Techguns HP (25, 18, 20, 25, 25, 30, 20, 30, 30, 35, 30, 75, 100, 30, 40, 50, 20, in the order above) | The default is the first spawned instance's max health, then frozen in the file | [infernal-mobs.md](infernal-mobs.md) |
| `config/InfernalMobs.cfg` | `mm_berserk.berserkMaxDamage`: set a positive value, for example `20.0` | The shipped `0.0` makes a Berserk mob deal 0 damage (pack-wide upstream bug) | [infernal-mobs.md](infernal-mobs.md) |
| `config/InfernalMobs.cfg` | Alternative (route A in pack-loot.md): append GregGuns items to `droppedItemIDsElite`, `droppedItemIDsUltra` and `droppedItemIDsInfernal` | The only way to show them on NEI's Infernal Drops page. Every infernal mob would drop them. | [infernal-mobs.md](infernal-mobs.md) |
| `config/mobsinfo/mobsinfo.cfg` | `mobhandler.MobBlacklist`: add `gregguns.SimpleTurret`, and `gregguns.TGDummySpawn` if the placeholder is kept | `IncludeEmptyMobs=true` would list them as empty mobs | [mob-drops-nei.md](mob-drops-nei.md) |
| `config/angermod.cfg` | `kamikaze.butcherItems`: add `gregguns.` | A 10% explosion on melee kills with GregGuns tools, otherwise. The match is a substring on the item's unlocalized name. | [spawning.md](spawning.md) |
| `config/ThaumcraftMobAspects/gregguns.cfg` | New file with the entries in the aspects table | Without it, no GregGuns mob is scannable | [registry-and-pack-hooks.md](registry-and-pack-hooks.md) |
| `config/GTNewHorizons/CustomDrops.xml` | Optional: `CustomDrop` entries for GregGuns classes, for example a once-per-player mini-boss reward | Keeps pack-specific rewards out of GregGuns code | [mob-drops-nei.md](mob-drops-nei.md) |
| `config/enderzoo/SpawnConfig_User.xml` | Optional: `entry` rows with `mobName="gregguns.<Name>"` to retune spawns by biome, dimension or rate | EnderZoo resolves any registered mob by name | [spawning.md](spawning.md) |

## Related areas

- [06-items](../06-items/pack-loot.md): owns every loot table, including the tier tables the infernal handler reads. This area owns only the trigger.
- [02-guns](../../techguns-spec/02-guns/index.md) and [0004](../../design/0004-damage-model.md): the damage pipeline rules here (GGS-10-05, GGS-10-06) refine the implementation of 0004.
- [07-blocks](../../techguns-spec/07-blocks/index.md): the spawner blocks and the infernal opt-out.
- [09-machines-utility](../09-machines-utility/index.md): the turret.
- [11-worldgen](../11-worldgen/index.md): camp flags and structure NPCs. They reach Infernal Mobs the same way, because every join is rolled.
- [05-armor](../05-armor/index.md): the Infernal Choke modifier reduces a player's air, and oxygen gear in [oxygen-gear.md](../05-armor/oxygen-gear.md) may refill it.

## Open questions

1. **Spawn architecture.** Keep Techguns' placeholder, or register real classes with a `CheckSpawn` gate. Recommended: real classes. Needs an amendment to [0005](../../design/0005-world-presence.md) ([spawning.md](spawning.md)).
2. **Nether-only NPCs** (Pigman Soldier, Cyberdemon). Under 0005 they never spawn naturally. Needs a line in 0005 or a Nether record ([spawning.md](spawning.md)).
3. **Tier ceilings for NPC drops.** Psycho Steve's fixed Chainsaw is HV, above the LV-MV surface default, and the infernal bands need a ceiling. Needs a record shared with [06-items](../06-items/pack-loot.md) ([mob-drops-nei.md](mob-drops-nei.md), [infernal-mobs.md](infernal-mobs.md)).
4. **Ranged damage-source classes.** The `EntityDamageSourceIndirect` rule refines [0004](../../design/0004-damage-model.md). A line in 0004 would stop M1 building the wrong base class ([infernal-mobs.md](infernal-mobs.md)).
5. **Is Infernal Mobs' health hack effective?** A test settles it, not a record ([infernal-mobs.md](infernal-mobs.md)).
6. **Alien Bug and Cyberdemon as infernal.** Confirm they stay eligible ([infernal-mobs.md](infernal-mobs.md)).
