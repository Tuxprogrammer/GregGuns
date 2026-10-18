# Utilities

Source: `java/techguns/util/*`, `dispenser/*`, and the static inventory helpers in `Techguns.java`.

## Inventory helpers (static, in `Techguns`)

Two stacks "match" when they have the same item and the same damage value. NBT is ignored.

| Method | Behaviour |
|---|---|
| `addItemToInventory(inv[], stack, start, end)` | Works on a copy of the stack. First pass: top up partial stacks that match `OreDictionary.itemMatches(slot, stack, strict = true)` (item and damage; NBT ignored) up to the max stack size. Second pass: put the remainder into the first empty slot. Returns the number of items that **didn't** fit (0 = all fit) |
| `addAmmoToPlayerInventory(player, stack)` | Fills Techguns ammo slots 6–11 first. If anything is left over, it then adds **the original full stack** (not the remainder) to the main inventory, which duplicates the part already stored (CORE-04). Without extended properties, it goes straight to the main inventory. Returns the number that didn't fit |
| `consumeAmmoPlayer(player, ammo)` | For an amount of 1: remove 1 from the first match in ammo slots 6–11, otherwise from the main inventory. For amounts above 1: check that the combined supply in both areas covers the amount (`canConsumeItem`), then consume from the ammo slots and take the rest from the main inventory. Returns success. Returns false when the player has no extended properties |
| `consumeAmmo(inv[], ammo, start, end)` | Decrements the first match by 1 and nulls the slot when it empties. Returns true if something was taken |
| `countMags(inv[], stack, start, end)` | Sum of stack sizes of matching items (used by the HUD) |
| `canConsumeItem(inv[], stack, start, end)` | Returns how many of `stack.stackSize` are **missing** (0 = available) |
| `consumeItem(inv[], stack, start, end)` | Takes up to `stack.stackSize` across several stacks and returns the shortfall. It does not pre-check |
| `consumeFood(inv[], start, end)` | Takes 1 from the first `ItemFood` stack and returns that single item (used by the auto-feeder) |

## ProjectileExplosion

A custom explosion used by grenades, rockets and energy blasts (04). It holds a dummy vanilla `Explosion` so it can call vanilla and Forge APIs that need one.

**Construction:** (world, x, y, z, exploder `EntityLivingBase`, explosionSize, entityDamage int, dmgRadius1, dmgRadius2).

**Optional settings:**
- a `TGDamageSource` override;
- the explosion sound (default `random.explode`);
- `isFlaming`.

**`doExplosion(blockDamage, attacker)`:**
- On the server it first posts `ExplosionEvent.Start` with the dummy explosion. If that is cancelled, nothing happens.
- Then it runs phase A and phase B. The client runs both phases without the event.

**Phase A: block selection.** This is the vanilla ray algorithm:
1. Cast rays from the centre towards each of the 16×16×16 cube-surface directions.
2. Each ray starts with strength explosionSize × (0.7 + 0.6·rand).
3. The ray steps 0.3 blocks at a time, losing 0.225 strength per step.
4. At a solid block it also loses (resistance + 0.3) × 0.3. The resistance comes from the exploder entity when there is one, otherwise from the block.
5. A block is added to the affected list while strength > 0 and the exploder (if any) allows it.

**Phase A: entity damage.**
1. `explosionSize` is doubled temporarily. Entities are collected from a box of half-size dmgRadius2 + 1 around the centre, excluding the exploder.
2. On the server, `ForgeEventFactory.onExplosionDetonate` is called with the dummy explosion and this entity list. The dummy's block list stays empty, so protection mods can't remove blocks from it (CORE-19).
3. For each entity within distance d ≤ dmgRadius2 (and d > 0):
   - `damage = entityDamage × min(1, 1 − (d − dmgRadius1) / (dmgRadius2 − dmgRadius1))`. That is full damage inside r1, falling linearly to 0 at r2. If r1 == r2 the expression divides by zero (CORE-20).
   - If an attacker is given and the target is living, multiply by `DamageSystem.getDamageFactor(attacker, target)` (the PvP, turret and NPC config factors).
   - Damage source: the override if one was set, otherwise `TGDamageSource.causeExplosionDamage(exploder, null, GORE)` with gore chance 0.35.
   - Knockback: `(1 − d/(2·size)) × blockDensity`, adjusted by Blast Protection (`EnchantmentProtection`), and added along the centre-to-eye direction.
   - Players are put in a knockback map, but that map is never sent to clients, so players don't feel the knockback (CORE-19).

**Phase B:**
1. Play the explosion sound at volume 4 and pitch (1 + (r1 − r2)·0.2) × 0.7, where r1 and r2 are two random floats (so the pitch varies slightly).
2. If `blockDamage`, each affected non-air block drops as an item with chance 1/explosionSize (when it allows drops from explosions) and is then exploded.
3. If `isFlaming`, each affected air block that sits on an opaque block catches fire with a 1-in-3 chance.

There are no particles here. A private `createExplosionEffect` routine (legacy Explosion, Fireblast ring and LargeSmoke particles scaled by dmgRadius2) is never called.

## Damage sources

- **`BulletDamageSource`** (`EntityDamageSource`, type `"bullet"`): the projectile is the direct source and the shooter is the attacker. It is marked as a projectile, and `ArmorPiercing` sets "bypasses armor". It carries a `DeathType`. The death message uses `death.attack.bullet` (or `.item` when the shooter's held item has a custom name). Those keys **don't exist in the lang file**; only `death.attack.tg_bullet` does (CORE-34). It is legacy: current guns use `damagesystem.TGDamageSource` (02).
- **`TGExplosionDamageSource`**: carries a `DeathType` and an attacker. It overrides the source entity and the attacker. Legacy as well.

## EntityDeathUtils

`DeathType` enum, with values sent as ints:

| Value | Name |
|---|---|
| 0 | DEFAULT |
| 1 | GORE |
| 2 | BIO |
| 3 | LASER |
| 4 | DISMEMBER |

**Gore list:** these entity classes are eligible for gore (exact class match):
- vanilla: Player, Zombie, Skeleton, Enderman, Creeper, Cow, Sheep, Pig, Chicken, PigZombie, Spider, CaveSpider, Witch, Slime, Horse, Mooshroom, Wolf, Squid, Ghast, Villager;
- Techguns: ZombieSoldier (listed twice), ArmySoldier, CyberDemon, ZombiePigmanSoldier, ZombieFarmer, ZombieMiner, Bandit, PsychoSteve, DictatorDave, SkeletonSoldier, AlienBug.

**`hasSpecialDeathAnim(entity, type)`:**
1. If the entity is a boss (`IBossDisplayData`): true only if it is a `GenericNPC`.
2. Otherwise, BIO or LASER: always true.
3. Otherwise: true if the entity's exact class is in the gore list. This holds for any type, so DISMEMBER is also allowed for listed classes.

## MathUtil

- `sin360`, `cos360`: sine and cosine of an angle in **degrees**, using MathHelper's lookup table.
- `clamp` (float and int), `abs(int)`, `inRange(v, min, max)` (inclusive).
- `randomInt(rand, a, b)`: inclusive on both ends, in either order. `randomFloat(rand, min, max)`.
- `polarOffsetXZ(x, z, r, angleRad)`: returns (x + r·cos a, z + r·sin a).
- `traceVector(world, src, dir, distance, steps, ignoreEnt, shooter)`: a segmented ray trace.
  1. Normalise the direction, then for each of up to `steps` segments of length `distance`:
  2. Ray-trace blocks with liquids ignored. In vanilla's argument order the flags are: stop on liquid = false, ignore blocks without a bounding box = true, return the last uncollidable block = false.
  3. Test entities found in the segment's box expanded by 1, excluding `ignoreEnt`, the shooter, dead entities and non-collidable ones. Each entity's bounding box is grown by 0.3 and intersected with the segment, and the nearest hit is kept.
  4. An entity hit overrides a block hit. The first segment with any hit returns it. If no segment hits, the result is null.
- `interpolateRotation(a, b, t)`: shortest-path angle lerp, as in vanilla's living renderer.
- `rotateAroundX`, `rotateAroundY`, `rotateAroundZ(vec, rad)`: rotate a `Vec3` in place.
- `Vec2` (x, y doubles):
  - `lenSquared`, `len`, `normalize` (in place), `getNormalized` (returns (0, 0) for a zero vector), `dot`.
  - `getRotated(deg)`: rotates as `(x·cos + y·sin, y·cos − x·sin)`.
  - `add`.
  - `substract(A, B)` returns **B − A**, despite its doc saying A − B. As a result `getVecTo(p)` returns `this − p` and `getVecFrom(p)` returns `p − this`, both reversed from their names. Only squared lengths are used today, so nothing breaks yet (CORE-22).
  - The deprecated `getAngleBetween` is mathematically wrong.

## BlockCoords / BlockPosInd

`BlockCoords`: mutable int x, y, z.
- **Constructors:** copy, from a tile entity, or from x/y/z.
- `shift(dir, n)` and `getShifted(dir, n)` move by n along a `ForgeDirection`: DOWN/UP = ∓y, NORTH = −z, SOUTH = +z, EAST = +x, WEST = −x.
- `getBoxWithOffsets(minX, maxX, minY, maxY, minZ, maxZ, includeOrigin)`: all positions in the inclusive offset box. Used by the multiblock machine checks (08).
- `getCuboid(dir, radius, length, round)`: a (2r+1)² cross-section extruded `length` blocks along `dir`. With `round`, the four corners of each slice are skipped.
- `equals` compares coordinates. There is **no** `hashCode` override.

`BlockPosInd` extends it with `index`, the palette index used when scanning and placing structures. Its `toString` is `x,y,z,index`.

## MBlock / MultiMBlock (structure block palette)

**`MBlock`** has these fields: `block`, `meta`, `hasRotation`, `rotationType` (`BlockUtils.BlockType`) and `pass` (placement pass). Equality is by block and meta. A block equal to `TGBlocks.airMarker` places air.

**`getRotationMeta(r)`** applies r clockwise quarter-turns:

| BlockType | Meta cycle per quarter-turn |
|---|---|
| TG | delegated to `GenericBlock.getRotationMeta(meta, r)`, otherwise unchanged |
| DOOR | 0→3→2→1→0 and 7→6→5→4→7 |
| FENCE_GATE | (m + r) mod 4, keeping the 4-bit (open flag) |
| LADDER | 5→2→4→3→5 |
| STAIRS | 2→0→3→1→2 and 6→4→7→5→6 |
| BED | 0→3→2→1→0 and 8→11→10→9→8 |
| STAIRS2_CHISEL | 8→11→9→10→8 and 12→15→13→14→12 |
| LEVER | 1→4→2→3→1 and 9→12→10→11→9 |
| CHEST | 3→5→2→4→3 |

**`setBlock(world, x, y, z, rotation, lootTier)`:**
1. Place the block with the rotated meta (flag 2). For CHEST-type blocks the meta is set a second time with notify.
2. Fill containers from Forge `ChestGenHooks`:
   - vanilla chest: `dungeonChest` loot;
   - `TGBlocks.tgchest_weapon`: the `TGItems.WEAPONCHEST_LOOT_TIER[lootTier]` category;
   - `TGBlocks.tgchest`: `REINFORCEDCHEST_LOOT_TIER[lootTier]`.

   All of this uses a static `Random`, not the world seed.

**`MultiMBlock`** holds parallel arrays: blocks, metas, weights, rotation types and per-entry rotation flags.
- Each placement rolls an index: roll an integer in [0, total weight] **inclusive** and pick the first index whose running sum ≥ roll. Index 0 therefore gets one extra outcome (CORE-23).
- The dungeon-chest fill checks the *first* palette block, not the rolled one.

## BlockUtils

**Structure files:** `loadStructureFromFile(name)` reads `/assets/techguns/structures/<name>` from the classpath.
- Line 1: the entry count N.
- Then N lines `x,y,z,paletteIndex`, all shorts.
- Returns `short[N][4]`. The data files themselves are documented in 11.

**Placement:**
- `placeScannedStructure(world, blocks, palette, x, y, z, centerX, centerZ, rotation, pass, lootTier)`: for each entry whose palette block's `pass` equals `pass`, rotate (x, z) about (centerX, centerZ) by `rotation` quarter-turns (`Structure.rotatePoint`, 11) and place it at (x + rx, y + by, z + rz) with that rotation.
- `placeFoundation(…, range)`: for every entry at layer y = 0, also place the same block 1…range blocks **below**, only into air. This fills gaps under structures.
- `cleanUpwards(…, range)`: for every y = 0 entry, set the 1…range blocks **above** to air.
- `setBlockRotated` and `setBlockRotatedReplaceAirOnly` are the per-block forms. The latter rotates with its own inline loop: (a, b) → (b, −a) per quarter-turn.
- `getBlockDirMeta(type, dir)` and `getBlockForgeDirMeta(type, forgeDir)`: initial meta for a facing.
  - ForgeDirection maps to an index: WEST 0, NORTH 1, EAST 2, SOUTH 3, DOWN 4, UP 5.
  - Then: DOOR 2→0, 3→1, 0→2, 1→3; FENCE_GATE (d + 1) mod 4; LADDER 0→5, 1→3, 2→4, 3→2; STAIRS 0→0, 1→2, 2→1, 3→3; anything else 0.

**Height queries:**

| Method | Returns |
|---|---|
| `getGroundY` | heightmap − 1 |
| `getHeightValueTopBlockAll` | first non-air block from the top + 1, or −1 |
| `getHeightValueNoTrees` | top solid-or-liquid block (excluding leaves and foliage), stepped down past leaves, wood and foliage |
| `getHeightValueNoTreesIgnoreLiquid` | as above, but −1 if the top block is liquid (wood is not skipped) |
| `getHeightValueUnderwater` | also steps down through liquid |
| `getHeightValueLiquid` | −1 unless the top non-air block is liquid |

**Spawn validation:**
- `getValidSpawnY`, `getValidSpawnYUnderwater`, `getValidSpawnYWater`: sample the 4 corners of a sizeX × sizeZ footprint. Fail (−1) if any sample is −1, or if any corner differs from corner 0 by ≥ the limit. Otherwise return the mean of the two middle values.
- `getValidSpawnYArea(…, step)`: samples a grid with the given step. Fails on liquid or when max − min > limit; otherwise returns the mean height. The intended clamp of the last row and column to the far edge never triggers (CORE-25).

**Terrain shaping:**
- `flattenArea(x, z, sx, sz, maxDiff)`: computes a "median" ground height. It is really the lower-middle element, and it is 0 when there are ≤ 2 samples (CORE-24). Columns are remapped linearly into a band of width maxDiff centred on that value.
- `flattenAreaToMaxHeight`: raises every column to the highest one.
- `apply2DHeightmapFilter(…, kernel)`: convolves the height map with `FILTER_GAUSSIAN_3x3` (1/16, 1/8, 1/16 / 1/8, 1/4, 1/8 / …) or `FILTER_GAUSSIAN_5x5`.
- All of these adjust a column the same way:
  - raising: the column is filled up with the top block if it is ground (ground, grass, rock, sand or clay material), otherwise with dirt, keeping the meta;
  - lowering: the excess blocks are set to air.
- `removeJunkInArea`: from the top down, sets non-ground blocks to air until the first ground block. `removeJunkInAreaOLD` is deprecated.

**Biome palette:** `getBiomeType(world, x, z)` returns a `BiomeColorType`:
- DESERT: beach, desert, desert hills, mesa variants, savanna variants;
- SNOW: cold beach, cold taiga (+ hills), frozen ocean, frozen river, ice mountains, ice plains, mega taiga (+ hills), taiga (+ hills);
- NETHER: hell;
- WOODLAND: everything else.

It drives camo choices in worldgen (11).

**Shapes** (all place an `MBlock` with rotation 0):
- `fillBlocks`, `fillBlocksAir`, `fillBlocksWithMeta` (with a flag) and `fillBlocksHollow` (walls only, no floor or roof).
- `fillSphere(r)`: points with distance² < r².
- `fillSphere2(r, thickness)`: inner block inside (r − t)², shell block up to r².
- `fillCylinder(p1, p2, r, inner, shell)`: point-to-line distance < r − 1 → inner block, < r → shell block. It loops over the axis-aligned box extended by r, exclusive at the max side.
- `drawLine`: a 3-D Bresenham line. When the Y axis dominates, it also places side blocks when X or Z steps, keeping the line 4-connected.

**Misc:**
- `exists(path, suffix)`: client only. Checks whether `textures/blocks/<path><suffix>.png` exists in the resource manager. Used by directional blocks (07) to pick optional face textures.
- `selectEntitiesWithinAABB(world, box, filter)`: an entity query that walks chunk entity lists directly. It calls `getChunkFromChunkCoords` with no loaded-chunk check, so it can load chunks. The AT'd `chunkExists` check is commented out.

## DataWatcherUtil

`forceUpdateObject(dataWatcher, id)` uses reflection: fields `field_151511_a` (the owning entity) and `objectChanged`. It marks the watcher entry as changed so ItemStack contents that were mutated in place get re-sent. Reflection errors are printed.

## ItemUtil

| Method | Behaviour |
|---|---|
| `existsInOredict(key)` | the ore list is not empty |
| `isItemEqual(a, b[, strict])` | null-safe `OreDictionary.itemMatches`; two nulls are equal |
| `isItemEqualAndEnough(have, req)` | both null, or same item and damage with `have.size ≥ req.size` |
| `setStackSizes(list, n)` | sets every stack's size to n |
| `isFluidEqual(a, b)` | null-safe `FluidStack.equals` |
| `addToolClassesTooltip(map, tooltip)` | if the map isn't empty, adds a header line `techguns.tooltip.toolclasses` ("Toolclasses"), then one line per entry `"  <class> : <level>"` |
| `addShiftExpandedTooltip(tooltip)` | adds `"<shift1> §a<shift2> §7<shift3>"`, which reads "Press §a[SHIFT]§7 for detailed tooltip" (keys `techguns.gun.tooltip.shift1/2/3`). Used by guns and items that have detailed tooltips (02/06) |

## NPCUtil

- `spawnEntity(world, x, y, z, name)`: server only.
  1. Look up `EntityList` name `Techguns.<name>`.
  2. Create the entity at the position with a random yaw.
  3. Spawn it, call `onSpawnWithEgg(null)`, and play its living sound.
  4. Log "Entity not found …" when the name is unknown.
- `EntityByNameSelector(names)`: an entity filter for living, alive entities whose `EntityList` class-to-name string is in the list.

## PacketUtil (ItemStack wire format)

These copy vanilla `PacketBuffer` logic for raw `ByteBuf`s.

**ItemStack:**
- short item ID, or −1 for null;
- byte stack size;
- short damage;
- an NBT block. NBT is included only if the item is damageable or its "share tag" is true; otherwise the NBT block is written as null.

**NBT block:** short length (−1 for null), then GZip-compressed NBT bytes. Reading enforces a 2 MiB size limit. Because the length is a short, compressed NBT over 32767 bytes would overflow.

## TextUtil

`trans(key)` = `I18n.format(key)`. It is client-only (the I18n class is client-side).

## SoundUtil

See [client-effects-audio.md](client-effects-audio.md#sound-routing-soundutil).

## Dispenser behaviour

`dispenser.BehaviorProjectileDispenseDamagevalue` is registered for `TGItems.sharedItem`. It extends the default "drop item" behaviour, so the vanilla click sound and smoke still play.

| Shared-item meta | Spawned projectile | Parameters |
|---|---|---|
| `TGItems.rocketAmmo` | `RocketProjectile` | damage and radius from the rocket launcher; speed 1.0; time-to-live 200; spread 0.3; gravity 0.01; no block damage; penetration 0 |
| `TGItems._40mmGrenade` | `GrenadeLauncherProjectile` | damage, radius, speed and gravity from the grenade launcher; time-to-live 100; spread 0.05; no block damage; penetration 0; 2 bounces. Afterwards, `GrenadeLauncherTrail` FX is sent to everyone within 200 blocks |
| `TGItems.rocketAmmoNuclear` | `NukeRocketProjectile` | values from the nuke launcher, otherwise the same as the rocket |
| any other meta | (none) | the item is ejected as a normal item |

Each projectile spawns at the dispense position. It is aimed along the facing (with +0.1 Y) at velocity 1.1 and inaccuracy 6.0, then one item is consumed. Nothing records a shooter, so dispensed projectiles have no owner.
