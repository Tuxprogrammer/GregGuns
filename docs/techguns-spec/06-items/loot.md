# Chest loot registered in `TGItems.postInit()`

Forge `ChestGenHooks` is used throughout. Each entry is `item, min count, max count, weight`. For guns and the stand-alone items, the stack is the item at damage 0. Note that a gun at damage 0 is *fully loaded*, because gun damage counts shots fired (see [02](../02-guns/index.md)).

Category-name constants (in [`TGItems`](item-catalog.md)):

| Constant | Category string | Min / max items per chest |
|---|---|---|
| `CHESTGEN_WEAPONCHEST_T1` | `techgunsWeaponchestT1` | 2 / 8 |
| `CHESTGEN_WEAPONCHEST_T2` | `techgunsWeaponchestT2` | 4 / 8 |
| `CHESTGEN_WEAPONCHEST_T3` | `techgunsWeaponchestT3` | 5 / 10 |
| `CHESTGEN_REINFORCEDCHEST_T1` | `techgunsReinforcedchestT1` | 3 / 6 |
| `CHESTGEN_REINFORCEDCHEST_T2` | `techgunsReinforcedchestT2` | 4 / 8 |
| `CHESTGEN_REINFORCEDCHEST_T3` | `techgunsReinforcedchestT3` | 5 / 9 |
| `CHESTGEN_MILBASEBARRACKS` | `techgunsMilBaseBarracks` | 1 / 4 |

Two arrays group them: `WEAPONCHEST_LOOT_TIER = {T1, T2, T3}` and `REINFORCEDCHEST_LOOT_TIER = {T1, T2, T3}`. Worldgen structures pick a category through `util/MBlock` and [`Barracks`](../11-worldgen/structure-catalog.md#b5-barracks-barracks) ([11-worldgen](../11-worldgen/index.md)).

## Vanilla categories

| Category | Item | Min–max | Weight |
|---|---|---|---|
| `bonusChest` | Hand Cannon (gun) | 1–1 | 10 |
| `bonusChest` | Stone Bullet | 8–16 | 10 |
| `dungeonChest` | 9mm Bullets | 4–8 | 3 |
| `dungeonChest` | Shotgun Shell | 4–32 | 3 |
| `dungeonChest` | Rifle Bullets | 2–8 | 3 |
| `dungeonChest` | High Caliber Rounds | 2–6 | 1 |
| `dungeonChest` | Assault Rifle Magazine | 2–4 | 2 |
| `dungeonChest` | SMG Magazine | 3–6 | 2 |
| `dungeonChest` | 9mm Pistol Magazine | 2–6 | 2 |
| `dungeonChest` | Revolver / Sawed Off / Thompson | 1 each | 2 each |
| `dungeonChest` | AK47 / M4 / Bolt Action / Pistol | 1 each | 1 each |
| `dungeonChest` | Nightvision Goggles | 1 | 1 |
| `dungeonChest` | Gas Mask | 1 | 1 |

## Weapon chests

| Category | Item | Min–max | Weight |
|---|---|---|---|
| T1 | Hand Cannon | 1 | 2 |
| T1 | Stone Bullet | 8–16 | 3 |
| T1 | 9mm Bullets | 4–8 | 3 |
| T1 | Shotgun Shell | 4–32 | 3 |
| T1 | Revolver, Sawed Off, Thompson | 1 | 2 each |
| T1 | SMG Magazine | 3–6 | 3 |
| T2 | 9mm Bullets | 4–12 | 4 |
| T2 | Shotgun Shell | 4–32 | 4 |
| T2 | Revolver, Sawed Off, Thompson | 1 | 1 each |
| T2 | SMG Magazine | 3–6 | 3 |
| T2 | Rifle Bullets | 4–12 | 4 |
| T2 | Assault Rifle Magazine | 2–8 | 3 |
| T2 | Bolt Action, Mac10, AK47, M4, Pistol | 1 | 1 each |
| T2 | 9mm Pistol Magazine | 2–5 | 2 |
| T2 | Stielgranate | 3–8 | 2 |
| T3 | 9mm Bullets | 4–12 | 4 |
| T3 | Shotgun Shell | 4–32 | 4 |
| T3 | Rifle Bullets | 4–12 | 4 |
| T3 | Assault Rifle Magazine | 4–8 | 4 |
| T3 | SMG Magazine | 4–8 | 4 |
| T3 | Combat Shotgun, Bolt Action, Mac10, AK47, M4, Pistol | 1 | 1 each |
| T3 | 9mm Pistol Magazine | 2–5 | 4 |
| T3 | Fuel Tank | 3–8 | 3 |
| T3 | Flamethrower, Rocket Launcher | 1 | 1 each |
| T3 | Rocket | 4–8 | 2 |
| T3 | Frag Grenade | 3–8 | 2 |
| T3 | Stielgranate | 4–12 | 2 |

## Reinforced chests (materials)

For the "OreDict first" rows, the code adds the **first** item registered under that OreDict name, and only if one exists.

| Category | Item | Min–max | Weight |
|---|---|---|---|
| T1 | Coal | 4–16 | 3 |
| T1 | Iron Ingot | 4–16 | 3 |
| T1 | Stone | 16–32 | 3 |
| T1 | Oak Log (log:0) | 8–16 | 3 |
| T1 | Gunpowder | 4–8 | 3 |
| T1 | OreDict first [`ingotTin`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | 8–32 | 3 |
| T1 | OreDict first [`ingotCopper`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | 8–32 | 3 |
| T1 | OreDict first [`ingotLead`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | 6–24 | 3 |
| T2 | Coal | 4–16 | 3 |
| T2 | Iron Ingot | 3–12 | 3 |
| T2 | Stone | 16–32 | 3 |
| T2 | Oak Log | 6–12 | 3 |
| T2 | Gunpowder | 4–8 | 3 |
| T2 | Gold Ingot | 2–6 | 3 |
| T2 | Rubber Bar | 2–8 | 3 |
| T2 | first `ingotTin` / `ingotCopper` / `ingotLead` | 4–16 / 4–16 / 3–12 | 3 each |
| T2 | first `ingotSilver` / [`ingotSteel`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) | 2–8 / 2–8 | 3 each |
| T3 | Coal | 8–16 | 3 |
| T3 | Iron Ingot | 8–16 | 3 |
| T3 | Stone | 24–32 | 3 |
| T3 | Oak Log | 8–16 | 3 |
| T3 | Gunpowder | 6–12 | 3 |
| T3 | Gold Ingot | 6–18 | 3 |
| T3 | Rubber Bar | 3–10 | 3 |
| T3 | Plastic Sheet | 2–8 | 3 |
| T3 | first `ingotTin` / `ingotCopper` / `ingotLead` | 8–16 each | 3 each |
| T3 | first `ingotSilver` / `ingotSteel` | 4–16 / 4–16 | 3 each |

## Military base barracks

| Item | Min–max | Weight |
|---|---|---|
| T1 combat boots / chestplate / helmet / leggings ([05](../05-armor/index.md)) | 1 | 2 each |
| T2 combat boots / chestplate / helmet / leggings ([05](../05-armor/index.md)) | 1 | 1 each |
| Glider | 1 | 1 |
| Gas Mask | 1 | 2 |
| Gas Mask Filter | 1 | 1 |
| Nightvision Goggles | 1 | 1 |
| Oxygen Mask | 1 | 1 |
| Compressed Air Tank | 1–5 | 2 |
| Oxygen Tanks | 1 | 1 |

The loot `ItemStack`s for shared items are the `TGItems` template stacks, so the meta is preserved.
