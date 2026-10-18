# 0001 · Tier map

- Status: Accepted (by default during the MVP push; revisit after MVP)
- Gate: D1
- Date: 2026-10-17

## Context
Every GregGuns item needs a GT tier and a gating mechanism (see [ROADMAP D1](../ROADMAP.md#d1--tier-map-blocks-m1m8)). The content comes from [02-guns](../techguns-spec/02-guns/index.md), [05-armor](../techguns-spec/05-armor/index.md) and [06-items](../techguns-spec/06-items/index.md).

## Decision
Each item's tier is fixed by **both** of these:
1. **The minimum machine voltage** of its final recipe (a GT recipe map at that tier's `TierEU`).
2. **At least one input material** that can't be obtained before that tier.

The gate materials below are starting points. Confirm each against the current GTNH progression when its recipe is written.

| Tier | Gate material | Weapons | Armour and equipment | Ammo and consumables |
|---|---|---|---|---|
| Steam / ULV | Bronze, wrought iron | Hand Cannon | T1 Scout and T1 Miner sets | Stone bullets |
| LV | Steel | 9mm Pistol, Revolver, Sawed Off, Bolt Action Rifle; Combat Knife, Crowbar | T1 Combat set | 9mm rounds and pistol magazines, shotgun shells, rifle bullets |
| MV | Aluminium | Thompson SMG, Mac10, Combat Shotgun, M4, AK47, akimbo pistols and revolvers; frag grenade, **Flashbang** | T2 Combat, Commando and Riot sets; gas mask, night-vision goggles, **Medkit** | SMG and assault-rifle magazines, 40 mm grenades |
| HV | Stainless steel | AUG, SCAR, Vector, Infiltrator, AS50, LMG, Grenade Launcher, Rocket Launcher, Flamethrower, Chainsaw, Power Hammer; akimbo Mac10 | Jump pack, glider, oxygen mask and tanks | LMG and AS50 magazines, rockets, fuel tanks (GT fuels), compressed-air tanks |
| EV | Titanium | Laser Rifle, PDW, Pulse Rifle, Minigun, Tesla Rifle, Sonic Rifle, Bio Gun, Power Hammer (Advanced) | T3 Combat set, steam armour, jetpack | Energy cells, advanced magazines, minigun drums, bio tanks; UV emitter as a component |
| IV | Tungstensteel | Blaster Rifle, De-Atomizer Pistol and its akimbo, **Scatter Laser Rifle**, Golden Revolver and its akimbo | Power armour, exo suit, anti-grav device | — |
| LuV | HSS-G or LuV-tier alloys | Nether Blaster, Alien Blaster, Grim Reaper | — | Nether charges |
| ZPM | Naquadah alloy | Nuclear Death Ray, Nuke Launcher | — | Nuclear power cells, tactical nuke rockets |

**Unfinished Techguns content:**
- **Build:** Flashbang (MV), Medkit (MV) and the Scatter Laser Rifle (IV), as finished designs.
- **Drop:** the Scrapper (see [0002](0002-machine-deconfliction.md)).
- **Keep:** the UV Emitter, as an EV component.

## Consequences
- The dev self-check enforces this table.
- An item whose recipe can't meet its tier moves tier through a new record. Its recipe is never weakened to fit.

## Alternatives considered
- **Techguns' flat tiers** (T1–T3 crafting tables): rejected, because they break GTNH progression.
- **Gating by material alone:** rejected, because voltage gating is how GTNH players read progress.
