# Extra equipment slots (Baubles Expanded)

- **Techguns baseline:** [extra-inventory.md](../../techguns-spec/05-armor/extra-inventory.md#1-data-model-tgplayerinventory-iinventory-12-slots), [special-equipment.md](../../techguns-spec/05-armor/special-equipment.md#2-registered-items)
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0003 energy model](../../design/0003-energy-model.md)
- **Pack mods:** Baubles Expanded (`Baubles|Expanded`, 2.2.25-GTNH), Galacticraft (`GalacticraftCore`, 3.4.34-GTNH; see [oxygen-gear.md](oxygen-gear.md))
- **Roadmap:** M3 (armour and equipment)

## What changes from Techguns

Techguns adds a 12-slot inventory tab, its own GUI, and two DataWatcher entries to show face and back gear to other players. GregGuns drops the tab. The pack already ships Baubles Expanded with a sidebar of typed slots, so GregGuns equipment becomes baubles where nothing else in the pack needs to see it.

The rule is: **anything a pack system must see is an armour-slot item or a Galacticraft slot item; anything only GregGuns reads is a bauble.** GT hazard checks and Galacticraft breathing checks read the vanilla armour slots only (see [hazard-protection.md](hazard-protection.md) and [oxygen-gear.md](oxygen-gear.md)).

| Techguns slot (index) | Techguns items | GregGuns slot | Why |
|---|---|---|---|
| Face (0), protective | Gas Mask, Tactical Mask | Helmet armour slot | GT's gas hazard check reads armour slots only. See [hazard-protection.md](hazard-protection.md) |
| Face (0), protective | Oxygen Mask | Galacticraft mask slot (subclass of GC's mask item) | GC reads its own slots. See [oxygen-gear.md](oxygen-gear.md) |
| Face (0), sensor | Night Vision Goggles | Baubles `head` | Nothing in the pack reads it, so a bauble keeps the "goggles plus helmet" feel |
| Back (1) | Jetpack, Jump Pack, Glider, Anti Gravity Device | Baubles `wings` (one slot, so only one back device at a time) | The pack already has a `wings` slot, and one slot removes flight stacking inside GregGuns |
| Back (1) | Oxygen Tanks | Galacticraft tank slots (subclass of GC's tank item) | GC drains and displays its own tanks |
| Ammo (6 to 11) | Ammo pouch contents | One **Ammo Pouch** item in Baubles `quiver` | One slot holds a pouch with its own 6-slot GUI. No tab needed |
| Food (2 to 4) | Auto-feeder food | Dropped (see Open questions) | No pack integration |
| Heal (5) | nothing | Dropped | Dead slot in Techguns ([ARM-17](../../techguns-spec/05-armor/index.md#suspected-bugs)) |

The Techguns toggle buttons (HUD, night vision, safe mode, step assist, jetpack) become key bindings and item-use toggles. They need no slot integration.

## Integration points

### Baubles Expanded: typed slots under `manualSlotSelection`

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/Baubles.cfg`, `menu.manualSlotSelection` = `true`. The slot list is the pack's `override.slotTypeOverrides`. As shipped it has 18 entries: `amulet`, `ring`, `ring`, `belt`, `Terminal`, `quiver`, `charm_pouch`, `wings`, `focus_pouch`, `cape`, `gauntlet`, `charm`, `title`, `head`, `heartcanister_red`, `heartcanister_yellow`, `heartcanister_green`, `universal`. There is one `head`, one `wings` and one `quiver`. `slotUniversalOverrides` lets `amulet`, `ring`, `belt`, `charm`, `head` and `universal` items also fit the `universal` slot.
- **Source:**
  - Slot types pre-registered by the mod, including `head`, `body`, `cape`, `quiver` and `wings`: [BaubleExpandedSlots.java#L317-L332](https://github.com/GTNewHorizons/Baubles-Expanded/blob/2.2.25-GTNH/src/main/java/baubles/api/expanded/BaubleExpandedSlots.java#L317-L332).
  - `IBaubleExpanded.getBaubleTypes(ItemStack)` is how an item names the slot types it fits: [IBaubleExpanded.java#L13-L29](https://github.com/GTNewHorizons/Baubles-Expanded/blob/2.2.25-GTNH/src/main/java/baubles/api/expanded/IBaubleExpanded.java#L13-L29).
  - Extra types can be added with `tryRegisterType`, only during pre-initialisation: [BaubleExpandedSlots.java#L45-L57](https://github.com/GTNewHorizons/Baubles-Expanded/blob/2.2.25-GTNH/src/main/java/baubles/api/expanded/BaubleExpandedSlots.java#L45-L57).
  - With `manualSlotSelection` on, `overrideSlots` replaces the assignments from the config list: [BaubleExpandedSlots.java#L301-L313](https://github.com/GTNewHorizons/Baubles-Expanded/blob/2.2.25-GTNH/src/main/java/baubles/api/expanded/BaubleExpandedSlots.java#L301-L313). A registered type that is not in the pack list gets **no slot**.
  - Slot positions for a type: [getIndexesOfAssignedSlotsOfType, L184](https://github.com/GTNewHorizons/Baubles-Expanded/blob/2.2.25-GTNH/src/main/java/baubles/api/expanded/BaubleExpandedSlots.java#L184).
  - Items in baubles drop on death unless `keepInventory`: [EventHandlerEntity.java#L44-L54](https://github.com/GTNewHorizons/Baubles-Expanded/blob/2.2.25-GTNH/src/main/java/baubles/common/event/EventHandlerEntity.java#L44-L54).
  - The mod syncs bauble contents to every player that starts tracking another: [EventHandlerEntity.java#L111-L116](https://github.com/GTNewHorizons/Baubles-Expanded/blob/2.2.25-GTNH/src/main/java/baubles/common/event/EventHandlerEntity.java#L111-L116). A renderer can read `BaublesApi.getBaubles(player)` on the client. GregGuns therefore needs no DataWatcher IDs, which retires the `DataWatcherID_FaceSlot` and `DataWatcherID_BackSlot` config entries.
  - Precedent in the pack: GT's own MagLev Harness is an `IBaubleExpanded` item for `belt` and `wings`: [ItemMagLevHarness.java#L41-L43](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/ItemMagLevHarness.java#L41-L43).
- **GregGuns does:**
  - Night Vision Goggles, the four back devices and the Ammo Pouch implement `IBaubleExpanded` and return `{"head"}`, `{"wings"}` and `{"quiver"}`.
  - `onWornTick` runs server-side only. State changes go through the item's NBT, so nothing is mutated on the client (fixes [ARM-34](../../techguns-spec/05-armor/index.md#suspected-bugs)).
  - Non-bauble code (ammo lookup, keybinds) finds equipment through `BaublesApi.getBaubles(player)` and `getIndexesOfAssignedSlotsOfType`, never by fixed index. Baubles Expanded is a soft dependency. Each bauble item declares the interface with `@Optional.Interface(iface = "baubles.api.expanded.IBaubleExpanded", modid = "Baubles|Expanded")`, and bauble-reading code is guarded by `Loader.isModLoaded("Baubles|Expanded")`.
  - Tooltips use `BaubleItemHelper.addSlotInformation`, as the MagLev Harness does.
  - The slot holds one item. GregGuns sets no `slotStackLimitOverrides`.
- **Pack-side change:** none for the default mapping, because `head`, `wings` and `quiver` are already in the list. If the pack wants GregGuns its own slots instead (Open questions), it must add the new type names to `slotTypeOverrides` in `config/Baubles.cfg`. The list is at 18 entries; the API's `slotLimit` is 20 ([BaubleExpandedSlots.java#L15](https://github.com/GTNewHorizons/Baubles-Expanded/blob/2.2.25-GTNH/src/main/java/baubles/api/expanded/BaubleExpandedSlots.java#L15)), though the Javadoc and the code of `overrideSlots` disagree on whether it is enforced.

### TravellersGear: not in the pack

`config/TravellersGear.cfg` and `config/TravellersGear_inv.cfg` exist under the pack config, but no TravellersGear jar is in `mods/` and `MOD-INDEX.md` has no entry for it. They are stale files. GregGuns does not integrate with TravellersGear.

### Night vision: avoid fighting the pack's other sources

- **Source:** GT's Mech Armor night-vision behaviour adds the effect with the *ambient* flag and, when switched off, removes only an ambient effect, so a potion's night vision survives: [NightVisionBehavior.java#L44-L62](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/armor/behaviors/NightVisionBehavior.java#L44-L62). GT drains 2 EU per tick for it.
- **GregGuns does:** the goggles apply `Potion.nightVision` with `ambient = true`, remove the effect on unequip or toggle-off only if it is ambient, and remove `Potion.blindness` only while on. This fixes [ARM-29](../../techguns-spec/05-armor/index.md#suspected-bugs) and matches GT.
- **Pack-side change:** none.
- **Not done:** IC2's goggles are helmet armour with no source in the pack index; GregGuns does not inspect them. Two sources can overlap harmlessly because both use the same potion.

## Requirements

- **GGS-05-01.** GregGuns registers no player-inventory tab, no extended-properties inventory and no DataWatcher entries for equipment.
- **GGS-05-02.** Night Vision Goggles, Jetpack, Jump Pack, Glider and Anti Gravity Device implement `IBaubleExpanded` with one slot type each, as in the table above.
- **GGS-05-03.** The Ammo Pouch is a stack-size-1 `IBaubleExpanded` item for `quiver` with a 6-slot GUI. Ammo lookup scans the pouch before the main inventory.
- **GGS-05-04.** Other players see worn bauble equipment by reading synced Baubles data on the client.
- **GGS-05-05.** Equipment effects are applied only on the server. No tick code changes a stack on the client.
- **GGS-05-06.** Goggles apply night vision with the ambient flag and never remove a non-ambient Night Vision effect.
- **GGS-05-07.** If Baubles Expanded is absent (dev runs), the bauble items still load without class-loading errors. They can then only be held, not worn, and GregGuns logs one warning.

## Tests

- GGS-05-01, 04: manual, two clients; unit test that no packet or DataWatcher registration exists for equipment.
- GGS-05-02, 03: unit test on `getBaubleTypes`; `runServer` check that `BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType("wings")` is non-empty under the pack config.
- GGS-05-05: unit test that `onWornTick` does nothing when `world.isRemote`.
- GGS-05-06: unit test with a potion night-vision effect (non-ambient) present before and after goggles are toggled.
- GGS-05-07: `runServer` without Baubles.

## Open questions

- **Dedicated slots or shared ones?** `wings` also takes GT's MagLev Harness, and `head` may be wanted by other mods. A dedicated `gregguns_back` type would need a pack-side `slotTypeOverrides` edit. Settle in a new record, "Equipment slot ownership".
- **Auto-feeder food slots.** No pack integration. Keep inside the Ammo Pouch, or drop? Needs a record.
- **No-Baubles fallback.** Should GregGuns declare Baubles Expanded a hard dependency? Dev runs would be simpler, but it is a change to `dependencies.gradle` policy.
