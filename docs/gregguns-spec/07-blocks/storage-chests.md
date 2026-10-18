# Weapon and Reinforced chests: sorting, drop-off and overlap

- **Techguns baseline:** [misc-tile-entities.md, chests](../../techguns-spec/09-machines-utility/misc-tile-entities.md), [special-blocks.md](../../techguns-spec/07-blocks/special-blocks.md), bugs [MUTL-34, MUTL-35, MUTL-36](../../techguns-spec/BUGS.md)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (chests kept, low priority, decorative storage)
- **Pack mods:** InventoryBogoSorter (`bogosorter`, 1.3.54-GTNH), IronChest (`IronChest`, 6.1.13), JABBA (`JABBA`, 1.5.23), StorageDrawers (`StorageDrawers`, 2.2.31-GTNH)
- **Roadmap:** the blocks milestone in [ROADMAP](../../ROADMAP.md)

## What changes from Techguns

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| Chest GUI is a plain vanilla-style `Container`. The pack's sorter ignores it | The container opts in to InventoryBogoSorter | The pack's players expect a sort button |
| Name decides nothing | The chest names must contain "Chest" | The sorter's drop-off feature matches target inventories by name |
| Weapon Chest takes any item and behaves like a trapped chest ([MUTL-35](../../techguns-spec/BUGS.md)) | Open question: restrict it to guns, ammo and magazines | Without a filter it is a second Reinforced Chest |
| Shift-click fills the TG slots first ([MUTL-36](../../techguns-spec/BUGS.md)) | Fixed in the container | Cited bug |

## Overlap

IronChest, JABBA and StorageDrawers all exist in the pack, so plain bulk storage is not a gap GregGuns fills. The chests stay because [0002](../../design/0002-machine-deconfliction.md) keeps them as decorative storage. Nothing here needs to call those mods.

## Integration points

### InventoryBogoSorter: make the GUI sortable
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/bogosorter.cfg`. `buttonEnabled=true`, `enableAutoRefill=true`, and the `dropoff` section has `dropoffRadius=4` and `dropoffTargetNames` = Chest, Barrel, Drawer, Crate, Present, Cabinet, Counter, Fridge, Filing, Compartment, Shulker.
- **Source:**
  - [BogoSortAPI.isValidSortable](https://github.com/GTNewHorizons/InventoryBogoSorter/blob/1.3.54-GTNH/src/main/java/com/cleanroommc/bogosorter/BogoSortAPI.java#L294-L295) is true only for a container that implements `ISortableContainer` or whose class has a registered compat. An unknown container gets no sort button.
  - [ISortableContainer](https://github.com/GTNewHorizons/InventoryBogoSorter/blob/1.3.54-GTNH/src/main/java/com/cleanroommc/bogosorter/api/ISortableContainer.java#L13-L33) has `buildSortingContext(ISortingContextBuilder)` and an optional `getPlayerButtonPosSetter()`.
  - [IBogoSortAPI.addCompat](https://github.com/GTNewHorizons/InventoryBogoSorter/blob/1.3.54-GTNH/src/main/java/com/cleanroommc/bogosorter/api/IBogoSortAPI.java#L56) registers a compat for a container class without implementing the interface.
  - [InventoryManager.java#L183-L218](https://github.com/GTNewHorizons/InventoryBogoSorter/blob/1.3.54-GTNH/src/main/java/com/cleanroommc/bogosorter/common/dropoff/InventoryManager.java#L183-L218): drop-off accepts a vanilla `TileEntityChest`, or any inventory whose localized stack name or `getInventoryName()` contains one of the configured words (case-insensitive).
- **No precedent found:** ModularUI2 2.3.92 has no Bogosorter hook, and GT5-Unofficial only lists the mod ID. So every GregGuns container with a storage grid needs its own opt-in.
- **GregGuns does:** the chest container uses `@Optional.Interface(iface = "com.cleanroommc.bogosorter.api.ISortableContainer", modid = "bogosorter")`, or calls `addCompat` from a class loaded only when `Loader.isModLoaded("bogosorter")`. It adds the chest slots as one slot group. Names are "Reinforced Chest" and "Weapon Chest", so drop-off finds them.
- **Pack-side change:** none. Drop-off already matches "Chest".

## Requirements

- **GGS-07-10** With bogosorter loaded, the Reinforced and Weapon Chest GUIs show the sort button, and the chest slots form one slot group.
- **GGS-07-11** Without bogosorter, the chests work and no bogosorter class is loaded.
- **GGS-07-12** The chest item names contain the word "Chest" in the default English lang.
- **GGS-07-13** Shift-click moves chest contents into the player inventory without favoring TG slots (fixes MUTL-36).
- **GGS-07-14** The sorting context adds only the chest slots. The player inventory is not added, because Bogosorter adds it itself (see the `buildSortingContext` Javadoc).

## Tests

| Req | Check |
|---|---|
| 10, 14 | Manual check in a client with bogosorter; unit test on the slot group builder |
| 11 | `runServer` boot and client boot without bogosorter |
| 12 | Unit test on the lang file |
| 13 | Unit test on `transferStackInSlot` |

## Open questions

- Does the Weapon Chest get an item filter, or is it dropped from the build? A design record on storage would settle it.
- Do the machine GUIs (Repair Bench) need a sort group too? The bench inventory is nine plain slots, so likely yes, but nothing in the pack requires it.
