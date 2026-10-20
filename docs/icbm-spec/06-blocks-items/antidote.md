# Antidote

Full sheet. The antidote is one of the few ICBM-Classic items GregGuns keeps ([D10](../../gregguns-icbm-spec/DECISIONS.md#d10-in-detail): "Kept, mapped to GT hazards"). Source: `content/items/ItemAntidote.java` at `9c1d985a`, cited relative to `src/main/java/icbm/classic/`. The gas effects it exists to cure are in [02-explosives/gas-and-status.md](../02-explosives/gas-and-status.md#5-antidote-contentitemsitemantidotejava), which has a short entry for it; this sheet is the complete one.

## 1. Identity

| Field | Value |
|---|---|
| Class | `content/items/ItemAntidote` extends `prefab/item/ItemBase` (plain item, no custom constructor) |
| Registry name | `icbmclassic:antidote` (registered by `ItemReg.registerItems`, name set with `setName("antidote")`) |
| Unlocalized name | `item.icbmclassic:antidote` |
| Metadata | none (single item, no subtypes) |
| English name | "Antidote" (`item.icbmclassic:antidote.name`) |
| Tooltip | `item.icbmclassic:antidote.info` = "Cures all effects" |
| Creative tab | the ICBM tab; ordered by the tab's catch-all pass, after the defined items |
| Oredict names | none |
| Stack size | 64 (the default; not overridden) |
| Config gate | none. It registers unconditionally, unlike the crafting items and the battery ([dropped-and-deferred.md](dropped-and-deferred.md)) |

## 2. Obtaining

| Source | Detail |
|---|---|
| Crafting | `recipes/antidote.json`, shaped 3 x 3, **all nine slots are seeds**, giving **6** antidotes. Each slot independently accepts melon seeds, pumpkin seeds or wheat seeds, so any mix works. Beetroot and modded seeds do not match. Full row in [recipes.md](recipes.md#06-own-recipes) |
| Loot, mob drops, worldgen | none |
| Smelting, other machines | none |
| Cost | 9 seeds for 6 doses: about 1.5 seeds per use. The cheapest recipe in the mod |

## 3. Numbers

| Constant | Value | Source |
|---|---|---|
| Use duration | 32 ticks (1.6 s) | `getMaxItemUseDuration`, line 31 |
| Use action | `EAT` (eating animation) | `getItemUseAction`, line 37 |
| Doses consumed per use | 1, unless the user is a player in creative mode | line 23 |
| Cooldown | none | n/a |
| Config keys | none | n/a |

## 4. Behavior

Right-click (client and server both run this):
1. Take the held stack from the hand used.
2. Set that hand as the active hand and return SUCCESS. **No condition is checked**: the player does not need any effect, can be at full health and full hunger, and creative players can use it.

While held down (vanilla item-use machinery, a *1.12-only mechanism*): the eating animation, eating sounds and crumb particles play, as for any `EAT` item. Releasing early or swapping items cancels the use: nothing is cured and nothing is consumed.

On finish (after 32 ticks):
1. **Server only:** call `clearActivePotions()` on the user, which removes every active potion effect, beneficial or harmful, whatever its curative items.
2. **Client and server:** if the user is not a player in creative mode, shrink the stack by 1.
3. Return the (shrunk) stack.

Edge cases:
- It cures effects only. It does not heal damage, restore hunger, put out fire or give any immunity. Standing in a gas cloud re-applies the effects within 5 ticks.
- Because the clear is unconditional, it also strips Regeneration, Absorption, Health Boost, Night Vision, Fire Resistance, Speed and any effect added by another mod. A player who drinks it while buffed loses the buffs and still spends the dose (ICBM-06-07).
- Only the player-use path exists. There is no dispenser behavior, no use-on-entity path and no mob use.
- Forge 1.12 probably lets an event handler veto individual removals inside `clearActivePotions` (a cancelable per-effect remove event); not verified, and it is a *1.12-only mechanism*. In 1.7.10, `clearActivePotions` removes everything with no event.

### What it actually cures in ICBM

All of the ICBM gas effects are ordinary vanilla potion effects, so one dose removes them all ([gas-and-status.md](../02-explosives/gas-and-status.md#3-poison-helpers-and-potions-contentpotion)):

| Source | Effects removed |
|---|---|
| Debilitation gas | Poison I, Mining Fatigue I, Slowness III |
| Chemical gas | Poison I, Nausea I, Hunger I, Weakness I, Mining Fatigue I |
| Contagious gas | Poison IV, Blindness IV, Hunger IV, Weakness IV, Mining Fatigue IV |
| Spikes (poison) | Poison I (7 s) |
| Milk (comparison) | A milk bucket cannot remove the poison part of chemical and contagious (empty curative list); the antidote can |

Radioactive blocks and the radiation potion name are not potion-based in 1.12 ICBM (`potion.radiation` is an unused lang key, ICBM-02-79), so the antidote has nothing to cure there.

## 5. Persistence and sync

None. No NBT on the stack, no packets, no capability. The server applies the cure and both sides shrink the stack, so a standard item-use sync is all that is needed.

## 6. UI

| Element | Detail |
|---|---|
| Tooltip | One line, "Cures all effects". `ItemBase.addInformation` prints `<unlocalized>.info`; there is no shift-detail line (`.info.detailed` is not defined) |
| Chat, action bar | none |
| GUI | none |

## 7. Visuals

| Asset | Detail |
|---|---|
| Item model | `models/item/antidote.json`: parent `item/generated`, one layer |
| Texture | `textures/items/antidote.png`, 16 x 16 |
| Held / use animation | vanilla eating pose (`EAT`) |
| Particles | vanilla crumb particles from the `EAT` action |

Catalog entries for [07-visuals-audio](../README.md#areas).

## 8. Audio

None of its own. The vanilla eating sound plays during use (the `EAT` action). No `sounds.json` entry.

## 9. Relationships

| Direction | Link |
|---|---|
| Cures | The gas blasts of [02-explosives](../02-explosives/gas-and-status.md) and poison spikes ([dropped-and-deferred.md](dropped-and-deferred.md#e-spikes-deferred)) |
| Made from | Seeds only; no ICBM crafting items, so the recipe does not depend on anything GregGuns drops |
| Used in recipes | None |
| Decision | [D10](../../gregguns-icbm-spec/DECISIONS.md#d10-in-detail): kept, mapped to GT hazards |

### Contract for the GT-hazards rebuild

What a rebuild must reproduce, and what it may change:

| Keep | Free to change |
|---|---|
| A consumable, used for 1.6 s with an eating animation, spent only when the use completes | The cure scope. ICBM's "everything, including buffs" is a quirk (ICBM-06-07); curing only harmful effects and GT hazards is the sensible reading |
| Server-authoritative cure | The recipe (9 seeds for 6 doses is a vanilla-economy number; the GTNH version should be a machine recipe) |
| Not consumed in creative | Refusing to start the use when there is nothing to cure |
| Cures effects that milk cannot | Cooldown, which ICBM does not have |

## 10. Bugs

| ID | Title | Location | Detail | Severity | Kind |
|---|---|---|---|---|---|
| ICBM-06-07 | Antidote strips beneficial effects and is spent with nothing to cure | `content/items/ItemAntidote.java:21,43-48` | `onItemRightClick` always starts the use, and the finish clears every active effect, good ones included. There is no check for "has a harmful effect" and no refusal when the user is clean | gameplay | logic |

See also [index.md](index.md#suspected-bugs).
