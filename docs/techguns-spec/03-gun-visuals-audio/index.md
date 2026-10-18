# 03: Gun visuals and audio

How every Techguns 1.7.10 (v0.1.2 alpha) hand-held weapon **looks and sounds**: 3D box models, item renderers (first person, third person, dropped, inventory), recoil, reload, charge and firing animations, muzzle flashes, scopes and zoom, camo textures, the `textures/guns/` sheets, the one OBJ in `models/guns/`, and all `sounds/guns/` audio with its triggers.

Out of scope here, see the owning areas:

| Topic | Area |
|---|---|
| Stats, ammo, firing logic, item classes, recipes, gun tooltips, packets | 02-guns |
| Projectiles and their renderers, muzzle-flash *particles* seen by other players, FX textures (`textures/fx/*`, including the scope and flash sheets named here), particle systems | 04-projectiles-fx |
| Armour models, back-slot and face-slot renderers (the `additionalslots` package also hosts the off-hand akimbo renderer described in §11 of the framework) | 05-armor |
| Shared item metadata (magazines, rockets) | 06-items |
| Keybinds, config (volume categories, FOV lock, debug keybinds), client proxy plumbing | 01-core |
| NPC models, the Cyberdemon and helicopter that reuse gun sounds | 10-entities-npcs |

## Contents

| File | What it covers |
|---|---|
| [rendering-framework.md](rendering-framework.md) | Renderer registration table, class hierarchy, client timing state, per-frame algorithm, animation curves, coordinate conventions, muzzle-flash ScreenEffect, textures and camo, zoom and scopes, ammo-dependent geometry, akimbo (including the off-hand renderer), grenades, magazine and rocket item renderers, sound playback mechanics |
| [gun-sheets.md](gun-sheets.md) | One sheet per weapon: renderer, model, textures, every transform, animation formulas, muzzle FX, scope, sounds |
| [gun-models.md](gun-models.md) | Part breakdown, box sizes, moving, glow and conditional parts, and texture size of all 55 model classes plus `grenadelauncher.obj` |
| [textures.md](textures.md) | All 62 `textures/guns/` PNGs: size, what they depict, palette, UV layout, users, model-size mismatches |
| [sounds.md](sounds.md) | All 185 `sounds/guns/` files, their `sounds.json` events and exactly when each plays |

## Weapon overview

| Weapon (registry name) | Renderer | Model | Texture(s) | Fire / reload sound |
|---|---|---|---|---|
| Hand Cannon (`handcannon`) | RenderHandgun | ModelHandgun | handgun | handgunFire / handgunReload |
| Pistol (`pistol`) | RenderPistol | ModelPistol3 (2 parts) | pistol3 | pistolFire / pistolReload |
| Sawed-off (`sawedoff`) | RenderSawedoff | ModelSawedoff | sawedOff | sawedoffFire / sawedoffReload |
| Bolt-action (`boltaction`) | RenderBoltActionRifle (scope) | ModelBoltaction | boltactionrifle ×3 camo | boltactionFire + Rechamber / boltactionReload |
| M4 (`m4`) | RenderM4 | ModelM4 | m4Texture ×5 camo | assaultRifleFire / assaultRifleReload |
| M4 Infiltrator (`m4_uq`) | RenderM4_Infiltrator (scope) | ModelM4_UQ | m4_uq_Texture | silencedM4Fire / assaultRifleReload |
| Flamethrower (`flamethrower`) | RenderFlamethrower | ModelFlamethrower | Flamethrower | flamethrowerStart → Fire / flamethrowerReload |
| Combat Shotgun (`combatshotgun`) | RenderCombatShotgun | ModelCombatShotgun (2) | combatShotgun | combatshotgunFire + Pump / combatshotgunReload |
| Laser Gun (`lasergun`) | RenderLasergun | ModelLasergun | Lasergun | lasergunFire / lasergunReload |
| Blaster Rifle (`blasterRifle`) | RenderBlasterRifle (scope) | ModelBlasterRifle | blasterRifle | blasterrifleFire / lasergunReload |
| Rocket Launcher (`rocketlauncher`) | RenderRocketLauncher | ModelRocketLauncher | rocketlauncher | rocketFire / rocketReload |
| Nuke Launcher (`nukelauncher`) | RenderRocketLauncher | ModelRocketLauncher | nukerocketlauncher | rocketFire / rocketReload |
| Grim Reaper (`grimreaper`) | RenderGrimReaper | ModelGrimReaper | grimreaper | rocketFire / rocketReload |
| Bio Gun (`biogun`) | RenderBiogun | ModelBiogun | BioGun | biogunFire / biogunReload |
| Thompson (`thompson`) | RenderThompson | ModelThompson | thompson | thompsonFire / thompsonReload |
| Revolver (`revolver`) | RenderRevolver | ModelRevolver | revolver | revolverFire / revolverReload |
| Golden Revolver (`goldenrevolver`) | RenderRevolver | ModelGoldenRevolver | goldenrevolver | goldenrevolverFire / revolverReload |
| AK-47 (`ak47`) | RenderAK | ModelAK | ak47Texture ×2 camo | ak47Fire / ak47Reload |
| AS50 (`as50`) | RenderAS50 (scope) | ModelAS50 | as50Texture | as50Fire / as50Reload |
| Tesla Gun (`teslagun`) | RenderTeslaGun | ModelTeslaGun | TeslaGun | teslaFire / teslaReload |
| PDW (`pdw`) | RenderPDW | ModelPDW | PDW ×3 camo | pdwFire / pdwReload |
| LMG (`lmg`) | RenderLMG | ModelLMG | mg2_texture | lmgFire / lmgReload |
| Pulse Rifle (`pulserifle`) | RenderPulseRifle (tech scope) | ModelPulseRifle | pulseRifle ×3 camo | pulserifleFire / pulserifleReload |
| Alien Blaster (`alienBlaster`) | RenderAlienBlaster | ModelAlienBlaster | alien_blaster | alienblasterFire / alienblasterReload |
| Cyberdemon Blaster (`cyberdemonBlaster`) | RenderCyberdemonBlaster | ModelCyberdemonBlaster | cyberdemonblaster | cyberdemonblasterFire / teslaReload |
| Minigun (`minigun`) | RenderMinigun | ModelMinigun (2) | minigun | minigunFire / minigunReload |
| Power Hammer (`powerHammer`) / Adv. (`powerHammerAdv`) | RenderPowerHammer | ModelPowerHammer (2) | powerHammer / powerHammerAdv | powerhammerCharge, Fire, Swing, Impact, ImpactGround / powerhammerReload |
| Grenade Launcher (`grenadelauncher`) | RenderGrenadeLauncher | grenadelauncher.obj (2 groups) | grenadelauncher | grenadeLauncherFire / grenadeLauncherReload |
| AUG (`aug`) | RenderAUG (scope) | ModelAUG (2) | AugTexture ×5 camo | augFire / augReload |
| Sonic Shotgun (`sonicShotgun`) | RenderSonicShotgun | ModelSonicShotgun | sonicshotgun | sonicshotgunFire / sonicshotgunReload |
| Chainsaw (`chainsaw`) | RenderChainsaw | ModelChainsaw | chainsaw | chainsawLoopStart → Loop, Swing, Hit / powerhammerReload |
| Scatter Beam Gun (`scatterBeamGun`) | RenderLaserGunNew | ModelLaserGunNew | laserGunNew | lasergunFire / lasergunReload |
| Nuclear Death Ray (`beamtestgun`) | RenderBeamgun | ModelNDR | NDR | beamgunStart → Fire / lasergunReload |
| Mac-10 (`mac10`) | RenderMac10 | ModelMac10 | mac10texture | mac10Fire / mac10Reload |
| MIB Gun (`mibGun`) | RenderMibGun | ModelMibGun | mibgun | MIBblasterFire / MIBblasterReload |
| SCAR (`scar`) | RenderScar (scope) | ModelScar (2) | scar_texture ×2 camo | scarFire / scarReload |
| Vector (`vector`) | RenderVector | ModelVector | vector_texture ×2 camo | vectorFire / vectorReload |
| Akimbo Pistols (`pistolAkimbo`) | RenderPistolAkimbo | ModelAkimbo(Pistol3, 2) | pistol3 | pistolFire / pistolReloadAkimbo |
| Akimbo Revolvers (`revolverAkimbo`) | RenderRevolverAkimbo | ModelAkimbo(Revolver) | revolver | revolverFire / revolverReloadAkimbo |
| Akimbo Golden Revolvers (`goldenrevolverAkimbo`) | RenderRevolverAkimbo | ModelAkimbo(GoldenRevolver) | goldenrevolver | goldenrevolverFire / revolverReloadAkimbo |
| Akimbo Mac-10 (`mac10Akimbo`) | RenderMac10Akimbo | ModelAkimbo(Mac10) | mac10texture | mac10Fire / mac10ReloadAkimbo |
| Akimbo MIB (`mibGunAkimbo`) | RenderMibGunAkimbo | ModelAkimbo(MibGun) | mibgun | MIBblasterFire / MIBblasterReloadAkimbo |
| Stielhandgranate (`stielGranate`) | RenderStielgranate | ModelStielgranate | stielgranate | vanilla random.bow |
| Frag Grenade (`fragGrenade`) | RenderFragGrenade | ModelFragGrenade | fragGrenade_texture | grenade_pin / grenade_throw |
| Magazines and rockets (shared item metas) | RenderGenericItem → M4Mag / AS50Mag / LMGMag / RocketAmmo | ModelM4Mag(E), ModelAS50Mag, ModelLmgMag(E), ModelRocket | m4Texture, as50Texture, mg2_texture, rocketlauncher, nukerocketlauncher | – |

Texture names are `textures/guns/<name>.png`. Sound names are `techguns:guns.<name>`.

**Unused content shipped in the jar:** the Neutrino gun (renderer, model and texture; the item is never created), MP5, Driller, ModelPistol, ModelPistol2, ModelPistol3Akimbo, ModelSniperScope, RenderGun, RenderGunScoped, RenderM4Scoped, RenderGunMultipart, RenderDriller and RenderMP5. Textures `pistol.png`, `pistol2Texture.png`, `mp5Texture.png`, `drillTexture.png`, `neutrinogun.png`. Sound events `guns.minigunStart`, `guns.chainsawLoopEnd`, `guns.pulserifleFireStereo`, `effects.biogunImpact`, and 4 files not in sounds.json (see sounds.md).

## Lang keys in scope

Only the camo names and the generic camo strings are in scope. They are listed in [rendering-framework.md §8](rendering-framework.md#8-textures-and-camouflage): `techguns.item.{boltaction,m4,ak47,pdw,pulseRifle,aug,scar,vector}.camoname.<i>`, `techguns.item.defaultcamo`, `techguns.item.invalidcamo`, `techguns.tooltip.currentcamo`, `techguns.message.camoswitch`. No tooltip lines are generated by renderers or models. Gun names, zoom tooltips (`techguns.gun.tooltip.zoom*`) and stat tooltips belong to 02-guns.

## Source files covered

* `java/techguns/client/renderer/item/`: all 56 non-armour, non-block renderers (RenderGunBase, RenderGun, RenderGunMultipart, RenderGunScoped, RenderGunAkimbo, RenderGrenade, RenderGenericItem, RenderRocketAmmo, RenderM4Mag, RenderAS50Mag, RenderLMGMag and every per-gun renderer, including unused ones).
* `java/techguns/client/models/`: all 55 weapon, base, magazine and grenade model classes.
* Supporting files read for behaviour (owned elsewhere): ClientProxy (registration, timing state, camo textures), ScreenEffect, TGRenderHelper, TGuns (texture and sound wiring), GenericGun, GenericGunCharge, PowerHammer, Chainsaw, grenades, SoundUtil, TGSound, PacketPlaySound, GunFiredMessage, ReloadStartedMessage, TechgunsEventhandler and TechgunsTickHandler (zoom, aimed pose, off-hand akimbo), and the additionalslots RenderAkimboGun family.

---

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| GVA-01 | Renderer registered for a non-existent item | java/techguns/client/ClientProxy.java:528 | `TGuns.neutrinoGun` is never created (its construction and registration are commented out in TGuns.java:154 and :211), but a RenderNeutrinoGun is still registered against the null field. Forge stores it under a null key. It is dead, and only harmless while the map tolerates null keys. | cosmetic |
| GVA-02 | Debug keybind offsets leak into release rendering | java/techguns/client/renderer/item/RenderVector.java:73; java/techguns/client/renderer/item/RenderFragGrenade.java:34; java/techguns/client/renderer/additionalslots/RenderAkimboMibGun.java:17 | The Vector muzzle flash, the frag grenade's third-person transform and the off-hand MIB transform add `Keybinds.X/Y/Z` (debug tuning values). With the debug keybinds config on, numpad presses move these in game. | visual |
| GVA-03 | Own recoil animation restarted by the server echo | java/techguns/packets/GunFiredMessage.java:70 | `GunFiredMessage` is sent to everyone, including the shooter. The shooter's client already started its recoil locally, and the echo resets the recoil window again (unless the gun uses checkRecoil). With any latency the first-person kick visibly restarts, most noticeably on long recoils (bolt-action 750 ms, combat shotgun 600 ms). | visual |
| GVA-04 | Additive blend function leaks | java/techguns/client/renderer/TGRenderHelper.java:37 (also RenderCyberdemonBlaster `drawMuzzleFX`) | `enableAlphaBlendAdditive` sets `glBlendFunc(ONE, ONE)` but `disableAlphaBlend` only pops ENABLE_BIT, which does not save the blend function. Anything rendered later that enables blending without setting its own function becomes additive. | visual |
| GVA-05 | Full-bright lightmap not restored | java/techguns/client/renderer/TGRenderHelper.java:20 | `enableFXLighting` forces lightmap 240/240, and `disableFXLighting` only re-enables lighting. Geometry drawn after glow parts or muzzle flashes in the same pass can stay full bright. | visual |
| GVA-06 | Grim Reaper model rotates without push/pop | java/techguns/client/models/ModelGrimReaper.java:441 | `render` applies rotY(180) to the caller's matrix. It is contained only because RenderGunBase pops right after. Any other caller inherits the rotation. | cosmetic |
| GVA-07 | NDR / Neutrino idle offset applied twice | java/techguns/client/renderer/item/RenderBeamgun.java:24 (and :53) | `GL_Transform_FirstPerson` already calls the idle translate, and the state branch translates again (idle, firing and reload all do). The resulting placement is double the declared offset. It may be tuned around, but it is inconsistent with every other renderer. | cosmetic |
| GVA-08 | Akimbo guns drawn twice in the right hand in third person | java/techguns/client/renderer/item/RenderGunBase.java:262 with RenderPistolAkimbo.java:33 (and the other akimbo renderers) | The base loops over all parts of both guns in EQUIPPED, and the akimbo equipped transforms ignore `leftGun`. The second gun is therefore rendered exactly on top of the first, while the real off-hand gun is drawn separately by RenderAkimboGun. Wasted draw, and z-fighting if transforms ever differ. | cosmetic |
| GVA-09 | Chainsaw third-person recoil is dead code | java/techguns/client/renderer/item/RenderChainsaw.java:108 | The recoil only moves `part == 1`, but the chainsaw renderer has 1 part. The attackType ≠ 0 lunge branch of the first-person firing transform is also unreachable (chainsaw recoils always use attackType 0). | cosmetic |
| GVA-10 | Renderer texture arguments are ignored or wrong | java/techguns/client/ClientProxy.java:505, :507 | The live base class never uses the constructor texture. The arguments passed are misleading: the scatter beam gets `LasergunNew.png` (the file is `laserGunNew.png`) and the NDR gets `Lasergun.png`. Harmless today, but any code that switches to the argument would break. | cosmetic |
| GVA-11 | M4 and Vector define scopes that can never show | java/techguns/client/ClientProxy.java:448, :532 | RenderM4 (for the M4) and RenderVector are built with `hasZoom = false`, so their sniper `renderScope()` is unreachable even though both guns zoom (0.75). Possibly intentional (iron or red-dot sights), but inconsistent with the M4 Infiltrator, AUG and SCAR. | cosmetic |
| GVA-12 | Negative UV origins wrap to the far texture edge | java/techguns/client/models/ModelLasergun.java:201; ModelLaserGunNew.java:299; ModelPDW.java:219 | `Glow1` (−6, 47), `TopRails` (−1, 90) and `Shape28` (−1, 48) start outside the sheet, so a column of their faces samples pixels from the opposite edge. (The M4 Infiltrator laser at (0, −60) is a deliberate trick and not a bug.) | visual |
| GVA-13 | Duplicate render call | java/techguns/client/models/ModelLaserGunNew.java:333, :338 | `Stock10` is rendered twice per frame. | cosmetic |
| GVA-14 | LMG belt shows one round too few | java/techguns/client/models/ModelLMG.java:406 | Bullet *k* is drawn only if ammoLeft > *k*, so with 1 round left no belt round is visible (the belt shows ammo − 1 rounds, capped at 6). Possibly off-by-one. | cosmetic |
| GVA-15 | Grenade launcher drum does not index a full chamber | java/techguns/client/renderer/item/RenderGrenadeLauncher.java:81 | The drum rotation 60·y2 runs from about 8.8° to 51° over the recoil window, then snaps back to 0°. It never completes the 60° step expected of a 6-shot drum, so it visibly jumps at the start and end of each shot. | visual |
| GVA-16 | Flamethrower pilot flame hidden under low ceilings | java/techguns/client/renderer/item/RenderFlamethrower.java:97 | The check `isOffsetPositionInLiquid(0, 1.5, 0)` is also false when the offset box collides with blocks, so the pilot flame vanishes whenever there is a block about 1.5 m above the player, not just underwater. | visual |
| GVA-17 | Melee "guns" use the aimed-bow arm pose | java/techguns/events/TechgunsEventhandler.java:404 | Every GenericGun (including the Chainsaw and Power Hammers) forces `aimedBow` on the player model, and RenderGunBase always applies the aimed pre-rotation for players. Melee weapons are therefore held out like rifles in third person. | visual |
| GVA-18 | Camo index is not bounds-checked when rendering | java/techguns/client/renderer/item/RenderGunBase.java:345 | `textures.get(camo)` uses the raw NBT byte. A stack whose `camo` tag is ≥ the gun's texture count (NBT edit, command, or a texture list shortened in an update) throws IndexOutOfBoundsException every frame. | crash |
| GVA-19 | Single global muzzle-flash timer | java/techguns/client/renderer/item/RenderGunBase.java:251 | `player_muzzleFlashtime` is not tied to a weapon. Switching items during a flash window draws the flash on the newly held gun. | cosmetic |
| GVA-20 | Dead or unused sound events and files | resources/assets/techguns/sounds.json; java/techguns/TGuns.java:137 | `guns.minigunStart` (setter commented out), `guns.chainsawLoopEnd` and `guns.pulserifleFireStereo` (never referenced), `effects.biogunImpact` (call commented out). `beamgun_reload`, `chainsaw_loop_d`, `minigun_start_b/c` are not in sounds.json at all. | cosmetic |
| GVA-21 | Placeholder or duplicate audio | resources/assets/techguns/sounds/guns/ | `chainsaw_loopstart`, `chainsaw_loopend` and `chainsaw_loop_d` are byte-identical 10 KB files. `chainsaw_hit_a`–`f` are six copies of one sound, so the random variation does nothing. | cosmetic |
| GVA-22 | Inconsistent sound category | resources/assets/techguns/sounds.json (`guns.chainsawLoopStart`) | It is the only gun event in category `master` (all others are `player`), so it ignores the Players volume slider. | cosmetic |
