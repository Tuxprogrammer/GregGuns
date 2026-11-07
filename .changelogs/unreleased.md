# Unreleased

Draft of the 0.4.0-beta release notes. When the release is cut, rename this file to `0.4.0-beta.md`, fill in the jar name, and start a fresh `unreleased.md`.

---

# GregGuns 0.4.0-beta (pre-release)

> [!WARNING]
> **This is a beta.** The ICBM module has been played on a dedicated server, soaked on the GTNH 2.9.0-RC-2 server pack, and checked with two clients and with shaders, but it has not had a long public playthrough. Back up your world before you update.

GregGuns 0.4.0 brings the **ICBM module** out of alpha and turns it **on by default**: warheads, missiles that fly from Galacticraft launch pads and Missile Silos, radar and air defense, endgame warheads, fallout, and abandoned silo complexes on every planet with solid ground, all on GregTech tiers. Since the alpha it has had a balance pass, blast doors and service runs in the complexes, a rebuilt silo sequence, Radar Gun targeting, and two new guide sheets (ICBM Recipes and an ICBM banner). The rest of GregGuns gets GT recycling for nearly every item, Vending Machine trades and quest loot bags, pack loot in structure chests, and fixes.

## Read this first (server owners)

- **Targets GTNH 2.9.0-RC-2.** Replace the jar on the server and on every client.
- **The ICBM module is on by default in new configs.** It needs Galacticraft, which the pack ships; without Galacticraft the module turns itself off and logs one warning, and the rest of GregGuns runs as before. OpenComputers is optional.
- **An existing `config/gregguns.cfg` keeps the value it was written with.** A server that ran 0.3.x or the 0.4.0 alpha with the module off still has `B:icbm=false`. To switch ICBM on, set it in the `modules` block and restart:
  ```
  modules {
      B:icbm=true
  }
  ```
  The server's flags apply to everyone who joins. Clients should match them: a client whose `modules.*` flags differ gets one chat line naming each module that differs, because recipes and NEI follow the client's own config.
- **Switching ICBM on in an existing world:** silo complexes generate only in new chunks, and complexes at the edge of explored land can come out half built. Explore new land (or use `/gregguns locate complex ... generate`) to find complete ones.
- **No pack file needs changing.** See "Pack changes" below for the one optional step.
- **Back up first.** Several items are removed outright (see Upgrading); stacks of them vanish.

## Upgrading

### From 0.3.3-beta

Everything under "New: the ICBM module" and "Changed since the alpha" below is new to you. The config file gains an `icbm` section, new `explosions.fallout*` keys and the new `compat.*` keys, all with safe defaults. The Zombie Hole, Bug Hole, Silo Guard Post and Silo Elite Post blocks are gone; any of them in an existing world turn to air, and structures place vanilla monster spawners instead the next time they generate.

### From 0.4.0-alpha

- **Removed items** (stacks vanish, no refund): Heavy Cloth and its wool recipe, Treated Leather, the Depleted Nuclear Power Cell and its refill recipe. GregGuns armor and its repairs take the pack's **Tanned Leather** instead (see below).
- **Warheads are built in steps now.** The one-step full-warhead Assembler recipes and the fill-1 condensed starter are gone. Warheads you already hold keep working.
- **Launch Control and silos.** A Missile Silo now takes its Launch Control as part of its structure. A silo built with its Launch Control merely nearby needs it moved into a wall, glass or floor-ring position of the silo. The silo's front is five wide: a silo built with the corners of its front empty no longer forms until they hold concrete, a hatch or the Launch Control.
- **Config keys.** `npcs.infernalAllowSpawnerNpcs` is replaced by `npcs.infernalSpawnerOptOut` (default off). `icbm.siloComplexActivityWeights` defaults to `30, 40, 30`; a config that keeps the old line keeps the old odds. Five switches flip to on (see Config), and `questbook.install` is on.
- **Existing silo complexes keep what they were built with.** The new blast doors, service runs, room machine tiers and room fit-out appear in complexes generated after the update. `/gregguns locate rebuild <cellX> <cellZ>` regenerates one complex in place.

## New: the ICBM module

- **Warheads, T1 (HV) to T5 (UHV).** Built in three steps: the Assembler makes an **Inert Warhead** of the casing's tier, fits a **payload** (sleeves, canisters, field generators, a fissile core and so on), and **Plastic Explosive charges** load it, 1 to 8 at a time in a crafting grid or a whole casing in the Assembler. Condensed, Fragmentation, Incendiary, Chemical, Thermobaric and more at T1 and T2; Exothermic, Endothermic, Nuclear, EMP, Ender and Anti-Gravitational at T3; Antimatter (UV) and Red Matter (UHV). Placed alone, a warhead is a demolition charge with a fuse that grows with its blast. Nuclear, antimatter and red matter kill everything living in their kill zone, bosses included.
- **De-arming.** A **Defuse Kit** and a live warhead in a crafting grid take out up to 8 charges, whole. The **Cutting Machine** empties a warhead, takes its payload out, and scraps a bare inert warhead into its plates, fuze, nose cone and hull plates.
- **Missiles, T1 to T3.** Build a body in Galacticraft's NASA Workbench (with the tier's schematic), or in the Assembler at HV and EV for T1 and T2, or on the Assembly Line at LuV for T3 after a Scanner research. Stand it on a Galacticraft launch pad, seat a warhead on its nose, fill it with rocket fuel, and fire it from the **Launch Control** by coordinates, a Radar Gun, a data stick, redstone or a GT machine controller cover. A missile climbs straight up its column to the top of the world, then arcs over onto the target. Ranges: 1,000 blocks at T1, 3,000 at T2, 8,000 at T3 (10,000 from a silo). Missiles sound like Galacticraft rockets and hold at most two chunks each.
- **Missile Silo.** A GT multiblock that builds the missile in its shaft from a body, a warhead and fuel through its hatches, opens its doors, counts down and launches. T3 missiles launch only from a silo. Its own Launch Control runs on the silo's EU.
- **Detection and defense.** HV and EV radars (small multiblocks with a spinning dish) and the IV Radar Tower; the Air-Defense Battery and its SAMs; the EMP Tower, which drains stored EU and jams multiblocks nearby. Handheld: Radar Gun, Remote Detonator, Defuse Kit. Every machine talks redstone, data sticks and, with OpenComputers, `gg_*` components; the **Golden Dome** floppy carries `launch`, `radarwatch`, `autodefense` and `emp` programs.
- **Fallout.** Nuclear strikes leave Irradiated Earth, and the strongest ones leave Trinitite and Fallout Goo, which GT machines process into a small share of the core's fuel. The **Antidote** clears radiation sickness and gas effects.
- **Launcher warheads.** Craft a warhead charge onto the Rocket Launcher, Grim Reaper, Grenade Launcher or Nuke Launcher; NEI's **Launcher Loading** page shows which loads where.
- **Abandoned silo complexes** on the Overworld and on every Galacticraft, GalaxySpace and Amun-Ra planet with solid ground: an underground base about 240 by 240 blocks across, 24 blocks down, with a 51 by 40 hub, three or four major halls behind blast doors, 35 to 40 rooms from 32 room kinds, and a silo gallery of 4 to 10 real silos, each missing one part for you to put back. Three activity levels (Alpha, Bravo, Charlie) set how ruined, lit, stocked and guarded a complex is. The loot ceiling rises with the planet's rocket tier, from EV on the Overworld and the Moon to UMV, and is rolled per complex on Ross128.
- **Guides and quests.** Ten ICBM guide sheets (ICBM banner, Warheads, Heavy Warheads, ICBM Recipes, Your First Missile, Missile Silo, Detection and Defense, Fallout, Abandoned Silos, Computer Control) and the ICBM quests in the GregGuns questbook tab.

## Changed since the alpha

### ICBM balance pass

- **Warheads are the expensive part of a missile.** Every Inert Warhead also takes the nose cone and 12 hull plates of its tier's Galacticraft rocket and its arming electronics (a Sensor, an Emitter and 2 circuits of its tier): T1 a Nose Cone and Heavy Duty Plates, T2 a Nose Cone and Heavy Duty Plates Tier 2, T3 a Heavy Nose Cone and Heavy Duty Plates Tier 4, T4 and T5 the Heavy Duty Nose Cones Tier 3 and 4 with Heavy Duty Plates Tier 6 and 7. Every casing is buildable at the floor of its tier: the Tier 2 casing at EV. A full missile costs about one Galacticraft rocket of its tier, measured in the pack: T1 0.99 to 1.32 rockets, T2 0.90 to 0.96, T3 1.02 to 2.62, nuclear 0.89, antimatter 1.35, red matter 0.94.
- **The Tier 3 missile body** is built from the Tier 4 rocket's parts: 4 Heavy Duty Plates Tier 4, 4 Heavy Rocket Fins and a Heavy Rocket Engine, all buildable at LuV, with the same circuits and lubricant. The T3 body has no GT recycling.
- The **nuclear payload** also takes 2 ZPM Field Generators to hold its core.
- Silo complex room machines (generators, buffers, pumps, labs and the rest) run mostly LV and MV, with the complex's top tier a rare find; radars and comms gear are unchanged. Inert T1 warheads are rarer in silo chests.
- The LuV ICBM quests pay an Ordnance III crate and HSS-G plates instead of IV rewards, and the LuV chapter's first quest offers Master circuits and LuV motors.
- Antimatter (UV) and Red Matter (UHV) quests carry their pack tier, need the pack's Tier 8 or Tier 9 quest, and pay new **Ordnance IV** and **Ordnance V** crates and Tier 4 and 5 warhead parts. The ZPM chapter and the Nuclear Warhead and Fallout quests offer ZPM parts.
- Blast damage was measured in the real pack against the targets (1,200 / 3,000 / 8,000 at the center for T1 / T2 / T3 types) and needed no change.

### Missiles, silos and Launch Control

- **The Missile Silo builds the missile in front of you.** A body from the Input Bus stands on the pad, then its 10-second cycle runs; a warhead is seated on its nose and mated in a second 10-second cycle; fuel goes in last. Parts can arrive in any order: a warhead with no body waits in the bus ("Waiting for a missile body"), and a body stacked in the shaft by hand is taken over. The parts stand in the shaft as blocks; breaking the silo mid-sequence leaves them standing.
- **The silo's front is five wide:** a 3 by 3 glass window around the controller with a hatch column each side. Generated silos have the Input Bus over the Energy Hatch with the Launch Control on top on the right, and the Input Hatch over the Maintenance Hatch on the left.
- **The Launch Control belongs to its silo.** A silo takes its Launch Control as part of its structure and that Launch Control fires its own silo only, paying its 300 EU per shot from the silo's Energy Hatch. A Launch Control outside a silo pairs with a pad only when it touches one of the pad's edges, level with it, and picks the pad with a ready missile. The screen names what it is bound to.
- **Radar Gun targeting.** The Launch Control's target slot takes a Radar Gun holding a target; press Load target, or right-click the block with the gun. Data sticks still work in the slot.
- A launched missile climbs straight up from its pad to the top of the world before it arcs over, so it no longer flies into the silo's roof or the hills and trees around a pad.
- The silo takes nothing without power: it shows "No power" and starts by itself once the hatch holds enough EU. A silo whose pad Galacticraft forms last now forms by itself within a couple of seconds.
- Missile Bodies T1 and T2 are built in the GT Assembler at HV and EV, with no research, as well as in the NASA Workbench. The Tier 3 body stays on the LuV Assembly Line.
- Missiles sound like Galacticraft rockets: the engine loop starts with the countdown, rises toward ignition, follows the flight and stops on impact, interception or abort.
- A missile in flight across a server restart resumes its flight and releases its chunk ticket when it lands.
- A silo never stands the next missile body under a missile that is counting down, so an aborted launch can fire again at once. An EMP-fried missile falls silent.

### Warheads

- A warhead is built in steps (inert casing, payload, charges); see "New: the ICBM module". An inert warhead cannot be placed, launched or set off, and a silo with one in its bus says so.
- No recipe takes both Industrial TNT and Plastic Explosive: payloads carry no explosive, and the launcher warheads take only Plastic Explosive.
- Scrapping a bare inert warhead in the Cutting Machine returns its plates, fuze, nose cone and hull plates; the arming electronics are spent. Macerating or arc smelting a warhead also returns its fuze's metal.
- New ICBM quest: De-arming.
- Gas clouds (chemical, debilitation, contagious, smoke) are a thick, clearly colored haze that reads at a glance from a distance, on grass as well as on stone.

### Silo complexes

- **Blast doors.** Every hall off the hub is closed by a Carpenter's Blocks garage door in the hall's own plating, as wide and tall as the hall's mouth. Right-click to open or shut the whole door. Charlie keeps its wings sealed; Bravo leaves its worked wings open and its dark wings shut; in Alpha a door is stuck open or breached, with columns torn out and jammed slats on rubble. Without Carpenter's Blocks the mouths stay open. On Ross128b and Ross128ba the door frames are the GregTech machine casing of the complex's tier.
- **Service runs.** Halls carry a cable tray of steel frame boxes with a real GregTech 4x cable under it, and the wide halls a large fluid pipe on the other wall, in metals that follow the complex's tier. In Alpha the runs are broken: gaps, fallen tray sections, stripped cable and missing pipe.
- **Rooms.** Every room is laid out from its own blueprint, with rows, islands and aisles between the lamp rows. Nine new rooms: an underground farm, a mushroom cellar, cold storage, a shooting range, a brig, a chemistry lab, showers and laundry, a recreation lounge and an incinerator. A complex deals its rooms like a deck of cards, every kind that fits before any repeats. Bunks and the infirmary have real beds; the pump room has two pools three blocks deep; brig cells are walled in on every side; machine rooms hold working GT machines.
- **Lived-in rooms.** BiblioCraft furniture is stocked to suit its room (books, the room's loot, potions, racks of guns or tools, typed notes); tables carry plates of food; a notice hangs under every room sign; furnaces, brewing stands and working GT machines hold finished goods; cauldrons hold water. How much a room holds follows its activity level.
- **Chests.** A whole complex holds exactly 100, 200 or 300 loot chests (Alpha, Bravo, Charlie) on every planet (`icbm.siloComplexChestTargets`). No chest is empty; leftover spots are empty crates. Each chest draws its room's own table, and its activity level decides how much is real loot and how much is junk and spoiled food.
- **Guards.** Every room, the hub included, rolls its own spawners: about one room in five in Alpha, zero to two per room (mostly one) in Bravo, one or two in every room in Charlie. Elite spawners (Commandos and Stormtroopers) turn up in rooms 30 or more blocks from the hub, never in Alpha. Halls hold none.
- **Ruins are picked clean.** In Alpha about 7 in 10 chests and wall frames are gone, one machine slot in five is empty, and nine in ten remaining machines are bare hulls.
- **Armories** show their guns on two display walls across the room, each gun in an item frame over a chest of its ammo.
- **Silo signs** ("Silo 1", "Silo 2"...) hang over each silo's window, and the hub's class sign has an activity sign under it (Alpha, Bravo or Charlie). Every silo in a complex comes with its own Launch Control.
- **Placement.** A complex under a village or temple generates 10 to 20 blocks deeper instead of being skipped. Roguelike Dungeons keep out of complexes. A silo's bore is open from its pad to the surface, and every room and hall stands on its own foundation, so caves never open under a floor.
- Lights are always light-emitting blocks; GT machines in complexes face the room's door side; generated Maintenance Hatches start in GT's default mode.

### NEI and tooltips

- NEI has a **Launcher Loading** page for the launcher warheads.
- The Radar Casing's tooltip says what it builds and where the hatches go.

### Everything else

- **GT recycling for nearly every GregGuns item, block and machine,** returning the materials of the cheapest recipe that makes it. Not recyclable on purpose: coins, spawn eggs, fallout, silo complex walls, world blocks from structures, cloth, leather and wooden parts, the Antidote, launcher warheads and guidance chips.
- **GregGuns Coins and the Vending Machine.** A GregGuns Coin in five values, and 176 trades in the pack's Vending Machine priced in coins by tier, unlocked by each tier's intro quest when the quest line is installed. Spawn eggs are offered only for NPC kinds that can spawn under the server's config.
- **Quest loot bags.** GregGuns quests reward GregGuns EnhancedLootBags bags (ids 1701 to 1714): eight tier bags, Spoils of War and the ICBM Ordnance crates.
- In single player with `compat.lootBags` off, a GregGuns loot bag says why it cannot open instead of "try again".
- **Tanned Leather.** GregGuns armor, its repairs, the Night Vision Goggles, the Ammo Pouch and the glider parts take the pack's Tanned Leather; mobs and loot bags drop it.
- **Nuclear Power Cell.** The Nuclear Death Ray takes one as its power core; the cell recipe makes one cell from half the materials. The cell, the Small Nuclear Warhead and the Rad Emitter are radioactive to carry without full hazmat.
- **Structure loot.** Structure chests hold a range of pack loot as well as GregGuns gear; guns are rarer and ammo common, and a gun always comes with some of its ammo. Fuel and Bio Tanks in chests are full.
- **Structures: calmer palettes.** Each structure is built from one material family; no more speckled walls or radiation-sign trim.
- **NPC drops.** A player kill drops a copy of the NPC's gun at 2% plus 1% per Looting level (never from bosses), and every NPC drops rounds for its gun, or parts of an energy gun. Mobs Info lists the drops.
- **NPCs** carry their gun at the hip when idle, hold it muzzle forward, and their armor follows their arms. Structure spawner NPCs can roll Elite, Ultra or Infernal.
- **Aim down sights** lines up on the Golden Gun, 9mm Pistol, Mac10, LMG, Thompson and Combat Shotgun.
- The Minigun's barrels whine as they wind up and down.
- GregGuns particles no longer draw a patch of the texture sheet with Angelica's shaders on.
- Abandoned structures no longer show Chisel's ERROR texture on glass panes.
- **Structures and silo complexes generate on every Overworld world type except flat**, including RWG2 and `RWG2_CONTINENTS` (the GTNH 2.9.0 default), which got none before. Only chunks generated after the update get them.
- Picking up an Assembly Line item no longer awards an unnamed GT achievement.
- **Quests:** the Fuel Tanks quest counts only full tanks, the Demolition Charge quest completes, and the Cyberdemon and Alien Bug quests are visible from the start.

## Config

Every key is documented in `docs/config.md`.

- **Defaults now on:** `modules.icbm`, `questbook.install`, `npcs.helicopter`, `npcs.alienBug`, `npcs.cyberdemon`, `worldgen.enabledAlienBugNest`, `utility.bioBlobSparesPlayers`. An existing config keeps its old values; set any of them yourself.
- **New keys:** `compat.vendingTrades`, `compat.lootBags` (and `config/gregguns-lootbags.xml` to replace the bag contents), `compat.roguelikeKeepOutOfComplexes`, `compat.enderioSpawnerBlacklist`, `npcs.infernalSpawnerOptOut` (replaces `npcs.infernalAllowSpawnerNpcs`), `icbm.siloComplexChestTargets`. `worldgen.structureWorldTypeBlacklist` (default `flat`) replaces the `worldgen.structureWorldTypes` allow-list: every world type not on it gets structures, so an old config's allow-list no longer applies.
- **The `icbm` section** covers blast budgets and radii (`icbm.blastBudgetMs`, `icbm.maxActiveBlasts`, `icbm.maxBlastRadius`, `icbm.maxGridMemoryMb`, `icbm.maxBlastTier`), per-tier terrain damage (`icbm.terrainDamageT1` to `T4`, which replace `explosions.blockDamage` for ICBM blasts), missiles (`icbm.maxMissilesInFlight`, fuel, range, scatter and silo factors, timeout, countdown and door timing), the EMP (`icbm.empMaxDevices`, `icbm.empMaxRadius` and the missile chances), the ender and anti-gravitational limits, and the silo complexes (`icbm.siloComplexEnabled`, `...Dimensions`, `...ChancePercent`, `...ActivityWeights`, `...Ross128Weights`, `...OverworldBiomes`, `...ChestTargets`). Claims and protection mods are always respected.
- **Commands (ops):** `/gregguns locate <kind> [radius_cells] [generate]` lists and generates GregGuns structures near you, silo complexes included, with their activity level; `/gregguns locate rebuild <cellX> <cellZ>` regenerates one silo complex in place.

## Pack changes

No pack file needs changing. The ICBM module reads other mods by registry name or reflection and writes no other mod's config: Galacticraft pads and fuel, Roguelike Dungeons, EnhancedLootBags, BiblioCraft furniture, OpenComputers components, the quests, the Vending Machine trades and GT's recipe maps are all handled in the jar. A recipe-clash review against the full pack found no clash with ICBM content.

- **Optional:** `config/forgeChunkLoading.cfg` needs no change at the pack's defaults. A missile in flight holds one ticket and at most two chunks, so the default cap of 20 missiles uses 20 of the pack's 200 tickets. A server that raises `icbm.maxMissilesInFlight` past that adds a `gregguns` section with a larger `maximumTicketCount`. A refused ticket is a refused launch, never a crash.
- **Optional:** a server that does not want ICBM sets `modules.icbm=false`. ICBM items and blocks in the world stay intact and show a "disabled" tooltip line.

## Performance

Soaked on the GTNH 2.9.0-RC-2 server pack with every module on, at the default caps (20 missiles in flight, 16 active blasts, a 12 ms blast budget, a 64-device EMP cap). Every run stayed playable and recovered, with no crash and no leaked chunk ticket:

- **20 missiles in flight** averaged 7 ms a tick; **100** (the highest allowed cap) ran 36 to 58 ms a tick for about 20 seconds while they generated terrain, then settled.
- **16 large blasts at once** (antimatter, nuclear, exothermic, endothermic, thermobaric) held about 14 TPS for the minute they ran, and the server was back to its baseline 8 seconds after the last crater. Expect lag spikes with 10 or more large blasts.
- **Red matter** and an **EMP over a GT base of 8,200 machine tiles** cost almost nothing past their first tick.
- A silo complex takes about one second of server time to generate, spread over the chunks it covers. Its GT cables and pipes tick like any GT pipe.
- Lower `icbm.blastBudgetMs` or `icbm.maxActiveBlasts` to trade blast speed for a steadier tick.

## Known issues

- Silo complexes on distant planets (GalaxySpace and Amun-Ra worlds) have been generated and checked on a test server, not played.
- **T Ceti E** (an ocean world) has no complexes.
- **Spawners are slow to mine.** GregTech makes every mob spawner very hard, so a guard spawner takes a long time to break by hand. A warhead destroys it.
- **Blast doors stop a walk, not a pickaxe.** Their slats are thin and break quickly. A GT cable or lever placed beside a door can open or shut it, as with any Carpenter's door.
- The T3 conventional warheads share the T1 model at full size.
- Everything listed for [0.3.3-beta](https://github.com/Tuxprogrammer/GregGuns/releases/tag/0.3.3-beta) still applies.

## Feedback

Bug reports are very welcome on [Issues](https://github.com/Tuxprogrammer/GregGuns/issues). Please include your GTNH version, Java version, whether the ICBM module is on, the crash report or `fml-client-latest.log`, and what you were doing at the time.

## Files

- `gregguns-0.4.0-beta.jar`: the mod. Put it in your instance's `mods/` folder, on the server and every client.
- `-dev` and `-sources` jars: for developers only.

MIT licensed, except the third-party art listed in `THIRD-PARTY-NOTICES.md` and `docs/assets/CREDITS.md`. The license doesn't cover Techguns, which isn't included.
