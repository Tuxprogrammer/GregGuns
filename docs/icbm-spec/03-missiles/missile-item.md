# Missile Items

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. The **missile item** (`ItemMissile`) is one item whose metadata value is the explosive ID. This sheet also covers the two single-purpose missile items that belong to this part, the **SAM item** and the **held-item missile item** with its two dynamic recipes. Cluster missile, cargo and grenade items are part b. Item capabilities (`ICapabilityProvider`) are a *1.12-only mechanism*; in 1.7.10 the same data would live in stack NBT.

## 1. Identity

| Item | Missile (`ItemMissile`) | SAM (`ItemSurfaceToAirMissile`) | Held-item missile (`ItemHeldItemMissile`) |
|---|---|---|---|
| Registry name | `icbmclassic:explosive_missile` (the pre-4.2.0 name `icbmclassic:missile` is remapped to it) | `icbmclassic:surface_to_air_missile` | `icbmclassic:held_item_missile` |
| Class | `content/items/ItemMissile` | `content/missile/entity/anti/item/ItemSurfaceToAirMissile` | `content/missile/entity/itemstack/item/ItemHeldItemMissile` |
| Stack size | 1 | 1 | 1 |
| Durability | none (`setMaxDamage(0)`), subtypes on | n/a | none, subtypes on; the durability bar mirrors the held item's bar |
| Metadata | explosive ID (see section 3) | 0 | 0 |
| Capability on the stack | explosive (`CapabilityExplosiveStack`) and missile stack (`lib/capability/missile/CapabilityMissileStack`) | missile stack (`CapabilitySAMStack`, id `icbmclassic:missile.sam`) | missile stack (`CapabilityHeldItemMissile`, id `icbmclassic:missile.item.held`) |
| Spawns | `EntityExplosiveMissile` | `EntitySurfaceToAirMissile` | `EntityHeldItemMissile` |
| Creative tab | the ICBM tab; the SAM is ordered explicitly in it | same | same |
| Oredict | none | none | none |

### English names

| Key | Text |
|---|---|
| `missile.<explosive registry key>.name` | one per explosive, listed in section 3; fallback `missile` has no `.name` of its own |
| `item.icbmclassic:surface_to_air_missile.name` | "Surface to Air Missile" |
| `item.icbmclassic:held_item_missile.name` | "Item Holder Missile" |
| `item.icbmclassic:held_item_missile.sword.name` | "Sword Missile" (used when the held item is any `ItemSword`) |

The unlocalized name of `ItemMissile` is `missile.` plus the explosive's registry key (for example `missile.icbmclassic:nuclear`), taken from the explosive capability; the item-wide fallback is `missile`.

## 2. Obtaining

Recipes live in `assets/icbmclassic/recipes/missile/`. They are shaped by custom recipe types that are built and registered in 06; this sheet lists them at catalog depth. Several have two variants, selected by a load condition on whether the oredict entries `circuitBasic` (and `ingotSteel`) exist. GregGuns replaces all of these with GT recipes (`docs/gregguns-icbm-spec`).

| Result | Recipe (file) | Notes |
|---|---|---|
| Missile module (item meta 24) | `missile.module.json` (steel and basic-circuit variant): ` i ` / `ici` / `ifi` with `i` = `ingotSteel`, `c` = `circuitBasic`, `f` = flint and steel. Fallback `vanilla/missile.module.json`: same shape with `ingotIron`, `redstone_block` in place of the circuit, and flint and steel (any damage) | The module is the base of all missiles |
| Explosive missile (any enabled explosive) | `ex/<name>.json`, 23 files: custom type `icbmclassic:explosive`, pattern `m x`: one **missile module** + one **explosive block** of that explosive (`device icbmclassic:block`) gives the missile of the same explosive | One recipe per explosive: antigravitational, antimatter, anvil, attractive, breaching, chemical, colors, condensed, contagious, debilitation, emp, ender, endothermic, exothermic, fragmentation, incendiary, nuclear, redmatter, repulsive, shrapnel, smoke, sonic, thermobaric |
| SAM | `special/missile.anti_ballistic.json` (with the circuit): `c` / `e` / `m` vertical, `c` = `circuitBasic`, `e` = `icbmclassic:explosives` meta 0 (condensed explosive block), `m` = missile module. Fallback `vanilla/missile.anti_ballistic.json` uses a repeater for `c` | The file name is the old "anti-ballistic" name |
| Held-item missile (empty) | `special/missile.held_item.json`: `e` / `c` / `m` vertical, `e` = sticky piston, `c` = `circuitBasic`, `m` = missile module | Only exists when `circuitBasic` exists (no vanilla fallback) |
| Held-item missile loaded | dynamic recipe `RecipeHeldItemMissile`: exactly two items, one empty or loaded held-item missile and one allowed item; the item order in the grid chooses the mode | below |
| Item recovered | dynamic recipe `RecipeHeldItemMissileDecraft`: a lone held-item missile that holds something returns the held stack and leaves an empty missile behind | below |

Creative tab: the missile item lists one stack per enabled explosive ID, then meta 24 (the missile module) as an explicit extra (a code comment calls this a workaround because the module has tier NONE and is not enabled for the missile content type).

### Dynamic held-item recipes

`RecipeHeldItemMissile` (output template: an empty held-item missile):
- Matches when the grid has exactly two stacks: one held-item missile and one item that passes the config ban/allow list (`item_ban_allow`; default ban list: balloon, parachute, cluster missile, explosive missile, SAM). A second missile, or a disallowed item, fails.
- The result is a **fresh copy of the template**, not of the input missile (bug 16). It holds a one-count copy of the cargo.
- Mode: scanning slots in index order, a cargo item that comes **after** the missile gives `SECONDARY` (right-click on impact); one that comes before gives `PRIMARY` (left-click). The `PRIMARY_FIRST` and `SECONDARY_FIRST` modes cannot be crafted (they are the default of a missile spawned empty).
- No container items are left behind.

`RecipeHeldItemMissileDecraft`:
- Matches only a single held-item missile of stack size 1 that holds something.
- Result = a copy of the held stack; the remaining item in the grid is the missile with its slot cleared.

## 3. Numbers

### Metadata to explosive map

The item meta equals the explosive ID assigned in `content/reg/ExplosiveInit.newEx`. Every explosive with tier other than NONE is enabled for missiles. Tier colors in tooltips: ONE green, TWO yellow, THREE gold, FOUR red.

| Meta | Registry key (`icbmclassic:`) | Tier | Name (`missile.<key>.name`) |
|---|---|---|---|
| 0 | `condensed` | ONE | Conventional Missile |
| 1 | `shrapnel` | ONE | Shrapnel Missile |
| 2 | `incendiary` | ONE | Incendiary Missile |
| 3 | `debilitation` | ONE | Debilitation Missile |
| 4 | `chemical` | ONE | Chemical Missile |
| 5 | `anvil` | ONE | Anvil Missile |
| 6 | `repulsive` | ONE | Repulsive Missile |
| 7 | `attractive` | ONE | Attractive Missile |
| 8 | `fragmentation` | TWO | Fragmentation Missile |
| 9 | `contagious` | TWO | Contagious Missile |
| 10 | `sonic` | TWO | Sonic Missile |
| 11 | `breaching` | TWO | Breaching Missile |
| 12 | (unused, comment "Regen") | | |
| 13 | `thermobaric` | TWO | Thermobaric Missile |
| 14 | (unused, comment "S-Mine") | | |
| 15 | `nuclear` | THREE | Nuclear Missile |
| 16 | `emp` | THREE | EMP Missile |
| 17 | `exothermic` | THREE | Exothermic Missile |
| 18 | `endothermic` | THREE | Endothermic Missile |
| 19 | `antigravitational` | THREE | Anti-Gravitational Missile |
| 20 | `ender` | THREE | Ender Missile |
| 21 | `hypersonic` | NONE | not a missile (no content enabled; a lang key remains) |
| 22 | `antimatter` | FOUR | Antimatter Missile |
| 23 | `redmatter` (declared as "redMatter"; resource keys are lowercased) | FOUR | Red Matter Missile |
| 24 | `missile` | NONE | Missile Module (`missile.icbmclassic:missile.name`) |
| 25, 26 | `rot`, `mutation` | NONE | not missiles |
| assigned | `colors`, `smoke` | ONE | Colorful Missile, Smoke Marking Missile. Their IDs are assigned dynamically by the explosive registry and persisted in `icbmclassic/explosive_reg.json` (02) |

The explosive registry, tiers and blast types are 02; the missile merely wraps the stack. The tier selects the (buggy) health, see missile-entity.md.

### Constants

| Name | Value |
|---|---|
| Held-item missile `held_item.block_damage_multiplier` | 4 |
| Held-item missile ban/allow list key | `icbmclassic/missile/held_item/item_ban_allow` (`ban` true = list is a ban list; default true) |
| Creative stacks of the held-item missile | empty, one holding a diamond sword, one holding shears |

## 4. Behavior

- **Missile item.** Not usable on its own (no right-click handler): it is consumed by launchers (04), the rocket launcher (part b), or by crafting. Its stack-capability `newMissile(world)` builds the entity and copies the stack into it; `getMissileId` is `icbmclassic:missile[<explosive key>]`.
- **SAM item.** `newMissile` returns a SAM entity. Used by launchers and rocket launcher like any missile. Launchers recognize it by item to use the SAM speed for their payload-velocity input (04).
- **Held-item missile item.** The capability holds a copy of the held stack and the action mode (`PRIMARY`, `SECONDARY`, `PRIMARY_FIRST`, `SECONDARY_FIRST`); `newMissile` copies both into the entity. The item shows the held item's durability bar. The name gains `.sword` when the held item is a sword.
- **Dropping.** A held-item missile entity that lands drops this item with the held stack as it ended (missile-entity.md).

## 5. Persistence and sync

| Key | Item | Meaning |
|---|---|---|
| `customizations` (list) | missile (explosive capability) | explosive customizations, for example ender coordinates (02) |
| `held` (item), `action_mode` (string, enum name) | held-item missile | stored stack and mode (the cap is serialized to the stack NBT by the capability provider) |

Capability storage uses empty `IStorage` stubs; the data rides in the item provider (*1.12-only mechanism*).

## 6. UI (tooltips)

`ItemBase.addInformation` (client) builds the tooltip in this order.

**Missile item**
1. Generic info: `missile.<key>.info`. No such lang keys exist, so nothing is added.
2. Detailed info (always on for this item): the block-explosive item's detailed-info routine, which adds the tier line when the explosive's tier is not NONE: dark red "Explosive tier" + ": " + the tier color + the tier name, for example "Explosive tier: Three" (`info.misc.tier`, `tier.icbmclassic.one` to `.four`). The module (tier NONE) shows no line. The red-matter taunt lines in that routine are unreachable for a missile because the explosive data is found first.
3. The explosive capability's `collectInformation` lines (explosive customizations; ender coordinates when set).

**SAM item:** `item.icbmclassic:surface_to_air_missile.info` = "Missile designed to fire from the ground to attack flying targets".

**Held-item missile item** (own `addInformation`)
- Empty: `item.icbmclassic:held_item_missile.info` = "Holds an item for use during impact." then "Craft to install item:" then "Missile + Item > Primary" and "Item + Missile > Secondary" (the lang string uses `\n` and `\t`).
- Loaded: one mode line, then a blank line, "Held Item" (`projectile.icbmclassic:holder.held`, underlined gray), the held stack's own tooltip, a blank line, and if the item is banned by config the red warning `projectile.icbmclassic:holder.disabled.config` ("Held item disabled by config, may not work correctly!!").
- Mode lines: `...held_item.primary` "Uses item's primary action on impact", `.secondary` "Uses item's secondary action on impact", `.primary_first` "Uses item's primary action, or secondary if nothing happens, on impact", `.secondary_first` the reverse.

## 7. Visuals

Item models are per-item JSON in `assets/icbmclassic/models/item/missiles/` (27 files: one per explosive plus `cluster`, `held_item`, `missile`, `surface_to_air`), block models built from boxes (the SAM model declares a 32 x 32 texture size; the others were not checked). Textures in `assets/icbmclassic/textures/items/missiles/`: per-explosive body textures (`condensed.png`, `nuclear.png`, `emp.png`, and so on, with `colorful.png` for colors, `replusive.png` (sic) for repulsive, `redmatter_body.png` and `redmatter_top.png`), `tier1.png`, `tier2.png`, `tier3.png` (shared body colors by tier), `missile_module.png`, `surface_to_air.png`, `cluster.png`. Sizes were not measured. The held-item missile model uses `tier1.png` and `smoke.png`. Models are registered per item and meta in `client/ClientReg` (07).

## 8. Audio

None.

## 9. Relationships

| Area | Link |
|---|---|
| 02-explosives | Explosive registry, IDs, tiers, the explosive capability on the stack, `ItemBlockExplosive` tooltip routine |
| 04-launch-control | Missile holders and launchers read `MISSILE_STACK_CAPABILITY` |
| 06-blocks-items | Recipe types, oredict and conditions, battery, other crafting items |
| 07-visuals-audio | Models, textures, lang |
| Part b | Cluster missile item, the rocket launcher, cargo items |

## 10. Bugs

Item-related: 16 (recipe), and the item side of 07 (ender missile cannot be set). See [index-a.md](index-a.md#suspected-bugs).
