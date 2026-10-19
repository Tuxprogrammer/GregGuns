# Sound gate report

Measured A/B checks per sound (no model listening). Verdict: keep when all pass, tweak with one or two failures, rework with three or more or any tonality failure.

171 sounds: keep 44, tweak 116, rework 11.

## `sounds/npcs/apache_burst.ogg`: rework (5.5/10)
- **envelope:** tenth-second contour r=0.82 (min 0.80), mean |dB| diff 10.1 (max 6)
- **band_balance:** band-energy distance 40 points (max 25); biggest gap low_mid: B 34% vs A 12%
- **brightness:** centroid B 478 Hz vs A 1140 Hz (-58%)
- **decay:** decay to -20 dB B 2.06 s vs A 0.56 s
- **embedding:** Audio Flamingo early-layer distance 4.26 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/apache_explode.ogg`: rework (5.5/10)
- **envelope:** tenth-second contour r=0.74 (min 0.80), mean |dB| diff 14.5 (max 6)
- **onsets:** B 9 onsets vs A 21
- **brightness:** centroid B 1347 Hz vs A 755 Hz (+78%)
- **decay:** decay to -20 dB B 3.46 s vs A 1.13 s
- **embedding:** Audio Flamingo early-layer distance 8.32 (pass <= 1.75; a different sound)

## `sounds/items/nightvision_on.ogg`: rework (6.4/10)
- **envelope:** tenth-second contour r=0.77 (min 0.80), mean |dB| diff 5.0 (max 6)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.330 vs A 0.048 (higher = more noise-like, lower = more tonal or resonant)
- **decay:** decay to -20 dB B 0.10 s vs A 0.34 s
- **embedding:** Audio Flamingo early-layer distance 9.12 (pass <= 1.75; a different sound)

## `sounds/npcs/turret_beep-06.ogg`: rework (7.0/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.035 vs A 0.007 (higher = more noise-like, lower = more tonal or resonant)
- **tonality:** 1 spurious tonal lines
- **embedding:** Audio Flamingo early-layer distance 5.15 (pass <= 1.75; same kind of sound but off)

## `sounds/blocks/metaldoor_open_a.ogg`: rework (7.3/10)
- **brightness:** centroid B 2111 Hz vs A 2753 Hz (-23%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.236 vs A 0.068 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 1.86 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/biogun_reload.ogg`: rework (7.3/10)
- **envelope:** tenth-second contour r=0.94 (min 0.80), mean |dB| diff 6.1 (max 6)
- **tonality:** 1 spurious tonal lines
- **embedding:** Audio Flamingo early-layer distance 2.10 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/pistol_reload_akimbo.ogg`: rework (7.3/10)
- **envelope:** tenth-second contour r=0.88 (min 0.80), mean |dB| diff 10.1 (max 6)
- **decay:** decay to -20 dB B 0.04 s vs A 0.25 s
- **embedding:** Audio Flamingo early-layer distance 1.91 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_shot_b.ogg`: rework (7.3/10)
- **brightness:** centroid B 170 Hz vs A 232 Hz (-27%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.035 vs A 0.272 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 2.68 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/vector_shot_a.ogg`: rework (7.3/10)
- **band_balance:** band-energy distance 35 points (max 25); biggest gap bass: B 39% vs A 27%
- **brightness:** centroid B 839 Hz vs A 1119 Hz (-25%)
- **embedding:** Audio Flamingo early-layer distance 2.35 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_idle-05.ogg`: rework (7.3/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.223 vs A 0.072 (higher = more noise-like, lower = more tonal or resonant)
- **tonality:** 1 spurious tonal lines
- **embedding:** Audio Flamingo early-layer distance 3.68 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/minigun_start_a.ogg`: tweak (7.8/10)
- **envelope:** tenth-second contour r=0.92 (min 0.80), mean |dB| diff 10.8 (max 6)
- **embedding:** Audio Flamingo early-layer distance 4.18 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/turret_beep-05.ogg`: tweak (8.0/10)
- **envelope:** tenth-second contour r=0.98 (min 0.80), mean |dB| diff 11.2 (max 6)
- **embedding:** Audio Flamingo early-layer distance 6.47 (pass <= 1.75; a different sound)

## `sounds/blocks/metaldoor_open_b.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.257 vs A 0.078 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 1.85 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/MIB_blaster_reload_akimbo.ogg`: tweak (8.2/10)
- **decay:** decay to -20 dB B 0.11 s vs A 0.05 s
- **embedding:** Audio Flamingo early-layer distance 3.42 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/ak47_reload.ogg`: tweak (8.2/10)
- **envelope:** tenth-second contour r=0.95 (min 0.80), mean |dB| diff 6.2 (max 6)
- **embedding:** Audio Flamingo early-layer distance 2.96 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/alienblaster_reload.ogg`: tweak (8.2/10)
- **decay:** decay to -20 dB B 0.06 s vs A 0.41 s
- **embedding:** Audio Flamingo early-layer distance 2.69 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/biogun_impact_a.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.117 vs A 0.505 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 3.05 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/biogun_impact_b.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.171 vs A 0.466 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 2.96 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/boltaction_reload.ogg`: tweak (8.2/10)
- **onsets:** B 5 onsets vs A 8
- **embedding:** Audio Flamingo early-layer distance 2.98 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/chainsaw_swing_b.ogg`: tweak (8.2/10)
- **decay:** decay to -20 dB B 0.05 s vs A 0.89 s
- **embedding:** Audio Flamingo early-layer distance 4.30 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/grenadelauncher_reload.ogg`: tweak (8.2/10)
- **envelope:** tenth-second contour r=0.77 (min 0.80), mean |dB| diff 7.0 (max 6)
- **embedding:** Audio Flamingo early-layer distance 1.82 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/handgun_reload.ogg`: tweak (8.2/10)
- **decay:** decay to -20 dB B 0.06 s vs A 0.30 s
- **embedding:** Audio Flamingo early-layer distance 2.01 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/lmg_reload.ogg`: tweak (8.2/10)
- **onsets:** B 29 onsets vs A 40
- **embedding:** Audio Flamingo early-layer distance 3.23 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/mac10_reload.ogg`: tweak (8.2/10)
- **onsets:** B 9 onsets vs A 12
- **embedding:** Audio Flamingo early-layer distance 2.70 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/mac10_reload_akimbo.ogg`: tweak (8.2/10)
- **onsets:** B 14 onsets vs A 21
- **embedding:** Audio Flamingo early-layer distance 4.97 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/mac10_shot_c.ogg`: tweak (8.2/10)
- **brightness:** centroid B 1127 Hz vs A 1524 Hz (-26%)
- **embedding:** Audio Flamingo early-layer distance 2.04 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_reload.ogg`: tweak (8.2/10)
- **onsets:** B 12 onsets vs A 8
- **embedding:** Audio Flamingo early-layer distance 2.57 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_swing_b.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.031 vs A 0.243 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 3.08 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/revolver_reload.ogg`: tweak (8.2/10)
- **envelope:** tenth-second contour r=0.97 (min 0.80), mean |dB| diff 8.4 (max 6)
- **embedding:** Audio Flamingo early-layer distance 3.68 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/sonicshotgun_reload.ogg`: rework (8.2/10)
- **tonality:** 1 spurious tonal lines
- **embedding:** Audio Flamingo early-layer distance 3.10 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/thompson_reload.ogg`: tweak (8.2/10)
- **onsets:** B 6 onsets vs A 9
- **embedding:** Audio Flamingo early-layer distance 2.69 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/vector_shot_c.ogg`: tweak (8.2/10)
- **brightness:** centroid B 397 Hz vs A 703 Hz (-44%)
- **embedding:** Audio Flamingo early-layer distance 3.61 (pass <= 1.75; same kind of sound but off)

## `sounds/items/nightvision_off.ogg`: tweak (8.2/10)
- **envelope:** tenth-second contour r=0.84 (min 0.80), mean |dB| diff 7.3 (max 6)
- **embedding:** Audio Flamingo early-layer distance 4.31 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_death_a.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.072 vs A 0.019 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 3.55 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_idle_c.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.010 vs A 0.038 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 3.48 (pass <= 1.75; same kind of sound but off)

## `sounds/machines/rc_beep.ogg`: tweak (8.9/10)
- **embedding:** Audio Flamingo early-layer distance 7.67 (pass <= 1.75; a different sound)

## `sounds/effects/C4Charge_Init.ogg`: tweak (9.0/10)
- **embedding:** Audio Flamingo early-layer distance 6.54 (pass <= 1.75; a different sound)

## `sounds/machines/rc_warning.ogg`: tweak (9.0/10)
- **embedding:** Audio Flamingo early-layer distance 8.15 (pass <= 1.75; a different sound)

## `sounds/npcs/apache_rotor.ogg`: tweak (9.0/10)
- **envelope:** tenth-second contour r=0.75 (min 0.80), mean |dB| diff 2.3 (max 6)

## `sounds/npcs/turret_beep-02.ogg`: tweak (9.0/10)
- **embedding:** Audio Flamingo early-layer distance 5.99 (pass <= 1.75; a different sound)

## `sounds/npcs/turret_beep-04.ogg`: tweak (9.0/10)
- **embedding:** Audio Flamingo early-layer distance 21.11 (pass <= 1.75; a different sound)

## `sounds/npcs/turret_beep-07.ogg`: tweak (9.0/10)
- **embedding:** Audio Flamingo early-layer distance 4.17 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/C4Charge_Plant.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 5.75 (pass <= 1.75; a different sound)

## `sounds/effects/C4Charge_Tick.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 4.68 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/DemoCharge_Init.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 4.08 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/DemoCharge_Plant.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.20 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/DemoCharge_Tick.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 5.70 (pass <= 1.75; a different sound)

## `sounds/effects/biodeath.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.32 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/bullet_impact_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.63 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/bullet_impact_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.34 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/bullet_impact_d.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.52 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/bullet_impact_e.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.74 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/explosion1_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.68 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/explosion1_c.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 5.20 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/explosion1_e.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.27 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/gore_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.77 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/gore_c.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.52 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/MIB_blaster_reload.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 5.13 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/SCAR_reload.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.18 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/ar1_reload.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.54 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/ar1_shot_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.77 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/as50_reload.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.79 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/as50_shot_b.ogg`: tweak (9.1/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.032 vs A 0.009 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/aug_reload.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 4.58 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/beamgun_loop_a.ogg`: tweak (9.1/10)
- **onsets:** B 4 onsets vs A 1

## `sounds/guns/beamgun_start_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.00 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/biogun_shot_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.97 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/biogun_shot_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.89 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/blasterrifle_shot_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.99 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/blasterrifle_shot_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.41 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/boltaction_rechamber.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.87 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/combatshotgun_shot_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.33 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/combatshotgun_shot_b.ogg`: tweak (9.1/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.067 vs A 0.016 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/flamethrower_reload.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.98 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/grenade_pin_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 7.65 (pass <= 1.75; a different sound)

## `sounds/guns/grenadelauncher_shot_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.27 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/handgun_shot_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.85 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/lasergun_reload.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.11 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/lmg_shot_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.33 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/lmg_shot_c.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.91 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/mac10_shot_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.75 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/minigun_reload.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.25 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/pdw_reload.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.90 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/pistol_reload.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.32 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_impactGround_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.52 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_impact_c.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.83 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_shot_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.37 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/revolver_reload_akimbo.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.83 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/revolver_shot_c.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.93 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/sawedoff_reload.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.43 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/teslagun_shot_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.05 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/teslagun_shot_c.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.97 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/thompson_shot_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.00 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/thompson_shot_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.18 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/thompson_shot_c.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.28 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/vector_reload.ogg`: tweak (9.1/10)
- **envelope:** tenth-second contour r=0.93 (min 0.80), mean |dB| diff 6.8 (max 6)

## `sounds/guns/vector_shot_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.17 (pass <= 1.75; same kind of sound but off)

## `sounds/items/antigrav_start_a.ogg`: tweak (9.1/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.049 vs A 0.157 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/machines/ammopress_work1_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.81 (pass <= 1.75; same kind of sound but off)

## `sounds/machines/ammopress_work1_c.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.68 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_aggro_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.82 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_aggro_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.10 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_aggro_d.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.41 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_bite_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.62 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_bite_c.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.70 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_bite_d.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 4.36 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_death_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 4.25 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_death_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.19 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_hit-01.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.33 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_hit-02.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.72 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_hit-03.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.97 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_idle-02.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.59 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_idle-03.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.13 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_idle-04.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.75 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_step_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.39 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_step_c.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.15 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/apache_hit_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 4.68 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/apache_hit_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 5.25 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_aggro_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.28 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_death_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.90 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_hurt_c.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.89 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_step_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.21 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/turret_beep-01.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.42 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/turret_beep-03.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 6.02 (pass <= 1.75; a different sound)

## `sounds/npcs/turret_gunequip-01.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.40 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/turret_gunequip-02.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.56 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/turret_gunequip-03.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 6.05 (pass <= 1.75; a different sound)

## `sounds/blocks/metaldoor_open_c.ogg`: keep (10.0/10)

## `sounds/effects/disintegrate.ogg`: keep (10.0/10)

## `sounds/effects/nukeexplosion.ogg`: keep (10.0/10)

## `sounds/effects/steamarmor_jump_a.ogg`: keep (10.0/10)

## `sounds/effects/steamarmor_jump_d.ogg`: keep (10.0/10)

## `sounds/guns/alienblaster_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/as50_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_start_b.ogg`: keep (10.0/10)

## `sounds/guns/boltaction_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/boltaction_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/boltaction_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/goldenrevolver_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/grenadelauncher_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/grenadelauncher_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/lasergun_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/lasergun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/minigun_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/minigun_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/minigun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/minigun_shot_d.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_charge_a.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_charge_b.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_impactGround_c.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_impactGround_d.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_impact_d.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_swing_a.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_swing_c.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_reload.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_shot_d.ogg`: keep (10.0/10)

## `sounds/guns/revolver_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/revolver_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/rocket_reload.ogg`: keep (10.0/10)

## `sounds/guns/sawedoff_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/sawedoff_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/sawedoff_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/sonicshotgun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/teslagun_reload.ogg`: keep (10.0/10)

## `sounds/items/antigrav_start_b.ogg`: keep (10.0/10)

## `sounds/items/glider_loop_stereo.ogg`: keep (10.0/10)

## `sounds/machines/ammopress_work1_a.ogg`: keep (10.0/10)

## `sounds/machines/ammopress_work2_b.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_hurt_a.ogg`: keep (10.0/10)
