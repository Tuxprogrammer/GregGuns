# Lang keys and generated text (06-items)

Item display names are in [item-catalog.md](item-catalog.md): every row gives its key (`techguns.item.<name>.name`) and its English text. This page covers the other strings, the text generated in code, and the lang-file anomalies.

<a id="generated-tooltips"></a>
## Tooltip lines

| Item | Line(s) | Key(s) → English | Driver |
|---|---|---|---|
| Magazines (shared metas 2, 5, 12, 17, 20, 23, 46) | 2 lines | `techguns.item.ammotooltip.ammobag` → "Right click to put in ammo inventory"; `techguns.item.ammotooltip.empty` → "Sneak + Right click to empty" | meta is in the `magazineTooltip` list |
| Ore drill heads (75–83) | `<size label> <dims>` | `techguns.oreDrillTooltip.size` → "Valid for drills with size", followed by the literal ` 1x1x1` / ` 1x1xN or 3x3xN` / ` 5x5xN or 7x7xN` | head size (small / medium / large) |
| | `<level label>: <n>` | `techguns.oreDrillTooltip.mininglevel` → "Bonus mining level", followed by `: 1` / `: 2` / `: 3` | material (steel / obsidian steel / carbon) |
| Ore Cluster Scanner | `<energy>/30000 RF` | literal | NBT `Energy` |
| Combat Knife, Crowbar | `DamageType: <type>` | prefix is literal; type = colored `techguns.TGDamageType.PHYSICAL` → "Physical" (dark gray) | `dmgType` field |
| | `ArmorPen: <x>%` | literal | only when penetration > 0 (never, for these two) |
| Crowbar, Shift held | header + one line per class + note | `techguns.tooltip.toolclasses` → "Toolclasses"; lines `  default : 2`, `  pickaxe : 2`, `  wrench : 2` (HashMap order); `techguns.tooltip.crowbar.destroycluster` → "Can destroy Ore Clusters" | Shift key down (client keyboard poll) |
| Crowbar, no Shift | 1 line | `techguns.gun.tooltip.shift1` "Press" + green `techguns.gun.tooltip.shift2` "[SHIFT]" + gray `techguns.gun.tooltip.shift3` "for detailed tooltip" | |
| Structure Test Tool | 1 line | `techguns.structureTestTool.Tooltip` → "Tool for structure spawning/world editing [WIP]" | always |
| Ore Cluster Debugger | 1 line | `techguns.oreClusterDebugger.Tooltip` → "WARNING: Removes all destroyable Blocks in a huge Radius, used to debug Ore cluster spawns" | always |

Other damage-type strings exist for [02](../02-guns/index.md) and are shown by the same `toString`: PROJECTILE "Projectile" (gray), FIRE "Fire" (red), EXPLOSION "Explosion" (dark red), ENERGY "Energy" (dark aqua), POISON "Poison" (dark green), ICE "Ice" (aqua), LIGHTNING "Lightning" (yellow), RADIATION "Radiation" (green). UNRESISTABLE (black) uses the key `techguns.TGDamageType.UNRESISTABLE`, which has **no** entry in `en_US.lang`.

## Chat messages

| Source | Text (key → English) |
|---|---|
| Camo Net Placer, mode change | `techguns.item.camoNetPlacer.info.msg` "Placing with Camo:" + space + `techguns.item.camoNetPlacer.info.camo.<n>`: 0 "Woodland", 1 "Snow", 2 "Desert" |
| Camo Net Placer | `…info.pos1set` "Position 1 set to:" + ` (x/y/z).` |
| | `…info.clear` "Cleared Position 1" |
| | `…info.noplane` "Can only Place in a Plane" |
| | `…info.maxBlock.1` "Can only Place a maximum of" + ` 64 ` + `…info.maxBlock.2` "in a single use." |
| | `…info.notEnough` "Not enough camo net blocks in inventory." |
| | `…info.placed` "Camo net placed." |
| Ore Cluster Scanner | `techguns.OreClusterScanner.msg.found` "Found Ore Clusters in this Chunk", then one line per cluster type (translated `<clusterBlockUnlocalized>_<meta>.name`, from [07](../07-blocks/index.md)/[11](../11-worldgen/index.md)) |
| | `techguns.OreClusterScanner.msg.notFound` "No Clusters in this Chunk" |
| Building Scan / Structure Test Tool (literal, not localized) | `Position1 set (x/y/z).`, `Position2 set (x/y/z).`, `Size: (sx/sy/sz).`, `structure tool mode set to "<mode>".` |
| Block Reader (literal) | `Block:<id> - Meta: <m> - Side: <s>`; `Door: i1 = … - j1 = … - flag = …`; `Blockbounds: X:a,b\|Y:c,d\|Z:e,f` |

## Other names used by items in this area

| Key | English | Note |
|---|---|---|
| `techguns.itemGroup.Techguns` | Techguns | creative tab |
| `techguns.item.bunkerDoor.name` | Bunker Door | door item |
| `techguns.item.camoNetPlacer.name` | Camo Net Placer | all metas |
| `techguns.item.combatKnife.name` | Combat Knife | |
| `techguns.item.crowbar.name` | Crowbar | |
| `techguns.item.oreClusterScanner.name` | Ore Cluster Scanner | |
| `techguns.item.bucket_acid.name` | Creeper Acid Bucket | |
| `techguns.item.structureTestTool.name` | Structure Test Tool | |
| `techguns.item.buildingScanTool.name` | Building Scan Tool | |
| `techguns.item.blockReaderTool.name` | Block Reader Tool | |
| `techguns.item.oreClusterDebugger.name` | Ore Cluster Debugger | |
| `techguns.item.medkit_0.name` / `_1.name` | Bandages / Medkit | item not registered |
| `techguns.item.gasMask.name` … `antiGravPack.name` | see the catalog | [05](../05-armor/index.md) items |
| Spawn eggs | (none) | the display name is generated as `Spawn <EntityName>` |
| `techguns.tile.camoNetPane_<0/1/2>.name`, `techguns.tile.camoNetRoof_<n>.name` | Camo Net (Woodland/Snow/Desert), Camo Net Roof (…) | blocks consumed by the placer ([07](../07-blocks/index.md)) |

## Lang anomalies (cosmetic)

| Key | Issue |
|---|---|
| `techguns.item.lmgMagEmpty.name= LMG Magazine (Empty)` | leading space in the value |
| `techguns.item.ironGunstock=Iron Stock` (line 12) | missing `.name`. The correct key also exists on line 34; the item is not registered anyway |
| `techguns.item.rocketAmmo.name` | defined twice (lines 21 and 68, same text) |
| `techguns.item.40mmgrenade.name` | lower-case duplicate of `techguns.item.40mmGrenade.name`; unused |
| `techguns.item.shotgunshell.name` | lower-case duplicate; unused |
| `techguns.item.testItem/testIngot/testGun/testMag.name`, `fireMechanism*`, `rubberDrop` | keys for items that are not registered |
| `techguns.fluid.creeperAcid` | not used by code (`fluid.creeperAcid` is the key used) |
| `techguns.item.minigunDrum_empty.name` | "(empty)" in lower case, unlike every other "(Empty)" |
| `techguns.item.gliderBackback.name` | "Backback" misspelling, matching the internal name |
