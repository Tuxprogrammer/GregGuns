# Sound gate report

Measured A/B checks per sound (no model listening). Verdict: keep when all pass, accept when every measured check passes and the embedding distance is below 4.5 (MVP tier), tweak with one or two failures, rework with three or more or any tonality failure.

278 sounds: keep 158, accept 69, tweak 32, rework 19.

## `sounds/npcs/apache_burst.ogg`: rework (5.5/10)
- **loudness:** B -7.0 LUFS vs target -3.9 (-3.1 LU, limit 3)
- **envelope:** tenth-second contour r=0.88 (min 0.80), mean |dB| diff 7.7 (max 6)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.379 vs A 0.081 (higher = more noise-like, lower = more tonal or resonant)
- **decay:** decay to -20 dB B 1.36 s vs A 0.56 s
- **embedding:** Audio Flamingo early-layer distance 3.12 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/turret_beep-02.ogg`: rework (6.0/10)
- **band_balance:** band-energy distance 51 points (max 25); biggest gap presence: B 28% vs A 54%
- **noisiness:** flatness in 200 Hz-8 kHz B 0.011 vs A 0.003 (higher = more noise-like, lower = more tonal or resonant)
- **tonality:** 13 spurious tonal lines
- **embedding:** Audio Flamingo early-layer distance 3.83 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/ak47_reload.ogg`: rework (6.4/10)
- **envelope:** tenth-second contour r=0.87 (min 0.80), mean |dB| diff 6.6 (max 6)
- **onsets:** B 20 onsets vs A 15
- **tonality:** 1 spurious tonal lines
- **embedding:** Audio Flamingo early-layer distance 4.57 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/as50_reload.ogg`: rework (6.4/10)
- **envelope:** tenth-second contour r=0.84 (min 0.80), mean |dB| diff 6.1 (max 6)
- **onsets:** B 41 onsets vs A 32
- **decay:** decay to -20 dB B 0.02 s vs A 0.22 s
- **embedding:** Audio Flamingo early-layer distance 3.35 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/pistol_reload.ogg`: rework (6.4/10)
- **envelope:** tenth-second contour r=0.74 (min 0.80), mean |dB| diff 9.3 (max 6)
- **onsets:** B 14 onsets vs A 11
- **decay:** decay to -20 dB B 0.02 s vs A 0.13 s
- **embedding:** Audio Flamingo early-layer distance 3.92 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/pistol_reload_akimbo.ogg`: rework (6.4/10)
- **envelope:** tenth-second contour r=0.75 (min 0.80), mean |dB| diff 11.8 (max 6)
- **onsets:** B 26 onsets vs A 19
- **decay:** decay to -20 dB B 0.03 s vs A 0.25 s
- **embedding:** Audio Flamingo early-layer distance 5.86 (pass <= 1.75; a different sound)

## `sounds/guns/revolver_reload_akimbo.ogg`: rework (6.4/10)
- **envelope:** tenth-second contour r=0.72 (min 0.80), mean |dB| diff 7.8 (max 6)
- **onsets:** B 33 onsets vs A 26
- **decay:** decay to -20 dB B 0.02 s vs A 0.06 s
- **embedding:** Audio Flamingo early-layer distance 4.84 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/vector_reload.ogg`: rework (6.4/10)
- **envelope:** tenth-second contour r=0.58 (min 0.80), mean |dB| diff 10.5 (max 6)
- **onsets:** B 13 onsets vs A 9
- **decay:** decay to -20 dB B 0.03 s vs A 0.09 s
- **embedding:** Audio Flamingo early-layer distance 3.70 (pass <= 1.75; same kind of sound but off)

## `sounds/items/nightvision_on.ogg`: rework (6.4/10)
- **envelope:** tenth-second contour r=0.77 (min 0.80), mean |dB| diff 4.9 (max 6)
- **decay:** decay to -20 dB B 0.11 s vs A 0.34 s
- **tonality:** 15 spurious tonal lines
- **embedding:** Audio Flamingo early-layer distance 7.29 (pass <= 1.75; a different sound)

## `sounds/effects/C4Charge_Init.ogg`: rework (7.0/10)
- **envelope:** tenth-second contour r=0.81 (min 0.80), mean |dB| diff 7.0 (max 6)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.026 vs A 0.137 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 3.92 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/turret_beep-07.ogg`: rework (7.0/10)
- **brightness:** centroid B 10910 Hz vs A 14639 Hz (-25%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.127 vs A 0.359 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 4.62 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/gore_a.ogg`: rework (7.3/10)
- **band_balance:** band-energy distance 44 points (max 25); biggest gap bass: B 42% vs A 29%
- **brightness:** centroid B 1118 Hz vs A 1881 Hz (-41%)
- **embedding:** Audio Flamingo early-layer distance 2.81 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/alienblaster_reload.ogg`: rework (7.3/10)
- **onsets:** B 27 onsets vs A 10
- **decay:** decay to -20 dB B 0.03 s vs A 0.41 s
- **embedding:** Audio Flamingo early-layer distance 4.47 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_shot_b.ogg`: rework (7.3/10)
- **brightness:** centroid B 170 Hz vs A 232 Hz (-27%)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.035 vs A 0.272 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 2.68 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/thompson_shot_b.ogg`: rework (7.3/10)
- **band_balance:** band-energy distance 45 points (max 25); biggest gap presence: B 22% vs A 6%
- **brightness:** centroid B 3305 Hz vs A 1224 Hz (+170%)
- **embedding:** Audio Flamingo early-layer distance 1.84 (pass <= 1.75; same kind of sound but off)

## `sounds/items/nightvision_off.ogg`: rework (7.3/10)
- **envelope:** tenth-second contour r=0.79 (min 0.80), mean |dB| diff 7.5 (max 6)
- **tonality:** 1 spurious tonal lines
- **embedding:** Audio Flamingo early-layer distance 6.55 (pass <= 1.75; a different sound)

## `sounds/guns/minigun_start_a.ogg`: tweak (7.8/10)
- **envelope:** tenth-second contour r=0.94 (min 0.80), mean |dB| diff 6.5 (max 6)
- **embedding:** Audio Flamingo early-layer distance 2.46 (pass <= 1.75; same kind of sound but off)

## `sounds/machines/rc_warning.ogg`: rework (8.0/10)
- **tonality:** 1 spurious tonal lines
- **embedding:** Audio Flamingo early-layer distance 11.32 (pass <= 1.75; a different sound)

## `sounds/npcs/turret_beep-06.ogg`: rework (8.0/10)
- **tonality:** 6 spurious tonal lines
- **embedding:** Audio Flamingo early-layer distance 2.65 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/bullet_impact_b.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.121 vs A 0.043 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 3.11 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/bullet_impact_c.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.196 vs A 0.043 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 4.00 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/bullet_impact_d.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.158 vs A 0.043 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 3.06 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/bullet_impact_e.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.163 vs A 0.043 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 3.14 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/MIB_blaster_reload.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.489 vs A 0.172 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 4.19 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/MIB_blaster_reload_akimbo.ogg`: tweak (8.2/10)
- **decay:** decay to -20 dB B 0.13 s vs A 0.05 s
- **embedding:** Audio Flamingo early-layer distance 2.62 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/biogun_impact_a.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.156 vs A 0.505 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 2.73 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/biogun_reload.ogg`: tweak (8.2/10)
- **onsets:** B 25 onsets vs A 13
- **embedding:** Audio Flamingo early-layer distance 3.60 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/handgun_reload.ogg`: tweak (8.2/10)
- **decay:** decay to -20 dB B 0.06 s vs A 0.30 s
- **embedding:** Audio Flamingo early-layer distance 1.89 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/lmg_reload.ogg`: tweak (8.2/10)
- **decay:** decay to -20 dB B 0.08 s vs A 0.39 s
- **embedding:** Audio Flamingo early-layer distance 3.10 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/mac10_reload.ogg`: tweak (8.2/10)
- **envelope:** tenth-second contour r=0.58 (min 0.80), mean |dB| diff 4.4 (max 6)
- **embedding:** Audio Flamingo early-layer distance 2.89 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/mac10_reload_akimbo.ogg`: tweak (8.2/10)
- **decay:** decay to -20 dB B 0.02 s vs A 0.14 s
- **embedding:** Audio Flamingo early-layer distance 3.45 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_reload.ogg`: tweak (8.2/10)
- **onsets:** B 12 onsets vs A 8
- **embedding:** Audio Flamingo early-layer distance 1.91 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/revolver_reload.ogg`: tweak (8.2/10)
- **decay:** decay to -20 dB B 0.02 s vs A 0.11 s
- **embedding:** Audio Flamingo early-layer distance 1.92 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/thompson_reload.ogg`: tweak (8.2/10)
- **envelope:** tenth-second contour r=0.86 (min 0.80), mean |dB| diff 6.4 (max 6)
- **embedding:** Audio Flamingo early-layer distance 2.93 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_idle-05.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.213 vs A 0.072 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 2.95 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/apache_die.ogg`: tweak (8.2/10)
- **decay:** decay to -20 dB B 3.95 s vs A 1.73 s
- **embedding:** Audio Flamingo early-layer distance 2.22 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/apache_explode.ogg`: tweak (8.2/10)
- **envelope:** tenth-second contour r=0.95 (min 0.80), mean |dB| diff 7.4 (max 6)
- **embedding:** Audio Flamingo early-layer distance 3.38 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_aggro_b.ogg`: tweak (8.2/10)
- **decay:** decay to -20 dB B 0.47 s vs A 0.23 s
- **embedding:** Audio Flamingo early-layer distance 2.57 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_death_a.ogg`: tweak (8.2/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.060 vs A 0.019 (higher = more noise-like, lower = more tonal or resonant)
- **embedding:** Audio Flamingo early-layer distance 3.08 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/turret_beep-03.ogg`: rework (8.2/10)
- **tonality:** 2 spurious tonal lines
- **embedding:** Audio Flamingo early-layer distance 2.92 (pass <= 1.75; same kind of sound but off)

## `sounds/machines/rc_beep.ogg`: accept (8.9/10)
- **embedding:** Audio Flamingo early-layer distance 4.43 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/apache_rotor.ogg`: tweak (9.0/10)
- **envelope:** tenth-second contour r=0.58 (min 0.80), mean |dB| diff 1.7 (max 6)

## `sounds/npcs/turret_beep-04.ogg`: tweak (9.0/10)
- **embedding:** Audio Flamingo early-layer distance 5.59 (pass <= 1.75; a different sound)

## `sounds/npcs/turret_beep-05.ogg`: accept (9.0/10)
- **embedding:** Audio Flamingo early-layer distance 4.06 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/C4Charge_Plant.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 5.76 (pass <= 1.75; a different sound)

## `sounds/effects/C4Charge_Tick.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 4.40 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/DemoCharge_Init.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.69 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/DemoCharge_Plant.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.93 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/DemoCharge_Tick.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.88 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/biodeath.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.18 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/bullet_impact_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.05 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/explosion1_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.42 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/explosion1_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 4.37 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/explosion1_e.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.02 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/gore_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.03 (pass <= 1.75; same kind of sound but off)

## `sounds/effects/gore_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.52 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/as50_shot_b.ogg`: tweak (9.1/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.039 vs A 0.009 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/aug_reload.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.40 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/biogun_impact_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.33 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/biogun_impact_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.27 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/blasterrifle_shot_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.13 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/chainsaw_hit_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.06 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/chainsaw_loop_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.80 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/chainsaw_loopstart.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.94 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/chainsaw_swing_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.30 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/chainsaw_swing_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.91 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/chainsaw_swing_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.40 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/combatshotgun_pump.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.23 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/combatshotgun_reload.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.08 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/combatshotgun_shot_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.31 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/combatshotgun_shot_b.ogg`: tweak (9.1/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.067 vs A 0.016 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/guns/grenade_pin_a.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 6.94 (pass <= 1.75; a different sound)

## `sounds/guns/grenade_pin_b.ogg`: tweak (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 5.95 (pass <= 1.75; a different sound)

## `sounds/guns/grenadelauncher_reload.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.09 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/grenadelauncher_shot_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.86 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/handgun_shot_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.77 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/handgun_shot_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.99 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/handgun_shot_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.05 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/lmg_shot_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.86 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/lmg_shot_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.80 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/mac10_shot_c.ogg`: tweak (9.1/10)
- **brightness:** centroid B 1128 Hz vs A 1524 Hz (-26%)

## `sounds/guns/pdw_reload.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.83 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/pistol_shot_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.75 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_impactGround_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.03 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_impactGround_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.96 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_impact_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.79 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/powerhammer_swing_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.06 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/sonicshotgun_reload.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.70 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/thompson_shot_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.95 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/thompson_shot_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.75 (pass <= 1.75; same kind of sound but off)

## `sounds/guns/vector_shot_a.ogg`: tweak (9.1/10)
- **brightness:** centroid B 865 Hz vs A 1119 Hz (-23%)

## `sounds/guns/vector_shot_c.ogg`: tweak (9.1/10)
- **brightness:** centroid B 523 Hz vs A 703 Hz (-26%)

## `sounds/machines/ammopress_work1_a.ogg`: tweak (9.1/10)
- **noisiness:** flatness in 200 Hz-8 kHz B 0.073 vs A 0.188 (higher = more noise-like, lower = more tonal or resonant)

## `sounds/machines/ammopress_work1_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.68 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_aggro_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.75 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_aggro_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.94 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_aggro_d.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.04 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_bite_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.30 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_bite_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.18 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_bite_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.12 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_bite_d.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.20 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_death_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.75 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_hit-03.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.90 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_idle-01.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.19 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_idle-02.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.46 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_idle-03.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.99 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_idle-04.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.96 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_idle-06.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.55 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_step_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.24 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/alienbug_step_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.07 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/apache_hit_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.83 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_aggro_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 3.19 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_aggro_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.94 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_hurt_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.20 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_idle_a.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.47 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_idle_b.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.53 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_idle_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.60 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/cyberdemon_step_c.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.44 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/turret_beep-01.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 2.53 (pass <= 1.75; same kind of sound but off)

## `sounds/npcs/turret_gunequip-03.ogg`: accept (9.1/10)
- **embedding:** Audio Flamingo early-layer distance 1.78 (pass <= 1.75; same kind of sound but off)

## `sounds/blocks/metaldoor_open_a.ogg`: keep (10.0/10)

## `sounds/blocks/metaldoor_open_b.ogg`: keep (10.0/10)

## `sounds/blocks/metaldoor_open_c.ogg`: keep (10.0/10)

## `sounds/effects/disintegrate.ogg`: keep (10.0/10)

## `sounds/effects/explosion1_a.ogg`: keep (10.0/10)

## `sounds/effects/explosion1_d.ogg`: keep (10.0/10)

## `sounds/effects/nukeexplosion.ogg`: keep (10.0/10)

## `sounds/effects/steamarmor_jump_a.ogg`: keep (10.0/10)

## `sounds/effects/steamarmor_jump_b.ogg`: keep (10.0/10)

## `sounds/effects/steamarmor_jump_c.ogg`: keep (10.0/10)

## `sounds/effects/steamarmor_jump_d.ogg`: keep (10.0/10)

## `sounds/guns/MIB_blaster_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/MIB_blaster_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/MIB_blaster_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/SCAR_reload.ogg`: keep (10.0/10)

## `sounds/guns/SCAR_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/SCAR_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/SCAR_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/ak47_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/ak47_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/ak47_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/alienblaster_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/alienblaster_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/alienblaster_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/ar1_reload.ogg`: keep (10.0/10)

## `sounds/guns/ar1_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/ar1_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/as50_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/aug_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/aug_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/aug_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_loop_a.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_loop_b.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_loop_c.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_loop_d.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_loop_e.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_loop_f.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_start_a.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_start_b.ogg`: keep (10.0/10)

## `sounds/guns/beamgun_start_c.ogg`: keep (10.0/10)

## `sounds/guns/biogun_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/biogun_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/biogun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/blasterrifle_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/blasterrifle_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/boltaction_rechamber.ogg`: keep (10.0/10)

## `sounds/guns/boltaction_reload.ogg`: keep (10.0/10)

## `sounds/guns/boltaction_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/boltaction_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/boltaction_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/cdb_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/cdb_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/cdb_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/cdb_shot_d.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_hit_a.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_hit_b.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_hit_d.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_hit_e.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_hit_f.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_loop_a.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_loop_c.ogg`: keep (10.0/10)

## `sounds/guns/chainsaw_loopend.ogg`: keep (10.0/10)

## `sounds/guns/combatshotgun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_reload.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_shot_d.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_shot_e.ogg`: keep (10.0/10)

## `sounds/guns/flamethrower_start_a.ogg`: keep (10.0/10)

## `sounds/guns/goldenrevolver_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/goldenrevolver_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/goldenrevolver_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/grenade_throw_a.ogg`: keep (10.0/10)

## `sounds/guns/grenade_throw_b.ogg`: keep (10.0/10)

## `sounds/guns/grenade_throw_c.ogg`: keep (10.0/10)

## `sounds/guns/grenadelauncher_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/grenadelauncher_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/lasergun_reload.ogg`: keep (10.0/10)

## `sounds/guns/lasergun_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/lasergun_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/lasergun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/lmg_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/mac10_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/mac10_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/minigun_reload.ogg`: keep (10.0/10)

## `sounds/guns/minigun_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/minigun_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/minigun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/minigun_shot_d.ogg`: keep (10.0/10)

## `sounds/guns/pdw_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/pdw_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/pdw_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/pistol_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/pistol_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_charge_a.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_charge_b.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_charge_c.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_impactGround_c.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_impactGround_d.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_impact_a.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_impact_b.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_impact_d.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_swing_a.ogg`: keep (10.0/10)

## `sounds/guns/powerhammer_swing_c.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_reload.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_shot_d.ogg`: keep (10.0/10)

## `sounds/guns/pulserifle_shot_stereo.ogg`: keep (10.0/10)

## `sounds/guns/revolver_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/revolver_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/revolver_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/rocket_reload.ogg`: keep (10.0/10)

## `sounds/guns/rocket_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/rocket_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/rocket_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/sawedoff_reload.ogg`: keep (10.0/10)

## `sounds/guns/sawedoff_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/sawedoff_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/sawedoff_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/silencedm4_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/silencedm4_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/sonicshotgun_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/sonicshotgun_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/sonicshotgun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/teslagun_reload.ogg`: keep (10.0/10)

## `sounds/guns/teslagun_shot_a.ogg`: keep (10.0/10)

## `sounds/guns/teslagun_shot_b.ogg`: keep (10.0/10)

## `sounds/guns/teslagun_shot_c.ogg`: keep (10.0/10)

## `sounds/guns/teslagun_shot_d.ogg`: keep (10.0/10)

## `sounds/guns/vector_shot_b.ogg`: keep (10.0/10)

## `sounds/items/antigrav_start_a.ogg`: keep (10.0/10)

## `sounds/items/antigrav_start_b.ogg`: keep (10.0/10)

## `sounds/items/glider_loop_stereo.ogg`: keep (10.0/10)

## `sounds/machines/ammopress_work1_b.ogg`: keep (10.0/10)

## `sounds/machines/ammopress_work2_a.ogg`: keep (10.0/10)

## `sounds/machines/ammopress_work2_b.ogg`: keep (10.0/10)

## `sounds/machines/ammopress_work2_c.ogg`: keep (10.0/10)

## `sounds/machines/rc_heatray_work.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_aggro_b.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_death_a.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_hit-01.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_hit-02.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_hit-04.ogg`: keep (10.0/10)

## `sounds/npcs/alienbug_step_b.ogg`: keep (10.0/10)

## `sounds/npcs/apache_hit_a.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_death_b.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_hurt_a.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_hurt_b.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_step_a.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_step_b.ogg`: keep (10.0/10)

## `sounds/npcs/cyberdemon_step_d.ogg`: keep (10.0/10)

## `sounds/npcs/turret_gunequip-01.ogg`: keep (10.0/10)

## `sounds/npcs/turret_gunequip-02.ogg`: keep (10.0/10)
