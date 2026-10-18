# Structure loot

The loot tables are registered in `TGItems.postInit` (the code is owned by **06-items**) through Forge `ChestGenHooks`. They are documented here because worldgen is what fills chests from them. Item identities (guns, ammo, armour) are cross-referenced to 02-guns, 05-armor and 06-items. Names below are the Java field names, for example `TGuns.ak47` or `TGItems.bullets9mm`.

## How chests get filled

Chests are filled right after placement:

* **Scanned structures and camp crates** go through `MBlock.setBlock`, which uses a static unseeded `Random`.
* **Barracks** use `world.rand`.

Each fill calls `ChestGenHooks.getCount(category, rnd)` and then vanilla `WeightedRandomChestContent.generateChestContents`. That function makes `count` weighted picks. Each pick has a stack size between min and max inclusive and goes into a random slot; if the stack is larger than the item's max size it is split into single items. Forge's count rule is `min + rnd(max − min)` when min < max, so the "max" value is exclusive.

| Chest block | Category used |
|---|---|
| vanilla `chest` | `ChestGenHooks.DUNGEON_CHEST` (vanilla dungeon pool + TG additions below) |
| `TGBlocks.tgchest` (Reinforced chest) | `REINFORCEDCHEST_LOOT_TIER[lootTier]` = `techgunsReinforcedchestT1/T2/T3` |
| `TGBlocks.tgchest_weapon` (Weapon chest) | `WEAPONCHEST_LOOT_TIER[lootTier]` = `techgunsWeaponchestT1/T2/T3` |
| Barracks vanilla chests | `techgunsMilBaseBarracks` |

| Structure | lootTier → TG table | Vanilla chests | `tgchest` | `tgchest_weapon` | Other |
|---|---|---|---|---|---|
| Small Factory Building | 0 → T1 | 2 | – | – | 2 furnaces (empty) |
| Small Train Station | 0 → T1 | 1 | – | – | furnace |
| Medium House | 0 → T1 | 8 | 6 | 4 | |
| Big Bunker | 1 → T2 | 3 | 3 | 8 | 2 Zombie Hole spawners |
| Aircraft Carrier | 2 → T3 | 6 | 8 | 12 | camp flag (Army Soldier, Commando) |
| Submarine | 2 → T3 | 3 | 2 | 4 | |
| Camp Bunker types 2/3 | 2 → T3 | – | 5 % per interior column | 5 % per interior column | crates |
| Camp crate props (variants 0/1/5) | 2 → T3 | – | 5 % per column | 5 % per column | crates, lantern |
| Barracks | – | one per qualifying wall cell | – | – | beds |
| Alien Bug Nest | – | – | – | – | Bug Hole spawner per satellite chamber |
| City Gas Station (dormant) | 0 | 2 | – | – | |

## Table contents

Each row is: item, metadata (where it is passed), stack min–max, weight.

### `techgunsWeaponchestT1` (count 2–8)
| Item | Stack | Weight |
|---|---|---|
| `TGuns.handcannon` (meta 0) | 1 | 2 |
| `TGItems.stonebullets` | 8–16 | 3 |
| `TGItems.bullets9mm` | 4–8 | 3 |
| `TGItems.shotgunShell` | 4–32 | 3 |
| `TGuns.revolver` | 1 | 2 |
| `TGuns.sawedoff` | 1 | 2 |
| `TGuns.thompson` | 1 | 2 |
| `TGItems.smgMag` | 3–6 | 3 |

### `techgunsWeaponchestT2` (count 4–8)
| Item | Stack | Weight |
|---|---|---|
| `bullets9mm` | 4–12 | 4 |
| `shotgunShell` | 4–32 | 4 |
| `revolver` | 1 | 1 |
| `sawedoff` | 1 | 1 |
| `thompson` | 1 | 1 |
| `smgMag` | 3–6 | 3 |
| `rifleBullets` | 4–12 | 4 |
| `m4Mag` | 2–8 | 3 |
| `boltaction` | 1 | 1 |
| `mac10` | 1 | 1 |
| `ak47` | 1 | 1 |
| `m4` | 1 | 1 |
| `pistol` | 1 | 1 |
| `pistolMag9mm` | 2–5 | 2 |
| `TGuns.stielGranate` (meta 0) | 3–8 | 2 |

### `techgunsWeaponchestT3` (count 5–10)
| Item | Stack | Weight |
|---|---|---|
| `bullets9mm` | 4–12 | 4 |
| `shotgunShell` | 4–32 | 4 |
| `rifleBullets` | 4–12 | 4 |
| `m4Mag` | 4–8 | 4 |
| `smgMag` | 4–8 | 4 |
| `combatShotgun` | 1 | 1 |
| `boltaction` | 1 | 1 |
| `mac10` | 1 | 1 |
| `ak47` | 1 | 1 |
| `m4` | 1 | 1 |
| `pistol` | 1 | 1 |
| `pistolMag9mm` | 2–5 | 4 |
| `flamethrowerTank` | 3–8 | 3 |
| `flamethrower` | 1 | 1 |
| `rocketlauncher` | 1 | 1 |
| `rocketAmmo` | 4–8 | 2 |
| `fragGrenade` | 3–8 | 2 |
| `stielGranate` | 4–12 | 2 |

### `techgunsReinforcedchestT1` (count 3–6)
| Item | Stack | Weight |
|---|---|---|
| coal | 4–16 | 3 |
| iron_ingot | 4–16 | 3 |
| stone | 16–32 | 3 |
| oak log (`log` meta 0) | 8–16 | 3 |
| gunpowder | 4–8 | 3 |
| first OreDict `ingotTin`, if any | 8–32 | 3 |
| first OreDict `ingotCopper`, if any | 8–32 | 3 |
| first OreDict `ingotLead`, if any | 6–24 | 3 |

### `techgunsReinforcedchestT2` (count 4–8)
| Item | Stack | Weight |
|---|---|---|
| coal | 4–16 | 3 |
| iron_ingot | 3–12 | 3 |
| stone | 16–32 | 3 |
| oak log | 6–12 | 3 |
| gunpowder | 4–8 | 3 |
| gold_ingot | 2–6 | 3 |
| `TGItems.rubberBar` | 2–8 | 3 |
| `ingotTin` | 4–16 | 3 |
| `ingotCopper` | 4–16 | 3 |
| `ingotLead` | 3–12 | 3 |
| `ingotSilver` | 2–8 | 3 |
| `ingotSteel` | 2–8 | 3 |

The OreDict ingot rows are only added if that OreDict entry exists.

### `techgunsReinforcedchestT3` (count 5–9)
| Item | Stack | Weight |
|---|---|---|
| coal | 8–16 | 3 |
| iron_ingot | 8–16 | 3 |
| stone | 24–32 | 3 |
| oak log | 8–16 | 3 |
| gunpowder | 6–12 | 3 |
| gold_ingot | 6–18 | 3 |
| `rubberBar` | 3–10 | 3 |
| `TGItems.plasticSheet` | 2–8 | 3 |
| `ingotTin` | 8–16 | 3 |
| `ingotCopper` | 8–16 | 3 |
| `ingotLead` | 8–16 | 3 |
| `ingotSilver` | 4–16 | 3 |
| `ingotSteel` | 4–16 | 3 |

The OreDict ingot rows are only added if that OreDict entry exists.

### `techgunsMilBaseBarracks` (count 1–4)
| Item | Stack | Weight |
|---|---|---|
| `TGArmors.t1_combat_Boots` | 1 | 2 |
| `TGArmors.t1_combat_Chestplate` | 1 | 2 |
| `TGArmors.t1_combat_Helmet` | 1 | 2 |
| `TGArmors.t1_combat_Leggings` | 1 | 2 |
| `TGArmors.t2_combat_Boots` | 1 | 1 |
| `TGArmors.t2_combat_Chestplate` | 1 | 1 |
| `TGArmors.t2_combat_Helmet` | 1 | 1 |
| `TGArmors.t2_combat_Leggings` | 1 | 1 |
| `TGItems.glider` | 1 | 1 |
| `TGItems.gasMask` | 1 | 2 |
| `TGItems.gasMaskFilter` | 1 | 1 |
| `TGItems.nightVisionGoggles` | 1 | 1 |
| `TGItems.oxygenMask` | 1 | 1 |
| `TGItems.compressedAirTank` | 1–5 | 2 |
| `TGItems.scubaTanks` | 1 | 1 |

### Additions to vanilla pools
These pools are also hit by vanilla dungeons and other structures.

**`DUNGEON_CHEST`:**
| Item | Stack | Weight |
|---|---|---|
| `bullets9mm` | 4–8 | 3 |
| `shotgunShell` | 4–32 | 3 |
| `rifleBullets` | 2–8 | 3 |
| `highCalibreBullets` | 2–6 | 1 |
| `m4Mag` | 2–4 | 2 |
| `smgMag` | 3–6 | 2 |
| `pistolMag9mm` | 2–6 | 2 |
| `revolver` | 1 | 2 |
| `sawedoff` | 1 | 2 |
| `thompson` | 1 | 2 |
| `ak47` | 1 | 1 |
| `m4` | 1 | 1 |
| `boltaction` | 1 | 1 |
| `pistol` | 1 | 1 |
| `nightVisionGoggles` | 1 | 1 |
| `gasMask` | 1 | 1 |

**`BONUS_CHEST`:**
| Item | Stack | Weight |
|---|---|---|
| `handcannon` | 1 | 10 |
| `stonebullets` | 8–16 | 10 |
