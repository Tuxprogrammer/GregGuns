# 02-guns: Entity sheets: launchers, tool guns and grenades

Each item below has a full entity sheet with fields 1 to 10 of the [conventions](../CONVENTIONS.md#entity-sheet): identity, obtaining, numbers, behavior, persistence and sync, UI, visuals, audio, relationships and bugs. The catalog tables ([gun-catalog.md](gun-catalog.md)) remain the quick reference; shared mechanics are in [gun-lifecycle.md](gun-lifecycle.md), the shared rules for these sheets are in [sheets-index.md](sheets-index.md), and class-level detail is in [per-gun-behavior.md](per-gun-behavior.md).

Items in this file: [Rocket Launcher](#rocket-launcher-rocketlauncher), [Grim Reaper](#grim-reaper-grimreaper), [Power Hammer](#power-hammer-powerhammer), [Nuke Launcher](#nuke-launcher-nukelauncher), [Power Hammer (Advanced)](#power-hammer-advanced-powerhammeradv), [Grenade Launcher](#grenade-launcher-grenadelauncher), [Chainsaw](#chainsaw-chainsaw), [Stielgranate](#stielgranate-stielgranate), [Frag Grenade](#frag-grenade-fraggrenade), [Flashbang](#flashbang-flashbang).

### Rocket Launcher (`rocketlauncher`)

**1. Identity.**

- Catalog row 9 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class [`RocketLauncher`](sheets-index.md#all-items-at-a-glance) (`java/techguns/items/guns/`).
- Registry name `Techguns:rocketlauncher` (`GameRegistry.registerItem(..., "rocketlauncher")`); unlocalized name `techguns.item.rocketlauncher`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 1). Creative tab [`Techguns`](../01-core/lifecycle-and-registration.md#lifecycle-overview) (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Rocket Launcher", lang key `techguns.item.rocketlauncher.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 15 [loaded only (contains 1 rocket)]: grid `r b b` / `· f ·` with r = Rocket, b = Hardened Steel Barrel, f = Steel Receiver.
- The recipe outputs item damage 0, i.e. a full clip (`onCreated` sets `ammo` to the clip size).
- Loot ([`TGItems`](../06-items/item-catalog.md) chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): `techgunsWeaponchestT3`: count 1, weight 1. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.
- NPC equipment (not obtainable directly): [`SuperMutantBasic`](../10-entities-npcs/per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (1 of 5; inherited by Heavy and Elite); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (1 of 15 at full difficulty only). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage | 50.0 (explosion damage per rocket; scaled by the NPC `damagebonus`); minimum and drop-off unused by rockets (config values 50.0 / 100 / 100 are inert) |
| Time to live / speed | 100 ticks / 1.0 blocks per tick (nominal reach 100 blocks; gravity 0.01 makes it fall short) |
| Radius / gravity | 1.75 blocks / 0.01 (config indices 7 and 8) |
| Accuracy (spread) | 0.05, an independent uniform random offset of +-2 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 10 ticks (2 per second) |
| Reload | 40 ticks (2 s); the gun refills at once and the player-wide `fireDelay` is set to 30 ticks (reload minus `minFiretime`) |
| Clip | 1 round |
| Ammo per reload | 1 x Rocket (shared item meta 14) |
| Empty returned | none |
| Forced-reload refund | none: the remaining rounds are discarded when a forced reload is used on a magazine-less gun (`GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 10 ticks (500 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 80 ticks (variance 26), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.rocketlauncher` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [50.0, 50.0, 100, 100, 100, 0.05, 0.0, 1.75, 0.01]; indices ignored by this gun: 1 DamageMin, 2 DamageDropStart, 3 DamageDropEnd |
| Other config keys | [`DamageFactorNPC`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (1.0), [`DamageTurretToPlayer`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (0.5), [`DamagePvP`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (0.5), [`ArmorPenetrationMultiplierNPCtoPlayer`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) and [`ArmorPenetrationMultiplierPvP`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (1.0) scale hits; [`ForceSafeModeForNonOps`](../01-core/config.md#category-general) (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends [`PacketShootGun`](../01-core/networking.md#8--packetshootgun) and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (10), broadcasts [`GunFiredMessage`](../01-core/networking.md#1--gunfiredmessage) within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- A [`GenericGunRadiusGrav`](gun-catalog.md#weapon-stats-config) subclass: radius 1.75 and gravity 0.01 are the two extra config values (indices 7 and 8). `spawnProjectile` creates a [`RocketProjectile`](../04-projectiles-fx/projectiles.md#34-rocketprojectile-and-nukerocketprojectile) with `damage x damagebonus`, radius, fixed speed 1.0, time-to-live, spread, gravity, block damage (player with safe mode off; never for NPCs) and penetration.
- Rockets do not use damage drop-off (the config minimum and drop distances are inert). Explosion damage, knockback x3 and block damage are in [`04-projectiles-fx`](../04-projectiles-fx/index.md).
- Clip 1: each rocket needs one reload (one Rocket item). No empty container. A dispenser loaded with the Rocket item fires a rocket using this gun's damage and radius ([`06-items`](../06-items/index.md)).
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 1) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 1, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 1). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 500 ms, attackType, checkRecoil, akimbo, doubleShot); [`ReloadStartedMessage`](../01-core/networking.md#3--reloadstartedmessage) (server to clients within 100 blocks: 2000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in [`TechgunsExtendedPlayerProperties`](../01-core/player-state.md#techgunsextendedplayerproperties): `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Rocket`
  - `Damage: 50.0-50.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark red):
  - `Ammo: Rocket`
  - `DamageType: Explosion`
  - `Damage: 50.0`
  - `Range: 100,100,100`
  - `Spread: 0.05`
  - `Clipsize: 1`
  - `ReloadTime: 2.0s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`RocketProjectile` (entity id 6)](../04-projectiles-fx/projectiles.md#34-rocketprojectile-and-nukerocketprojectile). Damage source: `tg_explosion / EXPLOSION`.
- Item renderer `RenderRocketLauncher` with model [`ModelRocketLauncher`](../03-gun-visuals-audio/gun-models.md#modelrocketlauncher-rocket-and-nuke-launcher-12864-family-x-29-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#rocket-launcher-rocketlauncher-and-nuke-launcher-nukelauncher)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/rocketlauncher.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×64, average #272728, dominant #101010 59%, #303030 11%, #507070 5%, 96% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun (0, -0.7, -0.2), muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.rocketFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 2.06 s, 48000 Hz, stereo, sounds.json category `player` |
| `guns.rocketReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.53 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Rocket (meta 14), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`SuperMutantBasic`](../10-entities-npcs/per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (1 of 5; inherited by Heavy and Elite); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (1 of 15 at full difficulty only). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from [`GenericNPC`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava), second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot ([`SlotTechgun`](../09-machines-utility/turret.md)); [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, -0.7, -0.2) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Recipes and machines: Four of them are the input of the Grim Reaper (recipe 23). A dispenser loaded with the Rocket item uses its damage and radius.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#rocket-launcher-rocketlauncher-and-nuke-launcher-nukelauncher); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Grim Reaper (`grimreaper`)

**1. Identity.**

- Catalog row 10 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `RocketLauncher` (`java/techguns/items/guns/`).
- Registry name `Techguns:grimreaper` (`GameRegistry.registerItem(..., "grimreaper")`); unlocalized name `techguns.item.grimreaper`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 4). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Grim Reaper", lang key `techguns.item.grimreaper.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 23 [output loaded]: grid `r r ·` / `r r c` with r = Rocket Launcher (damage 0) ×4, c = Carbon Receiver.
- The recipe outputs item damage 0, i.e. a full clip (`onCreated` sets `ammo` to the clip size).
- Loot: none.
- NPC equipment (not obtainable directly): [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7); [`GenericNPC` default `pickRandomGun`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) (1 of 4, only for subclasses that keep the default equipment routine). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage | 50.0 (explosion damage per rocket; scaled by the NPC `damagebonus`); minimum and drop-off unused by rockets (config values 50.0 / 100 / 100 are inert) |
| Time to live / speed | 100 ticks / 1.0 blocks per tick (nominal reach 100 blocks; gravity 0.01 makes it fall short) |
| Radius / gravity | 1.75 blocks / 0.01 (config indices 7 and 8) |
| Accuracy (spread) | 0.05, an independent uniform random offset of +-2 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 6 ticks (3.33 per second) |
| Reload | 40 ticks (2 s); the gun refills at once and the player-wide `fireDelay` is set to 34 ticks (reload minus `minFiretime`) |
| Clip | 4 rounds |
| Ammo per reload | up to 4 x Rocket (shared item meta 14), one round each (all-or-nothing per item, loads as many as the inventory has) |
| Empty returned | none |
| Forced-reload refund | none needed: a shell-by-shell gun keeps its loaded rounds on a forced reload and tops up with up to 4 minus the rounds left in items |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 10 ticks (500 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 120 ticks (variance 40), burst 4 shots, burst delay 30 ticks |
| Config key | `WeaponStats_techguns.item.grimreaper` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [50.0, 50.0, 100, 100, 100, 0.05, 0.0, 1.75, 0.01]; indices ignored by this gun: 1 DamageMin, 2 DamageDropStart, 3 DamageDropEnd |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (6), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Same class and projectile as the Rocket Launcher, but a 4-tube launcher: `ammoCount` 4, so one reload consumes up to 4 Rocket items and gives that many rounds. One rocket leaves per trigger pull (6-tick interval).
- Created by crafting four Rocket Launchers with a Carbon Receiver; the recipe ignores their ammo, so four empty launchers yield a loaded Grim Reaper (`GUN-15`).
- Commented-out code once let shift-right-click toggle block damage in NBT; only the safe-mode flag remains.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 4) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 4, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 4). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 500 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: 4x Rocket`
  - `Damage: 50.0-50.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark red):
  - `Ammo: 4x Rocket`
  - `DamageType: Explosion`
  - `Damage: 50.0`
  - `Range: 100,100,100`
  - `Spread: 0.05`
  - `Clipsize: 4`
  - `ReloadTime: 2.0s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`RocketProjectile` (entity id 6)](../04-projectiles-fx/projectiles.md#34-rocketprojectile-and-nukerocketprojectile). Damage source: `tg_explosion / EXPLOSION`.
- Item renderer `RenderGrimReaper` with model [`ModelGrimReaper`](../03-gun-visuals-audio/gun-models.md#modelgrimreaper-12864-family-x-59-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#grim-reaper-grimreaper)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/grimreaper.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×64, average #727461, dominant #909070 32%, #505050 11%, #707050 10%, 96% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun (0, -0.7, -0.4), muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.rocketFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 2.06 s, 48000 Hz, stereo, sounds.json category `player` |
| `guns.rocketReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.53 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Rocket (meta 14), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7); [`GenericNPC` default `pickRandomGun`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) (1 of 4, only for subclasses that keep the default equipment routine). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, -0.7, -0.4) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#grim-reaper-grimreaper); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-15`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Power Hammer (`powerHammer`)

**1. Identity.**

- Catalog row 26 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `PowerHammer` (`java/techguns/items/guns/`).
- Registry name `Techguns:powerHammer` (`GameRegistry.registerItem(..., "powerHammer")`); unlocalized name `techguns.item.powerHammer`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 300). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Power Hammer", lang key `techguns.item.powerHammer.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 20 [loaded]: grid `· m ·` / `s p r` / `· m c` with m = Mechanical Parts (×2), s = "plateIron", p = Piston (vanilla block), r = Iron Receiver, c = Compressed Air Tank; recipe 20e [empty]: grid same with c = Compressed Air Tank (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage | air used x 2.5, so 2.5 to 25.0 (config `DamageMax` 2.5 is the per-air factor) |
| Minimum damage | 2.5, not scaled by air used (`GUN-09`) |
| Drop-off | full damage to 3 blocks, minimum from 3 blocks (a 4 x 4 block box reaching about 3 blocks) |
| Time to live / speed | 2 ticks / 1.5 blocks per tick; nominal reach 3 blocks |
| Accuracy | 0.0 (fixed by the constructor, and ignored on release) |
| Armor penetration | none (0) |
| Fire mode | right-click charge (`fullChargeTime` 20 ticks), release to fire; `minFiretime` 4 sets the fire delay at charge start; left click is vanilla melee and mining |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 41 ticks (reload minus `minFiretime`) |
| Clip | 300 rounds |
| Ammo per reload | 1 x Compressed Air Tank (shared item meta 28) |
| Empty returned | 1 x Compressed Air Tank (Empty) (shared item meta 29) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | none |
| Animation times | recoil 12 ticks (600 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 3 blocks, attack time 30 ticks (variance 10), burst 0 shots, burst delay 0 ticks |
| Tool stats | classes: pickaxe 2, shovel 2 (the internal `default` entry takes the first level); dig speed 12.0; melee damage 6.0 with ammo / 2.0 without; 1 round per block mined or per melee hit |
| Config key | `WeaponStats_techguns.item.powerHammer` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [2.5, 2.5, 3, 3, 2, 0.0, 0.0]; indices ignored by this gun: 5 Accuracy |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Charge tool (`PowerHammer` extends [`GenericGunCharge`](per-gun-behavior.md#genericguncharge-genericgunchargejava), `fullChargeTime` 20). The constructor fixes TTL 2 and accuracy 0; left click is vanilla melee and mining (`setShootWithLeftClick(false)`).
- Right-click and hold: needs air and a free `fireDelay`, sets `fireDelay` to 4 and plays `guns.powerhammerCharge`. On release: air used = floor(10 x charge) clamped to 1..10 and to the air left (none consumed in creative); a [`PowerHammerProjectile`](../04-projectiles-fx/projectiles.md#311-powerhammerprojectile) is spawned with damage = air used x 2.5 (2.5 to 25), speed 1.5, TTL 2 (about 3 blocks), a 4 x 4 block collision box, the charge value and the gun's drop-off (3 to 3, minimum 2.5, not scaled by air, `GUN-09`). The fire sound `guns.powerhammerFire` plays; no `GunFiredMessage` is sent (`GUN-31`).
- Melee: attribute damage 6 with air, 2 without; with air the source has gore 1.0, armor penetration 0.25, knockback x1.5; each hit uses 1 air. Mining: pickaxe and shovel level 2, dig speed 12 for pickaxe, shovel or class-less blocks while air remains, otherwise 1; each block broken with air uses 1 air and plays `guns.powerhammerImpactGround`.
- Swing: with air the client starts a 250 ms recoil animation and the vanilla swing is canceled; the server sends `guns.powerhammerSwing` to players within 50 blocks at most every 5 ticks and not while a container GUI is open.
- NPC use fires a plain bullet instead of a hammer blast (`GUN-08`). Harvest check reads metadata 0 only (`GUN-27`).
- Client / server: right-click handlers run on both sides; the server is authoritative for projectiles and ammo; the shot is not broadcast to other players (`GUN-31`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 300) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 300, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 300). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag) (not used: this tool fires through right-click handlers); `GunFiredMessage` is never sent for this tool (the charged release is not broadcast, `GUN-31`); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms for the normal reload path; the right-click charge-gun reload sends none (`GUN-10`)). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Compressed Air Tank`
  - `Damage: 2.5-2.5`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark gray):
  - `Ammo: Compressed Air Tank`
  - `DamageType: Physical`
  - `Damage: 2.5`
  - `Range: 3,3,2`
  - `Spread: 0.0`
  - `Clipsize: 300`
  - `ReloadTime: 2.25s`
  - `Toolclasses`
  - `  default : 2`
  - `  pickaxe : 2`
  - `  shovel : 2`
  - The tool-class lines come from a `HashMap`, so their order is not defined. Vanilla also appends its own attribute block ("+6 Attack Damage" for full, "+2" for empty) because the tool guns expose an attack-damage modifier; that line is generated by vanilla 1.7.10, not by Techguns code.
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`PowerHammerProjectile` (entity id 15)](../04-projectiles-fx/projectiles.md#311-powerhammerprojectile). Damage source: `powerhammer / PHYSICAL`.
- Item renderer `RenderPowerHammer` with model [`ModelPowerHammer`](../03-gun-visuals-audio/gun-models.md#modelpowerhammer-6464-family-x-40-boxes-2-parts) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#power-hammer-powerhammer-and-advanced-power-hammer-powerhammeradv)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/powerHammer.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×64, average #2B2522, dominant #101010 51%, #503030 16%, #505050 6%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun none, muzzle none.
- Recoil: 12 ticks (600 ms) for the charged shot; the left-click swing starts a separate 250 ms recoil.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.powerhammerFire` | charge release (server) | 1.0 / 1.0 (`world.playSoundAtEntity`) | 3 files, 2.20 to 3.19 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.powerhammerReload` | reload started from the right-click path | 1.0 / 1.0 (`world.playSoundAtEntity`, not the reload helper, `GUN-10`) | 1 file, 3.43 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.powerhammerCharge` | charge start (right-click with air and a free fire delay) | volume 1.0, pitch 1.0, `world.playSoundAtEntity` | 3 files, 2.53 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.powerhammerSwing` | left-click swing with air | packet [`PacketPlaySound`](../01-core/networking.md#9--packetplaysound), distance/volume 1.0, pitch 1.0, category GUN_FIRE, to players within 50 blocks, at most once per 5 ticks | 3 files, 1.39 to 1.67 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.powerhammerImpactGround` | a block is broken with air left | `world.playSoundEffect` at the block center, volume 1.0, pitch 1.0 | 4 files, 1.55 to 1.80 s, 32000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Compressed Air Tank (meta 28), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Recipes and machines: Input of the Power Hammer (Advanced) recipe.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#power-hammer-powerhammer-and-advanced-power-hammer-powerhammeradv); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-08`, `GUN-09`, `GUN-10`, `GUN-27`, `GUN-28`, `GUN-31`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Nuke Launcher (`nukelauncher`)

**1. Identity.**

- Catalog row 27 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class [`NukeLauncher`](index.md#coverage) (`java/techguns/items/guns/`).
- Registry name `Techguns:nukelauncher` (`GameRegistry.registerItem(..., "nukelauncher")`); unlocalized name `techguns.item.nukelauncher`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 1). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Nuke Launcher", lang key `techguns.item.nukelauncher.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 30 [loaded]: grid `· p p` / `r b b` / `· f ·` with p = "plateLead", r = Rocket (Tactical Nuke), b = Hardened Steel Barrel, f = Carbon Receiver; recipe 30e [empty]: grid `· p p` / `· b b` / `· f ·` with as above without the rocket.
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage | 250.0 (scaled by the NPC `damagebonus`); minimum and drop-off unused |
| Time to live / speed | 100 ticks / 1.0 blocks per tick (nominal reach 100 blocks; gravity 0.01 makes it fall short) |
| Radius / gravity | 10.0 blocks / 0.01 (config indices 7 and 8) |
| Accuracy (spread) | 0.05, an independent uniform random offset of +-2 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 10 ticks (2 per second) |
| Reload | 40 ticks (2 s); the gun refills at once and the player-wide `fireDelay` is set to 30 ticks (reload minus `minFiretime`) |
| Clip | 1 round |
| Ammo per reload | 1 x Rocket (Tactical Nuke) (shared item meta 105) |
| Empty returned | none |
| Forced-reload refund | none: the remaining rounds are discarded when a forced reload is used on a magazine-less gun (`GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 10 ticks (500 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 80 ticks (variance 26), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.nukelauncher` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [250.0, 250.0, 100, 100, 100, 0.05, 0.0, 10.0, 0.01]; indices ignored by this gun: 1 DamageMin, 2 DamageDropStart, 3 DamageDropEnd |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (10), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Same shape as the Rocket Launcher with radius 10, gravity 0.01 and damage 250, spawning a [`NukeRocketProjectile`](../04-projectiles-fx/projectiles.md#34-rocketprojectile-and-nukerocketprojectile) (explosion knockback x10). Ammo is the Rocket (Tactical Nuke); clip 1; no drop-off (config minimum and drop distances inert).
- A dispenser holding the Tactical Nuke Rocket fires a nuke rocket with this gun's damage and radius ([`06-items`](../06-items/index.md)).
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 1) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 1, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 1). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 500 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Rocket (Tactical Nuke)`
  - `Damage: 250.0-250.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark red):
  - `Ammo: Rocket (Tactical Nuke)`
  - `DamageType: Explosion`
  - `Damage: 250.0`
  - `Range: 100,100,100`
  - `Spread: 0.05`
  - `Clipsize: 1`
  - `ReloadTime: 2.0s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`NukeRocketProjectile` (entity id 7)](../04-projectiles-fx/projectiles.md#34-rocketprojectile-and-nukerocketprojectile). Damage source: `tg_explosion / EXPLOSION (knockback 10)`.
- Item renderer `RenderRocketLauncher` with model `ModelRocketLauncher` ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#rocket-launcher-rocketlauncher-and-nuke-launcher-nukelauncher)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/nukerocketlauncher.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×64, average #241D0E, dominant #101010 65%, #505030 5%, #303010 3%, 96% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun (0, -0.7, -0.2), muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.rocketFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 2.06 s, 48000 Hz, stereo, sounds.json category `player` |
| `guns.rocketReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.53 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Rocket (Tactical Nuke) (meta 105), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, -0.7, -0.2) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Recipes and machines: A dispenser loaded with the Tactical Nuke Rocket uses its damage and radius.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#rocket-launcher-rocketlauncher-and-nuke-launcher-nukelauncher); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Power Hammer (Advanced) (`powerHammerAdv`)

**1. Identity.**

- Catalog row 28 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `PowerHammer` (`java/techguns/items/guns/`).
- Registry name `Techguns:powerHammerAdv` (`GameRegistry.registerItem(..., "powerHammerAdv")`); unlocalized name `techguns.item.powerHammerAdv`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 300). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Power Hammer (Advanced)", lang key `techguns.item.powerHammerAdv.name`.

**2. Obtaining.**

- Crafting: shapeless [`ShapelessOreRecipeCopyNBT`](recipes.md#shapeless--custom-recipes): one Power Hammer + 3 diamonds (`gemDiamond`); the result copies the hammer's NBT (ammo, camo). See [recipes.md](recipes.md#shapeless--custom-recipes).
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage | air used x 2.5, so 2.5 to 25.0 (config `DamageMax` 2.5 is the per-air factor) |
| Minimum damage | 2.5, not scaled by air used (`GUN-09`) |
| Drop-off | full damage to 2 blocks, minimum from 2 blocks (a 4 x 4 block box reaching about 3 blocks) |
| Time to live / speed | 2 ticks / 1.5 blocks per tick; nominal reach 3 blocks |
| Accuracy | 0.0 (fixed by the constructor, and ignored on release) |
| Armor penetration | none (0) |
| Fire mode | right-click charge (`fullChargeTime` 20 ticks), release to fire; `minFiretime` 4 sets the fire delay at charge start; left click is vanilla melee and mining |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 41 ticks (reload minus `minFiretime`) |
| Clip | 300 rounds |
| Ammo per reload | 1 x Compressed Air Tank (shared item meta 28) |
| Empty returned | 1 x Compressed Air Tank (Empty) (shared item meta 29) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | none |
| Animation times | recoil 12 ticks (600 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 3 blocks, attack time 30 ticks (variance 10), burst 0 shots, burst delay 0 ticks |
| Tool stats | classes: pickaxe 3, shovel 3 (the internal `default` entry takes the first level); dig speed 15.0; melee damage 7.0 with ammo / 2.0 without; 1 round per block mined or per melee hit |
| Config key | `WeaponStats_techguns.item.powerHammerAdv` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [2.5, 2.5, 2, 2, 2, 0.0, 0.0]; indices ignored by this gun: 5 Accuracy |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Identical to the Power Hammer except: melee damage 7 with air (2 without), tool classes pickaxe and shovel level 3, dig speed 15, and no `setDamageDrop` (so the drop-off is 2 to 2 with minimum equal to the base 2.5).
- Crafted shapelessly from a Power Hammer plus 3 diamonds with all NBT (ammo and camo) copied from the hammer.
- Client / server: right-click handlers run on both sides; the server is authoritative for projectiles and ammo; the shot is not broadcast to other players (`GUN-31`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 300) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 300, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 300). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag) (not used: this tool fires through right-click handlers); `GunFiredMessage` is never sent for this tool (the charged release is not broadcast, `GUN-31`); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms for the normal reload path; the right-click charge-gun reload sends none (`GUN-10`)). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Compressed Air Tank`
  - `Damage: 2.5-2.5`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark gray):
  - `Ammo: Compressed Air Tank`
  - `DamageType: Physical`
  - `Damage: 2.5`
  - `Range: 2,2,2`
  - `Spread: 0.0`
  - `Clipsize: 300`
  - `ReloadTime: 2.25s`
  - `Toolclasses`
  - `  default : 3`
  - `  pickaxe : 3`
  - `  shovel : 3`
  - The tool-class lines come from a `HashMap`, so their order is not defined. Vanilla also appends its own attribute block ("+6 Attack Damage" for full, "+2" for empty) because the tool guns expose an attack-damage modifier; that line is generated by vanilla 1.7.10, not by Techguns code.
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`PowerHammerProjectile` (entity id 15)](../04-projectiles-fx/projectiles.md#311-powerhammerprojectile). Damage source: `powerhammer / PHYSICAL`.
- Item renderer `RenderPowerHammer` with model `ModelPowerHammer` ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#power-hammer-powerhammer-and-advanced-power-hammer-powerhammeradv)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/powerHammerAdv.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×64, average #2A2421, dominant #101010 51%, #503030 16%, #303030 6%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun none, muzzle none.
- Recoil: 12 ticks (600 ms) for the charged shot; the left-click swing starts a separate 250 ms recoil.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.powerhammerFire` | charge release (server) | 1.0 / 1.0 (`world.playSoundAtEntity`) | 3 files, 2.20 to 3.19 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.powerhammerReload` | reload started from the right-click path | 1.0 / 1.0 (`world.playSoundAtEntity`, not the reload helper, `GUN-10`) | 1 file, 3.43 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.powerhammerCharge` | charge start | volume 1.0, pitch 1.0 | 3 files, 2.53 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.powerhammerSwing` | left-click swing with air | as Power Hammer | 3 files, 1.39 to 1.67 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.powerhammerImpactGround` | block broken with air | volume 1.0, pitch 1.0 | 4 files, 1.55 to 1.80 s, 32000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Compressed Air Tank (meta 28), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#power-hammer-powerhammer-and-advanced-power-hammer-powerhammeradv); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-08`, `GUN-09`, `GUN-10`, `GUN-27`, `GUN-28`, `GUN-31`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Grenade Launcher (`grenadelauncher`)

**1. Identity.**

- Catalog row 29 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class [`GrenadeLauncher`](index.md#coverage) (`java/techguns/items/guns/`).
- Registry name `Techguns:grenadelauncher` (`GameRegistry.registerItem(..., "grenadelauncher")`); unlocalized name `techguns.item.grenadelauncher`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 6). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Grenade Launcher", lang key `techguns.item.grenadelauncher.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 27 [empty only]: grid `m r s` / `· p ·` with m = Hardened Steel Barrel, r = Steel Receiver, s = Plastic Gun Stock, p = "plateSteel".
- The recipe outputs item damage = clip, which `onCreated` turns into **0 rounds**: the crafted gun is empty and must be loaded.
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage | 30.0 raw, never scaled for NPCs; minimum and drop-off unused (`GUN-04`) |
| Time to live / speed | 100 ticks hard-coded (config 35 ignored) / 1.0 blocks per tick; ballistic, 2 bounces |
| Radius / gravity | 1.5 blocks / 0.01 (config indices 7 and 8) |
| Accuracy | config 0.015 ignored; hard-coded spread 0.05 (about +-2.0 degrees); armor and zoom bonuses have no effect |
| Penetration | 0 (hard-coded; config 0.0 ignored) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 5 ticks (max 4 per second) |
| Reload | 100 ticks (5 s); the gun refills at once and the player-wide `fireDelay` is set to 95 ticks (reload minus `minFiretime`) |
| Clip | 6 rounds |
| Ammo per reload | up to 6 x 40mm Grenade (shared item meta 93), one round each (all-or-nothing per item, loads as many as the inventory has) |
| Empty returned | none |
| Forced-reload refund | none needed: a shell-by-shell gun keeps its loaded rounds on a forced reload and tops up with up to 6 minus the rounds left in items |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 40 ticks (variance 13), burst 3 shots, burst delay 20 ticks |
| Config key | `WeaponStats_techguns.item.grenadelauncher` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [30.0, 30.0, 35, 35, 35, 0.015, 0.0, 1.5, 0.01]; indices ignored by this gun: 1 DamageMin, 2 DamageDropStart, 3 DamageDropEnd, 4 TimeToLive, 5 Accuracy, 6 Penetration |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (5), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Semi-auto, `ammoCount` 6: a reload loads up to 6 40mm Grenades. Radius 1.5, gravity 0.01. The class declares its own `speed = 1.0` field that shadows the base field.
- `spawnProjectile` ignores most stats: damage is the gun's raw `damage` (not scaled for NPCs), speed 1.0, TTL 100, spread 0.05, penetration 0 and 2 bounces are hard-coded, so the configured TTL, accuracy and penetration (and the armor and zoom accuracy bonuses) have no effect (`GUN-04`). It then sends the `GrenadeLauncherTrail` packet.
- A dispenser loaded with 40mm Grenades fires the same projectile using this gun's damage, radius, speed and gravity ([`06-items`](../06-items/index.md)).
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 6) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 6, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 6). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 5000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: 6x 40mm Grenade`
  - `Damage: 30.0-30.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark red):
  - `Ammo: 6x 40mm Grenade`
  - `DamageType: Explosion`
  - `Damage: 30.0`
  - `Range: 35,35,35`
  - `Spread: 0.015`
  - `Clipsize: 6`
  - `ReloadTime: 5.0s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`GrenadeLauncherProjectile` (entity id 19)](../04-projectiles-fx/projectiles.md#1-registration). Damage source: tg_explosion / EXPLOSION for the area damage; a direct hit uses the vanilla explosion source.
- Item renderer [`RenderGrenadeLauncher`](../03-gun-visuals-audio/gun-models.md#obj-model-modelsgunsgrenadelauncherobj) with model [`ModelModel`](../03-gun-visuals-audio/gun-models.md#obj-model-modelsgunsgrenadelauncherobj) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#grenade-launcher-grenadelauncher)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/grenadelauncher.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×128, average #3F3E3D, dominant #303030 31%, #101010 26%, #505050 18%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun none, muzzle none.
- Trail FX `GrenadeLauncherTrail` is sent for every shot.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.grenadeLauncherFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.68 to 1.71 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.grenadeLauncherReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 4.32 s, 48000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: 40mm Grenade (meta 93), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Recipes and machines: A dispenser loaded with 40mm Grenades uses its damage, radius, speed and gravity.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#grenade-launcher-grenadelauncher); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-04`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Chainsaw (`chainsaw`)

**1. Identity.**

- Catalog row 32 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `Chainsaw` (`java/techguns/items/guns/`).
- Registry name `Techguns:chainsaw` (`GameRegistry.registerItem(..., "chainsaw")`); unlocalized name `techguns.item.chainsaw`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 300). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Chainsaw", lang key `techguns.item.chainsaw.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 28 [loaded]: grid `c c p` / `m m r` / `c c f` with c = Mechanical Parts (×4), p = Plastic Sheet, m = "plateIron" (×2), r = Iron Receiver, f = Fuel Tank; recipe 28e [empty]: grid same with f = Fuel Tank (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.
- NPC equipment (not obtainable directly): [`PsychoSteve`](../10-entities-npcs/per-mob.md#12-psycho-steve-psychosteve-early-game-mini-boss) (always). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).
- Guaranteed / special NPC drop: Psycho Steve always drops one Chainsaw (count 1, no NBT, so full) plus 4 to 8 Fuel Tanks, from his `dropFewItems`, on any death.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 10.0 per projectile |
| Minimum damage | 10.0 (no drop-off: start and end are both 2, equal to the time-to-live) |
| Time to live / speed | 2 ticks / 1.5 blocks per tick; nominal reach 3 blocks |
| Accuracy | 0.0 |
| Armor penetration | 25% (0.25) |
| Fire mode | right-click cut, repeats at the vanilla right-click rate; `minFiretime` 3 ticks (max 6.67 per second); left click is vanilla melee and mining |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 42 ticks (reload minus `minFiretime`) |
| Clip | 300 rounds |
| Ammo per reload | 1 x Fuel Tank (shared item meta 8) |
| Empty returned | 1 x Fuel Tank (Empty) (shared item meta 9) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | none |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 3 blocks, attack time 10 ticks (variance 3), burst 0 shots, burst delay 0 ticks |
| Tool stats | classes: axe 3 (the internal `default` entry takes the first level); dig speed 14.0; melee damage 12.0 with ammo / 2.0 without; 1 round per block mined or per melee hit |
| Config key | `WeaponStats_techguns.item.chainsaw` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [10.0, 10.0, 2, 2, 2, 0.0, 0.25]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Tool gun (`setShootWithLeftClick(false)`), recoil 5, penetration 0.25, TTL 2, damage 10, 300 fuel per Fuel Tank.
- Right-click calls `shootGunLeftClick` directly on both sides (no zoom) and repeats at the vanilla right-click rate, limited by `minFiretime` 3. Each "shot" uses 1 fuel and spawns an [`InvisibleProjectile`](../04-projectiles-fx/projectiles.md#43-invisibleprojectile) (speed 1.5, TTL 2, about 3 blocks): damage 10 x NPC bonus, PHYSICAL [`chainsaw`](gun-catalog.md#table-a-identity-and-ammunition) source, gore 1.0, no knockback. An empty saw reloads through the normal path (Fuel Tank in, empty tank out).
- Melee (left click): attribute damage 12 with fuel, 2 without; with fuel the source has gore 1.0, penetration 0.25 and no knockback; each hit uses 1 fuel. Mining: axe level 3, dig speed 14; each block broken with fuel uses 1 fuel.
- Swing sound is sent like the Power Hammer's but with distance 2.0. Harvest check reads metadata 0 only (`GUN-27`) and the tooltip lists the internal `default` class (`GUN-28`).
- Client / server: right-click handlers run on both sides; the server spawns the projectile.
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 300) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 300, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 300). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag) (not used: this tool fires through right-click handlers); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns), `loopSoundDelay` (10) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Fuel Tank`
  - `Damage: 10.0-10.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark gray):
  - `Ammo: Fuel Tank`
  - `DamageType: Physical`
  - `Damage: 10.0`
  - `Range: 2,2,2`
  - `Spread: 0.0`
  - `Clipsize: 300`
  - `ReloadTime: 2.25s`
  - `ArmorPen: 25.0%`
  - `Toolclasses`
  - `  default : 3`
  - `  axe : 3`
  - The tool-class lines come from a `HashMap`, so their order is not defined. Vanilla also appends its own attribute block ("+6 Attack Damage" for full, "+2" for empty) because the tool guns expose an attack-damage modifier; that line is generated by vanilla 1.7.10, not by Techguns code.
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`InvisibleProjectile` (entity id 21)](../04-projectiles-fx/projectiles.md#312-invisibleprojectile-chainsaw). Damage source: `chainsaw / PHYSICAL`.
- Item renderer `RenderChainsaw` with model [`ModelChainsaw`](../03-gun-visuals-audio/gun-models.md#modelchainsaw-12864-family-x-47-boxes-tabula-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#chainsaw-chainsaw)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/chainsaw.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×64, average #5C492C, dominant #101010 52%, #B0B0B0 15%, #F0B010 14%, 97% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun (0, 0.5, 0), muzzle none.
- Recoil 5 ticks (250 ms) is used for the right-click cut; the left-click swing starts a 250 ms recoil.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.chainsawLoopStart` | first shot after `loopSoundDelay` ran out (at least 10 idle ticks) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 1 file, 0.86 s, 32000 Hz, mono, sounds.json category `master` |
| `guns.chainsawLoop` | every later shot while firing continues | 4.0 / 1.0, same call | 3 files, 1.89 s, 32000 Hz, mono, sounds.json category `player` |
| `guns.powerhammerReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.43 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.chainsawSwing` | left-click swing with fuel | packet `PacketPlaySound`, distance/volume 2.0, pitch 1.0, category GUN_FIRE, to players within 50 blocks, at most once per 5 ticks, not with a container GUI open | 3 files, 1.10 to 1.62 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.chainsawHit` | block broken with fuel left | `world.playSoundEffect` at the block center, volume 0.65, pitch 1.0 | 6 files, 1.34 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Fuel Tank (meta 8), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`PsychoSteve`](../10-entities-npcs/per-mob.md#12-psycho-steve-psychosteve-early-game-mini-boss) (always). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0.5, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#chainsaw-chainsaw); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-27`, `GUN-28`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Stielgranate (`stielGranate`)

**1. Identity.**

- Class [`GenericGrenade`](per-gun-behavior.md#grenades-genericgrenade-fraggrenade-flashbang) (`java/techguns/items/guns/`). Not a [`GenericGun`](gun-lifecycle.md): it extends vanilla `Item` directly and is modeled on the bow.
- Registry name `Techguns:stielGranate`; unlocalized name [`stielGranate`](gun-catalog.md#table-f-grenades-not-genericgun) (the `techguns.` prefix is added only in `getUnlocalizedName(ItemStack)`, so stack names resolve to `techguns.item.stielGranate`). Metadata: none. Max stack 16, no repair. Creative tab `Techguns`. Ore dictionary names: none.
- English name "Stielgranate", lang key `techguns.item.stielGranate.name`.

**2. Obtaining.**

- Crafting yields 16: grid ` it` / ` wi` / `i  ` with i = iron ingot (`ingotIron`, x3), t = TNT, w = planks (`plankWood`) ([recipes.md](recipes.md#grenade-recipes)).
- Loot: `techgunsWeaponchestT2`: count 3 to 8, weight 2; `techgunsWeaponchestT3`: count 4 to 12, weight 2 ([11-worldgen/loot.md](../11-worldgen/loot.md)).
- NPC equipment or drops: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage | 20.0 (explosion; tooltip type always Explosion) |
| Radius | 5.0 blocks |
| Bounces | 3 |
| Time to live | 200 ticks before it detonates |
| Throw charge | j = ticks held; f = j/20; f = (f squared + 2f)/3, capped at 1 (the vanilla bow curve; full at 20 ticks) |
| Launch speed | 0.5 + 0.5 x f blocks per tick |
| Gravity | 0.015 / f (a weak throw falls faster; f = 0 divides by zero, `GUN-22`) |
| Spread / penetration / block damage | 0.05 / 0 / off |
| Use duration | 72000 ticks (`maxUseDur` 144000 is stored but ignored, `GUN-23`) |
| Stack size | 16 |
| Config keys | none for grenades (they are not in `Techguns_WeaponStats.cfg`); `DamageFactorNPC`, `DamagePvP` apply to hits |

**4. Behavior.**

- Right-click starts the wind-up (`setItemInUse`); nothing is played.
- Release (`onPlayerStoppedUsing`, both sides): compute f; if not creative consume one grenade item (the first matching stack in the inventory, `GUN-30`); on the server call `spawnGrenade`; on both sides swing the arm.
- `spawnGrenade` creates a [`GrenadeProjectile`](../04-projectiles-fx/projectiles.md#37-grenadeprojectile-family) with damage 20.0, radius 5.0, launch speed 0.5 + 0.5 f, TTL 200, spread 0.05, gravity 0.015/f, no block damage, penetration 0, 3 bounces, and plays `random.bow` at volume 1.0 and pitch 1/(random x 0.4 + 1.2) + 0.5 f.
- Dispenser: a dispenser holding it throws a grenade projectile (speed 1.0, TTL 200, spread 0.05, gravity 0.015, 3 bounces, no block damage) using the item's damage and radius.

**5. Persistence and sync.**

- Stack NBT: none; no packets of its own; no per-player state. The thrown entity is tracked normally (see [`04-projectiles-fx`](../04-projectiles-fx/index.md)).

**6. UI.**

- Tooltip without SHIFT: `Damage: 20.0` and `Press [SHIFT] for detailed tooltip`.
- Tooltip with SHIFT: `DamageType: Explosion` (dark red), `Damage: 20.0`, `Radius: 5.0`, `Bounces: 3`.
- Chat messages: none.

**7. Visuals.**

- Projectile: [`GrenadeProjectile` (entity id 11)](../04-projectiles-fx/projectiles.md#37-grenadeprojectile-family). Damage source: area damage `tg_explosion` / EXPLOSION; a direct hit uses the vanilla explosion source.
- Renderer `RenderStielgranate`, model [`ModelStielgranate`](../03-gun-visuals-audio/gun-models.md#modelstielgranate-3232-3-boxes), texture [`textures/guns/stielgranate.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (32×32, average #4F603E, dominant #909050 40%, #305030 29%, #105030 17%). [03 sheet](../03-gun-visuals-audio/gun-sheets.md#grenades).
- Camo: none. FX: see the projectile entry.

**8. Audio.** (Durations, rates and channels are measured; no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `random.bow` (vanilla) | release, server | 1.0 / 1/(rand x 0.4 + 1.2) + 0.5 x f | vanilla sound |

**9. Relationships.**

- Not used by any NPC class. Dispenser: see section 4. Ammo: it is its own ammo (the throw consumes the item).
- Visual and audio detail: [03-gun-visuals-audio](../03-gun-visuals-audio/gun-sheets.md#grenades); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific: `GUN-22`, `GUN-23`, `GUN-30`.

---

### Frag Grenade (`fragGrenade`)

**1. Identity.**

- Class [`FragGrenade`](per-gun-behavior.md#grenades-genericgrenade-fraggrenade-flashbang) (`java/techguns/items/guns/`) extending `GenericGrenade`. Not a `GenericGun`: it extends vanilla `Item` directly and is modeled on the bow.
- Registry name `Techguns:fragGrenade`; unlocalized name [`fragGrenade`](gun-catalog.md#table-f-grenades-not-genericgun) (the `techguns.` prefix is added only in `getUnlocalizedName(ItemStack)`, so stack names resolve to `techguns.item.fragGrenade`). Metadata: none. Max stack 16, no repair. Creative tab `Techguns`. Ore dictionary names: none.
- English name "Frag Grenade", lang key `techguns.item.fragGrenade.name`.

**2. Obtaining.**

- Crafting yields 16: grid ` if` / `iti` / ` i ` with i = steel ingot ([`ingotSteel`](../06-items/item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems), x4), f = Flint and Steel, t = TNT ([recipes.md](recipes.md#grenade-recipes)).
- Loot: `techgunsWeaponchestT3`: count 3 to 8, weight 2 ([11-worldgen/loot.md](../11-worldgen/loot.md)).
- NPC equipment or drops: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage | 25.0 (explosion; tooltip type always Explosion) |
| Radius | 6.0 blocks |
| Bounces | 2 |
| Time to live | 120 ticks before it detonates |
| Throw charge | j = ticks held; f = j/20; f = (f squared + 2f)/3, capped at 1 (the vanilla bow curve; full at 20 ticks) |
| Launch speed | 0.5 + 0.5 x f blocks per tick |
| Gravity | 0.015 / f (a weak throw falls faster; f = 0 divides by zero, `GUN-22`) |
| Spread / penetration / block damage | 0.05 / 0 / off |
| Use duration | 72000 ticks (`maxUseDur` 144000 is stored but ignored, `GUN-23`) |
| Stack size | 16 |
| Config keys | none for grenades (they are not in `Techguns_WeaponStats.cfg`); `DamageFactorNPC`, `DamagePvP` apply to hits |

**4. Behavior.**

- Right-click starts the wind-up and plays `guns.grenade_pin` (volume 1.0, pitch 1.0, `world.playSoundAtEntity`, both sides).
- Release (`onPlayerStoppedUsing`, both sides): compute f; if not creative consume one grenade item (the first matching stack in the inventory, `GUN-30`); on the server call `spawnGrenade`; on both sides swing the arm.
- `spawnGrenade` creates a [`FragGrenadeProjectile`](../04-projectiles-fx/projectiles.md#1-registration) with damage 25.0, radius 6.0, launch speed 0.5 + 0.5 f, TTL 120, spread 0.05, gravity 0.015/f, no block damage, penetration 0, 2 bounces, and plays `guns.grenade_throw` at volume 1.0 and pitch 1.0.
- Dispenser: a dispenser holding it throws a grenade projectile (speed 1.0, TTL 120, spread 0.05, gravity 0.015, 2 bounces, no block damage) using the item's damage and radius.

**5. Persistence and sync.**

- Stack NBT: none; no packets of its own; no per-player state. The thrown entity is tracked normally (see [`04-projectiles-fx`](../04-projectiles-fx/index.md)).

**6. UI.**

- Tooltip without SHIFT: `Damage: 25.0` and `Press [SHIFT] for detailed tooltip`.
- Tooltip with SHIFT: `DamageType: Explosion` (dark red), `Damage: 25.0`, `Radius: 6.0`, `Bounces: 2`.
- Chat messages: none.

**7. Visuals.**

- Projectile: [`FragGrenadeProjectile` (entity id 24)](../04-projectiles-fx/projectiles.md#37-grenadeprojectile-family). Damage source: area damage `tg_explosion` / EXPLOSION; a direct hit uses the vanilla explosion source.
- Renderer `RenderFragGrenade`, model [`ModelFragGrenade`](../03-gun-visuals-audio/gun-models.md#modelfraggrenadeshowring-declared-3232-used-with-a-6464-png-11-boxes), texture [`textures/guns/fragGrenade_texture.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (64×64, average #434F43, dominant #303030 29%, #305030 29%, #707070 24%). [03 sheet](../03-gun-visuals-audio/gun-sheets.md#grenades).
- Camo: none. FX: see the projectile entry.

**8. Audio.** (Durations, rates and channels are measured; no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.grenade_pin` | right-click (wind-up start) | 1.0 / 1.0 | 2 files, 0.77 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.grenade_throw` | release, server | 1.0 / 1.0 | 3 files, 0.77 to 0.88 s, 48000 Hz, mono, sounds.json category `player` |

**9. Relationships.**

- Not used by any NPC class. Dispenser: see section 4. Ammo: it is its own ammo (the throw consumes the item).
- Visual and audio detail: [03-gun-visuals-audio](../03-gun-visuals-audio/gun-sheets.md#grenades); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific: `GUN-22`, `GUN-23`, `GUN-30`.

---

### Flashbang (`flashbang`)

**1. Identity.**

- Class `Flashbang` (`java/techguns/items/guns/`) extending `GenericGrenade`. Not a `GenericGun`: it extends vanilla `Item` directly and is modeled on the bow.
- **Never registered**: the item is constructed in [`TGuns.init`](gun-catalog.md) (stack size 16) but there is no `GameRegistry.registerItem` call, so it has no registry name, no lang key and no recipe, and cannot exist in game (`GUN-24`). Unlocalized name would be `techguns.flashbang` (the prefix is added only by `getUnlocalizedName(ItemStack)`).
- English name: none (no lang key). Creative tab would be `Techguns`.

**2. Obtaining.**

- None: unobtainable (no recipe, never registered).
- Loot: none.
- NPC equipment or drops: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage | 20.0 (explosion; tooltip type always Explosion) |
| Radius | 5.0 blocks |
| Bounces | 0 |
| Time to live | 200 ticks before it detonates |
| Throw charge | j = ticks held; f = j/20; f = (f squared + 2f)/3, capped at 1 (the vanilla bow curve; full at 20 ticks) |
| Launch speed | 0.5 + 0.5 x f blocks per tick |
| Gravity | 0.015 / f (a weak throw falls faster; f = 0 divides by zero, `GUN-22`) |
| Spread / penetration / block damage | 0.05 / 0 / off |
| Use duration | 72000 ticks (`maxUseDur` 144000 is stored but ignored, `GUN-23`) |
| Stack size | 16 |
| Config keys | none for grenades (they are not in `Techguns_WeaponStats.cfg`); `DamageFactorNPC`, `DamagePvP` apply to hits |

**4. Behavior.**

- Right-click starts the wind-up (`setItemInUse`); nothing is played.
- Release (`onPlayerStoppedUsing`, both sides): compute f; if not creative consume one grenade item (the first matching stack in the inventory, `GUN-30`); on the server call `spawnGrenade`; on both sides swing the arm.
- `spawnGrenade` creates a [`FlashBangProjectile`](../04-projectiles-fx/projectiles.md#1-registration) with damage 20.0, radius 5.0, launch speed 0.5 + 0.5 f, TTL 200, spread 0.05, gravity 0.015/f, no block damage, penetration 0, 0 bounces, and plays `random.bow` at volume 1.0 and pitch 1/(random x 0.4 + 1.2) + 0.5 f.
- Dead feature: even if registered, the Flashbang would use 0 bounces and a `FlashBangProjectile` whose effect is documented in [`04-projectiles-fx`](../04-projectiles-fx/index.md).
- Dispenser: none (never registered).

**5. Persistence and sync.**

- Stack NBT: none; no packets of its own; no per-player state. The thrown entity is tracked normally (see [`04-projectiles-fx`](../04-projectiles-fx/index.md)).

**6. UI.**

- Tooltip without SHIFT: `Damage: 20.0` and `Press [SHIFT] for detailed tooltip`.
- Tooltip with SHIFT: `DamageType: Explosion` (dark red), `Damage: 20.0`, `Radius: 5.0`, `Bounces: 0`.
- Chat messages: none.

**7. Visuals.**

- Projectile: [`FlashBangProjectile` (never reachable)](../04-projectiles-fx/projectiles.md#1-registration). Damage source: area damage `tg_explosion` / EXPLOSION; a direct hit uses the vanilla explosion source.
- No item renderer and no texture: never registered.
- Camo: none. FX: see the projectile entry.

**8. Audio.** (Durations, rates and channels are measured; no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `random.bow` (vanilla) | release, server | 1.0 / 1/(rand x 0.4 + 1.2) + 0.5 x f | vanilla sound |

**9. Relationships.**

- Not used by any NPC class. Dispenser: see section 4. Ammo: it is its own ammo (the throw consumes the item).
- Visual and audio detail: [03-gun-visuals-audio](../03-gun-visuals-audio/gun-sheets.md#grenades); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific: `GUN-22`, `GUN-23`, `GUN-30`, `GUN-24`.

