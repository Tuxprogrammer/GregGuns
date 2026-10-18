# Lang and strings

Every lang key and generated string that belongs to the projectile and FX area, checked against `resources/assets/techguns/lang/en_US.lang` (the only lang file in the mod). Section 7 of [death-effects.md](death-effects.md) first listed the death messages; this page is the full inventory, with the damage-source name that produces each key, the missing keys, and the places where this area has no visible text at all.

## 1. Death messages (`death.attack.*`)

The key is built by the vanilla entity-damage-source message routine as `death.attack.<damage source name>`. When the attacker holds an item with a custom display name, the routine uses the `.item` key instead and passes three arguments (victim, attacker, item name); otherwise it passes two (victim, attacker). Where no `.item` key exists (the three sources marked "none" below) the lang file has no text for that case.

| Source name | Created by | Damage type, death type | Plain key and English text | `.item` key and English text |
|---|---|---|---|---|
| `tg_bullet` | [`GenericProjectile`](projectiles.md#41-genericprojectile-the-base) default, [`SimpleProjectile`](projectiles.md#31-simpleprojectile), [`MiningDummyProjectile`](projectiles.md#314-miningdummyprojectile) | PROJECTILE, GORE | `death.attack.tg_bullet` = `%s was shot by %s` | `death.attack.tg_bullet.item` = `%s was shot by %s with %s` |
| `tg_explosion` | [`RocketProjectile`](projectiles.md#34-rocketprojectile-and-nukerocketprojectile), [`NukeRocketProjectile`](projectiles.md#34-rocketprojectile-and-nukerocketprojectile), [`TGDamageSource.causeExplosionDamage`](../02-guns/damage-system.md#2-tgdamagesource-extends-vanilla-entitydamagesource) | EXPLOSION, GORE | `death.attack.tg_explosion` = `%s was blown up by %s` | `.item` = `%s was blown up by %s with %s` |
| `tg_fire` | [`FlamethrowerProjectile`](projectiles.md#32-flamethrowerprojectile), [`CyberdemonBlasterProjectile`](projectiles.md#38-cyberdemonblasterprojectile), [`AlienBlasterProjectile`](projectiles.md#1-registration) | FIRE | `death.attack.tg_fire` = `%s was burned to death by %s` | `.item` = `%s was burned to death by %s with %s` |
| `tg_poison` | [`BiogunProjectile`](projectiles.md#35-biogunprojectile), bio blob explosion | POISON, BIO | `death.attack.tg_poison` = `%s was poisoned by %s` | `.item` = `%s was poisoned by %s with %s` |
| `powerhammer` | [`PowerHammerProjectile`](projectiles.md#311-powerhammerprojectile) | PHYSICAL, GORE | `death.attack.powerhammer` = `%s was dismembered by %s with a powerhammer` | `.item` = `%s was dismembered by %s with %s` |
| `tg_lightning` | [`TeslaProjectile`](projectiles.md#36-teslaprojectile-chain-lightning) | LIGHTNING, LASER | `death.attack.tg_lightning` = `%s was electrocuted by %s` | `.item` = `%s was electrocuted by %s with %s` |
| `tg_energy` | [`LaserProjectile`](projectiles.md#33-laserprojectile), [`BeamProjectile`](projectiles.md#1-registration), [`BlasterRifleProjectile`](projectiles.md#1-registration), [`EnergyBlastProjectile`](projectiles.md#310-energyblastprojectile-mib-gun) | ENERGY | `death.attack.tg_energy` = `%s was melted by %s` | `.item` = `%s was melted by %s with %s` |
| [`chainsaw`](../02-guns/gun-catalogue.md#table-a-identity-and-ammunition) | [`InvisibleProjectile`](projectiles.md#43-invisibleprojectile) | PHYSICAL, GORE | `death.attack.chainsaw` = `%s was dismembered by %s with a chainsaw` | none |
| `tg_sonic` | [`SonicShotgunProjectile`](projectiles.md#313-sonicshotgunprojectile) | PHYSICAL, GORE | `death.attack.tg_sonic` = `%s was shreddered by %s's sonic weapon` | none |
| `tg_rad` | [`MovingBeam`](projectiles.md#1-registration), [`NeutrinoBeam`](projectiles.md#1-registration) | RADIATION, LASER | `death.attack.tg_rad` = `%s was radiated by %s` | none |

Observations:
- Exactly 17 `death.attack.*` keys exist (lines 99 to 115 of the lang file): seven plain-plus-`.item` pairs (14 keys) plus three plain-only keys.
- `chainsaw`, `tg_sonic` and `tg_rad` have no `.item` variant. The `powerhammer` variant says "with a powerhammer" in the plain text, so its `.item` text is the only one that uses the item's own name.
- Typo: "shreddered" (should be "shredded").
- The `tg_sonic` text uses an apostrophe-s on a `%s` argument, so it reads as "X's sonic weapon" and never names the held item.
- Vanilla-style source names used by dead code: `bullet` ([`BulletDamageSource`](../01-core/textures-sounds-lang.md#missing-or-untranslated-keys-used-by-core-code), used by [`RailgunProjectile`](projectiles.md#316-railgunprojectile-never-spawned) and [`BlasterProjectile`](projectiles.md#318-blasterprojectile-deprecated)).

## 2. Missing keys (PFX-31 and related)

| Key that code would request | Cause | Result |
|---|---|---|
| `death.attack.tg_knockback` | The 0.01-damage dummy hit (`TGDamageSource` name `tg_knockback`, PHYSICAL, DEFAULT) can deliver the killing blow to a nearly dead entity. | The raw key is shown in chat. |
| `death.attack.tg_knockback.item` | As above, when the attacker holds a named item. | Raw key. |
| `death.attack.bullet` (and `.item`) | `BulletDamageSource` name used by the unspawned `RailgunProjectile` and `BlasterProjectile`. | Latent; only visible if those classes were ever spawned. |
| `death.attack.chainsaw.item`, `death.attack.tg_sonic.item`, `death.attack.tg_rad.item` | Held-item name present on the attacker. | Raw key. |
| `entity.Techguns.<name>.name` for every projectile except the rocket | See section 3. | Nothing in the UI shows projectile entity names, so the gap is invisible in play. |

## 3. Entity names

| Key | English | Notes |
|---|---|---|
| `entity.Techguns.RocketProjectile.name` | `Rocket` | The only projectile entity name in `en_US.lang` (line 548). It would appear in vanilla death messages that use `getCommandSenderName` or in the `/summon` listing. Registration names for the other 25 projectile entities have no lang key. |

The NPC `entity.Techguns.*.name` keys on the same lines of the lang file belong to [`10-entities-npcs`](../10-entities-npcs/index.md).

## 4. Other keys touching this area

| Key | English | Where used in this area |
|---|---|---|
| `techguns.TGDamageType.PROJECTILE` | `Projectile` | Damage-type name shown in resistance tooltips; the bullet sources deliver this type (tooltips are owned by [`05-armor`](../05-armor/index.md)). |
| `techguns.TGDamageType.EXPLOSION` | `Explosion` | As above, for explosions and rockets. |
| `techguns.TGDamageType.ENERGY` | `Energy` | Laser, blaster, MIB gun. |
| `techguns.TGDamageType.FIRE` | `Fire` | Flamethrower, cyberdemon blaster, alien blaster. |
| `techguns.TGDamageType.LIGHTNING` | `Lightning` | Tesla gun. |
| `techguns.TGDamageType.POISON` | `Poison` | Bio gun. |
| `techguns.TGDamageType.RADIATION` | `Radiation` | Nuclear death ray. |
| `techguns.TGDamageType.PHYSICAL` | `Physical` | Chainsaw, power hammer, sonic shotgun, the knockback dummy hit. |
| `techguns.TGDamageType.ICE` | `Ice` | No projectile in this area uses it. |

## 5. Strings generated in code

| String | Condition |
|---|---|
| Debug lines printed to stdout (`writeEntityToNBT`, `ReadEntityFromNBT`, FX parse messages, dismember and laser-FX per-frame prints) | Developer output only, never shown to a player ([PFX-30](index.md#suspected-bugs), [PFX-33](index.md#suspected-bugs)). |
| The FX parser's literal text `error` | Printed when a data-file line fails to parse (PFX-33). |
| Warning from [`PacketSpawnParticleOnEntity`](../01-core/networking.md#14--packetspawnparticleonentity) when the entity id is not found | Alien blaster trail ([PFX-01](index.md#suspected-bugs)). |

This area contains no tooltips, no chat messages, no GUI text and no hard-coded English that a player can read. The names of FX lists and particle systems (`LargeExplosion`, `TeslaFlare` and so on) are internal data-file identifiers, not lang keys.

## 6. Sound subtitles

`sounds.json` carries no subtitle or lang keys for the `effects.*` events (1.7.10 has no subtitle system).
