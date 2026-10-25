<!-- Known differences from the original sound pack. Built from tools/asset-gen/sound-status.json and the clean-room blueprint judge; update when sounds change. -->
# Known differences from the original sound pack

> **Archived (2026-10-25).** The owner accepts the sounds as they are, and the sound review pipeline is no longer maintained ([design record 0017](../design/0017-accept-sounds-archive-sound-pipeline.md)). This page is kept as a record. The open items below are not planned work; an adopter is expected to commission its own assets.

GregGuns sounds are original, procedurally synthesized files, measured against the sounds they replace and never copied from them. This page records, in measurements and words only, where ours still differ and what to try when the generators are revisited. It holds no audio data from the originals and names none of their files.

## Summary

Two independent checks cover all 278 sounds.

1. **The measured gate and embedding check** (`tools/asset-gen/sound_gate.py`; report in [previews/sound-gate-report.md](previews/sound-gate-report.md)). It compares each sound with the one it replaces on loudness, true peak, length, loudness contour, hit count, band balance, brightness, noisiness, decay and spurious tonal lines, plus the distance between audio-model embeddings (pass at 1.75 or less). Tiers: **keep** passes everything; **accept** fails only the embedding check, within the same-kind band; **tweak** fails one or two checks; **rework** fails three or more, or fails tonality. Calibration: [tools/audio-describe/CALIBRATION.md](../../tools/audio-describe/CALIBRATION.md).
2. **The clean-room blueprint judge.** Each original was reduced once to a numeric blueprint (timing, loudness contour, band shares, pitch and modulation tracks). The judge compares our file with the blueprint metric by metric. Each tolerance is set from the spread of our own keep sounds (the 99.4th percentile of their absolute deltas, so about 94 percent of keep sounds pass). A failure therefore means a sound is farther from its original than a sound we already approve of. A softer tier flags near misses without failing. The analyzer, blueprints and judge are kept locally and are not in this repository, because they derive from the originals.

Gate tiers per family, with the judge's pass and fail counts at its last run:

| Family | Sounds | Keep | Accept | Tweak | Rework | Judge pass | Judge fail |
|---|---|---|---|---|---|---|---|
| blocks | 3 | 3 | 0 | 0 | 0 | 3 | 0 |
| effects | 26 | 8 | 11 | 5 | 2 | 26 | 0 |
| guns | 181 | 124 | 29 | 19 | 9 | 176 | 6 |
| items | 5 | 3 | 0 | 0 | 2 | 5 | 0 |
| machines | 9 | 5 | 2 | 1 | 1 | 9 | 0 |
| npcs | 54 | 15 | 27 | 7 | 5 | 47 | 7 |
| **All** | **278** | **158** | **69** | **32** | **19** | **266** | **13** |

The judge passes 266 of 279 sounds. Before the clean-room pass the gate read keep 165, accept 73, tweak 33, rework 7; it now reads keep 158, accept 69, tweak 32, rework 19. The two checks pull against each other on some sounds: 28 sounds changed tier, and several multi-hit reloads (as50_reload, pistol_reload, vector_reload, revolver_reload_akimbo) and thompson_shot_b moved down to rework while their blueprint distance improved. The owner listened to the edited sounds and approved them. Reconciling the two checks is the first open item below.

Judge failures by metric: `rms_active_db` 3, `energy_t50_log2` 3, `f0_cents` 2, `am_rate_log2` 1, `audible_len_log2` 1, `centroid_offset_oct` 1, `band_share_max_db` 1, `energy_t90_log2` 1, `env_corr_loss` 1, `crest_active_db` 1.

## Family by family

Everywhere, ours is procedural: noise, modal and FM layers, synthetic impulse responses and a limiter, seeded from the sound's id so renders are byte-identical. No recordings or sampled room responses are involved, which shapes every difference below.

- **Gun shots.** Crack and body are noise bursts over a tuned tail bed. Our tails are cleaner and smoother than the originals' (less grit in the 1 to 8 kHz range), and some shots sit a little bright or dark. The low octave bands are the usual miss (thompson_shot_b, mac10_shot_c).
- **Gun reloads and mechanical foley.** Clicks are placed from numeric hit lists. Hit counts and spacing now follow the blueprints closely, but individual clicks are cleaner and less varied, so dense multi-hit reloads sound thinner.
- **Gun impacts and loops.** Powerhammer and chainsaw sounds are the right kind of event; ours lose some late sustain. The chainsaw loop is the only strongly pitched gun sound.
- **Explosions, gore and bullet impacts.** Ours has a less dense debris texture and a thinner wet low body. Bullet impacts carry a 53 Hz hum to match the originals' voiced fundamental.
- **Charges and beeps.** The charge ticks and plant sounds are built from clean tones and clicks, while the originals are noisier and more mechanical, so ours sound toy-like beside them. This is the weakest effects group.
- **Machines and items.** Ammo press and repair-bench sounds are close. The warning ladder and the night vision pair are the exceptions (spurious tonal lines, envelope).
- **Hostile NPCs.** Alien bug and cyberdemon voices have the right role and timing but plainer growl and grit textures. Apache sounds are the extreme case, because the originals are clipped far past full scale.
- **Loudness behavior.** Several originals peak above full scale (sample peaks up to about +6 dBFS). Our finishing matches integrated loudness to the manifest target and caps true peak at -1 dBTP with at most 3.5 dB of limiting. We therefore cannot reproduce those sounds' crest or, for the loudest, their active loudness (apache_burst).
- **Turret beeps.** The originals are single steady lines, often above 2 kHz, over a pedestal of haze. Making ours both pitch-correct and free of spurious lines (the tonality gate) pulls in opposite directions.

## Per sound

Every sound that fails the judge or is not keep in the gate (122 rows). Distance is the embedding distance (keep at 1.75 or less). The Judge column lists failing metrics as delta (ours minus blueprint) against the tolerance; `pass` means the judge passes it.

| Sound | Tier | Distance | Judge | Reason |
|---|---|---|---|---|
| `sounds/effects/C4Charge_Init.ogg` | rework | 3.92 | pass | Beep and click are toy-like against the original's noisier, mechanical texture. |
| `sounds/effects/C4Charge_Plant.ogg` | tweak | 5.76 | pass | Beep and click are toy-like against the original's noisier, mechanical texture. |
| `sounds/effects/C4Charge_Tick.ogg` | accept | 4.40 | pass | Beep and click are toy-like against the original's noisier, mechanical texture. |
| `sounds/effects/DemoCharge_Init.ogg` | accept | 3.69 | pass | Beep and click are toy-like against the original's noisier, mechanical texture. |
| `sounds/effects/DemoCharge_Plant.ogg` | accept | 1.93 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/effects/DemoCharge_Tick.ogg` | accept | 3.88 | pass | Opener rebuilt as an exactly periodic pulse train with eight short ticks; still toy-like in texture. |
| `sounds/effects/biodeath.ogg` | accept | 2.18 | pass | Squelch layer differs; lowering the tail time further trips the decay check. |
| `sounds/effects/bullet_impact_a.ogg` | accept | 3.05 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/effects/bullet_impact_b.ogg` | tweak | 3.11 | pass | Right kind of event, but timbre or texture differs a little from the original; smoother (less noisy) than the original. |
| `sounds/effects/bullet_impact_c.ogg` | tweak | 4.00 | pass | Right kind of event, but timbre or texture differs a little from the original; smoother (less noisy) than the original. |
| `sounds/effects/bullet_impact_d.ogg` | tweak | 3.06 | pass | Right kind of event, but timbre or texture differs a little from the original; smoother (less noisy) than the original. |
| `sounds/effects/bullet_impact_e.ogg` | tweak | 3.14 | pass | Right kind of event, but timbre or texture differs a little from the original; smoother (less noisy) than the original. |
| `sounds/effects/explosion1_b.ogg` | accept | 3.42 | pass | Needs a structural change to the roar and bump layers. |
| `sounds/effects/explosion1_c.ogg` | accept | 4.37 | pass | Needs a structural change to the roar and bump layers. |
| `sounds/effects/explosion1_e.ogg` | accept | 2.02 | pass | Debris grains are less dense and darker than the original's. |
| `sounds/effects/gore_a.ogg` | rework | 2.81 | pass | Splat and crack balance is closer, but the wetter low body is missing. |
| `sounds/effects/gore_b.ogg` | accept | 2.03 | pass | Wet splat texture differs; the noise-draw search plateaued. |
| `sounds/effects/gore_c.ogg` | accept | 3.52 | pass | Wet splat texture differs; the noise-draw search plateaued. |
| `sounds/guns/MIB_blaster_reload.ogg` | tweak | 4.19 | pass | Right kind of event, but timbre or texture differs a little from the original; smoother (less noisy) than the original. |
| `sounds/guns/MIB_blaster_reload_akimbo.ogg` | tweak | 2.62 | pass | Right kind of event, but timbre or texture differs a little from the original; decay time differs. |
| `sounds/guns/ak47_reload.ogg` | rework | 4.57 | pass | Right kind of event, but timbre or texture differs a little from the original; loudness contour differs; hit count or spacing differs; spurious tonal line. |
| `sounds/guns/alienblaster_reload.ogg` | rework | 4.47 | pass | Right kind of event, but timbre or texture differs a little from the original; hit count or spacing differs; decay time differs. |
| `sounds/guns/as50_reload.ogg` | rework | 3.35 | pass | Right kind of event, but timbre or texture differs a little from the original; loudness contour differs; hit count or spacing differs; decay time differs. |
| `sounds/guns/as50_shot_b.ogg` | tweak | 1.17 | pass | Smoother (less noisy) than the original. |
| `sounds/guns/aug_reload.ogg` | accept | 2.40 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/biogun_impact_a.ogg` | tweak | 2.73 | pass | Right kind of event, but timbre or texture differs a little from the original; smoother (less noisy) than the original. |
| `sounds/guns/biogun_impact_b.ogg` | accept | 2.33 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/biogun_impact_c.ogg` | accept | 2.27 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/biogun_reload.ogg` | tweak | 3.60 | pass | Right kind of event, but timbre or texture differs a little from the original; hit count or spacing differs. |
| `sounds/guns/blasterrifle_shot_c.ogg` | accept | 2.13 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/chainsaw_hit_c.ogg` | accept | 2.06 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/chainsaw_loop_b.ogg` | accept | 1.80 | `f0_cents` +961.83 (tol 100.00) | Only strongly pitched gun sound. The judge run read our buzz at 117 Hz against the original's 67 Hz; the generator was retuned to 67 Hz afterward, so the pitch check needs a judge rerun. |
| `sounds/guns/chainsaw_loopstart.ogg` | accept | 1.94 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/chainsaw_swing_a.ogg` | accept | 2.30 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/chainsaw_swing_b.ogg` | accept | 2.91 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/chainsaw_swing_c.ogg` | accept | 2.40 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/combatshotgun_pump.ogg` | accept | 2.23 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/combatshotgun_reload.ogg` | accept | 3.08 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/combatshotgun_shot_a.ogg` | accept | 2.31 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/combatshotgun_shot_b.ogg` | tweak | 1.31 | pass | Smoother (less noisy) than the original. |
| `sounds/guns/grenade_pin_a.ogg` | tweak | 6.94 | pass | The original is a multi-hit cluster over a dark steady tail; ours reproduces neither (hard case). |
| `sounds/guns/grenade_pin_b.ogg` | tweak | 5.95 | pass | Same multi-hit cluster and dark tail as the other pin sound (hard case). |
| `sounds/guns/grenadelauncher_reload.ogg` | accept | 2.09 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/grenadelauncher_shot_a.ogg` | accept | 2.86 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/handgun_reload.ogg` | tweak | 1.89 | pass | Right kind of event, but timbre or texture differs a little from the original; decay time differs. |
| `sounds/guns/handgun_shot_a.ogg` | accept | 1.77 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/handgun_shot_b.ogg` | accept | 1.99 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/handgun_shot_c.ogg` | accept | 2.05 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/lmg_reload.ogg` | tweak | 3.10 | pass | Right kind of event, but timbre or texture differs a little from the original; decay time differs. |
| `sounds/guns/lmg_shot_a.ogg` | accept | 1.86 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/lmg_shot_b.ogg` | accept | 1.80 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/mac10_reload.ogg` | tweak | 2.89 | pass | Right kind of event, but timbre or texture differs a little from the original; loudness contour differs. |
| `sounds/guns/mac10_reload_akimbo.ogg` | tweak | 3.45 | pass | Right kind of event, but timbre or texture differs a little from the original; decay time differs. |
| `sounds/guns/mac10_shot_c.ogg` | tweak | 1.43 | `am_rate_log2` -1.16 (tol 1.00) | Amplitude-modulation rate is slower than the original's fast flutter; the 125 Hz band is also weak. |
| `sounds/guns/minigun_start_a.ogg` | tweak | 2.46 | pass | Right kind of event, but timbre or texture differs a little from the original; loudness contour differs. |
| `sounds/guns/pdw_reload.ogg` | accept | 1.83 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/pistol_reload.ogg` | rework | 3.92 | pass | Right kind of event, but timbre or texture differs a little from the original; loudness contour differs; hit count or spacing differs; decay time differs. |
| `sounds/guns/pistol_reload_akimbo.ogg` | rework | 5.86 | pass | Right kind of event, but timbre or texture differs a little from the original; loudness contour differs; hit count or spacing differs; decay time differs. |
| `sounds/guns/pistol_shot_a.ogg` | accept | 1.75 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/powerhammer_impactGround_a.ogg` | accept | 2.03 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/powerhammer_impactGround_b.ogg` | accept | 1.96 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/powerhammer_impact_a.ogg` | keep | 1.53 | `audible_len_log2` -0.48 (tol 0.28) | Audible tail ends about 0.37 s earlier than the original's and the centroid is darker. |
| `sounds/guns/powerhammer_impact_c.ogg` | accept | 1.79 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/powerhammer_reload.ogg` | tweak | 1.91 | pass | Right kind of event, but timbre or texture differs a little from the original; hit count or spacing differs. |
| `sounds/guns/powerhammer_shot_b.ogg` | rework | 2.68 | pass | Needs a structural recipe: brightness and noisiness both off. |
| `sounds/guns/powerhammer_swing_b.ogg` | accept | 2.06 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/revolver_reload.ogg` | tweak | 1.92 | pass | Right kind of event, but timbre or texture differs a little from the original; decay time differs. |
| `sounds/guns/revolver_reload_akimbo.ogg` | rework | 4.84 | pass | Right kind of event, but timbre or texture differs a little from the original; loudness contour differs; hit count or spacing differs; decay time differs. |
| `sounds/guns/silencedm4_shot_b.ogg` | keep | 1.27 | `centroid_offset_oct` -2.94 (tol 2.85) | Spectrum sits about three octaves darker than the original's. |
| `sounds/guns/sonicshotgun_reload.ogg` | accept | 2.70 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/thompson_reload.ogg` | tweak | 2.93 | pass | Right kind of event, but timbre or texture differs a little from the original; loudness contour differs. |
| `sounds/guns/thompson_shot_a.ogg` | accept | 1.95 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/thompson_shot_b.ogg` | rework | 1.84 | `band_share_max_db` -27.40 (tol 24.92) | The 62 Hz octave band is far weaker than the original's. |
| `sounds/guns/thompson_shot_c.ogg` | accept | 1.75 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/guns/vector_reload.ogg` | rework | 3.70 | pass | Right kind of event, but timbre or texture differs a little from the original; loudness contour differs; hit count or spacing differs; decay time differs. |
| `sounds/guns/vector_shot_a.ogg` | tweak | 1.63 | pass | Top end sits off the original. |
| `sounds/guns/vector_shot_c.ogg` | tweak | 1.24 | pass | Top end sits off the original. |
| `sounds/items/nightvision_off.ogg` | rework | 6.55 | pass | Envelope differs and a spurious tonal line appears (hard case). |
| `sounds/items/nightvision_on.ogg` | rework | 7.29 | pass | Envelope, noisiness and decay all differ; the recipe needs rework (hard case). |
| `sounds/machines/ammopress_work1_a.ogg` | tweak | 1.48 | pass | Smoother (less noisy) than the original. |
| `sounds/machines/ammopress_work1_c.ogg` | accept | 2.68 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/machines/rc_beep.ogg` | accept | 4.43 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/machines/rc_warning.ogg` | rework | 11.32 | pass | Warning ladder with a spurious tonal line; retuned to a 226 Hz base and a lighter sub bed in the clean-room pass. |
| `sounds/npcs/alienbug_aggro_a.ogg` | accept | 1.75 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_aggro_c.ogg` | accept | 2.94 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_aggro_d.ogg` | accept | 2.04 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_bite_a.ogg` | accept | 2.30 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_bite_b.ogg` | accept | 2.18 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_bite_c.ogg` | accept | 2.12 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_bite_d.ogg` | accept | 2.20 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_death_b.ogg` | accept | 1.75 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_hit-03.ogg` | accept | 2.90 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_idle-01.ogg` | accept | 2.19 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_idle-02.ogg` | accept | 2.46 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_idle-03.ogg` | accept | 1.99 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_idle-04.ogg` | accept | 1.96 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_idle-05.ogg` | tweak | 2.95 | pass | Right kind of event, but timbre or texture differs a little from the original; smoother (less noisy) than the original. |
| `sounds/npcs/alienbug_idle-06.ogg` | accept | 2.55 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/alienbug_step_a.ogg` | accept | 2.24 | `energy_t50_log2` -1.39 (tol 1.09) | Energy is front-loaded: half has arrived by 60 ms against 190 ms. |
| `sounds/npcs/alienbug_step_c.ogg` | accept | 2.07 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/apache_burst.ogg` | rework | 3.12 | `rms_active_db` -7.46 (tol 3.89); `energy_t90_log2` +0.96 (tol 0.93) | The original is clipped far above full scale; our -1 dBTP cap cannot reproduce its loudness, and energy arrives later than in the original. |
| `sounds/npcs/apache_die.ogg` | tweak | 2.22 | pass | Right kind of event, but timbre or texture differs a little from the original; decay time differs. |
| `sounds/npcs/apache_explode.ogg` | tweak | 3.38 | pass | A clipped plateau and tail cannot both be matched under the peak cap; energy sits later. |
| `sounds/npcs/apache_hit_b.ogg` | accept | 1.83 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/apache_rotor.ogg` | tweak | 1.03 | `env_corr_loss` +0.91 (tol 0.41) | Passes embedding, but the loop's frame-level envelope wiggle (rotor chop depth) does not correlate with the original's. |
| `sounds/npcs/cyberdemon_aggro_a.ogg` | accept | 3.19 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/cyberdemon_aggro_b.ogg` | tweak | 2.57 | `energy_t50_log2` -1.62 (tol 1.09) | Energy is front-loaded: half has arrived by 240 ms against 780 ms in the original. |
| `sounds/npcs/cyberdemon_aggro_c.ogg` | accept | 1.94 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/cyberdemon_death_a.ogg` | tweak | 3.08 | pass | Right kind of event, but timbre or texture differs a little from the original; smoother (less noisy) than the original. |
| `sounds/npcs/cyberdemon_hurt_c.ogg` | accept | 2.20 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/cyberdemon_idle_a.ogg` | accept | 2.47 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/cyberdemon_idle_b.ogg` | accept | 2.53 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/cyberdemon_idle_c.ogg` | accept | 2.60 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/cyberdemon_step_c.ogg` | accept | 2.44 | pass | Right kind of event, but timbre or texture differs a little from the original. |
| `sounds/npcs/turret_beep-01.ogg` | accept | 2.53 | pass | Tone and haze balance off; the lone-line pitch check (`line_cents`) passes. |
| `sounds/npcs/turret_beep-02.ogg` | rework | 3.83 | `energy_t50_log2` -3.36 (tol 1.09) | The original has a 1.2 kHz body and later energy; our line bank leaves spurious tonal lines (hard case). |
| `sounds/npcs/turret_beep-03.ogg` | rework | 2.92 | pass | Two spurious tonal lines fail the tonality check (hard case). |
| `sounds/npcs/turret_beep-04.ogg` | tweak | 5.59 | pass | Passes every measured check; the original's roughly 15 ms bursts at a 39 ms period over broadband haze match in timing but not timbre (hard case). |
| `sounds/npcs/turret_beep-05.ogg` | accept | 4.06 | `f0_cents` -1024.10 (tol 100.00) | Strong lines at 5.3, 7.8 and 15.6 kHz mean this is not a lone-line alias, so `f0_cents` applies; our fundamental reads about 977 Hz against 1765 Hz (hard case). |
| `sounds/npcs/turret_beep-06.ogg` | rework | 2.65 | `rms_active_db` -6.06 (tol 3.89); `crest_active_db` +6.03 (tol 5.35) | Quieter and peakier than the original; also fails tonality (hard case). |
| `sounds/npcs/turret_beep-07.ogg` | rework | 4.62 | pass | The original is very noisy with a centroid near 14.6 kHz; ours is less noisy and brighter in line. |
| `sounds/npcs/turret_gunequip-03.ogg` | accept | 1.78 | pass | Right kind of event, but timbre or texture differs a little from the original. |

## Open items for revisiting the generators

### Judge failures (13)

Six guns and seven NPC sounds, as of the last judge run (listed in the table above). The chainsaw loop was retuned after that run, so it needs a rerun. `mac10_shot_c`, `flashbang_bang`, `thompson_shot_b` and `silencedm4_shot_b` miss by about 1.2x of tolerance or less on one metric each. `apache_burst` needs a ruling on the peak cap: its original is clipped far above full scale, so either the cap or the loudness tolerance must give.

### Judge rulings in force

- **Crest.** Where an original's sample peak is above -1 dBFS, crest is compared against the crest the original would have with its peak capped at -1 dBFS. Overshoots are never forgiven. The tolerance (5.35 dB) is unchanged. This removed the crest failures on four alien bug sounds and both apache sounds.
- **Lone-line pitch.** When an original's strongest prominent line is above 2 kHz, at least 1.8 times its fundamental, and no other strong peak sits near a multiple of the fundamental, the fundamental is a lag-grid alias. The judge then compares our strongest line with the target line (`line_cents`, 100 cents) instead of `f0_cents`. This applies to `npcs/turret_beep-01` and `-07`; `-05` is not an alias and may be re-disputed with evidence of harmonic structure.

### Known hard cases

- **Turret beeps** (`npcs/turret_beep-01` to `-07`): pitch and the tonality gate conflict; the originals' haze and burst timbre are not matched.
- **Apache** (`npcs/apache_burst`, `apache_explode`, `apache_rotor`): clipped originals, late energy, and a near-flat loop whose frame-level wiggle does not correlate.
- **Grenade pins** (`guns/grenade_pin_a`, `grenade_pin_b`): multi-hit cluster over a dark steady tail. `grenade_pin_a` is also slow to render (about six minutes) because of a scripted search; keep it out of quick full renders.
- **Night vision** (`items/nightvision_on`, `nightvision_off`): envelope, noisiness, decay and a spurious tonal line.

### Shared-code items

- **`blast()` skirts.** The shared shot and explosion engine leaves skirts (energy spread outside the intended band) that the fixers repeatedly had to work around; the low and top octave bands of several shots miss because of them. Needs a look in `sndengines.py`.
- **Conform pinning.** The conforming stage pins band balance and loudness contour toward the measured targets and can fight a recipe that sets its own contour, so recipes now switch it off per sound (`conform_env`, `conform_bands`). A cleaner switch or per-metric pinning would remove the special cases.
- **`apache_rotor` loop crossfade.** The loop's seam dip at the start was fixed, but the envelope correlation of the near-flat loop still fails and probably needs a different modulation model.
- **Peak cap.** The -1 dBTP finish (at most 3.5 dB of limiting, at most 2 LU under target) is the common cause of the crest and loudness failures on clipped originals.
- **Pre-existing drift.** `guns/combatshotgun_shot_c` was committed from an older recipe state; the current recipe renders different bytes. It was left untouched (it is a keep sound), so a full re-render changes it.

### Techniques that worked

- **Gap-fill for reloads:** place the numeric hit list first, then fill the gaps with a quiet texture bed so the loudness contour and audible length match.
- **Band-share tail beds:** a dark or band-limited noise tail whose octave-band shares come from the blueprint carries length and decay.
- **Gate-aware scripted search with baked tables:** search per-sound knobs with a score of embedding distance plus a penalty per failing check, then bake the winners into the `_TUNED*` tables so renders stay deterministic.
- **Blueprint-guided fixes:** read the failing metric, find the layer that owns it (pitch track, envelope track, hit times), change that layer only, and re-render one sound at a time.

### How to rerun

- Generating and finishing sounds: [tools/asset-gen/README.md](../../tools/asset-gen/README.md). Render one sound at a time on Windows; a full pool run can hit a spawn permission error.
- The measured gate, tonality check and embedding distance, and how the thresholds were calibrated: [tools/audio-describe/CALIBRATION.md](../../tools/audio-describe/CALIBRATION.md). The embedding step runs on a GPU server, referred to here as `<audio-server>`.
- The clean-room analyzer, blueprints and judge are kept locally and are not in the repository. They derive from the originals, so committing them would publish the originals' measurements and break the clean-room boundary (see [CONTRIBUTING](../../CONTRIBUTING.md#originality-the-techguns-boundary)). To rerun the judge, rebuild them locally from the original pack.
