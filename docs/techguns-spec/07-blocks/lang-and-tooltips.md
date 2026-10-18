# 07-blocks: lang keys and tooltips

Source: `lang/en_US.lang`. Keys are given exactly. "Derived key" means the key the code actually looks up; a mismatch with the lang file is flagged.

## Block and item names

| Block / stack | Derived key | English |
|---|---|---|
| `camoNetPane` (block) | `techguns.tile.camoNetPane.name` | Camo Net |
| `camoNetPane:0` / `:1` / `:2` | `techguns.tile.camoNetPane_0.name` / `_1` / `_2` | Camo Net (Woodland) / Camo Net (Snow) / Camo Net (Desert) |
| `camoNetRoof` (block) | `techguns.tile.camoNetRoof.name` | Camo Net Roof |
| `camoNetRoof:0` / `:1` / `:2` | `techguns.tile.camoNetRoof_0.name` … | Camo Net Roof (Woodland) / (Snow) / (Desert) |
| `screen` | `techguns.tile.screen.name` | Radar Screen |
| `controlpanel` | `techguns.tile.controlpanel.name` | Control Panel |
| `ladder01` (block) | `techguns.tile.LadderBlock.name` | Metal Ladder |
| `ladder01:0`–`:3` | `techguns.tile.LadderBlock_0.name` … `_3` | Metal Ladder (Grey) |
| `ladder01:4`–`:7` | `…_4` … `_7` | Metal Ladder (Rusty) |
| `ladder01:8`–`:11` | `…_8` … `_11` | Metal Ladder (Shiny) |
| `ladder01:12`–`:15` | `…_12` … `_15` | Metal Ladder (Black) |
| `lamp01:0`–`:6` | `techguns.tile.Lamp_0.name` … `_6` | Lamp (Yellow) |
| `lamp01:7` | `techguns.tile.Lamp_7.name` | Lantern (Yellow) |
| `lamp01:8`–`:14` | `techguns.tile.Lamp_8.name` … `_14` | Lamp (White) |
| `lamp01:15` | `techguns.tile.Lamp_15.name` | Lantern (White) |
| `door01` | `tile.null.name` (no `setBlockName`) | *(no entry; shows the raw key)* (BLK-03). The door item is `techguns.item.bunkerDoor.name` = "Bunker Door" (06-items). |
| `sandbags` | `techguns.tile.sandbags.name` | Sandbags |
| `bugnesteggs` | `techguns.tile.bugnesteggs.name` | Strange Glowing Eggs |
| `bugnestslimy` | `techguns.tile.slimetrail.name` | Slime Trail |
| `airMarker` | `techguns.tile.AirMarker.name` | Air Marker |
| `oreTitanIron:0` / `:1` | `techguns.tile.oreTitanIron_0.name` / `_1` | Titanium Iron Ore / Uranium Ore |
| `TGMonsterSpawner:0` / `:1` | `techguns.tile.TGMonsterSpawner_0.name` / `_1` | Bug Hole / Zombie Hole |
| `acid` | `techguns.tile.creeperAcid.name` | Creeper Acid |
| Creeper acid fluid | `fluid.creeperAcid` | Creeper Acid (`techguns.fluid.creeperAcid` = "Creeper Acid" also exists but is never read) |
| `tgchest` | `techguns.tile.tgchest.name` | Reinforced Chest |
| `tgchest_weapon` | `techguns.tile.tgchest_weapon.name` | Weapon Chest |
| `BioBlobSmall` | `tile.Bioblob.name` (the class lacks the `techguns.` prefix) | *(no match)*. The lang has `techguns.tile.Bioblob.name` = "Bio Blob" (BLK-09). |
| `container` | `techguns.tile.chisel.container.name` | Container Block |
| `concrete` | `techguns.tile.chisel.concrete.name` | Concrete |
| `metalpanel` | `techguns.tile.chisel.metalpanel.name` | Metal Panel |
| `metalpanel2` | `techguns.tile.chisel.metalpanel2.name` | Metal Panel (Connected) |
| `bugnest` | `techguns.tile.chisel.bugnest.name` | Hardened Sand |
| `oreCluster:N` | `techguns.tile.oreCluster_N.name` | Ore Cluster (Iron), (Gold), (Redstone), (Coal), (Lapis), (Diamond), (Emerald), (Copper), (Tin), (Lead), (Osmium), (Silver), (Nickel), (Aluminium), (Platinum), (Uranium), for N = 0–15 |
| `oreCluster2:N` | `techguns.tile.oreCluster2_N.name` | Ore Cluster (Nether Quartz), (Titanium Iron), (Certus Quartz), (Oil Hole), (Glowstone), for N = 0–4 |
| `oreClusterCustom`, `oreClusterCustom2` | `techguns.tile.oreClusterCustom.name`, `techguns.tile.oreClusterCustom2.name` | Ore Cluster |
| (unused `ItemBlockOreCluster`) | `techguns.tile.oreCluster.name` | Ore Cluster |
| `oreTin`, `oreCopper`, `oreLead` (01-core blocks) | `techguns.tile.oreTin.name`, `techguns.tile.oreCopper.name`, `techguns.tile.oreLead.name` | Tin Ore, Copper Ore, Lead Ore |

Machine names (`techguns.tile.BasicMachine_N.name`, `techguns.tile.oreDrill_N.name`, `techguns.tile.OreDrillBlock_N.name`, `techguns.tile.machineMultiBlock_N.name`) are documented by 08 and 09.

## Chisel variation descriptions

Chisel shows these as tooltip lines on the carvable item.

| Key | English |
|---|---|
| `techguns.tile.container.0.desc` | Shipping Container (Red) |
| `techguns.tile.container.1.desc` | Shipping Container (Green) |
| `techguns.tile.container.2.desc` | Shipping Container (Blue) |
| `techguns.tile.container.3.desc` | Shipping Container (Orange) |
| `techguns.tile.container.4.desc` | Futuristic Container |
| `techguns.tile.container.5.desc` | Futuristic Container (Rusty) |
| `techguns.tile.concrete.0.desc` | Light |
| `techguns.tile.concrete.1.desc` | Dark |
| `techguns.tile.concrete.2.desc` | Steel Frame Light |
| `techguns.tile.concrete.3.desc` | Steel Frame Dark |
| `techguns.tile.concrete.4.desc` | Weathered Column |
| `techguns.tile.concrete.5.desc` | Light Noise |
| `techguns.tile.concrete.6.desc` | Dark Noise |
| `techguns.tile.metalpanel.0.desc` … `.7.desc` | Rusty, Rusty 2, Hazard, Dark, Rusty Grate, Rusty Grate 2, Hazard Grate, Dark Grate |
| `techguns.tile.metalpanel2.0.desc` … `.6.desc` | Rusty, Hazard, Silver Bars, Dark Bars, Shiny, Dark, Silver |
| `techguns.tile.bugnest.0.desc` | Bugnest Sand |

The machine variations registered in TGBlocks use an empty description string.

## Code-generated tooltip and chat lines

| Source | Condition | Text (key → English) |
|---|---|---|
| `GenericItemBlockCamoChangeable.addInformation` | always (camo net, camo net roof, ladder items) | `techguns.tile.camoNet.tooltip` ("Current Camo") + ": " + the translated variation name. Nets: `techguns.tile.camoNetPane.camo.<m>` / `techguns.tile.camoNetRoof.camo.<m>` = Woodland / Snow / Desert. Ladder: `techguns.tile.LadderBlock.<damage>` with damage 0, 4, 8, 12 = Grey / Rusty / Shiny / Black. Example: "Current Camo: Snow". |
| `ItemBlockTGChest.addInformation` | stack NBT has `TileEntityData` | `techguns.block.chestHasContents` → "Has contents" |
| `ItemBlockMetaDataMachine.addInformation` | stack NBT has `TileEntityData` | `techguns.block.hasTileEntityData` → "Has TileEntity Data" |
| `ItemBlockOreClusterCustom.addInformation` | always | the slot's fluid localized name, or the ore stack's `<unlocalized>.name` translated (e.g. "Iron Ore") |
| `BlockTGChest.onBlockActivated` (client) | tile entity not usable by the player | `techguns.container.security.denied` → "[DENIED] You have no permission to use" |
| `ItemBlockOreDrill.placeBlockAt` (client) | rod placed on a cluster that already has a rod | `techguns.msg.error.alreadyrod` → "Can't place here, this cluster already has a connected drill rod" |
| `ItemBlockOreDrill.placeBlockAt` (client) | controller placed next to a controller | key used: `msg.error.oredrill.controllernext` (**no entry**; the lang key is `techguns.msg.error.oredrill.controllernext` = "Can't place 2 ore drill controllers next to each other") (BLK-17) |

## Container (GUI) titles for the chests

| Key | English |
|---|---|
| `techguns.container.tgchest` | Reinforced Chest |
| `techguns.container.tgchest.double` | Large Reinforced Chest |
| `techguns.container.tgchest_weapon` | Weapon Chest |
| `techguns.container.tgchest_weapon.double` | Large Weapon Chest |

The security tooltip keys (`techguns.container.security.tooltip*`, `techguns.container.security.owner`) belong to the machine GUIs (09). Chest ownership is disabled in this version.
