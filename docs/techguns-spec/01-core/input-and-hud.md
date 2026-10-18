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
| `key.reloadFX` | Numpad Enter | Chat "Reloading FXList...", reloads [`particles/FXList.txt`](../04-projectiles-fx/fx-catalog.md) ([04](../04-projectiles-fx/index.md)), then "Done!" |
| `key.killAllEntityFX` | Numpad , | Chat "Removing all Particles...", kills all FX particles, then "Done!" |

`debug/Uuidgen.java` is a standalone `main` that prints a random UUID. It is a developer tool and is not used at runtime.

## Firing guns with the left mouse button

Guns can be "shoot with left click". When they are, vanilla attack and mining are suppressed and Techguns runs its own fire loop:

1. **MouseEvent** (Forge bus, client, priority HIGH): for button 0 while the game has focus and the player holds a `GenericGun`:
   - If the gun shoots with left click: store the button state (down or up) in [`ClientProxy.keyFirePressed`](lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) and **cancel** the event, so vanilla never sees the click.
   - Otherwise (a melee or mining type gun such as the chainsaw): if a reload is still in progress (reload end time in the future), cancel the press so you can't attack or mine while reloading.
2. **BreakSpeed** (priority HIGHEST): while holding a left-click gun, the break speed becomes −1 and the event is canceled, so the player can't mine.
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
| 1 | Chest armor is a [`PoweredArmor`](../05-armor/armor-catalog.md#9-power-system-poweredarmor) | (249, 35) battery | `§e<batteries>x§f<power%>%`, where batteries = count of that armor's battery item in main inventory and ammo slots, and power% = stored ÷ max power. Drawn at x = W + 14 − len×6 |
| 2 | Back slot holds a jetpack, jump pack or anti-grav pack | (242, 42) jetpack off / (249, 42) jetpack on (shows `enableJetpack`) | `§e<fuel items>x§f<charge%>%`, where charge% = 1 − displayDamage ÷ maxDamage of the pack, and fuel items = count of the pack's ammo item. Same x formula |
| 3 | `proxy.hasNightvision` (armor provides NV) | (242, 7) off / (249, 7) on, per `enableNightVision` | — |
| 4 | `proxy.hasStepassist` (armor provides step assist) | (242, 21) off / (249, 21) on, per `enableStepAssist` | — |
| 5 | Armor durability warning | (242, 28) yellow "!" when any worn piece is under 35 % durability; (249, 28) red "!" when a piece is broken (damage ≥ max − 1) | — |

For row 5 the pieces are checked in slot order. A piece under 35 % sets "warning" and skips the broken check for that same piece. A single broken piece therefore only shows yellow; red appears only if a *later* piece is broken ([CORE-17](index.md#suspected-bugs)).

### Armor bar replacement (`RenderGameOverlayEvent.Pre`, ARMOR)

When `showTGHudElements` is on, the vanilla armor bar is canceled and replaced by a bar that can show up to **24** armor points:
- Armor level = Forge's total armor value.
- Start position: x = W/2 − 93, y = H − 48.
- 12 icons, 8×8, spaced 7 px apart (they overlap by 1 px). They are drawn only if level > 0.
- For i = 1, 3, 5, …, 23: draw a full icon (0, 168) if i < level, a half icon (8, 168) if i = level, and an empty icon (16, 168) if i > level.
- Blending is enabled while drawing. Afterward the vanilla `textures/gui/icons.png` is bound again.

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
| (0, 0, 176, 166) | Main panel of the Techguns player inventory, in the vanilla light-gray beveled style. From top-left: a column of 4 armor slots; a black player-preview window (~52×72); a column of 2 slots right of the preview (face and back); a small gray shield (armor-value) icon; a thin horizontal graduated gauge strip with tick marks at the top right; a 3×3 slot grid on the right (3 food + 6 ammo slots) with one single slot to its left on the bottom row (the heal slot); then the standard 3×9 inventory and 1×9 hotbar | GuiTGPlayerInventory (the GUI's slot logic belongs to the player inventory GUI) |
| (177, 0, 26, 26) | Tab background, light gray (current tab) | TGGuiTabButton (disabled) |
| (177, 26, 26, 26) | Tab background, darker gray (other tab) | TGGuiTabButton (enabled) |
| (213, 0, 3, 26) | Connector strip for the current tab | TGGuiTabButton |
| (242, 0, 7, 7) / (249, 0, 7, 7) | Red X (off) / green check mark (on) | on/off toggle buttons in the Techguns inventory |
| (242, 7) / (249, 7), 7×5 | Goggles: dark gray (off) / green (on) | NV HUD row |
| (242, 14) / (249, 14) | Red "!" shield (unsafe) / green "S" (safe mode) | safe-mode HUD icon |
| (242, 21) / (249, 21), 7×6 | Step-assist "stairs and arrow": red/gray (off) / green (on) | step-assist HUD row |
| (242, 28) / (249, 28) | Yellow "!" panel (low durability) / red "!" panel (broken) | durability warning |
| (249, 35) | Gray battery (242, 35 is empty) | powered-armor row |
| (242, 42) / (249, 42) | Jetpack with red crosses (disabled) / with yellow flames (enabled) | jetpack row |
| (0, 168, 8, 8) / (8, 168) / (16, 168) | Armor point: full (gray gradient plate with 4 dark rivets in a black frame) / half (left half gray, right half transparent) / empty (transparent interior, frame and rivets only) | armor bar replacement |

Palette: the vanilla GUI grays (#C6C6C6 panel, #8B8B8B slots, #373737 and #FFFFFF bevels), black (#000000) outlines, signal red #FF0000, greens #00C60E / #00FF12 / #1EFF00 / #006607, yellow #FFFF00, dark gray #353535 and mid gray #727272.

## Keybind entity sheets (consistency pass)

### Behavior shared by all five Techguns keys

| Aspect | Detail |
|---|---|
| Registration | `TGKeybinds.init` builds five `KeyBinding` objects (name = lang key, default LWJGL key, category `techguns.key.categories.techguns`) and registers them with `ClientRegistry`. `ClientProxy` (line 1297) registers the `TGKeybinds` instance on the FML bus |
| Held versus pressed | Every key uses `KeyBinding.isPressed()`: it is edge-triggered and consumes one queued press. Holding the key does nothing more; one physical press produces one packet. There is no hold, repeat or release handling for any Techguns key |
| Chain | One `else if` chain in the order night vision, safe mode, jetpack, step assist, force reload. A press that is not reached in one event stays queued and is handled on the next key event |
| Prediction | None for the four toggles: the server flips the flag and replies with a full sync, so the HUD and GUI change one round trip later. Force reload predicts locally |
| Feedback | No chat message. State shows in the HUD icon column and in the Techguns inventory toggle buttons. Night vision plays a sound (below) |
| Works in GUIs | The Techguns handler has no screen check. Whether FML posts `KeyInputEvent` while a GUI is open is Forge behavior that cannot be verified from the Techguns source |

### Entity sheets

| Field | Night vision | Safe mode | Jetpack | Step assist | Force reload |
|---|---|---|---|---|---|
| Static field | `KEY_TOGGLE_NIGHTVISION` | `KEY_TOGGLE_SAFEMODE` | `KEY_TOGGLE_JETPACK` | `KEY_TOGGLE_STEPASSIST` | `KEY_FORCE_RELOAD` |
| Lang key | `techguns.key.toggleNightvision` | `techguns.key.toggleSafemode` | `techguns.key.toggleJetpack` | `techguns.key.toggleStepassist` | `techguns.key.forceReload` |
| English text (en_US.lang line) | Toggle Nightvision (407) | Toggle Safe Mode (408) | Toggle Jetpack (410) | Toggle Step Assist (409) | Force Reload (411) |
| Default key (LWJGL constant) | N (`KEY_N`) | B (`KEY_B`) | J (`KEY_J`) | V (`KEY_V`) | R (`KEY_R`) |
| Category / English | `techguns.key.categories.techguns` = Techguns (line 406) | same | same | same | same |
| Packet sent | [`PacketTGKeybindPress`](networking.md#7--packettgkeybindpress) id 0 | id 1 | id 7 | id 8 | id 6 |
| Client-side condition | none | none | none | none | main-hand item is a `GenericGun`, extended properties exist, `fireDelay <= 0`, gun not fully loaded; then also calls `tryForcedReload` locally |
| Server effect | Flip `enableNightVision`, remove any Night Vision potion when turning off, play `nightvision.off` (when removing) or `nightvision.on` (turning on with NV armor), full sync | Flip `enableSafemode`; revert to on if [`ForceSafeModeForNonOps`](config.md#category-general) forbids it; full sync | Flip `enableJetpack` only while the jump flag is false; send field-sync 1 | Flip `enableStepAssist`; full sync | `tryForcedReload` on the held gun, no `fireDelay` or full-clip check on the server side of the packet |
| Also reachable from | Toggle button index 1 in the Techguns inventory GUI | Toggle button index 2 | Toggle button index 4 | Toggle button index 3 | none |
| Sound | `techguns:nightvision.on` / `.off`, vol 1.0, pitch 1.0 (see [textures-sounds-lang.md](textures-sounds-lang.md#sounds)) | none | none | none | whatever the reload plays ([02](../02-guns/index.md)) |
| Bugs | [CORE-15](index.md#suspected-bugs) | none | none | [CORE-14](index.md#suspected-bugs) (related) | none |

Toggle button index 0 (HUD icons, id 9) has no key. The HUD toggle can only be changed in the Techguns inventory GUI.

### Other input paths that act like keybinds

| Input | Mechanism | Held or pressed | Packet or effect |
|---|---|---|---|
| Left mouse button with a left-click gun | `MouseEvent` (HIGH) stores the button state; `PlayerTickEvent` START reads it | Held. Auto guns repeat every tick that `fireDelay <= 0`; semi-auto guns clear the flag after one attempt | [`PacketShootGun`](networking.md#8--packetshootgun) (id 8 in the packet list) every firing tick |
| Jump key with a jetpack or jump pack | [`ItemJetpack`](../05-armor/special-equipment.md#43-jetpack-itemjetpack) slot tick compares the live jump-key state with the stored flag | Edge-detected: rising edge sends `JETPACK_BOOST_START` (2), falling edge sends `JETPACK_BOOST_STOP` (3). Only evaluated while `enableJetpack` is on. Stop is also sent when the pack is empty (item damage at maximum) and the flag is still set | `PacketTGKeybindPress` |
| Forward key | Commented out in `ItemJetpack` | not active | ids 4 and 5 unused |
| Inventory tab and toggle buttons | `TGGuiTabButton`, [`TGGuiToggleButton`](../05-armor/extra-inventory.md) mouse presses | Click | [`PacketOpenPlayerGUI`](networking.md#4--packetopenplayergui), `PacketTGKeybindPress` |

### Debug keybinds (sheet)

Registered by `debug.Keybinds.init` only when [`enableDebugKeybinds`](config.md#category-debug) is true (`ClientProxy` line 1292). Handler: `debug.KeyInputHandler.onKeyInput` on the FML bus, with independent `if` blocks (not an `else if` chain), so several can fire in one event. All use `isPressed()` (edge-triggered). Step is 0.1. None of the names or the category have lang entries ([CORE-29](index.md#suspected-bugs)), so Forge's controls screen shows the raw key strings.

| Field | Lang key (untranslated) | Default key | Action | Chat output (hard-coded English) |
|---|---|---|---|---|
| `plusX` | `key.+x` | Numpad 7 | X += 0.1 | `X:<value>` |
| `minusX` | `key.-x` | Numpad 8 | X -= 0.1 | `X:<value>` |
| `plusY` | `key.+y` | Numpad 4 | Y += 0.1 | `Y:<value>` |
| `minusY` | `key.-y` | Numpad 5 | Y -= 0.1 | `Y:<value>` |
| `plusZ` | `key.+z` | Numpad 1 | Z += 0.1 | `Z:<value>` |
| `minusZ` | `key.-z` | Numpad 2 | Z -= 0.1 | `Z:<value>` |
| `showXYZ` | `key.show` | Numpad 0 | print all three | `XYZ: x, y, z` |
| `reloadFX` | `key.reloadFX` | Numpad Enter | reload the FX definition file | `Reloading FXList...` then `Done!` |
| `killAllEntityFX` | `key.killAllEntityFX` | Numpad , (comma) | remove every live FX particle | `Removing all Particles...` then `Done!` |

Category for all nine: `key.categories.techguns` (no English text). Packets sent: none; everything is client-local.

## `TGPlayerInventory.png` pixel regions (measured, consistency pass)

Measured from the file itself. The texture is 256x256; opaque content is confined to (0, 0)-(216, 52) plus the icon strip at x 242-255 (y 0-49) and the armor-point tiles at (0, 168, 24, 8). The earlier table above remains valid; this section gives the full rectangle list, including each slot, with the container coordinates that [`TGPlayerContainer`](../05-armor/extra-inventory.md#4-container-tgplayercontainer) and the vanilla-style frames use. A slot's 16x16 well sits inside an 18x18 beveled frame, so the frame starts one pixel up and left of the listed well. Slot role and item filtering belong to [05-armor](../05-armor/extra-inventory.md).

### Main panel (0, 0, 176, 166)

| Rectangle (x, y, w, h) | Role | Notes |
|---|---|---|
| (0, 0, 176, 166) | Panel background | Light gray #C6C6C6 face with white and dark bevel edges |
| (26, 8, 52, 70) | Player-preview viewport | Pure black #000000, fully enclosed; the live 3-D player is drawn over it |
| (8, 8, 16, 16) | Armor slot 0 (helmet) | Slot well color #8B8B8B |
| (8, 26, 16, 16) | Armor slot 1 (chestplate) | |
| (8, 44, 16, 16) | Armor slot 2 (leggings) | |
| (8, 62, 16, 16) | Armor slot 3 (boots) | |
| (80, 8, 16, 16) | Face slot (Techguns slot 0) | |
| (80, 26, 16, 16) | Back slot (Techguns slot 1) | |
| (96, 62, 16, 16) | Heal slot (Techguns slot 5) | The only slot not in the 3x3 grid |
| (116, 24, 16, 16), (134, 24, 16, 16), (152, 24, 16, 16) | Food slots (Techguns slots 2-4) | Top row of the 3x3 grid |
| (116, 44, 16, 16), (134, 44, 16, 16), (152, 44, 16, 16) | Ammo slots 6-8 | Middle row |
| (116, 62, 16, 16), (134, 62, 16, 16), (152, 62, 16, 16) | Ammo slots 9-11 | Bottom row |
| (99, 7, 14, 14) | Total-armor-values icon (hover area) | Small beveled shield well; hover tooltip lists the nine damage types ([05-armor](../05-armor/extra-inventory.md#5-gui-guitgplayerinventory)) |
| (115, 7, 54, 6) | Radiation gauge trough | Measured fill of the strip (116, 8, 52, 4) in slot-well gray with a darker top-left and white bottom-right bevel; the bar shows `radlevel / 1000` ([CORE-42](index.md#suspected-bugs)) |
| (8, 84) to (152, 120), 9 columns by 3 rows, pitch 18, each 16x16 | Main inventory (27 slots) | Standard vanilla layout |
| (8, 142) to (152, 142), 9 slots, pitch 18, each 16x16 | Hotbar | |

### Tab sprites

| Rectangle | Role | Colors (measured) |
|---|---|---|
| (177, 0, 26, 26) | Tab, current (disabled) variant | face #C6C6C6 (460 px), white bevel (90 px), black outline (76 px) |
| (177, 26, 26, 26) | Tab, other (enabled) variant | face #8B8B8B (460 px), same bevel and outline counts |
| (213, 0, 3, 26) | Connector strip that joins the current tab to the panel | #C6C6C6 with white pixels; drawn only for the disabled tab |

### HUD and toggle icon sheet (7x7 cells at u = 242 off / 249 on)

| v | Cell | Role | Measured content |
|---|---|---|---|
| 0 | (242, 0) / (249, 0) | HUD toggle: off / on | off: red X, 9 px of #FF0000 inside (243, 1, 5, 5); on: green check mark (#00C60E) with a black and white edge, bounding box (250, 0, 6, 7) |
| 7 | (242, 7) / (249, 7) | Night vision: off / on | off: dark-gray #353535 goggles, bounding box (243, 9, 5, 2); on: green goggles (#00C60E and #006607), bounding box (249, 8, 7, 4) |
| 14 | (242, 14) / (249, 14) | Safe mode: unsafe / safe | full 7x7 cell. Unsafe: #353535 frame, red #FF0000 glyph, black detail. Safe: #353535 frame, green #00FF12 glyph |
| 21 | (242, 21) / (249, 21) | Step assist: off / on | 7x6 cell. Off: #353535 with red #FF0000 steps; on: #353535 with green #1EFF00 and #00C60E steps |
| 28 | (242, 28) / (249, 28) | Durability warning: yellow / red | full 7x7 cell, #FFFF00 (41 px) or #FF0000 (41 px) with #727272 and black corner pixels |
| 35 | (242, 35) empty / (249, 35) | Powered-armor battery | (249, 35) glyph occupies (250, 35, 5, 7): black outline (19 px) and #727272 body (12 px). The cell at (242, 35) is fully transparent |
| 42 | (242, 42) / (249, 42) | Jetpack: disabled / enabled | full 7x7 cell. Disabled: black and #727272 pack with red #FF0000 crosses; enabled: same pack with red flame pixels |
| 49 | (242, 49), (249, 49) | unused | fully transparent |

The toggle buttons in the inventory GUI read rows v = 0, 7, 14, 21 and 42 (index 4 skips to 42); the HUD overlay additionally reads v = 28 and 35. Sizes for the goggles (7x5) and step-assist (7x6) in the older table are approximate: the opaque bounding boxes are 7x4 (on goggles) and 5x2 (off goggles).

### Armor-point tiles

| Rectangle | Role | Measured content |
|---|---|---|
| (0, 168, 8, 8) | Full armor point | 64 opaque px; 32 black outline px plus gray gradient plate, average #424242 |
| (8, 168, 8, 8) | Half armor point | 48 opaque px, average #262626. Interior columns 1-3 (x 9-11) are filled gray; interior columns 4-6 (x 12-14) are transparent |
| (16, 168, 8, 8) | Empty armor point | 32 opaque px, all black (#000000): only the frame and the four rivets |
| Rivets at tile offsets (2, 2), (5, 2), (2, 5), (5, 5) | Rivet pixels | Four black pixels in every tile. They show up as isolated 1x1 components only where the neighboring interior is transparent: (13, 170), (13, 173) in the half tile and (18, 170), (21, 170), (18, 173), (21, 173) in the empty tile |

Correction: an earlier version of this document called the interior of the half and empty tiles "white". The pixels there are fully transparent (alpha 0); the white came from viewing the PNG on a white backdrop. In game, blending is on while the bar is drawn, so the HUD background shows through.
