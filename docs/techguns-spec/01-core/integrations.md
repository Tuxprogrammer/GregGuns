# Mod integrations

Source: `plugins/ftbutilities/*`, `plugins/tconstruct/*`, `plugins/thermalexpansion/*`, plus the loader checks in `Techguns.postInit`. MineTweaker (`plugins/minetweaker`) is documented in 08-machines-production. Chisel and ForgeMultipart only appear as load-order hints. Chisel is in fact required on the client (CORE-03). ForgeMultipart integration (`TGMultiPartRegister`) is commented out.

All checks happen in **postInit** through `Loader.isModLoaded`.

## FTB Utilities (`FTBU`): friends

| Class | Role |
|---|---|
| `IFTBUtilitiesIntegration` | one method: `areFriends(UUID a, UUID b)` |
| `FTBUtilitiesIntegrationActual` | delegates to FTBU's `FriendsAPI.areFriends(a, b)` |
| `FTBUtilitiesIntegrationDummy` | always returns **true** |

The chosen implementation is stored in `TGNpcFactions.friendsAPI`. `TGNpcFactions.isHostile(uuidA, uuidB)` returns the negation of `areFriends`. Turrets and owner-based security use it to decide whether one player's turret attacks, or blocks access for, another player (09/10).

- Without FTBU, every player counts as everyone's friend: turrets never target other players and "Friends" security means everyone.
- With FTBU, the FTBU friend list decides.

## Tinkers' Construct (`TConstruct`)

When TConstruct is loaded, `Techguns.isTConstructLoaded` is set to true. The integration object is created by reflection: the client class on the client side, the common class otherwise. Both implement `ITechgunsTConstructIntegration`, a one-method interface (`init()`), so the hard dependency on TConstruct classes stays behind reflection. Then `init()` is called.

### Common: materials sent over IMC

These are sent with `FMLInterModComms.sendMessage("TConstruct", …)`. The IDs are hard-coded (a TODO says "id to config file").

| Material id | Name | Harvest lvl | Durability | Mining speed | Attack | Handle mod. | Reinforced | Text style | Colour (ARGB) | Bow draw / proj. speed | Arrow mass / fragility |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 50 | Plastic | 1 | 1000 | 350 | 1 | 1.5 | — | WHITE | FF C8C8C8 | 150 / 1.2 | 1.0 / 0.95 |
| 51 | Carbon | 5 | 2500 | 250 | 4 | 2.0 | 3 | BLACK | FF 232323 | 180 / 1.3 | 1.1 / 0.975 |
| 53 | ObsidianSteel | 4 | 2000 | 250 | 3 | 2.0 | 2 | DARK_PURPLE | FF 321941 | 180 / 1.2 | 1.2 / 0.975 |
| 52 | Titanium | 4 | 2250 | 500 | 3 | 2.5 | — | GRAY | FF DCDCDC | 180 / 1.5 | 1.0 / 0.975 |
| 54 | Rubber | 0 | 500 | 2500 | 0 | 1.0 | — | DARK_GRAY | FF 414100 | 20 / 1.0 | 0.25 / 1.0 |

Each is sent as an `addMaterial` message. Then the item mappings follow (all items are `TGItems` sub-items, 06):

| Message | Material | Item | Value |
|---|---|---|---|
| `addMaterialItem` (repair item) | 50 Plastic | plasticSheet | 2 |
| `addPartBuilderMaterial` | 50 Plastic | plasticSheet | 2 |
| `addMaterialItem` | **50 Plastic again** | plasticSheet | 2 |
| `addPartBuilderMaterial` | 51 Carbon | carbonPlate | 1 |
| `addPartBuilderMaterial` | 52 Titanium | ingotTitanium | 1 |
| `addPartBuilderMaterial` | 53 ObsidianSteel | ingotObsidianSteel | 1 |
| `addPartBuilderMaterial` | 54 Rubber | rubberBar | 1 |

The duplicate Plastic `addMaterialItem` looks like a copy-paste slip: carbon, titanium, obsidian steel and rubber get no repair-item mapping. Also, sending the messages in postInit may be too late for TConstruct to read them (CORE-26).

**Lang keys for the material names:**
- `material.plastic` = Plastic
- `material.carbon` = Carbon
- `material.titanium` = Titanium
- `material.obsidiansteel` = Obsidian Steel
- `material.rubber.name` = Rubber (this one has a different key shape from the others)

### Client: inventory tab

`TechgunsTConstructIntegrationClient.init()` runs the common part and then registers `TGInventoryTab` with TConstruct's `TabRegistry`.
- `TGInventoryTab` extends TConstruct's `AbstractTab`. Its icon is the 9 mm bullets item and it is always added to the list.
- Clicking it sends `PacketOpenPlayerGUI(17)`. The 17 is hard-coded and ignores `TechgunsGUI_TabID` (CORE-07).
- `addTabs(guiLeft, guiTop, buttonList)` updates the tab positions and adds TConstruct's tabs to a GUI. The Techguns player inventory GUI calls it so that, with TConstruct installed, its tab row matches TConstruct's.
- While TConstruct is loaded, the vanilla-inventory tab buttons from [input-and-hud.md](input-and-hud.md#inventory-tab-buttons) are **not** added.

## Thermal Expansion (`ThermalExpansion`)

`TechgunsThermalExpansionIntegration.init()` has an empty body. A Smelter recipe (iron ingot + coal dust → steel ingot, 1000 RF) is commented out. A static helper `addSmelterRecipe(energy, in1, in2, out1, out2, chance)` would build a TE `SmelterRecipe` IMC compound with keys `energy`, `primaryInput`, `secondaryInput`, `primaryOutput`, optional `secondaryOutput` and `secondaryChance`, but nothing calls it. The class's `@Optional.Interface` names a non-existent interface path (`techguns.plugins.tconstruct.ITechgunsThermalExpansionIntegration`) (CORE-27).

## MineTweaker 3 (`MineTweaker3`)

`TechgunsMineTweakerIntegration.init()` is called. Described in 08-machines-production.
