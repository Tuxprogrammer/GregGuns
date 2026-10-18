# 02-guns: Entity sheets: energy, fire, bio and special guns

Each item below has a full entity sheet with fields 1 to 10 of the [conventions](../CONVENTIONS.md#entity-sheet): identity, obtaining, numbers, behavior, persistence and sync, UI, visuals, audio, relationships and bugs. The catalog tables ([gun-catalog.md](gun-catalog.md)) remain the quick reference; shared mechanics are in [gun-lifecycle.md](gun-lifecycle.md), the shared rules for these sheets are in [sheets-index.md](sheets-index.md), and class-level detail is in [per-gun-behavior.md](per-gun-behavior.md).

Items in this file: [Flamethrower](#flamethrower-flamethrower), [Laser Rifle](#laser-rifle-lasergun), [Bio Gun](#bio-gun-biogun), [Tesla Rifle](#tesla-rifle-teslagun), [Alien Blaster](#alien-blaster-alienblaster), [Nether Blaster](#nether-blaster-cyberdemonblaster), [Blaster Rifle](#blaster-rifle-blasterrifle), [Sonic Rifle](#sonic-rifle-sonicshotgun), [Scatter Laser Rifle [WIP]](#scatter-laser-rifle-wip-scatterbeamgun), [Nuclear Death Ray](#nuclear-death-ray-nucleardeathray), [De-Atomizer Pistol](#de-atomizer-pistol-mibgun), [Akimbo De-Atomizer Pistols](#akimbo-de-atomizer-pistols-mibgunakimbo).

### Flamethrower (`flamethrower`)

**1. Identity.**

- Catalog row 6 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `Flamethrower` (`java/techguns/items/guns/`).
- Registry name `Techguns:flamethrower` (`GameRegistry.registerItem(..., "flamethrower")`); unlocalized name `techguns.item.flamethrower`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 100). Creative tab [`Techguns`](../01-core/lifecycle-and-registration.md#lifecycle-overview) (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Flamethrower", lang key `techguns.item.flamethrower.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 9 [loaded only]: grid `p r s` / `f m ·` with p = Pump Mechanism, r = Iron Receiver, s = Plastic Gun Stock, f = Flint and Steel, m = Fuel Tank.
- The recipe outputs item damage 0, i.e. a full clip (`onCreated` sets `ammo` to the clip size).
- Loot ([`TGItems`](../06-items/item-catalog.md) chest hooks; Techguns weapon chests are documented in [11-worldgen/loot.md](../11-worldgen/loot.md)): `techgunsWeaponchestT3`: count 1, weight 1. A looted stack has no NBT, so `getCurrentAmmo` initializes it as a full gun on first use.
- NPC equipment (not obtainable directly): [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (2 of 15 at full difficulty only). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 5.0 per projectile |
| Minimum damage | 2.0 |
| Drop-off | full damage up to 4 blocks, linear to the minimum at 16 blocks, minimum beyond |
| Time to live / speed | 16 ticks / 1.0 blocks per tick (fixed in `Flamethrower`); nominal reach 16 blocks |
| Accuracy (spread) | 0.05, an independent uniform random offset of +-2 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 2 ticks (10 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 43 ticks (reload minus `minFiretime`) |
| Clip | 100 rounds |
| Ammo per reload | 1 x Fuel Tank (shared item meta 8) |
| Empty returned | 1 x Fuel Tank (Empty) (shared item meta 9) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 12 blocks, attack time 20 ticks (variance 6), burst 5 shots, burst delay 5 ticks |
| Config key | `WeaponStats_techguns.item.flamethrower` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [5.0, 2.0, 4, 16, 16, 0.05, 0.0]; indices all honored |
| Other config keys | [`DamageFactorNPC`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (1.0), [`DamageTurretToPlayer`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (0.5), [`DamagePvP`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (0.5), [`ArmorPenetrationMultiplierNPCtoPlayer`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) and [`ArmorPenetrationMultiplierPvP`](../01-core/config.md#category-damage-factors-used-by-damagesystemdamagesystem-described-in-0205) (1.0) scale hits; [`ForceSafeModeForNonOps`](../01-core/config.md#category-general) (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends [`PacketShootGun`](../01-core/networking.md#8--packetshootgun) and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (2), broadcasts [`GunFiredMessage`](../01-core/networking.md#1--gunfiredmessage) within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- `Flamethrower` overrides `spawnProjectile` and the tooltip type. The [`FlamethrowerProjectile`](../04-projectiles-fx/projectiles.md#32-flamethrowerprojectile) has fixed speed 1.0, gravity 0.01, piercing off. The ignite flag is true only for a player with safe mode off; NPC flamethrowers never ignite. The same flag is passed on as the block-damage flag (`GUN-05`).
- Looping sound: `guns.flamethrowerStart` plays on the first shot after at least 10 idle ticks (`loopSoundDelay`), later shots play `guns.flamethrowerFire`.
- A Fuel Tank holds 100 rounds and comes back as a Fuel Tank (Empty). The fuel tank is also the ammo of the Chainsaw.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 100) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 100, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 100). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); [`ReloadStartedMessage`](../01-core/networking.md#3--reloadstartedmessage) (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in [`TechgunsExtendedPlayerProperties`](../01-core/player-state.md#techgunsextendedplayerproperties): `fireDelay` (shared by all guns), `loopSoundDelay` (10) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Fuel Tank`
  - `Damage: 5.0-2.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: red):
  - `Ammo: Fuel Tank`
  - `DamageType: Fire`
  - `Damage: 5.0-2.0`
  - `Range: 4,16,16`
  - `Spread: 0.05`
  - `Clipsize: 100`
  - `ReloadTime: 2.25s`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`FlamethrowerProjectile` (entity id 3)](../04-projectiles-fx/projectiles.md#32-flamethrowerprojectile). Damage source: `tg_fire / FIRE`.
- Item renderer `RenderFlamethrower` with model [`ModelFlamethrower`](../03-gun-visuals-audio/gun-models.md#modelflamethrower-6464-family-x-36-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#flamethrower-flamethrower)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/Flamethrower.png`](../03-gun-visuals-audio/textures.md#texturesgunsflamethrowerpng-flamethrower) (camo 0, default): 64×64, average #463C3B, dominant #101010 58%, #F0F0F0 8%, #505050 5%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.flamethrowerStart` | first shot after `loopSoundDelay` ran out (at least 10 idle ticks) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 1 file, 1.00 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.flamethrowerFire` | every later shot while firing continues | 4.0 / 1.0, same call | 5 files, 1.10 to 1.30 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.flamethrowerReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.21 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Fuel Tank (meta 8), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7); [`ZombiePigmanSoldier`](../10-entities-npcs/per-mob.md#4-zombie-pigman-soldier-zombiepigmansoldier) (2 of 15 at full difficulty only). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from [`GenericNPC`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava), second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot ([`SlotTechgun`](../09-machines-utility/turret.md)); [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#flamethrower-flamethrower); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-05`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Laser Rifle (`lasergun`)

**1. Identity.**

- Catalog row 8 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `Lasergun` (`java/techguns/items/guns/`).
- Registry name `Techguns:lasergun` (`GameRegistry.registerItem(..., "lasergun")`); unlocalized name `techguns.item.lasergun`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 45). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Laser Rifle", lang key `techguns.item.lasergun.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 11 [loaded only]: grid `· g d` / `b r s` / `· e m` with g = "paneGlass", d = Redstone (item), b = Laser Barrel, r = Obsidian Steel Receiver, s = Plastic Gun Stock, e = Energy Cell, m = Mechanical Parts (Carbon).
- The recipe outputs item damage 0, i.e. a full clip (`onCreated` sets `ammo` to the clip size).
- Loot: none.
- NPC equipment (not obtainable directly): [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7); [`StormTrooper`](../10-entities-npcs/per-mob.md#10-stormtrooper-stormtrooper-registry-name-stormtrooper) (1 of 2); [`SuperMutantBasic`](../10-entities-npcs/per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (2 of 5; inherited by Heavy and Elite). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 12.0 per projectile |
| Minimum damage | 12.0 (no drop-off: start and end are both 5, equal to the time-to-live) |
| Time to live / speed | 5 ticks / 7.5 blocks per tick; nominal reach 37.5 blocks |
| Accuracy (spread) | 0.0: no random offset |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 5 ticks (4 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 40 ticks (reload minus `minFiretime`) |
| Clip | 45 rounds |
| Ammo per reload | 1 x Energy Cell (shared item meta 15) |
| Empty returned | 1 x Energy Cell (Empty) (shared item meta 16) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | toggle on right-click; FOV x0.75, spread x0.75 while zoomed |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 30 ticks (variance 10), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.lasergun` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [12.0, 12.0, 5, 5, 5, 0.0, 0.0]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (5), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- `Lasergun` constants: `BEAM_LIFETIME` 5, `LIFETIME` 5, `SPEED` 5.0; the builder applies the speed multiplier 5.0 to the base 1.5 (7.5 blocks/tick) and passes `LIFETIME` as time-to-live. Accuracy is 0, damage has no drop-off.
- Scoped (FOV x0.75, zoomed spread x0.75). Energy Cell ammo (45 rounds), returned as an Energy Cell (Empty).
- The [`LaserProjectile`](../04-projectiles-fx/projectiles.md#33-laserprojectile) spawns its own [`LaserBeam2`](../04-projectiles-fx/projectiles.md#1-registration) visual entity (see [`04-projectiles-fx`](../04-projectiles-fx/index.md)).
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 45) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 45, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 45). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Energy Cell`
  - `Damage: 12.0-12.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark aqua):
  - `Ammo: Energy Cell`
  - `DamageType: Energy`
  - `Damage: 12.0`
  - `Range: 5,5,5`
  - `Spread: 0.0`
  - `Clipsize: 45`
  - `ReloadTime: 2.25s`
  - `Zoom:(toggle) Mult:0.75`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`LaserProjectile` (entity id 4), which also spawns a `LaserBeam2` visual](../04-projectiles-fx/projectiles.md#33-laserprojectile). Damage source: `tg_energy / ENERGY`.
- Item renderer `RenderLasergun` with model [`ModelLasergun`](../03-gun-visuals-audio/gun-models.md#modellasergun-6464-41-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#laser-gun-lasergun)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/Lasergun.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×64, average #423D3A, dominant #101010 38%, #303030 16%, #909090 14%, 94% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.lasergunFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.56 to 1.79 s, 44100 Hz, stereo, sounds.json category `player` |
| `guns.lasergunReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.05 s, 32000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Energy Cell (meta 15), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7); [`StormTrooper`](../10-entities-npcs/per-mob.md#10-stormtrooper-stormtrooper-registry-name-stormtrooper) (1 of 2); [`SuperMutantBasic`](../10-entities-npcs/per-mob.md#14-super-mutants-supermutantbasic-supermutantheavy-supermutantelite) (2 of 5; inherited by Heavy and Elite). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#laser-gun-lasergun); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-12`, `GUN-07`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Bio Gun (`biogun`)

**1. Identity.**

- Catalog row 11 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `Biogun` (`java/techguns/items/guns/`).
- Registry name `Techguns:biogun` (`GameRegistry.registerItem(..., "biogun")`); unlocalized name `techguns.item.biogun`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 30). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Bio Gun", lang key `techguns.item.biogun.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 10 [loaded only]: grid `f r s` / `· m ·` with f = Pump Mechanism, r = Steel Receiver, s = Plastic Gun Stock, m = Bio Tank.
- The recipe outputs item damage 0, i.e. a full clip (`onCreated` sets `ammo` to the clip size).
- Loot: none.
- NPC equipment (not obtainable directly): [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage | 10.0 raw (never scaled for NPCs, `GUN-03`); the projectile level 1 to 3 comes from the charge |
| Minimum damage | 8.0 raw |
| Drop-off | full damage to 8 blocks, linear to the minimum at 15 blocks |
| Time to live / speed | 35 ticks / 1.0 blocks per tick (fixed in `Biogun`); nominal reach 35 blocks |
| Accuracy (spread) | 0.015, an independent uniform random offset of +-0.6 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | 15% (0.15) |
| Fire mode | left click: full-auto, one round every 6 ticks (3.33 per second); right click: hold to charge (`fullChargeTime` 30 ticks) and release |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 39 ticks (reload minus `minFiretime`) |
| Clip | 30 rounds |
| Ammo per reload | 1 x Bio Tank (shared item meta 10) |
| Empty returned | 1 x Bio Tank (Empty) (shared item meta 11) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | none |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 18 blocks, attack time 30 ticks (variance 10), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.biogun` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [10.0, 8.0, 8, 15, 35, 0.015, 0.15]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Charge-on-hold gun ([`GenericGunCharge`](per-gun-behavior.md#genericguncharge-genericgunchargejava) with `fullChargeTime` 30 ticks). Left click is the normal automatic path (6-tick interval): one round, a level-1 [`BiogunProjectile`](../04-projectiles-fx/projectiles.md#35-biogunprojectile), damage not scaled for NPCs (`GUN-03`).
- Right-click and hold, then release: charge = held ticks / 30. Charge up to 0.45 gives level 1, up to 0.95 level 2, above that level 3. The rounds used should equal the level, but when fewer rounds remain than the level the code subtracts the wrong way (`GUN-02`). `ammoConsumedOnFullCharge` (3) is set but never read.
- The release path ignores armor accuracy and zoom, sends no `GunFiredMessage` (`GUN-31`), plays the fire sound once at the shooter and has a degraded reload path (`GUN-10`). Use duration is `Integer.MAX_VALUE`.
- Projectile effect per level (size, splash, poison) is in [`04-projectiles-fx`](../04-projectiles-fx/index.md). The trail FX packet `BioGunTrail` is sent for every shot.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 30) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 30, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 30). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot) for left-click shots; the charged release sends none (`GUN-31`); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms for the normal reload path; the right-click charge-gun reload sends none (`GUN-10`)). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Bio Tank`
  - `Damage: 10.0-8.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark green):
  - `Ammo: Bio Tank`
  - `DamageType: Poison`
  - `Damage: 10.0-8.0`
  - `Range: 8,15,35`
  - `Spread: 0.015`
  - `Clipsize: 30`
  - `ReloadTime: 2.25s`
  - `ArmorPen: 15.000001%`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`BiogunProjectile` (entity id 8)](../04-projectiles-fx/projectiles.md#35-biogunprojectile). Damage source: `tg_poison / POISON`.
- Item renderer `RenderBiogun` with model [`ModelBiogun`](../03-gun-visuals-audio/gun-models.md#modelbiogun-6464-41-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#bio-gun-biogun)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/BioGun.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×64, average #2E452C, dominant #101010 46%, #505050 7%, #B0B090 5%, 97% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun none, muzzle none.
- Trail FX `BioGunTrail` is sent for every shot ([`04-projectiles-fx`](../04-projectiles-fx/index.md)).

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.biogunFire` | left-click shot (both sides) and NPC shot (server); the right-click charged release plays it again at the shooter | 4.0 / 1.0 for left-click and NPC shots (`playSoundOnEntityGunPosition`, category GUN_FIRE); 1.0 / 1.0 for the charged release (`world.playSoundAtEntity`, server) | 3 files, 1.58 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.biogunReload` | reload started from the right-click path | 1.0 / 1.0 (`world.playSoundAtEntity`, not the reload helper, `GUN-10`) | 1 file, 3.42 s, 48000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Bio Tank (meta 10), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#bio-gun-biogun); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-02`, `GUN-03`, `GUN-10`, `GUN-31`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Tesla Rifle (`teslagun`)

**1. Identity.**

- Catalog row 16 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class [`Teslagun`](index.md#coverage) (`java/techguns/items/guns/`).
- Registry name `Techguns:teslagun` (`GameRegistry.registerItem(..., "teslagun")`); unlocalized name `techguns.item.teslagun`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 25). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Tesla Rifle", lang key `techguns.item.teslagun.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 21 [loaded]: grid `· g d` / `c r s` / `· e ·` with g = "paneGlass", d = "dustRedstone", c = Coil, r = Carbon Receiver, s = Carbon Gunstock, e = Energy Cell; recipe 21e [empty]: grid same with e = Energy Cell (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.
- NPC equipment (not obtainable directly): [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 12.0 per projectile |
| Minimum damage | 12.0 (no drop-off: start and end are both 10, equal to the time-to-live) |
| Time to live / speed | 10 ticks / 5.0 blocks per tick (fixed in [`TeslaProjectile`](../04-projectiles-fx/projectiles.md#36-teslaprojectile-chain-lightning), as are TTL 10 and spread 0); nominal reach 50 blocks |
| Accuracy | config 0.0, ignored (projectile spread fixed at 0) |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 8 ticks (2.5 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 37 ticks (reload minus `minFiretime`) |
| Clip | 25 rounds |
| Ammo per reload | 1 x Energy Cell (shared item meta 15) |
| Empty returned | 1 x Energy Cell (Empty) (shared item meta 16) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | toggle on right-click; FOV x0.75, spread x1.0 while zoomed |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 10 ticks (500 ms) |
| AI stats | range 24 blocks, attack time 30 ticks (variance 10), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.teslagun` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [12.0, 12.0, 10, 10, 10, 0.0, 0.0]; indices ignored by this gun: 4 TimeToLive, 5 Accuracy |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (8), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Constants: `BEAM_LIFETIME` 10, `LIFETIME` 10, `SPEED` 5.0, `CHAIN_RANGE` 5.0. The fields `chain = 4` and `chainDmgDrop = 4` are never read (`GUN-21`). Each shot spawns two entities: a [`TeslaBeam`](../04-projectiles-fx/projectiles.md#1-registration) visual attached to the shooter and a `TeslaProjectile` with damage and minimum scaled by the NPC bonus, the gun's drop-off and penetration, a hard-coded 3 chain jumps, chain damage drop 0 and block damage from safe mode.
- The projectile forces its own speed (5.0), lifetime (10) and spread (0), so the configured TTL and accuracy have no effect. The scope (FOV x0.75, zoomed spread x1.0) only changes the view. Muzzle flash lasts 10 ticks.
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 25) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 25, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 25). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Energy Cell`
  - `Damage: 12.0-12.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: yellow):
  - `Ammo: Energy Cell`
  - `DamageType: Lightning`
  - `Damage: 12.0`
  - `Range: 10,10,10`
  - `Spread: 0.0`
  - `Clipsize: 25`
  - `ReloadTime: 2.25s`
  - `Zoom:(toggle) Mult:0.75`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`TeslaProjectile` (entity id 9) plus a `TeslaBeam` visual (entity id 10)](../04-projectiles-fx/projectiles.md#36-teslaprojectile-chain-lightning). Damage source: `tg_lightning / LIGHTNING`.
- Item renderer `RenderTeslaGun` with model [`ModelTeslaGun`](../03-gun-visuals-audio/gun-models.md#modelteslagun-6464-63-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#tesla-gun-teslagun)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/TeslaGun.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×64, average #383839, dominant #101010 48%, #909090 16%, #505050 14%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun none, muzzle none.
- A `TeslaBeam` visual entity is spawned with every shot.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.teslaFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 4 files, 3.44 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.teslaReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.35 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Energy Cell (meta 15), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`Outcast`](../10-entities-npcs/per-mob.md#8-outcast-outcast) (1 of 7). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#tesla-gun-teslagun); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-21`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-12`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Alien Blaster (`alienBlaster`)

**1. Identity.**

- Catalog row 22 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `AlienBlaster` (`java/techguns/items/guns/`).
- Registry name `Techguns:alienBlaster` (`GameRegistry.registerItem(..., "alienBlaster")`); unlocalized name `techguns.item.alienBlaster`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 10). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Alien Blaster", lang key `techguns.item.alienBlaster.name`.

**2. Obtaining.**

- Crafting: none. Unobtainable in survival: creative tab and commands only (no loot table or NPC carries it).
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 16.0 per projectile |
| Minimum damage | 16.0 (no drop-off: start and end are both 50, equal to the time-to-live) |
| Time to live / speed | 50 ticks / 1.5 blocks per tick; nominal reach 75 blocks |
| Accuracy (spread) | 0.0: no random offset |
| Armor penetration | 20% (0.2) |
| Fire mode | full-auto: one round every 8 ticks (2.5 per second) |
| Reload | 35 ticks (1.75 s); the gun refills at once and the player-wide `fireDelay` is set to 27 ticks (reload minus `minFiretime`) |
| Clip | 10 rounds |
| Ammo per reload | 1 x Energy Cell (shared item meta 15) |
| Empty returned | 1 x Energy Cell (Empty) (shared item meta 16) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 10 ticks (500 ms) |
| AI stats | range 24 blocks, attack time 40 ticks (variance 13), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.alienBlaster` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [16.0, 16.0, 50, 50, 50, 0.0, 0.2]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (8), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- `AlienBlaster` creates an [`AlienBlasterProjectile`](../04-projectiles-fx/projectiles.md#1-registration) (speed 1.5, damage and minimum scaled for NPCs, ignite only for a player with safe mode off; NPCs never ignite) and then sends the packet `PacketSpawnParticleOnEntity("AlienBlasterTrail")` within 200 blocks. The projectile has no entity registration, so clients never see it and the trail packet finds no entity (`PFX-01`).
- The weapon has no recipe, no loot entry and no NPC user: it exists only in the creative tab (or via commands).
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 10) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 10, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 10). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 1750 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Energy Cell`
  - `Damage: 16.0-16.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: red):
  - `Ammo: Energy Cell`
  - `DamageType: Fire`
  - `Damage: 16.0`
  - `Range: 50,50,50`
  - `Spread: 0.0`
  - `Clipsize: 10`
  - `ReloadTime: 1.75s`
  - `ArmorPen: 20.0%`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`AlienBlasterProjectile` (no entity registration)](../04-projectiles-fx/projectiles.md#39-alienblasterprojectile-not-registered-see-1). Damage source: `tg_fire / FIRE`.
- Item renderer `RenderAlienBlaster` with model [`ModelAlienBlaster`](../03-gun-visuals-audio/gun-models.md#modelalienblaster-12864-family-z-24-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#alien-blaster-alienblaster)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/alien_blaster.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×64, average #C6C9CE, dominant #D0D0D0 71%, #B0B0B0 7%, #D0D0F0 5%, 99% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun none, muzzle none.
- Trail FX `AlienBlasterTrail` is sent for every shot but finds no entity (`PFX-01`).

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.alienblasterFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 2.65 to 2.80 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.alienblasterReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.97 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Energy Cell (meta 15), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#alien-blaster-alienblaster); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Nether Blaster (`cyberdemonBlaster`)

**1. Identity.**

- Catalog row 23 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `CyberdemonBlaster` (`java/techguns/items/guns/`).
- Registry name `Techguns:cyberdemonBlaster` (`GameRegistry.registerItem(..., "cyberdemonBlaster")`); unlocalized name `techguns.item.cyberdemonBlaster`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 10). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Nether Blaster", lang key `techguns.item.cyberdemonBlaster.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 25 [loaded only]: grid `c p c` / `b n s` / `c p c` with c = Cybernetic Parts (×4), p = "plateObsidianSteel", b = Hardened Steel Barrel, n = Nether Charge, s = Pump Mechanism.
- The recipe outputs item damage 0, i.e. a full clip (`onCreated` sets `ammo` to the clip size).
- Loot: none.
- NPC equipment (not obtainable directly): [`CyberDemon`](../10-entities-npcs/per-mob.md#11-cyberdemon-cyberdemon) (always). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).
- Guaranteed / special NPC drop: The Cyberdemon overrides the rare-drop hook to drop a fresh Nether Blaster (count 1, no NBT, so full). The vanilla rare-drop roll is a player kill with 2.5% base chance plus 0.5% per Looting level (as documented in [`10-entities-npcs`](../10-entities-npcs/index.md)). The equivalent override in `SuperMutantBasic` is commented out.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 14.0 per projectile |
| Minimum damage | 8.0 |
| Drop-off | full damage up to 15 blocks, linear to the minimum at 30 blocks, minimum beyond |
| Time to live / speed | 50 ticks / 0.85 blocks per tick (fixed in `CyberdemonBlaster`); nominal reach 42.5 blocks |
| Accuracy (spread) | 0.0: no random offset |
| Armor penetration | 15% (0.15) |
| Fire mode | full-auto: one round every 8 ticks (2.5 per second) |
| Reload | 35 ticks (1.75 s); the gun refills at once and the player-wide `fireDelay` is set to 27 ticks (reload minus `minFiretime`) |
| Clip | 10 rounds |
| Ammo per reload | 1 x Nether Charge (shared item meta 63) |
| Empty returned | none |
| Forced-reload refund | none: the remaining rounds are discarded when a forced reload is used on a magazine-less gun (`GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 10 ticks (500 ms) |
| AI stats | range 24 blocks, attack time 40 ticks (variance 13), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.cyberdemonBlaster` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [14.0, 8.0, 15, 30, 50, 0.0, 0.15]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (8), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Spawns a [`CyberdemonBlasterProjectile`](../04-projectiles-fx/projectiles.md#38-cyberdemonblasterprojectile) with fixed speed 0.85, gravity 0, piercing off, the given spread, the gun's drop-off and penetration and NPC damage scaling. The ignite flag defaults to true and is replaced by the safe-mode state only for players, so NPC shooters always ignite and do block damage (`GUN-05`). Trail FX packet `CyberDemonBlasterTrail`.
- Ammo is the Nether Charge (1 charge gives 10 shots, no empty item). The reload sound is the Tesla Rifle's `guns.teslaReload`.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 10) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 10, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 10). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 1750 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Nether Charge`
  - `Damage: 14.0-8.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: red):
  - `Ammo: Nether Charge`
  - `DamageType: Fire`
  - `Damage: 14.0-8.0`
  - `Range: 15,30,50`
  - `Spread: 0.0`
  - `Clipsize: 10`
  - `ReloadTime: 1.75s`
  - `ArmorPen: 15.000001%`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`CyberdemonBlasterProjectile` (entity id 13)](../04-projectiles-fx/projectiles.md#38-cyberdemonblasterprojectile). Damage source: `tg_fire / FIRE (knockback 1)`.
- Item renderer `RenderCyberdemonBlaster` with model [`ModelCyberdemonBlaster`](../03-gun-visuals-audio/gun-models.md#modelcyberdemonblaster-6464-family-z-24-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#cyberdemon-blaster-cyberdemonblaster)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/cyberdemonblaster.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×64, average #4E3F39, dominant #101010 44%, #707070 22%, #909090 18%, 93% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun none, muzzle none.
- Trail FX `CyberDemonBlasterTrail` is sent for every shot.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.cyberdemonblasterFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 4 files, 2.00 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.teslaReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.35 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Nether Charge (meta 63), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`CyberDemon`](../10-entities-npcs/per-mob.md#11-cyberdemon-cyberdemon) (always). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#cyberdemon-blaster-cyberdemonblaster); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-05`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Blaster Rifle (`blasterRifle`)

**1. Identity.**

- Catalog row 25 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `BlasterRifle` (`java/techguns/items/guns/`).
- Registry name `Techguns:blasterRifle` (`GameRegistry.registerItem(..., "blasterRifle")`); unlocalized name `techguns.item.blasterRifle`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 50). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Blaster Rifle", lang key `techguns.item.blasterRifle.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 12 [loaded only]: grid `p c g` / `b m s` / `· e ·` with p = "plateCarbon", c = "circuitElite", g = blockGlassHardened\|blockGlass, b = Laser Barrel, m = Carbon Receiver, s = Carbon Gunstock, e = Energy Cell. A key `r` = Redstone is declared but not used in the grid..
- The recipe outputs item damage 0, i.e. a full clip (`onCreated` sets `ammo` to the clip size).
- Loot: none.
- NPC equipment (not obtainable directly): [`StormTrooper`](../10-entities-npcs/per-mob.md#10-stormtrooper-stormtrooper-registry-name-stormtrooper) (1 of 2). Any NPC holding it can also drop a fresh copy through the rare-drop roll, and the 8.5% equipment drop can hand over the held stack with random wear ([10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops)).

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 10.0 per projectile |
| Minimum damage | 8.0 |
| Drop-off | full damage up to 25 blocks, linear to the minimum at 35 blocks, minimum beyond |
| Time to live / speed | 50 ticks / 1.5 blocks per tick; nominal reach 75 blocks |
| Accuracy (spread) | 0.025, an independent uniform random offset of +-1 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus) and by 0.75 while zoomed |
| Armor penetration | 20% (0.2) |
| Fire mode | full-auto: one round every 5 ticks (4 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 40 ticks (reload minus `minFiretime`) |
| Clip | 50 rounds |
| Ammo per reload | 1 x Energy Cell (shared item meta 15) |
| Empty returned | 1 x Energy Cell (Empty) (shared item meta 16) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | toggle on right-click; FOV x0.5, spread x0.75 while zoomed |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 30 ticks (variance 10), burst 5 shots, burst delay 3 ticks |
| Config key | `WeaponStats_techguns.item.blasterRifle` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [10.0, 8.0, 25, 35, 50, 0.025, 0.2]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (5), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- `BlasterRifle` spawns a [`BlasterRifleProjectile`](../04-projectiles-fx/projectiles.md#1-registration) with the standard [`GenericGun`](gun-lifecycle.md) stat set. Silencing and forward offset are supported by the code path but unused. Scope FOV x0.5, zoomed spread x0.75. The reload sound is the Laser Rifle's `guns.lasergunReload`.
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 50) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 50, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 50). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Energy Cell`
  - `Damage: 10.0-8.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark aqua):
  - `Ammo: Energy Cell`
  - `DamageType: Energy`
  - `Damage: 10.0-8.0`
  - `Range: 25,35,50`
  - `Spread: 0.025`
  - `Clipsize: 50`
  - `ReloadTime: 2.25s`
  - `ArmorPen: 20.0%`
  - `Zoom:(toggle) Mult:0.5`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`BlasterRifleProjectile` (entity id 14)](../04-projectiles-fx/projectiles.md#1-registration). Damage source: `tg_energy / ENERGY`.
- Item renderer `RenderBlasterRifle` with model [`ModelBlasterRifle`](../03-gun-visuals-audio/gun-models.md#modelblasterrifle-12864-used-with-a-256128-png-family-z-23-boxes-tabula-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#blaster-rifle-blasterrifle)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/blasterRifle.png`](../03-gun-visuals-audio/textures.md#texturesgunsblasterriflepng-blaster-rifle) (camo 0, default): 256×128, average #1E1E1E, dominant #101010 50%, #303030 49%, #505050 0%, 31% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashLaser` at offset (0, -0.1, 0.1); turret offsets: gun none, muzzle (0, 0, -0.4).

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.blasterrifleFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 0.77 to 0.83 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.lasergunReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.05 s, 32000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Energy Cell (meta 15), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: [`StormTrooper`](../10-entities-npcs/per-mob.md#10-stormtrooper-stormtrooper-registry-name-stormtrooper) (1 of 2). NPC shots deal 25 to 37.5% of the listed damage on Easy / Normal / Hard (first factor 0.5 from `GenericNPC`, second factor 0.5 / 0.6 / 0.75), then `DamageFactorNPC`; NPCs never use ammo ([gun-lifecycle.md](gun-lifecycle.md#6-damage-bonus-and-npc-firing-fireweaponfromnpcshooter-dmgscale-accscale)).
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#blaster-rifle-blasterrifle); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-12`, `GUN-07`, `GUN-06`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Sonic Rifle (`sonicShotgun`)

**1. Identity.**

- Catalog row 31 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class [`SonicShotgun`](index.md#coverage) (`java/techguns/items/guns/`).
- Registry name `Techguns:sonicShotgun` (`GameRegistry.registerItem(..., "sonicShotgun")`); unlocalized name `techguns.item.sonicShotgun`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 8). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Sonic Rifle", lang key `techguns.item.sonicShotgun.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 29 [loaded]: grid `· p ·` / `e r s` / `· c ·` with p = "plateTitanium", e = Sonic Emitter, r = Carbon Receiver, s = Carbon Gunstock, c = Energy Cell; recipe 29e [empty]: grid same with c = Energy Cell (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 25.0 per projectile |
| Minimum damage | 5.0 |
| Drop-off | full damage up to 5 blocks, linear to the minimum at 15 blocks, minimum beyond |
| Time to live / speed | 20 ticks / 1.5 blocks per tick; nominal reach 30 blocks |
| Accuracy | config 0.0, ignored (fixed ring geometry: cone angles 0.075 and 0.15 rad) |
| Pellets | 15 ring projectiles (5 + 10) plus the center one = 16 per shot, all sharing one hit list |
| Armor penetration | 25% (0.25) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 12 ticks (max 1.67 per second) |
| Reload | 40 ticks (2 s); the gun refills at once and the player-wide `fireDelay` is set to 28 ticks (reload minus `minFiretime`) |
| Clip | 8 rounds |
| Ammo per reload | 1 x Energy Cell (shared item meta 15) |
| Empty returned | 1 x Energy Cell (Empty) (shared item meta 16) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 18 blocks, attack time 40 ticks (variance 13), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.sonicShotgun` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [25.0, 5.0, 5, 15, 20, 0.0, 0.25]; indices ignored by this gun: 5 Accuracy |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (12), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Semi-auto. `spawnProjectile` ignores the spread passed in (so there is no zoom, armor or NPC accuracy effect) and uses a fixed ring angle of 0.075 rad. One center [`SonicShotgunProjectile`](../04-projectiles-fx/projectiles.md#313-sonicshotgunprojectile) flies along the look vector (flagged as the main projectile, plus the `SonicShotgunTrail` packet), then ring 1 adds 5 projectiles at a 0.075 rad cone and ring 2 adds 10 at 0.15 rad (about 4.3 and 8.6 degrees): 16 projectiles per shot.
- All 16 share one hit list, so together they damage each entity at most once. Damage and minimum are scaled by the NPC bonus; the forward offset is honored.
- The tooltip type says Projectile because there is no `getDamageType` override, while the hits are PHYSICAL `tg_sonic` with knockback x2 (`GUN-20`). Death message `death.attack.tg_sonic`.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 8) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 8, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 8). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Energy Cell`
  - `Damage: 25.0-5.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: gray):
  - `Ammo: Energy Cell`
  - `DamageType: Projectile`
  - `Damage: 25.0-5.0`
  - `Range: 5,15,20`
  - `Spread: 0.0`
  - `Clipsize: 8`
  - `ReloadTime: 2.0s`
  - `ArmorPen: 25.0%`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`SonicShotgunProjectile` (entity id 20), 16 per shot](../04-projectiles-fx/projectiles.md#313-sonicshotgunprojectile). Damage source: `tg_sonic / PHYSICAL (knockback 2)`.
- Item renderer `RenderSonicShotgun` with model [`ModelSonicShotgun`](../03-gun-visuals-audio/gun-models.md#modelsonicshotgun-12864-family-x-65-boxes-tabula-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#sonic-shotgun-sonicshotgun)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/sonicshotgun.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×64, average #3C4447, dominant #303030 35%, #505050 18%, #101010 8%, 55% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashBlue` at offset (0, 0.05, -0.35); turret offsets: gun none, muzzle none.
- Trail FX `SonicShotgunTrail` is sent for the center projectile.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.sonicshotgunFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.59 to 1.76 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.sonicshotgunReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.95 s, 48000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Energy Cell (meta 15), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#sonic-shotgun-sonicshotgun); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-20`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Scatter Laser Rifle [WIP] (`scatterBeamGun`)

**1. Identity.**

- Catalog row 33 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `Lasergun` (`java/techguns/items/guns/`).
- Registry name `Techguns:scatterBeamGun` (`GameRegistry.registerItem(..., "scatterBeamGun")`); unlocalized name `techguns.item.scatterBeamGun`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 40). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Scatter Laser Rifle [WIP]", lang key `techguns.item.scatterBeamGun.name`.

**2. Obtaining.**

- Crafting: none. Unobtainable in survival: creative tab and commands only (no loot table or NPC carries it).
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 6.0 per projectile |
| Minimum damage | 6.0 (no drop-off: start and end are both 5, equal to the time-to-live) |
| Time to live / speed | 5 ticks / 7.5 blocks per tick; nominal reach 37.5 blocks |
| Accuracy (spread) | 0.1, an independent uniform random offset of +-4 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus) and by 0.75 while zoomed |
| Pellets | 4 extra projectiles per shot (total 5), pellet spread 0.15 (+-6 degrees); one round is used per shot |
| Armor penetration | none (0) |
| Fire mode | full-auto: one round every 7 ticks (2.86 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 38 ticks (reload minus `minFiretime`) |
| Clip | 40 rounds |
| Ammo per reload | 1 x Energy Cell (shared item meta 15) |
| Empty returned | 1 x Energy Cell (Empty) (shared item meta 16) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | toggle on right-click; FOV x0.75, spread x0.75 while zoomed |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 18 blocks, attack time 30 ticks (variance 10), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.scatterBeamGun` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [6.0, 6.0, 5, 5, 5, 0.1, 0.0]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (7), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Built as a plain `Lasergun` (the separate [`ScatterBeamGun`](index.md#coverage) class is never instantiated, `GUN-25`) with `setShotgunSpread(4, 0.15)`: one beam with accuracy 0.1 plus 4 pellets with spread 0.15 (5 `LaserProjectile` per shot, tooltip `Damage(x5)`). Speed 7.5 blocks/tick, TTL 5, no drop-off, scope FOV x0.75 with zoomed spread x0.75.
- No recipe, no loot and no NPC user: creative tab only. "[WIP]" is part of the English name.
- Scope: right-click toggles the client zoom; the zoom flag travels in `PacketShootGun` and is trusted by the server (`GUN-12`). Switching to another gun with the same zoom multiplier keeps the zoom.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 40) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 40, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 40). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Energy Cell`
  - `Damage(x5): 6.0-6.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark aqua):
  - `Ammo: Energy Cell`
  - `DamageType: Energy`
  - `Damage(x5): 6.0`
  - `Range: 5,5,5`
  - `Spread: 0.1`
  - `Clipsize: 40`
  - `ReloadTime: 2.25s`
  - `Zoom:(toggle) Mult:0.75`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`LaserProjectile` (entity id 4), which also spawns a `LaserBeam2` visual](../04-projectiles-fx/projectiles.md#33-laserprojectile). Damage source: `tg_energy / ENERGY`.
- Item renderer `RenderLaserGunNew` with model [`ModelLaserGunNew`](../03-gun-visuals-audio/gun-models.md#modellasergunnew-scatter-beam-gun-128128-family-z-44-boxes-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#scatter-beam-gun-scatterbeamgun)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/laserGunNew.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×128, average #515150, dominant #505050 77%, #303030 15%, #909090 3%, 65% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun none, muzzle none.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.lasergunFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.56 to 1.79 s, 44100 Hz, stereo, sounds.json category `player` |
| `guns.lasergunReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.05 s, 32000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Energy Cell (meta 15), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#scatter-beam-gun-scatterbeamgun); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-25`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-12`, `GUN-07`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Nuclear Death Ray (`nuclearDeathRay`)

**1. Identity.**

- Catalog row 34 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `Beamgun` (`java/techguns/items/guns/`).
- Registry name `Techguns:beamtestgun` (`GameRegistry.registerItem(..., "beamtestgun")`); unlocalized name `techguns.item.nuclearDeathRay` (registry name differs from the unlocalized name).
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 100). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Nuclear Death Ray", lang key `techguns.item.nuclearDeathRay.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 31 [loaded]: grid `l t l` / `e r s` / `· m ·` with l = "plateLead", t = "plateTitanium", e = Rad Emitter, r = Carbon Receiver, s = Carbon Gunstock, m = Nuclear Power Cell; recipe 31e [empty]: grid same with m = Nuclear Power Cell (Depleted).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 6.0 per projectile |
| Minimum damage | 1.0 |
| Drop-off | full damage up to 20 blocks, linear to the minimum at 40 blocks, minimum beyond |
| Time to live / speed | 5 ticks / 7.5 blocks per tick; hitscan beam length up to 48 blocks ([`04-projectiles-fx`](../04-projectiles-fx/index.md)) |
| Accuracy (spread) | 0.0: no random offset |
| Armor penetration | 35% (0.35) |
| Fire mode | full-auto: one round every 5 ticks (4 per second) |
| Reload | 30 ticks (1.5 s); the gun refills at once and the player-wide `fireDelay` is set to 25 ticks (reload minus `minFiretime`) |
| Clip | 100 rounds |
| Ammo per reload | 1 x Nuclear Power Cell (shared item meta 106) |
| Empty returned | 1 x Nuclear Power Cell (Depleted) (shared item meta 107) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 40 ticks (variance 13), burst 5 shots, burst delay 5 ticks |
| Config key | `WeaponStats_techguns.item.nuclearDeathRay` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [6.0, 1.0, 20, 40, 5, 0.0, 0.35]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0; the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (5), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Registry name `beamtestgun`, unlocalized name [`nuclearDeathRay`](gun-catalog.md#table-a-identity-and-ammunition). `Beamgun` constants `BEAM_LIFETIME` 10, `LIFETIME` 5, `SPEED` 5.0; the builder applies `Lasergun.SPEED` (7.5 blocks/tick). It spawns a [`MovingBeam`](../04-projectiles-fx/projectiles.md#1-registration) (a continuous hitscan beam, not a flying bullet) with NPC bonus scaling; damage 6 falls to 1 between 20 and 40 blocks, penetration 35%.
- Looping sound: `guns.beamgunStart` on the first shot after 10 idle ticks, then `guns.beamgunFire`. Powered by a Nuclear Power Cell (100 shots) that comes back as a Depleted cell.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 100) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 100, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 100). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 1500 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns), `loopSoundDelay` (10) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Nuclear Power Cell`
  - `Damage: 6.0-1.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: green):
  - `Ammo: Nuclear Power Cell`
  - `DamageType: Radiation`
  - `Damage: 6.0-1.0`
  - `Range: 20,40,5`
  - `Spread: 0.0`
  - `Clipsize: 100`
  - `ReloadTime: 1.5s`
  - `ArmorPen: 35.0%`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`MovingBeam` (entity id 22)](../04-projectiles-fx/projectiles.md#315-beam-visual-entities). Damage source: `tg_rad / RADIATION`.
- Item renderer `RenderBeamgun` with model [`ModelNDR`](../03-gun-visuals-audio/gun-models.md#modelndr-nuclear-death-ray-6464-68-boxes-tabula-with-child-parts-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#nuclear-death-ray-beamtestgun-field-ndr-unlocalized-nucleardeathray)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/NDR.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 64×64, average #967E6E, dominant #505050 29%, #909090 19%, #B03030 10%, 44% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: none (index 0); turret offsets: gun (0.1, 0, -0.4), muzzle (-0.2, 0, 0).
- The beam carries the FX `BeamGunMuzzleFX` and `BeamGunImpactFX` ([`04-projectiles-fx`](../04-projectiles-fx/index.md)).

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.beamgunStart` | first shot after `loopSoundDelay` ran out (at least 10 idle ticks) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 1.31 to 1.83 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.beamgunFire` | every later shot while firing continues | 4.0 / 1.0, same call | 6 files, 1.01 s, 48000 Hz, mono, sounds.json category `player` |
| `guns.lasergunReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 3.05 s, 32000 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Nuclear Power Cell (meta 106), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0.1, 0, -0.4) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#nuclear-death-ray-beamtestgun-field-ndr-unlocalized-nucleardeathray); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-11`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### De-Atomizer Pistol (`mibGun`)

**1. Identity.**

- Catalog row 40 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `MibGun` (`java/techguns/items/guns/`).
- Registry name `Techguns:mibGun` (`GameRegistry.registerItem(..., "mibGun")`); unlocalized name `techguns.item.mibGun`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 20). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "De-Atomizer Pistol", lang key `techguns.item.mibGun.name`.

**2. Obtaining.**

- Crafting (full text in [recipes.md](recipes.md#shaped-gun-recipes)): recipe 35 [loaded]: grid `b s p` / `· f p` / `· · m` with b = Laser Barrel, s = Sonic Emitter, p = "plateTitanium" (×2), f = Mechanical Parts (Carbon), m = Energy Cell; recipe 35e [empty]: grid same with m = Energy Cell (Empty).
- The loaded recipe outputs item damage 0 (a full clip); the empty recipe (suffix `e`) outputs item damage = clip (an empty gun). Conversion to the `ammo` NBT happens in `onCreated`.
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 16.0 per projectile |
| Minimum damage | 8.0 |
| Drop-off | full damage up to 20 blocks, linear to the minimum at 30 blocks, minimum beyond |
| Time to live / speed | 50 ticks / 1.5 blocks per tick; nominal reach 75 blocks |
| Accuracy (spread) | 0.035, an independent uniform random offset of +-1.4 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | 20% (0.2) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 8 ticks (max 2.5 per second) |
| Reload | 45 ticks (2.25 s); the gun refills at once and the player-wide `fireDelay` is set to 37 ticks (reload minus `minFiretime`) |
| Clip | 20 rounds |
| Ammo per reload | 1 x Energy Cell (shared item meta 15) |
| Empty returned | 1 x Energy Cell (Empty) (shared item meta 16) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 60 ticks (variance 20), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.mibGun` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [16.0, 8.0, 20, 30, 50, 0.035, 0.2]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (8), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- Semi-auto energy pistol. `MibGun` spawns an [`EnergyBlastProjectile`](../04-projectiles-fx/projectiles.md#310-energyblastprojectile-mib-gun) (with the `leftGun` flag for akimbo hand selection) and then sends the `BlueBlasterTrail` packet within 200 blocks. Silencing and forward offset are supported but unused.
- The recipe uses a Laser Barrel, a Sonic Emitter and Titanium Plates.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 20) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 20, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 20). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 2250 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Energy Cell`
  - `Damage: 16.0-8.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark aqua):
  - `Ammo: Energy Cell`
  - `DamageType: Energy`
  - `Damage: 16.0-8.0`
  - `Range: 20,30,50`
  - `Spread: 0.035`
  - `Clipsize: 20`
  - `ReloadTime: 2.25s`
  - `ArmorPen: 20.0%`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`EnergyBlastProjectile` (entity id 26)](../04-projectiles-fx/projectiles.md#310-energyblastprojectile-mib-gun). Damage source: `tg_energy / ENERGY (gore 1.0)`.
- Item renderer `RenderMibGun` with model [`ModelMibGun`](../03-gun-visuals-audio/gun-models.md#modelmibgun-mib-gun-declared-6464-used-with-a-128128-png-family-z-50-boxes-tabula-1-part) ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#mib-gun-mibgun)).
- Texture (measured, from ASSET-FACTS):
  - [`textures/guns/mibgun.png`](../03-gun-visuals-audio/textures.md#measured-colors-all-62-textures) (camo 0, default): 128×128, average #F0F1F1, dominant #F0F0F0 82%, #B0B0B0 9%, #D0D0D0 7%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashAlienBlue` at offset (0, -0.1, 0.1); turret offsets: gun none, muzzle (0, 0, -0.2).
- Trail FX `BlueBlasterTrail` is sent for every shot.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.MIBblasterFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 2.34 to 2.44 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.MIBblasterReload` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.29 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Energy Cell (meta 15), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: accepted by the turret base gun slot (`SlotTechgun`); `NPCTurret` fires it through `fireWeaponFromNPC` with damage and accuracy factors 1.0, uses the AI stats above for its follow range and attack timing, and draws it with the turret gun offset (0, 0, 0) ([09-machines-utility/turret.md](../09-machines-utility/turret.md), [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#17-turret-npcturret-registry-name-simpleturret)).
- Recipes and machines: Input of the Akimbo De-Atomizer Pistols combine recipe.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#mib-gun-mibgun); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: none.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

---

### Akimbo De-Atomizer Pistols (`mibGunAkimbo`)

**1. Identity.**

- Catalog row 43 in [gun-catalog.md](gun-catalog.md#table-a-identity-and-ammunition). Class `MibGun (akimbo, alternating)` (`java/techguns/items/guns/`).
- Registry name `Techguns:mibGunAkimbo` (`GameRegistry.registerItem(..., "mibGunAkimbo")`); unlocalized name `techguns.item.mibGunAkimbo`.
- Metadata: none (a single item; the item-damage field only carries "rounds missing" out of crafting recipes, maximum 40). Creative tab `Techguns` (`techguns.itemGroup.Techguns`). Ore dictionary names: none.
- English name "Akimbo De-Atomizer Pistols", lang key `techguns.item.mibGunAkimbo.name`.

**2. Obtaining.**

- Crafting: shapeless [`RecipeAkimboGun`](recipes.md#shapeless--custom-recipes) with two De-Atomizer Pistols (result NBT is the first gun's tag with `ammo` set to the sum of both guns' rounds). One Akimbo can be split back by [`RecipeAkimboSplit`](recipes.md#shapeless--custom-recipes), which yields two single guns through [`TechgunsTickHandler.onCraftingEvent`](../01-core/events.md#techgunstickhandler-fml-bus). See [recipes.md](recipes.md#shapeless--custom-recipes).
- Loot: none.

**3. Numbers.**

| Stat | Value |
|---|---|
| Damage (max) | 16.0 per projectile |
| Minimum damage | 8.0 |
| Drop-off | full damage up to 20 blocks, linear to the minimum at 30 blocks, minimum beyond |
| Time to live / speed | 50 ticks / 1.5 blocks per tick; nominal reach 75 blocks |
| Accuracy (spread) | 0.04, an independent uniform random offset of +-1.6 degrees on yaw and pitch; multiplied by (1 - armor accuracy bonus); no scope, so no zoom multiplier |
| Armor penetration | 20% (0.2) |
| Fire mode | semi-auto: one shot per click; `minFiretime` 5 ticks (max 4 per second) |
| Akimbo | alternating: 1 round per pull, hand by parity of the rounds left |
| Reload | 60 ticks (3 s); the gun refills at once and the player-wide `fireDelay` is set to 55 ticks (reload minus `minFiretime`) |
| Clip | 40 rounds |
| Ammo per reload | 2 x Energy Cell (shared item meta 15) |
| Empty returned | 2 x Energy Cell (Empty) (shared item meta 16) when a reload consumes ammo |
| Forced-reload refund | none (energy / fuel / air container: the empty container is returned but no bullet item, `GUN-13`) |
| Zoom | none (right-click does nothing) |
| Animation times | recoil 5 ticks (250 ms); muzzle flash 5 ticks (250 ms) |
| AI stats | range 24 blocks, attack time 60 ticks (variance 20), burst 0 shots, burst delay 0 ticks |
| Config key | `WeaponStats_techguns.item.mibGunAkimbo` (category `Weapon Stats` in `Techguns_WeaponStats.cfg`), default list [16.0, 8.0, 20, 30, 50, 0.04, 0.2]; indices all honored |
| Other config keys | `DamageFactorNPC` (1.0), `DamageTurretToPlayer` (0.5), `DamagePvP` (0.5), `ArmorPenetrationMultiplierNPCtoPlayer` and `ArmorPenetrationMultiplierPvP` (1.0) scale hits; `ForceSafeModeForNonOps` (false) controls whether non-ops may turn safe mode off |

**4. Behavior.**

- Fire cycle (shared logic, [gun-lifecycle.md](gun-lifecycle.md#5-firing-shootgunleftclickstack-world-player-iszooming)): the client sends `PacketShootGun` and predicts the shot when `fireDelay` is 0 (semi-auto: the held flag is cleared after each evaluation); the server then spawns the projectile(s), sets `fireDelay` to `minFiretime` (5), broadcasts `GunFiredMessage` within 100 blocks and plays the fire sound. A trigger pull on an empty gun reloads (section 7 of the lifecycle file).
- `setAkimbo(false)`: reload consumes 2 Energy Cells for 40 rounds; hands alternate by parity, and the `leftGun` flag selects the muzzle side of the `EnergyBlastProjectile`. Compared with the single gun: `minFiretime` 5 (vs 8), clip 40, reload 60, accuracy 0.040 (vs 0.035), sounds `guns.MIBblasterFire` and `guns.MIBblasterReloadAkimbo`.
- Combined and split by crafting (`GUN-26`). Rejected by the turret gun slot.
- Client / server: spawn, ammo authority and `fireDelay` are server-side; recoil, muzzle flash and zoom are client-side. Both sides hold independent `fireDelay` counters (`GUN-32`).
- Automation / redstone: only the dispenser behaviors noted in section 9; no sided inventories.

**5. Persistence and sync.**

- Stack NBT: `ammo` (short, rounds loaded, 0 to 40) and `camo` (byte, selected variant, 0 to 0). Created by `onCreated` / `getSubItems` (`ammo` = 40, `camo` = 0). There is no other persisted state.
- Item damage: unused as wear (the durability bar shows 1 - ammo / 40). It carries "rounds missing" from empty-variant recipes and from vanilla equipment-drop wear into NBT on the first read.
- Camo: only one texture, so `switchCamo` returns 0; it still creates a tag holding only `camo` on a tag-less stack, which then reads as empty (`GUN-01`).
- Packets: `PacketShootGun` (client to server, 1 boolean: zoom flag); `GunFiredMessage` (server to clients within 100 blocks: recoil 250 ms, attackType, checkRecoil, akimbo, doubleShot); `ReloadStartedMessage` (server to clients within 100 blocks: 3000 ms). Wire formats are in [01-core/networking.md](../01-core/networking.md).
- Per-player state in `TechgunsExtendedPlayerProperties`: `fireDelay` (shared by all guns) and `enableSafemode`. DataWatcher indices and GUI progress fields: none.

**6. UI.**

- Tooltip without SHIFT (computed with float32 arithmetic as the game does; the last digit of a float may differ):
  - `Ammo: Energy Cell`
  - `Damage: 16.0-8.0`
  - `Press [SHIFT] for detailed tooltip`
- Tooltip with SHIFT held (the `Ammo` line is white-colored, the type word takes the damage-type color: dark aqua):
  - `Ammo: Energy Cell`
  - `DamageType: Energy`
  - `Damage: 16.0-8.0`
  - `Range: 20,30,50`
  - `Spread: 0.04`
  - `Clipsize: 40`
  - `ReloadTime: 3.0s`
  - `ArmorPen: 20.0%`
- The durability bar is always visible; the camo name appears in the Camo Bench. Chat messages: none. No GUI of its own.

**7. Visuals.**

- Projectile fired: [`EnergyBlastProjectile` (entity id 26)](../04-projectiles-fx/projectiles.md#310-energyblastprojectile-mib-gun). Damage source: `tg_energy / ENERGY (gore 1.0)`.
- Item renderer `RenderMibGunAkimbo` with model `ModelAkimbo(ModelMibGun)` ([03 sheet](../03-gun-visuals-audio/gun-sheets.md#akimbo-mib-guns-mibgunakimbo)).
- Texture (measured, from ASSET-FACTS):
  - `textures/guns/mibgun.png` (camo 0, default): 128×128, average #F0F1F1, dominant #F0F0F0 82%, #B0B0B0 9%, #D0D0D0 7%, 100% opaque.
- Camo variants: 1 (no alternative camo).
- Muzzle flash particle: `MuzzleFlashAlienBlue` at offset (0, -0.1, 0.1); turret offsets: gun none, muzzle (0, 0, -0.2).
- Trail FX `BlueBlasterTrail` is sent for every shot; the renderer draws a second gun in the left hand.

**8. Audio.** (File durations, rates and channels are measured; what the clips sound like cannot be checked here, so no audible description is given.)

| Event | Trigger | Volume / pitch passed | Files and category |
|---|---|---|---|
| `guns.MIBblasterFire` | each shot, on both sides, and each NPC or turret shot (server) | 4.0 / 1.0 (`playSoundOnEntityGunPosition`, category GUN_FIRE, gun position) | 3 files, 2.34 to 2.44 s, 44100 Hz, mono, sounds.json category `player` |
| `guns.MIBblasterReloadAkimbo` | reload start (trigger on an empty gun, or the force-reload key) | 1.0 / 1.0 (`playReloadSoundOnEntity`, category RELOAD, moving; the client drops it if another reload sound played in the last 500 ms) | 1 file, 2.99 s, 44100 Hz, mono, sounds.json category `player` |
| `mob.villager.idle` (vanilla) | reload attempted with no ammo item in the inventory | 1.0 / 1.0, server only | vanilla sound |

**9. Relationships.**

- Ammo item: Energy Cell (meta 15), documented in [06-items/ammo.md](../06-items/ammo.md). Damage pipeline: [damage-system.md](damage-system.md).
- NPCs: no NPC class in the source is given this gun.
- Turrets: rejected by the turret gun slot (`SlotTechgun` refuses akimbo guns).
- Recipes and machines: Output of the De-Atomizer combine recipe; splits back into two.
- Visual and audio detail: [03-gun-visuals-audio sheet](../03-gun-visuals-audio/gun-sheets.md#akimbo-mib-guns-mibgunakimbo); projectile: [04-projectiles-fx](../04-projectiles-fx/index.md).

**10. Bugs.**

- Specific to this gun: `GUN-26`.
- Shared behavior that applies here: `GUN-17`, `GUN-18`, `GUN-19`, `GUN-13`, `GUN-32`, `GUN-47`, `GUN-29`, `GUN-01` (see the shared list in [sheets-index.md](sheets-index.md#shared-bugs)). Every hit also goes through the damage pipeline bugs `GUN-33` to `GUN-45`.

