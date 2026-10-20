# Registry Catalog

Every item, block, tile entity and entity that ICBM-Classic `prod/1.12` @ `9c1d985a` registers, with its registry name, form and the area that documents it. Source: `content/reg/ItemReg`, `BlockReg`, `EntityReg` (the explosive registrations in `ExplosiveInit` are [02-explosives/explosive-registry.md](../02-explosives/explosive-registry.md)), `api/refs/ICBMEntities`, and the constructors of each class, cited relative to `src/main/java/icbm/classic/`.

Conventions:
- Mod ID and domain are `icbmclassic`. The creative tab "ICBM-Classic" (`itemGroup.icbmclassic`) is the ICBM tab; its icon is the explosive missile.
- Registry names are lowercase in the running game. Several are *written* in mixed case in source (`radarGun`, `glassButton`) and the 1.12 `ResourceLocation` lowercases them. 1.7.10 registers case-sensitively, so a rebuild must pick one spelling (ICBM-06-15).
- "Meta" is the item damage value. For explosive devices it is the explosive's numeric ID ([explosive-registry.md](../02-explosives/explosive-registry.md#2-the-catalog)).
- "Gate" is a config flag in `icbmclassic/item` (`ConfigItems`). A blank gate means the entry is always registered.
- Owning-area links go to the sheet that documents the thing.

## Items

Registered by `ItemReg.registerItems`. Block items come after the plain items.

| Registry name | Class | Form and meta | Gate | Documented in |
|---|---|---|---|---|
| `icbmclassic:grenade` | `content/items/ItemGrenade` | Thrown grenade; meta = explosive ID (tier 1 only) | | [03 thrown-and-handheld](../03-missiles/thrown-and-handheld.md) |
| `icbmclassic:bombcart` | `content/items/ItemBombCart` | Cart item; meta = explosive ID | | [03 bomb-cart](../03-missiles/bomb-cart.md) |
| `icbmclassic:poisonpowder` | `prefab/item/ItemBase` | Plain item | | [dropped-and-deferred](dropped-and-deferred.md#h-powders-dropped) |
| `icbmclassic:sulfurdust` | `ItemBase` | Plain item; oredict `dustSulfur` | | [dropped-and-deferred](dropped-and-deferred.md#h-powders-dropped) |
| `icbmclassic:saltpeter` | `ItemBase` | Plain item; oredict `dustSaltpeter` | | [dropped-and-deferred](dropped-and-deferred.md#h-powders-dropped) |
| `icbmclassic:saltpeter_ball` | `ItemBase` | Plain item (furnace input) | | [dropped-and-deferred](dropped-and-deferred.md#h-powders-dropped) |
| `icbmclassic:antidote` | `content/items/ItemAntidote` | Consumable | | [antidote](antidote.md) |
| `icbmclassic:defuser` | `content/items/ItemDefuser` | Tool | | [05 handheld-tools](../05-detection-defense/handheld-tools.md) |
| `icbmclassic:radargun` | `content/items/ItemRadarGun` | Tool (source name `radarGun`) | | [05 handheld-tools](../05-detection-defense/handheld-tools.md) |
| `icbmclassic:remotedetonator` | `content/items/ItemRemoteDetonator` | Tool | | [05 handheld-tools](../05-detection-defense/handheld-tools.md) |
| `icbmclassic:laserdetonator` | `content/items/ItemLaserDetonator` | Tool | | [05 handheld-tools](../05-detection-defense/handheld-tools.md) |
| `icbmclassic:rocketlauncher` | `content/items/ItemRocketLauncher(false)` | Direct-fire launcher | | [03 thrown-and-handheld](../03-missiles/thrown-and-handheld.md) |
| `icbmclassic:ballisticlauncher` | `ItemRocketLauncher(true)` | Lobbed launcher | | [03 thrown-and-handheld](../03-missiles/thrown-and-handheld.md) |
| `icbmclassic:explosive_missile` | `content/items/ItemMissile` | Missile item; meta = explosive ID; meta 24 = the missile module. Old name `icbmclassic:missile` is remapped (see below) | | [03 missile-item](../03-missiles/missile-item.md) |
| `icbmclassic:surface_to_air_missile` | `content/missile/entity/anti/item/ItemSurfaceToAirMissile` | SAM item | | [03 missile-item](../03-missiles/missile-item.md) |
| `icbmclassic:cluster_missile` | `content/cluster/missile/ItemClusterMissile` | Cluster missile item | | [03 cluster-missile](../03-missiles/cluster-missile.md) |
| `icbmclassic:held_item_missile` | `content/missile/entity/itemstack/item/ItemHeldItemMissile` | Held-item missile item | | [03 missile-item](../03-missiles/missile-item.md) |
| `icbmclassic:empty_bomblet` | `ItemBase` | Plain item ("Droplet") | | [03 cluster-missile](../03-missiles/cluster-missile.md) |
| `icbmclassic:explosive_bomblet` | `content/cluster/bomblet/ItemBombDroplet` | Bomblet item; meta = explosive ID (tier 1) | | [03 cluster-missile](../03-missiles/cluster-missile.md) |
| `icbmclassic:parachute` | `content/cargo/ItemThrowableProjectile` | Cargo holder item | | [03 cargo](../03-missiles/cargo.md) |
| `icbmclassic:balloon` | `ItemThrowableProjectile` | Cargo holder item | | [03 cargo](../03-missiles/cargo.md) |
| `icbmclassic:ingot` | `content/items/ItemCrafting` | Meta 0 steel, 1 copper | `enable_crafting_items` and `enable_ingots` | [dropped-and-deferred G](dropped-and-deferred.md#g-crafting-items-dropped) |
| `icbmclassic:clump` | `ItemCrafting` | Meta 0 steel | same as ingot | same |
| `icbmclassic:plate` | `ItemCrafting` | Meta 0 steel, 1 iron | `enable_crafting_items` and `enable_plates` | same |
| `icbmclassic:circuit` | `ItemCrafting` | Meta 0 basic, 1 advanced, 2 elite | `enable_crafting_items` and `enable_circuits` | same |
| `icbmclassic:wire` | `ItemCrafting` | Meta 0 copper, 1 gold | `enable_crafting_items` and `enable_wires` | same |
| `icbmclassic:battery` | `content/items/ItemBattery` | Energy item, stack size 1 | `enable_battery` | [dropped-and-deferred F](dropped-and-deferred.md#f-battery-dropped) |

Total: 27 item registry names (not counting block items). The 5 crafting families and the battery are config-gated (6 names); the other 21 are always present.

### Block items

Each block below also registers an item of the same registry name.

| Item class | Used for | Notes |
|---|---|---|
| `ItemBlock` | glass pressure plate, glass button, reinforced glass, radar station, launcher frame, launcher base, launcher connector, launcher screen, cruise launcher | No metadata |
| `prefab/item/ItemBlockSubTypes` | spikes, concrete, radioactive | Metadata is the block type; unlocalized name looks up `.<meta>.name` |
| `content/blocks/explosive/ItemBlockExplosive` | explosive block | Metadata is the explosive ID |
| `content/blocks/emptower/ItemBlockEmpTower` | EMP tower | Meta 0 tower, 1 extender (subtype naming as above) |

## Blocks

Registered by `BlockReg.registerBlocks`, all unconditional. The `BlockICBM` prefab (`prefab/tile/BlockICBM`) lowercases the name it is given and puts the block in the ICBM tab.

| Registry name | Class | Meta / states | Tile entity | Documented in |
|---|---|---|---|---|
| `icbmclassic:glasspressureplate` | `content/blocks/BlockGlassPressurePlate` | `powered` | none | [dropped-and-deferred D](dropped-and-deferred.md#d-glass-pressure-plate-deferred) |
| `icbmclassic:glassbutton` | `BlockGlassButton` | `facing`, `powered` | none | [C](dropped-and-deferred.md#c-glass-button-deferred) |
| `icbmclassic:spikes` | `BlockSpikes` | meta 0 normal, 1 poison, 2 fire | none | [E](dropped-and-deferred.md#e-spikes-deferred) |
| `icbmclassic:concrete` | `BlockConcrete` | meta 0 normal, 1 compact, 2 reinforced | none | [A](dropped-and-deferred.md#a-concrete-dropped) |
| `icbmclassic:reinforcedglass` | `BlockReinforcedGlass` | none | none | [B](dropped-and-deferred.md#b-reinforced-glass-dropped) |
| `icbmclassic:explosives` | `content/blocks/explosive/BlockExplosive` | meta = explosive ID | `icbmclassic:explosive` | [02 explosive-block](../02-explosives/explosive-block.md) |
| `icbmclassic:emptower` | `content/blocks/emptower/BlockEmpTower` | meta 0 tower, 1 extender | `icbmclassic:emptower`, `icbmclassic:emptower_fake` | [05 emp-tower](../05-detection-defense/emp-tower.md) |
| `icbmclassic:radarstation` | `content/blocks/radarstation/BlockRadarStation` (source `radarStation`) | radar state | `icbmclassic:radarstation` | [05 radar-station](../05-detection-defense/radar-station.md) |
| `icbmclassic:launcherframe` | `content/blocks/launcher/frame/BlockLaunchFrame` | top / middle / bottom | `icbmclassic:launcherframe` | [04 launcher-frame](../04-launch-control/launcher-frame.md) |
| `icbmclassic:launcher_connector` | `content/blocks/launcher/connector/BlockLaunchConnector` | none | `icbmclassic:launcher_connector` | [04 launcher-connector-and-network](../04-launch-control/launcher-connector-and-network.md) |
| `icbmclassic:launcherbase` | `content/blocks/launcher/base/BlockLauncherBase` | rotation | `icbmclassic:launcherbase` | [04 launcher-base](../04-launch-control/launcher-base.md) |
| `icbmclassic:launcherscreen` | `content/blocks/launcher/screen/BlockLaunchScreen` | rotation | `icbmclassic:launcherscreen` | [04 launch-screen](../04-launch-control/launch-screen.md) |
| `icbmclassic:cruiselauncher` | `content/blocks/launcher/cruise/BlockCruiseLauncher` (source `cruiseLauncher`) | rotation | `icbmclassic:cruiselauncher` | [04 cruise-launcher](../04-launch-control/cruise-launcher.md) |
| `icbmclassic:radioactive` | `content/radioactive/BlockRadioactive` | meta 0 dirt, 1 stone | none | [02 radioactive-and-mutation](../02-explosives/radioactive-and-mutation.md) |

14 blocks.

### Tile entities

| Registry name | Class | Notes |
|---|---|---|
| `icbmclassic:explosive` | `content/blocks/explosive/TileEntityExplosive` | Registered in `BlockReg` |
| `icbmclassic:emptower` | `TileEMPTower` | Registered by its own `register()` |
| `icbmclassic:emptower_fake` | `TileEmpTowerFake` | Placeholder tile used for the extender |
| `icbmclassic:radarstation` | `TileRadarStation` | Own `register()` |
| `icbmclassic:launcherframe` | `TileLauncherFrame` | |
| `icbmclassic:launcher_connector` | `TileLauncherConnector` | |
| `icbmclassic:launcherbase` | `TileLauncherBase` | Own `register()` |
| `icbmclassic:launcherscreen` | `TileLauncherScreen` | Own `register()` |
| `icbmclassic:multiblock` | `content/blocks/multiblock/TileMulti` | `@Deprecated`; its `onLoad` is a TODO, no block creates it. Dead code |
| `icbmclassic:cruiselauncher` | `TileCruiseLauncher` | Own `register()`; the constant is spelled `cruiseLauncher` |

## Entities

Registered by `EntityReg.registerEntity` through one helper that sets the entry name to the full `icbmclassic:<name>` string, assigns numeric IDs 0 to 16 in this order (the counter is static; IDs are not persisted, names are), and enables velocity updates. Names are the constants in `api/refs/ICBMEntities`.

| ID | Registry name | Class | Track range / update interval | Documented in |
|---|---|---|---|---|
| 0 | `icbmclassic:block.gravity` | `content/entity/flyingblock/EntityFlyingBlock` | 128 / 1 | [02 effect-entities](../02-explosives/effect-entities.md) |
| 1 | `icbmclassic:block.fragment` | `content/entity/EntityFragments` | 40 / 1 | [02 effect-entities](../02-explosives/effect-entities.md) |
| 2 | `icbmclassic:block.explosive` | `content/entity/EntityExplosive` | 50 / 5 | [02 explosive-block](../02-explosives/explosive-block.md) |
| 3 | `icbmclassic:explosive_missile` | `content/missile/entity/explosive/EntityExplosiveMissile` | 500 / 1 | [03 missile-entity](../03-missiles/missile-entity.md) |
| 4 | `icbmclassic:generic_missile` | `content/missile/entity/explosive/EntityMissileActionable` | 500 / 1 | [03 missile-entity](../03-missiles/missile-entity.md) |
| 5 | `icbmclassic:surface_to_air_missile` | `content/missile/entity/anti/EntitySurfaceToAirMissile` | 500 / 1 | [03 missile-entity](../03-missiles/missile-entity.md) |
| 6 | `icbmclassic:held_item_missile` | `content/missile/entity/itemstack/EntityHeldItemMissile` | 500 / 1 | [03 missile-entity](../03-missiles/missile-entity.md) |
| 7 | `icbmclassic:holder.explosion` | `content/entity/EntityExplosion` | 100 / 5 | [02 effect-entities](../02-explosives/effect-entities.md) |
| 8 | `icbmclassic:beam.light` | `content/entity/EntityLightBeam` | 80 / 5 | [02 effect-entities](../02-explosives/effect-entities.md) |
| 9 | `icbmclassic:item.grenade` | `content/entity/EntityGrenade` | 50 / 5 | [03 thrown-and-handheld](../03-missiles/thrown-and-handheld.md) |
| 10 | `icbmclassic:cart.bomb` | `content/entity/EntityBombCart` | 50 / 2 | [03 bomb-cart](../03-missiles/bomb-cart.md) |
| 11 | `icbmclassic:holder.seat` | `content/entity/EntityPlayerSeat` | 50 / 2 | [03 cargo](../03-missiles/cargo.md) |
| 12 | `icbmclassic:redmatter` | `content/blast/redmatter/EntityRedmatter` | 500 / 1 | [02 red-matter](../02-explosives/red-matter.md) |
| 13 | `icbmclassic:smoke` | `content/entity/EntitySmoke` | 100 / 15 | [02 effect-entities](../02-explosives/effect-entities.md) |
| 14 | `icbmclassic:bomb_droplet` | `content/cluster/bomblet/EntityBombDroplet` | 500 / 1 | [03 cluster-missile](../03-missiles/cluster-missile.md) |
| 15 | `icbmclassic:parachute` | `content/cargo/parachute/EntityParachute` | 500 / 1 | [03 cargo](../03-missiles/cargo.md) |
| 16 | `icbmclassic:balloon` | `content/cargo/balloon/EntityBalloon` | 500 / 1 | [03 cargo](../03-missiles/cargo.md) |

17 entities. The Christmas mobs (`EntityXmas*` skeletons, zombies, creeper, snowman, rocket) are commented out in `EntityReg` and are not registered. Two entities (`EntityExplosion`, `EntityLightBeam`) are internal helpers, not things a player sees as such.

Entity entries have no English names in `en_us.lang` (no `entity.*` keys), so any UI that prints the entity name shows the raw key (ICBM-06-16).

## Remaps

| Registry | Old name | New name | Where |
|---|---|---|---|
| Items | `icbmclassic:missile` | `icbmclassic:explosive_missile` | `ItemReg.missingMapping` (renamed in v4.2.0) |
| Entities | `icbmclassic:missile` | `icbmclassic:explosive_missile` | `EntityReg.missingMapping` |

There is no block remap.

## Ore dictionary names registered by this area

| Name | Item | When |
|---|---|---|
| `ingotSteel`, `ingotCopper` | `icbmclassic:ingot` metas 0, 1 | `enable_ingots` |
| `clumpSteel` | `icbmclassic:clump` | `enable_ingots` |
| `plateSteel`, `plateIron` | `icbmclassic:plate` metas 0, 1 | `enable_plates` |
| `circuitBasic`, `circuitAdvanced`, `circuitElite` | `icbmclassic:circuit` metas 0 to 2 | `enable_circuits` |
| `wireCopper`, `wireGold` | `icbmclassic:wire` metas 0, 1 | `enable_wires` |
| `dustSulfur`, `dustSaltpeter` | sulfur dust, saltpeter | always (registered twice, ICBM-01-19) |

The crafting-item names register at highest priority in the recipe registration event, so every recipe condition and ore ingredient can see them (see ICBM-06-17 for the open timing question). Everything the recipes ask for that ICBM does not itself register (`ingotIron`, `ingotGold`, `paper`, `plankWood`, `stickWood`, `dirt`, `treeLeaves`, `treeSapling`, `string`, `dustCarbon`, `dustCharcoal`, `dustCoal`, `ingotUranium`, `pelletUranium235`) is a vanilla or other-mod name.

## Other registrations

| Kind | Name | Notes |
|---|---|---|
| Recipe type | `icbmclassic:explosive` | `lib/recipe/ExRecipeFactory`; declared in `recipes/_factories.json` |
| Ingredient type | `icbmclassic:explosive` | `lib/recipe/ExIngredientFactory` (same file) |
| Recipe condition | `icbmclassic:ore_value` | `lib/recipe/ConditionOreValue` (same file) |
| Code recipes | `balloon_cargo`, `balloon_cargo_decraft`, `parachute_cargo`, `parachute_cargo_decraft`, `cluster_missile`, `held_item_missile`, `held_item_missile_decraft` | `ICBMClassic.registerRecipes`; behavior in [03 cargo](../03-missiles/cargo.md), [cluster-missile](../03-missiles/cluster-missile.md), [missile-item](../03-missiles/missile-item.md) |
| Smelting | clump to ingot, iron plate to iron ingot, saltpeter starter to saltpeter | `ICBMClassic.registerRecipes` |
| Damage sources | `icbmclassic:spikes` (spikes); the others are in 02 | `BlockSpikes.DAMAGE_SOURCE` |

All of these registries and events are *1.12-only mechanisms*; the behavior they produce is described in the linked sheets.

## Lang leftovers with no registration

These `en_US.lang` keys describe things that no longer exist in 1.12. They are catalog entries for [07-visuals-audio](../README.md#areas) and the 1.7.10 lineage appendix (A2).

| Key | English | Note |
|---|---|---|
| `tile.icbmclassic:missilecoordinator.name` | Missile Coordinator | no block |
| `tile.icbmclassic:camouflage.name` | Camouflage | no block (appendix A2) |
| `tile.icbmclassic:proximityDetector.name` | Proximity Detector | no block (appendix A2) |
| `tile.icbmclassic:batterybox.0.name` | Battery Box | no block |

## Coverage

All of `ItemReg`, `BlockReg`, `EntityReg` and `ICBMEntities` were read in full. The class column for items and blocks owned by other areas comes from the registration lines and constructors; those classes are documented in their own areas. Item and block form details (stack size, tooltips) for other areas' things are not repeated here.
