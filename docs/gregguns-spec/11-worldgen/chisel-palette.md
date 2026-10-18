# Structure palettes and Chisel

- **Techguns baseline:** [structure-catalog.md § Chisel aliases](../../techguns-spec/11-worldgen/structure-catalog.md), [structure-system.md § MBlock](../../techguns-spec/11-worldgen/structure-system.md#4-block-descriptors-mblock-multimblock-blockposind); bug [WGEN-24](../../techguns-spec/11-worldgen/index.md#suspected-bugs) (also CORE-03 in [BUGS.md](../../techguns-spec/BUGS.md))
- **Design records:** [0005](../../design/0005-world-presence.md), [0006](../../design/0006-art-direction.md)
- **Pack mods:** Chisel (`chisel`, 2.17.34-GTNH)
- **Roadmap:** M8

## What changes from Techguns

Techguns' scanned structures name Chisel blocks (`team.chisel.init.ChiselBlocks.*`) in static initializers, but the mod only declares `after:Chisel`. Without Chisel, structure generation throws `NoClassDefFoundError` (WGEN-24). Chisel is part of the GTNH pack, so the blocks exist there, but GregGuns still must not crash when it is absent (a dev instance, a stripped server).

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| Palette entries hold Chisel block references directly | Palette entries hold a **palette key** (`concrete`, `factory_wall`, …) resolved once at `postInit` to a block and meta | One place to change a block |
| No fallback | If `Loader.isModLoaded("chisel")` is false, or a Chisel field is null, the key resolves to a vanilla or GregGuns fallback block | Fixes WGEN-24 |
| Metal storage corner of Chisel lead, copper, tin, iron and gold blocks in the Medium House | The same keys, falling back to vanilla iron and gold blocks | Cosmetic only |

## Integration points

### Chisel: block field names

- **Pack config:** none.
- **Source:** [`ChiselBlocks`](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/init/ChiselBlocks.java) declares the fields Techguns' aliases use, as null placeholders filled at registration: `concrete` ([L57](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/init/ChiselBlocks.java#L57)), `stonebricksmooth` (L70), `factoryblock` (L95), `technical` (L112), `leadblock` (L126), `glass_pane` (L54), `limestoneStairs[]` (L150), `planks[]` (L157). `copperblock`, `tinblock`, `iron_block` and `gold_block` are also present.
- **GregGuns does:** reads each field by name at `postInit`, after Chisel has registered, and checks it for null. The soft dependency is guarded with `Loader.isModLoaded("chisel")`, and no GregGuns class that is loaded without Chisel references `team.chisel.*` directly (the resolver class does, and is loaded only inside the guard).
- **Pack-side change:** none.

## Requirements

- **GGS-11-25** No structure class references `team.chisel.*` outside the palette resolver.
- **GGS-11-26** With Chisel absent, every palette key resolves to a vanilla block and the structures still generate.
- **GGS-11-27** A palette key that resolves to null logs one warning with the key name and uses its fallback. It never throws during worldgen.
- **GGS-11-28** Structure data files and palette keys are validated at `postInit`. A missing file or a malformed header disables that structure and logs an error. It does not throw from a static initializer (WGEN-23).

## Tests

- GGS-11-25: source scan test.
- GGS-11-26, 27: unit test of the resolver with Chisel absent and with a null field.
- GGS-11-28: unit test with a missing and a truncated structure file.

## Open questions

- Which block set gives the "Techguns look" under [0006](../../design/0006-art-direction.md): Chisel variants or GregGuns' own concrete and lamp blocks (area 07)? A design record for 07 settles it. This sheet works either way, since only the palette keys change.
