# Dev self-check

A dev-only consistency check that stops the dev server when GregGuns content is incomplete or breaks the D1 tier map. Required by [CONTRIBUTING § Testing](../../CONTRIBUTING.md#testing).

## When it runs

- Only when `com.gregguns.core.DevEnvironment.isActive()` is true (the deobfuscated workspace from `runClient` or `runServer`). It never runs in the pack.
- In `FMLLoadCompleteEvent` (`CommonProxy.loadComplete`), not in postInit. GT buffers its crafting-table recipes and registers the second pass in its own load-complete handler, and other mods add recipes in their postInit, so postInit would check an incomplete recipe set. This deviates from the wording in CONTRIBUTING; the deviation is recorded in `CommonProxy.loadComplete`.
- On both sides, so a dedicated server (`runServer`) checks too.

## What it checks

| Check | Rule |
|---|---|
| Recipes resolve | Crafting-table recipes (shaped, shapeless, ore-dict) and GT recipe-map recipes that have a GregGuns input or output have no null or empty item inputs, no empty ore-dict entries, no unregistered fluids, and no null outputs. A `null` in a shaped grid is a legitimate gap. An input of a GT machine recipe with stack size 0 is valid too: GT uses size 0 for an input that is required but not consumed (programmed circuits, molds). |
| Tier map | For each GregGuns output: the recipe's EU/t must fit the output's tier (`Tier.lowestForEut(eut) <= output tier`; exactly the tier voltage passes, one EU/t above fails), and no GregGuns input may be of a higher tier than the output. Crafting-table recipes have no voltage. A GregGuns output with no tier is a violation. |
| Content | Every registered GregGuns item and block has a tier (`Tiered`), a lang key `<unlocalized name>.name` in `assets/gregguns/lang/en_US.lang`, an icon name whose PNG exists under `assets/<domain>/textures/items/` (`blocks/` for blocks), and a creative tab or the `HiddenFromCreativeTab` marker. |
| Unification | No GregGuns item is ore-dictionaried under a GT `OrePrefixes` name (`plateSteel`, `circuitBasic`, or a bare `plate`), and GT's unificator (`GTOreDictUnificator.getAssociation`) does not treat any GregGuns stack as a `prefix.material` part. |
| Component coverage | Every registered `GregGunsComponent` is the output of a crafting or GT-map recipe, unless it is on the allowlist (`PartCatalog.noRecipeAllowlist`, each entry with a reason; the depleted Nuclear Power Cell is made by use). A stale or reasonless allowlist entry fails too. A component whose row needs a disabled module (the Laser Barrel without the machines module) is deferred, not failed. |
| Sounds | If `assets/gregguns/sounds.json` exists, every sound it references has an `.ogg` on the classpath. Absent or `{}` means no events. |

Lang keys and textures are read from the classpath, because a dedicated server's `StatCollector` and resource manager do not load mod assets.

## Declaring tier and hidden

```java
public class ItemPistol extends Item implements Tiered {
    @Override public Tier getTier() { return Tier.LV; }
}
// Deliberately not in the creative tab:
public class ItemPlaceholder extends Item implements Tiered, HiddenFromCreativeTab { ... }
```

`Tier` (STEAM, LV, MV, HV, EV, IV, LUV, ZPM) maps to GT's voltage index; its voltage comes from `GTValues.V`. For a block, implement the interfaces on the `Block`.

Items use `setTextureName("gregguns:<name>")`. An item that overrides `registerIcons` with several icons still needs the base icon name to exist.

## Reading a failure

The server stops with `SelfCheckFailedException`, and the same text is logged at ERROR by `[gregguns]`. The message lists **every** violation, one per line:

```
GregGuns self-check failed with 2 violation(s):
  - item gregguns:pistol: lang key item.gregguns.pistol.name is missing from assets/gregguns/lang/en_US.lang
  - recipe gt:gt.recipe.assembler -> gregguns:pistol: output gregguns:pistol: 128 EU/t needs MV but the output is LV (limit 32 EU/t)
```

Fix them all, then rerun. A clean run logs one INFO line: `Self-check passed: N items/blocks, M recipes, ...`.

## Code map

- Pure logic (unit tested, no Minecraft types): `com.gregguns.core.selfcheck` `RecipeTierCheck`, `RecipeChecks`, `ContentChecks`, `SoundChecks`, `UnificationChecks`, `ComponentChecks`, `SelfCheck`, over the descriptors `StackDescriptor`, `RecipeDescriptor`, `ContentDescriptor`.
- Glue: `SelfCheckRunner` reads the item and block registries, `CraftingManager` and GT's `RecipeMap.ALL_RECIPE_MAPS` into descriptors. It reads a few `@SideOnly(CLIENT)` fields (`Item.iconString`, `Item.tabToDisplayOn`, `Block.textureName`, `Block.displayOnCreativeTab`) reflectively by their deobfuscated names, which is safe because it only runs in the dev workspace.
