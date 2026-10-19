package com.gregguns.utility.charge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Geometry of the explosive charges, and the only place it lives. Renderers read {@link #boxes} and draw the boxes;
 * nothing else in the mod knows what the charge looks like.
 *
 * <p>
 * <b>Frame.</b> Units are 1/16 of a block, so a cell spans 0 to 16 on each axis (x east, y up, z south). Charges are
 * authored in the <i>wall pose</i> of the model notes: the charge hugs the cell's north face ({@code z = 0}) and its
 * detonator plate faces south ({@code +z}). A box is {@code {minX, minY, minZ, maxX, maxY, maxZ}} with min below max.
 * A box may be turned about the vertical axis through its centre (the TNT sticks are diamonds).
 *
 * <p>
 * <b>Face mounting.</b> {@link #boxes(ChargeType, int)} moves the wall pose onto any of the six orientations (the face
 * that was clicked, as {@link ChargeGeometry} numbers them) by a rigid rotation of the cell about its centre, so the
 * rear always lands on the cell face the charge blasts toward. The four walls turn about the vertical axis, so the
 * charge stays upright; the floor and ceiling lay it on its back.
 *
 * <p>
 * <b>Textures.</b> Every box has an unwrap origin in its charge's {@value #TEX_SIZE}-square texture. A box of
 * {@code w x h x d} unwraps like a vanilla model box: the top at {@code (u + d, v)}, the bottom at
 * {@code (u + d + w, v)}, the left, front, right and back faces in a row at {@code v + d}.
 *
 * <p>
 * Pure data; no Minecraft types, so both sides and the tests can use it.
 */
// Spec: docs/techguns-spec/09-machines-utility/explosive-charges.md (World rendering, Models: described only by size);
// model notes (clean-room source of the numbers below):
// docs/techguns-spec/09-machines-utility/model-notes/{tnt_charge,exp_charge}.md
public final class ChargeModel {

    /** Side of the square charge textures, in texels. */
    public static final int TEX_SIZE = 32;

    /** Face indices of a box, in the wall pose: the left face is {@code -x}, the front face is {@code +z}. */
    public static final int LEFT = 0, RIGHT = 1, BOTTOM = 2, TOP = 3, BACK = 4, FRONT = 5;

    /** One drawn box of the model. */
    public static final class Box {

        /** World bounds {minX, minY, minZ, maxX, maxY, maxZ} in 1/16 block: the box as placed, turn included. */
        public final float[] bounds;
        /** What the box stands for, e.g. {@code body} or {@code detonator}; a renderer may tint by it. */
        public final String part;

        private final int orientation;
        /** Wall-pose bounds before the turn. */
        private final float[] local;
        private final float spin;
        private final int u;
        private final int v;

        Box(String part, int orientation, float spinDegrees, int u, int v, float x0, float y0, float z0, float x1,
            float y1, float z1) {
            this.part = part;
            this.orientation = orientation;
            this.spin = spinDegrees;
            this.u = u;
            this.v = v;
            this.local = new float[] { x0, y0, z0, x1, y1, z1 };
            float[] lo = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
            float[] hi = { -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE };
            for (int c = 0; c < 8; c++) {
                float[] p = world((c & 1) == 0 ? x0 : x1, (c & 2) == 0 ? y0 : y1, (c & 4) == 0 ? z0 : z1);
                for (int a = 0; a < 3; a++) {
                    lo[a] = Math.min(lo[a], p[a]);
                    hi[a] = Math.max(hi[a], p[a]);
                }
            }
            this.bounds = new float[] { lo[0], lo[1], lo[2], hi[0], hi[1], hi[2] };
        }

        Box turned(int newOrientation) {
            return new Box(
                part,
                newOrientation,
                spin,
                u,
                v,
                local[0],
                local[1],
                local[2],
                local[3],
                local[4],
                local[5]);
        }

        /** A wall-pose point (box turn applied) moved into the cell's placed orientation. */
        private float[] world(float x, float y, float z) {
            float px = x;
            float pz = z;
            if (spin != 0F) {
                float cx = (local[0] + local[3]) / 2;
                float cz = (local[2] + local[5]) / 2;
                double a = Math.toRadians(spin);
                float c = (float) Math.cos(a);
                float s = (float) Math.sin(a);
                float dx = x - cx;
                float dz = z - cz;
                px = cx + dx * c + dz * s;
                pz = cz - dx * s + dz * c;
            }
            return place(orientation, px, y, pz);
        }

        /** The four world corners of a face, counter-clockwise seen from outside, starting bottom-left. */
        public float[][] quad(int face) {
            float x0 = local[0], y0 = local[1], z0 = local[2], x1 = local[3], y1 = local[4], z1 = local[5];
            float[][] c;
            switch (face) {
                case LEFT:
                    c = new float[][] { { x0, y0, z0 }, { x0, y0, z1 }, { x0, y1, z1 }, { x0, y1, z0 } };
                    break;
                case RIGHT:
                    c = new float[][] { { x1, y0, z1 }, { x1, y0, z0 }, { x1, y1, z0 }, { x1, y1, z1 } };
                    break;
                case BOTTOM:
                    c = new float[][] { { x0, y0, z0 }, { x1, y0, z0 }, { x1, y0, z1 }, { x0, y0, z1 } };
                    break;
                case TOP:
                    c = new float[][] { { x0, y1, z1 }, { x1, y1, z1 }, { x1, y1, z0 }, { x0, y1, z0 } };
                    break;
                case BACK:
                    c = new float[][] { { x1, y0, z0 }, { x0, y0, z0 }, { x0, y1, z0 }, { x1, y1, z0 } };
                    break;
                default: // FRONT
                    c = new float[][] { { x0, y0, z1 }, { x1, y0, z1 }, { x1, y1, z1 }, { x0, y1, z1 } };
                    break;
            }
            float[][] out = new float[4][];
            for (int i = 0; i < 4; i++) {
                out[i] = world(c[i][0], c[i][1], c[i][2]);
            }
            return out;
        }

        /** Texels {u, v} of the four corners of {@link #quad}, in the same order. */
        public float[][] uv(int face) {
            float w = local[3] - local[0];
            float h = local[4] - local[1];
            float d = local[5] - local[2];
            switch (face) {
                case LEFT:
                    return rect(u, v + d, d, h);
                case FRONT:
                    return rect(u + d, v + d, w, h);
                case RIGHT:
                    return rect(u + d + w, v + d, d, h);
                case BACK:
                    return rect(u + 2 * d + w, v + d, w, h);
                case TOP:
                    return rect(u + d, v, w, d);
                default: // BOTTOM
                    return rect(u + d + w, v, w, d);
            }
        }

        /** Corners bottom-left, bottom-right, top-right, top-left, as the texture is drawn (v grows downward). */
        private static float[][] rect(float u0, float v0, float w, float h) {
            return new float[][] { { u0, v0 + h }, { u0 + w, v0 + h }, { u0 + w, v0 }, { u0, v0 } };
        }

        /** Outward unit normal of a face in the placed orientation. */
        public float[] normal(int face) {
            float[][] q = quad(face);
            float ax = q[1][0] - q[0][0], ay = q[1][1] - q[0][1], az = q[1][2] - q[0][2];
            float bx = q[3][0] - q[0][0], by = q[3][1] - q[0][1], bz = q[3][2] - q[0][2];
            float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            return new float[] { nx / len, ny / len, nz / len };
        }
    }

    // --- BOX LISTS (wall pose; clean-room from tnt_charge.md and exp_charge.md) ----------------------------------
    // TNT: three 4 x 12 x 4 sticks turned 45 degrees (centres X 4, 8, 12 at Z 3; Y 2 to 14), and the 8 x 4 x 2
    // plate over the middle of their south corners. Texture: sticks at (0, 0), plate at (0, 16).
    private static final List<Box> TNT_WALL = Collections.unmodifiableList(
        java.util.Arrays.asList(
            new Box("body", 3, 45F, 0, 0, 2F, 2F, 1F, 6F, 14F, 5F),
            new Box("body", 3, 45F, 0, 0, 6F, 2F, 1F, 10F, 14F, 5F),
            new Box("body", 3, 45F, 0, 0, 10F, 2F, 1F, 14F, 14F, 5F),
            new Box("detonator", 3, 0F, 0, 16, 4F, 6F, 4F, 12F, 10F, 6F)));
    // Exp charge: a 10 x 10 x 5 brick on the wall and the same plate sticking out 1 unit.
    // Texture: brick at (0, 0), plate at (0, 16).
    private static final List<Box> ADVANCED_WALL = Collections.unmodifiableList(
        java.util.Arrays.asList(
            new Box("body", 3, 0F, 0, 0, 3F, 3F, 0F, 13F, 13F, 5F),
            new Box("detonator", 3, 0F, 0, 16, 4F, 6F, 4F, 12F, 10F, 6F)));
    // --- end of box lists -----------------------------------------------------------------------------------------

    private ChargeModel() {}

    /**
     * @param type        the charge type
     * @param orientation 0 to 5 (any other value reads as 1)
     * @return the boxes of the charge in a cell, in the frame above, moved to mount on the given face; fresh list
     */
    public static List<Box> boxes(ChargeType type, int orientation) {
        List<Box> wall = type == ChargeType.ADVANCED ? ADVANCED_WALL : TNT_WALL;
        int o = ChargeGeometry.normalize(orientation);
        List<Box> out = new ArrayList<>(wall.size());
        for (Box b : wall) {
            out.add(b.turned(o));
        }
        return out;
    }

    /**
     * Moves a wall-pose point (the charge hugging {@code z = 0}, plate toward {@code +z}) to an orientation. Each case
     * is a proper rotation of the cell about its centre that lays the rear on the face the orientation blasts into:
     * floor and ceiling lay the charge on its back; the walls yaw it about the vertical axis so it stays upright.
     */
    private static float[] place(int o, float x, float y, float z) {
        switch (o) {
            case 0: // blasts +Y: on the ceiling, plate down
                return new float[] { x, 16 - z, y };
            case 1: // blasts -Y: on the floor, plate up
                return new float[] { x, z, 16 - y };
            case 2: // blasts +Z: on the south wall
                return new float[] { 16 - x, y, 16 - z };
            case 4: // blasts +X: on the east wall
                return new float[] { 16 - z, y, x };
            case 5: // blasts -X: on the west wall
                return new float[] { z, y, 16 - x };
            default: // 3: blasts -Z, the wall pose itself
                return new float[] { x, y, z };
        }
    }
}
