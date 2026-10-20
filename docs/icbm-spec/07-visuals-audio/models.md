# Model and blockstate catalog

*1.12-only mechanism*: blockstate JSON, JSON models and the model registry. 1.7.10 has none of them (it uses `ISimpleBlockRenderingHandler`, `IItemRenderer` and flat icons), so a GregGuns rebuild re-expresses each entry as a renderer or an icon. This sheet records which assets exist, their shape and who loads them. Hand-modelled files carry a Blockbench `credit` and a `display` transform table; `Credits.md` lists the authors (Darkguardsman, ARR licensed to BBM, for every model with elements).

## models/block/ (22)

| File | Parent or type | Elements | Loaded by (blockstate) |
|---|---|---|---|
| `concrete` | block/cube_all | 0 | concrete |
| `cruise_launcher` | hand-built, 24 cuboids | 24 | cruiselauncher |
| `emp_tower` | hand-built, 24 cuboids | 24 | emptower |
| `emp_tower_1` | hand-built, 24 cuboids | 24 | emptower |
| `emp_tower_base` | hand-built, 1 cuboids | 1 | emptower |
| `emp_tower_coil` | hand-built, 23 cuboids | 23 | emptower |
| `emp_tower_electric` | hand-built, 1 cuboids | 1 | emptower |
| `emp_tower_spin` | hand-built, 1 cuboids | 1 | emptower |
| `frame_bottom` | hand-built, 10 cuboids | 10 | launcherframe |
| `frame_middle` | hand-built, 9 cuboids | 9 | launcherframe |
| `frame_top` | hand-built, 9 cuboids | 9 | launcherframe |
| `glass_button` | block/button | 0 | glassbutton |
| `glass_button_inventory` | block/button_inventory | 0 | (item model parent or unused) |
| `glass_button_pressed` | block/button_pressed | 0 | glassbutton |
| `glass_reinforced` | block/cube_all | 0 | reinforcedglass |
| `launcher_connector` | block/cube | 0 | launcher_connector |
| `radar_screen` | block/cube | 0 | radarstation |
| `radioactive_dirt` | block/cube_all | 0 | radioactive |
| `radioactive_stone` | block/cube_all | 0 | radioactive |
| `silo_pad` | block/cube_bottom_top | 0 | launcherbase |
| `silo_screen` | block/cube | 0 | launcherscreen |
| `spikes` | block/tinted_cross | 0 | spikes |

Notes: the EMP tower uses six models. The block has two metadata values, 0 (tower) and 1 (extender, blockstate property value `spin`). A placed tower renders `emp_tower_base` through the blockstate; its rotating coil (`emp_tower_coil`) and glowing arcs (`emp_tower_electric`, translucent layer) are drawn on top by the tile renderer (see [renderers.md](renderers.md#tesremptower)). A placed extender renders `emp_tower_spin` and its tile renderer draws a coil of its own. `emp_tower` and `emp_tower_1` are the complete tower and extender used only for the inventory item (`inventory_0`, `inventory_1`). `block/item/generated.json` is a workaround file whose only content is a comment and a parent of `minecraft:item/generated`, so blockstates can borrow the flat-item shape.

## models/item/

### Single items

| File | Parent or type | Texture(s) | Used by |
|---|---|---|---|
| `antidote` | item/generated | items/antidote | ItemReg antidote |
| `ballisticlauncher` | hand-built, 9 cuboids | items/rpg | handheld ballistic launcher |
| `balloon` | hand-built, 3 cuboids | items/balloon | 3D balloon (blockstate `balloon` variant `render=3d`, drawn by the entity renderer) |
| `battery` | item/generated | items/battery | battery |
| `concrete` | icbmclassic:block/concrete | (inherited) | concrete item (meta 0) |
| `concrete_compact` | icbmclassic:block/concrete | blocks/concrete_compact | concrete item (meta 1) |
| `concrete_reinforced` | icbmclassic:block/concrete | blocks/concrete_reinforced | concrete item (meta 2) |
| `defuser` | item/generated | items/defuser | defuser |
| `empty_bomblet` | hand-built, 8 cuboids | items/bomblet | empty bomblet item (droplet) |
| `explosive_bomblet` | hand-built, 8 cuboids | items/bomblet | explosive bomblet item (every explosive maps here) |
| `glassbutton` | icbmclassic:block/glass_button_inventory | (inherited) | glass button item |
| `laserdetonator` | item/generated | items/laser_designator | laser designator item |
| `parachute` | item/generated | items/parachute | parachute item |
| `poisonpowder` | item/generated | items/poison_powder | poison powder |
| `radargun` | item/generated | items/radar_gun | radar gun |
| `radioactive_dirt` | icbmclassic:block/radioactive_dirt | (inherited) | radioactive dirt item (meta 0) |
| `radioactive_stone` | icbmclassic:block/radioactive_stone | (inherited) | radioactive stone item (meta 1) |
| `reinforcedglass` | icbmclassic:block/glass_reinforced | (inherited) | reinforced glass item |
| `remotedetonator` | item/generated | items/remote_detonator | remote detonator |
| `rocketlauncher` | hand-built, 9 cuboids | items/rpg | handheld direct launcher |
| `saltpeter` | item/generated | items/saltpeter | saltpeter dust |
| `saltpeter_ball` | item/generated | items/saltpeter_ball | saltpeter starter ball |
| `spikes` | item/generated | blocks/spikes | spikes item (meta 0) |
| `spikes_fire` | item/generated | blocks/spikes_fire | spikes item (meta 2) |
| `spikes_poison` | item/generated | blocks/spikes_poison | spikes item (meta 1) |
| `sulfurdust` | item/generated | items/sulfur_dust | sulfur dust |

### models/item/bombcarts/ (24, one row)

| Count | Parent | Texture | Used by |
|---|---|---|---|
| 24 | `item/generated` | vanilla `items/minecart_tnt` for all | `ItemBombCart` via `ItemModelMapperExplosive` (one file per explosive registry key: antigravitational, antimatter, anvil, attractive, breaching, chemical, colors, condensed, contagious, debilitation, emp, ender, endothermic, exothermic, fragmentation, incendiary, nuclear, redmatter, rejuvenation, repulsive, shrapnel, smoke, sonic, thermobaric) |

Every bomb cart item looks like the vanilla TNT minecart icon. `rejuvenation` exists as a file but has no matching explosive in the 1.12 registry (it is a leftover from 1.7.10, see appendix A2-lineage).

### models/item/grenades/ (11)

| Count | Parent | Textures | Used by |
|---|---|---|---|
| 11 | `grenades/grenade` (which extends `item/generated` with a `display` table) | `items/grenade/<name>` (`colors` uses `color.png`) | `ItemGrenade`; files: anvil, attractive, chemical, colors, condensed, debilitation, grenade, incendiary, repulsive, shrapnel, smoke |

### models/item/missiles/ (27)

Hand-built Blockbench models, one per explosive type plus four specials (`missile` the module and fallback, `cluster`, `held_item`, `surface_to_air`). The **tier** of a missile is expressed only by which body texture the model uses: texture slot `0` is `tier1`, `tier2` or `tier3` (32 x 32 colored hulls), and the second slot is the warhead texture. No Java code picks a tier model. `ItemModelMapperExplosive` picks the file by registry key; a type with no file falls back to `missile.json` (the module).

| Model | Cuboids | Body texture (slot 0) | Warhead texture | Extra animated or vanilla texture |
|---|---|---|---|---|
| `antigravitational` | 32 | tier3 | antigravitational |  |
| `antimatter` | 27 | antimatter |  | blocks/antimatter |
| `anvil` | 19 | tier1 |  | blocks/anvil_base |
| `attractive` | 22 | tier1 | attractive |  |
| `breaching` | 21 | tier2 | breaching |  |
| `chemical` | 21 | tier1 | chemical |  |
| `cluster` | 42 | cluster |  |  |
| `colors` | 21 | tier1 | colorful |  |
| `condensed` | 17 | tier1 | condensed |  |
| `contagious` | 24 | tier2 | contagious |  |
| `debilitation` | 19 | tier1 | debilitation |  |
| `emp` | 37 | tier3 | emp |  |
| `ender` | 28 | tier3 | ender |  |
| `endothermic` | 46 | tier3 | endothermic | blocks/cryo_flow |
| `exothermic` | 31 | tier3 | exothermic | blocks/lava_flow |
| `fragmentation` | 29 | tier2 | fragmentation |  |
| `held_item` | 17 | tier1 | smoke |  |
| `incendiary` | 17 | tier1 | incendiary |  |
| `missile` | 16 | tier1 | missile_module |  |
| `nuclear` | 32 | tier3 | nuclear |  |
| `redmatter` | 38 | redmatter_body |  | blocks/redmatter_flow |
| `repulsive` | 22 | tier1 | replusive |  |
| `shrapnel` | 25 | tier1 | shrapnel |  |
| `smoke` | 17 | tier1 | smoke |  |
| `sonic` | 29 | tier2 | sonic |  |
| `surface_to_air` | 26 | tier1 | surface_to_air |  |
| `thermobaric` | 29 | tier2 | thermobaric |  |

Tier groups by body texture: tier 1 for condensed, shrapnel, incendiary, debilitation, chemical, anvil, repulsive, attractive, colors, smoke, SAM, held-item and the module; tier 2 for fragmentation, contagious, sonic, breaching, thermobaric; tier 3 for nuclear, EMP, exothermic, endothermic, anti-gravitational, ender; antimatter, red matter and cluster use their own body texture. The red matter and antimatter models also use animated block textures (`redmatter_flow`, `antimatter`), the endothermic model uses `cryo_flow`.

## blockstates/

All use the Forge blockstate format (`forge_marker: 1`, `defaults`, per-property `variants`) unless noted.

| File | Style | Model(s) | Used by |
|---|---|---|---|
| `balloon.json` | variants `render=2d/3d` | `item/generated` (2d, `balloon_craft`), `item/balloon` (3d) | balloon item (`ClientReg` registers meta 0 as 2d and a fake meta 1 as 3d for the entity renderer) |
| `circuit.json` | variants `name=basic/advanced/elite` | `item/generated` + `circuit_*` texture | `ItemCrafting` circuit |
| `clump.json` | variants `name=steel/bronze` | `item/generated` + `clump_*` | `ItemCrafting` clump (`bronze` is never registered, texture missing) |
| `concrete.json` | variants `type=normal/compact/reinforced` | `block/concrete` with three textures | `BlockConcrete` |
| `cruiselauncher.json` | `rotation` y-rotations | `block/cruise_launcher` (hand-built, 24 cuboids) | `BlockCruiseLauncher`; the moving top is a TESR |
| `emptower.json` | variants `type=base/electric/coil/spin`, plus `inventory_0/1` | `emp_tower`, `emp_tower_1`, `emp_tower_base`, `emp_tower_electric`, `emp_tower_coil`, `emp_tower_spin` | `BlockEmpTower` |
| `explosives.json` | legacy, one `explosive=condensed` variant | `cube_all` with textures that do not exist | unused (superseded by `blockstates/explosives/*`) |
| `glassbutton.json` | plain `facing`/`powered` variants with uvlock and x/y rotations | `block/glass_button`, `glass_button_pressed` | `BlockGlassButton` |
| `glasspressureplate.json` | forge, `powered` variants | vanilla `pressure_plate_up/down` with a glass texture | `BlockGlassPressurePlate` |
| `ingot.json` | variants `name=steel/copper` | `item/generated` + `ingot_*` | `ItemCrafting` ingot |
| `launcher_connector.json` | per-face booleans `up/down/north/...` each swapping one face texture | `block/launcher_connector` (`cube`) | `BlockLaunchConnector` (connected vs disconnected face) |
| `launcherbase.json` | `facing` rotations | `silo_pad` (`cube_bottom_top`) | `BlockLauncherBase` |
| `launcherframe.json` | variants `type=top/middle/bottom` plus `rotation` | `frame_top`, `frame_middle`, `frame_bottom` | `BlockLaunchFrame` |
| `launcherscreen.json` | `rotation` y-rotations | `silo_screen` (`cube`, front face is the screen) | `BlockLaunchScreen` |
| `plate.json` | variants `name=steel/iron` | `item/generated` + `plate_*` | `ItemCrafting` plate |
| `radarstation.json` | variants `type=off/on/warning/danger`, each swapping the north-face texture | `radar_screen` (`cube`) | `BlockRadarStation` |
| `radioactive.json` | variants `type=dirt/stone` | `radioactive_dirt`, `radioactive_stone` | `BlockRadioactive` |
| `reinforcedglass.json` | one `normal` variant | `glass_reinforced` | `BlockReinforcedGlass` |
| `spikes.json` | variants `type=normal/fire/poison` | `block/spikes` (`tinted_cross`) | `BlockSpikes` |
| `wire.json` | variants `name=gold/copper` | `item/generated` + `wire_*` | `ItemCrafting` wire |
| `explosives/*.json` (23) | one file per explosive: `defaults` cube_all textures (side, up, down, particle), `variants.explosive.<domain>_<name>` and six `rotation` entries (`up` none, `down` x180, `north` x90, `south` x270, `east` x90 y90, `west` x90 y270) | `cube_all` built from the six textures in `defaults` | `BlockExplosive` through `BlockModelMapperExplosive`; also the item inventory variant for `ItemBlockExplosive` |

Explosive blockstate files: antigravitational, antimatter, anvil, attractive, breaching, chemical, colors, condensed, contagious, debilitation, emp, ender, endothermic, exothermic, fragmentation, incendiary, nuclear, redmatter, repulsive, shrapnel, smoke, sonic, thermobaric.

Observations:

- The explosive block never uses a hand-built model. Each explosive is a `cube_all` model whose six faces are retextured in its own blockstate, so a rebuild needs only a table of (top, bottom, side) textures per explosive, not 23 model files.
- Not every registered explosive has a blockstate file or texture: `rejuvenation` and `hypersonic`/`nightmare` appear only in lang and datafixers. See [lang.md](lang.md#stale-keys).
- The `explosives.json` file at the root of `blockstates/` is a leftover. The registered block name is `explosives` so Forge still loads it for the plain block, but `BlockModelMapperExplosive` overrides every state to the per-explosive model location (and falls back to the `condensed` model, not to this file).
