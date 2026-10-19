package com.gregguns.client.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * THE TURRET GEOMETRY: one clearly marked box list, built clean-room from the model notes. Nothing else
 * in the mod knows the shapes; {@link TurretModel} reads this list and nothing more. Pure data, no Minecraft or GL
 * types, so a unit test checks it.
 *
 * <h2>Format</h2>
 * Units are model pixels, 1/16 of a block. The frame is right-handed and Y up: x is to the turret's right, y is up,
 * and +z is the direction it faces at yaw 0. Each box is {@code (x, y, z)}, its minimum corner, and
 * {@code (w, h, d)}, its size along x, y and z, plus a texture origin. A BASE box may also turn about the vertical
 * through its center ({@code rotY}); the four base struts do.
 *
 * <ul>
 * <li>{@link Part#BASE} boxes are in the <em>block frame</em>: origin at the center of the base block's bottom face,
 * so they fill y 0 to 16. They never move. {@link TurretBaseRenderer} draws them from the base block's tile
 * entity render hook.
 * <li>All other boxes are in the <em>entity frame</em>: origin at the turret's feet, which stand on the top face of the
 * base block, centered on it.
 * <li>{@link Part#FIXED} boxes stay with the body. {@link Part#YAW} boxes turn with the head (they are the yoke).
 * {@link Part#PITCH} boxes turn with the head and also tilt up and down about {@link #PITCH_PIVOT_Y} on the x axis
 * (they are the gun mount).
 * </ul>
 *
 * <p>
 * The texture is one sheet shared by every box ({@code u, v} are each box's texture origin in a sheet of
 * {@link #TEXTURE_SIZE} pixels). Client side by package, but safe on both.
 */
// Spec: docs/gregguns-spec/09-machines-utility/turret.md; model notes in
// docs/techguns-spec/09-machines-utility/model-notes/turret_base.md and turret_head.md
public final class TurretGeometry {

    /** Which part of the turret a box belongs to. */
    public enum Part {
        /** Block frame, never moves. The only boxes that may carry a {@link Box#rotY}. */
        BASE,
        /** Entity frame, stays with the body. */
        FIXED,
        /** Entity frame, turns with the head. */
        YAW,
        /** Entity frame, turns with the head and tilts about the pitch pivot. */
        PITCH
    }

    /** One box, axis-aligned unless it has a {@link Box#rotY}. */
    public static final class Box {

        /** A label for debugging and tests. */
        public final String name;
        /** Which part moves it. */
        public final Part part;
        /** Minimum corner x, in pixels. */
        public final float x;
        /** Minimum corner y, in pixels. */
        public final float y;
        /** Minimum corner z, in pixels. */
        public final float z;
        /** Size along x. */
        public final int w;
        /** Size along y. */
        public final int h;
        /** Size along z. */
        public final int d;
        /** Texture origin u. */
        public final int u;
        /** Texture origin v. */
        public final int v;

        /** Turn about the vertical through the box center, in degrees (x axis toward +z); 0 for most boxes. */
        public final float rotY;

        Box(String name, Part part, float x, float y, float z, int w, int h, int d, int u, int v) {
            this(name, part, x, y, z, w, h, d, u, v, 0.0F);
        }

        Box(String name, Part part, float x, float y, float z, int w, int h, int d, int u, int v, float rotY) {
            this.rotY = rotY;
            this.name = name;
            this.part = part;
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
            this.h = h;
            this.d = d;
            this.u = u;
            this.v = v;
        }

        /**
         * @return the four footprint corners as {x, z} pairs, after the {@link #rotY} turn about the box center
         */
        public float[][] cornersXZ() {
            float cx = x + w / 2.0F;
            float cz = z + d / 2.0F;
            double a = Math.toRadians(rotY);
            float cos = (float) Math.cos(a);
            float sin = (float) Math.sin(a);
            float[][] out = new float[4][];
            for (int i = 0; i < 4; i++) {
                float dx = ((i & 1) == 0 ? -w : w) / 2.0F;
                float dz = ((i & 2) == 0 ? -d : d) / 2.0F;
                out[i] = new float[] { cx + dx * cos - dz * sin, cz + dx * sin + dz * cos };
            }
            return out;
        }
    }

    /** Pixels per block. */
    public static final int PIXELS_PER_BLOCK = 16;
    /** Width and height of the texture sheet the {@code u, v} values address. */
    public static final int TEXTURE_SIZE = 64;
    /** Height of the pitch pivot above the turret's feet, in pixels. */
    public static final float PITCH_PIVOT_Y = 9.5F;

    /** Every box of the turret. REPLACE THIS LIST to change the look. */
    // ---- BEGIN BOX LIST ----
    public static final List<Box> BOXES;

    // Clean-room build from docs/techguns-spec/09-machines-utility/model-notes/turret_base.md (base, block frame:
    // body cube, four corner posts, top plate, four 45 degree struts, four bottom rails, four side ports) and
    // turret_head.md (head, entity frame: foot plate, two uprights, pitching mount plate). One 64 x 64 sheet
    // carries both; tools/asset-gen/turret_models.py reads this list to paint it, so keep one box per line.
    static {
        List<Box> list = new ArrayList<>();
        // Base: the 14 cube, inset 1 from each edge, under the 8 x 8 plate.
        list.add(new Box("base_body", Part.BASE, -7, 0, -7, 14, 14, 14, 0, 0));
        list.add(new Box("base_plate", Part.BASE, -4, 14, -4, 8, 2, 8, 8, 28));
        // Four corner posts, flush with the block edges, 1 below the plate top.
        list.add(new Box("base_post_nw", Part.BASE, -8, 0, -8, 2, 15, 2, 0, 28));
        list.add(new Box("base_post_ne", Part.BASE, 6, 0, -8, 2, 15, 2, 0, 28));
        list.add(new Box("base_post_sw", Part.BASE, -8, 0, 6, 2, 15, 2, 0, 28));
        list.add(new Box("base_post_se", Part.BASE, 6, 0, 6, 2, 15, 2, 0, 28));
        // Four struts turned 45 degrees about the vertical, corner post to plate: an X seen from above.
        list.add(new Box("base_strut_nw", Part.BASE, -8, 14, -6, 6, 1, 2, 8, 38, 45));
        list.add(new Box("base_strut_ne", Part.BASE, 2, 14, -6, 6, 1, 2, 8, 38, -45));
        list.add(new Box("base_strut_sw", Part.BASE, -8, 14, 4, 6, 1, 2, 8, 38, -45));
        list.add(new Box("base_strut_se", Part.BASE, 2, 14, 4, 6, 1, 2, 8, 38, 45));
        // Four bottom rails, flush with the post faces.
        list.add(new Box("base_rail_n", Part.BASE, -6, 0, -8, 12, 2, 1, 8, 41));
        list.add(new Box("base_rail_s", Part.BASE, -6, 0, 7, 12, 2, 1, 8, 41));
        list.add(new Box("base_rail_w", Part.BASE, -8, 0, -6, 1, 2, 12, 0, 46));
        list.add(new Box("base_rail_e", Part.BASE, 7, 0, -6, 1, 2, 12, 0, 46));
        // Four side ports, 1 proud of the cube face.
        list.add(new Box("base_port_n", Part.BASE, -3, 5, -8, 6, 6, 1, 40, 28));
        list.add(new Box("base_port_s", Part.BASE, -3, 5, 7, 6, 6, 1, 40, 28));
        list.add(new Box("base_port_w", Part.BASE, -8, 5, -3, 1, 6, 6, 40, 35));
        list.add(new Box("base_port_e", Part.BASE, 7, 5, -3, 1, 6, 6, 40, 35));
        // Head (entity frame): the fixed yoke turns with the head yaw.
        list.add(new Box("head_foot", Part.YAW, -4, 0, -4, 8, 1, 8, 26, 48));
        list.add(new Box("head_upright_l", Part.YAW, 2, 1, -1, 2, 11, 2, 54, 28));
        list.add(new Box("head_upright_r", Part.YAW, -4, 1, -1, 2, 11, 2, 54, 28));
        // The mount plate pitches with the gun about the left-right axis at PITCH_PIVOT_Y.
        list.add(new Box("head_mount", Part.PITCH, -2, 10, -2, 4, 1, 4, 26, 58));
        BOXES = Collections.unmodifiableList(list);
    }
    // ---- END BOX LIST ----

    private TurretGeometry() {}
}
