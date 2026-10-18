# Entity names, aspects and other pack hooks

- **Techguns baseline:** [mob-catalogue.md § registration](../../techguns-spec/10-entities-npcs/mob-catalogue.md#registration-javatechgunstgentitiesjava), [lang.md](../../techguns-spec/10-entities-npcs/lang.md), [spawning.md § camp flags](../../techguns-spec/10-entities-npcs/spawning.md#4-camp-flags-and-npcutil)
- **Design records:** [0005 World presence](../../design/0005-world-presence.md)
- **Pack mods:**
  - ThaumcraftMobAspects (`ThaumcraftMobAspects`, 1.2.1-GTNH)
  - Mobs Info (`mobsinfo`, 0.6.0), Infernal Mobs (`InfernalMobs`, 1.10.7-GTNH), EnderZoo (`EnderZoo`, 1.3.6) and NewHorizonsCoreMod (`dreamcraft`, 2.9.84), as consumers of the names below
- **Roadmap:** M7 (registration), M9 (pack config)

Five pack mods identify a GregGuns mob in five different ways. This sheet lists them so that a rename or a registration mistake is caught in one place, and covers ThaumcraftMobAspects, which has no other home.

## What changes from Techguns

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| Entities are registered under mod id `Techguns`, so the `EntityList` name is `Techguns.<Name>`. | Mod id `gregguns`, so the name is `gregguns.<Name>`. Registry names keep Techguns' spelling. | The mod id is `gregguns` in `gradle.properties`. |
| Camp lists use short names and the NPC counter compares them with the full registered name, so the count never matches ([NPC-17](../../techguns-spec/10-entities-npcs/index.md#suspected-bugs)). | One constant per mob feeds registration, eggs, spawn tables and camp lists. | The same mistake would break pack-side configs that key off the name. |
| No aspects. | The pack ships aspect values for the NPCs. | Thaumcraft's entity scan. |

## Which name each pack mod uses

| Consumer | Key | Value for the Bandit | Source |
|---|---|---|---|
| Infernal Mobs (`permittedentities`, `entitiesalwaysinfernal`, `entitybasehealth`) | Class simple name, because the pack sets `useSimpleEntityClassnames=true` | `Bandit` | [InfernalMobsCore#L438-L466](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L438-L466) |
| Infernal Mobs `/spawninfernal` | `EntityList` name | `gregguns.Bandit` | [InfernalCommandSpawnInfernal#L45-L62](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/commands/InfernalCommandSpawnInfernal.java#L45-L62) |
| Mobs Info `MobBlacklist`, `MobOverrides.json` | `EntityList` name | `gregguns.Bandit` | [MobRecipeLoader#L492](https://github.com/GTNewHorizons/Mobs-Info/blob/0.6.0-GTNH/src/main/java/com/kuba6000/mobsinfo/loader/MobRecipeLoader.java#L492) |
| EnderZoo `SpawnConfig_User.xml` | `EntityList` name | `gregguns.Bandit` | [MobSpawns#L40-L52](https://github.com/GTNewHorizons/EnderZoo/blob/1.3.6/src/main/java/crazypants/enderzoo/spawn/MobSpawns.java#L40-L52) |
| ThaumcraftMobAspects file | `EntityList` name. The file name is the part before the first dot. | `gregguns.Bandit` in `gregguns.cfg` | [ModFileCache#L67-L71](https://github.com/GTNewHorizons/ThaumcraftMobAspects/blob/1.2.1-GTNH/src/main/java/iguanaman/thaumcraftmobaspects/ModFileCache.java#L67-L71) |
| dreamcraft `CustomDrops.xml` | Fully qualified class name, exact match | `com.gregguns.entity.npc.Bandit` (placeholder package) | [CustomDrops#L27-L45](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/modcustomdrops/CustomDrops.java#L27-L45) |

## Integration points

### ThaumcraftMobAspects: one file per mod id

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/ThaumcraftMobAspects/`.
  - There is one `<modid>.cfg` per mod, as a JSON array of `{entityName, aspects}` objects, for example `openmodularturrets.cfg`, `gregtech.cfg` and `SpecialMobs.cfg`.
  - The pack leaves plain hostile mobs empty (28 of 29 vanilla entries are `{}`) and gives modded, themed mobs values (107 of 109 `SpecialMobs` entries have some).
  - No `gregguns.cfg` ships, and none for Techguns.
- **Source:** [ThaumcraftMobAspects#L53-L132](https://github.com/GTNewHorizons/ThaumcraftMobAspects/blob/1.2.1-GTNH/src/main/java/iguanaman/thaumcraftmobaspects/ThaumcraftMobAspects.java#L53-L132). At `postInit` it:
  - reads every file;
  - adds an empty entry for every registered `EntityList` name that has none;
  - writes all files back;
  - calls `ThaumcraftApi.registerEntityTag` only for entries with at least one aspect.

  So without a `gregguns.cfg`, the first run writes one full of empty entries, and nothing is scannable.
- **GregGuns does:** nothing at runtime. GregGuns never writes into another mod's config folder.
- **Pack-side change:** ship `config/ThaumcraftMobAspects/gregguns.cfg`. The format is the one the other files use:

```json
[
  {
    "_HEADER": "*****************************************",
    "entityName": "gregguns.Bandit",
    "_FOOTER": "*****************************************",
    "_COMMENT": "Aspects are in the form aspect : value ",
    "aspects": { "humanus": 3, "telum": 2, "tutamen": 1 }
  }
]
```

Starting values, modelled on the pack's `SpecialMobs.GatlingSkeleton` entry (`terra`, `machina`, `motus`, `exanimis`, `humanus`). The M9 pass can change them. Every aspect name below already appears in the shipped files.

| Entity | Aspects |
|---|---|
| `gregguns.ZombieSoldier`, `ZombieFarmer`, `ZombieMiner` | exanimis 3, humanus 1, telum 1 |
| `gregguns.ZombiePigmanSoldier` | exanimis 3, humanus 1, telum 1, ignis 2 |
| `gregguns.SkeletonSoldier` | exanimis 3, humanus 1, telum 1, terra 1 |
| `gregguns.ArmySoldier`, `Commando`, `Bandit`, `DictatorDave`, `Outcast` | humanus 3, telum 2, tutamen 1 |
| `gregguns.PsychoSteve` | humanus 3, telum 2, ignis 1, perditio 1 |
| `gregguns.Stormtrooper` | humanus 2, machina 2, telum 2, tutamen 1 |
| `gregguns.CyberDemon` | machina 2, ignis 2, telum 2, perditio 1 |
| `gregguns.SuperMutantBasic`, `SuperMutantHeavy`, `SuperMutantElite` | humanus 1, bestia 3, telum 1 |
| `gregguns.AlienBug` | bestia 3, alienis 2 |
| `gregguns.AttackHelicopter` | machina 3, aer 2, telum 2 |
| `gregguns.SimpleTurret` | machina 3, telum 2, tutamen 1 |

### dreamcraft Custom Drops, Mobs Info overrides and AngerMod

These use the names above and need no GregGuns code. They are described where they matter: Custom Drops and `MobOverrides.json` in [mob-drops-nei.md](mob-drops-nei.md), and AngerMod in [spawning.md](spawning.md). `MobOverrides.json` (`config/mobsinfo/`) holds `removeAll`, `additions` and `removals` per `EntityList` name, with stack, type, chance and damage weights per entry. The shipped file contains only the `ExampleMob` template.

## Requirements

| ID | Requirement |
|---|---|
| GGS-10-30 | Every NPC and the turret entity are registered with `registerModEntity` under mod id `gregguns`, so their `EntityList` names are `gregguns.<Name>`, with the Techguns registry spelling (for example `Stormtrooper`, not `StormTrooper`). |
| GGS-10-31 | Each registry name is defined once, as a constant used by registration, spawn eggs, spawn tables, camp lists and counters. A mismatch between the registered name and a lookup name fails a unit test ([NPC-17](../../techguns-spec/10-entities-npcs/index.md#suspected-bugs), [NPC-18](../../techguns-spec/10-entities-npcs/index.md#suspected-bugs)). |
| GGS-10-32 | The names in the aspects table and in the NPC tables of the other sheets all exist in the registry. A test compares the table in this document, kept as a resource list, with the registry. |

## Tests

| ID | Verification |
|---|---|
| GGS-10-30, 31 | Unit test over the registry: each name starts with `gregguns.`, each lookup by the constant finds the class, and the camp counter counts a freshly spawned NPC. |
| GGS-10-32 | Unit test as described. runServer check with the pack's `gregguns.cfg` in place: after boot the file is unchanged, apart from entries for mods added later. |
| Aspects in play | Manual check: scan a Bandit with a Thaumometer and read its aspects. |

## Open questions

1. **Aspect values** are placeholders. Whether the pack wants GregGuns mobs scannable at all is a pack-maintainer decision. The vanilla hostile mobs are left empty.
2. **Package name** for the Custom Drops hook. It stays a placeholder until M7 fixes the entity package.
