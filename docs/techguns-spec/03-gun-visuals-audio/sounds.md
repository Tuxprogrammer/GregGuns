# Sound catalogue: `sounds/guns/`

All 185 `.ogg` files in `assets/techguns/sounds/guns/`, the `sounds.json` events that reference them, and exactly when each event plays. Playback mechanics (TGSound, packets, gun position, volume categories) are in [rendering-framework.md §14](rendering-framework.md#14-sound-playback-mechanics-summary). Event names are used with the `techguns:` prefix in code, e.g. `techguns:guns.ak47Fire`.

When an event lists several files, Minecraft picks one at random each time it plays (vanilla `sounds.json` behaviour). Every gun event uses `"category": "player"` except `guns.chainsawLoopStart`, which uses `"master"`. The sound-system category is distinct from Techguns' own volume-multiplier category (GUN_FIRE / RELOAD / …, framework §14).

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
| guns.rocketFire | rocket_shot_a–c | fire | Rocket Launcher, Nuke Launcher, Grim Reaper. Also the Attack Helicopter NPC firing a rocket (volume 5, area 10). Dispenser use is commented out |
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
