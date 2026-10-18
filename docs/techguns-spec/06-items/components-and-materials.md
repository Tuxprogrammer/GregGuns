# Components and materials: dependency summary

This page shows what each intermediate item is made from and where it goes next, as a production tree. Exact grids are in [recipes.md](recipes.md). Machine recipes (Metal Press, Chem Lab, Reaction Chamber, Fabricator, Ammo Press, Charging Station) belong to **08-machines-production** and are given here only as one-line summaries so the tree is complete.

Legend: **[C]** crafting table, **[S]** furnace smelting, **[MP]** Metal Press, **[CL]** Chemical Laboratory, **[RC]** Reaction Chamber, **[FAB]** Fabricator, **[CS]** Charging Station, **[AP]** Ammo Press.

## 1. Base metals (external inputs)

Techguns assumes these OreDict ingots/nuggets come from other mods. It can add its own when the config flags are on (see [item-catalogue.md §3](item-catalogue.md#3-items-registered-in-techgunsjava-preinit-not-in-tgitems)).

| OreDict | Needed for |
|---|---|
| `ingotIron`, `nuggetIron` | almost everything tier 1 |
| `ingotCopper`, `nuggetCopper` | 9mm bullets, shotgun shells, copper wire, copper plates |
| `ingotTin` | tin plates (ladders, air tanks) |
| `ingotBronze` | bronze plate → steam armour plate |
| `ingotLead`, `nuggetLead` | 9mm bullets, shotgun shells, lead plate, Ammo Press |
| `ingotSteel`, `nuggetSteel` | steel plates, receivers, magazines, drill heads, obsidian steel |
| `ingotGold` (or `ingotElectrum`) | gold wire, laser barrel, stack upgrade |
| `gemQuartz`, `gemDiamond`, `gemLapis`, `dyeGreen`, `slimeball`, `logWood`, `stone`, `cobblestone` | various |
| `oreUranium` | Yellowcake ([CL]) |

## 2. Metal chain

```
ingotX + ingotX           --[MP]-->  2 plateX        (X = Tin, Copper, Bronze, Iron, Steel, Lead, Titanium, ObsidianSteel)
ingotSteel + obsidian     --[MP]-->  1 ingotObsidianSteel
plateObsidianSteel        --[S]--->  ingotObsidianSteel    (no XP)
plateTitanium             --[S]--->  ingotTitanium
oreTitanium (item)        --[S]--->  ingotTitanium
Titanium-iron ore block   --[RC heat ray, acid]--> 2 oreTitanium + 1 iron ore    (block from 07/11)
plateCopper ×2            --[MP]-->  8 copper wire
nuggetCopper ×5           --[C]--->  1 copper wire
ingotGold ×2              --[MP]-->  2 gold wire
fiberCarbon ×2            --[MP]-->  2 carbon plate
gemDiamond + blaze powder + 1000 mB lava --[CL]--> 2 carbon fibers
plateSteel + plateBronze  --[MP]-->  1 steam armour plating
```

The only way to make plates is the Metal Press. No crafting-table plate recipe exists.

## 3. Mechanical part tiers

| Tier | Item | Crafting [C] | Machine |
|---|---|---|---|
| 1 | Mechanical Parts (30) | flint surrounded by 4 iron ingots (plus shape) | [MP] iron plate + flint → 1 |
| 2 | Mechanical Parts (Obsidian Steel) (34) | – | [MP] obsidian-steel plate + `gemQuartz` → 1 |
| 3 | Mechanical Parts (Carbon) (37) | – | [MP] carbon plate + blaze rod → 2 |

The tier feeds the matching receiver: Iron Receiver ← T1, Steel Receiver ← T2, Obsidian Steel and Carbon Receivers ← T3.

## 4. Gun parts

| Part | Made from |
|---|---|
| Stone Barrel | 6 stone |
| Wooden Stock | 3 logs |
| Iron Barrel | 6 iron ingots |
| Hardened Steel Barrel (`obsidianSteelBarrel`) | 6 obsidian-steel ingots (exact Techguns item, not OreDict) |
| Carbon Barrel | 6 carbon plates |
| Laser Barrel | 6 gold (or electrum) ingots, 2 glass (hardened glass if available), Laser Focus |
| Laser Focus | [RC] diamond in liquid redstone (lava if no redstone fluid) with the heat-ray focus |
| Plastic Gun Stock | 4 plastic sheets |
| Carbon Gunstock | 3 carbon plates + 2 plastic sheets |
| Pump Mechanism | 6 iron nuggets, 2 glass, piston |
| Iron / Steel / Obsidian Steel / Carbon Receiver | ingots/plates of the material + nuggets/ingots + mech parts of the matching tier |

All of these feed gun recipes (02-guns).

## 5. Polymers and organics

```
logWood + 1000 mB water            --[CL]--> raw rubber
raw rubber                         --[S]---> rubber bar          (OreDict itemRubber)
raw rubber + 500 mB oil            --[CL]--> raw plastic         (if an "oil" fluid exists)
raw rubber + coal + 1000 mB water  --[CL]--> raw plastic         (fallback when no oil)
raw plastic                        --[S]---> plastic sheet       (OreDict sheetPlastic)
slimeball + 4 green dye            --[C]---> 4 bio mass
2 leather + slimeball + 500 mB creeper acid --[CL]--> 2 treated leather
wool ×4 + leather                  --[C]---> 3 heavy cloth
```

## 6. Electronics

```
copper wire ×4 + green dye ×2 + redstone ×2 + iron plate --[C]--> 4 circuit board  (circuitBasic)
circuit board + gold wire + lapis                       --[C]--> 1 elite circuit board (circuitElite)
copper wire ×4 + iron ingot ×2 + redstone ×2 + mech parts --[C]--> electric engine
copper wire ×4 + iron ingot ×3                          --[C]--> coil
soul sand + gold wire + redstone + plastic              --[FAB]--> cybernetic parts
2 titanium ingot + 4 basic circuit + 1 mech T3 + 4 carbon plate --[FAB]--> 2 power plating
coil + 2 elite circuit + 1 mech T3 + 1 titanium plate    --[FAB]--> sonic emitter
enriched uranium + 2 elite circuit + 2 mech T3 + 2 lead plate --[FAB]--> rad emitter
```

## 7. Explosives, nuclear and exotic

```
gunpowder + lapis + 250 mB fuel (500 mB lava fallback) --[CL]--> TGX
netherrack + soul sand + 1000 mB lava               --[CL]--> 4 nether charge
oreUranium + 250 mB creeper acid                    --[CL]--> yellowcake (dustUranium)
dustUranium in water                                --[RC heat ray]--> enriched uranium
4 lead plate + 2 TGX + 2 basic circuit + 1 enriched uranium --[C]--> 2 small nuclear warhead
rocket + small nuclear warhead                      --[C]--> tactical nuke rocket
nether star in liquid ender (lava fallback)         --[RC heat ray]--> anti gravity core
quartz rod (3 quartz diagonal) in lava              --[RC heat ray]--> blaze rod
```

## 8. Glider and wearable gear components

```
heavy cloth ×6 + iron ingot ×3  --> glider wing
heavy cloth ×7 + iron ingot ×2  --> glider backpack ("Backback")
backpack + 2 wings              --> Glider (back-slot item)
backpack + 2 compressed air tanks + 2 iron plates + 2 iron nuggets --> Jump Pack (05)
Glider or Jump Pack + 2 fuel tanks + 2 obsidian-steel plates + 2 rockets + mech T2 --> Jetpack (05)
backpack + nuclear power cell + 2 titanium plates + 2 elite circuits + 2 mech T3 + anti-grav core --> Anti Gravity Device (05)
8 iron nuggets + coal                                --> gas mask filter
rubber ×6 + filter + 2 glass panes                   --> Gas Mask (05)
plastic ×2 + rubber ×2                               --> Oxygen Mask (face-slot item)
plastic ×4 + NVG + gas mask + oxygen mask + 2 circuits --> Tactical Mask (05)
```

## 9. Machine-related items

| Item | Made from | Used by |
|---|---|---|
| Machine Upgrade (Stack Operation) | 4 iron plates, 3 gold ingots, green dye, chest | Upgrade slot of Ammo Press, Metal Press, Chem Lab, Fabricator, Charging Station (`SlotMachineUpgrade` accepts only this meta). In the Metal Press, up to `upgrades + 1` items are processed per operation, so 7 upgrades give 8×. The other machines' use is in 08. Max stack 7. |
| Turret Armor (Iron / Steel / Obsidian Steel / Carbon) | 5 plates of the material (T shape) | Turret armour slot: +5 / +10 / +15 / +20 armour |
| Ore Drill Heads Small / Medium / Large × Steel / Obsidian Steel / Carbon | Small: ingots + plates of the material (carbon: 7 carbon plates). Medium = 3 small. Large = 3 medium. | Ore Drill head slot (08). Tooltip shows valid drill sizes and the bonus mining level 1 / 2 / 3. |
| UV Emitter (beam focus) | **No recipe** (creative/loot only) | Reaction Chamber focus, plays `techguns:machines.chemlabWork` |
| Reaction Chamber Heat Ray Focus | 2 elite circuits, gold wire, 4 steel plates, 2 redstone lamps | Reaction Chamber focus, plays `techguns:machines.rc_heatrayWork` |

## 10. Items with no way to obtain them in survival (as shipped)

- **UV Emitter (92)**: no recipe anywhere.
- **Power Plating (89)**: only the Fabricator (its crafting recipe is commented out).
- **Rifle Bullets (7)** and **High Caliber Rounds (19)**: Ammo Press only, plus loot and NPC drops.
- **Energy Cell (15)**: only via the Charging Station or loot.
- **`rubberDrop`**: has a lang entry (`techguns.item.rubberDrop.name=Rubber Drop`) but no item exists.

## 11. Dependency overview (tiers)

```
Tier 0  vanilla + OreDict metals
Tier 1  iron plates, copper wire, mech parts T1, circuit board, electric engine, iron receiver/barrel
        -> Ammo Press, Metal Press, Chem Lab, Turret, Repair Bench, Camo Bench (basicMachine 0-5)
Tier 2  obsidian steel ingot/plate, steel plate/receiver, mech T2, rubber, plastic, TGX, fuel
        -> advanced magazines, jetpack, explosive charges
Tier 3  carbon fibers/plates, mech T3, elite circuit, gold wire, cybernetic parts
        -> Reaction Chamber / Fabricator multiblocks, Charging Station
Tier 4  titanium, uranium chain, power plating, emitters, anti-gravity core, nuclear cells
```
