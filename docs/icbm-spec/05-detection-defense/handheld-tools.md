# Handheld Tools

Four items: radar gun, remote detonator, laser detonator, defuser. Source root: `src/main/java/icbm/classic/content/items/` at `9c1d985a`. The radio bus and message types they send are in `01-core`; what a launcher or explosive does when it receives the message is in `04-launch-control` and `02-explosives`.

Shared facts:
- All four are single items (meta 0), stack size 1 (the defuser inherits its base class default), ICBM creative tab, no oredict names.
- The remote and laser detonators extend `ItemRadio` (`prefab/item/ItemRadio`), which keeps the channel in the stack NBT key `hz` (a float; the channel string is its floor as an integer). Setting and tooltips for the channel live in that base class (`01-core`).
- Registered item textures are all 16 x 16: `textures/items/radar_gun.png`, `remote_detonator.png`, `laser_designator.png`, `defuser.png`. Item models `models/item/radargun.json`, `remotedetonator.json`, `laserdetonator.json`, `defuser.json`. None has sounds, particles or a held-item renderer.
- Events are Forge-bus events in the ICBM API; GregGuns rebuilds them as its own hooks (*1.12-only mechanism* for the event classes, the behavior is what counts).

## Radar Gun (Laser Range Finder)

**1. Identity.** `ItemRadarGun`, registry `icbmclassic:radargun` (registered as `radarGun`), "Laser Range Finder" (`item.icbmclassic:radarGun.name`).
**2. Obtaining.** Shaped: `g c i` / `. b w` (the button and planks sit in the right two columns of row 2); g = glass, c = `circuitBasic`, i = `ingotSteel`, b = stone button, w = `plankWood` (`recipes/tools/radargun.json`).
**3. Numbers.** Max range 200 (constant `MAX_RANGE`, a TODO to make it config; used for the tooltip and the client ray length). Stack size 1.
**4. Behavior.**
- Right-click in the air: sneaking clears the stored target (server: wipes the stack tag, resets damage to 0, chat `gps.cleared.name`). Not sneaking: the client ray-traces up to 200 blocks; on a hit that is not a launcher tile, it sends the hit position to the server (hand flag plus 3 doubles). The server schedules `onTrace` on the main thread.
- Right-click on a block (server): sneaking clears, as above. Otherwise `onTrace` with the clicked position (bug ICBM-05-07: the Y uses the X hit offset).
- `onTrace`: requires the held stack to be the radar gun; fires `RadarGunTraceEvent` (cancelable, may rewrite the position or null it). On cancel, nothing and no feedback. Otherwise store the position and the current world in the stack's GPS data and tell the player `gps.pos.set.name`. Returns true either way.
- Launcher blocks are excluded only client-side; the server accepts any position (ICBM-05-12).
**5. Persistence.** The GPS capability data (`gps_data` on the stack; a *1.12-only mechanism* for attaching it): position, dimension. Legacy v5.3 data key `linkPos` (compound with `dimension`, `x`, `y`, `z`) is migrated on load and removed.
**6. UI.** Tooltip: `radarGun.data.pos` / `.data.all` ("Target: (x, y, z) W: (dim)name") when set with or without a world, `.data.empty` when unset, then `.info` ("Stores GPS data of targetted position / Max range of %1$sm"). Coordinates print with one decimal. Chat: `gps.pos.set.name` = "GPS data saved", `gps.cleared.name` = "GPS data cleared".
**7. Visuals.** Item texture and model above.
**8. Audio.** None.
**9. Relationships.** The GPS data is read by launchers and the GPS transfer logic (`04-launch-control`); the laser detonator is crafted from it.
**10. Bugs.** ICBM-05-07, 05-12.

## Remote Detonator

**1. Identity.** `ItemRemoteDetonator`, registry `icbmclassic:remotedetonator` (`remoteDetonator`), "Remote Detonator" (`item.icbmclassic:remoteDetonator.name`), tooltip `.info` "Sends a radio signal to trigger devices".
**2. Obtaining.** Shaped: `i r i` / `i c i` / `i b i`; i = `ingotSteel`, r = redstone torch, c = `circuitAdvanced`, b = wooden button (`recipes/tools/remote.json`).
**3. Numbers.** None. No cooldown, no range limit (the radio bus is global per dimension; see `01-core`), no energy, no repair (`setNoRepair`).
**4. Behavior.** Right-click (server only): fire `RemoteTriggerEvent` (cancelable); if not canceled, read the channel and pop a `TriggerActionMessage(channel)` on the radio bus with a fake sender carrying the player and stack. Always returns SUCCESS. Sneak does not bypass block use (`doesSneakBypassUse` true).
**5. Persistence.** NBT `hz` (float) on the stack.
**6. UI.** The base class tooltip and channel entry; lang `message.detonator.locked`, `.nopower` and `info.detonator.linked` / `.noLink` exist but this class never emits them (leftovers from the 1.7 design; `02-explosives`).
**7. Visuals.** Item texture and model.
**8. Audio.** None.
**9. Relationships.** Triggers the EMP tower ([emp-tower.md](emp-tower.md)) and any radio-listening explosive or launcher.
**10. Bugs.** None found.

## Laser Detonator (Laser Designator)

**1. Identity.** `ItemLaserDetonator`, registry `icbmclassic:laserdetonator` (`laserDetonator`), "Laser Designator" (`item.icbmclassic:laserDetonator.name`). Implements the packet receiver for its own packet.
**2. Obtaining.** Shaped, on the diagonal: `r . .` / `. c .` / `. . h`; r = radar gun, c = `circuitElite`, h = remote detonator (`recipes/tools/laser.json`). Corrected from an earlier "one column" reading, per [06 recipes](../06-blocks-items/recipes.md).
**3. Numbers.** `RANGE` 200 blocks; `COOLDOWN` 20 ticks (client only).
**4. Behavior.**
- Client right-click: if the cooldown field is 0, set it to 20, ray-trace 200 blocks. On a hit that is not a launcher tile, send the hit position (3 doubles) in a player-item packet. On a miss, show an action-bar message `laserDetonator.laser.missed` with the range. The cooldown field lives on the shared item instance, is decremented once per inventory-stack update (so two detonators halve it; ICBM-05-15) and is reset when a use finishes.
- Server, on packet: take the **main-hand** item only (offhand use is ignored); require it to be this item. On the main thread, fire `LaserRemoteTriggerEvent`. Not canceled: chat `laserDetonator.target` with the position (two decimals), then pop a `TriggerActionTargetMessage(channel, position)` on the radio bus. Canceled with a reason: action-bar message of that reason. Canceled without: chat `laserDetonator.laser.canceled`.
- The server does not check distance, line of sight or position validity (ICBM-05-12).
**5. Persistence.** NBT `hz` (float) on the stack.
**6. UI.** Lang below. Hard-coded English: none.
**7. Visuals.** Item texture `laser_designator.png` and model.
**8. Audio.** None.
**9. Relationships.** The target message is read by launchers with a "follow laser" setting and by the EMP tower as a plain trigger (`04-launch-control`, [emp-tower.md](emp-tower.md)).
**10. Bugs.** ICBM-05-12, 05-15.

## Defuser (Defuse Kit)

**1. Identity.** `ItemDefuser extends ItemICBMElectrical`, registry `icbmclassic:defuser`, "Defuse Kit" (`item.icbmclassic:defuser.name`), tooltip `.info` "Used to disarm explosive devices (blocks, entities)".
**2. Obtaining.** Shaped: `w w` / `r a r` / `i c i`; w = `plankWood`, r = redstone, a = shears, i = `ingotCopper`, c = `circuitBasic` (`recipes/tools/defuser.json`).
**3. Numbers.** `ENERGY_COST` 2,000 per use. The electrical base class is a stub: it always reports `Integer.MAX_VALUE` stored energy and `discharge` does nothing, so the kit never runs down (ICBM-05-09).
**4. Behavior.** Left-click on an entity (`onLeftClickEntity`):
1. If stored energy is at least the cost (always true):
   - ICBM explosive entity: server only, fire `ExplosiveDefuseEvent.ICBMExplosive` (cancelable; on cancel return false and the swing hits normally). Else tell the explosive it was defused, then `setDead` (no drop).
   - Primed vanilla TNT: fire `ExplosiveDefuseEvent.TNTExplosive` (cancelable); server drops one TNT block item at the position; remove the entity (the event and removal run on both sides).
   - ICBM bomb cart: fire `ExplosiveDefuseEvent.ICBMBombCart` (cancelable); kill the minecart with generic damage.
   - **Any other entity**: nothing happens to it, but the code still falls through to the discharge and returns true, which swallows the attack (ICBM-05-08).
2. Else send chat `message.defuser.nopower`, return false (the attack proceeds). This branch is unreachable today, and its key is missing from the lang file (ICBM-05-09).
**5. Persistence.** None.
**6. UI.** Chat `message.defuser.nopower` (missing key, would print raw).
**7. Visuals.** Item texture `defuser.png` and model.
**8. Audio.** None.
**9. Relationships.** Defuses what `02-explosives` places (explosive entities, bomb carts).
**10. Bugs.** ICBM-05-08, 05-09.

## Lang

| Key | English |
|---|---|
| `item.icbmclassic:radarGun.name` | Laser Range Finder |
| `item.icbmclassic:radarGun.info` | Stores GPS data of targetted position / Max range of %1$sm |
| `item.icbmclassic:radarGun.data.pos` | Target: %1$sx, %2$sy, %3$sz |
| `item.icbmclassic:radarGun.data.all` | Target: (%1$sx, %2$sy, %3$sz) W: %4$s |
| `item.icbmclassic:radarGun.data.empty` | Target: empty |
| `item.icbmclassic:remoteDetonator.name` / `.info` | Remote Detonator / Sends a radio signal to trigger devices |
| `item.icbmclassic:laserDetonator.name` | Laser Designator |
| `item.icbmclassic:laserDetonator.info.info` | Sends a radio signal to trigger devices with the target information |
| `item.icbmclassic:laserDetonator.laser.canceled` | Laser event canceled |
| `item.icbmclassic:laserDetonator.laser.missed` | Laser Error; Nothing on path within %1$sm |
| `item.icbmclassic:laserDetonator.target` | Target set (%1$s, %2$s, %3$s) |
| `item.icbmclassic:defuser.name` / `.info` | Defuse Kit / Used to disarm explosive devices (blocks, entities) |
| `gps.pos.set.name` | GPS data saved |
| `gps.cleared.name` | GPS data cleared |
| `message.radarGun.*`, `message.detonator.*` | exist but unused by these classes (scanned, nopower, transfer, noCoords; locked, nopower) |
| `message.defuser.nopower` | missing |

The laser detonator's `.info.info` key is probably a typo for `.info` (the base class builds the tooltip key from the item name; unverified, in the radio base class that is outside this area's scope). The radar gun's `.info` spelling "targetted" is a typo.
