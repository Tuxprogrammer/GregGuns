# 0013 · Default key map in the GTNH pack

- Status: Accepted (by default)
- Gate: none
- Date: 2026-10-20

## Context
Closes M0 audit item D-3 ([m0-exit-gate.md](../gregguns-spec/01-core/m0-exit-gate.md)). [keybinds.md](../gregguns-spec/01-core/keybinds.md) left the key-conflict check for a client run. A real GTNH 2.9.0-RC-2 client run recorded every pack key in `options.txt`. A clash is real only when both keys act in the same context. GregGuns keys all act in-world, so a pack key that works only inside a screen (NEI and similar) does not clash. Sources checked: the jars in `reference/gtnh/2.9.0-RC-2` and, for AE2FC, `KeybindLoader` at tag 1.5.113-gtnh.

## Decision

| GregGuns key | Old | Pack bindings on the same key | Context | Real clash | New default |
|---|---|---|---|---|---|
| Back device (Glider, Anti Gravity) | J | GT `toggle_inertiacanceling`, JourneyMap `map_toggle` | in-world | yes | **Semicolon (39)**: home row beside K and L; every other neighbor of J is taken |
| Night vision | N | `Mode Switch Key` (IC2 and GraviSuite armor and tool modes) | in-world | yes | **M (50)**: the key next to N |
| Ammo pouch | P | `key.openCompendium` (Hardcore Ender Expansion), NEI potion and pattern view | in-world, GUI | yes (compendium) | **Right bracket (27)**: on the row of P, beside its old spot |
| Reload | R | NEI recipe (GUI only), AE2FC `OpenCraftingTerminal` | GUI; in-world but shipped unbound | no | **R (unchanged)** |

R notes: NEI's recipe key acts only in a screen. AE2FC's crafting-terminal key does act in-world (it opens the Ultra wireless terminal when no screen is open), but the mod registers it with no default key, so only a player who binds it to R clashes, and the Controls screen shows that in red. Reload keeps R.

Free keys (no pack binding in `options.txt`): Esc, 0, right bracket, semicolon, apostrophe, M, comma, right Alt, numpad multiply and most function and numpad keys. The three new defaults come from this set.

Rule: **every new GregGuns key is checked against the pack's `options.txt`** before it ships. Add it to `DefaultKeyMapTest` and refresh `src/test/resources/pack-keys.csv` when the pack version changes. The test fails if a default repeats another GregGuns default or matches a pack key marked in-world (unclassified keys count as in-world).

## Consequences
- Code: the defaults in `EquipmentKeybinds` change; `GunKeybinds` is unchanged.
- Players with a saved `options.txt` keep their old bindings; the 0.3.0-beta changelog says so.
- Docs updated: keybinds.md, the 05-armor implementation notes (FLT-DEV-08), m0-exit-gate.md.
- The CSV context labels are name based; unknown keys are treated as in-world, which can only make the test stricter.

## Alternatives considered
- Leave the defaults and rely on the red overlap in Controls: the worst clash (J) would fire two actions on one press for every new player.
- Modifier chords: GTNHLib `SyncedKeybind` has no chord support.
