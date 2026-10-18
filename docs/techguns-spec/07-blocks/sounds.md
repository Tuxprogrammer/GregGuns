# 07-blocks: sounds (`sounds/blocks/`)

All three files are variants of one sound event.

| Path | Format | Length |
|---|---|---|
| `sounds/blocks/metaldoor_open_a.ogg` | Vorbis, mono, 32 kHz | 0.96 s |
| `sounds/blocks/metaldoor_open_b.ogg` | Vorbis, mono, 32 kHz | 0.96 s |
| `sounds/blocks/metaldoor_open_c.ogg` | Vorbis, mono, 32 kHz | 0.83 s |

**Measured facts** (from [ASSET-FACTS.md](../ASSET-FACTS.md)):

| Path | Event | Category | Duration | Sample rate | Channels |
|---|---|---|---|---|---|
| `sounds/blocks/metaldoor_open_a.ogg` | `blocks.metaldoorOpen` | block | 0.96 s | 32000 Hz | mono |
| `sounds/blocks/metaldoor_open_b.ogg` | `blocks.metaldoorOpen` | block | 0.96 s | 32000 Hz | mono |
| `sounds/blocks/metaldoor_open_c.ogg` | `blocks.metaldoorOpen` | block | 0.83 s | 32000 Hz | mono |

**Event table.**

| Event | Trigger (code condition) | Volume | Pitch | Where played |
|---|---|---|---|---|
| `techguns:blocks.metaldoorOpen` | `BlockTGDoor` right-click handler, once per `door01` leaf toggled (the clicked leaf plus a neighbouring double-door leaf) | 1.0 | 1.0 | `World.playSound` at integer block coordinates with no distance delay; client only ([BLK-02](index.md#suspected-bugs)) |
| `random.chestopen` (vanilla) | [`TGChestTileEnt`](../09-machines-utility/misc-tile-entities.md#tile-entity-tgchesttileent) tick, when at least one player is using the chest while the lid angle is 0 and the chest has no neighbour on its −X or −Z side (so a double chest plays it once) | 0.5 | 0.9 + 0.1 × random float, so 0.9–1.0 | `playSoundEffect` at the chest centre (shifted half a block toward a +X or +Z neighbour), also in the server tick |
| `random.chestclosed` (vanilla) | `TGChestTileEnt` tick, when the lid angle falls from 0.5 or above to below 0.5 while closing (no −X or −Z neighbour) | 0.5 | 0.9 + 0.1 × random float | as above |
| `techguns:effects.biodeath` | Bio Blob burst; owned by [04-projectiles-fx](../04-projectiles-fx/index.md) | see [04](../04-projectiles-fx/index.md) | see [04](../04-projectiles-fx/index.md) | see [04](../04-projectiles-fx/index.md) |

**Step, break and place sounds.** Techguns blocks only pick a vanilla sound type. The mapping per block (stone, piston, metal, glass, cloth, sand, wood) and the vanilla step, break and place event names are in [block-sheets.md](block-sheets.md) (conventions and each sheet's Audio row). There are no custom step or break sound files. The three files above are the only audio under `sounds/blocks/`.

**What it sounds like.** Inferred from the file name and its use only (it has not been listened to): a short metallic door or hatch clank, three variants chosen at random with equal weight. The variants are 0.83–0.96 s long, so one plays to completion before a quick close-open cycle.

**sounds.json entry.** Event `blocks.metaldoorOpen` (full name `techguns:blocks.metaldoorOpen`), category `block`. Its sound list is `blocks/metaldoor_open_a`, `blocks/metaldoor_open_b` and `blocks/metaldoor_open_c`, picked at random with equal weight.

**When it plays.** It is a heavy metal door/hatch opening clank, used for **both opening and closing**:

- Every time a player right-clicks a Techguns metal door (`door01`, [`BlockTGDoor`](special-blocks.md#8-metal-door-door01-blocktgdoor)).
- Once more for each neighbouring `door01` leaf toggled as part of a double door.

Volume 1.0, pitch 1.0. It is issued with `World.playSound(x, y, z, …, distanceDelay = false)` at the integer block corner. That method does nothing on the server, so only the clicking player's client hears it ([BLK-02](index.md#suspected-bugs)).

**Not used for redstone.** Opening the door with redstone goes through the inherited vanilla door code, which plays the vanilla door sound instead.

**Other sounds connected to blocks in this area** live under other folders and are owned elsewhere:

- `techguns:effects.biodeath`: Bioblob burst, [04-projectiles-fx](../04-projectiles-fx/index.md).
- The explosive-charge "plant" sound: played from [`ItemBlockMetaDataMachine`](special-blocks.md#16-itemblockmetadatamachine), owned by [08](../08-machines-production/index.md)/09.
- Vanilla `random.chestopen` and `random.chestclosed`: TG chests.
