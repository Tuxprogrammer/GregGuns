package com.gregguns.client.utility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * THE PLACEHOLDER TURRET GEOMETRY: one clearly marked box list that a clean-room builder replaces later. Nothing else
 * in the mod knows the shapes; {@link TurretModel} reads this list and nothing more. Pure data, no Minecraft or GL
 * types, so a unit test checks it.
 *
 * <h2>Format</h2>
 * Units are model pixels, 1/16 of a block. The frame is right-handed and Y up: x is to the turret's right, y is up,
 * and +z is the direction it faces at yaw 0. Each box is {@code (x, y, z)}, its minimum corner, and
 * {@code (w, h, d)}, its size along x, y and z.
 *
 * <ul>
 * <li>{@link Part#BASE} boxes are in the <em>block frame</em>: origin at the center of the base block's bottom face,
 * so they fill y 0 to 16. They never move. Nothing draws them yet, because GT draws the block; they are the data a
 * future block or item renderer uses.
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
// Spec: docs/gregguns-spec/09-machines-utility/turret.md (placeholder rendering; the clean-room builder follows the
// model notes in docs/techguns-spec/09-machines-utility/model-notes/turret_base.md and turret_head.md)
public final class TurretGeometry {

    /** Which part of the turret a box belongs to. */
    public enum Part {
        /** Block frame, never moves, not drawn yet. */
        BASE,
        /** Entity frame, stays with the body. */
        FIXED,
        /** Entity frame, turns with the head. */
        YAW,
        /** Entity frame, turns with the head and tilts about the pitch pivot. */
        PITCH
    }

    /** One axis-aligned box. */
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

        Box(String name, Part part, float x, float y, float z, int w, int h, int d, int u, int v) {
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
    }

    /** Pixels per block. */
    public static final int PIXELS_PER_BLOCK = 16;
    /** Width and height of the texture sheet the {@code u, v} values address. */
    public static final int TEXTURE_SIZE = 16;
    /** Height of the pitch pivot above the turret's feet, in pixels. */
    public static final float PITCH_PIVOT_Y = 12.0F;

    /** Every box of the placeholder turret. REPLACE THIS LIST to change the look. */
    // ---- BEGIN BOX LIST ----
    public static final List<Box> BOXES;

    static {
        List<Box> list = new ArrayList<>();
        // Base block (block frame): a plinth, a mounting plate and four corner posts.
        list.add(new Box("base_body", Part.BASE, -7, 0, -7, 14, 14, 14, 0, 0));
        list.add(new Box("base_plate", Part.BASE, -4, 14, -4, 8, 2, 8, 0, 0));
        list.add(new Box("base_post_nw", Part.BASE, -8, 0, -8, 2, 15, 2, 0, 0));
        list.add(new Box("base_post_ne", Part.BASE, 6, 0, -8, 2, 15, 2, 0, 0));
        list.add(new Box("base_post_sw", Part.BASE, -8, 0, 6, 2, 15, 2, 0, 0));
        list.add(new Box("base_post_se", Part.BASE, 6, 0, 6, 2, 15, 2, 0, 0));
        // Turret (entity frame): a collar, a turntable with two yoke arms, and the pitching gun mount.
        list.add(new Box("collar", Part.FIXED, -3, 0, -3, 6, 1, 6, 0, 0));
        list.add(new Box("turntable", Part.YAW, -4, 1, -4, 8, 1, 8, 0, 0));
        list.add(new Box("yoke_left", Part.YAW, 3, 2, -1, 2, 10, 2, 0, 0));
        list.add(new Box("yoke_right", Part.YAW, -5, 2, -1, 2, 10, 2, 0, 0));
        list.add(new Box("gun_mount", Part.PITCH, -2, 10, -3, 4, 4, 6, 0, 0));
        list.add(new Box("gun_barrel", Part.PITCH, -1, 11, 3, 2, 2, 6, 0, 0));
        BOXES = Collections.unmodifiableList(list);
    }
    // ---- END BOX LIST ----

    private TurretGeometry() {}
}
