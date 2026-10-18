# Sound catalogue: `sounds/guns/`

All 185 `.ogg` files in `assets/techguns/sounds/guns/`, the `sounds.json` events that reference them, and exactly when each event plays. Playback mechanics (TGSound, packets, gun position, volume categories) are in [rendering-framework.md §14](rendering-framework.md#14-sound-playback-mechanics-summary). Event names are used with the `techguns:` prefix in code, e.g. `techguns:guns.ak47Fire`.

When an event lists several files, Minecraft picks one at random each time it plays (vanilla `sounds.json` behaviour). Every gun event uses `"category": "player"` except `guns.chainsawLoopStart`, which uses `"master"`. The sound-system category is distinct from Techguns' own volume-multiplier category (GUN_FIRE / RELOAD / …, framework §14). That multiplier is an identity function in v0.1.2 (correction made in the consistency pass).

Measured durations, sample rates and channels for every file, plus the exact volume and pitch each event is played with, are in [§ Volume, pitch and exact trigger per event](#volume-pitch-and-exact-trigger-per-event) and [§ Measured file facts](#measured-file-facts-all-185-files) below. All measured numbers are quoted from [ASSET-FACTS.md](../ASSET-FACTS.md). **What each sound *sounds like* is inferred from its file name, size and use. The audio was not listened to.**

## Common trigger rules (from GenericGun, area 02)

* **Fire sound**: plays on every shot. If the gun has a *loop delay* (`maxLoopDelay` > 0: Flamethrower, Chainsaw, NDR, all 10 ticks), a separate *start* sound replaces the fire sound for the first shot after the player's loop timer has run out. Each shot refreshes the timer to 10 ticks, and it counts down 1 per tick. Holding the trigger therefore gives start, loop, loop, …, and releasing for at least 10 ticks re-arms the start sound. Fire and start sounds are sent at volume 4.0 (`SOUND_DISTANCE`), pitch 1, from 1 block in front of the shooter at half its height. They do not follow the shooter.
* **Rechamber sound** (Bolt-action, Combat Shotgun): played *together with* every fire sound, at volume 1. The visual rechamber happens later in the recoil animation, but the sound itself is not delayed.
* **Reload sound**: plays when a reload starts, whether automatic (firing on empty with ammo available) or forced (reload key). Volume 1. It moves with the shooter. A second local reload sound within 500 ms is suppressed.
* **Empty with no ammo**: vanilla `mob.villager.idle` (not a Techguns file).
* **NPCs** firing a gun (`fireWeaponFromNPC`) play that gun's fire sound the same way. NPCs never play the start or rechamber sounds.
* **Charge guns** (Power Hammer ×2, Bio Gun) play fire, reload and rechamber with plain `world.playSoundAtEntity` at volume 1. These are positional vanilla sounds sent by the server.

## Event table

| Event | Files | Played when | By |
|---|---|---|---|
| guns.assaultRifleFire | ar1_shot_a, ar1_shot_b | fire | M4. Also the GenericGun default fire and start sound |
| guns.assaultRifleReload | ar1_reload | reload | M4, M4 Infiltrator (default reload) |
| guns.handgunFire | handgun_shot_a–c | fire | Hand Cannon |
| guns.handgunReload | handgun_reload | reload | Hand Cannon |
| guns.pistolFire | pistol_shot_a–c | fire | Pistol, Akimbo Pistols |
| guns.pistolReload | pistol_reload | reload | Pistol |
| guns.pistolReloadAkimbo | pistol_reload_akimbo | reload | Akimbo Pistols |
| guns.sawedoffFire | sawedoff_shot_a–c | fire | Sawed-off |
| guns.sawedoffReload | sawedoff_reload | reload | Sawed-off |
| guns.boltactionFire | boltaction_shot_a–c | fire | Bolt-action |
| guns.boltactionRechamber | boltaction_rechamber | with every shot (volume 1) | Bolt-action |
| guns.boltactionReload | boltaction_reload | reload | Bolt-action |
| guns.flamethrowerStart | flamethrower_start_a | first shot of a burst (loop start) | Flamethrower |
| guns.flamethrowerFire | flamethrower_shot_a–e | each subsequent shot (every 2 ticks while held) | Flamethrower |
| guns.flamethrowerReload | flamethrower_reload | reload | Flamethrower |
| guns.combatshotgunFire | combatshotgun_shot_a–c | fire | Combat Shotgun |
| guns.combatshotgunPump | combatshotgun_pump | with every shot (volume 1) | Combat Shotgun |
| guns.combatshotgunReload | combatshotgun_reload | reload | Combat Shotgun |
| guns.lasergunFire | lasergun_shot_a–c | fire | Laser Gun, Scatter Beam Gun |
| guns.lasergunReload | lasergun_reload | reload | Laser Gun, Scatter Beam Gun, Blaster Rifle, NDR |
| guns.rocketFire | rocket_shot_a–c | fire | Rocket Launcher, Nuke Launcher, Grim Reaper. Also the Attack Helicopter NPC firing a rocket (volume 5, area [10](../10-entities-npcs/index.md)). Dispenser use is commented out |
| guns.rocketReload | rocket_reload | reload | Rocket Launcher, Nuke Launcher, Grim Reaper |
| guns.biogunFire | biogun_shot_a–c | charge released | Bio Gun |
| guns.biogunReload | biogun_reload | reload | Bio Gun |
| effects.biogunImpact | biogun_impact_a–c | **never** (the only call is commented out in BiogunProjectile) | – |
| guns.thompsonFire | thompson_shot_a–c | fire | Thompson |
| guns.thompsonReload | thompson_reload | reload | Thompson |
| guns.revolverFire | revolver_shot_a–c | fire | Revolver, Akimbo Revolvers |
| guns.revolverReload | revolver_reload | reload | Revolver, Golden Revolver |
| guns.revolverReloadAkimbo | revolver_reload_akimbo | reload | Akimbo Revolvers, Akimbo Golden Revolvers |
| guns.goldenrevolverFire | goldenrevolver_shot_a–c | fire | Golden Revolver, Akimbo Golden Revolvers |
| guns.ak47Fire | ak47_shot_a–c | fire | AK-47 |
| guns.ak47Reload | ak47_reload | reload | AK-47 |
| guns.as50Fire | as50_shot_a, as50_shot_b | fire | AS50 |
| guns.as50Reload | as50_reload | reload | AS50 |
| guns.teslaFire | teslagun_shot_a–d | fire | Tesla Gun |
| guns.teslaReload | teslagun_reload | reload | Tesla Gun, Cyberdemon Blaster |
| guns.pdwFire | pdw_shot_a–c | fire | PDW |
| guns.pdwReload | pdw_reload | reload | PDW |
| guns.lmgFire | lmg_shot_a–c | fire | LMG |
| guns.lmgReload | lmg_reload | reload | LMG |
| guns.silencedM4Fire | silencedm4_shot_a, _b | fire | M4 Infiltrator |
| guns.pulserifleFire | pulserifle_shot_a–d | fire | Pulse Rifle |
| guns.pulserifleFireStereo | pulserifle_shot_stereo | **never** (no reference in code) | – |
| guns.pulserifleReload | pulserifle_reload | reload | Pulse Rifle |
| guns.cyberdemonblasterFire | cdb_shot_a–d | fire | Cyberdemon Blaster (player or Cyberdemon NPC) |
| guns.minigunFire | minigun_shot_a–d | fire (every tick while held) | Minigun |
| guns.minigunStart | minigun_start_a | **never** (the setter call is commented out) | – |
| guns.minigunReload | minigun_reload | reload | Minigun |
| guns.alienblasterFire | alienblaster_shot_a–c | fire | Alien Blaster |
| guns.alienblasterReload | alienblaster_reload | reload | Alien Blaster |
| guns.powerhammerCharge | powerhammer_charge_a–c | right-click charge begins (volume 1) | Power Hammer, Advanced Power Hammer |
| guns.powerhammerFire | powerhammer_shot_a–c | charge released | Power Hammers |
| guns.powerhammerSwing | powerhammer_swing_a–c | left-click swing with ammo (packet to all within 50 blocks including the swinger, volume 1.0, at most once per 5 ticks, not while a non-inventory GUI is open) | Power Hammers |
| guns.powerhammerImpact | powerhammer_impact_a–d | the hammer projectile hits an entity | Power Hammers |
| guns.powerhammerImpactGround | powerhammer_impactGround_a–d | the hammer projectile hits a block or expires. Also every block mined with ammo (at the block) | Power Hammers |
| guns.powerhammerReload | powerhammer_reload | reload | Power Hammers, Chainsaw |
| guns.blasterrifleFire | blasterrifle_shot_a–c | fire | Blaster Rifle |
| guns.grenadeLauncherFire | grenadelauncher_shot_a–c | fire | Grenade Launcher (dispenser use commented out) |
| guns.grenadeLauncherReload | grenadelauncher_reload | reload | Grenade Launcher |
| guns.augFire | aug_shot_a–c | fire | AUG |
| guns.augReload | aug_reload | reload | AUG |
| guns.sonicshotgunFire | sonicshotgun_shot_a–c | fire | Sonic Shotgun |
| guns.sonicshotgunReload | sonicshotgun_reload | reload | Sonic Shotgun |
| guns.chainsawLoopStart | chainsaw_loopstart | first right-click shot of a run (category master) | Chainsaw |
| guns.chainsawLoop | chainsaw_loop_a–c | subsequent right-click shots (every 3 ticks while held) | Chainsaw |
| guns.chainsawLoopEnd | chainsaw_loopend | **never** (no reference) | – |
| guns.chainsawSwing | chainsaw_swing_a–c | left-click swing with fuel (packet incl. swinger, volume 2.0, once per 5 ticks, not with a GUI open) | Chainsaw |
| guns.chainsawHit | chainsaw_hit_a–f | a block is broken with fuel (volume 0.65, at the block) | Chainsaw |
| guns.beamgunStart | beamgun_start_a–c | first shot of a beam run | NDR |
| guns.beamgunFire | beamgun_loop_a–f | subsequent shots (every 5 ticks while held) | NDR |
| guns.mac10Fire | mac10_shot_a–c | fire | Mac-10, Akimbo Mac-10 (one sound per double shot) |
| guns.mac10Reload | mac10_reload | reload | Mac-10 |
| guns.mac10ReloadAkimbo | mac10_reload_akimbo | reload | Akimbo Mac-10 |
| guns.scarFire | SCAR_shot_a–c | fire | SCAR |
| guns.scarReload | SCAR_reload | reload | SCAR |
| guns.vectorFire | vector_shot_a–c | fire | Vector |
| guns.vectorReload | vector_reload | reload | Vector |
| guns.MIBblasterFire | MIB_blaster_shot_a–c | fire | MIB gun, Akimbo MIB |
| guns.MIBblasterReload | MIB_blaster_reload | reload | MIB gun |
| guns.MIBblasterReloadAkimbo | MIB_blaster_reload_akimbo | reload | Akimbo MIB |
| guns.grenade_pin | grenade_pin_a, _b | right-click on a frag grenade (pin pulled, vanilla positional, volume 1) | Frag grenade |
| guns.grenade_throw | grenade_throw_a–c | frag grenade released (thrown), volume 1, pitch 1 | Frag grenade |

The Stielhandgranate uses vanilla `random.bow`. No Techguns sound is involved.

**Unreferenced files** (in no sounds.json event, so they can never play): `sounds/guns/beamgun_reload.ogg`, `sounds/guns/chainsaw_loop_d.ogg`, `sounds/guns/minigun_start_b.ogg`, `sounds/guns/minigun_start_c.ogg`.

**Byte-identical files:** `chainsaw_hit_a` … `chainsaw_hit_f` are six copies of one sound. `chainsaw_loopstart`, `chainsaw_loopend` and `chainsaw_loop_d` are three copies of one short (10 KB) file.

## Character notes (for replacement sound design)

| Group | Character (inferred from names, sizes and use) |
|---|---|
| `*_shot_*` ballistic | short single gunshots, 3 variations each. Sizes 9–52 KB: the combat shotgun and AS50 are the longest and heaviest, the Thompson and blaster rifle the shortest |
| `*_reload` | one complete reload sequence. It plays once at reload start and runs alongside the reload animation, whose length is the gun's reload time (from 1.4 s for the sawed-off to 5 s for the LMG, minigun and grenade launcher) |
| `*_reload_akimbo` | reloading two guns |
| `boltaction_rechamber`, `combatshotgun_pump` | short mechanical bolt-cycle and pump-rack clacks |
| `flamethrower_start_a` / `_shot_*` | ignition whoosh, then short burning-roar segments that chain together at 2-tick intervals |
| `beamgun_start_*` / `beamgun_loop_*` | beam power-up, then sustained hum segments (fired every 5 ticks) |
| `chainsaw_loop_*`, `chainsaw_swing_*`, `chainsaw_hit_*` | engine buzz segments, revving swing, cutting into wood |
| `powerhammer_charge_*`, `_shot_*`, `_swing_*`, `_impact_*`, `_impactGround_*` | pneumatic wind-up, piston release, whoosh, flesh/armour hit, ground or rock thud |
| `teslagun_shot_*`, `cdb_shot_*`, `alienblaster_shot_*`, `MIB_blaster_shot_*`, `lasergun_shot_*`, `blasterrifle_shot_*`, `pulserifle_shot_*`, `sonicshotgun_shot_*` | energy-weapon zaps, crackles and pulses |
| `grenade_pin_*`, `grenade_throw_*` | pin pull clink, throw whoosh |
| `minigun_start_*` | spin-up whine (unused) |
| `biogun_impact_*` | wet splat (unused) |

## Full file list

| File | Size | sounds.json event |
|---|---|---|
| sounds/guns/MIB_blaster_reload.ogg | 51859B | `guns.MIBblasterReload` |
| sounds/guns/MIB_blaster_reload_akimbo.ogg | 41989B | `guns.MIBblasterReloadAkimbo` |
| sounds/guns/MIB_blaster_shot_a.ogg | 36523B | `guns.MIBblasterFire` |
| sounds/guns/MIB_blaster_shot_b.ogg | 35739B | `guns.MIBblasterFire` |
| sounds/guns/MIB_blaster_shot_c.ogg | 37130B | `guns.MIBblasterFire` |
| sounds/guns/SCAR_reload.ogg | 65989B | `guns.scarReload` |
| sounds/guns/SCAR_shot_a.ogg | 25129B | `guns.scarFire` |
| sounds/guns/SCAR_shot_b.ogg | 25845B | `guns.scarFire` |
| sounds/guns/SCAR_shot_c.ogg | 25563B | `guns.scarFire` |
| sounds/guns/ak47_reload.ogg | 26109B | `guns.ak47Reload` |
| sounds/guns/ak47_shot_a.ogg | 13213B | `guns.ak47Fire` |
| sounds/guns/ak47_shot_b.ogg | 13244B | `guns.ak47Fire` |
| sounds/guns/ak47_shot_c.ogg | 13252B | `guns.ak47Fire` |
| sounds/guns/alienblaster_reload.ogg | 25882B | `guns.alienblasterReload` |
| sounds/guns/alienblaster_shot_a.ogg | 21428B | `guns.alienblasterFire` |
| sounds/guns/alienblaster_shot_b.ogg | 21433B | `guns.alienblasterFire` |
| sounds/guns/alienblaster_shot_c.ogg | 22598B | `guns.alienblasterFire` |
| sounds/guns/ar1_reload.ogg | 19968B | `guns.assaultRifleReload` |
| sounds/guns/ar1_shot_a.ogg | 12314B | `guns.assaultRifleFire` |
| sounds/guns/ar1_shot_b.ogg | 13049B | `guns.assaultRifleFire` |
| sounds/guns/as50_reload.ogg | 46243B | `guns.as50Reload` |
| sounds/guns/as50_shot_a.ogg | 34124B | `guns.as50Fire` |
| sounds/guns/as50_shot_b.ogg | 34275B | `guns.as50Fire` |
| sounds/guns/aug_reload.ogg | 38050B | `guns.augReload` |
| sounds/guns/aug_shot_a.ogg | 19618B | `guns.augFire` |
| sounds/guns/aug_shot_b.ogg | 19898B | `guns.augFire` |
| sounds/guns/aug_shot_c.ogg | 19548B | `guns.augFire` |
| sounds/guns/beamgun_loop_a.ogg | 19087B | `guns.beamgunFire` |
| sounds/guns/beamgun_loop_b.ogg | 21351B | `guns.beamgunFire` |
| sounds/guns/beamgun_loop_c.ogg | 18650B | `guns.beamgunFire` |
| sounds/guns/beamgun_loop_d.ogg | 21511B | `guns.beamgunFire` |
| sounds/guns/beamgun_loop_e.ogg | 20714B | `guns.beamgunFire` |
| sounds/guns/beamgun_loop_f.ogg | 18951B | `guns.beamgunFire` |
| sounds/guns/beamgun_reload.ogg | 71465B | **none** |
| sounds/guns/beamgun_start_a.ogg | 22416B | `guns.beamgunStart` |
| sounds/guns/beamgun_start_b.ogg | 23156B | `guns.beamgunStart` |
| sounds/guns/beamgun_start_c.ogg | 29578B | `guns.beamgunStart` |
| sounds/guns/biogun_impact_a.ogg | 16221B | `effects.biogunImpact` |
| sounds/guns/biogun_impact_b.ogg | 16401B | `effects.biogunImpact` |
| sounds/guns/biogun_impact_c.ogg | 15955B | `effects.biogunImpact` |
| sounds/guns/biogun_reload.ogg | 37124B | `guns.biogunReload` |
| sounds/guns/biogun_shot_a.ogg | 16421B | `guns.biogunFire` |
| sounds/guns/biogun_shot_b.ogg | 16634B | `guns.biogunFire` |
| sounds/guns/biogun_shot_c.ogg | 16861B | `guns.biogunFire` |
| sounds/guns/blasterrifle_shot_a.ogg | 9800B | `guns.blasterrifleFire` |
| sounds/guns/blasterrifle_shot_b.ogg | 9309B | `guns.blasterrifleFire` |
| sounds/guns/blasterrifle_shot_c.ogg | 9290B | `guns.blasterrifleFire` |
| sounds/guns/boltaction_rechamber.ogg | 10062B | `guns.boltactionRechamber` |
| sounds/guns/boltaction_reload.ogg | 13963B | `guns.boltactionReload` |
| sounds/guns/boltaction_shot_a.ogg | 17861B | `guns.boltactionFire` |
| sounds/guns/boltaction_shot_b.ogg | 19084B | `guns.boltactionFire` |
| sounds/guns/boltaction_shot_c.ogg | 18927B | `guns.boltactionFire` |
| sounds/guns/cdb_shot_a.ogg | 19134B | `guns.cyberdemonblasterFire` |
| sounds/guns/cdb_shot_b.ogg | 19691B | `guns.cyberdemonblasterFire` |
| sounds/guns/cdb_shot_c.ogg | 19545B | `guns.cyberdemonblasterFire` |
| sounds/guns/cdb_shot_d.ogg | 19531B | `guns.cyberdemonblasterFire` |
| sounds/guns/chainsaw_hit_a.ogg | 14693B | `guns.chainsawHit` |
| sounds/guns/chainsaw_hit_b.ogg | 14693B | `guns.chainsawHit` |
| sounds/guns/chainsaw_hit_c.ogg | 14693B | `guns.chainsawHit` |
| sounds/guns/chainsaw_hit_d.ogg | 14693B | `guns.chainsawHit` |
| sounds/guns/chainsaw_hit_e.ogg | 14693B | `guns.chainsawHit` |
| sounds/guns/chainsaw_hit_f.ogg | 14693B | `guns.chainsawHit` |
| sounds/guns/chainsaw_loop_a.ogg | 16478B | `guns.chainsawLoop` |
| sounds/guns/chainsaw_loop_b.ogg | 15867B | `guns.chainsawLoop` |
| sounds/guns/chainsaw_loop_c.ogg | 16553B | `guns.chainsawLoop` |
| sounds/guns/chainsaw_loop_d.ogg | 10020B | **none** |
| sounds/guns/chainsaw_loopend.ogg | 10020B | `guns.chainsawLoopEnd` |
| sounds/guns/chainsaw_loopstart.ogg | 10020B | `guns.chainsawLoopStart` (cat. master) |
| sounds/guns/chainsaw_swing_a.ogg | 13241B | `guns.chainsawSwing` |
| sounds/guns/chainsaw_swing_b.ogg | 16026B | `guns.chainsawSwing` |
| sounds/guns/chainsaw_swing_c.ogg | 11581B | `guns.chainsawSwing` |
| sounds/guns/combatshotgun_pump.ogg | 9092B | `guns.combatshotgunPump` |
| sounds/guns/combatshotgun_reload.ogg | 36252B | `guns.combatshotgunReload` |
| sounds/guns/combatshotgun_shot_a.ogg | 38061B | `guns.combatshotgunFire` |
| sounds/guns/combatshotgun_shot_b.ogg | 39637B | `guns.combatshotgunFire` |
| sounds/guns/combatshotgun_shot_c.ogg | 51750B | `guns.combatshotgunFire` |
| sounds/guns/flamethrower_reload.ogg | 23736B | `guns.flamethrowerReload` |
| sounds/guns/flamethrower_shot_a.ogg | 13786B | `guns.flamethrowerFire` |
| sounds/guns/flamethrower_shot_b.ogg | 14147B | `guns.flamethrowerFire` |
| sounds/guns/flamethrower_shot_c.ogg | 12178B | `guns.flamethrowerFire` |
| sounds/guns/flamethrower_shot_d.ogg | 12211B | `guns.flamethrowerFire` |
| sounds/guns/flamethrower_shot_e.ogg | 12171B | `guns.flamethrowerFire` |
| sounds/guns/flamethrower_start_a.ogg | 11115B | `guns.flamethrowerStart` |
| sounds/guns/goldenrevolver_shot_a.ogg | 29779B | `guns.goldenrevolverFire` |
| sounds/guns/goldenrevolver_shot_b.ogg | 27109B | `guns.goldenrevolverFire` |
| sounds/guns/goldenrevolver_shot_c.ogg | 26712B | `guns.goldenrevolverFire` |
| sounds/guns/grenade_pin_a.ogg | 17243B | `guns.grenade_pin` |
| sounds/guns/grenade_pin_b.ogg | 17157B | `guns.grenade_pin` |
| sounds/guns/grenade_throw_a.ogg | 18044B | `guns.grenade_throw` |
| sounds/guns/grenade_throw_b.ogg | 17393B | `guns.grenade_throw` |
| sounds/guns/grenade_throw_c.ogg | 20312B | `guns.grenade_throw` |
| sounds/guns/grenadelauncher_reload.ogg | 43512B | `guns.grenadeLauncherReload` |
| sounds/guns/grenadelauncher_shot_a.ogg | 15964B | `guns.grenadeLauncherFire` |
| sounds/guns/grenadelauncher_shot_b.ogg | 15973B | `guns.grenadeLauncherFire` |
| sounds/guns/grenadelauncher_shot_c.ogg | 16204B | `guns.grenadeLauncherFire` |
| sounds/guns/handgun_reload.ogg | 16692B | `guns.handgunReload` |
| sounds/guns/handgun_shot_a.ogg | 17484B | `guns.handgunFire` |
| sounds/guns/handgun_shot_b.ogg | 20635B | `guns.handgunFire` |
| sounds/guns/handgun_shot_c.ogg | 24921B | `guns.handgunFire` |
| sounds/guns/lasergun_reload.ogg | 27912B | `guns.lasergunReload` |
| sounds/guns/lasergun_shot_a.ogg | 30315B | `guns.lasergunFire` |
| sounds/guns/lasergun_shot_b.ogg | 31839B | `guns.lasergunFire` |
| sounds/guns/lasergun_shot_c.ogg | 28177B | `guns.lasergunFire` |
| sounds/guns/lmg_reload.ogg | 63054B | `guns.lmgReload` |
| sounds/guns/lmg_shot_a.ogg | 18208B | `guns.lmgFire` |
| sounds/guns/lmg_shot_b.ogg | 18412B | `guns.lmgFire` |
| sounds/guns/lmg_shot_c.ogg | 18322B | `guns.lmgFire` |
| sounds/guns/mac10_reload.ogg | 42570B | `guns.mac10Reload` |
| sounds/guns/mac10_reload_akimbo.ogg | 40343B | `guns.mac10ReloadAkimbo` |
| sounds/guns/mac10_shot_a.ogg | 23433B | `guns.mac10Fire` |
| sounds/guns/mac10_shot_b.ogg | 23283B | `guns.mac10Fire` |
| sounds/guns/mac10_shot_c.ogg | 23143B | `guns.mac10Fire` |
| sounds/guns/minigun_reload.ogg | 54640B | `guns.minigunReload` |
| sounds/guns/minigun_shot_a.ogg | 11467B | `guns.minigunFire` |
| sounds/guns/minigun_shot_b.ogg | 11246B | `guns.minigunFire` |
| sounds/guns/minigun_shot_c.ogg | 11258B | `guns.minigunFire` |
| sounds/guns/minigun_shot_d.ogg | 11278B | `guns.minigunFire` |
| sounds/guns/minigun_start_a.ogg | 6540B | `guns.minigunStart` |
| sounds/guns/minigun_start_b.ogg | 6743B | **none** |
| sounds/guns/minigun_start_c.ogg | 6334B | **none** |
| sounds/guns/pdw_reload.ogg | 30006B | `guns.pdwReload` |
| sounds/guns/pdw_shot_a.ogg | 13381B | `guns.pdwFire` |
| sounds/guns/pdw_shot_b.ogg | 13599B | `guns.pdwFire` |
| sounds/guns/pdw_shot_c.ogg | 13380B | `guns.pdwFire` |
| sounds/guns/pistol_reload.ogg | 19762B | `guns.pistolReload` |
| sounds/guns/pistol_reload_akimbo.ogg | 32678B | `guns.pistolReloadAkimbo` |
| sounds/guns/pistol_shot_a.ogg | 20200B | `guns.pistolFire` |
| sounds/guns/pistol_shot_b.ogg | 19351B | `guns.pistolFire` |
| sounds/guns/pistol_shot_c.ogg | 20845B | `guns.pistolFire` |
| sounds/guns/powerhammer_charge_a.ogg | 23269B | `guns.powerhammerCharge` |
| sounds/guns/powerhammer_charge_b.ogg | 23288B | `guns.powerhammerCharge` |
| sounds/guns/powerhammer_charge_c.ogg | 22955B | `guns.powerhammerCharge` |
| sounds/guns/powerhammer_impactGround_a.ogg | 15177B | `guns.powerhammerImpactGround` |
| sounds/guns/powerhammer_impactGround_b.ogg | 15392B | `guns.powerhammerImpactGround` |
| sounds/guns/powerhammer_impactGround_c.ogg | 16326B | `guns.powerhammerImpactGround` |
| sounds/guns/powerhammer_impactGround_d.ogg | 17112B | `guns.powerhammerImpactGround` |
| sounds/guns/powerhammer_impact_a.ogg | 15355B | `guns.powerhammerImpact` |
| sounds/guns/powerhammer_impact_b.ogg | 11403B | `guns.powerhammerImpact` |
| sounds/guns/powerhammer_impact_c.ogg | 15457B | `guns.powerhammerImpact` |
| sounds/guns/powerhammer_impact_d.ogg | 11545B | `guns.powerhammerImpact` |
| sounds/guns/powerhammer_reload.ogg | 33889B | `guns.powerhammerReload` |
| sounds/guns/powerhammer_shot_a.ogg | 28282B | `guns.powerhammerFire` |
| sounds/guns/powerhammer_shot_b.ogg | 20620B | `guns.powerhammerFire` |
| sounds/guns/powerhammer_shot_c.ogg | 23983B | `guns.powerhammerFire` |
| sounds/guns/powerhammer_swing_a.ogg | 15507B | `guns.powerhammerSwing` |
| sounds/guns/powerhammer_swing_b.ogg | 13883B | `guns.powerhammerSwing` |
| sounds/guns/powerhammer_swing_c.ogg | 14390B | `guns.powerhammerSwing` |
| sounds/guns/pulserifle_reload.ogg | 25012B | `guns.pulserifleReload` |
| sounds/guns/pulserifle_shot_a.ogg | 25060B | `guns.pulserifleFire` |
| sounds/guns/pulserifle_shot_b.ogg | 24581B | `guns.pulserifleFire` |
| sounds/guns/pulserifle_shot_c.ogg | 24676B | `guns.pulserifleFire` |
| sounds/guns/pulserifle_shot_d.ogg | 25223B | `guns.pulserifleFire` |
| sounds/guns/pulserifle_shot_stereo.ogg | 33455B | `guns.pulserifleFireStereo` |
| sounds/guns/revolver_reload.ogg | 32320B | `guns.revolverReload` |
| sounds/guns/revolver_reload_akimbo.ogg | 48750B | `guns.revolverReloadAkimbo` |
| sounds/guns/revolver_shot_a.ogg | 16479B | `guns.revolverFire` |
| sounds/guns/revolver_shot_b.ogg | 17161B | `guns.revolverFire` |
| sounds/guns/revolver_shot_c.ogg | 17645B | `guns.revolverFire` |
| sounds/guns/rocket_reload.ogg | 36259B | `guns.rocketReload` |
| sounds/guns/rocket_shot_a.ogg | 38655B | `guns.rocketFire` |
| sounds/guns/rocket_shot_b.ogg | 38722B | `guns.rocketFire` |
| sounds/guns/rocket_shot_c.ogg | 39810B | `guns.rocketFire` |
| sounds/guns/sawedoff_reload.ogg | 17549B | `guns.sawedoffReload` |
| sounds/guns/sawedoff_shot_a.ogg | 13713B | `guns.sawedoffFire` |
| sounds/guns/sawedoff_shot_b.ogg | 14213B | `guns.sawedoffFire` |
| sounds/guns/sawedoff_shot_c.ogg | 13990B | `guns.sawedoffFire` |
| sounds/guns/silencedm4_shot_a.ogg | 11163B | `guns.silencedM4Fire` |
| sounds/guns/silencedm4_shot_b.ogg | 11166B | `guns.silencedM4Fire` |
| sounds/guns/sonicshotgun_reload.ogg | 38387B | `guns.sonicshotgunReload` |
| sounds/guns/sonicshotgun_shot_a.ogg | 17286B | `guns.sonicshotgunFire` |
| sounds/guns/sonicshotgun_shot_b.ogg | 17231B | `guns.sonicshotgunFire` |
| sounds/guns/sonicshotgun_shot_c.ogg | 15992B | `guns.sonicshotgunFire` |
| sounds/guns/teslagun_reload.ogg | 24371B | `guns.teslaReload` |
| sounds/guns/teslagun_shot_a.ogg | 29866B | `guns.teslaFire` |
| sounds/guns/teslagun_shot_b.ogg | 30820B | `guns.teslaFire` |
| sounds/guns/teslagun_shot_c.ogg | 30450B | `guns.teslaFire` |
| sounds/guns/teslagun_shot_d.ogg | 30465B | `guns.teslaFire` |
| sounds/guns/thompson_reload.ogg | 20645B | `guns.thompsonReload` |
| sounds/guns/thompson_shot_a.ogg | 9641B | `guns.thompsonFire` |
| sounds/guns/thompson_shot_b.ogg | 10046B | `guns.thompsonFire` |
| sounds/guns/thompson_shot_c.ogg | 9792B | `guns.thompsonFire` |
| sounds/guns/vector_reload.ogg | 32323B | `guns.vectorReload` |
| sounds/guns/vector_shot_a.ogg | 12757B | `guns.vectorFire` |
| sounds/guns/vector_shot_b.ogg | 12905B | `guns.vectorFire` |
| sounds/guns/vector_shot_c.ogg | 12640B | `guns.vectorFire` |

Total: 185 files (181 referenced by sounds.json, 4 unreferenced).


---

## Volume, pitch and exact trigger per event

*Added in the consistency pass.* Each row gives:
* the event and its `sounds.json` category;
* the measured length of each variant, with the sample rate and channels;
* how the sound is played;
* the **volume and pitch the code passes**;
* the code condition that triggers it.

Transport legend:
* **TGSound at gun position**: [`SoundUtil.playSoundOnEntityGunPosition`](../01-core/utilities.md#soundutil). The shooter's client plays it locally, and the server sends [`PacketPlaySound`](../01-core/networking.md#9--packetplaysound) to every other client within 100 blocks (`playOnOwnPlayer` false). The sound sits 1 block in front of the shooter's head yaw, at half its height. It does not move.
* **TGSound following the shooter**: `SoundUtil.playReloadSoundOnEntity`. Same path, but the sound tracks the entity each tick.
* **vanilla**: `World.playSoundAtEntity` / `playSoundEffect`. The server broadcasts it to nearby players, at a fixed position.

No call randomises pitch. Every Techguns gun sound plays at pitch 1.0. The only randomised pitch belongs to the Stielhandgranate's vanilla `random.bow`, see the next section.

| Event | Category | Durations (s) / rate / channels | Transport | Volume | Pitch | Exact trigger (code condition) | Used by |
|---|---|---|---|---|---|---|---|
| `guns.assaultRifleFire` | player | 1.23, 1.40 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | M4 (also the GenericGun default) |
| `guns.handgunFire` | player | 1.71, 2.01, 2.29 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Hand Cannon |
| `guns.pistolFire` | player | 1.14, 1.14, 1.14 / 44100 Hz / stereo | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Pistol, Akimbo Pistols |
| `guns.sawedoffFire` | player | 1.30, 1.30, 1.30 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Sawed-off |
| `guns.boltactionFire` | player | 1.98, 1.98, 1.98 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Bolt-action |
| `guns.combatshotgunFire` | player | 2.10, 2.32, 3.14 / 48000 Hz / stereo | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Combat Shotgun |
| `guns.lasergunFire` | player | 1.79, 1.74, 1.56 / 44100 Hz / stereo | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Laser Gun, Scatter Beam Gun |
| `guns.rocketFire` | player | 2.06, 2.06, 2.06 / 48000 Hz / stereo | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Rocket, Nuke, Grim Reaper |
| `guns.thompsonFire` | player | 1.01, 1.01, 1.01 / 192000 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Thompson |
| `guns.revolverFire` | player | 1.78, 1.78, 1.78 / 48000 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Revolver, Akimbo Revolvers |
| `guns.goldenrevolverFire` | player | 3.49, 3.02, 3.02 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Golden Revolver (+akimbo) |
| `guns.ak47Fire` | player | 1.50, 1.50, 1.50 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | AK-47 |
| `guns.as50Fire` | player | 4.93, 4.93 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | AS50 |
| `guns.teslaFire` | player | 3.44, 3.44, 3.44, 3.44 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Tesla Gun |
| `guns.pdwFire` | player | 1.26, 1.26, 1.26 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | PDW |
| `guns.lmgFire` | player | 1.98, 1.98, 1.98 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | LMG |
| `guns.silencedM4Fire` | player | 1.07, 1.07 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | M4 Infiltrator |
| `guns.pulserifleFire` | player | 2.75, 2.75, 2.75, 2.75 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Pulse Rifle |
| `guns.cyberdemonblasterFire` | player | 2.00, 2.00, 2.00, 2.00 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Cyberdemon Blaster |
| `guns.minigunFire` | player | 1.00, 1.00, 1.00, 1.00 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Minigun |
| `guns.alienblasterFire` | player | 2.65, 2.76, 2.80 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Alien Blaster |
| `guns.blasterrifleFire` | player | 0.83, 0.77, 0.77 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Blaster Rifle |
| `guns.grenadeLauncherFire` | player | 1.71, 1.68, 1.71 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Grenade Launcher |
| `guns.augFire` | player | 2.44, 2.44, 2.44 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | AUG |
| `guns.sonicshotgunFire` | player | 1.76, 1.76, 1.59 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Sonic Shotgun |
| `guns.mac10Fire` | player | 1.42, 1.42, 1.42 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Mac-10 (+akimbo) |
| `guns.scarFire` | player | 1.60, 1.60, 1.60 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | SCAR |
| `guns.vectorFire` | player | 0.60, 0.60, 0.60 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | Vector |
| `guns.MIBblasterFire` | player | 2.44, 2.44, 2.34 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot fired by a player (`shootGunLeftClick`, ammo > 0, fire delay elapsed) or by an NPC/turret (`fireWeaponFromNPC`) | MIB gun (+akimbo) |
| `guns.assaultRifleReload` | player | 2.09 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | M4, M4 Infiltrator |
| `guns.handgunReload` | player | 1.32 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Hand Cannon |
| `guns.pistolReload` | player | 1.81 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Pistol |
| `guns.pistolReloadAkimbo` | player | 2.60 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Akimbo Pistols |
| `guns.sawedoffReload` | player | 1.54 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Sawed-off |
| `guns.boltactionReload` | player | 1.94 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Bolt-action |
| `guns.flamethrowerReload` | player | 2.21 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Flamethrower |
| `guns.combatshotgunReload` | player | 2.95 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Combat Shotgun |
| `guns.lasergunReload` | player | 3.05 / 32000 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Laser Gun, Scatter Beam, Blaster Rifle, NDR |
| `guns.rocketReload` | player | 3.53 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Rocket, Nuke, Grim Reaper |
| `guns.thompsonReload` | player | 1.76 / 48000 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Thompson |
| `guns.revolverReload` | player | 2.72 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Revolver, Golden Revolver |
| `guns.revolverReloadAkimbo` | player | 3.51 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Akimbo Revolvers (both) |
| `guns.ak47Reload` | player | 2.78 / 48000 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | AK-47 |
| `guns.as50Reload` | player | 5.00 / 48000 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | AS50 |
| `guns.teslaReload` | player | 2.35 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Tesla Gun, Cyberdemon Blaster |
| `guns.pdwReload` | player | 2.57 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | PDW |
| `guns.lmgReload` | player | 5.72 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | LMG |
| `guns.pulserifleReload` | player | 2.22 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Pulse Rifle |
| `guns.minigunReload` | player | 6.24 / 32000 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Minigun |
| `guns.alienblasterReload` | player | 2.97 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Alien Blaster |
| `guns.grenadeLauncherReload` | player | 4.32 / 48000 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Grenade Launcher |
| `guns.augReload` | player | 4.03 / 48000 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | AUG |
| `guns.sonicshotgunReload` | player | 3.95 / 48000 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Sonic Shotgun |
| `guns.mac10Reload` | player | 2.64 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Mac-10 |
| `guns.mac10ReloadAkimbo` | player | 3.41 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Akimbo Mac-10 |
| `guns.scarReload` | player | 3.53 / 48000 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | SCAR |
| `guns.vectorReload` | player | 1.54 / 48000 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Vector |
| `guns.MIBblasterReload` | player | 2.29 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | MIB gun |
| `guns.MIBblasterReloadAkimbo` | player | 2.99 / 44100 Hz / mono | TGSound following the shooter (moving), RELOAD; repeat within 500 ms suppressed locally | 1.0 | 1.0 | reload starts: firing with an empty magazine while reserve ammo exists, or the reload key (`tryForcedReload`) with a non-full magazine and reserve ammo | Akimbo MIB |
| `guns.boltactionRechamber` | player | 1.03 / 44100 Hz / mono | TGSound at gun position, not moving, RELOAD | 1.0 | 1.0 | immediately with every shot (same tick as the fire sound) | Bolt-action |
| `guns.combatshotgunPump` | player | 0.89 / 44100 Hz / mono | TGSound at gun position, not moving, RELOAD | 1.0 | 1.0 | immediately with every shot (same tick as the fire sound) | Combat Shotgun |
| `guns.flamethrowerStart` | player | 1.00 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | first shot of a run: player loop timer (`loopSoundDelay`) ≤ 0 when firing | Flamethrower |
| `guns.flamethrowerFire` | player | 1.30, 1.30, 1.10, 1.10, 1.10 / 44100 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot after the first of a run, while the player loop timer (10 ticks, refreshed each shot) is still running; NPC shots always use this event | Flamethrower |
| `guns.chainsawLoopStart` | master | 0.86 / 32000 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | first shot of a run: player loop timer (`loopSoundDelay`) ≤ 0 when firing (right-click held) | Chainsaw |
| `guns.chainsawLoop` | player | 1.89, 1.89, 1.89 / 32000 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot after the first of a run, while the player loop timer (10 ticks, refreshed each shot) is still running; NPC shots always use this event | Chainsaw |
| `guns.beamgunStart` | player | 1.31, 1.31, 1.83 / 48000 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | first shot of a run: player loop timer (`loopSoundDelay`) ≤ 0 when firing | NDR |
| `guns.beamgunFire` | player | 1.01, 1.01, 1.01, 1.01, 1.01, 1.01 / 48000 Hz / mono | TGSound at gun position, not moving, GUN_FIRE | 4.0 | 1.0 | each shot after the first of a run, while the player loop timer (10 ticks, refreshed each shot) is still running; NPC shots always use this event | NDR |
| `guns.biogunFire` | player | 1.58, 1.58, 1.58 / 48000 Hz / mono | vanilla `World.playSoundAtEntity` on the player (server broadcast) | 1.0 | 1.0 | right-click released after charging (`onPlayerStoppedUsing`), server side only | Bio Gun |
| `guns.biogunReload` | player | 3.42 / 48000 Hz / mono | vanilla `World.playSoundAtEntity` (right-click path, [`GenericGunCharge`](../02-guns/per-gun-behaviour.md#genericguncharge-genericgunchargejava)); the reload key uses the TGSound reload path instead | 1.0 | 1.0 | right-click on an empty Bio Gun with reserve ammo | Bio Gun |
| `guns.powerhammerFire` | player | 3.19, 2.20, 2.58 / 48000 Hz / mono | vanilla `World.playSoundAtEntity` on the player (server broadcast) | 1.0 | 1.0 | right-click released after charging, server side only | Power Hammers |
| `guns.powerhammerReload` | player | 3.43 / 44100 Hz / mono | Power Hammers: vanilla `World.playSoundAtEntity` (right-click path) or TGSound reload (reload key). Chainsaw: TGSound reload path | 1.0 | 1.0 | reload start (see Common trigger rules) | Power Hammers, Chainsaw |
| `guns.powerhammerCharge` | player | 2.53, 2.53, 2.53 / 48000 Hz / mono | vanilla `World.playSoundAtEntity` on the player (server broadcast) | 1.0 | 1.0 | right-click with ammo and fire delay elapsed: charging begins (`startCharge`) | Power Hammers |
| `guns.powerhammerSwing` | player | 1.67, 1.39, 1.49 / 48000 Hz / mono | `PacketPlaySound` to all clients within 50 blocks including the swinger; TGSound at gun position, not moving, GUN_FIRE | 1.0 | 1.0 | left-click swing (`onEntitySwing`) with ammo > 0, server side, at most once per 5 ticks (`swingSoundDelay`), not while a non-player container is open | Power Hammers |
| `guns.powerhammerImpact` | player | 1.65, 1.14, 1.65, 1.14 / 32000 Hz / mono | vanilla `World.playSoundAtEntity` on the hammer projectile | 1.0 | 1.0 | PowerHammerProjectile impact on an entity | Power Hammers |
| `guns.powerhammerImpactGround` | player | 1.72, 1.55, 1.80, 1.72 / 32000 Hz / mono | vanilla: `playSoundAtEntity` on the projectile, or `playSoundEffect` at the block centre | 1.0 | 1.0 | PowerHammerProjectile impact without an entity hit; also `onBlockDestroyed` with ammo > 0 (block mined) | Power Hammers |
| `guns.chainsawSwing` | player | 1.28, 1.62, 1.10 / 44100 Hz / mono | `PacketPlaySound` to all clients within 50 blocks including the swinger; TGSound at gun position, not moving, GUN_FIRE | 2.0 | 1.0 | left-click swing with fuel, server side, at most once per 5 ticks, not while a non-player container is open | Chainsaw |
| `guns.chainsawHit` | player | 1.34, 1.34, 1.34, 1.34, 1.34, 1.34 / 44100 Hz / mono | vanilla `World.playSoundEffect` at the block centre | 0.65 | 1.0 | `onBlockDestroyed` with fuel > 0 (each block cut) | Chainsaw |
| `guns.chainsawLoopEnd` | player | 0.86 / 32000 Hz / mono | – | – | – | never played (no code reference) | – |
| `guns.minigunStart` | player | 0.30 / 44100 Hz / mono | – | – | – | never played (setter commented out in TGuns) | – |
| `guns.pulserifleFireStereo` | player | 2.75 / 44100 Hz / stereo | – | – | – | never played (no code reference) | – |
| `effects.biogunImpact` | player | 1.58, 1.58, 1.58 / 48000 Hz / mono | – | – | – | never played (the BiogunProjectile call is commented out) | – |
| `guns.grenade_pin` | player | 0.77, 0.77 / 48000 Hz / mono | vanilla `World.playSoundAtEntity` on the player (server broadcast) | 1.0 | 1.0 | right-click on a frag grenade (item use starts) | Frag Grenade |
| `guns.grenade_throw` | player | 0.79, 0.77, 0.88 / 48000 Hz / mono | vanilla `World.playSoundAtEntity` on the player (server broadcast) | 1.0 | 1.0 | frag grenade released (`spawnGrenade`, server side) | Frag Grenade |



## Other (vanilla) sounds played by weapons in this area

| Sound | Trigger | Volume | Pitch | Side |
|---|---|---|---|---|
| `random.bow` (vanilla) | Stielhandgranate released ([`GenericGrenade.spawnGrenade`](../02-guns/per-gun-behaviour.md#grenades-genericgrenade-fraggrenade-flashbang)) | 1.0 | 1 / (rand × 0.4 + 1.2) + charge × 0.5, where charge = (t² + 2t)/3 with t = ticks held ÷ 20, capped at 1 | server |
| `random.bow` (vanilla) | Flashbang throw: unregistered item, so unreachable | 1.0 | same formula | server |
| `mob.villager.idle` (vanilla) | firing or forced reload with an empty magazine and no reserve ammo | 1.0 | 1.0 | server |

## Measured file facts (all 185 files)

*Added in the consistency pass.* Duration, sample rate and channels are quoted from [ASSET-FACTS.md](../ASSET-FACTS.md). The volume and pitch come from the event table above. Totals: 185 files, 172 mono and 13 stereo (all 13 stereo files are referenced). 111 files are 44.1 kHz mono, 42 are 48 kHz mono, 16 are 32 kHz mono, 7 are 44.1 kHz stereo, 6 are 48 kHz stereo, and 3 are 192 kHz mono.

| File | Event [category] | Duration | Sample rate | Channels | Volume / pitch passed | Notes |
|---|---|---|---|---|---|---|
| `sounds/guns/MIB_blaster_reload.ogg` | guns.MIBblasterReload [player] | 2.29 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/MIB_blaster_reload_akimbo.ogg` | guns.MIBblasterReloadAkimbo [player] | 2.99 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/MIB_blaster_shot_a.ogg` | guns.MIBblasterFire [player] | 2.44 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/MIB_blaster_shot_b.ogg` | guns.MIBblasterFire [player] | 2.44 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/MIB_blaster_shot_c.ogg` | guns.MIBblasterFire [player] | 2.34 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/SCAR_reload.ogg` | guns.scarReload [player] | 3.53 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/SCAR_shot_a.ogg` | guns.scarFire [player] | 1.60 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/SCAR_shot_b.ogg` | guns.scarFire [player] | 1.60 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/SCAR_shot_c.ogg` | guns.scarFire [player] | 1.60 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/ak47_reload.ogg` | guns.ak47Reload [player] | 2.78 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/ak47_shot_a.ogg` | guns.ak47Fire [player] | 1.50 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/ak47_shot_b.ogg` | guns.ak47Fire [player] | 1.50 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/ak47_shot_c.ogg` | guns.ak47Fire [player] | 1.50 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/alienblaster_reload.ogg` | guns.alienblasterReload [player] | 2.97 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/alienblaster_shot_a.ogg` | guns.alienblasterFire [player] | 2.65 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/alienblaster_shot_b.ogg` | guns.alienblasterFire [player] | 2.76 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/alienblaster_shot_c.ogg` | guns.alienblasterFire [player] | 2.80 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/ar1_reload.ogg` | guns.assaultRifleReload [player] | 2.09 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/ar1_shot_a.ogg` | guns.assaultRifleFire [player] | 1.23 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/ar1_shot_b.ogg` | guns.assaultRifleFire [player] | 1.40 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/as50_reload.ogg` | guns.as50Reload [player] | 5.00 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/as50_shot_a.ogg` | guns.as50Fire [player] | 4.93 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/as50_shot_b.ogg` | guns.as50Fire [player] | 4.93 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/aug_reload.ogg` | guns.augReload [player] | 4.03 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/aug_shot_a.ogg` | guns.augFire [player] | 2.44 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/aug_shot_b.ogg` | guns.augFire [player] | 2.44 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/aug_shot_c.ogg` | guns.augFire [player] | 2.44 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/beamgun_loop_a.ogg` | guns.beamgunFire [player] | 1.01 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/beamgun_loop_b.ogg` | guns.beamgunFire [player] | 1.01 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/beamgun_loop_c.ogg` | guns.beamgunFire [player] | 1.01 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/beamgun_loop_d.ogg` | guns.beamgunFire [player] | 1.01 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/beamgun_loop_e.ogg` | guns.beamgunFire [player] | 1.01 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/beamgun_loop_f.ogg` | guns.beamgunFire [player] | 1.01 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/beamgun_reload.ogg` | (no sounds.json event) | 2.99 s | 48000 Hz | mono | never played | not in sounds.json (unreachable) |
| `sounds/guns/beamgun_start_a.ogg` | guns.beamgunStart [player] | 1.31 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/beamgun_start_b.ogg` | guns.beamgunStart [player] | 1.31 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/beamgun_start_c.ogg` | guns.beamgunStart [player] | 1.83 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/biogun_impact_a.ogg` | effects.biogunImpact [player] | 1.58 s | 48000 Hz | mono | never played | event unused |
| `sounds/guns/biogun_impact_b.ogg` | effects.biogunImpact [player] | 1.58 s | 48000 Hz | mono | never played | event unused |
| `sounds/guns/biogun_impact_c.ogg` | effects.biogunImpact [player] | 1.58 s | 48000 Hz | mono | never played | event unused |
| `sounds/guns/biogun_reload.ogg` | guns.biogunReload [player] | 3.42 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/biogun_shot_a.ogg` | guns.biogunFire [player] | 1.58 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/biogun_shot_b.ogg` | guns.biogunFire [player] | 1.58 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/biogun_shot_c.ogg` | guns.biogunFire [player] | 1.58 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/blasterrifle_shot_a.ogg` | guns.blasterrifleFire [player] | 0.83 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/blasterrifle_shot_b.ogg` | guns.blasterrifleFire [player] | 0.77 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/blasterrifle_shot_c.ogg` | guns.blasterrifleFire [player] | 0.77 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/boltaction_rechamber.ogg` | guns.boltactionRechamber [player] | 1.03 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/boltaction_reload.ogg` | guns.boltactionReload [player] | 1.94 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/boltaction_shot_a.ogg` | guns.boltactionFire [player] | 1.98 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/boltaction_shot_b.ogg` | guns.boltactionFire [player] | 1.98 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/boltaction_shot_c.ogg` | guns.boltactionFire [player] | 1.98 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/cdb_shot_a.ogg` | guns.cyberdemonblasterFire [player] | 2.00 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/cdb_shot_b.ogg` | guns.cyberdemonblasterFire [player] | 2.00 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/cdb_shot_c.ogg` | guns.cyberdemonblasterFire [player] | 2.00 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/cdb_shot_d.ogg` | guns.cyberdemonblasterFire [player] | 2.00 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/chainsaw_hit_a.ogg` | guns.chainsawHit [player] | 1.34 s | 44100 Hz | mono | 0.65 / 1.0 | byte-identical to the other chainsaw_hit files |
| `sounds/guns/chainsaw_hit_b.ogg` | guns.chainsawHit [player] | 1.34 s | 44100 Hz | mono | 0.65 / 1.0 | byte-identical to the other chainsaw_hit files |
| `sounds/guns/chainsaw_hit_c.ogg` | guns.chainsawHit [player] | 1.34 s | 44100 Hz | mono | 0.65 / 1.0 | byte-identical to the other chainsaw_hit files |
| `sounds/guns/chainsaw_hit_d.ogg` | guns.chainsawHit [player] | 1.34 s | 44100 Hz | mono | 0.65 / 1.0 | byte-identical to the other chainsaw_hit files |
| `sounds/guns/chainsaw_hit_e.ogg` | guns.chainsawHit [player] | 1.34 s | 44100 Hz | mono | 0.65 / 1.0 | byte-identical to the other chainsaw_hit files |
| `sounds/guns/chainsaw_hit_f.ogg` | guns.chainsawHit [player] | 1.34 s | 44100 Hz | mono | 0.65 / 1.0 | byte-identical to the other chainsaw_hit files |
| `sounds/guns/chainsaw_loop_a.ogg` | guns.chainsawLoop [player] | 1.89 s | 32000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/chainsaw_loop_b.ogg` | guns.chainsawLoop [player] | 1.89 s | 32000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/chainsaw_loop_c.ogg` | guns.chainsawLoop [player] | 1.89 s | 32000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/chainsaw_loop_d.ogg` | (no sounds.json event) | 0.86 s | 32000 Hz | mono | never played | not in sounds.json (unreachable); byte-identical to chainsaw_loopstart/loopend/loop_d |
| `sounds/guns/chainsaw_loopend.ogg` | guns.chainsawLoopEnd [player] | 0.86 s | 32000 Hz | mono | never played | event unused; byte-identical to chainsaw_loopstart/loopend/loop_d |
| `sounds/guns/chainsaw_loopstart.ogg` | guns.chainsawLoopStart [master] | 0.86 s | 32000 Hz | mono | 4.0 / 1.0 | byte-identical to chainsaw_loopstart/loopend/loop_d |
| `sounds/guns/chainsaw_swing_a.ogg` | guns.chainsawSwing [player] | 1.28 s | 44100 Hz | mono | 2.0 / 1.0 | – |
| `sounds/guns/chainsaw_swing_b.ogg` | guns.chainsawSwing [player] | 1.62 s | 44100 Hz | mono | 2.0 / 1.0 | – |
| `sounds/guns/chainsaw_swing_c.ogg` | guns.chainsawSwing [player] | 1.10 s | 44100 Hz | mono | 2.0 / 1.0 | – |
| `sounds/guns/combatshotgun_pump.ogg` | guns.combatshotgunPump [player] | 0.89 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/combatshotgun_reload.ogg` | guns.combatshotgunReload [player] | 2.95 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/combatshotgun_shot_a.ogg` | guns.combatshotgunFire [player] | 2.10 s | 48000 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/combatshotgun_shot_b.ogg` | guns.combatshotgunFire [player] | 2.32 s | 48000 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/combatshotgun_shot_c.ogg` | guns.combatshotgunFire [player] | 3.14 s | 48000 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/flamethrower_reload.ogg` | guns.flamethrowerReload [player] | 2.21 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/flamethrower_shot_a.ogg` | guns.flamethrowerFire [player] | 1.30 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/flamethrower_shot_b.ogg` | guns.flamethrowerFire [player] | 1.30 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/flamethrower_shot_c.ogg` | guns.flamethrowerFire [player] | 1.10 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/flamethrower_shot_d.ogg` | guns.flamethrowerFire [player] | 1.10 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/flamethrower_shot_e.ogg` | guns.flamethrowerFire [player] | 1.10 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/flamethrower_start_a.ogg` | guns.flamethrowerStart [player] | 1.00 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/goldenrevolver_shot_a.ogg` | guns.goldenrevolverFire [player] | 3.49 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/goldenrevolver_shot_b.ogg` | guns.goldenrevolverFire [player] | 3.02 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/goldenrevolver_shot_c.ogg` | guns.goldenrevolverFire [player] | 3.02 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/grenade_pin_a.ogg` | guns.grenade_pin [player] | 0.77 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/grenade_pin_b.ogg` | guns.grenade_pin [player] | 0.77 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/grenade_throw_a.ogg` | guns.grenade_throw [player] | 0.79 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/grenade_throw_b.ogg` | guns.grenade_throw [player] | 0.77 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/grenade_throw_c.ogg` | guns.grenade_throw [player] | 0.88 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/grenadelauncher_reload.ogg` | guns.grenadeLauncherReload [player] | 4.32 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/grenadelauncher_shot_a.ogg` | guns.grenadeLauncherFire [player] | 1.71 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/grenadelauncher_shot_b.ogg` | guns.grenadeLauncherFire [player] | 1.68 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/grenadelauncher_shot_c.ogg` | guns.grenadeLauncherFire [player] | 1.71 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/handgun_reload.ogg` | guns.handgunReload [player] | 1.32 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/handgun_shot_a.ogg` | guns.handgunFire [player] | 1.71 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/handgun_shot_b.ogg` | guns.handgunFire [player] | 2.01 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/handgun_shot_c.ogg` | guns.handgunFire [player] | 2.29 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/lasergun_reload.ogg` | guns.lasergunReload [player] | 3.05 s | 32000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/lasergun_shot_a.ogg` | guns.lasergunFire [player] | 1.79 s | 44100 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/lasergun_shot_b.ogg` | guns.lasergunFire [player] | 1.74 s | 44100 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/lasergun_shot_c.ogg` | guns.lasergunFire [player] | 1.56 s | 44100 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/lmg_reload.ogg` | guns.lmgReload [player] | 5.72 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/lmg_shot_a.ogg` | guns.lmgFire [player] | 1.98 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/lmg_shot_b.ogg` | guns.lmgFire [player] | 1.98 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/lmg_shot_c.ogg` | guns.lmgFire [player] | 1.98 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/mac10_reload.ogg` | guns.mac10Reload [player] | 2.64 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/mac10_reload_akimbo.ogg` | guns.mac10ReloadAkimbo [player] | 3.41 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/mac10_shot_a.ogg` | guns.mac10Fire [player] | 1.42 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/mac10_shot_b.ogg` | guns.mac10Fire [player] | 1.42 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/mac10_shot_c.ogg` | guns.mac10Fire [player] | 1.42 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/minigun_reload.ogg` | guns.minigunReload [player] | 6.24 s | 32000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/minigun_shot_a.ogg` | guns.minigunFire [player] | 1.00 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/minigun_shot_b.ogg` | guns.minigunFire [player] | 1.00 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/minigun_shot_c.ogg` | guns.minigunFire [player] | 1.00 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/minigun_shot_d.ogg` | guns.minigunFire [player] | 1.00 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/minigun_start_a.ogg` | guns.minigunStart [player] | 0.30 s | 44100 Hz | mono | never played | event unused |
| `sounds/guns/minigun_start_b.ogg` | (no sounds.json event) | 0.30 s | 44100 Hz | mono | never played | not in sounds.json (unreachable) |
| `sounds/guns/minigun_start_c.ogg` | (no sounds.json event) | 0.30 s | 44100 Hz | mono | never played | not in sounds.json (unreachable) |
| `sounds/guns/pdw_reload.ogg` | guns.pdwReload [player] | 2.57 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/pdw_shot_a.ogg` | guns.pdwFire [player] | 1.26 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/pdw_shot_b.ogg` | guns.pdwFire [player] | 1.26 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/pdw_shot_c.ogg` | guns.pdwFire [player] | 1.26 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/pistol_reload.ogg` | guns.pistolReload [player] | 1.81 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/pistol_reload_akimbo.ogg` | guns.pistolReloadAkimbo [player] | 2.60 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/pistol_shot_a.ogg` | guns.pistolFire [player] | 1.14 s | 44100 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/pistol_shot_b.ogg` | guns.pistolFire [player] | 1.14 s | 44100 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/pistol_shot_c.ogg` | guns.pistolFire [player] | 1.14 s | 44100 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/powerhammer_charge_a.ogg` | guns.powerhammerCharge [player] | 2.53 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_charge_b.ogg` | guns.powerhammerCharge [player] | 2.53 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_charge_c.ogg` | guns.powerhammerCharge [player] | 2.53 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_impactGround_a.ogg` | guns.powerhammerImpactGround [player] | 1.72 s | 32000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_impactGround_b.ogg` | guns.powerhammerImpactGround [player] | 1.55 s | 32000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_impactGround_c.ogg` | guns.powerhammerImpactGround [player] | 1.80 s | 32000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_impactGround_d.ogg` | guns.powerhammerImpactGround [player] | 1.72 s | 32000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_impact_a.ogg` | guns.powerhammerImpact [player] | 1.65 s | 32000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_impact_b.ogg` | guns.powerhammerImpact [player] | 1.14 s | 32000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_impact_c.ogg` | guns.powerhammerImpact [player] | 1.65 s | 32000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_impact_d.ogg` | guns.powerhammerImpact [player] | 1.14 s | 32000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_reload.ogg` | guns.powerhammerReload [player] | 3.43 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_shot_a.ogg` | guns.powerhammerFire [player] | 3.19 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_shot_b.ogg` | guns.powerhammerFire [player] | 2.20 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_shot_c.ogg` | guns.powerhammerFire [player] | 2.58 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_swing_a.ogg` | guns.powerhammerSwing [player] | 1.67 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_swing_b.ogg` | guns.powerhammerSwing [player] | 1.39 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/powerhammer_swing_c.ogg` | guns.powerhammerSwing [player] | 1.49 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/pulserifle_reload.ogg` | guns.pulserifleReload [player] | 2.22 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/pulserifle_shot_a.ogg` | guns.pulserifleFire [player] | 2.75 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/pulserifle_shot_b.ogg` | guns.pulserifleFire [player] | 2.75 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/pulserifle_shot_c.ogg` | guns.pulserifleFire [player] | 2.75 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/pulserifle_shot_d.ogg` | guns.pulserifleFire [player] | 2.75 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/pulserifle_shot_stereo.ogg` | guns.pulserifleFireStereo [player] | 2.75 s | 44100 Hz | stereo | never played | event unused; stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/revolver_reload.ogg` | guns.revolverReload [player] | 2.72 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/revolver_reload_akimbo.ogg` | guns.revolverReloadAkimbo [player] | 3.51 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/revolver_shot_a.ogg` | guns.revolverFire [player] | 1.78 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/revolver_shot_b.ogg` | guns.revolverFire [player] | 1.78 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/revolver_shot_c.ogg` | guns.revolverFire [player] | 1.78 s | 48000 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/rocket_reload.ogg` | guns.rocketReload [player] | 3.53 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/rocket_shot_a.ogg` | guns.rocketFire [player] | 2.06 s | 48000 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/rocket_shot_b.ogg` | guns.rocketFire [player] | 2.06 s | 48000 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/rocket_shot_c.ogg` | guns.rocketFire [player] | 2.06 s | 48000 Hz | stereo | 4.0 / 1.0 | stereo: plays without 3D attenuation (`GVA-23`) |
| `sounds/guns/sawedoff_reload.ogg` | guns.sawedoffReload [player] | 1.54 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/sawedoff_shot_a.ogg` | guns.sawedoffFire [player] | 1.30 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/sawedoff_shot_b.ogg` | guns.sawedoffFire [player] | 1.30 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/sawedoff_shot_c.ogg` | guns.sawedoffFire [player] | 1.30 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/silencedm4_shot_a.ogg` | guns.silencedM4Fire [player] | 1.07 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/silencedm4_shot_b.ogg` | guns.silencedM4Fire [player] | 1.07 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/sonicshotgun_reload.ogg` | guns.sonicshotgunReload [player] | 3.95 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/sonicshotgun_shot_a.ogg` | guns.sonicshotgunFire [player] | 1.76 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/sonicshotgun_shot_b.ogg` | guns.sonicshotgunFire [player] | 1.76 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/sonicshotgun_shot_c.ogg` | guns.sonicshotgunFire [player] | 1.59 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/teslagun_reload.ogg` | guns.teslaReload [player] | 2.35 s | 44100 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/teslagun_shot_a.ogg` | guns.teslaFire [player] | 3.44 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/teslagun_shot_b.ogg` | guns.teslaFire [player] | 3.44 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/teslagun_shot_c.ogg` | guns.teslaFire [player] | 3.44 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/teslagun_shot_d.ogg` | guns.teslaFire [player] | 3.44 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/thompson_reload.ogg` | guns.thompsonReload [player] | 1.76 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/thompson_shot_a.ogg` | guns.thompsonFire [player] | 1.01 s | 192000 Hz | mono | 4.0 / 1.0 | unusual 192 kHz rate (`GVA-24`) |
| `sounds/guns/thompson_shot_b.ogg` | guns.thompsonFire [player] | 1.01 s | 192000 Hz | mono | 4.0 / 1.0 | unusual 192 kHz rate (`GVA-24`) |
| `sounds/guns/thompson_shot_c.ogg` | guns.thompsonFire [player] | 1.01 s | 192000 Hz | mono | 4.0 / 1.0 | unusual 192 kHz rate (`GVA-24`) |
| `sounds/guns/vector_reload.ogg` | guns.vectorReload [player] | 1.54 s | 48000 Hz | mono | 1.0 / 1.0 | – |
| `sounds/guns/vector_shot_a.ogg` | guns.vectorFire [player] | 0.60 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/vector_shot_b.ogg` | guns.vectorFire [player] | 0.60 s | 44100 Hz | mono | 4.0 / 1.0 | – |
| `sounds/guns/vector_shot_c.ogg` | guns.vectorFire [player] | 0.60 s | 44100 Hz | mono | 4.0 / 1.0 | – |

Rows: 185
