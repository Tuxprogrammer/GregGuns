# NEI order

The one order in which GregGuns items and blocks appear in NEI and the creative tab, and how it reaches worlds that were created before it was fixed.

- **Techguns baseline:** none; Techguns lists items in meta order ([item-catalog.md](../../techguns-spec/06-items/item-catalog.md)). This is a GregGuns decision (owner request 2026-10-20).
- **Code:** `NeiOrder` (the list), `NeiOrderRemap` (existing-world logic), `NEIGregGunsConfig` (the NEI plugin), and the per-area order classes `GunsItemOrder`, `ArmorItemOrder`, `UtilityItemOrder` and `NpcEggOrder`.
- **Pack mods:** NotEnoughItems 2.8.155-GTNH.

## The order

Ten blocks, always in this sequence. Inside every block that has tiers, items run tier by tier: Steam, LV, MV, HV, EV, IV, LuV, ZPM. The "first" and "last" columns are checked against the code by `NeiOrderTest`; a new item must slot into its block, or the test names where it landed.

| # | Block | What it holds, and the order inside | First | Last |
|---|---|---|---|---|
| 1 | `WEAPONS_AND_AMMO` | Each gun, its akimbo variant, the magazines it uses (empty, then full), the loose rounds it uses; then the tier's melee tools and grenades. Shared ammo sits at the lowest-tier gun that uses it. | `handcannon` | `ammo_rocket_nuke` |
| 2 | `GUN_PARTS` | Gun parts, then gun components (tanks, cells), tier by tier | `part_mechanical_1` | `cell_nuclear_depleted` |
| 3 | `MACHINE_BLOCKS` | The Ammunition Works casing block. The Ammo Presses, Ammunition Works and benches are GT machines, see below | `casings` | `casings` |
| 4 | `ARMOR` | Every armor set in catalog order, helmet, chestplate, leggings, boots, tier by tier | `t1_scout_helmet` | `t3_exo_boots` |
| 5 | `ACCESSORIES` | Special-slot gear (goggles, pouch, masks, oxygen) then flight devices, tier by tier | `night_vision_goggles` | `anti_gravity_device` |
| 6 | `ARMOR_PARTS` | Crafting parts of armor, tier by tier | `part_heavy_cloth` | `part_glider_wing` |
| 7 | `UTILITY` | Turret plates cheapest first, the Explosive Charge, Reinforced Chest, Weapon Chest, Camp Flag, Bio Blob | `turret_armor_iron` | `bio_blob` |
| 8 | `NPC_EGG` | The spawn egg; its subtypes follow [`NpcEggOrder`](../10-entities-npcs/index.md) (undead, bug, factions weakest first, mutants, bosses, machines) | `npc_egg` | `npc_egg` |
| 9 | `WORLD_BLOCKS` | Bug Nest, Bug Nest Slime | `bug_nest` | `bug_nest_slime` |
| 10 | `DEV` | Dev-only items (registered only in a dev environment) | `probe_tool_gun` | `probe_tool_gun` |

Blocks 1 to 3 register in the guns module, 4 to 6 in armor, 7 in utility, 8 in npcs, 9 and 10 in worldgen and guns (the dev probe is registered last). That is also the module registration order, so a NEW world's numeric ids already follow the table.

### GT machines

The Ammo Presses, the Ammunition Works and the Repair Bench, Armory Gantry and Camo Bench are GregTech meta tile entities. NEI lists them under GregTech, in GT's own meta order, and their meta IDs are reserved ([GregGunsMTEIDs](../08-machines-production/machine-roster.md)). GregGuns cannot move them into its own group, and does not try.

## How NEI shows it in existing worlds

In 1.7.10 a saved world keeps the numeric item id it gave each item (FML's item data in `level.dat`). NEI's pack default sort is `minecraft, mod, id, default, damage, name`, so within GregGuns the decider is the numeric id, and a world created on 0.2.0 keeps the old, scattered ids. Changing registration order alone fixes new worlds only.

### What NEI offers (2.8.155-GTNH)

- **Sort options**: `API.addSortOption(name, Comparator<ItemStack>)` ([API.java#L358](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/api/API.java#L358)) adds a row to the player's sort list. This is the only ordering hook. The saved list lives in the client config (`inventory.itemsort`); an option the saved list does not name is appended at the END ([ItemSorter.fromSaveString](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/ItemSorter.java#L128-L139)), after `damage` and `name`, where it can never decide anything unless the player drags it up in the sort GUI.
- **Collapsible groups**: file only (`collapsibleitems.cfg`), they hide variants and do not order.
- **IMC**: `registerHandlerInfo`, `removeHandlerInfo`, `registerCatalystInfo`, `removeCatalystInfo`, `addItemInfo` ([IMCHandler](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/config/IMCHandler.java#L31-L45)). None of them orders items.
- **No "custom order" setting** exists. GT5-Unofficial does not control its NEI order either (it has no sort option; its machines list by meta).

### What GregGuns does

`NEIGregGunsConfig` is an NEI plugin ([discovery](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/ClientHandler.java#L317-L346); the class name starts with `NEI` and ends with `Config`). Plugins load before the saved sort list is applied ([NEIClientConfig#L1017-L1034](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.155-GTNH/src/main/java/codechicken/nei/NEIClientConfig.java#L1017-L1034)). In `loadConfig` it replaces the comparator of the existing `nei.itemsort.id` entry, so the row keeps its place in the player's list:

- A GregGuns item sorts by an **effective id**: GregGuns' own numeric ids, handed back in `NeiOrder` order (`NeiOrderRemap`). The first listed item gets the smallest id GregGuns holds, the next the next smallest, and so on.
- Every other item sorts by its real id.

Because the effective ids are a permutation of ids GregGuns already holds, other mods' items compare to GregGuns items exactly as before, so the sort stays a valid total order.

**Existing worlds are fixed**: the order is computed from registry names at NEI load, not from saved ids. Limits:

- It applies while the `Item ID` row is in the player's sort list, as it is by default. A player who moves it below `Name` sees names decide first, and the sort GUI's reset restores the default.
- It changes only GregGuns' own order. The creative tab, which sorts by id, still follows the saved ids in an old world; a new world gets the table order from registration.
- GT machines stay in GT's order (see above).
- Spawn egg subtypes keep their order through NEI's `default` sorter, which uses the order the item lists its subtypes.
