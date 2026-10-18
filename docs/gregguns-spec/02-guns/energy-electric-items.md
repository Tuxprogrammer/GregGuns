# Energy weapons as IC2 electric items

- **Techguns baseline:** [sheets-energy-special.md](../../techguns-spec/02-guns/sheets-energy-special.md) (Laser Rifle, Tesla Rifle and the other energy guns), [gun-lifecycle.md § reloading](../../techguns-spec/02-guns/gun-lifecycle.md#7-reloading) (Energy Cell as ammo)
- **Design records:** [0003 Energy model](../../design/0003-energy-model.md), [0001 Tier map](../../design/0001-tier-map.md)
- **Pack mods:**
  - Industrial Craft 2 (`IC2`, `2.2.2.828`), API only
  - GregTech (`gregtech`, `5.09.54.205`)
- **Roadmap:** M5 (energy and exotic weapons); D3 gate

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| The gun consumes an **Energy Cell** item per reload, and the empty cell comes back | The gun **is** the battery. It stores EU in its own NBT and spends it per shot. | [0003](../../design/0003-energy-model.md): EU only, rechargeable items. |
| Cells are charged in the Techguns Charging Station (RF) | A GT charger or battery buffer of the gun's tier charges the gun | GT has the machines. Charging Station is dropped. |
| Gun damage value (`damage = maxDamage` for an empty gun) tracks rounds | Gun damage value is owned by IC2's electric item manager and shows the charge bar | See the contract below. Ammo for energy guns cannot use the damage value. |

## Integration points

### IC2: the electric item contract

- **Pack config:** none for the contract. The pack ships `config/IC2.ini` and `config/ic2/`; nothing there changes how `IElectricItem` works.
- **Source (API):** IC2 has no public source for its internals. Its API jar is the official source of `ic2.api`. See [INTEGRATION.md](../INTEGRATION.md#step-1-find-the-mod-and-its-pinned-source). The API ships as `industrialcraft-2-2.2.828-experimental-api.jar` from `https://maven.ic2.player.to/net/industrial-craft/industrialcraft-2/2.2.828-experimental/`. Files read: `ic2/api/item/IElectricItem.java`, `IElectricItemManager.java`, `ElectricItem.java`.
  - `IElectricItem` has six methods: `canProvideEnergy`, `getChargedItem`, `getEmptyItem`, `getMaxCharge`, `getTier`, `getTransferLimit`. Its Javadoc says the item should have a maximum damage of 13 and that lower tiers cannot send energy to higher ones.
  - `ElectricItem.manager` is the static manager that does `charge`, `discharge`, `getCharge`, `canUse` and `use`.
- **Source (precedent in the pack):** a plain item class that implements `IElectricItem` and spends charge with `ElectricItem.manager.use(...)`: [ItemTeslaStaff.java#L40-L72](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/items/ItemTeslaStaff.java#L40-L72). It also adds a charged and an empty variant to the creative tab in `getSubItems`: [ItemTeslaStaff.java#L83-L94](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/bartworks/common/items/ItemTeslaStaff.java#L83-L94).
- **Decompiled evidence:**
  - Jar: `industrialcraft-2-2.2.828-experimental-dev.jar` (GTNH 2.9.0-RC-2), class `ic2.core.item.ElectricItemManager`, methods `charge`, `discharge`, `use`. IC2's internals have no source, so this is **unverified** and may change when the pack updates IC2.
  - Findings:
    - `charge` refuses unless the **item's tier is at most the supplied tier**, so a higher-tier charger can charge a lower-tier item.
    - The charge is stored as a `double` under the NBT key `charge`.
    - After every charge or discharge the manager **rewrites the item's damage value** to draw the charge bar.
    - `use` does not check creative mode.
- **GregGuns does:**
  - Every energy gun's item class implements `IElectricItem`. It does not implement `ISpecialElectricItem`, so IC2's default manager applies, as with `ItemTeslaStaff`.
  - `getTier` returns the **GT tier index** of the gun's [0001](../../design/0001-tier-map.md) tier (LV = 1, MV = 2, HV = 3, EV = 4, IV = 5, LuV = 6, ZPM = 7). This is the same numbering GT uses for its batteries, see below.
  - `canProvideEnergy` returns `false`. A gun is charged but never drained into a machine. GT's `Tool_Scanner` and `Tool_Cover_Copy_Paste` use the same "charge only" mode (`-1` in the table below).
  - `getTransferLimit` returns `GTValues.V[tier]`.
  - `getChargedItem` and `getEmptyItem` return the gun itself.
  - A shot spends EU through `ElectricItem.manager.use(stack, euPerShot, shooter)` on the server. The code checks creative mode first, because IC2's `use` does not.
  - Rounds, magazines and reload state for energy guns live in a GregGuns NBT tag, not in the damage value. It never uses the key `charge`.
  - NPC-held energy guns do not spend EU. They follow the NPC rules in [10-entities-npcs](../../techguns-spec/10-entities-npcs/index.md).
- **Pack-side change:** none.

### GT: charging, and the tier match

- **Source:**
  - The GT electric item stats table gives each GT battery its capacity, transfer limit, tier and mode. The Lithium batteries, for example: LV 100,000 EU at tier 1, MV 400,000 at tier 2, HV 1,600,000 at tier 3: [MetaGeneratedItem01.java#L5208-L5246](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/MetaGeneratedItem01.java#L5208-L5246).
  - `MetaBaseItem` implements `ISpecialElectricItem` and is its own manager. `canProvideEnergy` is true only for modes above 0, or `-2`/`-3` on a stack of 1; `charge` accepts modes `-1` and `-3`: [MetaBaseItem.java#L338-L343](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/MetaBaseItem.java#L338-L343) and [MetaBaseItem.java#L359-L372](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/MetaBaseItem.java#L359-L372).
  - A GT machine charges an item with `GTModHandler.chargeElectricItem(stack, storedEU, machineOutputTier, false, false)`: [BaseMetaTileEntity.java#L213-L222](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/BaseMetaTileEntity.java#L213-L222).
  - That method only charges when `itemTier < 0 || itemTier == machineTier`. When the transfer limit is not ignored it caps the charge at `V[itemTier]` per call: [GTModHandler.java#L2053-L2072](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L2053-L2072).
  - A battery buffer only accepts items whose tier **equals** the buffer's tier: [MTEBasicBatteryBuffer.java#L226](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicBatteryBuffer.java#L226), using [GTModHandler.java#L2290-L2299](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTModHandler.java#L2290-L2299).
- **GregGuns does:** relies on this. An LV gun (tier 1) charges in LV chargers and LV battery buffers only. Capacity per tier starts from the GT batteries above ([0003](../../design/0003-energy-model.md): "what a GT battery of the same tier holds"), as a config option per gun.
- **Pack-side change:** none.

| Gun tier | GT item whose capacity is the baseline | Capacity (EU) | Source row |
|---|---|---|---|
| LV | `Battery_RE_LV_Lithium` | 100,000 | L5213 |
| MV | `Battery_RE_MV_Lithium` | 400,000 | L5218 |
| HV | `Battery_RE_HV_Lithium` | 1,600,000 | L5223 |
| EV | `BatteryHull_EV_Full` (the only EV-tier battery in the table) | 6,400,000 | L5238 |
| IV | `Energy_Lapotronic_Orb` | 100,000,000 | L5225 |
| LuV | `Energy_Lapotronic_orb_2` | 1,000,000,000 | L5227 |
| ZPM | `Energy_Module` | 10,000,000,000 | L5233 |

Rows are lines in the [stats table](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/MetaGeneratedItem01.java#L5208-L5246). The baseline is a starting point; M5 tunes each gun.

## Requirements

- **GGS-02-09.** Each energy gun's item class implements `ic2.api.item.IElectricItem` and registers no RF interface.
- **GGS-02-10.** `getTier(stack)` equals the GT tier index of the gun's [0001](../../design/0001-tier-map.md) tier.
- **GGS-02-11.** `canProvideEnergy(stack)` is `false` and `getTransferLimit(stack)` is `GTValues.V[tier]`.
- **GGS-02-12.** Energy guns spend EU only on the server and only for player shooters, and skip the spend for a creative-mode player.
- **GGS-02-13.** An energy gun's ammo and reload state are stored in a GregGuns NBT tag. The code never reads or writes the stack's damage value for ammo, and never uses the NBT key `charge`.
- **GGS-02-14.** The default capacity of an energy gun of tier T is the capacity of the GT item in the table above, and each gun's value is a config option.
- **GGS-02-15.** The creative tab lists each energy gun twice, empty and fully charged.
- **GGS-02-16.** A gun fails to charge in a GT charger or battery buffer of any other tier. The tooltip names the required tier.

## Tests

- GGS-02-09, 10, 11, 14: unit test that instantiates each gun item and checks the `IElectricItem` values against the tier table.
- GGS-02-12, 13: `/gregguns test` scenario that fires an energy gun and checks the stored charge, the damage value and the NBT.
- GGS-02-16: `/gregguns test` scenario that places a gun in an LV and an MV battery buffer and checks that only the LV buffer accepts an LV gun.
- GGS-02-15: manual check of the creative tab.
- A `runServer` check that IC2 and GT are loaded and the item registers.

## Open questions

- **Charger tier rule.** [0003](../../design/0003-energy-model.md) says an item "charges only in a GT charger or battery buffer of that tier **or higher**". GT's machines accept only the **exact** tier (see above). IC2's own manager would accept a higher tier, but GT's machines never offer one. Amend 0003 to "exactly that tier".
- **EV baseline.** The GT stats table has no EV player battery. `BatteryHull_EV_Full` is the closest. Confirm in M5.
- **Ammo for energy guns.** Techguns spends one Energy Cell per reload and returns the empty cell. The gun-as-battery model removes that loop. Confirm that per-shot EU, not rounds, is the energy guns' only ammo. A new record is needed only if swappable charge packs are wanted.
