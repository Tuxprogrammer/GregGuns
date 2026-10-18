# Scrapper (unfinished, unreachable)

| Part | Class / value |
|---|---|
| Tile entity | `ScrapperTileEnt`, registered as `ScrapperTileEnt` |
| Container | `ScrapperContainer` |
| GUI | `GuiScrapper` |
| GUI id | 10 (wired in `TechgunsGuiHandler`) |

## Status in 0.1.2

**The scrapper has no scrap table and cannot be obtained.**

- The case that would create `ScrapperTileEnt` for `BasicMachine` meta 10 is commented out, so the block returns no tile entity.
- `BasicMachine` only lists metas 0–9 in the creative tab, and no recipe produces meta 10.
- The item renderer has a placeholder slot 10 that draws the repair-bench cube.
- The machine's only "processing" code inspects the input item and prints debug text. It never produces output.

**There is no "what scraps into what" table.** The code only hints at the intended design:

- **Armour (`ItemArmor`, repairable):** look up the armour material's repair item and the item's damage fraction (damage ÷ maxDamage). This was presumably meant to return a share of the repair material scaled by remaining durability.
- **Tools (`ItemTool`, repairable):** look up the tool material by name and use its repair item stack.
- **Anything else:** print the item.

Only items for which `isRepairable()` is true are considered. Nothing is consumed or produced, and `canScrap` always returns false.

## Tile entity (as written)

- Extends `BasicMachineTileEntity` (08): **10** slots, **10 000 RF** buffer, standard redstone/security/owner fields.
- Slot 0 is the input; slots 1–9 are outputs.
- Inventory name: the custom name if one is set, else `techguns.container.scrapper`. **This key is missing from `en_US.lang`.**
- **Automation:** all sides expose slots 0–9. Insertion is allowed only into slot 0 and extraction only from slots 1–9. `isItemValidForSlot` always returns false, so automation can never actually insert.
- **Tick:** after the base update, if redstone allows it, no operation is running and slot 0 holds something, it calls `canScrap(slot 0)`. That method only prints debug text to stdout, every tick.
- **NBT:** inherited from `BasicMachineTileEntity`:
  - `Items`, `rotation`, `CustomName`
  - `security`, `owner`
  - `Energy`, `hasSignal`, `redstoneBehaviour`
  - `Progress`, `TotalTime`, `StackMultiplier`, `CurrentOperation`

## Container (`ScrapperContainer`)

| Index | Slot | Position |
|---|---|---|
| 0 | input, plain `Slot` | (35, 17) |
| 1–9 | outputs, `SlotOutput` (rejects placement); index 1 + 3·col + row | (116 + 18·col, 17 + 18·row) |
| 10–36 | player main | (8 + 18·col, 84 + 18·row) |
| 37–45 | hotbar | (8 + 18·i, 142) |

Progress-bar sync ids:

| Id | Value |
|---|---|
| 0 | progress |
| 1 | totalTime |
| 2 | stored power, sent every tick while working or whenever it changes |

Shift-click is not implemented; it falls through to vanilla `Container.transferStackInSlot`. See MUTL-04.

## GUI (`GuiScrapper`)

The GUI extends `GuiTechgunsPoweredTileEnt` (08), so it has the power bar, redstone button and security button. Its background is the ammo press texture `textures/gui/ammoPress_GUI.png`; 08 catalogues the file.

| Element | Source (u,v,w,h) | Destination |
|---|---|---|
| Panel | (0,0,176,166) | (0,0) |
| Extra patch | (119,59,20,20) | (151,59) |
| Progress arrow, while working: height = progress scaled to 21, plus 1 | (176,0,19,h) | (119,36) |

Text: the title is centred at y = 6, and "Inventory" is drawn at (8, ySize − 94).

## Textures and sounds

None of its own.
