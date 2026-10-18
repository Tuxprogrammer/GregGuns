# 02-guns: entity sheets index and shared rules

The 43 guns and the 2 registered grenades (plus the unregistered Flashbang) each have a full entity sheet with the ten fields of the [conventions](../CONVENTIONS.md#entity-sheet). The sheets are split over four files by family. [gun-catalog.md](gun-catalog.md) stays the quick-reference table set; the sheets restate its numbers per gun and add what the tables do not hold (obtaining, tooltips, sound events, relationships, bug lists). Where a sheet and a catalog table disagree the sheet was checked against [`TGuns.java`](gun-catalog.md) again, and any corrected table cell is listed in the [index](index.md#coverage).

## Sheet files

| File | Contents |
|---|---|
| [sheets-handguns-smgs.md](sheets-handguns-smgs.md) | Hand Cannon, 9mm Pistol, Submachine Gun, Revolver, Golden Revolver, Mac10, Vector and the four non-energy akimbo guns (11 sheets) |
| [sheets-rifles-shotguns.md](sheets-rifles-shotguns.md) | Sawed Off, Combat Shotgun, Bolt Action Rifle, M4, The Infiltrator, Kalaschnikow, AUG, SCAR, AS50, PDW, Pulse Rifle, LMG, Minigun (13 sheets) |
| [sheets-energy-special.md](sheets-energy-special.md) | Flamethrower, Laser Rifle, Scatter Laser Rifle, Blaster Rifle, Tesla Rifle, Nuclear Death Ray, Alien Blaster, Nether Blaster, De-Atomizer Pistol and its akimbo version, Sonic Rifle, Bio Gun (12 sheets) |
| [sheets-launchers-tools-grenades.md](sheets-launchers-tools-grenades.md) | Rocket Launcher, Grim Reaper, Nuke Launcher, Grenade Launcher, Power Hammer (both), Chainsaw, Stielgranate, Frag Grenade, Flashbang (10 sheets) |
| [lang.md](lang.md) | Every lang key, tooltip string and death message of this area, with the missing and stray ones |

## All items at a glance

| # | Item | Registry name | Class | Sheet | Ammo | Clip | Damage | Obtained |
|---|---|---|---|---|---|---|---|---|
| 1 | Hand Cannon | [`handcannon`](gun-catalog.md#table-a-identity-and-ammunition) | [`Handgun`](sheets-handguns-smgs.md#hand-cannon-handcannon) | [sheet](sheets-handguns-smgs.md#hand-cannon-handcannon) | Stone Bullet | 1 | 8.0-5.0 | crafted (recipe 1), loot, NPC-held |
| 2 | 9mm Pistol | [`pistol`](gun-catalog.md#table-a-identity-and-ammunition) | [`GenericGun`](gun-lifecycle.md) | [sheet](sheets-handguns-smgs.md#9mm-pistol-pistol) | 9mm Pistol Magazine | 18 | 8.0-5.0 | crafted (recipe 2), loot, NPC-held |
| 3 | Sawed Off | [`sawedoff`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#sawed-off-sawedoff) | 2x Shotgun Shell | 2 | 4.0-1.5 | crafted (recipe 4), loot, NPC-held |
| 4 | Bolt Action Rifle | [`boltaction`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#bolt-action-rifle-boltaction) | Rifle Bullets | 6 | 16.0-10.0 | crafted (recipe 7), loot, NPC-held |
| 5 | M4 Assault Rifle | `m4` | `GenericGun` | [sheet](sheets-rifles-shotguns.md#m4-assault-rifle-m4) | Assault Rifle Magazine | 30 | 8.0-6.0 | crafted (recipe 6), loot, NPC-held |
| 6 | Flamethrower | [`flamethrower`](gun-catalog.md#table-a-identity-and-ammunition) | [`Flamethrower`](sheets-energy-special.md#flamethrower-flamethrower) | [sheet](sheets-energy-special.md#flamethrower-flamethrower) | Fuel Tank | 100 | 5.0-2.0 | crafted (recipe 9), loot, NPC-held |
| 7 | Combat Shotgun | [`combatshotgun`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#combat-shotgun-combatshotgun) | 8x Shotgun Shell | 8 | 4.0-1.5 | crafted (recipe 8), loot, NPC-held |
| 8 | Laser Rifle | [`lasergun`](gun-catalog.md#table-a-identity-and-ammunition) | [`Lasergun`](sheets-energy-special.md#laser-rifle-lasergun) | [sheet](sheets-energy-special.md#laser-rifle-lasergun) | Energy Cell | 45 | 12.0 | crafted (recipe 11), NPC-held |
| 9 | Rocket Launcher | [`rocketlauncher`](gun-catalog.md#table-a-identity-and-ammunition) | `RocketLauncher` | [sheet](sheets-launchers-tools-grenades.md#rocket-launcher-rocketlauncher) | Rocket | 1 | 50.0 | crafted (recipe 15), loot, NPC-held |
| 10 | Grim Reaper | [`grimreaper`](gun-catalog.md#table-a-identity-and-ammunition) | `RocketLauncher` | [sheet](sheets-launchers-tools-grenades.md#grim-reaper-grimreaper) | 4x Rocket | 4 | 50.0 | crafted (upgrade recipe 23), NPC-held |
| 11 | Bio Gun | [`biogun`](gun-catalog.md#table-a-identity-and-ammunition) | [`Biogun`](sheets-energy-special.md#bio-gun-biogun) | [sheet](sheets-energy-special.md#bio-gun-biogun) | Bio Tank | 30 | 10.0-8.0 | crafted (recipe 10), NPC-held |
| 12 | Submachine Gun (Thompson) | [`thompson`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-handguns-smgs.md#submachine-gun-thompson-thompson) | SMG Magazine | 20 | 5.0-3.0 | crafted (recipe 5), loot, NPC-held |
| 13 | Revolver | [`revolver`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-handguns-smgs.md#revolver-revolver) | 9mm Bullets | 6 | 8.0-6.0 | crafted (recipe 3), loot, NPC-held |
| 14 | Kalaschnikow (AK47) | [`ak47`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#kalaschnikow-ak47-ak47) | Assault Rifle Magazine | 30 | 9.0-5.0 | crafted (recipe 13), loot, NPC-held |
| 15 | AS50 Sniper Rifle | [`as50`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#as50-sniper-rifle-as50) | AS50 Magazine | 10 | 32.0-24.0 | crafted (recipe 14), NPC-held |
| 16 | Tesla Rifle | [`teslagun`](gun-catalog.md#table-a-identity-and-ammunition) | [`Teslagun`](index.md#coverage) | [sheet](sheets-energy-special.md#tesla-rifle-teslagun) | Energy Cell | 25 | 12.0 | crafted (recipe 21), NPC-held |
| 17 | PDW | [`pdw`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#pdw-pdw) | Advanced Magazine | 20 | 6.0-3.0 | crafted (recipe 18) |
| 18 | LMG | [`lmg`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#lmg-lmg) | LMG Magazine | 100 | 8.0-6.0 | crafted (recipe 16), NPC-held |
| 19 | The Infiltrator | [`m4_uq`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#the-infiltrator-m4_uq) | Assault Rifle Magazine | 30 | 8.0-6.0 | crafted (upgrade recipe 24), NPC-held |
| 20 | Golden Revolver | [`goldenrevolver`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-handguns-smgs.md#golden-revolver-goldenrevolver) | 9mm Bullets | 6 | 14.0-6.0 | crafted (upgrade recipe 22), NPC-held, special NPC drop |
| 21 | Pulse Rifle | [`pulserifle`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#pulse-rifle-pulserifle) | Advanced Magazine | 12 | 10.0-8.0 | crafted (recipe 19) |
| 22 | Alien Blaster | [`alienBlaster`](gun-catalog.md#table-a-identity-and-ammunition) | [`AlienBlaster`](sheets-energy-special.md#alien-blaster-alienblaster) | [sheet](sheets-energy-special.md#alien-blaster-alienblaster) | Energy Cell | 10 | 16.0 | creative / commands only |
| 23 | Nether Blaster | [`cyberdemonBlaster`](gun-catalog.md#table-a-identity-and-ammunition) | [`CyberdemonBlaster`](sheets-energy-special.md#nether-blaster-cyberdemonblaster) | [sheet](sheets-energy-special.md#nether-blaster-cyberdemonblaster) | Nether Charge | 10 | 14.0-8.0 | crafted (recipe 25), NPC-held, special NPC drop |
| 24 | Minigun | [`minigun`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#minigun-minigun) | Minigun Ammo Drum | 200 | 5.0-3.0 | crafted (recipe 17), NPC-held |
| 25 | Blaster Rifle | [`blasterRifle`](gun-catalog.md#table-a-identity-and-ammunition) | [`BlasterRifle`](sheets-energy-special.md#blaster-rifle-blasterrifle) | [sheet](sheets-energy-special.md#blaster-rifle-blasterrifle) | Energy Cell | 50 | 10.0-8.0 | crafted (recipe 12), NPC-held |
| 26 | Power Hammer | [`powerHammer`](gun-catalog.md#table-a-identity-and-ammunition) | [`PowerHammer`](sheets-launchers-tools-grenades.md#power-hammer-powerhammer) | [sheet](sheets-launchers-tools-grenades.md#power-hammer-powerhammer) | Compressed Air Tank | 300 | 2.5 x air | crafted (recipe 20) |
| 27 | Nuke Launcher | [`nukelauncher`](gun-catalog.md#table-a-identity-and-ammunition) | [`NukeLauncher`](index.md#coverage) | [sheet](sheets-launchers-tools-grenades.md#nuke-launcher-nukelauncher) | Rocket (Tactical Nuke) | 1 | 250.0 | crafted (recipe 30) |
| 28 | Power Hammer (Advanced) | [`powerHammerAdv`](gun-catalog.md#table-a-identity-and-ammunition) | `PowerHammer` | [sheet](sheets-launchers-tools-grenades.md#power-hammer-advanced-powerhammeradv) | Compressed Air Tank | 300 | 2.5 x air | crafted (shapeless upgrade) |
| 29 | Grenade Launcher | [`grenadelauncher`](gun-catalog.md#table-a-identity-and-ammunition) | [`GrenadeLauncher`](index.md#coverage) | [sheet](sheets-launchers-tools-grenades.md#grenade-launcher-grenadelauncher) | 6x 40mm Grenade | 6 | 30.0 | crafted (recipe 27) |
| 30 | AUG | [`aug`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#aug-aug) | Assault Rifle Magazine | 30 | 8.0-7.0 | crafted (recipe 26) |
| 31 | Sonic Rifle | [`sonicShotgun`](gun-catalog.md#table-a-identity-and-ammunition) | [`SonicShotgun`](index.md#coverage) | [sheet](sheets-energy-special.md#sonic-rifle-sonicshotgun) | Energy Cell | 8 | 25.0-5.0 | crafted (recipe 29) |
| 32 | Chainsaw | [`chainsaw`](gun-catalog.md#table-a-identity-and-ammunition) | [`Chainsaw`](sheets-launchers-tools-grenades.md#chainsaw-chainsaw) | [sheet](sheets-launchers-tools-grenades.md#chainsaw-chainsaw) | Fuel Tank | 300 | 10.0 | crafted (recipe 28), NPC-held, special NPC drop |
| 33 | Scatter Laser Rifle [WIP] | [`scatterBeamGun`](gun-catalog.md#table-a-identity-and-ammunition) | `Lasergun` | [sheet](sheets-energy-special.md#scatter-laser-rifle-wip-scatterbeamgun) | Energy Cell | 40 | 6.0 | creative / commands only |
| 34 | Nuclear Death Ray | `beamtestgun` | [`Beamgun`](sheets-energy-special.md#nuclear-death-ray-nucleardeathray) | [sheet](sheets-energy-special.md#nuclear-death-ray-nucleardeathray) | Nuclear Power Cell | 100 | 6.0-1.0 | crafted (recipe 31) |
| 35 | Mac10 | [`mac10`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-handguns-smgs.md#mac10-mac10) | SMG Magazine | 32 | 5.0-3.0 | crafted (recipe 32), loot |
| 36 | Akimbo Pistols | [`pistolAkimbo`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-handguns-smgs.md#akimbo-pistols-pistolakimbo) | 9mm Pistol Magazine | 36 | 8.0-5.0 | crafted (combine two singles) |
| 37 | Akimbo Revolvers | [`revolverAkimbo`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-handguns-smgs.md#akimbo-revolvers-revolverakimbo) | 9mm Bullets | 12 | 8.0-6.0 | crafted (combine two singles) |
| 38 | Akimbo Golden Revolvers | [`goldenrevolverAkimbo`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-handguns-smgs.md#akimbo-golden-revolvers-goldenrevolverakimbo) | 9mm Bullets | 12 | 16.0-6.0 | crafted (combine two singles) |
| 39 | Akimbo Mac10s | [`mac10Akimbo`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-handguns-smgs.md#akimbo-mac10s-mac10akimbo) | SMG Magazine | 64 | 5.0-3.0 | crafted (combine two singles) |
| 40 | De-Atomizer Pistol | [`mibGun`](gun-catalog.md#table-a-identity-and-ammunition) | [`MibGun`](sheets-energy-special.md#de-atomizer-pistol-mibgun) | [sheet](sheets-energy-special.md#de-atomizer-pistol-mibgun) | Energy Cell | 20 | 16.0-8.0 | crafted (recipe 35) |
| 41 | SCAR | [`scar`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-rifles-shotguns.md#scar-scar) | Assault Rifle Magazine | 20 | 12.0-10.0 | crafted (recipe 33) |
| 42 | Vector | [`vector`](gun-catalog.md#table-a-identity-and-ammunition) | `GenericGun` | [sheet](sheets-handguns-smgs.md#vector-vector) | SMG Magazine | 25 | 6.0-4.0 | crafted (recipe 34) |
| 43 | Akimbo De-Atomizer Pistols | [`mibGunAkimbo`](gun-catalog.md#table-a-identity-and-ammunition) | `MibGun` | [sheet](sheets-energy-special.md#akimbo-de-atomizer-pistols-mibgunakimbo) | Energy Cell | 40 | 16.0-8.0 | crafted (combine two singles) |
| - | Stielgranate | [`stielGranate`](gun-catalog.md#table-f-grenades-not-genericgun) | [`GenericGrenade`](per-gun-behavior.md#grenades-genericgrenade-fraggrenade-flashbang) | [sheet](sheets-launchers-tools-grenades.md#stielgranate-stielgranate) | itself | - | 20.0 | crafted (16 per craft), loot |
| - | Frag Grenade | [`fragGrenade`](gun-catalog.md#table-f-grenades-not-genericgun) | [`FragGrenade`](per-gun-behavior.md#grenades-genericgrenade-fraggrenade-flashbang) | [sheet](sheets-launchers-tools-grenades.md#frag-grenade-fraggrenade) | itself | - | 25.0 | crafted (16 per craft), loot |
| - | Flashbang | not registered | [`Flashbang`](sheets-launchers-tools-grenades.md#flashbang-flashbang) | [sheet](sheets-launchers-tools-grenades.md#flashbang-flashbang) | itself | - | 20.0 | unobtainable |

Damage cells show maximum-minimum; tool and charge guns, rockets and the Grenade Launcher have special damage rules (see their sheets). "NPC-held" means the gun appears in an NPC equipment table of [10-entities-npcs](../10-entities-npcs/per-mob.md).

## How the sheets are built

- **Numbers** are recomputed from the builder chain in `TGuns.init` after every setter, not copied from the catalog. Fixed values forced by a class (for example the Hand Cannon speed or the Tesla Rifle time-to-live) are shown in the cell together with the config value that they override.
- **Tooltips** are produced by running the code's own rules: float values use 32-bit arithmetic (so, for example, a 15% penetration prints as `15.000001%` if the product is not exactly representable; the sheets show the value the arithmetic gives). Color codes are described in words. The tool-class order of the tool guns comes from a Java `HashMap` and is not defined.
- **Audio** rows give the event, the exact call that plays it, the volume and pitch the code passes, and the measured files behind the event from [ASSET-FACTS.md](../ASSET-FACTS.md): number of files, duration range, sample rate, channels and the `sounds.json` category. Nothing here describes how a clip sounds, because the audio cannot be listened to in this environment.
- **Visuals** rows give each texture with its measured size, average color and dominant colors; the full per-texture description is in [03-gun-visuals-audio/textures.md](../03-gun-visuals-audio/textures.md).
- **NPC equipment** probabilities come from the `switch (random.nextInt(n))` blocks of each NPC class; "1 of 6" means one of six equally likely cases. The drop rules are in [10-entities-npcs/per-mob.md](../10-entities-npcs/per-mob.md#drops).

## Shared bugs

The sheets list these only by ID; this table says what each one means for a gun. The full rows are in the [index](index.md#suspected-bugs).

| ID | Applies to | Effect on the gun |
|---|---|---|
| `GUN-01` | every gun (camo switch) | A camo switch on a stack without NBT creates a tag holding only `camo`, so the gun then reads as empty. |
| `GUN-06` | guns carried by [`GenericNPC`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava) subclasses | The difficulty accuracy factor is applied twice, so NPC spread is much larger than the nominal value. |
| `GUN-07` | scoped guns with a zoom bonus below 1 | NPCs always get the zoomed-spread bonus. |
| `GUN-11` | full-auto left-click guns | With the trigger held and no ammo, the out-of-ammo sound plays every tick. |
| `GUN-12` | scoped guns | The server trusts the client zoom flag. |
| `GUN-13` | every gun (forced reload) | Remaining rounds are discarded without refund (magazine-less guns), or the empty container is returned even if the cell was partly full. |
| `GUN-17` | every gun (config) | A shortened `WeaponStats_...` list crashes at load. |
| `GUN-18` | every gun (tooltip, config) | The third `Range` value and the `TimeToLive` comment call ticks a distance. |
| `GUN-19` | every gun (tooltip) | The short tooltip always prints `max-min`, even for equal values. |
| `GUN-29` | semi-auto left-click guns | A click during the fire delay is swallowed. |
| `GUN-32` | left-click guns | Client and server `fireDelay` counters can disagree, losing predicted shots. |
| `GUN-47` | left-click guns | The zoom flag is sent as true even for guns without a scope (harmless). |
| `GUN-33` to `GUN-45` | every hit | Damage-pipeline bugs, see [damage-system.md](damage-system.md). |

