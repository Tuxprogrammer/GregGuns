package com.gregguns.client.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.gregguns.client.render.profile.BoxSpec;
import com.gregguns.client.render.profile.ClientGunDefinition;
import com.gregguns.client.render.profile.ClientGunDefinitions;

/**
 * Finds z-fighting in the box models: two boxes of one part with a face each that point the same way, lie in the same
 * world-space plane (within {@link #PLANE_EPS} texels) and overlap there with positive area. Both faces then draw at
 * the same depth and flicker. Rotations are applied as {@code ModelRenderer} does (X, then Y, then Z about the pivot),
 * and each box's {@code inflate} moves its faces, so a small negative inflate clears a finding. Pure.
 */
class GunBoxZFightTest {

    private static final double PLANE_EPS = 1e-3;
    private static final double MIN_AREA = 1e-3;
    private static final double PARALLEL_EPS = 1e-4;

    /** Findings that cannot be seen (an internal face), as "gun:boxA|boxB:axis". Each needs a reason. */
    private static final List<String> ALLOWED = new ArrayList<>();

    @Test
    void noTwoBoxesShareAVisibleCoplanarFace() {
        List<String> findings = new ArrayList<>();
        for (ClientGunDefinition def : ClientGunDefinitions.ALL) {
            if (def.gunName()
                .startsWith("akimbo_")) {
                continue; // draws its single gun's boxes twice; the single gun is checked on its own
            }
            findings.addAll(find(def.gunName(), def.boxes()));
        }
        findings.removeAll(ALLOWED);
        assertTrue(findings.isEmpty(), findings.size() + " z-fighting face pair(s):\n" + String.join("\n", findings));
    }

    /** @return one line per coplanar overlapping same-facing face pair, "gun:boxA|boxB:axis (area)" */
    static List<String> find(String gun, BoxSpec[] boxes) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < boxes.length; i++) {
            for (int j = i + 1; j < boxes.length; j++) {
                if (boxes[i].part() != boxes[j].part()) {
                    continue;
                }
                double[][][] fa = faces(boxes[i]);
                double[][][] fb = faces(boxes[j]);
                for (int a = 0; a < 6; a++) {
                    for (int b = 0; b < 6; b++) {
                        double area = overlap(fa[a], fb[b]);
                        if (area > MIN_AREA) {
                            out.add(
                                gun + ":"
                                    + boxes[i].name()
                                    + "|"
                                    + boxes[j].name()
                                    + ":"
                                    + axisName(fa[a])
                                    + " (area "
                                    + String.format("%.3f", area)
                                    + ")");
                        }
                    }
                }
            }
        }
        return out;
    }

    private static String axisName(double[][] face) {
        double[] n = normal(face);
        String[] names = { "x", "y", "z" };
        int best = 0;
        for (int k = 1; k < 3; k++) {
            if (Math.abs(n[k]) > Math.abs(n[best])) {
                best = k;
            }
        }
        return (n[best] > 0 ? "+" : "-") + names[best] + "-ish";
    }

    /** @return the six faces, each four world-space corners in order */
    private static double[][][] faces(BoxSpec s) {
        double i = s.inflate();
        double x0 = s.x() - i, x1 = s.x() + s.w() + i;
        double y0 = s.y() - i, y1 = s.y() + s.h() + i;
        double z0 = s.z() - i, z1 = s.z() + s.d() + i;
        double[][][] f = { { { x0, y0, z0 }, { x0, y1, z0 }, { x0, y1, z1 }, { x0, y0, z1 } }, // -x
            { { x1, y0, z0 }, { x1, y0, z1 }, { x1, y1, z1 }, { x1, y1, z0 } }, // +x
            { { x0, y0, z0 }, { x0, y0, z1 }, { x1, y0, z1 }, { x1, y0, z0 } }, // -y
            { { x0, y1, z0 }, { x1, y1, z0 }, { x1, y1, z1 }, { x0, y1, z1 } }, // +y
            { { x0, y0, z0 }, { x1, y0, z0 }, { x1, y1, z0 }, { x0, y1, z0 } }, // -z
            { { x0, y0, z1 }, { x0, y1, z1 }, { x1, y1, z1 }, { x1, y0, z1 } } }; // +z
        boolean tilted = s.tiltXDegrees() != 0 || s.tiltYDegrees() != 0 || s.tiltZDegrees() != 0;
        if (tilted) {
            double[] p = { s.pivotX(), s.pivotY(), s.pivotZ() };
            for (double[][] face : f) {
                for (double[] v : face) {
                    rotate(v, p, s);
                }
            }
        }
        return f;
    }

    private static void rotate(double[] v, double[] p, BoxSpec s) {
        double[] r = { v[0] - p[0], v[1] - p[1], v[2] - p[2] };
        double a = Math.toRadians(s.tiltXDegrees());
        double c = Math.cos(a), sn = Math.sin(a);
        r = new double[] { r[0], r[1] * c - r[2] * sn, r[1] * sn + r[2] * c };
        a = Math.toRadians(s.tiltYDegrees());
        c = Math.cos(a);
        sn = Math.sin(a);
        r = new double[] { r[0] * c + r[2] * sn, r[1], -r[0] * sn + r[2] * c };
        a = Math.toRadians(s.tiltZDegrees());
        c = Math.cos(a);
        sn = Math.sin(a);
        r = new double[] { r[0] * c - r[1] * sn, r[0] * sn + r[1] * c, r[2] };
        for (int k = 0; k < 3; k++) {
            v[k] = r[k] + p[k];
        }
    }

    private static double[] normal(double[][] q) {
        double[] u = sub(q[1], q[0]);
        double[] w = sub(q[3], q[0]);
        double[] n = { u[1] * w[2] - u[2] * w[1], u[2] * w[0] - u[0] * w[2], u[0] * w[1] - u[1] * w[0] };
        double len = Math.sqrt(dot(n, n));
        if (len < 1e-9) {
            return new double[] { 0, 0, 0 };
        }
        return new double[] { n[0] / len, n[1] / len, n[2] / len };
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[] { a[0] - b[0], a[1] - b[1], a[2] - b[2] };
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /** @return the overlap area of two faces when they face the same way in one plane, else 0 */
    private static double overlap(double[][] a, double[][] b) {
        double[] na = normal(a);
        double[] nb = normal(b);
        if (dot(na, na) == 0 || dot(nb, nb) == 0 || dot(na, nb) < 1 - PARALLEL_EPS) {
            return 0;
        }
        if (Math.abs(dot(na, sub(b[0], a[0]))) > PLANE_EPS) {
            return 0;
        }
        double[] e1 = sub(a[1], a[0]);
        double l = Math.sqrt(dot(e1, e1));
        e1 = new double[] { e1[0] / l, e1[1] / l, e1[2] / l };
        double[] e2 = { na[1] * e1[2] - na[2] * e1[1], na[2] * e1[0] - na[0] * e1[2], na[0] * e1[1] - na[1] * e1[0] };
        List<double[]> pa = project(a, a[0], e1, e2);
        List<double[]> pb = project(b, a[0], e1, e2);
        return area(clip(pa, pb));
    }

    private static List<double[]> project(double[][] q, double[] o, double[] e1, double[] e2) {
        List<double[]> r = new ArrayList<>();
        for (double[] v : q) {
            double[] d = sub(v, o);
            r.add(new double[] { dot(d, e1), dot(d, e2) });
        }
        if (area(r) < 0) {
            Collections.reverse(r);
        }
        return r;
    }

    private static double area(List<double[]> p) {
        double s = 0;
        for (int i = 0; i < p.size(); i++) {
            double[] u = p.get(i), v = p.get((i + 1) % p.size());
            s += u[0] * v[1] - v[0] * u[1];
        }
        return s / 2;
    }

    /** Sutherland-Hodgman clip of a convex counter-clockwise subject by a convex counter-clockwise clipper. */
    private static List<double[]> clip(List<double[]> subject, List<double[]> clipper) {
        List<double[]> out = subject;
        for (int i = 0; i < clipper.size() && !out.isEmpty(); i++) {
            double[] c0 = clipper.get(i), c1 = clipper.get((i + 1) % clipper.size());
            List<double[]> in = out;
            out = new ArrayList<>();
            for (int k = 0; k < in.size(); k++) {
                double[] p = in.get(k), q = in.get((k + 1) % in.size());
                double sp = side(c0, c1, p), sq = side(c0, c1, q);
                if (sp >= 0) {
                    out.add(p);
                }
                if ((sp >= 0) != (sq >= 0)) {
                    double t = sp / (sp - sq);
                    out.add(new double[] { p[0] + t * (q[0] - p[0]), p[1] + t * (q[1] - p[1]) });
                }
            }
        }
        return out;
    }

    private static double side(double[] a, double[] b, double[] p) {
        return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]);
    }
}
