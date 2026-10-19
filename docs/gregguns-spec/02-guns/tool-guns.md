# Tool guns: Chainsaw and Power Hammers

- **Techguns baseline:** [gun-lifecycle.md section 9](../../techguns-spec/02-guns/gun-lifecycle.md), [sheets-launchers-tools-grenades.md](../../techguns-spec/02-guns/sheets-launchers-tools-grenades.md) (Chainsaw, Power Hammer)
- **Plan:** M5 piece I11 (the base); the items come in batch B3.
- **Code:** `ItemToolGun`, `ToolStats`, `ToolDrain` (pure), `GunDefinition.Builder.tool(..)`.

## What the base does

| Input | Behavior |
|---|---|
| Left click on an entity | Vanilla-style melee through `MeleeStrike` (PHYSICAL GregGuns hit). Attack attribute `meleeDamage` loaded, `emptyMeleeDamage` (2) with no round loaded; armor penetration only while loaded. One hit spends `hitCostMilli`. |
| Left click on a block | Vanilla mining. Loaded: the tool classes, harvest levels and dig speed of `ToolStats`; empty: none (a bare hand). Each block with hardness spends `blockCostMilli`. |
| Right click | The gun's shot: a plain repeating shot (Chainsaw) or a charge-hold shot (Hammers, `GunStats.Builder.charge`). A charge tool gun ignores the plain trigger on the server. |
| Break on the player's behalf | `ItemToolGun.breakBlock(player, stack, x, y, z)`: `BlockBreakGuard`, then drain, then remove and harvest. Every shockwave or cut must use it. |
| Swing | Never cancelled. The sound `guns.<name>.swing` plays at most once per 5 ticks per player, only while loaded. Breaking a block while loaded plays `guns.<name>.hit`. |
| Reload | The normal path: a full tank in, the empty tank back. |

## Drain

Rounds are the fuel or air. A cost is in thousandths of a round (`ToolStats.costs`); the part of the current round already used is the *remainder*, stored as the short `toolrem` next to `ammo`. The last round is consumed whole. Shots spend whole rounds through the existing controller (Chainsaw) or charge release (Hammers).

## Deviations

- **GGS-02-32 fuel-value scaling stays deferred (plan Q7).** A full tank is a fixed number of rounds whatever the fluid. Revisit after M5.
- **GUN-27 not reproduced:** the harvest check is Forge's, per block metadata (`getToolClasses` and `getHarvestLevel(stack, class)` on the item), not metadata 0 only.
- **GUN-28 not reproduced:** there is no internal `default` tool class and the tooltip lists real classes only.
- The empty tool mines as a bare hand (dig speed 1, no classes); Techguns only fixed the dig speed at 1.
- The vanilla mining path does not call `BlockBreakGuard` itself: Forge's `ItemInWorldManager` already posts `BlockEvent.BreakEvent` for a player's break.

## The items (batch B3b)

- **Power Hammer (HV, GR-91) and Power Hammer (Advanced) (EV, GR-92).** Compressed Air Tank, 300 rounds. Right click charges 20 ticks (`ChargeMode.SCALED`, 1 to 10 rounds). The release hits every living entity in a 4 x 4 x 4 cube for `rounds x 2.5` and breaks the cube's blocks nearest-first through `ItemToolGun.breakBlock` (one round each, `BlockBreakGuard` asked per block). The cube is carried 3 blocks along the look and stops at the first solid block. Tile entities, liquids and blocks the hammer cannot harvest are left alone (`Shockwave`, `ItemPowerHammer`). Adv: melee 7, level 3, speed 15, the second skin; GR-92 is an upgrade row, so the NBT of the base hammer is ignored and the result is fresh (GGS-02-40).
- **Chainsaw (HV, GR-90).** Fuel Tank, 300 rounds. Right click cuts with an `InvisibleProjectile` (entity 21; 10 damage, 25% penetration, no knockback, speed 1.5 for 2 ticks). Melee 12, axe 3, speed 14.
- **Deviation:** no gore on the cut or the melee hit (the death-effect milestone owns it). The hammer blast breaking blocks is new; Techguns' blast never did.
- **Sounds:** `guns.power_hammer_adv.fire` plays only `powerhammer_shot_c` and `guns.power_hammer.fire` plays a and b, because each fire file belongs to one event (GGS-03-09).
