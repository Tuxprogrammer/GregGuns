# EMP Tower and EMP Action

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. The tower is one block with two metas (tower and extender). The EMP effect itself (`ActionEmpArea` and the receiver capability) is shared with the EMP explosive, bomblet, missile and cart; those only reach it through `02-explosives` and `03-missiles`, and this sheet is where the effect is specified.

## 1. Identity

| Item | Value |
|---|---|
| Classes | `content/blocks/emptower/BlockEmpTower`, `ItemBlockEmpTower`, `TileEMPTower`, `TileEmpTowerFake`, `RadioEmpTower`, `PropertyTowerStates`, `TESREmpTower`, `gui/ContainerEMPTower`, `gui/GuiEMPTower`; `content/actions/emp/ActionDataEmpArea`, `ActionEmpArea`, `EmpHandler`; `lib/capability/emp/*` (6 classes); `content/missile/entity/CapabilityEmpMissile` |
| Registry names | block and tile `icbmclassic:emptower`; action data `icbmclassic:emp.area`; capability resource names `icbmclassic:emp.wrapper.entity.item`, `icbmclassic:emp.wrapper.entity.creeper` |
| Metas | 0 = tower base (real tile `TileEMPTower`); 1 = extender (dummy tile `TileEmpTowerFake`, GUI-less look-alike) |
| English names | `tile.icbmclassic:emptower.0.name` = "EMP Tower"; `.1.name` = "EMP Tower Extender" |
| Tooltips | `.0.info` = "Generates an emp blast using power"; `.1.info` = "Increases max range of emp tower by %1$s" (filled with `BONUS_RADIUS`) |
| Blockstate | `type` property: BASE, COIL, ELECTRIC, SPIN (*1.12-only mechanism*). Meta 1 maps to SPIN, everything else to BASE. COIL and ELECTRIC are render-only states used by the TESR |
| Creative tab | the ICBM tab (both metas) |
| Oredict | none |

## 2. Obtaining

| Meta | Shaped recipe |
|---|---|
| 0 tower | `p w p` / `e c e` / `p r p`; p = `plateSteel`, w = `wireGold`, e = `icbmclassic:battery`, c = `circuitElite`, r = redstone dust (`recipes/machine/emptower.json`) |
| 1 extender | `p r p` / `e w e` / `p r p`; same ingredients, no circuit (`emptower.booster.json`) |

## 3. Numbers

| Name | Default | Config key (`icbmclassic/emp_tower`) |
|---|---|---|
| Range on placement | 60 m | none (tile field `range`) |
| Base max range | 150 m | `base_max_range` |
| Range bonus per extender | +20 m | `extender_range_bonus` |
| Firing cost | `range^2 * 100` FE | `firing_cost_area` = 100 |
| Upkeep per tick | `range * 10` FE | `energy_upkeep` = 10 |
| Upkeep capacity | 100 ticks of upkeep | `energy_upkeep_capacity` = 100 |
| Buffer size | `firingCost + upkeep * 100` (recomputed from the current range) | derived |
| Receive limit | 10,000 FE per receive call; no receiving while cooling down; no extraction | `energy_receive_limit` |
| Cooldown after a shot | 200 ticks | `firing_cooling_ticks` = 10 * 20 |
| Block hardness / resistance | 10 / 10, iron material, not opaque, not a full cube | none |
| Rotation speed | `chargeFraction * 15` degrees per tick | constant |

At the default range 60: firing 360,000 FE, upkeep 600 FE per tick, buffer 420,000 FE. At 150: firing 2,250,000 FE, upkeep 1,500, buffer 2,400,000. With `REQUIRES_POWER` false, both costs are 0 and the charge fraction is 1.

Max range = base + extenders * bonus. An extender counts only when it is in an unbroken column directly above the base. Energy is Forge Energy (*1.12-only mechanism*).

## 4. Behavior

### Tower tick

**Server:**
1. Pay the upkeep every tick (a real extraction; it runs even when full or cooling down).
2. Hum: if `ticks % 20 == 0` and ready, play `MACHINE_HUM` at 0.5 volume, pitch `0.85 * charge fraction`. Else if cooling down and `ticks % 10` equals a random 0-9, play the vanilla lava-extinguish sound at 0.5 volume, pitch 0.6-0.75.
3. If ready and the tile receives any strong redstone power (`getStrongPower > 0`), fire with no cause. A held signal re-fires every time the cooldown ends.

**Both sides:** the extender column is rescanned every 5 ticks (`updateStructure`). It walks upward from the tile while the block above is the EMP tower block, and each `TileEmpTowerFake` found gets its host set. While `cooldownTicks > 0` it counts down and every 5 ticks spawns one smoke particle per column segment on a random face. Otherwise the coil rotation advances by `charge * 15` degrees per tick.

### Ready and firing

- `isReady` = cooldown is 0 AND (power not required OR the buffer can pay the firing cost).
- `fire(cause)`: if ready, run the action `icbmclassic:emp.area` at the tower position with the field AREA_SIZE = range. If the action returns GREEN, pay the firing cost, set cooldown to 200, return true. On any other result log a server warning and do nothing (no player feedback). Power is paid after the action runs.
- Fire sources: the GUI fire button (`fire` packet, no cause), redstone, radio.

### Radio (`RadioEmpTower`)

On a message the tower first checks it can receive (channel match and not disabled; `01-core`). Then:
- `IncomingMissileMessage` with the trigger flag: fire only if the distance from the tower to the missile is under the tower's current range. This message is the one the radar station broadcasts every 40 ticks.
- Any other `ITriggerActionMessage` with the trigger flag (the remote detonator's message): fire unconditionally.
- On success reply with a text message on the sender: `tile.emptower.radio.success`.

If the sender is a `FakeRadioSender` (an item), the cause is the sending player.

### EMP action (`ActionEmpArea`, server only)

Default size 50 (`ConfigBlast.emp.scale`, minimum 1) unless the caller supplies AREA_SIZE (the tower does). Order: tiles first, then entities, then play the `emp` sound at (4.0 volume, pitch about 0.7 +/- 0.14). The action always returns COMPLETED.

**Tiles** (skipped if `allow_tiles` is false). Visit every loaded block in a cube. The cube is skewed by one block (ICBM-05-10): offsets run from `-size` to `size - 1` on each axis around the block position. This is 8 * size^3 blocks, about 1.7 million at the default range 60 and about 27 million at 150. Per block:
1. Fire `EmpEvent.BlockPre` (cancelable). If canceled, skip to the post event.
2. If the block state matches the replacement list (`emp/replacements/list` config, default empty, a weighted block-chance table), replace the block and stop for this block (the post event is skipped; minor).
3. Else, if the tile has the EMP capability, call its receiver with power 1.0 and then ask whether to also hit its contents.
4. Else, if `allow_draining_energy_tiles` and the tile's energy can be set directly, set its energy to 0. Tiles that cannot be set directly are left alone (a TODO for draining over time).
5. If contents are allowed and the tile has an item handler: apply the inventory pass (below), if `allow_tile_inventory`.
6. Fire `EmpEvent.BlockPost`.
On the tower's own Y layer each block also asks for a large smoke particle, but the call runs on the server world and so never reaches clients (ICBM-05-13).

**Entities** (skipped if `allow_entities` is false). All entities in the box `position +/- size` (a true symmetric box on all three axes). Per entity: fire `EmpEvent.EntityPre` (cancelable). If the entity has the EMP capability, apply it; otherwise, if `allow_draining_energy_entity` and the entity exposes settable energy, zero it. Then, if allowed, the inventory pass runs over the entity's item handler (players, carts, anything with the capability), if `allow_entity_inventory`. Then fire `EmpEvent.EntityPost`. A receiver returns a power value that carries to the next step; no built-in receiver changes it, so power stays 1.0.

**Inventory pass** (`CapabilityEmpInventory`). Only item handlers that allow direct stack setting are affected. For each non-empty slot: copy the stack; if the stack has the EMP capability, call it, and note whether to descend into the item; else if `allow_draining_energy_items`, set the item's energy to 0; if `allow_item_inventory` and the item itself holds an item handler (bags, backpacks), recurse into it. If the copy differs from the original, write it back. Unsafe writes are caught and logged.

### What the EMP does to each kind of thing

| Target | Rule | Config |
|---|---|---|
| Energy blocks or entities without EMP support | Energy set to 0 if settable | `allow_draining_energy_tiles/_entity/_items`, all true |
| Items in any inventory | Energy zeroed; stacks with EMP support get their callback | `allow_*_inventory` and `allow_item_inventory`, all true |
| Dropped items (`EntityItem`) | Same item rules on a copy, written back if changed | `allow_ground_items`, true |
| Creeper | Struck by a fresh lightning bolt (becomes charged); ignores the simulate flag | `allow_creeper_charging`, true |
| Primed ICBM explosive entity (`EntityExplosive`) | `setDead` at once, no drop, no explosion | none |
| Missile (`CapabilityEmpMissile`) | Chance chain below | `missiles.*` |
| Blocks in the swap list | Replaced | `replacements.list` |
| Other mobs, players | Only their inventories and energy | as above |

**Missile chain** (all rolls use `random <= chance`; applies only if `missiles.enabled` and the missile is alive):

| Step | Chance | Result |
|---|---|---|
| 1. Destroy | `missile_kill_chance` = 0.5 | Missile removed; then with `engine_fuel_blow_chance` = 1.0 an explosion that does not break blocks, of strength `engine_fuel_blow_strength` = 1.0 at the missile |
| 2. else: fry guidance | `engine_kill_chance` = 0.8 | Flight logic is replaced by a dead (no thrust) logic with 0 ticks; the missile falls |
| 3. else: trigger | `missile_trigger_chance` = 0.1 | Explosive missile: runs its explosive at its own position. Actionable missile: runs its main action. SAM: a strength-1.0 explosion |
| 4. else | remainder | Nothing |

With the defaults the per-pulse outcomes are: destroyed 50%, guidance fried 40%, triggered 1%, unaffected 9%. The explosive missile may be left alive after step 3 (ICBM-05-14).

## 5. Persistence and sync

NBT (tower tile): `emp_action` (the potential-action state), `inventory`, `radio`, `range` int, `cooldown` int, `energy` int, `rotation` float, `prev_rotation` float. The dummy extender tile stores nothing; its host link is rebuilt by the 5-tick scan.

Packets (*1.12-only mechanism*; fields only):

| Name | Direction | Fields |
|---|---|---|
| `description` | server to clients in range | rotation float, cooldown int |
| `gui` | server to GUI users every 3 ticks | energy int, channel string, radio-disabled bool, range int |
| `radius` | client to server | range int; clamped only from above (ICBM-05-11) |
| `fire` | client to server | none; fires with no cause |
| `radio.frequency`, `radio.disable` | client to server | string; toggle |

The EMP capability stores nothing (its storage reads and writes nothing).

## 6. UI

Texture `textures/gui/gui_emp_tower.png` (256 x 256); container 175 x 166; battery slot (141, 47); player inventory at (7, 84).

| Element | Position |
|---|---|
| Range int field | (18, 17), 40 x 12; to its right the text "/ <max range>" at (62, 19) |
| Frequency field | (135, 17), 34 x 12 |
| Radio disable button | (119, 16) |
| Fire button | (24, 38); enabled only when ready; glass overlay when disabled |
| Energy bar | (141, 66), with tick-cost and action-cost markers |
| Range tooltip | over (2, 16), 14 x 14 |
| Title | "EMP Tower" at (52, 6) |

Fire-button tooltip: cooling, `gui.icbmclassic:empTower.cooling` with the percentage; no power, `.power` with the charge percentage; else `.ready`. Note the unpowered check in the lambda uses a real extraction on the client copy (client only, harmless).

## 7. Visuals

- Base model `models/block/emp_tower_base.json`, blockstate `blockstates/emptower.json`; item models `emp_tower.json`, `emp_tower_1.json`. Variant models `emp_tower_coil.json`, `emp_tower_electric.json`, `emp_tower_spin.json`.
- A TESR (`TESREmpTower`) draws the spinning coil (the COIL state) at the tile's rotation, interpolated by partial ticks. Extenders spin with their host's rotation, with +45 degrees on odd heights. When the charge fraction exceeds 0.2 it also draws the translucent ELECTRIC overlay at 50% alpha. The render bounding box is infinite so it never culls.
- Textures: `textures/blocks/emp_tower/base.png` and `coil.png` (16 x 16 each), `electric.png` (16 x 64, animated via `electric.png.mcmeta`).
- Particles: smoke (`SMOKE_NORMAL`) on the faces while cooling; EMP smoke (`SMOKE_LARGE`) on the server, which does not display (ICBM-05-13).

## 8. Audio

| Sound | Trigger | Volume / pitch |
|---|---|---|
| `icbmclassic:machinehum` (`sounds/machinehum.ogg`, 21,729 bytes; duration unmeasured) | every 20 ticks while ready | 0.5 / `0.85 * charge`; the "true" flag is a distance-delay flag |
| vanilla lava-extinguish | cooling, roughly twice a second | 0.5 / 0.6-0.75 |
| `icbmclassic:emp` (`sounds/emp.ogg`, 60,305 bytes; duration unmeasured) | each EMP action | 4.0 / about 0.7 +/- 0.14 |

Subtitle for `emp`: "EMP Discharge" (`subtitle.icbmclassic:explosion.emp`). Both custom sounds are category `block`. The cooling sound has no custom subtitle (a TODO in the code).

## 9. Relationships

- Receives the radar's `IncomingMissileMessage` ([radar-station.md](radar-station.md)); receives the remote detonator's trigger ([handheld-tools.md](handheld-tools.md)).
- The EMP action and receiver are also used by the EMP explosive, missile, bomblet and cart (`02-explosives`, `03-missiles`); missile and explosive entities supply their own receivers.
- Public API: `registerBlockEmpHandler` and the `IEMPReceiver` capability (`01-core`).
- Radio bus, energy model and config groups: `01-core`.

## 10. Bugs

ICBM-05-10, 05-11, 05-13, 05-14. Details in [index.md](index.md#suspected-bugs).

## Lang

| Key | English |
|---|---|
| `tile.icbmclassic:emptower.0.name` / `.1.name` | EMP Tower / EMP Tower Extender |
| `tile.icbmclassic:emptower.0.info` | Generates an emp blast using power |
| `tile.icbmclassic:emptower.1.info` | Increases max range of emp tower by %1$s |
| `gui.icbmclassic:empTower.name` | EMP Tower |
| `gui.icbmclassic:empTower.power` | Firing Safety: / Charging... %1$s |
| `gui.icbmclassic:empTower.cooling` | Firing Safety: / Cooling... %1$s |
| `gui.icbmclassic:empTower.ready` | Ready to fire |
| `subtitle.icbmclassic:explosion.emp` | EMP Discharge |
| `tile.emptower.radio.success` | not in `en_us.lang` (missing; shown as the raw key on a radio sender) |
| `explosive.icbmclassic:emp.info` | Over 9000? Not anymore (the EMP explosive, `02-explosives`) |
