# 02-guns: Entity sheets: handguns, revolvers, SMGs and akimbo guns

Each item below has a full entity sheet with fields 1 to 10 of the [conventions](../CONVENTIONS.md#entity-sheet): identity, obtaining, numbers, behavior, persistence and sync, UI, visuals, audio, relationships and bugs. The catalog tables ([gun-catalog.md](gun-catalog.md)) remain the quick reference; shared mechanics are in [gun-lifecycle.md](gun-lifecycle.md), the shared rules for these sheets are in [sheets-index.md](sheets-index.md), and class-level detail is in [per-gun-behavior.md](per-gun-behavior.md).

Items in this file: [Hand Cannon](#hand-cannon-handcannon), [9mm Pistol](#9mm-pistol-pistol), [Submachine Gun (Thompson)](#submachine-gun-thompson-thompson), [Revolver](#revolver-revolver), [Golden Revolver](#golden-revolver-goldenrevolver), [Mac10](#mac10-mac10), [Akimbo Pistols](#akimbo-pistols-pistolakimbo), [Akimbo Revolvers](#akimbo-revolvers-revolverakimbo), [Akimbo Golden Revolvers](#akimbo-golden-revolvers-goldenrevolverakimbo), [Akimbo Mac10s](#akimbo-mac10s-mac10akimbo), [Vector](#vector-vector).

### Hand Cannon (`handcannon`)

**1. Identity.**

- Catalog row 1 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `Handgun` (`java/techguns/items/guns/`).
- Registry name `Techguns:handcannon` (`GameRegistry.registerItem(..., "handcannon")`); unlocalized name `techguns.item.handcannon`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 1). Creative tab [`Techguns`](../01-core/lifecycle-and-registration.md#lifecycle-overview) (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Hand Cannon", lang key `techguns.item.handcannon.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 1 [empty only (vanilla shaped)]: grid `b f s` with b = Stone Barrel, f = Flint and Steel, s = Wooden Stock.
- The recipe outputs item damage = clip, which `onCreated` turns into **0 rounds**: the crafted gun is empty and must be loaded.
- Loot ([`TGItems`](../06-items/item-catalog.md) chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): vanilla bonus chest (`ChestGenHooks.BONUS_CHEST`): count 1, weight 10; `techgunsWeaponchestT1`: count 1, weight 2. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.
- NPC equipment (not obtainable directly): [`ZombieFarmer`](../10-entities-npcs/per-mob.md#2-zombie-farmer-zombiefarmer) (1 of 4 weapon rolls; the other three are hoes); [`ZombieMiner`](../10-entities-npcs/per-mob.md#3-zombie-miner-zombieminer) (1 of 4 weapon rolls; the other three are pickaxes); [`SkeletonSoldier`](../10-entities-npcs/per-mob.md#5-skeleton-soldier-skeletonsoldier) (1 of 3 weapon rolls). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 8.0 per projectile |
| Minimum damage | 5.0 |
| Drop-off | full damage up to 10 blocks, linear to the minimum at 25 blocks, minimum beyond |
| Time to live / speed | 25 ticks / 1.0 blocks per tick (fixed), gravity 0.015; nominal reach 25 blocks |
| Accuracy (spread) | 0.035, an independent uniform random offset of +-1.4 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 12 ticks (1.67 per second) |
| Reload | 30 ticks (1.5 s); the gun refills at once and the player-wide `fireDelay` is set to 18 ticks (reload minus `minFiretime`) |
| Clip | 1 round |
| Ammo per reload | 1 x Stone Bullet (shared item meta 0) |
| Empty returned | none |
| Forced-reload refund | none: the remaining rounds are discarded when a forced reload is used on a magazine-less gun (`GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 12 blocks, attack time 60 ticks (variance 20), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.handcannon` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [8.0, 5.0, 10, 25, 25, 0.035, 0.0]; indices all honored |
| Other config keys | [`DamageFactorNPC`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (1.0), [`DamageTurretToPlayer`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (0.5), [`DamagePvP`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (0.5), [`ArmorPenetrationMultiplierNPCtoPlayer`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) and [`ArmorPenetrationMultiplierPvP`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (1.0) scale hits; [`ForceSafeModeForNonOps`](../01-core/config.md#category-general) (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends [`PacketShootGun`](../01-core/networking.md#8--packetshootgun) and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (12), broadcasts [`GunFiredMessage`](../01-core/networking.md#1--gunfiredmessage) within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- `Handgun` overrides `spawnProjectile` only. It fires a [`SimpleProjectile`](../04-projectiles-fx/projectiles.md#31-simpleprojectile) whose speed is fixed at 1.0 blocks/tick (the shared `speed` field of 1.5 is ignored), with gravity 0.015 so the shot drops, scale 1.0 and the sprite `techguns:textures/entity/handgunBullet.png`. The `damagebonus` (NPC scaling), drop-off and penetration are applied; block damage follows the shooter's safe-mode flag.
- Clip 1: every shot empties the gun, and the next trigger pull performs the automatic reload (consumes one Stone Bullet, refills one round). The shot lockout is 12 ticks and the reload lockout 18 ticks, so one full shoot-and-reload cycle takes 30 ticks (1.5 s).
- No empty container and no forced-reload refund. It is the icon item of the Techguns creative tab.
- The default equipment routine of [`GenericNPC`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) lists the Hand Cannon as its fifth case, but its random roll only produces 0 to 3, so that branch never runs (`GUN-48`).
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 1) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 1, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 1). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); [`ReloadStartedMessage`](../01-core/networking.md#3--reloadstartedmessage) (server to clients within 100 blocks: 1500 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in [`TechgunsExtendedPlayerProperties`](../01-core/player-state.md#techgunsextendedplayerproperties): `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Stone Bullet`
  - `Damage: 8.0-5.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: Stone Bullet`
  - `DamageType: Projectile`
  - `Damage: 8.0-5.0`
  - `Range: 10,25,25`
  - `Spread: 0.035`
  - `Clipsize: 1`
  - `ReloadTime: 1.5s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`SimpleProjectile` (entity id 2)](../04-projectiles-fx/projectiles.md#31-simpleprojectile). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderHandgun` with model [`ModelHandgun`](../03-gun-visuals-audio/gun-models.md#modelhandgun-hand-cannon-6432-family-x-14-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#hand-cannon-handcannon)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/handgun.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×32, average #615E55, dominant #B0B0B0 26%, #101010 20%, #707070 14%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashExp` at offset (0, 0, 0.2); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.handgunFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.71 to 2.29 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.handgunReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 1.32 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Stone Bullet (meta 0), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`ZombieFarmer`](../10-entities-npcs/per-mob.md#2-zombie-farmer-zombiefarmer) (1 of 4 weapon rolls; the other three are hoes); [`ZombieMiner`](../10-entities-npcs/per-mob.md#3-zombie-miner-zombieminer) (1 of 4 weapon rolls; the other three are pickaxes); [`SkeletonSoldier`](../10-entities-npcs/per-mob.md#5-skeleton-soldier-skeletonsoldier) (1 of 3 weapon rolls). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot ([`SlotTechgun`](../09-machines-utility/turret.md)); [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#hand-cannon-handcannon); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-48`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### 9mm Pistol (`pistol`)

**1. Identity.**

- Catalog row 2 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class [`GenericGun`](gun-lifecycle.md) (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:pistol` (`GameRegistry.registerItem(..., "pistol")`); unlocalized name `techguns.item.pistol`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 18). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "9mm Pistol", lang key `techguns.item.pistol.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 2 [loaded]: grid `b s p` / `· f p` / `· · m` with b = Hardened Steel Barrel, s = "plateSteel", p = Plastic Sheet, f = Mechanical Parts (Obsidian Steel), m = 9mm Pistol Magazine; recipe 2e [empty]: grid same with m = 9mm Pistol Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot (`TGItems` chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): vanilla dungeon chest: count 1, weight 1; `techgunsWeaponchestT2`: count 1, weight 1; `techgunsWeaponchestT3`: count 1, weight 1. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.
- NPC equipment (not obtainable directly): [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (2 of 6: cases 0 and default); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (2 of 15 at full difficulty, 2 of 9 at difficulty 1, never at difficulty 0). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 8.0 per projectile |
| Minimum damage | 5.0 |
| Drop-off | full damage up to 18 blocks, linear to the minimum at 25 blocks, minimum beyond |
| Time to live / speed | 30 ticks / 1.5 blocks per tick; nominal reach 45 blocks |
| Accuracy (spread) | 0.025, an independent uniform random offset of +-1 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 4 ticks (max 5 per second) |
| Reload | 35 ticks (1.75 s); the gun refills at once and the player-wide `fireDelay` is set to 31 ticks (reload minus `minFiretime`) |
| Clip | 18 rounds |
| Ammo per reload | 1 x 9mm Pistol Magazine (shared item meta 2) |
| Empty returned | 1 x 9mm Pistol Magazine (Empty) (shared item meta 3) when a reload consumes ammo |
| Forced-reload refund | 9mm Bullets x floor(rounds left / 6) (6 rounds per item) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 3 ticks (150 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 30 ticks (variance 10), burst 3 shots, burst delay 10 ticks |
| Config key | `WeaponStats_techguns.item.pistol` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [8.0, 5.0, 18, 25, 30, 0.025, 0.0]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (4), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Plain `GenericGun`: no class override. Semi-auto, so each click fires one round and a click during the lockout is swallowed (`GUN-29`).
- The builder sets damage drop-off twice and the later call wins (18 to 25 blocks, minimum 5, `GUN-16`).
- One 9mm Pistol Magazine holds 18 rounds and is worth 3 9mm Bullets in a forced-reload refund (6 rounds per bullet item). Two pistols can be crafted into Akimbo Pistols.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 18) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 18, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 18). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 150 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 1750 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: 9mm Pistol Magazine`
  - `Damage: 8.0-5.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: 9mm Pistol Magazine`
  - `DamageType: Projectile`
  - `Damage: 8.0-5.0`
  - `Range: 18,25,30`
  - `Spread: 0.025`
  - `Clipsize: 18`
  - `ReloadTime: 1.75s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderPistol` with model [`ModelPistol3`](../03-gun-visuals-audio/gun-models.md#modelpistol3-pistol-declared-6432-used-with-a-12864-png-family-z-9-boxes-2-parts) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#pistol-pistol)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/pistol3.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×64, average #252323, dominant #303030 42%, #101010 40%, #301010 6%, 20% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.05, -0.45); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.pistolFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.14 s, 44100 Hz, stereo, sounds.json category `player` |
| `guns.pistolReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 1.81 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: 9mm Pistol Magazine (meta 2), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (2 of 6: cases 0 and default); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (2 of 15 at full difficulty, 2 of 9 at difficulty 1, never at difficulty 0). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Recipes and machines: Input of the Akimbo Pistols combine recipe.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#pistol-pistol); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-16`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Submachine Gun (Thompson) (`thompson`)

**1. Identity.**

- Catalog row 12 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:thompson` (`GameRegistry.registerItem(..., "thompson")`); unlocalized name `techguns.item.thompson`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 20). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Submachine Gun", lang key `techguns.item.thompson.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 5 [loaded]: grid `b f s` / `· m ·` with b = Iron Barrel, f = Iron Receiver, s = Wooden Stock, m = SMG Magazine; recipe 5e [empty]: grid same with m = SMG Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot (`TGItems` chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): vanilla dungeon chest: count 1, weight 2; `techgunsWeaponchestT1`: count 1, weight 2; `techgunsWeaponchestT2`: count 1, weight 1. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.
- NPC equipment (not obtainable directly): [`ZombieSoldier`](../10-entities-npcs/per-mob.md#1-zombie-soldier-zombiesoldier) (1 of 4); [`SkeletonSoldier`](../10-entities-npcs/per-mob.md#5-skeleton-soldier-skeletonsoldier) (1 of 3); [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (1 of 6); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (3 of 15 at full difficulty, 3 of 9 at difficulty 1, 3 of 5 at difficulty 0). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 5.0 per projectile |
| Minimum damage | 3.0 |
| Drop-off | full damage up to 15 blocks, linear to the minimum at 24 blocks, minimum beyond |
| Time to live / speed | 25 ticks / 1.5 blocks per tick; nominal reach 37.5 blocks |
| Accuracy (spread) | 0.05, an independent uniform random offset of +-2 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 3 ticks (6.67 per second) |
| Reload | 40 ticks (2 s); the gun refills at once and the player-wide `fireDelay` is set to 37 ticks (reload minus `minFiretime`) |
| Clip | 20 rounds |
| Ammo per reload | 1 x SMG Magazine (shared item meta 5) |
| Empty returned | 1 x SMG Magazine (Empty) (shared item meta 6) when a reload consumes ammo |
| Forced-reload refund | 9mm Bullets x floor(rounds left / 10) (10 rounds per item) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 18 blocks, attack time 45 ticks (variance 15), burst 3 shots, burst delay 3 ticks |
| Config key | `WeaponStats_techguns.item.thompson` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [5.0, 3.0, 15, 24, 25, 0.05, 0.0]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (3), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Plain full-auto `GenericGun`. The registry and unlocalized name is [`thompson`](gun-catalog.md#table-a-identity-and-ammunition), the English name is "Submachine Gun". It shares the SMG Magazine (20 rounds here, worth 2 9mm Bullets) with the Mac10 and Vector.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 20) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 20, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 20). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: SMG Magazine`
  - `Damage: 5.0-3.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: SMG Magazine`
  - `DamageType: Projectile`
  - `Damage: 5.0-3.0`
  - `Range: 15,24,25`
  - `Spread: 0.05`
  - `Clipsize: 20`
  - `ReloadTime: 2.0s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderThompson` with model [`ModelThompson`](../03-gun-visuals-audio/gun-models.md#modelthompson-6464-family-x-24-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#thompson-thompson)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/thompson.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×64, average #271E15, dominant #101010 60%, #505050 13%, #705010 12%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.05, 0); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.thompsonFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.01 s, 192000 Hz, mono, sounds.json category `player` |
| `guns.thompsonReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 1.76 s, 48000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: SMG Magazine (meta 5), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`ZombieSoldier`](../10-entities-npcs/per-mob.md#1-zombie-soldier-zombiesoldier) (1 of 4); [`SkeletonSoldier`](../10-entities-npcs/per-mob.md#5-skeleton-soldier-skeletonsoldier) (1 of 3); [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (1 of 6); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (3 of 15 at full difficulty, 3 of 9 at difficulty 1, 3 of 5 at difficulty 0). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#thompson-thompson); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Revolver (`revolver`)

**1. Identity.**

- Catalog row 13 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:revolver` (`GameRegistry.registerItem(..., "revolver")`); unlocalized name `techguns.item.revolver`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 6). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Revolver", lang key `techguns.item.revolver.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 3 [empty only]: grid `b m f` / `· s ·` with b = Iron Barrel, m = Mechanical Parts, f = Flint and Steel, s = Wooden Stock.
- The recipe outputs item damage = clip, which `onCreated` turns into **0 rounds**: the crafted gun is empty and must be loaded.
- Loot (`TGItems` chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): vanilla dungeon chest: count 1, weight 2; `techgunsWeaponchestT1`: count 1, weight 2; `techgunsWeaponchestT2`: count 1, weight 1. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.
- NPC equipment (not obtainable directly): [`ZombieSoldier`](../10-entities-npcs/per-mob.md#1-zombie-soldier-zombiesoldier) (1 of 4 weapon rolls); [`SkeletonSoldier`](../10-entities-npcs/per-mob.md#5-skeleton-soldier-skeletonsoldier) (1 of 3); [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (1 of 6); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (2 of 15 at full difficulty, 2 of 9 at difficulty 1, 2 of 5 at difficulty 0). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 8.0 per projectile |
| Minimum damage | 6.0 |
| Drop-off | full damage up to 12 blocks, linear to the minimum at 20 blocks, minimum beyond |
| Time to live / speed | 25 ticks / 1.5 blocks per tick; nominal reach 37.5 blocks |
| Accuracy (spread) | 0.025, an independent uniform random offset of +-1 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 6 ticks (max 3.33 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 39 ticks (reload minus `minFiretime`) |
| Clip | 6 rounds |
| Ammo per reload | 1 x 9mm Bullets (shared item meta 1) |
| Empty returned | none |
| Forced-reload refund | none: the remaining rounds are discarded when a forced reload is used on a magazine-less gun (`GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 6 ticks (300 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 18 blocks, attack time 90 ticks (variance 30), burst 6 shots, burst delay 20 ticks |
| Config key | `WeaponStats_techguns.item.revolver` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [8.0, 6.0, 12, 20, 25, 0.025, 0.0]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (6), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Plain semi-auto `GenericGun` that loads straight from 9mm Bullets: one item gives all 6 rounds. No magazine, no empty container and no refund, so a forced reload discards the remaining rounds (`GUN-13`).
- Input of the Golden Revolver upgrade recipe (`GUN-15`) and of the Akimbo Revolvers combine recipe.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 6) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 6, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 6). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 300 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: 9mm Bullets`
  - `Damage: 8.0-6.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: 9mm Bullets`
  - `DamageType: Projectile`
  - `Damage: 8.0-6.0`
  - `Range: 12,20,25`
  - `Spread: 0.025`
  - `Clipsize: 6`
  - `ReloadTime: 2.25s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderRevolver` with model [`ModelRevolver`](../03-gun-visuals-audio/gun-models.md#modelrevolver-6432-family-x-20-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#revolver-revolver-and-golden-revolver-goldenrevolver)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/revolver.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×32, average #433D37, dominant #101010 54%, #909090 14%, #B0B0B0 12%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashExp` at offset (0, 0.05, -0.45); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.revolverFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.78 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.revolverReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.72 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: 9mm Bullets (meta 1), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`ZombieSoldier`](../10-entities-npcs/per-mob.md#1-zombie-soldier-zombiesoldier) (1 of 4 weapon rolls); [`SkeletonSoldier`](../10-entities-npcs/per-mob.md#5-skeleton-soldier-skeletonsoldier) (1 of 3); [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (1 of 6); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (2 of 15 at full difficulty, 2 of 9 at difficulty 1, 2 of 5 at difficulty 0). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Recipes and machines: Input of the Golden Revolver upgrade (recipe 22) and of the Akimbo Revolvers combine recipe.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#revolver-revolver-and-golden-revolver-goldenrevolver); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-13`, `GUN-15`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Golden Revolver (`goldenrevolver`)

**1. Identity.**

- Catalog row 20 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:goldenrevolver` (`GameRegistry.registerItem(..., "goldenrevolver")`); unlocalized name `techguns.item.goldenrevolver`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 6). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Golden Revolver", lang key `techguns.item.goldenrevolver.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 22 [output loaded]: grid `g g g` / `g r g` / `g g g` with g = "ingotGold" (×8), r = Revolver (damage 0, which matches any crafted revolver regardless of ammo).
- The recipe outputs item damage 0, i.e. a full clip (`onCreated` sets `ammo` to the clip size).
- Loot: none.
- NPC equipment (not obtainable directly): [`DictatorDave`](../10-entities-npcs/per-mob.md#13-dictator-dave-dictatordave-mini-boss) (always). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).
- Guaranteed / special NPC drop: Dictator Dave always drops one Golden Revolver (count 1, no NBT, so full) plus 8 to 16 9mm Bullets, from his `dropFewItems`, on any death.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 14.0 per projectile |
| Minimum damage | 6.0 |
| Drop-off | full damage up to 12 blocks, linear to the minimum at 30 blocks, minimum beyond |
| Time to live / speed | 25 ticks / 1.5 blocks per tick; nominal reach 37.5 blocks |
| Accuracy (spread) | 0.015, an independent uniform random offset of +-0.6 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 8 ticks (max 2.5 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 37 ticks (reload minus `minFiretime`) |
| Clip | 6 rounds |
| Ammo per reload | 1 x 9mm Bullets (shared item meta 1) |
| Empty returned | none |
| Forced-reload refund | none: the remaining rounds are discarded when a forced reload is used on a magazine-less gun (`GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 8 ticks (400 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 60 ticks (variance 20), burst 6 shots, burst delay 15 ticks |
| Config key | `WeaponStats_techguns.item.goldenrevolver` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [14.0, 6.0, 12, 30, 25, 0.015, 0.0]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (8), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Plain semi-auto `GenericGun`: damage 14 falling to 6 between 12 and 30 blocks, accuracy 0.015. Recoil animation 8 ticks. Uses 9mm Bullets directly (no magazine) and shares the `guns.revolverReload` sound with the Revolver.
- Upgrade recipe: a Revolver surrounded by 8 gold ingots; it ignores the revolver's ammo and camo and outputs a full gun (`GUN-15`).
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 6) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 6, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 6). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 400 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: 9mm Bullets`
  - `Damage: 14.0-6.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: 9mm Bullets`
  - `DamageType: Projectile`
  - `Damage: 14.0-6.0`
  - `Range: 12,30,25`
  - `Spread: 0.015`
  - `Clipsize: 6`
  - `ReloadTime: 2.25s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderRevolver` with model [`ModelGoldenRevolver`](../03-gun-visuals-audio/gun-models.md#modelgoldenrevolver-6432-family-x-24-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#revolver-revolver-and-golden-revolver-goldenrevolver)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/goldenrevolver.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×32, average #636136, dominant #101010 49%, #F0F090 17%, #D0D070 11%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashExp` at offset (0, 0.05, -0.4); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.goldenrevolverFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 3.02 to 3.49 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.revolverReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.72 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: 9mm Bullets (meta 1), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`DictatorDave`](../10-entities-npcs/per-mob.md#13-dictator-dave-dictatordave-mini-boss) (always). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Recipes and machines: Input of the Akimbo Golden Revolvers combine recipe.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#revolver-revolver-and-golden-revolver-goldenrevolver); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-13`, `GUN-15`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Mac10 (`mac10`)

**1. Identity.**

- Catalog row 35 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:mac10` (`GameRegistry.registerItem(..., "mac10")`); unlocalized name `techguns.item.mac10`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 32). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Mac10", lang key `techguns.item.mac10.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 32 [loaded]: grid `· p i` / `b r i` / `· m ·` with p = "sheetPlastic", i = "nuggetIron" (×2), b = Hardened Steel Barrel, r = Steel Receiver, m = SMG Magazine; recipe 32e [empty]: grid same with m = SMG Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot (`TGItems` chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): `techgunsWeaponchestT2`: count 1, weight 1; `techgunsWeaponchestT3`: count 1, weight 1. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 5.0 per projectile |
| Minimum damage | 3.0 |
| Drop-off | full damage up to 15 blocks, linear to the minimum at 24 blocks, minimum beyond |
| Time to live / speed | 25 ticks / 1.5 blocks per tick; nominal reach 37.5 blocks |
| Accuracy (spread) | 0.05, an independent uniform random offset of +-2 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 2 ticks (10 per second) |
| Reload | 40 ticks (2 s); the gun refills at once and the player-wide `fireDelay` is set to 38 ticks (reload minus `minFiretime`) |
| Clip | 32 rounds |
| Ammo per reload | 1 x SMG Magazine (shared item meta 5) |
| Empty returned | 1 x SMG Magazine (Empty) (shared item meta 6) when a reload consumes ammo |
| Forced-reload refund | 9mm Bullets x floor(rounds left / 16) (16 rounds per item) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 2 ticks (100 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 18 blocks, attack time 35 ticks (variance 11), burst 3 shots, burst delay 2 ticks |
| Config key | `WeaponStats_techguns.item.mac10` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [5.0, 3.0, 15, 24, 25, 0.05, 0.0]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (2), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Plain full-auto `GenericGun`, interval 2 ticks, 32-round SMG Magazine (worth 16 rounds per 9mm Bullets item, 2 per magazine). Recoil animation 2 ticks. Can be combined into Akimbo Mac10s.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 32) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 32, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 32). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 100 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: SMG Magazine`
  - `Damage: 5.0-3.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: SMG Magazine`
  - `DamageType: Projectile`
  - `Damage: 5.0-3.0`
  - `Range: 15,24,25`
  - `Spread: 0.05`
  - `Clipsize: 32`
  - `ReloadTime: 2.0s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderMac10` with model [`ModelMac10`](../03-gun-visuals-audio/gun-models.md#modelmac10-declared-6432-used-with-a-12864-png-family-z-23-boxes-tabula-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#mac-10-mac10)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/mac10texture.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×64, average #555958, dominant #505050 58%, #303030 15%, #707070 9%, 33% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.05, 0); turret offsets: gun none, muzzle (0, 0, -0.45).

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.mac10Fire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.42 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.mac10Reload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.64 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: SMG Magazine (meta 5), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Recipes and machines: Input of the Akimbo Mac10s combine recipe.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#mac-10-mac10); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Akimbo Pistols (`pistolAkimbo`)

**1. Identity.**

- Catalog row 36 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun (akimbo, alternating)` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:pistolAkimbo` (`GameRegistry.registerItem(..., "pistolAkimbo")`); unlocalized name `techguns.item.pistolAkimbo`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 36). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Akimbo Pistols", lang key `techguns.item.pistolAkimbo.name`.

**2. Obtaining.**

- Crafting: shapeless [`RecipeAkimboGun`](recipes.md#shapeless--custom-recipes) with two Pistols (result NBT is the first gun's tag with `ammo` set to the sum of both guns' rounds). One Akimbo can be split back by [`RecipeAkimboSplit`](recipes.md#shapeless--custom-recipes), which yields two single guns through [`TechgunsTickHandler.onCraftingEvent`](../01-core/events.md#techgunstickhandler-fml-bus). See [recipes.md](recipes.md#shapeless--custom-recipes).
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 8.0 per projectile |
| Minimum damage | 5.0 |
| Drop-off | full damage up to 18 blocks, linear to the minimum at 25 blocks, minimum beyond |
| Time to live / speed | 30 ticks / 1.5 blocks per tick; nominal reach 45 blocks |
| Accuracy (spread) | 0.035, an independent uniform random offset of +-1.4 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 3 ticks (max 6.67 per second) |
| Akimbo | alternating: 1 round per pull, hand by parity of the rounds left |
| Reload | 52 ticks (2.6 s); the gun refills at once and the player-wide `fireDelay` is set to 49 ticks (reload minus `minFiretime`) |
| Clip | 36 rounds |
| Ammo per reload | 2 x 9mm Pistol Magazine (shared item meta 2) |
| Empty returned | 2 x 9mm Pistol Magazine (Empty) (shared item meta 3) when a reload consumes ammo |
| Forced-reload refund | 9mm Bullets x floor(rounds left / 6) (6 rounds per item) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 3 ticks (150 ms); muzzle flash 3 ticks (150 ms) |
| AI stats | range 24 blocks, attack time 30 ticks (variance 10), burst 3 shots, burst delay 10 ticks |
| Config key | `WeaponStats_techguns.item.pistolAkimbo` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [8.0, 5.0, 18, 25, 30, 0.035, 0.0]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (3), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- `setAkimbo(false)`: the ammo stack size doubles (a reload consumes 2 pistol magazines and returns 2 empty ones) and the hand alternates, chosen by the parity of the rounds left before the shot (odd = left gun). One round per pull.
- Compared with the single pistol: `minFiretime` 3 (vs 4), clip 36, reload 52, accuracy 0.035 (vs 0.025), muzzle flash 3 ticks, refund 6 rounds per 9mm Bullets item (the same as the single pistol: 36 rounds are 6 bullet items from 2 magazines).
- Made by combining two pistols (ammo summed, only the first gun's camo kept) and split back by crafting (`GUN-26`). Builder repeats the drop-off and muzzle calls as the pistol does (`GUN-16`). Rejected by the turret gun slot.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 36) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 36, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 36). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 150 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2600 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: 9mm Pistol Magazine`
  - `Damage: 8.0-5.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: 9mm Pistol Magazine`
  - `DamageType: Projectile`
  - `Damage: 8.0-5.0`
  - `Range: 18,25,30`
  - `Spread: 0.035`
  - `Clipsize: 36`
  - `ReloadTime: 2.6000001s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderPistolAkimbo` with model `ModelAkimbo(ModelPistol3)` ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#akimbo-pistols-pistolakimbo)).
- Texture (measured, from ASSET-FACTS):
  - `textures/guns/pistol3.png` (camo 0, default): 128×64, average #252323, dominant #303030 42%, #101010 40%, #301010 6%, 20% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.05, -0.45); turret offsets: gun none, muzzle none.
- The renderer draws a second gun in the left hand.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.pistolFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.14 s, 44100 Hz, stereo, sounds.json category `player` |
| `guns.pistolReloadAkimbo` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.60 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: 9mm Pistol Magazine (meta 2), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: rejected by the turret gun slot (`SlotTechgun` refuses akimbo guns).
- Recipes and machines: Output of the pistol combine recipe; splits back into two pistols.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#akimbo-pistols-pistolakimbo); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-16`, `GUN-26`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Akimbo Revolvers (`revolverAkimbo`)

**1. Identity.**

- Catalog row 37 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun (akimbo, alternating)` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:revolverAkimbo` (`GameRegistry.registerItem(..., "revolverAkimbo")`); unlocalized name `techguns.item.revolverAkimbo`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 12). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Akimbo Revolvers", lang key `techguns.item.revolverAkimbo.name`.

**2. Obtaining.**

- Crafting: shapeless `RecipeAkimboGun` with two Revolvers (result NBT is the first gun's tag with `ammo` set to the sum of both guns' rounds). One Akimbo can be split back by `RecipeAkimboSplit`, which yields two single guns through `TechgunsTickHandler.onCraftingEvent`. See [recipes.md](recipes.md#shapeless--custom-recipes).
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 8.0 per projectile |
| Minimum damage | 6.0 |
| Drop-off | full damage up to 12 blocks, linear to the minimum at 20 blocks, minimum beyond |
| Time to live / speed | 25 ticks / 1.5 blocks per tick; nominal reach 37.5 blocks |
| Accuracy (spread) | 0.035, an independent uniform random offset of +-1.4 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 4 ticks (max 5 per second) |
| Akimbo | alternating: 1 round per pull, hand by parity of the rounds left |
| Reload | 70 ticks (3.5 s); the gun refills at once and the player-wide `fireDelay` is set to 66 ticks (reload minus `minFiretime`) |
| Clip | 12 rounds |
| Ammo per reload | 2 x 9mm Bullets (shared item meta 1) |
| Empty returned | none |
| Forced-reload refund | none: the remaining rounds are discarded when a forced reload is used on a magazine-less gun (`GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 6 ticks (300 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 18 blocks, attack time 90 ticks (variance 30), burst 6 shots, burst delay 20 ticks |
| Config key | `WeaponStats_techguns.item.revolverAkimbo` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [8.0, 6.0, 12, 20, 25, 0.035, 0.0]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (4), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- `setAkimbo(false)`: reload consumes 2 9mm Bullets (6 rounds per item) for 12 rounds; hands alternate by parity. Compared with the single revolver: `minFiretime` 4 (vs 6), clip 12, reload 70, accuracy 0.035 (vs 0.025); sounds `guns.revolverFire` and `guns.revolverReloadAkimbo`.
- Combined and split by crafting (`GUN-26`). Rejected by the turret gun slot.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 12) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 12, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 12). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 300 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 3500 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: 9mm Bullets`
  - `Damage: 8.0-6.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: 9mm Bullets`
  - `DamageType: Projectile`
  - `Damage: 8.0-6.0`
  - `Range: 12,20,25`
  - `Spread: 0.035`
  - `Clipsize: 12`
  - `ReloadTime: 3.5s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderRevolverAkimbo` with model `ModelAkimbo(ModelRevolver)` ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#akimbo-revolvers-revolverakimbo-goldenrevolverakimbo)).
- Texture (measured, from ASSET-FACTS):
  - `textures/guns/revolver.png` (camo 0, default): 64×32, average #433D37, dominant #101010 54%, #909090 14%, #B0B0B0 12%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashExp` at offset (0, 0.05, -0.45); turret offsets: gun none, muzzle none.
- The renderer draws a second gun in the left hand.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.revolverFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.78 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.revolverReloadAkimbo` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.51 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: 9mm Bullets (meta 1), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: rejected by the turret gun slot (`SlotTechgun` refuses akimbo guns).
- Recipes and machines: Output of the revolver combine recipe; splits back into two revolvers.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#akimbo-revolvers-revolverakimbo-goldenrevolverakimbo); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-13`, `GUN-26`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Akimbo Golden Revolvers (`goldenrevolverAkimbo`)

**1. Identity.**

- Catalog row 38 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun (akimbo, alternating)` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:goldenrevolverAkimbo` (`GameRegistry.registerItem(..., "goldenrevolverAkimbo")`); unlocalized name `techguns.item.goldenrevolverAkimbo`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 12). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name " Akimbo Golden Revolvers" (the lang value starts with a space), lang key `techguns.item.goldenrevolverAkimbo.name`.

**2. Obtaining.**

- Crafting: shapeless `RecipeAkimboGun` with two Golden Revolvers (result NBT is the first gun's tag with `ammo` set to the sum of both guns' rounds). One Akimbo can be split back by `RecipeAkimboSplit`, which yields two single guns through `TechgunsTickHandler.onCraftingEvent`. See [recipes.md](recipes.md#shapeless--custom-recipes).
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 16.0 per projectile |
| Minimum damage | 6.0 |
| Drop-off | full damage up to 15 blocks, linear to the minimum at 30 blocks, minimum beyond |
| Time to live / speed | 25 ticks / 1.5 blocks per tick; nominal reach 37.5 blocks |
| Accuracy (spread) | 0.035, an independent uniform random offset of +-1.4 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 6 ticks (max 3.33 per second) |
| Akimbo | alternating: 1 round per pull, hand by parity of the rounds left |
| Reload | 70 ticks (3.5 s); the gun refills at once and the player-wide `fireDelay` is set to 64 ticks (reload minus `minFiretime`) |
| Clip | 12 rounds |
| Ammo per reload | 2 x 9mm Bullets (shared item meta 1) |
| Empty returned | none |
| Forced-reload refund | none: the remaining rounds are discarded when a forced reload is used on a magazine-less gun (`GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 8 ticks (400 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 60 ticks (variance 20), burst 6 shots, burst delay 15 ticks |
| Config key | `WeaponStats_techguns.item.goldenrevolverAkimbo` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [16.0, 6.0, 15, 30, 25, 0.035, 0.0]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (6), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- `setAkimbo(false)`: reload consumes 2 9mm Bullets for 12 rounds; hands alternate. Compared with the single Golden Revolver: damage **16** (single 14), drop-off 15 to 30 (single 12 to 30), `minFiretime` 6 (vs 8), reload 70, accuracy 0.035 (vs 0.015).
- Its English name begins with a stray space (`GUN-46`). Combined and split by crafting (`GUN-26`). Rejected by the turret gun slot.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 12) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 12, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 12). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 400 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 3500 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: 9mm Bullets`
  - `Damage: 16.0-6.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: 9mm Bullets`
  - `DamageType: Projectile`
  - `Damage: 16.0-6.0`
  - `Range: 15,30,25`
  - `Spread: 0.035`
  - `Clipsize: 12`
  - `ReloadTime: 3.5s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderRevolverAkimbo` with model `ModelAkimbo(ModelGoldenRevolver)` ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#akimbo-revolvers-revolverakimbo-goldenrevolverakimbo)).
- Texture (measured, from ASSET-FACTS):
  - `textures/guns/goldenrevolver.png` (camo 0, default): 64×32, average #636136, dominant #101010 49%, #F0F090 17%, #D0D070 11%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashExp` at offset (0, 0.05, -0.4); turret offsets: gun none, muzzle none.
- The renderer draws a second gun in the left hand.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.goldenrevolverFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 3.02 to 3.49 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.revolverReloadAkimbo` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.51 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: 9mm Bullets (meta 1), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: rejected by the turret gun slot (`SlotTechgun` refuses akimbo guns).
- Recipes and machines: Output of the golden-revolver combine recipe; splits back into two.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#akimbo-revolvers-revolverakimbo-goldenrevolverakimbo); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-13`, `GUN-26`, `GUN-46`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Akimbo Mac10s (`mac10Akimbo`)

**1. Identity.**

- Catalog row 39 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun (akimbo, double shot)` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:mac10Akimbo` (`GameRegistry.registerItem(..., "mac10Akimbo")`); unlocalized name `techguns.item.mac10Akimbo`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 64). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Akimbo Mac10s", lang key `techguns.item.mac10Akimbo.name`.

**2. Obtaining.**

- Crafting: shapeless `RecipeAkimboGun` with two Mac10s (result NBT is the first gun's tag with `ammo` set to the sum of both guns' rounds). One Akimbo can be split back by `RecipeAkimboSplit`, which yields two single guns through `TechgunsTickHandler.onCraftingEvent`. See [recipes.md](recipes.md#shapeless--custom-recipes).
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 5.0 per projectile |
| Minimum damage | 3.0 |
| Drop-off | full damage up to 15 blocks, linear to the minimum at 24 blocks, minimum beyond |
| Time to live / speed | 25 ticks / 1.5 blocks per tick; nominal reach 37.5 blocks |
| Accuracy (spread) | 0.07, an independent uniform random offset of +-2.8 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 2 ticks (10 per second) |
| Akimbo | double shot: 2 rounds and 2 main projectiles per pull |
| Reload | 68 ticks (3.4 s); the gun refills at once and the player-wide `fireDelay` is set to 66 ticks (reload minus `minFiretime`) |
| Clip | 64 rounds |
| Ammo per reload | 2 x SMG Magazine (shared item meta 5) |
| Empty returned | 2 x SMG Magazine (Empty) (shared item meta 6) when a reload consumes ammo |
| Forced-reload refund | 9mm Bullets x floor(rounds left / 32) (32 rounds per item) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 2 ticks (100 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 18 blocks, attack time 45 ticks (variance 15), burst 3 shots, burst delay 3 ticks |
| Config key | `WeaponStats_techguns.item.mac10Akimbo` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [5.0, 3.0, 15, 24, 25, 0.07, 0.0]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (2), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- `setAkimbo(true)`: both guns fire on every trigger pull. Each pull uses 2 rounds and spawns two main projectiles (right, then left), each with the full damage; the tooltip does not show a multiplier. A reload consumes 2 SMG Magazines for 64 rounds.
- Compared with the single Mac10: clip 64, reload 68, accuracy 0.07, AI attack time 45 with burst 3 and delay 3. The refund factor passed to the builder is 2 (32 rounds per bullet item, so a full akimbo refunds 2 items, `GUN-14`). Combined and split by crafting (`GUN-26`). Rejected by the turret gun slot.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 64) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 64, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 64). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 100 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 3400 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: SMG Magazine`
  - `Damage: 5.0-3.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: SMG Magazine`
  - `DamageType: Projectile`
  - `Damage: 5.0-3.0`
  - `Range: 15,24,25`
  - `Spread: 0.07`
  - `Clipsize: 64`
  - `ReloadTime: 3.4s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderMac10Akimbo` with model `ModelAkimbo(ModelMac10)` ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#akimbo-mac-10-mac10akimbo)).
- Texture (measured, from ASSET-FACTS):
  - `textures/guns/mac10texture.png` (camo 0, default): 128×64, average #555958, dominant #505050 58%, #303030 15%, #707070 9%, 33% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.05, 0); turret offsets: gun none, muzzle none.
- The renderer draws a second gun in the left hand; muzzle FX are spawned for both hands on a double shot.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.mac10Fire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.42 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.mac10ReloadAkimbo` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.41 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: SMG Magazine (meta 5), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: rejected by the turret gun slot (`SlotTechgun` refuses akimbo guns).
- Recipes and machines: Output of the Mac10 combine recipe; splits back into two.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#akimbo-mac-10-mac10akimbo); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-14`, `GUN-26`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Vector (`vector`)

**1. Identity.**

- Catalog row 42 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:vector` (`GameRegistry.registerItem(..., "vector")`); unlocalized name `techguns.item.vector`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 25). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Vector", lang key `techguns.item.vector.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 34 [loaded]: grid `· g d` / `b r s` / `· m ·` with g = "paneGlass", d = "dustRedstone", b = Hardened Steel Barrel, r = Obsidian Steel Receiver, s = Plastic Gun Stock, m = SMG Magazine; recipe 34e [empty]: grid same with m = SMG Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 6.0 per projectile |
| Minimum damage | 4.0 |
| Drop-off | full damage up to 17 blocks, linear to the minimum at 25 blocks, minimum beyond |
| Time to live / speed | 30 ticks / 1.5 blocks per tick; nominal reach 45 blocks |
| Accuracy (spread) | 0.05, an independent uniform random offset of +-2 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus) and by 0.35 while zoomed |
| Armor penetration | 15% (0.15) |
| Fire mode | full-auto: one round every 2 ticks (10 per second) |
| Reload | 40 ticks (2 s); the gun refills at once and the player-wide `fireDelay` is set to 38 ticks (reload minus `minFiretime`) |
| Clip | 25 rounds |
| Ammo per reload | 1 x SMG Magazine (shared item meta 5) |
| Empty returned | 1 x SMG Magazine (Empty) (shared item meta 6) when a reload consumes ammo |
| Forced-reload refund | 9mm Bullets x floor(rounds left / 12.5) (12.5 rounds per item) |
| Zoom | toggle on right-click; FOV x0.75, spread x0.35 while zoomed |
| Animation times | recoil 2 ticks (100 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 18 blocks, attack time 35 ticks (variance 11), burst 3 shots, burst delay 2 ticks |
| Config key | `WeaponStats_techguns.item.vector` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [6.0, 4.0, 17, 25, 30, 0.05, 0.15]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (2), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Plain full-auto SMG with the shared SMG Magazine (25 rounds here) and a light scope (FOV x0.75, zoomed spread x0.35). Recoil animation 2 ticks.
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 25) and `camo` (byte, selected variant, 0 to 1). Created by `onCreated` / `getSubItems` (`ammo` = 25, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 25). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: 2 variants; the Camo Bench cycles the byte (wrap-around) through [`ICamoChangeable`](../05-armor/armor-catalog.md#1-class-hierarchy) ([09-machines-utility](../09-machines-utility/camo-bench.md)). On a tag-less stack the first switch empties the gun (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 100 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: SMG Magazine`
  - `Damage: 6.0-4.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: SMG Magazine`
  - `DamageType: Projectile`
  - `Damage: 6.0-4.0`
  - `Range: 17,25,30`
  - `Spread: 0.05`
  - `Clipsize: 25`
  - `ReloadTime: 2.0s`
  - `ArmorPen: 15.000001%`
  - `Zoom:(toggle) Mult:0.75`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderVector` with model [`ModelVector`](../03-gun-visuals-audio/gun-models.md#modelvector-12864-used-with-a-256128-png-family-z-28-boxes-tabula-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#vector-vector)).
- Textures (measured, from ASSET-FACTS):
  - [`textures/guns/vector_texture.png`](../03-gun-visuals-audio/textures.md#texturesgunsvector_texturepng-vector_texture_1png-vector) (camo 0, default): 256×128, average #585842, dominant #303030 54%, #B0B070 34%, #101010 5%, 29% opaque.
  - [`textures/guns/vector_texture_1.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 1, Black): 256×128, average #2A2929, dominant #303030 92%, #101010 6%, #505050 0%, 29% opaque.
- Camo variants: 2 (index 0 "Default Camo", 1 "Black").
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.05, 0); turret offsets: gun none, muzzle (0, 0, -0.45).

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.vectorFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 0.60 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.vectorReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 1.54 s, 48000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: SMG Magazine (meta 5), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#vector-vector); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-12`, `GUN-07`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

