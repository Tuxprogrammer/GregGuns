# Item textures (`textures/items/`, 177 files)

Every PNG in `assets/techguns/textures/items/` is listed below. **None of them is animated**: there are no `.mcmeta` files in this folder. All are plain inventory icons with a transparent background, drawn flat (no UV atlas), except where a note says otherwise. "Palette" gives the three or four dominant opaque colors (median-cut). "Coverage" is the share of opaque pixels, which tells a replacement artist how much of the canvas the subject fills.

**Measured column.** The last column of every table quotes [ASSET-FACTS.md](../ASSET-FACTS.md) verbatim: canvas size; animation (`static` = no `.mcmeta`, which applies to all 177 files here); the average color of the opaque pixels; the three dominant colors after bucketing each channel to 8 levels, with their share of opaque pixels; and the opaque share of the canvas. The earlier "Palette" and "Coverage" columns came from my own median-cut quantization, so their hex values differ slightly from the bucketed ones. Both are kept. Where a figure is needed, quote the measured column.

**Layout.** None of these files is a UV atlas or a GUI texture. Each one is a single flat inventory icon drawn over the whole canvas, so the only "region" is the opaque bounding box mentioned in the descriptions. Minecraft draws them at 16×16 screen pixels whatever the canvas size, so 32×32 and 64×64 icons are 2× and 4× high-resolution icons, and the 96×96 knife is 6×.

Style notes for replacements: icons are 32×32 (some 64×64 or 16×16), with the subject drawn at a 45° diagonal (bottom-left → top-right) for long objects. Lighting comes from the top-left, outlines are dark and soft (no pure-black 1-px outline on most), and shading is painterly and slightly noisy. The 16×16 ingots and nuggets follow the vanilla ingot/nugget silhouettes exactly.

## 1. Ammunition and magazines

| Path | Size | Depicts | Palette | Coverage | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/items/stonebullets.png` | 32×32 | Pile of 4–5 rough gray stone balls (one large at upper-left, smaller ones lower-right) | #686868, #2b2b2b, #8e8e8e | 53 % | meta 0 Stone Bullet | 32×32 · static · avg #595959 · #707070 28%, #303030 25%, #505050 19% · opaque 53% |
| `textures/items/bullets9mm.png` | 32×32 | Three short brass pistol cartridges fanned from lower-right, pale silver-blue rounded tips | brass #8b9068 / #fdf452, tip #d9f3f2 | 47 % | meta 1 | 32×32 · static · avg #9EA370 · #F0F050 7%, #D0F0F0 5%, #D0B030 5% · opaque 47% |
| `textures/items/pistolMag9mm.png` | 32×32 | Upright slim black pistol magazine with one brass round visible at the top lip | #353635, #1d1e1e, brass #837b56 | 41 % | meta 2 | 32×32 · static · avg #3B3A34 · #303030 67%, #101010 11%, #505050 5% · opaque 40% |
| `textures/items/pistolMag9mmEmpty.png` | 32×32 | Same magazine, open top, no round | #353635, #202120 | 40 % | meta 3 | 32×32 · static · avg #2B2B2B · #303030 69%, #101010 21%, #505050 5% · opaque 40% |
| `textures/items/shotgunShell.png` | 32×32 | Upright red/salmon shotgun shell with brass base; white/gray pellets spilling out of the open top | red #8d6359 / #a7896e, pellets #e2e2d8, brass #beba8d | 41 % | meta 4 | 32×32 · static · avg #957B6A · #905050 8%, #F0F0F0 8%, #B0B070 6% · opaque 41% |
| `textures/items/smgMag.png` | 32×32 | Long straight dark-gray stick magazine on the diagonal, a small brass/blue round at the upper-right end | #414143, #5f5f62, #222222 | 30 % | meta 5 | 32×32 · static · avg #494A49 · #505050 34%, #707070 16%, #303030 15% · opaque 29% |
| `textures/items/smgMagEmpty.png` | 32×32 | Same without the round | #3e3f42, #505153 | 29 % | meta 6 | 32×32 · static · avg #444447 · #505050 36%, #303030 18%, #707070 15% · opaque 28% |
| `textures/items/rifleBullets.png` | 32×32 | Four tall brass rifle cartridges standing in a row, one lying horizontally in front | #958b3c, #dccd54, #fdf554, dark #393516 | 45 % | meta 7 | 32×32 · static · avg #AAA143 · #F0F050 12%, #F0F070 10%, #101010 6% · opaque 45% |
| `textures/items/rifleBulletsStack.png` | 32×32 | Dense block of a dozen-plus brass rifle cartridges standing and leaning, filling the frame | #72692e, #a99d45, #f2e050 | 84 % | meta 88 | 32×32 · static · avg #8E8336 · #505010 7%, #B09030 7%, #303010 6% · opaque 84% |
| `textures/items/highCalibreBullets.png` | 32×32 | Two very long brass rounds crossed in an X, silver tips at the top | #7b7232, #decb4e, #f5e75d | 41 % | meta 19 | 32×32 · static · avg #958A38 · #303010 8%, #D0B030 7%, #101010 6% · opaque 41% |
| `textures/items/advBullets.png` | 64×64 | Three futuristic caseless rounds: pale steel-blue bodies with dark navy, needle-like glowing tips; two standing, one lying | #1f272e, #87a7b6, #aec6d3, #c0dbe8 | 35 % | meta 22 | 64×64 · static · avg #6B8390 · #303030 11%, #90B0B0 10%, #B0D0D0 9% · opaque 35% |
| `textures/items/pdwMag.png` | 64×64 | Tall ribbed blue-gray steel magazine, tilted, with a glowing pale-blue round at the top | #555e66, #606c76, #657784 | 33 % | meta 23 | 64×64 · static · avg #5A666F · #507070 21%, #707070 16%, #505050 14% · opaque 33% |
| `textures/items/pdwMagEmpty.png` | 64×64 | Same without the glowing round | #545c63, #5e6a74 | 31 % | meta 24 | 64×64 · static · avg #545F68 · #507070 22%, #707070 17%, #505050 16% · opaque 30% |
| `textures/items/minigunDrum.png` | 64×64 | Large cylindrical gray drum magazine on the diagonal (ribbed sides, light top face) with a short belt of brass rounds coming out at the upper-left | #c1c0bd, #92908e, #282827 | 63 % | meta 46 | 64×64 · static · avg #767674 · #B0B0B0 18%, #707070 16%, #303030 15% · opaque 63% |
| `textures/items/minigunDrum_empty.png` | 64×64 | Same drum, no belt | #5c5c5c, #8e8e8e, #262626 | 61 % | meta 47 | 64×64 · static · avg #747474 · #B0B0B0 19%, #707070 17%, #303030 16% · opaque 60% |
| `textures/items/40mmGrenade.png` | 32×32 | Fat 40 mm grenade round on the diagonal: cream/tan brass case at lower-left, red band, rounded cream warhead at upper-right | #c8bf95, #8a6b4e, red #b34233, #633121 | 38 % | meta 93 | 32×32 · static · avg #856349 · #707050 9%, #505030 7%, #101010 6% · opaque 38% |

Metas 12, 13, 14, 17, 18, 20, 21 and 105 have **no** texture here; they are drawn as 3D models (see special-items).

## 2. Tanks, cells and power sources

| Path | Size | Depicts | Palette | Coverage | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/items/flamethrowerTank.png` | 64×64 | Fat cream-white pressurized canister on the diagonal. Gray screw cap at the top-right, red stripe band, yellow/orange hazard label with a black flame pictogram, and a window showing orange fuel | #ece7cf, #6a2818, #b28d52, #cfb86c | 59 % | meta 8 | 64×64 · static · avg #AF936C · #F0F0F0 8%, #F0F0D0 6%, #D0D0D0 6% · opaque 58% |
| `textures/items/flamethrowerTankEmpty.png` | 64×64 | Same canister, window gray/empty | #8a765f, #5d241c, #dbd9c4 | 59 % | meta 9 | 64×64 · static · avg #A28C74 · #F0F0F0 8%, #D0D0D0 7%, #F0F0D0 6% · opaque 58% |
| `textures/items/bioTank.png` | 64×64 | Canister on the diagonal with gray metal end caps and a cage of vertical bars over bright toxic-green liquid | #1ed62a, #333e34, #9fa09f | 59 % | meta 10 | 64×64 · static · avg #549257 · #909090 12%, #505050 10%, #B0B0B0 9% · opaque 58% |
| `textures/items/bioTankEmpty.png` | 64×64 | Same canister, gray interior | #818181, #adadad, #343434 | 59 % | meta 11 | 64×64 · static · avg #6E6E6E · #505050 25%, #909090 23%, #707070 17% · opaque 58% |
| `textures/items/energyCell.png` | 64×64 | Square black battery block seen from the front-top: three round metal terminals on top, yellow trim stripes, a red glowing cell grid in the center window | #2d2525, #a9a45c, red #c91616 | 66 % | meta 15 | 64×64 · static · avg #624734 · #303030 29%, #101010 16%, #505050 10% · opaque 65% |
| `textures/items/energyCellEmpty.png` | 64×64 | Same with a dark gray (unlit) window | #292929, #484845, #c7c755 | 66 % | meta 16 | 64×64 · static · avg #4B4B39 · #303030 35%, #101010 21%, #505050 14% · opaque 65% |
| `textures/items/nuclearPowerCell.png` | 64×64 | Tilted cylindrical cell: gray end caps, a glowing yellow lens on the top cap, black/yellow body with red-on-white radiation trefoil labels | #c4bd5d, #646045, #121213, red #3a2721 | 49 % | meta 106 | 64×64 · static · avg #5A5435 · #101010 31%, #303030 11%, #505050 9% · opaque 48% |
| `textures/items/nuclearPowerCellDepleted.png` | 64×64 | Same with a dull gray lens and a darker, desaturated body | #514e46, #afac6d, #121112 | 49 % | meta 107 | 64×64 · static · avg #504938 · #101010 33%, #303030 12%, #505050 11% · opaque 48% |
| `textures/items/compressedAirTank.png` | 64×64 | Gray scuba-style gas bottle on the diagonal, dark shoulders, a yellow/black quartered hazard label in the middle, a valve nozzle at the upper-right | #bdc0a4, #747671, #0f0f0f | 42 % | meta 28 | 64×64 · static · avg #5B5D54 · #101010 31%, #303030 13%, #B0B0B0 9% · opaque 42% |
| `textures/items/compressedAirTankEmpty.png` | 64×64 | Same bottle **without the valve** (open neck) | #bdc0a3, #767772, #0f0f0f | 40 % | meta 29 | 64×64 · static · avg #5C5D54 · #101010 31%, #303030 13%, #B0B0B0 10% · opaque 39% |
| `textures/items/nethercharge.png` | 32×32 | Glowing orange-yellow fire orb (white-hot center) in a jagged gray-brown rock shell | #ffea44, #fffbb9, #8f7b6b, #7e4227 | 67 % | meta 63 | 32×32 · static · avg #B99A60 · #707070 15%, #909090 10%, #B0B0B0 7% · opaque 66% |

## 3. Gun parts

| Path | Size | Depicts | Palette | Coverage | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/items/stoneBarrel.png` | 32×32 | Thick gray stone tube on the diagonal, open end at the upper-left | #818181, #cbcbcb, #2c2c2c | 33 % | meta 25 | 32×32 · static · avg #767676 · #505050 22%, #303030 19%, #909090 18% · opaque 32% |
| `textures/items/ironBarrel.png` | 32×32 | Same tube shape, bright light-gray iron | #dedede, #aeaeae, #676767 | 33 % | meta 27 | 32×32 · static · avg #949494 · #707070 21%, #505050 15%, #B0B0B0 14% · opaque 32% |
| `textures/items/obsidianSteelBarrel.png` | 32×32 | Same tube, dark blue-violet steel with a light highlight stripe | #2a2b3e, #5e5f71, #a7a7ad | 33 % | meta 33 | 32×32 · static · avg #565764 · #303050 14%, #B0B0B0 14%, #505070 14% · opaque 32% |
| `textures/items/carbonBarrel.png` | 32×32 | Same tube, near-black matte | #040404, #292929, #424242 | 33 % | meta 85 | 32×32 · static · avg #202020 · #101010 50%, #303030 39%, #505050 10% · opaque 32% |
| `textures/items/laserBarrel.png` | 32×32 | Same tube, cream/off-white with a pink-red emitter tip at the upper-left | #a8a78e, #e4e4dd, #b38873 | 33 % | meta 35 | 32×32 · static · avg #C3B9A8 · #B0B090 21%, #D0D0D0 21%, #909070 14% · opaque 32% |
| `textures/items/woodStock.png` | 32×32 | Wooden rifle buttstock seen from the side, diagonal, pale tan wood with a dark grain outline | #978d76, #51462b, #211b0a | 40 % | meta 26 | 32×32 · static · avg #5A5039 · #505030 22%, #303010 13%, #909070 13% · opaque 40% |
| `textures/items/plasticGunstock.png` | 32×32 | Black polymer buttstock, horizontal wedge pointing left | #1c1c1d, #333334, #535353 | 33 % | meta 31 | 32×32 · static · avg #303031 · #303030 60%, #101010 24%, #505050 13% · opaque 32% |
| `textures/items/carbonGunstock.png` | 32×32 | Black skeleton stock, horizontal, with three rectangular cut-outs (transparent holes) along the body | #151515, #0f0f0f, #4e4e4e | 36 % | meta 84 | 32×32 · static · avg #1B1B1B · #101010 79%, #303030 9%, #505050 7% · opaque 35% |
| `textures/items/ironReceiver.png` | 32×32 | Side view of a gun receiver: box-shaped upper body with an ejection slot, trigger guard and pistol grip at the lower-right; light gray | #484848, #8c8c8c, #919191 | 36 % | meta 53 | 32×32 · static · avg #707070 · #909090 42%, #505050 29%, #707070 23% · opaque 36% |
| `textures/items/steelReceiver.png` | 32×32 | Same silhouette, mid/dark gray | #525252, #2c2c2c, #606060 | 36 % | meta 54 | 32×32 · static · avg #494949 · #505050 42%, #303030 31%, #707070 22% · opaque 36% |
| `textures/items/obsidianSteelReceiver.png` | 32×32 | Same silhouette, dark purple-black | #27212a, #37303c | 36 % | meta 55 | 32×32 · static · avg #2A252E · #303030 64%, #301030 18%, #101010 11% · opaque 36% |
| `textures/items/carbonReceiver.png` | 32×32 | Same silhouette, almost pure black | #060606, #1e1e1e | 36 % | meta 56 | 32×32 · static · avg #0B0B0B · #101010 94%, #303030 5% · opaque 36% |
| `textures/items/pumpMechanism.png` | 64×64 | Diagonal mechanical assembly: gray ribbed piston cylinder at the lower-left, an orange housing with a yellow/black hazard tag (drop icon) at the upper-right, and two thin wire rods alongside | #6a5642, #b6ac9f, #958a7c, orange #d9d08b | 56 % | meta 32 | 64×64 · static · avg #7D7360 · #505050 16%, #B0B0B0 15%, #707070 14% · opaque 56% |

## 4. Mechanical and electronic components

| Path | Size | Depicts | Palette | Coverage | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/items/mechanicalParts.png` | 32×32 | Composition of a large cog (upper-left), a T-bolt (upper-right) and a screw (lower-middle, diagonal); light steel gray | #616161, #7c7c7c, #9d9d9d, #c0c0c0 | 31 % | meta 30 | 32×32 · static · avg #747474 · #707070 30%, #505050 24%, #909090 15% · opaque 31% |
| `textures/items/mechanicalPartsT2.png` | 32×32 | Identical layout, dusky purple (obsidian steel) | #47334d, #5c4265, #7d5a89 | 31 % | meta 34 | 32×32 · static · avg #513A58 · #503050 25%, #705070 24%, #303030 15% · opaque 31% |
| `textures/items/mechanicalPartsT3.png` | 32×32 | Identical layout, black with a faint brown sheen (carbon) | #120d05, #231908, #5d523f | 31 % | meta 37 | 32×32 · static · avg #211B10 · #101010 55%, #301010 13%, #303010 9% · opaque 31% |
| `textures/items/circuitBoard.png` | 32×32 | Square green PCB filling the frame, gold/yellow trace maze, small red components | #207c2d, #13471a, #f4bd22, #9c8c0e | 77 % | meta 68 | 32×32 · static · avg #52721F · #107030 34%, #F0D030 7%, #307030 7% · opaque 76% |
| `textures/items/circuitBoardElite.png` | 32×32 | Green PCB with pale cream/white traces and blue plus red chips; slightly wider (edge to edge) | #1b6a2f, #ded27f, #387e44, #565556 | 77 % | meta 87 | 32×32 · static · avg #5C7F4E · #107030 26%, #107050 8%, #F0F0F0 7% · opaque 77% |
| `textures/items/coil.png` | 32×32 | Diagonal rod (gray core visible at the lower-left end) tightly wound with copper wire | #854f25, #9a653b, #bd7841, #cf8952 | 24 % | meta 69 | 32×32 · static · avg #8D5C38 · #B07030 14%, #D09050 14%, #905030 11% · opaque 23% |
| `textures/items/wireCopper.png` | 32×32 | Loose flat spiral of thin tan/copper wire | #b29351, #95733b, #c0a15a | 32 % | meta 57 | 32×32 · static · avg #AC8C4B · #B09050 67%, #907050 10%, #907030 5% · opaque 32% |
| `textures/items/goldWire.png` | 32×32 | Same spiral, bright yellow-gold | #c6b402, #9f9002, #d5c103 | 32 % | meta 86 | 32×32 · static · avg #BDAB02 · #D0B010 57%, #B09010 13%, #B0B010 12% · opaque 32% |
| `textures/items/electricEngine.png` | 64×64 | Black cylindrical electric motor on the diagonal with cooling fins/ribs and a brass shaft at the upper-right | #262626, #111111, #444443, #77776c | 57 % | meta 64 | 64×64 · static · avg #333331 · #303030 48%, #101010 27%, #505050 15% · opaque 56% |
| `textures/items/cyberneticParts.png` | 64×64 | A tangle of white/gray cables and thin red/yellow/brown wires over a gray mechanical plate | #79534f, #282828, #aca79b, #f9faf9 | 50 % | meta 91 | 64×64 · static · avg #8E847F · #F0F0F0 16%, #505050 13%, #D0D0D0 9% · opaque 50% |
| `textures/items/sonicEmitter.png` | 64×64 | Black cylinder on the diagonal ending in a gray loudspeaker cone at the upper-right, with glowing pale-blue coils in mid-body | #9ea3a8, #515659, #292a2a | 46 % | meta 97 | 64×64 · static · avg #4F5154 · #303030 35%, #101010 14%, #505050 14% · opaque 46% |
| `textures/items/radEmitter.png` | 64×64 | Black cylinder with yellow bands, a red/black radiation trefoil disc on the top face, orange glowing windows on the side | #201f23, #85795d, #dbd378, red #492929 | 52 % | meta 108 | 64×64 · static · avg #736A4C · #303030 19%, #707070 7%, #505050 7% · opaque 51% |
| `textures/items/laserFocus.png` | 64×64 | Faceted ruby-red gem in a circular steel lens mount with a black ring, pale light rays radiating outward | #3c1a1a, #c8aeae, red #d83e3e, #fdfdfd | 70 % | meta 36 | 64×64 · static · avg #B47E7E · #F0F0F0 21%, #D0D0D0 6%, #501010 6% · opaque 70% |

## 5. Metals: ingots, nuggets, plates, ore

### 5a. Ingots and nuggets (vanilla silhouettes, 16×16)

Ingot silhouette: the vanilla ingot (trapezoid bar, 16 px wide, rows 2–13, coverage about 54 %). Nugget silhouette: the vanilla gold nugget (6×8 px, center, coverage 13 %).

| Path | Size | Color | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|
| `textures/items/ingotSteel.png` | 16×16 | dark steel: black outline, #656565 body, #cccccc highlight | Techguns [`ingotSteel`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) (config) | 16×16 · static · avg #565656 · #101010 31%, #707070 18%, #B0B0B0 13% · opaque 54% |
| `textures/items/ingotCopper.png` | 16×16 | golden copper: #bb9827, #dfc161, dark #4c3d0f | [`ingotCopper`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) (config) | 16×16 · static · avg #9B8232 · #F0D070 15%, #503010 13%, #907010 11% · opaque 54% |
| `textures/items/ingotTin.png` | 16×16 | pale teal: #73ada5, #bbd8d3, #2c4c47 | [`ingotTin`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) (config) | 16×16 · static · avg #6F9792 · #305050 23%, #70B0B0 14%, #509090 10% · opaque 54% |
| `textures/items/ingotBronze.png` | 16×16 | dark orange-brown: #683d03, #b3751f, near-black #1d0b00 | [`ingotBronze`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) (config) | 16×16 · static · avg #5E3707 · #503010 20%, #B07010 17%, #101010 16% · opaque 54% |
| `textures/items/ingotLead.png` | 16×16 | blue-gray: #435563, #758ea1, #1c232a | [`ingotLead`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) (config) | 16×16 · static · avg #40505C · #305050 16%, #303030 15%, #505070 15% · opaque 54% |
| `textures/items/ingotObsidianSteel.png` | 16×16 | near-black with dark violet highlight #484361 | meta 40 | 16×16 · static · avg #171620 · #101010 69%, #303050 18%, #101030 6% · opaque 51% |
| `textures/items/ingotTitanium.png` | 16×16 | bright silver-white: #adadad, #ececec, #424242 | meta 95 | 16×16 · static · avg #929292 · #505050 27%, #F0F0F0 24%, #B0B0B0 17% · opaque 51% |
| `textures/items/nuggetIron.png` | 16×16 | white/light gray: #efefef, #626262 | [`nuggetIron`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) (config) | 16×16 · static · avg #8F8F8F · #707070 38%, #F0F0F0 29%, #303030 20% · opaque 13% |
| `textures/items/copperNugget.png` | 16×16 | orange-gold: #9f6601, #fcbf54, highlight #ffefd1 | [`nuggetCopper`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) (config) | 16×16 · static · avg #BC934A · #907010 29%, #705010 20%, #F0F0B0 11% · opaque 13% |
| `textures/items/nuggetLead.png` | 16×16 | slate blue: #697b97, #343d4b | [`nuggetLead`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) (config) | 16×16 · static · avg #455164 · #303050 29%, #707090 29%, #303030 20% · opaque 13% |
| `textures/items/nuggetSteel.png` | 16×16 | black with gray highlight #b0b0b0 | [`nuggetSteel`](item-catalog.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) (config) | 16×16 · static · avg #4B4B4B · #101010 50%, #B0B0B0 29%, #909090 11% · opaque 13% |

### 5b. Plates, current style (32×32)

The same template for all: a square plate inset 3 px (rows/cols 3–29, coverage 66 %), a thick dark bevel on the right and bottom edges, a lighter top-left bevel, and a soft diagonal sheen across the face.

| Path | Face color | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|
| `textures/items/plateIron.png` | light gray #adadad / #c9c9c9, dark edge #363434 | meta 43 | 32×32 · static · avg #8C8B8B · #B0B0B0 35%, #D0D0D0 17%, #909090 14% · opaque 65% |
| `textures/items/plateSteel.png` | mid gray #929292 with a strong white diagonal shine #c7c7c7 | meta 45 | 32×32 · static · avg #777777 · #909090 21%, #707070 16%, #101010 14% · opaque 65% |
| `textures/items/plateTin.png` | pale cyan-gray #a6c3c5 / #c0d5d6 | meta 41 | 32×32 · static · avg #879C9D · #B0D0D0 38%, #303030 13%, #90B0B0 13% · opaque 65% |
| `textures/items/plateCopper.png` | warm pale gold #edc368 / #f1cb7b | meta 42 | 32×32 · static · avg #B19357 · #F0D070 23%, #F0D090 14%, #F0B050 8% · opaque 65% |
| `textures/items/plateBronze.png` | copper-orange #a05729 / #bc7945, very dark edge #1d0f07 | meta 44 | 32×32 · static · avg #774724 · #B07030 18%, #301010 13%, #B05030 13% · opaque 65% |
| `textures/items/plateLead.png` | periwinkle/lavender #6c60a2 / #8379b1 | meta 104 | 32×32 · static · avg #4C446F · #7070B0 33%, #101010 21%, #705090 9% · opaque 65% |
| `textures/items/plateObsidianSteel.png` | dark plum #543c5a / #66496d | meta 65 | 32×32 · static · avg #3A2A3E · #101010 33%, #503050 28%, #505070 13% · opaque 65% |
| `textures/items/plateTitanium.png` | white #f4f4f4 with high-contrast silver shine, black edge | meta 96 | 32×32 · static · avg #9C9C9C · #F0F0F0 29%, #D0D0D0 20%, #B0B0B0 15% · opaque 65% |
| `textures/items/carbonPlate.png` | dark gray #6a6a6a / #949494 with a checker-weave sheen | meta 62 | 32×32 · static · avg #575757 · #303030 25%, #505050 20%, #707070 20% · opaque 65% |
| `textures/items/plasticSheet.png` | off-white #dedede / #f5f5f5, soft gray edge | meta 72 | 32×32 · static · avg #DBDBDB · #F0F0F0 61%, #D0D0D0 23%, #909090 13% · opaque 65% |
| `textures/items/rubberBar.png` | flat very dark olive-brown #2e2918 square, almost no shading | meta 73 | 32×32 · static · avg #2A2516 · #303010 85%, #101010 13%, #301010 0% · opaque 65% |
| `textures/items/steamArmorPlate.png` | brownish-bronze #716142 with a black frame and four brass corner bolts | meta 67 | 32×32 · static · avg #50442E · #707050 26%, #101010 21%, #705030 13% · opaque 65% |
| `textures/items/powerPlating.png` | mottled composite (beige / gray / bluish speckle #948e86, #ada99f) with a dark frame | meta 89 | 32×32 · static · avg #78736E · #303030 14%, #909090 13%, #B0B0B0 12% · opaque 65% |

### 5c. Plates, old style (unused)

Older 32×32 plates (inset 2 px, coverage 77 %): a noisy, stone-like metal texture with eight dark "+" rivets (four corners plus four edge midpoints). **No code references them.**

| Path | Color | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|
| `textures/items/plateIronOLD.png` | mid gray #636363 / #7a7a7a | 32×32 · static · avg #686868 · #707070 61%, #505050 21%, #909090 12% · opaque 76% |
| `textures/items/plateSteelOLD.png` | dark gray #4b4b4b / #585858 | 32×32 · static · avg #4C4C4C · #505050 78%, #303030 11%, #707070 7% · opaque 76% |
| `textures/items/plateTinOLD.png` | blue-gray #717883 / #848c9a | 32×32 · static · avg #6A717B · #707070 32%, #707090 17%, #709090 10% · opaque 76% |
| `textures/items/plateCopperOLD.png` | orange-brown #6d4c17 / #88590f | 32×32 · static · avg #734D13 · #705010 43%, #905010 20%, #705030 9% · opaque 76% |
| `textures/items/plateBronzeOLD.png` | dull brown #563d16 / #694711 | 32×32 · static · avg #593E13 · #705010 33%, #503010 31%, #505010 15% · opaque 76% |

### 5d. Ore item

| Path | Size | Depicts | Palette | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|---|
| `textures/items/oreTitanium.png` | 32×32 | Irregular crumbly chunk of pale silver-white ore with dark specks, tilted | #646f70, #92a1a3, #bdcbcd, #eaf3f4 | meta 94 | 32×32 · static · avg #7E898A · #707070 10%, #505050 8%, #F0F0F0 8% · opaque 39% |

## 6. Materials and organics

| Path | Size | Depicts | Palette | Coverage | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|---|---|
| `textures/items/rawRubber.png` | 32×32 | Upright teardrop blob, olive/khaki, glossy highlight on the left | #959676, #51523e, #bdbea8 | 23 % | meta 70 | 32×32 · static · avg #747561 · #909070 14%, #D0D0B0 11%, #101010 11% · opaque 23% |
| `textures/items/rawPlastic.png` | 32×32 | Mound of white granular pellets | #b9b9b9, #d2d2d2, #ebebeb | 37 % | meta 71 | 32×32 · static · avg #C0C0C0 · #D0D0D0 37%, #F0F0F0 22%, #B0B0B0 19% · opaque 36% |
| `textures/items/heavyCloth.png` | 32×32 | Thick brown fabric/leather roll, tilted, with dark folds | #422f19, #5d4223, #997141 | 48 % | meta 39 | 32×32 · static · avg #543C1F · #303010 18%, #503010 15%, #705030 15% · opaque 47% |
| `textures/items/treatedLeather.png` | 32×32 | Flat tanned hide (animal-skin outline with four leg flaps) in very dark green | #1a2a12, #213417 | 52 % | meta 109 | 32×32 · static · avg #192912 · #103010 85%, #305010 8%, #101010 4% · opaque 51% |
| `textures/items/bioMass.png` | 64×64 | Lumpy, slimy bright-green blob with lighter highlights | #468d05, #5cb40b, #316b04 | 63 % | meta 38 | 64×64 · static · avg #448B06 · #307010 37%, #509010 34%, #50B010 12% · opaque 62% |
| `textures/items/carbonFibers.png` | 32×32 | Open honeycomb lattice of dark-gray hexagon outlines (graphene-like); transparent inside each cell | #4d4d4d, #3b3b3b, #6a6a6a | 29 % | meta 61 | 32×32 · static · avg #454545 · #505050 53%, #303030 35%, #101010 7% · opaque 28% |
| `textures/items/tgx.png` | 32×32 | Pale ice-blue crystalline lump with white highlights and dark-blue facets | #4f7393, #74b1dd, #9ad7f3, #d4eefc | 56 % | meta 60 | 32×32 · static · avg #668EAC · #D0F0F0 7%, #90D0F0 7%, #305070 7% · opaque 55% |
| `textures/items/quartz_rod.png` | 16×16 | Thin light-gray/white rod on the diagonal, 2 px wide | #b8b8b8, #d6d6d6 | 20 % | meta 99 | 16×16 · static · avg #C2C2C2 · #B0B0B0 54%, #D0D0D0 39%, #F0F0F0 5% · opaque 19% |
| `textures/items/yellowcake.png` | 16×16 | Small rounded pile of chartreuse/yellow-green powder | #a6b023, #dce656, #424512 | 45 % | meta 101 | 16×16 · static · avg #8E9625 · #B0B010 13%, #707010 9%, #303010 8% · opaque 45% |
| `textures/items/enrichedUranium.png` | 16×16 | Pale olive-green oval pellet with horizontal bands | #6d9055, #86a96e, #303f26 | 54 % | meta 102 | 16×16 · static · avg #5C7849 · #709050 28%, #305030 21%, #90B070 15% · opaque 53% |

## 7. Machine items

| Path | Size | Depicts | Palette | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|---|
| `textures/items/turretArmorIron.png` | 32×32 | Square armor plate (inset 2 px) with a riveted border (8 rivets per side) and a large bold black "T" embossed in the center; light polished gray | #a0a0a0, #bababa, #242424 | meta 48 | 32×32 · static · avg #7B7B7B · #B0B0B0 23%, #303030 16%, #D0D0D0 15% · opaque 76% |
| `textures/items/turretArmorSteel.png` | 32×32 | Same design, mid gray | #727272, #9f9f9f, #0f0f0f | meta 49 | 32×32 · static · avg #5A5A5A · #101010 26%, #303030 17%, #707070 17% · opaque 76% |
| `textures/items/turretArmorObsidianSteel.png` | 32×32 | Same design, gray-violet | #605868, #91889a | meta 50 | 32×32 · static · avg #4A444F · #101010 28%, #303030 12%, #705070 9% · opaque 76% |
| `textures/items/turretArmorCarbon.png` | 32×32 | Same design, dark charcoal | #3c3c3c, #646464, #0b0b0b | meta 66 | 32×32 · static · avg #333333 · #101010 34%, #303030 31%, #505050 20% · opaque 76% |
| `textures/items/oreDrillSmallSteel.png` | 32×32 | Small downward-pointing conical drill bit with spiral grooves, centered (16×17 px) | #575657, #777576, #c8c4c5 | meta 75 | 32×32 · static · avg #6C6A6B · #707070 25%, #303030 19%, #505050 17% · opaque 15% |
| `textures/items/oreDrillSmallObsidianSteel.png` | 32×32 | Same, dark violet-gray | #302932, #524756 | meta 76 | 32×32 · static · avg #2D272F · #303030 44%, #101010 27%, #505050 14% · opaque 15% |
| `textures/items/oreDrillSmallCarbon.png` | 32×32 | Same, black | #080808, #333333 | meta 77 | 32×32 · static · avg #202020 · #101010 57%, #303030 29%, #505050 12% · opaque 15% |
| `textures/items/oreDrillMediumSteel.png` | 32×32 | Larger downward cone (22×26 px), horizontal spiral bands | #646263, #878586, #d7d3d5 | meta 78 | 32×32 · static · avg #7B797B · #707070 20%, #505050 19%, #D0D0D0 16% · opaque 33% |
| `textures/items/oreDrillMediumObsidianSteel.png` | 32×32 | Same, steel-blue/dark slate | #3f5165, #2a3744 | meta 79 | 32×32 · static · avg #26323E · #303030 19%, #103030 15%, #101010 15% · opaque 33% |
| `textures/items/oreDrillMediumCarbon.png` | 32×32 | Same, black | #101010, #2a2a2a | meta 80 | 32×32 · static · avg #1B1B1B · #101010 69%, #303030 20%, #505050 8% · opaque 33% |
| `textures/items/oreDrillLargeSteel.png` | 32×32 | Big spiral cone lying on the diagonal, tip at the lower-left, base at the upper-right (fills 26×26) | #afafaf, #939293, #e3e3e3 | meta 81 | 32×32 · static · avg #9B9B9B · #B0B0B0 32%, #909090 19%, #D0D0D0 13% · opaque 33% |
| `textures/items/oreDrillLargeObsidianSteel.png` | 32×32 | Same, dark violet | #201c29, #332e42, #413a55 | meta 82 | 32×32 · static · avg #252130 · #303030 34%, #101010 23%, #303050 14% · opaque 33% |
| `textures/items/oreDrillLargeCarbon.png` | 32×32 | Same, black | #161616, #252525 | meta 83 | 32×32 · static · avg #1A1A1A · #101010 67%, #303030 32% · opaque 33% |
| `textures/items/machineStackUpgrade.png` | 32×32 | Green circuit card filling the frame: a yellow "+" at the top-left, a tall yellow up-arrow on the right, two small brown crates at the bottom-left | #21581d, #83960b, #123814 | meta 52 | 32×32 · static · avg #365113 · #103010 18%, #105010 17%, #101010 10% · opaque 76% |
| `textures/items/uvemitter.png` | 64×64 | Wide dark trapezoid lamp housing (top) over a glowing purple/violet lens; occupies the middle band only (rows 16–37) | #9275b7, #6c5091, #2d2d2d | meta 92 | 64×64 · static · avg #55456A · #303030 27%, #9070B0 25%, #101010 14% · opaque 21% |
| `textures/items/rcHeatRay.png` | 64×64 | Upright device: black housing with a gray frame, a round lens with a cross-grid glowing yellow-white in the upper half, and an orange glowing bulb at the bottom | #656562, #c7c2a9, #191918 | meta 98 | 64×64 · static · avg #605F57 · #303030 22%, #505050 20%, #101010 16% · opaque 58% |

## 8. Exotic, nuclear and anti-gravity

| Path | Size | Depicts | Palette | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|---|
| `textures/items/antiGravityCore.png` | 32×32 | Octagonal dark-gray disc with a glowing cyan/blue four-pointed star in the center | #1b8fc4, #25c2df, #424552 | meta 100 | 32×32 · static · avg #337D9E · #505050 19%, #1090D0 15%, #10B0D0 7% · opaque 43% |
| `textures/items/tacticalNuclearWarhead.png` | 32×32 | Flat lens-shaped gray casing, horizontal (rows 10–24), with a yellow-and-black radiation trefoil in the middle | #333333, #444444, #d3ec01 | meta 103 | 32×32 · static · avg #4F5427 · #303030 41%, #505050 23%, #101010 14% · opaque 34% |

## 9. Wearables and special-slot icons

| Path | Size | Depicts | Palette | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|---|
| `textures/items/glider.png` | 32×32 | Front view: brown backpack in the center with cream canvas wings (dark frame) spreading left and right | #784535, #33221d, #b7b49d | meta 51 Glider | 32×32 · static · avg #644C40 · #705030 19%, #303030 13%, #503030 10% · opaque 61% |
| `textures/items/gliderBackback.png` | 32×32 | Just the brown backpack with two straps | #37211b, #6e3f31, #794636 | meta 59 | 32×32 · static · avg #5D362A · #705030 31%, #503030 16%, #703030 15% · opaque 37% |
| `textures/items/gliderWing.png` | 32×32 | One cream canvas wing panel (trapezoid) with a dark gray spar frame and vertical ribs | #c0bda5, #2c2c2c, #858372 | meta 58 | 32×32 · static · avg #818071 · #303030 27%, #B0B090 18%, #B0B0B0 17% · opaque 59% |
| `textures/items/oxygenMask.png` | 16×16 | Horizontal gray breathing mask: ribbed center canister with a small valve on top, straps out to both sides | #747474, #888888, #adadad | meta 90 | 16×16 · static · avg #818181 · #707070 43%, #909090 32%, #B0B0B0 15% · opaque 33% |
| `textures/items/gasMaskFilter.png` | 32×32 | Round black filter disc, face-on, with six radial spokes | #272e27, #101310 | meta 74 | 32×32 · static · avg #181C18 · #101010 59%, #303030 31%, #103010 8% · opaque 60% |
| `textures/items/gasMask.png` | 32×32 | Front view of a dark green gas mask, two round eye lenses (transparent holes), round filter at the bottom | #3a443d, #26392b, #3a6043 | Gas Mask ([05](../05-armor/index.md)) | 32×32 · static · avg #2B3E2F · #305030 20%, #103010 17%, #505050 15% · opaque 41% |
| `textures/items/nightVisionGoggles.png` | 32×32 | Black head strap across the top, two round green lenses on a black housing below | #141613, #060606, green #017925 | Nightvision Goggles ([05](../05-armor/index.md)) | 32×32 · static · avg #0D2011 · #101010 75%, #107030 8%, #303030 6% · opaque 28% |
| `textures/items/tacticalMask.png` | 16×16 | Black full-face helmet/mask with a dark red visor band | #0e0e0e, red #591919 | Tactical Mask ([05](../05-armor/index.md)) | 16×16 · static · avg #1D1111 · #101010 81%, #701010 8%, #501010 7% · opaque 67% |
| `textures/items/scubaTanks.png` | 32×32 | Two gray cylinders side by side, joined by dark straps | #181818, #7d7d7d, #9d9d9d | Oxygen Tanks ([05](../05-armor/index.md)) | 32×32 · static · avg #606060 · #909090 21%, #101010 21%, #B0B0B0 15% · opaque 72% |
| `textures/items/jetpack.png` | 64×64 | Front view, middle band only (rows 16–47): two gray thruster cylinders with yellow/black hazard caps on top, joined by a dark central frame | #171512, #5e5c5e, #918f83 | Jetpack ([05](../05-armor/index.md)) | 64×64 · static · avg #4F4D4B · #505050 26%, #707070 21%, #101010 19% · opaque 30% |
| `textures/items/jumppack.png` | 32×32 | Two upright gray canisters with yellow/black striped tops on a dark frame with straps | #747273, #24201a, #989686 | Jump Pack ([05](../05-armor/index.md)) | 32×32 · static · avg #5F5D57 · #707070 28%, #909090 20%, #303030 16% · opaque 57% |
| `textures/items/antiGravPack.png` | 32×32 | Rectangular gray machined frame with dark side clamps and a glowing blue diamond in the center | #57626c, #9ea3a5, #4eabc2 | Anti Gravity Device ([05](../05-armor/index.md)) | 32×32 · static · avg #5C717B · #909090 15%, #707070 14%, #505050 13% · opaque 58% |

## 10. Armor item icons (owned by 05-armor, stored here)

All are 32×32 front-view inventory icons in the vanilla armor-icon layout: helmet in the upper area, chestplate as a torso, leggings as a pair of trousers, boots as a pair of boots. They are used as `techguns:<armor unlocalized name>` by [`GenericArmor`](../05-armor/armor-catalog.md#1-class-hierarchy) ([05](../05-armor/index.md)).

| Path | Depicts | Palette | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|
| `textures/items/t1_combat_helmet.png` | Soldier's helmet: gray-white dome with a brown chin strap | #bfc0b2, #3c362f | 32×32 · static · avg #8D8B80 · #B0B0B0 28%, #D0D0B0 19%, #505050 12% · opaque 35% |
| `textures/items/t1_combat_chestplate.png` | Brown leather/khaki vest with pouches | #352719, #4a3722, #58462f | 32×32 · static · avg #413221 · #303010 21%, #101010 14%, #503030 13% · opaque 62% |
| `textures/items/t1_combat_leggings.png` | Brown trousers with side pockets | #2b1f15, #513e28 | 32×32 · static · avg #493826 · #303010 29%, #503030 24%, #505030 20% · opaque 46% |
| `textures/items/t1_combat_boots.png` | Black lace-up boots | #26231f, #5d5955 | 32×32 · static · avg #2A2724 · #101010 49%, #303030 24%, #303010 7% · opaque 33% |
| `textures/items/t1_miner_helmet.png` | Yellow hard hat with a white headlamp | #e7db04, #5c5613 | 32×32 · static · avg #BCB312 · #F0D010 18%, #D0D010 18%, #F0F010 17% · opaque 27% |
| `textures/items/t1_miner_chestplate.png` | Blue dungaree bib | #312f6a, #413f87 | 32×32 · static · avg #383672 · #303070 34%, #303090 18%, #505090 15% · opaque 44% |
| `textures/items/t1_miner_leggings.png` | Blue work trousers | #343273, #3c3a81 | 32×32 · static · avg #363473 · #303070 38%, #303090 18%, #505090 17% · opaque 45% |
| `textures/items/t1_miner_boots.png` | Dark boots with yellow cuffs and gray toes | #2f2f2e, #929075 | 32×32 · static · avg #464438 · #303030 54%, #909090 14%, #101010 10% · opaque 46% |
| `textures/items/t1_scout_helmet.png` | Tan hood/bandana with a dark face cloth | #695e4d, #8c7f64 | 32×32 · static · avg #6C624F · #707050 24%, #909070 12%, #907050 11% · opaque 44% |
| `textures/items/t1_scout_chestplate.png` | Ragged tan/brown bandit vest | #3c2d19, #7f7256 | 32×32 · static · avg #52452F · #303010 26%, #707050 13%, #503010 10% · opaque 56% |
| `textures/items/t1_scout_leggings.png` | Tan ragged trousers | #74674b, #827557 | 32×32 · static · avg #72674D · #907050 28%, #707050 27%, #909070 14% · opaque 43% |
| `textures/items/t1_scout_boots.png` | Dark brown worn boots | #352c26, #463d37 | 32×32 · static · avg #393029 · #303030 38%, #503030 22%, #301010 8% · opaque 35% |
| `textures/items/t2_beret.png` | Dark red beret, side view | #520000, #300000 | 32×32 · static · avg #3D0000 · #501010 33%, #101010 23%, #301010 23% · opaque 22% |
| `textures/items/t2_combat_helmet.png` | Olive-gray combat helmet with dark goggles | #596054, #666f62 | 32×32 · static · avg #484D45 · #303030 22%, #707070 21%, #505050 20% · opaque 38% |
| `textures/items/t2_combat_chestplate.png` | Dark olive plate carrier | #191c16, #282c24 | 32×32 · static · avg #262922 · #101010 40%, #303030 20%, #505050 10% · opaque 63% |
| `textures/items/t2_combat_leggings.png` | Gray-olive cargo trousers | #3c3c38, #4d4e45 | 32×32 · static · avg #454640 · #303030 36%, #505050 36%, #707050 5% · opaque 47% |
| `textures/items/t2_combat_boots.png` | Dark gray combat boots | #1f1c19, #454341 | 32×32 · static · avg #32302D · #101010 34%, #303030 27%, #505050 12% · opaque 35% |
| `textures/items/t2_commando_helmet.png` | Black balaclava/helmet with a gray goggle visor and respirator | #2c2c2c, #808080 | 32×32 · static · avg #303030 · #101010 39%, #303030 36%, #505050 9% · opaque 49% |
| `textures/items/t2_commando_chestplate.png` | Black tactical vest | #131313, #383838 | 32×32 · static · avg #141414 · #101010 80%, #303030 16%, #505050 2% · opaque 70% |
| `textures/items/t2_commando_leggings.png` | Black trousers | #1b1b1b, #323232 | 32×32 · static · avg #191919 · #101010 66%, #303030 29%, #707070 2% · opaque 47% |
| `textures/items/t2_commando_boots.png` | Black boots | #242424, #0a0a0a | 32×32 · static · avg #141414 · #101010 77%, #303030 22% · opaque 40% |
| `textures/items/t2_riot_helmet.png` | Olive drab helmet (Ranger Veteran) | #4b4c39, #606248 | 32×32 · static · avg #3E3F30 · #101010 28%, #303030 17%, #707050 17% · opaque 38% |
| `textures/items/t2_riot_chestplate.png` | Brown duster coat with a green bandolier of brass rounds | #503a1f, #382d19 | 32×32 · static · avg #40341F · #503010 42%, #303010 12%, #301010 7% · opaque 70% |
| `textures/items/t2_riot_leggings.png` | Olive drab trousers | #3e3a25, #545238 | 32×32 · static · avg #45422B · #505030 41%, #303030 26%, #303010 13% · opaque 47% |
| `textures/items/t2_riot_boots.png` | Dark olive boots | #302f24, #41402f | 32×32 · static · avg #2B2B20 · #101010 38%, #303030 28%, #505030 11% · opaque 35% |
| `textures/items/t3_combat_helmet.png` | White armored helmet with a blue visor | #9db0bd, #2c476b, #f8f9fa | 32×32 · static · avg #6F7F92 · #F0F0F0 14%, #101030 11%, #B0D0D0 6% · opaque 45% |
| `textures/items/t3_combat_chestplate.png` | White segmented chest armor on a dark undersuit, small red emblem | #192025, #dbe2e7 | 32×32 · static · avg #7D868D · #F0F0F0 16%, #103030 15%, #303030 13% · opaque 67% |
| `textures/items/t3_combat_leggings.png` | Dark navy leggings with white knee pads | #252f37, #91a3b1 | 32×32 · static · avg #303A42 · #303030 38%, #103030 23%, #101030 9% · opaque 46% |
| `textures/items/t3_combat_boots.png` | White armored boots on a dark base | #12171b, #cfd8df | 32×32 · static · avg #717E88 · #101010 10%, #303030 9%, #D0F0F0 9% · opaque 38% |
| `textures/items/t3_exo_helmet.png` | Like the t3 combat helmet: white/blue visor with gray trim | #2e4466, #d4dce2 | 32×32 · static · avg #6E7B8D · #F0F0F0 14%, #101030 11%, #103070 5% · opaque 45% |
| `textures/items/t3_exo_chestplate.png` | White chest plates on a gray exo frame, red emblem | #1e252b, #dbe2e7, #7b7e88 | 32×32 · static · avg #8F959C · #F0F0F0 16%, #103030 9%, #303030 9% · opaque 67% |
| `textures/items/t3_exo_leggings.png` | Dark leggings with gray exoskeleton struts | #27323a, #a4a0a4 | 32×32 · static · avg #5D6166 · #303030 29%, #B0B0B0 13%, #909090 11% · opaque 50% |
| `textures/items/t3_exo_boots.png` | Gray/white exo boots | #afacae, #dde3e7 | 32×32 · static · avg #87888C · #B0B0B0 13%, #D0D0D0 10%, #909090 8% · opaque 42% |
| `textures/items/t3_miner_helmet.png` | Black HEV-style helmet with a blue-gray visor and an orange band | #0f0f0f, #305875 | 32×32 · static · avg #282925 · #101010 56%, #305070 13%, #303030 5% · opaque 32% |
| `textures/items/t3_miner_chestplate.png` | Black/orange HEV chest with ribbed plating | #0f0f0f, #d18823 | 32×32 · static · avg #372915 · #101010 62%, #303030 10%, #503030 4% · opaque 54% |
| `textures/items/t3_miner_leggings.png` | Black leggings with orange panels | #000000, #bd7819 | 32×32 · static · avg #443014 · #101010 49%, #B07010 11%, #505050 6% · opaque 43% |
| `textures/items/t3_miner_boots.png` | Black boots with orange stripes | #000000, #996215 | 32×32 · static · avg #2E2210 · #101010 63%, #303030 9%, #705010 8% · opaque 28% |

## 11. Tools, placers, door, bucket, debug items

| Path | Size | Depicts | Palette | Used by | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|---|
| `textures/items/combatKnife.png` | 96×96 | Dark gunmetal combat knife on the diagonal (blade upper-right, grip lower-left, small cross-guard). It occupies only about 60×58 px of the canvas (coverage 7 %), so it is drawn smaller than other icons | #2f3332, #373b39, #3e4c46 | Combat Knife | 96×96 · static · avg #2F3532 · #303030 57%, #505050 13%, #101010 9% · opaque 7% |
| `textures/items/crowbar.png` | 32×32 | Thin dark-red/maroon crowbar on the diagonal, curved hook at the top-right, flat claw at the bottom-left | #632f2f, #ad5757, #462626 | Crowbar | 32×32 · static · avg #713B3B · #703030 25%, #503030 24%, #905050 15% · opaque 10% |
| `textures/items/camoNetPlacer0.png` | 32×32 | Two olive wooden poles on the diagonal with a frayed net between them, woodland green camo pattern, small transparent holes | #6a784b, #567e55, #3c3e26 | Camo Net Placer meta 0 | 32×32 · static · avg #586A43 · #707050 23%, #505030 19%, #507050 15% · opaque 38% |
| `textures/items/camoNetPlacer1.png` | 32×32 | Same, snow camo: white/gray net | #686845 poles, #9a9a9a, #b8b8b8 | meta 1 | 32×32 · static · avg #72725F · #707050 23%, #909090 19%, #505030 17% · opaque 38% |
| `textures/items/camoNetPlacer2.png` | 32×32 | Same, desert camo: sand-yellow net | #6d6e40, #abb25b, #c0c671 | meta 2 | 32×32 · static · avg #7E8147 · #707050 23%, #505030 18%, #B0B050 18% · opaque 38% |
| `textures/items/metalDoor.png` | 32×32 | Gray-green bunker door, front view: rounded top corners, rivets along the frame, a vertical handle bar on the right | #666c63, #798075, #a6aca0 | Bunker Door item | 32×32 · static · avg #6F756B · #707070 30%, #909090 16%, #505050 12% · opaque 73% |
| `textures/items/bucket_acid.png` | 16×16 | Vanilla bucket silhouette filled with murky ochre/brown-yellow liquid | #dfdfdf, #9f9f9f, liquid #866918 / #66562c | Creeper Acid Bucket | 16×16 · static · avg #7E755B · #303030 25%, #907010 20%, #D0D0D0 14% · opaque 50% |
| `textures/items/oreClusterScanner.png` | 32×32 | Gray handheld scanner with two antennae; green grid screen at the left, small red readout and keypad at the right | #444444, #545654, green #009000 | Ore Cluster Scanner | 32×32 · static · avg #3E553C · #505050 41%, #303030 15%, #707070 12% · opaque 48% |
| `textures/items/blockReaderTool.png` | 32×32 | Magnifying glass (white lens rim, gray handle to the lower-right) over a gray stone cube | #6f7073, #b9b9b9, #090909 | Block Reader Tool | 32×32 · static · avg #5E5E5F · #101010 20%, #707070 17%, #505050 14% · opaque 62% |
| `textures/items/structureScanTool.png` | 32×32 | Magnifying glass over a dark-red brick wall | #000000, #a2a2a2, #421616 | Building Scan Tool | 32×32 · static · avg #5A4C4C · #101010 36%, #909090 11%, #F0F0F0 7% · opaque 50% |
| `textures/items/structureTestTool.png` | 32×32 | Spanner and hammer crossed over a dark-red brick wall | #030303, #471e1e, #818181 | Structure Test Tool | 32×32 · static · avg #534444 · #101010 29%, #303030 12%, #909090 9% · opaque 62% |
| `textures/items/testItem.png` | 32×32 | Placeholder: the letters "LEL" scrawled in black | #000000 | Ore Cluster Debugger; [`GenericItem`](special-items.md#base-classes) default | 32×32 · static · avg #000000 · #101010 100% · opaque 28% |
| `textures/items/spawn_egg.png` | 32×32 | Speckled egg in **grayscale** (it is tinted at runtime with the egg's base color) | #797979, #a1a1a1, #d4d4d4 | all spawn eggs, render pass 0 | 32×32 · static · avg #898989 · #B0B0B0 23%, #909090 21%, #707070 14% · opaque 50% |
| `textures/items/spawn_egg_overlay.png` | 32×32 | The letters "TG" in white with a dark gray outline, centered (rows 11–25), tinted at runtime with the spots color | #4f4f4f, #ffffff | all spawn eggs, render pass 1 | 32×32 · static · avg #939393 · #505050 55%, #F0F0F0 35%, #909090 8% · opaque 16% |

## 12. Unused / unregistered

| Path | Size | Depicts | Status | Measured (ASSET-FACTS): size · animation · average · dominant · opaque |
|---|---|---|---|---|
| `textures/items/handgun.png` | 32×32 | Old gray pistol/hand-cannon barrel on the diagonal with a brown grip at the lower-right (#36322b, #595959, #8b8c8c) | No code reference (guns use 3D models and textures under `textures/guns/`) | 32×32 · static · avg #4A4946 · #303030 25%, #707070 21%, #505050 21% · opaque 35% |
| `textures/items/medkit_0.png` | 32×32 | Roll of white gauze bandage with an unrolled strip (#d6d6d6, #898989) | Only `ItemMedkit`, which is not registered | 32×32 · static · avg #C2C2C2 · #D0D0D0 44%, #F0F0F0 23%, #909090 12% · opaque 44% |
| `textures/items/medkit_1.png` | 32×32 | Red first-aid pack, its white panel showing a red heart (#aa3636, #b8b8b8, #7d2828) | Only `ItemMedkit`, which is not registered | 32×32 · static · avg #934D4B · #B03030 30%, #505030 17%, #903030 12% · opaque 50% |
| `textures/items/plate*OLD.png` (5 files) | 32×32 | see §5c | unused | see the individual rows in §5c |

## Coverage check

Each of the 177 PNG files in the folder appears exactly once above by its full path. (The `plate*OLD.png` row in §12 only points back to §5c.) Breakdown: ammo/mags 16, tanks/cells 11, gun parts 13, components 13, ingots/nuggets 11, plates 13 + 5 old, ore 1, materials 10, machine items 16, exotic 2, wearable icons 12, armor icons 37, tools/placers/debug 14, unused 3.
