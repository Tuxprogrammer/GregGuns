# Sounds in `sounds/items/` (15 files)

All the `sounds.json` events below use category `player`. Events with several files pick one at random each time they play. Most of these sounds are *triggered* by wearable gear ([05-armor](../05-armor/index.md)) or the player tick handler. Only the glider loop is triggered by an item in this area, but the whole folder is listed here.

| File (`sounds/…`) | Size | sounds.json event | When it plays |
|---|---|---|---|
| `sounds/items/glider_loop_stereo.ogg` | 182 745 B | `techguns:glider.loop` | Client side, looped and attached to the player, while `isGliding` is true: back slot holds a Glider (or a Jetpack acting as a glider, 05), the player is airborne and sneaking, and motionY < −0.1. It starts when gliding begins (volume 1, pitch 1, [`TGSoundCategory.PLAYER_EFFECT`](../01-core/client-effects-audio.md#tgsoundcategory)) and is stopped as soon as gliding ends. See [special-items.md](special-items.md). |
| `sounds/items/glider_loop.ogg` | 32 795 B | **none** | Unused. Not referenced by `sounds.json` or code. By its name and much smaller size, presumably a non-stereo take of the same glider wind loop. |
| `sounds/items/jetpack_start_a.ogg` | 16 218 B | `techguns:jetpack.start` (variant a) | Once, at the player position, when the jetpack thrust key state goes from released to pressed (client `setJumpkeyPressed(true)` while no loop is active). Volume 1, pitch 1. ([05](../05-armor/index.md)) |
| `sounds/items/jetpack_start_b.ogg` | 16 881 B | `techguns:jetpack.start` (b) | as above |
| `sounds/items/jetpack_start_c.ogg` | 17 077 B | `techguns:jetpack.start` (c) | as above |
| `sounds/items/jetpack_loop_a.ogg` | 14 213 B | `techguns:jetpack.loop` (a) | Looping sound attached to the player while the jetpack thrust is held (created on press, stopped on release). ([05](../05-armor/index.md)) |
| `sounds/items/jetpack_loop_b.ogg` | 13 997 B | `techguns:jetpack.loop` (b) | as above |
| `sounds/items/jetpack_loop_c.ogg` | 14 008 B | `techguns:jetpack.loop` (c) | as above |
| `sounds/items/jetpack_stop_a.ogg` | 17 358 B | `techguns:jetpack.stop` (a) | Once, when the thrust key is released while the loop is playing. ([05](../05-armor/index.md)) |
| `sounds/items/jetpack_stop_b.ogg` | 17 005 B | `techguns:jetpack.stop` (b) | as above |
| `sounds/items/jetpack_stop_c.ogg` | 17 501 B | `techguns:jetpack.stop` (c) | as above |
| `sounds/items/nightvision_on.ogg` | 22 902 B | `techguns:nightvision.on` | Server, `playSoundAtEntity` (volume 1, pitch 1), when the player presses the "toggle night vision" key, night vision becomes enabled, and the player's gear provides a NIGHTVISION bonus > 0. ([05](../05-armor/index.md)) |
| `sounds/items/nightvision_off.ogg` | 9 254 B | `techguns:nightvision.off` | Server, when the toggle disables night vision **and** the player currently has the Night Vision potion effect (which is then removed). ([05](../05-armor/index.md)) |
| `sounds/items/antigrav_start_a.ogg` | 50 947 B | `techguns:items.antiGravStart` (a) | When a player whose gear grants the CREATIVE_FLIGHT bonus (Anti Gravity Device, 05) starts flying (was not flying last tick). The server sends a [`PacketPlaySound`](../01-core/networking.md#9--packetplaysound-sc) to everyone within 50 blocks (volume 1, pitch 1, PLAYER_EFFECT category). The client shows the `AntiGravRing` FX at the same moment. |
| `sounds/items/antigrav_start_b.ogg` | 52 097 B | `techguns:items.antiGravStart` (b) | as above |

Other sounds that items in this area use, all vanilla:
- Medkit (unregistered): `random.eat` every 7 ticks while in use, `random.burp` on completion.
- Spawn eggs: the spawned mob's living sound.
- Dispensed rockets and grenades: the vanilla dispenser click (the custom launch sounds are commented out).

The Reaction Chamber foci name the sounds `techguns:machines.chemlabWork` (UV Emitter) and `techguns:machines.rc_heatrayWork` (Heat Ray). These are machine sounds documented in 08.
