# Warhead and body models

- **Baseline:** [icbm-spec 07-visuals-audio](../../icbm-spec/07-visuals-audio/index.md)
- **Design records:** [D31 (3D warheads and bodies)](../DECISIONS.md#d31-in-detail), [D43 (OBJ format)](../DECISIONS.md), [D47](../DECISIONS.md), [asset-sourcing.md](asset-sourcing.md)
- **Preview sheet:** `docs/assets/previews/icbm/warheads.png`, built locally and git-ignored (D50) (built by `tools/asset-gen/icbm/warheads_sheet.py`)

## Conventions for every model

- Wavefront OBJ in Forge 1.7.10's strict grammar, one group (`warhead`), triangles only, `v/vt/vn` corners, one texture each. Checked by loading through `render3d.obj_scene` in strict mode (the same reader the guide renderer uses).
- 1 unit = 1 block. **Nose up (+Y), base at y = 0.** Footprint center is the block-local middle of the footprint: (0.5, 0.5) for a 1 x 1 footprint, (1.0, 1.0) for a 2 x 2 footprint, measured from the footprint's minimum corner. Filler blocks fill the rest of the footprint (D31).
- Paths: `assets/gregguns/models/thirdparty/ntm/<name>.obj` and `assets/gregguns/textures/models/thirdparty/ntm/<name>.png` (outside the block atlas; bind the texture directly).
- Source for all six: HBM's Nuclear Tech Mod, LGPL-3.0, tag `1.0.27_X5846`. All six are modified (scaled, recentered, rewritten; two recolored).

## Warheads

| Name | Tier | Upstream model | Modification | Footprint (blocks) | Model size (x, y, z) | Faces | Texture |
|---|---|---|---|---|---|---|---|
| `warhead_t1` | T1 | `mp_w_10_he` (TNT band) | scale 0.5 | 1 x 1 x 1 | 0.50, 1.00, 0.50 | 46 | 64 px |
| `warhead_t2` | T2 | `mp_w_10_incendiary` (orange band) | scale 0.4 | 1 x 1 x 1 | 0.40, 1.00, 0.40 | 70 | 52 px |
| `warhead_t3` | T3 conventional | `mp_w_10_he` | none | 1 x 1 x 2 | 1.00, 2.00, 1.00 | 46 | 64 px (shared look with T1) |
| `warhead_nuclear` | Nuclear | `mp_w_10_nuclear_large` (trefoil, checker base) | scale 0.8 | 1 x 1 x 2 | 0.80, 2.00, 0.80 | 116 | 64 px |
| `warhead_antimatter` | Antimatter (UV) | `mp_w_15_nuclear` | texture hue +140 degrees (yellow to cyan) | 2 x 2 x 4 | 1.50, 3.50, 1.50 | 140 | 96 px |
| `warhead_redmatter` | Red matter (UHV) | `mp_w_15_balefire` (engine pods) | scale 1.2; texture hue -120 degrees (green to red), "PIP 31" label painted out | 2 x 2 x 4 | 1.80, 3.30, 1.80 | 350 | 72 px |

Polygon budget: at most 350 faces per warhead (static block model, not a per-tick renderer). T1 and T2 sit inside 0..1 on every axis with a margin, so they fit a 1 x 1 x 1 cell and a missile body's nose.

Decisions this sheet settles: antimatter and red matter are **2 x 2 x 4**. Texture sizes are not powers of two (48 to 96 px); GL 2.0 and the pack's Angelica handle that, and nothing stitches them into an atlas.

## Known limits

- The "TNT" lettering on T1 and T3 and the radiation trefoils are plain hazard markings from the source textures. NTM lore text ("PIP 31") was removed.
- T3 conventional reuses the T1 model at full size. A different licensed model for it is welcome later.
- **Missile bodies are not sourced yet** (I2). NTM's `missile_parts` fuselage, stability and thruster OBJs under the same license are the planned source.
