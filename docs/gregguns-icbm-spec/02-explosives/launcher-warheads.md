# Launcher warheads

- **ICBM baseline:** ICBM-Classic 1.12 has a handheld rocket launcher and grenades; D10 dropped them. This sheet is the GregGuns side of [D12](../DECISIONS.md): ICBM blast types become warheads the existing GregGuns launchers can carry.
- **Decisions:** [D12](../DECISIONS.md), [D69](../DECISIONS.md), [D150, D151, D152, D154](../DECISIONS.md)
- **Pack mods:** GregTech (`gregtech`, 5.09.54.205): Assembler recipe map
- **Roadmap:** [I5](../../ROADMAP-ICBM.md#i5--endgame-warheads-and-systems)
- **Code:** `com.gregguns.icbm.launcher`; hooks in `RocketProjectile` and `GrenadeLauncherProjectile`; one tooltip line in `ItemGun`

## What changes from ICBM

| ICBM behavior | GregGuns behavior | Why |
|---|---|---|
| A handheld launcher with its own rockets | No new launcher. The Rocket Launcher, Grim Reaper, Grenade Launcher and Nuke Launcher carry ICBM blasts | D12: one shared registry, one set of launchers |
| One rocket per explosive type | A **launcher warhead** item: four charges of a blast type, crafted onto a launcher | The gun ammo system is one ammo item per gun (`ammoType`), so a type cannot ride on the rocket item |

## The design

A launcher warhead is a loose item (`launcher_warhead_<key>`) worth **4 charges**. Put it in a crafting grid with a launcher of its family and you get that launcher back with the warhead loaded (NBT `gg_launcher_warhead = {key, charges}`; every other tag, such as loaded ammo and camo, is kept).

- The same type adds charges, to a **cap of 16**. Nothing is ever lost on a load (D154): the recipe does not exist for a different type while charges are loaded, nor for a load that would pass 16. Fire the charges off first.
- Each shot **still spends the launcher's own rocket or grenade**, and spends **one charge**. The projectile carries the key. Where it would explode, it starts the ICBM blast at **fill 1** (`IcbmBlasts.detonate`) instead of its own explosion. A rocket's direct-hit damage is unchanged.
- Fill 1 keeps every blast small (the curve of [warheads-and-fill](warheads-and-fill.md)). The launcher is a tactical tool, not a missile.
- The ICBM module must be active (Galacticraft present). With it off, the item exists but a loaded launcher fires normally and spends nothing, and a projectile saved with a charge explodes as a plain shot.
- `icbm.maxBlastTier`, claims and protection apply as for any blast; a refused blast falls back to the normal explosion. A launcher is a hand-held weapon (D52), so terrain breaks only when both the per-tier `icbm.terrainDamageT<n>` switch and `explosions.blockDamage` allow it.

### Damage and radius (D154)

The warhead changes what a shot **does**, not how hard it hits. The blast's own center damage at fill 1 (537 for condensed, 671 for fragmentation) is far above any launcher's hit, so it is capped at **1.25 times the launcher's own projectile damage** (`LauncherRules.DAMAGE_CAP_FACTOR`; never above 1.5). The numbers come from the gun stats in code and are pinned by `LauncherDamageCapTest`:

| Family | Launcher damage | Cap (x 1.25) | Warheads |
|---|---|---|---|
| Rocket Launcher, Grim Reaper | 60 | 75 | HE, Heavy Slug, Phosphorus (blasts with damage); Incendiary and Demolition deal none |
| Grenade Launcher | 36 | 45 | Grenade HE, Cluster |
| Nuke Launcher | 400 | 500 | EMP (no damage) |

| Warhead | Blast | Radius (fill 1) | Center damage |
|---|---|---|---|
| HE | condensed | 5.3 | 75 |
| Heavy Slug | anvil | 4.7 | 75 |
| Incendiary | incendiary | 7.0 | 0 |
| Phosphorus | shrapnel | 5.8 | 75 |
| Demolition | breaching | 4.4 | 0 |
| EMP | emp | 12 | 0 |
| Grenade HE | condensed | 5.3 | 45 |
| Grenade Cluster | fragmentation | 5.2 | 45 |

The radius is the unchanged fill-1 radius. The Nuke Launcher's own nuke (400 on a direct shot) stays the big hit.
- An NPC or turret that holds a loaded launcher uses it the same way.

## What is offered

| Item | Blast | Launchers | Assembler tier |
|---|---|---|---|
| High Explosive (`he`) | `condensed` | Rocket Launcher, Grim Reaper | HV |
| Heavy Slug (`heat`) | `anvil` | Rocket Launcher, Grim Reaper | HV |
| Incendiary (`incendiary`) | `incendiary` | Rocket Launcher, Grim Reaper | HV |
| Phosphorus (`phosphorus`) | `shrapnel` | Rocket Launcher, Grim Reaper | HV |
| Demolition (`demolition`) | `breaching` | Rocket Launcher, Grim Reaper | EV |
| EMP (`emp`) | `emp` | Nuke Launcher | LuV |
| Grenade HE (`grenade_he`) | `condensed` | Grenade Launcher | HV |
| Grenade Cluster (`grenade_cluster`) | `fragmentation` | Grenade Launcher | EV |

**Left out on purpose.** Gas, push and smoke blasts: a fill-1 cloud lasts as long as a full one and a launcher can spam it, which is the abuse the cloud caps exist to stop. Contagious, sonic and thermobaric: T2 specials that need a casing's fill to mean something. Nuclear: the Nuke Launcher already fires the tactical nuke (D83). The T3 exothermic and endothermic: casing-sized. Everything from the Grenade Launcher and Rocket Launcher is T1 or T2; the Nuke Launcher takes only EMP, which does no block or entity damage, so it cannot outdo its own nuke.

## Recipes (D69, D63)

One Assembler recipe per item at its tier (HV 480 EU/t, EV 1,920, LuV 30,720), 200 ticks, with a programmed circuit (6 to 13: all of 1 to 24 are used by warheads and parts, so they were picked so that no launcher recipe and no other ICBM recipe with the same circuit contain one another, because GT crafts any recipe whose inputs are all present; `LauncherWarheadClashTest` checks every pair, and the clash dump was run for the whole recipe map), out of the parts the warheads use (plastic explosive, fuze assemblies, sleeves, canisters, liner cones) plus the casing metal. Cost reading: a stack of four charges costs about the plate-equivalents of a rocket press run plus a fuze, so each charge is dearer than a plain rocket but far below a T1 warhead. `LauncherWarheadSpecTest` holds the band (8 to 60 plate equivalents), the HV floor and the per-tier input limits. No recycling data: the items are consumed.

## Tooltip (D41)

Short: tier line, one summary line, "Press [SHIFT]". Shift: the launchers it fits (by their display names), the blast's radius and the most center damage at fill 1 (capped, D154), charges per item and the cap, how to load it, "Each shot spends a charge and uses this blast", "Added by". A loaded launcher shows `Warhead: <name> (<n> charges)` under its ammo line. `LauncherTooltipFormat` has unit tests.

## Requirements

- **GGI-02-60.** A warhead loads only on a launcher of its family; the loading recipe never changes a launcher's other tags.
- **GGI-02-61.** One shot spends exactly one charge, and a launcher with no charge fires its own blast.
- **GGI-02-62.** The ICBM blast replaces, never adds to, the projectile's explosion.
- **GGI-02-63.** With `modules.icbm` off, nothing is spent and no ICBM blast starts; a saved charge explodes as a plain shot.
- **GGI-02-64.** A launcher blast never deals more center damage than 1.25 times its launcher's own damage, keeps the fill-1 radius, and breaks terrain only with `explosions.blockDamage` on.
- **GGI-02-65.** No launcher warhead recipe can be crafted from another ICBM recipe's inputs under the same circuit, or the other way round.

## Tests

`LauncherRulesTest`, `LauncherWarheadSpecTest`, `LauncherDamageCapTest`, `LauncherWarheadClashTest`, `TerrainRuleTest` (units); scenario `icbm.launcher.load_and_fire` (with `explosions.blockDamage` forced off: exactly one `condensed` blast starts, the rocket's own explosion does not run, the cap is 75 and terrain stays whole).

## Open questions

None. NEI shows the Assembler recipes; the crafting-grid load is a custom recipe and is not listed in NEI (the item tooltip says how).
