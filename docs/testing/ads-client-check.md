# Aim down sights: client check

For the owner or the batched client pass. Unit tests cover the math (the sights land on the screen center, the blend curve, the spread and the aim state machine), but nothing here has been seen on screen yet. Spec: [03-gun-visuals-audio/ads.md](../gregguns-spec/03-gun-visuals-audio/ads.md). Run it with no shader pack, then again with the pack's shader (Complementary Unbound plus Euphoria Patches).

## Per gun (16 guns)

9mm Pistol, Golden Gun, Revolver, Golden Revolver, Thompson, Mac10, PDW, Combat Shotgun, Grenade Launcher, AK47, M4, Vector, LMG, Laser Rifle, Tesla Rifle, Scatter Laser Rifle. For each, hold it, right-click once, wait half a second, and look:

1. The sights (or the red dot) sit on the middle of the screen, on the spot the crosshair was. Note the gun if they sit visibly high, low, left or right: the fix is that gun's nudge in `AdsTable` (texels).
2. The gun does not clip into the near plane, grow or shrink, and the sights are not hidden behind a thick part of the model.
3. The muzzle flash still sits on the muzzle when you fire while aiming.
4. Right-click again: the gun slides back to the normal hold with no jump.

## Behavior

- Field of view narrows a little on a gun with no scope (pistols, AK47, Thompson, Mac10, PDW, shotgun, grenade launcher) and keeps its own x0.75 on the M4, Vector, LMG, Laser, Tesla and Scatter Laser. It eases in and out.
- The crosshair disappears once the gun is nearly in place and returns when you let go.
- Sprint (hold the sprint key while walking, then right-click): you drop to walking and stay there while aiming. After you stop aiming, press the sprint key again; it should sprint normally. Look for any case where the key feels stuck.
- Ends cleanly, with no leftover shift, FOV or missing crosshair: swap to another slot while aiming; press reload while aiming; die while aiming and respawn; open the inventory while aiming (the gun eases back, and returns when you close it); switch to third person (F5) and back.
- Fire while aiming with a sights-only gun: the spread is visibly tighter (use the Thompson or Mac10 at 20 blocks against a wall).
- The scoped guns (Bolt Action, AS50, AUG, Infiltrator, SCAR, Pulse Rifle, Blaster Rifle) behave exactly as before; the Sawed Off, Hand Cannon, flamethrower and akimbo guns do nothing on right-click.
- Config: set `client.adsEnabled` false and confirm nothing moves and the right-click does nothing on a pistol; set `aim.adsEnabled` false on the server and confirm the same for a connected client.
- Multiplayer: another player watching you aim sees no change to your third-person pose (ADS is first person only).

## Shaders

With the shader pack on: the gun and its muzzle flash render in place while aiming, with no black or white flash, no missing hands and no changed brightness of the world when you start or stop aiming. Compare the frame against the same gun held normally.

## With Backhand

With Backhand loaded and an item in the offhand, right-click a pistol: it aims and the offhand item is not used.
