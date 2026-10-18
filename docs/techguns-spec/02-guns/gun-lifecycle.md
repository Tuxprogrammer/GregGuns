# 02-guns: gun lifecycle (input → fire → projectile → reload)

This file covers the general behaviour shared by every `GenericGun` (`java/techguns/items/guns/GenericGun.java`). Class-specific overrides are in [per-gun-behaviour.md](per-gun-behaviour.md). Packet wire formats, the extended player properties object and the tick/event handlers are owned by **01-core**. Projectile movement, impact and effects are owned by **04-projectiles-fx**. Rendering, animation and sound files are owned by **03-gun-visuals-audio**.

## 1. Item basics

* `GenericGun` extends `GenericItem` (which prefixes unlocalized names with `techguns.`, so the lang key is `techguns.item.<name>.name`) and implements `ICamoChangeable`.
* Max stack size 1. `setNoRepair()` disables anvil and crafting repair. Not enchantable (enchantability 0). Creative tab `Techguns.tabTechgun`.
* `maxDamage = clipsize`, but the damage value is **not** wear. It only carries "how many rounds are missing" from a crafting recipe into NBT (see §2). Guns never wear out.
* The durability bar is always shown. Its fill is `1 − ammo/clipsize` (an empty gun shows an empty bar). If the stack has no NBT, it falls back to `damage/maxDamage`.
* Every constructed gun adds itself to a static list (`GenericGun.getGuns()`). The weapon-stats config iterates this list (see catalogue → config).

## 2. NBT and ammo storage

| Key | Type | Meaning |
|---|---|---|
| `ammo` | short | Rounds currently loaded. |
| `camo` | byte | Selected camo index (0 = default texture). |

* **Initialisation (`onCreated`)**: only runs if the stack has no NBT at all. It creates the tag with `camo=0` and `ammo = clipsize` when the item damage is 0, otherwise `clipsize − damage`. It then resets the item damage to 0. It is called by vanilla when a gun is taken out of a crafting grid, and lazily by `getCurrentAmmo` whenever a tag-less stack is read. Recipes that produce `new ItemStack(gun, 1, gun.getMaxDamage())` therefore yield an **empty** gun; recipes with damage 0 yield a **full** gun.
* Creative-menu stacks (`getSubItems`) are created with `camo=0` and `ammo=clipsize`.
* `useAmmo(n)` subtracts n, clamped at 0. `reloadAmmo(n)` adds n with **no** upper clamp. `reloadAmmo()` with no argument adds `clipsize`, which is only called when the gun is empty. `isFullyLoaded` is `ammo == clipsize`.
* **Camo** (`ICamoChangeable`, used by the camo bench in 09): `getCamoCount` is the number of texture variants. `switchCamo(back)` cycles the index forward or backward, wrapping in `[0, camoCount)`, and returns 0 when only one variant exists. If the stack had no tag, it creates one containing only `camo` (see bug GUN-01). `getCurrentCamoName` returns the lang string `techguns.item.<name>.camoname.<id>` for id > 0, or `techguns.item.defaultcamo` ("Default Camo").

## 3. Per-player state used by guns (owned by 01-core)

These fields live in `TechgunsExtendedPlayerProperties` and exist separately on the client and the server.

| Field | Use |
|---|---|
| `fireDelay` (int ticks) | Shared by **all** guns of that player. It is decremented by 1 at the start of every player tick (both sides) while > 0. A shot sets it to `minFiretime`. A reload sets it to `reloadtime − minFiretime`. Switching guns doesn't reset it. |
| `loopSoundDelay` (int ticks) | Controls start/loop sound selection for looping guns. Decremented each tick on both sides. |
| `swingSoundDelay` (byte) | Server-only throttle for the chainsaw and power hammer swing sounds. Set to 5 when a sound is sent, decremented each server tick. |
| `enableSafemode` (bool, default true) | When safe mode is ON, projectiles do **no** block damage, and fire weapons don't ignite. Toggled with a keybind ("Toggle Safe Mode"). The server refuses to turn it off for players that `TGConfig.allowUnsafemode` rejects. |

## 4. Input (client)

1. **Left mouse button** (`TechgunsEventhandler.onMouseEvent`, client): while holding a gun whose `shootWithLeftClick` is true, the LMB event is cancelled and `ClientProxy.keyFirePressed` mirrors the button state. For tool guns (`shootWithLeftClick=false`), LMB is left to vanilla (mining and melee), but an LMB press is cancelled while the client reload timer is running.
2. **Every client player tick** (START phase, own player only, and only while the game window has focus): if the held item is a left-click gun and `keyFirePressed` is set:
   * if `fireDelay ≤ 0`: send `PacketShootGun(isZooming)` to the server, then run `shootGunLeftClick` locally (client-side prediction);
   * then, if the gun is semi-auto, clear `keyFirePressed`. This happens **whether or not** a shot was fired, so a click during cooldown is swallowed.
   * If the held item isn't a left-click gun, or the window isn't focused, `keyFirePressed` is cleared.
3. Vanilla breaking and attacking is suppressed for left-click guns in three ways: a `BreakSpeed` handler at HIGHEST priority cancels it (newSpeed −1), `onBlockStartBreak` returns true, and `onLeftClickEntity` returns true. `onEntitySwing` also returns true, which cancels the arm swing.
4. **Right mouse button** goes through vanilla `onItemRightClick`:
   * Normal guns: if the gun has toggle zoom, the client flips `ClientProxy.player_zoom` between 1.0 and `zoomMult`. Nothing else happens. Guns without zoom do nothing on right-click.
   * Charge guns (Biogun, Power Hammer) and the Chainsaw override this (see per-gun file).
5. **Force reload key** (`TGKeybinds`, `KEY_FORCE_RELOAD`): if the held item is a gun, `fireDelay ≤ 0` and the gun isn't full, the client sends `PacketTGKeybindPress(FORCE_RELOAD)` and runs `tryForcedReload` locally. The server handler calls `tryForcedReload` on the held gun.

### Zoom state machine (client)

* `player_zoom` is a single float in `ClientProxy`. The FOV event multiplies the new FOV by `player_zoom`, and also by the optional speed-FOV lock factor from config (01).
* `isZooming()` is true when `player_zoom == this.zoomMult`.
* Every render tick: if the held item isn't a gun, zoom resets to 1. For a gun with *hold* zoom (`toggleZoom=false`, which no shipped gun uses), zoom is `zoomMult` while sneaking and 1 otherwise. For toggle guns, zoom resets to 1 if this gun isn't the one zooming. In practice switching to another gun with the **same** `zoomMult` keeps the zoom.
* Starting a reload (normal or forced) resets toggle zoom to 1.
* Shots fired while zoomed multiply their spread by `zoombonus` (see §5). The zoom flag is sent to the server in `PacketShootGun` and the server trusts it (GUN-12).

## 5. Firing: `shootGunLeftClick(stack, world, player, isZooming)`

It runs on the **client** (prediction, from the tick handler) and on the **server** (from `PacketShootGun`). Both sides change the stack's NBT, and the server's copy is authoritative.

```
ammo = current ammo
attackType = 0; if akimbo and not doubleShot: attackType = ammo mod 2   (alternates hands)
if ammo > 0:
    if fireDelay <= 0:
        fireDelay = minFiretime
        if not creative: use 1 round (2 if doubleShot)
        if akimbo, creative and not doubleShot: flip parity (use 1 if even, add 1 if odd) so hands still alternate
        SERVER: accuracyBonus = 1 − armour GUN_ACCURACY bonus (05-armor)
                if isZooming: accuracyBonus *= zoombonus
                shootGun(world, player, stack, accuracyBonus, damageBonus = 1.0, attackType)
        CLIENT: start local recoil animation (recoiltime → ms), unless checkRecoil and still recoiling
                set local muzzle-flash timer (muzzleFlashtime → ms)
                client_weaponFired() hook (no-op in all shipped guns)
        BOTH:   sound (see below)
    else: nothing (cooldown)
else: start a reload (§7)
```

**Fire sound selection** (both sides, through `SoundUtil.playSoundOnEntityGunPosition` with volume/distance 4.0, pitch 1.0 and category `GUN_FIRE`; transport details in 01/03):

* If `maxLoopDelay > 0` and `loopSoundDelay ≤ 0`: play `firesoundStart` and set `loopSoundDelay = maxLoopDelay`.
* Otherwise: play `firesound`. If `maxLoopDelay > 0`, refresh `loopSoundDelay = maxLoopDelay`.

So the Flamethrower, Chainsaw and Nuclear Death Ray play their "start" sound on the first shot after at least 10 idle ticks, then the loop sound on each later shot.

* If `rechamberSound` isn't empty, it also plays after every shot (volume 1.0, category `RELOAD`). This is used by the Bolt Action and the Combat Shotgun.

### `shootGun(world, shooter, stack, accuracyBonus, damageBonus, attackType)` (server)

1. Send `GunFiredMessage(shooter, recoiltime in ms, attackType, checkRecoil, akimbo, doubleShot)` to all clients within 100 blocks. The client handler starts the recoil animation on that entity (unless `checkRecoil` and still recoiling) and spawns muzzle FX. For a doubleShot it spawns FX for both hands; for alternating akimbo, `attackType 1` means the left hand (03).
2. `leftGun = akimbo && attackType == 1`.
3. Spawn the main projectile with spread `accuracy × accuracyBonus`, forward offset 0, damage bonus `damageBonus`, `leftGun`.
4. If akimbo and doubleShot: spawn a second main projectile on the left side.
5. If the gun is a shotgun: spawn `bulletcount` extra projectiles with spread `spread × accuracyBonus` (the pellet spread, not `accuracy`). If `burst`, pellet *i* (1-based) is shifted forward by `i / bulletcount` × its per-tick motion.

### Default `spawnProjectile` (GenericGun)

It creates a `GenericProjectile` (04) with `damage × damageBonus`, `speed`, `ticksToLive`, the given spread, `damageDropStart`, `damageDropEnd`, `damageMin × damageBonus`, `penetration`, `blockDamage = getDoBlockDamage(shooter)` and `leftGun`. It calls `setSilenced()` if silenced, and shifts forward by the offset if one was given.

* `getDoBlockDamage(shooter)` is true only if the shooter is a player whose safe mode is **off**. NPCs never do block damage through this path.
* How the projectile applies spread (04, summarised): the start orientation is the shooter's head yaw and pitch, each plus an independent uniform random offset in ±`spread × 40`°. The spawn point is at eye height, 0.1 below, and 0.16 blocks to the right (to the left for `leftGun`). The velocity is the normalised look vector × `speed`. Damage drop-off is measured from the projectile's first-tick position.

## 6. Damage bonus and NPC firing (`fireWeaponFromNPC(shooter, dmgScale, accScale)`)

Used by `GenericNPC.attackEntityWithRangedAttack` and `NPCTurret` (10-entities-npcs).

* It plays `firesound` at the gun position (volume 4.0).
* Turrets (`NPCTurret`): difficulty factor dmg 1.0, acc 1.0.
* Other shooters, by world difficulty:

| Difficulty | dmg factor | acc factor |
|---|---|---|
| Peaceful | 0.0 | 1.3 |
| Easy | 0.5 | 1.3 |
| Normal | 0.6 | 1.15 |
| Hard | 0.75 | 1.0 |

* Server only: calls `shootGun(world, shooter, null stack, accuracyBonus = zoombonus × accScale × acc, damageBonus = dmgScale × dmg, attackType 0)`.
* `GenericNPC` passes `dmgScale = 0.5` and `accScale` set to the same difficulty acc factor (1.3/1.15/1.0, 1.0 on Peaceful). Turrets pass 1.0/1.0. Effective NPC damage is therefore 0.25 / 0.30 / 0.375 of the listed damage on Easy/Normal/Hard. Effective NPC spread is `accuracy × zoombonus × acc²` (GUN-06, GUN-07).
* NPCs never use ammo, never reload and never apply zoom or armour accuracy bonuses (other than the zoombonus above). Later, when the projectile hits, `DamageSystem.getDamageFactor` applies a further factor (see damage-system.md).
* Guns that ignore `damageBonus`: Biogun (GUN-03), Grenade Launcher (GUN-04), Power Hammer (fires a plain bullet for NPCs, GUN-08).

### AI stats → `EntityAIRangedAttack` (implemented in 10; parameters defined here)

`getAIAttack(mob)` builds the AI with move speed 1.0, `attackTimeVariance = attackTime / 3`, `maxRangedAttackTime = attackTime`, `attackRange = range` (blocks; compared squared), `maxBurstCount = burstCount` and `shotDelay = burstDelay`. In behaviour terms:

* The mob paths toward the target unless the target is within range and has been seen for at least 20 ticks. It always looks at the target.
* The attack timer counts down every tick. At 0 it fires only if the target is within range and visible. With `f = distance / range` (clamped to 0.1..1 for the value passed to the shot), after each shot:
  * if a burst is configured and shots remain in the burst, the next shot comes after `burstDelay` ticks;
  * otherwise the burst counter is refilled and the next attack comes after `floor(f × (attackTime − attackTime/3) + attackTime/3)` ticks.
* `burstCount` is therefore the number of shots per burst (0 means single shots).

## 7. Reloading

### Automatic reload: trigger pulled with `ammo == 0`

It doesn't check `fireDelay`. In practice the client only sends the packet when `fireDelay ≤ 0`.

1. Try `Techguns.consumeAmmoPlayer(player, ammoStack)` (01-core). This looks first in the Techguns ammo-pouch slots, then in the main inventory. It needs the full stack size (2 for akimbo guns) and is all-or-nothing.
2. On failure: the server plays vanilla `mob.villager.idle` (volume 1, pitch 1) at the player, which is the "out of ammo" grunt. Nothing else happens. With a full-auto gun and the trigger held, this repeats every tick (GUN-11).
3. On success:
   * If the gun has an empty-magazine item, give the player 1 of it (2 for akimbo) via `addAmmoToPlayerInventory` (pouch first, then main inventory). The server drops any overflow as an item entity at the player.
   * Client: toggle zoom resets to 1.
   * `fireDelay = reloadtime − minFiretime`.
   * If `ammoCount > 1` (shell-by-shell guns: Sawed Off 2, Combat Shotgun 8, Grim Reaper 4, Grenade Launcher 6): keep consuming one more ammo item while fewer than `ammoCount` have been taken and ammo is available, then add that many rounds. Otherwise add a full `clipsize`.
   * The reload sound plays through `SoundUtil.playReloadSoundOnEntity` (volume 1, pitch 1, category `RELOAD`) on both sides.
   * Client: start the local reload animation timer (`reloadtime` → ms, attackType). Hook `client_startReload()` is a no-op in shipped guns.
   * Server: send `ReloadStartedMessage(player, reloadtime in ms, attackType)` to all within 100 blocks. Receivers other than the shooter start the reload animation on that entity.
4. The rounds are added **immediately** at reload start. The "reload time" is purely the `fireDelay` lockout plus the animation. The lockout is player-wide, so the player can't fire any gun during it.

Timeline example (M4, minFiretime 3, reload 45): the last round fires at t=0 (fireDelay 3). At t=3 the trigger hits an empty gun, the reload starts, the gun now holds 30, and fireDelay = 42. The next shot is at t=45. The tooltip shows "ReloadTime: 2.25s".

### Forced reload: `tryForcedReload(stack, world, player)`

This only does something if `fireDelay ≤ 0` and the gun isn't full.

1. `oldAmmo` = current rounds. Consume one ammo stack as above. On failure, the server plays `mob.villager.idle` and stops.
2. If `ammoCount ≤ 1` (magazine or full-load guns): the gun is emptied (the `oldAmmo` rounds are removed).
3. If there is an empty-magazine item: return 1 (or 2 for akimbo) empty magazine. Then, if `floor(oldAmmo / shotsPerBullet) > 0`, return that many **bullet** items (pouch, then inventory, overflow dropped). Guns without an empty-magazine item get no refund at all: revolvers, Nether Blaster, launchers, Hand Cannon. Energy and fuel guns get an empty container back but never a refund (GUN-13).
4. Zoom reset. `fireDelay = reloadtime − minFiretime`.
5. If `ammoCount > 1`: take up to `ammoCount − oldAmmo` items in total (the first was already taken) and add that many rounds on top of `oldAmmo`. Otherwise add `clipsize`.
6. Same reload sound and animation as above, but the animation and `ReloadStartedMessage` always use attackType 0.

### Charge guns

Charge guns (`GenericGunCharge`) have their own reload path inside `onItemRightClick`. See per-gun-behaviour.

## 8. Akimbo

* `setAkimbo(doubleShot)` sets `akimbo=true`, multiplies the ammo stack size by 2, and stores `doubleShot`.
* Alternating akimbo (pistols, revolvers, golden revolvers, MIB guns): one round per trigger. The hand is chosen by the parity of the rounds left **before** the shot (odd = left gun = attackType 1).
* Double-shot akimbo (Mac10s): both guns fire every trigger and use 2 rounds. Two projectiles spawn: right, then left.
* Reload consumes 2 ammo items at once, returns 2 empty magazines, and uses the akimbo reload sound.
* Akimbo guns are rejected by the Techguns gun slot (`SlotTechgun.isItemValid`, 01/05).
* Combining and splitting is done through crafting (see recipes.md).
* The renderer draws the second gun on the player (03).

## 9. Tool guns (shootWithLeftClick = false): shared logic in GenericGun

These hooks are shared logic; the per-gun specifics are in per-gun-behaviour.

* `onEntitySwing`: returns true (cancels the vanilla swing) when ammo ≥ `miningAmmoConsumption` (1), otherwise false. The Chainsaw and Power Hammer override it.
* `onLeftClickEntity`: a reimplementation of the vanilla player melee attack with a custom `TGDamageSource` from `getMeleeDamageSource` (default: name `player`, PHYSICAL, gore death). It keeps the vanilla behaviour: attack-damage attribute, enchantment bonus, ×1.5 critical while falling, sprint knockback, fire aspect, crit and magic-crit particles, the Overkill achievement at ≥ 18 damage, `hitEntity`, damage stat and 0.3 exhaustion. After the attack attempt it uses `meleeHitAmmoConsumption` (1) round if available. On the client it also starts the recoil animation (`recoiltime`). It always returns true, which cancels the vanilla attack.
* `onBlockStartBreak` returns false, so breaking is allowed.
* `getDigSpeed(block, meta)`: returns 1 if ammo < 1 or the gun has no tool classes. Otherwise it returns `digSpeed` if the block's harvest tool is null or is one of the gun's tool classes, and 1 for any other tool class.
* `getHarvestLevel(toolClass)`: the stored level for that class, the `default` entry when the class is null, and 0 otherwise.
* `canHarvestBlock(block)`: compares the block's harvest tool and level **for metadata 0** against `getHarvestLevel` (GUN-27).
* `setTool(class, level)` also creates a `default` entry with the first level given.

## 10. Tooltips (`addInformation`, client)

Lang keys and their English strings:

| Key | English |
|---|---|
| `techguns.gun.tooltip.shift1` | Press |
| `techguns.gun.tooltip.shift2` | [SHIFT] |
| `techguns.gun.tooltip.shift3` | for detailed tooltip |
| `techguns.gun.tooltip.ammo` | Ammo |
| `techguns.gun.tooltip.damage` | Damage |
| `techguns.gun.tooltip.damageType` | DamageType |
| `techguns.gun.tooltip.range` | Range |
| `techguns.gun.tooltip.spread` | Spread |
| `techguns.gun.tooltip.clipsize` | Clipsize |
| `techguns.gun.tooltip.reloadTime` | ReloadTime |
| `techguns.gun.tooltip.armorPen` | ArmorPen |
| `techguns.gun.tooltip.zoom` | Zoom |
| `techguns.gun.tooltip.zoom.hold` | hold |
| `techguns.gun.tooltip.zoom.toogle` | toggle (sic, the key is spelled `toogle`) |
| `techguns.gun.tooltip.zoom.multiplier` | Mult |
| `techguns.gun.tooltip.radius` | Radius (grenades) |
| `techguns.gun.tooltip.bounces` | Bounces (grenades) |
| `techguns.tooltip.toolclasses` | Toolclasses |
| `techguns.item.defaultcamo` | Default Camo |
| `techguns.TGDamageType.PHYSICAL` | Physical |
| `techguns.TGDamageType.PROJECTILE` | Projectile |
| `techguns.TGDamageType.EXPLOSION` | Explosion |
| `techguns.TGDamageType.ENERGY` | Energy |
| `techguns.TGDamageType.FIRE` | Fire |
| `techguns.TGDamageType.ICE` | Ice |
| `techguns.TGDamageType.LIGHTNING` | Lightning |
| `techguns.TGDamageType.POISON` | Poison |
| `techguns.TGDamageType.RADIATION` | Radiation |
| `techguns.TGDamageType.UNRESISTABLE` | (missing from en_US.lang, GUN-43) |

Lines built in code (`§x` = chat colour). Without SHIFT held:

1. `Ammo: ` + (`<ammoCount>x ` if ammoCount > 1) + `§f` + localized ammo item name.
2. `Damage` + (`(x<bulletcount+1>)` if the gun is a shotgun or burst gun) + `: <damage>-<damageMin>` (always prints both values).
3. `Press §a[SHIFT]§7 for detailed tooltip`.

With either SHIFT key held:

1. The Ammo line, as above.
2. `DamageType: ` + the coloured type name. Colours: Physical dark-gray, Projectile gray, Fire red, Explosion dark-red, Energy dark-aqua, Poison dark-green, Unresistable black, Ice aqua, Lightning yellow, Radiation green.
3. `Damage[(xN)]: <damage>` + (`-<damageMin>` only if min < max).
4. `Range: <dropStart>,<dropEnd>,<TTL>` (raw numbers; the third value is in ticks).
5. `Spread: <accuracy>`.
6. `Clipsize: <clipsize>`.
7. `ReloadTime: <reloadtime × 0.05>s`.
8. `ArmorPen: <penetration × 100>%`, only if penetration > 0.
9. `Zoom:(toggle) Mult:<zoomMult>` or `Zoom:(hold) …`, only for scoped guns.
10. For tool guns: `Toolclasses`, then one line `  <class> : <level>` per entry, including the internal `default` entry (GUN-28).

Grenades (`GenericGrenade.addInformation`): without SHIFT, `Damage: <damage>` and the SHIFT hint line. With SHIFT: `DamageType: Explosion`, `Damage: <damage>`, `Radius: <radius>`, `Bounces: <maxBounces>`.

HUD helpers used by the ammo overlay (01/03): `getAmmoLeftCountTooltip` (rounds × (bulletcount+1) for burst guns), `getClipsizeTooltip` (likewise) and `getAmmoLeftCount(mags)` (`clipsize / ammoCount × mags`, × (bulletcount+1) for burst guns).

## 11. Sounds triggered by gun mechanics

Event names (all `techguns:` events exist in `sounds.json`; files are documented in 03). The per-gun mapping is in catalogue Table C.

| Event | When it plays | Played by |
|---|---|---|
| `<gun firesound>` | Each shot (and every NPC shot) | `SoundUtil.playSoundOnEntityGunPosition`, vol 4.0, both sides |
| `<gun firesoundStart>`: `guns.flamethrowerStart`, `guns.chainsawLoopStart`, `guns.beamgunStart` | First shot after ≥ 10 idle ticks (loop guns) | same |
| `guns.boltactionRechamber`, `guns.combatshotgunPump` | After every shot of the Bolt Action / Combat Shotgun | same, vol 1.0, category RELOAD |
| `<gun reloadsound>` | Reload start (auto or forced) | `SoundUtil.playReloadSoundOnEntity` (charge guns: `world.playSoundAtEntity`) |
| `mob.villager.idle` (vanilla) | Reload attempted with no ammo in inventory | server `world.playSoundAtEntity`, vol 1, pitch 1 |
| `guns.powerhammerCharge` | Power Hammer charge start (right-click) | `world.playSoundAtEntity`, vol 1 |
| `guns.powerhammerFire` | Power Hammer charge release | server `world.playSoundAtEntity` |
| `guns.biogunFire` | Bio Gun charged-shot release (the auto-fire path uses the normal fire-sound path) | server `world.playSoundAtEntity` |
| `guns.powerhammerSwing` | Power Hammer LMB swing with air, at most every 5 ticks, not while a container GUI is open | server sends `PacketPlaySound` (distance 1.0, vol 1.0, category GUN_FIRE) to players within 50 blocks |
| `guns.chainsawSwing` | Chainsaw LMB swing with fuel (same throttle) | `PacketPlaySound`, distance 2.0 |
| `guns.powerhammerImpactGround` | Block broken by the Power Hammer with air | `world.playSoundEffect` at the block centre, vol 1.0 |
| `guns.chainsawHit` | Block broken by the Chainsaw with fuel | `world.playSoundEffect`, vol 0.65 |
| `random.bow` (vanilla) | Stielgranate / Flashbang throw; pitch `1/(rand·0.4+1.2) + 0.5·charge` | server |
| `guns.grenade_throw` | Frag Grenade throw | server, vol 1, pitch 1 |
| `guns.grenade_pin` | Frag Grenade right-click (start of the wind-up) | `world.playSoundAtEntity`, both sides |

## 12. Client/server split summary

| Step | Client | Server |
|---|---|---|
| Trigger detection | yes (mouse event + tick) | – |
| Ammo decrement and reload NBT | yes (prediction) | yes (authoritative; NBT syncs back through normal inventory sync) |
| `fireDelay`, `loopSoundDelay` | own copy | own copy |
| Projectile spawn | – | yes |
| Recoil / muzzle animation for the shooter | local | `GunFiredMessage` → nearby clients |
| Reload animation | local | `ReloadStartedMessage` → others |
| Zoom | yes (state lives only on the client) | receives the isZooming flag |
| Sounds | both call the SoundUtil helpers (01/03 decide audibility) | |

The client and server fire-delay counters run independently. If the server's counter is still 1 when the packet arrives, the server drops a shot that the client already predicted (GUN-32).
