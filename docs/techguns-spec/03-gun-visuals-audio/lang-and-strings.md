# Lang keys and strings in this area

Every lang key and generated string that belongs to the gun visuals and audio area, with the exact English text from `assets/techguns/lang/en_US.lang` (the only language file shipped). Gun names, stat tooltips and the zoom tooltip belong to [02-guns](../02-guns/index.md); the same camo mechanism for armour belongs to [05-armor](../05-armor/index.md). The renderers, models and `sounds.json` contribute no strings of their own: no renderer draws text, and 1.7.10 has no sound subtitles.

## 1. Camo name keys (guns)

The display name of camo index *i* (greater than 0) is built as `<unlocalizedName>.camoname.<i>`. The unlocalized name is `techguns.item.<registry name>`. Index 0 never uses a per-gun key: it shows `techguns.item.defaultcamo`. The index equals the position in the gun's texture list ([rendering-framework.md §8](rendering-framework.md#8-textures-and-camouflage), [textures.md](textures.md)).

| Lang key | English | Line in `en_US.lang` | Texture it names |
|---|---|---|---|
| `techguns.item.boltaction.camoname.1` | Olive | 178 | [`textures/guns/boltactionrifle_1.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.boltaction.camoname.2` | Tactical Black | 179 | [`textures/guns/boltactionrifle_2.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.m4.camoname.1` | Swamp | 487 | [`textures/guns/m4Texture_1.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.m4.camoname.2` | Desert | 488 | [`textures/guns/m4Texture_2.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.m4.camoname.3` | Ice | 489 | [`textures/guns/m4Texture_3.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.m4.camoname.4` | Hazard | 490 | [`textures/guns/m4Texture_4.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.ak47.camoname.1` | Blue | 486 | [`textures/guns/ak47Texture_1.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.pdw.camoname.1` | Silver | 482 | [`textures/guns/PDW_1.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.pdw.camoname.2` | Dark Green | 483 | [`textures/guns/PDW_2.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.pulseRifle.camoname.1` | Grey | 484 | [`textures/guns/pulseRifle_1.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.pulseRifle.camoname.2` | Dark Green | 485 | [`textures/guns/pulseRifle_2.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.aug.camoname.1` | Grey | 505 | [`textures/guns/AugTexture_1.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.aug.camoname.2` | Wood | 506 | [`textures/guns/AugTexture_2.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.aug.camoname.3` | Sand | 507 | [`textures/guns/AugTexture_3.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.aug.camoname.4` | Nether | 508 | [`textures/guns/AugTexture_4.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.scar.camoname.1` | Black | 654 | [`textures/guns/scar_texture_1.png`](textures.md#measured-colours-all-62-textures) |
| `techguns.item.vector.camoname.1` | Black | 653 | [`textures/guns/vector_texture_1.png`](textures.md#measured-colours-all-62-textures) |

17 per-gun camo keys cover all 8 camo-capable guns. The default texture of each gun is index 0 and shows "Default Camo".

## 2. Generic camo strings

| Lang key | English | Line | Where it is used for guns |
|---|---|---|---|
| `techguns.item.defaultcamo` | Default Camo | 182 | Camo name for index 0 ([`GenericGun.getCurrentCamoName`](../02-guns/gun-lifecycle.md), shown by the Camo Bench GUI) |
| `techguns.item.invalidcamo` | No Camo | 181 | Camo Bench GUI label when the input slot is empty |
| `techguns.container.camobench` | Camo Workbench | 180 | Camo Bench container title (area [09-machines-utility](../09-machines-utility/index.md)) |
| `techguns.tooltip.currentcamo` | Current Camo | 131 | Armour and special-slot tooltip only. **No gun tooltip line uses it** (see the correction below) |
| `techguns.message.camoswitch` | Switched Camo to | 130 | Armour camo-switch chat message only. Guns send no chat message |

## 3. Strings generated in code

| String | Produced by | Condition |
|---|---|---|
| `<camo name>` for a gun | `GenericGun.getCurrentCamoName(stack)` | The `camo` byte NBT tag is greater than 0: the translated `<unlocalizedName>.camoname.<i>`. Tag absent or 0: the translated `techguns.item.defaultcamo` |
| `(<index+1>/<camoCount>):` followed by the camo name | Camo Bench GUI foreground | A camo-changeable item is in the bench slot. Counting is 1-based in the GUI, 0-based in NBT |
| `No Camo` | Camo Bench GUI foreground | The bench slot is empty (the count prefix is blank) |
| Scope overlay, muzzle flash, reload and fire animations | – | **No text.** Scopes draw the textures named in [rendering-framework.md §9](rendering-framework.md#9-zoom-and-scopes). Nothing is translated |

## 4. Correction

[rendering-framework.md §8](rendering-framework.md#8-textures-and-camouflage) says the camo tooltip line is "built in area [02](../02-guns/index.md)". It is not: `GenericGun` adds no camo line to its tooltip. `techguns.tooltip.currentcamo` and `techguns.message.camoswitch` are only referenced by the armour class [`GenericArmorMultiCamo`](../05-armor/armor-catalogue.md#1-class-hierarchy) (area [05](../05-armor/index.md)). A gun's camo is changed only through the Camo Bench ([`CamoBenchTileEnt`](../09-machines-utility/index.md#key-facts) calls `switchCamo`) and is visible only in that GUI and in the model texture. There is no keybind, chat message or tooltip for gun camos.

## 5. Missing keys, typos and hard-coded English

| Item | Finding |
|---|---|
| Missing camo keys | **None.** Every texture variant index of the 8 camo guns has a key (17 keys for 17 non-default variants) |
| Unused camo keys | None for guns. Index-0 keys are never defined for guns (not needed) |
| Typos | `techguns.gun.tooltip.zoom.toogle` ("toogle" in the key; the English value "toggle" is correct) belongs to [02-guns](../02-guns/index.md) |
| Hard-coded English in renderers, models or sound code | **None found.** |
| Sound events | `sounds.json` events (`guns.*`) are not lang keys and have no subtitle entries |
| Other languages | Only `en_US.lang` exists, so there are no translated gun camo names |
