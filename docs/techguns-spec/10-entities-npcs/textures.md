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
- **Measured:** 128×128, 28% opaque, average `#5C4D3E`, dominant #505050 20%, #303030 13%, #B07050 7% ([ASSET-FACTS.md](../ASSET-FACTS.md)).
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
- **Measured:** 128×128, 28% opaque, average `#484E2B`, dominant #303030 13%, #307030 11%, #507030 8% ([ASSET-FACTS.md](../ASSET-FACTS.md)).
- **Users:** Zombie Soldier, Zombie Farmer and Zombie Miner. Also their gib texture.
- **Depicts:** an undead soldier.
  - **Head:** mottled bright-green rotting skin, small glowing **red eyes**, dark bloody patches and smears.
  - **Body:** torn grey-brown fatigues spattered with blood, with a bloody wound in the chest.
  - **Arms:** green skin with ragged bloody sleeve ends.
  - **Legs:** grey-brown trousers, darker near the boots.
- **Palette:** zombie green `#408020`–`#60a040`, drab grey-brown `#504838`, blood red `#a01010`–`#c02020`, dark `#202020`.

### `textures/entity/bandit.png` (64×64)
- **Measured:** 64×64, 28% opaque, average `#5B514B`, dominant #101010 25%, #B0B0B0 18%, #909090 12% ([ASSET-FACTS.md](../ASSET-FACTS.md)).
- **Users:** Bandit, plus the Bandit gib texture.
- **Depicts:** a rough desert raider.
  - **Head:** dark brown skin, long thick black hair or dreadlocks covering the top, back and sides, white eyes and a thin dark beard line.
  - **Body:** a light-grey sleeveless shirt or vest, with a brown neck/collar triangle.
  - **Arms:** bare brown, with light-grey shoulders.
  - **Legs:** dark brown trousers with black boots.
- **Palette:** black hair `#101010`, brown skin `#604020`, light grey `#a0a0a0`, dark brown `#402010`.

### `textures/entity/dictator.png` (64×64)
- **Measured:** 64×64, 28% opaque, average `#594F49`, dominant #101010 27%, #B0B0B0 17%, #705030 13% ([ASSET-FACTS.md](../ASSET-FACTS.md)).
- **User:** Dictator Dave.
- **Depicts:** the same base as the bandit (brown skin, black hair), with **black sunglasses** and a moustache-beard.
  - **Body:** a light-grey shirt with a black belt line across the bottom.
  - **Arms:** bare, the same as the bandit's.
  - **Legs:** dark brown trousers and boots.
- In-game the beret, chestplate, leggings and boots armour pieces cover most of the skin.
- **Palette:** the same as bandit.png.

### `textures/entity/cyberdemon.png` (128×128, fully opaque on a black background)
- **Measured:** 128×128, 100% opaque, average `#38120D`, dominant #101010 51%, #701010 13%, #501010 10% ([ASSET-FACTS.md](../ASSET-FACTS.md)).
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

- **Measured, `_1`:** 128×128, 42% opaque, average `#987A22`, dominant #B09030 26%, #B09010 13%, #D09030 11% ([ASSET-FACTS.md](../ASSET-FACTS.md)).
- **Measured, `_2`:** 128×128, 45% opaque, average `#907525`, dominant #B09030 23%, #B09010 11%, #503030 10% ([ASSET-FACTS.md](../ASSET-FACTS.md)).
- **Measured, `_3`:** 128×128, 51% opaque, average `#877130`, dominant #B09030 18%, #505050 10%, #707070 9% ([ASSET-FACTS.md](../ASSET-FACTS.md)).

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
- **Measured:** 64×64, 34% opaque, average `#AF8A56`, dominant #B09050 28%, #D0B070 14%, #D09050 7% ([ASSET-FACTS.md](../ASSET-FACTS.md)).
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
- **Measured:** 256×256, 100% opaque, average `#3A402A`, dominant #505030 26%, #303030 24%, #305030 22% ([ASSET-FACTS.md](../ASSET-FACTS.md)).
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

## Measured sizes and colours (all 19 entity textures and the 2 spawn-egg textures)

Numbers come from [ASSET-FACTS.md](../ASSET-FACTS.md): the average is the mean of opaque pixels (alpha above 16); the dominant colours are the three most common colours after bucketing each channel into 8 levels, with their share of opaque pixels. None of these files has a `.mcmeta`, so none is animated. The last column is a palette in words derived from those measurements.

| Path | Size | Opaque | Average colour | Dominant colours | Palette in words |
|---|---|---|---|---|---|
| `textures/entity/BlueBeam.png` | 32×32 | 100% | `#5083B9` | #105090 23%, #105070 14%, #F0F0F0 9% | saturated blues (`#105090`, `#105070`) with a white core (`#F0F0F0`); a mid blue mean |
| `textures/entity/Laser.png` | 32×32 | 100% | `#F85D26` | #F01010 31%, #F07010 12%, #D01010 9% | saturated reds (`#F01010`, `#D01010`) with orange (`#F07010`) and a white core; a bright red-orange mean |
| `textures/entity/alienbug.png` | 64×64 | 34% | `#AF8A56` | #B09050 28%, #D0B070 14%, #D09050 7% | sand and tan chitin (`#B09050`, `#D0B070`, `#D09050`); a warm mid-tan mean |
| `textures/entity/apache.png` | 256×256 | 100% | `#3A402A` | #505030 26%, #303030 24%, #305030 22% | olive-drab hull (`#505030`, `#305030`) with dark grey panels (`#303030`); a very dark olive mean |
| `textures/entity/army_soldier.png` | 128×128 | 28% | `#5C4D3E` | #505050 20%, #303030 13%, #B07050 7% | neutral dark greys (`#505050`, `#303030`) for the fatigues and shading, tan skin (`#B07050`); the mean is a muddy brown-olive |
| `textures/entity/bandit.png` | 64×64 | 28% | `#5B514B` | #101010 25%, #B0B0B0 18%, #909090 12% | black hair and boots, light grey vest (`#B0B0B0`, `#909090`), brown skin; a high-contrast black and grey texture whose mean is a warm dark grey |
| `textures/entity/bioblob.png` | 64×64 | 73% | `#57A432` | #509010 18%, #307010 14%, #305010 8% | lime and forest greens (`#509010`, `#307010`, `#305010`); a mid green mean |
| `textures/entity/bullet.png` | 32×32 | 18% | `#C68E47` | #B05010 17%, #F07010 13%, #D07010 11% | burnt orange and amber sprites (`#B05010`, `#F07010`, `#D07010`); a warm brass mean |
| `textures/entity/cyberdemon.png` | 128×128 | 100% | `#38120D` | #101010 51%, #701010 13%, #501010 10% | black background (51% of opaque pixels), then blood-red flesh (`#701010`, `#501010`); the mean is a very dark red |
| `textures/entity/dictator.png` | 64×64 | 28% | `#594F49` | #101010 27%, #B0B0B0 17%, #705030 13% | the bandit palette plus a larger brown band (`#705030`) for the skin; black hair and sunglasses are the largest colour |
| `textures/entity/gore.png` | 128×128 | 28% | `#761E18` | #701010 29%, #501010 19%, #903030 16% | blood-red flesh (`#701010`, `#501010`, `#903030`); a dark red mean |
| `textures/entity/handgunBullet.png` | 16×16 | 82% | `#616161` | #B0B0B0 17%, #707070 16%, #303030 16% | neutral greys from `#B0B0B0` to `#303030`; a mid grey mean |
| `textures/entity/launchergrenade.png` | 32×16 | 100% | `#967953` | #101010 12%, #D0D090 7%, #B0B070 6% | near-black outline (`#101010`), then cream-brass (`#D0D090`, `#B0B070`); a muted brass mean |
| `textures/entity/reactionchamber.png` | 256×256 | 100% | `#3B3B3B` | #101010 64%, #F0F0F0 9%, #303030 7% | black background (64%), white panels (`#F0F0F0`) and dark grey; a dark grey mean |
| `textures/entity/reactionchamberglass.png` | 128×128 | 4% | `#C7C7C7` | #D0D0D0 99%, #B0B0B0 0% | almost uniform pale grey (`#D0D0D0`, 99%) on a 96% transparent field |
| `textures/entity/supermutant_texture_1.png` | 128×128 | 42% | `#987A22` | #B09030 26%, #B09010 13%, #D09030 11% | mustard-yellow skin (`#B09030`, `#B09010`, `#D09030`); mean is dark mustard |
| `textures/entity/supermutant_texture_2.png` | 128×128 | 45% | `#907525` | #B09030 23%, #B09010 11%, #503030 10% | mustard skin as texture 1 plus dark brown leather straps (`#503030`) in the third dominant slot |
| `textures/entity/supermutant_texture_3.png` | 128×128 | 51% | `#877130` | #B09030 18%, #505050 10%, #707070 9% | mustard skin plus steel greys (`#505050`, `#707070`) from the helmet and pauldrons |
| `textures/entity/zombie_soldier.png` | 128×128 | 28% | `#484E2B` | #303030 13%, #307030 11%, #507030 8% | rotting green skin (`#307030`, `#507030`) over dark grey-brown fatigues; mid-dark olive overall |
| `textures/items/spawn_egg.png` | 32×32 | 50% | `#898989` | #B0B0B0 23%, #909090 21%, #707070 14% | light greys (`#B0B0B0`, `#909090`, `#707070`) only: a greyscale egg meant to be tinted |
| `textures/items/spawn_egg_overlay.png` | 32×32 | 16% | `#939393` | #505050 55%, #F0F0F0 35%, #909090 8% | dark grey outline (`#505050`, 55%) and white letters (`#F0F0F0`, 35%): greyscale, meant to be tinted |

The 21 rows above are every texture this area owns: 19 in `textures/entity/` and 2 in `textures/items/`. Of the 19, 10 are mob textures (`army_soldier`, `zombie_soldier`, `bandit`, `dictator`, `cyberdemon`, the three `supermutant_texture_*`, `alienbug` and `apache`); the other 9 belong to projectiles, machines or the gib fallback (see the table above).

## UV regions as pixel rectangles

A box of size W×H×D with UV origin (u, v) unfolds into a cross that covers a rectangle (2D+2W) wide by (D+H) high pixels starting at (u, v). Each model declares a logical 64×64 texture, so a 128×128 file multiplies every coordinate by 2 and a 64×64 file uses them as they are. The rectangles below are derived that way from the box sizes in [rendering-and-models.md](rendering-and-models.md#7-models); they were computed, not read back from the PNGs, but the measured opaque bounding boxes given for each file agree with them.

### Biped skins: `bandit.png` and `dictator.png` (64×64, scale 1); `army_soldier.png` and `zombie_soldier.png` (128×128, scale 2)

Measured opaque bounding box: `bandit.png` (0,0)–(56,32) of the 64×64 canvas and `army_soldier.png` (0,0)–(112,64) of 128×128, so only the top half is used (a 64×32 layout) and the lower half is transparent.

**64×64 files (scale 1):**

| Region | Logical UV origin, box W×H×D | Pixel rectangle (x, y) W×H in the file | Role |
|---|---|---|---|
| Head | (0,0) 8×8×8 | (0,0) 32×16 | Head box |
| Hat layer | (32,0) 8×8×8 | (32,0) 32×16 | Overlay, inflated 0.5 |
| Body | (16,16) 8×12×4 | (16,16) 24×16 | Torso |
| Right arm (left arm mirrors it) | (40,16) 4×12×4 | (40,16) 16×16 | Arm |
| Right leg (left leg mirrors it) | (0,16) 4×12×4 | (0,16) 16×16 | Leg |

**128×128 files (scale 2):**

| Region | Logical UV origin, box W×H×D | Pixel rectangle (x, y) W×H in the file | Role |
|---|---|---|---|
| Head | (0,0) 8×8×8 | (0,0) 64×32 | Head box |
| Hat layer | (32,0) 8×8×8 | (64,0) 64×32 | Overlay, inflated 0.5 |
| Body | (16,16) 8×12×4 | (32,32) 48×32 | Torso |
| Right arm (left arm mirrors it) | (40,16) 4×12×4 | (80,32) 32×32 | Arm |
| Right leg (left leg mirrors it) | (0,16) 4×12×4 | (0,32) 32×32 | Leg |

Face sub-rectangles (logical coordinates; multiply by 2 for the 128 files):

| Face | Logical rectangle (x, y, w, h) |
|---|---|
| Head top | (8,0,8,8) |
| Head bottom | (16,0,8,8) |
| Head right | (0,8,8,8) |
| Head front | (8,8,8,8) |
| Head left | (16,8,8,8) |
| Head back | (24,8,8,8) |
| Body top | (20,16,8,4) |
| Body bottom | (28,16,8,4) |
| Body right | (16,20,4,12) |
| Body front | (20,20,8,12) |
| Body left | (28,20,4,12) |
| Body back | (32,20,8,12) |
| Arm front | (44,20,4,12) |
| Leg front | (4,20,4,12) |

### `cyberdemon.png` (128×128, scale 2; logical 64×64)

The whole canvas is opaque (measured bounding box (0,0)–(128,128)); the black background (51% of pixels) is the unused area.

| Region | Logical UV origin, box W×H×D | Pixel rectangle (x, y) W×H in the file | Role |
|---|---|---|---|
| `head` | (0,0) 8×8×7 | (0,0) 60×30 | Skull |
| `h6`/`h7` horns | (24,0) 4×3×3 | (48,0) 28×12 | Horns |
| `h4`/`h8` horn tips | (30,8) 2×2×5 | (60,16) 28×14 | Horn tips |
| `h2` muzzle | (48,0) 6×5×2 | (96,0) 32×14 | Toothed muzzle |
| `h3`/`h5` side implants | (52,7) 2×4×4 | (104,14) 24×16 | Temple implants |
| `body` | (0,15) 12×7×6 | (0,30) 72×26 | Chest |
| `b2` | (0,28) 9×5×5 | (0,56) 56×20 | Waist |
| upper arms | (44,16) 5×7×5 | (88,32) 40×24 | Arms |
| forearms `ra2`/`la2` | (48,28) 4×8×4 | (96,56) 32×24 | Forearms |
| thighs | (0,38) 5×7×5 | (0,76) 40×24 | Thighs |
| shins `rl2`/`ll2` | (20,39) 4×7×4 | (40,78) 32×22 | Shins |
| feet `rl3`/`ll3` | (0,50) 5×3×5 | (0,100) 40×16 | Hooves |

### `supermutant_texture_1.png`, `_2.png`, `_3.png` (128×128, scale 2; logical 64×64)

Measured opaque bounding boxes: `_1` (0,0)–(120,125) and `_3` (0,0)–(128,125). The lower part of the canvas is used (boots, plates, belt), unlike the plain biped skins.

| Region | Logical UV origin, box W×H×D | Pixel rectangle (x, y) W×H in the file | Role |
|---|---|---|---|
| Head | (0,0) 8×8×8 | (0,0) 64×32 | Head |
| Helmet (hat layer) | (32,0) 8×8×8 | (64,0) 64×32 | Painted on `_3` only |
| Body | (16,16) 8×12×4 | (32,32) 48×32 | Torso |
| Arms | (40,16) 4×12×4 | (80,32) 32×32 | Arms |
| Legs | (0,16) 4×12×4 | (0,32) 32×32 | Legs |
| Boots | (0,33) 5×6×5 | (0,66) 40×22 | Boot sleeve over each leg |
| Chest plate, front | (21,34) 6×7×1 | (42,68) 28×16 | Plate (on `_2`, `_3`) |
| Chest plate, back | (21,44) 6×7×1 | (42,88) 28×16 | Plate (on `_2`, `_3`) |
| Upper-arm sleeve | (40,33) 5×7×5 | (80,66) 40×24 | Sleeve; pauldron on `_3` |
| Belt | (0,53) 9×4×5 | (0,106) 56×18 | Waist belt and buckle |

### `alienbug.png` (64×64, scale 1)

Measured opaque bounding box (0,0)–(64,47); the bottom quarter is empty. Opaque share is 34%.

| Region | Logical UV origin, box W×H×D | Pixel rectangle (x, y) W×H in the file | Role |
|---|---|---|---|
| Lower legs `L1_B*` | (0,0) 11×2×2 | (0,0) 26×4 | Four lower legs |
| Upper legs `L1_A*` | (0,4) 9×3×3 | (0,4) 24×6 | Four upper legs |
| Head `h1` | (20,9) 5×5×6 | (20,9) 22×11 | Head |
| Upper mandible `JAW_U` | (2,15) 4×4×10 | (2,15) 28×14 | Upper jaw, eyes, teeth |
| Lower mandible `JAW_L` | (21,20) 3×4×9 | (21,20) 24×13 | Lower jaw |
| Crest `h2` | (36,18) 6×3×8 | (36,18) 28×11 | Head crest |
| Thorax `b1` | (0,33) 5×5×9 | (0,33) 28×14 | Thorax |
| Abdomen `b2` | (23,33) 6×4×4 | (23,33) 20×8 | Abdomen segment 1 |
| Segment `b3` | (43,31) 4×3×4 | (43,31) 16×7 | Abdomen segment 2 |
| Tail tip `b4` | (56,30) 2×2×2 | (56,30) 8×4 | Tail |

The flat frill plane `h3` is 14×14 with zero thickness at UV origin (22,0). The unfolding formula gives a 28×14 footprint for a zero-height box (2·14 wide, 14 deep), roughly (22,0)–(50,14), which overlaps the lower-leg rectangle (0,0,26,4) at the top left by a few pixels. The overlap comes from the generic formula; a flat plane most likely only samples the 14×14 area from (22,0), so it need not be real.


### `apache.png` (256×256), regions from the OBJ texture coordinates

The OBJ has 720 texture coordinates in three groups. Taking the bounding box of each group's `vt` values (u × 256, (1 − v) × 256, assuming the usual OBJ bottom-left origin) gives the pixel rectangles below. These are bounding boxes of scattered islands, not single cells.

| OBJ group | Face references with a texture coordinate | Bounding rectangle (x, y) to (x, y) | Role |
|---|---|---|---|
| `HELCOPTER` | 864 | (3,45) to (255,254) | Fuselage, cockpit glass, engine, rocket pods and tail; practically the whole lower and right atlas |
| `ROTOR` | 120 | (23,29) to (230,89) | The long rubber-black rotor strip in the upper middle of the atlas |
| `GUN` | 60 | (34,219) to (57,247) | The chin gun, in the lower left of the atlas |

The group name `HELCOPTER` is spelled that way in the OBJ file.

### Non-model textures: measured bounding boxes

| File | Size | Opaque bounding box measured | Notes |
|---|---|---|---|
| `textures/entity/gore.png` | 128×128 | (0,0)–(112,64) | Biped layout in the top half, scale 2 |
| `textures/entity/bullet.png` | 32×32 | (0,0)–(16,20), one connected shape of 186 opaque pixels | The two sprites (brass tracer above, orange copy below) sit in the top-left 16×20 area; the rest is transparent |
| `textures/entity/handgunBullet.png` | 16×16 | (0,0)–(16,16), 212 opaque pixels | A single ball sprite filling the canvas |
| `textures/entity/bioblob.png` | 64×64 | (1,4)–(64,64), 2,993 opaque pixels, one component | A round sprite; the corners are transparent |
| `textures/entity/launchergrenade.png` | 32×16 | Fully opaque; the OBJ group `Cylinder01` has texture coordinates spanning (0,0)–(32,16) | The whole strip is one cylinder map |
| `textures/entity/reactionchamberglass.png` | 128×128 | Large pane at (0,0) size 128×78 (532 opaque pixels); small marks at (35,25) 16×16, (7,9) 11×11, (105,43) 11×11, (45,59) 11×11, and several 5×5 corner brackets such as (57,2), (121,2) and (66,57) | 15 connected shapes over 4% of the canvas |
| `textures/entity/BlueBeam.png`, `textures/entity/Laser.png` | 32×32 | Fully opaque | Gradient strips; the bright line is in the centre |
| `textures/entity/reactionchamber.png` | 256×256 | Fully opaque | Machine atlas; its regions are owned by [08-machines-production](../08-machines-production/index.md) |

### Spawn-egg textures: measured bounding boxes

| File | Size | Opaque bounding box | Regions |
|---|---|---|---|
| `textures/items/spawn_egg.png` | 32×32 | (5,3), 24×28, one connected shape of 515 pixels (50% of the canvas) | The egg silhouette, centred slightly right and low; the rest is transparent |
| `textures/items/spawn_egg_overlay.png` | 32×32 | (7,11), 20×14, 169 pixels (16% of the canvas) | The "TG" lettering with its outline, in the lower middle of the egg; drawn in render pass 1 over the egg |
