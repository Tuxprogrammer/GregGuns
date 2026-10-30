# Unreleased

Notes for the next release. Fold these into that release's changelog when it is cut.

## Module flag mismatch warning

- When a client joins a server whose `modules.*` flags differ from the client's own `config/gregguns.cfg`, the player now gets one chat line naming each module that differs, and the log gets one warning. Recipes and NEI follow the client's own config, so set the same values there and restart. Singleplayer is unaffected.

## Fixed

- Picking up a GregGuns item that GT builds on the Assembly Line (a Tier 3 Missile Body, a gun, armor) no longer awards an unnamed `achievement.tile.gregguns...` achievement. GregGuns items are taken out of GregTech's auto-generated achievements at startup; GT's recipe unlock on pickup still works.
- Abandoned houses and other structures no longer place Chisel glass panes (and gold blocks) at a variant Chisel does not define, which showed the ERROR texture. They use Chisel's borderless pane and large-ingot gold now, and any Chisel block at an undefined variant is replaced by its vanilla stand-in.

- Aiming down the sights now lines up on the screen center for the Golden Gun, the 9mm Pistol and the Mac10 (they sat well left of it at every field of view), and the LMG now aims through its red dot instead of through its muzzle sight, which put the view on the buttstock.
- The Minigun's barrels now whine as they wind up before the first shot and wind down after the last, with real CC0 recordings that follow the shooter and stop cleanly when you let go, switch away, reload or die.
- GregGuns NPCs from structure spawners (silo and bunker guards, room guards) can now spawn as Elite, Ultra or Infernal mobs, like every other GregGuns NPC. Spawner NPCs were skipped by default; the new `npcs.infernalSpawnerOptOut` (default off) restores that as a farm guard, and replaces `npcs.infernalAllowSpawnerNpcs`.
- Missile Bodies T1 and T2 are now built in the GT Assembler at HV and EV, with no research, instead of on the Assembly Line, which GTNH players only reach at LuV. Same inputs and times: plates, fins, engine, 2 circuits and 250 L of lubricant. The Tier 3 body stays on the LuV Assembly Line.
- Missiles now sound like Galacticraft rockets: the engine loop starts with the countdown, rises in pitch toward ignition, follows the missile through the climb and arc, and stops on impact, interception or abort.

## New: GregGuns Coins and the Vending Machine

- **GregGuns Coin** (`gregguns:coin`), GregGuns' own currency, in five values like the pack's coins: 1, 10, 100, 1000 and 10K (damage 0 to 4). One coin breaks into ten of the next lower value in a crafting grid; ten coins and programmed circuit 10 make one of the next higher value in any GT Assembler.
- **GregGuns trades in the pack's Vending Machine.** 178 trades with every module on: guns, akimbo pairs, melee tools, loaded magazines, rounds, grenades, gun parts and components, armor, special-slot gear, flight devices, armor parts, turret plates and a spawn egg for every NPC, the Attack Helicopter, Cyberdemon, Alien Bug and Dictator Dave included. All are priced in GregGuns Coins by tier. With the GregGuns quest line installed, each tier's trades open with that tier's intro quest. The trades are added in memory when the server starts; `config/vendingmachine/tradeDatabase.json` is not touched. New key `compat.vendingTrades` (default on).

### Quest loot bags

- GregGuns quests reward GregGuns loot bags: eight tier bags from Recruit (Steam) to General (ZPM), Spoils of War for boss kills and three ICBM Ordnance crates. They are EnhancedLootBags bags (ids 1701 to 1712), so they look, sort and take Fortune like the pack's own, and they hold GregGuns ammunition, magazines, parts, guns, armor and the materials GregGuns recipes use.
- GregGuns adds these groups to EnhancedLootBags at startup on both sides; the pack's `LootBags.xml` is not touched. `compat.lootBags` turns this off, and `config/gregguns-lootbags.xml` replaces the contents.
- A bag whose group is missing is kept with a chat line instead of crashing, and `/lootbags reload` keeps the GregGuns groups.
