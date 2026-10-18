# Per-gun visual sheets

One sheet per hand-held weapon: model, textures, every render transform, animations, muzzle flash, scope and sounds. Read [rendering-framework.md](rendering-framework.md) first. It defines the frame order, the curves (`yF`, `yR`, `sin`, `P66`, `P75`, `P50`, `K`, `Kfull`, `D30`, `B`, `w`, `tri`), the pivot notation and the muzzle frame.

**Sheet legend**

| Row | Meaning |
|---|---|
| Item | `GameRegistry` name / unlocalized name (`techguns.item.<name>`, display strings in area [02](../02-guns/index.md)) |
| Timing | animation windows, in ticks (ms = ticks × 50): recoil, muzzle flash, reload |
| FP base | first-person base transform, applied every frame before the state transform |
| FP idle | "Translate" transform used when idle, after reload or after recoil |
| FP fire | first-person firing transform with p = recoil progress (replaces idle) |
| FP reload | first-person reload transform with p = reload progress (replaces idle) |
| 3P base | third-person equipped transform (after the aimed pre-rotation for players) |
| 3P aim | extra translate inside the aimed pre-rotation (framework §6.3). "–" = none |
| 3P recoil / 3P reload | third-person animation with p = recoil / reload progress |
| Ground / Icon | dropped-item and inventory transforms |
| Muzzle FX | first-person ScreenEffect: sheet, offset (x, y, z), scale, extra rotation, blend. Effective offset = scale × offset (framework §7) |
| 3P flash | muzzle-flash particle index used for other viewers (area [04](../04-projectiles-fx/index.md)): 1 = MuzzleFlashYellow, 2 = MuzzleFlashExp, 3 = MuzzleFlashBlue, 4 = MuzzleFlashLaser, 5 = MuzzleFlashAlienBlue, – = none. The offset (x, y, z) is from `setMuzzleParticle`. |
| Sounds | `techguns:` event names (see [sounds.md](sounds.md)) |

Transforms are listed in GL call order. "rotX180" = rotX(180), and so on. Unless a sheet says otherwise, the 3P recoil and 3P reload are applied about the gun's current origin.

---

## Hand Cannon (`handcannon`)

| | |
|---|---|
| Item | [`handcannon`](../02-guns/gun-catalogue.md#table-a-identity-and-ammunition) (class Handgun, single shot, crafted from a stone barrel and wood stock) |
| Renderer / model | RenderHandgun / ModelHandgun (1 part) |
| Texture | textures/guns/handgun.png |
| Timing | recoil 5, flash 5, reload 30 |
| FP base | rotX180, rotY−45, S(2.5) |
| FP idle | T(0.1, 0, −0.5) |
| FP fire | rotZ(30·yF), T(0.1 + yF, 0.5·yF, −0.5): the barrel tips up and the gun pulls back |
| FP reload | s = sin(p): rotZ(70·s), T(0.1 + s, −s/2, −0.5): swings far up and out, then back |
| 3P base | rotX180, rotY225, S(1.5), T(−0.3, −0.45, 0) |
| 3P aim | T(0, −0.1, 0) |
| 3P recoil | about pivot (−0.2, −0.1, 0): rotZ(−40·K) |
| 3P reload | about pivot (−0.2, −0.1, 0): rotZ(−40·yR) |
| Ground / Icon | rotX180, rotY90 / rotX180, rotY180, S(0.7) |
| Muzzle FX | muzzleFlash, (−0.75, −2.5, 1.2), scale 1.85, default blend |
| 3P flash | 2, (0, 0, 0.2) |
| Sounds | fire `guns.handgunFire`, reload `guns.handgunReload` |

## Pistol (`pistol`)

| | |
|---|---|
| Renderer / model | RenderPistol / ModelPistol3, 2 parts (0 = frame, 1 = slide) |
| Texture | textures/guns/pistol3.png (128×64 PNG for a 64×32 model) |
| Timing | recoil 3, flash 5, reload 35 |
| FP base | rotX180, rotY225, S(2.0) |
| FP idle | T(−0.55, −0.45, −0.8) |
| FP fire | rotX(10·yF), T(−0.55, −0.45, −0.8 − yF·(0.3 + 0.2·part)): the slide (part 1) travels 0.5·tri back, the frame 0.3·tri |
| FP reload | rotX(−50·yR), T(−0.55, −0.45, −0.8): tilts the muzzle up to 50° and back |
| 3P base | rotX180, rotY135, S(1.5), T(0.05, −0.55, 0.2) |
| 3P aim | T(0, 0, 0.2) |
| 3P recoil | about pivot (0, 0, 0.9): rotX(−15·K). Part 1 also gets T(0, 0, 0.15·2p) while p < 0.5 (slide back) |
| 3P reload | about pivot (0, 0.3, 0.9): rotX(−40·yR) |
| Ground / Icon | rotX180, T(0, −0.35, −0.3) / rotX180, S(1.5), rotY90, T(0.3, −0.5, 0) |
| Muzzle FX | muzzleFlash2, (−0.65, −0.75, 1.55), scale 1.0, default |
| 3P flash | 1, (0, 0.05, −0.45) |
| Sounds | `guns.pistolFire`, `guns.pistolReload` |

## Sawed-off (`sawedoff`)

| | |
|---|---|
| Renderer / model | RenderSawedoff / ModelSawedoff |
| Texture | textures/guns/sawedOff.png |
| Timing | recoil 5, flash 5, reload 28 |
| FP base | rotX180, rotY−45, S(2.2) |
| FP idle | T(0.1, −0.1, −0.5) |
| FP fire | rotZ(30·yF), T(0.1 + 0.5·yF, −0.1 + 0.3·yF, −0.5) |
| FP reload | s = sin(p): rotZ(70·s), T(0.1 + s, −0.1 − s/2, −0.5) |
| 3P base | rotX180, rotY225, S(1.5), T(−0.5, −0.25, 0) |
| 3P aim | T(0, 0.15, 0) |
| 3P recoil / reload | about pivot (−0.2, −0.1, 0): rotZ(−40·K) / rotZ(−40·yR) |
| Ground / Icon | rotX180, rotY90 / rotX180, rotY180, S(0.7) |
| Muzzle FX | muzzleFlash, (−0.55, −2.15, 1.05), scale 1.85, default |
| 3P flash | 2, z 0.1 |
| Sounds | `guns.sawedoffFire`, `guns.sawedoffReload` |

## Bolt-action Rifle (`boltaction`)

| | |
|---|---|
| Renderer / model | RenderBoltActionRifle (hasZoom) / ModelBoltaction |
| Textures (camo) | textures/guns/boltactionrifle.png, _1 (Olive), _2 (Tactical Black) |
| Timing | recoil 15 (750 ms, of which ¾ is rechambering), flash 5, reload 50 |
| FP base | rotX180, rotY−45, S(1.5), T(−0.09, −0.37, 0.07) |
| FP idle | T(0.1, −0.25, −0.5) |
| FP fire | **p < 0.25 (kick):** y = yF(4p): rotZ(25·y), T(0.1 + y, −0.25, −0.5). **p ≥ 0.25 (rechamber):** s = sin((p − 0.25)·1.333): rotZ(4·s), rotY(−16·s), rotX(−4·s), T(0.1 + 0.25·s, −0.25 + 0.05·s, −0.5 + 0.05·s). The gun cants and pulls in as if the bolt is worked |
| FP reload | y = yR: rotZ(−10·y), rotY(50·y), rotX(10·y), T(0.1 − y, −0.25 − 0.5·y, −0.5 − 0.25·y) |
| 3P base | rotX180, rotY225, S(1.5), T(−0.65, −0.25, 0) |
| 3P aim | T(0, 0.2, −0.25) |
| 3P recoil | about pivot (0, 0.3, −0.9): rotZ(−20·K) |
| 3P reload | rotX(30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, rotY90, S(0.9), T(0, −0.25, 0) / rotX180, rotY180, S(0.6), T(−0.4, 0.1, 0) |
| Muzzle FX | muzzleFlash2, (−0.4, −3.0, 1.15), scale 1.85, default |
| Scope | zoom 0.35 (toggle). Sniper overlay, default scope recoil |
| 3P flash | 1, (0, 0.05, 0.3) |
| Sounds | fire `guns.boltactionFire`, rechamber `guns.boltactionRechamber` (with every shot, volume 1), reload `guns.boltactionReload` |

## M4 (`m4`)

| | |
|---|---|
| Renderer / model | RenderM4 (handleDamage false, hasZoom false) / ModelM4 |
| Textures (camo) | textures/guns/m4Texture.png, _1 Swamp, _2 Desert, _3 Ice, _4 Hazard |
| Timing | recoil 5, flash 5, reload 45 |
| FP base | rotX180, rotY225, S(1.4) |
| FP idle | T(−0.55, −0.35, −0.65) |
| FP fire | rotX(10·yF), T(−0.55, −0.35, −0.65 − 0.3·yF) |
| FP reload | rotX(−50·yR), then idle |
| 3P base | rotX180, rotY135, S(0.8), T(0.1, −0.6, 1.1) |
| 3P aim | T(0, 0.1, 0) |
| 3P recoil | rotX(−15·K) |
| 3P reload | about pivot (0, −0.2, 0.05): rotX(−30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, S(0.6), T(0, −0.45, 0) / rotX180, S(0.48), T(0.3, 0, 0), rotY90 |
| Muzzle FX | muzzleFlash2, (−0.48, −3.4, 1.10), scale 1.5, default |
| Scope | zoom 0.75 (toggle), **FOV only**: the model stays visible (its EOTech is the sight). A `renderScope` is defined but unreachable |
| 3P flash | 1, (0, 0.025, 0) |
| Sounds | `guns.assaultRifleFire`, `guns.assaultRifleReload` |

## M4 Infiltrator (`m4_uq`)

Same transforms as the M4, with these differences:

| | |
|---|---|
| Renderer / model | RenderM4_Infiltrator (handleDamage **true**, hasZoom **true**) / ModelM4_UQ |
| Texture | textures/guns/m4_uq_Texture.png |
| Laser | twin laser-beam planes visible in every view except the inventory icon |
| Muzzle FX | **none** (silenced) |
| Scope | zoom 0.5. Sniper overlay, scope recoil (0.1, 0.15, 0.05) |
| 3P flash | none |
| Sounds | fire `guns.silencedM4Fire`, reload `guns.assaultRifleReload` |

## Flamethrower (`flamethrower`)

| | |
|---|---|
| Renderer / model | RenderFlamethrower / ModelFlamethrower |
| Texture | textures/guns/Flamethrower.png |
| Timing | recoil 5, flash 5, reload 45. Fires every 2 ticks |
| FP base | rotX180, rotY−45, S(2.0) |
| FP idle | T(0.55, −0.3, −0.7) |
| FP fire | identical to idle (no recoil motion) |
| FP reload | s = P66(p): rotZ(20·s), rotY(−10·s), T(0.5·s, 0.25·s, 0.25·s), then idle |
| 3P base | rotX180, rotY225, S(1.5), T(−0.8, −0.6, 0) |
| 3P aim | T(0, −0.15, −0.4) |
| 3P recoil | none |
| 3P reload | rotX(30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, rotY90, S(0.85), T(0, −0.45, 0) / rotX180, rotY180, S(0.7), T(−0.3, 0, 0) |
| Muzzle FX | none. **Pilot flame** extra effect instead (framework §7) |
| 3P flash | none. The flame stream itself is particles (area [04](../04-projectiles-fx/index.md)) |
| Sounds | looping fire: the first shot after ≥ 10 ticks of silence plays `guns.flamethrowerStart`, later shots `guns.flamethrowerFire` (framework §14). Reload `guns.flamethrowerReload` |

## Combat Shotgun (`combatshotgun`)

| | |
|---|---|
| Renderer / model | RenderCombatShotgun / ModelCombatShotgun, 2 parts (0 = gun, 1 = pump) |
| Texture | textures/guns/combatShotgun.png |
| Timing | recoil 12 (600 ms: kick, then pump), flash 5, reload 50 |
| FP base | rotX180, rotY−45, S(2.25) |
| FP idle | T(0.1, −0.25, −0.5) |
| FP fire | **p < 0.25:** y = yF(4p): rotZ(20·y), T(0.1 + 1.2·y, −0.25, −0.5). **p ≥ 0.25 (pump):** s = sin((p − 0.25)·1.333): rotZ(8·s), rotY(−18·s), rotX(−6·s), then for the gun T(0.1 + 0.5·s, −0.25 + 0.05·s, −0.5 + 0.05·s), for the pump T(0.1 − 0.1·s, …), so the pump racks 0.6·s back relative to the gun |
| FP reload | **p < 0.8:** s = P66(p/0.8): rotZ(10·s), rotY(−50·s), rotX(−10·s), T(0.1 + s, −0.25 + 0.5·s, −0.5 + 0.25·s): turns the loading port towards the camera. **p ≥ 0.8:** the pump-rack motion with s = sin((p − 0.8)/0.2) |
| 3P base | rotX180, rotY225, S(1.15), T(−0.8, −0.40, 0) |
| 3P aim | T(0, 0.25, −0.25) |
| 3P recoil | p < 0.5: rotZ(−20·K). p ≥ 0.5: the pump only, T(0.45·yR(2(p − 0.5)), 0, 0) |
| 3P reload | pump only: if p > 0.8, T(−0.5·p/0.8, 0, 0). Then all parts rotX(30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, rotY90, S(0.75), T(0, −0.30, 0) / rotX180, rotY180, S(0.6), T(−0.3, 0.1, 0) |
| Muzzle FX | muzzleFlash, (−0.4, −2.5, 0.7), scale 1.85, default |
| 3P flash | 2, z 0.1 |
| Sounds | fire `guns.combatshotgunFire`, pump `guns.combatshotgunPump` (with every shot), reload `guns.combatshotgunReload` |

## Laser Gun (`lasergun`)

| | |
|---|---|
| Renderer / model | RenderLasergun / ModelLasergun |
| Texture | textures/guns/Lasergun.png |
| Timing | recoil 5, flash 5, reload 45 |
| FP base | rotX180, rotY−45, S(1.3), T(0, −0.5, 0) |
| FP idle | T(0.1, −0.25, −0.5) |
| FP fire | T(0.1 + 0.5·yF, −0.25, −0.5): straight push back, no rotation |
| FP reload | s = P66(p): rotZ(20·s), rotY(−10·s), T(0.1 + 0.5·s, −0.25 + 0.25·s, −0.5 + 0.25·s) |
| 3P base | rotX180, rotY225, S(1.25), T(−0.7, −0.5, 0) |
| 3P aim | T(0, 0.1, −0.2) |
| 3P recoil | T(−0.25·K, 0, 0): slides along the barrel axis |
| 3P reload | about pivot (−0.2, −0.1, 0): rotZ(−40·yR), rotY(50·yR) |
| Ground / Icon | rotX180, rotY90, T(0, −0.35, 0), S(0.75) / rotX180, rotY180, T(−0.1, 0, 0), S(0.55) |
| Muzzle FX | muzzleFlashLaser, (−0.65, −1.0, −0.30), scale 1.5, rotY90, additive + alpha test |
| Scope | zoom 0.75, FOV only |
| 3P flash | none (the laser beam is the effect, area [04](../04-projectiles-fx/index.md)) |
| Sounds | `guns.lasergunFire`, `guns.lasergunReload` |

## Blaster Rifle (`blasterRifle`)

| | |
|---|---|
| Renderer / model | RenderBlasterRifle (hasZoom) / ModelBlasterRifle |
| Texture | textures/guns/blasterRifle.png (256×128 for a 128×64 model) |
| Timing | recoil 5, flash 5, reload 45 |
| FP base | rotX180, rotY225, S(1.4), T(0.1, −0.1, 0) |
| FP idle / fire / reload | as M4: idle T(−0.55, −0.35, −0.65). Fire rotX(10·yF), T(…, −0.65 − 0.3·yF). Reload rotX(−50·yR) then idle |
| 3P base | rotX180, rotY135, S(0.95), T(0.1, −0.5, 0.9) |
| 3P aim | T(0, 0.05, 0.2) |
| 3P recoil / reload | rotX(−15·K) / about pivot (0, −0.2, 0.05): rotX(−30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, S(0.65), T(0, −0.45, 0) / rotX180, S(0.48), T(0.3, 0, 0), rotY90 |
| Muzzle FX | muzzleFlashLaser, (−1.05, −2.8, −0.45), scale 1.5, rotY90, additive + alpha test |
| Scope | zoom 0.5. Sniper overlay, default scope recoil |
| 3P flash | 4 (laser), (0, −0.1, 0.1). Turret offset (0, 0, −0.4) |
| Sounds | fire `guns.blasterrifleFire`, reload `guns.lasergunReload` |

## Rocket Launcher (`rocketlauncher`) and Nuke Launcher (`nukelauncher`)

| | |
|---|---|
| Renderer / model | RenderRocketLauncher (handleDamage) / ModelRocketLauncher |
| Texture | rocketlauncher.png / nukerocketlauncher.png |
| Timing | recoil 10, flash 5, reload 40 |
| FP base | rotX180, rotY−45, S(2.25) |
| FP idle | T(0.15, −0.25, −0.5) |
| FP fire | T(0.1 + 0.75·yF, −0.25, −0.5), rotZ(25·yF) |
| FP reload | s = P66(p): rotZ(40·s), rotY(−20·s), T(0.1 + 0.5·s, −0.25 + 0.15·s, −0.5 + 0.15·s) |
| 3P base | rotX180, rotY225, S(1.5), T(−1.2, −1.8, 0) (shoulder-mounted) |
| 3P aim | T(0, −1.8, 0.2) |
| 3P recoil | about pivot (0, 0.3, −0.9): rotZ(−20·K) |
| 3P reload | rotX(40·yR), rotY(60·yR) |
| Ground / Icon | rotX180, rotY90, T(0, −0.25, 0) / rotX180, rotY180, S(0.68), T(−0.3, 0.3, 0) |
| Ammo visual | the rocket in the tube shows only when loaded, and only from 50 % of a reload onwards |
| Muzzle FX / 3P flash | none / none (the rocket and exhaust are area [04](../04-projectiles-fx/index.md)) |
| Sounds | `guns.rocketFire`, `guns.rocketReload` (both launchers) |

## Grim Reaper (`grimreaper`)

| | |
|---|---|
| Renderer / model | RenderGrimReaper (handleDamage false) / ModelGrimReaper (internally rotY180) |
| Texture | textures/guns/grimreaper.png |
| Timing | recoil 10, flash 5, reload 40 |
| FP base | rotX180, rotY−45, S(2.1) |
| FP idle | T(0.25, −0.65, −0.6) |
| FP fire | T(0.25 + 0.75·yF, −0.65, −0.6), rotZ(25·yF) |
| FP reload | s = P66(p): rotZ(60·s), rotY(−30·s), T(0.25 − 0.33·s, −0.65 + 0.33·s, −0.6 − 0.25·s) |
| 3P base | rotX180, rotY225, S(1.5), T(−0.9, −1.8, 0) |
| 3P aim | T(0, −1.5, −0.4) |
| 3P recoil / reload | about pivot (0, 0.3, −0.9): rotZ(−20·K) / rotX(40·yR), rotY(60·yR) |
| Ground / Icon | rotX180, rotY90, T(0, −0.25, 0) / rotX180, rotY180, S(0.5), T(0.2, 0.1, 0.1) |
| Muzzle FX / 3P flash | none / none |
| Sounds | `guns.rocketFire`, `guns.rocketReload` |

## Bio Gun (`biogun`)

| | |
|---|---|
| Renderer / model | RenderBiogun (handleProgress) / ModelBiogun (glow parts) |
| Texture | textures/guns/BioGun.png |
| Timing | charge gun: full charge 30 ticks. Recoil 5, flash 5, reload 45 |
| FP base | rotX180, rotY−45, S(2.0), T(−0.25·charge, 0, 0): the gun draws back as it charges |
| FP idle | T(0, −0.35, −0.4) |
| FP fire | T(0.5·yF, −0.35, −0.4), rotZ(15·yF) |
| FP reload | s = P66(p): rotZ(20·s), rotY(−10·s), T(0.5·s, −0.35 + 0.25·s, −0.4 + 0.25·s) |
| 3P base | rotX180, rotY225, S(1.5), T(−0.35, −0.7, 0) |
| 3P aim | T(0, 0, −0.15) |
| 3P recoil | while p < 0.5, about pivot (0, 0.3, −0.9): rotZ(−30·K) |
| 3P reload | rotX(30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, rotY90, T(0.30, −0.65, 0) / rotX180, rotY180, S(0.8), T(0.2, 0.1, 0) |
| Muzzle FX / 3P flash | none / none |
| Sounds | fire `guns.biogunFire` on release, reload `guns.biogunReload`: both vanilla positional at volume 1 (charge-gun path). Impact `effects.biogunImpact` is commented out (unused) |

## Thompson (`thompson`)

| | |
|---|---|
| Renderer / model | RenderThompson / ModelThompson |
| Texture | textures/guns/thompson.png |
| Timing | recoil 5, flash 5, reload 40 |
| FP base | rotX180, rotY−45, S(1.8) |
| FP idle | T(0.1, −0.35, −0.5) |
| FP fire | rotZ(10·yF), T(0.1 + 0.5·yF, −0.35, −0.5) |
| FP reload | s = P66(p): rotZ(25·s), rotY(−15·s), T(0.1 + 0.5·s, −0.35 + 0.25·s, −0.5 + 0.25·s) |
| 3P base | rotX180, rotY225, S(1.15), T(−0.85, −0.6, 0) |
| 3P aim | – |
| 3P recoil / reload | about pivot (0, 0.3, −0.9): rotZ(−20·K) / rotX(30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, rotY90, S(0.8), T(0, −0.45, 0) / rotX180, rotY180, S(0.65), T(−0.25, 0.1, 0) |
| Muzzle FX | muzzleFlash2, (−0.5, −2.0, 1.0), scale 1.5, default |
| 3P flash | 1, (0, 0.05, 0) |
| Sounds | `guns.thompsonFire`, `guns.thompsonReload` |

## Revolver (`revolver`) and Golden Revolver (`goldenrevolver`)

| | |
|---|---|
| Renderer / model | RenderRevolver / ModelRevolver or ModelGoldenRevolver |
| Texture | revolver.png / goldenrevolver.png |
| Timing | recoil 6 (golden 8), flash 5, reload 45 |
| FP base | rotX180, rotY−45, S(1.5) |
| FP idle | T(0.20, −0.5, −0.45) |
| FP fire | rotZ(35·yF), T(0.2 + 0.5·yF, −0.5 + 0.4·yF, −0.45): a strong muzzle flip |
| FP reload | s = P66(p): rotZ(25·s), rotY(−20·s), T(0.2 + 0.6·s, −0.5 + 0.3·s, −0.45 + 0.25·s) |
| 3P base | rotX180, rotY225, S(0.85), T(−1.4, −0.8, 0) |
| 3P aim | T(0, −0.05, 0.2) |
| 3P recoil | while p < 0.5, about pivot (0, 0.25, 1.0): rotZ(−30·K) |
| 3P reload | about pivot (0, 0.25, 1.0): rotZ(−40·yR) |
| Ground / Icon | rotX180, rotY90, T(0, −0.35, 0), S(0.8) / rotX180, rotY180, S(1.0), T(−0.5, −0.2, 0) |
| Muzzle FX | muzzleFlash, (−0.4, −2.5, 0.7), scale 1.85, default |
| 3P flash | 2, (0, 0.05, −0.45) (golden: z −0.4) |
| Sounds | fire `guns.revolverFire` / `guns.goldenrevolverFire`, reload `guns.revolverReload` (both) |

## AK-47 (`ak47`)

| | |
|---|---|
| Renderer / model | RenderAK / ModelAK |
| Textures (camo) | ak47Texture.png, _1 (Blue) |
| Timing | recoil 5, flash 5, reload 45 |
| FP base / idle | rotX180, rotY225, S(1.4) / T(−0.55, −0.35, −0.65) |
| FP fire / reload | rotX(10·yF), T(…, −0.65 − 0.3·yF) / rotX(−50·yR), then idle |
| 3P base | rotX180, rotY135, S(0.7), T(0.1, −0.8, 1.35) |
| 3P aim | – |
| 3P recoil / reload | rotX(−15·K) / rotX(−30·yR), rotY(40·yR) (no pivot) |
| Ground / Icon | rotX180, S(0.45), T(0, −0.45, 0) / rotX180, S(0.33), T(0.35, 0, 0), rotY90 |
| Muzzle FX | muzzleFlash2, (−0.38, −3.4, 0.95), scale 1.5, default |
| 3P flash | 1, (0, 0.05, 0.12) |
| Sounds | `guns.ak47Fire`, `guns.ak47Reload` |

## AS50 (`as50`)

| | |
|---|---|
| Renderer / model | RenderAS50 (hasZoom) / ModelAS50 |
| Texture | textures/guns/as50Texture.png |
| Timing | recoil 10, flash 5, reload 80 |
| FP base / idle | rotX180, rotY225, S(1.4) / T(−0.55, −0.35, −0.65) |
| FP fire | rotX(12·yF), T(−0.55, −0.35, −0.65 − 0.5·yF) |
| FP reload | s = P66(p): rotX(30·s), rotY(−15·s), T(−0.55, −0.35 + 0.5·s, −0.65): drops and tilts to swap the magazine |
| 3P base | rotX180, rotY135, S(0.7), T(0.1, −0.5, 1.35) |
| 3P aim | T(0, 0.2, 0) |
| 3P recoil / reload | rotX(−15·K) / rotX(−30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, S(0.55), T(0, −0.45, 0) / rotX180, S(0.33), T(0.35, 0.25, 0.5), rotY90 |
| Muzzle FX | muzzleFlash2, (−0.28, −4.0, 0.93), scale **2.0**, default |
| Scope | zoom 0.35. Sniper overlay, scope recoil (0.25, 0.5, 0.1): the strongest kick |
| 3P flash | 1, (0, 0.05, 0.3) |
| Sounds | `guns.as50Fire`, `guns.as50Reload` |

## Tesla Gun (`teslagun`)

| | |
|---|---|
| Renderer / model | RenderTeslaGun / ModelTeslaGun (glowing studs) |
| Texture | textures/guns/TeslaGun.png |
| Timing | recoil 5, flash **10**, reload 45 |
| FP base | rotX180, rotY−45, S(1.3), T(0, −0.5, 0) |
| FP idle / fire | T(0.1, −0.25, −0.5) / T(0.1 + 0.5·yF, −0.25, −0.5) |
| FP reload | s = P66(p): rotZ(20·s), rotY(−10·s), T(0.1 + 0.5·s, −0.25 + 0.25·s, −0.5 + 0.25·s) |
| 3P base | rotX180, rotY225, S(1.25), T(−0.7, −0.5, 0) |
| 3P aim | T(0, 0.2, −0.55) |
| 3P recoil | base default: rotZ(−30·D30) |
| 3P reload | none |
| Ground / Icon | rotX180, rotY90, T(0.3, −0.35, 0), S(0.8) / rotX180, rotY180, T(−0.1, 0, 0), S(0.55) |
| Muzzle FX | muzzleFlashLightning, (−0.65, −1.0, −0.30), scale 1.75, rotY90, additive |
| Scope | zoom 0.75, FOV only |
| 3P flash | none |
| Sounds | `guns.teslaFire`, `guns.teslaReload` |

## PDW (`pdw`)

| | |
|---|---|
| Renderer / model | RenderPDW / ModelPDW |
| Textures (camo) | PDW.png, _1 (Silver), _2 (Dark Green) |
| Timing | recoil 5 (checkRecoil: a new recoil does not restart while one is running in first person), flash 5, reload 40. 2-round burst |
| FP base / idle | rotX180, rotY−45, S(2.1) / T(0.70, −0.30, −0.6) |
| FP fire | fold p into [0, 0.5) (p − 0.5 if p > 0.5), y = 0.25 − \|p − 0.25\|: rotZ(−10·y), T(0.7 − 0.5·y, −0.3, −0.6). Two small bumps per window, one per burst round |
| FP reload | s = P66(p): rotZ(25·s), rotY(−15·s), T(0.7 + 0.5·s, −0.3 + 0.25·s, −0.6 + 0.25·s) |
| 3P base | rotX180, rotY225, S(1.25), T(−0.85, −0.6, 0) |
| 3P aim | T(0, 0.1, 0.2) |
| 3P recoil / reload | about pivot (0, 0.3, −0.9): rotZ(−20·K) / rotX(−30·yR), rotY(−40·yR) |
| Ground / Icon | rotX180, rotY90, S(0.8), T(0, −0.60, 0) / rotX180, rotY180, S(0.75), T(−0.25, 0.1, 0) |
| Muzzle FX | muzzleFlash_blue, (1.2, −2.0, 0.8), scale 1.65, rotY−90, default |
| 3P flash | 3, (0, 0.05, −0.35) |
| Sounds | `guns.pdwFire`, `guns.pdwReload` |

## LMG (`lmg`)

| | |
|---|---|
| Renderer / model | RenderLMG (handleDamage) / ModelLMG (ammo belt) |
| Texture | textures/guns/mg2_texture.png |
| Timing | recoil 5, flash 5, reload 100 |
| FP base / idle | rotX180, rotY225, S(1.4) / T(−0.55, −0.35, −0.65) |
| FP fire | rotX(10·yF), T(…, −0.65 − 0.3·yF) |
| FP reload | trapezoid y = p/0.3 (p < 0.3), 1 (0.3…0.7), (1 − p)/0.3 (p > 0.7): rotX(55·y), then idle. Holds the gun tilted 55° for the middle 40 % |
| 3P base | rotX180, rotY135, S(0.8), T(0.1, −0.65, 1.1) |
| 3P aim | T(0, −0.05, −0.1) |
| 3P recoil / reload | rotX(−15·K) / about pivot (0, −0.2, 0.05): rotX(−30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, S(0.6), T(0, −0.50, 0) / rotX180, S(0.48), T(0.3, 0, 0), rotY90 |
| Ammo visual | the belt shows min(ammo − 1, 6) rounds, and is hidden in the first half of a reload |
| Muzzle FX | muzzleFlash2, (−0.38, −3.4, 1.05), scale 1.5, default |
| Scope | zoom 0.75, FOV only |
| 3P flash | 1, (0, 0.1, 0.02) |
| Sounds | `guns.lmgFire`, `guns.lmgReload` |

## Pulse Rifle (`pulserifle`, field `pulseRifle`)

| | |
|---|---|
| Renderer / model | RenderPulseRifle (hasZoom) / ModelPulseRifle |
| Textures (camo) | pulseRifle.png, _1 (Grey), _2 (Dark Green) |
| Timing | recoil 8 (one 3-round burst per window), flash 5, reload 45 |
| FP base / idle | rotX180, rotY−45, S(2.1) / T(0.70, −0.30, −0.6) |
| FP fire | p < 0.5: fold (subtract 0.25 if p > 0.25), d = 0.33·sin(√((p mod 0.25)·4)) (two small kicks). p ≥ 0.5: d = sin(√((p mod 0.5)·2)) (one full kick). Then rotZ(−7.5·d), T(0.7 − 0.15·d, −0.3, −0.6) |
| FP reload | s = P66(p): rotZ(25·s), rotY(−15·s), T(0.7 + 0.5·s, −0.3 + 0.25·s, −0.6 + 0.25·s) |
| 3P base | rotX180, rotY225, S(1.25), T(−0.85, −0.6, 0) |
| 3P aim | T(0, 0.1, 0.2) |
| 3P recoil / reload | about pivot (0, 0.3, −0.9): rotZ(−15·K) / rotX(30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, rotY90, S(0.8), T(0, −0.6, 0) / rotX180, rotY180, S(0.7), T(−0.2, −0.3, 0.1) |
| Muzzle FX | muzzleFlash_blue, (1.05, −2.50, 0.65), scale 1.75, rotY−90, default |
| Scope | zoom 0.35. **techScope** overlay (scale 2.25, rotY90, alpha-blended), scope recoil (0.10, 0.10, 0) |
| 3P flash | 3, (0, 0.025, −0.05) |
| Sounds | `guns.pulserifleFire`, `guns.pulserifleReload` |

## Alien Blaster (`alienBlaster`)

| | |
|---|---|
| Renderer / model | RenderAlienBlaster / ModelAlienBlaster |
| Texture | textures/guns/alien_blaster.png |
| Timing | recoil 5, flash **10**, reload 35 |
| FP base / idle / fire / reload | rotX180, rotY225, S(1.4) / T(−0.55, −0.35, −0.65) / rotX(10·yF), T(…, −0.65 − 0.3·yF) / rotX(−50·yR), then idle |
| 3P base | rotX180, rotY135, S(0.8), T(0.1, −0.75, 0.9) |
| 3P aim | T(0, −0.1, 0) |
| 3P recoil / reload | while p < 0.5, about pivot (0, 0, 0.9): rotX(−25·K) / about pivot (0, 0, 0.9): rotX(−50·yR) |
| Ground / Icon | rotX180, S(0.6), T(0, −0.45, 0) / rotX180, S(0.8), T(0.2, 0, 0), rotY90 |
| Muzzle FX | muzzleFlashAlienBlaster, (−0.55, −1.5, 1.01), scale 1.5, additive |
| 3P flash | none |
| Sounds | `guns.alienblasterFire`, `guns.alienblasterReload` |

## Cyberdemon Blaster (`cyberdemonBlaster`)

| | |
|---|---|
| Renderer / model | RenderCyberdemonBlaster / ModelCyberdemonBlaster (glow) |
| Texture | textures/guns/cyberdemonblaster.png |
| Timing | recoil 5, flash **10**, reload 35 |
| FP base / idle | rotX180, rotY225, S(2.0) / T(−0.85, −0.20, −0.75) |
| FP fire | y = sin(√p·π): rotX(−10·y), T(−0.85, −0.2, −0.75 + 0.25·y): a fast punch back, then a slow return |
| FP reload | rotX(−50·yR), then idle |
| 3P base | rotX180, rotY135, S(1.35), T(0.20, −0.25, 0.07) |
| 3P aim | – |
| 3P recoil / reload | about pivot (0, 0, 1.0): rotX(−30·K) (p < 0.5) / about pivot (0, 0, 1.0): rotX(−40·yR) |
| Ground / Icon | rotX180, S(0.8), T(0, −0.45, 0) / rotX180, S(0.68), T(0.3, 0, 0), rotY90 |
| Muzzle FX | muzzleFlashBlaster, (−0.72, −1.0, 1.35), scale 1.5, blend ONE/ONE (pushed attrib) |
| 3P flash | none |
| Sounds | fire `guns.cyberdemonblasterFire`, reload `guns.teslaReload` |

## Minigun (`minigun`)

| | |
|---|---|
| Renderer / model | RenderMinigun (handleDamage, handleProgress) / ModelMinigun, 2 parts (0 = housing, 1 = rotor) |
| Texture | textures/guns/minigun.png |
| Timing | recoil 5 (checkRecoil). Fires every tick, so the rotor turns one revolution per 250 ms while the trigger is held. Flash 5, reload 100 |
| FP base / idle | rotX180, rotY−45, S(1.5) / T(0.70, −0.30, −0.6) |
| FP fire | d = w(p): rotZ(−1.5·d), T(−0.05·d, 0, 0). Then, about pivot (0, −0.3, −0.6), the rotor (part 1) rotX(360·p). Then idle. A light shake plus the spinning barrels |
| FP reload | s = P75(p): rotZ(25·s), rotY(−15·s), T(0.7 + 0.5·s, −0.3 + 0.25·s, −0.6 + 0.25·s) |
| 3P base | rotX180, rotY225, S(1.25), T(−0.6, 0, 0), then rotor rotX(360·p) |
| 3P aim | T(−0.05, −0.8, 0.95) |
| 3P recoil / reload | none / rotX(30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, rotY90, S(0.8), T(0, −0.6, 0) / rotX180, rotY180, S(0.5), T(0.25, 0.55, −0.6) |
| Muzzle FX | muzzleFlash2 with progress folded (p − 0.5 if p > 0.5, then ×2), so the 8 frames play twice per window. (−0.75, −2.2, −0.45), scale 1.65, rotY90, default |
| 3P flash | 1, (0, −0.8, 0.4). Turret offset (0, 0.6, 0) |
| Sounds | fire `guns.minigunFire` each shot, reload `guns.minigunReload`. Spin-up `guns.minigunStart` is defined but only referenced in commented-out code |

## Power Hammer (`powerHammer`) and Advanced Power Hammer (`powerHammerAdv`)

| | |
|---|---|
| Renderer / model | RenderPowerHammer (handleDamage, handleProgress) / ModelPowerHammer, 2 parts (0 = body, 1 = head and piston) |
| Texture | powerHammer.png / powerHammerAdv.png |
| Timing | charge gun: full charge 20 ticks. Recoil 12, flash 5, reload 45. Left-click swings set a 250 ms recoil with attackType 0. Charged release uses attackType 1 |
| FP base | rotX180, rotY−45, S(1.5), then charge pull-back: body T(−0.15·c, 0, 0), head T(−0.45·c, 0, 0) (c = charge 0…1) |
| FP idle | T(0, −0.75, −0.3) |
| FP fire, attackType 1 (charged blow) | d = sin(2π√p): rotZ(2.5·d), T(−0.15 + 0.15·√p, 0, 0), T((B(p)/2 − p4)·0.15, 0, 0) where p4 = 0.5 + 0.5·sin((p − 0.25)·2π). Head: T(−0.30 + 0.3·min(15p, 1), 0, 0): it snaps forward within the first 1/15 of the window. Then idle |
| FP fire, attackType 0 (swing) | d = sin(2π√p), d1 = sin²(πp): rotZ(1.5·d), T(0.15·d1, 0, 0). Head: d2 = −4p (p < 0.25), else √(min(1, (p − 0.25)·1.333)) − 1, giving T(0.45·d2, 0, 0): retracts quickly, then extends back. Then idle |
| FP reload | s = P75(p): rotZ(−32·s), rotY(−15·s), T(0.05·s, 0.4·s, −0.2·s), then idle |
| 3P base | rotX180, rotY225, S(1.45), T(−0.5, −0.65, 0) |
| 3P aim | T(0, −0.15, 0.3) |
| 3P recoil | head only: T(−0.30 + 0.3·min(15p, 1), 0, 0) |
| 3P reload | rotX(30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, rotY90, S(0.8), T(0, −0.6, 0) / rotX180, rotY180, S(1.0) |
| Muzzle FX / 3P flash | none / none |
| Sounds | charging starts: `guns.powerhammerCharge` (vanilla, volume 1). Release: `guns.powerhammerFire` (vanilla). Reload: `guns.powerhammerReload` (vanilla). Left-click swing with ammo: `guns.powerhammerSwing` (packet to all within 50 blocks *including the swinger*, volume 1.0, at most once per 5 ticks, not while a non-inventory GUI is open). Projectile hits an entity: `guns.powerhammerImpact`. Otherwise (block or expiry): `guns.powerhammerImpactGround`. Mining a block with ammo: `guns.powerhammerImpactGround` at the block |

## Grenade Launcher (`grenadelauncher`)

| | |
|---|---|
| Renderer / model | RenderGrenadeLauncher (handleDamage, handleProgress; the model ignores ammo) / ModelModel("models/guns/grenadelauncher.obj"), 2 parts (0 = body, 1 = drum) |
| Texture | textures/guns/grenadelauncher.png |
| Timing | recoil 5, flash 5, reload 100 |
| FP base | rotY45, S(2.0) (no X flip: the OBJ is already Y-up) |
| FP idle | T(1.05, 0.1, 0.85) |
| FP fire | y = B(p), y2 = 0.5 + 0.5·sin((p/2 − 0.25)π) (0.15 → 0.85). Drum (part 1), about pivot (0, −0.15, 0.85): rotX(60·y2) (it indexes round by about 42° during the window, then snaps back). Then all parts: rotZ(10·y), T(1.05 − 0.25·y, 0.1 + 0.1·y, 0.85) |
| FP reload | s = P50(p): rotZ(−25·s), rotY(−25·s), rotY(30·s), idle, T(−0.25·s, −0.20·s, −0.15·s) |
| 3P base | rotY135, S(1.3), T(−0.2, 0.35, 0) |
| 3P aim | T(0, 0.15, 0) |
| 3P recoil / reload | about pivot (−0.2, −0.1, 0): rotZ(**+40**·K) / rotZ(40·yR) |
| Ground / Icon | S(0.80), rotY−90, T(0, 0.35, 0) / rotX180, rotZ180, S(0.7), T(0, 0.15, −0.15) |
| Muzzle FX | muzzleFlash, (−0.6, −2.2, 1.6), scale 1.5, default |
| 3P flash | none |
| Sounds | `guns.grenadeLauncherFire`, `guns.grenadeLauncherReload` |

## AUG (`aug`)

| | |
|---|---|
| Renderer / model | RenderAUG (handleDamage, hasZoom) / ModelAUG, 2 parts (0 = gun, 1 = cocking handle) |
| Textures (camo) | AugTexture.png, _1 Grey, _2 Wood, _3 Sand, _4 Nether |
| Timing | recoil 5, flash 5, reload 45 |
| FP base / idle / reload | rotX180, rotY225, S(1.4) / T(−0.55, −0.35, −0.65) / rotX(−50·yR), then idle |
| FP fire | rotX(10·yF), T(…, −0.65 − 0.3·yF). Handle: t = p/0.15 (p < 0.15), else (1 − p)/0.85, giving T(0, 0, 0.6·t): it snaps back and slides home |
| 3P base | rotX180, rotY135, S(0.8), T(0.1, −0.6, 1.4) |
| 3P aim | T(0, 0.1, 0) |
| 3P recoil / reload | rotX(−15·K) / about pivot (0, −0.2, 0.05): rotX(−30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, S(0.6), T(0, −0.45, 0) / rotX180, S(0.48), T(0.3, 0, 0), rotY90 |
| Muzzle FX | muzzleFlash2, (−0.5, −3.4, 1.10), scale 1.5, default |
| Scope | zoom 0.5. Sniper overlay, scope recoil (0.15, 0.20, 0.10) |
| 3P flash | 1, (0, 0.025, 0) |
| Sounds | `guns.augFire`, `guns.augReload` |

## SCAR (`scar`)

Identical to the AUG except:

| | |
|---|---|
| Model / textures | ModelScar, parts 0 = gun, 1 = charging handle / scar_texture.png, _1 (Black) |
| FP fire handle travel | T(0, 0, **0.7**·t) |
| 3P base | rotX180, rotY135, S(0.8), T(0.1, −0.6, **1.1**) |
| Icon | rotX180, S(**0.52**), T(0.3, 0, 0), rotY90 |
| Muzzle FX | muzzleFlash2, (−0.48, −3.4, 1.10), scale 1.5 |
| Scope | zoom **0.65**. Sniper overlay, scope recoil (0.15, 0.20, 0.10) |
| 3P flash | 1, (0, 0.030, 0) |
| Sounds | `guns.scarFire`, `guns.scarReload` |

## Sonic Shotgun (`sonicShotgun`)

| | |
|---|---|
| Renderer / model | RenderSonicShotgun / ModelSonicShotgun (glow core) |
| Texture | textures/guns/sonicshotgun.png |
| Timing | recoil 5, flash 5, reload 40 |
| FP base / idle | rotX180, rotY−45, S(2.0) / T(0.2, −0.2, −0.7) |
| FP fire | y = B(p): rotZ(−8·y), idle, T(−0.6·y, 0, 0): a hard shove back, then settle |
| FP reload | s = P66(p): rotZ(25·s), rotY(−15·s), idle, T(0.5·s, 0.25·s, 0.25·s) |
| 3P base | rotX180, rotY225, S(1.25), T(−0.85, −0.6, 0) |
| 3P aim | T(0, 0.1, 0.2) |
| 3P recoil | about pivot (0, 0.3, −0.9): rotZ(−20·Kfull(p)) (over the whole window) |
| 3P reload | rotX(30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, rotY90, S(0.65), T(0, −0.60, 0) / rotX180, rotY180, S(0.55), T(−0.25, 0.1, 0) |
| Muzzle FX | muzzleFlashSonic, (0.95, −2.0, 0.35), scale **2.5**, rotY−90, additive |
| 3P flash | 3, (0, 0.05, −0.35) |
| Sounds | `guns.sonicshotgunFire`, `guns.sonicshotgunReload` |

## Chainsaw (`chainsaw`)

| | |
|---|---|
| Renderer / model | RenderChainsaw (handleDamage, handleProgress) / ModelChainsaw (chain flicker) |
| Texture | textures/guns/chainsaw.png |
| Timing | recoil 5 (right-click "fire" every 3 ticks). Left-click swing: 250 ms, attackType 0. Reload 45 |
| FP base / idle | rotX180, rotY−45, S(1.35) / T(1.1, 0, −1.6) |
| FP fire | idle, then attackType 0: y = w(p): T(0.15·y, 0, 0), rotZ(−5·y), rotY(−2.5·y), rotX(−1.5·y) (vibration). Otherwise y = sin²(πp): T(0.3·y, 0, 0), rotZ(−10·y), rotY(−5·y), rotX(−2.5·y) (lunge). In practice every chainsaw recoil is attackType 0 |
| FP reload | s = P75(p): rotZ(−32·s), rotY(−15·s), T(0.05·s, 0.4·s, −0.2·s), then idle |
| 3P base | rotX180, rotY225, S(1.30), T(−1.1, 0.55, −0.1) |
| 3P aim | T(0, −0.15, 0.3) |
| 3P recoil | applies only to a part 1, which this 1-part model does not have, so no visible effect |
| 3P reload | rotX(30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, rotY90, S(0.7), T(0, −0.60, 0) / rotX180, rotY180, S(0.5), T(−0.3, 0.3, −0.4) |
| Chain | blade plane alternates about 40 times per recoil window, in all views |
| Muzzle FX / 3P flash | none / none |
| Sounds | right-click held: first `guns.chainsawLoopStart`, then `guns.chainsawLoop` (loop delay 10). Left-click swing with fuel: `guns.chainsawSwing` (packet incl. swinger, volume 2.0, once per 5 ticks, not with a GUI open). Block cut with fuel: `guns.chainsawHit` (volume 0.65). Reload `guns.powerhammerReload`. `guns.chainsawLoopEnd` is defined but never played |

## Scatter Beam Gun (`scatterBeamGun`)

| | |
|---|---|
| Renderer / model | RenderLaserGunNew / ModelLaserGunNew |
| Texture | textures/guns/laserGunNew.png (the renderer's own texture argument names a non-existent `LasergunNew.png`, but it is ignored) |
| Timing | recoil 5, flash 5, reload 45 |
| FP base / idle / fire / reload | rotX180, rotY225, S(1.4) / T(−0.55, −0.35, −0.65) / rotX(10·yF), T(…, −0.65 − 0.3·yF) / rotX(−50·yR), then idle |
| 3P base | rotX180, rotY135, S(0.8), T(0.1, −0.5, 0.9) |
| 3P aim | T(0, 0.05, 0.2) |
| 3P recoil / reload | rotX(−15·K) / about pivot (0, −0.2, 0.05): rotX(−30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, S(0.6), T(0, −0.45, 0) / rotX180, S(0.48), T(0.3, 0, 0), rotY90 |
| Muzzle FX | muzzleFlashLaser, (−1.05, −2.8, −0.45), scale 1.5, rotY90, additive + alpha test |
| Scope | zoom 0.75, FOV only |
| 3P flash | none |
| Sounds | `guns.lasergunFire`, `guns.lasergunReload` |

## Nuclear Death Ray (`beamtestgun`, field `ndr`, unlocalized `nuclearDeathRay`)

| | |
|---|---|
| Renderer / model | RenderBeamgun (handleDamage, handleProgress) / ModelNDR (glow, firing crown) |
| Texture | textures/guns/NDR.png (the renderer argument [`Lasergun.png`](../02-guns/gun-catalogue.md#table-d-ai-visual-hooks-and-textures) is ignored) |
| Timing | recoil 5, fires every 5 ticks (continuous beam), flash 5, reload 30 |
| FP base | rotX180, rotY−45, S(1.1), **plus** T(0.2, −0.5, −0.2). The base already contains the idle translate, so it is applied twice |
| FP idle | T(0.2, −0.5, −0.2) |
| FP fire | y = w(p): idle, T(0.05·y, 0.025·y, 0), rotZ(0.5·y): a faint hum |
| FP reload | s = P66(p): rotZ(20·s), rotY(−10·s), idle, T(0.5·s, 0.25·s, 0.25·s) |
| 3P base | rotX180, rotY225, S(1.25), T(−0.7, −0.6, 0.1) |
| 3P aim | T(0, 0.1, −0.2) |
| 3P recoil / reload | T(−0.25·K, 0, 0) / about pivot (−0.2, −0.1, 0): rotZ(−40·yR), rotY(50·yR) |
| Ground / Icon | rotX180, rotY90, S(0.75), T(0.6, −0.2, 0) / rotX180, rotY180, T(−0.1, −0.3, 0.4), S(0.65) |
| Firing crown | shown in every view while a recoil window is live (fireProgress > 0) |
| Muzzle FX | muzzleFlashNukeBeam, (−0.7, −0.2, −0.3), scale 1.5, rotY90, additive + alpha test |
| 3P flash | none. Turret muzzle offset (−0.2, 0, 0) |
| Sounds | loop: `guns.beamgunStart` then `guns.beamgunFire` (delay 10). Reload `guns.lasergunReload` |

## Mac-10 (`mac10`)

| | |
|---|---|
| Renderer / model | RenderMac10 / ModelMac10 |
| Texture | textures/guns/mac10texture.png (128×64 PNG for a 64×32 model) |
| Timing | recoil **2**, flash 5, reload 40 |
| FP base / idle | rotX180, rotY225, S(2.0) / T(−0.55, −0.45, −0.8) |
| FP fire | rotX(7·yF), T(−0.55, −0.45, −0.8 − 0.3·yF) |
| FP reload | rotX(−50·yR), T(−0.55, −0.45, −0.8) |
| 3P base | rotX180, rotY135, S(1.2), T(0.05, −0.6, 0.4) |
| 3P aim | T(0, 0, 0.2) |
| 3P recoil / reload | while p < 0.5, about pivot (0, 0, 0.9): rotX(−10·K) / about pivot (0, 0.3, 0.9): rotX(−40·yR) |
| Ground / Icon | rotX180, S(0.9), T(0, −0.5, −0.3) / rotX180, S(1.1), rotY90, T(0.3, −0.5, 0) |
| Muzzle FX | muzzleFlash2, (−0.65, −1.25, 1.6), scale 1.0, default |
| 3P flash | 1, (0, 0.05, 0). Turret (0, 0, −0.45) |
| Sounds | `guns.mac10Fire`, `guns.mac10Reload` |

## MIB Gun (`mibGun`)

| | |
|---|---|
| Renderer / model | RenderMibGun / ModelMibGun |
| Texture | textures/guns/mibgun.png (128×128 PNG for a 64×64 model) |
| Timing | recoil 5, flash 5, reload 45 |
| FP base / idle | rotX180, rotY225, **S(2.5)** / T(−0.75, −0.45, −0.8) |
| FP fire | about pivot (0, 0.4, 0): rotX(25·yF), then idle: a sharp muzzle flip of the tiny gun |
| FP reload | rotX(−35·yR), then idle |
| 3P base | rotX180, rotY135, S(1.5), T(0.05, −0.80, 0.6) |
| 3P aim | T(0, 0, 0.2) |
| 3P recoil / reload | while p < 0.5, about pivot (0, 0.5, 0.3): rotX(−15·K) / about pivot (0, 0.5, 0.3): rotX(−35·yR) |
| Ground / Icon | rotX180, T(0, −0.6, 0) / rotX180, S(1.4), rotY90, T(0.3, −0.7, 0.4) |
| Muzzle FX | muzzleFlashAlienBlaster **tinted RGB (0.2, 1.0, 0.7)** (colour reset to white after), (−2.15, −2.45, −0.85), scale 1.0, rotY90, additive + alpha test |
| 3P flash | 5, (0, −0.1, 0.1). Turret (0, 0, −0.2) |
| Sounds | `guns.MIBblasterFire`, `guns.MIBblasterReload` |

## Vector (`vector`)

| | |
|---|---|
| Renderer / model | RenderVector (hasZoom false) / ModelVector |
| Textures (camo) | vector_texture.png, _1 (Black) (256×128 PNGs for a 128×64 model) |
| Timing | recoil **2**, flash 5, reload 40 |
| FP base / idle | rotX180, rotY225, S(1.4) / T(−0.65, −0.55, −0.65) |
| FP fire / reload | rotX(10·yF), T(−0.65, −0.55, −0.65 − 0.3·yF) / rotX(−50·yR), then idle |
| 3P base | rotX180, rotY135, S(0.8), T(0.1, −0.7, 1.1) |
| 3P aim | T(0, 0.1, 0) |
| 3P recoil / reload | rotX(−15·K) / about pivot (0, −0.2, 0.05): rotX(−30·yR), rotY(40·yR) |
| Ground / Icon | rotX180, S(0.6), T(0, −0.45, 0) / rotX180, S(0.75), T(0.3, −0.7, −0.6), rotY90 |
| Muzzle FX | muzzleFlash2 at (−0.88 + X, −2.4 + Y, 1.60 + Z), scale 1.25, default, where X/Y/Z are the **debug keybind offsets** (0 unless debug keybinds are enabled and used) |
| Scope | zoom 0.75, FOV only (the renderer has a sniper `renderScope` that is never reached) |
| 3P flash | 1, (0, 0.05, 0). Turret (0, 0, −0.45) |
| Sounds | `guns.vectorFire`, `guns.vectorReload` |

---

## Akimbo weapons

All use RenderGunAkimbo (framework §11). Only the left gun (`leftGun`) gets the extra translate. The "gate" means a gun's firing transform runs only when attackType matches it (0 = right gun, 1 = left gun).

### Akimbo Pistols (`pistolAkimbo`)

| | |
|---|---|
| Renderer / model | RenderPistolAkimbo / ModelAkimbo(ModelPistol3, 2): 4 passes (frame and slide per gun) |
| Texture | pistol3.png |
| Timing | recoil 3, flash **3**, reload 52. Alternating (ammo % 2) |
| FP base | rotX180, rotY225, S(2.0). Left gun: T(1.85, 0, 0) |
| FP idle | T(−0.55, −0.45, −0.8) |
| FP fire | gated y = yF: rotX(10·y), T(−0.55, −0.45, −0.8 − y·(0.3 + 0.2·part)) |
| FP reload | rotX(−50·yR), T(−0.55, −0.45, −0.8) (both guns) |
| 3P base | rotX180, rotY135, S(1.5), T(0.05, −0.55, 0.2) (both copies in the right hand) |
| 3P aim | T(0, 0, 0.2) |
| 3P recoil | only when attackType = 0: about pivot (0, 1.0, 1.0): rotX(−30·D30) |
| 3P reload | about pivot (0, 0.3, 0.9): rotX(−40·yR) |
| Ground | left gun T(0.2, 0.2, 0) first. Then rotX180, T(0, −0.35, −0.3) |
| Icon | left gun T(0, 0, −0.5). rotX180, S(1.5), rotY90. Left gun T(0.3, 0.4, −0.6). T(0.3, −0.6, 0.25) |
| Muzzle FX | muzzleFlash2, scale 1.0, at (−0.65, −0.75, 1.55) when attackType = 0, or (−0.65, −0.75, −1.9) when 1 |
| Off-hand (3P) | RenderAkimboGun, framework §11 |
| 3P flash | 1, (0, 0.05, −0.45). The x offset is mirrored for the left gun |
| Sounds | fire `guns.pistolFire`, reload `guns.pistolReloadAkimbo` |

### Akimbo Revolvers (`revolverAkimbo`, `goldenrevolverAkimbo`)

| | |
|---|---|
| Renderer / model | RenderRevolverAkimbo / ModelAkimbo(ModelRevolver or ModelGoldenRevolver, 1) |
| Textures | revolver.png / goldenrevolver.png |
| Timing | recoil 6 / 8, flash 5, reload 70. Alternating |
| FP base | rotX180, rotY−45, S(1.5). Left gun: T(0, 0, 1.75) |
| FP idle / fire | T(0.20, −0.5, −0.45) / gated y = yF: rotZ(35·y), T(0.2 + 0.5·y, −0.5 + 0.4·y, −0.45) |
| FP reload | s = P66(p): rotZ(25·s), then rotY(−15·s) for the right gun or rotY(+15·s) for the left (mirrored), then T(0.2 + 0.6·s, −0.5 + 0.3·s, −0.45 + 0.25·s) |
| 3P base | rotX180, rotY225, S(0.85), T(−1.4, −0.8, 0) |
| 3P aim | T(0, −0.05, 0.2) |
| 3P recoil | attackType 0 only, while p < 0.5, about pivot (0, 0.25, 1.0): rotZ(−30·K) |
| 3P reload | about pivot (0, 0.25, 1.0): rotZ(−40·yR) |
| Ground | left T(0.2, 0.2, 0). Then rotX180, rotY90, T(0, −0.35, 0), S(0.8) |
| Icon | S(0.8). Left T(0, 0, −0.5). rotX180, rotY180, S(1.0). Left T(0.3, 0.4, −0.6). T(−0.4, −0.2, 0) |
| Muzzle FX | muzzleFlash, scale 1.35, at (−0.5, −2.0, 1.05) when attackType = 0, or (−0.5, −2.0, −1.4) when 1 |
| 3P flash | 2, (0, 0.05, −0.45) / (0, 0.05, −0.4) |
| Sounds | fire `guns.revolverFire` / `guns.goldenrevolverFire`, reload `guns.revolverReloadAkimbo` |

### Akimbo Mac-10 (`mac10Akimbo`)

| | |
|---|---|
| Renderer / model | RenderMac10Akimbo / ModelAkimbo(ModelMac10, 1) |
| Texture | mac10texture.png |
| Timing | recoil 2, flash 5, reload 68. **Double shot**: both guns fire each time |
| FP base | rotX180, rotY225, S(2.0). Left gun: T(1.85, 0, 0) |
| FP idle / fire / reload | T(−0.55, −0.45, −0.8) / both guns: rotX(10·yF), T(−0.55, −0.45, −0.8 − 0.3·yF) / rotX(−50·yR), T(−0.55, −0.45, −0.8) |
| 3P base | rotX180, rotY135, S(1.2), T(0.05, −0.6, 0.4) |
| 3P aim | T(0, 0, 0.2) |
| 3P recoil | always: about pivot (0, 1.0, 1.0): rotX(−15·D30) |
| 3P reload | about pivot (0, 0.3, 0.9): rotX(−40·yR) |
| Ground | left T(0.2, 0.2, 0). rotX180, S(0.9), T(0, −0.5, −0.3) |
| Icon | left T(0, 0, −0.5). rotX180, S(1.1), rotY90. Left T(0.3, 0.4, −0.6). T(0.3, −0.7, 0.15) |
| Muzzle FX | muzzleFlash2 at **both** (−0.65, −1.25, 1.6) and (−0.65, −1.25, −2.0), scale 1.0 |
| 3P flash | 1, (0, 0.05, 0), spawned for both guns |
| Sounds | fire `guns.mac10Fire`, reload `guns.mac10ReloadAkimbo` |

### Akimbo MIB Guns (`mibGunAkimbo`)

| | |
|---|---|
| Renderer / model | RenderMibGunAkimbo / ModelAkimbo(ModelMibGun, 1) |
| Texture | mibgun.png |
| Timing | recoil 5, flash 5, reload 60. Alternating |
| FP base | rotX180, rotY225, S(2.5). Left gun: T(2.05, 0, 0) |
| FP idle / fire / reload | T(−0.75, −0.45, −0.8) / gated r = yF, if r ≠ 0, about pivot (0, 0.4, 0): rotX(25·r), then idle / rotX(−35·yR), then idle |
| 3P base | rotX180, rotY135, S(1.5), T(0.05, −0.80, 0.7) |
| 3P aim | – (default) |
| 3P recoil | attackType 0, while p < 0.5, about pivot (0, 0.5, 0.3): rotX(−15·K) |
| 3P reload | about pivot (0, 0.5, 0.3): rotX(−35·yR) |
| Ground | left T(0.2, 0.2, 0). rotX180, T(0, −0.6, 0) |
| Icon | S(0.9). Left T(0, 0, −0.5). rotX180, S(1.4), rotY90. Left T(0.3, 0.4, −0.6). T(0.1, −0.8, 0.4) |
| Muzzle FX | tinted alien flare (0.2, 1.0, 0.7), scale 1.0, rotY90, additive + alpha test, at (−2.15, −2.45, −0.85) when attackType = 0, or (2.55, −2.45, −0.85) when 1 |
| 3P flash | 5, (0, −0.1, 0.1). Turret (0, 0, −0.2) |
| Sounds | fire `guns.MIBblasterFire`, reload `guns.MIBblasterReloadAkimbo` |

---

## Grenades

| | Stielhandgranate ([`stielGranate`](../02-guns/gun-catalogue.md#table-f-grenades-not-genericgun)) | Frag Grenade ([`fragGrenade`](../02-guns/gun-catalogue.md#table-f-grenades-not-genericgun)) |
|---|---|---|
| Renderer / model | RenderStielgranate / ModelStielgranate | RenderFragGrenade / ModelFragGrenade(showRing = true) |
| Texture | textures/guns/stielgranate.png | textures/guns/fragGrenade_texture.png |
| Transforms | framework §12 | framework §12 |
| Animation | tilts back up to 30° over a 1 s wind-up (hold right-click) | tilts back up to 20°. The pull ring disappears as soon as the hold starts |
| Sounds | throw: vanilla `random.bow`, volume 1, pitch 1/(rand·0.4 + 1.2) + charge·0.5 | right-click (pin pulled): `guns.grenade_pin`. Throw: `guns.grenade_throw` (both vanilla positional, volume 1, pitch 1) |

The [`flashbang`](../02-guns/gun-catalogue.md#table-f-grenades-not-genericgun) item object is constructed in [`TGuns`](../02-guns/gun-catalogue.md) but never registered, and it has no custom renderer (area [02](../02-guns/index.md)).

---

## Unused content

| Item | Content | Status |
|---|---|---|
| Neutrino Gun | RenderNeutrinoGun / ModelNeutrinoGun / textures/guns/neutrinogun.png. FP rotX180, rotY225, S(1.0) + T(−0.3, −0.6, 0.1) (doubled like the NDR). Fire w(p) hum. Reload P66. Muzzle FX muzzleFlashNukeBeam (−0.7, −1.1, −0.3), scale 1.5, rotY90, additive. 3P recoil T(0, 0, −0.25·K). Intended sounds were `guns.beamgunStart`/`beamgunFire`/`lasergunReload` | Item creation commented out. The renderer is registered against a null item |
| MP5 | RenderMP5 / ModelMp5 / mp5Texture.png | commented out |
| Driller | RenderDriller (legacy RenderGunMultipart: drill bit part 1 spins 360°·p about Z while firing and in 3P) / ModelDriller / drillTexture.png | commented out |
| M4 scoped variant | RenderM4Scoped (legacy RenderGunScoped) | never registered |
| Railgun | would have reused RenderLasergun / Lasergun.png | commented out |
