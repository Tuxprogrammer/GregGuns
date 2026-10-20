# Unreleased

Notes for the next alpha. Fold these into that release's changelog when it is cut.

## Removed: the Reaction Chamber and the Retort

Design record [0012](../docs/design/0012-retire-reaction-chamber-and-retort.md) retires both machines. A bespoke machine for two recipes was not worth keeping in a pack this rich, so the two recipes moved onto GregTech machines you already own:

- **Laser Focus** (EV) is now made in a **GT Laser Engraver**: a diamond and an EV Emitter, with a white lens that is not used up. The Emitter is now consumed per Laser Focus.
- **Anti Gravity Core** (IV) is now made in a **GT Assembler**: a Nether Star, an IV Emitter, two IV circuits and 1,152 mB molten Enderium.
- **Heat Ray Focus** is gone. Uranium enrichment, which was only planned, is dropped.
- The risk gameplay (intensity buttons, breakage, explosions, radiation, Retort pollution) is gone, and so are the `machines.*` config keys.

The Retort is not lost. It is archived whole on branch `archive/retort-multiblock` and tag `archive/retort-v1`, in case it is ever offered to the GTNH community as a standalone multiblock.

### Existing worlds

- The MTE ids **20106** (Reaction Chamber EV), **20107** (Reaction Chamber IV) and **20108** (Retort) are retired and reserved forever. A placed Reaction Chamber or Retort turns into an inert "retired" block that you can break; **whatever was inside it is lost**, and a Laser Focus or Core that was mid-run is lost with it. Retort casings stay as decorative Reaction Casing blocks for one alpha.
- Machine items of the three retired ids in an inventory still render and show a "retired" tooltip instead of crashing; they do nothing.
- **Heat Ray Focus** stacks vanish with no refund.
- Back up your world as always; the alpha makes no stability promise.
