# GregGuns configuration

Reference for every option in `config/gregguns.cfg`. GTNHLib creates the file on first run, with a comment above each key, and the in-game config GUI lists the same options. The classes behind it are in `com.gregguns.core.config`; the design is in [gregguns-spec/01-core/config-gtnhlib.md](gregguns-spec/01-core/config-gtnhlib.md).

Keep this file in step with the code: a PR that adds or changes an option updates its row here.

## Module flags (`modules`)

Each roadmap content area is a module. **A flag never changes what GregGuns registers.** Every block, item, entity, tile entity, machine and network packet exists whatever the flags say, so a client and a server with different flags can join each other, and turning a module off never makes Forge treat its items as missing or delete them from a world. A flag only switches behavior. When a module is `false`:

- it registers no recipes and no event handlers (so no crafting, no natural spawning, no structure generation);
- its machines do nothing, its guns do not fire and its other items and blocks cannot be used (a right click or swing is cancelled with a chat line);
- its items and blocks are hidden from NEI and the GregGuns creative tab;
- items that already exist stay intact, with a tooltip line "Disabled by the server's GregGuns config (modules.X)".

**A flag defaults to `true` once its milestone's exit gate is met.** The gates for M2 to M8 are met, so every module flag defaults to `true`, `worldgen` (M8) included. The exception is `icbm`, which defaults to `false` until the ICBM beta gate ([ROADMAP-ICBM](ROADMAP-ICBM.md)). The PR that completes a milestone flips that milestone's default, in `ModulesConfig` and in the table below.

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
| `modules.icbm` | boolean | `false` | n/a | login packet | `guns`, `machines`, and the Galacticraft mod | ICBM I0 | ICBM missiles and warheads. Off by default until the ICBM beta gate. With the flag on and Galacticraft not installed, the module turns itself off and logs one warning (D9); the flag then reads as off, also in the login packet. |

All module flags need a game restart. **The server's flags win on a multiplayer client.** At login the server sends which modules are active on it (`PacketModuleSync`, on the `gregguns` channel); the client uses those for behavior and visibility (tooltips, NEI, the creative tab, accuracy and gating) until it disconnects, and its own flags in single player. One exception: recipes are registered at startup from each side's own config, so a client whose module is off shows no recipes for it in its crafting grid and NEI.

### Server-authoritative keys

The module flags (login packet) and every key marked `Synced: yes` in the tables below (GTNHLib `@Config.Sync`: the server value overrides the client's while connected and is restored on disconnect) are server-authoritative. Everything else is either server-side only (NPC, worldgen and utility tuning, which the client never reads) or a client-only preference (`client.*`: HUD, animations, sounds, key binds), and stays local.

The "Needs" column is the module order in `ModuleId`, taken from the roadmap's entry gates.

## Debug (`debug`)

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `debug.enableDebugKeybinds` | boolean | `false` | n/a | no | Registers the numpad debug keybinds. Nothing reads it until the M1 keybinds exist. Kept from Techguns' `enableDebugKeybinds`. |
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
| `utility.bioBlobSparesPlayers` | boolean | `false` | n/a | no | Bio Blob poison skips players. |
| `utility.bioBlobDecayTicks` | int | `60` | 1 to 24000 | no | Ticks per Bio Blob decay step. |
| `utility.campFlagSpawnIntervalTicks` | int | `200` | 20 to 24000 | no | Ticks between Camp Flag spawn attempts. |
| `utility.campFlagCapFactor` | float | `0.75` | 0 to 10 | no | NPC cap is int(((sizeX + sizeZ) / 2) * factor). |
| `utility.campFlagBreakHelicopter` | boolean | `true` | n/a | no | Breaking a Camp Flag sends a helicopter (inert until NPCs exist). |

## NPCs (`npcs`)

NPC spawn gate, weights, danger distances, caps and gun damage (M7). Server-side only, not synced. A weight of 0 means the mob is never selected (it is still registered, like every other mob).

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `npcs.spawnDimensions` | int list | `[0]` | any | no | Dimension ids where NPCs spawn naturally. |
| `npcs.zombies` / `military` / `bandits` / `miniBosses` / `mutants` | boolean | `true` | n/a | no | Per-faction spawn switch. |
| `npcs.alienBug` / `cyberdemon` / `helicopter` | boolean | `false` | n/a | no | Switches for the special mobs. |
| `npcs.weightZombieFarmer` / `weightZombieMiner` | int | `100` | 0 to 10000 | no | Spawn weights (half of Techguns). |
| `npcs.weightZombieSoldier` / `weightSkeletonSoldier` | int | `50` | 0 to 10000 | no | Spawn weights. |
| `npcs.weightBandit` | int | `25` | 0 to 10000 | no | Spawn weight. |
| `npcs.weightPsychoSteve` | int | `1` | 0 to 10000 | no | Spawn weight. |
| `npcs.distanceLevel0` / `1` / `2` | int | `500` / `1000` / `2500` | 0 to 100000 | no | Blocks from world spawn where each distance danger step begins. |
| `npcs.perChunkCap` | int | `4` | 0 to 256 | no | Most NPCs per chunk before natural spawning stops. |
| `npcs.perPlayerCap` | int | `8` | 0 to 1024 | no | Most NPCs around one player before natural spawning stops. |
| `npcs.gunDamageEnabled` | boolean | `true` | n/a | no | NPC bullets deal damage. |
| `npcs.infernalTieredLoot` | boolean | `true` | n/a | no | Elite, ultra and infernal GregGuns NPCs add tiered GregGuns drops (needs Infernal Mobs). |
| `npcs.infernalAllowSpawnerNpcs` | boolean | `false` | n/a | no | NPCs from spawner blocks may roll as infernal mobs. |

## Worldgen (`worldgen`)

Structure placement, gating and loot tuning (M8). Read only while `modules.worldgen` is on. Server-side only, not synced. Structure ids: `FactorySmall`, `TrainStation`, `GasStation`, `HouseMedium`, `BigBunker`, `AlienBugNest`, `MilitaryBase`, `AircraftCarrier`, `Submarine`.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `worldgen.spawnStructures` | boolean | `true` | n/a | no | When false the structure generator is not registered. |
| `worldgen.spacingSmall` / `spacingMedium` / `spacingBig` | int | `16` / `32` / `64` | 2 to 1024 | no | Grid cell size in chunks per size class. |
| `worldgen.structureDimensions` | int list | `[0]` | any | no | Dimension ids where structures are placed. |
| `worldgen.structureWorldTypes` | string list | `default`, `RWG`, `largeBiomes`, `amplified` | any | no | World type names that allow structures (case-insensitive). Flat and unknown types are out. |
| `worldgen.enabled<Id>` | boolean | `true` (`false` for `AlienBugNest`) | n/a | no | Place that structure. |
| `worldgen.biomeBlacklist<Id>` | string list | none | any | no | BiomeDictionary type names in which that structure is never placed. |
| `worldgen.lootTierCeiling<Id>` | int | `1` (LV); `2` (MV) for `BigBunker`, `MilitaryBase`, `AircraftCarrier`, `Submarine` | 1 to 3 | no | Highest loot tier; picks the loot category. |
| `worldgen.lootTier<Id>` | int | `1` | 0 to 2 | no | Loot richness; scales counts and weights only. |

## Damage factors (`damagefactors`)

Techguns' "Damage Factors" category. All keys are synced from server to client (GGS-01-03). The factor is chosen at the hit from who shoots and who is hit (`DamageFactors`, [damage-interop.md](gregguns-spec/02-guns/damage-interop.md)). Turret and NPC factors take effect with those milestones (M6, M7).

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `damagefactors.damagePvP` | float | `0.5` | 0 to 100 | yes | Damage multiplier when a player's shot hits another player. Zero while server PvP is off. |
| `damagefactors.damageTurretToPlayer` | float | `0.5` | 0 to 100 | yes | Damage multiplier when a turret's shot hits a player. |
| `damagefactors.damageFactorNPC` | float | `1.0` | 0 to 100 | yes | Damage multiplier for other NPCs hitting players, and for NPC against NPC. |
| `damagefactors.armorPenetrationMultiplierNPCtoPlayer` | float | `1.0` | 0 to 1 | yes | Scales the armor penetration of non-player attackers against players. |
| `damagefactors.armorPenetrationMultiplierPvP` | float | `1.0` | 0 to 1 | yes | Scales the armor penetration of a player's shots against other players. |

## Explosions (`explosions`)

Both keys are synced from server to client (GGS-01-03). Block removal always passes through the protection hooks: the explosion events (ServerUtilities filters the affected-block list) and, for a player's blast, a `BlockEvent.BreakEvent` per block. See [explosions-pack.md](gregguns-spec/04-projectiles-fx/explosions-pack.md).

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `explosions.blockDamage` | boolean | `false` | n/a | yes | Whether GregGuns explosions remove blocks. Off: they still hurt entities. An NPC-caused blast also needs the `mobGriefing` gamerule. Also gates Bio Gun blobs and block fire from the Flamethrower, Nether Blaster and Alien Blaster: with it off the Bio Gun places no blob and its tooltip says "Blobs disabled: explosions.blockDamage is off". |
| `explosions.selfDamage` | boolean | `true` | n/a | yes | Whether the player who set off an explosion can be hurt by it (fixes PFX-25). |
| `explosions.machineExplosions` | boolean | `true` | n/a | yes | Whether GregTech machines in a GregGuns blast react to it (can chain). False leaves them standing (Q4). |

## ICBM blasts (`icbm`)

All keys are synced. They govern every ICBM blast ([blast-engine.md](gregguns-icbm-spec/02-explosives/blast-engine.md)). A blast removes terrain according to its tier's `terrainDamageT<n>` key alone (D52); `explosions.blockDamage` governs hand-held weapons and is not consulted. Claims and spawn protection still apply through the explosion events and, for a player's blast, `BlockEvent.BreakEvent`.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `icbm.maxBlastTier` | int | `4` | 1 to 4 | yes | The highest explosive tier that may detonate. |
| `icbm.terrainDamageT1` to `icbm.terrainDamageT4` | boolean | `true` | n/a | yes | Whether blasts of that tier may remove terrain (on by default; `explosions.blockDamage` does not apply). |
| `icbm.blastBudgetMs` | int | `12` | 1 to 40 | yes | Milliseconds per server tick that all running blasts share for breaking blocks. |
| `icbm.maxActiveBlasts` | int | `16` | 1 to 256 | yes | Most blasts running at once; more are refused. |
| `icbm.maxBlastRadius` | int | `96` | 8 to 200 | yes | Largest blast radius; the ray cast copies a cube this wide. |
| `icbm.blastDrops` | boolean | `false` | n/a | yes | Whether destroyed blocks drop items. |

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

## Hidden keys

These keys are in `config/gregguns.cfg` but not in the in-game config GUI (their class carries GTNHLib's `@Config.ExcludeFromAutoGui`). They are for server owners and are edited in the file.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `questbook.install` | boolean | `false` | n/a | no | On server start, installs the GregGuns quest line from this jar into BetterQuesting's database (design record 0015). Needs BetterQuesting; with it absent the key is ignored (one INFO line). Needs a server restart. |

**Questbook install.** Set `questbook.install=true` and restart the server. After BetterQuesting has loaded its database, GregGuns reads the quest line from `assets/gregguns/questbook/` in its own jar and upserts it by fixed quest ID, marks the database for BetterQuesting's next world save, syncs connected players and logs `Installed/updated GregGuns quest line: N quests`.

- **Safe to leave on.** It only touches the GregGuns quests and the GregGuns line. Every other quest and line stays as it is. Player progress is kept: completion and claims live per quest ID, and task progress is restored after the definition is replaced. A GregGuns update that changes a quest text or position updates it on the next start.
- **Tab order.** The GregGuns tab is added after the existing tabs the first time. If an admin later moves it, the move is kept. **In the GTNH pack this does not hold:** while the pack's `config/modpack-update` marker is present, DreamCoreMod runs `/bq_admin default load` on every start, which rebuilds the database from the pack's DefaultQuests. The install then adds the GregGuns line again (the log says "121 added") and the tab goes back to last. Player progress is still kept.
- **Off is not removal.** Setting the key back to `false` does nothing at all: an installed line stays in the world's database. To remove it, open the questbook in edit mode (`/bq_admin edit`) and delete the GregGuns line and its quests, or delete the quests by ID with the `/bq_admin` commands. Back up `betterquesting/` in the world folder first; deleting a quest also deletes its progress. While the key stays `true`, a removed quest comes back on the next start, so turn the key off first.
- **Dropped quests.** A quest an older GregGuns shipped and a newer one dropped is not removed by the install; delete it by hand if you care.

## Rules for new options

- Declare it in a `@Config` class listed in `GregGunsConfig.CLASSES`; never read a `Configuration` by hand (GGS-01-01).
- Every field has a `@Config.Comment` that includes `Default: ...`, and every numeric field has a `@Config.Range*` (GGS-01-04). Unit tests enforce both.
- Fields the client reads, or that gate server behavior at runtime, are `@Config.Sync` (GGS-01-03). Add them to `EXPECTED_SYNCED` in `ConfigRulesTest` and mark "Synced: yes" here.
- Do not add keys for ore generation, ingots, nuggets, ore drills, oil holes, DataWatcher IDs, GUI tab IDs or a version checker (GGS-01-02). A test rejects those names.
- Config classes hold fields only and import no Minecraft types (GGS-01-06).
- Document the option in this file.
