# Unreleased

Notes for the next release. Fold these into that release's changelog when it is cut.

## Module flag mismatch warning

- When a client joins a server whose `modules.*` flags differ from the client's own `config/gregguns.cfg`, the player now gets one chat line naming each module that differs, and the log gets one warning. Recipes and NEI follow the client's own config, so set the same values there and restart. Singleplayer is unaffected.

## Fixed

- Missiles now sound like Galacticraft rockets: the engine loop starts with the countdown, rises in pitch toward ignition, follows the missile through the climb and arc, and stops on impact, interception or abort.

## New: GregGuns Coins and the Vending Machine

- **GregGuns Coin** (`gregguns:coin`), GregGuns' own currency, in five values like the pack's coins: 1, 10, 100, 1000 and 10K (damage 0 to 4). One coin breaks into ten of the next lower value in a crafting grid; ten coins and programmed circuit 10 make one of the next higher value in any GT Assembler.
- **GregGuns trades in the pack's Vending Machine.** 178 trades with every module on: guns, akimbo pairs, melee tools, loaded magazines, rounds, grenades, gun parts and components, armor, special-slot gear, flight devices, armor parts, turret plates and a spawn egg for every NPC, the Attack Helicopter, Cyberdemon, Alien Bug and Dictator Dave included. All are priced in GregGuns Coins by tier. With the GregGuns quest line installed, each tier's trades open with that tier's intro quest. The trades are added in memory when the server starts; `config/vendingmachine/tradeDatabase.json` is not touched. New key `compat.vendingTrades` (default on).
