# 01 · Core: Techguns 1.7.10 (v0.1.2_alphatest4.1)

This area covers the mod skeleton:
- mod entry point and lifecycle, plus registrations that don't belong to any one content area (metal items and ores, projectile entity IDs, tile entity names, oredict, dispenser behaviours, creative tab, GUI ID table);
- the main config file;
- the network channel and every packet;
- keybinds and mouse handling, HUD overlays, and the inventory tab;
- per-player extended data and the per-tick player logic (armor bonuses, auto-feeder, flight, night vision, step assist);
- the global Forge/FML event handlers;
- client sound plumbing, death effects, attack timing and the version checker;
- general utilities (explosion, block/structure helpers, math, packet helpers);
- the FTB Utilities, Tinkers' Construct and Thermal Expansion integrations.

**Out of scope here:**
- gun stats and behaviour ([02](../02-guns/index.md)); gun models, textures and sounds ([03](../03-gun-visuals-audio/index.md));
- projectiles, particles and FXList ([04](../04-projectiles-fx/index.md)); armor bonuses and special-slot items ([05](../05-armor/index.md));
- items ([06](../06-items/index.md)); blocks and fluids ([07](../07-blocks/index.md)); machines ([08](../08-machines-production/index.md), [09](../09-machines-utility/index.md));
- NPCs and spawning ([10](../10-entities-npcs/index.md)); worldgen and structures ([11](../11-worldgen/index.md));
- MineTweaker ([08](../08-machines-production/index.md)).

## Contents

| File | Topic |
|---|---|
| [lifecycle-and-registration.md](lifecycle-and-registration.md) | Mod identity, mcmod.info, access transformer, creative tab, preInit/init/postInit order, metal items, ores and their recipes, oredict, entity ID table, tile entity names, recipe sorter, dispenser behaviours, GUI ID table, client renderer registration index, proxy API |
| [config.md](config.md) | Every key in [`Techguns.cfg`](lifecycle-and-registration.md#lifecycle-overview): category, type, default, range, effect |
| [networking.md](networking.md) | Channel `Techguns`, discriminator table, every packet's fields, direction, sender and handler, plus the sync model |
| [input-and-hud.md](input-and-hud.md) | Keybinds (and debug keybinds), left-click gun firing, zoom and FOV, ammo/status HUD, 24-point armor bar, inventory tabs, [`TGPlayerInventory.png`](../05-armor/extra-inventory.md#1-data-model-tgplayerinventory-iinventory-12-slots) atlas map |
| [player-state.md](player-state.md) | [`TechgunsExtendedPlayerProperties`](player-state.md#techgunsextendedplayerproperties) fields, NBT keys, slot layout, DataWatcher mirrors, death/clone, per-tick player logic, auto-feeder |
| [events.md](events.md) | Catalogue of every event subscription and what it does |
| [client-effects-audio.md](client-effects-audio.md) | [`TGSound`](client-effects-audio.md#tgsound), sound categories, [`SoundUtil`](utilities.md#soundutil) routing, core sound list, recoil/reload timing, muzzle-flash placement, legacy named particles, death effects and gore table, version checker |
| [utilities.md](utilities.md) | Inventory helpers, [`ProjectileExplosion`](../04-projectiles-fx/explosions.md#projectileexplosion), damage sources, `EntityDeathUtils`, [`MathUtil`](utilities.md#mathutil), [`BlockCoords`](utilities.md#blockcoords--blockposind), [`MBlock`](../11-worldgen/structure-system.md#mblock) rotation tables, [`BlockUtils`](utilities.md#blockutils) (structure placement, terrain shaping, height queries), [`PacketUtil`](utilities.md#packetutil-itemstack-wire-format) wire format, [`ItemUtil`](utilities.md#itemutil), [`NPCUtil`](../10-entities-npcs/spawning.md#4-camp-flags-and-npcutil), dispenser behaviour |
| [integrations.md](integrations.md) | FTBU friends, TConstruct materials and inventory tab, Thermal Expansion stub |
| [textures-sounds-lang.md](textures-sounds-lang.md) | Every core-owned texture (with layout and palette), sound file and lang key |

## Key facts at a glance

- **Mod id** `Techguns`, asset domain `techguns`, network channel `Techguns` with 16 registered messages.
- **Config files:**
  - `config/Techguns.cfg`, with categories general, ID Conflicts, NPC Spawn, Ore Drills, Spawn Controll, Damage Factors, Debug and Clientside;
  - `config/Techguns_WeaponStats.cfg` ([02](../02-guns/index.md)).
- **Player data:**
  - extended property `TechgunsExtendedPlayer`, holding 12 extra slots: face, back, 3 food, heal and 6 ammo;
  - the face and back items are mirrored to DataWatcher IDs 23/24 for rendering;
  - toggles: night vision, safe mode (default on), hover, jetpack, HUD, step assist.
- **Default keys:** N night vision, B safe mode, V step assist, R force reload, J jetpack.
- **Event handlers:**
  - [`TechgunsEventhandler`](events.md#techgunseventhandler-forge-bus) on the Forge bus;
  - [`TechgunsTickHandler`](events.md#techgunstickhandler-fml-bus) on the FML bus;
  - [`GuiOverlayAmmoCount`](events.md#guioverlayammocount-forge-bus-client) (client, Forge bus).
- **Projectile entity IDs** 1–26 are registered in preInit. NPC IDs continue from 27 in [`TGEntities`](../10-entities-npcs/mob-catalogue.md#registration-javatechgunstgentitiesjava) ([10](../10-entities-npcs/index.md)).
- **Integrations:**
  - TConstruct: 5 tool materials (ids 50–54) plus an inventory tab;
  - FTBU: friends decide player-vs-player hostility. Without FTBU, every player is everyone's friend;
  - Thermal Expansion: a no-op.

## Coverage notes

- Every Java file in scope was read in full. `TGPlayerInventory`, [`TechgunsGuiHandler`](../08-machines-production/gui-handler.md#1-techgunsguihandler-complete-gui-id-table), [`TGGuiTabButton`](../05-armor/extra-inventory.md), `TGWeaponStats` and a few call sites were read only to explain behaviour; they are documented here only where they touch core features.
- Core-owned textures were each viewed and described (19).
- The particle sheets referenced by the legacy particle table (`textures/fx/*`) belong to [04](../04-projectiles-fx/index.md) and are only named here.

## Related areas

- [02-guns](../02-guns/index.md): the fire/reload packets, the client tick handler that drives guns, and the weapon-stats and damage-factor config.
- [03-gun-visuals-audio](../03-gun-visuals-audio/index.md): client timing state for recoil and muzzle flash, and sound routing (`TGSound`).
- [04-projectiles-fx](../04-projectiles-fx/index.md): particle and death-effect packets, the legacy particle table and the explosion utility.
- [05-armor](../05-armor/index.md): the extended player inventory, per-tick armour bonuses and the armour HUD.
- [06-items](../06-items/index.md): items, metals and ores registered directly in `Techguns.java`, plus oredict.
- [08-machines-production](../08-machines-production/index.md), [09-machines-utility](../09-machines-utility/index.md): the GUI handler ID table and the tile-entity sync and button packets.
- [10-entities-npcs](../10-entities-npcs/index.md): spawn and faction config and FTB Utilities friend checks.
- [11-worldgen](../11-worldgen/index.md): worldgen config gates and the structure helpers in `util/` (`BlockUtils`, `MBlock`).

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| CORE-01 | Iron nuggets registered twice | `java/techguns/Techguns.java:360,374` | With [`addIronNuggets`](config.md#category-general) on, [`nuggetIron`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) is registered under the same name twice. FML either throws a duplicate-registration error or logs a big warning, depending on the Forge build | crash (likely) |
| CORE-02 | NPE when the version checker is disabled | `java/techguns/events/TechgunsTickHandler.java:492` | `versionChecker` is only created when [`EnableVersionChecker`](config.md#category-clientside) is true, but the player-tick handler dereferences it unconditionally on the client | crash |
| CORE-03 | Undeclared hard Chisel dependency | [`java/techguns/client/ClientProxy.java:642,1280`](lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) (also `blocks/TGChiselBlocks`) | Chisel's `RendererCTM` is instantiated with no `isModLoaded` check, while Chisel is declared only as `after:`. Without Chisel, class loading fails | crash |
| CORE-04 | Ammo dupe on partial fit | `java/techguns/Techguns.java:766-768` | When the ammo slots take part of the stack, the **original full stack** is then added to the main inventory instead of the remainder | gameplay (dupe) |
| CORE-05 | Packet handlers mutate world state off-thread | `java/techguns/packets/*.java` (every `Handler.onMessage`) | Handlers run on the Netty thread and touch entities, inventories, tile entities and potions without scheduling onto the main thread. This risks races and ConcurrentModificationExceptions | crash |
| CORE-06 | Server opens any GUI id a client asks for | [`java/techguns/packets/PacketOpenPlayerGUI.java:37`](networking.md#4--packetopenplayergui-cs) | Any guiID > 0 is passed to `openGui` at the player's position. Machine GUIs then cast a missing or wrong tile entity (or the block, for ID 20) and fail on the server | crash |
| CORE-07 | GUI tab ID config is ignored | `java/techguns/gui/TechgunsGuiHandler.java:69,121`; [`java/techguns/plugins/tconstruct/TGInventoryTab.java:18`](integrations.md#client-inventory-tab) | The handler and the TConstruct tab hard-code 17, but the vanilla tab button sends [`TechgunsGUI_TabID`](config.md#category-id-conflicts). Changing the config breaks the tab or opens the wrong GUI | gameplay |
| CORE-08 | Unvalidated tile-entity requests | [`java/techguns/packets/GUIButtonClick.java:54`](networking.md#0--guibuttonclick-cs); [`java/techguns/packets/RequestTileEntitySync.java:46`](networking.md#2--requesttileentitysync-cs) | No distance or permission check (clients can press machine buttons anywhere), arbitrary coordinates can force chunk loads, and a null description packet would NPE | crash / exploit |
| CORE-09 | Partial player sync resets flags | [`java/techguns/packets/PacketTGExtendedPlayerSync.java:121-129`](networking.md#5--packettgextendedplayersync-sc) | The handler copies food, saturation and the six toggles even when they weren't transmitted (size = 2), overwriting them with defaults | visual |
| CORE-10 | StartTracking/StopTracking handlers are dead | `java/techguns/events/TechgunsEventhandler.java:598-615` | Both check `isRemote`, but these events only fire on the server. The other-player sync request and the client cleanup never happen | visual |
| CORE-11 | Fragile string decoding in packets | [`java/techguns/packets/PacketSpawnParticle.java:51`](networking.md#13--packetspawnparticle-sc); [`PacketSpawnParticleOnEntity.java:33`](networking.md#14--packetspawnparticleonentity-sc); [`PacketPlaySound.java:93,111`](networking.md#9--packetplaysound-sc) | `readBytes(len).array()` assumes a heap buffer whose backing array is exactly `len` bytes (pooled or direct buffers break this) and never releases the buffer. All strings use the platform default charset | crash / visual |
| CORE-12 | [`FixedSprintFovMultiplier`](config.md#category-clientside) unused | `java/techguns/events/TechgunsEventhandler.java:99-101` | Sprint FOV is hard-coded to 1.2. The config key has no effect | cosmetic |
| CORE-13 | Night-vision HUD flag set by any player | `java/techguns/events/TechgunsTickHandler.java:319` | `setHasNightvision` runs for every ticked player on both sides (other players, and the integrated server), so the HUD icon can reflect someone else's armor | visual |
| CORE-14 | Step height forcibly clamped | `java/techguns/events/TechgunsTickHandler.java:301-303` | Without Techguns step assist, any step height above 0.5 is reset to 0.5 every tick, breaking other mods' step assist | gameplay |
| CORE-15 | NV toggle-off strips potion night vision | [`java/techguns/packets/PacketTGKeybindPress.java:54`](networking.md#7--packettgkeybindpress-cs) | Toggling NV off removes *any* Night Vision effect, including one from a brewed potion | gameplay |
| CORE-16 | Auto-feeder runs on the client too; container items lost | `java/techguns/events/TechgunsTickHandler.java:383-447` | Client and server both consume food and call `onFoodEaten` (double side effects, later resynced). Only `onFoodEaten` runs, so stew bowls and other container returns are lost | gameplay |
| CORE-17 | Broken armor shows only the yellow warning | `java/techguns/events/GuiOverlayAmmoCount.java:164-169` | The else-if means the first piece under 35 % never gets checked for "broken". A single broken piece shows yellow instead of red | visual |
| CORE-18 | HUD text alignment counts formatting codes | `java/techguns/events/GuiOverlayAmmoCount.java:68,111,139` | Width is `String.length()*6`, including the "§e"/"§f" codes, so text sits about 12 px too far left | cosmetic |
| CORE-19 | Explosion events and knockback incomplete | `java/techguns/util/ProjectileExplosion.java:185,245` | Detonate is posted with a dummy explosion whose block list is empty, so protection mods can't filter blocks. Player knockback is collected but never sent to clients | gameplay |
| CORE-20 | NaN explosion damage when r1 == r2 | `java/techguns/util/ProjectileExplosion.java:212` | The falloff divides by (dmgRadius2 − dmgRadius1) | gameplay (edge case) |
| CORE-21 | "Gun position" sounds are offset sideways | `java/techguns/client/audio/TGSound.java:26,96` | The offset uses (cos yaw, sin yaw), which points to the entity's left, not forwards as documented | cosmetic |
| CORE-22 | `Vec2` helpers reversed or broken | `java/techguns/util/MathUtil.java:126,146,155,183` | `substract` returns B − A, so `getVecTo`/`getVecFrom` are reversed. `getAngleBetween` is mathematically wrong | cosmetic (latent) |
| CORE-23 | Weighted palette bias | [`java/techguns/util/MultiMBlock.java:59,276`](../11-worldgen/structure-system.md#multimblock-random-palette-entry) | The roll is in [0, total] inclusive, giving index 0 one extra outcome. The chest-loot check tests the first palette block, not the rolled one | cosmetic |
| CORE-24 | `flattenArea` "median" wrong | `java/techguns/util/BlockUtils.java:111-122` | It picks the lower-middle element, and returns 0 for footprints of ≤ 2 columns, which would flatten the terrain down to y≈0 | gameplay (worldgen) |
| CORE-25 | Area spawn sampling skips the far edge | `java/techguns/util/BlockUtils.java:683,691` | The edge-clamp branch can't be reached, so the far row and column are only sampled when the size is divisible by the step | cosmetic |
| CORE-26 | TConstruct IMC late and partly duplicated | [`java/techguns/plugins/tconstruct/TechgunsTConstructIntegration.java:203-208`](lifecycle-and-registration.md#postinit-in-order); `java/techguns/Techguns.java:714-722` | IMC is sent in postInit (Techguns loads after TConstruct, so it may never be read). The second `addMaterialItem` reuses the Plastic id, leaving carbon, titanium, obsidian steel and rubber without repair items | gameplay |
| CORE-27 | Thermal Expansion integration is a no-op | `java/techguns/plugins/thermalexpansion/TechgunsThermalExpansionIntegration.java:8` | The body is commented out, and `@Optional.Interface` names a non-existent interface path | cosmetic |
| CORE-28 | Toggles default to off for old saves | `java/techguns/extendedproperties/TechgunsExtendedPlayerProperties.java:176-182` | Keys go into the player NBT root; missing booleans load as false, so jetpack, HUD and step assist (code default true) start disabled | gameplay |
| CORE-29 | Debug keybinds untranslated | [`java/techguns/debug/Keybinds.java:205-220`](input-and-hud.md#keybinds-tgkeybinds-client-only) | Category `key.categories.techguns` and all names (`key.+x` …) have no lang entries | cosmetic |
| CORE-30 | mcmod.info author list | `resources/mcmod.info` | `authorList` holds one string, "pWn3d_1337, Th3_St4lk3r", instead of two entries | cosmetic |
| CORE-31 | Glider loop is stereo | `java/techguns/client/ClientProxy.java:1365` (sounds.json `glider.loop`) | The event points at `glider_loop_stereo.ogg`, so the attached moving sound isn't positional for observers. The mono `glider_loop.ogg` is unused | cosmetic |
| CORE-32 | Semi-auto click swallowed during cooldown | `java/techguns/events/TechgunsTickHandler.java:129-136` | `keyFirePressed` is cleared for semi-auto guns even when `fireDelay` blocked the shot, so slightly early clicks are lost | gameplay |
| CORE-33 | Armor bar doesn't advance `left_height` | `java/techguns/events/GuiOverlayAmmoCount.java:186-233` | The vanilla armor element is cancelled and redrawn at a fixed H−48, and Forge's left HUD stacking height isn't updated, so it can overlap other left-side HUD elements (for example extra health rows) | visual |
| CORE-34 | Missing death message keys | [`java/techguns/util/BulletDamageSource.java:60,64`](textures-sounds-lang.md#missing-or-untranslated-keys-used-by-core-code) | Damage type `bullet` needs `death.attack.bullet[.item]`, but only `tg_bullet` exists in the lang file | cosmetic |
| CORE-35 | Unused access transformer | `resources/META-INF/techguns_at.cfg` | `World.chunkExists` is made public, but its only use is commented out | cosmetic |
| CORE-36 | Akimbo split inserts a client-side ghost gun | `java/techguns/events/TechgunsTickHandler.java:573-611` | `ItemCraftedEvent` also fires on the client and adds the second gun to the client inventory (desync until resync). It also dereferences the output's NBT without a null check | gameplay |
| CORE-37 | Logout cleanup is dead | `java/techguns/events/TechgunsTickHandler.java:619-625` | Client-only handler on an event FML fires on the server. Stale attack-time entries accumulate | cosmetic |
| CORE-38 | Inventory merge ignores NBT | `java/techguns/Techguns.java:789` | `addItemToInventory` merges stacks whose NBT differs (item + damage only), losing the incoming stack's NBT | gameplay |
| CORE-39 | Sound categories have no effect | [`java/techguns/TGConfig.java:336-338`](config.md) | `getVolumeMultiplier` is a stub. [`TGSoundCategory`](client-effects-audio.md#tgsoundcategory) is transmitted but never applied | cosmetic |
