# NEI visibility

Which GregGuns items NEI shows, hides and collapses.

- **Techguns baseline:** [item-catalogue.md](../../techguns-spec/06-items/item-catalogue.md) (metas, debug items, spawn eggs), [special-items.md](../../techguns-spec/06-items/special-items.md)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (dropped machines leave no NEI handlers), [CONTRIBUTING § NEI](../../../CONTRIBUTING.md#gtnh-integration) ("Hide unobtainable and technical items")
- **Pack mods:** NotEnoughItems (`NotEnoughItems`, 2.8.155-GTNH)
- **Roadmap:** M2 (items exist), M4 (recipes shown by GT's maps)

## What changes from Techguns

Techguns lists every shared-item meta in the creative tab and registers four debug tools unconditionally ([ITEM-13](../../techguns-spec/06-items/index.md#suspected-bugs)). In NEI that is 110 entries in one block, with empty and full variants side by side. GregGuns hides the technical items and collapses the variants. Recipe handlers for the dropped Techguns machines are not written; GT's recipe maps appear in NEI on their own.

| Techguns item group | GregGuns NEI treatment | How |
|---|---|---|
| Debug tools (Block Reader, Building Scan Tool, Structure Test Tool, Ore Cluster Debugger) | Not registered in release builds; if kept for development, hidden | `API.hideItem` in a dev-only branch |
| Ore Cluster Scanner | Not built (no ore generation, [0005](../../design/0005-world-presence.md)) | n/a |
| Empty magazines, tanks and cells | Collapsed with their full form | Pack `collapsibleitems.cfg` |
| Ammo of one calibre in several tiers | Collapsed per calibre | Pack `collapsibleitems.cfg` |
| Spawn eggs | Collapsed with the pack's existing spawn-egg group | Pack `collapsibleitems.cfg` |
| Guns | One entry per gun, in the default state, not one per damage value | `API.setItemListEntries` |
| Items with 3D-only models | Normal entries; fix icons ([ITEM-08](../../techguns-spec/06-items/index.md#suspected-bugs)) in the item code | n/a |

## Integration points

### NEI: runtime hiding and item lists

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/NEI/hiddenitems.cfg` (373 lines; one pack-managed rule per line). The header documents the syntax: `modname:itemid`, `$orename`, `tag.color=red`, damage values `0` or `0-12`, `!` (not), `r/…/` (regex), `,` (or within a token), `|` (or between items).
- **Source:** [`API`](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/api/API.java):
  - `hideItem(ItemStack)` ([L151](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/api/API.java#L151)) and `hideItem(String rule)` ([L158](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/api/API.java#L158)) hide at run time.
  - `setItemListEntries(Item, Iterable<ItemStack>)` ([L211](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/api/API.java#L211)) replaces the stacks NEI lists for an item.
  - `addSubset(String, Iterable<ItemStack>)` ([L329](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/api/API.java#L329)) adds a subset in NEI's subset menu.
  - Entry point: the pack already has an NEI plugin precedent in EnhancedLootBags (`integration/nei/NEIEnhancedLootBagsConfig`). GregGuns follows the same pattern, an `IConfigureNEI` class.
- **GregGuns does:** a client-side NEI plugin class, active only when `NotEnoughItems` is loaded (`Loader.isModLoaded("NotEnoughItems")`, with the class kept out of the common code path). In `loadConfig()` it:
  - hides items that exist but should not be listed (debug tools, if present);
  - calls `setItemListEntries` for each gun so NEI lists one stack, not one per damage value;
  - optionally adds subsets such as "GregGuns: Weapons" and "GregGuns: Ammo".
- **Pack-side change:** none required for hiding. A pack author who prefers a file rule can use `hiddenitems.cfg`; GregGuns does not depend on it.

### NEI: collapsible groups (file only)

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/NEI/collapsibleitems.cfg` (958 lines). One line is one group; an item goes in the first group that matches. The first group is spawn eggs:
  `minecraft:spawn_egg | EnderZoo:SpawnEggEnderZoo | … | TwilightForest:item.tfspawnegg | miscutils:item.BasicMetaSpawnEgg | tconstruct:titleicon`. GT parts are grouped with regexes such as `$r/^bolt/`.
- **Source:** [`CollapsibleItems.load`](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/CollapsibleItems.java#L62-L80) reads `collapsibleitems.cfg` (when collapsible items are enabled in the client config) and builds groups from it. The API listing above has no call to add a collapsible group, so groups come from the file.
- **GregGuns does:** nothing at run time.
- **Pack-side change:** add to `collapsibleitems.cfg` (names are placeholders, final in M2):

```
# spawn egg: append to the existing first group
... | gregguns:spawn_egg
# gregguns magazines, tanks and cells (full and empty)
r/^gregguns:(mag|tank|cell)_/
# gregguns ammo by calibre
r/^gregguns:ammo_9mm/
r/^gregguns:ammo_rifle/
r/^gregguns:ammo_shotgun/
# gregguns armour sets
r/^gregguns:armor_t1_/
r/^gregguns:armor_t2_/
```

The regexes assume the registry names follow the prefixes shown. If the names change, the pack lines change with them.

## Requirements

- **GGS-06-26.** GregGuns lists no debug or technical item in NEI in a release build.
- **GGS-06-27.** Each gun appears once in NEI.
- **GGS-06-28.** All NEI calls are in a class that is loaded only when `NotEnoughItems` is present. The server never loads it.
- **GGS-06-29.** GregGuns registers no NEI recipe handler for a process GT already shows (any recipe map in [ammo.md](ammo.md#gregtech-recipe-maps-for-ammo)).

## Tests

| ID | Verified by |
|---|---|
| GGS-06-26 | Manual check in NEI; unit test that the debug items are absent from the registry in a release profile |
| GGS-06-27 | Manual check in NEI |
| GGS-06-28 | `runServer` boot with NEI absent from the dev classpath; `runClient` with it |
| GGS-06-29 | Code review |

## Open questions

- **Guns and `getSubItems`.** Whether the simplest fix is for the gun item's own `getSubItems` to return one stack (no NEI API call) is a coding choice for M2. The NEI call is the fallback.
- **Registry names.** The regex lines in the pack need the final names.
