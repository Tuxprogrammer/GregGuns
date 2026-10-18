# Ammunition, magazines and power sources

Gun statistics (damage, fire rate and so on) and the reload state machine belong to **02-guns**. This file covers the ammunition *items*: which gun takes which, how many are consumed, what comes back, and how each is produced and refilled.

## How guns consume ammo (summary needed to read the tables)

Each gun is built with an ammo `ItemStack` and a `clipsize` (the gun's item max damage). A reload:
1. Consumes one ammo stack of the gun's ammo-stack size from the player. Ammo is searched in the Techguns ammo inventory first, then the main inventory (details in 02/05).
2. If the gun has an **empty-mag item**, gives that many empties back (×2 for akimbo guns) through `addAmmoToPlayerInventory`: ammo slots first, then main inventory, then a dropped `EntityItem` for the remainder.
3. Fills the gun to `clipsize`. A gun with `ammoCount > 1` (shotguns, Grim Reaper, grenade launcher) instead loads up to `ammoCount` individual items in one reload, each worth `clipsize / ammoCount` shots.

`setAkimbo(..)` doubles the ammo stack size, so akimbo guns consume **two** magazines/cells/bullets per reload and return two empties.

The third argument of `setEmptyMag(empty, bullet, n)` records the bullet item and the bullets-per-magazine count. It sets `shotsPerBullet = clipsize / n`, which 02 uses (for example for NPC/turret ammo accounting). It matches the bullet count in the crafting refill recipe and in the sneak-use emptying behaviour below.

## Gun → ammo table

| Gun (registry name, see 02) | Ammo item (meta) | Consumed per reload | Clip | Empty returned | Bullet / count per mag |
|---|---|---|---|---|---|
| handcannon | Stone Bullet (0) | 1 | 1 | – | – |
| pistol | 9mm Pistol Magazine (2) | 1 | 18 | 9mm Pistol Magazine (Empty) (3) ×1 | 9mm Bullets ×3 |
| pistolAkimbo | 9mm Pistol Magazine (2) | 2 | 36 | (3) ×2 | 9mm Bullets ×6 |
| revolver | 9mm Bullets (1) | 1 | 6 | – | – |
| revolverAkimbo | 9mm Bullets (1) | 2 | 12 | – | – |
| goldenrevolver | 9mm Bullets (1) | 1 | 6 | – | – |
| goldenrevolverAkimbo | 9mm Bullets (1) | 2 | 12 | – | – |
| thompson | SMG Magazine (5) | 1 | 20 | SMG Magazine (Empty) (6) ×1 | 9mm Bullets ×2 |
| mac10 | SMG Magazine (5) | 1 | 32 | (6) ×1 | 9mm Bullets ×2 |
| mac10Akimbo | SMG Magazine (5) | 2 | 64 | (6) ×2 | 9mm Bullets ×2 |
| vector | SMG Magazine (5) | 1 | 25 | (6) ×1 | 9mm Bullets ×2 |
| sawedoff | Shotgun Shell (4) | up to 2 (ammoCount 2) | 2 | – | – |
| combatshotgun | Shotgun Shell (4) | up to 8 (ammoCount 8) | 8 | – | – |
| boltaction | Rifle Bullets (7) | 1 | 6 | – | – |
| m4 | Assault Rifle Magazine (12) | 1 | 30 | Assault Rifle Magazine (Empty) (13) ×1 | Rifle Bullets ×3 |
| m4_uq ("The Infiltrator") | (12) | 1 | 30 | (13) ×1 | Rifle Bullets ×3 |
| ak47 | (12) | 1 | 30 | (13) ×1 | Rifle Bullets ×3 |
| aug | (12) | 1 | 30 | (13) ×1 | Rifle Bullets ×3 |
| scar | (12) | 1 | 20 | (13) ×1 | Rifle Bullets ×3 |
| as50 | AS50 Magazine (17) | 1 | 10 | AS50 Magazine (Empty) (18) ×1 | High Caliber Rounds ×2 |
| lmg | LMG Magazine (20) | 1 | 100 | LMG Magazine (Empty) (21) ×1 | Rifle Bullets ×8 |
| minigun | Minigun Ammo Drum (46) | 1 | 200 | Minigun Ammo Drum (empty) (47) ×1 | Rifle Bullets ×16 |
| pdw | Advanced Magazine (23) | 1 | 20 | Advanced Magazine (Empty) (24) ×1 | Advanced Bullets ×3 |
| pulseRifle | (23) | 1 | 12 | (24) ×1 | Advanced Bullets ×3 |
| flamethrower | Fuel Tank (8) | 1 | 100 | Fuel Tank (Empty) (9) ×1 | – |
| chainsaw | Fuel Tank (8) | 1 | 300 | (9) ×1 | – |
| biogun | Bio Tank (10) | 1 | 30 | Bio Tank (Empty) (11) ×1 | – |
| lasergun | Energy Cell (15) | 1 | 45 | Energy Cell (Empty) (16) ×1 | – |
| teslagun | (15) | 1 | 25 | (16) ×1 | – |
| alienBlaster | (15) | 1 | 10 | (16) ×1 | – |
| blasterRifle | (15) | 1 | 50 | (16) ×1 | – |
| sonicShotgun | (15) | 1 | 8 | (16) ×1 | – |
| scatterBeamGun | (15) | 1 | 40 | (16) ×1 | – |
| mibGun | (15) | 1 | 20 | (16) ×1 | – |
| mibGunAkimbo | (15) | 2 | 40 | (16) ×2 | – |
| nuclearDeathRay | Nuclear Power Cell (106) | 1 | 100 | Nuclear Power Cell (Depleted) (107) ×1 | – |
| powerHammer | Compressed Air Tank (28) | 1 | 300 | Compressed Air Tank (Empty) (29) ×1 | – |
| powerHammerAdv | (28) | 1 | 300 | (29) ×1 | – |
| rocketlauncher | Rocket (14) | 1 | 1 | – | – |
| grimreaper | Rocket (14) | up to 4 (ammoCount 4) | 4 | – | – |
| nukelauncher | Rocket (Tactical Nuke) (105) | 1 | 1 | – | – |
| grenadelauncher | 40mm Grenade (93) | up to 6 (ammoCount 6) | 6 | – | – |
| cyberdemonBlaster ("Nether Blaster") | Nether Charge (63) | 1 | 10 | – | – |

Item-side ammo users outside 02: the Jetpack takes Fuel Tanks; Night Vision Goggles and Tactical Mask take Energy Cells; Jump Pack and Oxygen Tanks take Compressed Air Tanks; the Anti Gravity Device takes Nuclear Power Cells (all 05).

## Magazine items

"Magazine tooltip" means the two tooltip lines `Right click to put in ammo inventory` / `Sneak + Right click to empty`.

| Full (meta) | Empty (meta) | Capacity in gun | Refill recipe (crafting) | Sneak + right-click on a full mag gives | Empty mag recipe |
|---|---|---|---|---|---|
| 9mm Pistol Magazine (2) | (3) | 18 | empty + 9mm Bullets ×3 (shapeless) | 1 empty + 3 × 9mm Bullets | 4 empties from 6 iron nuggets, 1 iron ingot, 1 Mechanical Parts |
| SMG Magazine (5) | (6) | 20/25/32 | empty + 9mm Bullets ×2 | 1 empty + 2 × 9mm Bullets | 4 from 6 iron nuggets + Mechanical Parts |
| Assault Rifle Magazine (12) | (13) | 20/30 | empty + Rifle Bullets ×3 | 1 empty + 3 × Rifle Bullets | 4 from 6 steel nuggets + Mechanical Parts |
| AS50 Magazine (17) | (18) | 10 | empty + High Caliber Rounds ×2 | 1 empty + 2 × High Caliber Rounds | 4 from 6 steel ingots + Mechanical Parts |
| LMG Magazine (20) | (21) | 100 | empty + 8 Rifle Bullets (shaped ring), **or** empty + 2 Rifle Bullets Stacks | 1 empty + 2 × Rifle Bullets Stack | 4 from 7 steel ingots + Mechanical Parts |
| Advanced Magazine (23) | (24) | 12/20 | empty + Advanced Bullets ×3 | 1 empty + 3 × Advanced Bullets | 4 from 6 obsidian-steel ingots + Mechanical Parts (Obsidian Steel) |
| Minigun Ammo Drum (46) | (47) | 200 | empty + 4 Rifle Bullets Stacks | 1 empty + 4 × Rifle Bullets Stack | 4 from 6 steel ingots, 2 plastic sheets, 1 Mechanical Parts (Obsidian Steel) |

The exact grids are in [recipes.md](recipes.md).

Rifle Bullets Stack (88) is a compression item: 4 Rifle Bullets ↔ 1 Stack (two shapeless recipes).

## Tanks, cells and other refillables

| Full | Empty | How it is filled | How empties are made |
|---|---|---|---|
| Fuel Tank (8) | (9) | Chem Lab: empty + 250 mB `fuel` (or 500 mB lava if no `fuel` fluid). Also a Forge fluid container with the same amounts, so tanks/pipes that use `FluidContainerRegistry` can fill and drain it. A reverse Chem Lab recipe drains it. | 4 from plastic / glass / plastic (vertical) |
| Bio Tank (10) | (11) | Chem Lab: Bio Mass + empty tank + 500 mB water. If OreDict `itemBioFuel` exists: 4 × itemBioFuel + empty + 500 mB water. | 1 from 7 steel nuggets + 2 glass |
| Energy Cell (15) | (16) | Charging Station: 50 000 energy units per cell (08/09) | Fabricator: gold ingot + copper wire + 3 redstone + plastic sheet (08) |
| Nuclear Power Cell (106) | (107) | Chem Lab: depleted cell + Enriched Uranium + 1000 mB water (40 ticks) | Fabricator: 2 steel ingots + basic circuit + 4 redstone + 2 lead plates (08) |
| Compressed Air Tank (28) | (29) | Chem Lab: coal + empty tank + 250 mB water | 7 from 7 iron (or tin) plates + lever |

There is **no** crafting recipe for a full Energy Cell or a full Nuclear Power Cell.

## Loose ammunition sources

| Item | Crafting | Machine (08) | Loot (see loot.md) |
|---|---|---|---|
| Stone Bullet (0) | 3 cobblestone + gunpowder → 16 (shapeless) | – | bonus chest, weapon chest T1 |
| 9mm Bullets (1) | 8 copper nuggets + lead ingot + gunpowder → 8 | Ammo Press: 12 per operation | dungeon, weapon chests |
| Shotgun Shell (4) | 3 lead nuggets + 5 copper nuggets + gunpowder → 5 | Ammo Press: 16 | dungeon, weapon chests |
| Rifle Bullets (7) | – (the crafting recipe is commented out) | Ammo Press: 8 | dungeon, weapon chests T2/T3 |
| High Caliber Rounds (19) | – (commented out) | Ammo Press: 4 | dungeon |
| Advanced Bullets (22) | – | Metal Press: Obsidian Steel Plate + TGX → 16 | – |
| Rocket (14) | 5 iron nuggets + TNT + gunpowder → 4 | – | weapon chest T3 |
| Rocket (Tactical Nuke) (105) | Rocket + Small Nuclear Warhead (shapeless) | – | – |
| 40mm Grenade (93) | – | Metal Press: Iron Plate + TNT → 16 | – |
| Nether Charge (63) | – | Chem Lab: netherrack + soul sand + 1000 mB lava → 4 | – |

The Ammo Press inputs are one "metal1" OreDict ingot (`ingotSteel` / `ingotLead`), one "metal2" ingot (`ingotCopper` / `ingotTin` / `ingotIron` / `ingotBronze`) and `itemGunpowder`. The plan selects the output (08).

## Dispenser behaviour

A dispenser holding the shared item fires metas **14 (Rocket)**, **93 (40mm Grenade)** and **105 (Tactical Nuke Rocket)** as live projectiles. Every other meta is dropped like a normal item. Details:
- The projectile is created at the dispenser's output position and aimed along the dispenser's facing (vertical +0.1), velocity 1.1, inaccuracy 6.0. One item is consumed.
- Rocket: a `RocketProjectile` using the Rocket Launcher's damage and radius, lifetime 200 ticks, speed factor 0.3, gravity 0.01.
- Nuke rocket: a `NukeRocketProjectile` using the Nuke Launcher's damage and radius, with the same numbers.
- 40mm: a `GrenadeLauncherProjectile` using the Grenade Launcher's damage, radius, speed and gravity, lifetime 100, 2 bounces. The server also broadcasts a `GrenadeLauncherTrail` particle effect within 200 blocks.
- No custom dispense sound plays (that code is commented out), so the vanilla dispenser click is heard.

The projectile entities and effects belong to 04-projectiles-fx.
