# AI, factions and targeting

Source files: [`entities/npc/GenericNPC.java`](per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava), `entities/npc/NPCTurret.java`, [`entities/npc/AttackHelicopter.java`](per-mob.md#16-attack-helicopter-attackhelicopter-boss), [`entities/npc/AlienBug.java`](per-mob.md#15-alien-bug-alienbug), `entities/npc/TGNpcFaction(s).java`, `entities/ai/*` (all 5), and `items/guns/GenericGun.getAIAttack/setAIStats` (the AI parameters are data owned by [02-guns](../02-guns/index.md)).

## 1. Factions (`TGNpcFaction`, `TGNpcFactions`)

The enum `TGNpcFaction` has three values: `HOSTILE`, `TURRET`, `NEUTRAL`. [`ITGNpcTeam.getTGFaction()`](per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) exposes an entity's faction.

The hostility table is symmetric:

| | HOSTILE | TURRET | NEUTRAL |
|---|---|---|---|
| HOSTILE | false | **true** | **true** |
| TURRET | true | false | false |
| NEUTRAL | true | false | false |

Members of the same faction never fight each other.

**Player-to-player relations.** `TGNpcFactions.isHostile(UUID a, UUID b)` returns `!friendsAPI.areFriends(a, b)`. `friendsAPI` is set in [`Techguns.postInit`](../01-core/lifecycle-and-registration.md#lifecycle-overview):
- if the mod `FTBU` (FTB Utilities) is loaded, it is the real FTB `FriendsAPI`;
- otherwise it is a dummy that **always** says "friends". Without FTBU, owned turrets never treat any player or another player's turret as hostile (bug [NPC-13](index.md#suspected-bugs)).

Faction membership:
- GenericNPC and its subclasses, and AlienBug: HOSTILE.
- NPCTurret: TURRET.
- AlienBugDomesticated (unused): NEUTRAL.
- AttackHelicopter: no faction. It is still an `IMob`, so turrets target it.

Projectiles use the same table to decide whether a hit NPC retaliates. When a [`GenericProjectile`](../04-projectiles-fx/projectiles.md#2-genericprojectile-the-base-for-almost-everything) from shooter S hits entity E:
- if both are `ITGNpcTeam`, E sets S as its revenge target only if their factions are hostile;
- otherwise E retaliates against any non-player shooter;
- player shooters are left to vanilla revenge handling.

This lives in `GenericProjectile.setAIRevengeTarget` ([04-projectiles-fx](../04-projectiles-fx/index.md)). The practical effect is that hostile NPCs caught in each other's crossfire do not start infighting.

## 2. GenericNPC task list

New-style AI (`isAIEnabled() = true`). The tasks are set up in the constructor, plus `setCombatTask()`.

| Priority | Task | Parameters / notes |
|---|---|---|
| 1 | `EntityAISwimming` | |
| 4 | **combat task**: either `EntityAIRangedAttack` (from the held gun) or `EntityAIAttackOnCollide` | Melee: target class **EntityPlayer**, speed ×1.2, no persistent memory. Because of the class filter, a melee NPC will not hit non-player targets such as a turret that attacked it (bug [NPC-24](index.md#suspected-bugs)). |
| 5 | `EntityAIWander` | speed ×1.0 |
| 6 | `EntityAIWatchClosest` | EntityPlayer, 8 blocks |
| 6 | `EntityAILookIdle` | |

| Priority | Target task | Parameters / notes |
|---|---|---|
| 1 | `EntityAIHurtByTargetTGFactions` | `callsForHelp = false` (see §4) |
| 2 | `EntityAINearestAttackableTarget` | EntityPlayer, target chance 0 (checks every tick), **needs sight**, search radius = the `followRange` attribute (40 to 75 blocks depending on mob) |

NPCs only seek players. They attack other things (turrets, players' pets) only in retaliation.

## 3. `EntityAIRangedAttack`, the gun-driven shooting AI

A burst-capable version of vanilla `EntityAIArrowAttack`. Mutex bits 3 (move and look).

Construction comes from [`GenericGun.getAIAttack(shooter)`](../02-guns/gun-lifecycle.md):

| Field | Value |
|---|---|
| move speed | 1.0 |
| `attackTimeVariance` | `AI_attackTime / 3` (integer division) |
| `maxRangedAttackTime` | `AI_attackTime` |
| `attackRange` | `AI_attackRange` (blocks; the code squares it for distance checks) |
| `maxBurstCount` | `AI_burstCount` |
| `shotDelay` | `AI_burstAttackTime` |

Gun defaults are range 15, attack time 60 and no burst. Each gun overrides them with `setAIStats(range, attackTime, burstCount, burstDelay)`. The range constants in [`TGuns`](../02-guns/gun-catalog.md) are MELEE 3, CLOSE 12, SHORT 18, MEDIUM 24 and FAR 36.

These are the guns NPCs actually carry, with their AI stats (from `TGuns.java`):

| Gun | Range | Attack time | Burst count | Burst delay |
|---|---|---|---|---|
| handcannon | 12 | 60 | 0 | 0 |
| pistol | 24 | 30 | 3 | 10 |
| sawed-off | 12 | 60 | 2 | 20 |
| bolt-action | 36 | 60 | 0 | 0 |
| M4 / M4 UQ / AK-47 | 24 | 30 | 3 | 3 |
| flamethrower | 12 | 20 | 5 | 5 |
| combat shotgun | 12 | 30 | 0 | 0 |
| laser gun | 24 | 30 | 0 | 0 |
| rocket launcher | 24 | 80 | 0 | 0 |
| Grim Reaper | 24 | 120 | 4 | 30 |
| bio gun | 18 | 30 | 0 | 0 |
| thompson | 18 | 45 | 3 | 3 |
| revolver | 18 | 90 | 6 | 20 |
| AS50 | 36 | 30 | 0 | 0 |
| tesla gun | 24 | 30 | 0 | 0 |
| LMG | 24 | 40 | 6 | 3 |
| golden revolver | 24 | 60 | 6 | 15 |
| Cyberdemon Blaster | 24 | 40 | 0 | 0 |
| minigun | 24 | 40 | 10 | 1 |
| blaster rifle | 24 | 30 | 5 | 3 |
| chainsaw | 3 | 10 | 0 | 0 |

**shouldExecute:** true while the host has an attack target. The task caches that target.

**continueExecuting:** `shouldExecute()`, or the navigator still has a path. Because of this, the task can keep running on a stale cached target until the current path ends (bug [NPC-31](index.md#suspected-bugs)).

**resetTask:** clears the cached target, resets seen-ticks to 0 and sets the timer to −1.

**updateTask, each tick:**
1. Let `d²` be the squared distance from the host to the target's feet, and `seen` whether the host can see the target.
2. If seen, increment `ticksTargetSeen`; otherwise reset it to 0.
3. If `d² ≤ range²` and the target has been seen for at least 20 consecutive ticks, stop moving (clear the path). Otherwise path toward the target at speed ×1.0.
4. Look at the target (max 30° yaw and 30° pitch change per tick).
5. Decrement the attack timer.
   - **If it reaches exactly 0:**
     - If the target is out of range or not seen, do nothing this tick. The timer drops below 0 next tick and is recomputed.
     - Otherwise let `f = distance / range`, clamped to [0.1, 1], and call `attackEntityWithRangedAttack(target, f)`.
     - Burst handling: if `maxBurstCount > 0`, decrement `burstCount`. If shots remain, set `timer = shotDelay`. Otherwise refill `burstCount` and set `timer = floor(f_raw · (maxTime − variance) + variance)`, where `f_raw` is the **unclamped** distance/range.
   - **If it is below 0** (fresh start): `timer = floor(f_raw · (maxTime − variance) + variance)`.

The pause between bursts therefore scales linearly with distance: about `variance` ticks point-blank and `maxTime` ticks at the edge of range. A partially fired burst carries over if the target breaks line of sight.

The host's `attackEntityWithRangedAttack` then fires the gun ([per-mob.md §0](per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) for NPCs, §17 for turrets).

**Arm pose.** While a GenericNPC holds a gun and `getHasAimedBowAnim()` is true, the client render-event handler and [`RenderGenericNPC`](rendering-and-models.md#2-rendergenericnpc-extends-vanilla-renderbiped) set `aimedBow` on the biped and armor models. This gives the two-arms-forward pose.

## 4. `EntityAIHurtByTargetTGFactions`

Extends vanilla `EntityAIHurtByTarget` and overrides `isSuitableTarget(target)`:
1. If both the owner and the attacker are `ITGNpcTeam`, the attacker is suitable only if the two factions are hostile. Nothing else is checked, so the vanilla checks are skipped in this case.
2. Else, if the owner is an `NPCTurret`, the attacker is a player, the turret is linked, and its base has an owner UUID: the attacker is suitable only if `isHostile(ownerUUID, attackerUUID)` is true, meaning not FTB friends.
3. Otherwise the vanilla suitability check applies (alive, not creative, same team, and so on).

## 5. Turret (`NPCTurret`)

### Task list (`setAITasks`)
| Priority | Task |
|---|---|
| 1 | `EntityAIRangedAttack` from the base's gun (added by `setCombatTask`, if a GenericGun is fitted) |
| 2 | `TurretEntityAIWatchClosest` (watch class `EntityLiving`, 80 blocks horizontal, ±20 vertical) |
| 3 | `EntityAILookIdle` |

| Priority | Target task |
|---|---|
| 1 | `EntityAIHurtByTargetTGFactions` (no help call) |
| 2 | `TurretEntityAINearestAttackableTarget` (class `EntityLivingBase`, chance 0, needs sight, not nearby-only, selector `TurretTargetSelector`, vertical reach ±20) |

`setCombatTask` also sets the `followRange` attribute to the gun's AI range. This makes the horizontal target-search radius equal the gun's range: 3 to 36 blocks.

### `TurretEntityAINearestAttackableTarget`
A copy of vanilla `EntityAINearestAttackableTarget`, rewritten because vanilla keeps its fields private. The search box is the turret's bounding box expanded by `followRange` horizontally and **±YOffset = 20** vertically (vanilla uses ±4).
- Candidates must pass `TurretTargetSelector` and the vanilla `EntityAITarget.isSuitableTarget` check (sight check on).
- The nearest candidate becomes the attack target.
- Mutex bit 1.

### `TurretTargetSelector`, the target filter
Checked in order:
1. Any `IMob` → **target**. This covers all TG hostile NPCs, the alien bug, the helicopter and vanilla monsters.
2. If the turret is linked to a base:
   - if base `security ≠ 0` and the base has an owner UUID:
     - another `NPCTurret` with a linked base → target if `isHostile(owner, otherOwner)`;
     - an `EntityPlayer` → never the owner; otherwise target if `isHostile(owner, player)`.
   - if the base's `attackAnimals` toggle is on and the entity is an `IAnimals` but not an `NPCTurret`:
     - a tamed `EntityTameable`, a tame `EntityHorse`, or an `IEntityOwnable` with an owner → skip;
     - every other `IAnimals` → target. This includes farm animals, squid, bats and **iron/snow golems**.
3. Anything else → not a target. Unlinked turrets only shoot `IMob`.

`security` is the byte from [`BasicOwnedTileEnt`](../08-machines-production/framework.md#3-ownership-and-security-basicownedtileent-itgtileentsecurity); its values and GUI are covered by 09. Here only zero versus non-zero matters. Without FTBU, `isHostile` on UUIDs is always false, so security has no effect (NPC-13).

### `TurretEntityAIWatchClosest`
A copy of vanilla `EntityAIWatchClosest` with a custom vertical range:
- Each tick there is a 2% chance (`0.02`) to start.
- It picks the nearest `EntityLiving` within 80 horizontally and ±20 vertically. It first assigns the current attack target, then overwrites it with the nearest-entity search. That is a vanilla copy bug ([NPC-26](index.md#suspected-bugs)).
- `EntityPlayer` is not an `EntityLiving`, so the turret never idly watches players.
- It looks for 40 to 79 ticks (yaw speed 10, vertical face speed per entity) while the target stays alive and in range.
- Mutex bit 2.

### Linking and sync
- **Placement:** the base spawns the turret one block above itself at the block center ([09](../09-machines-utility/index.md)).
- **Server relink:** while `mountedTileEnt` is null, every tick the turret looks for a [`TurretBaseEnt`](../09-machines-utility/turret.md#tile-entity-turretbaseent) at `(floor(x), round(y) − 1, floor(z))`. If it finds one, it links both ways, marks the block for update and re-runs `setCombatTask`.
- **Client:**
  - while unlinked and within 64 blocks horizontally of the client player (`clientInRangeSquared(x, z, 4096)`), it sends `PacketRequestTurretSync{int turret_entityId}` to the server, at most once every 600 ticks;
  - the server replies with the base tile's description packet;
  - the base tile also links itself to its turret client-side ([09](../09-machines-utility/index.md)).
- **Gun change:** the base watches the turret's held stack, and on a change it re-runs `setCombatTask` ([09](../09-machines-utility/index.md)).

## 6. Attack Helicopter

Legacy `updateEntityActionState` (Ghast-derived). Each tick:

1. **Peaceful check:** on Peaceful (server) the helicopter removes itself. Then `despawnEntity()` runs.
2. **Waypoint:** let `Δ` be the vector to the current waypoint. If `|Δ|² < 1` or `|Δ|² > 3600` (60 blocks), choose a new waypoint:
   - X and Z: the current position ± up to 16 (uniform);
   - Y: with 75% chance, the terrain height of the current column (`getHeightValue`) **+ 25** (the "preferred height"); otherwise the current Y ± up to 16.
3. **Course correction:** when the cooldown counter runs out, add 2 to 6 ticks to it. Then sweep the bounding box along the line to the waypoint in 1-block steps.
   - If the path is clear, add `0.1 · Δ/|Δ|` to the motion.
   - If blocked, set the waypoint to the current position, which forces a new pick next tick.
4. **Target:** drop the target if it is dead. If there is no target, or `aggroCooldown` has run out, take the closest vulnerable player within **100** blocks. If one is found, set `aggroCooldown = 20`.
5. **Engaging** (target within **64** blocks):
   - Face the target. Body yaw is the horizontal angle to the target's center.
   - **If the target is visible**, increment `attackCounter`:
     - **counter = 16:** play `techguns:npcs.apacheBurst` (volume 5).
     - **counter 16 to 29, odd values only** (7 shots): fire a machine-gun bullet.
       - It is a `GenericProjectile` with yaw = heli yaw, damage 12, speed 1.0, lifetime 100, spread 0.05, damage-drop start 30 / end 40, min damage 8, penetration 0.25, no block damage, and **no shooter**.
       - Pitch is the angle between the horizontal direction to the target and the direction from `(posY + 2.25)` to the target's feet. That angle comes from `acos`, so it is always ≥ 0 and the gun can only aim level or down (bug [NPC-10](index.md#suspected-bugs)).
       - Spawn point: 8 blocks along the horizontal look vector, at `posY + height/2 − 1.5`.
       - A `Fireblast` muzzle particle (±0.1 jitter) appears 4 blocks from the center along the body yaw, at `posY − 0.25`.
     - **counter = 30:** fire a rocket.
       - Play vanilla aux SFX 1008 (ghast fireball sound).
       - Spawn a [`RocketProjectile`](../04-projectiles-fx/projectiles.md#34-rocketprojectile-and-nukerocketprojectile) with damage 20, blast radius 2, speed 1.0, lifetime 100, spread 0.05, gravity 0, no block damage, penetration 0, and no shooter.
       - Pitch uses the same acos method from `posY`, multiplied by 1.18 as a correction.
       - Spawn point: 8 blocks along the look vector, at `posY + height/2 − 1`.
       - Play `techguns:guns.rocketFire` (volume 5), then set `attackCounter = −40`.
     - The full cycle is about 70 ticks: 7 bullets, then a rocket, then a 56-tick wind-up.
   - **If the target is not visible**, `attackCounter` decays by 1 per tick down to 0.
6. **Not engaging:** body yaw follows the motion vector, and `attackCounter` decays.
7. **Sync:** the server keeps DataWatcher byte 16 equal to 1 while `attackCounter > 10`.

The rotor loop sound plays independently every 61 ticks (rotor clip ≈ 3.07 s).

## 7. Alien Bug

`EntitySpider` legacy AI with no task list. `EntityCreature.updateEntityActionState` drives it: when it has no target it calls `findPlayerToAttack` every tick, then paths to the target and calls `attackEntity(target, dist)` when close. The overrides are described in [per-mob.md §15](per-mob.md#15-alien-bug-alienbug).
- Targeting: always hostile, 16-block range, line of sight required.
- Attack: bite at < 2 blocks with a 10-tick timer and no leap.
- Plus the spider's wall climbing.

## 8. Unused: `TurretEntityUNUSED`

Described in per-mob §19. It never runs.
