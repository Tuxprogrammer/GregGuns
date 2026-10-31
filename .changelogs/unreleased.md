# Unreleased

Notes for the next release. Fold these into that release's changelog when it is cut.

## Changed: ICBM balance pass

- **Warheads are the expensive part of a missile.** Every Inert Warhead now also takes the nose cone and 12 hull plates of its tier's Galacticraft rocket and its arming electronics (a Sensor, an Emitter and 2 circuits of its tier): T1 a Nose Cone and Heavy Duty Plates, T2 a Heavy Nose Cone and Heavy Duty Plates Tier 2, T3 a Heavy Nose Cone and Heavy Duty Plates Tier 4, T4 and T5 the Heavy Duty Nose Cones Tier 3 and 4 with Heavy Duty Plates Tier 6 and 7. A full missile now costs about one Galacticraft rocket of its tier.
- The **nuclear payload** also takes 2 ZPM Field Generators to hold its core.
- Scrapping a bare inert warhead in the Cutting Machine returns its plates, fuze, nose cone and hull plates; the arming electronics are spent.
- Silo complex room machines (generators, buffers, pumps, labs and the rest) now run mostly LV and MV, with the complex's top tier a rare find; radars and comms gear are unchanged. Inert T1 warheads are rarer in silo chests.
- The LuV ICBM quests pay an Ordnance III crate and HSS-G plates instead of IV rewards, and the LuV chapter's first quest offers Master circuits and LuV motors.

## Module flag mismatch warning

- When a client joins a server whose `modules.*` flags differ from the client's own `config/gregguns.cfg`, the player now gets one chat line naming each module that differs, and the log gets one warning. Recipes and NEI follow the client's own config, so set the same values there and restart. Singleplayer is unaffected.

## New: locate structures

- Ops get `/gregguns locate <kind> [radius_cells] [generate]` (permission level 2) for every GregGuns structure: `complex` (silo complexes), `military_base`, `big_bunker`, `factory_small`, `train_station`, `gas_station`, `house_medium`, `alien_bug_nest`, `aircraft_carrier`, `submarine` or `any`. It lists the sites near you in this dimension, nearest first, each as built, skipped (with the reason, such as ground too steep) or not generated yet. `generate` makes the nearest five undecided sites decide now, a chunk per tick, and reports which were built. A click on a built site's line teleports you to it. The log now names every structure's placed or skipped verdict.

## Tanned Leather replaces Heavy Cloth

- GregGuns armor is now lined with the pack's own **Tanned Leather** (`Backpack:tannedLeather`: Bound Leather on a drying rack, or two leather in phosphoric acid in the LV Chemical Bath) instead of GregGuns' Heavy Cloth, which is removed along with its wool recipe.
- Tanned Leather takes Heavy Cloth's place one for one in the Miner, Steam, Soldier's (T1 Combat), T2 Combat and Beret recipes, the Night Vision Goggles, the Ammo Pouch and the glider parts, and in every armor repair that used cloth. The light Bandit (Scout) set is mostly plain leather now: six Tanned Leather for the whole set.
- Zombies, bandits and soldiers drop Tanned Leather where they dropped Heavy Cloth, the Recruit loot bag holds two Tanned Leather, and the Vending Machine no longer sells Heavy Cloth.
- The Steam chapter's Heavy Cloth quest is replaced by a Tanned Leather quest, and quest rewards that paid Heavy Cloth pay Tanned Leather.

## Changed: Treated Leather and the Nuclear Power Cell

- GregGuns' **Treated Leather** is removed with its Chemical Bath recipe. The Ranger Veteran (Riot) set and its repairs take the pack's **Tanned Leather** at the same counts, like every other set. The Rifleman loot bag holds four Tanned Leather in its place.
- The **Nuclear Death Ray** recipe now takes one **Nuclear Power Cell** as its power core. The gun still runs on stored EU.
- The Nuclear Power Cell recipe makes one cell from half the materials.
- The Nuclear Power Cell, the Small Nuclear Warhead and the Rad Emitter are radioactive, like the tactical nuke rocket: carrying one without full radiation hazmat irradiates you. They still recycle into their plates, never into uranium.
- The **Depleted Nuclear Power Cell** and its refill recipe are removed: nothing reloads from a cell.

## Changed: quests

- Quests above ZPM carry their pack tier: Antimatter is a UV quest and Red Matter a UHV quest, with the pack's UV and UHV title colors, and each now also needs the pack's Tier 8 (UV) or Tier 9 (UHV) quest.
- The **Fuel Tanks** quest counts only full Fuel Tanks; empty or part-filled ones no longer complete it.
- The **Demolition Charge** quest completes: it now matches the warhead's fill as the number the warhead stores.
- The **Dungeon Weapons** quest says where the gun and blade come from in any pack: GregGuns ruins and camps, the LV Assembler, or dungeon chests on servers that stock them.
- The Nuclear Power Cell and Nuclear Death Ray quests describe the cell as the Death Ray's power core.
- Antimatter (UV) and Red Matter (UHV) pay their own tier: new **Ordnance IV** and **Ordnance V** loot bags and choices of the Tier 4 and Tier 5 warhead parts (naquadria and neutronium plates, Field Generators, Quantum Eyes, UHV Emitters, Inert warheads). The ZPM chapter and the Nuclear Warhead and Fallout quests offer ZPM parts in their choices.

## Changed

- `modules.icbm` is on by default. A server without Galacticraft still turns the ICBM module off and logs one warning. A server that does not want ICBM sets `modules.icbm=false`.
- Five switches are on by default: `npcs.helicopter`, `npcs.alienBug`, `npcs.cyberdemon`, `worldgen.enabledAlienBugNest` and `utility.bioBlobSparesPlayers`. Camps can send the Attack Helicopter and a Camp Flag break calls one in, Alien Bugs and the Cyberdemon (Nether) may spawn, Alien Bug Nests generate in sandy biomes, and Bio Blob poison spares players (other creatures are still poisoned). Set any of them to `false` to turn it off.
- `questbook.install` is on by default and shows in the in-game config GUI: a server with BetterQuesting gets the GregGuns quest line on start. Set it to `false` to skip the install. The Cyberdemon and Alien Bug quests are visible from the start.

## Changed: the Launch Control belongs to its silo

- A Launch Control between two silos no longer reports "No missile" because it picked the empty one. A Missile Silo now takes its Launch Control as part of its structure: build it in place of any wall, glass or floor-ring block (one per silo). That Launch Control fires its own silo and no other, and says so when the silo isn't formed. Silos built with a Launch Control merely nearby need it moved into the structure.
- A Launch Control that isn't part of a silo pairs with a Galacticraft pad only when it touches one of the pad's edges, level with it, the way Galacticraft's own launch controller does. Otherwise it reads "No pad adjacent". Next to two pads it picks the one with a ready missile, and always the same one when both are ready.
- The Launch Control screen names what it is bound to: the silo's sign ("Silo 2") or the silo or pad and its coordinates.
- Every silo in a silo complex comes with its own Launch Control, on top of the right-hand hatch column beside its window.
- A silo's own Launch Control runs on the silo's power: it pays its 300 EU per shot from the silo's Energy Hatch, so it needs no cable. Its screen shows the silo's EU, and "Silo has too little EU" when the hatch is short.

## Changed: abandoned silo complexes

- Abandoned complexes now keep one or two guard spawners (Army Soldiers and Commandos) at a hall branch or a guard post room, so a ruin is no longer empty of danger.
- Abandoned complexes are picked clean: about 7 in 10 chests and wall frames are gone, one machine slot in five is empty, and nine in ten of the machines that remain are bare hulls. Finding a working machine in a ruin is a rare treat. Partly abandoned and operational complexes are unchanged.

## GT recycling for every GregGuns item

- Nearly every GregGuns item, block and machine now recycles in GregTech's Macerator and Arc Furnace (and the other GT recyclers), returning the materials of the cheapest recipe that makes it, the way GT recycles its own machines. This adds most guns (the Assembly Line guns too), the Gun Mount Hatches, every casing and Bastion Plating, turret armor plates, explosive charges, grenades, the LMG, AS50 and Minigun magazines, rockets, shotgun shells and the other press ammo, the chainsaw and power hammers. Circuits, gunpowder and other explosives, and radioactive materials do not come back.
- Not recyclable on purpose: coins, spawn eggs, fallout, silo complex walls, world blocks from structures, cloth, leather and wooden parts (GT gives those materials no recycling value), the antidote, launcher warheads and guidance chips.

## Structure loot

- Chests in GregGuns structures now hold a range of pack loot as well as GregGuns gear, modeled on the top floors of Roguelike Dungeons: food (HarvestCraft meals included), torches, string and other everyday materials, ores and GT ingots, tools and Forestry bags, potions and bandages, bows, swords and TConstruct parts, armor and trinkets, books and enchanted books. Plain chests are mostly this general loot with a little GregGuns ammo and gear; weapon crates lean toward weapons; reinforced crates toward GT materials; barracks toward armor, food and ammo.
- Guns are rarer and ammo is common. A weapon crate holds ammo almost every time and a gun about one time in four, a plain chest a gun about one time in eight, and a gun always comes with some of its own ammo. Richer structures give more ammo and gear, not more guns.
- Fuel and Bio Tanks found in chests are full.
- Every item rolled for a structure chest lands in it; items no longer replace each other in the same slot.
- Items from a mod the pack does not have are left out of the chests.

## Structures: calmer palettes

- Newly generated structures are built from one material family each, matching the Techguns originals: the Small Factory is rusted steel on a concrete base, the Train Station weathered red brick under a gray stone roof, the Gas Station an asphalt forecourt with a stone-brick shop under a steel canopy, the Medium House gray stone under its dark red roof, the Big Bunker plain concrete, and the carrier and submarine gray and dark steel. Walls no longer speckle concrete with dark bricks and purple metal, roofs no longer checker gray and sandstone stairs, and no wall or deck repeats Chisel's radiation sign, glowing hex plating or flame lanterns: hazard trim is a hazard stripe and lights are light panels.
- Military Bases and the Big Bunker: containers and sandbags are Chisel hempcrete in red and brown (GregTech's stone layers no longer eat into either), crates are Chisel crates, and desert tents use Chisel's desert camo.
- The Gas Station canopy now carries a whole iron-bar railing; before, only a fence, an iron bar and a fence stood in one corner of its roof.

## Silo signs

- Every silo in a complex now has a sign reading "Silo 1", "Silo 2" and so on (north to south, then west to east), hanging on the silo's concrete over its glass window, centered on the controller and facing the gallery. A silo with its controller missing gets the same sign. The Launch Control's screen reads the silo's name from a sign in that spot, on a silo you build too.
- The hub's "Silo Complex / Class" sign now has a second sign under it naming the complex's activity level: Alpha (abandoned), Bravo (partly abandoned) or Charlie (operational).

## Changed: silo complex armories

- The guns of a complex's armory no longer hang on the outer walls. Two short display walls (two blocks tall, open above) stand across the room with walkways between them, each showing a gun in an item frame every fifth block. A chest on the floor directly below each gun holds that gun's ammo: loaded magazines and loose rounds, or the rockets, grenades or fuel it burns. The armory shows about as many guns as before and holds far more ammo. Abandoned complexes keep fewer of the displays.

## Fixed

- Missiles no longer fly into the roof or walls around a Missile Silo, or the hills and trees around a launch pad. A launched missile now climbs straight up from its pad to the top of the world, then arcs over and down onto its target. Range and accuracy are as before; the flight takes a few seconds longer. A short shot from that height comes down at a steadier speed, so air defense can still catch it.

- The Missile Silo no longer starts a mating cycle or a fuel pump without power. With no EU in its Energy Hatch it used to take the body, warhead or fuel, power-fail on the first tick and switch itself back on, over and over. It now takes nothing, shows "No power" in its status line, and starts by itself once the hatch holds enough EU.

- Silo complex lights are always light-emitting blocks. A lit lamp is Chisel light stone or glowstone and is checked to give light when the palette resolves it, and a dead lamp is never a Chisel Technical vent. The vents are a sparse accent in the ceilings of plant rooms, halls, bunkers and granite halls, never a light. The role audit now reports a lit lamp that gives no light.

## Changed: the Missile Silo's front is five wide

- The silo's front, in its three glass rings, is now five wide: a clear 3 by 3 glass window around the controller, with a hatch column on each side, flush with the glass. The two hatch columns are part of the structure and must hold concrete, a hatch or the Launch Control; a silo built with the corners of its front left empty no longer forms until they are filled. Concrete inside the window no longer counts.
- Generated silos use this layout. From the gallery: on the right, the Input Bus over the Energy Hatch with the Launch Control on top; on the left, the Input Hatch over the Maintenance Hatch with concrete on top. A silo missing its Energy Hatch has a concrete block at the foot of the right-hand column.
- The walkway in front of each silo is now five wide and four high, so the whole face shows from the gallery.

## New: rebuild a silo complex

- Ops get `/gregguns locate rebuild <cellX> <cellZ>`: it clears a built silo complex's box (from just above the bedrock up to the surface, its surface buildings included) and generates it again with the current rooms, at the activity level and depth it was first given. It works one chunk per tick and reports its progress. The rock and caves under the box come back as the terrain generator makes them; ores and other things chunk population adds there do not.

## Fixed

- A Missile Silo whose pad Galacticraft forms after the rest is built now forms by itself within a couple of seconds. It used to stay "incomplete" until the player clicked Update Structure.
- Picking up a GregGuns item that GT builds on the Assembly Line (a Tier 3 Missile Body, a gun, armor) no longer awards an unnamed `achievement.tile.gregguns...` achievement. GregGuns items are taken out of GregTech's auto-generated achievements at startup; GT's recipe unlock on pickup still works.
- Abandoned houses and other structures no longer place Chisel glass panes (and gold blocks) at a variant Chisel does not define, which showed the ERROR texture. They use Chisel's borderless pane and large-ingot gold now, and any Chisel block at an undefined variant is replaced by its vanilla stand-in.

- GT machines in generated silo complexes (pumps, generators, battery buffers, workbenches and the rest) now face the room's door side instead of straight down, and a pump's output side is its back, so the machines read and plug in like ones you placed yourself.
- Aiming down the sights now lines up on the screen center for the Golden Gun, the 9mm Pistol and the Mac10 (they sat well left of it at every field of view), and the LMG now aims through its red dot instead of through its muzzle sight, which put the view on the buttstock.
- The Thompson and the Combat Shotgun now aim through their front blade sight, with the muzzle tipped up a few degrees so the tall rear sight drops out of the line of sight, instead of filling the middle of the screen. Any gun can carry such a tilt in its aim data (`AdsTable`).
- The Minigun's barrels now whine as they wind up before the first shot and wind down after the last, with real CC0 recordings that follow the shooter and stop cleanly when you let go, switch away, reload or die.
- GregGuns NPCs from structure spawners (silo and bunker guards, room guards) can now spawn as Elite, Ultra or Infernal mobs, like every other GregGuns NPC. Spawner NPCs were skipped by default; the new `npcs.infernalSpawnerOptOut` (default off) restores that as a farm guard, and replaces `npcs.infernalAllowSpawnerNpcs`.

- NPCs without a target carry their gun at the hip, angled forward and down with the gun arm held still while they walk; they raise it to aim as before.
- NPCs hold their guns the right way round: the muzzle points forward along the raised arm, for every NPC and every gun, as it does on a player. Players' own guns are unchanged.
- NPC armor follows the NPC's arms: an NPC carrying its gun at the hip or aiming it no longer shows its bare arm outside the sleeves, for every armor model. Players are unchanged.
- GregGuns particles (muzzle flashes, smoke, sparks, blasts) no longer draw a patch of the whole block and item texture sheet with Angelica's shader support on. A particle's random turn is now a mirror, which Angelica draws correctly.
- The Maintenance Hatch of a generated silo, and of the radar station, no longer shows a black cross over its wrench face: generated GT hatches and machines start in GT's default modes, as placed ones do.
- Missile Bodies T1 and T2 are now built in the GT Assembler at HV and EV, with no research, instead of on the Assembly Line, which GTNH players only reach at LuV. Same inputs and times: plates, fins, engine, 2 circuits and 250 L of lubricant. The Tier 3 body stays on the LuV Assembly Line.
- A missile in flight when the server stops flies on after the restart and reaches its target, with no player nearby. It used to hang in the air and keep its chunk ticket for good.
- Missiles now sound like Galacticraft rockets: the engine loop starts with the countdown, rises in pitch toward ignition, follows the missile through the climb and arc, and stops on impact, interception or abort.

## Changed: the Missile Silo builds the missile in front of you

- The Missile Silo now assembles the missile in its shaft one part at a time, each part appearing before its working cycle starts. A missile body in the Input Bus is stood on the launch pad, then its cycle runs; a warhead is then seated on the body's nose and mated in a second cycle, which ends with the armed missile; fuel from the Input Hatch goes in last. The two cycles take 10 seconds each, the same 20 seconds as before.
- Parts can arrive in any order or all at once: a warhead with no body waits in the bus ("Waiting for a missile body"), and fuel waits in the hatch. A body alone goes up and waits for its warhead.
- The parts stand in the shaft as blocks, never inside the controller. Breaking the silo mid-sequence leaves them standing; breaking a body under a seated warhead stops the cycle and drops each part once.
- A body stacked in the shaft by hand is now taken over by the silo, which mates a warhead from the bus onto it. Arming by hand still works.

## Changed: building an ICBM warhead

- A warhead is now built in steps. First the Assembler makes an **Inert Warhead** of its casing tier from the casing plates and the Fuze Assembly. The Assembler then fits a **payload** (shrapnel sleeve, napalm, gas canisters, field generators, a fissile core and so on). Last, **Plastic Explosive charges** go in, one charge per unit of fill. Load 1 to 8 at a time in a crafting grid (top-ups work), or fill the whole casing in the Assembler. Charges alone in a T1 casing make a Condensed warhead.
- An inert warhead cannot be placed, launched or set off, and a silo with one in its Input Bus says it is inert.
- No recipe takes both Industrial TNT and Plastic Explosive any more: the warhead payloads carry no explosive, and the launcher warheads take only Plastic Explosive (the HE, HEAT, Phosphorus and Grenade HE ones no longer take vanilla TNT beside it).
- The one-step full-warhead Assembler recipes and the fill-1 condensed starter recipe are gone. Existing warheads keep working.

## New: de-arming a warhead

- Put a live warhead and a **Defuse Kit** in a crafting grid to take out up to 8 Plastic Explosive per craft. The charges come back whole and the kit loses one use. The last charge out leaves the inert warhead with its payload, ready to be loaded again.
- The **Cutting Machine** runs the rest of the chain backwards at the warhead's own tier: it empties a full warhead at once, takes a payload out (its parts come back, its fluid does not) and cuts a bare inert warhead back into its plates and Fuze Assembly. Check NEI.
- Macerating or arc smelting a warhead now also returns its fuze's metal.
- New ICBM quest: De-arming.

## New: GregGuns Coins and the Vending Machine

- **GregGuns Coin** (`gregguns:coin`), GregGuns' own currency, in five values like the pack's coins: 1, 10, 100, 1000 and 10K (damage 0 to 4). One coin breaks into ten of the next lower value in a crafting grid; ten coins and programmed circuit 10 make one of the next higher value in any GT Assembler.
- **GregGuns trades in the pack's Vending Machine.** 177 trades with every module on: guns, akimbo pairs, melee tools, loaded magazines, rounds, grenades, gun parts and components, armor, special-slot gear, flight devices, armor parts, turret plates and a spawn egg for every NPC, the Attack Helicopter, Cyberdemon, Alien Bug and Dictator Dave included. All are priced in GregGuns Coins by tier. With the GregGuns quest line installed, each tier's trades open with that tier's intro quest. The trades are added in memory when the server starts; `config/vendingmachine/tradeDatabase.json` is not touched. New key `compat.vendingTrades` (default on).

### Quest loot bags

- GregGuns quests reward GregGuns loot bags: eight tier bags from Recruit (Steam) to General (ZPM), Spoils of War for boss kills and five ICBM Ordnance crates. They are EnhancedLootBags bags (ids 1701 to 1714), so they look, sort and take Fortune like the pack's own, and they hold GregGuns ammunition, magazines, parts, guns, armor and the materials GregGuns recipes use.
- GregGuns adds these groups to EnhancedLootBags at startup on both sides; the pack's `LootBags.xml` is not touched. `compat.lootBags` turns this off, and `config/gregguns-lootbags.xml` replaces the contents.
- A bag whose group is missing is kept with a chat line instead of crashing, and `/lootbags reload` keeps the GregGuns groups.

## ICBM: Radar Gun targeting

- The Launch Control's target slot takes a Radar Gun: drop one that holds a target into the slot and press Load target (or right-click the block with it). The gun stays in the slot. An empty slot shows a faint Radar Gun, the screen reads "No target: use a Radar Gun" with no target set, and the tooltips, the Launch Control and Handheld Tools quests and the guide sheets lead with the gun.
- Data sticks still work in the slot, quietly: nothing in the Launch Control advertises them any more.

## Changed: silo complex placement and activity levels

- A silo complex under a village or temple is no longer skipped: it generates 10 to 20 blocks deeper, and its shafts and escape ladder reach the surface where they always would. The village is found from where the world will put it, so the result is the same whichever chunk you reach first. A complex is skipped only where it cannot go that deep above bedrock.
- Roguelike Dungeons keep out of silo complexes: a dungeon that would start near a complex picks another spot, and none of its blocks are written inside one. The new switch `compat.roguelikeKeepOutOfComplexes` (default on) turns this off, and the startup log says whether it attached.
- Activity levels now roll 30% abandoned, 40% partly abandoned and 30% operational (`icbm.siloComplexActivityWeights` default `30, 40, 30`; a server that keeps the old line in its config keeps the old odds). Each complex rolls once, when it is first placed, and keeps its level if the weights change later. Complexes placed before this update roll their level with the current weights.
- `/gregguns locate complex` shows each built complex's activity level and how much deeper it lies under a village.

## Fixed: silo complex shafts and foundations

- A silo's bore is open from its launch pad to the surface: no complex wall stands inside a silo, and trees, snow, glow lichen and other growth that later terrain generation puts into a bore or onto its cap is cleared once the land around the silo has generated. The concrete cap still closes the shaft, half closes it or leaves it open by the complex's activity level.
- Every room, hall and tunnel stands on its own foundation, with solid fill behind its walls and over its ceiling, so caves never open under a floor or beside a wall.

## Changed: NPC drops

- GregGuns NPCs no longer drop the gun they hold as a vanilla equipment drop. Instead, a player kill drops a copy of the gun at 2%, plus 1% per Looting level (never from bosses).
- Every NPC that drops loot also drops rounds for the gun it carried: a quarter to three quarters of a magazine, at least one round, or one full tank for a tank-fed gun.
- NPCs carrying an energy gun (Laser, Tesla, Blaster and the rest) drop parts and materials of that gun instead of ammunition: two picks from its recipe, such as plates, screws, foil, lenses, small gears, its barrel or its receiver.
- With Mobs Info installed, each GregGuns NPC's page lists the rare gun, ammo and parts drops.

## Changed: silo complex rooms

- Every room of a silo complex is laid out from its own blueprint that uses the whole floor: rows, islands and aisles between the lamp rows, with wall pieces where they belong. The hub is a container yard with lanes kept clear to every portal.
- Nine new rooms: an underground farm, a mushroom cellar, cold storage, a shooting range with wool targets downrange, a brig, a chemistry lab, showers and laundry, a recreation lounge and an incinerator.
- A complex deals its rooms like a deck of cards: it uses every room kind that fits before it repeats one.
- Bunks and the infirmary have real beds, in head-to-head pairs sharing a locker, with a chest at the foot of each bed facing the aisle. The infirmary has curtains and treatment bays.
- Every room's way in runs as wide as its doorway with the frame either side, from the door to the back wall, and a floor strip in the room's design lies under it.
- The pump room has two large pools, three blocks deep and lined in concrete, either side of the aisle, with the pumps standing along the aisle between them. An abandoned pump room's pools have overflowed onto the floor.
- Brig cells are walled in on every side in the room's own wall block: walls between the cells and behind the second row, and a solid jamb beside each iron door, with iron bars on the rest of the front. No cell is ever left open.
- Rooms are dressed with Chisel shipping containers, wooden crates and metal-block props, and the machine rooms hold working GT machines: Chemical Reactors in the chemistry lab, machine tools in the workshop, recyclers and arc furnaces in the incinerator.
- Each room shows its complex's activity level. Operational (Charlie): everything lit and in place, furnaces burning, crops ripe, machines running. Partly abandoned (Bravo): lit rooms still in service, dark rooms mothballed with pieces missing and machines dead. Abandoned (Alpha): gutted rooms with rubble piles, missing furniture, dead fields and broken glass. Wear and rubble never block the way to a chest, a machine or a door.

## Changed: silo complex chests

- A whole silo complex holds exactly **100** loot chests when abandoned, **200** when partly abandoned and **300** when operational, on every planet (config `icbm.siloComplexChestTargets`). No chest is ever empty. Chest spots left over are empty wooden crates, and caved-in rooms hold no chests. Armory display chests come on top of the count.
- Each chest draws from its **room's own table**: the mess and cafeteria are food-heavy (Pam's HarvestCraft and vanilla meals, snacks, drinks and ingredients), the bunks armor-heavy, the shooting range gun- and ammo-heavy, and the missile assembly floor holds missile body parts and warhead components (and, rarely, a whole missile body: intact in an active complex, salvaged in an abandoned one). Every other room has its own theme, from the chem lab's chemical cells to the incinerator's ash and scrap.
- How lived-in a room is shows in its chests: an operational complex's chests are full, a partly abandoned complex's dark rooms hold less, plus some junk and spoiled food, and an abandoned complex's chests hold a few real finds among junk, rotten food and worn armor. Canned food never spoils.

## Changed: silo complex spawners

- Every room of a silo complex, the middle assembly bay included, rolls its own mob spawners, so the guards spread through the whole complex, the outer rooms and far wings among them, instead of gathering around the middle. An abandoned complex puts one in about one room in five. A partly abandoned complex puts zero, one or two in a room, mostly one. An operational complex puts one or two in every room. The spawners stand on bare floor, and the halls hold none.
## Silo complexes look lived in

- BiblioCraft furniture in silo complexes is stocked to suit its room: bookcases hold crew notes and manuals, shelves and tables hold the room's own loot, medicine shelves hold potions, racks hold guns and knives in armed rooms and tools elsewhere, armor stands hold armor or stand empty, desks hold an open book, typewriters hold paper and a typed note, and clipboards carry the room's checklist.
- Mess halls and cafeterias have dinner plates with food on their tables and cookie jars on their counters; offices and the rec lounge have flower pots; rooms have clipboards on the wall over their desks, consoles and bunks.
- A notice hangs under every room's name sign: a duty roster or a note on the room's work in a staffed room, a closing notice in a shut one, a last log in a ruin.
- Furnaces hold fuel and the room's work, brewing stands hold potions, and working GregTech machines hold finished outputs (never an input or a job in progress).
- Every cauldron holds water, and a staffed farm's crops grow at mixed stages.
- How much a room holds follows its activity level: a Charlie complex is fully stocked, Bravo's lit rooms are well stocked and its dark rooms half empty, and an Alpha ruin holds little, much of it spoiled or worn.

## Silo complexes: blast doors and service runs

- Every hall off a complex's central hub is closed by a blast door: a Carpenter's Blocks garage door in the hall's own plating, as wide and tall as the hall's mouth. Right-click it to open or shut the whole door. An operational complex keeps its wings sealed; a partly abandoned one leaves its worked wings open and its dark wings shut; in an abandoned complex a door is stuck open or breached, with columns torn out, jammed slats on rubble and debris on the floor. Without Carpenter's Blocks the mouths stay open.
- On Ross128b and Ross128ba the blast-door frames are the GregTech machine casing of the complex's tier, so you can read a complex's class from its doors.
- Halls and hallways carry services under the ceiling: a cable tray of steel frame boxes with a real GregTech cable hung under it, and in the wide halls a large fluid pipe on the other wall. The cable and pipe follow the complex's tier. In an abandoned complex the runs are broken: gaps in the tray, fallen tray sections, stripped cable and missing pipe.

## ICBM: NEI and tooltips

- NEI has a **Launcher Loading** page: look up a Rocket Launcher, Grim Reaper, Grenade Launcher or Nuke Launcher, or a launcher warhead, to see which warheads load into which launcher in the crafting grid.
- The Radar Casing's tooltip says what it builds (the Radar and the Radar Tower) and where their hatches go.
