# 05 – Armour, special-slot equipment and the Techguns inventory tab

Behavioural specification of Techguns 1.7.10 v0.1.2 alpha. This area covers:

- **45 armour items:**
  - 11 four-piece sets: T1 Combat, T1 Scout, T1 Miner, Steam, T2 Combat, T2 Commando, T2 Riot/Ranger Veteran, T3 Combat, T3 Power, T3 HEV, T3 Exo
  - the Beret
- **Their stats and damage-type resistances**, camo variants, per-piece bonuses, and the Steam/RF power system.
- **9 face/back "special-slot" items:** Gas Mask, Nightvision Goggles, Tactical Mask, Oxygen Mask, Oxygen Tanks, Glider, Jetpack, Jump Pack, Anti Gravity Device. Includes their flight and gliding physics, fuel, and reloading.
- **The extra 12-slot Techguns player inventory:** face, back, 3 food, 1 heal, 6 ammo. Includes its GUI tab, toggle buttons and slot types.
- **Every armour and attachment model**, the player-attachment renderers, the akimbo off-hand gun renderers, and the 3-D item icons for Steam and Power armour.
- **All 82 textures in `textures/armor/`**, plus the inventory GUI texture.

## Contents

| File | Covers |
|---|---|
| [armor-catalogue.md](armor-catalogue.md) | Class hierarchy; materials (durability, 9 damage-type values, penetration resistance); protection maths; every piece with its stats, bonuses, model, textures and repair mats; set totals; camo system; tooltips; the powered-armour energy system |
| [set-bonuses.md](set-bonuses.md) | `TGArmorBonus`; how bonuses are summed; where and how each is applied and removed (attribute modifiers, events, potions, flight); player toggles |
| [special-equipment.md](special-equipment.md) | Face/back items: stats, reload, behaviour; glider, jetpack, jump-pack and anti-grav physics; armour and equipment sounds; FX names; toggle packets and keybinds |
| [extra-inventory.md](extra-inventory.md) | `TGPlayerInventory`, `TGSlotType`, slot classes, container layout and shift-click rules, GUI, tabs, toggle buttons, GUI texture layout |
| [rendering-and-models.md](rendering-and-models.md) | Armour model selection and posing; part breakdown of every armour and attachment model; face/back renderers; akimbo renderers; `RenderArmorItem` transforms |
| [textures.md](textures.md) | All 82 armour textures: layout conventions, what each depicts, palette and usage |
| [recipes.md](recipes.md) | Every armour and special-equipment recipe; repair routes; loot entries |
| [lang-and-strings.md](lang-and-strings.md) | All lang keys with English text; hard-coded strings; missing keys |

## Key facts

- **Protection:** each piece has a value per damage type; it is the material total × the slot factor (helmet 0.25, chest 0.30, legs 0.25, boots 0.20). Each armour point removes 4 % of the non-penetrating share of the damage. Penetration resistance lowers incoming armour penetration.
- **Durability:** 13.75 × the material base durability, the same for every slot. Armour never breaks; at 1 point left it gives nothing.
- **Bonuses:** there are no full-set bonuses. Every bonus is per piece and summed with the others and with face/back items. They are re-applied as attribute modifiers each tick (speed ×(1 + sum), doubled while sprinting; health +HP; knockback resistance +sum), or used by event hooks (jump, fall, break speed, fire immunity, gun accuracy).
- **Powered armour:** the chestplate holds the energy (Steam/Power 3600, Exo 7200, HEV 10000) and refills the other pieces, which hold 10 each.
  - Drain: 1 per piece per second of movement; 5 per jump (leggings); 5 per fall > 3 blocks (boots); 1 per blocked fire hit (HEV).
  - An empty chest auto-consumes a battery item (Compressed Air Tank for Steam, Energy Cell for RF) and returns the empty.
  - Bonuses need power; protection does not.
- **Special items:** durability is used as fuel or charge. Each item auto-reloads from its ammo item (Energy Cell, Compressed Air Tank, Fuel Tank or Nuclear Power Cell); the Gas Mask instead uses a crafting refill with a Gas Mask Filter.

## Cross-references

- **01-core:** extended player properties and their NBT, packets, keybind registration, GUI handler, damage system (`TGDamageSource`, `getReduction`, penetration config), tick and event handler skeletons, HUD overlay, Tinkers' Construct tab integration.
- **02-guns:** gun accuracy use of `GUN_ACCURACY`; akimbo gun items; safe mode.
- **03-gun-visuals-audio:** gun models and textures used by the akimbo renderers; reload/recoil timers; the HUD icons for night vision and step assist.
- **04-projectiles-fx:** the particle and FX names `Jetpack`, `JumpPackBoost`, `JumpPackFall`, `AntiGravRing`; sound files.
- **06-items:** batteries and ammo items, shared-item metadata (glider, glider parts, oxygen mask, gas mask filter), all item icons in `textures/items/`, crafting materials.
- **08/09-machines:** Camo Bench (`switchCamo`), Repair Bench (`getRepairMats`), the other `TGSlotType` users.
- **10-entities-npcs:** NPCs spawned in Techguns armour with set camos.
- **11-worldgen:** loot chest categories.

## Coverage

- **Source files in scope, all read in full (56):**
  - `TGArmors`, `TGArmorBonus`
  - 15 classes in `items/armors/`
  - 17 model classes
  - `RenderArmor`, `RenderArmorItem`
  - 11 classes in `additionalslots/`
  - 7 classes in `gui/playerinventory/`
  - `TGSlotType`, `SlotArmor`
- **Supporting files read for behaviour:** `TGArmorMaterial`, `DamageSystem` (parts), `TechgunsTickHandler`, `TechgunsEventhandler` (parts), `PacketTGKeybindPress`, `PacketOpenPlayerGUI`, `TGKeybinds`/`TGKeybindsID`, `TechgunsExtendedPlayerProperties` (+Client), `GenericItemShared` (parts), `TGItems` (parts), `TGConfig` (parts), `ClientProxy` (parts), `TechgunsGuiHandler` (parts), `sounds.json`, `en_US.lang`.
- **Textures:** 82/82 in `textures/armor/` viewed and described, plus `textures/gui/TGPlayerInventory.png`.
- **Sounds:** 9 sounds.json events (19 files) referenced by armour and equipment code are listed with their triggers (special-equipment.md §6). One unreferenced file is noted.

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ARM-01 | Typo in Steam Helmet registry name | `java/techguns/TGArmors.java:136` | The registry name is `steam,_helmet` (with a comma); every other piece uses its unlocalized name. | cosmetic |
| ARM-02 | Penetration resistance compounds across armour pieces | `java/techguns/items/armors/GenericArmor.java:571-578` | Each piece subtracts its penetration resistance from the *shared* damage source's `armorPenetration`, applies the config multiplier again, and writes the result back. Later pieces see an already-reduced value, so the outcome depends on slot order and the number of pieces. | gameplay |
| ARM-03 | Bonus tooltip uses an else-if chain | `java/techguns/items/armors/GenericArmor.java:507-513` | If a piece has a health bonus, its speed and jump lines are hidden; if it has a speed bonus, its jump line is hidden. | cosmetic |
| ARM-04 | Health bonus is labelled in hearts but applied in health points | `GenericArmor.java:508`, `java/techguns/events/TechgunsTickHandler.java:254-258` | The tooltip shows "+1 Hearts", but the modifier adds 1 HP (half a heart). | cosmetic |
| ARM-05 | `SPEED_WATER` bonus probably does nothing | `java/techguns/events/TechgunsTickHandler.java:233-235` | It is added to the movementSpeed multiplier, but vanilla 1.7.10 swimming and lava movement do not use that attribute. The HEV's +5.0 water speed therefore has little or no effect. | gameplay |
| ARM-06 | Aiming a gun adds +5.0 speed, but only for speed-armour wearers | `TechgunsTickHandler.java:244-249` | Meant to cancel the item-use slowdown. It only applies when the armour speed is > 0, so players with any speed armour aim-walk at about 1.2× or more of walk speed, while others are slowed to 0.2×. | gameplay |
| ARM-07 | Step assist overrides other step-height sources | `TechgunsTickHandler.java:299-305` | Whenever step assist is off or unavailable, `stepHeight` is clamped to 0.5 every tick, which breaks other mods' step-height effects. | gameplay |
| ARM-08 | Night-vision HUD flag is set from other players' ticks | `TechgunsTickHandler.java:318-319` | `setHasNightvision` runs on the client for every ticked player, not just the local one, so the HUD indicator can show another player's state. | visual |
| ARM-09 | Powered-armour drain and battery reload also run client-side | `TechgunsTickHandler.java:220`, `java/techguns/items/armors/PoweredArmor.java:348-398` | The client consumes battery items from its own inventory copy (and does the same for other players' copies). This can desync until the server corrects it. | gameplay |
| ARM-10 | Steam jump sound plays for all powered leggings | `PoweredArmor.java:318-325` | Power Armour, HEV and Exo leggings all play `effects.steamarmorJump` on every jump. | cosmetic |
| ARM-11 | "No Camo" shown for stacks without NBT | `java/techguns/items/armors/GenericArmorMultiCamo.java:126-138` | Creative, NEI and loot stacks render camo 0 but their tooltip reads "No Camo". | cosmetic |
| ARM-12 | Unchecked camo index | `GenericArmorMultiCamo.java:35-50`, `java/techguns/client/renderer/additionalslots/RenderAdditionalArmorModel.java:36-38` | Any `camo` byte outside 0..count−1 (from NBT editing or a bad NPC call) causes an ArrayIndexOutOfBounds exception during rendering. | crash |
| ARM-13 | Missing lang keys | `GenericArmorMultiCamo.java:134`, `java/techguns/items/armors/ItemTGSpecialSlot.java:46` | There are no `t1_scout.camoname.*`, `t2_riotArmor.camoname.0` or `tacticalMask.camoname.3` keys, so the raw key is displayed. | cosmetic |
| ARM-14 | Tactical-mask texture path has the wrong case | `java/techguns/client/renderer/additionalslots/RenderAdditionalSlotItem.java:29` | The code requests `textures/armor/tacticalMask_lightgreen.png`, but the file is `tacticalmask_lightgreen.png`. Resource lookup inside a jar is case-sensitive, so camo 3 shows the missing texture. | visual |
| ARM-15 | Jump-key state can stay stuck | `java/techguns/items/armors/ItemJetpack.java:39-57`, `java/techguns/extendedproperties/TechgunsExtendedPlayerPropertiesClient.java:30-49` | Only the jetpack's tick sends BOOST_STOP. If the jetpack is removed while boosting, `jumpkeyPressed` stays true: the loop sound keeps playing, and a later jetpack immediately boosts on the server. | gameplay |
| ARM-16 | Dead branch in the jetpack toggle handler | `java/techguns/packets/PacketTGKeybindPress.java:74-81` | The inner "stop boost if disabling while pressed" branch sits inside a `!isJumpkeyPressed()` guard, so it can never run. | cosmetic |
| ARM-17 | Auto-heal slot cannot hold anything | `java/techguns/gui/playerinventory/TGPlayerContainer.java:37`, `java/techguns/gui/playerinventory/SlotTG.java:28-35` | No item reports `HEALSLOT`, and `SlotTG` rejects non-special items unless the type is NORMAL. The slot is dead UI. | gameplay |
| ARM-18 | Configurable tab GUI ID is not honoured by the handler | `java/techguns/gui/TechgunsGuiHandler.java:69,121`, `java/techguns/gui/playerinventory/GuiTGPlayerInventory.java:459` | The tab sends config `TechgunsGUI_TabID`, but the handler hard-codes `case 17`. Changing the config breaks the tab (the "ID conflict" option is useless). | gameplay |
| ARM-19 | Toggle buttons cache player properties statically | `java/techguns/gui/playerinventory/TGGuiToggleButton.java:122-139` | `props` is a static field filled once. After relogging, respawning or changing world, the buttons show a stale player's flags. | visual |
| ARM-20 | Both tab buttons share one button ID | `java/techguns/events/TechgunsEventhandler.java:350-351`, `GuiTGPlayerInventory.java:458-459` | Both tabs are created with the same `index`. This is harmless today but fragile. | cosmetic |
| ARM-21 | Oxygen Mask cannot be quick-equipped | `java/techguns/items/GenericItemShared.java:339-356` | The shared-item right-click handles BACKSLOT and AMMOSLOT but not FACESLOT. | cosmetic |
| ARM-22 | Fuel can be merged via vanilla repair crafting | `java/techguns/items/armors/ItemTGSpecialSlot.java:21-31` | Special items are damageable with repair left enabled. Two part-used jetpacks, anti-grav packs and so on combine into one with a +5 % durability bonus, which creates free fuel. | gameplay |
| ARM-23 | Anti-grav pack UVs overrun the texture | `java/techguns/client/models/ModelAntiGravPack.java:230-232` | The 6×6×3 box at u = 16 needs 18 columns on a 32-wide texture, so its back faces wrap into the cylinder area. | visual |
| ARM-24 | Mask and goggle models draw the head shell twice | `java/techguns/client/models/ModelFaceMask.java:14-22` (also `ModelGasMask.java:16`, `ModelNightVisionGoggles.java:16`, `ModelNightVisionGogglesOff.java:17`) | The inherited biped head box (inflated by 1/16) plus a child 8×8×8 hood box on the same texture render together, which can z-fight or double-draw. | visual |
| ARM-25 | Debug offsets in a production renderer | `java/techguns/client/renderer/additionalslots/RenderAkimboMibGun.java:17` | The off-hand MIB gun translation adds the debug keybind X/Y/Z values. These are normally 0, but the debug keys can move it when enabled. | cosmetic |
| ARM-26 | 3-D icons ignore camo and have no flat icon | `java/techguns/client/ClientProxy.java:542-554` | Steam and Power armour items always render the default-camo texture in inventories. There are no `textures/items` icons for them, so the icon registration logs missing textures. | visual |
| ARM-27 | T2 Commando set cannot be repaired | `java/techguns/TGArmors.java:149-152` | No `setRepairMats` call, so the Repair Bench rejects it and the anvil repair item is null. It may be intentional, but every other set has repair mats. | gameplay |
| ARM-28 | NBT created on read and in tooltips | `java/techguns/items/armors/PoweredArmor.java:282-288, 402-410` | `getPower` and `addInformation` attach new tags to stacks just by looking at them, including on the client. Stacks can stop matching recipes or stacks that expect no NBT. | gameplay |
| ARM-29 | Night-vision toggle-off strips potion night vision | `java/techguns/packets/PacketTGKeybindPress.java:51-56` | Turning the toggle off removes any Night Vision effect, including one from a vanilla potion. | gameplay |
| ARM-30 | Unchecked cast of face/back items | `java/techguns/items/armors/GenericArmor.java:288,296`; tick handler `TechgunsTickHandler.java:455,462` | Any non-`ITGSpecialSlot` item that reaches slot 0 or 1 (the inventory's `isItemValidForSlot` accepts everything, and NBT edits are possible) throws a ClassCastException every tick. | crash |
| ARM-31 | Client may open any GUI id | `java/techguns/packets/PacketOpenPlayerGUI.java:36-37` (01-core owns it; used by the tab) | The server opens whatever positive id the client sends, at the player's position, with no validation. | gameplay |
| ARM-32 | Dead or unused code and assets | `ModelT3PowerArmorOld.java`, `ModelGasMask2.java`, `ModelBipedTGBackSlot.java`, `RenderGasMask.java`, `RenderAntiGravPack.java`, `RenderArmor.java` (fully commented); `textures/armor/steam_armor_1.png`, `textures/armor/t2_riotArmor.png`, `sounds/items/glider_loop.ogg`; `TGArmorBonus.WATER_ELECTROLYZER`; `TGSlotType.FOODSLOT`; jetpack hover/forward code | Never referenced or never reachable. | cosmetic |
| ARM-33 | Repair totals are inconsistent | `java/techguns/TGArmors.java:129,186` | T1 Miner leggings need 2 repair mats where every other leggings needs 3. The Exo chest uses copper wire instead of carbon fibres. Possibly typos. | cosmetic |
