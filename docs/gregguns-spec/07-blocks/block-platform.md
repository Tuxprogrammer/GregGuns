# Block platform: GT wrench, Waila and ID space

- **Techguns baseline:** [special-blocks.md, chests](../../techguns-spec/07-blocks/special-blocks.md), [block-catalogue.md](../../techguns-spec/07-blocks/block-catalogue.md), bug [BLK-15](../../techguns-spec/BUGS.md)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md), [0003 energy model](../../design/0003-energy-model.md)
- **Pack mods:** GT5-Unofficial (`gregtech`, 5.09.54.205), IC2 (`IC2`, API only), Waila (`Waila`, 1.19.34), EndlessIDs (`endlessids`, 1.7.4)
- **Roadmap:** the blocks milestone in [ROADMAP](../../ROADMAP.md)

## What changes from Techguns

| Techguns behaviour | GregGuns behaviour | Why |
|---|---|---|
| A sneaking right-click with an item of tool class `wrench` triggers chest pickup in the block code ([BLK-15](../../techguns-spec/BUGS.md) puts the drop at the block corner) | The block declares harvest tool `wrench` and its tile entity implements IC2's `IWrenchable`. The GT wrench then picks it up through its own path | The pack's wrench is the GT wrench |
| No Waila support | Waila providers for non-MTE blocks. MTEs get GT's provider for free | The pack ships Waila |
| Blocks use metadata 0 to 15 | Same. Nothing relies on extended metadata | EndlessIDs is installed but the pack gives no promise about metadata |

## Integration points

### GT wrench: harvest tool and `IWrenchable`
- **Source:**
  - [ToolWrench.isMinableBlock](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tools/ToolWrench.java#L89-L95) accepts a block whose `getHarvestTool(meta)` is `"wrench"`.
  - [ToolWrench.onBreakBlock](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/common/tools/ToolWrench.java#L164-L173): for a tile that is an `IWrenchable`, `wrenchCanRemove` can cancel the break, and `getWrenchDrop` and `getWrenchDropRate` supply the drop.
  - [GTToolHarvestHelper.isAppropriateTool](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTToolHarvestHelper.java#L14-L21) is the harvest-tool test.
- **GregGuns does:** chests, charges and any other pick-up-with-contents block call `setHarvestLevel("wrench", 0)` and implement `ic2.api.tile.IWrenchable` on the tile entity (IC2 is a hard dependency). `getWrenchDrop` returns the item with the `TileEntityData` NBT. The stack is delivered through the wrench's drop path, so BLK-15 does not occur.
- **Pack-side change:** none.

### Waila: tooltips for our blocks
- **Source:**
  - [IWailaDataProvider](https://github.com/GTNewHorizons/waila/blob/1.19.34/src/main/java/mcp/mobius/waila/api/IWailaDataProvider.java#L31-L126) and [IWailaRegistrar](https://github.com/GTNewHorizons/waila/blob/1.19.34/src/main/java/mcp/mobius/waila/api/IWailaRegistrar.java#L43-L53) define the API.
  - [Waila.processIMC](https://github.com/GTNewHorizons/waila/blob/1.19.34/src/main/java/mcp/mobius/waila/Waila.java#L88-L100) receives the registration IMC.
  - GT registers the same way: [crossmod/waila/Waila.java#L13-L31](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/crossmod/waila/Waila.java#L13-L31) sends `FMLInterModComms.sendMessage("Waila", "register", "<class>.callbackRegister")`.
  - GT's provider handles every tile that implements [IGregtechWailaProvider](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/interfaces/tileentity/IGregtechWailaProvider.java#L16-L30) through `BaseMetaTileEntity` ([GregtechTEWailaDataProvider.java#L31-L40](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/crossmod/waila/GregtechTEWailaDataProvider.java#L31-L40)).
- **Pack config:** `reference/gtnh/2.9.0-RC-2/.minecraft/config/Waila.cfg`. Waila has per-mod toggles under `modules`. A new mod's options can be added with the `addconfig` IMC.
- **GregGuns does:**
  - Kept machines are MTEs. They implement `IGregtechWailaProvider` where they have something to show, and need no Waila code of their own.
  - Plain tile blocks (chests, charges, Camp Flag) register a provider through the `register` IMC from a class loaded only when `Loader.isModLoaded("Waila")`. A charge shows its armed state and remaining fuse. A chest shows its owner only if it has one.
  - Server-side data uses the `getNBTData` hook, never client tile state.
- **Pack-side change:** none.

### EndlessIDs: ID space
- **Pack config:** the pack has no `config/endlessids*` file, so the defaults apply. The file `NEID.cfg` is a leftover from the earlier NEID mod and has no effect on EndlessIDs.
- **Source:** [GeneralConfig.java#L50-L53](https://github.com/GTMEGA/EndlessIDs/blob/1.7.4/src/main/java/com/falsepattern/endlessids/config/GeneralConfig.java#L50-L53): with `extendBlockItem`, the limit is 16 777 216 block and item IDs. EndlessIDs is `GPL + LGPL` and is a [`upstream-tag`](../INTEGRATION.md#step-1-find-the-mod-and-its-pinned-source) source.
- **GregGuns does:** registers every block and item by name and never stores a numeric ID in NBT, config or network data. It does not use metadata above 15 for blocks.
- **Pack-side change:** none.

## Requirements

- **GGS-07-20** Chests and any block that is picked up with contents declare harvest tool `wrench`, and their tile entities implement `IWrenchable`.
- **GGS-07-21** A wrenched block's drop carries its contents and is delivered by the GT wrench path (no manual item spawn).
- **GGS-07-22** Plain tile blocks register a Waila provider through the `register` IMC only when Waila is loaded.
- **GGS-07-23** Waila data comes from `getNBTData` on the server.
- **GGS-07-24** Nothing persists a numeric block or item ID.
- **GGS-07-25** Block metadata stays in 0 to 15.

## Tests

| Req | Check |
|---|---|
| 20, 21 | `/gregguns test` scenario: wrench a filled chest, place it, and check contents |
| 22, 23 | Client check with Waila; `runServer` boot without it |
| 24, 25 | Unit test over registered blocks and NBT writers |

## Open questions

- A dedicated wrench for GregGuns blocks is not planned. Is the GT wrench harvest requirement acceptable for early tiers? A tier-gating note in [0001](../../design/0001-tier-map.md) would settle it.
- GT's Waila provider is not hooked for non-MTE blocks. No decision is needed unless we turn a kept machine into a plain tile block.
