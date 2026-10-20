# Explosive Block, Tile and Fuse

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. One block, `icbmclassic:explosives`, stands in for all 23 block-form explosives. The explosive is *not* in the block's metadata: it lives in the tile entity's item stack. When triggered the block turns into a falling, bouncing primed entity (`EntityExplosive`) that explodes when its fuse runs out. This sheet covers the block, its item, the tile, the triggers and the fuse. The blast that finally runs is in [blast-engine.md](blast-engine.md), [threaded-blasts.md](threaded-blasts.md) and part b. The catalog and fuse defaults are in [explosive-registry.md](explosive-registry.md).

## 1. Identity

| Item | Value |
|---|---|
| Classes | `content/blocks/explosive/BlockExplosive`, `ItemBlockExplosive`, `TileEntityExplosive`, `PropertyExplosive`; base `prefab/tile/BlockICBM`; item base `prefab/item/ItemBlockAbstract`; the primed entity `content/entity/EntityExplosive` (only its fuse and tick logic are described here) |
| Registry names | block `icbmclassic:explosives`; item the same name; tile entity `icbmclassic:explosive` (`BlockReg.java:101`); unlocalized name `tile.icbmclassic:explosives` for the block, but the item uses `explosive.<registry key>` (for example `explosive.icbmclassic:condensed`) |
| Metadata | **Block meta = facing** (`EnumFacing` ordinal 0 to 5: down, up, north, south, west, east). **Item damage = explosive ID** (0 to 23, see the registry). The two are unrelated |
| English names | `explosive.icbmclassic:<name>.name`, for example "Condensed Explosives". Full list in [explosive-registry.md](explosive-registry.md#7-lang) |
| Creative tab | the ICBM tab; the sub-block list is every ID enabled for the **block** form (23 entries); the item also lists on the search tab |
| Oredict | none |
| Blockstate (*1.12-only mechanism*) | two properties: `rotation` (the six facings) and `explosive` (an `IProperty` over the registry's explosive data; the string form replaces `:` with `_`). Only `rotation` is stored in the world. `explosive` is **derived at render time** from the tile (see `getActualState`) |
| Material and feel | `Material.TNT`, sound type cloth, hardness **2** (the constructor sets the base class's 10, then overrides to 2), resistance 10, full opaque cube, solid on every side |

## 2. Obtaining

Crafted. The 23 shaped recipes (`recipes/explosives/*.json`, plus `vanilla` and `mekanism` variants for nuclear, antimatter and red matter) are tabulated in [explosive-registry.md](explosive-registry.md#block-recipes-compact). Creative: the tab. The pack will replace these recipes (see `gregguns-icbm-spec`).

Breaking the placed block: `damageDropped` returns the explosive ID read from the *raw* block state, which has no explosive stored (ICBM-02-22). Pick-block (middle click) uses the actual state and gives the correct item.

## 3. Numbers

| Name | Value |
|---|---|
| Hardness / resistance | 2 / 10 |
| Fuse (block form) | per explosive, from `FUSE_TIMES.EXPLOSIVES`; default 100 ticks; fallback 100 for `colors` and `smoke` |
| Fuse range | configurable 0 to 2,147,483,647; but only 0 to 127 survives a save (ICBM-02-20) |
| Primed entity size | 0.98 x 0.98 |
| Initial entity velocity | horizontal: a random direction at 0.02 blocks per tick; vertical +0.2 |
| Per-tick motion | x and z multiplied by 0.95; y decreases by 0.045; on ground, x and z multiplied by 0.7 and y reversed and halved (a small bounce) |
| Flame-spawned fuse | `setFire(100)` (burning 5 seconds) when ignited by adjacent fire or lava at placement |
| Detonation center | entity position + (0.5, 0.5, 0.5) (ICBM-02-27) |
| Fuse tick listener (block form) | `incendiary`, `exothermic`: a lava particle at the entity every tick (both sides) |

The full per-explosive fuse table is in the registry sheet.

## 4. Behavior

### Placement

1. `ItemBlockAbstract.onItemUse`: the usual rules (replaceable target, player can edit, `mayPlace`). The new state's facing is the **clicked face** (not the player's facing), and the explosive for the state is looked up from the held stack's damage value. If that ID is unknown, the code logs an error, resets the stack's damage to 0 and uses condensed.
2. `onBlockPlacedBy` (server and client): creates the tile's explosive capability from a copy of the placed stack. Then:
   - if the block is receiving redstone power (`isBlockPowered`), **trigger now** (with no fire);
   - else for each of the four **horizontal** neighbors, if it is fire, flowing lava or lava, **trigger with fire** (the `break` stops after the first).
   - logs "`<player>` placed `<explosive key>` in: `<pos>`" at info level.
3. `onBlockAdded` is deliberately empty (the comment says the blockstate has no explosive yet at that point).

Blocks placed without `onBlockPlacedBy` (commands, structure code, other mods) get a tile with an **empty stack**, which reads as explosive ID 0 (condensed) and has an empty stack to throw.

### Triggers

| Trigger | Where | Effect |
|---|---|---|
| Redstone | `neighborChanged`: any neighbor change while `isBlockPowered` | trigger, no fire |
| Right-click with flint and steel or fire charge | `onBlockActivated` | trigger, no fire; flint and steel loses 1 durability; a fire charge is consumed unless the player is in creative. (The tile is triggered on the server only, the item use runs on both sides.) |
| Burning arrow | `onEntityCollidedWithBlock` (server): an arrow entity that `isBurning` | trigger, no fire. Whether a sticking arrow actually reaches this callback was not verified |
| Any explosion | `onBlockExploded`: called when a vanilla or ICBM explosion removes the block | trigger, then the vanilla removal. **This is the chain reaction:** a neighboring threaded blast's destroy step calls it on each explosive block it removes, and each becomes a primed entity with its own full fuse |
| Fire or lava next to it at placement | `onBlockPlacedBy` (horizontal neighbors only) | trigger with fire. **Not** checked later: a block that has fire or lava placed next to it afterward does nothing |
| Sonic blast | `BlastSonic` calls the tile's `trigger(false)` directly | part b |
| Remote detonator, laser detonator, defuser | `05-detection-defense` | not re-described here; whether they act on the block, the entity or the capability was not checked in this part |

### `trigger(setFire)` (tile)

Once only (`hasBeenTriggered` guards, but it is not saved):

- **Breaching** (compared by explosive identity): creates the blast **immediately** at the block center via the registry factory with an action source of the block position and *no cause*, passing the field `HOST_DIRECTION` = the **opposite** of the block's facing (so a breaching charge placed on the floor fires into the floor), and calls `doAction`. No entity, no fuse, no fire handling. The comment calls this a "temp work around for direction being funky".
- **Everything else**: spawn an `EntityExplosive` at the block center (+0.5 on each axis) carrying the block's facing and a one-count copy of the stack; if `setFire`, `setFire(100)` on it.
- Then `setBlockToAir(pos)` (the block disappears, no drop), and an info log "`TileEntityExplosive: Triggered ITEM[stack] <key> at location <pos>`".

### Primed entity and fuse (`EntityExplosive`)

Server and client each tick:

```
apply gravity/drag (see Numbers), move, bounce
if fuse == -1: fuse = getFuseTime(world, pos, explosiveID)       # first tick only; per-ID supplier or 100
tickFuse(world, pos, fuse, explosiveID)                           # registry callback (lava particle for incendiary/exothermic)
if (fuse-- < 1): explode()
```

So a fuse of N explodes on tick N + 1 after the first update (a fuse of 0 explodes on the first update). `explode()` asks the entity's explosive capability to run the blast at (x + 0.5, y + 0.5, z + 0.5) with an `EntityCause` action source, then removes the entity.

The entity is a solid, pushable, collidable entity (other entities and players can shove it). It also carries an EMP capability (`CapabilityEmpKill`) whose name says an EMP pulse kills the entity (the class was not read; see [emp-tower.md](../05-detection-defense/emp-tower.md)). Defusing is in `05-detection-defense/handheld-tools.md`.

### Ender special

The registry attaches an activation listener for the ender explosive that was meant to let a GPS tool bind a target to the block. The block never calls it (ICBM-02-17), and a customization added directly to the tile's capability would not survive a save or the conversion to an entity (ICBM-02-23).

## 5. Persistence and sync

| Where | Key | Type | Meaning |
|---|---|---|---|
| Tile NBT | `explosive_stack` | compound | the one-count item stack (id, damage = explosive ID, count, tag). Written only if the capability has a stack |
| Tile sync | `getUpdateTag` / update packet | the whole tile NBT | lets the client derive the `explosive` blockstate property |
| Block state | meta 0 to 5 | int | facing; `rotation` property |
| Tile (not saved) | `hasBeenTriggered` | boolean | a trigger flag that resets on reload (harmless: the block is gone after a trigger) |
| Entity NBT | `Fuse` | **byte** | remaining fuse; truncates above 127 (ICBM-02-20) |
| Entity NBT | `explosive_stack` | compound | the capability's own save (`customizations` list; see `01-core`) |
| Entity spawn data | fuse (int), facing (byte), capability tag | | sent to clients |
| Item | capability provider | | `ItemBlockExplosive.initCapabilities` attaches a `CapabilityExplosiveStack` so item stacks can carry customizations (for example an ender target) in their NBT |
| Datafixers | `TileExplosivesDataFixer`, `ItemStackDataFixer`, `EntityExplosiveDataFixer` | | upgrade pre-stack saves that used an explosive ID instead of a stack (*1.12-only mechanism*) |

## 6. UI

No GUI. Tooltip lines (`ItemBlockAbstract.addInformation`, client):

1. the flavor text from `explosive.icbmclassic:<name>.info`, if the key exists (see the registry sheet's list);
2. `ItemBlockExplosive.getDetailedInfo`: the line "`Explosive tier`: `<tier name>`" (label dark red, tier colored: One green, Two yellow, Three gold, Four red), except for tier NONE;
3. a joke line block for red matter exists but is unreachable (ICBM-02-19).

The same tooltip method is reused by the grenade, bomb cart, missile and bomblet items. Customization info (for example the ender target lines `explosive.icbmclassic:ender.pos`, `.world`) is exposed by the capability but this item class does not print it.

## 7. Visuals

- Blockstate files: `blockstates/explosives.json` (the main file, Forge marker format) and `blockstates/explosives/<name>.json`, 23 files, one per block-form explosive. Each is a `cube_all` model with a per-face texture map and the six rotation variants: up (none), down x180, north x90, south x270, east x90 y90, west x90 y270. The main file's default textures name `explosive_base_1`, `explosive_bottom_1` and so on, which no longer exist (stale placeholders; the per-explosive files override them). The renderer maps (explosive, rotation) to a baked model (`BlockModelMapperExplosive`, `ItemModelMapperExplosive`; *1.12-only mechanism*), and a primed entity draws the same block via `client/render/entity/RenderExBlock`. These client classes were not read in full.
- Textures: `textures/blocks/explosive/`, 16 x 16 unless noted.

| Texture | Used by |
|---|---|
| `condensed_side` / `condensed_top` | condensed sides / top (bottom face `bottom_1`) |
| `shrapnel_side` | shrapnel sides; its top face uses `bottom_1`, so `shrapnel_top` is unused |
| `breaching_side` / `breaching_top` | breaching sides / top (bottom face `bottom_2`) |
| `incendiary`, `debilitation`, `chemical`, `anvil`, `repulsive`, `attractive`, `colorful`, `smoke` | one texture on the four sides; top and bottom `bottom_1` |
| `fragmentation`, `contagious`, `sonic`, `thermobaric` | sides; top and bottom `bottom_2` |
| `nuclear`, `emp`, `exothermic`, `endothermic`, `anti_gravitational`, `ender` | sides; top and bottom `bottom_3` |
| `bottom_1`, `bottom_2`, `bottom_3` | the tier-grouped top and bottom faces above |
| `antimatter` (16 x 320) | antimatter top and sides, animated: 20 frames, frametime 2, played 0 to 19 and back down to 1 (a 38-entry frame list). Its bottom face is `redmatter_bottom` |
| `redmatter_top` (16 x 320), `redmatter_side`, `redmatter_bottom` | red matter; the top is animated like antimatter |

The `explosive` variant key in each blockstate file is `icbmclassic_<name>` (the registry key with `:` replaced by `_`).

- Fuse effects: lava particle for incendiary and exothermic only. No flashing or hissing is coded in the block, tile or entity logic read. `RenderExBlock` may add a visual; not read in full.
- Item models: `models/item/` (not enumerated).

## 8. Audio

- Placement uses the block's sound type (cloth): `((volume + 1) / 2, pitch x 0.8)` from `ItemBlockAbstract.onItemUse`.
- No fuse sound in the block, tile or entity logic read. The detonation sounds belong to the blast ([threaded-blasts.md](threaded-blasts.md#11-audio) for the three heavy ones; others in part b).

## 9. Relationships

- Uses: the explosive registry and `IExplosive` capability (`01-core`, `CapabilityExplosive*`), `BlockReg` (registration), the EMP capability, the blast factory.
- Used by: threaded blasts (chain reaction through `onBlockExploded`), `BlastSonic` (calls `trigger`), the remote detonator, laser detonator and defuser (`05-detection-defense`), cluster missiles and bomb carts (these have their own entities but the same explosive IDs, `03-missiles`, `06-blocks-items`).
- Compare: the bomb cart's fuse comes from `FUSE_TIMES.BOMB_CARTS` and the grenade's from `GRENADES`. Their entities are in `06-blocks-items`.

## 10. Bugs

ICBM-02-17 (ender activation listener never called), 02-20 (fuse saved as a byte), 02-22 (drop uses raw state), 02-23 (tile customizations lost), 02-27 (detonation center shifted). The table is in `index.md`.
