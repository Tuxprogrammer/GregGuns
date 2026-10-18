# 01 · Core: GregGuns against the GTNH pack

## Overview

This area covers the mod skeleton: config, networking, keybinds, HUD, mod compatibility and player relations. It pairs with [techguns-spec/01-core](../../techguns-spec/01-core/index.md). The sheets here record where GregGuns differs and what it plugs into in GTNH 2.9.0-RC-2.

The GTNHLib version in the pack (0.11.52) supplies config, packets, synced keybinds and teams, so most Techguns core code is replaced rather than ported.

Two Techguns integrations do not exist in the pack. FTB Utilities is not installed, and Thermal Expansion is not installed. The pack has ServerUtilities and GTNHLib teams in place of FTBU friends.

## Contents

| File | Topic |
|---|---|
| [config-gtnhlib.md](config-gtnhlib.md) | `@Config` classes, `@Config.Sync`, which Techguns keys disappear |
| [../../config.md](../../config.md) | Reference for every config option and module flag: default, range, synced or not |
| [networking-gtnhlib.md](networking-gtnhlib.md) | `IPacket` and `NetworkChannel`, thread safety, DataWatcher limit, potion array |
| [keybinds.md](keybinds.md) | `SyncedKeybind`, server-side key state, pack key facts |
| [team-relations.md](team-relations.md) | GTNHLib teams and ServerUtilities allies instead of FTBU friends |
| [mod-compat-tinkers-scripting.md](mod-compat-tinkers-scripting.md) | Tinkers' materials and tab, Thermal Expansion, CraftTweaker and GTTweaker |
| [hud-overlays.md](hud-overlays.md) | OverloadedArmorBar, Angelica HUD caching, InGame-Info-XML, Waila, HoloInventory, sound muffler |

No integration point (see the Techguns sheet and the [design records](../../design/README.md)):

- Lifecycle and registration, creative tab, GUI ID table ([lifecycle-and-registration.md](../../techguns-spec/01-core/lifecycle-and-registration.md)). Techguns ore, metal and fluid registrations are dropped by [0002](../../design/0002-machine-deconfliction.md) and [0005](../../design/0005-world-presence.md); the rest is GregGuns' own code.
- Player state and per-tick logic ([player-state.md](../../techguns-spec/01-core/player-state.md)). The only pack touchpoint is the DataWatcher slots, covered in [networking-gtnhlib.md](networking-gtnhlib.md).
- Event catalogue, client effects, utilities, textures, sounds and lang files. These are internal. The sound-muffler check is in [hud-overlays.md](hud-overlays.md).
- Potions: no custom potion in the Techguns config, so no sheet (a check is in [networking-gtnhlib.md](networking-gtnhlib.md#hodgepodge-potion-array-checked-no-change)).

## Integration map

| Techguns feature | GregGuns approach | Pack mod or system | Evidence |
|---|---|---|---|
| `Techguns.cfg` by hand | `@Config` classes; synced fields; dropped keys | GTNHLib 0.11.52 | [config-gtnhlib.md](config-gtnhlib.md) |
| 16 packets on a raw channel | `IPacket` on a `NetworkChannel`; main-thread handlers | GTNHLib 0.11.52 | [networking-gtnhlib.md](networking-gtnhlib.md) |
| DataWatcher IDs 23 and 24 | No DataWatcher slots | NEID (`ExtendDataWatcher=false`) | [networking-gtnhlib.md](networking-gtnhlib.md) |
| Custom potion IDs | None needed | Hodgepodge (`enlargePotionArray`) | [networking-gtnhlib.md](networking-gtnhlib.md) |
| Five `KeyBinding`s plus `PacketTGKeybindPress` | `SyncedKeybind` with server listeners | GTNHLib 0.11.52; NEI keys checked | [keybinds.md](keybinds.md) |
| FTBU `FriendsAPI` | `PlayerRelations` over GTNHLib teams and ServerUtilities allies | GTNHLib, ServerUtilities 2.4.14 | [team-relations.md](team-relations.md) |
| TConstruct materials by IMC | None; Tinkers-Gregworks covers GT materials | TinkersConstruct 1.14.118-GTNH, TGregworks | [mod-compat-tinkers-scripting.md](mod-compat-tinkers-scripting.md) |
| TConstruct inventory tab | One `AbstractTab`, soft dependency | TinkersConstruct | [mod-compat-tinkers-scripting.md](mod-compat-tinkers-scripting.md) |
| Thermal Expansion stub | Dropped | Not in the pack | [mod-compat-tinkers-scripting.md](mod-compat-tinkers-scripting.md) |
| MineTweaker 3 ZenScript | None; GT recipe maps are reachable by name | CraftTweaker 3.4.8, GTTweaker 2.3.4 | [mod-compat-tinkers-scripting.md](mod-compat-tinkers-scripting.md) |
| 24-point armour bar | Dropped; Forge armour value feeds the pack's bar | OverloadedArmorBar 1.1.1 | [hud-overlays.md](hud-overlays.md) |
| Ammo HUD, bottom-right | Same corner, config offsets | InGame-Info-XML 2.9.7, Angelica 2.2.28 | [hud-overlays.md](hud-overlays.md) |
| Block tooltips | Waila providers; HoloInventory unchanged | waila 1.19.34, HoloInventory 2.5.17-GTNH | [hud-overlays.md](hud-overlays.md) |
| Sound categories | Names work with the muffler; categories are a 03 decision | Extreme Sound Muffler 1.1.1 | [hud-overlays.md](hud-overlays.md) |
| Quest-line progression | A pack-side change only | BetterQuesting 3.8.89-GTNH | Pack-side changes below |

## Pack-side changes

| File | Change | Why | Sheet |
|---|---|---|---|
| `config/betterquesting/DefaultQuests/` | The GregGuns quest tab: see [12-questbook](../12-questbook/index.md) | Quests live in the pack | [12-questbook](../12-questbook/index.md) |

No other pack-side change is needed for this area.

## Related areas

- [02-guns](../../techguns-spec/02-guns/index.md): fire and reload packets, damage factors (synced config).
- [05-armor](../05-armor/index.md): armour value, extra-inventory GUI and cosmetic slots.
- [10-entities-npcs](../../techguns-spec/10-entities-npcs/index.md): faction hostility uses `PlayerRelations`.
- [09-machines-utility](../../techguns-spec/09-machines-utility/index.md): turret and security owner checks use `PlayerRelations`.
- [../INTEGRATION.md](../INTEGRATION.md): the evidence rules used on every sheet here.

## Open questions

- Default `playerRelationMode`: `TEAMS` or `EVERYONE_FRIENDLY`? See [team-relations.md](team-relations.md). Needs a short design record.
- Do Techguns' default keys (N, B, V, R, J) clash in the full pack? Only a running client can show it. See [keybinds.md](keybinds.md).
- Do face and back cosmetics need their own sync, or should they use Baubles Expanded or TravellersGear slots? See [networking-gtnhlib.md](networking-gtnhlib.md). Needs a record with the 05-armor sheets.
- Can GTTweaker add recipes to an arbitrary GT recipe map by name? See [mod-compat-tinkers-scripting.md](mod-compat-tinkers-scripting.md).
