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

## 4. Per-file facts (all 35 files)

Source: `ASSET-FACTS.md`, read from the Ogg Vorbis headers. Every file is **mono**, and every `sounds.json` event is in category `block` (so the Blocks slider in the sound options controls them). The sample rate is the rate stored in the header; Minecraft resamples on playback.

| File (`sounds/machines/…`) | Event | Duration | Sample rate | Channels | Category |
|---|---|---|---|---|---|
| `ammopress_work1_a.ogg` | `machines.ammopressWork1` | 2.57 s | 44100 Hz | mono | block |
| `ammopress_work1_b.ogg` | `machines.ammopressWork1` | 2.77 s | 44100 Hz | mono | block |
| `ammopress_work1_c.ogg` | `machines.ammopressWork1` | 2.06 s | 44100 Hz | mono | block |
| `ammopress_work2_a.ogg` | `machines.ammopressWork2` | 1.06 s | 44100 Hz | mono | block |
| `ammopress_work2_b.ogg` | `machines.ammopressWork2` | 1.35 s | 44100 Hz | mono | block |
| `ammopress_work2_c.ogg` | `machines.ammopressWork2` | 1.14 s | 44100 Hz | mono | block |
| `charginstation_work.ogg` (sic) | `machines.chargingstationwork` | 2.08 s | 44100 Hz | mono | block |
| `chemlab_work_a.ogg` | `machines.chemlabWork` | 3.50 s | 44100 Hz | mono | block |
| `chemlab_work_b.ogg` | `machines.chemlabWork` | 3.50 s | 44100 Hz | mono | block |
| `chemlab_work_c.ogg` | `machines.chemlabWork` | 3.50 s | 44100 Hz | mono | block |
| `chemlab_work_d.ogg` | `machines.chemlabWork` | 3.50 s | 44100 Hz | mono | block |
| `fabricator_work_a.ogg` | `machines.fabricatorWork` | 5.58 s | 44100 Hz | mono | block |
| `fabricator_work_b.ogg` | `machines.fabricatorWork` | 5.58 s | 44100 Hz | mono | block |
| `fabricator_work_c.ogg` | `machines.fabricatorWork` | 5.58 s | 44100 Hz | mono | block |
| `metalpress_small_work_a.ogg` | `machines.metalpressWork` | 2.25 s | 44100 Hz | mono | block |
| `metalpress_small_work_b.ogg` | `machines.metalpressWork` | 2.25 s | 44100 Hz | mono | block |
| `metalpress_small_work_c.ogg` | `machines.metalpressWork` | 2.25 s | 44100 Hz | mono | block |
| `metalpress_work_a.ogg` | `machines.metalpressLargeWork` (unused) | 2.98 s | **8000 Hz** | mono | block |
| `metalpress_work_b.ogg` | `machines.metalpressLargeWork` (unused) | 2.98 s | 44100 Hz | mono | block |
| `metalpress_work_c.ogg` | `machines.metalpressLargeWork` (unused) | 2.98 s | 44100 Hz | mono | block |
| `oredrill_s_work_a.ogg` | `machines.oredrillSmallWork` | 6.06 s | 176400 Hz | mono | block |
| `oredrill_s_work_b.ogg` | `machines.oredrillSmallWork` | 6.06 s | 176400 Hz | mono | block |
| `oredrill_s_work_c.ogg` | `machines.oredrillSmallWork` | 6.06 s | 176400 Hz | mono | block |
| `oredrill_s_work_d.ogg` | `machines.oredrillSmallWork` | 9.71 s | 176400 Hz | mono | block |
| `oredrill_m_work_a.ogg` | `machines.oredrillMediumWork` | 4.01 s | 48000 Hz | mono | block |
| `oredrill_m_work_b.ogg` | `machines.oredrillMediumWork` | 8.43 s | 48000 Hz | mono | block |
| `oredrill_m_work_c.ogg` | `machines.oredrillMediumWork` | 4.01 s | 48000 Hz | mono | block |
| `oredrill_m_work_d.ogg` | `machines.oredrillMediumWork` | 5.79 s | 48000 Hz | mono | block |
| `oredrill_l_work_a.ogg` | `machines.oredrillLargeWork` | 6.93 s | 44100 Hz | mono | block |
| `oredrill_l_work_b.ogg` | `machines.oredrillLargeWork` | 6.93 s | **8000 Hz** | mono | block |
| `oredrill_l_work_c.ogg` | `machines.oredrillLargeWork` | 6.93 s | **8000 Hz** | mono | block |
| `oredrill_l_work_d.ogg` | `machines.oredrillLargeWork` | 6.93 s | **8000 Hz** | mono | block |
| `rc_beep.ogg` | `machines.rc_beep` | 0.11 s | 44100 Hz | mono | block |
| `rc_heatray_work.ogg` | `machines.rc_heatrayWork` | 4.00 s | 44100 Hz | mono | block |
| `rc_warning.ogg` | `machines.rc_warning` | 1.35 s | 22050 Hz | mono | block |

Notes on the sample rates:
- **Ore Drill, small (`oredrill_s_work_*`).** The headers say **176400 Hz** (4 x 44100) for all four files. This agrees with `ASSET-FACTS.md`, so the rate quoted in section 1 is already right and nothing was changed. (The pass brief suggested these files were wrong; the measured table doesn't support that.) Minecraft resamples on playback.
- **Ore Drill, large (`oredrill_l_work_*`).** Only `_a` is 44100 Hz. `_b`, `_c` and `_d` are **8000 Hz** files of the same 6.93 s length. An 8 kHz rate can carry nothing above 4 kHz, so they'd sound duller than `_a` (inferred from the rate, not heard).
- **Metal Press large (`metalpress_work_*`).** `_a` is 8000 Hz, `_b` and `_c` are 44100 Hz. The whole event is unused (nothing calls it).
- **Reaction Chamber warning (`rc_warning.ogg`)** is the only 22050 Hz file in the area.
- The large drill and the large metal press are the only events whose variants don't share a sample rate. Minecraft picks a variant at random, so tonal quality could vary between plays (inferred).

## 5. Per-event call parameters (volume, pitch, side, exact condition)

Every call is `World.playSound(x, y, z, name, volume, pitch, distanceDelay)` using the **integer** tile coordinates (block corner). The `techguns:` prefix is part of the name string in code; `sounds.json` keys have no prefix. In 1.7.10 a volume above 1 doesn't play louder at close range; it **extends the audible radius** (the base radius is 16 blocks, multiplied by the volume). So volume 3.0 on the large drill means roughly 48 blocks.

| Event | Caller | Volume | Pitch | Distance delay | Exact trigger |
|---|---|---|---|---|---|
| `machines.ammopressWork1` | [`AmmoPressTileEnt`](ammo-press.md) ambient-sound routine, run from `updateEntity` on both sides | 1.0 | 1.0 | true | `progress == round(0.05·totalTime)` or that + `round(0.5·totalTime)` (ticks 5 and 55 of 100) |
| `machines.ammopressWork2` | same | 1.0 | 1.0 | true | `progress == round(0.30·totalTime)` or that + half (ticks 30 and 80). It is an `else if`, so it never shares a tick with Work1 |
| `machines.metalpressWork` | [`MetalPressTileEnt`](framework.md#1-class-hierarchy) ambient-sound routine | 1.0 | 1.0 | true | `progress == round(0.075·totalTime)` or that + half (ticks 8 and 58). A `tile.piston.in` call at 25 % is commented out |
| `machines.chemlabWork` | [`ChemLabTileEnt`](chem-lab.md) ambient-sound routine | 1.0 | 1.0 | true | `progress == 1` or `1 + round(0.5·totalTime)` (ticks 1 and 51), client only, while working and powered |
| `machines.fabricatorWork` | [`FabricatorTileEntMaster`](fabricator.md) ambient-sound routine | 1.0 | 1.0 | true | `progress == 1` (once per operation), client only |
| `machines.chargingstationwork` | [`ChargingStationTileEnt`](charging-station.md) effects routine | 1.0 | 1.0 | true | a countdown `lastsound` that reaches 0 is reset to 20 and the sound plays; the routine runs once per tick of activity, client only |
| `machines.rc_heatrayWork` (Heat Ray focus) or `machines.chemlabWork` (UV Emitter focus) | [`ReactionChamberTileEntMaster`](framework.md#1-class-hierarchy) ambient-sound routine | 1.0 | 1.0 | true | the name comes from the focus registered in [`ReactionBeamFocus`](reaction-chamber.md#beam-foci-reactionbeamfocus). Played whenever the client-side `nextTick` hits 59 and a known focus is in slot 1 |
| `machines.rc_beep` | same routine | 1.0 | 1.0 | true | same moment, when the setting equals the operation's preferred intensity |
| `machines.rc_warning` | same routine | 1.0 | 1.0 | true | same moment, when it doesn't |
| `machines.oredrillSmallWork` | [`OreDrillTileEntMaster`](../09-machines-utility/ore-drill.md#5-operation-oredrilltileentmaster) ambient-sound routine | 1.5 | 1.0 | true | `radius == 0`; a counter is reset to 61 after each play, giving one play per 62 ticks (3.1 s) |
| `machines.oredrillMediumWork` | same | 2.25 | 1.0 | true | `radius` 1 to 2, same counter |
| `machines.oredrillLargeWork` | same | 3.0 | 1.0 | true | `radius >= 3`, same counter |
| `machines.metalpressLargeWork` | none | n/a | n/a | n/a | unused event |

Timing observations:
- The Ammo Press, Metal Press and Chem Lab schedules are written as fractions of `totalTime`, but `totalTime` is always 100 for these machines, so the ticks are fixed.
- The ore drill repeats every 62 ticks (3.1 s), which is shorter than every drill clip (4.0 to 9.7 s), so successive plays **overlap**.
- No pitch or volume is randomised in any of these calls.

## 6. Sounds in this area that are commented out or vanilla

| Sound | Where | Status |
|---|---|---|
| `minecraft:game.player.swim.splash` (vol 1, pitch 1, no delay) | `ChemLabTileEnt`, when a drained tank fills a container | **commented out**, never plays |
| `tile.piston.in` (vol 1, pitch 1) | `MetalPressTileEnt`, at 25 % of the operation | **commented out** |
| `random.explode` (vol 4.0, pitch 0.7·(1 + 0.2·(rand − rand))) | `ReactionChamberTileEntMaster`, low-risk explosion | active (vanilla event) |
| `gui.button.press` | vanilla `GuiButton` click handling | active for every standard button in the machine GUIs |

## 7. Caveat on descriptions

The audio can't be listened to here. Section 2 and the notes in section 4 are inferences from names, durations, sample rates and call timing only. Treat any statement about what a clip sounds like as a hypothesis for whoever does replacement work.
