# Per-mob specification

This file covers each mob in turn. Shared mechanics are described once in the first section and referenced below it. AI task lists are in [ai.md](ai.md), spawning in [spawning.md](spawning.md), and visuals in [rendering-and-models.md](rendering-and-models.md).

Item names refer to fields in [`TGuns`](../02-guns/gun-catalogue.md) and [`TGItems`](../06-items/item-catalogue.md) and to their English lang names. Their stats are owned by [02-guns](../02-guns/index.md), [05-armor](../05-armor/index.md) and [06-items](../06-items/index.md). Armour "camo N" means the NBT byte `camo` = N on a [`GenericArmorMultiCamo`](../05-armor/armor-catalogue.md#1-class-hierarchy) stack (see [05-armor](../05-armor/index.md)). The camo indices used below are:

| Armour set | Camo index → texture variant |
|---|---|
| T1 Scout | 0 default, 1 forest, 2 snow, 3 black |
| T1 Miner | 0 default, 1 red, 2 green, 3 black |
| T2 Combat | 0 default, 1 wood, 2 desert, 3 arctic, 4 swat, 5 security |
| T2 Beret | 0 default, 1 black, 2 green |
| T3 Power Armour | 0 default, 1 dark, 2 dark2 |
| T3 Miner (HEV suit) | 0 default, 1 silver, 2 black |

---

## 0. Shared GenericNPC mechanics (`entities/npc/GenericNPC.java`)

`GenericNPC` extends `EntityMob`. It implements `IRangedAttackMob`, `INPCTechgunsShooter` (muzzle position), [`INpcTGDamageSystem`](../02-guns/damage-system.md#3-interfaces) (Techguns armour per damage type) and `ITGNpcTeam` (faction). Every humanoid TG mob derives from it.

### Lifecycle and equipment
- **Equipment roll.** `onSpawnByManager(difficulty)` calls `addRandomArmor(difficulty)` (each subclass overrides it), then `setCombatTask()`.
  - The spawn manager passes its computed danger level (0 to 5) as `difficulty`.
  - Eggs, spawner blocks and camp flags go through `onSpawnWithEgg`, which runs the vanilla `EntityMob` egg logic and then rolls `difficulty` uniformly in 0 to 3.
  - Only Zombie Pigman Soldier reads `difficulty`.
- **Fallback equipment.** The base `addRandomArmor` gives a random gun and full T1 Combat armour. Every concrete mob overrides it.
  - The gun pick uses `nextInt(4)`: revolver, thompson, AS50 or Grim Reaper, each ¼. The handcannon branch is unreachable.
  - The only mob that uses the base version is the never-materialised `TGDummySpawn`.
- **Combat task.** `setCombatTask()` removes both combat tasks, then adds one depending on the held item:
  - a [`GenericGun`](../02-guns/gun-lifecycle.md) adds the gun's ranged AI (`gun.getAIAttack(this)`) at priority 4;
  - anything else, including empty hands, adds the melee `EntityAIAttackOnCollide` (target class `EntityPlayer`, speed ×1.2, no long memory) at priority 4.
  - It runs in the constructor (server side), after NBT load, and whenever slot 0 (held item) changes.
- **Persistence.** There is no custom NBT, so equipment persists through vanilla equipment saving. These are normal hostile mobs: they despawn like vanilla monsters, including the two mini-bosses. They do **not** burn in daylight.

### Ranged attack (`attackEntityWithRangedAttack`)
If the NPC holds a `GenericGun`, it calls `gun.fireWeaponFromNPC(this, dmgscale = 0.5, accscale)`, where `accscale` depends on difficulty: Easy 1.3, Normal 1.15, Hard 1.0.

Inside `fireWeaponFromNPC` (owned by [02-guns](../02-guns/index.md)):
1. It plays the gun's fire sound at the gun position.
2. It applies a **second** difficulty factor: damage ×0.5 / 0.6 / 0.75 / 0 and accuracy ×1.3 / 1.15 / 1.0 / 1.3 for Easy / Normal / Hard / Peaceful. Turrets always use 1.0 / 1.0.
3. It calls `shootGun` with spread multiplier `gun.zoombonus × accscale × acc` and damage multiplier `dmgscale × dmg`.

The net result for a humanoid NPC is the gun's player damage times 0.25 (Easy), 0.30 (Normal) or 0.375 (Hard). Spread is multiplied by 1.69 / 1.32 / 1.0. Players then also take the [`DamageFactorNPC`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) config factor (default 1.0, see [01-core](../01-core/index.md)).

### Drops
`EntityLivingBase.onDeath` always calls `dropFewItems`; the rare drop requires a recent player kill.

| Source | Rule |
|---|---|
| `dropFewItems` | Rolls `rand(3 + looting)` times (0 to 2+looting). Each roll draws one stack from the mob's loot table (`getRandomItemFromLoottable`; `null` drops nothing). It does not require a player kill. |
| `dropRareDrop` | Drops a **fresh copy** of the held item's `Item`: count 1, default damage, no NBT. Vanilla chance is 2.5% + 0.5% per looting level, on a player kill only. Mobs holding hoes or shovels drop those. |
| Equipment | Vanilla `equipmentDropChances`: 8.5% per slot (+1% per looting), on a player kill only. Damageable items get random wear, so a dropped gun can be partially empty. |
| XP | `experienceValue` (5 by default) plus 1 to 3 per equipped item. |

**Reduced loot tables.** The config [`RestrictedLoottables`](../01-core/config.md#category-general) (general category, default `false`, comment: *"Restricts mob loottables to only thing dropped by vanilla mods or techguns specific items. (No more Coal,Redstone,Iron,Bread,... drops etc)"*) switches the mobs that support it to `reducedLoottable()`. Army Soldier, Commando and Stormtrooper ignore the flag.

In the loot tables below, `y` is a uniform float in [0,1) rolled once per draw, and `round(k·y)` is Java `Math.round`. So "1+round(7y)" ranges from 1 to 8.

### Techguns armour (`getTotalArmorAgainstType`)
This sums the Techguns armour value for the incoming damage type over the four worn armour slots:
- a [`GenericArmor`](../05-armor/armor-catalogue.md#1-class-hierarchy) contributes its per-type value ([05-armor](../05-armor/index.md));
- a vanilla `ItemArmor` contributes `damageReductionAmount`, converted by [`DamageSystem.getArmorAgainstDamageTypeDefault`](../02-guns/index.md#coverage) ([01-core](../01-core/index.md)).

Penetration resistance is 0.

### Sounds (defaults)
| Hook | Sound |
|---|---|
| Idle (`getLivingSound`) | `mob.villager.idle` (vanilla) |
| Hurt | `mob.villager.hit` |
| Death | `mob.villager.death` |
| Step | vanilla block step only |

### Misc
- **Faction:** HOSTILE.
- **Riding offset:** `getYOffset` is the default minus 0.5.
- **Bow pose:** `hasAimedBowAnim = true`, so the arms are raised in the "aimed bow" pose while a gun is held. CyberDemon sets it to false.
- **Muzzle position:** `getWeaponPosX/Y/Z` = (0, 1.6, 0). The client adds this to the muzzle-flash particle offset; see [03-gun-visuals-audio](../03-gun-visuals-audio/index.md).

---

## 1. Zombie Soldier (`ZombieSoldier`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `ZombieSoldier` (global name [`Techguns.ZombieSoldier`](../01-core/lifecycle-and-registration.md#lifecycle-overview)), mod entity ID **27**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.ZombieSoldier` extends `GenericNPC` |
| Lang key / English name | `entity.Techguns.ZombieSoldier.name` = Zombie Soldier |
| Spawn egg | Item `spawnEggZombieSoldier` (unlocalized `spawn_egg_zombiesoldier`), display name hard-coded **"Spawn ZombieSoldier"** ([NPC-19](index.md#suspected-bugs), no lang key); base colour `0x757468`, spot colour `0x38B038`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 5 (the `EntityMob` default) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). |
| Renderer, model, texture | [`RenderGenericNPC`](rendering-and-models.md#2-rendergenericnpc-extends-vanilla-renderbiped) (shadow 0.5), model [`ModelGenericNPC`](rendering-and-models.md#71-modelgenericnpc-extends-modelbiped), texture [`textures/entity/zombie_soldier.png`](textures.md#texturesentityzombie_soldierpng-128128) (128×128). Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | idle `mob.zombie.say`, hurt `mob.zombie.hurt`, death `mob.zombie.death` (all volume 1.0, pitch 0.8 to 1.2 random); step `mob.zombie.step` at 0.15 / 1.0 in addition to the block step |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, [`AttackTime`](../01-core/networking.md#1--gunfiredmessage), `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 25, attack 4, speed 0.25, follow 50.
- **Armour:** each T1 Combat piece (helmet, chest, legs, boots) independently at 50%, default camo.
- **Weapon** (`nextInt(4)`): revolver, thompson, iron shovel, stone shovel, ¼ each. With a shovel it uses melee.
- **Sounds:** idle `mob.zombie.say`, hurt `mob.zombie.hurt`, death `mob.zombie.death`; each step also plays `mob.zombie.step` (volume 0.15, pitch 1).
- **Loot** (`nextInt(5)`):

| Roll | Normal | Reduced (`nextInt(11)`) |
|---|---|---|
| 0 | Heavy Cloth ×1 | Heavy Cloth ×1 |
| 1 | Iron Ingot ×1 | Gunpowder ×1 |
| 2 | Gunpowder ×1 | – (nothing) |
| 3 | Shotgun Shell ×(1+round(7y)) | Rotten Flesh ×1 |
| 4 | 9mm Bullets ×(1+round(2y)) | Rotten Flesh ×1 |
| 5 | – | Shotgun Shell ×(1+round(7y)) |
| 6 | – | 9mm Bullets ×(1+round(2y)) |
| 7–10 | – | nothing |

- **Spawning:** natural, danger level 1, overworld. Also from the "Zombie Hole" spawner block.
- **Gore:** zombie-soldier gib texture with dark-red blood ([04-projectiles-fx](../04-projectiles-fx/index.md)).

## 2. Zombie Farmer (`ZombieFarmer`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `ZombieFarmer` (global name `Techguns.ZombieFarmer`), mod entity ID **28**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.ZombieFarmer` extends `GenericNPC` |
| Lang key / English name | `entity.Techguns.ZombieFarmer.name` = Zombie Farmer |
| Spawn egg | Item `spawnEggZombieFarmer` (unlocalized `spawn_egg_zombiefarmer`), display name hard-coded **"Spawn ZombieFarmer"** (NPC-19, no lang key); base colour `0x757468`, spot colour `0x38B038`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 5 (the `EntityMob` default) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). With a hoe it holds a non-gun item, so the melee task is used. |
| Renderer, model, texture | `RenderGenericNPC` (shadow 0.5), model `ModelGenericNPC`, texture `textures/entity/zombie_soldier.png`. Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | idle `mob.zombie.say`, hurt `mob.zombie.hurt`, death `mob.zombie.death` (all volume 1.0, pitch 0.8 to 1.2 random); step `mob.zombie.step` at 0.15 / 1.0 in addition to the block step |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 18, attack 3, speed 0.20, follow 50.
- **Armour:** one random T1 Miner camo index (0 to 3) is rolled and used for all pieces. Chestplate always; leggings 50%; boots 50%; no helmet.
- **Weapon** (`nextInt(4)`): wooden hoe, iron hoe, stone hoe, handcannon, ¼ each.
- **Sounds:** the zombie set, the same as Zombie Soldier.
- **Loot** (`nextInt(7)`): 0 Heavy Cloth ×1; 1 Wheat Seeds ×(1+round(2y)); 2 Gunpowder; 3 Wheat ×(1+round(2y)); 4 Leather ×(1+round(2y)); 5 Bread ×(1+round(y)); 6 Apple ×(1+round(y)).
- **Reduced loot** (`nextInt(9)`): 0 Heavy Cloth; 1 Wheat Seeds; 2 Gunpowder; 3 Apple; 4–8 nothing.
- **Spawning:** natural, danger level 0, overworld (default weight 200).

## 3. Zombie Miner (`ZombieMiner`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `ZombieMiner` (global name `Techguns.ZombieMiner`), mod entity ID **29**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.ZombieMiner` extends `GenericNPC` |
| Lang key / English name | `entity.Techguns.ZombieMiner.name` = Zombie Miner |
| Spawn egg | Item `spawnEggZombieMiner` (unlocalized `spawn_egg_zombieminer`), display name hard-coded **"Spawn ZombieMiner"** (NPC-19, no lang key); base colour `0x757468`, spot colour `0x38B038`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 5 (the `EntityMob` default) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). With a pickaxe it uses melee. |
| Renderer, model, texture | `RenderGenericNPC` (shadow 0.5), model `ModelGenericNPC`, texture `textures/entity/zombie_soldier.png`. Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | idle `mob.zombie.say`, hurt `mob.zombie.hurt`, death `mob.zombie.death` (all volume 1.0, pitch 0.8 to 1.2 random); step `mob.zombie.step` at 0.15 / 1.0 in addition to the block step |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 20, attack 4, speed 0.20, follow 50.
- **Armour:** a shared random T1 Miner camo. Helmet always; chest, legs and boots 50% each.
- **Weapon:** wooden pickaxe, iron pickaxe, stone pickaxe, handcannon, ¼ each.
- **Sounds:** the zombie set.
- **Loot** (`nextInt(5)`): 0 Heavy Cloth; 1 Coal; 2 Gunpowder; 3 Iron Ingot; 4 Redstone (all ×1).
- **Reduced loot** (`nextInt(9)`): 0 Heavy Cloth; 2 Gunpowder; everything else nothing.
- **Spawning:** natural, danger level 0, overworld (weight 200).

## 4. Zombie Pigman Soldier (`ZombiePigmanSoldier`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `ZombiePigmanSoldier` (global name `Techguns.ZombiePigmanSoldier`), mod entity ID **30**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.ZombiePigmanSoldier` extends `GenericNPC`; `isImmuneToFire = true` |
| Lang key / English name | `entity.Techguns.ZombiePigmanSoldier.name` = Zombie Pigman Soldier |
| Spawn egg | Item `spawnEggZombiePigmanSoldier` (unlocalized `spawn_egg_zombiepigmansoldier`), display name hard-coded **"Spawn ZombiePigmanSoldier"** (NPC-19, no lang key); base colour `0xFF1111`, spot colour `0x770000`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 5 (the `EntityMob` default) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). |
| Renderer, model, texture | `RenderGenericNPC` (shadow 0.5), model `ModelGenericNPC`, texture vanilla `minecraft:textures/entity/zombie_pigman.png`. Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | idle `mob.zombiepig.zpig`, hurt `mob.zombiepig.zpighurt`, death `mob.zombiepig.zpigdeath` (volume 1.0, pitch 0.8 to 1.2); step `mob.zombie.step` at 0.15 / 1.0 |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 25, attack 5, speed 0.30, follow 50, **fire immune**.
- **Armour:** T2 Combat camo 3 ("arctic").
  - Helmet: intended 50%, but a missing brace makes it **always** equipped (bug [NPC-03](index.md#suspected-bugs)).
  - Chest, legs and boots: 50% each.
- **Weapon:** the pool grows with spawn difficulty. Draw `r = nextInt(bound)`:

| Difficulty | bound | thompson | revolver | AK-47 | pistol | flamethrower | combat shotgun | LMG | rocket launcher |
|---|---|---|---|---|---|---|---|---|---|
| 0 | 5 | 3/5 | 2/5 | – | – | – | – | – | – |
| 1 | 9 | 3/9 | 2/9 | 2/9 | 2/9 | – | – | – | – |
| ≥2 | 15 | 3/15 | 2/15 | 2/15 | 2/15 | 2/15 | 2/15 | 1/15 | 1/15 |

- **Sounds:** idle `mob.zombiepig.zpig`, hurt `mob.zombiepig.zpighurt`, death `mob.zombiepig.zpigdeath`, step `mob.zombie.step` (0.15).
- **Loot** (`nextInt(6)`): 0 Steel Plate ×1; 1 Obsidian Steel Ingot ×`nextInt(2)` (can be **0**, bug [NPC-04](index.md#suspected-bugs)); 2 Gold Ingot ×(1–3); 3 Gunpowder ×(1–5); 4 Shotgun Shell ×(1–7); 5 Rifle Bullets ×(1–3). These counts use `1+nextInt(n)`.
- **Reduced loot** (`nextInt(7)`): 0 Gold Nugget; 1 Gunpowder; 2 Shotgun Shell ×(1–7); 3 Rifle Bullets ×(1–3); 4–6 nothing.
- **Spawning:** natural, danger level 0, **Nether only** (dimension −1, weight 100).
- **Rendering:** uses the vanilla `minecraft:textures/entity/zombie_pigman.png`.

## 5. Skeleton Soldier (`SkeletonSoldier`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `SkeletonSoldier` (global name `Techguns.SkeletonSoldier`), mod entity ID **37**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.SkeletonSoldier` extends `GenericNPC` |
| Lang key / English name | `entity.Techguns.SkeletonSoldier.name` = Skeleton Soldier |
| Spawn egg | Item `spawnEggSkeletonSoldier` (unlocalized `spawn_egg_skeletonsoldier`), display name hard-coded **"Spawn SkeletonSoldier"** (NPC-19, no lang key); base colour `0x404040`, spot colour `0xF0F0F0`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 5 (the `EntityMob` default) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). |
| Renderer, model, texture | `RenderGenericNPC` (gun offset (0.1, 0, 0.05), shadow 0.5), model `ModelSkeletonSoldier`, texture vanilla `minecraft:textures/entity/skeleton/skeleton.png`. Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | idle `mob.skeleton.say`, hurt `mob.skeleton.hurt`, death `mob.skeleton.death` (volume 1.0, pitch 0.8 to 1.2); step `mob.skeleton.step` at 0.15 / 1.0, replacing the block step |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 25, attack 4, speed 0.25, follow 50.
- **Armour:** helmet is always one of T1 Combat or T1 Scout (50/50). Boots are always one of T1 Combat or T1 Scout (50/50). No chest or legs.
- **Weapon** (`nextInt(3)`): revolver, thompson, handcannon, ⅓ each. The stone-shovel branch is unreachable.
- **Sounds:** idle `mob.skeleton.say`, hurt `mob.skeleton.hurt`, death `mob.skeleton.death`. Step plays `mob.skeleton.step` (0.15) and **replaces** the block step sound.
- **Loot** (`nextInt(3)`): 0 Bone ×(1+round(y)); 1 Coal ×(1+round(2y)); 2 Gunpowder ×1.
- **Reduced loot** (`nextInt(9)`): 0 Heavy Cloth; 1 Bone; 2 Gunpowder; else nothing.
- **Spawning:** natural, danger level 1, overworld (weight 100).
- **Rendering:** vanilla skeleton texture on the thin-limbed [`ModelSkeletonSoldier`](rendering-and-models.md#72-modelskeletonsoldier-extends-modelgenericnpc). Gore is bone-white with no blood.

## 6. Army Soldier (`ArmySoldier`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `ArmySoldier` (global name `Techguns.ArmySoldier`), mod entity ID **34**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.ArmySoldier` extends `GenericNPC` |
| Lang key / English name | `entity.Techguns.ArmySoldier.name` = Army Soldier |
| Spawn egg | Item `spawnEggArmySoldier` (unlocalized `spawn_egg_armysoldier`), display name hard-coded **"Spawn ArmySoldier"** (NPC-19, no lang key); base colour `0x74806e`, spot colour `0x191512`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 5 (the `EntityMob` default) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). Follow range 75. |
| Renderer, model, texture | `RenderGenericNPC` (shadow 0.5), model `ModelGenericNPC`, texture [`textures/entity/army_soldier.png`](textures.md#texturesentityarmy_soldierpng-128128) (128×128). Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | idle `mob.villager.idle`, hurt `mob.villager.hit`, death `mob.villager.death` (all volume 1.0, pitch 0.8 to 1.2 random); step: block step only |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 30, attack 4, speed 0.30, follow **75**.
- **Armour:**
  - Helmet (66%): of that, 50% T2 Combat helmet (default camo) and 50% T2 Beret camo 2 (green).
  - Chest, legs and boots: T2 Combat, 50% each.
- **Weapon:** M4, combat shotgun, bolt-action rifle, ⅓ each.
- **Sounds:** villager set.
- **Loot** (`nextInt(5)`, no reduced variant): 0 Heavy Cloth; 1 Iron Ingot; 2 Gunpowder; 3 Shotgun Shell ×(1+round(7y)); 4 Rifle Bullets ×(1+round(2y)).
- **Spawning:** not in the natural spawn tables. It comes from Military Camp and Aircraft Carrier camp flags ([11-worldgen](../11-worldgen/index.md)) and from its egg.

## 7. Bandit (`Bandit`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `Bandit` (global name `Techguns.Bandit`), mod entity ID **35**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.Bandit` extends `GenericNPC` |
| Lang key / English name | `entity.Techguns.Bandit.name` = Bandit |
| Spawn egg | Item `spawnEggBandit` (unlocalized `spawn_egg_bandit`), display name hard-coded **"Spawn Bandit"** (NPC-19, no lang key); base colour `0x8f9d59`, spot colour `0x2c3117`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 5 (the `EntityMob` default) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). Follow range 40. |
| Renderer, model, texture | `RenderGenericNPC` (shadow 0.5), model `ModelGenericNPC`, texture [`textures/entity/bandit.png`](textures.md#texturesentitybanditpng-6464) (64×64). Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | inherits the `GenericNPC` villager set: idle `mob.villager.idle`, hurt `mob.villager.hit`, death `mob.villager.death` (all volume 1.0, pitch 0.8 to 1.2 random); step: block step only |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 20, attack 5, speed 0.30, follow 40.
- **Armour:** T1 Scout chest, legs and boots always (default camo); T1 Scout helmet 50%.
- **Weapon** (`nextInt(6)`): pistol, AK-47, bolt-action, sawed-off, thompson, revolver, ⅙ each.
- **Sounds:** villager set.
- **Loot** (`nextInt(6)`): 0 Heavy Cloth; 1 Iron Ingot; 2 Gunpowder; 3 Shotgun Shell ×(1+round(7y)); 4 Rifle Bullets ×(1+round(2y)); 5 9mm ×(1+round(3y)).
- **Reduced loot** (`nextInt(11)`): 0 Heavy Cloth; 1 Gunpowder; 2 Shotgun Shell ×(1+round(7y)); 3 Rifle Bullets ×(1+round(2y)); 4 9mm ×(1+round(3y)); 5–10 nothing.
- **Spawning:** natural, danger level **2**, overworld (weight 50). The config comment calls them "Bandit groups", but each spawn is a single bandit.

## 8. Outcast (`Outcast`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `Outcast` (global name `Techguns.Outcast`), mod entity ID **36**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.Outcast` extends `GenericNPC` |
| Lang key / English name | `entity.Techguns.Outcast.name` = Outcast |
| Spawn egg | Item `spawnEggOutcast` (unlocalized `spawn_egg_outcast`), display name hard-coded **"Spawn Outcast"** (NPC-19, no lang key); base colour `0x101010`, spot colour `0xFF0000`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 5 (the `EntityMob` default) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). |
| Renderer, model, texture | `RenderGenericNPC` (shadow 0.5), model `ModelGenericNPC`, texture `textures/entity/army_soldier.png` (hidden under the power armour). Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | inherits the `GenericNPC` villager set: idle `mob.villager.idle`, hurt `mob.villager.hit`, death `mob.villager.death` (all volume 1.0, pitch 0.8 to 1.2 random); step: block step only |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 30, attack 8, speed 0.20, follow 60.
- **Armour:** full T3 Power Armour. Each piece independently gets camo 1 (dark) or 2 (dark2).
- **Weapon** (`nextInt(7)`): laser gun, Grim Reaper (multi-rocket launcher), minigun, bio gun, flamethrower, tesla gun, LMG, 1/7 each.
- **Sounds:** villager set (inherited).
- **Loot:** no table, so `dropFewItems` yields nothing. It can still drop equipment and the rare held-item drop.
- **Spawning:** egg only. Nothing else spawns it in 0.1.2.

## 9. Commando (`Commando`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `Commando` (global name `Techguns.Commando`), mod entity ID **42**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.Commando` extends `GenericNPC` |
| Lang key / English name | `entity.Techguns.Commando.name` = Commando |
| Spawn egg | Item `spawnEggCommando` (unlocalized `spawn_egg_commando`), display name hard-coded **"Spawn Commando"** (NPC-19, no lang key); base colour `0x191512`, spot colour `0x74806e`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 5 (the `EntityMob` default) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). Follow range 75, speed 0.35. |
| Renderer, model, texture | `RenderGenericNPC` (shadow 0.5), model `ModelGenericNPC`, texture `textures/entity/army_soldier.png` (under the commando armour). Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | idle `mob.villager.idle`, hurt `mob.villager.hit`, death `mob.villager.death` (all volume 1.0, pitch 0.8 to 1.2 random); step: block step only |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 30, attack 5, speed 0.35, follow 75.
- **Armour:** full T2 Commando (helmet, chest, legs, boots).
- **Weapon:** silenced M4 (`TGuns.m4_uq`).
- **Sounds:** villager set.
- **Loot** (`nextInt(10)`, no reduced variant):

| Roll | Drop |
|---|---|
| 0 | Obsidian Steel Ingot ×1 |
| 1 | nothing |
| 2–3 | Rubber Bar ×1 |
| 4–5 | Gunpowder ×1 |
| 6–7 | Rifle Bullets ×(1+round(2y)) |
| 8 | [`TGBlocks.basicMachine`](../07-blocks/block-catalogue.md#registered-blocks) meta 6 = **Explosive Charge** |
| 9 | `TGBlocks.basicMachine` meta 8 = **Advanced Explosive Charge** |

- **Spawning:** Aircraft Carrier camp flag (with Army Soldiers), and egg.

## 10. Stormtrooper (`StormTrooper`, registry name `Stormtrooper`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | [`Stormtrooper`](mob-catalogue.md#registration-javatechgunstgentitiesjava) (global name `Techguns.Stormtrooper`), mod entity ID **43**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.StormTrooper` extends `GenericNPC` (the class name differs in case from the registry name) |
| Lang key / English name | `entity.Techguns.Stormtrooper.name` = Stormtrooper |
| Spawn egg | Item `spawnEggStormtrooper` (unlocalized `spawn_egg_stormtrooper`), display name hard-coded **"Spawn Stormtrooper"** (NPC-19, no lang key); base colour `0xF0F0F0`, spot colour `0x404040`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 5 (the `EntityMob` default) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). Follow range 75. |
| Renderer, model, texture | `RenderGenericNPC` (shadow 0.5), model `ModelGenericNPC`, texture `textures/entity/army_soldier.png` (under the HEV armour). Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | idle `mob.villager.idle`, hurt `mob.villager.hit`, death `mob.villager.death` (all volume 1.0, pitch 0.8 to 1.2 random); step: block step only |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 35, attack 4, speed 0.30, follow 75.
- **Armour:** full T3 Miner (HEV suit) camo 1 (silver).
- **Weapon** (`nextInt(2)`): blaster rifle or laser gun, 50/50.
- **Sounds:** villager set.
- **Loot** (`nextInt(9)`, no reduced variant): 0–1 Plastic Sheet; 2–3 Obsidian Steel Ingot; 4–6 Redstone ×1 (the count formula always gives 1, bug [NPC-05](index.md#suspected-bugs)); 7 Circuit Board; 8 Energy Cell.
- **Spawning:** egg only.

## 11. Cyberdemon (`CyberDemon`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `CyberDemon` (global name `Techguns.CyberDemon`), mod entity ID **33**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.CyberDemon` extends `GenericNPC`; `isImmuneToFire = true`; `hasAimedBowAnim = false` |
| Lang key / English name | `entity.Techguns.CyberDemon.name` = Cyberdemon |
| Spawn egg | Item `spawnEggCyberDemon` (unlocalized `spawn_egg_cyberdemon`), display name hard-coded **"Spawn CyberDemon"** (NPC-19, no lang key); base colour `0xFF1111`, spot colour `0x777777`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 5 (the `EntityMob` default) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). The held Cyberdemon Blaster supplies the ranged AI. |
| Renderer, model, texture | `RenderGenericNPC` (gun offset (-0.11, 0.15, -0.18), shadow 0.75), model `ModelCyberDemon`, texture [`textures/entity/cyberdemon.png`](textures.md#texturesentitycyberdemonpng-128128-fully-opaque-on-a-black-background) (128×128). Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | idle `techguns:npcs.cyberdemonIdle`, hurt `…cyberdemonHurt`, death `…cyberdemonDeath` (volume 1.0, pitch 0.8 to 1.2); step `…cyberdemonStep` at 0.15 / 1.0 after the block step; aggro `…cyberdemonAggro` at 1.0 / 1.0 coded but never reached (NPC-06). Full detail: [sounds.md](sounds.md#event-detail-all-18-events-trigger-call-volume-pitch) |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 30, attack 7, speed 0.35, follow 50, **fire immune**.
- **Armour:** none.
- **Weapon:** Cyberdemon Blaster (`TGuns.cyberdemonBlaster`). `hasAimedBowAnim = false`, so the arms are not raised.
- **Rare drop:** always a Cyberdemon Blaster (2.5% + looting on a player kill), replacing the held-item copy.
- **Sounds:**
  - idle `techguns:npcs.cyberdemonIdle`, hurt `techguns:npcs.cyberdemonHurt`, death `techguns:npcs.cyberdemonDeath`;
  - each step plays `techguns:npcs.cyberdemonStep` at volume 0.15 through `world.playSoundAtEntity`, in addition to the block step sound;
  - an "aggro" sound `techguns:npcs.cyberdemonAggro` (volume 1) is coded in `findPlayerToAttack`, but that legacy hook never runs for AI-enabled mobs, so **it never plays** (bug [NPC-06](index.md#suspected-bugs)).
- **Loot** (`nextInt(20)`): 0 Ghast Tear; 2–5 Blaze Rod; **every other roll** (1, 6–19) Cybernetic Parts ×1, which is 75%.
- **Reduced loot** (`nextInt(2)`): 0 Cybernetic Parts; 1 nothing.
- **Spawning:** natural, danger level 0, **Nether only** (weight 30).
- **Rendering:** custom [`ModelCyberDemon`](rendering-and-models.md#73-modelcyberdemon-extends-modelbiped) (red flesh with cybernetic implants). Gore is green "blood" at scale 1.0.

## 12. Psycho Steve (`PsychoSteve`), early-game mini-boss

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `PsychoSteve` (global name `Techguns.PsychoSteve`), mod entity ID **40**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.PsychoSteve` extends `GenericNPC` and implements `IBossDisplayData` |
| Lang key / English name | `entity.Techguns.PsychoSteve.name` = Psycho Steve |
| Spawn egg | Item `spawnEggPsychoSteve` (unlocalized `spawn_egg_psychosteve`), display name hard-coded **"Spawn PsychoSteve"** (NPC-19, no lang key); base colour `0x757468`, spot colour `0xF0F0F0`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 25 (set in the constructor) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). The chainsaw is a gun with AI range 3 and attack time 10, so it fights at melee range through the ranged AI. Speed 0.60. |
| Renderer, model, texture | `RenderGenericNPC` (shadow 0.5), model `ModelGenericNPC`, texture `textures/entity/army_soldier.png` (under the red miner armour). Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | idle `mob.villager.idle`, hurt `mob.villager.hit`, death `mob.villager.death` (all volume 1.0, pitch 0.8 to 1.2 random); step: block step only; step also plays `mob.zombie.step` at 0.15 / 1.0 |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 75, attack 7, speed **0.60** (very fast), follow 60, XP 25.
- Implements `IBossDisplayData`, but nothing calls `BossStatus`, so no boss bar appears (bug [NPC-22](index.md#suspected-bugs)).
- **Armour:** full T1 Miner camo 1 (red).
- **Weapon:** chainsaw (melee-range gun AI, range 3, attack time 10).
- **Drops:** the rare drop is disabled. `dropFewItems` always drops **Chainsaw ×1** and **Fuel Tank ×(4 + nextInt(5))**, so 4 to 8, regardless of how it died. Equipment can also drop at 8.5%.
- **Sounds:** villager set; step also plays `mob.zombie.step` (0.15).
- **Spawning:** natural, danger level 1, overworld, weight 3 (config comment: *"early game boss, don't set to high value"*).

## 13. Dictator Dave (`DictatorDave`), mini-boss

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `DictatorDave` (global name `Techguns.DictatorDave`), mod entity ID **41**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.DictatorDave` extends `GenericNPC` |
| Lang key / English name | `entity.Techguns.DictatorDave.name` = Dictator Dave |
| Spawn egg | Item `spawnEggDictatorDave` (unlocalized `spawn_egg_dictatordave`), display name hard-coded **"Spawn DictatorDave"** (NPC-19, no lang key); base colour `0x8f9d59`, spot colour `0xA0A000`; creative tab Techguns |
| Size (W×H) / base XP / faction | 0.6×1.8; base XP 35 (set in the constructor) plus 1 to 3 per equipped item; HOSTILE |
| AI summary | [ai.md section 2](ai.md#2-genericnpc-task-list): swim (priority 1), combat task at priority 4 (burst ranged AI from the held gun, see [ai.md section 3](ai.md#3-entityairangedattack-the-gun-driven-shooting-ai), or melee on players), wander (5), watch closest player within 8 blocks (6), look idle (6). Targets: faction-aware hurt-by (1), then nearest player in `followRange` with line of sight (2). Speed 0.40, follow range 60. |
| Renderer, model, texture | `RenderGenericNPC` (shadow 0.5), model `ModelGenericNPC`, texture [`textures/entity/dictator.png`](textures.md#texturesentitydictatorpng-6464) (64×64). Details: [rendering-and-models.md section 1](rendering-and-models.md#1-renderer-bindings). |
| Sounds (volume and pitch) | idle `mob.villager.idle`, hurt `mob.villager.hit`, death `mob.villager.death` (all volume 1.0, pitch 0.8 to 1.2 random); step: block step only; step also plays `mob.zombie.step` at 0.15 / 1.0 |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
- **Stats:** HP 100, attack 7, speed 0.40, follow 60, XP 35.
- **Armour:** T2 Beret camo 0 (default), T1 Combat chestplate, T1 Scout leggings camo 2 (snow), T1 Combat boots.
- **Weapon:** golden revolver.
- **Drops:** rare drop disabled. Always drops **Golden Revolver ×1** and **9mm Bullets ×(8 + nextInt(9))**, so 8 to 16.
- **Sounds:** villager set; step also plays `mob.zombie.step`.
- **Boss bar:** not an `IBossDisplayData`, so there is none.
- **Spawning:** egg only.

## 14. Super Mutants (`SuperMutantBasic`, `SuperMutantHeavy`, `SuperMutantElite`)

**Entity sheet (self-contained summary)**

| Field | Basic | Heavy | Elite |
|---|---|---|---|
| Registry name / entity ID | `SuperMutantBasic` / **44** | `SuperMutantHeavy` / **45** | `SuperMutantElite` / **46** |
| Class | `techguns.entities.npc.SuperMutantBasic` extends `GenericNPC` | `SuperMutantHeavy` extends `SuperMutantBasic` | `SuperMutantElite` extends `SuperMutantBasic` |
| Lang key / English name | `entity.Techguns.SuperMutantBasic.name` = Super Mutant | `entity.Techguns.SuperMutantHeavy.name` = Super Mutant | `entity.Techguns.SuperMutantElite.name` = Super Mutant |
| Spawn egg | `spawnEggSuperMutantBasic`, "Spawn SuperMutantBasic" (NPC-19); base `0xc6a96b`, spot `0x71552e` | `spawnEggSuperMutantHeavy`, "Spawn SuperMutantHeavy"; same colours | `spawnEggSuperMutantElite`, "Spawn SuperMutantElite"; same colours |
| Size (W×H) | 0.6×2.43 (height assigned directly, width unchanged: [NPC-25](index.md#suspected-bugs)) | 0.6×2.70 | 0.6×2.97 |
| Base XP / faction | 5 plus 1 to 3 per equipped item (none are equipped) / HOSTILE | same | same |
| AI summary | The `GenericNPC` task list ([ai.md section 2](ai.md#2-genericnpc-task-list)), ranged AI from the held gun; follow range 50 | same, speed 0.25 | same, speed 0.20 |
| Renderer, model, texture | [`RenderSuperMutant`](rendering-and-models.md#3-rendersupermutant-extends-rendergenericnpc) type 0, [`ModelSuperMutant`](rendering-and-models.md#74-modelsupermutant-extends-modelbiped-tabula-export-credited-pwn3d), `supermutant_texture_1.png`, shadow 0.6, gun offset (-0.1, 0.3, 0) | type 1, `supermutant_texture_2.png`, shadow 0.65, offset (-0.15, 0.3, 0) | type 2, `supermutant_texture_3.png`, shadow 0.7, offset (-0.2, 0.4, 0) |
| Sounds (volume and pitch) | The Cyberdemon set: idle, hurt and death at 1.0 with pitch 0.8 to 1.2; step `techguns:npcs.cyberdemonStep` at 0.15 / 1.0 after the block step; aggro never played (NPC-06) | same | same |
| NBT and DataWatcher | The generic `GenericNPC` set described in section 0: no Techguns keys, vanilla DataWatcher indices only | same | same |

Heavy and Elite extend Basic and only override the numbers below.

| | Basic | Heavy | Elite |
|---|---|---|---|
| HP | 30 | 40 | 50 |
| Attack | 7 | 9 | 11 |
| Speed | 0.30 | 0.25 | 0.20 |
| Follow | 50 | 50 | 50 |
| Model scale (`getModelScale`) | 1.35 | 1.50 | 1.65 |
| Hitbox height (1.8 × scale, set directly; width stays 0.6) | 2.43 | 2.70 | 2.97 |
| Render Y offset (`getModelHeightOffset`) | 0.55 | 0.75 | 0.95 |
| Muzzle pos (X, Y, Z) | (−0.20, 2.2, 0.3) | (−0.20, 2.3, 0.3) | (−0.25, 2.4, 0.3) |
| Vanilla armour (`getTotalArmorValue`) | 7 | 12 | 10 |
| TG armour: explosion, lightning, energy, fire, ice | 10 | 16 | 14 |
| TG armour: physical, projectile | 7 | 12 | 10 |
| TG armour: poison, radiation | 15 | 18 | 16 |
| TG armour: unresistable | 0 | 0 | 0 |
| Texture | `supermutant_texture_1` | `_2` | `_3` |
| Shadow | 0.6 | 0.65 | 0.7 |

- **Weapon** (`nextInt(5)`, the same for all three): rocket launcher ⅕, AK-47 ⅕, combat shotgun ⅕, laser gun ⅖. No armour items.
- **Sounds:** the Cyberdemon set (idle, hurt, death, step 0.15). The aggro hook is dead here too (NPC-06).
- **Loot:** identical to the Cyberdemon (ghast tear, blaze rods, otherwise Cybernetic Parts), including the reduced table. The rare drop is the generic held-item copy.
- **Spawning:** egg only.
- **Names:** all three are "Super Mutant".

## 15. Alien Bug (`AlienBug`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `AlienBug` (global name `Techguns.AlienBug`), mod entity ID **32**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.AlienBug` extends vanilla `EntitySpider`; implements `ITGNpcTeam`, `INpcTGDamageSystem` |
| Lang key / English name | `entity.Techguns.AlienBug.name` = Alien Bug |
| Spawn egg | Item `spawnEggAlienBug` (unlocalized `spawn_egg_alienbug`), display name hard-coded **"Spawn AlienBug"** (NPC-19, no lang key); base colour `0xc6a96b`, spot colour `0x71552e`; creative tab Techguns |
| Size (W×H) / base XP / faction | 1.1×1.2; base XP 5 (`EntityMob`); HOSTILE |
| AI summary | Legacy non-task AI (spider): `findPlayerToAttack` every tick while idle, path to the target, bite at under 2 blocks with a 10-tick timer, no leap. See [ai.md section 7](ai.md#7-alien-bug). |
| Renderer, model, texture | `RenderAlienBug` (shadow 1.0), model [`ModelAlienBug`](rendering-and-models.md#75-modelalienbug-extends-modelbase-tabula-411-export), texture [`textures/entity/alienbug.png`](textures.md#texturesentityalienbugpng-6464-about-34-opaque) (64×64). See [rendering-and-models.md section 4](rendering-and-models.md#4-renderalienbug-extends-renderliving). |
| Sounds (volume and pitch) | idle `techguns:npcs.alienbugIdle`, hurt `…alienbugHurt`, death `…alienbugDeath` (1.0, pitch 0.8 to 1.2); step `…alienbugStep` 0.15 / 1.0 replacing the block step; bite `…alienbugBite` 0.75 / 1.0; aggro `…alienbugAggro` 1.0 / 1.0 (NPC-07) |
| NBT and DataWatcher | NBT: the vanilla living keys; the spider adds no keys of its own. DataWatcher: index 16 (byte) is the vanilla spider flag (bit 0 set while climbing); Techguns adds none. Entity status byte **4** is broadcast on each bite so clients set `attackTimer` to 10 (this is an entity event, not stored data). |

Extends vanilla **`EntitySpider`**, not `GenericNPC`. It uses the legacy (non-AI-task) mob logic. Implements `ITGNpcTeam` (HOSTILE) and `INpcTGDamageSystem`.

- **Stats:** HP 20, attack 4, movement speed **1.0** (spider default 0.8). Size 1.1 × 1.2.
- **Inherited spider traits:** climbs walls; poison immunity; drops string 0–2 (+looting) and, on a player kill, a spider eye at the vanilla chance. From an egg or spawner there is a 1% spider-jockey chance (skeleton rider) and, on Hard, possible potion buffs.
- **Overrides:**
  - **Spawn light:** `isValidLightLevel` always returns true, so it can spawn in any light. In practice it only spawns from the Bug Hole spawner or an egg.
  - **Targeting:** `findPlayerToAttack` takes the closest vulnerable player within 16 blocks and accepts them only if visible. It **ignores daylight**, unlike the spider, so the bug is always aggressive.
    - Whenever such a player exists, even an unseen one, it plays `techguns:npcs.alienbugAggro` (volume 1).
    - The method is polled every tick while the bug has no target, so the sound can repeat every tick (bug [NPC-07](index.md#suspected-bugs)).
  - **Melee:** `attackEntity` replaces the spider leap. When its attack timer is 0, the target is closer than 2 blocks, and the bounding boxes overlap vertically, it calls `attackEntityAsMob`. There is no leap.
  - **Bite:** `attackEntityAsMob` does the normal mob hit and sets `attackTimer = 10`. It plays `techguns:npcs.alienbugBite` (volume 0.75) and broadcasts entity status byte **4** so clients also set the timer to 10. The timer counts down 1 per tick and drives the jaw-snap animation.
- **Sounds:** idle `techguns:npcs.alienbugIdle`, hurt `…alienbugHurt`, death `…alienbugDeath`. Step plays `…alienbugStep` (0.15), replacing the block step sound.
- **TG armour:** energy, explosion, ice, lightning 5; physical 10; projectile 10; poison 20; radiation 20; fire 0; unresistable 0. Penetration resistance 0.
- **XP:** 5 (EntityMob).
- **Gore:** [`ModelGibsAlienBug`](../04-projectiles-fx/death-effects.md#gib-models-clientmodelsmodelgibsjava), 8 parts, alienbug texture, scale 0.8, yellow-green blood (RGB 235, 255, 70); see 04.

### AlienBugDomesticated (dead code)
An `EntityTameable` copy of the bug: HP 20, speed 1.0, same sounds, bite, attack timer and TG armour, faction **NEUTRAL**. `createChild` returns null and it has no AI tasks. Its registration is commented out, but [`RenderAlienBug`](rendering-and-models.md#4-renderalienbug-extends-renderliving) is still bound to the class. It cannot appear in game.

**Entity sheet for `AlienBugDomesticated` (unregistered, dead code)**

| Field | Value |
|---|---|
| Registry name / entity ID | none. `registerModEntityWithEgg(AlienBugDomesticated.class, "AlienBugDomesticated", 0xc6a96b, 0x71552e)` is commented out in [`TGEntities`](mob-catalogue.md#registration-javatechgunstgentitiesjava), so no ID is consumed and no egg exists. The intended egg colours were the same as the Alien Bug (`0xc6a96b`, `0x71552e`). |
| Class | `techguns.entities.npc.AlienBugDomesticated` extends vanilla `EntityTameable`; implements `ITGNpcTeam` (faction NEUTRAL) and `INpcTGDamageSystem` |
| Lang key / English name | none: no `entity.Techguns.AlienBugDomesticated.name` exists |
| Size (W×H), HP, speed | 1.1×1.2; HP 20; movement speed 1.0 (the line that would set the attack damage attribute is commented out) |
| XP | the `EntityLiving` default of 0, because `EntityTameable` is not an `EntityMob` and no value is set |
| Behaviour | `attackEntityAsMob` sets `attackTimer` to 10, plays `techguns:npcs.alienbugBite` (0.75 / 1.0) and broadcasts status byte 4; `onLivingUpdate` counts the timer down. No AI tasks are added and `createChild` returns null, so even if it were registered it would stand still. |
| Renderer, model, texture | `RenderAlienBug` is still bound to the class in [`ClientProxy`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) ([NPC-33](index.md#suspected-bugs)), model `ModelAlienBug`, `textures/entity/alienbug.png`, shadow 1.0 |
| Sounds (volume and pitch) | idle `techguns:npcs.alienbugIdle`, hurt `…alienbugHurt`, death `…alienbugDeath` (1.0, pitch 0.8 to 1.2); step `…alienbugStep` 0.15 / 1.0 via an explicit call |
| TG armour | the same table as the Alien Bug (energy, explosion, ice, lightning 5; physical 10; projectile 10; poison 20; radiation 20; fire 0; unresistable 0) |
| NBT and DataWatcher | `EntityTameable` stores `Owner` (string) and `Sitting` (byte) and uses DataWatcher index 16 (tame/sit flags byte) and 17 (owner name string); the class adds nothing |
| Obtaining | unobtainable: not registered, so no egg, spawner or command can create it |
| Bugs | NPC-33 (renderer bound to an unregistered class) |

## 16. Attack Helicopter (`AttackHelicopter`), boss

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `AttackHelicopter` (global name `Techguns.AttackHelicopter`), mod entity ID **31**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.AttackHelicopter` extends vanilla `EntityFlying`; implements `IMob`, `IBossDisplayData`, `INpcTGDamageSystem` |
| Lang key / English name | `entity.Techguns.AttackHelicopter.name` = Attack Helicopter (defined twice in `en_US.lang`, lines 271 and 275: [NPC-29](index.md#suspected-bugs)) |
| Spawn egg | Item `spawnEggAttackHelicopter` (unlocalized `spawn_egg_attackhelicopter`), display name hard-coded **"Spawn AttackHelicopter"** (NPC-19, no lang key); base colour `0x373d23`, spot colour `0x8ec0d7`; creative tab Techguns |
| Size (W×H) / base XP / faction | 10.0×4.0; base XP 50 (set in the constructor); none (not `ITGNpcTeam`), but `IMob`, so turrets target it |
| AI summary | Legacy Ghast-style `updateEntityActionState`, no task list: waypoint flight, nearest player within 100 blocks, engage within 64 blocks with a 7-round MG burst then a rocket (about 70 ticks per cycle). See [ai.md section 6](ai.md#6-attack-helicopter). |
| Renderer, model, texture | [`RenderHelicopter`](rendering-and-models.md#5-renderhelicopter-extends-rendererlivingentity) (shadow 5.0), model [`models/entity/apache.obj`](textures.md#texturesentityapachepng-256256-opaque) (groups `HELCOPTER`, `ROTOR`, `GUN`) with the placeholder [`ModelNull`](../03-gun-visuals-audio/rendering-framework.md#2-class-hierarchy), texture [`textures/entity/apache.png`](textures.md#texturesentityapachepng-256256-opaque) (256×256). The renderer calls `BossStatus.setBossStatus` every frame. See [rendering-and-models.md section 5](rendering-and-models.md#5-renderhelicopter-extends-rendererlivingentity). |
| Sounds (volume and pitch) | rotor `techguns:npcs.apacheRotor` 6.0 / 1.0 every 61 ticks while alive; burst `…apacheBurst` 5.0 / 1.0; rocket `techguns:guns.rocketFire` 5.0 / 1.0; hurt `…apacheHit` at entity volume 10.0 with pitch 0.8 to 1.2; death sound has no domain (NPC-08); ambient is `""` (NPC-09); death-sequence sounds `…apacheDie` and `…apacheExplode` at 6.0 / 1.0 |
| NBT and DataWatcher | NBT: the vanilla living keys plus `ExplosionPower` (int, written from `explosionStrength`, read only if present as a number; unused by gameplay). DataWatcher: index 16 (byte) is 1 while `attackCounter > 10`, written server-side each tick; the renderer does not read it. Vanilla indices 0 and 6 to 11 as usual. |

Extends `EntityFlying` (Ghast-style, legacy AI) and implements `IMob`, `IBossDisplayData` and `INpcTGDamageSystem`. It is **not** `ITGNpcTeam`.

- **Stats:** HP 100, size **10 × 4**, XP 50, sound volume 10.
- **Boss bar:** the renderer calls `BossStatus.setBossStatus(entity, true)` every frame, so a boss health bar shows while it is rendered.
- **Peaceful:** the helicopter removes itself.
- **Despawning:** vanilla `despawnEntity` runs, so it can despawn like a normal mob.
- **Flight and attack AI:** see [ai.md § Attack Helicopter](ai.md#6-attack-helicopter).
- **Damage:**
  - a `"fireball"` damage source from a player (a reflected ghast fireball) deals 1000, an instant kill. This is a Ghast leftover;
  - TG armour: explosion 5; lightning and energy 10; fire, ice, physical, projectile, poison and radiation 20; unresistable 0;
  - penetration resistance 0.4 against PROJECTILE damage, 0 otherwise.
- **Drops** (`dropFewItems`, on death, independent of looting): Rifle Bullets ×(16 + nextInt(16)); Rocket ×(8 + nextInt(8)); Cybernetic Parts ×(2 + nextInt(2)); **Iron Block** ×(4 + nextInt(4)). The coded `getDropItem` (gunpowder) is unused.
- **Death sequence** (`onDeathUpdate`, 100 ticks instead of vanilla 20):
  - **Tick 0:** play `techguns:npcs.apacheDie`, volume 6.
  - **Every 5 ticks:** one `LargeSmoke` particle at a random point in a box of ±width horizontally and 0 to height vertically.
  - **Every 6 ticks:** three `Fireblast` particles at one random angle on a circle of radius 1 around the centre, at height `posY − 10·(t/100)²`. Each has upward velocity r (r random in 0.5 to 1) and outward velocity r × {1, 0.75, 1.25}.
  - **Tick 100:** if a player recently hit it, XP orbs are dropped. Then the entity is removed and `techguns:npcs.apacheExplode` plays (volume 6), with 10 × (`ExplosionHuge` + `LargeSmoke`) particles in a box of ±width horizontally and `posY − 10 … posY − 10 + 2·height` vertically.
  - These particles go through `Techguns.proxy.spawnParticle` and only appear client-side. The renderer animates the crash (see rendering).
- **Sounds:**
  - rotor `techguns:npcs.apacheRotor` at volume 6 every 61 ticks while alive;
  - hurt `techguns:npcs.apacheHit`;
  - death sound string `npcs.apacheHit` is missing its domain, so it resolves to a nonexistent vanilla sound (bug [NPC-08](index.md#suspected-bugs));
  - ambient is the empty string `""` (bug [NPC-09](index.md#suspected-bugs)).
- **NBT:** `ExplosionPower` (int, default 1). It is saved and loaded but unused (Ghast leftover).
- **DataWatcher:** index 16 (byte) is 1 while `attackCounter > 10`. The renderer does not use it.
- **Natural-spawn guards** (`getCanSpawnHere`: 1/20 chance, not Peaceful; max 1 per chunk) are moot, because it is never added to any spawn list. It comes from its egg only.

## 17. Turret (`NPCTurret`, registry name `SimpleTurret`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | [`SimpleTurret`](mob-catalogue.md#registration-javatechgunstgentitiesjava) (global name `Techguns.SimpleTurret`), mod entity ID **38**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.NPCTurret` extends `EntityCreature`; implements `IAnimals`, `IRangedAttackMob`, `INPCTechgunsShooter`, `ITGNpcTeam` |
| Lang key / English name | `entity.Techguns.SimpleTurret.name` = Turret |
| Spawn egg | none: registered with `registerModEntity`, not `registerModEntityWithEgg`, so there is no egg |
| Size (W×H) / base XP / faction | 0.6×1.0 (height forced; eye height 0.8); XP 0 (`onDeath` skips the vanilla path, so none is awarded); TURRET |
| AI summary | Tasks: ranged AI from the base's gun (1), [`TurretEntityAIWatchClosest`](ai.md#turretentityaiwatchclosest) (2), look idle (3); targets: faction hurt-by (1), [`TurretEntityAINearestAttackableTarget`](ai.md#turretentityainearestattackabletarget) with [`TurretTargetSelector`](ai.md#turrettargetselector-the-target-filter) (2). See [ai.md section 5](ai.md#5-turret-npcturret). |
| Renderer, model, texture | [`RenderSimpleTurret`](rendering-and-models.md#6-rendersimpleturret-extends-plain-render-for-npcturret), model [`ModelTurret`](../07-blocks/block-rendering-and-models.md#modelturret-and-modelsimpleturret-uv-6464--6432) (`client/models/blocks/`), texture [`textures/blocks/TurretBase.png`](../07-blocks/textures.md#11-model-textures) or `TurretBase_2` to `_5.png` by armour level (128×128, owned by [09-machines-utility](../09-machines-utility/index.md)); no shadow. Tracking range **120** instead of 80. |
| Sounds (volume and pitch) | idle none (`null`); hurt `mob.irongolem.hit` and death `mob.irongolem.death` (volume 1.0, pitch 0.8 to 1.2); step suppressed. The turret sounds `npcs.turretBeep` and `npcs.turretGunequip` are defined but never played ([NPC-27](index.md#suspected-bugs)). |
| NBT and DataWatcher | None written: the entity's `writeToNBT`/`readFromNBT` overrides only call super (the old position-link code is commented out). The turret is re-linked to its base by position every tick while unlinked. `canDespawn` is false. Vanilla DataWatcher indices only. |

This is the gun-wielding head that sits on a Turret base block ([`BasicMachine`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) meta 2). The block, its GUI, power, ammo, repair and security settings are owned by **[09-machines-utility](../09-machines-utility/index.md)**. This section covers the entity side.

It extends `EntityCreature` and implements `IAnimals`, `IRangedAttackMob`, `INPCTechgunsShooter` and `ITGNpcTeam`. Its faction is **TURRET**.

- **Stats:** HP 30, movement 0, knockback resistance 1.0, follow range 20 (replaced by the gun's AI range once a gun is fitted). Height is forced to 1.0 (width 0.6) and eye height is 0.8. It never despawns.
- **Position lock:** after every `onUpdate` the turret resets its position to its previous-tick position, so it cannot be moved.
- **Weapon:** `getHeldItem()` returns the gun in the linked [`TurretBaseEnt`](../09-machines-utility/turret.md#tile-entity-turretbaseent) (`mountedTileEnt.getWeapon()`), or null if unlinked.
- **Armour:** `getTotalArmorValue()` returns the base's turret-armour value: 0, 5 (Iron Turret Armor), 10 (Steel), 15 (Obsidian Steel) or 20 (Carbon). Vanilla armour maths applies.
- **Firing:** when the ranged AI fires, the turret checks it holds a `GenericGun` **and** that the base can `consumeAmmo()`. Only then does it call `fireWeaponFromNPC(this, 1.0, 1.0)`, giving full damage and accuracy. Against players, the [`DamageTurretToPlayer`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) config factor (default 0.5) applies ([01-core](../01-core/index.md)).
- **Muzzle pos:** (0.05, 0.7, 0.8), plus the gun's turret offsets ([02](../02-guns/index.md)/[03](../03-gun-visuals-audio/index.md)).
- **Sounds:** no ambient sound; hurt `mob.irongolem.hit`; death `mob.irongolem.death`; no step sound.
- **Death:** `onDeath` does **not** call super. It calls `mountedTileEnt.onTurretDeath()` and the base starts its repair timer ([09](../09-machines-utility/index.md)). This throws an NPE if the turret is unlinked (bug [NPC-12](index.md#suspected-bugs)). There are no drops, no rare drop, and 0 XP. The vanilla 20-tick death removal still runs.
- **`destroy()`:** sets health to 0 and kills the entity. The base calls it when the block is broken.
- **Linking:** see [ai.md § Turret](ai.md#5-turret-npcturret).
- **AI enable/disable:** `disable()` removes every AI task and sets `active = false`. The base does this when it is unpowered or redstone-disabled. Re-enabling calls `setAITasks()` and `setCombatTask()`.
- **NBT:** none of its own. The link is re-established by position (see ai.md).

## 18. TGDummySpawn (`TGDummySpawn`)

**Entity sheet (self-contained summary)**

| Field | Value |
|---|---|
| Registry name / entity ID | `TGDummySpawn` (global name `Techguns.TGDummySpawn`), mod entity ID **39**; tracking range 80, update frequency 3, no velocity updates |
| Class | `techguns.entities.npc.TGDummySpawn`, an empty subclass of `GenericNPC` |
| Lang key / English name | `none (no `entity.Techguns.TGDummySpawn.name` exists; a client that displayed its name would show the raw key)` = none: registered without an egg |
| Spawn egg | 0.6×1.8 |
| Size (W×H) / base XP / faction | not relevant (never lives long enough to die); HOSTILE (inherited); None in practice: the spawn manager replaces it on join and cancels the join (see [spawning.md section 1.2](spawning.md#12-replacement)). If it did exist it would run the `GenericNPC` task list. |
| AI summary | `RenderGenericNPC` with `ModelGenericNPC` and vanilla `zombie.png` (the `GenericNPC` fallback binding); never visible. |
| Renderer, model, texture | inherits the villager set but never plays any, because it never exists in a loaded world |
| Sounds (volume and pitch) | Nothing is ever saved: the entity is cancelled in `EntityJoinWorldEvent` before it is added to a chunk. |
| NBT and DataWatcher | No Techguns keys. `GenericNPC` overrides the NBT hooks only to re-run `setCombatTask` after loading, so the AI always matches the loaded held item. Saved by vanilla: `Health`, `HurtTime`, `DeathTime`, `AttackTime`, `AbsorptionAmount`, `Attributes`, `ActiveEffects`, `Equipment` (held item then four armour slots, each stack keeping its own NBT such as `camo`), `DropChances`, `CanPickUpLoot`, `PersistenceRequired`, `Leash`/`Leashed`, plus Forge `CustomName` and `CustomNameVisible`. DataWatcher: only the vanilla indices (0 flags, 6 health, 7 potion colour, 8 potion ambient, 9 arrow count, 10 custom name, 11 name visible); Techguns adds none. |
| Registered spawn entries | Added to vanilla monster spawning in `TGEntities.postInit`: weight `spawnWeightTGOverworld` (600) in all non-Nether, non-End biomes and `spawnWeightTGNether` (300) in Nether-type biomes, group size 1 to 2; see [spawning.md](spawning.md) |
An empty subclass of `GenericNPC`. It is the placeholder that vanilla natural spawning creates. A server-side `EntityJoinWorldEvent` handler intercepts it the moment it is added to the world, replaces it with a real NPC and cancels the join, so it never exists in the world. See [spawning.md](spawning.md).

### TGDummySpawn lifecycle in detail

| Step | Side | What happens |
|---|---|---|
| 1. Eligibility | server | Vanilla `SpawnerAnimals` picks a spot for creature type `monster` and a biome entry for `TGDummySpawn`. Because the dummy is an `EntityMob`, the usual light-level, solid-floor, free-space and not-Peaceful checks apply, and each attempt creates a group of 1 to 2 dummies. Weights: `spawnWeightTGOverworld` (default 600) in every non-Nether, non-End biome, and `spawnWeightTGNether` (default 300) in Nether-type biomes. |
| 2. Construction | server | The dummy is built by its `(World)` constructor, so it gets the full `GenericNPC` constructor work (attributes, AI tasks, combat task). Its attributes are therefore the `GenericNPC` defaults (the catalogue lists HP 20), and it holds no weapon or armour because `onSpawnByManager` is not called on it. |
| 3. Interception | server | `onEntityJoinWorld` sees a `TGDummySpawn` and calls [`TGSpawnManager.handleSpawn`](spawning.md#11-vanilla-hook), then cancels the join. The dummy is never added to a chunk, never sent to clients, never ticks and is never saved. |
| 4. Replacement | server | A real NPC class is chosen by danger-weighted table and spawned at the dummy's position and yaw ([spawning.md section 1.2](spawning.md#12-replacement)). It counts toward the vanilla monster cap as an `IMob`; the dummy itself does not. |
| 5. Failure paths | server | Total weight 0 (other dimensions, [NPC-16](index.md#suspected-bugs)) prints a log line and spawns nothing. A reflection error is printed. Both leave the dummy marked dead, so vanilla simply wastes the attempt. |

The dummy has no loot table, no sounds that anyone can hear, no renderer output (the `GenericNPC` fallback binding exists only because the class is a `GenericNPC`), no lang key, and no egg. Because it is only ever used to borrow vanilla's spawn selection, its inherited `addRandomArmor` (the revolver, thompson, AS50 or Grim Reaper pool with T1 Combat armour) never runs.

## 19. TurretEntityUNUSED (dead code)
An `EntityLiving` prototype turret: HP 10, follow range 0, speed 0, knockback resistance 1.
- Every 21 idle ticks it scans a box of ±20 horizontally and ±5 vertically for `EntityLiving`, and picks the nearest one that a block ray-trace "sees". The logic is broken: it compares a block ray-trace result to `ENTITY`.
- The aiming code computes vectors and does nothing with them.
- It is never registered, and its renderer binding is commented out.

**Entity sheet for `TurretEntityUNUSED` (dead code)**

| Field | Value |
|---|---|
| Registry name / entity ID | none. The `registerModEntity(TurretEntityUNUSED.class, "Turret", ...)` line in `Techguns.java` is commented out, so no ID is consumed. |
| Class | `techguns.entities.TurretEntityUNUSED` extends `EntityLiving` |
| Lang key / English name | none (the intended registry name `Turret` would have mapped to `entity.Techguns.Turret.name`, which does not exist) |
| Spawn egg | none |
| Size / XP / faction | default `EntityLiving` size; XP 0; not `ITGNpcTeam` |
| AI summary | A hand-written scan every 21 idle ticks (box of ±20 horizontally and ±5 vertically for `EntityLiving`), which does not work (see above) |
| Renderer, model, texture | [`RenderTurret`](index.md#coverage) exists in `entities/renderer` and uses one static texture, but the binding in `ClientProxy` is commented out |
| Sounds | none |
| NBT and DataWatcher | none of its own; vanilla living keys only |
| Obtaining | unobtainable (not registered) |
