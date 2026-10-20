# Workstations (ICBM 2)

Source: BuiltBrokenModding/ICBM @ `13e6a524`, paths relative to `src/main/java/com/builtbroken/icbm/content/crafting/station/`. Three machines: the **small missile workstation** (hand assembly), the **missile auto crafter** ("SMAuto"), and the **warhead station**. None uses energy.

## 1. Small missile workstation (`small/TileSmallMissileWorkstation.java`)

A block-in-world assembly stand. There is no crafting grid and no real GUI: the player clicks the missile itself.

| Fact | Value |
|---|---|
| Inventory | 1 slot (`INPUT_SLOT` 0) holding the missile item |
| Accepts | only a **SMALL** missile (`isItemValidForSlot` compares the missile size to SMALL) |
| Material | iron, hardness 10, resistance 10, non-opaque, tile-entity render |
| Recipe | tool recipe: rods and plates of iron plus hammer and other tools (`RecipeTool`) |

### Footprint (`small/TileSmallMissileStationBase.java`)

The block is a 3-block multiblock: the host plus **one inventory-proxy tile on each side of the missile axis** (`#RenderBlock=false` tiles). The two extra cells depend on rotation:

| Placed on | Facing | Extra cells (relative) |
|---|---|---|
| Floor or ceiling | N or S | (0,0,+1), (0,0,-1) |
| Floor or ceiling | E or W | (+1,0,0), (-1,0,0) |
| Wall | up or down | (0,+1,0), (0,-1,0) |
| Wall | other | along the side axis |

So the station is a 1 x 1 x 3 bar. The block remembers the face it was placed against (`connectedBlockSide`, from block metadata). Facing may never equal the attached face or its opposite. A wrench cycles facing (sneak flips 180 degrees) and refuses if blocks are in the way. The tile checks its multiblock cells are free before building and drops itself as an item if no valid rotation is left. Breaking any proxy cell collapses the whole station.

### GUI flow (it is hands-on, not a screen)

1. Right-click the **main block** with a small missile: it goes into the slot. A sneaking, empty-handed click takes it back out.
2. With a missile loaded, right-click the **main block** holding a **guidance** module: installed if the guidance slot is empty (consumes the item), else "Guidance already set". Empty-hand right-click removes the guidance and puts it in your hand.
3. Right-click the **warhead end** (front, the proxy cell on the warhead side) with a warhead: installs it ("Warhead already installed" otherwise). Empty hand removes it. Non-warheads get "Only warheads can fit on the tip."
4. Right-click the **engine end** (back proxy cell) with an engine: same rules, "Only engines can fit in the end."
5. Wrong-end hints: an engine clicked on the main block says "Try clicking the back of the missile"; a warhead says "Try clicking the front of the missile".
6. Every change rewrites the missile item in the slot (`updateMissileItem`) and sends a description packet; the client draws the missile and the installed parts by hit position (the client class is large because it handles the animated click zones).

Key point: the tile is only a **module installer**. It never validates that the warhead size matches the missile, never needs the casing built, and no resources are consumed other than the modules themselves. Assembly is instant.

### Micro missiles

The micro size has no workstation. Two shapeless crafting recipes build it in a grid: `RecipeMicroMissileWarhead` (micro casing + micro warhead) and `RecipeMicroMissileEngine` (casing + engine; two missiles' engines can be combined by recipe). Also the warhead block's recipe list adds warhead-in-missile recipes.

## 2. Missile auto crafter (`small/auto/TileSMAutoCraft.java`)

An automation-friendly version, a "worktable" block with a proper GUI (`GuiSMAutoCraft`, `ContainerSMAutoCraft`). It is multi-tile (model "3x1Cart", footprint 3 x 1; rotation by block metadata, key `icbm:icbmSMAuto`). A small-station "assembly line" helper (`MissileAssemblyLine`) is an unfinished list holder for chaining stations (`add`, `merge`, `kill` only).

| Slot | Index | Accepts |
|---|---|---|
| Input (casing or partial missile) | 0 | any missile module |
| Warhead | 1 | warhead modules |
| Guidance | 2 | guidance modules |
| Engine | 3 | engine modules |
| Output | 4 | removal only |

Config flags saved in NBT: `isAutocrafting`, `requiresWarhead`, `requiresGuidance`, `requiresEngine` (default true), and `checkForCraft`.

Flow:

1. `getCraftedMissile` takes the input missile, then for each module slot that holds a module and whose missile slot is empty, installs it. In manual mode every present module is used; in auto mode only the ones that are "required" are used.
2. **Manual mode:** the GUI shows the preview result; the Craft button (packet id 1) calls `doCrafting`, which consumes one of each used module slot, writes the finished missile to the output slot and removes one input.
3. **Auto mode** (`isAutocrafting`): every 10 ticks, if `checkForCraft` (set by any inventory change) and `canCraft()`, it crafts without a button press. `canCraft` in auto mode needs every *required* module present in the result, so an assembler never emits a half-built missile unless the flags are cleared.
4. Automation (pipes or hoppers): `insertRequiredItem(stack, slot, ...)` puts one item into the right slot when the slot is empty and the module type matches. Only the output is removable. The slot-less overload is a stub ("implement when less lazy").
5. Crafting progress and required-item queries are stubs (`getCraftingProgress` returns 0, `getRequiredItems` returns empty).
6. The client renders both the started missile and the finished one from the description packet.

It also accepts the casing of any size, not only SMALL. The size check against the warhead is again absent.

## 3. Warhead station (`warhead/TileWarheadStation.java`)

Fills a warhead item with explosive items and an optional trigger. It has two GUI tabs (`GuiWarheadStation`, `ContainerWarheadStation`; the tab is a packet id 2 that reopens the GUI with another id) and the same manual/auto split.

| Slot | Index | Accepts |
|---|---|---|
| Warhead | 0 | any `IWarhead` module item |
| Explosive | 1 | anything the explosive registry knows |
| Output | 2 | removal only |
| Trigger | 3 | `ITrigger` module items |

Settings (packet id 3, saved in NBT): `isAutocrafting`, `requireExplosive` (default true), `requireTrigger` (default false), `explosiveStackSizeRequired` (default 1, server clamps to 1..64).

Flow of `doCrafting`:

1. Needs a warhead item, a registered explosive, and a free or matching output slot.
2. The warhead module is rebuilt from its item. If it has space for that explosive (`hasSpaceForExplosives`: same explosive, capacity left), it moves `min(required, stack, space)` explosive items in. Existing contents are topped up; an empty warhead takes the stack as its explosive.
3. If a trigger is in the trigger slot and the warhead has none, it installs the trigger and returns what `setTrigger` hands back (the old trigger, normally none).
4. The finished warhead item goes to output (or stacks onto it), one warhead and `insert` explosive items are consumed.
5. Manual mode: the Craft button (packet id 1). Auto mode: every 10 ticks if `checkForCraft` and `canCraft` (honors `requireExplosive`, `requireTrigger` and the required count).
6. The explosive amount per craft is the "required size". A large warhead (128 cap) needs several crafts, since the clamp is 64.

This is the **warhead-fill step**: casing capacity (1, 5, 20, 64 or 128 items) times the explosive's per-item size decides blast strength. A full pipeline is therefore casing -> warhead station -> missile workstation.

Automation: only the warhead, explosive and trigger slots accept input, only the output is removable. Inserting an explosive has a guard bug (ICBM-A1-08).

## 4. Where the stations connect to the module system

```
 casing (missile item) --+                       warhead item <-- explosive item(s) + trigger   (warhead station)
                         |                                  |
 guidance item ----------+--> missile workstation / auto crafter <--+
 engine item ------------+          (writes module stacks into missile item NBT)
```

See [modular-missiles.md](modular-missiles.md) for what each module changes.

## 5. Unfinished or odd

- The small workstation takes SMALL only. There are no stations for standard, medium or large.
- Progress bars, required-item lists and the slotless insert are stubs on both crafters.
- `ejectCraftingItems` loops one slot too far (ICBM-A1-12).
- The auto crafter reads its flags back wrongly from NBT (ICBM-A1-09), checks the wrong slot for output room (ICBM-A1-10) and overwrites the output (ICBM-A1-11).
- The trigger slot is commented out of the first GUI tab; it lives alone on the second tab.
