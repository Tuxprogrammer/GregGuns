# 07-blocks: sounds (`sounds/blocks/`)

All three files are variants of one sound event.

| Path | Format | Length |
|---|---|---|
| `sounds/blocks/metaldoor_open_a.ogg` | Vorbis, mono, 32 kHz | 0.96 s |
| `sounds/blocks/metaldoor_open_b.ogg` | Vorbis, mono, 32 kHz | 0.96 s |
| `sounds/blocks/metaldoor_open_c.ogg` | Vorbis, mono, 32 kHz | 0.83 s |

**sounds.json entry.** Event `blocks.metaldoorOpen` (full name `techguns:blocks.metaldoorOpen`), category `block`. Its sound list is `blocks/metaldoor_open_a`, `blocks/metaldoor_open_b` and `blocks/metaldoor_open_c`, picked at random with equal weight.

**When it plays.** It is a heavy metal door/hatch opening clank, used for **both opening and closing**:

- Every time a player right-clicks a Techguns metal door (`door01`, `BlockTGDoor`).
- Once more for each neighbouring `door01` leaf toggled as part of a double door.

Volume 1.0, pitch 1.0. It is issued with `World.playSound(x, y, z, …, distanceDelay = false)` at the integer block corner. That method does nothing on the server, so only the clicking player's client hears it (BLK-02).

**Not used for redstone.** Opening the door with redstone goes through the inherited vanilla door code, which plays the vanilla door sound instead.

**Other sounds connected to blocks in this area** live under other folders and are owned elsewhere:

- `techguns:effects.biodeath`: Bioblob burst, 04-projectiles-fx.
- The explosive-charge "plant" sound: played from `ItemBlockMetaDataMachine`, owned by 08/09.
- Vanilla `random.chestopen` and `random.chestclosed`: TG chests.
