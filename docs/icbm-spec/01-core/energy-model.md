# Energy Model

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Classes: `lib/energy/storage/EnergyBuffer`, `EnergyChangeCallback`, `EnergyActionType`; `lib/energy/system/EnergySystem`, `IEnergySystem`, `EnergySystemFE`, `EnergySystemNull`; `api/energy/IEnergyBuffer`, `IEnergyBufferProvider` (both `@Deprecated`, unused); `content/items/ItemBattery`; `prefab/item/ItemICBMElectrical`, `ItemStackCapProvider`; the energy fields of every machine tile.

This is the system GregGuns **replaces with GregTech EU** (hatches, tiers, voltage, amperage). This sheet records what ICBM does, so the replacement can keep the behavior that matters and drop the defects. Forge Energy, `Capability` and `IEnergyStorage` are *1.12-only mechanisms*; there is no FE on GTNH 1.7.10 (RF/EU only).

## 1. Layers

| Layer | Class | Role |
|---|---|---|
| Storage | `EnergyBuffer` | A single integer counter with a capacity, optional per-call receive and extract limits, optional gates, and a change callback. Implements Forge's `IEnergyStorage` and saves as a single NBT int |
| Adapter | `EnergySystem` + `IEnergySystem` | A small registry of "energy systems" that can read, add, remove or set energy on any tile, entity or item stack. Only one system exists: `EnergySystemFE` (checks for the FE capability). `EnergySystemNull` is returned when nothing matches |
| Legacy API | `IEnergyBuffer`, `IEnergyBufferProvider` | Old 1.7-style buffer interface ("VE joules"). Deprecated, no implementers in the covered code |
| Users | Every machine tile; the battery item; the EMP action | See §4 and §5 |

The adapter exists so machines never call FE directly, only "charge from this item stack" and "drain this object". A GT version replaces the adapter with EU hatch logic and an "is this an electric item" check.

## 2. `EnergyBuffer` semantics

Configuration knobs (all lambdas, read each call, so they follow live config): capacity; `canReceive` (default **true**); `canExtract` (default **false**); receive limit (default unlimited); extract limit (default unlimited); on-change callback `(previous, current, reason ADD|REMOVE|SET)`.

| Operation | Behavior |
|---|---|
| `receiveEnergy(n, simulate)` | 0 if `canReceive` is false. Clamp `n` to the receive limit, then to free room. Returns the amount accepted. Applies the change unless simulating |
| `extractEnergy(n, simulate)` | 0 if `canExtract` is false. Clamp `n` to the extract limit, then calls the internal extract. **Defect: see below** |
| `receiveEnergyInternal`, `extractEnergyInternal` | Same, without the gates or limits. Machines use these to spend power |
| `consumePower(amount, simulate)` | Returns true outright when `use_energy` is off. Otherwise true when the internal extract returned at least `amount` |
| `setEnergyStored(n)` | Direct set, clamped to `[0, capacity]`. Meant for packet sync, loading and EMP |
| `dischargeItem(stack)` | If the stack is an FE item and the buffer can receive: ask the item to give up `capacity - stored` FE (ignoring the buffer's own receive limit), then `receiveEnergy` whatever came back. Called once per tick by the machine's energy slot. Returns the stack unchanged |
| Save/load | One NBT int (the stored amount); capacity and limits are not saved |

### The extract defect (ICBM-01-01, ICBM-01-02)

`extractEnergyInternal` returns `min(capacity, requested)` instead of `min(stored, requested)`. It does clamp the stored value at zero, so the buffer never goes negative, but it reports the full request as removed. Consequences for everything built on it:

- `consumePower(cost, ...)` succeeds whenever the buffer holds **any** energy (> 0) and `cost <= capacity`; it then empties the buffer. A launcher (capacity 1200, cost 1200) fires with 1 FE stored. The EMP tower is "ready" with 1 FE and fires. The radar runs while at least 1 FE arrives each tick.
- The effective cost of any action is `min(cost, whatever is stored)`, so a trickle supply is as good as a full one.
- `extractEnergy` (the external path, only reachable when `canExtract` is true) over-reports in the same way. Any item that exposed such a buffer with extraction enabled would duplicate energy.

A faithful port must decide deliberately: the intended rule is clearly "must have the full cost stored".

## 3. Adapter: `EnergySystemFE`

| Method | Behavior |
|---|---|
| `canSupport(object, side)` | `hasCapability(ENERGY)` on a tile, entity or stack |
| `getEnergy`, `getCapacity` | stored / max |
| `addEnergy`, `removeEnergy` | pass through to `receiveEnergy` / `extractEnergy` (so the buffer's gates and limits apply) |
| `setEnergy(object, side, value, simulate)` | For vanilla Forge `EnergyStorage` objects only, writes the private `energy` field by reflection, then calls `removeEnergy(MAX_INT)`. Net effect: **drain everything**, whatever `value` was; returns what the drain removed. For any other storage (including `EnergyBuffer`) it just calls `removeEnergy(MAX_INT)`, which does nothing when extraction is disabled (ICBM-01-05) |
| `canSetEnergyDirectly` | true only if the reflection did not fail and the storage is a vanilla `EnergyStorage` |

Only caller of `setEnergy`: the EMP action when it drains FE items lying in the world or in containers (`DRAIN_ENERGY_ITEMS` in the EMP config, 02/05). Because the ICBM battery refuses extraction, an EMP does not empty ICBM batteries.

## 4. Machines

All machine energy is an `EnergyBuffer` field named `energyStorage`, exposed as the FE capability on every side (no per-side rules) **only while `use_energy` is true**. Each machine also has an inventory slot that accepts any FE item and drains it into the buffer every tick through `dischargeItem`.

| Machine | Capacity | Cost | Receive gate / limit | Extract | Energy slot |
|---|---|---|---|---|---|
| EMP tower | `firingCost + upkeep * 100` (recomputed from current range) | firing `range^2 * firing_cost_area` (default 100); upkeep `range * energy_upkeep` (default 10) per tick | only while cooldown is zero; `energy_receive_limit` 10,000 per call | never | slot 0 |
| Launcher base | `power_capacity` (default 1200) | `power_per_launch` (default 1200) per launch | none (unlimited) | blocked (default) | slot 1 |
| Cruise launcher | same keys as launcher base | same | none | blocked | slot 1 |
| Launcher screen | same keys | none of its own; the screen's launch asks the launchers | none | blocked | slot 0 |
| Radar station | `ConfigRadar.POWER_CAPACITY` (default 20,000) | `ConfigRadar.POWER_COST` (default 1,000) per tick while running | none | blocked | slot 0 |

Numbers and per-machine behavior beyond this table are in [05-detection-defense](../05-detection-defense/index.md) (radar, EMP tower) and [04-launch-control](../04-launch-control/) (launcher family; the launcher network shares power across linked launchers there).

When `use_energy` is false: `consumePower` always succeeds, the tower reads its costs as 0 (so its capacity is 0), the capability is not exposed, and GUIs show `NEEDS_POWER=false`.

Each tick every energy slot drains the held item completely into the buffer (room permitting). Because `dischargeItem` asks the item for "all the free room" but the buffer then applies its own receive limit, a limited receiver (the EMP tower, 10,000) can destroy the difference (ICBM-01-04).

The tooltip/state data each tile publishes uses keys like `NEEDS_POWER`, `ENERGY_COST_ACTION`, `ENERGY_COST_TICK`, `ENERGY_RECEIVE_LIMIT`; these feed a "what does this machine need" readout used by the radar/launcher GUIs and probes (names in `TileEMPTower`, `TileLauncherBase`, ...).

## 5. The battery item

| Property | Value |
|---|---|
| Registry name | `icbmclassic:battery`, stack size 1, has subtypes flag (no subtypes defined) |
| Capacity / limits | `battery_tier_1_capacity` 100,000 FE; input `battery_tier_1_input` 10,000; output `battery_tier_1_output` 10,000 (config in [config](config.md#3-batterycfg-configbattery)) |
| Capability | One `EnergyBuffer` attached via a generic capability provider (saved under key `battery` in the stack's capability NBT) |
| Extraction | **Disabled.** The buffer is built with an output limit but never given `canExtract`, whose default is false. A machine's `dischargeItem` therefore gets 0. The battery fills from a cable/charger and never gives energy back (ICBM-01-03) |
| Charge on spawn | Starts empty. It has no `getSubItems` override, so creative shows one empty battery |
| Tooltip | `L: <percent>%` and `E: <stored>/<max> FE` (client). A shift tooltip is a TODO |
| Durability bar | `getDurabilityForDisplay` returns stored/max (full battery = 1.0 = an empty-looking bar), but the item is never "damaged", so vanilla never draws the bar |
| Recipe | shaped `RCR / PPP / SIS`: R redstone dust, C `ingotCopper`, P `paper`, S `dustSulfur`, I `ingotIron`; one battery |
| Creative tab | the ICBM tab (via the "everything else" collection) |
| Lang | `item.icbmclassic:battery.name` = "Battery"; `.info` = "Used to store and transport power" |

`ItemICBMElectrical` (base for the defuser and others) is a stub: `getEnergy` returns `Integer.MAX_VALUE` and `discharge` does nothing. Tools that extend it never consume charge.

## 6. What a GT replacement must keep or drop

Keep (observable gameplay):

1. Launchers, EMP tower and radar need external power; a switch to turn that off (`use_energy`).
2. Each machine has a buffer that fills from a cable and can also be topped up by dropping a power item in a slot.
3. The EMP tower's cost scales with area (range squared) and has a continuous upkeep while armed; it will not accept power while cooling down.
4. Launch cost is per launch, not per tick; radar cost is per tick while on.

Do not inherit:

- The extract/consume defect (§2): machines would run on any non-zero charge.
- The non-extractable battery.
- The EMP "set energy" that always drains.
- `dischargeItem` ignoring the receive limit.

Mapping suggestions (EU voltage tiers, hatch types, buffer sizes) belong to the GregGuns spec, [docs/gregguns-icbm-spec](../../gregguns-icbm-spec/README.md).
