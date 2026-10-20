# A1: ICBM 2 modular missiles (appendix)

## Overview

A design-option appendix on **ICBM 2** (BuiltBrokenModding/ICBM, MIT code, Minecraft 1.7.10), the older sibling of ICBM-Classic. Its missile is built from a casing plus swappable modules (warhead, engine, guidance, trigger), assembled on hand-operated and automated workstations, stored in silos and fed by magazines. GregGuns decided in D8 ([`DECISIONS.md`](../../gregguns-icbm-spec/DECISIONS.md#d8-in-detail)) to build missiles on a Galacticraft NASA Workbench page with part slots, so ICBM 2 is the closest prior art. This appendix records which module types exist, what each changes, and how they combine, so D8 can borrow ideas.

Source: ICBM @ `13e6a5245c76f5599baca521b462168b9ebe75d8`. Java paths are relative to `src/main/java/com/builtbroken/icbm/` and content JSON to `src/main/resources/content/icbm/`. Permalinks look like `https://github.com/BuiltBrokenModding/ICBM/blob/13e6a5245c76f5599baca521b462168b9ebe75d8/<path>#L<a>-L<b>`. The `archive/` folder (dead 1.6 code) is ignored. Depth is moderate, as this is an options appendix, not full entity sheets.

## Contents

- [modular-missiles.md](modular-missiles.md): size classes, casings, warheads and their explosive caps, engines and fuels, guidance (gears vs. chips), triggers, how the modules combine into flight (respawn-over-target math), unfinished flight-data classes.
- [workstations.md](workstations.md): the small missile workstation (click-on-the-missile assembly), the auto crafter, and the warhead station; GUI flow and slots.
- [silos-and-doors.md](silos-and-doors.md): small and standard silo footprints and recipes, the silo door, missile magazines, fire sequence.
- [command-network.md](command-network.md): short summary of Command Central, antennas, connectors, link tool and GPS flag.

## Key facts

- A missile is **one casing item with three module stacks nested in its NBT** (`warhead`, `engine`, `guidance`); a trigger sits inside the warhead. All module state lives in the module's own item.
- Five sizes (micro, small, standard, medium, large). Warhead caps are 1, 5, 20, 64 and 128 explosive items, and blast size scales with the number of items in the casing. Medium and large warheads are disabled in the creative lists.
- Only `engine range > 0` and `engine speed > 0` are needed to launch. **Range is never enforced in flight**, guidance **failure chance is never read**, and the flight-path classes (arc and direct) are unused.
- Flight is a stub: climb at engine speed, vanish at Y = 400, then respawn above the target after `horizontal distance / speed + 1` ticks, falling straight down at 1 block/tick, scattered by the guidance fall-off range (100 with none).
- Hand assembly is clicking the missile: guidance on the middle, warhead on the nose, engine on the tail. Automation uses three input slots plus a casing slot.
- One silo holds one missile of one size. The small silo is 1x3x1, the standard silo 1x7x1 (spawn offset +7), the door a 3x1x3 plate that takes 9 s to open.

## Ideas worth borrowing for D8

Each idea is tied to a D8 workbench slot (warhead, body plates, engine, fins, guidance computer).

| Slot / topic | ICBM 2 idea | Why it fits GregGuns |
|---|---|---|
| **Warhead** | Explosive amount = items in the casing, capped per casing size (1 / 5 / 20 / 64 / 128). The blast scales with that count | Use GT material quantity (cells or dust count) against a tier cap. Gives players a continuous knob and the tier a hard ceiling |
| **Body plates** | Casing size is the missile's class and gates which silo and which warhead fit; health, mass and width are data (JSON), not code | Make the plate tier set size class, mass and entity health from one data table. Skip ICBM's enum-and-switch (its health table is off by one, bug 04) |
| **Engine** | Engines have a speed and a range figure from the fuel; the range figure scales with fuel quantity and quality (coal 15 per item, coke x2, logs x0.85, sticks x0.05) | Matches D19 and D20: fuel type sets speed, fuel amount sets range. **Actually enforce it** (ICBM does not) by comparing fuel range to target distance at launch, so "unburned fuel" in D20 has a real number to work from |
| **Fins** | ICBM has none as parts (they are only a crafting ingredient of the casing) | Treat fins as ICBM treats guidance: a quality slot with one number, e.g., scatter radius, instead of a pure cosmetic ingredient |
| **Guidance computer** | A tier ladder of two numbers: fall-off range (scatter radius in blocks) and failure chance. Values run from 100 blocks / 50% (wood gears) to 20 / 3% (best chip). Scatter is applied per axis as the difference of two uniform rolls, so hits cluster near the aim point | Take the two-number model and the triangular scatter formula (MIT-portable, [modular-missiles.md](modular-missiles.md#respawn-over-target-math-mit-portable)). Make the tiers monotonic (ICBM's are not) and actually roll failure |
| **Fusing** | A trigger inside the warhead decides when it fires, using impact kinetic energy against min and max bounds | Optional extra slot. A kinetic-energy window gives "bunker buster" (high minimum) versus "airburst" (low maximum) variants. ICBM never filled in the bounds |
| **Combination rule** | Slots are independent, a launch needs only an engine, and everything else degrades gracefully (no guidance = 100 scatter, no warhead = a harmless dud) | Good for a workbench: partial builds are legal; the launch pre-check names the missing part |
| **Assembly** | Hand mode (click each end) and auto mode with "required parts" toggles on the same machine | The NASA Workbench is the hand mode. A GT Assembler recipe could be the auto mode, with the same required-part flags |
| **Silo rules** | One missile per silo, size filter on the silo, shot delayed by a slow-opening door that the silo forces open, a 40-tick re-arm delay, a 200-tick close delay | A cheap, readable launch sequence for the T3+ multiblock. Rails or hatches can feed the single slot, as ICBM's 2-second-per-hop magazine does |
| **Footprints** | Tall 1-wide shafts (3 and 7 high) for the silo, a 3x1x3 door, a 1x3x1 magazine | Reference sizes for the GT multiblock layouts |
| **Fall from orbit** | Missiles leave the world at Y 400 and re-enter over the target, a travel time computed from speed and distance only | Cheap, chunk-load-friendly, and compatible with the D11 one-chunk-ticket rule. Add a chunk load on the target instead of ICBM's "wait until loaded" |
| **Range of radio** | Antenna reach grows with tower size and height (100 blocks per piece) | Only if the design wants wireless launch control; D13 favors OpenComputers |

Things **not** to copy: the deprecated enum-based registries (`MissileSize`, `MissileModuleBuilder`), the module-as-nested-item-NBT save format (slow and large), and dead trajectory classes.

## Coverage

Read for this appendix (code with comments, imports and getters filtered out for the big classes):

- `content/missile/data/` (6 of 6 non-client files): `MissileSize`, `MissileCasingData`, `Missile`, `FlightData`, `FlightDataDirect`, `FlightDataArk`. `AmmoDataMissile`, `AmmoTypeMissile`, `TriggerCauseMissileDestroyed` not read.
- `content/missile/parts/` (all non-render logic): `MissileModule`, `MissileModuleBuilder`, 5 warhead classes, `Warhead`, `WarheadCasings`, `Engines`, `RocketEngine`, creative, fluid (`RocketEngineFluid`, `Fuel`, `Oil`), solid (`Solid`, `Gunpowder`, `CoalPowered`), 3 chips, `Guidance`, `GuidanceModules`, gears (wood, stone, iron, diamond sampled), `Trigger`, `Triggers`, 3 impact triggers. The three casing render classes were skimmed for dimensions only. Recipe helper classes (`RocketEngineCoalRecipe`, `RocketEngineGunpowderRecipe`, `RecipeMicroMissile*`) were sampled.
- `content/missile/entity/EntityMissile`, `tracking/MissileTracker`, `MissileTrackingData` (the latter two partly).
- `content/crafting/station/` (7 of 12): `TileAbstractWorkstation`, `TileSmallMissileStationBase`, `TileSmallMissileWorkstation`, `TileSMAutoCraft`, `MissileAssemblyLine`, `TileWarheadStation`, `ContainerWarheadStation` (slots only). The GUI, client workstation render and container classes were not read.
- `content/launcher/` (6 of about 12 relevant): `TileAbstractLauncher` (read for logic, partly skimmed), `TileMissileContainer`, `TileSmallSilo`, `TileStandardSilo`, `TileSiloDoor`, `DoorData`; door listeners only grepped.
- `content/storage/` : `TileSmallMag`, `IMissileMag`, `IMissileMagOutput`.
- Command network (summary level): `TileCommandController` (radio handler and update), `TileCommandSiloConnector` (constants and link), `AntennaNetwork` (range section), `ItemGPSFlag`. `TileAntenna`, `TileAntennaPart`, `BlockAntennaParts`, the silo interface classes, `ItemLinkTool` and local/direct controllers were grepped, not read in full.
- JSON: `missile/*.json`, `silo/*.json`, `door/*`, `storage/smallMag.json`, `launcher/standard.json`, `workshop/missile.json` (head), `controller/controller.local.json`.
- Not covered by design: explosives and blasts, client rendering and GUIs, the radar and launch-pad internals (see the main ICBM spec areas).

## Known limits

- The workstation click zones and GUI layouts are described from server logic only, not from the client code.
- Whether the JSON `for` multiblock loops include index 0 (the host cell) is not verified. Footprints for silos come from explicit `tiles` lists, which exclude the host.
- Mass units, and the explosive registry's per-item size, belong to other mods (VoltzEngine) and are not in this repository.
- The shipped door id (`starseeker3x3`) versus the tile's default (`3x3StarSeeker`) could resolve through code outside the files read (bug 18 is "suspected").
- Config defaults are mostly hard-coded `public static` fields here; ICBM 2 has few config keys for this area.

## Suspected bugs

IDs `ICBM-A1-01` to `ICBM-A1-23` are used; `24` to `39` are unused. All are "logic" (could be inherited), since ICBM 2 targets 1.7.10 too. Line numbers are in the files named, at the commit above, relative to `src/main/java/com/builtbroken/icbm/content/`.

| ID | Where | What | Severity |
|---|---|---|---|
| ICBM-A1-01 | `missile/data/flight/FlightDataDirect.java:11-15` | `doOnce` starts true and the test is `!doOnce`, so motion is never computed (stays 0). `updatePath` also never calls the parent, so `ticks` and distances stay -1. The class is dead anyway | logic, dead code |
| ICBM-A1-02 | `missile/entity/EntityMissile.java:136-152` | The `ark` / `track` flag of all `setTarget` methods is ignored; no arc or direct choice exists. The 1000-block apex in `FlightDataArk.java:18` is above the build limit | logic, dead code |
| ICBM-A1-03 | `missile/data/missile/MissileSize.java:90` with `missile/large.json:6` | Default casing id is built as `icbm:<size>Missile`; the large casing is `icbm:large`, so `getDefaultMissileCasing` returns null for LARGE and `Missile.createMissile(LARGE, ...)` fails | logic, crash |
| ICBM-A1-04 | `missile/entity/EntityMissile.java:75-93` | Health switch uses cases 0, 1, 2, 4, 5 against ordinals 0-4: MEDIUM (3) falls through to 2 HP, LARGE gets 100, and case 5 never exists | logic, gameplay |
| ICBM-A1-05 | `missile/parts/guidance/*` (`Guidance.java:26`) | `getChanceToFail` is defined for every guidance and called nowhere | logic, gameplay |
| ICBM-A1-06 | `missile/data/missile/Missile.java:189`, `launcher/TileAbstractLauncher.java:357` | Engine `getMaxDistance` is used only as a "greater than zero" launch gate. Fuel amount never limits range | logic, gameplay |
| ICBM-A1-07 | `missile/parts/engine/solid/RocketEngineGunpowder.java:49,71`; `RocketEngineCoalPowered.java:90` | Ore dictionary names are read with `getOreName(i)` (loop index) instead of `ids[i]`, so ore-dict fuel matching checks the wrong names | logic, gameplay |
| ICBM-A1-08 | `crafting/station/warhead/TileWarheadStation.java:255-259` | Automation insert of an explosive tests `getTriggerStack() == null` instead of the explosive slot, then overwrites the explosive slot with one item, discarding what was there | logic, gameplay |
| ICBM-A1-09 | `crafting/station/small/auto/TileSMAutoCraft.java:507` | `getBoolean` reads the `isAutocrafting` key for every flag, so `requiresWarhead`, `requiresGuidance` and `requiresEngine` reload as that value | logic, gameplay |
| ICBM-A1-10 | `crafting/station/small/auto/TileSMAutoCraft.java:156` | `canCraft` checks room left in `INPUT_SLOT`, not the output slot | logic, gameplay |
| ICBM-A1-11 | `crafting/station/small/auto/TileSMAutoCraft.java:122` | `doCrafting` sets the output slot to the new missile instead of adding to it, replacing a matching stack already there | logic, gameplay |
| ICBM-A1-12 | `crafting/station/TileAbstractWorkstation.java:178` | `ejectCraftingItems` loops `i <= getSizeInventory()`, one past the last slot | logic, crash |
| ICBM-A1-13 | `launcher/controller/remote/antenna/AntennaNetwork.java:169-172` | West and north send ranges multiply the (negative) box minimum by 100, so the product is below zero and clamps to the bonus alone. Only east and south get 100 blocks per piece | logic, gameplay |
| ICBM-A1-14 | `launcher/controller/remote/antenna/AntennaNetwork.java:165` | Altitude bonus divides by `192 - maxY + baseY` where `192 - (maxY + baseY)` was meant; a higher base gives a smaller bonus | logic, gameplay |
| ICBM-A1-15 | `launcher/TileAbstractLauncher.java:110-128,331-338` | If the engine exists but `canLaunch` is false (empty tank), `fireMissile` is never cleared. The silo door is held open and the silo retries forever, and each 20-tick redstone check re-arms it | logic, gameplay |
| ICBM-A1-16 | `missile/entity/EntityMissile.java:417` | `doDropsOnDeath` calls `getWarhead().getExplosive()` with no null check. A missile with no warhead (a legal build) that lands reaches it and throws | logic, crash |
| ICBM-A1-17 | `missile/entity/EntityMissile.java:216-218` | `getCommandSenderName` calls `toString` on the warhead's explosive handler, which is null for an empty warhead | logic, crash |
| ICBM-A1-18 | `launcher/door/TileSiloDoor.java:62`, `launcher/door/listeners/DoorBlockStackListener.java:85,89-96` | Default `doorID` `3x3StarSeeker` does not match the data id `starseeker3x3`; and a door placed from an item with no `doorID` NBT sets `doorID` to null, which the lookup lowercases. Resolves to a null door, or throws. Suspected | logic, crash |
| ICBM-A1-19 | `launcher/TileMissileContainer.java:46-53` | `readDescPacket` reads an item stack and calls `getItem()` on it with no null check | logic, crash |
| ICBM-A1-20 | `missile/parts/trigger/Trigger.java`, `impact/ImpactTrigger.java:22,30-36` | The `enabled` toggle and `failureChance` are never consulted by `shouldTrigger`. A warhead with a trigger installed also refuses to fire when its missile is shot down | logic, gameplay |
| ICBM-A1-21 | `launcher/controller/remote/central/TileCommandController.java:~370-400` | Radio data is cast blindly (`(short) data[0]`, strings). A pass code sent as an Integer throws and is only logged, so the shot silently fails | logic, gameplay |
| ICBM-A1-22 | `storage/small/TileSmallMag.java:96` | Neighbor scan checks `location.isChunkLoaded()` (its own location) instead of the neighbor `l` | logic, cosmetic |
| ICBM-A1-23 | `launcher/TileAbstractLauncher.java:349` | Uses the raw `fofStation` field instead of `getFoFStation()`, so after a world reload the missile misses its friend-or-foe tag until the getter runs once | logic, gameplay |
