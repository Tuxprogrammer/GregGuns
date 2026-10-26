# 0020 · OpenModularTurrets overlap: the Rampart stays, nothing is merged

- Status: Accepted (orchestrator, by default; the owner can change it)
- Gate: M9 polish item "the OpenModularTurrets overlap investigation"
- Date: 2026-10-25

## Context
OpenModularTurrets (OMT 2.4.8, `openmodularturrets`) ships in the GTNH 2.9.0-RC-2 pack. [0002](0002-machine-deconfliction.md) kept the Turret and [0011](0011-machine-policy.md) gave it the Rampart multiblock. The [turret sheet](../gregguns-spec/09-machines-utility/turret.md) already compares the two and says OMT's target filter never touches our turret. The roadmap asked for a full look before the handoff: what OMT is, where it overlaps the Turret and the Rampart, whether anything breaks when both run, and whether ammo crosses over.

Evidence is OMT's source at tag 2.4.8 (the pack's tag) and the pack's `config/openmodularturrets.cfg`. Links below are pinned to that tag.

### What OMT provides
- **Turrets are two blocks.** A base (tier 1 to 5, with an inventory, a power buffer and an owner plus trusted-player list) carries one head block. Heads are fixed weapons: disposable item thrower, potato cannon, machine gun ("gun"), grenade launcher, incendiary, rocket, laser, railgun, plus a relativistic and a teleporter head that deal no damage. Expander blocks add power or inventory; addons (solar, redstone reactor, recycler, damage amp and others) and upgrades (accuracy, efficiency, range, rate of fire) tune a base.
- **Numbers come from config** (pack defaults). Damage is in half-hearts, power in RF per shot:

| Head | Damage | Range | Cooldown (ticks) | RF per shot | Ammo |
|---|---|---|---|---|---|
| Disposable | 2 | 10 | 25 | 2 | any item in the base |
| Potato cannon | 6 | 15 | 35 | 10 | OMT potato ammo |
| Machine gun | 5 | 18 | 8 | 100 | OMT bullet (`bulletCraftable`) |
| Grenade | 18 | 20 | 40 | 3000 | OMT grenade |
| Incendiary | 5 | 12 | 25 | 250 | OMT blazing clay |
| Rocket | 25 | 30 | 30 | 5000 | OMT rocket |
| Laser | 5 | 25 | 10 | 10000 | none (power only) |
| Railgun | 200 | 30 | 100 | 25000 | OMT ferro slug |

- **Power.** RF buffers by base tier (500 up to 10 million RF, 50 to 5000 RF/t in). With "Can turrets be powered with EU?" on (the pack default), a base takes GT EU at 8 RF per EU. [TurretBase.java#L496-L503](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/tileentity/turretbase/TurretBase.java#L496-L503) reads `aVoltage * aAmperage` and checks no voltage, so there is no overvoltage explosion. That is the whole GT integration: no GT materials, no recipe map, no tiers.
- **Targeting.** [TurretHeadUtil.getTarget](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/util/TurretHeadUtil.java#L98-L145) picks any `EntityAnimal` or `EntityAmbientCreature` (neutrals), any entity of `EnumCreatureType.monster` (that is, `IMob`) and any `EntityPlayerMP` that is not the owner, a trusted player or in creative. Each class of target has a per-base toggle plus a global config toggle (all `true` in the pack). Trust is OMT's own list, not teams. There is no IFF beyond that.
- **Recipes and progression.** `Which recipes should we do?` = `auto` picks EnderIO, Thermal Expansion or Mekanism recipes when loaded and otherwise [the vanilla set](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/handler/recipes/RecipeHandler.java#L20-L45). GTNH ships none of those three, so the vanilla set applies: tier 1 from cobblestone, planks and redstone; ammo from iron ingots, gunpowder and redstone (64 bullets per craft); upper tiers from gold, diamond, quartz, obsidian and OMT's own sensor, barrel and chamber items ([VanillaRecipeHandler](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/handler/recipes/VanillaRecipeHandler.java)). No GT item appears. OMT is therefore available from the first hours of the pack and does not follow GT tiers. A player finds it as a cheap, early, always-on defense.

### How ours differs
Already summarized in the turret sheet; the rest of the picture:

| | OMT | GregGuns Turret / Rampart |
|---|---|---|
| Weapon | Fixed head per type, stats from OMT config | The gun the player mounts, with the gun's own stats, tier gate and damage model ([0004](0004-damage-model.md), [0016](0016-damage-balance-pass.md)) |
| Ammo | OMT items (bullet, rocket, grenade...), matched by exact `Item`; only the disposable head takes any item | GregGuns ammo items, matched by the gun's `ammoType`; empties return to output slots |
| Tier / power | Base tier 1 to 5, RF buffer, EU at 8:1, no voltage rules | GT MTE, MV to IV, EU with overvoltage rules; the Rampart controller is HV, mount hatches MV to IV |
| Progression | Vanilla materials, early game | GT hulls, sensors and circuits; the Turret needs MV parts, the Rampart HV |
| Targets | Neutrals, `IMob`, non-trusted players | Hostile mobs and camp turrets, other players' turrets, non-friend players at security level above public, optionally passive animals (not tamed) |
| Friend or foe | Owner plus a trusted list per base | Owner plus ServerUtilities team ALLY or higher |
| Body | Tile entity, camouflage block | Tile plus a 30 HP `TurretEntity` that heals and respawns |
| Scale | One head per base, many bases | Rampart: 2 to 10 mounts on one ammo feed and one power feed, plating tier is armor |

### Runtime conflicts
Checked against our entities and OMT's code:

1. **OMT shooting our hostile NPCs.** `NpcEntity` extends `EntityMob`, so OMT turrets shoot raiders. This is fine and good: a base with OMT guns is defended against our mobs.
2. **OMT shooting our turrets.** `TurretEntity` is an `EntityCreature` that is none of `IMob`, `EntityAnimal` or `EntityAmbientCreature`, so OMT never selects it. Pinned by `TurretHierarchyTest` and `OmtCoexistenceTest`.
3. **OMT and the hostile camp turret.** `NpcTurret` is not an `IMob` on purpose (it keeps Infernal Mobs away), so OMT turrets never shoot it either, while our Turret does. A player who defends only with OMT cannot silence a camp turret from range. This is a small gap, accepted: our turret and any gun can.
4. **Our turrets shooting OMT.** OMT turrets are tile entities. The only entity OMT spawns is a package-private helper (`InvisibleTurretHelperEntity`, an `EntityLivingBase`, not `IMob` or `IAnimals`) used for line-of-sight tests, so our selector never picks it. An OMT base's owner and friends are shot by nobody extra: our Turret's friend rules look only at our own security setting.
5. **Stray OMT bullets.** `BulletProjectile` damages the first entity it meets (`attackEntityFrom` with a plain damage source named "bullet"), including a `TurretEntity` standing in the line of fire. The same goes for OMT shots at a player. It affects only players who place both side by side; OMT's own `canDamagePlayer` setting governs players. Not guarded in code: the damage source is OMT's own class, and filtering it would be a mixin-weight change for a rare case.
6. **Shared ore dictionary or ammo.** None. OMT matches ammo by exact `Item` (`useSpecificItemStackItemFromBase`), registers nothing in the ore dictionary for ammo, and our ammo matching is by registry name. Neither mod's ammo works in the other's turret, except one case: the **disposable head accepts any item** ([useAnyItemStackFromBase](https://github.com/GTNewHorizons/OpenModularTurrets/blob/2.4.8/src/main/java/openmodularturrets/util/TurretHeadUtil.java#L595-L604)), so a GregGuns round put in that base is spent as a 2 damage pellet. That is the player's choice and wasteful, not a conflict.
7. **Power.** Both accept EU. OMT's base accepts any voltage for free, ours obeys GT overvoltage. Cables serving both behave per machine. No interaction.

## Decision
1. **The Rampart and the Turret stay.** They are not duplicates. OMT is a cheap, early, fixed-weapon defense that ignores GT progression. Ours is the endpoint of the GregGuns loop: your own guns, your own ammo and GT-tier machines. Removing ours would strand the gun, ammo and armor-plate chain that feeds it; removing OMT is outside our remit (it is a pack mod).
2. **Differentiate (already done) and integrate nothing.** We add no OMT dependency, no compat code, no shared inventory and no cross-ammo. Reasons: OMT's stats are global config rather than gun stats, so any bridge would need new balance rules; OMT's ownership model (its own trust list) does not map to ServerUtilities teams; and a hard dependency on a third-party mod's internals breaks the self-contained-jar goal of [0015](0015-m9-self-contained-polish.md).
3. **Do nothing in the runtime code.** The investigation found no conflict that needs a guard. The class relationships that matter (our turrets are not targetable by OMT, our raiders are) are already as intended. The in-jar change is a test: `OmtCoexistenceTest` pins what OMT's class-based filter selects, including the camp-turret gap, so a hierarchy refactor cannot change it silently.
4. **No pack change is implied.** The pack's OMT config needs no edit for us. The adopter may, at their discretion, lower OMT's `Can turrets be powered with EU?` or its damage values if they want ours to be the stronger late-game option; that is a balance choice recorded below, not a requirement.

## Consequences
- The turret sheet's OMT section gains a pointer to this record; the roadmap polish item is done by it.
- One new unit test class, no behavior change, no config option, no `PACK-CHANGES.md` row.
- Players who run both see no cross-targeting of turrets, shared ammo or power quirks. The two known soft spots (camp turrets are invisible to OMT; stray OMT bullets can hit our turret entity) are documented.

### Follow-ups (not done; for the adopter or a later session)
- **Stray-bullet guard.** If playtests show OMT shots killing our turret entities, cancel `LivingAttackEvent` for damage sources whose type is `bullet` and which carry no entity when the target is a `TurretEntity`. Needs a live check with both mods before shipping, because it is a global rule on a name any mod could reuse.
- **Camp turrets and OMT.** If the owner wants OMT to defend against camp turrets, the only route is a pack-side tweak or an event that retargets from the OMT side; making `NpcTurret` an `IMob` would expose it to Infernal Mobs and is rejected.
- **Late-game balance.** An OMT railgun (200 half-hearts, 30 blocks, no ammo beyond ferro slugs) outranges every MV to IV GregGuns gun. Whether the pack should raise OMT's costs or we should tune ours belongs to the balance pass ([0016](0016-damage-balance-pass.md)) and the handoff list.
- **GTNH quest or recipe hooks.** A pack-side GT recipe for OMT parts would move OMT onto GT tiers and shrink the overlap; it is the adopter's call.

## Alternatives considered
- **Retire our Turret and Rampart in favor of OMT.** Rejected: loses real-gun turrets, the use of GregGuns ammo and the GT-tier defense identity, and gives OMT's unscaled stats the whole role.
- **Let OMT heads fire GregGuns ammo** (or our turret accept OMT bullets). Rejected: needs a damage mapping between OMT config numbers and gun stats, a compile or reflection dependency on OMT, and a balance pass for each head.
- **Bridge ownership, friend lists or targeting** (OMT trusted list from ServerUtilities teams, or our friends from OMT's list). Rejected for now: nothing is broken without it, and it is a feature for the adopter if players ask.
- **A config toggle to make our turrets shoot or ignore OMT bases.** Rejected: OMT bases are not entities, so there is nothing to target.
- **Make `NpcTurret` an `IMob` so OMT shoots it.** Rejected: it breaks the Infernal Mobs exclusion that [0007](0007-pack-loot-and-infernal-mobs.md) and GGS-09-01 rely on.
