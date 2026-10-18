# 07-blocks: overview

Behavioural spec of Techguns 0.1.2's **non-machine blocks**, their renderers and models, the connected-texture setup, the (inactive) ForgeMultipart code, and the full `textures/blocks/` and `sounds/blocks/` asset folders.

## Source scope

- `java/techguns/TGBlocks.java`
- `java/techguns/TGMultiPartRegister.java`
- `java/techguns/blocks/*.java`: 32 classes, excluding `blocks/machines/`
- `java/techguns/blocks/ctm/*`
- `java/techguns/blocks/multipart/*`
- `java/techguns/client/renderer/block/*`
- `java/techguns/client/models/blocks/*`
- `java/techguns/client/renderer/item/RenderBlockModelItem.java`, `RenderBlockModelItemChest.java`, `RenderBlockModelItemTNTCharge.java`
- `resources/assets/techguns/textures/blocks/`: 163 PNGs and 9 `.mcmeta` files
- `resources/assets/techguns/sounds/blocks/`: 3 OGGs

Machine blocks (`basicMachine`, `oreDrill`, `machineMultiBlock`) are owned by **08-machines-production** and **09-machines-utility**. This area only documents their item-block classes that live in `blocks/`, the variations registered in `TGBlocks`, their model geometry, and their textures. World placement of blocks is in **11-worldgen**. Projectile interactions (Bio Blob, camo nets burning) are in **04-projectiles-fx**. Block recipes are in **06-items/recipes.md** (B11–B34).

## What a re-implementation needs, in short

- **Decorative blocks** (one block id per family, metadata = variant):
  - shipping containers (6)
  - concrete (7, including a pillar CTM variant)
  - metal panels (8)
  - connected metal panels (7, including a 3×3 pattern variant)
  - hardened bug sand

  Most use connected textures from the Chisel API.
- **Shaped utility blocks:**
  - connecting sandbag walls
  - vertical camo nets with wooden poles, and flat camo net roofs (3 camo variants each)
  - 4-colour metal ladders
  - wall lamps and lanterns (yellow and white)
  - a hand-openable metal double door with a custom open sound
  - a radar screen and control panel with animated glowing overlays
- **Storage:** Reinforced Chest and Weapon Chest. 36-slot chests that pair into doubles; a wrench picks them up with their contents.
- **World and gameplay blocks:**
  - titanium iron / uranium ore
  - unbreakable ore clusters with a tinted overlay, mined by the ore drill
  - alien nest blocks: bug sand, glowing eggs, climbable slime, and monster-spawner "holes"
  - the Bio Blob goo
  - Creeper Acid fluid
  - an Air Marker editor block

## Files

| File | Contents |
|---|---|
| [block-catalogue.md](block-catalogue.md) | Every block registered in `TGBlocks`: registry and unlocalized names, class, material, hardness/resistance, light, harvest, sound, item block, metadata, drops, oredict. Variation tables for the Chisel blocks. Unregistered blocks. Recipe cross-references. |
| [special-blocks.md](special-blocks.md) | Per-family behaviour: placement, connection rules, bounds, rotation, redstone, contact damage, spawners, ore-cluster type tables, chest mechanics, item-block placement hooks, `TGChiselBlocks` constants, unused classes. |
| [connected-textures.md](connected-textures.md) | How CTM is wired (Chisel API, not Techguns code). The CTM, CTMV and V9 texture layouts and the per-quadrant algorithm. The dead stand-in `CarvingRegistry`. |
| [multipart.md](multipart.md) | ForgeMultipart status (inactive: no microblocks) and the dormant part classes. |
| [block-rendering-and-models.md](block-rendering-and-models.md) | ISBRH renderers and how render ids are assigned. Cube helpers and their UV conventions. Geometry and animation of every block model. Block item renderers and their transforms. |
| [textures.md](textures.md) | All 163 textures in `textures/blocks/`: size, depiction, layout, palette, animation, user. Includes machine textures for 08/09. |
| [sounds.md](sounds.md) | The 3 door sounds and when they play. |
| [lang-and-tooltips.md](lang-and-tooltips.md) | Every block lang key with its English text, plus code-generated tooltip and chat lines. |

## Coverage

| Item | Covered / in scope |
|---|---|
| Source files | 69 / 69: TGBlocks, TGMultiPartRegister, 32 `blocks/`, 3 `ctm/`, 2 `multipart/`, 12 `renderer/block/`, 15 `models/blocks/`, 3 item renderers. The large model constructors were read through a box-extraction script; their render methods were read in full. |
| Textures | 163 / 163 (every one viewed). 9 `.mcmeta` animations recorded. |
| Sounds | 3 / 3 |

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| BLK-01 | Door upper half duplicates the door item | `java/techguns/blocks/BlockTGDoor.java:224` | `getItemDropped` returns null only for meta 8. A right-hinged upper half (meta 9) also drops the door item, and the lower half then drops another. Vanilla checks `(meta & 8) != 0`. | gameplay |
| BLK-02 | Door sound only heard by the clicking client | `java/techguns/blocks/BlockTGDoor.java:211` | It uses `World.playSound(...)`, which is a no-op on the server, and plays at the block corner (integer coordinates, no +0.5). Other players hear nothing. A double door plays it twice. | cosmetic |
| BLK-03 | Door block has no name and is listed in the creative tab | `java/techguns/blocks/BlockTGDoor.java:28-36` | There is no `setBlockName`, so the name is `tile.null`. `setCreativeTab` exposes a raw block item that places a lone lower half. | cosmetic |
| BLK-04 | Camo nets lose their variant when broken or picked | `java/techguns/blocks/GenericBlockPane.java` (whole class), `java/techguns/blocks/GenericBlockVerticalPane.java` (whole class) | Neither overrides `damageDropped`, so drops and pick-block are always meta 0 (Woodland). | gameplay |
| BLK-05 | Camo Bench "back" on camo nets or ladders makes a negative item damage | `java/techguns/blocks/GenericItemBlockCamoChangeable.java:37` | `(index − 1) % count` is negative for index 0, giving damage −1 (nets) or −4 (ladders). The icon lookup `icons[meta]` / `textures[meta/4]` then throws ArrayIndexOutOfBounds while rendering. | crash |
| BLK-06 | Ladder that loses its support drops the wrong variant | `java/techguns/blocks/BlockTGLadder.java:247` | It passes `meta/4` as the metadata to `dropBlockAsItem`, so `damageDropped` yields 0 for every variant. | gameplay |
| BLK-07 | Ladder placement can produce facing −1 | `java/techguns/blocks/BlockTGLadder.java:178-212` | In the cascading checks, a later failed check overwrites an earlier success with −1. The stored meta is then `itemDamage − 1`: the previous variant's facing 3, or −1 for variant 0. | gameplay |
| BLK-08 | Bio Blob never poisons players | `java/techguns/blocks/Bioblob.java:147` | It casts to `EntityLiving` (mobs only). Players throw a ClassCastException, which is silently caught. | gameplay |
| BLK-09 | Bio Blob lang key mismatch | `java/techguns/blocks/Bioblob.java` (no `getUnlocalizedName` override) | The name is `tile.Bioblob.name` but the lang defines `techguns.tile.Bioblob.name`. | cosmetic |
| BLK-10 | Glowing eggs light value 75 | `java/techguns/TGBlocks.java:146` | `setLightLevel(5.0f)` means 15 × 5 = 75. That exceeds the 0–15 range: light spreads oddly and is stored truncated. 5/15 was probably meant. | visual |
| BLK-11 | Bug sand never spawns bugs | `java/techguns/blocks/BlockBugSand.java:13-30` | The `updateTick` spawn logic is unreachable because the constructor sets `setTickRandomly(false)` and nothing schedules ticks. | gameplay |
| BLK-12 | Zombie Hole drops dark concrete | `java/techguns/blocks/BlockTGMonsterSpawner.java:91-99` | Meta 1 drops `concrete` with damage 1 (Dark), although it is textured as `concrete01` (meta 0). | cosmetic |
| BLK-13 | Weapon Chest acts as a redstone source | `java/techguns/blocks/BlockTGChest.java:611-626` | `canProvidePower` is `field1 == 1`, copied from vanilla's trapped chest. Type 1 is the weapon chest, so it emits power while opened. Probably unintended. | gameplay |
| BLK-14 | Chest security is non-functional | `java/techguns/blocks/BlockTGChest.java:496-532`; `tileentities/TGChestTileEnt.java:515` | The owner and security code is commented out. "[DENIED]" can only trigger on the distance check (effectively never), and the security lang keys are orphaned. | cosmetic |
| BLK-15 | Wrench pickup spawns the chest item at the block corner | `java/techguns/blocks/BlockTGChest.java:517` | The `EntityItem` is created at integer x, y, z rather than the centre. | cosmetic |
| BLK-16 | Chest item third-person transform includes debug offsets | `java/techguns/client/renderer/item/RenderBlockModelItemChest.java:27` | It adds `Keybinds.X/Y/Z` (debug tuning values) to the equipped transform. | visual |
| BLK-17 | Ore drill controller error shows a raw key | `java/techguns/blocks/ItemBlockOreDrill.java:137` | It uses `msg.error.oredrill.controllernext` (missing the `techguns.` prefix) through `I18n`. | cosmetic |
| BLK-18 | Deep recursion in the rod-connection check | `java/techguns/blocks/ItemBlockOreDrill.java:149-177` | It recursively flood-fills the whole cluster with a list-based visited set. That is O(n²) and the recursion depth equals the cluster size, so huge clusters risk a StackOverflowError. | crash (unlikely) |
| BLK-19 | Ore dictionary typo | `java/techguns/TGBlocks.java:265` | `oreIllmenite` should be `oreIlmenite`, so it will not match other mods. | cosmetic |
| BLK-20 | Screen and control-panel overlay "glow" does not work as intended | `java/techguns/client/renderer/block/RenderDirectionalBlock.java:38-69` | The GL lighting and lightmap calls inside an ISBRH run during chunk compilation and do not affect the tessellated quads. The overlay faces take whatever brightness the tessellator last had. | visual |
| BLK-21 | Missing-icon registrations | `java/techguns/blocks/GenericBlockMetadata.java:38`; `java/techguns/blocks/BlockTGMonsterSpawner.java:63` | `super.registerBlockIcons` registers an icon for a null texture name, which logs missing textures. | cosmetic |
| BLK-22 | Negative machine rotation for camo and repair benches | `java/techguns/blocks/ItemBlockMetaDataMachine.java:66-73` | `(facing − 2) % 4` gives −2 or −1 for two facings (source comment: "just don't ask why"). It may only work by accident in the renderer. | visual |
| BLK-23 | Ore cluster base texture follows the viewer's dimension | `java/techguns/blocks/OreCluster.java:169-177` | The stone, netherrack or end-stone choice uses the client player's dimension, not the block's world. | cosmetic |
| BLK-24 | Debug print in dead helper | `java/techguns/blocks/BlockBugSlime.java:47` | `getDirectionMeta` prints "meta:" to stdout. Never called. | cosmetic |
| BLK-25 | Ore cluster ignores other mods' uranium | `java/techguns/TGBlocks.java:258,426` | `oreUranium` is pre-set to Techguns' ore before `initOre`, which only fills nulls. | gameplay |
| BLK-26 | Acid fluid block in the creative tab | `java/techguns/blocks/BlockFluidAcid.java:28` | `setCreativeTab` on a fluid block exposes a placeable source item. | cosmetic |
| BLK-27 | Custom ore cluster tooltip NullPointerException | `java/techguns/blocks/ItemBlockOreClusterCustom.java:30-34` | A stack for an unset slot (e.g. via `/give`) has `ores[meta] == null`, which crashes the tooltip. | crash |
| BLK-28 | Bio Blob collision uses shared mutable bounds | `java/techguns/blocks/Bioblob.java` (no collision override) | Collision uses the last bounds set by any blob, which depend on size and side. Only the selection box is recomputed. | gameplay |
| BLK-29 | Inconsistent face normals in the cube helper | `java/techguns/client/renderer/block/TessellatorCubeProper.java:187-235` | Normals are mislabelled (e.g. "back" uses (−1, 1, 0)), giving slightly wrong lighting in item rendering. | visual |
| BLK-30 | Oil-hole cluster item shows stone, not bedrock | `java/techguns/client/renderer/block/RenderOrecluster.java:47` | The inventory render always uses the stone base icon. | cosmetic |
| BLK-31 | Flag item renders as a repair bench | `java/techguns/client/ClientProxy.java:607` (wiring for `RenderBlockModelItem`) | `basicMachine:7` (Flag) is mapped to the repair-bench cube instead of `ModelFlagPost`. | visual |
| BLK-32 | Dead "thin door sides" conditions | `java/techguns/blocks/BlockTGDoor.java:130-133` | They compare a 0–3 value against 5 and 6, so they can never match. | cosmetic |

## Dead or unused code (not bugs, for awareness)

- **Never registered:** `BlockTGLampFMP`, `smallCube`, `steelframe_glass`, all multipart classes, `TGMultiPartRegister`.
- **Never used or instantiated:** `TurretBase`, `BlockBugNest`, `ItemBlockOreCluster`, `CarvingRegistry` (assigned to `TGBlocks.chiselRegistry`, never read), `ModelSimpleTurret`, `ModelLadder01` (passed but never drawn), `ReactionChamberFluidBox`.
- **Unused textures** (listed in [textures.md](textures.md)): `tent`, `flagBlock`, `Bioblock`, `ladder01`, `ladder01_inv`, the legacy `oreCluster*` set (11 files), `reactionChamber*` (5), `paintmachine_top/front/side/bottom`, `drillFrameCorner`, `drillRodSide`, `drillRodTop`, `steelframe_glass`.
