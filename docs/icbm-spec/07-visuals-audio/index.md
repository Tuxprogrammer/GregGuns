# 07-visuals-audio

## Overview

Everything ICBM-Classic shows and plays: the client renderers (entities, tile entities, the missile item model, particles, one render layer), the JSON models and blockstates, all 140 textures, the 16 sound events and 24 OGG files, and the English language file. Assets are at **catalog depth** (decision D3): one line each with size or duration and what uses it. Full asset entries (palette, layout, envelope) get written later, only for the assets GregGuns keeps. Source: ICBM-Classic `prod/1.12` @ `9c1d985a`; Java is cited relative to `src/main/java/icbm/classic/`, resources relative to `src/main/resources/assets/icbmclassic/`.

Red matter's renderer is specified in [02-explosives/red-matter.md](../02-explosives/red-matter.md) and is only linked here.

## Contents

- [renderers.md](renderers.md): every renderer, model, particle and mapper class: what it draws, its transforms, and its animations (fuse swell and flash, beam growth, coil spin, missile pose, held-item mount).
- [textures.md](textures.md): all 140 PNGs with pixel size, animation, user and license.
- [models.md](models.md): all item and block models and blockstates, grouped, plus the missile model table (the only place "tier" is expressed).
- [sounds.md](sounds.md): the 16 events, 24 files, durations, subtitle keys, triggers, vanilla sounds in use, and provenance.
- [lang.md](lang.md): all 425 English keys by group, plus missing, broken and stale keys.

## Key facts

- **The radar station has no dish and no animation.** There is no renderer for it. It is a static cube whose front face swaps between four 16 x 16 screen textures (off, on, warning, danger). The only moving machine part in ICBM is the EMP tower coil, and the only other tile renderers are the launcher pad (draws the stored missile) and the cruise launcher (a yaw and pitch top plus its missile).
- **Missile appearance is data, not code.** `RenderMissile` renders an item model. Which model is chosen by the explosive registry key (with a generic module model as fallback), and the *tier look* is only a 32 x 32 hull texture (`tier1`, `tier2`, `tier3`) baked into each missile's JSON. A rebuild needs a table of (explosive, hull color, warhead texture), not 27 hand-built models. Each is 16 to 46 cuboids.
- **Explosive blocks, grenades, carts and bomblets are cheap.** Explosive blocks are plain textured cubes (top, bottom, side per explosive). Grenades are flat icons. Carts all use the vanilla TNT minecart icon. All bomblets share one model.
- **The held-item missile draws two models.** The missile, then the carried item at a fixed offset on it, with a third-person-hand transform and half scale for non-tool items.
- **Licensing blocks reuse of the art.** Per `Credits.md`: 55 textures are CC BY-NC-SA 4.0 (author Morton, non-commercial, share-alike), 75 are all rights reserved by Darkguardsman and licensed to the ICBM team only, 1 is CC0, 2 are modified vanilla; 7 have no row. Every model with cuboids is Darkguardsman's. Of the 24 sounds, 21 are "unknown, marked for removal/replacement". Only `meep` (CC BY 4.0), `missileinair_short` (CC BY 3.0) and `radioactive_tick` (CC0) have a known license. GregGuns can not take any of it as is; this catalog is reference and a list of what must be recreated.
- **Sounds are all `BLOCKS` category** except `meep` (NEUTRAL). Two sounds fire constantly: the missile engine sound plays a new 2.4-second instance every flight tick, and the red matter blast plays a 1-second sound every tick (ICBM-07-17).
- **Animated textures:** four plain strips (antimatter and red matter missile glows, cryo, EMP arcs) and two ping-pong sheets (explosive block faces of antimatter and red matter).
- **The language file has real holes.** At least 11 translation keys the code asks for do not exist, and about 45 keys in the file are never asked for (dropped 1.7.10 features). See [lang.md](lang.md).
- Blockstates, JSON models, `ModelRegistryEvent`, mesh definitions, state mappers, sound registry events and the `.lang` key scheme are *1.12-only mechanisms*. The sheets describe the behavior they produce.

## Related areas

| Area | Why |
|---|---|
| `01-core` | `ClientProxy` and `CommonProxy` split, `PacketSpawnAirParticle` and `PacketSpawnBlockExplosion` (the packets that call the particle methods), config flags `MISSILE_ENGINE_SMOKE` and `MISSILE_LAUNCH_SMOKE`, lang keys of commands |
| `02-explosives` | `RenderRedmatter` (red matter), the blasts that play the sounds, flying blocks, fragments, explosive block |
| `03-missiles` | `EntityMissile`, held-item missile, flight logic that gates engine effects, bomblet, parachute and balloon entities |
| `04-launch-control` | Launcher pad and cruise launcher tiles that the TESRs read; launcher lang keys |
| `05-detection-defense` | EMP tower rotation and charge fields, radar state textures, radar and EMP GUI textures |
| `06-blocks-items` | Block and item classes that own the models, recipes (not catalogued here) |

## Coverage

Java read in full, 26 of 26 files under `client/` (`ClientProxy`, `ClientReg`, `ICBMSounds`, `ICBMCreativeTab`, `SpikeMeshDefinition`, `fx/` 5, `mapper/` 2, `models/` 3, `render/entity/` 11), plus the six renderers outside it that `ClientReg` registers (`TESREmpTower`, `TESRLauncherBase`, `TESRCruiseLauncher`, `RenderParachute`, `RenderBombDroplet`, `RenderEntityBlock`). `RenderRedmatter` is out of scope (02-explosives).

Resources catalogued: 140 PNGs (header read), 6 `.mcmeta`, 23 + 1 block models, 88 item models, 20 blockstates plus 23 explosive blockstates, `sounds.json` and 24 OGGs (header and last page read), `lang/en_US.lang` (425 keys; the other six language files only counted), and `Credits.md` (read for licenses).

Not covered: `recipes/` (belongs to the areas that own the items, area 06 and others), the contents of the other language files, texture pixels and palettes, the Blockbench cuboid layouts of individual missile models, the GUI layouts that use the GUI textures (in each machine's area), `RenderRedmatter`.

## Known limits

- No code was run and no image or audio was opened. Sizes, frame counts and durations come from file headers. Durations are from the last Ogg page's sample position and can differ from a decoder by a few milliseconds.
- "Stale key" findings come from a text search of the Java source. Keys built at runtime from registry names were not checked individually, so they are not flagged either way.
- Several rendering transforms (missile yaw/pitch, cruise launcher flip) are described from the code. Whether the pose is visually right depends on the model files, which were not viewed.
- The sign convention of the engine smoke offset in `ClientProxy.spawnMissileSmoke` was not verified against the missile's yaw/pitch convention, so no bug is filed for it.

## Suspected bugs

IDs are `ICBM-07-nn`. "logic" means a GregGuns rebuild could inherit it; "1.12-only" means it comes from the 1.12 asset system.

| ID | Severity | Kind | Where | Problem |
|---|---|---|---|---|
| ICBM-07-01 | visual | logic | `client/ClientProxy.java:122` | Engine smoke is meant to jitter each puff by scaling the position by a random 0.975 to 1; the call discards its result (positions are immutable), so all 10 puffs per tick start at one point. |
| ICBM-07-02 | cosmetic | 1.12-only | `client/ClientReg.java:68-75,255-266` | The model cache clear skips the bomblet map, and the bomblet loop walks the grenade registry instead of its own. Harmless today (one shared model) but a model reload keeps stale entries. |
| ICBM-07-03 | cosmetic | 1.12-only | `client/SpikeMeshDefinition.java:139-155` | Dead class: `init()` is never called (spikes models are bound in `ClientReg`), and it registers the base variant twice. |
| ICBM-07-04 | cosmetic | logic | `client/fx/FXAntimatterParticle.java`, `ClientProxy.spawnSmoke`, `ParticleSmokeICBM`, `ParticleLauncherSmoke.java:19` | Dead code: no caller creates the antimatter particle or the smoke particle; the "blocks to ignore" collision set is public but never filled. |
| ICBM-07-05 | visual | logic | `client/render/entity/RenderFragments.java:41-44` | Anvil fragments pick damage state and facing from the world RNG on every frame, so the anvil flickers while it flies. |
| ICBM-07-06 | visual | logic | `RenderFragments.java:43` | The facing index is `nextInt(3)` over four horizontal facings, so the fourth is never chosen. |
| ICBM-07-07 | visual | logic | `client/render/entity/RenderLightBeam.java:35-44` | Client beam progress grows by `rate * partialTicks` once per frame, so the extend and retract speed depends on frame rate. It also mutates entity state inside the renderer. |
| ICBM-07-08 | visual | logic | `RenderLightBeam.java:33,49` | The beam is always drawn from 5 blocks below the entity, with height `255 - floor(y)`; the TODO to find the ground is open, so a beam over a cliff or a deep pit starts in the air or inside rock. |
| ICBM-07-09 | cosmetic | logic | `client/render/entity/RenderMissile.java:141` | Debug motion line uses alpha 2555 (typo for 255) and leaves texturing and lighting state to be repaired afterward. |
| ICBM-07-10 | cosmetic | logic | `client/ClientReg.java:84-85,88-89` | The EMP tower tint uses the 0 to 1 charge fraction as radians in `cos`/`sin`, so the tint range is limited (red 255 to 138, blue 0 to 214) instead of using the full range. |
| ICBM-07-11 | visual | logic | `content/blocks/launcher/cruise/TESRCruiseLauncher.java:31-40` | The aim angles are read without interpolation (partial ticks unused for the model), so the cruise launcher top moves at server tick rate. Suspected. |
| ICBM-07-12 | cosmetic | 1.12-only | `blockstates/explosives.json` | Legacy blockstate names four textures that do not exist; it is shadowed by the per-explosive files. |
| ICBM-07-13 | cosmetic | 1.12-only | `blockstates/clump.json`, `ingot.json`, `wire.json` | A `bronze` clump variant points at a missing texture, and defaults use an un-namespaced `items/iron_ingot`. Never reached for registered sub-items. |
| ICBM-07-14 | cosmetic | 1.12-only | `models/item/bombcarts/rejuvenation.json` | Model for an explosive that is not registered in 1.12 (leftover from 1.7.10). |
| ICBM-07-15 | cosmetic | 1.12-only | `textures/items/missiles/replusive.png` | Misspelled file name that the repulsive missile model depends on. |
| ICBM-07-16 | cosmetic | logic | `content/blast/redmatter/logic/RedmatterLogic.java:118` | Red matter pitch is random in [-0.8, 1.2]; the engine clamps to [0.5, 2], so about 65% of plays hit the floor (~0.5). |
| ICBM-07-17 | gameplay | logic | `content/missile/entity/EntityMissile.java:124` | The 2.4-second engine sound is started every flight tick, so each missile layers about 48 overlapping instances. With a few missiles in the air the client's sound channel limit is exhausted and other sounds drop. |
| ICBM-07-18 | cosmetic | logic | `client/ICBMSounds.java:102-105` | The `IPos3D` overload ignores `distanceDelay` and never branches on `isRemote`. |
| ICBM-07-19 | cosmetic | logic | `content/blocks/launcher/LauncherLangs.java:17` | `ERROR_NO_LAUNCHER` is `ERROR + "network.launchers"` without a dot, so the message shows as a raw key. |
| ICBM-07-20 | cosmetic | logic | `LauncherLangs.java:20,23,25` | Keys `missile.invalid`, `missile.queued` and `group.empty` have no lang entry. |
| ICBM-07-21 | cosmetic | logic | `command/sub/blast/CommandBlastTrigger.java:30` | Requests `icbm.blast.threading`; the file defines `icbm.blast.threaded`. |
| ICBM-07-22 | cosmetic | logic | `content/blocks/emptower/gui/GuiEMPTower.java:26` | Tooltip key `gui.icbmclassic:empTower.range` has no lang entry. |
| ICBM-07-23 | cosmetic | logic | `content/cluster/bomblet/EntityBombDroplet.java:111-113` | Entity name keys use the prefix `bomb.droplet.`; the lang file defines `bomblet.`. |
| ICBM-07-24 | cosmetic | logic | `content/blocks/explosive/ItemBlockExplosive.java:25,145` | Red matter tooltip taunts are looked up as `icbm.explosive.redMatter.info.*`; the lang file defines `explosive.icbmclassic:redMatter.info.*`, so the 11 taunts never show. |
| ICBM-07-25 | cosmetic | logic | `content/reg/ExplosiveInit.java:346,350`, `lib/capability/gps/GPSDataHelpers.java:35` | `chat.launcher.toolTargetSet` and `noTargetInTool` are not in the lang file. |
| ICBM-07-26 | cosmetic | logic | `lang/en_US.lang` key `info.icbmclassic:gps.set.all` | Uses `%3$s` twice, so the world name is never shown (the Z value is shown instead). |
| ICBM-07-27 | cosmetic | logic | `lang/en_US.lang` key `command.icbmclassic:icbm.remove` | Contains `%1%d`, an invalid format token. |
| ICBM-07-28 | cosmetic | logic | `lang/en_US.lang` key `item.icbmclassic:laserDetonator.info.info` | Doubled `.info`; the item's tooltip lookup is `.info`, so the designator has no tooltip. |
| ICBM-07-29 | cosmetic | logic | `lang/en_US.lang` | Text typos: "Debilition", "Unexpect", "targetted", "Transfered", "aswell". |
| ICBM-07-30 | cosmetic | 1.12-only | `sounds.json` | Every sound has `"sream": false` instead of `"stream"`. Ignored, default behavior is the same. |
| ICBM-07-31 | cosmetic | logic | `Credits.md` | The credits table lists nine sounds that no longer exist and omits license data for all but three present sounds. |
| ICBM-07-32 | visual | logic | `client/render/entity/layer/LayerChickenHelmet.java:36` | Lowers the alpha test threshold to near zero and never restores it. Suspected to leak to later draws in the same frame. |
| ICBM-07-33 | visual | logic | `client/fx/ParticleLauncherSmoke.java:88-96` | The custom collision move zeroes horizontal velocity on a block hit but not vertical velocity (vanilla does both), so pad smoke keeps pushing down into the floor. |

**Count:** 33 suspected bugs (IDs 01 to 33); the range 34 to 39 is unused.
