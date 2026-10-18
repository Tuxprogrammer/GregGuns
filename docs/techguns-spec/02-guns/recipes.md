# 02-guns: gun and grenade recipes (`TGuns.postInit`)

All recipes are registered in [`TGuns.postInit()`](gun-catalog.md).

* Recipe type is Forge `ShapedOreRecipe` unless noted as "vanilla shaped".
* Grids are written row by row, with `·` for an empty cell. Shorter rows and fewer rows follow normal shaped-recipe rules, so the pattern can be placed anywhere in the grid.
* Quoted names are ore-dictionary entries. Named items are Techguns parts (documented in [06-items](../06-items/index.md)) or vanilla items.

**Loaded vs empty output.**

* **Loaded** recipes output the gun with damage 0. They consume a full magazine or ammo item, and the gun starts with a full clip.
* **Empty** recipes output the gun with damage = `maxDamage` (= clip). They consume the empty magazine, and the gun starts with 0 rounds.

The conversion into the `ammo` NBT happens in `onCreated` (see lifecycle §2).

Ore-dict name `blockGlassHardened|blockGlass` stands for [`TGItems.hardenedGlassOrGlass`](../06-items/item-catalog.md): the ore name `blockGlassHardened` if any item is registered under it at init time, otherwise `blockGlass`.

## Shaped gun recipes

| # | Output | Variant | Grid | Key |
|---|---|---|---|---|
| 1 | Hand Cannon | empty only (vanilla shaped) | `b f s` | b = Stone Barrel, f = Flint and Steel, s = Wooden Stock |
| 2 | 9mm Pistol | loaded | `b s p` / `· f p` / `· · m` | b = Hardened Steel Barrel, s = "plateSteel", p = Plastic Sheet, f = Mechanical Parts (Obsidian Steel), m = 9mm Pistol Magazine |
| 2e | 9mm Pistol | empty | same | m = 9mm Pistol Magazine (Empty) |
| 3 | Revolver | empty only | `b m f` / `· s ·` | b = Iron Barrel, m = Mechanical Parts, f = Flint and Steel, s = Wooden Stock |
| 4 | Sawed Off | empty only (vanilla shaped) | `b f w` / `b f ·` | b = Iron Barrel, f = Flint and Steel, w = Wooden Stock |
| 5 | Submachine Gun (thompson) | loaded | `b f s` / `· m ·` | b = Iron Barrel, f = Iron Receiver, s = Wooden Stock, m = SMG Magazine |
| 5e | Submachine Gun | empty | same | m = SMG Magazine (Empty) |
| 6 | M4 Assault Rifle | loaded | `· g r` / `b f s` / `n m i` | g = "paneGlass", r = Redstone (item), b = Hardened Steel Barrel, f = Steel Receiver, s = Plastic Gun Stock, n = "nuggetIron", m = Assault Rifle Magazine, i = "ingotIron" |
| 6e | M4 Assault Rifle | empty | same | m = Assault Rifle Magazine (Empty) |
| 7 | Bolt Action Rifle | empty only | `g i ·` / `b f s` | g = "blockGlass", i = "plateIron", b = Hardened Steel Barrel, f = Iron Receiver, s = Wooden Stock |
| 8 | Combat Shotgun | empty only | `b f s` / `i · ·` | b = Hardened Steel Barrel, f = Steel Receiver, s = Plastic Gun Stock, i = "ingotSteel" |
| 9 | Flamethrower | loaded only | `p r s` / `f m ·` | p = Pump Mechanism, r = Iron Receiver, s = Plastic Gun Stock, f = Flint and Steel, m = Fuel Tank |
| 10 | Bio Gun | loaded only | `f r s` / `· m ·` | f = Pump Mechanism, r = Steel Receiver, s = Plastic Gun Stock, m = Bio Tank |
| 11 | Laser Rifle | loaded only | `· g d` / `b r s` / `· e m` | g = "paneGlass", d = Redstone (item), b = Laser Barrel, r = Obsidian Steel Receiver, s = Plastic Gun Stock, e = Energy Cell, m = Mechanical Parts (Carbon) |
| 12 | Blaster Rifle | loaded only | `p c g` / `b m s` / `· e ·` | p = "plateCarbon", c = "circuitElite", g = blockGlassHardened\|blockGlass, b = Laser Barrel, m = Carbon Receiver, s = Carbon Gunstock, e = Energy Cell. A key `r` = Redstone is declared but not used in the grid. |
| 13 | Kalaschnikow (ak47) | loaded | `b f s` / `w m ·` | b = Hardened Steel Barrel, f = Iron Receiver, s = Wooden Stock, w = "logWood", m = Assault Rifle Magazine |
| 13e | Kalaschnikow | empty | same | m = Assault Rifle Magazine (Empty) |
| 14 | AS50 Sniper Rifle | loaded | `d p d` / `b b f` / `· m t` | d = "gemDiamond", p = "plateObsidianSteel", b = Hardened Steel Barrel, f = Obsidian Steel Receiver, m = AS50 Magazine, t = Plastic Gun Stock |
| 14e | AS50 | empty | same | m = AS50 Magazine (Empty) |
| 15 | Rocket Launcher | loaded only (contains 1 rocket) | `r b b` / `· f ·` | r = Rocket, b = Hardened Steel Barrel, f = Steel Receiver |
| 16 | LMG | loaded | `· g r` / `b f s` / `· m ·` | g = "paneGlass", r = Redstone (item), b = Hardened Steel Barrel, f = Obsidian Steel Receiver, s = Plastic Gun Stock, m = LMG Magazine |
| 16e | LMG | empty | same | m = LMG Magazine (Empty) |
| 17 | Minigun | loaded | `b b e` / `b b r` / `b b m` | b = Hardened Steel Barrel (×6), e = Electric Engine, r = Obsidian Steel Receiver, m = Minigun Ammo Drum |
| 17e | Minigun | empty | same | m = Minigun Ammo Drum (empty) |
| 18 | PDW | loaded | `b r s` / `· m ·` | b = Carbon Barrel, r = Carbon Receiver, s = Carbon Gunstock, m = Advanced Magazine |
| 18e | PDW | empty | same | m = Advanced Magazine (Empty) |
| 19 | Pulse Rifle | loaded | `d p c` / `b r s` / `· m ·` | d = "gemDiamond", p = "plateTitanium", c = "circuitElite", b = Carbon Barrel, r = Carbon Receiver, s = Carbon Gunstock, m = Advanced Magazine |
| 19e | Pulse Rifle | empty | same | m = Advanced Magazine (Empty) |
| 20 | Power Hammer | loaded | `· m ·` / `s p r` / `· m c` | m = Mechanical Parts (×2), s = "plateIron", p = Piston (vanilla block), r = Iron Receiver, c = Compressed Air Tank |
| 20e | Power Hammer | empty | same | c = Compressed Air Tank (Empty) |
| 21 | Tesla Rifle | loaded | `· g d` / `c r s` / `· e ·` | g = "paneGlass", d = "dustRedstone", c = Coil, r = Carbon Receiver, s = Carbon Gunstock, e = Energy Cell |
| 21e | Tesla Rifle | empty | same | e = Energy Cell (Empty) |
| 22 | Golden Revolver | output loaded | `g g g` / `g r g` / `g g g` | g = "ingotGold" (×8), r = Revolver (damage 0, which matches any crafted revolver regardless of ammo) |
| 23 | Grim Reaper | output loaded | `r r ·` / `r r c` | r = Rocket Launcher (damage 0) ×4, c = Carbon Receiver |
| 24 | The Infiltrator (m4_uq) | output loaded | `g p g` / `w w m` / `r r i` | g = blockGlassHardened\|blockGlass, p = "plateSteel", w = "blockWool", m = M4 (damage 0), r = "dustRedstone", i = "ingotSteel" |
| 25 | Nether Blaster | loaded only | `c p c` / `b n s` / `c p c` | c = Cybernetic Parts (×4), p = "plateObsidianSteel", b = Hardened Steel Barrel, n = Nether Charge, s = Pump Mechanism |
| 26 | AUG | loaded | `g p g` / `b f s` / `i · m` | g = "paneGlass", p = "plateSteel", b = Hardened Steel Barrel, f = Steel Receiver, s = Plastic Gun Stock, i = "sheetPlastic", m = Assault Rifle Magazine |
| 26e | AUG | empty | same | m = Assault Rifle Magazine (Empty) |
| 27 | Grenade Launcher | empty only | `m r s` / `· p ·` | m = Hardened Steel Barrel, r = Steel Receiver, s = Plastic Gun Stock, p = "plateSteel" |
| 28 | Chainsaw | loaded | `c c p` / `m m r` / `c c f` | c = Mechanical Parts (×4), p = Plastic Sheet, m = "plateIron" (×2), r = Iron Receiver, f = Fuel Tank |
| 28e | Chainsaw | empty | same | f = Fuel Tank (Empty) |
| 29 | Sonic Rifle | loaded | `· p ·` / `e r s` / `· c ·` | p = "plateTitanium", e = Sonic Emitter, r = Carbon Receiver, s = Carbon Gunstock, c = Energy Cell |
| 29e | Sonic Rifle | empty | same | c = Energy Cell (Empty) |
| 30 | Nuke Launcher | loaded | `· p p` / `r b b` / `· f ·` | p = "plateLead", r = Rocket (Tactical Nuke), b = Hardened Steel Barrel, f = Carbon Receiver |
| 30e | Nuke Launcher | empty | `· p p` / `· b b` / `· f ·` | as above without the rocket |
| 31 | Nuclear Death Ray | loaded | `l t l` / `e r s` / `· m ·` | l = "plateLead", t = "plateTitanium", e = Rad Emitter, r = Carbon Receiver, s = Carbon Gunstock, m = Nuclear Power Cell |
| 31e | Nuclear Death Ray | empty | same | m = Nuclear Power Cell (Depleted) |
| 32 | Mac10 | loaded | `· p i` / `b r i` / `· m ·` | p = "sheetPlastic", i = "nuggetIron" (×2), b = Hardened Steel Barrel, r = Steel Receiver, m = SMG Magazine |
| 32e | Mac10 | empty | same | m = SMG Magazine (Empty) |
| 33 | SCAR | loaded | `g p d` / `b r s` / `· m ·` | g = blockGlassHardened\|blockGlass, p = "sheetPlastic", d = "gemDiamond", b = Hardened Steel Barrel, r = Obsidian Steel Receiver, s = Plastic Gun Stock, m = Assault Rifle Magazine |
| 33e | SCAR | empty | same | m = Assault Rifle Magazine (Empty) |
| 34 | Vector | loaded | `· g d` / `b r s` / `· m ·` | g = "paneGlass", d = "dustRedstone", b = Hardened Steel Barrel, r = Obsidian Steel Receiver, s = Plastic Gun Stock, m = SMG Magazine |
| 34e | Vector | empty | same | m = SMG Magazine (Empty) |
| 35 | De-Atomizer Pistol (mibGun) | loaded | `b s p` / `· f p` / `· · m` | b = Laser Barrel, s = Sonic Emitter, p = "plateTitanium" (×2), f = Mechanical Parts (Carbon), m = Energy Cell |
| 35e | De-Atomizer Pistol | empty | same | m = Energy Cell (Empty) |

Guns with **no** recipe: Alien Blaster, Scatter Laser Rifle [WIP], and the four akimbo guns (crafting combine only). Upgrade recipes 22–24 ignore the input gun's ammo and camo and output a fully loaded gun ([GUN-15](index.md#suspected-bugs)).

## Shapeless / custom recipes

| Output | Recipe class | Ingredients | Behavior |
|---|---|---|---|
| Power Hammer (Advanced) | `ShapelessOreRecipeCopyNBT` | Power Hammer + 3× "gemDiamond" | Copies the full NBT (ammo, camo) of the first matching Power Hammer in the grid onto the result. |
| Akimbo Pistols | `RecipeAkimboGun` | 2× 9mm Pistol | See below. |
| Akimbo Revolvers | `RecipeAkimboGun` | 2× Revolver | |
| Akimbo Golden Revolvers | `RecipeAkimboGun` | 2× Golden Revolver | |
| Akimbo Mac10s | `RecipeAkimboGun` | 2× Mac10 | |
| Akimbo De-Atomizer Pistols | `RecipeAkimboGun` | 2× De-Atomizer Pistol | |
| 9mm Pistol (+ second via event) | `RecipeAkimboSplit` | 1× Akimbo Pistols | See below. |
| Revolver (+ second) | `RecipeAkimboSplit` | 1× Akimbo Revolvers | |
| Golden Revolver (+ second) | `RecipeAkimboSplit` | 1× Akimbo Golden Revolvers | |
| Mac10 (+ second) | `RecipeAkimboSplit` | 1× Akimbo Mac10s | |
| De-Atomizer Pistol (+ second) | `RecipeAkimboSplit` | 1× Akimbo De-Atomizer Pistols | |

**`RecipeAkimboGun`** (shapeless, two copies of the single gun):

1. Find the first two grid slots holding that gun item.
2. The result's NBT is a copy of the **first** gun's tag, so the first gun's camo wins.
3. `ammo` is set to the sum of both guns' rounds (at most twice the single clip, which equals the akimbo clip).
4. If the first gun had no tag, the result has none and starts full via `onCreated`. In practice reading the ammo creates the tag first, so this doesn't happen.

**`RecipeAkimboSplit`** (shapeless, one akimbo gun):

* The result is **one** single gun carrying a copy of the akimbo gun's tag, with `ammo = floor(akimboAmmo / 2)`. The stack size is forced to 1.
* The **second** gun comes from [`TechgunsTickHandler.onCraftingEvent`](../01-core/events.md#techgunstickhandler-fml-bus) (`ItemCraftedEvent`, [01-core](../01-core/index.md)). When the crafted item is a gun and the first non-empty grid slot holds an akimbo gun, a copy of the result is made with `ammo = akimboAmmo − resultAmmo` (so an odd round goes to the second gun). It is merged into the player's main inventory, and the server drops it at the player if there is no room. This runs on both sides.

**`ShapelessOreRecipeCopyNBT`** (generic): finds the first grid slot containing the first ingredient's item type and copies its tag (if any) onto the result. It is only used for the advanced Power Hammer.

## Grenade recipes

| Output | Grid | Key |
|---|---|---|
| 16× Stielgranate | `· i t` / `· w i` / `i · ·` | i = "ingotIron" (×3), t = TNT, w = "plankWood" |
| 16× Frag Grenade | `· i f` / `i t i` / `· i ·` | i = "ingotSteel" (×4), f = Flint and Steel, t = TNT |

The Flashbang has no recipe and isn't registered.

## Commented-out recipes (not active)

The source contains disabled recipes for an older pistol, the MP5, an older flamethrower (with invar), an older laser gun (with laser focus), rocket ammo, the plastic gun stock and foregrip, and a shapeless "M4 + nether star → Infiltrator". None of them is registered.
