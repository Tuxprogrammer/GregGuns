# 02-guns: lang keys and strings

Every `en_US.lang` key in the scope of this area, with the exact English text, and the strings that the gun code builds at run time. The lang file is `resources/assets/techguns/lang/en_US.lang`; the "Line" column is the line in that file. Where a key is defined more than once the Java properties rule applies (the last definition wins); all duplicates found here hold identical values.

Other areas own: the ammo and part item names ([06-items/lang.md](../06-items/lang.md)), armour names and armour camo names ([05-armor/lang-and-strings.md](../05-armor/lang-and-strings.md)), NPC names ([10-entities-npcs](../10-entities-npcs/index.md)) and projectile entity names ([04-projectiles-fx](../04-projectiles-fx/index.md)).

## 1. Gun and grenade names

The in-game name of a gun is looked up with `getUnlocalizedName(stack) + ".name"`, which is `techguns.item.<unlocalized name>.name`. Item sheets: [sheets-index.md](sheets-index.md).

| Key | English text | Line | Notes |
|---|---|---|---|
| `techguns.item.handcannon.name` | "Hand Cannon" | 13 |  |
| `techguns.item.pistol.name` | "9mm Pistol" | 15 |  |
| `techguns.item.sawedoff.name` | "Sawed Off" | 16 |  |
| `techguns.item.boltaction.name` | "Bolt Action Rifle" | 17 |  |
| `techguns.item.m4.name` | "M4 Assault Rifle" | 36 |  |
| `techguns.item.flamethrower.name` | "Flamethrower" | 18 |  |
| `techguns.item.combatshotgun.name` | "Combat Shotgun" | 39 |  |
| `techguns.item.lasergun.name` | "Laser Rifle" | 40 |  |
| `techguns.item.rocketlauncher.name` | "Rocket Launcher" | 19 |  |
| `techguns.item.grimreaper.name` | "Grim Reaper" | 478 |  |
| `techguns.item.biogun.name` | "Bio Gun" | 41 | Defined 2 times (lines 20, 41). |
| `techguns.item.thompson.name` | "Submachine Gun" | 43 | Registry and unlocalized name [`thompson`](gun-catalogue.md#table-a-identity-and-ammunition); the English name is "Submachine Gun". |
| `techguns.item.revolver.name` | "Revolver" | 44 |  |
| `techguns.item.ak47.name` | "Kalaschnikow" | 58 | English spelling "Kalaschnikow"; the config comment calls it "Kalashnikov". |
| `techguns.item.as50.name` | "AS50 Sniper Rifle" | 59 |  |
| `techguns.item.teslagun.name` | "Tesla Rifle" | 42 |  |
| `techguns.item.pdw.name` | "PDW" | 78 |  |
| `techguns.item.lmg.name` | "LMG" | 79 |  |
| `techguns.item.m4_uq.name` | "The Infiltrator" | 80 | In game "The Infiltrator". |
| `techguns.item.goldenrevolver.name` | "Golden Revolver" | 479 |  |
| `techguns.item.pulseRifle.name` | "Pulse Rifle" | 121 | Registered as [`pulserifle`](gun-catalogue.md#table-a-identity-and-ammunition), but the lang key uses the unlocalized name [`pulseRifle`](gun-catalogue.md#table-a-identity-and-ammunition). |
| `techguns.item.alienBlaster.name` | "Alien Blaster" | 480 |  |
| `techguns.item.cyberdemonBlaster.name` | "Nether Blaster" | 122 | Called the "Nether Blaster" in game; the key keeps the old "cyberdemon" name. |
| `techguns.item.minigun.name` | "Minigun" | 423 |  |
| `techguns.item.blasterRifle.name` | "Blaster Rifle" | 474 |  |
| `techguns.item.powerHammer.name` | "Power Hammer" | 428 |  |
| `techguns.item.nukelauncher.name` | "Nuke Launcher" | 477 |  |
| `techguns.item.powerHammerAdv.name` | "Power Hammer (Advanced)" | 429 |  |
| `techguns.item.grenadelauncher.name` | "Grenade Launcher" | 516 |  |
| `techguns.item.aug.name` | "AUG" | 504 |  |
| `techguns.item.sonicShotgun.name` | "Sonic Rifle" | 521 | Called the "Sonic Rifle" in game; the key keeps the "shotgun" name. |
| `techguns.item.chainsaw.name` | "Chainsaw" | 524 |  |
| `techguns.item.scatterBeamGun.name` | "Scatter Laser Rifle [WIP]" | 566 | The "[WIP]" tag is part of the in-game name. |
| `techguns.item.nuclearDeathRay.name` | "Nuclear Death Ray" | 614 | Registered as `beamtestgun`; the lang key uses the unlocalized name [`nuclearDeathRay`](gun-catalogue.md#table-a-identity-and-ammunition). |
| `techguns.item.mac10.name` | "Mac10" | 622 |  |
| `techguns.item.pistolAkimbo.name` | "Akimbo Pistols" | 621 |  |
| `techguns.item.revolverAkimbo.name` | "Akimbo Revolvers" | 624 |  |
| `techguns.item.goldenrevolverAkimbo.name` | " Akimbo Golden Revolvers" (the value has leading or trailing whitespace) | 625 | The value begins with a space (`GUN-46`). |
| `techguns.item.mac10Akimbo.name` | "Akimbo Mac10s" | 623 |  |
| `techguns.item.mibGun.name` | "De-Atomizer Pistol" | 630 | In game "De-Atomizer Pistol". |
| `techguns.item.scar.name` | "SCAR" | 646 |  |
| `techguns.item.vector.name` | "Vector" | 648 |  |
| `techguns.item.mibGunAkimbo.name` | "Akimbo De-Atomizer Pistols" | 697 | In game "Akimbo De-Atomizer Pistols". |
| `techguns.item.stielGranate.name` | "Stielgranate" | 419 |  |
| `techguns.item.fragGrenade.name` | "Frag Grenade" | 626 |  |

[`flashbang`](gun-catalogue.md#table-f-grenades-not-genericgun) has no lang key because the item is never registered (`GUN-24`).

### Unused or stray gun keys

| Key | English text | Line | Notes |
|---|---|---|---|
| `techguns.item.mp5.name` | "Submachine Gun" | 35 | The MP5 gun is not registered (its [`TGuns`](gun-catalogue.md) lines are commented out). The text is the same as the Thompson's. |
| `techguns.item.neutrinoGun.name` | "Neutrino Gun [WIP]" | 631 | The Neutrino Gun registration is commented out. |
| `techguns.item.testGun.name` | "Test Gun" | 3 | Name of the unused default `GenericGun()` constructor (unlocalized name `testGun`). |
| `techguns.item.testMag.name` | "Test Gun magazine" | 4 | Test magazine; no gun uses it. |
| `techguns.item.fragGenade.name` | "Frag Grenade" | 639 | Typo of [`fragGrenade`](gun-catalogue.md#table-f-grenades-not-genericgun); nothing reads it. |
| `techguns.item.rocketLauncher.name` | "Rocket Launcher" | 67 | Capital-L variant; the gun is [`rocketlauncher`](gun-catalogue.md#table-a-identity-and-ammunition), so this key is never looked up. |

## 2. Tooltip keys read by the gun code

| Key | English text | Line | Notes |
|---|---|---|---|
| `techguns.gun.tooltip.shift1` | "Press" | 597 | Hint line, part 1. |
| `techguns.gun.tooltip.shift2` | "[SHIFT]" | 598 | Hint line, part 2 (coloured green by the code). |
| `techguns.gun.tooltip.shift3` | "for detailed tooltip" | 599 | Hint line, part 3 (grey). |
| `techguns.gun.tooltip.ammo` | "Ammo" | 600 | Prefix of the ammo line. |
| `techguns.gun.tooltip.damage` | "Damage" | 601 | Prefix of the damage line. |
| `techguns.gun.tooltip.damageType` | "DamageType" | 602 | Prefix of the damage-type line. |
| `techguns.gun.tooltip.range` | "Range" | 603 | Shows drop start, drop end and time to live in ticks (`GUN-18`). |
| `techguns.gun.tooltip.spread` | "Spread" | 604 |  |
| `techguns.gun.tooltip.clipsize` | "Clipsize" | 605 |  |
| `techguns.gun.tooltip.reloadTime` | "ReloadTime" | 606 |  |
| `techguns.gun.tooltip.armorPen` | "ArmorPen" | 607 |  |
| `techguns.gun.tooltip.zoom` | "Zoom" | 608 |  |
| `techguns.gun.tooltip.zoom.hold` | "hold" | 609 | No shipped gun uses hold zoom. |
| `techguns.gun.tooltip.zoom.toogle` | "toggle" | 610 | The key is misspelt "toogle"; the code uses the same spelling. |
| `techguns.gun.tooltip.zoom.multiplier` | "Mult" | 611 |  |
| `techguns.gun.tooltip.radius` | "Radius" | 612 | Grenades only. |
| `techguns.gun.tooltip.bounces` | "Bounces" | 613 | Grenades only. |
| `techguns.tooltip.toolclasses` | "Toolclasses" | 680 | Header of the tool-class list ([`ItemUtil.addToolClassesTooltip`](../01-core/utilities.md#itemutil)). |
| `techguns.item.defaultcamo` | "Default Camo" | 182 | Returned for camo index 0 (`getCurrentCamoName`) and shown by the Camo Bench. |
| `techguns.item.invalidcamo` | "No Camo" | 181 | Camo Bench text when the item cannot change camo. |
| `techguns.itemGroup.Techguns` | "Techguns" | 72 | Creative tab label. |

## 3. Damage-type keys (tooltip)

| Key | English text | Line | Notes |
|---|---|---|---|
| `techguns.TGDamageType.PHYSICAL` | "Physical" | 430 |  |
| `techguns.TGDamageType.PROJECTILE` | "Projectile" | 431 |  |
| `techguns.TGDamageType.EXPLOSION` | "Explosion" | 432 |  |
| `techguns.TGDamageType.ENERGY` | "Energy" | 433 |  |
| `techguns.TGDamageType.FIRE` | "Fire" | 434 |  |
| `techguns.TGDamageType.ICE` | "Ice" | 435 |  |
| `techguns.TGDamageType.LIGHTNING` | "Lightning" | 436 |  |
| `techguns.TGDamageType.POISON` | "Poison" | 437 |  |
| `techguns.TGDamageType.RADIATION` | "Radiation" | 438 |  |
| `techguns.TGDamageType.UNRESISTABLE` | (missing) | | Missing: the raw key would be shown (`GUN-43`). |

## 4. Gun camo names

The lookup is `techguns.item.<name>.camoname.<index>` for index above 0; index 0 always uses `techguns.item.defaultcamo`. Camo counts come from the texture variants in `TGuns`; every variant above 0 has a key.

| Key | English text | Line | Notes |
|---|---|---|---|
| `techguns.item.boltaction.camoname.1` | "Olive" | 178 |  |
| `techguns.item.boltaction.camoname.2` | "Tactical Black" | 179 |  |
| `techguns.item.m4.camoname.1` | "Swamp" | 487 |  |
| `techguns.item.m4.camoname.2` | "Desert" | 488 |  |
| `techguns.item.m4.camoname.3` | "Ice" | 489 |  |
| `techguns.item.m4.camoname.4` | "Hazard" | 490 |  |
| `techguns.item.ak47.camoname.1` | "Blue" | 486 |  |
| `techguns.item.pdw.camoname.1` | "Silver" | 482 |  |
| `techguns.item.pdw.camoname.2` | "Dark Green" | 483 |  |
| `techguns.item.pulseRifle.camoname.1` | "Grey" | 484 |  |
| `techguns.item.pulseRifle.camoname.2` | "Dark Green" | 485 |  |
| `techguns.item.aug.camoname.1` | "Grey" | 505 |  |
| `techguns.item.aug.camoname.2` | "Wood" | 506 |  |
| `techguns.item.aug.camoname.3` | "Sand" | 507 |  |
| `techguns.item.aug.camoname.4` | "Nether" | 508 |  |
| `techguns.item.scar.camoname.1` | "Black" | 654 |  |
| `techguns.item.vector.camoname.1` | "Black" | 653 |  |

No other gun has more than one texture (see the sheets), so none needs more keys.

## 5. Death messages (`death.attack.<source name>`)

| Key | English text | Line | Notes |
|---|---|---|---|
| `death.attack.tg_bullet` | "%s was shot by %s" | 99 |  |
| `death.attack.tg_bullet.item` | "%s was shot by %s with %s" | 100 | Used when the killer's held item has a custom display name. |
| `death.attack.tg_explosion` | "%s was blown up by %s" | 101 |  |
| `death.attack.tg_explosion.item` | "%s was blown up by %s with %s" | 102 |  |
| `death.attack.tg_fire` | "%s was burned to death by %s" | 103 |  |
| `death.attack.tg_fire.item` | "%s was burned to death by %s with %s" | 104 |  |
| `death.attack.tg_poison` | "%s was poisoned by %s" | 105 |  |
| `death.attack.tg_poison.item` | "%s was poisoned by %s with %s" | 106 |  |
| `death.attack.powerhammer` | "%s was dismembered by %s with a powerhammer" | 107 | Hard-coded item name instead of `%s`. |
| `death.attack.powerhammer.item` | "%s was dismembered by %s with %s" | 108 |  |
| `death.attack.tg_lightning` | "%s was electrocuted by %s" | 109 |  |
| `death.attack.tg_lightning.item` | "%s was electrocuted by %s with %s" | 110 |  |
| `death.attack.tg_energy` | "%s was melted by %s" | 111 |  |
| `death.attack.tg_energy.item` | "%s was melted by %s with %s" | 112 |  |
| `death.attack.chainsaw` | "%s was dismembered by %s with a chainsaw" | 113 | Hard-coded item name; no `.item` variant exists. |
| `death.attack.tg_sonic` | "%s was shreddered by %s's sonic weapon" | 114 | Grammar slip ("shreddered"); no `.item` variant exists. |
| `death.attack.tg_rad` | "%s was radiated by %s" | 115 | No `.item` variant exists. |

Source names with no key: `tg_knockback` (the 0.01-damage dummy source, practically never lethal) and the melee source `player`, which falls back to the vanilla `death.attack.player`. `.item` variants are missing for [`chainsaw`](gun-catalogue.md#table-a-identity-and-ammunition), `tg_sonic` and `tg_rad`, which is harmless because the base keys always apply. Exact rules for choosing the key are in [damage-system.md](damage-system.md#2-tgdamagesource-extends-vanilla-entitydamagesource).

## 6. Gun-related keys owned by 01-core

| Key | English text | Line | Notes |
|---|---|---|---|
| `techguns.key.categories.techguns` | "Techguns" | 406 | Keybind category. |
| `techguns.key.forceReload` | "Force Reload" | 411 | Keybind that sends `PacketTGKeybindPress(FORCE_RELOAD)`. |
| `techguns.key.toggleSafemode` | "Toggle Safe Mode" | 408 | Keybind for safe mode. |
| `techguns.tgguitooltip.togglesafe` | "Toggle Safe Mode" | 391 |  |
| `techguns.tgguitooltip.togglesafe.tooltip1` | "In safe mode guns don't" | 395 |  |
| `techguns.tgguitooltip.togglesafe.tooltip2` | " destroy or ignite blocks." (the value has leading or trailing whitespace) | 396 | Starts with a space so the two lines join. |

## 7. Strings built in code

Everything the gun classes print is assembled in [`GenericGun.addInformation`](gun-lifecycle.md) and [`GenericGrenade.addInformation`](per-gun-behaviour.md#grenades-genericgrenade-fraggrenade-flashbang). The colour codes are `EnumChatFormatting` constants. "Translate" means [`TextUtil.trans`](../01-core/utilities.md#textutil), which looks the key up in the lang file.

| String | Built from | Condition |
|---|---|---|
| `Ammo: [Nx ]<ammo item name>` | translate(`techguns.gun.tooltip.ammo`), `": "`, the ammo count and `"x "` only when `ammoCount` is above 1, a white colour code, translate(`<ammo item unlocalized name>.name`) | Always, in both tooltip modes. The akimbo stack size of 2 is not shown. |
| `DamageType: <coloured type>` | translate(`techguns.gun.tooltip.damageType`), `": "`, [`DamageType.toString()`](damage-system.md#1-damage-types-damagetype-enum) (colour + translated type) | SHIFT held. |
| `Damage[(xN)]: <max>-<min>` | translate(`techguns.gun.tooltip.damage`), `"(x"` + (pellets + 1) + `")"` when the gun is a shotgun or burst gun, `": "`, damage, `"-"` and the minimum | Without SHIFT: always both values (`GUN-19`). With SHIFT: the `-<min>` part only when the minimum is below the maximum. |
| `Range: a,b,c` | translate key, raw integers drop start, drop end, time to live | SHIFT held. The third value is ticks (`GUN-18`). |
| `Spread: <accuracy>` | translate key, the raw float | SHIFT held. |
| `Clipsize: <n>` | translate key, clip size | SHIFT held. |
| `ReloadTime: <t>s` | translate key, `reloadtime x 0.05f` and a literal `"s"` | SHIFT held. |
| `ArmorPen: <p>%` | translate key, `penetration x 100` and a literal `"%"` | SHIFT held and penetration above 0. |
| `Zoom:(toggle) Mult:<m>` or `Zoom:(hold) Mult:<m>` | translate keys, no space after `Zoom:` | SHIFT held and the gun has a scope. |
| `Toolclasses` then `  <class> : <level>` lines | translate(`techguns.tooltip.toolclasses`); one indented line per `HashMap` entry, including the internal `default` entry (`GUN-28`) | SHIFT held and the gun has tool classes (Power Hammer, Chainsaw). |
| `Press [SHIFT] for detailed tooltip` | shift1 + space + green + shift2 + space + grey + shift3 | Without SHIFT, always. |
| Grenade: `Damage: <d>` plus the hint line | translate(`techguns.gun.tooltip.damage`) | Without SHIFT. |
| Grenade: `DamageType: Explosion`, `Damage`, `Radius: <r>`, `Bounces: <n>` | translate keys | SHIFT held. |
| Vanilla attribute line `+6 Attack Damage` (Power Hammer, Chainsaw) | generated by vanilla 1.7.10 from `getAttributeModifiers` (6 or 2 for the hammer, 12 or 2 for the chainsaw, depending on ammo) | Always shown for tool guns; not Techguns code. |

The out-of-ammo feedback is the vanilla sound `mob.villager.idle`, not a chat message. No gun code sends chat messages.

## 8. Missing keys, typos and hard-coded English

| Item | Detail |
|---|---|
| Missing key | `techguns.TGDamageType.UNRESISTABLE` (`GUN-43`). |
| Leading space | `techguns.item.goldenrevolverAkimbo.name` (`GUN-46`). The same defect exists for `techguns.item.lmgMagEmpty.name` (an ammo item, [06-items](../06-items/lang.md)). |
| Misspelt key | `techguns.gun.tooltip.zoom.toogle` (read with the same spelling, so it works). |
| Hard-coded English | The punctuation and units of the generated lines (`": "`, `"x "`, `"(x"`, `"s"`, `"%"`, the two-space indent and `" : "` of the tool classes); the strings `"default"`, `"pickaxe"`, `"shovel"` and `"axe"` shown as tool-class names; the sound name `mob.villager.idle`. |
| Hard-coded item names in death messages | `death.attack.powerhammer` and `death.attack.chainsaw` embed "a powerhammer" / "a chainsaw" instead of using the item name. |
| Duplicate keys | `techguns.item.biogun.name` is defined twice with the same text. |
| Unused keys | See section 1: `mp5`, `neutrinoGun`, `testGun`, `testMag`, `fragGenade`, `rocketLauncher`. |
| Case-variant key | `techguns.item.rocketLauncher.name` versus the real `techguns.item.rocketlauncher.name`. |
| No item name | The Flashbang (unregistered). |

