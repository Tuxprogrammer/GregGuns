# Rendering and models (living entities)

Source files: `client/ClientProxy.registerRenderers` (bindings), `client/renderer/entity/RenderGenericNPC.java`, `RenderSuperMutant.java`, `RenderAlienBug.java`, `RenderHelicopter.java`, `RenderSimpleTurret.java`, `entities/renderer/RenderTurret.java`, and the models `client/models/ModelGenericNPC.java`, `ModelSkeletonSoldier.java`, `ModelCyberDemon.java`, `ModelSuperMutant.java`, `ModelAlienBug.java`, `ModelNull.java`, `ModelGrimReaper.java` and `client/models/blocks/ModelTurret.java`. The OBJ models are `models/entity/*.obj`.

Projectile renderers (including `RenderDummy`, which is also bound to the invisible particle-system entity) and the gib/death effects belong to 04-projectiles-fx. Texture details for every file named here are in [textures.md](textures.md).

## 1. Renderer bindings

| Entity class | Renderer | Model | Texture | Shadow | Gun offset (x, y, z) |
|---|---|---|---|---|---|
| `GenericNPC` (fallback; also used by `TGDummySpawn`) | RenderGenericNPC | ModelGenericNPC | vanilla `minecraft:textures/entity/zombie/zombie.png` | 0.5 | 0, 0, 0 |
| `ZombieSoldier` | RenderGenericNPC | ModelGenericNPC | `textures/entity/zombie_soldier.png` | 0.5 | 0 |
| `ZombieFarmer` | RenderGenericNPC | ModelGenericNPC | `textures/entity/zombie_soldier.png` | 0.5 | 0 |
| `ZombieMiner` | RenderGenericNPC | ModelGenericNPC | `textures/entity/zombie_soldier.png` | 0.5 | 0 |
| `ZombiePigmanSoldier` | RenderGenericNPC | ModelGenericNPC | vanilla `minecraft:textures/entity/zombie_pigman.png` | 0.5 | 0 |
| `SkeletonSoldier` | RenderGenericNPC | ModelSkeletonSoldier | vanilla `minecraft:textures/entity/skeleton/skeleton.png` | 0.5 | 0.1, 0, 0.05 |
| `ArmySoldier` | RenderGenericNPC | ModelGenericNPC | `textures/entity/army_soldier.png` | 0.5 | 0 |
| `Outcast` | RenderGenericNPC | ModelGenericNPC | `textures/entity/army_soldier.png` (hidden under power armour) | 0.5 | 0 |
| `PsychoSteve` | RenderGenericNPC | ModelGenericNPC | `textures/entity/army_soldier.png` (under miner armour) | 0.5 | 0 |
| `Commando` | RenderGenericNPC | ModelGenericNPC | `textures/entity/army_soldier.png` (under commando armour) | 0.5 | 0 |
| `StormTrooper` | RenderGenericNPC | ModelGenericNPC | `textures/entity/army_soldier.png` (under HEV armour) | 0.5 | 0 |
| `Bandit` | RenderGenericNPC | ModelGenericNPC | `textures/entity/bandit.png` | 0.5 | 0 |
| `DictatorDave` | RenderGenericNPC | ModelGenericNPC | `textures/entity/dictator.png` | 0.5 | 0 |
| `CyberDemon` | RenderGenericNPC | ModelCyberDemon | `textures/entity/cyberdemon.png` | 0.75 | −0.11, 0.15, −0.18 |
| `SuperMutantBasic` | RenderSuperMutant (type 0) | ModelSuperMutant | `textures/entity/supermutant_texture_1.png` | 0.6 | −0.1, 0.3, 0 |
| `SuperMutantHeavy` | RenderSuperMutant (type 1) | ModelSuperMutant | `textures/entity/supermutant_texture_2.png` | 0.65 | −0.15, 0.3, 0 |
| `SuperMutantElite` | RenderSuperMutant (type 2) | ModelSuperMutant | `textures/entity/supermutant_texture_3.png` | 0.7 | −0.2, 0.4, 0 |
| `AlienBug` | RenderAlienBug | ModelAlienBug | `textures/entity/alienbug.png` | 1.0 | – |
| `AlienBugDomesticated` (unregistered) | RenderAlienBug | ModelAlienBug | `textures/entity/alienbug.png` | 1.0 | – |
| `AttackHelicopter` | RenderHelicopter | OBJ `models/entity/apache.obj` | `textures/entity/apache.png` | 5.0 | – |
| `NPCTurret` | RenderSimpleTurret | ModelTurret | `textures/blocks/TurretBase.png` … `_5.png` by armour (owned by 09) | none | turret offsets |

`entities/renderer/RenderTurret` is a minimal `RenderLiving` with a single static texture. It is not bound to anything; the binding using it for the unused turret entity is commented out.

## 2. `RenderGenericNPC` (extends vanilla `RenderBiped`)

- **Base behaviour:** vanilla biped rendering, including the four armour layers through RenderBiped's armour models. Techguns armour supplies its own armour models and textures (05-armor). The constructor scale is 1.0.
- **`preRenderCallback`:** if `getHasAimedBowAnim()` is true and the held item is a `GenericGun`, it sets `aimedBow = true` on both armour models (the inner and outer armour bipeds). A Forge render-pre event in `TechgunsEventhandler` sets the same flag on the main model, using the entity's `hasAimedBowAnim` (or true for non-GenericNPC shooters). Together they produce the two-handed aiming pose.
- **`getEntityTexture`:** returns the fixed texture given at construction.
- **`renderEquippedItems`**, a re-implementation of the vanilla one:
  - **Helmet slot:** a block item, or an item whose `IItemRenderer` wants BLOCK_3D, is drawn on the head the vanilla way. A vanilla skull is drawn with its owner profile. The vanilla code that draws the pumpkin/head is kept.
  - **Held item**, attached at the right arm's `postRender`, then translated (−1/16, 7/16, 1/16):
    - **Block items** (3D): translate (0, 3/16, −5/16), rotate 20° X then 45° Y, scale 0.375 with flipped X/Y (vanilla).
    - **Bow:** vanilla bow transform.
    - **`GenericGun`:**
      1. Translate by the renderer's gun offset (table above).
      2. Translate (0, 0.125, −0.3125).
      3. Rotate −100° about Y.
      4. Scale (0.375, −0.375, 0.375).
      5. Rotate 20° about Z, then 55° about Y.
      6. Render with item render type EQUIPPED, so the gun's own custom renderer (03) draws the model.
    - **Full-3D items** (tools): vanilla transform (scale 0.625, optional 180° flip, −100° X, 45° Y).
    - **Anything else:** vanilla flat-item transform.
  - Multi-pass items are tinted per pass with `getColorFromItemStack`.
- The skeleton offset (0.1, 0, 0.05) compensates for its thin arms. The cyberdemon offset compensates for its bulky arms.

## 3. `RenderSuperMutant` (extends RenderGenericNPC)

- **Construction:** it is given a texture prefix and `type`, and passes `prefix + type + ".png"` to the parent. That builds `supermutant_texture_0.png`, which does not exist, but the result is ignored (NPC-30).
- **`getEntityTexture`:** returns `textures/entity/supermutant_texture_{type+1}.png`.
- **`doRender`:** adds `getModelHeightOffset()` (0.55, 0.75 or 0.95) to the render Y. The model is scaled up about the model origin, which would otherwise sink the feet into the ground.
- **Scaling:** done by the model, which multiplies its render scale by `getModelScale()`. The held gun is still attached through the unscaled arm transform, hence the larger gun offsets.

## 4. `RenderAlienBug` (extends `RenderLiving`)

- Model `ModelAlienBug`, shadow 1.0, texture `textures/entity/alienbug.png`.
- It declares `getDeathMaxRotation(EntityLiving)` returning 180°. That does not override the `EntityLivingBase` signature, so the vanilla 90° death topple is used (NPC-21).

## 5. `RenderHelicopter` (extends `RendererLivingEntity`)

- **Setup:** model `ModelNull` (a `ModelBase` with no boxes) satisfies the superclass. The real geometry is the OBJ loaded through Forge `AdvancedModelLoader` from `techguns:models/entity/apache.obj`. Texture `techguns:textures/entity/apache.png`. Shadow 5.0.
- **`doRender`:** fully replaces the living-renderer pipeline, so there is **no** name tag, no red hurt flash and no vanilla death rotation.
  1. Interpolate body yaw, head yaw and pitch.
  2. **`BossStatus.setBossStatus(entity, true)`**, which shows the boss bar.
  3. Translate to the entity and scale uniformly by **0.25**. One OBJ unit is a quarter block.
  4. Rotate by body yaw about Y and by pitch about Z. The OBJ nose points +X.
  5. Bind the texture.
- **Alive** (`deathTime == 0`):
  - Render every group except `ROTOR` and `GUN`.
  - **Chin gun:** pivot at (16, −1, 0) model units. Yaw by (headYaw − bodyYaw), pitch it down 25° (rotation −25° about Z), then render `GUN`.
  - **Main rotor:** rotate about Y by `((worldTime mod 60) + partialTick) · 24°`, one full turn every 15 ticks, then render `ROTOR`.
- **Dying:** let `p = ((deathTime + partialTick) / maxDeathTime)²`, with `maxDeathTime = 100`.
  - Translate down by `40·p` model units (10 blocks at the end).
  - Spin the whole airframe `720·p` degrees about Y.
  - Render everything except `ROTOR`; the gun stays fixed and unaimed.
  - Render the rotor spinning at half speed (12° per tick).

### `models/entity/apache.obj`
A Max2Obj export ("Max2Obj Version 4.0"): 270 vertices, 348 faces, three groups, UV-mapped to `apache.png`. Bounds are in model units; multiply by 0.25 for blocks.

| Group | Faces | Bounds X | Bounds Y | Bounds Z | Role |
|---|---|---|---|---|---|
| `HELCOPTER` *(sic)* | 206 | −47 … 26 | 2 … 20 | −12 … 12 | Fuselage, tail boom and fin, stub wings and pods, cockpit glazing. About 18 × 4.5 × 6 blocks. |
| `ROTOR` | 48 | −37 … 37 | 13 … 18 | −37 … 37 | Main-rotor hub and blades, about 18.5 blocks in diameter. Spins about the model Y axis through the origin. |
| `GUN` | 16 | 14 … 24 | 0 … 2 | −1 … 1 | Chin cannon barrel under the nose (+X). Pivots at (16, −1, 0). |

The entity hitbox is only 10 × 4 blocks, so the visual model overhangs it considerably.

### Other OBJ files in `models/entity/` (in this folder, used elsewhere)
| File | Groups | Bounds (units) | Used by |
|---|---|---|---|
| `models/entity/launchergrenade.obj` | `Cylinder01` (10 verts, 16 faces) | X −3…5, Y −2…2, Z −2…2 | `GrenadeLauncherProjectile` renderer via `ModelModel` with `textures/entity/launchergrenade.png`. See 04. |
| `models/entity/reactionchamber.obj` | `CHAMBER` (276 verts), `GLASS` (8 verts) | CHAMBER X/Z −48…48, Y 0…128; GLASS X/Z −32…32, Y 34…112 | Reaction Chamber multiblock TESR (`RenderReactionChamber`) with `textures/entity/reactionchamber.png` and `reactionchamberglass.png`. See 08. |

## 6. `RenderSimpleTurret` (extends plain `Render`) for `NPCTurret`

Because it is not a living renderer, there is no name tag, hurt flash, death animation or shadow.

1. Translate to the entity and enable GL_RESCALE_NORMAL.
2. Bind `turret.getTexture()`. This depends on `getTotalArmorValue()`: 0 → `textures/blocks/TurretBase.png`, 5 → `TurretBase_2`, 10 → `_3`, 15 → `_4`, 20 → `_5`; any other value → the base texture.
3. Translate up 1.5, flip 180° about X (standard model-space flip), and rotate by `rotationYawHead` about Y.
4. Render **part 0**: the base plate and two posts.
5. Translate by 0.9, rotate by `rotationPitch` about X, translate back, then render **part 1** (the mount plate). The turret head pitches about a hinge 0.9 units above the model origin.
6. Rotate 170° about Y and draw the held gun:
   - flip 180° X;
   - translate by (0.025, −1.0, 0.3) plus the gun's `turretPosOffsetX/Y/Z` (02);
   - translate (0, 0.125, −0.3125);
   - scale 0.375;
   - rotate −55° Y;
   - render as EQUIPPED, with multi-pass tinting as in RenderGenericNPC.

## 7. Models

All box models use Minecraft pixel units (1/16 block), with Y pointing down from the model origin. Origin Y = 24 is the feet for bipeds. Sizes are W × H × D.

### 7.1 `ModelGenericNPC` (extends `ModelBiped`)
- A standard biped: head 8×8×8, hat layer 8×8×8 inflated 0.5, body 8×12×4, arms and legs 4×12×4, using the standard 64×32 UV layout.
- The default constructor declares the texture as **64 × 64**. The standard layout therefore occupies the top half, and the bottom half of each NPC skin is empty. The HD skins are 128 × 128 at 2× resolution with the same proportions.
- Animation is plain vanilla biped: walk swing, head look, aimed-bow pose and attack swing.

### 7.2 `ModelSkeletonSoldier` (extends ModelGenericNPC)
- Built with model size 0.0625 (head, hat and body slightly inflated) and a **64 × 32** texture.
- Arms and legs are replaced by vanilla-skeleton-style **2×12×2** sticks: arms at (±5, 2, 0), UV (40,16); legs at (±2, 12, 0), UV (0,16). The left side is mirrored.

### 7.3 `ModelCyberDemon` (extends ModelBiped)
The six biped parts are empty pivots (no boxes): head (0,0,0), body (0,0,0), arms (±5,2,0) and legs (±1.9,12,0). Each carries child boxes. The texture is 64 × 64 logical (the actual file is 128 × 128). The look is a hulking, horned, digitigrade demon.

| Part | Child box (UV origin) | Size | Placement and angle | Depicts |
|---|---|---|---|---|
| Head | `head` (0,0) | 8×8×7 | x −4…4, y −8…0, z −3…4 | Skull, set slightly back |
| Head | `h2` (48,0) | 6×5×2 | front, z −5…−3, y −5…0 | Protruding toothed muzzle/jaw |
| Head | `h3`, `h5` (52,7) | 2×4×4 | sides at x ±4…6, y −7…−3 | Temple implants and horn bases |
| Head | `h6`, `h7` (24,0) | 4×3×3 | pivots (±6, −5, 1.5), yawed ±40° outward | Horns sweeping sideways and back |
| Head | `h4`, `h8` (30,8) | 2×2×5 | pivots (∓7.5, −5.5, −1), pitched −25° | Horn tips curving forward |
| Body | `body` (0,15) | 12×7×6 | x −6…6, y 0…7 | Broad chest |
| Body | `b2` (0,28) | 9×5×5 | y 7…12 | Waist/pelvis |
| Arm | `rightArm` / `leftArm` (44,16) | 5×7×5 | from the shoulder pivot, outward | Massive upper arms |
| Arm | `ra2` / `la2` (48,28) | 4×8×4 | y 3…11, pitched −30° (forward) | Forearms angled forward |
| Leg | `rightLeg` / `leftLeg` (0,38) | 5×7×5 | y −1…6, pitched −30° | Thighs |
| Leg | `rl2` / `ll2` (20,39) | 4×7×4 | y 3…10, z +1.5, pitched −30° | Shins (cyber-legs) |
| Leg | `rl3` / `ll3` (0,50) | 5×3×5 | y 9…12, z −4…1, unrotated | Hoof-like feet |

The hat, cloak and ear renderers exist but have no boxes. Animation is the inherited biped animation applied to the pivots. The Cyberdemon does not use the aimed-bow pose.

### 7.4 `ModelSuperMutant` (extends ModelBiped; Tabula export credited "pWn3d")
Texture 64 × 64 logical (128 × 128 files).

| Part | Box (UV) | Size | Notes |
|---|---|---|---|
| Head | (0,0) | 8×8×8 | Standard |
| Hat / helmet | (32,0) | 8×8×8, inflated 0.5 | Painted only on texture 3 (Elite), as a metal helmet |
| Body | (16,16) | 8×12×4 | Standard |
| Chest plate, front | (21,34) | 6×7×1 | Child of the body at (1,1,−1). Shows on textures 2 and 3. |
| Chest plate, back | (21,44) | 6×7×1 | Child of the body at (1,1,4) |
| Belt | (0,53) | 9×4×5 | Child of the body at (−4.5, 9, −2.5); wraps the waist |
| Right / left arm | (40,16) | 4×12×4 | Pivot (±5, 2, 0); the right arm box sits further outward (x −3…1) |
| Shoulder / upper-arm sleeve | (40,33) | 5×7×5 | Child of each arm. Bulky upper arm; a metal pauldron on texture 3. |
| Right / left leg | (0,16) | 4×12×4 | Pivot (±1.9, **11**, 0), 1 px higher than vanilla |
| Boot | (0,33) | 5×6×5 | Child of each leg, covering its lower half |

**Rendering:** `render()` calls the biped render with `scale × getModelScale()` (1.35, 1.5 or 1.65). This enlarges geometry and pivots about the model origin, which the renderer's Y offset compensates for. Animation is the vanilla biped animation, aimed-bow while holding a gun.

### 7.5 `ModelAlienBug` (extends ModelBase; Tabula 4.1.1 export)
Texture 64 × 64. It is a quadruped insectoid with a long two-part mandible snout, a crested head and an upturned segmented abdomen.

| Part | UV | Size | Pivot / offset | Rotation | Depicts |
|---|---|---|---|---|---|
| `b1` | (0,33) | 5×5×9 | (−2.5, 14, −2) | – | Thorax |
| `b2` | (23,33) | 6×4×4 | (−3, 13, 6) | X +34° | Abdomen segment rising |
| `b3` | (43,31) | 4×3×4 | (−2, 11, 10) | X +13° | Second abdomen segment |
| `b4` | (56,30) | 2×2×2 | (−1, 10.5, 14.2) | – | Tail tip |
| `h1` head | (20,9) | 5×5×6 | pivot (0, 11.5, −1), box centred | X +7.8° (rest) | Head; yaw and pitch follow the look direction |
| `h2` (child of h1) | (36,18) | 6×3×8 | (−3, −6.5, 3.5) | X +20° | Carapace crest on top of the head |
| `h3` (child of h1) | (22,0) | 14×0×14 flat plane | (−3, −6.5, 3.5), box x −4…10 | X +20° | Flat crest/frill plane with dark antennae painted on |
| `JAW_U` (child of h1) | (2,15) | 4×4×10 | (−2, −1, −2), extends −Z | X −10.4° | Upper mandible/snout with eyes and teeth |
| `JAW_L` (child of h1) | (21,20) | 3×4×9 | (−1.5, −1.5, −2.5), extends −Z | X +26° | Lower mandible |
| 4 × leg upper `L1_A*` | (0,4) | 9×3×3 | (±1.5, 17, 0 or 4) | yaw ±20° or ±160°, roll ±30° | Upper legs radiating outward (front pair forward, rear pair back), angled upward |
| 4 × leg lower `L1_B*` (child) | (0,0) | 11×2×2 | at 7.5 along the upper leg | roll 100° | Lower legs bending down to the ground |

**Animation** (`setRotationAngles`):
- **Head:** yaw = netHeadYaw, pitch = headPitch (radians).
- **Legs:** reset to the rest angles each frame. Then four oscillators are computed from walk phase `s = limbSwing · 0.6662` and amplitude `a = 0.4 · limbSwingAmount`:
  - A = −cos(2s)·a
  - B = −cos(2s + π)·a
  - C = −cos(3s)·a
  - D = −cos(4s + π)·a

  | Leg | Yaw | Pitch | Roll |
  |---|---|---|---|
  | Front-left (`L1_A`) | +1.25A | −A | −0.75C |
  | Front-right (`L1_A_3`) | −1.25A | +A | +0.75C |
  | Rear-left (`L1_A_2`) | +1.25B | −B | −0.75D |
  | Rear-right (`L1_A_1`) | −1.25B | +B | +0.75D |

  The diagonal pairs move together, giving a skittering gait.
- **Idle jaw:** the upper jaw pitch adds cos(age·0.1)·0.1; the lower jaw subtracts cos(age·0.2)·0.1. The mandibles slowly work.
- **Bite:** while `attackTimer` is in (5, 10], let `f = (timer − 5)/5` and `e = 0.5·(1 − cos(π·f))`. Add 0.3·e to the upper jaw and subtract 0.4·e from the lower jaw. The jaws gape wide when the bite starts (timer 10) and snap shut by timer 5.
- `render()` draws b1–b4, the head (with its children) and the four legs (with their children).

### 7.6 `ModelTurret` (`client/models/blocks/`, a `ModelMultipart`) for the turret entity
Texture 64 × 64 logical (the `TurretBase*.png` files are 128 × 128).

| Part | Box (UV) | Size | Position |
|---|---|---|---|
| Part 0 | `Base` (32,22) | 8×1×8 | y 23 (bottom swivel plate) |
| Part 0 | `Stick01`, `Stick02` (9,30) | 2×11×2 | x +2 and −4, y 12…23 (two upright posts / gun cradle arms) |
| Part 1 | `Mount` (34,12) | 4×1×4 | x −2…2, y 13 (gun mounting plate; pitches) |

`ModelSimpleTurret` in the same folder is not used by any renderer (see 09).

### 7.7 `ModelNull`
An empty `ModelBase`. It is the placeholder model for the OBJ-rendered helicopter.

### 7.8 `ModelGrimReaper` and `ModelCyberdemonBlaster` (gun models, listed for completeness)
`ModelGrimReaper` is **not** an NPC model. It is the item model for the Grim Reaper quad rocket launcher (`TGuns.grimreaper`), which Outcasts may carry: a `ModelMultipart` with 59 boxes and a 128 × 64 texture (`textures/guns/grimreaper.png`), rendered by `RenderGrimReaper` with an extra 180° Y flip. Its geometry and texture are specified by **03-gun-visuals-audio**, as is `ModelCyberdemonBlaster` (the Cyberdemon's gun).

## 8. Death and gore hookup (cross-reference)

When a mob dies from a Techguns damage source with a special death type, the server sends `PacketEntityDeathType` and the client plays gore, disintegration and similar effects. These are owned by 04-projectiles-fx. The per-mob gore registrations in `client/DeathEffect` that concern this area's mobs are:

| Mob | Gib model | Parts | Texture | Gib scale | Blood RGB |
|---|---|---|---|---|---|
| ZombieSoldier, ZombieFarmer, ZombieMiner | biped gibs | 6 | `textures/entity/zombie_soldier.png` | 0.66 | 110, 21, 41 |
| ArmySoldier | biped gibs | 6 | `textures/entity/army_soldier.png` | 0.66 | 160, 21, 31 |
| Bandit | biped gibs | 6 | `textures/entity/bandit.png` | 0.66 | 160, 21, 31 |
| ZombiePigmanSoldier | biped gibs | 6 | vanilla zombie_pigman | 0.66 | 110, 51, 11 |
| SkeletonSoldier | skeleton gibs, no blood | 6 | vanilla skeleton | 0.66 | 255, 255, 255 |
| CyberDemon | `ModelGibsBipedGeneric(ModelCyberDemon)` | 6 | `textures/entity/cyberdemon.png` | 1.0 | 85, 156, 17 |
| AlienBug | `ModelGibsAlienBug` | 8 | `textures/entity/alienbug.png` | 0.8 | 235, 255, 70 |
| *(others, e.g. Outcast, Commando, Stormtrooper, Steve, Dave, Super Mutants)* | generic fallback | 6 | `textures/entity/gore.png`, random scale 0.5–0.8 | 0.66 | 160, 21, 31 |
