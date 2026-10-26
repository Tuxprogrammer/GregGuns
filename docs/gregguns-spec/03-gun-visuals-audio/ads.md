# Aim down sights (ADS)

M9 polish (design record [0015](../../design/0015-m9-self-contained-polish.md)). Techguns has no aim down sights: right-click only zooms a scoped gun, and the model never moves. This is a GregGuns addition, so it is a deviation note and not a port.

## What the player sees

Right-click a gun that has iron sights or a red dot and no scope overlay. The gun slides to the middle of the screen with its sights lined up on the crosshair spot, the field of view narrows a little, the crosshair hides, and sprinting stops. Right-click again to let go. This is the input scoped guns already use for zoom, and it is a toggle like theirs.

| Situation | What happens |
|---|---|
| Weapon swap, reload start, death, world unload | The aim ends at once, with no animation (server resets its state; the client resets its blend). |
| A GUI opens, or the camera goes third person | The picture eases back to the normal hold. The server keeps the aim, so it comes back when the GUI closes. |
| `client.adsEnabled` false | The gun is not moved, the right-click is not sent for sights-only guns, and no FOV or crosshair change is made. |
| `aim.adsEnabled` false (server) | The server ignores the request, and drops an aim already running. A gun's own scope zoom is unaffected. |
| `modules.guns` false | Guns do nothing at all, so there is nothing to aim. |
| Backhand loaded | No change needed: `CompatConfig` already puts every GregGuns gun on `offhandBlacklist` and `mainhandUseStopsOffhandFallback`, which covers this right-click. |

## Which guns

`AdsSupport.GUNS` (server and client share it) and `AdsTable.ALL` (the client data). A test pins the two together.

| Gun | Sights | Scope FOV and spread of its own |
|---|---|---|
| 9mm Pistol (`pistol_9mm`) | rear and front | no |
| Golden Gun (`golden_gun`) | rear and front (the pistol's model) | no |
| Revolver (`revolver`) | front blade | no |
| Golden Revolver (`golden_revolver`) | rear and front | no |
| Thompson SMG (`thompson_smg`) | leaf rear, front post | no |
| Mac10 (`mac10`) | post and blade | no |
| PDW (`pdw`) | rear ears, front post | no |
| Combat Shotgun (`combat_shotgun`) | rear and front | no |
| Grenade Launcher (`grenade_launcher`) | rear sight | no |
| AK47 (`ak47`) | leaf and front tip | no |
| M4 (`m4`) | red dot | yes, x0.75 FOV and spread |
| Vector (`vector`) | red dot | yes, x0.75 FOV, x0.35 spread |
| LMG (`lmg`) | post and cap | yes, x0.75 |
| Laser Rifle (`laser_rifle`) | red dot | yes, x0.75 |
| Tesla Rifle (`tesla_rifle`) | red dot | yes, x0.75 FOV, x1.0 spread |
| Scatter Laser Rifle (`scatter_laser_rifle`) | glow window | yes, x0.75 |

Not listed: guns with a scope overlay (Bolt Action Rifle, AS50, AUG, Infiltrator, SCAR, Pulse Rifle, Blaster Rifle), which keep their scope zoom; the akimbo guns, which cannot share one line of sight; and guns with no sights (Sawed Off, Hand Cannon, launchers, flamethrower, minigun, tool guns, the exotic energy guns).

## Rules

- **The server owns the aim.** `ZoomState` gains an `ads` flag next to `zoomed`. The right-click sends the existing `PacketGunZoom` (no flag in it, GUN-12); `ServerGunHandler.onZoomToggle` calls `GunController.toggleAim`, and the answer goes out as the existing `ZOOM` event, which now means "aiming" (scope zoom or ADS). No new packet.
- **Spread.** A gun with a scope keeps its spec `zoomSpreadMultiplier`; ADS adds nothing on top. A sights-only gun gets `aim.adsSpreadMultiplier` (default 0.75, 25 percent tighter), applied through `ShotContext.withAdsSpread` and `SpreadModel`. It multiplies with the armor accuracy bonus like the zoom bonus does. NPCs never aim.
- **FOV.** A sights-only gun narrows by `client.adsFov` (default 0.9) through `FOVUpdateEvent`, so it multiplies with Angelica's zoom key. A gun that already has a scope FOV keeps that FOV and ADS adds only the model move.
- **Speed.** `client.adsTransitionTicks` (default 5) is the length of the move in and out, eased with a smoothstep (`AdsBlend`).
- **Movement.** There was no slow-down convention for scoped guns, so ADS does not slow walking. It stops sprinting: while aiming, the sprint flag and the sprint key are cleared at the start of the player's tick (`client.adsStopsSprint`). The key reads as released until pressed again, as in vanilla.
- **Crosshair.** Cancelled on the `CROSSHAIRS` overlay element once the blend passes half (`client.adsHideCrosshair`), first person only.

## How the model moves

Per-gun data is in `AdsTable`: the names of the model boxes that form the sights, and an optional nudge in texels. The sight point is the center of those boxes in the model's own axes.

`AdsOffset` evaluates the gun's own first-person rows (`fpBase`, `fpIdle`) on top of the shared item frame (`InHandFit.firstPersonItemFrame`: camera offset 0.56, -0.52, -0.72, rotY 45, scale 0.4) to find where the sight point sits in eye space, then computes the translation in the item frame that moves it to x = 0, y = 0 at the same depth. Keeping the depth means the gun neither grows nor shrinks. `GunItemRenderer` multiplies that translation by the eased blend and applies it inside its own matrix push, before the gun's rows. So:

- it works with every existing first-person row (idle, fire, reload, charge) unchanged;
- the muzzle flash, drawn in the same frame, moves with the gun;
- it changes no GL state, so a shader pack sees nothing new (the shader GL-state fix, branch `fix/shader-gl-state`, is already on master; the code here only calls `glTranslated` between the renderer's own `glPushMatrix` and `glPopMatrix`).

The sight point and offsets are computed from the model, not hand-tuned. A gun that sits a little off after a client check is fixed by its nudge in `AdsTable`, not in the renderer.

## Config

| Key | Default | Side |
|---|---|---|
| `aim.adsEnabled` | true | server |
| `aim.adsSpreadMultiplier` | 0.75 | server |
| `client.adsEnabled` | true | client |
| `client.adsFov` | 0.9 | client |
| `client.adsTransitionTicks` | 5 | client |
| `client.adsHideCrosshair` | true | client |
| `client.adsStopsSprint` | true | client |

See [config.md](../../config.md).

## Code and tests

`AdsSupport`, `ZoomState`, `ShotContext`, `SpreadModel` (common, pure); `AdsBlend`, `AdsOffset`, `AdsSpec`, `AdsTable` (client, pure); `AdsController`, `GunItemRenderer`, `GunClientForgeEvents`, `ClientGunEvents` (client, thin). Tests: `AdsLogicTest`, `AdsBlendTest`, `AdsOffsetTest`. What a person must check in game: [docs/testing/ads-client-check.md](../../testing/ads-client-check.md).
