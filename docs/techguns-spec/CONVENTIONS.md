# Spec conventions

The standard every area of this spec meets, so that a gun, a mob, a machine and a block are each documented to the same depth. Each section names the best existing example to model new writing on.

## Ground rules

- **Describe, don't copy.** Write behavior, formulas and state machines in your own words, tables or pseudocode. No pasted or closely paraphrased Techguns source.
- **Facts are welcome.** Names, IDs, metadata values, constants, recipe grids, NBT keys, packet fields, lang keys and their English text, and asset paths and sizes are all fair to record.
- **Only add.** Raising one area never means trimming another. Correct a statement only when it's wrong, and mention the correction in the area's coverage notes.
- **Say "none" explicitly.** If a template field doesn't apply (no sounds, no GUI, no NBT), write *none* rather than leaving it out. Then a reader can tell "doesn't exist" from "wasn't checked".
- **Unreachable content is still documented.** Mark it *unregistered*, *dead code* or *unobtainable*, so implementers can decide whether to build it.
- **Links:**
  - Cross-reference other areas by folder name (`04-projectiles-fx`) or with a relative link.
  - A link script turns backticked class names, registry names, config keys, asset paths and bug IDs into links, so write them in backticks.
  - Keep existing links intact when editing.

## Entity sheet

Every documented *thing* gets a sheet: a gun, grenade, armor piece, special-slot item, item meta with behavior, block family, machine, tile entity, projectile, mob, structure, packet or keybind. Use these fields, in this order, in a section or table.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | Classes; registry name; unlocalized name; metadata value; English name with its lang key; creative tab; oredict names |
| 2 | **Obtaining** | Exact crafting grid, or a link to it. Otherwise: machine recipe, loot table, mob drop, worldgen source, or *unobtainable* with the reason |
| 3 | **Numbers** | Every stat, constant and default, plus each config key that changes one (with its default) |
| 4 | **Behavior** | Ordered logic, with client and server labeled: tick logic; use, right-click, sneak and left-click interactions; automation (sided slots); redstone; edge cases |
| 5 | **Persistence and sync** | NBT keys with type and meaning; packets, DataWatcher indices and GUI progress-bar fields |
| 6 | **UI** | GUI layout with pixel coordinates for slots, bars, tanks and buttons; tooltip lines exactly as shown, and when each appears; chat messages |
| 7 | **Visuals** | Renderer and model, transforms, the textures used (paths), animations, particle and FX names |
| 8 | **Audio** | Sound events played, the exact trigger, volume and pitch |
| 9 | **Relationships** | What uses it and what it uses, linked across areas |
| 10 | **Bugs** | Inline bug IDs (`GUN-12`) for everything suspicious |

**Models to follow:**
- [08-machines-production/chem-lab.md](08-machines-production/chem-lab.md) and [09-machines-utility/repair-bench.md](09-machines-utility/repair-bench.md) for complex things;
- [10-entities-npcs/per-mob.md](10-entities-npcs/per-mob.md) for compact, many-of-a-kind sheets;
- [04-projectiles-fx/projectiles.md](04-projectiles-fx/projectiles.md) for per-class logic.

Catalog tables (gun, item, block, mob, armor) are kept as quick references. They are **in addition to** the sheets, not a replacement for them.

## Asset entries

Measured numbers come from [ASSET-FACTS.md](ASSET-FACTS.md). Every area quotes them the same way.

**Texture:**
- path, size, and animation (frame count and frametime from `.mcmeta`);
- what it depicts;
- palette: the measured average and dominant colors, plus a description in words;
- layout: UV or pixel rectangles for every region of a model, GUI or atlas texture;
- what uses it;
- notes such as unused, missing or wrong-case.

Model: [05-armor/textures.md](05-armor/textures.md).

**GUI texture:** everything above, plus each region's rectangle and role: background, slot, progress bar with fill direction, tank, button states, tabs. Model: the region maps in [08-machines-production/gui-textures.md](08-machines-production/gui-textures.md).

**Sound:**
- path and `sounds.json` event;
- category;
- duration, sample rate and channels;
- exact trigger (code condition);
- volume and pitch the code passes;
- notes.

Model: [08-machines-production/sounds.md](08-machines-production/sounds.md). The audio can't be listened to here, so any description of what a sound *sounds like* is inferred from its name and use, and must say so.

## Lang

Each area has a lang section or file listing:
- every lang key in its scope, with the exact English text;
- strings generated in code (tooltips, chat), with the condition that produces each one;
- missing keys, typos and hard-coded English.

Model: [05-armor/lang-and-strings.md](05-armor/lang-and-strings.md).

## Area index

Every `index.md` has these sections, in this order:

1. **Overview**: what the area covers, in a few sentences.
2. **Contents**: links to every file in the area, one line each.
3. **Key facts** (optional): things an implementer must know up front.
4. **Related areas**: the other areas it touches, and why.
5. **Coverage**: source files read out of those in scope, plus textures, sounds and lang keys covered out of those in scope.
6. **Known limits**: what couldn't be verified, and why.
7. **Suspected bugs**: the table `ID | Title | Location | What looks wrong | Severity`, with severity one of crash, gameplay, visual or cosmetic. Merged and deduplicated in [BUGS.md](BUGS.md).
