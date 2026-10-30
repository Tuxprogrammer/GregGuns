# Structure palettes and Chisel

- **Techguns baseline:** [structure-catalog.md § Chisel aliases](../../techguns-spec/11-worldgen/structure-catalog.md), [structure-system.md § MBlock](../../techguns-spec/11-worldgen/structure-system.md#4-block-descriptors-mblock-multimblock-blockposind); bug [WGEN-24](../../techguns-spec/11-worldgen/index.md#suspected-bugs) (also CORE-03 in [BUGS.md](../../techguns-spec/BUGS.md))
- **Design records:** [0005](../../design/0005-world-presence.md), [0006](../../design/0006-art-direction.md), [0025](../../design/0025-structure-palettes.md)
- **Pack mods:** Chisel (`chisel`, 2.17.34-GTNH)
- **Roadmap:** M8

## What changes from Techguns

Techguns' scanned structures name Chisel blocks (`team.chisel.init.ChiselBlocks.*`) in static initializers, but the mod only declares `after:Chisel`. Without Chisel, structure generation throws `NoClassDefFoundError` (WGEN-24). Chisel is part of the GTNH pack, so the blocks exist there, but GregGuns still must not crash when it is absent (a dev instance, a stripped server).

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| Palette entries hold Chisel block references directly | Palette entries hold a **palette role** (`concrete`, `factory_wall`, …) resolved once at `postInit` to a block and meta | One place to change a block |
| No fallback | Each role is a chain of candidates of one look, best first: a Chisel variant, sometimes a GregTech block, then a vanilla stand-in. The first registered candidate wins | Fixes WGEN-24 |
| Weighted entries (`MultiMBlock`) mix any blocks | A weighted entry draws from one family of roles only (plus air for gaps) | Deviation (0025): the cross-family mixes read as speckle |
| Hazard trim is Chisel's hazard stripe | Same; no role is a sign, an icon block or an animated overlay | 0025 |
| Metal storage corner of Chisel lead, copper, tin, iron and gold blocks in the Medium House | The same keys, falling back to vanilla iron and gold blocks | Cosmetic only |

## Integration points

### Chisel: block registry names and variations

- **Pack config:** none (`chisel.cfg` enables every block used here, including `hempcrete`).
- **Source:** [`Features.java`](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java) registers each block under its carver name (`registerAll(block, "factoryblock")`) and its looks by meta (`addVariation(..., meta, texture)`); the role table cites the lines per block. A wrapped vanilla block's meta 0 (glass pane, gold block, bookshelf, the wood planks) has no variation.
- **GregGuns does:** (`PaletteKeys`) looks each Chisel candidate up by registry name (`GameRegistry.findBlock("chisel", name)`, null when absent) at `postInit`, after Chisel has registered, guarded with `Loader.isModLoaded("chisel")`. No GregGuns class names `team.chisel.*`. A Chisel meta that Chisel defines no variation for (`ChiselVariants`, generated from `Features.java`) is skipped, since it would draw Chisel's missing texture.
- **Pack-side change:** none.

## Roles and families

The role table in `PaletteKeys` ([0025](../../design/0025-structure-palettes.md)). Chisel metas are from `Features.java` at 2.17.34-GTNH; GT concrete metas from GT's lang file.

| Family | Role | Pack block (first candidate) | Then |
|---|---|---|---|
| concrete | `concrete` | Chisel concrete 0 | GT light concrete (8), stone bricks |
| concrete | `concrete_panel` | Chisel concrete 1 (block) | GT smooth light concrete (15), stone bricks |
| concrete | `concrete_dark` | Chisel concrete 4 (weathered) | GT cracked light concrete bricks (12), cracked stone bricks |
| concrete | `asphalt` | Chisel concrete 10 (asphalt) | GT dark concrete (0), smooth stone slab |
| masonry | `brick`, `brick_worn`, `brick_trim` | Chisel stone bricks 5 (large), 7 (chaotic), 12 (hard panel) | stone bricks, cracked stone bricks, chiseled stone bricks |
| red_brick | `red_brick`, `red_brick_worn` | Chisel bricks 5 (aged), 13 (sooty) | vanilla bricks |
| steel | `factory_wall` | Chisel factory 3 (plate) | smooth stone slab |
| steel | `plating` | Chisel factory 13 (plating) | smooth stone slab |
| steel | `factory_floor` | Chisel technical 13 (grate) | smooth stone slab |
| steel | `hull_dark` | Chisel laboratory 11 (large steel) | stone bricks |
| rust | `rust_wall` | Chisel factory 1 (rust) | hardened clay |
| hazard | `hazard` | Chisel factory 6 (hazard stripe) | yellow stained clay |
| lamp | `lamp` | Chisel light stone 4 (panel, light 15) | glowstone |
| glass | `glass_pane` | Chisel glass pane 2 (no border) | glass pane |
| wood | `crate` | Chisel spruce planks 10 (crate) | spruce planks |
| camo | `camo_woodland`, `camo_desert`, `camo_snow` | oak leaves; Chisel desert camo (rebel 0); snow | sandstone (desert) |
| sandbag | `sandbag` | Chisel hempcrete 12 (brown) | brown wool |
| container | `container` | Chisel hempcrete 14 (red) | red wool |
| metal_storage | `lead_block`, `copper_block`, `tin_block`, `gold_plate` | Chisel metal storage blocks | iron, gold, clay |
| bugnest | `bugnest_wall`, `bugnest_egg`, `bugnest_slime` | GregGuns nest blocks | sandstone, clay, vines |

Rules, checked by `StructurePaletteRulesTest` and at load:

- a weighted entry's choices share one family (air allowed; the train station's track bed may mix gravel and cobblestone);
- a role key takes no `#meta` in a layout (`LayoutRegistry` rejects it): a meta means something else for each block of a chain;
- no role names a sign, icon or animated block (Chisel warning signs, hex plating, lanterns);
- the first candidate of a role, the block the pack gets, is not one GregTech's stone layers overwrite (stone, stained clay, cobblestone, GT granites and stones), and no layout names one directly except the track bed;
- every chain ends in a vanilla block.

## Palette review

`worldgen.palette_showcase` (test hooks, not in `test all`) builds every surface structure at rotation 0 and three Military Bases (woodland, desert, snow camo) on flat lots from x 60000, z 60000, through the real populator and the pack's resolved roles, and logs each box. `worldgen.palette_showcase_cam` moves a player and sends client signals from `showcase-cam.txt`, so a script can frame the same cameras before and after a palette change.

## Requirements

- **GGS-11-25** No GregGuns class references `team.chisel.*`; Chisel blocks are found by registry name.
- **GGS-11-26** With Chisel absent, every palette key resolves to a vanilla block and the structures still generate.
- **GGS-11-27** A palette key that resolves to null logs one warning with the key name and uses its fallback. It never throws during worldgen.
- **GGS-11-28** Structure data files and palette keys are validated at `postInit`. A missing file or a malformed header disables that structure and logs an error. It does not throw from a static initializer (WGEN-23).

## Tests

- GGS-11-25: source scan test.
- GGS-11-26, 27: unit test of the resolver with Chisel absent and with a null Chisel block (`PaletteKeysTest`).
- Roles and families: `StructurePaletteRulesTest`, `ChiselVariantsTest` (every Chisel candidate names a defined variation); in the pack, scenario `worldgen.palette_roles` (every Chisel role got its Chisel block).
- GGS-11-28: unit test with a missing and a truncated structure file.

## Open questions

- None. Which block set gives the Techguns look is settled by [0025](../../design/0025-structure-palettes.md): Chisel variants of one family per structure, with GT and vanilla behind them.
