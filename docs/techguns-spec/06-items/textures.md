# Item textures (`textures/items/`, 177 files)

Every PNG in `assets/techguns/textures/items/` is listed below. **None of them is animated**: there are no `.mcmeta` files in this folder. All are plain inventory icons with a transparent background, drawn flat (no UV atlas), except where a note says otherwise. "Palette" gives the three or four dominant opaque colours (median-cut). "Coverage" is the share of opaque pixels, which tells a replacement artist how much of the canvas the subject fills.

Style notes for replacements: icons are 32×32 (some 64×64 or 16×16), with the subject drawn at a 45° diagonal (bottom-left → top-right) for long objects. Lighting comes from the top-left, outlines are dark and soft (no pure-black 1-px outline on most), and shading is painterly and slightly noisy. The 16×16 ingots and nuggets follow the vanilla ingot/nugget silhouettes exactly.

## 1. Ammunition and magazines

| Path | Size | Depicts | Palette | Coverage | Used by |
|---|---|---|---|---|---|
| `textures/items/stonebullets.png` | 32×32 | Pile of 4–5 rough grey stone balls (one large at upper-left, smaller ones lower-right) | #686868, #2b2b2b, #8e8e8e | 53 % | meta 0 Stone Bullet |
| `textures/items/bullets9mm.png` | 32×32 | Three short brass pistol cartridges fanned from lower-right, pale silver-blue rounded tips | brass #8b9068 / #fdf452, tip #d9f3f2 | 47 % | meta 1 |
| `textures/items/pistolMag9mm.png` | 32×32 | Upright slim black pistol magazine with one brass round visible at the top lip | #353635, #1d1e1e, brass #837b56 | 41 % | meta 2 |
| `textures/items/pistolMag9mmEmpty.png` | 32×32 | Same magazine, open top, no round | #353635, #202120 | 40 % | meta 3 |
| `textures/items/shotgunShell.png` | 32×32 | Upright red/salmon shotgun shell with brass base; white/grey pellets spilling out of the open top | red #8d6359 / #a7896e, pellets #e2e2d8, brass #beba8d | 41 % | meta 4 |
| `textures/items/smgMag.png` | 32×32 | Long straight dark-grey stick magazine on the diagonal, a small brass/blue round at the upper-right end | #414143, #5f5f62, #222222 | 30 % | meta 5 |
| `textures/items/smgMagEmpty.png` | 32×32 | Same without the round | #3e3f42, #505153 | 29 % | meta 6 |
| `textures/items/rifleBullets.png` | 32×32 | Four tall brass rifle cartridges standing in a row, one lying horizontally in front | #958b3c, #dccd54, #fdf554, dark #393516 | 45 % | meta 7 |
| `textures/items/rifleBulletsStack.png` | 32×32 | Dense block of a dozen-plus brass rifle cartridges standing and leaning, filling the frame | #72692e, #a99d45, #f2e050 | 84 % | meta 88 |
| `textures/items/highCalibreBullets.png` | 32×32 | Two very long brass rounds crossed in an X, silver tips at the top | #7b7232, #decb4e, #f5e75d | 41 % | meta 19 |
| `textures/items/advBullets.png` | 64×64 | Three futuristic caseless rounds: pale steel-blue bodies with dark navy, needle-like glowing tips; two standing, one lying | #1f272e, #87a7b6, #aec6d3, #c0dbe8 | 35 % | meta 22 |
| `textures/items/pdwMag.png` | 64×64 | Tall ribbed blue-grey steel magazine, tilted, with a glowing pale-blue round at the top | #555e66, #606c76, #657784 | 33 % | meta 23 |
| `textures/items/pdwMagEmpty.png` | 64×64 | Same without the glowing round | #545c63, #5e6a74 | 31 % | meta 24 |
| `textures/items/minigunDrum.png` | 64×64 | Large cylindrical grey drum magazine on the diagonal (ribbed sides, light top face) with a short belt of brass rounds coming out at the upper-left | #c1c0bd, #92908e, #282827 | 63 % | meta 46 |
| `textures/items/minigunDrum_empty.png` | 64×64 | Same drum, no belt | #5c5c5c, #8e8e8e, #262626 | 61 % | meta 47 |
| `textures/items/40mmGrenade.png` | 32×32 | Fat 40 mm grenade round on the diagonal: cream/tan brass case at lower-left, red band, rounded cream warhead at upper-right | #c8bf95, #8a6b4e, red #b34233, #633121 | 38 % | meta 93 |

Metas 12, 13, 14, 17, 18, 20, 21 and 105 have **no** texture here; they are drawn as 3D models (see special-items).

## 2. Tanks, cells and power sources

| Path | Size | Depicts | Palette | Coverage | Used by |
|---|---|---|---|---|---|
| `textures/items/flamethrowerTank.png` | 64×64 | Fat cream-white pressurised canister on the diagonal. Grey screw cap at the top-right, red stripe band, yellow/orange hazard label with a black flame pictogram, and a window showing orange fuel | #ece7cf, #6a2818, #b28d52, #cfb86c | 59 % | meta 8 |
| `textures/items/flamethrowerTankEmpty.png` | 64×64 | Same canister, window grey/empty | #8a765f, #5d241c, #dbd9c4 | 59 % | meta 9 |
| `textures/items/bioTank.png` | 64×64 | Canister on the diagonal with grey metal end caps and a cage of vertical bars over bright toxic-green liquid | #1ed62a, #333e34, #9fa09f | 59 % | meta 10 |
| `textures/items/bioTankEmpty.png` | 64×64 | Same canister, grey interior | #818181, #adadad, #343434 | 59 % | meta 11 |
| `textures/items/energyCell.png` | 64×64 | Square black battery block seen from the front-top: three round metal terminals on top, yellow trim stripes, a red glowing cell grid in the centre window | #2d2525, #a9a45c, red #c91616 | 66 % | meta 15 |
| `textures/items/energyCellEmpty.png` | 64×64 | Same with a dark grey (unlit) window | #292929, #484845, #c7c755 | 66 % | meta 16 |
| `textures/items/nuclearPowerCell.png` | 64×64 | Tilted cylindrical cell: grey end caps, a glowing yellow lens on the top cap, black/yellow body with red-on-white radiation trefoil labels | #c4bd5d, #646045, #121213, red #3a2721 | 49 % | meta 106 |
| `textures/items/nuclearPowerCellDepleted.png` | 64×64 | Same with a dull grey lens and a darker, desaturated body | #514e46, #afac6d, #121112 | 49 % | meta 107 |
| `textures/items/compressedAirTank.png` | 64×64 | Grey scuba-style gas bottle on the diagonal, dark shoulders, a yellow/black quartered hazard label in the middle, a valve nozzle at the upper-right | #bdc0a4, #747671, #0f0f0f | 42 % | meta 28 |
| `textures/items/compressedAirTankEmpty.png` | 64×64 | Same bottle **without the valve** (open neck) | #bdc0a3, #767772, #0f0f0f | 40 % | meta 29 |
| `textures/items/nethercharge.png` | 32×32 | Glowing orange-yellow fire orb (white-hot centre) in a jagged grey-brown rock shell | #ffea44, #fffbb9, #8f7b6b, #7e4227 | 67 % | meta 63 |

## 3. Gun parts

| Path | Size | Depicts | Palette | Coverage | Used by |
|---|---|---|---|---|---|
| `textures/items/stoneBarrel.png` | 32×32 | Thick grey stone tube on the diagonal, open end at the upper-left | #818181, #cbcbcb, #2c2c2c | 33 % | meta 25 |
| `textures/items/ironBarrel.png` | 32×32 | Same tube shape, bright light-grey iron | #dedede, #aeaeae, #676767 | 33 % | meta 27 |
| `textures/items/obsidianSteelBarrel.png` | 32×32 | Same tube, dark blue-violet steel with a light highlight stripe | #2a2b3e, #5e5f71, #a7a7ad | 33 % | meta 33 |
| `textures/items/carbonBarrel.png` | 32×32 | Same tube, near-black matte | #040404, #292929, #424242 | 33 % | meta 85 |
| `textures/items/laserBarrel.png` | 32×32 | Same tube, cream/off-white with a pink-red emitter tip at the upper-left | #a8a78e, #e4e4dd, #b38873 | 33 % | meta 35 |
| `textures/items/woodStock.png` | 32×32 | Wooden rifle buttstock seen from the side, diagonal, pale tan wood with a dark grain outline | #978d76, #51462b, #211b0a | 40 % | meta 26 |
| `textures/items/plasticGunstock.png` | 32×32 | Black polymer buttstock, horizontal wedge pointing left | #1c1c1d, #333334, #535353 | 33 % | meta 31 |
| `textures/items/carbonGunstock.png` | 32×32 | Black skeleton stock, horizontal, with three rectangular cut-outs (transparent holes) along the body | #151515, #0f0f0f, #4e4e4e | 36 % | meta 84 |
| `textures/items/ironReceiver.png` | 32×32 | Side view of a gun receiver: box-shaped upper body with an ejection slot, trigger guard and pistol grip at the lower-right; light grey | #484848, #8c8c8c, #919191 | 36 % | meta 53 |
| `textures/items/steelReceiver.png` | 32×32 | Same silhouette, mid/dark grey | #525252, #2c2c2c, #606060 | 36 % | meta 54 |
| `textures/items/obsidianSteelReceiver.png` | 32×32 | Same silhouette, dark purple-black | #27212a, #37303c | 36 % | meta 55 |
| `textures/items/carbonReceiver.png` | 32×32 | Same silhouette, almost pure black | #060606, #1e1e1e | 36 % | meta 56 |
| `textures/items/pumpMechanism.png` | 64×64 | Diagonal mechanical assembly: grey ribbed piston cylinder at the lower-left, an orange housing with a yellow/black hazard tag (drop icon) at the upper-right, and two thin wire rods alongside | #6a5642, #b6ac9f, #958a7c, orange #d9d08b | 56 % | meta 32 |

## 4. Mechanical and electronic components

| Path | Size | Depicts | Palette | Coverage | Used by |
|---|---|---|---|---|---|
| `textures/items/mechanicalParts.png` | 32×32 | Composition of a large cog (upper-left), a T-bolt (upper-right) and a screw (lower-middle, diagonal); light steel grey | #616161, #7c7c7c, #9d9d9d, #c0c0c0 | 31 % | meta 30 |
| `textures/items/mechanicalPartsT2.png` | 32×32 | Identical layout, dusky purple (obsidian steel) | #47334d, #5c4265, #7d5a89 | 31 % | meta 34 |
| `textures/items/mechanicalPartsT3.png` | 32×32 | Identical layout, black with a faint brown sheen (carbon) | #120d05, #231908, #5d523f | 31 % | meta 37 |
| `textures/items/circuitBoard.png` | 32×32 | Square green PCB filling the frame, gold/yellow trace maze, small red components | #207c2d, #13471a, #f4bd22, #9c8c0e | 77 % | meta 68 |
| `textures/items/circuitBoardElite.png` | 32×32 | Green PCB with pale cream/white traces and blue plus red chips; slightly wider (edge to edge) | #1b6a2f, #ded27f, #387e44, #565556 | 77 % | meta 87 |
| `textures/items/coil.png` | 32×32 | Diagonal rod (grey core visible at the lower-left end) tightly wound with copper wire | #854f25, #9a653b, #bd7841, #cf8952 | 24 % | meta 69 |
| `textures/items/wireCopper.png` | 32×32 | Loose flat spiral of thin tan/copper wire | #b29351, #95733b, #c0a15a | 32 % | meta 57 |
| `textures/items/goldWire.png` | 32×32 | Same spiral, bright yellow-gold | #c6b402, #9f9002, #d5c103 | 32 % | meta 86 |
| `textures/items/electricEngine.png` | 64×64 | Black cylindrical electric motor on the diagonal with cooling fins/ribs and a brass shaft at the upper-right | #262626, #111111, #444443, #77776c | 57 % | meta 64 |
| `textures/items/cyberneticParts.png` | 64×64 | A tangle of white/grey cables and thin red/yellow/brown wires over a grey mechanical plate | #79534f, #282828, #aca79b, #f9faf9 | 50 % | meta 91 |
| `textures/items/sonicEmitter.png` | 64×64 | Black cylinder on the diagonal ending in a grey loudspeaker cone at the upper-right, with glowing pale-blue coils in mid-body | #9ea3a8, #515659, #292a2a | 46 % | meta 97 |
| `textures/items/radEmitter.png` | 64×64 | Black cylinder with yellow bands, a red/black radiation trefoil disc on the top face, orange glowing windows on the side | #201f23, #85795d, #dbd378, red #492929 | 52 % | meta 108 |
| `textures/items/laserFocus.png` | 64×64 | Faceted ruby-red gem in a circular steel lens mount with a black ring, pale light rays radiating outward | #3c1a1a, #c8aeae, red #d83e3e, #fdfdfd | 70 % | meta 36 |

## 5. Metals: ingots, nuggets, plates, ore

### 5a. Ingots and nuggets (vanilla silhouettes, 16×16)

Ingot silhouette: the vanilla ingot (trapezoid bar, 16 px wide, rows 2–13, coverage about 54 %). Nugget silhouette: the vanilla gold nugget (6×8 px, centre, coverage 13 %).

| Path | Size | Colour | Used by |
|---|---|---|---|
| `textures/items/ingotSteel.png` | 16×16 | dark steel: black outline, #656565 body, #cccccc highlight | Techguns `ingotSteel` (config) |
| `textures/items/ingotCopper.png` | 16×16 | golden copper: #bb9827, #dfc161, dark #4c3d0f | `ingotCopper` (config) |
| `textures/items/ingotTin.png` | 16×16 | pale teal: #73ada5, #bbd8d3, #2c4c47 | `ingotTin` (config) |
| `textures/items/ingotBronze.png` | 16×16 | dark orange-brown: #683d03, #b3751f, near-black #1d0b00 | `ingotBronze` (config) |
| `textures/items/ingotLead.png` | 16×16 | blue-grey: #435563, #758ea1, #1c232a | `ingotLead` (config) |
| `textures/items/ingotObsidianSteel.png` | 16×16 | near-black with dark violet highlight #484361 | meta 40 |
| `textures/items/ingotTitanium.png` | 16×16 | bright silver-white: #adadad, #ececec, #424242 | meta 95 |
| `textures/items/nuggetIron.png` | 16×16 | white/light grey: #efefef, #626262 | `nuggetIron` (config) |
| `textures/items/copperNugget.png` | 16×16 | orange-gold: #9f6601, #fcbf54, highlight #ffefd1 | `nuggetCopper` (config) |
| `textures/items/nuggetLead.png` | 16×16 | slate blue: #697b97, #343d4b | `nuggetLead` (config) |
| `textures/items/nuggetSteel.png` | 16×16 | black with grey highlight #b0b0b0 | `nuggetSteel` (config) |

### 5b. Plates, current style (32×32)

The same template for all: a square plate inset 3 px (rows/cols 3–29, coverage 66 %), a thick dark bevel on the right and bottom edges, a lighter top-left bevel, and a soft diagonal sheen across the face.

| Path | Face colour | Used by |
|---|---|---|
| `textures/items/plateIron.png` | light grey #adadad / #c9c9c9, dark edge #363434 | meta 43 |
| `textures/items/plateSteel.png` | mid grey #929292 with a strong white diagonal shine #c7c7c7 | meta 45 |
| `textures/items/plateTin.png` | pale cyan-grey #a6c3c5 / #c0d5d6 | meta 41 |
| `textures/items/plateCopper.png` | warm pale gold #edc368 / #f1cb7b | meta 42 |
| `textures/items/plateBronze.png` | copper-orange #a05729 / #bc7945, very dark edge #1d0f07 | meta 44 |
| `textures/items/plateLead.png` | periwinkle/lavender #6c60a2 / #8379b1 | meta 104 |
| `textures/items/plateObsidianSteel.png` | dark plum #543c5a / #66496d | meta 65 |
| `textures/items/plateTitanium.png` | white #f4f4f4 with high-contrast silver shine, black edge | meta 96 |
| `textures/items/carbonPlate.png` | dark grey #6a6a6a / #949494 with a checker-weave sheen | meta 62 |
| `textures/items/plasticSheet.png` | off-white #dedede / #f5f5f5, soft grey edge | meta 72 |
| `textures/items/rubberBar.png` | flat very dark olive-brown #2e2918 square, almost no shading | meta 73 |
| `textures/items/steamArmorPlate.png` | brownish-bronze #716142 with a black frame and four brass corner bolts | meta 67 |
| `textures/items/powerPlating.png` | mottled composite (beige / grey / bluish speckle #948e86, #ada99f) with a dark frame | meta 89 |

### 5c. Plates, old style (unused)

Older 32×32 plates (inset 2 px, coverage 77 %): a noisy, stone-like metal texture with eight dark "+" rivets (four corners plus four edge midpoints). **No code references them.**

| Path | Colour |
|---|---|
| `textures/items/plateIronOLD.png` | mid grey #636363 / #7a7a7a |
| `textures/items/plateSteelOLD.png` | dark grey #4b4b4b / #585858 |
| `textures/items/plateTinOLD.png` | blue-grey #717883 / #848c9a |
| `textures/items/plateCopperOLD.png` | orange-brown #6d4c17 / #88590f |
| `textures/items/plateBronzeOLD.png` | dull brown #563d16 / #694711 |

### 5d. Ore item

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/items/oreTitanium.png` | 32×32 | Irregular crumbly chunk of pale silver-white ore with dark specks, tilted | #646f70, #92a1a3, #bdcbcd, #eaf3f4 | meta 94 |

## 6. Materials and organics

| Path | Size | Depicts | Palette | Coverage | Used by |
|---|---|---|---|---|---|
| `textures/items/rawRubber.png` | 32×32 | Upright teardrop blob, olive/khaki, glossy highlight on the left | #959676, #51523e, #bdbea8 | 23 % | meta 70 |
| `textures/items/rawPlastic.png` | 32×32 | Mound of white granular pellets | #b9b9b9, #d2d2d2, #ebebeb | 37 % | meta 71 |
| `textures/items/heavyCloth.png` | 32×32 | Thick brown fabric/leather roll, tilted, with dark folds | #422f19, #5d4223, #997141 | 48 % | meta 39 |
| `textures/items/treatedLeather.png` | 32×32 | Flat tanned hide (animal-skin outline with four leg flaps) in very dark green | #1a2a12, #213417 | 52 % | meta 109 |
| `textures/items/bioMass.png` | 64×64 | Lumpy, slimy bright-green blob with lighter highlights | #468d05, #5cb40b, #316b04 | 63 % | meta 38 |
| `textures/items/carbonFibers.png` | 32×32 | Open honeycomb lattice of dark-grey hexagon outlines (graphene-like); transparent inside each cell | #4d4d4d, #3b3b3b, #6a6a6a | 29 % | meta 61 |
| `textures/items/tgx.png` | 32×32 | Pale ice-blue crystalline lump with white highlights and dark-blue facets | #4f7393, #74b1dd, #9ad7f3, #d4eefc | 56 % | meta 60 |
| `textures/items/quartz_rod.png` | 16×16 | Thin light-grey/white rod on the diagonal, 2 px wide | #b8b8b8, #d6d6d6 | 20 % | meta 99 |
| `textures/items/yellowcake.png` | 16×16 | Small rounded pile of chartreuse/yellow-green powder | #a6b023, #dce656, #424512 | 45 % | meta 101 |
| `textures/items/enrichedUranium.png` | 16×16 | Pale olive-green oval pellet with horizontal bands | #6d9055, #86a96e, #303f26 | 54 % | meta 102 |

## 7. Machine items

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/items/turretArmorIron.png` | 32×32 | Square armour plate (inset 2 px) with a riveted border (8 rivets per side) and a large bold black "T" embossed in the centre; light polished grey | #a0a0a0, #bababa, #242424 | meta 48 |
| `textures/items/turretArmorSteel.png` | 32×32 | Same design, mid grey | #727272, #9f9f9f, #0f0f0f | meta 49 |
| `textures/items/turretArmorObsidianSteel.png` | 32×32 | Same design, grey-violet | #605868, #91889a | meta 50 |
| `textures/items/turretArmorCarbon.png` | 32×32 | Same design, dark charcoal | #3c3c3c, #646464, #0b0b0b | meta 66 |
| `textures/items/oreDrillSmallSteel.png` | 32×32 | Small downward-pointing conical drill bit with spiral grooves, centred (16×17 px) | #575657, #777576, #c8c4c5 | meta 75 |
| `textures/items/oreDrillSmallObsidianSteel.png` | 32×32 | Same, dark violet-grey | #302932, #524756 | meta 76 |
| `textures/items/oreDrillSmallCarbon.png` | 32×32 | Same, black | #080808, #333333 | meta 77 |
| `textures/items/oreDrillMediumSteel.png` | 32×32 | Larger downward cone (22×26 px), horizontal spiral bands | #646263, #878586, #d7d3d5 | meta 78 |
| `textures/items/oreDrillMediumObsidianSteel.png` | 32×32 | Same, steel-blue/dark slate | #3f5165, #2a3744 | meta 79 |
| `textures/items/oreDrillMediumCarbon.png` | 32×32 | Same, black | #101010, #2a2a2a | meta 80 |
| `textures/items/oreDrillLargeSteel.png` | 32×32 | Big spiral cone lying on the diagonal, tip at the lower-left, base at the upper-right (fills 26×26) | #afafaf, #939293, #e3e3e3 | meta 81 |
| `textures/items/oreDrillLargeObsidianSteel.png` | 32×32 | Same, dark violet | #201c29, #332e42, #413a55 | meta 82 |
| `textures/items/oreDrillLargeCarbon.png` | 32×32 | Same, black | #161616, #252525 | meta 83 |
| `textures/items/machineStackUpgrade.png` | 32×32 | Green circuit card filling the frame: a yellow "+" at the top-left, a tall yellow up-arrow on the right, two small brown crates at the bottom-left | #21581d, #83960b, #123814 | meta 52 |
| `textures/items/uvemitter.png` | 64×64 | Wide dark trapezoid lamp housing (top) over a glowing purple/violet lens; occupies the middle band only (rows 16–37) | #9275b7, #6c5091, #2d2d2d | meta 92 |
| `textures/items/rcHeatRay.png` | 64×64 | Upright device: black housing with a grey frame, a round lens with a cross-grid glowing yellow-white in the upper half, and an orange glowing bulb at the bottom | #656562, #c7c2a9, #191918 | meta 98 |

## 8. Exotic, nuclear and anti-gravity

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/items/antiGravityCore.png` | 32×32 | Octagonal dark-grey disc with a glowing cyan/blue four-pointed star in the centre | #1b8fc4, #25c2df, #424552 | meta 100 |
| `textures/items/tacticalNuclearWarhead.png` | 32×32 | Flat lens-shaped grey casing, horizontal (rows 10–24), with a yellow-and-black radiation trefoil in the middle | #333333, #444444, #d3ec01 | meta 103 |

## 9. Wearables and special-slot icons

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/items/glider.png` | 32×32 | Front view: brown backpack in the centre with cream canvas wings (dark frame) spreading left and right | #784535, #33221d, #b7b49d | meta 51 Glider |
| `textures/items/gliderBackback.png` | 32×32 | Just the brown backpack with two straps | #37211b, #6e3f31, #794636 | meta 59 |
| `textures/items/gliderWing.png` | 32×32 | One cream canvas wing panel (trapezoid) with a dark grey spar frame and vertical ribs | #c0bda5, #2c2c2c, #858372 | meta 58 |
| `textures/items/oxygenMask.png` | 16×16 | Horizontal grey breathing mask: ribbed centre canister with a small valve on top, straps out to both sides | #747474, #888888, #adadad | meta 90 |
| `textures/items/gasMaskFilter.png` | 32×32 | Round black filter disc, face-on, with six radial spokes | #272e27, #101310 | meta 74 |
| `textures/items/gasMask.png` | 32×32 | Front view of a dark green gas mask, two round eye lenses (transparent holes), round filter at the bottom | #3a443d, #26392b, #3a6043 | Gas Mask (05) |
| `textures/items/nightVisionGoggles.png` | 32×32 | Black head strap across the top, two round green lenses on a black housing below | #141613, #060606, green #017925 | Nightvision Goggles (05) |
| `textures/items/tacticalMask.png` | 16×16 | Black full-face helmet/mask with a dark red visor band | #0e0e0e, red #591919 | Tactical Mask (05) |
| `textures/items/scubaTanks.png` | 32×32 | Two grey cylinders side by side, joined by dark straps | #181818, #7d7d7d, #9d9d9d | Oxygen Tanks (05) |
| `textures/items/jetpack.png` | 64×64 | Front view, middle band only (rows 16–47): two grey thruster cylinders with yellow/black hazard caps on top, joined by a dark central frame | #171512, #5e5c5e, #918f83 | Jetpack (05) |
| `textures/items/jumppack.png` | 32×32 | Two upright grey canisters with yellow/black striped tops on a dark frame with straps | #747273, #24201a, #989686 | Jump Pack (05) |
| `textures/items/antiGravPack.png` | 32×32 | Rectangular grey machined frame with dark side clamps and a glowing blue diamond in the centre | #57626c, #9ea3a5, #4eabc2 | Anti Gravity Device (05) |

## 10. Armour item icons (owned by 05-armor, stored here)

All are 32×32 front-view inventory icons in the vanilla armour-icon layout: helmet in the upper area, chestplate as a torso, leggings as a pair of trousers, boots as a pair of boots. They are used as `techguns:<armour unlocalized name>` by `GenericArmor` (05).

| Path | Depicts | Palette |
|---|---|---|
| `textures/items/t1_combat_helmet.png` | Soldier's helmet: grey-white dome with a brown chin strap | #bfc0b2, #3c362f |
| `textures/items/t1_combat_chestplate.png` | Brown leather/khaki vest with pouches | #352719, #4a3722, #58462f |
| `textures/items/t1_combat_leggings.png` | Brown trousers with side pockets | #2b1f15, #513e28 |
| `textures/items/t1_combat_boots.png` | Black lace-up boots | #26231f, #5d5955 |
| `textures/items/t1_miner_helmet.png` | Yellow hard hat with a white headlamp | #e7db04, #5c5613 |
| `textures/items/t1_miner_chestplate.png` | Blue dungaree bib | #312f6a, #413f87 |
| `textures/items/t1_miner_leggings.png` | Blue work trousers | #343273, #3c3a81 |
| `textures/items/t1_miner_boots.png` | Dark boots with yellow cuffs and grey toes | #2f2f2e, #929075 |
| `textures/items/t1_scout_helmet.png` | Tan hood/bandana with a dark face cloth | #695e4d, #8c7f64 |
| `textures/items/t1_scout_chestplate.png` | Ragged tan/brown bandit vest | #3c2d19, #7f7256 |
| `textures/items/t1_scout_leggings.png` | Tan ragged trousers | #74674b, #827557 |
| `textures/items/t1_scout_boots.png` | Dark brown worn boots | #352c26, #463d37 |
| `textures/items/t2_beret.png` | Dark red beret, side view | #520000, #300000 |
| `textures/items/t2_combat_helmet.png` | Olive-grey combat helmet with dark goggles | #596054, #666f62 |
| `textures/items/t2_combat_chestplate.png` | Dark olive plate carrier | #191c16, #282c24 |
| `textures/items/t2_combat_leggings.png` | Grey-olive cargo trousers | #3c3c38, #4d4e45 |
| `textures/items/t2_combat_boots.png` | Dark grey combat boots | #1f1c19, #454341 |
| `textures/items/t2_commando_helmet.png` | Black balaclava/helmet with a grey goggle visor and respirator | #2c2c2c, #808080 |
| `textures/items/t2_commando_chestplate.png` | Black tactical vest | #131313, #383838 |
| `textures/items/t2_commando_leggings.png` | Black trousers | #1b1b1b, #323232 |
| `textures/items/t2_commando_boots.png` | Black boots | #242424, #0a0a0a |
| `textures/items/t2_riot_helmet.png` | Olive drab helmet (Ranger Veteran) | #4b4c39, #606248 |
| `textures/items/t2_riot_chestplate.png` | Brown duster coat with a green bandolier of brass rounds | #503a1f, #382d19 |
| `textures/items/t2_riot_leggings.png` | Olive drab trousers | #3e3a25, #545238 |
| `textures/items/t2_riot_boots.png` | Dark olive boots | #302f24, #41402f |
| `textures/items/t3_combat_helmet.png` | White armoured helmet with a blue visor | #9db0bd, #2c476b, #f8f9fa |
| `textures/items/t3_combat_chestplate.png` | White segmented chest armour on a dark undersuit, small red emblem | #192025, #dbe2e7 |
| `textures/items/t3_combat_leggings.png` | Dark navy leggings with white knee pads | #252f37, #91a3b1 |
| `textures/items/t3_combat_boots.png` | White armoured boots on a dark base | #12171b, #cfd8df |
| `textures/items/t3_exo_helmet.png` | Like the t3 combat helmet: white/blue visor with grey trim | #2e4466, #d4dce2 |
| `textures/items/t3_exo_chestplate.png` | White chest plates on a grey exo frame, red emblem | #1e252b, #dbe2e7, #7b7e88 |
| `textures/items/t3_exo_leggings.png` | Dark leggings with grey exoskeleton struts | #27323a, #a4a0a4 |
| `textures/items/t3_exo_boots.png` | Grey/white exo boots | #afacae, #dde3e7 |
| `textures/items/t3_miner_helmet.png` | Black HEV-style helmet with a blue-grey visor and an orange band | #0f0f0f, #305875 |
| `textures/items/t3_miner_chestplate.png` | Black/orange HEV chest with ribbed plating | #0f0f0f, #d18823 |
| `textures/items/t3_miner_leggings.png` | Black leggings with orange panels | #000000, #bd7819 |
| `textures/items/t3_miner_boots.png` | Black boots with orange stripes | #000000, #996215 |

## 11. Tools, placers, door, bucket, debug items

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/items/combatKnife.png` | 96×96 | Dark gunmetal combat knife on the diagonal (blade upper-right, grip lower-left, small cross-guard). It occupies only about 60×58 px of the canvas (coverage 7 %), so it is drawn smaller than other icons | #2f3332, #373b39, #3e4c46 | Combat Knife |
| `textures/items/crowbar.png` | 32×32 | Thin dark-red/maroon crowbar on the diagonal, curved hook at the top-right, flat claw at the bottom-left | #632f2f, #ad5757, #462626 | Crowbar |
| `textures/items/camoNetPlacer0.png` | 32×32 | Two olive wooden poles on the diagonal with a frayed net between them, woodland green camo pattern, small transparent holes | #6a784b, #567e55, #3c3e26 | Camo Net Placer meta 0 |
| `textures/items/camoNetPlacer1.png` | 32×32 | Same, snow camo: white/grey net | #686845 poles, #9a9a9a, #b8b8b8 | meta 1 |
| `textures/items/camoNetPlacer2.png` | 32×32 | Same, desert camo: sand-yellow net | #6d6e40, #abb25b, #c0c671 | meta 2 |
| `textures/items/metalDoor.png` | 32×32 | Grey-green bunker door, front view: rounded top corners, rivets along the frame, a vertical handle bar on the right | #666c63, #798075, #a6aca0 | Bunker Door item |
| `textures/items/bucket_acid.png` | 16×16 | Vanilla bucket silhouette filled with murky ochre/brown-yellow liquid | #dfdfdf, #9f9f9f, liquid #866918 / #66562c | Creeper Acid Bucket |
| `textures/items/oreClusterScanner.png` | 32×32 | Grey handheld scanner with two antennae; green grid screen at the left, small red readout and keypad at the right | #444444, #545654, green #009000 | Ore Cluster Scanner |
| `textures/items/blockReaderTool.png` | 32×32 | Magnifying glass (white lens rim, grey handle to the lower-right) over a grey stone cube | #6f7073, #b9b9b9, #090909 | Block Reader Tool |
| `textures/items/structureScanTool.png` | 32×32 | Magnifying glass over a dark-red brick wall | #000000, #a2a2a2, #421616 | Building Scan Tool |
| `textures/items/structureTestTool.png` | 32×32 | Spanner and hammer crossed over a dark-red brick wall | #030303, #471e1e, #818181 | Structure Test Tool |
| `textures/items/testItem.png` | 32×32 | Placeholder: the letters "LEL" scrawled in black | #000000 | Ore Cluster Debugger; `GenericItem` default |
| `textures/items/spawn_egg.png` | 32×32 | Speckled egg in **greyscale** (it is tinted at runtime with the egg's base colour) | #797979, #a1a1a1, #d4d4d4 | all spawn eggs, render pass 0 |
| `textures/items/spawn_egg_overlay.png` | 32×32 | The letters "TG" in white with a dark grey outline, centred (rows 11–25), tinted at runtime with the spots colour | #4f4f4f, #ffffff | all spawn eggs, render pass 1 |

## 12. Unused / unregistered

| Path | Size | Depicts | Status |
|---|---|---|---|
| `textures/items/handgun.png` | 32×32 | Old grey pistol/hand-cannon barrel on the diagonal with a brown grip at the lower-right (#36322b, #595959, #8b8c8c) | No code reference (guns use 3D models and textures under `textures/guns/`) |
| `textures/items/medkit_0.png` | 32×32 | Roll of white gauze bandage with an unrolled strip (#d6d6d6, #898989) | Only `ItemMedkit`, which is not registered |
| `textures/items/medkit_1.png` | 32×32 | Red first-aid pack, its white panel showing a red heart (#aa3636, #b8b8b8, #7d2828) | Only `ItemMedkit`, which is not registered |
| `textures/items/plate*OLD.png` (5 files) | 32×32 | see §5c | unused |

## Coverage check

Each of the 177 PNG files in the folder appears exactly once above by its full path. (The `plate*OLD.png` row in §12 only points back to §5c.) Breakdown: ammo/mags 16, tanks/cells 11, gun parts 13, components 13, ingots/nuggets 11, plates 13 + 5 old, ore 1, materials 10, machine items 16, exotic 2, wearable icons 12, armour icons 37, tools/placers/debug 14, unused 3.
