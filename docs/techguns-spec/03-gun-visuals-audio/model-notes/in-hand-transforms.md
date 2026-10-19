# In-hand transforms: how Techguns holds, scales and places each gun

Dirty-room notes. Facts only: numbers, no code. Measured from the Techguns renderers and models, then compared with the GregGuns profiles in `src/main/java/com/gregguns/client/render/`.

The M2 guns come first in every table. The M5 guns are appended below them, in the same method and columns: Laser Rifle, Tesla Rifle, Sonic Rifle (Sonic Shotgun), Blaster Rifle, Deatomizer Pistol (the MIB gun) with its akimbo variant, Scatter Laser Rifle (Techguns' second laser rifle), Nether Blaster (the cyberdemon blaster), Alien Blaster, Grim Reaper, Bio Gun, Power Hammer, Chainsaw, Nuclear Death Ray, Nuke Launcher, PDW, Pulse Rifle and Minigun. Every M5 number was computed from the Techguns transform rows and the model boxes (every box, tilted ones and glow parts included, conditional effect parts left out). The same computation reproduces the existing M2 rows for the M4, AK-47, Rocket Launcher and Thompson (centre, muzzle and rear depth, to the second decimal), which is the check on the method.

## 0. Conventions

- **Model units.** One box unit (texel) is 1/16 of a block. Every Techguns gun is drawn at that scale. The GregGuns models use the same 1/16 (the Mac-10 and the Vector use 1/32, so their box numbers are doubled).
- **Model axes.** Minecraft model space: +x right, +y **down**, +z back. Two authoring families exist:
  - **X family.** The gun lies along x. The muzzle is at +x. First-person rotation is "rotate X 180, then rotate Y -45".
  - **Z family.** The gun lies along z. The muzzle is at -z (the long end). First-person rotation is "rotate X 180, then rotate Y 225".
  - Both families end up with the muzzle pointing straight away from the camera in first person.
  - The grenade launcher is a Y-up model file. It uses "rotate Y 45" only, with +x forward.
- **Net matrix.** The rotations come first (outermost), then a uniform scale S, then a translate T. T is applied *inside* the scale, so a T of 1.0 moves the gun by S blocks. All "T" numbers below are written that way, as the renderer writes them.
- **Item frame (H).** Before the gun's own transforms, the game and Forge supply a shared frame:
  - First person: camera offset (0.56, -0.52, -0.72), then rotate Y 45, then scale 0.4, then a shift of (-0.5, -0.5, -0.5) because the renderer asks for the block render helper.
  - Third person: the hand frame is scaled 0.375, with the same -0.5 shift.
  - "H-blocks" below means item-frame units, before the 0.4 (first person) or 0.375 (third person) factor. **Camera blocks** are H-blocks times 0.4. **World blocks** are H-blocks times 0.375.
  - Check on this frame: Techguns' akimbo left pistol is placed 3.7 H-blocks sideways from the right one, and the numbers above put it at exactly mirrored camera x (+0.74 and -0.74). That supports the frame constants.
- **Camera coordinates.** (right, up, depth). Depth is the distance in front of the camera. The camera looks down -z. The near plane is about 0.05, and the vertical field of view is 70 degrees at default settings.
- **Muzzle and rear points.** The centre of the barrel tip slice and of the opposite tip slice of the model's bounding box. The centre is the box centre. These are approximations: the box includes magazines and stocks.
- **Resting pose.** Techguns adds no idle sway and no bob of its own. The only motion that changes the pose is the shot kick (section 6) and the reload swing. Vanilla hand bobbing and the equip lift still apply to both sides.

## 1. Model size: ours equals Techguns (this is the root fact)

For every gun on the M2 list, our box length along the barrel matches the Techguns model within about 2 percent (the Infiltrator is the exception, below). Our models are not "larger" than theirs.

| Gun | TG length (texels) | Ours (texels) |
|---|---|---|
| Hand Cannon | 39 | 40 |
| Revolver | 28 | 28 |
| Golden Revolver | 29 | 29 |
| Sawed-off | 39 | 39 |
| 9mm Pistol | 15 | 15 |
| Mac-10 | 22 | 22 |
| Thompson | 52 | 52 |
| M4 | 69 | 69 |
| AK-47 | 98 | 98 |
| AUG | 70 | 70 |
| SCAR | 72 | 72 |
| Vector | 44 | 44 |
| AS50 | 100 | 100 |
| LMG | 73 | 73 |
| Combat Shotgun | 55 | 55 |
| Rocket Launcher | 48 | 48 |
| Grenade Launcher | 44 | 44 |
| Flamethrower | 43 | 42 |
| Bolt-action | 53.5 | 53.5 |
| Infiltrator | 109 (union of every part, including the extended front) | 76 (rifle part only) |
| Laser Rifle | 53 | 53 |
| Tesla Rifle | 49.5 | 49.5 |
| Sonic Rifle | 59 | 59 |
| Blaster Rifle | 50 | 50 |
| Deatomizer Pistol (MIB gun) | 18 | 17.8 (18.35 counting the tilted trigger and rails) |
| Scatter Laser Rifle | 63 | 63 |
| Nether Blaster | 32.3 | 33.0 (32.3 counting the tilted parts) |
| Alien Blaster | 36 | 36 |
| Grim Reaper | 49 | 49 |
| Bio Gun | 38 | 38 |
| Power Hammer | 31 (head and body parts) | 31 |
| Chainsaw | 70 (either blade frame gives the same extent) | 70 |
| Nuclear Death Ray | 43 (the firing starburst does not change the extent) | 43 |
| Nuke Launcher | 48 (the Rocket Launcher model with a nuke skin) | 48 (the Rocket Launcher boxes) |
| PDW | 32 | 32 |
| Pulse Rifle | 48 | 47.4 (48.0 counting the tilted parts) |
| Minigun | 62 (body and rotor parts) | 62 (on the unmerged Minigun branch) |

So any first-person size difference comes from the scale in the transform, not from the model. The M5 guns agree too: the largest gap is the Nether Blaster at 2.4 percent.

## 2. First person

Columns:
- **TG S, idle T.** The scale and the idle translate in the renderer's own terms.
- **TG length.** The scaled barrel-axis length in H-blocks, with camera blocks in brackets.
- **Ours S, length.** Our first-person scale and the resulting length, in H-blocks.
- **Ratio.** Ours length divided by TG length.
- **Centre.** The box centre in camera coordinates (right, up, depth).

| Gun | TG rotate Y | TG S | TG idle T | TG length H (cam) | Ours S | Ours length H | Ratio | TG centre | Ours centre |
|---|---|---|---|---|---|---|---|---|---|
| Hand Cannon | -45 | 2.5 | 0.1, 0, -0.5 | 6.09 (2.44) | 0.375 | 0.94 | **0.15** | 0.75, -0.78, 0.98 | 0.55, -0.47, 0.94 |
| 9mm Pistol | 225 | 2.0 | -0.55, -0.45, -0.8 | 1.88 (0.75) | 1.0 | 0.94 | 0.50 | 0.74, -0.59, 1.14 | 0.76, -0.45, 1.29 |
| Revolver | -45 | 1.5 | 0.2, -0.5, -0.45 | 2.62 (1.05) | 0.536 | 0.94 | 0.36 | 0.51, -0.58, 1.18 | 0.71, -0.47, 1.22 |
| Golden Revolver | -45 | 1.5 | 0.2, -0.5, -0.45 | 2.72 (1.09) | 0.517 | 0.94 | 0.34 | 0.51, -0.58, 1.20 | 0.71, -0.47, 1.22 |
| Sawed-off | -45 | 2.2 | 0.1, -0.1, -0.5 | 5.36 (2.15) | 0.385 | 0.94 | **0.17** | 0.69, -0.66, 0.95 | 0.55, -0.47, 0.94 |
| Mac-10 | 225 | 2.0 | -0.55, -0.45, -0.8 | 2.75 (1.10) | 0.80 | 1.10 | 0.40 | 0.74, -0.71, 1.11 | 0.76, -0.45, 1.29 |
| Thompson | -45 | 1.8 | 0.1, -0.35, -0.5 | 5.85 (2.34) | 0.615 | 2.0 | 0.34 | 0.59, -0.63, 0.84 | 0.55, -0.47, 0.94 |
| M4 | 225 | 1.4 | -0.55, -0.35, -0.65 | 6.04 (2.42) | 0.464 | 2.0 | 0.33 | 0.60, -0.57, 1.31 | 0.55, -0.47, 0.94 |
| Infiltrator | 225 | 1.4 | -0.55, -0.35, -0.65 | 9.54 (3.81) | 0.421 | 2.0 | **0.21** | 0.60, -0.54, 2.01 | 0.55, -0.47, 0.94 |
| AK-47 | 225 | 1.4 | -0.55, -0.35, -0.65 | 8.57 (3.43) | 0.327 | 2.0 | **0.23** | 0.60, -0.67, 1.40 | 0.55, -0.47, 0.94 |
| AUG | 225 | 1.4 | -0.55, -0.35, -0.65 | 6.12 (2.45) | 0.457 | 2.0 | 0.33 | 0.59, -0.57, 1.29 | 0.55, -0.47, 0.94 |
| SCAR | 225 | 1.4 | -0.55, -0.35, -0.65 | 6.28 (2.51) | 0.446 | 2.0 | 0.32 | 0.59, -0.50, 1.37 | 0.55, -0.47, 0.94 |
| Vector | 225 | 1.4 | -0.65, -0.55, -0.65 | 3.85 (1.54) | 0.727 | 2.0 | 0.52 | 0.66, -0.61, 0.91 | 0.55, -0.47, 0.94 |
| AS50 | 225 | 1.4 | -0.55, -0.35, -0.65 | 8.75 (3.50) | 0.320 | 2.0 | **0.23** | 0.61, -0.36, 1.68 | 0.55, -0.47, 0.94 |
| LMG | 225 | 1.4 | -0.55, -0.35, -0.65 | 6.39 (2.55) | 0.438 | 2.0 | 0.31 | 0.62, -0.52, 1.35 | 0.55, -0.47, 0.94 |
| Combat Shotgun | -45 | 2.25 | 0.1, -0.25, -0.5 | 7.73 (3.09) | 0.582 | 2.0 | 0.26 | 0.70, -0.65, 1.06 | 0.55, -0.47, 0.94 |
| Rocket Launcher | -45 | 2.25 | 0.15, -0.25, -0.5 | 6.75 (2.70) | 0.667 | 2.0 | 0.30 | 0.70, -0.63, 1.14 | 0.55, -0.47, 0.94 |
| Grenade Launcher | 45 (no X flip) | 2.0 | 1.05, 0.1, 0.85 | 5.50 (2.20) | 0.727 | 2.0 | 0.36 | 0.96, -0.72, 1.36 | 0.55, -0.47, 0.94 |
| Flamethrower | -45 | 2.0 | 0.55, -0.3, -0.7 | 5.37 (2.15) | 0.762 | 2.0 | 0.37 | 0.81, -0.62, 1.33 | 0.55, -0.47, 0.94 |
| Bolt-action | -45 | 1.5 | -0.09, -0.37, 0.07 added to 0.1, -0.25, -0.5 | 5.02 (2.01) | 0.598 | 2.0 | 0.40 | 0.54, -0.35, 1.02 | 0.55, -0.47, 0.94 |
| Laser Rifle | -45 | 1.3 | 0.1, -0.75, -0.5 (a 0, -0.5, 0 step added to the 0.1, -0.25, -0.5 idle) | 4.31 (1.72) | 0.604 | 2.0 | 0.46 | 0.50, -0.40, 0.92 | 0.55, -0.47, 0.94 |
| Tesla Rifle | -45 | 1.3 | 0.1, -0.75, -0.5 (the same two steps) | 4.02 (1.61) | 0.646 | 2.0 | 0.50 | 0.52, -0.44, 0.68 | 0.55, -0.47, 0.94 |
| Sonic Rifle | -45 | 2.0 | 0.2, -0.2, -0.7 | 7.38 (2.95) | 0.542 | 2.0 | **0.27** | 0.79, -0.73, 1.16 | 0.55, -0.47, 0.94 |
| Blaster Rifle | 225 | 1.4 | -0.45, -0.45, -0.65 (a 0.1, -0.1, 0 step added to the -0.55, -0.35, -0.65 idle) | 4.38 (1.75) | 0.640 | 2.0 | 0.46 | 0.41, -0.49, 1.12 | 0.55, -0.47, 0.94 |
| Deatomizer Pistol | 225 | 2.5 | -0.75, -0.45, -0.8 | 2.81 (1.12) | 0.843 | 0.94 | **0.33** | 1.03, -0.76, 1.65 | 0.55, -0.47, 0.94 |
| Scatter Laser Rifle | 225 | 1.4 | -0.55, -0.35, -0.65 | 5.51 (2.20) | 0.508 | 2.0 | 0.36 | 0.59, -0.50, 1.21 | 0.55, -0.47, 0.94 |
| Nether Blaster | 225 | 2.0 | -0.85, -0.2, -0.75 | 4.03 (1.61) | 0.968 | 2.0 | 0.50 | 0.96, -0.49, 0.96 | 0.55, -0.47, 0.94 |
| Alien Blaster | 225 | 1.4 | -0.55, -0.35, -0.65 | 3.15 (1.26) | 0.417 | 0.94 | **0.30** | 0.60, -0.53, 1.08 | 0.55, -0.47, 0.94 |
| Grim Reaper | -45 (the model is also turned 180 about its vertical axis) | 2.1 | 0.25, -0.65, -0.6 | 6.43 (2.57) | 0.653 | 2.0 | **0.31** | 0.66, -0.41, 0.59 | 0.55, -0.47, 0.94 |
| Bio Gun | -45 | 2.0 | 0, -0.35, -0.4 | 4.75 (1.90) | 0.842 | 2.0 | 0.42 | 0.57, -0.78, 0.52 | 0.55, -0.47, 0.94 |
| Power Hammer | -45 | 1.5 | 0, -0.75, -0.3 | 2.91 (1.16) | 0.903 | 1.75 | 0.60 | 0.34, -0.40, 0.70 | 0.55, -0.47, 0.94 |
| Chainsaw | -45 | 1.35 | 1.1, 0, -1.6 | 5.91 (2.36) | 0.457 | 2.0 | **0.34** | 1.06, -0.58, 1.75 | 0.55, -0.47, 0.94 |
| Nuclear Death Ray | -45 | 1.1 | 0.4, -1.0, -0.4 (the 0.2, -0.5, -0.2 idle counted twice, see below) | 2.96 (1.18) | 0.744 | 2.0 | 0.68 | 0.45, -0.30, 0.61 | 0.55, -0.47, 0.94 |
| Nuke Launcher | -45 | 2.25 | 0.15, -0.25, -0.5 | 6.75 (2.70) | 0.667 | 2.0 | **0.30** | 0.70, -0.63, 1.14 | 0.55, -0.47, 0.94 |
| PDW | -45 | 2.1 | 0.7, -0.3, -0.6 | 4.20 (1.68) | 1.000 | 2.0 | 0.48 | 0.78, -0.70, 1.36 | 0.55, -0.47, 0.94 |
| Pulse Rifle | -45 | 2.1 | 0.7, -0.3, -0.6 | 6.30 (2.52) | 0.675 | 2.0 | 0.32 | 0.78, -0.66, 1.41 | 0.55, -0.47, 0.94 |
| Minigun | -45 | 1.5 | 0.7, -0.3, -0.6 | 5.81 (2.33) | 0.516 | 2.0 | 0.34 | 0.71, -0.53, 1.40 | 0.55, -0.47, 0.94 |

Our values come from the fit rule: a fixed on-screen length (0.9375 H-blocks for a handgun, 1.1 for the Mac-10, 2.0 for every long gun), with the box centre pinned to one anchor. That is why every long gun has the same centre and the same 2.0 length. The M5 rows follow the same rule with two differences: the Power Hammer's target is 1.75 (its profile picks that), and the Deatomizer Pistol and Alien Blaster use the handgun target of 0.9375. Every M5 profile uses the X-family hold (rotate Y -45) and the X-family anchor, including the models Techguns draws in the Z family, so the Ours centre is the same for all of them.

Notes on the M5 first-person rows:
- Where the Techguns first-person block adds its own translate before the idle one (Laser Rifle, Tesla Rifle, Blaster Rifle), the two are summed, as for the Bolt-action. Both act inside the scale.
- **Nuclear Death Ray.** Its first-person block applies the idle translate itself, and the shared renderer applies the idle translate again when nothing is firing or reloading. At rest the idle is therefore counted twice: (0.4, -1.0, -0.4). During a shot or a reload it is counted once, so the gun steps by (-0.2, +0.5, +0.2) inside the scale, which is (-0.22, +0.55, +0.22) H-blocks at scale 1.1, the moment firing starts. With a single idle the centre would be (0.37, -0.52, 0.52) and the rear depth -0.07. Whether the double step is intended is not known (section 8).
- **Power Hammer and Bio Gun.** Their first-person blocks also shift the model along the barrel while the charge builds: the hammer's body by up to 0.15 and its head by up to 0.45, the Bio Gun by up to 0.25, all times the scale and toward the camera. The rows above are the resting pose (charge 0).
- **Grim Reaper.** Its model draws every box after a 180 degree turn about the vertical axis, so x and z are negated before the scale and the idle step. The row uses the turned geometry. See section 8.
- **Nuke Launcher.** The same renderer, model and numbers as the Rocket Launcher; only the skin differs.
- **Minigun.** Ours comes from the Minigun branch that is not on master yet (commit 5ce048c). The figure is the rule's output and depends only on the model's length.

### Muzzle and rear placement (camera coordinates)

| Gun | TG muzzle (R, U, depth) | Ours muzzle | TG rear depth | Ours rear depth |
|---|---|---|---|---|
| Hand Cannon | 0.75, -0.72, 2.20 | 0.55, -0.47, 1.13 | -0.24 (behind the camera plane) | 0.76 |
| 9mm Pistol | 0.74, -0.44, 1.51 | 0.76, -0.45, 1.48 | 0.76 | 1.10 |
| Revolver | 0.51, -0.38, 1.70 | 0.71, -0.47, 1.41 | 0.65 | 1.04 |
| Sawed-off | 0.69, -0.55, 2.02 | 0.55, -0.47, 1.13 | -0.13 | 0.76 |
| Mac-10 | 0.74, -0.41, 1.66 | 0.76, -0.45, 1.51 | 0.56 | 1.07 |
| Thompson | 0.59, -0.40, 2.01 | 0.55, -0.47, 1.34 | -0.33 | 0.54 |
| M4 / AUG / SCAR | 0.60, -0.42, 2.52 to 2.62 | 0.55, -0.47, 1.34 | about 0.1 | 0.54 |
| AK-47 | 0.60, -0.45, 3.11 | 0.55, -0.47, 1.34 | -0.32 | 0.54 |
| AS50 | 0.60, -0.33, 3.43 | 0.55, -0.47, 1.34 | 0.07 | 0.54 |
| LMG | 0.60, -0.38, 2.62 | 0.55, -0.47, 1.34 | 0.07 | 0.54 |
| Combat Shotgun | 0.70, -0.41, 2.61 | 0.55, -0.47, 1.34 | -0.48 | 0.54 |
| Rocket Launcher | 0.70, -0.52, 2.49 | 0.55, -0.47, 1.34 | -0.21 | 0.54 |
| Flamethrower | 0.81, -0.47, 2.41 | 0.55, -0.47, 1.34 | 0.26 | 0.54 |
| Bolt-action | 0.52, -0.29, 2.02 | 0.55, -0.47, 1.34 | 0.01 | 0.54 |
| Laser Rifle | 0.50, -0.33, 1.78 | 0.55, -0.47, 1.34 | 0.06 | 0.54 |
| Tesla Rifle | 0.52, -0.35, 1.49 | 0.55, -0.47, 1.34 | -0.12 | 0.54 |
| Sonic Rifle | 0.79, -0.51, 2.63 | 0.55, -0.47, 1.34 | -0.32 | 0.54 |
| Blaster Rifle | 0.41, -0.38, 1.99 | 0.55, -0.47, 1.34 | 0.24 | 0.54 |
| Deatomizer Pistol | 1.03, -0.61, 2.21 | 0.55, -0.47, 1.13 | 1.08 | 0.76 |
| Scatter Laser Rifle | 0.59, -0.33, 2.31 | 0.55, -0.47, 1.34 | 0.10 | 0.54 |
| Nether Blaster | 0.96, -0.61, 1.77 | 0.55, -0.47, 1.34 | 0.16 | 0.54 |
| Alien Blaster | 0.60, -0.40, 1.71 | 0.55, -0.47, 1.13 | 0.45 | 0.76 |
| Grim Reaper (far tip, see section 8) | 0.66, -0.20, 1.88 | 0.55, -0.47, 1.34 | -0.70 | 0.54 |
| Bio Gun | 0.57, -0.54, 1.47 | 0.55, -0.47, 1.34 | -0.43 | 0.54 |
| Power Hammer (striker end) | 0.34, -0.33, 1.28 | 0.55, -0.47, 1.29 | 0.12 | 0.59 |
| Chainsaw (bar tip) | 1.06, -0.74, 2.93 | 0.55, -0.47, 1.34 | 0.57 | 0.54 |
| Nuclear Death Ray | 0.45, -0.28, 1.20 | 0.55, -0.47, 1.34 | 0.02 | 0.54 |
| Nuke Launcher | 0.70, -0.52, 2.49 | 0.55, -0.47, 1.34 | -0.21 | 0.54 |
| PDW | 0.78, -0.55, 2.20 | 0.55, -0.47, 1.34 | 0.52 | 0.54 |
| Pulse Rifle | 0.78, -0.55, 2.67 | 0.55, -0.47, 1.34 | 0.15 | 0.54 |
| Minigun (muzzle tips) | 0.71, -0.54, 2.57 | 0.55, -0.47, 1.34 | 0.24 | 0.54 |

How the Techguns guns look on screen (vertical FOV 70, 16:9):
- Every long gun's rear end sits at or behind the camera plane. The gun enters from the lower-right screen corner, with the butt cut off by the near plane.
- Its muzzle ends up at roughly 0.15 to 0.3 right and 0.2 to 0.5 down in normalized screen coordinates. For example the Hand Cannon muzzle is at (+0.27, -0.47), and the AK-47 muzzle at (+0.16, -0.21).
- Ours: the long guns all show a short gun from (+0.81, -1.25) to (+0.33, -0.50). The whole gun is clustered in the lower right corner, about a third of the length.

### Akimbo, first person

Techguns draws the left gun as the same model shifted sideways inside the scaled frame:
- Pistol and Mac-10: +1.85 in the rotated model x.
- Revolvers: +1.75 in z.

Resulting centres (camera):

| Gun | Right gun | Left gun |
|---|---|---|
| Pistol (TG) | 0.74, -0.59, 1.14 | -0.74, -0.59, 1.14 |
| Revolver (TG) | 0.51, -0.58, 1.18 | -0.54, -0.58, 1.18 |
| Golden Revolver (TG) | 0.51, -0.58, 1.20 | -0.54, -0.58, 1.20 |
| Mac-10 (TG) | 0.74, -0.71, 1.11 | -0.74, -0.71, 1.11 |

The left gun is an exact lateral mirror of the right at equal depth.

**Deatomizer Pistol akimbo (M5).** Techguns draws the same model twice. The left gun gets a +2.05 step in the rotated model x, inside the 2.5 scale, before the idle step. That is 5.1 H-blocks sideways.

| Gun | Right gun | Left gun |
|---|---|---|
| Deatomizer Pistol (TG) | 1.03, -0.76, 1.65 | -1.02, -0.76, 1.65 |
| Deatomizer Pistol (ours, computed) | 0.55, -0.47, 0.94 | -0.74, -0.47, -0.34 |

- The Techguns pair is an exact lateral mirror at equal height and depth, as for the other akimbo guns. The muzzles are at (+1.03, -0.61, 2.21) and (-1.02, -0.61, 2.21), and both rear depths are 1.08. Both guns are fully visible, and their centres are 2.05 camera blocks apart.
- Ours: the same mirror-plane rule as above. The left gun's centre lands at depth -0.34, behind the camera plane. The mirror also turns the gun's axis sideways: its tips are at (-0.93, -0.42, -0.34) and (-0.54, -0.42, -0.34). It would not be visible. Computed, not yet seen in game. Needed: left gun at (-right.x, same y, same depth).
- Third person: the akimbo item has its own hold row (section 3). Techguns gives the second gun no separate third-person placement, so the two guns overlap on the same spot.

Ours mirrors in item-frame x about x = -1.4. The item frame is rotated 45 degrees from the screen, so this mirror plane is not the screen centre. By the same frame constants as above, our left gun lands at camera (-1.09, -0.45, depth **-0.55**) for the pistol and (-1.02, -0.47, +0.50) for the revolvers. In camera coordinates that is depth -0.55, so the left gun sits behind the camera plane and would not be visible. This is computed, not yet seen in game. Needed: left gun at (-right.x, same y, same depth).

## 3. Third person

The shared transform for every held gun, before the gun's own rows (identical on both sides):
1. Aimed pre-rotation: rotate X -45, rotate Y -18, rotate Z 45, rotate Y 45.
2. Translate (0, -1.3, -0.4), plus the gun's aim extra.
3. Rotate Y -45.

The gun rows follow: "rotate X 180, rotate Y a, scale S, translate T". In world terms the item frame is scaled 0.375.

| Gun | TG rotate Y | TG S | TG T | TG aim extra | TG length (world blocks) | Ours S | Ours T | Ours aim extra | Ours length (world) | Ratio | Centre shift, ours minus TG, along the barrel (world blocks) |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Hand Cannon | 225 | 1.5 | -0.3, -0.45, 0 | 0, -0.1, 0 | 1.37 | 0.85 | -1.4, -0.8, 0 | 0, -0.1, 0 | 0.80 | **0.58** | -0.36 |
| 9mm Pistol | 135 | 1.5 | 0.05, -0.55, 0.2 | 0, 0, 0.2 | 0.53 | 1.5 | 0.05, -0.55, 0.2 | 0, 0, 0.2 | 0.53 | 1.00 | -0.04 |
| Revolver | 225 | 0.85 | -1.4, -0.8, 0 | 0, -0.05, 0.2 | 0.56 | 0.85 | same | same | 0.56 | 1.00 | -0.01 |
| Golden Revolver | 225 | 0.85 | -1.4, -0.8, 0 | 0, -0.05, 0.2 | 0.58 | 0.85 | same | same | 0.58 | 1.00 | -0.01 |
| Sawed-off | 225 | 1.5 | -0.5, -0.25, 0 | 0, 0.15, 0 | 1.37 | 0.7 | -1.4, -0.8, 0 | 0, 0.15, 0 | 0.64 | **0.47** | -0.03 |
| Mac-10 | 135 | 1.2 | 0.05, -0.6, 0.4 | 0, 0, 0.2 | 0.62 | 1.0 | 0.05, -0.6, 0.4 | 0, 0, 0.2 | 0.52 | 0.83 | -0.06 |
| Thompson | 225 | 1.15 | -0.85, -0.6, 0 | 0, 0, 0 | 1.40 | 1.15 | same | same | 1.40 | 1.00 | +0.66 |
| M4 | 135 | 0.8 | 0.1, -0.6, 1.1 | 0, 0.1, 0 | 1.29 | 0.9 | -1.2, -0.75, 0 | 0, 0.1, 0 | 1.46 | 1.12 | +0.52 |
| Infiltrator | 135 | 0.8 | 0.1, -0.6, 1.1 | 0, 0.1, 0 | 2.04 | 0.9 | -1.2, -0.75, 0 | 0, 0.1, 0 | 1.60 | 0.78 | +0.23 |
| AK-47 | 135 | 0.7 | 0.1, -0.8, 1.35 | none | 1.61 | 0.6 | -1.9, -0.6, 0 | 0, 0.2, -0.25 | 1.38 | 0.86 | +0.56 |
| AUG | 135 | 0.8 | 0.1, -0.6, 1.4 | 0, 0.1, 0 | 1.31 | 0.9 | -1.6, -0.75, 0 | 0, 0.1, 0 | 1.48 | 1.12 | +0.49 |
| SCAR | 135 | 0.8 | 0.1, -0.6, 1.1 | 0, 0.1, 0 | 1.35 | 0.8 | -1.25, -1.0, 0 | 0, 0.2, -0.25 | 1.35 | 1.00 | +0.57 |
| Vector | 135 | 0.8 | 0.1, -0.7, 1.1 | 0, 0.1, 0 | 0.83 | 0.9 | -1.2, -0.75, 0 | 0, 0.1, 0 | 0.93 | 1.12 | +0.47 |
| AS50 | 135 | 0.7 | 0.1, -0.5, 1.35 | 0, 0.2, 0 | 1.64 | 0.7 | -1.6, -1.3, 0 | 0, 0.2, -0.25 | 1.64 | 1.00 | +0.58 |
| LMG | 135 | 0.8 | 0.1, -0.65, 1.1 | 0, -0.05, -0.1 | 1.37 | 0.8 | -1.3, -0.6, 0 | 0, 0.2, -0.25 | 1.37 | 1.00 | +0.54 |
| Combat Shotgun | 225 | 1.15 | -0.8, -0.4, 0 | 0, 0.25, -0.25 | 1.48 | 1.15 | -0.95, -0.5, 0 | 0, 0.2, -0.25 | 1.48 | 1.00 | +0.55 |
| Rocket Launcher | 225 | 1.5 | -1.2, -1.8, 0 | 0, -1.8, 0.2 | 1.69 | 1.4 | -1.7, -0.4, 0 | 0, 0.2, -0.25 | 1.57 | 0.93 | +0.56 |
| Grenade Launcher | 135 (no X flip) | 1.3 | -0.2, 0.35, 0 | 0, 0.15, 0 | 1.34 | 1.1 | -0.85, -0.7, 0 | 0, 0.2, -0.25 | 1.13 | 0.85 | +0.53 |
| Flamethrower | 225 | 1.5 | -0.8, -0.6, 0 | 0, -0.15, -0.4 | 1.51 | 1.1 | -0.75, -0.6, 0 | 0, 0.2, -0.25 | 1.08 | 0.72 | +0.50 |
| Bolt-action | 225 | 1.5 | -0.65, -0.25, 0 | 0, 0.2, -0.25 | 1.88 | 1.2 | -0.65, -0.25, 0 | 0, 0.2, -0.25 | 1.50 | 0.80 | +0.54 |
| Laser Rifle | 225 | 1.25 | -0.7, -0.5, 0 | 0, 0.1, -0.2 | 1.55 | 1.2 | -0.55, -0.4, 0 | 0, 0.1, -0.2 | 1.49 | 0.96 | +0.08 |
| Tesla Rifle | 225 | 1.25 | -0.7, -0.5, 0 | 0, 0.2, -0.55 | 1.45 | 1.25 | 0.75, -0.5, 0 | 0, 0.2, -0.25 | 1.45 | 1.00 | +0.57 |
| Sonic Rifle | 225 | 1.25 | -0.85, -0.6, 0 | 0, 0.1, 0.2 | 1.73 | 1.25 | 0.35, -0.45, 0 | 0, 0.2, -0.25 | 1.73 | 1.00 | +0.73 |
| Blaster Rifle | 135 | 0.95 | 0.1, -0.5, 0.9 | 0, 0.05, 0.2 | 1.11 | 1.2 | -0.55, -0.4, 0 | 0, 0.1, -0.2 | 1.41 | **1.26** | +0.26 |
| Deatomizer Pistol | 135 | 1.5 | 0.05, -0.8, 0.6 | 0, 0, 0.2 | 0.63 | 1.0 | -0.22, -0.53, 0 | 0, 0.05, 0.1 | 0.42 | 0.66 | +0.43 |
| Deatomizer Pistol Akimbo | 135 | 1.5 | 0.05, -0.8, 0.7 | none | 0.63 | same as the single pistol (only the main hand is drawn) | | | 0.42 | 0.66 | not computed |
| Scatter Laser Rifle | 135 | 0.8 | 0.1, -0.5, 0.9 | 0, 0.05, 0.2 | 1.18 | 1.0 | -1.16, -0.81, 0 | 0, 0.2, -0.25 | 1.48 | **1.25** | +0.68 |
| Nether Blaster | 135 | 1.35 | 0.2, -0.25, 0.07 | none | 1.02 | 1.2 | -0.55, -0.4, 0 | 0, 0.1, -0.2 | 0.93 | 0.91 | -0.12 |
| Alien Blaster | 135 | 0.8 | 0.1, -0.75, 0.9 | 0, -0.1, 0 | 0.68 | 0.5 | 0.59, -0.56, 0 | 0, 0.1, 0 | 0.42 | 0.62 | +0.38 |
| Grim Reaper | 225 (model also turned 180) | 1.5 | -0.9, -1.8, 0 | 0, -1.5, -0.4 | 1.72 | 1.2 | 0.5, -0.6, 0 | 0, 0.1, 0 | 1.38 | 0.80 | +0.99 |
| Bio Gun | 225 | 1.5 | -0.35, -0.7, 0 | 0, 0, -0.15 | 1.34 | 1.2 | -0.4, -0.4, 0 | 0, 0, -0.15 | 1.07 | 0.80 | +0.30 |
| Power Hammer | 225 | 1.45 | -0.5, -0.65, 0 | 0, -0.15, 0.3 | 1.05 | 1.25 | 0.28, -0.24, 0 | 0, 0.1, 0 | 0.91 | 0.86 | +0.52 |
| Chainsaw | 225 | 1.3 | -1.1, 0.55, -0.1 | 0, -0.15, 0.3 | 2.13 | 0.6 | -0.375, 0.75, -0.2 | 0, -0.15, 0.3 | 0.98 | **0.46** | +0.24 |
| Nuclear Death Ray | 225 | 1.25 | -0.7, -0.6, 0.1 | 0, 0.1, -0.2 | 1.26 | 1.1 | -1.62, -0.4, 0.125 | 0, 0, -0.15 | 1.11 | 0.88 | +0.50 |
| Nuke Launcher | 225 | 1.5 | -1.2, -1.8, 0 | 0, -1.8, 0.2 | 1.69 | 1.4 | -1.7, -0.4, 0 | 0, 0.2, -0.25 | 1.57 | 0.93 | +0.56 |
| PDW | 225 | 1.25 | -0.85, -0.6, 0 | 0, 0.1, 0.2 | 0.94 | 1.25 | -0.1, -0.3, 0 | 0, 0.1, 0 | 0.94 | 1.00 | +0.43 |
| Pulse Rifle | 225 | 1.25 | -0.85, -0.6, 0 | 0, 0.1, 0.2 | 1.41 | 1.1 | -1.25, -0.6, 0 | 0, 0.1, 0.2 | 1.22 | 0.87 | +0.45 |
| Minigun | 225 | 1.25 | -0.6, 0, 0 | -0.05, -0.8, 0.95 | 1.82 | 0.5 | -0.6, -0.1, 0 | -0.05, -0.8, 0.95 | 0.73 | **0.40** | +0.05 |

Notes:
- The barrel direction in third person matches within 0 to 6 degrees for every gun (12 degrees for the pistol, 16 for the two revolvers).
- **Centre shift.** Positive means our gun sits further toward the muzzle than Techguns'. Techguns centres every long gun's box within about 0.25 blocks of the hand. Ours sits the box centre about 0.5 world blocks forward of that. The shift is computed from the shared hand frame and is not yet checked in game. Handguns match within 0.06 blocks, except the Hand Cannon.
- Techguns akimbo third person uses the same rows as the single gun (revolvers and pistols above).

Notes on the M5 third-person rows:
- Ours is always drawn with rotate Y 225 (the X family). From the rotation rows alone, its barrel direction equals Techguns' for every M5 gun, including the Z-family models Techguns draws at rotate Y 135.
- The centre-shift column uses the same computation as above. It reproduces the existing AK-47 (+0.56) and Rocket Launcher (+0.56) rows exactly, and the M4 and Thompson rows to within 0.01 (+0.53 and +0.67 against +0.52 and +0.66). It is not yet checked in game.
- Shifts of +0.5 or more (ours too far toward the muzzle): Grim Reaper +0.99, Sonic Rifle +0.73, Scatter Laser Rifle +0.68, Tesla Rifle +0.57, Nuke Launcher +0.56, Power Hammer +0.52, Nuclear Death Ray +0.50. The Grim Reaper's sign depends on which end is the muzzle (section 8). Close to Techguns: Laser Rifle +0.08, Minigun +0.05, Nether Blaster -0.12.
- Ours is much shorter than Techguns for the Chainsaw (0.46) and Minigun (0.40), and noticeably shorter for the Alien Blaster (0.62) and Deatomizer Pistol (0.66). It is too long for the Blaster Rifle (1.26) and Scatter Laser Rifle (1.25).
- **Deatomizer Pistol akimbo.** The akimbo hold differs from the single gun's: 0.7 instead of 0.6 on the third translate axis, and no aim extra (the single has 0, 0, 0.2). Both guns are drawn at the same spot, so the pair looks like one gun.
- **Minigun.** In Techguns the rotor part turns once about the barrel axis per unit of shot progress. The row is the resting pose.

## 4. Dropped item and inventory

Lengths are the model's barrel length times the scale, in blocks. Dropped items also get the rotations shown in the earlier tables, and they sit slightly below the origin.

| Gun | TG ground length | Ours ground | TG inventory length | Ours inventory |
|---|---|---|---|---|
| Hand Cannon | 2.44 | 2.00 | 1.71 | 1.75 |
| 9mm Pistol | 0.94 | 0.94 | 1.41 | 1.41 |
| Revolver | 1.40 | 1.40 | 1.75 | 1.75 |
| Golden Revolver | 1.45 | 1.45 | 1.81 | 1.81 |
| Sawed-off | 2.44 | 1.46 | 1.71 | 1.71 |
| Mac-10 | 1.24 | 0.96 | 1.51 | 1.38 |
| Thompson | 2.60 | 2.60 | 2.11 | 1.95 |
| M4 | 2.59 | 2.59 | 2.07 | 2.07 |
| Infiltrator | 4.09 | 2.71 | 3.27 | 2.09 |
| AK-47 | 2.76 | 2.76 | 2.02 | 2.02 |
| AUG | 2.62 | 2.62 | 2.10 | 2.10 |
| SCAR | 2.69 | 2.69 | 2.33 | 2.02 |
| Vector | 1.65 | 1.65 | 2.06 | 2.06 |
| AS50 | 3.44 | 2.81 | 2.06 | 2.06 |
| LMG | 2.74 | 2.74 | 2.19 | 2.19 |
| Combat Shotgun | 2.58 | 1.89 | 2.06 | 1.99 |
| Rocket Launcher | 3.00 | 2.70 | 2.04 | 2.04 |
| Grenade Launcher | 2.20 | 2.20 | 1.92 | 1.92 |
| Flamethrower | 2.28 | 2.23 | 1.88 | 1.84 |
| Bolt-action | 3.01 | 2.68 | 2.01 | 2.01 |

Ground and inventory are largely matched already (most within 5 percent). The notable gaps are:
- ground: Sawed-off 0.60 of TG, Infiltrator 0.66 (the Infiltrator model is shorter), Combat Shotgun 0.73, Mac-10 0.77;
- inventory: Infiltrator 0.64.

## 5. Hand Cannon deep-dive

**Techguns, first person.**
- Rotate X 180, rotate Y -45, uniform scale **2.5**, then the idle translate (0.1, 0, -0.5) inside that scale.
- The model is 39 texels long: x runs from -17 to +22, with the muzzle at +22. It is 8 tall (y -3 to 5) and 5 wide (z -2 to 3). That is 2.44 by 0.50 by 0.31 blocks.
- At scale 2.5 the gun is **6.09 H-blocks (2.44 camera blocks)** long. The idle shift is 0.25 right, 0 up and 1.25 toward the camera in H-blocks.
- Box centre: camera (0.75 right, 0.78 down, 0.98 deep).
- Muzzle: (0.75, -0.72) at depth 2.20, which is screen (+0.27, -0.47). The rear sits at depth -0.24, behind the camera plane, so the grip end is clipped.
- The gun fills the lower-right quarter of the screen, rising to a muzzle right of and below centre.

**Ours, first person.**
- Boxes run from x -20 to +20: 40 texels, 2.5 blocks.
- The fit rule scales the model so its length is 15/16 of a block (the 9mm Pistol's tuned size), giving S = 0.375. Length is **0.94 H-blocks (0.375 camera blocks)**.
- Box centre pinned to the Revolver's anchor: camera (0.55, -0.47, 0.94). Muzzle depth 1.13, rear depth 0.76.
- On screen it runs from (+0.58, -0.90) to (+0.39, -0.60). Both ends are in the corner, and the whole gun is about 0.2 of the screen wide.

**Ratio in first person: 0.15.** The Hand Cannon is 6.5 times too small in length. Its apparent area is about 1/40 of Techguns'.

**Why the Hand Cannon differs.**
1. The fit rule's premise is that our boxes are larger than Techguns' and the sheet's scale draws them too big. For this gun the premise is false: our model is 40 texels and theirs is 39.
2. The rule then forces a handgun to 15/16 of a block. That target came from the 9mm Pistol, a 15-texel gun. The Hand Cannon is a 39-texel gun, so it shrinks by 2.6 times relative to its own class.
3. The pistol target is itself half of Techguns' pistol (0.94 against 1.88).
4. The idle position anchors the box centre close to the camera (depth 0.94). Techguns' centre is about the same depth, but its rear end is behind the camera plane.

**Third person.** Techguns: scale 1.5, T (-0.3, -0.45, 0), giving 3.66 H-blocks, which is **1.37 world blocks**. Ours reuses the Revolver's S 0.85 and T (-1.4, -0.8, 0), giving 2.12 H-blocks, which is **0.80 world blocks**. Ratio **0.58**. Our centre sits 0.36 world blocks toward the butt compared with Techguns.

**Ground and inventory.** Ground: Techguns 2.44 blocks, ours 2.00 (0.82). Inventory: 1.71 against 1.75, matched.

**Shot kick, first person.**
- Techguns tilts the gun about z by up to -15 degrees at the mid-point of the kick. Over the kick it moves (-0.5, -0.25, 0) scaled by 2.5, which is 1.25 H-blocks (0.5 camera blocks) back along the barrel.
- Reload swing: up to 70 degrees about z. It travels (+1.0, -0.5, 0) times 2.5 at the peak, so the gun swings out and down by about 1.0 camera block.
- These are in the gun's own scale, so they only look right at Techguns' scale.

## 6. Shot kick and reload motion, first person

Peak values of the kick. They return to the resting pose when the kick ends. Techguns has no idle motion. Translations are in the scaled frame (so multiplied by S in H-blocks). Our kick rows keep the sheet's screen motion by rescaling the translation.

| Gun | Peak tilt (axis) | Peak back-kick (x S) | Back-kick H-blocks |
|---|---|---|---|
| Hand Cannon | -15, z | 0.5 | 1.25 |
| 9mm Pistol | -5, x | 0.15 (0.25 on the slide) | 0.3 to 0.5 |
| Revolver | -17.5, z | 0.25 | 0.38 |
| Sawed-off | -15, z | 0.25 | 0.55 |
| Mac-10 | -3.5, x | 0.15 to 0.25 | 0.3 to 0.5 |
| Thompson | -5, z | 0.25 | 0.45 |
| M4, AK-47, AUG, SCAR, Vector, LMG | -5, x | 0.15 | 0.21 |
| AS50 | -6, x | 0.25 | 0.35 |
| Combat Shotgun | -10, z first quarter, then pump cycle | 0.6 | 1.35 |
| Rocket Launcher | -12.5, z | 0.375 | 0.84 |
| Bolt-action | -12.5, z, then bolt cycle | 0.5 | 0.75 |
| Flamethrower | none | 0 | 0 |

## 7. Recommended targets

Facts to hit. Lengths are the on-screen gun length from rear tip to muzzle tip, with a tolerance of about 10 percent. All values are what Techguns shows, so the first-person ones are large and the rear is clipped by the camera plane.

**First person: length in camera blocks (H-blocks) and the placement.** Centre is (right, up, depth) in camera blocks. Muzzle is the (right, up) and depth of the muzzle tip.

| Gun | Length cam (H) | Centre | Muzzle |
|---|---|---|---|
| Hand Cannon | 2.44 (6.1) | 0.75, -0.78, 0.98 | 0.75, -0.72, 2.20 |
| 9mm Pistol | 0.75 (1.9) | 0.74, -0.59, 1.14 | 0.74, -0.44, 1.51 |
| Revolver | 1.05 (2.6) | 0.51, -0.58, 1.18 | 0.51, -0.38, 1.70 |
| Golden Revolver | 1.09 (2.7) | 0.51, -0.58, 1.20 | 0.51, -0.37, 1.74 |
| Sawed-off | 2.15 (5.4) | 0.69, -0.66, 0.95 | 0.69, -0.55, 2.02 |
| Mac-10 | 1.10 (2.75) | 0.74, -0.71, 1.11 | 0.74, -0.41, 1.66 |
| Thompson | 2.34 (5.85) | 0.59, -0.63, 0.84 | 0.59, -0.40, 2.01 |
| M4 | 2.42 (6.0) | 0.60, -0.57, 1.31 | 0.60, -0.42, 2.52 |
| Infiltrator | 3.81 (9.5), if the model is extended to 109 texels; else scale to M4's 2.4 per 69 texels | 0.60, -0.54, 2.01 | 0.60, -0.56, 3.92 |
| AK-47 | 3.43 (8.6) | 0.60, -0.67, 1.40 | 0.60, -0.45, 3.11 |
| AUG | 2.45 (6.1) | 0.59, -0.57, 1.29 | 0.60, -0.42, 2.52 |
| SCAR | 2.51 (6.3) | 0.59, -0.50, 1.37 | 0.60, -0.43, 2.62 |
| Vector | 1.54 (3.85) | 0.66, -0.61, 0.91 | 0.66, -0.45, 1.68 |
| AS50 | 3.50 (8.75) | 0.61, -0.36, 1.68 | 0.60, -0.33, 3.43 |
| LMG | 2.55 (6.4) | 0.62, -0.52, 1.35 | 0.60, -0.38, 2.62 |
| Combat Shotgun | 3.09 (7.7) | 0.70, -0.65, 1.06 | 0.70, -0.41, 2.61 |
| Rocket Launcher | 2.70 (6.75) | 0.70, -0.63, 1.14 | 0.70, -0.52, 2.49 |
| Grenade Launcher | 2.20 (5.5) | 0.96, -0.72, 1.36 | 0.96, -0.63, 2.46 |
| Flamethrower | 2.15 (5.4) | 0.81, -0.62, 1.33 | 0.81, -0.47, 2.41 |
| Bolt-action | 2.01 (5.0) | 0.54, -0.35, 1.02 | 0.52, -0.29, 2.02 |
| Laser Rifle | 1.72 (4.3) | 0.50, -0.40, 0.92 | 0.50, -0.33, 1.78 |
| Tesla Rifle | 1.61 (4.0) | 0.52, -0.44, 0.68 | 0.52, -0.35, 1.49 |
| Sonic Rifle | 2.95 (7.4) | 0.79, -0.73, 1.16 | 0.79, -0.51, 2.63 |
| Blaster Rifle | 1.75 (4.4) | 0.41, -0.49, 1.12 | 0.41, -0.38, 1.99 |
| Deatomizer Pistol | 1.12 (2.8) | 1.03, -0.76, 1.65 | 1.03, -0.61, 2.21 |
| Deatomizer Pistol Akimbo | 1.12 (2.8) each | right 1.03, -0.76, 1.65; left -1.02, -0.76, 1.65 | right 1.03, -0.61, 2.21; left -1.02, -0.61, 2.21 |
| Scatter Laser Rifle | 2.20 (5.5) | 0.59, -0.50, 1.21 | 0.59, -0.33, 2.31 |
| Nether Blaster | 1.61 (4.0) | 0.96, -0.49, 0.96 | 0.96, -0.61, 1.77 |
| Alien Blaster | 1.26 (3.15) | 0.60, -0.53, 1.08 | 0.60, -0.40, 1.71 |
| Grim Reaper | 2.57 (6.4) | 0.66, -0.41, 0.59 | 0.66, -0.20, 1.88 (far tip) |
| Bio Gun | 1.90 (4.75) | 0.57, -0.78, 0.52 | 0.57, -0.54, 1.47 |
| Power Hammer | 1.16 (2.9) | 0.34, -0.40, 0.70 | 0.34, -0.33, 1.28 |
| Chainsaw | 2.36 (5.9) | 1.06, -0.58, 1.75 | 1.06, -0.74, 2.93 |
| Nuclear Death Ray | 1.18 (3.0) | 0.45, -0.30, 0.61 | 0.45, -0.28, 1.20 |
| Nuke Launcher | 2.70 (6.75) | 0.70, -0.63, 1.14 | 0.70, -0.52, 2.49 |
| PDW | 1.68 (4.2) | 0.78, -0.70, 1.36 | 0.78, -0.55, 2.20 |
| Pulse Rifle | 2.52 (6.3) | 0.78, -0.66, 1.41 | 0.78, -0.55, 2.67 |
| Minigun | 2.33 (5.8) | 0.71, -0.53, 1.40 | 0.71, -0.54, 2.57 |

Rules of thumb:
- The muzzle sits 0.5 to 0.96 right, 0.3 to 0.7 down and 1.5 to 3.9 deep.
- The butt reaches the camera plane (depth about 0 to 0.8) and is clipped.
- Handguns and SMGs (pistol, revolvers, Mac-10) stay fully visible: rear depth 0.56 to 0.76.
- Akimbo: the left gun is the mirror at equal depth and height, with centre x negated.

Rules of thumb, M5 additions:
- The M5 muzzles sit 0.34 to 1.06 right, 0.20 to 0.74 down and 1.2 to 2.9 deep. The on-screen lengths run from 1.12 (Deatomizer Pistol) to 2.95 (Sonic Rifle).
- The energy rifles are shorter than the M2 long guns: Laser Rifle 1.72, Tesla Rifle 1.61, Blaster Rifle 1.75 and Nether Blaster 1.61 camera blocks. Their rear ends are at or just in front of the camera plane (depth -0.12 to 0.24), so the butt is clipped but most of the gun is visible.
- Rear depth 0.45 or more, so the whole gun is in view: Deatomizer Pistol (1.08), Chainsaw (0.57), PDW (0.52), Alien Blaster (0.45).
- Rear behind the camera plane: Grim Reaper (-0.70), Bio Gun (-0.43), Sonic Rifle (-0.32), Nuke Launcher (-0.21), Tesla Rifle (-0.12).
- Deatomizer akimbo: mirror at equal depth and height, centre x negated (1.03 and -1.02).

**Third person: length in world blocks, plus position.**
- Targets are Techguns' lengths from section 3: Hand Cannon 1.37, Sawed-off 1.37, Mac-10 0.62, Thompson 1.40, M4 1.29, Infiltrator 2.04, AK-47 1.61, AUG 1.31, SCAR 1.35, Vector 0.83, AS50 1.64, LMG 1.37, Combat Shotgun 1.48, Rocket Launcher 1.69, Grenade Launcher 1.34, Flamethrower 1.51, Bolt-action 1.88. The pistol (0.53), revolvers (0.56 and 0.58) already match.
- M5 targets, same method (world blocks): Laser Rifle 1.55, Tesla Rifle 1.45, Sonic Rifle 1.73, Blaster Rifle 1.11, Deatomizer Pistol 0.63 (single and akimbo), Scatter Laser Rifle 1.18, Nether Blaster 1.02, Alien Blaster 0.68, Grim Reaper 1.72, Bio Gun 1.34, Power Hammer 1.05, Chainsaw 2.13, Nuclear Death Ray 1.26, Nuke Launcher 1.69, PDW 0.94, Pulse Rifle 1.41, Minigun 1.82.
- Position: the box centre should sit within about 0.25 world blocks of the hand (the item-frame origin). This rule is a Techguns property. Ours currently sits about 0.5 blocks too far forward for every long gun (the shifts in section 3).

## 8. Unsure

- The first-person frame constants (camera offset 0.56, -0.52, -0.72; scale 0.4; the -0.5 helper shift) and the third-person hand scale (0.375) come from the standard 1.7.10 item pipeline. The akimbo symmetry check supports them, but the whole table should be confirmed with a screenshot.
- The Infiltrator's Techguns model length (109 texels) is a union of every part the model can draw, so its real resting length may be shorter.
- The grenade launcher is a Y-up model file with +x assumed forward from its transform rotation. The muzzle end is the +x one (18 texels) and the rear is -26, which looks unusual for a launcher.
- The muzzle and rear y values are the centre of the end slices, so they ignore sights and stocks.
- The akimbo-left-behind-camera result and the +0.5 third-person shift are computed. Neither has been seen in game.
- **Grim Reaper muzzle.** The model's four-hole muzzle face is on its +x end, but the model is drawn after a 180 degree turn about the vertical axis. With the X-family hold that sends the four-hole end toward the shooter and the rear vent end away from the camera. The tables give the far tip (the vent end) as the muzzle, because that is what the camera sees pointing away. Whether Techguns means it that way, or the turn is a leftover (the existing spec already records it as a rough edge), is not settled. Length, centre and rear depth do not depend on this reading. The muzzle row and the third-person direction do.
- **Nuclear Death Ray first person.** The double idle step (section 2) comes straight from the transform code: the first-person block applies it and the shared renderer applies it again when idle. The numbers use that reading. If something outside the transform code skips the second step, the single-idle centre (0.37, -0.52, 0.52) applies instead. Not seen in game.
- **Power Hammer and Minigun parts.** Both have two parts (the hammer's head and rods, the minigun's rotor). The tables use the union of both parts at rest, with the rotor unturned. During a charge or a spin the parts move relative to each other, so the real extent changes.
- **Chainsaw and hammer ends.** The chainsaw's bar tip and the hammer's striker end are taken as the muzzle, as their model notes do. Both are the far end from the camera, like every other gun.
- **Muzzle y values.** The Techguns muzzle y is the centre of the tip slice (boxes within one texel of the barrel end), so sights, hammer heads and rotor tips can pull it away from the box centre. The Ours muzzle y in the table is the box centre, as for the M2 rows.
- **Ours for the Minigun.** Its profile is not on master yet (branch commit 5ce048c), so its rows may move when it lands. The Pulse Rifle's and Nether Blaster's raw lengths differ from their tilted extents by 0.6 and 0.7 texels, which changes the Ours muzzle and rear depth by under 0.02.
- **Ours for the Deatomizer akimbo left gun** uses the same mirror constants as the M2 revolvers and pistol and has not been seen in game. For the revolvers, the Akimbo section above prints a left-gun depth of +0.50. By the same constants it works out to -0.50, which would agree with the pistol (behind the camera plane). Worth a recheck.
