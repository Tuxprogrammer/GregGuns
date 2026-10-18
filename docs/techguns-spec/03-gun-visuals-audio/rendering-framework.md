# Gun rendering framework

How Techguns draws hand-held weapons: the renderer class hierarchy, the client-side timing state that drives animation, the per-frame algorithm, shared animation curves, muzzle flashes, scopes and zoom, camo textures, ammo-dependent parts, akimbo, grenades and magazine items. Per-gun numbers live in [gun-sheets.md](gun-sheets.md). Model geometry is in [gun-models.md](gun-models.md).

All guns use Forge 1.7.10 `IItemRenderer` custom item renderers. They draw a `ModelBase`/`ModelRenderer` box model, or one OBJ model, for every render type: first person, third person (held), dropped entity and inventory icon. Item icons in inventories are drawn from the 3D model too. No 2D sprite exists for any gun covered here.

---

## 1. Registration (client proxy)

[`ClientProxy.registerRenderers()`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) binds one renderer instance per gun item. The `ResourceLocation` texture argument passed to every renderer constructor is **ignored** by the live base class (`RenderGunBase`). The bound texture always comes from the gun item's own texture list (see §8). The constructor flags that matter are listed below. The `scale/x/y/z` arguments are stored but never read.

| Item field (`TGuns.*`) | Renderer class | Model | handleDamage | handleProgress | parts | hasZoom (scope overlay) |
|---|---|---|---|---|---|---|
| handcannon | RenderHandgun | ModelHandgun | no | no | 1 | no |
| pistol | RenderPistol | ModelPistol3 | yes | no | 2 | no |
| sawedoff | RenderSawedoff | ModelSawedoff | no | no | 1 | no |
| boltaction | RenderBoltActionRifle | ModelBoltaction | no | no | 1 | **yes** |
| flamethrower | RenderFlamethrower | ModelFlamethrower | no | no | 1 | no |
| m4 | RenderM4 (dmg=false, scope=false) | ModelM4 | no | no | 1 | no |
| combatShotgun | RenderCombatShotgun | ModelCombatShotgun | yes | no | 2 | no |
| lasergun | RenderLasergun | ModelLasergun | no | no | 1 | no |
| blasterRifle | RenderBlasterRifle | ModelBlasterRifle | no | no | 1 | **yes** |
| rocketlauncher | RenderRocketLauncher | ModelRocketLauncher | yes | no | 1 | no |
| nukeLauncher | RenderRocketLauncher | ModelRocketLauncher | yes | no | 1 | no |
| biogun | RenderBiogun | ModelBiogun | no | **yes** | 1 | no |
| thompson | RenderThompson | ModelThompson | no | no | 1 | no |
| revolver | RenderRevolver | ModelRevolver | no | no | 1 | no |
| goldenrevolver | RenderRevolver (scale arg 1.10, unused) | ModelGoldenRevolver | no | no | 1 | no |
| ak47 | RenderAK | ModelAK | no | no | 1 | no |
| as50 | RenderAS50 | ModelAS50 | no | no | 1 | **yes** |
| teslagun | RenderTeslaGun | ModelTeslaGun | no | no | 1 | no |
| pdw | RenderPDW | ModelPDW | no | no | 1 | no |
| lmg | RenderLMG | ModelLMG | yes | no | 1 | no |
| m4_uq | RenderM4_Infiltrator (extends RenderM4, dmg=true, scope=true) | ModelM4_UQ | yes | no | 1 | **yes** |
| grimreaper | RenderGrimReaper | ModelGrimReaper | no | no | 1 | no |
| pulseRifle | RenderPulseRifle | ModelPulseRifle | no | no | 1 | **yes** |
| alienBlaster | RenderAlienBlaster | ModelAlienBlaster | no | no | 1 | no |
| cyberdemonBlaster | RenderCyberdemonBlaster | ModelCyberdemonBlaster | no | no | 1 | no |
| minigun | RenderMinigun | ModelMinigun | yes | **yes** | 2 | no |
| powerHammer / powerHammerAdv | RenderPowerHammer | ModelPowerHammer | yes | **yes** | 2 | no |
| aug | RenderAUG (dmg=true, scope=true) | ModelAUG | yes | no | 2 | **yes** |
| grenadeLauncher | RenderGrenadeLauncher | ModelModel("models/guns/grenadelauncher.obj") | yes | **yes** | 2 | no |
| sonicShotgun | RenderSonicShotgun | ModelSonicShotgun | no | no | 1 | no |
| chainsaw | RenderChainsaw | ModelChainsaw | yes | **yes** | 1 | no |
| scatterBeamGun | RenderLaserGunNew | ModelLaserGunNew | no | no | 1 | no |
| ndr (registry name `beamtestgun`) | RenderBeamgun | ModelNDR | yes | **yes** | 1 | no |
| mac10 | RenderMac10 | ModelMac10 | no | no | 1 | no |
| mibGun | RenderMibGun | ModelMibGun | no | no | 1 | no |
| neutrinoGun (**item is never created**) | RenderNeutrinoGun | ModelNeutrinoGun | yes | yes | 1 | no |
| scar | RenderScar (dmg=true, scope=true) | ModelScar | yes | no | 2 | **yes** |
| vector | RenderVector | ModelVector | no | no | 1 | no |
| akimbo_pistol | RenderPistolAkimbo | ModelAkimbo(ModelPistol3, 2) | yes | no | 2 per gun (4 total) | no |
| akimbo_revoler | RenderRevolverAkimbo | ModelAkimbo(ModelRevolver, 1) | yes | no | 1 per gun (2) | no |
| akimbo_goldenrevolver | RenderRevolverAkimbo | ModelAkimbo(ModelGoldenRevolver, 1) | yes | no | 1 per gun (2) | no |
| akimbo_mac10 | RenderMac10Akimbo | ModelAkimbo(ModelMac10, 1) | yes | no | 1 per gun (2) | no |
| akimbo_mibGun | RenderMibGunAkimbo | ModelAkimbo(ModelMibGun, 1) | yes | no | 1 per gun (2) | no |
| stielGranate | RenderStielgranate | ModelStielgranate | – | – | – | – |
| fragGrenade | RenderFragGrenade | ModelFragGrenade(showRing = true) | – | – | – | – |
| TGItems.sharedItem (magazines and rockets only) | RenderGenericItem | see §13 | – | – | – | – |

Notes on the flags:

* **handleDamage**: the model's extended `render(…, ammoLeft, reloadProgress, renderType, part, fireProgress)` is called. Otherwise the plain `render(…)` is called, which `ModelMultipart` forward with `ammoLeft = 100`, `reloadProgress = -1`, `renderType = ENTITY`, `part = 0`, `fireProgress = 0`. **Consequence:** a multipart model whose renderer has `handleDamage = false` always receives `part = 0`. This is why `RenderAUG`/`RenderScar` are given `dmg = true`. Without it, their bolt part would never be selected.
* **handleProgress**: in first person, while the player has the item "in use" (`getItemInUse() != null`), `prog` = ticks in use ÷ `fullChargeTime` for charge guns ([`GenericGunCharge`](../02-guns/per-gun-behavior.md#genericguncharge-genericgunchargejava)), otherwise ticks in use ÷ 20, clamped to [0,1]. Only charge guns and grenades ever put the item in use, so this only animates the Power Hammer and Bio Gun charge. For the other flagged guns it stays 0 until firing overwrites it (see §3).
* **parts**: the model is drawn *parts* times per frame. Each pass gets its own GL matrix and its own transform calls with the part index. Multipart models render a different subset of boxes per index. This is how slides, pumps, bolts, drums, spinning barrels and hammer heads move independently.

Unused renderer classes are present in the source but never registered: `RenderGun` (legacy single-part base), `RenderGunScoped` + `RenderM4Scoped`, `RenderGunMultipart` + [`RenderDriller`](gun-models.md#modeldriller-unused-12864-family-z-18-boxes-2-parts), `RenderMP5` (extends RenderPistol, adds nothing), and `RenderNeutrinoGun` (registered against a `null` item). `RenderGenericItem` *is* used, for magazine and rocket shared items.

---

## 2. Class hierarchy

```
IItemRenderer
 ├─ RenderGunBase (abstract)          live base for every gun
 │   ├─ RenderHandgun, RenderPistol, RenderSawedoff, … (one per gun)
 │   ├─ RenderM4 ─ RenderM4_Infiltrator
 │   └─ RenderGunAkimbo (abstract)     splits parts into right/left gun
 │       └─ RenderPistolAkimbo, RenderRevolverAkimbo, RenderMac10Akimbo, RenderMibGunAkimbo
 ├─ RenderGun (abstract, legacy)       ─ RenderGunScoped ─ RenderM4Scoped     (unused)
 ├─ RenderGunMultipart (abstract, legacy) ─ RenderDriller                     (unused)
 ├─ RenderGrenade (abstract)           ─ RenderStielgranate, RenderFragGrenade
 ├─ RenderGenericItem                  dispatches to RenderM4Mag / RenderAS50Mag / RenderLMGMag / RenderRocketAmmo
 └─ (RenderAkimboGun, in renderer/additionalslots: draws the off-hand gun on the third-person player; see §11)
```

Models: `ModelMultipart` (abstract, extends ModelBase) declares the extended `render` with part, ammo, reload and fire progress. `ModelAkimbo` wraps another ModelMultipart: part indices ≥ partsPerModel are folded back (`part − partsPerModel`) so the same geometry is drawn for the second gun. [`ModelModel`](gun-models.md#obj-model-modelsgunsgrenadelauncherobj) wraps an OBJ (`AdvancedModelLoader`). It scales by the 0.0625 factor and renders the OBJ group whose name is the part index as a string (`"0"`, `"1"`); the plain `render` draws all groups. `ModelGunAmmo` is the legacy ammo-aware base used only by the dead `RenderGun`. `ModelNull` is an empty model (used by the helicopter renderer, area [10](../10-entities-npcs/index.md)).

`handleRenderType` returns true for all render types and `shouldUseRenderHelper` returns true for all helpers on every gun renderer. Forge's standard inventory and entity helpers (bobbing, centering) therefore apply.

---

## 3. Client timing state (what drives every animation)

Per entity (key = player name for players, entity id for other mobs) the client keeps an [`AttackTime`](../01-core/client-effects-audio.md#attack-timing-recoil--reload) record in `ClientProxy.attackTimes`:

| Field | Meaning |
|---|---|
| recoilTime | absolute wall-clock ms when the current recoil ends (0 = none) |
| recoilTimeTotal | recoil length in ms |
| reloadTime | absolute ms when the current reload ends (0 = none) |
| reloadTimeTotal | reload length in ms |
| attackType | byte. 0 normally. For alternating akimbo guns it is the gun index, `ammo % 2` *before* the shot (0 = right/main gun, 1 = left gun). Charge-gun releases (Power Hammer, Bio Gun) and their reloads set 1. Chainsaw and Power Hammer swings set 0. |

Plus two globals for the local player only: `player_muzzleFlashtime` (absolute end ms) and `player_muzzleFlashtime_total`.

Times are set from tick values with ms = ticks ÷ 20 × 1000:

| Event | Where set | Values |
|---|---|---|
| Local player fires (client half of `shootGunLeftClick`) | recoil (skipped if the gun has `checkRecoil` and the same attackType is still recoiling in first person), muzzle flash | gun `recoiltime` (default 5 ticks = 250 ms), `muzzleFlashtime` (default 5 ticks) |
| Any client receives [`GunFiredMessage`](../01-core/networking.md#1--gunfiredmessage-sc) (server echo to everyone within 100 blocks, **including the shooter**) | recoil for that shooter (same checkRecoil rule) and a muzzle-flash *particle* (area [04](../04-projectiles-fx/index.md)) | recoil ms from packet |
| Local reload starts | reload | gun `reloadtime` |
| [`ReloadStartedMessage`](../01-core/networking.md#3--reloadstartedmessage-sc) (others only) | reload for that shooter | |
| Charge gun released | recoil (attackType 1), muzzle flash | |
| Chainsaw / Power Hammer swing (client) | recoil 250 ms, attackType 0 (only if no recoil is active) | |

Progress values used by renderers: `progress = 1 − (end − now) / total`, giving 0 → 1 over the window. When a renderer finds a window expired (remaining ≤ 0, or remaining > total for recoil), it resets that window to 0. That reset also resets attackType to 0.

---

## 4. Per-frame algorithm (`RenderGunBase.renderItem`)

For each part index `i` in 0 … parts−1:

1. Push matrix. Set `prog = 0`, `attackType = 0`, `reloadProgress = 10` (a sentinel above 1 that means "not reloading").
2. **First person (`EQUIPPED_FIRST_PERSON`):**
   1. attackType = the local player's stored attackType.
   2. If handleProgress and the item is in use: prog = charge/use progress (§1).
   3. Apply **FirstPerson base transform**(i, prog, attackType).
   4. If a reload window exists: if expired → clear it and apply **Idle translate**(i); else compute reloadProgress and apply **Reloading transform**(i, reloadProgress).
   5. Else if a recoil window exists: if the scoped renderer is zooming (`hasZoom` and `player_zoom ≠ 1`), set *renderScope*. Otherwise, if expired → clear it and apply **Idle translate**; else fireProgress = progress, apply **Firing transform**(i, fireProgress), and set **prog = fireProgress**.
   6. Else (idle): if zooming → *renderScope*; else apply **Idle translate**.
   7. If the muzzle-flash window is live: *showMuzzleFX*, flashProgress = its progress (expired → cleared).
3. **Third person (`EQUIPPED`, held by any living entity):**
   1. attackType = the shooter's. If reloading (window live): reloadProgress = progress, *reloading*. Else if recoiling: prog = recoil progress (expired windows are cleared).
   2. *Aimed* = shooter is any player, or a Techguns NPC ([`GenericNPC`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava)) whose `getHasAimedBowAnim()` is true. If aimed, apply the **aimed pre-rotation** (§6.3).
   3. Apply **Equipped base transform**(i, prog, attackType).
   4. If the shooter is an [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret), prog = 0 (turrets never show recoil).
   5. Apply **Equipped reload transform**(i, reloadProgress) if reloading, else **Equipped recoil transform**(i, prog).
4. **Dropped (`ENTITY`):** apply **Ground transform**(i).
5. **Inventory (`INVENTORY`):** apply **Icon transform**(i).
6. If not *renderScope*:
   * Pick the texture: index = the item's camo index if the item implements [`ICamoChangeable`](../05-armor/armor-catalog.md#1-class-hierarchy) (all guns do), else 0. Bind `gun.textures[index]`.
   * Render the model: extended call with `ammoLeft = gun.getCurrentAmmo(stack)`, reloadProgress, render type, part i and `prog` (handleDamage), or the plain call.
   * Pop matrix.
   * After the **last** part only: if *showMuzzleFX*, draw the muzzle effect (§7) in the muzzle frame. If first person, call `drawExtraEffects()` in the same frame (only the flamethrower overrides it).
7. If *renderScope*: pop matrix. On part 0 only, push the muzzle frame, apply scope recoil if a recoil window is live (§9.3), call `renderScope()`, then pop. The gun model and muzzle flash are **not** drawn while scoped.

So in first person the gun is drawn with (base) → (reload | firing | idle) transforms. In third person it is drawn with (aimed) → (equipped base) → (reload | recoil). The muzzle flash is a first-person-only screen effect. Other players' flashes are particles (area [04](../04-projectiles-fx/index.md)).

`RenderGrenade` (§12) and the magazine/rocket renderers (§13) are much simpler, fixed-transform renderers.

---

## 5. Animation curves

In the renderer descriptions and in [gun-sheets.md](gun-sheets.md), transforms are listed in GL call order: each later call applies to the model first. `rotX(a)` = rotate *a* degrees about X. `T(x,y,z)` = translate. `S(s)` = uniform scale. "about pivot P: …" = translate P, do …, translate −P. `p` is the relevant progress (0 → 1).

| Name | Definition | Shape |
|---|---|---|
| `tri(p)` | 1 − \|2p − 1\| | 0 → 1 at p = 0.5 → 0 |
| `yF(p)` ("fire dip") | −tri(p)/2 | 0 → −0.5 → 0. Used by almost every *firing* transform. |
| `yR(p)` ("reload dip") | −tri(p) | 0 → −1 → 0. Used by most *reload/equipped-reload* transforms. |
| `sin(p)` | sin(πp) | smooth 0 → 1 → 0 |
| `P66(p)` | 1.5 · min(0.66, sin(πp)) | rises to ≈0.99 at p ≈ 0.23, holds a plateau, falls after p ≈ 0.77 |
| `P75(p)` | 1.333 · min(0.75, sin(πp)) | plateau ≈1.0 between p ≈ 0.27 and 0.73 |
| `P50(p)` | 2 · min(0.5, sin(πp)) | plateau 1.0 between p ≈ 0.17 and 0.83 |
| `K(p)` ("kick") | only while p < 0.5: with q = 2p, q < 0.25 → 4q, else 1.25(1 − q). 0 when p ≥ 0.5 | snaps to full kick at p = 0.125, then eases back (starting at 0.9375) to 0 at p = 0.5 |
| `Kfull(p)` | p < 0.25 → 4p, else 1.25(1 − p) | the same kick spread over the whole window (Sonic Shotgun) |
| `D30(p)` (base default 3P recoil) | p < 0.3 → p/0.3, else 0 | linear ramp then snap back |
| `B(p)` ("bounce") | 0.5 + 0.5·sin((√p + 0.75)·2π) | 0 at p = 0, peaks 1 at p = 0.25, 0.5 at p ≈ 0.56, back to 0 at p = 1 |
| `w(p)` | sin(2πp) | one full sine wave (vibration) |

Typical uses:

* **Firing (first person):** most rifles do `rotX(10·yF)` and `T(base.x, base.y, base.z − 0.3·yF)`: a slight muzzle climb and a push back. Revolvers and handguns rotate about Z and translate along X.
* **Reload (first person):** either a tilt and hold, `rotX(−50·yR)` (magazine-fed rifles), or a swing out with `P66` or `sin` (shotguns and energy guns).
* **Third-person recoil:** `K(p)` scaled by −10° to −40° about X (Z-axis models) or Z (X-axis models), often about a pivot near the grip.

---

## 6. Coordinate conventions

### 6.1 Model space

Model units are 1/16 block. `ModelRenderer.render(0.0625)` maps 16 units to 1 GL unit. Models are authored in one of two orientations, and each renderer uses the matching base rotations:

| Family | Muzzle points | First-person base | Third-person base | Members |
|---|---|---|---|---|
| **X-family** | +X | rotX(180), rotY(−45) | rotX(180), rotY(225) | Handgun, Sawed-off, Bolt-action, Combat Shotgun, Thompson, Revolver, Golden Revolver, Flamethrower, Laser Gun, Tesla Gun, PDW, Bio Gun, Rocket/Nuke Launcher, Grim Reaper (model internally flipped), Pulse Rifle, Minigun, Power Hammer, Sonic Shotgun, Chainsaw, NDR |
| **Z-family** | −Z | rotX(180), rotY(225) | rotX(180), rotY(135) | Pistol, Mac-10, AK-47, AS50, AUG, Alien Blaster, Blaster Rifle, Cyberdemon Blaster, LMG, Scatter Beam Gun, M4, M4 Infiltrator, MIB gun, SCAR, Vector, Neutrino gun (unused) |
| OBJ | n/a | rotY(45) only | rotY(135) | Grenade Launcher |

The rotX(180) flips the model's Y-down box space to world Y-up.

### 6.2 Muzzle frame

The muzzle flash, extra effects and scope are drawn in a frame that is *not* attached to the gun's transforms. After popping the gun matrix: push, rotY(45), rotZ(90), T(1.5, −1.0, −0.5), then the effect. Scope only: T(1.795, 0, −0.685) is added. Because this frame ignores firing and reload transforms, flashes do not follow the gun's recoil motion.

### 6.3 Third-person aimed pre-rotation

Applied before the equipped base transform for players (always, whenever any [`GenericGun`](../02-guns/gun-lifecycle.md) is held) and for Techguns NPCs with the aimed-bow animation: rotX(−45), rotY(−18), rotZ(45), rotY(45), T(0, −1.3, −0.4), *[gun-specific extra translate]*, rotY(−45). The gun-specific extra translate (`transformEquippedAimed` override) is listed per gun in the sheets. At the same time an event handler (`RenderPlayerEvent.Pre`) forces `aimedBow = true` on the player's biped, armor and chestplate models whenever a GenericGun is held. Both arms are therefore raised forward like a drawn bow. Grenades are not GenericGuns and use the normal arm pose.

---

## 7. Muzzle flash (first person screen effect)

[`ScreenEffect`](../04-projectiles-fx/renderers.md#screeneffect-muzzle-flashes-and-scope-overlays) is a sprite-sheet quad. An instance holds a texture, a column count, a row count and a frame count. `doRender(progress, offX, offY, offZ, scale [, rotX, rotY, rotZ])` does the following:

* frame = ⌊frames × progress⌋. Nothing is drawn once frame ≥ frames. Frames are read left to right, top to bottom.
* Enables "FX lighting" (lighting off, lightmap forced to full bright 240/240). Binds the sheet, pushes, rotates X, Y, Z, then scales by `scale`.
* Draws a single quad in the local XZ plane (normal ±Y) centered at (offX, offY, offZ), side length `scale`. Because the scale is applied first, the **effective** offset is `scale × off` and the effective side is `scale²` GL units.
* Brightness 15728880 (full). Restores lighting.

Blending is whatever the caller sets: *default* (no explicit blend; uses the current item-render state, normally alpha-tested), *additive* (`enableAlphaBlendAdditive`: blend ONE, ONE inside a pushed ENABLE_BIT), or *additive + alpha test*.

Instances used by guns (textures belong to area [04](../04-projectiles-fx/index.md), `textures/fx/…`):

| ScreenEffect | Texture | Grid | Frames | Used by |
|---|---|---|---|---|
| muzzleFlash | textures/fx/muzzleflash4x4.png | 4×4 | 10 | Handgun, Sawed-off, Combat Shotgun, Revolver/Golden, Revolver akimbo, Grenade Launcher |
| muzzleFlash2 | textures/fx/muzzleflash2.png | 4×4 | 8 | Pistol (+akimbo), Bolt-action, M4, Thompson, AK, AS50, LMG, AUG, Mac-10 (+akimbo), SCAR, Vector, Minigun |
| muzzleFlash_blue | textures/fx/muzzleflash_blue.png | 4×4 | 8 | PDW, Pulse Rifle |
| muzzleFlashLaser | textures/fx/laserflare02.png | 4×4 | 7 | Laser Gun, Blaster Rifle, Scatter Beam Gun |
| muzzleFlashLightning | textures/fx/teslaflare01.png | 4×4 | 14 | Tesla Gun |
| muzzleFlashBlaster | textures/fx/blasterflare01.png | 4×4 | 14 | Cyberdemon Blaster |
| muzzleFlashAlienBlaster | textures/fx/alienflare.png | 4×4 | 14 | Alien Blaster, MIB gun (+akimbo, tinted) |
| muzzleFlashSonic | textures/fx/sonicwave4x4.png | 4×4 | 16 | Sonic Shotgun |
| muzzleFlashNukeBeam | textures/fx/nukebeamflare.png | 4×4 | 16 | NDR, (Neutrino gun) |
| FlamethrowerMuzzleFlame | textures/fx/flamethrower2.png | 4×4 | 16 | Flamethrower pilot flame |
| sniperScope | textures/fx/Testscope.png | 1×1 | 1 | scope overlay (Bolt-action, AS50, AUG, SCAR, Blaster Rifle, M4 Infiltrator) |
| techScope | textures/fx/techscope.png | 1×1 | 1 | Pulse Rifle scope overlay |
| (defined, unused by guns) muzzleFlashYellow, muzzleFlashLightningOld, muzzleFlashFireball_alpha/_add, muzzleFlashNew, muzzleFlashNew2 | muzzleFlashYellow2x2.png, lightningflare4x4_2.png, fireball_4x4_alpha/add.png, muzzleflashnew.png, muzzleflashnew_2_add.png | | | only in commented-out code |

The flash plays over the gun's `muzzleFlashtime` window (§3). Per-gun offsets, scales, rotations and blend modes are in the gun sheets. All are first-person only and drawn after the last part.

**Flamethrower pilot flame** (`drawExtraEffects`): drawn every first-person frame while the local player is neither reloading nor recoiling, and the space 1.5 blocks above the player's position is free of liquid and of colliding blocks (so it is hidden underwater). It uses the FlamethrowerMuzzleFlame sheet with progress = (world total time mod 30)/30, so all 16 frames loop every 30 ticks. Additive blend, offset (−1.85, −3.3, −0.65), scale 1.0, rotY 90.

---

## 8. Textures and camouflage

`GenericGun.setTexture(path)` = `setTextures(path, 1)`. `setTextures(path, n)` makes the client add `assets/techguns/<path>.png` for index 0 and `<path>_<i>.png` for i = 1 … n−1. It also sets `camoCount = n`. The renderer binds `textures[camoIndex]`.

Camo state lives in item NBT: byte tag **`camo`** (absent = 0). `switchCamo(stack[, back])` cycles 0 … camoCount−1 forward or backward and creates the tag if missing (UI and keybind in areas [01](../01-core/index.md)/[08](../08-machines-production/index.md)). The display name of a camo is the lang key `<unlocalizedName>.camoname.<i>` for i > 0. For i = 0 it is `techguns.item.defaultcamo` = "Default Camo".

| Gun | Texture base | Variants | Lang keys (en_US) |
|---|---|---|---|
| Bolt-action | textures/guns/boltactionrifle | 3 | techguns.item.boltaction.camoname.1 = Olive, .2 = Tactical Black |
| M4 | textures/guns/m4Texture | 5 | techguns.item.m4.camoname.1 = Swamp, .2 = Desert, .3 = Ice, .4 = Hazard |
| AK-47 | textures/guns/ak47Texture | 2 | techguns.item.ak47.camoname.1 = Blue |
| PDW | textures/guns/PDW | 3 | techguns.item.pdw.camoname.1 = Silver, .2 = Dark Green |
| Pulse Rifle | textures/guns/pulseRifle | 3 | techguns.item.pulseRifle.camoname.1 = Gray, .2 = Dark Green |
| AUG | textures/guns/AugTexture | 5 | techguns.item.aug.camoname.1 = Gray, .2 = Wood, .3 = Sand, .4 = Nether |
| SCAR | textures/guns/scar_texture | 2 | techguns.item.scar.camoname.1 = Black |
| Vector | textures/guns/vector_texture | 2 | techguns.item.vector.camoname.1 = Black |
| every other gun | its single texture | 1 | – |

Other related lang strings: `techguns.item.defaultcamo` = Default Camo. `techguns.item.invalidcamo` = No Camo. `techguns.tooltip.currentcamo` = Current Camo. `techguns.message.camoswitch` = Switched Camo to. The tooltip line itself is built in area 02. *Correction (consistency pass):* no gun tooltip or chat message shows the camo; `techguns.tooltip.currentcamo` and `techguns.message.camoswitch` are used by armor only, and a gun's camo is changed only in the Camo Bench GUI. See [lang-and-strings.md](lang-and-strings.md).

**Texture resolution vs model.** Box UVs are normalized by the model's declared `textureWidth/Height`, so a PNG may be an exact integer multiple of the declared size and render as a higher-resolution version of the same layout. Mismatches (all ×2) are: Blaster Rifle (model 128×64, PNG 256×128), Frag Grenade (32×32 / 64×64), Mac-10 (64×32 / 128×64), MIB gun (64×64 / 128×128), Neutrino gun (128×128 / 256×256), Pistol3 (64×32 / 128×64), SCAR (128×64 / 256×128), Vector (128×64 / 256×128). See [textures.md](textures.md).

---

## 9. Zoom and scopes

### 9.1 Zoom state

`ClientProxy.player_zoom` (1.0 = none) multiplies the FOV in the FOV-update event: `newFov = newFov × player_zoom × speedFactor`. The speed factor is 1 unless the client config "lock speed FOV" is on (area [01](../01-core/index.md)). A gun's zoom is set by `setZoom(mult, toggle, accuracyBonus)`. Every gun that has zoom uses **toggle** mode:

* Right-click while holding the gun (client) toggles `player_zoom` between 1 and `zoomMult`.
* Starting a reload resets the zoom to 1.
* Every render tick: if the held item is a GenericGun with hold-mode zoom, zoom = mult while sneaking, else 1 (no gun uses hold mode). If the gun's `isZooming()` (player_zoom == its own mult) is false, zoom = 1. If no gun is held, zoom = 1. Switching to a different gun therefore cancels the zoom unless both guns have the same multiplier.

Zoom multipliers: Bolt-action 0.35, AS50 0.35, Pulse Rifle 0.35, Blaster Rifle 0.5, M4 Infiltrator 0.5, AUG 0.5, SCAR 0.65, M4 0.75, Laser Gun 0.75, Tesla Gun 0.75, LMG 0.75, Scatter Beam 0.75, Vector 0.75.

### 9.2 Scope overlay

Only renderers built with `hasZoom = true` replace the gun model with an overlay while zoomed: Bolt-action, AS50, Blaster Rifle, Pulse Rifle, AUG, SCAR, M4 Infiltrator. The others (M4, Laser Gun, Tesla Gun, LMG, Scatter Beam, Vector) only narrow the FOV and keep drawing the model. Their iron sights or red dots are the "sight". RenderM4 and RenderVector still define a `renderScope()` that is never reached.

While zoomed, the gun model, muzzle flash and extra effects are not drawn. On part 0 the scope quad is drawn in the muzzle frame (§6.2) plus T(1.795, 0, −0.685):

* **sniperScope**: [`textures/fx/Testscope.png`](../04-projectiles-fx/textures.md#single-images), scale `SNIPER_SCOPE_SCALE` = 2.75, no extra rotation, current blend state.
* **techScope** (Pulse Rifle): [`textures/fx/techscope.png`](../04-projectiles-fx/textures.md#single-images), scale 2.25, rotY(90). The quad is drawn with alpha blending (SRC_ALPHA, ONE_MINUS_SRC_ALPHA) inside a pushed ENABLE_BIT.

Recoil while scoped never uses the firing transform. Instead the scope quad is shifted (§9.3).

### 9.3 Scope recoil

While a recoil window is live and the overlay is shown: r = 5p for p < 0.2, else 1 − 1.25(p − 0.2) (0 → 1 at p = 0.2 → 0 at p = 1). Then T(a·r, b·r, c·r) with (a, b, c):

| Renderer | (a, b, c) |
|---|---|
| default (Bolt-action, Blaster Rifle) | (0.2, 0.25, 0.10) |
| AS50 | (0.25, 0.5, 0.1) |
| AUG, SCAR | (0.15, 0.20, 0.10) |
| M4 Infiltrator | (0.1, 0.15, 0.05) |
| Pulse Rifle | (0.10, 0.10, 0) |

In the rotated muzzle frame, X is screen up/down. The overlay therefore kicks upward and slightly sideways.

[`ModelSniperScope`](gun-models.md#modelsniperscope-unused) (a single 1080×1080×0 box with no texture size set) is an abandoned scope model. It is not referenced anywhere.

---

## 10. Ammo- and state-dependent geometry

| Model | Rule |
|---|---|
| ModelRocketLauncher (rocket and nuke launcher) | The rocket in the tube (boxes R1–R7) is drawn only if `ammoLeft > 0` **and** `reloadProgress > 0.5`. It is hidden when empty and during the first half of a reload, and reappears halfway through the reload. Outside reloads reloadProgress is the sentinel 10, so the rocket shows whenever loaded, in every view. |
| ModelLMG | The six visible belt rounds Bullet01…[06](../06-items/index.md) are drawn only while `reloadProgress > 0.5` (always, outside reloads). Bullet *k* is drawn only if ammoLeft > *k* (Bullet06 if > 6 … Bullet01 if > 1). The belt therefore visibly empties over the last six rounds, and the belt is hidden in the first half of a reload. |
| ModelM4_UQ (Infiltrator) | Two crossed 60-unit laser-beam planes (LaserBeam, LaserBeam01) extend 60 units (3.75 blocks of model space) forward from the laser box in every render type **except** INVENTORY. They are textured from the red top row of the texture (see textures.md). |
| ModelChainsaw | Two chain/blade planes alternate: with i = round(fireProgress × 40) mod 2, blade1 (i = 0) or blade2 (i = 1). While recoiling (each swing or tick of use) the chain therefore flickers between two tooth patterns about 40 times per window. Idle shows blade1. |
| ModelFragGrenade(showRing) | The pull ring is drawn only if the instance was built with showRing (the item renderer is, the thrown projectile is not) **and** fireProgress ≤ 0. In first person fireProgress = right-click hold progress, so the ring disappears once the pin is pulled. |
| ModelNDR | Body plus the GLOW box are always drawn (glow full bright). When fireProgress > 0 (first person while recoiling, third person while recoiling) it also draws the "crown": glowA1 (7×5×1 panel) rendered 8 times at X rotations 25°, 63°, 117°, 155°, −155°, −117°, −63°, −25° around the barrel axis. Back-face culling is off and alpha blending on, and the whole crown is scaled by s = 0.90 + 0.1·sin(2π·fireProgress) (pulsing). |
| ModelAS50Mag(empty) / ModelM4Mag vs ModelM4MagE / ModelLmgMag vs ModelLmgMagE | The "full" magazine item models add visible cartridges (AS50: 2 rounds. M4: one round on top. LMG: 6 belt rounds). The empty variants omit them. |
| Glow parts | Bio Gun (Glow1–5), Cyberdemon Blaster (Glow1–4), Laser Gun (Glow1, 2, 3, 5), NDR (GLOW), Sonic Shotgun (GLOW01) and Tesla Gun (Glow1–8 and Shape43) are drawn between `enableFXLighting`/`disableFXLighting`. They are full bright regardless of world light. |

---

## 11. Akimbo

`RenderGunAkimbo` passes `parts × 2` to the base. Part indices ≥ partsPerGun belong to the second (left) gun. Every transform hook receives the folded part index plus a `leftGun` flag. `ModelAkimbo` folds the index for the model in the same way.

* **First person:** both guns are drawn. The left gun gets an extra sideways translation after the base scale (Pistol/Mac-10 +1.85 X, MIB +2.05 X, Revolver +1.75 Z).
* **Which gun animates:** for alternating akimbo (pistol, revolvers, MIB; created with `setAkimbo(false)`), each shot's attackType = ammo % 2 picks the gun. Only the firing transform for the matching gun applies, and the muzzle flash is drawn at that gun's position. The Mac-10 akimbo (`setAkimbo(true)` = doubleShot) fires both guns on every shot: both animate and both muzzle flashes are drawn.
* **Third person (right hand):** the base renderer draws *all* parts (both guns) at the same right-hand transform. Recoil applies only when attackType = 0 (Mac-10: always). The second copy coincides exactly with the first, so it is a harmless duplicate draw.
* **Third person (left hand):** an event handler (`RenderPlayerEvent.Specials.Pre`) draws the off-hand gun on the player model's left arm through [`RenderAkimboGun`](../05-armor/rendering-and-models.md#3-special-slot-rendering-on-the-player-rendereradditionalslots) (package `client.renderer.additionalslots`, shared with the back-slot renderers of area [05](../05-armor/index.md)). It binds a hard-coded texture, applies the left-arm post-render transform, then T(0, 0.3, 0), rotX(−90), a per-gun scale and a per-gun translate. It draws `parts` passes, each with its own reload or recoil transform:

| Off-hand renderer | Texture | Scale | Translate | Extra rotation | Recoil (left gun) | Reload |
|---|---|---|---|---|---|---|
| RenderAkimboGun (pistols, 2 parts) | textures/guns/pistol3.png | 0.55 | (0.2, −0.5, −2.0) | – | only when attackType = 1: about pivot Z 0.7: rotX(−30·D30) | about pivot (0, 0.3, 0.9): rotX(−40·yR) |
| RenderAkimboRevolver (revolver, golden) | revolver.png / goldenrevolver.png | 0.3 | (2.7, −0.55, 0.15) | rotY(90) | when attackType = 1: rotZ(−30·D30) | about pivot (0, 0.3, 0.9): rotZ(−40·yR) |
| RenderAkimboMac10 (doubleShot) | mac10texture.png | 0.5 | (0.2, −0.45, −2.15) | – | every shot: about pivot Z 0.7: rotX(−15·D30) | as base |
| RenderAkimboMibGun | mibgun.png | default 0.55 | (0.1, −0.7, −1.7) **plus the debug keybind offset (X, Y, Z)** | – | when attackType = 1: about pivot (0, 0.3, 0.5): rotX(−15·K) | about pivot (0, 0.5, 0.3): rotX(−35·yR) |

---

## 12. Grenade renderers (`RenderGrenade`)

The texture argument *is* used here: [`textures/guns/stielgranate.png`](textures.md#measured-colors-all-62-textures) and [`textures/guns/fragGrenade_texture.png`](textures.md#measured-colors-all-62-textures). There are no timers. In first person, prog = item-in-use ticks ÷ 20, clamped to [0,1] (the throw wind-up). Every view calls the model's extended render with part 0, ammo 0, reloadProgress 0 and fireProgress = prog (0 outside first person).

| | Stielhandgranate | Frag grenade |
|---|---|---|
| Common base ("Translate") | rotZ(180), rotY(−45), S(1.15) | rotZ(180), rotY(−45) |
| First person | base, T(−0.2, 0.2, 0.7), rotZ(30·prog) (tilts back as you wind up) | base, S(1.5), T(−0.2, 0.5, 0.7), rotZ(20·prog) |
| Third person | base, T(0, −0.1, 1.7), rotX(60) | base, S(1.8), T(0, 0.2, 1.3), **T(debug keybind X, Y, Z)**, rotX(60), rotY(−90) |
| Dropped | base | base, S(1.15), T(0, −0.5, 0) |
| Inventory | base, T(0, 1.0, 0) | base, rotY(−125), S(2.75), T(0, 0.9, 0) |

The same models, with `ModelFragGrenade(false)` (no ring), render the thrown projectiles. That projectile rendering is area 04.

---

## 13. Magazine and rocket item renderers (`RenderGenericItem`)

[`TGItems.sharedItem`](../06-items/item-catalog.md) is a metadata item (area [06](../06-items/index.md)). Its renderer claims only these metas and draws a 3D model. All other metas fall back to normal 2D icons. Metas below were computed from registration order in `TGItems` (verify against area [06](../06-items/index.md)).

| Shared item | meta | Renderer | Model | Texture |
|---|---|---|---|---|
| m4Mag | 12 | RenderM4Mag | ModelM4Mag (with top round) | textures/guns/m4Texture.png |
| m4MagEmpty | 13 | RenderM4Mag | ModelM4MagE | textures/guns/m4Texture.png |
| rocketAmmo | 14 | RenderRocketAmmo | ModelRocket | textures/guns/rocketlauncher.png |
| as50Mag | 17 | RenderAS50Mag | ModelAS50Mag(false) (2 rounds) | textures/guns/as50Texture.png |
| as50MagEmpty | 18 | RenderAS50Mag | ModelAS50Mag(true) | textures/guns/as50Texture.png |
| lmgMag | 20 | RenderLMGMag | ModelLmgMag (6 rounds) | textures/guns/mg2_texture.png |
| lmgMagEmpty | 21 | RenderLMGMag | ModelLmgMagE | textures/guns/mg2_texture.png |
| rocketAmmoNuclear | 105 | RenderRocketAmmo | ModelRocket | textures/guns/nukerocketlauncher.png |

These renderers use the passed texture and the plain model render, with no animation. Transforms:

| Renderer | First person | Third person | Dropped | Inventory |
|---|---|---|---|---|
| RenderM4Mag | rotX180, rotY225, T(−0.3, −1.5, 0.6) | rotX180, rotY135, S(0.8), T(0.1, −1.3, 1.35) | rotX180, S(0.65), T(0, −0.65, 0) | rotX180, S(2), T(0, −0.45, 0.1) |
| RenderAS50Mag | rotX180, rotY225, T(−0.3, −1.2, 0.6) | rotX180, rotY135, S(0.8), T(0.1, −1.15, 1.45) | rotX180, S(0.8), T(0, −0.35, 0.2) | rotX180, S(1.75), T(0, −0.20, 0.4) |
| RenderLMGMag | rotX180, rotY225, T(−0.3, −1.5, 0.6) | rotX180, rotY135, S(0.8), T(0.1, −1.3, 1.35) | rotX180, rotY90, T(0, −0.45, 0), S(0.65) | rotX180, S(1.35), T(0, −0.45, 0.1) |
| RenderRocketAmmo | T(1.0, 0, 0.5), rotZ(90), S(1.2) | rotX180, rotY225, S(1.5), T(−0.5, −0.25, 0) | rotX180, rotY90 | rotX180, S(1.45), T(0, 0, −0.025) |

The magazine models reuse the exact boxes, and UV positions, of the magazine on the corresponding gun model. They therefore texture correctly from the gun's sheet.

---

## 14. Sound playback mechanics (summary)

Full catalog in [sounds.md](sounds.md). Gun sounds are [`TGSound`](../01-core/client-effects-audio.md#tgsound) moving sounds:

* **Fire / loop-start / rechamber:** [`SoundUtil.playSoundOnEntityGunPosition`](../01-core/utilities.md#soundutil). Server side, it sends [`PacketPlaySound`](../01-core/networking.md#9--packetplaysound-sc) to every client within 100 blocks. Clients other than the shooter (`playOnOwnPlayer` is false for these) play it. On the shooting client the same call runs locally, so the shooter hears it without waiting for the server. Volume: `SOUND_DISTANCE` = 4.0 for fire sounds (with vanilla attenuation that is audible to about 64 blocks), 1.0 for rechamber. Pitch 1. "Gun position" = 1 block in front of the entity along its head yaw, at half its height. The sound follows the entity only if *moving* is set (fire sounds are not moving, so they stay where they were emitted).
* **Reload:** `SoundUtil.playReloadSoundOnEntity`: same packet path, moving = true. Locally it is suppressed if the previous local reload sound started less than 500 ms ago (prevents double play).
* Volume is passed through [`TGConfig.getVolumeMultiplier(volume, category)`](../01-core/config.md) (area [01](../01-core/index.md)). *Correction (consistency pass):* in v0.1.2 that method returns the volume unchanged, so there is no per-category volume config. The category is plumbing for an unfinished feature. Categories: GUN_FIRE (fire, loop, swing), RELOAD (reload, rechamber), PLAYER_EFFECT, EXPLOSION, MACHINE, DEATHEFFECT.
* Charge guns (Power Hammer, Bio Gun) use plain `world.playSoundAtEntity(player, …, 1.0, 1.0)` for fire and reload. These are vanilla positional sounds, not TGSounds.
* Out of ammo with no reserve: vanilla `mob.villager.idle` at volume 1 (server).
* **Audible range (added in the consistency pass):** Minecraft 1.7.10 attenuates linearly to max(1, volume) × 16 blocks. A fire sound at 4.0 therefore carries 64 blocks, but the packet only reaches clients within 100 blocks (swing packets: 50). Reload and rechamber sounds at 1.0 carry 16 blocks. Stereo files are the exception: the sound engine plays them without positional attenuation (see `GVA-23`).
