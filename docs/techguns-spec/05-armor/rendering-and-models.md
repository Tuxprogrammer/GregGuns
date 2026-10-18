# Rendering and models

Source:
- `java/techguns/client/models/`: `ModelSteamArmor`, `ModelT3PowerArmor`, `ModelT3PowerArmorOld`, `ModelExoSuit`, `ModelArmorCoat`, `ModelBeret`, `ModelFaceMask`, `ModelGasMask`, `ModelGasMask2`, `ModelNightVisionGoggles`, `ModelNightVisionGogglesOff`, `ModelGlider`, `ModelJetPack`, `ModelOxygenFlasks`, `ModelAntiGravPack`, `ModelBipedMultipart`, `ModelBipedTGBackSlot`
- [`java/techguns/client/renderer/item/RenderArmor.java`](index.md#coverage), [`RenderArmorItem.java`](index.md#contents)
- `java/techguns/client/renderer/additionalslots/*`
- [`GenericArmor.getArmorModel`](armor-catalogue.md#1-class-hierarchy)

Conventions:
- Geometry is in model units, where 1 unit = 1/16 block and the vanilla head box is 8×8×8.
- "UV (u,v)" is the texture offset in the model's declared texture space. Where the PNG is larger (2× HD), multiply the offset by the scale factor given.
- Boxes are listed as W×H×D.
- Each part is a child of the named biped limb, so it follows that limb's animation.

## 1. Armour model pipeline (`GenericArmor.getArmorModel`)

Only pieces with a model index ≥ 0 return a model (armor-catalogue.md §4). Pieces with −1 use the vanilla armour model and texture layer.

The returned model is one of the 12 shared static instances in [`ClientProxy.armorModels`](../01-core/lifecycle-and-registration.md#proxy-api-surface-commonproxy-overridden-by-clientproxy). It is configured before every render:
- **Visible parts by slot:**
  - head and headwear: helmet only (headwear is always hidden for the beret)
  - body: chestplate or leggings
  - both arms: chestplate only
  - both legs: leggings or boots
- **Pose flags** copied from the wearer: sneaking, riding, child.
- `heldItemRight` = 1 if the wearer holds anything.
- **Players:** `aimedBow` is true while holding any [`GenericGun`](../02-guns/gun-lifecycle.md); otherwise it follows vanilla bow-drawing (use duration > 2 with a bow action). `heldItemRight` = 3 while blocking.
- **Techguns NPCs ([`GenericNPC`](../10-entities-npcs/per-mob.md#0-shared-genericnpc-mechanics-entitiesnpcgenericnpcjava)):** `aimedBow` is true when the NPC has its aimed-bow animation flag and holds a `GenericGun`.

A separate `RenderPlayerEvent.Pre` hook also sets `aimedBow` on the player's main and armour models whenever a `GenericGun` is held ([01-core](../01-core/index.md) / [03](../03-gun-visuals-audio/index.md)).

## 2. Armour models

### 2.1 `ModelSteamArmor(type)`: Steam Armour (indices 0 and 1)
- UV space 128×64. The PNGs are 256×128, so ×2.
- All vanilla biped limbs are replaced by **empty** pivots (same pivot positions as vanilla), so only the custom boxes render. It reads as bulky brass and leather plating with pistons and pipes.

**Type 0** (helmet, chest, boots):

| Limb | Parts (size, UV) |
|---|---|
| Head | **Helmet shell** 9×9×9 enclosing the head (0,0). **Ear plates** 1×6×6 on each side (36,0). **Respirator/face box** 4×4×2 protruding in front at mouth height (95,32). **Chin bar** 7×2×1 in front (107,32). On the back: two **vertical pipes** 1×6×1 (124,0) and four **2×2×1 rivets/valves** (107,35). |
| Body | **Torso shell** 9×12×5 (20,18). **Back boiler plate** 8×10×2 (50,36). Two front **chest plates**, 7×4×1 upper (50,0) and 5×4×1 lower (50,5). On the back: two **3×4×2 boxes** (96,11), four **3×1×2 straps** (94,38), four **2×1×2 pipe caps** (96,18), and two **2×3×2 canisters** (113,18) and (120,18). |
| Each arm | **Sleeve** 5×13×5 (48,18). **Shoulder cap** 6×2×6 (71,32). **Forearm ring** 6×2×6 (71,40). **Piston rod** 2×9×2 down the outside (71,48). **Elbow piston joint** 2×3×3 (96,5). **Shoulder bar** 2×2×6 (79,48). **Exhaust stack** on the shoulder, tilted 30° outward: a 3×2×3 base (96,0) and a 2×2×2 tip (108,0). |
| Each leg (boots) | **Boot shell** 5×4×5 (0,18). **Sole** 6×2×6 (100,56). **Toe plate** 3×3×1 (95,49). |

**Type 1** (leggings):

| Limb | Parts |
|---|---|
| Body | **Belt** 10×2×6 (95,41). **Hip block** 9×3×5 (20,47). **Front crotch strip** 2×2×6 (84,56). |
| Each leg | **Thigh/shin shell** 5×9×5 (0,47). **Knee ring** 5×2×6 (71,32). **Outer piston rod** 2×10×2 (71,48). **Knee joint** 2×3×3 (96,5). |

The helmet and leggings parts share several UV regions with other parts (the piston rod, rings and joints); a texture repaint must keep those shared regions consistent.

### 2.2 `ModelT3PowerArmor(type)`: Power Armour (indices 2 and 3)
- UV space 128×64; the PNGs are 256×128 (×2).
- Vanilla limbs are again replaced by empty pivots.
- The silhouette is Fallout-style power armour: a huge helmet with a snout respirator, massive tilted pauldrons and a stepped chest.

**Type 0:**

| Limb | Parts |
|---|---|
| Head | **Helmet shell** 9×9×9 (0,0). **Respirator snout** 4×4×3 in front (0,19). Two **cheek vents** 3×2×2 either side of the snout (0,28) and (0,33). Two **jaw side strips** 1×2×8 (13,27) and (89,27). **Neck guard** 9×2×1 at the back (19,19) with a 4×3×1 plate (24,23). A **side lamp/antenna** 2×2×4 rotated 45° about its axis on the left temple (28,0). A small 2×2×1 **brow light** at the front top (0,5). |
| Body | **Torso** 9×12×5 (41,19). **Chest plate** 8×5×3 projecting forward (48,0). **Abdomen plate** 7×5×2 tilted 21° outward (71,0). Two **back tanks** 3×5×2 (48,10). A small back fitting: two 1×2×1 posts (59,10), a 5×1×1 bar (59,15) and a 2×2×1 diamond (64,10). |
| Each arm | **Arm shell** 5×13×5 (right (69,38), left (90,38)). **Pauldron** 7×2×8 tilted 30° down-outward (72,8), topped by a 7×1×6 layer (right (73,19), left (100,19)). **Shoulder block** 3×3×6 (70,27). **Forearm plate** 1×5×4 on the outside (56,38). **Elbow plate** 3×4×1 at the back (46,38). |
| Each leg (boots) | **Boot** 5×4×5 (103,0). **Toe cap** 4×2×2 (103,10). |

**Type 1:**

| Limb | Parts |
|---|---|
| Body | **Hip block** 9×3×5 (40,48). **Belt** 10×2×6 (0,38). **Crotch plate** 3×2×6 (21,47). |
| Each leg | **Leg shell** 5×9×5 (0,47). **Diamond knee plate** 3×3×1 rotated 45° (21,56). **Knee hinge** 1×2×2 rotated 45° about X (30,56). Two **outer hydraulic struts** 1×3×2 forming a shallow "<" on the outside of the knee (±8.5°) (33,38). |

**`ModelT3PowerArmorOld`** is the same geometry with some mirror flags and UVs changed: both arms use the right arm's UVs, and the cheek-vent and jaw-strip UVs are swapped. It also sets its texture size to 128×128 after the boxes are built, which has no effect. **It is never instantiated.**

### 2.3 `ModelExoSuit(type, scale)`: Exo Suit (indices 4, 5, 6)
- A vanilla biped built with box inflation = `scale`, on a 64×64 UV grid. The PNGs are 128×128 (×2).
- The top half of the texture uses the vanilla biped layout; the exo frame parts use the lower half.
- Inflation by use: helmet 1.0, chest and boots 0.75, leggings 0.5.
- The look is a white armour suit with a grey mechanical exoskeleton frame bolted on.

**Type 0:**

| Limb | Frame parts |
|---|---|
| Each arm | **Shoulder block** 6×5×6 (0,32). **Forearm cuff** 6×2×6 (24,32). **Outer arm rod** 2×8×2 (48,32). **Elbow joint** 2×3×3 (27,41). |
| Body | **Back cross-bar** 10×2×2 across the shoulder blades (0,60). **Spine** 3×9×2 down the back (38,40). |
| Each leg (boots only) | **Ankle plate** 3×3×1 (56,51). **Sole** 6×2×6 (40,56). |

**Type 1:**

| Limb | Frame parts |
|---|---|
| Body | **Belt** 10×2×6 (0,43). **Centre strip** 2×2×6 (0,52). |
| Each leg | **Outer leg rod** 2×10×2 (56,32). **Knee joint** 2×3×3 (27,41). **Thigh cuff** 5×2×6 (17,52). |

### 2.4 `ModelArmorCoat(type, scale)`: Riot / Ranger Veteran (indices 8–11)
- A vanilla biped with inflation `scale` on a 64×64 UV grid. The PNGs are 128×128 (×2).
- Extra parts use the same inflation.
- The source credits the Tabula model "RiotGear" by pWn3d.
- The look is an NCR-Ranger-style long duster coat with shoulder pads.

| Type (use, scale) | Extra parts |
|---|---|
| 0 (helmet, 1.0) | none |
| 1 (jacket, 0.51) | Two **coat front panels** 2×12×5 hanging at the body's sides (50,47). A flat **coat back panel** 10×12×0, a zero-thickness plane (44,35). Two **shoulder pads** 5×3×6 on the arms, tilted ±13° (0,34). |
| 2 (pants, 0.49) | Two **coat skirts** 5×10×5 around each leg (0,48), so the coat continues below the waist. |
| 3 (boots, 0.75) | none |

### 2.5 `ModelBeret` (index 7)
- UV 32×32; the PNG is 64×64 (×2). The vanilla head is replaced by an empty pivot.
- Parts:
  - **Crown** 6×2×8, inflated 0.6, sitting on top of the head and shifted toward the wearer's left (0,0).
  - **Drooping side** 3×2×8, inflated 0.55, on the other side, rotated −35° about Z so it hangs over the ear (0,11).
- Rendering uses the normal biped render; the headwear layer is hidden.

### 2.6 Face models (rendered on the head)

All are `ModelBiped(0.0625, 0, 32×32)` subclasses: the inherited head box is inflated by 1/16, and the custom parts are children of the head. Only the head is rendered. UV space 32×32; the PNGs are 64×64 (×2).

| Model | Parts |
|---|---|
| `ModelFaceMask(hasFront)` (Oxygen Mask, Tactical Mask) | **Hood** 8×8×8 over the head (0,0). If `hasFront`: a **mouth piece** 4×3×1 in front of the mouth (9,17). |
| `ModelGasMask` (Gas Mask) | Hood (0,0). Mouth piece 4×3×1 (9,17). **Filter mount** 3×3×1 in front, tilted 19° downward (0,17). **Filter canister** 2×2×2, rotated (20°, −13°, 45°) so it sits as a diamond on the snout (20,17). |
| `ModelGasMask2` (**unused**; a `ModelBase`, not a biped) | Hood, a 4×2×1 mouth piece, and **twin side filters** (3×3×1 discs angled ±34°) on 2×2×3 mounts, plus a centre mount. |
| `ModelNightVisionGoggles` (goggles **down**) | **Strap/hood** 8×8×8 (0,0). Two **lens tubes** 2×2×4 rotated 45° about Z (diamond section) sticking forward from the eyes (0,17). **Bridge** 3×1×2 (13,17). **Mount arm** 2×1×4 angled 45° up to the forehead (13,21). |
| `ModelNightVisionGogglesOff` (goggles **flipped up**) | The same parts, re-posed. The tubes sit above the forehead, pointing up and back (rotated about −135° X, −31° Y, 35° Z). The mount arm is folded back about 75°, and the bridge is on top of the head. |

The inherited inflated head box *and* the child hood box both render, so the head is drawn twice, 1/16 apart (bug [ARM-24](index.md#suspected-bugs)).

### 2.7 Back models (`ModelBipedMultipart` subclasses)

UV 32×32; the PNGs are 64×64 (×2). Every part's X-rotation copies the body's X-rotation, so the pack leans when the wearer sneaks. All parts hang from a single root ("BackBack") placed behind the body (z ≈ 2–6).

| Model | Part 0 | Part 1 |
|---|---|---|
| `ModelGlider` | **Backpack** 7×10×4 (0,0) | **Wings**, spanning about 38 units (≈2.4 blocks) in total. Each side has: an inner strut 1×7×2 (23,0); two top spars 6×1×2 (13,15); an outer strut 1×6×2 (21,19); an inner canvas panel 5×7×1 (0,15); an outer canvas panel 6×6×1 (0,24). |
| `ModelJetPack(variant)` | **Central pack** 4×7×4 (16,0). Two **fuel tanks** 3×9×3 either side (0,0), each with a 2×2×2 cap (0,13). Variant 0 (Jetpack) adds small **stub wings** on each side: a 5×6×1 panel (0,24), a 5×1×2 top edge (13,15) and a 1×6×2 outer edge (21,19). Variant 1 (Jump Pack) has no wings. | – |
| `ModelOxygenFlasks` | A small **harness block** 2×6×3 (16,0). Two **air cylinders** 3×9×3 (0,0), each with a 2×1×2 **valve** (0,13). | – |
| `ModelAntiGravPack` | **Pack** 6×6×3 (16,0) (its UV strip overruns the 32-wide texture; bug [ARM-23](index.md#suspected-bugs)). Two slim **side cylinders** 2×7×2 (0,0). A **core crystal** 3×3×3 rotated 45° about Z, showing as a diamond, on the back (6,10). A **back plate** 5×4×1 (20,11). Four tiny **fins** 2×1×1 (0,10), two of them turned 180°. | – |

`ModelBipedMultipart` (abstract) adds a multipart `render(entity, …, ammoLeft, reloadProgress, renderType, part, fireProgress)`. The plain `render` calls it with part 0 and type ENTITY.

`ModelBipedTGBackSlot` (abstract) declares `renderOnPlayer(...)` and a helper that copies the player model's body rotation. **It has no subclasses (dead code).**

## 3. Special-slot rendering on the player (`renderer/additionalslots`)

**Hook:** `RenderPlayerEvent.SetArmorModel` in the event handler ([01-core](../01-core/index.md)).
- **Armour pass 0** (helmet pass): read the face stack from the DataWatcher. `showSlot` = not (the helmet is a `GenericArmor` with `hideFaceslot`).
- **Armour pass 1** (chest pass): the same with the back stack and `hideBackslot`.

Rendering happens inside the player's model space, so the slot models line up with the player's biped.

`RenderAdditionalSlotItem.getRenderForItem(stack)` picks one shared renderer per item:

| Item | Renderer | Texture(s) (`textures/armor/…`), indexed by camo | Honours `showSlot`? |
|---|---|---|---|
| Gas Mask (only while not empty) | `RenderAdditionalArmorModel` + `ModelGasMask` | `gasMask.png` | yes |
| Oxygen Mask (shared item) | `RenderAdditionalArmorModel` + `ModelFaceMask(true)` | `oxygenMask.png` | yes |
| Tactical Mask | `RenderAdditionalArmorModel` + `ModelFaceMask(true)` | `tacticalMask.png`, `tacticalMask_green.png`, `tacticalMask_white.png`, `tacticalMask_lightgreen.png` (the actual file is `tacticalmask_lightgreen.png`) | yes |
| Night Vision Goggles | `RenderNightVisionGoggles` (down model while the wearer has Night Vision, else up model) | `nightVisionGoggles.png` | yes |
| Glider (shared item) | `RenderGlider` + `ModelGlider` | `glider.png` | backpack only; the wings ignore it |
| Jetpack | `RenderJetpack` + `ModelJetPack(0)` | `jetPack.png`, `jetPack_bronze.png`, `jetPack_black.png`, `jetPack_darkgreen.png` | no (always drawn) |
| Jump Pack | `RenderJetpack` + `ModelJetPack(1)` | same four jetpack textures | no |
| Oxygen Tanks | `RenderOxygenFlasks` + `ModelOxygenFlasks` | `oxygenFlasks.png` | yes |
| Anti Gravity Device | `RenderOxygenFlasks` + `ModelAntiGravPack` | `AntiGravPack.png`, `AntiGravPack_bronze.png`, `AntiGravPack_black.png`, `AntiGravPack_darkGreen.png`, `AntiGravPack_silver.png` | yes |
| Akimbo guns (held item) | `RenderAkimboGun` family (see §4) | gun textures ([03](../03-gun-visuals-audio/index.md)) | n/a |

**`RenderAdditionalArmorModel.render`** (base for all except the akimbo guns), when showing:
1. Bind the texture for the item's camo index, or index 0 if the item is not [`ICamoChangeable`](armor-catalogue.md#1-class-hierarchy).
2. Compute the head yaw relative to the body:
   - Interpolate body yaw and head yaw.
   - If riding a living entity, use the mount's body yaw, clamp the difference to ±85°, and add 20 % of the difference when it exceeds 50°. This matches vanilla living-render logic.
3. Interpolate the head pitch.
4. Copy sneak, child, riding and swing progress from the player's main model.
5. Call `model.render(entity, limbSwing 0, limbSwingAmount 0, f4, yawDiff, pitch, 1/16)`.

So head-mounted gear follows head yaw and pitch, and back gear follows the body including the sneak lean.

**Renderer variants:**
- **`RenderGlider`:** draws part 0 (backpack) if `showSlot`. Draws part 1 (wings) whenever the wearer is airborne and sneaking, i.e. the same condition as the glide physics.
- **`RenderJetpack`:** always draws part 0. Then, if the wearer's `jumpkeyPressed` flag is true, spawns two `Jetpack` flame particles (special-equipment.md §4.3).
- **`RenderOxygenFlasks`:** draws part 0.
- **`RenderNightVisionGoggles`:** keeps two models, both posed from the main model.

**Unused renderers:**
- `RenderGasMask`: an older face renderer equivalent to `RenderAdditionalArmorModel`.
- `RenderAntiGravPack`: an older back renderer that applied manual GL transforms (translate 0.175 down, rotate by body yaw, pitch = 70 × body angle around a 0.15 pivot).

## 4. Akimbo second-gun renderers (in this folder; the gun data and animations belong to 02-guns / 03-gun-visuals-audio)

**Triggered by** `RenderPlayerEvent.Specials.Pre` whenever the held `GenericGun` is akimbo. The gun is drawn in the **left** hand.

**Transform order:**
1. Rotate 180° about X.
2. Apply the main model's left-arm transform (with the Y and Z axes flipped around the call).
3. Translate up 0.3.
4. Apply the weapon rotation (−90° about X; the revolvers add +90° about Y).
5. Apply the uniform scale.
6. Apply the per-gun translation.
7. For each model part: push, apply reload/recoil, render the part (type EQUIPPED), pop.

| Gun ([`TGuns`](../02-guns/gun-catalogue.md) field) | Renderer | Model, texture | Parts | Scale | Translation | Double-shot recoil |
|---|---|---|---|---|---|---|
| `akimbo_pistol` | `RenderAkimboGun` | [`ModelPistol3`](../03-gun-visuals-audio/gun-models.md#modelpistol3-pistol-declared-6432-used-with-a-12864-png-family-z-9-boxes-2-parts), `textures/guns/pistol3.png` | 2 | 0.55 | (0.2, −0.5, −2.0) | no |
| `akimbo_revoler` | `RenderAkimboRevolver` | [`ModelRevolver`](../03-gun-visuals-audio/gun-models.md#modelrevolver-6432-family-x-20-boxes-1-part), `textures/guns/revolver.png` | 1 | 0.3 | (2.7, −0.55, 0.15) | no |
| `akimbo_goldenrevolver` | `RenderAkimboRevolver` | [`ModelGoldenRevolver`](../03-gun-visuals-audio/gun-models.md#modelgoldenrevolver-6432-family-x-24-boxes-1-part), `textures/guns/goldenrevolver.png` | 1 | 0.3 | (2.7, −0.55, 0.15) | no |
| `akimbo_mac10` | `RenderAkimboMac10` | [`ModelMac10`](../03-gun-visuals-audio/gun-models.md#modelmac10-declared-6432-used-with-a-12864-png-family-z-23-boxes-tabula-1-part), `textures/guns/mac10texture.png` | 1 | 0.5 | (0.2, −0.45, −2.15) | yes |
| `akimbo_mibGun` | `RenderAkimboMibGun` | [`ModelMibGun`](../03-gun-visuals-audio/gun-models.md#modelmibgun-mib-gun-declared-6464-used-with-a-128128-png-family-z-50-boxes-tabula-1-part), `textures/guns/mibgun.png` | 1 | 0.55 | (0.1, −0.7, −1.7) plus the debug offsets (bug [ARM-25](index.md#suspected-bugs)) | no |

**Animation inputs:** per-entity reload and recoil timers held by `ClientProxy` ([03](../03-gun-visuals-audio/index.md)).
- Reload progress p = 1 − remaining/total.
- Recoil progress p = 1 − remaining/total. An expired timer is cleared.
- Recoil is applied only when the gun is double-shot or the stored attack type is 1, meaning the off-hand fired.

**Reload tilt:**
- Angle = 40° × (−min(0.5, (|0.5 − p| − 0.5) × 2)).
- This peaks at 40° at mid-reload and is 0 at the ends.
- Rotation is about X around the pivot (0, 0.3, 0.9).
- The revolvers rotate about Z instead.
- MIB gun: 35°, pivot (0, 0.5, 0.3).

**Recoil kick:**

| Gun | Kick |
|---|---|
| Default (pistol) | During p < 0.3, rotate −30° × p/0.3 about X around z = 0.7, then snap back. |
| Mac10 | −15° instead of −30°. |
| Revolver | −30° about Z. |
| MIB | For p < 0.5, with q = 2p: rise to −15° over q < 0.25, then ease back over the rest. Pivot (0, 0.3, 0.5). |

## 5. 3-D inventory icons for Steam and Power armour (`RenderArmorItem`)

`RenderArmorItem(model, slot, textureFile)` is registered as the item renderer for the 8 Steam and Power pieces:
- Steam: `ModelSteamArmor(0)` for helmet, chest and boots; `ModelSteamArmor(1)` for leggings; texture `steam_armor.png`.
- Power: `ModelT3PowerArmor(0)` and `(1)`; texture `powerarmor.png`.

The texture is always the **default camo**. These items have no icon PNGs in `textures/items`.

It handles all render types and uses the render helper. It hides all biped parts, then shows only the slot's parts: head and headwear (0); body and both arms (1); body and both legs (2); both legs (3). It renders at scale 1/16 with no entity.

Per-type transforms, applied before the render:

| Render type | Common | Helmet (0) | Chest (1) | Legs (2) | Boots (3) |
|---|---|---|---|---|---|
| ENTITY (dropped) | rotate 180° X, −90° Y | translate y −0.85, scale 1.5 | y −1.9, scale 2 | y −2.5, scale 2 | y −2.9, scale 2 |
| EQUIPPED (third person) | rotate 180° X; translate x 1.0; then scale 2 | y −1.25 | y −2.10 | y −3.00 | y −3.60 |
| EQUIPPED_FIRST_PERSON | rotate 180° X, 90° Y; translate z 0.5; then scale 2 | y −1.75 | y −2.60 | y −3.30 | y −3.60 |
| INVENTORY | rotate 180° X | y −1.25, scale 2.25 | y −2.10, scale 2.30 | y −3.00, scale 2.50 | y −3.60, scale 2.70 |

Commented-out registrations exist for the beret and Exo pieces. `RenderArmor.java` is entirely commented out.
