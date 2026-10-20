# Bomb Cart

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. A **bomb cart** is a vanilla TNT minecart that carries one ICBM explosive instead of vanilla TNT. It is one item (`icbmclassic:bombcart`, metadata = explosive ID) and one entity. The blast it fires is a normal ICBM blast ([02-explosives](../02-explosives/blast-types.md)); this sheet covers the cart, its item, the dispenser behavior and the cart side of the fuse configuration. The grenade fuse table sits in the same config and is repeated in [thrown-and-handheld.md](thrown-and-handheld.md).

The cart does **not** use the projectile base. It extends vanilla `EntityMinecartTNT`, so everything the vanilla class does (and the 1.12 minecart physics) is inherited. Those inherited parts are not in this repository; they are listed in section 4 from the vanilla 1.12.2 class and were not re-read here.

Capabilities, entity registration and the config system are *1.12-only mechanisms*; the behavior they produce is described.

## 1. Identity

| Item | Value |
|---|---|
| Classes | `content/items/ItemBombCart`, `content/entity/EntityBombCart`, `content/items/behavior/BombCartDispenseBehavior`; registry class `lib/explosive/reg/ExMinecartContentReg`; config `config/blast/ConfigFuseTimes.BombCarts` (under `ConfigBlast.FUSE_TIMES`) |
| Item | `icbmclassic:bombcart`, max stack **3**, has subtypes, damage = explosive registry ID. Creative tab: the ICBM tab. Lists one stack per explosive enabled for carts |
| Entity | `icbmclassic:cart.bomb` (`ICBMEntities.BOMB_CART`), tracking range 50, update every 2 ticks. Sends its explosive stack in the spawn packet |
| Unlocalized name | `bombcart.<explosive registry key>` (for example `bombcart.icbmclassic:nuclear`); fallback `bombcart` |
| English names | `bombcart.icbmclassic:condensed.name` = "Explosive Cart"; shrapnel, incendiary, debilitation, chemical, anvil, repulsive, attractive, fragmentation, contagious, sonic, breaching, thermobaric, nuclear, emp, exothermic, endothermic, ender: "<Type> Minecart" (for example "Nuclear Minecart", "Shrapnel Minecart"); colors = "Colorful Minecart"; smoke = "Smoke Marking Minecart". Anti-gravitational = "Anti-Gravitational Minecart", antimatter = "Antimatter Minecart", red matter = "Red Matter Minecart". A "Hypersonic Minecart" name also exists although no such cart can be made. `bombcart.icbmclassic:nightmare.name` exists but the nightmare explosive does not (see appendix A2) |
| Tooltip | the explosive block's detail lines (the explosive's description and customization info). No cart-specific line |
| Which explosives get a cart | every explosive whose tier is not NONE: all 22 of tiers ONE to FOUR plus colors and smoke. Hypersonic, the missile module, rot and mutation (tier NONE) get none (`content/reg/ExplosiveInit.java:301-305`) |
| Oredict | none |

## 2. Obtaining

23 shaped recipes of one custom type, `icbmclassic:explosive` (`recipes/bombcarts/<type>.json`), each with the same grid: the explosive **block** of that type directly above a vanilla `minecraft:minecart`. Result: one bomb cart of that type. Types with a recipe: antigravitational, antimatter, anvil, attractive, breaching, chemical, colors, condensed, contagious, debilitation, emp, ender, endothermic, exothermic, fragmentation, incendiary, nuclear, redmatter, repulsive, shrapnel, smoke, sonic, thermobaric (23; the 24th cart type, hypersonic, is not allowed to exist).

Dispensers, loot and drops: the cart item also drops when the cart is broken (section 4).

## 3. Numbers

### Cart fuse times (`icbmclassic/blast` config, group `FUSE_TIMES`, subgroup `BOMB_CARTS`)

Ticks (20 = 1 second), integer range 0 to 2,147,483,647. The keys have no `@Config.Name`, so each key is the field name:

| Cart | Ticks |
|---|---|
| Conventional (`CONDENSED`), shrapnel, incendiary, debilitation, chemical, anvil | 100 |
| Repulsive, attractive | 120 |
| Fragmentation, contagious, sonic, thermobaric | 100 |
| Breaching | **0** (explodes on the first fuse tick) |
| Nuclear, EMP, exothermic, endothermic, anti-gravitational (`ANTI_GRAVITATIONAL`), ender | 100 |
| Antimatter | 300 |
| Red matter | 100 |
| `HYPERSONIC` | 100, but there is no hypersonic cart, so this key does nothing (ICBM-03-59) |
| Colors, smoke, any explosive added by another mod | no key; the registry default **100** |

The fuse is read through the cart registry (`EX_MINECART_REGISTRY.getFuseTime`) at the moment of ignition, so a config change applies to the next ignition.

### Entity

| Name | Value |
|---|---|
| Minecart physics, size, speed | vanilla (inherited) |
| Vanilla TNT cart wall/speed trigger | squared horizontal speed >= 0.01 (about 0.1 block/tick) |
| Blast radius scaling by speed | **none**: `explodeCart` ignores its speed argument |
| Fuse sound | vanilla `ENTITY_TNT_PRIMED` at volume 1, pitch 1, block category, unless the cart is silent |
| Dispenser offset | 1.125 blocks in front of the dispenser (x and z) |
| Dispenser height | rail level: +0.1 flat, +0.6 ascending; no rail but a rail below the target cell: -0.9 flat, -0.4 if the rail below ascends and the dispenser does not face down |

## 4. Behavior

### Placing on a rail (`ItemBombCart.onItemUse`)

1. The clicked block must be a rail block; otherwise the use fails and nothing happens.
2. Server: read the rail direction; spawn the cart at the block center, 0.0625 above the rail (+0.5 if the rail ascends). Copy the stack's custom name to the cart. The cart takes a one-count copy of the stack as its explosive.
3. The stack is reduced by one, on both sides, in every game mode (like the vanilla minecart).

### Dispenser (`BombCartDispenseBehavior`)

Registered for the bomb cart item only (not for any other cart). Takes the dispenser's facing and looks at the cell in front:
- A rail there: place the cart on it (height as in the table).
- Air in front with a rail below that: place the cart on the lower rail.
- Anything else: fall back to the vanilla "drop as an item" behavior.
Consumes one item and copies the stack's custom name.

### Igniting and the fuse

- `ignite()` (called by vanilla when an activator rail powers the cart, when fire or a flaming arrow hits it, and by other mods): set the vanilla fuse counter to the configured time for this explosive; server: send the "primed" entity-status byte (10) to clients and play the TNT-primed sound.
- Each tick the vanilla parent counts the fuse down and calls `explodeCart` when it reaches 0. Additionally, while ignited, the cart calls the cart registry's per-tick fuse callback with the **remaining** fuse value. No explosive registers a cart tick callback (the registry is empty), so this does nothing today (ICBM-03-59).
- Clients: the vanilla status handler sets the client-side fuse counter to a fixed 80, not the configured value, so any client-side fuse effect runs on an 80-tick scale (inherited, not in this source).

### Exploding (`explodeCart`)

Run the stack's explosive at the cart's position with the cart as the cause (a TODO notes that the trigger source and player are not passed), then `setDead`. The explosive capability's `isExploding` guard makes a second call a no-op. If the cart has no explosive stack, the capability falls back to the **conventional** explosive.

### Breaking (`killMinecart`, server only)

Called by the vanilla damage logic (for example enough accumulated damage in survival).
1. `setDead`.
2. If the damage is **not** an explosion: drop the cart item (an exact copy of the explosive stack). The drop call also redirects any vanilla TNT drop into the cart item (`entityDropItem` override).
3. If the damage is fire or explosion, **or** the cart's squared horizontal speed is at least 0.01: explode (as above).

So a fast-moving cart broken by a player both drops its item and explodes (ICBM-03-58). An explosion kills the cart without a drop but still sets it off.

### Inherited vanilla triggers (unverified, from the 1.12.2 base class)

The parent cart also explodes on a hard wall impact (horizontal collision at speed >= 0.1 block/tick, no explosion radius scaling in ICBM's override), after a fall of 3 or more blocks, and when a burning or flaming-arrow damage source hits it. These are inherited unchanged and call the same `explodeCart`.

## 5. Persistence and sync

| Where | Key | Type | Meaning |
|---|---|---|---|
| Cart entity | `explosive` | compound | the explosive stack's NBT (item id, count 1, metadata, tag) |
| Cart entity (legacy) | `explosive` as a number | int | pre-4.x save: the explosive ID; read as a bomb cart stack of that metadata. Chosen by the NBT type of the same key (`NBTConstants.EXPLOSIVE`) |
| Cart entity | vanilla keys | | `TNTFuse` and the rest of the minecart save |
| Spawn packet | explosive stack | item stack | written with `writeItemStack`; read back with `setStack` |

The explosive stack is never changed after spawning. `datafix/EntityBombCartDataFixer` (1.12-only mechanism, outside this part) upgrades old saves.

## 6. UI

None. Chat and messages: none. The tooltip is the explosive's detail lines, as for a grenade or a block.

## 7. Visuals

- Item: each metadata uses `models/item/bombcarts/<type>.json`, all of which are `item/generated` with the **vanilla** `minecraft:items/minecart_tnt` texture. There is no ICBM art for the item.
- Entity: the vanilla minecart renderer draws the cart body and the contents block comes from `getDefaultDisplayTile`: the explosive block with the cart's explosive property and facing UP (conventional when unset). The cart therefore looks like the matching explosive block riding a minecart.
- Fuse: vanilla flashing/smoke on the cart (inherited).

## 8. Audio

Fuse: vanilla TNT-primed sound (above). The blast sound belongs to the explosive.

## 9. Relationships

| Direction | What |
|---|---|
| Needs | the explosive registry and `EX_MINECART_REGISTRY` ([02-explosives](../02-explosives/explosive-registry.md)); the explosive capabilities (`CapabilityExplosiveStack` on the item, `CapabilityExplosiveEntity` on the entity); `ConfigBlast.FUSE_TIMES` |
| Used by | dispensers; activator rails; the defuser: `onDefuse` drops the explosive stack and removes the cart ([05-detection-defense](../05-detection-defense/handheld-tools.md)); the `EMP` cart is a normal cart whose blast runs the EMP action ([emp-tower.md](../05-detection-defense/emp-tower.md)) |
| GregGuns note | Owner keeps the bomb cart as a full feature. A GT rebuild needs only the item, the entity (a 1.7.10 TNT minecart subclass), the dispenser hook and a config table; the fuse table can come from the explosive's own definition |

## 10. Bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-03-58 | Breaking a moving cart drops the item and explodes (logic) | `content/entity/EntityBombCart.java:68-84` | The drop is skipped only for explosion damage. Fire damage or a speed of 0.1 block/tick or more also explodes, so the player gets the whole cart item (possibly a nuclear one) back and the blast. The vanilla TNT cart behaves the same with its TNT, so the rebuild decides whether to keep it | gameplay |
| ICBM-03-59 | Dead and missing cart fuse entries (logic) | `config/blast/ConfigFuseTimes.java:82`, `content/entity/EntityBombCart.java:93`, `content/reg/ExplosiveInit.java:301-308` | A `HYPERSONIC` cart fuse key exists but hypersonic cannot be a cart. Colors and smoke carts (and grenades) have no key. The per-tick fuse callback is wired for carts (and grenades) but no explosive ever registers one (only the block registry has two), and the cart passes the remaining fuse while the grenade passes ticks lived | cosmetic |

Minor, not given IDs: the cart ignores its speed argument, so a fast cart does not hit harder than a stopped one (unlike vanilla); the item hard-codes max stack 3; the lang file has a leftover `nightmare` entry; `ConfigFuseTimes` uses non-static nested classes with fields named in capitals (the config keys are therefore the raw field names).
