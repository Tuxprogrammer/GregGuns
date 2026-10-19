# Nuke Launcher: clean-room skin notes

Skin-only notes for the Techguns **Nuke Launcher**. It is the **Rocket Launcher with a different texture**: same model, same renderer, same transforms. Our builder reuses the existing rocket-launcher layout from [rocket_launcher.md](rocket_launcher.md) and adds only a skin. Read that page first; this one lists only what differs. Written by the "dirty room" analyst from study of the original's two textures and its renderer registration. No source, no box lists and no texture coordinates are included.

Related facts: [ModelRocketLauncher](../gun-models.md#modelrocketlauncher-rocket-and-nuke-launcher-128x64-family-x-29-boxes-1-part), [ModelRocket](../gun-models.md#modelrocket-rocket-item--projectile-128x64-11-boxes), [rendering framework](../rendering-framework.md).

## 1. Geometry, renderer and transforms: no differences

- **Model:** the identical launcher model, all 29 boxes, same sizes, same positions. The loaded-rocket parts are the same too.
- **Renderer:** the same renderer class, built with the same arguments (scale 1, no offset, the same two flags). Only the texture path differs.
- **Transforms:** every view (first person, firing, reload, aimed, third person, recoil, ground, icon) is identical to the Rocket Launcher's, as listed in section 6 of [rocket_launcher.md](rocket_launcher.md). The muzzle flash is also absent.
- **Loaded rocket:** shown and hidden by the same rule (ammo left and not in the first half of the reload). The nose that pokes out of the muzzle is in the nuke skin's rocket colors (section 3).
- **Camo:** none. One skin, like the Rocket Launcher.
- **Not model differences:** the nuke gun fires a bigger, slower rocket (radius 10, a different gravity) and the rocket projectile and the nuclear rocket ammo item use the same nuke skin (so they are yellow too). All of that is gameplay and projectile code; nothing in the model changes.

## 2. Skin overview

The launcher goes from a **cool blue-gray tube with a white-and-red rocket** to a **yellow-olive and brown "caution-striped" tube with a yellow-and-orange rocket**, and all the black hardware shifts from neutral black to a **warm brown-black**. Nothing else changes; there are no new decals, no emblem and no text. The only markings are the diagonal hazard stripes on the tube.

Shading style, same as the original's: a few levels of per-pixel noise, no highlights beyond what is listed, no wear marks. The nuke skin is grimier and lower in contrast than the Rocket Launcher's, so it reads as dark and dirty rather than clean steel.

## 3. Per part

### Tube (the long pipe, 36 long, 6 across)
- **Original:** brushed blue-gray steel, base #5A6467, highlight #79888E to #AABCC2, shadow #32373A, horizontal streaks along the length.
- **Nuke:** **diagonal hazard stripes** over a grimy noise, on every face of the pipe. Two alternating bands:
  - olive-yellow: base #675C33, highlight #90814A (brightest speckle #C2AC5F), shadow #4A411F;
  - brown-gray: base #504539, shadow #241D16, with a few pale gray-pink speckle pixels (#B0A59C to #CCC5BF).
- **Stripe geometry:** 45 degrees. Each band is about 5 texels wide measured along the tube, so one olive plus one brown band repeats every **10 units along the length**, about 3.6 repeats along the 36-long pipe. The stripes run across the pipe at 45 degrees and are continuous round the corners; on the near face they rise toward the muzzle, and on the far face they mirror. The bands' edges are ragged and soft (a one-pixel dither), not clean.
- The **lower edge** of each pipe face darkens to near-black brown (#241D16 down to #14100C) like the original's seam lines. There is **no** longitudinal streaking.
- The sampled mean color of the whole tube is about #4A3F2A, against #50595C for the original.

### Rear cap and muzzle ring (black ends)
- **Original:** black gunmetal, cap about #222323, ring about #202121.
- **Nuke:** warm brown-black. Region mean about #12100D, typical range #050504 to #191612, brightest pixel #1D1A14. About 40 percent darker than the original and tinted brown instead of neutral. No markings, no stripes, no emblem.

### Collar housing, side plates, top bridge
- **Original:** black gunmetal, housing #2D2F2F, side plates #292B2B.
- **Nuke:** warm brown-black, mean about #1D1712, with a typical range of #110E0B (shadow) to #2E231B (highlight); the lightest speckle is #392E27. Unmarked. The collar is still clearly darker than the striped tube, and with the tube's olive stripes this makes a stronger contrast than in the original (dark brown against yellow-olive instead of near-black against pale gray).

### Flip-up sight, trigger stub, grip
- **Original:** black gunmetal, about #2D2F2F (the grip a few levels lighter).
- **Nuke:** same warm brown-black as the collar (#1D1712 mean), the grip a few levels lighter (towards #2E231B). Unmarked.

### Loaded rocket (the stepped nose that shows at the muzzle)
The rocket's nose, collar and body use the same layout as the Rocket Launcher's, with new colors:

| Rocket part | Original | Nuke |
|---|---|---|
| Body and wing panels (the white areas) | white to pale gray, #F0F0F0 / #D8D9D9, with a pure-white seam line | **yellow**, base #FFF600 (variants #FFF300, #FFF713), shading down to #E8D000 and #E4D200, highlight #FFFA07 |
| Seam lines (the 1-texel lines that are pure white in the original) | #FFFFFF | **dark gray #585858** (range #323232 to #5C5C5C): they invert from the lightest thing on the rocket to the darkest |
| Red bands, nose, tail ring | signal red #FE1C1C, highlight #FE3737, shadow #FD1414 | **orange-red**, base about #FB3A0D (range #FF2C09 to #FF5A1D), shadow #CA1E00 (dark red-brown) |
| Fins (black) | near-black #161616 | near-black olive #191600 (range #151300 to #282500) |

The three-step nose in the muzzle (the 4 by 4 collar, 3 by 3 step, 2 by 2 tip) therefore reads as **orange-red collar, yellow step, orange-red tip**, instead of red, white and red.

### Rocket projectile and ammo item
They use the same skin as the launcher (the same texture file), so the flying rocket and the nuclear rocket item are yellow with orange-red bands and olive-black fins. The projectile geometry is unchanged (section 11 of [rocket_launcher.md](rocket_launcher.md)).

## 4. Summary table

| Region | Rocket Launcher | Nuke Launcher |
|---|---|---|
| Tube | blue-gray #5A6467, horizontal streaks | olive #675C33 and brown #504539 diagonal stripes |
| Black hardware | #292B2B | #1D1712 |
| Cap and ring | #1C1D1D | #12100D |
| Rocket body | white #F0F0F0 | yellow #FFF600 |
| Rocket seams | pure white | dark gray #585858 |
| Rocket red | #FE1C1C | orange-red #FB3A0D |
| Rocket fins | #161616 | #191600 |

## 5. Acceptance checklist

- [ ] The geometry, UV-independent layout and in-game transforms are exactly the existing Rocket Launcher's. Only the skin changes.
- [ ] The tube has 45-degree olive and brown caution stripes, about 10 units per olive-plus-brown repeat, on every face.
- [ ] The stripe colors are about #675C33 (olive) and #504539 (brown), with a dark lower edge on each face.
- [ ] The cap, ring, collar, side plates, sight, grip and trigger are warm brown-black (mean about #1D1712; the cap and ring a little darker, about #12100D), with no markings.
- [ ] The loaded rocket's nose shows orange-red, yellow, orange-red from the outside in.
- [ ] Rocket body areas are yellow (#FFF600 family); seam lines are dark gray, not white.
- [ ] The rocket's red is orange-red (about #FB3A0D), not the signal red of the Rocket Launcher.
- [ ] There is no emblem, text, trefoil or other decal anywhere.
- [ ] No camo variant.

## 6. Unsure

- **Stripe direction on each face.** The stripes are 45-degree diagonals on the flat texture, and which way they lean on each pipe face depends on how the faces are unwrapped. "Rising toward the muzzle on the near face" is the most likely reading; either direction is acceptable.
- **No hidden emblem.** A pixel-by-pixel comparison against the Rocket Launcher's texture found the same layout everywhere, with no extra decal region. A **radiation trefoil, nuclear emblem or text is not present** in the original and need not be invented. Because the sampled region of the black hardware has the same layout and noise as the original, any faint pattern there is only grime.
- **The loaded rocket's seam-line colors** change from white to dark gray; this is from the texture and was not checked in a render.
