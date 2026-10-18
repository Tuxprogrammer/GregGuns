# Turret (base block + mounted turret entity)

`BasicMachine` meta 2. Item/tile name `techguns.tile.BasicMachine_2.name` = "Turret". Container title `techguns.container.turretbase` = "Turret Platform".

| Part | Class / value |
|---|---|
| Tile entity | `TurretBaseEnt`, registered as `TurretBaseEnt` |
| GUI id | 2 |
| Container | `TurretBaseContainer`, with slot classes `SlotTechgun` and `SlotTurretArmor` |
| GUI | `GuiTurretBase` |
| Extra button | `GuiButtonTurretTargetAnimals` |
| Base TESR | `RenderBlockTileEntModel` + `ModelTurretBase` |
| Mounted entity | `NPCTurret`, registered as `SimpleTurret`, lang `entity.Techguns.SimpleTurret.name` = "Turret". Its AI, rendering (`RenderSimpleTurret`, `ModelTurret`) and combat belong to **10-entities-npcs**; this file covers only the base and how the two are linked. |

## Concept

The block is a powered platform. Loaded with a Techguns gun, ammunition and optional armour plating, it maintains an immobile turret entity standing on top of it.

The base:

- supplies the gun, the ammo and the armour value;
- pays a power upkeep;
- heals the turret;
- respawns the turret after a delay when it is destroyed.

## Tile entity `TurretBaseEnt`

- Extends `BasicPoweredTileEnt` (08): **20 slots**, **1 000 RF** buffer.
- It has an owner, a security level (0 public, 1 friends, 2 owner) and a redstone mode.
- Energy connects on every side **except the top**.

| Slot(s) | Role | Valid |
|---|---|---|
| 0–8 | ammo input | anything |
| 9–17 | empty magazine output | anything |
| 18 | weapon | `GenericGun` only (GUI slot also rejects akimbo guns) |
| 19 | turret armour | anything by `isItemValidForSlot`; the GUI slot accepts only `ITGSpecialSlot` items whose slot type is `TURRETARMOR` |

**Automation:**

- From the bottom face: nothing.
- From any other face: slots 0–17 are exposed; insertion into 0–8 and extraction from 9–17.

### Linked entity

- `spawnTurret` (server) creates an `NPCTurret` bound to the base and positions it at block centre, y + 1 (on top of the base). An overload sets the turret's starting health.
- `BasicMachine.onBlockPlacedBy` spawns a turret at placement unless `turretDeath` is set. The flag can be set when the block is placed from a wrenched item.
- On every **server** tick, if the turret's held weapon (its base's slot 18) changed since last tick, the base syncs the block and tells the turret to rebuild its ranged-attack AI (`setCombatTask`).
- **Client re-linking:**
  - A client base with no turret, while not dead, sends `RequestTileEntitySync(x, short y, z)` to the server at most once per 1 000 ms of system time. The server answers with the tile's description packet.
  - A client turret with no base, within 64 blocks horizontally of the player, sends `PacketRequestTurretSync(entityId)` at most every 600 ticks. The server answers with its base's description packet.
  - The tile NBT carries the turret's entity id. On read, it looks up that entity and links both ways.
- **Server-side re-linking:** a turret with no base checks the block below its position every tick. If that block is a `TurretBaseEnt`, it links itself (see 10).
- **Breaking the base** (`BasicMachine.breakBlock` for meta 2): the mounted turret is killed (health 0, set dead) and the inventory spills.

### Per-tick logic (both sides; power consumption is authoritative on the server)

**Turret dead (`turretDeath`):** only while the redstone condition holds:

1. If 50 RF can be paid, `repairTime` − 1.
2. When `repairTime` ≤ 0: clear `turretDeath` and, on the server, spawn a new turret with **10 HP**, then sync.

Without power the countdown stalls.

**Turret present, redstone enabled:**

- **Upkeep:** 5 RF per tick.
  - If it cannot be paid and the turret is active: deactivate it (`disable()` strips all AI tasks, `active = false`).
  - If it can be paid and the turret is inactive: activate it (re-add AI tasks, rebuild the combat task).
- **Healing:** a heal cooldown counts down every tick. At 0, if the turret is below max health and 500 RF can be paid, heal **1 HP** and reset the cooldown to **60 ticks**.

**Turret present, redstone disabled:** deactivate it if it is active.

**Turret death:** the entity calls `onTurretDeath()` on its base (server). That clears the link, sets `turretDeath = true` and `repairTime = 200` ticks (10 s), and syncs.

### Ammunition (`consumeAmmo`, called by the turret for each shot)

1. If there is no gun in slot 18, the shot fails.
2. If the gun's loaded ammo (gun NBT `ammo`, see 02) is ≤ 0, **reload instantly**:
   1. Take one item of the gun's ammo type (matching item and damage) from slots 0–8. If there is none, the reload fails.
   2. If the gun has an empty-magazine item, add one to slots 9–17. Any overflow is dropped above the base. A stack-size-0 drop is spawned even when there is no overflow (MUTL-24).
   3. If the gun's `ammoCount` > 1, keep taking ammo items, up to `ammoCount` in total, and add as many rounds as items consumed. Otherwise refill to the full clip size.
   4. Sync.
3. If at least 1 round is loaded, use 1 round and allow the shot.

The turret calls `fireWeaponFromNPC(turret, 1.0, 1.0)` on the gun only when `consumeAmmo` succeeds (see 02 and 10).

### Armour

Slot 19 sets both the turret's armour value (vanilla armour points; each point reduces damage by 4%) and the textures:

| Slot 19 item (06) | Armour value | Base and turret texture |
|---|---|---|
| empty or other | 0 | `textures/blocks/TurretBase.png` |
| `turretArmorIron` "Iron Turret Armor" | 5 | `textures/blocks/TurretBase_2.png` |
| `turretArmorSteel` | 10 | `textures/blocks/TurretBase_3.png` |
| `turretArmorObsidianSteel` | 15 | `textures/blocks/TurretBase_4.png` |
| `turretArmorCarbon` | 20 | `textures/blocks/TurretBase_5.png` |

### Targeting rules

The selector is `TurretTargetSelector`; 10 owns the AI that uses it. A candidate is chosen as follows:

1. **Any hostile mob (`IMob`): target.**
2. If the base has an owner **and** security ≠ 0:
   - Another `NPCTurret` whose base has an owner: target it iff the two owners are **not friends**. The decision ends here.
   - A player: never the owner. Otherwise target iff not friends with the owner. The decision ends here.

   "Friends" comes from the FTBUtilities integration. **Without FTBUtilities, every pair counts as friends**, so turrets never shoot players or other turrets.
3. If **Target Animals** is on and the candidate is a passive creature (`IAnimals`, but not a turret):
   - tamed `EntityTameable`, tamed horses and any `IEntityOwnable` with an owner are excluded;
   - everything else is a target.
4. Otherwise: not a target. With security 0 (the default after placement) players and turrets are never targeted.

The turret also retaliates against whoever hurts it, using faction rules (10).

The turret entity's base stats are listed for reference; 10 owns them:

- max health 30, movement speed 0, knockback resistance 1, eye height 0.8;
- follow range 20, replaced by the gun's AI attack range when a gun is mounted;
- hurt sound `mob.irongolem.hit`, death sound `mob.irongolem.death`;
- no drops; never despawns.

### Button handling

Ids 1 (security) and 2 (redstone) come from `BasicPoweredTileEnt`. **Id 3** toggles `attackAnimals` when the player passes the usable check, and syncs.

### NBT

| Source | Keys |
|---|---|
| Base classes | `Items`, `rotation`, `CustomName`, `security`, `owner`, `Energy`, `hasSignal`, `redstoneBehaviour` |
| Turret | `mountedTurret` (int entity id, only when linked), `turretDeath`, `repairTime`, `turretHealTime`, `attackAnimals` |

A wrench-dismantled turret item carries all of this in `TileEntityData`.

## Container (`TurretBaseContainer`)

| Index | Slot | Position |
|---|---|---|
| 0–8 | ammo input; index 3·col + row (column-major) | (18 + 18·col, 17 + 18·row) |
| 9–17 | magazine output; index 9 + 3·col + row | (116 + 18·col, 17 + 18·row) |
| 18 | `SlotTechgun`: non-akimbo `GenericGun`, stack limit 1 | (85, 19) |
| 19 | `SlotTurretArmor`: `ITGSpecialSlot` with type `TURRETARMOR`, limit 1 | (85, 42) |
| 20–46 | player main | (8 + 18·col, 84 + 18·row) |
| 47–55 | hotbar | (8 + 18·i, 142) |

Shift-click rules:

| From | To |
|---|---|
| Machine slot | player slots 20–55, from the end |
| Player slot, `GenericGun` | slot 18 |
| Player slot, turret armour | slot 19 |
| Player slot, anything else | ammo slots 0–8, from the end |

Progress-bar sync:

| Id | Value | Sent |
|---|---|---|
| 0 | `repairTime` | when changed |
| 1 | `turretDeath` (0/1) | when changed |
| 2 | stored power | every tick, unconditionally |

## GUI (`GuiTurretBase`)

The GUI extends `GuiTechgunsPoweredTileEnt` (08): power bar at (8,17), redstone button at (−22,10), security button at (−22,40). The background is `textures/gui/turretBase_GUI.png`, catalogued by 08.

| Texture region (px) | Depicts | Drawn |
|---|---|---|
| (0,0) 176×166 | Grey panel. Power-bar slot at the left (≈(7,16)–(12,66)). 3×3 ammo grid whose wells show three-bullet silhouettes. Weapon slot with a pistol silhouette. Armour slot with a riveted-plate silhouette. 3×3 output grid whose wells show three short bar (empty magazine) silhouettes. Below the output grid, a long thin repair-bar frame (≈(115,73)–(168,78)) with a small grey wrench mark in the middle. Player inventory. | (0,0) |
| (177,1) 52×4 | Repair fill: horizontal gradient red → orange → yellow → green, with the grey wrench mark overlaid | while dead, at (116,74), width = 52 − (repairTime × 52 ÷ 200) + 1 |
| (176,6) 16×16 | Pink pig with a green check mark ("animals safe") | Target-animals button icon when `attackAnimals` is false |
| (176,22) 16×16 | Pink pig with a red crosshair over it ("animals targeted") | the same button when true |

After the parent's background, the GUI draws one more side-button backdrop: `ammoPress_GUI.png` (195,0) 27 × 30 at (−27, 65).

**Button:** `GuiButtonTurretTargetAnimals`, id 3, 20 × 20 at (−22, 70). It draws the vanilla button background, then the 16 × 16 state icon at offset (2,2).

**Text:**

- The title is centred at y = 6. "Inventory" is drawn at (8, 72).

| Hover rect | Tooltip |
|---|---|
| (115, 73, 53, 5), only while dead | `techguns.container.repair` + " " + seconds + "s", for example "Repair time left 7s" |
| (−22, 70, 20, 20) | `techguns.turret.targetAnimals` + ": " + either red `techguns.yes` "Yes" or green `techguns.no` "No". Displayed as "Target Animals: Yes/No". |

The power, redstone and security tooltips come from the parent (08).

## Base model and rendering

`RenderBlockTileEntModel` draws `ModelTurretBase` with the armour-dependent texture (see shared-rendering.md §3); the tile's rotation is ignored. The item form uses the same model with `TurretBase.png`.

`ModelTurretBase` uses 64 × 64 texture units, drawn on a 128 px texture (2 px per unit). Its boxes, in units (Y down, block occupies y 8..24, x/z −8..8):

| Part | Box w×h×d | Position | UV origin | Look |
|---|---|---|---|---|
| Base | 14×14×14 | centred, y 10..24 | (8,36) | main body cube: concrete/metal faces with a recessed square panel |
| Top | 8×2×8 | centred on top, y 8..10 | (30,0) | turret mounting plate |
| Corner posts ×4 | 2×15×2 | at the four corners, y 9..24 | (0,30) | dark vertical posts |
| Diagonal bars ×4 | 6×1×2 | from each corner post top toward the centre (45° yaw) | (36,18) | dark braces |
| Bottom rails ×4 | 1×2×12 | along the bottom edges, y 22..24 | (0,15) | dark skirting |
| Sockets ×4 | 1×6×6 | middle of each side face, y 13..19 | (0,0) | dark square ports (power/data sockets) |

The turret head model (10's `ModelTurret`) maps into the **same texture**:

- mount 4×1×4 at UV (34,12);
- plate 8×1×8 at (32,22);
- two arms 2×11×2 at (9,30).

The darker regions at the top right of the sheet are the head's parts.

### Textures `textures/blocks/TurretBase*.png`

All are 128 × 128 with no animation and share one layout. Magenta-free areas are used; unused areas are transparent.

| File | Variant | Palette / look |
|---|---|---|
| `textures/blocks/TurretBase.png` | no armour | Body: mottled mid/light-grey concrete with a darker recessed square on each face. Posts, rails and braces: near-black charcoal. Sockets: two dark squares with lighter inset squares. Head pieces: dark grey, with a radial dark gradient on the top plate. |
| `textures/blocks/TurretBase_2.png` | iron | Body: cleaner, lighter brushed grey with a bevelled square panel. Same dark trim. |
| `textures/blocks/TurretBase_3.png` | steel | Body: grey with white speckle noise (galvanised steel); trim slightly darker |
| `textures/blocks/TurretBase_4.png` | obsidian steel | Body: very dark purple-black with lilac speckles (obsidian) |
| `textures/blocks/TurretBase_5.png` | carbon | Body: near-black, faint woven carbon texture; trim black |

## Sounds

The base itself plays none. `sounds.json` defines two turret events that nothing in the code plays (MUTL-28):

| Event | Category | Files |
|---|---|---|
| `techguns:npcs.turretBeep` | hostile | `sounds/npcs/turret_beep-01.ogg` … `turret_beep-07.ogg` (7 variants) |
| `techguns:npcs.turretGunequip` | hostile | `sounds/npcs/turret_gunequip-01.ogg` … `-03.ogg` (3 variants) |

Their intended use is presumably an idle or target-acquired beep, and a sound when a gun is inserted. 10 may also list them.

## Unused classes

`blocks/TurretBase` is an old stand-alone `BlockContainer`:

- material rock, hardness 4, pickaxe level 2, display name "Turret Base";
- on being added to the world it spawned a `TurretEntityUNUSED`;
- on break it called `breakTurret()`.

Its registration in `TGBlocks` is commented out. `TurretBaseEnt.breakTurret()` is only reachable from that dead class; the live path is `BasicMachine.breakBlock` → `NPCTurret.destroy()`.
