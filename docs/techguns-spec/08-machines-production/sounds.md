# Machine sounds (`sounds/machines/`, all 35 files)

All the files are mono Ogg Vorbis. Durations and sample rates were read from the Ogg headers. Every event is in category `block`. When a sounds.json event lists several files, Minecraft picks one at random each time it plays.

All playback in this area uses `World.playSound(x, y, z, event, volume, pitch, distanceDelay=true)` at the machine's block coordinates (the corner, not the centre). This call **is local-only**: it plays only on the side that calls it. The Ammo Press and Metal Press call it from both sides, so only the client call is audible. The others call it client-side only.

## 1. Events and files

| sounds.json event | Files (`sounds/machines/…`) | Duration / rate | Played by | When |
|---|---|---|---|---|
| `machines.ammopressWork1` | `sounds/machines/ammopress_work1_a.ogg`, `sounds/machines/ammopress_work1_b.ogg`, `sounds/machines/ammopress_work1_c.ogg` | 2.57 s / 2.77 s / 2.06 s, 44.1 kHz | Ammo Press | progress ticks 5 and 55 of each 100-tick operation (vol 1, pitch 1) |
| `machines.ammopressWork2` | `sounds/machines/ammopress_work2_a.ogg`, `sounds/machines/ammopress_work2_b.ogg`, `sounds/machines/ammopress_work2_c.ogg` | 1.06 s / 1.35 s / 1.14 s, 44.1 kHz | Ammo Press | progress ticks 30 and 80 |
| `machines.metalpressWork` | `sounds/machines/metalpress_small_work_a.ogg`, `sounds/machines/metalpress_small_work_b.ogg`, `sounds/machines/metalpress_small_work_c.ogg` | 2.25 s each, 44.1 kHz | Metal Press | progress ticks 8 and 58 |
| `machines.metalpressLargeWork` | `sounds/machines/metalpress_work_a.ogg` (8 kHz), `sounds/machines/metalpress_work_b.ogg`, `sounds/machines/metalpress_work_c.ogg` (44.1 kHz) | 2.98 s each | nothing | **unused** event |
| `machines.chemlabWork` | `sounds/machines/chemlab_work_a.ogg`, `sounds/machines/chemlab_work_b.ogg`, `sounds/machines/chemlab_work_c.ogg`, `sounds/machines/chemlab_work_d.ogg` | 3.50 s each, 44.1 kHz | Chem Lab; Reaction Chamber (UV Emitter focus sound) | Chem Lab: progress ticks 1 and 51 (client). RC: once per reaction tick (every 60 ticks) while the UV Emitter is in the focus slot |
| `machines.fabricatorWork` | `sounds/machines/fabricator_work_a.ogg`, `sounds/machines/fabricator_work_b.ogg`, `sounds/machines/fabricator_work_c.ogg` | 5.58 s each, 44.1 kHz | Fabricator | progress tick 1 of each operation (client) |
| `machines.chargingstationwork` | `sounds/machines/charginstation_work.ogg` (sic) | 2.08 s, 44.1 kHz | Charging Station | every 20th tick of activity (recipe progress or item charging), client |
| `machines.rc_heatrayWork` | `sounds/machines/rc_heatray_work.ogg` | 4.00 s, 44.1 kHz | Reaction Chamber | once per reaction tick (60 ticks) while the Heat Ray focus is in the focus slot |
| `machines.rc_beep` | `sounds/machines/rc_beep.ogg` | 0.11 s, 44.1 kHz | Reaction Chamber | each reaction tick when the intensity setting equals the current preferred intensity ("on target" confirmation) |
| `machines.rc_warning` | `sounds/machines/rc_warning.ogg` | 1.35 s, 22.05 kHz | Reaction Chamber | each reaction tick when the intensity is off target |
| `machines.oredrillSmallWork` | `sounds/machines/oredrill_s_work_a.ogg` (6.06 s), `sounds/machines/oredrill_s_work_b.ogg` (6.06 s), `sounds/machines/oredrill_s_work_c.ogg` (6.06 s), `sounds/machines/oredrill_s_work_d.ogg` (9.71 s); headers say 176.4 kHz | | Ore Drill ([09](../09-machines-utility/index.md)) | while drilling with radius 0, every 62 ticks, volume 1.5 |
| `machines.oredrillMediumWork` | `sounds/machines/oredrill_m_work_a.ogg` (4.01 s), `sounds/machines/oredrill_m_work_b.ogg` (8.43 s), `sounds/machines/oredrill_m_work_c.ogg` (4.01 s), `sounds/machines/oredrill_m_work_d.ogg` (5.79 s); 48 kHz | | Ore Drill ([09](../09-machines-utility/index.md)) | radius 1–2, every 62 ticks, volume 2.25 |
| `machines.oredrillLargeWork` | `sounds/machines/oredrill_l_work_a.ogg` (6.93 s, 44.1 kHz), `sounds/machines/oredrill_l_work_b.ogg`, `sounds/machines/oredrill_l_work_c.ogg`, `sounds/machines/oredrill_l_work_d.ogg` (6.93 s, 8 kHz) | | Ore Drill ([09](../09-machines-utility/index.md)) | radius ≥3, every 62 ticks, volume 3.0 |

File count: ammo press 6, metal press 6, chem lab 4, fabricator 3, charging station 1, reaction chamber 3, ore drill 12, giving **35**.

## 2. Character, for replacement work (inferred from the event names and timing)

- **Ammo press.** Work1 is a longer mechanical clunk-and-whirr (about 2–3 s). Work2 is a short stamping hit (about 1 s). Two of each per 5-second cycle.
- **Metal press** (small). A 2.25 s hydraulic press stroke, twice per cycle. "Large" is a heavier 3 s variant that's never used.
- **Chem lab.** A 3.5 s bubbling/liquid loop, twice per cycle.
- **Fabricator.** A 5.6 s machining sequence (drill, tube and laser) that roughly covers the whole 5 s operation.
- **Charging station.** A 2 s electrical hum, repeated every second while active (the clips overlap).
- **Reaction chamber.** A 4 s heat-ray beam hum per 3 s reaction tick; a very short beep as the "in tune" cue; a 1.35 s alarm as the "out of tune" cue.

## 3. Other sounds played by machines in this area

- `random.explode` (vanilla) at volume 4, pitch 0.7·(1 ± 0.2·random): Reaction Chamber low-risk explosion. The medium explosion uses vanilla `createExplosion`, which plays its own sound.
- Vanilla `gui.button.press` comes from the standard buttons.
