# M6 exit gate audit

Evidence for the three M6 exit-gate boxes in [ROADMAP.md](../../ROADMAP.md#m6--utility-and-defense). Audited on master with `m6/rampart` merged. Scenario names and what each asserts are in [docs/testing/README.md](../../testing/README.md). Unit tests are under `com.gregguns.utility`.

## Turret targeting respects owners and friends, with no NPEs (NPC-12, MUTL-25)

| Rule | Scenario | Unit tests |
|---|---|---|
| Owner and friends are never targets at security 0 to 2; hostile mobs are shot | `utility.turret_spares_owner`, `utility.turret_kills_zombie` | `TurretTargetSelectorTest`, `TurretLogicTest` |
| The base links its turret by saved entity UUID and AABB search, never by a stale entity id (MUTL-25) | `utility.turret_no_npe` (kill, repair, respawn after 200 ticks) | `TurretLinkTest`, `TurretHierarchyTest` |
| No NPE with the entity dead, unlinked or unpowered | `utility.turret_no_npe`, `utility.turret_registered` | `TurretAmmoTest`, `RedstoneModeTest` |
| A Rampart mount uses the same rules and fires | `utility.rampart_mount_fires`, `utility.rampart_break_hatch` | `RampartShapeTest`, `RampartPowerTest` |

## Charges use permission checks (MUTL-31)

| Rule | Scenario | Unit tests |
|---|---|---|
| Only the owner or a team member may press any of the seven buttons | `utility.charge_foreign_button` | `ChargePermissionTest` |
| Block breaks go through the break event, so protection mods can cancel them | `utility.charge_respects_cancel` | `ChargeBlastRulesTest` |
| Offline owner: events carry a fake player, not null | `utility.charge_offline_owner` | `ChargePermissionTest` |
| The charge still works: bores stone, drops GT ore correctly | `utility.charge_bores_stone`, `utility.charge_gt_ore_drop` | `ChargeFuseTest`, `ChargeGeometryTest`, `ChargeParamsTest` |

## An integration scenario exists for each block

| Block | Scenario |
|---|---|
| Turret base (MTE 20116 to 20119) | `utility.turret_kills_zombie` |
| TNT and Explosive Charge | `utility.charge_bores_stone` |
| Reinforced Chest and Weapon Chest | `utility.chest_roundtrip` |
| Bio Blob | `utility.bioblob_lifecycle`, `utility.bioblob_poison` |
| Camp Flag | `utility.campflag_spawns`, `utility.campflag_break_drop` |
| Rampart (MTE 20120) and Bastion Plating | `utility.rampart_assembles` |
| Gun Mount Hatch (MTE 20121 to 20124) | `utility.rampart_mount_fires` |

## Gate run

All scenarios named above exist and passed in the final gate run (see the roadmap boxes for the date). Each scenario run boots with the self-check on.
