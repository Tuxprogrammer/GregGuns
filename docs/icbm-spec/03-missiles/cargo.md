# Cargo Holders and the Player Seat (compact)

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. GregGuns **defers** all of this (cargo, parachutes, balloons, the cargo hook), so these sheets are compact: identity, numbers, behavior and bugs. The cargo system is a general "put one item, block or mob under a thrown holder" feature; the cluster missile ([cluster-missile.md](cluster-missile.md)) and the held-item missile ([missile-item.md](missile-item.md)) both reuse it. The projectile base is [projectile-base.md](projectile-base.md). The player seat is included because the projectile base excludes it from collisions; it belongs to launchers ([04-launch-control](../04-launch-control/launcher-base.md)).

Capabilities, the recipe registry and the config system are *1.12-only mechanisms*.

---

## A. Cargo holders (parachute and balloon)

### Identity

| Item | Value |
|---|---|
| Classes | `content/cargo/`: `ItemThrowableProjectile`, `CargoProjectileData`, `ProjectileCargoMode`, `CargoHolderHandler`, `RecipeCargoData`, `RecipeCargoDataDecraft`; `parachute/`: `EntityParachute`, `ParachuteProjectileData`, `RenderParachute`; `balloon/`: `EntityBalloon`, `BalloonProjectileData`; `placer/`: `BlockCargoHook`, `TileEntityCargoHook`, `TileEntityCargoHookTicking`. Config `config/missile/ConfigCargoItem` |
| Items | `icbmclassic:parachute` ("Payload Parachute", tooltip "Allow slowing deployed payloads") and `icbmclassic:balloon` ("Payload Balloon", "Comical way to drop payloads"); both are `ItemThrowableProjectile`, stack 16, ICBM tab |
| Entities | `icbmclassic:parachute` and `icbmclassic:balloon`, both on the projectile base, tracking range 500, update every 1 tick, size 0.5 (parachute 0.5 x scale) |
| Projectile data keys | `icbmclassic:holder.parachute`, `icbmclassic:holder.balloon` |
| Registered recipes | dynamic: `icbmclassic:parachute_cargo`, `parachute_cargo_decraft`, `balloon_cargo`, `balloon_cargo_decraft` |
| Cargo hook block | `BlockCargoHook` + two empty tile classes: **not registered anywhere**, dead code (ICBM-03-73) |
| Oredict | none |

### Obtaining

| Thing | How |
|---|---|
| Parachute x4 | shaped: carpet (white, data 0) top center; carpet, slime ball, carpet; three string (`recipes/parachute.json`) |
| Balloon x4 | shaped: string, carpet, string / carpet, blaze powder, carpet / string, carpet, string (`recipes/balloon.json`; carpet is white, data 0) |
| Loading | `RecipeCargoData`: exactly two stacks in the grid, one empty or loaded holder of the matching type and one other item that passes the ban list. Result: the holder with the item (count 1) as its cargo, mode ITEM. The container item is not returned |
| Unloading | `RecipeCargoDataDecraft`: a lone holder (stack count 1) with cargo in the grid. The result is the held item; the holder stays in the grid emptied (returned through the remaining-items hook). Empty holders also match and produce an empty result |
| Creative stacks | empty; parachute with an egg (ITEM), a furnace (BLOCK), TNT (BLOCK); balloon with an egg (ITEM) |

### Numbers

| Name | Parachute | Balloon |
|---|---|---|
| Gravity (per tick, applied after drag) | 0.01 | 0.005 while descending; **-0.02** (upward) for the first `FLOATING_DURATION` = 600 ticks |
| Air resistance (velocity kept per tick) | 0.95 | 0.90 |
| Terminal speed (`gravity / (1 - drag)`) | 0.2 block/tick down (4 blocks/s) | 0.2 block/tick up while lifting; 0.05 block/tick down afterwards |
| Pop chance | none | 5% per tick once the lift has ended (about 20 ticks on average) |
| Throw velocity | 0.5 block/tick along the thrower's look, no randomness | same |
| Lifetime | in-air limit 1,200 ticks (60 s) | same |
| Passenger render scale | 1 for a dropped item, 2 for a block or a mob | fixed |
| Passenger seat height | item -0.7 (-0.6 for a block item), flying block -1.5, other `+height - 0.25` | item -0.65, other `+height - 0.55` |
| Hold to throw | 60 ticks maximum use, `energy` unused (throw speed is fixed) | same |

Config (`icbmclassic/missile`, group `cargo_holder`): `item_ban_allow.ban` (default true) and `item_ban_allow.list` (default empty). With the defaults everything is allowed. The comment on these keys wrongly mentions the cluster missile.

### Behavior

1. **Throw** (`ItemThrowableProjectile`): right-click starts a 60-tick charge; releasing on the server builds the holder entity from the stack's projectile data (an empty stack makes an empty holder), aims it from the thrower's head at 0.5 block/tick, spawns it, and only then calls the data's `onEntitySpawned` so the cargo can be attached as a passenger. Survival players lose one item (see ICBM-03-68). Holding for the full 60 ticks cancels the throw (ICBM-03-61).
2. **Cargo spawning** (`CargoProjectileData.onEntitySpawned`, server) by mode:
   - ITEM: spawn a dropped item copy as the passenger.
   - BLOCK: for a block item, take the block's placement state and spawn a flying block (the 02 entity) as passenger; otherwise fall back to ITEM.
   - ENTITY: only for a spawn egg: spawn the mob, apply the egg's entity data and ride the holder; else ITEM.
   - PROJECTILE: spawn the stack through the projectile registry and ride the holder; else ITEM.
   If an entity fails to spawn or to board, the failure is logged ("likely resulted in loss of items") and the cargo is gone.
   Crafted holders are always ITEM mode; the other modes exist only on creative stacks, NBT or other mods (ICBM-03-70).
3. **Flight:** gravity and drag as in the table. Entity hits are ignored (only blocks matter); the thrower is skipped.
4. **Landing or expiry:** a block hit releases at once. A parachute removes its passengers, dies and **drops its own item** (`dropItemStack`, a new parachute; a TODO suggests a "used" item). A balloon removes passengers and dies (no drop). Both also release after 1,200 in-air ticks (60 s), which for a long fall can happen mid-air (ICBM-03-72). A parachute with no passengers expires on its first update and drops its item back (this is what an unloaded holder does); a balloon with none expires too.
5. **Parachute only:** one passenger at most; the holder grows to scale 2 when a non-item passenger boards. Its `shouldExpire` replaces the base check, so it has no ground timer and no below-world kill. A balloon's lift counter only runs while the holder is moving.

NBT: parachute `renderItem`, `dropItem` (stacks); balloon `lift_ticks` (int), `render_stack`; held cargo is a normal item/entity passenger, so it is saved with the holder's passengers. Item NBT (projectile capability, key `data`): the buildable data with `stack` (the held item) and `mode` (one of ITEM, BLOCK, ENTITY, PROJECTILE; names are saved as strings and must not change). The spawn packet sends the render stack (and `ticksInAir` for the parachute, `liftTicks` for the balloon).

Tooltip (client): with no data, the item's `.info` line. With data: the mode line `projectile.icbmclassic:holder.<parachute|balloon>.info.<item|block|entity>` ("Deploys item with parachute", "Deploys block with balloon", "Deploys entity with parachute", ...), then "Held Item" in dark gray with the held stack's own tooltip, or "Held Item: <empty>". A disabled item adds the "disabled by config" line. The PROJECTILE mode has no lang key (ICBM-03-70). Dev-error messages if a holder cannot be thrown: `error.icbmclassic:projectile.throwing.interface` = "Dev Error: Missing %1$s" and `.type` = "Dev Error: Missing Projectile Type %1$s".

Assets: item textures `textures/items/parachute.png` (16 x 16, box model `models/item/parachute.json`), `textures/items/balloon.png` and `balloon_craft.png` (16 x 16 each), `textures/blocks/parachute.png` (16 x 16); `blockstates/balloon.json` is a Forge item-state file giving the balloon a flat inventory icon (`balloon_craft.png`) and a 3D model (`balloon.png`) for hand and world. The parachute entity renders its item scaled; the balloon uses the generic item-as-entity renderer. Sounds: none.

### Bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-03-68 | Throw item consumption has an operator-precedence error (logic) | `content/cargo/ItemThrowableProjectile.java:88` | The condition is `(server && threw && !isPlayer) \|\| !creative`. A survival player's stack is reduced on both sides every time, even when the throw failed or produced nothing; the client also shrinks its own copy ahead of the server; a failed throw by a non-player throws a cast exception | gameplay |
| ICBM-03-69 | Cargo recipe depends on slot order and allows nesting (logic) | `content/cargo/RecipeCargoData.java:46-67,59-62` | `hasCargoItem` is overwritten by each holder stack, and the rule that refuses cluster and held-item missiles only triggers if the missile comes before the holder in slot order. Two parachutes match (one becomes the other's cargo) | gameplay |
| ICBM-03-70 | Only ITEM mode is craftable; PROJECTILE mode text is missing (logic) | `content/cargo/CargoProjectileData.java:58,74`, `RecipeCargoData.java` (`getCraftingResult`) | The recipe builds the default data (ITEM). BLOCK, ENTITY and PROJECTILE can only come from creative, NBT or API use. The tooltip key `holder.<x>.info.projectile` is not in the lang file | cosmetic |
| ICBM-03-71 | Spawn-egg mode names the holder, not the mob (logic) | `content/cargo/CargoProjectileData.java:125-129` | The custom name of the egg is applied with `entity.setCustomNameTag` where `entity` is the parachute or balloon, though the check is on the mob | cosmetic |
| ICBM-03-72 | 60-second timer releases cargo mid-air (logic) | `lib/projectile/EntityProjectile.java:81-85,353`, `content/cargo/parachute/EntityParachute.java:83` | A parachute falls at 0.2 block/tick, so a drop from more than about 240 blocks up is released while still falling. A balloon spends 600 of its 1,200 ticks lifting. Cargo dropped from high-altitude missiles can be released early | gameplay |
| ICBM-03-73 | Cargo hook block is unregistered dead code (logic) | `content/cargo/placer/BlockCargoHook.java:40-45` | The block is never registered, has no behavior, no meta mapping for its `ticking` property, and the tile choice treats meta 1 as the non-ticking tile (so every tile would be the ticking one). Both tile classes are empty | cosmetic |
| ICBM-03-75 | Decraft returns the held stack itself and matches empty holders (logic) | `content/cargo/RecipeCargoDataDecraft.java:30-48,99` | The result is the cargo stack object from the input, not a copy; an empty holder matches with an empty result | cosmetic |

---

## B. Player seat (`EntityPlayerSeat`)

### Identity and numbers

| Item | Value |
|---|---|
| Class | `content/entity/EntityPlayerSeat` (extends `Entity`; the renderer is `RenderSeat`, in `07-visuals-audio`) |
| Entity | `icbmclassic:holder.seat` (`ICBMEntities.MISSILE_SEAT`), tracking range 50, update every 2 ticks. Spawn data: a flag plus the host's block position |
| Purpose | a fixed invisible mount placed on a missile launcher so a player can sit on the missile; owned by the launcher tile (`TileLauncherBase`, [04-launch-control](../04-launch-control/launcher-base.md)) |
| Box | upright launcher: 0.5 wide, 2.5 tall when empty, 0.5 tall when ridden; sideways launchers: 2.5 long when empty, 0.5 when ridden |
| Passenger offset | `rotation face * 0.2` plus `launch face * 0.5`; for a down-facing launcher minus the seat's height, for a sideways one minus 0.6 in Y and half the width along the face |
| Eye height | 0 |

### Behavior

- Each tick: if the seat has no host but a saved host position, look the tile up and re-link. On the server, remove the seat (dismounting riders safely) when the host is missing or invalid or the seat is below Y -64. If the launcher's facing, seat side or occupied state changed, recompute the offsets, the position (host center plus half a block along the launch face), the rotation (Euler angles from the seat side) and the bounding box.
- Right-click: not while sneaking; if already ridden, the click is consumed; a creative player holding a vanilla minecart gets an empty minecart spawned and seated instead; anyone else mounts it (server side).
- The seat never moves, takes no velocity, pushes nothing, has no collision with passengers and has none at all while ridden. The projectile base skips seats when testing hits, so missiles do not hit their own mount.
- NBT: `pos` (block position of the host); the spawn packet carries the same.

### Bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-03-74 | Dismount safety check compares Y with Z (logic) | `content/entity/EntityPlayerSeat.java:91` | `Math.abs(prevY - entity.posZ)` should use `posY`. The "player warped to the bottom of the map" fix does not trigger when only Y changed, and can trigger when Y and Z happen to differ | gameplay |
