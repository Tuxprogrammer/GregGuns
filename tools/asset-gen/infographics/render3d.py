"""Offline renderer for GregGuns box models, used to illustrate the player guide infographics.

It follows Minecraft 1.7.10 ModelRenderer/ModelBox semantics (model space is Y-down, in texels; a renderer
translates to its rotation point and rotates Z, then Y, then X; a box's six faces unwrap from its texture origin).
Output is an RGBA image with a transparent background, so a render can be laid straight onto an infographic.

Sources it reads, all from this repository:
- gun models exported by the test-side ModelExporter (set GREGGUNS_EXPORT_MODELS and run ModelExportTest);
- data-driven mob models in assets/gregguns/entity_models/*.json;
- custom armor models in assets/gregguns/armor_models/*.json and the vanilla two-layer armor layout;
- block textures, for voxel scenes such as multiblock structures.
"""

from __future__ import annotations

import json
import math
from dataclasses import dataclass, field
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter

REPO = Path(__file__).resolve().parents[3]
ASSETS = REPO / "src/main/resources/assets/gregguns"

# ---------------------------------------------------------------- matrices


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]], float)


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]], float)


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]], float)


def tr(x, y, z):
    m = np.eye(4)
    m[:3, 3] = [x, y, z]
    return m


def sc(x, y=None, z=None):
    return np.diag([x, x if y is None else y, x if z is None else z, 1.0])


def rot_zyx(r):
    """ModelRenderer order: glRotate Z, then Y, then X (radians)."""
    return rz(r[2]) @ ry(r[1]) @ rx(r[0])


# ---------------------------------------------------------------- scene


@dataclass
class Quad:
    v: np.ndarray  # (4, 3) world space, Y up, blocks
    uv: np.ndarray  # (4, 2) normalized
    tex: int
    shade: float | None = None  # fixed brightness (voxel faces); None = lit by normal
    tag: int = 0  # nonzero marks faces to outline (see render's marks)


@dataclass
class Scene:
    quads: list = field(default_factory=list)
    textures: list = field(default_factory=list)
    _tex_ids: dict = field(default_factory=dict)

    def texture(self, path) -> int:
        key = str(path)
        if key not in self._tex_ids:
            self._tex_ids[key] = len(self.textures)
            self.textures.append(np.array(Image.open(path).convert("RGBA")))
        return self._tex_ids[key]

    def texture_array(self, name: str, arr: np.ndarray) -> int:
        self._tex_ids[name] = len(self.textures)
        self.textures.append(arr)
        return self._tex_ids[name]

    def extend(self, other: "Scene", M=None):
        """Merge another scene's quads, optionally moved by a world-space 4x4."""
        remap = {}
        for k, i in other._tex_ids.items():
            if k not in self._tex_ids:
                self._tex_ids[k] = len(self.textures)
                self.textures.append(other.textures[i])
            remap[i] = self._tex_ids[k]
        for q in other.quads:
            v = q.v if M is None else (np.hstack([q.v, np.ones((4, 1))]) @ M.T)[:, :3]
            self.quads.append(Quad(v, q.uv, remap[q.tex], q.shade))

    def bounds(self):
        V = np.vstack([q.v for q in self.quads])
        return V.min(0), V.max(0)


# Model space (Y down, texels) to world (Y up, blocks), the same flip Minecraft's entity renderer applies.
MODEL_TO_WORLD = np.diag([-1.0, -1.0, 1.0, 1.0]) @ sc(1 / 16)


def box_quads(M, off, size, uv0, tw, th, mirror, inflate, tex, uscale=1.0, vscale=1.0):
    """The six faces of a 1.7.10 ModelBox, transformed by M. uscale/vscale map a 64x32 layout onto a larger sheet."""
    x, y, z = off
    w, h, d = size
    s = inflate
    x1, y1, z1 = x - s, y - s, z - s
    x2, y2, z2 = x + w + s, y + h + s, z + d + s
    if mirror:
        x1, x2 = x2, x1
    p7, p0, p1, p2 = (x1, y1, z1), (x2, y1, z1), (x2, y2, z1), (x1, y2, z1)
    p3, p4, p5, p6 = (x1, y1, z2), (x2, y1, z2), (x2, y2, z2), (x1, y2, z2)
    u, v = uv0
    w, h, d = int(w), int(h), int(d)
    faces = [
        ([p4, p0, p1, p5], u + d + w, v + d, u + d + w + d, v + d + h),
        ([p7, p3, p6, p2], u, v + d, u + d, v + d + h),
        ([p4, p3, p7, p0], u + d, v, u + d + w, v + d),
        ([p1, p2, p6, p5], u + d + w, v + d, u + d + w + w, v),
        ([p0, p7, p2, p1], u + d, v + d, u + d + w, v + d + h),
        ([p3, p4, p5, p6], u + d + w + d, v + d, u + d + w + d + w, v + d + h),
    ]
    out = []
    for verts, u1, v1, u2, v2 in faces:
        uvs = [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
        if mirror:  # flipFace: the vertex order reverses and each vertex keeps its own texture coordinate
            verts, uvs = verts[::-1], uvs[::-1]
        P = np.array([[*p, 1.0] for p in verts]) @ M.T
        out.append(Quad(P[:, :3], np.array(uvs, float) / [tw, th], tex))
    return out


# ---------------------------------------------------------------- loaders


def gun_scene(export_json: Path, texture: Path | None = None) -> Scene:
    """A gun from ModelExporter JSON; the barrel ends up along world -Z... callers orient with gun_frame()."""
    m = json.loads(Path(export_json).read_text())
    sc_ = Scene()
    if texture is None:
        texture = ASSETS / m["textures"][0].split(":", 1)[1]
    t = sc_.texture(texture)
    tw, th = m["textureWidth"], m["textureHeight"]
    for b in m["boxes"]:
        M = MODEL_TO_WORLD @ tr(*b["rotationPoint"]) @ rot_zyx(b["rotate"])
        sc_.quads += box_quads(M, b["offset"], b["size"], b["uv"], tw, th, b["mirror"], b.get("inflate", 0.0), t)
    return sc_


def _part_matrices(parts, pose, base=None):
    """World matrix of every named part, following parents. pose: {part: (x, y, z) degrees} replaces rotate."""
    by = {p["name"]: p for p in parts}
    cache = {}

    def mat(name):
        if name in cache:
            return cache[name]
        p = by[name]
        r = pose.get(name, p.get("rotate", [0, 0, 0]))
        local = tr(*p.get("pivot", [0, 0, 0])) @ rot_zyx([math.radians(a) for a in r])
        parent = p.get("parent")
        M = (mat(parent) if parent else (base if base is not None else MODEL_TO_WORLD)) @ local
        cache[name] = M
        return M

    for n in by:
        mat(n)
    return cache


def mob_scene(model: str, texture: Path, pose: dict | None = None, hide=()) -> tuple[Scene, dict]:
    """A data-driven mob. Returns the scene and the world matrix of each part (for attaching held items)."""
    m = json.loads((ASSETS / "entity_models" / f"{model}.json").read_text())
    s = Scene()
    t = s.texture(texture)
    mats = _part_matrices(m["parts"], pose or {})
    for p in m["parts"]:
        if p["name"] in hide:
            continue
        for b in p["boxes"]:
            s.quads += box_quads(mats[p["name"]], b["offset"], b["size"], b["uv"], m["textureWidth"],
                                 m["textureHeight"], b.get("mirror", False), b.get("inflate", 0.0), t)
    return s, mats


BIPED_PIVOTS = {"head": (0, 0, 0), "body": (0, 0, 0), "rightArm": (-5, 2, 0), "leftArm": (5, 2, 0),
                "rightLeg": (-1.9, 12, 0), "leftLeg": (1.9, 12, 0)}


def vanilla_armor(scene: Scene, mats: dict, tex_layer1: Path, tex_layer2: Path | None, pieces=("helmet", "chest", "legs", "boots")):
    """Vanilla two-layer armor (64x32 layout, any integer multiple) on a biped's part matrices."""
    def add(part, off, size, uv, inflate, tex, mirror=False):
        scene.quads.extend(box_quads(mats[part], off, size, uv, 64, 32, mirror, inflate, tex))

    t1 = scene.texture(tex_layer1)
    if "helmet" in pieces:
        add("head", (-4, -8, -4), (8, 8, 8), (0, 0), 1.0, t1)
    if "chest" in pieces:
        add("body", (-4, 0, -2), (8, 12, 4), (16, 16), 1.0, t1)
        add("rightArm", (-3, -2, -2), (4, 12, 4), (40, 16), 1.0, t1)
        add("leftArm", (-1, -2, -2), (4, 12, 4), (40, 16), 1.0, t1, True)
    if "boots" in pieces:
        add("rightLeg", (-2, 0, -2), (4, 12, 4), (0, 16), 1.0, t1)
        add("leftLeg", (-2, 0, -2), (4, 12, 4), (0, 16), 1.0, t1, True)
    if "legs" in pieces and tex_layer2 is not None:
        t2 = scene.texture(tex_layer2)
        add("body", (-4, 0, -2), (8, 12, 4), (16, 16), 0.5, t2)
        add("rightLeg", (-2, 0, -2), (4, 12, 4), (0, 16), 0.5, t2)
        add("leftLeg", (-2, 0, -2), (4, 12, 4), (0, 16), 0.5, t2, True)


def custom_armor(scene: Scene, mats: dict, set_id: str, texture: Path | None = None, slots=None):
    m = json.loads((ASSETS / "armor_models" / f"{set_id}.json").read_text())
    t = scene.texture(texture or ASSETS / "textures/models/armor" / f"{set_id}.png")
    for b in m["boxes"]:
        if slots and b.get("slot") not in slots:
            continue
        scene.quads += box_quads(mats[b["part"]], b["offset"], b["size"], b["uv"], m["textureWidth"],
                                 m["textureHeight"], b.get("mirror", False), b.get("inflate", 0.0), t)


# ---------------------------------------------------------------- voxels


def voxel_scene(blocks: dict, textures: dict, tags: dict | None = None) -> Scene:
    """blocks: {(x, y, z): key}; textures: key -> path or (top, side[, front]) paths. Hidden faces are culled.

    Faces get fixed Minecraft-style brightness (top 1.0, north/south 0.8, east/west 0.6, bottom 0.5)."""
    s = Scene()
    ids = {}
    for k, spec in textures.items():
        if isinstance(spec, (str, Path)):
            spec = (spec, spec, spec)
        if len(spec) == 2:
            spec = (spec[0], spec[1], spec[1])
        ids[k] = tuple(s.texture(p) for p in spec)
    solid = set(blocks)
    UV = np.array([(0, 1), (1, 1), (1, 0), (0, 0)], float)
    # (normal, corner offsets ccw seen from outside, brightness)
    faces = {
        (0, 1, 0): ([(0, 1, 1), (1, 1, 1), (1, 1, 0), (0, 1, 0)], 1.0),
        (0, -1, 0): ([(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)], 0.5),
        (0, 0, -1): ([(1, 0, 0), (0, 0, 0), (0, 1, 0), (1, 1, 0)], 0.8),
        (0, 0, 1): ([(0, 0, 1), (1, 0, 1), (1, 1, 1), (0, 1, 1)], 0.8),
        (-1, 0, 0): ([(0, 0, 0), (0, 0, 1), (0, 1, 1), (0, 1, 0)], 0.6),
        (1, 0, 0): ([(1, 0, 1), (1, 0, 0), (1, 1, 0), (1, 1, 1)], 0.6),
    }
    for (x, y, z), key in blocks.items():
        top, side, front = ids[key if not isinstance(key, tuple) else key[0]]
        facing = key[1] if isinstance(key, tuple) else None  # e.g. ("controller", (0, 0, -1))
        for n, (corners, bright) in faces.items():
            if (x + n[0], y + n[1], z + n[2]) in solid:
                continue
            tex = front if facing == n else (top if n[1] != 0 else side)
            v = np.array([(x + a, y + b, z + c) for a, b, c in corners], float)
            s.quads.append(Quad(v, UV, tex, bright, (tags or {}).get(key[0] if isinstance(key, tuple) else key, 0)))
    return s


# ---------------------------------------------------------------- camera and raster


def camera(yaw_deg: float, pitch_deg: float):
    """Orthographic basis. yaw 0 looks at the -Z face (a mob's front); positive yaw orbits toward +X."""
    y, p = math.radians(yaw_deg), math.radians(pitch_deg)
    f = np.array([math.sin(y) * math.cos(p), math.sin(p), -math.cos(y) * math.cos(p)])  # object -> camera
    right = np.cross(np.array([0, 1.0, 0]), f)
    right /= np.linalg.norm(right)
    up = np.cross(f, right)
    return right, up, f


LIGHT = np.array([-0.45, 0.8, -0.55])
LIGHT /= np.linalg.norm(LIGHT)


def render(scene: Scene, yaw=30.0, pitch=25.0, ppu=None, fit=None, ss=3, pad=8, outline=None, ambient=0.6,
           marks: dict | None = None) -> Image.Image:
    """Render to RGBA. ppu = pixels per block; or fit=(w, h) to scale the model into that box.

    outline: optional (r, g, b, a, width) drawn around the silhouette, which keeps dark models legible on dark art.
    marks: {tag: (r, g, b)} rings drawn around the visible faces carrying that tag, e.g. a multiblock's hatches."""
    right, up, f = camera(yaw, pitch)
    V = np.vstack([q.v for q in scene.quads])
    sx, sy = V @ right, V @ up
    w_u, h_u = sx.max() - sx.min(), sy.max() - sy.min()
    if ppu is None:
        fw, fh = fit
        ppu = min((fw - 2 * pad) / max(w_u, 1e-6), (fh - 2 * pad) / max(h_u, 1e-6))
    W = int(math.ceil(w_u * ppu)) + 2 * pad
    H = int(math.ceil(h_u * ppu)) + 2 * pad
    Ws, Hs, S = W * ss, H * ss, ppu * ss
    ox, oy = sx.min() - pad / ppu, sy.max() + pad / ppu
    img = np.zeros((Hs, Ws, 4), np.float32)
    tagbuf = np.zeros((Hs, Ws), np.uint8)
    zb = np.full((Hs, Ws), -1e18, np.float64)
    # Opaque faces first; then translucent ones (glass) far to near, blended and without depth writes.
    see_through = [bool(((t[..., 3] > 25) & (t[..., 3] < 235)).mean() > 0.2) for t in scene.textures]
    opaque = [q for q in scene.quads if not see_through[q.tex]]
    glass = sorted((q for q in scene.quads if see_through[q.tex]), key=lambda q: float((q.v @ f).mean()))
    for q in opaque + glass:
        blend = see_through[q.tex]
        tex = scene.textures[q.tex]
        th_, tw_ = tex.shape[:2]
        px = (q.v @ right - ox) * S
        py = (oy - q.v @ up) * S
        dep = q.v @ f
        if q.shade is not None:
            n = np.cross(q.v[1] - q.v[0], q.v[3] - q.v[0])
            if n @ f <= 0:  # voxel faces are one-sided
                continue
            k = q.shade
        else:
            n = np.cross(q.v[1] - q.v[0], q.v[2] - q.v[0])
            nl = np.linalg.norm(n)
            if nl < 1e-12:
                n = np.cross(q.v[2] - q.v[0], q.v[3] - q.v[0])
                nl = np.linalg.norm(n)
                if nl < 1e-12:
                    continue
            n = n / nl
            if n @ f < 0:
                n = -n
            k = ambient + (1 - ambient) * max(0.0, float(n @ LIGHT))
        for a, b, c in ((0, 1, 2), (0, 2, 3)):
            xs, ys = px[[a, b, c]], py[[a, b, c]]
            x0, x1 = max(0, int(math.floor(xs.min()))), min(Ws - 1, int(math.ceil(xs.max())))
            y0, y1 = max(0, int(math.floor(ys.min()))), min(Hs - 1, int(math.ceil(ys.max())))
            if x0 > x1 or y0 > y1:
                continue
            den = (ys[1] - ys[2]) * (xs[0] - xs[2]) + (xs[2] - xs[1]) * (ys[0] - ys[2])
            if abs(den) < 1e-9:
                continue
            gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            l0 = ((ys[1] - ys[2]) * (gx - xs[2]) + (xs[2] - xs[1]) * (gy - ys[2])) / den
            l1 = ((ys[2] - ys[0]) * (gx - xs[2]) + (xs[0] - xs[2]) * (gy - ys[2])) / den
            l2 = 1 - l0 - l1
            m = (l0 >= -1e-5) & (l1 >= -1e-5) & (l2 >= -1e-5)
            if not m.any():
                continue
            z = l0 * dep[a] + l1 * dep[b] + l2 * dep[c]
            uu = l0 * q.uv[a, 0] + l1 * q.uv[b, 0] + l2 * q.uv[c, 0]
            vv = l0 * q.uv[a, 1] + l1 * q.uv[b, 1] + l2 * q.uv[c, 1]
            tx = np.clip(np.floor(uu * tw_).astype(int), 0, tw_ - 1)
            ty = np.clip(np.floor(vv * th_).astype(int), 0, th_ - 1)
            texel = tex[ty, tx]
            m &= texel[..., 3] > 25
            sub = zb[y0:y1 + 1, x0:x1 + 1]
            m &= z > sub + 1e-7
            if not m.any():
                continue
            dst = img[y0:y1 + 1, x0:x1 + 1]
            if blend:
                a = (texel[..., 3][m] / 255.0)[:, None]
                dst[m, :3] = dst[m, :3] * (1 - a) + texel[..., :3][m] * k * a
                dst[m, 3] = np.maximum(dst[m, 3], texel[..., 3][m])
                continue
            sub[m] = z[m]
            tagbuf[y0:y1 + 1, x0:x1 + 1][m] = q.tag
            dst[m, :3] = texel[..., :3][m] * k
            dst[m, 3] = 255
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")
    if ss != 1:
        out = out.resize((W, H), Image.LANCZOS)
    if marks:
        for tag, rgb in marks.items():
            mask = Image.fromarray(((tagbuf == tag) * 255).astype(np.uint8)).resize((W, H), Image.BILINEAR)
            mask = mask.point(lambda v: 255 if v > 90 else 0)
            if not mask.getbbox():
                continue
            ring = np.array(mask.filter(ImageFilter.MaxFilter(7))).astype(int) - np.array(mask.filter(ImageFilter.MinFilter(3)))
            ring = Image.fromarray(np.clip(ring, 0, 255).astype(np.uint8))
            layer = Image.new("RGBA", out.size, (*rgb, 255))
            layer.putalpha(ring)
            out.alpha_composite(layer)
    if outline:
        r_, g_, b_, alpha, width = outline
        a = out.getchannel("A").point(lambda v: 255 if v > 40 else 0)
        grown = a.filter(ImageFilter.MaxFilter(2 * width + 1)).point(lambda v: v * alpha // 255)
        ring = Image.new("RGBA", out.size, (r_, g_, b_, 255))
        ring.putalpha(grown)
        ring.alpha_composite(out)
        out = ring
    return out


def trim(img: Image.Image, pad=4) -> Image.Image:
    box = img.getchannel("A").getbbox()
    if not box:
        return img
    x0, y0, x1, y1 = box
    return img.crop((max(0, x0 - pad), max(0, y0 - pad), min(img.width, x1 + pad), min(img.height, y1 + pad)))
