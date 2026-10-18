# Turret: ownership, targeting and coexistence

- **Techguns baseline:** [turret.md](../../techguns-spec/09-machines-utility/turret.md), [targeting rules](../../techguns-spec/09-machines-utility/turret.md#targeting-rules), [10 ai.md, TurretTargetSelector](../../techguns-spec/10-entities-npcs/ai.md#turrettargetselector-the-target-filter)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (Turret kept), [0003 energy model](../../design/0003-energy-model.md)
- **Pack mods:** OpenModularTurrets (`openmodularturrets`, 2.4.8), ServerUtilities (`serverutilities`, 2.4.14), Infernal Mobs (`InfernalMobs`, 1.10.7-GTNH), GT5-Unofficial (`gregtech`, 5.09.54.205)
- **Roadmap:** the turret milestone in [ROADMAP](../../ROADMAP.md). Machine registration is owned by [08](../08-machines-production/).

## What changes from Techguns

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| 1 000 RF buffer, 5 RF/t upkeep | GT MetaTileEntity on EU at its tier voltage. The numbers are set in the energy model | [0003](../../design/0003-energy-model.md); MTE registration is in [08](../08-machines-production/) |
| "Friends" come from FTB Utilities, a mod the pack does not contain. The dummy fallback says everyone is a friend ([NPC-13](../../techguns-spec/BUGS.md)) | Friends come from ServerUtilities teams. Without it, only the owner is a friend | ServerUtilities replaced FTBUtilities in GTNH |
| Mounted `NPCTurret` is an `EntityCreature` and `IAnimals` | Same, and it is made `IEntityOwnable` too. It must never become `IMob` | Keeps Infernal Mobs and mob-targeting mods away from it |
| Nothing says how it relates to other turret mods | Documented below. OpenModularTurrets stays; ours is the "real gun" turret | OMT is in the pack and is EU-powered too |

## How ours differs from OpenModularTurrets

Both are EU-powered, owner-aware defense blocks. They must not read as duplicates in NEI or in play.

| | OpenModularTurrets 2.4.8 | GregGuns Turret |
|---|---|---|
| Weapon | A fixed head block per type (machine gun, rocket, laser and so on). Damage, range and RF per shot come from config | A GregGuns gun from the player's own loadout, fed with real ammo items. Stats are the gun's |
| Body | Tile entity only, with an optional camouflage block | Tile entity plus a 30 HP `NPCTurret` entity that heals, can die and respawns |
| Power | RF storage; GT EU is converted at `EU to RF Ratio` = 8.0 | EU natively, with GT overvoltage rules |
| Ownership | UUID plus a trusted-player list | Owner UUID plus ServerUtilities team status |

- **Source (OMT power):** [TurretBase.java#L496-L503](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/tileentity/turretbase/TurretBase.java#L496-L503) takes `aVoltage * aAmperage` with no voltage check. [L564-L572](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/tileentity/turretbase/TurretBase.java#L564-L572) converts EU to RF.
- **Source (OMT ownership and camo):** [TurretBase.java#L55-L59](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/tileentity/turretbase/TurretBase.java#L55-L59), [L72-L74](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/tileentity/turretbase/TurretBase.java#L72-L74).
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/openmodularturrets.cfg`. `globaltargetingparameters` has mobs, neutrals and players all `true`. `miscellaneous` has "Should turret bases be camouflage-able with normal blocks?" = `true`. `modcompatability` has "Can turrets be powered with EU?" = `true`.

## Integration points

### OpenModularTurrets: target filter coexistence
- **Source:** [TurretHeadUtil.java#L108-L145](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/util/TurretHeadUtil.java#L108-L145). A candidate is a target if it is an `EntityAnimal` or `EntityAmbientCreature` (neutrals), `isCreatureType(monster)` (mobs), or an `EntityPlayerMP` that is neither the owner, trusted, nor in creative. Trust is checked at [L340-L348](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/util/TurretHeadUtil.java#L340-L348).
- **GregGuns does:** nothing in code. `NPCTurret` is none of those types, so an OMT turret never shoots it. GregGuns hostile NPCs extend `EntityMob`, so OMT turrets do shoot them. That is intended. A test asserts the class relationships so a refactor cannot change this.
- **Pack-side change:** none.

### ServerUtilities: who counts as a friend
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/serverutilities/serverutilities.cfg`, section `teams`, `disable_teams=false`. The pack has no FTB Utilities or FTBLib (`MOD-INDEX.md` lists none), so the Techguns `FTBU` check never succeeds.
- **Source:**
  - [Universe.get()](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/Universe.java#L72-L76) throws `NullPointerException` when the universe is not loaded.
  - [Universe.getPlayer(UUID)](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/Universe.java#L560-L568) returns null for an unknown UUID.
  - [ForgePlayer.team](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/ForgePlayer.java#L58) is the player's team.
  - [ForgeTeam.getHighestStatus](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/ForgeTeam.java#L323-L345) returns an `EnumTeamStatus`. The order is ENEMY, NONE, INVITED, ALLY, MEMBER, MOD, OWNER ([EnumTeamStatus](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/EnumTeamStatus.java#L18-L24)).
- **GregGuns does:** behind `Loader.isModLoaded("serverutilities")`, in a class that is only loaded when the mod is present. Player A is a friend of owner B when A is B, or `B.team.getHighestStatus(A)` is ALLY or higher. The lookup is wrapped so a missing universe or unknown UUID means "not a friend". With the mod absent, only the owner is a friend. This fixes NPC-13 in the safe direction.
- **Pack-side change:** none.

### Infernal Mobs: keep the turret out
- **Source:** [InfernalMobsCore.java#L411-L418](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L411-L418). An entity qualifies when it is `IMob` and not `IEntityOwnable`.
- **GregGuns does:** `NPCTurret` stays non-`IMob`, and also implements `IEntityOwnable` with the base owner's UUID. Either alone keeps it out of Infernal. Both together also keep it out of other mods that skip owned entities. Infernal for GregGuns hostiles is owned by area 10.
- **Pack-side change:** none.

## Requirements

- **GGS-09-01** `NPCTurret` is not an `IMob`, `EntityAnimal` or `EntityAmbientCreature`.
- **GGS-09-02** `NPCTurret` implements `IEntityOwnable`, and the owner UUID is the base's owner.
- **GGS-09-03** With ServerUtilities loaded, a player is a friend of the owner when they are the same player, or their team status is ALLY or higher.
- **GGS-09-04** Without ServerUtilities, only the owner is a friend. Security level 1 then behaves like level 2.
- **GGS-09-05** Every ServerUtilities call is guarded against a missing universe and an unknown UUID, and never throws into the tick.
- **GGS-09-06** The ServerUtilities classes are not loaded when the mod is absent.
- **GGS-09-07** The turret's EU intake follows GT overvoltage rules. There is no free over-voltage like OMT's `injectEnergyUnits`.

## Tests

| Req | Check |
|---|---|
| 01, 02 | Unit test on the class hierarchy |
| 03, 04 | Unit test with a stub friend provider; `runServer` check with and without ServerUtilities |
| 05, 06 | `runServer` boot without ServerUtilities; manual test in a fresh world before the first login |
| 07 | GT test scenario owned by area 08 |

## Open questions

- Should "friend" mean team ALLY or higher (this sheet), or MEMBER only? A design record on turret security would settle it.
- Does the turret MTE need one tier or several? Settled in area 08 and [0003](../../design/0003-energy-model.md).
