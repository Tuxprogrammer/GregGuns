# Textures, sounds and lang owned by core

Paths are relative to `assets/techguns`. "Item" and "Block" textures are registered as icons (`techguns:<name>` → `textures/items/<name>.png` or `textures/blocks/<name>.png`).

## Textures

### GUI / HUD

| Path | Size | Anim | Depicts / layout | Palette | Used by |
|---|---|---|---|---|---|
| [`textures/gui/TGPlayerInventory.png`](../08-machines-production/gui-textures.md#14-texturesguitgplayerinventorypng-owned-by-the-player-inventory-area-0105) | 256×256 | no | Techguns player-inventory panel (0, 0, 176×166), tab backgrounds at (177, 0) and (177, 26) (26×26 each), tab connector strip (213, 0, 3×26), a 2-column HUD icon sheet at u = 242/249 (off/on), v = 0, 7, 14, 21, 28, 35, 42 (7×7 each), and the 8×8 armor-point icons at (0/8/16, 168). Full region table in [input-and-hud.md](input-and-hud.md#tgplayerinventorypng-layout) | vanilla GUI greys, black outlines, red #FF0000, greens #00C60E / #00FF12, yellow #FFFF00 | HUD overlay, armor bar, inventory tab buttons, Techguns player inventory GUI (ID 17); also referenced as [`ClientProxy.player_inv_texture`](lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy) |

### Gore

| Path | Size | Anim | Depicts / layout | Palette | Used by |
|---|---|---|---|---|---|
| [`textures/entity/gore.png`](../10-entities-npcs/textures.md#non-mob-textures-stored-in-texturesentity) | 128×128 | no | A generic "raw flesh" biped skin. Only the top half (0, 0)–(112, 64) is painted; the rest is transparent. It follows the standard 64×32 player-skin layout at 2× scale: head cube at the top left (u 0–64, v 0–32), body, arm and leg strips in the band v 32–64. The surface is mottled dark-red muscle with brighter blood splotches and a few dark "eye socket" spots on the head front | maroon #761E18 average, highlights ~#C81E1E, shadows ~#3A0A0A | [`DeathEffect`](client-effects-audio.md#deatheffectcreatedeatheffectentity-type-mx-my-mz) generic gore entry: the gib texture for any entity class without its own gore data |

### Milk fluid icons

These are registered by the core `TextureStitchEvent` handler only when Techguns adds its own milk fluid. The fluid itself is documented in [07-blocks](../07-blocks/index.md).

| Path | Size | Anim | Depicts | Palette | Used by |
|---|---|---|---|---|---|
| [`textures/blocks/milk_still.png`](../07-blocks/textures.md#9-fluids) | 16×512 | 32 frames of 16×16, `frametime` 2 | Still milk surface: near-uniform off-white with faint blotchy luminance noise drifting between frames (a recoloured still-water animation) | light grey-white ~#CDCDCD, ~70 % alpha (avg 178/255) | icon `techguns:milk_still` → [`TGFluids.MILK`](../06-items/fluids.md) still icon |
| [`textures/blocks/milk_flow.png`](../07-blocks/textures.md#9-fluids) | 32×1024 | 32 frames of 32×32, `animation {}` (frametime 1) | Flowing milk: off-white with faint vertical streaks scrolling down (a recoloured water flow) | ~#CDCDCD, ~69 % alpha | icon `techguns:milk_flow` → `TGFluids.MILK` flowing icon |

### Metal items, ores and debug tools registered in `Techguns.java`

All are 16×16 (the tools are 32×32), not animated, with transparent backgrounds except the ore blocks, which are fully opaque.

| Path | Size | Depicts | Palette | Used by |
|---|---|---|---|---|
| [`textures/items/ingotSteel.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Vanilla-style ingot: a slanted rectangular bar seen from above at an angle, with a dark outline and a light top edge | neutral greys, avg #4E4E4E, highlights ~#D0D0D0 | Steel Ingot |
| [`textures/items/ingotCopper.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Same ingot silhouette | golden ochre / yellow (avg #937B2F). Reads closer to gold or brass than to copper | Copper Ingot |
| [`textures/items/ingotTin.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Same ingot silhouette | pale cyan-white, avg #688F8A | Tin Ingot |
| [`textures/items/ingotBronze.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Same ingot silhouette | dark orange-brown, avg #573306, highlights ~#C07020 | Bronze Ingot |
| [`textures/items/ingotLead.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Same ingot silhouette | dark blue-grey, avg #3C4B57 | Lead Ingot |
| [`textures/items/nuggetIron.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Small rounded nugget (~5×8 px) in the centre with a dark outline and a top highlight | light grey, avg #8F8F8F | Iron Nugget |
| [`textures/items/copperNugget.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Same nugget shape | orange-gold, avg #BC934A | Copper Nugget |
| [`textures/items/nuggetLead.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Same nugget shape | blue-grey, avg #455164 | Lead Nugget |
| [`textures/items/nuggetSteel.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Same nugget shape | dark grey, avg #4B4B4B, white highlight | Steel Nugget |
| [`textures/blocks/oreLeadBlock.png`](../07-blocks/textures.md#7-ores) | 16×16 | Vanilla-stone-style grey noise with scattered small flecks | stone greys + pale blue/lavender flecks, avg #7E8087 | Lead Ore block (all faces) |
| [`textures/blocks/oreTinBlock.png`](../07-blocks/textures.md#7-ores) | 16×16 | Stone with flecks | stone greys + pale cyan-white flecks, avg #838789 | Tin Ore |
| [`textures/blocks/oreCopperBlock.png`](../07-blocks/textures.md#7-ores) | 16×16 | Stone with flecks | stone greys + orange-brown flecks, avg #897D75 | Copper Ore |
| [`textures/items/structureTestTool.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items) | 32×32 | A grey wrench and a black-headed hammer standing upright behind a small red-brick wall segment | greys, red brick #8A2A2A, black | Structure Test Tool ([06](../06-items/index.md)) |
| [`textures/items/blockReaderTool.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items) | 32×32 | A magnifying glass (grey ring, white-grey lens, diagonal handle to the lower right) over a grey stone cube | greys, white lens highlight | Block Reader Tool ([06](../06-items/index.md)) |
| [`textures/items/structureScanTool.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items) | 32×32 | The same magnifying glass over a red-brick wall segment | greys, red brick | Building Scan Tool ([06](../06-items/index.md)) |

### Textures referenced but owned elsewhere

These are only referenced from the core renderer and particle tables and are described by their owning areas:
- `textures/fx/*` (legacy particle sheets): [04](../04-projectiles-fx/index.md)
- gun textures under `textures/guns/`: [03](../03-gun-visuals-audio/index.md)
- entity, NPC and projectile textures: [04](../04-projectiles-fx/index.md), [10](../10-entities-npcs/index.md)
- machine block textures: [08](../08-machines-production/index.md), [09](../09-machines-utility/index.md)
- the vanilla textures used by the gore table

## Sounds

The full list of sounds played by core code, with trigger conditions, is in [client-effects-audio.md](client-effects-audio.md#sounds-played-by-core-code). Files:

| Path | sounds.json event |
|---|---|
| [`sounds/items/nightvision_on.ogg`](../06-items/sounds.md) | `nightvision.on` |
| [`sounds/items/nightvision_off.ogg`](../06-items/sounds.md) | `nightvision.off` |
| [`sounds/effects/gore_a.ogg`](../04-projectiles-fx/sounds.md), [`sounds/effects/gore_b.ogg`](../04-projectiles-fx/sounds.md), [`sounds/effects/gore_c.ogg`](../04-projectiles-fx/sounds.md) | `effects.gore` |
| [`sounds/effects/biodeath.ogg`](../04-projectiles-fx/sounds.md) | `effects.biodeath` |
| [`sounds/effects/disintegrate.ogg`](../04-projectiles-fx/sounds.md) | `effects.disintegrate` |
| [`sounds/items/jetpack_start_a.ogg`](../06-items/sounds.md), `_b.ogg`, `_c.ogg` | `jetpack.start` |
| [`sounds/items/jetpack_loop_a.ogg`](../06-items/sounds.md), `_b.ogg`, `_c.ogg` | `jetpack.loop` |
| [`sounds/items/jetpack_stop_a.ogg`](../06-items/sounds.md), `_b.ogg`, `_c.ogg` | `jetpack.stop` |
| [`sounds/items/glider_loop_stereo.ogg`](../06-items/sounds.md) | `glider.loop` |
| [`sounds/items/glider_loop.ogg`](../06-items/sounds.md) | (none; unreferenced mono version) |
| [`sounds/items/antigrav_start_a.ogg`](../06-items/sounds.md), [`sounds/items/antigrav_start_b.ogg`](../06-items/sounds.md) | `items.antiGravStart` |
| `sounds/test.ogg` | `test` (category `master`). A leftover placeholder: no code plays it. The only reference is a commented-out `techguns:test` block texture in [`TGBlocks.java:162`](../07-blocks/block-catalogue.md#registered-blocks). It doesn't need recreating. |

## Lang keys (core-owned)

### Creative tab, keybinds, chat

| Key | English |
|---|---|
| `techguns.itemGroup.Techguns` | Techguns |
| `techguns.key.categories.techguns` | Techguns |
| `techguns.key.toggleNightvision` | Toggle Nightvision |
| `techguns.key.toggleSafemode` | Toggle Safe Mode |
| `techguns.key.toggleStepassist` | Toggle Step Assist |
| `techguns.key.toggleJetpack` | Toggle Jetpack |
| `techguns.key.forceReload` | Force Reload |
| `techguns.tgmessage.0` | Invalid Block for Reaction Chamber at |
| `techguns.updateCheck.newVersion.beta` | New beta version is available! |
| `techguns.updateCheck.newVersion.stable` | New stable version is available! |
| `techguns.updateCheck.download` | Click to open the download page |
| `techguns.updateCheck.click` | Click to download |

### Tooltip helpers (`ItemUtil`)

| Key | English |
|---|---|
| `techguns.gun.tooltip.shift1` | Press |
| `techguns.gun.tooltip.shift2` | [SHIFT] |
| `techguns.gun.tooltip.shift3` | for detailed tooltip |
| `techguns.tooltip.toolclasses` | Toolclasses |

**Code-generated lines:**
- `"Press §a[SHIFT] §7for detailed tooltip"`, shown when Shift isn't held.
- Under "Toolclasses", one line `"  <toolclass> : <harvest level>"` per tool class.

### Items and blocks registered in `Techguns.java`

| Key | English |
|---|---|
| `techguns.item.ingotSteel.name` | Steel Ingot |
| `techguns.item.ingotLead.name` | Lead Ingot |
| `techguns.item.ingotCopper.name` | Copper Ingot |
| `techguns.item.ingotTin.name` | Tin Ingot |
| `techguns.item.ingotBronze.name` | Bronze Ingot |
| `techguns.item.nuggetIron.name` | Iron Nugget |
| `techguns.item.nuggetCopper.name` | Copper Nugget |
| `techguns.item.nuggetLead.name` | Lead Nugget |
| `techguns.item.nuggetSteel.name` | Steel Nugget |
| `techguns.tile.oreTin.name` | Tin Ore |
| `techguns.tile.oreCopper.name` | Copper Ore |
| `techguns.tile.oreLead.name` | Lead Ore |
| `techguns.item.structureTestTool.name` | Structure Test Tool |
| `techguns.item.buildingScanTool.name` | Building Scan Tool |
| `techguns.item.blockReaderTool.name` | Block Reader Tool |
| `techguns.item.oreClusterDebugger.name` | Ore Cluster Debugger |

The tool tooltips `techguns.structureTestTool.Tooltip` and `techguns.oreClusterDebugger.Tooltip` are emitted by the item classes ([06](../06-items/index.md)).

### Generic GUI strings and player-inventory toggles

| Key | English |
|---|---|
| `techguns.yes` / `techguns.no` | Yes / No |
| `techguns.gui.empty` | Empty |
| `techguns.button.switch` | Switch |
| `techguns.container.info.on` / `.off` | On / Off |
| `techguns.tgguitooltip.on` / `.off` | Enabled / Disabled |
| `techguns.tgguitooltip.current` | Current state |
| `techguns.tgguitooltip.togglehud` | Toggle HUD Icons |
| `techguns.tgguitooltip.togglehud.tooltip1` / `2` | Shows icons for current / " states in the players HUD" |
| `techguns.tgguitooltip.togglenv` | Toggle Nightvision |
| `techguns.tgguitooltip.togglenv.tooltip1` / `2` | Enables nightvision if / " provided by armor" |
| `techguns.tgguitooltip.togglesafe` | Toggle Safe Mode |
| `techguns.tgguitooltip.togglesafe.tooltip1` / `2` | In safe mode guns don't / " destroy or ignite blocks." |
| `techguns.tgguitooltip.togglestep` | Toggle Step Assist |
| `techguns.tgguitooltip.togglestep.tooltip1` / `2` | Allows to step up a whole / " block if provided by armor" |
| `techguns.tgguitooltip.togglejetpack` | Toggle Jetpack |
| `techguns.tgguitooltip.togglejetpack.tooltip1` / `2` | Enables your Jetpack / " when you jump" |

The second tooltip lines start with a leading space, so the two halves concatenate into one sentence.

### Integrations and fluid

| Key | English |
|---|---|
| `material.plastic` | Plastic |
| `material.carbon` | Carbon |
| `material.titanium` | Titanium |
| `material.obsidiansteel` | Obsidian Steel |
| `material.rubber.name` | Rubber |
| `fluid.milk` | Milk |

### Missing or untranslated keys used by core code

- Debug keybinds: the category `key.categories.techguns` and the names `key.+x`, `key.-x`, `key.+y`, `key.-y`, `key.+z`, `key.-z`, `key.show`, `key.reloadFX`, `key.killAllEntityFX`. They show as raw keys ([CORE-29](index.md#suspected-bugs)).
- `death.attack.bullet` and `death.attack.bullet.item` from `BulletDamageSource` ([CORE-34](index.md#suspected-bugs)).
- Hard-coded English in chat: the debug keybind messages, "Techguns is up to date." (log only), and `UNKNOW_PLAYERNAME`.
