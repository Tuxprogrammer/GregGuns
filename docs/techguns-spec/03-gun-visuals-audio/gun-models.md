# Gun and weapon models

Geometry of every weapon model in `java/techguns/client/models/` that belongs to this area, plus the one OBJ in `models/guns/`. This page describes the models; it does not transcribe their box lists.

**Conventions**

* Units are model units (1/16 block before renderer scaling). A box is written *w×h×d* (X × Y × Z). Y grows **downward** in model space; renderers flip it with rotX(180).
* "Family X/Z" is the orientation family from [rendering-framework.md §6.1](rendering-framework.md#61-model-space). X-family models point the muzzle to +X, Z-family models to −Z.
* "rot45" means the box is rotated 45° about its own long axis. In practice that is how Techguns makes square boxes read as round barrels.
* "Extent" is the overall bounding box of the unrotated boxes. Rotated boxes can poke slightly outside it.
* `infl` is box inflation (`addBox` size delta). Negative inflation shrinks a box to make thin rods.
* Unless noted, every box uses `mirror = true` (Techne default). That makes no visible difference on these symmetric unwraps.
* **Parts:** for multipart models, part 0 is drawn first and part *n* is drawn in its own GL pass so the renderer can move it ([rendering-framework.md §1](rendering-framework.md#1-registration-client-proxy)).
* UV footprints of the major boxes are in [textures.md](textures.md).

---

## Shared bases

| Class | Role |
|---|---|
| ModelMultipart | Abstract base. Adds the extended render (ammoLeft, reloadProgress, renderType, part, fireProgress). The plain render forward with ammo 100, reload −1, type ENTITY, part 0, fire 0. |
| ModelAkimbo(model, partsPerModel) | Wraps another multipart model. Part ≥ partsPerModel is folded to part − partsPerModel, so the same geometry is drawn for the left gun. |
| ModelModel(objPath) | Loads a Wavefront OBJ via Forge `AdvancedModelLoader` from `assets/techguns/<objPath>`. Its render scales by the passed factor (0.0625) and renders the OBJ group named after the part index (`"0"`, `"1"`). The plain render draws every group. |
| ModelNull | Empty ModelBase (used by the helicopter renderer, area [10](../10-entities-npcs/index.md)). |
| ModelGunAmmo | Abstract legacy ammo-aware ModelBase (ammoLeft, reloadProgress, renderType). No concrete subclass remains. Only the dead [`RenderGun`](rendering-framework.md#1-registration-client-proxy) path casts to it. |

---

## Handguns

### ModelHandgun ("Hand Cannon"): 64×32, family X, 14 boxes, 1 part
Extent x −17…22, y −3…5, z −2…3 (39 × 8 × 5): a crude stone-barreled flintlock pistol.
* **Barrel** (x 0…22): core slab 22×5×3 sandwiched between two 22×3×1 side plates. Front sight 1×1×1 on top at the muzzle (x 21).
* **Lock/breech** (x −8…0): two 2×5×5 blocks (x −8 and −2) around a 4×4×5 center block. Each block has a 2×1×3 top cap (the hammer and frizzen silhouette).
* **Stock** (x −17…−8): 7×4×3 wrist, two 7×2×1 side plates, 2×1×3 lower lip, 2×6×5 butt block at x −17.
* No moving parts. A commented-out duplicate barrel box ("Shape13") was removed.

### ModelPistol3 ("Pistol"): declared 64×32 (used with a 128×64 PNG), family Z, 9 boxes, **2 parts**
Extent z −3…12, y −0.5…10, x −2…1 (15 long): a modern semi-automatic pistol.
* **Part 0, frame:** frame rail 3×1×14 (y 2…3), barrel 1×1×6 rot45 poking 1 unit out of the slide front, pistol grip 2×7×3 tilted 12.5° back, trigger-guard front 1×3×1 and bottom 1×1×3, trigger 1×2×1 tilted −23°.
* **Part 1, slide:** slide 3×2×14 (y 0…2) with front and rear sight cubes 1×1×1 (z −0.5 and z 9.5).
* The slide is the moving part. While firing, the renderer pushes it back further than the frame (§ Pistol in the gun sheets).

Clean-room rebuild notes: [model-notes/pistol-9mm.md](model-notes/pistol-9mm.md).

### Unused pistol variants
* **ModelPistol** (64×32, ModelBase, 9 boxes): early boxy pistol. Top 2×3×12, stepped grip 2×(3,5,4)×1, barrel 1×1×2, sights. Pairs with [`textures/guns/pistol.png`](textures.md#measured-colors-all-62-textures). Not registered.
* **ModelPistol2** (64×32, 8 boxes): ModelPistol3's frame with a solid 3×3×14 top instead of separate frame and slide. Pairs with [`textures/guns/pistol2Texture.png`](textures.md#measured-colors-all-62-textures). Not registered.
* **ModelPistol3Akimbo**: a [`ModelBipedMultipart`](../05-armor/rendering-and-models.md#27-back-models-modelbipedmultipart-subclasses) (area [05](../05-armor/index.md) base) that attaches the Pistol3 boxes as children of `bipedLeftArm` and renders only that arm. Its only reference is an import in [`RenderAkimboGun`](../05-armor/rendering-and-models.md#3-special-slot-rendering-on-the-player-rendereradditionalslots), which actually uses plain ModelPistol3. Dead.

### ModelRevolver: 64×32, family X, 20 boxes, 1 part
Extent x −5…23, y −2.8…11.5, z −1.5…3.5: a long-barreled six-shooter.
* **Barrel** (x 9…23): 14×2×2 rotated 45° about X (a diamond-section barrel), 8×1×1 rot45 ejector rod underneath, front sight 2×1×1 at x 20.5.
* **Frame:** 9×1×2 top strap, 9×2×2 and 9×2×3 frame blocks (x 0…9), 1×4×2 recoil shield at x −1.
* **Cylinder** (x 1.5…6.5): 5×2×5 drum plus 5×1×4 top and bottom slices, about 5 units in diameter.
* **Hammer:** 1×1×1 spur and 2×2×1 body at x −3.5…−2.5.
* **Trigger guard:** 6×1×2 bar and two 1×2×2 posts (y 2.5…5.5). Trigger 3×0×2 (zero-thickness plane).
* **Grip:** stepped 2×2×2 → 6×2×2 → 4×2×2 → 4×5×2, curving down and back to y 11.5.

Clean-room rebuild notes: [model-notes/revolver.md](model-notes/revolver.md).

### ModelGoldenRevolver: 64×32, family X, 24 boxes, 1 part
Identical frame, cylinder, hammer, guard and grip to ModelRevolver, with an ornate barrel:
* Barrel 15×2×2 rotated 45° about X (x 9…24), top strip 13×1×1, ventilated rib 13×1×1 rot45, and two flared 12×2×1 side fins rotated ±21° about X.
* Sights 2×2×1 at the rear (x −0.2) and front (x 21.8).

### ModelMac10: declared 64×32 (used with a 128×64 PNG), family Z, 23 boxes (Tabula), 1 part
Extent z −6…16, y −2…16.7, x −2…1: a compact machine pistol with a wire stock.
* **Receiver** 3×4×17 (z −3…14). Barrel 1×1×6 rot45 extending 3 units forward.
* **Grip and magazine:** grip 2×7×3 with the magazine 2×7×3 (infl −0.3) below it, reaching y 16.7. Back-strap 2×3×1 / 2×5×1 tilted −12.5° / +7.4° (infl −0.1). Trigger guard 1×3×1 + 1×1×3. Trigger 1×2×1 tilted −23°.
* **Sights:** 1×1×1 rotated 45° about Y (rear), 1×1×2 (infl −0.3) front post, two zero-width 0×1×1 ears.
* **Folding wire stock** (z 10…16): two 1×5×1 struts rotated 45° about Y, two 1×1×4 rods rotated 45° about Z and splayed a few degrees, 1×1×2 links, 2×1×1 hinge rotated 45° about X, 3×3×1 butt pad rotated 45° about Z (infl −0.45), 3×2×1 + 2×1×1 butt bars. Most stock parts use infl −0.2 to read as thin wire.

### ModelMibGun ("MIB gun"): declared 64×64 (used with a 128×128 PNG), family Z, 50 boxes (Tabula), 1 part
Extent z −11…7, y 2…13.8, x −2…1.8: a tiny chrome "cricket" blaster.
* **Hexagonal body tube** (z −2…7): top 2×2×9, bottom 2×1×9, six 1×1×9 side bars (two straight, four rotated ±28° about Z).
* **Core:** 2×2×6 core + 2×2×1 core cap + 2×2×1 *light* lamp at the very front (z −10), which is blue in the texture.
* **Barrel ring:** twelve 1×1×6 barrels (z −8…−2) arranged in a ring of radius ≈1.3 around the core, rotated 0°, ±24° or −67° about Z. Twelve 1×1×1 "roundel" muzzle caps at z −9.
* **Prongs:** four 1×1×2 tips at z −11, angled ±23° outward (up, down, left, right).
* **Grip and details:** laser rod 1×1×7 rot45, two zero-width holders 0×1×6, holder 1×2×1 tilted 31°, grip 2×7×3 tilted 12°, guard 1×3×1 + 1×1×3, trigger 1×2×1 tilted −31°, side button 1×1×1 rotated 45° about X, rear tilt plate 2×3×1 (10°), bottom tilt 2×1×2 (27°).

---

## Shotguns

### ModelSawedoff: 64×32, family X, 20 boxes, 1 part
Extent x −17…22, y −4…5, z −3…4: a side-by-side double-barrel cut down to a pistol grip.
* **Barrels** (x −6…22): two 28×4×2 tubes (z −2…0 and 1…3), with 28×2×1 ribs above, between and outside (z −3, 0, 3). Twin bead sights 1×1×1 at x 21. Under-rib 8×1×1.
* **Breech face** (x −7): 1×1×1 / 1×2×1 caps closing the barrel ends.
* **Action and grip** (x −17…−6): 2×3×3 lock block, 4×1×3 top lever, 6×1×3 trigger plate, 7×4×3 grip, two 7×2×1 side plates, 2×1×3, butt 2×6×5.

### ModelCombatShotgun: 64×32, family X, 20 boxes, **2 parts**
Extent x −23…32, y −3.5…8.5, z −1.5…2.5: a pump shotgun with a folding skeleton stock.
* **Part 0, gun:**
  * Barrel 24×3×1 with two 24×1×1 side rails (x 8…32). Magazine tube 22×2×2 underneath (y 1…3). End caps 2×1×1 at x 27 (tube) and x 29 (barrel top).
  * Receiver 10×5×3 (x −2…8) with 10×1×2 top. Rear receiver 8×4×3 (x −10…−2) with 7×1×2 top.
  * Pistol grip 3×7×2 tilted 19°. Trigger 2×1×1, rear sight 2×1×1.
  * Folding stock: 2×5×3 hinge block at x −21, 2×5×3 + 2×3×2 + 1×2×2 butt at x −23…−21, lower strut 12×2×2 tilted −15°, upper strut 10×1×1 tilted −8°.
* **Part 1, pump:** 12×5×4 fore-end around the tube (x 7…19). It slides back relative to the gun during the rechamber and reload animations.

### ModelSonicShotgun: 128×64, family X, 65 boxes (Tabula), 1 part
Extent x −24…35, y −8…15, z −2.5…4.5: a sci-fi shotgun with a horn-shaped emitter.
* **Emitter horn** (x 27…35): 1×8×4 back plate. 7×1×4 top and bottom plates and four 7×3×1 side plates tilted ±12° about X flare outward. 5×4×4 core, 5×1×3 inner lips.
* **Barrel** (x 11…27): two 16×2×5 slabs, 16×3×3 underside, 16×1×3 top, 10×2×4 + 9×1×3 under-shroud. Three 2×2×5 rings (x 11, 18, 25). Four 5×2×2 rot45 coil segments at z ±1.4 (x 13 and 20).
* **Body** (x 2…11): 8×4×7 center, 8×1×6 top and bottom, 8×2×5, 8×1×4, with a **7×3×6 GLOW01 core** (full bright). Power cell 8×7×4 hanging below (y 4…11). Front plate 1×9×4 at x 10.
* **Carry handle:** 18×1×2 bar at y −7 (x −0.5…17.5) on posts, with tiny sight pips.
* **Grip:** 3×8×2 tilted 18°, small guard bits, 4×5×4 + 3×4×2 rear housing.
* **Stock** (x −24…−2): 10×4×4 + 10×1×3 + 12×4×3 + 8×1×2 frame, 20×1×2 bottom rail, 9×6×2 and 2×7×2 (tilted −66°) supports, 8×8×3 butt block, 2×7×4 butt plate, two 2×2×3 nubs.

---

## Rifles and submachine guns (Z-family "modern" rifles)

All Z-family rifles share a common skeleton from the same author: pistol grip **3×9×4 tilted 26°** at z 6, trigger **2×1×5–6** + **1×2×1 tilted −15°**, magazine socket **3×3×8**, and M4-style magazine **2×5×6 + 2×4×6 tilted −9°**.

### ModelAK: 128×64, family Z, 35 boxes, 1 part
Extent z −58…40, y −9…16, x −2.5…2.5.
* **Barrel group:** main barrel 2×2×27 rot45 (z −58…−31), muzzle device 3×3×6 rot45 (z −52…−46), collar 3×3×3 rot45 (z −40), rear barrel collar 3×3×3 (z −31). Gas tube 2×2×9 rot45 above (z −37…−28). Gas block 2×2×5 rotated 55° about X. Front sight post 1×6×1 at z −51 plus a 1×6×1 brace tilted 21°, and 1×1×2 sight cap.
* **Handguard:** upper 3×3×9 (z −28…−19), lower 4×5×14 (z −28…−14).
* **Receiver:** 4×5×28 body (z −14…14), 3×3×23 dust cover, 4×3×7 front trunnion, rear 3×3×4 tilted −50°, rear-sight leaf 1×1×7 tilted 8°, 1×1×1 tilted −70°.
* **Curved magazine:** 3×8×5 tilted −9° and 3×9×5 tilted −20°, with 2×8×2 rib strips on the front edge. Magazine well 3×3×2, 2×4×1, 3×3×5.
* **Grip and trigger:** grip 3×8×4 tilted 21°, 3×2×1 grip cap. Trigger guard 2×1×7, trigger 1×3×1 tilted −15°.
* **Stock** (z 14…40): 4×4×26 core, layered 4×1×(21, 15, 10, 7, 12, 2) planks stepping down to the butt. That gives a tapered wooden stock.

### ModelM4: 128×64, family Z, 42 boxes, 1 part
Extent z −41…28, y −11…13, x −3.5…3.5.
* **Barrel:** 2×2×18 rot45 (z −41…−23). Front sight post: 1×4×1 tilted 25° + 1×2×2 + 1×3×1 at z −25/−26. Gas block 4×4×2 rot45 (z −8).
* **Quad-rail handguard** (z −23…−8): 4×4×15 core, 1×2×13 side rails, 2×1×13 top and bottom rails.
* **Receiver:** upper 4×4×16 (z −6…10), lower 3×3×15, 3×1×2 + 4×3×2 rear pieces. Top rail 2×1×15. Carry-handle rear sight 3×2×4. Charging handle 2×1×1 + 4×1×1. Forward assist 4×1×1 rotated 75° about Y.
* **Holographic sight (EOTech):** 4×3×9 base, 4×1×4 hood, two 1×2×4 side ears, 1×1×0 red-dot plane.
* **Magazine:** socket 3×3×8, magazine 2×5×6 + 2×4×6 tilted −9°.
* **Grip and trigger** (shared skeleton). **Vertical foregrip:** 3×2×4 clamp + 2×8×2 rotated 45° about Y (z −13.5).
* **Collapsible stock:** buffer tube 3×3×15 (z 13…28), 2×2×1 rot45 castle nut, and 1×4×8, 2×8×2, 1×5×3, 2×3×1, 2×2×2 ×2, 2×9×2 (tilted 34°), 2×4×2 (tilted 60°) forming the stock frame.

### ModelM4_UQ ("M4 Infiltrator"): 128×64, family Z, 45 boxes, 1 part
Same skeleton as ModelM4 (several UV origins shifted down by 1 row) with these changes:
* No EOTech hood, ears or red dot. The base becomes a smaller 3×3×6 scope mount.
* **Silencer** 3×3×18 rot45 (z −48…−30) over the muzzle.
* **Laser module** 3×4×5 under the handguard (z −21.5).
* **Laser beam:** two zero-thickness 0×1×60 planes (z −81…−21), one rotated 90° about Z, crossing to form an X-section beam. Not drawn in INVENTORY.
* **Scope:** body 4×4×8 (z −3…5, y −12…−8), front bell 4×4×3 rot45, eyepiece 3×3×2 rot45.
* Foregrip moved to z −12.5.

### ModelM4Mag / ModelM4MagE (item models): 128×64
The M4's 2×5×6 + 2×4×6 (tilted) magazine boxes plus a 2×2×6 top. The full variant adds a 1×1×5 box rotated −45° about Z: the top cartridge, which picks the yellow UV at (112, 30).

### ModelAUG: 128×64, family Z, 24 boxes (Tabula), **2 parts**
Extent z −41…29, y −11.5…12, x −2.5…3.5: a bullpup rifle.
* **Part 0:**
  * Main body: one 4×4×44 box rot45 (z −15…29), the long rounded shell. 3×2×18 lower receiver.
  * Barrel 2×2×23 rot45 (z −38…−15) with a 2×2×3 rot45 muzzle cap and a 1×1×6 rot45 gas tube.
  * Foregrip 3×9×3 rotated 45° about Y (z −14). Trigger guard 3×9×1 tilted 34° + 3×1×6 underside. Pistol grip 3×9×4 tilted 26° at z 0. Trigger 1×3×2.
  * Stock (behind the grip): 3×3×7 tilted −18°, 3×5×10, 3×3×10 tilted −26°, 3×7×4, butt 4×12×4 (z 25…29), 3×2×4.
  * Magazine behind the grip (bullpup): socket 3×3×8 tilted −12°, magazine 2×5×6 tilted −12° and 2×4×6 tilted −21°.
  * Integrated scope/handle: two mounts 2×8×1 (tilted −35°) and 2×7×1 (−25°), scope tube 2×2×9 rot45, objective 3×3×10 rot45.
* **Part 1, bolt:** 1×2×2 rot45 cocking handle on the right side (x 2.5, y −5, z −13). It slides back during the firing animation.

### ModelScar: 128×64 (used with a 256×128 PNG), family Z, 45 boxes (Tabula), **2 parts**
Extent z −44…27.8, y −12.3…11, x −3.5…3.5. Many boxes are drawn with a per-box non-uniform scale (a Tabula export feature), noted as [sx, sy, sz].
* **Part 0:**
  * Receiver 4×5×37 (z −25…12) and 3×3×16 lower. Top rail 2×1×36, side rails 1×2×12 ×2, bottom rail 2×1×17.
  * Barrel 2×2×8 [0.7, 0.7, 1], 2×2×4 and 2×2×5 [0.8, 0.8, 1] mounts, 2×2×1 [0.7, 0.7, 1] collar, muzzle brake 2×2×6 rot45 (z −44…−38).
  * Front sight 2×1×4 + 2×2×1 [0.8, 1, 1]. Rear sight 1×1×3 + 3×1×2 [0.9, 1, 1] + 4×2×2 rotated −45° about X [0.8, 0.7, 0.7].
  * Reflex sight: 3×1×4 mount [0.9], 2×3×6 plate [0.9], 3×3×5 and 2×2×6 rot45 [0.9], 3×3×3 [0.9], 2×2×2 rotated −45° about X [0.8, 0.7, 0.7], 2×2×2 rotated 45° about Y [0.7].
  * Front grip: 3×2×6, 3×1×5, 2×1×6 tilted −40°, 2×1×5 tilted −110°, 2×1×1 ×2 [0.8].
  * Pistol grip 3×9×4 tilted 26° [0.99, 1, 1]. Trigger 2×1×6 + 1×2×1. Magazine 2×6×7, socket 3×3×8, 3×2×2 receiver bits.
  * Folding stock: 4×7×2 hinge, 2×6×2 rotated 45° about Y [0.6, 1, 0.6], 3×3×10, 4×4×11 tilted −10° [0.99], 3×10×3 tilted 15° [0.99], 3×11×3, 4×12×2 butt, 1×1×2 rot45.
* **Part 1, bolt:** 2×1×1 rotated 45° about X [1, 0.7, 0.7] at x 1.5, z −11: the right-side charging handle. It slides back when firing.

### ModelVector: 128×64 (used with a 256×128 PNG), family Z, 28 boxes (Tabula), 1 part
Extent z −17…27, y −8.2…20, x −2.5…1.5: the angular "Kriss Vector" silhouette.
* Upper receiver 4×3×25 [0.8, 1, 1] (z −14…11) and 3×3×10 lower front. Top rail 2×1×23, under-rail 2×1×7.
* **Slanted lower receiver:** two 3×11×5 blocks tilted 20° (z −8…2) [0.99], 3×1×2 tilted −20° [0.98], bottom plates 3×1×10 and 3×1×9.
* Magazine 2×11×3 tilted 20° [0.99]. Pistol grip 3×10×4 tilted 20°. Vertical foregrip 2×7×2 rotated 45° about Y with a 3×1×4 clamp. Grip cap 3×1×4 tilted 18° [0.99]. Trigger 1×3×1 tilted −10°.
* Short muzzle 1×1×2 rot45 + 2×2×1 [0.7, 0.7, 1].
* **Stock:** 3×2×11 (z 11…22), 1×10×2 tilted 20°, 3×10×2 butt, 2×2×2 and 2×2×4 rot45, 3×1×3 tilted −20° [0.99].
* **Holo sight:** 4×3×9 base, 4×1×4 hood, two 1×2×4 ears (all [0.8, 0.8, 0.8]), red-dot plane 1×1×0.

### ModelLMG: 128×128, family Z, 46 boxes, 1 part
Extent z −44…29, y −12…11.5, x −7.5…5.5.
* **Barrel:** 2×2×18 rot45 (z −44…−26), 2×2×8 rot45 and 3×3×2 rot45 collars, 1×2×4 sight base, front sight 1×4×1 (tilted 25°) + 1×2×2 + 1×3×1.
* **Heavy receiver:** 5×6×18 front (z −26…−8) with 4×1×18 and 3×1×16 top plates. 5×5×21 main receiver (z −8…13) with 4×1×20 top cover, 3×3×8, 3×2×2 and 5×3×10 underside. Top rail 2×1×17, bottom rail 2×1×12.
* **Holographic sight:** 3×2×8 base, 1×2×5 sides ×2, 1×3×4 uprights ×2, 2×1×4 top bar, 1×1×0 red dot.
* **Box magazine** on the left side: 13×6×7 (x −7.5…5.5, y 5…11), 9×4×7 top, 3×6×7 rotated 45° about Z (feed lip).
* **Belt:** six 1×1×6 rot45 rounds (Bullet01–[06](../06-items/index.md)) stepping from the box up to the receiver (x 2.5…4.5, y −4…0.5). Visibility depends on ammo ([rendering-framework.md §10](rendering-framework.md#10-ammo--and-state-dependent-geometry)).
* Grip 3×9×4 tilted 26°, trigger pieces, foregrip 3×2×4 + 2×8×2 rotated 45° about Y.
* **Stock:** 3×3×8 tilted 10°, two 3×5×10 side cheeks splayed (−12° X, ±6° Y), 3×7×8 butt, 3×1×6, 3×1×7, 3×1×1.

### ModelLmgMag / ModelLmgMagE (item models): 128×128
The LMG's three magazine boxes. The full variant adds six 1×1×6 rot45 rounds lying in a row along the top (x 0…5).

### ModelAS50: 256×128, family Z, 50 boxes, 1 part
Extent z −67…33, y −19.5…11, x −4.3…4.5: an anti-materiel sniper rifle.
* **Barrel:** 2×2×38 rot45 (z −59…−21) and 1×1×19 rot45 upper barrel. 4×4×6 and 4×4×8 rot45 barrel shrouds with a 2×2×14 top. Mid and front collars 3×3×3 rot45 with 2×1×2 lugs, 2×3×2 tilted −38°.
* **Muzzle brake** (z −67…−61): three 4×4×1 rot45 baffles, a 3×3×1 face, two 1×1×1 ports.
* **Receiver** 4×6×32 (z −15…17), 3×1×32 top. Top rail 2×2×25. Mid rail 2×1×14 with 2×1×1 ribs and a 2×3×1 front tilted −18°.
* **Magazine:** 2×8×12 (z −11.5…0.5), 2×2×11 tilted −5°, 2×2×1 base. Holder 3×4×13 tilted −5°.
* **Trigger group:** 4×3×17 upper, 3×1×13 lower, grip 3×9×4 tilted 26°, trigger 2×1×6 + 2×2×1 + 1×2×1.
* **Stock:** 4×5×14 lower (z 17…31), 4×4×16 rot45 cheek rest, 3×2×10 flip, butt plates 4×10×1 and 5×10×1 (z 31…33).
* **Scope:** rings 3×3×1 ×2, mount 3×1×10, objective bell 5×5×5 rot45 (z −18…−13), tube 3×3×14 rot45, 4×4×5 and 4×4×7 rot45 sections, center 4×4×4. Turrets 2×3×3 (rotated 45° about X) and 3×2×3 (45° about Y).

### ModelAS50Mag(empty) (item model): 256×128
The AS50 magazine boxes (2×8×12, 2×2×11 tilted, 2×2×1). The full variant adds a 2×2×6 rot45 cartridge with a 1×1×5 rot45 tip on top. In the empty variant those two `ModelRenderer` fields stay null and are never rendered.

### ModelThompson: 64×64, family X, 24 boxes, 1 part
Extent x −25…27, y −3.5…10.5, z −1…3.
* Barrel 18×2×2 rotated 45° about X (x 9…27). Finned barrel shroud 13×3×3 (x 10…23). Front sight 1×2×1 at x 25.5.
* Receiver 22×3×3 (x −13…9), 15×1×4 below, 5×1×4. Rear sight: two 2×1×0 planes and a 0×1×1 plane at x −12.
* Stick magazine 3×10×2 hanging below at x 2. Pistol grip 3×6×2 tilted 25° at x −5. Trigger group 3×2×3, 3×0×2, 1×2×3, 4×1×3, 7×1×3.
* Stock (x −25…−13): 10×3×3, 6×2×3, 4×2×3, 2×1×3, 1×1×3 ×2, 3×1×3, butt 2×6×3 at x −25.

### ModelPDW: 64×64, family X, 41 boxes, 1 part
Extent x −15…17, y −5…14, z −2…2: a bullpup personal defense weapon (P90-like).
* **Upper body:** 25×3×4 shell (x −13…12) with 25×1×3 top, 9×1×3 rear top, 13×1×2 sight rail (y −4). Rear sight block 3×2×3 with two 2×2×1 ears. Front sight 2×1×2 + 1×2×2 + 1×1×0 planes at x 9…10.
* **Muzzle** (x 12…17): 3×2×2 rotated 45° about X, 1×2×1 / 1×2×2 ears, two 1×1×1 rings (one rotated 45°).
* **Under-body:** 6×2×3, 6×1×2, 1×3×3, 1×1×2 trigger housing (x 4.5…11.5). 1×8×1 tilted 25° trigger guard, 4×8×2 tilted 25° pistol grip, 2×2×3, 4×3×3, trigger 2×0×2 rotated −45°.
* **Thumbhole stock:** 11×1×2 and 3×6×2 tilted 25°, 6×2×2 tilted 15°, 7×1×2, 5×1×3, 2×1×2 tilted 50°, 3×0×2. Butt 2×4×4 + 2×1×3 + 2×2×3 + 2×2×2 + 2×3×2 (tilted −28°) at x −15.

### ModelPulseRifle: 128×64, family X, 72 boxes, 1 part
Extent x −22…26, y −5.5…13, z −2.5…2.5: a slab-sided sci-fi rifle.
* **Muzzle** (x 17…26): 4×2×2 and 3×1×1 ×2 prongs, 2×1×1 (one rotated 45° about X), 9×2×2 rotated 45° about X barrel, 1×7×3 front plate, 1×1×1 rotated tip, 3×1×2 tilted 36°.
* **Upper receiver:** 24×2×4 spine (x −20…4), 14×1×4, 8×1×4, 7×1×4 stacked tops. 7×2×5 and 6×2×4 scope base (y −5.5). 2×3×3 sight posts ×2, 2×2×1 ×2, 2×1×4 ×2 (rear sight).
* **Body:** 7×4×5, 7×3×4, 7×2×4, 5×1×3, 11×1×2, 10×2×4 lower front, 1×2×4, 1×1×3. Ammo housing 3×2×3, 2×2×1 tilted 23°, 3×2×2.
* **Magazine and grip:** 10×1×3, 7×1×2, 1×3×2, 2×1×2 ×3, 5×1×2 and 5×1×3 tilted −36° (curved magazine). Grip 3×9×2 tilted 35°, 2×4×2 tilted 35°, 1×5×2 tilted 35°, trigger planes 3×0×2.
* **Stock** (x −22…−11): 2×5×5 butt with 2×1×4 top and bottom (all tilted −10°), 6×1×3, 9×1×4, 3×2×4, 2×1×3, 3×1×3, 6×1×3, 6×1×4, 5×4×2 / 5×5×2 tilted −15°, 2×5×2 tilted −38°, 7×1×2 / 3×3×2 / 2×4×2 / 2×1×2 lower frame.

### ModelBoltaction: 128×32, family X, 22 boxes, 1 part
Extent x −19…34.5, y −6…6, z −3…3: a scoped hunting or sniper rifle.
* Barrel 38×2×2 (x −3.5…34.5). Fore-end 24×1×3 + 13×1×3 underneath. Action 7×2×3 + 2×3×3.
* Stock: 8×4×3 wrist, 3×7×3 butt (x −19), 9×2×1 side plates ×2, 3×1×3 and 2×1×3 lower pieces.
* Scope: two 3×3×3 bells (x −7 and 3, y −6…−3), 8×2×2 tube, 2×2×1 turret, 1×2×3 mounts ×2, 1×1×1 mount caps ×2, 1×1×1 top cap.
* Bolt 7×1×1 and **bolt lever** 1×1×3 sticking out to z −3. These are static geometry. The bolt "cycling" is faked by the whole-gun rechamber motion.

### ModelBlasterRifle: 128×64 (used with a 256×128 PNG), family Z, 23 boxes (Tabula), 1 part
Extent z −26…24, y −9.5…11, x −3…10.
* Receiver 4×5×28 (z −6…22), 2×1×27 top, 3×2×15 lower, 5×5×2 rot45 rear cap (z 22…24).
* Barrel guard 4×4×19 rot45 (z −25…−6) with two **1×5×16 cooling fins rotated ±45° about Z** (an X-shaped radiator). Front lip 3×2×7 tilted −13°. Muzzle 3×3×1 rot45 (z −26). Under-rail 2×2×15.
* Side energy magazine (right side, x 1…10): 5×2×5 cap, 4×3×7 housing, 3×1×1 neck, two 1×1×6 rot45 cells. Side box 3×5×8 (z 6…14).
* Scope: 3×3×5 and 3×3×8 rot45 tubes, 2×2×2 rot45 front. Shared grip and trigger skeleton.

### ModelLaserGunNew ("Scatter Beam Gun"): 128×128, family Z, 44 boxes, 1 part
Extent z −35…28, y −11.5…15, x −3…3.
* **Emitter:** 4×4×19 front box (z −32…−13) ending in stacked plates: 4×1×3 top lip (z −35), 4×4×1 rotated 45° about X, 4×1×1, 4×2×1. Under-pipe 2×2×16 rot45 + 2×2×2 + 2×5×2 tilted 54°.
* **Body:** 5×5×27 main box (z −13…14), 4×1×27 top, 2×1×12 rail, 3×3×15 lower, 3×4×8 handguard.
* **Fins:** two 1×1×6 tilted 11° (top front), 1×3×6 splayed ±5° (front sides), 1×3×6 splayed ±10° (rear sides), 3×1×6 tilted −11° (rear top).
* **Energy cell** on the right (x 2…3): 1×3×4 cell, three 1×4×1 fins rotated −45° about Y, two 2×1×4 caps, 1×3×3 mount rotated 20° about Y.
* **Holo sight:** 4×1×3 base, two 1×4×1 posts, 3×3×0 glass plane (z 2.5).
* Grip skeleton plus a 2×1×5 front guard tilted −36°.
* **Skeleton stock:** 3×4×15 top bar (z 13…28), 2×15×2 diagonal strut tilted 63°, 2×10×2 butt, 1×8×5, 1×5×3, 2×4×2, 2×2×4, and a 1×7×2 that is rendered twice.

---

## Energy and exotic weapons (X-family unless noted)

### ModelLasergun: 64×64, 41 boxes, 1 part
Extent x −22…31, y −7.5…12, z −2…4.
* **Square emitter barrel** (x 9…25): two 16×6×1 side walls and 16×1×6 top and bottom walls make a hollow box. Two **7×4×4 glow blocks** (Glow2 x 18…25, Glow3 x 9…16) sit inside and are visible through the transparent slots cut in the wall texture.
* **Muzzle** (x 25…31): 2×4×6 ring, 2×1×4 top and bottom, 4×1×3 and 4×3×1 claw plates.
* **Junctions:** 2×4×6 + 2×1×5 ×2 at x 7. 2×6×2 + 2×2×6 at x 16.
* **Body** 17×6×4 (x −10…7) with 15×2×1 side rails, 6×2×3 top block, rear sight 3×3×1 ×2 + 3×1×2 (y −7.5), and a tiny zero-width **Glow5** indicator.
* **Energy cell:** 8×6×6 housing (x −1…7, y 1…7) holding **Glow1** 4×4×6, 8×1×5 under-plate.
* Grip 3×8×2 tilted 20°, 7×1×3, 2×1×1, 1×2×3, 4×1×2.
* **Stock** (x −22…−14): 2×6×4 butt, 2×4×3, 2×1×3, 10×1×2 strut, 4×1×2 ×2 (one tilted −44°).
* Glow1, 2, 3 and 5 are full bright. A Glow4 field does not exist.

### ModelTeslaGun: 64×64, 63 boxes, 1 part
Extent x −27.5…22, y −6…13, z −4…7.
* **Coil head** (x 6…22): 8×5×2 + 8×2×5 cross-shaped core, four 10×1×1 rods at the corners, 1×4×4 rings at x 6 and 8, 1×3×3 at x 7, emitter 2×3×3 rotated 45° about X + 3×2×2 tip (x 17…22).
* **Glowing studs:** eight 2×2×2 diamonds (rotated 45° about Y or Z) on top, bottom and both sides at x 10 and 14, each with a 1×1×1 **Glow** cube (Glow1–8, full bright).
* **Body:** 10×4×6 (x −8…2), 5×5×5 (x 1…6), two 3×3×4 capacitors, 8×2×3, 8×1×5 under-plate, 3×2×3. Diagonal 13×1×1 coil wires rotated 45°/−135° about X with 1×1×4 struts.
* **Top handle and sight:** two 1×3×1 posts, two 1×2×1 rotated 45°, zero-width plate (Shape43, full bright).
* Foregrip 2×6×2 rotated 45° about Y. Grip 3×7×2 tilted 18°, 4×1×3 tilted 18°, 4×2×3, 1×2×1.
* **Stock** (x −27.5…−14): 3×5×5, 4×5×4, 3×4×3 blocks, 8×1×2 strut, two 2×5×1 diagonals (45° about Z), two 8×2×1 rails, butt 1×4×3 + 1×6×4 + 1×2×3.

### ModelBiogun: 64×64, 41 boxes, 1 part
Extent x −23…15, y −2.5…16, z −3…4.
* **Muzzle** (x 10…15): 5×3×7 block with 5×1×6 top and bottom. Ring 1×3×7 + 1×1×6 ×2 at x 8.
* **Glow chamber** (x 0…9): Glow1 8×3×6 and Glow2 8×4×5 + Glow3 1×4×5, caged by six 8×1×1 bars.
* **Rear body:** 4×3×7 + 4×1×6 ×2 (x −4…0), 6×4×5 (x −10…−4), 8×1×4, 2×4×4, 1×4×3. Carry rail 8×1×1 ×2 on four 1×2×1 posts (y −2.5).
* **Bio tank:** a canister under the rear, tilted −30° about Z: 4×8×4 shell holding Glow4 5×6×3 and Glow5 3×6×5, 5×2×5 cap, 3×1×3 neck. 6×3×4 and 4×3×3 mounting blocks.
* Grip 3×7×2 tilted 20°.
* **Skeletal stock** (x −23…−11): 1×7×2 butt, 1×4×1 and 1×4×3 uprights, 9×1×1 and 4×1×1 ×2 rails.
* Glow1–5 are full bright.

### ModelCyberdemonBlaster: 64×64, family Z, 24 boxes, 1 part
Extent z −9…24, y −5.8…5, x −4…4: a forearm-mounted cannon (it is also the Cyberdemon NPC's weapon).
* **Barrel body:** 6×5×6 core (z −3…3), 6×5×4 blocks fore and aft, 7×7×6 (z 8…14), 4×3×19 underside spine, 4×1×14 top plate, side ribs 1×4×6 ×2, rails 1×1×13 ×2 (top) and 1×1×15 ×2 (bottom), zero-width fins 0×1×10 ×2, 4×1×2, 4×4×1 front plate (z −9).
* **Arm cuff** (z 14…24): 8×8×3 + 8×8×7, both tilted 22° about X, with 4×1×6 straps above and below.
* **Glow:** Glow1 5×4×8 core (z −4…4), Glow2 2×2×1 muzzle (z −9), Glow3 and Glow4 3×3×7 underneath. Full bright, textured with a lava-orange patch.

### ModelAlienBlaster: 128×64, family Z, 24 boxes, 1 part
Extent z −18…18, y −7…11, x −4…3.
* **Body:** 6×6×12 rot45 (z 1…13) diamond-section bulb, 5×5×2 rot45 rings at z −1 and 13, then a cone of 4×4×2, 3×3×1 and 2×2×1 rot45 (z −5…−1).
* **Barrel:** 1×1×12 rot45 (z −17…−5). Muzzle 2×2×2 rot45 + two 3×3×1 rot45 rings (z −15, −13).
* **Rear cell:** 2×2×2 rot45 + 1×1×1 (z 15…18).
* **Fins:** 3×1×7 side fins angled ±25° about Y, 1×3×7 top fin tilted 25° about X.
* Six 1×1×1 rot45 light studs on top (z 4, 6, 8, x ±2). Not full bright.
* Grip 3×9×4 tilted 26° + 3×3×5 grip block, trigger 1×3×2 tilted 15°.

### ModelNDR ("Nuclear Death Ray"): 64×64, 68 boxes (Tabula with child parts), 1 part
Extent x −32…14.5, y −12…10, z −6…10.
* **Reactor core** (x −5…3): 7×4×6 housing, 7×6×4 cross block, **GLOW 7×4×4** (full bright), 1×4×6 end plates, 1×1×4 bars.
* **Heat-sink fins** (x 4…11): 3×2×3, 3×1×4, 4×1×4 plates at 60° steps (hexagonal arrangement).
* **Emitter** (+X): three prong assemblies at 0°, 120° and −120° about X. Each has a 3×2×2 base, a 9×1×1 arm and a 4×1×1 tip bent 45° about Z, giving a three-claw emitter ending at x 14.5. One prong carries a 2×6×2 antenna rotated 45° about Y and a 1×1×1 cap.
* **Barrel tube** (rear, −X): two hexagonal rings, each a 3×3×5 plate with five 3×3×5 children at ±60°/±120°/180° (at x −8 and x −21). Between them a hexagonal 8×3×3 / 8×3×6 sleeve (x −17…−9), built as a parent with four rotated children.
* **Frame:** 14×1×4 top rail, 6×1×3 and 10×1×3 under-rails, 1×4×4 rot45, 1×7×4 uprights ×2. Grip 3×2×2 + 2×5×2 + 1×3×1 tilted −23°.
* **Rear stock** (x −32…−23): 2×6×4, 2×2×1 ×2, 3×4×1, 1×4×1, 6×2×1, 5×4×0, 1×1×1.
* **Firing crown:** glowA1 7×5×1 (see [rendering-framework.md §10](rendering-framework.md#10-ammo--and-state-dependent-geometry)).

### ModelNeutrinoGun (unused): declared 128×128 (PNG 256×256), family Z, 139 boxes (Tabula), 1 part
Extent z −40…26, y −2.5…7.5, x −8…9. A large rifle built from bundles of parallel rods. The item itself is never created ([index.md bugs](index.md#suspected-bugs)).
* **Barrel bundle** (z −33…−10): 1×1×23 rot45 rods ×6, 1×2×23 ×2, 2×4×23, 1×4×18 half slab, with 1×1×2 / 1×1×3 rot45 spacers.
* **Muzzle** (z −40…−34): twelve 1×1×6 / 2×1×6 / 1×2×6 exhaust rods, 2×4×1 and 1×2×1 rings, 1×1×1 rot45 ring studs, 4×3×1 holder, 2×1×1 frontal holder, 3×1×2 side spike, 1×1×1 frontal button, 5×0×8 cable plane.
* **Left-side finger grips** (z −31…−15): 2×4×1 / 1×4×2 / 2×4×2 plates rotated 20° or 72° about Y.
* **Side barrels** at z −14: 2×4×4 with 1×1×4 / 1×2×4 rot45 rods.
* **Body:** 9×8×6, 9×4×6, 9×4×4 blocks, 9×5×6 rotated 45° about X, 9×1×1 rotated 45° about X, 2×6×5 vent, 2×4×2 side cube. Control panel 3×6×3 rotated 45° about X with two 1×2×2 regulators, 1×1×1 regulators, buttons and a light. Tubes 1×1×5 rot45 ×9 and 2×4×5, 1×2×5 at z −11.
* **Under-handle** (z 0…26): 2×4×26, 1×2×26 ×2, 1×1×26 rot45 ×4, 4×1×24. Finger grooves 4×2×1 / 4×1×2 rotated 20°/72° along z 7…22. Back rollers 1×2×2, 2×4×2, 1×1×2 rotated 46°, back studs 1×1×2 rot45. 7×0×5 cable plane.
* Most boxes share UV origin (0,0) (lazy unwrap).

### ModelGrimReaper: 128×64, family X, 59 boxes, 1 part
The model's render applies **rotY(180)** before drawing, without push/pop, so the authored −X end becomes the front. Authored extent x −18…31, y −6.5…15.5, z −11.5…7.5: a four-tube rocket launcher.
* **Front housing** (authored x −14…3): 17×9×11 block, 17×1×9 top and bottom plates, 6×6×1 side plates ×2, 6×2×10 lower plate.
* **Muzzle face** (authored x −18…−14): 4×3×3 center, eight 4×3×1 / 4×1×3 dividers and a 4×8×1 / 4×1×10 frame forming four barrel openings. Sight vanes 4×0×11, 4×1×0, 4×4×0 planes and 2×2×1 knobs at x −16.
* **Quad tubes** (x 3…27): two 24×11×4 and two 24×4×11 slabs crossing to form four tubes. Rear plate 4×12×10 (x 27…31) with 4×10×1 ×2.
* **Mid band** (x 0…16): 16×1×10 top and bottom, 16×1×2 edges ×4, 16×8×1 side plates ×2.
* **Grips:** 4×1×3, 3×7×2 rear grip (x 3, y 7.5), 7×1×4, 2×7×2 rotated 45° about Y front grip (x −9), 4×1×4.
* **Side sight** (on −Z): 1×2×2 rot45, 3×3×2 tilted 36°, 4×3×3 rot45, 2×3×3 rot45, 2×2×2 rot45, 3×2×2.

### ModelRocketLauncher (rocket and nuke launcher): 128×64, family X, 29 boxes, 1 part
Extent x −19…29, y −6…11, z −3.5…4.5.
* **Tube:** 36×5×5 core (x −16…20) with 36×3×6 and 36×6×3 cross slabs (octagonal look).
* **Front bell** 6×6×6 (x 20…26) with 6×1×3 / 6×3×1 rim strips ×4. **Rear bell** 3×6×6 (x −19…−16) with 3×1×4 / 3×4×1 strips ×4.
* **Grip and sight block** (x 6…13): 6×7×5 body, 3×1×3 top, 6×5×1 side plates ×2, pistol grip 3×7×2 tilted 17°, trigger 2×1×1, flip-up sight (1×0×2 + two 1×3×0 planes at y −6).
* **Loaded rocket** (conditional): 10×3×3 body (x 18…28), 6×4×4 warhead, cross fins 4×2×5 / 4×5×2 / 2×6×1 / 2×1×6, 1×2×2 nose tip at x 28. The rocket nose shows in the tube mouth.

### ModelRocket (rocket item / projectile): 128×64, 11 boxes
21 units long (x −10…11): 6×4×4 warhead (x 3…9) + 1×2×2 nose (x 10), body halves 10×3×3 (x 0…10 and −10…0), canards 4×2×5 / 4×5×2 (x 4…8) and 2×6×1 / 2×1×6 (x 4.1), motor 6×4×4 (x −9.9…−3.9), tail fins 2×6×1 / 2×1×6 (x −9.8). It uses the rocket UVs of the launcher texture (`rocketlauncher.png` or `nukerocketlauncher.png`). The projectile renderer is area 04.

### ModelFlamethrower: 64×64, family X, 36 boxes, 1 part
Extent x −18…24.5, y −3.5…9, z −2.5…3.5.
* **Fuel tank** under the barrel: 14×6×4 (x 2…16, y 2…8) with 14×4×1 side plates and 1×4×4 / 1×3×4 end caps.
* **Barrel:** 13×4×2 core + 13×2×1 ×2 side rails (x 5…18).
* **Nozzle** (x 18…24.5): 1×7×4 shield, 2×3×3 block, 3×2×2 tip with four 2×1×1 prongs, 1×2×2. The **pilot-light arm** is a 4×1×1 tilted 21° plus a 1×3×1 bent −151°.
* **Body:** 4×4×5 (x 1…5) + 4×1×3 top. Rear body 10×5×3 (x −9…1) + 10×1×2 top. 9×1×1 side pipes ×2.
* Valve 2×2×2, 2×1×2, 1×1×2. Grips 2×6×2 (pistol grip), 1×5×2, 2×1×2.
* **Stock:** 7×1×2 bar, 4×1×2 ×2, butt 2×5×3 + 2×3×2 at x −18.

### ModelPowerHammer: 64×64, family X, 40 boxes, **2 parts**
Extent x −16…15, y −4.5…11, z −3…9.5: a pneumatic sledgehammer.
* **Part 0, body:**
  * 13×4×5 main housing (x −9…4), 11×4×6 upper, 12×1×4 bottom, 8×1×3 belly.
  * Front housing 1×6×7, 2×4×6, 1×4×7 rotated −45° about Z, 3×1×4 ×2, 3×1×3 ×2.
  * Cooling fins 1×2×6 ×2. Top valve 4×2×4 + 3×1×3. Plates 5×1×4, 2×1×4.
  * Side air tank 2×2×6 rot45 (z 3.5…9.5) + 3×3×2.
  * Grip 3×6×2 tilted 15° with trigger planes.
  * Rear: 1×5×4, 3×2×2, 3×1×1 ×2, butt 2×2×4 + 1×5×3 + 2×1×4 + 2×5×5 (x −16…−13).
* **Part 1, hammer and piston:** piston 7×3×3 rotated 45° about X (x 6…13), four 7×1×1 rot45 guide rods, head 1×5×5 (x 13) + striking face 1×6×4 (x 14), 1×1×4 lips top and bottom. It pulls back while charging and slams forward on release.

### ModelChainsaw: 128×64, family X, 47 boxes (Tabula), 1 part
Extent x −22…48, y −14…5.5, z −6…11.
* **Engine block:** 13×9×11 (x −3…10), 5×12×8 (x −8…−3), 10×3×8 top, 8×2×6, 3×3×6, 3×1×6, 1×1×6, 3×1×11 bottom plate.
* **Front guard frame** (x 10…12): 1×7×7, 2×8×2. Hand guard: 1×6×1 ×2 tilted 42°, 1×5×1 ×2, 1×1×6 ×3.
* **Bar:** 33×6×1 (x 12…45, z −3.5) with a 1×5×1 + 1×3×1 tip.
* **Chain:** two alternating zero-thickness planes blade1 / blade2 36×8×0 (x 12…48, z −3), animated ([rendering-framework.md §10](rendering-framework.md#10-ammo--and-state-dependent-geometry)).
* **Top/front handle** (arching over the engine): 2×10×2 vertical post, 2×7×2 tilted 35° about X, 2×2×6 rotated 35° about Y, 2×2×7, 2×2×2, 12×2×2 tilted −49° about Z.
* **Rear handle loop** (x −22…−9): 9×2×2 ×2 (one tilted −13°), 2×3×2 ×2, 2×2×2 ×2, 1×1×2 ×3, 1×5×2.
* **Details:** starter 2×2×2 + 2×1×1 ×2, side exhaust 5×5×1 rotated 45° about Z, plates 3×3×1, 4×4×1, 2×3×2, 3×3×1.

### ModelMinigun: 128×128, family X, 73 boxes (Techne), **2 parts**
Extent x −24…38, y −11…10.5, z −13.5…9.
* **Part 0, housing:**
  * Body 16×6×10 (x −13…3) with 16×1×8 and 16×1×6 skins top and bottom.
  * Rear spade grips: 2×11×2 upright, 13×1×2 top bar, two 10×2×2 bars converging at ±15°, 2×5×2 end post at x −24.
  * **Ammo box** on the −Z side: 12×7×9, 14×6×8, 14×1×6 ×2, 12×1×4 ×2, 12×1×7 ×2, 12×4×1 ×2.
  * Feed chute on +Z: 12×3×3 and 10×4×4 rotated 45° about X, two 1×5×5 rot45.
  * Front bearing ring (x 4…7): 3×2×14 + 3×2×2 + 3×3×2, 2×6×1 ×2, 2×4×1 ×2 rotated ±45° about X, 2×1×8.
  * Carry handle: 2×1×7 + 2×3×1 ×2 at y −11…−7.5.
  * Motor detail blocks (2×1×8 rotated 125°/55° about Y, small rot45 cubes).
* **Part 1, rotor:** six 21×2×2 barrels (x 5…26) and six 12×1×1 barrel tips (x 26…38) at 60° steps about the X axis. Three clamp discs built from crossing plates at x 3…5, x 17 and x 35. The rotor spins about X while firing.

### ModelDriller (unused): 128×64, family Z, 18 boxes, 2 parts
* Part 0: 7×7×28 drill motor body (z −16…12) on the shared grip and trigger skeleton.
* Part 1: drill bit made of stacked 2-thick plates shrinking from 11×11 to 1×1 (z −18…−36), alternately rotated 45° about Z.
* Paired with the unused `RenderDriller` (spins part 1 about Z) and [`textures/guns/drillTexture.png`](textures.md#measured-colors-all-62-textures). The Driller item is commented out.

### ModelMp5 (unused): 64×32, family Z, 18 boxes, 1 part
* 2×2×20 receiver, 1×1×19 top rail, 2×1×11 / 2×1×8 lower body, 1×1×2 barrel, 1×4×1 front sight, 1×1×2 rear sight.
* Magazine 2×2×2 + 2×3×2 + 2×2×2 (curved). Stepped grip 2×(3, 5, 4)×1, trigger 1×2×1 + 1×1×2.
* Stock 2×1×8 + 2×3×1 + 2×6×1.
* Paired with [`textures/guns/mp5Texture.png`](textures.md#measured-colors-all-62-textures). The MP5 item is commented out.

---

## Grenades

### ModelStielgranate: 32×32, 3 boxes
Vertical (Y axis), extent y −27…−9: a stick grenade.
* Handle 2×12×2 rotated 45° about Y (field `Granate01`).
* Head 4×5×4 on top (field `Stiel01`, y −27…−22).
* End cap 3×1×3 at the handle bottom (field `Stiel02`).
* The field names are the reverse of the German words. The same model renders the thrown projectile (area [04](../04-projectiles-fx/index.md)).

### ModelFragGrenade(showRing): declared 32×32 (used with a 64×64 PNG), 11 boxes
Vertical, extent y −20.2…−11.
* Body 4×4×4 plus two 4×2×4 boxes rotated 45° about Y (infl −0.1): an octagonal egg.
* Top and bottom caps 3×1×3 with rotated copies.
* Fuze 2×2×2 (infl −0.2).
* Spoon lever 4×1×2 tilted 23° and 5×1×2 rotated 90° about Z (infl −0.3) running down the side.
* Pull ring 2×4×4 (infl −0.9, which shrinks it to a thin ring), offset to z −3.3. Created and drawn only when showRing is true and fireProgress ≤ 0.

---

## OBJ model: `models/guns/grenadelauncher.obj`

Exported with "Max2Obj Version 4.0", all triangles, 252 vertices, 594 texture vertices, 252 normals. Two groups, which `ModelModel` maps to parts:

| Group | Faces | Extent (model units) | Content | UV range |
|---|---|---|---|---|
| `0` | 324 tris (240 verts) | x −26…18, y −12…9, z ±6.93 | Launcher body: long barrel, frame, stock and grip. The X axis is the long axis. The muzzle-flash placement suggests the barrel points to +X. | Whole sheet |
| `1` | 12 tris (12 verts) | x −4…4, y −11…3, z ±6.06 | **Revolving drum**: a hexagonal prism around the X axis. The renderer rotates it about X during firing. | u 0.797…0.953, v 0.469…0.867: the strip of red 40 mm rounds on the right of the texture |

Used only by `RenderGrenadeLauncher` with [`textures/guns/grenadelauncher.png`](textures.md#measured-colors-all-62-textures). A second OBJ, [`models/entity/launchergrenade.obj`](../10-entities-npcs/rendering-and-models.md#other-obj-files-in-modelsentity-in-this-folder-used-elsewhere), renders the grenade projectile (area [04](../04-projectiles-fx/index.md)).

---

## ModelSniperScope (unused)
A ModelBase holding a texture reference and one 1080×1080×0 plane. Texture size is not set (vanilla default 64×32). A commented-out tessellator quad remains in the source. It is never instantiated. The scope overlay is drawn by [`ScreenEffect.sniperScope`](../04-projectiles-fx/renderers.md#screeneffect-muzzle-flashes-and-scope-overlays) instead ([rendering-framework.md §9](rendering-framework.md#9-zoom-and-scopes)).

---

## Model to texture and size table (all model classes)

*Added in the consistency pass.* Each model with the texture it is drawn with and both sizes: the UV size the model class declares (which normalizes every box UV) and the measured size of the PNG ([ASSET-FACTS.md](../ASSET-FACTS.md)). A ratio of x2 means the PNG is a higher-resolution version of the same layout and all UVs scale by two. Texture names are `textures/guns/<name>.png`. Colors and layouts are in [textures.md](textures.md).

| Model class | Declared UV size | Texture | PNG size | Ratio | Used by |
|---|---|---|---|---|---|
| ModelHandgun | 64x32 | `handgun` | 64×32 | x1 | Hand Cannon |
| ModelPistol3 | 64x32 | `pistol3` | 128×64 | x2 | Pistol, Akimbo Pistols |
| ModelPistol / ModelPistol2 (unused) | 64x32 | [`pistol`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) / `pistol2Texture` | 64×32 | x1 | none |
| ModelPistol3Akimbo (unused) | not used | `pistol3` | 128×64 | x2 | none |
| ModelRevolver | 64x32 | [`revolver`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) | 64×32 | x1 | Revolver, Akimbo Revolvers |
| ModelGoldenRevolver | 64x32 | [`goldenrevolver`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) | 64×32 | x1 | Golden Revolver, Akimbo Golden Revolvers |
| ModelMac10 | 64x32 | `mac10texture` | 128×64 | x2 | Mac-10, Akimbo Mac-10 |
| ModelMibGun | 64x64 | `mibgun` | 128×128 | x2 | MIB Gun, Akimbo MIB |
| ModelSawedoff | 64x32 | `sawedOff` | 64×32 | x1 | Sawed-off |
| ModelCombatShotgun | 64x32 | `combatShotgun` | 64×32 | x1 | Combat Shotgun |
| ModelSonicShotgun | 128x64 | `sonicshotgun` | 128×64 | x1 | Sonic Shotgun |
| ModelAK | 128x64 | `ak47Texture`, `_1` | 128×64 | x1 | AK-47 |
| ModelM4 / ModelM4Mag / ModelM4MagE | 128x64 | `m4Texture`, `_1` to `_4` | 128×64 | x1 | M4 and its magazine items |
| ModelM4_UQ | 128x64 | `m4_uq_Texture` | 128×64 | x1 | M4 Infiltrator |
| ModelAUG | 128x64 | `AugTexture`, `_1` to `_4` | 128×64 | x1 | AUG |
| ModelScar | 128x64 | `scar_texture`, `_1` | 256×128 | x2 | SCAR |
| ModelVector | 128x64 | `vector_texture`, `_1` | 256×128 | x2 | Vector |
| ModelLMG / ModelLmgMag / ModelLmgMagE | 128x128 | `mg2_texture` | 128×128 | x1 | LMG and its magazine items |
| ModelAS50 / ModelAS50Mag | 256x128 | `as50Texture` | 256×128 | x1 | AS50 and its magazine items |
| ModelThompson | 64x64 | [`thompson`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) | 64×64 | x1 | Thompson |
| ModelPDW | 64x64 | `PDW`, `_1`, `_2` | 64×64 | x1 | PDW |
| ModelPulseRifle | 128x64 | [`pulseRifle`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition), `_1`, `_2` | 128×64 | x1 | Pulse Rifle |
| ModelBoltaction | 128x32 | `boltactionrifle`, `_1`, `_2` | 128×32 | x1 | Bolt-action |
| ModelBlasterRifle | 128x64 | [`blasterRifle`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) | 256×128 | x2 | Blaster Rifle |
| ModelLaserGunNew | 128x128 | `laserGunNew` | 128×128 | x1 | Scatter Beam Gun |
| ModelLasergun | 64x64 | [`Lasergun`](../02-guns/sheets-energy-special.md#laser-rifle-lasergun) | 64×64 | x1 | Laser Gun |
| ModelTeslaGun | 64x64 | `TeslaGun` | 64×64 | x1 | Tesla Gun |
| ModelBiogun | 64x64 | `BioGun` | 64×64 | x1 | Bio Gun |
| ModelCyberdemonBlaster | 64x64 | `cyberdemonblaster` | 64×64 | x1 | Cyberdemon Blaster |
| ModelAlienBlaster | 128x64 | `alien_blaster` | 128×64 | x1 | Alien Blaster |
| ModelNDR | 64x64 | `NDR` | 64×64 | x1 | Nuclear Death Ray |
| ModelNeutrinoGun (unused) | 128x128 | `neutrinogun` | 256×256 | x2 | none |
| ModelGrimReaper | 128x64 | [`grimreaper`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) | 128×64 | x1 | Grim Reaper |
| ModelRocketLauncher / ModelRocket | 128x64 | [`rocketlauncher`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) / `nukerocketlauncher` | 128×64 | x1 | Rocket and Nuke Launcher, rocket ammo items |
| ModelFlamethrower | 64x64 | [`Flamethrower`](../02-guns/sheets-energy-special.md#flamethrower-flamethrower) | 64×64 | x1 | Flamethrower |
| ModelPowerHammer | 64x64 | [`powerHammer`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) / [`powerHammerAdv`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) | 64×64 | x1 | Power Hammer, Advanced Power Hammer |
| ModelChainsaw | 128x64 | [`chainsaw`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) | 128×64 | x1 | Chainsaw |
| ModelMinigun | 128x128 | [`minigun`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) | 128×128 | x1 | Minigun |
| ModelDriller (unused) | 128x64 | `drillTexture` | 128×64 | x1 | none |
| ModelMp5 (unused) | 64x32 | `mp5Texture` | 64×32 | x1 | none |
| ModelStielgranate | 32x32 | `stielgranate` | 32×32 | x1 | Stielhandgranate |
| ModelFragGrenade | 32x32 | `fragGrenade_texture` | 64×64 | x2 | Frag Grenade |
| `models/guns/grenadelauncher.obj` | none (normalized OBJ UVs) | [`grenadelauncher`](../02-guns/gun-catalog.md#table-a-identity-and-ammunition) | 128×128 | n/a | Grenade Launcher |
| ModelSniperScope (unused) | 64x32 (vanilla default) | none set | n/a | n/a | none |
