# GregGuns configuration

Reference for every option in `config/gregguns.cfg`. GTNHLib creates the file on first run, with a comment above each key, and the in-game config GUI lists the same options. The classes behind it are in `com.gregguns.core.config`; the design is in [gregguns-spec/01-core/config-gtnhlib.md](gregguns-spec/01-core/config-gtnhlib.md).

Keep this file in step with the code: a PR that adds or changes an option updates its row here.

## Module flags (`modules`)

Each roadmap content area is a module. **A flag never changes what GregGuns registers.** Every block, item, entity, tile entity, machine and network packet exists whatever the flags say, so a client and a server with different flags can join each other, and turning a module off never makes Forge treat its items as missing or delete them from a world. A flag only switches behavior. When a module is `false`:

- it registers no recipes and no event handlers (so no crafting, no natural spawning, no structure generation);
- its machines do nothing, its guns do not fire and its other items and blocks cannot be used (a right click or swing is cancelled with a chat line);
- its items and blocks are hidden from NEI and the GregGuns creative tab;
- items that already exist stay intact, with a tooltip line "Disabled by the server's GregGuns config (modules.X)".

**A flag defaults to `true` once its milestone's exit gate is met.** The gates for M2 to M8 are met, and the ICBM beta checkpoint ([ROADMAP-ICBM](ROADMAP-ICBM.md)) is met, so every module flag defaults to `true`, `worldgen` (M8) and `icbm` included. `icbm` also needs the Galacticraft mod. The PR that completes a milestone flips that milestone's default, in `ModulesConfig` and in the table below.

A module also needs the modules listed in "Needs". If a flag is on but a needed module is off, the module behaves as off and a warning is logged. This is a behavior rule only; it never gates registration.

| Key | Type | Default | Range | Synced | Needs | Milestone | What it enables |
|---|---|---|---|---|---|---|---|
| `modules.guns` | boolean | `true` | n/a | login packet | none | M1, M2 | Damage system, guns, projectiles, ammo |
| `modules.armor` | boolean | `true` | n/a | login packet | `guns` | M3 | Armor, set bonuses, special slots |
| `modules.machines` | boolean | `true` | n/a | login packet | `guns` | M4 | Production chain and GregTech machines |
| `modules.energyWeapons` | boolean | `true` | n/a | login packet | `guns`, `machines` | M5 | Energy and exotic weapons |
| `modules.utility` | boolean | `true` | n/a | login packet | `machines` | M6 | Utility and defense machines |
| `modules.npcs` | boolean | `true` | n/a | login packet | `guns` | M7 | NPCs and spawning |
| `modules.worldgen` | boolean | `true` | n/a | login packet | `npcs` | M8 | Structures and worldgen loot |
| `modules.icbm` | boolean | `true` | n/a | login packet | `guns`, `machines`, and the Galacticraft mod | ICBM I0 | ICBM missiles and warheads. On by default. With the flag on and Galacticraft not installed, the module turns itself off and logs one warning (D9); the flag then reads as off, also in the login packet. |

All module flags need a game restart. **The server's flags win on a multiplayer client.** At login the server sends which modules are active on it (`PacketModuleSync`, on the `gregguns` channel); the client uses those for behavior and visibility (tooltips, NEI, the creative tab, accuracy and gating) until it disconnects, and its own flags in single player. One exception: recipes are registered at startup from each side's own config, so a client whose module is off shows no recipes for it in its crafting grid and NEI.

### Server-authoritative keys

The module flags (login packet) and every key marked `Synced: yes` in the tables below (GTNHLib `@Config.Sync`: the server value overrides the client's while connected and is restored on disconnect) are server-authoritative. Everything else is either server-side only (NPC, worldgen and utility tuning, which the client never reads) or a client-only preference (`client.*`: HUD, animations, sounds, key binds), and stays local.

The "Needs" column is the module order in `ModuleId`, taken from the roadmap's entry gates.

## Compat (`compat`)

In-memory pack-fit hooks ([PACK-CHANGES.md](gregguns-spec/PACK-CHANGES.md)). Each adjusts another mod's already-loaded settings at startup, never its config file, and does nothing when that mod is absent. Not synced: each side applies its own copy.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `compat.backhandGuns` | boolean | `true` | n/a | no | Adds every GregGuns gun to Backhand's `offhandBlacklist` and `mainhandUseStopsOffhandFallback`. |
| `compat.thaumcraftMobAspects` | boolean | `true` | n/a | no | Registers Thaumcraft scan aspects for the GregGuns NPCs. |
| `compat.listHooks` | boolean | `true` | n/a | no | Adds `gregguns.` to AngerMod's `butcherItems` and `gregguns.SimpleTurret` to Mobs Info's `MobBlacklist`. |
| `compat.enderioSpawnerBlacklist` | boolean | `true` | n/a | no | Puts every GregGuns NPC on EnderIO's Powered Spawner blacklist by IMC: a broken GregGuns spawner drops no Broken Spawner and releases no burst, and GregGuns NPCs stay out of the Powered Spawner. |
| `compat.vendingTrades` | boolean | `true` | n/a | no | Server only. Adds the GregGuns trades (176 with every module on: guns, ammo, magazines, parts, armor, gear and NPC spawn eggs, priced in GregGuns Coins) to the Vending Machine in memory once the server has started; `tradeDatabase.json` is never written. With the GregGuns quest line installed, each tier's trades open with that tier's intro quest. Off removes them at the next start. Needs the Vending Machine; ignored without it. See [coins-and-vending.md](gregguns-spec/06-items/coins-and-vending.md). |
| `compat.lootBags` | boolean | `true` | n/a | no | Adds the GregGuns loot bag groups (EnhancedLootBags ids 1701 to 1714, the quest rewards) to EnhancedLootBags' group list at startup, from the jar or from `config/gregguns-lootbags.xml` when that file exists. `LootBags.xml` is never written. Turn it off only when the pack defines these groups itself; GregGuns still keeps a bag whose group is missing from crashing (the use is denied with a chat line). |
| `compat.roguelikeKeepOutOfComplexes` | boolean | `true` | n/a | no | Keeps Roguelike Dungeons out of silo complexes: a dungeon whose entrance would lie near a complex tries another spot, and none of its blocks are written inside one. Needs Roguelike Dungeons and the ICBM module. The startup log says whether it attached; if it cannot, Roguelike Dungeons runs unchanged. |

## Debug (`debug`)

Hidden from the in-game config GUI (developer aids; edit the file).

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `debug.recipeDump` | boolean | `false` | n/a | no | Enables the op-only `/gregguns dumprecipes <file>` command (recipe-clash review). Leave off in play. |

## Bio Gun (`bioGun`)

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `bioGun.acceptBiomass` | boolean | `false` | n/a | yes | Also lets the Bio Tank take plain GT Biomass, besides BartWorks' E. coli culture fluid (made from Biomass in the Bacterial Vat). Biomass is accepted regardless when the culture fluid cannot be resolved (BartWorks missing). |

## Utility (`utility`)

Turret, explosive charge, Bio Blob and Camp Flag tuning (M6). Server-side only, not synced.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `utility.turretUpkeepEuPerTick` | int | `4` | 0 to 4096 | no | EU per tick a Turret draws while on guard. |
| `utility.turretEuPerHealedHp` | int | `120` | 0 to 100000 | no | EU spent per hit point a Turret heals. |
| `utility.turretRepairEuPerTick` | int | `10` | 0 to 4096 | no | EU per tick during the repair after the entity died. |
| `utility.turretRepairTicks` | int | `200` | 20 to 24000 | no | Ticks of repair before the entity respawns. |
| `utility.turretRange` | int | `24` | 4 to 64 | no | Blocks within which a Turret looks for targets. |
| `utility.chargeMaxHardness` | float | `30.0` | 0 to 100000 | no | Hardest block the Explosive Charge bores (exclusive). |
| `utility.chargeAdvancedMaxHardness` | float | `100.0` | 0 to 100000 | no | Hardest block the Advanced Explosive Charge bores (inclusive). |
| `utility.bioBlobSparesPlayers` | boolean | `true` | n/a | no | Bio Blob poison skips players. |
| `utility.bioBlobDecayTicks` | int | `60` | 1 to 24000 | no | Ticks per Bio Blob decay step. |
| `utility.campFlagSpawnIntervalTicks` | int | `200` | 20 to 24000 | no | Ticks between Camp Flag spawn attempts. |
| `utility.campFlagCapFactor` | float | `0.75` | 0 to 10 | no | NPC cap is int(((sizeX + sizeZ) / 2) * factor). |
| `utility.campFlagBreakHelicopter` | boolean | `true` | n/a | no | Breaking a Camp Flag sends a helicopter (inert until NPCs exist). |

## NPCs (`npcs`)

NPC spawn gate, weights, danger distances, caps and gun damage (M7). Server-side only, not synced. A weight of 0 means the mob is never selected (it is still registered, like every other mob).

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `npcs.spawnDimensions` | int list | `[0]` | any | no | Dimension ids that use the Overworld spawn table. A dimension in no spawn list has no natural NPC spawns. |
| `npcs.netherSpawnDimensions` | int list | `[-1]` | any | no | Dimension ids that use the Nether spawn table (Zombie Pigman Soldiers, Super Mutants, the Cyberdemon), in Nether biomes. |
| `npcs.endSpawnDimensions` | int list | `[1]` | any | no | Dimension ids that use the End spawn table (Commando squads), in End biomes. |
| `npcs.zombies` / `military` / `bandits` / `miniBosses` / `mutants` | boolean | `true` | n/a | no | Per-faction spawn switch. `zombies` (with the Nether's Pigman Soldier), `military` (with the End's Commandos) and `bandits` gate natural spawning; `miniBosses` (Psycho Steve, and Dictator Dave at red bases) and `mutants` also refuse eggs, camps and spawners. |
| `npcs.alienBug` / `cyberdemon` / `outcast` / `helicopter` | boolean | `true` | n/a | no | Switches for the special mobs. `alienBug`, `cyberdemon` and `outcast` refuse every spawn of their mob (natural, egg, camp and spawner); `helicopter` gates the camp and Camp Flag helicopter. |
| `npcs.weightZombieFarmer` / `weightZombieMiner` | int | `100` | 0 to 10000 | no | Spawn weights (half of Techguns). |
| `npcs.weightZombieSoldier` / `weightSkeletonSoldier` | int | `50` | 0 to 10000 | no | Spawn weights. |
| `npcs.weightBandit` | int | `25` | 0 to 10000 | no | Spawn weight. |
| `npcs.weightPsychoSteve` | int | `1` | 0 to 10000 | no | Spawn weight. |
| `npcs.weightNetherPigmanSoldier` | int | `15` | 0 to 10000 | no | Nether spawn weight of the Zombie Pigman Soldier, anywhere in the Nether. |
| `npcs.weightNetherMutantBasic` / `Heavy` / `Elite` | int | `6` / `3` / `2` | 0 to 10000 | no | Nether spawn weights of the Super Mutants, from Nether danger 2, 3 and 4. |
| `npcs.weightNetherCyberdemon` | int | `1` | 0 to 10000 | no | Nether spawn weight of the Cyberdemon, from Nether danger 4, always alone. |
| `npcs.weightEndCommando` | int | `1` | 0 to 10000 | no | End spawn weight of a Commando squad of 2 to 3. |
| `npcs.endMinDistance` | int | `256` | 0 to 100000 | no | No natural NPC spawns closer than this to the End's center. |
| `npcs.bossSpacing` | int | `128` | 0 to 1024 | no | A natural Cyberdemon does not spawn while another is within this many blocks. |
| `npcs.distanceLevel0` / `1` / `2` | int | `500` / `1000` / `2500` | 0 to 100000 | no | Blocks from world spawn where each distance danger step begins. The Nether counts its distance times 8 and adds 1; the End counts from its center and adds 2. |
| `npcs.perChunkCap` | int | `4` | 0 to 256 | no | Most NPCs per chunk before natural spawning stops. |
| `npcs.perPlayerCap` | int | `8` | 0 to 1024 | no | Most NPCs around one player before natural spawning stops. |
| `npcs.gunDamageEnabled` | boolean | `true` | n/a | no | NPC bullets deal damage. |
| `npcs.infernalTieredLoot` | boolean | `true` | n/a | no | Elite, ultra and infernal GregGuns NPCs add tiered GregGuns drops (needs Infernal Mobs). |
| `npcs.infernalSpawnerOptOut` | boolean | `false` | n/a | no | NPCs from spawner blocks never roll as infernal mobs when true (farm guard). Off, they roll at Infernal Mobs' own rates like every other NPC. |

## Worldgen (`worldgen`)

Structure placement, gating and loot tuning (M8). Read only while `modules.worldgen` is on. Server-side only, not synced. Structure ids: `FactorySmall`, `TrainStation`, `GasStation`, `HouseMedium`, `BigBunker`, `AlienBugNest`, `MilitaryBase`, `AircraftCarrier`, `Submarine`.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `worldgen.spawnStructures` | boolean | `true` | n/a | no | When false the structure generator is not registered. |
| `worldgen.spacingSmall` / `spacingMedium` / `spacingBig` | int | `16` / `32` / `64` | 2 to 1024 | no | Grid cell size in chunks per size class. |
| `worldgen.structureDimensions` | int list | `[0]` | any | no | Dimension ids where structures are placed. |
| `worldgen.structureWorldTypeBlacklist` | string list | `flat` | any | no | World type names in which structures and silo complexes are never placed (case-insensitive). Every other type gets them: `default`, `largeBiomes`, `amplified`, `RWG`, `RWG2`, `RWG2_CONTINENTS` and any type a mod adds. |
| `worldgen.enabled<Id>` | boolean | `true` | n/a | no | Place that structure. |
| `worldgen.biomeBlacklist<Id>` | string list | none | any | no | BiomeDictionary type names in which that structure is never placed. |
| `worldgen.lootTierCeiling<Id>` | int | `1` (LV); `2` (MV) for `BigBunker`, `MilitaryBase`, `AircraftCarrier`, `Submarine` | 1 to 3 | no | Highest loot tier; picks the loot table of each chest kind (supply, weapon, reinforced), and with it the tier of the guns, ammo, parts and GT metals in it. |
| `worldgen.redBaseChanceMilitaryBase` | int | `20` | 0 to 100 | no | Percent of Military Bases that are red: red flag, pad and standards, and Dictator Dave at his post. The rest are blue. |
| `worldgen.lootTier<Id>` | int | `1` | 0 to 2 | no | Loot richness; adds picks to every chest and weight to the table's top-tier lines. Guns keep their weight, so a richer chest holds more ammo and gear, not more guns. |

## Loot (`loot`)

One cap on the tier of everything GregGuns puts in the world for a player to take. An item's tier is what a player needs to make it at all (its whole crafting chain: circuits by circuit tier, GT material items by their material, GT parts and machines by their own tier), never the voltage of the recipe that outputs it. Server-side only, not synced.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `loot.maxTier` | int | `4` (EV) | 1 to 12 | no | Highest GT tier (voltage index: 1 LV ... 4 EV, 5 IV ... 12 UMV) of anything in structure and silo complex chests, furniture, furnaces, GT machine outputs in complexes, item frames, armories and GregGuns NPC drops. Planet and Ross128 complex ceilings apply up to it. Quest loot bags and Vending trades follow their own quest tier. Items with no known tier never appear. |

## Damage factors (`damagefactors`)

Techguns' "Damage Factors" category. All keys are synced from server to client (GGS-01-03). The factor is chosen at the hit from who shoots and who is hit (`DamageFactors`, [damage-interop.md](gregguns-spec/02-guns/damage-interop.md)). Turret and NPC factors take effect with those milestones (M6, M7).

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `damagefactors.damagePvP` | float | `0.5` | 0 to 100 | yes | Damage multiplier when a player's shot hits another player. Zero while server PvP is off. |
| `damagefactors.damageTurretToPlayer` | float | `0.5` | 0 to 100 | yes | Damage multiplier when a turret's shot hits a player. |
| `damagefactors.damageFactorNPC` | float | `1.0` | 0 to 100 | yes | Damage multiplier for other NPCs hitting players, and for NPC against NPC. |
| `damagefactors.armorPenetrationMultiplierNPCtoPlayer` | float | `0.5` | 0 to 1 | yes | Scales the armor penetration of non-player attackers against players. At 0.5 an NPC's armor-piercing share is halved, so the armor you wear counts for more. |
| `damagefactors.armorPenetrationMultiplierPvP` | float | `1.0` | 0 to 1 | yes | Scales the armor penetration of a player's shots against other players. |

## Combat (`combat`)

Hit knockback, and how hard and how accurately hostile NPCs and turrets shoot at you (design [0027](design/0027-combat-difficulty-knobs.md)). All keys are synced from server to client. The NPC keys cover GregGuns NPCs, the hostile NPC turret and the Attack Helicopter; your own Rampart turret and your own guns are not affected.

An NPC shot's damage is its gun's NPC damage, times the difficulty factor (Easy 0.25, Normal 0.3, Hard 0.375), times the tier multiplier of the gun it fires, times `npcEliteDamage` when the shooter is an Infernal Mobs elite, ultra or infernal or a boss, times `damagefactors.damageFactorNPC`. A turret skips the difficulty factor and uses `damagefactors.damageTurretToPlayer` instead. Armor then takes its share, and `damagefactors.armorPenetrationMultiplierNPCtoPlayer` decides how much of the gun's penetration skips it.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `combat.knockback` | boolean | `false` | n/a | yes | Whether GregGuns gun and projectile hits push their target, both ways: your shots pushing mobs and NPC shots pushing you. Includes a rocket's direct hit, shrapnel, beams and the Power Hammer's shockwave. Explosion blasts push whatever this says. |
| `combat.knockbackMultiplier` | float | `1.0` | 0 to 10 | yes | While knockback is on, the push of a hit is vanilla's times this times the gun's own knockback (3 for the Rocket Launcher, 2 for the Sonic Rifle, 0 for the Chainsaw). |
| `combat.npcDamageSteam` | float | `1.0` | 0 to 10 | yes | Damage multiplier for NPC and turret shots from Steam-tier guns. |
| `combat.npcDamageLV` | float | `1.0` | 0 to 10 | yes | Damage multiplier for NPC and turret shots from LV guns. |
| `combat.npcDamageMV` | float | `1.0` | 0 to 10 | yes | Damage multiplier for NPC and turret shots from MV guns. The NPC turret's AK47 is MV. |
| `combat.npcDamageHV` | float | `0.85` | 0 to 10 | yes | Damage multiplier for NPC and turret shots from HV guns. The Attack Helicopter counts as HV. |
| `combat.npcDamageEV` | float | `0.7` | 0 to 10 | yes | Damage multiplier for NPC shots from EV guns. |
| `combat.npcDamageIV` | float | `0.55` | 0 to 10 | yes | Damage multiplier for NPC shots from IV guns. |
| `combat.npcDamageLuV` | float | `0.45` | 0 to 10 | yes | Damage multiplier for NPC shots from LuV guns. |
| `combat.npcDamageZPM` | float | `0.4` | 0 to 10 | yes | Damage multiplier for NPC shots from ZPM guns. |
| `combat.npcEliteDamage` | float | `0.75` | 0 to 10 | yes | Extra damage multiplier for shots from an elite, ultra or infernal NPC (Infernal Mobs) or a boss such as the Attack Helicopter. |
| `combat.npcAccuracyRange` | float | `5.0` | 0 to 256 | yes | Blocks within which NPC and turret shots keep their gun's full accuracy. |
| `combat.npcSpreadPerBlock` | float | `0.12` | 0 to 10 | yes | Degrees of aim error an NPC or turret gains per block beyond `npcAccuracyRange`. One error is drawn per shot, so a shotgun's pellets keep their pattern. 0 turns the distance penalty off. |
| `combat.npcMaxSpread` | float | `4.0` | 0 to 45 | yes | Largest aim error, in degrees, that distance adds. With the defaults an NPC 10 blocks away still hits a standing player with nearly every shot, and from about 20 blocks on it misses more often than it hits. |

### Make combat harder

The defaults are the gentle end. To make fights harder, raise these keys; each one stands alone, so pick the ones you want.

- **Plain gun damage at every tier:** set every `combat.npcDamage*` key to `1.0` and `combat.npcEliteDamage` to `1.0`.
- **Armor-piercing NPC guns:** set `damagefactors.armorPenetrationMultiplierNPCtoPlayer` to `1.0`.
- **Sharpshooting NPCs:** set `combat.npcSpreadPerBlock` to `0` (no distance penalty), or keep it and raise `combat.npcAccuracyRange`.
- **Getting shoved around:** set `combat.knockback` to `true`; raise `combat.knockbackMultiplier` for stronger pushes.
- **Plain gun stats:** the four changes above together let every NPC gun hit you with its full stats.
- **Harder still:** raise `damagefactors.damageFactorNPC` above `1.0` (2.0 doubles every NPC shot), or the `combat.npcDamage*` keys above `1.0`, and play on Hard difficulty.

## Explosions (`explosions`)

Both keys are synced from server to client (GGS-01-03). Block removal always passes through the protection hooks: the explosion events (ServerUtilities filters the affected-block list) and, for a player's blast, a `BlockEvent.BreakEvent` per block. See [explosions-pack.md](gregguns-spec/04-projectiles-fx/explosions-pack.md).

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `explosions.blockDamage` | boolean | `false` | n/a | yes | Whether GregGuns explosions remove blocks. Off: they still hurt entities. An NPC-caused blast also needs the `mobGriefing` gamerule. Also gates Bio Gun blobs and block fire from the Flamethrower, Nether Blaster and Alien Blaster: with it off the Bio Gun places no blob and its tooltip says "Blobs disabled: explosions.blockDamage is off". |
| `explosions.selfDamage` | boolean | `true` | n/a | yes | Whether the player who set off an explosion can be hurt by it (fixes PFX-25). |
| `explosions.machineExplosions` | boolean | `true` | n/a | yes | Whether GregTech machines in a GregGuns blast react to it (can chain). False leaves them standing (Q4). |
| `explosions.fallout` | boolean | `true` | n/a | yes | Whether a nuclear blast (the Nuke Launcher, the ICBM nuclear warhead) leaves fallout: irradiated surface blocks. Only blasts that broke terrain leave any. |
| `explosions.falloutMaxBlocks` | int | `6000` | 0 to 100000 | yes | The most blocks one nuclear blast may turn into irradiated earth. The conversion chance is scaled so the expected total stays under this, and the count is a hard stop. |
| `explosions.falloutMaxRadius` | int | `96` | 8 to 256 | yes | The largest fallout radius in blocks, whatever the blast radius. |
| `explosions.falloutColumnsPerTick` | int | `400` | 1 to 5000 | yes | The most columns fallout scans in one server tick, across all blasts. |
| `explosions.falloutBudgetMs` | int | `3` | 1 to 20 | yes | The most milliseconds fallout may spend in one server tick. |
| `explosions.falloutDecayMinutes` | int | `60` | 1 to 10080 | yes | Minutes of game time before irradiated earth reverts to the block it was. |
| `explosions.falloutRevertPerTick` | int | `32` | 1 to 1000 | yes | The most irradiated blocks that revert per tick in one world. |
| `explosions.falloutRadiationLevel` | int | `2` | 0 to 10 | yes | GregTech radioactivity level applied about once a second to an unprotected player standing on or beside irradiated earth. 0 turns exposure off. |
| `explosions.falloutMaxGoo` | int | `24` | 0 to 500 | yes | The most fallout goo blocks one blast makes (D21). Goo is rare and near ground zero; while it stands in a chunk, that chunk's irradiated earth does not decay. |

## ICBM blasts (`icbm`)

Every key is synced except the `icbm.siloComplex*` keys, which only the server reads. The synced ones govern every ICBM blast ([blast-engine.md](gregguns-icbm-spec/02-explosives/blast-engine.md)). A blast removes terrain according to its tier's `terrainDamageT<n>` key alone (D52); `explosions.blockDamage` governs hand-held weapons and is not consulted. Claims and spawn protection still apply through the explosion events and, for a player's blast, `BlockEvent.BreakEvent`.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `icbm.maxBlastTier` | int | `5` | 1 to 5 | yes | The highest explosive tier that may detonate. |
| `icbm.terrainDamageT1` to `icbm.terrainDamageT4` | boolean | `true` | n/a | yes | Whether blasts of that tier may remove terrain (on by default; `explosions.blockDamage` does not apply). Tier 4 covers antimatter (UV) and red matter (UHV). |
| `icbm.redMatterLifetimeSeconds` | int | `60` | 5 to 600 | yes | The longest a red matter body lives; it also ends sooner when it is idle for 10 s (D132). |
| `icbm.blastBudgetMs` | int | `12` | 1 to 40 | yes | Milliseconds per server tick that all running blasts share for breaking blocks. |
| `icbm.maxActiveBlasts` | int | `16` | 1 to 256 | yes | Most blasts running at once; more are refused. |
| `icbm.maxBlastRadius` | int | `96` | 8 to 200 | yes | Largest blast radius; the ray cast copies a cube this wide. |
| `icbm.maxGridMemoryMb` | int | `256` | 16 to 4096 | yes | Caps the memory of all running blasts' grids together; a blast that would go past it waits in the queue until another finishes its cast. A lone blast always runs. |
| `icbm.fuseMultiplier` | double | `1.0` | 0.1 to 10 | yes | Multiplier on a redstone- or hand-lit warhead's fuse. The base is `20 * (3 + ceil(radius / 3))` ticks from the warhead's blast radius at its fill (D51); a warhead set off by another explosion keeps a fixed 10 tick fuse. |
| `icbm.blastDrops` | boolean | `false` | n/a | yes | Whether destroyed blocks drop items. |
| `icbm.siloComplexEnabled` | boolean | `true` | n/a | no | Whether silo complexes (D128) generate. Also needs `modules.worldgen` and `worldgen.spawnStructures`. |
| `icbm.siloComplexDimensions` | string list | Overworld and every D38 planet except T Ceti E, an ocean world (D239) | n/a | no | Dimensions that get complexes, by the dimension's key (case-insensitive): "Overworld", or a Galacticraft planet's own unlocalized name such as `moon`, `iojupiter`, `ganymed`, `barnarda2`, `vega1`, `ross128b`. Remove a name to keep complexes out of that planet; a name the D38 table lacks gets no complexes, and the log says so once. A complex that already generated keeps generating after its dimension is removed. |
| `icbm.siloComplexChancePercent` | int | `25` | 0 to 100 | no | Share of the sparse grid's cells (one per 64 x 64 chunks) that attempt a complex. Complexes already generated stay whatever the value; only new decisions follow it. |
| `icbm.siloComplexChestTargets` | int list | `100, 200, 300` | three whole numbers, 0 or more | no | How many loot chests a whole complex holds: abandoned, partly abandoned, operational. The same on every planet. Every chest holds loot; a chest spot left over is an empty wooden crate. Armory display chests come on top. Other lists keep the default. |
| `icbm.siloComplexRoss128Weights` | int list | `30, 22, 16, 11, 8, 6, 4, 2, 1` | nine values | no | Weights, EV first to UMV last, of the loot ceiling a complex on Ross128b or Ross128ba rolls. |
| `icbm.siloComplexOverworldBiomes` | string list | dry and temperate land types | n/a | no | Overworld biome types (BiomeDictionary names) that may host a complex; blank allows all. |
| `icbm.siloComplexActivityWeights` | int list | `30, 40, 30` | three weights, 0 or more | no | Relative weights of the activity levels (abandoned, partly abandoned, operational) a complex rolls from its seed. Other lists keep the default. Each complex rolls once, when it is first placed, and keeps that level. |
| `icbm.maxMissilesInFlight` | int | `20` | 1 to 100 | yes | Most missiles in flight at once, server wide (D11). Each holds at most two chunks. |
| `icbm.missileFuelFactor` | double | `1.0` | 0.1 to 10 | yes | Multiplier on every missile tier's fuel tank (D20, D60). |
| `icbm.missileRangeFactor` | double | `1.0` | 0.1 to 10 | yes | Multiplier on every missile tier's range (D60). |
| `icbm.siloScatterFactor` | double | `0.5` | 0.1 to 2 | yes | Multiplier on the scatter of a missile launched from a Missile Silo, on top of the guidance rule; the 10% floor still applies (D70). |
| `icbm.siloRangeFactor` | double | `1.25` | 0.5 to 5 | yes | Multiplier on the range of a missile launched from a Missile Silo, on top of `missileRangeFactor`; the open pad keeps the plain range (D70). |
| `icbm.fuelBonusMax` | double | `0.25` | 0 to 1 | yes | The most unburned fuel adds to a blast radius, as a fraction: 0.25 is up to 25% at point-blank range, nothing at full range. 0 turns it off. |
| `icbm.missileTimeoutTicks` | int | `600` | 20 to 6000 | yes | Ticks a missile may fly past its planned arrival before it detonates where it is. |
| `icbm.doorOpenTicks` | int | `40` | 10 to 200 | yes | Ticks the Missile Silo doors take to swing open or shut (D89). |
| `icbm.countdownTicks` | int | `200` | 20 to 1200 | yes | Ticks of countdown between the doors being open (or the launch, on a pad) and ignition; the launch can be aborted until it ends (D88). |
| `icbm.doorCloseHeight` | int | `4` | 1 to 32 | yes | Blocks the missile's bottom must climb above the silo rim before the doors swing shut (D89). |
| `icbm.empMaxDevices` | int | `64` | 1 to 1024 | yes | Most devices one EMP pulse may drain, nearest first (D16). A device is a GT machine that stores EU, a battery buffer, a multiblock (a Lapotronic Supercapacitor included), or an IC2 energy store; a buffer's batteries and a supercapacitor's stored EU are drained with it (D102 amendment). Missiles are not counted. |
| `icbm.empMaxRadius` | int | `100` | 8 to 256 | yes | Largest radius an EMP Tower may be set to, blocks. A pulse costs 25 EU per square block of radius. |
| `icbm.empMissileKillChance` | double | `0.5` | 0 to 1 | yes | Chance an EMP pulse destroys a missile in flight outright. |
| `icbm.empMissileGuidanceChance` | double | `0.8` | 0 to 1 | yes | Chance, among the survivors, that a pulse fries a missile's guidance so it falls where its drift carries it. |
| `icbm.empMissileTriggerChance` | double | `0.1` | 0 to 1 | yes | Chance, among the rest, that a pulse sets a missile's warhead off where it is. |
| `icbm.enderMaxTeleports` | int | `64` | 1 to 1024 | yes | Most entities one ender warhead teleports (D80); the rest are only pulled. |
| `icbm.antigravMaxBlocks` | int | `512` | 0 to 4096 | yes | Most blocks one anti-gravitational warhead lifts into flying blocks (D80), top layer first. |

**Finding structures.** Ops (permission level 2, and the console) can list them with `/gregguns locate <kind> [radius_cells] [generate]`, where the kind is `complex` (silo complexes), `military_base`, `big_bunker`, `factory_small`, `train_station`, `gas_station`, `house_medium`, `alien_bug_nest`, `aircraft_carrier`, `submarine` or `any`. It prints the sites within the radius (1 to 16 cells of 1,024 blocks, default 3) that the seed and the spacing, pool and chance settings select, nearest first, each as built, skipped (with the reason) or not generated yet. Add `generate` to make the nearest five undecided sites decide now, one chunk per tick, with a progress line per site. Click a built site's line to land on the surface at it. It works in the dimension you stand in, and only where that dimension generates the kind.

## Equipment (`equipment`)

All keys are synced. Used by the special-slot equipment of the armor module ([baubles-slots.md](gregguns-spec/05-armor/baubles-slots.md), [oxygen-gear.md](gregguns-spec/05-armor/oxygen-gear.md)).

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `equipment.nightVisionCapacityEu` | int | `100000` | 1000 to 100000000 | yes | EU the Night Vision Goggles hold (MV electric item). |
| `equipment.nightVisionDrainEuPerTick` | int | `2` | 0 to 1000 | yes | EU per tick the goggles and the Tactical Mask draw while night vision is on (GT's Mech Armor draws 2). |
| `equipment.tacticalMaskCapacityEu` | int | `200000` | 1000 to 100000000 | yes | EU the Tactical Mask holds (HV electric item). |
| `equipment.gasMaskFilterTicks` | int | `300` | 2 to 100000 | yes | Poison or wither cure ticks one Gas Mask filter holds. |
| `equipment.oxygenTankCapacity` | int | `2000` | 100 to 100000 | yes | Air units the Oxygen Tanks hold (their durability). Galacticraft's tier 2 tank is 2000. |

## Flight (`flight`)

All keys are synced. Used by the back devices of the armor module ([powered-flight.md](gregguns-spec/05-armor/powered-flight.md)). The Glider uses no energy and has no keys.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `flight.jumpPackCapacityEu` | int | `1000000` | 1000 to 100000000 | yes | EU the Jump Pack holds (HV electric item). |
| `flight.jumpPackJumpCostEu` | int | `2000` | 0 to 1000000 | yes | EU one boost jump costs. |
| `flight.jumpPackFallCostEu` | int | `1000` | 0 to 1000000 | yes | EU the pack spends to absorb one fall longer than 3 blocks. |
| `flight.antiGravityCapacityEu` | int | `2000000` | 1000 to 1000000000 | yes | EU the Anti Gravity Device holds (IV electric item). |
| `flight.antiGravityDriveEuPerTick` | int | `100` | 0 to 100000 | yes | EU per tick the device draws while it is flying; about 17 minutes of flight on a full charge. |

## Energy (`energy`)

Used by the energy guns ([energy-electric-items.md](gregguns-spec/02-guns/energy-electric-items.md), GGS-02-14). A gun's capacity is the GT battery of its tier, scaled, unless a per-gun line overrides it.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `energy.capacityScalePercent` | int | `100` | 1 to 10000 | yes | Scales every energy gun's default capacity, in percent. |
| `energy.capacityOverrides` | string list | empty | n/a | no | Lines `gunId=eu` (for example `laser_rifle=8000000`) that set one gun's capacity and win over the scale. Bad lines are ignored. Set the same lines on server and clients so tooltips match. |

## Client (`client`)

Display options for the local player only. Not synced.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `client.showAmmoHud` | boolean | `true` | n/a | no | Draws the ammo HUD (rounds in the gun, magazines carried, reload progress) while a gun is held. |
| `client.hudOffsetX` | int | `6` | 0 to 2000 | no | Pixels from the right screen edge to the right edge of the ammo HUD text. |
| `client.hudOffsetY` | int | `40` | 0 to 2000 | no | Pixels from the bottom screen edge to the bottom of the ammo HUD. The default clears the InGame-Info-XML potion lines in the same corner; M1 still owes a screenshot check with several potions active. |
| `client.ammoPressAnimation` | boolean | `true` | n/a | no | Draws the Ammo Press's front animation, with the frame chosen from recipe progress. Off shows the still front. Cosmetic only. |
| `client.particleCeiling` | int | `2000` | 100 to 8000 | no | Most GregGuns effect particles alive at once. Bursts shrink as the count nears it and never pass it, leaving room under the pack's 8000-particle cap. |

Aim down sights (see [gregguns-spec/03-gun-visuals-audio/ads.md](gregguns-spec/03-gun-visuals-audio/ads.md)):

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `client.adsEnabled` | boolean | `true` | n/a | no | Right-click on an iron-sight or red-dot gun moves the gun to the screen center. Off: no move, and the right-click is not sent for those guns. |
| `client.adsFov` | float | `0.9` | 0.3 to 1.0 | no | Field of view multiplier while aiming a gun with no scope zoom of its own. 1.0 leaves the view alone. |
| `client.adsTransitionTicks` | int | `5` | 1 to 20 | no | Client ticks the gun takes to move in and out. |
| `client.adsHideCrosshair` | boolean | `true` | n/a | no | Hides the crosshair while the sights are lined up. |
| `client.adsStopsSprint` | boolean | `true` | n/a | no | Stops sprinting while aiming; the sprint key is ignored until you stop. |

## Aim (`aim`)

Server side of aim down sights. Read on the server; none is synced.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `aim.adsEnabled` | boolean | `true` | n/a | no | Lets sights-only guns aim on a right-click. Off: the server ignores the request and drops a running aim. Scope zoom is unaffected. |
| `aim.adsSpreadMultiplier` | float | `0.75` | 0.0 to 1.0 | no | Multiplier on spread while aiming a gun with no scope zoom of its own (0.75 is 25 percent tighter). A scoped gun keeps its own zoom multiplier. |

## Guns (`guns`)

Server side of gun handling. Read on the server; none is synced, because the server starts every reload and the client animates its reload message.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `guns.idleReloadDelayTicks` | int | `40` | 0 to 1200 | no | Ticks a held chamber gun (Revolvers, Golden Revolvers, Combat Shotgun, Sawed Off) goes without a shot, a trigger pull or a reload before it loads its empty chambers by itself, when the player carries its ammo. A trigger pull stops it. `0` turns the idle reload off. Design record [0026](design/0026-single-round-and-chamber-reloads.md). |

## Questbook (`questbook`)

Server only. The key is in the in-game config GUI and in `config/gregguns.cfg`. (The `debug` category is hidden from the GUI and edited in the file.)

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `questbook.install` | boolean | `true` | n/a | no | On server start, installs the GregGuns quest line from this jar into BetterQuesting's database (design record 0015). Needs BetterQuesting; with it absent the key is ignored (one INFO line). Needs a server restart. |

**Questbook install.** With `questbook.install` on (the default), the server installs the line on each start. After BetterQuesting has loaded its database, GregGuns reads the quest line from `assets/gregguns/questbook/` in its own jar and upserts it by fixed quest ID, marks the database for BetterQuesting's next world save, syncs connected players and logs `Installed/updated GregGuns quest line: N quests`.

- **Safe to leave on.** It only touches the GregGuns quests and the GregGuns line. Every other quest and line stays as it is. Player progress is kept: completion and claims live per quest ID, and task progress is restored after the definition is replaced. The install rewrites GregGuns quest text and positions on each start.
- **Tab order.** The GregGuns tab is added after the existing tabs the first time. If an admin later moves it, the move is kept. **In the GTNH pack this does not hold:** while the pack's `config/modpack-update` marker is present, DreamCoreMod runs `/bq_admin default load` on every start, which rebuilds the database from the pack's DefaultQuests. The install then adds the GregGuns line again (the log says "121 added") and the tab goes back to last. Player progress is still kept.
- **Off is not removal.** Setting the key to `false` does nothing at all: an installed line stays in the world's database. To remove it, open the questbook in edit mode (`/bq_admin edit`) and delete the GregGuns line and its quests, or delete the quests by ID with the `/bq_admin` commands. Back up `betterquesting/` in the world folder first; deleting a quest also deletes its progress. While the key stays `true`, a removed quest comes back on the next start, so turn the key off first.
- **Retired quests.** The install deletes exactly the quest IDs that `index.json` retires, together with their progress, and touches no other quest.

## Rules for new options

- Declare it in a `@Config` class listed in `GregGunsConfig.CLASSES`; never read a `Configuration` by hand (GGS-01-01).
- Every field has a `@Config.Comment` that includes `Default: ...`, and every numeric field has a `@Config.Range*` (GGS-01-04). Unit tests enforce both.
- Fields the client reads, or that gate server behavior at runtime, are `@Config.Sync` (GGS-01-03). Add them to `EXPECTED_SYNCED` in `ConfigRulesTest` and mark "Synced: yes" here.
- Do not add keys for ore generation, ingots, nuggets, ore drills, oil holes, DataWatcher IDs, GUI tab IDs or a version checker (GGS-01-02). A test rejects those names.
- Config classes hold fields only and import no Minecraft types (GGS-01-06).
- Document the option in this file.
