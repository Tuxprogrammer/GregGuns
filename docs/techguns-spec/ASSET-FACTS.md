# Measured asset facts

Machine-measured facts for every Techguns texture and sound, generated from the files themselves. Areas cite these numbers so every asset is described the same way.

- **Textures:**
  - **Average colour:** the mean of opaque pixels (alpha > 16).
  - **Dominant colours:** the three most common colours after bucketing each channel into 8 levels, with their share of opaque pixels.
  - **Opaque:** the share of all pixels that are opaque.
  - **Animation:** the frame count for textures with an `.mcmeta` (vertical strips).
- **Sounds:**
  - **Duration, sample rate and channels:** read from the Ogg Vorbis headers.
  - **Event and category:** taken from `sounds.json`.

What each asset *depicts*, and how it is laid out, is described in the owning area's `textures.md` or `sounds.md`.


## Textures

| Texture | Size | Animation | Average colour (opaque px) | Dominant colours | Opaque |
|---|---|---|---|---|---|
| `textures/armor/AntiGravPack.png` | 64×64 |  | #788A8E | #707070 29%, #B0B0B0 24%, #909090 13% | 30% |
| `textures/armor/AntiGravPack_black.png` | 64×64 |  | #342626 | #303030 44%, #101010 30%, #505050 10% | 30% |
| `textures/armor/AntiGravPack_bronze.png` | 64×64 |  | #8E8951 | #907030 22%, #D0B050 21%, #B09050 10% | 30% |
| `textures/armor/AntiGravPack_darkGreen.png` | 64×64 |  | #273527 | #103010 45%, #101010 33%, #D0D0D0 6% | 30% |
| `textures/armor/AntiGravPack_silver.png` | 64×64 |  | #9AB1B2 | #909090 23%, #D0D0D0 20%, #F0F0F0 18% | 30% |
| `textures/armor/beret_texture.png` | 64×64 |  | #640000 | #701010 55%, #901010 15%, #501010 11% | 23% |
| `textures/armor/beret_texture_black.png` | 64×64 |  | #323232 | #303030 66%, #505050 17%, #101010 15% | 23% |
| `textures/armor/beret_texture_green.png` | 64×64 |  | #0B2D06 | #103010 75%, #101010 17%, #105010 7% | 23% |
| `textures/armor/gasMask.png` | 64×64 |  | #232B21 | #303030 69%, #103010 23%, #101010 5% | 45% |
| `textures/armor/glider.png` | 64×64 |  | #6E6957 | #303030 14%, #505030 14%, #707070 10% | 54% |
| `textures/armor/hevsuit_1.png` | 128×64 |  | #483519 | #101010 51%, #303030 13%, #F09010 5% | 46% |
| `textures/armor/hevsuit_2.png` | 128×64 |  | #895A19 | #B07010 17%, #905010 16%, #101010 12% | 13% |
| `textures/armor/hevsuit_black_1.png` | 128×64 |  | #150F0F | #101010 78%, #303030 15%, #501010 2% | 46% |
| `textures/armor/hevsuit_black_2.png` | 128×64 |  | #0A0A0A | #101010 96%, #303030 3% | 13% |
| `textures/armor/hevsuit_silver_1.png` | 128×64 |  | #35383A | #101010 48%, #303030 21%, #505050 6% | 46% |
| `textures/armor/hevsuit_silver_2.png` | 128×64 |  | #565656 | #101010 22%, #303030 19%, #505050 16% | 13% |
| `textures/armor/jetPack.png` | 64×64 |  | #5D5A55 | #303030 31%, #707070 12%, #909090 12% | 41% |
| `textures/armor/jetPack_black.png` | 64×64 |  | #222020 | #101010 58%, #303030 33%, #505050 6% | 41% |
| `textures/armor/jetPack_bronze.png` | 64×64 |  | #60502B | #303010 24%, #101010 19%, #B09050 10% | 41% |
| `textures/armor/jetPack_darkgreen.png` | 64×64 |  | #1F221C | #101010 47%, #303030 26%, #303010 13% | 41% |
| `textures/armor/nightVisionGoggles.png` | 64×64 |  | #161816 | #101010 95%, #105010 1%, #101030 1% | 15% |
| `textures/armor/oxygenFlasks.png` | 64×64 |  | #424242 | #101010 44%, #707070 23%, #909090 14% | 20% |
| `textures/armor/oxygenMask.png` | 64×64 |  | #777777 | #707070 63%, #909090 25%, #505050 6% | 9% |
| `textures/armor/powerarmor.png` | 256×128 |  | #737472 | #909090 25%, #707070 22%, #B0B0B0 17% | 46% |
| `textures/armor/powerarmor_dark.png` | 256×128 |  | #191212 | #101010 74%, #303030 14%, #501010 4% | 47% |
| `textures/armor/powerarmor_dark2.png` | 256×128 |  | #171212 | #101010 77%, #303030 15%, #301010 3% | 47% |
| `textures/armor/steam_armor.png` | 256×128 |  | #4B422A | #101010 59%, #B09070 6%, #907050 4% | 99% |
| `textures/armor/steam_armor_1.png` | 256×128 |  | #2E1D06 | #101010 59%, #503010 12%, #B07010 7% | 99% |
| `textures/armor/steam_armor_hazard.png` | 256×128 |  | #3A3309 | #101010 62%, #D0D010 6%, #B0B010 4% | 99% |
| `textures/armor/steam_armor_iron.png` | 256×128 |  | #2C2B2B | #101010 59%, #707070 7%, #909090 6% | 99% |
| `textures/armor/steam_armor_rusty.png` | 256×128 |  | #3F2C17 | #101010 59%, #703010 4%, #D09050 3% | 99% |
| `textures/armor/t1_combat_1.png` | 128×64 |  | #695F50 | #B0B0B0 12%, #101010 11%, #505030 8% | 32% |
| `textures/armor/t1_combat_2.png` | 128×64 |  | #523C25 | #503010 26%, #705030 19%, #503030 18% | 13% |
| `textures/armor/t1_miner_1.png` | 128×64 |  | #726C36 | #303030 19%, #303070 16%, #F0D010 10% | 27% |
| `textures/armor/t1_miner_2.png` | 128×64 |  | #373575 | #303070 49%, #505090 14%, #303050 14% | 13% |
| `textures/armor/t1_miner_black_1.png` | 128×64 |  | #6D411D | #303030 39%, #F07010 13%, #101010 10% | 27% |
| `textures/armor/t1_miner_black_2.png` | 128×64 |  | #242424 | #303030 69%, #101010 30%, #505050 0% | 13% |
| `textures/armor/t1_miner_green_1.png` | 128×64 |  | #246E65 | #303030 19%, #10D0F0 17%, #305030 15% | 27% |
| `textures/armor/t1_miner_green_2.png` | 128×64 |  | #214421 | #305030 54%, #103010 40%, #307030 4% | 13% |
| `textures/armor/t1_miner_red_1.png` | 128×64 |  | #5A6819 | #50F010 19%, #303030 19%, #701010 12% | 27% |
| `textures/armor/t1_miner_red_2.png` | 128×64 |  | #891717 | #901010 40%, #701010 38%, #B01010 13% | 13% |
| `textures/armor/t1_scout_1.png` | 128×64 |  | #5C513D | #707050 21%, #303030 10%, #907050 9% | 30% |
| `textures/armor/t1_scout_2.png` | 128×64 |  | #6A5E44 | #707050 37%, #907050 15%, #505030 12% | 12% |
| `textures/armor/t1_scout_black_1.png` | 128×64 |  | #161616 | #101010 80%, #303030 18%, #505030 0% | 30% |
| `textures/armor/t1_scout_black_2.png` | 128×64 |  | #131313 | #101010 98%, #303030 1% | 12% |
| `textures/armor/t1_scout_forest_1.png` | 128×64 |  | #1D3515 | #103010 29%, #101010 16%, #105010 15% | 30% |
| `textures/armor/t1_scout_forest_2.png` | 128×64 |  | #323811 | #303010 46%, #305010 21%, #505010 18% | 12% |
| `textures/armor/t1_scout_snow_1.png` | 128×64 |  | #777575 | #909090 29%, #707070 22%, #B0B0B0 16% | 30% |
| `textures/armor/t1_scout_snow_2.png` | 128×64 |  | #858585 | #909090 52%, #707070 20%, #B0B0B0 17% | 12% |
| `textures/armor/t2_combat_1.png` | 128×64 |  | #42463E | #101010 20%, #505050 19%, #303030 18% | 33% |
| `textures/armor/t2_combat_2.png` | 128×64 |  | #4D5449 | #505050 47%, #507050 14%, #303030 12% | 13% |
| `textures/armor/t2_combat_arctic_1.png` | 128×64 |  | #525150 | #303030 22%, #101010 20%, #505050 15% | 33% |
| `textures/armor/t2_combat_arctic_2.png` | 128×64 |  | #686868 | #505050 29%, #707070 20%, #303030 16% | 13% |
| `textures/armor/t2_combat_desert_1.png` | 128×64 |  | #695A4E | #101010 14%, #B09090 11%, #301010 9% | 33% |
| `textures/armor/t2_combat_desert_2.png` | 128×64 |  | #917D6D | #909070 16%, #B09070 11%, #B09090 11% | 13% |
| `textures/armor/t2_combat_security_1.png` | 128×64 |  | #1E1D30 | #101010 36%, #303030 31%, #101070 15% | 33% |
| `textures/armor/t2_combat_security_2.png` | 128×64 |  | #151556 | #101070 46%, #101050 28%, #101010 11% | 13% |
| `textures/armor/t2_combat_swat_1.png` | 128×64 |  | #1F1E1E | #101010 59%, #303030 31%, #505050 5% | 33% |
| `textures/armor/t2_combat_swat_2.png` | 128×64 |  | #111110 | #101010 89%, #303030 10% | 13% |
| `textures/armor/t2_combat_wood_1.png` | 128×64 |  | #3B4932 | #101010 20%, #303030 12%, #305030 10% | 33% |
| `textures/armor/t2_combat_wood_2.png` | 128×64 |  | #435738 | #305030 24%, #303030 13%, #505030 10% | 13% |
| `textures/armor/t2_commando_1.png` | 128×64 |  | #181818 | #101010 77%, #303030 20%, #909090 1% | 37% |
| `textures/armor/t2_commando_2.png` | 128×64 |  | #181818 | #101010 70%, #303030 29%, #505050 0% | 13% |
| `textures/armor/t2_riotArmor.png` | 128×128 |  | #888A85 | #B0B090 30%, #303030 11%, #F0F0F0 8% | 34% |
| `textures/armor/t2_riotArmor_1.png` | 128×128 |  | #49412A | #503010 27%, #505030 17%, #707050 14% | 33% |
| `textures/armor/t2_riotArmor_2.png` | 128×128 |  | #473B24 | #503010 39%, #505030 21%, #303010 13% | 24% |
| `textures/armor/t3_combat_1.png` | 128×64 |  | #7B8790 | #F0F0F0 14%, #303030 13%, #101030 8% | 40% |
| `textures/armor/t3_combat_2.png` | 128×64 |  | #212730 | #303030 38%, #101030 25%, #103030 20% | 13% |
| `textures/armor/t3_combat_green_1.png` | 128×64 |  | #171C12 | #101010 63%, #103010 14%, #303010 10% | 40% |
| `textures/armor/t3_combat_green_2.png` | 128×64 |  | #151F0E | #101010 55%, #103010 26%, #303010 16% | 13% |
| `textures/armor/t3_combat_silver_1.png` | 128×64 |  | #7A7977 | #101010 27%, #D0D0D0 19%, #B0B0B0 12% | 40% |
| `textures/armor/t3_combat_silver_2.png` | 128×64 |  | #1C1C1C | #101010 77%, #303030 19%, #505050 0% | 13% |
| `textures/armor/t3_exo_1.png` | 128×128 |  | #93999D | #909090 18%, #F0F0F0 13%, #B0B0B0 12% | 49% |
| `textures/armor/t3_exo_2.png` | 128×128 |  | #8A8D90 | #909090 25%, #B0B0B0 15%, #707070 13% | 36% |
| `textures/armor/t3_exo_green_1.png` | 128×128 |  | #171A15 | #101010 83%, #303030 5%, #103010 5% | 49% |
| `textures/armor/t3_exo_green_2.png` | 128×128 |  | #171916 | #101010 89%, #103010 5%, #303010 3% | 36% |
| `textures/armor/t3_exo_silver_1.png` | 128×128 |  | #7C7C7B | #707070 20%, #909090 16%, #505050 15% | 49% |
| `textures/armor/t3_exo_silver_2.png` | 128×128 |  | #6A6A6A | #707070 24%, #505050 19%, #101010 17% | 36% |
| `textures/armor/tacticalMask.png` | 64×64 |  | #1B1A1A | #101010 68%, #303030 29%, #701010 0% | 40% |
| `textures/armor/tacticalMask_green.png` | 64×64 |  | #0E180E | #101010 92%, #103010 6%, #301010 1% | 40% |
| `textures/armor/tacticalMask_white.png` | 64×64 |  | #939394 | #B0B0B0 52%, #909090 21%, #707070 13% | 40% |
| `textures/armor/tacticalmask_lightgreen.png` | 64×64 |  | #232F1A | #303030 38%, #303010 29%, #103010 19% | 40% |
| `textures/blocks/AmmoPress.png` | 256×128 |  | #292724 | #101010 66%, #505050 5%, #909090 3% | 100% |
| `textures/blocks/Bioblock.png` | 64×32 |  | #2E7C10 | #101010 25%, #307010 15%, #30B010 11% | 100% |
| `textures/blocks/ChargingStation.png` | 128×128 |  | #7A7B74 | #707070 20%, #505050 12%, #B0B0B0 11% | 43% |
| `textures/blocks/ChemLab.png` | 256×128 |  | #363435 | #101010 63%, #D0D0D0 9%, #707070 4% | 96% |
| `textures/blocks/ExpCharge.png` | 64×64 |  | #283337 | #303030 52%, #505050 12%, #305050 11% | 50% |
| `textures/blocks/ExpCharge_2.png` | 64×64 |  | #2D3136 | #303030 52%, #505050 11%, #305050 11% | 50% |
| `textures/blocks/Fabricator.png` | 256×256 |  | #535151 | #303030 56%, #909090 10%, #707070 8% | 36% |
| `textures/blocks/MetalPress.png` | 256×128 |  | #383532 | #101010 58%, #909090 7%, #505050 5% | 100% |
| `textures/blocks/TurretBase.png` | 128×128 |  | #717171 | #909090 32%, #B0B0B0 18%, #303030 15% | 47% |
| `textures/blocks/TurretBase_2.png` | 128×128 |  | #757575 | #505050 27%, #B0B0B0 26%, #909090 18% | 47% |
| `textures/blocks/TurretBase_3.png` | 128×128 |  | #585858 | #303030 26%, #909090 20%, #101010 17% | 47% |
| `textures/blocks/TurretBase_4.png` | 128×128 |  | #322A34 | #303030 30%, #101010 25%, #503050 17% | 47% |
| `textures/blocks/TurretBase_5.png` | 128×128 |  | #161616 | #101010 70%, #303030 28%, #505050 0% | 47% |
| `textures/blocks/acid_flow.png` | 32×1024 | 32 frames | #928100 | #909010 82%, #907010 17%, #B07010 0% | 100% |
| `textures/blocks/acid_still.png` | 16×512 | 32 frames | #918100 | #909010 90%, #907010 9%, #B07010 0% | 100% |
| `textures/blocks/airMarker.png` | 32×32 |  | #4E4E4E | #505050 38%, #303030 35%, #707070 21% | 38% |
| `textures/blocks/bio.png` | 16×16 |  | #33A40C | #30B010 55%, #309010 28%, #307010 6% | 100% |
| `textures/blocks/bugnestblock.png` | 16×16 |  | #B8A77F | #D0B090 30%, #B0B070 24%, #B09070 23% | 100% |
| `textures/blocks/bugnestblock_hole.png` | 16×16 |  | #423C30 | #101010 50%, #303030 9%, #707050 5% | 100% |
| `textures/blocks/bugnesteggs.png` | 16×64 | 4 frames | #9AB262 | #B0B070 6%, #D0B090 4%, #F0F0B0 4% | 100% |
| `textures/blocks/bugnestslimy.png` | 16×16 |  | #B3E571 | #D0F070 12%, #D0F090 10%, #B0F050 6% | 53% |
| `textures/blocks/camoNet.png` | 32×32 |  | #465834 | #709050 14%, #101010 12%, #305030 10% | 66% |
| `textures/blocks/camoNet_desert.png` | 32×32 |  | #887F5C | #D0D090 11%, #303030 8%, #707050 8% | 66% |
| `textures/blocks/camoNet_snow.png` | 32×32 |  | #757572 | #D0D0D0 15%, #101010 12%, #B0B0B0 11% | 66% |
| `textures/blocks/concrete01.png` | 32×32 |  | #B3AFA6 | #B0B0B0 79%, #B0B090 14%, #D0B0B0 3% | 100% |
| `textures/blocks/concrete02.png` | 32×32 |  | #89847B | #909070 44%, #909090 28%, #907070 17% | 100% |
| `textures/blocks/concrete03.png` | 32×32 |  | #776F69 | #707070 53%, #907070 15%, #707050 12% | 100% |
| `textures/blocks/concrete04.png` | 32×32 |  | #ACA49D | #B0B0B0 37%, #B0B090 25%, #B09090 19% | 100% |
| `textures/blocks/concrete_hole.png` | 32×32 |  | #4D4B48 | #101010 47%, #B0B0B0 21%, #303030 9% | 100% |
| `textures/blocks/concrete_old2-ctmv.png` | 64×64 |  | #9E968B | #B0B0B0 34%, #B0B090 12%, #909090 7% | 100% |
| `textures/blocks/concrete_old2-top.png` | 32×32 |  | #B8B3AB | #B0B0B0 67%, #B0B090 11%, #D0D0D0 9% | 100% |
| `textures/blocks/concrete_r-ctm.png` | 64×64 |  | #8D8A85 | #B0B0B0 27%, #909090 15%, #505050 14% | 100% |
| `textures/blocks/concrete_r.png` | 32×32 |  | #7A7772 | #707070 18%, #909090 18%, #505050 16% | 100% |
| `textures/blocks/concrete_r02-ctm.png` | 64×64 |  | #706C68 | #707070 19%, #505050 15%, #909070 14% | 100% |
| `textures/blocks/concrete_r02.png` | 32×32 |  | #605C59 | #707070 21%, #505050 19%, #303030 15% | 100% |
| `textures/blocks/container_blue-ctm.png` | 32×32 |  | #363A8B | #303090 51%, #303070 18%, #7070D0 7% | 100% |
| `textures/blocks/container_blue.png` | 16×16 |  | #323685 | #303070 30%, #303090 24%, #5050B0 12% | 100% |
| `textures/blocks/container_green-ctm.png` | 32×32 |  | #2F5330 | #305030 56%, #103010 21%, #709070 8% | 100% |
| `textures/blocks/container_green.png` | 16×16 |  | #294D2B | #103010 33%, #305030 31%, #507050 12% | 100% |
| `textures/blocks/container_orange-ctm.png` | 32×32 |  | #9E6323 | #905010 26%, #B07030 17%, #B07010 15% | 100% |
| `textures/blocks/container_orange.png` | 16×16 |  | #975D20 | #705010 22%, #905010 20%, #D09030 14% | 100% |
| `textures/blocks/container_red-ctm.png` | 32×32 |  | #8B3636 | #903030 55%, #703030 18%, #D07070 8% | 100% |
| `textures/blocks/container_red.png` | 16×16 |  | #853232 | #703030 30%, #903030 25%, #B05050 12% | 100% |
| `textures/blocks/container_scifi1-ctm.png` | 64×64 |  | #6F6A5B | #707070 40%, #707050 9%, #505050 9% | 100% |
| `textures/blocks/container_scifi1.png` | 32×32 |  | #777262 | #707070 16%, #909090 10%, #505050 10% | 100% |
| `textures/blocks/container_scifi_rusty-ctm.png` | 64×64 |  | #856444 | #907050 39%, #503030 7%, #905050 7% | 100% |
| `textures/blocks/container_scifi_rusty.png` | 32×32 |  | #826947 | #907050 21%, #B09070 8%, #301010 6% | 100% |
| `textures/blocks/controlpanel_front.png` | 32×32 |  | #4A3D3A | #101010 17%, #503030 17%, #303030 16% | 100% |
| `textures/blocks/controlpanel_overlay_front.png` | 32×256 | 8 frames | #095200 | #103010 31%, #101010 29%, #10F010 15% | 13% |
| `textures/blocks/controlpanel_side.png` | 32×32 |  | #4F413D | #503030 32%, #505030 17%, #707070 12% | 100% |
| `textures/blocks/controlpanel_top.png` | 32×32 |  | #534540 | #503030 49%, #505030 20%, #707070 12% | 100% |
| `textures/blocks/door01.png` | 32×32 |  | #888E82 | #909090 37%, #707070 19%, #709070 14% | 100% |
| `textures/blocks/door01_lower.png` | 32×32 |  | #666A5D | #909090 16%, #707070 16%, #505050 15% | 100% |
| `textures/blocks/door01_upper.png` | 32×32 |  | #747970 | #909090 24%, #707070 22%, #505050 10% | 100% |
| `textures/blocks/drillEngine.png` | 32×32 |  | #4A4745 | #303030 24%, #101010 20%, #505050 20% | 100% |
| `textures/blocks/drillEngineController.png` | 32×32 |  | #4F4C4A | #101010 20%, #303030 19%, #505050 17% | 100% |
| `textures/blocks/drillEngineTop_c-ctm.png` | 64×64 |  | #4F4B49 | #101010 21%, #303030 18%, #505050 18% | 100% |
| `textures/blocks/drillEngineTop_c.png` | 32×32 |  | #4B4746 | #101010 22%, #303030 20%, #505050 19% | 100% |
| `textures/blocks/drillEngine_c-ctm.png` | 64×64 |  | #4F4B49 | #101010 21%, #303030 18%, #505050 18% | 100% |
| `textures/blocks/drillEngine_c.png` | 32×32 |  | #4B4746 | #101010 22%, #303030 20%, #505050 19% | 100% |
| `textures/blocks/drillFrame.png` | 32×32 |  | #6A5F62 | #303030 19%, #505050 17%, #705050 9% | 48% |
| `textures/blocks/drillFrameCorner.png` | 32×32 |  | #2D5638 | #305030 28%, #307050 27%, #103010 17% | 55% |
| `textures/blocks/drillFrame_c-ctm.png` | 64×64 |  | #6F6467 | #303030 19%, #505050 14%, #B0B0B0 11% | 33% |
| `textures/blocks/drillFrame_c.png` | 32×32 |  | #6A5F62 | #303030 19%, #505050 17%, #705050 9% | 48% |
| `textures/blocks/drillFrame_outer.png` | 32×32 |  | #705854 | #705050 25%, #907070 13%, #503030 7% | 100% |
| `textures/blocks/drillFrame_outer_c-ctm.png` | 64×64 |  | #715A57 | #705050 22%, #907070 15%, #503030 8% | 82% |
| `textures/blocks/drillFrame_outer_c.png` | 32×32 |  | #705853 | #705050 20%, #907070 14%, #303030 8% | 100% |
| `textures/blocks/drillRod-ctmv.png` | 64×64 |  | #767574 | #505050 16%, #B0B0B0 15%, #707070 14% | 100% |
| `textures/blocks/drillRod-top.png` | 32×32 |  | #5A5A5A | #303030 25%, #505050 22%, #707070 18% | 100% |
| `textures/blocks/drillRodSide.png` | 32×32 |  | #6A6A6A | #505050 18%, #707070 18%, #303030 17% | 100% |
| `textures/blocks/drillRodTop.png` | 32×32 |  | #4D4D4D | #101010 31%, #303030 16%, #707070 13% | 100% |
| `textures/blocks/drillhead.png` | 32×32 |  | #5E5E5E | #303030 26%, #505050 21%, #707070 17% | 100% |
| `textures/blocks/drillheadCarbon.png` | 32×32 |  | #252525 | #303030 45%, #101010 44%, #505050 10% | 100% |
| `textures/blocks/drillheadObsidian.png` | 32×32 |  | #393A4D | #505070 19%, #303030 17%, #303050 15% | 100% |
| `textures/blocks/fabricatorController.png` | 32×32 |  | #787D7E | #101010 24%, #D0D0D0 21%, #B0B0B0 12% | 100% |
| `textures/blocks/fabricatorGlass.png` | 32×32 |  | #1A1919 | #101010 99%, #301010 0% | 23% |
| `textures/blocks/fabricatorHousing.png` | 32×32 |  | #949494 | #F0F0F0 36%, #101010 24%, #D0D0D0 13% | 100% |
| `textures/blocks/flagBlock.png` | 32×32 |  | #A296A4 | #B0B0B0 17%, #D0D0D0 15%, #F0F0F0 15% | 100% |
| `textures/blocks/flagpost.png` | 128×128 |  | #8B7D86 | #D0D0D0 16%, #B0B0B0 14%, #909090 12% | 40% |
| `textures/blocks/invisible.png` | 16×16 |  | fully transparent |  | 0% |
| `textures/blocks/ladder01.png` | 16×16 |  | #757575 | #707070 76%, #909090 18%, #505050 5% | 71% |
| `textures/blocks/ladder01_inv.png` | 32×32 |  | #8D8D8D | #909090 94%, #707070 5% | 23% |
| `textures/blocks/lamp01.png` | 32×32 |  | #746E45 | #101010 58%, #F0D050 9%, #F0F0B0 9% | 100% |
| `textures/blocks/lamp02.png` | 32×32 |  | #DCDCDA | #F0F0F0 57%, #D0D0D0 31%, #B0B0B0 11% | 100% |
| `textures/blocks/lantern01_side.png` | 32×32 |  | #888250 | #101010 49%, #F0F0B0 17%, #F0F050 12% | 100% |
| `textures/blocks/lantern01_top.png` | 32×32 |  | #1B1B1B | #101010 83%, #303030 16% | 100% |
| `textures/blocks/lantern02_side.png` | 32×32 |  | #DADAD6 | #F0F0F0 40%, #D0D0D0 27%, #F0F0D0 17% | 100% |
| `textures/blocks/lantern02_top.png` | 32×32 |  | #D4D4D4 | #D0D0D0 59%, #F0F0F0 29%, #B0B0B0 11% | 100% |
| `textures/blocks/metalLadder.png` | 32×32 |  | #627076 | #507070 23%, #709090 18%, #707070 17% | 71% |
| `textures/blocks/metalLadderCarbon.png` | 32×32 |  | #191919 | #101010 80%, #303030 19% | 71% |
| `textures/blocks/metalLadderRusty.png` | 32×32 |  | #40240F | #301010 36%, #503010 30%, #303010 11% | 71% |
| `textures/blocks/metalLadderShiny.png` | 32×32 |  | #DDD9DB | #F0F0F0 43%, #D0D0D0 30%, #B0B0B0 10% | 71% |
| `textures/blocks/metalgrate_dark.png` | 32×32 |  | #262522 | #101010 60%, #303030 15%, #707070 7% | 100% |
| `textures/blocks/metalgrate_hazard.png` | 32×32 |  | #373425 | #101010 51%, #303030 10%, #707070 6% | 100% |
| `textures/blocks/metalgrate_rusty.png` | 32×32 |  | #38352F | #101010 49%, #303030 12%, #707070 7% | 100% |
| `textures/blocks/metalgrate_rusty2.png` | 32×32 |  | #342C23 | #101010 51%, #303030 7%, #303010 5% | 100% |
| `textures/blocks/metalpanel16_largeborder-ctm.png` | 32×32 |  | #727170 | #B0B0B0 23%, #505050 16%, #707070 16% | 100% |
| `textures/blocks/metalpanel16_largeborder.png` | 16×16 |  | #4F4E4D | #505050 33%, #303030 25%, #707070 20% | 100% |
| `textures/blocks/metalpanel2_hazard-ctm.png` | 64×64 |  | #71642D | #909030 10%, #303030 9%, #503030 7% | 100% |
| `textures/blocks/metalpanel2_hazard.png` | 32×32 |  | #6B5A1F | #101010 11%, #301010 6%, #303010 6% | 100% |
| `textures/blocks/metalpanel2_rusty-ctm.png` | 64×64 |  | #776856 | #707050 19%, #705050 15%, #909070 8% | 100% |
| `textures/blocks/metalpanel2_rusty.png` | 32×32 |  | #715E48 | #707050 11%, #705030 10%, #505030 9% | 100% |
| `textures/blocks/metalpanel_dark.png` | 32×32 |  | #2D2B27 | #303030 38%, #101010 29%, #303010 11% | 100% |
| `textures/blocks/metalpanel_hazard.png` | 32×32 |  | #645E31 | #303030 15%, #303010 12%, #101010 6% | 100% |
| `textures/blocks/metalpanel_rusty.png` | 32×32 |  | #676154 | #505030 13%, #303030 11%, #707050 10% | 100% |
| `textures/blocks/metalpanel_rusty2.png` | 32×32 |  | #4F4334 | #303010 16%, #503030 15%, #505030 11% | 100% |
| `textures/blocks/metalpanel_techgrid-v9.png` | 48×48 |  | #A2A5A5 | #B0B0B0 47%, #909090 28%, #90B0B0 6% | 100% |
| `textures/blocks/milk_flow.png` | 32×1024 | 32 frames | #CDCDCD | #D0D0D0 100% | 100% |
| `textures/blocks/milk_still.png` | 16×512 | 32 frames | #CDCDCD | #D0D0D0 100% | 100% |
| `textures/blocks/oreCluster.png` | 32×32 |  | #727272 | #707070 59%, #909090 26%, #505050 14% | 62% |
| `textures/blocks/oreCluster1.png` | 32×32 |  | #808080 | #707070 16%, #909090 15%, #303030 13% | 37% |
| `textures/blocks/oreClusterNew1.png` | 32×32 |  | #696969 | #303030 20%, #101010 15%, #707070 15% | 37% |
| `textures/blocks/oreClusterNew2.png` | 32×32 |  | #545454 | #303030 29%, #505050 20%, #707070 15% | 33% |
| `textures/blocks/oreClusterNew3.png` | 32×32 |  | #838383 | #303030 17%, #707070 16%, #B0B0B0 15% | 23% |
| `textures/blocks/oreClusterNew4.png` | 32×32 |  | #929292 | #909090 30%, #B0B0B0 30%, #D0D0D0 12% | 57% |
| `textures/blocks/oreCluster_1.png` | 32×32 |  | #797979 | #707070 52%, #909090 43%, #505050 3% | 66% |
| `textures/blocks/oreCluster_11.png` | 32×32 |  | #6F6F6F | #303030 20%, #505050 19%, #909090 18% | 33% |
| `textures/blocks/oreCluster_2.png` | 32×32 |  | #7A7A7A | #909090 48%, #707070 47%, #505050 4% | 76% |
| `textures/blocks/oreCluster_21.png` | 32×32 |  | #A9A9A9 | #F0F0F0 26%, #707070 19%, #909090 16% | 23% |
| `textures/blocks/oreCluster_3.png` | 32×32 |  | #612F2D | #301010 20%, #503030 17%, #501010 11% | 62% |
| `textures/blocks/oreCluster_31.png` | 32×32 |  | #6B6B6B | #101010 30%, #F0F0F0 13%, #303030 11% | 37% |
| `textures/blocks/oreCluster_4.png` | 32×32 |  | #4D4D4D | #505050 39%, #303030 27%, #909090 15% | 42% |
| `textures/blocks/oreCluster_41.png` | 32×32 |  | #A1A1A1 | #B0B0B0 36%, #909090 27%, #D0D0D0 19% | 57% |
| `textures/blocks/oreCluster_5.png` | 32×32 |  | #6B3331 | #503030 16%, #301010 15%, #905050 12% | 76% |
| `textures/blocks/oreCopperBlock.png` | 16×16 |  | #897D75 | #707070 48%, #909090 26%, #907070 8% | 100% |
| `textures/blocks/oreLeadBlock.png` | 16×16 |  | #7E8087 | #707070 51%, #909090 27%, #7090B0 5% | 100% |
| `textures/blocks/oreTinBlock.png` | 16×16 |  | #838789 | #707070 49%, #909090 30%, #B0D0D0 5% | 100% |
| `textures/blocks/oreTitanIron.png` | 16×64 | 4 frames | #878989 | #707070 65%, #909090 19%, #D0D0D0 6% | 100% |
| `textures/blocks/oreUranium.png` | 16×64 | 4 frames | #7F927C | #707070 38%, #909090 19%, #709070 14% | 100% |
| `textures/blocks/paintmachine_bottom.png` | 32×32 |  | #515150 | #505050 48%, #303030 26%, #909090 12% | 100% |
| `textures/blocks/paintmachine_front.png` | 32×32 |  | #676B68 | #909090 21%, #B0B0B0 13%, #101010 11% | 100% |
| `textures/blocks/paintmachine_side.png` | 32×32 |  | #515150 | #505050 48%, #303030 26%, #909090 12% | 100% |
| `textures/blocks/paintmachine_textures.png` | 64×64 |  | #5A5B5A | #505050 23%, #909090 17%, #303030 15% | 75% |
| `textures/blocks/paintmachine_top.png` | 32×32 |  | #555555 | #101010 24%, #909090 18%, #707070 14% | 100% |
| `textures/blocks/rc_block.png` | 32×32 |  | #C5C5C5 | #F0F0F0 56%, #B0B0B0 28%, #505050 7% | 100% |
| `textures/blocks/rc_controller.png` | 32×32 |  | #C8D3DC | #F0F0F0 50%, #B0B0B0 34%, #B0D0F0 2% | 100% |
| `textures/blocks/rc_glass.png` | 32×32 |  | #D7D7D7 | #F0F0F0 55%, #B0B0B0 42%, #D0D0D0 2% | 52% |
| `textures/blocks/reactionChamber.png` | 32×32 |  | #3B3B3B | #101010 76%, #F0F0F0 23% | 100% |
| `textures/blocks/reactionChamber2.png` | 32×32 |  | #3B0000 | #101010 76%, #F01010 23% | 100% |
| `textures/blocks/reactionChamberController.png` | 32×32 |  | #505050 | #101010 58%, #F0F0F0 23%, #707070 17% | 100% |
| `textures/blocks/reactionChamberController2.png` | 32×32 |  | #411808 | #101010 58%, #F01010 23%, #105030 8% | 100% |
| `textures/blocks/reactionChamberGlass.png` | 32×32 |  | #FFFFFF | #F0F0F0 100% | 23% |
| `textures/blocks/reinforced_chest.png` | 128×128 |  | #5B513E | #907050 22%, #303030 19%, #505050 16% | 40% |
| `textures/blocks/reinforced_chest_double.png` | 256×128 |  | #5B503C | #907050 23%, #303030 16%, #505050 13% | 36% |
| `textures/blocks/repairbench_textures.png` | 64×64 |  | #5B554E | #505050 21%, #909090 17%, #705030 11% | 75% |
| `textures/blocks/sandbags-side.png` | 32×32 |  | #514D3E | #505030 18%, #101010 14%, #707050 13% | 100% |
| `textures/blocks/sandbags-top.png` | 32×32 |  | #706A59 | #707050 17%, #505050 10%, #101010 9% | 100% |
| `textures/blocks/screen_front.png` | 32×32 |  | #2F2C2B | #101010 58%, #303030 17%, #707070 12% | 100% |
| `textures/blocks/screen_overlay_front.png` | 32×256 | 8 frames | #004300 | #101010 28%, #103010 25%, #105010 20% | 74% |
| `textures/blocks/screen_side.png` | 32×32 |  | #4D403B | #503030 29%, #505030 14%, #707070 11% | 100% |
| `textures/blocks/screen_top.png` | 32×32 |  | #534540 | #503030 49%, #505030 20%, #707070 12% | 100% |
| `textures/blocks/steelframe_blue-ctm.png` | 32×32 |  | #616973 | #303030 14%, #707090 12%, #709090 12% | 100% |
| `textures/blocks/steelframe_blue.png` | 16×16 |  | #5B6066 | #303030 25%, #101010 12%, #D0D0D0 11% | 100% |
| `textures/blocks/steelframe_dark-ctm.png` | 32×32 |  | #424141 | #505050 33%, #303030 32%, #707070 14% | 100% |
| `textures/blocks/steelframe_dark.png` | 16×16 |  | #2F2E2E | #303030 49%, #101010 26%, #505050 19% | 100% |
| `textures/blocks/steelframe_glass.png` | 32×32 |  | #0B0B0B | #101010 85%, #303030 15% | 52% |
| `textures/blocks/steelframe_scaffold-ctm.png` | 32×32 |  | #444445 | #303030 34%, #505050 32%, #707070 18% | 100% |
| `textures/blocks/steelframe_scaffold.png` | 16×16 |  | #4A4A4A | #303030 44%, #505050 21%, #909090 12% | 100% |
| `textures/blocks/tent.png` | 32×32 |  | #C3B692 | #D0B090 69%, #B0B090 24%, #D0D090 2% | 100% |
| `textures/blocks/tntCharge.png` | 64×64 |  | #5F201D | #901010 25%, #501010 24%, #505050 20% | 29% |
| `textures/blocks/tntCharge_2.png` | 64×64 |  | #671D1C | #901010 25%, #501010 24%, #701010 20% | 29% |
| `textures/blocks/weapon_chest.png` | 128×128 |  | #1C2117 | #303010 50%, #101010 35%, #103010 11% | 40% |
| `textures/blocks/weapon_chest_double.png` | 256×128 |  | #1B2016 | #303010 52%, #101010 36%, #103010 9% | 36% |
| `textures/entity/BlueBeam.png` | 32×32 |  | #5083B9 | #105090 23%, #105070 14%, #F0F0F0 9% | 100% |
| `textures/entity/Laser.png` | 32×32 |  | #F85D26 | #F01010 31%, #F07010 12%, #D01010 9% | 100% |
| `textures/entity/alienbug.png` | 64×64 |  | #AF8A56 | #B09050 28%, #D0B070 14%, #D09050 7% | 34% |
| `textures/entity/apache.png` | 256×256 |  | #3A402A | #505030 26%, #303030 24%, #305030 22% | 100% |
| `textures/entity/army_soldier.png` | 128×128 |  | #5C4D3E | #505050 20%, #303030 13%, #B07050 7% | 28% |
| `textures/entity/bandit.png` | 64×64 |  | #5B514B | #101010 25%, #B0B0B0 18%, #909090 12% | 28% |
| `textures/entity/bioblob.png` | 64×64 |  | #57A432 | #509010 18%, #307010 14%, #305010 8% | 73% |
| `textures/entity/bullet.png` | 32×32 |  | #C68E47 | #B05010 17%, #F07010 13%, #D07010 11% | 18% |
| `textures/entity/cyberdemon.png` | 128×128 |  | #38120D | #101010 51%, #701010 13%, #501010 10% | 100% |
| `textures/entity/dictator.png` | 64×64 |  | #594F49 | #101010 27%, #B0B0B0 17%, #705030 13% | 28% |
| `textures/entity/gore.png` | 128×128 |  | #761E18 | #701010 29%, #501010 19%, #903030 16% | 28% |
| `textures/entity/handgunBullet.png` | 16×16 |  | #616161 | #B0B0B0 17%, #707070 16%, #303030 16% | 82% |
| `textures/entity/launchergrenade.png` | 32×16 |  | #967953 | #101010 12%, #D0D090 7%, #B0B070 6% | 100% |
| `textures/entity/reactionchamber.png` | 256×256 |  | #3B3B3B | #101010 64%, #F0F0F0 9%, #303030 7% | 100% |
| `textures/entity/reactionchamberglass.png` | 128×128 |  | #C7C7C7 | #D0D0D0 99%, #B0B0B0 0% | 4% |
| `textures/entity/supermutant_texture_1.png` | 128×128 |  | #987A22 | #B09030 26%, #B09010 13%, #D09030 11% | 42% |
| `textures/entity/supermutant_texture_2.png` | 128×128 |  | #907525 | #B09030 23%, #B09010 11%, #503030 10% | 45% |
| `textures/entity/supermutant_texture_3.png` | 128×128 |  | #877130 | #B09030 18%, #505050 10%, #707070 9% | 51% |
| `textures/entity/zombie_soldier.png` | 128×128 |  | #484E2B | #303030 13%, #307030 11%, #507030 8% | 28% |
| `textures/fx/Explosion.png` | 480×480 |  | #FD956F | #F07050 54%, #F0F0F0 12%, #F0F0B0 5% | 29% |
| `textures/fx/Testscope.png` | 3072×3072 |  | #000000 | #101010 100% | 92% |
| `textures/fx/alienflare.png` | 512×512 |  | #1E0C24 | #101010 76%, #301030 3%, #301050 2% | 100% |
| `textures/fx/bio.png` | 64×64 |  | #5DB049 | #30B030 10%, #70D070 7%, #50B030 7% | 100% |
| `textures/fx/blasterflare01.png` | 512×512 |  | #23190C | #101010 76%, #301010 3%, #503010 2% | 100% |
| `textures/fx/blobs.png` | 512×512 |  | #142F0E | #101010 68%, #103010 5%, #105010 3% | 100% |
| `textures/fx/blood01.png` | 256×256 |  | #56090A | #501010 67%, #701010 21%, #301010 8% | 28% |
| `textures/fx/blood4x4.png` | 256×256 |  | #858585 | #909090 45%, #707070 18%, #B0B0B0 15% | 39% |
| `textures/fx/dustclouds.png` | 256×256 |  | #8B8B8B | #909090 54%, #707070 22%, #B0B0B0 18% | 58% |
| `textures/fx/dustclouds02.png` | 256×256 |  | #787878 | #707070 85%, #909090 13%, #907090 0% | 35% |
| `textures/fx/fireball.png` | 64×64 |  | #FB9527 | #F0D010 13%, #F0B010 12%, #F01010 11% | 56% |
| `textures/fx/fireball_4x4_add.png` | 256×256 |  | #2E1C0A | #101010 63%, #301010 10%, #503010 5% | 100% |
| `textures/fx/fireball_4x4_alpha.png` | 256×256 |  | #FF9133 | #F09030 30%, #F07010 26%, #F07030 10% | 37% |
| `textures/fx/fireblast.png` | 512×512 |  | #574F40 | #101010 48%, #F0F0F0 11%, #303030 7% | 29% |
| `textures/fx/flamethrower.png` | 512×512 |  | #724416 | #101010 39%, #301010 5%, #703010 3% | 100% |
| `textures/fx/flamethrower2.png` | 512×512 |  | #3B230C | #101010 65%, #301010 3%, #703010 2% | 100% |
| `textures/fx/flamethrower_a.png` | 512×512 |  | #C3802D | #F09010 6%, #F07010 6%, #301010 5% | 57% |
| `textures/fx/glow.png` | 64×64 |  | #FFFFFF | #F0F0F0 100% | 100% |
| `textures/fx/heatray.png` | 128×128 |  | #44401F | #101010 61%, #F0F0F0 4%, #F0F0D0 2% | 100% |
| `textures/fx/heatray2.png` | 128×128 |  | #FF964A | #F01010 14%, #F0F0F0 11%, #F03010 9% | 42% |
| `textures/fx/impact.png` | 256×256 |  | #2F2F2F | #101010 70%, #F0F0F0 9%, #303030 5% | 100% |
| `textures/fx/largesmoke.png` | 512×512 |  | #957F80 | #907070 24%, #707070 19%, #705050 11% | 27% |
| `textures/fx/laser2.png` | 128×64 |  | #B36733 | #301010 8%, #501010 6%, #F0F090 6% | 100% |
| `textures/fx/laser2_a.png` | 128×64 |  | #EE7F3F | #F07030 8%, #B01010 8%, #F05030 8% | 86% |
| `textures/fx/laser3.png` | 128×64 |  | #B94637 | #F0D0B0 9%, #101010 8%, #D01010 8% | 100% |
| `textures/fx/laserDeathfx.png` | 64×64 |  | #C23700 | #D03010 21%, #F05010 15%, #F03010 13% | 100% |
| `textures/fx/laserFlash2x2.png` | 256×256 |  | #FE3030 | #F03030 47%, #F01010 35%, #F05050 9% | 16% |
| `textures/fx/laser_blue.png` | 128×64 |  | #3367B3 | #101030 8%, #101050 6%, #90F0F0 6% | 100% |
| `textures/fx/laserflare01.png` | 512×512 |  | #110902 | #101010 87%, #301010 3%, #501010 1% | 100% |
| `textures/fx/laserflare02.png` | 512×512 |  | #120302 | #101010 87%, #301010 3%, #501010 1% | 100% |
| `textures/fx/lensflare1.png` | 128×128 |  | #252525 | #101010 67%, #303030 11%, #505050 6% | 100% |
| `textures/fx/lensflare2.png` | 128×128 |  | #222222 | #101010 72%, #303030 10%, #505050 4% | 100% |
| `textures/fx/lensflare3.png` | 128×128 |  | #1D1D1D | #101010 69%, #303030 16%, #505050 5% | 100% |
| `textures/fx/lensflare4.png` | 128×128 |  | #1D1D1D | #101010 68%, #303030 17%, #505050 5% | 100% |
| `textures/fx/lightning.png` | 1024×512 |  | #2236FE | #1010F0 70%, #1030F0 4%, #D0D0F0 3% | 18% |
| `textures/fx/lightning2.png` | 1024×512 |  | #AAB4FE | #F0F0F0 17%, #9090F0 17%, #B0B0F0 14% | 34% |
| `textures/fx/lightning3.png` | 1024×512 |  | #A7EBFD | #70D0F0 34%, #F0F0F0 27%, #B0F0F0 26% | 12% |
| `textures/fx/lightningflare4x4.png` | 512×512 |  | #A1DFF8 | #F0F0F0 22%, #50B0F0 22%, #90F0F0 15% | 24% |
| `textures/fx/lightningflare4x4_2.png` | 512×512 |  | #A1DEF6 | #F0F0F0 23%, #50B0F0 22%, #90F0F0 14% | 15% |
| `textures/fx/muzzleflash2.png` | 256×256 |  | #FEE785 | #F0D050 34%, #F0F0F0 19%, #F0F0B0 10% | 5% |
| `textures/fx/muzzleflash4x4.png` | 256×256 |  | #F0C0A3 | #F07050 15%, #D0D0D0 14%, #F0F0F0 14% | 13% |
| `textures/fx/muzzleflash_blue.png` | 256×256 |  | #9CB5FF | #90B0F0 17%, #7090F0 16%, #D0D0F0 11% | 5% |
| `textures/fx/nukebeam.png` | 128×1088 |  | #897430 | #101010 28%, #F0F0F0 11%, #301010 7% | 100% |
| `textures/fx/nukebeamflare.png` | 512×512 |  | #61501A | #101010 48%, #F0F0F0 6%, #301010 6% | 100% |
| `textures/fx/shockwave.png` | 64×64 |  | #0B0B09 | #101010 88%, #303030 6%, #303010 2% | 100% |
| `textures/fx/sonicwave4x4.png` | 1024×1024 |  | #090C13 | #101010 87%, #101030 1%, #103030 1% | 100% |
| `textures/fx/splat.png` | 384×384 |  | #020302 | #101010 97%, #303030 0%, #505050 0% | 100% |
| `textures/fx/techscope.png` | 2048×2048 |  | #021616 | #101010 90%, #10F0F0 7%, #303030 1% | 78% |
| `textures/fx/teslaflare01.png` | 512×512 |  | #09111E | #101010 80%, #101030 3%, #101050 2% | 100% |
| `textures/gui/ChargingStation_Gui.png` | 256×256 |  | #A7A7A7 | #D0D0D0 49%, #909090 34%, #F0F0F0 6% | 44% |
| `textures/gui/Fabricator_Gui.png` | 256×256 |  | #A4A4A3 | #D0D0D0 43%, #909090 36%, #F0F0F0 7% | 45% |
| `textures/gui/ReactionChamber_Gui.png` | 256×256 |  | #9FA099 | #909090 39%, #D0D0D0 35%, #F0F0F0 8% | 47% |
| `textures/gui/TGPlayerInventory.png` | 256×256 |  | #868685 | #909090 45%, #D0D0D0 21%, #101010 14% | 48% |
| `textures/gui/ammoPress_GUI.png` | 256×256 |  | #A4A4A2 | #D0D0D0 46%, #909090 33%, #F0F0F0 7% | 48% |
| `textures/gui/camoBench_GUI.png` | 256×256 |  | #969EAB | #D0D0D0 38%, #909090 34%, #F0F0F0 8% | 50% |
| `textures/gui/chemLab_Gui.png` | 256×256 |  | #A2A2A2 | #D0D0D0 41%, #909090 37%, #F0F0F0 7% | 47% |
| `textures/gui/expChargeGui.png` | 256×256 |  | #545A54 | #909090 24%, #505050 21%, #305030 20% | 44% |
| `textures/gui/metalPress_GUI.png` | 256×256 |  | #A7A7A7 | #D0D0D0 48%, #909090 35%, #F0F0F0 6% | 45% |
| `textures/gui/oreDrill_GUI.png` | 256×256 |  | #9F9E9E | #909090 44%, #D0D0D0 34%, #F0F0F0 9% | 46% |
| `textures/gui/repairBench_GUI.png` | 256×256 |  | #A1A1A1 | #909090 43%, #D0D0D0 38%, #F0F0F0 8% | 44% |
| `textures/gui/tgchest_gui.png` | 256×256 |  | #989898 | #909090 59%, #D0D0D0 20%, #F0F0F0 9% | 77% |
| `textures/gui/tntChargeGui.png` | 256×256 |  | #763A3A | #909090 24%, #101010 13%, #B01010 10% | 44% |
| `textures/gui/turretBase_GUI.png` | 256×256 |  | #989896 | #909090 41%, #D0D0D0 28%, #505050 10% | 45% |
| `textures/guns/AugTexture.png` | 128×64 |  | #1F372C | #305030 57%, #101010 24%, #307050 4% | 37% |
| `textures/guns/AugTexture_1.png` | 128×64 |  | #212121 | #303030 64%, #101010 25%, #505050 5% | 37% |
| `textures/guns/AugTexture_2.png` | 128×64 |  | #18371F | #105030 43%, #101010 24%, #103010 17% | 37% |
| `textures/guns/AugTexture_3.png` | 128×64 |  | #8B8D60 | #D0D090 38%, #101010 24%, #B0B070 9% | 37% |
| `textures/guns/AugTexture_4.png` | 128×64 |  | #520606 | #701010 34%, #101010 24%, #501010 17% | 37% |
| `textures/guns/BioGun.png` | 64×64 |  | #2E452C | #101010 46%, #505050 7%, #B0B090 5% | 97% |
| `textures/guns/Flamethrower.png` | 64×64 |  | #463C3B | #101010 58%, #F0F0F0 8%, #505050 5% | 100% |
| `textures/guns/Lasergun.png` | 64×64 |  | #423D3A | #101010 38%, #303030 16%, #909090 14% | 94% |
| `textures/guns/NDR.png` | 64×64 |  | #967E6E | #505050 29%, #909090 19%, #B03030 10% | 44% |
| `textures/guns/PDW.png` | 64×64 |  | #262A2C | #101010 55%, #303030 23%, #303050 7% | 100% |
| `textures/guns/PDW_1.png` | 64×64 |  | #212120 | #101010 73%, #303030 14%, #D0D0D0 5% | 100% |
| `textures/guns/PDW_2.png` | 64×64 |  | #090B06 | #101010 81%, #103010 15%, #303010 1% | 100% |
| `textures/guns/TeslaGun.png` | 64×64 |  | #383839 | #101010 48%, #909090 16%, #505050 14% | 100% |
| `textures/guns/ak47Texture.png` | 128×64 |  | #382C22 | #303030 64%, #503010 14%, #703010 7% | 43% |
| `textures/guns/ak47Texture_1.png` | 128×64 |  | #192B42 | #303030 64%, #103070 12%, #103090 4% | 42% |
| `textures/guns/alien_blaster.png` | 128×64 |  | #C6C9CE | #D0D0D0 71%, #B0B0B0 7%, #D0D0F0 5% | 99% |
| `textures/guns/as50Texture.png` | 256×128 |  | #33322C | #101010 54%, #303030 26%, #505050 13% | 16% |
| `textures/guns/blasterRifle.png` | 256×128 |  | #1E1E1E | #101010 50%, #303030 49%, #505050 0% | 31% |
| `textures/guns/boltactionrifle.png` | 128×32 |  | #13100D | #101010 71%, #303010 14%, #303030 7% | 100% |
| `textures/guns/boltactionrifle_1.png` | 128×32 |  | #323A27 | #303010 43%, #303030 22%, #101010 13% | 33% |
| `textures/guns/boltactionrifle_2.png` | 128×32 |  | #202020 | #303030 58%, #101010 40%, #909090 0% | 33% |
| `textures/guns/chainsaw.png` | 128×64 |  | #5C492C | #101010 52%, #B0B0B0 15%, #F0B010 14% | 97% |
| `textures/guns/combatShotgun.png` | 64×32 |  | #373737 | #101010 33%, #303030 26%, #505050 18% | 100% |
| `textures/guns/cyberdemonblaster.png` | 64×64 |  | #4E3F39 | #101010 44%, #707070 22%, #909090 18% | 93% |
| `textures/guns/drillTexture.png` | 128×64 |  | #1A1B21 | #101010 59%, #303030 38%, #707070 1% | 88% |
| `textures/guns/fragGrenade_texture.png` | 64×64 |  | #434F43 | #303030 29%, #305030 29%, #707070 24% | 31% |
| `textures/guns/goldenrevolver.png` | 64×32 |  | #636136 | #101010 49%, #F0F090 17%, #D0D070 11% | 100% |
| `textures/guns/grenadelauncher.png` | 128×128 |  | #3F3E3D | #303030 31%, #101010 26%, #505050 18% | 100% |
| `textures/guns/grimreaper.png` | 128×64 |  | #727461 | #909070 32%, #505050 11%, #707050 10% | 96% |
| `textures/guns/handgun.png` | 64×32 |  | #615E55 | #B0B0B0 26%, #101010 20%, #707070 14% | 100% |
| `textures/guns/laserGunNew.png` | 128×128 |  | #515150 | #505050 77%, #303030 15%, #909090 3% | 65% |
| `textures/guns/m4Texture.png` | 128×64 |  | #25262A | #303030 44%, #101010 31%, #101030 9% | 40% |
| `textures/guns/m4Texture_1.png` | 128×64 |  | #375320 | #305010 21%, #103010 16%, #507030 11% | 40% |
| `textures/guns/m4Texture_2.png` | 128×64 |  | #727A44 | #909050 22%, #505030 17%, #90B070 12% | 40% |
| `textures/guns/m4Texture_3.png` | 128×64 |  | #7F7E7C | #909090 44%, #707070 32%, #B0B0B0 10% | 40% |
| `textures/guns/m4Texture_4.png` | 128×64 |  | #B67D10 | #F0B010 24%, #B07010 15%, #F0D010 11% | 40% |
| `textures/guns/m4_uq_Texture.png` | 128×64 |  | #2B2325 | #303030 47%, #101010 38%, #505050 4% | 49% |
| `textures/guns/mac10texture.png` | 128×64 |  | #555958 | #505050 58%, #303030 15%, #707070 9% | 33% |
| `textures/guns/mg2_texture.png` | 128×128 |  | #22272A | #303030 71%, #101010 22%, #505050 3% | 26% |
| `textures/guns/mibgun.png` | 128×128 |  | #F0F1F1 | #F0F0F0 82%, #B0B0B0 9%, #D0D0D0 7% | 100% |
| `textures/guns/minigun.png` | 128×128 |  | #2A2929 | #101010 70%, #707070 5%, #909090 3% | 100% |
| `textures/guns/mp5Texture.png` | 64×32 |  | #2A3A38 | #303030 46%, #305050 23%, #305030 15% | 30% |
| `textures/guns/neutrinogun.png` | 256×256 |  | #302E2D | #301010 43%, #101010 25%, #303030 19% | 17% |
| `textures/guns/nukerocketlauncher.png` | 128×64 |  | #241D0E | #101010 65%, #505030 5%, #303010 3% | 96% |
| `textures/guns/pistol.png` | 64×32 |  | #464646 | #505050 76%, #303030 23% | 12% |
| `textures/guns/pistol2Texture.png` | 64×32 |  | #494949 | #505050 96%, #303030 3% | 15% |
| `textures/guns/pistol3.png` | 128×64 |  | #252323 | #303030 42%, #101010 40%, #301010 6% | 20% |
| `textures/guns/powerHammer.png` | 64×64 |  | #2B2522 | #101010 51%, #503030 16%, #505050 6% | 100% |
| `textures/guns/powerHammerAdv.png` | 64×64 |  | #2A2421 | #101010 51%, #503030 16%, #303030 6% | 100% |
| `textures/guns/pulseRifle.png` | 128×64 |  | #333A3C | #101010 57%, #303030 12%, #D0F0F0 9% | 99% |
| `textures/guns/pulseRifle_1.png` | 128×64 |  | #2E2E2E | #101010 71%, #D0D0D0 9%, #303030 6% | 99% |
| `textures/guns/pulseRifle_2.png` | 128×64 |  | #080A06 | #101010 87%, #103010 10%, #303010 2% | 99% |
| `textures/guns/revolver.png` | 64×32 |  | #433D37 | #101010 54%, #909090 14%, #B0B0B0 12% | 100% |
| `textures/guns/rocketlauncher.png` | 128×64 |  | #272728 | #101010 59%, #303030 11%, #507070 5% | 96% |
| `textures/guns/sawedOff.png` | 64×32 |  | #524C3E | #101010 24%, #505030 21%, #B0B0B0 15% | 100% |
| `textures/guns/scar_texture.png` | 256×128 |  | #757559 | #B0B070 48%, #303030 19%, #101010 8% | 45% |
| `textures/guns/scar_texture_1.png` | 256×128 |  | #292929 | #303030 83%, #101010 11%, #505050 2% | 45% |
| `textures/guns/sonicshotgun.png` | 128×64 |  | #3C4447 | #303030 35%, #505050 18%, #101010 8% | 55% |
| `textures/guns/stielgranate.png` | 32×32 |  | #4F603E | #909050 40%, #305030 29%, #105030 17% | 24% |
| `textures/guns/thompson.png` | 64×64 |  | #271E15 | #101010 60%, #505050 13%, #705010 12% | 100% |
| `textures/guns/vector_texture.png` | 256×128 |  | #585842 | #303030 54%, #B0B070 34%, #101010 5% | 29% |
| `textures/guns/vector_texture_1.png` | 256×128 |  | #2A2929 | #303030 92%, #101010 6%, #505050 0% | 29% |
| `textures/items/40mmGrenade.png` | 32×32 |  | #856349 | #707050 9%, #505030 7%, #101010 6% | 38% |
| `textures/items/advBullets.png` | 64×64 |  | #6B8390 | #303030 11%, #90B0B0 10%, #B0D0D0 9% | 35% |
| `textures/items/antiGravPack.png` | 32×32 |  | #5C717B | #909090 15%, #707070 14%, #505050 13% | 58% |
| `textures/items/antiGravityCore.png` | 32×32 |  | #337D9E | #505050 19%, #1090D0 15%, #10B0D0 7% | 43% |
| `textures/items/bioMass.png` | 64×64 |  | #448B06 | #307010 37%, #509010 34%, #50B010 12% | 62% |
| `textures/items/bioTank.png` | 64×64 |  | #549257 | #909090 12%, #505050 10%, #B0B0B0 9% | 58% |
| `textures/items/bioTankEmpty.png` | 64×64 |  | #6E6E6E | #505050 25%, #909090 23%, #707070 17% | 58% |
| `textures/items/blockReaderTool.png` | 32×32 |  | #5E5E5F | #101010 20%, #707070 17%, #505050 14% | 62% |
| `textures/items/bucket_acid.png` | 16×16 |  | #7E755B | #303030 25%, #907010 20%, #D0D0D0 14% | 50% |
| `textures/items/bullets9mm.png` | 32×32 |  | #9EA370 | #F0F050 7%, #D0F0F0 5%, #D0B030 5% | 47% |
| `textures/items/camoNetPlacer0.png` | 32×32 |  | #586A43 | #707050 23%, #505030 19%, #507050 15% | 38% |
| `textures/items/camoNetPlacer1.png` | 32×32 |  | #72725F | #707050 23%, #909090 19%, #505030 17% | 38% |
| `textures/items/camoNetPlacer2.png` | 32×32 |  | #7E8147 | #707050 23%, #505030 18%, #B0B050 18% | 38% |
| `textures/items/carbonBarrel.png` | 32×32 |  | #202020 | #101010 50%, #303030 39%, #505050 10% | 32% |
| `textures/items/carbonFibers.png` | 32×32 |  | #454545 | #505050 53%, #303030 35%, #101010 7% | 28% |
| `textures/items/carbonGunstock.png` | 32×32 |  | #1B1B1B | #101010 79%, #303030 9%, #505050 7% | 35% |
| `textures/items/carbonPlate.png` | 32×32 |  | #575757 | #303030 25%, #505050 20%, #707070 20% | 65% |
| `textures/items/carbonReceiver.png` | 32×32 |  | #0B0B0B | #101010 94%, #303030 5% | 36% |
| `textures/items/circuitBoard.png` | 32×32 |  | #52721F | #107030 34%, #F0D030 7%, #307030 7% | 76% |
| `textures/items/circuitBoardElite.png` | 32×32 |  | #5C7F4E | #107030 26%, #107050 8%, #F0F0F0 7% | 77% |
| `textures/items/coil.png` | 32×32 |  | #8D5C38 | #B07030 14%, #D09050 14%, #905030 11% | 23% |
| `textures/items/combatKnife.png` | 96×96 |  | #2F3532 | #303030 57%, #505050 13%, #101010 9% | 7% |
| `textures/items/compressedAirTank.png` | 64×64 |  | #5B5D54 | #101010 31%, #303030 13%, #B0B0B0 9% | 42% |
| `textures/items/compressedAirTankEmpty.png` | 64×64 |  | #5C5D54 | #101010 31%, #303030 13%, #B0B0B0 10% | 39% |
| `textures/items/copperNugget.png` | 16×16 |  | #BC934A | #907010 29%, #705010 20%, #F0F0B0 11% | 13% |
| `textures/items/crowbar.png` | 32×32 |  | #713B3B | #703030 25%, #503030 24%, #905050 15% | 10% |
| `textures/items/cyberneticParts.png` | 64×64 |  | #8E847F | #F0F0F0 16%, #505050 13%, #D0D0D0 9% | 50% |
| `textures/items/electricEngine.png` | 64×64 |  | #333331 | #303030 48%, #101010 27%, #505050 15% | 56% |
| `textures/items/energyCell.png` | 64×64 |  | #624734 | #303030 29%, #101010 16%, #505050 10% | 65% |
| `textures/items/energyCellEmpty.png` | 64×64 |  | #4B4B39 | #303030 35%, #101010 21%, #505050 14% | 65% |
| `textures/items/enrichedUranium.png` | 16×16 |  | #5C7849 | #709050 28%, #305030 21%, #90B070 15% | 53% |
| `textures/items/flamethrowerTank.png` | 64×64 |  | #AF936C | #F0F0F0 8%, #F0F0D0 6%, #D0D0D0 6% | 58% |
| `textures/items/flamethrowerTankEmpty.png` | 64×64 |  | #A28C74 | #F0F0F0 8%, #D0D0D0 7%, #F0F0D0 6% | 58% |
| `textures/items/gasMask.png` | 32×32 |  | #2B3E2F | #305030 20%, #103010 17%, #505050 15% | 41% |
| `textures/items/gasMaskFilter.png` | 32×32 |  | #181C18 | #101010 59%, #303030 31%, #103010 8% | 60% |
| `textures/items/glider.png` | 32×32 |  | #644C40 | #705030 19%, #303030 13%, #503030 10% | 61% |
| `textures/items/gliderBackback.png` | 32×32 |  | #5D362A | #705030 31%, #503030 16%, #703030 15% | 37% |
| `textures/items/gliderWing.png` | 32×32 |  | #818071 | #303030 27%, #B0B090 18%, #B0B0B0 17% | 59% |
| `textures/items/goldWire.png` | 32×32 |  | #BDAB02 | #D0B010 57%, #B09010 13%, #B0B010 12% | 32% |
| `textures/items/handgun.png` | 32×32 |  | #4A4946 | #303030 25%, #707070 21%, #505050 21% | 35% |
| `textures/items/heavyCloth.png` | 32×32 |  | #543C1F | #303010 18%, #503010 15%, #705030 15% | 47% |
| `textures/items/highCalibreBullets.png` | 32×32 |  | #958A38 | #303010 8%, #D0B030 7%, #101010 6% | 41% |
| `textures/items/ingotBronze.png` | 16×16 |  | #5E3707 | #503010 20%, #B07010 17%, #101010 16% | 54% |
| `textures/items/ingotCopper.png` | 16×16 |  | #9B8232 | #F0D070 15%, #503010 13%, #907010 11% | 54% |
| `textures/items/ingotLead.png` | 16×16 |  | #40505C | #305050 16%, #303030 15%, #505070 15% | 54% |
| `textures/items/ingotObsidianSteel.png` | 16×16 |  | #171620 | #101010 69%, #303050 18%, #101030 6% | 51% |
| `textures/items/ingotSteel.png` | 16×16 |  | #565656 | #101010 31%, #707070 18%, #B0B0B0 13% | 54% |
| `textures/items/ingotTin.png` | 16×16 |  | #6F9792 | #305050 23%, #70B0B0 14%, #509090 10% | 54% |
| `textures/items/ingotTitanium.png` | 16×16 |  | #929292 | #505050 27%, #F0F0F0 24%, #B0B0B0 17% | 51% |
| `textures/items/ironBarrel.png` | 32×32 |  | #949494 | #707070 21%, #505050 15%, #B0B0B0 14% | 32% |
| `textures/items/ironReceiver.png` | 32×32 |  | #707070 | #909090 42%, #505050 29%, #707070 23% | 36% |
| `textures/items/jetpack.png` | 64×64 |  | #4F4D4B | #505050 26%, #707070 21%, #101010 19% | 30% |
| `textures/items/jumppack.png` | 32×32 |  | #5F5D57 | #707070 28%, #909090 20%, #303030 16% | 57% |
| `textures/items/laserBarrel.png` | 32×32 |  | #C3B9A8 | #B0B090 21%, #D0D0D0 21%, #909070 14% | 32% |
| `textures/items/laserFocus.png` | 64×64 |  | #B47E7E | #F0F0F0 21%, #D0D0D0 6%, #501010 6% | 70% |
| `textures/items/machineStackUpgrade.png` | 32×32 |  | #365113 | #103010 18%, #105010 17%, #101010 10% | 76% |
| `textures/items/mechanicalParts.png` | 32×32 |  | #747474 | #707070 30%, #505050 24%, #909090 15% | 31% |
| `textures/items/mechanicalPartsT2.png` | 32×32 |  | #513A58 | #503050 25%, #705070 24%, #303030 15% | 31% |
| `textures/items/mechanicalPartsT3.png` | 32×32 |  | #211B10 | #101010 55%, #301010 13%, #303010 9% | 31% |
| `textures/items/medkit_0.png` | 32×32 |  | #C2C2C2 | #D0D0D0 44%, #F0F0F0 23%, #909090 12% | 44% |
| `textures/items/medkit_1.png` | 32×32 |  | #934D4B | #B03030 30%, #505030 17%, #903030 12% | 50% |
| `textures/items/metalDoor.png` | 32×32 |  | #6F756B | #707070 30%, #909090 16%, #505050 12% | 73% |
| `textures/items/minigunDrum.png` | 64×64 |  | #767674 | #B0B0B0 18%, #707070 16%, #303030 15% | 63% |
| `textures/items/minigunDrum_empty.png` | 64×64 |  | #747474 | #B0B0B0 19%, #707070 17%, #303030 16% | 60% |
| `textures/items/nethercharge.png` | 32×32 |  | #B99A60 | #707070 15%, #909090 10%, #B0B0B0 7% | 66% |
| `textures/items/nightVisionGoggles.png` | 32×32 |  | #0D2011 | #101010 75%, #107030 8%, #303030 6% | 28% |
| `textures/items/nuclearPowerCell.png` | 64×64 |  | #5A5435 | #101010 31%, #303030 11%, #505050 9% | 48% |
| `textures/items/nuclearPowerCellDepleted.png` | 64×64 |  | #504938 | #101010 33%, #303030 12%, #505050 11% | 48% |
| `textures/items/nuggetIron.png` | 16×16 |  | #8F8F8F | #707070 38%, #F0F0F0 29%, #303030 20% | 13% |
| `textures/items/nuggetLead.png` | 16×16 |  | #455164 | #303050 29%, #707090 29%, #303030 20% | 13% |
| `textures/items/nuggetSteel.png` | 16×16 |  | #4B4B4B | #101010 50%, #B0B0B0 29%, #909090 11% | 13% |
| `textures/items/obsidianSteelBarrel.png` | 32×32 |  | #565764 | #303050 14%, #B0B0B0 14%, #505070 14% | 32% |
| `textures/items/obsidianSteelReceiver.png` | 32×32 |  | #2A252E | #303030 64%, #301030 18%, #101010 11% | 36% |
| `textures/items/oreClusterScanner.png` | 32×32 |  | #3E553C | #505050 41%, #303030 15%, #707070 12% | 48% |
| `textures/items/oreDrillLargeCarbon.png` | 32×32 |  | #1A1A1A | #101010 67%, #303030 32% | 33% |
| `textures/items/oreDrillLargeObsidianSteel.png` | 32×32 |  | #252130 | #303030 34%, #101010 23%, #303050 14% | 33% |
| `textures/items/oreDrillLargeSteel.png` | 32×32 |  | #9B9B9B | #B0B0B0 32%, #909090 19%, #D0D0D0 13% | 33% |
| `textures/items/oreDrillMediumCarbon.png` | 32×32 |  | #1B1B1B | #101010 69%, #303030 20%, #505050 8% | 33% |
| `textures/items/oreDrillMediumObsidianSteel.png` | 32×32 |  | #26323E | #303030 19%, #103030 15%, #101010 15% | 33% |
| `textures/items/oreDrillMediumSteel.png` | 32×32 |  | #7B797B | #707070 20%, #505050 19%, #D0D0D0 16% | 33% |
| `textures/items/oreDrillSmallCarbon.png` | 32×32 |  | #202020 | #101010 57%, #303030 29%, #505050 12% | 15% |
| `textures/items/oreDrillSmallObsidianSteel.png` | 32×32 |  | #2D272F | #303030 44%, #101010 27%, #505050 14% | 15% |
| `textures/items/oreDrillSmallSteel.png` | 32×32 |  | #6C6A6B | #707070 25%, #303030 19%, #505050 17% | 15% |
| `textures/items/oreTitanium.png` | 32×32 |  | #7E898A | #707070 10%, #505050 8%, #F0F0F0 8% | 39% |
| `textures/items/oxygenMask.png` | 16×16 |  | #818181 | #707070 43%, #909090 32%, #B0B0B0 15% | 33% |
| `textures/items/pdwMag.png` | 64×64 |  | #5A666F | #507070 21%, #707070 16%, #505050 14% | 33% |
| `textures/items/pdwMagEmpty.png` | 64×64 |  | #545F68 | #507070 22%, #707070 17%, #505050 16% | 30% |
| `textures/items/pistolMag9mm.png` | 32×32 |  | #3B3A34 | #303030 67%, #101010 11%, #505050 5% | 40% |
| `textures/items/pistolMag9mmEmpty.png` | 32×32 |  | #2B2B2B | #303030 69%, #101010 21%, #505050 5% | 40% |
| `textures/items/plasticGunstock.png` | 32×32 |  | #303031 | #303030 60%, #101010 24%, #505050 13% | 32% |
| `textures/items/plasticSheet.png` | 32×32 |  | #DBDBDB | #F0F0F0 61%, #D0D0D0 23%, #909090 13% | 65% |
| `textures/items/plateBronze.png` | 32×32 |  | #774724 | #B07030 18%, #301010 13%, #B05030 13% | 65% |
| `textures/items/plateBronzeOLD.png` | 32×32 |  | #593E13 | #705010 33%, #503010 31%, #505010 15% | 76% |
| `textures/items/plateCopper.png` | 32×32 |  | #B19357 | #F0D070 23%, #F0D090 14%, #F0B050 8% | 65% |
| `textures/items/plateCopperOLD.png` | 32×32 |  | #734D13 | #705010 43%, #905010 20%, #705030 9% | 76% |
| `textures/items/plateIron.png` | 32×32 |  | #8C8B8B | #B0B0B0 35%, #D0D0D0 17%, #909090 14% | 65% |
| `textures/items/plateIronOLD.png` | 32×32 |  | #686868 | #707070 61%, #505050 21%, #909090 12% | 76% |
| `textures/items/plateLead.png` | 32×32 |  | #4C446F | #7070B0 33%, #101010 21%, #705090 9% | 65% |
| `textures/items/plateObsidianSteel.png` | 32×32 |  | #3A2A3E | #101010 33%, #503050 28%, #505070 13% | 65% |
| `textures/items/plateSteel.png` | 32×32 |  | #777777 | #909090 21%, #707070 16%, #101010 14% | 65% |
| `textures/items/plateSteelOLD.png` | 32×32 |  | #4C4C4C | #505050 78%, #303030 11%, #707070 7% | 76% |
| `textures/items/plateTin.png` | 32×32 |  | #879C9D | #B0D0D0 38%, #303030 13%, #90B0B0 13% | 65% |
| `textures/items/plateTinOLD.png` | 32×32 |  | #6A717B | #707070 32%, #707090 17%, #709090 10% | 76% |
| `textures/items/plateTitanium.png` | 32×32 |  | #9C9C9C | #F0F0F0 29%, #D0D0D0 20%, #B0B0B0 15% | 65% |
| `textures/items/powerPlating.png` | 32×32 |  | #78736E | #303030 14%, #909090 13%, #B0B0B0 12% | 65% |
| `textures/items/pumpMechanism.png` | 64×64 |  | #7D7360 | #505050 16%, #B0B0B0 15%, #707070 14% | 56% |
| `textures/items/quartz_rod.png` | 16×16 |  | #C2C2C2 | #B0B0B0 54%, #D0D0D0 39%, #F0F0F0 5% | 19% |
| `textures/items/radEmitter.png` | 64×64 |  | #736A4C | #303030 19%, #707070 7%, #505050 7% | 51% |
| `textures/items/rawPlastic.png` | 32×32 |  | #C0C0C0 | #D0D0D0 37%, #F0F0F0 22%, #B0B0B0 19% | 36% |
| `textures/items/rawRubber.png` | 32×32 |  | #747561 | #909070 14%, #D0D0B0 11%, #101010 11% | 23% |
| `textures/items/rcHeatRay.png` | 64×64 |  | #605F57 | #303030 22%, #505050 20%, #101010 16% | 58% |
| `textures/items/rifleBullets.png` | 32×32 |  | #AAA143 | #F0F050 12%, #F0F070 10%, #101010 6% | 45% |
| `textures/items/rifleBulletsStack.png` | 32×32 |  | #8E8336 | #505010 7%, #B09030 7%, #303010 6% | 84% |
| `textures/items/rubberBar.png` | 32×32 |  | #2A2516 | #303010 85%, #101010 13%, #301010 0% | 65% |
| `textures/items/scubaTanks.png` | 32×32 |  | #606060 | #909090 21%, #101010 21%, #B0B0B0 15% | 72% |
| `textures/items/shotgunShell.png` | 32×32 |  | #957B6A | #905050 8%, #F0F0F0 8%, #B0B070 6% | 41% |
| `textures/items/smgMag.png` | 32×32 |  | #494A49 | #505050 34%, #707070 16%, #303030 15% | 29% |
| `textures/items/smgMagEmpty.png` | 32×32 |  | #444447 | #505050 36%, #303030 18%, #707070 15% | 28% |
| `textures/items/sonicEmitter.png` | 64×64 |  | #4F5154 | #303030 35%, #101010 14%, #505050 14% | 46% |
| `textures/items/spawn_egg.png` | 32×32 |  | #898989 | #B0B0B0 23%, #909090 21%, #707070 14% | 50% |
| `textures/items/spawn_egg_overlay.png` | 32×32 |  | #939393 | #505050 55%, #F0F0F0 35%, #909090 8% | 16% |
| `textures/items/steamArmorPlate.png` | 32×32 |  | #50442E | #707050 26%, #101010 21%, #705030 13% | 65% |
| `textures/items/steelReceiver.png` | 32×32 |  | #494949 | #505050 42%, #303030 31%, #707070 22% | 36% |
| `textures/items/stoneBarrel.png` | 32×32 |  | #767676 | #505050 22%, #303030 19%, #909090 18% | 32% |
| `textures/items/stonebullets.png` | 32×32 |  | #595959 | #707070 28%, #303030 25%, #505050 19% | 53% |
| `textures/items/structureScanTool.png` | 32×32 |  | #5A4C4C | #101010 36%, #909090 11%, #F0F0F0 7% | 50% |
| `textures/items/structureTestTool.png` | 32×32 |  | #534444 | #101010 29%, #303030 12%, #909090 9% | 62% |
| `textures/items/t1_combat_boots.png` | 32×32 |  | #2A2724 | #101010 49%, #303030 24%, #303010 7% | 33% |
| `textures/items/t1_combat_chestplate.png` | 32×32 |  | #413221 | #303010 21%, #101010 14%, #503030 13% | 62% |
| `textures/items/t1_combat_helmet.png` | 32×32 |  | #8D8B80 | #B0B0B0 28%, #D0D0B0 19%, #505050 12% | 35% |
| `textures/items/t1_combat_leggings.png` | 32×32 |  | #493826 | #303010 29%, #503030 24%, #505030 20% | 46% |
| `textures/items/t1_miner_boots.png` | 32×32 |  | #464438 | #303030 54%, #909090 14%, #101010 10% | 46% |
| `textures/items/t1_miner_chestplate.png` | 32×32 |  | #383672 | #303070 34%, #303090 18%, #505090 15% | 44% |
| `textures/items/t1_miner_helmet.png` | 32×32 |  | #BCB312 | #F0D010 18%, #D0D010 18%, #F0F010 17% | 27% |
| `textures/items/t1_miner_leggings.png` | 32×32 |  | #363473 | #303070 38%, #303090 18%, #505090 17% | 45% |
| `textures/items/t1_scout_boots.png` | 32×32 |  | #393029 | #303030 38%, #503030 22%, #301010 8% | 35% |
| `textures/items/t1_scout_chestplate.png` | 32×32 |  | #52452F | #303010 26%, #707050 13%, #503010 10% | 56% |
| `textures/items/t1_scout_helmet.png` | 32×32 |  | #6C624F | #707050 24%, #909070 12%, #907050 11% | 44% |
| `textures/items/t1_scout_leggings.png` | 32×32 |  | #72674D | #907050 28%, #707050 27%, #909070 14% | 43% |
| `textures/items/t2_beret.png` | 32×32 |  | #3D0000 | #501010 33%, #101010 23%, #301010 23% | 22% |
| `textures/items/t2_combat_boots.png` | 32×32 |  | #32302D | #101010 34%, #303030 27%, #505050 12% | 35% |
| `textures/items/t2_combat_chestplate.png` | 32×32 |  | #262922 | #101010 40%, #303030 20%, #505050 10% | 63% |
| `textures/items/t2_combat_helmet.png` | 32×32 |  | #484D45 | #303030 22%, #707070 21%, #505050 20% | 38% |
| `textures/items/t2_combat_leggings.png` | 32×32 |  | #454640 | #303030 36%, #505050 36%, #707050 5% | 47% |
| `textures/items/t2_commando_boots.png` | 32×32 |  | #141414 | #101010 77%, #303030 22% | 40% |
| `textures/items/t2_commando_chestplate.png` | 32×32 |  | #141414 | #101010 80%, #303030 16%, #505050 2% | 70% |
| `textures/items/t2_commando_helmet.png` | 32×32 |  | #303030 | #101010 39%, #303030 36%, #505050 9% | 49% |
| `textures/items/t2_commando_leggings.png` | 32×32 |  | #191919 | #101010 66%, #303030 29%, #707070 2% | 47% |
| `textures/items/t2_riot_boots.png` | 32×32 |  | #2B2B20 | #101010 38%, #303030 28%, #505030 11% | 35% |
| `textures/items/t2_riot_chestplate.png` | 32×32 |  | #40341F | #503010 42%, #303010 12%, #301010 7% | 70% |
| `textures/items/t2_riot_helmet.png` | 32×32 |  | #3E3F30 | #101010 28%, #303030 17%, #707050 17% | 38% |
| `textures/items/t2_riot_leggings.png` | 32×32 |  | #45422B | #505030 41%, #303030 26%, #303010 13% | 47% |
| `textures/items/t3_combat_boots.png` | 32×32 |  | #717E88 | #101010 10%, #303030 9%, #D0F0F0 9% | 38% |
| `textures/items/t3_combat_chestplate.png` | 32×32 |  | #7D868D | #F0F0F0 16%, #103030 15%, #303030 13% | 67% |
| `textures/items/t3_combat_helmet.png` | 32×32 |  | #6F7F92 | #F0F0F0 14%, #101030 11%, #B0D0D0 6% | 45% |
| `textures/items/t3_combat_leggings.png` | 32×32 |  | #303A42 | #303030 38%, #103030 23%, #101030 9% | 46% |
| `textures/items/t3_exo_boots.png` | 32×32 |  | #87888C | #B0B0B0 13%, #D0D0D0 10%, #909090 8% | 42% |
| `textures/items/t3_exo_chestplate.png` | 32×32 |  | #8F959C | #F0F0F0 16%, #103030 9%, #303030 9% | 67% |
| `textures/items/t3_exo_helmet.png` | 32×32 |  | #6E7B8D | #F0F0F0 14%, #101030 11%, #103070 5% | 45% |
| `textures/items/t3_exo_leggings.png` | 32×32 |  | #5D6166 | #303030 29%, #B0B0B0 13%, #909090 11% | 50% |
| `textures/items/t3_miner_boots.png` | 32×32 |  | #2E2210 | #101010 63%, #303030 9%, #705010 8% | 28% |
| `textures/items/t3_miner_chestplate.png` | 32×32 |  | #372915 | #101010 62%, #303030 10%, #503030 4% | 54% |
| `textures/items/t3_miner_helmet.png` | 32×32 |  | #282925 | #101010 56%, #305070 13%, #303030 5% | 32% |
| `textures/items/t3_miner_leggings.png` | 32×32 |  | #443014 | #101010 49%, #B07010 11%, #505050 6% | 43% |
| `textures/items/tacticalMask.png` | 16×16 |  | #1D1111 | #101010 81%, #701010 8%, #501010 7% | 67% |
| `textures/items/tacticalNuclearWarhead.png` | 32×32 |  | #4F5427 | #303030 41%, #505050 23%, #101010 14% | 34% |
| `textures/items/testItem.png` | 32×32 |  | #000000 | #101010 100% | 28% |
| `textures/items/tgx.png` | 32×32 |  | #668EAC | #D0F0F0 7%, #90D0F0 7%, #305070 7% | 55% |
| `textures/items/treatedLeather.png` | 32×32 |  | #192912 | #103010 85%, #305010 8%, #101010 4% | 51% |
| `textures/items/turretArmorCarbon.png` | 32×32 |  | #333333 | #101010 34%, #303030 31%, #505050 20% | 76% |
| `textures/items/turretArmorIron.png` | 32×32 |  | #7B7B7B | #B0B0B0 23%, #303030 16%, #D0D0D0 15% | 76% |
| `textures/items/turretArmorObsidianSteel.png` | 32×32 |  | #4A444F | #101010 28%, #303030 12%, #705070 9% | 76% |
| `textures/items/turretArmorSteel.png` | 32×32 |  | #5A5A5A | #101010 26%, #303030 17%, #707070 17% | 76% |
| `textures/items/uvemitter.png` | 64×64 |  | #55456A | #303030 27%, #9070B0 25%, #101010 14% | 21% |
| `textures/items/wireCopper.png` | 32×32 |  | #AC8C4B | #B09050 67%, #907050 10%, #907030 5% | 32% |
| `textures/items/woodStock.png` | 32×32 |  | #5A5039 | #505030 22%, #303010 13%, #909070 13% | 40% |
| `textures/items/yellowcake.png` | 16×16 |  | #8E9625 | #B0B010 13%, #707010 9%, #303010 8% | 45% |

## Sounds

| Sound | Duration | Sample rate | Channels | sounds.json event [category] |
|---|---|---|---|---|
| `sounds/test.ogg` | 1.23 s | 44100 Hz | mono | test [master] |
| `sounds/blocks/metaldoor_open_a.ogg` | 0.96 s | 32000 Hz | mono | blocks.metaldoorOpen [block] |
| `sounds/blocks/metaldoor_open_b.ogg` | 0.96 s | 32000 Hz | mono | blocks.metaldoorOpen [block] |
| `sounds/blocks/metaldoor_open_c.ogg` | 0.83 s | 32000 Hz | mono | blocks.metaldoorOpen [block] |
| `sounds/effects/C4Charge_Init.ogg` | 0.88 s | 44100 Hz | mono | c4charge.init [block] |
| `sounds/effects/C4Charge_Plant.ogg` | 1.50 s | 44100 Hz | mono | c4charge.plant [block] |
| `sounds/effects/C4Charge_Tick.ogg` | 0.70 s | 44100 Hz | mono | c4charge.tick [block] |
| `sounds/effects/DemoCharge_Init.ogg` | 1.91 s | 44100 Hz | mono | tntcharge.init [block] |
| `sounds/effects/DemoCharge_Plant.ogg` | 0.76 s | 44100 Hz | mono | tntcharge.plant [block] |
| `sounds/effects/DemoCharge_Tick.ogg` | 1.00 s | 44100 Hz | mono | tntcharge.tick [block] |
| `sounds/effects/biodeath.ogg` | 3.99 s | 32000 Hz | mono | effects.biodeath [hostile] |
| `sounds/effects/bullet_impact_a.ogg` | 0.98 s | 22050 Hz | mono | effects.bulletimpact [player] |
| `sounds/effects/bullet_impact_b.ogg` | 0.98 s | 22050 Hz | mono | effects.bulletimpact [player] |
| `sounds/effects/bullet_impact_c.ogg` | 0.98 s | 22050 Hz | mono | effects.bulletimpact [player] |
| `sounds/effects/bullet_impact_d.ogg` | 0.98 s | 22050 Hz | mono | effects.bulletimpact [player] |
| `sounds/effects/bullet_impact_e.ogg` | 0.98 s | 22050 Hz | mono | effects.bulletimpact [player] |
| `sounds/effects/disintegrate.ogg` | 5.29 s | 32000 Hz | mono | effects.disintegrate [hostile] |
| `sounds/effects/explosion1_a.ogg` | 2.47 s | 44100 Hz | mono | effects.explosion1 [master] |
| `sounds/effects/explosion1_b.ogg` | 1.75 s | 44100 Hz | mono | effects.explosion1 [master] |
| `sounds/effects/explosion1_c.ogg` | 1.68 s | 44100 Hz | mono | effects.explosion1 [master] |
| `sounds/effects/explosion1_d.ogg` | 1.21 s | 44100 Hz | mono | effects.explosion1 [master] |
| `sounds/effects/explosion1_e.ogg` | 2.85 s | 44100 Hz | mono | effects.explosion1 [master] |
| `sounds/effects/gore_a.ogg` | 2.99 s | 44100 Hz | mono | effects.gore [hostile] |
| `sounds/effects/gore_b.ogg` | 2.99 s | 44100 Hz | mono | effects.gore [hostile] |
| `sounds/effects/gore_c.ogg` | 2.99 s | 44100 Hz | mono | effects.gore [hostile] |
| `sounds/effects/nukeexplosion.ogg` | 13.77 s | 44100 Hz | mono | effects.nukeexplosion [master] |
| `sounds/effects/steamarmor_jump_a.ogg` | 1.21 s | 44100 Hz | mono | effects.steamarmorJump [player] |
| `sounds/effects/steamarmor_jump_b.ogg` | 1.21 s | 44100 Hz | mono | effects.steamarmorJump [player] |
| `sounds/effects/steamarmor_jump_c.ogg` | 1.21 s | 44100 Hz | mono | effects.steamarmorJump [player] |
| `sounds/effects/steamarmor_jump_d.ogg` | 1.21 s | 44100 Hz | mono | effects.steamarmorJump2 [player] |
| `sounds/guns/MIB_blaster_reload.ogg` | 2.29 s | 44100 Hz | mono | guns.MIBblasterReload [player] |
| `sounds/guns/MIB_blaster_reload_akimbo.ogg` | 2.99 s | 44100 Hz | mono | guns.MIBblasterReloadAkimbo [player] |
| `sounds/guns/MIB_blaster_shot_a.ogg` | 2.44 s | 44100 Hz | mono | guns.MIBblasterFire [player] |
| `sounds/guns/MIB_blaster_shot_b.ogg` | 2.44 s | 44100 Hz | mono | guns.MIBblasterFire [player] |
| `sounds/guns/MIB_blaster_shot_c.ogg` | 2.34 s | 44100 Hz | mono | guns.MIBblasterFire [player] |
| `sounds/guns/SCAR_reload.ogg` | 3.53 s | 48000 Hz | mono | guns.scarReload [player] |
| `sounds/guns/SCAR_shot_a.ogg` | 1.60 s | 44100 Hz | mono | guns.scarFire [player] |
| `sounds/guns/SCAR_shot_b.ogg` | 1.60 s | 44100 Hz | mono | guns.scarFire [player] |
| `sounds/guns/SCAR_shot_c.ogg` | 1.60 s | 44100 Hz | mono | guns.scarFire [player] |
| `sounds/guns/ak47_reload.ogg` | 2.78 s | 48000 Hz | mono | guns.ak47Reload [player] |
| `sounds/guns/ak47_shot_a.ogg` | 1.50 s | 44100 Hz | mono | guns.ak47Fire [player] |
| `sounds/guns/ak47_shot_b.ogg` | 1.50 s | 44100 Hz | mono | guns.ak47Fire [player] |
| `sounds/guns/ak47_shot_c.ogg` | 1.50 s | 44100 Hz | mono | guns.ak47Fire [player] |
| `sounds/guns/alienblaster_reload.ogg` | 2.97 s | 44100 Hz | mono | guns.alienblasterReload [player] |
| `sounds/guns/alienblaster_shot_a.ogg` | 2.65 s | 44100 Hz | mono | guns.alienblasterFire [player] |
| `sounds/guns/alienblaster_shot_b.ogg` | 2.76 s | 44100 Hz | mono | guns.alienblasterFire [player] |
| `sounds/guns/alienblaster_shot_c.ogg` | 2.80 s | 44100 Hz | mono | guns.alienblasterFire [player] |
| `sounds/guns/ar1_reload.ogg` | 2.09 s | 44100 Hz | mono | guns.assaultRifleReload [player] |
| `sounds/guns/ar1_shot_a.ogg` | 1.23 s | 44100 Hz | mono | guns.assaultRifleFire [player] |
| `sounds/guns/ar1_shot_b.ogg` | 1.40 s | 44100 Hz | mono | guns.assaultRifleFire [player] |
| `sounds/guns/as50_reload.ogg` | 5.00 s | 48000 Hz | mono | guns.as50Reload [player] |
| `sounds/guns/as50_shot_a.ogg` | 4.93 s | 44100 Hz | mono | guns.as50Fire [player] |
| `sounds/guns/as50_shot_b.ogg` | 4.93 s | 44100 Hz | mono | guns.as50Fire [player] |
| `sounds/guns/aug_reload.ogg` | 4.03 s | 48000 Hz | mono | guns.augReload [player] |
| `sounds/guns/aug_shot_a.ogg` | 2.44 s | 44100 Hz | mono | guns.augFire [player] |
| `sounds/guns/aug_shot_b.ogg` | 2.44 s | 44100 Hz | mono | guns.augFire [player] |
| `sounds/guns/aug_shot_c.ogg` | 2.44 s | 44100 Hz | mono | guns.augFire [player] |
| `sounds/guns/beamgun_loop_a.ogg` | 1.01 s | 48000 Hz | mono | guns.beamgunFire [player] |
| `sounds/guns/beamgun_loop_b.ogg` | 1.01 s | 48000 Hz | mono | guns.beamgunFire [player] |
| `sounds/guns/beamgun_loop_c.ogg` | 1.01 s | 48000 Hz | mono | guns.beamgunFire [player] |
| `sounds/guns/beamgun_loop_d.ogg` | 1.01 s | 48000 Hz | mono | guns.beamgunFire [player] |
| `sounds/guns/beamgun_loop_e.ogg` | 1.01 s | 48000 Hz | mono | guns.beamgunFire [player] |
| `sounds/guns/beamgun_loop_f.ogg` | 1.01 s | 48000 Hz | mono | guns.beamgunFire [player] |
| `sounds/guns/beamgun_reload.ogg` | 2.99 s | 48000 Hz | mono | (no sounds.json event) |
| `sounds/guns/beamgun_start_a.ogg` | 1.31 s | 48000 Hz | mono | guns.beamgunStart [player] |
| `sounds/guns/beamgun_start_b.ogg` | 1.31 s | 48000 Hz | mono | guns.beamgunStart [player] |
| `sounds/guns/beamgun_start_c.ogg` | 1.83 s | 48000 Hz | mono | guns.beamgunStart [player] |
| `sounds/guns/biogun_impact_a.ogg` | 1.58 s | 48000 Hz | mono | effects.biogunImpact [player] |
| `sounds/guns/biogun_impact_b.ogg` | 1.58 s | 48000 Hz | mono | effects.biogunImpact [player] |
| `sounds/guns/biogun_impact_c.ogg` | 1.58 s | 48000 Hz | mono | effects.biogunImpact [player] |
| `sounds/guns/biogun_reload.ogg` | 3.42 s | 48000 Hz | mono | guns.biogunReload [player] |
| `sounds/guns/biogun_shot_a.ogg` | 1.58 s | 48000 Hz | mono | guns.biogunFire [player] |
| `sounds/guns/biogun_shot_b.ogg` | 1.58 s | 48000 Hz | mono | guns.biogunFire [player] |
| `sounds/guns/biogun_shot_c.ogg` | 1.58 s | 48000 Hz | mono | guns.biogunFire [player] |
| `sounds/guns/blasterrifle_shot_a.ogg` | 0.83 s | 44100 Hz | mono | guns.blasterrifleFire [player] |
| `sounds/guns/blasterrifle_shot_b.ogg` | 0.77 s | 44100 Hz | mono | guns.blasterrifleFire [player] |
| `sounds/guns/blasterrifle_shot_c.ogg` | 0.77 s | 44100 Hz | mono | guns.blasterrifleFire [player] |
| `sounds/guns/boltaction_rechamber.ogg` | 1.03 s | 44100 Hz | mono | guns.boltactionRechamber [player] |
| `sounds/guns/boltaction_reload.ogg` | 1.94 s | 44100 Hz | mono | guns.boltactionReload [player] |
| `sounds/guns/boltaction_shot_a.ogg` | 1.98 s | 44100 Hz | mono | guns.boltactionFire [player] |
| `sounds/guns/boltaction_shot_b.ogg` | 1.98 s | 44100 Hz | mono | guns.boltactionFire [player] |
| `sounds/guns/boltaction_shot_c.ogg` | 1.98 s | 44100 Hz | mono | guns.boltactionFire [player] |
| `sounds/guns/cdb_shot_a.ogg` | 2.00 s | 44100 Hz | mono | guns.cyberdemonblasterFire [player] |
| `sounds/guns/cdb_shot_b.ogg` | 2.00 s | 44100 Hz | mono | guns.cyberdemonblasterFire [player] |
| `sounds/guns/cdb_shot_c.ogg` | 2.00 s | 44100 Hz | mono | guns.cyberdemonblasterFire [player] |
| `sounds/guns/cdb_shot_d.ogg` | 2.00 s | 44100 Hz | mono | guns.cyberdemonblasterFire [player] |
| `sounds/guns/chainsaw_hit_a.ogg` | 1.34 s | 44100 Hz | mono | guns.chainsawHit [player] |
| `sounds/guns/chainsaw_hit_b.ogg` | 1.34 s | 44100 Hz | mono | guns.chainsawHit [player] |
| `sounds/guns/chainsaw_hit_c.ogg` | 1.34 s | 44100 Hz | mono | guns.chainsawHit [player] |
| `sounds/guns/chainsaw_hit_d.ogg` | 1.34 s | 44100 Hz | mono | guns.chainsawHit [player] |
| `sounds/guns/chainsaw_hit_e.ogg` | 1.34 s | 44100 Hz | mono | guns.chainsawHit [player] |
| `sounds/guns/chainsaw_hit_f.ogg` | 1.34 s | 44100 Hz | mono | guns.chainsawHit [player] |
| `sounds/guns/chainsaw_loop_a.ogg` | 1.89 s | 32000 Hz | mono | guns.chainsawLoop [player] |
| `sounds/guns/chainsaw_loop_b.ogg` | 1.89 s | 32000 Hz | mono | guns.chainsawLoop [player] |
| `sounds/guns/chainsaw_loop_c.ogg` | 1.89 s | 32000 Hz | mono | guns.chainsawLoop [player] |
| `sounds/guns/chainsaw_loop_d.ogg` | 0.86 s | 32000 Hz | mono | (no sounds.json event) |
| `sounds/guns/chainsaw_loopend.ogg` | 0.86 s | 32000 Hz | mono | guns.chainsawLoopEnd [player] |
| `sounds/guns/chainsaw_loopstart.ogg` | 0.86 s | 32000 Hz | mono | guns.chainsawLoopStart [master] |
| `sounds/guns/chainsaw_swing_a.ogg` | 1.28 s | 44100 Hz | mono | guns.chainsawSwing [player] |
| `sounds/guns/chainsaw_swing_b.ogg` | 1.62 s | 44100 Hz | mono | guns.chainsawSwing [player] |
| `sounds/guns/chainsaw_swing_c.ogg` | 1.10 s | 44100 Hz | mono | guns.chainsawSwing [player] |
| `sounds/guns/combatshotgun_pump.ogg` | 0.89 s | 44100 Hz | mono | guns.combatshotgunPump [player] |
| `sounds/guns/combatshotgun_reload.ogg` | 2.95 s | 44100 Hz | mono | guns.combatshotgunReload [player] |
| `sounds/guns/combatshotgun_shot_a.ogg` | 2.10 s | 48000 Hz | stereo | guns.combatshotgunFire [player] |
| `sounds/guns/combatshotgun_shot_b.ogg` | 2.32 s | 48000 Hz | stereo | guns.combatshotgunFire [player] |
| `sounds/guns/combatshotgun_shot_c.ogg` | 3.14 s | 48000 Hz | stereo | guns.combatshotgunFire [player] |
| `sounds/guns/flamethrower_reload.ogg` | 2.21 s | 44100 Hz | mono | guns.flamethrowerReload [player] |
| `sounds/guns/flamethrower_shot_a.ogg` | 1.30 s | 44100 Hz | mono | guns.flamethrowerFire [player] |
| `sounds/guns/flamethrower_shot_b.ogg` | 1.30 s | 44100 Hz | mono | guns.flamethrowerFire [player] |
| `sounds/guns/flamethrower_shot_c.ogg` | 1.10 s | 44100 Hz | mono | guns.flamethrowerFire [player] |
| `sounds/guns/flamethrower_shot_d.ogg` | 1.10 s | 44100 Hz | mono | guns.flamethrowerFire [player] |
| `sounds/guns/flamethrower_shot_e.ogg` | 1.10 s | 44100 Hz | mono | guns.flamethrowerFire [player] |
| `sounds/guns/flamethrower_start_a.ogg` | 1.00 s | 44100 Hz | mono | guns.flamethrowerStart [player] |
| `sounds/guns/goldenrevolver_shot_a.ogg` | 3.49 s | 44100 Hz | mono | guns.goldenrevolverFire [player] |
| `sounds/guns/goldenrevolver_shot_b.ogg` | 3.02 s | 44100 Hz | mono | guns.goldenrevolverFire [player] |
| `sounds/guns/goldenrevolver_shot_c.ogg` | 3.02 s | 44100 Hz | mono | guns.goldenrevolverFire [player] |
| `sounds/guns/grenade_pin_a.ogg` | 0.77 s | 48000 Hz | mono | guns.grenade_pin [player] |
| `sounds/guns/grenade_pin_b.ogg` | 0.77 s | 48000 Hz | mono | guns.grenade_pin [player] |
| `sounds/guns/grenade_throw_a.ogg` | 0.79 s | 48000 Hz | mono | guns.grenade_throw [player] |
| `sounds/guns/grenade_throw_b.ogg` | 0.77 s | 48000 Hz | mono | guns.grenade_throw [player] |
| `sounds/guns/grenade_throw_c.ogg` | 0.88 s | 48000 Hz | mono | guns.grenade_throw [player] |
| `sounds/guns/grenadelauncher_reload.ogg` | 4.32 s | 48000 Hz | mono | guns.grenadeLauncherReload [player] |
| `sounds/guns/grenadelauncher_shot_a.ogg` | 1.71 s | 44100 Hz | mono | guns.grenadeLauncherFire [player] |
| `sounds/guns/grenadelauncher_shot_b.ogg` | 1.68 s | 44100 Hz | mono | guns.grenadeLauncherFire [player] |
| `sounds/guns/grenadelauncher_shot_c.ogg` | 1.71 s | 44100 Hz | mono | guns.grenadeLauncherFire [player] |
| `sounds/guns/handgun_reload.ogg` | 1.32 s | 44100 Hz | mono | guns.handgunReload [player] |
| `sounds/guns/handgun_shot_a.ogg` | 1.71 s | 44100 Hz | mono | guns.handgunFire [player] |
| `sounds/guns/handgun_shot_b.ogg` | 2.01 s | 44100 Hz | mono | guns.handgunFire [player] |
| `sounds/guns/handgun_shot_c.ogg` | 2.29 s | 44100 Hz | mono | guns.handgunFire [player] |
| `sounds/guns/lasergun_reload.ogg` | 3.05 s | 32000 Hz | mono | guns.lasergunReload [player] |
| `sounds/guns/lasergun_shot_a.ogg` | 1.79 s | 44100 Hz | stereo | guns.lasergunFire [player] |
| `sounds/guns/lasergun_shot_b.ogg` | 1.74 s | 44100 Hz | stereo | guns.lasergunFire [player] |
| `sounds/guns/lasergun_shot_c.ogg` | 1.56 s | 44100 Hz | stereo | guns.lasergunFire [player] |
| `sounds/guns/lmg_reload.ogg` | 5.72 s | 44100 Hz | mono | guns.lmgReload [player] |
| `sounds/guns/lmg_shot_a.ogg` | 1.98 s | 44100 Hz | mono | guns.lmgFire [player] |
| `sounds/guns/lmg_shot_b.ogg` | 1.98 s | 44100 Hz | mono | guns.lmgFire [player] |
| `sounds/guns/lmg_shot_c.ogg` | 1.98 s | 44100 Hz | mono | guns.lmgFire [player] |
| `sounds/guns/mac10_reload.ogg` | 2.64 s | 44100 Hz | mono | guns.mac10Reload [player] |
| `sounds/guns/mac10_reload_akimbo.ogg` | 3.41 s | 44100 Hz | mono | guns.mac10ReloadAkimbo [player] |
| `sounds/guns/mac10_shot_a.ogg` | 1.42 s | 44100 Hz | mono | guns.mac10Fire [player] |
| `sounds/guns/mac10_shot_b.ogg` | 1.42 s | 44100 Hz | mono | guns.mac10Fire [player] |
| `sounds/guns/mac10_shot_c.ogg` | 1.42 s | 44100 Hz | mono | guns.mac10Fire [player] |
| `sounds/guns/minigun_reload.ogg` | 6.24 s | 32000 Hz | mono | guns.minigunReload [player] |
| `sounds/guns/minigun_shot_a.ogg` | 1.00 s | 44100 Hz | mono | guns.minigunFire [player] |
| `sounds/guns/minigun_shot_b.ogg` | 1.00 s | 44100 Hz | mono | guns.minigunFire [player] |
| `sounds/guns/minigun_shot_c.ogg` | 1.00 s | 44100 Hz | mono | guns.minigunFire [player] |
| `sounds/guns/minigun_shot_d.ogg` | 1.00 s | 44100 Hz | mono | guns.minigunFire [player] |
| `sounds/guns/minigun_start_a.ogg` | 0.30 s | 44100 Hz | mono | guns.minigunStart [player] |
| `sounds/guns/minigun_start_b.ogg` | 0.30 s | 44100 Hz | mono | (no sounds.json event) |
| `sounds/guns/minigun_start_c.ogg` | 0.30 s | 44100 Hz | mono | (no sounds.json event) |
| `sounds/guns/pdw_reload.ogg` | 2.57 s | 44100 Hz | mono | guns.pdwReload [player] |
| `sounds/guns/pdw_shot_a.ogg` | 1.26 s | 44100 Hz | mono | guns.pdwFire [player] |
| `sounds/guns/pdw_shot_b.ogg` | 1.26 s | 44100 Hz | mono | guns.pdwFire [player] |
| `sounds/guns/pdw_shot_c.ogg` | 1.26 s | 44100 Hz | mono | guns.pdwFire [player] |
| `sounds/guns/pistol_reload.ogg` | 1.81 s | 44100 Hz | mono | guns.pistolReload [player] |
| `sounds/guns/pistol_reload_akimbo.ogg` | 2.60 s | 44100 Hz | mono | guns.pistolReloadAkimbo [player] |
| `sounds/guns/pistol_shot_a.ogg` | 1.14 s | 44100 Hz | stereo | guns.pistolFire [player] |
| `sounds/guns/pistol_shot_b.ogg` | 1.14 s | 44100 Hz | stereo | guns.pistolFire [player] |
| `sounds/guns/pistol_shot_c.ogg` | 1.14 s | 44100 Hz | stereo | guns.pistolFire [player] |
| `sounds/guns/powerhammer_charge_a.ogg` | 2.53 s | 48000 Hz | mono | guns.powerhammerCharge [player] |
| `sounds/guns/powerhammer_charge_b.ogg` | 2.53 s | 48000 Hz | mono | guns.powerhammerCharge [player] |
| `sounds/guns/powerhammer_charge_c.ogg` | 2.53 s | 48000 Hz | mono | guns.powerhammerCharge [player] |
| `sounds/guns/powerhammer_impactGround_a.ogg` | 1.72 s | 32000 Hz | mono | guns.powerhammerImpactGround [player] |
| `sounds/guns/powerhammer_impactGround_b.ogg` | 1.55 s | 32000 Hz | mono | guns.powerhammerImpactGround [player] |
| `sounds/guns/powerhammer_impactGround_c.ogg` | 1.80 s | 32000 Hz | mono | guns.powerhammerImpactGround [player] |
| `sounds/guns/powerhammer_impactGround_d.ogg` | 1.72 s | 32000 Hz | mono | guns.powerhammerImpactGround [player] |
| `sounds/guns/powerhammer_impact_a.ogg` | 1.65 s | 32000 Hz | mono | guns.powerhammerImpact [player] |
| `sounds/guns/powerhammer_impact_b.ogg` | 1.14 s | 32000 Hz | mono | guns.powerhammerImpact [player] |
| `sounds/guns/powerhammer_impact_c.ogg` | 1.65 s | 32000 Hz | mono | guns.powerhammerImpact [player] |
| `sounds/guns/powerhammer_impact_d.ogg` | 1.14 s | 32000 Hz | mono | guns.powerhammerImpact [player] |
| `sounds/guns/powerhammer_reload.ogg` | 3.43 s | 44100 Hz | mono | guns.powerhammerReload [player] |
| `sounds/guns/powerhammer_shot_a.ogg` | 3.19 s | 48000 Hz | mono | guns.powerhammerFire [player] |
| `sounds/guns/powerhammer_shot_b.ogg` | 2.20 s | 48000 Hz | mono | guns.powerhammerFire [player] |
| `sounds/guns/powerhammer_shot_c.ogg` | 2.58 s | 48000 Hz | mono | guns.powerhammerFire [player] |
| `sounds/guns/powerhammer_swing_a.ogg` | 1.67 s | 48000 Hz | mono | guns.powerhammerSwing [player] |
| `sounds/guns/powerhammer_swing_b.ogg` | 1.39 s | 48000 Hz | mono | guns.powerhammerSwing [player] |
| `sounds/guns/powerhammer_swing_c.ogg` | 1.49 s | 48000 Hz | mono | guns.powerhammerSwing [player] |
| `sounds/guns/pulserifle_reload.ogg` | 2.22 s | 44100 Hz | mono | guns.pulserifleReload [player] |
| `sounds/guns/pulserifle_shot_a.ogg` | 2.75 s | 44100 Hz | mono | guns.pulserifleFire [player] |
| `sounds/guns/pulserifle_shot_b.ogg` | 2.75 s | 44100 Hz | mono | guns.pulserifleFire [player] |
| `sounds/guns/pulserifle_shot_c.ogg` | 2.75 s | 44100 Hz | mono | guns.pulserifleFire [player] |
| `sounds/guns/pulserifle_shot_d.ogg` | 2.75 s | 44100 Hz | mono | guns.pulserifleFire [player] |
| `sounds/guns/pulserifle_shot_stereo.ogg` | 2.75 s | 44100 Hz | stereo | guns.pulserifleFireStereo [player] |
| `sounds/guns/revolver_reload.ogg` | 2.72 s | 44100 Hz | mono | guns.revolverReload [player] |
| `sounds/guns/revolver_reload_akimbo.ogg` | 3.51 s | 44100 Hz | mono | guns.revolverReloadAkimbo [player] |
| `sounds/guns/revolver_shot_a.ogg` | 1.78 s | 48000 Hz | mono | guns.revolverFire [player] |
| `sounds/guns/revolver_shot_b.ogg` | 1.78 s | 48000 Hz | mono | guns.revolverFire [player] |
| `sounds/guns/revolver_shot_c.ogg` | 1.78 s | 48000 Hz | mono | guns.revolverFire [player] |
| `sounds/guns/rocket_reload.ogg` | 3.53 s | 44100 Hz | mono | guns.rocketReload [player] |
| `sounds/guns/rocket_shot_a.ogg` | 2.06 s | 48000 Hz | stereo | guns.rocketFire [player] |
| `sounds/guns/rocket_shot_b.ogg` | 2.06 s | 48000 Hz | stereo | guns.rocketFire [player] |
| `sounds/guns/rocket_shot_c.ogg` | 2.06 s | 48000 Hz | stereo | guns.rocketFire [player] |
| `sounds/guns/sawedoff_reload.ogg` | 1.54 s | 44100 Hz | mono | guns.sawedoffReload [player] |
| `sounds/guns/sawedoff_shot_a.ogg` | 1.30 s | 44100 Hz | mono | guns.sawedoffFire [player] |
| `sounds/guns/sawedoff_shot_b.ogg` | 1.30 s | 44100 Hz | mono | guns.sawedoffFire [player] |
| `sounds/guns/sawedoff_shot_c.ogg` | 1.30 s | 44100 Hz | mono | guns.sawedoffFire [player] |
| `sounds/guns/silencedm4_shot_a.ogg` | 1.07 s | 44100 Hz | mono | guns.silencedM4Fire [player] |
| `sounds/guns/silencedm4_shot_b.ogg` | 1.07 s | 44100 Hz | mono | guns.silencedM4Fire [player] |
| `sounds/guns/sonicshotgun_reload.ogg` | 3.95 s | 48000 Hz | mono | guns.sonicshotgunReload [player] |
| `sounds/guns/sonicshotgun_shot_a.ogg` | 1.76 s | 44100 Hz | mono | guns.sonicshotgunFire [player] |
| `sounds/guns/sonicshotgun_shot_b.ogg` | 1.76 s | 44100 Hz | mono | guns.sonicshotgunFire [player] |
| `sounds/guns/sonicshotgun_shot_c.ogg` | 1.59 s | 44100 Hz | mono | guns.sonicshotgunFire [player] |
| `sounds/guns/teslagun_reload.ogg` | 2.35 s | 44100 Hz | mono | guns.teslaReload [player] |
| `sounds/guns/teslagun_shot_a.ogg` | 3.44 s | 44100 Hz | mono | guns.teslaFire [player] |
| `sounds/guns/teslagun_shot_b.ogg` | 3.44 s | 44100 Hz | mono | guns.teslaFire [player] |
| `sounds/guns/teslagun_shot_c.ogg` | 3.44 s | 44100 Hz | mono | guns.teslaFire [player] |
| `sounds/guns/teslagun_shot_d.ogg` | 3.44 s | 44100 Hz | mono | guns.teslaFire [player] |
| `sounds/guns/thompson_reload.ogg` | 1.76 s | 48000 Hz | mono | guns.thompsonReload [player] |
| `sounds/guns/thompson_shot_a.ogg` | 1.01 s | 192000 Hz | mono | guns.thompsonFire [player] |
| `sounds/guns/thompson_shot_b.ogg` | 1.01 s | 192000 Hz | mono | guns.thompsonFire [player] |
| `sounds/guns/thompson_shot_c.ogg` | 1.01 s | 192000 Hz | mono | guns.thompsonFire [player] |
| `sounds/guns/vector_reload.ogg` | 1.54 s | 48000 Hz | mono | guns.vectorReload [player] |
| `sounds/guns/vector_shot_a.ogg` | 0.60 s | 44100 Hz | mono | guns.vectorFire [player] |
| `sounds/guns/vector_shot_b.ogg` | 0.60 s | 44100 Hz | mono | guns.vectorFire [player] |
| `sounds/guns/vector_shot_c.ogg` | 0.60 s | 44100 Hz | mono | guns.vectorFire [player] |
| `sounds/items/antigrav_start_a.ogg` | 1.81 s | 48000 Hz | mono | items.antiGravStart [player] |
| `sounds/items/antigrav_start_b.ogg` | 1.81 s | 48000 Hz | mono | items.antiGravStart [player] |
| `sounds/items/glider_loop.ogg` | 3.02 s | 48000 Hz | mono | (no sounds.json event) |
| `sounds/items/glider_loop_stereo.ogg` | 9.41 s | 48000 Hz | stereo | glider.loop [player] |
| `sounds/items/jetpack_loop_a.ogg` | 1.10 s | 48000 Hz | mono | jetpack.loop [player] |
| `sounds/items/jetpack_loop_b.ogg` | 1.10 s | 48000 Hz | mono | jetpack.loop [player] |
| `sounds/items/jetpack_loop_c.ogg` | 1.10 s | 48000 Hz | mono | jetpack.loop [player] |
| `sounds/items/jetpack_start_a.ogg` | 1.29 s | 48000 Hz | mono | jetpack.start [player] |
| `sounds/items/jetpack_start_b.ogg` | 1.29 s | 48000 Hz | mono | jetpack.start [player] |
| `sounds/items/jetpack_start_c.ogg` | 1.29 s | 48000 Hz | mono | jetpack.start [player] |
| `sounds/items/jetpack_stop_a.ogg` | 1.40 s | 48000 Hz | mono | jetpack.stop [player] |
| `sounds/items/jetpack_stop_b.ogg` | 1.40 s | 48000 Hz | mono | jetpack.stop [player] |
| `sounds/items/jetpack_stop_c.ogg` | 1.40 s | 48000 Hz | mono | jetpack.stop [player] |
| `sounds/items/nightvision_off.ogg` | 0.44 s | 44100 Hz | mono | nightvision.off [player] |
| `sounds/items/nightvision_on.ogg` | 1.60 s | 44100 Hz | mono | nightvision.on [player] |
| `sounds/machines/ammopress_work1_a.ogg` | 2.57 s | 44100 Hz | mono | machines.ammopressWork1 [block] |
| `sounds/machines/ammopress_work1_b.ogg` | 2.77 s | 44100 Hz | mono | machines.ammopressWork1 [block] |
| `sounds/machines/ammopress_work1_c.ogg` | 2.06 s | 44100 Hz | mono | machines.ammopressWork1 [block] |
| `sounds/machines/ammopress_work2_a.ogg` | 1.06 s | 44100 Hz | mono | machines.ammopressWork2 [block] |
| `sounds/machines/ammopress_work2_b.ogg` | 1.35 s | 44100 Hz | mono | machines.ammopressWork2 [block] |
| `sounds/machines/ammopress_work2_c.ogg` | 1.14 s | 44100 Hz | mono | machines.ammopressWork2 [block] |
| `sounds/machines/charginstation_work.ogg` | 2.08 s | 44100 Hz | mono | machines.chargingstationwork [block] |
| `sounds/machines/chemlab_work_a.ogg` | 3.50 s | 44100 Hz | mono | machines.chemlabWork [block] |
| `sounds/machines/chemlab_work_b.ogg` | 3.50 s | 44100 Hz | mono | machines.chemlabWork [block] |
| `sounds/machines/chemlab_work_c.ogg` | 3.50 s | 44100 Hz | mono | machines.chemlabWork [block] |
| `sounds/machines/chemlab_work_d.ogg` | 3.50 s | 44100 Hz | mono | machines.chemlabWork [block] |
| `sounds/machines/fabricator_work_a.ogg` | 5.58 s | 44100 Hz | mono | machines.fabricatorWork [block] |
| `sounds/machines/fabricator_work_b.ogg` | 5.58 s | 44100 Hz | mono | machines.fabricatorWork [block] |
| `sounds/machines/fabricator_work_c.ogg` | 5.58 s | 44100 Hz | mono | machines.fabricatorWork [block] |
| `sounds/machines/metalpress_small_work_a.ogg` | 2.25 s | 44100 Hz | mono | machines.metalpressWork [block] |
| `sounds/machines/metalpress_small_work_b.ogg` | 2.25 s | 44100 Hz | mono | machines.metalpressWork [block] |
| `sounds/machines/metalpress_small_work_c.ogg` | 2.25 s | 44100 Hz | mono | machines.metalpressWork [block] |
| `sounds/machines/metalpress_work_a.ogg` | 2.98 s | 8000 Hz | mono | machines.metalpressLargeWork [block] |
| `sounds/machines/metalpress_work_b.ogg` | 2.98 s | 44100 Hz | mono | machines.metalpressLargeWork [block] |
| `sounds/machines/metalpress_work_c.ogg` | 2.98 s | 44100 Hz | mono | machines.metalpressLargeWork [block] |
| `sounds/machines/oredrill_l_work_a.ogg` | 6.93 s | 44100 Hz | mono | machines.oredrillLargeWork [block] |
| `sounds/machines/oredrill_l_work_b.ogg` | 6.93 s | 8000 Hz | mono | machines.oredrillLargeWork [block] |
| `sounds/machines/oredrill_l_work_c.ogg` | 6.93 s | 8000 Hz | mono | machines.oredrillLargeWork [block] |
| `sounds/machines/oredrill_l_work_d.ogg` | 6.93 s | 8000 Hz | mono | machines.oredrillLargeWork [block] |
| `sounds/machines/oredrill_m_work_a.ogg` | 4.01 s | 48000 Hz | mono | machines.oredrillMediumWork [block] |
| `sounds/machines/oredrill_m_work_b.ogg` | 8.43 s | 48000 Hz | mono | machines.oredrillMediumWork [block] |
| `sounds/machines/oredrill_m_work_c.ogg` | 4.01 s | 48000 Hz | mono | machines.oredrillMediumWork [block] |
| `sounds/machines/oredrill_m_work_d.ogg` | 5.79 s | 48000 Hz | mono | machines.oredrillMediumWork [block] |
| `sounds/machines/oredrill_s_work_a.ogg` | 6.06 s | 176400 Hz | mono | machines.oredrillSmallWork [block] |
| `sounds/machines/oredrill_s_work_b.ogg` | 6.06 s | 176400 Hz | mono | machines.oredrillSmallWork [block] |
| `sounds/machines/oredrill_s_work_c.ogg` | 6.06 s | 176400 Hz | mono | machines.oredrillSmallWork [block] |
| `sounds/machines/oredrill_s_work_d.ogg` | 9.71 s | 176400 Hz | mono | machines.oredrillSmallWork [block] |
| `sounds/machines/rc_beep.ogg` | 0.11 s | 44100 Hz | mono | machines.rc_beep [block] |
| `sounds/machines/rc_heatray_work.ogg` | 4.00 s | 44100 Hz | mono | machines.rc_heatrayWork [block] |
| `sounds/machines/rc_warning.ogg` | 1.35 s | 22050 Hz | mono | machines.rc_warning [block] |
| `sounds/npcs/alienbug_aggro_a.ogg` | 1.12 s | 44100 Hz | mono | npcs.alienbugAggro [hostile] |
| `sounds/npcs/alienbug_aggro_b.ogg` | 1.41 s | 22050 Hz | mono | npcs.alienbugAggro [hostile] |
| `sounds/npcs/alienbug_aggro_c.ogg` | 1.28 s | 22050 Hz | mono | npcs.alienbugAggro [hostile] |
| `sounds/npcs/alienbug_aggro_d.ogg` | 1.12 s | 22050 Hz | mono | npcs.alienbugAggro [hostile] |
| `sounds/npcs/alienbug_bite_a.ogg` | 0.78 s | 44100 Hz | mono | npcs.alienbugBite [hostile] |
| `sounds/npcs/alienbug_bite_b.ogg` | 0.73 s | 44100 Hz | mono | npcs.alienbugBite [hostile] |
| `sounds/npcs/alienbug_bite_c.ogg` | 0.72 s | 44100 Hz | mono | npcs.alienbugBite [hostile] |
| `sounds/npcs/alienbug_bite_d.ogg` | 0.73 s | 44100 Hz | mono | npcs.alienbugBite [hostile] |
| `sounds/npcs/alienbug_death_a.ogg` | 2.85 s | 32000 Hz | mono | npcs.alienbugDeath [hostile] |
| `sounds/npcs/alienbug_death_b.ogg` | 1.78 s | 32000 Hz | mono | npcs.alienbugDeath [hostile] |
| `sounds/npcs/alienbug_hit-01.ogg` | 1.03 s | 44100 Hz | mono | npcs.alienbugHurt [hostile] |
| `sounds/npcs/alienbug_hit-02.ogg` | 1.21 s | 44100 Hz | mono | npcs.alienbugHurt [hostile] |
| `sounds/npcs/alienbug_hit-03.ogg` | 0.86 s | 44100 Hz | mono | npcs.alienbugHurt [hostile] |
| `sounds/npcs/alienbug_hit-04.ogg` | 0.82 s | 44100 Hz | mono | npcs.alienbugHurt [hostile] |
| `sounds/npcs/alienbug_idle-01.ogg` | 0.59 s | 44100 Hz | mono | npcs.alienbugIdle [hostile] |
| `sounds/npcs/alienbug_idle-02.ogg` | 0.80 s | 44100 Hz | mono | npcs.alienbugIdle [hostile] |
| `sounds/npcs/alienbug_idle-03.ogg` | 0.56 s | 44100 Hz | mono | npcs.alienbugIdle [hostile] |
| `sounds/npcs/alienbug_idle-04.ogg` | 0.54 s | 44100 Hz | mono | npcs.alienbugIdle [hostile] |
| `sounds/npcs/alienbug_idle-05.ogg` | 0.71 s | 44100 Hz | mono | npcs.alienbugIdle [hostile] |
| `sounds/npcs/alienbug_idle-06.ogg` | 0.48 s | 44100 Hz | mono | npcs.alienbugIdle [hostile] |
| `sounds/npcs/alienbug_step_a.ogg` | 0.93 s | 48000 Hz | mono | npcs.alienbugStep [hostile] |
| `sounds/npcs/alienbug_step_b.ogg` | 0.83 s | 48000 Hz | mono | npcs.alienbugStep [hostile] |
| `sounds/npcs/alienbug_step_c.ogg` | 0.87 s | 48000 Hz | mono | npcs.alienbugStep [hostile] |
| `sounds/npcs/apache_burst.ogg` | 2.22 s | 44100 Hz | mono | npcs.apacheBurst [hostile] |
| `sounds/npcs/apache_die.ogg` | 5.99 s | 48000 Hz | mono | npcs.apacheDie [hostile] |
| `sounds/npcs/apache_explode.ogg` | 6.79 s | 44100 Hz | mono | npcs.apacheExplode [hostile] |
| `sounds/npcs/apache_hit_a.ogg` | 3.25 s | 44100 Hz | mono | npcs.apacheHit [hostile] |
| `sounds/npcs/apache_hit_b.ogg` | 3.05 s | 44100 Hz | mono | npcs.apacheHit [hostile] |
| `sounds/npcs/apache_rotor.ogg` | 3.07 s | 22050 Hz | mono | npcs.apacheRotor [hostile] |
| `sounds/npcs/cyberdemon_aggro_a.ogg` | 3.31 s | 44100 Hz | mono | npcs.cyberdemonAggro [hostile] |
| `sounds/npcs/cyberdemon_aggro_b.ogg` | 3.89 s | 44100 Hz | mono | npcs.cyberdemonAggro [hostile] |
| `sounds/npcs/cyberdemon_aggro_c.ogg` | 3.93 s | 44100 Hz | mono | npcs.cyberdemonAggro [hostile] |
| `sounds/npcs/cyberdemon_death_a.ogg` | 2.18 s | 48000 Hz | mono | npcs.cyberdemonDeath [hostile] |
| `sounds/npcs/cyberdemon_death_b.ogg` | 2.65 s | 48000 Hz | mono | npcs.cyberdemonDeath [hostile] |
| `sounds/npcs/cyberdemon_hurt_a.ogg` | 1.46 s | 44100 Hz | mono | npcs.cyberdemonHurt [hostile] |
| `sounds/npcs/cyberdemon_hurt_b.ogg` | 1.70 s | 44100 Hz | mono | npcs.cyberdemonHurt [hostile] |
| `sounds/npcs/cyberdemon_hurt_c.ogg` | 1.46 s | 44100 Hz | mono | npcs.cyberdemonHurt [hostile] |
| `sounds/npcs/cyberdemon_idle_a.ogg` | 1.89 s | 44100 Hz | mono | npcs.cyberdemonIdle [hostile] |
| `sounds/npcs/cyberdemon_idle_b.ogg` | 2.13 s | 44100 Hz | mono | npcs.cyberdemonIdle [hostile] |
| `sounds/npcs/cyberdemon_idle_c.ogg` | 1.98 s | 44100 Hz | mono | npcs.cyberdemonIdle [hostile] |
| `sounds/npcs/cyberdemon_step_a.ogg` | 0.53 s | 44100 Hz | mono | npcs.cyberdemonStep [hostile] |
| `sounds/npcs/cyberdemon_step_b.ogg` | 0.53 s | 44100 Hz | mono | npcs.cyberdemonStep [hostile] |
| `sounds/npcs/cyberdemon_step_c.ogg` | 0.53 s | 44100 Hz | mono | npcs.cyberdemonStep [hostile] |
| `sounds/npcs/cyberdemon_step_d.ogg` | 0.53 s | 44100 Hz | mono | npcs.cyberdemonStep [hostile] |
| `sounds/npcs/turret_beep-01.ogg` | 2.24 s | 48000 Hz | mono | npcs.turretBeep [hostile] |
| `sounds/npcs/turret_beep-02.ogg` | 0.62 s | 48000 Hz | mono | npcs.turretBeep [hostile] |
| `sounds/npcs/turret_beep-03.ogg` | 0.84 s | 48000 Hz | mono | npcs.turretBeep [hostile] |
| `sounds/npcs/turret_beep-04.ogg` | 0.55 s | 48000 Hz | mono | npcs.turretBeep [hostile] |
| `sounds/npcs/turret_beep-05.ogg` | 0.32 s | 48000 Hz | mono | npcs.turretBeep [hostile] |
| `sounds/npcs/turret_beep-06.ogg` | 0.31 s | 48000 Hz | mono | npcs.turretBeep [hostile] |
| `sounds/npcs/turret_beep-07.ogg` | 0.26 s | 48000 Hz | mono | npcs.turretBeep [hostile] |
| `sounds/npcs/turret_gunequip-01.ogg` | 0.81 s | 48000 Hz | mono | npcs.turretGunequip [hostile] |
| `sounds/npcs/turret_gunequip-02.ogg` | 0.88 s | 48000 Hz | mono | npcs.turretGunequip [hostile] |
| `sounds/npcs/turret_gunequip-03.ogg` | 0.50 s | 48000 Hz | mono | npcs.turretGunequip [hostile] |
