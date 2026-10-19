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
