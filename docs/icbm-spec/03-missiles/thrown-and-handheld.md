# Grenade and Rocket Launcher (compact)

Source root: `src/main/java/icbm/classic/` at `9c1d985a`. Two small handheld features. GregGuns **drops both** (it has its own grenade and launcher), so each gets a compact sheet: identity, numbers, behavior, bugs. The numbers are kept so a rebuild can match them if wanted. The shared fuse table for grenades is in `config/blast/ConfigFuseTimes` (the cart half is in [bomb-cart.md](bomb-cart.md)).

Neither the grenade entity nor the launcher uses the projectile base. The grenade is a plain `Entity`; the launcher spawns missiles, which do ([projectile-base.md](projectile-base.md)).

---

## A. Grenade

### Identity

| Item | Value |
|---|---|
| Classes | `content/items/ItemGrenade`, `content/entity/EntityGrenade`, `content/items/behavior/GrenadeDispenseBehavior`; registry `lib/explosive/reg/ExGrenadeContentReg` |
| Item | `icbmclassic:grenade`, metadata = explosive ID, max stack 16, ICBM tab. Lists one stack per grenade-capable explosive |
| Which explosives | tier ONE only: conventional, shrapnel, incendiary, debilitation, chemical, anvil, repulsive, attractive, plus colors and smoke (`ExplosiveInit.java:306-308`) |
| Entity | `icbmclassic:item.grenade`, 0.25 x 0.25, tracking range 50, update every 5 ticks; explosive stack sent as NBT in the spawn packet |
| Names | `grenade.icbmclassic:<key>.name`: condensed "Conventional Grenade", shrapnel "Shrapnel Grenade", incendiary "Incendiary Grenade", debilitation "Debilitation Grenade", chemical "Chemical Grenade", anvil "Anvil Grenade", repulsive "Repulsive Grenade", attractive "Attractive Grenade", colors "Colorful Grenade", smoke "Smoke Marking Grenade" |
| Recipes | 10 shaped, `recipes/grenades/<type>.json`: `string` oredict directly above the explosive (type `icbmclassic:explosive`), result 1 grenade of that type |

### Numbers

| Name | Value |
|---|---|
| Use duration (max charge) | 60 ticks (`ItemGrenade.MAX_USE_DURATION`, a TODO to make it config) |
| Throw energy | `(60 - timeLeft) / 60`, i.e. held ticks / 60; **not clamped** |
| Throw speed | `1.8 * energy` blocks/tick (the 1.8 is a TODO), aim error gaussian * 0.0075 |
| Spawn offset | at the thrower's eyes, 0.16 sideways, 0.1 down |
| Drag / gravity | x0.98 per tick (x0.8 in water) / -0.03 per tick; on the ground all three axes x0.5 |
| Fuse | explodes when `ticksExisted > fuse`; fuse from `FUSE_TIMES.GRENADES` (`icbmclassic/blast`): 100 for conventional, shrapnel, incendiary, debilitation, chemical, anvil; 120 for repulsive and attractive; colors and smoke have no key and use the default 100 |
| Blast point | the grenade's position + 0.3 up |
| Throw sound | vanilla `ENTITY_TNT_PRIMED`, blocks category, volume 0.5, pitch `0.4 / (rand * 0.4 + 0.8)` |
| Dispenser | speed 0.5, aim error as above; direction = the dispenser's horizontal facing with a fixed 0.1 upward component |

### Behavior

1. Right-click starts a charge (bow pose, no minimum). Releasing (server) plays the sound, builds the entity at the thrower with the held stack's explosive, sets its velocity from the charge and spawns it; survival players lose one item.
2. The grenade moves with vanilla entity movement (`move`), so it stops against walls (vanilla zeroes the blocked axis), falls under gravity and slows on the ground. It does not bounce and never rolls far. It can be pushed and collided with.
3. Each tick the fuse is checked. Before it ends, the grenade calls the grenade registry's fuse callback (none registered) and spawns a smoke particle behind it. When the fuse ends: run the explosive at the blast point with the grenade as the cause, then remove the entity. There is no impact trigger; it only goes off by time.
4. Charging for the full 60 ticks cancels the throw without any message (ICBM-03-61). Releasing at once throws at speed 0 (a drop at the thrower's feet).
5. Dispenser: spawns the grenade at the dispenser's block center (no offset in front), speed 0.5. Returns the stack minus one. Dispensers facing up or down send the grenade almost straight up (only the 0.1 upward component survives) (ICBM-03-63).

NBT: entity `explosive` (compound, the stack NBT). The thrower, the tick counter and the charge are not saved, so a grenade that survives a reload restarts its fuse (ICBM-03-60). The defuser drops the explosive stack and removes the grenade (`onDefuse`, [05](../05-detection-defense/handheld-tools.md)).

Assets: item models `models/item/grenades/<type>.json` (parent `item/generated`) with `textures/items/grenade/<type>.png`, all 16 x 16 (anvil, attractive, chemical, color, condensed, debilitation, incendiary, repulsive, shrapnel, smoke; the colors type uses `color.png`). Entity: the held item drawn as a billboard (`RenderAsItem`). Sounds: none beyond the throw sound.

### Bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-03-60 | Grenade forgets its fuse and thrower on reload (logic) | `content/entity/EntityGrenade.java:282,324-336` | The fuse uses vanilla `ticksExisted`, which is not saved. The thrower is never saved either, and the blast cause is always the grenade, so kill credit and protected-area checks never see the player | gameplay |
| ICBM-03-61 | Over-charging loses the throw; a tap throws at zero speed (logic) | `content/items/ItemGrenade.java:68-90` | Only `onPlayerStoppedUsing` throws. After 60 ticks the vanilla use flow ends the charge without calling it, so the grenade is neither thrown nor consumed. The energy is not clamped and a tap gives energy 0 (speed 0). The parachute and balloon items share this flow ([cargo.md](cargo.md)). Based on the vanilla item-use flow, not in this source | gameplay |
| ICBM-03-62 | Missing fuse config for colors and smoke grenades (logic) | `config/blast/ConfigFuseTimes.java` (the `Grenades` class, last in the file) | `GRENADES` has only eight keys, so the colors and smoke grenades cannot be tuned and silently use 100 | cosmetic |
| ICBM-03-63 | Dispenser grenade has no forward offset, vertical dispensers lob it upward (logic) | `content/items/behavior/GrenadeDispenseBehavior.java:35-37` | The entity spawns at the dispenser's center and the direction ignores the vertical facing. Whether the grenade starts inside the dispenser block without sticking was not verified | gameplay |

---

## B. Rocket Launcher

### Identity

| Item | Value |
|---|---|
| Class | `content/items/ItemRocketLauncher` (one class, two items), electrical item base `prefab/item/ItemICBMElectrical` |
| Items | `icbmclassic:rocketLauncher` ("Rocket Launcher: Direct") and `icbmclassic:ballisticLauncher` ("Rocket Launcher: Ballistic"); the constructor flag `fireUpDown` picks the mode |
| Recipes | direct (`recipes/tools/rpg.json`, shaped): 3 `ingotSteel` / radar gun + `circuitBasic` + `wireCopper` / 2 `plankWood` (pattern `sss` / `rcg` / `ww`: radar gun, circuit and copper wire in the middle row, the two planks under the radar gun and circuit). Ballistic (`rpgBallistic.json`, shapeless): a direct launcher + a launcher screen |
| Tooltips | direct: "Fires missiles in a direct line"; ballistic: "Fires missiles in an arc towards target. Has a 3m pre-kick and 20m y+ offset."; `.info.creative` = "Creative: Bypasses tier restriction" (no tier restriction exists, the key is never shown) |
| Chat (action bar) | `item.icbmclassic:rocketLauncher.error.IMissileAiming` = "Missile %1 doesn't implement IMissileAiming to allow for use in handheld launchers"; `.error.spawning` = "Missile spawning was canceled by world"; `.error.targeting` = "Fire Safety: No Target"; `.error.distance.min` = "Fire Safety: Min Range %1$s meters" |
| Assets | models `models/item/rocketlauncher.json` and `ballisticlauncher.json` (box models by "QueenOfMissiles"), both on `textures/items/rpg.png` (32 x 32); a `pulling` item property switches the pose while charging |

### Numbers

| Name | Value |
|---|---|
| Energy per shot / required | 1,000,000 (but see ICBM-03-65: energy is a stub) |
| Click delay | 1,000 ms (client-only, ICBM-03-64) |
| Ballistic minimum range | 20 blocks (distance from the player to the aimed point) |
| Cluster target lift (ballistic only) | +20 blocks added to the target Y |
| Aim trace | 500 blocks, blocks only, liquids ignored |
| Direct mode | missile at `speed_direct_fire` (config, default 2) with a "dead" flight logic of `handheld_fuel` ticks (default 200), then it falls |
| Ballistic mode | a 3-block move along the look vector at 0.2 acceleration, then the normal arc flight |
| Use duration | `Integer.MAX_VALUE` (hold as long as wanted; there is no charge, only the release matters) |

### Behavior

1. Right-click (`onItemRightClick`): refuse if this player fired less than 1,000 ms ago; otherwise start using.
2. Release (`onPlayerStoppedUsing`, player only): needs the launcher to have 1,000,000 energy or the player to be in creative. Scan the **entire inventory** (hotbar, main, armor, off-hand) in slot order for the first stack with the missile capability. Only that one is tried.
3. Server: build the missile from the stack; it must be an aimable missile, else the "IMissileAiming" message and stop. Set its position and heading from the player (eye position, `speed_direct_fire` speed), ray-trace 500 blocks along the look vector.
   - **Direct:** the flight logic is a timed "dead" logic. A hit within 500 blocks becomes the missile's target data (used by air-burst fuses); no hit means no target.
   - **Ballistic:** hit within 20 blocks: "Min Range" message and stop. No hit: "No Target" message and stop. Otherwise the target is the hit point and the flight is the 3-block kick then the arc. If the missile is a cluster missile, its target is raised 20 blocks.
   - The source is the player (an entity cause). Launch, then spawn.
4. After a successful spawn: a sneaking player **rides the missile** (and is un-sneaked); otherwise, with a lead in the off-hand, the first leashed mob within 7 blocks rides it instead. A spawn refused by the world shows the "spawning canceled" message.
5. Non-creative: replace the slot's stack with the consumed missile stack and spend the energy (a no-op).

### Bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| ICBM-03-64 | Click delay exists only on the client (logic) | `content/items/ItemRocketLauncher.java:221,235-240` | The timestamp is stored after the server-only block, which `return`s first, so only clients record it. A dedicated server never enforces the delay, and the map is keyed by player name and never cleaned | gameplay |
| ICBM-03-65 | Energy requirement is a stub (logic) | `content/items/ItemRocketLauncher.java:97,203`, `prefab/item/ItemICBMElectrical.java:12-18` | `getEnergy` returns the integer maximum and `discharge` is empty, so the 1,000,000 energy cost is never charged. Same root cause as ICBM-05-09 | gameplay |
| ICBM-03-66 | Launcher fires the first missile anywhere in the inventory (logic) | `content/items/ItemRocketLauncher.java:100-103,213-218` | No ammo choice (a TODO). The scan includes armor slots and the off-hand and takes the first capability match, even a cluster, SAM or held-item missile. If that one cannot be aimed, the launcher gives up instead of trying the next | gameplay |
| ICBM-03-67 | Wrong or unused launcher strings (logic) | lang `item.icbmclassic:rocketLauncher.error.IMissileAiming`, `.info.creative`, `ballisticLauncher.info` | `%1` has no argument index, so the stack name is never substituted; the creative line mentions a tier restriction that does not exist; the ballistic tooltip says "20m y+ offset" but the lift applies to cluster missiles only | cosmetic |

Minor, not given IDs: the ballistic minimum-range check measures from the player's feet, not the eyes; the `onUpdate` override is empty; a TODO says the launcher should use the missile's own source/cause data rather than building one.
