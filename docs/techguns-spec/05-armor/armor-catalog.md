# Armor catalog

Source: `java/techguns/TGArmors.java`, `java/techguns/items/armors/GenericArmor.java`, `GenericArmorMultiCamo.java`, `PoweredArmor.java`, `ArmorPowerType.java`, plus `java/techguns/damagesystem/TGArmorMaterial.java` (owned by [01-core](../01-core/index.md), summarized here only as far as armor stats need it).

Everything is registered in `TGArmors.init()`. There are **11 four-piece sets and 1 single-piece hat (the beret)**, 45 items in total. All are in the Techguns creative tab.

## 1. Class hierarchy

| Class | Extends | Role |
|---|---|---|
| `GenericArmor` | vanilla `ItemArmor`, implements Forge `ISpecialArmor` | Base armor. Holds a `TGArmorMaterial`, a texture base name, per-piece bonus values, an optional custom-model index, repair material data and the face/back-slot hide flags. |
| `GenericArmorMultiCamo` | `GenericArmor`, implements `ICamoChangeable` | Has an array of texture base names (one per camo). The camo index is stored in item NBT. |
| `PoweredArmor` | `GenericArmorMultiCamo` | Adds a power type (`STEAM` or `RF`), an internal power store (NBT), a battery item for the chestplate, and a second "unpowered" set of bonus values. |
| `ArmorPowerType` | enum | `STEAM` (shown as "Steam") and `RF` (shown as "RF"). Labels only; nothing converts between them. |
| `ICamoChangeable` | interface | `getCamoCount`, `switchCamo(stack)`, `switchCamo(stack, back)`, `getCurrentCamoIndex`, `getCurrentCamoName`. Also implemented by guns, tools, camo-net item blocks and the special-slot items. |

Constructor behavior (`GenericArmor`):
- Builds a vanilla `ArmorMaterial` from the TG material (see §2): the vanilla per-slot armor points are the rounded **physical** values, and the vanilla durability multiplier is the base durability. Enchantability comes from the material, and every Techguns material passes **0**, so no Techguns armor can be enchanted at a table.
- Unlocalized name = the given name. The item icon is `techguns:<unlocalizedName>` (so `textures/items/<name>.png`, owned by [06-items](../06-items/index.md)).
- `getUnlocalizedName(stack)` prefixes `techguns.`, so lang keys are `techguns.item.<name>.name`.
- Max damage = `material.getDurability(slot)`, which overrides the vanilla value.
- `armorValue` (used for the vanilla armor bar) = the rounded physical value for the slot.

## 2. Materials (`TGArmorMaterial`)

Each material has a **base durability**, an enchantability (always 0 here), nine damage-type armor totals, and a penetration resistance.

The constructor used by `TGArmors` takes one armor value **A**. It sets PHYSICAL = PROJECTILE = A, sets FIRE, EXPLOSION, ENERGY, ICE and LIGHTNING to 0.75·A, and sets POISON and RADIATION to 0. Chained setters then override individual types.

**Per-slot split** (same for every material; there are setters to change it, but nothing calls them): helmet 0.25, chestplate 0.30, leggings 0.25, boots 0.20 of each total.

**Durability per piece** = round(0.25 × 55 × baseDur) = 13.75 × baseDur. It is the same for all four slots.

| Material | baseDur | Durability (each piece) | PHYS | PROJ | EXPL | ENERGY | FIRE | ICE | LIGHTN | POISON | RAD | Pen. resist | Vanilla armor points H/C/L/B |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| T1_COMBAT | 60 | 825 | 15 | 15 | 11.25 | 11.25 | 11.25 | 11.25 | 11.25 | 0 | 0 | 0 | 4/5/4/3 |
| T1_SCOUT | 60 | 825 | 13 | 13 | 9.75 | 9.75 | 9.75 | 9.75 | 9.75 | 0 | 0 | 0 | 3/4/3/3 |
| T1_MINER | 60 | 825 | 13 | 13 | 9.75 | 9.75 | 9.75 | 9.75 | 9.75 | 0 | 0 | 0 | 3/4/3/3 |
| T1_STEAM | 200 | 2750 | 20 | 20 | 18 | 16 | 18 | 16 | 16 | 10 | 6 | 0.10 | 5/6/5/4 |
| T2_COMBAT | 72 | 990 | 19 | 19 | 14.25 | 14.25 | 14.25 | 14.25 | 14.25 | 0 | 0 | 0 | 5/6/5/4 |
| T2_COMMANDO | 72 | 990 | 18 | 18 | 15 | 15 | 15 | 15 | 15 | 10 | 5 | 0 | 5/5/5/4 |
| T2_RIOT | 96 | 1320 | 20 | 20 | 18 | 18 | 18 | 17 | 17 | 16 | 16 | 0.10 | 5/6/5/4 |
| T3_COMBAT | 240 | 3300 | 22 | 22 | 18 | 18 | 18 | 18 | 18 | 10 | 6 | 0.15 | 6/7/6/4 |
| T3_POWER | 360 | 4950 | 24 | 24 | 22 | 20 | 20 | 20 | 20 | 15 | 10 | 0.25 | 6/7/6/5 |
| T3_MINER | 300 | 4125 | 21 | 21 | 21 | 21 | 21 | 21 | 21 | 21 | 21 | 0.15 | 5/6/5/4 |
| T3_EXO | 300 | 4125 | 22 | 22 | 18 | 18 | 18 | 18 | 18 | 10 | 6 | 0.15 | 6/7/6/4 |
| T2_BERET | 60 | 825 | 8 | 8 | 6 | 6 | 6 | 6 | 6 | 0 | 0 | 0 | 2/2/2/2 (only the helmet exists) |

Per-piece value for a damage type = total × slot factor. For example, T3_POWER chestplate PHYS = 24 × 0.30 = 7.2. A full set's per-type value equals the material total. The vanilla points are rounded half-up, so the full T3_POWER set fills the vanilla armor bar (6+7+6+5 = 24).

A commented-out block of older vanilla `EnumHelper` materials remains in the source. It is inactive.

## 3. Protection and durability behavior (`ISpecialArmor`)

- **"Broken" state:** a piece whose damage is ≥ maxDamage−1 (one durability point left) counts as broken. It gives 0 protection, shows 0 on the vanilla armor bar, and contributes no bonuses. Armor **never actually breaks**. `damageArmor` caps incoming durability damage so that at least 1 point always remains, and creative-mode players take no durability damage.
- **Damage reduction (`getProperties`)**, when not broken:
  1. Convert the incoming `DamageSource` to a [`TGDamageSource`](../02-guns/damage-system.md#2-tgdamagesource-extends-vanilla-entitydamagesource) ([01-core](../01-core/index.md)). Its `damageType` picks which armor value applies.
  2. If that armor value > 0 and the source has armor penetration > 0: subtract this piece's material penetration resistance, apply the config PvP/NPC penetration multiplier ([`DamageSystem.calcPenetration`](../02-guns/index.md#coverage), [01-core](../01-core/index.md)), clamp to [0,1], and **write the result back into the damage source** (see bug [ARM-02](index.md#suspected-bugs)).
  3. Reduction ratio for this piece = (1 − penetration) × 0.04 × pieceArmorValue (from `DamageSystem.getReduction`, [01-core](../01-core/index.md)). In effect each armor point blocks 4 % of the non-penetrating share, so 24 points block 96 %.
  4. The piece returns Forge `ArmorProperties(priority 0, ratio, maxAbsorb = maxDamage + 1 − damage)`. Forge adds up the ratios of all pieces.
- **Tooltip percentage helper `formatAV(value)`:** prints the value (format `#.##`) and, in parentheses, value/24 × 0.96 × 100 %. Constants: `MAX_ARMOR = 24`, `ARMOR_CAP = 24/25` (the cap is the same for every damage type).
- **Vanilla anvil repair** (`getIsRepairable`): the repair item is the metal repair mat if set, otherwise the cloth mat (matched with the OreDictionary wildcard). If neither is set, the piece cannot be repaired on an anvil.
- **Repair Bench** (machine owned by [09-machines-utility](../09-machines-utility/index.md)): `canRepairOnRepairBench` is true when the piece's repair-material count > 0. `getRepairMats(stack)` returns:
  - `dmgFraction = damage / (maxDamage − 1)`
  - `count = ceil(totalMats × dmgFraction)`
  - `metal = ceil(count × metalRatio)`, `cloth = count − metal`

  Each material is returned only if it is non-null and its count is > 0.

## 4. Custom model index (`setArmorModel`)

| Index | Model instance ([`ClientProxy.armorModels`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy)) | Used by |
|---|---|---|
| −1 (default) | vanilla biped armor model | T1 combat/scout/miner, T2 combat/commando, T3 combat, T3 HEV |
| 0 | `ModelSteamArmor(type 0)` | steam helmet, chestplate, boots |
| 1 | `ModelSteamArmor(type 1)` | steam leggings |
| 2 | `ModelT3PowerArmor(type 0)` | power helmet, chestplate, boots |
| 3 | `ModelT3PowerArmor(type 1)` | power leggings |
| 4 | `ModelExoSuit(type 0, scale 1.0)` | exo helmet |
| 5 | `ModelExoSuit(type 1, scale 0.5)` | exo leggings |
| 6 | `ModelExoSuit(type 0, scale 0.75)` | exo chestplate and exo boots |
| 7 | [`ModelBeret`](rendering-and-models.md#25-modelberet-index-7) | beret |
| 8 | `ModelArmorCoat(type 0, scale 1.0)` | riot helmet |
| 9 | `ModelArmorCoat(type 1, scale 0.51)` | riot chestplate |
| 10 | `ModelArmorCoat(type 2, scale 0.49)` | riot leggings |
| 11 | `ModelArmorCoat(type 3, scale 0.75)` | riot boots |

**Texture-name rule** (`getArmorTexture`):
- The armor uses a **double texture** (`textures/armor/<base>_1.png` for helmet, chest and boots; `<base>_2.png` for leggings) when its model index is −1, 4, 5, 6 or 8–11.
- Otherwise (0–3 and 7) it uses a **single texture** `textures/armor/<base>.png` for all slots.
- For multi-camo items, `<base>` = `textureNames[camo]`.

See rendering-and-models.md for how the model is posed.

## 5. Piece catalog

Column legend: **Spd** = movement speed bonus; **Jmp** = jump bonus (added to motionY); **Fall** = fall-damage multiplier reduction / free fall height in blocks; **Mine** = break-speed bonus; **WMine** = extra underwater break-speed bonus; **Acc** = gun accuracy bonus; **HP** = extra max health (half-hearts); **KB** = knockback resistance; **Step** = step assist; **O2** = oxygen gear; **Cool** = cooling system; **WSpd** = speed bonus in water or lava. Blank = 0. For powered armor, all values apply **only while the piece has power**. Every unpowered value is 0, because every registration passes 0 for the unpowered variants, and the single-argument `setKnockbackResistance` sets only the powered value.

Repair column format: *metal item / cloth item, metal ratio, total mats*.

### 5.1 T1 Combat ("Soldier's …"): `GenericArmor`, material T1_COMBAT, texture `t1_combat`, no camo

| Field / registry name | English name | Slot | KB | Repair |
|---|---|---|---|---|
| `t1_combat_Helmet` / `t1_combat_helmet` | Soldier's Helmet | 0 | 0.05 | iron ingot / heavy cloth, 0.5, 2 |
| `t1_combat_Chestplate` / `t1_combat_chestplate` | Soldier's Vest | 1 | 0.20 | iron / cloth, 0.5, 4 |
| `t1_combat_Leggings` / `t1_combat_leggings` | Soldier's Pants | 2 | 0.10 | iron / cloth, 1/3, 3 |
| `t1_combat_Boots` / `t1_combat_boots` | Soldier's Boots | 3 | 0.05 | iron / cloth, 0.5, 2 |

### 5.2 T1 Scout ("Bandit …"): `GenericArmorMultiCamo`, T1_SCOUT, camos `t1_scout`, `t1_scout_forest`, `t1_scout_snow`, `t1_scout_black`

| Registry name | English | Spd | Jmp | Fall | Repair |
|---|---|---|---|---|---|
| `t1_scout_helmet` | Bandit Mask | 0.125 | 0.02 | | – / heavy cloth, 0, 2 |
| `t1_scout_chestplate` | Bandit Vest | 0.125 | 0.02 | | – / cloth, 0, 4 |
| `t1_scout_leggings` | Bandit Pants | 0.125 | 0.02 | | – / cloth, 0, 3 |
| `t1_scout_boots` | Bandit Boots | 0.125 | 0.10 | 0.2 / 1.0 | – / cloth, 0, 2 |

Because there is no metal mat, the anvil repair item is heavy cloth.

### 5.3 T1 Miner ("Miner's …"): `GenericArmorMultiCamo`, T1_MINER, camos `t1_miner`, `t1_miner_red`, `t1_miner_green`, `t1_miner_black`

The helmet calls `setCamoNameSuffix("helmet")`, so its camo names come from `techguns.item.t1_miner.helmet.camoname.N`.

| Registry name | English | Spd | Jmp | Mine | Fall | Repair |
|---|---|---|---|---|---|---|
| `t1_miner_helmet` | Miner's Helmet | 0.08 | | 0.05 | | iron / cloth, 0.5, 2 |
| `t1_miner_chestplate` | Miner's Jacket | 0.08 | | 0.05 | | iron / cloth, 0.5, 4 |
| `t1_miner_leggings` | Miner's Pants | 0.08 | | 0.05 | | iron / cloth, 1/3, **2** |
| `t1_miner_boots` | Miner's Boots | 0.08 | 0.10 | 0.05 | 0.2 / 1.0 | iron / cloth, 0.5, 2 |

### 5.4 Steam Armor: `PoweredArmor`, STEAM, T1_STEAM, camos `steam_armor`, `steam_armor_rusty`, `steam_armor_iron`, `steam_armor_hazard`

| Registry name | English | maxpower | Spd | Jmp | Mine | HP | Fall | Step | KB | Model | Flags | Repair |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **`steam,_helmet`** (typo, unlocalized `steam_helmet`) | Steam Helmet | 10 | 0.05 | 0.03 | 0.05 | 1 | | | 0.15 | 0 | hides face slot | steam armor plate / steam armor plate, 1.0, 2 |
| `steam_chestplate` | Steam Chestplate | 3600 | 0.05 | 0.03 | 0.05 | 2 | | | 0.30 | 0 | hides back slot; battery = compressed air tank → empty tank | plate, 1.0, 4 |
| `steam_leggings` | Steam Legplates | 10 | 0.05 | 0.03 | 0.05 | 1 | | | 0.20 | 1 | | plate, 1.0, 3 |
| `steam_boots` | Steam Boots | 10 | 0.05 | 0.15 | 0.05 | 1 | 0.2 / 1.0 | 1.0 | 0.15 | 0 | | plate, 1.0, 2 |

### 5.5 T2 Combat ("Combat …"): `GenericArmorMultiCamo`, T2_COMBAT, camos `t2_combat`, `t2_combat_wood`, `t2_combat_desert`, `t2_combat_arctic`, `t2_combat_swat`, `t2_combat_security`

| Registry name | English | Spd | Jmp | KB | Repair |
|---|---|---|---|---|---|
| `t2_combat_helmet` | Combat Helmet | 0.10 | | 0.10 | obsidian steel ingot / heavy cloth, 0.5, 2 |
| `t2_combat_chestplate` | Combat Vest | 0.10 | | 0.25 | 0.5, 4 |
| `t2_combat_leggings` | Combat Pants | 0.10 | | 0.15 | 1/3, 3 |
| `t2_combat_boots` | Combat Boots | 0.10 | 0.10 | 0.10 | 0.5, 2 |

### 5.6 T2 Commando ("Commando …"): `GenericArmor`, T2_COMMANDO, texture `t2_commando`, no camo, **no repair mats** (cannot be repaired)

| Registry name | English | Spd | Jmp | WMine | Acc | KB | Other |
|---|---|---|---|---|---|---|---|
| `t2_commando_helmet` | Commando Headgear | 0.10 | | 1.25 | 0.05 | 0.05 | Oxygen gear 1.0 |
| `t2_commando_chestplate` | Commando Vest | 0.10 | | 1.25 | 0.05 | 0.20 | |
| `t2_commando_leggings` | Commando Pants | 0.10 | | 1.25 | 0.05 | 0.10 | |
| `t2_commando_boots` | Commando Boots | 0.10 | 0.10 | 1.25 | 0.05 | 0.05 | Fall 0.2 / 1.0 |

### 5.7 T2 Riot / "Ranger Veteran": `GenericArmorMultiCamo`, T2_RIOT, single camo `t2_riotArmor`, models 8–11 (double texture)

| Registry name | English | Spd | Jmp | KB | Model | Repair |
|---|---|---|---|---|---|---|
| `t2_riot_helmet` | Ranger Veteran Helmet | 0.12 | | 0.125 | 8 | obsidian steel ingot / treated leather, 0.5, 2 |
| `t2_riot_chestplate` | Ranger Veteran Jacket | 0.12 | | 0.275 | 9 | 0.5, 4 |
| `t2_riot_leggings` | Ranger Veteran Pants | 0.13 | | 0.175 | 10 | 1/3, 3 |
| `t2_riot_boots` | Ranger Veteran Boots | 0.13 | 0.15 | 0.125 | 11 | 0.5, 2 |

### 5.8 T3 Combat ("Advanced Combat …"): `GenericArmorMultiCamo`, T3_COMBAT, camos `t3_combat`, `t3_combat_silver`, `t3_combat_green`

| Registry name | English | Spd | Jmp | KB | Repair |
|---|---|---|---|---|---|
| `t3_combat_helmet` | Advanced Combat Helmet | 0.10 | | 0.15 | carbon plate / carbon fibers, 0.5, 2 |
| `t3_combat_chestplate` | Advanced Combat Chestplate | 0.10 | | 0.30 | 0.5, 4 |
| `t3_combat_leggings` | Advanced Combat Leggings | 0.10 | | 0.20 | 1/3, 3 |
| `t3_combat_boots` | Advanced Combat Boots | 0.10 | 0.10 | 0.15 | 0.5, 2 |

### 5.9 T3 Power Armor: `PoweredArmor`, RF, T3_POWER, camos `powerarmor` (Paladin), `powerarmor_dark` (Outcast), `powerarmor_dark2` (Outcast 2)

| Registry name | English | maxpower | Spd | Jmp | Mine | HP | Fall | Step | KB | Model | Flags | Repair |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| `t3_power_helmet` | Power Armor Helmet | 10 | 0.05 | 0.03 | 0.05 | 1 | | | 0.20 | 2 | hides face slot | power plating / gold wire, 0.5, 2 |
| `t3_power_chestplate` | Power Armor Chestplate | 3600 | 0.05 | 0.03 | 0.05 | 2 | | | 0.35 | 2 | back slot shown; battery = energy cell → empty cell | 0.5, 4 |
| `t3_power_leggings` | Power Armor Leggings | 10 | 0.05 | 0.03 | 0.05 | 1 | | | 0.25 | 3 | | 1/3, 3 |
| `t3_power_boots` | Power Armor Boots | 10 | 0.05 | 0.15 | 0.05 | 1 | 0.2 / 1.0 | 1.0 | 0.20 | 2 | | 0.5, 2 |

### 5.10 T3 Mining / "Hazardous Environment" (HEV): `PoweredArmor`, RF, T3_MINER, camos `hevsuit` (Orange), `hevsuit_silver`, `hevsuit_black`, vanilla model (double texture)

| Registry name | English | maxpower | Spd | Jmp | WSpd | Mine | WMine | Other | KB | Repair |
|---|---|---|---|---|---|---|---|---|---|---|
| `t3_miner_helmet` | Hazardous Environment Helmet | 10 | 0.10 | **0.30** | 1.25 | 0.10 | 1.25 | Oxygen gear 1.0; hides face slot | 0.10 | carbon plate / elite circuit board, 0.5, 2 |
| `t3_miner_chestplate` | Hazardous Environment Chestplate | 10000 | 0.10 | 0.03 | 1.25 | 0.10 | 1.25 | Cooling system 1.0; battery energy cell | 0.25 | 0.5, 4 |
| `t3_miner_leggings` | Hazardous Environment Leggings | 10 | 0.10 | 0.03 | 1.25 | 0.10 | 1.25 | | 0.15 | 1/3, 3 |
| `t3_miner_boots` | Hazardous Environment Boots | 10 | 0.10 | 0.15 | 1.25 | 0.10 | 1.25 | Step 1.0; Fall 0.5 / 3.0 | 0.10 | 0.5, 2 |

### 5.11 T3 Exo Suit: `PoweredArmor`, RF, T3_EXO, camos `t3_exo`, `t3_exo_silver`, `t3_exo_green`, models 4/6/5/6 (double texture)

| Registry name | English | maxpower | Spd | Jmp | Mine | Other | KB | Model | Repair |
|---|---|---|---|---|---|---|---|---|---|
| `t3_exo_helmet` | Exo Suit Helmet | 10 | 0.20 | 0.10 | 0.075 | face slot shown | 0.15 | 4 | carbon plate / carbon fibers, 0.5, 2 |
| `t3_exo_chestplate` | Exo Suit Chestplate | 7200 | 0.20 | 0.10 | 0.075 | battery energy cell; back slot shown | 0.30 | 6 | carbon plate / **copper wire**, 0.5, 4 |
| `t3_exo_leggings` | Exo Suit Leggings | 10 | 0.20 | 0.10 | 0.075 | | 0.20 | 5 | carbon plate / carbon fibers, 1/3, 3 |
| `t3_exo_boots` | Exo Suit Boots | 10 | 0.20 | 0.30 | 0.075 | Step 1.0; Fall 0.5 / 5.0 | 0.15 | 6 | 0.5, 2 |

### 5.12 Beret: `GenericArmorMultiCamo`, T2_BERET, helmet only, camos `beret_texture` (Red), `beret_texture_black`, `beret_texture_green`, model 7 (single texture)

| Registry name | English | Spd | Repair |
|---|---|---|---|
| `t2_beret` | Beret | 0.10 | heavy cloth / heavy cloth, 1.0, 2 |

### 5.13 Obtaining, lang key and English name for every piece

Added by the consistency pass. All 45 pieces: the lang key is `techguns.item.<unlocalized>.name` (full text in [lang-and-strings.md](lang-and-strings.md) §1). Grids are top row first; keys are defined per set. The full recipe tables, repair routes and loot are in [recipes.md](recipes.md). Armor is also worn by NPCs ([`ArmySoldier`](../10-entities-npcs/per-mob.md#6-army-soldier-armysoldier), [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit), [`Commando`](../10-entities-npcs/per-mob.md#9-commando-commando), [`DictatorDave`](../10-entities-npcs/per-mob.md#13-dictator-dave-dictatordave-mini-boss), [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast), [`PsychoSteve`](../10-entities-npcs/per-mob.md#12-psycho-steve-psychosteve-early-game-mini-boss), [`SkeletonSoldier`](../10-entities-npcs/per-mob.md#5-skeleton-soldier-skeletonsoldier), [`StormTrooper`](../10-entities-npcs/per-mob.md#10-stormtrooper-stormtrooper-registry-name-stormtrooper), [`ZombieFarmer`](../10-entities-npcs/per-mob.md#2-zombie-farmer-zombiefarmer), [`ZombieMiner`](../10-entities-npcs/per-mob.md#3-zombie-miner-zombieminer), [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier), [`GenericNPC`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) default kit; see [10-entities-npcs](../10-entities-npcs/index.md)). None of those classes overrides the equipment drop chances, so the vanilla default (inferred from vanilla 1.7.10 `EntityLiving`, not read in the Techguns source: about 8.5 % per equipped slot, only when killed by a player) applies to worn pieces. Camo on NPC pieces is set with `getNewWithCamo`. There is no armor in worldgen or any shop. Loot chest entries are listed in each row's note and in [recipes.md](recipes.md) §4.

| Registry name | Lang key | English name | Obtained from |
|---|---|---|---|
| `t1_combat_helmet` | `techguns.item.t1_combat_helmet.name` | Soldier's Helmet | Crafting: `iii` / `c_c` (`i` = "ingotIron", `c` = Heavy Cloth). Loot: `techgunsMilBaseBarracks` chest (weight 2 each). |
| `t1_combat_chestplate` | `techguns.item.t1_combat_chestplate.name` | Soldier's Vest | Crafting: `c_c` / `iii` / `ccc` (`i` = "ingotIron", `c` = Heavy Cloth). Loot: `techgunsMilBaseBarracks` chest (weight 2 each). |
| `t1_combat_leggings` | `techguns.item.t1_combat_leggings.name` | Soldier's Pants | Crafting: `iii` / `c_c` / `c_c` (`i` = "ingotIron", `c` = Heavy Cloth). Loot: `techgunsMilBaseBarracks` chest (weight 2 each). |
| `t1_combat_boots` | `techguns.item.t1_combat_boots.name` | Soldier's Boots | Crafting: `c_c` / `i_i` (`i` = "ingotIron", `c` = Heavy Cloth). Loot: `techgunsMilBaseBarracks` chest (weight 2 each). |
| `t1_scout_helmet` | `techguns.item.t1_scout_helmet.name` | Bandit Mask | Crafting: `ccc` / `c_c` (`c` = Heavy Cloth). Crafting only. |
| `t1_scout_chestplate` | `techguns.item.t1_scout_chestplate.name` | Bandit Vest | Crafting: `c_c` / `ccc` / `ccc` (`c` = Heavy Cloth). Crafting only. |
| `t1_scout_leggings` | `techguns.item.t1_scout_leggings.name` | Bandit Pants | Crafting: `ccc` / `c_c` / `c_c` (`c` = Heavy Cloth). Crafting only. |
| `t1_scout_boots` | `techguns.item.t1_scout_boots.name` | Bandit Boots | Crafting: `c_c` / `c_c` (`c` = Heavy Cloth). Crafting only. |
| `t1_miner_helmet` | `techguns.item.t1_miner_helmet.name` | Miner's Helmet | Crafting: `iyi` / `c_c` (`i` = "ingotIron" (boots: "nuggetIron"), `c` = Heavy Cloth, `y` = "dyeYellow"). Crafting only. |
| `t1_miner_chestplate` | `techguns.item.t1_miner_chestplate.name` | Miner's Jacket | Crafting: `c_c` / `ccc` / `ici` (`i` = "ingotIron" (boots: "nuggetIron"), `c` = Heavy Cloth, `y` = "dyeYellow"). Crafting only. |
| `t1_miner_leggings` | `techguns.item.t1_miner_leggings.name` | Miner's Pants | Crafting: `ici` / `c_c` / `c_c` (`i` = "ingotIron" (boots: "nuggetIron"), `c` = Heavy Cloth, `y` = "dyeYellow"). Crafting only. |
| `t1_miner_boots` | `techguns.item.t1_miner_boots.name` | Miner's Boots | Crafting: `c_c` / `i_i` (`i` = "ingotIron" (boots: "nuggetIron"), `c` = Heavy Cloth, `y` = "dyeYellow"). Crafting only. |
| `steam,_helmet` (typo) | `techguns.item.steam_helmet.name` | Steam Helmet | Crafting: `sss` / `s_s` (`s` = Steam Armor Plating). Crafting only. Crafted stacks start with `power` 0. |
| `steam_chestplate` | `techguns.item.steam_chestplate.name` | Steam Chestplate | Crafting: `s_s` / `sss` / `sss` (`s` = Steam Armor Plating). Crafting only. Crafted stacks start with `power` 0. |
| `steam_leggings` | `techguns.item.steam_leggings.name` | Steam Legplates | Crafting: `sss` / `s_s` / `s_s` (`s` = Steam Armor Plating). Crafting only. Crafted stacks start with `power` 0. |
| `steam_boots` | `techguns.item.steam_boots.name` | Steam Boots | Crafting: `s_s` / `s_s` (`s` = Steam Armor Plating). Crafting only. Crafted stacks start with `power` 0. |
| `t2_combat_helmet` | `techguns.item.t2_combat_helmet.name` | Combat Helmet | Crafting: `iii` / `h_h` (`i` = "ingotObsidianSteel", `h` = Heavy Cloth). Loot: `techgunsMilBaseBarracks` chest (weight 1 each). |
| `t2_combat_chestplate` | `techguns.item.t2_combat_chestplate.name` | Combat Vest | Crafting: `h_h` / `iii` / `iii` (`i` = "ingotObsidianSteel", `h` = Heavy Cloth). Loot: `techgunsMilBaseBarracks` chest (weight 1 each). |
| `t2_combat_leggings` | `techguns.item.t2_combat_leggings.name` | Combat Pants | Crafting: `ihi` / `i_i` / `h_h` (`i` = "ingotObsidianSteel", `h` = Heavy Cloth). Loot: `techgunsMilBaseBarracks` chest (weight 1 each). |
| `t2_combat_boots` | `techguns.item.t2_combat_boots.name` | Combat Boots | Crafting: `h_h` / `i_i` (`i` = "ingotObsidianSteel", `h` = Heavy Cloth). Loot: `techgunsMilBaseBarracks` chest (weight 1 each). |
| `t2_commando_helmet` | `techguns.item.t2_commando_helmet.name` | Commando Headgear | Crafting: `ihi` / `hgh` (`h` = "itemRubber", `i` = "ingotObsidianSteel", `g` = "paneGlass"). Crafting only. |
| `t2_commando_chestplate` | `techguns.item.t2_commando_chestplate.name` | Commando Vest | Crafting: `h_h` / `hih` / `iii` (`h` = "itemRubber", `i` = "ingotObsidianSteel", `g` = "paneGlass"). Crafting only. |
| `t2_commando_leggings` | `techguns.item.t2_commando_leggings.name` | Commando Pants | Crafting: `hih` / `i_i` / `h_h` (`h` = "itemRubber", `i` = "ingotObsidianSteel", `g` = "paneGlass"). Crafting only. |
| `t2_commando_boots` | `techguns.item.t2_commando_boots.name` | Commando Boots | Crafting: `i_i` / `h_h` (`h` = "itemRubber", `i` = "ingotObsidianSteel", `g` = "paneGlass"). Crafting only. |
| `t2_riot_helmet` | `techguns.item.t2_riot_helmet.name` | Ranger Veteran Helmet | Crafting: `php` / `l_l` (`h` = Combat Helmet) (`p` = "plateLead", `l` = Treated Leather; the center item is the matching T2 Combat piece (any damage and camo)). Crafting only (consumes a T2 Combat piece). |
| `t2_riot_chestplate` | `techguns.item.t2_riot_chestplate.name` | Ranger Veteran Jacket | Crafting: `p_p` / `lcl` / `lpl` (`c` = Combat Vest) (`p` = "plateLead", `l` = Treated Leather; the center item is the matching T2 Combat piece (any damage and camo)). Crafting only (consumes a T2 Combat piece). |
| `t2_riot_leggings` | `techguns.item.t2_riot_leggings.name` | Ranger Veteran Pants | Crafting: `lhl` / `p_p` / `l_l` (`h` = Combat Pants) (`p` = "plateLead", `l` = Treated Leather; the center item is the matching T2 Combat piece (any damage and camo)). Crafting only (consumes a T2 Combat piece). |
| `t2_riot_boots` | `techguns.item.t2_riot_boots.name` | Ranger Veteran Boots | Crafting: `lhl` / `p_p` (`h` = Combat Boots) (`p` = "plateLead", `l` = Treated Leather; the center item is the matching T2 Combat piece (any damage and camo)). Crafting only (consumes a T2 Combat piece). |
| `t3_combat_helmet` | `techguns.item.t3_combat_helmet.name` | Advanced Combat Helmet | Crafting: `ppp` / `f_f` (`p` = "plateCarbon", `f` = "fiberCarbon"). Crafting only. |
| `t3_combat_chestplate` | `techguns.item.t3_combat_chestplate.name` | Advanced Combat Chestplate | Crafting: `p_p` / `ppp` / `fpf` (`p` = "plateCarbon", `f` = "fiberCarbon"). Crafting only. |
| `t3_combat_leggings` | `techguns.item.t3_combat_leggings.name` | Advanced Combat Leggings | Crafting: `pfp` / `p_p` / `f_f` (`p` = "plateCarbon", `f` = "fiberCarbon"). Crafting only. |
| `t3_combat_boots` | `techguns.item.t3_combat_boots.name` | Advanced Combat Boots | Crafting: `f_f` / `p_p` (`p` = "plateCarbon", `f` = "fiberCarbon"). Crafting only. |
| `t3_power_helmet` | `techguns.item.t3_power_helmet.name` | Power Armor Helmet | Crafting: `cpc` / `pgp` (`p` = Power Plating, `g` = "paneGlass", `c` = "circuitElite", `e` = Electric Engine). Crafting only. Crafted stacks start with `power` 0. |
| `t3_power_chestplate` | `techguns.item.t3_power_chestplate.name` | Power Armor Chestplate | Crafting: `p_p` / `ece` / `ppp` (`p` = Power Plating, `g` = "paneGlass", `c` = "circuitElite", `e` = Electric Engine). Crafting only. Crafted stacks start with `power` 0. |
| `t3_power_leggings` | `techguns.item.t3_power_leggings.name` | Power Armor Leggings | Crafting: `pcp` / `e_e` / `p_p` (`p` = Power Plating, `g` = "paneGlass", `c` = "circuitElite", `e` = Electric Engine). Crafting only. Crafted stacks start with `power` 0. |
| `t3_power_boots` | `techguns.item.t3_power_boots.name` | Power Armor Boots | Crafting: `e_e` / `p_p` (`p` = Power Plating, `g` = "paneGlass", `c` = "circuitElite", `e` = Electric Engine). Crafting only. Crafted stacks start with `power` 0. |
| `t3_miner_helmet` | `techguns.item.t3_miner_helmet.name` | Hazardous Environment Helmet | Crafting: `pcp` / `lgl` (`p` = "plateCarbon", `l` = "plateTitanium", `c` = "circuitElite", `g` = "paneGlass"). Crafting only. Crafted stacks start with `power` 0. |
| `t3_miner_chestplate` | `techguns.item.t3_miner_chestplate.name` | Hazardous Environment Chestplate | Crafting: `p_p` / `lcl` / `lpl` (`p` = "plateCarbon", `l` = "plateTitanium", `c` = "circuitElite", `g` = "paneGlass"). Crafting only. Crafted stacks start with `power` 0. |
| `t3_miner_leggings` | `techguns.item.t3_miner_leggings.name` | Hazardous Environment Leggings | Crafting: `ppp` / `c_c` / `l_l` (`p` = "plateCarbon", `l` = "plateTitanium", `c` = "circuitElite", `g` = "paneGlass"). Crafting only. Crafted stacks start with `power` 0. |
| `t3_miner_boots` | `techguns.item.t3_miner_boots.name` | Hazardous Environment Boots | Crafting: `l_l` / `p_p` (`p` = "plateCarbon", `l` = "plateTitanium", `c` = "circuitElite", `g` = "paneGlass"). Crafting only. Crafted stacks start with `power` 0. |
| `t3_exo_helmet` | `techguns.item.t3_exo_helmet.name` | Exo Suit Helmet | Crafting: shapeless: Advanced Combat Helmet (any) + Mechanical Parts T3 + "circuitElite" + Electric Engine + "ingotTitanium" (upgrade of the matching T3 Combat piece (damage and camo discarded)). Crafting only. Crafted stacks start with `power` 0. |
| `t3_exo_chestplate` | `techguns.item.t3_exo_chestplate.name` | Exo Suit Chestplate | Crafting: shapeless: Advanced Combat Chestplate (any) + Mechanical Parts T3 + "circuitElite" + Electric Engine + "ingotTitanium" x2 (upgrade of the matching T3 Combat piece (damage and camo discarded)). Crafting only. Crafted stacks start with `power` 0. |
| `t3_exo_leggings` | `techguns.item.t3_exo_leggings.name` | Exo Suit Leggings | Crafting: shapeless: Advanced Combat Leggings (any) + Mechanical Parts T3 + "circuitElite" + Electric Engine + "ingotTitanium" x2 (upgrade of the matching T3 Combat piece (damage and camo discarded)). Crafting only. Crafted stacks start with `power` 0. |
| `t3_exo_boots` | `techguns.item.t3_exo_boots.name` | Exo Suit Boots | Crafting: shapeless: Advanced Combat Boots (any) + Mechanical Parts T3 + "circuitElite" + Electric Engine + "ingotTitanium" (upgrade of the matching T3 Combat piece (damage and camo discarded)). Crafting only. Crafted stacks start with `power` 0. |
| `t2_beret` | `techguns.item.t2_beret.name` | Beret | Crafting: `cc_` / `l_l` (`c` = Heavy Cloth, `l` = vanilla leather). Crafting only. |

Other loot: the vanilla dungeon chest has no armor pieces (only Nightvision Goggles and Gas Mask, see [special-equipment.md](special-equipment.md)).

## 6. Full-set totals (all bonuses add up per piece; there is no extra full-set bonus)

| Set | Speed | Jump | KB resist | Mining | Underwater mining | Other |
|---|---|---|---|---|---|---|
| T1 Combat | 0 | 0 | 0.40 | 0 | 0 | |
| T1 Scout | 0.50 | 0.16 | 0 | 0 | 0 | fall 0.2/1 |
| T1 Miner | 0.32 | 0.10 | 0 | 0.20 | 0 | fall 0.2/1 |
| Steam (powered) | 0.20 | 0.24 | 0.80 | 0.20 | 0 | +5 HP, step assist, fall 0.2/1 |
| T2 Combat | 0.40 | 0.10 | 0.60 | 0 | 0 | |
| T2 Commando | 0.40 | 0.10 | 0.40 | 0 | 5.0 | gun accuracy 0.20, oxygen gear, fall 0.2/1 |
| T2 Riot | 0.50 | 0.15 | 0.70 | 0 | 0 | |
| T3 Combat | 0.40 | 0.10 | 0.80 | 0 | 0 | |
| T3 Power (powered) | 0.20 | 0.24 | 1.00 | 0.20 | 0 | +5 HP, step, fall 0.2/1 |
| T3 HEV (powered) | 0.40 | 0.51 | 0.60 | 0.40 | 5.0 | water speed 5.0, oxygen gear, cooling, step, fall 0.5/3 |
| T3 Exo (powered) | 0.80 | 0.60 | 0.80 | 0.30 | 0 | step, fall 0.5/5 |

How each bonus is applied is described in set-bonuses.md.

## 7. Camo mechanics

- NBT byte **`camo`** on the stack gives the index into `textureNames`. A missing tag means index 0 for the texture, but the camo *name* shows "No Camo" (`techguns.item.invalidcamo`).
- `onCreated` (crafting or smelting output) writes `camo = 0`. `PoweredArmor.onCreated` also writes `power = 0` and `maxpower = <maxpower>`.
- `switchCamo(stack, back)` steps forward (or back) through the camos with wrap-around and creates the tag if it is missing. It is called by the Camo Bench ([09-machines-utility](../09-machines-utility/index.md)). Sneak-right-click camo switching on the item is present but commented out. The lang key `techguns.message.camoswitch` ("Switched Camo to") is a leftover from it.
- Camo display name key: `techguns.item.<textureNames[0]>.<suffix>camoname.<index>`, where the suffix is empty or `helmet.`.
- Static helpers used by NPC spawning ([10-entities-npcs](../10-entities-npcs/index.md)): `getRandomCamoIndexFor(item)` returns a uniform random index below the camo count, and `getNewWithCamo(item, camo)` returns a new stack with the `camo` byte set.
- Shift tooltip line: `techguns.tooltip.currentcamo` + ": " + camo name.

| Texture base[0] | Index → texture → English camo name |
|---|---|
| `t1_scout` | 0 `t1_scout`, 1 `t1_scout_forest`, 2 `t1_scout_snow`, 3 `t1_scout_black`. **No lang keys exist**, so the raw key is shown. |
| `t1_miner` (body) | 0 Blue, 1 Red, 2 Green, 3 Black |
| `t1_miner` + `helmet.` | 0 Yellow, 1 Lime, 2 Light Blue, 3 Orange (the helmet colors on the same sheets) |
| `steam_armor` | 0 Default, 1 Rusty, 2 Steel (`steam_armor_iron`), 3 Hazard |
| `t2_combat` | 0 Urban, 1 Woodland (`_wood`), 2 Desert, 3 Arctic, 4 SWAT, 5 Security |
| `t2_riotArmor` | 0 only. **No lang key.** |
| `t3_combat` | 0 White, 1 Silver, 2 Dark Green |
| `powerarmor` | 0 Paladin, 1 Outcast (`_dark`), 2 Outcast 2 (`_dark2`) |
| `hevsuit` | 0 Orange, 1 Silver, 2 Black |
| `t3_exo` | 0 White, 1 Silver, 2 Dark Green |
| `beret_texture` | 0 Red, 1 Black, 2 Green |

## 8. Tooltips (`addInformation`)

All text uses the lang keys in index.md §Lang.

**PoweredArmor only (always shown, before everything else):**
- An aqua line `Power: <power>/<maxpower>`. This is hard-coded English. If the stack has no NBT, the tooltip code creates it with power 0.
- An aqua line `PowerType: Steam|RF`. On chestplates it continues ` (<battery item display name>)`. Also hard-coded English.

**Without Shift:**
- `Durability: <max−damage>/<max>`
- Then `Press` + green `[SHIFT]` + gray `for detailed tooltip` (keys `techguns.gun.tooltip.shift1/2/3`).

**With Shift (Left or Right Shift):**
1. `Durability: …`
2. `Armor Values:`
3. If the penetration resistance > 0: dark-gray `Penetration Resistance <pct>%`.
4. Nine resistance lines, each in the form `" <ABBR>: <value> (<pct>%)"` (`formatAV`). The abbreviations and colors, in order, are:
   - `AR` (physical), dark gray
   - `PR` (projectile), gray
   - `EX`, dark red
   - `E` (energy), dark aqua
   - `F` (fire), red
   - `I` (ice), aqua
   - `L` (lightning), yellow
   - `P` (poison), dark green
   - `RAD`, green
5. Bonus lines. These are computed from the *powered* values:
   - `Health Bonus: +<int> Hearts`, **else if** speed > 0: `Move speed: +<speed×100>%`, **else if** jump > 0: `Jump height: +<jump>`. Because of the else-if chain, at most one of these three lines appears (bug [ARM-03](index.md#suspected-bugs)).
   - Then, each shown independently when > 0:
     - `Fall damage: -<x×100>%`
     - `Fall height: -<x>`
     - `Mining speed: +<x×100>%`
     - `Underwater mining speed: +<x×100>%`
     - `Knockback Resistance: +<x×100>%`
     - `Nightvision` (no armor sets this)
     - `Step Assist`
     - `Oxygen Gear`
     - `Cooling System`
6. Multi-camo items add `Current Camo: <name>`.

Numbers are printed with Java's default float `toString` (e.g. `12.5`, `0.30000001` is possible).

## 9. Power system (PoweredArmor)

- **NBT ints:** `power` and `maxpower`. `maxpower` is filled in from the item's `maxpower` whenever it is missing or 0.
- **Ticking:** `calculateConsumptionTick(player)` is called from the player tick handler (END phase, both sides) whenever the player wears at least one Techguns armor piece.
  1. "Moving" = |motionX| > 0.001 or |motionZ| > 0.001.
  2. The chest power type comes from the chestplate (`armorItemInSlot(2)`) if it is a `PoweredArmor`.
  3. For each worn `PoweredArmor` piece **other than the chestplate** whose power type matches the chest's: top it up from the chest's current power, moving at most the piece's missing amount, and deduct the same amount from the chest. Pieces of a different type (or with no powered chest) are never recharged.
  4. On ticks where world time % 20 == 0 and the player is moving, every worn powered piece, chest included, loses 1 power.
  5. If the chestplate is powered and its power is 0, try to consume one **battery item**. Ammo-type consumption checks the Techguns ammo slots first, then the main inventory. On success:
     - The empty-battery item is given back. Any overflow is dropped as an item entity, server side.
     - The chest is set to full.
     - The other pieces are topped up immediately.
- **Action costs** (`applyPowerConsumptionOnAction`): when a bonus lookup is done with "consume = true" and the piece's powered bonus is > 0, power is deducted:

  | Bonus | Piece | Cost | Side effect |
  |---|---|---|---|
  | JUMP | leggings only | 5 | server plays `techguns:effects.steamarmorJump` (vol 0.66, pitch 1.0) at the player, for **every** powered leggings type |
  | FALLDMG | boots only | 5 | only when the fall distance is > 3 |
  | COOLING_SYSTEM | chestplate only | 1 | per fire/lava damage event that was canceled |
  | anything else | – | 0 | |

- A piece with power > 0 uses its powered bonuses. With 0 power it uses its unpowered bonuses, which are all 0 for every registered piece. **Armor values and protection do not depend on power.**
- Batteries and consumables (compressed air tank, energy cell and their empties) are [06-items](../06-items/index.md).
