# 02-guns: Entity sheets: shotguns, rifles and machine guns

Each item below has a full entity sheet with fields 1 to 10 of the [conventions](../CONVENTIONS.md#entity-sheet): identity, obtaining, numbers, behavior, persistence and sync, UI, visuals, audio, relationships and bugs. The catalog tables ([gun-catalog.md](gun-catalog.md)) remain the quick reference; shared mechanics are in [gun-lifecycle.md](gun-lifecycle.md), the shared rules for these sheets are in [sheets-index.md](sheets-index.md), and class-level detail is in [per-gun-behavior.md](per-gun-behavior.md).

Items in this file: [Sawed Off](#sawed-off-sawedoff), [Bolt Action Rifle](#bolt-action-rifle-boltaction), [M4 Assault Rifle](#m4-assault-rifle-m4), [Combat Shotgun](#combat-shotgun-combatshotgun), [Kalaschnikow (AK47)](#kalaschnikow-ak47-ak47), [AS50 Sniper Rifle](#as50-sniper-rifle-as50), [PDW](#pdw-pdw), [LMG](#lmg-lmg), [The Infiltrator](#the-infiltrator-m4_uq), [Pulse Rifle](#pulse-rifle-pulserifle), [Minigun](#minigun-minigun), [AUG](#aug-aug), [SCAR](#scar-scar).

### Sawed Off (`sawedoff`)

**1. Identity.**

- Catalog row 3 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class [`GenericGun`](gun-lifecycle.md) (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:sawedoff` (`GameRegistry.registerItem(..., "sawedoff")`); unlocalized name `techguns.item.sawedoff`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 2). Creative tab [`Techguns`](../01-core/lifecycle-and-registration.md#lifecycle-overview) (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Sawed Off", lang key `techguns.item.sawedoff.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 4 [empty only (vanilla shaped)]: grid `b f w` / `b f ·` with b = Iron Barrel, f = Flint and Steel, w = Wooden Stock.
- The recipe outputs item damage = clip, which `onCreated` turns into **0 rounds**: the crafted gun is empty and must be loaded.
- Loot ([`TGItems`](../06-items/item-catalog.md) chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): vanilla dungeon chest: count 1, weight 2; `techgunsWeaponchestT1`: count 1, weight 2; `techgunsWeaponchestT2`: count 1, weight 1. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.
- NPC equipment (not obtainable directly): [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (1 of 6). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 4.0 per projectile |
| Minimum damage | 1.5 |
| Drop-off | full damage up to 1 blocks, linear to the minimum at 4 blocks, minimum beyond |
| Time to live / speed | 10 ticks / 1.5 blocks per tick; nominal reach 15 blocks |
| Accuracy (spread) | 0.01, an independent uniform random offset of +-0.4 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Pellets | 7 extra projectiles per shot (total 8), pellet spread 0.2 (+-8 degrees); one round is used per shot |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 4 ticks (5 per second) |
| Reload | 28 ticks (1.4 s); the gun refills at once and the player-wide `fireDelay` is set to 24 ticks (reload minus `minFiretime`) |
| Clip | 2 rounds |
| Ammo per reload | up to 2 x Shotgun Shell (shared item meta 4), one round each (all-or-nothing per item, loads as many as the inventory has) |
| Empty returned | none |
| Forced-reload refund | none needed: a shell-by-shell gun keeps its loaded rounds on a forced reload and tops up with up to 2 minus the rounds left in items |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 12 blocks, attack time 60 ticks (variance 20), burst 2 shots, burst delay 20 ticks |
| Config key | `WeaponStats_techguns.item.sawedoff` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [4.0, 1.5, 1, 4, 10, 0.01, 0.0]; indices all honored |
| Other config keys | [`DamageFactorNPC`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (1.0), [`DamageTurretToPlayer`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (0.5), [`DamagePvP`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (0.5), [`ArmorPenetrationMultiplierNPCtoPlayer`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) and [`ArmorPenetrationMultiplierPvP`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (1.0) scale hits; [`ForceSafeModeForNonOps`](../01-core/config.md#category-general) (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends [`PacketShootGun`](../01-core/networking.md#8--packetshootgun) and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (4), broadcasts [`GunFiredMessage`](../01-core/networking.md#1--gunfiredmessage) within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Shell-by-shell double barrel: `ammoCount` is 2, so a reload from empty consumes up to 2 Shotgun Shells (each is one round) and refills only as many rounds as shells were found. Forced reload tops up from the current rounds.
- Each trigger pull spawns 1 center projectile with the normal accuracy plus 7 pellets with the pellet spread 0.2 (8 [`GenericProjectile`](../04-projectiles-fx/projectiles.md#41-genericprojectile-the-base) in total, each with the full per-projectile damage). The tooltip shows `Damage(x8)`.
- Very short drop-off (1 to 4 blocks) and a time-to-live of 10 ticks make it a close-range weapon. No empty container, no refund.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 2) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 2, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 2). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); [`ReloadStartedMessage`](../01-core/networking.md#3--reloadstartedmessage) (server to clients within 100 blocks: 1400 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in [`TechgunsExtendedPlayerProperties`](../01-core/player-state.md#techgunsextendedplayerproperties): `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: 2x Shotgun Shell`
  - `Damage(x8): 4.0-1.5`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: 2x Shotgun Shell`
  - `DamageType: Projectile`
  - `Damage(x8): 4.0-1.5`
  - `Range: 1,4,10`
  - `Spread: 0.01`
  - `Clipsize: 2`
  - `ReloadTime: 1.4s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderSawedoff` with model [`ModelSawedoff`](../03-gun-visuals-audio/gun-models.md#modelsawedoff-6432-family-x-20-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#sawed-off-sawedoff)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/sawedOff.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×32, average #524C3E, dominant #101010 24%, #505030 21%, #B0B0B0 15%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashExp` at offset (0, 0, 0.1); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.sawedoffFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.30 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.sawedoffReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 1.54 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Shotgun Shell (meta 4), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (1 of 6). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from [`GenericNPC`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava), second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot ([`SlotTechgun`](../09-machines-utility/turret.md)); [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#sawed-off-sawedoff); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Bolt Action Rifle (`boltaction`)

**1. Identity.**

- Catalog row 4 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:boltaction` (`GameRegistry.registerItem(..., "boltaction")`); unlocalized name `techguns.item.boltaction`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 6). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Bolt Action Rifle", lang key `techguns.item.boltaction.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 7 [empty only]: grid `g i ·` / `b f s` with g = "blockGlass", i = "plateIron", b = Hardened Steel Barrel, f = Iron Receiver, s = Wooden Stock.
- The recipe outputs item damage = clip, which `onCreated` turns into **0 rounds**: the crafted gun is empty and must be loaded.
- Loot (`TGItems` chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): vanilla dungeon chest: count 1, weight 1; `techgunsWeaponchestT2`: count 1, weight 1; `techgunsWeaponchestT3`: count 1, weight 1. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.
- NPC equipment (not obtainable directly): [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (1 of 6); [`ArmySoldier`](../10-entities-npcs/per-mob.md#6-army-soldier-armysoldier) (1 of 3). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 16.0 per projectile |
| Minimum damage | 10.0 |
| Drop-off | full damage up to 20 blocks, linear to the minimum at 30 blocks, minimum beyond |
| Time to live / speed | 40 ticks / 2.25 blocks per tick; nominal reach 90 blocks |
| Accuracy (spread) | 0.05, an independent uniform random offset of +-2 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus) and by 0.125 while zoomed |
| Armor penetration | 15% (0.15) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 25 ticks (max 0.8 per second) |
| Reload | 50 ticks (2.5 s); the gun refills at once and the player-wide `fireDelay` is set to 25 ticks (reload minus `minFiretime`) |
| Clip | 6 rounds |
| Ammo per reload | 1 x Rifle Bullets (shared item meta 7) |
| Empty returned | none |
| Forced-reload refund | none: the remaining rounds are discarded when a forced reload is used on a magazine-less gun (`GUN-13`) |
| Zoom | toggle on right-click; FOV x0.35, spread x0.125 while zoomed |
| Animation times | recoil 15 ticks (750 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 36 blocks, attack time 60 ticks (variance 20), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.boltaction` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [16.0, 10.0, 20, 30, 40, 0.05, 0.15]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (25), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Scoped semi-auto rifle. Right-click toggles the zoom (FOV x0.35); shots fired while zoomed multiply the spread by 0.125. Zoom is canceled when a reload starts.
- Bullet speed is the base 1.5 multiplied by 1.5 (2.25 blocks/tick). After every shot the rechamber sound `guns.boltactionRechamber` also plays (volume 1.0, category RELOAD).
- Reload takes one Rifle Bullets item for all 6 rounds (no magazine item). Long recoil animation (15 ticks).
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 6) and `camo` (byte, selected variant, 0 to 2). Created by `onCreated` / `getSubItems` (`ammo` = 6, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 6). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: 3 variants; the Camo Bench cycles the byte (wrap-around) through [`ICamoChangeable`](../05-armor/armor-catalog.md#1-class-hierarchy) ([09-machines-utility](../09-machines-utility/camo-bench.md)). On a tag-less stack the first switch empties the gun (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 750 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2500 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Rifle Bullets`
  - `Damage: 16.0-10.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: Rifle Bullets`
  - `DamageType: Projectile`
  - `Damage: 16.0-10.0`
  - `Range: 20,30,40`
  - `Spread: 0.05`
  - `Clipsize: 6`
  - `ReloadTime: 2.5s`
  - `ArmorPen: 15.000001%`
  - `Zoom:(toggle) Mult:0.35`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderBoltActionRifle` with model [`ModelBoltaction`](../03-gun-visuals-audio/gun-models.md#modelboltaction-12832-family-x-22-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#bolt-action-rifle-boltaction)).
- Textures (measured, from ASSET-FACTS):
  - [`textures/guns/boltactionrifle.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×32, average #13100D, dominant #101010 71%, #303010 14%, #303030 7%, 100% opaque.
  - [`textures/guns/boltactionrifle_1.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 1, Olive): 128×32, average #323A27, dominant #303010 43%, #303030 22%, #101010 13%, 33% opaque.
  - [`textures/guns/boltactionrifle_2.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 2, Tactical Black): 128×32, average #202020, dominant #303030 58%, #101010 40%, #909090 0%, 33% opaque.
- Camo variants: 3 (index 0 "Default Camo", 1 "Olive", 2 "Tactical Black").
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.05, 0.3); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.boltactionFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.98 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.boltactionReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 1.94 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.boltactionRechamber` | after every shot | 1.0 / 1.0 (category RELOAD) | 1 file, 1.03 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Rifle Bullets (meta 7), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (1 of 6); [`ArmySoldier`](../10-entities-npcs/per-mob.md#6-army-soldier-armysoldier) (1 of 3). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#bolt-action-rifle-boltaction); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-12`, `GUN-07`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### M4 Assault Rifle (`m4`)

**1. Identity.**

- Catalog row 5 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:m4` (`GameRegistry.registerItem(..., "m4")`); unlocalized name `techguns.item.m4`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 30). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "M4 Assault Rifle", lang key `techguns.item.m4.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 6 [loaded]: grid `· g r` / `b f s` / `n m i` with g = "paneGlass", r = Redstone (item), b = Hardened Steel Barrel, f = Steel Receiver, s = Plastic Gun Stock, n = "nuggetIron", m = Assault Rifle Magazine, i = "ingotIron"; recipe 6e [empty]: grid same with m = Assault Rifle Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot (`TGItems` chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): vanilla dungeon chest: count 1, weight 1; `techgunsWeaponchestT2`: count 1, weight 1; `techgunsWeaponchestT3`: count 1, weight 1. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.
- NPC equipment (not obtainable directly): [`ArmySoldier`](../10-entities-npcs/per-mob.md#6-army-soldier-armysoldier) (1 of 3). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 8.0 per projectile |
| Minimum damage | 6.0 |
| Drop-off | full damage up to 25 blocks, linear to the minimum at 35 blocks, minimum beyond |
| Time to live / speed | 35 ticks / 1.5 blocks per tick; nominal reach 52.5 blocks |
| Accuracy (spread) | 0.015, an independent uniform random offset of +-0.6 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus) and by 0.75 while zoomed |
| Armor penetration | 15% (0.15) |
| Fire mode | full-auto: one round every 3 ticks (6.67 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 42 ticks (reload minus `minFiretime`) |
| Clip | 30 rounds |
| Ammo per reload | 1 x Assault Rifle Magazine (shared item meta 12) |
| Empty returned | 1 x Assault Rifle Magazine (Empty) (shared item meta 13) when a reload consumes ammo |
| Forced-reload refund | Rifle Bullets x floor(rounds left / 10) (10 rounds per item) |
| Zoom | toggle on right-click; FOV x0.75, spread x0.75 while zoomed |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 30 ticks (variance 10), burst 3 shots, burst delay 3 ticks |
| Config key | `WeaponStats_techguns.item.m4` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [8.0, 6.0, 25, 35, 35, 0.015, 0.15]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (3), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Plain full-auto `GenericGun` with a toggle scope (FOV x0.75, zoomed spread x0.75). It shares the Assault Rifle Magazine (30 rounds, worth 3 Rifle Bullets) with the AK47, AUG, SCAR and Infiltrator.
- It is the input of the Infiltrator upgrade recipe, which ignores its ammo and camo (`GUN-15`).
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 30) and `camo` (byte, selected variant, 0 to 4). Created by `onCreated` / `getSubItems` (`ammo` = 30, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 30). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: 5 variants; the Camo Bench cycles the byte (wrap-around) through `ICamoChangeable` ([09-machines-utility](../09-machines-utility/camo-bench.md)). On a tag-less stack the first switch empties the gun (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Assault Rifle Magazine`
  - `Damage: 8.0-6.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: Assault Rifle Magazine`
  - `DamageType: Projectile`
  - `Damage: 8.0-6.0`
  - `Range: 25,35,35`
  - `Spread: 0.015`
  - `Clipsize: 30`
  - `ReloadTime: 2.25s`
  - `ArmorPen: 15.000001%`
  - `Zoom:(toggle) Mult:0.75`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderM4` with model [`ModelM4`](../03-gun-visuals-audio/gun-models.md#modelm4-12864-family-z-42-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#m4-m4)).
- Textures (measured, from ASSET-FACTS):
  - [`textures/guns/m4Texture.png`](../03-gun-visuals-audio/textures.md#texturesgunsm4texturepng--camo-variants-m4-m4-magazine-items) (camo 0, default): 128×64, average #25262A, dominant #303030 44%, #101010 31%, #101030 9%, 40% opaque.
  - [`textures/guns/m4Texture_1.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 1, Swamp): 128×64, average #375320, dominant #305010 21%, #103010 16%, #507030 11%, 40% opaque.
  - [`textures/guns/m4Texture_2.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 2, Desert): 128×64, average #727A44, dominant #909050 22%, #505030 17%, #90B070 12%, 40% opaque.
  - [`textures/guns/m4Texture_3.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 3, Ice): 128×64, average #7F7E7C, dominant #909090 44%, #707070 32%, #B0B0B0 10%, 40% opaque.
  - [`textures/guns/m4Texture_4.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 4, Hazard): 128×64, average #B67D10, dominant #F0B010 24%, #B07010 15%, #F0D010 11%, 40% opaque.
- Camo variants: 5 (index 0 "Default Camo", 1 "Swamp", 2 "Desert", 3 "Ice", 4 "Hazard").
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.025, 0); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.assaultRifleFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 2 files, 1.23 to 1.40 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.assaultRifleReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.09 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Assault Rifle Magazine (meta 12), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`ArmySoldier`](../10-entities-npcs/per-mob.md#6-army-soldier-armysoldier) (1 of 3). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Recipes and machines: Input of The Infiltrator upgrade (recipe 24).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#m4-m4); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-15`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-12`, `GUN-07`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Combat Shotgun (`combatshotgun`)

**1. Identity.**

- Catalog row 7 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:combatshotgun` (`GameRegistry.registerItem(..., "combatshotgun")`); unlocalized name `techguns.item.combatshotgun`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 8). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Combat Shotgun", lang key `techguns.item.combatshotgun.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 8 [empty only]: grid `b f s` / `i · ·` with b = Hardened Steel Barrel, f = Steel Receiver, s = Plastic Gun Stock, i = "ingotSteel".
- The recipe outputs item damage = clip, which `onCreated` turns into **0 rounds**: the crafted gun is empty and must be loaded.
- Loot (`TGItems` chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): `techgunsWeaponchestT3`: count 1, weight 1. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.
- NPC equipment (not obtainable directly): [`ArmySoldier`](../10-entities-npcs/per-mob.md#6-army-soldier-armysoldier) (1 of 3); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (2 of 15 at full difficulty only); [`SuperMutantBasic`](../10-entities-npcs/per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (1 of 5; inherited by Heavy and Elite). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 4.0 per projectile |
| Minimum damage | 1.5 |
| Drop-off | full damage up to 2 blocks, linear to the minimum at 5 blocks, minimum beyond |
| Time to live / speed | 15 ticks / 1.5 blocks per tick; nominal reach 22.5 blocks |
| Accuracy (spread) | 0.01, an independent uniform random offset of +-0.4 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Pellets | 7 extra projectiles per shot (total 8), pellet spread 0.15 (+-6 degrees); one round is used per shot |
| Armor penetration | 15% (0.15) |
| Fire mode | full-auto: one round every 14 ticks (1.43 per second) |
| Reload | 50 ticks (2.5 s); the gun refills at once and the player-wide `fireDelay` is set to 36 ticks (reload minus `minFiretime`) |
| Clip | 8 rounds |
| Ammo per reload | up to 8 x Shotgun Shell (shared item meta 4), one round each (all-or-nothing per item, loads as many as the inventory has) |
| Empty returned | none |
| Forced-reload refund | none needed: a shell-by-shell gun keeps its loaded rounds on a forced reload and tops up with up to 8 minus the rounds left in items |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 12 ticks (600 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 12 blocks, attack time 30 ticks (variance 10), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.combatshotgun` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [4.0, 1.5, 2, 5, 15, 0.01, 0.15]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (14), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Shell-by-shell pump shotgun, `ammoCount` 8: a reload loads up to 8 Shotgun Shells at once. Each pull fires 1 center projectile plus 7 pellets with spread 0.15 (tooltip `Damage(x8)`).
- After every shot the pump sound `guns.combatshotgunPump` plays (volume 1.0, category RELOAD). Long recoil animation (12 ticks).
- `setTurretPosOffset(0, 0.075, 0)` nudges the gun when mounted on a turret.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 8) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 8, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 8). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 600 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2500 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: 8x Shotgun Shell`
  - `Damage(x8): 4.0-1.5`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: 8x Shotgun Shell`
  - `DamageType: Projectile`
  - `Damage(x8): 4.0-1.5`
  - `Range: 2,5,15`
  - `Spread: 0.01`
  - `Clipsize: 8`
  - `ReloadTime: 2.5s`
  - `ArmorPen: 15.000001%`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderCombatShotgun` with model [`ModelCombatShotgun`](../03-gun-visuals-audio/gun-models.md#modelcombatshotgun-6432-family-x-20-boxes-2-parts) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#combat-shotgun-combatshotgun)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/combatShotgun.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×32, average #373737, dominant #101010 33%, #303030 26%, #505050 18%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashExp` at offset (0, 0, 0.1); turret offsets: gun (0, 0.075, 0), muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.combatshotgunFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 2.10 to 3.14 s, 48000 Hz, stereo, sounds.json category `player` |
| `guns.combatshotgunReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.95 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.combatshotgunPump` | after every shot | 1.0 / 1.0 (category RELOAD) | 1 file, 0.89 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Shotgun Shell (meta 4), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`ArmySoldier`](../10-entities-npcs/per-mob.md#6-army-soldier-armysoldier) (1 of 3); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (2 of 15 at full difficulty only); [`SuperMutantBasic`](../10-entities-npcs/per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (1 of 5; inherited by Heavy and Elite). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0.075, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#combat-shotgun-combatshotgun); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Kalaschnikow (AK47) (`ak47`)

**1. Identity.**

- Catalog row 14 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:ak47` (`GameRegistry.registerItem(..., "ak47")`); unlocalized name `techguns.item.ak47`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 30). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Kalaschnikow", lang key `techguns.item.ak47.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 13 [loaded]: grid `b f s` / `w m ·` with b = Hardened Steel Barrel, f = Iron Receiver, s = Wooden Stock, w = "logWood", m = Assault Rifle Magazine; recipe 13e [empty]: grid same with m = Assault Rifle Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot (`TGItems` chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): vanilla dungeon chest: count 1, weight 1; `techgunsWeaponchestT2`: count 1, weight 1; `techgunsWeaponchestT3`: count 1, weight 1. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.
- NPC equipment (not obtainable directly): [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (1 of 6); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (2 of 15 at full difficulty, 2 of 9 at difficulty 1, never at difficulty 0); [`SuperMutantBasic`](../10-entities-npcs/per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (1 of 5; inherited by [`SuperMutantHeavy`](../10-entities-npcs/per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) and [`SuperMutantElite`](../10-entities-npcs/per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite)). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 9.0 per projectile |
| Minimum damage | 5.0 |
| Drop-off | full damage up to 20 blocks, linear to the minimum at 30 blocks, minimum beyond |
| Time to live / speed | 35 ticks / 1.5 blocks per tick; nominal reach 52.5 blocks |
| Accuracy (spread) | 0.03, an independent uniform random offset of +-1.2 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | 15% (0.15) |
| Fire mode | full-auto: one round every 3 ticks (6.67 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 42 ticks (reload minus `minFiretime`) |
| Clip | 30 rounds |
| Ammo per reload | 1 x Assault Rifle Magazine (shared item meta 12) |
| Empty returned | 1 x Assault Rifle Magazine (Empty) (shared item meta 13) when a reload consumes ammo |
| Forced-reload refund | Rifle Bullets x floor(rounds left / 10) (10 rounds per item) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 30 ticks (variance 10), burst 3 shots, burst delay 3 ticks |
| Config key | `WeaponStats_techguns.item.ak47` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [9.0, 5.0, 20, 30, 35, 0.03, 0.15]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (3), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Plain full-auto `GenericGun` using the shared Assault Rifle Magazine. Unscoped. Config comments call it "Kalashnikov"; the English name is "Kalaschnikow".
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 30) and `camo` (byte, selected variant, 0 to 1). Created by `onCreated` / `getSubItems` (`ammo` = 30, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 30). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: 2 variants; the Camo Bench cycles the byte (wrap-around) through `ICamoChangeable` ([09-machines-utility](../09-machines-utility/camo-bench.md)). On a tag-less stack the first switch empties the gun (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Assault Rifle Magazine`
  - `Damage: 9.0-5.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: Assault Rifle Magazine`
  - `DamageType: Projectile`
  - `Damage: 9.0-5.0`
  - `Range: 20,30,35`
  - `Spread: 0.03`
  - `Clipsize: 30`
  - `ReloadTime: 2.25s`
  - `ArmorPen: 15.000001%`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderAK` with model [`ModelAK`](../03-gun-visuals-audio/gun-models.md#modelak-12864-family-z-35-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#ak-47-ak47)).
- Textures (measured, from ASSET-FACTS):
  - [`textures/guns/ak47Texture.png`](../03-gun-visuals-audio/textures.md#texturesgunsak47texturepng-ak47texture_1png-ak-47) (camo 0, default): 128×64, average #382C22, dominant #303030 64%, #503010 14%, #703010 7%, 43% opaque.
  - [`textures/guns/ak47Texture_1.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 1, Blue): 128×64, average #192B42, dominant #303030 64%, #103070 12%, #103090 4%, 42% opaque.
- Camo variants: 2 (index 0 "Default Camo", 1 "Blue").
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.05, 0.12); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.ak47Fire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.50 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.ak47Reload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.78 s, 48000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Assault Rifle Magazine (meta 12), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Bandit`](../10-entities-npcs/per-mob.md#7-bandit-bandit) (1 of 6); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (2 of 15 at full difficulty, 2 of 9 at difficulty 1, never at difficulty 0); [`SuperMutantBasic`](../10-entities-npcs/per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (1 of 5; inherited by `SuperMutantHeavy` and `SuperMutantElite`). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#ak-47-ak47); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### AS50 Sniper Rifle (`as50`)

**1. Identity.**

- Catalog row 15 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:as50` (`GameRegistry.registerItem(..., "as50")`); unlocalized name `techguns.item.as50`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 10). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "AS50 Sniper Rifle", lang key `techguns.item.as50.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 14 [loaded]: grid `d p d` / `b b f` / `· m t` with d = "gemDiamond", p = "plateObsidianSteel", b = Hardened Steel Barrel, f = Obsidian Steel Receiver, m = AS50 Magazine, t = Plastic Gun Stock; recipe 14e [empty]: grid same with m = AS50 Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.
- NPC equipment (not obtainable directly): [`GenericNPC` default `pickRandomGun`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) (1 of 4, only for subclasses that keep the default equipment routine). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 32.0 per projectile |
| Minimum damage | 24.0 |
| Drop-off | full damage up to 30 blocks, linear to the minimum at 40 blocks, minimum beyond |
| Time to live / speed | 50 ticks / 2.25 blocks per tick; nominal reach 112.5 blocks |
| Accuracy (spread) | 0.0625, an independent uniform random offset of +-2.5 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus) and by 0.125 while zoomed |
| Armor penetration | 25% (0.25) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 10 ticks (max 2 per second) |
| Reload | 80 ticks (4 s); the gun refills at once and the player-wide `fireDelay` is set to 70 ticks (reload minus `minFiretime`) |
| Clip | 10 rounds |
| Ammo per reload | 1 x AS50 Magazine (shared item meta 17) |
| Empty returned | 1 x AS50 Magazine (Empty) (shared item meta 18) when a reload consumes ammo |
| Forced-reload refund | High Caliber Rounds x floor(rounds left / 5) (5 rounds per item) |
| Zoom | toggle on right-click; FOV x0.35, spread x0.125 while zoomed |
| Animation times | recoil 10 ticks (500 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 36 blocks, attack time 30 ticks (variance 10), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.as50` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [32.0, 24.0, 30, 40, 50, 0.0625, 0.25]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (10), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Scoped semi-auto anti-materiel rifle: FOV x0.35, zoomed spread x0.125, speed 1.5 x 1.5 = 2.25 blocks/tick, damage 32 falling to 24 between 30 and 40 blocks, penetration 25%. Recoil 10 ticks.
- AS50 Magazine (10 rounds), worth 2 High Caliber Rounds on a forced reload. A four-second reload (80 ticks).
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 10) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 10, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 10). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 500 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 4000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: AS50 Magazine`
  - `Damage: 32.0-24.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: AS50 Magazine`
  - `DamageType: Projectile`
  - `Damage: 32.0-24.0`
  - `Range: 30,40,50`
  - `Spread: 0.0625`
  - `Clipsize: 10`
  - `ReloadTime: 4.0s`
  - `ArmorPen: 25.0%`
  - `Zoom:(toggle) Mult:0.35`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderAS50` with model [`ModelAS50`](../03-gun-visuals-audio/gun-models.md#modelas50-256128-family-z-50-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#as50-as50)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/as50Texture.png`](../03-gun-visuals-audio/textures.md#texturesgunsas50texturepng-as50-as50-magazine-items) (camo 0, default): 256×128, average #33322C, dominant #101010 54%, #303030 26%, #505050 13%, 16% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.05, 0.3); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.as50Fire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 2 files, 4.93 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.as50Reload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 5.00 s, 48000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: AS50 Magazine (meta 17), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`GenericNPC` default `pickRandomGun`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) (1 of 4, only for subclasses that keep the default equipment routine). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#as50-as50); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-12`, `GUN-07`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### PDW (`pdw`)

**1. Identity.**

- Catalog row 17 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:pdw` (`GameRegistry.registerItem(..., "pdw")`); unlocalized name `techguns.item.pdw`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 20). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "PDW", lang key `techguns.item.pdw.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 18 [loaded]: grid `b r s` / `· m ·` with b = Carbon Barrel, r = Carbon Receiver, s = Carbon Gunstock, m = Advanced Magazine; recipe 18e [empty]: grid same with m = Advanced Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 6.0 per projectile |
| Minimum damage | 3.0 |
| Drop-off | full damage up to 16 blocks, linear to the minimum at 25 blocks, minimum beyond |
| Time to live / speed | 25 ticks / 1.5 blocks per tick; nominal reach 37.5 blocks |
| Accuracy (spread) | 0.03, an independent uniform random offset of +-1.2 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Extra projectiles | 1 per pull in a straight line (burst), spread 0.03 (+-1.2 degrees); each is shifted forward by i/1 of a tick of travel; one round is used per pull |
| Armor penetration | 25% (0.25) |
| Fire mode | full-auto: one round every 2 ticks (10 per second) |
| Projectiles per second | 10 x 2 |
| Reload | 40 ticks (2 s); the gun refills at once and the player-wide `fireDelay` is set to 38 ticks (reload minus `minFiretime`) |
| Clip | 20 rounds |
| Ammo per reload | 1 x Advanced Magazine (shared item meta 23) |
| Empty returned | 1 x Advanced Magazine (Empty) (shared item meta 24) when a reload consumes ammo |
| Forced-reload refund | Advanced Bullets x floor(rounds left / 6.67) (6.67 rounds per item) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms); `checkRecoil` on (animation not restarted while still playing) |
| AI stats | range 18 blocks, attack time 30 ticks (variance 10), burst 4 shots, burst delay 2 ticks |
| Config key | `WeaponStats_techguns.item.pdw` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [6.0, 3.0, 16, 25, 25, 0.03, 0.25]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (2), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Burst gun: `setShotgunSpread(1, 0.03, burst=true)`. One trigger pull uses one round but spawns 1 + 1 projectiles in a line (the second is shifted forward by one full tick of travel, 1/1), so one round is a 2-bullet burst. The HUD and tooltip multiply round counts by 2 and the tooltip reads `Damage(x2)`.
- `checkRecoil` stops the recoil animation restarting every tick. Advanced Magazine (20 rounds, worth 3 Advanced Bullets).
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 20) and `camo` (byte, selected variant, 0 to 2). Created by `onCreated` / `getSubItems` (`ammo` = 20, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 20). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: 3 variants; the Camo Bench cycles the byte (wrap-around) through `ICamoChangeable` ([09-machines-utility](../09-machines-utility/camo-bench.md)). On a tag-less stack the first switch empties the gun (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Advanced Magazine`
  - `Damage(x2): 6.0-3.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: Advanced Magazine`
  - `DamageType: Projectile`
  - `Damage(x2): 6.0-3.0`
  - `Range: 16,25,25`
  - `Spread: 0.03`
  - `Clipsize: 20`
  - `ReloadTime: 2.0s`
  - `ArmorPen: 25.0%`
- HUD ammo counter shows rounds x 2 (clip shown as 40).
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderPDW` with model [`ModelPDW`](../03-gun-visuals-audio/gun-models.md#modelpdw-6464-family-x-41-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#pdw-pdw)).
- Textures (measured, from ASSET-FACTS):
  - [`textures/guns/PDW.png`](../03-gun-visuals-audio/textures.md#texturesgunspdwpng--camo-variants-pdw) (camo 0, default): 64×64, average #262A2C, dominant #101010 55%, #303030 23%, #303050 7%, 100% opaque.
  - [`textures/guns/PDW_1.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 1, Silver): 64×64, average #212120, dominant #101010 73%, #303030 14%, #D0D0D0 5%, 100% opaque.
  - [`textures/guns/PDW_2.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 2, Dark Green): 64×64, average #090B06, dominant #101010 81%, #103010 15%, #303010 1%, 100% opaque.
- Camo variants: 3 (index 0 "Default Camo", 1 "Silver", 2 "Dark Green").
- Muzzle flash particle: `MuzzleFlashBlue` at offset (0, 0.05, -0.35); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.pdwFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.26 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.pdwReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.57 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Advanced Magazine (meta 23), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#pdw-pdw); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### LMG (`lmg`)

**1. Identity.**

- Catalog row 18 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:lmg` (`GameRegistry.registerItem(..., "lmg")`); unlocalized name `techguns.item.lmg`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 100). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "LMG", lang key `techguns.item.lmg.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 16 [loaded]: grid `· g r` / `b f s` / `· m ·` with g = "paneGlass", r = Redstone (item), b = Hardened Steel Barrel, f = Obsidian Steel Receiver, s = Plastic Gun Stock, m = LMG Magazine; recipe 16e [empty]: grid same with m = LMG Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.
- NPC equipment (not obtainable directly): [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (1 of 15 at full difficulty only). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 8.0 per projectile |
| Minimum damage | 6.0 |
| Drop-off | full damage up to 30 blocks, linear to the minimum at 40 blocks, minimum beyond |
| Time to live / speed | 50 ticks / 1.5 blocks per tick; nominal reach 75 blocks |
| Accuracy (spread) | 0.02, an independent uniform random offset of +-0.8 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus) and by 0.75 while zoomed |
| Armor penetration | 15% (0.15) |
| Fire mode | full-auto: one round every 2 ticks (10 per second) |
| Reload | 100 ticks (5 s); the gun refills at once and the player-wide `fireDelay` is set to 98 ticks (reload minus `minFiretime`) |
| Clip | 100 rounds |
| Ammo per reload | 1 x LMG Magazine (shared item meta 20) |
| Empty returned | 1 x LMG Magazine (Empty) (shared item meta 21) when a reload consumes ammo |
| Forced-reload refund | Rifle Bullets x floor(rounds left / 12.5) (12.5 rounds per item) |
| Zoom | toggle on right-click; FOV x0.75, spread x0.75 while zoomed |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 40 ticks (variance 13), burst 6 shots, burst delay 3 ticks |
| Config key | `WeaponStats_techguns.item.lmg` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [8.0, 6.0, 30, 40, 50, 0.02, 0.15]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (2), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- 100-round LMG Magazine (worth 8 Rifle Bullets), a 5-second reload, scope FOV x0.75 with zoomed spread x0.75, fire interval 2 ticks (10 rounds/s).
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 100) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 100, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 100). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 5000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: LMG Magazine`
  - `Damage: 8.0-6.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: LMG Magazine`
  - `DamageType: Projectile`
  - `Damage: 8.0-6.0`
  - `Range: 30,40,50`
  - `Spread: 0.02`
  - `Clipsize: 100`
  - `ReloadTime: 5.0s`
  - `ArmorPen: 15.000001%`
  - `Zoom:(toggle) Mult:0.75`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderLMG` with model [`ModelLMG`](../03-gun-visuals-audio/gun-models.md#modellmg-128128-family-z-46-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#lmg-lmg)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/mg2_texture.png`](../03-gun-visuals-audio/textures.md#texturesgunsmg2_texturepng-lmg-lmg-magazine-items) (camo 0, default): 128×128, average #22272A, dominant #303030 71%, #101010 22%, #505050 3%, 26% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.1, 0.02); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.lmgFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.98 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.lmgReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 5.72 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: LMG Magazine (meta 20), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (1 of 15 at full difficulty only). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#lmg-lmg); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-12`, `GUN-07`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### The Infiltrator (`m4_uq`)

**1. Identity.**

- Catalog row 19 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:m4_uq` (`GameRegistry.registerItem(..., "m4_uq")`); unlocalized name `techguns.item.m4_uq`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 30). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "The Infiltrator", lang key `techguns.item.m4_uq.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 24 [output loaded]: grid `g p g` / `w w m` / `r r i` with g = blockGlassHardened\|blockGlass, p = "plateSteel", w = "blockWool", m = M4 (damage 0), r = "dustRedstone", i = "ingotSteel".
- The recipe outputs item damage 0, i.e. a full clip (`onCreated` sets `ammo` to the clip size).
- Loot: none.
- NPC equipment (not obtainable directly): [`Commando`](../10-entities-npcs/per-mob.md#9-commando-commando) (always). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 8.0 per projectile |
| Minimum damage | 6.0 |
| Drop-off | full damage up to 25 blocks, linear to the minimum at 35 blocks, minimum beyond |
| Time to live / speed | 35 ticks / 1.5 blocks per tick; nominal reach 52.5 blocks |
| Accuracy (spread) | 0.01, an independent uniform random offset of +-0.4 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus) and by 0.5 while zoomed |
| Armor penetration | 15% (0.15) |
| Fire mode | full-auto: one round every 3 ticks (6.67 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 42 ticks (reload minus `minFiretime`) |
| Clip | 30 rounds |
| Ammo per reload | 1 x Assault Rifle Magazine (shared item meta 12) |
| Empty returned | 1 x Assault Rifle Magazine (Empty) (shared item meta 13) when a reload consumes ammo |
| Forced-reload refund | Rifle Bullets x floor(rounds left / 10) (10 rounds per item) |
| Zoom | toggle on right-click; FOV x0.5, spread x0.5 while zoomed |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 30 ticks (variance 10), burst 3 shots, burst delay 3 ticks |
| Config key | `WeaponStats_techguns.item.m4_uq` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [8.0, 6.0, 25, 35, 35, 0.01, 0.15]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (3), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- A silenced M4: `setSilenced(true)` flags every projectile as silenced (effects in [`04-projectiles-fx`](../04-projectiles-fx/index.md)), the fire sound is the quieter `guns.silencedM4Fire`, there is no muzzle flash particle, accuracy 0.010 and scope FOV x0.50 with zoomed spread x0.5. The reload sound is the shared `guns.assaultRifleReload`.
- Crafted from an M4 plus glass, steel plate, wool, redstone dust and a steel ingot; the recipe accepts any M4 and outputs a full gun, ignoring the input's ammo and camo (`GUN-15`).
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 30) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 30, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 30). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Assault Rifle Magazine`
  - `Damage: 8.0-6.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: Assault Rifle Magazine`
  - `DamageType: Projectile`
  - `Damage: 8.0-6.0`
  - `Range: 25,35,35`
  - `Spread: 0.01`
  - `Clipsize: 30`
  - `ReloadTime: 2.25s`
  - `ArmorPen: 15.000001%`
  - `Zoom:(toggle) Mult:0.5`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderM4_Infiltrator` with model [`ModelM4_UQ`](../03-gun-visuals-audio/gun-models.md#modelm4_uq-m4-infiltrator-12864-family-z-45-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#m4-infiltrator-m4_uq)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/m4_uq_Texture.png`](../03-gun-visuals-audio/textures.md#texturesgunsm4_uq_texturepng-m4-infiltrator) (camo 0, default): 128×64, average #2B2325, dominant #303030 47%, #101010 38%, #505050 4%, 49% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.silencedM4Fire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 2 files, 1.07 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.assaultRifleReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.09 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Assault Rifle Magazine (meta 12), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Commando`](../10-entities-npcs/per-mob.md#9-commando-commando) (always). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#m4-infiltrator-m4_uq); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-15`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-12`, `GUN-07`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Pulse Rifle (`pulseRifle`)

**1. Identity.**

- Catalog row 21 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:pulserifle` (`GameRegistry.registerItem(..., "pulserifle")`); unlocalized name `techguns.item.pulseRifle` (registry name differs from the unlocalized name).
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 12). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Pulse Rifle", lang key `techguns.item.pulseRifle.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 19 [loaded]: grid `d p c` / `b r s` / `· m ·` with d = "gemDiamond", p = "plateTitanium", c = "circuitElite", b = Carbon Barrel, r = Carbon Receiver, s = Carbon Gunstock, m = Advanced Magazine; recipe 19e [empty]: grid same with m = Advanced Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 10.0 per projectile |
| Minimum damage | 8.0 |
| Drop-off | full damage up to 25 blocks, linear to the minimum at 35 blocks, minimum beyond |
| Time to live / speed | 50 ticks / 2.25 blocks per tick; nominal reach 112.5 blocks |
| Accuracy (spread) | 0.024, an independent uniform random offset of +-0.96 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus) and by 0.5 while zoomed |
| Extra projectiles | 2 per pull in a straight line (burst), spread 0.015 (+-0.6 degrees); each is shifted forward by i/2 of a tick of travel; one round is used per pull |
| Armor penetration | 25% (0.25) |
| Fire mode | full-auto: one round every 7 ticks (2.86 per second) |
| Projectiles per second | 2.86 x 3 |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 38 ticks (reload minus `minFiretime`) |
| Clip | 12 rounds |
| Ammo per reload | 1 x Advanced Magazine (shared item meta 23) |
| Empty returned | 1 x Advanced Magazine (Empty) (shared item meta 24) when a reload consumes ammo |
| Forced-reload refund | Advanced Bullets x floor(rounds left / 4) (4 rounds per item) |
| Zoom | toggle on right-click; FOV x0.35, spread x0.5 while zoomed |
| Animation times | recoil 8 ticks (400 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 30 ticks (variance 10), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.pulseRifle` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [10.0, 8.0, 25, 35, 50, 0.024, 0.25]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (7), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Burst gun: `setShotgunSpread(2, 0.015, burst=true)`. One round per pull, 1 + 2 projectiles in a line (shifted forward by 1/2 and 2/2 of a tick of travel), giving a 3-bullet burst. HUD and tooltip multiply by 3; tooltip `Damage(x3)`.
- Speed 1.5 x 1.5 = 2.25 blocks/tick, scope FOV x0.35 with zoomed spread x0.5, recoil 8 ticks. The item is registered as [`pulserifle`](gun-catalog.md#table-a-identity-and-ammunition) (lower case) but its unlocalized name, lang key and config key use [`pulseRifle`](gun-catalog.md#table-a-identity-and-ammunition). Shares the Advanced Magazine with the PDW (12 rounds here).
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 12) and `camo` (byte, selected variant, 0 to 2). Created by `onCreated` / `getSubItems` (`ammo` = 12, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 12). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: 3 variants; the Camo Bench cycles the byte (wrap-around) through `ICamoChangeable` ([09-machines-utility](../09-machines-utility/camo-bench.md)). On a tag-less stack the first switch empties the gun (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 400 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Advanced Magazine`
  - `Damage(x3): 10.0-8.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: Advanced Magazine`
  - `DamageType: Projectile`
  - `Damage(x3): 10.0-8.0`
  - `Range: 25,35,50`
  - `Spread: 0.024`
  - `Clipsize: 12`
  - `ReloadTime: 2.25s`
  - `ArmorPen: 25.0%`
  - `Zoom:(toggle) Mult:0.35`
- HUD ammo counter shows rounds x 3 (clip shown as 36).
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderPulseRifle` with model [`ModelPulseRifle`](../03-gun-visuals-audio/gun-models.md#modelpulserifle-12864-family-x-72-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#pulse-rifle-pulserifle-field-pulserifle)).
- Textures (measured, from ASSET-FACTS):
  - [`textures/guns/pulseRifle.png`](../03-gun-visuals-audio/textures.md#texturesgunspulseriflepng--camo-variants-pulse-rifle) (camo 0, default): 128×64, average #333A3C, dominant #101010 57%, #303030 12%, #D0F0F0 9%, 99% opaque.
  - [`textures/guns/pulseRifle_1.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 1, Gray): 128×64, average #2E2E2E, dominant #101010 71%, #D0D0D0 9%, #303030 6%, 99% opaque.
  - [`textures/guns/pulseRifle_2.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 2, Dark Green): 128×64, average #080A06, dominant #101010 87%, #103010 10%, #303010 2%, 99% opaque.
- Camo variants: 3 (index 0 "Default Camo", 1 "Gray", 2 "Dark Green").
- Muzzle flash particle: `MuzzleFlashBlue` at offset (0, 0.025, -0.05); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.pulserifleFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 4 files, 2.75 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.pulserifleReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.22 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Advanced Magazine (meta 23), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#pulse-rifle-pulserifle-field-pulserifle); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-12`, `GUN-07`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Minigun (`minigun`)

**1. Identity.**

- Catalog row 24 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:minigun` (`GameRegistry.registerItem(..., "minigun")`); unlocalized name `techguns.item.minigun`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 200). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Minigun", lang key `techguns.item.minigun.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 17 [loaded]: grid `b b e` / `b b r` / `b b m` with b = Hardened Steel Barrel (×6), e = Electric Engine, r = Obsidian Steel Receiver, m = Minigun Ammo Drum; recipe 17e [empty]: grid same with m = Minigun Ammo Drum (empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.
- NPC equipment (not obtainable directly): [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 5.0 per projectile |
| Minimum damage | 3.0 |
| Drop-off | full damage up to 20 blocks, linear to the minimum at 40 blocks, minimum beyond |
| Time to live / speed | 50 ticks / 1.5 blocks per tick; nominal reach 75 blocks |
| Accuracy (spread) | 0.025, an independent uniform random offset of +-1 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | 15% (0.15) |
| Fire mode | full-auto: `minFiretime` 0, one round every tick (20 per second) |
| Reload | 100 ticks (5 s); the gun refills at once and the player-wide `fireDelay` is set to 100 ticks (reload minus `minFiretime`) |
| Clip | 200 rounds |
| Ammo per reload | 1 x Minigun Ammo Drum (shared item meta 46) |
| Empty returned | 1 x Minigun Ammo Drum (empty) (shared item meta 47) when a reload consumes ammo |
| Forced-reload refund | Rifle Bullets x floor(rounds left / 12.5) (12.5 rounds per item) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms); `checkRecoil` on (animation not restarted while still playing) |
| AI stats | range 24 blocks, attack time 40 ticks (variance 13), burst 10 shots, burst delay 1 ticks |
| Config key | `WeaponStats_techguns.item.minigun` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [5.0, 3.0, 20, 40, 50, 0.025, 0.15]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (0), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- `minFiretime` is 0, so a round leaves every tick (20 per second) while the trigger is held; `checkRecoil` keeps the recoil animation from restarting every tick. A spin-up start sound (`guns.minigunStart`) and a 2-pellet spread exist only as commented-out builder calls.
- 200-round Minigun Ammo Drum, worth 16 Rifle Bullets (12.5 rounds each) on a forced reload.
- The turret offsets `(0, 0.2, 0)` and `(0, 0.6, 0)` place the gun and its muzzle flash on a turret.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 200) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 200, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 200). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 5000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Minigun Ammo Drum`
  - `Damage: 5.0-3.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: Minigun Ammo Drum`
  - `DamageType: Projectile`
  - `Damage: 5.0-3.0`
  - `Range: 20,40,50`
  - `Spread: 0.025`
  - `Clipsize: 200`
  - `ReloadTime: 5.0s`
  - `ArmorPen: 15.000001%`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer `RenderMinigun` with model [`ModelMinigun`](../03-gun-visuals-audio/gun-models.md#modelminigun-128128-family-x-73-boxes-techne-2-parts) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#minigun-minigun)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/minigun.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×128, average #2A2929, dominant #101010 70%, #707070 5%, #909090 3%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, -0.8, 0.4); turret offsets: gun (0, 0.2, 0), muzzle (0, 0.6, 0).

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.minigunFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 4 files, 1.00 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.minigunReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 6.24 s, 32000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Minigun Ammo Drum (meta 46), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0.2, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#minigun-minigun); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### AUG (`aug`)

**1. Identity.**

- Catalog row 30 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:aug` (`GameRegistry.registerItem(..., "aug")`); unlocalized name `techguns.item.aug`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 30). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "AUG", lang key `techguns.item.aug.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 26 [loaded]: grid `g p g` / `b f s` / `i · m` with g = "paneGlass", p = "plateSteel", b = Hardened Steel Barrel, f = Steel Receiver, s = Plastic Gun Stock, i = "sheetPlastic", m = Assault Rifle Magazine; recipe 26e [empty]: grid same with m = Assault Rifle Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 8.0 per projectile |
| Minimum damage | 7.0 |
| Drop-off | full damage up to 25 blocks, linear to the minimum at 40 blocks, minimum beyond |
| Time to live / speed | 35 ticks / 1.5 blocks per tick; nominal reach 52.5 blocks |
| Accuracy (spread) | 0.01, an independent uniform random offset of +-0.4 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus) and by 0.5 while zoomed |
| Armor penetration | 15% (0.15) |
| Fire mode | full-auto: one round every 3 ticks (6.67 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 42 ticks (reload minus `minFiretime`) |
| Clip | 30 rounds |
| Ammo per reload | 1 x Assault Rifle Magazine (shared item meta 12) |
| Empty returned | 1 x Assault Rifle Magazine (Empty) (shared item meta 13) when a reload consumes ammo |
| Forced-reload refund | Rifle Bullets x floor(rounds left / 10) (10 rounds per item) |
| Zoom | toggle on right-click; FOV x0.5, spread x0.5 while zoomed |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 30 ticks (variance 10), burst 3 shots, burst delay 3 ticks |
| Config key | `WeaponStats_techguns.item.aug` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [8.0, 7.0, 25, 40, 35, 0.01, 0.15]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (3), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Plain full-auto `GenericGun` with the shared Assault Rifle Magazine. Scope FOV x0.50 with zoomed spread x0.5. Five texture variants (the most of any gun, with the M4).
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 30) and `camo` (byte, selected variant, 0 to 4). Created by `onCreated` / `getSubItems` (`ammo` = 30, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 30). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: 5 variants; the Camo Bench cycles the byte (wrap-around) through `ICamoChangeable` ([09-machines-utility](../09-machines-utility/camo-bench.md)). On a tag-less stack the first switch empties the gun (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Assault Rifle Magazine`
  - `Damage: 8.0-7.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: Assault Rifle Magazine`
  - `DamageType: Projectile`
  - `Damage: 8.0-7.0`
  - `Range: 25,40,35`
  - `Spread: 0.01`
  - `Clipsize: 30`
  - `ReloadTime: 2.25s`
  - `ArmorPen: 15.000001%`
  - `Zoom:(toggle) Mult:0.5`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer [`RenderAUG`](../03-gun-visuals-audio/rendering-framework.md#1-registration-client-proxy) with model [`ModelAUG`](../03-gun-visuals-audio/gun-models.md#modelaug-12864-family-z-24-boxes-tabula-2-parts) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#aug-aug)).
- Textures (measured, from ASSET-FACTS):
  - [`textures/guns/AugTexture.png`](../03-gun-visuals-audio/textures.md#texturesgunsaugtexturepng--camo-variants-aug) (camo 0, default): 128×64, average #1F372C, dominant #305030 57%, #101010 24%, #307050 4%, 37% opaque.
  - [`textures/guns/AugTexture_1.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 1, Gray): 128×64, average #212121, dominant #303030 64%, #101010 25%, #505050 5%, 37% opaque.
  - [`textures/guns/AugTexture_2.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 2, Wood): 128×64, average #18371F, dominant #105030 43%, #101010 24%, #103010 17%, 37% opaque.
  - [`textures/guns/AugTexture_3.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 3, Sand): 128×64, average #8B8D60, dominant #D0D090 38%, #101010 24%, #B0B070 9%, 37% opaque.
  - [`textures/guns/AugTexture_4.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 4, Nether): 128×64, average #520606, dominant #701010 34%, #101010 24%, #501010 17%, 37% opaque.
- Camo variants: 5 (index 0 "Default Camo", 1 "Gray", 2 "Wood", 3 "Sand", 4 "Nether").
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.025, 0); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.augFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 2.44 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.augReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 4.03 s, 48000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Assault Rifle Magazine (meta 12), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#aug-aug); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-12`, `GUN-07`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### SCAR (`scar`)

**1. Identity.**

- Catalog row 41 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `GenericGun` (`java/techguns/items/guns/`), built by the `GenericGun` constructor family.
- Registry name `Techguns:scar` (`GameRegistry.registerItem(..., "scar")`); unlocalized name `techguns.item.scar`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 20). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "SCAR", lang key `techguns.item.scar.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 33 [loaded]: grid `g p d` / `b r s` / `· m ·` with g = blockGlassHardened\|blockGlass, p = "sheetPlastic", d = "gemDiamond", b = Hardened Steel Barrel, r = Obsidian Steel Receiver, s = Plastic Gun Stock, m = Assault Rifle Magazine; recipe 33e [empty]: grid same with m = Assault Rifle Magazine (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 12.0 per projectile |
| Minimum damage | 10.0 |
| Drop-off | full damage up to 30 blocks, linear to the minimum at 40 blocks, minimum beyond |
| Time to live / speed | 50 ticks / 1.5 blocks per tick; nominal reach 75 blocks |
| Accuracy (spread) | 0.015, an independent uniform random offset of +-0.6 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus) and by 0.5 while zoomed |
| Armor penetration | 25% (0.25) |
| Fire mode | full-auto: one round every 4 ticks (5 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 41 ticks (reload minus `minFiretime`) |
| Clip | 20 rounds |
| Ammo per reload | 1 x Assault Rifle Magazine (shared item meta 12) |
| Empty returned | 1 x Assault Rifle Magazine (Empty) (shared item meta 13) when a reload consumes ammo |
| Forced-reload refund | Rifle Bullets x floor(rounds left / 6.67) (6.67 rounds per item) |
| Zoom | toggle on right-click; FOV x0.65, spread x0.5 while zoomed |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 30 ticks (variance 10), burst 5 shots, burst delay 2 ticks |
| Config key | `WeaponStats_techguns.item.scar` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [12.0, 10.0, 30, 40, 50, 0.015, 0.25]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (4), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Plain full-auto `GenericGun` using the shared Assault Rifle Magazine but loading only 20 of its 30 rounds (6.67 rounds per Rifle Bullets item for refunds). Scope FOV x0.65 with zoomed spread x0.5, damage 12 falling to 10 between 30 and 40 blocks.
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 20) and `camo` (byte, selected variant, 0 to 1). Created by `onCreated` / `getSubItems` (`ammo` = 20, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 20). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: 2 variants; the Camo Bench cycles the byte (wrap-around) through `ICamoChangeable` ([09-machines-utility](../09-machines-utility/camo-bench.md)). On a tag-less stack the first switch empties the gun (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Assault Rifle Magazine`
  - `Damage: 12.0-10.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: Assault Rifle Magazine`
  - `DamageType: Projectile`
  - `Damage: 12.0-10.0`
  - `Range: 30,40,50`
  - `Spread: 0.015`
  - `Clipsize: 20`
  - `ReloadTime: 2.25s`
  - `ArmorPen: 25.0%`
  - `Zoom:(toggle) Mult:0.65`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GenericProjectile` (entity id 1)](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything). Damage source: `tg_bullet / PROJECTILE`.
- Item renderer [`RenderScar`](../03-gun-visuals-audio/rendering-framework.md#1-registration-client-proxy) with model [`ModelScar`](../03-gun-visuals-audio/gun-models.md#modelscar-12864-used-with-a-256128-png-family-z-45-boxes-tabula-2-parts) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#scar-scar)).
- Textures (measured, from ASSET-FACTS):
  - [`textures/guns/scar_texture.png`](../03-gun-visuals-audio/textures.md#texturesgunsscar_texturepng-scar_texture_1png-scar) (camo 0, default): 256×128, average #757559, dominant #B0B070 48%, #303030 19%, #101010 8%, 45% opaque.
  - [`textures/guns/scar_texture_1.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 1, Black): 256×128, average #292929, dominant #303030 83%, #101010 11%, #505050 2%, 45% opaque.
- Camo variants: 2 (index 0 "Default Camo", 1 "Black").
- Muzzle flash particle: `MuzzleFlashYellow` at offset (0, 0.03, 0); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.scarFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.60 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.scarReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.53 s, 48000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Assault Rifle Magazine (meta 12), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#scar-scar); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-12`, `GUN-07`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

