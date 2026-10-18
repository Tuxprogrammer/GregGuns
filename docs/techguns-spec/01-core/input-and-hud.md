# Input, HUD and inventory tab

Source: `keybinds/TGKeybinds.java`, [`keybinds/TGKeybindsID.java`](networking.md#7--packettgkeybindpress-cs), `debug/*`, `events/GuiOverlayAmmoCount.java`, and parts of [`events/TechgunsEventhandler.java`](events.md#techgunseventhandler-forge-bus) and [`events/TechgunsTickHandler.java`](events.md#techgunstickhandler-fml-bus).

## Keybinds (`TGKeybinds`, client only)

All five are registered in init (client handlers step), in the category `techguns.key.categories.techguns` ("Techguns"). A single `InputEvent.KeyInputEvent` handler on the FML bus checks them in an if/else-if chain, so at most one Techguns key is handled per input event; the rest stay queued for the next one.

| Field | Lang key | English | Default key | Effect |
|---|---|---|---|---|
| KEY_TOGGLE_NIGHTVISION | `techguns.key.toggleNightvision` | Toggle Nightvision | **N** | Sends `PacketTGKeybindPress(0)` |
| KEY_TOGGLE_SAFEMODE | `techguns.key.toggleSafemode` | Toggle Safe Mode | **B** | Sends id 1 |
| KEY_TOGGLE_JETPACK | `techguns.key.toggleJetpack` | Toggle Jetpack | **J** | Sends id 7 |
| KEY_TOGGLE_STEPASSIST | `techguns.key.toggleStepassist` | Toggle Step Assist | **V** | Sends id 8 |
| KEY_FORCE_RELOAD | `techguns.key.forceReload` | Force Reload | **R** | Only when holding a [`GenericGun`](../02-guns/gun-lifecycle.md), the local `fireDelay` ≤ 0 and the gun is not fully loaded: sends id 6 **and** calls the gun's `tryForcedReload` locally as a prediction |

What the server does with each ID is in [networking.md](networking.md) (packet 7, PacketTGKeybindPress). The other keybind IDs aren't tied to a key binding:
- Jetpack boost start/stop is driven by the vanilla jump key, read by the jetpack and jump-pack items ([05](../05-armor/index.md)).
- TOGGLE_HUD (9) comes from a toggle button in the Techguns inventory GUI.
- Jetpack-forward start/stop (4/5) are never sent.

Safe mode ("In safe mode guns don't destroy or ignite blocks.") starts **on**. Guns check it when deciding whether to damage blocks ([02](../02-guns/index.md)).

### Debug keybinds

These exist only when config [`enableDebugKeybinds`](config.md#category-debug) = true. They tune three global offsets, `debug.Keybinds.X/Y/Z` (doubles, start at 0), that developers used for live positioning. In this build no gameplay code reads X/Y/Z; the reads are commented out. The category `key.categories.techguns` and every name below have **no lang entries**, so they show as raw keys.

| Name | Default key | Effect (prints chat feedback) |
|---|---|---|
| `key.+x` / `key.-x` | Numpad 7 / Numpad 8 | X ± 0.1, then chat "X:<value>" |
| `key.+y` / `key.-y` | Numpad 4 / Numpad 5 | Y ± 0.1 |
| `key.+z` / `key.-z` | Numpad 1 / Numpad 2 | Z ± 0.1 |
| `key.show` | Numpad 0 | Chat "XYZ: x, y, z" |
| `key.reloadFX` | Numpad Enter | Chat "Reloading FXList...", reloads [`particles/FXList.txt`](../04-projectiles-fx/fx-catalogue.md) ([04](../04-projectiles-fx/index.md)), then "Done!" |
| `key.killAllEntityFX` | Numpad , | Chat "Removing all Particles...", kills all FX particles, then "Done!" |

`debug/Uuidgen.java` is a standalone `main` that prints a random UUID. It is a developer tool and is not used at runtime.

## Firing guns with the left mouse button

Guns can be "shoot with left click". When they are, vanilla attack and mining are suppressed and Techguns runs its own fire loop:

1. **MouseEvent** (Forge bus, client, priority HIGH): for button 0 while the game has focus and the player holds a `GenericGun`:
   - If the gun shoots with left click: store the button state (down or up) in [`ClientProxy.keyFirePressed`](lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) and **cancel** the event, so vanilla never sees the click.
   - Otherwise (a melee or mining type gun such as the chainsaw): if a reload is still in progress (reload end time in the future), cancel the press so you can't attack or mine while reloading.
2. **BreakSpeed** (priority HIGHEST): while holding a left-click gun, the break speed becomes −1 and the event is cancelled, so the player can't mine.
3. **PlayerTickEvent START** (client, local player only, game focused):
   - If the held item is a left-click gun and `keyFirePressed` is set, and `fireDelay` ≤ 0: send `PacketShootGun(gun.isZooming())` and call `shootGunLeftClick` locally.
   - For **semi-auto** guns, `keyFirePressed` is then cleared, even when the shot was blocked by `fireDelay`. A click made slightly before the cooldown ends is lost ([CORE-32](index.md#suspected-bugs)).
   - Switching to a non-gun item, or losing focus, clears `keyFirePressed`.
4. `fireDelay` (ticks) is decremented at the start of every player tick on both sides. Guns set it when they fire ([02](../02-guns/index.md)).

## FOV and zoom

### Zoom state (`RenderTickEvent`)

Each render tick, `ClientProxy.player_zoom` is updated from the item in the local player's main hand:

| Held item | Result |
|---|---|
| Not a `GenericGun` | 1.0 |
| Gun with "hold zoom" | the gun's zoom multiplier while sneaking, otherwise 1.0 |
| Other gun | reset to 1.0 only if the gun reports it isn't zooming; otherwise the value set by the gun's toggle-zoom logic ([02](../02-guns/index.md)) stays |

### FOV event (`FOVUpdateEvent`)

`newFov` is multiplied by `player_zoom × f`. When [`LockSpeedDependantFov`](config.md#category-clientside) is off, f = 1. When it is on:
- f = 1 / ((movementSpeedAttribute / walkSpeed + 1) / 2). This undoes the vanilla speed-based widening, including speed from Techguns armor.
- If the local player is sprinting, f is multiplied by a hard-coded **1.2** to keep a fixed sprint widening. The config value [`FixedSprintFovMultiplier`](config.md#category-clientside) (1.15) is never read ([CORE-12](index.md#suspected-bugs)).

## HUD overlay (`GuiOverlayAmmoCount`)

Registered on the Forge bus (client only). All coordinates use the scaled resolution: W = scaled width, H = scaled height. Icons come from [`textures/gui/TGPlayerInventory.png`](../08-machines-production/gui-textures.md#14-texturesguitgplayerinventorypng-owned-by-the-player-inventory-area-0105) (layout [below](#tgplayerinventorypng-layout)). Texts use the vanilla font in white (0xFFFFFFFF), and "§e" (yellow) and "§f" (white) formatting codes appear inside them. Right alignment is computed as `string length × 6` px, and that length includes the two-character formatting codes, so texts end up about 12 px further left than intended ([CORE-18](index.md#suspected-bugs)).

The main overlay is drawn on the **post** phase of the `EXPERIENCE` element. The handler ignores cancelable (pre) events, so it runs once per frame after the XP bar.

### Ammo counter (always shown when a gun is held)

- **Text:** `<ammo left>/<clip size>§ex<magazines>`. Ammo left and clip size come from the gun's tooltip helpers ([02](../02-guns/index.md)).
- **Magazines:**
  1. Count every item stack matching the gun's ammo item (same item and damage) in the 36 main inventory slots, plus Techguns ammo slots 6–11.
  2. Divide by the gun's "ammo count" (ammo items per reload) when it is above 1.
  3. Halve the result for akimbo guns.
- **Position:** x = W + 1 − len×6, y = H − FONT_HEIGHT − 2 (the bottom-right corner).
- **Safe-mode icon:** if HUD elements are enabled, a 7×7 icon at (W − 10, H − 20). It uses u = 242 (unsafe, red "!") or u = 249 (safe, green "S"), at v = 14.

### Status column (when `showTGHudElements` is on)

Starts at y = H − 30. Each shown row draws a 7×7 icon at x = W − 10, and then y moves up 10 px:

| Order | Condition | Icon (u, v) | Text |
|---|---|---|---|
| 1 | Chest armor is a [`PoweredArmor`](../05-armor/armor-catalogue.md#9-power-system-poweredarmor) | (249, 35) battery | `§e<batteries>x§f<power%>%`, where batteries = count of that armor's battery item in main inventory and ammo slots, and power% = stored ÷ max power. Drawn at x = W + 14 − len×6 |
| 2 | Back slot holds a jetpack, jump pack or anti-grav pack | (242, 42) jetpack off / (249, 42) jetpack on (shows `enableJetpack`) | `§e<fuel items>x§f<charge%>%`, where charge% = 1 − displayDamage ÷ maxDamage of the pack, and fuel items = count of the pack's ammo item. Same x formula |
| 3 | `proxy.hasNightvision` (armor provides NV) | (242, 7) off / (249, 7) on, per `enableNightVision` | — |
| 4 | `proxy.hasStepassist` (armor provides step assist) | (242, 21) off / (249, 21) on, per `enableStepAssist` | — |
| 5 | Armor durability warning | (242, 28) yellow "!" when any worn piece is under 35 % durability; (249, 28) red "!" when a piece is broken (damage ≥ max − 1) | — |

For row 5 the pieces are checked in slot order. A piece under 35 % sets "warning" and skips the broken check for that same piece. A single broken piece therefore only shows yellow; red appears only if a *later* piece is broken ([CORE-17](index.md#suspected-bugs)).

### Armor bar replacement (`RenderGameOverlayEvent.Pre`, ARMOR)

When `showTGHudElements` is on, the vanilla armor bar is cancelled and replaced by a bar that can show up to **24** armor points:
- Armor level = Forge's total armor value.
- Start position: x = W/2 − 93, y = H − 48.
- 12 icons, 8×8, spaced 7 px apart (they overlap by 1 px). They are drawn only if level > 0.
- For i = 1, 3, 5, …, 23: draw a full icon (0, 168) if i < level, a half icon (8, 168) if i = level, and an empty icon (16, 168) if i > level.
- Blending is enabled while drawing. Afterwards the vanilla `textures/gui/icons.png` is bound again.

The handler does not advance Forge's `left_height` ([CORE-33](index.md#suspected-bugs)).

## Inventory tab buttons

When **TConstruct is not loaded**, opening the vanilla `GuiInventory` adds two [`TGGuiTabButton`](../05-armor/extra-inventory.md)s (`InitGuiEvent.Post`). Both use button ID 2. With k = (screenW − 176)/2 and l = (screenH − 166)/2:

| Button | Position | State | Icon | Action |
|---|---|---|---|---|
| Vanilla inventory | (k − 26, l + 5) | disabled (this is the current tab) | crafting table | none |
| Techguns inventory | (k − 26, l + 31) | enabled | 9 mm bullets item | sends `PacketOpenPlayerGUI(TechgunsGUI_TabID)` |

The Techguns inventory GUI itself adds the mirror pair: the vanilla tab enabled (it closes the container and opens `GuiInventory` client-side), and its own tab disabled. With TConstruct present, Techguns registers a TConstruct tab instead ([integrations.md](integrations.md#tinkers-construct-tconstruct)).

`TGGuiTabButton` is 26×26:
- Background: (177, 0) when disabled/current, (177, 26) when enabled.
- A disabled tab also gets a 3-px connector strip from (213, 0) drawn just right of the tab, joining it to the GUI frame.
- The item icon is rendered at offset (+6, +5).
- Clicking plays `gui.button.press` (vanilla), only if the button is enabled.

## `TGPlayerInventory.png` layout

`textures/gui/TGPlayerInventory.png`, 256×256, not animated. Used by the HUD overlay, the armor bar, the tab buttons and the Techguns player inventory GUI (GUI ID 17). The content occupies the top 176 rows; everything else is transparent.

| Region (x, y, w, h) | Content | Used by |
|---|---|---|
| (0, 0, 176, 166) | Main panel of the Techguns player inventory, in the vanilla light-grey bevelled style. From top-left: a column of 4 armor slots; a black player-preview window (~52×72); a column of 2 slots right of the preview (face and back); a small grey shield (armor-value) icon; a thin horizontal graduated gauge strip with tick marks at the top right; a 3×3 slot grid on the right (3 food + 6 ammo slots) with one single slot to its left on the bottom row (the heal slot); then the standard 3×9 inventory and 1×9 hotbar | GuiTGPlayerInventory (the GUI's slot logic belongs to the player inventory GUI) |
| (177, 0, 26, 26) | Tab background, light grey (current tab) | TGGuiTabButton (disabled) |
| (177, 26, 26, 26) | Tab background, darker grey (other tab) | TGGuiTabButton (enabled) |
| (213, 0, 3, 26) | Connector strip for the current tab | TGGuiTabButton |
| (242, 0, 7, 7) / (249, 0, 7, 7) | Red X (off) / green check mark (on) | on/off toggle buttons in the Techguns inventory |
| (242, 7) / (249, 7), 7×5 | Goggles: dark grey (off) / green (on) | NV HUD row |
| (242, 14) / (249, 14) | Red "!" shield (unsafe) / green "S" (safe mode) | safe-mode HUD icon |
| (242, 21) / (249, 21), 7×6 | Step-assist "stairs and arrow": red/grey (off) / green (on) | step-assist HUD row |
| (242, 28) / (249, 28) | Yellow "!" panel (low durability) / red "!" panel (broken) | durability warning |
| (249, 35) | Grey battery (242, 35 is empty) | powered-armor row |
| (242, 42) / (249, 42) | Jetpack with red crosses (disabled) / with yellow flames (enabled) | jetpack row |
| (0, 168, 8, 8) / (8, 168) / (16, 168) | Armor point: full (grey gradient plate with 4 dark rivets in a black frame) / half (left half grey, right half white) / empty (white plate with rivets) | armor bar replacement |

Palette: the vanilla GUI greys (#C6C6C6 panel, #8B8B8B slots, #373737 and #FFFFFF bevels), black (#000000) outlines, signal red #FF0000, greens #00C60E / #00FF12 / #1EFF00 / #006607, yellow #FFFF00, dark grey #353535 and mid grey #727272.
