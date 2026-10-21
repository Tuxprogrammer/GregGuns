# The GregGuns quest line

The full quest list for the GregGuns tab: 121 quests in 14 chapters. Nine chapters are the main path (a start chapter, then Steam/ULV up to ZPM). Five are side branches (NPCs, structures, machines, equipment, ammo logistics).

- **Techguns baseline:** none. Techguns has no questbook. The content comes from [02-guns](../../techguns-spec/02-guns/index.md), [05-armor](../../techguns-spec/05-armor/index.md), [06-items](../../techguns-spec/06-items/index.md), [08-machines-production](../../techguns-spec/08-machines-production/index.md), [09-machines-utility](../../techguns-spec/09-machines-utility/index.md), [10-entities-npcs](../../techguns-spec/10-entities-npcs/index.md) and [11-worldgen](../../techguns-spec/11-worldgen/index.md).
- **Design records:** [0001 tier map](../../design/0001-tier-map.md), [0005 world presence](../../design/0005-world-presence.md), [0007 pack loot and Infernal Mobs](../../design/0007-pack-loot-and-infernal-mobs.md), [0008 exact-tier charging](../../design/0008-exact-tier-charging.md), [0009 GTNH integration defaults](../../design/0009-gtnh-integration-defaults.md)
- **Pack mods:**
  - BetterQuesting (`betterquesting`, `bq_standard`, 3.8.89-GTNH)
  - EnhancedLootBags (`enhancedlootbags`, 1.3.5)
  - Infernal Mobs (`InfernalMobs`, 1.10.7-GTNH)
- **Roadmap:** [M9](../../ROADMAP.md#m9--pack-integration-and-release). The quest line ships in the jar ([0015](../../design/0015-m9-self-contained-polish.md)). Every item and entity name below is a registered name, checked by `QuestbookResourcesTest`.

How to turn these tables into files is in [authoring.md](authoring.md). Gates, tab placement and the list of pack quests we depend on are in [index.md](index.md).

## How to read the tables

Every row is one quest. The **key** is stable: the quest's UUID is derived from it ([authoring.md](authoring.md#ids)), so a rename of the key is a new quest.

| Column | Meaning |
|---|---|
| Key | `gg.<chapter>.<name>`. The quest's identity |
| Title | The quest's `name` property. `SECRET` marks a quest with `visibility` SECRET |
| Tier | The D1 tier ([0001](../../design/0001-tier-map.md)) this quest teaches. Rewards never exceed it |
| Hint | The short text. The pack file's `desc` expands it to two to four short paragraphs |
| Task | `checkbox`, `retrieval` (the player holds the item; `or` means any one of the items), `crafting` (crafting table only) or `hunt` (kills by a player). More than one task means all of them |
| Reward | `bag` is `enhancedlootbags:lootbag` with the metadata of the tier group (Steam 2, LV 4, MV 5, HV 6, EV 7, IV 8, LuV 41, ZPM 42). Other rewards are the item and the count |
| Prerequisites | `gg.…` is a GregGuns quest. `pack N` is an existing pack quest, listed with its name in [index.md](index.md#referenced-pack-quests). `any of` means the quest unlocks when one of them is done. `(hover)` is a BetterQuesting implicit prerequisite: it still gates, but its line shows only on hover or with Shift held |
| Layout | `x, y (size)` in canvas units. Size 28 is a chapter head, 24 everything else. No quest is `main` |

Item names are the registry names (`gregguns:<name>`, as `NeiOrder` lists them). The GT meta tile entities (the Ammo Press, Ammunition Works, Repair Bench, Armory Gantry and Turret) are `gregtech:gt.blockmachines` stacks whose damage is the reserved MTE id from `GregGunsMTEIDs`. The data behind these tables is `tools/questbook/quests.json`; the tables are regenerated from it, so edit the data and rebuild.

## Design rules

1. **One mechanic per quest.** Each quest teaches one thing: reloading, filling a magazine, pressing a part, charging at the right tier, mending a weapon, wearing an item in a Baubles slot. The [coverage matrix](#coverage-matrix) lists where each mechanic is taught.
2. **The tab gates on GTNH progression, not on itself.** A chapter intro needs the pack's tier quest and the pack's gate-material quest ([unlock design](index.md#unlock-design)). A player who skipped the Steam chapter can still open LV.
3. **Machine-made items use retrieval.** `crafting` tasks listen for the crafting table, the furnace and the anvil only ([`TaskCrafting`](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/tasks/TaskCrafting.java#L189-L200)). The pack's own Readme also limits crafting quests to the early game. Only the four Steam-chapter quests use `crafting`; everything made in a GT machine uses `retrieval`, so a loot-bag find counts too.
4. **Rewards are tier-legal.** A quest never pays above its Tier column, and its Tier never exceeds the highest pack tier among its prerequisites ([GGS-12-07](index.md#requirements)). This is the rule [0007](../../design/0007-pack-loot-and-infernal-mobs.md) uses for loot, applied to quest rewards.
5. **Bags are random, so they are never the only help.** A tier bag draws one or two items from the whole tier group ([pack-loot.md](../06-items/pack-loot.md#enhancedlootbags)), and most of that group is not GregGuns. Quests that teach a weapon pay in ammunition or a magazine for it. Bags mark the end of a chapter or a hunt.
6. **Honor-system quests are rare and labeled.** Infernal variants cannot be matched by a hunt task (see [Infernal variants](#npcs-and-bosses)), and structure visits have no location task. Both use a checkbox. There are 45 checkbox quests in all, 22 of them chapter intros and summaries; the rest are one-line "do this once" lessons.
7. **No command rewards.** A command reward runs with the quest's authority and skips the tier rule. None is used.

### Reward budget

25 quests pay a bag. The counts below are the starting point for the M9 balance pass.

| Tier | Group id (`Damage`) | Bags | Quests |
|---|---|---|---|
| Steam | 2 | 2 | `gg.steam.summary`, `gg.npc.zombies` |
| LV | 4 | 6 | `gg.lv.summary`, `gg.npc.bandits`, `gg.npc.steve`, `gg.npc.elite`, `gg.site.surface`, `gg.site.dungeon` |
| MV | 5 | 7 | `gg.mv.summary`, `gg.npc.army`, `gg.npc.commandos`, `gg.npc.ultra`, `gg.site.bunker`, `gg.site.camp`, `gg.site.ships` |
| HV | 6 | 3 | `gg.hv.summary`, `gg.npc.helicopter`, `gg.npc.infernal` |
| EV | 7 | 3 | `gg.ev.summary`, `gg.npc.cyberdemon`, `gg.npc.alien_bug` |
| IV | 8 | 2 | `gg.iv.summary`, `gg.npc.dave` |
| LuV | 41 | 1 | `gg.luv.summary` |
| ZPM | 42 | 1 | `gg.zpm.summary` |

Fixed item rewards are small: 4 to 64 rounds, one magazine, one grenade pack. They exist so that a player who has just built a gun can fire it.

## Layout

The layout is a forest of short trees on a 36 unit pitch (a 24 unit box plus 12 of air, close to the Applied Energistics tab). Each tier chapter is a vertical tree hanging from its head (28 units); the heads sit in one row, left to right in tier order, and each head chains from the previous tier's head (Steam from `gg.start.controls`), so there is no fan-out from `gg.start.welcome`. The five side chapters (NPCs, sites, machines, ammo, gear) are horizontal trees in a band underneath. A quest's first prerequisite is its parent in the tree and is drawn; a second one (summaries, cross-branch needs) is an implicit prerequisite, so it gates without drawing a braid. Each side chapter's head unlocks from the gun quest it depends on, as an implicit prerequisite. No quest is `main`, so every line is the thin 4 unit kind ([README](../../../tools/questbook/README.md#how-betterquesting-draws-a-line)). The validator ([authoring.md](authoring.md#builder-and-validator)) rejects overlapping boxes. `python tools/questbook/render_preview.py out.png` draws the line the way BetterQuesting does.

## Relation to the pack's lines

- **Kill All the Things** (`KillAlltheThings-AAAAAAAAAAAAAAAAAAAAJQ==`, 83 quests, last tab in `QuestLinesOrder.txt`) is a list of `bq_standard:hunt` quests. Each hunt has a `target:8` entity name (`Zombie`, `witchery.babayaga`, `etfuturum.fox`), a `required:3` count and no tier gate. Its roots are pack quests 11 (Monster Hunter), 225 (Searching for Answers), 479 (THE NETHER) and 986 (the End). Prerequisites cross lines freely: those four live in other lines.
- GregGuns follows the same pattern. `gg.start.welcome` unlocks from quests that live elsewhere (11 and 44), and the NPC branch uses the same hunt task with `gregguns.<Name>` targets. GregGuns stays a separate tab for three reasons. Its hunts need tier gating that Kill All the Things does not have. Its rewards are tier bags. And a pack without GregGuns must be able to drop the tab as one folder.
- The tab is placed right after Kill All the Things ([index.md](index.md#tab-placement)).
- An optional one-quest cross link puts `gg.npc.intro` into the Kill All the Things line, which the pack Readme allows for transition quests ([index.md](index.md#delivered-from-the-jar)).

## Chapters

### Start here

- **Gate:** pack 11 (Monster Hunter) or pack 44 (Welcome to Tier 0.5, Steam!), whichever comes first.
- **Teaches:** controls, ammo vocabulary, where gear comes from, how to read a tier. No rewards.

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.start.welcome` | Welcome to GregGuns | Steam | Guns here follow GregTech progression: each weapon needs a machine tier and a gate material. This tab walks you up the tiers. | checkbox | - | any of: pack 11, pack 44 | 58, 22 (28) |
| `gg.start.controls` | Fire, Reload, Zoom | Steam | Hold use to fire, press the reload key to swap magazines, right-click to zoom. Rebind keys in Controls. | checkbox | - | `gg.start.welcome` | 60, 60 (24) |
| `gg.start.ammo_types` | Ammo and Magazines | Steam | Each gun takes one caliber. Loose rounds fill a magazine; sneak-click a magazine to empty it. | checkbox | - | `gg.start.controls` | 96, 60 (24) |
| `gg.start.find_gear` | Where Gear Comes From | Steam | Craft it in GT machines, or find it: quest loot bags, chests in ruins, drops from hostile soldiers. NEI lists every recipe and drop. | checkbox | - | `gg.start.welcome` | 96, 24 (24) |
| `gg.start.tiers` | Reading a Tier | Steam | An LV item needs an LV machine and steel, an MV item needs aluminum, and so on. Tooltips show the tier. | checkbox | - | `gg.start.find_gear` | 132, 24 (24) |

### Steam / ULV

- **Gate:** pack 44 and pack 948 (Better Than Iron: bronze and wrought iron, the tier-map gate for this tier).
- **Teaches:** crafting-table guns and ammo, armor value. The only chapter that uses `crafting` tasks.

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.steam.intro` | Steam-Age Arms | Steam | Bronze and wrought iron are enough for a first firearm. No electricity needed. | checkbox | - | `gg.start.controls`, pack 44, pack 948 | 58, 94 (28) |
| `gg.steam.hand_cannon` | Hand Cannon | Steam | A single-shot bronze and wrought-iron cannon. Slow and loud. | crafting `gregguns:handcannon` | `gregguns:ammo_stone_bullet` x16 | `gg.steam.intro` | 24, 132 (24) |
| `gg.steam.stone_bullets` | Stone Bullets | Steam | Cobblestone and gunpowder at a crafting table. Your first lesson in ammo supply. | crafting `gregguns:ammo_stone_bullet` | `gregguns:ammo_stone_bullet` x16 | `gg.steam.hand_cannon` | 24, 168 (24) |
| `gg.steam.first_blood` | First Blood | Steam | Shoot five zombies with the Hand Cannon. Reload between shots. | hunt `Zombie` x5 | `gregguns:ammo_stone_bullet` x32 | `gg.steam.stone_bullets` | 24, 204 (24) |
| `gg.steam.scout_armor` | T1 Scout Armor | Steam | Light armor. Each piece adds to your armor value; read the tooltips. | crafting `gregguns:t1_scout_chestplate` | - | `gg.steam.intro` | 60, 132 (24) |
| `gg.steam.miner_armor` | T1 Miner Armor | Steam | Miner set, crafted from bronze plate and wrought iron. | crafting `gregguns:t1_miner_helmet` | - | `gg.steam.intro` | 96, 132 (24) |
| `gg.steam.summary` | Steam Age Done | Steam | Steel is next. Make it in a GT furnace setup, then open the LV chapter. | checkbox | bag `enhancedlootbags:lootbag:2` (Steam) | `gg.steam.miner_armor`, `gg.steam.first_blood` (hover) | 96, 168 (24) |

### LV

- **Gate:** pack 88 (Welcome to Tier 1, LV) and pack 76 (Time to Get Some Steel).
- **Teaches:** forming-press parts, assembler ammo, magazines, sidearm, shotgun and rifle roles, melee, steel armor.

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.lv.intro` | Steel-Age Firearms | LV | LV opens steel guns. The last step of each recipe is an LV machine. | checkbox | - | `gg.steam.intro`, pack 88, pack 76 | 270, 94 (28) |
| `gg.lv.ammo_press` | Ammo Press | LV | Ammo is made in the GregGuns Ammo Press, not in a GT Assembler. Build the LV press: it has its own recipe list in NEI. | retrieval `gregtech:gt.blockmachines:20101` (Ammo Press LV) | - | `gg.lv.intro` | 213, 132 (24) |
| `gg.lv.ammo_9mm` | 9mm Rounds | LV | The Ammo Press builds 9mm rounds from a GT round, a foil casing and gunpowder. Check the Ammo Press tab in NEI. | retrieval `gregguns:ammo_9mm_bullet` | `gregguns:ammo_9mm_bullet` x32 | `gg.lv.ammo_press` | 168, 168 (24) |
| `gg.lv.pistol` | 9mm Pistol | LV | Your first real sidearm. Magazine-fed. | retrieval `gregguns:pistol_9mm` | `gregguns:mag_9mm_pistol` x1 | `gg.lv.ammo_9mm` | 150, 204 (24) |
| `gg.lv.magazines` | Magazines | LV | Empty and filled magazines are Ammo Press recipes too. Sneak-click a magazine to empty it. | retrieval `gregguns:mag_9mm_pistol` | `gregguns:ammo_9mm_bullet` x16 | `gg.lv.pistol` | 150, 240 (24) |
| `gg.lv.revolver` | Revolver | LV | A heavier sidearm. Compare its damage with the pistol on the tooltip. | retrieval `gregguns:revolver` | - | `gg.lv.ammo_9mm` | 186, 204 (24) |
| `gg.lv.sawed_off` | Sawed Off | LV | Spread pellets: strong at point-blank range, weak far away. Uses shotgun shells. | retrieval `gregguns:sawed_off` | `gregguns:ammo_shotgun_shell` x8 | `gg.lv.ammo_press` | 222, 168 (24) |
| `gg.lv.bolt_rifle` | Bolt Action Rifle | LV | Long range and slow to cycle. Right-click to zoom. | retrieval `gregguns:bolt_action_rifle` | `gregguns:ammo_rifle_bullet` x8 | `gg.lv.ammo_press` | 258, 168 (24) |
| `gg.lv.melee` | Knife or Crowbar | LV | Melee tools never run dry. | retrieval `gregguns:combat_knife` or `gregguns:crowbar` | - | `gg.lv.intro` | 294, 132 (24) |
| `gg.lv.combat_armor` | T1 Combat Armor | LV | Steel plate pressed in the LV Assembler. | retrieval `gregguns:t1_combat_chestplate` | - | `gg.lv.intro` | 330, 132 (24) |
| `gg.lv.summary` | Steel Age Done | LV | MV needs aluminum and a bigger power setup. Move on when your GT line gets there. | checkbox | bag `enhancedlootbags:lootbag:4` (LV) | `gg.lv.combat_armor`, `gg.lv.pistol` (hover) | 330, 168 (24) |

### MV

- **Gate:** pack 1148 (Tier 2 (MV)) and pack 1598 (the MV assembler).
- **Teaches:** automatic weapons and their separate magazines, offhand (akimbo) use, explosives, the stun grenade, second-tier armor sets.

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.mv.intro` | Aluminum-Age Arms | MV | MV adds automatic weapons, grenades and second-tier armor. Aluminum is the gate. | checkbox | - | `gg.lv.intro`, pack 1148, pack 1598 | 472, 94 (28) |
| `gg.mv.smg_mags` | SMG Magazines | MV | Press an empty magazine in the Ammo Press, then fill it with rounds. | retrieval `gregguns:mag_smg` | - | `gg.mv.intro` | 384, 132 (24) |
| `gg.mv.thompson` | Thompson SMG | MV | Fast, light automatic fire at MV. | retrieval `gregguns:thompson_smg` | `gregguns:mag_smg` x1 | `gg.mv.smg_mags` | 384, 168 (24) |
| `gg.mv.rifle` | M4 or AK47 | MV | Assault rifles use their own magazines, not SMG magazines. | retrieval `gregguns:m4` or `gregguns:ak47` | `gregguns:mag_assault_rifle` x1 | `gg.mv.intro` | 420, 132 (24) |
| `gg.mv.combat_shotgun` | Combat Shotgun | MV | Magazine-fed shotgun. Uses the same shells as the Sawed Off. | retrieval `gregguns:combat_shotgun` | `gregguns:ammo_shotgun_shell` x16 | `gg.mv.intro` | 456, 132 (24) |
| `gg.mv.akimbo` | Akimbo | MV | A matching pistol in each hand. Needs the offhand slot. | retrieval `gregguns:akimbo_pistols` or `gregguns:akimbo_revolvers` | - | `gg.mv.intro` | 492, 132 (24) |
| `gg.mv.grenades` | Frag Grenade | MV | Explosives need TNT dust from the GT line. Throw from a safe distance. | retrieval `gregguns:frag_grenade` | `gregguns:frag_grenade` x4 | `gg.mv.intro`, pack 762 | 528, 132 (24) |
| `gg.mv.flashbang` | Flashbang | MV | Blinds and stuns instead of killing. Useful for clearing a camp. | retrieval `gregguns:flashbang` | `gregguns:flashbang` x4 | `gg.mv.grenades` | 528, 168 (24) |
| `gg.mv.t2_armor` | T2 Armor | MV | Combat, Commando or Riot. Pick the set that suits your style. | retrieval `gregguns:t2_combat_chestplate` or `gregguns:t2_commando_chestplate` or `gregguns:t2_riot_chestplate` | - | `gg.mv.intro` | 564, 132 (24) |
| `gg.mv.summary` | Aluminum Age Done | MV | HV brings heavy weapons and stainless steel. | checkbox | bag `enhancedlootbags:lootbag:5` (MV) | `gg.mv.t2_armor`, `gg.mv.thompson` (hover) | 564, 168 (24) |

### HV

- **Gate:** pack 160 (Tier 3 (HV)) and pack 753 (stainless steel).
- **Teaches:** heavy weapons, launchers and their ammo, fuel, and the first exact-tier charge (HV tools at an HV buffer).

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.hv.intro` | Heavy Arms | HV | HV adds heavy weapons, launchers and electric tools. Stainless steel is the gate. | checkbox | - | `gg.mv.intro`, pack 160, pack 753 | 706, 94 (28) |
| `gg.hv.rifles` | AUG, SCAR or Vector | HV | Specialist rifles. Pick one; they are all HV builds. | retrieval `gregguns:aug` or `gregguns:scar` or `gregguns:vector` | `gregguns:mag_assault_rifle` x2 | `gg.hv.intro` | 618, 132 (24) |
| `gg.hv.lmg` | LMG | HV | Belt-fed and heavy. LMG magazines are an HV part. | retrieval `gregguns:mag_lmg`; retrieval `gregguns:lmg` (all) | - | `gg.hv.intro` | 654, 132 (24) |
| `gg.hv.as50` | AS50 | HV | Anti-materiel rifle with its own magazines. | retrieval `gregguns:as50` | - | `gg.hv.intro` | 690, 132 (24) |
| `gg.hv.grenade_launcher` | Grenade Launcher | HV | Fires the 40 mm grenades you learned to make at MV. | retrieval `gregguns:grenade_launcher` | `gregguns:ammo_40mm_grenade` x8 | `gg.hv.intro` | 726, 132 (24) |
| `gg.hv.rocket_launcher` | Rocket Launcher | HV | Rockets need explosive dust and a circuit. Mind your own blast radius. | retrieval `gregguns:rocket_launcher` | `gregguns:ammo_rocket` x2 | `gg.hv.grenade_launcher` | 726, 168 (24) |
| `gg.hv.flamethrower` | Flamethrower | HV | Burns GT fuel from a fuel tank. Diesel works. | retrieval `gregguns:flamethrower` | `gregguns:tank_fuel` x1 | `gg.hv.intro`, pack 553 | 762, 132 (24) |
| `gg.hv.tools` | Chainsaw and Power Hammer | HV | Electric tools charge only in a charger or battery buffer of exactly their tier: HV. | retrieval `gregguns:chainsaw` or `gregguns:power_hammer` | - | `gg.hv.intro`, pack 1142 | 798, 132 (24) |
| `gg.hv.summary` | Heavy Arms Done | HV | EV brings energy weapons. | checkbox | bag `enhancedlootbags:lootbag:6` (HV) | `gg.hv.tools`, `gg.hv.lmg` (hover) | 798, 168 (24) |

### EV

- **Gate:** pack 176 (Tier 4 (EV)) and pack 860 (Titanium, Finally).
- **Teaches:** energy cells, energy weapons and charging them at exactly EV ([0008](../../design/0008-exact-tier-charging.md)), advanced ammunition, the T3 and steam suits.

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.ev.intro` | Energy Weapons | EV | EV adds lasers, pulse rifles and electric ammo. Titanium is the gate. | checkbox | - | `gg.hv.intro`, pack 176, pack 860 | 940, 94 (28) |
| `gg.ev.laser_focus` | Laser Focus | EV | The Laser Focus is cut in a GT Laser Engraver at EV. It is the core of every laser weapon. | retrieval `gregguns:part_laser_focus` | - | `gg.ev.intro` | 852, 132 (24) |
| `gg.ev.laser` | Laser Rifle | EV | Electric, so it charges at an EV battery buffer. An HV buffer will not accept it. | retrieval `gregguns:laser_rifle` | - | `gg.ev.laser_focus`, pack 1262 | 852, 168 (24) |
| `gg.ev.pulse` | Pulse Rifle or PDW | EV | Advanced magazines are the EV ammo step up. | retrieval `gregguns:pulse_rifle` or `gregguns:pdw` | `gregguns:mag_advanced` x2 | `gg.ev.intro` | 888, 132 (24) |
| `gg.ev.minigun` | Minigun | EV | Drum-fed, very high fire rate. Heavy on ammo. | retrieval `gregguns:minigun` | `gregguns:mag_minigun_drum` x1 | `gg.ev.pulse` | 888, 168 (24) |
| `gg.ev.exotics` | Tesla or Sonic Rifle | EV | Specialist weapons. Pick one. | retrieval `gregguns:tesla_rifle` or `gregguns:sonic_rifle` | - | `gg.ev.intro` | 924, 132 (24) |
| `gg.ev.t3_armor` | T3 Combat Armor | EV | Titanium plate in the EV Assembler. | retrieval `gregguns:t3_combat_chestplate` | - | `gg.ev.intro` | 960, 132 (24) |
| `gg.ev.steam_armor` | Steam Armor | EV | Self-contained powered suit. | retrieval `gregguns:steam_chestplate` | - | `gg.ev.intro` | 996, 132 (24) |
| `gg.ev.summary` | Energy Weapons Done | EV | IV brings blasters and power armor. | checkbox | bag `enhancedlootbags:lootbag:7` (EV) | `gg.ev.t3_armor`, `gg.ev.laser` (hover) | 960, 168 (24) |
| `gg.ev.bio_culture` | E. coli Culture | EV | The Bio Gun runs on culture fluid. Brew Biomass, make the E. coli dish in the Bio Lab, then grow the culture in the BartWorks Bacterial Vat. | checkbox | - | `gg.ev.intro` | 1032, 132 (24) |
| `gg.ev.bio_tank` | Bio Tank | EV | The Bio Tank is built from a GT Large Fluid Cell. A Fluid Canner fills it with E. coli culture fluid. | retrieval `gregguns:tank_bio` | - | `gg.ev.bio_culture` | 1032, 168 (24) |
| `gg.ev.bio_gun` | Bio Gun | EV | Sprays culture fluid that eats flesh. Refill it from the Bio Tank. The Nether Blaster later shares this chain. | retrieval `gregguns:bio_gun` | - | `gg.ev.bio_tank` | 1032, 204 (24) |

**Pack intermediates in the EV tier.** The Bio Gun chain has its own three quests (`gg.ev.bio_culture`, `gg.ev.bio_tank`, `gg.ev.bio_gun`). Their pack steps are hint text, not prerequisites. The Fuel, Bio and Compressed Air Tanks are built from the pack's Large Fluid Cells (stainless, titanium and aluminium), so the quests that retrieve a tank or the guns that use one can point the player at the pack's Large Fluid Cell quests (Tier 3 to 4 fluid storage). Cybernetic Parts need a Plasma Membrane from the pack's Bio Lab line: Bio Lab (HV) makes the E. coli dish, the Bacterial Vat makes culture fluid, and an HV Centrifuge turns 1,000 mB of it into a Plasma Membrane (circuit 17). The same culture fluid fills the Bio Tank, so `gg.ev.exotics` and the Nether Blaster line (`gg.luv.nether_charge`) share one bio chain. The quests carry a hint, not a prerequisite on a pack quest id, because the Bacterial Vat and Bio Lab quest ids are not in the referenced table. Recipe detail: [recipes.md](../06-items/recipes.md) RC-20, RC-52 to RC-54; pack requirements: [PACK-CHANGES](../PACK-CHANGES.md).

### IV

- **Gate:** pack 213 (Tier 5 (IV)) and pack 861 (Tungstensteel).
- **Teaches:** blasters, power armor and its charging, exo suit and anti-grav device.

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.iv.intro` | Power Armor Era | IV | IV adds blasters, power armor and the anti-grav device. Tungstensteel is the gate. | checkbox | - | `gg.ev.intro`, pack 213, pack 861 | 1156, 94 (28) |
| `gg.iv.blaster` | Blaster Rifle | IV | High-damage energy rifle at IV voltage. | retrieval `gregguns:blaster_rifle` | - | `gg.iv.intro` | 1086, 132 (24) |
| `gg.iv.deatomizer` | De-Atomizer Pistol | IV | One pistol or two; the akimbo pair needs the offhand slot. | retrieval `gregguns:deatomizer_pistol` or `gregguns:akimbo_deatomizer_pistols` | - | `gg.iv.intro` | 1122, 132 (24) |
| `gg.iv.scatter` | Scatter Laser Rifle | IV | A shotgun-style laser. Short range, wide spread. | retrieval `gregguns:scatter_laser_rifle` | - | `gg.iv.blaster` | 1086, 168 (24) |
| `gg.iv.golden` | Golden Revolver | IV | A showpiece sidearm. Single or akimbo. | retrieval `gregguns:golden_revolver` or `gregguns:akimbo_golden_revolvers` | - | `gg.iv.intro` | 1158, 132 (24) |
| `gg.iv.power_armor` | Power Armor | IV | Full powered suit. Charge it at an IV buffer. | retrieval `gregguns:t3_power_chestplate` | - | `gg.iv.intro` | 1230, 132 (24) |
| `gg.iv.exo` | Exo Suit | IV | Strength and carry bonuses. | retrieval `gregguns:t3_exo_chestplate` | - | `gg.iv.power_armor` | 1194, 168 (24) |
| `gg.iv.antigrav` | Anti-Gravity Device | IV | Back device for flight. Needs the wings slot. | retrieval `gregguns:anti_gravity_device` | - | `gg.iv.power_armor` | 1230, 168 (24) |
| `gg.iv.summary` | Power Armor Era Done | IV | LuV recipes use the Assembly Line. | checkbox | bag `enhancedlootbags:lootbag:8` (IV) | `gg.iv.power_armor`, `gg.iv.blaster` (hover) | 1266, 168 (24) |

### LuV

- **Gate:** pack 1493 (Tier 6 (LuV)), pack 1491 (HSS-G) and pack 1412 (Assembly Line).
- **Teaches:** Assembly Line recipes with research, Nether charges.

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.luv.intro` | Assembly-Line Weapons | LuV | LuV weapons are built in the Assembly Line with a research item, like GT's own LuV parts. | checkbox | - | `gg.iv.intro`, pack 1493, pack 1491, pack 1412 | 1354, 94 (28) |
| `gg.luv.nether_charge` | Nether Charge | LuV | The explosive behind the Nether Blaster. | retrieval `gregguns:ammo_nether_charge` | - | `gg.luv.intro` | 1320, 132 (24) |
| `gg.luv.nether_blaster` | Nether Blaster | LuV | Fires nether charges. Mind the splash. | retrieval `gregguns:nether_blaster` | - | `gg.luv.nether_charge` | 1320, 168 (24) |
| `gg.luv.alien_blaster` | Alien Blaster | LuV | LuV energy weapon. | retrieval `gregguns:alien_blaster` | - | `gg.luv.intro` | 1356, 132 (24) |
| `gg.luv.grim_reaper` | Grim Reaper | LuV | Late-game close-range weapon. | retrieval `gregguns:grim_reaper` | - | `gg.luv.intro` | 1392, 132 (24) |
| `gg.luv.summary` | LuV Arms Done | LuV | ZPM is the top of the GregGuns tree. | checkbox | bag `enhancedlootbags:lootbag:41` (LuV) | `gg.luv.grim_reaper`, `gg.luv.nether_blaster` (hover) | 1392, 168 (24) |

### ZPM

- **Gate:** pack 2610 (Tier 7 (ZPM)) and pack 1667 (Your First Naquadah Ingot).
- **Teaches:** nuclear power cells and the top weapons.

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.zpm.intro` | Nuclear Arms | ZPM | ZPM weapons use nuclear cells. Naquadah is the gate. | checkbox | - | `gg.luv.intro`, pack 2610, pack 1667 | 1462, 94 (28) |
| `gg.zpm.cell` | Nuclear Power Cell | ZPM | Uranium-based power cell. Store it carefully. | retrieval `gregguns:cell_nuclear` | - | `gg.zpm.intro` | 1464, 132 (24) |
| `gg.zpm.death_ray` | Nuclear Death Ray | ZPM | The top energy weapon. | retrieval `gregguns:nuclear_death_ray` | - | `gg.zpm.cell` | 1446, 168 (24) |
| `gg.zpm.nuke_launcher` | Nuke Launcher | ZPM | Fires tactical nuke rockets. Do not use near your base. | retrieval `gregguns:nuke_launcher` | - | `gg.zpm.cell` | 1482, 168 (24) |
| `gg.zpm.summary` | Top of the Tree | ZPM | You have built everything GregGuns offers. | checkbox | bag `enhancedlootbags:lootbag:42` (ZPM) | `gg.zpm.nuke_launcher`, `gg.zpm.death_ray` (hover) | 1482, 204 (24) |

### NPCs and bosses

- **Gate:** pack 11 and `gg.start.welcome`, then each rank by tier quest. Rewards follow the NPC's tier band ([0005](../../design/0005-world-presence.md), [0007](../../design/0007-pack-loot-and-infernal-mobs.md)).
- **Teaches:** which factions exist, what they carry, and what Infernal ranks mean.
- **Hunt targets** are the `gregguns.<Name>` entity names from [registry-and-pack-hooks.md](../10-entities-npcs/registry-and-pack-hooks.md), with `subtypes` off so a subclass does not count. A hunt counts only a kill by a player ([`TaskHunt.onKilledByPlayer`](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.89-GTNH/src/main/java/bq_standard/tasks/TaskHunt.java#L119-L137)). Projectiles from a GregGuns weapon use an indirect damage source with the player as the attacker ([0009](../../design/0009-gtnh-integration-defaults.md) item 3), so those kills count.
- **Infernal variants.** Infernal Mobs stores the modifier list as one string in the entity's persistent data under `InfernalMobsMod` ([EntityEventHandler#L72-L73](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L72-L73), [InfernalMobsCore#L125-L127](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L125-L127)). A hunt task can only compare NBT for an exact match, so it cannot say "any infernal mob". The three rank quests are checkboxes ([GGS-12-11](index.md#requirements)).
- **The three secret quests** (Dictator Dave, Cyberdemon, Alien Bug) are bosses that do not spawn naturally ([0009](../../design/0009-gtnh-integration-defaults.md) item 5, GGS-11-15). They are `SECRET` so that a pack with those spawns off never shows a quest nobody can finish.

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.npc.intro` | Hostile Factions | Steam | Soldiers, bandits and zombies roam the Overworld. Some carry guns; some drop ammo. | checkbox | - | `gg.steam.first_blood` (hover), pack 11 | 22, 382 (28) |
| `gg.npc.zombies` | Walking Dead | Steam | Hunt the zombie variants: soldiers, farmers and miners. | hunt `gregguns.ZombieSoldier` x3; hunt `gregguns.ZombieFarmer` x3; hunt `gregguns.ZombieMiner` x3 (all) | bag `enhancedlootbags:lootbag:2` (Steam) | `gg.npc.intro` | 60, 384 (24) |
| `gg.npc.skeletons` | Skeleton Soldiers | Steam | Armed skeletons shoot back. Use cover. | hunt `gregguns.SkeletonSoldier` x5 | `gregguns:ammo_stone_bullet` x16 | `gg.npc.zombies` | 96, 384 (24) |
| `gg.npc.bandits` | Bandits and Outcasts | LV | Raiders carry LV gear. Expect 9mm drops. | hunt `gregguns.Bandit` x3; hunt `gregguns.Outcast` x3 (all) | bag `enhancedlootbags:lootbag:4` (LV) | `gg.npc.skeletons`, pack 88 | 132, 384 (24) |
| `gg.npc.steve` | Psycho Steve | LV | A chainsaw mini-boss. His drop is tier-legal for the Overworld, not an HV saw. | hunt `gregguns.PsychoSteve` x1 | bag `enhancedlootbags:lootbag:4` (LV) | `gg.npc.bandits` | 168, 312 (24) |
| `gg.npc.army` | Army Soldiers | MV | Disciplined and well armed. They carry MV ammunition. | hunt `gregguns.ArmySoldier` x5 | bag `enhancedlootbags:lootbag:5` (MV) | `gg.npc.bandits`, pack 1148 | 168, 384 (24) |
| `gg.npc.commandos` | Commandos and Stormtroopers | MV | Elite soldiers. Armor up first. | hunt `gregguns.Commando` x2; hunt `gregguns.Stormtrooper` x2 (all) | bag `enhancedlootbags:lootbag:5` (MV) | `gg.npc.army` | 204, 384 (24) |
| `gg.npc.helicopter` | Attack Helicopter | HV | A flying boss with its own health bar. Bring launchers. | hunt `gregguns.AttackHelicopter` x1 | bag `enhancedlootbags:lootbag:6` (HV) | `gg.npc.commandos`, pack 160 | 240, 384 (24) |
| `gg.npc.elite` | Elite Variants | LV | Kill one elite Infernal mob: up to five modifiers, glowing name. Tick the box when you have. | checkbox | bag `enhancedlootbags:lootbag:4` (LV) | `gg.npc.bandits` | 168, 456 (24) |
| `gg.npc.ultra` | Ultra Variants | MV | Kill one ultra Infernal mob. Ultra drops are MV. | checkbox | bag `enhancedlootbags:lootbag:5` (MV) | `gg.npc.elite`, pack 1148 | 204, 456 (24) |
| `gg.npc.infernal` | Infernal Variants | HV | Kill one infernal-rank mob. It can drop rare HV items. | checkbox | bag `enhancedlootbags:lootbag:6` (HV) | `gg.npc.ultra`, pack 160 | 240, 456 (24) |
| `gg.npc.dave` | Dictator Dave SECRET | IV | A secret mini-boss with a Golden Revolver. Appears only from spawn eggs or the pack's own spawner setup. | hunt `gregguns.DictatorDave` x1 | bag `enhancedlootbags:lootbag:8` (IV) | `gg.npc.helicopter`, pack 213 | 276, 348 (24) |
| `gg.npc.cyberdemon` | Cyberdemon SECRET | EV | A config-gated late boss. No natural spawn. | hunt `gregguns.CyberDemon` x1 | bag `enhancedlootbags:lootbag:7` (EV) | `gg.npc.helicopter`, pack 176 | 276, 384 (24) |
| `gg.npc.alien_bug` | Alien Bug SECRET | EV | A config-gated boss from the bug nest. | hunt `gregguns.AlienBug` x1 | bag `enhancedlootbags:lootbag:7` (EV) | `gg.npc.helicopter`, pack 176 | 276, 420 (24) |

### Structures

- **Gate:** pack 11 and `gg.start.welcome`, then tier quests. GregGuns structures are not vanilla map-gen structures, so a `location` task cannot name them; every quest here is a checkbox ([GGS-12-12](index.md#requirements)).
- **Teaches:** where the tiered chests are. Bag tiers follow the structure ceilings in [structure-loot.md](../11-worldgen/structure-loot.md).

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.site.intro` | Ruins and Camps | Steam | Natural structures dot the Overworld: factories, houses, bunkers, camps and ships. Their chests hold tiered gear. | checkbox | - | `gg.steam.hand_cannon` (hover), pack 11 | 346, 328 (28) |
| `gg.site.surface` | Small Ruins | LV | Factory building, train station, medium house and gas station. Their chests hold LV gear. | checkbox | bag `enhancedlootbags:lootbag:4` (LV) | `gg.site.intro`, pack 88 | 384, 330 (24) |
| `gg.site.dungeon` | Dungeon Weapons | LV | Roguelike dungeons drop weapons by depth: LV near the surface, MV deeper. | checkbox | bag `enhancedlootbags:lootbag:4` (LV) | `gg.site.surface` | 420, 312 (24) |
| `gg.site.bunker` | Big Bunker | MV | Reinforced chests and soldiers. MV ceiling. | checkbox | bag `enhancedlootbags:lootbag:5` (MV) | `gg.site.surface`, pack 1148 | 420, 348 (24) |
| `gg.site.camp` | Military Camp | MV | Tents, barracks and a flag. Many soldiers. | checkbox | bag `enhancedlootbags:lootbag:5` (MV) | `gg.site.bunker` | 456, 348 (24) |
| `gg.site.ships` | Carrier and Submarine | MV | Large structures with MV weapon chests. | checkbox | bag `enhancedlootbags:lootbag:5` (MV) | `gg.site.camp` | 492, 348 (24) |

### Machines

- **Gate:** pack 97 (the LV assembler) and `gg.start.welcome`, then a tier quest per machine.
- **Teaches:** the GregGuns-owned machines: the Repair Bench, the Armory Gantry and the Turret. The Repair Bench and Turret tiers are not settled ([09 open questions](../09-machines-utility/index.md#open-questions)), so their rows carry a provisional tier that must be revisited.
- The **Laser Focus** (EV) is made in a plain GT Laser Engraver and the **Anti Gravity Core** (IV) in a plain GT Assembler ([design record 0012](../../design/0012-retire-reaction-chamber-and-retort.md)); the Reaction Chamber quests were removed with the machine.

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.mach.intro` | GregGuns Machines | LV | Most machines are plain GT. Only the Repair Bench and the Turret are GregGuns' own. | checkbox | - | `gg.lv.ammo_press` (hover), pack 97 | 562, 337 (28) |
| `gg.mach.repair_bench` | Repair Bench | MV | Mends guns and armor using the item's gate material. Tier is provisional (MV) until area 09 settles it. | retrieval `gregtech:gt.blockmachines:20110` (Repair Bench MV) | - | `gg.mach.intro`, pack 1148 | 600, 312 (24) |
| `gg.mach.repair` | Mending a Weapon | MV | Put a damaged gun and its gate material in the bench. | checkbox | - | `gg.mach.repair_bench` | 636, 312 (24) |
| `gg.mach.turret` | Turret | HV | Automatic defense. Tier is provisional (HV) until area 09 settles it. | retrieval `gregtech:gt.blockmachines:20117` (Turret HV) | - | `gg.mach.intro`, pack 160 | 600, 366 (24) |
| `gg.mach.turret_team` | Friend or Foe | HV | Turrets spare you and your allies on a GTNHLib team, and shoot hostiles. | checkbox | - | `gg.mach.turret` | 636, 348 (24) |
| `gg.mach.turret_power` | Powering the Turret | HV | The turret takes EU at its tier. Overvoltage damages it. | checkbox | - | `gg.mach.turret` | 636, 384 (24) |
| `gg.mach.gantry` | Armory Gantry | HV | The Gantry is a multiblock repair hall with a row of bays, for armor and guns in bulk. Its controller is HV and it needs an interior lamp. | retrieval `gregtech:gt.blockmachines:20114` (Armory Gantry) | - | `gg.mach.repair`, pack 160 | 672, 312 (24) |

### Armor and equipment

- **Gate:** pack 88 and `gg.start.welcome`, then tier quests.
- **Teaches:** armor slots against Baubles slots ([baubles-slots.md](../05-armor/baubles-slots.md)), hazard protection ([hazard-protection.md](../05-armor/hazard-protection.md)), oxygen gear ([oxygen-gear.md](../05-armor/oxygen-gear.md)) and the back devices ([0009](../../design/0009-gtnh-integration-defaults.md) item 2).

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.gear.intro` | Equipment Slots | LV | Armor uses armor slots. Goggles, packs and pouches use Baubles slots. | checkbox | - | `gg.lv.intro` (hover), pack 88 | 1138, 382 (28) |
| `gg.gear.medkit` | Medkit | MV | Heals you over time. Craft one before a fight. | retrieval `gregguns:medkit` | - | `gg.gear.intro`, pack 1148 | 1176, 312 (24) |
| `gg.gear.gas_mask` | Gas Mask | MV | A helmet-slot mask. GT hazards check armor slots only. | retrieval `gregguns:gas_mask` | - | `gg.gear.intro`, pack 1148, pack 1888 | 1176, 348 (24) |
| `gg.gear.hazards` | Know Your Hazards | MV | Each protective item lists the hazards it blocks. Check the tooltip. | checkbox | - | `gg.gear.gas_mask` | 1212, 348 (24) |
| `gg.gear.nvg` | Night Vision Goggles | MV | Wear in the head Baubles slot, not the helmet slot. | retrieval `gregguns:night_vision_goggles` | - | `gg.gear.intro`, pack 1148 | 1176, 384 (24) |
| `gg.gear.pouch` | Ammo Pouch | MV | Wear in the quiver Baubles slot. | retrieval `gregguns:ammo_pouch` | - | `gg.gear.nvg` | 1212, 384 (24) |
| `gg.gear.oxygen` | Oxygen Mask and Tanks | HV | For space and bad air. Needs compressed air. | retrieval `gregguns:oxygen_mask`; retrieval `gregguns:oxygen_tanks` (all) | - | `gg.gear.intro`, pack 160, pack 822, pack 824 | 1176, 420 (24) |
| `gg.gear.jump_pack` | Jump Pack | HV | Back device in the wings slot. | retrieval `gregguns:jump_pack` | - | `gg.gear.intro`, pack 160 | 1176, 456 (24) |
| `gg.gear.glider` | Glider | HV | Glide down from heights. Also in the wings slot. | retrieval `gregguns:glider` | - | `gg.gear.jump_pack` | 1212, 456 (24) |

### Ammo logistics

- **Gate:** pack 88 and `gg.start.welcome`, then tier quests.
- **Teaches:** that ammo is a production line, built in the Ammo Press and the Ammunition Works with GT parts ([ammo.md](../06-items/ammo.md)), and that electric items charge only at their own tier ([energy-electric-items.md](../02-guns/energy-electric-items.md)).

| Key | Title | Tier | Hint | Task | Reward | Prerequisites | Layout |
|---|---|---|---|---|---|---|---|
| `gg.ammo.intro` | Ammo Logistics | LV | Ammo is the real cost of a gun. Build it as production, not as crafting. | checkbox | - | `gg.lv.ammo_9mm` (hover), pack 88 | 742, 328 (28) |
| `gg.ammo.press` | Run the Press | LV | The Ammo Press makes rounds in bulk. Feed it GT parts and powder, and take the shells from its output. | retrieval `gregguns:ammo_shotgun_shell` x16 | - | `gg.ammo.intro` | 780, 312 (24) |
| `gg.ammo.magazines` | Magazines in the Press | LV | The same press makes empty magazines and fills them, so one machine covers the whole chain. | checkbox | - | `gg.ammo.press` | 816, 312 (24) |
| `gg.ammo.pipes` | Pipe It | LV | Move rounds and parts between machines with GT item pipes, so the line runs unattended. | checkbox | - | `gg.ammo.magazines`, pack 114 | 852, 312 (24) |
| `gg.ammo.depot` | Ammo Depot | LV | Stock 256 rounds in a chest: your own supply. | retrieval `gregguns:ammo_9mm_bullet` x256 | `gregguns:ammo_9mm_bullet` x64 | `gg.ammo.pipes` | 888, 312 (24) |
| `gg.ammo.explosives` | Explosives Handling | MV | Grenades and rockets need explosive dust. Store it away from the line. | checkbox | - | `gg.ammo.depot`, pack 762, pack 1598 | 924, 312 (24) |
| `gg.ammo.charging` | Charging at the Right Tier | HV | A GregGuns electric item has one tier. Only a charger or buffer of exactly that tier takes it. | checkbox | - | `gg.ammo.intro`, pack 619, pack 1082, pack 1142 | 780, 348 (24) |
| `gg.ammo.fuel` | Fuel Tanks | HV | Fuel tanks hold GT fuel. Diesel from the oil line works. | checkbox | - | `gg.ammo.explosives`, pack 553, pack 160 | 960, 312 (24) |
| `gg.ammo.advanced` | Advanced Ammunition | EV | EV ammo: advanced magazines and minigun drums. | retrieval `gregguns:mag_advanced` x8 | - | `gg.ammo.fuel`, pack 176 | 996, 312 (24) |
| `gg.ammo.works` | Ammunition Works | HV | The Ammunition Works is a multiblock press hall with high parallels. Build it when one press cannot keep up. It needs an interior lamp. | retrieval `gregtech:gt.blockmachines:20105` (Ammunition Works) | - | `gg.ammo.advanced`, pack 160 | 1032, 312 (24) |
| `gg.ammo.filling_bench` | Filling Bench (optional) | HV | An optional annex behind the packing end of the Works, built around six workbench tables. It loads magazines from the Works output. | checkbox | - | `gg.ammo.works` | 1068, 312 (24) |

## Coverage matrix

Where each mechanic is taught.

| Mechanic | Quest keys |
|---|---|
| Fire, reload, zoom | `gg.start.controls`, `gg.steam.first_blood`, `gg.lv.bolt_rifle` |
| Magazines (fill, sneak-click to empty) | `gg.start.ammo_types`, `gg.lv.magazines`, `gg.mv.smg_mags` |
| Ammo made in the Ammo Press and Works | `gg.lv.ammo_press`, `gg.lv.ammo_9mm`, `gg.ammo.press`, `gg.ammo.magazines`, `gg.ammo.pipes`, `gg.ammo.depot`, `gg.ammo.works`, `gg.ammo.filling_bench` |
| Explosives and fuel | `gg.mv.grenades`, `gg.ammo.explosives`, `gg.hv.rocket_launcher`, `gg.hv.flamethrower`, `gg.ammo.fuel` |
| Charging at the exact tier | `gg.hv.tools`, `gg.ev.laser`, `gg.ammo.charging`, `gg.iv.power_armor` |
| Repair | `gg.mach.repair_bench`, `gg.mach.repair`, `gg.mach.gantry` |
| Offhand and akimbo | `gg.mv.akimbo`, `gg.iv.deatomizer` |
| Armor sets | `gg.steam.scout_armor`, `gg.lv.combat_armor`, `gg.mv.t2_armor`, `gg.ev.t3_armor`, `gg.iv.power_armor` |
| Baubles slots | `gg.gear.intro`, `gg.gear.nvg`, `gg.gear.pouch`, `gg.gear.jump_pack` |
| Hazard protection | `gg.gear.gas_mask`, `gg.gear.hazards`, `gg.gear.oxygen` |
| Hostile factions and bosses | `gg.npc.zombies`, `gg.npc.bandits`, `gg.npc.army`, `gg.npc.steve`, `gg.npc.helicopter` |
| Infernal ranks | `gg.npc.elite`, `gg.npc.ultra`, `gg.npc.infernal` |
| Structures and their loot | `gg.site.surface`, `gg.site.bunker`, `gg.site.camp`, `gg.site.ships`, `gg.site.dungeon` |
| Turret | `gg.mach.turret`, `gg.mach.turret_team`, `gg.mach.turret_power` |
| Assembly Line recipes | `gg.luv.intro` |

## Totals

| Chapter | Quests | Checkbox | Retrieval | Crafting | Hunt | Bags |
|---|---|---|---|---|---|---|
| Start here | 5 | 5 | 0 | 0 | 0 | 0 |
| Steam / ULV | 7 | 2 | 0 | 4 | 1 | 1 |
| LV | 11 | 2 | 9 | 0 | 0 | 1 |
| MV | 10 | 2 | 8 | 0 | 0 | 1 |
| HV | 9 | 2 | 7 | 0 | 0 | 1 |
| EV | 12 | 3 | 9 | 0 | 0 | 1 |
| IV | 9 | 2 | 7 | 0 | 0 | 1 |
| LuV | 6 | 2 | 4 | 0 | 0 | 1 |
| ZPM | 5 | 2 | 3 | 0 | 0 | 1 |
| NPCs and bosses | 14 | 4 | 0 | 0 | 10 | 12 |
| Structures | 6 | 6 | 0 | 0 | 0 | 5 |
| Machines | 7 | 4 | 3 | 0 | 0 | 0 |
| Armor and equipment | 9 | 2 | 7 | 0 | 0 | 0 |
| Ammo logistics | 11 | 7 | 4 | 0 | 0 | 0 |
| **Total** | **121** | **45** | **61** | **4** | **11** | **25** |
