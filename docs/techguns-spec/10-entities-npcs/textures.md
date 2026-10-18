# Textures

This covers every file in `textures/entity/` (19 files, none animated, with no `.mcmeta` files in the folder) and the two spawn-egg item textures registered by this area. Paths are relative to `assets/techguns`.

Several files in `textures/entity/` are used by projectiles or machines rather than mobs. They are described here because they live in this folder, and the owning area is noted for each.

Vanilla textures referenced by this area's renderers are **not** Techguns assets and need no replacement art:
- `minecraft:textures/entity/zombie_pigman.png`
- `minecraft:textures/entity/skeleton/skeleton.png`
- `minecraft:textures/entity/zombie/zombie.png`

The turret entity's textures [`textures/blocks/TurretBase.png`](../07-blocks/textures.md#11-model-textures) and `TurretBase_2.png` to `TurretBase_5.png` (128×128) are shared with the turret base block and are specified by [09-machines-utility](../09-machines-utility/index.md). The entity only uses their [`ModelTurret`](../07-blocks/block-rendering-and-models.md#modelturret-and-modelsimpleturret-uv-6464--6432) UV regions (base plate (32,22), posts (9,30), mount (34,12), in 64-unit coordinates).

## Biped skin layout reference

The humanoid NPC skins use the **standard 1.7 64×32 biped layout**, placed in the top half of a 64×64 canvas (or 128×128 at 2× resolution). The bottom half is unused and transparent. In 64-unit coordinates:

| Region | UV box (x, y, w, h) | Faces |
|---|---|---|
| Head | (0,0) – (32,16) | top/bottom row y 0–8, then right/front/left/back y 8–16; front face (8,8)–(16,16) |
| Hat overlay | (32,0) – (64,16) | same arrangement |
| Right leg | (0,16) – (16,32) | front (4,20)–(8,32) |
| Body | (16,16) – (40,32) | front (20,20)–(28,32) |
| Right arm | (40,16) – (56,32) | front (44,20)–(48,32); the left arm/leg mirror the right |

The 128-pixel files double every coordinate.

## Mob textures

### `textures/entity/army_soldier.png` (128×128)
- **Users:** Army Soldier, and as the hidden base skin of Outcast, Psycho Steve, Commando and Stormtrooper. Also the Army Soldier gib texture.
- **Depicts:** a modern soldier.
  - **Head:** short dark-brown hair on top, back and sides; tanned skin face with blue eyes and a small mouth.
  - **Hat layer:** empty except a small rear patch.
  - **Torso:** grey-green camouflage fatigue shirt with two chest pouches, a darker load-bearing vest/belt band and a slightly darker collar.
  - **Arms:** grey-green upper sleeve with a tiny red-white-blue flag patch, and bare tanned forearms.
  - **Legs:** dark grey-green trousers with darker cargo pockets.
- **Palette:** olive/grey-green `#405040`–`#607060`, tan skin `#a06040`, dark brown hair `#402010`, near-black `#202020` shadows.
- Hand-shaded, noisy texture at 2× resolution.

### `textures/entity/zombie_soldier.png` (128×128)
- **Users:** Zombie Soldier, Zombie Farmer and Zombie Miner. Also their gib texture.
- **Depicts:** an undead soldier.
  - **Head:** mottled bright-green rotting skin, small glowing **red eyes**, dark bloody patches and smears.
  - **Body:** torn grey-brown fatigues spattered with blood, with a bloody wound in the chest.
  - **Arms:** green skin with ragged bloody sleeve ends.
  - **Legs:** grey-brown trousers, darker near the boots.
- **Palette:** zombie green `#408020`–`#60a040`, drab grey-brown `#504838`, blood red `#a01010`–`#c02020`, dark `#202020`.

### `textures/entity/bandit.png` (64×64)
- **Users:** Bandit, plus the Bandit gib texture.
- **Depicts:** a rough desert raider.
  - **Head:** dark brown skin, long thick black hair or dreadlocks covering the top, back and sides, white eyes and a thin dark beard line.
  - **Body:** a light-grey sleeveless shirt or vest, with a brown neck/collar triangle.
  - **Arms:** bare brown, with light-grey shoulders.
  - **Legs:** dark brown trousers with black boots.
- **Palette:** black hair `#101010`, brown skin `#604020`, light grey `#a0a0a0`, dark brown `#402010`.

### `textures/entity/dictator.png` (64×64)
- **User:** Dictator Dave.
- **Depicts:** the same base as the bandit (brown skin, black hair), with **black sunglasses** and a moustache-beard.
  - **Body:** a light-grey shirt with a black belt line across the bottom.
  - **Arms:** bare, the same as the bandit's.
  - **Legs:** dark brown trousers and boots.
- In-game the beret, chestplate, leggings and boots armour pieces cover most of the skin.
- **Palette:** the same as bandit.png.

### `textures/entity/cyberdemon.png` (128×128, fully opaque on a black background)
- **User:** [`ModelCyberDemon`](rendering-and-models.md#73-modelcyberdemon-extends-modelbiped). Also the Cyberdemon gib texture.
- **Depicts:** a skinless demon. Saturated **red muscle flesh** with darker red striations, overlaid with **grey cybernetic implants** and **tan/orange cables**.
- **UV regions** (64-unit coordinates; 2× in the file):

| Region | UV | Contents |
|---|---|---|
| `head` 8×8×7 | (0,0) | Red flesh face with two dark eye sockets. One eye is replaced by a grey metal implant plate with a glowing red lens and a cable. |
| `h6/h7` horns 4×3×3 | (24,0) | Pale bone/stone grey |
| `h4/h8` horn tips 2×2×5 | (30,8) | Pale bone |
| `h2` muzzle 6×5×2 | (48,0) | Red flesh with a row of white/red jagged teeth |
| `h3/h5` side implants 2×4×4 | (52,7) | Dark red |
| `body` chest 12×7×6 | (0,15) | Red muscled chest; a grey metal box implant with cables |
| `b2` waist 9×5×5 | (0,28) | Red with a striped grey/red rib implant and a cable to a black socket |
| Upper arms 5×7×5 | (44,16) | Red flesh with a dark port |
| Forearms 4×8×4 | (48,28) | Red flesh |
| Thighs 5×7×5 | (0,38) | Red flesh |
| Shins 4×7×4 | (20,39) | Grey-white metal plating with orange cables (cyber-legs) |
| Feet 5×3×5 | (0,50) | Grey metal hooves |

- **Palette:** blood red `#800000`–`#c02020`, dark red `#400000`, black `#000000` background, steel grey `#808080`–`#a0a0a0`, cable orange `#c08040`.

### `textures/entity/supermutant_texture_1.png`, `_2.png`, `_3.png` (128×128 each)

| File | User | Differences |
|---|---|---|
| `textures/entity/supermutant_texture_1.png` | Super Mutant Basic | Bare skin; chest-plate and helmet areas transparent |
| `textures/entity/supermutant_texture_2.png` | Super Mutant Heavy | Adds leather **straps or harness** across the chest and back, and grey riveted **metal chest plates** (front and back plate UVs filled) |
| `textures/entity/supermutant_texture_3.png` | Super Mutant Elite | As _2, plus a grey **steel helmet** in the hat-layer UVs (with a magenta-transparent eye-slit notch) and grey **steel pauldrons** over the upper-arm sleeve UVs |

**Shared look:**
- **Skin:** mottled **mustard-yellow** (`#a08020`–`#c0a030`) with darker olive vein and scar line patterns.
- **Face:** brutish, with heavy brows, red-rimmed yellow eyes and a snarling mouth with teeth.
- **Torso:** etched rib/sinew lines.
- **Legs:** the upper legs are skin with a dark band; the boots are dark-brown leather with a black pocket. A dark-brown belt with a silver buckle sits around the waist.

**UV regions** (64-unit coordinates):

| Region | UV |
|---|---|
| Head | (0,0) |
| Helmet | (32,0) |
| Body | (16,16) |
| Arms | (40,16) |
| Legs | (0,16) |
| Boots | (0,33) |
| Chest plates | front (21,34), back (21,44) |
| Upper-arm sleeves | (40,33) |
| Belt | (0,53) |

### `textures/entity/alienbug.png` (64×64, about 34% opaque)
- **User:** [`ModelAlienBug`](rendering-and-models.md#75-modelalienbug-extends-modelbase-tabula-411-export). Also the bug gib texture.
- **Depicts:** a sand-coloured chitinous insect.
- **UV regions:**

| Region | UV | Contents |
|---|---|---|
| Lower legs 11×2×2 | (0,0) | Pale grey-white tips |
| Upper legs 9×3×3 | (0,4) | Tan with darker joints |
| Head 5×5×6 | (20,9) | Tan |
| Upper mandible 4×4×10 | (2,15) | Tan, with two small black eyes and a row of pale teeth along the edge |
| Lower mandible 3×4×9 | (21,20) | Tan with teeth |
| Head crest 6×3×8 | (36,18) | Tan with a dark bracket marking |
| Flat frill plane 14×14 | (22,0) | Mostly transparent; a dark-brown spidery antennae/frill decal |
| Thorax 5×5×9 | (0,33) | Tan |
| Abdomen segments | (23,33), (43,31), (56,30) | Tan with darker banding |

- **Palette:** sand/tan `#a08040`–`#c0a060`, darker brown `#604020`, pale bone `#d0c8b0`, black eye dots.

### `textures/entity/apache.png` (256×256, opaque)
- **User:** `models/entity/apache.obj` (Attack Helicopter).
- **Depicts:** an olive-drab military helicopter skin atlas.
  - The background is mottled dark olive green (`#404020`–`#506030`) with fine light panel lines and rivet dots.
- **Distinct islands:**
  - a long black/dark-grey rubber-like strip, the rotor blade (upper middle);
  - a grey ribbed cylinder and a dark-grey block (engine/exhaust, upper left);
  - a black circular port, the gun muzzle or sensor (centre);
  - four **light-blue glass** panes with white glint streaks, one triangular (cockpit canopy, centre right);
  - a 2×2 grid of red and black dots, the rocket pod tubes (lower left);
  - a black square with a dot (lower left);
  - a darker olive square panel (lower right).

## Non-mob textures stored in `textures/entity/`

| Path | Size | Depicts | Palette | Used by (owner) |
|---|---|---|---|---|
| `textures/entity/gore.png` | 128×128, top half used (biped layout) | A generic flayed body: dark red raw flesh and blood in a biped skin layout, with darker clotted areas and bright-red highlights | `#600000`, `#802020`, `#400000` | Generic gib fallback in [`DeathEffect`](../01-core/client-effects-audio.md#deatheffectcreatedeatheffectentity-type-mx-my-mz) ([04](../04-projectiles-fx/index.md)) |
| `textures/entity/BlueBeam.png` | 32×32, opaque | Horizontal glow-beam gradient: noisy dark blue edges, through mid blue, to a white-hot centre line | `#004080` → `#2060c0` → white | [`RenderRailgunProjectile`](../04-projectiles-fx/projectiles.md#1-registration) ([04](../04-projectiles-fx/index.md)) |
| `textures/entity/Laser.png` | 32×32, opaque | The same gradient in red: noisy dark red edges, through orange and yellow, to a white centre line | `#c00000` → `#e06000` → white | [`RenderBlasterProjectile`](../04-projectiles-fx/projectiles.md#1-registration) ([04](../04-projectiles-fx/index.md)) |
| `textures/entity/bioblob.png` | 64×64, round with transparent corners | A glossy lime-green sphere with swirl highlights and specular dots | `#408000`, `#206000`, `#a0e0a0` | [`RenderBiogunProjectile`](../04-projectiles-fx/textures.md#projectile-entity-textures-outside-fx-bound-by-renderers-in-this-area) ([04](../04-projectiles-fx/index.md)) |
| `textures/entity/bullet.png` | 32×32, mostly transparent | Two small sprites at the top left: a pale brass/cream elongated tracer with a pointed tail, and below it an orange copy | `#c0c080`, `#e06000` | [`RenderGenericBullet`](../04-projectiles-fx/projectiles.md#1-registration), [`RenderSonicShotgunProjectile`](../04-projectiles-fx/projectiles.md#1-registration) ([04](../04-projectiles-fx/index.md)) |
| `textures/entity/handgunBullet.png` | 16×16 | A grey shaded stone/lead ball with a lit top-left | `#a0a0a0` → `#202020` | [`SimpleProjectile`](../04-projectiles-fx/projectiles.md#31-simpleprojectile) (handcannon stone bullets) ([04](../04-projectiles-fx/index.md)) |
| `textures/entity/launchergrenade.png` | 32×16, opaque | A strip map for the 40 mm grenade: a cream/brass casing section, a red band, a brass section, and a small pale cap square at the top right | `#e0e0a0`, `#c04030`, `#a0a060` | `launchergrenade.obj` via [`RenderGrenadeLauncherProjectile`](../04-projectiles-fx/textures.md#projectile-entity-textures-outside-fx-bound-by-renderers-in-this-area) ([04](../04-projectiles-fx/index.md)) |
| `textures/entity/reactionchamber.png` | 256×256, opaque | Machine atlas on black: white and light-grey sci-fi panels with bevels; a large square frame with a cross-shaped black port and hazard-striped corners; dark grey vent grids; small grilles; a yellow-black hazard marker; a light-blue glass tile | white `#e0e0e0`, greys, black | `reactionchamber.obj` TESR ([08](../08-machines-production/index.md)) |
| `textures/entity/reactionchamberglass.png` | 128×128, ~4% opaque | Transparent glass: two large rectangles outlined in light grey, with diagonal white glint streaks and corner brackets | `#c0c0c0` on transparent | Reaction-chamber glass ([08](../08-machines-production/index.md)) |

## Spawn-egg item textures (registered in `TGEntities`)

| Path | Size | Depicts | Use |
|---|---|---|---|
| [`textures/items/spawn_egg.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items) | 32×32 | An egg silhouette in **greyscale** mottled stone/speckle shading with a dark outline. Transparent outside the egg. | Render pass 0, tinted by the egg's base colour |
| [`textures/items/spawn_egg_overlay.png`](../06-items/textures.md#11-tools-placers-door-bucket-debug-items) | 32×32 | The letters **"TG"** in white with a dark-grey 1-px outline, centred in the lower-middle of the egg area. The rest is transparent. | Render pass 1, tinted by the egg's spot colour |

Replacement art should keep the overlay greyscale or white so tinting works, and keep the egg shape within the 32×32 canvas.
