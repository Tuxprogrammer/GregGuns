# Armour textures (`textures/armor/`, 82 files)

There are **no `.mcmeta` files** in this folder, so none of these textures are animated. "Palette" lists the dominant colours, measured by bucketing the opaque pixels. All files are RGBA PNGs with transparent unused areas.

## 0. Layout conventions

**A. Vanilla armour layers at 2× (128×64).** These are used by the default biped armour model (model index −1), with the vanilla 64×32 UV grid doubled. Pixel regions:

| Region (px) | Vanilla part |
|---|---|
| (0,0)–(64,32) | head box (helmet): top and bottom faces in row 0–16; sides, front and back in row 16–32 |
| (64,0)–(128,32) | headwear/hat overlay box (helmet outer shell). It is mostly empty here, and some sets put straps or a visor in it. |
| (0,32)–(32,64) | leg box (boots in layer `_1`, trousers in layer `_2`) |
| (32,32)–(80,64) | body box (chest in `_1`, belt or waist in `_2`) |
| (80,32)–(112,64) | arm box (sleeves, chest layer only) |

`<set>_1.png` is drawn for helmet, chest and boots; `<set>_2.png` for leggings. In `_2` only the leg box and the lower body box are painted.

**B. 2× biped plus extras (128×128).** Used by `ModelExoSuit` and [`ModelArmorCoat`](rendering-and-models.md#24-modelarmorcoattype-scale-riot--ranger-veteran-indices-811), whose UV grid is 64×64.
- The top half (rows 0–64 px) follows layout A.
- The bottom half (rows 64–128 px) holds the extra boxes. Their UV offsets are given in rendering-and-models.md; multiply them by 2 to get pixels.

**C. Custom-model atlases (256×128 = 2× of 128×64).** Used by `ModelSteamArmor` and `ModelT3PowerArmor`. There are no vanilla boxes; every region is one part's box unwrap at the UV offsets in rendering-and-models.md §2.1–2.2 (×2).

**D. Small attachment models (64×64 = 2× of 32×32).** Used by the face, back and beret models. Box unwraps are at the UV offsets in rendering-and-models.md §2.5–2.7 (×2). The head hood box (0,0)–(64,32) px is the standard 8×8×8 head unwrap.

## 1. T1 armour (layout A, 128×64)

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/armor/t1_combat_1.png` | 128×64 | WWII-style soldier. **Head:** cream/off-white cloth-covered helmet, with thin brown chin straps hanging from the hat overlay. **Body:** brown leather vest with crossing straps and two front pouches. **Arms:** brown sleeves with a tiny red-white-blue flag patch on the right shoulder. **Legs (boots):** charcoal-black boots. | cream #d0d0c0, brown #503010–#705030, black #101010 | T1 Combat helmet, chest, boots |
| `textures/armor/t1_combat_2.png` | 128×64 | Brown canvas trousers with knee seams; a dark belt band on the lower body box. | browns #503010, #705030, olive-brown #505030 | T1 Combat leggings |
| `textures/armor/t1_scout_1.png` | 128×64 | "Bandit" outfit. Khaki/sand head wrap (helmet) with a dark grey bandana band around the lower face. Khaki body with a crossed rag sash and straps; khaki arm wraps with dark bands; dark grey-brown boots. | khaki #707050, tan #907050, dark #303030 | Bandit pieces, camo 0 |
| `textures/armor/t1_scout_forest_1.png` | 128×64 | Same layout, recoloured to forest greens. | greens #103010, #105010, #305010 | camo 1 |
| `textures/armor/t1_scout_snow_1.png` | 128×64 | Same layout in white/light grey. | greys #909090, #707070, #b0b0b0 | camo 2 |
| `textures/armor/t1_scout_black_1.png` | 128×64 | Same layout in near-black. | #101010, #303030 | camo 3 |
| `textures/armor/t1_scout_2.png` | 128×64 | Khaki trousers with dark horizontal wrap bands and a sand belt. | #707050, #907050 | Bandit leggings, camo 0 |
| `textures/armor/t1_scout_forest_2.png` | 128×64 | Olive/forest trousers with dark bands. | #303010, #305010, #505010 | camo 1 |
| `textures/armor/t1_scout_snow_2.png` | 128×64 | White/grey trousers. | #909090, #707070 | camo 2 |
| `textures/armor/t1_scout_black_2.png` | 128×64 | Black trousers. | #101010 | camo 3 |
| `textures/armor/t1_miner_1.png` | 128×64 | Miner. **Head:** glossy **yellow** hard hat with a brim (hat-overlay band) and a small white **headlamp** at front-centre. **Body:** **blue** bib overalls (shoulder straps, a square bib pocket) over a dark shirt. **Arms:** grey shirt sleeves. **Boots:** dark grey work boots with olive tops. A small dark square badge (pocket) appears in the arm and leg areas. | yellow #f0d010, blue #303070, grey #303030 | Miner pieces, camo 0 (helmet "Yellow", body "Blue") |
| `textures/armor/t1_miner_red_1.png` | 128×64 | Same, with a **lime** hard hat and **red** overalls. | lime #50f010, reds #701010–#901010 | camo 1 (helmet "Lime", body "Red") |
| `textures/armor/t1_miner_green_1.png` | 128×64 | Same, with a **cyan/light-blue** hard hat and **dark green** overalls. | cyan #10d0f0, greens #305030, #103010 | camo 2 ("Light Blue" / "Green") |
| `textures/armor/t1_miner_black_1.png` | 128×64 | Same, with an **orange** hard hat and **black** overalls. | orange #f07010, #f05010, black #303030 | camo 3 ("Orange" / "Black") |
| `textures/armor/t1_miner_2.png` | 128×64 | Blue overall trousers with a knee patch and a lighter blue cuff/belt line. | blues #303070, #505090 | Miner leggings camo 0 |
| `textures/armor/t1_miner_red_2.png` | 128×64 | Red overall trousers. | #901010, #701010 | camo 1 |
| `textures/armor/t1_miner_green_2.png` | 128×64 | Green overall trousers. | #305030, #103010 | camo 2 |
| `textures/armor/t1_miner_black_2.png` | 128×64 | Black overall trousers. | #303030, #101010 | camo 3 |

## 2. T2 armour (layout A, 128×64)

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/armor/t2_combat_1.png` | 128×64 | Modern combat kit, **urban grey**. **Helmet:** grey patterned combat helmet with a dark goggle band and chin straps. **Body:** grey camo shirt under a black plate-carrier vest with magazine pouches. **Arms:** grey sleeves with a small round flag patch. **Boots:** black. | greys #505050, #303030, #707070, black #101010 | T2 Combat pieces, camo 0 "Urban" |
| `textures/armor/t2_combat_wood_1.png` | 128×64 | Same, woodland camo (green/brown/black blotches) with a black vest. | #305030, #507050, #103010 | camo 1 "Woodland" |
| `textures/armor/t2_combat_desert_1.png` | 128×64 | Same, desert tan/pink camo with a dark brown vest. | #b09090, #909070, #301010 | camo 2 "Desert" |
| `textures/armor/t2_combat_arctic_1.png` | 128×64 | Same, white/grey arctic splinter camo with a black vest. | #909090, #505050, #303030 | camo 3 "Arctic" |
| `textures/armor/t2_combat_swat_1.png` | 128×64 | Same, all black; white "SWAT" lettering on the back of the vest and a small badge on the front. | #101010, #303030 | camo 4 "SWAT" |
| `textures/armor/t2_combat_security_1.png` | 128×64 | Dark grey helmet and vest with a **navy blue** shirt and sleeves. | #101010, #303030, navy #101070 | camo 5 "Security" |
| `textures/armor/t2_combat_2.png` | 128×64 | Grey camo trousers with thigh pockets and knee pads; a black belt. | #505050, #507050 | T2 Combat leggings, camo 0 |
| `textures/armor/t2_combat_wood_2.png` | 128×64 | Woodland camo trousers. | #305030, #505030, #307030 | camo 1 |
| `textures/armor/t2_combat_desert_2.png` | 128×64 | Desert camo trousers. | #909070, #b09070 | camo 2 |
| `textures/armor/t2_combat_arctic_2.png` | 128×64 | Arctic camo trousers. | #505050, #707070, #909090 | camo 3 |
| `textures/armor/t2_combat_swat_2.png` | 128×64 | Black trousers. | #101010 | camo 4 |
| `textures/armor/t2_combat_security_2.png` | 128×64 | Navy trousers. | #101070, #101050 | camo 5 |
| `textures/armor/t2_commando_1.png` | 128×64 | All-black special-forces kit. **Head:** a black balaclava/hood with grey **diving goggles** at eye level and a grey rebreather mouthpiece. **Body:** black wetsuit-style vest with harness straps and pouches. **Arms:** black sleeves with grey cuff bands. **Boots:** black. | #101010, #303030, a few greys #909090 | T2 Commando helmet, chest, boots |
| `textures/armor/t2_commando_2.png` | 128×64 | Black trousers with a dark grey belt. | #101010, #303030 | T2 Commando leggings |

## 3. T3 armour and HEV (layout A, 128×64)

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/armor/t3_combat_1.png` | 128×64 | Sci-fi trooper armour (Halo/Mass-Effect style). **Helmet:** white armour shell with a **blue visor** band across the eyes and a black chin strap. **Body:** white chest plates with red accent marks over a dark navy undersuit. **Arms:** white shoulder plates over navy sleeves. **Boots:** white plating with a dark band. | white #f0f0f0, #d0f0f0, navy #101030, #303030, red accents | T3 Combat pieces, camo 0 "White" |
| `textures/armor/t3_combat_silver_1.png` | 128×64 | Same, with silver/grey plates and an **amber visor**. | #d0d0d0, #b0b0b0, #101010 | camo 1 "Silver" |
| `textures/armor/t3_combat_green_1.png` | 128×64 | Same, with dark olive plates, a black undersuit and white unit numbers on the chest. | #101010, #103010, #303010 | camo 2 "Dark Green" |
| `textures/armor/t3_combat_2.png` | 128×64 | Navy undersuit trousers with a pale glowing knee/thigh panel. | #303030, #101030, #103030 | T3 Combat leggings camo 0 |
| `textures/armor/t3_combat_silver_2.png` | 128×64 | Black undersuit trousers with a red-white panel. | #101010, #303030 | camo 1 |
| `textures/armor/t3_combat_green_2.png` | 128×64 | Black/olive trousers. | #101010, #103010 | camo 2 |
| `textures/armor/hevsuit_1.png` | 128×64 | Half-Life-style **HEV suit**, orange. **Helmet:** black head with an orange collar ring and a **blue visor** strip at the hat-overlay level. **Body:** black suit with the orange **Lambda-style chest plate** (front) and an orange ladder/backplate. **Arms:** orange-banded sleeves with black joints. **Boots:** black. | black #101010, #303030, orange #f09010, #b07010 | HEV pieces, camo 0 "Orange" |
| `textures/armor/hevsuit_silver_1.png` | 128×64 | Same design with white/silver plates. | #101010, #303030, #b0b0b0, #d0d0d0 | camo 1 "Silver" |
| `textures/armor/hevsuit_black_1.png` | 128×64 | Same, all black with dark red accents and a red visor glow. | #101010, #303030, #501010, #b01010 | camo 2 "Black" |
| `textures/armor/hevsuit_2.png` | 128×64 | Orange armoured trousers with black joint bands. | #b07010, #905010, #d09010 | HEV leggings camo 0 |
| `textures/armor/hevsuit_silver_2.png` | 128×64 | Silver trousers with black bands. | #b0b0b0, #505050 | camo 1 |
| `textures/armor/hevsuit_black_2.png` | 128×64 | Black trousers. | #101010 | camo 2 |

## 4. Riot / Ranger Veteran (layout B, 128×128, `ModelArmorCoat`)

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/armor/t2_riotArmor_1.png` | 128×128 | **Top half** (layout A): an olive/khaki combat helmet with a gas-mask-style dark face area; a body of olive-green shirt with a bandolier of brass shells and a dark brown duster coat on the back; olive sleeves; brown boots. **Bottom half:** khaki **shoulder pad** unwraps at (0,68) px; the brown leather **coat back panel** at (88,70) px; the **coat front panels** at (100,94) px; the brown **coat-skirt** leg unwraps at (0,96) px. The coat parts include belt-buckle and pocket details. | brown #503010, olive #505030, #707050, dark #303010 | Riot helmet, jacket, boots (models 8, 9, 11) |
| `textures/armor/t2_riotArmor_2.png` | 128×128 | Leggings layer: olive trousers on the leg box; a brown coat waist on the body box; the same bottom-half coat parts, of which the coat skirts (0,96) px are the ones used by model 10. | #503010, #505030, #303010 | Riot pants (model 10) |
| `textures/armor/t2_riotArmor.png` | 128×128 | **Unused leftover.** The top half is a copy of the T3 Combat white/blue-visor layout; the bottom half has sand-coloured patches. | #b0b090, #303030, #f0f0f0 | nothing (the code only appends `_1`/`_2`) |

## 5. Exo Suit (layout B, 128×128, `ModelExoSuit`)

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/armor/t3_exo_1.png` | 128×128 | **Top half:** the same white/blue-visor trooper armour as `t3_combat_1` (helmet, chest, arms, boots). **Bottom half:** light grey **exoskeleton frame** parts: shoulder blocks (0,64) px; forearm cuffs (48,64); arm and leg rods (96,64) and (112,64) px; joints with dark bolt dots (54,82); the spine and cross-bar (76,80) and (0,120); belt and centre strip (0,86) and (0,104); thigh cuffs (34,104); soles and ankle plates (80,112) and (112,102). Rendered with white highlights and darker grey rims. | #909090, #f0f0f0, #b0b0b0, #707070 | Exo helmet, chest, boots |
| `textures/armor/t3_exo_2.png` | 128×128 | Leggings layer: navy undersuit trousers (as `t3_combat_2`) in the top half, plus the same frame atlas. | #909090, #b0b0b0, #707070 | Exo leggings |
| `textures/armor/t3_exo_silver_1.png` | 128×128 | Silver plates, amber visor, mid-grey frame. | #707070, #909090, #d0d0d0 | camo 1 |
| `textures/armor/t3_exo_silver_2.png` | 128×128 | Silver leggings layer. | #707070, #505050 | camo 1 |
| `textures/armor/t3_exo_green_1.png` | 128×128 | Dark olive plates with unit numbers; a **black** frame. | #101010, #103010 | camo 2 |
| `textures/armor/t3_exo_green_2.png` | 128×128 | Dark green leggings layer with a black frame. | #101010 | camo 2 |

## 6. Steam Armour (layout C, 256×128, `ModelSteamArmor`)

The atlas is fully opaque: unused areas are solid black (#101010), not transparent.

| Region (px) | Content |
|---|---|
| (0,0)–(72,36) | helmet shell (9×9×9 unwrap) |
| (72,0)–(100,24) | ear plates |
| (100,0)–(136,18) | front plates |
| (40,36)–(96,70) | torso |
| (96,36)–(136,74) | arm sleeves |
| (100,72)–(140,94) | back plate |
| (40,94)–(96,110) | hip block |
| (0,94)–(40,122) | thigh shells (leggings) |
| (0,36)–(40,54) | boot shells |
| (142,64)–(250,128) | piston, ring, belt and sole parts, painted brass/yellow |
| (190,0)–(256,52) | small fittings in grey: exhaust stacks, piston joints, back boxes, pipe caps and rivets; the two back canisters carry blue gauge glass |

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/armor/steam_armor.png` | 256×128 | Steampunk plate armour: **polished tan/beige brass-steel plates** with recessed panels; **bright yellow-brass** pistons and rings; grey goggle-like faceplate details; blue glass gauges. | black bg, tan #b09070, #907050, yellow #f0f070, #f0d050 | Steam pieces, camo 0 "Default"; also the 3-D inventory icon |
| `textures/armor/steam_armor_rusty.png` | 256×128 | Same layout; the plates are mottled **rusty** orange-tan, and the pistons are **dark rust-red** with speckles. | #703010, #d09050, #b07030 | camo 1 "Rusty" |
| `textures/armor/steam_armor_iron.png` | 256×128 | Same layout; **grey iron/steel** plates and dark grey pistons. | #707070, #909090, #505050 | camo 2 "Steel" |
| `textures/armor/steam_armor_hazard.png` | 256×128 | Same layout; plates painted with **yellow/black diagonal hazard stripes**, yellow pistons, and a red faceplate. | yellow #d0d010, #b0b010, black, red | camo 3 "Hazard" |
| `textures/armor/steam_armor_1.png` | 256×128 | **Unused.** A darker bronze/brown variant of the same atlas with orange pistons. | #503010, #b07010, #703010 | nothing references it |

## 7. Power Armour (layout C, 256×128, `ModelT3PowerArmor`)

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/armor/powerarmor.png` | 256×128 | "Paladin" power armour. **Light grey/steel plating** with soft shading and dark recessed seams: the large helmet with dark eye slots and a snout grille, rounded pauldrons, a stepped chest, boots. **Green/black and yellow/black striped hoses and cables** (around the snout and the knee struts), and two black visor holes. | #909090, #707070, #b0b0b0, #505050 | Power pieces, camo 0 "Paladin"; also the 3-D inventory icon |
| `textures/armor/powerarmor_dark.png` | 256×128 | "Outcast": the same atlas in **black/dark grey** plating with **red** trim and red eye glow. | #101010, #303030, #501010, red #f01010 | camo 1 "Outcast" |
| `textures/armor/powerarmor_dark2.png` | 256×128 | "Outcast 2": black plating with **red scratched/scrawled markings** and white details. | #101010, #303030, #301010 | camo 2 "Outcast 2" |

## 8. Beret (layout D, 64×64, `ModelBeret`)

The crown unwrap is at (0,0)–(56,20) px and the side flap at (0,22)–(44,42) px.

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| `textures/armor/beret_texture.png` | 64×64 | Red felt beret, darker toward one side, with a black band edge. | #701010, #901010, #501010 | Beret camo 0 "Red" |
| `textures/armor/beret_texture_black.png` | 64×64 | Same, black/charcoal. | #303030, #505050 | camo 1 "Black" |
| `textures/armor/beret_texture_green.png` | 64×64 | Same, dark green. | #103010, #105010 | camo 2 "Green" |

## 9. Face gear (layout D, 64×64)

| Path | Size | Depicts | Layout | Palette | Used by |
|---|---|---|---|---|---|
| `textures/armor/gasMask.png` | 64×64 | A full **dark olive rubber hood** covering the whole head, with two round **light-grey lenses** on the front face. The filter mount, mouth piece and filter canister unwraps are in dark grey/olive. | hood (0,0)–(64,32); mount (0,34)–(16,42); mouth piece (18,34)–(38,42); canister (40,34)–(56,42) | #303030, #103010 | Gas Mask on the player |
| `textures/armor/oxygenMask.png` | 64×64 | Mostly transparent. A **thin grey strap** runs around the head at mouth level, with a small valve nub at the front; a grey **breathing mask** block has a dark slotted grille. | strap across the head-side row y≈28–30 px; mask block (18,34)–(38,42) | #707070, #909090 | Oxygen Mask |
| `textures/armor/tacticalMask.png` | 64×64 | A black **balaclava** hood with a **red-tinted goggle band** across the eyes, and a black respirator mouth piece. | hood (0,0)–(64,32); mouth piece (18,34)–(38,42) | #101010, #303030, red #701010 | Tactical Mask camo 0 |
| `textures/armor/tacticalMask_green.png` | 64×64 | Same, very dark green hood. | same | #101010, #103010 | camo 1 |
| `textures/armor/tacticalMask_white.png` | 64×64 | Same, white/light-grey hood with a **blue goggle band**. | same | #b0b0b0, #909090 | camo 2 |
| `textures/armor/tacticalmask_lightgreen.png` | 64×64 | Same, olive/light-green hood with a red goggle band. Code requests `tacticalMask_lightgreen.png` (case mismatch). | same | #303030, #303010, #103010 | camo 3 |
| `textures/armor/nightVisionGoggles.png` | 64×64 | Mostly transparent. A **black head strap** (one band over the top, one around the head), and black lens tubes with a **green** and a **blue** lens pixel at their front faces; black bridge and mount bars. | strap in the hood area; tubes (0,34)–(24,46); bridge (26,34)–(46,40); mount (26,42)–(46,52) | #101010 | NV Goggles (both up and down models) |

## 10. Back gear (layout D, 64×64)

| Path | Size | Depicts | Layout | Palette | Used by |
|---|---|---|---|---|---|
| `textures/armor/glider.png` | 64×64 | A **brown leather/canvas backpack** with two dark vertical slots (straps); **grey metal** spars and struts; **pale cream/yellowish canvas** wing panels with olive edges. | backpack (0,0)–(44,28); inner struts (46,0)–(58,18); spars (26,30)–(58,36); outer struts (42,38)–(54,54); canvas (0,30)–(24,46) and (0,48)–(28,62) | #303030, #505030, #707070, #b0b090 | Glider |
| `textures/armor/jetPack.png` | 64×64 | **Silver/grey cylindrical fuel tanks** (with soft highlights); a **brown** central pack with grey cross straps; **yellow/black checkered** nozzle caps; dark grey stub-wing panels and edges. | tanks (0,0)–(24,24); pack (32,0)–(64,22); caps (0,26)–(16,34); wings (26,30)–(54,36), (42,38)–(54,54), (0,48)–(24,62) | #303030, #707070, #909090, #b0b0b0 | Jetpack and Jump Pack, camo 0 |
| `textures/armor/jetPack_bronze.png` | 64×64 | Same, with **golden-bronze** tanks and orange/black caps. | same | #b09050, #907050, #303010 | camo 1 "Bronze" |
| `textures/armor/jetPack_black.png` | 64×64 | Same, with black tanks and red/black caps. | same | #101010, #303030 | camo 2 "Black" |
| `textures/armor/jetPack_darkgreen.png` | 64×64 | Same, with dark olive tanks and white/black caps. | same | #101010, #303010, #103010 | camo 3 "Dark Green" |
| `textures/armor/oxygenFlasks.png` | 64×64 | Two **grey steel cylinders** with black bands; a black harness block; grey valve tops. | cylinders (0,0)–(24,24); harness (32,0)–(52,18); valves (0,26)–(16,32) | #101010, #707070, #909090 | Oxygen Tanks |
| `textures/armor/AntiGravPack.png` | 64×64 | A **grey/white metallic** pack and side cylinders, with a glowing **cyan core crystal** and dark fins. | cylinders (0,0)–(16,18); fins (0,20)–(12,24); core (12,20)–(36,32); plate (40,22)–(64,32); pack (32,0)–(64,18) (overruns) | #707070, #b0b0b0, cyan | Anti-Grav camo 0 |
| `textures/armor/AntiGravPack_bronze.png` | 64×64 | Same, **gold/bronze** metal with a cyan core. | same | #907030, #d0b050 | camo 1 "Bronze" |
| `textures/armor/AntiGravPack_black.png` | 64×64 | Same, black metal with a **red** core. | same | #303030, #101010, #901010 | camo 2 "Black" |
| `textures/armor/AntiGravPack_darkGreen.png` | 64×64 | Same, dark green metal with a **white** core. | same | #103010, #101010, #d0d0d0 | camo 3 "Dark Green" |
| `textures/armor/AntiGravPack_silver.png` | 64×64 | Same, bright silver/white metal with a cyan core. | same | #909090, #d0d0d0, #f0f0f0 | camo 4 "Silver" |

## 11. Coverage checklist

Counts: T1 18, T2 combat 12, T2 commando 2, T3 combat 6, HEV 6, riot 3, exo 6, steam 5, power 3, beret 3, face 7, back 11. Total **82**.

Unused files: `textures/armor/steam_armor_1.png` and `textures/armor/t2_riotArmor.png`.

Related textures documented elsewhere:
- [`textures/gui/TGPlayerInventory.png`](../08-machines-production/gui-textures.md#14-texturesguitgplayerinventorypng-owned-by-the-player-inventory-area-0105): extra-inventory.md §6.
- Item icons `textures/items/*`: [06-items](../06-items/index.md).
- Gun textures used by the akimbo renderers: [03-gun-visuals-audio](../03-gun-visuals-audio/index.md).
