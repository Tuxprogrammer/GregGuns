# Sound catalog

`sounds.json` defines 16 events backed by 24 OGG files. Durations come from the last Ogg page granule position divided by the Vorbis sample rate (read with a short script; no audio was played). `ICBMSounds` (`client/ICBMSounds.java`) is the Java enum that registers each event as a `SoundEvent` at registry time (*1.12-only mechanism*) and wraps `world.playSound`: on the client it plays locally with an optional distance delay, on the server it broadcasts to nearby players. The Java category column is the `SoundCategory` passed to `playSound`: BLOCKS for every event except `meep` (NEUTRAL). The `category` field in `sounds.json` reads `block` on every entry and is not what the code uses.

## Events

| Event (`icbmclassic:`) | File(s) | Java category | Duration (s) | Rate / channels | Subtitle key | Trigger |
|---|---|---|---|---|---|---|
| `antimatter` | `antimatter.ogg` | BLOCKS | 10.59 | 48000 Hz stereo | `subtitle.icbmclassic:explosion.antimatter` | `BlastAntimatter:31` (content/blast/threaded): volume 7, pitch 0.9 to 1.0, at blast start |
| `beamcharging` | `beamcharging.ogg` | BLOCKS | 1.10 | 48000 Hz stereo | `subtitle.icbmclassic:beam.charge` | `BlastBeam:63`: volume 4, pitch 0.8, as the beam charges |
| `collapse` | `collapse1.ogg`, `collapse2.ogg`, `collapse3.ogg`, `collapse4.ogg`, `collapse5.ogg`, `collapse6.ogg`, `collapse7.ogg`, `collapse8.ogg` | BLOCKS | 3.09, 2.42, 3.42, 2.42, 1.98, 2.20, 3.20, 3.09 | 44100 Hz stereo | `subtitle.icbmclassic:explosion.collapse` | `RedmatterLogic:115`: 1 in 8 ticks while a red matter blast runs, at a random point within the blast size; volume 5 to 6, pitch 0.6 to 1.0 (only if red matter audio is enabled) |
| `debilitation` | `debilitation.ogg` | BLOCKS | 11.15 | 44100 Hz stereo | `subtitle.icbmclassic:explosion.debilitation` | `BlastGasBase:61`: volume 4, randomized pitch, when the gas blast starts |
| `emp` | `emp.ogg` | BLOCKS | 2.44 | 44100 Hz stereo | `subtitle.icbmclassic:explosion.emp` | `ActionEmpArea:83`: volume 4, randomized pitch, once per EMP pulse |
| `explosion` | `explosion1.ogg` | BLOCKS | 6.64 | 44100 Hz stereo | `subtitle.icbmclassic:explosion` | `BlastNuclear:158,218` and `BlastThermobaric:156,194`: volume 7 at start, volume 10 at the main blast |
| `explosionfire` | `explosionfire.ogg` | BLOCKS | 3.11 | 48000 Hz stereo | `subtitle.icbmclassic:explosion.fire` | `BlastFire:86`: volume 4, randomized pitch |
| `gasleak` | `gasleak.ogg` | BLOCKS | 2.45 | 44100 Hz stereo | `subtitle.icbmclassic:gas.leak` | `BlastGasBase:168`: periodically while a gas cloud is active |
| `machinehum` | `machinehum.ogg` | BLOCKS | 1.00 | 44100 Hz stereo | `subtitle.icbmclassic:machine.hum` | `TileEMPTower:178`: volume 0.5, pitch 0.85 times charge fraction, while the tower is charging |
| `missileinair` | `missileinair_short.ogg` | BLOCKS | 2.39 | 44100 Hz stereo | `subtitle.icbmclassic:missile.movement` | `EntityMissile:124`: every tick the flight logic reports engine effects; volume ramps to 1 over the first 40 ticks, pitch about 0.7 with random variation |
| `missilelaunch` | `missilelaunch1.ogg`, `missilelaunch2.ogg` | BLOCKS | 3.23, 5.03 | 44100 Hz stereo | `subtitle.icbmclassic:missile.launch` | `CapabilityMissile:126`: once at launch, volume 1, pitch about 0.7 with variation |
| `powerdown` | `powerdown.ogg` | BLOCKS | 6.59 | 48000 Hz stereo | `subtitle.icbmclassic:power.down` | `BlastBeam:199`: volume 4, pitch 0.8, as the beam ends |
| `redmatter` | `redmatter.ogg` | BLOCKS | 1.01 | 44100 Hz stereo | `subtitle.icbmclassic:explosion.redmatter` | `RedmatterLogic:118`: every tick of a red matter blast, volume 3, pitch random in [-0.8, 1.2] (the engine clamps pitch) |
| `sonicwave` | `sonicwave.ogg` | BLOCKS | 11.34 | 44100 Hz mono | `subtitle.icbmclassic:explosion.sonicwave` | `BlastSonic:43`: volume 4, pitch about 0.7 |
| `meep` | `meep.ogg` | NEUTRAL | 0.42 | 44100 Hz mono | `subtitle.icbmclassic:meep` | `EntityMissile:149`: when a missile hits a chicken (the chicken boards the missile). Volume 2 |
| `radioactive_tick` | `radioactive_tick.ogg` | BLOCKS | 0.92 | 48000 Hz mono | `subtitle.icbmclassic:radioactive_tick` | `BlockRadioactive:130`: client display tick, 1 in 12 chance, volume 1.0 to 1.5 |

## Mapping from `ICBMSounds` constants to events

| Constant | Event | Notes |
|---|---|---|
| `ANTIMATTER` | `antimatter` | |
| `BEAM_CHARGING` | `beamcharging` | |
| `COLLAPSE` | `collapse` | 8 variants; the engine picks one at random |
| `DEBILITATION` | `debilitation` | |
| `EMP` | `emp` | |
| `EXPLOSION` | `explosion` | one file, `explosion1.ogg` |
| `EXPLOSION_FIRE` | `explosionfire` | |
| `GAS_LEAK` | `gasleak` | |
| `MACHINE_HUM` | `machinehum` | |
| `POWER_DOWN` | `powerdown` | |
| `REDMATTER` | `redmatter` | |
| `SONICWAVE` | `sonicwave` | |
| `MISSILE_LAUNCH` | `missilelaunch` | 2 variants |
| `MISSILE_ENGINE` | `missileinair` | file is `missileinair_short.ogg` |
| `MEEP` | `meep` | NEUTRAL category |
| `RADIOACTIVE_TICK` | `radioactive_tick` | BLOCKS category |

## Vanilla sounds used by ICBM code

These are not in `sounds.json`.

| Vanilla event | Where |
|---|---|
| `ENTITY_GENERIC_EXPLODE` | `Blast.playExplodeSound:319`, `BlastTNT:95`, `BlastBreach:169` |
| `ENTITY_TNT_PRIMED` | bomb cart priming (`EntityBombCart:108`), grenade throw (`ItemGrenade:73`) |
| `ENTITY_ARROW_HIT`, `BLOCK_ANVIL_HIT` | `EntityFragments:390,434,438` (anvil fragments use the anvil sound) |
| `BLOCK_STONE_BUTTON_CLICK_ON/OFF` | `BlockGlassButton:46,52` |
| `BLOCK_LAVA_EXTINGUISH` | `TileEMPTower:182`, while cooling down |
| `ENTITY_ENDERMEN_TELEPORT`, `BLOCK_PORTAL_AMBIENT` | `BlastEnder:136,155` |
| block place sound | `EntityFlyingBlock:219` (landing), `ItemBlockAbstract:62` |

## Files with no event

None. Every OGG in `sounds/` is named by an event. `Credits.md` lists nine more sounds that are not in the repository (`airstrike`, `alarm`, `antigravity`, `hypersonic`, `interface1`, `laser`, `lasershot`, `radaralarm`, `targetlocked`), so the credits table is stale.

## Provenance and licensing

| License (per `Credits.md`) | Files |
|---|---|
| unknown, marked for removal | 21 |
| Benboncan, CC BY 4.0 | 1 |
| primeval_polypod, CC BY 3.0 | 1 |
| ImAFoley, CC0 | 1 |

Only three sounds have a known author: `meep.ogg` (Benboncan, CC BY 4.0), `missileinair_short.ogg` (primeval_polypod, CC BY 3.0) and `radioactive_tick.ogg` (ImAFoley, CC0, edited). The rest are marked "unknown, marked for removal/replacement" with the note "sourced from freesound and likely is free to use". None can be reused without re-checking its license.

## Notes on `sounds.json`

- Every sound entry carries `"sream": false`, a typo of `stream`. 1.12 ignores unknown keys, so the effect is the default (non-streaming) either way (ICBM-07-30).
- The `explosion` event has a single file although `collapse` has eight and `missilelaunch` two.
