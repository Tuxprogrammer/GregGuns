# Unreleased

Notes for the next release. Fold these into that release's changelog when it is cut.

## Module flag mismatch warning

- When a client joins a server whose `modules.*` flags differ from the client's own `config/gregguns.cfg`, the player now gets one chat line naming each module that differs, and the log gets one warning. Recipes and NEI follow the client's own config, so set the same values there and restart. Singleplayer is unaffected.

## Fixed

- Missiles now sound like Galacticraft rockets: the engine loop starts with the countdown, rises in pitch toward ignition, follows the missile through the climb and arc, and stops on impact, interception or abort.

## Changed: building an ICBM warhead

- A warhead is now built in steps. First the Assembler makes an **Inert Warhead** of its casing tier from the casing plates and the Fuze Assembly. The Assembler then fits a **payload** (shrapnel sleeve, napalm, gas canisters, field generators, a fissile core and so on). Last, **Plastic Explosive charges** go in, one charge per unit of fill. Load 1 to 8 at a time in a crafting grid (top-ups work), or fill the whole casing in the Assembler. Charges alone in a T1 casing make a Condensed warhead.
- An inert warhead cannot be placed, launched or set off, and a silo with one in its Input Bus says it is inert.
- No recipe takes both Industrial TNT and Plastic Explosive any more: the warhead payloads carry no explosive, and the Demolition, Cluster and EMP launcher warheads take more Plastic Explosive instead.
- The one-step full-warhead Assembler recipes and the fill-1 condensed starter recipe are gone. Existing warheads keep working.

## New: de-arming a warhead

- Put a live warhead and a **Defuse Kit** in a crafting grid to take out up to 8 Plastic Explosive per craft. The charges come back whole and the kit loses one use. The last charge out leaves the inert warhead with its payload, ready to be loaded again.
- The **Cutting Machine** runs the rest of the chain backwards at the warhead's own tier: it empties a full warhead at once, takes a payload out (its parts come back, its fluid does not) and cuts a bare inert warhead back into its plates and Fuze Assembly. Check NEI.
- Macerating or arc smelting a warhead now also returns its fuze's metal.
- New ICBM quest: De-arming.

## New: GregGuns Coins and the Vending Machine

- **GregGuns Coin** (`gregguns:coin`), GregGuns' own currency, in five values like the pack's coins: 1, 10, 100, 1000 and 10K (damage 0 to 4). One coin breaks into ten of the next lower value in a crafting grid; ten coins and programmed circuit 10 make one of the next higher value in any GT Assembler.
- **GregGuns trades in the pack's Vending Machine.** 178 trades with every module on: guns, akimbo pairs, melee tools, loaded magazines, rounds, grenades, gun parts and components, armor, special-slot gear, flight devices, armor parts, turret plates and a spawn egg for every NPC, the Attack Helicopter, Cyberdemon, Alien Bug and Dictator Dave included. All are priced in GregGuns Coins by tier. With the GregGuns quest line installed, each tier's trades open with that tier's intro quest. The trades are added in memory when the server starts; `config/vendingmachine/tradeDatabase.json` is not touched. New key `compat.vendingTrades` (default on).

### Quest loot bags

- GregGuns quests reward GregGuns loot bags: eight tier bags from Recruit (Steam) to General (ZPM), Spoils of War for boss kills and three ICBM Ordnance crates. They are EnhancedLootBags bags (ids 1701 to 1712), so they look, sort and take Fortune like the pack's own, and they hold GregGuns ammunition, magazines, parts, guns, armor and the materials GregGuns recipes use.
- GregGuns adds these groups to EnhancedLootBags at startup on both sides; the pack's `LootBags.xml` is not touched. `compat.lootBags` turns this off, and `config/gregguns-lootbags.xml` replaces the contents.
- A bag whose group is missing is kept with a chat line instead of crashing, and `/lootbags reload` keeps the GregGuns groups.
