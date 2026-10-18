# Decorative blocks: Chisel, connected textures and overlap

- **Techguns baseline:** [connected-textures.md](../../techguns-spec/07-blocks/connected-textures.md), [registration flow](../../techguns-spec/07-blocks/connected-textures.md#registration-flow-per-block), [block-catalogue.md](../../techguns-spec/07-blocks/block-catalogue.md), [multipart.md](../../techguns-spec/07-blocks/multipart.md)
- **Design records:** [0006 art direction](../../design/0006-art-direction.md), [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (the Techguns machines list, not blocks)
- **Pack mods:** Chisel (`chisel`, 2.17.34-GTNH), ForgeMultipart (`ForgeMultipart`, 1.7.16), ChiselTones (`chiseltones`, 1.2.0-GTNH), Ztones (`Ztones`, 2.2.2, no source)
- **Roadmap:** the blocks milestone in [ROADMAP](../../ROADMAP.md)

## What changes from Techguns

Techguns reaches into the Chisel API for its decorative blocks. GregGuns keeps that, but as a **soft** dependency on the client and for carving, and picks its own group names.

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| Chisel is a hard client dependency (CORE-03) | Chisel is optional. Blocks render without connected textures if it is missing | The pack has Chisel, but a dev environment or other pack may not |
| `registerAll(block, "concrete")` puts Techguns concrete in the group named `concrete`, which is also **Chisel's own concrete group** | Group names are prefixed `gregguns_` so the two sets stay separate | Chisel registers its own `concrete` group. Both sets are different art, so they should not cycle into each other |
| `TGBlocks.chiselRegistry` is a dead stand-in registry | Removed. Use `Carving.chisel` from the API | Dead code |
| Spec says no block is a microblock material | Registration through the helper does make normal-cube variants microblock materials when ForgeMultipart is loaded (see below) | Observed in Chisel's source; Techguns got this without knowing |

## Overlap with the pack

| GregGuns / Techguns block | Pack equivalent | Decision |
|---|---|---|
| Concrete (7 variants) | Chisel `concrete` (11 variants, [Features.java#L850-L868](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java#L850-L868)) | Keep ours. Different art; kept in a separate group |
| Metal panels, connected metal panels | ChiselTones and Ztones (`Ztones.cfg` enables most colour sets) | Keep ours; art follows [0006](../../design/0006-art-direction.md). Nothing to call |
| Shipping containers | none | Keep |
| Hardened bug sand | none | Keep, or drop with alien nests in area 11 |
| Camo nets, sandbags, ladders, lamps, doors, radar | none | Keep |
| Ore clusters, ore blocks | GT ores | Dropped by [0005](../../design/0005-world-presence.md). No ore blocks of any kind |

The pack config for the overlapping mods is `reference/gtnh/2.9.0-RC-2/.minecraft/config/chisel.cfg`, `ChiselTones.cfg` and `Ztones.cfg`. None needs a change.

## Integration points

### Chisel: carving groups and connected textures
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/chisel.cfg`, `client.connectedTextures=true`, `client.connectInsideCTM=false`.
- **Source:**
  - [CarvableHelper.registerAll / registerVariation](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/com/cricketcraft/chisel/api/carving/CarvableHelper.java#L164-L206) renames the block to `chisel.<name>`, registers it, and adds every variation to the carving group `<name>`. `forbidChiseling` (L204) skips the carving step.
  - [IMC.java#L21](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/com/cricketcraft/chisel/api/IMC.java#L21) defines the `variation:add` IMC message (`group|modid:block|meta`). [IMCHandler.java#L27-L47](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/compat/IMCHandler.java#L27-L47) handles it.
  - [ICarvingRegistry](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/com/cricketcraft/chisel/api/carving/ICarvingRegistry.java) is the registry API.
- **GregGuns does:**
  - Block classes that use Chisel's rendering are in a client-only class loaded only when `Loader.isModLoaded("chisel")`.
  - Variations are added with `CarvableHelper` under a `gregguns_<name>` group. Machine and multiblock blocks set `forbidChiseling = true`, as Techguns does.
  - Cross-mod grouping, if ever wanted, goes through the `variation:add` IMC message, not through Chisel internals.
  - The lang keys follow the helper's rename: `tile.chisel.<name>.name`. The Techguns lang key table must be re-derived for our names.
- **Pack-side change:** none.

### ForgeMultipart: microblock side effect
- **Source:** [CarvableHelper.registerVariation](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/com/cricketcraft/chisel/api/carving/CarvableHelper.java#L188-L203) calls `FMPIMC.registerFMP(block, meta)` for each normal, opaque cube when ForgeMultipart is loaded. [FMPIMC.java](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/com/cricketcraft/chisel/api/FMPIMC.java) sends the `ForgeMicroblock` IMC `microMaterial`.
- **GregGuns does:** accepts it for concrete, metal panels and containers. Blocks that must not be microblocks (anything with a tile entity or a custom shape) are not registered through `registerAll`.
- **Pack-side change:** none.

## Requirements

- **GGS-07-01** The mod loads and every decorative block places and renders (without connected textures) when Chisel is absent.
- **GGS-07-02** Carving groups are named `gregguns_<name>`. None is named `concrete` or any other Chisel group name.
- **GGS-07-03** Machine, multiblock and tile-entity blocks set `forbidChiseling = true`.
- **GGS-07-04** No Chisel or ForgeMultipart class is referenced from a class that loads on a dedicated server without the mod.
- **GGS-07-05** No ore block, ore cluster or ore-gen helper is registered.

## Tests

| Req | Check |
|---|---|
| 01, 04 | `runServer` boot, plus a client boot with Chisel removed |
| 02, 03 | Unit test over the registered groups |
| 05 | Unit test that lists registered blocks |

## Open questions

- Should the metal panels also join a Chisel group, so players can chisel to them? This sheet says no. A decision on art integration with the pack would settle it.
- The Ztones overlap is by look only. Nothing in this sheet depends on it.
