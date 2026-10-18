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
| [`textures/items/ingotSteel.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Vanilla-style ingot: a slanted rectangular bar seen from above at an angle, with a dark outline and a light top edge | neutral greys, avg #565656, highlights ~#D0D0D0 | Steel Ingot |
| [`textures/items/ingotCopper.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Same ingot silhouette | golden ochre / yellow (avg #9B8232). Reads closer to gold or brass than to copper | Copper Ingot |
| [`textures/items/ingotTin.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Same ingot silhouette | pale cyan-white, avg #6F9792 | Tin Ingot |
| [`textures/items/ingotBronze.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Same ingot silhouette | dark orange-brown, avg #5E3707, highlights ~#C07020 | Bronze Ingot |
| [`textures/items/ingotLead.png`](../06-items/textures.md#5a-ingots-and-nuggets-vanilla-silhouettes-1616) | 16×16 | Same ingot silhouette | dark blue-grey, avg #40505C | Lead Ingot |
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

## Measured texture facts (consistency pass)

Numbers come from [ASSET-FACTS.md](../ASSET-FACTS.md) (average of opaque pixels with alpha above 16; the three dominant colours after bucketing each channel into 8 levels, so values land on multiples of 0x20 plus 0x10; "opaque" is the share of all pixels that are opaque) plus the opaque bounding box, which I measured from the file. The bounding box is the pixel rectangle (x, y, w, h) of everything with alpha above 16. All 19 core-owned textures are listed.

| # | Path | Size | Animation | Average | Dominant colours | Opaque | Opaque bounding box (x, y, w, h) |
|---|---|---|---|---|---|---|---|
| 1 | `textures/gui/TGPlayerInventory.png` | 256x256 | none | #868685 | #909090 45%, #D0D0D0 21%, #101010 14% | 48% | several islands, see [input-and-hud.md](input-and-hud.md#tgplayerinventorypng-pixel-regions-measured-consistency-pass) |
| 2 | `textures/entity/gore.png` | 128x128 | none | #761E18 | #701010 29%, #501010 19%, #903030 16% | 28% | (0, 0, 112, 64) |
| 3 | `textures/blocks/milk_still.png` | 16x512 | 32 frames, `frametime` 2 | #CDCDCD | #D0D0D0 100% | 100% | whole sheet; alpha ranges 168 to 226, so the fluid is translucent |
| 4 | `textures/blocks/milk_flow.png` | 32x1024 | 32 frames, empty `animation` object (default frametime 1) | #CDCDCD | #D0D0D0 100% | 100% | whole sheet; alpha ranges 165 to 211 |
| 5 | `textures/items/ingotSteel.png` | 16x16 | none | #565656 | #101010 31%, #707070 18%, #B0B0B0 13% | 54% | (0, 2, 16, 12) |
| 6 | `textures/items/ingotCopper.png` | 16x16 | none | #9B8232 | #F0D070 15%, #503010 13%, #907010 11% | 54% | (0, 2, 16, 12) |
| 7 | `textures/items/ingotTin.png` | 16x16 | none | #6F9792 | #305050 23%, #70B0B0 14%, #509090 10% | 54% | (0, 2, 16, 12) |
| 8 | `textures/items/ingotBronze.png` | 16x16 | none | #5E3707 | #503010 20%, #B07010 17%, #101010 16% | 54% | (0, 2, 16, 12) |
| 9 | `textures/items/ingotLead.png` | 16x16 | none | #40505C | #305050 16%, #303030 15%, #505070 15% | 54% | (0, 2, 16, 12) |
| 10 | `textures/items/nuggetIron.png` | 16x16 | none | #8F8F8F | #707070 38%, #F0F0F0 29%, #303030 20% | 13% | (5, 4, 6, 8) |
| 11 | `textures/items/copperNugget.png` | 16x16 | none | #BC934A | #907010 29%, #705010 20%, #F0F0B0 11% | 13% | (5, 4, 6, 8) |
| 12 | `textures/items/nuggetLead.png` | 16x16 | none | #455164 | #303050 29%, #707090 29%, #303030 20% | 13% | (5, 4, 6, 8) |
| 13 | `textures/items/nuggetSteel.png` | 16x16 | none | #4B4B4B | #101010 50%, #B0B0B0 29%, #909090 11% | 13% | (5, 4, 6, 8) |
| 14 | `textures/blocks/oreLeadBlock.png` | 16x16 | none | #7E8087 | #707070 51%, #909090 27%, #7090B0 5% | 100% | (0, 0, 16, 16) |
| 15 | `textures/blocks/oreTinBlock.png` | 16x16 | none | #838789 | #707070 49%, #909090 30%, #B0D0D0 5% | 100% | (0, 0, 16, 16) |
| 16 | `textures/blocks/oreCopperBlock.png` | 16x16 | none | #897D75 | #707070 48%, #909090 26%, #907070 8% | 100% | (0, 0, 16, 16) |
| 17 | `textures/items/structureTestTool.png` | 32x32 | none | #534444 | #101010 29%, #303030 12%, #909090 9% | 62% | (1, 1, 31, 31) |
| 18 | `textures/items/blockReaderTool.png` | 32x32 | none | #5E5E5F | #101010 20%, #707070 17%, #505050 14% | 62% | (1, 1, 29, 30) |
| 19 | `textures/items/structureScanTool.png` | 32x32 | none | #5A4C4C | #101010 36%, #909090 11%, #F0F0F0 7% | 50% | (1, 1, 29, 30) |

**Palette in words.**
- Ingots share one silhouette (a 16x12 bar from y = 2 to y = 13) and differ only in hue: steel is a dark neutral grey, copper a yellow-ochre, tin a teal-white, bronze a dark orange-brown, lead a dark slate blue. The 31 % black share of the steel ingot is its outline.
- Nuggets share one 6x8 silhouette centred at (5, 4); only 13 % of the tile is opaque. Iron and steel are grey (iron brighter), copper is orange-gold with a pale highlight, lead is blue-grey.
- Ore blocks are fully opaque vanilla-stone-style noise: about half of the pixels are mid grey (#707070), a quarter lighter grey (#909090), and 5 to 8 % are the ore flecks (blue-lavender for lead, pale cyan for tin, brown-pink for copper).
- Milk is a single flat off-white (#D0D0D0 bucket) at about 65 to 90 % alpha, animated only in subtle luminance noise.
- The three tool icons are dark outlined 32x32 sprites; the large black share (20 to 36 %) is the outline and the handle. They are the only core items that use 32x32 art.
- `gore.png` is dominated by dark red (#701010 and #501010 buckets) with lighter flesh (#903030); 72 % of the sheet is transparent because only the upper band is painted.

**Layout of the multi-region textures.**
- [`TGPlayerInventory.png`](../05-armor/extra-inventory.md#1-data-model-tgplayerinventory-iinventory-12-slots): full rectangle list in [input-and-hud.md](input-and-hud.md#tgplayerinventorypng-pixel-regions-measured-consistency-pass).
- `gore.png`: painted band (0, 0, 112, 64) on a 128x128 sheet. It follows the 64x32 player-skin layout at 2x scale (head, body, arm and leg strips); the rest is transparent.
- Milk sheets: the strip is `frames x frame height` tall, so milk_still has frames of (0, 16n, 16, 16) for n = 0..31 and milk_flow has frames of (0, 32n, 32, 32) for n = 0..31.

**Corrections to earlier numbers in this file** (previous averages were approximate, now replaced with the measured values): [`ingotSteel`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) #4E4E4E to #565656; [`ingotCopper`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) #937B2F to #9B8232; [`ingotTin`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) #688F8A to #6F9792; [`ingotBronze`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) #573306 to #5E3707; [`ingotLead`](../06-items/item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems) #3C4B57 to #40505C. The nugget size changes from "about 5x8" to 6x8.

## Measured sound facts and play parameters (consistency pass)

Duration, sample rate and channels are from [ASSET-FACTS.md](../ASSET-FACTS.md) (Ogg Vorbis headers). Category is the `category` field in `sounds.json`, which is the vanilla sound category, not the [`TGSoundCategory`](client-effects-audio.md#tgsoundcategory) ordinal. Where an event lists several files, vanilla picks one at random with equal weight each time the event plays. Descriptions of what a sound "sounds like" are not given because the audio cannot be listened to here; any inference is marked as such.

### Files

| File | Event (`sounds.json`) | Category | Duration | Sample rate | Channels |
|---|---|---|---|---|---|
| `sounds/items/nightvision_on.ogg` | `nightvision.on` | player | 1.60 s | 44100 Hz | mono |
| `sounds/items/nightvision_off.ogg` | `nightvision.off` | player | 0.44 s | 44100 Hz | mono |
| `sounds/effects/gore_a.ogg` | `effects.gore` | hostile | 2.99 s | 44100 Hz | mono |
| `sounds/effects/gore_b.ogg` | `effects.gore` | hostile | 2.99 s | 44100 Hz | mono |
| `sounds/effects/gore_c.ogg` | `effects.gore` | hostile | 2.99 s | 44100 Hz | mono |
| `sounds/effects/biodeath.ogg` | `effects.biodeath` | hostile | 3.99 s | 32000 Hz | mono |
| `sounds/effects/disintegrate.ogg` | `effects.disintegrate` | hostile | 5.29 s | 32000 Hz | mono |
| `sounds/items/jetpack_start_a.ogg` | `jetpack.start` | player | 1.29 s | 48000 Hz | mono |
| [`sounds/items/jetpack_start_b.ogg`](../06-items/sounds.md) | `jetpack.start` | player | 1.29 s | 48000 Hz | mono |
| [`sounds/items/jetpack_start_c.ogg`](../06-items/sounds.md) | `jetpack.start` | player | 1.29 s | 48000 Hz | mono |
| `sounds/items/jetpack_loop_a.ogg` | `jetpack.loop` | player | 1.10 s | 48000 Hz | mono |
| [`sounds/items/jetpack_loop_b.ogg`](../06-items/sounds.md) | `jetpack.loop` | player | 1.10 s | 48000 Hz | mono |
| [`sounds/items/jetpack_loop_c.ogg`](../06-items/sounds.md) | `jetpack.loop` | player | 1.10 s | 48000 Hz | mono |
| `sounds/items/jetpack_stop_a.ogg` | `jetpack.stop` | player | 1.40 s | 48000 Hz | mono |
| [`sounds/items/jetpack_stop_b.ogg`](../06-items/sounds.md) | `jetpack.stop` | player | 1.40 s | 48000 Hz | mono |
| [`sounds/items/jetpack_stop_c.ogg`](../06-items/sounds.md) | `jetpack.stop` | player | 1.40 s | 48000 Hz | mono |
| `sounds/items/glider_loop_stereo.ogg` | `glider.loop` | player | 9.41 s | 48000 Hz | **stereo** |
| `sounds/items/glider_loop.ogg` | none (unreferenced) | n/a | 3.02 s | 48000 Hz | mono |
| `sounds/items/antigrav_start_a.ogg` | `items.antiGravStart` | player | 1.81 s | 48000 Hz | mono |
| `sounds/items/antigrav_start_b.ogg` | `items.antiGravStart` | player | 1.81 s | 48000 Hz | mono |
| `sounds/test.ogg` | `test` | master | 1.23 s | 44100 Hz | mono |

That is 21 files. Of the 21, 20 are reachable from an event and 19 are reachable from code. `glider_loop.ogg` has no event; `test.ogg` has an event nothing plays.

### Events, exact triggers, volume and pitch

Parameter order for the proxy call is (name, volume, pitch, repeat, moving, gunPosition, category).

| Event | Exact trigger (code condition) | Played by | Volume | Pitch | Repeat | Moving | Notes |
|---|---|---|---|---|---|---|---|
| `techguns:nightvision.on` | [`PacketTGKeybindPress`](networking.md#7--packettgkeybindpress) id 0, after the flag becomes true and `getArmorBonusForPlayer(NIGHTVISION) > 0` | server, `world.playSoundAtEntity(player, ...)` | 1.0 | 1.0 | no | no | A vanilla world sound, so the server sends it to players within the vanilla radius for volume 1 (16 blocks), including the player. Does not use `TGSoundCategory` |
| `techguns:nightvision.off` | Same packet, flag becomes false **and** `getActivePotionEffect(Potion.nightVision) != null` (the potion is removed first) | server | 1.0 | 1.0 | no | no | Not played if the player has no Night Vision effect |
| `techguns:effects.gore` | [`PacketEntityDeathType`](networking.md#6--packetentitydeathtype) handler, death type GORE, [`EnableDeathEffects`](config.md#category-clientside) and [`EnableGoreDeathEffect`](config.md#category-clientside) both on, entity found | client, `proxy.playSoundOnEntity(entity, ...)` | 1.0 | 1.0 | no | no | gunPosition false; category DEATHEFFECT; plays at the entity (fixed, not following) |
| `techguns:effects.biodeath` | Same, death type BIO | client | 1.0 | 1.0 | no | no | category DEATHEFFECT |
| `techguns:effects.disintegrate` | Same, death type LASER | client | 1.0 | 1.0 | no | no | category DEATHEFFECT |
| `techguns:jetpack.start` | `setJumpkeyPressed(true)` on a client world while `jetPackLoop` is null | client, [`SoundUtil.playSoundAtEntityPos`](utilities.md#soundutil) | 1.0 | 1.0 | no | no (fixed at the player's position) | category PLAYER_EFFECT. For the local player the call comes from [`ItemJetpack`](../05-armor/special-equipment.md#43-jetpack-itemjetpack); for others from the field-sync packet |
| `techguns:jetpack.loop` | Created in the same call (`new TGSound`), started on the false-to-true edge via `getSoundHandler().playSound` | client | 1.0 | 1.0 | **yes** | **yes** (follows the player) | gunPosition false; stopped by `setDonePlaying()` on the true-to-false edge |
| `techguns:jetpack.stop` | `setJumpkeyPressed(false)` on a client world while the loop object exists | client, `SoundUtil.playSoundAtEntityPos` | 1.0 | 1.0 | no | no | category PLAYER_EFFECT |
| `techguns:glider.loop` | `ClientProxy.handlePlayerGliding` on a client world, `isGliding` true and `gliderLoop` null | client, new [`TGSound`](client-effects-audio.md#tgsound) | 1.0 | 1.0 | **yes** | **yes** | PLAYER_EFFECT. Stopped by `setDonePlaying()` when `isGliding` goes false. The 9.41 s stereo file is not positional ([CORE-31](index.md#suspected-bugs)) |
| `techguns:items.antiGravStart` | Tick handler, `CREATIVE_FLIGHT` bonus above 0, player is flying, `wasFlying` false, server side | server, [`PacketPlaySound`](networking.md#9--packetplaysound) to all within **50** blocks | 1.0 | 1.0 | no | yes | gunPosition false; `playOnOwnPlayer` **true**, so the flyer hears it too; PLAYER_EFFECT |
| `test` | nothing | none | n/a | n/a | n/a | n/a | Dead event |
| `random.eat` (vanilla) | Auto-feeder eats an item (server only) | server, `playSoundAtEntity` | 1.0 | 1.0 | no | no | Lines 387 and 429 of [`TechgunsTickHandler`](events.md#techgunstickhandler-fml-bus) |
| `gui.button.press` (vanilla) | [`TGGuiTabButton`](../05-armor/extra-inventory.md) click on an enabled tab | client, `PositionedSoundRecord.func_147674_a(location, 1.0F)` | fixed inside the vanilla helper (believed to be 0.25; not Techguns code) | 1.0 (the only argument Techguns passes) | no | no | Not played for the disabled (current) tab |

**Inferred, not heard.** From names and durations only: `nightvision_on` (1.60 s) is probably a power-up cue and `nightvision_off` (0.44 s) a shorter power-down cue; the three `jetpack_loop_*` clips (1.10 s each) are meant to be seamlessly repeated by the loop flag; `glider_loop_stereo.ogg` (9.41 s) is a long wind-like or engine-like loop. None of this is verified by listening.

**Category versus `TGSoundCategory`.** The `TGSoundCategory` value passed in the calls above (PLAYER_EFFECT, DEATHEFFECT) is only a label; [`TGConfig.getVolumeMultiplier`](config.md) returns the volume unchanged ([CORE-39](index.md#suspected-bugs)). The player-visible volume slider is the vanilla category in `sounds.json` ("player" for items, "hostile" for the three death sounds).

## Lang file index (consistency pass)

All keys are in `assets/techguns/lang/en_US.lang`, the only language file. Line numbers are those of that file in the reference build.

| Group | Keys (en_US.lang line) |
|---|---|
| Metal items | `techguns.item.ingotSteel.name` (5), `...ingotLead.name` (6), `...ingotCopper.name` (7), `...ingotTin.name` (8), `...ingotBronze.name` (9), `techguns.item.nuggetIron.name` (53), `...nuggetCopper.name` (54), `...nuggetLead.name` (55), `...nuggetSteel.name` (56) |
| Ores | `techguns.tile.oreTin.name` (69), `...oreCopper.name` (70), `...oreLead.name` (71) |
| Creative tab | `techguns.itemGroup.Techguns` (72) |
| Debug tools | `techguns.oreClusterDebugger.Tooltip` (467), `techguns.structureTestTool.Tooltip` (468), `techguns.item.structureTestTool.name` (469), `...buildingScanTool.name` (470), `...blockReaderTool.name` (471), `...oreClusterDebugger.name` (472) |
| Keybinds | `techguns.key.categories.techguns` (406), `...toggleNightvision` (407), `...toggleSafemode` (408), `...toggleStepassist` (409), `...toggleJetpack` (410), `...forceReload` (411) |
| Inventory toggles | `techguns.tgguitooltip.on` (387), `.off` (388), `.current` (389), then `.togglenv` (390), `.togglesafe` (391), `.togglestep` (392), their `.tooltip1`/`.tooltip2` (393-398), `.togglehud` (399) with tooltips (400-401), `.togglejetpack` (402) with tooltips (403-404) |
| Player inventory GUI extras | `techguns.tgguitooltip.totalarmorvalues` = Total Armor Values (405), `techguns.tgguitooltip.currentrad` = Current Radioactive Contamination (439), `techguns.tgguitooltip.radeffects` = Effects (440). Owned by [05-armor](../05-armor/index.md) but drawn on the core-owned texture |
| Generic GUI | `techguns.container.info.off` (197), `.on` (198), `techguns.button.switch` (199), `techguns.gui.empty` (627), `techguns.yes` (688), `techguns.no` (689) |
| Chat | `techguns.tgmessage.0` (515), `techguns.updateCheck.newVersion.beta` (691), `.stable` (692), `.download` (693), `.click` (694) |
| Tooltip helpers | `techguns.gun.tooltip.shift1` (597), `.shift2` (598), `.shift3` (599), `techguns.tooltip.toolclasses` (680) |
| Materials and fluid | `material.carbon` (672), `material.plastic` (673), `material.titanium` (674), `material.obsidiansteel` (675), `material.rubber.name` (676), `fluid.milk` (649) |
| Death messages (damage sources, owned by [02](../02-guns/index.md)) | `death.attack.tg_bullet` (99) and `.item` (100), `tg_explosion` (101-102), `tg_fire` (103-104), `tg_poison` (105-106), `powerhammer` (107-108), `tg_lightning` (109-110), `tg_energy` (111-112), [`chainsaw`](../02-guns/gun-catalogue.md#table-a-identity-and-ammunition) (113, no `.item` variant), `tg_sonic` (114), `tg_rad` (115) |

**Generated and hard-coded strings** (conditions):
- The update-check chat line 1 starts with the literal `§a[Techguns](<VERSION>): §f` before the translated text; the "§7>" prefix on line 2 is also literal.
- Debug keybind chat: `X:`, `Y:`, `Z:`, `XYZ: `, `Reloading FXList...`, `Removing all Particles...`, `Done!` (only with [`enableDebugKeybinds`](config.md#category-debug)).
- Log lines (never shown in chat): "Techguns is up to date.", "Could not check Techguns version: ...", "Got Packet for FX ... but ent was null".
- Tooltip helper: `Press §a[SHIFT] §7for detailed tooltip` is assembled from the three `shift` keys with literal colour codes; the Toolclasses list uses the literal `  <class> : <level>` format.
- Missing keys: `key.categories.techguns`, `key.+x`, `key.-x`, `key.+y`, `key.-y`, `key.+z`, `key.-z`, `key.show`, `key.reloadFX`, `key.killAllEntityFX` (debug keybinds, [CORE-29](index.md#suspected-bugs)); `death.attack.bullet` and `death.attack.bullet.item` ([CORE-34](index.md#suspected-bugs)); `death.attack.chainsaw` and `death.attack.tg_sonic` have no `.item` variant in the lang file, unlike the other damage types (whether the code ever asks for one was not traced).
