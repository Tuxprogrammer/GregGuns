"""Own software renderer for exported GregGuns model JSON (vanilla ModelRenderer box unwrap).

Library used by gun_icons.py; the export comes from ModelExportTest (GREGGUNS_EXPORT_MODELS).
"""
import math
import numpy as np
from PIL import Image, ImageDraw



def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx  # X applied first, then Y, then Z (ModelRenderer order)


def box_quads(b):
    u, v = b["uv"]
    w, h, d = b["size"]
    rp = np.array(b["rotationPoint"], float)
    off = np.array(b["offset"], float)
    x1, y1, z1 = off
    x2, y2, z2 = off + np.array([w, h, d], float)
    V = [np.array(t, float) for t in [(x1, y1, z1), (x2, y1, z1), (x2, y2, z1), (x1, y2, z1),
                                       (x1, y1, z2), (x2, y1, z2), (x2, y2, z2), (x1, y2, z2)]]
    v0, v1, v2, v3, v4, v5, v6, v7 = V
    R = rot(*b["rotate"])

    def W(p):
        return R @ p + rp
    Q = [("left", (v5, v1, v2, v6), (u + d + w, v + d, u + d + w + d, v + d + h)),   # +X face
         ("right", (v0, v4, v7, v3), (u, v + d, u + d, v + d + h)),                  # -X face
         ("top", (v5, v4, v0, v1), (u + d, v, u + d + w, v + d)),
         ("bottom", (v2, v3, v7, v6), (u + d + w, v, u + 2 * w + d, v + d)),
         ("front", (v1, v0, v3, v2), (u + d, v + d, u + d + w, v + d + h)),          # -Z face
         ("back", (v4, v5, v6, v7), (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h))]  # +Z face
    out = []
    for name, vs, (u1, v1_, u2, v2_) in Q:
        uv = [(u2, v1_), (u1, v1_), (u1, v2_), (u2, v2_)]
        out.append((b["name"], name, [W(p) for p in vs], uv))
    return out


def basis(r, dn):
    r = np.array(r, float)
    dn = np.array(dn, float)
    return r, dn, np.cross(r, dn)


def views():
    return {"right (-Z side)": basis((1, 0, 0), (0, 1, 0)), "left (+Z side)": basis((-1, 0, 0), (0, 1, 0)),
            "front (from +X)": basis((0, 0, 1), (0, 1, 0)), "back (from -X)": basis((0, 0, -1), (0, 1, 0)),
            "top": basis((1, 0, 0), (0, 0, -1)), "bottom": basis((1, 0, 0), (0, 0, 1))}


def iso(yaw, pitch):
    y, p = math.radians(yaw), math.radians(pitch)
    r = np.array([math.cos(y), 0, -math.sin(y)])
    f = np.array([math.sin(y) * math.cos(p), math.sin(p), math.cos(y) * math.cos(p)])
    dn = np.cross(f, r)
    dn /= np.linalg.norm(dn)
    if dn[1] < 0:
        dn = -dn
        r = -r  # keep handedness by flipping the pair
    f = np.cross(r, dn)
    return r, dn, f


def raster(quads, tex, bas, S, pad, bounds, sil=False):
    r, dn, f = bas
    x0, y0, x1, y1 = bounds
    Wp = int((x1 - x0) * S) + 2 * pad
    Hp = int((y1 - y0) * S) + 2 * pad
    img = np.full((Hp, Wp, 3), 255.0 if sil else 36.0)
    zb = np.full((Hp, Wp), 1e9)
    light = np.array([-0.3, -0.8, -0.5])
    light /= np.linalg.norm(light)
    for bn, fn, P, UV in quads:
        pts = np.array(P)
        n = np.cross(pts[1] - pts[0], pts[2] - pts[0])
        ln = np.linalg.norm(n)
        if ln < 1e-9:
            n = np.cross(pts[2] - pts[0], pts[3] - pts[0])
            ln = np.linalg.norm(n)
            if ln < 1e-9:
                continue
        n = n / ln
        nn = n if np.dot(n, f) < 0 else -n
        sh = 1 if sil else 0.62 + 0.38 * max(0, np.dot(nn, -light))
        sc = np.stack([(pts @ r - x0) * S + pad, (pts @ dn - y0) * S + pad], 1)
        dep = pts @ f
        for tri in ((0, 1, 2), (0, 2, 3)):
            a, b, c = [sc[i] for i in tri]
            den = (b[1] - c[1]) * (a[0] - c[0]) + (c[0] - b[0]) * (a[1] - c[1])
            if abs(den) < 1e-9:
                continue
            xmin = int(max(0, math.floor(min(a[0], b[0], c[0]))))
            xmax = int(min(Wp - 1, math.ceil(max(a[0], b[0], c[0]))))
            ymin = int(max(0, math.floor(min(a[1], b[1], c[1]))))
            ymax = int(min(Hp - 1, math.ceil(max(a[1], b[1], c[1]))))
            if xmax < xmin or ymax < ymin:
                continue
            gx, gy = np.meshgrid(np.arange(xmin, xmax + 1) + .5, np.arange(ymin, ymax + 1) + .5)
            l1 = ((b[1] - c[1]) * (gx - c[0]) + (c[0] - b[0]) * (gy - c[1])) / den
            l2 = ((c[1] - a[1]) * (gx - c[0]) + (a[0] - c[0]) * (gy - c[1])) / den
            l3 = 1 - l1 - l2
            m = (l1 >= -1e-6) & (l2 >= -1e-6) & (l3 >= -1e-6)
            if not m.any():
                continue
            z = l1 * dep[tri[0]] + l2 * dep[tri[1]] + l3 * dep[tri[2]]
            sub = zb[ymin:ymax + 1, xmin:xmax + 1]
            m &= z < sub - 1e-6
            if not m.any():
                continue
            if sil:
                col = np.zeros((m.sum(), 3))
            else:
                uu = l1 * UV[tri[0]][0] + l2 * UV[tri[1]][0] + l3 * UV[tri[2]][0]
                vv = l1 * UV[tri[0]][1] + l2 * UV[tri[1]][1] + l3 * UV[tri[2]][1]
                tx = np.clip(np.floor(uu[m]).astype(int), 0, tex.shape[1] - 1)
                ty = np.clip(np.floor(vv[m]).astype(int), 0, tex.shape[0] - 1)
                col = tex[ty, tx][:, :3] * sh
            sub[m] = z[m]
            img[ymin:ymax + 1, xmin:xmax + 1][m] = col
    return img
