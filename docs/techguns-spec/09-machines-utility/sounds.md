# Sounds used by area 09

Every sound event that the utility machines, charges, turret platform, chests and Bio Blob play or define.

- Durations, sample rates and channels are quoted from [ASSET-FACTS.md](../ASSET-FACTS.md).
- Events and categories come from `assets/techguns/sounds.json`.
- Volume and pitch are the values the code passes.
- The audio can't be listened to here. Any "character" note is **inferred from the event name and use**.

Model: [08-machines-production/sounds.md](../08-machines-production/sounds.md). The ore drill files sit in `sounds/machines/` and are also listed there.

## 1. Ore Drill work loop

**Trigger** ([`OreDrillTileEntMaster`](ore-drill.md#5-operation-oredrilltileentmaster), client only):

1. During a tick in which the controller is complete, has a drill, is redstone-enabled, has a running operation with progress < totalTime, and `haspower` is true, the client increments progress.
2. On that same tick it decrements a private delay counter.
3. When the counter is ≤ 0, it plays the event with `World.playSound(x, y, z, event, volume, 1.0, distanceDelay = true)` at the controller's corner coordinates (not the block center), then sets the counter to 61.

The result is one play every 62 powered ticks (≈ 3.1 s). The clips are longer than that, so consecutive plays overlap ([MUTL-46](index.md#suspected-bugs)). The server never plays it.

| Event (`sounds.json`) | Category | Radius | Volume | Pitch | File | Duration | Rate | Ch |
|---|---|---|---|---|---|---|---|---|
| `machines.oredrillSmallWork` | block | 0 | 1.5 | 1.0 | [`sounds/machines/oredrill_s_work_a.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 6.06 s | 176400 Hz | mono |
| | | | | | [`sounds/machines/oredrill_s_work_b.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 6.06 s | 176400 Hz | mono |
| | | | | | [`sounds/machines/oredrill_s_work_c.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 6.06 s | 176400 Hz | mono |
| | | | | | [`sounds/machines/oredrill_s_work_d.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 9.71 s | 176400 Hz | mono |
| `machines.oredrillMediumWork` | block | 1–2 | 2.25 | 1.0 | [`sounds/machines/oredrill_m_work_a.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 4.01 s | 48000 Hz | mono |
| | | | | | [`sounds/machines/oredrill_m_work_b.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 8.43 s | 48000 Hz | mono |
| | | | | | [`sounds/machines/oredrill_m_work_c.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 4.01 s | 48000 Hz | mono |
| | | | | | [`sounds/machines/oredrill_m_work_d.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 5.79 s | 48000 Hz | mono |
| `machines.oredrillLargeWork` | block | 3–4 | 3.0 | 1.0 | [`sounds/machines/oredrill_l_work_a.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 6.93 s | 44100 Hz | mono |
| | | | | | [`sounds/machines/oredrill_l_work_b.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 6.93 s | 8000 Hz | mono |
| | | | | | [`sounds/machines/oredrill_l_work_c.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 6.93 s | 8000 Hz | mono |
| | | | | | [`sounds/machines/oredrill_l_work_d.ogg`](../08-machines-production/sounds.md#1-events-and-files) | 6.93 s | 8000 Hz | mono |

Notes:

- Minecraft picks one file per event at random.
- The small-drill files declare an unusual 176.4 kHz rate. Three of the large-drill files are 8 kHz, so they will sound low-fidelity ([MUTL-47](index.md#suspected-bugs)).
- Character (inferred): mechanical drilling and grinding loops that get heavier with drill size.

## 2. Explosive charges

All are played server-side with `World.playSoundEffect` at the block center (x + 0.5, y + 0.5, z + 0.5).

| Event | Category | Trigger | Volume | Pitch | File | Duration | Rate | Ch |
|---|---|---|---|---|---|---|---|---|
| `tntcharge.plant` | block | TNT charge (meta 6) placed ([`ItemBlockMetaDataMachine.placeBlockAt`](../07-blocks/special-blocks.md#16-itemblockmetadatamachine), after the orientation is stored) | 4.0 | 1.0 | [`sounds/effects/DemoCharge_Plant.ogg`](../04-projectiles-fx/sounds.md) | 0.76 s | 44100 Hz | mono |
| `tntcharge.init` | block | Button 0 (Detonate) accepted on an unarmed TNT charge | 4.0 | 1.0 | [`sounds/effects/DemoCharge_Init.ogg`](../04-projectiles-fx/sounds.md) | 1.91 s | 44100 Hz | mono |
| `tntcharge.tick` | block | Each armed tick where fusetime > 0 and fusetime mod 20 = 0 (once per second) | 4.0 | 1.0 | [`sounds/effects/DemoCharge_Tick.ogg`](../04-projectiles-fx/sounds.md) | 1.00 s | 44100 Hz | mono |
| `c4charge.plant` | block | Advanced charge (meta 8) placed | 4.0 | 1.0 | [`sounds/effects/C4Charge_Plant.ogg`](../04-projectiles-fx/sounds.md) | 1.50 s | 44100 Hz | mono |
| `c4charge.init` | block | Advanced charge armed | 4.0 | 1.0 | [`sounds/effects/C4Charge_Init.ogg`](../04-projectiles-fx/sounds.md) | 0.88 s | 44100 Hz | mono |
| `c4charge.tick` | block | Advanced charge, once per second of fuse | 4.0 | 1.0 | [`sounds/effects/C4Charge_Tick.ogg`](../04-projectiles-fx/sounds.md) | 0.70 s | 44100 Hz | mono |
| vanilla `random.explode` | (vanilla) | Fuse reaches 0 (server) | 4.0 | (1 + (r1 − r2)·0.2)·0.7, about 0.56–0.84 | — | — | — | — |

Character (inferred): "plant" is a placing or sticking sound, "init" an arming beep sequence, "tick" a countdown beep. "Demo" stands for demolition charge.

## 3. Bio Blob

| Event | Category | Trigger | Volume | Pitch | File | Duration | Rate | Ch |
|---|---|---|---|---|---|---|---|---|
| `effects.biodeath` | hostile | Bio Blob bursts: size > 3 after `hitBlob`. Played by [`ProjectileExplosion.doExplosionB`](../04-projectiles-fx/explosions.md#projectileexplosion) at the blob center via `playSoundEffect`. That method runs only on the server side of `doExplosion`. | 4.0 | (1 + (r1 − r2)·0.2)·0.7 | [`sounds/effects/biodeath.ogg`](../04-projectiles-fx/sounds.md) | 3.99 s | 32000 Hz | mono |

The file is shared with the bio death effect. It is owned by [04-projectiles-fx](../04-projectiles-fx/index.md) / [10-entities-npcs](../10-entities-npcs/index.md).

## 4. Turret

| Event | Category | Trigger | File | Duration | Rate | Ch |
|---|---|---|---|---|---|---|
| `npcs.turretBeep` | hostile | **Never played** (no code reference; [MUTL-28](index.md#suspected-bugs)) | [`sounds/npcs/turret_beep-01.ogg`](../10-entities-npcs/sounds.md#event-table) | 2.24 s | 48000 Hz | mono |
| | | | [`sounds/npcs/turret_beep-02.ogg`](../10-entities-npcs/sounds.md#complete-file-list-54) | 0.62 s | 48000 Hz | mono |
| | | | [`sounds/npcs/turret_beep-03.ogg`](../10-entities-npcs/sounds.md#complete-file-list-54) | 0.84 s | 48000 Hz | mono |
| | | | [`sounds/npcs/turret_beep-04.ogg`](../10-entities-npcs/sounds.md#complete-file-list-54) | 0.55 s | 48000 Hz | mono |
| | | | [`sounds/npcs/turret_beep-05.ogg`](../10-entities-npcs/sounds.md#complete-file-list-54) | 0.32 s | 48000 Hz | mono |
| | | | [`sounds/npcs/turret_beep-06.ogg`](../10-entities-npcs/sounds.md#complete-file-list-54) | 0.31 s | 48000 Hz | mono |
| | | | [`sounds/npcs/turret_beep-07.ogg`](../10-entities-npcs/sounds.md#complete-file-list-54) | 0.26 s | 48000 Hz | mono |
| `npcs.turretGunequip` | hostile | **Never played** | [`sounds/npcs/turret_gunequip-01.ogg`](../10-entities-npcs/sounds.md#event-table) | 0.81 s | 48000 Hz | mono |
| | | | [`sounds/npcs/turret_gunequip-02.ogg`](../10-entities-npcs/sounds.md#complete-file-list-54) | 0.88 s | 48000 Hz | mono |
| | | | [`sounds/npcs/turret_gunequip-03.ogg`](../10-entities-npcs/sounds.md#complete-file-list-54) | 0.50 s | 48000 Hz | mono |

Sounds the turret **does** make come from the [`NPCTurret`](../10-entities-npcs/ai.md#5-turret-npcturret) entity (owned by [10](../10-entities-npcs/index.md)), using vanilla living-entity volume and pitch:

| Situation | Sound |
|---|---|
| hurt | `mob.irongolem.hit` |
| death | `mob.irongolem.death` |
| ambient | none |
| step | none (step sounds are suppressed) |

The base block itself plays no sound. Shots use the mounted gun's fire sound ([02](../02-guns/index.md)/[03](../03-gun-visuals-audio/index.md)).

Character (inferred): sentry beeps for target acquisition, and a weapon-mount clack.

## 5. Chests

The vanilla chest logic in [`TGChestTileEnt.updateEntity`](misc-tile-entities.md#tile-entity-tgchesttileent) plays these on both sides, centered on a double chest:

| Event | Trigger | Volume | Pitch |
|---|---|---|---|
| vanilla `random.chestopen` | The viewer count is > 0, the lid angle is 0, and this tile is the primary (−X / −Z) half | 0.5 | 0.9–1.0 random |
| vanilla `random.chestclosed` | The lid angle drops below 0.5 this tick, primary half | 0.5 | 0.9–1.0 random |

## 6. Things with no sounds

Repair Bench, Camo Bench, Scrapper, Camp Flag, the Ore Drill slaves, and the legacy [`OreDrillTileEnt`](entity-sheets.md#10-legacy-oredrill--oredrilltileent-dead-code): **none**. Block step and break sounds use the block's step sound: metal for [`BasicMachine`](../08-machines-production/blocks-and-items.md#1-basicmachine-registry-name-basicmachine) and [`oreDrill`](../07-blocks/block-catalog.md#registered-blocks), wood for the chests, and the default stone for [`Bioblob`](../07-blocks/special-blocks.md#13-bio-blob-bioblobsmall-bioblob).

## 7. Consistency-pass summary

Per-file measurements above were checked against [ASSET-FACTS.md](../ASSET-FACTS.md). All 12 ore drill files, 6 charge files, 1 Bio Blob file and 10 turret files listed here match it (duration, rate, channels, event and category). No numeric correction was needed in this file.

| Group | Files | Events | Played by this area? |
|---|---|---|---|
| Ore drill work | 12 | 3 | yes, client, 3 events |
| Explosive charges | 6 | 6 | yes, server |
| Bio Blob death | 1 | 1 | yes, via `ProjectileExplosion` |
| Turret beeps and gun equip | 10 | 2 | no (MUTL-28) |
| Chests (vanilla) | 0 | 2 vanilla events | yes, both sides |
| Vanilla `random.explode` | 0 | 1 | yes, charges |

Step and break sounds (block `stepSound`, not `sounds.json` events):

| Block | Sound type |
|---|---|
| [`basicMachine`](../07-blocks/block-catalog.md#registered-blocks) (all area-09 metas) | metal |
| `oreDrill` | metal |
| [`tgchest`](../07-blocks/block-catalog.md#registered-blocks), [`tgchest_weapon`](../07-blocks/block-catalog.md#registered-blocks) | wood |
| `Bioblob` | default (stone), since none is set |

The ore drill rate oddities are recorded as MUTL-47. The 8 kHz and 176.4 kHz rates are copied from the Ogg headers and were not listened to; whether the engine resamples them cleanly was not tested here.
