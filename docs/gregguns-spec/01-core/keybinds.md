# Keybinds

- **Techguns baseline:** [input-and-hud.md § Keybinds](../../techguns-spec/01-core/input-and-hud.md#keybinds-tgkeybinds-client-only), [packet 7 `PacketTGKeybindPress`](../../techguns-spec/01-core/networking.md#7--packettgkeybindpress-cs), [CORE-15](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-29](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-41](../../techguns-spec/01-core/index.md#suspected-bugs)
- **Design records:** [0013 default key map](../../design/0013-default-key-map.md)
- **Pack mods:**
  - GTNHLib (`gtnhlib`, 0.11.52)
  - NotEnoughItems (`NotEnoughItems`, 2.8.155-GTNH)
  - Hodgepodge (`hodgepodge`, 2.7.215)
  - Controlling (`controlling`, 2.1.10; the pack's conflict-search mod, config only)
  - JourneyMap (`journeymap`, 5.2.23; no source, see Open questions)
- **Roadmap:** M0 (input plumbing), then each feature's milestone adds its key

## What changes from Techguns

Techguns registers five `KeyBinding`s and sends a numbered `PacketTGKeybindPress` for each press. GregGuns registers the same five keys through GTNHLib's `SyncedKeybind`, which tells the server both when a key is pressed and whether it is held.

| Techguns | GregGuns | Why |
|---|---|---|
| Five `KeyBinding`s registered by hand, one `if/else-if` chain on `KeyInputEvent` | `SyncedKeybind.createConfigurable(nameKey, categoryKey, keyCode)` per key | It registers the `KeyBinding` in the Controls menu and syncs press and hold state to the server |
| `PacketTGKeybindPress` with an ID byte, and the server trusts it (CORE-41) | A server-side `IKeyPressedListener` per key. The server derives the action from its own state (worn items, config, fuel) | A modified client cannot spam jetpack or night-vision actions |
| Jetpack boost driven by the jump key plus two packets (IDs 2 and 3) | `SyncedKeybind.createFromMC(() -> () -> gameSettings.keyBindJump)`, and the jetpack item reads `isKeyDown(player)` on the server | One source of truth for "is the jump key held" |
| Debug keys and category without lang entries (CORE-29) | Lang entries for every key and the category `key.categories.gregguns` | Fixes CORE-29 |

Fire and reload stay as they are: firing uses the vanilla attack key (the spec's [left-click handling](../../techguns-spec/01-core/input-and-hud.md#firing-guns-with-the-left-mouse-button)), and reload is a `SyncedKeybind` like the others.

## Integration points

### GTNHLib: `SyncedKeybind`
- **Pack config:** none. Key bindings are stored in the player's `options.txt`. The pack ships no `options.txt` (`reference/gtnh/2.9.0-RC-2/.minecraft/` has none), so there are no pack defaults to merge with.
- **Source:**
  - [`SyncedKeybind.createConfigurable`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/keybind/SyncedKeybind.java#L110): registers a `KeyBinding` and syncs held and pressed state to the server.
  - [`createFromMC`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/keybind/SyncedKeybind.java#L89) wraps an existing vanilla binding; the argument is a doubly wrapped supplier so the server never loads client classes.
  - [`isKeyDown(EntityPlayer)`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/keybind/SyncedKeybind.java#L121): the client reads the real key, the server reads the synced map.
  - [`registerGlobalListener`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/keybind/SyncedKeybind.java#L168) and [`IKeyPressedListener.onKeyPressed`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/keybind/IKeyPressedListener.java), called on the server only, for press and release.
  - The sync packet is `PacketKeyDown`, registered in [`NetworkHandler.init`](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.52/src/main/java/com/gtnewhorizon/gtnhlib/network/NetworkHandler.java).
- **GregGuns does:** creates the `SyncedKeybind` objects in a class that is safe to load on a dedicated server (the constructor guards client work itself, see the `FMLCommonHandler` side check in the constructors). Each toggle registers one global listener that validates and acts.
- **Pack-side change:** none.

### NEI: in-GUI keys
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/NEI/client.cfg`. The keys are all under `keys.gui.*` and `keys.world.*`. Values that fall on a Techguns letter: `keys.gui.recipe` = 19 (R), `keys.gui.hide_bookmarks` = 48 (B), `keys.gui.bookmark_pull_items` = 47 (V), `keys.gui.xu_ping` = 20 (T). The `keys.world.*` entries are `0` (unbound) except `chunkoverlay` = 67 (F9) and `moboverlay` = 65 (F7).
- **Source:** the config file only. NEI's `keys.gui.*` entries apply to NEI screens, so they do not meet our in-world keys. Nothing here is read from NEI source.
- **GregGuns does:** NEI's GUI keys act only inside screens, so they do not clash with our in-world keys; reload stays on R. The Controls screen shows any overlap in red, and Controlling lists them.
- **Pack-side change:** none.

### Hodgepodge: IC2 key fixes
- **Pack config:** `config/hodgepodge.cfg`, keys `fixIc2KeybindsIgnoreKeyState` = `true` and `fixIc2KeybindsInGuis` = `true`; also `fixJourneymapKeybinds` = `true`.
- **Source:** config comments only. IC2's own mode and boost keys act on `KeyBinding` state rather than the raw key, and not in GUIs.
- **GregGuns does:** reads `KeyBinding` state (as `SyncedKeybind` does) rather than `Keyboard.isKeyDown`, so another mod can suppress our key and a GUI blocks it.
- **Pack-side change:** none.

## Requirements

- **GGS-01-13** Every GregGuns key is a `SyncedKeybind` (or wraps a vanilla `KeyBinding`) with a lang entry for its name and the category `key.categories.gregguns`.
- **GGS-01-14** The server never trusts a client-sent action ID. Each toggle's listener checks the player's worn items, config and fuel before it changes state.
- **GGS-01-15** Removing a Night Vision toggle only removes the effect that GregGuns added (fixes CORE-15).
- **GGS-01-16** Key handling reads `KeyBinding` state, so a GUI or another mod can block it.
- **GGS-01-17** The class holding the `SyncedKeybind` objects loads on a dedicated server without error.
- **GGS-01-18** No GregGuns default key shares a code with a pack key that acts in the same context (in-world). A new key is checked against the pack's `options.txt` first ([record 0013](../../design/0013-default-key-map.md)).

## Tests

- GGS-01-13: a unit test over the key table checks that every lang key exists.
- GGS-01-14, GGS-01-15: unit tests on the pure decision methods (worn item present or not, fuel empty, own effect versus potion effect).
- GGS-01-16: manual check with `runClient`: open a chest GUI and press N.
- GGS-01-17: the `runServer` boot smoke test.
- GGS-01-18: `DefaultKeyMapTest` checks the defaults are distinct and absent from the pack's in-world key list.

## Open questions

- **Default key conflicts: closed** (M0 audit D-3). A real GTNH 2.9.0-RC-2 client run showed J, N and P clashing with in-world pack keys, so the defaults moved: night vision M, ammo pouch right bracket, back device semicolon; reload stays R. See [design record 0013](../../design/0013-default-key-map.md). Players with a saved `options.txt` keep their old bindings.
