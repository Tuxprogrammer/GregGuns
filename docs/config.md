# GregGuns configuration

Reference for every option in `config/gregguns.cfg`. GTNHLib creates the file on first run, with a comment above each key, and the in-game config GUI lists the same options. The classes behind it are in `com.gregguns.core.config`; the design is in [gregguns-spec/01-core/config-gtnhlib.md](gregguns-spec/01-core/config-gtnhlib.md).

Keep this file in step with the code: a PR that adds or changes an option updates its row here.

## Module flags (`modules`)

Each roadmap content area is a module. A module's flag decides whether its code runs at all: when it is `false`, the module registers no items, blocks, entities, recipes or event handlers, so nothing is left behind to crash or to orphan a recipe.

**All flags default to `false` for now.** The roadmap says a flag defaults to on once its milestone's exit gate is met, and no content milestone is met yet. The PR that completes a milestone flips that milestone's default, in `ModulesConfig` and in the table below.

A module also needs the modules listed in "Needs". If a flag is on but a needed module is off, the module is skipped and a warning is logged.

| Key | Type | Default | Range | Synced | Needs | Milestone | What it enables |
|---|---|---|---|---|---|---|---|
| `modules.guns` | boolean | `false` | n/a | no | none | M1, M2 | Damage system, guns, projectiles, ammo |
| `modules.armor` | boolean | `false` | n/a | no | `guns` | M3 | Armor, set bonuses, special slots |
| `modules.machines` | boolean | `false` | n/a | no | `guns` | M4 | Production chain and GregTech machines |
| `modules.energyWeapons` | boolean | `false` | n/a | no | `guns`, `machines` | M5 | Energy and exotic weapons |
| `modules.utility` | boolean | `false` | n/a | no | `machines` | M6 | Utility and defense machines |
| `modules.npcs` | boolean | `false` | n/a | no | `guns` | M7 | NPCs and spawning |
| `modules.worldgen` | boolean | `false` | n/a | no | `npcs` | M8 | Structures and worldgen loot |

All module flags need a game restart. They are not synced from server to client (GGS-01-03): content is registered once at startup, so a value that arrived after joining could not add or remove it. Start the client and the server with the same flags.

The "Needs" column is the module order in `ModuleId`, taken from the roadmap's entry gates.

## Debug (`debug`)

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `debug.enableDebugKeybinds` | boolean | `false` | n/a | no | Registers the numpad debug keybinds. Nothing reads it until the M1 keybinds exist. Kept from Techguns' `enableDebugKeybinds`. |
| `debug.recipeDump` | boolean | `false` | n/a | no | Enables the op-only `/gregguns dumprecipes <file>` command (recipe-clash review). Leave off in play. |

## Machines (`machines`)

Reaction Chamber and Retort tuning (GGS-08-25). Server-side only, not synced.

| Key | Type | Default | Range | Synced | What it does |
|---|---|---|---|---|---|
| `machines.retortPollutionPerSecond` | int | `100` | 0 to 100000 | no | Pollution the Retort adds per second while running. The single-block chamber pollutes nothing. |
| `machines.reactionFailurePollution` | int | `500` | 0 to 100000 | no | One-off pollution burst when a reaction fails. |
| `machines.reactionRadiationRadius` | int | `8` | 1 to 64 | no | Blocks around a failed reaction in which players are irradiated. |
| `machines.radiationLevelLow` / `Medium` / `High` | int | `1` / `3` / `5` | 0 to 10 | no | GT `applyRadioactivity` level for `RAD_LOW` / `RAD_MEDIUM` / `RAD_HIGH`. |
| `machines.explosionLowVoltageIndex` | int | `1` | 0 to 7 | no | GT voltage index whose blast power an `EXPLOSION_LOW` failure uses. |
| `machines.explosionMediumVoltageIndex` | int | `2` | 0 to 7 | no | Same for `EXPLOSION_MEDIUM`. |

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

NPC spawn gate, weights, danger distances, caps and gun damage (M7). Server-side only, not synced. A weight of 0 means the mob is never registered or selected.

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
| `explosions.blockDamage` | boolean | `false` | n/a | yes | Whether GregGuns explosions remove blocks. Off: they still hurt entities. An NPC-caused blast also needs the `mobGriefing` gamerule. |
| `explosions.selfDamage` | boolean | `true` | n/a | yes | Whether the player who set off an explosion can be hurt by it (fixes PFX-25). |
| `explosions.machineExplosions` | boolean | `true` | n/a | yes | Whether GregTech machines in a GregGuns blast react to it (can chain). False leaves them standing (Q4). |

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

## Rules for new options

- Declare it in a `@Config` class listed in `GregGunsConfig.CLASSES`; never read a `Configuration` by hand (GGS-01-01).
- Every field has a `@Config.Comment` that includes `Default: ...`, and every numeric field has a `@Config.Range*` (GGS-01-04). Unit tests enforce both.
- Fields the client reads, or that gate server behavior at runtime, are `@Config.Sync` (GGS-01-03). Add them to `EXPECTED_SYNCED` in `ConfigRulesTest` and mark "Synced: yes" here.
- Do not add keys for ore generation, ingots, nuggets, ore drills, oil holes, DataWatcher IDs, GUI tab IDs or a version checker (GGS-01-02). A test rejects those names.
- Config classes hold fields only and import no Minecraft types (GGS-01-06).
- Document the option in this file.
