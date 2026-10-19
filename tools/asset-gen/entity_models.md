# Mob models: the data format and the Python DSL

A GregGuns mob model is data. A builder writes a Python function in [`entity_models.py`](entity_models.py); the script writes
`assets/gregguns/entity_models/<name>.json` and a painted sheet `textures/entity/<name>.png`. The game builds the model parts from the
JSON. No Java is needed to add a mob model; the entity class registers a renderer with one call (see "Java side").

Behavior reference: [rendering-and-models.md](../../docs/techguns-spec/10-entities-npcs/rendering-and-models.md). Code: `com.gregguns.mob.model`
(pure data, parser, validator, animation math) and `com.gregguns.client.mob` (parts, renderers, registration).

## Conventions

- Units are Minecraft model units (1/16 block). Y points **down**. The mob faces **-Z**; its right side is **-X**. Origin Y = 24 is the feet.
- Angles are **degrees** in the file. Rest rotations and animation angles all use degrees.
- A part's `pivot` is relative to its **parent's pivot** (a root's pivot is relative to the model origin). A box `offset` is the box corner
  relative to its **part's pivot**, exactly what `ModelRenderer.addBox` takes. A child follows its parent's rotation.
- Box `size` is whole numbers (the sheet unwrap needs integers). `0` is allowed on one or two axes (a flat plane). All three zero is not.
- A box never rotates by itself. To tilt a box, give it a child part with a `rotate`.
- Rotation order matches `ModelRenderer`: the vertex is rotated about X, then Y, then Z.

## JSON format

```json
{
 "name": "demo_biped",
 "textureWidth": 64, "textureHeight": 32,
 "biped": true,
 "parts": [
  {"name": "head", "pivot": [0,0,0], "rotate": [0,0,0],
   "boxes": [{"name": "skull", "uv": [0,0], "offset": [-4,-8,-4], "size": [8,8,8],
              "inflate": 0, "mirror": false, "material": "skin"}]},
  {"name": "visor", "parent": "head", "fullbright": true, "pivot": [0,0,0], "boxes": [ ... ]}
 ],
 "animations": [
  {"type": "look", "part": "head"},
  {"type": "swing", "part": "rightLeg", "axis": "x", "amplitude": 80.2}
 ]
}
```

| Level | Field | Meaning |
|---|---|---|
| model | `name` | Must equal the file name without `.json` |
| | `textureWidth`, `textureHeight` | Sheet size in pixels. An HD sheet at an integer multiple also works, because UVs are fractions. |
| | `biped` | Optional. `true` maps the model onto `ModelBiped` (below). |
| part | `name` | Letters, digits, underscore. Unique. |
| | `parent` | Optional. Another part's name. Parents may be listed in any order, loops are rejected. |
| | `pivot`, `rotate` | Rotation point and rest rotation (degrees). Default zero. |
| | `fullbright` | Optional. The part **and its children** draw at maximum light with vanilla lighting off (eyes, lamps). |
| | `boxes` | Optional list. A part with no boxes is a pure pivot. |
| box | `name` | For messages |
| | `uv` | `[u, v]` texture origin. The unwrap (`2*(d+w)` by `d+h`) must fit the sheet. |
| | `offset`, `size` | Corner and `[w, h, d]` |
| | `inflate`, `mirror` | Optional. `mirror` is for left limbs sharing the right limb's rectangle. |
| | `material` | Optional painter tag; the game ignores it. |

### Biped mode

`"biped": true` requires six **root** parts named `head`, `body`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg`. They become the six
`ModelBiped` fields, so `RenderBiped` draws held items on the right arm, the vanilla armor layers wear over the model, and `aimedBow` and
`heldItemRight` keep their meaning. Any other part must be a descendant of one of the six. The vanilla walk, aimed and sneak code does **not**
run: the model moves only as its `animations` say. Start from `vanilla_biped(name)`, which already carries the vanilla motion.

Without `biped`, the model is generic (`DataMobModel`) and may have any roots.

## Animations

A list, run in file order for each frame, starting from the parts' rest rotations. Each entry targets one `part`. Unknown types, parts, fields
and bad values fail at load with a message naming the entry.

| `type` | Fields | Adds to the part's angle |
|---|---|---|
| `look` | `yaw` (1), `pitch` (1), `maxYaw`, `maxPitch` | `clamp(netHeadYaw, maxYaw) * yaw` on Y and `clamp(headPitch, maxPitch) * pitch` on X. Limits default to none. |
| `swing` | `axis`, `amplitude`, `speed` (0.6662), `phase` (0) | `amplitude * limbSwingAmount * cos(limbSwing * speed + phase)` |
| `idle` | `axis`, `amplitude`, `speed` (0.1), `phase` (0), `offset` (0) | `offset + amplitude * sin(ageInTicks * speed + phase)` |
| `spin` | `axis`, `rate` | `rate * ageInTicks` (degrees per tick) |
| `pose` | `when`, `mode`, `angles`, `followYaw`, `followPitch`, `blend`, `window` | see below |

`axis` is `"x"`, `"y"` or `"z"`. `amplitude`, `offset`, `phase` and `rate` are degrees; `speed` is radians per unit of limb swing (`swing`) or per
tick (`idle`). A `swing` at limbSwing 0 and amount 1 equals its amplitude times `cos(phase)`.

**Reproducing known motion.**

- Vanilla legs: `swing` x amplitude 80.2 (1.4 rad), the left leg `phase` 180. Vanilla arms: amplitude 57.3, the right arm `phase` 180.
- A gait with other frequencies: set `speed` to the multiple (`2 * 0.6662`, `3 * 0.6662`). A mirror leg uses a negative amplitude or `phase` 180.
- A cosine idle (`cos(age * 0.1)`) is `phase` 90 on an `idle`.

### Pose

Angles that apply on a condition, for the aimed pose and attacks.

- `when`: `aiming` (weight 1 while the mob aims; false otherwise), `holding` (an item is in the right hand), or `attacking` (weight from the
  attack progress).
- `angles`: `{"x": .., "y": .., "z": ..}`, any subset. `mode` is `"add"` (default; adds the weighted angles) or `"set"` (moves the listed axes toward
  the given values by the weight, so it overrides an earlier swing).
- `followYaw`, `followPitch`: add that share of the head yaw to Y and of the head pitch to X (the aimed arms follow the head).
- `attacking` only: `blend` is `step` (1 inside the window), `cosine` (`0.5 * (1 - cos(pi * p))`) or `sine` (`sin(pi * p)`, out and back);
  `window` is `[from, to]` of attack progress (default `[0, 1]`), and `p` is the progress rescaled over it. The progress comes from the entity
  (`MobAnimSource.attackProgress`) or, when it has none, from the vanilla swing progress. An alien-bug-style bite is a `cosine` pose on the jaw
  with a window covering the bite.

The aimed pose of a biped, as in `vanilla_biped`:

```python
m.pose("rightArm", "aiming", x=-90, y=-5.73, z=0, mode="set", follow_yaw=1, follow_pitch=1)
m.pose("leftArm", "aiming", x=-90, y=28.65, z=0, mode="set", follow_yaw=1, follow_pitch=1)
```

## Python DSL (`entity_models.py`)

```python
m = Mob("name", 64, 64, biped=False)        # sheet size; biped=True for ModelBiped mode
p = m.part("head", parent=None, pivot=(0, 0, 0), rotate=(0, 0, 0), fullbright=False)
p.box("skull", x, y, z, w, h, d, uv=None, inflate=0, mirror=False, material="skin", group=None)
m.look(part, yaw=1, pitch=1, max_yaw=None, max_pitch=None)
m.swing(part, axis, amplitude, speed=None, phase=0)
m.idle(part, axis, amplitude, speed=0.1, phase=0, offset=0)
m.spin(part, axis, rate)
m.pose(part, when, x=None, y=None, z=None, mode="add", follow_yaw=0, follow_pitch=0, blend=None, window=None)
m.palettes["name"] = {"default": "#808080", "skin": "#c89a78"}   # one entry per sheet (color variants)
m.detail = fn(mob, key, put, rect)           # optional: paint faces, eyes, marks over the base colors
```

- `uv=None` lets the packer place the box. Boxes with the same `group` share one rectangle (give a mirrored left copy the right limb's group
  and `mirror=True`). A pinned `uv` keeps a known layout (the vanilla biped sheet).
- The painter fills each face from `palettes[key][material]` with edge bevels and fine noise. In `detail`, `put(x, y, "#rrggbb"-tuple)` sets a
  pixel and `rect(box, "front")` gives a face's `(x, y, w, h)` (`top bottom right front left back`).
- `vanilla_biped(name)` returns a biped with vanilla proportions and vanilla motion; `demo_biped()` adds a fullbright visor and is what the
  tests use. New mobs add a function and list it in `MODELS`.
- Preview: `preview(mob)` returns front, side and top flat-shaded views of the rest pose beside the sheet. The script writes
  `docs/assets/previews/entity_models.png`.

Run from the repo root: `python -I -X utf8 tools/asset-gen/entity_models.py` (Pillow needed for the sheet and preview).
`--emit-json <dir>` writes only the JSON files and needs just the standard library; the unit test uses it to check the committed JSON equals what the
DSL produces.

## Java side (for the entity author)

Common code (server safe): `MobModels.load(name)` returns a validated `MobModelDef`. An entity class may implement `MobAnimSource`
(`isAiming()`, `attackProgress(partial)`) to drive `aiming` and `attacking` poses; without it a biped aims when a `gregguns` gun is in the
right hand and attacks follow the vanilla swing progress.

Client init (the client proxy), one call per entity class:

```java
MobRenderers.biped(Class<? extends EntityLiving> entity, String model, String texture,
                   float scale, float shadow, float gunX, float gunY, float gunZ);
MobRenderers.living(Class<? extends EntityLiving> entity, String model, String texture,
                    float scale, float shadow);
```

`texture` is a name under `assets/gregguns/textures/entity` (without `.png`); overloads take a `ResourceLocation` for other mods or vanilla.
`scale` scales about the feet (the shadow is unscaled); `gunX/Y/Z` is the held-gun offset in blocks in the right arm's frame, applied only when
a gun is held (the gun draws through its own third-person item renderer, in the vanilla hand frame). A bad model file throws at registration.
