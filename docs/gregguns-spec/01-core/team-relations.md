# Player relations: teams instead of FTB Utilities friends

- **Techguns baseline:** [integrations.md § FTB Utilities](../../techguns-spec/01-core/integrations.md#ftb-utilities-ftbu-friends), [`TGNpcFactions.friendsAPI`](../../techguns-spec/10-entities-npcs/ai.md#1-factions-tgnpcfaction-tgnpcfactions)
- **Design records:** none decide this. See Open questions.
- **Pack mods:**
  - GTNHLib (`gtnhlib`, 0.11.52): the team system GT itself uses
  - ServerUtilities (`serverutilities`, 2.4.14): the pack's team, claim and rank mod
  - GT5-Unofficial (`gregtech`, 5.09.54.205): precedent only
- **Roadmap:** the first milestone that ships turrets or owner-based security

## What changes from Techguns

Techguns asks one question: are player A and player B friends? It answers through FTB Utilities' `FriendsAPI`, and without FTBU it answers "yes" for everyone, so turrets never target players.

FTB Utilities is **not in the pack**. Neither is any `FriendsAPI`. The pack has two team systems, and they do not talk to each other:

| System | Owner | Concept | Always present |
|---|---|---|---|
| GTNHLib teams | GTNHLib | A team with owner, officer and member roles. Every player has one (a solo team is created on demand) | Yes (hard dependency) |
| ServerUtilities teams | ServerUtilities | A team with owner, mod, member, invited, **ally** and **enemy** statuses, plus chunk claims | No (soft; `disable_teams` can switch teams off) |

| Techguns | GregGuns | Why |
|---|---|---|
| `IFTBUtilitiesIntegration.areFriends(a, b)`, dummy returns `true` | `PlayerRelations.areFriendly(UUID owner, UUID other)` in `com.gregguns.api`, with a pure-logic core | One question, answered from the pack's real data |
| Without the optional mod, everyone is a friend | Default mode `TEAMS`: friendly means same GTNHLib team, or an ServerUtilities ally or member when that mod is loaded. A config switch `playerRelationMode = EVERYONE_FRIENDLY` restores the Techguns default | GTNHLib is always loaded, so "no teams mod" is no longer a case. Pack makers on a trusted server can switch to the old behavior |

## Integration points

### GTNHLib: team membership
- **Pack config:** none.
- **Source:**
  - [`TeamManager.getTeamByPlayer(UUID)`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/teams/TeamManager.java#L58): may return `null` and logs an error when a player has no team.
  - [`TeamManager.getOrCreateTeam(String, UUID)`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/teams/TeamManager.java#L95): the null-safe form. GTNHLib's own javadoc recommends it.
  - [`Team.isMember(UUID)`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/teams/Team.java#L57).
  - Precedent: GT's [`GTPowerfailTracker`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/data/GTPowerfailTracker.java#L178) resolves a machine owner's team with `TeamManager.getOrCreateTeam(ownerName, ownerUuid)`.
- **GregGuns does:** stores the owner's UUID and name on turrets and security blocks. To test friendliness it looks up the owner's team with `getOrCreateTeam` and tests `isMember(other)`. Server side only.
- **Pack-side change:** none.

### ServerUtilities: ally status (soft dependency)
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/serverutilities/serverutilities.cfg`, category `teams`: `disable_teams` = `false`, `autocreate_mp` = `false`, `autocreate_sp` = `true`. With the shipped values, a multiplayer player has no ServerUtilities team until they make one.
- **Source:**
  - [`Universe.getNullable()`](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/Universe.java#L80) and [`Universe.getPlayer(UUID)`](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/Universe.java#L561).
  - [`ForgePlayer.team`](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/ForgePlayer.java#L58) and `hasTeam()` (L106).
  - [`ForgeTeam.isAlly(ForgePlayer)`](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/ForgeTeam.java#L537): true for members and for players the team set to ALLY or higher. [`isEnemy`](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/ForgeTeam.java#L574) is separate.
  - [`getHighestStatus`](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.14/src/main/java/serverutils/lib/data/ForgeTeam.java#L323) lists the statuses in priority order: owner, mod, member, enemy, ally, invited, none.
- **GregGuns does:** guarded by `Loader.isModLoaded("serverutilities")`, in a separate class that is only loaded when the mod is present. It uses `Universe.getNullable()`, finds the owner's `ForgePlayer`, and returns `team.isAlly(other)`. It treats `isEnemy(other)` as hostile even if the GTNHLib team says friendly. If the universe is `null` (world not loaded) it returns "not friendly".
- **Pack-side change:** none. Server owners should expect two separate team lists. A code search of ServerUtilities for `gtnhlib.teams` found no use, so assume the two are not mirrored until someone reads the full tree.

### OpenModularTurrets (overlap, for context)
- **Pack config:** `config/openmodularturrets.cfg` exists. GregGuns does not read it, and this sheet makes no claim about how OpenModularTurrets decides targets. The overlap with our Turret belongs to [09-machines-utility](../../techguns-spec/09-machines-utility/index.md).

## Requirements

- **GGS-01-18** Player-versus-player friendliness is decided in one place, `PlayerRelations.areFriendly(owner, other)`, on the server only.
- **GGS-01-19** In `TEAMS` mode, a player is friendly to an owner when both are in the same GTNHLib team.
- **GGS-01-20** When `serverutilities` is loaded and the owner has a ServerUtilities team, an ally or member of that team is friendly, and an enemy of that team is not.
- **GGS-01-21** In `EVERYONE_FRIENDLY` mode every player is friendly. This is the Techguns behavior.
- **GGS-01-22** A missing team, a missing ServerUtilities universe or an unknown UUID never throws. The answer is "not friendly" in `TEAMS` mode.
- **GGS-01-23** No class that references `serverutils.*` is loaded when `serverutilities` is absent.

## Tests

- GGS-01-18 to GGS-01-22: unit tests on a pure-logic decision method that takes the three inputs as plain booleans (`sameGtnhlibTeam`, `suAlly`, `suEnemy`) and the mode.
- GGS-01-23: the `runServer` boot smoke test with `serverutilities` removed from the run classpath.
- Manual: two-account session, place a turret, join and leave a GTNHLib team, check the turret's targeting.

## Open questions

- Is `TEAMS` the right default? It makes turrets shoot strangers on a public server and in singleplayer-with-friends over LAN. It also differs from Techguns. A short design record should confirm the default and name who gets a bypass (ops?).
- Should GregGuns ever write to a team (for example, create a solo GTNHLib team for an unregistered owner)? `getOrCreateTeam` does this as a side effect. If that is unwanted, use `getTeamByPlayer` and treat `null` as "no team".
