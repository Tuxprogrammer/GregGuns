# Beta client pass checklist (0.3.0-beta gate)

Generated from the code registries on master, not from memory. This is the checklist for the "Client pass" item in [ROADMAP § Beta review gate](../ROADMAP.md): a batched in-game session over every gun, armor set, machine, multiblock, NPC and structure, with screenshots, run with and without shaders. How to run it headless is in [beta-client-pass-runbook.md](beta-client-pass-runbook.md).

## Sources of each list

| List | Registry | Count |
|---|---|---|
| Guns | `GunDefinitions.ALL` (`com.gregguns.gun.def`); `GunCatalog` holds ammo IDs only | 43 |
| Armor sets | `ArmorCatalog` (`com.gregguns.armor.logic`), 36 camos in all | 12 |
| Accessories | `AccessoryDefs` (`com.gregguns.client.accessory`) and `FlightItems` | 8 |
| Machines, multiblocks, hatches | `GregGunsMTEIDs` (20100 to 20124 used) and [machine-roster.md](../gregguns-spec/08-machines-production/machine-roster.md) | 17, 4, 4 |
| Utility blocks | `WorldBlockRegistration`, `ChargeRegistration` | 6 |
| NPCs | `NpcMob` enum, `NpcRegistry` (entity IDs 61 to 80) | 19 |
| Structures | `StructureId` and `src/main/resources/assets/gregguns/structures/*.txt` (7 main layouts and 37 camp parts) | 9 |

Item and block renderers behind these rows: `GunItemRenderer` (guns), `ChargeRenderer`, `ChestRenderer`, `TurretRenderer`, `CampFlagRenderer`, `AmmoPressRenderer`, `BoxArmorModel`, `AccessoryModel`, `GrenadeRenderers`, `RenderBeam`, `MobRenderers`.

## How to read the tables

- **Capture** says how the screenshot is taken. **S** = a scripted scenario exists today (run it by name; see the runbook). **N** = no scenario yet, but plain commands plus the existing `[gregguns-autotest]` signals (`view`, `shot`, `hud`, `gui`, `hold`, `fire`, `creativegive`, `close`) can drive it; a small new scenario or console script is needed. **E** = only a person can do it (none are purely E; the live-eyes column carries that).
- **Live eyes** = Y when a person must judge something a still frame cannot show: sound character, animation smoothness, flicker, z-fighting that only shows while moving, FX timing. Every row's PNGs are read by a person or reviewer either way; the harness only proves files were written.
- **Passes.** Run the whole list twice: pass 1 with no shader pack (`np`), pass 2 with a shader pack active (`sh`). Group K runs in pass 2 only. A row passes when both passes pass.
- **Screenshot id** is the file stem. Final path: `shots/<pass>/<row-id>/<shot-id>.png`, with `<pass>` = `np` or `sh`. Scenario-produced names (`look-...`, `Alice-armor-...`, `acc-...`) are renamed by the collector; see the runbook.
- Failure severities for the exit gate: crash, invisible render (anything that vanishes, including water), other glitch. The first two block the beta.

## Session order

One world, creative, op, flat for A to I, a second default-type world for J. Walk the groups in this order; each group is one block of the scripted session. Group I needs hostile difficulty (`/difficulty 2`) and a cleared arena; use `/kill @e[type=!Player]` between mobs.

| Step | Group | Time per pass, scripted (estimate) |
|---|---|---|
| 1 | A. Boot and core | 5 min (boot and join) |
| 2 | B. Guns (GunDefinitions.ALL) | 12 min |
| 3 | C. Throwables and melee | 2 min |
| 4 | D. Armor sets (ArmorCatalog) | 3 min |
| 5 | E. Accessories | 3 min |
| 6 | F. Machines, single block | 8 min |
| 7 | G. Multiblocks and hatches | 15 min (building the multiblocks dominates) |
| 8 | H. Utility blocks | 3 min |
| 9 | I. NPCs (NpcMob) | 12 min |
| 10 | J. Structures (M8 worldgen) | 10 min (world gen and teleport walk) |
| 11 | K. Shader-only checks (pass 2 only) | 20 min (pass 2 only) |

## A. Boot and core (5 rows)

| Row | Subject | What to check | Screenshot ids | Capture | Live eyes |
|---|---|---|---|---|---|
| S-01 | `Boot to menu` | No crash on load with GregGuns modules on; main menu; log has no GregGuns errors | `core-menu` | S | N |
| S-02 | `Creative tab` | GregGuns tab icon, every item renders in the tab, no purple/black textures | `core-creative-tab` | S | Y |
| S-03 | `mp.give sweep` | Every registered item held once: tooltip builds, HUD and renderer run | `give-sweep (log only)` | S | N |
| S-04 | `Item icons in inventory and NEI` | Gun, ammo, armor, accessory inventory icons (flat and 3D) | `core-inventory, core-nei` | S | Y |
| S-05 | `Save and relog` | Worn armor, held gun, special slots survive a relog | `core-relog` | S | N |

## B. Guns (GunDefinitions.ALL) (43 rows)

| Row | Subject | What to check | Screenshot ids | Capture | Live eyes |
|---|---|---|---|---|---|
| G-01 | `pistol` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-pistol-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-02 | `revolver` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-revolver-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-03 | `sawed_off` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-sawed_off-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-04 | `handcannon` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-handcannon-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-05 | `bolt_action_rifle` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-bolt_action_rifle-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-06 | `thompson_smg` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-thompson_smg-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-07 | `mac10` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-mac10-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-08 | `m4` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-m4-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-09 | `ak47` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-ak47-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-10 | `combat_shotgun` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-combat_shotgun-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-11 | `akimbo_pistols` | FP both hands (position, no overlap, alternate fire); TP front; FX and sound | `look-akimbo_pistols-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-12 | `akimbo_revolvers` | FP both hands (position, no overlap, alternate fire); TP front; FX and sound | `look-akimbo_revolvers-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-13 | `rocket_launcher` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; blast FX and smoke | `look-rocket_launcher-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-14 | `grenade_launcher` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; blast FX and smoke | `look-grenade_launcher-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-15 | `as50` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-as50-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-16 | `lmg` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-lmg-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-17 | `akimbo_mac10s` | FP both hands (position, no overlap, alternate fire); TP front; FX and sound | `look-akimbo_mac10s-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-18 | `aug` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-aug-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-19 | `scar` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-scar-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-20 | `vector` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-vector-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-21 | `infiltrator` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-infiltrator-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-22 | `flamethrower` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-flamethrower-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-23 | `golden_revolver` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-golden_revolver-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-24 | `akimbo_golden_revolvers` | FP both hands (position, no overlap, alternate fire); TP front; FX and sound | `look-akimbo_golden_revolvers-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-25 | `laser_rifle` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-laser_rifle-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-26 | `tesla_rifle` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-tesla_rifle-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-27 | `sonic_rifle` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-sonic_rifle-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-28 | `pdw` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-pdw-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-29 | `pulse_rifle` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-pulse_rifle-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-30 | `minigun` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation | `look-minigun-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-31 | `blaster_rifle` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-blaster_rifle-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-32 | `deatomizer_pistol` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-deatomizer_pistol-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-33 | `akimbo_deatomizer_pistols` | FP both hands (position, no overlap, alternate fire); TP front; FX and sound; beam origin at the muzzle; translucent glow | `look-akimbo_deatomizer_pistols-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-34 | `scatter_laser_rifle` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-scatter_laser_rifle-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-35 | `bio_gun` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-bio_gun-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-36 | `power_hammer` | FP idle and swing/use; TP front; hit FX and sound; tool use on a block | `look-power_hammer-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-37 | `power_hammer_adv` | FP idle and swing/use; TP front; hit FX and sound; tool use on a block | `look-power_hammer_adv-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-38 | `chainsaw` | FP idle and swing/use; TP front; hit FX and sound; tool use on a block | `look-chainsaw-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-39 | `alien_blaster` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-alien_blaster-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-40 | `grim_reaper` | FP idle and swing/use; TP front; hit FX and sound; tool use on a block | `look-grim_reaper-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-41 | `nether_blaster` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-nether_blaster-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-42 | `nuclear_death_ray` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; beam origin at the muzzle; translucent glow | `look-nuclear_death_ray-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |
| G-43 | `nuke_launcher` | FP idle/fire (right size and position, no invisibility, no z-fighting on the model); TP front; fire FX and sound; reload animation; blast FX and smoke | `look-nuke_launcher-first-person-idle, -first-person-fire, -first-person-aim (scoped only), -third-person-front` | S | Y |

Shader-sensitive guns (the row check adds beam origin and translucent glow): akimbo_deatomizer_pistols, alien_blaster, bio_gun, blaster_rifle, deatomizer_pistol, flamethrower, laser_rifle, nether_blaster, nuclear_death_ray, pulse_rifle, scatter_laser_rifle, sonic_rifle, tesla_rifle. Blast guns: grenade_launcher, nuke_launcher, rocket_launcher. Repeat these with water in frame in group K.

Automation: `gregguns test mp.look.all` (one run for all 43; `mp.look.<gun>` for one) takes the shots per gun. Sound, FX timing and feel are judged by ear and eye in a live run, or from the AUTOTEST sound lines in the client log. The FX tags above come from the projectile class list; confirm against each `*Def` when a row looks wrong.

## C. Throwables and melee (5 rows)

| Row | Subject | What to check | Screenshot ids | Capture | Live eyes |
|---|---|---|---|---|---|
| T-01 | `stielgranate` | Held FP, throw, fuse, blast FX | `grenade-stielgranate-held, -thrown, -blast` | S | N |
| T-02 | `frag_grenade` | Held FP, throw, blast and fragments | `grenade-frag_grenade-held, -thrown, -blast` | S | N |
| T-03 | `flashbang` | Held FP, throw, screen flash and fade, no stuck white screen | `grenade-flashbang-held, -thrown, -flash` | S | Y |
| T-04 | `combat_knife` | Held FP and TP, swing | `melee-combat_knife-held, -swing` | S | N |
| T-05 | `crowbar` | Held FP and TP, swing | `melee-crowbar-held, -swing` | S | N |

## D. Armor sets (ArmorCatalog) (12 rows)

| Row | Subject | What to check | Screenshot ids | Capture | Live eyes |
|---|---|---|---|---|---|
| A-01 | `t1_combat` | Worn render, all 1 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: armor bar, no stuck overlay | `Alice-armor-t1_combat-<camo>-front/back (1 camos)` | S | Y |
| A-02 | `t1_scout` | Worn render, all 4 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: armor bar, no stuck overlay | `Alice-armor-t1_scout-<camo>-front/back (4 camos)` | S | Y |
| A-03 | `t1_miner` | Worn render, all 4 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: armor bar, no stuck overlay | `Alice-armor-t1_miner-<camo>-front/back (4 camos)` | S | Y |
| A-04 | `t2_combat` | Worn render, all 6 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: armor bar, no stuck overlay | `Alice-armor-t2_combat-<camo>-front/back (6 camos)` | S | Y |
| A-05 | `t2_commando` | Worn render, all 1 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: armor bar, no stuck overlay | `Alice-armor-t2_commando-<camo>-front/back (1 camos)` | S | Y |
| A-06 | `t2_riot` | Worn render, all 1 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: riot helmet view (no blocked view expected) | `Alice-armor-t2_riot-<camo>-front/back (1 camos)` | S | Y |
| A-07 | `t2_beret` | Worn render, all 3 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: armor bar, no stuck overlay | `Alice-armor-t2_beret-<camo>-front/back (3 camos)` | S | Y |
| A-08 | `t3_combat` | Worn render, all 3 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: night vision overlay if the helmet has it | `Alice-armor-t3_combat-<camo>-front/back (3 camos)` | S | Y |
| A-09 | `steam` | Worn render, all 4 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: steam armor tooltip and hazard overlay | `Alice-armor-steam-<camo>-front/back (4 camos)` | S | Y |
| A-10 | `t3_hev` | Worn render, all 3 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: HEV oxygen/hazard HUD | `Alice-armor-t3_hev-<camo>-front/back (3 camos)` | S | Y |
| A-11 | `t3_power` | Worn render, all 3 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: power HUD and energy readout | `Alice-armor-t3_power-<camo>-front/back (3 camos)` | S | Y |
| A-12 | `t3_exo` | Worn render, all 3 camo(s): front for all, back for custom-model sets; head/torso/legs/boots aligned, no clipping or z-fighting; held-item swing over the sleeve; HUD: power HUD, step assist | `Alice-armor-t3_exo-<camo>-front/back (3 camos)` | S | Y |

Automation: `gregguns test armor.showcase`, about 1,000 server ticks, front shots for all 36 camos plus back shots for the five custom-model sets, HUD hidden. HUD checks are a second, HUD-on pass (`hud on`, `view 0`) and need a person. Set bonuses (night vision, step height, flight) are asserted by the unit and server scenarios, not here.

## E. Accessories (8 rows)

| Row | Subject | What to check | Screenshot ids | Capture | Live eyes |
|---|---|---|---|---|---|
| X-01 | `gas_mask` | Worn TP front/back; HUD overlay (first person mask view) | `acc-gas_mask-front/back` | S | Y |
| X-02 | `tactical_mask` | 4 camos; worn TP; no helmet clipping | `acc-tactical_mask-front/back` | S | Y |
| X-03 | `night_vision_goggles` | On and off models; night vision overlay on/off; brightness under shaders | `acc-night_vision_goggles-front/back` | S | Y |
| X-04 | `oxygen_mask` | Worn TP; oxygen HUD | `acc-oxygen_mask-front/back` | S | Y |
| X-05 | `oxygen_tanks` | Worn TP back; tank gauge | `acc-oxygen_tanks-front/back` | S | Y |
| X-06 | `jump_pack` | Worn TP back; jump use and flame FX | `acc-jump_pack-front/back` | S | Y |
| X-07 | `anti_gravity_pack` | 5 camos; worn TP back; hover FX | `acc-anti_gravity_pack-front/back` | S | Y |
| X-08 | `glider` | Stowed on the ground; wings out while falling; wing symmetry | `acc-glider-front/back` | S | Y |

Automation: `gregguns test armor.showcase.accessories`. Items in Galacticraft slots are skipped with a note.

## F. Machines, single block (17 rows)

| Row | Subject | What to check | Screenshot ids | Capture | Live eyes |
|---|---|---|---|---|---|
| M-01 | `ammo_press_steam (MTE 20100)` | Animated front overlay while crafting; GUI (slots, progress); NEI recipe page for gregguns.ammoPress; place, rotate, break; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-ammo_press_steam-block, -gui, -nei, -tooltip` | N | Y |
| M-02 | `ammo_press_lv (MTE 20101)` | Animated front overlay while crafting; GUI (slots, progress); NEI recipe page for gregguns.ammoPress; place, rotate, break; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-ammo_press_lv-block, -gui, -nei, -tooltip` | S | Y |
| M-03 | `ammo_press_mv (MTE 20102)` | Animated front overlay while crafting; GUI (slots, progress); NEI recipe page for gregguns.ammoPress; place, rotate, break; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-ammo_press_mv-block, -gui, -nei, -tooltip` | N | Y |
| M-04 | `ammo_press_hv (MTE 20103)` | Animated front overlay while crafting; GUI (slots, progress); NEI recipe page for gregguns.ammoPress; place, rotate, break; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-ammo_press_hv-block, -gui, -nei, -tooltip` | N | Y |
| M-05 | `ammo_press_ev (MTE 20104)` | Animated front overlay while crafting; GUI (slots, progress); NEI recipe page for gregguns.ammoPress; place, rotate, break; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-ammo_press_ev-block, -gui, -nei, -tooltip` | N | Y |
| M-06 | `repair_bench_lv (MTE 20109)` | GUI; repair a worn armor piece and a gun; tier gate message; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-repair_bench_lv-block, -gui, -nei, -tooltip` | N | Y |
| M-07 | `repair_bench_mv (MTE 20110)` | GUI; repair a worn armor piece and a gun; tier gate message; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-repair_bench_mv-block, -gui, -nei, -tooltip` | N | Y |
| M-08 | `repair_bench_hv (MTE 20111)` | GUI; repair a worn armor piece and a gun; tier gate message; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-repair_bench_hv-block, -gui, -nei, -tooltip` | N | Y |
| M-09 | `repair_bench_ev (MTE 20112)` | GUI; repair a worn armor piece and a gun; tier gate message; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-repair_bench_ev-block, -gui, -nei, -tooltip` | N | Y |
| M-10 | `repair_bench_iv (MTE 20113)` | GUI; repair a worn armor piece and a gun; tier gate message; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-repair_bench_iv-block, -gui, -nei, -tooltip` | N | Y |
| M-11 | `camo_bench (MTE 20115)` | GUI; cycle camo on a gun, armor piece and camo net; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-camo_bench-block, -gui, -nei, -tooltip` | N | Y |
| M-12 | `turret_mv (MTE 20116)` | Base and mounted turret entity render; rotation to target; GUI (gun, ammo, redstone mode); turret plate item; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-turret_mv-block, -gui, -nei, -tooltip` | N | Y |
| M-13 | `turret_hv (MTE 20117)` | Base and mounted turret entity render; rotation to target; GUI (gun, ammo, redstone mode); turret plate item; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-turret_hv-block, -gui, -nei, -tooltip` | N | Y |
| M-14 | `turret_ev (MTE 20118)` | Base and mounted turret entity render; rotation to target; GUI (gun, ammo, redstone mode); turret plate item; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-turret_ev-block, -gui, -nei, -tooltip` | N | Y |
| M-15 | `turret_iv (MTE 20119)` | Base and mounted turret entity render; rotation to target; GUI (gun, ammo, redstone mode); turret plate item; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-turret_iv-block, -gui, -nei, -tooltip` | N | Y |
| M-16 | `reaction_chamber_ev (MTE 20106)` | ONLY if still present (the roadmap retires it at this gate): GUI, FX, NEI; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-reaction_chamber_ev-block, -gui, -nei, -tooltip` | N | Y |
| M-17 | `reaction_chamber_iv (MTE 20107)` | ONLY if still present (the roadmap retires it at this gate): GUI, FX, NEI; tooltip contains "Added by: GregGuns"; NEI search finds it | `machine-reaction_chamber_iv-block, -gui, -nei, -tooltip` | N | Y |

Gap: no scenario yet screenshots a placed MTE. `mp.visual` covers the LV Ammo Press GUI and NEI only. The "Added by: GregGuns" tooltip is asserted by `machine.addedby` (server) and `mp.nei_addedby` (client NEI search); the other tiers need the shots. Place a machine with `/give @p gregtech:gt.blockmachines 1 <MTE id>` then right-click, or place it server-side with the `place()` helper the repair scenarios use. The Reaction Chamber rows are conditional because the roadmap retires it at this gate.

## G. Multiblocks and hatches (8 rows)

| Row | Subject | What to check | Screenshot ids | Capture | Live eyes |
|---|---|---|---|---|---|
| MB-01 | `Ammunition Works (MTE 20105)` | Build per the StructureLib hologram; formation (controller turns active); glass side windows; press-line interior (frame rods, pipes, gearboxes, assembler arch) renders and animates while running; hatches accept; no z-fighting on casing/glass; NEI page; controller tooltip "Added by: GregGuns" | `mb-ammo_works-hologram, -formed-outside, -formed-inside, -running, -nei` | N | Y |
| MB-02 | `Armory Gantry (MTE 20114)` | Formation; hoist, floor rails, pipes inside the portals render; bulk repair run; NEI page; controller tooltip "Added by: GregGuns" | `mb-armory_gantry-hologram, -formed-outside, -formed-inside, -running, -nei` | N | Y |
| MB-03 | `Rampart (MTE 20120)` | Formation; crenellated wall; up to ten mounts; Gun Mount Hatch gun renders on the wall; turret fires; ammo and power feed; controller tooltip "Added by: GregGuns" | `mb-rampart-hologram, -formed-outside, -formed-inside, -running, -nei` | N | Y |
| MB-04 | `Retort (MTE 20108)` | ONLY if still present (the roadmap retires it at this gate): formation, pump/dip tube/coils interior, GUI; controller tooltip "Added by: GregGuns" | `mb-retort-hologram, -formed-outside, -formed-inside, -running, -nei` | N | Y |
| MB-05 | `gun_mount_hatch_mv (MTE 20121)` | Places in a Rampart; holds a gun; render of the mounted gun; tooltip "Added by: GregGuns" | `mb-gun_mount_hatch_mv-block, -mounted` | N | Y |
| MB-06 | `gun_mount_hatch_hv (MTE 20122)` | Places in a Rampart; holds a gun; render of the mounted gun; tooltip "Added by: GregGuns" | `mb-gun_mount_hatch_hv-block, -mounted` | N | Y |
| MB-07 | `gun_mount_hatch_ev (MTE 20123)` | Places in a Rampart; holds a gun; render of the mounted gun; tooltip "Added by: GregGuns" | `mb-gun_mount_hatch_ev-block, -mounted` | N | Y |
| MB-08 | `gun_mount_hatch_iv (MTE 20124)` | Places in a Rampart; holds a gun; render of the mounted gun; tooltip "Added by: GregGuns" | `mb-gun_mount_hatch_iv-block, -mounted` | N | Y |

Gap: no scenario builds a multiblock and photographs it. Existing machine and utility scenarios assert formation on the server. Build with the StructureLib hologram, or have a scenario call the controller's construct helper, then signal `view 2` and `shot`. Interior shots need a camera inside the structure: `/tp` into the interior, or shoot through the glass. The Retort row is conditional.

## H. Utility blocks (6 rows)

| Row | Subject | What to check | Screenshot ids | Capture | Live eyes |
|---|---|---|---|---|---|
| U-01 | `explosive_charge` | TNT charge: place on wall/floor, GUI unarmed/armed/non-owner, fuse, blast  (existing: utility.charge_gui_shots, mp.look.charge) | `util-explosive_charge-<view>` | S | Y |
| U-02 | `advanced_explosive_charge` | Advanced charge: same, plus C4 look  (existing: utility.charge_gui_shots, mp.look.charge) | `util-advanced_explosive_charge-<view>` | S | Y |
| U-03 | `weapon_chest` | Closed/open, inventory item render, GUI  (existing: mp.look.chest) | `util-weapon_chest-<view>` | S | Y |
| U-04 | `reinforced_chest` | Closed/open, inventory item render, GUI, ownership  (existing: mp.look.chest) | `util-reinforced_chest-<view>` | S | Y |
| U-05 | `camp_flag` | Flag render (pole, cloth, no z-fighting); interaction  (existing: none) | `util-camp_flag-<view>` | N | Y |
| U-06 | `bio_blob` | Block render, growth sizes  (existing: none) | `util-bio_blob-<view>` | N | Y |

## I. NPCs (NpcMob) (19 rows)

| Row | Subject | What to check | Screenshot ids | Capture | Live eyes |
|---|---|---|---|---|---|
| N-01 | `ZombieSoldier (entity id 61)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-ZombieSoldier-idle, -attack, -egg` | N | Y |
| N-02 | `ZombieFarmer (entity id 62)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-ZombieFarmer-idle, -attack, -egg` | N | Y |
| N-03 | `ZombieMiner (entity id 63)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-ZombieMiner-idle, -attack, -egg` | N | Y |
| N-04 | `ZombiePigmanSoldier (entity id 64)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-ZombiePigmanSoldier-idle, -attack, -egg` | N | Y |
| N-05 | `AttackHelicopter (entity id 65)` | Egg spawn; rotor spin; flight; fires; boss bar; crash/death FX | `npc-AttackHelicopter-idle, -attack, -egg` | N | Y |
| N-06 | `AlienBug (entity id 66)` | Egg spawn; model; melee attack animation; death | `npc-AlienBug-idle, -attack, -egg` | N | Y |
| N-07 | `CyberDemon (entity id 67)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body; boss bar if any | `npc-CyberDemon-idle, -attack, -egg` | N | Y |
| N-08 | `ArmySoldier (entity id 68)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-ArmySoldier-idle, -attack, -egg` | N | Y |
| N-09 | `Bandit (entity id 69)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-Bandit-idle, -attack, -egg` | N | Y |
| N-10 | `Outcast (entity id 70)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-Outcast-idle, -attack, -egg` | N | Y |
| N-11 | `SkeletonSoldier (entity id 71)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-SkeletonSoldier-idle, -attack, -egg` | N | Y |
| N-12 | `PsychoSteve (entity id 73)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body; boss bar if any | `npc-PsychoSteve-idle, -attack, -egg` | N | Y |
| N-13 | `DictatorDave (entity id 74)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body; boss bar if any | `npc-DictatorDave-idle, -attack, -egg` | N | Y |
| N-14 | `Commando (entity id 75)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-Commando-idle, -attack, -egg` | N | Y |
| N-15 | `Stormtrooper (entity id 76)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-Stormtrooper-idle, -attack, -egg` | N | Y |
| N-16 | `SuperMutantBasic (entity id 77)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-SuperMutantBasic-idle, -attack, -egg` | N | Y |
| N-17 | `SuperMutantHeavy (entity id 78)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-SuperMutantHeavy-idle, -attack, -egg` | N | Y |
| N-18 | `SuperMutantElite (entity id 79)` | Egg in the creative tab with colors; egg spawns it on a block face; idle render (model, held gun, armor, size); attack animation, gun FX and sound; death; no invisible body | `npc-SuperMutantElite-idle, -attack, -egg` | N | Y |
| N-19 | `SimpleTurret (entity id 72, no egg)` | Spawn with /summon gregguns.SimpleTurret; render; targets a hostile; faction | `npc-SimpleTurret-idle, -attack` | N | Y |

Existing server scenarios (`npc.egg_spawns`, `npc.all_eggs`, `npc.*_fires`) assert spawn, stats and fire on the server only; none takes a screenshot. Spawn with the egg from the creative tab (damage value = entity ID) or `/give @p gregguns:npc_egg 1 <entity id>`, then right-click a block. The attack animation needs the NPC to target the player: use a second player or `npc.soak` conditions. Run the opt-in `npc.soak` (about 2 minutes of game time) once for crash detection.

## J. Structures (M8 worldgen) (9 rows)

| Row | Subject | What to check | Screenshot ids | Capture | Live eyes |
|---|---|---|---|---|---|
| W-01 | `Small Factory Building` | Generates naturally in a default world type; terrain shaping, no floating blocks; chests hold loot of the right tier; spawners and NPCs present; interior lighting; no z-fighting between layered blocks | `struct-factory_small-exterior, -interior, -loot` | N | Y |
| W-02 | `Small Train Station` | Generates naturally in a default world type; terrain shaping, no floating blocks; chests hold loot of the right tier; spawners and NPCs present; interior lighting; no z-fighting between layered blocks | `struct-train_station-exterior, -interior, -loot` | N | Y |
| W-03 | `Gas Station` | Generates naturally in a default world type; terrain shaping, no floating blocks; chests hold loot of the right tier; spawners and NPCs present; interior lighting; no z-fighting between layered blocks | `struct-gas_station-exterior, -interior, -loot` | N | Y |
| W-04 | `Medium House` | Generates naturally in a default world type; terrain shaping, no floating blocks; chests hold loot of the right tier; spawners and NPCs present; interior lighting; no z-fighting between layered blocks | `struct-house_medium-exterior, -interior, -loot` | N | Y |
| W-05 | `Big Bunker (2 spawners)` | Generates naturally in a default world type; terrain shaping, no floating blocks; chests hold loot of the right tier; spawners and NPCs present; interior lighting; no z-fighting between layered blocks | `struct-big_bunker-exterior, -interior, -loot` | N | Y |
| W-06 | `Aircraft Carrier (flag, ocean)` | Generates naturally in a default world type; terrain shaping, no floating blocks; chests hold loot of the right tier; spawners and NPCs present; interior lighting; no z-fighting between layered blocks | `struct-aircraft_carrier-exterior, -interior, -loot` | N | Y |
| W-07 | `Submarine (ocean)` | Generates naturally in a default world type; terrain shaping, no floating blocks; chests hold loot of the right tier; spawners and NPCs present; interior lighting; no z-fighting between layered blocks | `struct-submarine-exterior, -interior, -loot` | N | Y |
| W-08 | `Military Base` | Generates naturally; camps along roads (camp parts: barracks, small and closed bunkers, containers, factory, helipad, props, tank farm, tents, towers; shoot one of each); loot; NPC camp spawn; camp flag | `struct-military_base-exterior, -interior, -loot` | N | Y |
| W-09 | `Alien Bug Nest` | Generates naturally; nest render; bugs present (placed by code, no layout file) | `struct-alien_bug_nest-exterior, -interior, -loot` | N | Y |

Existing scenarios `worldgen.*_places`, `worldgen.generator_test_layout`, `worldgen.loot_ceiling` and `worldgen.no_cascade` assert placement, loot ceiling and no chunk cascade on the server. They take no screenshots. For this pass: create a fresh world of type `default` (flat is excluded by `structureWorldTypes`), set the worldgen config grid sizes (`spacingSmall`, `spacingMedium`, `spacingBig`, category `worldgen`) to 2 to 4 so structures are dense, `/tp` along a grid line, and photograph each. Naval structures need ocean. The Alien Bug Nest is placed by code, not a layout file; check that it generates at all.

## K. Shader-only checks (pass 2 only) (11 rows)

| Row | Subject | What to check | Screenshot ids | Capture | Live eyes |
|---|---|---|---|---|---|
| SH-01 | `Water near beam FX` | Stand beside and in water; fire each beam gun (laser, tesla, sonic, deatomizer, blaster, alien, nether, nuclear death ray, bio gun, flamethrower) across a pool: water stays visible, nothing turns invisible (known 0.2.0 bug class) | `sh-water-<gun>` | N | Y |
| SH-02 | `Water under blast` | Rocket, grenade and nuke launcher blasts over and under water; water and sky still draw afterward | `sh-water-blast-<gun>` | N | Y |
| SH-03 | `Beam origin` | Beams start at the muzzle in first and third person, both hands for akimbo, with the pack's depth of field and motion blur on | `sh-beam-origin-<gun>` | N | Y |
| SH-04 | `Translucent FX` | Muzzle flash, tracers, laser core, sonic ring, flames and smoke draw additive or translucent, with correct depth against glass and water, no black quads | `sh-fx-<gun>` | N | Y |
| SH-05 | `Held item under shadows` | Guns held first person: no flicker, no missing model in the shader hand pass; shadow cast in third person | `sh-hand-<gun>` | N | Y |
| SH-06 | `Armor and accessories under shaders` | Worn sets and accessories (night vision overlay, glider wings) keep texture and state | `sh-armor-<set>` | N | Y |
| SH-07 | `Machines and multiblocks under shaders` | Animated Ammo Press overlay; multiblock glass windows; interior props | `sh-machine-<name>` | N | Y |
| SH-08 | `NPCs under shaders` | Mob models, helicopter rotor, mutant models | `sh-npc-<name>` | N | Y |
| SH-09 | `GL state leakage` | After 100 shots, F3 and the next-frame look show no state drift; HUD, chat and NEI render normally | `sh-gl-state` | N | Y |
| SH-10 | `Scope overlay and HUD caching` | Scoped guns with enableHudCaching on: overlay moves at full frame rate; the Angelica zoom key stacks | `sh-scope-<gun>` | N | Y |
| SH-11 | `Shader toggle in world` | Toggle the pack with a gun in hand and armor on, then toggle back: no crash, nothing stays invisible | `sh-toggle` | N | Y |

## Summary

| Category | Rows |
|---|---|
| A. Boot and core | 5 |
| B. Guns (GunDefinitions.ALL) | 43 |
| C. Throwables and melee | 5 |
| D. Armor sets (ArmorCatalog) | 12 |
| E. Accessories | 8 |
| F. Machines, single block | 17 |
| G. Multiblocks and hatches | 8 |
| H. Utility blocks | 6 |
| I. NPCs (NpcMob) | 19 |
| J. Structures (M8 worldgen) | 9 |
| K. Shader-only checks (pass 2 only) | 11 |
| **Total** | **143** |

Capture split by row: **S (scripted today) 78**, **N (commands or a small new scenario) 65**, **E (eyes only) 0**. Rows that also need live eyes beyond reading the PNG: 136 of 143.

Screenshots: pass 1 is about 330 files, pass 2 repeats it and adds group K.
