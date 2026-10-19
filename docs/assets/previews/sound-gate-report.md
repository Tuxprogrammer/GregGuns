# Sound gate report

Measured A/B checks per sound (no model listening). Verdict: keep when all pass, tweak with one or two failures, rework with three or more or any tonality failure.

278 sounds: keep 107, tweak 141, rework 30.

## `sounds/npcs/apache_burst.ogg`: rework (4.0/10)
- **loudness:** B -7.7 LUFS vs target 2.0 (-9.7 LU, limit 3)
- **envelope:** tenth-second contour r=0.90 (min 0.80), mean |dB| diff 7.3 (max 6)
- **onsets:** B 0 onsets vs A 3
- **band_balance:** band-energy distance 29 points (max 25); biggest gap low_mid: B 26% vs A 12%
- **brightness:** centroid B 346 Hz vs A 1140 Hz (-70%)
- **decay:** decay to -20 dB B 1.15 s vs A 0.56 s

## `sounds/machines/ammopress_work1_c.ogg`: rework (5.0/10)
- **onsets:** B 2 onsets vs A 6
- **band_balance:** band-energy distance 55 points (max 25); biggest gap low_mid: B 46% vs A 20%
- **brightness:** centroid B 1036 Hz vs A 1746 Hz (-41%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.019 vs A 0.283 (higher = more noise-like, lower = more tonal or resonant)
- **decay:** decay to -20 dB B 1.46 s vs A 0.35 s

## `sounds/npcs/turret_beep-05.ogg`: rework (5.6/10)
- **envelope:** tenth-second contour r=0.53 (min 0.80), mean |dB| diff 13.1 (max 6)
- **band_balance:** band-energy distance 86 points (max 25); biggest gap air: B 75% vs A 32%
- **brightness:** centroid B 13539 Hz vs A 8802 Hz (+54%)
- **decay:** decay to -20 dB B 0.09 s vs A 0.20 s

## `sounds/npcs/turret_beep-06.ogg`: rework (5.6/10)
- **envelope:** tenth-second contour r=0.37 (min 0.80), mean |dB| diff 23.0 (max 6)
- **band_balance:** band-energy distance 27 points (max 25); biggest gap presence: B 66% vs A 80%
- **noisiness:** flatness in 200 Hz-8 kHz B 0.098 vs A 0.007 (higher = more noise-like, lower = more tonal or resonant)
- **decay:** decay to -20 dB B 0.11 s vs A 0.05 s

## `sounds/effects/DemoCharge_Plant.ogg`: rework (6.0/10)
- **envelope:** tenth-second contour r=0.67 (min 0.80), mean |dB| diff 6.9 (max 6)
- **band_balance:** band-energy distance 85 points (max 25); biggest gap bass: B 68% vs A 27%
- **brightness:** centroid B 529 Hz vs A 1028 Hz (-49%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.014 vs A 0.209 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/powerhammer_shot_b.ogg`: rework (6.0/10)
- **onsets:** B 5 onsets vs A 2
- **brightness:** centroid B 124 Hz vs A 232 Hz (-47%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.039 vs A 0.272 (higher = more noise-like, lower = more tonal or resonant)
- **decay:** decay to -20 dB B 0.20 s vs A 1.40 s

## `sounds/npcs/turret_beep-01.ogg`: rework (6.0/10)
- **envelope:** tenth-second contour r=0.77 (min 0.80), mean |dB| diff 10.3 (max 6)
- **band_balance:** band-energy distance 110 points (max 25); biggest gap presence: B 97% vs A 44%
- **brightness:** centroid B 7372 Hz vs A 4174 Hz (+77%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.007 vs A 0.162 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/effects/C4Charge_Init.ogg`: rework (6.7/10)
- **band_balance:** band-energy distance 83 points (max 25); biggest gap mid: B 77% vs A 43%
- **brightness:** centroid B 4211 Hz vs A 5956 Hz (-29%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.001 vs A 0.137 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/npcs/turret_beep-04.ogg`: rework (6.7/10)
- **envelope:** tenth-second contour r=0.54 (min 0.80), mean |dB| diff 8.9 (max 6)
- **onsets:** B 0 onsets vs A 6
- **band_balance:** band-energy distance 70 points (max 25); biggest gap presence: B 19% vs A 54%

## `sounds/effects/DemoCharge_Init.ogg`: rework (7.0/10)
- **band_balance:** band-energy distance 49 points (max 25); biggest gap low_mid: B 20% vs A 4%
- **brightness:** centroid B 4672 Hz vs A 6392 Hz (-27%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.016 vs A 0.189 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/effects/explosion1_b.ogg`: rework (7.0/10)
- **onsets:** B 1 onsets vs A 5
- **brightness:** centroid B 376 Hz vs A 250 Hz (+50%)
- **decay:** decay to -20 dB B 0.27 s vs A 0.96 s

## `sounds/effects/gore_a.ogg`: rework (7.0/10)
- **band_balance:** band-energy distance 48 points (max 25); biggest gap presence: B 6% vs A 18%
- **brightness:** centroid B 818 Hz vs A 1881 Hz (-57%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.160 vs A 0.441 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/effects/steamarmor_jump_d.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.86 (min 0.80), mean |dB| diff 7.9 (max 6)
- **brightness:** centroid B 332 Hz vs A 257 Hz (+29%)
- **decay:** decay to -20 dB B 0.14 s vs A 0.63 s

## `sounds/guns/MIB_blaster_reload.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.82 (min 0.80), mean |dB| diff 8.3 (max 6)
- **band_balance:** band-energy distance 26 points (max 25); biggest gap mid: B 46% vs A 36%
- **noisiness:** flatness in 200 Hz-8 kHz B 0.467 vs A 0.172 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/combatshotgun_shot_a.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.97 (min 0.80), mean |dB| diff 8.0 (max 6)
- **brightness:** centroid B 407 Hz vs A 620 Hz (-34%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.040 vs A 0.011 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/grenadelauncher_reload.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.81 (min 0.80), mean |dB| diff 7.0 (max 6)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.352 vs A 0.119 (higher = more noise-like, lower = more tonal or resonant)
- **decay:** decay to -20 dB B 0.14 s vs A 0.04 s

## `sounds/guns/powerhammer_reload.ogg`: rework (7.0/10)
- **onsets:** B 13 onsets vs A 8
- **band_balance:** band-energy distance 33 points (max 25); biggest gap air: B 9% vs A 18%
- **brightness:** centroid B 2917 Hz vs A 4244 Hz (-31%)

## `sounds/guns/powerhammer_swing_b.ogg`: rework (7.0/10)
- **onsets:** B 8 onsets vs A 3
- **brightness:** centroid B 615 Hz vs A 881 Hz (-30%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.027 vs A 0.243 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/revolver_reload.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.87 (min 0.80), mean |dB| diff 12.7 (max 6)
- **onsets:** B 10 onsets vs A 19
- **band_balance:** band-energy distance 27 points (max 25); biggest gap mid: B 47% vs A 35%

## `sounds/guns/revolver_reload_akimbo.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.88 (min 0.80), mean |dB| diff 12.1 (max 6)
- **onsets:** B 11 onsets vs A 26
- **decay:** decay to -20 dB B 0.19 s vs A 0.06 s

## `sounds/guns/teslagun_shot_a.ogg`: rework (7.0/10)
- **onsets:** B 5 onsets vs A 11
- **brightness:** centroid B 1656 Hz vs A 2266 Hz (-27%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.078 vs A 0.277 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/vector_reload.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.91 (min 0.80), mean |dB| diff 7.8 (max 6)
- **band_balance:** band-energy distance 34 points (max 25); biggest gap presence: B 13% vs A 25%
- **brightness:** centroid B 2840 Hz vs A 3972 Hz (-28%)

## `sounds/guns/vector_shot_a.ogg`: rework (7.0/10)
- **band_balance:** band-energy distance 43 points (max 25); biggest gap sub: B 22% vs A 10%
- **brightness:** centroid B 624 Hz vs A 1119 Hz (-44%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.032 vs A 0.158 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/vector_shot_b.ogg`: rework (7.0/10)
- **band_balance:** band-energy distance 34 points (max 25); biggest gap bass: B 34% vs A 19%
- **brightness:** centroid B 472 Hz vs A 907 Hz (-48%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.043 vs A 0.140 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/vector_shot_c.ogg`: rework (7.0/10)
- **band_balance:** band-energy distance 26 points (max 25); biggest gap bass: B 52% vs A 44%
- **brightness:** centroid B 314 Hz vs A 703 Hz (-55%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.036 vs A 0.131 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/items/nightvision_on.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.70 (min 0.80), mean |dB| diff 5.7 (max 6)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.222 vs A 0.048 (higher = more noise-like, lower = more tonal or resonant)
- **decay:** decay to -20 dB B 0.10 s vs A 0.34 s

## `sounds/npcs/alienbug_hit-03.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.87 (min 0.80), mean |dB| diff 6.8 (max 6)
- **onsets:** B 3 onsets vs A 8
- **decay:** decay to -20 dB B 0.05 s vs A 0.22 s

## `sounds/npcs/apache_explode.ogg`: rework (7.0/10)
- **loudness:** B -9.3 LUFS vs target -4.7 (-4.6 LU, limit 3)
- **envelope:** tenth-second contour r=0.80 (min 0.80), mean |dB| diff 11.7 (max 6)
- **onsets:** B 8 onsets vs A 21

## `sounds/npcs/turret_gunequip-01.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.87 (min 0.80), mean |dB| diff 6.9 (max 6)
- **band_balance:** band-energy distance 42 points (max 25); biggest gap mid: B 63% vs A 45%
- **brightness:** centroid B 3989 Hz vs A 5356 Hz (-26%)

## `sounds/npcs/turret_gunequip-03.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.75 (min 0.80), mean |dB| diff 10.5 (max 6)
- **band_balance:** band-energy distance 56 points (max 25); biggest gap mid: B 53% vs A 36%
- **brightness:** centroid B 4025 Hz vs A 5784 Hz (-30%)

## `sounds/items/glider_loop_stereo.ogg`: tweak (7.8/10)
- **envelope:** tenth-second contour r=0.69 (min 0.80), mean |dB| diff 2.0 (max 6)
- **brightness:** centroid B 1498 Hz vs A 1148 Hz (+30%)

## `sounds/npcs/apache_rotor.ogg`: tweak (7.8/10)
- **envelope:** tenth-second contour r=0.22 (min 0.80), mean |dB| diff 4.4 (max 6)
- **onsets:** B 10 onsets vs A 0

## `sounds/npcs/turret_beep-02.ogg`: tweak (7.8/10)
- **envelope:** tenth-second contour r=0.33 (min 0.80), mean |dB| diff 8.5 (max 6)
- **band_balance:** band-energy distance 52 points (max 25); biggest gap presence: B 28% vs A 54%

## `sounds/npcs/turret_beep-07.ogg`: tweak (7.8/10)
- **envelope:** tenth-second contour r=0.61 (min 0.80), mean |dB| diff 18.6 (max 6)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.074 vs A 0.359 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/blocks/metaldoor_open_a.ogg`: tweak (8.0/10)
- **band_balance:** band-energy distance 26 points (max 25); biggest gap presence: B 15% vs A 22%
- **decay:** decay to -20 dB B 0.14 s vs A 0.40 s

## `sounds/effects/C4Charge_Plant.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.85 (min 0.80), mean |dB| diff 6.3 (max 6)
- **onsets:** B 8 onsets vs A 4

## `sounds/effects/DemoCharge_Tick.ogg`: tweak (8.0/10)
- **band_balance:** band-energy distance 58 points (max 25); biggest gap presence: B 36% vs A 64%
- **noisiness:** flatness in 200 Hz-8 kHz B 0.187 vs A 0.031 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/effects/explosion1_c.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.87 (min 0.80), mean |dB| diff 8.2 (max 6)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.021 vs A 0.084 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/effects/gore_c.ogg`: tweak (8.0/10)
- **onsets:** B 5 onsets vs A 2
- **brightness:** centroid B 678 Hz vs A 1073 Hz (-37%)

## `sounds/effects/nukeexplosion.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.94 (min 0.80), mean |dB| diff 6.0 (max 6)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.000 vs A 0.006 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/MIB_blaster_reload_akimbo.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.74 (min 0.80), mean |dB| diff 9.2 (max 6)
- **decay:** decay to -20 dB B 0.13 s vs A 0.05 s

## `sounds/guns/alienblaster_reload.ogg`: tweak (8.0/10)
- **onsets:** B 16 onsets vs A 10
- **decay:** decay to -20 dB B 0.02 s vs A 0.41 s

## `sounds/guns/as50_reload.ogg`: tweak (8.0/10)
- **band_balance:** band-energy distance 76 points (max 25); biggest gap sub: B 26% vs A 1%
- **brightness:** centroid B 1667 Hz vs A 3884 Hz (-57%)

## `sounds/guns/as50_shot_a.ogg`: tweak (8.0/10)
- **onsets:** B 0 onsets vs A 4
- **noisiness:** flatness in 200 Hz-8 kHz B 0.044 vs A 0.009 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/biogun_reload.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.89 (min 0.80), mean |dB| diff 7.3 (max 6)
- **decay:** decay to -20 dB B 0.19 s vs A 0.03 s

## `sounds/guns/boltaction_reload.ogg`: tweak (8.0/10)
- **onsets:** B 5 onsets vs A 8
- **decay:** decay to -20 dB B 0.08 s vs A 0.03 s

## `sounds/guns/flamethrower_reload.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.91 (min 0.80), mean |dB| diff 6.6 (max 6)
- **decay:** decay to -20 dB B 0.02 s vs A 0.06 s

## `sounds/guns/grenadelauncher_shot_a.ogg`: tweak (8.0/10)
- **onsets:** B 0 onsets vs A 3
- **noisiness:** flatness in 200 Hz-8 kHz B 0.009 vs A 0.202 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/grenadelauncher_shot_c.ogg`: tweak (8.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.005 vs A 0.229 (higher = more noise-like, lower = more tonal or resonant)
- **decay:** decay to -20 dB B 0.14 s vs A 0.29 s

## `sounds/guns/handgun_reload.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.84 (min 0.80), mean |dB| diff 7.2 (max 6)
- **decay:** decay to -20 dB B 0.05 s vs A 0.30 s

## `sounds/guns/lasergun_reload.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.88 (min 0.80), mean |dB| diff 6.3 (max 6)
- **decay:** decay to -20 dB B 0.13 s vs A 0.04 s

## `sounds/guns/lmg_reload.ogg`: tweak (8.0/10)
- **onsets:** B 20 onsets vs A 40
- **decay:** decay to -20 dB B 0.11 s vs A 0.39 s

## `sounds/guns/mac10_reload_akimbo.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.92 (min 0.80), mean |dB| diff 6.4 (max 6)
- **onsets:** B 10 onsets vs A 21

## `sounds/guns/mac10_shot_c.ogg`: tweak (8.0/10)
- **band_balance:** band-energy distance 31 points (max 25); biggest gap sub: B 24% vs A 15%
- **brightness:** centroid B 1018 Hz vs A 1524 Hz (-33%)

## `sounds/guns/minigun_reload.ogg`: tweak (8.0/10)
- **onsets:** B 23 onsets vs A 17
- **brightness:** centroid B 1999 Hz vs A 2705 Hz (-26%)

## `sounds/guns/minigun_shot_a.ogg`: tweak (8.0/10)
- **brightness:** centroid B 918 Hz vs A 1488 Hz (-38%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.030 vs A 0.285 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/minigun_shot_b.ogg`: tweak (8.0/10)
- **brightness:** centroid B 879 Hz vs A 1231 Hz (-29%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.044 vs A 0.259 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/minigun_shot_c.ogg`: tweak (8.0/10)
- **brightness:** centroid B 929 Hz vs A 1222 Hz (-24%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.027 vs A 0.254 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/pistol_reload.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.69 (min 0.80), mean |dB| diff 8.5 (max 6)
- **decay:** decay to -20 dB B 0.05 s vs A 0.13 s

## `sounds/guns/pistol_reload_akimbo.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.75 (min 0.80), mean |dB| diff 12.4 (max 6)
- **decay:** decay to -20 dB B 0.03 s vs A 0.25 s

## `sounds/guns/powerhammer_shot_a.ogg`: tweak (8.0/10)
- **onsets:** B 4 onsets vs A 13
- **noisiness:** flatness in 200 Hz-8 kHz B 0.026 vs A 0.262 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/powerhammer_swing_c.ogg`: tweak (8.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.081 vs A 0.257 (higher = more noise-like, lower = more tonal or resonant)
- **decay:** decay to -20 dB B 0.16 s vs A 0.45 s

## `sounds/guns/revolver_shot_c.ogg`: tweak (8.0/10)
- **onsets:** B 0 onsets vs A 3
- **noisiness:** flatness in 200 Hz-8 kHz B 0.047 vs A 0.164 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/thompson_shot_b.ogg`: tweak (8.0/10)
- **band_balance:** band-energy distance 25 points (max 25); biggest gap sub: B 8% vs A 0%
- **brightness:** centroid B 947 Hz vs A 1224 Hz (-23%)

## `sounds/guns/thompson_shot_c.ogg`: tweak (8.0/10)
- **band_balance:** band-energy distance 26 points (max 25); biggest gap sub: B 8% vs A 0%
- **brightness:** centroid B 796 Hz vs A 1102 Hz (-28%)

## `sounds/items/antigrav_start_a.ogg`: tweak (8.0/10)
- **brightness:** centroid B 357 Hz vs A 262 Hz (+36%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.034 vs A 0.157 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/npcs/alienbug_aggro_a.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.77 (min 0.80), mean |dB| diff 7.7 (max 6)
- **decay:** decay to -20 dB B 0.03 s vs A 0.07 s

## `sounds/npcs/alienbug_bite_c.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.98 (min 0.80), mean |dB| diff 8.5 (max 6)
- **decay:** decay to -20 dB B 0.05 s vs A 0.13 s

## `sounds/npcs/alienbug_bite_d.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.90 (min 0.80), mean |dB| diff 10.5 (max 6)
- **decay:** decay to -20 dB B 0.03 s vs A 0.15 s

## `sounds/npcs/alienbug_idle-04.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.91 (min 0.80), mean |dB| diff 6.3 (max 6)
- **decay:** decay to -20 dB B 0.05 s vs A 0.11 s

## `sounds/npcs/alienbug_step_b.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.85 (min 0.80), mean |dB| diff 10.0 (max 6)
- **onsets:** B 3 onsets vs A 7

## `sounds/npcs/alienbug_step_c.ogg`: tweak (8.0/10)
- **onsets:** B 2 onsets vs A 7
- **decay:** decay to -20 dB B 0.06 s vs A 0.18 s

## `sounds/npcs/apache_hit_a.ogg`: tweak (8.0/10)
- **onsets:** B 10 onsets vs A 17
- **noisiness:** flatness in 200 Hz-8 kHz B 0.005 vs A 0.115 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/npcs/apache_hit_b.ogg`: tweak (8.0/10)
- **onsets:** B 6 onsets vs A 15
- **noisiness:** flatness in 200 Hz-8 kHz B 0.009 vs A 0.038 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/npcs/cyberdemon_idle_c.ogg`: tweak (8.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.011 vs A 0.038 (higher = more noise-like, lower = more tonal or resonant)
- **decay:** decay to -20 dB B 0.10 s vs A 0.28 s

## `sounds/npcs/turret_beep-03.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.77 (min 0.80), mean |dB| diff 6.1 (max 6)
- **onsets:** B 9 onsets vs A 6

## `sounds/npcs/turret_gunequip-02.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.93 (min 0.80), mean |dB| diff 7.6 (max 6)
- **band_balance:** band-energy distance 32 points (max 25); biggest gap mid: B 55% vs A 41%

## `sounds/guns/minigun_start_a.ogg`: tweak (8.8/10)
- **envelope:** tenth-second contour r=0.92 (min 0.80), mean |dB| diff 10.8 (max 6)

## `sounds/machines/rc_beep.ogg`: tweak (8.8/10)
- **envelope:** tenth-second contour r=0.14 (min 0.80), mean |dB| diff 8.0 (max 6)

## `sounds/machines/rc_warning.ogg`: tweak (8.9/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.014 vs A 0.000 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/blocks/metaldoor_open_b.ogg`: tweak (9.0/10)
- **decay:** decay to -20 dB B 0.10 s vs A 0.25 s

## `sounds/blocks/metaldoor_open_c.ogg`: tweak (9.0/10)
- **decay:** decay to -20 dB B 0.10 s vs A 0.42 s

## `sounds/effects/C4Charge_Tick.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.018 vs A 0.001 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/effects/biodeath.ogg`: tweak (9.0/10)
- **envelope:** tenth-second contour r=0.93 (min 0.80), mean |dB| diff 7.1 (max 6)

## `sounds/effects/bullet_impact_a.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.002 vs A 0.043 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/effects/bullet_impact_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.002 vs A 0.043 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/effects/bullet_impact_d.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.003 vs A 0.043 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/effects/bullet_impact_e.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.005 vs A 0.043 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/effects/disintegrate.ogg`: tweak (9.0/10)
- **onsets:** B 10 onsets vs A 14

## `sounds/effects/explosion1_e.ogg`: tweak (9.0/10)
- **onsets:** B 8 onsets vs A 12

## `sounds/effects/steamarmor_jump_a.ogg`: tweak (9.0/10)
- **brightness:** centroid B 493 Hz vs A 332 Hz (+48%)

## `sounds/guns/SCAR_reload.ogg`: tweak (9.0/10)
- **band_balance:** band-energy distance 29 points (max 25); biggest gap mid: B 46% vs A 35%

## `sounds/guns/ak47_reload.ogg`: tweak (9.0/10)
- **envelope:** tenth-second contour r=0.94 (min 0.80), mean |dB| diff 8.6 (max 6)

## `sounds/guns/alienblaster_shot_c.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.101 vs A 0.261 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/ar1_reload.ogg`: tweak (9.0/10)
- **onsets:** B 6 onsets vs A 11

## `sounds/guns/ar1_shot_a.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.245 vs A 0.083 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/as50_shot_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.079 vs A 0.009 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/aug_reload.ogg`: tweak (9.0/10)
- **onsets:** B 20 onsets vs A 16

## `sounds/guns/beamgun_loop_a.ogg`: tweak (9.0/10)
- **onsets:** B 4 onsets vs A 1

## `sounds/guns/beamgun_start_a.ogg`: tweak (9.0/10)
- **onsets:** B 5 onsets vs A 2

## `sounds/guns/beamgun_start_b.ogg`: tweak (9.0/10)
- **onsets:** B 5 onsets vs A 1

## `sounds/guns/biogun_impact_a.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.117 vs A 0.505 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/biogun_impact_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.171 vs A 0.466 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/biogun_shot_a.ogg`: tweak (9.0/10)
- **onsets:** B 1 onsets vs A 4

## `sounds/guns/biogun_shot_b.ogg`: tweak (9.0/10)
- **onsets:** B 1 onsets vs A 4

## `sounds/guns/blasterrifle_shot_a.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.038 vs A 0.196 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/blasterrifle_shot_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.058 vs A 0.167 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/boltaction_rechamber.ogg`: tweak (9.0/10)
- **onsets:** B 3 onsets vs A 6

## `sounds/guns/boltaction_shot_a.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.132 vs A 0.045 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/boltaction_shot_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.113 vs A 0.036 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/boltaction_shot_c.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.096 vs A 0.036 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/chainsaw_swing_b.ogg`: tweak (9.0/10)
- **decay:** decay to -20 dB B 0.04 s vs A 0.89 s

## `sounds/guns/combatshotgun_shot_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.107 vs A 0.016 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/goldenrevolver_shot_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.124 vs A 0.024 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/grenade_pin_b.ogg`: tweak (9.0/10)
- **onsets:** B 5 onsets vs A 2

## `sounds/guns/grenadelauncher_shot_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.022 vs A 0.297 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/handgun_shot_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.052 vs A 0.012 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/lasergun_shot_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.058 vs A 0.241 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/lasergun_shot_c.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.050 vs A 0.244 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/lmg_shot_b.ogg`: tweak (9.0/10)
- **brightness:** centroid B 938 Hz vs A 1348 Hz (-30%)

## `sounds/guns/lmg_shot_c.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.024 vs A 0.067 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/mac10_reload.ogg`: tweak (9.0/10)
- **onsets:** B 7 onsets vs A 12

## `sounds/guns/mac10_shot_b.ogg`: tweak (9.0/10)
- **brightness:** centroid B 957 Hz vs A 1444 Hz (-34%)

## `sounds/guns/minigun_shot_d.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.045 vs A 0.259 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/pdw_reload.ogg`: tweak (9.0/10)
- **decay:** decay to -20 dB B 0.05 s vs A 0.11 s

## `sounds/guns/powerhammer_charge_a.ogg`: tweak (9.0/10)
- **onsets:** B 1 onsets vs A 4

## `sounds/guns/powerhammer_charge_b.ogg`: tweak (9.0/10)
- **decay:** decay to -20 dB B 0.64 s vs A 1.39 s

## `sounds/guns/powerhammer_impactGround_a.ogg`: tweak (9.0/10)
- **onsets:** B 6 onsets vs A 3

## `sounds/guns/powerhammer_impactGround_c.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.241 vs A 0.025 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/powerhammer_impactGround_d.ogg`: tweak (9.0/10)
- **onsets:** B 0 onsets vs A 4

## `sounds/guns/powerhammer_impact_c.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.124 vs A 0.393 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/powerhammer_impact_d.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.142 vs A 0.370 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/powerhammer_shot_c.ogg`: tweak (9.0/10)
- **onsets:** B 2 onsets vs A 6

## `sounds/guns/powerhammer_swing_a.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.046 vs A 0.209 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/pulserifle_reload.ogg`: tweak (9.0/10)
- **envelope:** tenth-second contour r=0.83 (min 0.80), mean |dB| diff 6.3 (max 6)

## `sounds/guns/pulserifle_shot_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.032 vs A 0.119 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/pulserifle_shot_d.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.044 vs A 0.129 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/revolver_shot_a.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.029 vs A 0.133 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/revolver_shot_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.030 vs A 0.151 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/rocket_reload.ogg`: tweak (9.0/10)
- **envelope:** tenth-second contour r=0.81 (min 0.80), mean |dB| diff 8.9 (max 6)

## `sounds/guns/sawedoff_reload.ogg`: tweak (9.0/10)
- **envelope:** tenth-second contour r=0.93 (min 0.80), mean |dB| diff 6.1 (max 6)

## `sounds/guns/sawedoff_shot_a.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.170 vs A 0.050 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/sawedoff_shot_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.133 vs A 0.048 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/sawedoff_shot_c.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.161 vs A 0.051 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/sonicshotgun_reload.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.452 vs A 0.152 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/sonicshotgun_shot_c.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.059 vs A 0.016 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/teslagun_reload.ogg`: tweak (9.0/10)
- **brightness:** centroid B 2714 Hz vs A 3492 Hz (-22%)

## `sounds/guns/teslagun_shot_c.ogg`: tweak (9.0/10)
- **onsets:** B 2 onsets vs A 10

## `sounds/guns/thompson_reload.ogg`: tweak (9.0/10)
- **onsets:** B 6 onsets vs A 9

## `sounds/guns/thompson_shot_a.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.034 vs A 0.145 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/items/antigrav_start_b.ogg`: tweak (9.0/10)
- **envelope:** tenth-second contour r=0.98 (min 0.80), mean |dB| diff 6.8 (max 6)

## `sounds/items/nightvision_off.ogg`: tweak (9.0/10)
- **envelope:** tenth-second contour r=0.82 (min 0.80), mean |dB| diff 7.6 (max 6)

## `sounds/machines/ammopress_work1_a.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.030 vs A 0.188 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/machines/ammopress_work1_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.037 vs A 0.189 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/machines/ammopress_work2_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.006 vs A 0.020 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/npcs/alienbug_aggro_b.ogg`: tweak (9.0/10)
- **onsets:** B 1 onsets vs A 10

## `sounds/npcs/alienbug_aggro_d.ogg`: tweak (9.0/10)
- **onsets:** B 3 onsets vs A 9

## `sounds/npcs/alienbug_bite_b.ogg`: tweak (9.0/10)
- **envelope:** tenth-second contour r=0.89 (min 0.80), mean |dB| diff 9.2 (max 6)

## `sounds/npcs/alienbug_death_a.ogg`: tweak (9.0/10)
- **onsets:** B 1 onsets vs A 5

## `sounds/npcs/alienbug_death_b.ogg`: tweak (9.0/10)
- **onsets:** B 3 onsets vs A 8

## `sounds/npcs/alienbug_hit-01.ogg`: tweak (9.0/10)
- **onsets:** B 2 onsets vs A 5

## `sounds/npcs/alienbug_hit-02.ogg`: tweak (9.0/10)
- **onsets:** B 1 onsets vs A 4

## `sounds/npcs/alienbug_idle-02.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.129 vs A 0.340 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/npcs/alienbug_idle-03.ogg`: tweak (9.0/10)
- **decay:** decay to -20 dB B 0.05 s vs A 0.12 s

## `sounds/npcs/alienbug_idle-05.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.219 vs A 0.072 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/npcs/cyberdemon_aggro_b.ogg`: tweak (9.0/10)
- **onsets:** B 2 onsets vs A 9

## `sounds/npcs/cyberdemon_death_a.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.088 vs A 0.019 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/npcs/cyberdemon_death_b.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.068 vs A 0.019 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/npcs/cyberdemon_hurt_a.ogg`: tweak (9.0/10)
- **envelope:** tenth-second contour r=0.92 (min 0.80), mean |dB| diff 6.1 (max 6)

## `sounds/npcs/cyberdemon_hurt_c.ogg`: tweak (9.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.165 vs A 0.063 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/npcs/cyberdemon_step_a.ogg`: tweak (9.0/10)
- **decay:** decay to -20 dB B 0.06 s vs A 0.17 s

## `sounds/effects/bullet_impact_c.ogg`: keep (10.0/10)

## `sounds/effects/explosion1_a.ogg`: keep (10.0/10)

## `sounds/effects/explosion1_d.ogg`: keep (10.0/10)

## `sounds/effects/gore_b.ogg`: keep (10.0/10)

## `sounds/effects/steamarmor_jump_b.ogg`: keep (10.0/10)

## `sounds/effects/steamarmor_jump_c.ogg`: keep (10.0/10)

## `sounds/guns/MIB_blaster_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/MIB_blaster_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/MIB_blaster_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/SCAR_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/SCAR_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/SCAR_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/ak47_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/ak47_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/ak47_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/alienblaster_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/alienblaster_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/ar1_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/aug_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/aug_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/aug_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_loop_b.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_loop_c.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_loop_d.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_loop_e.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_loop_f.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_start_c.ogg`: keep (10.0/10)

## `sounds/guns/biogun_impact_c.ogg`: keep (10.0/10)

## `sounds/guns/biogun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/blasterrifle_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/cdb_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/cdb_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/cdb_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/cdb_shot_d.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_hit_a.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_hit_b.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_hit_c.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_hit_d.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_hit_e.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_hit_f.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_loop_a.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_loop_b.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_loop_c.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_loopend.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_loopstart.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_swing_a.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_swing_c.ogg`: keep (10.0/10)

## `sounds/guns/combatshotgun_pump.ogg`: keep (10.0/10)

## `sounds/guns/combatshotgun_reload.ogg`: keep (10.0/10)

## `sounds/guns/combatshotgun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_shot_d.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_shot_e.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_start_a.ogg`: keep (10.0/10)

## `sounds/guns/goldenrevolver_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/goldenrevolver_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/grenade_pin_a.ogg`: keep (10.0/10)

## `sounds/guns/grenade_throw_a.ogg`: keep (10.0/10)

## `sounds/guns/grenade_throw_b.ogg`: keep (10.0/10)

## `sounds/guns/grenade_throw_c.ogg`: keep (10.0/10)

## `sounds/guns/handgun_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/handgun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/lasergun_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/lmg_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/mac10_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/pdw_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/pdw_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/pdw_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/pistol_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/pistol_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/pistol_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_charge_c.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_impactGround_b.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_impact_a.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_impact_b.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_shot_stereo.ogg`: keep (10.0/10)

## `sounds/guns/rocket_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/rocket_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/rocket_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/silencedm4_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/silencedm4_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/sonicshotgun_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/sonicshotgun_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/teslagun_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/teslagun_shot_d.ogg`: keep (10.0/10)

## `sounds/machines/ammopress_work2_a.ogg`: keep (10.0/10)

## `sounds/machines/ammopress_work2_c.ogg`: keep (10.0/10)

## `sounds/machines/rc_heatray_work.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_aggro_c.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_bite_a.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_hit-04.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_idle-01.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_idle-06.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_step_a.ogg`: keep (10.0/10)

## `sounds/npcs/apache_die.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_aggro_a.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_aggro_c.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_hurt_b.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_idle_a.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_idle_b.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_step_b.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_step_c.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_step_d.ogg`: keep (10.0/10)
