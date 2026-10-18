# Sound catalog: `sounds/guns/`

All 185 `.ogg` files in `assets/techguns/sounds/guns/`, the `sounds.json` events that reference them, and exactly when each event plays. Playback mechanics (TGSound, packets, gun position, volume categories) are in [rendering-framework.md §14](rendering-framework.md#14-sound-playback-mechanics-summary). Event names are used with the `techguns:` prefix in code, e.g. `techguns:guns.ak47Fire`.

When an event lists several files, Minecraft picks one at random each time it plays (vanilla `sounds.json` behavior). Every gun event uses `"category": "player"` except `guns.chainsawLoopStart`, which uses `"master"`. The sound-system category is distinct from Techguns' own volume-multiplier category (GUN_FIRE / RELOAD / …, framework §14). That multiplier is an identity function in v0.1.2 (correction made in the consistency pass).

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
| `powerhammer_charge_*`, `_shot_*`, `_swing_*`, `_impact_*`, `_impactGround_*` | pneumatic wind-up, piston release, whoosh, flesh/armor hit, ground or rock thud |
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

No call randomizes pitch. Every Techguns gun sound plays at pitch 1.0. The only randomized pitch belongs to the Stielhandgranate's vanilla `random.bow`, see the next section.

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
| `guns.biogunReload` | player | 3.42 / 48000 Hz / mono | vanilla `World.playSoundAtEntity` (right-click path, [`GenericGunCharge`](../02-guns/per-gun-behavior.md#genericguncharge-genericgunchargejava)); the reload key uses the TGSound reload path instead | 1.0 | 1.0 | right-click on an empty Bio Gun with reserve ammo | Bio Gun |
| `guns.powerhammerFire` | player | 3.19, 2.20, 2.58 / 48000 Hz / mono | vanilla `World.playSoundAtEntity` on the player (server broadcast) | 1.0 | 1.0 | right-click released after charging, server side only | Power Hammers |
| `guns.powerhammerReload` | player | 3.43 / 44100 Hz / mono | Power Hammers: vanilla `World.playSoundAtEntity` (right-click path) or TGSound reload (reload key). Chainsaw: TGSound reload path | 1.0 | 1.0 | reload start (see Common trigger rules) | Power Hammers, Chainsaw |
| `guns.powerhammerCharge` | player | 2.53, 2.53, 2.53 / 48000 Hz / mono | vanilla `World.playSoundAtEntity` on the player (server broadcast) | 1.0 | 1.0 | right-click with ammo and fire delay elapsed: charging begins (`startCharge`) | Power Hammers |
| `guns.powerhammerSwing` | player | 1.67, 1.39, 1.49 / 48000 Hz / mono | `PacketPlaySound` to all clients within 50 blocks including the swinger; TGSound at gun position, not moving, GUN_FIRE | 1.0 | 1.0 | left-click swing (`onEntitySwing`) with ammo > 0, server side, at most once per 5 ticks (`swingSoundDelay`), not while a non-player container is open | Power Hammers |
| `guns.powerhammerImpact` | player | 1.65, 1.14, 1.65, 1.14 / 32000 Hz / mono | vanilla `World.playSoundAtEntity` on the hammer projectile | 1.0 | 1.0 | PowerHammerProjectile impact on an entity | Power Hammers |
| `guns.powerhammerImpactGround` | player | 1.72, 1.55, 1.80, 1.72 / 32000 Hz / mono | vanilla: `playSoundAtEntity` on the projectile, or `playSoundEffect` at the block center | 1.0 | 1.0 | PowerHammerProjectile impact without an entity hit; also `onBlockDestroyed` with ammo > 0 (block mined) | Power Hammers |
| `guns.chainsawSwing` | player | 1.28, 1.62, 1.10 / 44100 Hz / mono | `PacketPlaySound` to all clients within 50 blocks including the swinger; TGSound at gun position, not moving, GUN_FIRE | 2.0 | 1.0 | left-click swing with fuel, server side, at most once per 5 ticks, not while a non-player container is open | Chainsaw |
| `guns.chainsawHit` | player | 1.34, 1.34, 1.34, 1.34, 1.34, 1.34 / 44100 Hz / mono | vanilla `World.playSoundEffect` at the block center | 0.65 | 1.0 | `onBlockDestroyed` with fuel > 0 (each block cut) | Chainsaw |
| `guns.chainsawLoopEnd` | player | 0.86 / 32000 Hz / mono | – | – | – | never played (no code reference) | – |
| `guns.minigunStart` | player | 0.30 / 44100 Hz / mono | – | – | – | never played (setter commented out in TGuns) | – |
| `guns.pulserifleFireStereo` | player | 2.75 / 44100 Hz / stereo | – | – | – | never played (no code reference) | – |
| `effects.biogunImpact` | player | 1.58, 1.58, 1.58 / 48000 Hz / mono | – | – | – | never played (the BiogunProjectile call is commented out) | – |
| `guns.grenade_pin` | player | 0.77, 0.77 / 48000 Hz / mono | vanilla `World.playSoundAtEntity` on the player (server broadcast) | 1.0 | 1.0 | right-click on a frag grenade (item use starts) | Frag Grenade |
| `guns.grenade_throw` | player | 0.79, 0.77, 0.88 / 48000 Hz / mono | vanilla `World.playSoundAtEntity` on the player (server broadcast) | 1.0 | 1.0 | frag grenade released (`spawnGrenade`, server side) | Frag Grenade |



## Other (vanilla) sounds played by weapons in this area

| Sound | Trigger | Volume | Pitch | Side |
|---|---|---|---|---|
| `random.bow` (vanilla) | Stielhandgranate released ([`GenericGrenade.spawnGrenade`](../02-guns/per-gun-behavior.md#grenades-genericgrenade-fraggrenade-flashbang)) | 1.0 | 1 / (rand × 0.4 + 1.2) + charge × 0.5, where charge = (t² + 2t)/3 with t = ticks held ÷ 20, capped at 1 | server |
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

<!-- audio-profiles:start -->
## How each sound sounds

Generated from [AUDIO-PROFILES.md](../AUDIO-PROFILES.md), which has full profiles with elements, envelope and recreation notes. Claude described these from spectrograms and measurements, so treat the prose as an informed reading.

| Sound | Sounds like | Character | Loudness (LUFS / peak dBFS) | Confidence |
|---|---|---|---|---|
| [`sounds/guns/ak47_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsak47_reloadogg) | A mechanical AK-47 reload made of clicky metallic handling sounds: magazine release and rattle, a heavy thunk around 1.1s, a pause, then a second cluster of clacks ending in a loud bolt/charging slap near 1.87s with a ringing tail. | transient, metallic, mechanical, clicky | -21.0 / -2.8 | medium |
| [`sounds/guns/ak47_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsak47_shot_aogg) | A single heavy, low-weighted rifle shot with a sharp instant attack, a boomy noisy body and a long reverberant decay tail fading by about 1.2 s. | transient, boomy, noisy, gunshot | -18.4 / -2.8 | medium |
| [`sounds/guns/ak47_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsak47_shot_bogg) | A single close, bass-heavy rifle shot with a sharp noisy crack that decays into a rumbling, reverberant tail over roughly one second. | transient, noisy, boomy, gunshot | -18.6 / -3.2 | high |
| [`sounds/guns/ak47_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsak47_shot_cogg) | A single heavy rifle shot with a loud, bass-heavy noisy crack that decays into a short reverberant tail over about 1 second. | gunshot, transient, noisy, boomy | -19.4 / -3.7 | medium |
| [`sounds/guns/alienblaster_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsalienblaster_reloadogg) | A roughly 3-second sci-fi weapon reload: low rumbling mechanical handling with a few sharp clicks early on, a louder clack near 1.4s, then a fading mid-range electronic whirr or charge-up and a quiet tail. | mechanical, electronic, rumbly, clicky | -25.4 / -4.5 | medium |
| [`sounds/guns/alienblaster_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsalienblaster_shot_aogg) | A heavy, noisy sci-fi blaster shot with a strong low-end thump and a quick downward zap sweep on top, followed by a long, rumbling reverb-like tail. | blast, noisy, boomy, electronic | -20.9 / -4.1 | medium |
| [`sounds/guns/alienblaster_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsalienblaster_shot_bogg) | A heavy, boomy sci-fi blaster shot with a fast descending laser-like zap at the start, a thick low-frequency body and a long noisy, reverb-like tail fading over about 2 seconds. | boomy, noisy, electronic, sci-fi | -21.5 / -4.4 | medium |
| [`sounds/guns/alienblaster_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsalienblaster_shot_cogg) | A heavy, boomy sci-fi gun blast with a bright descending zap at the start and a long, noisy, reverberant decay. | boomy, noisy, electronic, transient | -19.9 / -2.4 | medium |
| [`sounds/guns/ar1_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsar1_reloadogg) | A mono assault rifle reload made of several separate mechanical handling sounds (clicks, a rattly slide, a thunk and a snap) with gaps of silence between them. | mechanical, metallic, transient, noisy | -22.3 / -3.0 | medium |
| [`sounds/guns/ar1_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsar1_shot_aogg) | A single heavy, noisy assault-rifle shot with a loud broadband crack and thick low-end punch, followed by a long, dense, fading tail of rumble and reverb. | gunshot, transient, noisy, boomy | -17.2 / -0.9 | medium |
| [`sounds/guns/ar1_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsar1_shot_bogg) | A heavy, noisy gunshot with a thick low-end thump and a bright crack that decays into a long, rumbling, reverb-like tail of roughly 1.2 seconds. | gunshot, noisy, boomy, broadband | -17.5 / -2.7 | medium |
| [`sounds/guns/as50_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsas50_reloadogg) | A roughly 5-second rifle reload made of quiet handling rustles and clicks, then a heavy mechanical clack around 1.2-1.6s, a bolt or magazine thunk near 2.8-3.1s, a faint tone, and two big metallic slams at 4.2s and 4.6s. | mechanical, transient, metallic, noisy | -19.5 / -3.4 | medium |
| [`sounds/guns/as50_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsas50_shot_aogg) | A heavy, boomy anti-materiel rifle gunshot with a deep low-end blast that decays into a long, rumbling reverb tail over about 3 seconds. | boomy, explosive, noisy, low-end | -19.1 / -3.9 | medium |
| [`sounds/guns/as50_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsas50_shot_bogg) | A heavy, boomy anti-materiel rifle shot with a sharp crack and huge low-end thump, followed by a long, dark, decaying reverberant tail. | boomy, explosive, noisy, transient | -19.2 / -2.7 | high |
| [`sounds/guns/aug_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsaug_reloadogg) | A sparse, multi-stage gun reload made of discrete mechanical clicks, rattles and clacks, with the loudest handling bursts near 2.0 s and 2.6-2.9 s, separated by near-silence. | transient, mechanical, metallic, noisy | -21.9 / -3.6 | medium |
| [`sounds/guns/aug_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsaug_shot_aogg) | A single punchy rifle shot with a heavy low-end thump and noisy crack, followed by a long, smooth reverb-like tail that fades out over about 2 seconds. | transient, noisy, boomy, percussive | -22.3 / -3.5 | high |
| [`sounds/guns/aug_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsaug_shot_bogg) | A single punchy rifle gunshot with a sharp noisy crack and heavy low-end thump, followed by a long decaying tail of low rumble and reverb that fades out over about 1.2 seconds. | transient, gunshot, noisy, boomy | -22.8 / -2.9 | high |
| [`sounds/guns/aug_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsaug_shot_cogg) | A single punchy rifle shot with a heavy low-end thump and bright noisy crack at the start, followed by a long smooth reverberant decay. | transient, noisy, boomy, gunshot | -22.9 / -3.3 | high |
| [`sounds/guns/beamgun_loop_a.ogg`](../AUDIO-PROFILES.md#soundsgunsbeamgun_loop_aogg) | A dense, low-frequency rumbling roar with a faint hiss on top, fading in and out over one second like a continuous energy-beam hum. | rumble, boomy, noisy, sustained | -12.0 / -0.5 | medium |
| [`sounds/guns/beamgun_loop_b.ogg`](../AUDIO-PROFILES.md#soundsgunsbeamgun_loop_bogg) | A one-second low-frequency rumbling buzz with a rough, pulsing texture that fades in and out, like a deep energy-beam hum. | rumble, sub-heavy, buzzy, pulsing | -11.4 / -0.7 | medium |
| [`sounds/guns/beamgun_loop_c.ogg`](../AUDIO-PROFILES.md#soundsgunsbeamgun_loop_cogg) | A dense, low-frequency rumbling buzz that swells in over about 0.3 s, holds a steady churning drone, then fades out by the end of its 1-second length. | rumble, noisy, boomy, mechanical | -11.0 / -0.6 | medium |
| [`sounds/guns/beamgun_loop_d.ogg`](../AUDIO-PROFILES.md#soundsgunsbeamgun_loop_dogg) | A low, rumbling, buzzy sustained roar that swells in over about half a second and fades out again, like a continuous energy-beam hum with a gritty, pulsing texture. | rumble, low, sustained, buzzy | -11.3 / -0.7 | medium |
| [`sounds/guns/beamgun_loop_e.ogg`](../AUDIO-PROFILES.md#soundsgunsbeamgun_loop_eogg) | A dense, low-frequency rumbling buzz that swells in over the first third, holds a rough, pulsing texture, then fades out by the end of a one-second clip, like a beam weapon's energy hum. | rumble, low-end, buzzy, pulsing | -11.8 / -0.9 | medium |
| [`sounds/guns/beamgun_loop_f.ogg`](../AUDIO-PROFILES.md#soundsgunsbeamgun_loop_fogg) | A low, rumbling, buzzy energy-beam hum that swells in over the first tenth of a second, sustains as a pulsing growl, and fades out at the end of a one-second loop clip. | rumble, low-end, noisy, pulsing | -12.2 / -0.8 | medium |
| [`sounds/guns/beamgun_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsbeamgun_reloadogg) | A two-part noisy, mechanical reload sequence: a crunchy clatter burst in the first half, then a heavier thud-and-rattle burst with a lingering gritty tail in the second half. | mechanical, noisy, transient, clattery | -18.9 / -1.4 | medium |
| [`sounds/guns/beamgun_start_a.ogg`](../AUDIO-PROFILES.md#soundsgunsbeamgun_start_aogg) | A low, rumbling, gritty roar that swells in quickly, holds with a choppy, pulsing texture and fades out over about 1.3 seconds, like a heavy energy weapon spooling up. | rumble, boomy, noisy, gritty | -14.3 / -0.7 | medium |
| [`sounds/guns/beamgun_start_b.ogg`](../AUDIO-PROFILES.md#soundsgunsbeamgun_start_bogg) | A roaring, rumbling low-frequency energy burst that swells quickly, holds a turbulent churn for about 0.7 s, then fades out, like a beam weapon powering up. | rumbling, noisy, boomy, turbulent | -13.0 / -0.8 | medium |
| [`sounds/guns/beamgun_start_c.ogg`](../AUDIO-PROFILES.md#soundsgunsbeamgun_start_cogg) | A loud, rumbling low-frequency burst of gritty noise that surges for about a second and then fades into a long, dark tail, like a heavy energy weapon spinning up. | boomy, rumbling, gritty, noisy | -13.8 / -0.7 | medium |
| [`sounds/guns/biogun_impact_a.ogg`](../AUDIO-PROFILES.md#soundsgunsbiogun_impact_aogg) | A low, rumbling, wet-sounding burst of noisy impact that starts hard, churns with pulsing bass for about a second, then thins out into a faint tail. | rumbling, noisy, boomy, wet | -19.6 / -3.2 | medium |
| [`sounds/guns/biogun_impact_b.ogg`](../AUDIO-PROFILES.md#soundsgunsbiogun_impact_bogg) | A low, rumbling, wet-sounding noisy burst with crackly mid texture that swells quickly, then pulses and fades over about 1.4 seconds, like a gooey bio-weapon impact. | noisy, boomy, rumble, crackle | -20.4 / -4.8 | medium |
| [`sounds/guns/biogun_impact_c.ogg`](../AUDIO-PROFILES.md#soundsgunsbiogun_impact_cogg) | A low, rumbling, noisy wet-sounding burst that hits almost immediately, stays dense and churning for about 0.7 s, then breaks into pulsing lumps and fades out by about 1.4 s. | boomy, rumbling, noisy, bassy | -20.1 / -4.6 | medium |
| [`sounds/guns/biogun_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsbiogun_reloadogg) | A multi-stage mechanical reload sequence of small clicks and rattles, a noisy mid-heavy slide or squelch around 0.55s, then a cluster of sharp clacks and a final heavy thunk with a fading rattly tail. | transient, mechanical, clicky, noisy | -25.8 / -4.0 | medium |
| [`sounds/guns/biogun_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsbiogun_shot_aogg) | A heavy, noisy, bass-rich energy-weapon blast with a bright crackly onset that sustains as a rumbling roar for about a second, then decays into a short low tail. | noisy, boomy, rumbling, sci-fi | -17.0 / -3.2 | medium |
| [`sounds/guns/biogun_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsbiogun_shot_bogg) | A heavy, noisy sci-fi gun blast with a strong sub and bass thump and a hissy crackling top end that sustains for about a second, then fades into a short rumbling tail. | noisy, boomy, blast, electronic | -17.7 / -3.5 | medium |
| [`sounds/guns/biogun_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsbiogun_shot_cogg) | A heavy, rumbling, noisy bio-weapon blast with a strong low-end thump that sustains for about a second, then sputters and fades into a short wobbly tail. | boomy, noisy, rumbling, gritty | -18.4 / -2.7 | medium |
| [`sounds/guns/blasterrifle_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsblasterrifle_shot_aogg) | A punchy, noisy sci-fi blaster rifle shot with a heavy low-end thump and a fast downward laser-like zap sweep, decaying into a short fading tail. | blaster, noisy, electronic, boomy | -18.7 / -3.8 | medium |
| [`sounds/guns/blasterrifle_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsblasterrifle_shot_bogg) | A short, noisy, bass-heavy sci-fi blaster shot: a dense burst with a thumpy low body and a bright, diagonally sweeping upper layer that decays over about half a second. | noisy, blaster, electronic, boomy | -17.6 / -2.6 | medium |
| [`sounds/guns/blasterrifle_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsblasterrifle_shot_cogg) | A short, noisy, bass-heavy blaster shot with a punchy burst at the start, a few faint descending sweep lines, and a rumbling tail that fades over about 0.6 s. | blaster, noisy, boomy, transient | -19.0 / -3.0 | medium |
| [`sounds/guns/boltaction_rechamber.ogg`](../AUDIO-PROFILES.md#soundsgunsboltaction_rechamberogg) | A short mechanical bolt-action cycle: a few light metallic clicks, a heavier bolt-slide clack, a quiet gap, then a second louder burst of rattling and a final click, after about 0.42s of silence. | mechanical, metallic, transient, noisy | -20.6 / -2.9 | medium |
| [`sounds/guns/boltaction_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsboltaction_reloadogg) | A mechanical bolt-action reload made of three bursts of noisy metallic clacks and slides separated by short silences, ending abruptly on a cut-off third burst. | mechanical, metallic, transient, noisy | -20.6 / -2.7 | medium |
| [`sounds/guns/boltaction_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsboltaction_shot_aogg) | A loud, boomy rifle shot with a sharp noisy crack and a heavy low-end thump, followed by a long, dark, fading reverberant tail. | gunshot, transient, noisy, boomy | -16.3 / -2.8 | high |
| [`sounds/guns/boltaction_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsboltaction_shot_bogg) | A loud, boomy rifle-style gunshot: a sharp broadband crack with heavy low-end thump that decays into a long, noisy reverberant tail over about 1.5 seconds. | transient, noisy, boomy, broadband | -12.5 / -0.2 | medium |
| [`sounds/guns/boltaction_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsboltaction_shot_cogg) | A loud, full-bodied rifle shot with a sharp noisy crack and heavy low-end thump, followed by a long reverberant tail that fades over about 1.7 seconds. | gunshot, transient, noisy, boomy | -13.0 / -0.5 | high |
| [`sounds/guns/cdb_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunscdb_shot_aogg) | A heavy, boomy, noise-based blast with a bass-heavy thump that decays over about 1.5 seconds into a rumbling tail, like a big energy cannon firing. | boomy, explosive, noisy, bassy | -13.0 / -1.7 | medium |
| [`sounds/guns/cdb_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunscdb_shot_bogg) | A heavy, boomy, noisy energy-blast gunshot with a thick sub and bass body that decays over about 1.5 seconds into a rumbling, hissy tail. | boomy, noisy, explosive, heavy | -13.9 / -1.9 | medium |
| [`sounds/guns/cdb_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunscdb_shot_cogg) | A heavy, booming, noisy energy-weapon blast with a thick low-end body that sustains for about a second and then fades into a rumbling tail. | boomy, noisy, heavy, electronic | -13.2 / -1.8 | medium |
| [`sounds/guns/cdb_shot_d.ogg`](../AUDIO-PROFILES.md#soundsgunscdb_shot_dogg) | A heavy, bass-dominated explosive blast with a rumbling noisy body that decays over about 1.7 seconds, like a big energy-weapon cannon shot. | boomy, explosive, rumbling, low-end | -13.4 / -1.6 | medium |
| [`sounds/guns/chainsaw_hit_a.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_hit_aogg) | A low, gritty, noisy chainsaw-like growl that swells quickly to a peak around 0.23 s, then slowly fades over about a second. | noisy, mechanical, gritty, low-end | -15.6 / -2.7 | medium |
| [`sounds/guns/chainsaw_hit_b.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_hit_bogg) | A low, growly, noisy mechanical burst that swells over about 0.2 s into a rough buzzing roar, then fades over roughly a second, like a chainsaw biting into something and winding down. | noisy, mechanical, growly, low-end heavy | -15.6 / -2.7 | medium |
| [`sounds/guns/chainsaw_hit_c.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_hit_cogg) | A low, rumbling, noisy chainsaw-like growl that swells quickly to a peak around 0.23 s and then slowly fades into a quiet tail over about 1.3 s. | noisy, mechanical, rumbling, boomy | -15.6 / -2.7 | medium |
| [`sounds/guns/chainsaw_hit_d.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_hit_dogg) | A low, rumbling, noisy chainsaw-like growl that swells quickly to a peak around 0.23s and then fades gradually over about a second. | noisy, boomy, mechanical, rumble | -15.6 / -2.7 | medium |
| [`sounds/guns/chainsaw_hit_e.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_hit_eogg) | A low, rough, noisy mechanical growl that swells quickly to a peak around 0.23 s, then gradually fades into a fluttering, chewing tail over about 1.2 s, like a chainsaw biting into something. | noisy, mechanical, low-end heavy, growl | -15.6 / -2.7 | medium |
| [`sounds/guns/chainsaw_hit_f.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_hit_fogg) | A low, rumbling, noisy chainsaw-like growl that swells quickly to a peak around 0.23s, then slowly fades into a ragged, fluttering tail over about 1.3 seconds. | noisy, mechanical, rumbling, boomy | -15.6 / -2.7 | medium |
| [`sounds/guns/chainsaw_loop_a.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_loop_aogg) | A low, buzzy, pulsing engine-like rumble with harmonic bass and low-mid content that holds at a steady level for about half a second, then fades away over the rest of the clip. | mechanical, buzzy, tonal, boomy | -14.4 / -2.0 | medium |
| [`sounds/guns/chainsaw_loop_b.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_loop_bogg) | A gritty, harmonically dense chainsaw-like engine growl that swells in for about half a second, holds briefly, then winds down and fades into a quiet tail. | mechanical, growly, tonal, harmonic | -14.0 / -2.5 | medium |
| [`sounds/guns/chainsaw_loop_c.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_loop_cogg) | A gritty, buzzing chainsaw-like engine rumble that swells over the first ~0.4 s, holds briefly, then fades out into a long decaying tail by about 1.5 s. | mechanical, engine, gritty, buzzy | -14.0 / -2.5 | medium |
| [`sounds/guns/chainsaw_loop_d.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_loop_dogg) | A short, low-pitched, rumbling chainsaw-like engine burst that swells up to a peak around 0.4 s and fades out again, with a rough, pulsing, buzzy texture. | mechanical, rumbling, low-end, pulsing | -13.0 / -2.5 | medium |
| [`sounds/guns/chainsaw_loopend.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_loopendogg) | A short, low-pitched chainsaw engine rumble that swells in, peaks around 0.4 s, then winds down and fades out in under a second. | mechanical, rumbling, low-end, tonal | -13.0 / -2.5 | medium |
| [`sounds/guns/chainsaw_loopstart.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_loopstartogg) | A short, low-pitched chainsaw engine rev-up swell: a gritty, rumbling motor burst that fades in, peaks around 0.45 s and fades out within 0.86 s. | mechanical, rumbling, buzzy, low-end | -13.0 / -2.5 | medium |
| [`sounds/guns/chainsaw_swing_a.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_swing_aogg) | A mono, engine-like chainsaw swing: a loud, buzzy, low-pitched motor rev for about 0.5 s that drops to a quieter, rattly sustain and fades out by about 1.2 s. | mechanical, engine, buzzy, noisy | -19.5 / -4.4 | medium |
| [`sounds/guns/chainsaw_swing_b.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_swing_bogg) | A buzzy, low-pitched chainsaw engine drone that pulses in surging bursts for about 1.4 seconds and then fades out. | mechanical, buzzy, tonal, harmonic | -18.6 / -4.3 | medium |
| [`sounds/guns/chainsaw_swing_c.ogg`](../AUDIO-PROFILES.md#soundsgunschainsaw_swing_cogg) | A short, gritty, low-heavy chainsaw-like buzz that swells in two rev pulses over about 0.45s, then drops to a thin, sputtering tail that fades out by about 1.1s. | mechanical, buzzy, low-heavy, harmonic | -19.2 / -4.0 | medium |
| [`sounds/guns/combatshotgun_pump.ogg`](../AUDIO-PROFILES.md#soundsgunscombatshotgun_pumpogg) | A shotgun pump-action cycle: a small click, a rattling metallic slide back with a heavy thunk, then a shorter slide forward that lands with a second solid clack. | mechanical, metallic, transient, noisy | -14.7 / -0.8 | medium |
| [`sounds/guns/combatshotgun_reload.ogg`](../AUDIO-PROFILES.md#soundsgunscombatshotgun_reloadogg) | A sequence of sharp metallic clacks, slides and clicks of a shotgun being reloaded, with a gap near 1.2 s and a loud double thunk-clack finish around 2.4-2.75 s. | transient, mechanical, metallic, clicky | -19.6 / -0.3 | medium |
| [`sounds/guns/combatshotgun_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunscombatshotgun_shot_aogg) | A single heavy, bass-rich shotgun blast with a noisy broadband crack that decays into a rumbling tail over about 1.5 seconds. | boomy, noisy, transient, explosive | -12.9 / -0.5 | medium |
| [`sounds/guns/combatshotgun_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunscombatshotgun_shot_bogg) | A single heavy, boomy shotgun blast with a bright noisy crack that collapses into a thick low-end thump and a long, fading reverberant tail. | transient, boomy, noisy, explosive | -12.9 / -0.3 | medium |
| [`sounds/guns/combatshotgun_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunscombatshotgun_shot_cogg) | A heavy, boomy shotgun blast with a thunderous low-end body and a long, decaying reverberant tail. | boomy, transient, noisy, explosive | -13.7 / -1.0 | medium |
| [`sounds/guns/flamethrower_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsflamethrower_reloadogg) | A multi-stage mechanical reload sequence of rattly clanks, clicks and handling thumps with a gap in the middle, reading as a weapon being manipulated, with fuel-canister or latch-like thuds. | mechanical, transient, metallic, clicky | -23.7 / -4.4 | medium |
| [`sounds/guns/flamethrower_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsflamethrower_shot_aogg) | A roughly 1.3 second burst of low, rumbling, noisy roar, like a gas jet or flame, that stays fairly steady and then cuts off with a short fade at the end. | noisy, rumbling, whoosh, boomy | -16.9 / -4.5 | medium |
| [`sounds/guns/flamethrower_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsflamethrower_shot_bogg) | A low, rumbling, roaring burst of noisy fire-like whoosh that holds steady for about a second, then fades out quickly. | noisy, rumbling, whoosh, roar | -18.0 / -4.7 | medium |
| [`sounds/guns/flamethrower_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsflamethrower_shot_cogg) | A continuous, low-frequency roaring rush of turbulent noise, like a flamethrower burst, that swells slightly early on and fades out over the last quarter second. | noisy, roaring, boomy, turbulent | -17.9 / -4.7 | medium |
| [`sounds/guns/flamethrower_shot_d.ogg`](../AUDIO-PROFILES.md#soundsgunsflamethrower_shot_dogg) | A low, rumbling, roaring burst of noisy fire-like turbulence that swells to a peak around 0.43 s, holds, then fades out by about 1.1 s. | roar, rumbling, noisy, boomy | -17.4 / -4.1 | medium |
| [`sounds/guns/flamethrower_shot_e.ogg`](../AUDIO-PROFILES.md#soundsgunsflamethrower_shot_eogg) | A roaring, rumbling burst of low-frequency noise like a gas flame jet, steady and fluttering for about a second before a quick cut-off tail. | noisy, rumbling, boomy, whoosh | -16.6 / -4.8 | medium |
| [`sounds/guns/flamethrower_start_a.ogg`](../AUDIO-PROFILES.md#soundsgunsflamethrower_start_aogg) | A roughly one-second low, rumbling roar of fire igniting, with a sudden whoomph onset followed by a sustained, fluttering, noisy burn that fades out. | rumbling, noisy, boomy, whoosh | -17.1 / -4.0 | medium |
| [`sounds/guns/goldenrevolver_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsgoldenrevolver_shot_aogg) | A single heavy, boomy revolver shot with a bright noisy crack that decays into a long low rumbling tail, with a small secondary bump near 0.94 s. | gunshot, boomy, noisy, transient | -15.9 / -0.8 | medium |
| [`sounds/guns/goldenrevolver_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsgoldenrevolver_shot_bogg) | A loud, boomy revolver gunshot: a sharp noisy crack with a heavy low-end thump that decays into a smooth rumbling tail over about 2.5 seconds. | gunshot, boomy, noisy, transient | -14.5 / -0.2 | high |
| [`sounds/guns/goldenrevolver_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsgoldenrevolver_shot_cogg) | A loud, boomy revolver gunshot with a broadband noisy blast at the start that falls into a low rumbling tail lasting about 2 seconds. | transient, boomy, noisy, gunshot | -15.0 / -1.0 | medium |
| [`sounds/guns/grenade_pin_a.ogg`](../AUDIO-PROFILES.md#soundsgunsgrenade_pin_aogg) | A short, two-part mechanical click-clack, like a grenade pin being pulled, with a sharp first snap and a slightly louder second snap about 80 ms later, followed by a quickly fading tail. | transient, mechanical, metallic, clicky | -16.6 / -0.9 | medium |
| [`sounds/guns/grenade_pin_b.ogg`](../AUDIO-PROFILES.md#soundsgunsgrenade_pin_bogg) | A short, bright metallic double-click, like a grenade pin being pulled: a sharp snap followed by a second heavier click and a quick rattly decay. | transient, metallic, clicky, mechanical | -17.7 / -1.1 | medium |
| [`sounds/guns/grenade_throw_a.ogg`](../AUDIO-PROFILES.md#soundsgunsgrenade_throw_aogg) | A short, low-heavy rustling and clunking handling sound, like a grenade being gripped and swung, with a build-up to a dull thump around 0.27s and a quiet fading tail. | noisy, mechanical, thump, low-mid | -20.1 / -1.2 | medium |
| [`sounds/guns/grenade_throw_b.ogg`](../AUDIO-PROFILES.md#soundsgunsgrenade_throw_bogg) | A short, low-mid heavy handling rustle with a couple of sharp clicks early on and a dense, thumpy cluster around 0.25s, then a quick fade, like a grenade being grabbed and pulled or thrown. | transient, handling, mechanical, clicky | -19.5 / -1.2 | medium |
| [`sounds/guns/grenade_throw_c.ogg`](../AUDIO-PROFILES.md#soundsgunsgrenade_throw_cogg) | A short, rattly grenade handling sound: a few small clicks and gear rustles, then a larger noisy metallic clack-and-scrape burst around 0.2-0.35s, with a quiet fading tail. | transient, noisy, metallic, mechanical | -18.0 / -1.2 | medium |
| [`sounds/guns/grenadelauncher_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsgrenadelauncher_reloadogg) | A multi-step mechanical reload sequence of metallic clicks, clacks and scrapes, with a heavier thunk near 0.7s and a louder clunk at about 3.2s followed by a gritty rattling tail. | mechanical, transient, metallic, clicky | -28.6 / -4.1 | medium |
| [`sounds/guns/grenadelauncher_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsgrenadelauncher_shot_aogg) | A deep, boomy grenade-launcher shot: a sharp thump with heavy low-end body that decays through a rumbling, crackly tail of about 0.7 s. | boomy, transient, noisy, low-end | -21.5 / -3.1 | medium |
| [`sounds/guns/grenadelauncher_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsgrenadelauncher_shot_bogg) | A heavy, boomy thump-style launcher shot with a noisy burst up front and a rumbling low-end tail that fades out in under a second. | boomy, thump, noisy, transient | -20.5 / -3.0 | medium |
| [`sounds/guns/grenadelauncher_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsgrenadelauncher_shot_cogg) | A single deep, boomy grenade-launcher thump with a short noisy crack at the front and a quickly decaying low-frequency rumble tail. | boomy, transient, noisy, low-end | -22.1 / -2.4 | medium |
| [`sounds/guns/handgun_reload.ogg`](../AUDIO-PROFILES.md#soundsgunshandgun_reloadogg) | A short, busy handgun reload made of several clicky, rattly mechanical handling noises: magazine and slide movements, with a heavier clack near 0.8 s, then a few lighter clicks and a quick fade. | transient, mechanical, metallic, clicky | -21.2 / -4.0 | medium |
| [`sounds/guns/handgun_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunshandgun_shot_aogg) | A boomy, bass-heavy handgun shot with a hard noisy crack at the start, followed by a long reverberant tail that decays over about 1.5 seconds. | boomy, noisy, transient, explosive | -18.0 / -2.8 | medium |
| [`sounds/guns/handgun_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunshandgun_shot_bogg) | A heavy, bass-weighted handgun shot with a sharp boomy crack at the start, followed by a long, noisy reverberant tail that decays over about 2 seconds. | gunshot, boomy, transient, noisy | -19.2 / -2.2 | medium |
| [`sounds/guns/handgun_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunshandgun_shot_cogg) | A boomy, bass-heavy handgun shot with a loud low-frequency crack at the start, a long decaying reverberant tail, and a faint secondary clatter around 1.4 seconds. | transient, boomy, noisy, low-end | -20.2 / -2.4 | medium |
| [`sounds/guns/lasergun_reload.ogg`](../AUDIO-PROFILES.md#soundsgunslasergun_reloadogg) | A multi-part mechanical reload sequence of clacky, metallic handling clicks in three bursts, ending with a decaying electronic charge-up or latch with a faint high whine. | transient, mechanical, metallic, clicky | -25.6 / -3.8 | medium |
| [`sounds/guns/lasergun_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunslasergun_shot_aogg) | A heavy, bass-laden sci-fi laser blast with a brief high-frequency downward zap at the start, dissolving into a rumbling noisy tail. | boomy, electronic, laser, sweep | -15.0 / -3.4 | medium |
| [`sounds/guns/lasergun_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunslasergun_shot_bogg) | A short, bass-heavy laser gun shot: a punchy, gritty low-end blast with a fast descending zap sweep at the very start, decaying into a quiet tail. | electronic, boomy, noisy, gritty | -15.1 / -3.6 | medium |
| [`sounds/guns/lasergun_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunslasergun_shot_cogg) | A heavy, bass-laden laser gun shot with a fast downward chirp at the start, a dense noisy body that fades over about 0.6 s, and a long quiet tail. | laser, electronic, boomy, noisy | -15.0 / -3.3 | medium |
| [`sounds/guns/lmg_reload.ogg`](../AUDIO-PROFILES.md#soundsgunslmg_reloadogg) | A multi-stage light machine gun reload of mechanical handling: a low thump, clicks and clacks, a mag-out and mag-in, a bolt or charging handle rack, then a final slap, with silence gaps between the stages. | mechanical, transient, metallic, clicky | -17.8 / -0.8 | medium |
| [`sounds/guns/lmg_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunslmg_shot_aogg) | A single heavy, bass-laden machine-gun shot with a loud noisy crack that quickly falls into a long, rumbling reverberant tail. | transient, boomy, noisy, heavy | -17.6 / -1.9 | high |
| [`sounds/guns/lmg_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunslmg_shot_bogg) | A single heavy, boomy machine-gun shot with a sharp noisy crack up front and a long, reverberant low-end tail that fades into silence. | transient, noisy, boomy, gunshot | -17.2 / -0.3 | high |
| [`sounds/guns/lmg_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunslmg_shot_cogg) | A single heavy, bass-rich machine-gun shot with a noisy crack at the start, a booming body and a fading reverberant tail. | transient, boomy, noisy, explosive | -17.4 / 0.3 | high |
| [`sounds/guns/mac10_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsmac10_reloadogg) | A mechanical SMG reload made of three separated bursts of clicky metal handling sounds: a magazine drop and rattle, a magazine insertion with clacks, and a final bolt or slide rack. | mechanical, metallic, transient, clicky | -19.7 / -1.3 | medium |
| [`sounds/guns/mac10_reload_akimbo.ogg`](../AUDIO-PROFILES.md#soundsgunsmac10_reload_akimboogg) | A mono, clicky mechanical reload sequence of a submachine gun with four clusters of metallic clacks and rattles separated by silent gaps, not gunshots. | transient, mechanical, metallic, clicky | -19.0 / -1.0 | medium |
| [`sounds/guns/mac10_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsmac10_shot_aogg) | A single punchy submachine-gun shot with a heavy low-end thump and noisy crack that decays into a short, smooth reverberant tail. | gunshot, transient, boomy, noisy | -15.9 / -0.2 | high |
| [`sounds/guns/mac10_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsmac10_shot_bogg) | A single heavy, boomy submachine-gun shot with a noisy crack at the start and a low-frequency body that decays into a short smooth tail over about a second. | gunshot, boomy, transient, noisy | -15.8 / -0.1 | high |
| [`sounds/guns/mac10_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsmac10_shot_cogg) | A single punchy, bass-heavy submachine gun shot with a noisy crack that decays quickly into a long, fading reverb-like tail. | gunshot, transient, noisy, boomy | -15.7 / -0.2 | high |
| [`sounds/guns/MIB_blaster_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsmib_blaster_reloadogg) | A 2.3-second sci-fi blaster reload built from sharp metallic clacks and clunks, a rattling mid-section, and a rising electronic charge-up whine at the very end. | transient, metallic, mechanical, clacky | -23.0 / -0.9 | medium |
| [`sounds/guns/MIB_blaster_reload_akimbo.ogg`](../AUDIO-PROFILES.md#soundsgunsmib_blaster_reload_akimboogg) | A busy mechanical reload sequence for dual blasters: sharp metallic clacks and snaps, a few heavy thunks, rattling handling noise, and a short rising electronic whine at the end. | transient, metallic, mechanical, clicky | -20.1 / 0.3 | medium |
| [`sounds/guns/MIB_blaster_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsmib_blaster_shot_aogg) | A loud sci-fi blaster shot with a bright, noisy zap on top of a heavy low-end thump, followed by a long fading rumbling tail. | blaster, electronic, noisy, boomy | -14.7 / 0.2 | medium |
| [`sounds/guns/MIB_blaster_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsmib_blaster_shot_bogg) | A loud, punchy sci-fi blaster shot with a noisy, boomy low-end thump, a bright crackly burst at the start, and a fading tail of rumble and hiss. | transient, boomy, noisy, electronic | -14.7 / -0.3 | medium |
| [`sounds/guns/MIB_blaster_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsmib_blaster_shot_cogg) | A heavy, bass-rich sci-fi blaster blast with a bright noisy crack up front that decays into a rumbling, reverberant tail over about 1.2 seconds. | boomy, noisy, transient, electronic | -13.9 / -0.6 | medium |
| [`sounds/guns/minigun_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsminigun_reloadogg) | A multi-stage mechanical reload sequence of clunks, rattles and metallic clacks with a few louder thuds around 1.6s, 3.3s, 3.8s and 5.2s, then a short tail. | mechanical, transient, metallic, clacky | -23.7 / -3.4 | medium |
| [`sounds/guns/minigun_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsminigun_shot_aogg) | A loud, low-heavy burst of rapid-fire gunfire noise lasting about 0.3 s, followed by a quickly fading, muffled rumble tail out to about 0.85 s. | gunshot, boomy, noisy, transient | -13.0 / -0.6 | medium |
| [`sounds/guns/minigun_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsminigun_shot_bogg) | A loud, gritty, bass-heavy burst of rapid-fire gunfire noise lasting about 0.3 s, followed by a quickly fading low-level tail out to 1 s. | noisy, boomy, transient, gritty | -12.8 / -1.5 | medium |
| [`sounds/guns/minigun_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsminigun_shot_cogg) | A loud, bass-heavy burst of rattling gunfire noise lasting about 0.3 s, followed by a quick muffled decay tail that fades out by about 0.9 s. | boomy, noisy, mechanical, burst | -12.9 / -1.2 | medium |
| [`sounds/guns/minigun_shot_d.ogg`](../AUDIO-PROFILES.md#soundsgunsminigun_shot_dogg) | A dense, rattling burst of low-heavy noisy gunfire lasting about 0.3 s, followed by a quickly fading muffled tail of roughly 0.7 s. | noisy, boomy, transient, mechanical | -12.8 / -1.2 | medium |
| [`sounds/guns/minigun_start_a.ogg`](../AUDIO-PROFILES.md#soundsgunsminigun_start_aogg) | A very short, low-pitched mechanical rumble of a minigun barrel starting to spin, pulsing about 11 times per second with a growl and some gritty noise on top. | mechanical, boomy, pulsing, low-end | -70.0 / -2.1 | medium |
| [`sounds/guns/minigun_start_b.ogg`](../AUDIO-PROFILES.md#soundsgunsminigun_start_bogg) | A very short, low-pitched mechanical rumble made of three heavy pulsing bursts over a noisy bed, like a minigun barrel beginning to spin up. | mechanical, boomy, pulsing, noisy | -70.0 / -4.6 | medium |
| [`sounds/guns/minigun_start_c.ogg`](../AUDIO-PROFILES.md#soundsgunsminigun_start_cogg) | A very short, low-end-heavy mechanical rumble of a minigun barrel starting to spin, with a few pulsing thuds repeating about every 0.1 s over a noisy bed. | mechanical, boomy, rumble, pulsing | -70.0 / -3.8 | medium |
| [`sounds/guns/pdw_reload.ogg`](../AUDIO-PROFILES.md#soundsgunspdw_reloadogg) | A mechanical weapon reload sequence: a series of metallic clicks, rattles and slides building to a louder bolt or charging-handle slam around 1.1s, followed by a second rattle and slam near 1.9s, then a few quiet settling ticks. | mechanical, metallic, transient, noisy | -17.0 / -1.3 | medium |
| [`sounds/guns/pdw_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunspdw_shot_aogg) | A heavy, bass-dominant gunshot with a thick low-end boom that stays strong for about half a second, then decays into a rumbling, noisy tail. | boomy, noisy, low-heavy, gunshot | -14.4 / -2.0 | medium |
| [`sounds/guns/pdw_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunspdw_shot_bogg) | A heavy, bass-dominant gunshot with a thick low-end boom and noisy mid crack that decays into a fading tail over about a second. | gunshot, boomy, noisy, transient | -14.1 / -1.4 | medium |
| [`sounds/guns/pdw_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunspdw_shot_cogg) | A single boomy, bass-heavy gunshot with a noisy crack at the start and a long rumbling reverberant decay into a quiet tail. | gunshot, boomy, noisy, low-end | -14.2 / -1.9 | medium |
| [`sounds/guns/pistol_reload.ogg`](../AUDIO-PROFILES.md#soundsgunspistol_reloadogg) | A pistol reload made of several separate mechanical handling sounds: a click-clack, a loud magazine thunk with rattles, a softer slide or scrape and a final slide-release snap, with gaps of silence between them. | transient, mechanical, metallic, clicky | -22.1 / -2.9 | medium |
| [`sounds/guns/pistol_reload_akimbo.ogg`](../AUDIO-PROFILES.md#soundsgunspistol_reload_akimboogg) | A sequence of crisp, bright mechanical handling sounds, like two pistols being reloaded in turn with clicks, slides and magazine snaps, separated by short gaps. | transient, mechanical, metallic, clicky | -20.7 / -2.5 | medium |
| [`sounds/guns/pistol_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunspistol_shot_aogg) | A single loud, bass-heavy pistol shot with a quick crack and a thick noisy body that decays into a long reverberant tail of about one second. | transient, boomy, noisy, broadband | -13.3 / -2.3 | high |
| [`sounds/guns/pistol_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunspistol_shot_bogg) | A heavy, boomy pistol shot with a thick low-end body and a noisy decaying tail that fades over about a second. | transient, boomy, noisy, gunshot | -12.2 / -2.0 | medium |
| [`sounds/guns/pistol_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunspistol_shot_cogg) | A loud, full-bodied pistol shot with a sharp noisy crack at the start and a heavy low-end thump, followed by a long reverberant tail that fades out over about 0.9 s. | transient, noisy, boomy, gunshot | -12.6 / -1.1 | medium |
| [`sounds/guns/powerhammer_charge_a.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_charge_aogg) | A roughly 2.5 second rumbling, noisy low-end swell, like a heavy energy weapon charging up, that peaks near 1.2 s and then tails off into a muffled decay. | noisy, rumbling, boomy, sustained | -17.5 / -3.6 | medium |
| [`sounds/guns/powerhammer_charge_b.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_charge_bogg) | A rumbling, noisy mechanical charge-up: a dense low-frequency roar that holds for about 1.3 seconds, then falls away into a fading, rattly tail. | rumbling, noisy, mechanical, boomy | -17.8 / -4.5 | medium |
| [`sounds/guns/powerhammer_charge_c.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_charge_cogg) | A noisy, low-heavy rumbling whoosh-like charge-up that swells quickly, holds a churning rumble for about a second, then fades into a long dark tail. | rumble, noisy, boomy, mechanical | -18.7 / -3.9 | medium |
| [`sounds/guns/powerhammer_impact_a.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_impact_aogg) | A heavy, low-end-weighted percussive impact thump with a noisy crunchy body that decays over about half a second into a faint, dark tail. | impact, boomy, transient, noisy | -17.0 / -1.8 | medium |
| [`sounds/guns/powerhammer_impact_b.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_impact_bogg) | A heavy, boomy low-frequency impact thud with a crunchy noisy body that decays over about 0.6 seconds into a short rumbling tail. | impact, boomy, transient, noisy | -17.2 / -1.8 | medium |
| [`sounds/guns/powerhammer_impact_c.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_impact_cogg) | A heavy, boomy impact thump with a noisy crack at the start that decays over about 0.7 s into a quiet, rumbling tail. | impact, boomy, transient, noisy | -17.2 / -1.8 | medium |
| [`sounds/guns/powerhammer_impact_d.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_impact_dogg) | A heavy, boomy low-frequency impact thump with a noisy crunchy mid layer that decays over about a second into a rumbling tail. | boomy, impact, noisy, heavy | -17.3 / -2.1 | medium |
| [`sounds/guns/powerhammer_impactGround_a.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_impactground_aogg) | A heavy, low-frequency ground slam with a thick rumbling body that decays over about a second into a dark rumble tail. | boomy, impact, rumbling, noisy | -17.7 / -2.4 | medium |
| [`sounds/guns/powerhammer_impactGround_b.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_impactground_bogg) | A heavy, bass-dominant rumbling slam with crunchy debris texture that sustains for about 0.8 s and then decays into a quiet tail, like a massive hammer smashing into the ground. | boomy, impact, rumble, noisy | -16.9 / -1.9 | medium |
| [`sounds/guns/powerhammer_impactGround_c.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_impactground_cogg) | A heavy, low-frequency ground-slam impact with a short bright crack at the start, a sustained rumbling body of about 0.5 s, and a fading low rumble tail. | boomy, impact, rumble, low-end | -17.6 / -2.0 | medium |
| [`sounds/guns/powerhammer_impactGround_d.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_impactground_dogg) | A heavy, low-frequency rumbling ground impact with a bright crunchy initial burst that sustains as a churning boom for about a second, then falls away into a fading low tail. | boomy, rumble, impact, noisy | -17.0 / -1.9 | medium |
| [`sounds/guns/powerhammer_reload.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_reloadogg) | A multi-stage mechanical gun reload: soft handling rustles, a small clunk, a heavy slam with noisy metallic impact around 1.8s, then a long bright ratcheting/sliding rattle with a final impact that fades out. | mechanical, metallic, noisy, transient | -24.0 / -4.6 | medium |
| [`sounds/guns/powerhammer_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_shot_aogg) | A heavy, bass-dominated gun blast with a noisy crack at the start, followed by a rumbling, crackling tail that fades over about 2 seconds. | boomy, noisy, explosive, rumbling | -20.1 / -0.6 | medium |
| [`sounds/guns/powerhammer_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_shot_bogg) | A heavy, bass-saturated gun blast with a crackly noisy burst up front that swells into a long, rumbling sub-heavy tail fading over about 2 seconds. | boomy, sub-heavy, noisy, explosive | -17.5 / -1.5 | medium |
| [`sounds/guns/powerhammer_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_shot_cogg) | A heavy, boomy low-frequency gun blast with a noisy crackling top end that rumbles and decays over about 1.5 seconds into a quiet tail. | boomy, rumbling, noisy, heavy | -19.2 / -2.2 | medium |
| [`sounds/guns/powerhammer_swing_a.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_swing_aogg) | A heavy, rumbling whoosh-swing of low-frequency noise that bursts in with a gritty, textured body, then gradually tapers into a low rumble over about 1.5 seconds, like a big heavy tool swung through the air. | whoosh, noisy, boomy, rumbling | -21.0 / -3.2 | medium |
| [`sounds/guns/powerhammer_swing_b.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_swing_bogg) | A heavy, rumbling low-end whoosh-swing with a gritty noisy start that fades into a pulsing, dying low rumble, like a massive hammer swung through the air. | boomy, rumble, noisy, whoosh | -18.8 / -1.7 | medium |
| [`sounds/guns/powerhammer_swing_c.ogg`](../AUDIO-PROFILES.md#soundsgunspowerhammer_swing_cogg) | A heavy, low-end-weighted noisy whoosh with a rumbling body and a few thudding impacts in the middle that fades out into a long soft tail, like a big hammer swung through the air. | whoosh, noisy, boomy, low-heavy | -19.7 / -3.7 | medium |
| [`sounds/guns/pulserifle_reload.ogg`](../AUDIO-PROFILES.md#soundsgunspulserifle_reloadogg) | A mechanical weapon reload made of a rapid series of metallic clicks, clacks and rattles in three bursts separated by short silent gaps. | mechanical, metallic, transient, clicky | -21.6 / -3.1 | medium |
| [`sounds/guns/pulserifle_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunspulserifle_shot_aogg) | A heavy, bass-rich energy-weapon shot: a loud broadband noisy crack and thump that decays over about 1.5 s into a dark rumbling tail. | boomy, noisy, transient, heavy | -13.7 / 0.1 | medium |
| [`sounds/guns/pulserifle_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunspulserifle_shot_bogg) | A heavy, noisy sci-fi rifle blast with a thick low-end thump and a bright crackly burst up front, then a long rumbling decay tail. | transient, noisy, boomy, explosive | -13.6 / -0.6 | medium |
| [`sounds/guns/pulserifle_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunspulserifle_shot_cogg) | A heavy, boomy, noisy gunshot-like blast with a strong low-end thump that decays into a long rumbling, reverberant tail over about 2.5 seconds. | explosive, noisy, boomy, low-heavy | -13.7 / -0.7 | medium |
| [`sounds/guns/pulserifle_shot_d.ogg`](../AUDIO-PROFILES.md#soundsgunspulserifle_shot_dogg) | A heavy, boomy noise-burst gunshot with a thick sub and bass thump and bright crackly top that decays into a long rumbling tail over about 1.5 seconds. | boomy, noisy, explosive, impact | -14.3 / -1.1 | medium |
| [`sounds/guns/pulserifle_shot_stereo.ogg`](../AUDIO-PROFILES.md#soundsgunspulserifle_shot_stereoogg) | A single heavy, boomy energy-weapon shot with a broadband noisy burst and strong low-end thump that decays over about 1.5 seconds into a rumbling tail. | boomy, noisy, transient, explosive | -11.2 / -1.9 | medium |
| [`sounds/guns/revolver_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsrevolver_reloadogg) | A multi-stage revolver reload: a heavy clunky handling burst at the start, a series of sparse metallic clicks and ratchet ticks through the middle, a silent gap near 2.2s, then a flurry of sharp clicks as the cylinder is closed or spun. | transient, metallic, mechanical, clicky | -26.6 / -4.6 | medium |
| [`sounds/guns/revolver_reload_akimbo.ogg`](../AUDIO-PROFILES.md#soundsgunsrevolver_reload_akimboogg) | A sequence of dry mechanical gun-handling sounds: two similar bursts of metallic clicks and ratchets at the start, sparse ticks through the middle, then two clusters of rapid clicks at the end, like a revolver being reloaded with both hands. | transient, metallic, mechanical, clicky | -24.7 / -4.5 | medium |
| [`sounds/guns/revolver_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsrevolver_shot_aogg) | A single heavy revolver gunshot with a sharp, broadband crack and a thick low-end boom that decays into a reverberant tail over roughly one second. | transient, boomy, noisy, explosive | -17.7 / -2.5 | medium |
| [`sounds/guns/revolver_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsrevolver_shot_bogg) | A single heavy revolver gunshot with a sharp noisy crack and strong low-end boom that decays into a fading reverberant tail over about 1.2 seconds. | transient, boomy, noisy, explosive | -17.3 / -2.0 | high |
| [`sounds/guns/revolver_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsrevolver_shot_cogg) | A single heavy revolver gunshot with a thick low-end boom and noisy crack that decays into a long, dark reverberant tail with faint late echo ticks. | transient, boomy, noisy, explosive | -17.1 / -2.1 | medium |
| [`sounds/guns/rocket_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsrocket_reloadogg) | A sparse, rustly mechanical rocket-launcher reload: soft handling clatters, one louder metallic clunk/thud around 1.0s, a quiet gap, then more small clicks and a low rumbly slide at the end. | transient, mechanical, noisy, clattery | -24.9 / -4.4 | medium |
| [`sounds/guns/rocket_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsrocket_shot_aogg) | A heavy, booming rocket launch blast with a rumbling sub-bass body and a noisy whoosh of debris that decays over about 1.5 seconds into a dark tail. | boomy, noisy, explosive, rumble | -16.5 / -3.3 | medium |
| [`sounds/guns/rocket_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsrocket_shot_bogg) | A heavy, low-end rocket launch blast: a sudden noisy boom with a rumbling whoosh that decays over about 1.6 seconds, with a few small crackle accents in the tail. | boomy, noisy, explosive, whoosh | -17.0 / -3.4 | medium |
| [`sounds/guns/rocket_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsrocket_shot_cogg) | A deep, noisy rocket launch blast with a heavy sub-bass thump that quickly rumbles and fades into a long, dark tail. | boomy, noisy, rumble, explosive | -18.6 / -4.0 | medium |
| [`sounds/guns/sawedoff_reload.ogg`](../AUDIO-PROFILES.md#soundsgunssawedoff_reloadogg) | A sawed-off shotgun reload built from a series of small metallic clicks and clacks (break-open, shell handling, loading) that ends in a louder, bright snap of the barrel closing. | transient, metallic, mechanical, clicky | -24.8 / -3.7 | medium |
| [`sounds/guns/sawedoff_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunssawedoff_shot_aogg) | A heavy, boomy shotgun blast with a thick low-end body and noisy crack that decays into a long rumbling tail of about 1.3 seconds. | boomy, noisy, explosive, low-heavy | -11.0 / -1.2 | medium |
| [`sounds/guns/sawedoff_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunssawedoff_shot_bogg) | A heavy, boomy shotgun blast with a fast attack and a long rumbling noise tail that fades over about 1.2 seconds. | boomy, noisy, transient, low-heavy | -11.0 / -1.5 | medium |
| [`sounds/guns/sawedoff_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunssawedoff_shot_cogg) | A heavy, boomy sawed-off shotgun blast: a dense, bass-heavy noise burst that sustains for about half a second and then decays into a short rumbling tail. | boomy, noisy, explosive, bassy | -11.0 / -1.5 | high |
| [`sounds/guns/SCAR_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsscar_reloadogg) | A noisy, clicky mechanical rifle reload in three distinct phases: rattly magazine handling, a mid-clip clack with a sharper snap, then a heavy bolt or charging slam, followed by a quiet tail. | mechanical, transient, metallic, noisy | -16.4 / 0.2 | medium |
| [`sounds/guns/SCAR_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsscar_shot_aogg) | A single punchy, low-heavy rifle shot with a sharp noisy crack that decays into a short boomy tail of roughly a second. | transient, noisy, boomy, gunshot | -16.6 / -0.3 | medium |
| [`sounds/guns/SCAR_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsscar_shot_bogg) | A loud, bass-heavy rifle shot with a sharp noisy crack that falls away into a short fading rumble and tail. | gunshot, transient, boomy, noisy | -14.4 / -0.1 | medium |
| [`sounds/guns/SCAR_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsscar_shot_cogg) | A loud, bass-heavy rifle shot with a sharp noisy crack that quickly collapses into a short low thump and a fading reverberant tail. | gunshot, transient, boomy, noisy | -15.6 / 0.1 | medium |
| [`sounds/guns/silencedm4_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunssilencedm4_shot_aogg) | A short, punchy suppressed rifle shot with a thumpy low-end body and a brief noisy crack that decays quickly into a faint tail. | transient, noisy, boomy, suppressed | -18.5 / -0.3 | medium |
| [`sounds/guns/silencedm4_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunssilencedm4_shot_bogg) | A single short, punchy suppressed rifle shot with a thumpy low-end body, a brief noisy crack on top, and a quick decaying tail. | transient, noisy, boomy, suppressed | -18.6 / -1.1 | medium |
| [`sounds/guns/sonicshotgun_reload.ogg`](../AUDIO-PROFILES.md#soundsgunssonicshotgun_reloadogg) | A multi-stage mechanical reload for a sci-fi shotgun: rustling handling and clacks, a sharp loud clack/slide near 1.8s, a few rattles, then a low humming energy swell that fades out. | mechanical, transient, clicky, rattly | -21.0 / -2.6 | medium |
| [`sounds/guns/sonicshotgun_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunssonicshotgun_shot_aogg) | A heavy, low-frequency sonic blast: a rumbling, buzzy boom that swells over about half a second, stays rough and pulsing, then fades into a slow throbbing tail. | boomy, sub-heavy, rumbling, pulsing | -14.5 / -2.6 | medium |
| [`sounds/guns/sonicshotgun_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunssonicshotgun_shot_bogg) | A heavy, bass-dominated noisy blast with a rumbling, crackling body that slowly decays over about 1.7 seconds, like a sonic shotgun boom. | boomy, noisy, explosive, low-end | -13.7 / -2.3 | medium |
| [`sounds/guns/sonicshotgun_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunssonicshotgun_shot_cogg) | A heavy, bass-dominated noisy blast that swells to a peak around 0.2 s and then rumbles and decays over about 1.4 s, like a low sonic shotgun boom. | boomy, noisy, bassy, rumble | -13.5 / -2.1 | medium |
| [`sounds/guns/teslagun_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsteslagun_reloadogg) | A two-part sci-fi weapon reload: a quiet, rattly mechanical handling phase with a couple of clunks, then a gap and a loud, sustained, low-heavy electrical charge-up surge that fades out. | mechanical, electronic, noisy, buzzy | -17.1 / -2.5 | medium |
| [`sounds/guns/teslagun_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsteslagun_shot_aogg) | A loud, broadband, crackling electrical blast with a heavy low-end thump that decays over about two seconds into a sputtering, rumbling tail. | noisy, electric, crackle, boomy | -13.8 / -0.8 | medium |
| [`sounds/guns/teslagun_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsteslagun_shot_bogg) | A loud, noisy tesla-gun blast with a heavy low-end thump and bright crackling hiss that fades over about 3 seconds into a dull rumbling tail. | noisy, boomy, electric, crackle | -13.7 / -2.0 | medium |
| [`sounds/guns/teslagun_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsteslagun_shot_cogg) | A loud, noisy, bass-heavy blast of crackling energy with a bright burst at the start that decays into a long rumbling low-end tail over about 3 seconds. | noisy, boomy, explosive, electronic | -14.1 / -1.5 | medium |
| [`sounds/guns/teslagun_shot_d.ogg`](../AUDIO-PROFILES.md#soundsgunsteslagun_shot_dogg) | A heavy, bass-laden electrical blast with a noisy crackling hiss that rumbles and decays over about 3 seconds into a low-frequency tail. | boomy, noisy, electronic, crackle | -13.5 / -1.2 | medium |
| [`sounds/guns/thompson_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsthompson_reloadogg) | A short mechanical reload sequence of three clustered bursts of metallic clacks and rattles (likely mag release, magazine insertion, bolt or slide), with quiet gaps between them. | mechanical, metallic, transient, noisy | -23.5 / -4.6 | medium |
| [`sounds/guns/thompson_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsthompson_shot_aogg) | A single punchy, low-weighted submachine-gun shot with a sharp noisy crack at the start and a short reverberant decay tail of about 0.7 s. | transient, percussive, boomy, noisy | -19.2 / -1.1 | medium |
| [`sounds/guns/thompson_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsthompson_shot_bogg) | A single heavy, boomy submachine-gun shot with a dense noisy crack that decays smoothly into a short, darkening reverberant tail. | gunshot, boomy, noisy, transient | -19.4 / -1.0 | medium |
| [`sounds/guns/thompson_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsthompson_shot_cogg) | A punchy, noisy submachine-gun shot with a heavy low-end thump and a long, fading reverberant tail. | transient, noisy, boomy, percussive | -19.4 / -0.6 | medium |
| [`sounds/guns/vector_reload.ogg`](../AUDIO-PROFILES.md#soundsgunsvector_reloadogg) | A mono, gritty mechanical reload sequence of several clacky metal-and-plastic handling noises (mag out, mag in, bolt or slide action) with a clear silent gap near 0.95-1.10 s. | transient, mechanical, metallic, clacky | -15.9 / -1.1 | medium |
| [`sounds/guns/vector_shot_a.ogg`](../AUDIO-PROFILES.md#soundsgunsvector_shot_aogg) | A single punchy gunshot with an instant, heavy low-end thump and a noisy mid crack that decays into a short, dull reverberant tail. | transient, percussive, boomy, noisy | -15.3 / -0.8 | medium |
| [`sounds/guns/vector_shot_b.ogg`](../AUDIO-PROFILES.md#soundsgunsvector_shot_bogg) | A short, punchy, bass-heavy gunshot with an instant noisy crack that decays into a rumbling, crackling tail in about half a second. | transient, boomy, noisy, gunshot | -14.6 / -0.9 | medium |
| [`sounds/guns/vector_shot_c.ogg`](../AUDIO-PROFILES.md#soundsgunsvector_shot_cogg) | A short, punchy, bass-heavy gunshot with an instant noisy crack and thump that decays into a rumbling tail in about 0.45 s. | transient, boomy, noisy, gunshot | -16.3 / -0.7 | medium |
<!-- audio-profiles:end -->
