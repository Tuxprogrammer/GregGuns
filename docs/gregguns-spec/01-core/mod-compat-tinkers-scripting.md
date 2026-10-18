# Tinkers' Construct, Thermal Expansion and recipe scripting

- **Techguns baseline:** [integrations.md](../../techguns-spec/01-core/integrations.md) (TConstruct IMC and tab, Thermal Expansion stub, MineTweaker 3), [minetweaker.md](../../techguns-spec/08-machines-production/minetweaker.md), [CORE-07](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-26](../../techguns-spec/01-core/index.md#suspected-bugs), [CORE-27](../../techguns-spec/01-core/index.md#suspected-bugs)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (Techguns metals and plates are dropped), [0003 energy model](../../design/0003-energy-model.md) (no RF)
- **Pack mods:**
  - TinkersConstruct (`TConstruct`, 1.14.118-GTNH)
  - Tinkers-Gregworks (`TGregworks`, 1.0.33; branch `GT-NH`, no tags)
  - CraftTweaker (mod ID `MineTweaker3`, 3.4.8)
  - Minetweaker-Gregtech-5-Addon (`GTTweaker`, 2.3.4)
  - Thermal Expansion is **not** in the pack. CoFH Core 3.1.4-329 is.
- **Roadmap:** the milestone that ships the first tool-capable material or the player inventory GUI; M4 for GT recipe maps

## What changes from Techguns

| Techguns | GregGuns | Why |
|---|---|---|
| Five Tinkers' tool materials sent by IMC, IDs 50 to 54 hard-coded | **None.** GregGuns registers no Tinkers' material | Four of the five already exist: Tinkers-Gregworks builds a material for each GT material, including Plastic, Carbon, Rubber and Titanium. The Techguns plate and ingot items are dropped anyway ([0002](../../design/0002-machine-deconfliction.md)) |
| `TGInventoryTab` registered with TConstruct's `TabRegistry`; own tab buttons when TConstruct is absent | One tab, only if GregGuns keeps an extra-inventory GUI ([05-armor](../../techguns-spec/05-armor/index.md)). TConstruct is always in the pack, so the fallback tab buttons are dropped | Removes the second code path and [CORE-07](../../techguns-spec/01-core/index.md#suspected-bugs) |
| Thermal Expansion stub (empty `init`, dead `@Optional.Interface`) | **Not ported** | Thermal Expansion is not in the pack and the stub does nothing ([CORE-27](../../techguns-spec/01-core/index.md#suspected-bugs)) |
| MineTweaker 3 classes for Techguns machines | No GregGuns ZenScript classes. GT recipes go through GT recipe maps, which GTTweaker already reaches | Machines are replaced by GT recipe maps ([0002](../../design/0002-machine-deconfliction.md)) |

## Integration points

### Tinkers-Gregworks: materials already cover GT materials
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/TGregworks.cfg`: `addMaterialsAnyway` = `true` (L361), `materialIDRangeStart` = `1500` (L367). The `materials` blocks list per-material entries including `Carbon`, `Plastic`, `Rubber`, `Steel` and `Titanium` (for example L1530, L1677, L1701, L1718, L1737).
- **Source:** config only. Tinkers-Gregworks is an `upstream-branch` mod (branch `GT-NH`) and its code was not read.
- **GregGuns does:** nothing. GregGuns guns and armor do not use Tinkers' materials. If a later milestone wants GregGuns tool parts, add them as GT materials and let Tinkers-Gregworks pick them up.
- **Pack-side change:** none.

### TinkersConstruct: IMC and inventory tab (reference)
- **Pack config:** `config/TinkersConstruct.cfg` (not read for this sheet).
- **Source:**
  - [`IMCHandler.processIMC`](https://github.com/GTNewHorizons/TinkersConstruct/blob/1.14.118-GTNH/src/main/java/tconstruct/util/IMCHandler.java#L34-L60): `addMaterial` takes an NBT message and reads the integer `Id`. It is not a name, so an unreserved hard-coded ID can clash.
  - [`TConstruct.handleIMC` and `loadComplete`](https://github.com/GTNewHorizons/TinkersConstruct/blob/1.14.118-GTNH/src/main/java/tconstruct/TConstruct.java#L280-L288): Tinkers' handles IMC from the normal IMC event **and** again at load-complete from the runtime queue. So a message sent in `postInit` is probably still read (by the FML runtime-message contract; not tested in a running pack). (This qualifies the worry in [CORE-26](../../techguns-spec/01-core/index.md#suspected-bugs), which assumed postInit was too late. The duplicated `addMaterialItem` slip is still a bug.)
  - [`TabRegistry.registerTab`](https://github.com/GTNewHorizons/TinkersConstruct/blob/1.14.118-GTNH/src/main/java/tconstruct/client/tabs/TabRegistry.java#L23): the tab hook, taking an `AbstractTab`.
- **GregGuns does:** if it ships the extra-inventory GUI, it registers one `AbstractTab` subclass in client init, behind `Loader.isModLoaded("TConstruct")`. The tab opens its GUI through a GregGuns packet ([networking-gtnhlib.md](networking-gtnhlib.md)), not a config ID. If GregGuns ever sends a TConstruct IMC, it uses an ID of 1500 or above from a GregGuns config option, and sends in `init`.
- **Pack-side change:** none.

### CraftTweaker and GTTweaker: scripting
- **Pack config:** none. The pack ships no `scripts` folder in the client instance (`reference/gtnh/2.9.0-RC-2/.minecraft/` has none), so scripts are a server or pack-repository matter.
- **Source:** [`GTRecipeMap.getRecipeMap`](https://github.com/GTNewHorizons/Minetweaker-Gregtech-5-Addon/blob/2.3.4/src/main/java/gttweaker/mods/gregtech/GTRecipeMap.java#L10-L19) finds a map by comparing its name to `RecipeMap.ALL_RECIPE_MAPS`' `unlocalizedName`. GTTweaker also has one hand-written class per machine under [`gttweaker/mods/gregtech/machines`](https://github.com/GTNewHorizons/Minetweaker-Gregtech-5-Addon/tree/2.3.4/src/main/java/gttweaker/mods/gregtech/machines).
- **GregGuns does:** builds its recipe maps with GT's recipe-map builder, so each is in `ALL_RECIPE_MAPS` and addressable by name. It adds no ZenScript class.
- **Pack-side change:** none. Whether a script can *add* recipes to a GregGuns map is an open question (below).

## Requirements

- **GGS-01-24** GregGuns sends no Tinkers' Construct IMC in the first release.
- **GGS-01-25** Any class that references `tconstruct.*` is loaded only after `Loader.isModLoaded("TConstruct")` returns true.
- **GGS-01-26** The GregGuns inventory tab, if present, opens its GUI by a GregGuns packet and never by a configurable numeric ID.
- **GGS-01-27** GregGuns has no code that references Thermal Expansion or any RF API.
- **GGS-01-28** Every GregGuns GT recipe map has a stable `unlocalizedName` that is documented, because scripts address maps by name.

## Tests

- GGS-01-24, GGS-01-27: a unit test scans GregGuns classes for banned package prefixes (`cofh.`, and `FMLInterModComms.sendMessage("TConstruct"`).
- GGS-01-25: the `runServer` boot smoke test (TConstruct is absent on the dev server unless added).
- GGS-01-26: manual check in `runClient`.
- GGS-01-28: a unit test lists the recipe-map names and checks each against a documented table.

## Open questions

- Can GTTweaker add recipes to an arbitrary map by name, or only to maps it has a class for? The source read here shows lookup by name and per-machine classes, not a generic add. If a generic entry point is missing, the pack team may want a GregGuns-specific ZenScript class. Check in M4 when the first recipe map exists.
