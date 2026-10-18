# 02-guns: per-class and per-gun behaviour

Numbers are listed in [gun-catalogue.md](gun-catalogue.md). This file describes what each class does **differently** from the shared lifecycle in [gun-lifecycle.md](gun-lifecycle.md). Projectile internals (flight, impact, explosion, chaining, beams) belong to 04-projectiles-fx; only the parameters a gun hands to them are documented here.

## Class hierarchy (`java/techguns/items/guns/`)

```
Item
├── GenericItem
│   └── GenericGun  (ICamoChangeable)
│       ├── Handgun                  hand cannon
│       ├── Flamethrower
│       ├── Lasergun                 laser rifle, scatter laser rifle
│       ├── Beamgun                  nuclear death ray
│       ├── Teslagun
│       ├── AlienBlaster
│       ├── CyberdemonBlaster        nether blaster
│       ├── BlasterRifle
│       ├── MibGun                   de-atomizer pistol (+ akimbo)
│       ├── SonicShotgun             sonic rifle
│       ├── Chainsaw                 tool gun
│       ├── GenericGunCharge         charge-on-hold base
│       │   ├── Biogun
│       │   └── PowerHammer          tool gun (normal + advanced)
│       ├── GenericGunRadius (abstract, + radius stat)
│       │   └── GenericGunRadiusGrav (abstract, + gravity stat)
│       │       ├── RocketLauncher   rocket launcher, grim reaper
│       │       ├── NukeLauncher
│       │       └── GrenadeLauncher
│       ├── Railgun                  UNUSED (registration commented out)
│       ├── NeutrinoGun              UNUSED
│       ├── ScatterBeamGun           UNUSED (scatter gun is built as Lasergun)
│       └── GenericGunTool           UNUSED ("driller", commented out)
└── GenericGrenade                   stielgranate
    ├── FragGrenade
    └── Flashbang                    constructed, never registered
```

Plain `GenericGun` instances (no subclass): pistol, sawed off, bolt action, M4, combat shotgun, thompson, revolver, AK47, AS50, PDW, LMG, Infiltrator, golden revolver, pulse rifle, minigun, AUG, mac10, SCAR, vector, akimbo pistols, akimbo revolvers, akimbo golden revolvers, akimbo mac10s.

Most subclasses only override `spawnProjectile` (which projectile to create) and `getDamageType` (tooltip only). "Ignite" in several classes means `!enableSafemode` for player shooters.

---

## Base-class variants

### GenericGunCharge (`GenericGunCharge.java`)

Fields: `fullChargeTime` (ticks, default 20) and `ammoConsumedOnFullCharge` (default 10). The extended constructor takes `fullChargeTime` as an extra argument.

**Right-click (`onItemRightClick`, both sides):**

1. If the gun can zoom, the player is sneaking and zoom is toggle mode: toggle zoom (client) and stop. No charge gun has zoom, so this never happens.
2. Otherwise, if ammo > 0 and `fireDelay ≤ 0`: set `fireDelay = minFiretime`, put the item "in use" for `getMaxItemUseDuration` (default 288000 ticks), and call the `startCharge` hook.
3. If ammo == 0: a separate reload path. It consumes the ammo stack exactly like the normal reload, but differs in four ways:
   * the empty container goes in through plain `inventory.addItemStackToInventory` and is silently lost if the inventory is full;
   * the reload sound is `world.playSoundAtEntity` (vol 1, pitch 1);
   * the client reload animation uses attackType 1;
   * **no** `ReloadStartedMessage` is sent (GUN-10).

   With no ammo available it plays the villager grunt instead.

**Release (`onPlayerStoppedUsing`, both sides):** `charge = useTicks / fullChargeTime`, capped at 1.0. `ammoUsed = consumeAmmoCharge(stack, charge, creative)` (base version: 1 round, none in creative).

* Server: `spawnChargedProjectile(world, player, stack, accuracy, charge, ammoUsed)`. If shotgun, also `bulletcount` more with `spread`. Then `world.playSoundAtEntity(firesound)` and the rechamber sound if one is set.
* Client: recoil animation (`getRecoilTime(charge)`, base value `recoiltime` in ms, attackType 1), muzzle-flash timer, `client_weaponFired`.
* The release path ignores armour accuracy, zoom bonus and NPC damage bonus. No `GunFiredMessage` is sent, so other players see no recoil or muzzle flash.
* The base `spawnChargedProjectile` spawns a plain `GenericProjectile` with full stats.

Charge guns still inherit the left-click path (`shootGunLeftClick`). The Biogun keeps `shootWithLeftClick = true`, so it can be fired with both buttons.

### GenericGunRadius / GenericGunRadiusGrav

These only add config stats: `radius` (default 1.0) at index 7 and `gravity` (default 0.01) at index 8 of the weapon-stats list. Their config comments are "[Radius - float, area of effect radius in block distance]" and "[Gravity - float, downward velocity of the projectile]".

---

## Individual guns

### Hand Cannon (`handcannon`, Handgun)

A single-shot muzzle-loader. It spawns a `SimpleProjectile` with fixed speed 1.0, gravity 0.015 (the round drops), scale 1.0 and render texture `techguns:textures/entity/handgunBullet.png` (04), plus the gun's damage, drop-off and penetration. The NPC damage bonus applies. Each Stone Bullet loads 1 round. The fire cycle is 12 ticks of recoil lockout, then an 18-tick reload lockout when the trigger is pulled on the empty gun (30 ticks per shot overall).

### 9mm Pistol (`pistol`)

Semi-auto. Its damage drop-off is set twice in the builder; the second call wins (18→25, min 5). One pistol magazine (18 rounds) represents 3 "9mm Bullets" items for refunds.

### Sawed Off (`sawedoff`) and Combat Shotgun (`combatshotgun`)

Shell-by-shell shotguns (`ammoCount = clip`). Each shot fires 1 centre projectile with `accuracy` spread plus 7 pellets with the pellet spread (0.2 / 0.15). A reload from empty loads as many shells as are available, up to the clip. The Combat Shotgun plays a pump sound after every shot and has a long recoil animation (12 ticks).

### Bolt Action Rifle (`boltaction`)

Semi-auto, scoped (FOV ×0.35; zoomed spread ×0.125). Bullet speed ×1.5 (2.25 b/t). Plays the rechamber sound after each shot, with a 15-tick recoil animation. One Rifle Bullets item loads all 6 rounds. Three camos: default, "Olive", "Tactical Black".

### M4 (`m4`), AK47 (`ak47`), AUG (`aug`), SCAR (`scar`), The Infiltrator (`m4_uq`)

Standard automatic rifles that share the Assault Rifle Magazine (and its empty version). SCAR loads only 20 of the magazine's 30. The Infiltrator is silenced (the projectile is flagged silenced, with effects in 04) and has no muzzle-flash particle. Camos: M4 has 5 (Swamp, Desert, Ice, Hazard), AK47 has 2 (Blue), AUG has 5 (Grey, Wood, Sand, Nether), SCAR has 2 (Black).

### Thompson (`thompson`), Mac10 (`mac10`), Vector (`vector`)

SMGs that share the SMG Magazine. The Vector is scoped (FOV ×0.75, spread ×0.35 when zoomed) and has 2 camos (Black).

### Revolver (`revolver`) and Golden Revolver (`goldenrevolver`)

Semi-auto. They load straight from "9mm Bullets" (1 item gives 6 rounds), with no magazine and no refund.

### AS50 (`as50`)

Semi-auto anti-materiel rifle. Scope FOV ×0.35, zoomed spread ×0.125. Speed 2.25 b/t, damage 32 → 24 between 30 and 40 blocks, penetration 25%.

### PDW (`pdw`) and Pulse Rifle (`pulseRifle`)

Burst guns (`setShotgunSpread(n, s, burst=true)`). Each trigger pull uses **1** round but spawns 1 + n projectiles in a line, each later one shifted forward by `i/n` of a tick's travel. That gives 2-round bursts (PDW) or 3-round bursts (Pulse Rifle). The HUD and tooltip multiply round counts by (n+1). The PDW also sets `checkRecoil`. The Pulse Rifle has 1.5× bullet speed and a scope (×0.35, zoomed spread ×0.5). Both use the Advanced Magazine; each has 3 camos (PDW: Silver, Dark Green; Pulse Rifle: Grey, Dark Green).

### LMG (`lmg`)

100-round box, scope ×0.75, 5-second reload.

### Minigun (`minigun`)

`minFiretime 0`: one round every tick (20/s) while the trigger is held. `checkRecoil` keeps the recoil animation from restarting every tick. 200-round drum; a full drum is worth 16 Rifle Bullets for refunds. A spin-up start sound and a pellet spread exist only as commented-out builder calls.

### Flamethrower (`flamethrower`, Flamethrower)

It spawns a `FlamethrowerProjectile` with speed 1.0, gravity 0.01, piercing false and ignite = (player with safe mode off). NPC flamethrowers never ignite. The ignite flag is also passed as the projectile's block-damage flag. Damage and min damage are scaled by the NPC bonus. Sound: `guns.flamethrowerStart` after ≥ 10 idle ticks, otherwise `guns.flamethrowerFire`. 100 "rounds" per Fuel Tank.

### Laser Rifle (`lasergun`, Lasergun) and Scatter Laser Rifle (`scatterBeamGun`, Lasergun)

Class constants: `BEAM_LIFETIME = 5`, `LIFETIME = 5`, `SPEED = 5.0`. Both guns pass `LIFETIME` as TTL and `setSpeed(SPEED)` (giving 7.5 b/t). They spawn `LaserProjectile`. Neither gun has damage drop-off.

* Laser Rifle: one beam, scope ×0.75 (zoomed spread ×0.75).
* Scatter Laser Rifle: `setShotgunSpread(4, 0.15)`, so 5 beams per shot, with 0.1 main accuracy. It also has the scope. It has no crafting recipe and is marked "[WIP]" in its name. A separate `ScatterBeamGun` class exists with identical behaviour but is never instantiated.

### Nuclear Death Ray (`nuclearDeathRay`, registry `beamtestgun`, Beamgun)

Class constants: `BEAM_LIFETIME = 10`, `LIFETIME = 5`, `SPEED = 5.0`, but the builder applies `Lasergun.SPEED`, so speed is 7.5 b/t. It spawns a `MovingBeam` (radiation damage, 04) with NPC bonus scaling. Damage 6 falls to 1 between 20 and 40 blocks, 35% penetration. It has a looping sound (`guns.beamgunStart` then `guns.beamgunFire`). It is powered by a Nuclear Power Cell (100 shots) and returns a Depleted cell.

### Tesla Rifle (`teslagun`, Teslagun)

Constants: `BEAM_LIFETIME 10`, `LIFETIME 10`, `SPEED 5.0`, `CHAIN_RANGE 5.0`. The instance fields `chain = 4` and `chainDmgDrop = 4` are unused (GUN-21). Each shot spawns two entities:

1. a `TeslaBeam` visual entity attached to the shooter;
2. a `TeslaProjectile` with damage and min scaled by the NPC bonus, the gun's drop-off and penetration, **3** chain jumps, chain damage drop 0, and block damage from safe mode.

The projectile forces its own speed (5.0) and lifetime (10), and spread 0, so the config TTL and accuracy have no effect. Chaining logic is in 04. The scope (×0.75, zoomed spread ×1.0) changes FOV only. Muzzle-flash time is 10 ticks.

### Alien Blaster (`alienBlaster`, AlienBlaster)

It spawns an `AlienBlasterProjectile` (speed 1.5, damage and min scaled, ignite = player with safe mode off; NPCs never ignite). The server then sends `PacketSpawnParticleOnEntity("AlienBlasterTrail", projectile)` to all within 200 blocks. Tooltip type is Fire. It has no crafting recipe, so it is creative/loot only (see 10/11).

### Nether Blaster (`cyberdemonBlaster`, CyberdemonBlaster)

It spawns a `CyberdemonBlasterProjectile` with **fixed speed 0.85**, gravity 0, the given spread, piercing false, ignite (default **true**, overridden by safe mode for players; so NPC shooters always ignite), the gun's drop-off and penetration, and NPC damage scaling. Trail particle "CyberDemonBlasterTrail". Ammo is the Nether Charge: 1 charge gives 10 shots, with no empty container. It uses the tesla reload sound.

### Blaster Rifle (`blasterRifle`, BlasterRifle)

It spawns a `BlasterRifleProjectile` (energy). Silencing and forward offset are supported, but neither is used. Scope ×0.5 (zoomed spread ×0.75). Muzzle particle `MuzzleFlashLaser`.

### De-Atomizer Pistol (`mibGun`) and Akimbo De-Atomizer Pistols (`mibGunAkimbo`) (MibGun)

They spawn an `EnergyBlastProjectile` with the `leftGun` flag (so akimbo shots come from the correct hand), then send the trail particle "BlueBlasterTrail". Semi-auto. The akimbo version alternates hands, holds 40 rounds and uses 2 Energy Cells per reload.

### Sonic Rifle (`sonicShotgun`, SonicShotgun)

Semi-auto. It overrides `spawnProjectile` and ignores the passed spread (so no zoom, armour or NPC accuracy effect). It uses a fixed ring angle of 0.075 rad.

* All projectiles share one `entitiesHit` list, so together they damage each entity once (04).
* Centre: one `SonicShotgunProjectile` along the look vector, flagged `mainProjectile`, plus the "SonicShotgunTrail" particle packet.
* Rings: for ring j = 1..2, place 5·j projectiles evenly around the axis (angle step 2π/(5j)) at a cone angle of 0.075·j rad (about 4.3° and 8.6°). That gives 5 + 10 = 15 ring projectiles, 16 in total. Each direction is built from (1,0,0): rotate by the cone angle about Y, by the ring angle about X, then by the shooter's pitch about Z, then by (−headYaw − 90°) about Y. It is then multiplied by `speed`.

  Damage and min are scaled by the NPC bonus. Forward offset is honoured.
* The tooltip says Projectile, but hits are PHYSICAL `tg_sonic` with knockback ×2 (GUN-20). The death message key is `death.attack.tg_sonic`.

### Rocket Launcher (`rocketlauncher`) and Grim Reaper (`grimreaper`) (RocketLauncher)

Radius 1.75, gravity 0.01. They spawn a `RocketProjectile` with damage × NPC bonus, radius, speed 1.0, TTL, spread, gravity, `blockDamage` (players with safe mode off; never for NPCs) and penetration. Explosion damage, knockback (×3) and block damage are in 04. The Grim Reaper is a 4-tube launcher: it loads up to 4 rockets per reload and fires one rocket per trigger pull (6-tick interval). Commented-out code once let shift-right-click toggle block damage in NBT.

### Nuke Launcher (`nukelauncher`, NukeLauncher)

Same as the rocket launcher, but radius 10, damage 250, and it spawns a `NukeRocketProjectile` (explosion knockback ×10). Ammo is the Rocket (Tactical Nuke).

### Grenade Launcher (`grenadelauncher`, GrenadeLauncher)

Semi-auto, 6 rounds, loading up to 6 40mm grenades per reload. Radius 1.5, gravity 0.01. The class has its **own** `speed = 1.0` field that shadows the base field. It spawns a `GrenadeLauncherProjectile` with these arguments:

* damage = the gun's `damage` (not scaled by NPC bonus);
* the radius;
* speed 1.0;
* **TTL 100**, **spread 0.05** and penetration **0**, all hard-coded (the gun's TTL 35, accuracy 0.015 and penetration are ignored);
* the gun's gravity;
* block damage from safe mode;
* **2 bounces**.

It then sends the "GrenadeLauncherTrail" particle packet (GUN-04).

### Bio Gun (`biogun`, Biogun → GenericGunCharge)

`fullChargeTime 30`. `ammoConsumedOnFullCharge` is set to 3 but never read. Use duration is `Integer.MAX_VALUE`.

* **Left click** (normal automatic path, every 6 ticks): 1 round per shot. It fires a level-1 `BiogunProjectile` (charge 0).
* **Right click** (hold then release): the charge level is computed from `charge = held ticks / 30`:

  | charge | level | hold time |
  |---|---|---|
  | ≤ 0.45 | 1 | < ~13.5 ticks |
  | > 0.45 and ≤ 0.95 | 2 | until ~28.5 ticks |
  | > 0.95 | 3 | longer |

  The rounds used equal the level. If ammo < level, the code sets level to `level − ammo`, which is wrong (GUN-02). The projectile is spawned with level = rounds used.
* `BiogunProjectile` gets the **unscaled** `damage` and `damageMin` (GUN-03), speed 1.0, the gun's TTL, drop-off and penetration, and block damage from safe mode. The server sends the "BioGunTrail" particle packet. What each level does (size, splash, poison) is in 04.
* Tooltip type: Poison.

### Power Hammer (`powerHammer`) and Power Hammer (Advanced) (`powerHammerAdv`) (PowerHammer → GenericGunCharge)

Its constructor fixes TTL 2, accuracy 0 and `fullChargeTime 20`, with `ammoConsumedOnFullCharge = 10`. It uses `setShootWithLeftClick(false)`, so LMB is vanilla melee and mining, and the "shot" is a right-click charge.

* **Charge** (RMB): requires ammo > 0 and `fireDelay ≤ 0`. It sets `fireDelay = 4` and plays `guns.powerhammerCharge` at the player.
* **Release**: `consume = floor(10 × charge)`, clamped to 1..10 and then to the available air (none consumed in creative). It spawns a `PowerHammerProjectile` with damage = `consume × 2.5` (2.5 to 25), speed 1.5, TTL 2 (so about 3 blocks of reach), a **4×4-block** collision box, the charge value, the gun's drop-off (powerHammer: 3→3 with min 2.5; Adv: none set, so 2→2 with min 2.5; min is **not** scaled by air used, GUN-09), penetration 0, and block damage from safe mode. It plays `guns.powerhammerFire`. Projectile effects (knockback, block interaction) are in 04.
* **Melee** (LMB on an entity): attribute damage 6 (Adv 7) with air, 2 without. With air, the melee damage source is gore 1.0, armour penetration 0.25 and knockback multiplier 1.5. Without air it uses the default death type. Each hit uses 1 air (shared logic).
* **Mining**: pickaxe and shovel, level 2 (Adv 3). Dig speed 12 (Adv 15) when the block's tool is pickaxe, shovel or none and the hammer has air; otherwise 1. Each block broken with air uses 1 air and plays `guns.powerhammerImpactGround`.
* **Swing** (`onEntitySwing`): with air, the client starts a 250 ms recoil animation (if the previous one is over) and cancels the vanilla swing. The server sends the swing sound `guns.powerhammerSwing` to players within 50 blocks, at most every 5 ticks, and not when the swing came from clicking inside an open container GUI. Without air, the vanilla swing happens.
* The Advanced hammer is crafted from a normal one plus 3 diamonds. NBT is copied, so ammo and camo carry over.
* For NPCs, `fireWeaponFromNPC` uses the inherited `spawnProjectile`, which fires a plain bullet (GUN-08). The AI range is 3.

### Chainsaw (`chainsaw`, Chainsaw)

`setShootWithLeftClick(false)`, recoil 5, penetration 0.25, TTL 2, damage 10, 300 fuel per Fuel Tank.

* **RMB** (`onItemRightClick`) calls `shootGunLeftClick` directly on both sides, with no zoom. Holding RMB repeats at the vanilla right-click rate, limited by `minFiretime 3`. Each "shot" uses 1 fuel and spawns an `InvisibleProjectile` (speed 1.5, TTL 2, so about 3 blocks): damage 10 × NPC bonus, PHYSICAL `chainsaw` source, gore 1.0, no knockback. It plays the loop sound (start `guns.chainsawLoopStart` after ≥ 10 idle ticks, then `guns.chainsawLoop`). An empty saw reloads through the normal path (Fuel Tank in, empty tank out).
* **Melee** (LMB): attribute damage 12 with fuel, 2 without. With fuel the source is gore 1.0, penetration 0.25 and **no** knockback. Each hit uses 1 fuel.
* **Mining**: axe level 3, dig speed 14. Each block broken with fuel uses 1 fuel and plays `guns.chainsawHit` (vol 0.65).
* **Swing**: same as the Power Hammer, but the sound is `guns.chainsawSwing` with distance 2.0.
* Death message keys: `death.attack.chainsaw` ("%s was dismembered by %s with a chainsaw") for the RMB projectile, `death.attack.player` (vanilla) for melee.

### Akimbo Pistols / Revolvers / Golden Revolvers / Mac10s

See lifecycle §8. Stat differences from the single guns:

| Akimbo | vs single |
|---|---|
| pistolAkimbo | minFire 3 (vs 4), clip 36, reload 52, accuracy 0.035 (vs 0.025), flash 3 ticks, refund factor 6 |
| revolverAkimbo | minFire 4, clip 12, reload 70, accuracy 0.035 |
| goldenrevolverAkimbo | minFire 6, clip 12, reload 70, **damage 16** (single 14), drop 15→30 (single 12→30), accuracy 0.035 |
| mac10Akimbo | doubleShot (2 rounds and 2 projectiles per trigger), clip 64, reload 68, accuracy 0.07, AI attackTime 45 / burst 3 / delay 3 |

---

## Grenades (`GenericGrenade`, `FragGrenade`, `Flashbang`)

They extend vanilla `Item` directly, not `GenericGun`, and are modelled on the bow. The unlocalized name gets the `techguns.` prefix (stack variant only).

* Fields: `damage` (20), `radius` (5), `maxBounces` (3). The constructor's `maxUseDur` argument (144000) is stored but **ignored**; the use duration is always 72000 (GUN-23). Max stack size 16, no repair.
* **Right-click**: start using (wind-up). The Frag Grenade also plays `guns.grenade_pin` here.
* **Release** (`onPlayerStoppedUsing`, both sides): with `j` = ticks held, `f = j/20`, then `f = (f² + 2f)/3`, capped at 1. This is the vanilla bow curve, full at 20 ticks.
  * If not creative, consume one grenade item from the inventory (the first matching item, not necessarily the held stack).
  * Server: `spawnGrenade(world, player, f)`.
  * Both sides: swing the arm.
* `spawnGrenade`, common to all three: gravity `0.015 / f` (a weak throw falls fast; f = 0 divides by zero, GUN-22), launch speed `0.5 + 0.5·f`, spread 0.05, penetration 0, block damage false.

  | Item | Projectile | TTL | Damage | Radius | Bounces | Sound |
  |---|---|---|---|---|---|---|
  | Stielgranate | `GrenadeProjectile` | 200 | 20 | 5 | 3 | `random.bow`, pitch `1/(rand·0.4+1.2) + 0.5·f` |
  | Frag Grenade | `FragGrenadeProjectile` | 120 | 25 | 6 | 2 | `guns.grenade_throw` |
  | Flashbang | `FlashBangProjectile` | 200 | 20 | 5 | 0 | `random.bow` (same pitch formula) |

* Tooltip: see lifecycle §10. The damage type is always Explosion.
* The Flashbang is created in `TGuns.init` but never registered with `GameRegistry`, so it doesn't exist in game (GUN-24).

---

## Unused gun classes (dead code, kept for completeness)

| Class | What it would do |
|---|---|
| `Railgun` | Spawns a `RailgunProjectile` with full stats. Its TGuns line (25 dmg, 15 clip, pen 1.0) is commented out. |
| `NeutrinoGun` | Like Beamgun but spawns a `NeutrinoBeam` (energy type). Its TGuns line (8 dmg, drop 24→48) and lang entry "Neutrino Gun [WIP]" exist, but registration is commented out. |
| `ScatterBeamGun` | Identical to the Lasergun spawn logic. Never instantiated. |
| `GenericGunTool` | Spawns a `MiningDummyProjectile` and prints "FIRE MINING PROJ" to stdout. The "driller" gun is commented out. |
| `GenericGun.setProjectileClass` | Deprecated and never read. |
| `onPlayerStoppedUsing` in `GenericGun` | Empty. |
