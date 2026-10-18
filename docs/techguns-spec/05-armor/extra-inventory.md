# The Techguns player inventory tab

Source: `java/techguns/gui/playerinventory/` (`TGPlayerInventory`, `TGPlayerContainer`, `GuiTGPlayerInventory`, `SlotTG`, `SlotFood`, `TGGuiTabButton`, `TGGuiToggleButton`), `java/techguns/items/TGSlotType.java`, `java/techguns/inventory/SlotArmor.java`.

Wiring that lives in other areas (see 01-core): `TechgunsExtendedPlayerProperties` (owns the inventory, saves and loads it, drops it on death, DataWatcher IDs), `TechgunsGuiHandler` (GUI ID 17), `PacketOpenPlayerGUI`, the `InitGuiEvent` hook that adds the tabs to the vanilla inventory, and the auto-feeder in the tick handler.

## 1. Data model: `TGPlayerInventory` (`IInventory`, 12 slots)

| Index | Constant | Purpose | Accepts |
|---|---|---|---|
| 0 | `SLOT_FACE` | face gear | items whose `ITGSpecialSlot.getSlot` returns `FACESLOT` |
| 1 | `SLOT_BACK` | back gear | `BACKSLOT` items |
| 2–4 | `SLOTS_AUTOFOOD_START`..`END` | auto-feeder food | any `ItemFood` |
| 5 | `SLOT_AUTOHEAL` | "auto heal" | `HEALSLOT` items, but **no item reports HEALSLOT**, so the slot can never be filled (bug ARM-17) |
| 6–11 | `SLOTS_AMMO_START`..`END` | ammo pouch | `AMMOSLOT` items (magazines, bullets, tanks, energy cells and the like from the shared item) |

**Inventory behaviour:**
- Name `TechgunsPlayerInventory`. Stack limit 64.
- `isItemValidForSlot` always returns true; the restrictions live in the GUI slots.
- `isUseableByPlayer` is always true.
- Writing to slot 0 or 1 via `setInventorySlotContents` also updates the player's DataWatcher entry.

**Persistence:** saved inside the player's extended-properties NBT as a tag list named `TechgunsPlayerInventory`. Each entry is the item-stack compound plus a byte `Slot`. On load, the inventory is rebuilt from scratch and indices outside 0–11 are ignored.

**Multiplayer visibility:**
- Face and back stacks are mirrored into two DataWatcher ItemStack entries (type 5), so other clients can render them.
- IDs come from the config: `DataWatcherID_FaceSlot` (default 23) and `DataWatcherID_BackSlot` (default 24), category "ID Conflicts", range 2–31.
- The server rewrites both entries every END-phase player tick.

**Death:** if `keepInventory` is off, all 12 slots drop with the player's drops. On respawn or dimension change (clone), the whole extended-properties NBT is copied across (01-core).

**Who reads it:**
- Bonus aggregation reads the face and back slots.
- Ammo consumption and reloads check slots 6–11 before the main inventory.
- The auto-feeder eats from slots 2–4 when food ≤ 19, unless the config `disableAutofeeder` is set (01-core).
- Ammo-type shared items quick-insert into slots 6–11 on right-click (06-items).
- Special-slot items quick-equip into face or back on right-click (special-equipment.md).

## 2. `TGSlotType` enum

| Constant | `toString` (shown in item tooltips as `Slot: …`) | Used for |
|---|---|---|
| `NORMAL` | "Normal Item" | default for shared items |
| `FACESLOT` | "Face" | TG inventory slot 0 |
| `BACKSLOT` | "Back" | TG inventory slot 1 |
| `AMMOSLOT` | "Ammo" | TG inventory slots 6–11 |
| `FOODSLOT` | "Food" | declared, unused (the food slots use `SlotFood` instead) |
| `HEALSLOT` | "Healing" | slot 5 (nothing provides it) |
| `TURRETARMOR` | "Turret armor" | turret base slot (09-machines-utility) |
| `REACTION_CHAMBER_FOCUS` | "Reaction Chamber Focus" | 08-machines-production |
| `DRILL_SMALL` / `DRILL_MEDIUM` / `DRILL_LARGE` | "Small Drill" / "Medium Drill" / "Large Drill" | ore drill (08) |

## 3. Slot classes

- **`SlotTG(inv, index, x, y, type)`:**
  - An `ITGSpecialSlot` item is accepted iff its `getSlot(stack)` equals the slot's type.
  - Any other item is accepted only if the slot type is `NORMAL`.
  - It is also used by machine GUIs.
- **`SlotFood`:** accepts `ItemFood` only.
- **`SlotArmor(inv, slotIndex, x, y, armorType, player)`:** a reimplementation of the vanilla armour slot.
  - Stack limit 1.
  - Valid iff `item.isValidArmor(stack, armorType, player)`.
  - Its background icon is the vanilla empty-armour-slot icon for that type.

## 4. Container `TGPlayerContainer`

| Container slot IDs | Contents | Screen position (x, y) relative to the GUI's top-left |
|---|---|---|
| 0 | face `SlotTG(FACESLOT)` | (80, 8) |
| 1 | back `SlotTG(BACKSLOT)` | (80, 26) |
| 2–4 | `SlotFood` | (116, 24), (134, 24), (152, 24) |
| 5 | heal `SlotTG(HEALSLOT)` | (96, 62) |
| 6–11 | ammo `SlotTG(AMMOSLOT)` in 2 rows of 3 | x = 116 + 18·col, y = 44 + 18·row |
| 12–15 | `SlotArmor` (helmet, chest, legs, boots), player inventory indices 39, 38, 37, 36 | x = 8, y = 8 + 18·i |
| 16–42 | main inventory (player indices 9–35) | x = 8 + 18·col, y = 84 + 18·row |
| 43–51 | hotbar (player indices 0–8) | x = 8 + 18·i, y = 142 |

`canInteractWith` is always true.

**Shift-click (`transferStackInSlot`):**
1. **From a TG slot (0–11):** merge into the main inventory plus hotbar (16–51), filling from the end.
2. **Else, if the stack is an `ItemArmor`** and the matching armour slot (12 + armourType) is empty: move it there.
3. **Else, if clicked in the main inventory or hotbar and the item fits some TG slot:** take the *first* TG slot whose `isItemValid` accepts the item and merge into a range starting there. The range is 6 slots for ammo, 3 for food, 1 otherwise.
4. **Else, main inventory (16–42):** move to the hotbar (43–51).
5. **Else, hotbar:** move to the main inventory.
6. **Otherwise** (for example from an armour slot): merge into 16–51.

## 5. GUI `GuiTGPlayerInventory`

Texture: `textures/gui/TGPlayerInventory.png` (256×256). The GUI is 176×166, the vanilla size.

**Background:** the region (0,0)–(176,166) of the texture is drawn at the GUI origin. It contains:
- a black 3-D player viewport at about (26,8)–(78,78)
- the armour slot column at x 7–25
- face and back slot frames to the right of the viewport
- a shield icon area at (99,7) 14×14
- a radiation bar trough at (115,7) 54×6
- the 3 food slots, 1 heal slot and 2×3 ammo slots
- the standard 3×9 inventory and hotbar frames
- a light-grey panel with bevelled white/dark edges

On top of the background, the player is drawn 3-D at (51, 75) relative to the GUI, scale 30, looking at the mouse (it reuses `GuiCamoBench.renderPlayer`).

**Hover tooltips** (foreground layer, coordinates relative to the GUI):

| Area | Tooltip lines |
|---|---|
| Toggle button i at (173, 7 + 11·i), 11×11 | (1) toggle name; (2) `Current state: ` + red `Disabled` or green `Enabled`; (3)–(4) the two description lines (table below) |
| (99,7) 14×14 shield | underlined `Total Armor Values`, then nine lines `<DamageType name>: <formatAV(total)>` coloured as in the armour tooltip (PHYSICAL dark grey, PROJECTILE grey, EXPLOSION dark red, ENERGY dark aqua, FIRE red, ICE aqua, LIGHTNING yellow, POISON dark green, RADIATION green). Totals come from `DamageSystem.getTotalArmorAgainstType`: the sum over worn armour, where vanilla armour counts only toward PHYSICAL. |
| (115,7) 54×6 radiation bar | green underlined `Current Radioactive Contamination`; `<radlevel×0.001>/1000`; `Effects: ` + green `NONE` (hard-coded). The radiation system belongs to 01-core. |

| Toggle index | Player flag | Name key / text | Description keys / text |
|---|---|---|---|
| 0 | `showTGHudElements` | `techguns.tgguitooltip.togglehud` "Toggle HUD Icons" | "Shows icons for current" / " states in the players HUD" |
| 1 | `enableNightVision` | `…togglenv` "Toggle Nightvision" | "Enables nightvision if" / " provided by armor" |
| 2 | `enableSafemode` | `…togglesafe` "Toggle Safe Mode" | "In safe mode guns don't" / " destroy or ignite blocks." |
| 3 | `enableStepAssist` | `…togglestep` "Toggle Step Assist" | "Allows to step up a whole" / " block if provided by armor" |
| 4 | `enableJetpack` | `…togglejetpack` "Toggle Jetpack" | "Enables your Jetpack" / " when you jump" |

**Toggle buttons (`TGGuiToggleButton`, 11×11 at (173, 7 + 11·i)):**
- Drawn with the vanilla button texture (`widgets.png`, continuous box) in its hover state.
- A 7×7 state icon is drawn at +2,+2 from the TG texture. Source u = 242 when off and 249 when on; v = 7 × index (0, 7, 14, 21) for indices 0–3 and **42** for index 4.
- Clicking sends `PacketTGKeybindPress` with button ID 9 (HUD), 0 (NV), 1 (safe mode), 8 (step) or 7 (jetpack). The server flips the flag and syncs it back. The button reads the flags from a statically cached properties object (bug ARM-19).

**Tab buttons (`TGGuiTabButton`, 26×26):**
- Without Tinkers' Construct, two tabs are placed at (guiLeft − 26, guiTop + 5) and (guiLeft − 26, guiTop + 31):
  - Tab 1: vanilla inventory, crafting-table icon, GUI id 0.
  - Tab 2: Techguns inventory, 9 mm bullets icon, GUI id = config `TechgunsGUI_TabID` (default **17**, category "ID Conflicts", range 0–1000).
- The tab for the current screen is disabled.
  - The vanilla `GuiInventory` gets the same two tabs through an `InitGuiEvent.Post` hook, with tab 1 disabled.
  - With Tinkers' Construct loaded, the tabs are added through TConstruct's tab API instead (`TechgunsTConstructIntegrationClient`, 01-core).
- **Sprite:** from the TG texture at (177, 26·i), where i = 1 for an enabled (clickable) tab and 0 for the current one. A disabled tab also draws a 3×26 connector strip from (213, 0) at its right edge. The icon item is rendered at +6,+5. The click sound is `gui.button.press`, played only if enabled.
- **Click:**
  - GUI id ≤ 0: close the container (send a vanilla close-window packet) and open the vanilla `GuiInventory`.
  - Otherwise: send `PacketOpenPlayerGUI(short id)`. The server opens that GUI id at the player's position, and the GUI handler maps ID 17 to the TG container and GUI.

## 6. GUI texture `textures/gui/TGPlayerInventory.png` (256×256)

Layout, so that a replacement keeps the same coordinates:

| Region (x, y, w, h) | Content |
|---|---|
| (0, 0, 176, 166) | Main panel as described in §5. Light grey #C6C6C6-style panel, black viewport, slot wells in mid grey with dark top-left and white bottom-right bevels (vanilla style). |
| (177, 0, 26, 26) + (213, 0, 3, 26) | Tab sprite, "current tab" variant: a light-grey (#C6C6C6) tab with a white top/left bevel and a black outline that is open on the right. The extra 3 px column joins the tab into the panel edge. |
| (177, 26, 26, 26) | Tab sprite, "other tab" variant: the same shape in a darker grey (#8B8B8B) face, closed on the right. |
| (242, 0) / (249, 0) | HUD toggle icon, off / on: red X / green check mark. |
| (242, 7) / (249, 7) | Night-vision icon, off / on: dark-grey goggles / green goggles on a green background. |
| (242, 14) / (249, 14) | Safe-mode icon, off / on: red block "S" in a grey frame / green "S". |
| (242, 21) / (249, 21) | Step-assist icon, off / on: red stair step / green stair step. |
| (242, 28) / (249, 28) | Yellow "!" and red "!" warning tiles. Not used by the toggles; the HUD overlay uses them (01-core / 03-gun-visuals-audio). |
| (249, 35) | Grey gas-canister glyph. Not used by the toggles (HUD overlay). |
| (242, 42) / (249, 42) | Jetpack icon, off / on: grey twin-tank jetpack with red X / with yellow-red flames under the nozzles. |
| (0, 168, 24, 8) | Three 8×8 tiles (black border with four black "dots"): fully grey, half grey/half empty, and empty. The HUD overlay uses them as a fill indicator (01-core / 03). |

The rest of the texture is transparent (shown magenta in the editor).
