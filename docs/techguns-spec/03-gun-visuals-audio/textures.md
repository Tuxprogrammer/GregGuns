# Texture catalogue: `textures/guns/`

All 62 PNGs in `assets/techguns/textures/guns/`. None has a `.mcmeta`, so none is animated, and none is an exact duplicate of another file.

**How to read the layouts.** Every texture except the grenade-launcher sheet is a vanilla **box-UV unwrap**. A box *w×h×d* with UV origin (u, v) occupies the rectangle u … u + 2(d + w) by v … v + (d + h):
* top row: [d gap][top face w×d][bottom face w×d]
* second row: [left side d×h][front w×h][right side d×h][back w×h]

The two top corners are unused. Regions are given in **texture pixels at the PNG's resolution**. For PNGs that are twice the model's declared size, all model UVs are doubled. "Transparent" pixels are alpha 0. They either fill unused space or, where they fall inside a face, render as see-through cut-outs (alpha-tested). "Black fill" means the unused space is opaque black (harmless, never sampled).

Palette notes use approximate hex colours from a quantised histogram (top colours by share of opaque pixels).

---

## Assault rifles and carbines

### `textures/guns/m4Texture.png` + camo variants (M4, M4 magazine items)

| Path | Size | Variant | Dominant palette |
|---|---|---|---|
| textures/guns/m4Texture.png | 128×64 | default: black-and-gunmetal tactical | #303030 44 %, #101010 31 %, #101030 10 %, greys |
| textures/guns/m4Texture_1.png | 128×64 | camo 1 "Swamp": mottled olive and moss green | #305010, #103010, #507030, #509030 |
| textures/guns/m4Texture_2.png | 128×64 | camo 2 "Desert": tan and khaki with olive blotches | #909050, #505030, #90b070 |
| textures/guns/m4Texture_3.png | 128×64 | camo 3 "Ice": light-grey and white digital | #909090 44 %, #707070 32 %, #b0b0b0 |
| textures/guns/m4Texture_4.png | 128×64 | camo 4 "Hazard": yellow-orange with diagonal black hazard stripes | #f0b010, #b07010, #f0d010 |

* **Depicts:** the unwrapped M4 carbine: receiver, quad-rail handguard, barrel, EOTech sight, collapsible stock, magazine and grip. 41 % opaque, transparent background.
* **Layout (shared by all five):**
  * Receiver01 (2, 5 … 42, 25).
  * Barrel (0, 26 … 40, 46): dark tube. In all camos it stays grey or black.
  * Handguard core (32, 13 … 70, 32), rails (36, 33 … 64, 48) and (34, 48 … 64, 62), top rail (2, 48 … 36, 64), striped black/colour to suggest picatinny teeth.
  * Stock tube (65, 46 … 101, 64), lower receiver (92, 0 … 128, 18), grip (68, 0 … 82, 13).
  * EOTech base (56, 15 … 82, 27), with hood and ears around (0, 3) to (16, 15) and the red-dot pixel near (0, 11).
  * Magazine (95, 19 … 111, 29) and (112, 19 … 128, 30): mid grey in every variant.
  * Cartridge tip (112, 30 … 124, 36): pale yellow, used only by the full magazine item model.
* In the camo variants only the furniture recolours. Barrel, magazine, rails' teeth and optics stay dark.

### `textures/guns/m4_uq_Texture.png` (M4 Infiltrator)

| Path | Size | Palette |
|---|---|---|
| textures/guns/m4_uq_Texture.png | 128×64 | black/gunmetal #303030 47 %, #101010 38 %, pure red #f01010 3 % |

* Same layout as m4Texture with V shifted by +1 on several parts.
* **Row 0 is a full-width bright red line (y = 0, x 0…127).** It is the texture of the laser-beam planes: their UV origin (0, −60) makes the 60-long side faces map exactly onto row 0.
* Adds the silencer (68, 24 … 110, 45, dark grey), laser box (95, 30), scope body (26, 1 … 50, 13), and a red laser-emitter dot near (101, 37).

### `textures/guns/ak47Texture.png`, `ak47Texture_1.png` (AK-47)

| Path | Size | Variant | Palette |
|---|---|---|---|
| textures/guns/ak47Texture.png | 128×64 | default: dark steel with walnut-brown wood furniture | #303030 64 %, browns #503010/#703010/#705030 |
| textures/guns/ak47Texture_1.png | 128×64 | camo 1 "Blue": wood replaced with blue polymer | #303030 64 %, blues #103070/#103090/#1050b0 |

* 43 % opaque, transparent background.
* **Steel (left half):** receiver (0, 31 … 64, 64), dust cover (18, 0 … 70, 26), magazine (9, 0 … 25, 14). There is a green front-sight dot at about (21, 27) and a red pixel below it.
* **Wood or polymer (right half):** stock (58, 1 … 118, 31), with layered stock planks (46 … 96, 34 … 58) shown as thin horizontal wood strips. Upper handguard (48, 10 … 72, 22), lower handguard (92, 0 … 128, 19).
* Barrel strip (70, 35 … 128, 64): dark grey.

### `textures/guns/AugTexture.png` + camo variants (AUG)

| Path | Size | Variant | Palette |
|---|---|---|---|
| textures/guns/AugTexture.png | 128×64 | default: flat dark olive-green polymer | #305030 58 %, black 25 % |
| textures/guns/AugTexture_1.png | 128×64 | camo 1 "Grey": flat charcoal | #303030 65 % |
| textures/guns/AugTexture_2.png | 128×64 | camo 2 "Wood": mottled woodland greens | #105030, #103010, #309050 |
| textures/guns/AugTexture_3.png | 128×64 | camo 3 "Sand": pale sand/khaki speckle | #d0d090, #b0b070 |
| textures/guns/AugTexture_4.png | 128×64 | camo 4 "Nether": blood red with dark red noise | #701010, #501010, #901010 |

* 37 % opaque, transparent background.
* **Coloured shell:** main body (10, 15 … 106, 63) is the large cross in the middle. Stock pieces (28, 0 … 54, 26), (72, 8 … 98, 21), (39, 27 … 53, 38). Butt (64, 0 … 80, 16). Foregrip (32, 40).
* **Black in all variants:** barrel (0, 32 … 50, 57), scope tubes (1, 1 … 27, 26), grip (64, 43 … 78, 56).
* **Magazine** (95, 19) / (112, 19): translucent-looking mid grey in all variants.

### `textures/guns/scar_texture.png`, `scar_texture_1.png` (SCAR)

| Path | Size | Variant | Palette |
|---|---|---|---|
| textures/guns/scar_texture.png | 256×128 | default: two-tone FDE: tan #b0b070 body, black fittings | #b0b070 48 %, #303030 19 % |
| textures/guns/scar_texture_1.png | 256×128 | camo 1 "Black": all charcoal | #303030 83 % |

* Model UVs are doubled (model declared 128×64). 45 % opaque, transparent background.
* **Tan in the default:** receiver (0, 44 … 164, 128), top rail (102, 42 … 254, 116, with black rail teeth), stock pieces (4, 0 … 64, 30) and (68, 0 … 120, 26).
* **Black:** magazine (218, 36 … 254, 62), grip (136, 0 … 164, 26), barrel and muzzle (0, 64 … 40, 84), reflex sight (186 … 254, 60 … 110).
* Fine details: screw dots, grip stippling, and a ribbed charging-handle strip (vertical striped column around x 208).

### `textures/guns/vector_texture.png`, `vector_texture_1.png` (Vector)

| Path | Size | Variant | Palette |
|---|---|---|---|
| textures/guns/vector_texture.png | 256×128 | default: two-tone: tan #b0b070 lower receiver and stock, charcoal upper | #303030 54 %, #b0b070 34 % |
| textures/guns/vector_texture_1.png | 256×128 | camo 1 "Black": all charcoal | #303030 92 % |

* Model UVs doubled. 29 % opaque. Everything lives in the top 94 rows.
* Upper receiver (0, 0 … 116, 56). Slanted lower blocks (32 … 100, 58 … 90), tan with grip stipple. Top rail (72, 46 … 172, 94), with a black ribbed rail strip around x 120…125. Magazine (120, 0 … 140, 28). Stock (144, 0 … 200, 26). EOTech (130, 64 … 182, 88). Red-dot pixel at about (0, 22).

### `textures/guns/pulseRifle.png` + camo variants (Pulse Rifle)

| Path | Size | Variant | Palette |
|---|---|---|---|
| textures/guns/pulseRifle.png | 128×64 | default: pale ice-blue armour panels on black, dark slate lower parts | #101010 57 %, #d0f0f0/#b0d0d0 ~17 %, #303050 9 % |
| textures/guns/pulseRifle_1.png | 128×64 | camo 1 "Grey": white/light-grey panels, yellow detail stripes | #101010 72 %, #d0d0d0, #b0b0b0 |
| textures/guns/pulseRifle_2.png | 128×64 | camo 2 "Dark Green": near-black with dark green blotches | #101010 87 %, #103010 10 % |

* 99 % opaque, black fill.
* **Light top half** (y 0…32): the upper receiver, spine (36, 0 … 92, 6), scope base, top plates. There are red accent stripes at x ≈ 33 and 43, and small magenta (transparent) slot pixels at y ≈ 15, which render as cut-out vents.
* **Dark bottom half** (y 32…64): grip, stock and magazine pieces. A cyan glow pixel at about (118, 35) (red in camo 2) is the energy indicator. There is a transparent 10×4 cut-out at (17, 40).

### `textures/guns/PDW.png` + camo variants (PDW)

| Path | Size | Variant | Palette |
|---|---|---|---|
| textures/guns/PDW.png | 64×64 | default: pale ice-blue upper shell, dark slate-blue lower | #101010 56 %, #303030 24 %, #303050 7 %, #d0f0f0 4 % |
| textures/guns/PDW_1.png | 64×64 | camo 1 "Silver": white/grey upper, yellow detail | #101010 73 %, #d0d0d0, #f0f0f0 |
| textures/guns/PDW_2.png | 64×64 | camo 2 "Dark Green": black and dark olive | #101010 82 %, #103010 16 % |

* 100 % opaque, black fill.
* **Upper shell** (0, 8 … 58, 19): light, with a red sight stripe (yellow in camo 1, pale pink in camo 2) at x ≈ 18. There is a small upper-rear detail block at (39, 4 … 63, 8).
* **Lower half** (y 20…64): dark grip, stock and magazine pieces.

### `textures/guns/mg2_texture.png` (LMG, LMG magazine items)

| Path | Size | Palette |
|---|---|---|
| textures/guns/mg2_texture.png | 128×128 | dark gunmetal #303030 71 %, black 22 %, faint teal-grey, a red pixel, pale yellow cartridge |

* 27 % opaque, transparent background. Content occupies rows 0…106.
* Receivers (54, 13 … 102, 58) and (66, 81 … 118, 107). Barrel (0, 26 … 40, 46): the pale yellow and brass column around (6, 26 … 14, 33) is the cartridge texture used by the belt rounds. Top rail (0, 0 … 38, 18), with black ribbed strips.
* Box magazine (0, 59 … 59, 84): large dark-teal grey slabs with small rivets. Stock (79, 64 … 128, 80). Holo sight around (104, 0 … 120, 16), with a red-dot pixel at (105, 10).

### `textures/guns/as50Texture.png` (AS50, AS50 magazine items)

| Path | Size | Palette |
|---|---|---|
| textures/guns/as50Texture.png | 256×128 | black #101010 55 %, gunmetal #303030 26 %, #505050 13 %, brass #f0d070 6 % |

* 17 % opaque. All content is in x 0…183. The rest is transparent.
* Barrel (0, 24 … 80, 64), receiver (56, 23 … 128, 61), stock (92, 0 … 128, 19), cheek rest (68, 3 … 108, 23), scope (129 … 184, 0 … 34), with subtle dot-grid stipple panels.
* **Brass cartridge** at the bottom-left: bullet body (0, 119 … 16, 127) and tip (18, 122 … 30, 128), used by the full magazine item model.

### `textures/guns/blasterRifle.png` (Blaster Rifle)

| Path | Size | Palette |
|---|---|---|
| textures/guns/blasterRifle.png | 256×128 | near-black #101010 51 %, charcoal #303030 49 % |

* Model UVs doubled (declared 128×64). 32 % opaque, transparent background.
* Monochrome dark sci-fi rifle. Receiver (0, 62 … 128, 128), barrel guard (164, 82 … 256, 128), cooling fins (132, 76 … 200, 118), side energy magazine (184 … 256, 36 … 62), scope (0 … 112, 26 … 50). Fine grille and vent texture in darker lines.

---

## Pistols and SMGs

| Path | Size | Depicts / palette | Layout notes | Used by |
|---|---|---|---|---|
| textures/guns/pistol3.png | 128×64 | Black polymer pistol: slide #303030 with lighter top highlight, frame #101010, dark-red grip accents. 21 % opaque | Model UVs doubled. Slide (0, 0 … 68, 32), with white sight-dot pixels at about (2, 27) and (54, 27). Frame rail (0, 34 … 68, 64). Grip (6, 0 … 26, 20). Barrel (0, 42 … 28, 56) | Pistol, Akimbo Pistols, off-hand akimbo renderer |
| textures/guns/pistol.png | 64×32 | Plain grey (#505050 / #303030) placeholder pistol. 12 % opaque | Top (0, 0 … 28, 15), grip strips (29 … 47, 0 … 10) | ModelPistol (unused) |
| textures/guns/pistol2Texture.png | 64×32 | Plain grey (#505050 97 %) placeholder. 16 % opaque | Top (0, 0 … 34, 17), grip (30, 0 … 40, 10) | ModelPistol2 (unused) |
| textures/guns/mac10texture.png | 128×64 | Mid-grey stamped-steel SMG (#505050 59 %), lighter grey stock rods. 34 % opaque, all content in x 0…81 | Model UVs doubled. Receiver (2, 22 … 82, 64), grip and magazine (0, 0 … 20, 40) with a light-grey magazine face, barrel (2, 40 … 30, 54), wire-stock pieces (40 … 72, 0 … 48) | Mac-10, Akimbo Mac-10, off-hand renderer |
| textures/guns/mp5Texture.png | 64×32 | Dark teal-grey SMG (#303030, #305050, #305030). 30 % opaque | Receiver (0, 10 … 44, 32), rail (24, 9 … 64, 29), magazine (0, 10 … 8, 25) | ModelMp5 (unused) |
| textures/guns/mibgun.png | 128×128 | **Opaque white background** (#f0f0f0 82 %) with silver-grey (#b0b0b0 / #d0d0d0) chrome parts. Black grip panel. Bright blue lamp (#30b0f0) at about (24 … 36, 24 … 30). One orange and one green indicator pixel | Model UVs doubled. Body tube (0, 78 … 44, 100). Side bars (86 … 126, 0 … 42). Grip (6, 56 … 26, 76), black. Barrels (96, 44 … 124, 58). The white background is never sampled except where faces land on it, so the gun reads as chrome-white | MIB gun, Akimbo MIB, off-hand renderer |
| textures/guns/thompson.png | 64×64 | Grey steel receiver and barrel (top band, y 0…24), warm brown wood stock, grip and foregrip (#705010 / #905010, y 24…44). Black fill | Barrel (0, 0 … 40, 4), receiver (0, 4 … 50, 10), stock (0, 33 … 26, 39) wood, stick magazine (54, 14 … 64, 26) grey | Thompson |

## Revolvers and hand cannon

| Path | Size | Depicts / palette | Layout notes | Used by |
|---|---|---|---|---|
| textures/guns/revolver.png | 64×32 | Brushed light-grey steel (#909090 / #b0b0b0) with a darker grey hammer and a **warm orange-brown wooden grip** (#705010) at the centre (x 24…44). Black fill | Cylinder (0, 11 … 20, 18) and its slices. Barrel (0, 0 … 32, 4). Frame (40 … 64, 0 … 15). Grip (25 … 43, 4 … 23), wood with lighter grain highlights | Revolver, Akimbo Revolvers, off-hand renderer |
| textures/guns/goldenrevolver.png | 64×32 | Same layout, **pale gold** (#f0f090 / #d0d070 / #d0d050) with olive shading, and a black grip with gold inlay pixels. Black fill | Ornate barrel (0, 25 … 34, 29), fins (0, 29 … 26, 32), top rib (27, 30 … 55, 32), all gold. Grip (25 … 37, 12 … 23), black with gold stripes | Golden Revolver, Akimbo Golden Revolvers |
| textures/guns/handgun.png | 64×32 | **Stone and wood**: cobblestone-grey barrel (#b0b0b0 / #707070 / #909090), the top band darker grey with a black muzzle hole near (25, 4). **Oak-plank brown** stock (#505030 / #303010) on the right side (x 44…64). Black fill bottom-left | Barrel slab (0, 1 … 50, 9) stone, side plates (4, 0 … 50, 4), stock (44, 14 … 64, 21) and butt (50, 0 … 64, 11) wood, lock blocks (0 … 32, 9 … 19) stone | Hand Cannon |

## Shotguns

| Path | Size | Depicts / palette | Layout notes | Used by |
|---|---|---|---|---|
| textures/guns/sawedOff.png | 64×32 | Light-grey steel barrels (top 9 rows, black bead pixels at x 0 and 30) over a **dark oak-wood** stock (#505030 / #303010) filling rows 9…32, x 22…64. Black fill bottom-left | Barrels (0, 0 … 60, 7), stock and grip (30 … 64, 9 … 29) wood | Sawed-off |
| textures/guns/combatShotgun.png | 64×32 | All greys: light-grey barrel and tube on top (y 0…8), dark charcoal receiver below, a lighter grey pump. One white highlight block at about (36, 26) | Barrel (0, 0 … 50, 4), tube (0, 6 … 48, 10), pump (0, 23 … 32, 32), receiver (32, 14 … 58, 32), stock bits (0 … 28, 10 … 23) | Combat Shotgun |
| textures/guns/sonicshotgun.png | 128×64 | Dark slate and teal-grey (#303030 / #505050 / #507070) sci-fi body, large transparent areas. **Cyan glow** strip pixels at about (66 … 86, 49 … 54). Bright ice-cyan panel (100 … 126, 54 … 63) for the GLOW01 core | Barrel slabs (0, 44 … 42, 64), body (44, 30 … 74, 41), power cell (64, 44 … 88, 55), stock (78 … 128, 0 … 44) | Sonic Shotgun |

## Bolt-action family

| Path | Size | Variant / palette | Notes |
|---|---|---|---|
| textures/guns/boltactionrifle.png | 128×32 | default: grey barrel strip (top 4 rows), **walnut wood** stock (#303010 / #503010) in rows 8…27, black scope and bolt. 100 % opaque, black fill | Barrel (0, 0 … 80, 4), with white highlights at the left. Stock (0, 8 … 66, 27), with black rectangles for the cut-outs between faces. Scope (57 … 82, 4 … 17), dark with two white lens-glint pixels at about (64, 15) and (70, 15) |
| textures/guns/boltactionrifle_1.png | 128×32 | camo 1 "Olive": olive-drab (#303010 / #305030) stock. **Transparent** background (33 % opaque) | same layout |
| textures/guns/boltactionrifle_2.png | 128×32 | camo 2 "Tactical Black": charcoal stock. Transparent background | same layout |

## Energy and exotic weapons

| Path | Size | Depicts / palette | Layout notes | Used by |
|---|---|---|---|---|
| textures/guns/Lasergun.png | 64×64 | Light warm-grey sci-fi pistol-rifle (#909090 / #707070 / #909070) with charcoal sections. **Bright red** emitter-glow regions (bottom-left, y 53…61) for the Glow2/Glow3 blocks. Red indicator pixel at (34, 0). 94 % opaque, black fill | Emitter walls (0, 0 … 44, 14): two **transparent slot strips** (rows 3…5 and 9…11) make see-through slits that reveal the red glow blocks inside. Body (0, 14 … 42, 24). Cell housing (36, 43 … 64, 55), with transparent windows at (44 … 48, 50 … 54) and (58 … 62, 50 … 54). Glow blocks (14, 53 … 36, 61), red and magenta-red (partly transparent) | Laser Gun |
| textures/guns/laserGunNew.png | 128×128 | Mostly flat medium grey (#505050 78 %) with darker grey shading. Small orange-red vent stripes (x 21…39, y 85…88). Pale yellow-green holo-glass patch (44 … 58, 8 … 13). Light-grey energy-cell bands. 65 % opaque | Main box (0, 57 … 64, 89), top box (0, 90 … 62, 118), front box (0, 33 … 46, 56), stock (65 … 116, 46 … 83), energy cell (88 … 116, 46 … 60) light grey with transparent slots. Holo plane (0, 90): its face is partly the top box texture, so the "glass" is the yellow patch on the plane's own UV | Scatter Beam Gun |
| textures/guns/TeslaGun.png | 64×64 | Grey industrial (#909090 / #707070 / #505050) with dark-navy accents (#101030). **Bright blue** glow pixels (one 4×2 at about (34, 9 … 38, 11) for the Glow cubes, plus small dots near (55 … 62, 58 … 62)). One red pixel at about (18, 38). 100 % opaque, black fill | Core and rods (18 … 64, 0 … 9). Body (0, 23 … 32, 33). Capacitors (50, 57 … 64, 64). Stock (0 … 32, 39 … 64) | Tesla Gun |
| textures/guns/NDR.png | 64×64 | Steel grey (#505050 / #909090) with **red** (#b03030) heat-sink panels, pale pink-grey (#d0b0b0) fins and a **bright yellow** (#f0d030) glow block (bottom-right, 42 … 64, 45 … 53) for GLOW. Small blue-violet pixel at (30, 9). Yellow-and-black hazard dots at about (9, 57) and (17, 57). 44 % opaque, transparent background | Reactor housing (38, 25 … 64, 35). Hex tube plates (0, 49 … 28, 64). Crown panel glowA1 at (48, 58 … 64, 64), yellow. Prongs (20 … 40, 0 … 10) | NDR |
| textures/guns/BioGun.png | 64×64 | Black body with pale khaki-grey (#b0b090) metal and dark green (#103010) trim. **Vivid lime green** (#30f030 – #a0ffa0) bio-glow regions: top-left (Glow1 and Glow2 at 0 … 28, 0 … 18) and bottom-right (Glow4 and Glow5 at 32 … 64, 42 … 63). Magenta squares inside the glow areas are transparent (the unused corners of the glow-box unwraps). 97 % opaque | Muzzle (40, 10 … 64, 20). Chamber cage bars (0, 18 … 18, 20). Tank (48, 52 … 64, 64). Stock rails (0 … 40, 46 … 64) | Bio Gun |
| textures/guns/alien_blaster.png | 128×64 | **Pale silver-white** (#d0d0d0 72 %) brushed metal with faint scratches, darker grey panel lines and grip grooves. One bright blue (#0050ff) 8×4 block at (40, 1 … 48, 5) for the muzzle rings. A tiny blue/magenta pixel pair at the top-left (light studs). 100 % opaque | Bulb (0, 0 … 36, 18). Barrel (47, 4 … 73, 17). Fins (40, 19 … 60, 47). Grip (68, 0 … 82, 13), darker grey. Most of the right half is filler texture | Alien Blaster |
| textures/guns/cyberdemonblaster.png | 64×64 | Mid-grey worn metal (#707070 / #909090) with black panel seams. A large **lava-orange** (#f05010 / #f03010) noise block (0, 52 … 26, 64) for the Glow1, Glow3 and Glow4 blocks. Magenta (transparent) cut-outs in the top-left cuff region (rivet holes). 94 % opaque, black fill | Spine (11, 0 … 57, 22). Cuff (0, 0 … 30, 15) and (0, 28 … 22, 39), with transparent slots. Barrel blocks (38, 1 … 62, 12). Rails (26, 48 … 58, 64). Orange glow (0, 52 … 26, 64) | Cyberdemon Blaster |
| textures/guns/neutrinogun.png | 256×256 | Dark maroon-black (#301010 44 %) and charcoal, white (#f0f0f0) T-shaped decals, cyan stripes, **green** and **red** circular "neutrino" glyphs (rings at about (44 … 70, 102 … 112) green and (72 … 90, 232 … 247) red), small yellow and red indicator pixels. 18 % opaque, all in x 0…144 | Model UVs doubled. Most model parts share UV (0, 0), so the top-left dark block (0, 0 … 112, 60) textures nearly every rod | Neutrino Gun (unused) |
| textures/guns/powerHammer.png | 64×64 | Black body with **dark reddish-brown leather** (#503030) housings, grey steel (#707070 / #505050) parts, a pale pink-white piston (26, 0 … 46, 8) and yellow-black hazard stripes on a grey band (0, 24 … 16, 26). 100 % opaque | Hammer face H3 (0, 0 … 10, 10) grey. Head H2 (10, 0 … 22, 10). Body (10, 41 … 46, 50), brown. Housing (27, 9 … 61, 19), grey | Power Hammer |
| textures/guns/powerHammerAdv.png | 64×64 | Identical to powerHammer.png except that the **hammer face and head region (0, 0 … 20, 10) is dark navy-steel** (#101830) instead of grey | as above | Advanced Power Hammer |
| textures/guns/chainsaw.png | 128×64 | **Safety orange** (#f0b010 / #f09010) engine housing with darker orange panel outlines, light-grey (#b0b0b0) metal guide bar with dark rivet holes (top strip, 18 … 86, 0 … 7), and a dark-grey rear handle (right side, 88 … 128, 30 … 64). An exhaust grille and a round white air-filter glint near (21, 37). 98 % opaque, black fill | Engine (0, 23 … 48, 43) and (0, 3 … 26, 23). **Chain planes**: blade2 (0, 48 … 72, 56) and blade1 (0, 56 … 72, 64). Grey chain links with small **transparent notches** along the top and bottom edges, offset between the two rows, so that alternating them animates the teeth. The middle of the strip has a magenta (transparent) gap at x ≈ 33…37 | Chainsaw |
| textures/guns/minigun.png | 128×128 | Black fill (70 %) with light-grey/silver (#b0b0b0 / #d0d0d0) barrels and bearing plates, mid-grey housing, and a **white ammo-box texture** with X-braced corners (bottom: 0 … 42, 94 … 128 and 48 … 92, 107 … 128). 100 % opaque | Housing (42, 17 … 94, 50). Barrels (48, 0 … 94, 6), silver strips. Bearing clamps (94 … 128, 0 … 23). Spade-grip pieces (0 … 40, 26 … 78) | Minigun |

### `textures/guns/Flamethrower.png` (Flamethrower)

| Path | Size | Palette |
|---|---|---|
| textures/guns/Flamethrower.png | 64×64 | black #101010 58 %, white #f0f0f0 9 %, greys, **dull red** #b05050 4 % |

* Content in the top half (y 0…37). The bottom half is black fill.
* **Fuel tank** (0, 22 … 36, 37): white with red stripes and two yellow-and-black hazard dots at about (8, 34) and (23, 34).
* **Barrel and pipes** (24 … 54, 0 … 9): light grey. Body (0, 14 … 26, 22): grey with dark bolts. Nozzle (0, 3 … 10, 14).
* **Weathered red** patches on the right (54 … 64, 0 … 30) and around (12 … 18, 2 … 12), for the stock and valve parts. A small beige patch around (16 … 24, 2 … 8).

## Launchers

| Path | Size | Depicts / palette | Layout notes | Used by |
|---|---|---|---|---|
| textures/guns/rocketlauncher.png | 128×64 | Dark grey/black launcher with a **slate-grey metal tube** (#507070 / #707070, rows 36…64) and a **white rocket with red bands** (#d0d0d0 / red) in the top-left (0 … 30, 0 … 22). Magenta pixels in the tube rows mark transparent cut-outs at the tube ends. 97 % opaque, black fill | Rocket R1/R9 (0, 0 … 20, 8), R2/R8 body (0, 16 … 26, 22), fins (12 … 46, 0 … 16). Tube Shape8 (0, 36 … 82, 46), Shape6 (1, 46 … 79, 55), Shape2 (0, 55 … 84, 64). Front bell (104, 0 … 128, 12), dark, with transparent ports. Grip block (30 … 76, 20 … 32) dark | Rocket Launcher, rocket-ammo item, rocket projectile (area [04](../04-projectiles-fx/index.md)) |
| textures/guns/nukerocketlauncher.png | 128×64 | Same layout. Rocket recoloured **yellow with red-orange bands** (radiation warhead). Tube texture is **olive-brown worn camouflage with dark diagonal hazard striping** (#505030 / #303010) | same | Nuke Launcher, nuclear rocket-ammo item, nuke rocket projectile (area [04](../04-projectiles-fx/index.md)) |
| textures/guns/grimreaper.png | 128×64 | Weathered **khaki-grey / olive-grey** metal (#909070 / #707050 / #505050), panel lines and rivets. **Transparent slot grilles** (magenta in the preview) on the quad-tube faces (around 48 … 80, 33 … 43 and 36 … 70, 55 … 64) give see-through vents. Black square muzzle holes ×4 at (1 … 10, 11 … 20) and (113 … 124, 41 … 51). Small white-and-black detail pixels. 97 % opaque | Front housing (0, 0 … 56, 20). Quad tubes (58, 0 … 128, 30). Rear plate (100, 30 … 128, 52). Bands (36, 32 … 88, 43) | Grim Reaper |
| textures/guns/grenadelauncher.png | 128×128 | **OBJ atlas**, not box UV. Dark grey and black gunmetal (#303030 / #101010 / #505050) with brushed and noisy metal, recessed panel outlines, grille bars (32 … 64, 12 … 24), a light-grey drum side panel (36 … 66, 76 … 112), a black-ringed muzzle hole (20 … 30, 98 … 108), a hex-pattern drum end cap with six round chambers (96 … 125, 75 … 110), and a vertical strip of four **red-bodied 40 mm grenades with brass caps** (104 … 122, 16 … 70) between white rails | The drum OBJ group samples the grenade strip (u 0.797…0.953, v 0.469…0.867). The body group samples the rest. Fully opaque | Grenade Launcher |

## Grenades

| Path | Size | Depicts / palette | Layout notes | Used by |
|---|---|---|---|---|
| textures/guns/stielgranate.png | 32×32 | Stick grenade: **khaki/tan wooden handle** (#909050) and a **dark green** (#305030 / #105030) metal head with a light reflection. 24 % opaque, transparent | Handle (0, 5 … 8, 19) tan. Head (13, 0 … 29, 9) green. End cap (0, 0 … 12, 4) green | Stielhandgranate item and grenade projectile (area [04](../04-projectiles-fx/index.md)) |
| textures/guns/fragGrenade_texture.png | 64×64 | Frag grenade: **olive-green** body (#305030), light-grey fuze, spoon and ring (#707070 / #909090). 31 % opaque, transparent | Model UVs doubled. Body (26, 0 … 58, 16) and (26, 32 … 58, 44), green. Caps (30, 22 … 54, 30), green. Ring (0, 14 … 24, 30): grey with transparent centres, forming a hollow ring. Fuze (30, 46 … 46, 54) grey. Spoon (2 … 62, 56 … 62) grey | Frag grenade item and projectile (area [04](../04-projectiles-fx/index.md)) |

## Unused textures

| Path | Size | Depicts / palette | Pairs with |
|---|---|---|---|
| textures/guns/drillTexture.png | 128×64 | Placeholder: left half solid near-black (#101010), right half dark slate (#252830) with a few grip and trigger details copied from the M4 sheet (magenta transparent background in that region). 89 % opaque | ModelDriller (unused item) |
| textures/guns/mp5Texture.png | 64×32 | see Pistols and SMGs | ModelMp5 (unused) |
| textures/guns/pistol.png, textures/guns/pistol2Texture.png | 64×32 | see Pistols and SMGs | ModelPistol / ModelPistol2 (unused) |
| textures/guns/neutrinogun.png | 256×256 | see Energy and exotic weapons | ModelNeutrinoGun (unused) |

---

## Texture ↔ model size check

| Texture | PNG | Model declares | Ratio |
|---|---|---|---|
| blasterRifle.png | 256×128 | 128×64 | ×2 |
| fragGrenade_texture.png | 64×64 | 32×32 | ×2 |
| mac10texture.png | 128×64 | 64×32 | ×2 |
| mibgun.png | 128×128 | 64×64 | ×2 |
| neutrinogun.png | 256×256 | 128×128 | ×2 |
| pistol3.png | 128×64 | 64×32 | ×2 |
| scar_texture*.png | 256×128 | 128×64 | ×2 |
| vector_texture*.png | 256×128 | 128×64 | ×2 |
| all other gun textures | = declared | | ×1 |

Out-of-sheet UV origins (sampling wraps to the opposite edge): Lasergun `Glow1` (−6, 47), LaserGunNew `TopRails` (−1, 90), PDW `Shape28` (−1, 48). The M4 Infiltrator laser planes at (0, −60) are deliberate (row-0 trick).
