---
navigation:
  title: Ammo and magazines
  parent: index.md
  position: 70
item_ids:
  - gregguns:ammo_stone_bullet
  - gregguns:ammo_9mm_bullet
  - gregguns:ammo_shotgun_shell
  - gregguns:ammo_rifle_bullet
  - gregguns:mag_9mm_pistol
  - gregguns:ammo_40mm_grenade
  - gregguns:mag_smg
  - gregguns:mag_assault_rifle
  - gregguns:ammo_high_caliber_round
  - gregguns:ammo_rocket
  - gregguns:mag_as50
  - gregguns:mag_lmg
  - gregguns:tank_fuel
  - gregguns:tank_air
  - gregguns:ammo_advanced_bullet
  - gregguns:mag_advanced
  - gregguns:mag_minigun_drum
  - gregguns:tank_bio
  - gregguns:ammo_nether_charge
  - gregguns:ammo_rocket_nuke
  - gregguns:mag_9mm_pistol_empty
  - gregguns:mag_smg_empty
  - gregguns:mag_assault_rifle_empty
  - gregguns:mag_as50_empty
  - gregguns:mag_lmg_empty
  - gregguns:mag_advanced_empty
  - gregguns:mag_minigun_drum_empty
---

# Ammo and magazines

Bullets are a production line. Feed GregTech parts into an Ammo Press, load magazines, and keep the line running.

## The line

1. **Materials:** Brass, Lead, Steel, Gunpowder, and higher-tier metals for later ammo.
2. **Parts:** Brass foil and casings make cases, lead rounds make the slugs, plates and small springs make magazines. Lathes, Bending Machines, Extruders and Assemblers make the parts.
3. **Ammo Press:** presses rounds and magazines. The press has its own recipe tab in NEI. A press runs every recipe at or below its tier. The **Ammunition Works** does the same in bulk.
4. **Magazines:** a loaded magazine is an empty one plus loose rounds, made in the press or by hand in a crafting grid.
5. **Reload:** press **R** to swap in a loaded magazine. The empty one comes back to your inventory.

Sneak and right-click a magazine to empty it back into loose rounds. Press **R** early to swap out a half-used magazine; the leftover rounds come back too.

## Magazines

- <ItemImage id="gregguns:mag_9mm_pistol" label="right" />: made from 3 <ItemLink id="gregguns:ammo_9mm_bullet" />; holds 18 rounds. Used by 9mm Pistol, Akimbo Pistols.
- <ItemImage id="gregguns:mag_smg" label="right" />: made from 2 <ItemLink id="gregguns:ammo_9mm_bullet" />; holds 20 to 32 rounds. Used by Thompson, Mac10, Vector.
- <ItemImage id="gregguns:mag_assault_rifle" label="right" />: made from 3 <ItemLink id="gregguns:ammo_rifle_bullet" />; holds 30 rounds. Used by M4, AK47, AUG, SCAR, Infiltrator.
- <ItemImage id="gregguns:mag_as50" label="right" />: made from 2 <ItemLink id="gregguns:ammo_high_caliber_round" />; holds 10 rounds. Used by AS50.
- <ItemImage id="gregguns:mag_lmg" label="right" />: made from 8 <ItemLink id="gregguns:ammo_rifle_bullet" />; holds 100 rounds. Used by LMG.
- <ItemImage id="gregguns:mag_advanced" label="right" />: made from 3 <ItemLink id="gregguns:ammo_advanced_bullet" />; holds 12 to 20 rounds. Used by PDW, Pulse Rifle.
- <ItemImage id="gregguns:mag_minigun_drum" label="right" />: made from 16 <ItemLink id="gregguns:ammo_rifle_bullet" />; holds 200 rounds. Used by Minigun.

Revolvers, shotguns, the Bolt Action Rifle and the launchers take loose rounds straight from your inventory. No magazines needed.

## What to stock at each tier

### Steam age

- <ItemImage id="gregguns:ammo_stone_bullet" label="right" />

### LV (Low Voltage)

- <ItemImage id="gregguns:ammo_9mm_bullet" label="right" />
- <ItemImage id="gregguns:ammo_shotgun_shell" label="right" />
- <ItemImage id="gregguns:ammo_rifle_bullet" label="right" />
- <ItemImage id="gregguns:mag_9mm_pistol" label="right" />

### MV (Medium Voltage)

- <ItemImage id="gregguns:ammo_40mm_grenade" label="right" />
- <ItemImage id="gregguns:mag_smg" label="right" />
- <ItemImage id="gregguns:mag_assault_rifle" label="right" />

### HV (High Voltage)

- <ItemImage id="gregguns:ammo_high_caliber_round" label="right" />
- <ItemImage id="gregguns:ammo_rocket" label="right" />
- <ItemImage id="gregguns:mag_as50" label="right" />
- <ItemImage id="gregguns:mag_lmg" label="right" />
- <ItemImage id="gregguns:tank_fuel" label="right" />
- <ItemImage id="gregguns:tank_air" label="right" />

### EV (Extreme Voltage)

- <ItemImage id="gregguns:ammo_advanced_bullet" label="right" />
- <ItemImage id="gregguns:mag_advanced" label="right" />
- <ItemImage id="gregguns:mag_minigun_drum" label="right" />
- <ItemImage id="gregguns:tank_bio" label="right" />

### LuV (Ludicrous Voltage)

- <ItemImage id="gregguns:ammo_nether_charge" label="right" />

### ZPM (Zero Point Module)

- <ItemImage id="gregguns:ammo_rocket_nuke" label="right" />

## Energy, tanks and endgame ammo

- **Energy weapons** have no cells to craft; charge the gun itself in a Charger or Battery Buffer of its own tier.
- **Fluid tanks:** fill <ItemLink id="gregguns:tank_fuel" /> (diesel, gasoline, ethanol), <ItemLink id="gregguns:tank_air" /> and <ItemLink id="gregguns:tank_bio" /> in a GregTech Fluid Canner. The Bio Tank takes E. coli culture: Biomass from a Brewery, grown in an EV Bacterial Vat. Empty tanks come back to you.
- **Endgame ammo:** <ItemLink id="gregguns:ammo_nether_charge" /> and <ItemLink id="gregguns:ammo_rocket_nuke" /> are too big for the press. Build them on the GregTech Assembly Line.
