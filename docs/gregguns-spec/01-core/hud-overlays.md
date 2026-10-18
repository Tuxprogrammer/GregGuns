# HUD and overlay mods

- **Techguns baseline:** [input-and-hud.md § HUD overlay](../../techguns-spec/01-core/input-and-hud.md#hud-overlay-guioverlayammocount), [armor bar replacement](../../techguns-spec/01-core/input-and-hud.md#armor-bar-replacement-rendergameoverlayeventpre-armor), [events.md § GuiOverlayAmmoCount](../../techguns-spec/01-core/events.md#guioverlayammocount-forge-bus-client), [CORE-17](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-18](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-33](../../techguns-spec/01-core/index.md#suspected-bugs)
- **Design records:** none
- **Pack mods:**
  - Angelica (`angelica`, 2.2.28)
  - OverloadedArmorBar (`overloadedarmorbar`, 1.1.1)
  - InGame-Info-XML (`InGameInfoXML`, 2.9.7)
  - waila (`Waila`, 1.19.34)
  - HoloInventory (`holoinventory`, 2.5.17-GTNH)
- **Roadmap:** M0 (HUD skeleton), M1 (ammo and status HUD), the turret milestone (Waila provider)

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| A 24-point armor bar: cancels the vanilla `ARMOR` element at `NORMAL` priority and redraws it at a fixed height | **Not ported.** GregGuns armor reports its value through Forge's total armor value and lets OverloadedArmorBar draw it | OverloadedArmorBar already cancels `ARMOR` first (see below), so the Techguns bar would never run in the pack. It also removes [CORE-33](../../techguns-spec/01-core/index.md#suspected-bugs) |
| Ammo text pinned to the bottom-right corner, at fixed coordinates | Ammo and status HUD anchored bottom-right with client config offsets `hudOffsetX` and `hudOffsetY` | InGame-Info-XML also draws in the bottom-right corner (see below) |
| Draws in the `POST` phase of `EXPERIENCE`, and ignores cancelable events | Same hook, kept inside `RenderGameOverlayEvent` handlers only | Keeps HUD caching safe if the pack turns it on |
| Right alignment by `String.length() * 6` (CORE-18) | Right alignment by `FontRenderer.getStringWidth` on the string without formatting codes | Fixes CORE-18 |

## Integration points

### OverloadedArmorBar: owns the `ARMOR` element
- **Pack config:** none read.
- **Source:** [`ArmorBarRenderer.onRenderGameOverlayEvent`](https://github.com/GTNewHorizons/OverloadedArmorBar/blob/1.1.1/src/main/java/locusway/overloadedarmorbar/overlay/ArmorBarRenderer.java#L34-L45) is a `HIGHEST`-priority `RenderGameOverlayEvent.Pre` handler for `ARMOR`. It reads `ForgeHooks.getTotalArmorValue`, draws, advances `GuiIngameForge.left_height` by 10 ([L82](https://github.com/GTNewHorizons/OverloadedArmorBar/blob/1.1.1/src/main/java/locusway/overloadedarmorbar/overlay/ArmorBarRenderer.java#L82-L86)) and cancels the event.
- **GregGuns does:** adds no `ARMOR` handler. GregGuns armor implements Forge's armor interfaces so the value counts in `getTotalArmorValue` (the armor spec is [05-armor](../../techguns-spec/05-armor/index.md)). Any other GregGuns element drawn on the left side reads and advances `GuiIngameForge.left_height`.
- **Pack-side change:** none.

### Angelica: HUD caching
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/angelica-modules.cfg`: `enableHudCaching` = `true` (L109), `enableHudCachingEventTransformer` = `true` (L112), `hudCachingActive` = `false` (L199), `hudCachingFPS` = `20` (L202). The cache is available at startup but **off at runtime**.
- **Source:**
  - [`HUDCaching` render path](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/hudcaching/HUDCaching.java#L156-L195): when `hudCachingActive` is false it calls `renderGameOverlay` every frame. When true, it re-draws the HUD into a framebuffer once per `1000 / hudCachingFPS` ms and reuses the picture between refreshes.
  - [`HUDCachingEarlyReturnTransformer`](https://github.com/GTNewHorizons/Angelica/blob/2.2.28/src/main/java/com/gtnewhorizons/angelica/loading/fml/compat/transformers/generic/HUDCachingEarlyReturnTransformer.java#L18-L36) inserts a call to `HUDCachingHooks.shouldReturnEarly()` at the top of named methods; the config comment says it applies to "all RenderGameOverlayEvent receivers". Which methods it patches was not traced for this sheet.
- **GregGuns does:** keeps all HUD drawing inside `RenderGameOverlayEvent` handlers. It holds no per-frame counters in the draw path, and it takes values from the player's current state at draw time. With caching on, the HUD would update at most every 50 ms, which is fine for ammo and status.
- **Pack-side change:** none. This is a check, not a request. If a player turns `hudCachingActive` on, test the HUD.

### InGame-Info-XML: shares the bottom-right corner
- **Pack config:**
  - `config/InGameInfoXML.cfg`, category `alignment`: `bottomright` = `-2 -2`.
  - `config/InGameInfoXML/InGameInfo.xml`, line 1236: a `<lines at="bottomright">` block that lists active potion durations and icons.
- **Source:** config files only.
- **GregGuns does:** defaults `hudOffsetY` so the ammo text sits above the InGame-Info-XML potion lines, and exposes the offsets for pack makers. The exact default is set in M1 by a screenshot test with several potions active.
- **Pack-side change:** none required. A pack maker who prefers can move the `bottomright` block in `InGameInfo.xml` instead.

### Waila and HoloInventory: tooltips on GregGuns blocks
- **Pack config:** `config/holoinventory.cfg`, key `bannedTiles` lists five tile classes, none from GregGuns. `config/Waila.cfg` has display options only.
- **Source:**
  - [`Waila.processIMC`](https://github.com/GTNewHorizons/waila/blob/1.19.34/src/main/java/mcp/mobius/waila/Waila.java#L89-L112): the IMC key `register` with a string naming a static method; that method receives an `IWailaRegistrar`.
  - [`IWailaRegistrar`](https://github.com/GTNewHorizons/waila/blob/1.19.34/src/main/java/mcp/mobius/waila/api/IWailaRegistrar.java#L43-L65): `registerBodyProvider(IWailaDataProvider, Class)` for blocks and `registerBodyProvider(IWailaEntityProvider, Class)` for entities, plus head, tail and NBT variants.
- **GregGuns does:** in `init`, if `Loader.isModLoaded("Waila")`, sends the `register` IMC and adds body providers for the Turret (owner, ammo, energy) and the Repair Bench. HoloInventory needs no code: the weapon and ammo chests are plain inventories and it shows them. Use `@Optional` on the provider classes so they load only with Waila.
- **Pack-side change:** none.

### Extreme Sound Muffler (checked, no change)
- **Pack config:** `config/extremesoundmuffler.cfg`, key `forbiddenSounds` = `ui.`, `music.`, `ambient.`. The config comment says the list holds sound names to blacklist, so the mod works on sound names. Its behavior for other sounds was inferred from this, not read from source (`Lyfts/ExtremeSoundMuffler-Legacy`, tag 1.1.1).
- **GregGuns does:** nothing. Gun and NPC sounds use `gregguns:`-prefixed event names, so they show up and can be muffled. (Techguns' sound categories had no effect in the original, [CORE-39](../../techguns-spec/01-core/index.md#suspected-bugs); GregGuns should make its categories real, which is an audio decision for [03](../../techguns-spec/03-gun-visuals-audio/index.md).)
- **Pack-side change:** none.

## Requirements

- **GGS-01-29** GregGuns registers no `RenderGameOverlayEvent.Pre` handler for `ARMOR`.
- **GGS-01-30** GregGuns armor counts in `ForgeHooks.getTotalArmorValue`.
- **GGS-01-31** All GregGuns HUD drawing runs inside `RenderGameOverlayEvent` handlers and does not depend on being called once per frame.
- **GGS-01-32** The ammo and status HUD position is `bottom-right + (hudOffsetX, hudOffsetY)` from client config, and text is aligned by `getStringWidth` on the stripped string.
- **GGS-01-33** Any left-side HUD element reads `GuiIngameForge.left_height` and advances it by its own height.
- **GGS-01-34** Waila providers are registered only when `Waila` is loaded, in classes marked `@Optional`.

## Tests

- GGS-01-29, GGS-01-33: a unit test over the handler registrations, or code review.
- GGS-01-30: an in-game check with a full GregGuns set and OverloadedArmorBar: the bar shows the set's value.
- GGS-01-31: manual check with `hudCachingActive=true` in `config/angelica-modules.cfg`.
- GGS-01-32: unit test of the alignment helper; manual screenshot with several potions active.
- GGS-01-34: the `runServer` boot smoke test (Waila is client-heavy, so also run once without it).

## Open questions

- Which methods does Angelica's early-return transformer patch, and does it affect any handler GregGuns registers? Trace `HUDCachingEarlyReturnTransformer` callers when M1 builds the HUD.
- Are two Waila providers worth the effort before the turret exists? Defer until the turret milestone.
