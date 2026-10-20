# 0007 · Pack loot and Infernal Mobs

- Status: Accepted (by default during the MVP push; revisit after MVP)
- Gate: none (refines [0005](0005-world-presence.md) for loot)
- Date: 2026-10-18

## Context
The owner wants two things:
- GregGuns hostile NPCs should be able to spawn as Infernal Mobs and drop loot by the elite, ultra and infernal tiers;
- GregGuns items should appear in GTNH's loot tables.

The research is in [06-items/pack-loot.md](../gregguns-spec/06-items/pack-loot.md) and [10-entities-npcs/infernal-mobs.md](../gregguns-spec/10-entities-npcs/infernal-mobs.md). The constraints it found:

- **TooMuchLoot replaces vanilla tables.** It ships every vanilla chest category as an `OVERRIDE` file and applies them at server start. Anything GregGuns code adds to a vanilla category is wiped. Its `ADD` mode merges instead.
- **Infernal eligibility is automatic.** Infernal Mobs rolls any `IMob` that isn't `IEntityOwnable`, and allows unseen classes by default. In this pack its keys are **simple class names** (`useSimpleEntityClassnames=true`).
- **Infernal drop lists are global.** They apply to every infernal mob, vanilla ones included. Infernal Mobs exposes `getIsRareEntity` and `getMobModifiers` publicly, so a soft-dependency drop handler can add drops for our NPCs only.
- **Loot bags and dungeon chests are pack data.** EnhancedLootBags tier groups (`tier_1_lv` … `tier_9_uhv`) and Roguelike Dungeons loot rules live only in pack config files.

## Decision
1. **Infernal NPCs.** Every hostile GregGuns NPC is infernal-eligible: it is `IMob` and not `IEntityOwnable`. The turret, allied NPCs and the attack helicopter are not. Each NPC class's simple name must be unique across the whole pack, because Infernal Mobs keys its config on simple names; Amun-Ra, for example, already owns `EntityAlienBug`. The names are listed in [infernal-mobs.md](../gregguns-spec/10-entities-npcs/infernal-mobs.md#infernal-mobs-which-gregguns-entities-are-eligible), and GGS-10-03 checks them.
2. **Tiered NPC loot (route B, on by default).** A soft-dependency `LivingDropsEvent` handler, at `HIGH` priority, adds GregGuns drops to our own infernal NPCs only:
   - modifier size up to 5 is **elite**: LV items;
   - up to 10 is **ultra**: MV items;
   - above 10 is **infernal**: MV items, with rare HV.
   - The handler makes one roll per 5 modifier points, as Infernal Mobs does.
   - The tables and the on/off switch are GregGuns config.
   - NPCs from spawner blocks are excluded unless config allows them.
3. **Global drop lists (route A, pack-side, limited).** The pack appends only **ammunition, magazines, grenades and consumables** up to MV to Infernal Mobs' three global lists. Guns and armor never go in the global lists, so vanilla infernal mobs don't hand out weapons.
4. **Chests.**
   - GregGuns code never adds to vanilla `ChestGenHooks` categories.
   - It registers its own `gregguns:*` categories for its structures.
   - Vanilla and pack chests get GregGuns items through pack-side TooMuchLoot `ADD` files, following the [tier bands](../gregguns-spec/06-items/pack-loot.md#tier-bands).
5. **Loot bags and dungeons.**
   - EnhancedLootBags groups get GregGuns items of their own tier.
   - Roguelike Dungeon levels 0–1 are LV, 2–3 MV and 4 rare HV.
6. **Tier ceiling.** This record amends 0005's "LV–MV for surface structures" rule in two places only: infernal drops from our NPCs and the deepest Roguelike level may be rare HV. Nothing in loot goes above HV.
7. **Delivery.** All pack-side changes are collected in [PACK-CHANGES.md](../gregguns-spec/PACK-CHANGES.md). They ship as one PR to the GTNH modpack repository in M9, after the registry names are fixed in M2–M5.

## Consequences
- M7 builds the NPC classes and the Infernal handler. The handler's band logic is a pure function with unit tests.
- M8 registers the `gregguns:*` chest categories.
- M9 delivers the pack-side PR.
- The draft XML in pack-loot.md uses placeholder registry names until then.

## Alternatives considered
- **Add to vanilla categories in code, as Techguns does:** rejected, because TooMuchLoot wipes it.
- **Guns in the global infernal lists:** rejected, because vanilla zombies would drop HV weapons with no location gate.
- **Ship config files from the mod into other mods' config folders:** rejected. That is the pack's job, and it breaks pack updates.

## Addendum: test victims (2026-10-20)

Scenario mobs opt out of Infernal Mobs with the same marker as spawner NPCs (`InfernalOptOut.MARKER`), written by the test harness (`PackMobGuard`). Infernal Mobs was not the cause of the pack-only `lost NaN` scenario failures; see [damage-interop.md](../gregguns-spec/02-guns/damage-interop.md#pack-mods-that-change-mobs-and-players-found-in-the-real-pack-2026-10-20).
