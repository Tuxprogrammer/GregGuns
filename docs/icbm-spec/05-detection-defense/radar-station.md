# Radar Station

Source root for all paths: `src/main/java/icbm/classic/` at `9c1d985a`. The radar map and registry live in `lib/radar/`. Related areas: the radio bus is in `01-core`, missile flight and SAM in `03-missiles`, launchers that react to the radar message in `04-launch-control`.

## 1. Identity

| Item | Value |
|---|---|
| Classes | `content/blocks/radarstation/BlockRadarStation`, `TileRadarStation`, `RadioRadar`, `EnumRadarState`, `PropertyRadarState`, `RadarObjectType` (unused), `data/RadarRenderData`, `RadarRenderDot`, `RadarDotType`, `gui/ContainerRadarStation`, `GuiRadarStation`, `RadarComponent`; `lib/radar/RadarMap`, `RadarRegistry`, `RadarEntity` |
| Registry name | block `icbmclassic:radarstation`, tile `icbmclassic:radarstation` (the block constructor passes the name `radarStation`, with a TODO to rename it `radar_screen`) |
| English name | "Radar Station", `tile.icbmclassic:radarstation.name`; tooltip `.info` = "Detects missiles in flight" |
| Meta | none (one block). Blockstate properties are a *1.12-only mechanism*: `redstone` (bool), the rotation property, and `type` = `off`/`on`/`warning`/`danger` |
| Creative tab | the ICBM tab |
| Oredict | none |

## 2. Obtaining

Shaped, 2 rows: `g c s` over `p c p`. g = `ingotGold`, c = `circuitAdvanced`, s = `icbmclassic:launcherscreen`, p = `plateSteel`. Result 1 (`recipes/machine/radar.json`). Recipes belong to `06-blocks-items`.

## 3. Numbers

| Name | Default | Notes |
|---|---|---|
| Detection range (per tile) | 100 | NBT `detection_range`; the scan uses `min(range, MAX_RANGE)` |
| Trigger range (per tile) | 50 | NBT `safetyRadius`; **never read by the scan** (ICBM-05-04) |
| Redstone output on | true | NBT `emitAll` |
| `ConfigRadar.MAX_RANGE` | 500 | config `icbmclassic/radar`, key `max_range` |
| `POWER_CAPACITY` | 20,000 FE | `power_capacity` |
| `POWER_COST` | 1,000 FE per tick | `power_cost` |
| `SCAN_TICKS` | 1 | `scan_rate`, ticks between scans |
| `SAM_TICKS` | 40 | `sam_delay`, ticks between launch messages |
| Redstone strength | `min(15, number of incoming threats)` | strong power on all six sides, only while the output flag is on |

`ConfigMain.REQUIRES_POWER` false makes every power check pass. Energy is Forge Energy, a *1.12-only mechanism*; GregGuns re-bases it on EU (see `docs/gregguns-icbm-spec`). Block hardness comes from `BlockICBM` (`06-blocks-items`).

## 4. Behavior

### Tick (server only)

1. `hasPower` = the buffer pays `POWER_COST`. This is a real extraction (the call's flag means "simulate", and it is passed false; see ICBM-05-03).
2. If powered and `ticks % SCAN_TICKS == 0`, run the scan.
3. If powered, the radio channel is not the empty channel (`EMPTY_HZ`) and `incomingThreats` is non-empty: count `firingCooldown` down. At 0, reset it to `SAM_TICKS` and broadcast an `IncomingMissileMessage(channel, nearest incoming missile, shouldTrigger=true)` on the radio bus (`01-core`). Only list element 0 is sent.
4. If unpowered, clear both threat lists (the render dots are not cleared).
5. `shouldBeOn = hasPower && incoming non-empty`. When it differs from the block's `redstone` property, set the blockstate and notify all six neighbors.
6. Server-side visual state = `getRadarState()`: OFF when it cannot pay; DANGER when incoming is non-empty; WARNING when only detected threats exist; ON otherwise. A change forces a block re-render and goes to clients in the `description` packet.

The battery slot discharges into the buffer every tick (slot 0 accepts any energy item). `markDirty` queues a description packet.

### Scan / detection algorithm: MIT-portable

Permalinks:
- Scan and classification: [`TileRadarStation.java#L217-L257`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blocks/radarstation/TileRadarStation.java#L217-L257)
- Incoming test: [`TileRadarStation.java#L292-L307`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/content/blocks/radarstation/TileRadarStation.java#L292-L307)
- Spatial query: [`RadarMap.java#L276-L292`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/lib/radar/RadarMap.java#L276-L292)
- Spatial add: [`RadarMap.java#L113-L164`](https://github.com/BuiltBrokenModding/ICBM-Classic/blob/9c1d985a375adc2868ff47707b3a636452d0b63a/src/main/java/icbm/classic/lib/radar/RadarMap.java#L113-L164)

**Radar map (one per dimension, server only).** Missiles register themselves on launch (`content/missile/entity/CapabilityMissile`, line 123) and remove themselves in `EntityMissile.setDead` (line 183). The map is a hash from chunk coordinate to a list of tracked entities, plus a flat list. Chunk key = `(int)x >> 4`, `(int)z >> 4`. The world tick (END phase, server) drops empty maps and otherwise calls `update`. Every 20 ticks, `update` removes and re-adds entries to catch chunk moves and prune dead or invalid ones (but see ICBM-05-02). Unloading a chunk drops that chunk's entries; unloading the world drops the map. While the map is iterating, add and remove calls are refused with a logged warning.

**Query.** Build a box centered on the station block center with half-width `R = min(detectionRange, MAX_RANGE)`, spanning y from 0 to 255 (`ICBMClassic.MAP_HEIGHT`). Return every tracked entity whose chunk lies in the box's chunk range, widened by 1 chunk on every side. The per-entity box test is dead code (ICBM-05-01), so the effective area is a chunk-aligned square reaching `R + 16..32` blocks, not a circle, and the y range is unbounded.

**Classification of each returned entity:**

```
threat   = not a SAM missile AND has Missile capability AND has Explosive capability
missile  = the capability handle; skip if ticksInAir <= 1
approaching = alive AND dist(mpos + motion, radarBlockCorner) < dist(mpos, radarBlockCorner)
approaching -> incoming list, else -> detected list
sort both lists by distance() (broken: ICBM-05-05)
```

"Approaching" uses only the distance trend to the block corner (not the center). It ignores `triggerRange`, the landing point and the flight logic, so any missile flying toward the station inside detection range counts, even one that will pass it. Only missiles carrying an explosive are tracked. The scan clears and refills both lists each time and rebuilds the GUI dots only if a player has the GUI open.

### Interactions

| Action | Effect |
|---|---|
| Right-click holding redstone dust (server) | Toggle `outputRedstone`; chat `message.radar.redstone.on` / `.off`. Nothing is consumed. If the tile is missing, a hard-coded red chat message "Unexpected error: Couldn't access radar station tile" |
| Right-click, anything else (server) | Open GUI id 0 |
| Redstone | Emits weak and strong power as above; `canConnectRedstone` follows the output flag |
| Automation | Energy capability, only if `REQUIRES_POWER` (*1.12-only mechanism*). The battery slot is not exposed |
| Break | `dropInventory` is true, so the battery drops |

## 5. Persistence and sync

NBT (`TileRadarStation` save logic):

| Key | Type | Meaning |
|---|---|---|
| `inventory` | compound | 1 slot, energy items |
| `radio` | compound | channel and disabled flag (`01-core`) |
| `emitAll` | bool | redstone output |
| `detection_range` | int | |
| `safetyRadius` | int | trigger range |
| `energy` | int | FE stored |
| `sam_delay` | int | `firingCooldown` |
| `NBTConstants.FREQUENCY` | int | read-only legacy: becomes the channel string |

Not saved (rebuilt by the next scan): the threat lists, the render dots, the visual state.

Packets (`PacketCodexTile`, a *1.12-only mechanism*; the fields are what matter):

| Name | Direction | Fields |
|---|---|---|
| `description` | server to clients in range | radar state enum |
| `gui` | server to GUI users every 3 ticks | trigger int, detection int, channel string, radio-disabled bool, redstone bool, dots (count int; per dot: x int, y int, type byte) |
| `range.trigger`, `range.detection` | client to server | int (no clamping; ICBM-05-06) |
| `redstone.output` | client to server | toggle |
| radio frequency, radio disable | client to server | channel string; toggle |

## 6. UI

GUI texture `textures/gui/gui_radar.png` (256 x 256). Container 175 x 184. Battery slot (141, 47). Player inventory origin (7, 102). Shift-click moves energy items into slot 0 and everything else out.

| Element | Position (x, y, w, h) |
|---|---|
| Frequency text field | 135, 17, 34, 12 |
| Trigger range int field | 18, 77, 29, 12 |
| Detection range int field | 49, 77, 29, 12 |
| Redstone toggle button | 160, 3 (relative to the GUI) |
| Energy bar | 141, 66, with a per-tick cost marker of 1,000 |
| Radar canvas | 5, 18, 55 x 55 px (54 UV plus 1) |
| Canvas counters | hostile count at (+56, +2) magenta; incoming count at (+56, +14) red; grid spacing in meters at (+56, +46) |
| Title | "Radar Station" in gray at (30, 6) |

Canvas drawing: black background. Green grid lines with `meterSpacing = floor(range / 100 * 40)` meters per cell (0 for ranges below 3; ICBM-05-06). A cyan square marks the trigger range. Dots: markers 1 px, hostile 2x2 magenta, incoming 2x2 red. A dot is placed at `floor(offset / detectionRange * 27)` px from the center, so contacts beyond range (the box overshoot, ICBM-05-01) draw outside the canvas.

Tooltips: range (shift for detail), radio, redstone on/off. Strings are in the Lang table.

## 7. Visuals

- Block model `models/block/radar_screen.json`; blockstate `blockstates/radarstation.json` (`forge_marker`). The north-face texture swaps by state: `textures/blocks/screen/radar_off.png`, `radar.png` (on), `radar_warning.png`, `radar_danger.png`. All are 16 x 16.
- No TESR. The state flows from the `type` blockstate property (*1.12-only mechanism*).
- No particles.

## 8. Audio

None.

## 9. Relationships

- Uses: the radio bus (`01-core`) for `IncomingMissileMessage`; the missile and explosive capabilities (`03-missiles`, `02-explosives`); the launcher screen item in the recipe (`04-launch-control`).
- Used by: SAM launchers listen for the broadcast (`03-missiles`, `04-launch-control`); the EMP tower fires when a broadcast missile is within its range ([emp-tower.md](emp-tower.md)).
- GTNH integration: see `docs/gregguns-icbm-spec`.

## 10. Bugs

ICBM-05-01, 05-02, 05-03, 05-04, 05-05, 05-06. Details in [index.md](index.md#suspected-bugs).

## Lang

| Key | English |
|---|---|
| `tile.icbmclassic:radarstation.name` | Radar Station |
| `tile.icbmclassic:radarstation.info` | Detects missiles in flight |
| `gui.icbmclassic:radar.name` | Radar Station |
| `gui.icbmclassic:radar.range` | Range Settings: / Trigger range & Detection range. / (aqua) Shift for more info |
| `gui.icbmclassic:radar.range.shift` | Trigger range will define threat area. Detection range will define radar max area of operation. Only detects flying entities of known type. Will trigger redstone and SAM missiles in response. |
| `gui.icbmclassic:radar.redstone.off` | Redstone Off: Click to enable outputting redstone for incoming threats |
| `gui.icbmclassic:radar.redstone.on` | Redstone On: Click to disable outputting redstone for incoming threats |
| `message.radar.redstone.on` | Radar set to emit redstone |
| `message.radar.redstone.off` | Radar set to not emit redstone |

The shift tooltip says the trigger range defines the threat area; the code never does that (ICBM-05-04). Hard-coded English: the "Unexpected error" chat line.
