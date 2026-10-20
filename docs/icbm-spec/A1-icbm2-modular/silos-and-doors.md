# Silos, silo door and missile magazines (ICBM 2)

Source: BuiltBrokenModding/ICBM @ `13e6a524`. Java under `src/main/java/com/builtbroken/icbm/content/launcher/` (and `storage/`); data under `src/main/resources/content/icbm/`. The "multiblock" here is ICBM's own tile-proxy system (a host tile plus invisible proxy tiles), not a pattern checked from player-built blocks. The host builds its proxy cells itself when placed.

## 1. Shared launcher base (`TileAbstractLauncher`, `TileMissileContainer`)

Every launcher (silos, the pad) extends one base. A silo holds exactly **one missile** (a 1-slot inventory).

| Fact | Value |
|---|---|
| Insert missile | right-click with a missile item (one is moved in; creative keeps the stack). Empty-hand right-click takes it out |
| Accept filter | `canAcceptMissile`: a missile object, and the subclass restricts size |
| Hopper / rail access | slot 0 loadable and unloadable from any side |
| Fire inputs | redstone (checked every 20 ticks: any power sets `fireMissile`), radio message from a command network, direct controller, GUI |
| Firing delay (`missileFireDelay`) | 40 ticks between shots |
| Door close delay (`siloDoorCloseDelay`) | 200 ticks after firing (saved in NBT) |
| Target | a `Pos`, default the silo itself; NBT `target` |
| Link code | random short, NBT `link_code`; needed by link tool, controllers and radio |
| Launch reports | last 20 launches (entity UUID and missile), NBT `launchReports` |
| Other NBT | `fofStationPos`, `siloDoorPos`, `siloDoorTimer`, `siloDoorDelay`, `customName` |

### Fire sequence

1. A fire request sets `fireMissile` (if the 40-tick cooldown allows). Each tick the flag stays true only while the silo is off cooldown and holds a missile.
2. If a silo door is linked, it is told `forceOpen` while `fireMissile` is true or the close timer runs.
3. `fireMissile(target)` runs when a missile is present and the door is open (or there is no door). The door's slow opening is what delays the shot (see section 4).
4. If the missile `canLaunch` (see [modular-missiles.md](modular-missiles.md)): the cooldown and close timers are set, the entity is built and spawned at `silo position + launch offset` with upward motion equal to engine speed, a launch report is recorded and the slot is emptied. The client plays smoke effects.
5. If there is **no engine**, 10% of attempts detonate the warhead inside the silo (`TriggerCauseFire`) and clear the slot.
6. If an engine exists but has zero range or speed (empty tank), nothing happens, and the flag and the open door stay stuck (ICBM-A1-15).
7. Missile entities report back to the launcher on impact or death through `onImpactOfMissile`, `onDeathOfMissile` (used for the report list) unless `noReport` is set by the tracker.

### Linking

`link(location, code)` accepts, within `TileLocalController.MAX_LINK_DISTANCE` (100 blocks) and the same dimension: a local controller (which links back), a friend-or-foe provider, or a silo door. Errors are lang keys such as `link.error.world.match`, `link.error.pos.distance.max`, `link.error.code.match`, `link.error.tile.invalid`, `link.error.tile.already.added`. Links are stored as positions and re-resolved lazily.

## 2. Small silo (`silo/small/TileSmallSilo.java`, `resources/content/icbm/silo/small.json`)

| Fact | Value |
|---|---|
| Accepts | SMALL missiles only |
| Footprint | host + proxy tiles at y+1 and y+2: a **1 x 3 x 1** column |
| Render bounds | 1 x 3 x 1 |
| Placement | must be placed on the **top** face of a block (`canPlaceOnSide`) |
| Rotation | host rotation block, direction = opposite of host metadata |
| Missile render offset | (0.5, 0.4, 0.5): the missile is drawn standing inside |
| Launch offset | default of the base (the small silo does not override it) |
| Recipe | `I I / I I / CBC`: iron bars (4), iron block, circuit (basic, or any `electronicCircuit`: two JSON recipes) |
| Material | iron |

Removing the block as a player drops the stored missile.

## 3. Standard silo (`silo/standard/TileStandardSilo.java`, `silo/standard.json`)

| Fact | Value |
|---|---|
| Accepts | STANDARD missiles only |
| Footprint | host + proxy tiles at y+1 to y+6: a **1 x 7 x 1** column |
| Render bounds | 1 x 7 x 1 |
| Launch offset | (0, 7, 0): the missile spawns at the top of the shaft |
| Placement | top face only |
| Missile render offset | (0.5, 0.7, 0.5) |
| Recipe | `ISI / ISI / CBC`: iron rods (4), **a small silo (2)**, iron plate, advanced circuit |

The standard silo is a taller copy of the small silo with a bigger recipe that consumes two small silos. There is no medium or large silo. (Legacy `blockSmallSilo` and `blockStandardSilo` registrations are commented out in `ICBM.java`; the JSON registers them now.)

## 4. Silo door (`door/TileSiloDoor.java`, `door/*.json`)

A hatch placed at ground level above the silo. Data-driven: `door/starseeker3x3/door.json` sets id `starseeker3x3`, `blockSize` 3, `blockHeight` 2, render and layout ids `icbm:silodoor.starseeker3x3`. `DoorData` validates that `blockSize` is odd and greater than 0.

| Fact | Value |
|---|---|
| Footprint | the host in the center plus **8 filler cells** in the same Y layer: a **3 x 1 x 3** plate (corners and edges, each with a render hint such as `render=icbm:door.filler;north.west`) |
| Fillers | mimic blocks. Default grass; the owner can right-click a filler with any block item to change it (stored per cell as `mimic_block_<i>` NBT) |
| Open state | `doorRotation` 0 to 90 degrees; `openingSpeed` 0.5 per tick, so a full open or close takes **180 ticks (9 s)** |
| Open trigger | any redstone on the structure, or `forceOpen` (set by a silo that is about to fire, or toggled by right-clicking the door with redstone) |
| Collision | none when fully open (`isOpen`), full when not |
| Sound | piston sound on state change |
| Sync | rotation, open flag and mimic blocks are sent in the description packet; mimic blocks refresh every 3 ticks |
| NBT | `playerSetOpen`, `isOpen`, `shouldBeOpen`, `doorRotation`, `doorID`, `mimic_block_<i>` |

The door is linked to a silo by the link tool (or by link code); the silo then drives it. The opening time is the main launch delay: 9 s before any fire, then the door stays open for 200 ticks after firing. If the door's `doorID` fails to resolve there is a fallback null door with unset render and layout ids (ICBM-A1-18).

## 5. Missile magazines (`storage/small/TileSmallMag.java`, `storage/smallMag.json`)

A feed rail that moves stored missiles toward a silo.

| Fact | Value |
|---|---|
| Accepts | SMALL missiles, one per magazine block |
| Footprint | **1 x 3 x 1** column (host + proxies at y+1, y+2); selection box 1 x 0.2 x 1 flat, render bounds 1 x 3 x 1 |
| Rotation | four horizontal directions via metadata 2 to 5, wrench-rotatable |
| Movement | every 20 ticks it queues a move; the missile waits **40 ticks (`MISSILE_MOTION_DELAY`)** and then is handed to the block in front if that block is a magazine or silo (anything implementing `IMissileMagOutput`: `canAcceptMissile`, `storeMissile`). A successful move wakes the neighbor magazines so a chain runs |
| Interaction | player insert/remove like a silo; harvesting drops the stored missile |
| Recipe | `ICI / IMI / GBG`: iron rods, advanced circuit, motor, iron gears, iron plate |
| Textures | power-rail style arrow textures for direction display |

A magazine line therefore feeds a silo's single slot; the silo does not hold more than one. This is "ammo storage" only, with no extra launch capability. The magazine only checks that the next block accepts the missile, and the silo only takes SMALL (or STANDARD for the standard silo; there is no standard magazine).

## 6. Launcher pad (`launcher/standard.json`, noted for completeness)

The standard launcher block has four multiblock layouts keyed by use id (micro, small, standard, medium): column cells of heights 2, 3, 6 and a **3 x 19 x 3** tower for medium (render bounds 7 x 30 x 7). Only the footprint data is listed here. Launchers belong to `04-launch-control` in the Classic spec. Medium missiles have a launcher but no silo.

## 7. What D8 can take from this

- A silo needs a single missile slot and a size filter, nothing more. Rail/hopper feeding is cheap to add.
- A door that must be open before the shot gives a fixed delay and a visible tell; the lever is "silo sets `forceOpen`, door owns the animation".
- Size-per-silo (small / standard) maps neatly onto GregGuns tiers (T1 and T2 on the pad, T3 and up in the silo).
