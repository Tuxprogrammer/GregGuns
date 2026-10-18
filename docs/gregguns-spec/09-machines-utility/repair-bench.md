# Repair Bench: GT materials as repair inputs

- **Techguns baseline:** [repair-bench.md](../../techguns-spec/09-machines-utility/repair-bench.md), [repair action](../../techguns-spec/09-machines-utility/repair-bench.md#repair-action-buttonclicked-server)
- **Design records:** [0002 machine deconfliction](../../design/0002-machine-deconfliction.md) (kept: "armor and gun repair using GT materials"), [0001 tier map](../../design/0001-tier-map.md), [0003 energy model](../../design/0003-energy-model.md)
- **Pack mods:** GT5-Unofficial (`gregtech`, 5.09.54.205)
- **Roadmap:** the utility-machines milestone in [ROADMAP](../../ROADMAP.md). Machine registration is owned by [08](../08-machines-production/).

## What changes from Techguns

| Techguns behavior | GregGuns behavior | Why |
|---|---|---|
| Armor asks for a metal stack and a cloth stack, each a Techguns item | The repair list is made of GT stacks looked up by ore prefix and material, for example plate of the item's gate material | GT first; unification means any mod's plate counts |
| Matching is by item and damage value | Matching uses `GTOreDictUnificator.isInputStackEqual`, so unified equivalents match | Players should not need one specific mod's plate |
| Armor only, and only worn armor | Worn armor **and** guns, as [0002](../../design/0002-machine-deconfliction.md) says | Guns have durability too |
| Unpowered | Registered as a GT MetaTileEntity ([08](../08-machines-production/)). Whether it draws EU is an open question | [0002](../../design/0002-machine-deconfliction.md) rebuilds kept machines as MTEs |
| Shift-click off-by-one ([MUTL-05](../../techguns-spec/BUGS.md)) | Fixed in the container | Cited bug |

GT's own tools are not repaired here. They keep their damage in the `GT.ToolStats` NBT and GT owns that ([MetaGeneratedTool.java#L258-L275](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/MetaGeneratedTool.java#L258-L275)).

## Integration points

### GT5-Unofficial: material lookup and matching
- **Source:**
  - [GTOreDictUnificator.get(OrePrefixes, Object, long)](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L134) returns the unified stack for a prefix and material.
  - [isInputStackEqual](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/util/GTOreDictUnificator.java#L284) compares an input stack with a unified one.
  - [Materials.mDurability](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/enums/Materials.java#L1167) is the material's durability stat. GT uses it for tool `MaxDamage` ([MetaGeneratedTool.java#L268-L271](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.205/src/main/java/gregtech/api/items/MetaGeneratedTool.java#L268-L271)).
- **GregGuns does:**
  - each repairable item declares a gate material from [0001](../../design/0001-tier-map.md) (a `Materials` value) and a non-metal part as an ore name or a GT item;
  - the repair cost keeps the Techguns shape: damage fraction × a base count, split between metal and non-metal. Only the item types change;
  - the bench consumes stacks that `isInputStackEqual` accepts, in the stated count, or nothing at all (no partial repair, as in Techguns).
- **Pack-side change:** none.

## Requirements

- **GGS-09-20** A repair list entry for metal is a GT prefix and material (for example `plate`), never a Techguns-only item.
- **GGS-09-21** A stack matches an entry when `GTOreDictUnificator.isInputStackEqual` says so.
- **GGS-09-22** Repair is all or nothing. It consumes exactly the listed count and sets damage to 0.
- **GGS-09-23** The repair button range check (same tile, within 8 blocks) is kept.
- **GGS-09-24** Shift-click from the player's main inventory covers every slot (fixes MUTL-05).
- **GGS-09-25** The bench never touches a GT tool's `GT.ToolStats` NBT.

## Tests

| Req | Check |
|---|---|
| 20, 21 | Unit test with a unified plate from a second mod (ore dictionary entry only) |
| 22, 23 | Unit test on the container and button handler |
| 24 | Unit test on `transferStackInSlot` |
| 25 | Unit test that a GT tool in the slot is rejected |

## Built notes (M4 wave A)

Built as `MTERepairBench` (`com.gregguns.machine.repair`), IDs 20109 to 20113, one class for LV to IV. The math is plain Java (`RepairMath`, `RepairPlanner`, `RepairMaterials`) with unit tests. Decisions, each marked `// Deviation:` in code:

- **EU, settled: yes.** The bench is a powered GT single block. A repair costs `2 s + 8 s x damage fraction` at half the recipe voltage of the bench tier (the roster's proposal), but the bench has no recipe to run, so the whole job's energy is taken from its buffer at the button press. With less stored than the job costs, nothing happens and the player is told. The buffer holds two worst-case jobs.
- **Targets.** The four worn pieces (helmet first) and the held item. Guns are not repairable yet: `ItemGun` damage is "rounds missing", not wear, and the gun is `setNoRepair`. A gun with real wear implements `BenchRepairable` and nothing else changes. GT tools never implement it, so GGS-09-25 holds by construction.
- **Tier gate.** The bench repairs items of its own tier and below ([GGS-08-50](../08-machines-production/machine-roster.md#requirements)); the check is `RepairMath.tierAllows`. Only STEAM and LV armor exists today, so it is covered by unit tests, not a scenario.
- **Slots.** Nine slots, insert from any side, no extraction by automation (the Techguns rule). GGS-09-24 (shift-click) needs no GregGuns code: MUI2 and GT own the player-inventory slots, so the Techguns off-by-one (MUTL-05) cannot occur.
- **GUI.** GT's single-block frame (MUI2) with the nine slots and five text buttons ("Head", "Chest", "Legs", "Feet", "Held"). A press is an `InteractionSyncHandler` that runs on the server; no GregGuns packet. Not built: the Techguns art, the wrench icons and the per-button tooltip listing missing parts.
- **Access.** The server repeats the 8-block reach check (GGS-09-23) and GT's `isUseableByPlayer` (owner, private use) before it does anything.

## Open questions

- The per-button "missing parts" tooltip of the Techguns GUI is not built.
- Do guns share the armor formula, or do they need their own? [02-guns](../../techguns-spec/02-guns/index.md) owns durability. A design record on repair costs would settle it.
