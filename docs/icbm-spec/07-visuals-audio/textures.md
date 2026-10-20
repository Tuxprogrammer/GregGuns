# Texture catalog

Every PNG under `src/main/resources/assets/icbmclassic/textures/` (140 files). Sizes are read from the PNG header; no image was opened. "Used by" lists the model or blockstate JSON files that name the texture (`(bs)` marks a blockstate) or the Java class that binds it. License comes from the per-file table in `Credits.md`; `not listed` means the file has no row, which `Credits.md` defines as all rights reserved by the author. Catalog depth only (decision D3): palettes and layouts are written later, only for assets GregGuns keeps.

## Licensing summary

| License (per `Credits.md`) | Textures |
|---|---|
| Darkguardsman, ARR (licensed to BBM) | 75 |
| Morton, CC BY-NC-SA 4.0 | 55 |
| not listed | 7 |
| modified vanilla | 2 |
| CC0 | 1 |

The Morton set is **non-commercial and share-alike**, and the Darkguardsman set is all rights reserved but licensed to the ICBM team only. Neither can be copied into GregGuns, so treat every texture here as reference. See [index.md](index.md#key-facts).

## textures/blocks/ (15)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `antimatter.png` | 16 x 320 | yes: frametime 3, 20 frames in order | antimatter | Darkguardsman, ARR (licensed to BBM) |
| `concrete.png` | 16 x 16 | no | concrete, concrete(bs) | Morton, CC BY-NC-SA 4.0 |
| `concrete_compact.png` | 16 x 16 | no | concrete(bs), concrete_compact | Morton, CC BY-NC-SA 4.0 |
| `concrete_reinforced.png` | 16 x 16 | no | concrete(bs), concrete_reinforced | Morton, CC BY-NC-SA 4.0 |
| `cruise_launcher.png` | 32 x 32 | no | cruise_launcher | Darkguardsman, ARR (licensed to BBM) |
| `cryo_flow.png` | 32 x 512 | yes: frametime 3, 16 frames in order | endothermic | Darkguardsman, ARR (licensed to BBM) |
| `glass_button.png` | 32 x 32 | no | glass_button, glass_button_pressed | modified vanilla |
| `glass_pressure_plate.png` | 32 x 32 | no | glasspressureplate(bs) | modified vanilla |
| `glass_reinforced.png` | 16 x 16 | no | glass_reinforced | Darkguardsman, ARR (licensed to BBM) |
| `machine_green.png` | 16 x 16 | no | radar_screen, silo_screen | Darkguardsman, ARR (licensed to BBM) |
| `parachute.png` | 16 x 16 | no | nothing found (the parachute item uses `items/parachute`) | not listed |
| `redmatter_flow.png` | 16 x 320 | yes: frametime 3, 20 frames in order | redmatter | Darkguardsman, ARR (licensed to BBM) |
| `spikes.png` | 16 x 16 | no | spikes, spikes(bs) | Darkguardsman, ARR (licensed to BBM) |
| `spikes_fire.png` | 16 x 16 | no | spikes(bs), spikes_fire | Darkguardsman, ARR (licensed to BBM) |
| `spikes_poison.png` | 16 x 16 | no | spikes(bs), spikes_poison | Darkguardsman, ARR (licensed to BBM) |

## textures/blocks/emp_tower/ (3)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `base.png` | 16 x 16 | no | emp_tower, emp_tower_base | Darkguardsman, ARR (licensed to BBM) |
| `coil.png` | 16 x 16 | no | emp_tower, emp_tower_1, emp_tower_coil (+2 more) | Darkguardsman, ARR (licensed to BBM) |
| `electric.png` | 16 x 64 | yes: frametime 3, 4 frames in order | emp_tower_electric | Darkguardsman, ARR (licensed to BBM) |

## textures/blocks/explosive/ (31)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `anti_gravitational.png` | 16 x 16 | no | antigravitational(bs) | Morton, CC BY-NC-SA 4.0 |
| `antimatter.png` | 16 x 320 | yes: frametime 2, ping-pong order of 38 listed frames (20 in sheet) | antimatter(bs) | Morton, CC BY-NC-SA 4.0 |
| `anvil.png` | 16 x 16 | no | anvil(bs) | Morton, CC BY-NC-SA 4.0 |
| `attractive.png` | 16 x 16 | no | attractive(bs) | Morton, CC BY-NC-SA 4.0 |
| `bottom_1.png` | 16 x 16 | no | anvil(bs), attractive(bs), chemical(bs) (+7 more) | Morton, CC BY-NC-SA 4.0 |
| `bottom_2.png` | 16 x 16 | no | breaching(bs), contagious(bs), fragmentation(bs) (+2 more) | Morton, CC BY-NC-SA 4.0 |
| `bottom_3.png` | 16 x 16 | no | antigravitational(bs), emp(bs), ender(bs) (+3 more) | Morton, CC BY-NC-SA 4.0 |
| `breaching_side.png` | 16 x 16 | no | breaching(bs) | Morton, CC BY-NC-SA 4.0 |
| `breaching_top.png` | 16 x 16 | no | breaching(bs) | Morton, CC BY-NC-SA 4.0 |
| `chemical.png` | 16 x 16 | no | chemical(bs) | Morton, CC BY-NC-SA 4.0 |
| `colorful.png` | 16 x 16 | no | colors(bs) | Morton, CC BY-NC-SA 4.0 |
| `condensed_side.png` | 16 x 16 | no | condensed(bs) | Morton, CC BY-NC-SA 4.0 |
| `condensed_top.png` | 16 x 16 | no | condensed(bs) | Morton, CC BY-NC-SA 4.0 |
| `contagious.png` | 16 x 16 | no | contagious(bs) | Morton, CC BY-NC-SA 4.0 |
| `debilitation.png` | 16 x 16 | no | debilitation(bs) | Morton, CC BY-NC-SA 4.0 |
| `emp.png` | 16 x 16 | no | emp(bs) | Morton, CC BY-NC-SA 4.0 |
| `ender.png` | 16 x 16 | no | ender(bs) | Morton, CC BY-NC-SA 4.0 |
| `endothermic.png` | 16 x 16 | no | endothermic(bs) | Morton, CC BY-NC-SA 4.0 |
| `exothermic.png` | 16 x 16 | no | exothermic(bs) | Morton, CC BY-NC-SA 4.0 |
| `fragmentation.png` | 16 x 16 | no | fragmentation(bs) | Morton, CC BY-NC-SA 4.0 |
| `incendiary.png` | 16 x 16 | no | incendiary(bs) | Morton, CC BY-NC-SA 4.0 |
| `nuclear.png` | 16 x 16 | no | nuclear(bs) | Morton, CC BY-NC-SA 4.0 |
| `redmatter_bottom.png` | 16 x 16 | no | antimatter(bs), redmatter(bs) | Morton, CC BY-NC-SA 4.0 |
| `redmatter_side.png` | 16 x 16 | no | redmatter(bs) | Morton, CC BY-NC-SA 4.0 |
| `redmatter_top.png` | 16 x 320 | yes: frametime 2, ping-pong order of 38 listed frames (20 in sheet) | redmatter(bs) | Morton, CC BY-NC-SA 4.0 |
| `repulsive.png` | 16 x 16 | no | repulsive(bs) | Morton, CC BY-NC-SA 4.0 |
| `shrapnel_side.png` | 16 x 16 | no | shrapnel(bs) | Morton, CC BY-NC-SA 4.0 |
| `shrapnel_top.png` | 16 x 16 | no | nothing found (the shrapnel blockstate uses `shrapnel_side` on every face) | Morton, CC BY-NC-SA 4.0 |
| `smoke.png` | 16 x 16 | no | smoke(bs) | Morton, CC BY-NC-SA 4.0 |
| `sonic.png` | 16 x 16 | no | sonic(bs) | Morton, CC BY-NC-SA 4.0 |
| `thermobaric.png` | 16 x 16 | no | thermobaric(bs) | Morton, CC BY-NC-SA 4.0 |

## textures/blocks/radioactive/ (2)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `dirt/side.png` | 16 x 16 | no | radioactive_dirt | not listed |
| `stone/side.png` | 16 x 16 | no | radioactive_stone | not listed |

## textures/blocks/screen/ (5)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `radar.png` | 16 x 16 | no | radar_screen, radarstation(bs) | Darkguardsman, ARR (licensed to BBM) |
| `radar_danger.png` | 16 x 16 | no | radarstation(bs) | Darkguardsman, ARR (licensed to BBM) |
| `radar_off.png` | 16 x 16 | no | radarstation(bs) | Darkguardsman, ARR (licensed to BBM) |
| `radar_warning.png` | 16 x 16 | no | radarstation(bs) | Darkguardsman, ARR (licensed to BBM) |
| `silo.png` | 16 x 16 | no | silo_screen | Darkguardsman, ARR (licensed to BBM) |

## textures/blocks/silo_connector/ (2)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `connected.png` | 16 x 16 | no | launcher_connector(bs) | Darkguardsman, ARR (licensed to BBM) |
| `disconnected.png` | 16 x 16 | no | launcher_connector, launcher_connector(bs) | Darkguardsman, ARR (licensed to BBM) |

## textures/blocks/silo_frame/ (4)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `base_bottom.png` | 16 x 16 | no | frame_bottom | Darkguardsman, ARR (licensed to BBM) |
| `base_side.png` | 16 x 16 | no | frame_bottom | Darkguardsman, ARR (licensed to BBM) |
| `base_top.png` | 16 x 16 | no | frame_bottom | Darkguardsman, ARR (licensed to BBM) |
| `frame.png` | 16 x 16 | no | frame_bottom, frame_middle, frame_top | Darkguardsman, ARR (licensed to BBM) |

## textures/blocks/silo_pad/ (3)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `bottom.png` | 16 x 16 | no | silo_pad | Darkguardsman, ARR (licensed to BBM) |
| `side.png` | 16 x 16 | no | silo_pad | Darkguardsman, ARR (licensed to BBM) |
| `top.png` | 16 x 16 | no | silo_pad | Darkguardsman, ARR (licensed to BBM) |

## textures/entity/fragments/ (1)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `fragment.png` | 32 x 32 | no | `RenderFragments` (arrow-style quads), `RenderBombDroplet` (bind only) | Darkguardsman, ARR (licensed to BBM) |

## textures/entity/redmatter/ (1)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `blackhole.png` | 32 x 32 | no | `RenderRedmatter` (see 02-explosives/red-matter.md) | Darkguardsman, ARR (licensed to BBM) |

## textures/entity/ (1)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `space_chicken.png` | 64 x 32 | no | `LayerChickenHelmet` | Darkguardsman, ARR (licensed to BBM) |

## textures/gui/ (6)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `gui_components.png` | 256 x 256 | no | `GuiContainerBase` (shared widget atlas) | Darkguardsman, ARR (licensed to BBM) |
| `gui_cruise_launcher.png` | 256 x 256 | no | `GuiCruiseLauncher` | Darkguardsman, ARR (licensed to BBM) |
| `gui_emp_tower.png` | 256 x 256 | no | `GuiEMPTower` | Darkguardsman, ARR (licensed to BBM) |
| `gui_radar.png` | 256 x 256 | no | `GuiRadarStation` | Darkguardsman, ARR (licensed to BBM) |
| `gui_silo_base.png` | 256 x 256 | no | `GuiLauncherBase` | Darkguardsman, ARR (licensed to BBM) |
| `gui_silo_screen.png` | 256 x 256 | no | `GuiLauncherScreen` | Darkguardsman, ARR (licensed to BBM) |

## textures/items/ (25)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `antidote.png` | 16 x 16 | no | antidote | Morton, CC BY-NC-SA 4.0 |
| `balloon.png` | 16 x 16 | no | balloon, balloon(bs) | not listed |
| `balloon_craft.png` | 16 x 16 | no | balloon(bs) | not listed |
| `battery.png` | 16 x 16 | no | battery | Morton, CC BY-NC-SA 4.0 |
| `bomblet.png` | 16 x 16 | no | empty_bomblet, explosive_bomblet | not listed |
| `circuit_advanced.png` | 16 x 16 | no | circuit(bs) | Morton, CC BY-NC-SA 4.0 |
| `circuit_basic.png` | 16 x 16 | no | circuit(bs) | Morton, CC BY-NC-SA 4.0 |
| `circuit_elite.png` | 16 x 16 | no | circuit(bs) | Morton, CC BY-NC-SA 4.0 |
| `clump_steel.png` | 16 x 16 | no | clump(bs) | Morton, CC BY-NC-SA 4.0 |
| `defuser.png` | 16 x 16 | no | defuser | Morton, CC BY-NC-SA 4.0 |
| `ingot_copper.png` | 16 x 16 | no | ingot(bs) | Morton, CC BY-NC-SA 4.0 |
| `ingot_steel.png` | 16 x 16 | no | ingot(bs) | Morton, CC BY-NC-SA 4.0 |
| `laser_designator.png` | 16 x 16 | no | laserdetonator | Morton, CC BY-NC-SA 4.0 |
| `parachute.png` | 16 x 16 | no | parachute | Darkguardsman, ARR (licensed to BBM) |
| `plate_iron.png` | 16 x 16 | no | plate(bs) | Morton, CC BY-NC-SA 4.0 |
| `plate_steel.png` | 16 x 16 | no | plate(bs) | Morton, CC BY-NC-SA 4.0 |
| `poison_powder.png` | 16 x 16 | no | poisonpowder | Morton, CC BY-NC-SA 4.0 |
| `radar_gun.png` | 16 x 16 | no | radargun | Morton, CC BY-NC-SA 4.0 |
| `remote_detonator.png` | 16 x 16 | no | remotedetonator | Morton, CC BY-NC-SA 4.0 |
| `rpg.png` | 32 x 32 | no | ballisticlauncher, rocketlauncher | Morton, CC BY-NC-SA 4.0 |
| `saltpeter.png` | 16 x 16 | no | saltpeter | Morton, CC BY-NC-SA 4.0 |
| `saltpeter_ball.png` | 16 x 16 | no | saltpeter_ball | Morton, CC BY-NC-SA 4.0 |
| `sulfur_dust.png` | 16 x 16 | no | sulfurdust | Morton, CC BY-NC-SA 4.0 |
| `wire_copper.png` | 16 x 16 | no | wire(bs) | Morton, CC BY-NC-SA 4.0 |
| `wire_gold.png` | 16 x 16 | no | wire(bs) | Morton, CC BY-NC-SA 4.0 |

## textures/items/grenade/ (10)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `anvil.png` | 16 x 16 | no | anvil | Darkguardsman, ARR (licensed to BBM) |
| `attractive.png` | 16 x 16 | no | attractive | Darkguardsman, ARR (licensed to BBM) |
| `chemical.png` | 16 x 16 | no | chemical | Darkguardsman, ARR (licensed to BBM) |
| `color.png` | 16 x 16 | no | colors | Darkguardsman, ARR (licensed to BBM) |
| `condensed.png` | 16 x 16 | no | condensed, grenade | Darkguardsman, ARR (licensed to BBM) |
| `debilitation.png` | 16 x 16 | no | debilitation | Darkguardsman, ARR (licensed to BBM) |
| `incendiary.png` | 16 x 16 | no | incendiary | Darkguardsman, ARR (licensed to BBM) |
| `repulsive.png` | 16 x 16 | no | repulsive | Darkguardsman, ARR (licensed to BBM) |
| `shrapnel.png` | 16 x 16 | no | shrapnel | Darkguardsman, ARR (licensed to BBM) |
| `smoke.png` | 16 x 16 | no | smoke | Darkguardsman, ARR (licensed to BBM) |

## textures/items/missiles/ (29)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `antigravitational.png` | 32 x 32 | no | antigravitational | Darkguardsman, ARR (licensed to BBM) |
| `antimatter.png` | 32 x 32 | no | antimatter | Darkguardsman, ARR (licensed to BBM) |
| `attractive.png` | 32 x 32 | no | attractive | Darkguardsman, ARR (licensed to BBM) |
| `breaching.png` | 32 x 32 | no | breaching | Darkguardsman, ARR (licensed to BBM) |
| `chemical.png` | 32 x 32 | no | chemical | Darkguardsman, ARR (licensed to BBM) |
| `cluster.png` | 32 x 32 | no | cluster | not listed |
| `colorful.png` | 32 x 32 | no | colors | Darkguardsman, ARR (licensed to BBM) |
| `condensed.png` | 32 x 32 | no | condensed | Darkguardsman, ARR (licensed to BBM) |
| `contagious.png` | 32 x 32 | no | contagious | Darkguardsman, ARR (licensed to BBM) |
| `debilitation.png` | 32 x 32 | no | debilitation | Darkguardsman, ARR (licensed to BBM) |
| `emp.png` | 32 x 32 | no | emp | Darkguardsman, ARR (licensed to BBM) |
| `ender.png` | 32 x 32 | no | ender | Darkguardsman, ARR (licensed to BBM) |
| `endothermic.png` | 32 x 32 | no | endothermic | Darkguardsman, ARR (licensed to BBM) |
| `exothermic.png` | 32 x 32 | no | exothermic | Darkguardsman, ARR (licensed to BBM) |
| `fragmentation.png` | 32 x 32 | no | fragmentation | Darkguardsman, ARR (licensed to BBM) |
| `incendiary.png` | 32 x 32 | no | incendiary | Darkguardsman, ARR (licensed to BBM) |
| `missile_module.png` | 16 x 16 | no | missile | Darkguardsman, ARR (licensed to BBM) |
| `nuclear.png` | 32 x 32 | no | nuclear | Darkguardsman, ARR (licensed to BBM) |
| `redmatter_body.png` | 32 x 32 | no | redmatter | Darkguardsman, ARR (licensed to BBM) |
| `redmatter_top.png` | 32 x 32 | no | nothing found (the red matter missile model uses `redmatter_body` and `redmatter_flow`) | Darkguardsman, ARR (licensed to BBM) |
| `replusive.png` | 32 x 32 | no | repulsive | Darkguardsman, ARR (licensed to BBM) |
| `shrapnel.png` | 32 x 32 | no | shrapnel | Darkguardsman, ARR (licensed to BBM) |
| `smoke.png` | 16 x 16 | no | held_item, smoke | Darkguardsman, ARR (licensed to BBM) |
| `sonic.png` | 32 x 32 | no | sonic | Darkguardsman, ARR (licensed to BBM) |
| `surface_to_air.png` | 32 x 32 | no | surface_to_air | Darkguardsman, ARR (licensed to BBM) |
| `thermobaric.png` | 32 x 32 | no | thermobaric | Darkguardsman, ARR (licensed to BBM) |
| `tier1.png` | 32 x 32 | no | anvil, attractive, chemical (+10 more) | Darkguardsman, ARR (licensed to BBM) |
| `tier2.png` | 32 x 32 | no | breaching, contagious, fragmentation (+2 more) | Darkguardsman, ARR (licensed to BBM) |
| `tier3.png` | 32 x 32 | no | antigravitational, emp, ender (+3 more) | Darkguardsman, ARR (licensed to BBM) |

## textures/models/ (2)

| Path | Pixel size | Animated | Used by | License |
|---|---|---|---|---|
| `cruise_launcher_top.png` | 64 x 64 | no | `TESRCruiseLauncher` with `CruiseLauncherTopModel` | Darkguardsman, ARR (licensed to BBM) |
| `grey.png` | 512 x 512 | no | `RenderRedmatter`, `TESRCruiseLauncher` debug spheres, `RenderExplosion` (draws nothing) | CC0 |

## Observations

- Animated sheets: `blocks/antimatter` and `blocks/redmatter_flow` (16 x 320, 20 frames), `blocks/cryo_flow` (32 x 512, 16 frames) and `blocks/emp_tower/electric` (16 x 64, 4 frames) play frames in order at frametime 3. The two explosive-block sheets (`explosive/antimatter`, `explosive/redmatter_top`) list frames 0 to 19 and back down to 1, a ping-pong, at frametime 2.
- The exothermic missile model takes its glow from vanilla `blocks/lava_flow`, not from a mod texture.
- Missile item textures are 32 x 32 except `missile_module` and `smoke`, which are 16 x 16 (the smoke and held-item missile models reuse `smoke.png`).
- `items/missiles/replusive.png` is a misspelling of "repulsive". The repulsive missile model references the misspelled name, so renaming the file breaks it.
- `models/grey.png` (512 x 512) is the only large file. It is a flat color source used by the red matter sphere and by debug spheres.
- The legacy `blockstates/explosives.json` names four textures that do not exist (`blocks/explosive_base_1`, `explosive_bottom_1`, `explosive_condensed_top`, `explosive_condensed_side`). The per-explosive files in `blockstates/explosives/` replace it (ICBM-07-12).
- `blockstates/clump.json` names `items/clump_bronze`, which does not exist (only a steel clump is registered). The `ingot.json` and `wire.json` defaults point at an un-namespaced `items/iron_ingot`, which is not a valid 1.12 path; the defaults are never reached because every registered sub-item has an explicit variant.
