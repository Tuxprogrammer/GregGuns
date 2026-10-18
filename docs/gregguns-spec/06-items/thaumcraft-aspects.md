# Thaumcraft aspects for items

How GregGuns items get Thaumcraft aspects, the way GT does it.

- **Techguns baseline:** none. Techguns registers no aspects ([item-catalog.md](../../techguns-spec/06-items/item-catalog.md)).
- **Design records:** none. This follows [CONTRIBUTING § GTNH integration](../../../CONTRIBUTING.md#gtnh-integration) (GT conventions).
- **Pack mods:**
  - Thaumcraft (`Thaumcraft`, 4.2.3.5), optional
  - GregTech (`gregtech`, 5.09.54.205), which bridges to Thaumcraft
  - ThaumcraftMobAspects (`ThaumcraftMobAspects`, 1.2.1-GTNH), for mobs
- **Roadmap:** M2 (items exist) and M7 (mob aspects)

## What changes from Techguns

Techguns items have no aspects of their own. They get whatever Thaumcraft's recipe scan derives from crafting-grid recipes. GregGuns items are mostly made in GT machines, which the scan does not read, so many would end up with **no aspects** and be invisible to Thaumcraft research and infusion. GregGuns registers aspects explicitly, in the same form GT uses for its own items.

## Integration points

### GT's Thaumcraft bridge

- **Pack config:** none.
- **Source:**
  - [`GTProxy` L462-L464](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTProxy.java#L462-L464) sets `GregTechAPI.sThaumcraftCompat` only when Thaumcraft is loaded, so the field is `null` otherwise.
  - [`IThaumcraftCompat.registerThaumcraftAspectsToItem(ItemStack, List<TC_AspectStack>, boolean additive)`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/interfaces/internal/IThaumcraftCompat.java#L31).
  - [`GTThaumcraftCompat`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/GTThaumcraftCompat.java#L305-L322): with `aAdditive = false` it registers the aspects only if the item has none yet; with `true` it merges them in.
  - [`TCAspects`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TCAspects.java) is the aspect enum and `TCAspects.TC_AspectStack(aspect, amount)` the entry type ([L86-L94](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/TCAspects.java#L86-L94)). It includes `METALLUM`, `TELUM`, `MACHINA`, `POTENTIA`, `TUTAMEN`, `IGNIS`, `RADIO`, `PERDITIO`, `VACUOS`, `MOTUS`.
  - GT's own use: the plastic Fuel Can is declared with `new TCAspects.TC_AspectStack(TCAspects.VACUOS, 1L)` next to its `ItemData` ([`MetaGeneratedItem01` L1093-L1100](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/items/MetaGeneratedItem01.java#L1093-L1100)), and `MetaGeneratedItem` passes them on with a `null` check ([L195-L196](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/MetaGeneratedItem.java#L195-L196)). `Uranium235` is declared with `RADIO` 2 and `METALLUM` 2 ([`MaterialsInit` L2090-L2108](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/loaders/materials/MaterialsInit.java#L2090-L2108)).
  - Thaumcraft's own entry point, for reference: [`ThaumcraftApi.registerObjectTag(ItemStack, AspectList)`](https://github.com/Azanor/thaumcraft-api/blob/ced30ed35097d53cdb2a843d705477e52896e54b/ThaumcraftApi.java#L363) and `registerComplexObjectTag` ([L415](https://github.com/Azanor/thaumcraft-api/blob/ced30ed35097d53cdb2a843d705477e52896e54b/ThaumcraftApi.java#L415)). This is Azanor's API repository, a branch, pinned here to commit `ced30ed`.
- **GregGuns does:**
  - After its items are registered, calls `GregTechAPI.sThaumcraftCompat.registerThaumcraftAspectsToItem(stack, aspects, false)` for each item that has a row below. It checks that the field is non-null first, so there is no hard dependency on Thaumcraft and no `Loader.isModLoaded` guard is needed around the call itself.
  - Uses the non-additive form so a pack script that already set aspects wins.
  - Keeps the aspect lists in one table class, not in each item.
- **Pack-side change:** none. A pack can still override aspects with Thaumcraft's own tools or a script.

### Aspect assignment

Aspect values follow GT's scale: small integers, one or two per aspect. Choose from the aspects below. The list is a starting point for M2 and is tuned in play.

| Item class | Aspects |
|---|---|
| Guns (all) | `TELUM` 2, `METALLUM` 2, `MACHINA` 1 |
| Energy guns and electric gear | `POTENTIA` 2, `MACHINA` 1, `TELUM` 1 |
| Bullets, shells and loose ammo | `TELUM` 1, `METALLUM` 1 |
| Magazines, drums | `TELUM` 1, `METALLUM` 1, `VACUOS` 1 |
| Explosive ammo (rockets, grenades) | `TELUM` 2, `IGNIS` 2, `PERDITIO` 1 |
| Armor pieces | `TUTAMEN` 2, `METALLUM` 1 |
| Tanks | `VACUOS` 1, `METALLUM` 1 |
| Nuclear items (cells, warhead, Rad Emitter) | `RADIO` 2, `POTENTIA` 1 (plus `PERDITIO` 1 for the warhead) |
| Gun parts (barrels, receivers) | `METALLUM` 1, `MACHINA` 1 |

### ThaumcraftMobAspects (entities)

Mob aspects belong to area 10; this is the contract.

- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/ThaumcraftMobAspects/<modid>.cfg`, one file per mod (`gregtech.cfg` exists, `openmodularturrets.cfg` holds projectile entries with empty `aspects`). Each entry has `entityName`, `aspects` and a header and footer.
- **Source:** [`ModFileCache`](https://github.com/GTNewHorizons/ThaumcraftMobAspects/blob/1.2.1-GTNH/src/main/java/iguanaman/thaumcraftmobaspects/ModFileCache.java#L35-L50) creates `<modid>.cfg` on demand. [`guessModId`](https://github.com/GTNewHorizons/ThaumcraftMobAspects/blob/1.2.1-GTNH/src/main/java/iguanaman/thaumcraftmobaspects/ModFileCache.java#L67-L91) takes the part of the registered entity name before the first `.`; a name without a dot is guessed from sibling classes in the same package.
- **GregGuns does:** registers every entity with a dotted name (`gregguns.<Name>`), so the aspects land in `gregguns.cfg` and not in an `unknown` file.
- **Pack-side change:** after the first run the pack fills in `config/ThaumcraftMobAspects/gregguns.cfg`, if it wants aspect values. This is optional; the file is created empty.

## Requirements

- **GGS-06-30.** Every GregGuns item class in the table above has aspects registered through `sThaumcraftCompat` when it is non-null.
- **GGS-06-31.** GregGuns code references no `thaumcraft.*` class directly (it uses GT's bridge), so the mod loads without Thaumcraft.
- **GGS-06-32.** Aspect registration uses the non-additive form.
- **GGS-06-33.** Every GregGuns entity is registered with a `gregguns.`-prefixed name.

## Tests

| ID | Verified by |
|---|---|
| GGS-06-30 | Unit test with a stub compat object: every registered item produces a call |
| GGS-06-31 | `runServer` boot without Thaumcraft; a class-reference check in the dev self-check |
| GGS-06-32 | Unit test with a stub compat object |
| GGS-06-33 | Unit test over the entity registry |
| Manual | Scan a gun, a magazine and a Nuclear Power Cell with a Thaumometer |

## Open questions

- **Timing.** The call is made after item registration. Which mod-loading phase is late enough, and whether Thaumcraft's recipe-derived scan can overwrite or skip the entries, is not confirmed. Verify with a Thaumometer in `runClient`.
